package utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.BitSet;
import java.util.Collections;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.RenderingHints;
import javax.imageio.ImageIO;
import javax.vecmath.Point3d;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;

import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
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
import org.openscience.cdk.geometry.cip.CIPTool;
import org.openscience.cdk.tools.SaturationChecker;
import org.openscience.cdk.geometry.cip.ILigand;
import org.openscience.cdk.geometry.cip.VisitedAtoms;
import org.openscience.cdk.Ring;
import org.openscience.cdk.interfaces.IRingSet;
import org.openscience.cdk.tools.manipulator.RingSetManipulator;
import org.openscience.cdk.ringsearch.SSSRFinder;
import org.openscience.cdk.qsar.DescriptorValue;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.IAtomicDescriptor;
import org.openscience.cdk.qsar.result.IntegerResult;
import org.openscience.cdk.qsar.descriptors.molecular.RotatableBondsCountDescriptor;
import org.openscience.cdk.qsar.descriptors.atomic.AtomDegreeDescriptor;
import org.openscience.cdk.charges.GasteigerMarsiliPartialCharges;
import org.openscience.cdk.qsar.descriptors.molecular.WeightDescriptor;
import org.openscience.cdk.fingerprint.Fingerprinter;
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
import org.openscience.cdk.similarity.Tanimoto;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;


import constants.DENOPTIMConstants;
import exception.DENOPTIMException;
import molecule.DENOPTIMVertex;
import molecule.DENOPTIMGraph;
import molecule.DENOPTIMRing;

//TODO del if writing of failing molecule is not made systematic
import io.DenoptimIO;

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
     * parses the SMILES string and returns the molecule
     * @param smiles string
     * @return the molecule or <code>null</code> if error in smiles
     * @throws exception.DENOPTIMException
     */

    public static IAtomContainer getMoleculeFromSMILES(String smiles)
                                                        throws DENOPTIMException
    {
        IAtomContainer mol = null;
        try
        {
            mol = SMPARSER.parseSmiles(smiles);
        }
        catch (InvalidSmilesException ise)
        {
            throw new DENOPTIMException(ise);
        }
        return mol;
    }

//------------------------------------------------------------------------------

    /**
     * Replace <code>PseudoAtoms</code>s representing ring closing attractors 
     * with H. No change in coordinates
     * @param mol
     * @throws exception.DENOPTIMException
     */
    public static void removeRCA(IAtomContainer mol) throws DENOPTIMException
    {

        // convert PseudoAtoms to H
        ArrayList<IAtom> atmsToRemove = new ArrayList<>();
        ArrayList<IBond> bndsToRemove = new ArrayList<>();
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
     * @throws exception.DENOPTIMException 
     */
    public static void removeRCA(IAtomContainer mol, DENOPTIMGraph graph) 
						      throws DENOPTIMException
    {
	// add ring-closing bonds
	ArrayList<DENOPTIMVertex> usedRcvs = graph.getUsedRCVertices();
        Map<DENOPTIMVertex,ArrayList<Integer>> vIdToAtmId =
                      DENOPTIMMoleculeUtils.getVertexToAtmIdMap(usedRcvs,mol);
	ArrayList<IAtom> atmsToRemove = new ArrayList<>();
	ArrayList<Boolean> doneVrtx = new ArrayList<>(
				  Collections.nCopies(usedRcvs.size(),false));
	for (DENOPTIMVertex v : usedRcvs)
	{
	    if (doneVrtx.get(usedRcvs.indexOf(v)))
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
	    int iSrcH = mol.getAtomNumber(
				        mol.getConnectedAtomsList(aH).get(0));
            int iSrcT = mol.getAtomNumber(
					mol.getConnectedAtomsList(aT).get(0));
	    atmsToRemove.add(aH);
	    atmsToRemove.add(aT);

	    switch (rings.get(0).getBondType())
	    {
	    case (1):
	        mol.addBond(iSrcH, iSrcT, IBond.Order.SINGLE);
		break;
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
	    doneVrtx.set(usedRcvs.indexOf(vH),true);
            doneVrtx.set(usedRcvs.indexOf(vT),true);
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
     * @throws exception.DENOPTIMException
     */

    public static String getSMILESForMolecule(IAtomContainer mol) throws DENOPTIMException
    {

        IAtomContainer fmol = new AtomContainer();
        try 
        {
            fmol = mol.clone();
        }  
        catch (Throwable t) 
        {
            throw new DENOPTIMException(t);
        }

        // remove Dummy atoms
        if (DATMHDLR != null)
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
	    DenoptimIO.writeMolecule("moldeule_causing_failure.sdf",fmol,false);
            throw new DENOPTIMException(iae);
        }
        return smiles;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the list of 3D atom coordinates
     * @param ac
     * @return list of 3D atom coordinates
     */

    public static ArrayList<double[]> getAtomCoordinates(IAtomContainer ac)
    {
        ArrayList<double[]> coords = new ArrayList<>();

        for (IAtom atm : ac.atoms())
        {
            double[] f = new double[3];
            f[0] = atm.getPoint3d().x;
            f[1] = atm.getPoint3d().y;
            f[2] = atm.getPoint3d().z;
            coords.add(f);
        }

        return coords;
    }

//------------------------------------------------------------------------------

    /**
      * Generates 2D coordinates for the molecule
      *
      * @param ac the molecule to layout.
      * @return A new molecule laid out in 2D.  If the molecule already has 2D
      *         coordinates then it is returned unchanged.  If layout fails then
      *         null is returned.
     * @throws exception.DENOPTIMException
      */
    public static IAtomContainer generate2DCoordinates(IAtomContainer ac)
                                                    throws DENOPTIMException
    {

        IAtomContainer fmol = new AtomContainer();
        try 
        { 
            fmol = ac.clone();
        }  
        catch (Throwable t) 
        {
            throw new DENOPTIMException(t);
        }

        // remove Dummy atoms before generating the inchi
        if (DATMHDLR != null)
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
     * @throws exception.DENOPTIMException
     */

    public static ObjectPair getInchiForMolecule(IAtomContainer mol)
                                                        throws DENOPTIMException
    {

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
        if (DATMHDLR != null)
            fmol = DATMHDLR.removeDummyInHapto(fmol);

        // remove PseudoAtoms
        removeRCA(fmol);

        // remove 3D coordinates: otherwise they are used to derive InChIKey
/*
        for (IAtom a : fmol.atoms())
            a.setPoint3d(new Point3d());
*/

        String inchi = null;
        try
        {
            InChIGeneratorFactory factory = InChIGeneratorFactory.getInstance();
            // Get InChIGenerator, this is a non-standard inchi
            InChIGenerator gen = factory.getInChIGenerator(fmol, "AuxNone RecMet SUU");
            INCHI_RET ret = gen.getReturnStatus();
            if (ret == INCHI_RET.WARNING)
            {
                String error = gen.getMessage();
                // InChI generated, but with warning message
                if (error.contains("unusual valence(s)"))
                {
                    //return new ObjectPair(null, error);
                }
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
            return new ObjectPair(null, "No InChii key generated");
    }


//------------------------------------------------------------------------------

    /**
     * Count number of rotatable bonds
     * @param mol
     * @return number of rotatable bonds
     * @throws DENOPTIMException
     */

    public static int getNumberOfRotatableBonds(IAtomContainer mol)
                                                        throws DENOPTIMException
    {
        int value = 0;
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


    private static void updateElementStats(HashMap<String, Integer> elementMap,
                                                                    String key)
    {
        if (elementMap.containsKey(key))
        {
            elementMap.put(key, elementMap.get(key) + 1);
        }
        else
        {
            elementMap.put(key, 1);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Calculates the Bertz complexity index. Uses the vertex degree as the
     * local graph invariant. Hydrogen atoms are ignored
     * Bertz SH: The First General Index of Molecular Complexity. Journal
     * of the American Chemical Society 1981, 103:3241-3243
     * @param mol
     * @return the Bertz complexity index
     */

    public static double getBertzMolecularComplexity(IAtomContainer mol)
    {
        double value = 0, c_a = 0, c_b = 0;
        HashMap<String, Integer> elementMap = new HashMap<>();

        double eta = 0, c_eta = 0;
        String elem = null;
        // calculate the atom degress
        IAtomicDescriptor descriptor  = new AtomDegreeDescriptor();
        // ignore hydrogens
        for (IAtom atom : mol.atoms())
        {
            elem = atom.getSymbol();
            if (elem.compareTo("H") == 0)
                continue;
            int deg = ((IntegerResult)
                       descriptor.calculate(atom, mol)
                       .getValue()).intValue();
            updateElementStats(elementMap, elem);
            eta += deg;
            c_eta += DENOPTIMMathUtils.log2(deg);
        }

        value += (2 * eta * DENOPTIMMathUtils.log2(eta) - c_eta);

        eta = 0; c_eta = 0;
        for (String key : elementMap.keySet())
        {
            int e = elementMap.get(key);
            eta += e;
            c_eta += (e * DENOPTIMMathUtils.log2(e));
        }

        value += (eta * DENOPTIMMathUtils.log2(eta) - c_eta);

        return DENOPTIMMathUtils.roundValue(value, 2);
    }

//------------------------------------------------------------------------------
    /**
     * Calculates the number of spiroatoms in the molecule
     * A spiro atom is the unique common member of two or more otherwise disjoint
     * ring systems (Pure Appl. Chem. 1999;71:531–558.)
     * @param mol The molecule
     * @return nSpiroAtoms number of spiroatoms
     */

    public static int getNumberOfSpiroAtoms(IAtomContainer mol)
    {
        // a spiroatom by definition requires a node degree of at least 4.
        // A spiro compound has two (or three) rings which have only one atom in
        // common and the two (or three) rings are not linked by a bridge.

        int nSpiroAtoms = 0;
        IRingSet allRings = getRingsInMolecule(mol);

        for (IAtom atom : mol.atoms())
        {
            if (isSpiroAtom(atom, mol, allRings))
                nSpiroAtoms++;
        }

        return nSpiroAtoms;
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
        IRingSet allRings = sssrFinder.findSSSR();
        return allRings;
    }

//------------------------------------------------------------------------------

    /**
     * Count the number of fused rings in the molecule
     * @param mol
     * @return the number of fused rings
     */

    public static int countFusedRings(IAtomContainer mol)
    {
        IRingSet rs = getRingsInMolecule(mol);
        int n = 0;
        if (rs != null)
        {
            List<IAtomContainer> lst =
                                    RingSetManipulator.getAllAtomContainers(rs);

            for (int i=0; i<lst.size()-1; i++)
            {
                for (int j=i+1; j<lst.size(); j++)
                {
                    IAtomContainer iac = AtomContainerManipulator.
                                    getIntersection(lst.get(i), lst.get(j));
                    if (iac != null)
                    {
                        n += iac.getBondCount();
                    }
                }
            }
        }

        return n;
    }

//------------------------------------------------------------------------------
    /**
     * Calculates the number of rings with more than 8 atoms in the molecule
     * @param mol The molecule
     * @return nMacroCycles number of macrocycles
     */
    public static int getNumberOfMacroCycles(IAtomContainer mol)
    {
        int nMacroCycles = 0;
        IRingSet allRings = getRingsInMolecule(mol);
        if (allRings != null)
        {
            for (int i = 0; i < allRings.getAtomContainerCount(); i++)
            {
                Ring ring = (Ring) allRings.getAtomContainer(i);
                if (ring.getAtomCount() > 8)
                    nMacroCycles++;
            }
        }

        return nMacroCycles;
    }

//------------------------------------------------------------------------------

    /**
     * Check an atom to see if it has a potential tetrahedral stereo center.
     * This can only be true if:
     * <ol>
     * <li>It has 4 neighbours OR 3 neighbours and a single implicit hydrogen</li>
     * <li>These four neighbours are different according to CIP rules</li>
     * </ol>
     * If these conditions are met, it returns true.
     *
     * @param atom the central atom of the stereocenter
     * @param mol the atom container the atom is in
     * @return <code>true</code> if all conditions for a stereocenter are met
     */
    public static boolean hasPotentialStereoCenter(IAtom atom, IAtomContainer mol)
    {
        List<IAtom> neighbours = mol.getConnectedAtomsList(atom);
        int numNbs = neighbours.size();
        boolean hasImplicitHydrogen = false;
        if (numNbs == 4)
        {
            hasImplicitHydrogen = false;
        }
        else if (numNbs == 3)
        {
            Integer implicitCount = atom.getImplicitHydrogenCount();
            if (implicitCount != null && implicitCount == 1)
            {
                hasImplicitHydrogen = true;
            }
            else
            {
                SaturationChecker checker = new SaturationChecker();
                try
                {
                    if (checker.calculateNumberOfImplicitHydrogens(atom, mol) == 1)
                    {
                        hasImplicitHydrogen = true;
                    }
                }
                catch (CDKException cdke)
                {
                    return false;
                }
            }
            if (!hasImplicitHydrogen)
            {
                return false;
            }
        }
        else if (numNbs > 4)
        {
            return false; // not tetrahedral, anyway
        }
        else if (numNbs < 3)
        {
            return false; // definitely not chiral
        }
        ILigand[] ligands = new ILigand[4];
        int index = 0;
        VisitedAtoms bitSet = new VisitedAtoms();
        int chiralAtomIndex = mol.getAtomNumber(atom);
        for (IAtom neighbour : neighbours)
        {
            int ligandAtomIndex = mol.getAtomNumber(neighbour);
            ligands[index] = CIPTool.defineLigand(
                    mol, bitSet, chiralAtomIndex, ligandAtomIndex);
            index++;
        }
        if (hasImplicitHydrogen)
        {
            ligands[index] = CIPTool.defineLigand(mol, bitSet, chiralAtomIndex,
                                                            CIPTool.HYDROGEN);
        }
        CIPTool.order(ligands);
        return CIPTool.checkIfAllLigandsAreDifferent(ligands);
    }

//------------------------------------------------------------------------------
    /**
     * Calculates the number of stereocenters in the molecule
     * @param mol The molecule
     * @return nStereoCenters number of Stereocenters
     */
    public static int getNumberOfStereoCenters(IAtomContainer mol)
    {
        int nStereoCenters = 0;
        for (IAtom atom : mol.atoms())
        {
            if (hasPotentialStereoCenter(atom, mol))
                nStereoCenters++;
        }

        return nStereoCenters;
    }

//------------------------------------------------------------------------------

    private static boolean isBondInRingSet(IBond bond, IRingSet allRings)
    {
        int m = allRings.getAtomContainerCount();
        for (int i=0; i<m; i++)
        {
            Ring ring = (Ring) allRings.getAtomContainer(i);
            if (ring.contains(bond))
                return true;
        }
        return false;
    }

//------------------------------------------------------------------------------

    // spiro compounds contain two cyclic rings that share one common carbon atom,
    // which is called as the spiroatom.

    private static boolean isSpiroAtom(IAtom atom, IAtomContainer mol,
                                                            IRingSet allRings)
    {
        int numNbs = mol.getConnectedAtomsCount(atom);
        if (numNbs < 4)
            return false;

        // check if this atom has 4 ring bonds
        int k = 0;
        List<IBond> bonds = mol.getConnectedBondsList(atom);
        Iterator<IBond> iter = bonds.iterator();
        while (iter.hasNext())
        {
            IBond bond = iter.next();
            if (isBondInRingSet(bond, allRings))
                k++;
        }

        if (k != 4)
            return false;
        return true;
    }

//------------------------------------------------------------------------------


    /**
     * The heavy atom count
     * @param mol
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
     *
     * @param features1
     * @param features2
     * @return the similarity value between the molecules based on their features
     * @throws DENOPTIMException
     */

    public static double calculate(double[] features1, double[] features2)
                                                        throws DENOPTIMException
    {

        if (features1.length != features2.length)
        {
            throw new DENOPTIMException("Features vectors must be of the same length");
        }

        int n = features1.length;
        double ab = 0.0;
        double a2 = 0.0;
        double b2 = 0.0;

        for (int i = 0; i < n; i++)
        {
            ab += features1[i] * features2[i];
            a2 += features1[i]*features1[i];
            b2 += features2[i]*features2[i];
        }
        return ab/(a2+b2-ab);
    }

//------------------------------------------------------------------------------

    public static double calculateSimilarity(IAtomContainer query,
                                    IAtomContainer target) throws DENOPTIMException
    {
        double tanimoto = 0;
        try
        {
            Fingerprinter fingerprinter = new Fingerprinter();
            BitSet bs1 = fingerprinter.getFingerprint(query);
            BitSet bs2 = fingerprinter.getFingerprint(target);
            tanimoto = Tanimoto.calculate(bs1, bs2);
        }
        catch (CDKException cdke)
        {
            throw new DENOPTIMException(cdke);
        }
        return tanimoto;
    }


//------------------------------------------------------------------------------

    /**
     * Set Gasteiger-Marsilli charges
     * @param mol
     * @throws DENOPTIMException
     */

    public static void setCharges(IAtomContainer mol) throws DENOPTIMException
    {
        try
        {
            GasteigerMarsiliPartialCharges peoe = new GasteigerMarsiliPartialCharges();
            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
            peoe.calculateCharges(mol);
        }
        catch (CDKException ex)
        {
            throw new DENOPTIMException(ex);
        }
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
     * @throws DENOPTIMException
     */

    public static Map<DENOPTIMVertex,ArrayList<Integer>> getVertexToAtmIdMap(
                                              ArrayList<DENOPTIMVertex> vertLst,
                                                             IAtomContainer mol)
                                                        throws DENOPTIMException
    {
        ArrayList<Integer> vertIDs = new ArrayList<>();
        for (int i=0; i<vertLst.size(); i++)
        {
            DENOPTIMVertex v = vertLst.get(i);
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
     * 
     * @param mol
     * @param filename
     * @throws Exception 
     */
    
    public static void moleculeToPNG(IAtomContainer mol, String filename) 
                                                        throws Exception
    {
        IAtomContainer iac;

        if (!GeometryTools.has2DCoordinates(mol))
        {
            iac = generate2DCoordinates(mol);
        }
        else
        {
            iac = mol;
        }
        
        if (iac == null)
        {
            throw new Exception("Failed to generate 2D coordinates.");
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
            //model.set(BasicAtomGenerator.KekuleStructure.class, true);
            model.set(BasicAtomGenerator.AtomRadius.class, 0.5);
            model.set(BasicAtomGenerator.CompactShape.class, BasicAtomGenerator.Shape.OVAL);
            //renderer.setZoom(0.9);
            
            // the call to 'setup' only needs to be done on the first paint
            renderer.setup(iac, drawArea);

            // paint the background
            Graphics2D g = (Graphics2D)image.getGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
		                     RenderingHints.VALUE_ANTIALIAS_ON);
            
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, WIDTH, HEIGHT);
            

            // the paint method also needs a toolkit-specific renderer
            renderer.paint(iac, new AWTDrawVisitor(g), new Rectangle2D.Double(0, 0, WIDTH, HEIGHT), true);
            g.dispose();

            ImageIO.write((RenderedImage)image, "PNG", new File(filename));
        }
        catch (IOException ioe)
        {
            throw ioe;
        }
    }
    
//------------------------------------------------------------------------------    

}
