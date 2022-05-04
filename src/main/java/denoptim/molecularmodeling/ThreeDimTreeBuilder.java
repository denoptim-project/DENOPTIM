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

package denoptim.molecularmodeling;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.openscience.cdk.Bond;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.DGraph;
import denoptim.graph.Edge;
import denoptim.graph.Edge.BondType;
import denoptim.graph.Ring;
import denoptim.graph.Vertex;
import denoptim.io.DenoptimIO;
import denoptim.programs.RunTimeParameters;
import denoptim.utils.GraphConversionTool;
import denoptim.utils.GraphUtils;
import denoptim.utils.MathUtils;
import denoptim.utils.MoleculeUtils;
import denoptim.utils.Randomizer;


/**
 * Tool to build build three-dimensional (3D) tree-like molecular structures 
 * from {@link DGraph}. The molecular structure is assembled from the
 * building blocks by attaching any set of atoms/pseudo-atoms contained in the
 * building block, and (optionally) aligning each incoming building block by 
 * using the geometric parameters defined in their attachment points.
 * By default, this builder does align building blocks, but method 
 * {@link ThreeDimTreeBuilder#setAlignBBsIn3D(boolean)} allows to configure this
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
     * Controls the maximum distance between a random place where unanchored
     * building blocks are placed and the system centroid. 
     */
    private double maxCoord = 20.0;
    
    /**
     * Flag controlling whether to align building blocks according to the AP
     * vectors or not. By default, we do align building blocks.
     */
    private boolean alignIn3D = true;
    
    /**
     * Private builder of atom containers
     */
    private IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
    
    /**
     * Program-specific logger
     */
    private Logger logger = Logger.getLogger("3dTreeBuilderLogger");
    
    /**
     * Program-specific randomizer
     */
    private Randomizer randomizer = new Randomizer();
    
    private static final String NL = DENOPTIMConstants.EOL;
    
//------------------------------------------------------------------------------
    
    /**
     * Constructor providing the program-specific logger and randomizer.
     * @param 
     */
    public ThreeDimTreeBuilder(Logger logger, Randomizer randomizer)
    {
        this.logger = logger;
        this.randomizer = randomizer;
    }

//------------------------------------------------------------------------------
    
    /**
     * Sets the flag that controls whether building blocks have to be aligned
     * according to the AP vectors or not. 
     * @param align the new value. Use <code>true</code> to align building 
     * blocks (default), or <code>false</code> to prevent alignment.
     */
    public void setAlignBBsIn3D(boolean align)
    {
        this.alignIn3D = align;
    }
    
//------------------------------------------------------------------------------

    /**
     * Created a three-dimensional molecular representation from a given 
     * {@link DGraph}. The conversion creates also two maps to retrace the 
     * attachment points within the final 3D structure both based on 
     * the ID of the source DENOPTIMVertex and correspondence to DENOPTIMEdge.
     * To retrieve this information see properties
     * {@link DENOPTIMConstants#MOLPROPAPxATOM},
     * {@link DENOPTIMConstants#MOLPROPAPxBOND},
     * {@link DENOPTIMConstants#MOLPROPAPxVID} and
     * {@link DENOPTIMConstants#MOLPROPAPxEDGE}. 
     * In addition each atom is blended with the unique index
     * of the DENOPTIMVertex corresponding to the molecular fragment to which
     * the atom belongs.
     * 
     * This method does not remove ring-closing attractors. You can remove them
     * by calling {@link ThreeDimTreeBuilder#convertGraphTo3DAtomContainer(
     * DGraph, boolean)} (with a <code>true</code> 2nd argument)
     * instead of this method.
     * 
     * @param graph the {@link DGraph} to be transformed into a 3D molecule
     * @return the <code>IAtomContainer</code> representation of the molecular
     * system represented by the {@link DGraph}.
     * @throws DENOPTIMException
     */

    public IAtomContainer convertGraphTo3DAtomContainer(DGraph graph)
                                                        throws DENOPTIMException
    {
    	return convertGraphTo3DAtomContainer(graph, false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Created a three-dimensional molecular representation from a given 
     * {@link DGraph}. The conversion creates also two maps to retrace the 
     * attachment points within the final 3D structure both based on 
     * the ID of the source DENOPTIMVertex and correspondence to DENOPTIMEdge.
     * To retrieve this information see properties
     * {@link DENOPTIMConstants#MOLPROPAPxATOM},
     * {@link DENOPTIMConstants#MOLPROPAPxBOND},
     * {@link DENOPTIMConstants#MOLPROPAPxVID} and
     * {@link DENOPTIMConstants#MOLPROPAPxEDGE}.  
     * In addition each atom is blended with the unique index
     * of the DENOPTIMVertex corresponding to the molecular fragment to which
     * the atom belongs.
     * 
     * @param graph the {@link DGraph} to be transformed into a 3D molecule
     * @param removeUsedRCAs when <code>true</code> this method will remove 
     * used RCAs (the content of ring-closing vertexes, RCVs) 
     * and add bonds to close the rings
     * defined by the DENOPTIMRings in the graph (does not alter the graph).
     * Does not change unused RCVs. Unused RCVs should have been already 
     * replaced by capping groups (or removed, if no capping needed), 
     * with the 
     * {@link GraphConversionTool#replaceUnusedRCVsWithCapps(DGraph)}
     * method.
     * @return the <code>AtomContainer</code> representation
     * @throws DENOPTIMException
     */

    public IAtomContainer convertGraphTo3DAtomContainer(DGraph graph,
            boolean removeUsedRCAs) throws DENOPTIMException
    {
        return convertGraphTo3DAtomContainer(graph,removeUsedRCAs,true, false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Created a three-dimensional molecular representation from a given 
     * {@link DGraph}. The conversion creates also two maps to retrace the 
     * attachment points within the final 3D structure both based on 
     * the ID of the source {@link Vertex} and correspondence to {@link Edge}.
     * To retrieve this information see properties
     * {@link DENOPTIMConstants#MOLPROPAPxATOM},
     * {@link DENOPTIMConstants#MOLPROPAPxBOND},
     * {@link DENOPTIMConstants#MOLPROPAPxVID} and
     * {@link DENOPTIMConstants#MOLPROPAPxEDGE}. 
     * In addition each atom is blended with the unique index
     * of the {@link Vertex} corresponding to the molecular fragment to which
     * the atom belongs.
     * 
     * @param graph the {@link DGraph} to be transformed into a 3D molecule
     * @param removeUsedRCAs when <code>true</code> this method will remove 
     * used RCAs (the content of ring-closing vertexes, RCVs) 
     * and add bonds to close the rings
     * defined by the {@link Ring} in the graph (does not alter the graph).
     * Does not change unused RCVs. Unused RCVs should have been already 
     * replaced by capping groups (or removed, if no capping needed), 
     * with the 
     * {@link GraphConversionTool#replaceUnusedRCVsWithCapps(DGraph)}
     * method.
     * @param setCDKRequirements when <code>true</code> this method will ensure
     * that the CDK requirements for IAtomContainers are all met. Namely,
     * no bond order is 'undefined' and that intrinsic hydrogen is zero for all
     * atoms (i.e., internal convention in DENOPTIM: all atoms are explicit).
     * @return rebuild when <code>true</code> the chemical representation of 
     * every building block is rebuilt ignoring previously available structures.
     * @return the <code>AtomContainer</code> representation
     * @throws DENOPTIMException
     */

    //NB: there is unused code that could turn out useful in debugging.
    @SuppressWarnings("unused") 
    public IAtomContainer convertGraphTo3DAtomContainer(DGraph graph,
            boolean removeUsedRCAs, boolean setCDKRequirements, boolean rebuild) 
                    throws DENOPTIMException
    {
        IAtomContainer mol = builder.newAtomContainer();
        
        // WARNING: assumption that the graph is an healthy spanning tree, as
        // it should always be in DENOPTIM.
        Vertex rootVrtx = graph.getSourceVertex();
        int idRootVrtx = rootVrtx.getVertexId();
        
        IAtomContainer iacRootVrtx = null;
        if (rootVrtx.containsAtoms())
        {
            iacRootVrtx = rootVrtx.getIAtomContainer(logger, randomizer, 
                    removeUsedRCAs, rebuild);
        
            if (iacRootVrtx == null)
            {
                String msg = "ThreeDimTreeBuilder found a building block daclaring "
                        + "to containg atoms, but returning null atom container. "
                        + "Building blocks: " + rootVrtx;
                throw new IllegalArgumentException(msg);
            }

            for (IAtom atm : iacRootVrtx.atoms())
            {
                Object prevPath = atm.getProperty(
                        DENOPTIMConstants.ATMPROPVERTEXPATH);
                if (prevPath!=null)
                {
                    atm.setProperty(DENOPTIMConstants.ATMPROPVERTEXPATH, 
                            idRootVrtx + ", " + prevPath.toString());
                } else {
                    atm.setProperty(DENOPTIMConstants.ATMPROPVERTEXPATH, 
                            idRootVrtx);
                }
                atm.setProperty(DENOPTIMConstants.ATMPROPVERTEXID,idRootVrtx);
            }
            mol.add(iacRootVrtx);
        }
        
        // Store APs in maps
        Map<Integer,ArrayList<AttachmentPoint>> apsPerVertexId = new HashMap<>();
        Map<Edge,ArrayList<AttachmentPoint>> apsPerEdge = new HashMap<>();
        Map<IAtom,ArrayList<AttachmentPoint>> apsPerAtom = new HashMap<>();
        Map<IBond,ArrayList<AttachmentPoint>> apsPerBond = new HashMap<>();
        ArrayList<AttachmentPoint> apsOnThisFrag = new ArrayList<>();
        for (AttachmentPoint ap : rootVrtx.getAttachmentPoints())
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
                    ArrayList<AttachmentPoint> apsOnThisAtm =
                            new ArrayList<>();
                    apsOnThisAtm.add(ap);
                    apsPerAtom.put(srcAtm,apsOnThisAtm);
                }
            }
        }
        apsPerVertexId.put(idRootVrtx,apsOnThisFrag);
        
        // Recursion on all branches of the tree (i.e., all incident edges)
        for (Edge edge : graph.getEdgesWithSrc(rootVrtx))
        {
            // Get the AP from the current vertex to the next
            AttachmentPoint apSrc = edge.getSrcAP();

            // Add APs to the map of APs per Edges
            ArrayList<AttachmentPoint> apOnThisEdge =
                                      new ArrayList<AttachmentPoint>();
            apOnThisEdge.add(apSrc);
            apOnThisEdge.add(edge.getTrgAP());
            apsPerEdge.put(edge,apOnThisEdge);
            
            if (rootVrtx.containsAtoms())
            {
                Point3d trgPtApSrc = new Point3d(apSrc.getDirectionVector());
                Point3d srcPtApSrc = new Point3d(
                        MoleculeUtils.getPoint3d(iacRootVrtx.getAtom(
                        apSrc.getAtomPositionNumber())));
                
                // Append next building block on AP-vector - start recursion
                append3DFragmentsViaEdges(mol, graph,
                        apSrc.getAtomPositionNumber(),
                        srcPtApSrc,trgPtApSrc,edge,removeUsedRCAs, rebuild,
                        apsPerVertexId,apsPerEdge,apsPerAtom,apsPerBond);
            } else {
                // Append next building block - start recursion
                Point3d pt = getRandomPoint(mol);
                append3DFragmentsViaEdges(mol, graph, -1, pt, pt, edge, 
                        removeUsedRCAs, rebuild,
                        apsPerVertexId,apsPerEdge,apsPerAtom,apsPerBond);
            }
        }

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
        	MoleculeUtils.removeUsedRCA(mol, graph, logger);
        }
        
        if (setCDKRequirements)
        {
            MoleculeUtils.setZeroImplicitHydrogensToAllAtoms(mol);
            MoleculeUtils.ensureNoUnsetBondOrders(mol);
        }
        
        // Code that may turn out useful for deep level debugging
        if (false)
        {
            DenoptimIO.writeGraphToJSON(new File("/tmp/graph.json"), graph); 
            StringBuilder sb = new StringBuilder();
            String file = "/tmp/iacTree.sdf";
            sb.append("Writing tree-like IAtomContainer to " + file+NL);
            IAtomContainer cmol = builder.newAtomContainer();
            try
            {
                cmol = mol.clone();
            }
            catch (Throwable t)
            {
                throw new DENOPTIMException(t);
            }
            
            sb.append("AP-per-VertexID"+NL);
            int i=0;
            for (int v : apsPerVertexId.keySet())
            {
                ArrayList<AttachmentPoint> aps = apsPerVertexId.get(v);
                for (AttachmentPoint ap : aps)
                {
                    i++;
                    IAtom atm = new PseudoAtom(String.valueOf(i), 
                                          new Point3d(ap.getDirectionVector()));
                    sb.append("Vertex: "+v+" AP-"+i+" = "+ap+NL);
                    cmol.addAtom(atm);
                    IBond bnd = new Bond(cmol.getAtom(
                    		ap.getAtomPositionNumber()),
                    		cmol.getAtom(mol.getAtomCount()+i-1),
                    		IBond.Order.SINGLE);
                    cmol.addBond(bnd);
                }
            }
            DenoptimIO.writeSDFFile(file, mol, false);
            DenoptimIO.writeSDFFile(file, cmol, true);
            sb.append("AP-per-Edge"+NL);
            for (Edge e : apsPerEdge.keySet())
            {
                ArrayList<AttachmentPoint> aps = apsPerEdge.get(e);
                for (AttachmentPoint ap : aps)
                {
                    sb.append("Edge: "+e+" AP = "+ap+NL);
                }
            }
            sb.append("AP-per-Atom"+NL);
            for (IAtom a : apsPerAtom.keySet())
            {
                ArrayList<AttachmentPoint> aps = apsPerAtom.get(a);
                for (AttachmentPoint ap : aps)
                {
                    sb.append("Atom: "+mol.indexOf(a) +" AP = "+ap+NL);
                }
            }
            sb.append("AP-per-Bond"+NL);
            for (IBond b : apsPerBond.keySet())
            {
                ArrayList<AttachmentPoint> aps = apsPerBond.get(b);
                for (AttachmentPoint ap : aps)
                {
                    sb.append("Bond: "+mol.indexOf(b.getAtom(0))
                             +"-"+mol.indexOf(b.getAtom(1))+" AP = "+ap+NL);
                }
            }
            System.out.println(sb.toString());
        }
        
        // Prepare the string-representation of unused APs on this graph
        LinkedHashMap<Integer,List<AttachmentPoint>> freeAPPerAtm =
                new LinkedHashMap<>();
        for (IAtom a : apsPerAtom.keySet())
        {
            int atmID = mol.indexOf(a);
            if (atmID<0)
            {
                // source atom is not anymore there: probably it was RCA and has
                // been removed
                continue;
            }
            
            ArrayList<AttachmentPoint> aps = apsPerAtom.get(a);
            for (AttachmentPoint ap : aps)
            {
                if (ap.isAvailableThroughout())
                {
                    if (freeAPPerAtm.containsKey(atmID))
                    {
                        freeAPPerAtm.get(atmID).add(ap);
                    } else {
                        List<AttachmentPoint> lst = 
                                new ArrayList<AttachmentPoint>();
                        lst.add(ap);
                        freeAPPerAtm.put(atmID,lst);
                    }
                }
            }
        }
        mol.setProperty(DENOPTIMConstants.APSTAG, 
                AttachmentPoint.getAPDefinitionsForSDF(freeAPPerAtm));
        
        // Add usual graph-related string-based data to SDF properties
        GraphUtils.writeSDFFields(mol, graph);
        
        mol.setProperty(DENOPTIMConstants.MOLPROPAPxVID, apsPerVertexId);
        mol.setProperty(DENOPTIMConstants.MOLPROPAPxEDGE, apsPerEdge);
        mol.setProperty(DENOPTIMConstants.MOLPROPAPxATOM, apsPerAtom);
        mol.setProperty(DENOPTIMConstants.MOLPROPAPxBOND, apsPerBond);

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
     * @param mol the container of atoms that is being built.
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
     * {@link GraphConversionTool#replaceUnusedRCVsWithCapps(DGraph)}
     * method.
     * @throws DENOPTIMException
     */

    private void append3DFragmentsViaEdges(IAtomContainer mol, DGraph graph,
            int idSrcAtmA, Point3d srcApA, Point3d trgApA, Edge edge, 
            boolean removeUsedRCAs, boolean rebuild,
            Map<Integer,ArrayList<AttachmentPoint>> apsPerVertexId,
            Map<Edge,ArrayList<AttachmentPoint>> apsPerEdge,
            Map<IAtom,ArrayList<AttachmentPoint>> apsPerAtom,
            Map<IBond,ArrayList<AttachmentPoint>> apsPerBond) 
                    throws DENOPTIMException
    {   
        logger.log(Level.FINE, "Appending 3D fragment via edge: "+edge + NL 
                +"#Atoms on growing mol: "+mol.getAtomCount());
        
        // Get the incoming fragment and its AP
        Vertex inVtx = edge.getTrgAP().getOwner();
        AttachmentPoint apB = edge.getTrgAP();
        
        //Used to keep track of which atom comes from which vertex
        int idInVrx = inVtx.getVertexId();
        
        logger.log(Level.FINE, "Incoming vertex : "+inVtx);
        
        int preNumAtms = mol.getAtomCount();
        IAtomContainer inFrag = null;
        if (inVtx.containsAtoms())
        {
            inFrag = inVtx.getIAtomContainer(logger, randomizer, removeUsedRCAs, 
                    true);
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
            
            logger.log(Level.FINE, "Incoming IAC #atoms: " + 
                    inFrag.getAtomCount());

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
                Point3d srcApB = new Point3d(MoleculeUtils.getPoint3d(
                        inFrag.getAtom(apB.getAtomPositionNumber())));
        
                // Translate atoms and APs of incoming building block so that 
                // trgApB is on srcApA
                Point3d tr1 = new Point3d();
                tr1.sub(trgApB,srcApA);
                for (IAtom atm : inFrag.atoms())
                {
                	atm.setPoint3d(MoleculeUtils.getPoint3d(atm));
                    atm.getPoint3d().sub(tr1);
                }
                for (AttachmentPoint ap : inVtx.getAttachmentPoints())
                {
                    Point3d pt = new Point3d(ap.getDirectionVector());
                    pt.sub(tr1);
                    ap.setDirectionVector(pt);
                }
                trgApB = new Point3d(apB.getDirectionVector());
                srcApB = new Point3d(MoleculeUtils.getPoint3d(
                        inFrag.getAtom(apB.getAtomPositionNumber())));
        
                //Get Vectors ApA and ApB (NOTE: inverse versus of ApB!!!)
                Vector3d vectApA = new Vector3d();
                Vector3d vectApB = new Vector3d();
                vectApA.sub(trgApA,srcApA);
                vectApB.sub(srcApB,trgApB);
                vectApA.normalize();
                vectApB.normalize();
        
                logger.log(Level.FINE, 
                        "After 1st translation and before rotation" + NL
                        + "srcApA "+srcApA+NL
                        + "trgApA "+trgApA+NL
                        + "vectApA "+vectApA+NL
                        + "srcApB "+srcApB+NL
                        + "trgApB "+trgApB+NL
                        + "vectApB "+vectApB);
        
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
                        rotAxis = MathUtils.getNormalDirection(vectApA);
                    }
                    Matrix3d rotMat = new Matrix3d();
        	        rotAxis.normalize();
                    rotMat.set(new AxisAngle4d(rotAxis,rotAng));
        
                    logger.log(Level.FINE,"rotAng "+rotAng+NL
                            +"rotAxis "+rotAxis
                            +"rotMat "+rotMat);
        
                    // Rotate atoms of incoming building block
                    for (IAtom atm : inFrag.atoms())
                    {
                    	//At this point all atoms have point3d
                        atm.getPoint3d().sub(srcApA);
                        rotMat.transform(atm.getPoint3d());
                        atm.getPoint3d().add(srcApA);
                    }
                    // Rotate APs of incoming building block
                    for (AttachmentPoint ap : 
                        inVtx.getAttachmentPoints())
                    {
                        Point3d pt = new Point3d(ap.getDirectionVector());
                        pt.sub(srcApA);
                        rotMat.transform(pt);
                        pt.add(srcApA);
                        ap.setDirectionVector(pt);
                    }
        
                    // Update points defining AP vector
                    trgApB = new Point3d(apB.getDirectionVector());
                    srcApB = new Point3d(inFrag.getAtom(
                            apB.getAtomPositionNumber()).getPoint3d());
                }
                else
                {
                    logger.log(Level.FINE,"RotAng below threshold. No rotation.");
                }
        
                logger.log(Level.FINE, 
                        "After rotation before 2nd translation"+NL
                        + "srcApA "+srcApA+NL
                        + "trgApA "+trgApA+NL
                        + "vectApA "+vectApA+NL
                        + "srcApB "+srcApB+NL
                        + "trgApB "+trgApB+NL
                        + "vectApB "+vectApB);
            
                // Check whether this edge involves a Ring Closing Attractors
                boolean edgeToRCA = false; 
                //if (rcParams.getRCStrategy().equals("BONDOVERLAP"))
                if (true)
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
                for (AttachmentPoint ap : inVtx.getAttachmentPoints())
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
                    ap.setDirectionVector(pt);
                }
            }
    
            // Store vertex ID on atoms
            for (IAtom atm : inFrag.atoms())
            {
                Object prevPath = atm.getProperty(
                        DENOPTIMConstants.ATMPROPVERTEXPATH);
                if (prevPath!=null)
                {
                    atm.setProperty(DENOPTIMConstants.ATMPROPVERTEXPATH, 
                            idInVrx + ", " + prevPath.toString());
                } else {
                    atm.setProperty(DENOPTIMConstants.ATMPROPVERTEXPATH, 
                            idInVrx);
                }
                atm.setProperty(DENOPTIMConstants.ATMPROPVERTEXID,idInVrx);
            }
            
            // Append atoms of new fragment to the growing molecule
            mol.add(inFrag);
        }
        
        // Make the bond (if any) according to graph's edge.
        // NB: we are still using CDK's Bond class, but we might need an 
        // implementation IBond that includes all denoptim's bond types, so
        // that we can deal with cases where bndTyp.hasCDKAnalogue() is false .
        BondType bndTyp = edge.getBondType();
        if (bndTyp.hasCDKAnalogue() && idSrcAtmA>-1 && inVtx.containsAtoms())
        {
            IAtom atmToBind = inFrag.getAtom(apB.getAtomPositionNumber());
            IBond bnd = new Bond(mol.getAtom(idSrcAtmA), atmToBind, 
                    bndTyp.getCDKOrder());
            mol.addBond(bnd);
            
            // Store the APs related to this atom-to-RCA bond
            ArrayList<AttachmentPoint> apsOnBond = new ArrayList<>();
            apsOnBond.add(edge.getSrcAP());
            apsOnBond.add(edge.getTrgAP());
            apsPerBond.put(bnd,apsOnBond);
            
            if (inVtx.isRCV())
            {
                atmToBind.setProperty(DENOPTIMConstants.RCAPROPAPCTORCA, 
                        edge.getSrcAP().getAPClass());
                //Since inVtx is RCV there can be only one ring, if any...
                ArrayList<Ring> rings = graph.getRingsInvolvingVertex(inVtx);
                if (rings.size()>0)
                {
                    // We record the type of the ring-closing bond, not that
                    // of the Atom-to-RCA bond.
                    atmToBind.setProperty(DENOPTIMConstants.RCAPROPCHORDBNDTYP, 
                            rings.get(0).getBondType());
                    atmToBind.setProperty(DENOPTIMConstants.RCAPROPRINGUSER, 
                            rings.get(0));
                } else {
                    //...but if none is there, the APClass is enough
                    atmToBind.setProperty(DENOPTIMConstants.RCAPROPCHORDBNDTYP, 
                            edge.getSrcAP().getAPClass().getBondType());
                }
            }
        }
        
        // Store APs per building block and per atom (if atom exists)
        ArrayList<AttachmentPoint> apsOnThisFrag = new ArrayList<>();
        for (AttachmentPoint ap : inVtx.getAttachmentPoints())
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
                    ArrayList<AttachmentPoint> apsOnThisAtm = 
                            new ArrayList<AttachmentPoint>();
                    apsOnThisAtm.add(ap);
                    apsPerAtom.put(srcAtm,apsOnThisAtm);
                }
            }
        }
        apsPerVertexId.put(idInVrx, apsOnThisFrag);
        
        // Recursion on all the edges leaving from this fragment
        for (Edge nextEdge : graph.getEdgesWithSrc(inVtx))
        {
            // Add APs to the map of APs per Edges
            ArrayList<AttachmentPoint> apOnThisEdge =
                                      new ArrayList<AttachmentPoint>();
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
                append3DFragmentsViaEdges(mol, graph,
                        nextEdge.getSrcAP().getAtomPositionNumberInMol(), 
                        srcNextApA, trgNextApA, nextEdge, removeUsedRCAs,
                        rebuild,
                        apsPerVertexId,apsPerEdge,apsPerAtom,apsPerBond);
            } else {
                // Append next building block - start recursion
                Point3d pt = getRandomPoint(mol);
                append3DFragmentsViaEdges(mol, graph, -1, pt, pt, nextEdge, 
                        removeUsedRCAs, rebuild,
                        apsPerVertexId,apsPerEdge,apsPerAtom,apsPerBond);
            }
        }
    }
    
//------------------------------------------------------------------------------
    
    private Point3d getRandomPoint(IAtomContainer mol)
    {
        double vx = ((double) randomizer.nextInt(100)) / 100.0;
        double vy = ((double) randomizer.nextInt(100)) / 100.0;
        double vz = ((double) randomizer.nextInt(100)) / 100.0;
        Point3d c = new Point3d(0,0,0);
        if (mol.getAtomCount() > 0)
        {
            c = MoleculeUtils.calculateCentroid(mol);
        }
        return new Point3d(maxCoord*vx+c.x, maxCoord*vy+c.y, maxCoord*vz+c.z);
    }

//------------------------------------------------------------------------------

}
