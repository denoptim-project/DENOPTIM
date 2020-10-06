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
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Collections;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.RenderingHints;

import javax.imageio.ImageIO;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;

import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.inchi.InChIGenerator;

import net.sf.jniinchi.INCHI_RET;

import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.ringsearch.AllRingsFinder;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.Atom;
import org.openscience.cdk.interfaces.IMoleculeSet;
import org.openscience.cdk.interfaces.IRingSet;
import org.openscience.cdk.ringsearch.SSSRFinder;
import org.openscience.cdk.qsar.DescriptorValue;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.result.IntegerResult;
import org.openscience.cdk.qsar.descriptors.molecular.RotatableBondsCountDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.WeightDescriptor;
import org.openscience.cdk.renderer.generators.BasicAtomGenerator;
import org.openscience.cdk.renderer.generators.BasicBondGenerator;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.interfaces.*;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMRing;
import denoptim.molecule.DENOPTIMVertex;

//TODO del if writing of failing molecule is not made systematic

/**
 * Utilities for molecule conversion
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class DENOPTIMMoleculeUtils
{
    private static final StructureDiagramGenerator SDG = new StructureDiagramGenerator();
    private static final SmilesParser SMPARSER =
                    new SmilesParser(DefaultChemObjectBuilder.getInstance());
    private static final SmilesGenerator SMGEN = new SmilesGenerator(true);
    private static final DummyAtomHandler DATMHDLR = new DummyAtomHandler(
					      DENOPTIMConstants.DUMMYATMSYMBOL);

//------------------------------------------------------------------------------

    public static double getMolecularWeight(IAtomContainer mol) throws DENOPTIMException
    {
        double ret_wd = 0;
        try
        {
            WeightDescriptor wd = new WeightDescriptor();
            Object[] pars = {"*"};
            wd.setParameters(pars);
            ret_wd = ((DoubleResult) wd.calculate(mol).getValue()).doubleValue();
        }
        catch (Exception ex)
        {
            throw new DENOPTIMException(ex);
        }
        return ret_wd;
    }

//------------------------------------------------------------------------------

    /**
     * Replace <code>PseudoAtom</code>s representing ring closing attractors
     * with H. No change in coordinates.
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
                if (a.getSymbol().equals(rcaEl))
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
     * Replace unused ring closing attractors (RCA) with H atoms and 
     * remove used RCAs (i.e., those involved in <code>DENOPTIMRing</code>s)
     * while adding the ring closing bonds
     * @param mol the molecular representation to be updated
     * @param graph the corresponding graph representation 
     * @throws DENOPTIMException if
     */
    public static void removeRCA(IAtomContainer mol, DENOPTIMGraph graph)
            throws DENOPTIMException {

        // add ring-closing bonds
        ArrayList<DENOPTIMVertex> usedRcvs = graph.getUsedRCVertices();
        Map<DENOPTIMVertex,ArrayList<Integer>> vIdToAtmId =
                DENOPTIMMoleculeUtils.getVertexToAtomIdMap(usedRcvs,mol);
        ArrayList<IAtom> atmsToRemove = new ArrayList<>();
        ArrayList<Boolean> doneVertices =
                new ArrayList<>(Collections.nCopies(usedRcvs.size(),false));
        for (DENOPTIMVertex v : usedRcvs)
        {
            if (doneVertices.get(usedRcvs.indexOf(v))) {
                continue;
            }
	        ArrayList<DENOPTIMRing> rings = graph.getRingsInvolvingVertex(v);
	        if (rings.size() != 1) {
                String s = "Unexpected inconsistency between used RCV list "
                        + v + " in {" + usedRcvs + "}"
                        + "and list of DENOPTIMRings "
                        + "{" + rings + "}. Check Code!";
                throw new DENOPTIMException(s);
            }
	        DENOPTIMVertex vHead = rings.get(0).getHeadVertex();
            DENOPTIMVertex vTail = rings.get(0).getTailVertex();

	        IAtom atomHead = mol.getAtom(vIdToAtmId.get(vHead).get(0));
	        IAtom atomTail = mol.getAtom(vIdToAtmId.get(vTail).get(0));

	        int iSrcH = mol.getAtomNumber(mol.getConnectedAtomsList(atomHead).get(0));
            int iSrcT = mol.getAtomNumber(mol.getConnectedAtomsList(atomTail).get(0));
            atmsToRemove.add(atomHead);
            atmsToRemove.add(atomTail);

            switch (rings.get(0).getBondType()) {
                case (2):
                    mol.addBond(iSrcH, iSrcT, IBond.Order.DOUBLE);
                    break;
                case (3):
                    mol.addBond(iSrcH, iSrcT, IBond.Order.TRIPLE);
                    break;
                default:
                    mol.addBond(iSrcH, iSrcT, IBond.Order.SINGLE);
                    break;
            }
            doneVertices.set(usedRcvs.indexOf(vHead),true);
            doneVertices.set(usedRcvs.indexOf(vTail),true);
	    }

        // remove used RCAs
        for (IAtom a : atmsToRemove)
        {
            mol.removeAtomAndConnectedElectronContainers(a);
        }

        // convert remaining PseudoAtoms to H
        removeRCA(mol);
    }

//------------------------------------------------------------------------------

    /**
     * returns the SMILES representation of the molecule
     * @param mol the molecule
     * @return smiles string
     * @throws DENOPTIMException
     */

    public static String getSMILESForMolecule(IAtomContainer mol)
            throws DENOPTIMException {
        IAtomContainer fmol = new AtomContainer();
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

        // convert PseudoAtoms to H
        removeRCA(fmol);

        String smiles = "";
        try
        {
            AllRingsFinder arf = new AllRingsFinder();
            arf.findAllRings(fmol);
            SMGEN.setRingFinder(arf);
            smiles = SMGEN.createSMILES(fmol);
        }
        catch (CDKException cdke)
        {
            if (cdke.getMessage().contains("Timeout for AllringsFinder exceeded"))
            {
                return null;
            }
            else
            {
               throw new DENOPTIMException(cdke);
            }
        }
        catch (IllegalArgumentException iae)
        {
            //TODO del or make systematic
	        DenoptimIO.writeMolecule("molecule_causing_failure.sdf",fmol,false);
            throw new DENOPTIMException(iae);
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

        IAtomContainer fmol = new AtomContainer();
        try 
        { 
            fmol = ac.clone();
        }  
        catch (CloneNotSupportedException e)
        {
            throw new DENOPTIMException(e);
        }

        // remove Dummy atoms before generating the inchi
        fmol = DATMHDLR.removeDummyInHapto(fmol);

        // remove PseudoAtoms
        removeRCA(fmol);

        // Generate 2D structure diagram (for each connected component).
        IAtomContainer ac2d = new AtomContainer();
        IMoleculeSet som = ConnectivityChecker.partitionIntoMolecules(fmol);

        SDG.setUseTemplates(true);

        for (int n = 0; n < som.getMoleculeCount(); n++)
        {
            synchronized (SDG)
            {
                IMolecule mol = som.getMolecule(n);
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

        return GeometryTools.has2DCoordinates(ac2d) ? ac2d : null;
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
        IAtomContainer fmol = new AtomContainer();
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
                String error = gen.getMessage();
                // InChI generated, but with warning message
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
            IMolecularDescriptor descriptor = new RotatableBondsCountDescriptor();
            descriptor.setParameters(new Object[]{Boolean.FALSE});
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
     * Calculates the rings in the molecule
     * @param mol The molecule
     * @return the set of rings
     */
    public static IRingSet getRingsInMolecule(IAtomContainer mol)
    {
        SSSRFinder sssrFinder = new SSSRFinder(mol);
        return sssrFinder.findSSSR();
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
        for (int f = 0; f < mol.getAtomCount(); f++)
        {
            if (!mol.getAtom(f).getSymbol().equals("H"))
                n++;
        }
        return n;
    }

//------------------------------------------------------------------------------

    /**
     * Method to generate the map making in relation <code>DENOPTIMVertex</code>
     * ID and atom index in the <code>IAtomContainer</code> representation of 
     * the chemical entity. Note that the S<code>IAtomContainer</code> must 
     * have been generated from the <code>DENOPTIMGraph</code> that contains the
     * required e<code>DENOPTIMVertex</code>s.
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
                int atmID = mol.getAtomNumber(atm);
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

        if (!GeometryTools.has2DCoordinates(mol))
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
            
            GeometryTools.scaleMolecule(iac, 0.9);
            GeometryTools.translateAllPositive(iac);
            // the draw area and the image should be the same size
            Rectangle drawArea = new Rectangle(WIDTH, HEIGHT);
            Image image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_4BYTE_ABGR);

            // generators make the image elements
            ArrayList<IGenerator<IAtomContainer>> generators = new ArrayList<>(); 
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
            model.set(BasicAtomGenerator.CompactShape.class, BasicAtomGenerator.Shape.OVAL);

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
            renderer.paint(iac, new AWTDrawVisitor(g), new Rectangle2D.Double(0, 0, WIDTH, HEIGHT), true);
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

//-----------------------------------------------------------------------------

}
