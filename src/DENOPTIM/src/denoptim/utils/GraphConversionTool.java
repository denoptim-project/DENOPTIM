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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.jgrapht.graph.SimpleGraph;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.APClass;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMEdge;
import denoptim.molecule.DENOPTIMEdge.BondType;
import denoptim.molecule.DENOPTIMFragment;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMRing;
import denoptim.molecule.DENOPTIMTemplate;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.molecule.DENOPTIMVertex.BBType;
import denoptim.molecule.EmptyVertex;
import denoptim.molecule.SymmetricSet;
import denoptim.molecule.UndirectedEdgeRelation;
import denoptim.threedim.ThreeDimTreeBuilder;
import denoptimga.EAUtils;


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
     * Removes unused ring-closing vertexes. 
     * If the resulting free AP needs to be capped, then the proper
     * capping group is places where a ring-closing vertex once stood.
     * @param g the graph to modify.
     * @throws DENOPTIMException 
     */
    public static void replaceUnusedRCVsWithCapps(DENOPTIMGraph g) 
            throws DENOPTIMException
    {
        for (DENOPTIMVertex v : g.getRCVertices())
        {
            if (g.getRingsInvolvingVertex(v).size()==0 
                    && v.getEdgeToParent()!=null)
            {
                DENOPTIMAttachmentPoint apOnG = v.getEdgeToParent().getSrcAP();
                g.removeVertex(v);
                
                APClass cappAPClass = FragmentSpace.getAPClassOfCappingVertex(
                        apOnG.getAPClass());
                
                if (cappAPClass != null)
                {
                    int capId = FragmentSpace.getCappingGroupsWithAPClass(
                            cappAPClass).get(0);
                    DENOPTIMVertex capVrt = DENOPTIMVertex.newVertexFromLibrary(
                            capId,BBType.CAP);
                    capVrt.setVertexId(v.getVertexId());
                    g.appendVertexOnAP(apOnG, capVrt.getAP(0));
                }
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Given a formatted string-like representation of a DENOPTIM graph
     * create the corresponding <code>DENOPTIMGraph</code> object. This method 
     * assumes the correspondence between the graph and the loaded fragment 
     * space.
     * @param strGraph the string representation in DENOPTIM format. NOTE: this
     * is not the serialized representation of a <code>DENOPTIMGraph</code>, but
     * the string obtained by the
     * {@link denoptim.molecule.DENOPTIMGraph#toString() toString} method the
     * <code>DENOPTIMGraph</code>.
     * @return the Graph representation that can be used by DENOPTIM
     * @throws denoptim.exception.DENOPTIMException
     */

    public static DENOPTIMGraph getGraphFromString(String strGraph)
                                                        throws DENOPTIMException
    {
    	return getGraphFromString(strGraph,true);
    }

//------------------------------------------------------------------------------

    /**
     * Given a formatted string-like representation of a DENOPTIM graph
     * create the corresponding <code>DENOPTIMGraph</code> object.
     * @param strGraph the string representation in DENOPTIM format. NOTE: this
     * is not the serialized representation of a <code>DENOPTIMGraph</code>, but
     * the string obtained by the 
     * {@link denoptim.molecule.DENOPTIMGraph#toString() toString} method the
     * <code>DENOPTIMGraph</code>.
     * @param useMolInfo set to <code>true</code> when molecular information 
     * is available for all fragments. That is, the libraries of fragments 
     * provided to the FragmentSpace correspond to the fragments implied in 
     * the string-representation of the graph.
     * @return the Graph representation that can be used by DENOPTIM
     * @throws denoptim.exception.DENOPTIMException
     * @deprecated this method reads the old string representation, which cannot
     * represent all possible states of a {@link DENOPTIMTemplate}. 
     * Use JSON string instead.
     */

    @Deprecated
    public static DENOPTIMGraph getGraphFromString(String strGraph, 
				    boolean useMolInfo) throws DENOPTIMException
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

        ArrayList<DENOPTIMVertex> vertices = new ArrayList<>();

        // for each vertex
        for (int i=0; i<s2.length; i++)
        {
            String[] s3 = s2[i].split("_");

            // vertex id
            int vid = Integer.parseInt(s3[0]);
            // molid
            int molid = Integer.parseInt(s3[1]) - 1;
            // fragment/scaffold
            DENOPTIMVertex.BBType fragtype = DENOPTIMVertex.BBType.parseInt(
                    Integer.parseInt(s3[2]));
	            
            DENOPTIMVertex dv;
            if (FragmentSpace.isDefined())
            {
                dv = DENOPTIMVertex.newVertexFromLibrary(vid, molid, fragtype);
            } else {
                // WARNING: in this case we cannot know the exact number of
                // attachment points, so we will add as many as needed to 
                // build the graph.
                dv =  new EmptyVertex(vid);
            }

            vertices.add(dv);
        }
        
        ArrayList<DENOPTIMEdge> edges = new ArrayList<>();

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
                DENOPTIMAttachmentPoint srcAP = dummy.getAP(0);
                DENOPTIMAttachmentPoint trgAP = dummy.getAP(1);
                
                try {
                    for (int j = 0, apsFound = 0; apsFound < 2; j++) {
                        DENOPTIMVertex vertex = vertices.get(j);
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
                                    ((EmptyVertex)vertex).addAP(0);
                                if (vertex instanceof DENOPTIMFragment)
                                    ((DENOPTIMFragment)vertex).addAP(0);
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
                                    ((EmptyVertex)vertex).addAP(0);
                                if (vertex instanceof DENOPTIMFragment)
                                    ((DENOPTIMFragment)vertex).addAP(0);
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
    
                DENOPTIMEdge ne = new DENOPTIMEdge(srcAP, trgAP, btype);
                edges.add(ne);
            }
        }
    
        // collect Rings
        ArrayList<DENOPTIMRing> rings = new ArrayList<>();
        String[] sr2 = rStr.split("DENOPTIMRing ");
        for (int i=1; i<sr2.length; i++)
        {
            String sr4 = sr2[i];
            String sr5 = sr4.substring(sr4.indexOf("=") + 1).trim();
            sr5 = sr5.substring(1,sr5.length()-2);
            String[] sr6 = sr5.split(",\\s");
            ArrayList<DENOPTIMVertex> lstVerteces =
                    new ArrayList<DENOPTIMVertex>();
            for (int j=0; j<sr6.length; j++)
            {
                String sr7[] = sr6[j].split("_");

                // vertex id
                int vid = Integer.parseInt(sr7[0]);

                for (DENOPTIMVertex v : vertices)
                {
                    if (v.getVertexId() == vid)
                    {
                        lstVerteces.add(v);
                        break;
                    }
                }
            }

            DENOPTIMRing r = new DENOPTIMRing(lstVerteces);
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
	
        DENOPTIMGraph g = new DENOPTIMGraph(vertices, edges, rings, symSets);

        // update bond type of chords
        for (DENOPTIMRing r : rings)
        {
            int vid = r.getHeadVertex().getVertexId();
            for (DENOPTIMEdge e : edges)
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
     * Converts a DENOPTIMGraph into a JGraphT {@link SimpleGraph}. Note that
     * the conversion does not produce a 1:1 list of vertexes and edges. 
     * Instead,
     * <ul>
     * <li>used pairs of RCVs are removed and the attachment points to
     * which they were bound are considered to be connected by an edge.</li>
     * <li>all edges are considered undirected.</li>
     * </ul>
     * @param dg
     * @return
     */
    public static DefaultUndirectedGraph<DENOPTIMVertex, UndirectedEdgeRelation>
    getJGraphFromGraph(DENOPTIMGraph dg)
    {
        //TODO: consider using DefaultUndirectedGraph to allow for loops
        // (i.e., rings involving only three vertexes: one proper vertex, and two RCVs)
        DefaultUndirectedGraph<DENOPTIMVertex, UndirectedEdgeRelation> g = 
                        new DefaultUndirectedGraph<>(UndirectedEdgeRelation.class);
        Map<DENOPTIMVertex,Integer> vis = new HashMap<DENOPTIMVertex,Integer>();
        int i = 0;
        for (DENOPTIMVertex v : dg.getVertexList())
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

        for (DENOPTIMEdge e : dg.getEdgeList())
        {
            DENOPTIMVertex vA = e.getSrcAP().getOwner();
            DENOPTIMVertex vB = e.getTrgAP().getOwner();
            if (!vA.isRCV() && !vB.isRCV())
            {
                g.addEdge(vA, vB, new UndirectedEdgeRelation(e.getSrcAP(), 
                        e.getTrgAP(), e.getBondType()));
            }
        }
        
        for (DENOPTIMRing r : dg.getRings())
        {
            DENOPTIMVertex vA = r.getHeadVertex();
            DENOPTIMVertex vB = r.getTailVertex();
            DENOPTIMVertex pA = vA.getParent();
            DENOPTIMVertex pB = vB.getParent();

            //TODO del try/catch
            //try
            //{
                g.addEdge(pA, pB, new UndirectedEdgeRelation(
                        vA.getEdgeToParent().getSrcAP(), 
                        vB.getEdgeToParent().getSrcAP(), r.getBondType()));
            /*
            } catch (Throwable t) {
                t.printStackTrace();
                System.out.println("HERE: graph "+dg.getGraphId()+" FAILES seemengly due to LOOP.");
                System.out.println("pA: "+pA);
                System.out.println("pB: "+pB);
                System.out.println("vA: "+vA);
                System.out.println("vB: "+vB);
                String filename = "/tmp/"+dg.getGraphId()+"failed.sdf";
                
                try
                {
                    ArrayList<DENOPTIMGraph> lst = new ArrayList<>();
                    lst.add(dg);
                    DenoptimIO.writeGraphsToSDF(new File(filename), lst, false);
                } catch (DENOPTIMException e1)
                {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
            */
        }
        return g;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Just a utility class that is used to convert the atom index in the
     * molecular representation of a specific vertex (i.e., vertexId) in the
     * atom index in the whole molecular representation of the graph.
     */
    
    private static class DENOPTIMVertexAtom
    {
        private int vertexId;
        private final HashMap<Integer, Integer> atmNumMap;

        public DENOPTIMVertexAtom(int vertexId,
                                  HashMap<Integer, Integer> atmNumMap)
        {
            this.vertexId = vertexId;
            this.atmNumMap = atmNumMap;
        }
        
        public int lookupMatchingAtomNumber(int anum)
        {
            return atmNumMap.getOrDefault(anum, -1);
        }
        
        public int getVertexId()
        {
            return vertexId;
        }
    }
    
//------------------------------------------------------------------------------

      /**
       * Return the atom number corresponding to the attachment point index
       * @param lstDVA
       * @param vertexId
       * @param dap_anum
       * @return the atom number corresponding to the attachment point index
       */

      public static int getCorrespondingAtomNumber(
             ArrayList<DENOPTIMVertexAtom> lstDVA, int vertexId, int dap_anum)
      {
          int mnum = -1;
          for (DENOPTIMVertexAtom dva : lstDVA)
          {
              if (dva.getVertexId() == vertexId)
              {
                  mnum = dva.lookupMatchingAtomNumber(dap_anum);
                  break;
              }
          }
          return mnum;
      }

//------------------------------------------------------------------------------

}
