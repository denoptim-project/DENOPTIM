/*
 *   DENOPTIM
 *   Copyright (C) 2019 Marco Foscato <marco.foscato@uib.no>
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

package denoptim.threedim;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Matrix3d;
import javax.vecmath.AxisAngle4d;

import org.openscience.cdk.Atom;
import org.openscience.cdk.Bond;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.io.DenoptimIO;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMEdge;
import denoptim.molecule.DENOPTIMFragment;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.molecule.DENOPTIMEdge.BondType;
import denoptim.rings.RingClosureParameters;
import denoptim.utils.DENOPTIMMathUtils;


/**
 * Tool to build three-dimensional (3D) tree-like molecular structures from 
 * 3D fragments using the geometric information contained in the fragment's
 * attachment points.
 *
 * @author Marco Foscato 
 */

public class TreeBuilder3D
{
    /**
     * Reference to libraries of fragments
     */
    private ArrayList<DENOPTIMVertex> libScaff;
    private ArrayList<DENOPTIMVertex> libFrag;
    private ArrayList<DENOPTIMVertex> libCap;

    /**
     * The DENOPTIMGraph representation of the current system
     */
    private DENOPTIMGraph graph = new DENOPTIMGraph();

    /**
     * The molecular representation 
     */
    private IAtomContainer mol = new AtomContainer();

    /**
     * Map of rototranslated <code>DENOPTIMAttachmentPoint</code>
     * per each vertex ID
     */
    private Map<Integer,ArrayList<DENOPTIMAttachmentPoint>> apsPerVertexId =
            new HashMap<>();

    /**
     * Map of rototranslated <code>DENOPTIMAttachmentPoint</code>
     * per each <code>DENOPTIMEdge</code> (edge has no unique ID)
     */
    private Map<DENOPTIMEdge,ArrayList<DENOPTIMAttachmentPoint>> apsPerEdge =
            new HashMap<>();

    /**
     * Map of rototranslated APs per each <code>IAtom</code>
     */
    private Map<IAtom,ArrayList<DENOPTIMAttachmentPoint>> apsPerAtom =
            new HashMap<>();

    /**
     * Map of rototranslated APs per each <code>IBond</code>
     */
    private Map<IBond,ArrayList<DENOPTIMAttachmentPoint>> apsPerBond =
            new HashMap<>();

    /**
     * Flag to ensure syncronization between fields
     */
    private boolean conversionCompleted = false;

    /**
     * Debug flag
     */
    private boolean debug = false;

//------------------------------------------------------------------------------

    /**
     * Constructs a new TreeBuilder3D that uses the active fragment space.
     */

    public TreeBuilder3D()
    {
        this.libScaff =  FragmentSpace.getScaffoldLibrary();
        this.libFrag =  FragmentSpace.getFragmentLibrary();
        this.libCap =  FragmentSpace.getCappingLibrary();
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a new TreeBuilder3D providing libraries of building blocks
     */

    public TreeBuilder3D(ArrayList<DENOPTIMVertex> libScaff,
                         ArrayList<DENOPTIMVertex> libFrag,
                         ArrayList<DENOPTIMVertex> libCap)
    {
        this.libScaff = libScaff;
        this.libFrag = libFrag;
        this.libCap = libCap;
    }

//------------------------------------------------------------------------------

    /**
     * Created a three-dimensional molecular representation from a given 
     * DENOPTIMGraph. The conversion creates also two maps to retrace the 
     * attachment points within the final 3D structure both based on 
     * the ID of the source DENOPTIMVertex and correspondence to DENOPTIMEdge.
     * To retrieve this information see methods 'getApsPerVertexId' and 
     * 'getApsPerEdge'. In addition each atom is blended with the unique index
     * of the DENOPTIMVertex corresponding to the molecular fragment to which
     * the atom belongs.
     * Calling this method cleans all the fields (see method 'clean').
     * 
     * @param m_graph the DENOPTIMGraph to be transformed into a 3D molecule
     * @return the <code>AtomContainer</code> representation
     * @throws DENOPTIMException
     */

    public IAtomContainer convertGraphTo3DAtomContainer(DENOPTIMGraph m_graph)
                                                        throws DENOPTIMException
    {
        // Clean all
        this.clean();

        this.graph = m_graph;

        // Add the first vertex in the graph (i.e., the root of the tree)
        DENOPTIMVertex rootVrtx = graph.getVertexList().get(0);
        int idRootVrtx = rootVrtx.getVertexId();
        
        //TODO-V3 assumption that scaffold is a Fragment, which cannot hold true 
        // in general, so this will have to change 
        if (rootVrtx instanceof DENOPTIMFragment == false)
        {
            Exception e = new Exception("TODO: Upgrade code to new hierarchy!!!");
            e.printStackTrace();
            System.err.println("ERROR! TreeBuilder3D not ready to deal with "
                    + "anything else than DENOPTIMFragments!!!");
            System.exit(-1);
        }
        
        //TODO-V3: any possibility that scaffold does not have an IAtomContainer?
        IAtomContainer iacRootVrtx = rootVrtx.getIAtomContainer();
        
        //TODO-V3: this should never happen, so remove once we are sure it does nto happen
        if (iacRootVrtx == null)
        {
            Exception e = new Exception("TODO: Upgrade code to new hierarchy!!!");
            e.printStackTrace();
            System.err.println("ERROR! TreeBuilder3D not ready to deal with "
                    + "anything else than DENOPTIMFragments!!!");
            System.exit(-1);
        }
        
        for (IAtom atm : iacRootVrtx.atoms())
        {
            atm.setProperty(DENOPTIMConstants.ATMPROPVERTEXID,idRootVrtx);
        }
        mol.add(iacRootVrtx);
        
        // Store copy of APs
        ArrayList<DENOPTIMAttachmentPoint> apsOnThisFrag =
                new ArrayList<>();
        for (int i=0; i<rootVrtx.getNumberOfAP(); i++)
        {
            DENOPTIMAttachmentPoint ap = rootVrtx.getAttachmentPoints().get(i);
            // For first vertex the atomPositionNumber remains the same
            int atmPos = ap.getAtomPositionNumber();
            DENOPTIMAttachmentPoint apClone = ap.clone();
            apsOnThisFrag.add(apClone);
            IAtom srcAtm = mol.getAtom(atmPos);
            if (apsPerAtom.containsKey(srcAtm))
            {
                apsPerAtom.get(srcAtm).add(apClone);
            }
            else
            {
                ArrayList<DENOPTIMAttachmentPoint> apsOnThisAtm =
                        new ArrayList<>();
                apsOnThisAtm.add(apClone);
                apsPerAtom.put(srcAtm,apsOnThisAtm);
            }
        }
        apsPerVertexId.put(idRootVrtx,apsOnThisFrag);

        // Recursion on all branches on the tree (i.e., all incident edges)
        for (int iEdge : graph.getIndexOfEdgesWithChild(idRootVrtx))
        {
            DENOPTIMEdge edge = graph.getEdgeAtPosition(iEdge);

            // Get the AP from the current vertex to the next
            DENOPTIMAttachmentPoint apSrc = apsOnThisFrag.get(
                                                           edge.getSrcAPID());
            int atmPosApSrc = apSrc.getAtomPositionNumber();

            // Add AP to the map of APs per Edges
            ArrayList<DENOPTIMAttachmentPoint> apOnThisEdge =
                                      new ArrayList<DENOPTIMAttachmentPoint>();
            apOnThisEdge.add(apSrc);
            apsPerEdge.put(edge,apOnThisEdge);

            // Get two point defining the AP vector in 3D
            Point3d trgPtApSrc = new Point3d(apSrc.getDirectionVector());
            Point3d srcPtApSrc = new Point3d(iacRootVrtx.getAtom(
                                                     atmPosApSrc).getPoint3d());

            // Append 3D fragment on AP-vector and start recursion
            append3DFragmentsViaEdges(atmPosApSrc,srcPtApSrc,trgPtApSrc,edge);
        }

        this.conversionCompleted = true;

        if (debug)
        {
            String file = "iacTree.sdf";
            System.out.println("Writing tree-like IAtomContainer to " + file);
            IAtomContainer cmol = new AtomContainer();
            try
            {
                cmol = mol.clone();
            }
            catch (Throwable t)
            {
                throw new DENOPTIMException(t);
            }
            System.out.println("AP-per-VertexID");
            int i=0;
            for (int v : apsPerVertexId.keySet())
            {
                ArrayList<DENOPTIMAttachmentPoint> aps = apsPerVertexId.get(v);
                for (DENOPTIMAttachmentPoint ap : aps)
                {
                    i++;
                    IAtom atm = new Atom(String.valueOf(i), 
                                          new Point3d(ap.getDirectionVector()));
                    System.out.println("Vertex: "+v+" AP-"+i+" = "+ap);
                    cmol.addAtom(atm);
		    IBond bnd = new Bond(cmol.getAtom(ap.getAtomPositionNumber()),
			         cmol.getAtom(mol.getAtomCount()+i-1),
				 IBond.Order.SINGLE);
		    cmol.addBond(bnd);
                }
            }
            DenoptimIO.writeMolecule(file, mol, false);
            DenoptimIO.writeMolecule(file, cmol, true);
            System.out.println("AP-per-Edge");
            for (DENOPTIMEdge e : apsPerEdge.keySet())
            {
                ArrayList<DENOPTIMAttachmentPoint> aps = apsPerEdge.get(e);
                for (DENOPTIMAttachmentPoint ap : aps)
                {
                    System.out.println("Edge: "+e+" AP = "+ap);
                }
            }
            System.out.println("AP-per-Atom");
            for (IAtom a : apsPerAtom.keySet())
            {
                ArrayList<DENOPTIMAttachmentPoint> aps = apsPerAtom.get(a);
                for (DENOPTIMAttachmentPoint ap : aps)
                {
                    System.out.println("Atom: "+mol.getAtomNumber(a)
                                                                  +" AP = "+ap);
                }
            }
            System.out.println("AP-per-Bond");
            for (IBond b : apsPerBond.keySet())
            {
                ArrayList<DENOPTIMAttachmentPoint> aps = apsPerBond.get(b);
                for (DENOPTIMAttachmentPoint ap : aps)
                {
                    System.out.println("Bond: "+mol.getAtomNumber(b.getAtom(0))
                             +"-"+mol.getAtomNumber(b.getAtom(1))+" AP = "+ap);
                }
            }
        }

        return mol;
    }

//------------------------------------------------------------------------------

    /**
     * Recursive method that appends branches of 3D fragments following the 
     * edges of the graph. The connection is controlled by the geometries of the
     * attachment point on a growing molecule (i.e., apA) and that of the
     * attachment point (i.e., apB) of the incoming fragment (inVtx or inFrag).
     *
     * @param idSrcAtmA the index of the atom holding the AP on the growing
     * molecules
     * @param srcApA the point in space corresponding to the source of the
     * attachment point vector on the growing molecule
     * @param trgApA the point in space corresponding to the end of the
     * attachment point vector on the growing molecule
     * @param edge the <code>DENOPTIMEdge</code> corresponding to the
     * connection this method is charged to make between 3D molecular
     * fragments
     * @throws DENOPTIMException
     */

    private void append3DFragmentsViaEdges(int idSrcAtmA, Point3d srcApA, 
                     Point3d trgApA, DENOPTIMEdge edge) throws DENOPTIMException
    {
        if (debug)
        {
            System.err.println("Appending 3D fragment via edge: "+edge);
            System.err.println("#Atoms on growing mol: "+mol.getAtomCount());
        }

        // Get the incoming fragment
        DENOPTIMVertex inVtx = graph.getVertexWithId(edge.getTrgVertex());
        int idInVrx = inVtx.getVertexId();
        
        if (!inVtx.containsAtoms())
        {
            return;
        }
        
        //TODO-V3 assumption that scaffold is a Fragment, which cannot hold true 
        // in general, so this will have to change 
        if (inVtx instanceof DENOPTIMFragment == false)
        {
            Exception e = new Exception("TODO: Upgrade code to new hierarchy!!!");
            e.printStackTrace();
            System.err.println("ERROR! TreeBuilder3D not ready to deal with "
                    + "anything else than DENOPTIMFragments!!!");
            System.exit(-1);
        }
        
        //TODO-V3: any possibility that scaffold does not have an IAtomContainer?
        IAtomContainer inFragOri = inVtx.getIAtomContainer();
        
        //TODO-V3: this should never happen, so remove once we are sure it does nto happen
        if (inFragOri == null)
        {
            Exception e = new Exception("TODO: Upgrade code to new hierarchy!!!");
            e.printStackTrace();
            System.err.println("ERROR! TreeBuilder3D not ready to deal with "
                    + "anything else than DENOPTIMFragments!!!");
            System.exit(-1);
        }
        
        if (debug)
        {
            System.err.println("Incoming vertex : "+inVtx);
            System.err.println("Incoming IAC #atoms: "+inFragOri.getAtomCount());
        }
        
        IAtomContainer inFrag = new AtomContainer();
        try
        {
            inFrag = (IAtomContainer) inFragOri.clone();
        }
        catch (Throwable t)
        {
            throw new DENOPTIMException(t);
        }

        // Get all APs on the incoming fragment (we need to rototranslate them)
        ArrayList<Point3d> allApsAsPt3D = new ArrayList<Point3d>();
        for (int iap=0; iap<inVtx.getNumberOfAP(); iap++)
        {
            DENOPTIMAttachmentPoint ap = inVtx.getAttachmentPoints().get(iap);
            Point3d pt = new Point3d(ap.getDirectionVector());
            allApsAsPt3D.add(pt);
        }

        // Get the attachment point on the incoming fragment (i.e., ApB)
        int idApB = edge.getTrgAPID();
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

        if (debug)
        {
            System.err.println("After first translation and Before rotation");
            System.err.println("srcApA "+srcApA);
            System.err.println("trgApA "+trgApA);
            System.err.println("vectApA "+vectApA);
            System.err.println("srcApB "+srcApB);
            System.err.println("trgApB "+trgApB);
            System.err.println("vectApB "+vectApB);
        }

        // Get rotation matrix that aligns ApB to ApA
        double rotAng = vectApA.angle(vectApB);
        double threshold = 0.00001;
        if (rotAng >= threshold)
        {
            Vector3d rotAxis = new Vector3d();
            if (rotAng <= (Math.PI-0.00001))
            {
                rotAxis.cross(vectApB,vectApA);
            }
            else
            {
                rotAxis = DENOPTIMMathUtils.getNormalDirection(vectApA);
            }
            Matrix3d rotMat = new Matrix3d();
	    rotAxis.normalize();
            rotMat.set(new AxisAngle4d(rotAxis,rotAng));

            if (debug)
            {
                System.err.println("rotAng "+rotAng);
                System.err.println("rotAxis "+rotAxis);
                System.err.println("rotMat "+rotMat);
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

            // Update points defining AP vector
            trgApB = new Point3d(allApsAsPt3D.get(idApB));
            srcApB = new Point3d(inFrag.getAtom(idSrcAtmB).getPoint3d());
        }
        else
        {
            if (debug)
            {
                System.err.println("RotAng below threshold. No rotation.");
            }
        }

        if (debug)
        {
            System.err.println("After rotation before second translation");
            System.err.println("srcApA "+srcApA);
            System.err.println("trgApA "+trgApA);
            System.err.println("vectApA "+vectApA);
            System.err.println("srcApB "+srcApB);
            System.err.println("trgApB "+trgApB);
            System.err.println("vectApB "+vectApB);
        }

        // Check whether this edge involves a Ring Closing Attractors
        boolean edgeToRCA = false;
        if (RingClosureParameters.getRCStrategy().equals("BONDOVERLAP"))
        {
            for (String atyp : DENOPTIMConstants.RCATYPEMAP.keySet())
            {
                if ((inFrag.getAtom(idSrcAtmB).getSymbol().equals(atyp)) ||
                       (mol.getAtom(idSrcAtmA).getSymbol().equals(atyp)))
                {
                    edgeToRCA = true;
                    break;
                }
            }
        }

        // Get translation vector accounting for different length of the APs
        vectApA.sub(trgApA,srcApA);
        vectApB.sub(srcApB,trgApB);

        Point3d tr2 = new Point3d();
        if (edgeToRCA)
        {
            // Here we set translation vector as to move the incoming fragment
            // of the lenght of the longest AP. This is to place RCA at 
            // a bonding distance from the connected atom
            if (vectApA.length() > vectApB.length())
            {
                tr2.add(vectApA);
            }
            else
            {
                // NOTE: in this case the RCA is on mol, thus no translation
		// is needed beacuse thgApB is already on top of srcApA
            }
        }
        else
        {
            tr2.sub(vectApA,vectApB);
            tr2.scale(0.5);
        }

        // Translate atoms and APs to their final position
        for (IAtom atm : inFrag.atoms())
        {
            atm.getPoint3d().add(tr2);
            if ((atm.getPoint3d().x != atm.getPoint3d().x) ||
                (atm.getPoint3d().y != atm.getPoint3d().y) ||
                (atm.getPoint3d().z != atm.getPoint3d().z))
            {
                String str = "ERROR! NaN coordinated from rototranslation of "
                             + "3D fragment. Check source code.";
                throw new DENOPTIMException(str);
            }
        }
        for (Point3d pt : allApsAsPt3D)
        {
            pt.add(tr2);
            if ((pt.x != pt.x ) || (pt.y != pt.y ) || (pt.z != pt.z ))
            {
                String str = "ERROR! NaN coordinated from rototranslation of "
                             + "3D fragment's APs. Check source code.";
                throw new DENOPTIMException(str);
            }
        }

        // Store vertex ID on atoms
        for (IAtom atm : inFrag.atoms())
        {
            atm.setProperty(DENOPTIMConstants.ATMPROPVERTEXID,idInVrx);
        }

        // Append atoms of new fragment to the growing molecule and make bond
        int preNumAtms = mol.getAtomCount();
        mol.add(inFrag);
        int newIdSrcAtmB = idSrcAtmB + preNumAtms;
        
        BondType bndTyp = edge.getBondType();
        if (bndTyp.hasCDKAnalogue())
        {
            mol.addBond(idSrcAtmA, newIdSrcAtmB, bndTyp.getCDKOrder());
        } else {
            System.out.println("WARNING! Attempt to add ring closing bond "
                    + "did not add any actual chemical bond because the "
                    + "bond type of the chord is '" + bndTyp +"'.");
        }

        // Store copies of APs properly oriented
        ArrayList<DENOPTIMAttachmentPoint> apsOnThisFrag = new ArrayList<>();
        for (int i=0; i<inVtx.getNumberOfAP(); i++)
        {
            DENOPTIMAttachmentPoint oriAP = inVtx.getAttachmentPoints().get(i);
            // For vertices other than the first AtomPositionNumber in mol
            int atmPos = oriAP.getAtomPositionNumber() + preNumAtms;
            DENOPTIMAttachmentPoint newAP = oriAP.clone();
            newAP.setAtomPositionNumber(atmPos);
            apsOnThisFrag.add(newAP);
            IAtom srcAtm = mol.getAtom(atmPos);
            if (apsPerAtom.containsKey(srcAtm))
            {
                apsPerAtom.get(srcAtm).add(newAP);
            }
            else
            {
                ArrayList<DENOPTIMAttachmentPoint> apsOnThisAtm =
                        new ArrayList<>();
                apsOnThisAtm.add(newAP);
                apsPerAtom.put(srcAtm,apsOnThisAtm);
            }
        }
        apsPerVertexId.put(idInVrx, apsOnThisFrag);
        if (!apsPerEdge.containsKey(edge))
        {
            String str = "ERROR! Check the code "
                + "and make sure that the srcDAP has been added to the map "
                + "before trying to append the connected 3D fragment.";
            throw new DENOPTIMException(str);
        }
        apsPerEdge.get(edge).add(apsOnThisFrag.get(idApB));

        // Remember also relation between APs and bonds in the IAtomContainer
        IBond b = mol.getBond(mol.getAtom(idSrcAtmA),mol.getAtom(newIdSrcAtmB));
        apsPerBond.put(b,apsPerEdge.get(edge));
        
        // Recursion on all the edges leaving from this fragment
        for (int iEdge : graph.getIndexOfEdgesWithChild(idInVrx))
        {
            DENOPTIMEdge nextEdge = graph.getEdgeAtPosition(iEdge);

            // Get the AP from the current vertex to the next
            DENOPTIMAttachmentPoint nextApA =
                    apsOnThisFrag.get(nextEdge.getSrcAPID());
            int newIdSrcAtmA = nextApA.getAtomPositionNumber();

            // Add AP to the map of APs per Edges
            ArrayList<DENOPTIMAttachmentPoint> apOnNextEdge = new ArrayList<>();
            apOnNextEdge.add(nextApA);
            apsPerEdge.put(nextEdge,apOnNextEdge);

            // Get two point defining the AP vector in 3D
            Point3d trgNextApA = new Point3d(nextApA.getDirectionVector());
            Point3d srcNextApA = new Point3d(inFrag.getAtom(
                                       newIdSrcAtmA - preNumAtms).getPoint3d());

            // Append 3D fragment on AP-vector and start recursion
            append3DFragmentsViaEdges(newIdSrcAtmA,
                                      srcNextApA,
                                      trgNextApA,
                                      nextEdge);
        }
    }

//------------------------------------------------------------------------------

    /**
     * This method takes a given fragment the proper library.
     *
     * @param ftype
     * @param molidx
     * @return the <code>AtomContainer</code> representation
     * @throws DENOPTIMException
     */
    
    //TODO-V3: this method is redundant and should be replaced by appropriate 
    // functionality from the FragmentSpace

    public IAtomContainer getFragment(int ftype, int molidx)
                                                        throws DENOPTIMException
    {
        IAtomContainer iac = null, mol = null;
        switch (ftype)
        {
        case 0:
            if (molidx >= libScaff.size())
            {
                String str = "ERROR! Missmatch between the library of "
                             + "scaffolds used to create the DENOPTIMGraph "
                             + "and that provided to TreeBuilder3D.";
                throw new DENOPTIMException(str);
            }
            iac = libScaff.get(molidx).getIAtomContainer();
            break;
        case 1:
            if (molidx >= libFrag.size())
            {
                String str = "ERROR! Missmatch between the library of "
                             + "fragments used to create the DENOPTIMGraph "
                             + "and that provided to TreeBuilder3D.";
                throw new DENOPTIMException(str);
            }
            iac = libFrag.get(molidx).getIAtomContainer();
            break;
        case 2:
            if (molidx >= libCap.size())
            {
                String str = "ERROR! Missmatch between the library of "
                             + "capping frags used to create the DENOPTIMGraph "
                             + "and that provided to TreeBuilder3D.";
                throw new DENOPTIMException(str);
            }
            iac = libCap.get(molidx).getIAtomContainer();
            break;
        default:
            String str = "ERROR! Unrecognized type of fragment.";
            throw new DENOPTIMException(str);
        }
        try
        {
            mol = (IAtomContainer) iac.clone();
        }
        catch (CloneNotSupportedException cnse)
        {
            throw new DENOPTIMException(cnse);
        }
        return mol;
    }

//------------------------------------------------------------------------------

    /**
     * Clean the current system and restore this builder to its initial status.
     * Graph and molecular representation as well as other fields are cleaned,
     * but the libraries of building blocks remain.
     */

    public void clean()
    {
        this.graph = new DENOPTIMGraph();
        this.mol = new AtomContainer();
        this.apsPerVertexId =
                      new HashMap<Integer,ArrayList<DENOPTIMAttachmentPoint>>();
        this.apsPerEdge =
                 new HashMap<DENOPTIMEdge,ArrayList<DENOPTIMAttachmentPoint>>();
        this.apsPerAtom = 
                        new HashMap<IAtom,ArrayList<DENOPTIMAttachmentPoint>>(); 
        this.apsPerBond =
                        new HashMap<IBond,ArrayList<DENOPTIMAttachmentPoint>>();
        this.conversionCompleted = false;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the map of rototranslated APs per each vertex ID. This method 
     * <b>MUST</b> be called after conversion of the <code>DENOPTIMGraph</code> 
     * into <code>AtomContainer</code>, that is, after having called 
     * {@link #convertGraphTo3DAtomContainer(DENOPTIMGraph) this} method.
     *
     * @return the <code>Map</code> of all rototranslated 
     * <code>DENOPTIMAttachmentPoint</code> per each vertex ID
     * @throws DENOPTIMException
     */

    public Map<Integer,ArrayList<DENOPTIMAttachmentPoint>> getApsPerVertexId()
                                                        throws DENOPTIMException
    {
        if (!conversionCompleted)
        {
            String str = "Misuse of TreeBuilder3D. Cannot return map of "
                         + "rototranslated APs per each vertex ID before "
                         + "building the 3D representation.";
            throw new DENOPTIMException(str);
        }
        return apsPerVertexId;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the map of rototranslated APs per each DENOPTIMEdge. This method 
     * <b>MUST</b> be called after conversion of the <code>DENOPTIMGraph</code> 
     * into <code>AtomContainer</code>, that is, after having called 
     * {@link #convertGraphTo3DAtomContainer(DENOPTIMGraph) this} method.
     *
     * @return the <code>Map</code> of all rototranslated 
     * <code>DENOPTIMAttachmentPoint</code> per each <code>DENOPTIMEdge</code>
     * @throws DENOPTIMException
     */

    public Map<DENOPTIMEdge,ArrayList<DENOPTIMAttachmentPoint>> getApsPerEdge()
                                                        throws DENOPTIMException
    {
        if (!conversionCompleted)
        {
            String str = "Misuse of TreeBuilder3D. Cannot return map of "
                         + "rototranslated APs per each DENOPTIMEdge before "
                         + "building the 3D representation.";
            throw new DENOPTIMException(str);
        }
        return apsPerEdge;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the map of rototranslated APs per each source atom. This method 
     * <b>MUST</b> be called after conversion of the <code>DENOPTIMGraph</code> 
     * into <code>AtomContainer</code>, that is, after having called 
     * {@link #convertGraphTo3DAtomContainer(DENOPTIMGraph) this} method.
     *
     * @return the <code>Map</code> of all rototranslated 
     * <code>DENOPTIMAttachmentPoint</code> per each <code>IAtom</code>
     * @throws DENOPTIMException
     */

    public Map<IAtom,ArrayList<DENOPTIMAttachmentPoint>> getApsPerAtom()
                                                        throws DENOPTIMException
    {
        if (!conversionCompleted)
        {
            String str = "Misuse of TreeBuilder3D. Cannot return map of "
                         + "rototranslated APs per each Atom before "
                         + "building the 3D representation.";
            throw new DENOPTIMException(str);
        }
        return apsPerAtom;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the map of rototranslated APs per each bond corresponding to 
     * and interfragment connection. This method 
     * <b>MUST</b> be called after conversion of the <code>DENOPTIMGraph</code> 
     * into <code>AtomContainer</code>, that is, after having called 
     * {@link #convertGraphTo3DAtomContainer(DENOPTIMGraph) this} method.
     *
     * @return the <code>Map</code> of all rototranslated 
     * <code>DENOPTIMAttachmentPoint</code> per each <code>IBond</code>
     * @throws DENOPTIMException
     */

    public Map<IBond,ArrayList<DENOPTIMAttachmentPoint>> getApsPerBond()
                                                        throws DENOPTIMException
    {
        if (!conversionCompleted)
        {
            String str = "Misuse of TreeBuilder3D. Cannot return map of "
                         + "rototranslated APs per each Atom before "
                         + "building the 3D representation.";
            throw new DENOPTIMException(str);
        }
        return apsPerBond;
    }

//------------------------------------------------------------------------------

}
