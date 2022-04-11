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
import java.util.logging.Logger;
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
import denoptim.graph.APClass.APClassDeserializer;
import denoptim.graph.Edge.BondType;
import denoptim.graph.Vertex.BBType;
import denoptim.graph.Vertex.DENOPTIMVertexDeserializer;
import denoptim.graph.Vertex.VertexType;
import denoptim.graph.rings.ClosableChain;
import denoptim.graph.rings.CyclicGraphHandler;
import denoptim.graph.rings.PathSubGraph;
import denoptim.graph.rings.RingClosureParameters;
import denoptim.graph.simplified.Node;
import denoptim.graph.simplified.NodeConnection;
import denoptim.graph.simplified.UndirectedEdge;
import denoptim.io.DenoptimIO;
import denoptim.json.DENOPTIMgson;
import denoptim.json.DENOPTIMgson.DENOPTIMExclusionStrategyNoAPMap;
import denoptim.logging.StaticLogger;
import denoptim.molecularmodeling.ThreeDimTreeBuilder;
import denoptim.programs.RunTimeParameters;
import denoptim.programs.RunTimeParameters.ParametersType;
import denoptim.utils.GraphEdit;
import denoptim.utils.MoleculeUtils;
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
public class DGraph implements Cloneable
{
    /**
     * The vertices belonging to this graph.
     */
    ArrayList<Vertex> gVertices;

    /**
     * The edges belonging to this graph.
     */
    ArrayList<Edge> gEdges;

    /**
     * The rings defined in this graph.
     */
    ArrayList<Ring> gRings;

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
     * Reference to the {@link Template} embedding this graph
     */
    Template templateJacket;
    
    /**
     * JGraph representation used to detect DENOPTIM-isomorphism
     */
    private DefaultUndirectedGraph<Vertex, UndirectedEdge> 
        jGraph = null;
    
    /**
     * JGraph representation used to detect DENOPTIM-isostructural graphs
     */
    private DefaultUndirectedGraph<Node, NodeConnection> 
        jGraphKernel = null;

    /**
     * Identifier for the format of string representations of a graph
     */
    public enum StringFormat {JSON, GRAPHENC}


//------------------------------------------------------------------------------

    public DGraph(ArrayList<Vertex> gVertices,
                            ArrayList<Edge> gEdges)
    {
        this.gVertices = gVertices;
        for (Vertex v : this.gVertices)
            v.setGraphOwner(this);
        this.gEdges = gEdges;
        gRings = new ArrayList<>();
        closableChains = new ArrayList<>();
        symVertices = new ArrayList<>();
        localMsg = "";
    }

//------------------------------------------------------------------------------

    public DGraph(ArrayList<Vertex> gVertices,
                            ArrayList<Edge> gEdges,
                            ArrayList<Ring> gRings)
    {
        this(gVertices, gEdges);
        this.gRings = gRings;
        closableChains = new ArrayList<>();
        symVertices = new ArrayList<>();
        localMsg = "";
    }

//------------------------------------------------------------------------------

    public DGraph(ArrayList<Vertex> gVertices,
                            ArrayList<Edge> gEdges,
                            ArrayList<Ring> gRings,
                            ArrayList<SymmetricSet> symVertices)
    {
        this(gVertices, gEdges, gRings);
        closableChains = new ArrayList<>();
        this.symVertices = symVertices;
        localMsg = "";
    }

//------------------------------------------------------------------------------

    public DGraph(ArrayList<Vertex> gVertices,
                            ArrayList<Edge> gEdges,
                            ArrayList<Ring> gRings,
                            ArrayList<ClosableChain> closableChains,
                            ArrayList<SymmetricSet> symVertices)
    {
        this(gVertices, gEdges, gRings, symVertices);
        this.closableChains = closableChains;
        localMsg = "";
    }

//------------------------------------------------------------------------------

    public DGraph()
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

    public boolean hasSymmetryInvolvingVertex(Vertex v)
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
    public ArrayList<Vertex> getSymVertexesForVertex(Vertex v)
    {
        ArrayList<Vertex> lst = new ArrayList<Vertex>();
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

    public SymmetricSet getSymSetForVertex(Vertex v)
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

    public void setVertexList(ArrayList<Vertex> vertices)
    {
        gVertices = vertices;
        jGraph = null;
        jGraphKernel = null;
    }

//------------------------------------------------------------------------------

    public void setEdgeList(ArrayList<Edge> edges)
    {
        gEdges = edges;
        jGraph = null;
        jGraphKernel = null;
    }

//------------------------------------------------------------------------------

    public void setRings(ArrayList<Ring> rings)
    {
        gRings = rings;
        jGraph = null;
        jGraphKernel = null;
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

    public ArrayList<Vertex> getVertexList()
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
    public Vertex getSourceVertex()
    {
        switch (gVertices.size())
        {
            case 0:
                return null;
            case 1:
                return getVertexAtPosition(0);
        }
        Vertex v0 = getVertexAtPosition(0);
        for (Edge e : this.getEdgeList())
        {
            if (e.getTrgAP().getOwner() == v0)
            {
                ArrayList<Vertex> parentTree = new ArrayList<>();
                getParentTree(v0,parentTree);
                return parentTree.get(parentTree.size()-1);
            }
        }
        return v0;
    }

//------------------------------------------------------------------------------

    public ArrayList<Edge> getEdgeList()
    {
        return gEdges;
    }

//------------------------------------------------------------------------------

    public ArrayList<Ring> getRings()
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
    public ArrayList<Edge> getEdgesWithSrc(Vertex v)
    {
    	ArrayList<Edge> edges = new ArrayList<Edge>();
    	for (Edge e : this.getEdgeList())
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
    public ArrayList<Ring> getRingsInvolvingVertex(Vertex v)
    {
        ArrayList<Ring> rings = new ArrayList<Ring>();
        for (Ring r : gRings)
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
    public ArrayList<Ring> getRingsInvolvingVertex(Vertex[] vs)
    {
        ArrayList<Ring> rings = new ArrayList<Ring>();
        for (Ring r : gRings)
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

    public ArrayList<Ring> getRingsInvolvingVertexID(int vid)
    {
        ArrayList<Ring> rings = new ArrayList<Ring>();
        for (Ring r : gRings)
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
        for (Ring  r : gRings)
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

    public boolean isVertexInRing(Vertex v)
    {
        boolean result = false;
        for (Ring  r : gRings)
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

    public ArrayList<Vertex> getRCVertices()
    {
        ArrayList<Vertex> rcvLst = new ArrayList<Vertex>();
        for (Vertex v : gVertices)
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

    public ArrayList<Vertex> getFreeRCVertices()
    {
        ArrayList<Vertex> rcvLst = getRCVertices();
        ArrayList<Vertex> free = new ArrayList<Vertex>();
        for (Vertex v : rcvLst)
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

    public ArrayList<Vertex> getUsedRCVertices()
    {
        ArrayList<Vertex> used = new ArrayList<Vertex>();
        used.addAll(getRCVertices());
        used.removeAll(getFreeRCVertices());
        return used;
    }

//------------------------------------------------------------------------------

    /**
     * Adds the edge to the list of edges belonging to this graph.
     * @param edge to be included in the list of edges
     */
    public void addEdge(Edge edge)
    {
        gEdges.add(edge);
        jGraph = null;
        jGraphKernel = null;
    }

//------------------------------------------------------------------------------

    public void addRing(Ring ring)
    {
        gRings.add(ring);
        jGraph = null;
        jGraphKernel = null;
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
    public void addRing(Vertex vI, Vertex vJ) 
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
        jGraphKernel = null;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Adds a chord between the given vertices, thus adding a ring in this
     * graph.
     * @param vI one of the ring-closing vertices.
     * @param vJ the other of the ring-closing vertices.
     * @param bndTyp the bond type the chord corresponds to.
     */
    public void addRing(Vertex vI, Vertex vJ, 
            BondType bndTyp)
    {
        PathSubGraph path = new PathSubGraph(vI,vJ,this);
        ArrayList<Vertex> arrLst = new ArrayList<Vertex>();
        arrLst.addAll(path.getVertecesPath());                    
        Ring ring = new Ring(arrLst);
        ring.setBondType(bndTyp);
        this.addRing(ring);
        jGraph = null;
        jGraphKernel = null;
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
    public void addVertex(Vertex vertex) throws DENOPTIMException
    {
        if (containsVertexID(vertex.getVertexId()))
            throw new DENOPTIMException("Vertex must have a VertexID that is "
                    + "unique within the graph. VertexID '" 
                    + vertex.getVertexId()+ "' already present in graph " 
                    + getGraphId());
        vertex.setGraphOwner(this);
        gVertices.add(vertex);
        jGraph = null;
        jGraphKernel = null;
    }

//------------------------------------------------------------------------------

    /**
     * Remove a vertex from this graph. This method removes also edges and rings
     * that involve the given vertex. Symmetric sets of vertices are corrected
     * accordingly: they are removed if there is only one remaining vertex in
     * the set, or purged from the vertex being removed.
     * @param vertex the vertex to remove.
     */
    public void removeVertex(Vertex vertex)
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
            ArrayList<Ring> rToRm = getRingsInvolvingVertex(vertex);
            for (Ring r : rToRm)
            {
                removeRing(r);
            }
        }

        // remove edges involving the removed vertex
        ArrayList<Edge> eToDel = new ArrayList<>();
        for (int i=0; i<gEdges.size(); i++)
        {
            Edge edge = gEdges.get(i);
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
        for (Edge e : eToDel)
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
        jGraphKernel = null;
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
    public boolean removeVertexAndWeld(Vertex vertex,
            FragmentSpace fragSpace) throws DENOPTIMException
    {
        if (!gVertices.contains(vertex))
        {
            return false;
        }
        
        ArrayList<Vertex> symSites = getSymVertexesForVertex(vertex);
        if (symSites.size() == 0)
        {
            symSites.add(vertex);
        } else {
            //TODO-V3 flip coin to decide if this should be a symmetric operation or not
        }
        for (Vertex oldLink : symSites)
        {
            GraphUtils.ensureVertexIDConsistency(this.getMaxVertexId());
            if (!removeSingleVertexAndWeld(oldLink, fragSpace))
            {
                return false;
            }
        }
        // Reject deletions that cause the collapse of a 3-atom ring into a
        // loop (i.e., 1-atom ring) or multiple connection (i.e., a 3-atom ring)
        for (Ring r : gRings)
        {
            // 3 = 1 actual vertex + 2 RCVs
            if (r.getSize()!=3)
                continue;
            
            AttachmentPoint apH = r.getHeadVertex().getEdgeToParent()
                    .getSrcAPThroughout();
            AttachmentPoint apT = r.getTailVertex().getEdgeToParent()
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
    public boolean removeSingleVertexAndWeld(Vertex vertex, 
            FragmentSpace fragSpace) throws DENOPTIMException
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
            for (AttachmentPoint ap : vertex.getAttachmentPoints())
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
                        templateJacket, fragSpace);
            }
        }
        
        // Get all APs that we'll try to weld into the parent
        ArrayList<AttachmentPoint> needyAPsOnChildren = 
                new ArrayList<AttachmentPoint>();
        // And all APs where we could weld onto
        ArrayList<AttachmentPoint> freeAPsOnParent = 
                new ArrayList<AttachmentPoint>();
        //        vertex.getAPsFromChilddren(); //No, because it enter templates
        Map<AttachmentPoint,AttachmentPoint> apOnOldToNeedyAP = 
                new HashMap<AttachmentPoint,AttachmentPoint>();
        for (AttachmentPoint apOnOld : vertex.getAttachmentPoints())
        {
            if (!apOnOld.isAvailableThroughout())
            {
                if (apOnOld.isSrcInUserThroughout())
                {
                    // Links that depart from vertex
                    AttachmentPoint needyAP = 
                            apOnOld.getLinkedAPThroughout();
                    // NB: here do not use getSrcThroughout because it would 
                    // enter trg templates rather than staying on their surface.
                    needyAPsOnChildren.add(needyAP);
                    apOnOldToNeedyAP.put(apOnOld, needyAP);
                } else {
                    AttachmentPoint apOnParent = 
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
        List<APMapping> mappings = fragSpace.mapAPClassCompatibilities(
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
            AttachmentPoint needyAP = needyAPsOnChildren.get(i);
            preferences.add(0);
            
            Vertex ownerOfNeedy = needyAP.getOwner();
            for (Ring r : ownerOfNeedy.getGraphOwner()
                    .getRingsInvolvingVertex(ownerOfNeedy))
            {
                // NB: here we stay at the level of the graph owning ownerOfNeedy
                Vertex lastBeforeOwnerOfNeedy = 
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
            for (AttachmentPoint ap : apm.values())
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
        for (Entry<AttachmentPoint, AttachmentPoint> e : 
            bestScoringMapping.entrySet())
        {
            bestScoringMappingReverse.put(e.getValue(), e.getKey());
        }
        
        // Update rings involving vertex directly (i.e., in this graph)
        ArrayList<Ring> rToEdit = getRingsInvolvingVertex(vertex);
        ArrayList<Ring> ringsToRemove = new ArrayList<Ring>();
        for (Ring r : rToEdit)
        {
            r.removeVertex(vertex);
            if (r.getSize() < 3)
                ringsToRemove.add(r);
        }
        for (Ring r : ringsToRemove)
        {
            removeRing(r);
        }
        
        // Remove edges to/from old vertex, while keeping track of edits to do
        // in a hypothetical jacket template (if such template exists)
        LinkedHashMap<AttachmentPoint,AttachmentPoint> newInReplaceOldInInTmpl = 
                new LinkedHashMap<AttachmentPoint,AttachmentPoint>();
        List<AttachmentPoint> oldAPToRemoveFromTmpl = 
                new ArrayList<AttachmentPoint>();
        for (AttachmentPoint oldAP : vertex.getAttachmentPoints())
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
                        AttachmentPoint lAP = 
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
        List<AttachmentPoint> reconnettedApsOnChilds = 
                new ArrayList<AttachmentPoint>();
        for (Entry<AttachmentPoint,AttachmentPoint> e : 
            bestScoringMapping.entrySet())
        {
            AttachmentPoint apOnParent = e.getKey();
            AttachmentPoint apOnChild = e.getValue();
            if (containsVertex(apOnChild.getOwner()) 
                    && containsVertex(apOnParent.getOwner()))
            {
                Edge edge = new Edge(apOnParent,apOnChild,
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
            for (AttachmentPoint apOnOld : oldAPToRemoveFromTmpl)
            {
                templateJacket.removeProjectionOfInnerAP(apOnOld);
            }
        }
        
        // Remove branches of child-APs that were not mapped/done
        for (AttachmentPoint apOnChild : needyAPsOnChildren)
        {
            if (!reconnettedApsOnChilds.contains(apOnChild))
            {
                removeOrphanBranchStartingAt(apOnChild.getOwner());
            }
        }

        jGraph = null;
        jGraphKernel = null;
        
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
        for (Vertex v : gVertices)
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
        for (Vertex v : gVertices)
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
        Map<String,List<Vertex>> collectedLabels = new HashMap<>();
        for (Vertex v : gVertices)
        {
            if (v.hasProperty(DENOPTIMConstants.VRTSYMMSETID))
            {
                String label = v.getProperty(
                        DENOPTIMConstants.VRTSYMMSETID).toString();
                if (collectedLabels.containsKey(label))
                {
                    collectedLabels.get(label).add(v);
                } else {
                    List<Vertex> lst = new ArrayList<Vertex>();
                    lst.add(v);
                    collectedLabels.put(label, lst);
                }
                v.removeProperty(DENOPTIMConstants.VRTSYMMSETID);
            }
        }
        
        for (String label : collectedLabels.keySet())
        {
            List<Vertex> symmVertexes = collectedLabels.get(label);
            
            if (symmVertexes.size()>1)
            {
                SymmetricSet ss = null;
                for (Vertex v : symmVertexes)
                {
                    if (hasSymmetryInvolvingVertex(v))
                    {
                        ss = getSymSetForVertex(v);
                        break;
                    }
                }
                
                if (ss != null)
                {
                    for (Vertex v : symmVertexes)
                        ss.add(v.getVertexId());
                    
                } else {
                    ss = new SymmetricSet();
                    for (Vertex v : symmVertexes)
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
    public boolean replaceSubGraph(List<Vertex> subGrpVrtxs, 
            DGraph incomingGraph, 
            LinkedHashMap<AttachmentPoint,AttachmentPoint> apMap,
            FragmentSpace fragSpace) throws DENOPTIMException
    {
        for (Vertex vToRemove : subGrpVrtxs)
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
        
        // Verify that also the surrounding of vertexes in the lists is 
        // consistent between subGrpVrtxs and vertexesToRemove. Even if the
        // two are consistent, there can still be differences in the childs.
        List<List<Vertex>> compatibleSymSubGrps = new ArrayList<List<Vertex>>();
        for (List<Vertex> symmetricSubGrpVrtx : getSymmetricSubGraphs(subGrpVrtxs))
        {
            boolean skip = false;
            for (int iv=0; iv<subGrpVrtxs.size(); iv++)
            {
                if (skip)
                    break;
                Vertex oriVs = subGrpVrtxs.get(iv);
                Vertex symVs = symmetricSubGrpVrtx.get(iv);
                List<Vertex> oriVsChildren = oriVs.getChilddren();
                List<Vertex> symVsChildren = symVs.getChilddren();
                
                // NB: oriVsChildren does not include CAPs from the 
                // initial subgraph, while symVsChildren does include the 
                // corresponding CAPs.
                oriVsChildren.removeIf(v -> v.getBuildingBlockType()==BBType.CAP);
                symVsChildren.removeIf(v -> v.getBuildingBlockType()==BBType.CAP);
                if (oriVsChildren.size()!=symVsChildren.size())
                {
                    // we continue in the outer loop
                    skip = true;
                    continue;
                }
                for (int ic=0; ic<oriVsChildren.size(); ic++)
                {
                    if (skip)
                        break;
                    // We have already checked those in the subgraph
                    if (subGrpVrtxs.contains(oriVsChildren.get(ic)))
                        continue;
                    // We do allow the two child to be different vertexes, but
                    // we avoid having one CAP and one not. this because it will
                    // lead to having free APs on the parent of the first and
                    // busy APs on the parent of the second. This prevents 
                    // finding a mapping of the free APs.
                    if (oriVsChildren.get(ic).getBuildingBlockType()
                            != symVsChildren.get(ic).getBuildingBlockType())
                    {
                        skip = true;
                        continue;
                    }
                }
            }
            if (!skip)
                compatibleSymSubGrps.add(symmetricSubGrpVrtx);
        }
        if (compatibleSymSubGrps.size()==0)
            throw new DENOPTIMException("Failed to detect autosymmetry.");
        
        for (List<Vertex> vertexesToRemove : compatibleSymSubGrps)
        {
            // Prepare incoming graph
            DGraph graphToAdd = incomingGraph.clone();
            graphToAdd.renumberGraphVertices();
            List<Vertex> vertexAddedToThis = new ArrayList<Vertex>(
                    graphToAdd.gVertices);
            
            // Prepare subgraph (it already does not contain caps)
            removeCappingGroupsFromChilds(vertexesToRemove);
            
            // Prepare AP mapping projecting the one for subGrpVrtxs
            LinkedHashMap<AttachmentPoint,AttachmentPoint> localApMap = 
                    new LinkedHashMap<AttachmentPoint,AttachmentPoint>();
            for (Map.Entry<AttachmentPoint,AttachmentPoint> e : apMap.entrySet())
            {
                // WARNING! Assumption that subGrpVrtxs and vertexesToRemove
                // are sorted accordingly to symmetry, which should be the case.
                int vrtPosOnOld = subGrpVrtxs.indexOf(e.getKey().getOwner());
                int apPosOnOld = e.getKey().getIndexInOwner();
                AttachmentPoint apOnOld = vertexesToRemove.get(
                        vrtPosOnOld).getAP(apPosOnOld); 
                
                int vrtPosOnNew = incomingGraph.indexOf(e.getValue().getOwner());
                int apPosOnNew = e.getValue().getIndexInOwner();
                AttachmentPoint apOnNew = graphToAdd.getVertexAtPosition(
                        vrtPosOnNew).getAP(apPosOnNew); 
                localApMap.put(apOnOld,apOnNew);
            }
            
            if (!replaceSingleSubGraph(vertexesToRemove, graphToAdd, localApMap))
            {
                return false;
            }
            addCappingGroups(vertexAddedToThis, fragSpace);
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
    public List<List<Vertex>> getSymmetricSubGraphs(
            List<Vertex> subGrpVrtxs) throws DENOPTIMException
    {
        if (subGrpVrtxs.stream().anyMatch(v -> v.getBuildingBlockType()==BBType.CAP))
            throw new DENOPTIMException("Capping groups must not be part of "
                    + "symmetric subgraphs");

        List<List<Vertex>> symSites = new ArrayList<List<Vertex>>();
        
        if (subGrpVrtxs.size()==1)
        {
            for (Vertex sv : getSymVertexesForVertex(subGrpVrtxs.get(0)))
            {
                ArrayList<Vertex> lst = new ArrayList<Vertex>();
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
        List<Vertex> thoseWithoutParent = new ArrayList<Vertex>();
        for (Vertex v : subGrpVrtxs)
        {
            if (!subGrpVrtxs.contains(v.getParent()))
                thoseWithoutParent.add(v);
        }
        if (thoseWithoutParent.size()!=1)
        {
            throw new DENOPTIMException("SubGraph has more than one grand "
                    + "parent.");
        }
        Vertex sourceOfSubGraph = thoseWithoutParent.get(0);
        int numSymmetricSubGraphs = getSymVertexesForVertex(sourceOfSubGraph).size();
        if (numSymmetricSubGraphs==0)
        {
            symSites.add(subGrpVrtxs);
            return symSites;
        }
        
        // Identify the ends of the subgraph's spanning tree
        List<Vertex> thoseWithoutChildren = new ArrayList<Vertex>();
        for (Vertex v : subGrpVrtxs)
        {
            if (Collections.disjoint(v.getChilddren(),subGrpVrtxs))
                thoseWithoutChildren.add(v);
        }
        
        // We want to verify that all the ends of the subgraph's spanning tree
        // have the same number of symmetric partners. This, while collecting
        // all ends that are outside the subgraph and are symmetric to any of
        // the ends belonging to the subgraph. The first, in fact, are the ends
        // of the symmetric subgraphs.
        Set<Vertex> upperLimits = new HashSet<Vertex>();
        Set<Vertex> doneBySymmetry = new HashSet<Vertex>();
        for (Vertex upperLimit : thoseWithoutChildren)
        {
            // We need to understand how many symmetric vertexes are already
            // within the subgraph
            int numInSubGraphReplicas = 1;
            
            if (doneBySymmetry.contains(upperLimit))
                continue;
            
            // These are symmetric vertexes that do belong to the subgraph
            Set<Vertex> symmSitesOnBranch = new HashSet<Vertex>(
                    getSymVertexesForVertex(upperLimit));
            symmSitesOnBranch.retainAll(subGrpVrtxs);
            if (symmSitesOnBranch.size()>0)
            {
                numInSubGraphReplicas = symmSitesOnBranch.size();
                doneBySymmetry.addAll(symmSitesOnBranch);
            }
            
            List<Vertex> lst = getSymVertexesForVertex(upperLimit);
            if (lst.size() != numInSubGraphReplicas*numSymmetricSubGraphs)
            {
                // The subgraph is not symmetrically reproduced.
                symSites.add(subGrpVrtxs);
                return symSites;
            }
            upperLimits.addAll(lst);
        }
        
        for (Vertex symSources : getSymVertexesForVertex(sourceOfSubGraph))
        {
            List<Vertex> symSubGraph = new ArrayList<Vertex>();
            // The source of the symmetric subgraph is always the first!
            symSubGraph.add(symSources);
            getChildTreeLimited(symSources, symSubGraph, upperLimits);
            //NB: Capping groups are not supposed to be in the list.
            symSubGraph.removeIf(v -> v.getBuildingBlockType()==BBType.CAP);
            if (symSubGraph.size()!=subGrpVrtxs.size())
            {
                symSites = new ArrayList<List<Vertex>>();
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
     * {@link DGraph#reassignSymmetricLabels()} on <code>newSubGraph</code>
     * prior to calling this method, and, after running this method, call
     * {@link DGraph#convertSymmetricLabelsToSymmetricSets()} on this
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
    public boolean replaceSingleSubGraph(List<Vertex> subGrpVrtxs, 
            DGraph newSubGraph, 
            LinkedHashMap<AttachmentPoint,AttachmentPoint> apMap) 
                    throws DENOPTIMException
    {
        if (!gVertices.containsAll(subGrpVrtxs) 
                || gVertices.contains(newSubGraph.getVertexAtPosition(0)))
        {
            return false;
        }
        
        // Identify vertex that will be added
        ArrayList<Vertex> newVertexes = new ArrayList<Vertex>();
        newVertexes.addAll(newSubGraph.getVertexList());
        
        // Collect APs from the vertexes that will be removed, and that might
        // be reflected onto the jacket template or used to make links to the 
        // rest of the graph.
        List<AttachmentPoint> interfaceApsOnOldBranch = 
                new ArrayList<AttachmentPoint>();
        for (Vertex vToDel : subGrpVrtxs)
        {
            for (AttachmentPoint ap : vToDel.getAttachmentPoints())
            {
                if (ap.isAvailable())
                {
                    // being available, this AP might be reflected onto jacket template
                    interfaceApsOnOldBranch.add(ap);
                } else {
                    Vertex user = ap.getLinkedAP().getOwner();
                    if (!subGrpVrtxs.contains(user))
                    {
                        interfaceApsOnOldBranch.add(ap);
                    }
                }
            }
        }
        List<AttachmentPoint> interfaceApsOnNewBranch = 
                new ArrayList<AttachmentPoint>();
        for (Vertex v : newSubGraph.getVertexList())
        {
            for (AttachmentPoint ap : v.getAttachmentPoints())
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
        LinkedHashMap<AttachmentPoint,AttachmentPoint> 
            linksToRecreate = new LinkedHashMap<>();
        LinkedHashMap<AttachmentPoint,BondType> 
            linkTypesToRecreate = new LinkedHashMap<>();
        LinkedHashMap<AttachmentPoint,AttachmentPoint> 
            inToOutAPForTemplate = new LinkedHashMap<>();
        List<AttachmentPoint> oldAPToRemoveFromTmpl = new ArrayList<>();
        AttachmentPoint trgAPOnNewLink = null;
        for (AttachmentPoint oldAP : interfaceApsOnOldBranch)
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
                        + "AP has no mapping. Missing mapping for AP "
                        + oldAP.getIndexInOwner() + " in " 
                        + oldAP.getOwner().getVertexId());
            }
            AttachmentPoint newAP = apMap.get(oldAP);
            linksToRecreate.put(newAP, oldAP.getLinkedAP());
            linkTypesToRecreate.put(newAP, oldAP.getEdgeUser().getBondType());
            
            // This is were we identify the edge/ap to the parent of the oldLink
            if (!oldAP.isSrcInUser())
            {
                trgAPOnNewLink = newAP;
            }
        }
        
        // Identify rings that are affected by the change of vertexes
        Map<Ring,List<Vertex>> ringsOverSubGraph = 
                new HashMap<Ring,List<Vertex>>();
        for (int iA=0; iA<interfaceApsOnOldBranch.size(); iA++)
        {
            AttachmentPoint apA = interfaceApsOnOldBranch.get(iA);
        
            if (apA.isAvailable())
                continue;
            
            for (int iB=(iA+1); iB<interfaceApsOnOldBranch.size(); iB++)
            {
                AttachmentPoint apB = interfaceApsOnOldBranch.get(iB);
            
                if (apB.isAvailable())
                    continue;
                
                Vertex vLinkedOnA = apA.getLinkedAP().getOwner();
                Vertex vLinkedOnB = apB.getLinkedAP().getOwner();
                for (Ring r : getRingsInvolvingVertex(
                        new Vertex[] {
                                apA.getOwner(), vLinkedOnA,
                                apB.getOwner(), vLinkedOnB}))
                {
                    List<Vertex> vPair = new ArrayList<Vertex>();
                    vPair.add(r.getCloserToHead(vLinkedOnA, vLinkedOnB));
                    vPair.add(r.getCloserToTail(vLinkedOnA, vLinkedOnB));
                    ringsOverSubGraph.put(r, vPair);
                }
            }
        }
        
        // remove the vertex-to-delete from the rings where they participate
        for (Ring r : ringsOverSubGraph.keySet())
        {
            List<Vertex> vPair = ringsOverSubGraph.get(r);
            PathSubGraph path = new PathSubGraph(vPair.get(0),vPair.get(1),this);
            List<Vertex> vertexesInPath = path.getVertecesPath();
            for (int i=1; i<(vertexesInPath.size()-1); i++)
            {
                r.removeVertex(vertexesInPath.get(i));
            }
        }
        
        // remove edges with old vertex
        for (AttachmentPoint oldAP : interfaceApsOnOldBranch)
        {
            if (!oldAP.isAvailable())
                removeEdge(oldAP.getEdgeUser());
        }

        // remove the vertex from the graph
        for (Vertex vToDel : subGrpVrtxs)
        {
            // WARNING! This removes rings involving these vertexes. 
            removeVertex(vToDel);
        }
        
        // finally introduce the new vertexes from incoming graph into this graph
        for (Vertex incomingVrtx : newSubGraph.getVertexList())
        {
            addVertex(incomingVrtx);
        }
        
        // import edges from incoming graph 
        for (Edge incomingEdge : newSubGraph.getEdgeList())
        {
            addEdge(incomingEdge);
        }
        
        // import rings from incoming graph
        for (Ring incomingRing : newSubGraph.getRings())
        {
            addRing(incomingRing);
        }
        
        // import symmetric sets from incoming graph? No, this method doesn't do
        // it because we want to use it in situations where we have to perform 
        // multiple replaceSubGraph operations and, afterwards, use the 
        // symmetric labels to create symmetric sets that might span across
        // more than one of the subgraphs that were added.
        
        // We keep track of the APs on the new link that have been dealt with
        List<AttachmentPoint> doneApsOnNew = 
                new ArrayList<AttachmentPoint>();
        
        // Connect the incoming subgraph to the rest of the graph
        if (trgAPOnNewLink != null)
        {
            // the incoming graph has a parent vertex, and the edge should be  
            // directed accordingly
            Edge edge = new Edge(
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
        for (AttachmentPoint apOnNew : linksToRecreate.keySet())
        {
            if (apOnNew == trgAPOnNewLink)
            {
                continue; //done just before this loop
            }
            AttachmentPoint trgOnChild = linksToRecreate.get(apOnNew);
            Edge edge = new Edge(apOnNew,trgOnChild, 
                    linkTypesToRecreate.get(apOnNew));
            addEdge(edge);
            doneApsOnNew.add(apOnNew);
        }
        
        // redefine rings that spanned over the removed subgraph
        for (Ring r : ringsOverSubGraph.keySet())
        {
            List<Vertex> vPair = ringsOverSubGraph.get(r);
            PathSubGraph path = new PathSubGraph(vPair.get(0),vPair.get(1),this);
            int initialInsertPoint = r.getPositionOf(vPair.get(0));
            List<Vertex> vertexesInPath = path.getVertecesPath();
            for (int i=1; i<(vertexesInPath.size()-1); i++)
            {
                r.insertVertex(initialInsertPoint+i, vertexesInPath.get(i));
            }
        }
        
        // update the mapping of this vertexes' APs in the jacket template
        if (templateJacket != null)
        {   
            for (AttachmentPoint apOnNew : inToOutAPForTemplate.keySet())
            {
                templateJacket.updateInnerApID(
                        inToOutAPForTemplate.get(apOnNew),apOnNew);
                doneApsOnNew.add(apOnNew);
            }
            
            // Project all remaining APs of new branch on the surface of template
            for (AttachmentPoint apOnNew : interfaceApsOnNewBranch)
            {
                if (!doneApsOnNew.contains(apOnNew))
                {
                    templateJacket.addInnerToOuterAPMapping(apOnNew);
                }
            }
            
            // Remove all APs that existed only in the old branch
            for (AttachmentPoint apOnOld : oldAPToRemoveFromTmpl)
            {
                templateJacket.removeProjectionOfInnerAP(apOnOld);
            }
        }
        
        jGraph = null;
        jGraphKernel = null;
        
        for (Vertex vOld : subGrpVrtxs)
            if (this.containsVertex(vOld))
                return false;
        for (Vertex vNew : newVertexes)
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
    public boolean replaceVertex(Vertex vertex, int bbId, BBType bbt,
            LinkedHashMap<Integer, Integer> apIdMap, FragmentSpace fragSpace)
                    throws DENOPTIMException
    {
        if (!gVertices.contains(vertex))
        {
            return false;
        }
        
        ArrayList<Vertex> symSites = getSymVertexesForVertex(vertex);
        if (symSites.size() == 0)
        {
            symSites.add(vertex);
        }
        
        GraphUtils.ensureVertexIDConsistency(this.getMaxVertexId());
        Vertex newLink = Vertex.newVertexFromLibrary(
                GraphUtils.getUniqueVertexIndex(), bbId, bbt, fragSpace);
        DGraph incomingGraph = new DGraph();
        incomingGraph.addVertex(newLink);
        incomingGraph.reassignSymmetricLabels();
        
        for (Vertex oldLink : symSites)
        {
            DGraph graphAdded = incomingGraph.clone();
            graphAdded.getVertexAtPosition(0).setMutationTypes(
                    oldLink.getUnfilteredMutationTypes());
            graphAdded.renumberGraphVertices();
            
            ArrayList<Vertex> oldVertex = new ArrayList<Vertex>();
            oldVertex.add(oldLink);
            LinkedHashMap<AttachmentPoint,AttachmentPoint> apMap =
                    new LinkedHashMap<AttachmentPoint,AttachmentPoint>();
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
    
    public boolean insertVertex(Edge edge, int bbId, BBType bbt,
            LinkedHashMap<AttachmentPoint,Integer> apMap, 
            FragmentSpace fragSpace) 
                    throws DENOPTIMException
    {
        if (!gEdges.contains(edge))
        {
            return false;
        }
        
        ArrayList<Edge> symSites = new ArrayList<Edge> ();
        ArrayList<LinkedHashMap<AttachmentPoint,Integer>> symApMaps = 
                new ArrayList<LinkedHashMap<AttachmentPoint,Integer>>();
        ArrayList<Vertex> symTrgVertexes = getSymVertexesForVertex(
                edge.getTrgAP().getOwner());
        if (symTrgVertexes.size() == 0)
        {
            symSites.add(edge);
            symApMaps.add(apMap);
        } else {
            for (Vertex trgVrtx : symTrgVertexes)
            {
                Edge symEdge = trgVrtx.getEdgeToParent();
                symSites.add(symEdge);
                
                LinkedHashMap<AttachmentPoint,Integer> locApMap = new
                        LinkedHashMap<AttachmentPoint,Integer>();
                locApMap.put(symEdge.getSrcAP(), apMap.get(edge.getSrcAP()));
                locApMap.put(symEdge.getTrgAP(), apMap.get(edge.getTrgAP()));
                symApMaps.add(locApMap);
            }
        }
        
        SymmetricSet newSS = new SymmetricSet();
        for (int i=0; i<symSites.size(); i++)
        {
            Edge symEdge = symSites.get(i);
            LinkedHashMap<AttachmentPoint,Integer> locApMap = symApMaps.get(i);
            
            GraphUtils.ensureVertexIDConsistency(this.getMaxVertexId());
            Vertex newLink = Vertex.newVertexFromLibrary(
                    GraphUtils.getUniqueVertexIndex(), bbId, bbt, fragSpace);
            newSS.add(newLink.getVertexId());
            LinkedHashMap<AttachmentPoint,AttachmentPoint> apToApMap =
                    new LinkedHashMap<AttachmentPoint,AttachmentPoint>();
            for (AttachmentPoint apOnGraph : locApMap.keySet())
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
    public boolean insertSingleVertex(Edge edge, Vertex newLink,
            LinkedHashMap<AttachmentPoint,AttachmentPoint> apMap) 
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
        AttachmentPoint orisEdgeSrc = edge.getSrcAP();
        AttachmentPoint orisEdgeTrg = edge.getTrgAP();
        Vertex srcVrtx = orisEdgeSrc.getOwner();
        Vertex trgVrtx = orisEdgeTrg.getOwner();
        
        removeEdge(edge);
        
        // Introduce the new vertex in the graph
        addVertex(newLink);
        
        // Connect the new vertex to the graph
        Edge eSrcToLink = new Edge(orisEdgeSrc,
                apMap.get(orisEdgeSrc), edge.getBondType());
        addEdge(eSrcToLink);
        Edge eLinkToTrg = new Edge(apMap.get(orisEdgeTrg),
                orisEdgeTrg, edge.getBondType());
        addEdge(eLinkToTrg);
        
        // update any affected ring
        if (isVertexInRing(srcVrtx) && isVertexInRing(trgVrtx))
        {
            ArrayList<Ring> rToEdit = new ArrayList<Ring>();
            rToEdit.addAll(getRingsInvolvingVertex(srcVrtx));
            rToEdit.retainAll(getRingsInvolvingVertex(trgVrtx));
            for (Ring r : rToEdit)
            {
                r.insertVertex(newLink,srcVrtx,trgVrtx);
            }
        }
    
        // NB: if this graph is embedded in a template, new free/available 
        // APs introduced with the new link need to be mapped on the surface of
        // the template
        if (templateJacket != null)
        {
            for (AttachmentPoint ap : newLink.getAttachmentPoints())
            {
                if (ap.isAvailable())
                {
                    templateJacket.addInnerToOuterAPMapping(ap);
                }
            }
        } 
        
        jGraph = null;
        jGraphKernel = null;
        
        return !gEdges.contains(edge) && this.containsVertex(newLink);
    }

//------------------------------------------------------------------------------

    /**
     * Returns the vertex that is in the given position of the list of vertexes 
     * belonging to this graph.
     * @param pos the position in the list.
     * @return the vertex in the given position.
     */
    public Vertex getVertexAtPosition(int pos)
    {
        return ((pos >= gVertices.size()) || pos < 0) ? null :
                gVertices.get(pos);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Check if the specified vertex is contained in this graph as a node or
     * in any inner graphs that may be embedded in {@link Template}-kind
     * vertex belonging to this graph. 
     * @param v the vertex.
     * @return <code>true</code> if the vertex belongs to this graph or is 
     * anyhow embedded in it.
     */
    public boolean containsOrEmbedsVertex(Vertex v)
    {
        if (gVertices.contains(v))
            return true;
           
        for (Vertex vrt : gVertices)
        {
            if (vrt instanceof Template)
            {
                Template t = (Template) vrt;
                if (t.getInnerGraph().containsOrEmbedsVertex(v))
                    return true;
            }
        }
        return false;
    }
    

//------------------------------------------------------------------------------

    /**
     * Check if this graph contains the specified vertex. Does not consider 
     * inner graphs that may be embedded in {@link Template}-kind
     * vertex belonging to this graph. 
     * @param v the vertex.
     * @return <code>true</code> if the vertex belongs to this graph.
     */
    public boolean containsVertex(Vertex v)
    {
        return gVertices.contains(v);
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the index of a vertex in the list of vertices of this graph.
     * @param v the vertex to search
     * @return the index of the vertex in the list of vertices.
     */
    public int indexOf(Vertex v)
    {
        return gVertices.indexOf(v);
    }

//------------------------------------------------------------------------------

    public Vertex getVertexWithId(int vid)
    {
        Vertex v = null;
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
            Vertex v = gVertices.get(i);
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
    public void removeEdge(Edge edge)
    {
        if (gEdges.contains(edge))
        {
            AttachmentPoint srcAP = edge.getSrcAP();
            AttachmentPoint trgAP = edge.getTrgAP();
            srcAP.setUser(null);
            trgAP.setUser(null);

            gEdges.remove(edge);
        }
        jGraph = null;
        jGraphKernel = null;
    }

//------------------------------------------------------------------------------

    public void removeRing(Ring ring)
    {
        if (gRings.contains(ring))
        {
            gRings.remove(ring);
        }
        jGraph = null;
        jGraphKernel = null;
    }

//------------------------------------------------------------------------------

    public Edge getEdgeAtPosition(int pos)
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
    public int getBondingAPIndex(Vertex srcVert, int dapidx,
                                    Vertex dstVert)
    {
        int n = getEdgeCount();
        for (int i = 0; i < n; i++)
        {
            Edge edge = getEdgeList().get(i);

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

    public ArrayList<Vertex> getChildVertices(Vertex vertex)
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
    public void getChildrenTree(Vertex vertex,
            List<Vertex> children) 
    {
        List<Vertex> lst = getChildVertices(vertex);
        if (lst.isEmpty()) 
        {
            return;
        }
        for (Vertex child : lst) 
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
    public void getChildrenTree(Vertex vertex,
            List<Vertex> children, int numLayers, boolean stopBeforeRCVs) 
    {
        if (numLayers==0)
        {
            return;
        }
        List<Vertex> lst = getChildVertices(vertex);
        if (lst.isEmpty()) 
        {
            return;
        }
        for (Vertex child : lst) 
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
    public void getChildTreeLimited(Vertex vertex,
            List<Vertex> children, boolean stopBeforeRCVs) 
    {
        List<Vertex> lst = getChildVertices(vertex);
        if (lst.isEmpty()) 
        {
            return;
        }
        for (Vertex child : lst) 
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
    public void getChildTreeLimited(Vertex vertex,
            List<Vertex> children, Set<Vertex> limits)
    {
        List<Vertex> lst = getChildVertices(vertex);
        if (lst.isEmpty()) 
        {
            return;
        }
        for (Vertex child : lst) 
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
    public void getChildTreeLimited(Vertex vertex,
            List<Vertex> children, List<Vertex> limitsInClone, 
            boolean stopBeforeRCVs) 
    {
        List<Vertex> lst = getChildVertices(vertex);
        if (lst.isEmpty()) 
        {
            return;
        }
        for (Vertex child : lst) 
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
    public Vertex getDeepestAmongThese(List<Vertex> list)
    {
        Vertex deepest = null;
        int shortest = Integer.MAX_VALUE;
        for (Vertex vertex : list)
        {
            List<Vertex> parentTree = new ArrayList<Vertex>();
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
    public void getParentTree(Vertex vertex,
            List<Vertex> parentTree) 
    {
        Vertex parent = getParent(vertex);
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

    public Vertex getParent(Vertex v)
    {
        Edge edge = v.getEdgeToParent();
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
        Vertex v = getVertexWithId(vid);
        for (AttachmentPoint ap : v.getAttachmentPoints())
        {
            Edge e = ap.getEdgeUser();
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
    public DGraph clone()
    {   
        // When cloning, the VertexID remains the same so we'll have two
        // deep-copies of the same vertex having the same VertexID
        ArrayList<Vertex> cListVrtx = new ArrayList<>();
        Map<Integer,Vertex> vidsInClone =
                new HashMap<Integer,Vertex>();
        for (Vertex vOrig : gVertices)
        {
            Vertex vClone = vOrig.clone();
            cListVrtx.add(vClone);
            vidsInClone.put(vClone.getVertexId(),vClone);
        }

        ArrayList<Edge> cListEdges = new ArrayList<>();
        for (Edge e : gEdges)
        {
            int srcVrtxId = e.getSrcVertex();
            int srcApId = this.getVertexWithId(srcVrtxId).getIndexOfAP(
                    e.getSrcAP());

            int trgVrtxId = e.getTrgVertex();
            int trgApId = this.getVertexWithId(trgVrtxId).getIndexOfAP(
                    e.getTrgAP());

            AttachmentPoint srcAPClone = vidsInClone.get(
                    srcVrtxId).getAP(srcApId);
            AttachmentPoint trgAPClone = vidsInClone.get(
                    trgVrtxId).getAP(trgApId);

            cListEdges.add(new Edge(srcAPClone, trgAPClone,
                    e.getBondType()));
        }

        DGraph clone = new DGraph(cListVrtx, cListEdges);

        // Copy the list but using the references to the cloned vertices
        ArrayList<Ring> cListRings = new ArrayList<>();
        for (Ring ring : gRings)
        {
            Ring cRing = new Ring();
            for (int iv=0; iv<ring.getSize(); iv++)
            {
                Vertex origVrtx = ring.getVertexAtPosition(iv);
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

    public Edge getEdgeWithParent(int vid)
    {
        Vertex v = getVertexWithId(vid);
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
            Edge edge = getEdgeAtPosition(j);

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

    public ArrayList<Edge> getEdgesWithChild(int vid)
    {
        ArrayList<Edge> lstEdges = new ArrayList<>();
        for (int j=0; j<getEdgeCount(); j++)
        {
            Edge edge = getEdgeAtPosition(j);

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
        for (Vertex v : gVertices) {
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
        for (Vertex v : gVertices) 
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
        jGraphKernel = null;
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
     * {@link GraphConversionTool#getJGraphFromGraph(DGraph)}.
     * Finally,
     * <ul>
     * <li>vertexes are compared excluding their vertex ID, i.e., 
     * {@link Vertex#sameAs()}</li>
     * <li>edges are considered undirected and compared considering the 
     * {@link BondType} and the 
     * identity of the attachment points connected thereby. This latter point
     * has an important implication: two apparently equal graphs (same vertexes
     * that are connected to each other forming the same vertex-chains) can be 
     * non-isomorphic when the APs used to connect two vertexes are not the
     * same. Chemically, this means the stereochemistry around one or both
     * vertexes, is different in the two graphs. Therefore two otherwise 
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
     * graph with symmetric branches and its isomorphic analog that is fully
     * asymmetric.</p>
     * 
     * @param other the graph to be compared with this.
     * @return <code>true</code> is this graph is isomorphic to the other.
     */
    public boolean isIsomorphicTo(DGraph other) {
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
        
        Comparator<Vertex> vComp = new Comparator<Vertex>() {
            
            Map<Vertex,Set<Vertex>> symmetryShortCuts = 
                    new HashMap<Vertex,Set<Vertex>>();
            
            @Override
            public int compare(Vertex v1, Vertex v2) {
                
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
                    Set<Vertex> symToV2 = new HashSet<Vertex>();
                    SymmetricSet ssV2 = v2.getGraphOwner().getSymSetForVertex(v2);
                    for (Integer sVrtId : ssV2.getList())
                    {
                        symToV2.add(v2.getGraphOwner().getVertexWithId(sVrtId));
                    }
                    
                    Set<Vertex> symToV1 = new HashSet<Vertex>();
                    SymmetricSet ssV1 = v1.getGraphOwner().getSymSetForVertex(v1);
                    for (Integer sVrtId : ssV1.getList())
                    {
                        symToV1.add(v1.getGraphOwner().getVertexWithId(sVrtId));
                    }
                    
                    for (Vertex v1s : symToV1)
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
        
        Comparator<UndirectedEdge> eComp =
                UndirectedEdge::compare;
        
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
        
        VF2GraphIsomorphismInspector<Vertex, UndirectedEdge> vf2 =
                new VF2GraphIsomorphismInspector<>(this.jGraph, other.jGraph, 
                        vComp, eComp);

        return vf2.isomorphismExists();
    }
    
//------------------------------------------------------------------------------

    /**
     * Checks if this graph is "DENOPTIM-isostructural" to the other one given. 
     * "DENOPTIM-isostructural" means that the two graphs are isomorfic when:
     * <ul>
     * <li>the comparison of the vertexes considers any implementation of 
     * {@link Vertex} equals to itself unless two vertexes differ by the
     * return value of {@link Vertex#isRCV()},</li>
     * <li>the comparison of the vertexes considers only the type of bond the 
     * edge corresponds to (if any),</li>
     * <li>free {@link AttachmentPoint}s that are marked as required
     * in either graph are reflected in the other.</li>
     * </ul>
     * @param other
     * @return
     */
    public boolean isIsostructuralTo(DGraph other) {
        if (this.jGraphKernel == null)
        {
            this.jGraphKernel = GraphConversionTool.getJGraphKernelFromGraph(this);
        }
        if (other.jGraphKernel == null)
        {
            other.jGraphKernel = GraphConversionTool.getJGraphKernelFromGraph(other);
        }
        
        Comparator<Node> vComp = new Comparator<Node>() {
            
            Map<Node,Set<Node>> symmetryShortCuts = 
                    new HashMap<Node,Set<Node>>();
            
            @Override
            public int compare(Node v1, Node v2) {
                
                // exploit symmetric relations between vertexes
                if (symmetryShortCuts.containsKey(v1) 
                        && symmetryShortCuts.get(v1).contains(v2))
                {
                    return 0;
                }
                
                int result = v1.compare(v2);
                if (result==0) 
                {
                    Vertex dv1 = v1.getDNPVertex();
                    Vertex dv2 = v2.getDNPVertex();
                    if (dv1==null && dv2==null)
                    {
                        return result;
                    }
                    
                    Set<Node> symToV2 = new HashSet<Node>();
                    SymmetricSet ssV2 = dv2.getGraphOwner()
                            .getSymSetForVertex(dv2);
                    for (Integer sVrtId : ssV2.getList())
                    {
                        Vertex sv = dv2.getGraphOwner()
                                .getVertexWithId(sVrtId);
                        symToV2.add((Node) sv.getProperty(
                                Node.REFTOVERTEXKERNEL));
                    }
                    
                    Set<Node> symToV1 = new HashSet<Node>();
                    SymmetricSet ssV1 = dv1.getGraphOwner()
                            .getSymSetForVertex(dv1);
                    for (Integer sVrtId : ssV1.getList())
                    {
                        Vertex sv = dv1.getGraphOwner()
                                .getVertexWithId(sVrtId);
                        symToV1.add((Node) sv.getProperty(
                                Node.REFTOVERTEXKERNEL));
                    }
                    
                    for (Node v1s : symToV1)
                    {
                        if (symmetryShortCuts.containsKey(v1s))
                        {
                            symmetryShortCuts.get(v1s).addAll(symToV2);
                        } else {
                            symmetryShortCuts.put(v1s,symToV2);
                        }
                    }
                    return 0;
                }
                return result;
            }
        };
        
        Comparator<NodeConnection> eComp = NodeConnection::compare;
        
        VF2GraphIsomorphismInspector<Node, NodeConnection> vf2 =
                new VF2GraphIsomorphismInspector<>(this.jGraphKernel, 
                        other.jGraphKernel, vComp, eComp);

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
    public boolean sameAs(DGraph other, StringBuilder reason)
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
    	Map<Vertex,Vertex> vertexMap =
    			new HashMap<Vertex,Vertex>();

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
    	for (Ring rT : this.getRings())
    	{
    		Vertex vhT = rT.getHeadVertex();
    		Vertex vtT = rT.getTailVertex();
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

    private boolean sameAsRings(StringBuilder reason, Map<Vertex,
            Vertex> vertexMap, Ring rT, Vertex vhT,
                                Vertex vtT, Ring rO) {
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
    public static boolean compareGraphNodes(Vertex thisV,
                                            DGraph thisG,
                                            Vertex otherV,
                                            DGraph otherG) throws DENOPTIMException
    {
        Map<Vertex, Vertex> map = new HashMap<>();
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
    private static boolean compareGraphNodes(Vertex seedOnA,
    		DGraph gA, Vertex seedOnB, DGraph gB, 
    		Map<Vertex,Vertex> vertexMap, StringBuilder reason) 
    		        throws DENOPTIMException
    {
    	if (!seedOnA.sameAs(seedOnB, reason))
    	{
    		reason.append("Different vertex ("+seedOnA+":"+seedOnB+")");
    		return false;
    	}

    	ArrayList<Edge> edgesFromThis = gA.getEdgesWithSrc(seedOnA);
    	ArrayList<Edge> edgesFromOther = gB.getEdgesWithSrc(seedOnB);
    	if (edgesFromThis.size() != edgesFromOther.size())
    	{
    		reason.append("Different number of edges from vertex "+seedOnA+" ("
    					+edgesFromThis.size()+":"
    					+edgesFromOther.size()+")");
    		return false;
    	}

    	// pairwise correspondence between child vertices
    	ArrayList<Vertex[]> pairs = new ArrayList<Vertex[]>();

    	for (Edge et : edgesFromThis)
    	{
    		boolean found = false;
    		Edge eo = null;
    		StringBuilder innerSb = new StringBuilder();
    		int otherEdgeI = 0;
    		for (Edge e : edgesFromOther)
    		{
    		    innerSb.append(" Edge"+otherEdgeI+":");
    		    if (et.sameAs(e,innerSb))
    			{
    				found = true;
    				eo = e;
    				break;
    			}
    		}
    		if (!found)
    		{
    			reason.append("Edge not found in other("+et+"). "
    			        + "Edges in othes: "+innerSb.toString());
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

    		Vertex[] pair = new Vertex[]{
    				gA.getVertexWithId(et.getTrgVertex()),
    				gB.getVertexWithId(eo.getTrgVertex())};
    		pairs.add(pair);
    	}

    	//Recursion on child vertices
    	for (Vertex[] pair : pairs)
    	{
    		Vertex v = pair[0];
    		Vertex o = pair[1];
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
        for (Vertex v : getVertexList())
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
        for (Vertex v : getVertexList())
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

    public ArrayList<AttachmentPoint> getAttachmentPoints()
    {
        ArrayList<AttachmentPoint> lstAPs =
                new ArrayList<AttachmentPoint>();
        for (Vertex v : gVertices)
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

    public ArrayList<AttachmentPoint> getAvailableAPs()
    {
        ArrayList<AttachmentPoint> lstFreeAPs =
                new ArrayList<AttachmentPoint>();
        for (AttachmentPoint ap : getAttachmentPoints())
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

    public AttachmentPoint getAPWithId(int id)
    {
        AttachmentPoint ap = null;
        for (AttachmentPoint apCand : getAttachmentPoints())
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

    public boolean graphNeedsCappingGroups(FragmentSpace fragSpace)
    {
        for (Vertex v : getVertexList()) {
            for (AttachmentPoint ap : v.getAttachmentPoints()) {
                if (ap.isAvailable()  && fragSpace.getAPClassOfCappingVertex(
                        ap.getAPClass()) !=null
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

    public void removeCappingGroupsOn(Vertex vertex)
    {
        ArrayList<Vertex> toDel = new ArrayList<Vertex>();
        for (Vertex vtx : this.getChildVertices(vertex))
        {
            if (vtx instanceof Fragment == false)
            {
                continue;
            }
            // capping groups have fragment type 2
            if (((Fragment) vtx).getBuildingBlockType() == BBType.CAP
                    && !isVertexInRing(vtx))
            {
                toDel.add(vtx);
            }
        }

        for (Vertex v : toDel)
        {
            removeVertex(v);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Remove capping groups that belong to this graph and are in the given list.
     * @param lstVerts the list of vertexes to analyze.
     */

    public void removeCappingGroups(List<Vertex> lstVerts)
    {
        ArrayList<Integer> rvids = new ArrayList<>();
        for (int i=0; i<lstVerts.size(); i++)
        {
            Vertex vtx = lstVerts.get(i);
            if (vtx instanceof Fragment == false)
            {
                continue;
            }
            
            if (((Fragment) vtx).getBuildingBlockType() == BBType.CAP
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
    
    public void removeCappingGroupsFromChilds(List<Vertex> lstVerts)
    {
        for (Vertex v : lstVerts)
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
        removeCappingGroups(new ArrayList<Vertex>(gVertices));
    }
    
//------------------------------------------------------------------------------

    /**
     * Add a capping groups on free unused attachment points.
     * Addition of Capping groups does not update the symmetry table
     * for a symmetric graph.
     */

    public void addCappingGroups(FragmentSpace fragSpace) throws DENOPTIMException
    {
        if (!fragSpace.useAPclassBasedApproach())
            return;
        addCappingGroups(new ArrayList<Vertex>(gVertices), fragSpace);
    }
    
//------------------------------------------------------------------------------

    /**
     * Add a capping group on the given vertexes, if needed. The need for such 
     * groups is manifest when an attachment point that is not used in this
     * or any embedding lever (i.e., attachment point is 
     * {@link AttachmentPoint#isAvailableThroughout()}) has and 
     * {@link APClass} that demands to be capped.
     * Addition of Capping groups does not update the symmetry table
     * for a symmetric graph.
     * @param vertexAddedToThis list of vertexes to operate on. They must belong to this
     * graph.
     * @throws DENOPTIMException if the addition of capping groups cannot be 
     * performed.
     */

    public void addCappingGroups(List<Vertex> vertexAddedToThis, 
            FragmentSpace fragSpace) throws DENOPTIMException
    {
        if (!fragSpace.useAPclassBasedApproach())
            return;

        for (Vertex curVertex : vertexAddedToThis)
        {
            // no capping of a capping group. Since capping groups are expected
            // to have only one AP, there should never be a capping group with 
            // a free AP.
            if (curVertex.getBuildingBlockType() == Vertex.BBType.CAP)
            {
                continue;
            }

            for (AttachmentPoint curDap : curVertex.getAttachmentPoints())
            {
                if (curDap.isAvailableThroughout())
                {
                    APClass apcCap = fragSpace.getAPClassOfCappingVertex(
                            curDap.getAPClass());
                    if (apcCap != null)
                    {
                        int bbIdCap = fragSpace.getCappingFragment(apcCap);

                        if (bbIdCap != -1)
                        {
                            Vertex capVrtx = Vertex.newVertexFromLibrary(
                                        GraphUtils.getUniqueVertexIndex(), 
                                        bbIdCap, BBType.CAP, fragSpace);
                            DGraph molGraph = curDap.getOwner()
                                    .getGraphOwner();
                            if (molGraph == null)
                                throw new Error("Cannot add capping "
                                        + "groups to a vertex that does not "
                                        + "belong to a graph.");
                            molGraph.appendVertexOnAP(curDap, capVrtx.getAP(0));
                        }
                        else
                        {
                            String msg = "Capping is required but no proper "
                                    + "capping fragment found with APCalss " 
                                    + apcCap;
                            throw new Error(msg);
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

    public DGraph extractSubgraph(int index)
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

    public DGraph extractSubgraph(Vertex seed)
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

    public DGraph extractSubgraph(Vertex seed, int numLayers, 
            boolean stopBeforeRCVs) throws DENOPTIMException
    {
        if (!this.gVertices.contains(seed))
        {
            throw new DENOPTIMException("Attempt to extract a subgraph giving "
                    + "a seed vertex that is not contained in this graph.");
        }
        DGraph subGraph = this.clone();
        Vertex seedClone = subGraph.getVertexAtPosition(
                this.indexOf(seed));
        
        ArrayList<Vertex> subGrpVrtxs = new ArrayList<Vertex>();
        subGrpVrtxs.add(seedClone);
        subGraph.getChildrenTree(seedClone, subGrpVrtxs, numLayers, stopBeforeRCVs);
        ArrayList<Vertex> toRemove = new ArrayList<Vertex>();
        for (Vertex v : subGraph.gVertices)
        {
            if (!subGrpVrtxs.contains(v))
            {
                toRemove.add(v);
            }
        }
        
        for (Vertex v : toRemove)
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
    public DGraph extractSubgraph(Vertex seed, 
            List<Vertex> limits, boolean stopBeforeRCVs) 
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
        
        DGraph subGraph = this.clone();
        Vertex seedClone = subGraph.getVertexAtPosition(
                this.indexOf(seed));
        
        List<Vertex> limitsInClone =  new ArrayList<Vertex>();
        for (Vertex v : limits)
            limitsInClone.add(subGraph.getVertexAtPosition(this.indexOf(v)));
        
        ArrayList<Vertex> subGrpVrtxs = new ArrayList<Vertex>();
        subGrpVrtxs.add(seedClone);
        subGraph.getChildTreeLimited(seedClone, subGrpVrtxs, limitsInClone, 
                stopBeforeRCVs);
        
        ArrayList<Vertex> toRemove = new ArrayList<Vertex>();
        for (Vertex v : subGraph.gVertices)
        {
            if (!subGrpVrtxs.contains(v))
            {
                toRemove.add(v);
            }
        }
        for (Vertex v : toRemove)
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
    public DGraph extractSubgraph(Vertex seed, boolean stopBeforeRCVs) 
                    throws DENOPTIMException
    {
        if (!this.gVertices.contains(seed))
        {
            throw new DENOPTIMException("Attempt to extract a subgraph giving "
                    + "a seed vertex that is not contained in this graph.");
        }
        
        DGraph subGraph = this.clone();
        Vertex seedClone = subGraph.getVertexAtPosition(
                this.indexOf(seed));
        
        ArrayList<Vertex> subGrpVrtxs = new ArrayList<Vertex>();
        subGrpVrtxs.add(seedClone);
        subGraph.getChildTreeLimited(seedClone, subGrpVrtxs, stopBeforeRCVs);
        
        ArrayList<Vertex> toRemove = new ArrayList<Vertex>();
        for (Vertex v : subGraph.gVertices)
        {
            if (!subGrpVrtxs.contains(v))
            {
                toRemove.add(v);
            }
        }
        for (Vertex v : toRemove)
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
    public DGraph extractSubgraph(List<Vertex> members) 
    {
        if (members.size()==0)
            return null;
        
        DGraph subGraph = this.clone();
        
        List<Vertex> subGrpVrtxs =  new ArrayList<Vertex>();
        for (Vertex v : members)
        {
            subGrpVrtxs.add(subGraph.getVertexAtPosition(this.indexOf(v)));
        }
        
        ArrayList<Vertex> toRemove = new ArrayList<Vertex>();
        for (Vertex v : subGraph.gVertices)
        {
            if (!subGrpVrtxs.contains(v))
            {
                toRemove.add(v);
            }
        }
        for (Vertex v : toRemove)
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
      
    public List<DGraph> extractPattern(GraphPattern pattern) 
            throws DENOPTIMException 
    {
        if (pattern != GraphPattern.RING) {
            throw new IllegalArgumentException("Graph pattern " + pattern +
                    " not supported.");
        }

        List<Set<Vertex>> disjointMultiCycleVertices = this
                  .getRings()
                  .stream()
                  .map(Ring::getVertices)
                  .map(HashSet::new)
                  .collect(Collectors.toList());

        GenUtils.unionOfIntersectingSets(disjointMultiCycleVertices);

        List<DGraph> subgraphs = new ArrayList<>();
        for (Set<Vertex> fusedRing : disjointMultiCycleVertices) {
            subgraphs.add(extractSubgraph(fusedRing));
        }
          
        for (DGraph g : subgraphs) {
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
    private DGraph extractSubgraph(Set<Vertex> definedOn) 
    {
        DGraph subgraph = this.clone();

        Set<Vertex> complement = subgraph
                .getVertexList()
                .stream()
                .filter(u -> definedOn
                        .stream()
                        .allMatch(v -> v.getVertexId() != u.getVertexId())
                ).collect(Collectors.toSet());

        for (Vertex v : complement) {
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
      private static void reorderVertexList(DGraph g) 
      {
          Vertex newScaffold = g.getSourceVertex();
          if (newScaffold == null) {
              return;
          }
          DGraph.setScaffold(newScaffold);
          fixEdgeDirections(g);
      }

//------------------------------------------------------------------------------

    /**
     * Flips edges in the graph so that the scaffold is the only source vertex.
     * @param graph to fix edges of.
     */
    private static void fixEdgeDirections(DGraph graph) 
    {
        Vertex src = graph.getSourceVertex();
        fixEdgeDirections(src, new HashSet<>());
    }

//------------------------------------------------------------------------------

    /**
     * Recursive utility method for fixEdgeDirections(DENOPTIMGraph graph).
     * @param v current vertex
     */
    private static void fixEdgeDirections(Vertex v, 
            Set<Integer> visited) 
    {
        visited.add(v.getVertexId());
        int visitedVertexEncounters = 0;
        for (int i = 0; i < v.getNumberOfAPs(); i++) {
            AttachmentPoint ap = v.getAP(i);
            Edge edge = ap.getEdgeUser();
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

    public boolean removeBranchStartingAt(Vertex v, boolean symmetry)
            throws DENOPTIMException
    {
        boolean res = true;
        if (hasSymmetryInvolvingVertex(v) && symmetry)
        {
            ArrayList<Vertex> toRemove = new ArrayList<Vertex>();
            for (int i=0; i<getSymSetForVertexID(v.getVertexId()).size(); i++)
            {
                int svid = getSymSetForVertexID(v.getVertexId()).getList().get(i);
                toRemove.add(getVertexWithId(svid));
            }
            for (Vertex sv : toRemove)
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

    public boolean removeBranchStartingAt(Vertex v)
            throws DENOPTIMException
    {
        Edge edgeToParent = v.getEdgeToParent();
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

    public boolean removeOrphanBranchStartingAt(Vertex v)
            throws DENOPTIMException
    {
        // now get the vertices attached to v
        ArrayList<Vertex> children = new ArrayList<Vertex>();
        getChildrenTree(v, children);

        // delete the children vertices and associated edges
        for (Vertex c : children) {
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
    public boolean removeChainUpToBranching(Vertex v, 
            FragmentSpace fragSpace) throws DENOPTIMException
    {
        List<Ring> rings = getRingsInvolvingVertex(v);
        if (rings.isEmpty())
        {
            return false;
        }
        
        // Identify the RCVs defining the chord that might become an edge
        // The corresponding ring is the "frame" we'll use throughout this 
        // operation, and is the smallest ring that is closest to the appointed
        // vertex.
        Ring frame = null;
        Vertex[] replacedByEdge = new Vertex[2];
        int minDist = Integer.MAX_VALUE;
        BondType bt = null;
        for (Ring r : rings)
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
            Vertex vi = frame.getVertexAtPosition(i);
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
        List<Vertex> remainingChain = new ArrayList<Vertex>();
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
        List<Vertex> toRemoveChain = new ArrayList<Vertex>();
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
            for (Vertex vtr : toRemoveChain)
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
        Vertex deepestVrtRemainingChain = null;
        for (Vertex vint : remainingChain)
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
            AttachmentPoint apSrcOfNewEdge = replacedByEdge[0]
                    .getEdgeToParent().getSrcAP();
            AttachmentPoint apTrgOfNewEdge = replacedByEdge[1]
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
            addEdge(new Edge(apSrcOfNewEdge, apTrgOfNewEdge, bt));
        } else {
            // We remove the frame already, otherwise we will try to recreate 
            // such ring from RCVs and waste time with it.
            removeRing(frame);
        }
        
        // Now, inspect the paths from the deepest vertex and outwards, to
        // find out where to start reversing edges.
        List<Vertex> chainToReverseA = new ArrayList<Vertex>();
        List<Vertex> chainToReverseB = new ArrayList<Vertex>();
        for (int i=(remainingChain.indexOf(deepestVrtRemainingChain)+1); 
                i<remainingChain.size(); i++)
        {
            Vertex vPrev = remainingChain.get(i-1);
            Vertex vHere = remainingChain.get(i);
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
            Vertex vPrev = remainingChain.get(i+1);
            Vertex vHere = remainingChain.get(i);
            if (!vPrev.getChilddren().contains(vHere))
            {
                if (chainToReverseB.size()==0)
                    chainToReverseB.add(vPrev);
                chainToReverseB.add(vHere);
            }
        }
        
        // These are to remember all chords that will have to be recreated
        LinkedHashMap<Vertex,Vertex> chordsToRecreate = 
                new LinkedHashMap<Vertex,Vertex>();
        LinkedHashMap<Vertex,BondType> chordsToRecreateBB = 
                new LinkedHashMap<Vertex,BondType>();
        
        // Change direction of those edges that have to be reversed as a 
        // consequence of the change in the spanning tree.
        if (chainToReverseA.size()+chainToReverseB.size() > 1)
        {
            List<Vertex> chainToWorkOn = null;
            for (int ic=0; ic<2; ic++)
            {
                if (ic == 1)
                    chainToWorkOn = chainToReverseA;
                else
                    chainToWorkOn = chainToReverseB;
            
                for (int i=1; i<chainToWorkOn.size(); i++)
                {
                    Vertex vHere = chainToWorkOn.get(i);
                    Vertex vPrev = chainToWorkOn.get(i-1);
                    List<Ring> ringsToRecreate = new ArrayList<>();
                    for (Ring r : getRingsInvolvingVertex(vHere))
                    {
                        ringsToRecreate.add(r);
                        chordsToRecreate.put(r.getHeadVertex(), 
                                r.getTailVertex());
                        chordsToRecreateBB.put(r.getHeadVertex(), 
                                r.getBondType());
                    }
                    for (Ring r : ringsToRecreate)
                    {
                        removeRing(r);
                    }
                    
                    Edge edgeToPrevious = vHere.getEdgeWith(vPrev);
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
                            AttachmentPoint newSrcAP = 
                                    edgeToPrevious.getTrgAP();
                            AttachmentPoint newTrgAP = 
                                    edgeToPrevious.getSrcAP();
                            if (newSrcAP.getAPClass().isCPMapCompatibleWith(
                                    newTrgAP.getAPClass(), fragSpace))
                            {
                                BondType oldBt = edgeToPrevious.getBondType();
                                removeEdge(edgeToPrevious);
                                addEdge(new Edge(newSrcAP, newTrgAP, 
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
        for (Vertex vtr : toRemoveChain) 
        {
            // This works across template boundaries!
            for (Vertex child : vtr.getChildrenThroughout())
            {
                if (remainingChain.contains(child)
                        || toRemoveChain.contains(child))
                    continue;
                DGraph ownerOfChild = child.getGraphOwner();
                ownerOfChild.removeVertex(child);
            }
            if (templateJacket!= null)
            {
                List<AttachmentPoint> apProjectionsToRemove = 
                        new ArrayList<AttachmentPoint>();
                for (AttachmentPoint outerAP : 
                    templateJacket.getAttachmentPoints())
                {
                    AttachmentPoint innerAP = 
                            templateJacket.getInnerAPFromOuterAP(outerAP);
                    if (innerAP.getOwner() == vtr)
                    {
                        apProjectionsToRemove.add(innerAP);
                    }
                }
                for (AttachmentPoint apToRemove : apProjectionsToRemove)
                    templateJacket.removeProjectionOfInnerAP(apToRemove);
            }
            removeVertex(vtr);
        }
        
        // Regenerate the rings that have been affected
        for (Vertex h : chordsToRecreate.keySet())
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
            for (AttachmentPoint innerAP : getAvailableAPs())
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
                Edge e = getEdgeList().get(j);
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
            Vertex v = getVertexList().get(i);
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
    
    public int getLevel(Vertex v)
    {
        ArrayList<Vertex> parentTree = new ArrayList<>();
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
     * and pseudo-atom in the molecular representation,
     * such coordinates do <b>NOT</b> represent a valid 3D model.
     * As a consequence stereochemical descriptors in the INCHI representation
     * are not consistent with the actual arrangement of fragments.
     * @return an object array containing the inchi code, the SMILES string
     *         and the 2D representation of the molecule.
     *         <code>null</code> is returned if any check or conversion fails.
     * @throws DENOPTIMException
     */
    
    public Object[] evaluateGraph(RunTimeParameters settings)
            throws DENOPTIMException
    {
        RingClosureParameters rcSettings = new RingClosureParameters();
        if (settings.containsParameters(ParametersType.RC_PARAMS))
        {
            rcSettings = (RingClosureParameters)settings.getParameters(
                    ParametersType.RC_PARAMS);
        }
        FragmentSpaceParameters fsSettings = new FragmentSpaceParameters();
        if (settings.containsParameters(ParametersType.FS_PARAMS))
        {
            fsSettings = (FragmentSpaceParameters)settings.getParameters(
                    ParametersType.FS_PARAMS);
        }
        
        // calculate the molecule representation
        ThreeDimTreeBuilder t3d = new ThreeDimTreeBuilder(settings);
        t3d.setAlidnBBsIn3D(false);
        IAtomContainer mol = t3d.convertGraphTo3DAtomContainer(this,true);
        if (mol == null)
        {
            String msg ="Evaluation of graph: graph-to-mol returned null!"
                    + toString();
            settings.getLogger().log(Level.FINE, msg);
            return null;
        }

        // check if the molecule is connected
        boolean isConnected = ConnectivityChecker.isConnected(mol);
        if (!isConnected)
        {
            String msg = "Evaluation of graph: Not all connected"
                    + toString();
            settings.getLogger().log(Level.FINE, msg);
            return null;
        }

        // SMILES
        String smiles = null;
        smiles = MoleculeUtils.getSMILESForMolecule(mol,settings.getLogger());
        if (smiles == null)
        {
            String msg = "Evaluation of graph: SMILES is null! "
                    + toString();
            settings.getLogger().log(Level.FINE, msg);
            smiles = "FAIL: NO SMILES GENERATED";
        }
        // if by chance the smiles indicates a disconnected molecule
        if (smiles.contains("."))
        {
            String msg = "Evaluation of graph: SMILES contains \".\"" + smiles;
            settings.getLogger().log(Level.FINE, msg);
            return null;
        }

        // criteria from definition of Fragment space
        // 1A) number of heavy atoms
        if (fsSettings.getMaxHeavyAtom() > 0)
        {
            if (MoleculeUtils.getHeavyAtomCount(mol) >
                fsSettings.getMaxHeavyAtom())
            {
                String msg = "Evaluation of graph: Max atoms constraint "
                        + " violated: " + smiles;
                settings.getLogger().log(Level.FINE, msg);
                return null;
            }
        }

        // 1B) molecular weight
        double mw = MoleculeUtils.getMolecularWeight(mol);
        if (fsSettings.getMaxMW() > 0)
        {
            if (mw > fsSettings.getMaxMW())
            {
                String msg = "Evaluation of graph: Molecular weight "
                        + "constraint violated: " + smiles + " | MW: " + mw;
                settings.getLogger().log(Level.FINE, msg);
                return null;
            }
        }
        mol.setProperty("MOL_WT", mw);

        // 1C) number of rotatable bonds
        int nrot = MoleculeUtils.getNumberOfRotatableBonds(mol);
        if (fsSettings.getMaxRotatableBond() > 0)
        {
            if (nrot > fsSettings.getMaxRotatableBond())
            {
                String msg = "Evaluation of graph: Max rotatable bonds "
                        + "constraint violated: "+ smiles;
                settings.getLogger().log(Level.FINE, msg);
                return null;
            }
        }
        mol.setProperty("ROT_BND", nrot);

        // 1D) unacceptable free APs
        if (fsSettings.getFragmentSpace().useAPclassBasedApproach())
        {
            if (hasForbiddenEnd(fsSettings))
            {
                String msg = "Evaluation of graph: forbidden end in graph!";
                settings.getLogger().log(Level.FINE, msg);
                return null;
            }
        }

        // criteria from settings of ring closures
        if (rcSettings.allowRingClosures())
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
                if (nThisType > rcSettings.getMaxRcaPerType() ||
                        nCompType > rcSettings.getMaxRcaPerType())
                {
                    String msg = "Evaluation of graph: too many RCAs! "
                            + rcaTyp + ":" + nThisType + " "
                            + rcaTypes.get(rcaTyp) + ":" + nCompType;
                    settings.getLogger().log(Level.FINE, msg);
                    return null;
                }
                if (nThisType < rcSettings.getMinRcaPerType() ||
                        nCompType < rcSettings.getMinRcaPerType())
                {
                    String msg = "Evaluation of graph: too few RCAs! "
                            + rcaTyp + ":" + nThisType + " "
                            + rcaTypes.get(rcaTyp) + ":" + nCompType;
                    settings.getLogger().log(Level.FINE, msg);
                    return null;
                }

                nPossRings = nPossRings + Math.min(nThisType, nCompType);
                doneType.add(rcaTyp);
                doneType.add(rcaTypes.get(rcaTyp));
            }
            if (nPossRings < rcSettings.getMinRingClosures())
            {
                String msg = "Evaluation of graph: too few ring candidates";
                settings.getLogger().log(Level.FINE, msg);
                return null;
            }
        }

        // get the smiles/Inchi representation
        ObjectPair pr = MoleculeUtils.getInChIForMolecule(mol, 
                settings.getLogger());
        if (pr.getFirst() == null)
        {
            String msg = "Evaluation of graph: INCHI is null!";
            settings.getLogger().log(Level.FINE, msg);
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
    public ArrayList<DGraph> makeAllGraphsWithDifferentRingSets(
            RunTimeParameters settings)
            throws DENOPTIMException 
    {
        RingClosureParameters rcSettings = new RingClosureParameters();
        if (settings.containsParameters(ParametersType.RC_PARAMS))
        {
            rcSettings = (RingClosureParameters)settings.getParameters(
                    ParametersType.RC_PARAMS);
        }
        FragmentSpaceParameters fsSettings = new FragmentSpaceParameters();
        if (settings.containsParameters(ParametersType.FS_PARAMS))
        {
            fsSettings = (FragmentSpaceParameters)settings.getParameters(
                    ParametersType.FS_PARAMS);
        }
        ArrayList<DGraph> lstGraphs = new ArrayList<>();

        boolean rcnEnabled = fsSettings.getFragmentSpace()
                .useAPclassBasedApproach();
        if (!rcnEnabled)
            return lstGraphs;

        boolean evaluateRings = rcSettings.allowRingClosures();
        if (!evaluateRings)
            return lstGraphs;

        // get a atoms/bonds molecular representation (no 3D needed)
        ThreeDimTreeBuilder t3d = new ThreeDimTreeBuilder(settings);
        t3d.setAlidnBBsIn3D(false);
        IAtomContainer mol = t3d.convertGraphTo3DAtomContainer(this,false);

        // Set rotatable property as property of IBond
        RotationalSpaceUtils.defineRotatableBonds(mol,
                fsSettings.getRotSpaceDefFile(), true, true,
                settings.getLogger());

        // get the set of possible RCA combinations = ring closures
        CyclicGraphHandler cgh = new CyclicGraphHandler(rcSettings,
                fsSettings.getFragmentSpace());
        ArrayList<List<Ring>> allCombsOfRings =
                cgh.getPossibleCombinationOfRings(mol, this);

        // Keep closable chains that are relevant for chelate formation
        if (rcSettings.buildChelatesMode())
        {
            ArrayList<List<Ring>> toRemove = new ArrayList<>();
            for (List<Ring> setRings : allCombsOfRings)
            {
                if (!cgh.checkChelatesGraph(this,setRings))
                {
                    toRemove.add(setRings);
                }
            }
            allCombsOfRings.removeAll(toRemove);
        }

        // prepare output graphs
        for (List<Ring> ringSet : allCombsOfRings)
        {
            // clone root graph
            DGraph newGraph = this.clone();

            Map<Integer,Integer> vRenum = newGraph.renumberVerticesGetMap();
            newGraph.setGraphId(GraphUtils.getUniqueGraphIndex());

            // add rings
            for (Ring oldRing : ringSet)
            {
                Ring newRing = new Ring();
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

    public boolean hasForbiddenEnd(FragmentSpaceParameters fsSettings)
    {
        ArrayList<Vertex> vertices = getVertexList();
        Set<APClass> classOfForbEnds = 
                fsSettings.getFragmentSpace().getForbiddenEndList();
        boolean found = false;
        for (Vertex vtx : vertices)
        {
            ArrayList<AttachmentPoint> daps = vtx.getAttachmentPoints();
            for (AttachmentPoint dp : daps)
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
                        fsSettings.getLogger().log(Level.WARNING, msg);
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

    public void appendGraphOnGraph(ArrayList<Vertex> parentVertices,
                                   ArrayList<Integer> parentAPIdx,
                                   DGraph subGraph,
                                   Vertex childVertex, int childAPIdx,
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

    public void appendVertexOnAP(AttachmentPoint srcAP, 
            AttachmentPoint trgAP) throws DENOPTIMException 
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
        Edge edge = new Edge(srcAP,trgAP, bndTyp);
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

    public void appendGraphOnAP(AttachmentPoint apOnThisGraph,
            AttachmentPoint apOnIncomingGraph, BondType bndType)
                                        throws DENOPTIMException
    {
        DGraph incomingGraph = apOnIncomingGraph.getOwner().getGraphOwner();
        incomingGraph.renumberGraphVertices();

        Edge edge = new Edge(apOnThisGraph, apOnIncomingGraph, 
                bndType);
        addEdge(edge);
        
        //Import vertexes
        for (Vertex incomingVrtx : incomingGraph.getVertexList())
        {
            addVertex(incomingVrtx);
        }
        
        //Import edges
        for (Edge incomingEdge : incomingGraph.getEdgeList())
        {
            addEdge(incomingEdge);
        }
        
        //Import rings
        for (Ring incomingRing : incomingGraph.getRings())
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

    public void appendGraphOnAP(Vertex parentVertex, int parentAPIdx,
                                DGraph subGraph,
                                Vertex childVertex, int childAPIdx,
                                BondType bndType,
                                Map<Integer,SymmetricSet> newSymSets)
                                        throws DENOPTIMException
    {
        // Clone and renumber the subgraph to ensure uniqueness
        DGraph sgClone = subGraph.clone();
        // The clones have the same vertex IDs before renumbering vertices
        Vertex cvClone = sgClone.getVertexWithId(
                childVertex.getVertexId());
        sgClone.renumberGraphVertices();

        Edge edge = new Edge(parentVertex.getAP(parentAPIdx),
                cvClone.getAP(childAPIdx), bndType);
        addEdge(edge);

        // Import all vertices from cloned subgraph, i.e., sgClone
        for (int i=0; i<sgClone.getVertexList().size(); i++)
        {
            Vertex clonV = sgClone.getVertexList().get(i);
            Vertex origV = subGraph.getVertexList().get(i);

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

    public void appendGraphOnGraph(Vertex parentVertex,
                                   int parentAPIdx, DGraph subGraph,
                                   Vertex childVertex, int childAPIdx,
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
     * @param logger manager of log
     * @return the list of matches
     */
    public ArrayList<Integer> findVerticesIds(VertexQuery query, Logger logger)
    {
        ArrayList<Integer> matches = new ArrayList<>();
        for (Vertex v : findVertices(query, logger))
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
     * @param logger manager of log
     * @return the list of vertexes that match the query.
     */

    public ArrayList<Vertex> findVertices(VertexQuery vrtxQuery, Logger logger)
    {
        ArrayList<Vertex> matches = new ArrayList<>(getVertexList());

        logger.log(Level.FINE, "Candidates: " + matches);

        //Check condition vertex ID
        Integer vidQuery = vrtxQuery.getVertexIDQuery();
        if (vidQuery != null)
        {
            ArrayList<Vertex> newLst = new ArrayList<>();
            for (Vertex v : matches)
            {
                if (v.getVertexId() == vidQuery.intValue())
                {
                    newLst.add(v);
                }
            }
            matches = newLst;
        }
        logger.log(Level.FINE,"  After filtering by vertex ID: " + matches);
        
        //Check condition vertex type (NB: essentially the vertex implementation
        VertexType vtQuery = vrtxQuery.getVertexTypeQuery();
        if (vtQuery != null)
        {
            ArrayList<Vertex> newLst = new ArrayList<>();
            for (Vertex v : matches)
            {
                if (v.getVertexType() == vtQuery)
                {
                    newLst.add(v);
                }
            }
            matches = newLst;
            logger.log(Level.FINER, "  After filtering by vertex type: "
                    + matches);
        }
        
        //Check condition building block Type
        BBType bbtQuery = vrtxQuery.getVertexBBTypeQuery();
        if (bbtQuery != null)
        {
            ArrayList<Vertex> newLst = new ArrayList<>();
            for (Vertex v : matches)
            {
                if (v.getBuildingBlockType() == bbtQuery)
                {
                    newLst.add(v);
                }
            }
            matches = newLst;
            logger.log(Level.FINER, "  After filtering by building block "
                        + "type: " + matches);
        }
        
        //Check condition building block ID
        Integer bbID = vrtxQuery.getVertexBBIDQuery();
        if (bbID != null)
        {
            ArrayList<Vertex> newLst = new ArrayList<>();
            for (Vertex v : matches)
            {   
                if (v.getBuildingBlockId() == bbID.intValue())
                {
                    newLst.add(v);
                }
            }
            matches = newLst;
            logger.log(Level.FINER, "  After filtering by building block ID: " 
                        + matches);
        } 

        //Check condition: level of vertex
        Integer levelQuery = vrtxQuery.getVertexLevelQuery();
        if (levelQuery != null)
        {
            ArrayList<Vertex> newLst = new ArrayList<Vertex>();
            for (Vertex v : matches)
            {
                if (getLevel(v) == levelQuery)
                {
                    newLst.add(v);
                }
            }
            matches = newLst;
            logger.log(Level.FINER, "  After filtering by level: " + matches);
        }
        
        logger.log(Level.FINE, "After all vertex-based filters: " + matches);
        
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
                ArrayList<Vertex> newLst = new ArrayList<>();
                for (Vertex v : matches)
                {
                    for (Edge e : edgeFinder.apply(v))
                    {
                        if (e.getTrgAPID() == eTrgApIDx)
                        {
                            newLst.add(v);
                            break;
                        }
                    }
                }
                matches = newLst;
                logger.log(Level.FINER, "  After " + inOrOut 
                            + " edge trgAPID filter: " + matches);
            }
            
            Integer eInSrcApIDx = edgeQuery.getSourceAPIdx();
            if (eInSrcApIDx != null)
            {
                ArrayList<Vertex> newLst = new ArrayList<>();
                for (Vertex v : matches)
                {
                    for (Edge e : edgeFinder.apply(v))
                    {
                        if (e != null && e.getSrcAPID() == eInSrcApIDx)
                        {
                            newLst.add(v);
                            break;
                        }
                    }
                }
                matches = newLst;
                logger.log(Level.FINER, "  After " + inOrOut 
                            + " edge srcAPID filter: " + matches);
            }
            
            if (i==0)
            {
                Integer eSrcVrtID = edgeQuery.getSourceVertexId();
                if (eSrcVrtID != null)
                {
                    ArrayList<Vertex> newLst = new ArrayList<>();
                    for (Vertex v : matches)
                    {
                        for (Edge e : edgeFinder.apply(v))
                        {
                            if(e.getSrcAP().getOwner().getVertexId()==eSrcVrtID)
                            {
                                newLst.add(v);
                                break;
                            }
                        }
                    }
                    matches = newLst;
                    logger.log(Level.FINER, "  After " + inOrOut 
                                + " edge src VertexID filter: " + matches);
                }
            } else if (i==1) {
                Integer eTrgVrtID = edgeQuery.getTargetVertexId();
                if (eTrgVrtID != null)
                {
                    ArrayList<Vertex> newLst = new ArrayList<>();
                    for (Vertex v : matches)
                    {
                        for (Edge e : edgeFinder.apply(v))
                        {
                            if(e.getTrgAP().getOwner().getVertexId()==eTrgVrtID)
                            {
                                newLst.add(v);
                                break;
                            }
                        }
                    }
                    matches = newLst;
                    logger.log(Level.FINER, "  After " + inOrOut 
                                + " edge trg VertexID filter: " + matches);
                }
            }

            BondType btQuery = edgeQuery.getBondType();
            if (btQuery != null)
            {
                ArrayList<Vertex> newLst = new ArrayList<>();
                for (Vertex v : matches)
                {
                    for (Edge e : edgeFinder.apply(v))
                    {
                        if (e.getBondType() == btQuery)
                        {
                            newLst.add(v);
                            break;
                        }
                    }
                }
                matches = newLst;
                logger.log(Level.FINER, "  After " + inOrOut 
                            + " edge bond type filter: " + matches);
            }

            APClass srcAPC = edgeQuery.getSourceAPClass();
            if (srcAPC != null)
            {
                ArrayList<Vertex> newLst = new ArrayList<>();
                for (Vertex v : matches)
                {
                    for (Edge e : edgeFinder.apply(v))
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
                ArrayList<Vertex> newLst = new ArrayList<>();
                for (Vertex v : matches)
                {
                    for (Edge e : edgeFinder.apply(v))
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
            logger.log(Level.FINER, "After all " + inOrOut 
                        + " edge-based filters: " + matches);
        }
    
        // Identify symmetric sets and keep only one member
        removeSymmetryRedundance(matches);

        logger.log(Level.FINE, "Final Matches (after symmetry): " + matches);

        return matches;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Utility to make selection of edges to a vertex tunable by a parameter.
     * The parameter given to the constructor defines whether we take the 
     * incoming or the outgoing edges of a vertex that is given as the argument
     * of this function.
     */
    private class EdgeFinder implements Function<Vertex,List<Edge>> 
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
        public List<Edge> apply(Vertex v)
        {
            List<Edge> edges = new ArrayList<Edge>();
            if (mode < 0)
            {
                Edge eToParent = v.getEdgeToParent();
                if (eToParent != null)
                    edges.add(eToParent);
            } else {
                for (AttachmentPoint ap : v.getAttachmentPoints())
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

    public void removeSymmetryRedundance(ArrayList<Vertex> list) {
        ArrayList<Vertex> symRedundant = new ArrayList<>();
        Iterator<SymmetricSet> itSymm = getSymSetsIterator();
        while (itSymm.hasNext())
        {
            SymmetricSet ss = itSymm.next();
            for (Vertex v : list)
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
        for (Vertex v : symRedundant)
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
        ArrayList<Vertex> vList = new ArrayList<>();
        for (int vid : list) {
            vList.add(getVertexWithId(vid));
        }
        removeSymmetryRedundance(vList);
        list.clear();
        for (Vertex v : vList) {
            list.add(v.getVertexId());
        }
    }

//------------------------------------------------------------------------------

    /**
     * Edit this graph according to a given list of edit tasks. This method 
     * assumes the vertex IDs are unique.
     * @param edits the list of edit tasks.
     * @param symmetry if <code>true</code> the same operation is performed on
     * vertexes related by symmetry.
     * @param logger the logger to use
     * @return the modified graph.
     */

    public DGraph editGraph(ArrayList<GraphEdit> edits,
            boolean symmetry, Logger logger) throws DENOPTIMException
    {
        DGraph modGraph = this.clone();

        for (GraphEdit edit : edits)
        {
            logger.log(Level.FINE, "Graph edit task: " + edit.getType());

            switch (edit.getType())
            {
                case REPLACECHILD:
                {
                    DGraph inGraph = edit.getIncomingGraph();
                    VertexQuery query = edit.getVertexQuery();
                    int idAPOnInGraph = -1; // Initialisation to invalid value
                    Vertex rootOfInGraph = null;
                    if (edit.getIncomingAPId() != null)
                    {
                        AttachmentPoint ap = inGraph.getAPWithId(
                                edit.getIncomingAPId().intValue());
                        idAPOnInGraph = ap.getIndexInOwner();
                        rootOfInGraph = ap.getOwner();
                    } else {
                        ArrayList<AttachmentPoint> freeAPs = 
                                inGraph.getAvailableAPs();
                        if (freeAPs.size()==1)
                        {
                            AttachmentPoint ap = freeAPs.get(0);
                            idAPOnInGraph = ap.getIndexInOwner();
                            rootOfInGraph = ap.getOwner();
                        } else {
                            String geClsName = GraphEdit.class.getSimpleName();
                            String msg = "Skipping " + edit.getType() + "on "
                                    + "graph " + getGraphId() + ". The incoming"
                                    + " graph has more than one free AP and "
                                    + "the " + geClsName + " "
                                    + "does not provide sufficient information "
                                    + "to unambiguously choose one AP. "
                                    + "Please, add 'idAPOnIncomingGraph' in "
                                    + "the definition of " + geClsName + ".";
                            logger.log(Level.WARNING, msg);
                        }
                    }
                    
                    ArrayList<Vertex> matches = modGraph.findVertices(query, 
                            logger);
                    if (symmetry)
                    {
                        modGraph.removeSymmetryRedundance(matches);
                    }
                    for (Vertex vertexToReplace : matches)
                    {
                        Edge edgeToParent = 
                                vertexToReplace.getEdgeToParent();
                        if (edgeToParent == null)
                        {
                            //The matched vertex has no parent, therefore there
                            // the change would correspond to changing the graph 
                            // completely. This is unlikely the desired effect, 
                            //so we do not do anything.
                            continue;
                        }
                        Vertex parent = vertexToReplace.getParent();
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
                    ArrayList<Vertex> matches = modGraph.findVertices(
                            edit.getVertexQuery(), logger);
                    for (Vertex vertexToRemove : matches)
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
    public List<Vertex> getMutableSites()
    {
        List<Vertex> mutableSites = new ArrayList<Vertex>();
        for (Vertex v : gVertices)
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
    public List<Vertex> getMutableSites(List<MutationType> ignoredTypes)
    {
        List<Vertex> mutableSites = new ArrayList<Vertex>();
        for (Vertex v : gVertices)
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
        for (Vertex v : gVertices)
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
            for (AttachmentPoint ap : v.getAttachmentPoints())
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
            for (Vertex v : gVertices)
            {
                for (AttachmentPoint ap : v.getAttachmentPoints())
                {
                    ap.setID(GraphUtils.getUniqueAPIndex());
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

    public static DGraph fromJson(String json)
    {
        Gson gson = DENOPTIMgson.getReader();
        DGraph graph = gson.fromJson(json, DGraph.class);
        return graph;
    }

//------------------------------------------------------------------------------

    /**
     * Reads a JSON string and returns an instance of this class.
     * @param json the string to parse.
     * @return a new instance of this class.
     */

    public static DGraph fromJson(Reader reader)
    {
        Gson gson = DENOPTIMgson.getReader();
        DGraph graph = gson.fromJson(reader, DGraph.class);
        return graph;
    }

//------------------------------------------------------------------------------

    /**
     * We expect unique IDs for vertices and attachment points.
     */
    public static class DENOPTIMGraphSerializer
    implements JsonSerializer<DGraph>
    {
        @Override
        public JsonElement serialize(DGraph g, Type typeOfSrc,
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
    implements JsonDeserializer<DGraph>
    {
        @Override
        public DGraph deserialize(JsonElement json, Type typeOfT,
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
                .registerTypeAdapter(Vertex.class,
                      new DENOPTIMVertexDeserializer())
                .registerTypeAdapter(APClass.class, new APClassDeserializer())
                .setPrettyPrinting()
                .create();

            DGraph graph = gson.fromJson(partialJsonObj,
                    DGraph.class);

            // Refresh APs
            for (Vertex v : graph.getVertexList())
            {
                // Regenerate reference to fragment owner
                v.setGraphOwner(graph);

                for (AttachmentPoint ap : v.getAttachmentPoints())
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
                AttachmentPoint srcAP = graph.getAPWithId(
                        o.get("srcAPID").getAsInt());
                AttachmentPoint trgAP = graph.getAPWithId(
                        o.get("trgAPID").getAsInt());

                Edge edge = new Edge(srcAP, trgAP,
                        context.deserialize(o.get("bondType"),BondType.class));
                graph.addEdge(edge);
            }

            // Now, recover the rings
            JsonArray ringArr = jsonObject.get("gRings").getAsJsonArray();
            for (JsonElement e : ringArr)
            {
                JsonObject o = e.getAsJsonObject();
                Ring ring = new Ring();
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
    public static void setScaffold(Vertex v) {
        ArrayList<Vertex> newVertexList = new ArrayList<>();

        Set<Integer> visited = new HashSet<>();
        Queue<Vertex> currLevel = new ArrayDeque<>();
        Queue<Vertex> nextLevel = new ArrayDeque<>();
        currLevel.add(v);

        while (!currLevel.isEmpty()) {
            Vertex currVertex = currLevel.poll();

            int currId = currVertex.getVertexId();
            if (!visited.contains(currId)) {
                visited.add(currId);

                newVertexList.add(currVertex);

                Iterable<Vertex> neighbors = currVertex
                        .getAttachmentPoints()
                        .stream()
                        .map(AttachmentPoint::getEdgeUser)
                        .filter(e -> e != null)
                        .map(e -> e.getSrcVertex() == currId ?
                                e.getTrgAP() : e.getSrcAP())
                        .map(AttachmentPoint::getOwner)
                        .collect(Collectors.toList());
                for (Vertex adj : neighbors) {
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
    public void setTemplateJacket(Template template)
    {
        this.templateJacket = template;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return the template that contains this graph or null.
     */
    public Template getTemplateJacket()
    {
        return templateJacket;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return the outermost graph object that can be reached from this 
     * possibly embedded graph.
     */
    public DGraph getOutermostGraphOwner()
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
    public List<Template> getEmbeddingPath()
    {
        List<Template> path = new ArrayList<Template>();
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
        for (Vertex v : gVertices)
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
     * @return the {@link AttachmentPoint} or null is the two vertex IDs
     * are not connected in this graph.
     */
    public AttachmentPoint getAPOnLeftVertexID(int vid1, int vid2)
    {
        Vertex v1 = getVertexWithId(vid1);
        Vertex v2 = getVertexWithId(vid2);
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
    public boolean isReversible(FragmentSpace fragSpace)
    {
        for (Edge e : gEdges)
        {
            if (!e.getTrgAPClass().isCPMapCompatibleWith(e.getSrcAPClass(),
                    fragSpace))
            {
                return false;
            }
        }
        return true;
    }
    
//------------------------------------------------------------------------------

    /**
     * Searches for a graphs (<b><i>X</i></b>) embedded at any level in a graph 
     * (<b><i>Y</i></b>) by knowing<ul>
     * <li>the embedding path (<b><i>p</i></b>) of another graph <b><i>A</i></b> 
     * that is embedded in graph <b><i>B</i></b></li> 
     * <li>and that graph <b><i>Y</i></b> is a unmodified clone of graph 
     * <b><i>B</i></b>, which implies that <b><i>X</i></b> is a clone of 
     * <b><i>A</i></b>.
     * @param graphY where we want to find the analogous of <code>graphA</code>.
     * @param graphB the graph <code>graphB</code> where <code>path</code> 
     * points to <code>graphA</code>.
     * @param path the embedding path of <code>graphA</code> in 
     * <code>graphB</code>.
     * @return the graph embedded in <code>graphY</code>, i.e., 
     * <code>graphX</code>.
     */
    public static DGraph getEmbeddedGraphInClone(DGraph graphY, 
            DGraph graphB, List<Template> path) 
    {
        if (path.isEmpty())
            return graphY;
        Template currentLevelVertex = null;
        DGraph currentLevelGraphEmdInB = graphB;
        DGraph currentLevelGraphEmbInY = graphY;
        for (Template t : path)
        {
            currentLevelVertex = (Template) currentLevelGraphEmbInY
                    .getVertexAtPosition(currentLevelGraphEmdInB.indexOf(t));
            currentLevelGraphEmdInB = t.getInnerGraph();
            currentLevelGraphEmbInY = currentLevelVertex.getInnerGraph();
        }
        return currentLevelVertex.getInnerGraph();
    }
    
//------------------------------------------------------------------------------    

    /**
     * Searches for all {@link AttachmentPoint}s that represent the
     * interface between a subgraph, identified by the given list of vertexes,
     * and any other vertex, i.e., either belonging to the same graph but
     * not to the same <i>sub</i>graph, or belonging to an outer embedding level.
     * @param subGraphB list of vertexes belonging to the subgraph.
     * @return the list of attachment points at the interface of the subgraph.
     */
    public List<AttachmentPoint> getInterfaceAPs(
            List<Vertex> subGraphB)
    {
        List<AttachmentPoint> interfaceAPs = new ArrayList<AttachmentPoint>();
        for (Vertex v : subGraphB)
        {
            for (AttachmentPoint ap : v.getAttachmentPoints())
            {
                if (ap.isAvailableThroughout())
                    continue;
                if (ap.isAvailable())
                {
                    // This AP is used across the template boundary
                    interfaceAPs.add(ap);
                } else {
                    Vertex user = ap.getLinkedAP().getOwner();
                    if (!subGraphB.contains(user))
                    {
                        // AP used to make a connection to outside subgraph
                        interfaceAPs.add(ap);
                    }
                }    
            }      
        }
        return interfaceAPs;
    }

//------------------------------------------------------------------------------

    /**
     * Searches for all {@link AttachmentPoint}s that are owned by 
     * vertexes in a subgraph but either available or used by vertexes that do 
     * not belong to the subgraph.
     * @param subGraphB list of vertexes belonging to the subgraph.
     * @return the list of attachment points originating from the subgraph.
     */
    public List<AttachmentPoint> getSubgraphAPs(
            List<Vertex> subGraphB)
    {
        List<AttachmentPoint> aps = new ArrayList<AttachmentPoint>();
        for (Vertex v : subGraphB)
        {
            for (AttachmentPoint ap : v.getAttachmentPoints())
            {
                if (ap.isAvailable())
                {
                    aps.add(ap);
                    continue;
                } 
                Vertex user = ap.getLinkedAP().getOwner();
                if (!subGraphB.contains(user))
                {
                    // AP used to make a connection to outside subgraph
                    aps.add(ap);
                }    
            }      
        }
        return aps;
    }
    
//------------------------------------------------------------------------------    
    
}
