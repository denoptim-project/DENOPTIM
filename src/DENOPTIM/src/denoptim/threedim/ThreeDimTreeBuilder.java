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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.openscience.cdk.Atom;
import org.openscience.cdk.Bond;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
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
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.rings.RingClosureParameters;
import denoptim.utils.DENOPTIMMathUtils;
import denoptim.utils.DENOPTIMMoleculeUtils;
import denoptim.utils.GraphConversionTool;
import denoptim.utils.ObjectPair;
import denoptim.utils.RandomUtils;


/**
 * Tool to build build three-dimensional (3D) tree-like molecular structures 
 * from DENOPTIMGraphs. The molecular structure is assembled from the
 * building blocks by attaching any set of atoms/pseudoatoms contained in the
 * building block, and (optionally) aligning each incoming building block by 
 * using the geometric parameters defined in their attachment points.
 * By default, this builder does align building blocks, but method 
 * {@link ThreeDimTreeBuilder#setAlidnBBsIn3D(boolean)} allows to configure this
 * builder so that building blocks are not aligned during their assembly. In the 
 * latter configuration, the coordinates of atom in the resulting molecular 
 * structure will be the same as those found in the building block stored in the
 * given library of building blocks.
 *
 * @author Marco Foscato 
 */

public class ThreeDimTreeBuilder
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
    private DENOPTIMGraph graph;

    /**
     * The molecular representation 
     */
    private IAtomContainer mol;
    
    /**
     * Controls the maximum distance between a random place where unanchored
     * building blocks are placed and the system centroid. 
     */
    private double maxCoord = 20.0;

    /**
     * Map of roto-translated <code>DENOPTIMAttachmentPoint</code>
     * per each vertex ID
     */
    private Map<Integer,ArrayList<DENOPTIMAttachmentPoint>> apsPerVertexId =
            new HashMap<>();

    /**
     * Map of roto-translated <code>DENOPTIMAttachmentPoint</code>
     * per each <code>DENOPTIMEdge</code> (edge has no unique ID)
     */
    private Map<DENOPTIMEdge,ArrayList<DENOPTIMAttachmentPoint>> apsPerEdge =
            new HashMap<>();

    /**
     * Map of roto-translated APs per each <code>IAtom</code>
     */
    private Map<IAtom,ArrayList<DENOPTIMAttachmentPoint>> apsPerAtom =
            new HashMap<>();

    /**
     * Map of roto-translated APs per each <code>IBond</code>
     */
    private Map<IBond,ArrayList<DENOPTIMAttachmentPoint>> apsPerBond =
            new HashMap<>();
    
    /**
     * Flag controlling whether to align building blocks according to the AP
     * vectors or not. By default, we do align building blocks.
     */
    private boolean alignIn3D = true;
    
    /**
     * Flag to ensure sync between fields
     */
    private boolean conversionCompleted = false;
    
    /**
     * Private builder of atom containers
     */
    private IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();

    /**
     * Debug flag
     */
    private boolean debug = false;

//------------------------------------------------------------------------------

    /**
     * Constructs a new ThreeDimTreeBuilder that uses the global fragment space.
     */

    public ThreeDimTreeBuilder()
    {
        this.graph = new DENOPTIMGraph();
        this.mol = builder.newAtomContainer();
        this.libScaff =  FragmentSpace.getScaffoldLibrary();
        this.libFrag =  FragmentSpace.getFragmentLibrary();
        this.libCap =  FragmentSpace.getCappingLibrary();
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a new ThreeDimTreeBuilder providing libraries of building blocks
     */

    public ThreeDimTreeBuilder(ArrayList<DENOPTIMVertex> libScaff,
                         ArrayList<DENOPTIMVertex> libFrag,
                         ArrayList<DENOPTIMVertex> libCap)
    {
        this();
        this.libScaff = libScaff;
        this.libFrag = libFrag;
        this.libCap = libCap;
    }

//------------------------------------------------------------------------------
    
    /**
     * Sets the flag that controls whether building blocks have to be aligned
     * according to the AP vectors or not. 
     * @param align the new value. Use <code>true</code> to align building 
     * blocks (default), or <code>false</code> to prevent alignment.
     */
    public void setAlidnBBsIn3D(boolean align)
    {
        this.alignIn3D = align;
    }
    
//------------------------------------------------------------------------------

    /**
     * Created a three-dimensional molecular representation from a given 
     * DENOPTIMGraph. The conversion creates also two maps to retrace the 
     * attachment points within the final 3D structure both based on 
     * the ID of the source DENOPTIMVertex and correspondence to DENOPTIMEdge.
     * To retrieve this information see methods 
     * {@link ThreeDimTreeBuilder#getApsPerVertexId()},
     * {@link ThreeDimTreeBuilder#getApsPerAtom()} and
     * {@link ThreeDimTreeBuilder#getApsPerEdge()}. 
     * In addition each atom is blended with the unique index
     * of the DENOPTIMVertex corresponding to the molecular fragment to which
     * the atom belongs.
     * Calling this method cleans all the fields from previously existing data
     * (see method {@link ThreeDimTreeBuilder#clean()}).
     * 
     * This method does not remove ring-closing attractors. You can remove them
     * by calling {@link ThreeDimTreeBuilder#convertGraphTo3DAtomContainer(
     * DENOPTIMGraph, boolean)} (with a <code>true</code> 2nd argument)
     * instead of this method.
     * 
     * @param graph the DENOPTIMGraph to be transformed into a 3D molecule
     * @return the <code>IAtomContainer</code> representation of the molecular
     * system represented by the DENOPTIMGraph.
     * @throws DENOPTIMException
     */

    public IAtomContainer convertGraphTo3DAtomContainer(DENOPTIMGraph graph)
                                                        throws DENOPTIMException
    {
    	return convertGraphTo3DAtomContainer(graph,false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Created a three-dimensional molecular representation from a given 
     * DENOPTIMGraph. The conversion creates also two maps to retrace the 
     * attachment points within the final 3D structure both based on 
     * the ID of the source DENOPTIMVertex and correspondence to DENOPTIMEdge.
     * To retrieve this information see methods 
     * {@link ThreeDimTreeBuilder#getApsPerVertexId()},
     * {@link ThreeDimTreeBuilder#getApsPerAtom()} and
     * {@link ThreeDimTreeBuilder#getApsPerEdge()}. 
     * In addition each atom is blended with the unique index
     * of the DENOPTIMVertex corresponding to the molecular fragment to which
     * the atom belongs.
     * Calling this method cleans all the fields from previously existing data
     * (see method {@link ThreeDimTreeBuilder#clean()}).
     * 
     * @param graph the DENOPTIMGraph to be transformed into a 3D molecule
     * @param removeUsedRCAs when <code>true</code> this method will remove 
     * used RCAs (the content of ring-closing vertexes, RCVs) 
     * and add bonds to close the rings
     * defined by the DENOPTIMRings in the graph (does not alter the graph).
     * Does not change unused RCVs. Unused RCVs should have been already 
     * replaced by capping groups (or removed, if no capping needed), 
     * with the 
     * {@link GraphConversionTool#replaceUnusedRCVsWithCapps(DENOPTIMGraph)}
     * method.
     * @return the <code>AtomContainer</code> representation
     * @throws DENOPTIMException
     */

    public IAtomContainer convertGraphTo3DAtomContainer(DENOPTIMGraph graph,
            boolean removeUsedRCAs) 
                    throws DENOPTIMException
    {
        return convertGraphTo3DAtomContainer(graph,removeUsedRCAs,true);
    }
    
//------------------------------------------------------------------------------

    /**
     * Created a three-dimensional molecular representation from a given 
     * DENOPTIMGraph. The conversion creates also two maps to retrace the 
     * attachment points within the final 3D structure both based on 
     * the ID of the source DENOPTIMVertex and correspondence to DENOPTIMEdge.
     * To retrieve this information see methods 
     * {@link ThreeDimTreeBuilder#getApsPerVertexId()},
     * {@link ThreeDimTreeBuilder#getApsPerAtom()} and
     * {@link ThreeDimTreeBuilder#getApsPerEdge()}. 
     * In addition each atom is blended with the unique index
     * of the DENOPTIMVertex corresponding to the molecular fragment to which
     * the atom belongs.
     * Calling this method cleans all the fields from previously existing data
     * (see method {@link ThreeDimTreeBuilder#clean()}).
     * 
     * @param graph the DENOPTIMGraph to be transformed into a 3D molecule
     * @param removeUsedRCAs when <code>true</code> this method will remove 
     * used RCAs (the content of ring-closing vertexes, RCVs) 
     * and add bonds to close the rings
     * defined by the DENOPTIMRings in the graph (does not alter the graph).
     * Does not change unused RCVs. Unused RCVs should have been already 
     * replaced by capping groups (or removed, if no capping needed), 
     * with the 
     * {@link GraphConversionTool#replaceUnusedRCVsWithCapps(DENOPTIMGraph)}
     * method.
     * @param setCDKRequirements when <code>true</code> this method will ensure
     * that the CDK requirements for IAtomContainers are all met. Namely,
     * no bond order is 'undefined' and that intrinsic hydrogen is zero for all
     * atoms (i.e., internal convention in DENOPTIM: all atoms are explicit).
     * @return the <code>AtomContainer</code> representation
     * @throws DENOPTIMException
     */

    public IAtomContainer convertGraphTo3DAtomContainer(DENOPTIMGraph graph,
            boolean removeUsedRCAs, boolean setCDKRequirements) 
                    throws DENOPTIMException
    {
        // Clean all and makes this.mol = builder.newAtomContainer();
        this.clean();
        
        this.graph = graph;
        
        // WARNING: assumption that the graph is an healthy spanning tree, as
        // it should always be in DENOPTIM.
        DENOPTIMVertex rootVrtx = this.graph.getSourceVertex();
        int idRootVrtx = rootVrtx.getVertexId();
        
        IAtomContainer iacRootVrtx = null;
        if (rootVrtx.containsAtoms())
        {
            iacRootVrtx = rootVrtx.getIAtomContainer();
        
            if (iacRootVrtx == null)
            {
                String msg = "ThreeDimTreeBuilder found a building block daclaring "
                        + "to containg atoms, but returning null atom container. "
                        + "Building blocks: " + rootVrtx;
                throw new IllegalArgumentException(msg);
            }
            
            for (IAtom atm : iacRootVrtx.atoms())
            {
                atm.setProperty(DENOPTIMConstants.ATMPROPVERTEXID,idRootVrtx);
            }
            mol.add(iacRootVrtx);
        }
        
        // Store APs in maps
        ArrayList<DENOPTIMAttachmentPoint> apsOnThisFrag = new ArrayList<>();
        for (DENOPTIMAttachmentPoint ap : rootVrtx.getAttachmentPoints())
        {
            // For first vertex the atomPositionNumber remains the same
            ap.setAtomPositionNumberInMol(ap.getAtomPositionNumber());
            apsOnThisFrag.add(ap);
            if (rootVrtx.containsAtoms())
            {
                IAtom srcAtm = iacRootVrtx.getAtom(ap.getAtomPositionNumber());
                if (apsPerAtom.containsKey(srcAtm))
                {
                    apsPerAtom.get(srcAtm).add(ap);
                }
                else
                {
                    ArrayList<DENOPTIMAttachmentPoint> apsOnThisAtm =
                            new ArrayList<>();
                    apsOnThisAtm.add(ap);
                    apsPerAtom.put(srcAtm,apsOnThisAtm);
                }
            }
        }
        apsPerVertexId.put(idRootVrtx,apsOnThisFrag);
        
        // Recursion on all branches of the tree (i.e., all incident edges)
        for (DENOPTIMEdge edge : graph.getEdgesWithSrc(rootVrtx))
        {
            // Get the AP from the current vertex to the next
            DENOPTIMAttachmentPoint apSrc = edge.getSrcAP();

            // Add APs to the map of APs per Edges
            ArrayList<DENOPTIMAttachmentPoint> apOnThisEdge =
                                      new ArrayList<DENOPTIMAttachmentPoint>();
            apOnThisEdge.add(apSrc);
            apOnThisEdge.add(edge.getTrgAP());
            apsPerEdge.put(edge,apOnThisEdge);
            
            if (rootVrtx.containsAtoms())
            {
                Point3d trgPtApSrc = new Point3d(apSrc.getDirectionVector());
                Point3d srcPtApSrc = new Point3d(iacRootVrtx.getAtom(
                        apSrc.getAtomPositionNumber()).getPoint3d());
            
                // Append next building block on AP-vector - start recursion
                append3DFragmentsViaEdges(apSrc.getAtomPositionNumber(),
                        srcPtApSrc,trgPtApSrc,edge,removeUsedRCAs);
            } else {
                // Append next building block - start recursion
                Point3d pt = getRandomPoint();
                append3DFragmentsViaEdges(-1, pt, pt, edge, removeUsedRCAs);
            }
        }
        
        // loop through the rings and make chords
        /*
        // TODO del: this is replaced by the call to demoveRCAs. However, this 
        // is an example of how to navigate the graph without dealing with atom
        // indexes as currently done in GraphConversionTool
        if (removeRCAs)
        {
            for (int i=0; i<this.graph.getRings().size(); i++)
            {
                DENOPTIMRing r = this.graph.getRings().get(i);
                DENOPTIMVertex vH = r.getHeadVertex();
                DENOPTIMVertex vT = r.getTailVertex();
                int vH_id = vH.getVertexId();
                int vT_id = vT.getVertexId();
                
                DENOPTIMAttachmentPoint apH = apsPerVertexId.get(vH_id).get(0);
                DENOPTIMAttachmentPoint apT = apsPerVertexId.get(vT_id).get(0);
                
                IBond bndToH = null;
                IBond bndToT = null;
                boolean doneH = false;
                boolean doneT = false;
                for (IBond b : apsPerBond.keySet())
                {
                	ArrayList<DENOPTIMAttachmentPoint> aps = apsPerBond.get(b);
                	if (!doneH && aps.contains(apH))
                	{
                		bndToH = b;
                		doneH = true;
                	}
                	if (!doneT && aps.contains(apT))
                	{
                		bndToT = b;
                		doneT = true;
                	}
                	if (doneT && doneH)
                	{
                		continue;
                	}
                }
                
                IAtom atmH = null;
                if (apsPerAtom.get(bndToH.getAtom(0)).contains(apH))
                {
                	atmH = bndToH.getAtom(1);
                } else if (apsPerAtom.get(bndToH.getAtom(1)).contains(apH))
                {
                	atmH = bndToH.getAtom(0);
                } else {
                	throw new DENOPTIMException("Could not identify atom "
                			+ "holding RCA "+vH_id);
                }
                
                IAtom atmT = null;
                if (apsPerAtom.get(bndToT.getAtom(0)).contains(apT))
                {
                	atmT = bndToT.getAtom(1);
                } else if (apsPerAtom.get(bndToT.getAtom(1)).contains(apT))
                {
                	atmT = bndToT.getAtom(0);
                } else {
                	throw new DENOPTIMException("Could not identify atom "
                			+ "holding RCA "+vH_id);
                }

                int idSrcH = mol.getAtomNumber(atmH);
                int idSrcT = mol.getAtomNumber(atmT);

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
        */

        if (removeUsedRCAs)
        {
        	// This is where we make the rings-closing bonds.
        	// Unused RCAs should have been already replaced by capping groups 
            // (or removed, if no capping needed), by changing the graph with
            // GraphConversionTool.removeUnusedRCVs
            // So, this will deal only with used RCAs.
            
            // WARNING: since we are changing the atom list, we need to make
            // sure we retail consistency between the changing atom list of
            // the atom indexes stored in the APs, and in particular, the 
            // index returned by getAtomPositionNumberInMol.
        	DENOPTIMMoleculeUtils.removeUsedRCA(mol, this.graph);
        }
        
        if (setCDKRequirements)
        {
            DENOPTIMMoleculeUtils.setZeroImplicitHydrogensToAllAtoms(mol);
            DENOPTIMMoleculeUtils.ensureNoUnsetBondOrders(mol);
        }
        
        if (debug)
        {
            String file = "/tmp/iacTree.sdf";
            System.out.println("Writing tree-like IAtomContainer to " + file);
            IAtomContainer cmol = builder.newAtomContainer();
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
                    IAtom atm = new PseudoAtom(String.valueOf(i), 
                                          new Point3d(ap.getDirectionVector()));
                    System.out.println("Vertex: "+v+" AP-"+i+" = "+ap);
                    cmol.addAtom(atm);
                    IBond bnd = new Bond(cmol.getAtom(
                    		ap.getAtomPositionNumber()),
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
        
        mol.setProperty(DENOPTIMConstants.GCODETAG, this.graph.getGraphId());
        mol.setProperty(DENOPTIMConstants.GRAPHTAG, this.graph.toString());
        mol.setProperty(DENOPTIMConstants.GRAPHJSONTAG, this.graph.toJson());
        if (this.graph.getLocalMsg() != null 
                && !this.graph.getLocalMsg().toString().equals(""))
        {
            mol.setProperty(DENOPTIMConstants.GMSGTAG,this.graph.getLocalMsg());
        }
        this.conversionCompleted = true;

        return mol;
    }

//------------------------------------------------------------------------------

    /**
     * Recursive method that appends branches of building blocks following the 
     * edges of the graph. The connection is controlled by the geometries of the
     * attachment point on a growing molecule and that of the
     * attachment point of the incoming vertex, i.e., the child/target vertex 
     * connected via the <code>edge</code> argument. If the latter contains
     * no atoms, then we simply return. Thus, vertexes that contain no atoms are
     * not expected to have more than one incident edge (i.e., the edge to their
     * parent vertex).
     *
     * @param idSrcAtmA the index of the atom holding the AP on the growing
     * molecules, or a negative number if no such bond exists and, thus, we do
     * not add any chemical bond between atoms of the growing molecule and the
     * incoming building block.
     * @param srcApA the point in space corresponding to the source of the
     * attachment point vector on the growing molecule
     * @param trgApA the point in space corresponding to the end of the
     * attachment point vector on the growing molecule
     * @param edge the <code>DENOPTIMEdge</code> corresponding to the
     * connection this method is asked to make between 3D molecular
     * fragments
     * @param removeUsedRCAs when <code>true</code> this method will remove 
     * used RCAs (the content of ring-closing vertexes, RCVs) 
     * and add bonds to close the rings
     * defined by the DENOPTIMRings in the graph (does not alter the graph).
     * Does not change unused RCVs.  Unused RCVs should have been already 
     * replaced by capping groups (or removed, if no capping needed), 
     * with the 
     * {@link GraphConversionTool#replaceUnusedRCVsWithCapps(DENOPTIMGraph)}
     * method.
     * @throws DENOPTIMException
     */

    private void append3DFragmentsViaEdges(int idSrcAtmA, Point3d srcApA, 
                     Point3d trgApA, DENOPTIMEdge edge, boolean removeUsedRCAs) 
                    		 throws DENOPTIMException
    {   
        if (debug)
        {
            System.err.println("Appending 3D fragment via edge: "+edge);
            System.err.println("#Atoms on growing mol: "+mol.getAtomCount());
        }

        // Get the incoming fragment and its AP
        DENOPTIMVertex inVtx = edge.getTrgAP().getOwner();
        DENOPTIMAttachmentPoint apB = edge.getTrgAP();
        
        //Used to keep track of which atom comes from which vertex
        int idInVrx = inVtx.getVertexId();
        
        if (debug)
        {
            System.err.println("Incoming vertex : "+inVtx);
        }
        
        int preNumAtms = mol.getAtomCount();
        IAtomContainer inFrag = null;
        if (inVtx.containsAtoms())
        {
            inFrag = inVtx.getIAtomContainer();
            
            if (inFrag == null)
            {
                String msg = "ThreeDimTreeBuilder found a building block "
                        + "daclaring "
                        + "to containg atoms, but returning a null atom "
                        + "container. "
                        + "Problematic building block is bbID=" 
                        + inVtx.getBuildingBlockId() + " bbType="
                        + inVtx.getBuildingBlockType();
                throw new IllegalArgumentException(msg);
            }
            
            if (debug)
            {
                System.err.println("Incoming IAC #atoms: " + 
                        inFrag.getAtomCount());
            }

            if (alignIn3D)
            {
                // Define the roto-translation operation that aligns
                // the incoming building block to the growing molecule. 
                // To this end,
                // we work on the APs that define the spatial relation 
                // between the
                // parent vertex, which is the growing molecule (i.e., ApA)
                // and that on the incoming building block (i.e., ApB).
                Point3d trgApB = new Point3d(apB.getDirectionVector());
                Point3d srcApB = new Point3d(DENOPTIMMoleculeUtils.getPoint3d(
                        inFrag.getAtom(apB.getAtomPositionNumber())));
        
                // Translate atoms and APs of incoming building block so that 
                // trgApB is on srcApA
                Point3d tr1 = new Point3d();
                tr1.sub(trgApB,srcApA);
                for (IAtom atm : inFrag.atoms())
                {
                	atm.setPoint3d(DENOPTIMMoleculeUtils.getPoint3d(atm));
                    atm.getPoint3d().sub(tr1);
                }
                for (DENOPTIMAttachmentPoint ap : inVtx.getAttachmentPoints())
                {
                    Point3d pt = new Point3d(ap.getDirectionVector());
                    pt.sub(tr1);
                    ap.setDirectionVector(new double[] {pt.x, pt.y, pt.z});
                }
                trgApB = new Point3d(apB.getDirectionVector());
                srcApB = new Point3d(DENOPTIMMoleculeUtils.getPoint3d(
                        inFrag.getAtom(apB.getAtomPositionNumber())));
        
                //Get Vectors ApA and ApB (NOTE: inverse versus of ApB!!!)
                Vector3d vectApA = new Vector3d();
                Vector3d vectApB = new Vector3d();
                vectApA.sub(trgApA,srcApA);
                vectApB.sub(srcApB,trgApB);
                vectApA.normalize();
                vectApB.normalize();
        
                if (debug)
                {
                    System.err.println("After 1st translation and before "
                            + "rotation");
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
        
                    // Rotate atoms of incoming building block
                    for (IAtom atm : inFrag.atoms())
                    {
                    	//At this point all atoms have point3d
                        atm.getPoint3d().sub(srcApA);
                        rotMat.transform(atm.getPoint3d());
                        atm.getPoint3d().add(srcApA);
                    }
                    // Rotate APs of incoming building block
                    for (DENOPTIMAttachmentPoint ap : 
                        inVtx.getAttachmentPoints())
                    {
                        Point3d pt = new Point3d(ap.getDirectionVector());
                        pt.sub(srcApA);
                        rotMat.transform(pt);
                        pt.add(srcApA);
                        ap.setDirectionVector(new double[] {pt.x, pt.y, pt.z});
                    }
        
                    // Update points defining AP vector
                    trgApB = new Point3d(apB.getDirectionVector());
                    srcApB = new Point3d(inFrag.getAtom(
                            apB.getAtomPositionNumber()).getPoint3d());
                }
                else
                {
                    if (debug)
                    {
                        System.err.println("RotAng below threshold. "
                                + "No rotation.");
                    }
                }
        
                if (debug)
                {
                    System.err.println("After rotation before 2nd translation");
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
                    if (edge.getSrcAP().getOwner().isRCV() || 
                            edge.getTrgAP().getOwner().isRCV())
                    {
                        edgeToRCA = true;
                    }
                }
        
                // Get translation vector accounting for different length of 
                // the APs
                vectApA.sub(trgApA,srcApA);
                vectApB.sub(srcApB,trgApB);
        
                Point3d tr2 = new Point3d();
                if (edgeToRCA)
                {
                    // Here we set translation vector as to move the 
                    // incoming frag
                    // of the length of the longest AP. This is to place RCA at 
                    // a bonding distance from the connected atom
                    if (vectApA.length() > vectApB.length())
                    {
                        tr2.add(vectApA);
                    }
                    else
                    {
                        // NOTE: in this case the RCA is on mol, thus no 
                        // translation
        		        // is needed because trgApB is already on top of srcApA
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
                        String str = "ERROR! NaN coordinated from "
                                + "rototranslation of 3D fragment. "
                                + "Check source code. Atm: "+atm;
                        throw new DENOPTIMException(str);
                    }
                }
                for (DENOPTIMAttachmentPoint ap : inVtx.getAttachmentPoints())
                {
                    Point3d pt = new Point3d(ap.getDirectionVector());
                    pt.add(tr2);
                    if ((pt.x != pt.x ) || (pt.y != pt.y ) || (pt.z != pt.z ))
                    {
                        String str = "ERROR! NaN coordinated from "
                                + "rototranslation of 3D fragment's APs. "
                                + "Check source code.";
                        throw new DENOPTIMException(str);
                    }
                    ap.setDirectionVector(new double[] {pt.x, pt.y, pt.z});
                }
            }
    
            // Store vertex ID on atoms
            for (IAtom atm : inFrag.atoms())
            {
                atm.setProperty(DENOPTIMConstants.ATMPROPVERTEXID,idInVrx);
            }
            
            // Append atoms of new fragment to the growing molecule
            mol.add(inFrag);
        }
        
        // Make the bond (if any) according to graph's edge
        BondType bndTyp = edge.getBondType();
        if (bndTyp.hasCDKAnalogue() && idSrcAtmA>-1 && inVtx.containsAtoms())
        {
            IBond bnd = new Bond(mol.getAtom(idSrcAtmA),
                    inFrag.getAtom(apB.getAtomPositionNumber()), 
                    bndTyp.getCDKOrder());
            mol.addBond(bnd);
            
            // Store the APs related to the new bond
            ArrayList<DENOPTIMAttachmentPoint> apsOnBond = new ArrayList<>();
            apsOnBond.add(edge.getSrcAP());
            apsOnBond.add(edge.getTrgAP());
            apsPerBond.put(bnd,apsOnBond);
        }
        
        // Store APs per building block and per atom (if atom exists)
        ArrayList<DENOPTIMAttachmentPoint> apsOnThisFrag = new ArrayList<>();
        for (DENOPTIMAttachmentPoint ap : inVtx.getAttachmentPoints())
        {
            // For vertices other than the first, we adjust the pointer to the
            // AP source atom according to the atom list of the entire molecule
            ap.setAtomPositionNumberInMol(ap.getAtomPositionNumber() 
                    + preNumAtms);
            apsOnThisFrag.add(ap);
            
            if (inVtx.containsAtoms())
            {
                IAtom srcAtm = mol.getAtom(ap.getAtomPositionNumberInMol());
                if (apsPerAtom.containsKey(srcAtm))
                {
                    apsPerAtom.get(srcAtm).add(ap);
                }
                else
                {
                    ArrayList<DENOPTIMAttachmentPoint> apsOnThisAtm =
                            new ArrayList<>();
                    apsOnThisAtm.add(ap);
                    apsPerAtom.put(srcAtm,apsOnThisAtm);
                }
            }
        }
        apsPerVertexId.put(idInVrx, apsOnThisFrag);
        
        // Recursion on all the edges leaving from this fragment
        for (DENOPTIMEdge nextEdge : graph.getEdgesWithSrc(inVtx))
        {
            // Add APs to the map of APs per Edges
            ArrayList<DENOPTIMAttachmentPoint> apOnThisEdge =
                                      new ArrayList<DENOPTIMAttachmentPoint>();
            apOnThisEdge.add(nextEdge.getSrcAP());
            apOnThisEdge.add(nextEdge.getTrgAP());
            apsPerEdge.put(nextEdge,apOnThisEdge);

            if (inVtx.containsAtoms())
            {
                // Get two points defining the src AP vector in 3D
                Point3d trgNextApA = new Point3d(
                        nextEdge.getSrcAP().getDirectionVector());
                Point3d srcNextApA = new Point3d(inFrag.getAtom(
                        nextEdge.getSrcAP().getAtomPositionNumber())
                        .getPoint3d());
    
                // Append fragment on AP-vector and start recursion
                append3DFragmentsViaEdges(
                        nextEdge.getSrcAP().getAtomPositionNumberInMol(), 
                        srcNextApA, trgNextApA, nextEdge, removeUsedRCAs);
            } else {
                // Append next building block - start recursion
                Point3d pt = getRandomPoint();
                append3DFragmentsViaEdges(-1, pt, pt, nextEdge, removeUsedRCAs);
            }
        }
    }
    
//------------------------------------------------------------------------------
    
    private Point3d getRandomPoint()
    {
        double vx = ((double) RandomUtils.getRNG().nextInt(100)) / 100.0;
        double vy = ((double) RandomUtils.getRNG().nextInt(100)) / 100.0;
        double vz = ((double) RandomUtils.getRNG().nextInt(100)) / 100.0;
        Point3d c = new Point3d(0,0,0);
        if (mol.getAtomCount() > 0)
        {
            c = DENOPTIMMoleculeUtils.calculateCentroid(mol);
        }
        return new Point3d(maxCoord*vx+c.x, maxCoord*vy+c.y, maxCoord*vz+c.z);
    }

//------------------------------------------------------------------------------

    /**
     * Cleans the molecule/graph-specific information and restores this builder 
     * to its initial status.
     * Graph and molecular representation as well as other fields are cleaned,
     * while the configuration of the builder remains,
     * i.e., the references to the libraries of building blocks and the 
     * flag controlling alignment of building blocks.
     */

    public void clean()
    {
        this.graph = new DENOPTIMGraph();
        this.mol = builder.newAtomContainer();
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
     * Returns the map of roto-translated APs per each vertex ID. This method 
     * <b>MUST</b> be called after conversion of the <code>DENOPTIMGraph</code> 
     * into <code>AtomContainer</code>, that is, after having called 
     * {@link #convertGraphTo3DAtomContainer(DENOPTIMGraph)}.
     *
     * @return the <code>Map</code> of all roto-translated 
     * <code>DENOPTIMAttachmentPoint</code> per each vertex ID
     * @throws DENOPTIMException
     */

    //TODO-V3 evaluate removal of this map
    public Map<Integer,ArrayList<DENOPTIMAttachmentPoint>> getApsPerVertexId()
                                                        throws DENOPTIMException
    {
        if (!conversionCompleted)
        {
            String str = "Misuse of ThreeDimTreeBuilder. Cannot return map of "
                         + "roto-translated APs per each vertex ID before "
                         + "building the 3D representation.";
            throw new DENOPTIMException(str);
        }
        return apsPerVertexId;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the map of roto-translated APs per each DENOPTIMEdge. This method 
     * <b>MUST</b> be called after conversion of the <code>DENOPTIMGraph</code> 
     * into <code>AtomContainer</code>, that is, after having called 
     * {@link #convertGraphTo3DAtomContainer(DENOPTIMGraph) this} method.
     *
     * @return the <code>Map</code> of all roto-translated 
     * <code>DENOPTIMAttachmentPoint</code> per each <code>DENOPTIMEdge</code>
     * @throws DENOPTIMException
     */

    //TODO-V3 evaluate removal of this map
    public Map<DENOPTIMEdge,ArrayList<DENOPTIMAttachmentPoint>> getApsPerEdge()
                                                        throws DENOPTIMException
    {
        if (!conversionCompleted)
        {
            String str = "Misuse of ThreeDimTreeBuilder. Cannot return map of "
                         + "roto-translated APs per each DENOPTIMEdge before "
                         + "building the 3D representation.";
            throw new DENOPTIMException(str);
        }
        return apsPerEdge;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the map of roto-translated APs per each source atom. This method 
     * <b>MUST</b> be called after conversion of the <code>DENOPTIMGraph</code> 
     * into <code>AtomContainer</code>, that is, after having called 
     * {@link #convertGraphTo3DAtomContainer(DENOPTIMGraph) this} method.
     *
     * @return the <code>Map</code> of all roto-translated 
     * <code>DENOPTIMAttachmentPoint</code> per each <code>IAtom</code>
     * @throws DENOPTIMException
     */

    //TODO-V3 evaluate removal of this map
    public Map<IAtom,ArrayList<DENOPTIMAttachmentPoint>> getApsPerAtom()
                                                        throws DENOPTIMException
    {
        if (!conversionCompleted)
        {
            String str = "Misuse of ThreeDimTreeBuilder. Cannot return map of "
                         + "roto-translated APs per each Atom before "
                         + "building the 3D representation.";
            throw new DENOPTIMException(str);
        }
        return apsPerAtom;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the map of roto-translated APs per each bond corresponding to 
     * and inter-fragment connection. This method 
     * <b>MUST</b> be called after conversion of the <code>DENOPTIMGraph</code> 
     * into <code>AtomContainer</code>, that is, after having called 
     * {@link #convertGraphTo3DAtomContainer(DENOPTIMGraph) this} method.
     *
     * @return the <code>Map</code> of all roto-translated 
     * <code>DENOPTIMAttachmentPoint</code> per each <code>IBond</code>
     * @throws DENOPTIMException
     */

    //TODO-V3 evaluate removal of this map
    public Map<IBond,ArrayList<DENOPTIMAttachmentPoint>> getApsPerBond()
                                                        throws DENOPTIMException
    {
        if (!conversionCompleted)
        {
            String str = "Misuse of ThreeDimTreeBuilder. Cannot return map of "
                         + "roto-translated APs per each Atom before "
                         + "building the 3D representation.";
            throw new DENOPTIMException(str);
        }
        return apsPerBond;
    }

//------------------------------------------------------------------------------

}
