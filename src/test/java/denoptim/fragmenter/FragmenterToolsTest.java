package denoptim.fragmenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openscience.cdk.Atom;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import denoptim.constants.DENOPTIMConstants;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.graph.APClass;
import denoptim.graph.DGraph;
import denoptim.graph.Fragment;
import denoptim.graph.GraphPattern;
import denoptim.graph.SymmetricVertexes;
import denoptim.graph.Template;
import denoptim.graph.Template.ContractLevel;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;
import denoptim.io.DenoptimIO;
import denoptim.programs.denovo.GAParameters;
import denoptim.programs.fragmenter.CuttingRule;
import denoptim.programs.fragmenter.FragmenterParameters;
import denoptim.programs.fragmenter.MatchedBond;
import denoptim.utils.MoleculeUtils;

/**
 * Unit test for fragmenter's tools.
 */
public class FragmenterToolsTest
{

    private static final String SEP = System.getProperty("file.separator");
    private static final String NL = System.getProperty("line.separator");

    @TempDir
    static File tempDir;

    /**
     * Private builder of atom containers
     */
    private IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
    
//-----------------------------------------------------------------------------

    @Test
    public void testGetMatchingBonds() throws Exception
    {
        IAtomContainer mol = builder.newAtomContainer();
        mol.addAtom(new Atom("H")); // 0
        mol.addAtom(new Atom("C")); // 1
        mol.addAtom(new Atom("C")); // 2
        mol.addAtom(new Atom("O")); // 3
        mol.addAtom(new Atom("O")); // 4
        mol.addAtom(new Atom("C")); // 5
        mol.addAtom(new Atom("S")); // 6
        mol.addAtom(new Atom("O")); // 7
        mol.addAtom(new Atom("H")); // 8
        mol.addAtom(new Atom("O")); // 9
        mol.addAtom(new Atom("O")); // 10
        mol.addAtom(new Atom("H")); // 11
        mol.addAtom(new Atom("H")); // 12
        mol.addBond(0, 1, IBond.Order.SINGLE);
        mol.addBond(1, 2, IBond.Order.TRIPLE);
        mol.addBond(2, 3, IBond.Order.SINGLE);
        mol.addBond(3, 4, IBond.Order.SINGLE);
        mol.addBond(4, 5, IBond.Order.SINGLE);
        mol.addBond(5, 6, IBond.Order.SINGLE);
        mol.addBond(6, 7, IBond.Order.SINGLE);
        mol.addBond(7, 8, IBond.Order.SINGLE);
        mol.addBond(6, 9, IBond.Order.DOUBLE);
        mol.addBond(6, 10, IBond.Order.DOUBLE);
        mol.addBond(5, 11, IBond.Order.SINGLE);
        mol.addBond(5, 12, IBond.Order.SINGLE);
        
        ArrayList<String> empty = new ArrayList<String>();
        List<CuttingRule> rules = new ArrayList<CuttingRule>();
        rules.add(new CuttingRule("RuleC","[#6]","[#8]","-",-1,empty));
        rules.add(new CuttingRule("RuleA","[#6]","[#6]","#",0,empty));
        rules.add(new CuttingRule("RuleB","[$([#6]#[#6])]","[#8]","-",1,empty));
        rules.add(new CuttingRule("RuleD","[#8]","[S]","-",2,empty));
        Set<String> anyAtomSMARTS = new HashSet<String>();
        anyAtomSMARTS.add("[$([*;!#1])]");
        
        Map<String, List<MatchedBond>> matches = 
                FragmenterTools.getMatchingBondsAllInOne(mol, rules, null);
        
        assertEquals(matches.get("RuleA").size(),2);
        assertEquals(matches.get("RuleB").size(),1);
        assertEquals(matches.get("RuleC").size(),2);
        assertEquals(matches.get("RuleD").size(),1);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Works on this system
     * <pre>
     *    A          B
     *  C-C-C     C-C-C-C-O-O
     *   \|/       \|/_/
     *    Ru-------Fe___
     *   /|       /|\ \ \
     *  C-C      C-C-C-C-C-S-S 
     *   C           D
     * </pre> 
     */
    @Test
    public void testExploreHapticity() throws Exception
    {
        IAtomContainer mol = builder.newAtomContainer();
        mol.addAtom(new Atom("Ru")); // 0
        mol.addAtom(new Atom("Fe")); // 1
        mol.addAtom(new Atom("C")); // 2 A 
        mol.addAtom(new Atom("C")); // 3 A 
        mol.addAtom(new Atom("C")); // 4 A
        mol.addAtom(new Atom("C")); // 5   B
        mol.addAtom(new Atom("C")); // 6   B
        mol.addAtom(new Atom("C")); // 7   B
        mol.addAtom(new Atom("C")); // 8   B
        mol.addAtom(new Atom("C")); // 9    C
        mol.addAtom(new Atom("C")); // 10   C
        mol.addAtom(new Atom("C")); // 11 D
        mol.addAtom(new Atom("C")); // 22 D
        mol.addAtom(new Atom("C")); // 13 D
        mol.addAtom(new Atom("C")); // 14 D
        mol.addAtom(new Atom("C")); // 15 D
        mol.addAtom(new Atom("O")); // 16 
        mol.addAtom(new Atom("O")); // 17
        mol.addAtom(new Atom("S")); // 18
        mol.addAtom(new Atom("S")); // 19  
        mol.addBond(0, 1, IBond.Order.SINGLE);
        
        // A
        mol.addBond(0, 2, IBond.Order.SINGLE);
        mol.addBond(0, 3, IBond.Order.SINGLE);
        mol.addBond(0, 4, IBond.Order.SINGLE);
        mol.addBond(2, 3, IBond.Order.SINGLE);
        mol.addBond(3, 4, IBond.Order.SINGLE);
        
        // C
        mol.addBond(0, 9, IBond.Order.SINGLE);
        mol.addBond(0, 10, IBond.Order.SINGLE);
        mol.addBond(9, 10, IBond.Order.SINGLE);
        
        // B
        mol.addBond(1, 5, IBond.Order.SINGLE);
        mol.addBond(1, 6, IBond.Order.SINGLE);
        mol.addBond(1, 7, IBond.Order.SINGLE);
        mol.addBond(1, 8, IBond.Order.SINGLE);
        mol.addBond(5, 6, IBond.Order.SINGLE);
        mol.addBond(6, 7, IBond.Order.SINGLE);
        mol.addBond(7, 8, IBond.Order.SINGLE);
        
        // D
        mol.addBond(1, 11, IBond.Order.SINGLE);
        mol.addBond(1, 12, IBond.Order.SINGLE);
        mol.addBond(1, 13, IBond.Order.SINGLE);
        mol.addBond(1, 14, IBond.Order.SINGLE);
        mol.addBond(1, 15, IBond.Order.SINGLE);
        mol.addBond(11, 12, IBond.Order.SINGLE);
        mol.addBond(12, 13, IBond.Order.SINGLE);
        mol.addBond(13, 14, IBond.Order.SINGLE);
        mol.addBond(14, 15, IBond.Order.SINGLE);
        
        // not in hapto
        mol.addBond(8, 16, IBond.Order.SINGLE);
        mol.addBond(17, 16, IBond.Order.SINGLE);
        mol.addBond(15, 18, IBond.Order.SINGLE);
        mol.addBond(18, 19, IBond.Order.SINGLE);
        
        ArrayList<IAtom> candidates = new ArrayList<IAtom>();
        candidates.add(mol.getAtom(2));
        candidates.add(mol.getAtom(3));
        candidates.add(mol.getAtom(4));
        candidates.add(mol.getAtom(5));
        candidates.add(mol.getAtom(6));
        candidates.add(mol.getAtom(7));
        candidates.add(mol.getAtom(8));
        candidates.add(mol.getAtom(9));
        candidates.add(mol.getAtom(10));
        candidates.add(mol.getAtom(11));
        candidates.add(mol.getAtom(12));
        candidates.add(mol.getAtom(13));
        candidates.add(mol.getAtom(14));
        candidates.add(mol.getAtom(15));
        
        Set<IAtom> inHapto = FragmenterTools.exploreHapticity(mol.getAtom(2), 
                mol.getAtom(0), candidates, mol);
        assertEquals(3,inHapto.size());
        assertTrue(inHapto.contains(mol.getAtom(2)));
        assertTrue(inHapto.contains(mol.getAtom(3)));
        assertTrue(inHapto.contains(mol.getAtom(4)));
        
        inHapto = FragmenterTools.exploreHapticity(mol.getAtom(9), 
                mol.getAtom(0), candidates, mol);
        assertEquals(2,inHapto.size());
        assertTrue(inHapto.contains(mol.getAtom(9)));
        assertTrue(inHapto.contains(mol.getAtom(10)));
        
        inHapto = FragmenterTools.exploreHapticity(mol.getAtom(8), 
                mol.getAtom(0), candidates, mol);
        assertEquals(4,inHapto.size());
        assertTrue(inHapto.contains(mol.getAtom(5)));
        assertTrue(inHapto.contains(mol.getAtom(6)));
        assertTrue(inHapto.contains(mol.getAtom(7)));
        assertTrue(inHapto.contains(mol.getAtom(8)));
        
        inHapto = FragmenterTools.exploreHapticity(mol.getAtom(11), 
                mol.getAtom(0), candidates, mol);
        assertEquals(5,inHapto.size());
        assertTrue(inHapto.contains(mol.getAtom(11)));
        assertTrue(inHapto.contains(mol.getAtom(12)));
        assertTrue(inHapto.contains(mol.getAtom(13)));
        assertTrue(inHapto.contains(mol.getAtom(14)));
        assertTrue(inHapto.contains(mol.getAtom(15)));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testExploreConnectivity() throws Exception
    {
        IAtomContainer mol = builder.newAtomContainer();
        mol.addAtom(new Atom("C")); // 0
        mol.addAtom(new Atom("C")); // 1
        mol.addAtom(new Atom("C")); // 2 
        mol.addAtom(new Atom("C")); // 3
        mol.addAtom(new Atom("B")); // 4 
        mol.addAtom(new Atom("H")); // 5
        mol.addAtom(new Atom("H")); // 6
        mol.addAtom(new Atom("B")); // 7
        mol.addAtom(new Atom("O")); // 8
        mol.addAtom(new Atom("B")); // 9
        mol.addBond(0, 1, IBond.Order.DOUBLE);
        mol.addBond(1, 2, IBond.Order.DOUBLE);
        mol.addBond(2, 3, IBond.Order.DOUBLE);
        mol.addBond(5, 6, IBond.Order.SINGLE);
        mol.addBond(7, 4, IBond.Order.SINGLE);
        mol.addBond(9, 4, IBond.Order.SINGLE);
        
        Set<IAtom> system = FragmenterTools.exploreConnectivity(mol.getAtom(2), 
                mol);
        assertEquals(4,system.size());
        assertTrue(system.contains(mol.getAtom(0)));
        assertTrue(system.contains(mol.getAtom(1)));
        assertTrue(system.contains(mol.getAtom(2)));
        assertTrue(system.contains(mol.getAtom(3)));
        
        system = FragmenterTools.exploreConnectivity(mol.getAtom(4), mol);
        assertEquals(3,system.size());
        assertTrue(system.contains(mol.getAtom(4)));
        assertTrue(system.contains(mol.getAtom(7)));
        assertTrue(system.contains(mol.getAtom(9)));
        
        system = FragmenterTools.exploreConnectivity(mol.getAtom(6),mol);
        assertEquals(2,system.size());
        assertTrue(system.contains(mol.getAtom(5)));
        assertTrue(system.contains(mol.getAtom(6)));
        
        system = FragmenterTools.exploreConnectivity(mol.getAtom(8),mol);
        assertEquals(1,system.size());
        assertTrue(system.contains(mol.getAtom(8)));
    }
    
//-----------------------------------------------------------------------------

    @Test
    public void testFilterFragment() throws Exception
    {
        // First test default criteria: 
        
        //Rejection of PseudoAtoms
        IAtomContainer mol = builder.newAtomContainer();
        mol.addAtom(new Atom("C"));
        mol.addAtom(new PseudoAtom("R"));
        IAtomContainer mol2 = builder.newAtomContainer();
        mol2.addAtom(new Atom("C"));
        mol2.addAtom(new PseudoAtom("C"));
        IAtomContainer mol3 = builder.newAtomContainer();
        mol3.addAtom(new Atom("C"));
        mol3.addAtom(new PseudoAtom("R"));
        IAtomContainer mol4 = builder.newAtomContainer();
        mol4.addAtom(new Atom("C"));
        mol4.addAtom(new PseudoAtom("*"));
        IAtomContainer mol5 = builder.newAtomContainer();
        mol5.addAtom(new Atom("C"));
        mol5.addAtom(new PseudoAtom(DENOPTIMConstants.DUMMYATMSYMBOL));
        FragmenterParameters settings = new FragmenterParameters(); // default
        Fragment frag1 = new Fragment(mol, BBType.UNDEFINED);
        Fragment frag2 = new Fragment(mol2, BBType.UNDEFINED);
        Fragment frag3 = new Fragment(mol3, BBType.UNDEFINED);
        Fragment frag4 = new Fragment(mol4, BBType.UNDEFINED);
        Fragment frag5 = new Fragment(mol5, BBType.UNDEFINED);
        assertFalse(FragmenterTools.filterFragment(frag1, settings));
        assertFalse(FragmenterTools.filterFragment(frag2, settings));
        assertFalse(FragmenterTools.filterFragment(frag3, settings));
        assertFalse(FragmenterTools.filterFragment(frag4, settings));
        assertTrue(FragmenterTools.filterFragment(frag5, settings));
        
        // Incomplete fragmentation. i.e., AP vector pointing to an atom
        mol = builder.newAtomContainer();
        mol.addAtom(new Atom("C", new Point3d(1,2,3)));
        mol.addAtom(new Atom("C", new Point3d(3,4,5)));
        mol.addAtom(new Atom("C", new Point3d(7,8,9)));
        frag1 = new Fragment(mol, BBType.UNDEFINED);
        frag1.addAP(0, APClass.make("dummy:0"), new Point3d(3,4,5));
        frag2 = new Fragment(mol, BBType.UNDEFINED);
        frag2.addAP(0, APClass.make("dummy:0"), new Point3d(13,14,15));
        settings = new FragmenterParameters(); // default
        assertFalse(FragmenterTools.filterFragment(frag1, settings));
        assertTrue(FragmenterTools.filterFragment(frag2, settings));
        
        //Weird isotopes
        mol = builder.newAtomContainer();
        IAtom c13 = new Atom("C");
        c13.setMassNumber(13);
        mol.addAtom(new Atom("C"));
        mol.addAtom(c13);
        frag1 = new Fragment(mol, BBType.UNDEFINED);
        IAtom d2 = new Atom("H");
        d2.setMassNumber(2);
        mol2 = builder.newAtomContainer();
        mol2.addAtom(new Atom("C"));
        mol2.addAtom(d2);
        frag2 = new Fragment(mol2, BBType.UNDEFINED);
        settings = new FragmenterParameters(); // default
        assertFalse(FragmenterTools.filterFragment(frag1, settings));
        assertFalse(FragmenterTools.filterFragment(frag2, settings));
        // Deactivate filtering of isotopes
        settings = new FragmenterParameters(); // default
        settings.setRejectWeirdIsotopes(false);
        assertTrue(FragmenterTools.filterFragment(frag1, settings));
        assertTrue(FragmenterTools.filterFragment(frag2, settings));
        
        // After the defaults, now test customizable criteria
        
        // Reject by element
        mol = builder.newAtomContainer();
        mol.addAtom(new Atom("Zn"));
        mol.addAtom(new Atom("C"));
        mol.addAtom(new Atom("H"));
        frag1 = new Fragment(mol, BBType.UNDEFINED);
        mol2 = builder.newAtomContainer();
        mol2.addAtom(new Atom("C"));
        mol2.addAtom(new Atom("O"));
        frag2 = new Fragment(mol2, BBType.UNDEFINED);
        settings = new FragmenterParameters(); 
        settings.setRejectedElements(new HashSet<>(Arrays.asList("Zn", "O")));
        assertFalse(FragmenterTools.filterFragment(frag1, settings));
        assertFalse(FragmenterTools.filterFragment(frag2, settings));
        
        // Min heavy atom count
        mol = builder.newAtomContainer();
        mol.addAtom(new Atom("C"));
        mol.addAtom(new Atom("C"));
        mol.addAtom(new Atom("C"));
        mol.addAtom(new Atom("H"));
        mol2 = builder.newAtomContainer();
        mol2.addAtom(new Atom("C"));
        mol2.addAtom(new Atom("H"));
        mol2.addAtom(new Atom("H"));
        mol2.addAtom(new Atom("H"));
        mol3 = builder.newAtomContainer();
        mol3.addAtom(new Atom("C"));
        mol3.addAtom(new Atom("C"));
        mol3.addAtom(new Atom("C"));
        mol3.addAtom(new Atom("O"));
        settings = new FragmenterParameters();
        settings.setMinFragHeavyAtomCount(2);
        Fragment fragOK = new Fragment(mol, BBType.UNDEFINED);
        Fragment fragNO = new Fragment(mol2, BBType.UNDEFINED);
        assertTrue(FragmenterTools.filterFragment(fragOK, settings));
        assertFalse(FragmenterTools.filterFragment(fragNO, settings));
        
        // Max heavy atom count
        settings = new FragmenterParameters();
        settings.setMaxFragHeavyAtomCount(2);
        fragOK = new Fragment(mol2, BBType.UNDEFINED);
        fragNO = new Fragment(mol, BBType.UNDEFINED);
        assertTrue(FragmenterTools.filterFragment(fragOK, settings));
        assertFalse(FragmenterTools.filterFragment(fragNO, settings));
        
        // Reject by APClass
        Set<String> apcRejecting = new HashSet<String>();
        apcRejecting.add("badAP");
        apcRejecting.add("badO");
        settings = new FragmenterParameters();
        settings.setRejectedAPClasses(apcRejecting);
        fragOK = new Fragment(mol, BBType.UNDEFINED);
        fragOK.addAP(0, APClass.make("APCgood:0"));
        fragOK.addAP(1, APClass.make("APCgood:1"));
        fragOK.addAP(2, APClass.make("BlaBla:0"));
        fragNO = new Fragment(mol2, BBType.UNDEFINED);
        fragNO.addAP(0, APClass.make("APCgood:0"));
        fragNO.addAP(1, APClass.make("APCgood:1"));
        fragNO.addAP(2, APClass.make("badAPC:0"));
        fragNO.addAP(3, APClass.make("badAPC:1"));
        fragNO.addAP(0, APClass.make("badOne:1"));
        fragNO.addAP(1, APClass.make("BlaBla:0"));
        assertTrue(FragmenterTools.filterFragment(fragOK, settings));
        assertFalse(FragmenterTools.filterFragment(fragNO, settings));
        
        // Reject by APClass combination
        frag3 = new Fragment(mol3, BBType.UNDEFINED);
        frag3.addAP(0, APClass.make("APCgood:0"));
        frag3.addAP(1, APClass.make("bar:2"));
        frag3.addAP(2, APClass.make("badAPC:0"));
        frag3.addAP(3, APClass.make("badAPC:1"));
        frag3.addAP(0, APClass.make("badOne:1"));
        frag3.addAP(1, APClass.make("foo:0"));
        frag4 = new Fragment(mol3, BBType.UNDEFINED);
        frag3.addAP(1, APClass.make("bar:2"));
        frag3.addAP(2, APClass.make("bad:0"));
        frag3.addAP(3, APClass.make("BlaBla:1"));
        frag3.addAP(1, APClass.make("foo:0"));
        Set<String[]> apcCombRejecting = new HashSet<String[]>();
        apcCombRejecting.add(new String[] {"badA","Bla"}); //rejects fragNO
        apcCombRejecting.add(new String[] {"foo","APCgood:0","bar"}); //rejects frag3
        settings = new FragmenterParameters();
        settings.setRejectedAPClassCombinations(apcCombRejecting);
        assertTrue(FragmenterTools.filterFragment(fragOK, settings));
        assertFalse(FragmenterTools.filterFragment(fragNO, settings));
        assertFalse(FragmenterTools.filterFragment(frag3, settings));
        assertTrue(FragmenterTools.filterFragment(frag4, settings));
        
        //Formula less-than
        mol = builder.newAtomContainer(); // C6 H6
        mol.addAtom(new Atom("C"));
        mol.addAtom(new Atom("C"));
        mol.addAtom(new Atom("C"));
        mol.addAtom(new Atom("C"));
        mol.addAtom(new Atom("C"));
        mol.addAtom(new Atom("C"));
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("H"));
        mol2 = builder.newAtomContainer(); // C1 O1 H4
        mol2.addAtom(new Atom("C"));
        mol2.addAtom(new Atom("O"));
        mol2.addAtom(new Atom("H"));
        mol2.addAtom(new Atom("H"));
        mol2.addAtom(new Atom("H"));
        mol2.addAtom(new Atom("H"));
        mol3 = builder.newAtomContainer(); // C4 O2 H4
        mol3.addAtom(new Atom("C")); 
        mol3.addAtom(new Atom("C")); 
        mol3.addAtom(new Atom("C")); 
        mol3.addAtom(new Atom("C")); 
        mol3.addAtom(new Atom("O"));
        mol3.addAtom(new Atom("O"));
        mol3.addAtom(new Atom("H"));
        mol3.addAtom(new Atom("H"));
        mol3.addAtom(new Atom("H"));
        mol3.addAtom(new Atom("H"));
        mol4 = builder.newAtomContainer(); // C3 H8
        mol4.addAtom(new Atom("C"));
        mol4.addAtom(new Atom("C"));
        mol4.addAtom(new Atom("C"));
        mol4.addAtom(new Atom("H"));
        mol4.addAtom(new Atom("H"));
        mol4.addAtom(new Atom("H"));
        mol4.addAtom(new Atom("H"));
        mol4.addAtom(new Atom("H"));
        mol4.addAtom(new Atom("H"));
        mol4.addAtom(new Atom("H"));
        mol4.addAtom(new Atom("H"));
        frag1 = new Fragment(mol, BBType.UNDEFINED);
        frag2 = new Fragment(mol2, BBType.UNDEFINED);
        frag3 = new Fragment(mol3, BBType.UNDEFINED);
        frag4 = new Fragment(mol4, BBType.UNDEFINED);
        settings = new FragmenterParameters();
        Set<Map<String,Double>> formulaMin = new HashSet<Map<String,Double>>();
        Map<String,Double> formulaA = new HashMap<String,Double>();
        formulaA.put("C", 5.0);
        formulaA.put("H", 6.0);
        Map<String,Double> formulaB = new HashMap<String,Double>();
        formulaB.put("O", 1.0);
        formulaMin.add(formulaA);
        formulaMin.add(formulaB);
        settings.setRejectedFormulaMoreThan(formulaMin);
        assertFalse(FragmenterTools.filterFragment(frag1, settings));
        assertTrue(FragmenterTools.filterFragment(frag2, settings));
        assertFalse(FragmenterTools.filterFragment(frag3, settings));
        assertFalse(FragmenterTools.filterFragment(frag4, settings));
        
        //Formula more-than
        Map<String,Double> formulaMax = new HashMap<String,Double>();
        formulaMax.put("C", 3.0);
        formulaMax.put("H", 2.0);
        formulaMax.put("O", 1.0);
        settings = new FragmenterParameters();
        settings.setRejectedFormulaLessThan(formulaMax);
        assertFalse(FragmenterTools.filterFragment(frag1, settings));
        assertFalse(FragmenterTools.filterFragment(frag2, settings));
        assertTrue(FragmenterTools.filterFragment(frag3, settings));
        assertFalse(FragmenterTools.filterFragment(frag4, settings));
        
        //SMARTS query rejection
        SmilesParser p = new SmilesParser(builder);
        mol = p.parseSmiles("c1ccccc1OC");
        mol2 = p.parseSmiles("CCOOCCl");
        mol3 = p.parseSmiles("CN(C)CC");
        mol4 = p.parseSmiles("c1ccccc1N(C)CCCCl");
        frag1 = new Fragment(mol, BBType.UNDEFINED);
        frag2 = new Fragment(mol2, BBType.UNDEFINED);
        frag3 = new Fragment(mol3, BBType.UNDEFINED);
        frag4 = new Fragment(mol4, BBType.UNDEFINED);
        Map<String,String> smarts = new HashMap<String,String>();
        smarts.put("A", "C!@-CO");
        settings = new FragmenterParameters();
        settings.setFragRejectionSMARTS(smarts);
        assertTrue(FragmenterTools.filterFragment(frag1, settings));
        assertFalse(FragmenterTools.filterFragment(frag2, settings));
        assertTrue(FragmenterTools.filterFragment(frag3, settings));
        assertTrue(FragmenterTools.filterFragment(frag4, settings));
        smarts.put("B", "[#6]O");
        settings = new FragmenterParameters();
        settings.setFragRejectionSMARTS(smarts);
        assertFalse(FragmenterTools.filterFragment(frag1, settings));
        assertFalse(FragmenterTools.filterFragment(frag2, settings));
        assertTrue(FragmenterTools.filterFragment(frag3, settings));
        assertTrue(FragmenterTools.filterFragment(frag4, settings));
        
        //SMARTS query retention
        smarts = new HashMap<String,String>();
        smarts.put("C", "O-O");
        smarts.put("D", "CCl");
        settings = new FragmenterParameters();
        settings.setFragRetentionSMARTS(smarts);
        assertFalse(FragmenterTools.filterFragment(frag1, settings));
        assertTrue(FragmenterTools.filterFragment(frag2, settings));
        assertFalse(FragmenterTools.filterFragment(frag3, settings));
        assertTrue(FragmenterTools.filterFragment(frag4, settings));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetMWSlotIdentifier() throws Exception
    {
        int slotSize = 2;
        IAtomContainer mol1 = builder.newAtomContainer();
        mol1.addAtom(new Atom("H"));
        IAtomContainer mol2 = builder.newAtomContainer();
        mol2.addAtom(new Atom("H"));
        mol2.addAtom(new Atom("H"));
        IAtomContainer mol3 = builder.newAtomContainer();
        mol3.addAtom(new Atom("H"));
        mol3.addAtom(new Atom("H"));
        mol3.addAtom(new Atom("H"));
        IAtomContainer mol4 = builder.newAtomContainer();
        mol4.addAtom(new Atom("C"));
        IAtomContainer mol5 = builder.newAtomContainer();
        mol5.addAtom(new Atom("C"));
        mol5.addAtom(new PseudoAtom(DENOPTIMConstants.DUMMYATMSYMBOL));
        Fragment f1 = new Fragment(mol1, BBType.UNDEFINED);
        Fragment f2 = new Fragment(mol2, BBType.UNDEFINED);
        Fragment f3 = new Fragment(mol3, BBType.UNDEFINED);
        Fragment f4 = new Fragment(mol4, BBType.UNDEFINED);
        Fragment f5 = new Fragment(mol5, BBType.UNDEFINED);
        assertEquals("0-2",FragmenterTools.getMWSlotIdentifier(f1, slotSize));
        assertEquals("2-4",FragmenterTools.getMWSlotIdentifier(f2, slotSize));
        assertEquals("2-4",FragmenterTools.getMWSlotIdentifier(f3, slotSize));
        assertEquals("12-14",FragmenterTools.getMWSlotIdentifier(f4, slotSize));
        assertEquals("12-14",FragmenterTools.getMWSlotIdentifier(f5, slotSize));
    }

//------------------------------------------------------------------------------

    @Test
    public void testMakeGraphFromFragmentationOfMol() throws Exception
    {
        // We use a hard-coded molecule to ensure the 3D geometry is fixed
        assertTrue(tempDir.isDirectory(),"Should be a directory ");
        String structureFile = tempDir.getAbsolutePath() + SEP + "mol.sdf";
        DenoptimIO.writeData(structureFile,
                "" + NL
                + " OpenBabel03302310043D" + NL
                + "" + NL
                + " 33 35  0  0  0  0  0  0  0  0999 V2000" + NL
                + "   -1.5455    1.4965    3.4529 O   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -1.1783    1.0876    2.3182 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -1.8597   -0.0221    1.6796 N   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -3.0535   -0.6083    2.2978 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "    0.0153    1.7383    1.6547 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -0.1038    1.5947    0.1342 C   0  0  1  0  0  0  0  0  0  0  0  0" + NL
                + "    0.9646    2.2439   -0.5585 O   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "    2.0315    1.3700   -0.8940 C   0  0  2  0  0  0  0  0  0  0  0  0" + NL
                + "    3.0842    2.1330   -1.6910 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "    3.5659    3.1894   -0.9391 F   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "    4.1344    1.2898   -2.0067 F   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "    2.5295    2.6237   -2.8597 F   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "    1.5529    0.2692   -1.6691 O   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "    0.4699   -0.4528   -1.1881 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -0.3192    0.1414   -0.2151 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -1.2683   -0.6053    0.4932 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -1.5222   -1.9320    0.0906 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -0.8092   -2.5038   -0.9766 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -1.1169   -3.8201   -1.3518 O   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -0.4725   -4.5263   -2.4070 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "    0.2040   -1.7556   -1.6127 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -3.7666   -0.9444    1.5155 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -3.5865    0.1389    2.9228 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -2.7606   -1.4709    2.9321 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "    0.9449    1.2474    2.0144 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "    0.0550    2.8163    1.9227 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -1.0337    2.1265   -0.1667 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "    2.5075    0.9860    0.0372 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -2.2462   -2.5403    0.6157 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -0.6162   -3.9902   -3.3689 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -0.9204   -5.5374   -2.4930 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "    0.6107   -4.6349   -2.1878 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "    0.8117   -2.1861   -2.3969 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "  1  2  2  0  0  0  0" + NL
                + "  2  3  1  0  0  0  0" + NL
                + "  2  5  1  0  0  0  0" + NL
                + "  3  4  1  0  0  0  0" + NL
                + "  4 22  1  0  0  0  0" + NL
                + "  4 23  1  0  0  0  0" + NL
                + "  4 24  1  0  0  0  0" + NL
                + "  5  6  1  0  0  0  0" + NL
                + "  5 25  1  0  0  0  0" + NL
                + "  5 26  1  0  0  0  0" + NL
                + "  6  7  1  0  0  0  0" + NL
                + "  6 27  1  6  0  0  0" + NL
                + "  7  8  1  0  0  0  0" + NL
                + "  8  9  1  0  0  0  0" + NL
                + "  8 13  1  0  0  0  0" + NL
                + "  8 28  1  1  0  0  0" + NL
                + "  9 10  1  0  0  0  0" + NL
                + "  9 11  1  0  0  0  0" + NL
                + "  9 12  1  0  0  0  0" + NL
                + " 13 14  1  0  0  0  0" + NL
                + " 14 15  2  0  0  0  0" + NL
                + " 15 16  1  0  0  0  0" + NL
                + " 15  6  1  0  0  0  0" + NL
                + " 16 17  2  0  0  0  0" + NL
                + " 16  3  1  0  0  0  0" + NL
                + " 17 18  1  0  0  0  0" + NL
                + " 17 29  1  0  0  0  0" + NL
                + " 18 19  1  0  0  0  0" + NL
                + " 18 21  2  0  0  0  0" + NL
                + " 19 20  1  0  0  0  0" + NL
                + " 20 30  1  0  0  0  0" + NL
                + " 20 31  1  0  0  0  0" + NL
                + " 20 32  1  0  0  0  0" + NL
                + " 21 14  1  0  0  0  0" + NL
                + " 21 33  1  0  0  0  0" + NL
                + "M  END" + NL
                + "$$$$", false);
        IAtomContainer mol = DenoptimIO.readSDFFile(structureFile).get(0);
        
        List<CuttingRule> cuttingRules = new ArrayList<CuttingRule>();
        cuttingRules.add(new CuttingRule("cC", "[c]", "[C]", "~", 0, 
                new ArrayList<String>()));
        cuttingRules.add(new CuttingRule("cN", "[c]", "[#7]", "~", 1, 
                new ArrayList<String>()));
        cuttingRules.add(new CuttingRule("cO", "[c]", "[#8]", "~", 2, 
                new ArrayList<String>()));
        cuttingRules.add(new CuttingRule("OC", "[O]", "[C]", "-", 3, 
                new ArrayList<String>()));
        cuttingRules.add(new CuttingRule("CF", "[C]", "[F]", "-", 4, 
                new ArrayList<String>()));
        cuttingRules.add(new CuttingRule("NC", "[N]", "[C]", "-", 5, 
                new ArrayList<String>()));
        
        FragmenterParameters frgParams = new FragmenterParameters();
        frgParams.setCuttingRules(cuttingRules);
        frgParams.setScaffoldingPolicy(ScaffoldingPolicy.LARGEST_FRAGMENT);
        
        DGraph graph = FragmenterTools.makeGraphFromFragmentationOfMol(mol,
                frgParams, new FragmentSpace(), true);
        
        assertEquals(16, graph.getVertexCount());
        assertEquals(15, graph.getEdgeCount());
        assertEquals(2, graph.getRingCount());
        assertEquals(0, graph.getVertexList()
                .stream()
                .filter(v -> v instanceof Template)
                .count());
        
        DGraph graphWithTemplate = graph.embedPatternsInTemplates(
                GraphPattern.RING, new FragmentSpace(), ContractLevel.FIXED);
        
        assertEquals(7, graphWithTemplate.getVertexCount());
        assertEquals(6, graphWithTemplate.getEdgeCount());
        assertEquals(0, graphWithTemplate.getRingCount());
        List<Vertex> templates = graphWithTemplate.getVertexList()
                .stream()
                .filter(v -> v instanceof Template)
                .collect(Collectors.toList());
        assertEquals(1, templates.size());
        Template tmpl = (Template) templates.get(0);
        assertEquals(BBType.SCAFFOLD, tmpl.getBuildingBlockType());
        assertEquals(ContractLevel.FIXED, tmpl.getContractLevel());
        assertEquals(2, tmpl.getInnerGraph().getRingCount());
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testMakeGraphFromFragmentationOfMol_ScaffoldingPolicy() 
            throws Exception
    {
        SmilesParser p = new SmilesParser(builder);
        IAtomContainer mol = p.parseSmiles("c1ccccc1OCN(CC)(C)[Ru](N)(N)C#O");
        
        List<CuttingRule> cuttingRules = new ArrayList<CuttingRule>();
        cuttingRules.add(new CuttingRule("C-O", "[#6]", "[#8]", "-", 2, 
                new ArrayList<String>()));
        cuttingRules.add(new CuttingRule("N-C", "[#7]", "[#6]", "-", 5, 
                new ArrayList<String>()));
        cuttingRules.add(new CuttingRule("Ru-Any", "[Ru]", "[$([*])]", "~", 5, 
                new ArrayList<String>()));
        
        FragmenterParameters frgParams = new FragmenterParameters();
        frgParams.setCuttingRules(cuttingRules);
        frgParams.setScaffoldingPolicy(ScaffoldingPolicy.LARGEST_FRAGMENT);
        
        DGraph graph1 = FragmenterTools.makeGraphFromFragmentationOfMol(mol,
                frgParams, new FragmentSpace(), true);
        
        assertEquals(10, graph1.getVertexCount());
        assertEquals(9, graph1.getEdgeCount());
        assertEquals(0, graph1.getRingCount());
        List<Vertex> scaffolds = graph1.getVertexList()
                .stream()
                .filter(v -> BBType.SCAFFOLD == v.getBuildingBlockType())
                .collect(Collectors.toList());
        assertEquals(1, scaffolds.size());
        Vertex scaffold = scaffolds.get(0);
        IAtomContainer iacScaffold = scaffold.getIAtomContainer();
        assertEquals(6, iacScaffold.getAtomCount());
        
        ScaffoldingPolicy policy = ScaffoldingPolicy.ELEMENT;
        policy.label = "Ru";
        
        frgParams.setScaffoldingPolicy(policy);
        DGraph graph2 = FragmenterTools.makeGraphFromFragmentationOfMol(mol,
                frgParams, new FragmentSpace(), true);
        
        assertEquals(10, graph2.getVertexCount());
        assertEquals(9, graph2.getEdgeCount());
        assertEquals(0, graph2.getRingCount());
        scaffolds = graph2.getVertexList()
                .stream()
                .filter(v -> BBType.SCAFFOLD == v.getBuildingBlockType())
                .collect(Collectors.toList());
        assertEquals(1, scaffolds.size());
        scaffold = scaffolds.get(0);
        iacScaffold = scaffold.getIAtomContainer();
        assertEquals(1, iacScaffold.getAtomCount());
        assertEquals("Ru", iacScaffold.getAtom(0).getSymbol());
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testMakeGraphFromFragmentationOfMol_Symmetry() throws Exception
    {
        SmilesParser p = new SmilesParser(builder);
        IAtomContainer mol = p.parseSmiles(
                "Oc1cc(O)cc(O)c1CN(CC)(CC)[Ru](N)(Nc1c(F)cc(F)cc1(Cl))C#O");
        
        List<CuttingRule> cuttingRules = new ArrayList<CuttingRule>();
        cuttingRules.add(new CuttingRule("C-F", "[#6]", "[#9]", "-", 1, 
                new ArrayList<String>()));
        cuttingRules.add(new CuttingRule("C-O", "[#6]", "[#8]", "-", 2, 
                new ArrayList<String>()));
        cuttingRules.add(new CuttingRule("N-C", "[#7]", "[#6]", "-", 5, 
                new ArrayList<String>()));
        cuttingRules.add(new CuttingRule("Ru-Any", "[Ru]", "[$([*])]", "~", 5, 
                new ArrayList<String>()));
        
        FragmenterParameters frgParams = new FragmenterParameters();
        frgParams.setCuttingRules(cuttingRules);
        frgParams.setScaffoldingPolicy(ScaffoldingPolicy.LARGEST_FRAGMENT);

        DGraph graph1 = FragmenterTools.makeGraphFromFragmentationOfMol(mol,
                frgParams, new FragmentSpace(), true);
        
        assertEquals(14, graph1.getVertexCount());
        assertEquals(13, graph1.getEdgeCount());
        assertEquals(0, graph1.getRingCount());
        assertEquals(4, graph1.getSymmetricSetCount());
        boolean foundO = false;
        boolean foundF = false;
        Iterator<SymmetricVertexes> iter = graph1.getSymSetsIterator();
        while (iter.hasNext())
        {
            SymmetricVertexes ss = iter.next();
            String el = ss.get(0).getIAtomContainer().getAtom(0).getSymbol();
            if ("O".equals(el))
                foundO = true;
            else if ("F".equals(el))
                foundF = true;
        }
        assertTrue(foundO);
        assertTrue(foundF);
        
        ScaffoldingPolicy policy = ScaffoldingPolicy.ELEMENT;
        policy.label = "Cl";
        
        frgParams.setScaffoldingPolicy(policy);

        DGraph graph2 = FragmenterTools.makeGraphFromFragmentationOfMol(mol,
                frgParams, new FragmentSpace(), true);
        
        assertEquals(14, graph2.getVertexCount());
        assertEquals(13, graph2.getEdgeCount());
        assertEquals(0, graph2.getRingCount());
        assertEquals(4, graph2.getSymmetricSetCount());
        foundO = false;
        foundF = false;
        iter = graph2.getSymSetsIterator();
        while (iter.hasNext())
        {
            SymmetricVertexes ss = iter.next();
            String el = ss.get(0).getIAtomContainer().getAtom(0).getSymbol();
            if ("O".equals(el))
                foundO = true;
            else if ("F".equals(el))
                foundF = true;
        }
        assertTrue(foundO);
        assertTrue(foundF);
        

        policy = ScaffoldingPolicy.ELEMENT;
        policy.label = "F";
        
        frgParams.setScaffoldingPolicy(policy);

        DGraph graph3 = FragmenterTools.makeGraphFromFragmentationOfMol(mol,
                frgParams, new FragmentSpace(), true);

        assertEquals(14, graph3.getVertexCount());
        assertEquals(13, graph3.getEdgeCount());
        assertEquals(0, graph3.getRingCount());
        assertEquals(3, graph3.getSymmetricSetCount());
        foundO = false;
        foundF = false;
        iter = graph3.getSymSetsIterator();
        while (iter.hasNext())
        {
            SymmetricVertexes ss = iter.next();
            String el = ss.get(0).getIAtomContainer().getAtom(0).getSymbol();
            if ("O".equals(el))
                foundO = true;
            else if ("F".equals(el))
                foundF = true;
        }
        assertTrue(foundO);
        assertFalse(foundF);
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testMakeGraphFromFragmentationOfMol_linearities() 
            throws Exception
    {
        SmilesParser p = new SmilesParser(builder);
        IAtomContainer mol = p.parseSmiles("C#C-C#C-C#N");

        // 2D is enough to get linearities. No need to waste time getting 3D
        StructureDiagramGenerator sdg = new StructureDiagramGenerator();
        sdg.generateCoordinates(mol);
        
        List<CuttingRule> cuttingRules = new ArrayList<CuttingRule>();
        cuttingRules.add(new CuttingRule("C-C", "[#6]", "[#6]", "-", 2, 
                new ArrayList<String>()));
        cuttingRules.add(new CuttingRule("C-X", "[#6]", "[#9,#17,#35,#53]", "~",
                3, new ArrayList<String>()));
        
        // Must add du on linearities
        FragmenterParameters frgParams = new FragmenterParameters();
        frgParams.setCuttingRules(cuttingRules);
        frgParams.setScaffoldingPolicy(ScaffoldingPolicy.LARGEST_FRAGMENT);
        frgParams.setLinearAngleLimit(170);

        DGraph graph = FragmenterTools.makeGraphFromFragmentationOfMol(mol,
                frgParams, new FragmentSpace(), true);
        
        assertEquals(3, graph.getVertexCount());
        assertEquals(2, graph.getEdgeCount());
        assertEquals(0, graph.getRingCount());
        int duAtmCount = 0;
        for (Vertex v : graph.getVertexList())
        {
            for (IAtom a : v.getIAtomContainer().atoms())
            {
                if (DENOPTIMConstants.DUMMYATMSYMBOL.equals(
                        MoleculeUtils.getSymbolOrLabel(a)))
                    duAtmCount++;
            }
             
        }
        assertEquals(4, duAtmCount);
        
        // Without linearities
        IAtomContainer molNonLinear = p.parseSmiles("C(Cl)(F)Br");
        sdg.generateCoordinates(molNonLinear);
        
        frgParams.setLinearAngleLimit(160);
        DGraph graph1 = FragmenterTools.makeGraphFromFragmentationOfMol(molNonLinear,
                frgParams, new FragmentSpace(), true);
        
        assertEquals(4, graph1.getVertexCount());
        assertEquals(3, graph1.getEdgeCount());
        assertEquals(0, graph1.getRingCount());
        duAtmCount = 0;
        for (Vertex v : graph1.getVertexList())
        {
            for (IAtom a : v.getIAtomContainer().atoms())
            {
                if (DENOPTIMConstants.DUMMYATMSYMBOL.equals(
                        MoleculeUtils.getSymbolOrLabel(a)))
                    duAtmCount++;
            }
             
        }
        assertEquals(0, duAtmCount);
        
        // there are linearities but we do not want Du on linearities.
        // An angle limit of 190 never matches, because a bond angle is at most 180.
        frgParams.setScaffoldingPolicy(ScaffoldingPolicy.LARGEST_FRAGMENT);
        frgParams.setLinearAngleLimit(190);
        DGraph graph2 = FragmenterTools.makeGraphFromFragmentationOfMol(mol,
                frgParams, new FragmentSpace(), true);
        
        assertEquals(3, graph2.getVertexCount());
        assertEquals(2, graph2.getEdgeCount());
        assertEquals(0, graph2.getRingCount());
        duAtmCount = 0;
        for (Vertex v : graph2.getVertexList())
        {
            for (IAtom a : v.getIAtomContainer().atoms())
            {
                if (DENOPTIMConstants.DUMMYATMSYMBOL.equals(
                        MoleculeUtils.getSymbolOrLabel(a)))
                    duAtmCount++;
            }
             
        }
        assertEquals(0, duAtmCount);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Test the detection of "some" symmetry.: a topology that would result in
     * something close to a C3 point group geometry. Three symmetric branches
     * that form a three-cyclic system. This method tests the capacity to 
     * detect that vertexes that have no sym set of APs but can be flagged as 
     * symmetric because they are part of a chain rooted on a vertex with sym 
     * APs (i.e., the bridge head).
     */
    @Test
    public void testMakeGraphFromFragmentationOfMol_symmetry() throws Exception
    {
        SmilesParser p = new SmilesParser(builder);
        IAtomContainer mol = p.parseSmiles(
                "P123C(OC[SiH2]O1)(OC[SiH2]O2)OC[SiH2]O3");
        
        List<CuttingRule> cuttingRules = new ArrayList<CuttingRule>();
        cuttingRules.add(new CuttingRule("PC-O", "[$(CP)]", "[O]", "-", -1, 
                new ArrayList<String>()));
        cuttingRules.add(new CuttingRule("C-O", "[C]", "[O]", "-", 0, 
                new ArrayList<String>()));
        cuttingRules.add(new CuttingRule("C-Si", "[C]", "[Si]", "-", 1, 
                new ArrayList<String>()));
        cuttingRules.add(new CuttingRule("Si-O", "[Si]", "[O]", "-", 2, 
                new ArrayList<String>()));
        cuttingRules.add(new CuttingRule("P-C", "[P]", "[C]", "-", 3, 
                new ArrayList<String>()));
        cuttingRules.add(new CuttingRule("P-O", "[P]", "[O]", "-", 4, 
                new ArrayList<String>()));
        
        ScaffoldingPolicy scaffoldOnP = ScaffoldingPolicy.ELEMENT;
        scaffoldOnP.label = "P";

        FragmenterParameters frgParams = new FragmenterParameters();
        frgParams.setCuttingRules(cuttingRules);
        frgParams.setScaffoldingPolicy(scaffoldOnP);
        frgParams.setLinearAngleLimit(170);

        DGraph graph = FragmenterTools.makeGraphFromFragmentationOfMol(mol,
                frgParams, new FragmentSpace(), true);
        
        assertEquals(6, graph.getSymmetricSetCount());
        List<List<Integer>> expected = new ArrayList<List<Integer>>();
        expected.add(Arrays.asList(3, 9, 15));      
        expected.add(Arrays.asList(5, 11, 17));  
        expected.add(Arrays.asList(7, 13, 19));
        expected.add(Arrays.asList(4, 10, 16));
        expected.add(Arrays.asList(2, 8, 14));
        expected.add(Arrays.asList(6, 12, 18));
        for (List<Integer> vrtxIDs : expected)
        {
            SymmetricVertexes sv0 = graph.getSymSetForVertex(
                    graph.getVertexWithId(vrtxIDs.get(0)));
            assertFalse(sv0.isEmpty());
            for (int i=1; i<vrtxIDs.size(); i++)
            {
                SymmetricVertexes svI = graph.getSymSetForVertex(
                        graph.getVertexWithId(vrtxIDs.get(i)));
                assertTrue(sv0 == svI);
            }
        }
        
        IAtomContainer mol2 = p.parseSmiles(
                "C1C(C(C(F)(F)(F))(C(F)(F)(F)))OCCC1((N(C([H])([H])C3=C"
                + "(C(=C(C(=C3[H])[H])[F])[F]))(C([H])([H])C3=C(C(=C(C"
                + "(=C3[H])[H])[F])[F]))))");

        cuttingRules = new ArrayList<CuttingRule>();
        cuttingRules.add(new CuttingRule("C-C", "[#6]", "[#6]", "-!@", -1, 
                new ArrayList<String>()));
        cuttingRules.add(new CuttingRule("C-X", "[#6]", "[#9]", "-", 0, 
                new ArrayList<String>()));
        cuttingRules.add(new CuttingRule("C-N", "[#6]", "[N]", "-", 1, 
                new ArrayList<String>()));
        
        ScaffoldingPolicy scaffold = ScaffoldingPolicy.LARGEST_FRAGMENT;

        frgParams.setCuttingRules(cuttingRules);
        frgParams.setScaffoldingPolicy(scaffold);
        frgParams.setLinearAngleLimit(170);

        graph = FragmenterTools.makeGraphFromFragmentationOfMol(mol2,
                frgParams, new FragmentSpace(), true);
        
        assertEquals(5, graph.getSymmetricSetCount());
        expected = new ArrayList<List<Integer>>();
        expected.add(Arrays.asList(11, 15));      
        expected.add(Arrays.asList(2, 6));  
        expected.add(Arrays.asList(3, 4, 5, 7, 8, 9));
        expected.add(Arrays.asList(13, 14, 17, 18));
        expected.add(Arrays.asList(12, 16));
        for (List<Integer> vrtxIDs : expected)
        {
            SymmetricVertexes sv0 = graph.getSymSetForVertex(
                    graph.getVertexWithId(vrtxIDs.get(0)));
            assertFalse(sv0.isEmpty());
            for (int i=1; i<vrtxIDs.size(); i++)
            {
                SymmetricVertexes svI = graph.getSymSetForVertex(
                        graph.getVertexWithId(vrtxIDs.get(i)));
                assertTrue(sv0 == svI);
            }
        }
        
    }
    
    
//------------------------------------------------------------------------------
    
    @Test
    public void testMakeGraphFromFragmentationOfMol_cappingGroups() 
            throws Exception
    {
        SmilesParser p = new SmilesParser(builder);
        IAtomContainer mol = p.parseSmiles("Cc1cnc(Cl)cc1O");
        MoleculeUtils.explicitHydrogens(mol);

        // 2D is enough to get linearities. No need to waste time getting 3D
        StructureDiagramGenerator sdg = new StructureDiagramGenerator();
        sdg.generateCoordinates(mol);
        
        GAParameters settings = new GAParameters();
        
        List<CuttingRule> cuttingRules = new ArrayList<CuttingRule>();
        cuttingRules.add(new CuttingRule("sAr", "[c]", "[*]", "-", 2, 
                new ArrayList<String>()));
        cuttingRules.add(new CuttingRule("Ox", "[O]", "[*]", "-", 3, 
                new ArrayList<String>()));

        APClass hyd1 = APClass.make("hyd:1");
        APClass sAr0 = APClass.make("sAr:0");
        APClass ox0 = APClass.make("Ox:0");
        Fragment capH = new Fragment();
        capH.addAtom(new Atom("H", new Point3d()));
        capH.addAP(0, hyd1, new Point3d(1.0,0,0));
        
        ArrayList<Vertex> cappingGroups = new ArrayList<Vertex>();
        cappingGroups.add(capH);
        
        HashMap<APClass, APClass> capMap = new HashMap<APClass, APClass>();
        capMap.put(sAr0, hyd1);
        capMap.put(ox0, hyd1);
        
        FragmentSpaceParameters fsp = new FragmentSpaceParameters();
        FragmentSpace fragSpace = new FragmentSpace(fsp,
                new ArrayList<Vertex>(), //no scaffolds
                new ArrayList<Vertex>(), //no fragments
                cappingGroups, //H as capping
                new HashMap<APClass,ArrayList<APClass>>(), 
                capMap, 
                new HashSet<APClass>(), 
                new HashMap<APClass,ArrayList<APClass>>());
        fragSpace.setAPclassBasedApproach(true);
        
        settings.setParameters(fsp);
        
        FragmenterParameters frgParams = new FragmenterParameters();
        frgParams.setCuttingRules(cuttingRules);
        frgParams.setScaffoldingPolicy(ScaffoldingPolicy.LARGEST_FRAGMENT);
        frgParams.setLinearAngleLimit(170);

        // Without a fragment space we do not identify capping groups
        DGraph graph = FragmenterTools.makeGraphFromFragmentationOfMol(mol,
                frgParams, new FragmentSpace(), true);
        assertEquals(7, graph.getVertexCount());
        graph.removeCappingGroups();
        assertEquals(7, graph.getVertexCount());
        
        graph = FragmenterTools.makeGraphFromFragmentationOfMol(mol,
                frgParams, fragSpace, true);

        assertEquals(7, graph.getVertexCount());
        //In case you want to take a look ar the results
        // DenoptimIO.writeGraphToSDF(new File("/tmp/graph.sdf"), graph, false,  
        // true,  settings.getLogger(), settings.getRandomizer());
        graph.removeCappingGroups();
        assertEquals(4, graph.getVertexCount());
    }


}
