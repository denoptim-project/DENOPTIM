package denoptim.fragmenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.isomorphism.Mappings;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import denoptim.constants.DENOPTIMConstants;
import denoptim.graph.APClass;
import denoptim.graph.Fragment;
import denoptim.graph.Vertex.BBType;
import denoptim.programs.fragmenter.CuttingRule;
import denoptim.programs.fragmenter.FragmenterParameters;
import denoptim.programs.fragmenter.MatchedBond;
import denoptim.utils.ManySMARTSQuery;

/**
 * Unit test for fragmenter's tools.
 */
public class FragmenterToolsTest
{

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
        
        Map<String, ArrayList<MatchedBond>> matches = 
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
        formulaB.put("O", 2.0);
        formulaMin.add(formulaA);
        formulaMin.add(formulaB);
        settings.setFormulaCriteriaLessThan(formulaMin);
        assertFalse(FragmenterTools.filterFragment(frag1, settings));
        assertTrue(FragmenterTools.filterFragment(frag2, settings));
        assertFalse(FragmenterTools.filterFragment(frag3, settings));
        assertFalse(FragmenterTools.filterFragment(frag4, settings));
        
        //Formula more-than
        Set<Map<String,Double>> formulaMax = new HashSet<Map<String,Double>>();
        Map<String,Double> formulaC = new HashMap<String,Double>();
        formulaC.put("C", 3.0);
        formulaC.put("H", 2.0);
        Map<String,Double> formulaD = new HashMap<String,Double>();
        formulaD.put("O", 1.0);
        formulaMax.add(formulaC);
        formulaMax.add(formulaD);
        settings = new FragmenterParameters();
        settings.setFormulaCriteriaMoreThan(formulaMax);
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
        smarts.put("C", "C!@-C");
        smarts.put("D", "CCl");
        settings = new FragmenterParameters();
        settings.setFragRetentionSMARTS(smarts);
        assertFalse(FragmenterTools.filterFragment(frag1, settings));
        assertTrue(FragmenterTools.filterFragment(frag2, settings));
        assertFalse(FragmenterTools.filterFragment(frag3, settings));
        assertTrue(FragmenterTools.filterFragment(frag4, settings));
    }
}
