/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no> and
 *   Marco Foscato <marco.foscato@uib.no>
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

package denoptim.graph;

import java.io.File;
import java.io.Reader;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.jgrapht.alg.isomorphism.VF2GraphIsomorphismInspector;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.fragspace.FragmentSpaceUtils;
import denoptim.graph.APClass.APClassDeserializer;
import denoptim.graph.DENOPTIMEdge.BondType;
import denoptim.graph.DENOPTIMVertex.BBType;
import denoptim.graph.DENOPTIMVertex.DENOPTIMVertexDeserializer;
import denoptim.graph.DENOPTIMVertex.VertexType;
import denoptim.io.DenoptimIO;
import denoptim.json.DENOPTIMgson;
import denoptim.json.DENOPTIMgson.DENOPTIMExclusionStrategyNoAPMap;
import denoptim.logging.DENOPTIMLogger;
import denoptim.rings.ClosableChain;
import denoptim.rings.CyclicGraphHandler;
import denoptim.rings.PathSubGraph;
import denoptim.rings.RingClosureParameters;
import denoptim.threedim.ThreeDimTreeBuilder;
import denoptim.utils.DENOPTIMGraphEdit;
import denoptim.utils.DENOPTIMMoleculeUtils;
import denoptim.utils.GenUtils;
import denoptim.utils.GraphConversionTool;
import denoptim.utils.GraphUtils;
import denoptim.utils.MutationType;
import denoptim.utils.ObjectPair;
import denoptim.utils.RotationalSpaceUtils;


/**
 * Container for the list of vertices and the edges that connect them
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class DENOPTIMGraph implements Serializable, Cloneable
{
    /**
     * Version UID
     */
    private static final long serialVersionUID = 8245921129778644804L;

    /**
     * The vertices belonging to this graph.
     */
    ArrayList<DENOPTIMVertex> gVertices;

    /**
     * The edges belonging to this graph.
     */
    ArrayList<DENOPTIMEdge> gEdges;

    /**
     * The rings defined in this graph.
     */
    ArrayList<DENOPTIMRing> gRings;

    /**
     * The potentially closable chains of vertices.
     */
    ArrayList<ClosableChain> closableChains;

    /*
     * Unique graph id
     */
    int graphId;

    /*
     * store the set of symmetric vertex ids at each level. This is only
     * applicable for symmetric graphs
     */
    ArrayList<SymmetricSet> symVertices;

    /**
     * A free-format string used to record simple properties in the graph. For
     * instance, whether this graph comes from a given initial population or
     * is generated anew from scratch, or from mutation/crossover.
     */
    String localMsg;

    /**
     * Reference to the candidate entity owning this graph, or null
     */
    Candidate candidate;
    
    /**
     * Reference to the {@link DENOPTIMTemplate} embedding this graph
     */
    DENOPTIMTemplate templateJacket;
    
    /**
     * JGraph representation used to detect DENOPTIM-isomorphism
     */
    private DefaultUndirectedGraph<DENOPTIMVertex, UndirectedEdgeRelation> 
        jGraph = null;

    /**
     * Identifier for the format of string representations of a graph
     */
    public enum StringFormat {JSON, GRAPHENC}


//------------------------------------------------------------------------------

    public DENOPTIMGraph(ArrayList<DENOPTIMVertex> gVertices,
                            ArrayList<DENOPTIMEdge> gEdges)
    {
        this.gVertices = gVertices;
        for (DENOPTIMVertex v : this.gVertices)
            v.setGraphOwner(this);
        this.gEdges = gEdges;
        gRings = new ArrayList<>();
        closableChains = new ArrayList<>();
        symVertices = new ArrayList<>();
        localMsg = "";
    }

//------------------------------------------------------------------------------

    public DENOPTIMGraph(ArrayList<DENOPTIMVertex> gVertices,
                            ArrayList<DENOPTIMEdge> gEdges,
                            ArrayList<DENOPTIMRing> gRings)
    {
        this(gVertices, gEdges);
        this.gRings = gRings;
        closableChains = new ArrayList<>();
        symVertices = new ArrayList<>();
        localMsg = "";
    }

//------------------------------------------------------------------------------

    public DENOPTIMGraph(ArrayList<DENOPTIMVertex> gVertices,
                            ArrayList<DENOPTIMEdge> gEdges,
                            ArrayList<DENOPTIMRing> gRings,
                            ArrayList<SymmetricSet> symVertices)
    {
        this(gVertices, gEdges, gRings);
        closableChains = new ArrayList<>();
        this.symVertices = symVertices;
        localMsg = "";
    }

//------------------------------------------------------------------------------

    public DENOPTIMGraph(ArrayList<DENOPTIMVertex> gVertices,
                            ArrayList<DENOPTIMEdge> gEdges,
                            ArrayList<DENOPTIMRing> gRings,
                            ArrayList<ClosableChain> closableChains,
                            ArrayList<SymmetricSet> symVertices)
    {
        this(gVertices, gEdges, gRings, symVertices);
        this.closableChains = closableChains;
        localMsg = "";
    }

//------------------------------------------------------------------------------

    public DENOPTIMGraph()
    {
        gVertices = new ArrayList<>();
        gEdges = new ArrayList<>();
        gRings = new ArrayList<>();
        closableChains = new ArrayList<>();
        symVertices = new ArrayList<>();
        localMsg = "";
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Sets the reference to the candidate item that is defined by this graph.
     * @param candidate the candidate owner.
     */
    public void setCandidateOwner(Candidate candidate)
    {
        this.candidate = candidate;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the reference of the candidate item that is defined by this 
     * graph.
     * @return the reference to the owner or null.
     */
    public Candidate getCandidateOwner()
    {
        return candidate;
    }

//------------------------------------------------------------------------------

    public void setGraphId(int id)
    {
        graphId = id;
    }

//------------------------------------------------------------------------------

    public int getGraphId()
    {
        return graphId;
    }

//------------------------------------------------------------------------------

    public void setLocalMsg(String msg)
    {
        localMsg = msg;
    }

//------------------------------------------------------------------------------

    public String getLocalMsg()
    {
        return localMsg;
    }

//------------------------------------------------------------------------------

    public boolean hasSymmetricAP()
    {
        return (!symVertices.isEmpty());
    }

//------------------------------------------------------------------------------

    public boolean hasSymmetryInvolvingVertex(DENOPTIMVertex v)
    {
        boolean res = false;
        for (SymmetricSet ss : symVertices)
        {
            if (ss.contains(v.getVertexId()))
            {
                res = true;
                break;
            }
        }
        return res;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the number of symmetric sets of vertices
     * @return the number of symmetric sets of vertices
     */
    public int getSymmetricSetCount()
    {
    	return symVertices.size();
    }

//------------------------------------------------------------------------------

    /**
     * Get an iterator for the sets of symmetrically related vertices.
     */

    public Iterator<SymmetricSet> getSymSetsIterator()
    {
        return symVertices.iterator();
    }

//------------------------------------------------------------------------------

    /**
     * @param v the vertex for which we want the list of symmetric vertexes.
     * @return a list that includes v but can be empty.
     */
    public ArrayList<DENOPTIMVertex> getSymVertexesForVertex(DENOPTIMVertex v)
    {
        ArrayList<DENOPTIMVertex> lst = new ArrayList<DENOPTIMVertex>();
        for (SymmetricSet ss : symVertices)
        {
            if (ss.contains(v.getVertexId()))
            {
                for (Integer vid : ss.getList())
                {
                    lst.add(this.getVertexWithId(vid));
                }
            }
        }
        return lst;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Removed the given symmetric set, if present.
     * @param ss the symmetric relation to be removed.
     */
    public void removeSymmetrySet(SymmetricSet ss)
    {
        symVertices.remove(ss); 
    }
    
//------------------------------------------------------------------------------

    public SymmetricSet getSymSetForVertex(DENOPTIMVertex v)
    {
        for (SymmetricSet ss : symVertices)
        {
            if (ss.contains(v.getVertexId()))
            {
                return ss;
            }
        }
        return new SymmetricSet();
    }
    
//------------------------------------------------------------------------------

    public SymmetricSet getSymSetForVertexID(int vid)
    {
        for (SymmetricSet ss : symVertices)
        {
            if (ss.contains(vid))
            {
                return ss;
            }
        }
        return new SymmetricSet();
    }

//------------------------------------------------------------------------------

    public void setSymmetricVertexSets(ArrayList<SymmetricSet> symVertices)
    {
        this.symVertices.clear();
        this.symVertices.addAll(symVertices);
    }

//------------------------------------------------------------------------------

    /**
     * Adds a symmetric set of vertices to this graph.
     * @param symSet the set to add
     * @throws DENOPTIMException is the symmetric set being added contains
     * an id that is already contained in another set already present.
     */
    public void addSymmetricSetOfVertices(SymmetricSet symSet)
                                                        throws DENOPTIMException
    {
        for (SymmetricSet oldSS : symVertices)
        {
            for (Integer vid : symSet.getList())
            {
                if (oldSS.contains(vid))
                {
                    throw new DENOPTIMException("Adding " + symSet + " while "
                                                + "there is already " + oldSS
                                                + " that contains " + vid);
                }
            }
        }
        symVertices.add(symSet);
    }

//------------------------------------------------------------------------------

    public void setVertexList(ArrayList<DENOPTIMVertex> vertices)
    {
        gVertices = vertices;
        jGraph = null;
    }

//------------------------------------------------------------------------------

    public void setEdgeList(ArrayList<DENOPTIMEdge> edges)
    {
        gEdges = edges;
        jGraph = null;
    }

//------------------------------------------------------------------------------

    public void setRings(ArrayList<DENOPTIMRing> rings)
    {
        gRings = rings;
        jGraph = null;
    }

//------------------------------------------------------------------------------

    public void setCandidateClosableChains(ArrayList<ClosableChain> closableChains)
    {
        this.closableChains = closableChains;
    }

//------------------------------------------------------------------------------

    public ArrayList<ClosableChain> getClosableChains()
    {
        return closableChains;
    }

//------------------------------------------------------------------------------

    public ArrayList<DENOPTIMVertex> getVertexList()
    {
        return gVertices;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Identifies and return the vertex from which the spanning tree originates.
     * This is typically the first vertex in the list, but it is possible to
     * programmatically build graphs that do not follow this convention. 
     * Therefore, here we test if the first vertex is a seed (i.e., it only has
     * departing edges) and, if not, we search for the closest seed.
     * <b>WARNING</b>: the graph is assumed to be an healthy spanning tree, in 
     * that it has only one seed that is reachable from any vertex by a 
     * inverse directed path.
     * The result <code>null</code> for disconnected graphs or otherwise 
     * unhealthy spanning trees.
     * 
     * @return the seed/root of the spanning tree
     */
    public DENOPTIMVertex getSourceVertex()
    {
        switch (gVertices.size())
        {
            case 0:
                return null;
            case 1:
                return getVertexAtPosition(0);
        }
        DENOPTIMVertex v0 = getVertexAtPosition(0);
        for (DENOPTIMEdge e : this.getEdgeList())
        {
            if (e.getTrgAP().getOwner() == v0)
            {
                ArrayList<DENOPTIMVertex> parentTree = new ArrayList<>();
                getParentTree(v0,parentTree);
                return parentTree.get(parentTree.size()-1);
            }
        }
        return v0;
    }

//------------------------------------------------------------------------------

    public ArrayList<DENOPTIMEdge> getEdgeList()
    {
        return gEdges;
    }

//------------------------------------------------------------------------------

    public ArrayList<DENOPTIMRing> getRings()
    {
        return gRings;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the list of edges that depart from the given vertex, i.e., edges
     * where the srcAP is owned by the given vertex.
     * @param v the given vertex.
     * @return the list of edges departing from the given vertex.
     */
    public ArrayList<DENOPTIMEdge> getEdgesWithSrc(DENOPTIMVertex v)
    {
    	ArrayList<DENOPTIMEdge> edges = new ArrayList<DENOPTIMEdge>();
    	for (DENOPTIMEdge e : this.getEdgeList())
    	{
    		if (e.getSrcAP().getOwner() == v)
    		{
    			edges.add(e);
    		}
    	}
    	return edges;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the list of rings that include the given vertex in their 
     * fundamental cycle.
     * @param v the vertex to search for.
     * @return the list of rings of an empty list.
     */
    public ArrayList<DENOPTIMRing> getRingsInvolvingVertex(DENOPTIMVertex v)
    {
        ArrayList<DENOPTIMRing> rings = new ArrayList<DENOPTIMRing>();
        for (DENOPTIMRing r : gRings)
        {
            if (r.contains(v))
            {
                rings.add(r);
            }
        }
        return rings;
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the list of rings that include the given list of vertexes in their 
     * fundamental cycle.
     * @param vs the collection of vertexes to search for.
     * @return the list of rings of an empty list.
     */
    public ArrayList<DENOPTIMRing> getRingsInvolvingVertex(DENOPTIMVertex[] vs)
    {
        ArrayList<DENOPTIMRing> rings = new ArrayList<DENOPTIMRing>();
        for (DENOPTIMRing r : gRings)
        {
            boolean matchesAll = true;
            for (int i=0; i<vs.length; i++)
            {
                if (!r.contains(vs[i]))
                {
                    matchesAll = false;
                    break;
                }
            }
            if (matchesAll)
            {
                rings.add(r);
            }
        }
        return rings;
    }

//------------------------------------------------------------------------------

    public ArrayList<DENOPTIMRing> getRingsInvolvingVertexID(int vid)
    {
        ArrayList<DENOPTIMRing> rings = new ArrayList<DENOPTIMRing>();
        for (DENOPTIMRing r : gRings)
        {
            if (r.containsID(vid))
            {
                rings.add(r);
            }
        }
        return rings;
    }

//------------------------------------------------------------------------------

    public boolean hasRings()
    {
        return gRings.size() > 0;
    }

//------------------------------------------------------------------------------

    public boolean isVertexIDInRing(int vid)
    {
        boolean result = false;
        for (DENOPTIMRing  r : gRings)
        {
            if (r.containsID(vid))
            {
                result = true;
                break;
            }
        }
        return result;
    }

//------------------------------------------------------------------------------

    public boolean isVertexInRing(DENOPTIMVertex v)
    {
        boolean result = false;
        for (DENOPTIMRing  r : gRings)
        {
            if (r.contains(v))
            {
                result = true;
                break;
            }
        }
        return result;
    }

//------------------------------------------------------------------------------

    /**
     * Search for ring closing vertices: vertices that contain only a
     * <code>RingClosingAttractor</code>
     * @return the list of ring closing vertices
     */

    public ArrayList<DENOPTIMVertex> getRCVertices()
    {
        ArrayList<DENOPTIMVertex> rcvLst = new ArrayList<DENOPTIMVertex>();
        for (DENOPTIMVertex v : gVertices)
        {
            if (v.isRCV())
            {
                rcvLst.add(v);
            }
        }
        return rcvLst;
    }

//------------------------------------------------------------------------------

    /**
     * Search for unused ring closing vertices: vertices that contain only a
     * <code>RingClosingAttractor</code> and are not part of any
     * <code>DENOPTIMRing</code>
     * @return the list of unused ring closing vertices
     */

    public ArrayList<DENOPTIMVertex> getFreeRCVertices()
    {
        ArrayList<DENOPTIMVertex> rcvLst = getRCVertices();
        ArrayList<DENOPTIMVertex> free = new ArrayList<DENOPTIMVertex>();
        for (DENOPTIMVertex v : rcvLst)
        {
            if (!isVertexInRing(v))
            {
                free.add(v);
            }
        }
        return free;
    }

//------------------------------------------------------------------------------

    /**
     * Search for used ring closing vertices: vertices that contain only a
     * <code>RingClosingAttractor</code> and are part of a
     * <code>DENOPTIMRing</code>.
     * @return the list of ring closing vertices
     */

    public ArrayList<DENOPTIMVertex> getUsedRCVertices()
    {
        ArrayList<DENOPTIMVertex> used = new ArrayList<DENOPTIMVertex>();
        used.addAll(getRCVertices());
        used.removeAll(getFreeRCVertices());
        return used;
    }

//------------------------------------------------------------------------------

    /**
     * Adds the edge to the list of edges belonging to this graph.
     * @param edge to be included in the list of edges
     */
    public void addEdge(DENOPTIMEdge edge)
    {
        gEdges.add(edge);
        jGraph = null;
    }

//------------------------------------------------------------------------------

    public void addRing(DENOPTIMRing ring)
    {
        gRings.add(ring);
        jGraph = null;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Adds a chord between the given vertices, thus adding a ring in this graph
     * @param vI one of the ring-closing vertices.
     * @param vJ the other of the ring-closing vertices.
     * @throws DENOPTIMException if the two vertices do not have consistent bond
     * types to their parents and, therefore, we cannot infer the bond type of
     * the chord.
     */
    public void addRing(DENOPTIMVertex vI, DENOPTIMVertex vJ) 
            throws DENOPTIMException
    {
        BondType bndTypI = vI.getEdgeToParent().getBondType();
        BondType bndTypJ = vJ.getEdgeToParent().getBondType();
        if (bndTypI != bndTypJ)
        {
            String s = "Attempt to close rings is not compatible "
            + "to the different bond type specified by the "
            + "head and tail APs: (" + bndTypI + "!=" 
            + bndTypJ + " for vertices " + vI + " " 
            + vJ + ")";
            throw new DENOPTIMException(s);
        }
        addRing(vI,vJ,bndTypI);
        jGraph = null;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Adds a chord between the given vertices, thus adding a ring in this
     * graph.
     * @param vI one of the ring-closing vertices.
     * @param vJ the other of the ring-closing vertices.
     * @param bndTyp the bond type the chord corresponds to.
     */
    public void addRing(DENOPTIMVertex vI, DENOPTIMVertex vJ, 
            BondType bndTyp)
    {
        PathSubGraph path = new PathSubGraph(vI,vJ,this);
        ArrayList<DENOPTIMVertex> arrLst = new ArrayList<DENOPTIMVertex>();
        arrLst.addAll(path.getVertecesPath());                    
        DENOPTIMRing ring = new DENOPTIMRing(arrLst);
        ring.setBondType(bndTyp);
        this.addRing(ring);
        jGraph = null;
    }

//------------------------------------------------------------------------------

    /**
     * Appends a vertex to this graph without creating any edge. This is a
     * good way to add the first vertex, but can be used also if you intend to 
     * add edges manually.
     * @param vertex the vertex that will be added (no cloning).
     * @throws DENOPTIMException in the vertex has an ID that is already 
     * present in the current list of vertices of this graph, if any.
     */
    public void addVertex(DENOPTIMVertex vertex) throws DENOPTIMException
    {
        if (containsVertexID(vertex.getVertexId()))
            throw new DENOPTIMException("Vertex must have a VertexID that is "
                    + "unique within the graph. VertexID '" 
                    + vertex.getVertexId()+ "' already present in graph " 
                    + getGraphId());
        vertex.setGraphOwner(this);
        gVertices.add(vertex);
        jGraph = null;
    }

//------------------------------------------------------------------------------

    /**
     * Remove a vertex from this graph. This method removes also edges and rings
     * that involve the given vertex. Symmetric sets of vertices are corrected
     * accordingly: they are removed if there is only one remaining vertex in
     * the set, or purged from the vertex being removed.
     * @param vertex the vertex to remove.
     */
    public void removeVertex(DENOPTIMVertex vertex)
    {
        if (!gVertices.contains(vertex))
        {
        	return;
        }

        vertex.resetGraphOwner();
        int vid = vertex.getVertexId();

        // delete also any ring involving the removed vertex
        if (isVertexInRing(vertex))
        {
            ArrayList<DENOPTIMRing> rToRm = getRingsInvolvingVertex(vertex);
            for (DENOPTIMRing r : rToRm)
            {
                removeRing(r);
            }
        }

        // remove edges involving the removed vertex
        ArrayList<DENOPTIMEdge> eToDel = new ArrayList<>();
        for (int i=0; i<gEdges.size(); i++)
        {
            DENOPTIMEdge edge = gEdges.get(i);
            if (vid == edge.getTrgVertex())
            {
                eToDel.add(edge);
            }
            // NB: the following allows to break the spanning tree
            if (vid == edge.getSrcVertex())
            {
                eToDel.add(edge);
            }
        }
        for (DENOPTIMEdge e : eToDel)
        {
            this.removeEdge(e);
        }

        // delete the removed vertex from symmetric sets, but leave other vrtxs
        ArrayList<SymmetricSet> ssToRemove = new ArrayList<SymmetricSet>();
        for (SymmetricSet ss : symVertices)
        {
            if (ss.contains(vid))
            {
                if (ss.size() < 3)
                {
                    ssToRemove.add(ss);
                }
                else
                {
                    // NB: casting needed to remove the object Integer 'vid'
                    // from the list instead of entry number 'vid'
                    ss.remove((Integer) vid);
                }
            }
        }
        symVertices.removeAll(ssToRemove);

        // remove the vertex from the graph
        gVertices.remove(vertex);
        
        jGraph = null;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Remove a given vertex belonging to this graph and re-connects the 
     * resulting graph branches as much as possible.
     * This method reproduces the change of vertex on all symmetric sites.
     * @param oldLink the vertex currently belonging to this graph and to be 
     * replaced.
     * @return <code>true</code> if the operation is successful.
     * @throws DENOPTIMException
     */
    public boolean removeVertexAndWeld(DENOPTIMVertex vertex) throws DENOPTIMException
    {
        if (!gVertices.contains(vertex))
        {
            return false;
        }
        
        ArrayList<DENOPTIMVertex> symSites = getSymVertexesForVertex(vertex);
        if (symSites.size() == 0)
        {
            symSites.add(vertex);
        } else {
            //TODO-V3 flip coin to decide if this should be a symmetric operation or not
        }
        for (DENOPTIMVertex oldLink : symSites)
        {
            GraphUtils.ensureVertexIDConsistency(this.getMaxVertexId());
            if (!removeSingleVertexAndWeld(oldLink))
            {
                return false;
            }
        }
        // Reject deletions that cause the collapse of a 3-atom ring into a
        // loop (i.e., 1-atom ring) or multiple connection (i.e., a 3-atom ring)
        for (DENOPTIMRing r : gRings)
        {
            // 3 = 1 actual vertex + 2 RCVs
            if (r.getSize()!=3)
                continue;
            
            DENOPTIMAttachmentPoint apH = r.getHeadVertex().getEdgeToParent()
                    .getSrcAPThroughout();
            DENOPTIMAttachmentPoint apT = r.getTailVertex().getEdgeToParent()
                    .getSrcAPThroughout();
            
            if (apH.hasSameSrcAtom(apT) || apH.hasConnectedSrcAtom(apT))
                return false;
        }
        return true;
    }
  
//------------------------------------------------------------------------------
    
    /**
     * Remove a given vertex belonging to this graph and re-connects the 
     * resulting graph branches as much as possible.
     * @param vertex the vertex currently belonging to this graph and to be 
     * replaced.
     * @return <code>true</code> if the operation is successful.
     * @throws DENOPTIMException
     */
    public boolean removeSingleVertexAndWeld(DENOPTIMVertex vertex) 
            throws DENOPTIMException
    {
        if (!gVertices.contains(vertex))
        {
            return false;
        }
        if (vertex == getSourceVertex())
        {
            // Make sure there is something to weld, or give up. This to avoid
            // trying to remove the scaffold vertex (i.e., a vertex that has no
            // parents in this graph and the APs of which are only user as 
            // source APs)
            boolean foundLinkToParent = false;
            for (DENOPTIMAttachmentPoint ap : vertex.getAttachmentPoints())
            {
                if (ap.isAvailable() && !ap.isAvailableThroughout())
                {
                    if (!ap.isSrcInUserThroughout())
                        foundLinkToParent = true;
                }
            }
            if (!foundLinkToParent)
                return false;
            
            // When we try to remove the only vertex inside a template, we
            // are removing the template itself
            if (gVertices.size()==1 && templateJacket!=null)
            {
                return templateJacket.getGraphOwner().removeSingleVertexAndWeld(
                        templateJacket);
            }
        }
        
        // Get all APs that we'll try to weld into the parent
        ArrayList<DENOPTIMAttachmentPoint> needyAPsOnChildren = 
                new ArrayList<DENOPTIMAttachmentPoint>();
        // And all APs where we could weld onto
        ArrayList<DENOPTIMAttachmentPoint> freeAPsOnParent = 
                new ArrayList<DENOPTIMAttachmentPoint>();
        //        vertex.getAPsFromChilddren(); //No, because it enter templates
        Map<DENOPTIMAttachmentPoint,DENOPTIMAttachmentPoint> apOnOldToNeedyAP = 
                new HashMap<DENOPTIMAttachmentPoint,DENOPTIMAttachmentPoint>();
        for (DENOPTIMAttachmentPoint apOnOld : vertex.getAttachmentPoints())
        {
            if (!apOnOld.isAvailableThroughout())
            {
                if (apOnOld.isSrcInUserThroughout())
                {
                    // Links that depart from vertex
                    DENOPTIMAttachmentPoint needyAP = 
                            apOnOld.getLinkedAPThroughout();
                    // NB: here do not use getSrcThroughout because it would 
                    // enter trg templates rather than staying on their surface.
                    needyAPsOnChildren.add(needyAP);
                    apOnOldToNeedyAP.put(apOnOld, needyAP);
                } else {
                    DENOPTIMAttachmentPoint apOnParent = 
                            apOnOld.getLinkedAPThroughout();
                    // NB: here do not use getSrcThroughout because it would 
                    // enter src templates rather than staying on their surface.
                    freeAPsOnParent.add(apOnParent);
                    freeAPsOnParent.addAll(apOnParent.getOwner()
                            .getFreeAPThroughout());
                }
            }
        }
        
        // Get all possible parentAPs-childAPs mappings
        List<APMapping> mappings = FragmentSpaceUtils.mapAPClassCompatibilities(
                freeAPsOnParent, needyAPsOnChildren, 500);
        if (mappings.size() == 0)
        {
            // No APClass-compatible possibility of removing the link.
            return false;
        }
        
        // Score mapping to prioritise those that preserve rings.
        // This to differentiate from the combination of DELETE+EXTEND operation
        // which cannot preserve rings.
        List<Integer> preferences = new ArrayList<Integer>();
        // Score rings in this level's graph
        for (int i=0; i<needyAPsOnChildren.size(); i++)
        {
            DENOPTIMAttachmentPoint needyAP = needyAPsOnChildren.get(i);
            preferences.add(0);
            
            DENOPTIMVertex ownerOfNeedy = needyAP.getOwner();
            for (DENOPTIMRing r : ownerOfNeedy.getGraphOwner()
                    .getRingsInvolvingVertex(ownerOfNeedy))
            {
                // NB: here we stay at the level of the graph owning ownerOfNeedy
                DENOPTIMVertex lastBeforeOwnerOfNeedy = 
                        needyAP.getLinkedAP().getOwner();
                if (r.contains(lastBeforeOwnerOfNeedy))
                {
                    preferences.set(i, preferences.get(i) + 1); 
                }
            }
        }
        
        // Choose best scoring mapping
        int maxScore = Integer.MIN_VALUE;
        APMapping bestScoringMapping = null;
        for (APMapping apm : mappings)
        {
            int score = 0;
            for (DENOPTIMAttachmentPoint ap : apm.values())
            {
                score = score + preferences.get(needyAPsOnChildren.indexOf(ap));
            }
            if (score > maxScore)
            {
                maxScore = score;
                bestScoringMapping = apm;
            }
        }
        
        APMapping bestScoringMappingReverse = new APMapping();
        for (Entry<DENOPTIMAttachmentPoint, DENOPTIMAttachmentPoint> e : 
            bestScoringMapping.entrySet())
        {
            bestScoringMappingReverse.put(e.getValue(), e.getKey());
        }
        
        // Update rings involving vertex directly (i.e., in this graph)
        ArrayList<DENOPTIMRing> rToEdit = getRingsInvolvingVertex(vertex);
        ArrayList<DENOPTIMRing> ringsToRemove = new ArrayList<DENOPTIMRing>();
        for (DENOPTIMRing r : rToEdit)
        {
            r.removeVertex(vertex);
            if (r.getSize() < 3)
                ringsToRemove.add(r);
        }
        for (DENOPTIMRing r : ringsToRemove)
        {
            removeRing(r);
        }
        
        // Remove edges to/from old vertex, while keeping track of edits to do
        // in a hypothetical jacket template (if such template exists)
        LinkedHashMap<DENOPTIMAttachmentPoint,DENOPTIMAttachmentPoint> newInReplaceOldInInTmpl = 
                new LinkedHashMap<DENOPTIMAttachmentPoint,DENOPTIMAttachmentPoint>();
        List<DENOPTIMAttachmentPoint> oldAPToRemoveFromTmpl = 
                new ArrayList<DENOPTIMAttachmentPoint>();
        for (DENOPTIMAttachmentPoint oldAP : vertex.getAttachmentPoints())
        {
            if (oldAP.isAvailable())
            {
                // NB: if this graph is embedded in a template, free/available 
                // APs at this level (and sources at the vertex we are removing) 
                // are mapped on the jacket templates' surface, and must be 
                // removed.
                if (templateJacket!=null)
                {
                    if (!oldAP.isAvailableThroughout())
                    {
                        DENOPTIMAttachmentPoint lAP = 
                                oldAP.getLinkedAPThroughout();
                        if (bestScoringMapping.keySet().contains(lAP))
                        {
                            newInReplaceOldInInTmpl.put(
                                    bestScoringMapping.get(lAP), oldAP);
                        } else if (bestScoringMapping.values().contains(lAP))
                        {
                            newInReplaceOldInInTmpl.put(
                                    bestScoringMappingReverse.get(lAP), oldAP);
                        } else {
                            oldAPToRemoveFromTmpl.add(oldAP);
                        }
                    } else {
                        oldAPToRemoveFromTmpl.add(oldAP);
                    }
                }
            } else {
                removeEdge(oldAP.getEdgeUser());
            }
        }
        
        // Update pointers in symmetric sets in this graph level
        // NB: this deals only with the symmetric relations of the removed vertex
        // The symm. relations of other removed child vertexes are dealt with
        // when removing those vertexes.
        int oldVrtxId = vertex.getVertexId();
        ArrayList<SymmetricSet> ssToRemove = new ArrayList<SymmetricSet>();
        Iterator<SymmetricSet> ssIter = getSymSetsIterator();
        while (ssIter.hasNext())
        {
            SymmetricSet ss = ssIter.next();
            if (ss.contains(oldVrtxId))
            {
                ss.remove((Integer) oldVrtxId);
            }
            if (ss.size() < 2)
                ssToRemove.add(ss);
        }
        symVertices.removeAll(ssToRemove);
        
        // Remove the vertex
        getVertexList().remove(vertex);
        vertex.resetGraphOwner();
        
        // Add new edges (within the graph owning the removed vertex) 
        List<DENOPTIMAttachmentPoint> reconnettedApsOnChilds = 
                new ArrayList<DENOPTIMAttachmentPoint>();
        for (Entry<DENOPTIMAttachmentPoint,DENOPTIMAttachmentPoint> e : 
            bestScoringMapping.entrySet())
        {
            DENOPTIMAttachmentPoint apOnParent = e.getKey();
            DENOPTIMAttachmentPoint apOnChild = e.getValue();
            if (containsVertex(apOnChild.getOwner()) 
                    && containsVertex(apOnParent.getOwner()))
            {
                DENOPTIMEdge edge = new DENOPTIMEdge(apOnParent,apOnChild,
                        apOnParent.getAPClass().getBondType());
                addEdge(edge);
                reconnettedApsOnChilds.add(apOnChild);
            } else {
                if (templateJacket!=null)
                {
                    if (containsVertex(apOnParent.getOwner()))
                    {
                        templateJacket.updateInnerApID(
                                newInReplaceOldInInTmpl.get(apOnParent), //AP on old vertex
                                apOnParent);
                        reconnettedApsOnChilds.add(apOnChild);
                    }
                    if (containsVertex(apOnChild.getOwner()))
                    {
                        templateJacket.updateInnerApID(
                                newInReplaceOldInInTmpl.get(apOnChild), //AP on old vertex
                                apOnChild);
                        reconnettedApsOnChilds.add(apOnChild);
                    }
                    // The case where neither is contained in 'this' cannot
                    // occur because of the initial checks that identify 
                    // attempts to remove the only vertex inside a template.
                } else {
                    DENOPTIMException de = new DENOPTIMException("AP '"
                            + apOnChild + "' seems connected to a template, "
                            + "no template was found. Possible bug!");
                    throw de;
                }
            }
        }
        
        // update the mapping of this vertex's APs in the jacket template
        if (templateJacket!=null)
        {   
            // Remove all APs that existed only in the old vertex
            for (DENOPTIMAttachmentPoint apOnOld : oldAPToRemoveFromTmpl)
            {
                templateJacket.removeProjectionOfInnerAP(apOnOld);
            }
        }
        
        // Remove branches of child-APs that were not mapped/done
        for (DENOPTIMAttachmentPoint apOnChild : needyAPsOnChildren)
        {
            if (!reconnettedApsOnChilds.contains(apOnChild))
            {
                removeOrphanBranchStartingAt(apOnChild.getOwner());
            }
        }

        jGraph = null;
        
        return !this.containsVertex(vertex);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Marks the vertexes of this graph with a string that is consistent for all
     * vertexes that belong to symmetric sets. All vertexes will get a label,
     * whether they belong to a symmetric set or not. The label is places in the
     * vertex property {@link DENOPTIMConstants#VRTSYMMSETID}.
     * Any previous labeling is ignored and overwritten.
     */
    protected void reassignSymmetricLabels()
    {
        //Remove previous labeling
        for (DENOPTIMVertex v : gVertices)
            v.removeProperty(DENOPTIMConstants.VRTSYMMSETID);
        
        Iterator<SymmetricSet> ssIter = getSymSetsIterator();
        int i = 0;
        while (ssIter.hasNext())
        {
            SymmetricSet ss = ssIter.next();
            String symmLabel = ss.hashCode() + "-" + i;
            for (Integer vid : ss.getList())
            {
                getVertexWithId(vid).setProperty(DENOPTIMConstants.VRTSYMMSETID,
                        symmLabel);
            }
            i++;
        }
        for (DENOPTIMVertex v : gVertices)
        {
            i++;
            if (!v.hasProperty(DENOPTIMConstants.VRTSYMMSETID))
                v.setProperty(DENOPTIMConstants.VRTSYMMSETID, v.hashCode()+"-"+i);
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Looks for any symmetric labels, creates symmetric sets that collect the 
     * same information, and removes the original symmetric labels.
     */
    protected void convertSymmetricLabelsToSymmetricSets()
    {
        Map<String,List<DENOPTIMVertex>> collectedLabels = new HashMap<>();
        for (DENOPTIMVertex v : gVertices)
        {
            if (v.hasProperty(DENOPTIMConstants.VRTSYMMSETID))
            {
                String label = v.getProperty(
                        DENOPTIMConstants.VRTSYMMSETID).toString();
                if (collectedLabels.containsKey(label))
                {
                    collectedLabels.get(label).add(v);
                } else {
                    List<DENOPTIMVertex> lst = new ArrayList<DENOPTIMVertex>();
                    lst.add(v);
                    collectedLabels.put(label, lst);
                }
                v.removeProperty(DENOPTIMConstants.VRTSYMMSETID);
            }
        }
        
        for (String label : collectedLabels.keySet())
        {
            List<DENOPTIMVertex> symmVertexes = collectedLabels.get(label);
            
            if (symmVertexes.size()>1)
            {
                SymmetricSet ss = null;
                for (DENOPTIMVertex v : symmVertexes)
                {
                    if (hasSymmetryInvolvingVertex(v))
                    {
                        ss = getSymSetForVertex(v);
                        break;
                    }
                }
                
                if (ss != null)
                {
                    for (DENOPTIMVertex v : symmVertexes)
                        ss.add(v.getVertexId());
                    
                } else {
                    ss = new SymmetricSet();
                    for (DENOPTIMVertex v : symmVertexes)
                        ss.add(v.getVertexId());
                    try
                    {
                        addSymmetricSetOfVertices(ss);
                    } catch (DENOPTIMException e)
                    {
                        //this can never happen because we are testing for
                        // preliminary existence of an id in the existing sets.
                    }
                }
            }
        }
    }
        
//------------------------------------------------------------------------------
    
    /**
     * Replaced the subgraph represented by a given collection of vertexes that
     * belong to this graph. 
     * This method reproduces the change of vertex on all symmetric sites, where
     * a site is identified by a symmetrically identified reflection of the 
     * subgraph to replace, which are identified using symmetric sets.
     * @param subGrpVrtxs the vertexes currently belonging to this graph and to  
     * be replaced. We assume these collection of vertexes is a connected graph,
     * i.e., all vertexes are reachable by one single vertex via a directed path
     * that does not involve any other vertex not included in this collections.
     * @param newSubGraph the graph that will be used to create the new branch 
     * replacing the old one. Vertexes of such branch will be cloned to create
     * the new vertexes to be added to this graph.
     * @param apMap mapping of attachment points belonging to any vertex in
     * <code>subGrpVrtxs</code> to attachment points in <code>newSubGraph</code>.
     * @return <code>true</code> if the substitution is successful.
     * @throws DENOPTIMException is capping groups are the only vertexes in the
     * subgraph.
     */
    public boolean replaceSubGraph(List<DENOPTIMVertex> subGrpVrtxs, 
            DENOPTIMGraph incomingGraph, 
            LinkedHashMap<DENOPTIMAttachmentPoint,DENOPTIMAttachmentPoint> apMap) 
                    throws DENOPTIMException
    {
        for (DENOPTIMVertex vToRemove : subGrpVrtxs)
        {
            if (!gVertices.contains(vToRemove))
            {
                return false;
            }
        }
        
        // Capping groups are removed and, if needed, re-added back
        subGrpVrtxs.stream().filter(v -> v.getBuildingBlockType()==BBType.CAP)
            .forEach(v -> this.removeVertex(v));
        subGrpVrtxs.removeIf(v -> v.getBuildingBlockType()==BBType.CAP);
        if (subGrpVrtxs.size() == 0)
        {
            throw new DENOPTIMException("Capping groups cannot be the only "
                    + "vertex in a subgraph to replace.");   
        }
        
        // Refresh the symmetry set labels so that clones of the branch inherit
        // the same symmetry set labels.
        incomingGraph.reassignSymmetricLabels();
        
        GraphUtils.ensureVertexIDConsistency(this.getMaxVertexId());
        
        for (List<DENOPTIMVertex> vertexesToRemove : getSymmetricSubGraphs(
                subGrpVrtxs))
        {
            DENOPTIMGraph graphToAdd = incomingGraph.clone();
            graphToAdd.renumberGraphVertices();
            
            removeCappingGroupsFromChilds(vertexesToRemove);
            
            List<DENOPTIMVertex> vertexAddedToThis = 
                    new ArrayList<DENOPTIMVertex>(graphToAdd.gVertices);
            LinkedHashMap<DENOPTIMAttachmentPoint,DENOPTIMAttachmentPoint> 
                localApMap = new LinkedHashMap<DENOPTIMAttachmentPoint,
                DENOPTIMAttachmentPoint>();
            for (Map.Entry<DENOPTIMAttachmentPoint,DENOPTIMAttachmentPoint>  e 
                    : apMap.entrySet())
            {
                // WARNING! Assumption that subGrpVrtxs and vertexesToRemove
                // are sorted accordingly to symmetry, which should be the case.
                int vrtPosOnOld = subGrpVrtxs.indexOf(e.getKey().getOwner());
                int apPosOnOld = e.getKey().getIndexInOwner();
                DENOPTIMAttachmentPoint apOnOld = vertexesToRemove.get(
                        vrtPosOnOld).getAP(apPosOnOld); 
                
                int vrtPosOnNew = incomingGraph.indexOf(e.getValue().getOwner());
                int apPosOnNew = e.getValue().getIndexInOwner();
                DENOPTIMAttachmentPoint apOnNew = graphToAdd.getVertexAtPosition(
                        vrtPosOnNew).getAP(apPosOnNew); 
                localApMap.put(apOnOld,apOnNew);
            }
            
            if (!replaceSingleSubGraph(vertexesToRemove, graphToAdd, localApMap))
            {
                return false;
            }
            addCappingGroups(vertexAddedToThis);
        }
        convertSymmetricLabelsToSymmetricSets();
        return true;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * We assume that the subgraph is a continuously connected, directed graph.
     * By contract, the source of the symmetric subgraph's spanning tree is 
     * always the first vertex in each returned list. Also, note that symmetry
     * does not pertain capping groups, so capping groups are not expected to be
     * among the vertexes in the given list and will not be present in the 
     * resulting subgraphs.
     * @param subGrpVrtxs
     * @return a collection of subgraphs, each represented by a list of vertexes
     * belonging to it.
     * @throws DENOPTIMException if capping groups are present in the list.
     */
    public List<List<DENOPTIMVertex>> getSymmetricSubGraphs(
            List<DENOPTIMVertex> subGrpVrtxs) throws DENOPTIMException
    {
        if (subGrpVrtxs.stream().anyMatch(v -> v.getBuildingBlockType()==BBType.CAP))
            throw new DENOPTIMException("Capping groups must not be part of "
                    + "symmetric subgraphs");

        List<List<DENOPTIMVertex>> symSites = new ArrayList<List<DENOPTIMVertex>>();
        
        if (subGrpVrtxs.size()==1)
        {
            for (DENOPTIMVertex sv : getSymVertexesForVertex(subGrpVrtxs.get(0)))
            {
                ArrayList<DENOPTIMVertex> lst = new ArrayList<DENOPTIMVertex>();
                lst.add(sv);
                symSites.add(lst);
            }
            if (symSites.size()==0)
            {
                symSites.add(subGrpVrtxs);
            }
            return symSites;
        }
        
        // Identify the (sole) grand parent.
        List<DENOPTIMVertex> thoseWithoutParent = new ArrayList<DENOPTIMVertex>();
        for (DENOPTIMVertex v : subGrpVrtxs)
        {
            if (!subGrpVrtxs.contains(v.getParent()))
                thoseWithoutParent.add(v);
        }
        if (thoseWithoutParent.size()!=1)
        {
            throw new DENOPTIMException("SubGraph has more than one grand "
                    + "parent.");
        }
        DENOPTIMVertex sourceOfSubGraph = thoseWithoutParent.get(0);
        int numSymmetricSubGraphs = getSymVertexesForVertex(sourceOfSubGraph).size();
        if (numSymmetricSubGraphs==0)
        {
            symSites.add(subGrpVrtxs);
            return symSites;
        }
        
        // Identify the ends of the subgraph's spanning tree
        List<DENOPTIMVertex> thoseWithoutChildren = new ArrayList<DENOPTIMVertex>();
        for (DENOPTIMVertex v : subGrpVrtxs)
        {
            if (Collections.disjoint(v.getChilddren(),subGrpVrtxs))
                thoseWithoutChildren.add(v);
        }
        
        // We want to verify that all the ends of the subgraph's spanning tree
        // have the same number of symmetric partners. This, while collecting
        // all ends that are outside the subgraph and are symmetric to any of
        // the ends belonging to the subgraph. The first, in fact, are the ends
        // of the symmetric subgraphs.
        Set<DENOPTIMVertex> upperLimits = new HashSet<DENOPTIMVertex>();
        Set<DENOPTIMVertex> doneBySymmetry = new HashSet<DENOPTIMVertex>();
        for (DENOPTIMVertex upperLimit : thoseWithoutChildren)
        {
            // We need to understand how many symmetric vertexes are already
            // within the subgraph
            int numInSubGraphReplicas = 1;
            
            if (doneBySymmetry.contains(upperLimit))
                continue;
            
            // These are symmetric vertexes that do belong to the subgraph
            Set<DENOPTIMVertex> symmSitesOnBranch = new HashSet<DENOPTIMVertex>(
                    getSymVertexesForVertex(upperLimit));
            symmSitesOnBranch.retainAll(subGrpVrtxs);
            if (symmSitesOnBranch.size()>0)
            {
                numInSubGraphReplicas = symmSitesOnBranch.size();
                doneBySymmetry.addAll(symmSitesOnBranch);
            }
            
            List<DENOPTIMVertex> lst = getSymVertexesForVertex(upperLimit);
            if (lst.size() != numInSubGraphReplicas*numSymmetricSubGraphs)
            {
                // The subgraph is not symmetrically reproduced.
                symSites.add(subGrpVrtxs);
                return symSites;
            }
            upperLimits.addAll(lst);
        }
        
        for (DENOPTIMVertex symSources : getSymVertexesForVertex(sourceOfSubGraph))
        {
            List<DENOPTIMVertex> symSubGraph = new ArrayList<DENOPTIMVertex>();
            // The source of the symmetric subgraph is always the first!
            symSubGraph.add(symSources);
            getChildTreeLimited(symSources, symSubGraph, upperLimits);
            //NB: Capping groups are not supposed to be in the list.
            symSubGraph.removeIf(v -> v.getBuildingBlockType()==BBType.CAP);
            if (symSubGraph.size()!=subGrpVrtxs.size())
            {
                symSites = new ArrayList<List<DENOPTIMVertex>>();
                symSites.add(subGrpVrtxs);
                return symSites;
            }
            symSites.add(symSubGraph);
        }

        return symSites;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Replaced the subgraph represented by a given collection of vertexes that
     * belong to this graph. 
     * This method does not project the 
     * change of vertex on symmetric sites, and does not alter the symmetric 
     * sets. To properly manage symmetry, you should run 
     * {@link DENOPTIMGraph#reassignSymmetricLabels()} on <code>newSubGraph</code>
     * prior to calling this method, and, after running this method, call
     * {@link DENOPTIMGraph#convertSymmetricLabelsToSymmetricSets()} on this
     * graph.
     * This strategy reflects the fact that multiple sub-graph replacements can
     * introduce vertexes that are symmetric throughout these newly inserted
     * subgraphs, thus a single subgraph replacement cannot know the complete
     * list of symmetric vertexes. 
     * Therefore, the handling of the symmetry is left outside of the
     * subgraph replacement operation.
     * @param subGrpVrtxs the vertexes currently belonging to this graph and to be 
     * replaced. We assume these collection of vertexes is a connected subgraph,
     * i.e., all vertexes are reachable by one single vertex via a directed path
     * that does not involve any other vertex not included in this collections.
     * @param newSubGraph the graph that will be attached on this graph. 
     * No copying or cloning: such graph contains the actual vertexes that will 
     * become part of <i>this</i> graph.
     * @param apMap mapping of attachment points belonging to any vertex in
     * <code>subGrpVrtxs</code> to attachment points in <code>newSubGraph</code>.
     * @return <code>true</code> if the substitution is successful.
     * @throws DENOPTIMException
     */
    public boolean replaceSingleSubGraph(List<DENOPTIMVertex> subGrpVrtxs, 
            DENOPTIMGraph newSubGraph, 
            LinkedHashMap<DENOPTIMAttachmentPoint,DENOPTIMAttachmentPoint> apMap) 
                    throws DENOPTIMException
    {
        if (!gVertices.containsAll(subGrpVrtxs) 
                || gVertices.contains(newSubGraph.getVertexAtPosition(0)))
        {
            return false;
        }
        
        // Identify vertex that will be added
        ArrayList<DENOPTIMVertex> newVertexes = new ArrayList<DENOPTIMVertex>();
        newVertexes.addAll(newSubGraph.getVertexList());
        
        // Collect APs from the vertexes that will be removed, and that might
        // be reflected onto the jacket template or used to make links to the 
        // rest of the graph.
        List<DENOPTIMAttachmentPoint> interfaceApsOnOldBranch = 
                new ArrayList<DENOPTIMAttachmentPoint>();
        for (DENOPTIMVertex vToDel : subGrpVrtxs)
        {
            for (DENOPTIMAttachmentPoint ap : vToDel.getAttachmentPoints())
            {
                if (ap.isAvailable())
                {
                    // being available, this AP might be reflected onto jacket template
                    interfaceApsOnOldBranch.add(ap);
                } else {
                    DENOPTIMVertex user = ap.getLinkedAP().getOwner();
                    if (!subGrpVrtxs.contains(user))
                    {
                        interfaceApsOnOldBranch.add(ap);
                    }
                }
            }
        }
        List<DENOPTIMAttachmentPoint> interfaceApsOnNewBranch = 
                new ArrayList<DENOPTIMAttachmentPoint>();
        for (DENOPTIMVertex v : newSubGraph.getVertexList())
        {
            for (DENOPTIMAttachmentPoint ap : v.getAttachmentPoints())
            {
                if (ap.isAvailable())
                {
                    // being available, this AP will have to be reflected onto jacket template
                    interfaceApsOnNewBranch.add(ap);
                }
            }
        }
        
        // Keep track of the links that will be broken and re-created,
        // and also of the relation free APs may have with a possible template
        // that embeds this graph.
        LinkedHashMap<DENOPTIMAttachmentPoint,DENOPTIMAttachmentPoint> 
            linksToRecreate = new LinkedHashMap<>();
        LinkedHashMap<DENOPTIMAttachmentPoint,BondType> 
            linkTypesToRecreate = new LinkedHashMap<>();
        LinkedHashMap<DENOPTIMAttachmentPoint,DENOPTIMAttachmentPoint> 
            inToOutAPForTemplate = new LinkedHashMap<>();
        List<DENOPTIMAttachmentPoint> oldAPToRemoveFromTmpl = new ArrayList<>();
        DENOPTIMAttachmentPoint trgAPOnNewLink = null;
        for (DENOPTIMAttachmentPoint oldAP : interfaceApsOnOldBranch)
        {
            if (oldAP.isAvailable())
            {
                // NB: if this graph is embedded in a template, free/available 
                // APs at this level (and from the old link) 
                // are mapped on the templates' surface
                if (templateJacket!=null)
                {
                    if (oldAP.isAvailableThroughout())
                    {
                        if (!apMap.containsKey(oldAP))
                        {
                            // An AP of the old link is going to be removed from the
                            // template-jacket's list of APs
                            oldAPToRemoveFromTmpl.add(oldAP);
                        } else {
                            // This AP is not used, not even outside of the template
                            // but for some reason the apMapping wants to keep it
                            inToOutAPForTemplate.put(apMap.get(oldAP),oldAP);
                        }
                    } else {
                        if (!apMap.containsKey(oldAP))
                        {
                            throw new DENOPTIMException("Cannot replace subgraph "
                                    + "if a used AP has no mapping.");
                        } else {
                            // This AP is not used, not even outside of the template
                            // but for some reason the apMapping wants to keep it
                            inToOutAPForTemplate.put(apMap.get(oldAP),oldAP);
                        }
                    }
                }
                continue;
            }
            
            if (!apMap.containsKey(oldAP))
            {
                throw new DENOPTIMException("Cannot replace subgraph if a used "
                        + "AP has no mapping.");
            }
            DENOPTIMAttachmentPoint newAP = apMap.get(oldAP);
            linksToRecreate.put(newAP, oldAP.getLinkedAP());
            linkTypesToRecreate.put(newAP, oldAP.getEdgeUser().getBondType());
            
            // This is were we identify the edge/ap to the parent of the oldLink
            if (!oldAP.isSrcInUser())
            {
                trgAPOnNewLink = newAP;
            }
        }
        
        // Identify rings that are affected by the change of vertexes
        Map<DENOPTIMRing,List<DENOPTIMVertex>> ringsOverSubGraph = 
                new HashMap<DENOPTIMRing,List<DENOPTIMVertex>>();
        for (int iA=0; iA<interfaceApsOnOldBranch.size(); iA++)
        {
            DENOPTIMAttachmentPoint apA = interfaceApsOnOldBranch.get(iA);
        
            if (apA.isAvailable())
                continue;
            
            for (int iB=(iA+1); iB<interfaceApsOnOldBranch.size(); iB++)
            {
                DENOPTIMAttachmentPoint apB = interfaceApsOnOldBranch.get(iB);
            
                if (apB.isAvailable())
                    continue;
                
                DENOPTIMVertex vLinkedOnA = apA.getLinkedAP().getOwner();
                DENOPTIMVertex vLinkedOnB = apB.getLinkedAP().getOwner();
                for (DENOPTIMRing r : getRingsInvolvingVertex(
                        new DENOPTIMVertex[] {
                                apA.getOwner(), vLinkedOnA,
                                apB.getOwner(), vLinkedOnB}))
                {
                    List<DENOPTIMVertex> vPair = new ArrayList<DENOPTIMVertex>();
                    vPair.add(r.getCloserToHead(vLinkedOnA, vLinkedOnB));
                    vPair.add(r.getCloserToTail(vLinkedOnA, vLinkedOnB));
                    ringsOverSubGraph.put(r, vPair);
                }
            }
        }
        
        // remove the vertex-to-delete from the rings where they participate
        for (DENOPTIMRing r : ringsOverSubGraph.keySet())
        {
            List<DENOPTIMVertex> vPair = ringsOverSubGraph.get(r);
            PathSubGraph path = new PathSubGraph(vPair.get(0),vPair.get(1),this);
            List<DENOPTIMVertex> vertexesInPath = path.getVertecesPath();
            for (int i=1; i<(vertexesInPath.size()-1); i++)
            {
                r.removeVertex(vertexesInPath.get(i));
            }
        }
        
        // remove edges with old vertex
        for (DENOPTIMAttachmentPoint oldAP : interfaceApsOnOldBranch)
        {
            if (!oldAP.isAvailable())
                removeEdge(oldAP.getEdgeUser());
        }

        // remove the vertex from the graph
        for (DENOPTIMVertex vToDel : subGrpVrtxs)
        {
            // WARNING! This removes rings involving these vertexes. 
            removeVertex(vToDel);
        }
        
        // finally introduce the new vertexes from incoming graph into this graph
        for (DENOPTIMVertex incomingVrtx : newSubGraph.getVertexList())
        {
            addVertex(incomingVrtx);
        }
        
        // import edges from incoming graph 
        for (DENOPTIMEdge incomingEdge : newSubGraph.getEdgeList())
        {
            addEdge(incomingEdge);
        }
        
        // import rings from incoming graph
        for (DENOPTIMRing incomingRing : newSubGraph.getRings())
        {
            addRing(incomingRing);
        }
        
        // import symmetric sets from incoming graph? No, this method doesn't do
        // it because we want to use it in situations where we have to perform 
        // multiple replaceSubGraph operations and, afterwards, use the 
        // symmetric labels to create symmetric sets that might span across
        // more than one of the subgraphs that were added.
        
        // We keep track of the APs on the new link that have been dealt with
        List<DENOPTIMAttachmentPoint> doneApsOnNew = 
                new ArrayList<DENOPTIMAttachmentPoint>();
        
        // Connect the incoming subgraph to the rest of the graph
        if (trgAPOnNewLink != null)
        {
            // the incoming graph has a parent vertex, and the edge should be  
            // directed accordingly
            DENOPTIMEdge edge = new DENOPTIMEdge(
                    linksToRecreate.get(trgAPOnNewLink),
                    trgAPOnNewLink, 
                    linkTypesToRecreate.get(trgAPOnNewLink));
            addEdge(edge);
            doneApsOnNew.add(trgAPOnNewLink);
        } else {
            // newLink does NOT have a parent vertex, so all the
            // edges see newLink as target vertex. Such links are dealt with
            // in the loop below. So, there is nothing special to do, here.
        }
        for (DENOPTIMAttachmentPoint apOnNew : linksToRecreate.keySet())
        {
            if (apOnNew == trgAPOnNewLink)
            {
                continue; //done just before this loop
            }
            DENOPTIMAttachmentPoint trgOnChild = linksToRecreate.get(apOnNew);
            DENOPTIMEdge edge = new DENOPTIMEdge(apOnNew,trgOnChild, 
                    linkTypesToRecreate.get(apOnNew));
            addEdge(edge);
            doneApsOnNew.add(apOnNew);
        }
        
        // redefine rings that spanned over the removed subgraph
        for (DENOPTIMRing r : ringsOverSubGraph.keySet())
        {
            List<DENOPTIMVertex> vPair = ringsOverSubGraph.get(r);
            PathSubGraph path = new PathSubGraph(vPair.get(0),vPair.get(1),this);
            int initialInsertPoint = r.getPositionOf(vPair.get(0));
            List<DENOPTIMVertex> vertexesInPath = path.getVertecesPath();
            for (int i=1; i<(vertexesInPath.size()-1); i++)
            {
                r.insertVertex(initialInsertPoint+i, vertexesInPath.get(i));
            }
        }
        
        // update the mapping of this vertexes' APs in the jacket template
        if (templateJacket != null)
        {   
            for (DENOPTIMAttachmentPoint apOnNew : inToOutAPForTemplate.keySet())
            {
                templateJacket.updateInnerApID(
                        inToOutAPForTemplate.get(apOnNew),apOnNew);
                doneApsOnNew.add(apOnNew);
            }
            
            // Project all remaining APs of new branch on the surface of template
            for (DENOPTIMAttachmentPoint apOnNew : interfaceApsOnNewBranch)
            {
                if (!doneApsOnNew.contains(apOnNew))
                {
                    templateJacket.addInnerToOuterAPMapping(apOnNew);
                }
            }
            
            // Remove all APs that existed only in the old branch
            for (DENOPTIMAttachmentPoint apOnOld : oldAPToRemoveFromTmpl)
            {
                templateJacket.removeProjectionOfInnerAP(apOnOld);
            }
        }
        
        jGraph = null;
        
        for (DENOPTIMVertex vOld : subGrpVrtxs)
            if (this.containsVertex(vOld))
                return false;
        for (DENOPTIMVertex vNew : newVertexes)
            if (!this.containsVertex(vNew))
                return false;
        return true;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Replaced a given vertex belonging to this graph with a new vertex 
     * generated specifically for this purpose. This method reproduces the 
     * change of vertex on all symmetric sites.
     * @param vertex the vertex currently belonging to this graph and to be 
     * replaced.
     * @param bbId the building block Id of the building blocks that will 
     * replace the original vertex.
     * @param bbt the type of building block to be used to replace the 
     * original vertex.
     * @param apMap the mapping of attachment points needed to install the new
     * vertex in the slot of the old one and recreate the edges to the rest of
     * the graph.
     * @return <code>true</code> if the substitution is successful.
     * @throws DENOPTIMException
     */
    public boolean replaceVertex(DENOPTIMVertex vertex, int bbId, BBType bbt,
            LinkedHashMap<Integer, Integer> apIdMap) throws DENOPTIMException
    {
        if (!gVertices.contains(vertex))
        {
            return false;
        }
        
        ArrayList<DENOPTIMVertex> symSites = getSymVertexesForVertex(vertex);
        if (symSites.size() == 0)
        {
            symSites.add(vertex);
        }
        
        GraphUtils.ensureVertexIDConsistency(this.getMaxVertexId());
        DENOPTIMVertex newLink = DENOPTIMVertex.newVertexFromLibrary(
                GraphUtils.getUniqueVertexIndex(), bbId, bbt);
        DENOPTIMGraph incomingGraph = new DENOPTIMGraph();
        incomingGraph.addVertex(newLink);
        incomingGraph.reassignSymmetricLabels();
        
        for (DENOPTIMVertex oldLink : symSites)
        {
            DENOPTIMGraph graphAdded = incomingGraph.clone();
            graphAdded.getVertexAtPosition(0).setMutationTypes(
                    oldLink.getUnfilteredMutationTypes());
            graphAdded.renumberGraphVertices();
            
            ArrayList<DENOPTIMVertex> oldVertex = new ArrayList<DENOPTIMVertex>();
            oldVertex.add(oldLink);
            LinkedHashMap<DENOPTIMAttachmentPoint,DENOPTIMAttachmentPoint> apMap =
                    new LinkedHashMap<DENOPTIMAttachmentPoint,DENOPTIMAttachmentPoint>();
            for (Map.Entry<Integer,Integer> e : apIdMap.entrySet())
            {
                apMap.put(oldLink.getAP(e.getKey()), 
                        graphAdded.getVertexAtPosition(0).getAP(e.getValue()));
            }
            
            if (!replaceSingleSubGraph(oldVertex, graphAdded, apMap))
            {
                return false;
            }
        }
        convertSymmetricLabelsToSymmetricSets();
        return true;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Inserts a given vertex in between two vertexes connected by the given
     * edge. This method reproduces the 
     * change of vertex on all symmetric sites.
     * @param edge the edge where to insert the new vertex.
     * @param bbId the building block Id of the building blocks that will 
     * be inserted.
     * @param bbt the type of building block to be inserted.
     * @param apMap the mapping of attachment points needed to install the new
     * building block and connect it to the rest of the graph. 
     * The syntax of this map must be:
     * <ul>
     * <li>keys: the APs originally involved in making the edge given as
     * parameter,</li>
     * <li>values: the 0-based index of the AP in the new building block that 
     * will be inserted.</li>
     * </ul>
     * @return <code>true</code> if the substitution is successful.
     * @throws DENOPTIMException
     */
    
    //NB: LinkedHashMap is used to retain reproducibility between runs.
    
    public boolean insertVertex(DENOPTIMEdge edge, int bbId, BBType bbt,
            LinkedHashMap<DENOPTIMAttachmentPoint,Integer> apMap) 
                    throws DENOPTIMException
    {
        if (!gEdges.contains(edge))
        {
            return false;
        }
        
        ArrayList<DENOPTIMEdge> symSites = new ArrayList<DENOPTIMEdge> ();
        ArrayList<LinkedHashMap<DENOPTIMAttachmentPoint,Integer>> symApMaps = 
                new ArrayList<LinkedHashMap<DENOPTIMAttachmentPoint,Integer>>();
        ArrayList<DENOPTIMVertex> symTrgVertexes = getSymVertexesForVertex(
                edge.getTrgAP().getOwner());
        if (symTrgVertexes.size() == 0)
        {
            symSites.add(edge);
            symApMaps.add(apMap);
        } else {
            for (DENOPTIMVertex trgVrtx : symTrgVertexes)
            {
                DENOPTIMEdge symEdge = trgVrtx.getEdgeToParent();
                symSites.add(symEdge);
                
                LinkedHashMap<DENOPTIMAttachmentPoint,Integer> locApMap = new
                        LinkedHashMap<DENOPTIMAttachmentPoint,Integer>();
                locApMap.put(symEdge.getSrcAP(), apMap.get(edge.getSrcAP()));
                locApMap.put(symEdge.getTrgAP(), apMap.get(edge.getTrgAP()));
                symApMaps.add(locApMap);
            }
        }
        
        SymmetricSet newSS = new SymmetricSet();
        for (int i=0; i<symSites.size(); i++)
        {
            DENOPTIMEdge symEdge = symSites.get(i);
            LinkedHashMap<DENOPTIMAttachmentPoint,Integer> locApMap = symApMaps.get(i);
            
            GraphUtils.ensureVertexIDConsistency(this.getMaxVertexId());
            DENOPTIMVertex newLink = DENOPTIMVertex.newVertexFromLibrary(
                    GraphUtils.getUniqueVertexIndex(), bbId, bbt);
            newSS.add(newLink.getVertexId());
            LinkedHashMap<DENOPTIMAttachmentPoint,DENOPTIMAttachmentPoint> apToApMap =
                    new LinkedHashMap<DENOPTIMAttachmentPoint,DENOPTIMAttachmentPoint>();
            for (DENOPTIMAttachmentPoint apOnGraph : locApMap.keySet())
            {
                apToApMap.put(apOnGraph, newLink.getAP(locApMap.get(apOnGraph)));
            }
            if (!insertSingleVertex(symEdge, newLink, apToApMap))
            {
                return false;
            }
        }
        if (newSS.size()>1)
        {
            addSymmetricSetOfVertices(newSS);
        }
        return true;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Inserts a given vertex in between two vertexes connected by the given
     * edge. This method reproduces the 
     * change of vertex on all symmetric sites.
     * @param edge the edge where to insert the new vertex.
     * @param newLink the new vertex to be inserted.
     * @param apMap the mapping of attachment points needed to install the new
     * vertex and connect it to the rest of the graph. The syntax of this map 
     * must be:
     * <ul>
     * <li>keys: the APs originally involved in making the edge given as
     * parameter,</li>
     * <li>values: the APs in the new vertex.</li>
     * </ul>
     * @return <code>true</code> if the substitution is successful.
     * @throws DENOPTIMException
     */
    public boolean insertSingleVertex(DENOPTIMEdge edge, DENOPTIMVertex newLink,
            LinkedHashMap<DENOPTIMAttachmentPoint,DENOPTIMAttachmentPoint> apMap) 
                    throws DENOPTIMException
    {
        //TODO: for reproducibility the AP mapping should become an optional
        // parameter: if given we try to use it, if not given we GraphLinkFinder
        // will try to find a suitable mapping.
        
        if (!gEdges.contains(edge) || gVertices.contains(newLink))
        {
            return false;
        }
        
        // First keep track of the links that will be broken and re-created,
        // and also of the relation free APs may have with a possible template
        // that embeds this graph.
        DENOPTIMAttachmentPoint orisEdgeSrc = edge.getSrcAP();
        DENOPTIMAttachmentPoint orisEdgeTrg = edge.getTrgAP();
        DENOPTIMVertex srcVrtx = orisEdgeSrc.getOwner();
        DENOPTIMVertex trgVrtx = orisEdgeTrg.getOwner();
        
        removeEdge(edge);
        
        // Introduce the new vertex in the graph
        addVertex(newLink);
        
        // Connect the new vertex to the graph
        DENOPTIMEdge eSrcToLink = new DENOPTIMEdge(orisEdgeSrc,
                apMap.get(orisEdgeSrc), edge.getBondType());
        addEdge(eSrcToLink);
        DENOPTIMEdge eLinkToTrg = new DENOPTIMEdge(apMap.get(orisEdgeTrg),
                orisEdgeTrg, edge.getBondType());
        addEdge(eLinkToTrg);
        
        // update any affected ring
        if (isVertexInRing(srcVrtx) && isVertexInRing(trgVrtx))
        {
            ArrayList<DENOPTIMRing> rToEdit = new ArrayList<DENOPTIMRing>();
            rToEdit.addAll(getRingsInvolvingVertex(srcVrtx));
            rToEdit.retainAll(getRingsInvolvingVertex(trgVrtx));
            for (DENOPTIMRing r : rToEdit)
            {
                r.insertVertex(newLink,srcVrtx,trgVrtx);
            }
        }
    
        // NB: if this graph is embedded in a template, new free/available 
        // APs introduced with the new link need to be mapped on the surface of
        // the template
        if (templateJacket != null)
        {
            for (DENOPTIMAttachmentPoint ap : newLink.getAttachmentPoints())
            {
                if (ap.isAvailable())
                {
                    templateJacket.addInnerToOuterAPMapping(ap);
                }
            }
        } 
        
        jGraph = null;
        
        return !gEdges.contains(edge) && this.containsVertex(newLink);
    }

//------------------------------------------------------------------------------

    /**
     * Returns the vertex that is in the given position of the list of vertexes 
     * belonging to this graph.
     * @param pos the position in the list.
     * @return the vertex in the given position.
     */
    public DENOPTIMVertex getVertexAtPosition(int pos)
    {
        return ((pos >= gVertices.size()) || pos < 0) ? null :
                gVertices.get(pos);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Check if the specified vertex is contained in this graph as a node or
     * in any inner graphs that may be embedded in {@link DENOPTIMTemplate}-kind
     * vertex belonging to this graph. 
     * @param v the vertex.
     * @return <code>true</code> if the vertex belongs to this graph or is 
     * anyhow embedded in it.
     */
    public boolean containsOrEmbedsVertex(DENOPTIMVertex v)
    {
        if (gVertices.contains(v))
            return true;
           
        for (DENOPTIMVertex vrt : gVertices)
        {
            if (vrt instanceof DENOPTIMTemplate)
            {
                DENOPTIMTemplate t = (DENOPTIMTemplate) vrt;
                if (t.getInnerGraph().containsOrEmbedsVertex(v))
                    return true;
            }
        }
        return false;
    }
    

//------------------------------------------------------------------------------

    /**
     * Check if this graph contains the specified vertex. Does not consider 
     * inner graphs that may be embedded in {@link DENOPTIMTemplate}-kind
     * vertex belonging to this graph. 
     * @param v the vertex.
     * @return <code>true</code> if the vertex belongs to this graph.
     */
    public boolean containsVertex(DENOPTIMVertex v)
    {
        return gVertices.contains(v);
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the index of a vertex in the list of vertices of this graph.
     * @param v the vertex to search
     * @return the index of the vertex in the list of vertices.
     */
    public int indexOf(DENOPTIMVertex v)
    {
        return gVertices.indexOf(v);
    }

//------------------------------------------------------------------------------

    public DENOPTIMVertex getVertexWithId(int vid)
    {
        DENOPTIMVertex v = null;
        int idx = indexOfVertexWithID(vid);
        if (idx != -1)
            v = gVertices.get(idx);
        return v;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the position of the first vertex that has the given ID.
     * @param vid the vertedID of the vertex we are looking for.
     * @return the index in the list of vertexes.
     */
    public int indexOfVertexWithID(int vid)
    {
        int idx = -1;
        for (int i=0; i<gVertices.size(); i++)
        {
            DENOPTIMVertex v = gVertices.get(i);
            if (v.getVertexId() == vid)
            {
                idx = i;
                break;
            }
        }
        return idx;
    }

//------------------------------------------------------------------------------

    /**
     * Removes an edge and update the free valences of the attachment points
     * that were originally involved in this edge
     * @param edge
     */
    public void removeEdge(DENOPTIMEdge edge)
    {
        if (gEdges.contains(edge))
        {
            DENOPTIMAttachmentPoint srcAP = edge.getSrcAP();
            DENOPTIMAttachmentPoint trgAP = edge.getTrgAP();
            srcAP.setUser(null);
            trgAP.setUser(null);

            gEdges.remove(edge);
        }
        jGraph = null;
    }

//------------------------------------------------------------------------------

    public void removeRing(DENOPTIMRing ring)
    {
        if (gRings.contains(ring))
        {
            gRings.remove(ring);
        }
        jGraph = null;
    }

//------------------------------------------------------------------------------

    public DENOPTIMEdge getEdgeAtPosition(int pos)
    {
        if ((pos >= gEdges.size()) || pos < 0)
            return null;
        return gEdges.get(pos);
    }

//------------------------------------------------------------------------------

    public int getEdgeCount()
    {
        return gEdges.size();
    }

//------------------------------------------------------------------------------

    public int getRingCount()
    {
        return gRings.size();
    }

//------------------------------------------------------------------------------

    public int getVertexCount()
    {
        return gVertices.size();
    }

//------------------------------------------------------------------------------

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(512);

        sb.append(graphId).append(" ");

        for (int i=0; i<gVertices.size(); i++)
        {
            sb.append(gVertices.get(i).toString()).append(",");
        }

        sb.append(" ");

        for (int i=0; i<gEdges.size(); i++)
        {
            sb.append(gEdges.get(i).toString()).append(",");
        }

        sb.append(" ");

        for (int i=0; i<gRings.size(); i++)
        {
            sb.append(gRings.get(i).toString()).append(" ");
        }

        for (int i=0; i<symVertices.size(); i++)
        {
            sb.append(symVertices.get(i).toString()).append(" ");
        }

        return sb.toString();
    }

//------------------------------------------------------------------------------

    /**
     *
     * @param srcVert
     * @param dapidx the AP corresponding to the source fragment
     * @param dstVert
     * @return the AP index of the destination fragment
     * @deprecated this depends on vertedID rather than reference
     */

    @Deprecated
    public int getBondingAPIndex(DENOPTIMVertex srcVert, int dapidx,
                                    DENOPTIMVertex dstVert)
    {
        int n = getEdgeCount();
        for (int i = 0; i < n; i++)
        {
            DENOPTIMEdge edge = getEdgeList().get(i);

            // get the vertex ids
            int v1_id = edge.getSrcVertex();
            int v2_id = edge.getTrgVertex();

            int dap_idx_v1 = edge.getSrcAPID();

            if (srcVert.getVertexId() == v1_id && v2_id == dstVert.getVertexId()
                                && dap_idx_v1 == dapidx)
            {
                return edge.getTrgAPID();
            }
        }

        return -1;
    }

//------------------------------------------------------------------------------

    /**
     * @param vertex the vertex for which the first level of child vertices
     * need to be found.
     * @return list of child vertices.
     */

    public ArrayList<DENOPTIMVertex> getChildVertices(DENOPTIMVertex vertex)
    {
        return vertex.getChilddren();
    }

//------------------------------------------------------------------------------

    /**
     * Gets all the children of the current vertex recursively. 
     * This method does not cross template 
     * boundaries, thus all children belong to the same graph.
     * @param vertex the vertex whose children are to be located
     * @param children list containing the references to all the children
     */
    public void getChildrenTree(DENOPTIMVertex vertex,
            List<DENOPTIMVertex> children) 
    {
        List<DENOPTIMVertex> lst = getChildVertices(vertex);
        if (lst.isEmpty()) 
        {
            return;
        }
        for (DENOPTIMVertex child : lst) 
        {
            if (!children.contains(child)) 
            {
                children.add(child);
                getChildrenTree(child, children);
            }
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Gets all the children of the current vertex recursively. 
     * This method does not cross template 
     * boundaries, thus all children belong to the same graph.
     * @param vertex the vertex whose children are to be located
     * @param children list containing the references to all the children
     * @param numLayers the maximum number of vertex layers after the seen 
     * vertex that we want to consider before stopping. If this value is 2, we 
     * will explore three layers: the seed, and two more layers away from it.
     * @param stopBeforeRCVs set <code>true</code> to make the exploration of
     * each branch stop before including ring closing vertexes.
     */
    public void getChildrenTree(DENOPTIMVertex vertex,
            List<DENOPTIMVertex> children, int numLayers, boolean stopBeforeRCVs) 
    {
        if (numLayers==0)
        {
            return;
        }
        List<DENOPTIMVertex> lst = getChildVertices(vertex);
        if (lst.isEmpty()) 
        {
            return;
        }
        for (DENOPTIMVertex child : lst) 
        {
            if (children.contains(child)) 
                continue;
            
            if (stopBeforeRCVs && child.isRCV())
                continue;
                
            children.add(child);
            getChildrenTree(child, children, numLayers-1, stopBeforeRCVs);
        }
    }
    
    
//------------------------------------------------------------------------------

    /**
     * Gets all the children of the current vertex recursively.
     * This method does not cross template 
     * boundaries, thus all children belong to the same graph.
     * @param vertex the vertex whose children are to be located
     * @param children list containing the references to all the children
     * @param stopBeforeRCVs set <code>true</code> to make the exploration of
     * each branch stop before including ring closing vertexes.
     */
    public void getChildTreeLimited(DENOPTIMVertex vertex,
            List<DENOPTIMVertex> children, boolean stopBeforeRCVs) 
    {
        List<DENOPTIMVertex> lst = getChildVertices(vertex);
        if (lst.isEmpty()) 
        {
            return;
        }
        for (DENOPTIMVertex child : lst) 
        {   
            if (children.contains(child)) 
                continue;
              
            if (stopBeforeRCVs && child.isRCV())
                continue;
                  
            children.add(child);
            getChildTreeLimited(child, children, stopBeforeRCVs);
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Gets all the children of the current vertex recursively until it 
     * finds one of the vertexes listed as limit.
     * This method does not cross template 
     * boundaries, thus all children belong to the same graph.
     * @param vertex the vertex whose children are to be located
     * @param children list containing the references to all the children
     * @param limits the list of vertexes where exploration should stop.
     */
    public void getChildTreeLimited(DENOPTIMVertex vertex,
            List<DENOPTIMVertex> children, Set<DENOPTIMVertex> limits)
    {
        List<DENOPTIMVertex> lst = getChildVertices(vertex);
        if (lst.isEmpty()) 
        {
            return;
        }
        for (DENOPTIMVertex child : lst) 
        {
            if (!children.contains(child)) 
            {
                children.add(child);
                if (!limits.contains(child))
                    getChildTreeLimited(child, children, limits);
            }
        }
    }
   
//------------------------------------------------------------------------------

    /**
     * Gets all the children of the current vertex recursively until it 
     * finds one of the vertexes listed as limit.
     * This method does not cross template 
     * boundaries, thus all children belong to the same graph.
     * @param vertex the vertex whose children are to be located
     * @param children list containing the references to all the children
     * @param limitsInClone the list of vertexes where exploration should stop.
     * @param stopBeforeRCVs set <code>true</code> to make the exploration of
     * each branch stop before including ring closing vertexes.
     */
    public void getChildTreeLimited(DENOPTIMVertex vertex,
            List<DENOPTIMVertex> children, List<DENOPTIMVertex> limitsInClone, 
            boolean stopBeforeRCVs) 
    {
        List<DENOPTIMVertex> lst = getChildVertices(vertex);
        if (lst.isEmpty()) 
        {
            return;
        }
        for (DENOPTIMVertex child : lst) 
        {   
            if (children.contains(child)) 
                continue;
            
            if (stopBeforeRCVs && child.isRCV())
                continue;
                
            children.add(child);
            if (!limitsInClone.contains(child))
                getChildTreeLimited(child, children, limitsInClone, stopBeforeRCVs);
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Identify the oldest ancestor (i.e., most great grandparent) in the given
     * collection. In case of two vertexes being at the same level, this returns
     * the first.
     * @return the vertex that has the shortest path to the source.
     */
    public DENOPTIMVertex getDeepestAmongThese(List<DENOPTIMVertex> list)
    {
        DENOPTIMVertex deepest = null;
        int shortest = Integer.MAX_VALUE;
        for (DENOPTIMVertex vertex : list)
        {
            List<DENOPTIMVertex> parentTree = new ArrayList<DENOPTIMVertex>();
            getParentTree(vertex, parentTree);
            if (parentTree.size()<shortest)
            {
                shortest = parentTree.size();
                deepest = vertex;
            }
        }
        return deepest;
    }
    
//------------------------------------------------------------------------------

    /**
     * Traverse the graph until it identifies the source of the directed path
     * reachable from the given vertex recursively.
     * @param vertex the child vertex from which we start traversing the graph.
     * @param parentTree list containing the references to all the parents
     * recursively visited so far.
     */
    public void getParentTree(DENOPTIMVertex vertex,
            List<DENOPTIMVertex> parentTree) 
    {
        DENOPTIMVertex parent = getParent(vertex);
        if (parent == null) 
        {
            return;
        }
        if (parentTree.contains(parent))
        {
            // Cyclic graphs are not allowed!
            throw new IllegalArgumentException();
        }
        parentTree.add(parent);
        getParentTree(parent, parentTree);
    }
    
//------------------------------------------------------------------------------

    public DENOPTIMVertex getParent(DENOPTIMVertex v)
    {
        DENOPTIMEdge edge = v.getEdgeToParent();
        if (edge != null)
        {
            return edge.getSrcAP().getOwner();
        }
        return null;
    }

//------------------------------------------------------------------------------

    /**
     * @param vid the vertex id for which the child vertices need to be found
     * @return Arraylist containing the vertex ids of the child vertices
     * @deprecated depends on vertedID
     */

    @Deprecated
    public ArrayList<Integer> getChildVertices(int vid)
    {
        ArrayList<Integer> lst = new ArrayList<>();
        DENOPTIMVertex v = getVertexWithId(vid);
        for (DENOPTIMAttachmentPoint ap : v.getAttachmentPoints())
        {
            DENOPTIMEdge e = ap.getEdgeUser();
            if (e != null && e.getTrgVertex()!=vid)
            {
                lst.add(e.getTrgVertex());
            }
        }
        return lst;
    }

//------------------------------------------------------------------------------

    /**
     * Returns almost "deep-copy" of this graph. Only the APCLass members of
     * member of this class should remain references to the original APClasses.
     * The vertex IDs are not changed, so you might want to renumber the graph.
     */
    @Override
    public DENOPTIMGraph clone()
    {   
        // When cloning, the VertexID remains the same so we'll have two
        // deep-copies of the same vertex having the same VertexID
        ArrayList<DENOPTIMVertex> cListVrtx = new ArrayList<>();
        Map<Integer,DENOPTIMVertex> vidsInClone =
                new HashMap<Integer,DENOPTIMVertex>();
        for (DENOPTIMVertex vOrig : gVertices)
        {
            DENOPTIMVertex vClone = vOrig.clone();
            cListVrtx.add(vClone);
            vidsInClone.put(vClone.getVertexId(),vClone);
        }

        ArrayList<DENOPTIMEdge> cListEdges = new ArrayList<>();
        for (DENOPTIMEdge e : gEdges)
        {
            int srcVrtxId = e.getSrcVertex();
            int srcApId = this.getVertexWithId(srcVrtxId).getIndexOfAP(
                    e.getSrcAP());

            int trgVrtxId = e.getTrgVertex();
            int trgApId = this.getVertexWithId(trgVrtxId).getIndexOfAP(
                    e.getTrgAP());

            DENOPTIMAttachmentPoint srcAPClone = vidsInClone.get(
                    srcVrtxId).getAP(srcApId);
            DENOPTIMAttachmentPoint trgAPClone = vidsInClone.get(
                    trgVrtxId).getAP(trgApId);

            cListEdges.add(new DENOPTIMEdge(srcAPClone, trgAPClone,
                    e.getBondType()));
        }

        DENOPTIMGraph clone = new DENOPTIMGraph(cListVrtx, cListEdges);

        // Copy the list but using the references to the cloned vertices
        ArrayList<DENOPTIMRing> cListRings = new ArrayList<>();
        for (DENOPTIMRing ring : gRings)
        {
            DENOPTIMRing cRing = new DENOPTIMRing();
            for (int iv=0; iv<ring.getSize(); iv++)
            {
                DENOPTIMVertex origVrtx = ring.getVertexAtPosition(iv);
                cRing.addVertex(
                        clone.getVertexWithId(origVrtx.getVertexId()));
            }
            cRing.setBondType(ring.getBondType());
            cListRings.add(cRing);
        }
        clone.setRings(cListRings);

        // The chainLinks are made of primitives, so it's just fine
        ArrayList<ClosableChain> cListClosableChains =
                new ArrayList<>();
        for (ClosableChain cc : closableChains)
        {
            cListClosableChains.add(cc.clone());
        }
        clone.setCandidateClosableChains(cListClosableChains);

        // Each "set" is a list of Integer, but SymmetricSet.clone takes care
        ArrayList<SymmetricSet> cSymVertices = new ArrayList<>();
        for (SymmetricSet ss : symVertices)
        {
            cSymVertices.add(ss.clone());
        }
        clone.setSymmetricVertexSets(cSymVertices);

        clone.setGraphId(graphId);
        clone.setLocalMsg(localMsg);

        return clone;
    }
 
//------------------------------------------------------------------------------

    /**
     * Looks for an edge that points to a vertex with the given vertex id.
     * @param vid
     * @return the edge whose target vertex has ID same as vid, or null
     */

    public DENOPTIMEdge getEdgeWithParent(int vid)
    {
        DENOPTIMVertex v = getVertexWithId(vid);
        if (v == null)
        {
            return null;
        }
        return v.getEdgeToParent();
    }

//------------------------------------------------------------------------------

    /**
     *
     * @param vid
     * @return the indices of all edges whose source vertex is same as vid
     */

    public ArrayList<Integer> getIndexOfEdgesWithChild(int vid)
    {
        ArrayList<Integer> lstEdges = new ArrayList<>();
        for (int j=0; j<getEdgeCount(); j++)
        {
            DENOPTIMEdge edge = getEdgeAtPosition(j);

            if (edge.getSrcVertex() == vid)
            {
                lstEdges.add(j);
            }
        }
        return lstEdges;
    }

//------------------------------------------------------------------------------

    /**
     *
     * @param vid
     * @return the edge whose source vertex is same as vid
     */

    public ArrayList<DENOPTIMEdge> getEdgesWithChild(int vid)
    {
        ArrayList<DENOPTIMEdge> lstEdges = new ArrayList<>();
        for (int j=0; j<getEdgeCount(); j++)
        {
            DENOPTIMEdge edge = getEdgeAtPosition(j);

            if (edge.getSrcVertex() == vid)
            {
                lstEdges.add(edge);
            }
        }
        return lstEdges;
    }

//------------------------------------------------------------------------------

    /**
     * @return the maximum value of vertex Id found in this graph.
     */
    public int getMaxVertexId()
    {
        int mval = Integer.MIN_VALUE;
        for (DENOPTIMVertex v : gVertices) {
            mval = Math.max(mval, v.getVertexId());
        }
        return mval;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Checks if a number is already used as VertexIDs within the graph.
     * @return <code>true</code> if the number is already used.
     */
    public boolean containsVertexID(int id)
    {
        boolean result = false;
        for (DENOPTIMVertex v : gVertices) 
        {
            if (id == v.getVertexId())
            {
                result = true;
                break;
            }
        }
        return result;
    }

//------------------------------------------------------------------------------

    /**
     * Wipes the data in this graph
     */
    public void cleanup()
    {
        if (gVertices != null)
        {
            gVertices.clear();
        }
        if (gEdges != null)
        {
            gEdges.clear();
        }
        if (gRings != null)
        {
            gRings.clear();
        }
        if (symVertices != null)
        {
            symVertices.clear();
        }
        if (closableChains != null)
        {
            closableChains.clear();
        }
        jGraph = null;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Checks if this graph is "DENOPTIM-isomorphic" to the other one given. 
     * "DENOPTIM-isomorphic" is a DENOPTIM-specific definition of  
     * <a href="https://mathworld.wolfram.com/IsomorphicGraphs.html">
     * graph isomorphism</a>
     * that differs from the most common meaning of isomorphism in graph theory.
     * In general, DENOPTIMGraphs are considered undirected when evaluating
     * DENOPTIM-isomorphism. 
     * Next, since a DENOPTIMGraph is effectively 
     * a spanning tree (ST_i={{vertexes}, {acyclic edges}}) 
     * with a set of fundamental cycles (FC_i={C_1, C_2,...C_n}), 
     * any DENOPTIMGraph G={ST_i,FC_i} that contains one or more cycles 
     * can be represented in multiple
     * ways, G={ST_j,FC_j} or G={ST_k,FC_k}, that differ by the position of the
     * chord/s and by the corresponding pair of ring-closing vertexes between
     * which each chord is defined.
     * The DENOPTIM-isomorphism for two DENOPTIMGraphs G1 and G2 
     * is given by the common graph theory isomorphism between 
     * two undirected graphs U1 and U2 build respectively from G1 and G2 
     * with the convention defined in 
     * {@link GraphConversionTool#getJGraphFromGraph(DENOPTIMGraph)}.
     * Finally,
     * <ul>
     * <li>vertexes are compared excluding their vertex ID, i.e., 
     * {@link DENOPTIMVertex#sameAs()}</li>
     * <li>edges are considered undirected and compared considering the 
     * {@link BondType} and the 
     * identity of the attachment points connected thereby. This latter point
     * has an important implication: two apparently equal graphs (same vertexes
     * that are connected to each other forming the same vertex-chains) can be 
     * non-isomorphic when the APs used to connect two vertexes are not the
     * same. Chemically, this means the stereochemistry around one or both
     * vertexed, is different in the two graphs. Therefore two otherwise 
     * equal-looking graphs can very well be, de facto, not isomorphic.</li>
     * </ul>
     * <p>
     * This method makes use of the Vento-Foggia VF2 algorithm (see 
     * <a href="http://ieeexplore.ieee.org/xpl/articleDetails.jsp?arnumber=1323804">DOI:10.1109/TPAMI.2004.75</a>)
     * as provided by JGraphT library in {@link VF2GraphIsomorphismInspector}.
     * </p>
     * 
     * <p>Detection of isomorphism can be very slow for pathological cases and
     * for graphs with large symmetric systems! TODO: consider adding a 
     * runtime limit or a further simplification/speed-up exploiting symmetry.
     * The symmetry, however, does not help detecting isomorphism between a
     * graph with symmetric branches and its isomorphic analogue that is fully
     * asymmetric.</p>
     * 
     * @param other the graph to be compared with this.
     * @return <code>true</code> is this graph is isomorphic to the other.
     */
    public boolean isIsomorphicTo(DENOPTIMGraph other) {
        if (this.jGraph == null)
        {
            this.jGraph = GraphConversionTool.getJGraphFromGraph(this);
        }
        if (other.jGraph == null)
        {
            other.jGraph = GraphConversionTool.getJGraphFromGraph(other);
        }
        
        
        // Simple but slow because it ignores symmetry
        /*
        Comparator<DENOPTIMVertex> vComp = (v1, v2) -> {
            // Vertex.sameAs returns boolean, so we need to produce
            // an int to allow comparison.
            StringBuilder sb = new StringBuilder();
            boolean areSame = v1.sameAs(v2, sb);
            
            if (areSame) {
                return 0;
            } else {
                return Integer.compare(v1.getBuildingBlockId(),
                        v2.getBuildingBlockId());
            }
        };
        */
        
        Comparator<DENOPTIMVertex> vComp = new Comparator<DENOPTIMVertex>() {
            
            Map<DENOPTIMVertex,Set<DENOPTIMVertex>> symmetryShortCuts = 
                    new HashMap<DENOPTIMVertex,Set<DENOPTIMVertex>>();
            
            @Override
            public int compare(DENOPTIMVertex v1, DENOPTIMVertex v2) {
                
                // exploit symmetric relations between vertexes
                if (symmetryShortCuts.containsKey(v1) 
                        && symmetryShortCuts.get(v1).contains(v2))
                {
                    return 0;
                }

                // Vertex.sameAs returns boolean, so we need to produce
                // an int to allow comparison.
                StringBuilder sb = new StringBuilder();
                if (v1.sameAs(v2, sb)) 
                {
                    Set<DENOPTIMVertex> symToV2 = new HashSet<DENOPTIMVertex>();
                    SymmetricSet ssV2 = v2.getGraphOwner().getSymSetForVertex(v2);
                    for (Integer sVrtId : ssV2.getList())
                    {
                        symToV2.add(v2.getGraphOwner().getVertexWithId(sVrtId));
                    }
                    
                    Set<DENOPTIMVertex> symToV1 = new HashSet<DENOPTIMVertex>();
                    SymmetricSet ssV1 = v1.getGraphOwner().getSymSetForVertex(v1);
                    for (Integer sVrtId : ssV1.getList())
                    {
                        symToV1.add(v1.getGraphOwner().getVertexWithId(sVrtId));
                    }
                    
                    for (DENOPTIMVertex v1s : symToV1)
                    {
                        if (symmetryShortCuts.containsKey(v1s))
                        {
                            symmetryShortCuts.get(v1s).addAll(symToV2);
                        } else {
                            symmetryShortCuts.put(v1s,symToV2);
                        }
                    }
                    return 0;
                } else {
                    // We must return something different than zero
                    if (Integer.compare(v1.getBuildingBlockId(),
                            v2.getBuildingBlockId())!=0)
                        return Integer.compare(v1.getBuildingBlockId(),
                                v2.getBuildingBlockId());
                    if (Integer.compare(v1.hashCode(),
                            v2.hashCode())!=0)
                        return Integer.compare(v1.hashCode(),
                                v2.hashCode());
                    return Integer.compare(v1.getBuildingBlockId()+v1.hashCode(),
                            v2.getBuildingBlockId()+v2.hashCode());
                }
            }
        };
        
        Comparator<UndirectedEdgeRelation> eComp =
                UndirectedEdgeRelation::compare;
        
        // NB: these two were created to evaluate the simplest and fasted 
        // possible scenario. It turns out that for a graph like the following
        // one the time spent to do automorphism (i.e., isomorphism with itself)
        // can only be improved by 20% when using these two simplest and 
        // useless (i.e., inaccurate) comparators. Instead the above, and useful
        // comparators do introduce some computational demands, but are less 
        // detrimental than having to address a wide multitude of possible 
        // mappings: this seems to be the liming factor, rather than the
        // implementation of the comparators.
        
        // Example of challenging, yet simple graph:
        //29 853_1_0_-1,855_2_1_0,857_1_1_0,858_1_1_0,859_1_1_0,861_2_1_1,862_2_1_1,863_2_1_1,864_2_1_1,865_2_1_1,866_2_1_1,867_2_1_1,868_2_1_1,869_2_1_1, 853_1_855_0_1_c:0_ATneutral:0,853_0_857_1_1_c:0_c:0,853_2_858_1_1_c:0_c:0,853_3_859_1_1_c:0_c:0,857_0_861_0_1_c:0_ATneutral:0,857_2_862_0_1_c:0_ATneutral:0,857_3_863_0_1_c:0_ATneutral:0,858_0_864_0_1_c:0_ATneutral:0,858_2_865_0_1_c:0_ATneutral:0,858_3_866_0_1_c:0_ATneutral:0,859_0_867_0_1_c:0_ATneutral:0,859_2_868_0_1_c:0_ATneutral:0,859_3_869_0_1_c:0_ATneutral:0, SymmetricSet [symIds=[857, 858, 859]] SymmetricSet [symIds=[861, 862, 863, 864, 865, 866, 867, 868, 869]]
        
        /*
        Comparator<UndirectedEdgeRelation> eCompDummy = (e1, e2) -> {
            return Integer.compare(e1.hashCode(),e2.hashCode());
        };
        Comparator<DENOPTIMVertex> vCompDummy = (v1, v2) -> {
            return Integer.compare(v1.getBuildingBlockId(),
                        v2.getBuildingBlockId());
        };
        */
        
        VF2GraphIsomorphismInspector<DENOPTIMVertex, UndirectedEdgeRelation> vf2 =
                new VF2GraphIsomorphismInspector<>(this.jGraph, other.jGraph, 
                        vComp, eComp);

        return vf2.isomorphismExists();
    }

//------------------------------------------------------------------------------

    /**
     * Compare this and another graph ignoring the vertex IDs. This method looks
     * into the structure of the graphs to determine if the two graphs have the
     * same spanning tree, same symmetric sets, and same rings set, despite
     * having different vertex IDs. Does not check for alternative permutations
     * nor alternatives spanning trees so it returns <code>false</code> for 
     * isomorphic graphs that are represented by different spanning trees.
     * @param other the other graph to be compared with this graph
     * @return <code>true</code> if the two graphs represent the same system
     */
    public boolean sameAs(DENOPTIMGraph other, StringBuilder reason)
    {
    	if (this.getEdgeCount() != other.getEdgeCount())
    	{
    		reason.append("Different number of edges ("+this.getEdgeCount()+":"
    					+other.getEdgeCount()+")");
    		return false;
    	}

    	if (this.getVertexCount() != other.getVertexCount())
    	{
    		reason.append("Different number of vertices ("+this.getVertexCount()+":"
    					+other.getVertexCount()+")");
    		return false;
    	}

    	if (this.getSymmetricSetCount() != other.getSymmetricSetCount())
    	{
    		reason.append("Different number of symmetric sets ("
    					+ this.getSymmetricSetCount() + ":"
    					+ other.getSymmetricSetCount() + ")");
    		return false;
    	}

    	if (this.getRingCount() != other.getRingCount())
    	{
    		reason.append("Different number of Rings ("+this.getRingCount()+":"
    					+other.getRingCount()+")");
    		return false;
    	}

    	//Pairwise correspondence of vertices
    	Map<DENOPTIMVertex,DENOPTIMVertex> vertexMap =
    			new HashMap<DENOPTIMVertex,DENOPTIMVertex>();

    	vertexMap.put(this.getVertexAtPosition(0),other.getVertexAtPosition(0));

    	//WARNING: assuming that the first vertex in the vertex list is the root
    	// and also that there are no disconnections. Both assumptions are
    	// reasonable for graphs.
    	try {
			if (!compareGraphNodes(this.getVertexAtPosition(0), this,
											other.getVertexAtPosition(0), other,
											vertexMap,reason))
			{
				return false;
			}
		} catch (DENOPTIMException e) {
			e.printStackTrace();
			reason.append("Exception");
			return false;
		}

    	//Check Symmetric sets
    	Iterator<SymmetricSet> ssIter = this.getSymSetsIterator();
    	while (ssIter.hasNext())
    	{
    		SymmetricSet ssT = ssIter.next();
    		int vIdT = ssT.get(0);

    		SymmetricSet ssO = other.getSymSetForVertexID(
    				vertexMap.get(this.getVertexWithId(vIdT)).getVertexId());
    		if (ssO.size() == 0)
    		{
    			// ssO is empty because no SymmetricSet was found that
    			// contains the given vertexID. This means the two graphs
    			// are different
    			reason.append("Symmetric set not found for vertex ("+vIdT+")");
    			return false;
    		}

    		if (ssT.size() != ssO.size())
    		{
    			reason.append("Different number of symmetric sets on vertex " + vIdT
    						+ "("+ssT.size()+":"+ssO.size()+")");
    			return false;
    		}

    		for (int it=0; it<ssT.size(); it++)
    		{
    			int svIdT = ssT.get(it);
    			if (!ssO.contains(vertexMap.get(this.getVertexWithId(svIdT))
    					.getVertexId()))
    			{
    				reason.append("Difference in symmetric set ("+svIdT
    							+" not in other)");
    				return false;
    			}
    		}
    	}

    	//Check Rings
    	for (DENOPTIMRing rT : this.getRings())
    	{
    		DENOPTIMVertex vhT = rT.getHeadVertex();
    		DENOPTIMVertex vtT = rT.getTailVertex();
    		boolean hasRing = other
                    .getRingsInvolvingVertex(vertexMap.get(vhT))
                    .stream()
                    .anyMatch(rO -> sameAsRings(reason, vertexMap, rT, vhT,
                            vtT, rO));
    		if (!hasRing) {
    		    return false;
            }
    	}

    	return true;
    }

//------------------------------------------------------------------------------

    private boolean sameAsRings(StringBuilder reason, Map<DENOPTIMVertex,
            DENOPTIMVertex> vertexMap, DENOPTIMRing rT, DENOPTIMVertex vhT,
                                DENOPTIMVertex vtT, DENOPTIMRing rO) {
        if (rT.getSize() != rO.getSize()) {
            reason.append("Different ring size (").append(rT.getSize())
                    .append(":").append(rO.getSize()).append(")");
            return false;
        }

        if (rO.getHeadVertex() == vertexMap.get(vhT)
                && rO.getTailVertex() == vertexMap.get(vtT)) {
            for (int i = 1; i < rT.getSize(); i++) {
                if (vertexMap.get(rT.getVertexAtPosition(i))
                        != rO.getVertexAtPosition(i)) {
                    reason.append("Rings differ (A) (").append(rT).append(":")
                            .append(rO).append(")");
                    return false;
                }
            }
        } else if (rO.getHeadVertex() == vertexMap.get(vtT)
                && rO.getTailVertex() == vertexMap.get(vhT)) {
            for (int i = 1; i < rT.getSize(); i++) {
                int j = rO.getSize() - i - 1;
                if (vertexMap.get(rT.getVertexAtPosition(i))
                        != rO.getVertexAtPosition(j)) {
                    reason.append("Rings differ (B) (").append(rT).append(":")
                            .append(rO).append(")");
                    return false;
                }
            }
        } else {
            reason.append("Rings differ (C) (").append(rT).append(":")
                    .append(rO).append(")");
            return false;
        }
        return true;
    }

//------------------------------------------------------------------------------

    /**
     * Compares graphs by spanning vertices starting from the
     * given vertex and following the direction of edges.
     * @param thisV starting vertex of first graph
     * @param thisG first graph
     * @param otherV starting vertex of second graph
     * @param otherG second graph
     * @return <code>true</code> if the graphs are same at this node
     * @throws DENOPTIMException
     */
    public static boolean compareGraphNodes(DENOPTIMVertex thisV,
                                            DENOPTIMGraph thisG,
                                            DENOPTIMVertex otherV,
                                            DENOPTIMGraph otherG) throws DENOPTIMException
    {
        Map<DENOPTIMVertex, DENOPTIMVertex> map = new HashMap<>();
        map.put(thisV, otherV);
        return compareGraphNodes(thisV, thisG, otherV, otherG, map,
                new StringBuilder());
    }

//------------------------------------------------------------------------------

    /** Compares graphs by spanning vertices starting from the
     * given vertex and following the direction of edges.
     * @param seedOnA initial vertex on the first graph.
     * @param gA the first graph.
     * @param seedOnB initial vertex on the second graph.
     * @param gB the second graph.
     * @param vertexMap vertex mapping.
     * @param reason a string recording the reason for returning false.
     * @return <code>true</code> if the graphs are same at this node.
     * @throws DENOPTIMException
     */
    private static boolean compareGraphNodes(DENOPTIMVertex seedOnA,
    		DENOPTIMGraph gA, DENOPTIMVertex seedOnB, DENOPTIMGraph gB, 
    		Map<DENOPTIMVertex,DENOPTIMVertex> vertexMap, StringBuilder reason) 
    		        throws DENOPTIMException
    {
    	if (!seedOnA.sameAs(seedOnB, reason))
    	{
    		reason.append("Different vertex ("+seedOnA+":"+seedOnB+")");
    		return false;
    	}

    	ArrayList<DENOPTIMEdge> edgesFromThis = gA.getEdgesWithSrc(seedOnA);
    	ArrayList<DENOPTIMEdge> edgesFromOther = gB.getEdgesWithSrc(seedOnB);
    	if (edgesFromThis.size() != edgesFromOther.size())
    	{
    		reason.append("Different number of edges from vertex "+seedOnA+" ("
    					+edgesFromThis.size()+":"
    					+edgesFromOther.size()+")");
    		return false;
    	}

    	// pairwise correspondence between child vertices
    	ArrayList<DENOPTIMVertex[]> pairs = new ArrayList<DENOPTIMVertex[]>();

    	for (DENOPTIMEdge et : edgesFromThis)
    	{
    		boolean found = false;
    		DENOPTIMEdge eo = null;
    		for (DENOPTIMEdge e : edgesFromOther)
    		{
    		    if (et.sameAs(e,reason))
    			{
    				found = true;
    				eo = e;
    				break;
    			}
    		}
    		if (!found)
    		{
    			reason.append("Edge not found in other("+et+")");
    			return false;
    		}

    		//Check: this should never be true
    		if (vertexMap.keySet().contains(
    				gA.getVertexWithId(et.getTrgVertex())))
    		{
    			throw new DENOPTIMException("More than one attempt to set vertex map.");
    		}
    		vertexMap.put(gA.getVertexWithId(et.getTrgVertex()),
    				gB.getVertexWithId(eo.getTrgVertex()));

    		DENOPTIMVertex[] pair = new DENOPTIMVertex[]{
    				gA.getVertexWithId(et.getTrgVertex()),
    				gB.getVertexWithId(eo.getTrgVertex())};
    		pairs.add(pair);
    	}

    	//Recursion on child vertices
    	for (DENOPTIMVertex[] pair : pairs)
    	{
    		DENOPTIMVertex v = pair[0];
    		DENOPTIMVertex o = pair[1];
    		boolean localRes = compareGraphNodes(v, gA, o, gB,vertexMap,
    				reason);
    		if (!localRes)
    		{
    			return false;
    		}
    	}

    	return true;
    }

//------------------------------------------------------------------------------

    /**
     * Returns <code>true</code> if this graph has any vertex that contains
     * atoms.
     * @return <code>true</code> if this graph has any vertex that contains
     * atoms.
     */
    public boolean containsAtoms()
    {
        for (DENOPTIMVertex v : getVertexList())
        {
            if (v.containsAtoms())
            {
                return true;
            }
        }
        return false;
    }

//------------------------------------------------------------------------------

    /**
     * Calculate the number of atoms from the graph representation
     * @return number of heavy atoms in the molecule
     */
    public int getHeavyAtomsCount()
    {
        int n = 0;
        for (DENOPTIMVertex v : getVertexList())
        {
            n += v.getHeavyAtomsCount();
        }
        return n;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the list of all attachment points contained in this graph
     * @return list of attachment points.
     */

    public ArrayList<DENOPTIMAttachmentPoint> getAttachmentPoints()
    {
        ArrayList<DENOPTIMAttachmentPoint> lstAPs =
                new ArrayList<DENOPTIMAttachmentPoint>();
        for (DENOPTIMVertex v : gVertices)
        {
            lstAPs.addAll(v.getAttachmentPoints());
        }
        return lstAPs;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the list of available attachment points contained in this graph
     * @return list of attachment points.
     */

    public ArrayList<DENOPTIMAttachmentPoint> getAvailableAPs()
    {
        ArrayList<DENOPTIMAttachmentPoint> lstFreeAPs =
                new ArrayList<DENOPTIMAttachmentPoint>();
        for (DENOPTIMAttachmentPoint ap : getAttachmentPoints())
        {
            if (ap.isAvailable())
            {
                lstFreeAPs.add(ap);
            }
        }
        return lstFreeAPs;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the attachment point with the given identifier, or null if no
     * AP is found with the given identifier.
     * @param id identifier of the attachment point to return
     * @return the attachment point with the given identifier, or null if no
     * AP is found with the given identifier.
     */

    public DENOPTIMAttachmentPoint getAPWithId(int id)
    {
        DENOPTIMAttachmentPoint ap = null;
        for (DENOPTIMAttachmentPoint apCand : getAttachmentPoints())
        {
            if (apCand.getID() == id)
            {
                ap = apCand;
                break;
            }
        }
        return ap;
    }

//------------------------------------------------------------------------------

    /**
     * Checks the graph for unused APs that need to be capped
     * @return <code>true</code> if the graph has at least one AP that needs
     * to be capped
     */

    public boolean graphNeedsCappingGroups()
    {
        for (DENOPTIMVertex v : getVertexList()) {
            for (DENOPTIMAttachmentPoint ap : v.getAttachmentPoints()) {
                if (ap.isAvailable()
                        && FragmentSpace.getAPClassOfCappingVertex(ap.getAPClass()) !=null
                ) {
                    return true;
                }
            }
        }
        return false;
    }

//------------------------------------------------------------------------------

    /**
     * Remove capping groups on the given vertex of this graph
     */

    public void removeCappingGroupsOn(DENOPTIMVertex vertex)
    {
        ArrayList<DENOPTIMVertex> toDel = new ArrayList<DENOPTIMVertex>();
        for (DENOPTIMVertex vtx : this.getChildVertices(vertex))
        {
            if (vtx instanceof DENOPTIMFragment == false)
            {
                continue;
            }
            // capping groups have fragment type 2
            if (((DENOPTIMFragment) vtx).getBuildingBlockType() == BBType.CAP
                    && !isVertexInRing(vtx))
            {
                toDel.add(vtx);
            }
        }

        for (DENOPTIMVertex v : toDel)
        {
            removeVertex(v);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Remove capping groups that belong to this graph and are in the given list.
     * @param lstVerts the list of vertexes to analyze.
     */

    public void removeCappingGroups(List<DENOPTIMVertex> lstVerts)
    {
        ArrayList<Integer> rvids = new ArrayList<>();
        for (int i=0; i<lstVerts.size(); i++)
        {
            DENOPTIMVertex vtx = lstVerts.get(i);
            if (vtx instanceof DENOPTIMFragment == false)
            {
                continue;
            }
            
            if (((DENOPTIMFragment) vtx).getBuildingBlockType() == BBType.CAP
                    && !isVertexInRing(vtx))
            {
                rvids.add(vtx.getVertexId());
            }
        }

        // remove the vids from the vertex lst
        for (int i=0; i<rvids.size(); i++)
        {
            int vid = rvids.get(i);
            removeVertex(getVertexWithId(vid));
        }
    }
    
//------------------------------------------------------------------------------
    
    public void removeCappingGroupsFromChilds(List<DENOPTIMVertex> lstVerts)
    {
        for (DENOPTIMVertex v : lstVerts)
        {
            removeCappingGroups(getChildVertices(v));
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Remove all capping groups on this graph
     */

    public void removeCappingGroups()
    {
        removeCappingGroups(new ArrayList<DENOPTIMVertex>(gVertices));
    }
    
//------------------------------------------------------------------------------

    /**
     * Add a capping groups on free unused attachment points.
     * Addition of Capping groups does not update the symmetry table
     * for a symmetric graph.
     */

    public void addCappingGroups() throws DENOPTIMException
    {
        if (!FragmentSpace.useAPclassBasedApproach())
            return;
        addCappingGroups(new ArrayList<DENOPTIMVertex>(gVertices));
    }
    
//------------------------------------------------------------------------------

    /**
     * Add a capping group on the given vertexes, if needed. The need for such 
     * groups is manifest when an attachment point that is not used in this
     * or any embedding lever (i.e., attachment point is 
     * {@link DENOPTIMAttachmentPoint#isAvailableThroughout()}) has and 
     * {@link APClass} that demands to be capped.
     * Addition of Capping groups does not update the symmetry table
     * for a symmetric graph.
     * @param vertexAddedToThis list of vertexes to operate on. They must belong to this
     * graph.
     * @throws DENOPTIMException if the addition of capping groups cannot be 
     * performed.
     */

    public void addCappingGroups(List<DENOPTIMVertex> vertexAddedToThis)
                                                    throws DENOPTIMException
    {
        if (!FragmentSpace.useAPclassBasedApproach())
            return;

        for (DENOPTIMVertex curVertex : vertexAddedToThis)
        {
            // no capping of a capping group. Since capping groups are expected
            // to have only one AP, there should never be a capping group with 
            // a free AP.
            if (curVertex.getBuildingBlockType() == DENOPTIMVertex.BBType.CAP)
            {
                //String msg = "Attempting to cap a capping group. Check your data.";
                //DENOPTIMLogger.appLogger.log(Level.WARNING, msg);
                continue;
            }

            for (DENOPTIMAttachmentPoint curDap : curVertex.getAttachmentPoints())
            {
                if (curDap.isAvailableThroughout())
                {
                    APClass apcCap = FragmentSpace.getAPClassOfCappingVertex(
                            curDap.getAPClass());
                    if (apcCap != null)
                    {
                        int bbIdCap = FragmentSpace.getCappingFragment(apcCap);

                        if (bbIdCap != -1)
                        {
                            DENOPTIMVertex capVrtx = 
                                    DENOPTIMVertex.newVertexFromLibrary(
                                        GraphUtils.getUniqueVertexIndex(), 
                                        bbIdCap, 
                                        DENOPTIMVertex.BBType.CAP);
                            DENOPTIMGraph molGraph = curDap.getOwner()
                                    .getGraphOwner();
                            if (molGraph == null)
                                throw new DENOPTIMException("Canno add capping "
                                        + "groups to a vertex that does not "
                                        + "belong to a graph.");
                            molGraph.appendVertexOnAP(curDap, capVrtx.getAP(0));
                        }
                        else
                        {
                            String msg = "Capping is required but no proper "
                                    + "capping fragment found with APCalss " 
                                    + apcCap;
                            DENOPTIMLogger.appLogger.log(Level.SEVERE,msg);
                            throw new DENOPTIMException(msg);
                        }
                    }
                }
            }
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Creates a new graph that corresponds to the subgraph of this graph 
     * when exploring the spanning tree from a given seed vertex.
     * Only the seed vertex and all child vertices (and further successors)
     * are considered part of
     * the subgraph, which includes also rings and symmetric sets. All
     * rings that include vertices not belonging to the subgraph are lost.
     * @param index the position of the seed vertex in the list of vertexes of this graph.
     * @return a new graph that corresponds to the subgraph of this graph.
     */

    public DENOPTIMGraph extractSubgraph(int index)
            throws DENOPTIMException
    {
        return extractSubgraph(this.getVertexAtPosition(index));
    }

//------------------------------------------------------------------------------

    /**
     * Creates a new graph that corresponds to the subgraph of this graph 
     * when exploring the spanning tree from a given seed vertex.
     * Only the seed vertex and all child vertices (and further successors)
     * are considered part of
     * the subgraph, which includes also rings and symmetric sets. All
     * rings that include vertices not belonging to the subgraph are lost.
     * @param seed the vertex from which the extraction has to start.
     * @return a new graph that corresponds to the subgraph of this graph.
     */

    public DENOPTIMGraph extractSubgraph(DENOPTIMVertex seed)
            throws DENOPTIMException
    {
        return extractSubgraph(seed, Integer.MAX_VALUE, false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Creates a new graph that corresponds to the subgraph of this graph 
     * when exploring the spanning tree from a given seed vertex and visiting
     * at most a given number of vertex layers, and optionally stopping the
     * exploration of a branch before including any ring-closing vertex.
     * Only the seed vertex and all child vertices (and further successors)
     * are considered part of
     * the subgraph, which includes also rings and symmetric sets. All
     * rings that include vertices not belonging to the subgraph are lost.
     * @param seed the vertex from which the extraction has to start.
     * @param numLayers the maximum number of vertex layers after the seen 
     * vertex that we want to consider before stopping. If this value is 2, we 
     * will explore three layers: the seed, and two more layers away from it.
     * @param stopBeforeRCVs set <code>true</code> to make the exploration of
     * each branch stop before including ring closing vertexes.
     * @return a new graph that corresponds to the subgraph of this graph.
     */

    public DENOPTIMGraph extractSubgraph(DENOPTIMVertex seed, int numLayers, 
            boolean stopBeforeRCVs) throws DENOPTIMException
    {
        if (!this.gVertices.contains(seed))
        {
            throw new DENOPTIMException("Attempt to extract a subgraph giving "
                    + "a seed vertex that is not contained in this graph.");
        }
        DENOPTIMGraph subGraph = this.clone();
        DENOPTIMVertex seedClone = subGraph.getVertexAtPosition(
                this.indexOf(seed));
        
        ArrayList<DENOPTIMVertex> subGrpVrtxs = new ArrayList<DENOPTIMVertex>();
        subGrpVrtxs.add(seedClone);
        subGraph.getChildrenTree(seedClone, subGrpVrtxs, numLayers, stopBeforeRCVs);
        ArrayList<DENOPTIMVertex> toRemove = new ArrayList<DENOPTIMVertex>();
        for (DENOPTIMVertex v : subGraph.gVertices)
        {
            if (!subGrpVrtxs.contains(v))
            {
                toRemove.add(v);
            }
        }
        
        for (DENOPTIMVertex v : toRemove)
        {
            subGraph.removeVertex(v);
        }
        
        return subGraph;
    }
    
//------------------------------------------------------------------------------

    /**
     * Creates a new graph that corresponds to the subgraph of this graph 
     * when exploring the spanning tree from a given seed vertex and 
     * stopping the exploration at the given end vertexes (i.e., limits).
     * Optionally, you can ask to stop the
     * exploration of a branch before including any ring-closing vertex.
     * Only the seed vertex and all child vertices (and further successors)
     * are considered part of
     * the subgraph, which includes also rings and symmetric sets. All
     * rings that include vertices not belonging to the subgraph are lost.
     * @param seed the vertex from which the extraction has to start.
     * @param limits the vertexes playing the role of subgraph end-points that 
     * stop the exploration of the graph.
     * @param stopBeforeRCVs set <code>true</code> to make the exploration of
     * each branch stop before including ring closing vertexes.
     * @return a new graph that corresponds to the subgraph of this graph.
     */
    public DENOPTIMGraph extractSubgraph(DENOPTIMVertex seed, 
            List<DENOPTIMVertex> limits, boolean stopBeforeRCVs) 
                    throws DENOPTIMException
    {
        if (!this.gVertices.contains(seed))
        {
            throw new DENOPTIMException("Attempt to extract a subgraph giving "
                    + "a seed vertex that is not contained in this graph.");
        }
        
        if (limits.size()==0)
        {
            return extractSubgraph(seed, stopBeforeRCVs);
        }
        
        DENOPTIMGraph subGraph = this.clone();
        DENOPTIMVertex seedClone = subGraph.getVertexAtPosition(
                this.indexOf(seed));
        
        List<DENOPTIMVertex> limitsInClone =  new ArrayList<DENOPTIMVertex>();
        for (DENOPTIMVertex v : limits)
            limitsInClone.add(subGraph.getVertexAtPosition(this.indexOf(v)));
        
        ArrayList<DENOPTIMVertex> subGrpVrtxs = new ArrayList<DENOPTIMVertex>();
        subGrpVrtxs.add(seedClone);
        subGraph.getChildTreeLimited(seedClone, subGrpVrtxs, limitsInClone, 
                stopBeforeRCVs);
        
        ArrayList<DENOPTIMVertex> toRemove = new ArrayList<DENOPTIMVertex>();
        for (DENOPTIMVertex v : subGraph.gVertices)
        {
            if (!subGrpVrtxs.contains(v))
            {
                toRemove.add(v);
            }
        }
        for (DENOPTIMVertex v : toRemove)
        {
            subGraph.removeVertex(v);
        }
        
        return subGraph;
    }
    
//------------------------------------------------------------------------------

    /**
     * Creates a new graph that corresponds to the subgraph of this graph 
     * when exploring the spanning tree from a given seed vertex.
     * Optionally, you can ask to stop the
     * exploration of a branch before including any ring-closing vertex.
     * Only the seed vertex and all child vertices (and further successors)
     * are considered part of
     * the subgraph, which includes also rings and symmetric sets. All
     * rings that include vertices not belonging to the subgraph are lost.
     * @param seed the vertex from which the extraction has to start.
     * @param stopBeforeRCVs set <code>true</code> to make the exploration of
     * each branch stop before including ring closing vertexes.
     * @return a new graph that corresponds to the subgraph of this graph.
     */
    public DENOPTIMGraph extractSubgraph(DENOPTIMVertex seed, boolean stopBeforeRCVs) 
                    throws DENOPTIMException
    {
        if (!this.gVertices.contains(seed))
        {
            throw new DENOPTIMException("Attempt to extract a subgraph giving "
                    + "a seed vertex that is not contained in this graph.");
        }
        
        DENOPTIMGraph subGraph = this.clone();
        DENOPTIMVertex seedClone = subGraph.getVertexAtPosition(
                this.indexOf(seed));
        
        ArrayList<DENOPTIMVertex> subGrpVrtxs = new ArrayList<DENOPTIMVertex>();
        subGrpVrtxs.add(seedClone);
        subGraph.getChildTreeLimited(seedClone, subGrpVrtxs, stopBeforeRCVs);
        
        ArrayList<DENOPTIMVertex> toRemove = new ArrayList<DENOPTIMVertex>();
        for (DENOPTIMVertex v : subGraph.gVertices)
        {
            if (!subGrpVrtxs.contains(v))
            {
                toRemove.add(v);
            }
        }
        for (DENOPTIMVertex v : toRemove)
        {
            subGraph.removeVertex(v);
        }
        
        return subGraph;
    }
    
//------------------------------------------------------------------------------

    /**
     * Creates a new graph that corresponds to the subgraph of this graph 
     * and that includes only the members corresponding to the given list of 
     * vertexes belonging to this graph.
     * @param members the vertexes belonging to the subgraph. 
     * @return a new graph that corresponds to the subgraph of this graph.
     */
    public DENOPTIMGraph extractSubgraph(List<DENOPTIMVertex> members) 
    {
        if (members.size()==0)
            return null;
        
        DENOPTIMGraph subGraph = this.clone();
        
        List<DENOPTIMVertex> subGrpVrtxs =  new ArrayList<DENOPTIMVertex>();
        for (DENOPTIMVertex v : members)
        {
            subGrpVrtxs.add(subGraph.getVertexAtPosition(this.indexOf(v)));
        }
        
        ArrayList<DENOPTIMVertex> toRemove = new ArrayList<DENOPTIMVertex>();
        for (DENOPTIMVertex v : subGraph.gVertices)
        {
            if (!subGrpVrtxs.contains(v))
            {
                toRemove.add(v);
            }
        }
        for (DENOPTIMVertex v : toRemove)
        {
            subGraph.removeVertex(v);
        }
        
        return subGraph;
    }

//------------------------------------------------------------------------------

    /**
     * Extracts subgraphs that match the provided pattern.
     * @param pattern to match against.
     * @return The subgraphs matching the provided pattern.
     * @throws DENOPTIMException 
     */
      
    public List<DENOPTIMGraph> extractPattern(GraphPattern pattern) 
            throws DENOPTIMException 
    {
        if (pattern != GraphPattern.RING) {
            throw new IllegalArgumentException("Graph pattern " + pattern +
                    " not supported.");
        }

        List<Set<DENOPTIMVertex>> disjointMultiCycleVertices = this
                  .getRings()
                  .stream()
                  .map(DENOPTIMRing::getVertices)
                  .map(HashSet::new)
                  .collect(Collectors.toList());

        GenUtils.unionOfIntersectingSets(disjointMultiCycleVertices);

        List<DENOPTIMGraph> subgraphs = new ArrayList<>();
        for (Set<DENOPTIMVertex> fusedRing : disjointMultiCycleVertices) {
            subgraphs.add(extractSubgraph(fusedRing));
        }
          
        for (DENOPTIMGraph g : subgraphs) {
            g.storeCurrentVertexIDs();
            g.renumberGraphVertices();
            reorderVertexList(g);
        }

        return subgraphs;
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the subgraph in the graph defined on the a set of vertices.
     * The graph is cloned before the subgraph is extracted.
     * @param graph To extract subgraph from.
     * @param definedOn Set of vertices in the graph that the subgraph is
     *                  defined on.
     * @return Subgraph of graph defined on set of vertices.
     */
    private DENOPTIMGraph extractSubgraph(Set<DENOPTIMVertex> definedOn) 
    {
        DENOPTIMGraph subgraph = this.clone();

        Set<DENOPTIMVertex> complement = subgraph
                .getVertexList()
                .stream()
                .filter(u -> definedOn
                        .stream()
                        .allMatch(v -> v.getVertexId() != u.getVertexId())
                ).collect(Collectors.toSet());

        for (DENOPTIMVertex v : complement) {
            subgraph.removeVertex(v);
        }
        return subgraph;
    }
    

  //------------------------------------------------------------------------------

      /**
       * Sets the vertex at the lowest level as the scaffold, changes the  
       * directions of edges so that the scaffold is the source, and changes 
       * the levels of the graph's other vertices to be consistent with the new
       * scaffold.
       * @param g Graph to fix.
       */
      private static void reorderVertexList(DENOPTIMGraph g) 
      {
          DENOPTIMVertex newScaffold = g.getSourceVertex();
          if (newScaffold == null) {
              return;
          }
          DENOPTIMGraph.setScaffold(newScaffold);
          fixEdgeDirections(g);
      }

//------------------------------------------------------------------------------

    /**
     * Flips edges in the graph so that the scaffold is the only source vertex.
     * @param graph to fix edges of.
     */
    private static void fixEdgeDirections(DENOPTIMGraph graph) 
    {
        DENOPTIMVertex src = graph.getSourceVertex();
        fixEdgeDirections(src, new HashSet<>());
    }

//------------------------------------------------------------------------------

    /**
     * Recursive utility method for fixEdgeDirections(DENOPTIMGraph graph).
     * @param v current vertex
     */
    private static void fixEdgeDirections(DENOPTIMVertex v, 
            Set<Integer> visited) 
    {
        visited.add(v.getVertexId());
        int visitedVertexEncounters = 0;
        for (int i = 0; i < v.getNumberOfAPs(); i++) {
            DENOPTIMAttachmentPoint ap = v.getAP(i);
            DENOPTIMEdge edge = ap.getEdgeUser();
            if (edge != null) {
                int srcVertex = edge.getSrcVertex();
                boolean srcIsVisited =
                        srcVertex != v.getVertexId() 
                        && visited.contains(srcVertex);

                visitedVertexEncounters += srcIsVisited ? 1 : 0;
                if (visitedVertexEncounters >= 2) {
                    throw new IllegalArgumentException("Invalid graph. "
                            + "Contains a cycle.");
                }

                boolean edgeIsWrongWay = edge.getTrgVertex() 
                        == v.getVertexId() && !srcIsVisited;
                if (edgeIsWrongWay) {
                    edge.flipEdge();
                }
                if (!srcIsVisited) {
                    fixEdgeDirections(edge.getTrgAP().getOwner(), visited);
                }
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Deletes the branch, i.e., the specified vertex and its children.
     * @param vid the vertexID of the root of the branch. We'll remove also
     * this vertex.
     * @param symmetry use <code>true</code> to enforce deletion of all
     * symmetric vertices.
     * @return <code>true</code> if operation is successful.
     * @throws DENOPTIMException
     */

    public boolean removeBranchStartingAt(DENOPTIMVertex v, boolean symmetry)
            throws DENOPTIMException
    {
        boolean res = true;
        if (hasSymmetryInvolvingVertex(v) && symmetry)
        {
            ArrayList<DENOPTIMVertex> toRemove = new ArrayList<DENOPTIMVertex>();
            for (int i=0; i<getSymSetForVertexID(v.getVertexId()).size(); i++)
            {
                int svid = getSymSetForVertexID(v.getVertexId()).getList().get(i);
                toRemove.add(getVertexWithId(svid));
            }
            for (DENOPTIMVertex sv : toRemove)
            {
                boolean res2 = removeBranchStartingAt(sv);
                if (!res2)
                {
                    res = res2;
                }
            }
        }
        else
        {
            res = removeBranchStartingAt(v);
        }
        return res;
    }
    
//------------------------------------------------------------------------------

    /**
     * Deletes the branch, i.e., the specified vertex and its children.
     * Updates symmetry accordingly, but does not remove symmetric branches.
     * @param vid the vertexID of the root of the branch. We'll remove also
     * this vertex.
     * @return <code>true</code> if operation is successful
     * @throws DENOPTIMException
     */

    public boolean removeBranchStartingAt(DENOPTIMVertex v)
            throws DENOPTIMException
    {
        DENOPTIMEdge edgeToParent = v.getEdgeToParent();
        if (edgeToParent != null)
        {
            removeEdge(edgeToParent);
        }
        return removeOrphanBranchStartingAt(v);
    }

//------------------------------------------------------------------------------

    /**
     * Deletes the branch, i.e., the specified vertex and its children.
     * This method does not cross template 
     * boundaries, thus all children belong to the same graph.
     * Symmetry relations are updated accordingly.
     * @param vid the vertexID of the root of the branch. We'll remove also
     * this vertex.
     * @param allowOrphan use <code>true</code> to allow removal of branches
     * starting with a vertex that has no edge to a parent.
     * @return <code>true</code> if operation is successful
     * @throws DENOPTIMException
     */

    public boolean removeOrphanBranchStartingAt(DENOPTIMVertex v)
            throws DENOPTIMException
    {
        // now get the vertices attached to v
        ArrayList<DENOPTIMVertex> children = new ArrayList<DENOPTIMVertex>();
        getChildrenTree(v, children);

        // delete the children vertices and associated edges
        for (DENOPTIMVertex c : children) {
            removeVertex(c);
        }

        // finally delete the vertex and associated edges
        removeVertex(v);

        return getVertexWithId(v.getVertexId()) == null;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Mutates the graph by removing the chain where a given vertex is located
     * up to the first branching (i.e., to non-capping group) on either side.
     * The graph is updated accordingly. In particular, the direction of 
     * edges in the surviving graph and the definition of fundamental cycles 
     * are changed to reflect the change of spanning tree.
     * @param v the vertex from which we start detecting a chain
     * @return<code>true</code> if successful.
     * @throws DENOPTIMException is any assumption on the healthy structure of 
     * this graph is not verified.
     */
    public boolean removeChainUpToBranching(DENOPTIMVertex v) throws DENOPTIMException
    {
        List<DENOPTIMRing> rings = getRingsInvolvingVertex(v);
        if (rings.isEmpty())
        {
            return false;
        }
        
        // Identify the RCVs defining the chord that might become an edge
        // The corresponding ring is the "frame" we'll use throughout this 
        // operation, and is the smallest ring that is closest to the appointed
        // vertex.
        DENOPTIMRing frame = null;
        DENOPTIMVertex[] replacedByEdge = new DENOPTIMVertex[2];
        int minDist = Integer.MAX_VALUE;
        BondType bt = null;
        for (DENOPTIMRing r : rings)
        {
            int dist = Math.min(r.getDistance(r.getHeadVertex(),v),
                    r.getDistance(r.getTailVertex(),v));
            if (dist < minDist)
            {
                minDist = dist;
                frame = r;
                replacedByEdge[0] = r.getHeadVertex();
                replacedByEdge[1] = r.getTailVertex();
                bt = r.getBondType();
            }
        }
        
        // Find out where branching vertexes are along the frame ring
        boolean frameHasBranching = false;
        List<Integer> branchingPositions = new ArrayList<Integer>();
        int posOfFocusVrtInRing = frame.getPositionOf(v);
        for (int i=0; i<frame.getSize(); i++)
        {
            DENOPTIMVertex vi = frame.getVertexAtPosition(i);
            // We consider the scaffold as a branching point, i.e., it will never be removed
            if (vi.getBuildingBlockType() == BBType.SCAFFOLD)
            {
                branchingPositions.add(i);
                continue;
            }
            long oNonCap = vi.getAttachmentPoints().size() 
                    - vi.getCappedAPCountThroughout()
                    - vi.getFreeAPCountThroughout();
            if (oNonCap > 2)
            {
                branchingPositions.add(i);
                frameHasBranching = true;
            }
        }
        
        // Make sure this ring is not all there is in this graph.
        if (rings.size()==1 && !frameHasBranching) 
        {
            return false;
        }
        
        // Identify the end points of chain that gets removed, i.e., the chain
        // containing V and is between branchingDownstreamId and 
        // branchingUpstreamId gets removed.
        int branchingDownstreamId = -1;
        for (int i=0; i<frame.getSize(); i++)
        {
            int iTranslated = i + posOfFocusVrtInRing;
            if (iTranslated >= frame.getSize())
                iTranslated = iTranslated - frame.getSize();
            if (branchingPositions.contains(iTranslated))
            {
                branchingDownstreamId = iTranslated;
                break;
            }
        }
        int branchingUpstreamId = -1;
        for (int i=(frame.getSize()-1); i>-1; i--)
        {
            int iTranslated = i + posOfFocusVrtInRing;
            if (iTranslated >= frame.getSize())
                iTranslated = iTranslated - frame.getSize();
            if (branchingPositions.contains(iTranslated))
            {
                branchingUpstreamId = iTranslated;
                break;
            }
        }
        
        // Now, collect the vertexes along the ring based on whether they
        // will be removed or remain (possible with a change of edge direction
        // and replacement of an RCV pair by edge)
        List<DENOPTIMVertex> remainingChain = new ArrayList<DENOPTIMVertex>();
        for (int i=0; i<frame.getSize(); i++)
        {
            int iTranslated = i + branchingDownstreamId;
            if (iTranslated >= frame.getSize())
                iTranslated = iTranslated - frame.getSize();
            remainingChain.add(frame.getVertexAtPosition(iTranslated));
            if (iTranslated == branchingUpstreamId)
            {
                break;
            }
        }
        List<DENOPTIMVertex> toRemoveChain = new ArrayList<DENOPTIMVertex>();
        for (int i=0; i<frame.getSize(); i++)
        {
            int iTranslated = i + branchingUpstreamId + 1; //exclude branch point
            if (iTranslated >= frame.getSize())
                iTranslated = iTranslated - frame.getSize();
            toRemoveChain.add(frame.getVertexAtPosition(iTranslated));
            if (iTranslated == (branchingDownstreamId - 1)) //exclude branch point
            {
                break;
            }
        }
        
        // Need to exclude the case where the chain to remove consists only of
        // the RCVs because it would lead to loss of the chord and creation of
        // cyclic graph. Also, proceeding without making the edge would simply
        // correspond to removing the chord.
        if (toRemoveChain.size() == 2)
        {
            int countOrRCVs = 0;
            for (DENOPTIMVertex vtr : toRemoveChain)
            {
                if (vtr.isRCV())
                    countOrRCVs++;
            }
            if (countOrRCVs == 2)
            {
                return false;
            }
        }
        
        // To understand if we need to reverse some edges, we identify the
        // deepest vertex level-wise. It may or may not be a scaffold!!!
        int deepestLevel = Integer.MAX_VALUE;
        DENOPTIMVertex deepestVrtRemainingChain = null;
        for (DENOPTIMVertex vint : remainingChain)
        {
            int lvl = getLevel(vint);
            if (lvl < deepestLevel)
            {
                deepestLevel = lvl;
                deepestVrtRemainingChain = vint;
            }
        }
        
        // Check if we need to transform a pair of RCVs into an edge, and 
        // identify APs used by the edge that replaces the to-be-removed RCVs.
        // I do not see how the chain can possibly contain only one of the 
        // RCVs, so I assume that if one RCV is there, then the other is too.
        if (remainingChain.contains(replacedByEdge[0]))
        {
            DENOPTIMAttachmentPoint apSrcOfNewEdge = replacedByEdge[0]
                    .getEdgeToParent().getSrcAP();
            DENOPTIMAttachmentPoint apTrgOfNewEdge = replacedByEdge[1]
                    .getEdgeToParent().getSrcAP();
            
            // Remove the RCVs that will be replaced by an edge
            removeVertex(replacedByEdge[0]);
            remainingChain.remove(replacedByEdge[0]);
            removeVertex(replacedByEdge[1]);
            remainingChain.remove(replacedByEdge[1]);
            
            // And make the new edge. The direction can be anything. Later, we
            // decide if we need to reverse it.
            //NB: the compatibility between the APs should be granted by the 
            // previous existence of the chord. so no need to check 
            // apSrcOfNewEdge.getAPClass().isCPMapCompatibleWith(
            //        apTrgOfNewEdge.getAPClass()))
            addEdge(new DENOPTIMEdge(apSrcOfNewEdge, apTrgOfNewEdge, bt));
        } else {
            // We remove the frame already, otherwise we will try to recreate 
            // such ring from RCVs and waste time with it.
            removeRing(frame);
        }
        
        // Now, inspect the paths from the deepest vertex and outwards, to
        // find out where to start reversing edges.
        List<DENOPTIMVertex> chainToReverseA = new ArrayList<DENOPTIMVertex>();
        List<DENOPTIMVertex> chainToReverseB = new ArrayList<DENOPTIMVertex>();
        for (int i=(remainingChain.indexOf(deepestVrtRemainingChain)+1); 
                i<remainingChain.size(); i++)
        {
            DENOPTIMVertex vPrev = remainingChain.get(i-1);
            DENOPTIMVertex vHere = remainingChain.get(i);
            if (!vPrev.getChilddren().contains(vHere))
            {
                // in an healthy spanning tree, once we find the first reversed
                // edge, all the following edges will also have to be reversed.
                if (chainToReverseA.size()==0)
                    chainToReverseA.add(vPrev);
                chainToReverseA.add(vHere);
            }
        }
        for (int i=(remainingChain.indexOf(deepestVrtRemainingChain)-1);i>-1;i--)
        {
            DENOPTIMVertex vPrev = remainingChain.get(i+1);
            DENOPTIMVertex vHere = remainingChain.get(i);
            if (!vPrev.getChilddren().contains(vHere))
            {
                if (chainToReverseB.size()==0)
                    chainToReverseB.add(vPrev);
                chainToReverseB.add(vHere);
            }
        }
        
        // These are to remember all chords that will have to be recreated
        LinkedHashMap<DENOPTIMVertex,DENOPTIMVertex> chordsToRecreate = 
                new LinkedHashMap<DENOPTIMVertex,DENOPTIMVertex>();
        LinkedHashMap<DENOPTIMVertex,BondType> chordsToRecreateBB = 
                new LinkedHashMap<DENOPTIMVertex,BondType>();
        
        // Change direction of those edges that have to be reversed as a 
        // consequence of the change in the spanning tree.
        if (chainToReverseA.size()+chainToReverseB.size() > 1)
        {
            List<DENOPTIMVertex> chainToWorkOn = null;
            for (int ic=0; ic<2; ic++)
            {
                if (ic == 1)
                    chainToWorkOn = chainToReverseA;
                else
                    chainToWorkOn = chainToReverseB;
            
                for (int i=1; i<chainToWorkOn.size(); i++)
                {
                    DENOPTIMVertex vHere = chainToWorkOn.get(i);
                    DENOPTIMVertex vPrev = chainToWorkOn.get(i-1);
                    List<DENOPTIMRing> ringsToRecreate = new ArrayList<>();
                    for (DENOPTIMRing r : getRingsInvolvingVertex(vHere))
                    {
                        ringsToRecreate.add(r);
                        chordsToRecreate.put(r.getHeadVertex(), 
                                r.getTailVertex());
                        chordsToRecreateBB.put(r.getHeadVertex(), 
                                r.getBondType());
                    }
                    for (DENOPTIMRing r : ringsToRecreate)
                    {
                        removeRing(r);
                    }
                    
                    DENOPTIMEdge edgeToPrevious = vHere.getEdgeWith(vPrev);
                    if (edgeToPrevious == null) 
                    {
                        // Since we have already made the new edge this should 
                        // never happen.
                        String debugFile = "debug_"+v.getVertexId()+".json";
                        DenoptimIO.writeGraphToJSON(new File(debugFile), this);
                        throw new DENOPTIMException("Unconnected vertexes "  
                                + vHere.getVertexId() + " and "
                                + vPrev.getVertexId() + ". Unable to deal with "
                                + "removal of " + v + " from ring " + frame 
                                + " in graph " + this.getGraphId() 
                                + ". See graph in " + debugFile);
                    } else {
                        if (edgeToPrevious.getSrcAP().getOwner() == vHere) 
                        {
                            // This is an edge that has to be reversed.
                            DENOPTIMAttachmentPoint newSrcAP = 
                                    edgeToPrevious.getTrgAP();
                            DENOPTIMAttachmentPoint newTrgAP = 
                                    edgeToPrevious.getSrcAP();
                            if (newSrcAP.getAPClass().isCPMapCompatibleWith(
                                    newTrgAP.getAPClass()))
                            {
                                BondType oldBt = edgeToPrevious.getBondType();
                                removeEdge(edgeToPrevious);
                                addEdge(new DENOPTIMEdge(newSrcAP, newTrgAP, 
                                        oldBt));
                            } else {
                                // There is a non-reversible connection along 
                                // the way, therefore we cannot do this 
                                // specific mutation.
                                return false;
                            }
                        }
                    }
                }
            }
        }
           
        // Delete the chain to be removed
        for (DENOPTIMVertex vtr : toRemoveChain) 
        {
            // This works across template boundaries!
            for (DENOPTIMVertex child : vtr.getChildrenThroughout())
            {
                if (remainingChain.contains(child)
                        || toRemoveChain.contains(child))
                    continue;
                DENOPTIMGraph ownerOfChild = child.getGraphOwner();
                ownerOfChild.removeVertex(child);
            }
            if (templateJacket!= null)
            {
                List<DENOPTIMAttachmentPoint> apProjectionsToRemove = 
                        new ArrayList<DENOPTIMAttachmentPoint>();
                for (DENOPTIMAttachmentPoint outerAP : 
                    templateJacket.getAttachmentPoints())
                {
                    DENOPTIMAttachmentPoint innerAP = 
                            templateJacket.getInnerAPFromOuterAP(outerAP);
                    if (innerAP.getOwner() == vtr)
                    {
                        apProjectionsToRemove.add(innerAP);
                    }
                }
                for (DENOPTIMAttachmentPoint apToRemove : apProjectionsToRemove)
                    templateJacket.removeProjectionOfInnerAP(apToRemove);
            }
            removeVertex(vtr);
        }
        
        // Regenerate the rings that have been affected
        for (DENOPTIMVertex h : chordsToRecreate.keySet())
        {
            addRing(h, chordsToRecreate.get(h), chordsToRecreateBB.get(h));
        }
        
        // Symmetry relation need to be compared with the change of topology.
        // The worst that can happen is that two vertexes that are
        // listed as symmetric are, instead, one the (grand)parent of 
        // the other. This is inconsistent with the expectations when
        // dealing with any operation with symmetric vertexes.
        // A different level does NOT imply a parent-child relation,
        // but is a sign that the topology has changed substantially,
        // and that the symmetric relation is, most likely, not sensible
        // anymore.
        List<SymmetricSet> ssToRemove = new ArrayList<SymmetricSet>();
        Iterator<SymmetricSet> ssIter = getSymSetsIterator();
        while (ssIter.hasNext())
        {
            SymmetricSet ss = ssIter.next();
            int level = -2;
            for (Integer vid : ss.getList())
            {
                if (level==-2)
                {
                    level = getLevel(getVertexWithId(vid));
                } else {
                    if (level != getLevel(getVertexWithId(vid)))
                    {
                        ssToRemove.add(ss);
                        break;
                    }
                }
            }
        }
        for (SymmetricSet ss : ssToRemove)
            removeSymmetrySet(ss);
        
        // The free-ed up APs need to be projected to template's surface
        if (templateJacket!= null)
        {
            for (DENOPTIMAttachmentPoint innerAP : getAvailableAPs())
            {
                templateJacket.addInnerToOuterAPMapping(innerAP);
            }
        }
        
        return true;
    }

//------------------------------------------------------------------------------

    /**
     * Change all vertex IDs to the corresponding negative value. For instance
     * if the vertex ID is 12 this method changes it into -12.
     */

    @Deprecated
    public void changeSignOfVertexID()
    {
        HashMap<Integer, Integer> nmap = new HashMap<>();
        for (int i=0; i<getVertexCount(); i++)
        {
            int vid = getVertexList().get(i).getVertexId();
            int nvid = -vid;
            nmap.put(vid, nvid);

            getVertexList().get(i).setVertexId(nvid);

            for (int j=0; j<getEdgeCount(); j++)
            {
                DENOPTIMEdge e = getEdgeList().get(j);
                if (e.getSrcVertex() == vid) {
                    e.getSrcAP().getOwner().setVertexId(nvid);
                }
                if (e.getTrgVertex() == vid) {
                    e.getTrgAP().getOwner().setVertexId(nvid);
                }
            }
        }
        Iterator<SymmetricSet> iter = getSymSetsIterator();
        while (iter.hasNext())
        {
            SymmetricSet ss = iter.next();
            for (int i=0; i<ss.getList().size(); i++)
            {
                ss.getList().set(i,nmap.get(ss.getList().get(i)));
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Reassign vertex IDs to all vertexes of this graph. The old IDs are stored 
     * in the vertex property {@link DENOPTIMConstants#STOREDVID}.
     * @throws DENOPTIMException if there are inconsistencies in the vertex IDs
     * used to refer to this graph's vertexes.
     */

    public void renumberGraphVertices() throws DENOPTIMException
    {
        renumberVerticesGetMap();
    }

//------------------------------------------------------------------------------

    /**
     * Reassign vertex IDs to a graph. The old IDs are stored 
     * in the vertex property {@link DENOPTIMConstants#STOREDVID}.
     * @return map with old IDs as key and new IDs as values.
     * @throws DENOPTIMException if there are inconsistencies in the vertex IDs
     * used to refer to this graph's vertexes.
     */

    public Map<Integer,Integer> renumberVerticesGetMap() throws DENOPTIMException 
    {
        Map<Integer, Integer> nmap = new HashMap<>();

        // for the vertices in the graph, get new vertex ids
        for (int i=0; i<getVertexCount(); i++)
        {
            DENOPTIMVertex v = getVertexList().get(i);
            int vid = v.getVertexId();
            int nvid = GraphUtils.getUniqueVertexIndex();

            nmap.put(vid, nvid);

            v.setVertexId(nvid);
            v.setProperty(DENOPTIMConstants.STOREDVID, vid);
        }

        // Update the sets of symmetric vertex IDs
        Iterator<SymmetricSet> iter = getSymSetsIterator();
        while (iter.hasNext())
        {
            SymmetricSet ss = iter.next();
            for (int i=0; i<ss.getList().size(); i++)
            {
                if (!nmap.containsKey(ss.getList().get(i)))
                {
                    DENOPTIMException e = new DENOPTIMException("Assumption "
                            + "violated: vertex IDs in "
                            + "symmetric set are out of sync w.r.t. actual "
                            + "vertex IDs. Report bug!");
                    throw e;
                }
                ss.getList().set(i,nmap.get(ss.getList().get(i)));
            }
        }

        return nmap;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Calculates the level of a vertex in this graph.
     * @param v the vertex for which we want the level.
     * @return the level, i.e., an integer that is -1 for the seed vertex of
     * this graph and increases by one unit per each edge that has to be 
     * traversed to reach the vertex given as argument via a direct path.
     */
    
    public int getLevel(DENOPTIMVertex v)
    {
        ArrayList<DENOPTIMVertex> parentTree = new ArrayList<>();
        getParentTree(v,parentTree);
        return parentTree.size() - 1;
    }

//------------------------------------------------------------------------------

    /**
     * Finalizes a candidate chemical entity by evaluating its
     * <code>DENOPTIMGraph</code> and assigning preliminary molecular,
     * SMILES and INCHI representations.
     * The chemical entity is evaluated also against the
     * criteria defined by <code>FragmentSpaceParameters</code> and
     * <code>RingClosureParameters</code>.
     * <b>WARNING</b> Although Cartesian coordinates are assigned to each atom
     * and pseudo-atom in the molecular representation ,
     * such coordinates do <b>NOT</b> represent a valid 3D model.
     * As a consequence stereochemical descriptors in the INCHI representation
     * are not consistent with the actual arrangement of fragments.
     * @return an object array containing the inchi code, the SMILES string
     *         and the 2D representation of the molecule.
     *         <code>null</code> is returned if any check or conversion fails.
     * @throws DENOPTIMException
     */

    public Object[] evaluateGraph()
            throws DENOPTIMException
    {
        // calculate the molecule representation
        ThreeDimTreeBuilder t3d = new ThreeDimTreeBuilder();
        t3d.setAlidnBBsIn3D(false);
        IAtomContainer mol = t3d.convertGraphTo3DAtomContainer(this,true);
        if (mol == null)
        {
            String msg ="Evaluation of graph: graph-to-mol returned null!"
                    + toString();
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            return null;
        }

        // check if the molecule is connected
        boolean isConnected = ConnectivityChecker.isConnected(mol);
        if (!isConnected)
        {
            String msg = "Evaluation of graph: Not all connected"
                    + toString();
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            return null;
        }

        // SMILES
        String smiles = null;
        smiles = DENOPTIMMoleculeUtils.getSMILESForMolecule(mol);
        if (smiles == null)
        {
            String msg = "Evaluation of graph: SMILES is null! "
                    + toString();
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            smiles = "FAIL: NO SMILES GENERATED";
        }
        // if by chance the smiles indicates a disconnected molecule
        if (smiles.contains("."))
        {
            String msg = "Evaluation of graph: SMILES contains \".\"" + smiles;
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            return null;
        }

        // criteria from definition of Fragment space
        // 1A) number of heavy atoms
        if (FragmentSpaceParameters.getMaxHeavyAtom() > 0)
        {
            if (DENOPTIMMoleculeUtils.getHeavyAtomCount(mol) >
                    FragmentSpaceParameters.getMaxHeavyAtom())
            {
                String msg = "Evaluation of graph: Max atoms constraint "
                        + " violated: " + smiles;
                DENOPTIMLogger.appLogger.log(Level.INFO, msg);
                return null;
            }
        }

        // 1B) molecular weight
        double mw = DENOPTIMMoleculeUtils.getMolecularWeight(mol);
        if (FragmentSpaceParameters.getMaxMW() > 0)
        {
            if (mw > FragmentSpaceParameters.getMaxMW())
            {
                String msg = "Evaluation of graph: Molecular weight "
                        + "constraint violated: " + smiles + " | MW: " + mw;
                DENOPTIMLogger.appLogger.log(Level.INFO, msg);
                return null;
            }
        }
        mol.setProperty("MOL_WT", mw);

        // 1C) number of rotatable bonds
        int nrot = DENOPTIMMoleculeUtils.getNumberOfRotatableBonds(mol);
        if (FragmentSpaceParameters.getMaxRotatableBond() > 0)
        {
            if (nrot > FragmentSpaceParameters.getMaxRotatableBond())
            {
                String msg = "Evaluation of graph: Max rotatable bonds "
                        + "constraint violated: "+ smiles;
                DENOPTIMLogger.appLogger.log(Level.INFO, msg);
                return null;
            }
        }
        mol.setProperty("ROT_BND", nrot);

        // 1D) unacceptable free APs
        if (FragmentSpace.useAPclassBasedApproach())
        {
            if (hasForbiddenEnd())
            {
                String msg = "Evaluation of graph: forbidden end in graph!";
                DENOPTIMLogger.appLogger.log(Level.INFO, msg);
                return null;
            }
        }

        // criteria from settings of ring closures
        if (RingClosureParameters.allowRingClosures())
        {
            // Count rings and RCAs
            int nPossRings = 0;
            Set<String> doneType = new HashSet<>();
            Map<String,String> rcaTypes = DENOPTIMConstants.RCATYPEMAP;
            for (String rcaTyp : rcaTypes.keySet())
            {
                if (doneType.contains(rcaTyp))
                {
                    continue;
                }

                int nThisType = 0;
                int nCompType = 0;
                for (IAtom atm : mol.atoms())
                {
                    if (atm.getSymbol().equals(rcaTyp))
                    {
                        nThisType++;
                    }
                    else if (atm.getSymbol().equals(rcaTypes.get(rcaTyp)))
                    {
                        nCompType++;
                    }
                }

                // check number of rca per type
                if (nThisType > RingClosureParameters.getMaxRcaPerType() ||
                        nCompType > RingClosureParameters.getMaxRcaPerType())
                {
                    String msg = "Evaluation of graph: too many RCAs! "
                            + rcaTyp + ":" + nThisType + " "
                            + rcaTypes.get(rcaTyp) + ":" + nCompType;
                    DENOPTIMLogger.appLogger.log(Level.INFO, msg);
                    return null;
                }
                if (nThisType < RingClosureParameters.getMinRcaPerType() ||
                        nCompType < RingClosureParameters.getMinRcaPerType())
                {
                    String msg = "Evaluation of graph: too few RCAs! "
                            + rcaTyp + ":" + nThisType + " "
                            + rcaTypes.get(rcaTyp) + ":" + nCompType;
                    DENOPTIMLogger.appLogger.log(Level.INFO, msg);
                    return null;
                }

                nPossRings = nPossRings + Math.min(nThisType, nCompType);
                doneType.add(rcaTyp);
                doneType.add(rcaTypes.get(rcaTyp));
            }
            if (nPossRings < RingClosureParameters.getMinRingClosures())
            {
                String msg = "Evaluation of graph: too few ring candidates";
                DENOPTIMLogger.appLogger.log(Level.INFO, msg);
                return null;
            }
        }

        // get the smiles/Inchi representation
        ObjectPair pr = DENOPTIMMoleculeUtils.getInChIForMolecule(mol);
        if (pr.getFirst() == null)
        {
            String msg = "Evaluation of graph: INCHI is null!";
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            pr.setFirst("UNDEFINED_INCHI");
        }

        Object[] res = new Object[3];
        res[0] = pr.getFirst(); // inchi
        res[1] = smiles; // smiles
        res[2] = mol;

        return res;
    }

//------------------------------------------------------------------------------

    /**
     * Evaluates the possibility of closing rings in this graph and
     * generates all alternative graphs resulting by different combinations of
     * rings
     * @return <code>true</code> unless no ring can be set up even if required
     * @throws denoptim.exception.DENOPTIMException
     */
    public ArrayList<DENOPTIMGraph> makeAllGraphsWithDifferentRingSets()
            throws DENOPTIMException {
        ArrayList<DENOPTIMGraph> lstGraphs = new ArrayList<>();

        boolean rcnEnabled = FragmentSpace.useAPclassBasedApproach();
        if (!rcnEnabled)
            return lstGraphs;

        boolean evaluateRings = RingClosureParameters.allowRingClosures();
        if (!evaluateRings)
            return lstGraphs;

        // get a atoms/bonds molecular representation (no 3D needed)
        ThreeDimTreeBuilder t3d = new ThreeDimTreeBuilder();
        t3d.setAlidnBBsIn3D(false);
        IAtomContainer mol = t3d.convertGraphTo3DAtomContainer(this,false);

        // Set rotatable property as property of IBond
        RotationalSpaceUtils.defineRotatableBonds(mol,
                        FragmentSpaceParameters.getRotSpaceDefFile(),
                        true, true);

        // get the set of possible RCA combinations = ring closures
        CyclicGraphHandler cgh = new CyclicGraphHandler();
        ArrayList<List<DENOPTIMRing>> allCombsOfRings =
                cgh.getPossibleCombinationOfRings(mol, this);

        // Keep closable chains that are relevant for chelate formation
        if (RingClosureParameters.buildChelatesMode())
        {
            ArrayList<List<DENOPTIMRing>> toRemove = new ArrayList<>();
            for (List<DENOPTIMRing> setRings : allCombsOfRings)
            {
                if (!cgh.checkChelatesGraph(this,setRings))
                {
                    toRemove.add(setRings);
                }
            }
            allCombsOfRings.removeAll(toRemove);
        }

        // prepare output graphs
        for (List<DENOPTIMRing> ringSet : allCombsOfRings)
        {
            // clone root graph
            DENOPTIMGraph newGraph = this.clone();

            Map<Integer,Integer> vRenum = newGraph.renumberVerticesGetMap();
            newGraph.setGraphId(GraphUtils.getUniqueGraphIndex());

            // add rings
            for (DENOPTIMRing oldRing : ringSet)
            {
                DENOPTIMRing newRing = new DENOPTIMRing();
                for (int i=0; i<oldRing.getSize(); i++)
                {
                    int oldVId = oldRing.getVertexAtPosition(i).getVertexId();
                    int newVId = vRenum.get(oldVId);
                    newRing.addVertex(newGraph.getVertexWithId(newVId));
                }
                newRing.setBondType(oldRing.getBondType());
                newGraph.addRing(newRing);
            }

            // store
            lstGraphs.add(newGraph);
        }

        return lstGraphs;
    }

//------------------------------------------------------------------------------

    /**
     * Check if there are forbidden ends: free attachment points that are not
     * suitable for capping and not allowed to stay unused.
     *
     * @return <code>true</code> if a forbidden end is found
     */

    public boolean hasForbiddenEnd()
    {
        ArrayList<DENOPTIMVertex> vertices = getVertexList();
        Set<APClass> classOfForbEnds = FragmentSpace.getForbiddenEndList();
        boolean found = false;
        for (DENOPTIMVertex vtx : vertices)
        {
            ArrayList<DENOPTIMAttachmentPoint> daps = vtx.getAttachmentPoints();
            for (DENOPTIMAttachmentPoint dp : daps)
            {
                if (dp.isAvailable())
                {
                    APClass apClass = dp.getAPClass();
                    if (classOfForbEnds.contains(apClass))
                    {
                        found = true;
                        String msg = "Forbidden free AP for Vertex: "
                                + vtx.getVertexId() + " "
                                + vtx.toString()
                                + "\n"+ this +" \n "
                                + " AP class: " + apClass;
                        DENOPTIMLogger.appLogger.log(Level.WARNING, msg);
                        break;
                    }
                }
            }
            if (found)
            {
                break;
            }
        }

        return found;
    }

//------------------------------------------------------------------------------

    /**
     * Append a graph (incoming=I) onto this (receiving=R).
     * Can append one or more copies of the same graph. The corresponding
     * vertex and attachment point ID for each connection are given in
     * separated arrays.
     * @param parentVertices the list of source vertices of R on which a copy
     * of I is to be attached.
     * @param parentAPIdx the list of attachment points on R's vertices to be
     * used to attach I
     * @param subGraph the incoming graph I, or child
     * @param childVertex the vertex of I that is to be connected to R
     * @param childAPIdx the index of the attachment point on the vertex of I
     * that is to be connected to R
     * @param bndType the bond type between R and I
     * @param onAllSymmAPs set to <code>true</code> to require the same graph I
     * to be attached on all available and symmetric APs on the same vertex of
     * the AP indicated in the list.
     */

    public void appendGraphOnGraph(ArrayList<DENOPTIMVertex> parentVertices,
                                   ArrayList<Integer> parentAPIdx,
                                   DENOPTIMGraph subGraph,
                                   DENOPTIMVertex childVertex, int childAPIdx,
                                   BondType bndType, boolean onAllSymmAPs) 
                                           throws DENOPTIMException
    {
        // Collector for symmetries created by appending copies of subGraph
        Map<Integer,SymmetricSet> newSymSets = new HashMap<>();

        // Repeat append for each parent vertex while collecting symmetries
        for (int i=0; i<parentVertices.size(); i++)
        {
            appendGraphOnGraph(parentVertices.get(i), parentAPIdx.get(i),
                    subGraph, childVertex, childAPIdx, bndType,
                    newSymSets, onAllSymmAPs);
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Append a vertex to this graph: adds the new vertex to the list of 
     * vertexes belonging to the graph, and adds the resulting edge to the graph.
     * Does not clone the incoming vertex.
     * Does not project on symmetrically related vertices or
     * attachment points. No change in symmetric sets, apart from importing
     * those sets that are already defined in the incoming vertex.
     * @param srcAP the attachment point on this graph (source AP).
     * @param trgAP the attachment point on the incoming vertex (target AP).
     * @throws DENOPTIMException 
     */

    public void appendVertexOnAP(DENOPTIMAttachmentPoint srcAP, 
            DENOPTIMAttachmentPoint trgAP) throws DENOPTIMException 
    {
        if (!srcAP.isAvailable())
        {
            throw new DENOPTIMException("Attempt to use unavailable attachment "
                    + "point " + srcAP + " on vertex " 
                    + srcAP.getOwner().getVertexId());
        }
        if ( !trgAP.isAvailable())
        {
            throw new DENOPTIMException("Attempt to use unavailable attachment "
                    + "point " + trgAP + " on vertex " 
                    + trgAP.getOwner().getVertexId());
        }
        
        BondType btSrc = srcAP.getBondType();
        BondType btTrg = trgAP.getBondType();
        BondType bndTyp = btSrc;
        if (btSrc != btTrg)
        {
            if (btSrc == BondType.ANY && btTrg != BondType.ANY)
            {
                bndTyp = btTrg;
            } else {
                if (btSrc != BondType.ANY && btTrg == BondType.ANY)
                {
                    bndTyp = btSrc;
                } else {
                    bndTyp = BondType.UNDEFINED;
                }
            }
        }

        this.addVertex(trgAP.getOwner());
        DENOPTIMEdge edge = new DENOPTIMEdge(srcAP,trgAP, bndTyp);
        addEdge(edge);
    }
    
//------------------------------------------------------------------------------

    /**
     * Appends a graph onto this graph. The operation is not projected by 
     * symmetry, and the incoming graph is not cloned/copied.
     * @param apOnThisGraph where to append to on this growing graph.
     * @param apOnIncomingGraph where to connect this graph into the incoming one.
     * @param bndType the bond type of the new edge created.
     * @throws DENOPTIMException
     */

    public void appendGraphOnAP(DENOPTIMAttachmentPoint apOnThisGraph,
            DENOPTIMAttachmentPoint apOnIncomingGraph, BondType bndType)
                                        throws DENOPTIMException
    {
        DENOPTIMGraph incomingGraph = apOnIncomingGraph.getOwner().getGraphOwner();
        incomingGraph.renumberGraphVertices();

        DENOPTIMEdge edge = new DENOPTIMEdge(apOnThisGraph, apOnIncomingGraph, 
                bndType);
        addEdge(edge);
        
        //Import vertexes
        for (DENOPTIMVertex incomingVrtx : incomingGraph.getVertexList())
        {
            addVertex(incomingVrtx);
        }
        
        //Import edges
        for (DENOPTIMEdge incomingEdge : incomingGraph.getEdgeList())
        {
            addEdge(incomingEdge);
        }
        
        //Import rings
        for (DENOPTIMRing incomingRing : incomingGraph.getRings())
        {
            addRing(incomingRing);
        }
        
        incomingGraph.cleanup();
    }

//------------------------------------------------------------------------------

    /**
     * Append a subgraph (I) to this graph (R) specifying
     * which vertex and attachment point to use for the connection.
     * Does not project on symmetrically related vertices or
     * attachment points. No change in symmetric sets, apart from importing
     * those sets that are already defined in the subgraph.
     * @param parentVertex the vertex of R on which a copy
     * of I is to be attached.
     * @param parentAPIdx the attachment point on R's vertex to be
     * used to attach I
     * @param subGraph the incoming graph I, or child graph.
     * @param childVertex the vertex of I that is to be connected to R
     * @param childAPIdx the index of the attachment point on the vertex of I
     * that is to be connected to R
     * @param bndType the bond type between R and I
     * @param newSymSets of symmetric sets. This parameter is only used to keep
     * track of the symmetric copies of I. Simply provide an empty data
     * structure.
     */

    public void appendGraphOnAP(DENOPTIMVertex parentVertex, int parentAPIdx,
                                DENOPTIMGraph subGraph,
                                DENOPTIMVertex childVertex, int childAPIdx,
                                BondType bndType,
                                Map<Integer,SymmetricSet> newSymSets)
                                        throws DENOPTIMException
    {
        // Clone and renumber the subgraph to ensure uniqueness
        DENOPTIMGraph sgClone = subGraph.clone();
        // The clones have the same vertex IDs before renumbering vertices
        DENOPTIMVertex cvClone = sgClone.getVertexWithId(
                childVertex.getVertexId());
        sgClone.renumberGraphVertices();

        DENOPTIMEdge edge = new DENOPTIMEdge(parentVertex.getAP(parentAPIdx),
                cvClone.getAP(childAPIdx), bndType);
        addEdge(edge);

        // Import all vertices from cloned subgraph, i.e., sgClone
        for (int i=0; i<sgClone.getVertexList().size(); i++)
        {
            DENOPTIMVertex clonV = sgClone.getVertexList().get(i);
            DENOPTIMVertex origV = subGraph.getVertexList().get(i);

            addVertex(sgClone.getVertexList().get(i));

            // also need to tmp store pointers to symmetric vertices
            // Why is this working on subGraph and not on sgClone?
            // Since we are only checking is there is symmetry, there should be
            // no difference between doing it on sgClone or subGraph.
            if (subGraph.hasSymmetryInvolvingVertex(origV))
            {
                if (newSymSets.containsKey(origV.getVertexId()))
                {
                    newSymSets.get(origV.getVertexId()).add(
                            clonV.getVertexId());
                }
                else
                {
                    newSymSets.put(origV.getVertexId(),
                            sgClone.getSymSetForVertexID(
                                    sgClone.getVertexList().get(i)
                                    .getVertexId()));
                }
            }
            else
            {
                if (newSymSets.containsKey(origV.getVertexId()))
                {
                    newSymSets.get(origV.getVertexId()).add(
                            clonV.getVertexId());
                }
                else
                {
                    SymmetricSet ss = new SymmetricSet();
                    ss.add(clonV.getVertexId());
                    newSymSets.put(origV.getVertexId(),ss);
                }
            }
        }
        // Import all edges from cloned subgraph
        for (int i=0; i<sgClone.getEdgeList().size(); i++)
        {
            addEdge(sgClone.getEdgeList().get(i));
        }
        // Import all rings from cloned subgraph
        for (int i=0; i<sgClone.getRings().size(); i++)
        {
            addRing(sgClone.getRings().get(i));
        }

        // project tmp symmetric set into final symmetric sets
        Set<SymmetricSet> doneTmpSymSets = new HashSet<SymmetricSet>();
        for (Map.Entry<Integer,SymmetricSet> e : newSymSets.entrySet())
        {
            SymmetricSet tmpSS = e.getValue();
            if (doneTmpSymSets.contains(tmpSS))
            {
                continue;
            }
            doneTmpSymSets.add(tmpSS);
            boolean done = false;
            // NB: no need to check all entries of tmpSS: the first is enough
            SymmetricSet oldSS;
            Iterator<SymmetricSet> iter = getSymSetsIterator();
            while (iter.hasNext())
            {
                oldSS = iter.next();
                if (oldSS.contains(tmpSS.getList().get(0)))
                {
                    done = true;
                    for (Integer symVrtID : tmpSS.getList())
                    {
                        // NB this adds only if not already contained
                        oldSS.add(symVrtID);
                    }
                    break;
                }
            }
            if (!done)
            {
                if (tmpSS.size() <= 1)
                {
                    // tmpSS has always at least one entry: the initial vrtId
                    continue;
                }
                //Move tmpSS into a new SS on molGraph
                SymmetricSet newSS = new SymmetricSet();
                for (Integer symVrtID : tmpSS.getList())
                {
                    newSS.add(symVrtID);
                }
                addSymmetricSetOfVertices(newSS);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Append a graph (incoming=I) onto this graph (receiving=R).
     * @param parentVertex the vertex of R on which the a copy
     * of I is to be attached.
     * @param parentAPIdx the attachment point on R's vertex to be
     * used to attach I
     * @param subGraph the incoming graph I, or child
     * @param childVertex the vertex of I that is to be connected to R
     * @param childAPIdx the index of the attachment point on the vertex of I
     * that is to be connected to R
     * @param bndType the bond type between R and I
     * @param newSymSets this parameter is only used to keep track
     * of the symmetric copies of I. Simply provide an empty data structure.
     * @param onAllSymmAPs set to <code>true</code> to require the same graph I
     * to be attached on all available and symmetric APs on the same vertex of
     * the AP indicated in the list.
     */

    public void appendGraphOnGraph(DENOPTIMVertex parentVertex,
                                   int parentAPIdx, DENOPTIMGraph subGraph,
                                   DENOPTIMVertex childVertex, int childAPIdx,
                                   BondType bndType,
                                   Map<Integer,SymmetricSet> newSymSets,
                                   boolean onAllSymmAPs)
                                           throws DENOPTIMException
    {
        SymmetricSet symAPs = parentVertex.getSymmetricAPs(parentAPIdx);
        if (symAPs != null && onAllSymmAPs)
        {
            ArrayList<Integer> apLst = symAPs.getList();
            for (int idx : apLst) {
                if (!parentVertex.getAttachmentPoints().get(idx).isAvailable())
                {
                    continue;
                }
                appendGraphOnAP(parentVertex, idx, subGraph, childVertex,
                        childAPIdx, bndType, newSymSets);
            }
        }
        else
        {
            appendGraphOnAP(parentVertex, parentAPIdx, subGraph, childVertex,
                    childAPIdx, bndType, newSymSets);
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Search a graph for vertices that match the criteria defined in a query
     * vertex.
     * @param query the query
     * @param verbosity the verbosity level
     * @return the list of matches
     */
    public ArrayList<Integer> findVerticesIds(
            VertexQuery query,
            int verbosity)
    {
        ArrayList<Integer> matches = new ArrayList<>();
        for (DENOPTIMVertex v : findVertices(query, verbosity))
        {
            matches.add(v.getVertexId());
        }
        return matches;
    }

//-----------------------------------------------------------------------------

    /**
     * Filters a list of vertices according to a query.
     * vertex.
     * @param vrtxQuery the query defining what is that we want to find.
     * @param verbosity the verbosity level.
     * @return the list of vertexes that match the query.
     */

    public ArrayList<DENOPTIMVertex> findVertices(
            VertexQuery vrtxQuery,
            int verbosity)
    {
        ArrayList<DENOPTIMVertex> matches = new ArrayList<>(getVertexList());

        if (verbosity > 1)
        {
            System.out.println("Candidates: " + matches);
        }

        //Check condition vertex ID
        Integer vidQuery = vrtxQuery.getVertexIDQuery();
        if (vidQuery != null)
        {
            ArrayList<DENOPTIMVertex> newLst = new ArrayList<>();
            for (DENOPTIMVertex v : matches)
            {
                if (v.getVertexId() == vidQuery.intValue())
                {
                    newLst.add(v);
                }
            }
            matches = newLst;
        }
        if (verbosity > 1)
        {
            System.out.println("  After filtering by vertex ID: " + matches);
        }
        
        //Check condition vertex type (NB: essentially the vertex implementation
        VertexType vtQuery = vrtxQuery.getVertexTypeQuery();
        if (vtQuery != null)
        {
            ArrayList<DENOPTIMVertex> newLst = new ArrayList<>();
            for (DENOPTIMVertex v : matches)
            {
                if (v.getVertexType() == vtQuery)
                {
                    newLst.add(v);
                }
            }
            matches = newLst;
            if (verbosity > 2)
            {
                System.out.println("  After filtering by vertex type: "
                        + matches);
            }
        }
        
        //Check condition building block Type
        BBType bbtQuery = vrtxQuery.getVertexBBTypeQuery();
        if (bbtQuery != null)
        {
            ArrayList<DENOPTIMVertex> newLst = new ArrayList<>();
            for (DENOPTIMVertex v : matches)
            {
                if (v.getBuildingBlockType() == bbtQuery)
                {
                    newLst.add(v);
                }
            }
            matches = newLst;
            if (verbosity > 2)
            {
                System.out.println("  After filtering by building block "
                        + "type: " + matches);
            }
        }
        
        //Check condition building block ID
        Integer bbID = vrtxQuery.getVertexBBIDQuery();
        if (bbID != null)
        {
            ArrayList<DENOPTIMVertex> newLst = new ArrayList<>();
            for (DENOPTIMVertex v : matches)
            {   
                if (v.getBuildingBlockId() == bbID.intValue())
                {
                    newLst.add(v);
                }
            }
            matches = newLst;
            if (verbosity > 2)
            {
                System.out.println("  After filtering by building block ID: " 
                        + matches);
            }
        } 

        //Check condition: level of vertex
        Integer levelQuery = vrtxQuery.getVertexLevelQuery();
        if (levelQuery != null)
        {
            ArrayList<DENOPTIMVertex> newLst = new ArrayList<DENOPTIMVertex>();
            for (DENOPTIMVertex v : matches)
            {
                if (getLevel(v) == levelQuery)
                {
                    newLst.add(v);
                }
            }
            matches = newLst;
            if (verbosity > 2)
            {
                System.out.println("  After filtering by level: " + matches);
            }
        }
        
        if (verbosity > 1)
        {
            System.out.println("After all vertex-based filters: " + matches);
        }

        List<EdgeQuery> inAndOutEdgeQueries = new ArrayList<>();
        inAndOutEdgeQueries.add(vrtxQuery.getInEdgeQuery());
        inAndOutEdgeQueries.add(vrtxQuery.getOutEdgeQuery());
        for (int i=0; i<2; i++) 
        {
            String inOrOut = "";
            if (i==0)
                inOrOut = "incoming";
            else 
                inOrOut = "ourgoing";
            
            EdgeQuery edgeQuery = inAndOutEdgeQueries.get(i);
            if (edgeQuery == null)
            {
                continue;
            }
            
            EdgeFinder edgeFinder = new EdgeFinder(i-1);
            
            Integer eTrgApIDx = edgeQuery.getTargetAPIdx();
            if (eTrgApIDx != null)
            {
                ArrayList<DENOPTIMVertex> newLst = new ArrayList<>();
                for (DENOPTIMVertex v : matches)
                {
                    for (DENOPTIMEdge e : edgeFinder.apply(v))
                    {
                        if (e.getTrgAPID() == eTrgApIDx)
                        {
                            newLst.add(v);
                            break;
                        }
                    }
                }
                matches = newLst;
                if (verbosity > 2)
                {
                    System.out.println("  After " + inOrOut 
                            + " edge trgAPID filter: " + matches);
                }
            }
            
            Integer eInSrcApIDx = edgeQuery.getSourceAPIdx();
            if (eInSrcApIDx != null)
            {
                ArrayList<DENOPTIMVertex> newLst = new ArrayList<>();
                for (DENOPTIMVertex v : matches)
                {
                    for (DENOPTIMEdge e : edgeFinder.apply(v))
                    {
                        if (e != null && e.getSrcAPID() == eInSrcApIDx)
                        {
                            newLst.add(v);
                            break;
                        }
                    }
                }
                matches = newLst;
                if (verbosity > 2)
                {
                    System.out.println("  After " + inOrOut 
                            + " edge srcAPID filter: " + matches);
                }
            }
            
            if (i==0)
            {
                Integer eSrcVrtID = edgeQuery.getSourceVertexId();
                if (eSrcVrtID != null)
                {
                    ArrayList<DENOPTIMVertex> newLst = new ArrayList<>();
                    for (DENOPTIMVertex v : matches)
                    {
                        for (DENOPTIMEdge e : edgeFinder.apply(v))
                        {
                            if(e.getSrcAP().getOwner().getVertexId()==eSrcVrtID)
                            {
                                newLst.add(v);
                                break;
                            }
                        }
                    }
                    matches = newLst;
                    if (verbosity > 2)
                    {
                        System.out.println("  After " + inOrOut 
                                + " edge src VertexID filter: " + matches);
                    }
                }
            } else if (i==1) {
                Integer eTrgVrtID = edgeQuery.getTargetVertexId();
                if (eTrgVrtID != null)
                {
                    ArrayList<DENOPTIMVertex> newLst = new ArrayList<>();
                    for (DENOPTIMVertex v : matches)
                    {
                        for (DENOPTIMEdge e : edgeFinder.apply(v))
                        {
                            if(e.getTrgAP().getOwner().getVertexId()==eTrgVrtID)
                            {
                                newLst.add(v);
                                break;
                            }
                        }
                    }
                    matches = newLst;
                    if (verbosity > 2)
                    {
                        System.out.println("  After " + inOrOut 
                                + " edge trg VertexID filter: " + matches);
                    }
                }
            }

            BondType btQuery = edgeQuery.getBondType();
            if (btQuery != null)
            {
                ArrayList<DENOPTIMVertex> newLst = new ArrayList<>();
                for (DENOPTIMVertex v : matches)
                {
                    for (DENOPTIMEdge e : edgeFinder.apply(v))
                    {
                        if (e.getBondType() == btQuery)
                        {
                            newLst.add(v);
                            break;
                        }
                    }
                }
                matches = newLst;
                if (verbosity > 2)
                {
                    System.out.println("  After " + inOrOut 
                            + " edge bond type filter: " + matches);
                }
            }

            APClass srcAPC = edgeQuery.getSourceAPClass();
            if (srcAPC != null)
            {
                ArrayList<DENOPTIMVertex> newLst = new ArrayList<>();
                for (DENOPTIMVertex v : matches)
                {
                    for (DENOPTIMEdge e : edgeFinder.apply(v))
                    {
                        if (e.getSrcAPClass().equals(srcAPC))
                        {
                            newLst.add(v);
                            break;
                        }
                    }
                }
                matches = newLst;
            }
            
            APClass trgAPC = edgeQuery.getTargetAPClass();
            if (trgAPC != null)
            {
                ArrayList<DENOPTIMVertex> newLst = new ArrayList<>();
                for (DENOPTIMVertex v : matches)
                {
                    for (DENOPTIMEdge e : edgeFinder.apply(v))
                    {
                        if (e.getTrgAPClass().equals(trgAPC))
                        {
                            newLst.add(v);
                            break;
                        }
                    }
                }
                matches = newLst;
            }
        
            if (verbosity > 1)
            {
                System.out.println("After all " + inOrOut 
                        + " edge-based filters: " + matches);
            }
        }
    
        // Identify symmetric sets and keep only one member
        removeSymmetryRedundance(matches);

        if (verbosity > 1)
        {
            System.out.println("Final Matches (after symmetry): " + matches);
        }

        return matches;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Utility to make selection of edges to a vertex tunable by a parameter.
     * The parameter given to the constructor defines whether we take the 
     * incoming or the outgoing edges of a vertex that is given as the argument
     * of this function.
     */
    private class EdgeFinder implements Function<DENOPTIMVertex,List<DENOPTIMEdge>> 
    {
        private int mode = 0;
        
        /**
         * @param i if positive we focus on departing edges, if negative we 
         * focus on the incoming edge.
         */
        public EdgeFinder(int i)
        {
            mode = i;   
        }
        
        @Override
        public List<DENOPTIMEdge> apply(DENOPTIMVertex v)
        {
            List<DENOPTIMEdge> edges = new ArrayList<DENOPTIMEdge>();
            if (mode < 0)
            {
                DENOPTIMEdge eToParent = v.getEdgeToParent();
                if (eToParent != null)
                    edges.add(eToParent);
            } else {
                for (DENOPTIMAttachmentPoint ap : v.getAttachmentPoints())
                {
                    if (!ap.isAvailable() && ap.isSrcInUser())
                    {
                        edges.add(ap.getEdgeUser());
                    }
                }
            }
            return edges;
        }
    }
    
//-----------------------------------------------------------------------------

    /**
     * Remove all but one of the symmetry-related partners in a list of 
     * vertexes. The vertices must belong to this graph.
     * @param list vertices to be purged.
     */

    public void removeSymmetryRedundance(ArrayList<DENOPTIMVertex> list) {
        ArrayList<DENOPTIMVertex> symRedundant = new ArrayList<>();
        Iterator<SymmetricSet> itSymm = getSymSetsIterator();
        while (itSymm.hasNext())
        {
            SymmetricSet ss = itSymm.next();
            for (DENOPTIMVertex v : list)
            {
                int vid = v.getVertexId();
                if (symRedundant.contains(v))
                {
                    continue;
                }
                if (ss.contains(vid))
                {
                    for (Integer idVrtInSS : ss.getList())
                    {
                        if (idVrtInSS != vid)
                        {
                            symRedundant.add(getVertexWithId(idVrtInSS));
                        }
                    }
                }
            }
        }
        for (DENOPTIMVertex v : symRedundant)
        {
            list.remove(v);
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Remove all but one of the symmetry-related partners in a given list of
     * vertex IDs. The vertices must belong to this graph.
     * @param list the list of vertex IDs to be purged.
     */

    public void removeSymmetryRedundantIds(ArrayList<Integer> list) {
        ArrayList<DENOPTIMVertex> vList = new ArrayList<>();
        for (int vid : list) {
            vList.add(getVertexWithId(vid));
        }
        removeSymmetryRedundance(vList);
        list.clear();
        for (DENOPTIMVertex v : vList) {
            list.add(v.getVertexId());
        }
    }

//------------------------------------------------------------------------------

    /**
     * Edit this graph according to a given list of edit tasks.
     * @param edits the list of edit tasks.
     * @param symmetry if <code>true</code> the same operation is performed on
     * vertexes related by symmetry.
     * @param verbosity the verbosity level.
     * @return the modified graph.
     */

    public DENOPTIMGraph editGraph(ArrayList<DENOPTIMGraphEdit> edits,
            boolean symmetry, int verbosity) throws DENOPTIMException
    {

        //Make sure there is no clash with vertex IDs? This changes vertex IDs
        // and makes querying by vertex ID impossible. So, we don't do it,
        // and we must therefore assume the vertex IDs are good in the graph.
        /*
        int maxId = getMaxVertexId();
        GraphUtils.ensureVertexIDConsistency(maxId);
        */

        DENOPTIMGraph modGraph = this.clone();

        for (DENOPTIMGraphEdit edit : edits)
        {
            if (verbosity > 1)
            {
                System.out.println(" ");
                System.out.println("Graph edit task: " + edit.getType());
            }

            switch (edit.getType())
            {
                case REPLACECHILD:
                {
                    DENOPTIMGraph inGraph = edit.getIncomingGraph();
                    VertexQuery query = edit.getVertexQuery();
                    int idAPOnInGraph = -1; // Initialisation to invalid value
                    DENOPTIMVertex rootOfInGraph = null;
                    if (edit.getIncomingAPId() != null)
                    {
                        DENOPTIMAttachmentPoint ap = inGraph.getAPWithId(
                                edit.getIncomingAPId().intValue());
                        idAPOnInGraph = ap.getIndexInOwner();
                        rootOfInGraph = ap.getOwner();
                    } else {
                        ArrayList<DENOPTIMAttachmentPoint> freeAPs = 
                                inGraph.getAvailableAPs();
                        if (freeAPs.size()==1)
                        {
                            DENOPTIMAttachmentPoint ap = freeAPs.get(0);
                            idAPOnInGraph = ap.getIndexInOwner();
                            rootOfInGraph = ap.getOwner();
                        } else {
                            String geClsName = DENOPTIMGraphEdit.class.getSimpleName();
                            String msg = "Skipping " + edit.getType() + "on "
                                    + "graph " + getGraphId() + ". The incoming"
                                    + " graph has more than one free AP and "
                                    + "the " + geClsName + " "
                                    + "does not provide sufficient information "
                                    + "to unambiguously choose one AP. "
                                    + "Please, add 'idAPOnIncomingGraph' in "
                                    + "the definition of " + geClsName + ".";
                            System.out.println(msg);
                        }
                    }
                    
                    ArrayList<DENOPTIMVertex> matches = modGraph.findVertices(
                            query,verbosity);
                    if (symmetry)
                    {
                        modGraph.removeSymmetryRedundance(matches);
                    }
                    for (DENOPTIMVertex vertexToReplace : matches)
                    {
                        DENOPTIMEdge edgeToParent = 
                                vertexToReplace.getEdgeToParent();
                        if (edgeToParent == null)
                        {
                            //The matched vertex has no parent, therefore there
                            // the change would correspond to changing the graph 
                            // completely. This is unlikely the desired effect, 
                            //so we do not do anything.
                            continue;
                        }
                        DENOPTIMVertex parent = vertexToReplace.getParent();
                        int srcApId = edgeToParent.getSrcAPID();
                        
                        BondType bondType = edgeToParent.getBondType();
                        
                        modGraph.removeBranchStartingAt(vertexToReplace,symmetry);
                        
                        modGraph.appendGraphOnGraph(parent, srcApId, inGraph,
                                rootOfInGraph, idAPOnInGraph, bondType, 
                                new HashMap<Integer,SymmetricSet>(), symmetry);
                    }
                    break;
                }
                case DELETEVERTEX:
                {
                    ArrayList<DENOPTIMVertex> matches = modGraph.findVertices(
                            edit.getVertexQuery(), verbosity);
                    for (DENOPTIMVertex vertexToRemove : matches)
                    {
                        modGraph.removeBranchStartingAt(vertexToRemove,symmetry);
                    }
                    break;
                }
            }
        }
        return modGraph;
    }

//------------------------------------------------------------------------------

    /**
     * A list of mutation sites from within this graph.
     * @return the list of vertexes that allow any mutation type.
     */
    public List<DENOPTIMVertex> getMutableSites()
    {
        List<DENOPTIMVertex> mutableSites = new ArrayList<DENOPTIMVertex>();
        for (DENOPTIMVertex v : gVertices)
        {
            mutableSites.addAll(v.getMutationSites(
                    new ArrayList<MutationType>()));
        }
        return mutableSites;
    }
    
//------------------------------------------------------------------------------

    /**
     * A list of mutation sites from within this graph.
     * @param ignoredTypes a collection of mutation types to ignore. Vertexes
     * that allow only ignored types of mutation will
     * not be considered mutation sites.
     * @return the list of vertexes that allow any non-ignored mutation type.
     */
    public List<DENOPTIMVertex> getMutableSites(List<MutationType> ignoredTypes)
    {
        List<DENOPTIMVertex> mutableSites = new ArrayList<DENOPTIMVertex>();
        for (DENOPTIMVertex v : gVertices)
        {
            mutableSites.addAll(v.getMutationSites(ignoredTypes));
        }
        return mutableSites;
    }

//------------------------------------------------------------------------------

    /**
     * Produces a string that represents this graph and that adheres to the
     * JSON format.
     * @return the JSON format as a single string
     * @throws DENOPTIMException if the graph contains non-unique vertex IDs or
     * AP IDs. Uniqueness of identifiers is required to restore references upon
     * deserialization.
     */

    public String toJson() throws DENOPTIMException
    {
        //TODO: vertexID uniqueness should be guaranteed by the addVertex method
        // Therefore, this check should not be needed. Consider removal.
        //TODO: then the addVertex method should ensure also AP ID uniqueness.
        
        // Check for uniqueness of vertexIDs and APIDs within the
        // graph (ignore nested graphs).
        boolean regenerateVrtxID = false;
        boolean regenerateAP = false;
        Set<Integer> unqVrtxIDs = new HashSet<Integer>();
        Set<Integer> unqApIDs = new HashSet<Integer>();
        for (DENOPTIMVertex v : gVertices)
        {
            if (!unqVrtxIDs.add(v.getVertexId()))
            {
                regenerateVrtxID = true;
                /*
                throw new DENOPTIMException("Duplicate vertex ID '"
                        + v.getVertexId()
                        + "'. Cannot generate JSON string for graph: " + this);
                        */
            }
            for (DENOPTIMAttachmentPoint ap : v.getAttachmentPoints())
            {
                if (!unqApIDs.add(ap.getID()))
                {
                    regenerateAP = true;
                    break;
                    /*
                    throw new DENOPTIMException("Duplicate attachment point ID "
                            + "'" + ap.getID() + "'. "
                            + "Cannot generate JSON string for graph: " + this);
                            */
                }
            }
        }
        if (regenerateVrtxID)
        {
            this.renumberGraphVertices();
        }
        if (regenerateAP)
        {
            for (DENOPTIMVertex v : gVertices)
            {
                for (DENOPTIMAttachmentPoint ap : v.getAttachmentPoints())
                {
                    ap.setID(FragmentSpace.apID.getAndIncrement());
                }
            }
        }

        Gson gson = DENOPTIMgson.getWriter();
        String jsonOutput = gson.toJson(this);
        return jsonOutput;
    }

//------------------------------------------------------------------------------

    /**
     * Reads a JSON string and returns an instance of this class.
     * @param json the string to parse.
     * @return a new instance of this class.
     */

    public static DENOPTIMGraph fromJson(String json)
    {
        Gson gson = DENOPTIMgson.getReader();
        DENOPTIMGraph graph = gson.fromJson(json, DENOPTIMGraph.class);
        return graph;
    }

//------------------------------------------------------------------------------

    /**
     * Reads a JSON string and returns an instance of this class.
     * @param json the string to parse.
     * @return a new instance of this class.
     */

    public static DENOPTIMGraph fromJson(Reader reader)
    {
        Gson gson = DENOPTIMgson.getReader();
        DENOPTIMGraph graph = gson.fromJson(reader, DENOPTIMGraph.class);
        return graph;
    }

//------------------------------------------------------------------------------

    /**
     * We expect unique IDs for vertices and attachment points.
     */
    public static class DENOPTIMGraphSerializer
    implements JsonSerializer<DENOPTIMGraph>
    {
        @Override
        public JsonElement serialize(DENOPTIMGraph g, Type typeOfSrc,
                JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("graphId", g.graphId);
            jsonObject.add("gVertices", context.serialize(g.gVertices));
            jsonObject.add("gEdges", context.serialize(g.gEdges));
            jsonObject.add("gRings", context.serialize(g.gRings));
            jsonObject.add("symVertices", context.serialize(g.symVertices));
            return jsonObject;
        }
    }


    //------------------------------------------------------------------------------

    public static class DENOPTIMGraphDeserializer
    implements JsonDeserializer<DENOPTIMGraph>
    {
        @Override
        public DENOPTIMGraph deserialize(JsonElement json, Type typeOfT,
                JsonDeserializationContext context) throws JsonParseException
        {
            JsonObject jsonObject = json.getAsJsonObject();

            // First, build a graph with a graph ID and a list of vertices
            JsonObject partialJsonObj = new JsonObject();
            partialJsonObj.add("graphId", jsonObject.get("graphId"));
            partialJsonObj.add("gVertices", jsonObject.get("gVertices"));
            // Eventually, also the sym sets will become references... so
            // also for symVertices we'll have to go through the list and
            // rebuild the references.
            partialJsonObj.add("symVertices", jsonObject.get("symVertices"));

            Gson gson = new GsonBuilder()
                .setExclusionStrategies(new DENOPTIMExclusionStrategyNoAPMap())
                .registerTypeAdapter(DENOPTIMVertex.class,
                      new DENOPTIMVertexDeserializer())
                .registerTypeAdapter(APClass.class, new APClassDeserializer())
                .setPrettyPrinting()
                .create();

            DENOPTIMGraph graph = gson.fromJson(partialJsonObj,
                    DENOPTIMGraph.class);

            // Refresh APs
            for (DENOPTIMVertex v : graph.getVertexList())
            {
                // Regenerate reference to fragment owner
                v.setGraphOwner(graph);

                for (DENOPTIMAttachmentPoint ap : v.getAttachmentPoints())
                {
                    // Regenerate reference to AP owner
                    ap.setOwner(v);
                }
            }

            // Then, recover those members that are heavily based on references:
            // edges, rings.
            JsonArray edgeArr = jsonObject.get("gEdges").getAsJsonArray();
            for (JsonElement e : edgeArr)
            {
                JsonObject o = e.getAsJsonObject();
                DENOPTIMAttachmentPoint srcAP = graph.getAPWithId(
                        o.get("srcAPID").getAsInt());
                DENOPTIMAttachmentPoint trgAP = graph.getAPWithId(
                        o.get("trgAPID").getAsInt());

                DENOPTIMEdge edge = new DENOPTIMEdge(srcAP, trgAP,
                        context.deserialize(o.get("bondType"),BondType.class));
                graph.addEdge(edge);
            }

            // Now, recover the rings
            JsonArray ringArr = jsonObject.get("gRings").getAsJsonArray();
            for (JsonElement e : ringArr)
            {
                JsonObject o = e.getAsJsonObject();
                DENOPTIMRing ring = new DENOPTIMRing();
                for (JsonElement re : o.get("vertices").getAsJsonArray())
                {
                    ring.addVertex(graph.getVertexWithId(re.getAsInt()));
                }
                ring.setBondType(context.deserialize(o.get("bndTyp"),
                        BondType.class));
                graph.addRing(ring);
            }

            return graph;
        }
    }

//------------------------------------------------------------------------------

    /**
     * Update the graph so that the vertex argument is at the scaffold level
     * i.e. is the source of this graph. The vertex list of this graph
     * will also be reordered in a way that corresponds to the BFS.
     *
     * @param v vertex to set as scaffold
     */
    public static void setScaffold(DENOPTIMVertex v) {
        ArrayList<DENOPTIMVertex> newVertexList = new ArrayList<>();

        Set<Integer> visited = new HashSet<>();
        Queue<DENOPTIMVertex> currLevel = new ArrayDeque<>();
        Queue<DENOPTIMVertex> nextLevel = new ArrayDeque<>();
        currLevel.add(v);

        while (!currLevel.isEmpty()) {
            DENOPTIMVertex currVertex = currLevel.poll();

            int currId = currVertex.getVertexId();
            if (!visited.contains(currId)) {
                visited.add(currId);

                newVertexList.add(currVertex);

                Iterable<DENOPTIMVertex> neighbors = currVertex
                        .getAttachmentPoints()
                        .stream()
                        .map(DENOPTIMAttachmentPoint::getEdgeUser)
                        .filter(e -> e != null)
                        .map(e -> e.getSrcVertex() == currId ?
                                e.getTrgAP() : e.getSrcAP())
                        .map(DENOPTIMAttachmentPoint::getOwner)
                        .collect(Collectors.toList());
                for (DENOPTIMVertex adj : neighbors) {
                    nextLevel.add(adj);
                }
            }

            if (currLevel.isEmpty()) {
                currLevel = nextLevel;
                nextLevel = new ArrayDeque<>();
            }
        }

        v.getGraphOwner().setVertexList(newVertexList);
    }

//------------------------------------------------------------------------------

    /**
     * Checks if this graph contains a scaffold vertex.
     * @return true if there is a scaffold vertex.
     */
    public boolean hasScaffoldTypeVertex() {
        return getVertexList()
                .stream()
                .anyMatch(v -> v.getBuildingBlockType() == BBType.SCAFFOLD);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Sets the reference to a template that embeds this graph, i.e., this
     * graph's "jacket" template.
     * @param denoptimTemplate the jacket template
     */
    public void setTemplateJacket(DENOPTIMTemplate template)
    {
        this.templateJacket = template;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return the template that contains this graph or null.
     */
    public DENOPTIMTemplate getTemplateJacket()
    {
        return templateJacket;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return the outermost graph object that can be reached from this 
     * possibly embedded graph.
     */
    public DENOPTIMGraph getOutermostGraphOwner()
    {
        if (templateJacket == null)
            return this;
        else
            return templateJacket.getGraphOwner().getOutermostGraphOwner();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Find the path that one has to traverse to reach this graph from any 
     * template-embedding structure. In practice, for a graph that is embedded 
     * a template jacket <code>T1</code>, which is part of another graph which
     * is itself embedded in a template <code>T2</code>, which is again part of 
     * another graph, and so on it returns the references to the embedding 
     * templates stating from the outermost to the template that embeds this 
     * graph (Outermost reference come first in the resulting list). 
     * Instead a graph that is not embedded returns an empty path.
     * @return the path of references that allow to reach this graph from the
     * outermost level of embedding.
     */
    public List<DENOPTIMTemplate> getEmbeddingPath()
    {
        List<DENOPTIMTemplate> path = new ArrayList<DENOPTIMTemplate>();
        if (templateJacket==null)
            return path;
        if (templateJacket.getGraphOwner()!=null)
            path.addAll(templateJacket.getGraphOwner().getEmbeddingPath());
        path.add(templateJacket);
        return path;
    }

//------------------------------------------------------------------------------    
    
    /**
     * Copies the current vertexID of each vertex into a property of the vertex 
     * itself. Use this to keep a memory of the vertex IDs in any given moment.
     */
    public void storeCurrentVertexIDs()
    {
        for (DENOPTIMVertex v : gVertices)
        {
            v.setProperty(DENOPTIMConstants.STOREDVID, v.getVertexId());
        }
    }

//------------------------------------------------------------------------------        
    
    /**
     * Finds the AP that is on the first given parameter and that is used to 
     * make a connection to the second vertex, no matter the direction of the 
     * edge. If the two vertexes are connected by a chord, i.e., there is a pair 
     * of RCVs in between the two vertexes, we still work out the AP on 
     * the vertex on the left, even if in the graph it is actually used to bind 
     * the RCV that defines the chord and that eventually connects the 
     * vertex on the left with that on the right.
     * @param vid1 vertex ID of one vertex.
     * @param vid2 vertex ID of another vertex.
     * @return the {@link DENOPTIMAttachmentPoint} or null is the two vertex IDs
     * are not connected in this graph.
     */
    public DENOPTIMAttachmentPoint getAPOnLeftVertexID(int vid1, int vid2)
    {
        DENOPTIMVertex v1 = getVertexWithId(vid1);
        DENOPTIMVertex v2 = getVertexWithId(vid2);
        if ( v1== null || v2 == null)
            return null;
        
        if (getChildVertices(v1).contains(v2))
        {
            return v2.getEdgeToParent().getSrcAP();
        } else if (getChildVertices(v2).contains(v1))
        {
            return v1.getEdgeToParent().getTrgAP();
        }
        
        // At this point the two vertexes are not directly connected, but there 
        // could still be a chord between them. Here, we check for chords:
        /*
        for (DENOPTIMRing r : getRingsInvolvingVertex(v1))
        {
            if (r.contains(v2))
            {
                int lstId = r.getSize()-1;
                // Position 0 and lstId are where the RCVs sit
                if (r.getPositionOf(v1)==1 && r.getPositionOf(v2)==lstId)
                {
                    return r.getHeadVertex().getEdgeToParent().getSrcAP();
                }
                if (r.getPositionOf(v1)==lstId && r.getPositionOf(v2)==1)
                {
                    return r.getTailVertex().getEdgeToParent().getSrcAP();
                }
            }
        }
        */
        return null;
    }

//------------------------------------------------------------------------------    
    
    /**
     * Checks is the every edge in the graph can be defined in the opposite 
     * direction according to the {@link APClass} compatibility rules. Note that
     * the reversions operation on a branched subgraph generates a graph with 
     * multiple source vertexes, which is not allowed. Yet, the reversion is
     * formally possible.
     * @return <code>true</code> if the all edges can be reverted and retain
     * consistency with {@link APClass} compatibility rules.
     */
    public boolean isReversible()
    {
        for (DENOPTIMEdge e : gEdges)
        {
            if (!e.getTrgAPClass().isCPMapCompatibleWith(e.getSrcAPClass()))
            {
                return false;
            }
        }
        return true;
    }

//------------------------------------------------------------------------------    
    
}
