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

import java.util.*;

import java.io.Serializable;
import java.util.logging.Level;

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
import denoptim.utils.*;

import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;


/**
 * Container for the list of vertices and the edges that connect them
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class DENOPTIMGraph implements Serializable, Cloneable
{
    ArrayList<DENOPTIMVertex> gVertices;
    ArrayList<DENOPTIMEdge> gEdges;
    ArrayList<DENOPTIMRing> gRings;
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
    
    String localMsg;


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

    public boolean hasSymmetryInvolvingVertex(int vid)
    {
        boolean res = false;
        for (SymmetricSet ss : symVertices)
        {
            if (ss.contains(vid)) 
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
        ArrayList<DENOPTIMEdge> lstEdges = this.getEdgeList();
        ArrayList<DENOPTIMEdge> eToDel = new ArrayList<>();
        for (int i=0; i<lstEdges.size(); i++)
        {
            DENOPTIMEdge edge = lstEdges.get(i);
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
        for (int i=0; i<eToDel.size(); i++)
        {        	
            //TODO-V3: these things should be done in a graph.removeEdge method
            DENOPTIMEdge edge = eToDel.get(i);
            
            // update the connections of the parent(src) vertex
            int iA = edge.getSrcAPID();
            int srcvid = edge.getSrcVertex();
            DENOPTIMVertex src = this.getVertexWithId(srcvid);
            src.updateAttachmentPoint(iA, edge.getBondType().getValence());
            
            // update the connections of the child(trg) vertex
            int iB = edge.getTrgAPID();
            int trgvid = edge.getTrgVertex();
            if (this.containsVertexId(trgvid))
            {
                DENOPTIMVertex trg = this.getVertexWithId(trgvid);
                trg.updateAttachmentPoint(iB, edge.getBondType().getValence());
            }
            this.removeEdge(edge);
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
    
    /**
     * Check if any of the vertexes in this graph has the given ID
     * @param vId the vertex ID to search for
     * @return <code>true</code> if the vertex ID is found in this graph
     */
    public boolean containsVertexId(int vId)
    {
    	boolean res = false;
    	for (DENOPTIMVertex v : gVertices)
    	{
    		if (vId == v.getVertexId())
    		{
    			res = true;
    			break;
    		}
    	}
    	return res;
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

    public DENOPTIMVertex getVertexWithId(int m_vertexId, int m_lvl)
    {
        DENOPTIMVertex v = null;
        int idx = -1;
        for (int i=0; i<gVertices.size(); i++)
        {
            v = gVertices.get(i);
            if (v.getVertexId() == m_vertexId && v.getLevel() == m_lvl)
            {
                idx = i;
                break;
            }
        }

        if (idx != -1)
        {
            v = gVertices.get(idx);
        }
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

    public void removeVertex(int m_vertexId)
    {
        DENOPTIMVertex v = getVertexWithId(m_vertexId);

        if (v != null)
        {
            removeVertex(v);
        }
    }


//------------------------------------------------------------------------------

    public void removeEdge(DENOPTIMEdge m_edge)
    {
        if (gEdges.contains(m_edge))
        {
            gEdges.remove(m_edge);
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
     * @param m_vid the vertex id for which the adjacent vertices need to be found
     * @return Arraylist containing the vertex ids of the adjacent vertices
     */

    public ArrayList<Integer> getAdjacentVertices(int m_vid)
    {
        ArrayList<Integer> lst = new ArrayList<>();
        for (int i=0; i<gEdges.size(); i++)
        {
            DENOPTIMEdge edge = gEdges.get(i);
            if (edge.getTrgVertex() == m_vid)
            {
                lst.add(edge.getSrcVertex());
            }
            if (edge.getSrcVertex() == m_vid)
            {
                lst.add(edge.getTrgVertex());
            }
        }
        return lst;
    }

//------------------------------------------------------------------------------

    /**
     * @param v the <code>DENOPTIMVertex</code> for which the incident edges
     * are to be found
     * @return the list of incident <code>DENOPTIMEdge</code>s
     */

    public ArrayList<DENOPTIMEdge> getIncidentEdges(DENOPTIMVertex v)
    {
        return getIncidentEdges(v.getVertexId());
    }

//------------------------------------------------------------------------------

    /**
     * @param vid the index of the <code>DENOPTIMVertex</code> for which the 
     * incident edges are to be found
     * @return the list of incident <code>DENOPTIMEdge</code>s
     */

    public ArrayList<DENOPTIMEdge> getIncidentEdges(int vid)
    {         
        ArrayList<DENOPTIMEdge> lst = new ArrayList<DENOPTIMEdge>();
        for (int i=0; i<gEdges.size(); i++)
        {
            DENOPTIMEdge edge = gEdges.get(i);
            if (edge.getTrgVertex() == vid ||
                edge.getSrcVertex() == vid)
            {
                lst.add(edge);
            }
        }
        return lst;
    }
    
//------------------------------------------------------------------------------    
    
    /**
     * Obtain the vertex connected at the AP
     *
     * @param vertex
     * @param dapidx
     * @param rcn
     * @return the vertex connected at the AP
     */
    
    //TODO-M6 check: is this ever used?
    public DENOPTIMVertex getBondingVertex(DENOPTIMVertex vertex, int dapidx, 
            APClass rcn)
    {
        int n = getEdgeCount();
        for (int i = 0; i < n; i++)
        {
            DENOPTIMEdge edge = getEdgeList().get(i);

            // get the vertex ids
            int v1_id = edge.getSrcVertex();
            int v2_id = edge.getTrgVertex();
            
            int dap_idx_v1 = edge.getSrcAPID();
            
            if (rcn != null)
            {
                APClass rcstr = edge.getSrcAPClass();
                if (vertex.getVertexId() == v1_id && dap_idx_v1 == dapidx 
                        && rcstr.equals(rcn))
                {
                    return getVertexWithId(v2_id);
                }
            }
            else
            {
                if (vertex.getVertexId() == v1_id && dap_idx_v1 == dapidx)
                    return getVertexWithId(v2_id);
            }
        }

        return null;
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
        ArrayList<DENOPTIMVertex> lst = new ArrayList<>();
        
        for (int vid : getChildVertices(vertex.getVertexId()))
        {
            lst.add(this.getVertexWithId(vid));
        }
        return lst;
    }
    
//------------------------------------------------------------------------------  
    
    /**
     * @param m_vid the vertex id for which the child vertices need to be found
     * @return Arraylist containing the vertex ids of the child vertices
     */

    public ArrayList<Integer> getChildVertices(int m_vid)
    {
        ArrayList<Integer> lst = new ArrayList<>();
        for (int i=0; i<gEdges.size(); i++)
        {
            DENOPTIMEdge edge = gEdges.get(i);
            if (edge.getSrcVertex() == m_vid)
            {
                lst.add(edge.getTrgVertex());
            }
        }
        return lst;
    }

//------------------------------------------------------------------------------

    /**
     * Returns almost " deep-copy" of this graph. Only the APCLass members of 
     * member of this class should remain references to the original APClasses.
     * The vertex IDs are not changed, so you might want to renumber the graph.
     */
    @Override
    public DENOPTIMGraph clone()
    {
        
        //TODO-V3: should this be replaced by DenoptimIO.deepCopy? NO!
        
        /*
        return (DENOPTIMGraph) DenoptimIO.deepCopy(this);
        */
        
        // When cloning, the VertedID remains the same so we'll have two 
        // deep-copies of the same vertex having the same VertexID
        ArrayList<DENOPTIMVertex> cListVrtx = new ArrayList<>();
        for (DENOPTIMVertex vOrig : gVertices)
        {
            DENOPTIMVertex vClone = vOrig.clone();

            for (int i=0; i<vOrig.getAttachmentPoints().size(); i++)
            {
                DENOPTIMAttachmentPoint origAp = 
                        vOrig.getAttachmentPoints().get(i);
                DENOPTIMAttachmentPoint cloneAp = 
                        vClone.getAttachmentPoints().get(i);
                cloneAp.setFreeConnections(origAp.getFreeConnections());
            }
            vClone.setLevel(vOrig.getLevel());
            cListVrtx.add(vClone);
        }
        
        // Only primitives inside the edges, so it's just fine
        ArrayList<DENOPTIMEdge> cListEdges = new ArrayList<>();
        for (DENOPTIMEdge e : gEdges)
        {
            cListEdges.add(e.clone());
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

//------------------------------------------------------------------------------

    /**
     *
     * @param m_vid
     * @return the index of edge whose target vertex is same as m_vid
     */

    public int getIndexOfEdgeWithParent(int m_vid)
    {
        for (int j=0; j<getEdgeCount(); j++)
        {
            DENOPTIMEdge edge = getEdgeAtPosition(j);

            if (edge.getTrgVertex() == m_vid)
            {
                return j;
            }
        }
        return -1;
    }

//------------------------------------------------------------------------------

    /**
     *
     * @param m_vid
     * @return the edge whose target vertex has ID same as m_vid, or null
     */

    public DENOPTIMEdge getEdgeWithParent(int m_vid)
    {
        for (int j=0; j<getEdgeCount(); j++)
        {
            DENOPTIMEdge edge = getEdgeAtPosition(j);

            if (edge.getTrgVertex() == m_vid)
            {
                return edge;
            }
        }
        return null;
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

    public DENOPTIMVertex getParent(int m_vid)
    {
        int idx = getIndexOfEdgeWithParent(m_vid);
        if (idx != -1)
        {
            DENOPTIMEdge edge = getEdgeAtPosition(idx);
            int src = edge.getSrcVertex();
            return getVertexWithId(src);
        }
        return null;
    }

//------------------------------------------------------------------------------

    public int getParentAPIndex(int m_vid)
    {
        int idx = getIndexOfEdgeWithParent(m_vid);
        if (idx != -1)
        {
            DENOPTIMEdge edge = getEdgeAtPosition(idx);
            return edge.getSrcAPID();
        }
        return -1;
    }  
    
//------------------------------------------------------------------------------    
    
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
     * Checks the graph for unused APs that need to be capped
     * @return <code>true</code> if the graph has at least one AP that needs
     * to be capped
     */

    public boolean graphNeedsCappingGroups()
    {
        for (DENOPTIMVertex v : getVertexList()) {
            for (DENOPTIMAttachmentPoint ap : v.getAttachmentPoints()) {
                if (ap.isAvailable()
                        && FragmentSpace.getCappingClass(ap.getAPClass()) !=null
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
     * Gets all the children of the current vertex
     * @param vid the vertex whose children are to be located
     * @param children list containing the vertex ids of the children
     */
    public void getChildren(int vid, ArrayList<Integer> children) {
        // get the child vertices of vid
        ArrayList<Integer> lst = getChildVertices(vid);
        if (lst.isEmpty()) {
            return;
        }
        for (Integer childId : lst) {
            if (!children.contains(childId)) {
                children.add(childId);
                getChildren(childId, children);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Extracts the subgraph of a graph G starting from a given vertex in G.
     * Only the given vertex V and all child vertexes are considered part of
     * the subgraph, which encodes also rings and symmetric sets. Potential
     * rings that include vertices not belonging to the subgraph are lost.
     * @param vid the vertex id from which the extraction has to start
     * @return the subgraph
     */

    public DENOPTIMGraph extractSubgraph(int vid)
            throws DENOPTIMException
    {
        DENOPTIMGraph subGraph = new DENOPTIMGraph();

        // Identify vertices belonging to subgraph (i.e., vid + children)
        ArrayList<Integer> subGrpVrtIDs = new ArrayList<>();
        subGrpVrtIDs.add(vid);
        getChildren(vid, subGrpVrtIDs);
        // Copy vertices to subgraph
        for (Integer i : subGrpVrtIDs)
        {
            subGraph.addVertex(getVertexWithId(i));
            // vertices are removed later (see below) otherwise also
            // rings and sym.sets are removed
        }

        // Remove the edge joining graph and gubgraph
        removeEdgeWithParent(vid);

        // Identify edges belonging to subgraph
        Iterator<DENOPTIMEdge> iter1 = getEdgeList().iterator();
        while (iter1.hasNext())
        {
            DENOPTIMEdge edge = iter1.next();
            for (Integer k : subGrpVrtIDs)
            {
                if (edge.getSrcVertex() == k || edge.getTrgVertex() == k)
                {
                    // Copy edge to subgraph...
                    subGraph.addEdge(edge);
                    // ...and remove it from molGraph
                    iter1.remove();
                    break;
                }
            }
        }

        // Identify and move rings
        Iterator<DENOPTIMRing> iter2 = getRings().iterator();
        while (iter2.hasNext())
        {
            DENOPTIMRing ring = iter2.next();
            boolean rSpanAnySGVrtx = false;
            boolean rWithinSubGrph = true;
            for (int i=0; i<ring.getSize(); i++)
            {
                int idVrtInRing = ring.getVertexAtPosition(i).getVertexId();
                if (subGrpVrtIDs.contains(idVrtInRing))
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
                iter2.remove();
            }
            //else if (!rSpanAnySGVrtx && rWithinSubGrph) imposible!
            else if (!rSpanAnySGVrtx && !rWithinSubGrph)
            {
                //ignore ring
                continue;
            }
            else if (rSpanAnySGVrtx && !rWithinSubGrph)
            {
                // only remove ring from molGraph
                iter2.remove();
            }
        }

        // Identify and move symmetiric sets
        Iterator<SymmetricSet> iter3 = getSymSetsIterator();
        while (iter3.hasNext())
        {
            SymmetricSet ss = iter3.next();
            boolean ssSpanAnySGVrtx = false;
            boolean ssWithinSubGrph = true;
            SymmetricSet partOfSSInSubGraph = new SymmetricSet();
            for (Integer idVrtInSS : ss.getList())
            {
                if (subGrpVrtIDs.contains(idVrtInSS))
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
                iter3.remove();
            }
            //else if (!ssSpanAnySGVrtx && ssWithinSubGrph) imposible!
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
                iter3.remove();
            }
        }

        // Remove vertices from molGraph
        for (Integer i : subGrpVrtIDs)
        {
            removeVertex(i);
        }

        return subGraph;
    }

//------------------------------------------------------------------------------

    /**
     * Removes the edge an updates the valence of the parent and child vertex 
     * after the edge has been removed.
     * @param vid the id of the vertex whose edge with its parent will be 
     * removed.
     * @return the id of the parent vertex.
     */

    public int removeEdgeWithParent(int vid)
    {
        int pvid = -1;
        int eid = getIndexOfEdgeWithParent(vid);

        if (eid != -1)
        {
            DENOPTIMEdge edge = getEdgeList().get(eid);
            
            DENOPTIMVertex src = getVertexWithId(edge.getSrcVertex());
            // update the attachment point of the source vertex
            int iA = edge.getSrcAPID();
            src.updateAttachmentPoint(iA, edge.getBondType().getValence());
            pvid = src.getVertexId();

            // update the attachment point of the target vertex
            DENOPTIMVertex trg = getVertexWithId(edge.getTrgVertex());
            int iB = edge.getTrgAPID();
            trg.updateAttachmentPoint(iB, edge.getBondType().getValence());

            // remove associated edge
            removeEdge(getEdgeList().get(eid));
        }

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

    public boolean removeBranchStartingAt(int vid, boolean symmetry)
            throws DENOPTIMException
    {
        boolean res = true;
        if (hasSymmetryInvolvingVertex(vid) && symmetry)
        {
            ArrayList<Integer> toRemove = new ArrayList<>();
            for (int i=0; i<getSymSetForVertexID(vid).size(); i++)
            {
                int svid = getSymSetForVertexID(vid).getList().get(i);
                toRemove.add(svid);
            }
            for (Integer svid : toRemove)
            {
                boolean res2 = removeBranchStartingAt(svid);
                if (!res2)
                {
                    res = res2;
                }
            }
        }
        else
        {
            res = removeBranchStartingAt(vid);
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

    public boolean removeBranchStartingAt(int vid)
            throws DENOPTIMException
    {
        // first delete the edge with the parent vertex
        int pvid = removeEdgeWithParent(vid);
        if (pvid == -1)
        {
            String msg = "Program Bug detected trying to delete vertex "
                    + vid + " from graph '" + this + "'. "
                    + "Unable to locate parent edge.";
            throw new DENOPTIMException(msg);
        }

        // now get the vertices attached to vid i.e. return vertex ids
        ArrayList<Integer> children = new ArrayList<>();
        getChildren(vid, children);

        // delete the children vertices
        for (int k : children) {
            removeVertex(k);
        }

        // now delete the edges containing the children
        ArrayList<DENOPTIMEdge> edges = getEdgeList();
        Iterator<DENOPTIMEdge> iter = edges.iterator();
        while (iter.hasNext())
        {
            DENOPTIMEdge edge = iter.next();
            for (int k : children) {
                if (edge.getSrcVertex() == k || edge.getTrgVertex() == k) {
                    // remove edge
                    iter.remove();
                    break;
                }
            }
        }

        // finally delete the vertex
        removeVertex(vid);

        return getVertexWithId(vid) == null;
    }

//------------------------------------------------------------------------------

    /**
     * Change all vertex IDs to the corresponding negative value. For instance
     * if the vertex ID is 12 this method changes it into -12.
     */

    @Deprecated
    public void changeSignToVertexID()
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
                if (getEdgeList().get(j).getSrcVertex() == vid) {
                    getEdgeList().get(j).setSrcVertex(nvid);
                }
                if (getEdgeList().get(j).getTrgVertex() == vid) {
                    getEdgeList().get(j).setTrgVertex(nvid);
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
     * Before any operation is performed on the graph, its vertices are
     * renumbered so as to differentiate them from their progenitors
     *
     */

    public void renumberGraphVertices()
    {
        renumberVerticesGetMap();
    }

//------------------------------------------------------------------------------

    /**
     * Reassign vertex IDs to a graph.
     * Before any operation is performed on the graph, its vertices are
     * renumbered so as to differentiate them from their progenitors
     * @return the key to convert old IDs into new ones
     */

    public HashMap<Integer,Integer> renumberVerticesGetMap() {
        HashMap<Integer, Integer> nmap = new HashMap<>();

        // for the vertices in the graph, get new vertex ids
        Set<DENOPTIMEdge> doneSrc = new HashSet<>();
        Set<DENOPTIMEdge> doneTrg = new HashSet<>();
        for (int i=0; i<getVertexCount(); i++)
        {
            int vid = getVertexList().get(i).getVertexId();
            int nvid = GraphUtils.getUniqueVertexIndex();

            nmap.put(vid, nvid);

            getVertexList().get(i).setVertexId(nvid);

            // update all edges with vid
            for (int j=0; j<getEdgeCount(); j++)
            {
                DENOPTIMEdge e = getEdgeList().get(j);
                if (e.getSrcVertex() == vid && !doneSrc.contains(e))
                {
                    e.setSrcVertex(nvid);
                    doneSrc.add(e);
                }
                if (e.getTrgVertex() == vid && !doneTrg.contains(e))
                {
                    e.setTrgVertex(nvid);
                    doneTrg.add(e);
                }
            }
        }
        // Update the sets of symmetrix vertex IDs
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
        //TODO-M6 ignore return value
        ArrayList<ObjectPair> rotBonds =
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
        
      //TODO-M6 del
        System.out.println("#######   allCombsOfRings: "+allCombsOfRings.size());

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
            //TODO-V3 get rid of serialization-based deep copying
            DENOPTIMGraph newGraph = (DENOPTIMGraph) DenoptimIO.deepCopy(this);
            // the above messes with the APClass references
            
            //TODO-M6
            // This is what we should use 
            //DENOPTIMGraph newGraph = this.clone();
            
            //TODO-M6 del
            System.out.println("_____Original");
            for (DENOPTIMAttachmentPoint ap : this.getAttachmentPoints())
            {
                APClass a = ap.getAPClass();
                System.out.println("  " +ap.getOwner()+ " "+ a + " " + a.hashCode());
            }
            System.out.println("_____CClone");
            for (DENOPTIMAttachmentPoint ap : newGraph.getAttachmentPoints())
            {
                APClass a = ap.getAPClass();
                System.out.println("  " +ap.getOwner()+ " "+ a + " " + a.hashCode());
            }
                
            HashMap<Integer,Integer> vRenum = newGraph.renumberVerticesGetMap();
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
        
        //DENOPTIMGraph sgClone = (DENOPTIMGraph) DenoptimIO.deepCopy(subGraph);
        sgClone.renumberGraphVertices();

        // Make the connection between molGraph and subGraph
        DENOPTIMVertex cvClone = sgClone.getVertexAtPosition(
                subGraph.getIndexOfVertex(childVertex.getVertexId()));

        DENOPTIMAttachmentPoint dap_Parent =
                parentVertex.getAttachmentPoints().get(parentAPIdx);
        DENOPTIMAttachmentPoint dap_Child =
                cvClone.getAttachmentPoints().get(childAPIdx);

        DENOPTIMEdge edge = null;
        if (FragmentSpace.useAPclassBasedApproach())
        {
            edge = parentVertex.connectVertices(
                    cvClone,
                    parentAPIdx,
                    childAPIdx);
            /*
             //TODO-M6: check this
                    dap_Parent.getAPClass(),
                    dap_Child.getAPClass());
                    */
        }
        else
        {
            edge = new DENOPTIMEdge(parentVertex.getAP(parentAPIdx),
                    cvClone.getAP(childAPIdx), parentVertex.getVertexId(),
                    cvClone.getVertexId(), parentAPIdx, childAPIdx, bndType);
            // decrement the num. of available connections
            dap_Parent.updateFreeConnections(-bndType.getValence());
            dap_Child.updateFreeConnections(-bndType.getValence());
        }
        if (edge == null)
        {
            String msg = "Program Bug in appendGraphOnAP: No edge created.";
            DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
            throw new DENOPTIMException(msg);
        }
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
            if (subGraph.hasSymmetryInvolvingVertex(origV.getVertexId()))
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
                    if (getIndexOfEdgeWithParent(v.getVertexId()) < 0)
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
                    if (getIndexOfEdgeWithParent(v.getVertexId()) < 0)
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
                    if (getIndexOfEdgeWithParent(v.getVertexId()) < 0)
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
                    if (getIndexOfEdgeWithParent(v.getVertexId()) < 0)
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

        //Outcoming connections (candidate vertex is the source)
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

        //TODO-V3 get rid of serialization-based deep copying
        DENOPTIMGraph modGraph = (DENOPTIMGraph) DenoptimIO.deepCopy(this);

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
                            modGraph.removeBranchStartingAt(cid,symmetry);
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
                        modGraph.removeBranchStartingAt(vid, symmetry);
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
}