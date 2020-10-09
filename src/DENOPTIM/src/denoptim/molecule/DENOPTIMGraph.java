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

import com.hp.hpl.jena.graph.GraphUtil;

import java.io.Serializable;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.rings.ClosableChain;
import denoptim.utils.DENOPTIMMoleculeUtils;


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
        gVertices = m_vertices;
        gEdges = m_edges;
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
        gVertices = m_vertices;
        gEdges = m_edges;
        gRings = m_rings;
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
        gVertices = m_vertices;
        gEdges = m_edges;
        gRings = m_rings;
        closableChains = m_closableChains;
        symVertices = m_symVerts;
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

    public void setSymMap(ArrayList<SymmetricSet> m_mp)
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

    // when a property of the vertex changes, we need to update the vertex
    // in the arraylist
    public void updateVertex(DENOPTIMVertex m_vertex)
    {
        int idx = getIndexOfVertex(m_vertex.getVertexId());
        if (idx != -1)
        {
            gVertices.set(idx, m_vertex);
        }
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
    		if (e.getSourceVertex() == v.getVertexId())
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
        gVertices.add(m_vertex);
    }

//------------------------------------------------------------------------------

    /**
     * Remove a vertex from this graph. This method removes also edges and rings 
     * that involve the removed vertex. Symmetric sets of vertexes are corrected
     * accordingly: they are removed if there only one remaining vertex in the 
     * set, of purged from the removed vertex.
     * @param m_vertex the vertex to remove.
     */
    public void removeVertex(DENOPTIMVertex m_vertex)
    {   	
        if (!gVertices.contains(m_vertex))
        {
        	return;
        }
        
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
            if (vid == edge.getTargetVertex())
            {
                eToDel.add(edge);
            }
            // NB: the following allows to break the spanning tree
            if (vid == edge.getSourceVertex())
            {
                eToDel.add(edge);
            }
        }
        for (int i=0; i<eToDel.size(); i++)
        {        	
            DENOPTIMEdge edge = eToDel.get(i);
            int bndOrder = edge.getBondType();
            
            // update the connections of the parent(src) vertex
            int iA = edge.getSourceDAP();
            int srcvid = edge.getSourceVertex();
            DENOPTIMVertex src = this.getVertexWithId(srcvid);
            src.updateAttachmentPoint(iA, bndOrder);
            
            // update the connections of the child(trg) vertex
            int iB = edge.getTargetDAP();
            int trgvid = edge.getTargetVertex();
            if (this.containsVertexId(trgvid))
            {
                DENOPTIMVertex trg = this.getVertexWithId(trgvid);
                trg.updateAttachmentPoint(iB, bndOrder);
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
            if (edge.getTargetVertex() == m_vid)
            {
                lst.add(edge.getSourceVertex());
            }
            if (edge.getSourceVertex() == m_vid)
            {
                lst.add(edge.getTargetVertex());
            }
        }
        return lst;
    }

//------------------------------------------------------------------------------

    /**
     * @param v the <code>DENOPTIMVertex</code> for which the icident edges
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
     * icident edges are to be found
     * @return the list of incident <code>DENOPTIMEdge</code>s
     */

    public ArrayList<DENOPTIMEdge> getIncidentEdges(int vid)
    {         
        ArrayList<DENOPTIMEdge> lst = new ArrayList<DENOPTIMEdge>();
        for (int i=0; i<gEdges.size(); i++)
        {
            DENOPTIMEdge edge = gEdges.get(i);
            if (edge.getTargetVertex() == vid ||
                edge.getSourceVertex() == vid)
            {
                lst.add(edge);
            }
        }
        return lst;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * 
     * @param vidSrc
     * @param vidDest
     * @param dapSrc
     * @param dapDest
     * @return the bond type if found else 1
     */
    
    public int getBondType(int vidSrc, int vidDest, int dapSrc, int dapDest)
    {
        for (int i=0; i<gEdges.size(); i++)
        {
            DENOPTIMEdge edge = gEdges.get(i);
            if (edge.getSourceVertex() == vidSrc && 
                    edge.getTargetVertex() == vidDest && 
                    edge.getSourceDAP() == dapSrc && 
                    edge.getTargetDAP() == dapDest)
            {
                return edge.getBondType();
            }
//            else if (edge.getSourceVertex() == vidDest && 
//                    edge.getTargetVertex() == vidSrc && 
//                    edge.getSourceDAP() == dapDest && 
//                    edge.getTargetDAP() == dapSrc)
//            {
//                return edge.getBondType();
//            }
        }
        return 1;
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
    
    public DENOPTIMVertex getBondingVertex(DENOPTIMVertex vertex, int dapidx, 
                                                String rcn)
    {
        int n = getEdgeCount();
        for (int i = 0; i < n; i++)
        {
            DENOPTIMEdge edge = getEdgeList().get(i);

            // get the vertex ids
            int v1_id = edge.getSourceVertex();
            int v2_id = edge.getTargetVertex();
            
            int dap_idx_v1 = edge.getSourceDAP();
            
            if (rcn != null)
            {
                String rcstr = edge.getSourceReaction();
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
            int v1_id = edge.getSourceVertex();
            int v2_id = edge.getTargetVertex();

            int dap_idx_v1 = edge.getSourceDAP();

            if (srcVert.getVertexId() == v1_id && v2_id == dstVert.getVertexId() 
                                && dap_idx_v1 == dapidx)
            {
                return edge.getTargetDAP();
            }
        }

        return -1;
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
            if (edge.getSourceVertex() == m_vid)
            {
                lst.add(edge.getTargetVertex());
            }
        }
        return lst;
    }

//------------------------------------------------------------------------------

    /**
     * Returns a deep-copy of this graph. The vertex IDs are not changed, so you
     * might want to renumber the graph with 
     * {@link GraphUtil.renumberVerticesGetMap}.
     */
    @Override
    public DENOPTIMGraph clone()
    {
        //TODO-V3: should this be replaced by DenoptimIO.deepCopy?
        
        /*
        return (DENOPTIMGraph) DenoptimIO.deepCopy(this);
        */
        
        // When cloning the VertedID remains the same so we'll have two 
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
        clone.setSymMap(cSymVertices);
        
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

            if (edge.getTargetVertex() == m_vid)
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

            if (edge.getTargetVertex() == m_vid)
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

            if (edge.getSourceVertex() == m_vid)
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

            if (edge.getSourceVertex() == m_vid)
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
            int src = edge.getSourceVertex();
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
            return edge.getSourceDAP();
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
    				thisG.getVertexWithId(et.getTargetVertex())))
    		{
    			throw new DENOPTIMException("More than one attempt to set vertex map.");
    		}
    		vertexMap.put(thisG.getVertexWithId(et.getTargetVertex()),
    				otherG.getVertexWithId(eo.getTargetVertex()));
    		
    		DENOPTIMVertex[] pair = new DENOPTIMVertex[]{
    				thisG.getVertexWithId(et.getTargetVertex()),
    				otherG.getVertexWithId(eo.getTargetVertex())};
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

    public List<DENOPTIMAttachmentPoint> getAvailableAPs() {
        return null;
    }

//------------------------------------------------------------------------------

    /**
     * calculate the number of atoms from the graph representation
     * @return number of heavy atoms in the molecule
     */
    public int getHeavyAtomsCount()
    {
        int n = 0;
        ArrayList<DENOPTIMVertex> vlst = getVertexList();

        for (DENOPTIMVertex denoptimVertex : vlst) 
        {
            n += denoptimVertex.getHeavyAtomsCount();
        }
        return n;
    }

//------------------------------------------------------------------------------
  
}
