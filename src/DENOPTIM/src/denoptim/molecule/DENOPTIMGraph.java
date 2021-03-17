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

package denoptim.molecule;

import java.io.Reader;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;

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
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonParseException;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.DENOPTIMEdge.BondType;
import denoptim.molecule.DENOPTIMFragment.BBType;
import denoptim.rings.ClosableChain;
import denoptim.rings.CyclicGraphHandler;
import denoptim.rings.RingClosureParameters;
import denoptim.utils.DENOPTIMGraphEdit;
import denoptim.utils.DENOPTIMMoleculeUtils;
import denoptim.utils.DENOPTIMVertexQuery;
import denoptim.utils.GraphConversionTool;
import denoptim.utils.GraphUtils;
import denoptim.utils.ObjectPair;
import denoptim.utils.RotationalSpaceUtils;
import denoptim.utils.DENOPTIMgson;


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
     * Identifier for the format of string representations of a graph
     */
    public enum StringFormat {JSON, GraphENC}


//------------------------------------------------------------------------------

    public DENOPTIMGraph(ArrayList<DENOPTIMVertex> m_vertices,
                            ArrayList<DENOPTIMEdge> m_edges)
    {
        gVertices = m_vertices;
        for (DENOPTIMVertex v : gVertices)
            v.setGraphOwner(this);
        gEdges = m_edges;
        gRings = new ArrayList<>();
        closableChains = new ArrayList<>();
        symVertices = new ArrayList<>();
        localMsg = "";
    }

//------------------------------------------------------------------------------

    public DENOPTIMGraph(ArrayList<DENOPTIMVertex> m_vertices,
                            ArrayList<DENOPTIMEdge> m_edges,
                            ArrayList<DENOPTIMRing> m_rings)
    {
        this(m_vertices, m_edges);
        gRings = m_rings;
        closableChains = new ArrayList<>();
        symVertices = new ArrayList<>();
        localMsg = "";
    }

//------------------------------------------------------------------------------

    public DENOPTIMGraph(ArrayList<DENOPTIMVertex> m_vertices,
                            ArrayList<DENOPTIMEdge> m_edges,
                            ArrayList<DENOPTIMRing> m_rings,
                            ArrayList<SymmetricSet> m_symVerts)
    {
        this(m_vertices, m_edges, m_rings);
        closableChains = new ArrayList<>();
        symVertices = m_symVerts;
        localMsg = "";
    }

//------------------------------------------------------------------------------

    public DENOPTIMGraph(ArrayList<DENOPTIMVertex> m_vertices,
                            ArrayList<DENOPTIMEdge> m_edges,
                            ArrayList<DENOPTIMRing> m_rings,
                            ArrayList<ClosableChain> m_closableChains,
                            ArrayList<SymmetricSet> m_symVerts)
    {
        this(m_vertices, m_edges, m_rings, m_symVerts);
        closableChains = m_closableChains;
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

    public void setGraphId(int m_id)
    {
        graphId = m_id;
    }

//------------------------------------------------------------------------------

    public int getGraphId()
    {
        return graphId;
    }

//------------------------------------------------------------------------------

    public void setMsg(String m_msg)
    {
        localMsg = m_msg;
    }

//------------------------------------------------------------------------------

    public String getMsg()
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
     * Returns the number of symmetric sets of vertexes
     * @return the number of symmetric sets of vertexes
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

    public void setSymmetricVertexSets(ArrayList<SymmetricSet> m_mp)
    {
        symVertices.clear();
        symVertices.addAll(m_mp);
    }

//------------------------------------------------------------------------------

    public void addSymmetricSetOfVertices(SymmetricSet m_ss)
                                                        throws DENOPTIMException
    {
        for (SymmetricSet oldSS : symVertices)
        {
            for (Integer vid : m_ss.getList())
            {
                if (oldSS.contains(vid))
                {
                    throw new DENOPTIMException("Adding " + m_ss + " while "
                                                + "there is already " + oldSS
                                                + " that contains " + vid);
                }
            }
        }
        symVertices.add(m_ss);
    }

//------------------------------------------------------------------------------

    public void setVertexList(ArrayList<DENOPTIMVertex> m_vertices)
    {
        gVertices = m_vertices;
    }

//------------------------------------------------------------------------------

    public void setEdgeList(ArrayList<DENOPTIMEdge> m_edges)
    {
        gEdges = m_edges;
    }

//------------------------------------------------------------------------------

    public void setRings(ArrayList<DENOPTIMRing> m_rings)
    {
        gRings = m_rings;
    }

//------------------------------------------------------------------------------

    public void setCandidateClosableChains(ArrayList<ClosableChain> m_closableChains)
    {
        closableChains = m_closableChains;
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

    public ArrayList<DENOPTIMEdge> getEdgesWithSrc(DENOPTIMVertex v)
    {
    	ArrayList<DENOPTIMEdge> edges = new ArrayList<DENOPTIMEdge>();
    	for (DENOPTIMEdge e : this.getEdgeList())
    	{
    		if (e.getSrcVertex() == v.getVertexId())
    		{
    			edges.add(e);
    		}
    	}
    	return edges;
    }

//------------------------------------------------------------------------------

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

    public void addEdge(DENOPTIMEdge m_edge)
    {
        gEdges.add(m_edge);
    }

//------------------------------------------------------------------------------

    public void addRing(DENOPTIMRing m_ring)
    {
        gRings.add(m_ring);
    }

//------------------------------------------------------------------------------

    public void addVertex(DENOPTIMVertex m_vertex)
    {
        m_vertex.setGraphOwner(this);
        gVertices.add(m_vertex);
    }

//------------------------------------------------------------------------------

    /**
     * Remove a vertex from this graph. This method removes also edges and rings
     * that involve the removed vertex. Symmetric sets of vertexes are corrected
     * accordingly: they are removed if there is only one remaining vertex in
     * the set, of purged from the removed vertex.
     * @param m_vertex the vertex to remove.
     */
    public void removeVertex(DENOPTIMVertex m_vertex)
    {
        //TODO-V3: deal with templates. They do not appear in the edges as
        // target Vertices, so the edges to templates will not be removed
        // once we remove the template vertex.

        if (!gVertices.contains(m_vertex))
        {
        	return;
        }

        m_vertex.resetGraphOwner();
        int vid = m_vertex.getVertexId();

        // delete also any ring involving the removed vertex
        if (isVertexInRing(m_vertex))
        {
            ArrayList<DENOPTIMRing> rToRm = getRingsInvolvingVertex(m_vertex);
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
        gVertices.remove(m_vertex);
    }

//------------------------------------------------------------------------------

    public DENOPTIMVertex getVertexAtPosition(int m_pos)
    {
        return ((m_pos >= gVertices.size()) || m_pos < 0) ? null :
                gVertices.get(m_pos);
    }

//------------------------------------------------------------------------------

    /**
     * Check if the graph contains the specified vertex.
     * @param v the vertex.
     * @return <code>true</code> if the vertex belong tho this graph.
     */
    public boolean containsVertex(DENOPTIMVertex v)
    {
        return gVertices.contains(v);
    }

//------------------------------------------------------------------------------

    public DENOPTIMVertex getVertexWithId(int m_vertexId)
    {
        DENOPTIMVertex v = null;
        int idx = getIndexOfVertex(m_vertexId);
        if (idx != -1)
            v = gVertices.get(idx);
        return v;
    }

//------------------------------------------------------------------------------

    public int getIndexOfVertex(int m_vertexId)
    {
        int idx = -1;
        for (int i=0; i<gVertices.size(); i++)
        {
            DENOPTIMVertex v = gVertices.get(i);
            if (v.getVertexId() == m_vertexId)
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

            srcAP.updateFreeConnections(edge.getBondType().getValence());
            trgAP.updateFreeConnections(edge.getBondType().getValence());

            srcAP.setUser(null);
            trgAP.setUser(null);

            gEdges.remove(edge);
        }
    }

//------------------------------------------------------------------------------

    public void removeRing(DENOPTIMRing m_ring)
    {
        if (gRings.contains(m_ring))
        {
            gRings.remove(m_ring);
        }
    }

//------------------------------------------------------------------------------

    public DENOPTIMEdge getEdgeAtPosition(int m_pos)
    {
        if ((m_pos >= gEdges.size()) || m_pos < 0)
            return null;
        return gEdges.get(m_pos);
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
     */

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
     * @param vertex the vertex whose children are to be located
     * @param children list containing the references to all the children
     */
    public void getChildrenTree(DENOPTIMVertex vertex,
            ArrayList<DENOPTIMVertex> children) {
        ArrayList<DENOPTIMVertex> lst = getChildVertices(vertex);
        if (lst.isEmpty()) {
            return;
        }
        for (DENOPTIMVertex child : lst) {
            if (!children.contains(child)) {
                children.add(child);
                getChildrenTree(child, children);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * @param m_vid the vertex id for which the child vertices need to be found
     * @return Arraylist containing the vertex ids of the child vertices
     */

    public ArrayList<Integer> getChildVertices(int m_vid)
    {
        ArrayList<Integer> lst = new ArrayList<>();
        DENOPTIMVertex v = getVertexWithId(m_vid);
        for (DENOPTIMAttachmentPoint ap : v.getAttachmentPoints())
        {
            DENOPTIMEdge e = ap.getEdgeUser();
            if (e != null && e.getTrgVertex()!=m_vid)
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
        // When cloning, the VertedID remains the same so we'll have two
        // deep-copies of the same vertex having the same VertexID
        ArrayList<DENOPTIMVertex> cListVrtx = new ArrayList<>();
        Map<Integer,DENOPTIMVertex> vidsInClone =
                new HashMap<Integer,DENOPTIMVertex>();
        for (DENOPTIMVertex vOrig : gVertices)
        {
            DENOPTIMVertex vClone = vOrig.clone();
            vClone.setLevel(vOrig.getLevel());
            cListVrtx.add(vClone);
            vidsInClone.put(vClone.getVertexId(),vClone);}

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

        // Copy the list but using the references to the cloned vertexes
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
        clone.setMsg(localMsg);

        return clone;
    }

    //TODO-V3 delete. This was only meant to test the references to the edge with parent vertex

    public void checkHashed(String fileName)
    {

        System.out.println("Writing "+fileName+" for graph "+getGraphId());
        boolean nogood = false;
        String NL = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder();
        for (DENOPTIMVertex v : gVertices)
        {
            sb.append("Vertex "+v+NL);
            if (v.getEdgeToParent() == null)
                continue;

            int vh = v.getEdgeToParent().hashCode();
            sb.append("hash edge "+vh+NL);
            boolean found = false;
            for (DENOPTIMEdge e : gEdges)
            {

                if (e.hashCode() == vh)
                {
                    found = true;
                    break;
                }
            }

            if (!found)
            {
                System.out.println("M7: found mismatch");
                System.out.println("FromVertex "+v+" hash is: "+vh);
                for (DENOPTIMEdge ee : gEdges)
                    System.out.println("   -> "+ee+" "+ee.hashCode());
                nogood = true;
            }
        }

        for (DENOPTIMEdge ee : gEdges)
        {
            sb.append("all edge "+ee+" "+ee.hashCode()+NL);
        }

        try
        {
            DenoptimIO.writeData(fileName, sb.toString(), false);
        } catch (DENOPTIMException e)
        {
            e.printStackTrace();
        }
        if (nogood)
        {
            System.out.println("NO GOOD");
            System.exit(0);
        }
        System.out.println("Seems all good");
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
     * @param m_vid
     * @return the indeces of all edges whose source vertex is same as m_vid
     */

    public ArrayList<Integer> getIndexOfEdgesWithChild(int m_vid)
    {
        ArrayList<Integer> lstEdges = new ArrayList<>();
        for (int j=0; j<getEdgeCount(); j++)
        {
            DENOPTIMEdge edge = getEdgeAtPosition(j);

            if (edge.getSrcVertex() == m_vid)
            {
                lstEdges.add(j);
            }
        }
        return lstEdges;
    }

//------------------------------------------------------------------------------

    /**
     *
     * @param m_vid
     * @return the edge whose source vertex is same as m_vid
     */

    public ArrayList<DENOPTIMEdge> getEdgesWithChild(int m_vid)
    {
        ArrayList<DENOPTIMEdge> lstEdges = new ArrayList<>();
        for (int j=0; j<getEdgeCount(); j++)
        {
            DENOPTIMEdge edge = getEdgeAtPosition(j);

            if (edge.getSrcVertex() == m_vid)
            {
                lstEdges.add(edge);
            }
        }
        return lstEdges;
    }

//------------------------------------------------------------------------------

    public int getMaxLevel()
    {
        int mval = -1;
        for (DENOPTIMVertex vtx : gVertices) {
            mval = Math.max(mval, vtx.getLevel());
        }
        return mval;
    }

//------------------------------------------------------------------------------

    public int getMaxVertexId()
    {
        int mval = Integer.MIN_VALUE;
        for (DENOPTIMVertex v : gVertices) {
            mval = Math.max(mval, v.getVertexId());
        }
        return mval;
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

    //TODO-V3 is this really needed?
    public void cleanup()
    {
        if (gVertices != null)
        {
            if (!gVertices.isEmpty())
            {
                for (DENOPTIMVertex vtx : gVertices) {
                    if (vtx != null)
                        vtx.cleanup();
                }
            }
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
    }

//------------------------------------------------------------------------------

    /**
     * Compare this and another graph ignoring the vertex IDs. This method looks
     * into the structure of the graphs to determine if the two graphs have the
     * same spanning tree, same symmetric sets, and same rings set, despite
     * having different vertex IDs.
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
    		reason.append("Different number of vertexes ("+this.getVertexCount()+":"
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

    	//Pairwise correspondence of vertexes
    	Map<DENOPTIMVertex,DENOPTIMVertex> vertexMap =
    			new HashMap<DENOPTIMVertex,DENOPTIMVertex>();

    	vertexMap.put(this.getVertexAtPosition(0),other.getVertexAtPosition(0));

    	//WARNING: assuming that vertex 0 is the root and there are no disconnections
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
    			reason.append("Different number of symmetric sets on verted " + vIdT
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
    		for (DENOPTIMRing rO :
    			other.getRingsInvolvingVertex(vertexMap.get(vhT)))
    		{
				if (rT.getSize() != rO.getSize())
				{
					reason.append("Different ring size ("+rT.getSize()+":"
								+rO.getSize()+")");
					continue;
				}

				boolean either = false;
    			if (rO.getHeadVertex() == vertexMap.get(vhT)
    					&& rO.getTailVertex() == vertexMap.get(vtT))
    			{
    				either = true;
    				for (int i=1; i<rT.getSize(); i++)
    				{
    					if (vertexMap.get(rT.getVertexAtPosition(i))
    							!= rO.getVertexAtPosition(i))
    					{
    						reason.append("Rings differ (A) ("+rT+":"+rO+")");
    						return false;
    					}
    				}
    			}
    			else if (rO.getHeadVertex() == vertexMap.get(vtT)
    					&& rO.getTailVertex() == vertexMap.get(vhT))
    			{
    				either = true;
    				for (int i=1; i<rT.getSize(); i++)
    				{
    					int j = rO.getSize()-i-1;
    					if (vertexMap.get(rT.getVertexAtPosition(i))
    							!= rO.getVertexAtPosition(j))
    					{
    						reason.append("Rings differ (B) ("+rT+":"+rO+")");
    						return false;
    					}
    				}
    			}
    			if (!either)
    			{
    				reason.append("Rings differ (C) ("+rT+":"+rO+")");
    				return false;
    			}
    		}
    	}

    	return true;
    }

//------------------------------------------------------------------------------

    /**
     * Compares this and another graph by spanning vertexes staring from the
     * given vertex and following the direction of edges.
     * @param thisV
     * @param otherV
     * @param reason a string recording the reason for returning false
     * @return <code>true</code> if the graphs are same at this node
     * @throws DENOPTIMException
     */
    private static boolean compareGraphNodes(DENOPTIMVertex thisV,
    		DENOPTIMGraph thisG,
    		DENOPTIMVertex otherV,
    		DENOPTIMGraph otherG, Map<DENOPTIMVertex,DENOPTIMVertex> vertexMap,
    		StringBuilder reason) throws DENOPTIMException
    {
    	if (!thisV.sameAs(otherV, reason))
    	{
    		reason.append("Different vertex ("+thisV+":"+otherV+")");
    		return false;
    	}

    	ArrayList<DENOPTIMEdge> edgesFromThis = thisG.getEdgesWithSrc(thisV);
    	ArrayList<DENOPTIMEdge> edgesFromOther = otherG.getEdgesWithSrc(otherV);
    	if (edgesFromThis.size() != edgesFromOther.size())
    	{
    		reason.append("Different number of edged from vertex "+thisV+" ("
    					+edgesFromThis.size()+":"
    					+edgesFromOther.size()+")");
    		return false;
    	}

    	// pairwise correspondence between child vertexes
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
    			reason.append ("Edge not found in other("+et+")");
    			return false;
    		}

    		//Check: this should never be true
    		if (vertexMap.keySet().contains(
    				thisG.getVertexWithId(et.getTrgVertex())))
    		{
    			throw new DENOPTIMException("More than one attempt to set vertex map.");
    		}
    		vertexMap.put(thisG.getVertexWithId(et.getTrgVertex()),
    				otherG.getVertexWithId(eo.getTrgVertex()));

    		DENOPTIMVertex[] pair = new DENOPTIMVertex[]{
    				thisG.getVertexWithId(et.getTrgVertex()),
    				otherG.getVertexWithId(eo.getTrgVertex())};
    		pairs.add(pair);
    	}

    	//Recursion on child vertexes
    	for (DENOPTIMVertex[] pair : pairs)
    	{
    		DENOPTIMVertex v = pair[0];
    		DENOPTIMVertex o = pair[1];
    		boolean localRes = compareGraphNodes(v, thisG, o, otherG,vertexMap,
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
     * Returns the list of all attachment points contained in this graph
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
            if (((DENOPTIMFragment) vtx).getFragmentType() == BBType.CAP
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
     * Remove all capping groups on this graph
     */

    public void removeCappingGroups()
    {
        ArrayList<DENOPTIMVertex> lstVerts = getVertexList();
        ArrayList<Integer> rvids = new ArrayList<>();
        for (int i=0; i<lstVerts.size(); i++)
        {
            DENOPTIMVertex vtx = lstVerts.get(i);
            if (vtx instanceof DENOPTIMFragment == false)
            {
                continue;
            }
            // capping groups have fragment type 2
            if (((DENOPTIMFragment) vtx).getFragmentType() == BBType.CAP
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

    /**
     * Extracts the subgraph of a graph G starting from a given seed vertex of
     * G.
     * Only the seed vertex and all child vertexes (and further successors)
     * are considered part of
     * the subgraph, which includes also rings and symmetric sets. All
     * rings that include vertices not belonging to the subgraph are lost.
     * @param seed the vertex from which the extraction has to start
     * @return the subgraph
     */

    public DENOPTIMGraph extractSubgraph(DENOPTIMVertex seed)
            throws DENOPTIMException
    {
        DENOPTIMGraph subGraph = new DENOPTIMGraph();
        ArrayList<DENOPTIMVertex> subGrpVrtxs = new ArrayList<DENOPTIMVertex>();
        subGrpVrtxs.add(seed);
        getChildrenTree(seed, subGrpVrtxs);
        // Copy vertices to subgraph
        for (DENOPTIMVertex v : subGrpVrtxs)
        {
            subGraph.addVertex(v);
            // vertices are removed later (see below) otherwise also
            // rings and sym.sets are removed
        }

        // Remove the edge joining graph and subgraph
        removeEdgeWithParent(seed);

        // Identify edges belonging to subgraph
        Iterator<DENOPTIMEdge> edgesIterator = getEdgeList().iterator();
        while (edgesIterator.hasNext())
        {
            DENOPTIMEdge edge = edgesIterator.next();
            for (DENOPTIMVertex v : subGrpVrtxs)
            {
                if (edge.getSrcAP().getOwner() == v
                        || edge.getTrgAP().getOwner() == v)
                {
                    // Copy edge to subgraph...
                    subGraph.addEdge(edge);
                    // ...and remove it from this graph
                    edgesIterator.remove();
                    break;
                }
            }
        }

        // Identify and move rings
        Iterator<DENOPTIMRing> ringsIterator = getRings().iterator();
        while (ringsIterator.hasNext())
        {
            DENOPTIMRing ring = ringsIterator.next();
            boolean rSpanAnySGVrtx = false;
            boolean rWithinSubGrph = true;
            for (int i=0; i<ring.getSize(); i++)
            {
                int idVrtInRing = ring.getVertexAtPosition(i).getVertexId();
                if (subGrpVrtxs.contains(getVertexWithId(idVrtInRing)))
                {
                    rSpanAnySGVrtx = true;
                }
                else
                {
                    rWithinSubGrph = false;
                }
            }
            if (rSpanAnySGVrtx && rWithinSubGrph)
            {
                //copy ring to subgraph
                subGraph.addRing(ring);
                //remove ring from molGraph
                ringsIterator.remove();
            }
            //else if (!rSpanAnySGVrtx && rWithinSubGrph) impossible!
            else if (!rSpanAnySGVrtx && !rWithinSubGrph)
            {
                //ignore ring
                continue;
            }
            else if (rSpanAnySGVrtx && !rWithinSubGrph)
            {
                // only remove ring from molGraph
                ringsIterator.remove();
            }
        }

        // Identify and move symmetric sets
        Iterator<SymmetricSet> symSetsIterator = getSymSetsIterator();
        while (symSetsIterator.hasNext())
        {
            SymmetricSet ss = symSetsIterator.next();
            boolean ssSpanAnySGVrtx = false;
            boolean ssWithinSubGrph = true;
            SymmetricSet partOfSSInSubGraph = new SymmetricSet();
            for (Integer idVrtInSS : ss.getList())
            {
                if (subGrpVrtxs.contains(getVertexWithId(idVrtInSS)))
                {
                    partOfSSInSubGraph.add(idVrtInSS);
                    ssSpanAnySGVrtx = true;
                }
                else
                {
                    ssWithinSubGrph = false;
                }
            }
            if (ssSpanAnySGVrtx && ssWithinSubGrph)
            {
                //copy symm.set to subgraph
                subGraph.addSymmetricSetOfVertices(ss);
                //remove symm.set from molGraph
                symSetsIterator.remove();
            }
            //else if (!ssSpanAnySGVrtx && ssWithinSubGrph) impossible!
            else if (!ssSpanAnySGVrtx && !ssWithinSubGrph)
            {
                //ignore sym.set
                continue;
            }
            else if (ssSpanAnySGVrtx && !ssWithinSubGrph)
            {
                //copy only portion of sym.set
                subGraph.addSymmetricSetOfVertices(partOfSSInSubGraph);
                //remove sym.set from molGraph
                symSetsIterator.remove();
            }
        }

        // Remove vertices from molGraph
        for (DENOPTIMVertex v : subGrpVrtxs)
        {
            removeVertex(v);
        }

        return subGraph;
    }

//------------------------------------------------------------------------------

    /**
     * Removes the edge that links the given vertex to its parent
     * @param vertex the vertex whose edge to parent is to be removed.
     * @return the id of the parent vertex.
     */

    public int removeEdgeWithParent(DENOPTIMVertex vertex)
    {
        DENOPTIMEdge e = vertex.getEdgeToParent();
        if (e == null)
            return -1;
        int pvid = e.getSrcVertex();
        removeEdge(e);
        return pvid;
    }

//------------------------------------------------------------------------------

    /**
     * Deletes the branch, i.e., the specified vertex and its children.
     * @param vid the vertexID of the root of the branch. We'll remove also
     * this vertex.
     * @param symmetry use <code>true</code> to enforce deletion of all
     * symmetric vertexes.
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
     * No handling of symmetry.
     * @param vid the vertexID of the root of the branch. We'll remove also
     * this vertex.
     * @return <code>true</code> if operation is successful
     * @throws DENOPTIMException
     */

    public boolean removeBranchStartingAt(DENOPTIMVertex v)
            throws DENOPTIMException
    {
        // first delete the edge with the parent vertex
        int pvid = removeEdgeWithParent(v);
        if (pvid == -1)
        {
            String msg = "Program Bug detected trying to delete vertex "
                    + v + " from graph '" + this + "'. "
                    + "Unable to locate parent edge.";
            throw new DENOPTIMException(msg);
        }

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
                    e.setSrcVertex(nvid);
                }
                if (e.getTrgVertex() == vid) {
                    e.setTrgVertex(nvid);
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
     * Reassign vertex IDs to a graph.
     * Before any operation is performed on the graph, its vertices should be
     * renumbered to differentiate them from their clones.
     */

    public void renumberGraphVertices()
    {
        renumberVerticesGetMap();
    }

//------------------------------------------------------------------------------

    /**
     * Reassign vertex IDs to a graph.
     * Before any operation is performed on the graph, its vertices should be
     * renumbered so as to differentiate them from their clones
     * @return map with old IDs as key and new IDs as values
     */

    public Map<Integer,Integer> renumberVerticesGetMap() {
        Map<Integer, Integer> nmap = new HashMap<>();

        // for the vertices in the graph, get new vertex ids
        for (int i=0; i<getVertexCount(); i++)
        {
            int vid = getVertexList().get(i).getVertexId();
            int nvid = GraphUtils.getUniqueVertexIndex();

            nmap.put(vid, nvid);

            getVertexList().get(i).setVertexId(nvid);
        }

        // Update the sets of symmetric vertex IDs
        Iterator<SymmetricSet> iter = getSymSetsIterator();
        while (iter.hasNext())
        {
            SymmetricSet ss = iter.next();
            for (int i=0; i<ss.getList().size(); i++)
            {
                ss.getList().set(i,nmap.get(ss.getList().get(i)));
            }
        }

        return nmap;
    }

//------------------------------------------------------------------------------

    /**
     * Update the level at which the new vertices have been added. This
     * is generally applicable for a crossover or for a substitution operation
     * @param lvl
     */

    public void updateLevels(int lvl)
    {
        List<DENOPTIMVertex> lstVert = getVertexList();
        int levRoot = lstVert.get(0).getLevel();
        int correction = lvl - levRoot;
        for (DENOPTIMVertex denoptimVertex : lstVert) {
            denoptimVertex.setLevel(denoptimVertex.getLevel() + correction);
        }
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
        IAtomContainer mol = GraphConversionTool.convertGraphToMolecule(this,
                true);
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
        ObjectPair pr = DENOPTIMMoleculeUtils.getInchiForMolecule(mol);
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
        IAtomContainer mol = GraphConversionTool.convertGraphToMolecule(this,
                false);

        // Set rotatable property as property of IBond
        RotationalSpaceUtils.defineRotatableBonds(mol,
                        FragmentSpaceParameters.getRotSpaceDefFile(),
                        true, true);

        // get the set of possible RCA combinations = ring closures
        CyclicGraphHandler cgh = new CyclicGraphHandler(
                FragmentSpace.getScaffoldLibrary(),
                FragmentSpace.getFragmentLibrary(),
                FragmentSpace.getCappingLibrary(),
                FragmentSpace.getRCCompatibilityMatrix());
        ArrayList<Set<DENOPTIMRing>> allCombsOfRings =
                cgh.getPossibleCombinationOfRings(mol, this);

        // Keep closable chains that are relevant for chelate formation
        if (RingClosureParameters.buildChelatesMode())
        {
            ArrayList<Set<DENOPTIMRing>> toRemove = new ArrayList<>();
            for (Set<DENOPTIMRing> setRings : allCombsOfRings)
            {
                if (!cgh.checkChelatesGraph(this,setRings))
                {
                    toRemove.add(setRings);
                }
            }
            allCombsOfRings.removeAll(toRemove);
        }

        // prepare output graphs
        for (Set<DENOPTIMRing> ringSet : allCombsOfRings)
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
                                   BondType bndType, boolean onAllSymmAPs
    ) throws DENOPTIMException
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
     * Append a subgraph (I) to this graph (R) specifying
     * which vertex and attachment point to use for the connection.
     * Does not project on symmetrically related vertexes or
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
        // The clones have the same vertex IDs before renumbering vertexes
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

            // also need to tmp store pointers to symmetric vertexes
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
            DENOPTIMVertexQuery query,
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
     * @param dnQuery the query
     * @param verbosity the verbosity level
     * @return the list of matched vertices
     */

    public ArrayList<DENOPTIMVertex> findVertices(
            DENOPTIMVertexQuery dnQuery,
            int verbosity)
    {
        DENOPTIMVertex vQuery = dnQuery.getVrtxQuery();
        DENOPTIMEdge eInQuery = dnQuery.getInEdgeQuery();
        DENOPTIMEdge eOutQuery = dnQuery.getOutEdgeQuery();

        ArrayList<DENOPTIMVertex> matches = new ArrayList<>(getVertexList());

        if (verbosity > 1)
        {
            System.out.println("Candidates: " + matches);
        }

        //Check condition vertex ID
        if (vQuery.getVertexId() > -1) //-1 would be the wildcard
        {
            if (verbosity > 2)
            {
                System.out.println("Keeping vertex ID: "+vQuery.getVertexId());
            }
            ArrayList<DENOPTIMVertex> newLst = new ArrayList<>();
            for (DENOPTIMVertex v : matches)
            {
                if (v.getVertexId() == vQuery.getVertexId())
                {
                    newLst.add(v);
                }
            }
            matches = newLst;
        }

        if (verbosity > 1)
        {
            System.out.println("After ID-based rule: " + matches);
        }

        //Check condition fragment ID
        if (vQuery instanceof DENOPTIMFragment)
        {
            int queryMolID = ((DENOPTIMFragment) vQuery).getMolId();
            if (queryMolID > -1) //-1 would be the wildcard
            {
                ArrayList<DENOPTIMVertex> newLst = new ArrayList<>();
                if (verbosity > 2)
                {
                    System.out.println("Keeping MolID: "+queryMolID);
                }
                for (DENOPTIMVertex v : matches)
                {
                    if (!(v instanceof DENOPTIMFragment))
                    {
                        continue;
                    }
                    if (((DENOPTIMFragment) v).getMolId() == queryMolID)
                    {
                        newLst.add(v);
                    }
                }
                matches = newLst;
            }

            if (verbosity > 2)
            {
                System.out.println("After MolID-based rule: " + matches);
            }

            //Check condition fragment type
            BBType queryFrgTyp = ((DENOPTIMFragment) vQuery).getFragmentType();
            if (queryFrgTyp != BBType.UNDEFINED)
            {
                if (verbosity > 2)
                {
                    System.out.println("Keeping FragType: "+queryFrgTyp);
                }
                ArrayList<DENOPTIMVertex> newLst = new ArrayList<>();
                for (DENOPTIMVertex v : matches)
                {
                    if (!(v instanceof DENOPTIMFragment))
                    {
                        continue;
                    }
                    if (((DENOPTIMFragment) v).getFragmentType() == queryFrgTyp)
                    {
                        newLst.add(v);
                    }
                }
                matches = newLst;
            }

            if (verbosity > 2)
            {
                System.out.println("After Frag-type rule: " + matches);
            }
        }

        //Check condition: level of vertex
        if (vQuery.getLevel() > -2) //-2 would be the wildcard
        {
            ArrayList<DENOPTIMVertex> newLst = new ArrayList<DENOPTIMVertex>();
            for (DENOPTIMVertex v : matches)
            {
                if (v.getLevel() == vQuery.getLevel())
                {
                    newLst.add(v);
                }
            }
            matches = newLst;
        }

        if (verbosity > 1)
        {
            System.out.println("After Vertex-based rules: " + matches);
        }

        //Incoming connections (candidate vertex is the target)
        if (eInQuery != null)
        {
            //Check condition target AP
            if (eInQuery.getTrgAPID() > -1)
            {
                ArrayList<DENOPTIMVertex> newLst =
                        new ArrayList<DENOPTIMVertex>();
                for (DENOPTIMVertex v : matches)
                {
                    if (getEdgeWithParent(v.getVertexId()) == null)
                    {
                        continue;
                    }
                    DENOPTIMEdge e = getEdgeWithParent(v.getVertexId());
                    if (e!=null && e.getTrgAPID() == eInQuery.getTrgAPID())
                    {
                        newLst.add(v);
                    }
                }
                matches = newLst;
            }

            if (verbosity > 2)
            {
                System.out.println("After OutEdge-srcAP rule: "+matches);
            }

            //Check condition bond type
            if (eInQuery.getBondType() != BondType.ANY)
            {
                ArrayList<DENOPTIMVertex> newLst =
                        new ArrayList<>();
                for (DENOPTIMVertex v : matches)
                {
                    if (getEdgeWithParent(v.getVertexId()) == null)
                    {
                        continue;
                    }
                    DENOPTIMEdge e = getEdgeWithParent(v.getVertexId());
                    if (e!=null && e.getBondType() == eInQuery.getBondType())
                    {
                        newLst.add(v);
                    }
                }
                matches = newLst;
            }

            if (verbosity > 2)
            {
                System.out.println("After InEdge-bond rule: "+matches);
            }

            //Check condition AP class
            if (!eInQuery.getTrgAPClass().equals(new APClass()))
            {
                ArrayList<DENOPTIMVertex> newLst =
                        new ArrayList<>();
                for (DENOPTIMVertex v : matches)
                {
                    if (getEdgeWithParent(v.getVertexId()) == null)
                    {
                        continue;
                    }
                    DENOPTIMEdge e = getEdgeWithParent(v.getVertexId());
                    if (e!=null && e.getTrgAPClass().equals(
                            eInQuery.getTrgAPClass()))
                    {
                        newLst.add(v);
                    }
                }
                matches = newLst;
            }
            if (!eInQuery.getSrcAPClass().equals(new APClass()))
            {
                ArrayList<DENOPTIMVertex> newLst =
                        new ArrayList<>();
                for (DENOPTIMVertex v : matches)
                {
                    if (getEdgeWithParent(v.getVertexId()) == null)
                    {
                        continue;
                    }
                    DENOPTIMEdge e = getEdgeWithParent(v.getVertexId());
                    if (e!=null && e.getSrcAPClass().equals(
                            eInQuery.getSrcAPClass()))
                    {
                        newLst.add(v);
                    }
                }
                matches = newLst;
            }
        }

        if (verbosity > 1)
        {
            System.out.println("After InEdge-based rules: " + matches);
        }

        //Out-coming connections (candidate vertex is the source)
        if (eOutQuery != null)
        {
            //Check condition target AP
            if (eOutQuery.getSrcAPID() > -1)
            {
                ArrayList<DENOPTIMVertex> newLst =
                        new ArrayList<>();
                for (DENOPTIMVertex v : matches)
                {
                    for (DENOPTIMEdge e : getEdgesWithChild(
                            v.getVertexId()))
                    {
                        if (e.getSrcAPID() == eOutQuery.getSrcAPID())
                        {
                            newLst.add(v);
                            break;
                        }
                    }
                }
                matches = newLst;
            }

            if (verbosity > 2)
            {
                System.out.println("After OutEdge-srcAP rule: "+matches);
            }

            //Check condition bond type
            if (eOutQuery.getBondType() != BondType.ANY)
            {
                ArrayList<DENOPTIMVertex> newLst =
                        new ArrayList<>();
                for (DENOPTIMVertex v : matches)
                {
                    for (DENOPTIMEdge e : getEdgesWithChild(
                            v.getVertexId()))
                    {
                        if (e.getBondType() == eOutQuery.getBondType())
                        {
                            newLst.add(v);
                            break;
                        }
                    }
                }
                matches = newLst;
            }

            if (verbosity > 2)
            {
                System.out.println("After OutEdge-bond rule: "+matches);
            }

            //Check condition AP class
            if (!eOutQuery.getTrgAPClass().equals(new APClass()))
            {
                ArrayList<DENOPTIMVertex> newLst =
                        new ArrayList<>();
                for (DENOPTIMVertex v : matches)
                {
                    for (DENOPTIMEdge e : getEdgesWithChild(
                            v.getVertexId()))
                    {
                        if (e.getTrgAPClass().equals(
                                eOutQuery.getTrgAPClass()))
                        {
                            newLst.add(v);
                            break;
                        }
                    }
                }
                matches = newLst;
            }
            if (!eOutQuery.getSrcAPClass().equals(new APClass()))
            {
                ArrayList<DENOPTIMVertex> newLst =
                        new ArrayList<>();
                for (DENOPTIMVertex v : matches)
                {
                    for (DENOPTIMEdge e : getEdgesWithChild(
                            v.getVertexId()))
                    {
                        if (e.getSrcAPClass().equals(
                                eOutQuery.getSrcAPClass()))
                        {
                            newLst.add(v);
                            break;
                        }
                    }
                }
                matches = newLst;
            }
        }

        if (verbosity > 1)
        {
            System.out.println("After OutEdge-based rule: " + matches);
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
     * Remove all but one of the symmetry-related partners in a list
     * @param list vertices to be purged
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
     * Remove all but one of the symmetry-related partners in a list.
     * @param list the list of vertex IDs to be purged
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
     * Edit a graph according to a given list of edit tasks.
     * @param edits the list of edit tasks
     * @param symmetry if <code>true</code> the symmetry is enforced
     * @param verbosity the verbosity level
     * @return the modified graph
     */

    public DENOPTIMGraph editGraph(ArrayList<DENOPTIMGraphEdit> edits,
                                   boolean symmetry, int verbosity)
                                           throws DENOPTIMException
    {

        //Make sure there is no clash with vertex IDs
        int maxId = getMaxVertexId();
        if (GraphUtils.vertexCounter.get() <= maxId)
        {
            try
            {
                GraphUtils.resetUniqueVertexCounter(maxId+1);
            }
            catch (Throwable t)
            {
                maxId = GraphUtils.vertexCounter.getAndIncrement();
            }
        }

        DENOPTIMGraph modGraph = this.clone();

        for (DENOPTIMGraphEdit edit : edits)
        {
            String task = edit.getType();

            if (verbosity > 1)
            {
                System.out.println(" ");
                System.out.println("Graph edit task: "+task);
                System.out.println("Graph to be edited: "+modGraph);
            }

            switch (task.toUpperCase())
            {
                case (DENOPTIMGraphEdit.REPLACECHILD):
                {
                    DENOPTIMEdge e =  edit.getFocusEdge();
                    DENOPTIMGraph inGraph = edit.getIncomingGraph();
                    DENOPTIMVertexQuery query = new DENOPTIMVertexQuery(
                            edit.getFocusVertex(),
                            null,
                            edit.getFocusEdge());
                    ArrayList<Integer> matches = modGraph.findVerticesIds(
                            query,verbosity);
                    for (int pid : matches)
                    {
                        int wantedApID = edit.getFocusEdge().getSrcAPID();
                        APClass wantedApCl =
                                edit.getFocusEdge().getSrcAPClass();
                        ArrayList<Integer> symmUnqChilds =
                                modGraph.getChildVertices(pid);
                        if (symmetry)
                        {
                            modGraph.removeSymmetryRedundantIds(symmUnqChilds);
                        }
                        for (int cid : symmUnqChilds)
                        {
                            DENOPTIMVertex cv = getVertexWithId(cid);
                            // Apply the query on the src AP on the focus vertex
                            // -1 id the wildcard
                            int srcApId = modGraph.getEdgeWithParent(
                                    cid).getSrcAPID();
                            if (wantedApID>-1 && wantedApID != srcApId)
                            {
                                continue;
                            }
                            // Apply the query on the AP Class
                            APClass srcApCl = modGraph.getEdgeWithParent(
                                    cid).getSrcAPClass();
                            if (!wantedApCl.equals(new APClass())
                                    && !wantedApCl.equals(srcApCl))
                            {
                                continue;
                            }
                            modGraph.removeBranchStartingAt(cv,symmetry);
                            int wantedTrgApId = e.getTrgAPID();
                            int trgApLstSize = inGraph.getVertexWithId(
                                    e.getTrgVertex()).getNumberOfAP();
                            if (wantedTrgApId >= trgApLstSize)
                            {
                                String msg = "Request to use AP number "
                                        + wantedTrgApId + " but only " + trgApLstSize
                                        + " are found in the designated vertex.";
                                throw new DENOPTIMException(msg);
                            }
                            modGraph.appendGraphOnGraph(
                                    modGraph.getVertexWithId(pid),
                                    srcApId,
                                    inGraph,
                                    inGraph.getVertexWithId(e.getTrgVertex()),
                                    wantedTrgApId,
                                    e.getBondType(),
                                    new HashMap<>(),
                                    symmetry
                            );
                        }
                    }
                    break;
                }
                case (DENOPTIMGraphEdit.DELETEVERTEX):
                {
                    DENOPTIMVertexQuery query = new DENOPTIMVertexQuery(
                            edit.getFocusVertex(), edit.getFocusEdge()
                    );
                    ArrayList<Integer> matches = modGraph.findVerticesIds(
                            query, verbosity);
                    for (int vid : matches)
                    {
                        modGraph.removeBranchStartingAt(getVertexWithId(vid),
                                symmetry);
                    }
                    break;
                }
            }
        }
        return modGraph;
    }

//------------------------------------------------------------------------------

    public Set<DENOPTIMVertex> getMutableSites()
    {
        Set<DENOPTIMVertex> mutableSites = new HashSet<DENOPTIMVertex>();
        for (DENOPTIMVertex v : gVertices)
        {
            mutableSites.addAll(v.getMutationSites());
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

        // Check for uniqueness of vertexIDs and APIDs within the
        // graph (ignore nested graphs).
        Set<Integer> unqVrtxIDs = new HashSet<Integer>();
        Set<Integer> unqApIDs = new HashSet<Integer>();
        for (DENOPTIMVertex v : gVertices)
        {
            if (!unqVrtxIDs.add(v.getVertexId()))
            {
                throw new DENOPTIMException("Duplicate vertex ID '"
                        + v.getVertexId()
                        + "'. Cannot generate JSON string for graph: " + this);
            }
            for (DENOPTIMAttachmentPoint ap : v.getAttachmentPoints())
            {
                if (!unqApIDs.add(ap.getID()))
                {
                    throw new DENOPTIMException("Duplicate attachment point ID "
                            + "'" + ap.getID() + "'. "
                            + "Cannot generate JSON string for graph: " + this);
                }           }
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
     * We expect unique IDs for vertexes and attachment points.
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
            // Eventually, also the sym sets will become references...
            partialJsonObj.add("symVertices", jsonObject.get("symVertices"));

            Gson gson = new GsonBuilder()
                .setExclusionStrategies(new DENOPTIMgson.DENOPTIMExclusionStrategyNoAPMap())
                .registerTypeAdapter(DENOPTIMVertex.class,
                      new DENOPTIMVertex.DENOPTIMVertexDeserializer())
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
                    // Reset connection count
                    ap.setFreeConnections(ap.getTotalConnections());
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
                graph.addRing(ring);
            }

            return graph;
        }
    }

// 
// List<DENOPTIMGraph> list_of_graphs;
// Gson writer = DENOPTIMgson.getWriter();
// String jsonOutput = writer.toJson(list_of_graphs);
// 


}
