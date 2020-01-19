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

package denoptimcg;

import java.util.ArrayList;
import java.util.Map;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.integration.tinker.TinkerMolecule;
import denoptim.integration.tinker.TinkerUtils;
import denoptim.io.DenoptimIO;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMEdge;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.rings.RingClosureParameters;
import denoptim.threedim.TreeBuilder3D;
import denoptim.utils.DummyAtomHandler;
import denoptim.utils.GenUtils;
import denoptim.utils.ObjectPair;
import denoptim.utils.OrganizeAtoms;
import denoptim.utils.RotationalSpaceUtils;

/**
 *
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class DENOPTIM3DMoleculeBuilder
{
    private String molName;
    private String workDir;
    private DENOPTIMGraph molGraph;
    private final static double EPSILON = 0.0001;
    
    private boolean debug = CGParameters.debug();
    private static int verbosity = CGParameters.getVerbosity();
	

    private double[] zerovec =
    {
        0.0, 0.0, 0.0
    };
    private String fsep = System.getProperty("file.separator");
    private static int spin = 1;
    private static String propNameVId = DENOPTIMConstants.ATMPROPVERTEXID;

//------------------------------------------------------------------------------

    public DENOPTIM3DMoleculeBuilder(String m_molName, DENOPTIMGraph m_molGraph,
            String m_dir)
    {
        molName = m_molName;
        workDir = m_dir;
        molGraph = m_molGraph;
    }

//------------------------------------------------------------------------------

    /**
     * Command line string for converting sdf to tinker XYZ format
     *
     * @param swPath
     * @param sdfinfile
     * @param xyzOutfile
     * @return the command string
     */
    private String sdfToTXYZ(String swPath, String sdfinfile,
            String xyzOutfile)
    {
        String cmdStr = swPath + " -isdf " + sdfinfile
                + " -otxyz -O " + xyzOutfile;
        return cmdStr;
    }

//------------------------------------------------------------------------------
    
    /**
     * Command line string for converting tinker XYZ format to internal
     * coordinates
     *
     * @param swPath
     * @param xyzinfile
     * @return the command statement
     */
    private String tXYZToInt(String swPath, String xyzinfile)
    {
        String cmdStr = swPath + " " + xyzinfile + " A";
        return cmdStr;
    }

//------------------------------------------------------------------------------
    
    /**
     * Internal coordinates to XYZ command line
     *
     * @param swPath
     * @param intfile
     * @return the command statement
     */
    private String intToXYZ(String swPath, String intfile)
    {
        String cmdStr = swPath + " " + intfile;
        return cmdStr;
    }

//------------------------------------------------------------------------------
    
    /**
     * Command line string for converting tinker XYZ format to internal
     * coordinates
     *
     * @param swPath
     * @param xyzinfile
     * @return the command statement
     */
    private String tXYZToSDF(String swPath, String xyzinfile, String sdfOutfile)
    {
        String cmdStr = swPath + " -itxyz " + xyzinfile + " -osdf -O " + sdfOutfile;
        return cmdStr;
    }

//------------------------------------------------------------------------------

    /**
     * Get the direction vector associated with the atom and AP index
     *
     * @param dirData
     * @param natm
     * @param apidx
     * @return direction vector
     */
    private double[] getDirectionVector(ArrayList<APAtomHolder> dirData,
            int natm, int apidx)
    {
        for (APAtomHolder aph : dirData)
        {
            if (aph.getAtomNumber() == natm)
            {
                if (aph.getAPIndex() == apidx)
                {
                    return aph.getDirVec();
                }
            }
        }
        return null;
    }

//------------------------------------------------------------------------------
    
    /**
     * For the bond connecting the 2 atoms, identify the AP with respect to
     * the second atom (the first neighbour) and return the associated AP 
     * index
     *
     * @param icidxA first atom position
     * @param icidxB second atom position
     * @param lstIcAp
     * @param other select the other AP index if <code>true</code>
     * @return the matching AP index
     * @throws DENOPTIMException
     */
    private int getAPIndex(int icidxA, int icidxB, ArrayList<ICAPVec> lstIcAp, 
                                boolean other) throws DENOPTIMException
    {
        int nidx = -1;
        for (ICAPVec icv : lstIcAp)
        {
            if (icv.getFirstIC() == icidxA && icv.getSecondIC() == icidxB)
            {
                nidx = icv.getFirstAP();
                if (other)
                    nidx = icv.getSecondAP();
                break;
            }
//            else if (icv.getFirstIC() == icidxB && icv.getSecondIC() == icidxA)
//            {
//                nidx = icv.getSecondAP();
//                if (other)
//                    nidx = icv.getFirstAP();
//                break;
//            }
        }

        if (nidx == -1)
        {
            String err = "ERROR! incorrect AP index!";
            throw new DENOPTIMException(err);
        }
        return nidx;
    }

//------------------------------------------------------------------------------

    /**
     * Given the graph representation of the molecular constitution,
     * along with the 3D
     * coordinates of the fragments and direction vectors, create 3D
     * structures by merging internal coordinates. More than one 
     * structure can be obtained if the provided graph allows for isomerism
     * for example by multiple possibility of ring formation or conformational
     * isomerism (not implemented yet).
     */

    public ArrayList<IAtomContainer> buildMulti3DStructure() 
                                                        throws DENOPTIMException
    {
        String msg = "";
	msg = "Building Multiple 3D representations for "
                 + "graph = " + molGraph.toString();
        System.out.println(msg);

        // Generate XYZ and INT representations
        long startTime = System.nanoTime();
        Molecule3DBuilder mol = build3DTree();
        long endTime = System.nanoTime();
        long time = (endTime - startTime);
        if (verbosity > 0)
        {
            msg = "TIME (build 3D model): " + time/1000000 + " ms"
                  + " #frags: " + mol.getGraph().getVertexList().size() 
                  + " #atoms: " + mol.getIAtomContainer().getAtomCount();
            System.out.println(msg);
	}

        // Evaluate source of isomerism
        // 1: Attempt Ring Closures 
        RingClosureTool rct = new RingClosureTool();
        ArrayList<Molecule3DBuilder> rcMols =new ArrayList<Molecule3DBuilder>();
        if (RingClosureParameters.allowRingClosures() && 
            mol.getGraph().getRings().size() != 0)
        {
            startTime = System.nanoTime();
            rcMols = rct.attemptAllRingClosures(mol);
            endTime = System.nanoTime();
            time = (endTime - startTime);
            if (verbosity > 0)
            {
                msg = "TIME (close ring): "+time/1000000+" ms"
                      + " #frags: " + mol.getGraph().getVertexList().size()
                      + " #atoms: " + mol.getIAtomContainer().getAtomCount()
                      + " #rings: " + mol.getGraph().getRings().size();
                System.out.println(msg);
	    }
        }
        else
        {
            Molecule3DBuilder nMol = mol.deepcopy();
            rct.saturateRingClosingAttractor(nMol);
            rcMols = new ArrayList<Molecule3DBuilder>();
            rcMols.add(nMol);
        }


        // 2: Conformational search
        ConformationalSearchPSSROT csPssRot = new ConformationalSearchPSSROT();
        startTime = System.nanoTime();
        ArrayList<Molecule3DBuilder> csMols = csPssRot.performPSSROT(rcMols);
        endTime = System.nanoTime();
        time = (endTime - startTime);
        if (verbosity > 0)
        {
            msg = "TIME (conf. search): "+time/1000000+" ms"
                  + " #frags: " + mol.getGraph().getVertexList().size()
                  + " #atoms: " + mol.getIAtomContainer().getAtomCount()
                  + " #rotBnds: " + mol.getRotatableBonds().size();
            System.out.println(msg);
	}

        // Convert and return results
        ArrayList<IAtomContainer> results = new ArrayList<IAtomContainer>();
        DummyAtomHandler dah = new DummyAtomHandler(
                                              DENOPTIMConstants.DUMMYATMSYMBOL);
        for (Molecule3DBuilder mol3db : csMols)
        {
            IAtomContainer iac = mol3db.getIAtomContainer();
            if (!CGParameters.getKeepDummyFlag())
            {
                iac = dah.removeDummyInHapto(iac);
                results.add(dah.removeDummy(iac));
            }
            else
            {
                results.add(iac);
            }
        }

        return results;
    }

//------------------------------------------------------------------------------

    /**
     * Generate 3D structure by assembling 3D fragments according to 
     * attachment point vector and following the tree-like structure of the 
     * graph.
     *
     * @throws DENOPTIMException
     */

    public Molecule3DBuilder build3DTree() throws DENOPTIMException
    {
        // Create 3D tree-like structure from DENOPTIMGraph
        TreeBuilder3D tb = new TreeBuilder3D();
        IAtomContainer initMol = tb.convertGraphTo3DAtomContainer(molGraph);

        // NOTE: the two following data structures might turn out useful in the
        // future, although now they are not needed

        // Create map of rototranslated APs per each vertex ID
        Map<Integer,ArrayList<DENOPTIMAttachmentPoint>> apsPerVertexId =
                                                         tb.getApsPerVertexId();
        
        // Create map of rototranslated APs per each edge (edge has no ID)
        Map<DENOPTIMEdge,ArrayList<DENOPTIMAttachmentPoint>> apsPerEdge =
                                                             tb.getApsPerEdge();

        if (debug)
        {
            System.out.println("apsPerVertexId: "+apsPerVertexId);
            System.out.println("apsPerEdge: "+apsPerEdge);
        }

        // Reorder atoms
        OrganizeAtoms oa = new OrganizeAtoms();
        oa.setScheme(CGParameters.getAtomOrderingScheme());
        int seedAtm = 0;
        IAtomContainer reorderedMol = oa.reorderStartingFrom(seedAtm, initMol);
        Map<Integer, ArrayList<Integer>> oldToNewMap = oa.getOldToNewOrder();
        if (debug)
        {
            System.out.println("oldToNewMap: "+oldToNewMap.get(seedAtm));
        }

        // Update list of AP on reordered atom container
        for (int vid : apsPerVertexId.keySet())
        {
            if (debug)
            {
                System.out.println("VERTEX: "+vid);
            }
            ArrayList<DENOPTIMAttachmentPoint> aps = apsPerVertexId.get(vid);
            for (DENOPTIMAttachmentPoint ap : aps)
            {
                if (debug)
                {
                    System.out.println("  AP old: "+ap);
                }
                int oldAtmId = ap.getAtomPositionNumber();
                int newAtmId = oldToNewMap.get(seedAtm).get(oldAtmId) - 1;
                ap.setAtomPositionNumber(newAtmId);
                if (debug)
                {
                    System.out.println("     new: "+ap);
                }
            }
        }

        // Collect rotatable bonds defined by fragment-fragment connections
        ArrayList<ObjectPair> rotBonds = 
                         RotationalSpaceUtils.defineRotatableBonds(reorderedMol,
                        FragmentSpaceParameters.getRotSpaceDefFile(),true,true);
        if  (verbosity > 0)
        {
            System.out.println("Rotatable bonds: "+rotBonds);
            if (debug)
            {
                System.out.println("Reordered IAtomContainer: 'iacToIC.sdf'");
                DenoptimIO.writeMolecule("iacToIC.sdf",reorderedMol,false);
		GenUtils.pause();
            }
        }

        // Generate Interal Coordinates
        TinkerMolecule tmol = TinkerUtils.getICFromIAC(reorderedMol,
                                                   CGParameters.getTinkerMap());
        TinkerUtils.setTinkerTypes(tmol,CGParameters.getTinkerMap());

        // Generate combined molecular representations (both XYZ and INT)
        Molecule3DBuilder m3DB = new Molecule3DBuilder(molGraph,
                                                       reorderedMol,
                                                       tmol,
                                                       molName,
                                                       rotBonds);

        return m3DB;
    }

//------------------------------------------------------------------------------

    /**
     * Append branch of 3D fragments following the edges of the graph. 
     * The connection is controlled by the geometries of the
     * attachment point on a growing molecule (i.e., apA) and that of the
     * attachment point (i.e., apB) of the incoming fragment (inVtx or inFrag).
     */
    private void append3DFragmentFollowingEdge(IAtomContainer mol,int idSrcAtmA,
         Point3d srcApA, Point3d trgApA, DENOPTIMEdge edge, DENOPTIMGraph graph,
                 Map<Integer,ArrayList<DENOPTIMAttachmentPoint>> apsPerVertexId,
                Map<DENOPTIMEdge,ArrayList<DENOPTIMAttachmentPoint>> apsPerEdge)
                                                        throws DENOPTIMException
    {
	if (debug)
	{
	    System.out.println("############### New Append ################");
	    System.out.println("srcApA: "+srcApA);
	    System.out.println("trgApA: "+trgApA);
	    System.out.println("idSrcAtmA:"+ idSrcAtmA);
	    System.out.println("edge: "+edge);
	    System.out.println("graph: "+graph);
	    System.out.println("apsPerVertexId: "+apsPerVertexId);
	    System.out.println("apsPerEdge: "+apsPerEdge);
	    System.out.println(" ");
	    System.out.println("MOL");
	    for (IAtom a : mol.atoms())
	    {
		System.out.println(" Atom: "+a.getSymbol()+" "+a.getPoint3d());
	    }
	}

        // Get the incoming fragment
        DENOPTIMVertex inVtx = graph.getVertexWithId(edge.getTargetVertex());
        int idInVrx = inVtx.getVertexId();
        IAtomContainer inFragOri = FragmentSpace.getFragment(
						        inVtx.getFragmentType(),
                                                              inVtx.getMolId());
        IAtomContainer inFrag = new AtomContainer();
        try
        {
            inFrag = (IAtomContainer) inFragOri.clone();
        }
        catch (Throwable t)
        {
            throw new DENOPTIMException(t);
        }

	if (debug)
	{
	    System.out.println(" ");
	    System.out.println("Incoming Frag");
	    for (IAtom a : inFrag.atoms())
	    {
	        System.out.println(" Atom: "+a.getSymbol()+" "+a.getPoint3d());
	    }
	}

        // Get all APs on the incoming fragment (we need to rototranslate them)
        ArrayList<Point3d> allApsAsPt3D = new ArrayList<Point3d>();
        for (int iap=0; iap<inVtx.getNumberOfAP(); iap++)
        {
            DENOPTIMAttachmentPoint ap = inVtx.getAttachmentPoints().get(iap);
            Point3d pt = new Point3d(ap.getDirectionVector());
            allApsAsPt3D.add(pt);
        }

	if (debug)
	{
	    for (Point3d pt :  allApsAsPt3D)
	    {
	        System.out.println(" AP pt: "+pt);
	    }
	}

        // Get the attachment point on the incoming fragment (i.e., ApB)
        int idApB = edge.getTargetDAP();
        DENOPTIMAttachmentPoint apB = inVtx.getAttachmentPoints().get(idApB);
        int idSrcAtmB = apB.getAtomPositionNumber();
        Point3d trgApB = new Point3d(allApsAsPt3D.get(idApB));
        Point3d srcApB = new Point3d(inFrag.getAtom(idSrcAtmB).getPoint3d());

        // Translate atoms and APs of fragment so that trgApB is on srcApA
        Point3d tr1 = new Point3d();
        tr1.sub(trgApB,srcApA);
        for (IAtom atm : inFrag.atoms()) 
        {
            atm.getPoint3d().sub(tr1);
        }
        for (Point3d pt : allApsAsPt3D)
        {
            pt.sub(tr1);
        }
        trgApB = new Point3d(allApsAsPt3D.get(idApB));
        srcApB = new Point3d(inFrag.getAtom(idSrcAtmB).getPoint3d());

        //Get Vectors ApA and ApB (NOTE: inverse versus of ApB!!!)
        Vector3d vectApA = new Vector3d();
        Vector3d vectApB = new Vector3d();
        vectApA.sub(trgApA,srcApA);
        vectApB.sub(srcApB,trgApB);
        vectApA.normalize();
        vectApB.normalize();

        // Get rotation matrix that aligns ApB to ApA
        double rotAng = vectApA.angle(vectApB);
        Vector3d rotAxis = new Vector3d();
        rotAxis.cross(vectApB,vectApA);
        rotAxis.normalize();
        Matrix3d rotMat = new Matrix3d();
        rotMat.set(new AxisAngle4d(rotAxis,rotAng));

	if (debug)
	{
	    System.out.println("rotAng "+rotAng);
	    System.out.println("rotAxis "+rotAxis);
	    System.out.println("rotMat "+rotMat);
	}
        
        // Rotate atoms and APs of fragment
        for (IAtom atm : inFrag.atoms())
        {
            atm.getPoint3d().sub(srcApA);
            rotMat.transform(atm.getPoint3d());
            atm.getPoint3d().add(srcApA);
        }
        for (Point3d pt :  allApsAsPt3D)
        {
            pt.sub(srcApA);
            rotMat.transform(pt);
            pt.add(srcApA);
        }
        trgApB = new Point3d(allApsAsPt3D.get(idApB));
        srcApB = new Point3d(inFrag.getAtom(idSrcAtmB).getPoint3d());

        // Translate to final position based on vectors lenght
        vectApA.sub(trgApA,srcApA);
        vectApB.sub(srcApB,trgApB);
        Point3d tr2 = new Point3d();
        tr2.sub(vectApA,vectApB);

        tr2.scale(0.5);

	if (debug)
	{
	    System.out.println("tr2 "+tr2);
	    System.out.println("");
	}

        for (IAtom atm : inFrag.atoms())
        {
            atm.getPoint3d().add(tr2);
        }
        for (Point3d pt : allApsAsPt3D)
        {
            pt.add(tr2);
        }

	if (debug)
	{
	    System.out.println(" ");
	    System.out.println("Incoming Frag (transl.rot.transl.)");
	    for (IAtom a : inFrag.atoms())
	    {
	        System.out.println(" Atom: "+a.getSymbol()+" "+a.getPoint3d());
	    }
	    for (Point3d pt :  allApsAsPt3D)
	    {
	        System.out.println(" AP pt: "+pt);
	    }
            System.out.println("dist: "+ 
		  inFrag.getAtom(0).getPoint3d().distance(allApsAsPt3D.get(0)));
	}
        
        // Store verted ID on atoms
        for (IAtom atm : inFrag.atoms())
        {
            atm.setProperty(propNameVId,idInVrx);
        }

        // Store copies of APs properly oriented
        ArrayList<DENOPTIMAttachmentPoint> apsOnThisFrag =
                                       new ArrayList<DENOPTIMAttachmentPoint>();
        for (int i=0; i<inVtx.getNumberOfAP(); i++)
        {
            DENOPTIMAttachmentPoint oriAP = inVtx.getAttachmentPoints().get(i);
            int atmpos = oriAP.getAtomPositionNumber();
            int connnum = oriAP.getAtmConnections();
            int apconn = oriAP.getAPConnections();
            String apCls = oriAP.getAPClass();
            Point3d pt = allApsAsPt3D.get(i);

            DENOPTIMAttachmentPoint newAP = new DENOPTIMAttachmentPoint(
                                               atmpos,
                                               connnum,
                                               apconn,
                                               new double[] {pt.x, pt.y, pt.z});

            newAP.setAPClass(apCls);
            apsOnThisFrag.add(newAP);
        }
        apsPerVertexId.put(idInVrx, apsOnThisFrag);
        if (!apsPerEdge.containsKey(edge))
        {
            System.out.println("ERROR! This should never happen. Check code "
                + "and make sure that the srcDAP has been added to the map "
                + "before trying to append the connected 3D fragment.");
            System.out.println("Edge: "+edge);
            System.out.println("AP: "+apsOnThisFrag.get(idApB));
            System.exit(1);
        }

        // Append atoms of new fragment to the growiong molecule
        int preNumAtms = mol.getAtomCount();
        mol.add(inFrag);
        int newIdSrcAtmB = idSrcAtmB + preNumAtms;
        switch (edge.getBondType())
        {
        case 1:
            mol.addBond(idSrcAtmA, newIdSrcAtmB, IBond.Order.SINGLE);
            break;
        case 2:
            mol.addBond(idSrcAtmA, newIdSrcAtmB, IBond.Order.DOUBLE);
            break;
        case 3:
            mol.addBond(idSrcAtmA, newIdSrcAtmB, IBond.Order.TRIPLE);
            break;
        }

        // Recursion on all the edges leaving from this fragment
        for (int iEdge : graph.getIndexOfEdgesWithChild(idInVrx))
        {
            DENOPTIMEdge nextEdge = molGraph.getEdgeAtPosition(iEdge);

            // Get the AP from the current vertex to the next
            DENOPTIMAttachmentPoint nextApA = apsOnThisFrag.get(
                                                       nextEdge.getSourceDAP());
            int newIdSrcAtmA = nextApA.getAtomPositionNumber() + preNumAtms;

            // Add AP to the map of APs per Edges
            ArrayList<DENOPTIMAttachmentPoint> apOnNextEdge =
                                       new ArrayList<DENOPTIMAttachmentPoint>();
            apOnNextEdge.add(nextApA);
            apsPerEdge.put(nextEdge,apOnNextEdge);

            // Get two point defining the AP vector in 3D
            Point3d trgNextApA = new Point3d(nextApA.getDirectionVector());
            Point3d srcNextApA = new Point3d(inFrag.getAtom(
                                 nextApA.getAtomPositionNumber()).getPoint3d());

            // Append 3D fragment on AP-vector and start recursion
            append3DFragmentFollowingEdge(mol,
                                          newIdSrcAtmA,
                                          srcNextApA, 
                                          trgNextApA,
                                          nextEdge, 
                                          graph,
                                          apsPerVertexId,
                                          apsPerEdge);
        }
    }

//------------------------------------------------------------------------------

}
