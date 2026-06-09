/*
 *   DENOPTIM
 *   Copyright (C) 2019 Marco Foscato <marco.foscato@uib.no>
 * 
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package denoptim.graph.rings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.vecmath.Point3d;

import org.openscience.cdk.graph.ShortestPaths;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.DGraph;
import denoptim.graph.Edge;
import denoptim.graph.Vertex;
import denoptim.io.DenoptimIO;
import denoptim.molecularmodeling.ThreeDimTreeBuilder;
import denoptim.utils.Randomizer;


/**
 * This object represents a path in a {@link DGraph}. The path 
 * involving more than one {@link Vertex} and {@link Edge}. 
 *
 * @author Marco Foscato 
 */

public class PathSubGraph
{
    /**
     * The graph representation of this path. Neither vertexes nor edges
     * belong to the original graph.
     */
    private DGraph graph;

    /**
     * The string identifier of this path
     */
    private String chainID;
    private String revChainID;
    private ArrayList<String> allPossibleChainIDs = new ArrayList<String>();

    /**
     * The vertex representing the first RCA: head of the path
     */
    private Vertex vA;

    /**
     * The vertex representing the second RCA: the tail of the path
     */
    private Vertex vB;

    /**
     * The list of vertices of the original graph and involved in the path.
     */
    private List<Vertex> vertPathVAVB;

    /**
     * The list of edges of the original graph and involved in the path.
     */
    private List<Edge> edgesPathVAVB;

    /**
     * The molecular representation of the fragment in the path
     */
    private IAtomContainer iacPathVAVB;

    /**
     * The list of atoms in the shortest path
     */
    private List<IAtom> atomsPathVAVB;

    /**
     * The list of bonds in the shortest path.
     * Note that the <code>IBond</code>s are those of the entire molecule
     * not of the container representing only this path.
     */
    private List<IBond> bondsPathVAVB;

    /**
     * Per each bond the pair of point used to define the torsion
     */
    private ArrayList<ArrayList<Point3d>> dihedralRefPts;

    /**
     * The flag defining whether this object has already a molecular
     * representation or not
     */
    private boolean hasMolRepr = false; 

    /**
     * The string of atoms involved (atom numbers from full molecule list)
     */
    private String atmNumStr;

    /**
     * The list of closable conformations, if any is found
     */
    private RingClosingConformations rcc;
    
    // Set to true to write useful debug data to file and log
    private boolean debug = false;


//-----------------------------------------------------------------------------
   
    /**
     * Constructs a new path sub graph specifying the first and last vertex of 
     * the path. 
     * @param vA the first vertex of the path
     * @param vB the last vertex of the path
     * @throws DENOPTIMException if the path cannot be found.
     */

    public PathSubGraph(Vertex vA, Vertex vB) throws DENOPTIMException
    {
        this(vA, vB, new HashMap<Vertex, List<Vertex>>());
    }

//-----------------------------------------------------------------------------

    /**
     * Constructs a new path sub graph specifying the first and last vertex of 
     * the path. The shortest path is found by breadth-first search over the
     * neighbourhood relations defined by the combination of the graph's
     * structure and the {@code additionalConnections}.
     * @param vA the first vertex of the path
     * @param vB the last vertex of the path
     * @param adjacency map defining adjacency between vertices. This may or
     * may not reflect the underlying {@link DGraph} structure.
     * @throws DENOPTIMException if the path cannot be found
     */

    public PathSubGraph(Vertex vA, Vertex vB, Map<Vertex, List<Vertex>> adjacency)
        throws DENOPTIMException
    {
        this(vA, vB, adjacency, true);
    }

//-----------------------------------------------------------------------------

    /**
     * Constructs a new path sub graph specifying the first and last vertex of 
     * the path. The shortest path is found by breadth-first search over the
     * neighbourhood relations defined by the graph's
     * structure optionally in combination with the {@code additionalConnections}.
     * @param vA the first vertex of the path
     * @param vB the last vertex of the path
     * @param adjacency map defining adjacency between vertices. This may or
     * may not reflect the underlying {@link DGraph} structure.
     * @param combineAdjacencyAndActualEdges if true, the adjacency and actual
     * edges are combined to form the path. If false, only the adjacency is used.
     * @throws DENOPTIMException if the path cannot be found
     */

    public PathSubGraph(Vertex vA, Vertex vB, Map<Vertex, List<Vertex>> adjacency,
        boolean combineAdjacencyAndActualEdges) throws DENOPTIMException
    {
        // Check assumption that vA and vB are in the same graph.
        if (vA == null || vB == null)
        {
            throw new IllegalArgumentException("Null vertex cannot be used to build a "
                + this.getClass().getSimpleName() + " object.");
        }
        DGraph graphA = vA.getGraphOwner();
        DGraph graphB = vB.getGraphOwner();
        if (graphA == null || graphB == null)
        {  
            Vertex noGraphVertex = null;
            if (graphA == null)
            {
                noGraphVertex = vA;
            } else {
                noGraphVertex = vB;
            }
            throw new IllegalArgumentException("Vertex " + noGraphVertex.getVertexId() 
                + " is not in a graph. "
                + "Cannot build a " + this.getClass().getSimpleName() + " object.");
        }
        if (graphA != graphB)
        {
            throw new IllegalArgumentException("Vertices " + vA.getVertexId() + " and " 
                + vB.getVertexId() + " are not in the same graph. "
                + "Cannot build a " + this.getClass().getSimpleName() + " object.");
        }

        // Build the adjacency map, optionally combining given and edge-based adjacency.
        Map<Vertex, List<Vertex>> adjacencyFromEdges = buildAdjacencyFromGraph(
            vA.getGraphOwner());
        if (combineAdjacencyAndActualEdges) 
        {
            // Add the actual edges to the adjacency map
            for (Map.Entry<Vertex, List<Vertex>> entry : adjacencyFromEdges.entrySet()) 
            {
                Vertex vertex = entry.getKey();
                List<Vertex> neighbors = entry.getValue();
                for (Vertex neighbor : neighbors) {
                    adjacency.computeIfAbsent(vertex, k -> new ArrayList<Vertex>()).add(neighbor);
                    adjacency.computeIfAbsent(neighbor, k -> new ArrayList<Vertex>()).add(vertex);
                }
            }
        }

        // Initialize the object.
        this.vA = vA;
        this.vB = vB;

        // Find the shortest path between vA and vB.
        vertPathVAVB = findShortestPath(vA, vB, adjacency);
        if (vertPathVAVB.isEmpty())
        {
            throw new DENOPTIMException("No path found between vertices "
                    + vA.getVertexId() + " and " + vB.getVertexId());
        }

        // Build the corresponding edge path, but may add null.
        // Here we also set the turning point vertex, if any.
        edgesPathVAVB = new ArrayList<Edge>();
        for (int i=1; i<vertPathVAVB.size(); i++)
        {
            Vertex vPrev = vertPathVAVB.get(i-1);
            Vertex vCurr = vertPathVAVB.get(i);
            Edge edge = vPrev.getEdgeWith(vCurr);
            if (edge != null)
            {
                edgesPathVAVB.add(edge);
            } else {
                edgesPathVAVB.add(null);
            }
        }
        setGraphAndChainIDs();
    }

//-----------------------------------------------------------------------------

    /**
     * Builds an adjacency map from a graph.
     * @param graph the graph to build the adjacency map from
     * @return the adjacency map
     */
    private static Map<Vertex, List<Vertex>> buildAdjacencyFromGraph(DGraph graph)
    {
        Map<Vertex, List<Vertex>> adjacency = new HashMap<Vertex, List<Vertex>>();
        for (Edge e : graph.getEdgeList())
        {
            Vertex srcVertex = e.getSrcAP().getOwner();
            Vertex trgVertex = e.getTrgAP().getOwner();
            adjacency.computeIfAbsent(srcVertex, k -> new ArrayList<Vertex>())
                    .add(trgVertex);
            adjacency.computeIfAbsent(trgVertex, k -> new ArrayList<Vertex>())
                    .add(srcVertex);
        }
        return adjacency;
    }

//-----------------------------------------------------------------------------

    /**
     * Finds the shortest vertex path from {@code vA} to {@code vB} using BFS.
     * Adjacency is defined solely by {@code additionalConnections}, which maps
     * each vertex to the list of its neighbours. The {@link Edge}s of 
     * underlying {@link DGraph} are not considered.
     * @return the shortest path from {@code vA} to {@code vB} as a list of 
     * {@link Vertex}es.
     * @throws DENOPTIMException if the path cannot be found.
     */
    private static List<Vertex> findShortestPath(Vertex vA, Vertex vB,
            Map<Vertex, List<Vertex>> neighbours)
    {
        if (vA == vB)
        {
            return new ArrayList<Vertex>(Arrays.asList(vA));
        }

        Map<Vertex, Vertex> parent = new HashMap<Vertex, Vertex>();
        Set<Long> visited = new HashSet<Long>();
        List<Vertex> queue = new ArrayList<Vertex>();

        queue.add(vA);
        visited.add(vA.getVertexId());
        parent.put(vA, null);

        while (!queue.isEmpty())
        {
            Vertex current = queue.remove(0);

            if (current == vB)
            {
                return reconstructVertexPath(vA, vB, parent);
            }

            List<Vertex> neighbors = neighbours.get(current);
            if (neighbors == null)
            {
                continue;
            }

            for (Vertex neighbor : neighbors)
            {
                if (visited.add(neighbor.getVertexId()))
                {
                    parent.put(neighbor, current);
                    queue.add(neighbor);
                    if (neighbor == vB)
                    {
                        return reconstructVertexPath(vA, vB, parent);
                    }
                }
            }
        }

        return new ArrayList<Vertex>();
    }

//-----------------------------------------------------------------------------

    /**
     * Reconstructs the vertex path from the parent map.
     * @param vA the first vertex of the path
     * @param vB the last vertex of the path
     * @param parent the parent map
     * @return the vertex path from {@code vA} to {@code vB} as a list of 
     * {@link Vertex}es.
     */
    private static List<Vertex> reconstructVertexPath(Vertex vA, Vertex vB,
            Map<Vertex, Vertex> parent)
    {
        // NB: we start from vB and climb towards vA.
        LinkedList<Vertex> path = new LinkedList<Vertex>();
        for (Vertex node = vB; node != null; node = parent.get(node))
        {
            path.addFirst(node);
        }
        return path;
    }

//-----------------------------------------------------------------------------

    /**
     * Sets the graph and chain IDs for the path sub graph.
     */
    private void setGraphAndChainIDs()
    {
        // Build the graph and chain IDs for this sub graph
        chainID = "";
        revChainID = "";
        ArrayList<Vertex> gVertices = new ArrayList<Vertex>();
        ArrayList<Edge> gEdges = new ArrayList<Edge>();
        for (int i=1; i < vertPathVAVB.size()-1; i++)
        {
            Vertex vertBack = vertPathVAVB.get(i-1);
            Vertex vertHere = vertPathVAVB.get(i);
            Vertex vertFrnt = vertPathVAVB.get(i+1);
            Edge edgeToBack = edgesPathVAVB.get(i-1); //NB: may be null!
            Edge edgeToFrnt = edgesPathVAVB.get(i); //NB: may be null!

            AttachmentPoint apBackToHere = null;
            AttachmentPoint apHereToBack = null;
            AttachmentPoint apHereToFrnt = null;
            AttachmentPoint apFrntToHere = null;
            if (edgeToBack!=null)
                {
                    if (edgeToBack.getSrcAP().getOwner() == vertHere)
                    {
                        apHereToBack = edgeToBack.getSrcAP();
                        apBackToHere = edgeToBack.getTrgAP();
                    } else {
                        apHereToBack = edgeToBack.getTrgAP();
                        apBackToHere = edgeToBack.getSrcAP();
                    }
                }
            if (edgeToFrnt!=null)
            {
                if (edgeToFrnt.getSrcAP().getOwner() == vertHere)
                {
                    apHereToFrnt = edgeToFrnt.getSrcAP();
                    apFrntToHere = edgeToFrnt.getTrgAP();
                } else {
                    apHereToFrnt = edgeToFrnt.getTrgAP();
                    apFrntToHere = edgeToFrnt.getSrcAP();
                }
            }

            // Build string representation of the path
            if (i==1)
            {
                // Syntax from vertex.getPathIDs() is used:
                String str = vertBack.getBuildingBlockId() + "/" 
                    + vertBack.getBuildingBlockType() + "/" + 
                    + vertBack.getIndexOfAP(apBackToHere);
                chainID = str + "_";
                revChainID = "_" + str;
            }
            String[] ids = vertHere.getPathIDs(apHereToBack, apHereToFrnt);
            chainID = chainID + ids[0];
            revChainID = ids[1] + revChainID;
            if (i==vertPathVAVB.size()-2)
            {
                String str = vertFrnt.getBuildingBlockId() + "/" 
                    + vertFrnt.getBuildingBlockType() + "/" + 
                    + vertFrnt.getIndexOfAP(apFrntToHere);
                chainID = chainID + "_" + str;
                revChainID = str + "_" + revChainID;
            }
            
            // To build the graph make clones of the actual vertices/edges
            Vertex cloneVertBack = vertBack.clone();
            Vertex cloneVertHere = vertHere.clone();
            Vertex cloneVertFrnt = vertFrnt.clone();
            
            // Collect vertices and make edges to build a graph from A to B
            if (i == 1) {
                gVertices.add(cloneVertBack);
            } else if (i > 1)
            {
                // Need to collect the reference to the vertex defined in the 
                // previous cycle. 
                cloneVertBack = gVertices.get(gVertices.size()-1);
            }
            gVertices.add(cloneVertHere);
            if (edgeToBack!=null) {
                gEdges.add(new Edge(cloneVertBack.getAP(vertBack.getIndexOfAP(apBackToHere)),
                    cloneVertHere.getAP(vertHere.getIndexOfAP(apHereToBack)),
                    edgeToBack.getBondType()));
            }
            if (i == vertPathVAVB.size()-2)
            {
                gVertices.add(cloneVertFrnt);
                if (edgeToFrnt!=null) {
                    gEdges.add(new Edge(cloneVertHere.getAP(vertHere.getIndexOfAP(apHereToFrnt)),
                        cloneVertFrnt.getAP(vertFrnt.getIndexOfAP(apFrntToHere)),
                        edgeToFrnt.getBondType()));
                }
            }
        }

        // Build the DENOPTIMGraph with edges directed from VA to VB
        this.graph = new DGraph(gVertices,gEdges);

    	// prepare alternative chain IDs
    	String[] pA = chainID.split("_");
    	String[] pB = revChainID.split("_");
    	for (int i=1; i<pA.length; i++)
    	{
    	    String altrnA = "";
    	    String altrnB = "";
    	    for (int j=0; j<pA.length; j++)
    	    {
        		if ((i+j) < pA.length)
        		{
                    altrnA = altrnA + pA[i+j] + "_";
                    altrnB = altrnB + pB[i+j] + "_";
        		} else {
        		    altrnA = altrnA + pA[i+j-pA.length] + "_";
                    altrnB = altrnB + pB[i+j-pA.length] + "_";
        		}
    	    }
    	}
    }

//------------------------------------------------------------------------------

    /**
     * Returns a path subgraph from the first given vertex to the
     * second one. The vertices in the path have vacant APs where they would
     * connect to another vertex in the graph that from and to belongs to.
     * The direction of the edges are the same as the graph that from and to
     * belongs to.
     *
     * An empty graph is returned if from == to.
     *
     * @param from start of path.
     * @param to end of path.
     * @throws IllegalArgumentException if from cannot reach to.
     */
    public static DGraph findPath(Vertex from, Vertex to) {
        DGraph g = new DGraph();
        try {
            if (from == to) {
                return g;
            }

            Iterator<AttachmentPoint> path = findPath(from, to,
                    new HashSet<>()).iterator();

            if (!path.hasNext()) {
                return g;
            }

            AttachmentPoint srcAP = path.next().clone();
            Vertex srcVertex = srcAP.getOwner().clone();
            srcAP.setOwner(srcVertex);

            g.addVertex(srcVertex);

            AttachmentPoint trgAP = path.next().clone();
            Vertex trgVertex = trgAP.getOwner().clone();
            trgAP.setOwner(trgVertex);

            g.appendVertexOnAP(srcAP, trgAP);

            while (path.hasNext()) {
                srcAP = path.next().clone();
                srcVertex = srcAP.getOwner().clone();
                srcAP.setOwner(srcVertex);

                trgAP = path.next().clone();
                trgVertex = trgAP.getOwner().clone();
                trgAP.setOwner(trgVertex);

                g.appendVertexOnAP(srcAP, trgAP);
            }
        } catch (DENOPTIMException e) {
            e.printStackTrace();
        }
        return g;
    }

//------------------------------------------------------------------------------

    /**
     * Returns a sequence of APs that is the path from vertex from to
     * vertex to. An empty sequence is returned if from cannot reach to. This
     * usually happens if from and to are not part of the same graph.
     * @param from start of path
     * @param to end of path
     * @param visited vertices already visited.
     * @return the path of APs from vertex from to vertex to.
     */
    private static Iterable<AttachmentPoint> findPath(
            Vertex from, Vertex to, Set<Long> visited) {

        long fromId = from.getVertexId();
        if (visited.contains(fromId)) {
            return new ArrayList<>();
        }
        visited.add(fromId);

        for (AttachmentPoint fromAP : from.getAttachmentPoints()) {
            Edge e = fromAP.getEdgeUser();
            AttachmentPoint adjAP = e.getSrcVertex() == fromId ?
                    e.getTrgAP() : e.getSrcAP();
            Vertex adj = adjAP.getOwner();

            if (adj == to) {
                return Arrays.asList(fromAP, adjAP);
            }

            Iterable<AttachmentPoint> path = findPath(adj, to, visited);
            // Non-empty if there exists a path
            if (path.iterator().hasNext()) {
                List<AttachmentPoint> extendedPath =
                        new ArrayList<AttachmentPoint>(Arrays.asList(
                                fromAP, adjAP));
                path.iterator().forEachRemaining(extendedPath::add);
                return extendedPath;
            }
        }
        // Dead end
        return Collections.emptyList();
    }

//------------------------------------------------------------------------------

    /**
     * Creates the molecular representation, list of atoms and bonds involved
     * in the path between the head and tail.
     * @param mol the full molecule
     * @param make3D if <code>true</code> makes the method generate the 3D
     * coordinates of the chain of fragments by rototranslation of the
     * 3D fragments so that to align the APvectors
     * @throws DENOPTIMException if we cannot make the molecular representation.
     */

    public void makeMolecularRepresentation(IAtomContainer mol, boolean make3D,
            Logger logger, Randomizer randomizer) throws DENOPTIMException
    {
        // Build molecular representation 
        ThreeDimTreeBuilder tb = new ThreeDimTreeBuilder(logger, randomizer);
        iacPathVAVB = tb.convertGraphTo3DAtomContainer(graph);
        Map<IAtom,ArrayList<AttachmentPoint>> apsPerAtom = 
                iacPathVAVB.getProperty(DENOPTIMConstants.MOLPROPAPxATOM);
        Map<IBond,ArrayList<AttachmentPoint>> apsPerBond =
                iacPathVAVB.getProperty(DENOPTIMConstants.MOLPROPAPxBOND);

        if (debug)
        {
    	    String f = "/tmp/pathSubGraph.sdf";
    	    System.out.println("Find SDF representation of path in: " + f);
    	    try {
        	    DenoptimIO.writeSDFFile(f,iacPathVAVB,false);
        	    DenoptimIO.writeSDFFile(f,mol,true);
    	    } catch (Throwable t) {
    	        throw new Error("Could not save debug file '" + f + "'.");
    	    }
    	}

        // Get shortest atom path between the two ends of the chain
        atomsPathVAVB = findAtomPath(iacPathVAVB);

        // Identify which atoms in mol represent the RCA in the current chain.
        // Since we are looking for the verteces of the RCA atoms
        // there is only one atom per each of the two vertexID required
        
        IAtom e0 = null;
        IAtom e1 = null;
        for (IAtom atm : mol.atoms())
        {
            long vrtId = (Long) atm.getProperty(
                    DENOPTIMConstants.ATMPROPVERTEXID);
            if (vrtId == vertPathVAVB.get(0).getVertexId())
                e0 = atm;
                      
            if (vrtId == vertPathVAVB.get(vertPathVAVB.size()-1).getVertexId())
                e1 = atm;
            
            if (e0 != null && e1 != null)
                break;
        }
        List<IAtom> ends = new ArrayList<IAtom>();
        ends.add(e0);
        ends.add(e1);

        // Get path in mol that corresponds to the shortest path in iacPathVAVB
        // This is done to get the bond properties from mol that is a 
        // fully defined molecule.
        // Note that multiple paths with length equal to the shortest length 
        // are possible if there are rings within fragments (vertices), 
        // but in such case the alternative paths involve only non-rotatable.
        // Therefore, the actual identity of the bond doesn't matter in
        // this particular context.
        List<IAtom> pathInFullMol = new ArrayList<IAtom>();
        
        IAtom currentAtm = null;
        long prevAtmInPathVID = -1;
        long currAtmInPathVID = -1;
        long nextAtmInPathVID = -1;
        List<IAtom> candidates = new ArrayList<IAtom>();
        for (int i=1; i<(atomsPathVAVB.size()); i++)
        {
            //We have already found the atoms corresponding to the extremes
            if (i==1)
            {
                currentAtm = ends.get(0);
                pathInFullMol.add(currentAtm);
                candidates.addAll(mol.getConnectedAtomsList(currentAtm));
            } else if (i==(atomsPathVAVB.size()-1))
            {
                pathInFullMol.add(ends.get(1));
                break;
            }
            
            // Now, standard behaviour for all other non-extreme cases
            prevAtmInPathVID = getVertexIdInPath(atomsPathVAVB.get(i-1));
            currAtmInPathVID = getVertexIdInPath(atomsPathVAVB.get(i));
            nextAtmInPathVID = getVertexIdInPath(atomsPathVAVB.get(i+1));
            
            if (prevAtmInPathVID != currAtmInPathVID)
            {
                for (IAtom c : candidates)
                {
                    if (getVertexIdInPath(c) == currAtmInPathVID)
                    {
                        currentAtm = c;
                        pathInFullMol.add(currentAtm);
                        candidates.clear();
                        
                        for (IAtom c2 : mol.getConnectedAtomsList(c))
                        {
                            if (!pathInFullMol.contains(c2))
                                candidates.add(c2);
                        }
                        break;
                    }
                }
                continue;
            } else {
                //NB:  currentAtm remains the same
                List<IAtom> newCandidates = new ArrayList<IAtom>(); 
                for (IAtom nbr : candidates)
                {
                    boolean foundNextLevel = false;
                    for (IAtom nbrNbr : mol.getConnectedAtomsList(nbr))
                    {
                        if (pathInFullMol.contains(nbrNbr)
                                || candidates.contains(nbrNbr))
                            continue;
                        
                        long vid = getVertexIdInPath(nbrNbr);
                        if (vid == nextAtmInPathVID 
                                && currAtmInPathVID!=nextAtmInPathVID)
                        {
                            ShortestPaths sp = new ShortestPaths(mol, 
                                    currentAtm);
                            List<IAtom> itnraVertPath = new ArrayList<IAtom>(
                                    Arrays.asList(sp.atomsTo(nbr)));

                            // currentAtm was already added: skip it
                            for (int j=1; j<itnraVertPath.size(); j++)
                                pathInFullMol.add(itnraVertPath.get(j));
                            
                            currentAtm = nbr;
                            newCandidates.clear();
                            newCandidates.add(nbrNbr);
                            foundNextLevel = true;
                            break;
                        } else {
                            if (vid == currAtmInPathVID)
                            {
                                newCandidates.add(nbrNbr);
                            }
                        }
                    }
                    if (foundNextLevel)
                        break;
                }
                candidates.clear();
                candidates.addAll(newCandidates);
                continue;
            } 
        }
        if (pathInFullMol.size() != atomsPathVAVB.size())
        {
            throw new IllegalStateException("Paths have different size! "
                    + "Unable to "
                    + "proceed in the evaluation of ring closability. "
                    + "Please report this to the author.");
        }

        // Identify the path of bonds between head and tail
        // This is taken from the fully defined mol to inherit rotatability
        // and allow identification of bonds used in multiple rings-closing
        // chains.
        bondsPathVAVB = new ArrayList<IBond>();
        for (int i=0; i < pathInFullMol.size()-1; i++)
        {
            IBond bnd = mol.getBond(pathInFullMol.get(i), 
                                    pathInFullMol.get(i+1));
            bondsPathVAVB.add(bnd);
        }

        // Define points used to calculate dihedral angle of each bond
        // excluding the first and last
        dihedralRefPts = new ArrayList<ArrayList<Point3d>>();
        String keyPropVrtID = DENOPTIMConstants.ATMPROPVERTEXID;
        for (int it=3; it<atomsPathVAVB.size(); it++)
        {
            ArrayList<Point3d> fourPoints = new ArrayList<Point3d>();
            Point3d p0;
            Point3d p1;
            Point3d p2;
            Point3d p3;
            IAtom a0 = atomsPathVAVB.get(it-3);
            IAtom a1 = atomsPathVAVB.get(it-2);
            IAtom a2 = atomsPathVAVB.get(it-1);
            IAtom a3 = atomsPathVAVB.get(it);
            long vIdA0 = a0.getProperty(keyPropVrtID);
            long vIdA1 = a1.getProperty(keyPropVrtID);
            long vIdA2 = a2.getProperty(keyPropVrtID);
            long vIdA3 = a3.getProperty(keyPropVrtID);
            IBond bndA1A2 = iacPathVAVB.getBond(a1,a2);

            // trivial for points 1 and 2
            p0 = a0.getPoint3d(); // only initialization
            p1 = a1.getPoint3d();
            p2 = a2.getPoint3d();
            p3 = a3.getPoint3d(); // only initialization

            // chose point 0
            if (vIdA0 != vIdA1)
            {
                // use the point identified by the ap on atom a1 that 
                // has the lowest index in the list of aps on a1, 
                // but is not the AP used to bind a1 and a2
                for (AttachmentPoint ap : apsPerAtom.get(a1))
                {
                    if (apsPerBond.keySet().contains(bndA1A2))
                    {
                        if (apsPerBond.get(bndA1A2).contains(ap))
                        {
                           continue;
                        }
                    }
                    p0 = new Point3d(ap.getDirectionVector());
                    break;
                }
            }
            else
            {
                // Choose the atom connected to a1 that has the lowest index
                // in the IAtomContainer and is not a2
                List<IAtom> nbrsOfA1 = iacPathVAVB.getConnectedAtomsList(a1);
                int lowIDs = 10000000;
                for (IAtom nbrOfA1 : nbrsOfA1)
                {
                    if (nbrOfA1 == a2)
                    {
                        continue;
                    }
                    int atmID = iacPathVAVB.indexOf(nbrOfA1);
                    if (atmID < lowIDs)
                    {
                        lowIDs = atmID;
                    }
                }
                p0 = new Point3d(iacPathVAVB.getAtom(lowIDs).getPoint3d());
            }

            // choose point 3 (as done for point 0)
            if (vIdA2 != vIdA3)
            {
                // use the point identified by the ap on atom a2 that 
                // has the lowest index in the list of aps on a2, 
                // but is not the AP used to bind a1 and a1
                for (AttachmentPoint ap : apsPerAtom.get(a2))
                {
                    if (apsPerBond.keySet().contains(bndA1A2))
                    {
                        if (apsPerBond.get(bndA1A2).contains(ap))
                        {
                           continue;
                        }
                    }
                    p3 = new Point3d(ap.getDirectionVector());
                    break;
                }
            }
            else
            {
                // Choose the atom connected to a2 that has the lowest index
                // in the IAtomContainer and is not a1.
                List<IAtom> nbrsOfA2 = iacPathVAVB.getConnectedAtomsList(a2);
                int lowIDs = 10000000;
                for (IAtom nbrOfA2 : nbrsOfA2)
                {
                    if (nbrOfA2 == a1)
                    {
                        continue;
                    }
                    int atmID = iacPathVAVB.indexOf(nbrOfA2);
                    if (atmID < lowIDs)
                    {
                        lowIDs = atmID;
                    }
                }
                p3 = new Point3d(iacPathVAVB.getAtom(lowIDs).getPoint3d());
            }

            fourPoints.add(p0);
            fourPoints.add(p1);
            fourPoints.add(p2);
            fourPoints.add(p3);

            dihedralRefPts.add(fourPoints);
        }

        if (debug)
        {
            System.out.println("Points for dihedral angle definition: ");
            for (ArrayList<Point3d> fp : dihedralRefPts)
            {
                for (Point3d p : fp)
                {
                    System.out.println("  "+p);
                }
            System.out.println("  ");
            }
        }

        // set flag
        hasMolRepr = true;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Finds the shortest path of atoms between the first and last atom in the 
     * given atom container.
     * @param iac the atom container
     * @return the path as a list of atoms where the first is the beginning and 
     * the last is the end.
     */
    public static List<IAtom> findAtomPath(IAtomContainer iac)
    {
        IAtom head = iac.getAtom(0);
        IAtom tail = iac.getAtom(iac.getAtomCount()-1);
        ShortestPaths sp = new ShortestPaths(iac, head);
        List<IAtom> path = new ArrayList<IAtom>(Arrays.asList(
                sp.atomsTo(tail)));
        return path;
    }

//-----------------------------------------------------------------------------
    
    private long getVertexIdInPath(IAtom a)
    {
        return a.getProperty(DENOPTIMConstants.ATMPROPVERTEXID, Long.class)
                .longValue();
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the string representation of the path
     */

    public String getChainID()
    {
        return chainID;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns all the possible IDs for this chain. The alternatives differ in
     * the position of the chord.
     */

    public List<String> getAllAlternativeChainIDs()
    {
        return allPossibleChainIDs;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the vertex representing the head of the chain
     */

    public Vertex getHeadVertex()
    {
        return vA;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the vertex representing the tail of the chain
     */

    public Vertex getTailVertex()
    {
        return vB;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the length of the list of edges involved in this path
     */

    public int getPathLength()
    {
        return edgesPathVAVB.size();
    }
//-----------------------------------------------------------------------------

    /**
     * Returns the list of verteces involved
     */

    public List<Vertex> getVertecesPath()
    {
        return vertPathVAVB;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the list of edges involved, if any.
     * 
     * @return the list of edges involved, if any. When edges do not exist
     * because the path is built upon defining a custom adjacency list,
     * some edges may be null.
     */

    public List<Edge> getEdgesPath()
    {
        return edgesPathVAVB;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns true if the molecular representation has been set
     */

    public boolean hasMolecularRepresentation()
    {
        return hasMolRepr;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the molecular representation 
     */
    public IAtomContainer getMolecularRepresentation()
    {
        return iacPathVAVB;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the list of atoms in the path between the head and the tail
     */

    public List<IAtom> getAtomPath()
    {
        return atomsPathVAVB;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the string with atom symbols and number for an easy
     * identification of the path in a molecular structure
     */

    public String getAtomRefStr()
    {
        return atmNumStr;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the list of bonds in the path between the head and the tail.
     * Note that the <code>IBond</code>s are those of the entire molecule
     * not of the <code>IAtomContainer</code> representing only this path.
     */

    public List<IBond> getBondPath()
    {
        return bondsPathVAVB;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the list of point to be used to define the torsion of a bond
     * uniquely (independently on the substituents present in this chain) as
     * a dihedral angle.
     */

    public ArrayList<ArrayList<Point3d>> getDihedralRefPoints()
    {
        return dihedralRefPts;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the ring closing conformations
     */

    public RingClosingConformations getRCC()
    {
        return rcc;
    }

//-----------------------------------------------------------------------------

    /**
     * Set the ring closing conformations to this object
     */

    public void setRCC(RingClosingConformations rcc)
    {
        this.rcc = rcc;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * @return a string describing this path to a human reader
     */
    
    @Override
    public String toString()
    {
       StringBuilder sb = new StringBuilder();
       boolean first = true;
       for (Vertex v : vertPathVAVB)
       {
           if (!first)
           {
               sb.append("-");
           }
           sb.append(v.getVertexId());
           first = false;
       }
       return sb.toString();
    }

    public DGraph getGraph() {
        return graph;
    }

//-----------------------------------------------------------------------------

}
