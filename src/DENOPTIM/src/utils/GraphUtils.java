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

package utils;

import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import org.openscience.cdk.graph.ConnectivityChecker;

import org.apache.commons.math3.random.MersenneTwister;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtom;

import constants.DENOPTIMConstants;
import exception.DENOPTIMException;
import molecule.DENOPTIMAttachmentPoint;
import molecule.DENOPTIMEdge;
import molecule.DENOPTIMGraph;
import molecule.DENOPTIMVertex;
import molecule.DENOPTIMRing;
import molecule.SymmetricSet;
import io.DenoptimIO;
import rings.ClosableChain;
import rings.CyclicGraphHandler;
import rings.RingClosureParameters;
import fragspace.FragmentSpace;
import fragspace.FragmentSpaceParameters;
import logging.DENOPTIMLogger;
import java.util.logging.Level;


/**
 * Utilities for graphs.
 *
 * @author Vishwesh Venkatraman 
 * @author Marco Foscato
 */
public class GraphUtils
{
    private static AtomicInteger vertexCounter = new AtomicInteger(1);
    private static AtomicInteger graphCounter = new AtomicInteger(1);
    private static AtomicInteger molCounter = new AtomicInteger(1);

    private static boolean debug = false;

//------------------------------------------------------------------------------

    /**
     * Reset the unique vertex counter to the given value. In order to keep the
     * uniqueness on the index, this method accepts only reset values
     * that are higher that the current one. Attempts to reset to lower values 
     * return an exception.
     * @param val the new value for the counter. This value will be given to
     * next call of the getUniqueVertexIndex method.
     * @throws DENOPTIMException if the reset value is lower than the current
     * value of the index.
     */

    public static void resetUniqueVertexCounter(int val) 
                                                        throws DENOPTIMException
    {
        if (vertexCounter.get() >= val)
        {
            String msg = "Attempt to reser the unique vertex ID using "
                         + val + " while the current value is " 
                         + vertexCounter.get();
            throw new DENOPTIMException(msg);
        }
        vertexCounter = new AtomicInteger(val);
    }

//-----------------------------------------------------------------------------

    /**
     * Unique counter for the number of graph vertices generated.
     * @return the new vertex id (number)
     */

    public static synchronized int getUniqueVertexIndex()
    {
        return vertexCounter.getAndIncrement();
    }

//------------------------------------------------------------------------------

    /**
     * Check if all the APs are satisfied.
     *
     * @param molGraph the Graph representation of the molecule
     * @return <code>true</code> if all APs have been satisfied
     */
    public static boolean areAllAPSatisfied(DENOPTIMGraph molGraph)
    {
        ArrayList<DENOPTIMVertex> vertices = molGraph.getVertexList();
        boolean found = true;
        for (DENOPTIMVertex vtx : vertices)
        {
            ArrayList<DENOPTIMAttachmentPoint> daps = vtx.getAttachmentPoints();
            for (DENOPTIMAttachmentPoint dp : daps)
            {
                if (dp.isAvailable())
                {
                    String msg = "Free APs available for Vertex: "
                            + vtx.getVertexId()
                            + " MolId: " + vtx.getMolId()
                            + " Ftype: " + vtx.getFragmentType();
                    DENOPTIMLogger.appLogger.log(Level.WARNING, msg);
                    found = false;
                    break;
                }
            }
        }

        return found;
    }

//------------------------------------------------------------------------------
    
    /**
     * Extracts the subgraph of a graph G starting from a given vertex in G.
     * Only the given vertex V and all child vertexes are considered part of 
     * the subgraph, which encodes also rings and symmetric sets. Potential
     * rings that include vertices not belonging to the subgraph are lost.
     * @param molGraph the graph G
     * @param vid the vertex id from which the extraction has to start
     * @return the subgraph 
     */

    public static DENOPTIMGraph extractSubgraph(DENOPTIMGraph molGraph, int vid)
 throws DENOPTIMException
    {
        DENOPTIMGraph subGraph = new DENOPTIMGraph();

        // Identify vertices belonging to subgraph (i.e., vid + children)
        ArrayList<Integer> subGrpVrtIDs = new ArrayList<>();
        subGrpVrtIDs.add(vid);
        GraphUtils.getChildren(molGraph, vid, subGrpVrtIDs);
        // Copy vertices to subgraph
        for (Integer i : subGrpVrtIDs)
        {
            subGraph.addVertex(molGraph.getVertexWithId(i));
            // vertices are removed later (see below) otherwise also
            // rings and sym.sets are removed
        }

        // Remove the edge joining graph and gubgraph
        removeEdgeWithParent(molGraph, vid);

        // Identify edges belonging to subgraph
        Iterator<DENOPTIMEdge> iter1 = molGraph.getEdgeList().iterator();
        while (iter1.hasNext())
        {
            DENOPTIMEdge edge = iter1.next();
            for (Integer k : subGrpVrtIDs)
            {
                if (edge.getSourceVertex() == k || edge.getTargetVertex() == k)
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
        Iterator<DENOPTIMRing> iter2 = molGraph.getRings().iterator();
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
        Iterator<SymmetricSet> iter3 = molGraph.getSymSetsIterator();
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
            molGraph.removeVertex(i);
        }

        return subGraph;
    }

//------------------------------------------------------------------------------    
    /**
     * remove the vertices and edges associated with the children of the 
     * current vertex OBSOLETE_METHOD
     * @param molGraph
     * @param vid the vertex id which is the crossover point
     * @param lstChild child vertices of vid
     * @param lstVert list of vertices to be populated
     * @param lstEdges list of edges to be populated
     */
    
    public static void extractSubgraph(DENOPTIMGraph molGraph,
                            int vid, ArrayList<Integer> lstChild,
                            ArrayList<DENOPTIMVertex> lstVert,
                            ArrayList<DENOPTIMEdge> lstEdges)
    {
        // for each child vertex update the level
        for (int i=0; i<lstChild.size(); i++)
        {
            lstVert.add(molGraph.getVertexWithId(lstChild.get(i)));
            molGraph.removeVertex(lstChild.get(i));
        }
        Iterator<DENOPTIMEdge> iter1 = molGraph.getEdgeList().iterator();
        while (iter1.hasNext())
        {
            DENOPTIMEdge edge = iter1.next();

            for (int i=0; i<lstChild.size(); i++)
            {
                int k = lstChild.get(i);
                if (edge.getSourceVertex() == k || edge.getTargetVertex() == k)
                {
                    lstEdges.add(edge);
                    // remove edge
                    iter1.remove();
                    break;
                }
            }
        }
        lstVert.add(0, molGraph.getVertexWithId(vid));
        molGraph.removeVertex(vid);
    }

//------------------------------------------------------------------------------
    
   /**
    * updates the valence of the parent and child vertex after the edge
    * has been removed
    * @param g the molecular graph representation
    * @param vid the id of the vertex whose edge with its parent will be removed
    * @return the id of the parent vertex
    */

    public static int removeEdgeWithParent(DENOPTIMGraph g, int vid)
    {
        int pvid = -1;
        int eid = g.getIndexOfEdgeWithParent(vid);

        if (eid != -1)
        {
            DENOPTIMEdge edge = g.getEdgeList().get(eid);

            int bndOrder = edge.getBondType();

            DENOPTIMVertex src = g.getVertexWithId(edge.getSourceVertex());
            // update the attachment point of the source vertex
            int iA = edge.getSourceDAP();
            src.updateAttachmentPoint(iA, bndOrder);
            pvid = src.getVertexId();


            // update the attachment point of the target vertex
            DENOPTIMVertex trg = g.getVertexWithId(edge.getTargetVertex());
            int iB = edge.getTargetDAP();
            trg.updateAttachmentPoint(iB, bndOrder);

            // remove associated edge
            g.removeEdge(g.getEdgeList().get(eid));
        }

        return pvid;
    }

//------------------------------------------------------------------------------

    /**
     * Obtain the parent vertex for the current vertex
     * @param g
     * @param vid
     * @return the vertex id of the parent
     */
    public static int getParentVertex(DENOPTIMGraph g, int vid)
    {
        int pvid = -1;
        for (int j=0; j<g.getEdgeCount(); j++)
        {
            DENOPTIMEdge edge = g.getEdgeAtPosition(j);
            if (edge.getTargetVertex() == vid)
            {
                pvid = edge.getSourceVertex();
                break;
            }
        }

        return pvid;
    }

//------------------------------------------------------------------------------

    /**
     * updates the number of connections for the parent and child vertex
     * after the edge has been removed
     * @param g the molecular graph representation
     * @param vid the id of the vertex whose edge with its child will be removed
     * @param cvid the id of the child vertex
     * @return pvid the id of the parent vertex i.e. vid
     */

    public static int removeEdgeWithChild(DENOPTIMGraph g, int vid, int cvid)
    {
        int pvid = -1;
        int eid = -1;
        for (int j=0; j<g.getEdgeCount(); j++)
        {
            DENOPTIMEdge edge = g.getEdgeAtPosition(j);

            if (edge.getSourceVertex() == vid && edge.getTargetVertex() == cvid)
            {
                int bndOrder = edge.getBondType();

                DENOPTIMVertex src = g.getVertexWithId(vid);
                // update the attachment point of the source vertex
                int iA = edge.getSourceDAP();
                DENOPTIMAttachmentPoint apA = src.getAttachmentPoints().get(iA);
                apA.updateAPConnections(bndOrder);


                // update the attachment point of the target vertex
                DENOPTIMVertex trg = g.getVertexWithId(cvid);
                int iB = edge.getTargetDAP();
                DENOPTIMAttachmentPoint apB = trg.getAttachmentPoints().get(iB);
                apB.updateAPConnections(bndOrder);

                pvid = vid;
                eid = j;

                break;
            }
        }

        // remove associated edge
        if (eid != -1)
        {
            g.removeEdge(g.getEdgeList().get(eid));
        }

        return pvid;
    }

//------------------------------------------------------------------------------

    /**
     * Checks the graph for unused APs that need to be capped
     * @param molGraph
     * @return <code>true</code> if the graph has at least one AP that needs 
     * to be capped
     */

    public static boolean graphNeedsCappingGroups(DENOPTIMGraph molGraph)
    {
        for (DENOPTIMVertex v : molGraph.getVertexList())
        {
            if (v.getFragmentType() == 2)
            {
                continue;
            }
            for (DENOPTIMAttachmentPoint ap : v.getAttachmentPoints())
            {
                if (ap.isAvailable())
                {
                    if (FragmentSpace.getCappingClass(ap.getAPClass()) != null)
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

//------------------------------------------------------------------------------

    /**
     * For the given graph remove all capping groups if any
     * @param molGraph
     */

    public static void removeCappingGroups(DENOPTIMGraph molGraph)
    {
        ArrayList<DENOPTIMVertex> lstVerts = molGraph.getVertexList();
        ArrayList<Integer> rvids = new ArrayList<>();
        for (int i=0; i<lstVerts.size(); i++)
        {
            DENOPTIMVertex vtx = lstVerts.get(i);
            // capping groups have fragment type 2
            if (vtx.getFragmentType() == 2 && !molGraph.isVertexInRing(vtx))
            {
                rvids.add(vtx.getVertexId());
            }
        }

        // remove the vids from the vertex lst
        for (int i=0; i<rvids.size(); i++)
        {
            int vid = rvids.get(i);
            molGraph.removeVertex(molGraph.getVertexWithId(vid));
        }

        // remove edges containing these vertex ids
        ArrayList<DENOPTIMEdge> lstEdges = molGraph.getEdgeList();
        ArrayList<DENOPTIMEdge> redges = new ArrayList<>();
        for (int i=0; i<lstEdges.size(); i++)
        {
            DENOPTIMEdge edge = lstEdges.get(i);
            if (rvids.contains(edge.getTargetVertex()))
            {
                redges.add(edge);
            }
        }

        for (int i=0; i<redges.size(); i++)
        {
            DENOPTIMEdge edge = redges.get(i);
            int bndOrder = edge.getBondType();
            // for the parent update the connections
            int iA = edge.getSourceDAP();
            int srcvid = edge.getSourceVertex();
            DENOPTIMVertex src = molGraph.getVertexWithId(srcvid);
            src.updateAttachmentPoint(iA, bndOrder);
            molGraph.removeEdge(edge);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Gets all the children of the current vertex
     * @param g the molecular graph representation
     * @param vid the vertex whose children are to be located
     * @param children list containing the vertex ids of the children
     */
    public static void getChildren(DENOPTIMGraph g, int vid,
                                                ArrayList<Integer> children)
    {
        // get the child vertices of vid
        ArrayList<Integer> lst = g.getChildVertices(vid);

        if (lst.isEmpty())
            return;

        for (int i=0; i<lst.size(); i++)
        {
            if (!children.contains(lst.get(i)))
            {
                children.add(lst.get(i));
                getChildren(g, lst.get(i), children);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Deletes the branch, i.e., the specified vertex and its children
     * @param molGraph
     * @param vid
     * @param symmetry use <code>true</code> to enforce deletion of all 
     * symmetric verices
     * @return <code>true</code> if operation is successful
     * @throws DENOPTIMException
     */

    public static boolean deleteVertex(DENOPTIMGraph molGraph, int vid, 
				      boolean symmetry) throws DENOPTIMException
    {
	boolean res = true;
        if (molGraph.hasSymmetryInvolvingVertex(vid) && symmetry)
        {
            ArrayList<Integer> toRemove = new ArrayList<Integer>();
            for (int i=0; i<molGraph.getSymSetForVertexID(vid).size(); i++)
            {
                int svid = molGraph.getSymSetForVertexID(vid).getList().get(i);
                toRemove.add(svid);
            }
            for (Integer svid : toRemove)
            {
                boolean res2 = GraphUtils.deleteVertex(molGraph, svid);
	        if (!res2)
		{
		    res = res2;
		}
            }
        }
	else
        {
            res = GraphUtils.deleteVertex(molGraph, vid);
        }

	return res;
    }

//------------------------------------------------------------------------------

    /**
     * Deletes the branch, i.e., the specified vertex and its children.
     * No handling of symmetry.
     * @param molGraph
     * @param vid
     * @return <code>true</code> if operation is successful
     * @throws DENOPTIMException
     */

    public static boolean deleteVertex(DENOPTIMGraph molGraph, int vid)
                                                        throws DENOPTIMException
    {
        // first delete the edge with the parent vertex
        int pvid = removeEdgeWithParent(molGraph, vid);
        if (pvid == -1)
        {
            String msg = "Program Bug detected trying to  delete vertex "
                          + vid + " from graph '" + molGraph + "'. "
			  + "Unable to locate parent edge.";
            throw new DENOPTIMException(msg);
        }

        // now get the vertices attached to vid i.e. return vertex ids
        ArrayList<Integer> children = new ArrayList<>();
        getChildren(molGraph, vid, children);

        // delete the children vertices
        for (int i=0; i<children.size(); i++)
        {
            int k = children.get(i);
            molGraph.getVertexWithId(k).getAttachmentPoints().clear();
            molGraph.removeVertex(k);
        }

        // now delete the edges containing the children
        ArrayList<DENOPTIMEdge> edges = molGraph.getEdgeList();
        Iterator<DENOPTIMEdge> iter = edges.iterator();
        while (iter.hasNext())
        {
            DENOPTIMEdge edge = iter.next();
            for (int i=0; i<children.size(); i++)
            {
                int k = children.get(i);
                if (edge.getSourceVertex() == k || edge.getTargetVertex() == k)
                {
                    // remove edge
                    iter.remove();
                    break;
                }
            }
        }

        // finally delete the vertex
        molGraph.removeVertex(vid);

        if (molGraph.getVertexWithId(vid) == null)
            return true;
        return false;
    }

//------------------------------------------------------------------------------

    /**
     * Change all vertex IDs to the corresponding negative value. For instance
     * if the vertex ID is 12 this method changes it into -12.
     * @param molGraph
     */

    public static void changeSignToVertexID(DENOPTIMGraph molGraph)
    {
        HashMap<Integer, Integer> nmap = new HashMap<>();
        for (int i=0; i<molGraph.getVertexCount(); i++)
        {
            int vid = molGraph.getVertexList().get(i).getVertexId();
            int nvid = -vid;
            nmap.put(vid, nvid);

            molGraph.getVertexList().get(i).setVertexId(nvid);

            for (int j=0; j<molGraph.getEdgeCount(); j++)
            {
                if (molGraph.getEdgeList().get(j).getSourceVertex() == vid)
                        molGraph.getEdgeList().get(j).setSourceVertex(nvid);
                if (molGraph.getEdgeList().get(j).getTargetVertex() == vid)
                        molGraph.getEdgeList().get(j).setTargetVertex(nvid);
            }
        }
        Iterator<SymmetricSet> iter = molGraph.getSymSetsIterator();
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
     * @param molGraph
     *
     */

    public static void renumberGraphVertices(DENOPTIMGraph molGraph)
    {
        HashMap<Integer, Integer> nmap = renumberVerticesGetMap(molGraph);
    }

//------------------------------------------------------------------------------

    /**
     * Reassign vertex IDs to a graph. 
     * Before any operation is performed on the graph, its vertices are 
     * renumbered so as to differentiate them from their progenitors
     * @param molGraph 
     * @return the key to convert old IDs into new ones
     */

    public static HashMap<Integer,Integer> renumberVerticesGetMap(
                                                         DENOPTIMGraph molGraph)
    {
        HashMap<Integer, Integer> nmap = new HashMap<>();

        // for the vertices in the graph, get new vertex ids
        Set<DENOPTIMEdge> doneSrc = new HashSet<DENOPTIMEdge>();
        Set<DENOPTIMEdge> doneTrg = new HashSet<DENOPTIMEdge>();
        for (int i=0; i<molGraph.getVertexCount(); i++)
        {
            int vid = molGraph.getVertexList().get(i).getVertexId();

            int nvid = getUniqueVertexIndex();

            nmap.put(vid, nvid);

            molGraph.getVertexList().get(i).setVertexId(nvid);

            // update all edges with vid
            for (int j=0; j<molGraph.getEdgeCount(); j++)
            {
                DENOPTIMEdge e = molGraph.getEdgeList().get(j);
                if (e.getSourceVertex() == vid && !doneSrc.contains(e))
                {
                    e.setSourceVertex(nvid);
                    doneSrc.add(e);
                }
                if (e.getTargetVertex() == vid && !doneTrg.contains(e))
                {
                    e.setTargetVertex(nvid);
                    doneTrg.add(e);
                }
            }
        }
        // Update the sets of symmetrix vertex IDs
        Iterator<SymmetricSet> iter = molGraph.getSymSetsIterator();
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
     * Connects 2 vertices by an edge based on reaction type
     * @param vtxA source vertex
     * @param vtxB target vertex
     * @param iA index of Attachment point in first vertex
     * @param iB index of Attachment point in second vertex
     * @param srcRcn the reaction scheme at the source
     * @param trgRcn the reaction scheme at the target
     * @return DENOPTIMEdge 
     */

    public static DENOPTIMEdge connectVertices(DENOPTIMVertex vtxA,
                                DENOPTIMVertex vtxB, int iA, int iB, 
                                String srcRcn, String trgRcn)
    {
        //System.err.println("Connecting vertices RCN");
        DENOPTIMAttachmentPoint dap_A = vtxA.getAttachmentPoints().get(iA);
        DENOPTIMAttachmentPoint dap_B = vtxB.getAttachmentPoints().get(iB);

        if (dap_A.isAvailable() && dap_B.isAvailable())
        {
            //System.err.println("Available APs");
            String rname = trgRcn.substring(0, trgRcn.indexOf(':'));

            // look up the reaction bond order table
            int bndOrder = FragmentSpace.getBondOrderMap().get(rname);
            //System.err.println("Bond: " + bndOrder + " " + srcRcn + " " + trgRcn);

            // create a new edge
            DENOPTIMEdge edge = new DENOPTIMEdge(vtxA.getVertexId(), vtxB.getVertexId(),
                                                     iA, iB, bndOrder);
            edge.setSourceReaction(srcRcn);
            edge.setTargetReaction(trgRcn);

            // update the attachment point info
            dap_A.updateAPConnections(-bndOrder); // decrement the connections
            dap_B.updateAPConnections(-bndOrder); // decrement the connections

            return edge;
        }
        //System.err.println("PROBLEM getting null");
        return null;
    }

//------------------------------------------------------------------------------

    /**
     * Get the id of the vertex attached at the specified attachment point
     * index of the given vertex
     * @param molGraph
     * @param curVertex
     * @param dapIdx
     * @return id of the vertex
     */

    public static int getVertexForAP(DENOPTIMGraph molGraph,
                                        DENOPTIMVertex curVertex, int dapIdx)
    {
        // loop thru the edges with the children and identify the vertex
        // associated
        int vid = -1;
        ArrayList<DENOPTIMEdge> edges = molGraph.getEdgeList();
        for (int i=0; i<edges.size(); i++)
        {
            DENOPTIMEdge edge = edges.get(i);
            if (edge.getSourceVertex() == curVertex.getVertexId())
            {
                if (edge.getSourceDAP() == dapIdx)
                {
                    vid = edge.getTargetVertex();
                    break;
                }
            }
        }

        return vid;
    }

//------------------------------------------------------------------------------    
    /**
     * Returns the list of vertices that are related by constitutional symmetry
     * to the given one. This method searches for symmetry independently on the
     * {@link SymmetricSet}s that can be stored in the {@link DENOPTIGraph}.
     * The vertices selected are those that satisfy simultaneously all the
     * following criteria:
     * <ul>
     * <li>vertices belonging to the same level (i.e. distance from the 
     * core/scaffold),</li>
     * <li>vertices containing the same fragment (i.e., same molID),</li>
     * <li>vertices bound to parents containing the same fragment (i.e., parents
     * with same molID),</li> 
     * <li>vertices binding their parents with APs having the same APclass.</li>
     * </ul>
     * TODO: consider comparing the two chains of edges that link 
     * i) the curVertes to the core/scaffold vertex, and 
     * ii) the candidate symmetric vertex to the core/Scaffold vertex.
     * @param molGraph molecular graph containing the vertex
     * @param curVertex the vertex at which a new fragment will be added
     * @param rcnstatus set to <code>true</code> when using class-based approach
     * @return the list of vertices related to <code>curVertex</code> according
     * to DENOPTIM's perception of symmetry.
     * The list does NOT include <code>curVertex</code>
     */

    public static ArrayList<Integer> getSymmetricVertices(
            DENOPTIMGraph molGraph, DENOPTIMVertex curVertex, boolean rcnstatus)
    {
        ArrayList<Integer> copylst = new ArrayList<>();

        // Identify the current Vertex
        int molid = curVertex.getMolId();
        int lvl = curVertex.getLevel();
        int cvid = curVertex.getVertexId();

        // Find the parent, if any
        DENOPTIMVertex parent = molGraph.getParent(cvid);

        if (parent == null)
        {
            //no other vertex in the graph: nothing to do
            return copylst;
        }

        // Identify the parent and the APclass used in by curVertex to bind
        // its parent
        int parent_molid = parent.getMolId();
        int eidx = molGraph.getIndexOfEdgeWithParent(cvid);
        DENOPTIMEdge edge = molGraph.getEdgeAtPosition(eidx);
        String rcn = edge.getTargetReaction();

        // Identify vertices at the same level that have the same molid
        ArrayList<Integer> lstv_symmolid =
                                      molGraph.getVerticesWithMolId(molid, lvl);

        // Analyse the list and apply further selection criteria (see below)
        for (int i=0; i<lstv_symmolid.size(); i++)
        {
            int vid = lstv_symmolid.get(i);

            // Exclute the curVertex from the analysis
            if (vid == cvid)
                continue;

            // for this vertex, get the parent
            DENOPTIMVertex parent_vid = molGraph.getParent(vid);

            // This should never happen, unless at some point multi-seed graphs
            // are used, which is not allowed currently
            if (parent_vid == null)
                continue;

            // get the molid of the parent
            int locParent_molid = parent_vid.getMolId();
            if (parent_molid != locParent_molid)
                continue;

            if (!rcnstatus)
            {
                // Without class-based approach, no other condition
                copylst.add(vid);
                continue;
            }
            else
            {
                // With class-based approach, compare also APClass of the AP
                // used by this candidate vertex to bind its parent vertex
                int idx = molGraph.getIndexOfEdgeWithParent(vid);
                DENOPTIMEdge e = molGraph.getEdgeAtPosition(idx);
                String rcn2 = e.getTargetReaction();
                if (rcn.equalsIgnoreCase(rcn2))
                {
                    // add the vertex to the symmetry copy list
                    copylst.add(vid);
                }
            }
        }

        return copylst;
    }

//------------------------------------------------------------------------------    
    /**
     * @Deprecated
     */
    public static void updateVertexCounter(int num)
    {
        vertexCounter.set(num);
    }
    
//------------------------------------------------------------------------------        

    /**
     * Update the level at which the new vertices have been added. This 
     * is generally applicable for a crossover or for a substitution operation
     * @param lstVert
     * @param lvl 
     */

    public static void updateLevels(ArrayList<DENOPTIMVertex> lstVert, int lvl)
    {
        int levRoot = lstVert.get(0).getLevel();
        int correction = lvl - levRoot;
        for (int i=0; i<lstVert.size(); i++)
        {
            lstVert.get(i).setLevel(lstVert.get(i).getLevel() + correction);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Reset the unique graph counter to the given value. In order to keep the
     * uniqueness on the index, this method accepts only reset values
     * that are higher that the current one. Attempts to reset to lower values
     * return an exception.
     * @param val the new value for the counter. This value will be given to
     * next call of the getUniqueGraphIndex method.
     * @throws DENOPTIMException if the reset value is lower than the current
     * value of the index.
     */

    public static void resetUniqueGraphCounter(int val) throws DENOPTIMException
    {
        if (graphCounter.get() >= val)
        {
            String msg = "Attempt to reser the unique graph ID using "
                         + val + " while the current value is "
                         + graphCounter.get();
            throw new DENOPTIMException(msg);
        }
        graphCounter = new AtomicInteger(val);
    }

//------------------------------------------------------------------------------
    
    /**
     * Unique counter for the number of graphs generated.
     * @return a new Graph id (number)
     */

    public static synchronized int getUniqueGraphIndex()
    {
        return graphCounter.getAndIncrement();
    }

//------------------------------------------------------------------------------

    /**
     * Reset the unique mol counter to the given value. In order to keep the
     * uniqueness on the index, this method accepts only reset values
     * that are higher that the current one. Attempts to reset to lower values
     * return an exception.
     * @param val the new value for the counter. This value will be given to
     * next call of the getUniqueMoleculeIndex method.
     * @throws DENOPTIMException if the reset value is lower than the current
     * value of the index.
     */

    public static void resetUniqueMoleculeCounter(int val) 
                                                        throws DENOPTIMException
    {
        if (molCounter.get() >= val)
        {
            String msg = "Attempt to reser the unique mol ID using "
                         + val + " while the current value is "
                         + molCounter.get();
            throw new DENOPTIMException(msg);
        }
        molCounter = new AtomicInteger(val);
    }

//------------------------------------------------------------------------------
    
    /**
     * Unique counter for the number of molecules generated.
     * @return the new molecule id (number)
     */

    public static synchronized int getUniqueMoleculeIndex()
    {
        return molCounter.getAndIncrement();
    }

//------------------------------------------------------------------------------

    /**
     * Finalizes a candidate chemical entity by evaluating its
     * <code>DENOPTIMGraph</code> and assigning prelominary molecular, 
     * SMILES and INCHI representations. 
     * The chemical entity is evaluated also gainst the
     * criteria defined by <code>FragmentSpaceParameters</code> and 
     * <code>RingClosureParameters</code>.
     * <b>WARNING</b> Although Cartesian coordinates are assigned to each atom
     * and pseudo-atom in the molecular representation ,
     * such coordinates do <b>NOT</b> represent a valid 3D model.
     * As a consequence stereochemical descriptors in the INCHI representation
     * are not consistent with the actual arrangement of fragments.
     * @param molGraph the molecular graph representation
     * @return an object array containing the inchi code, the SMILES string
     *         and the 2D representation of the molecule.
     *         <code>null</code> is returned if any check or conversion fails.
     * @throws DENOPTIMException
     */

    public static Object[] evaluateGraph(DENOPTIMGraph molGraph)
                                                      throws DENOPTIMException
    {
        // check for null graph
        if (molGraph == null)
        {
            String msg = "Evaluation of graph: input graph is null!";
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            return null;
        }

        // calculate the molecule representation        
        IAtomContainer mol = GraphConversionTool.convertGraphToMolecule(molGraph, true);
        if (mol == null)
        {
            String msg ="Evaluation of graph: graph-to-mol returned null!"
                                                          + molGraph.toString();
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            return null;
        }

        // check if the molecule is connected
        boolean isConnected = ConnectivityChecker.isConnected(mol);
        if (!isConnected)
        {
            String msg = "Evaluation of graph: Not all connected"
                                                          + molGraph.toString();
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            return null;
        }

        // SMILES
        String smiles = null;
        smiles = DENOPTIMMoleculeUtils.getSMILESForMolecule(mol);
        if (smiles == null)
        {
            String msg = "Evaluation of graph: SMILES is null! "
                                                          + molGraph.toString();
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
        if (FragmentSpaceParameters.useAPclassBasedApproach())
        {
            if (foundForbiddenEnd(molGraph))
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
     * Evaluates the possibility of closing rings in a given graph and if
     * any ring can be closed, then it randomy choses one of the combinations
     * of ring closures
     * that involves the highest number of new rings.
     * @param res an object array containing the inchi code, the smiles string
     * and the 2D representation of the molecule. This object can be
     * <code>null</code> if inchi/smiles/2D conversion fails.
     * @param molGraph the <code>DENOPTIMGraph</code> on which rings are to
     * be identified
     * @return <code>true</code> unless no ring can be set up even if required
     * @throws exception.DENOPTIMException
     */

    public static boolean setupRings(Object[] res, DENOPTIMGraph molGraph)
                                                        throws DENOPTIMException
    {
        boolean rcnEnabled = FragmentSpaceParameters.useAPclassBasedApproach();
        if (!rcnEnabled)
            return true;

        boolean evaluateRings = RingClosureParameters.allowRingClosures();
        if (!evaluateRings)
            return true;

        // get a atoms/bonds molecular representation (no 3D needed)
        IAtomContainer mol = GraphConversionTool.convertGraphToMolecule(molGraph,false);

        // Set rotatability property as property of IBond
        ArrayList<ObjectPair> rotBonds =
                                  RotationalSpaceUtils.defineRotatableBonds(mol,
                                   FragmentSpaceParameters.getRotSpaceDefFile(),
                                                                    true, true);

        // perceive possibility to close rings...
        CyclicGraphHandler cgh = new CyclicGraphHandler(
                                             FragmentSpace.getScaffoldLibrary(),
                                             FragmentSpace.getFragmentLibrary(),
                                              FragmentSpace.getCappingLibrary(),
                                      FragmentSpace.getRCCompatibilityMatrix());
        // ...and choose one
        Set<DENOPTIMRing> combsOfRings = cgh.getRandomCombinationOfRings(mol,
                                                                      molGraph);

        // Assign the rings to the graph
        if (combsOfRings.size() > 0)
        {
            for (DENOPTIMRing ring : combsOfRings)
            {
                molGraph.addRing(ring);
            }
        }

        // Update the IAtomContainer representation
        DENOPTIMMoleculeUtils.removeRCA(mol,molGraph);
        res[2] = mol;

        // Update the SMILES representation
        String molsmiles = DENOPTIMMoleculeUtils.getSMILESForMolecule(mol);
        if (molsmiles == null)
        {
            String msg = "Evaluation of graph: SMILES is null! "
                                                          + molGraph.toString();
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            molsmiles = "FAIL: NO SMILES GENERATED";
        }
        res[1] = molsmiles;

        // Update the INCHI key representation
        ObjectPair pr = DENOPTIMMoleculeUtils.getInchiForMolecule(mol);
        if (pr.getFirst() == null)
        {
            String msg = "Evaluation of graph: INCHI is null!";
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            pr.setFirst("UNDEFINED");
        }
        res[0] = pr.getFirst();

        return true;
    }

//------------------------------------------------------------------------------

    /**
     * Evaluates the possibility of closing rings in a given graph and 
     * generates all alternative graphs requlting by different combinations of
     * rings
     * @param molGraph the <code>DENOPTIMGraph</code> on which rings are to
     * be identified
     * @return <code>true</code> unless no ring can be set up even if required
     * @throws exception.DENOPTIMException
     */
    public static ArrayList<DENOPTIMGraph> makeAllGraphsWithDifferentRingSets(
                                                         DENOPTIMGraph molGraph)
                                                        throws DENOPTIMException
    {
        ArrayList<DENOPTIMGraph> lstGraphs = new ArrayList<>();

        boolean rcnEnabled = FragmentSpaceParameters.useAPclassBasedApproach();
        if (!rcnEnabled)
            return lstGraphs;

        boolean evaluateRings = RingClosureParameters.allowRingClosures();
        if (!evaluateRings)
            return lstGraphs;

        // get a atoms/bonds molecular representation (no 3D needed)
        IAtomContainer mol = GraphConversionTool.convertGraphToMolecule(molGraph,false);

        // Set rotatability property as property of IBond
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
                               cgh.getPossibleCombinationOfRings(mol, molGraph);

        // Keep closable chains that are relevant for chelate formation
        if (RingClosureParameters.buildChelatesMode())
        {
            ArrayList<Set<DENOPTIMRing>> toRemove = new ArrayList<>();
            for (Set<DENOPTIMRing> setRings : allCombsOfRings)
            {
                if (!cgh.checkChelatesGraph(molGraph,setRings))
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
            DENOPTIMGraph newGraph = (DENOPTIMGraph) DenoptimIO.deepCopy(
                                                                      molGraph);
            HashMap<Integer,Integer> vRenum = renumberVerticesGetMap(newGraph);
            newGraph.setGraphId(getUniqueGraphIndex());

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
     * @param molGraph the Graph representation of the molecule
     * @return <code>true</code> if a forbidden end is found
     */

    public static boolean foundForbiddenEnd(DENOPTIMGraph molGraph)
    {
        ArrayList<DENOPTIMVertex> vertices = molGraph.getVertexList();
        ArrayList<String> classOfForbEnds = FragmentSpace.getForbiddenEndList();
        boolean found = false;
        for (DENOPTIMVertex vtx : vertices)
        {
            ArrayList<DENOPTIMAttachmentPoint> daps = vtx.getAttachmentPoints();
            for (DENOPTIMAttachmentPoint dp : daps)
            {
                if (dp.isAvailable())
                {
                    String apClass = dp.getAPClass();
                    if (classOfForbEnds.contains(apClass))
                    {
                        found = true;
                        String msg = "Forbidden free AP for Vertex: "
                            + vtx.getVertexId()
                            + " MolId: " + (vtx.getMolId() + 1)
                            + " Ftype: " + vtx.getFragmentType()
                            + "\n"+ molGraph+" \n "
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
     * Collect the list of closable paths involving the given scaffold
     * @param scaf
     * @param libraryOfRCCs
     * @return 
     * @throws exception.DENOPTIMException 
     */

//TODO del if not used. Should be replaced by a more efficient approach
// using the map of ClosableChains per fragment MolID/type

    public static ArrayList<ClosableChain> getClosableVertexChainsFromDB(
                        DENOPTIMVertex scaf,
                        HashMap<String,ArrayList<String>> libraryOfRCCs)
                                                throws DENOPTIMException
//                String rccIndexFile,
//                String rccRootFolder) throws DENOPTIMException
    {
        ArrayList<ClosableChain> clbChains = new ArrayList<>();

        for (String chainId : libraryOfRCCs.keySet())
        {
            String closability = libraryOfRCCs.get(chainId).get(1);
            if (closability.equals("T"))
            {
                ClosableChain cc = new ClosableChain(chainId);
                int pos = cc.involvesVertex(scaf);
                if (pos != -1)
                {
                    clbChains.add(cc);
                }
            }
        }


/*
TODO del if not useful
        //TODO this code is taken mostry from UpdateUID class. need a toolbox
        //somewhere in DENOPTIM to do the task of accessing a file with lock

        // Later a synchronized method has been introduced: evaluate whether
        // to use the lock or the synchronized approach.

        File file1 = new File(rccIndexFile);
        RandomAccessFile rafile1 = null;
        FileChannel channel1 = null;
        FileLock lock1 = null;
        try
        {
            rafile1 = new RandomAccessFile(file1, "rw");
            channel1 = rafile1.getChannel();

            int nTry = 0;
            while (true)
            {
                try
                {
                    lock1 = channel1.tryLock();
                    if (lock1 != null)
                        break;
//TODO: maybe something missing here: check
                }
                catch (OverlappingFileLockException e)
                {
                    nTry++;
                }
            }

            // read file
            for (String line; (line = rafile1.readLine()) != null;)
            {
                if (line.trim().length() == 0)
                    continue;

                String[] parts = line.trim().split("\\s+");
                String cID = parts[0];
                int rccID = Integer.parseInt(parts[1]);
                String closability = parts[2];
                if (closability.equals("T"))
                {
                    ClosableChain cc = new ClosableChain(line);
                    int pos = cc.involvesVertex(scaf);
                    if (pos != -1)
                    {
                        clbChains.add(cc);
                    }
                }
            }
        }
        catch (Throwable t)
        {
             throw new DENOPTIMException(t);
        }
        finally
        {
            try
            {
                if (channel1 != null)
                    channel1.close();
                if (lock1 != null && lock1.isValid())
                    lock1.release();
            }
            catch (Throwable t)
            {
                t.printStackTrace();
                throw new DENOPTIMException("GraphUtils is unable to "
                        + " unlock file '" + rccIndexFile + "'. " + t);
            }
        }
*/

//TODO del
System.out.println("DB: Found " + clbChains.size() + " for vertex " + scaf); 

        return clbChains;
    }

//------------------------------------------------------------------------------    

    /**
     * Compare two DENOPTIMGraphs. It returns true only if the two graphs, 
     * which can be either equal or different spanning trees, project the same 
     * arrangement of connected verteces. Note that the connections
     * may be DENOPTIMEdges or cyclic bonds that result only from the 
     * definition of DENOPTIMRings.
     * TODO: this first implementation is to be further tested and get mature.
     * @param gA the first graph
     * @param gB the second graph
     * @return <code>true</code> if the two graphs define the same arrangement 
     * of verteces and connections.
     * @throws exception.DENOPTIMException
     */

     public static boolean equivalentGraphs(DENOPTIMGraph gA, DENOPTIMGraph gB)
                                                        throws DENOPTIMException
     {
        boolean result = false;

        // fast comparison of number of verteces and rings
        if ((gA.getRingCount() != gB.getRingCount()) ||
            (gA.getVertexCount() != gB.getVertexCount()))
        {
            return false;
        }

//TODO del
System.out.println("Comparing these two graphs: ");
System.out.println(gA);
System.out.println(" ");
System.out.println(gB);
GenUtils.pause();

        ArrayList<Integer> visitedA = new ArrayList<Integer>();
        ArrayList<Integer> visitedB = new ArrayList<Integer>();
        result = exploreGraphs(gA, gB, 0, 0, -1, -1, visitedA, visitedB, 
                                                                false, false);

//TODO del
System.out.println("End og equivalentGraphs: "+result);
System.out.println("VisitedA: "+visitedA);
System.out.println("VisitedB: "+visitedB);
GenUtils.pause();

        return result;
     }

//------------------------------------------------------------------------------

    /**
     * This method evaluates equality of two given verteces each belonging to
     * a given graph. If verteces are found to be equal the method moves
     * (recursively) to one of the neighbour verteces connected by edge or
     * by cyclic bond (that is, DENOPTIMRing).
     * @param gA the first graph (A)
     * @param gB the second graph (B)
     * @param cvA index current vertex on graph A
     * @param cvB index current vertex on graph B
     * @param ceA index corrent edge on A
     * @param ceB index corrent edge on B
     * @param visitedA the list of visited IDs on A
     * @param visitedB the list of visited IDs on B
     * @param backwardA true if the direction of the exploration on A is
     * towards the scaffold
     * @param backwardB true if the direction of the exploration on B is
     * towards the scaffold
     * @return <code>true</code> if the current pair of verteces and the whole
     * branches starting from them, descibe the same arrangement of verteces.
     */

     private static boolean exploreGraphs(DENOPTIMGraph gA, 
                                   DENOPTIMGraph gB,
                                   int cvA,
                                   int cvB,
                                   int ceA,
                                   int ceB,
                                   ArrayList<Integer> visitedA,
                                   ArrayList<Integer> visitedB,
                                   boolean backwardA,
                                   boolean backwardB)
                                                throws DENOPTIMException        
    {
        boolean areEqual = false;

//TODO del
System.out.println("Beginning of 'exploreGraphs'------------------------");

        DENOPTIMVertex vA = gA.getVertexAtPosition(cvA);
        DENOPTIMVertex vB = gB.getVertexAtPosition(cvB);

//TODO del
System.out.println("VA: "+vA+" vB: "+vB);
System.out.println("VisitedA: "+visitedA);
System.out.println("VisitedB: "+visitedB);
System.out.println("backwardA: "+backwardA);
System.out.println("backwardB: "+backwardB);

        ArrayList<Integer> addedToVisitedA = new ArrayList<Integer>();
        ArrayList<Integer> addedToVisitedB = new ArrayList<Integer>();

        // Check vA as ring closing attractor (RCA) vertex
        ArrayList<DENOPTIMRing> ringsOnVA = new ArrayList<DENOPTIMRing>();
        for (DENOPTIMRing r : gA.getRings())
        {
            if (vA == r.getHeadVertex() || vA == r.getTailVertex())
            {
                ringsOnVA.add(r);
            }
        }
        if (ringsOnVA.size() > 1)
        {
            throw new DENOPTIMException("Unecpected vertex being head/tail of "
                                        + "more than one DENOPTIMRing. Check "
                                        + "graph: " + gA);
        }
        // Check vB as ring closing attractor (RCA) vertex
        ArrayList<DENOPTIMRing> ringsOnVB = new ArrayList<>();
        for (DENOPTIMRing r : gB.getRings())
        {
            if (vB == r.getHeadVertex() || vB == r.getTailVertex())
            {
                ringsOnVB.add(r);
            }
        }
        if (ringsOnVB.size() > 1)
        {
            throw new DENOPTIMException("Unecpected vertex being head/tail of "
                                        + "more than one DENOPTIMRing. Check "
                                        + "graph: " + gB);
        }

        // In case of RCA vertex move to the next non-RCA vertex in the ring
        int apVAToBack = -1;
        boolean nextDirOnA = backwardA;
        if (ringsOnVA.size() == 1)
        {
            DENOPTIMRing r = ringsOnVA.get(0);
//TODO del
System.out.println("vA is head/tail of ring "+vA+" "+r);
            if (vA == r.getHeadVertex())
            {
                visitedA.add(vA.getVertexId());
                addedToVisitedA.add(vA.getVertexId());
                visitedA.add(r.getTailVertex().getVertexId());
                addedToVisitedA.add(r.getTailVertex().getVertexId());
                vA = r.getVertexAtPosition(r.getSize() - 2);
                ceA = gA.getIndexOfEdgeWithParent(vA.getVertexId());
                apVAToBack = gA.getEdgeAtPosition(ceA).getTargetDAP();
            }
            else if (vA == r.getTailVertex())
            {
                visitedA.add(vA.getVertexId());
                addedToVisitedA.add(vA.getVertexId());
                visitedA.add(r.getHeadVertex().getVertexId());
                addedToVisitedA.add(r.getHeadVertex().getVertexId());
                vA = r.getVertexAtPosition(1);
                ceA = gA.getIndexOfEdgeWithParent(
                                        r.getHeadVertex().getVertexId());
                apVAToBack = gA.getEdgeAtPosition(ceA).getSourceDAP();
            }
            nextDirOnA = true;
//TODO del
System.out.println("vA: "+vA);
System.out.println("edge used: "+gA.getEdgeAtPosition(ceA));
System.out.println("nextDirOnA: "+nextDirOnA);
System.out.println("apVAToBack: "+apVAToBack);

        }
        else
        {
            if (ceA >= 0)
            {
                if (backwardA)
                {
                    apVAToBack = gA.getEdgeAtPosition(ceA).getSourceDAP();
                }
                else
                {
                    apVAToBack = gA.getEdgeAtPosition(ceA).getTargetDAP();
                }
            }
        }
        // In case of RCA vertex move to the next non-RCA vertex in the ring
        int apVBToBack = -1;
        boolean nextDirOnB = backwardB;
        if (ringsOnVB.size() == 1)
        {
            DENOPTIMRing r = ringsOnVB.get(0);
//TODO del
System.out.println("vB is head/tail of ring "+vB+" "+r);
            if (vB == r.getHeadVertex())
            {
                visitedB.add(vB.getVertexId());
                addedToVisitedB.add(vB.getVertexId());
                visitedB.add(r.getTailVertex().getVertexId());
                addedToVisitedB.add(r.getTailVertex().getVertexId());
                vB = r.getVertexAtPosition(r.getSize() - 2);
                ceB = gB.getIndexOfEdgeWithParent(vB.getVertexId());
                apVBToBack = gB.getEdgeAtPosition(ceB).getTargetDAP();
            }
            else if (vB == r.getTailVertex())
            {
                visitedB.add(vB.getVertexId());
                addedToVisitedB.add(vB.getVertexId());
                visitedB.add(r.getHeadVertex().getVertexId());
                addedToVisitedB.add(r.getHeadVertex().getVertexId());
                vB = r.getVertexAtPosition(1);
                ceB = gB.getIndexOfEdgeWithParent(
                                        r.getHeadVertex().getVertexId());
                apVBToBack = gB.getEdgeAtPosition(ceB).getSourceDAP();
            }
            nextDirOnB = true;
//TODO del
System.out.println("vB: "+vB);
System.out.println("edge used: "+gB.getEdgeAtPosition(ceB));
System.out.println("nextDirOnB: "+nextDirOnB);
System.out.println("apVBToBack: "+apVBToBack);
        }
        else
        {
            if (ceB >= 0)
            {
                if (backwardB)
                {
                    apVBToBack = gB.getEdgeAtPosition(ceB).getSourceDAP();
                }
                else
                {
                    apVBToBack = gB.getEdgeAtPosition(ceB).getTargetDAP();
                }
            }
        }

        // check if already visited
        if (visitedA.contains(vA.getVertexId()) && 
            visitedB.contains(vB.getVertexId()))
        {
//TODO del
System.out.println("returning true-1: "+visitedA.contains(vA.getVertexId())+" "+visitedB.contains(vB.getVertexId()));
            return true;
        }
        else if ((visitedA.contains(vA.getVertexId()) &&
                   !visitedB.contains(vB.getVertexId())) ||
                 (!visitedA.contains(vA.getVertexId()) &&
                   visitedB.contains(vB.getVertexId())))
        {
//TODO del
System.out.println("returning false-1: "+visitedA.contains(vA.getVertexId())+" "+visitedB.contains(vB.getVertexId()));
            return false;
        }

        // add to list of visited
        visitedA.add(vA.getVertexId());
        addedToVisitedA.add(vA.getVertexId());
        visitedB.add(vB.getVertexId());
        addedToVisitedB.add(vB.getVertexId());

        // Compare the current fragments
//TODO del
System.out.println("Comparing type:  "+vA.getFragmentType()+" "+vB.getFragmentType());
System.out.println("Comparing molID: "+vA.getMolId()+" "+vB.getMolId());
System.out.println("Comparing apID:  "+apVAToBack+" "+apVBToBack);
        boolean sameFragTyp = (vA.getFragmentType() == vB.getFragmentType());
        boolean sameGrafID = (vA.getMolId() == vB.getMolId());
        boolean sameAPtoHere = false;
        if (apVAToBack<0 && apVBToBack<0)
        {
            // There is no edge before the scaffold
            sameAPtoHere = true;
        }
        else
        {
            sameAPtoHere = (apVAToBack == apVBToBack);
        }

        if (sameFragTyp && sameGrafID && sameAPtoHere)
        {
//TODO del
System.out.println("SAME");
            // move to next level
            boolean inner = true;
            for (int nextAp=0; nextAp<vA.getAttachmentPoints().size(); nextAp++)
            {
                if (nextAp == apVAToBack)
                {
                    continue;
                }

                int nextVrtxAID = -1;
                int nextVrtxBID = -1;
                int nextEdgeAID = -1;
                int nextEdgeBID = -1;

                boolean branchAIsOver = true;
                boolean branchBIsOver = true;

                if (nextDirOnA)
                {
                    nextEdgeAID = gA.getIndexOfEdgeWithParent(vA.getVertexId());
                    DENOPTIMEdge e = gA.getEdgeAtPosition(nextEdgeAID);
                    nextVrtxAID = gA.getIndexOfVertex(e.getSourceVertex());
                    int nextVrtxAVrtxID = gA.getVertexAtPosition(nextVrtxAID).getVertexId();
                    if (visitedA.contains(nextVrtxAVrtxID))
                    {
                        nextDirOnA = false;
                    }
                    else
                    {
                        branchAIsOver = false;
//TODO del
System.out.println("Edge-A1: "+e);
                    }

                }
                if (!nextDirOnA)
                {
                    for (int ie=0; ie<gA.getEdgeCount(); ie++)
                    {
                        DENOPTIMEdge e = gA.getEdgeAtPosition(ie);
                        if (e.getSourceVertex() == vA.getVertexId() &&
                            e.getSourceDAP() == nextAp)
                        {
                            nextEdgeAID = ie;
                            nextVrtxAID =  gA.getIndexOfVertex(
                                                        e.getTargetVertex());
//TODO del
System.out.println("Edge-A2: "+e);
                            branchAIsOver = false;
                            break;
                        }
                    }
                }

                if (nextDirOnB)
                {
                    nextEdgeBID = gB.getIndexOfEdgeWithParent(vB.getVertexId());
                    DENOPTIMEdge e = gB.getEdgeAtPosition(nextEdgeBID);
                    nextVrtxBID = gB.getIndexOfVertex(e.getSourceVertex());
                    int nextVrtxBVrtxID = gB.getVertexAtPosition(nextVrtxBID).getVertexId();
                    if (visitedB.contains(nextVrtxBVrtxID))
                    {
                        nextDirOnB = false;
                    }
                    else
                    {
                        branchBIsOver = false;
//TODO del
System.out.println("Edge-B1: "+e);
                    }
                }
                if (!nextDirOnB)
                {
                    for (int ie=0; ie<gB.getEdgeCount(); ie++)
                    {
                        DENOPTIMEdge e = gB.getEdgeAtPosition(ie);
                        if (e.getSourceVertex() == vB.getVertexId() &&
                            e.getSourceDAP() == nextAp)
                        {
                            nextEdgeBID = ie;
                            nextVrtxBID = gB.getIndexOfVertex(
                                                        e.getTargetVertex());
//TODO del
System.out.println("Edge-B2: "+e);
                            branchBIsOver = false;
                            break;
                        }
                    }
                }

                if (branchAIsOver && branchBIsOver)
                {
//TODO del
System.out.println("Branches are over");
                    return true;
                }
                else if ((branchAIsOver && !branchBIsOver) ||
                         (!branchAIsOver && branchBIsOver))
                {
//TODO del
System.out.println("Only one branch is over");
                    return false;
                }

//TODO del
System.out.println("Next AP: "+nextAp+" of vertx. "+vA);
System.out.println("nextVrtxAID: "+nextVrtxAID+" "+nextEdgeAID);
System.out.println("nextVrtxBID: "+nextVrtxBID+" "+nextEdgeBID);

                inner = exploreGraphs(gA, gB, 
                                      nextVrtxAID, nextVrtxBID,
                                      nextEdgeAID, nextEdgeBID,
                                      visitedA, visitedB,
                                      nextDirOnA, nextDirOnB);

                if (!inner)
                {
                    break;
                }                
            }
            if (inner)
            {
                areEqual = true;
            }
        }
        else
        {
//TODO del
System.out.println("DIFFERENT");
        }

        if (!areEqual)
        {
            // Reset flag of visited
            visitedA.removeAll(addedToVisitedA);
            visitedB.removeAll(addedToVisitedB);
        }


//TODO del
System.out.println("End of 'exploreGraphs': "+areEqual+"----------------------------");

        
        return areEqual;
    }

//------------------------------------------------------------------------------
    
    /**
     * connects 2 vertices based on their free AP connections
     * @param a vertex
     * @param b vertex
     * @return edge connecting the vertices
     */

    public static DENOPTIMEdge connectVertices(DENOPTIMVertex a,
                                                    DENOPTIMVertex b)
    {
        ArrayList<Integer> apA = a.getFreeAPList();
        ArrayList<Integer> apB = b.getFreeAPList();

        if (apA.isEmpty() || apB.isEmpty())
            return null;

        // select random APs - these are the indices in the list
        MersenneTwister rng = RandomUtils.getRNG();
        
        
        //int iA = apA.get(GAParameters.getRNG().nextInt(apA.size()));
        //int iB = apB.get(GAParameters.getRNG().nextInt(apB.size()));
        int iA = apA.get(rng.nextInt(apA.size()));
        int iB = apB.get(rng.nextInt(apB.size()));

        DENOPTIMAttachmentPoint dap_A = a.getAttachmentPoints().get(iA);
        DENOPTIMAttachmentPoint dap_B = b.getAttachmentPoints().get(iB);

        // if no reaction/class specific info available set to single bond
        int bndOrder = 1;

        // create a new edge
        DENOPTIMEdge edge = new DENOPTIMEdge(a.getVertexId(), b.getVertexId(),
                                                     iA, iB, bndOrder);

        // update the attachment point info
        dap_A.updateAPConnections(-bndOrder); // decrement the connections
        dap_B.updateAPConnections(-bndOrder); // decrement the connections

        return edge;
    }

//------------------------------------------------------------------------------

    /**
     * Attaches the specified fragment to the vertex using the specified pair
     * of AP.
     * @param molGraph
     * @param curVertex the vertex to which the fragment is to be attached
     * @param srcAPIdx index of the AP at which the fragment is to be attached
     * @param fId the fragment id in the library
     * @param fTyp the type of fragment (0: scaffold, 1: fragment, 2: cap)
     * @param trgAPIdx index of the AP on the incoming fragment
     * @return the id of the new vertex created
     * @throws DENOPTIMException
     */

    public static int attachNewFragmentAtAPWithAP (DENOPTIMGraph molGraph, 
                                                      DENOPTIMVertex curVertex,
                                                      int srcAPIdx, 
                                                      int fId, 
                                                      int fTyp,
                                                      int trgAPIdx) 
                                                      throws DENOPTIMException
    {
//TODO del
if(debug)
{
    System.out.println("Attempt to attach: molID:"+fId+" fTyp:"+fTyp+" apId:"+trgAPIdx);
    System.out.println("On src AP: "+srcAPIdx+" of vertex "+curVertex);
}
        // create the new DENOPTIMVertex
        int lvl = curVertex.getLevel();
        int nvid = GraphUtils.getUniqueVertexIndex();
        ArrayList<DENOPTIMAttachmentPoint> fragAPs =
                                 FragmentUtils.getAPForFragment(fId, fTyp);

//TODO del
if(debug)
    System.out.println("fragAPs: "+fragAPs);

        DENOPTIMVertex fragVertex = new DENOPTIMVertex(nvid,fId,fragAPs,fTyp);

        // update the level of the vertex based on its parent
        fragVertex.setLevel(lvl+1);

        // get molecular representation
        IAtomContainer mol = FragmentSpace.getFragmentLibrary().get(fId);

        // identify the symmetric APs if any for this fragment vertex
        ArrayList<SymmetricSet> simAP = FragmentUtils.getMatchingAP(mol, fragAPs);
        fragVertex.setSymmetricAP(simAP);

        // identify the src AP (on the current vertex)
        ArrayList<DENOPTIMAttachmentPoint> curAPs =
                                                curVertex.getAttachmentPoints();
        DENOPTIMAttachmentPoint srcAP = curAPs.get(srcAPIdx);
        String srcAPCls = srcAP.getAPClass();

//TODO
if(debug)
    System.out.println("trgAPIdx: "+trgAPIdx);

        // identify the target AP (on the appended verex)
        DENOPTIMAttachmentPoint trgAP = fragAPs.get(trgAPIdx);

//TODO del
if(debug)
    System.out.println("trgAP: "+trgAP);
        String trgAPCls = trgAP.getAPClass();

        // create the new DENOPTIMEdge
        DENOPTIMEdge edge;
        edge = connectVertices(curVertex, fragVertex, srcAPIdx, trgAPIdx, 
                                                            srcAPCls, trgAPCls);

        if (edge != null)
        {
            // update graph
            molGraph.addVertex(fragVertex);
            molGraph.addEdge(edge);

            return fragVertex.getVertexId();
        }

        return -1;
    }

//------------------------------------------------------------------------------

    /**
     * Append a graph (incoming=I) onto another graph (receiving=R).
     * This method ignores symmetry.
     * @param molGraph the receiving graph R, or parent
     * @param parentVrtx the source vertix of R on which I will be attached
     * @param parentAPIdx the attachment point on R's verticx to be
     * used to attach I
     * @param subGraph the incoming graph I, or child
     * @param childVrtx the vertex of I that is to be connected to R
     * @param childAPIdx the index of the atachment point on the vertex of I
     * that is to be connected to R
     * @param bndType the bond type between R and I
     */

    public static void appendGraphOnGraph(DENOPTIMGraph molGraph,
                                     DENOPTIMVertex parentVrtx,
                                     int parentAPIdx,
                                     DENOPTIMGraph subGraph,
                                     DENOPTIMVertex childVrtx,
                                     int childAPIdx,
                                     int bndType)
                                                        throws DENOPTIMException
    {
        appendGraphOnGraph(molGraph, parentVrtx, parentAPIdx,
                                       subGraph, childVrtx, childAPIdx, bndType,
                                    new HashMap<Integer,SymmetricSet>(), false);
    }

//------------------------------------------------------------------------------

    /**
     * Append a graph (incoming=I) onto another graph (receiving=R).
     * This method ignores symmetry.
     * @param molGraph the receiving graph R, or parent
     * @param parentVrtx the source vertix of R on which I will be attached
     * @param parentAPIdx the attachment point on R's verticx to be
     * used to attach I
     * @param subGraph the incoming graph I, or child
     * @param childVrtx the vertex of I that is to be connected to R
     * @param childAPIdx the index of the atachment point on the vertex of I
     * that is to be connected to R
     * @param bndType the bond type between R and I
     * @param symmetry use <code>true</code> to reproduce the same operation
     * onto symmetric parters and generate the relative symmetric sets.
     */

    public static void appendGraphOnGraph(DENOPTIMGraph molGraph,
                                     DENOPTIMVertex parentVrtx,
                                     int parentAPIdx,
                                     DENOPTIMGraph subGraph,
                                     DENOPTIMVertex childVrtx,
                                     int childAPIdx,
                                     int bndType,
                                     boolean symmetry)
                                                        throws DENOPTIMException
    {
        appendGraphOnGraph(molGraph, parentVrtx, parentAPIdx,
                                       subGraph, childVrtx, childAPIdx, bndType,
                                 new HashMap<Integer,SymmetricSet>(), symmetry);
    }
 
//------------------------------------------------------------------------------

    /**
     * Append a graph (incoming=I) onto another graph (receiving=R). 
     * Can append one or more copyes of the same graph. The corresponding
     * vertex and attachment point ID for each connection are given in
     * separated arrays.
     * @param molGraph the receiving graph R, or parent
     * @param parentVrtxs the list of source vertices of R on which a copy
     * of I is to be attached.
     * @param parentAPIdx the list of attachment points on R's vertices to be
     * used to attach I 
     * @param subGraph the incoming graph I, or child
     * @param childVrtx the vertex of I that is to be connected to R
     * @param childAPIdx the index of the atachment point on the vertex of I 
     * that is to be connected to R
     * @param bndType the bond type between R and I
     * @param onAllSymmAPs set to <code>true</code> to require the same graph I
     * to be attached on all available and symmetric APs on the same vertex of 
     * the AP indicated in the list.
     */
    
    public static void appendGraphOnGraph(DENOPTIMGraph molGraph,
                                     ArrayList<DENOPTIMVertex> parentVrtxs,
                                     ArrayList<Integer> parentAPIdx,
                                     DENOPTIMGraph subGraph,
                                     DENOPTIMVertex childVrtx,
                                     int childAPIdx,
                                     int bndType,
                                     boolean onAllSymmAPs)
                                                        throws DENOPTIMException
    {
        // Collector for symmetries created by appending copyes of subGraph
        Map<Integer,SymmetricSet> newSymSets = 
                                            new HashMap<Integer,SymmetricSet>();

        // Repeat append for each parent vertex while collecting symmetries
        for (int i=0; i<parentVrtxs.size(); i++)
        {
            appendGraphOnGraph(molGraph, parentVrtxs.get(i), parentAPIdx.get(i),
                                       subGraph, childVrtx, childAPIdx, bndType,
                                                     newSymSets, onAllSymmAPs);
        }
    }

//------------------------------------------------------------------------------
    
    /**
     * Append a graph (incoming=I) onto another graph (receiving=R). 
     * @param molGraph the receiving graph R, or parent
     * @param parentVrtxs the vertex of R on which the a copy
     * of I is to be attached.
     * @param parentAPIdx the attachment point on R's vertex to be
     * used to attach I 
     * @param subGraph the incoming graph I, or child
     * @param childVrtx the vertex of I that is to be connected to R
     * @param childAPIdx the index of the attachment point on the vertex of I 
     * that is to be connected to R
     * @param bndType the bond type between R and I
     * @param newSymSets this parameter is only used to keep track
     * of the symmetric copies of I. Simply provide an empty data structure.
     * @param onAllSymmAPs set to <code>true</code> to require the same graph I
     * to be attached on all available and symmetric APs on the same vertex of 
     * the AP indicated in the list.
     */
    
    public static void appendGraphOnGraph(DENOPTIMGraph molGraph,
                                     DENOPTIMVertex parentVrtx,
                                     int parentAPIdx,
                                     DENOPTIMGraph subGraph,
                                     DENOPTIMVertex childVrtx,
                                     int childAPIdx,
                                     int bndType,
                                     Map<Integer,SymmetricSet> newSymSets,
                                     boolean onAllSymmAPs)
                                                        throws DENOPTIMException
    {
        SymmetricSet symAPs = parentVrtx.getPartners(parentAPIdx);
        if (symAPs != null && onAllSymmAPs)
        {
            ArrayList<Integer> apLst = symAPs.getList();
            for (int i=0; i<apLst.size(); i++)
            {
                int idx = apLst.get(i);
                if (!parentVrtx.getAttachmentPoints().get(idx).isAvailable())
                {
                    continue;
                }
                appendGraphOnAP(molGraph, parentVrtx, idx,
                         subGraph, childVrtx, childAPIdx, bndType, newSymSets);
            }
        }
        else
        {
            appendGraphOnAP(molGraph, parentVrtx, parentAPIdx,
                         subGraph, childVrtx, childAPIdx, bndType, newSymSets);
        }
    }

//------------------------------------------------------------------------------
    
    /**
     * Append a subgraph (subGraph) to an existing graph (molGraph) specifying 
     * which vertex and attachment point to use for the connection.
     * Does not project on symmetrically related verteces or
     * attachment points. No change in symmetric sets, apart from importing 
     * those already defined in the subgraph.
     * @param molGraph the receiving graph R, or parent
     * @param parentVrtxs the vertex of R on which the a copy
     * of I is to be attached.
     * @param parentAPIdx the attachment point on R's vertex to be
     * used to attach I 
     * @param subGraph the incoming graph I, or child
     * @param childVrtx the vertex of I that is to be connected to R
     * @param childAPIdx the index of the attachment point on the vertex of I 
     * that is to be connected to R
     * @param bndType the bond type between R and I
     * @param map of symmetric sets. This parameter is only used to keep track
     * of the symmetric copies of I. Simply provide an empty data structure.
     */

    public static void appendGraphOnAP(DENOPTIMGraph molGraph, 
                                        DENOPTIMVertex parentVrtx, 
                                        int parentAPIdx, 
                                        DENOPTIMGraph subGraph, 
                                        DENOPTIMVertex childVrtx, 
                                        int childAPIdx, 
                                        int bndType, 
                                        Map<Integer,SymmetricSet> newSymSets)
                                                    throws DENOPTIMException
    {
        // Clone and renumber the subgraph to ensure uniqueness
        DENOPTIMGraph sgClone = (DENOPTIMGraph) DenoptimIO.deepCopy(subGraph);
        GraphUtils.renumberGraphVertices(sgClone);

        // Make the connection between molGraph and subGraph
        DENOPTIMVertex cvClone = sgClone.getVertexAtPosition(
                            subGraph.getIndexOfVertex(childVrtx.getVertexId()));
        DENOPTIMAttachmentPoint dap_Parent =
                              parentVrtx.getAttachmentPoints().get(parentAPIdx);
        DENOPTIMAttachmentPoint dap_Child =
                                  cvClone.getAttachmentPoints().get(childAPIdx);
        DENOPTIMEdge edge = null;
        if (FragmentSpaceParameters.useAPclassBasedApproach())
        {
            String rcnP = dap_Parent.getAPClass();
            String rcnC = dap_Child.getAPClass();
            edge = connectVertices(parentVrtx, cvClone, parentAPIdx, childAPIdx,
                                                                    rcnP, rcnC);
        }
        else
        {
            edge = new DENOPTIMEdge(parentVrtx.getVertexId(), 
                       cvClone.getVertexId(), parentAPIdx, childAPIdx, bndType);
            // decrement the num. of available connections
            dap_Parent.updateAPConnections(-bndType); 
            dap_Child.updateAPConnections(-bndType); 
        }
        if (edge == null)
        {
            String msg = "Program Bug in appendGraphOnAP: No edge created.";
            DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
            throw new DENOPTIMException(msg);
        }
        molGraph.addEdge(edge);

        // Import all vertices from cloned subgraph
        for (int i=0; i<sgClone.getVertexList().size(); i++)
        {
            DENOPTIMVertex clonV = sgClone.getVertexList().get(i);
            DENOPTIMVertex origV = subGraph.getVertexList().get(i);

            molGraph.addVertex(sgClone.getVertexList().get(i));

            // also need to tmp store pointers to symmetriic verteces
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
                                 sgClone.getVertexList().get(i).getVertexId()));
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
            molGraph.addEdge(sgClone.getEdgeList().get(i));
        }
        // Import all rings from cloned subgraph
        for (int i=0; i<sgClone.getRings().size(); i++)
        {
            molGraph.addRing(sgClone.getRings().get(i));
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
            Iterator<SymmetricSet> iter = molGraph.getSymSetsIterator();
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
                    // tmpSS has always at leas one entry: the initial vrtId
                    continue;
                }
                //Move tmpSS into a new SS on molGraph
                SymmetricSet newSS = new SymmetricSet();
                for (Integer symVrtID : tmpSS.getList())
                {
                    newSS.add(symVrtID);
                }
                molGraph.addSymmetricSetOfVertices(newSS);
            }
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Search a graph for vertices that match the criteria defined in a query
     * vertex.
     * @param graph the graph to be searched
     * @param query the query
     * @param onlyOneSymm if <code>true</code> then only one match will be 
     * collected for each symmetric set of partners.
     * @param verbosity the verbosity level
     * @return the list of matches
     */
    public static ArrayList<Integer> findVerticesIds(DENOPTIMGraph graph, 
                  DENOPTIMVertexQuery query, boolean onlyOneSymm, int verbosity)
    {
        ArrayList<Integer> matches = new ArrayList<Integer>();
        for (DENOPTIMVertex v : findVertices(graph,query,onlyOneSymm,verbosity))
        {
            matches.add(v.getVertexId());
        }
        return matches;
    }

//-----------------------------------------------------------------------------

    /**
     * Filters a list of vertices according to a query.
     * vertex.
     * @param graph the graph to be searched
     * @param dnQuery the query
     * @param onlyOneSymm if <code>true</code> then only one match will be 
     * collected for each symmetric set of partners.
     * @param verbosity the verbosity level
     * @return the list of mathced vertices
     */

    public static ArrayList<DENOPTIMVertex> findVertices(DENOPTIMGraph graph,
                DENOPTIMVertexQuery dnQuery, boolean onlyOneSymm, int verbosity)
    {
        DENOPTIMVertex vQuery = dnQuery.getVrtxQuery();
        DENOPTIMEdge eInQuery = dnQuery.getInEdgeQuery();
        DENOPTIMEdge eOutQuery = dnQuery.getOutEdgeQuery();
        
        ArrayList<DENOPTIMVertex> matches = new ArrayList<DENOPTIMVertex>();
        matches.addAll(graph.getVertexList());

        if (verbosity > 1)
        {
            System.out.println("Searching vertices - candidates: " + matches);
        }

        //Check condition vertex ID
        int query = vQuery.getVertexId();
        if (query > -1) //-1 would be the wildcard
        {
            ArrayList<DENOPTIMVertex> newLst = new ArrayList<DENOPTIMVertex>();
            for (DENOPTIMVertex v : matches)
            {
                if (v.getVertexId() == query)
                {
                    newLst.add(v);
                }
            }
            matches = newLst;
        }

        if (verbosity > 2)
        {
            System.out.println("After ID-based rule: " + matches);
        }

        //Check condition fragment ID
        query = vQuery.getMolId();
        if (query > -1) //-1 would be the wildcard
        {
            ArrayList<DENOPTIMVertex> newLst = new ArrayList<DENOPTIMVertex>();
            for (DENOPTIMVertex v : matches)
            {
                if (v.getMolId() == query)
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
        query = vQuery.getFragmentType();
        if (query > -1) //-1 would be the wildcard
        {
            ArrayList<DENOPTIMVertex> newLst = new ArrayList<DENOPTIMVertex>();
            for (DENOPTIMVertex v : matches)
            {
                if (v.getFragmentType() == query)
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

        //Check condition level of vertex
        query = vQuery.getLevel();
        if (query > -2) //-2 would be the wildcard
        {
            ArrayList<DENOPTIMVertex> newLst = new ArrayList<DENOPTIMVertex>();
            for (DENOPTIMVertex v : matches)
            {
                if (v.getLevel() == query)
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
            query = eInQuery.getTargetDAP();
            if (query > -1)
            {
                ArrayList<DENOPTIMVertex> newLst = 
                                               new ArrayList<DENOPTIMVertex>();
                for (DENOPTIMVertex v : matches)
                {
		    if (graph.getIndexOfEdgeWithParent(v.getVertexId()) < 0)
		    {
			continue;
		    }
                    DENOPTIMEdge e = graph.getEdgeWithParent(v.getVertexId());
                    if (e!=null && e.getTargetDAP() == query)
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
            query = eInQuery.getBondType();
            if (query > -1)
            {
                ArrayList<DENOPTIMVertex> newLst = 
                                               new ArrayList<DENOPTIMVertex>();
                for (DENOPTIMVertex v : matches)
                {
                    if (graph.getIndexOfEdgeWithParent(v.getVertexId()) < 0)
                    {
                        continue;
                    }
                    DENOPTIMEdge e = graph.getEdgeWithParent(v.getVertexId());
                    if (e!=null && e.getBondType() == query)
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
            String squery = eInQuery.getTargetReaction();
            if (!squery.equals("*"))
            {
                ArrayList<DENOPTIMVertex> newLst = 
                                               new ArrayList<DENOPTIMVertex>();
                for (DENOPTIMVertex v : matches)
                {
                    if (graph.getIndexOfEdgeWithParent(v.getVertexId()) < 0)
                    {
                        continue;
                    }
                    DENOPTIMEdge e = graph.getEdgeWithParent(v.getVertexId());
                    if (e!=null && e.getTargetReaction().equals(squery))
                    {
                        newLst.add(v);
                    }
                }
                matches = newLst;
            }
            squery = eInQuery.getSourceReaction();
            if (!squery.equals("*"))
            {
                ArrayList<DENOPTIMVertex> newLst =
                                               new ArrayList<DENOPTIMVertex>();
                for (DENOPTIMVertex v : matches)
                {
                    if (graph.getIndexOfEdgeWithParent(v.getVertexId()) < 0)
                    {
                        continue;
                    }
                    DENOPTIMEdge e = graph.getEdgeWithParent(v.getVertexId());
                    if (e!=null && e.getSourceReaction().equals(squery))
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
            query = eOutQuery.getSourceDAP();
            if (query > -1)
            {
                ArrayList<DENOPTIMVertex> newLst = 
                                               new ArrayList<DENOPTIMVertex>();
                for (DENOPTIMVertex v : matches)
                {
                    for (DENOPTIMEdge e : graph.getEdgesWithChild(
                                                              v.getVertexId()))
                    {
                        if (e.getSourceDAP() == query)
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
            query = eOutQuery.getBondType();
            if (query > -1)
            {
                ArrayList<DENOPTIMVertex> newLst = 
                                               new ArrayList<DENOPTIMVertex>();
                for (DENOPTIMVertex v : matches)
                {
                    for (DENOPTIMEdge e : graph.getEdgesWithChild(
                                                              v.getVertexId()))
                    {
                        if (e.getBondType() == query)
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
            String squery = eOutQuery.getTargetReaction();
            if (!squery.equals("*"))
            {
                ArrayList<DENOPTIMVertex> newLst =
                                               new ArrayList<DENOPTIMVertex>();
                for (DENOPTIMVertex v : matches)
                {
                    for (DENOPTIMEdge e : graph.getEdgesWithChild(
                                                              v.getVertexId()))
                    {
                        if (e.getTargetReaction().equals(squery))
                        {
                            newLst.add(v);
                            break;
                        }
                    }
                }
                matches = newLst;
            }
            squery = eOutQuery.getSourceReaction();
            if (!squery.equals("*"))
            {
                ArrayList<DENOPTIMVertex> newLst = 
                                               new ArrayList<DENOPTIMVertex>();
                for (DENOPTIMVertex v : matches)
                {
                    for (DENOPTIMEdge e : graph.getEdgesWithChild(
                                                              v.getVertexId()))
                    {
                        if (e.getSourceReaction().equals(squery))
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

        // Identify symmetiric sets and keep only one member
	removeSymmetryRedundance(graph,matches);

        if (verbosity > 1)
        {
            System.out.println("Final Matches (after symmetry): " + matches);
        }

        return matches;
    }

//------------------------------------------------------------------------------

    /**
     * Edit a graph according to a given list of edit tasks.
     * @param graph the graph to edit
     * @param edits the list of edit tasks
     * @param symmetry if <code>true</code> the symmetry is enforced
     * @param verbosity the verbosity level
     * @return the modified graph
     */

    public static DENOPTIMGraph editGraph(DENOPTIMGraph graph, 
            ArrayList<DENOPTIMGraphEdit> edits, boolean symmetry, int verbosity)
                                                        throws DENOPTIMException
    {
	//Make sure there is no clash with vertex IDs
	int maxId = graph.getMaxVertexId();
	if (vertexCounter.get() <= maxId)
	{
	    try
	    {
	        resetUniqueVertexCounter(maxId+1);
	    }
	    catch (Throwable t)
	    {
		maxId = vertexCounter.getAndIncrement();
	    }
	}

        DENOPTIMGraph modGraph = (DENOPTIMGraph) DenoptimIO.deepCopy(graph);
        for (DENOPTIMGraphEdit edit : edits)
        {
            String task = edit.getType();
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
                    ArrayList<Integer> matches = GraphUtils.findVerticesIds(
                                             modGraph,query,symmetry,verbosity);
                    for (int pid : matches)
                    {
                        int wantedApID = edit.getFocusEdge().getSourceDAP();
                        String wantedApCl = 
                                        edit.getFocusEdge().getSourceReaction();
			ArrayList<Integer> symmUnqChilds = 
                                                 modGraph.getChildVertices(pid);
			if (symmetry)
			{
			    removeSymmetryRedundantIds(modGraph,symmUnqChilds);
			}
                        for (int cid : symmUnqChilds)
                        {
                            // Apply the query on the src AP on the focus vertex
                            // -1 id the wildcar
                            int srcApId = modGraph.getEdgeWithParent(
                                                            cid).getSourceDAP();
                            if (wantedApID>-1 && wantedApID != srcApId)
                            {
                                continue;
                            }
                            // Apply the query on the AP Class 
                            String srcApCl = modGraph.getEdgeWithParent(
                                                       cid).getSourceReaction();
                            if (!wantedApCl.equals("*") 
                                && !wantedApCl.equals(srcApCl))
                            {
                                continue;
                            }
                            GraphUtils.deleteVertex(modGraph,cid,symmetry);
                            int wantedTrgApId = e.getTargetDAP();
                            int trgApLstSize = inGraph.getVertexWithId(
                                           e.getTargetVertex()).getNumberOfAP();
                            if (wantedTrgApId >= trgApLstSize)
                            {
                                String msg = "Request to use AP number " 
                                + wantedTrgApId + " but only " + trgApLstSize
                                + " are found in the designated vertex.";
                                throw new DENOPTIMException(msg);
                            }
                            GraphUtils.appendGraphOnGraph(modGraph,
                                                 modGraph.getVertexWithId(pid),
                                                                       srcApId,
                                                                       inGraph,
                                  inGraph.getVertexWithId(e.getTargetVertex()),
                                                                 wantedTrgApId,
                                                               e.getBondType(),
                                                                      symmetry);
                        }
                    }
                    break;
                }
                case (DENOPTIMGraphEdit.DELETEVERTEX):
                {
                    DENOPTIMVertexQuery query =
                                 new DENOPTIMVertexQuery(edit.getFocusVertex(),
							   edit.getFocusEdge());
                    ArrayList<Integer> matches = GraphUtils.findVerticesIds(
                                             modGraph,query,symmetry,verbosity);
                    for (int vid : matches)
                    {
                        GraphUtils.deleteVertex(modGraph,vid,symmetry);
                    }
                    break;
                }
            }
        }
        return modGraph;
    }

//-----------------------------------------------------------------------------

    /**
     * Remove all but one of the symmetry-related partners in a list.
     * @param graph the graph to which the vertices IDs belong
     * @param list the list of vertex IDs to be purged
     */

    public static void removeSymmetryRedundantIds(DENOPTIMGraph graph,
                                                        ArrayList<Integer> list)
    {
	ArrayList<DENOPTIMVertex> vList = new ArrayList<DENOPTIMVertex>();
	for (int vid : list)
	{
	    vList.add(graph.getVertexWithId(vid));
	}
	removeSymmetryRedundance(graph,vList);
	list.clear();
	for (DENOPTIMVertex v : vList)
	{
	    list.add(v.getVertexId());
	}
    }

//-----------------------------------------------------------------------------

    /**
     * Remove all but one of the symmetry-related partners in a list
     * @param graph the graph to which the vertices belong
     * @param vList the list of vertices to be purged
     */

    public static void removeSymmetryRedundance(
			    DENOPTIMGraph graph, ArrayList<DENOPTIMVertex> list)
    {
        ArrayList<DENOPTIMVertex> symRedundnt = new ArrayList<DENOPTIMVertex>();
        Iterator<SymmetricSet> itSymm = graph.getSymSetsIterator();
        while (itSymm.hasNext())
        {
            SymmetricSet ss = itSymm.next();
            for (DENOPTIMVertex v : list)
            {
                int vid = v.getVertexId();
                if (symRedundnt.contains(v))
                {
                    continue;
                }
                if (ss.contains(vid))
                {
                    for (Integer idVrtInSS : ss.getList())
                    {
                        if (idVrtInSS != vid)
                        {
                            symRedundnt.add(graph.getVertexWithId(idVrtInSS));
                        }
                    }
                }
            }
        }
        for (DENOPTIMVertex v : symRedundnt)
        {
            list.remove(v);
        }
    }

//------------------------------------------------------------------------------

}
