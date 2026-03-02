package denoptim.utils;

/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no>
 *   and Marco Foscato <marco.foscato@uib.no>
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
import static denoptim.utils.MoleculeUtils.getPoint3d;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.Bond;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

/**
 * Unit test for DENOPTIMMoleculeUtils
 * 
 * @author Marco Foscato
 */

public class MoleculeUtilsTest
{
    private IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetPoint3d() throws Exception
    {
    	IAtom a = new Atom();
    	assertTrue(areCloseEnough(getPoint3d(a).x,0.0));
    	assertTrue(areCloseEnough(getPoint3d(a).y,0.0));
    	assertTrue(areCloseEnough(getPoint3d(a).z,0.0));
    	
    	a.setPoint2d(new Point2d(2.6, -4.2));
    	assertTrue(areCloseEnough(getPoint3d(a).x,2.6));
    	assertTrue(areCloseEnough(getPoint3d(a).y,-4.2));
    	assertTrue(areCloseEnough(getPoint3d(a).z,0.0));
    	
    	a.setPoint3d(new Point3d(2.6, -4.2, 6.4));
    	assertTrue(areCloseEnough(getPoint3d(a).x,2.6));
    	assertTrue(areCloseEnough(getPoint3d(a).y,-4.2));
    	assertTrue(areCloseEnough(getPoint3d(a).z,6.4));
    	
    	assertFalse(areCloseEnough(1.00001, 1.00002));
    }
    
//------------------------------------------------------------------------------

    private boolean areCloseEnough(double a, double b)
    {
    	double delta = 0.0000001;
    	return Math.abs(a-b) <= delta;
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testCalculateCentroid() throws Exception
    {
        IAtomContainer mol = builder.newAtomContainer();
        mol.addAtom(new Atom("H", new Point3d(1.2,-2.9,2.5)));
        mol.addAtom(new Atom("H", new Point3d(2.3,-4.8,4.2)));
        mol.addAtom(new Atom("H", new Point3d(4.5,2.6,-5.4)));
        
        Point3d c = MoleculeUtils.calculateCentroid(mol);
        assertTrue(areCloseEnough(2.6666666,c.x), "Wrong value in X");
        assertTrue(areCloseEnough(-1.7,c.y), "Wrong value in Y");
        assertTrue(areCloseEnough(0.4333333,c.z), "Wrong value in Z");
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testCalculateBondAngleAgreement() throws Exception
    {
        // Create a simple water-like molecule (H-O-H) with known angles
        IAtomContainer template = builder.newAtomContainer();
        IAtom o1 = new Atom("O", new Point3d(0.0, 0.0, 0.0));
        IAtom h1 = new Atom("H", new Point3d(1.0, 0.0, 0.0));
        IAtom h2 = new Atom("H", new Point3d(0.0, 1.0, 0.0));
        template.addAtom(o1);
        template.addAtom(h1);
        template.addAtom(h2);
        template.addBond(new Bond(o1, h1));
        template.addBond(new Bond(o1, h2));
        
        // Create a molecule with the same structure but slightly different angle
        IAtomContainer mol = builder.newAtomContainer();
        IAtom o2 = new Atom("O", new Point3d(0.0, 0.0, 0.0));
        IAtom h3 = new Atom("H", new Point3d(1.0, 0.0, 0.0));
        IAtom h4 = new Atom("H", new Point3d(0.1, 0.995, 0.0)); // Slightly different angle
        mol.addAtom(o2);
        mol.addAtom(h3);
        mol.addAtom(h4);
        mol.addBond(new Bond(o2, h3));
        mol.addBond(new Bond(o2, h4));
        
        // Create mapping
        Map<IAtom, IAtom> mapping = new HashMap<>();
        mapping.put(o1, o2);
        mapping.put(h1, h3);
        mapping.put(h2, h4);
        
        // Calculate bond angle agreement
        double[] rawData = MoleculeUtils.calculateBondAngleAgreement(template, mol, mapping);
        double score = MoleculeUtils.normalizeBondAngleScore(rawData);
        
        // Score should be small (good agreement) but not zero
        assertTrue(score < 10.0, "Bond angle agreement score should be small for similar structures");
        assertTrue(score >= 0.0, "Score should be non-negative");
        
        // Test with identical structures (should have very low score)
        IAtomContainer mol2 = builder.newAtomContainer();
        IAtom o3 = new Atom("O", new Point3d(0.0, 0.0, 0.0));
        IAtom h5 = new Atom("H", new Point3d(1.0, 0.0, 0.0));
        IAtom h6 = new Atom("H", new Point3d(0.0, 1.0, 0.0));
        mol2.addAtom(o3);
        mol2.addAtom(h5);
        mol2.addAtom(h6);
        mol2.addBond(new Bond(o3, h5));
        mol2.addBond(new Bond(o3, h6));
        
        Map<IAtom, IAtom> mapping2 = new HashMap<>();
        mapping2.put(o1, o3);
        mapping2.put(h1, h5);
        mapping2.put(h2, h6);
        
        double[] rawData2 = MoleculeUtils.calculateBondAngleAgreement(template, mol2, mapping2);
        double score2 = MoleculeUtils.normalizeBondAngleScore(rawData2);
        assertTrue(score2 < 0.1, "Identical structures should have very low score");
        
        // Test with incomplete mapping (should return MAX_VALUE)
        Map<IAtom, IAtom> incompleteMapping = new HashMap<>();
        incompleteMapping.put(o1, o2);
        // Missing h1 and h2 mappings
        
        double[] rawData3 = MoleculeUtils.calculateBondAngleAgreement(template, mol, incompleteMapping);
        double score3 = MoleculeUtils.normalizeBondAngleScore(rawData3);
        assertEquals(Double.MAX_VALUE, score3, "Incomplete mapping should return MAX_VALUE, but got " + score3);

        // Test CH4 with different mappings
        // Create CH4 template with tetrahedral geometry
        // Carbon at center, 4 hydrogens at tetrahedral positions
        IAtomContainer ch4Template = builder.newAtomContainer();
        IAtom cTemplate = new Atom("C", new Point3d(0.0, 0.0, 0.0));
        // Tetrahedral positions: normalized to unit distance
        double sqrt3 = Math.sqrt(1.0/3.0);
        IAtom h1Template = new Atom("H", new Point3d(sqrt3, sqrt3, sqrt3));
        IAtom h2Template = new Atom("H", new Point3d(sqrt3, -sqrt3, -sqrt3));
        IAtom h3Template = new Atom("H", new Point3d(-sqrt3, sqrt3, -sqrt3));
        IAtom h4Template = new Atom("H", new Point3d(-sqrt3, -sqrt3, sqrt3));
        ch4Template.addAtom(cTemplate);
        ch4Template.addAtom(h1Template);
        ch4Template.addAtom(h2Template);
        ch4Template.addAtom(h3Template);
        ch4Template.addAtom(h4Template);
        ch4Template.addBond(new Bond(cTemplate, h1Template));
        ch4Template.addBond(new Bond(cTemplate, h2Template));
        ch4Template.addBond(new Bond(cTemplate, h3Template));
        ch4Template.addBond(new Bond(cTemplate, h4Template));
        
        // Create CH4 target with same geometry (perfect match)
        IAtomContainer ch4Target = builder.newAtomContainer();
        IAtom cTarget = new Atom("C", new Point3d(0.0, 0.0, 0.0));
        IAtom h1Target = new Atom("H", new Point3d(sqrt3, sqrt3, sqrt3));
        IAtom h2Target = new Atom("H", new Point3d(sqrt3, -sqrt3, -sqrt3));
        IAtom h3Target = new Atom("H", new Point3d(-sqrt3, sqrt3, -sqrt3));
        IAtom h4Target = new Atom("H", new Point3d(-sqrt3, -sqrt3, sqrt3));
        ch4Target.addAtom(cTarget);
        ch4Target.addAtom(h1Target);
        ch4Target.addAtom(h2Target);
        ch4Target.addAtom(h3Target);
        ch4Target.addAtom(h4Target);
        ch4Target.addBond(new Bond(cTarget, h1Target));
        ch4Target.addBond(new Bond(cTarget, h2Target));
        ch4Target.addBond(new Bond(cTarget, h3Target));
        ch4Target.addBond(new Bond(cTarget, h4Target));
        
        // Test correct mapping (identity mapping)
        Map<IAtom, IAtom> correctMapping = new HashMap<>();
        correctMapping.put(cTemplate, cTarget);
        correctMapping.put(h1Template, h1Target);
        correctMapping.put(h2Template, h2Target);
        correctMapping.put(h3Template, h3Target);
        correctMapping.put(h4Template, h4Target);
        
        double[] rawDataIdentical = MoleculeUtils.calculateBondAngleAgreement(ch4Template, ch4Target, correctMapping);
        double identicalMappingScore = MoleculeUtils.normalizeBondAngleScore(rawDataIdentical);
        assertTrue(identicalMappingScore < 0.1, "Correct mapping should have very low score: " + identicalMappingScore);
        
        // Test swapped mapping (h1 <-> h2)
        Map<IAtom, IAtom> swappedMapping = new HashMap<>();
        swappedMapping.put(cTemplate, cTarget);
        swappedMapping.put(h1Template, h2Target); // Swapped
        swappedMapping.put(h2Template, h1Target); // Swapped
        swappedMapping.put(h3Template, h3Target);
        swappedMapping.put(h4Template, h4Target);
        
        double[] rawDataSwapped = MoleculeUtils.calculateBondAngleAgreement(ch4Template, ch4Target, swappedMapping);
        double swappedScore = MoleculeUtils.normalizeBondAngleScore(rawDataSwapped);
        // Since all H are equivalent in perfect tetrahedron, score should be similar
        assertTrue(swappedScore > identicalMappingScore, 
            "Swapped mapping should give higher score than identical mapping (> "
            + identicalMappingScore + "): " + swappedScore);
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testFindAtomMapping() throws Exception
    {
        Logger logger = Logger.getLogger("TestLogger");
        
        // Create a simple linear molecule: H-C-C-H
        IAtomContainer substructure = builder.newAtomContainer();
        IAtom c1 = new Atom("C", new Point3d(0.0, 0.0, 0.0));
        IAtom c2 = new Atom("C", new Point3d(1.5, 0.0, 0.0));
        substructure.addAtom(c1);
        substructure.addAtom(c2);
        substructure.addBond(new Bond(c1, c2));
        
        // Create a larger molecule containing the substructure: H-C-C-C-H
        IAtomContainer mol = builder.newAtomContainer();
        IAtom h1 = new Atom("H", new Point3d(-1.0, 0.0, 0.0));
        IAtom c3 = new Atom("C", new Point3d(0.0, 0.0, 0.0));
        IAtom c4 = new Atom("C", new Point3d(1.5, 0.0, 0.0));
        IAtom c5 = new Atom("C", new Point3d(3.0, 0.0, 0.0));
        IAtom h2 = new Atom("H", new Point3d(4.0, 0.0, 0.0));
        mol.addAtom(h1);
        mol.addAtom(c3);
        mol.addAtom(c4);
        mol.addAtom(c5);
        mol.addAtom(h2);
        mol.addBond(new Bond(h1, c3));
        mol.addBond(new Bond(c3, c4));
        mol.addBond(new Bond(c4, c5));
        mol.addBond(new Bond(c5, h2));
        
        // Find mapping
        Map<IAtom, IAtom> mapping = MoleculeUtils.findBestAtomMapping(substructure, mol, logger);
        
        // Should find a mapping
        assertNotNull(mapping, "Mapping should not be null");
        assertEquals(2, mapping.size(), "Mapping should contain 2 atoms");
        assertTrue(mapping.containsKey(c1), "Mapping should contain first carbon");
        assertTrue(mapping.containsKey(c2), "Mapping should contain second carbon");
        
        // Verify the mapped atoms are carbons
        IAtom mappedC1 = mapping.get(c1);
        IAtom mappedC2 = mapping.get(c2);
        assertNotNull(mappedC1, "Mapped atom should not be null");
        assertNotNull(mappedC2, "Mapped atom should not be null");
        assertEquals("C", mappedC1.getSymbol(), "Mapped atom should be carbon");
        assertEquals("C", mappedC2.getSymbol(), "Mapped atom should be carbon");
        
        // Verify they are bonded in the molecule
        IBond bond = mol.getBond(mappedC1, mappedC2);
        assertNotNull(bond, "Mapped atoms should be bonded");
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testFindAtomMappingChiral() throws Exception
    {
        Logger logger = Logger.getLogger("TestLogger");
        
        // Create a chiral center: C(H)(F)(Cl)(Cl) - two identical Cl atoms make it non-trivial
        // This creates a chiral molecule where enantiomeric mappings should be distinguished
        IAtomContainer chiralTemplate = builder.newAtomContainer();
        IAtom cTemplate = new Atom("C", new Point3d(0.0, 0.0, 0.0));
        // Create tetrahedral geometry
        double sqrt3 = Math.sqrt(1.0/3.0);
        IAtom hTemplate = new Atom("H", new Point3d(sqrt3, sqrt3, sqrt3));
        IAtom fTemplate = new Atom("F", new Point3d(sqrt3, -sqrt3, -sqrt3));
        IAtom cl1Template = new Atom("Cl", new Point3d(-sqrt3, sqrt3, -sqrt3));
        IAtom cl2Template = new Atom("Cl", new Point3d(-sqrt3, -sqrt3, sqrt3));
        chiralTemplate.addAtom(cTemplate);
        chiralTemplate.addAtom(hTemplate);
        chiralTemplate.addAtom(fTemplate);
        chiralTemplate.addAtom(cl1Template);
        chiralTemplate.addAtom(cl2Template);
        chiralTemplate.addBond(new Bond(cTemplate, hTemplate));
        chiralTemplate.addBond(new Bond(cTemplate, fTemplate));
        chiralTemplate.addBond(new Bond(cTemplate, cl1Template));
        chiralTemplate.addBond(new Bond(cTemplate, cl2Template));
        
        // Create target molecule with same geometry (same chirality)
        IAtomContainer chiralTarget = builder.newAtomContainer();
        IAtom cTarget = new Atom("C", new Point3d(0.0, 0.0, 0.0));
        IAtom hTarget = new Atom("H", new Point3d(sqrt3, sqrt3, sqrt3));
        IAtom fTarget = new Atom("F", new Point3d(sqrt3, -sqrt3, -sqrt3));
        IAtom cl1Target = new Atom("Cl", new Point3d(-sqrt3, sqrt3, -sqrt3));
        IAtom cl2Target = new Atom("Cl", new Point3d(-sqrt3, -sqrt3, sqrt3));
        chiralTarget.addAtom(cTarget);
        chiralTarget.addAtom(hTarget);
        chiralTarget.addAtom(fTarget);
        chiralTarget.addAtom(cl1Target);
        chiralTarget.addAtom(cl2Target);
        chiralTarget.addBond(new Bond(cTarget, hTarget));
        chiralTarget.addBond(new Bond(cTarget, fTarget));
        chiralTarget.addBond(new Bond(cTarget, cl1Target));
        chiralTarget.addBond(new Bond(cTarget, cl2Target));
        
        // Find mapping - should prefer the correct enantiomeric mapping
        Map<IAtom, IAtom> mapping = MoleculeUtils.findBestAtomMapping(chiralTemplate, chiralTarget, logger);
        
        // Should find a mapping
        assertNotNull(mapping, "Mapping should not be null");
        assertEquals(5, mapping.size(), "Mapping should contain all 5 atoms");
        
        // Verify correct mapping (should map H->H, F->F, and Cl->Cl with correct chirality)
        assertEquals(hTarget, mapping.get(hTemplate), "H should map to H");
        assertEquals(fTarget, mapping.get(fTemplate), "F should map to F");
        // Both Cl atoms should map to Cl atoms
        assertTrue(mapping.get(cl1Template).getSymbol().equals("Cl"), "cl1Template should map to Cl");
        assertTrue(mapping.get(cl2Template).getSymbol().equals("Cl"), "cl2Template should map to Cl");
        
        // Test with enantiomeric target (mirror image by swapping the two Cl positions)
        IAtomContainer enantiomerTarget = builder.newAtomContainer();
        IAtom cEnantiomer = new Atom("C", new Point3d(0.0, 0.0, 0.0));
        IAtom hEnantiomer = new Atom("H", new Point3d(sqrt3, sqrt3, sqrt3));
        IAtom fEnantiomer = new Atom("F", new Point3d(sqrt3, -sqrt3, -sqrt3));
        // Swap the two Cl positions to create opposite chirality
        IAtom cl1Enantiomer = new Atom("Cl", new Point3d(-sqrt3, -sqrt3, sqrt3)); // Swapped position
        IAtom cl2Enantiomer = new Atom("Cl", new Point3d(-sqrt3, sqrt3, -sqrt3)); // Swapped position
        enantiomerTarget.addAtom(cEnantiomer);
        enantiomerTarget.addAtom(hEnantiomer);
        enantiomerTarget.addAtom(fEnantiomer);
        enantiomerTarget.addAtom(cl1Enantiomer);
        enantiomerTarget.addAtom(cl2Enantiomer);
        enantiomerTarget.addBond(new Bond(cEnantiomer, hEnantiomer));
        enantiomerTarget.addBond(new Bond(cEnantiomer, fEnantiomer));
        enantiomerTarget.addBond(new Bond(cEnantiomer, cl1Enantiomer));
        enantiomerTarget.addBond(new Bond(cEnantiomer, cl2Enantiomer));
        
        // Find mapping for enantiomer
        Map<IAtom, IAtom> enantiomerMapping = MoleculeUtils.findBestAtomMapping(
                chiralTemplate, enantiomerTarget, logger);
        
        // Should still find a mapping (isomorphism exists)
        assertNotNull(enantiomerMapping, "Enantiomer mapping should not be null");
        assertEquals(5, enantiomerMapping.size(), "Enantiomer mapping should contain all 5 atoms");
        
        // Verify that the mapping swaps the Cl atoms (chirality-sensitive mapping)
        assertEquals(hEnantiomer, enantiomerMapping.get(hTemplate), "H should map to H");
        assertEquals(fEnantiomer, enantiomerMapping.get(fTemplate), "F should map to F");
        // The Cl atoms should be swapped in the mapping
        assertEquals(cl1Enantiomer, enantiomerMapping.get(cl2Template), 
                "cl2Template should map to cl1Enantiomer (swapped due to opposite chirality)");
        assertEquals(cl2Enantiomer, enantiomerMapping.get(cl1Template), 
                "cl1Template should map to cl2Enantiomer (swapped due to opposite chirality)");
        
        // Calculate scores for both mappings to verify chirality sensitivity
        Map<IAtom, IAtom> correctMapping = new HashMap<>();
        correctMapping.put(cTemplate, cTarget);
        correctMapping.put(hTemplate, hTarget);
        correctMapping.put(fTemplate, fTarget);
        correctMapping.put(cl1Template, cl1Target);
        correctMapping.put(cl2Template, cl2Target);
        
        // Mapping with swapped Cl (wrong chirality)
        Map<IAtom, IAtom> swappedClMapping = new HashMap<>();
        swappedClMapping.put(cTemplate, cTarget);
        swappedClMapping.put(hTemplate, hTarget);
        swappedClMapping.put(fTemplate, fTarget);
        swappedClMapping.put(cl1Template, cl2Target); // Swapped
        swappedClMapping.put(cl2Template, cl1Target); // Swapped
        
        // Enantiomer mapping (should be similar to swapped mapping)
        Map<IAtom, IAtom> enantiomerMappingForScore = new HashMap<>();
        enantiomerMappingForScore.put(cTemplate, cEnantiomer);
        enantiomerMappingForScore.put(hTemplate, hEnantiomer);
        enantiomerMappingForScore.put(fTemplate, fEnantiomer);
        enantiomerMappingForScore.put(cl1Template, cl1Enantiomer);
        enantiomerMappingForScore.put(cl2Template, cl2Enantiomer);
        
        double[] rawDataCorrect = MoleculeUtils.calculateBondAngleAgreement(
                chiralTemplate, chiralTarget, correctMapping);
        double correctScore = MoleculeUtils.normalizeBondAngleScore(rawDataCorrect);
        double[] rawDataSwappedCl = MoleculeUtils.calculateBondAngleAgreement(
                chiralTemplate, chiralTarget, swappedClMapping);
        double swappedClScore = MoleculeUtils.normalizeBondAngleScore(rawDataSwappedCl);
        double[] rawDataEnantiomer = MoleculeUtils.calculateBondAngleAgreement(
                chiralTemplate, enantiomerTarget, enantiomerMappingForScore);
        double enantiomerScore = MoleculeUtils.normalizeBondAngleScore(rawDataEnantiomer);
        
        // The swapped Cl mapping should have a higher score due to opposite chirality
        assertTrue(swappedClScore > correctScore,
                "Swapped Cl mapping should have higher score due to opposite chirality. " +
                "Correct: " + correctScore + ", Swapped: " + swappedClScore);
        
        // The enantiomer should also have a higher score
        assertTrue(enantiomerScore > correctScore,
                "Enantiomer mapping should have higher score due to opposite chirality. " +
                "Correct: " + correctScore + ", Enantiomer: " + enantiomerScore);
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testFindAtomMappingNoMatch() throws Exception
    {
        Logger logger = Logger.getLogger("TestLogger");
        
        // Create a substructure with nitrogen
        IAtomContainer substructure = builder.newAtomContainer();
        IAtom n1 = new Atom("N", new Point3d(0.0, 0.0, 0.0));
        IAtom n2 = new Atom("N", new Point3d(1.5, 0.0, 0.0));
        substructure.addAtom(n1);
        substructure.addAtom(n2);
        substructure.addBond(new Bond(n1, n2));
        
        // Create a molecule with only carbons
        IAtomContainer mol = builder.newAtomContainer();
        IAtom c1 = new Atom("C", new Point3d(0.0, 0.0, 0.0));
        IAtom c2 = new Atom("C", new Point3d(1.5, 0.0, 0.0));
        mol.addAtom(c1);
        mol.addAtom(c2);
        mol.addBond(new Bond(c1, c2));
        
        // Find mapping - should return empty map
        Map<IAtom, IAtom> mapping = MoleculeUtils.findBestAtomMapping(substructure, mol, logger);
        
        assertNotNull(mapping, "Mapping should not be null");
        assertTrue(mapping.isEmpty(), "Mapping should be empty when no match found");
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testFindAtomMappingWithMultipleMatches() throws Exception
    {
        Logger logger = Logger.getLogger("TestLogger");
        
        // Create a simple substructure: C-C
        IAtomContainer substructure = builder.newAtomContainer();
        IAtom c1 = new Atom("C", new Point3d(0.0, 0.0, 0.0));
        IAtom c2 = new Atom("C", new Point3d(1.5, 0.0, 0.0));
        substructure.addAtom(c1);
        substructure.addAtom(c2);
        substructure.addBond(new Bond(c1, c2));
        
        // Create a molecule with multiple C-C bonds: C-C-C-C
        // Arrange atoms to have different angles so we can test angle-based selection
        IAtomContainer mol = builder.newAtomContainer();
        IAtom c3 = new Atom("C", new Point3d(0.0, 0.0, 0.0));
        IAtom c4 = new Atom("C", new Point3d(1.5, 0.0, 0.0));
        IAtom c5 = new Atom("C", new Point3d(3.0, 0.0, 0.0));
        IAtom c6 = new Atom("C", new Point3d(4.5, 0.0, 0.0));
        mol.addAtom(c3);
        mol.addAtom(c4);
        mol.addAtom(c5);
        mol.addAtom(c6);
        mol.addBond(new Bond(c3, c4));
        mol.addBond(new Bond(c4, c5));
        mol.addBond(new Bond(c5, c6));
        
        // Find mapping - should select one of the possible matches
        Map<IAtom, IAtom> mapping = MoleculeUtils.findBestAtomMapping(substructure, mol, logger);
        
        assertNotNull(mapping, "Mapping should not be null");
        assertEquals(2, mapping.size(), "Mapping should contain 2 atoms");
        assertTrue(mapping.containsKey(c1), "Mapping should contain first carbon");
        assertTrue(mapping.containsKey(c2), "Mapping should contain second carbon");
        
        // Verify the mapped atoms are bonded
        IAtom mappedC1 = mapping.get(c1);
        IAtom mappedC2 = mapping.get(c2);
        IBond bond = mol.getBond(mappedC1, mappedC2);
        assertNotNull(bond, "Mapped atoms should be bonded");
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testFindUniqueAtomMappingsMultipleOccurrences() throws Exception
    {
        Logger logger = Logger.getLogger("TestLogger");
        
        // Create a simple substructure: C-C (carbon-carbon bond)
        IAtomContainer substructure = builder.newAtomContainer();
        IAtom c1 = new Atom("C", new Point3d(0.0, 0.0, 0.0));
        IAtom c2 = new Atom("C", new Point3d(1.5, 0.0, 0.0));
        substructure.addAtom(c1);
        substructure.addAtom(c2);
        substructure.addBond(new Bond(c1, c2));
        
        // Create a larger molecule with multiple C-C bonds: C-C-C-C
        // This molecule has 3 distinct C-C bonds that match the substructure:
        // 1. c3-c4 (atoms 0-1)
        // 2. c4-c5 (atoms 1-2)
        // 3. c5-c6 (atoms 2-3)
        IAtomContainer mol = builder.newAtomContainer();
        IAtom c3 = new Atom("C", new Point3d(0.0, 0.0, 0.0));
        IAtom c4 = new Atom("C", new Point3d(1.5, 0.0, 0.0));
        IAtom c5 = new Atom("C", new Point3d(3.0, 0.0, 0.0));
        IAtom c6 = new Atom("C", new Point3d(4.5, 0.0, 0.0));
        mol.addAtom(c3);
        mol.addAtom(c4);
        mol.addAtom(c5);
        mol.addAtom(c6);
        mol.addBond(new Bond(c3, c4));
        mol.addBond(new Bond(c4, c5));
        mol.addBond(new Bond(c5, c6));
        
        // Find all unique atom mappings
        List<Map<IAtom, IAtom>> mappings = MoleculeUtils.findUniqueAtomMappings(
                substructure, mol, logger);
        
        // Should find multiple mappings (at least 3, one for each C-C bond)
        assertNotNull(mappings, "Mappings list should not be null");
        assertTrue(mappings.size() > 1, 
                "Should find more than one mapping when substructure appears multiple times. " +
                "Found: " + mappings.size());
        
        // Verify each mapping contains 2 atoms (the C-C pair)
        for (Map<IAtom, IAtom> mapping : mappings)
        {
            assertEquals(2, mapping.size(), 
                    "Each mapping should contain exactly 2 atoms");
            assertTrue(mapping.containsKey(c1), 
                    "Each mapping should contain the first carbon from substructure");
            assertTrue(mapping.containsKey(c2), 
                    "Each mapping should contain the second carbon from substructure");
            
            // Verify the mapped atoms are carbons and are bonded
            IAtom mappedC1 = mapping.get(c1);
            IAtom mappedC2 = mapping.get(c2);
            assertNotNull(mappedC1, "Mapped atom should not be null");
            assertNotNull(mappedC2, "Mapped atom should not be null");
            assertEquals("C", mappedC1.getSymbol(), "Mapped atom should be carbon");
            assertEquals("C", mappedC2.getSymbol(), "Mapped atom should be carbon");
            
            IBond bond = mol.getBond(mappedC1, mappedC2);
            assertNotNull(bond, "Mapped atoms should be bonded in the target molecule");
        }
        
        // Verify that we have distinct mappings (different atom pairs)
        // Count unique pairs of mapped atoms
        Set<String> uniquePairs = new HashSet<>();
        for (Map<IAtom, IAtom> mapping : mappings)
        {
            IAtom mappedC1 = mapping.get(c1);
            IAtom mappedC2 = mapping.get(c2);
            // Create a unique identifier for the pair (sorted to handle both directions)
            String pairId = mappedC1.getIndex() < mappedC2.getIndex() 
                    ? mappedC1.getIndex() + "-" + mappedC2.getIndex()
                    : mappedC2.getIndex() + "-" + mappedC1.getIndex();
            uniquePairs.add(pairId);
        }
        
        assertTrue(uniquePairs.size() > 1, 
                "Should have multiple distinct atom pair mappings. " +
                "Found " + uniquePairs.size() + " unique pairs");
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testFindShortestPath() throws Exception
    {
        IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
        
        // Create a simple chain molecule: C-C-C-C-C
        IAtomContainer mol = builder.newAtomContainer();
        IAtom c1 = new Atom("C", new Point3d(0.0, 0.0, 0.0));
        IAtom c2 = new Atom("C", new Point3d(1.0, 0.0, 0.0));
        IAtom c3 = new Atom("C", new Point3d(2.0, 0.0, 0.0));
        IAtom c4 = new Atom("C", new Point3d(3.0, 0.0, 0.0));
        IAtom c5 = new Atom("C", new Point3d(4.0, 0.0, 0.0));
        mol.addAtom(c1);
        mol.addAtom(c2);
        mol.addAtom(c3);
        mol.addAtom(c4);
        mol.addAtom(c5);
        mol.addBond(new Bond(c1, c2));
        mol.addBond(new Bond(c2, c3));
        mol.addBond(new Bond(c3, c4));
        mol.addBond(new Bond(c4, c5));
        
        // Create vertex ID map (all same vertex ID for simple path)
        Map<IAtom, Long> atomToVertexId = new HashMap<>();
        atomToVertexId.put(c1, 1L);
        atomToVertexId.put(c2, 1L);
        atomToVertexId.put(c3, 1L);
        atomToVertexId.put(c4, 1L);
        atomToVertexId.put(c5, 1L);
        
        // Test path from c1 to c5 (should be c1-c2-c3-c4-c5)
        List<IAtom> path = MoleculeUtils.findShortestPath(mol, c1, c5, atomToVertexId);
        assertNotNull(path, "Path should not be null");
        assertEquals(5, path.size(), "Path should contain 5 atoms");
        assertEquals(c1, path.get(0), "First atom should be c1");
        assertEquals(c2, path.get(1), "Second atom should be c2");
        assertEquals(c3, path.get(2), "Third atom should be c3");
        assertEquals(c4, path.get(3), "Fourth atom should be c4");
        assertEquals(c5, path.get(4), "Fifth atom should be c5");
        
        // Test path from c2 to c4 (should be c2-c3-c4)
        List<IAtom> path2 = MoleculeUtils.findShortestPath(mol, c2, c4, atomToVertexId);
        assertNotNull(path2, "Path should not be null");
        assertEquals(3, path2.size(), "Path should contain 3 atoms");
        assertEquals(c2, path2.get(0), "First atom should be c2");
        assertEquals(c3, path2.get(1), "Second atom should be c3");
        assertEquals(c4, path2.get(2), "Third atom should be c4");
        
        // Test path from atom to itself (should return empty list)
        List<IAtom> path3 = MoleculeUtils.findShortestPath(mol, c3, c3, atomToVertexId);
        assertNotNull(path3, "Path should not be null");
        assertTrue(path3.isEmpty(), "Path from atom to itself should be empty");
        
        // Test with different vertex IDs (should still find path)
        Map<IAtom, Long> atomToVertexId2 = new HashMap<>();
        atomToVertexId2.put(c1, 1L);
        atomToVertexId2.put(c2, 1L);
        atomToVertexId2.put(c3, 2L); // Different vertex ID
        atomToVertexId2.put(c4, 2L);
        atomToVertexId2.put(c5, 2L);
        
        // Path from c1 to c5 should still work (boundary transition allowed)
        List<IAtom> path4 = MoleculeUtils.findShortestPath(mol, c1, c5, atomToVertexId2);
        assertNotNull(path4, "Path should not be null");
        assertEquals(5, path4.size(), "Path should contain 5 atoms even with different vertex IDs");
    }
    
//------------------------------------------------------------------------------

}
