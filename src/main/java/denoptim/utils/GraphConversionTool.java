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

package denoptim.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jgrapht.graph.DefaultUndirectedGraph;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.DGraph;
import denoptim.graph.Edge;
import denoptim.graph.Edge.BondType;
import denoptim.graph.EmptyVertex;
import denoptim.graph.Fragment;
import denoptim.graph.Ring;
import denoptim.graph.SymmetricSet;
import denoptim.graph.Template;
import denoptim.graph.Vertex;
import denoptim.graph.simplified.Node;
import denoptim.graph.simplified.NodeConnection;
import denoptim.graph.simplified.UndirectedEdge;


/**
 * Tool to convert string into graphs and into molecular representation.
 *
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */

public class GraphConversionTool
{

//------------------------------------------------------------------------------

    /**
     * Given a formatted string-like representation of a DENOPTIM graph
     * create the corresponding <code>DENOPTIMGraph</code> object. This method 
     * assumes the correspondence between the graph and the loaded fragment 
     * space.
     * @param strGraph the string representation in DENOPTIM format. NOTE: this
     * is not the serialized representation of a <code>DENOPTIMGraph</code>, but
     * the string obtained by the
     * {@link denoptim.graph.DGraph#toString() toString} method the
     * <code>DENOPTIMGraph</code>.
     * @return the Graph representation that can be used by DENOPTIM
     * @throws denoptim.exception.DENOPTIMException
     */

    public static DGraph getGraphFromString(String strGraph,
            FragmentSpace fragSpace) throws DENOPTIMException
    {
    	return getGraphFromString(strGraph, true, fragSpace);
    }

//------------------------------------------------------------------------------

    /**
     * Given a formatted string-like representation of a DENOPTIM graph
     * create the corresponding <code>DENOPTIMGraph</code> object.
     * @param strGraph the string representation in DENOPTIM format. NOTE: this
     * is not the serialized representation of a <code>DENOPTIMGraph</code>, but
     * the string obtained by the 
     * {@link denoptim.graph.DGraph#toString() toString} method the
     * <code>DENOPTIMGraph</code>.
     * @param useMolInfo set to <code>true</code> when molecular information 
     * is available for all fragments. That is, the libraries of fragments 
     * provided to the FragmentSpace correspond to the fragments implied in 
     * the string-representation of the graph.
     * @return the Graph representation that can be used by DENOPTIM
     * @throws denoptim.exception.DENOPTIMException
     * @deprecated this method reads the old string representation, which cannot
     * represent all possible states of a {@link Template}. 
     * Use JSON string instead.
     */

    @Deprecated
    public static DGraph getGraphFromString(String strGraph, 
            boolean useMolInfo, FragmentSpace fragSpace) throws DENOPTIMException
    {  
    	// get the main blocks to parse: graphID, vertices, edges, rings, symSet
        String[] s1 = strGraph.split("\\s+");
        int gcode = Integer.parseInt(s1[0]);
        String vStr = s1[1];
        String eStr = "";
        if (s1.length > 2)
        {
            eStr= s1[2];
        }
        String oStr = "";
        for (int i=3; i<s1.length; i++)
        {
            oStr = oStr + " " + s1[i];
        }
        String rStr = "";
        String sStr = "";
		int beginningOfSymSets = oStr.indexOf("SymmetricSet");
		if (beginningOfSymSets == -1)
		{
		    rStr = oStr;
		}
		else
		{
	            rStr = oStr.substring(0,beginningOfSymSets);
	            sStr = oStr.substring(beginningOfSymSets);
		}

        // split vertices on the comma
        String[] s2 = vStr.split(",");

        ArrayList<Vertex> vertices = new ArrayList<>();

        // for each vertex
        for (int i=0; i<s2.length; i++)
        {
            String[] s3 = s2[i].split("_");

            // vertex id
            int vid = Integer.parseInt(s3[0]);
            // molid
            int molid = Integer.parseInt(s3[1]) - 1;
            // fragment/scaffold
            Vertex.BBType fragtype = Vertex.BBType.parseInt(
                    Integer.parseInt(s3[2]));
	            
            Vertex dv;
            if (fragSpace.isDefined())
            {
                dv = Vertex.newVertexFromLibrary(vid, molid, fragtype,
                        fragSpace);
            } else {
                // WARNING: in this case we cannot know the exact number of
                // attachment points, so we will add as many as needed to 
                // build the graph.
                dv =  new EmptyVertex(vid);
            }
            vertices.add(dv);
        }
        
        ArrayList<Edge> edges = new ArrayList<>();

        // split edges on the comma
        if (eStr.contains(","))
        {
            s2 = eStr.split(",");
            for (int i=0; i<s2.length; i++)
            {
                String[] s4 = s2[i].split("_");
                int srcVrtxID = Integer.parseInt(s4[0]);
    
                int srcAPID = Integer.parseInt(s4[1]);
    
                int trgVrtxID = Integer.parseInt(s4[2]);
    
                int trgAPID = Integer.parseInt(s4[3]);
    
                BondType btype = BondType.parseStr(s4[4]);

                /* Find source and target attachment points of edge */
                EmptyVertex dummy = new EmptyVertex();
                dummy.addAP();
                dummy.addAP();
                AttachmentPoint srcAP = dummy.getAP(0);
                AttachmentPoint trgAP = dummy.getAP(1);
                
                try {
                    for (int j = 0, apsFound = 0; apsFound < 2; j++) {
                        Vertex vertex = vertices.get(j);
                        if (vertex.getVertexId() == srcVrtxID) {
                            // WARNING!
                            // When we import graphs without a definition of the
                            // fragment space we can only guess how many APs 
                            // there are on a vertex. Here we add as many as 
                            // needed to allow formation of the edge.
                            // Currently we cannot know the index of the src
                            // atom, so we simply put this index to 0.
                            for (int k = vertex.getNumberOfAPs(); k<(srcAPID+1);
                                 k++)
                            {
                                if (vertex instanceof EmptyVertex)
                                    ((EmptyVertex)vertex).addAP();
                                if (vertex instanceof Fragment)
                                    ((Fragment)vertex).addAP(0);
                            }

                            srcAP = vertex.getAP(srcAPID);
                            if (s4.length > 5) {
                                srcAP.setAPClass(s4[5]);
                            }
                            apsFound++;
                        } else if (vertex.getVertexId() == trgVrtxID) {
                            // WARNING!
                            // When we import graphs without a definition of the
                            // fragment space we can only guess how many APs 
                            // there are on a vertex. Here we add as many as 
                            // needed to allow formation of the edge.
                            // Currently we cannot know the index of the src
                            // atom, so we simply put this index to 0.
                            for (int k = vertex.getNumberOfAPs(); k<(trgAPID+1);
                                 k++)
                            {
                                if (vertex instanceof EmptyVertex)
                                    ((EmptyVertex)vertex).addAP();
                                if (vertex instanceof Fragment)
                                    ((Fragment)vertex).addAP(0);
                            }

                            trgAP = vertex.getAP(trgAPID);
                            if (s4.length > 5) {
                                trgAP.setAPClass(s4[6]);
                            }
                            apsFound++;
                        }
                    }
                } catch (IndexOutOfBoundsException e) {
                    throw new IllegalStateException("Searching for srcVrtxID:"
                            + srcVrtxID + ", srcAPID:"
                            + srcAPID + ", trgVrtxID:"
                            + trgVrtxID + ", trgAPID:"
                            + trgAPID + ", but source or target " +
                            "attachment point not present on source or target" +
                            " vertex. "+strGraph, e);
                }
    
                Edge ne = new Edge(srcAP, trgAP, btype);
                edges.add(ne);
            }
        }
    
        // collect Rings
        ArrayList<Ring> rings = new ArrayList<>();
        String[] sr2 = rStr.split("DENOPTIMRing ");
        for (int i=1; i<sr2.length; i++)
        {
            String sr4 = sr2[i];
            String sr5 = sr4.substring(sr4.indexOf("=") + 1).trim();
            sr5 = sr5.substring(1,sr5.length()-2);
            String[] sr6 = sr5.split(",\\s");
            ArrayList<Vertex> lstVerteces =
                    new ArrayList<Vertex>();
            for (int j=0; j<sr6.length; j++)
            {
                String sr7[] = sr6[j].split("_");

                // vertex id
                int vid = Integer.parseInt(sr7[0]);

                for (Vertex v : vertices)
                {
                    if (v.getVertexId() == vid)
                    {
                        lstVerteces.add(v);
                        break;
                    }
                }
            }

            Ring r = new Ring(lstVerteces);
            rings.add(r);
        }

		// collect map of symmetric vertices
        ArrayList<SymmetricSet> symSets = new ArrayList<SymmetricSet>();
        String[] ss8 = sStr.split("SymmetricSet ");
        for (int i=1; i<ss8.length; i++)
        {
            String ss4 = ss8[i];
            String ss5 = ss4.substring(ss4.indexOf("=") + 1).trim();
            ss5 = ss5.substring(1,ss5.length()-2);
            String[] ss6 = ss5.split(",\\s");
            ArrayList<Integer> symVrtxIds = new ArrayList<Integer>();
            for (int j=0; j<ss6.length; j++)
            {
            	symVrtxIds.add(Integer.parseInt(ss6[j]));
            }

            SymmetricSet ss = new SymmetricSet(symVrtxIds);
            symSets.add(ss);
        }
	
        DGraph g = new DGraph(vertices, edges, rings, symSets);

        // update bond type of chords
        for (Ring r : rings)
        {
            int vid = r.getHeadVertex().getVertexId();
            for (Edge e : edges)
            {
                if (e.getTrgVertex() == vid || e.getSrcVertex() == vid)
                {
                    r.setBondType(e.getBondType());
                    break;
                }
            }
        }
        
        g.setGraphId(gcode);
        
        return g;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Converts a {@link DGraph} into a simplified JGraphT 
     * {@link DefaultUndirectedGraph}. 
     * The simplification consist of not producing a 1:1 list of vertexes and 
     * edges compared to the {@link DGraph}. 
     * Instead,
     * <ul>
     * <li>pairs of used RCVs are removed and the attachment points to
     * which they were bound are considered to be connected by an edge.</li>
     * <li>all edges are considered undirected.</li>
     * </ul>
     * @param dg the graph to convert.
     * @return the simplified graph.
     */
    public static DefaultUndirectedGraph<Vertex, UndirectedEdge>
        getJGraphFromGraph(DGraph dg)
    {
        DefaultUndirectedGraph<Vertex, UndirectedEdge> g = 
                        new DefaultUndirectedGraph<>(UndirectedEdge.class);
        Map<Vertex,Integer> vis = new HashMap<Vertex,Integer>();
        int i = 0;
        for (Vertex v : dg.getVertexList())
        {
            vis.put(v, i);
            i += 1;
            if (v.isRCV())
            {
                if (!dg.isVertexInRing(v))
                {
                    g.addVertex(v);
                }
            } else {
                g.addVertex(v);
            }
        }

        for (Edge e : dg.getEdgeList())
        {
            Vertex vA = e.getSrcAP().getOwner();
            Vertex vB = e.getTrgAP().getOwner();
            if (!vA.isRCV() && !vB.isRCV())
            {
                g.addEdge(vA, vB, new UndirectedEdge(e.getSrcAP(), 
                        e.getTrgAP(), e.getBondType()));
            }
        }
        
        for (Ring r : dg.getRings())
        {
            Vertex vA = r.getHeadVertex();
            Vertex vB = r.getTailVertex();
            Vertex pA = vA.getParent();
            Vertex pB = vB.getParent();

            g.addEdge(pA, pB, new UndirectedEdge(
                    vA.getEdgeToParent().getSrcAP(), 
                    vB.getEdgeToParent().getSrcAP(), r.getBondType()));
        }
        return g;
    }

//------------------------------------------------------------------------------
    
    /**
     * Converts a {@link DGraph} into a simplified JGraphT 
     * {@link DefaultUndirectedGraph}. The simplification is even greater
     * than for graphs produced by  
     * {@link #getJGraphFromGraph(DGraph)} in that the content of each 
     * vertex and the identify of the attachment points are both ignored.
     * However, in this method any free {@link AttachmentPoint} on the 
     * given graph will be converted into a node of the JGraphT, so that the
     * location of {@link AttachmentPoint}s relative to the structure of
     * the graph can be detected.
     * @param dg the graph to convert.
     * @return the simplified graph.
     */
    public static DefaultUndirectedGraph<Node, NodeConnection>
        getJGraphKernelFromGraph(DGraph dg)
    {
        DefaultUndirectedGraph<Node, NodeConnection> g = 
                        new DefaultUndirectedGraph<>(NodeConnection.class);
        for (Vertex v : dg.getVertexList())
        {
            if (v.isRCV())
            {
                if (!dg.isVertexInRing(v))
                {
                    g.addVertex(new Node(v));
                }
            } else {
                g.addVertex(new Node(v));
            }
        }

        for (Edge e : dg.getEdgeList())
        {
            Vertex vA = e.getSrcAP().getOwner();
            Vertex vB = e.getTrgAP().getOwner();
            if (!vA.isRCV() && !vB.isRCV())
            {
                Node vkA = (Node) vA.getProperty(
                        Node.REFTOVERTEXKERNEL);
                Node vkB = (Node) vB.getProperty(
                        Node.REFTOVERTEXKERNEL);
                g.addEdge(vkA, vkB, new NodeConnection(e.getBondType()));
            }
        }
        
        for (Ring r : dg.getRings())
        {
            Vertex vA = r.getHeadVertex();
            Vertex vB = r.getTailVertex();
            Vertex pA = vA.getParent();
            Vertex pB = vB.getParent();
            Node pvkA = (Node) pA.getProperty(
                    Node.REFTOVERTEXKERNEL);
            Node pvkB = (Node) pB.getProperty(
                    Node.REFTOVERTEXKERNEL);
            g.addEdge(pvkA, pvkB, new NodeConnection(r.getBondType()));
        }
        
        for (AttachmentPoint ap : dg.getAvailableAPs())
        {
            Node vap = new Node(ap);
            Vertex srcVrtx = ap.getOwner();
            Node pSrcVrtx = (Node) srcVrtx.getProperty(
                    Node.REFTOVERTEXKERNEL);
            g.addVertex(vap);
            g.addEdge(pSrcVrtx, vap, new NodeConnection(ap.getBondType()));
        }
        
        // Cleanup
        for (Vertex v : dg.getVertexList())
        {
            v.removeProperty(Node.REFTOVERTEXKERNEL);
        }
        
        return g;
    }

//------------------------------------------------------------------------------

}
