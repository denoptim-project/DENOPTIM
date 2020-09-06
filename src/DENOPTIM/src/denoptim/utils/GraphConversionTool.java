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
import java.util.List;
import java.util.HashMap;
import java.util.logging.Level;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.*;


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
     * Generate the CDK atom-bond representation from the graph
     * @param g the molecular graph
     * @param closeRings if <code>true</code> imposes to make ring closing
     * bonds as specifies by the <code>DENOPTIMRing</code>s in the graph.
     * @return CDK representation of the molecular graph
     */

    public static IAtomContainer convertGraphToMolecule(DENOPTIMGraph g, 
                                   boolean closeRings) throws DENOPTIMException
    {
        ArrayList<DENOPTIMVertexAtom> lstDVA = new ArrayList<>();

        IAtomContainer mol = new AtomContainer();

        int molidx = 0, l = 0, j = 0;

        // loop through the vertices
        int n = g.getVertexCount();
        for (int i=0; i<n; i++)
        {
            DENOPTIMVertex vertex = g.getVertexList().get(i);
            molidx = vertex.getMolId();
            IGraphBuildingBlock igbb = FragmentSpace.getFragment(
                                              vertex.getFragmentType(), molidx);
            IAtomContainer iac = null;
            if (igbb instanceof DENOPTIMFragment)
            {
            	iac = (DENOPTIMFragment) igbb;
            } else if (igbb instanceof DENOPTIMTemplate) 
            {
            	//TODO
            	System.out.println("This is where some magic will occur... ");
            	continue;
            } else {
            	//The vertex does not point to anything that contains molecular fragments
            	// so we will ignore it here
            	continue;
            }
            
            for (IAtom atm : iac.atoms())
            {
                atm.setProperty(DENOPTIMConstants.ATMPROPVERTEXID,
                                                          vertex.getVertexId());
            }

            mol.add(iac);

            HashMap<Integer, Integer> anum_map = new HashMap<>();

            for(IAtom atom : iac.atoms())
            {
                j = iac.getAtomNumber(atom);
                anum_map.put(j, l);
                l++;
            }

            Integer id = vertex.getVertexId();

            DENOPTIMVertexAtom vtxAtm = new DENOPTIMVertexAtom(id, anum_map);
            //vtxAtm.printDetails();

            lstDVA.add(vtxAtm);
        }


        // loop through the edges
        n = g.getEdgeCount();
        for (int i=0; i<n; i++)
        {
            DENOPTIMEdge edge = g.getEdgeList().get(i);

            // get the vertex ids
            int v1_id = edge.getSourceVertex();
            int v2_id = edge.getTargetVertex();


            DENOPTIMVertex v1 = g.getVertexWithId(v1_id);
            DENOPTIMVertex v2 = g.getVertexWithId(v2_id);

            int dap_idx_v1 = edge.getSourceDAP();
            int dap_idx_v2 = edge.getTargetDAP();

            int dap1_anum = v1.
                            getAttachmentPoints().
                            get(dap_idx_v1).
                            getAtomPositionNumber();
            int dap2_anum = v2.
                            getAttachmentPoints().
                            get(dap_idx_v2).
                            getAtomPositionNumber();

            // get the new atom indices for the dap's
            int atom1 = FragmentUtils.getCorrespondingAtomNumber(lstDVA, v1_id,
                                                                     dap1_anum);
            int atom2 = FragmentUtils.getCorrespondingAtomNumber(lstDVA, v2_id,
                                                                     dap2_anum);

            //System.err.println(atom1 + " " + v1_id + " " + dap_idx_v1 + " " + dap1_anum);
            //System.err.println(atom2 + " " + v2_id + " " + dap_idx_v2 + " " + dap2_anum);

            if (atom1 >= 0 && atom2 >= 0)
            {
                switch (edge.getBondType())
                {
                case 1:
                    mol.addBond(atom1, atom2, IBond.Order.SINGLE);
                    break;
                case 2:
                    mol.addBond(atom1, atom2, IBond.Order.DOUBLE);
                    break;
                case 3:
                    mol.addBond(atom1, atom2, IBond.Order.TRIPLE);
                    break;
                }
            }
            else
            {
                String msg = "Incorrect indices. Although this may be a bug, " +
                        "it is more likely an error in the atom specification. "
                        + "Kindly check your input files."
                        + "Error occurred while dealing with edge " + edge
                        + " and vertexes " + v1 + " and " + v2 + ".";
                //System.err.println("ERROR: " + g.toString());
                DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                throw new DENOPTIMException(msg);
            }
        }
       
        // loop through the rings
        if (closeRings)
        {
            for (int i=0; i<g.getRings().size(); i++)
            {
                DENOPTIMRing r = g.getRings().get(i);
                DENOPTIMVertex vH = r.getHeadVertex();
                DENOPTIMVertex vT = r.getTailVertex();
                int vH_id = vH.getVertexId();
                int vT_id = vT.getVertexId();

                // ASSUMPTION: Ring closing vertex contains only one atom
                // that is the RingClosingAttractor (RCA)

                int idH = FragmentUtils.getCorrespondingAtomNumber(lstDVA, 
                                                                      vH_id, 0);
                int idT = FragmentUtils.getCorrespondingAtomNumber(lstDVA, 
                                                                      vT_id, 0);

                List<IAtom> cH = mol.getConnectedAtomsList(mol.getAtom(idH)); 
                List<IAtom> cT = mol.getConnectedAtomsList(mol.getAtom(idT));

                // Check connectivity of RCAs
                if (cH.size() != 1 || cT.size() != 1)
                {
                    String s = "Unable to make convert graph to molecule. " 
                               + "Head or tail vertex in ring " + r 
                               + " does not terminate with RCA. "
                               + "Check graph "+ g;
                    DENOPTIMLogger.appLogger.log(Level.SEVERE, s);
                    throw new DENOPTIMException(s);
                }
        
                int idSrcH = mol.getAtomNumber(cH.get(0));
                int idSrcT = mol.getAtomNumber(cT.get(0));

                int bndOrder = r.getBondType(); 
                switch (bndOrder)
                {
                case 1:
                    mol.addBond(idSrcH, idSrcT, IBond.Order.SINGLE);
                    break;
                case 2:
                    mol.addBond(idSrcH, idSrcT, IBond.Order.DOUBLE);
                    break;
                case 3:
                    mol.addBond(idSrcH, idSrcT, IBond.Order.TRIPLE);
                    break;
                default:
                    mol.addBond(idSrcH, idSrcT, IBond.Order.SINGLE);
                    break;
                }
            }
        }
 
        lstDVA.clear();

	// Store graph as property
        mol.setProperty(DENOPTIMConstants.GCODETAG, g.getGraphId());
        mol.setProperty(DENOPTIMConstants.GRAPHTAG, g.toString());
        if (g.getMsg() != null)
        {
            mol.setProperty(DENOPTIMConstants.GMSGTAG, g.getMsg());
        }

        return mol;
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
     */

    public static DENOPTIMGraph getGraphFromString(String strGraph, 
				    boolean useMolInfo) throws DENOPTIMException
    {
    	// get the main blocks to parse: graphID, vertices, edges, rings, symSet
        String s1[] = strGraph.split("\\s+");
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
        String s2[] = vStr.split(",");

        ArrayList<DENOPTIMVertex> vertices = new ArrayList<>();

        // for each vertex
        for (int i=0; i<s2.length; i++)
        {
            String s3[] = s2[i].split("_");

            // vertex id
            int vid = Integer.parseInt(s3[0]);
            // molid
            int molid = Integer.parseInt(s3[1]) - 1;
            // fragment/scaffold
            int fragtype = Integer.parseInt(s3[2]);
            // level
            int level = Integer.parseInt(s3[3]);

            ArrayList<DENOPTIMAttachmentPoint> lstAP;
		    if (useMolInfo)
		    {
	            lstAP = FragmentUtils.getAPForFragment(molid, fragtype);
		    }
		    else
		    {
				// NB: empty list of APs will be updated later when we have 
				// the edges.
				lstAP = new ArrayList<DENOPTIMAttachmentPoint>();
		    }
	
            DENOPTIMVertex dv = new DENOPTIMVertex(vid, molid, lstAP, fragtype);
            dv.setLevel(level);

            if (useMolInfo)
            {
                IGraphBuildingBlock mol = FragmentSpace.getFragment(fragtype,molid);
                ArrayList<SymmetricSet> simAP = 
                                        FragmentUtils.getMatchingAP(mol, lstAP);
                dv.setSymmetricAP(simAP);
		    }
		    // NB: we cannot record which APs are symmetric from the string
		    // representation of a graph without the library of fragments.

            vertices.add(dv);
        }
	
        ArrayList<DENOPTIMEdge> edges = new ArrayList<>();

        // split edges on the comma
        if (eStr.contains(","))
        {
            s2 = eStr.split(",");
            for (int i=0; i<s2.length; i++)
            {
                String s4[] = s2[i].split("_");
                int srcVertex = Integer.parseInt(s4[0]);
    
                int srcDAP = Integer.parseInt(s4[1]);
    
                int trgVertex = Integer.parseInt(s4[2]);
    
                int trgDAP = Integer.parseInt(s4[3]);
    
                int btype = Integer.parseInt(s4[4]);
    
                DENOPTIMEdge ne = new DENOPTIMEdge(srcVertex, trgVertex, srcDAP,
                                                        trgDAP, btype);
    
                //System.err.println("EDGE: " + ne.toString());
    
                if (s4.length > 5)
                {
                    ne.setSourceReaction(s4[5]);
                    ne.setTargetReaction(s4[6]);
                }
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
            ArrayList<DENOPTIMVertex> lstVerteces = new ArrayList<>();
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

        // update the attachment point info based on the edge info
        for (int i=0; i<edges.size(); i++)
        {
            DENOPTIMEdge edge = edges.get(i);
            int bndOrder = edge.getBondType();
            int srcvid = edge.getSourceVertex();
            int trgvid = edge.getTargetVertex();
            int iA = edge.getSourceDAP();
            int iB = edge.getTargetDAP();

            //System.err.println("iA=" + iA + " " + "iB=" + iB);

            DENOPTIMVertex src = g.getVertexWithId(srcvid);
            DENOPTIMVertex trg = g.getVertexWithId(trgvid);
	
		    // Here we fill the vertices with placeholder. This because
		    // we want to be able to define a graph even without knowing
		    // anything on the actual fragment contained in the vertex.
		    // In particular, the list of APs is not knowable from the 
		    // DENOPTIMGraph without the corresponding library of fragments.
            if (!useMolInfo)
            {
            	ArrayList<DENOPTIMAttachmentPoint> lstAPsrc = 
							      src.getAttachmentPoints();
				if (lstAPsrc.size() <= iA)
				{
				    while (lstAPsrc.size() <= (iA+1))
				    {
				        lstAPsrc.add(new DENOPTIMAttachmentPoint());
				    }
				}
				src.setAttachmentPoints(lstAPsrc);
		        ArrayList<DENOPTIMAttachmentPoint> lstAPtrg =
		        		trg.getAttachmentPoints();
                if (lstAPtrg.size() <= iB)
                {
                    while (lstAPtrg.size() <= (iB+1))
                    {
                        lstAPtrg.add(new DENOPTIMAttachmentPoint());
                    }
                }
                trg.setAttachmentPoints(lstAPtrg);
            }
		
            DENOPTIMAttachmentPoint apA = src.getAttachmentPoints().get(iA);
            DENOPTIMAttachmentPoint apB = trg.getAttachmentPoints().get(iB);
		    if (useMolInfo)
	        {
                apA.updateFreeConnections(-bndOrder);
                apB.updateFreeConnections(-bndOrder);
		    }
        }

        // update bond type of chords
        for (DENOPTIMRing r : rings)
        {
            int vid = r.getHeadVertex().getVertexId();
            for (DENOPTIMEdge e : edges)
            {
                if (e.getTargetVertex() == vid || e.getSourceVertex() == vid)
                {
                    int bt = e.getBondType();
                    r.setBondType(bt);
                    break;
                }
            }
        }

        g.setGraphId(gcode);

        return g;
    }

//------------------------------------------------------------------------------

}
