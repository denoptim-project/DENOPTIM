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

import java.util.ArrayList;
import java.util.Iterator;
import java.io.Serializable;

import denoptim.exception.DENOPTIMException;
import denoptim.rings.ClosableChain;


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
        if ((m_pos >= gVertices.size()) || m_pos < 0)
            return null;
        return gVertices.get(m_pos);
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

//TODO: define whether this is a shallow or deep copy... might return a shallow
// copy. Have a look at DenoptimIO.deepCopy method
    @Override
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }

//------------------------------------------------------------------------------

    /**
     * return a list of vertex ids whose molecule id are the same as m_molId
     * @param m_molId
     * @return the list of vertices representing the fragment corresponding to m_molId
     */
    public ArrayList<Integer> getVerticesWithMolId(int m_molId)
    {
        ArrayList<Integer> lst = new ArrayList<>();
        for (int i=0; i<gVertices.size(); i++)
        {
            DENOPTIMVertex v = gVertices.get(i);
            if (v.getMolId() == m_molId)
                lst.add(v.getVertexId());
        }
        return lst;
    }

//------------------------------------------------------------------------------

    /**
     * returns a list of vertex ids whose molecule id are the same as
     * m_molId and occur at the given level
     * @param m_molId
     * @param m_lvl
     * @return the list of vertices representing the fragment corresponding to m_molId 
     * at the specified level
     */
    public ArrayList<Integer> getVerticesWithMolId(int m_molId, int m_lvl)
    {
        ArrayList<Integer> lst = new ArrayList<>();
        for (int i=0; i<gVertices.size(); i++)
        {
            DENOPTIMVertex v = gVertices.get(i);
            if (v.getMolId() == m_molId && v.getLevel() == m_lvl)
                lst.add(v.getVertexId());
        }
        return lst;
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
        for (int i=0; i<gVertices.size(); i++)
        {
            DENOPTIMVertex vtx = gVertices.get(i);
            mval = Math.max(mval, vtx.getLevel());
        }
        return mval;
    }

//------------------------------------------------------------------------------

    public int getMaxVertexId()
    {
        int mval = Integer.MIN_VALUE;
        for (int i=0; i<gVertices.size(); i++)
        {
            DENOPTIMVertex v = gVertices.get(i);
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
            DENOPTIMVertex vtx = getVertexWithId(src);
            return vtx;
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
            int srcdap = edge.getSourceDAP();
            return srcdap;
        }
        return -1;
    }

//------------------------------------------------------------------------------
    
    public ArrayList<Integer> getFragments(int ftype)
    {
        ArrayList<Integer> lstFrags = new ArrayList<>();
        for (int i=0; i<gVertices.size(); i++)
        {
            DENOPTIMVertex vtx = gVertices.get(i);
            if (vtx.getFragmentType() == ftype)
                lstFrags.add(vtx.getMolId());
        }
        return lstFrags;
    }    
    
//------------------------------------------------------------------------------    
    
    public void cleanup()
    {
        if (gVertices != null)
        {
            if (!gVertices.isEmpty())
            {
                for (int i=0; i<gVertices.size(); i++)
                {
                    DENOPTIMVertex vtx = gVertices.get(i);
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

}

