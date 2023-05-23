package denoptim.fragmenter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.vecmath.Point3d;

import org.openscience.cdk.Bond;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.io.iterator.IteratingSDFReader;
import org.openscience.cdk.isomorphism.Mappings;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.files.FileFormat;
import denoptim.files.UndetectedFileFormatException;
import denoptim.graph.APClass;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.Fragment;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;
import denoptim.graph.rings.RingClosingAttractor;
import denoptim.io.DenoptimIO;
import denoptim.io.IteratingAtomContainerReader;
import denoptim.programs.RunTimeParameters.ParametersType;
import denoptim.programs.fragmenter.CuttingRule;
import denoptim.programs.fragmenter.FragmenterParameters;
import denoptim.programs.fragmenter.MatchedBond;
import denoptim.utils.DummyAtomHandler;
import denoptim.utils.FormulaUtils;
import denoptim.utils.ManySMARTSQuery;
import denoptim.utils.MoleculeUtils;

public class FragmenterTools
{
    
//------------------------------------------------------------------------------
    
    /**
     * Processes all molecules analyzing the composition of the structure in
     * the chemical representation as compared to the molecular formula 
     * declared in the
     * {@link DENOPTIMConstants#FORMULASTR} property, and extracts those 
     * molecules where the declared formula matches the composition of
     * the chemical representation.
     * @param input the source of chemical structures.
     * @param output the file where to write extracted structures.
     * @param logger a task-dedicated logger where we print messages for the 
     * user.
     * @throws DENOPTIMException
     * @throws IOException
     */
    public static void checkElementalAnalysisAgainstFormula(File input,
            File output, Logger logger) 
                    throws DENOPTIMException, IOException
    {
        FileInputStream fis = new FileInputStream(input);
        IteratingSDFReader reader = new IteratingSDFReader(fis, 
                DefaultChemObjectBuilder.getInstance());

        int index = -1;
        int maxBufferSize = 2000;
        ArrayList<IAtomContainer> buffer = new ArrayList<IAtomContainer>(500);
        try {
            while (reader.hasNext())
            {
                index++;
                if (logger!=null)
                {
                    logger.log(Level.FINE,"Checking elemental analysis of "
                            + "structure " + index);
                }
                IAtomContainer mol = reader.next();
                if (mol.getProperty(DENOPTIMConstants.FORMULASTR)==null)
                {
                    throw new Error("Property '" + DENOPTIMConstants.FORMULASTR 
                            + "' not found in molecule " + index + " in file "
                            + input + ". Cannot compare formula with elemental"
                            + "analysis.");
                }
                String formula = mol.getProperty(DENOPTIMConstants.FORMULASTR)
                        .toString();
                
                if (FormulaUtils.compareFormulaAndElementalAnalysis(formula, 
                        mol, logger))
                {
                    buffer.add(mol);
                } else {
                    if (logger!=null)
                    {
                        logger.log(Level.INFO,"Inconsistency between elemental "
                                + "analysis of structure and molecular formula."
                                + " Rejecting structure " + index + ": " 
                                + mol.getTitle());
                    }
                }
                
                // If max buffer size is reached, then bump to file
                if (buffer.size() >= maxBufferSize)
                {
                    DenoptimIO.writeSDFFile(output.getAbsolutePath(), buffer, 
                            true);
                    buffer.clear();
                }
            }
        }
        finally {
            reader.close();
        }
        if (buffer.size() < maxBufferSize)
        {
            DenoptimIO.writeSDFFile(output.getAbsolutePath(), buffer, true);
            buffer.clear();
        }
    }
    
//------------------------------------------------------------------------------
    

    /** 
     * Do any pre-processing on a {@link IAtomContainer} meant to be fragmented.
     * These are DENOPTIM specific manipulations, in particular meant to
     * comply to requirements to write SDF files: unset bond orders 
     * can only be used in query-type files. So types 4 and 8 are not
     * expected to be found (but CSD uses them...).
     * Also, it deals with implicit H formalism according to the given settings.
     * 
     * @param mol the system that this method prepares to fragmentation.
     * @param settings the settings controlling how the molecule is prepared.
     * @param index identifies the given {@link IAtomContainer} in a collection
     * of systems to work on. This is used only for logging.
     */
    public static boolean prepareMolToFragmentation(IAtomContainer mol, 
            FragmenterParameters settings, int index)
    {
        try
        {
            if (settings.addExplicitH())
            {
                MoleculeUtils.explicitHydrogens(mol);
            } else {
                MoleculeUtils.setZeroImplicitHydrogensToAllAtoms(mol);
            }
            MoleculeUtils.ensureNoUnsetBondOrders(mol);
        } catch (CDKException e)
        {
            if (!settings.acceptUnsetToSingeBO())
            {
                settings.getLogger().log(Level.WARNING,"Some bond order "
                        + "are unset and attempt to kekulize the "
                        + "system has failed "
                        + "for structure " + index + ". "
                        + "This hampers use of SMARTS queries, which "
                        + "may very well "
                        + "not work as expected. Structure " + index 
                        + " will be rejected. "
                        + "You can avoid rejection by using "
                        + "keyword " 
                        + ParametersType.FRG_PARAMS.getKeywordRoot() 
                        + "UNSETTOSINGLEBO, but you'll "
                        + "still be using a peculiar connectivity "
                        + "table were "
                        + "many bonds are artificially marked as "
                        + "single to "
                        + "avoid use of 'UNSET' bond order. "
                        + "Further details on the problem: " 
                        + e.getMessage());
                return false;
            } else {
                settings.getLogger().log(Level.WARNING,"Failed "
                        + "kekulization "
                        + "for structure " + index 
                        + " but UNSETTOSINGLEBO "
                        + "keyword used. Forcing use of single bonds to "
                        + "replace bonds with unset order.");
                for (IBond bnd : mol.bonds())
                {
                    if (bnd.getOrder().equals(IBond.Order.UNSET)) 
                    {
                        bnd.setOrder(IBond.Order.SINGLE);
                    }
                }
            }
        }
        return true;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Removes from the structures anyone that matches any of the given SMARTS 
     * queries.
     * @param input the source of chemical structures.
     * @param smarts the queries leading to rejection.
     * @param output the file where to write extracted structures.
     * @param logger a task-dedicated logger where we print messages for the 
     * user.
     * @throws DENOPTIMException
     * @throws IOException
     */
    public static void filterStrucutresBySMARTS(File input, Set<String> smarts,
            File output, Logger logger) 
                    throws DENOPTIMException, IOException
    {
        FileInputStream fis = new FileInputStream(input);
        IteratingSDFReader reader = new IteratingSDFReader(fis, 
                DefaultChemObjectBuilder.getInstance());

        int i = -1;
        Map<String, String> smartsMap = new HashMap<String, String>();
        for (String s : smarts)
        {
            i++;
            smartsMap.put("prefilter-"+i, s);
        }
        
        int index = -1;
        int maxBufferSize = 2000;
        ArrayList<IAtomContainer> buffer = new ArrayList<IAtomContainer>(500);
        try {
            while (reader.hasNext())
            {
                index++;
                if (logger!=null)
                {
                    logger.log(Level.FINE,"Prefiltering structure " + index);
                }
                IAtomContainer mol = reader.next();
                
                ManySMARTSQuery msq = new ManySMARTSQuery(mol, smartsMap);
                if (msq.hasProblems())
                {
                    String msg = "WARNING! Problems while searching for "
                            + "specific atoms/bonds using SMARTS: " 
                            + msq.getMessage();
                    throw new DENOPTIMException(msg,msq.getProblem());
                }
                Map<String, Mappings> allMatches = msq.getAllMatches();
                
                if (allMatches.size()==0)
                {
                    buffer.add(mol);
                } else {
                    String hits = "";
                    for (String s : allMatches.keySet())
                        hits = hits + DenoptimIO.NL + smartsMap.get(s);
                    if (logger!=null)
                    {
                        logger.log(Level.INFO,"Found match for " + hits
                                + "Rejecting structure " + index + ": " 
                                + mol.getTitle());
                    }
                }
                
                // If max buffer size is reached, then bump to file
                if (buffer.size() >= maxBufferSize)
                {
                    DenoptimIO.writeSDFFile(output.getAbsolutePath(), buffer, 
                            true);
                    buffer.clear();
                }
            }
        } finally {
            reader.close();
        }
        if (buffer.size() < maxBufferSize)
        {
            DenoptimIO.writeSDFFile(output.getAbsolutePath(), buffer, true);
            buffer.clear();
        }
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Performs fragmentation according to the given cutting rules.
     * @param input the source of chemical structures.
     * @param settings configurations including cutting rules and filtration 
     * criteria.
     * @param output the file where to write extracted structures. 
     * @param logger where to direct log messages. This is typically different 
     * from the logger registered in the {@link FragmenterParameters}, which is
     * the master logger, as we want thread-specific logging.
     * @throws DENOPTIMException
     * @throws IOException
     * @throws UndetectedFileFormatException 
     * @throws IllegalArgumentException 
     * @throws CDKException 
     * @return <code>true</code> if the fragmentation produced at least one 
     * fragment that survived post filtering, i.e., the <code>output</code> file
     * does contain something.
     */
    
    public static boolean fragmentation(File input, FragmenterParameters settings,
            File output, Logger logger) throws CDKException, IOException, 
    DENOPTIMException, IllegalArgumentException, UndetectedFileFormatException
    {
        IteratingAtomContainerReader iterator = 
                new IteratingAtomContainerReader(input);

        int totalProd = 0;
        int totalKept = 0;
        int index = -1;
        try {
            while (iterator.hasNext())
            {
                index++;
                if (logger!=null)
                {
                    logger.log(Level.FINE,"Fragmenting structure " + index);
                }
                IAtomContainer mol = iterator.next();
                String molName = "noname-mol" + index;
                if (mol.getTitle()!=null && !mol.getTitle().isBlank())
                    molName = mol.getTitle();
                
                // Generate the fragments
                List<Vertex> fragments = fragmentation(mol, 
                        settings.getCuttingRules(), 
                        logger);
                if (logger!=null)
                {
                    logger.log(Level.FINE,"Fragmentation produced " 
                            + fragments.size() + " fragments.");
                }
                totalProd += fragments.size();
                
                // Post-fragmentation processing of fragments
                List<Vertex> keptFragments = new ArrayList<Vertex>();
                int fragCounter = 0;
                for (Vertex frag : fragments)
                {
                    // Add metadata
                    String fragIdStr = "From_" + molName + "_" + fragCounter;
                    frag.setProperty("cdk:Title", fragIdStr);
                    fragCounter++;
                    manageFragmentCollection(frag, fragCounter, settings,
                            keptFragments, logger);
                }
                if (logger!=null)
                {
                    logger.log(Level.FINE,"Fragments surviving post-"
                            + "processing: " + keptFragments.size());
                }
                totalKept += keptFragments.size();
                if (!settings.doManageIsomorphicFamilies() && totalKept>0)
                {
                    DenoptimIO.writeVertexesToFile(output, FileFormat.VRTXSDF, 
                            keptFragments,true);
                }
            }
        } finally {
            iterator.close();
        }
        
        // Did we actually produce anything? We might not...
        if (totalProd==0)
        {
            if (logger!=null)
            {
                logger.log(Level.WARNING,"No fragment produced. Cutting rules "
                        + "were ineffective on the given structures.");
            }
            return false;
        } else if (totalKept==0)
        {
            if (logger!=null)
            {
                logger.log(Level.WARNING,"No fragment kept out of " + totalProd 
                        + " produced fragments. Filtering criteria might be "
                        + "too restrictive.");
            }
            return false;
        }
        return true;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Chops one chemical structure by applying the given cutting rules.
     * @param mol
     * @param rules
     * @param logger
     * @return the list of fragments
     * @throws DENOPTIMException 
     */
    public static List<Vertex> fragmentation(IAtomContainer mol, 
            List<CuttingRule> rules, Logger logger) throws DENOPTIMException
    {   
        Fragment masterFrag = new Fragment(mol,BBType.UNDEFINED);
        IAtomContainer fragsMol = masterFrag.getIAtomContainer();
        
        // Identify bonds
        Map<String, List<MatchedBond>> matchingbonds = 
                FragmenterTools.getMatchingBondsAllInOne(fragsMol,rules,logger);
        
        // Select bonds to cut and what rule to use for cutting them
        int cutId = -1;
        for (CuttingRule rule : rules) // NB: iterator follows rule's priority
        {
            String ruleName = rule.getName();

            // Skip unmatched rules
            if (!matchingbonds.keySet().contains(ruleName))
                continue;

            for (MatchedBond tb: matchingbonds.get(ruleName)) 
            {
                IAtom atmA = tb.getAtmSubClass0();
                IAtom atmB = tb.getAtmSubClass1();

                //ignore if bond already broken
                if (!fragsMol.getConnectedAtomsList(atmA).contains(atmB))
                { 
                    continue;
                }

                //treatment of n-hapto ligands
                if (rule.isHAPTO())
                {
                    // Get central atom (i.e., the "mono-hapto" side, 
                    // typically the metal)
                    // As a convention the central atom has subclass '0'
                    IAtom centralAtm = atmA;

                    // Get list of candidates for hapto-system: 
                    // they have same cutting Rule and central metal
                    ArrayList<IAtom> candidatesForHapto = new ArrayList<IAtom>();
                    for (MatchedBond tbForHapto : matchingbonds.get(ruleName))
                    {
                        //Consider only bond involving same central atom
                        if (tbForHapto.getAtmSubClass0() == centralAtm)
                            candidatesForHapto.add(tbForHapto.getAtmSubClass1());
                    }

                    // Select atoms in n-hapto system: contiguous neighbors with  
                    // same type of bond with the same central atom.
                    Set<IAtom> atmsInHapto = new HashSet<IAtom>();
                    atmsInHapto.add(tb.getAtmSubClass1());
                    atmsInHapto = exploreHapticity(tb.getAtmSubClass1(), 
                            centralAtm, candidatesForHapto, fragsMol);
                    if (atmsInHapto.size() == 1)
                    {
                        logger.log(Level.WARNING,"Unable to find more than one "
                                + "bond involved in high-hapticity ligand! "
                                + "Bond ignored.");
                        continue;
                    }

                    // Check existence of all bonds involved in multi-hapto system
                    boolean isSystemIntact = true;
                    for (IAtom ligAtm : atmsInHapto)
                    {
                        List<IAtom> nbrsOfLigAtm = 
                                fragsMol.getConnectedAtomsList(ligAtm);
                        if (!nbrsOfLigAtm.contains(centralAtm))
                        {
                            isSystemIntact = false;
                            break;
                        }
                    } 

                    // If not, it means that another rule already acted on the  
                    // system thus kill this attempt without generating du-atom
                    if (!isSystemIntact)
                        continue;

                    // A dummy atom will be used to define attachment point of
                    // ligand with high hapticity
                    Point3d dummyP3d = new Point3d(); //Used also for 2D
                    for (IAtom ligAtm : atmsInHapto)
                    {
                        Point3d ligP3d = MoleculeUtils.getPoint3d(ligAtm);
                        dummyP3d.x = dummyP3d.x + ligP3d.x;
                        dummyP3d.y = dummyP3d.y + ligP3d.y;
                        dummyP3d.z = dummyP3d.z + ligP3d.z;
                    }

                    dummyP3d.x = dummyP3d.x / (double) atmsInHapto.size();
                    dummyP3d.y = dummyP3d.y / (double) atmsInHapto.size();
                    dummyP3d.z = dummyP3d.z / (double) atmsInHapto.size();

                    //Add Dummy atom to molecular object
                    //if no other Du is already in the same position
                    IAtom dummyAtm = null;
                    for (IAtom oldDu : fragsMol.atoms())
                    {
                        if (MoleculeUtils.getSymbolOrLabel(oldDu) 
                                == DENOPTIMConstants.DUMMYATMSYMBOL)
                        {
                            Point3d oldDuP3d = oldDu.getPoint3d();
                            if (oldDuP3d.distance(dummyP3d) < 0.002)
                            {
                                dummyAtm = oldDu;
                                break;
                            }
                        } 
                    }
                
                    if (dummyAtm==null)
                    {
                        dummyAtm = new PseudoAtom(DENOPTIMConstants.DUMMYATMSYMBOL);
                        dummyAtm.setPoint3d(dummyP3d);
                        fragsMol.addAtom(dummyAtm);
                    }

                    // Modify connectivity of atoms involved in high-hapticity 
                    // coordination creation of Du-to-ATM bonds 
                    // By internal convention the bond order is "SINGLE".
                    IBond.Order border = IBond.Order.valueOf("SINGLE");
                    
                    for (IAtom ligAtm : atmsInHapto)
                    {
                        List<IAtom> nbrsOfDu = fragsMol.getConnectedAtomsList(
                                dummyAtm);
                        if (!nbrsOfDu.contains(ligAtm))
                        {
                            // Add bond with dummy
                            Bond bnd = new Bond(dummyAtm,ligAtm,border);
                            fragsMol.addBond(bnd);
                        }
                        // Remove bonds between central and coordinating atoms
                        IBond oldBnd = fragsMol.getBond(centralAtm,ligAtm);
                        fragsMol.removeBond(oldBnd);
                    }
                    
                    // NB: by convention the "first" class (i.e., the ???:0 class)
                    // is always  on the central atom.
                    AttachmentPoint apA = masterFrag.addAPOnAtom(centralAtm, 
                            rule.getAPClass0(), 
                            MoleculeUtils.getPoint3d(dummyAtm));
                    AttachmentPoint apB = masterFrag.addAPOnAtom(dummyAtm, 
                            rule.getAPClass1(), 
                            MoleculeUtils.getPoint3d(centralAtm));

                    cutId++;
                    apA.setCutId(cutId);
                    apB.setCutId(cutId);
                } else {
                    //treatment of mono-hapto ligands
                    IBond bnd = fragsMol.getBond(atmA,atmB);
                    fragsMol.removeBond(bnd);

                    AttachmentPoint apA = masterFrag.addAPOnAtom(atmA, 
                            rule.getAPClass0(), 
                            MoleculeUtils.getPoint3d(atmB));
                    AttachmentPoint apB = masterFrag.addAPOnAtom(atmB, 
                            rule.getAPClass1(), 
                            MoleculeUtils.getPoint3d(atmA));

                    cutId++;
                    apA.setCutId(cutId);
                    apB.setCutId(cutId);
                } //end of if (hapticity>1)
            } //end of loop over matching bonds
        } //end of loop over rules
        
        // Extract isolated fragments
        ArrayList<Vertex>  fragments = new ArrayList<Vertex>();
        Set<Integer> doneAlready = new HashSet<Integer>();
        for (int idx=0 ; idx<masterFrag.getAtomCount(); idx++)
        {
            if (doneAlready.contains(idx))
                continue;
            
            Fragment cloneOfMaster = masterFrag.clone();
            IAtomContainer iac = cloneOfMaster.getIAtomContainer();
            Set<IAtom> atmsToKeep = exploreConnectivity(iac.getAtom(idx), iac);
            atmsToKeep.stream().forEach(atm -> doneAlready.add(iac.indexOf(atm)));
            
            Set<IAtom> atmsToRemove = new HashSet<IAtom>();
            for (IAtom atm : cloneOfMaster.atoms())
            {
                if (!atmsToKeep.contains(atm))
                {
                    atmsToRemove.add(atm);
                }
            }
            cloneOfMaster.removeAtoms(atmsToRemove);
            if (cloneOfMaster.getAttachmentPoints().size()>0)
                fragments.add(cloneOfMaster);
        }
        
        return fragments;
    }
    
//------------------------------------------------------------------------------
    /**
     * Identifies non-central atoms involved in the same n-hapto ligand as 
     * the seed atom.
     * @param seed atom acting as starting point.
     * @param centralAtom the central atom (typically the metal) to which the
     * seed is bonded as part of the multihapto system.
     * @param candidates atoms that may belong to the multihapto ligand (i.e., 
     * atoms that have matches the cutting rule).
     * @param mol the atom container that own all the atoms we work with.
     * @return list of atoms that is confirmed to belong the 
     * multihapto system.
     */

    static Set<IAtom> exploreHapticity(IAtom seed, IAtom centralAtom, 
            ArrayList<IAtom> candidates, IAtomContainer mol)
    {
        Set<IAtom> atmsInHapto = new HashSet<IAtom>();
        atmsInHapto.add(seed);
        ArrayList<IAtom> toVisitAtoms = new ArrayList<IAtom>();
        toVisitAtoms.add(seed);
        ArrayList<IAtom> visitedAtoms = new ArrayList<IAtom>();
        while (toVisitAtoms.size()>0)
        {
            ArrayList<IAtom> toVisitLater = new ArrayList<IAtom>();
            for (IAtom atomInFocus : toVisitAtoms)
            {
                if (visitedAtoms.contains(atomInFocus) 
                        || atomInFocus==centralAtom)
                    continue;
                else 
                    visitedAtoms.add(atomInFocus);
                
                if (candidates.contains(atomInFocus))
                {
                    atmsInHapto.add(atomInFocus);
                    toVisitLater.addAll(mol.getConnectedAtomsList(atomInFocus));
                }
            }
            toVisitAtoms.clear();
            toVisitAtoms.addAll(toVisitLater);
        }
        return atmsInHapto;
    }
    
//------------------------------------------------------------------------------
    /**
     * Explores the connectivity annotating which atoms have been visited.
     * @param seed atom acting as starting point.
     * @param mol the atom container that own all the atoms we work with.
     * @return list of atoms that is confirmed to belong the continuously 
     * connected system of the seed atom.
     */

    static Set<IAtom> exploreConnectivity(IAtom seed, IAtomContainer mol)
    {
        Set<IAtom> atmsReachableFromSeed = new HashSet<IAtom>();
        ArrayList<IAtom> toVisitAtoms = new ArrayList<IAtom>();
        toVisitAtoms.add(seed);
        ArrayList<IAtom> visitedAtoms = new ArrayList<IAtom>();
        while (toVisitAtoms.size()>0)
        {
            ArrayList<IAtom> toVisitLater = new ArrayList<IAtom>();
            for (IAtom atomInFocus : toVisitAtoms)
            {
                if (visitedAtoms.contains(atomInFocus))
                    continue;
                else 
                    visitedAtoms.add(atomInFocus);
                
                atmsReachableFromSeed.add(atomInFocus);
                toVisitLater.addAll(mol.getConnectedAtomsList(atomInFocus));
            }
            toVisitAtoms.clear();
            toVisitAtoms.addAll(toVisitLater);
        }
        return atmsReachableFromSeed;
    }
    
//-----------------------------------------------------------------------------

    /**
     * Identification of the bonds matching a list of SMARTS queries
     * @param mol chemical system to be analyzed
     * @param rules priority-sorted list of cutting rules.
     * @param logger
     * @return the list of matches.
     */

    static Map<String, List<MatchedBond>> getMatchingBondsAllInOne(
            IAtomContainer mol, List<CuttingRule> rules, Logger logger)
    {
        // Collect all SMARTS queries
        Map<String,String> smarts = new HashMap<String,String>();
        for (CuttingRule rule : rules)
        {
            smarts.put(rule.getName(),rule.getWholeSMARTSRule());
        }

        // Prepare a data structure for the return value
        Map<String, List<MatchedBond>> bondsMatchingRules = 
                new HashMap<String, List<MatchedBond>>();

        // Get all the matches to the SMARTS queries
        ManySMARTSQuery msq = new ManySMARTSQuery(mol, smarts);
        if (msq.hasProblems())
        {
            if (logger!=null)
            {
                logger.log(Level.WARNING, "Problem matching SMARTS: " 
                        + msq.getMessage());
            }
            return bondsMatchingRules;
        }

        for (CuttingRule rule : rules)
        {
            String ruleName = rule.getName();

            if (msq.getNumMatchesOfQuery(ruleName) == 0)
            {
                continue;
            }
           
            // Get atoms matching cutting rule queries
            Mappings purgedPairs = msq.getMatchesOfSMARTS(ruleName);
            
            // Evaluate subclass membership and eventually store target bonds
            ArrayList<MatchedBond> bondsMatched = new ArrayList<MatchedBond>();
            for (int[] pair : purgedPairs) 
            {
                if (pair.length!=2)
                {
                    throw new Error("Cutting rule: " + ruleName 
                            + " has identified " + pair.length + " atoms "
                            + "instead of 2. Modify rule to make it find a "
                            + "pair of atoms.");
                }
                MatchedBond tb = new MatchedBond(mol.getAtom(pair[0]),
                        mol.getAtom(pair[1]), rule);
                
                // Apply any further option of the cutting rule
                if (tb.satisfiesRuleOptions(logger))
                    bondsMatched.add(tb);
            }

            if (!bondsMatched.isEmpty())
                bondsMatchingRules.put(ruleName, bondsMatched);
        }

        return bondsMatchingRules;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Management of fragments: includes application of fragment filters, 
     * rejection rules, and collection rules (also of isomorphic fragments, thus
     * management of duplicates) to manipulate collection of fragments.
     */
    public static void manageFragmentCollection(File input, 
            FragmenterParameters settings,
            File output, Logger logger) throws DENOPTIMException, IOException, 
                IllegalArgumentException, UndetectedFileFormatException
    {
        FileInputStream fis = new FileInputStream(input);
        IteratingSDFReader reader = new IteratingSDFReader(fis, 
                DefaultChemObjectBuilder.getInstance());

        int index = -1;
        int maxBufferSize = 2000;
        ArrayList<Vertex> buffer = new ArrayList<Vertex>(500);
        try {
            while (reader.hasNext())
            {
                index++;
                if (logger!=null)
                {
                    logger.log(Level.FINE,"Processing fragment " + index);
                }
                Vertex frag = new Fragment(reader.next(), BBType.UNDEFINED);
                manageFragmentCollection(frag, index, settings,
                        buffer, logger);
                
                // If max buffer size is reached, then bump to file
                if (buffer.size() >= maxBufferSize)
                {
                    DenoptimIO.writeVertexesToFile(output, FileFormat.VRTXSDF, 
                            buffer, true);
                    buffer.clear();
                }
            }
        } finally {
            reader.close();
        }
        if (buffer.size() < maxBufferSize)
        {
            DenoptimIO.writeVertexesToFile(output, FileFormat.VRTXSDF, 
                    buffer, true);
            buffer.clear();
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Management of fragments: includes application of fragment filters, 
     * rejection rules, and collection rules (also of isomorphic fragments, thus
     * management of duplicates) to manipulate collection of fragments.
     * @param frag the fragment to analyze now. This is a fragment that is 
     * candidates to enter the collection of fragments.
     * @param fragCounter fragment index. Used only for logging purposes.
     * @param settings configuration of the filters and methods uses throughout 
     * the fragmentation and pre-/post-processing of fragments.
     * @param collector where accepted fragments are collected. This is the 
     * collection of fragment where we want to put <code>frag</code>.
     * @param logger where to direct all log.
     * @throws DENOPTIMException
     * @throws IllegalArgumentException
     * @throws UndetectedFileFormatException
     * @throws IOException
     */
    public static void manageFragmentCollection(Vertex frag, int fragCounter, 
            FragmenterParameters settings, 
            List<Vertex> collector, Logger logger) 
                    throws DENOPTIMException, IllegalArgumentException, 
                    UndetectedFileFormatException, IOException
    {

        if (!filterFragment((Fragment) frag, settings, logger))
        {
            return;
        }
        
        //Compare with list of fragments to ignore
        if (settings.getIgnorableFragments().size() > 0)
        {
            if (settings.getIgnorableFragments().stream()
                    .anyMatch(ignorable -> ((Fragment)frag)
                            .isIsomorphicTo(ignorable)))
            {
                if (logger!=null)
                {
                    logger.log(Level.FINE,"Fragment " + fragCounter 
                            + " is ignorable.");
                }
                return;
            }
        }
        
        //Compare with list of fragments to retain
        if (settings.getTargetFragments().size() > 0)
        {
            if (!settings.getTargetFragments().stream()
                    .anyMatch(ignorable -> ((Fragment)frag)
                            .isIsomorphicTo(ignorable)))
            {
                if (logger!=null)
                {
                    logger.log(Level.FINE,"Fragment " + fragCounter 
                          + " doesn't match any target: rejected.");
                }
                return;
            }
        }
        
        // Add dummy atoms on linearities
        if (MoleculeUtils.getDimensions(frag.getIAtomContainer())==3 
                && settings.doAddDuOnLinearity())
        {
            DummyAtomHandler.addDummiesOnLinearities((Fragment) frag,
                    settings.getLinearAngleLimit());
        }
        
         // Management of duplicate fragments:
         // -> identify duplicates (isomorphic fragments), 
         // -> keep one (or more, if we want to sample the isomorphs),
         // -> reject the rest.
        if (settings.doManageIsomorphicFamilies())
        {
            synchronized (settings.MANAGEMWSLOTSSLOCK)
            {
                String mwSlotID = getMWSlotIdentifier(frag, 
                        settings.getMWSlotSize());

                File mwFileUnq = settings.getMWSlotFileNameUnqFrags(
                        mwSlotID);
                File mwFileAll = settings.getMWSlotFileNameAllFrags(
                        mwSlotID);
                
                // Compare this fragment with previously seen ones
                Vertex unqVersion = null;
                if (mwFileUnq.exists())
                {
                    ArrayList<Vertex> knownFrags = 
                            DenoptimIO.readVertexes(mwFileUnq,BBType.UNDEFINED);
                    unqVersion = knownFrags.stream()
                        .filter(knownFrag -> 
                            ((Fragment)frag).isIsomorphicTo(knownFrag))
                        .findAny()
                        .orElse(null);
                }
                if (unqVersion!=null)
                {
                    // Identify this unique fragment
                    String isoFamID = unqVersion.getProperty(
                            DENOPTIMConstants.ISOMORPHICFAMILYID)
                            .toString();
                    
                    // Do we already have enough isomorphic family members 
                    // for this fragment?
                    int sampleSize = settings.getIsomorphsCount()
                            .get(isoFamID);
                    if (sampleSize < settings.getIsomorphicSampleSize())
                    {
                        // Add this isomorphic version to the sample
                        frag.setProperty(
                                DENOPTIMConstants.ISOMORPHICFAMILYID,
                                isoFamID);
                        settings.getIsomorphsCount().put(isoFamID,
                                sampleSize+1);
                        DenoptimIO.writeVertexToFile(mwFileAll, 
                                FileFormat.VRTXSDF, frag, true);
                        collector.add(frag);
                    } else {
                        // This would be inefficient in the long run
                        // because it by-passes the splitting by MW. 
                        // Do not do it!
                        /*
                        if (logger!=null)
                        {
                            logger.log(Level.FINE,"Fragment " 
                                  + fragCounter 
                                  + " is isomorphic to unique fragment " 
                                  + unqVersionID + ", but we already "
                                  + "have a sample of " + sampleSize
                                  + ": ignoring this fragment from now "
                                  + "on.");
                        }
                        settings.getIgnorableFragments().add(frag);
                        */
                    }
                } else {
                    // This is a never-seen fragment
                    String isoFamID = settings.newIsomorphicFamilyID();
                    frag.setProperty(
                            DENOPTIMConstants.ISOMORPHICFAMILYID,
                            isoFamID);
                    settings.getIsomorphsCount().put(isoFamID, 1);
                    DenoptimIO.writeVertexToFile(mwFileUnq, 
                            FileFormat.VRTXSDF, frag, true);
                    DenoptimIO.writeVertexToFile(mwFileAll, 
                            FileFormat.VRTXSDF, frag, true);
                    collector.add(frag);
                }
            } // end synchronized block
        } else {
            //If we are here, we did not ask to remove duplicates
            collector.add(frag);
        }
    }

//------------------------------------------------------------------------------
    
    /**
     * Filter fragments according to the criteria defined in the settings. Log
     * is sent to logger in the {@link FragmenterParameters} argument.
     * @param frag the fragment to analyze. 
     * @param settings collection of settings including filtering criteria.
     * @return <code>true</code> if the fragment should be kept, or 
     * <code>false</code> if any of the filtering criteria is offended and,
     * therefore, the fragment should be rejected.
     */
    public static boolean filterFragment(Fragment frag, 
            FragmenterParameters settings)
    {
       return filterFragment(frag, settings, settings.getLogger());
    }

//------------------------------------------------------------------------------
    
    /**
     * Filter fragments according to the criteria defined in the settings.
     * @param frag the fragment to analyze. 
     * @param settings collection of settings including filtering criteria.
     * @param logger where to direct log messages. This is typically different 
     * from the logger registered in the {@link FragmenterParameters}, which is
     * the master logger, as we want thread-specific logging.
     * @return <code>true</code> if the fragment should be kept, or 
     * <code>false</code> if any of the filtering criteria is offended and,
     * therefore, the fragment should be rejected.
     */
    public static boolean filterFragment(Fragment frag, 
            FragmenterParameters settings, Logger logger)
    {
        // Default filtering criteria: get ring of R/*/X/Xx
        for (IAtom atm : frag.atoms())
        {
            if (MoleculeUtils.isElement(atm))
            {
                continue;
            }
            String smb = MoleculeUtils.getSymbolOrLabel(atm);
            if (DENOPTIMConstants.DUMMYATMSYMBOL.equals(smb))
            {
                continue;
            }
            logger.log(Level.FINE,"Removing fragment contains non-element '"
                    + smb + "'");
            return false;
        }

        if (settings.isWorkingIn3D()) 
        {   
            // Incomplete 3D fragmentation: an atom has the same coords of an AP.
            for (AttachmentPoint ap : frag.getAttachmentPoints())
            {
                Point3d ap3d = ap.getDirectionVector();
                if (ap3d!=null)
                {
                    for (IAtom atm : frag.atoms())
                    {
                        Point3d atm3d = MoleculeUtils.getPoint3d(atm);
                        double dist = ap3d.distance(atm3d);
                        if (dist < 0.0002)
                        {
                            logger.log(Level.FINE,"Removing fragment with AP"
                            + frag.getIAtomContainer().indexOf(atm)
                            + " and atom " + MoleculeUtils.getSymbolOrLabel(atm)
                            + " coincide.");
                            return false;
                        }   
                    }
                }
            }
        }
        if (settings.doRejectWeirdIsotopes())
        {
            for (IAtom atm : frag.atoms())
            {
                if (MoleculeUtils.isElement(atm))
                {
                    // Unconfigured isotope has null mass number
                    if (atm.getMassNumber() == null)
                        continue;
                    
                    String symb =  MoleculeUtils.getSymbolOrLabel(atm);
                    int a = atm.getMassNumber();
                    try {
                        IIsotope major = Isotopes.getInstance().getMajorIsotope(symb);
                        if (a != major.getMassNumber())
                        {
                            logger.log(Level.FINE,"Removing fragment containing "
                                    + "isotope "+symb+a+".");
                            return false;
                        }
                    } catch (Throwable t) {
                        logger.log(Level.WARNING,"Not able to perform Isotope"
                                + "detection.");
                    }
                }
                
            }
        }
        
        // User-controlled filtering criteria
        
        if (settings.getRejectedElements().size() > 0)
        {
            for (IAtom atm : frag.atoms())
            {
                String symb = MoleculeUtils.getSymbolOrLabel(atm);
                if (settings.getRejectedElements().contains(symb))
                {
                    logger.log(Level.FINE,"Removing fragment containing '" 
                            + symb + "'.");
                    return false;
                }
            }
        }
        
        if (settings.getRejectedFormulaLessThan().size() > 0
                || settings.getRejectedFormulaMoreThan().size() > 0)
        {
            Map<String,Double> eaMol = FormulaUtils.getElementalanalysis(
                    frag.getIAtomContainer());
            
            for (Map<String,Double> criterion : 
                settings.getRejectedFormulaMoreThan())
            {
                for (String el : criterion.keySet())
                {
                    if (eaMol.containsKey(el))
                    {
                        // -0.5 to make it strictly less-than
                        if (eaMol.get(el) - criterion.get(el) > 0.5)
                        {
                            logger.log(Level.FINE,"Removing fragment that "
                                    + "contains too much '" + el + "' "
                                    + "as requested by formula"
                                    + "-based (more-than) settings (" + el
                                    + eaMol.get(el)  + " > " + criterion + ").");
                            return false;
                        }
                    }
                }
            }
            
            Map<String,Double> criterion = settings.getRejectedFormulaLessThan();
            for (String el : criterion.keySet())
            {
                if (!eaMol.containsKey(el))
                {
                    logger.log(Level.FINE,"Removing fragment that does not "
                            + "contain '" + el + "' as requested by formula"
                            + "-based (less-than) settings.");
                    return false;
                } else {
                    // 0.5 to make it strictly more-than
                    if (eaMol.get(el) - criterion.get(el) < -0.5)
                    {
                        logger.log(Level.FINE,"Removing fragment that "
                                + "contains too little '" + el + "' "
                                + "as requested by formula"
                                + "-based settings (" + el
                                + eaMol.get(el)  + " < " + criterion + ").");
                        return false;
                    }
                }
            }
            
        }
        
        if (settings.getRejectedAPClasses().size() > 0)
        {
            for (APClass apc : frag.getAllAPClasses())
            {
                for (String s : settings.getRejectedAPClasses())
                {
                    if (apc.toString().startsWith(s))
                    {
                        logger.log(Level.FINE,"Removing fragment with APClass "
                                + apc);
                        return false;
                    }    
                }
            }
        }
        
        if (settings.getRejectedAPClassCombinations().size() > 0)
        {
            loopOverCombinations:
            for (String[] conditions : settings.getRejectedAPClassCombinations())
            {
                for (int ip=0; ip<conditions.length; ip++)
                {   
                    String condition = conditions[ip];
                    boolean found = false;
                    for (APClass apc : frag.getAllAPClasses())
                    {
                        if (apc.toString().startsWith(condition))
                        {
                            found = true;
                            continue;
                        }
                    }
                    if (!found)
                        continue loopOverCombinations;
                    // Here we do have at least one AP satisfying the condition.
                }
                // Here we manage or satisfy all conditions. Therefore, we can
                // reject this fragment
                
                String allCondsAsString = "";
                for (int i=0; i<conditions.length; i++)
                    allCondsAsString = allCondsAsString + " " + conditions[i];
                
                logger.log(Level.FINE,"Removing fragment with combination of "
                        + "APClasses matching '" + allCondsAsString + "'.");
                return false;
            }
        }
        
        if (settings.getMaxFragHeavyAtomCount()>0 
                || settings.getMinFragHeavyAtomCount()>0)
        {
            int totHeavyAtm = 0;
            for (IAtom atm : frag.atoms())
            {
                if (MoleculeUtils.isElement(atm))
                {
                    String symb = MoleculeUtils.getSymbolOrLabel(atm);
                    if ((!symb.equals("H")) && (!symb.equals(
                            DENOPTIMConstants.DUMMYATMSYMBOL)))
                        totHeavyAtm++;
                }
            }
            if (settings.getMaxFragHeavyAtomCount() > 0 
                    && totHeavyAtm > settings.getMaxFragHeavyAtomCount())
            {
                logger.log(Level.FINE,"Removing fragment with too many atoms (" 
                        + totHeavyAtm + " < " 
                        + settings.getMaxFragHeavyAtomCount() 
                        + ")");
                return false;
            }
            if (settings.getMinFragHeavyAtomCount() > 0 
                    && totHeavyAtm < settings.getMinFragHeavyAtomCount())
            {
                logger.log(Level.FINE,"Removing fragment with too few atoms (" 
                        + totHeavyAtm + " < " 
                        + settings.getMinFragHeavyAtomCount() 
                        + ")");
                return false;
            }
        }
        
        if (settings.getFragRejectionSMARTS().size() > 0)
        {
            ManySMARTSQuery msq = new ManySMARTSQuery(frag.getIAtomContainer(),
                    settings.getFragRejectionSMARTS());
            if (msq.hasProblems())
            {
                logger.log(Level.WARNING,"Problems evaluating SMARTS-based "
                        + "rejection criteria. " + msq.getMessage());
            }
        
            for (String criterion : settings.getFragRejectionSMARTS().keySet())
            {
                if (msq.getNumMatchesOfQuery(criterion)>0)
                {
                    logger.log(Level.FINE,"Removing fragment that matches "
                            + "SMARTS-based rejection criteria '" + criterion 
                            + "'.");
                    return false;
                }
            }
        }
        
        if (settings.getFragRetentionSMARTS().size() > 0)
        {
            ManySMARTSQuery msq = new ManySMARTSQuery(frag.getIAtomContainer(),
                    settings.getFragRetentionSMARTS());
            if (msq.hasProblems())
            {
                logger.log(Level.WARNING,"Problems evaluating SMARTS-based "
                        + "rejection criteria. " + msq.getMessage());
            }
        
            boolean matchesAny = false;
            for (String criterion : settings.getFragRetentionSMARTS().keySet())
            {
                if (msq.getNumMatchesOfQuery(criterion) > 0)
                {
                    matchesAny = true;
                    break;
                }
            }
            if (!matchesAny)
            {
                logger.log(Level.FINE,"Removing fragment that does not "
                        + "match any SMARTS-based retention criteria.");
                return false;
            }
        }
        return true;
    }
    
//------------------------------------------------------------------------------
  
    /**
     * Determines the name of the MW slot to use when comparing the given
     * fragment with previously stored fragments.
     * @param frag the fragment for which we want the MW slot identifier.
     * @param slotSize the size of the MW slot.
     * @return the MW slot identifier.
     */
    public static String getMWSlotIdentifier(Vertex frag, int slotSize)
    {
        for (IAtom a : frag.getIAtomContainer().atoms())
        {
            if (a.getImplicitHydrogenCount()==null)
                a.setImplicitHydrogenCount(0);
        }
        double mw = AtomContainerManipulator.getMass(frag.getIAtomContainer());
        int slotNum = (int) (mw / (Double.valueOf(slotSize)));
        return slotNum*slotSize + "-" + (slotNum+1)*slotSize;
    }
    
//------------------------------------------------------------------------------
    
    public static Vertex getRCPForAP(AttachmentPoint ap, APClass rcvApClass) 
            throws DENOPTIMException
    {
        IAtomContainer mol = SilentChemObjectBuilder.getInstance()
                .newAtomContainer();
        Point3d apv = ap.getDirectionVector();
        mol.addAtom(new PseudoAtom(RingClosingAttractor.RCALABELPERAPCLASS.get(rcvApClass), 
                new Point3d(
                    Double.valueOf(apv.x),
                    Double.valueOf(apv.y),
                    Double.valueOf(apv.z))));
        
        Fragment rcv = new Fragment(mol, BBType.FRAGMENT);

        Point3d aps = MoleculeUtils.getPoint3d(
                ap.getOwner().getIAtomContainer().getAtom(
                        ap.getAtomPositionNumber()));
        rcv.addAP(0, rcvApClass, new Point3d(
                Double.valueOf(aps.x),
                Double.valueOf(aps.y),
                Double.valueOf(aps.z)));
        return rcv;
    }
    
//------------------------------------------------------------------------------
 
}
