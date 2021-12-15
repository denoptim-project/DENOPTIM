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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomRef;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.aromaticity.Kekulization;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.geometry.GeometryUtil;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.inchi.InChIGenerator;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IAtomType.Hybridization;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IElement;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.qsar.DescriptorValue;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.RotatableBondsCountDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.WeightDescriptor;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.qsar.result.IntegerResult;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.BasicAtomGenerator;
import org.openscience.cdk.renderer.generators.BasicBondGenerator;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.graph.APClass;
import denoptim.graph.DENOPTIMAttachmentPoint;
import denoptim.graph.DENOPTIMEdge.BondType;
import denoptim.graph.DENOPTIMFragment;
import denoptim.graph.DENOPTIMGraph;
import denoptim.graph.DENOPTIMRing;
import denoptim.graph.DENOPTIMVertex;
import denoptim.graph.DENOPTIMVertex.BBType;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import net.sf.jniinchi.INCHI_RET;


/**
 * Utilities for molecule conversion
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class DENOPTIMMoleculeUtils
{
    private static final StructureDiagramGenerator SDG = 
            new StructureDiagramGenerator();
    private static final SmilesGenerator SMGEN = new SmilesGenerator(
            SmiFlavor.Generic);
    private static final DummyAtomHandler DATMHDLR = new DummyAtomHandler(
					      DENOPTIMConstants.DUMMYATMSYMBOL);
    private static final  IChemObjectBuilder builder = 
            SilentChemObjectBuilder.getInstance();

//------------------------------------------------------------------------------

    public static double getMolecularWeight(IAtomContainer mol) 
            throws DENOPTIMException
    {
        double ret_wd = 0;
        try
        {
            WeightDescriptor wd = new WeightDescriptor();
            Object[] pars = {"*"};
            wd.setParameters(pars);
            ret_wd = ((DoubleResult) wd.calculate(mol).getValue())
                    .doubleValue();
        }
        catch (Exception ex)
        {
            throw new DENOPTIMException(ex);
        }
        return ret_wd;
    }
    
//------------------------------------------------------------------------------

    /**
     * Check element symbol corresponds to real element of Periodic Table
     * @param atom to check.
     * @return <code>true</code> if the element symbol correspond to an atom
     * in the periodic table.
     */

    public static boolean isElement(IAtom atom)
    {
        String symbol = atom.getSymbol();
        return isElement(symbol);
    }
    
//------------------------------------------------------------------------------

    /**
     * Check element symbol corresponds to real element of Periodic Table
     * @param symbol of the element to check
     * @return <code>true</code> if the element symbol correspond to an atom
     * in the periodic table.
     */

    public static boolean isElement(String symbol)
    {
        boolean res = false;
        IsotopeFactory ifact = null;
        try {
            ifact = Isotopes.getInstance();
            if (ifact.isElement(symbol))
            {
                @SuppressWarnings("unused")
                IElement el = ifact.getElement(symbol);
                res = true;
            }
        } catch (Throwable t) {
            System.out.println("ERROR! Unable to create IsotopeFactory "
                                        + " (in AtomUtils.getAtomicNumber)");
            System.exit(-1);;
        }
        return res;
    }

//------------------------------------------------------------------------------

    /**
     * Replace any <code>PseudoAtom</code>s representing ring closing attractors
     * with H. No change in coordinates, but any reference to the original
     * atom, beyond those managed internally to the CDK library, will be broken.
     * @param mol Molecule to replace <code>PseudoAtom</code>s of.
     */
    public static void removeRCA(IAtomContainer mol) {

        // convert PseudoAtoms to H
        for (IAtom a : mol.atoms())
        {
            boolean isRca = false;
            Set<String> rcaElSymbols = DENOPTIMConstants.RCATYPEMAP.keySet();
            for (String rcaEl : rcaElSymbols)
            {
                if (DENOPTIMMoleculeUtils.getSymbolOrLabel(a).equals(rcaEl))
                {
                    isRca = true;
                    break;
                }
            }
            if (isRca)
            {
                IAtom newAtm = new Atom("H",new Point3d(a.getPoint3d()));
                newAtm.setProperties(a.getProperties());
                AtomContainerManipulator.replaceAtomByAtom(mol,a,newAtm);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Replace used RCAs (i.e., those involved in {@link DENOPTIMRing}s)
     * while adding the ring closing bonds. Does not alter the graph.
     * @param mol the molecular representation to be updated
     * @param graph the corresponding graph representation 
     * @throws DENOPTIMException if
     */

    public static void removeUsedRCA(IAtomContainer mol, DENOPTIMGraph graph)
            throws DENOPTIMException {

        // add ring-closing bonds
        ArrayList<DENOPTIMVertex> usedRcvs = graph.getUsedRCVertices();
        Map<DENOPTIMVertex,ArrayList<Integer>> vIdToAtmId =
                DENOPTIMMoleculeUtils.getVertexToAtomIdMap(usedRcvs,mol);
        if (vIdToAtmId.size() == 0)
        {
            // No used RCV to remove.
            return;
        }
        ArrayList<IAtom> atmsToRemove = new ArrayList<>();
        ArrayList<Boolean> doneVertices =
                new ArrayList<>(Collections.nCopies(usedRcvs.size(),false));

        for (DENOPTIMVertex v : usedRcvs)
        {
            if (doneVertices.get(usedRcvs.indexOf(v)))
            {
                continue;
            }
            ArrayList<DENOPTIMRing> rings = graph.getRingsInvolvingVertex(v);
            if (rings.size() != 1)
            {
                String s = "Unexpected inconsistency between used RCV list "
                       + v + " in {" + usedRcvs + "}"
                       + "and list of DENOPTIMRings "
                       + "{" + rings + "}. Check Code!";
                throw new DENOPTIMException(s);
            }
            DENOPTIMVertex vH = rings.get(0).getHeadVertex();
            DENOPTIMVertex vT = rings.get(0).getTailVertex();
            IAtom aH = mol.getAtom(vIdToAtmId.get(vH).get(0));
            IAtom aT = mol.getAtom(vIdToAtmId.get(vT).get(0));
            if (mol.getConnectedAtomsList(aH).size() == 0
                    || mol.getConnectedAtomsList(aT).size() == 0)
            {
                // This can happen when building a graph with empty vertexes
                continue;
            }
            int iSrcH = mol.indexOf(mol.getConnectedAtomsList(aH).get(0));
            int iSrcT = mol.indexOf(mol.getConnectedAtomsList(aT).get(0));
            atmsToRemove.add(aH);
            atmsToRemove.add(aT);

            BondType bndTyp = rings.get(0).getBondType();
            if (bndTyp.hasCDKAnalogue())
            {
                mol.addBond(iSrcH, iSrcT, bndTyp.getCDKOrder());
            } else {
                System.out.println("WARNING! Attempt to add ring closing bond "
                        + "did not add any actual chemical bond because the "
                        + "bond type of the chord is '" + bndTyp +"'.");
            }

            doneVertices.set(usedRcvs.indexOf(vH),true);
            doneVertices.set(usedRcvs.indexOf(vT),true);
	    }
        
        // Adapt atom indexes in APs to the upcoming change of atom list
        ArrayList<Integer> removedIds = new ArrayList<Integer>();
        for (IAtom a : atmsToRemove)
        {
            removedIds.add(mol.indexOf(a));
        }
        Collections.sort(removedIds);
        for (DENOPTIMVertex v : graph.getVertexList())
        {
            for (DENOPTIMAttachmentPoint ap : v.getAttachmentPoints())
            {
                int apSrcId = ap.getAtomPositionNumberInMol();
                int countOfAtmsBEforeSrc = 0;
                for (Integer removingId : removedIds)
                {
                    if (removingId < apSrcId)
                    {
                        countOfAtmsBEforeSrc++; 
                    } else if (removingId > apSrcId)
                    {
                        break;
                    }
                }
                ap.setAtomPositionNumberInMol(apSrcId-countOfAtmsBEforeSrc);
            }
        }

        // remove used RCAs
        for (IAtom a : atmsToRemove)
        {
            mol.removeAtom(a);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Returns the SMILES representation of the molecule. All atoms are expected 
     * to be explicit, as we set the count of implicit H to zero for all atoms.
     * @param mol the molecule
     * @return smiles string
     * @throws DENOPTIMException
     */

    public static String getSMILESForMolecule(IAtomContainer mol)
            throws DENOPTIMException {
        IAtomContainer fmol = builder.newAtomContainer();
        try 
        {
            fmol = mol.clone();
        }  
        catch (CloneNotSupportedException e)
        {
            throw new DENOPTIMException(e);
        }

        // remove Dummy atoms
        fmol = DATMHDLR.removeDummyInHapto(fmol);
        fmol = DATMHDLR.removeDummy(fmol);

        // convert PseudoAtoms to H
        removeRCA(fmol);
        
        // WARNING: assumptions on implicit H count and bond orders!
        DENOPTIMMoleculeUtils.setZeroImplicitHydrogensToAllAtoms(fmol);
        DENOPTIMMoleculeUtils.ensureNoUnsetBondOrders(fmol);

        String smiles = "";
        try
        {
            smiles = SMGEN.create(fmol);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        	String fileName = "failed_generation_of_SMILES.sdf";
        	System.out.println("WARNING: Skipping calculation of SMILES. See "
        			+ "file '" + fileName + "'");
        	DenoptimIO.writeMolecule(fileName,fmol,false);
        	smiles = "calculation_or_SMILES_crashed";
        }
        return smiles;
    }

//------------------------------------------------------------------------------

    /**
      * Generates 2D coordinates for the molecule
      *
      * @param ac the molecule to layout.
      * @return A new molecule laid out in 2D.  If the molecule already has 2D
      *         coordinates then it is returned unchanged.  If layout fails then
      *         null is returned.
     * @throws denoptim.exception.DENOPTIMException
      */
    public static IAtomContainer generate2DCoordinates(IAtomContainer ac)
                                                    throws DENOPTIMException
    {
        IAtomContainer fmol = builder.newAtomContainer();
        try 
        { 
            fmol = ac.clone();
        }  
        catch (CloneNotSupportedException e)
        {
            throw new DENOPTIMException(e);
        }

        // remove Dummy atoms
        fmol = DATMHDLR.removeDummyInHapto(fmol);

        // remove ring-closing attractors
        removeRCA(fmol);

        // Generate 2D structure diagram (for each connected component).
        IAtomContainer ac2d = builder.newAtomContainer();
        IAtomContainerSet som = ConnectivityChecker.partitionIntoMolecules(
                fmol);

        for (int n = 0; n < som.getAtomContainerCount(); n++)
        {
            synchronized (SDG)
            {
                IAtomContainer mol = som.getAtomContainer(n);
                SDG.setMolecule(mol, true);
                try
                {
                    // Generate 2D coordinates for this molecule.
                    SDG.generateCoordinates();
                    mol = SDG.getMolecule();
                }
                catch (Exception e)
                {
                    throw new DENOPTIMException(e);
                }

                ac2d.add(mol);  // add 2D molecule.
            }
        }
        
        return GeometryUtil.has2DCoordinates(ac2d) ? ac2d : null;
    }

//------------------------------------------------------------------------------

    /**
     * Generates the INCHI key for the molecule
     * @param mol the molecule
     * @return the InchiKey. <code>null</code> if error
     * @throws denoptim.exception.DENOPTIMException
     */

    public static ObjectPair getInchiForMolecule(IAtomContainer mol)
            throws DENOPTIMException {
        IAtomContainer fmol = builder.newAtomContainer();
        try 
        { 
            fmol = mol.clone();
        }  
        catch (Throwable t) 
        {
            throw new DENOPTIMException(t);
        }

        // remove Dummy atoms before generating the inchi
        fmol = DATMHDLR.removeDummyInHapto(fmol);

        // remove PseudoAtoms
        removeRCA(fmol);

        String inchi;
        try
        {
            InChIGeneratorFactory factory = InChIGeneratorFactory.getInstance();
            // Get InChIGenerator, this is a non-standard inchi
            InChIGenerator gen = factory.getInChIGenerator(
                    fmol,
                    "AuxNone RecMet SUU"
            );
            INCHI_RET ret = gen.getReturnStatus();
            if (ret == INCHI_RET.WARNING)
            {
                //String error = gen.getMessage();
                //InChI generated, but with warning message
                //return new ObjectPair(null, error);
            }
            else if (ret != INCHI_RET.OKAY)
            {
                // InChI generation failed
                String error = "Failed to create INCHI" + ret.toString()
                + " [" + gen.getMessage() + "]";
                return new ObjectPair(null, error);
            }
            inchi = gen.getInchiKey();
        }
        catch (CDKException cdke)
        {
            throw new DENOPTIMException(cdke);
        }
        if (inchi.length() > 0)
            return new ObjectPair(inchi, null);
        else
            return new ObjectPair(null, "No InChi key generated");
    }


//------------------------------------------------------------------------------

    /**
     * Count number of rotatable bonds
     * @param mol molecule to count rotatable bonds in
     * @return number of rotatable bonds
     * @throws DENOPTIMException
     */

    public static int getNumberOfRotatableBonds(IAtomContainer mol)
            throws DENOPTIMException {
        int value;
        try
        {
            IMolecularDescriptor descriptor = 
                    new RotatableBondsCountDescriptor();
            descriptor.setParameters(new Object[]{Boolean.FALSE,Boolean.FALSE});
            DescriptorValue result = descriptor.calculate(mol);
            value = ((IntegerResult)result.getValue()).intValue();
        }
        catch (CDKException cdke)
        {
            throw new DENOPTIMException(cdke);
        }
        return value;
    }

//------------------------------------------------------------------------------

    /**
     * The heavy atom count
     * @param mol Molecule to count heavy atoms in
     * @return heavy atom count
     */

    public static int getHeavyAtomCount(IAtomContainer mol)
    {
        int n = 0;
        for (IAtom atm : mol.atoms())
        {
            if (DENOPTIMMoleculeUtils.isElement(atm)
                    && !DENOPTIMMoleculeUtils.getSymbolOrLabel(atm).equals("H"))
                n++;
        }
        return n;
    }
    
//------------------------------------------------------------------------------

    /**
     * Count atoms with the given elemental symbol.
     * @param mol Molecule to count atoms in.
     * @param symbol the elemental symbol to look for.
     * @return the total number of those elements.
     */

    public static int countAtomsOfElement(IAtomContainer mol, String symbol)
    {
        int n = 0;
        for (IAtom atm : mol.atoms())
        {
            if (atm.getSymbol().equals(symbol))
                n++;
        }
        return n;
    }

//------------------------------------------------------------------------------

    /**
     * Method to generate the map making in relation <code>DENOPTIMVertex</code>
     * ID and atom index in the <code>IAtomContainer</code> representation of 
     * the chemical entity. Note that the <code>IAtomContainer</code> must 
     * have been generated from the <code>DENOPTIMGraph</code> that contains the
     * required <code>DENOPTIMVertex</code>s.
     * @param vertLst the list of <code>DENOPTIMVertex</code> to find
     * @param mol the molecular representation
     * @return the map of atom indexes per each <code>DENOPTIMVertex</code> ID
     */

    public static Map<DENOPTIMVertex,ArrayList<Integer>> getVertexToAtomIdMap(
            ArrayList<DENOPTIMVertex> vertLst,
            IAtomContainer mol
    ) {

        ArrayList<Integer> vertIDs = new ArrayList<>();
        for (DENOPTIMVertex v : vertLst) {
            vertIDs.add(v.getVertexId());
        }

        Map<DENOPTIMVertex,ArrayList<Integer>> map = new HashMap<>();
        for (IAtom atm : mol.atoms())
        {
            int vID = Integer.parseInt(atm.getProperty(
                                 DENOPTIMConstants.ATMPROPVERTEXID).toString());
            if (vertIDs.contains(vID))
            {
                DENOPTIMVertex v = vertLst.get(vertIDs.indexOf(vID));
                int atmID = mol.indexOf(atm);
                if (map.containsKey(v))
                {
                    map.get(v).add(atmID);
                }
                else
                {
                    ArrayList<Integer> atmLst = new ArrayList<>();
                    atmLst.add(atmID);
                    map.put(v,atmLst);
                }
            }
        }
        return map;
    }

//------------------------------------------------------------------------------
    
    /**
     * Generate a PNG image from molecule <code>mol</code>
     * @param mol generate molecule PNG from
     * @param filename name of file to store generated PNG as
     * @throws DENOPTIMException
     */
    
    public static void moleculeToPNG(IAtomContainer mol, String filename)
            throws DENOPTIMException
    {
        IAtomContainer iac = mol;

        if (!GeometryUtil.has2DCoordinates(mol))
        {
            iac = generate2DCoordinates(mol);
        }
        
        if (iac == null)
        {
            throw new DENOPTIMException("Failed to generate 2D coordinates.");
        }
    
        try
        {
            int WIDTH = 400;
            int HEIGHT = 400;
            
            GeometryUtil.scaleMolecule(iac, 0.9);
            GeometryUtil.translateAllPositive(iac);
            // the draw area and the image should be the same size
            Rectangle drawArea = new Rectangle(WIDTH, HEIGHT);
            Image image = new BufferedImage(WIDTH, HEIGHT, 
                    BufferedImage.TYPE_4BYTE_ABGR);

            // generators make the image elements
            ArrayList<IGenerator<IAtomContainer>> generators = 
                    new ArrayList<>(); 
            generators.add(new BasicSceneGenerator());
            generators.add(new BasicBondGenerator());
            generators.add(new BasicAtomGenerator());
            

            // the renderer needs to have a toolkit-specific font manager
            AtomContainerRenderer renderer =
                    new AtomContainerRenderer(generators, new AWTFontManager());
                    
            RendererModel model = renderer.getRenderer2DModel();
            model.set(BasicSceneGenerator.UseAntiAliasing.class, true);
            model.set(BasicBondGenerator.BondWidth.class, 2.0);            
            model.set(BasicAtomGenerator.ShowExplicitHydrogens.class, false);
            model.set(BasicAtomGenerator.AtomRadius.class, 0.5);
            model.set(BasicAtomGenerator.CompactShape.class, 
                    BasicAtomGenerator.Shape.OVAL);

            // the call to 'setup' only needs to be done on the first paint
            renderer.setup(iac, drawArea);

            // paint the background
            Graphics2D g = (Graphics2D)image.getGraphics();
            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, WIDTH, HEIGHT);
            

            // the paint method also needs a toolkit-specific renderer
            renderer.paint(iac, new AWTDrawVisitor(g), 
                    new Rectangle2D.Double(0, 0, WIDTH, HEIGHT), true);
            g.dispose();

            ImageIO.write((RenderedImage)image, "PNG", new File(filename));
        }
        catch (IOException e)
        {
            throw new DENOPTIMException(e);
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Return the 3D coordinates, if present.
     * If only 2D coords exist, then it returns the 2D projected in 3D space.
     * If neither 3D nor 2D are present returns [0, 0, 0].
     * @param atm the atom to analyze.
     * @return a not null.
     */
    public static Point3d getPoint3d(IAtom atm)
    {
        Point3d p = atm.getPoint3d();

        if (p == null)
        {
            Point2d p2d = atm.getPoint2d();
            if (p2d == null)
            {
                p = new Point3d(0.0, 0.0, 0.0);
            }
            else
            {
                p = new Point3d(p2d.x, p2d.y, 0.0);
            }
        }
        return p;
    }
    
//------------------------------------------------------------------------------

    /**
     * Sets zero implicit hydrogen count to all atoms.
     * @param iac the container to process
     */
    public static void setZeroImplicitHydrogensToAllAtoms(IAtomContainer iac)
    {
        for (IAtom atm : iac.atoms()) {
            atm.setImplicitHydrogenCount(0);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Sets bond order = single to all otherwise unset bonds.
     * @param iac the container to process
     */
    
    public static void ensureNoUnsetBondOrders(IAtomContainer iac)
    {
        try {
            Kekulization.kekulize(iac);
        } catch (CDKException e) {
            DENOPTIMLogger.appLogger.log(Level.WARNING, "Kekulization failed. "
                    + "Bond orders will be unreliable.");
        }
        
        for (IBond bnd : iac.bonds())
        {
            if (bnd.getOrder().equals(IBond.Order.UNSET)) 
            {
                bnd.setOrder(IBond.Order.SINGLE);
            }
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Constructs a copy of an atom container, i.e., a molecule that reflects 
     * the one given in the input argument
     * in terms of atom count, type, and geometric properties, and bond count 
     * and type. Other properties are not copyied.
     * @param mol the container to copy.
     * @return the chemical-copy of the input argument.
     * @throws DENOPTIMException if there are bonds involving more than two atoms.
     */
    public static IAtomContainer makeSameAs(IAtomContainer mol) throws DENOPTIMException
    {
        IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
        IAtomContainer iac = builder.newAtomContainer();
        
        for (IAtom oAtm : mol.atoms())
        {
            IAtom nAtm = DENOPTIMMoleculeUtils.makeSameAtomAs(oAtm,true,true);
            iac.addAtom(nAtm);
        }
        
        for (IBond oBnd : mol.bonds())
        {
            if (oBnd.getAtomCount() != 2)
            {
                throw new DENOPTIMException("Unable to deal with bonds "
                        + "involving more than two atoms.");
            }
            int ia = mol.indexOf(oBnd.getAtom(0));
            int ib = mol.indexOf(oBnd.getAtom(1));
            iac.addBond(ia,ib,oBnd.getOrder());
        }
        return iac;
    }
    
//------------------------------------------------------------------------------

    /**
     * Method that constructs an atom that reflect the same atom given as 
     * parameter in terms of element symbol (or label, for pseudoatoms),
     * and Cartesian coordinates, and most of the other attributes.
     * This method copies valence and implicit H count, in addition to most of
     * the fields accessible via IAtom. Use 
     * {@link #makeSameAtomAs(IAtom, boolean, boolean)} to have the option to
     * exclude valence and implicit H count.
     * This is basically a cloning method that ignores some fields.
     * @param oAtm the original
     * @return the copy of the original
     */
    public static IAtom makeSameAtomAs(IAtom oAtm)
    {
        return makeSameAtomAs(oAtm, false, false);
    }
    
//------------------------------------------------------------------------------

    /**
     * Method that constructs an atom that reflect the same atom given as 
     * parameter in terms of element symbol (or label, for pseudoatoms),
     * and Cartesian coordinates, and most of the other attributes unless 
     * otherwise specified by the flags.
     * This is basically a cloning method that ignores some fields.
     * @param oAtm the original
     * @param ignoreValence if <code>true</code> the returned atom will have 
     * null valence
     * @param ignoreImplicitH if <code>true</code> the returned atom will have 
     * null implicit hydrogen count.
     * @return the copy of the original
     */
    public static IAtom makeSameAtomAs(IAtom oAtm, boolean ignoreValence,
            boolean ignoreImplicitH)
    {
        IAtom nAtm = null;
        String s = getSymbolOrLabel(oAtm);
        if (DENOPTIMMoleculeUtils.isElement(oAtm))
        {
            nAtm = new Atom(s);
        } else {
            nAtm = new PseudoAtom(s);
        }
        if (oAtm.getPoint3d() != null)
        {
            Point3d p3d = oAtm.getPoint3d();
            nAtm.setPoint3d(new Point3d(p3d.x, p3d.y, p3d.z));
        } else if (oAtm.getPoint2d() != null)
        {
            Point2d p2d = oAtm.getPoint2d();
            nAtm.setPoint3d(new Point3d(p2d.x, p2d.y, 0.00001));
        }
        if (oAtm.getFormalCharge() != null)
            nAtm.setFormalCharge(oAtm.getFormalCharge());
        if (oAtm.getBondOrderSum() != null)
            nAtm.setBondOrderSum(oAtm.getBondOrderSum());
        if (oAtm.getCharge() != null)
            nAtm.setCharge(oAtm.getCharge());
        if (oAtm.getValency() != null && !ignoreValence)
            nAtm.setValency(oAtm.getValency());
        if (oAtm.getExactMass() != null)
            nAtm.setExactMass(oAtm.getExactMass());
        if (oAtm.getMassNumber() != null)
            nAtm.setMassNumber(oAtm.getMassNumber());
        if (oAtm.getFormalNeighbourCount() != null)
            nAtm.setFormalNeighbourCount(oAtm.getFormalNeighbourCount());
        if (oAtm.getFractionalPoint3d() != null)
            nAtm.setFractionalPoint3d(new Point3d(oAtm.getFractionalPoint3d()));
        if (oAtm.getHybridization() != null)
            nAtm.setHybridization(Hybridization.valueOf(
                    oAtm.getHybridization().toString()));
        if (oAtm.getImplicitHydrogenCount() != null && !ignoreImplicitH)
            nAtm.setImplicitHydrogenCount(oAtm.getImplicitHydrogenCount());
        if (oAtm.getStereoParity() != null)
            nAtm.setStereoParity(oAtm.getStereoParity());
        if (oAtm.getMaxBondOrder() != null)
            nAtm.setMaxBondOrder(oAtm.getMaxBondOrder());
        if (oAtm.getNaturalAbundance() != null)
            nAtm.setNaturalAbundance(oAtm.getNaturalAbundance());
        
        return nAtm;
    }

//------------------------------------------------------------------------------
    
    /**
     * Gets either the elemental symbol (for standard atoms) of the label (for
     * pseudoatoms). Other classes implementing IAtom are not considered.
     * This method is a response to the change from CDK-1.* to CDK-2.*. 
     * In the old CDK-1.* the getSymbol() method of IAtom would return the label 
     * for a PseudoAtom, but this behaviour is not retained in newer versions.
     * @param atm
     * @return either the element symbol of the label
     */
    public static String getSymbolOrLabel(IAtom atm)
    {
        String s = "none";
        if (DENOPTIMMoleculeUtils.isElement(atm))
        {
            s = atm.getSymbol();
        } else {
            //TODO: one day will account for the possibility of having any 
            // other implementation of IAtom
            IAtom a = null;
            if (atm instanceof AtomRef) 
            {
                a = ((AtomRef) atm).deref();
                if (a instanceof PseudoAtom)
                {
                    s = ((PseudoAtom) a).getLabel();
                } else {
                    // WARNING: we fall back to standard behaviour, but there
                    // could still be cases where this is not good...
                    s = a.getSymbol();
                }
            } else if (atm instanceof PseudoAtom)
            {
                s = ((PseudoAtom) atm).getLabel();
            }
        }       
        return s;
    }
    
//------------------------------------------------------------------------------

    /**
     * Calculated the centroid of the given molecule.
     * @param mol
     * @return the centroid.
     */
    public static Point3d calculateCentroid(IAtomContainer mol)
    {
        Point3d c = new Point3d(0,0,0);
        for (IAtom atm : mol.atoms())
        {
            c.add(getPoint3d(atm));
        }
        c.scale(1.0 / ((double) mol.getAtomCount()));
        return c;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Selects only the atoms that originate from a subgraph of a whole graph 
     * that originated the whole molecule given as parameter.
     * @param wholeIAC the molecular representation of the whole graph. The 
     * atoms contained here are expected to be have the property
     * {@link DENOPTIMConstants#ATMPROPVERTEXID}, 
     * which is used to identify which 
     * atoms originated from which vertex of the whole graph.
     * @param subGraph the portion of the whole graph for which we want the 
     * corresponding atoms. The vertexes are expected to have the 
     * {@link DENOPTIMConstants#STOREDVID} property that defines their original 
     * vertex ID in the whole graph. These labels are expected to be consistent
     *  with those in the property
     * {@link DENOPTIMConstants#ATMPROPVERTEXID} of the <code>wholeIAC</code>
     * parameter.
     * Note that the current vertexID of each 
     * vertex can be different from the ID of the original vertex had in the
     * while graph.
     * @return a new container with the requested substructure
     * @throws DENOPTIMException 
     */
    public static IAtomContainer extractIACForSubgraph(IAtomContainer wholeIAC, 
            DENOPTIMGraph subGraph, DENOPTIMGraph wholeGraph) throws DENOPTIMException
    {
        IAtomContainer iac = makeSameAs(wholeIAC);
        
        Set<Integer> wantedVIDs = new HashSet<Integer>();
        Map<Integer,DENOPTIMVertex> wantedVertexesMap = new HashMap<>();
        for (DENOPTIMVertex v : subGraph.getVertexList())
        {
            Object o = v.getProperty(DENOPTIMConstants.STOREDVID);
            if (o == null)
            {
                throw new DENOPTIMException("Property '" 
                        + DENOPTIMConstants.STOREDVID + "' not defined in "
                        + "vertex " + v + ", but is needed to extract "
                                + "substructure.");
            }
            wantedVIDs.add(((Integer) o).intValue());
            wantedVertexesMap.put(((Integer) o).intValue(), v);
        }
        
        // Identify the destiny of each atom: keep, remove, or make AP from it.
        IAtomContainer toRemove = new AtomContainer();
        Map<IAtom,IAtom> toAP = new HashMap<IAtom,IAtom>();
        Map<IAtom,DENOPTIMAttachmentPoint> mapAtmToAPInG = 
                new HashMap<IAtom,DENOPTIMAttachmentPoint>();
        Map<IAtom,APClass> apcMap = new HashMap<IAtom,APClass>();
        for (int i=0; i<wholeIAC.getAtomCount(); i++)
        {
            IAtom cpAtm = iac.getAtom(i);
            IAtom oriAtm = wholeIAC.getAtom(i);
            Object o = oriAtm.getProperty(DENOPTIMConstants.ATMPROPVERTEXID);
            if (o == null)
            {
                throw new DENOPTIMException("Property '" 
                        + DENOPTIMConstants.ATMPROPVERTEXID 
                        + "' not defined in atom "
                        + oriAtm.getSymbol() + wholeIAC.indexOf(oriAtm) 
                        + ", but is needed to extract substructure.");
            }
            int vid = ((Integer) o).intValue();
            if (wantedVIDs.contains(vid))
            {
                continue; //keep this atom cpAtm
            }
            // Now, decide if the current atom should become an AP
            boolean willBecomeAP = false;
            for (IAtom nbr : wholeIAC.getConnectedAtomsList(oriAtm))
            {
                Object oNbr = nbr.getProperty(DENOPTIMConstants.ATMPROPVERTEXID);
                if (oNbr == null)
                {
                    throw new DENOPTIMException("Property '" 
                            + DENOPTIMConstants.ATMPROPVERTEXID 
                            + "' not defined in atom "
                            + nbr.getSymbol() + wholeIAC.indexOf(nbr) 
                            + ", but is needed to extract substructure.");
                }
                int nbrVid = ((Integer) oNbr).intValue();
                if (wantedVIDs.contains(nbrVid))
                {
                    // cpAtm is connected to an atom to keep and will therefore
                    // become an AP. Note that the connection may go through
                    // a chord in the graph, i.e., a pairs of RCVs!
                    toAP.put(cpAtm,iac.getAtom(wholeIAC.indexOf(nbr)));
                    DENOPTIMAttachmentPoint apInWholeGraph = 
                            wholeGraph.getAPOnLeftVertexID(nbrVid,vid);
                    if (apInWholeGraph == null)
                    {
                        String debugFile = "failedAPIdentificationIACSubGraph" 
                                + wholeGraph.getGraphId() + ".sdf";
                        DenoptimIO.writeGraphToSDF(new File(debugFile), wholeGraph, willBecomeAP);
                        throw new DENOPTIMException("Unmexpected null AP from "
                                + nbrVid + " " + vid +" on " + wholeGraph 
                                + " See " + debugFile);
                    }
                    DENOPTIMAttachmentPoint apInSubGraph = 
                            wantedVertexesMap.get(nbrVid).getAP(apInWholeGraph.getIndexInOwner());
                    mapAtmToAPInG.put(cpAtm, apInSubGraph);
                    APClass apc = apInWholeGraph.getAPClass();
                    apcMap.put(cpAtm, apc);
                    willBecomeAP = true;
                    break;
                }
            }
            if (!willBecomeAP)
                toRemove.addAtom(cpAtm);
        }
        
        iac.remove(toRemove);
        
        // NB: the molecular representation in frag is NOT iac! It's a clone of it
        DENOPTIMFragment frag = new DENOPTIMFragment(iac,BBType.FRAGMENT);
        
        List<IAtom> atmosToRemove = new ArrayList<>();
        List<IBond> bondsToRemove = new ArrayList<>();
        for (IAtom trgAtmInIAC : toAP.keySet())
        {
            IAtom trgAtm = frag.getAtom(iac.indexOf(trgAtmInIAC));
            IAtom srcAtmInISC = toAP.get(trgAtmInIAC);
            IAtom srcAtm = frag.getAtom(iac.indexOf(srcAtmInISC));
            
            DENOPTIMAttachmentPoint apInG = mapAtmToAPInG.get(trgAtmInIAC);
            
            // Make Attachment point
            Point3d srcP3d = DENOPTIMMoleculeUtils.getPoint3d(srcAtm);
            Point3d trgP3d = DENOPTIMMoleculeUtils.getPoint3d(trgAtm);
            double currentLength = srcP3d.distance(trgP3d);
            //TODO-V3+? change hard-coded value with property of AP, when such
            // property will be available, i. e., one refactoring of AP and 
            // atom coordinates is done.
            double idealLength = 1.53;
            /*
            double idealLength = apInG.getProperty(
                    DENOPTIMConstants.APORIGINALLENGTH);
                    */
            Point3d vector = new Point3d();
            vector.x = srcP3d.x + (trgP3d.x - srcP3d.x)*(idealLength/currentLength);
            vector.y = srcP3d.y + (trgP3d.y - srcP3d.y)*(idealLength/currentLength);
            vector.z = srcP3d.z + (trgP3d.z - srcP3d.z)*(idealLength/currentLength);
            
            DENOPTIMAttachmentPoint createdAP = frag.addAPOnAtom(srcAtm, 
                    apcMap.get(trgAtmInIAC), vector);
            createdAP.setProperty(DENOPTIMConstants.LINKAPS, apInG);
            
            for (IBond bnd : frag.bonds())
            {
                if (bnd.contains(trgAtm))
                {
                    bondsToRemove.add(bnd);
                }
            }
            atmosToRemove.add(trgAtm);
        }
        
        // Remove atom that has become an AP and associated bonds
        for (IBond bnd : bondsToRemove)
        {
            frag.removeBond(bnd);
        }
        for (IAtom a : atmosToRemove)
        {
            frag.removeAtomAndConnectedElectronContainers(a);
        }
        
        frag.updateAPs();
        
        return frag.getIAtomContainer();
    }

//------------------------------------------------------------------------------

}
