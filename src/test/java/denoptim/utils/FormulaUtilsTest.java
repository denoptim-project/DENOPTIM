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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

/**
 * Unit test for tools manipulating molecular formulae.
 * 
 * @author Marco Foscato
 */

public class FormulaUtilsTest
{

    /**
     * Private builder of atom containers
     */
    private IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();

//------------------------------------------------------------------------------
    
    @Test
    public void testParseFormula() throws Exception
    {
        String formula = "H2 O";
        Map<String, Double> elCounts = FormulaUtils.parseFormula(formula);
        assertEquals("H 2.0 O 1.0",convertSimpleToString(elCounts));
        
        formula = "H2 O1";
        elCounts = FormulaUtils.parseFormula(formula);
        assertEquals("H 2.0 O 1.0",convertSimpleToString(elCounts));
        
        formula = "O H2";
        elCounts = FormulaUtils.parseFormula(formula);
        assertEquals("H 2.0 O 1.0",convertSimpleToString(elCounts));
        
        formula = "O1 H2";
        elCounts = FormulaUtils.parseFormula(formula);
        assertEquals("H 2.0 O 1.0",convertSimpleToString(elCounts));
        
        formula = "O H2 C3 W2 Ca";
        elCounts = FormulaUtils.parseFormula(formula);
        assertEquals("C 3.0 Ca 1.0 H 2.0 O 1.0 W 2.0",
                convertSimpleToString(elCounts));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testParsingOfCSDFormula() throws Exception
    {
        String formula = "H2 O";
        Map<String, ArrayList<Double>> elCounts = FormulaUtils.parseCSDFormula(formula);
        assertEquals("H 2.0 2.0 O 1.0 1.0",convertToString(elCounts));
        
        formula = "H2 O1";
        elCounts = FormulaUtils.parseCSDFormula(formula);
        assertEquals("H 2.0 2.0 O 1.0 1.0",convertToString(elCounts));
        
        formula = "O H2";
        elCounts = FormulaUtils.parseCSDFormula(formula);
        assertEquals("H 2.0 2.0 O 1.0 1.0",convertToString(elCounts));
        
        formula = "O1 H2";
        elCounts = FormulaUtils.parseCSDFormula(formula);
        assertEquals("H 2.0 2.0 O 1.0 1.0",convertToString(elCounts));
        
        formula = "O H2 C3 W2 Ca";
        elCounts = FormulaUtils.parseCSDFormula(formula);
        assertEquals("C 3.0 3.0 Ca 1.0 1.0 H 2.0 2.0 O 1.0 1.0 W 2.0 2.0",
                convertToString(elCounts));
        
        formula = "H2 O, C2 H6 O";
        elCounts = FormulaUtils.parseCSDFormula(formula);
        assertEquals("C 0.0 2.0 0.0 2.0 2.0 "
                + "H 2.0 6.0 2.0 6.0 8.0 "
                + "O 1.0 1.0 1.0 1.0 2.0",
                convertToString(elCounts));
        
        formula = "C2 H6 O 2-";
        elCounts = FormulaUtils.parseCSDFormula(formula);
        assertEquals("C 2.0 2.0 H 6.0 6.0 O 1.0 1.0",
                convertToString(elCounts));
        
        formula = "2(C16 H36 N1 1+),C8 Au1 N4 S4 2-,2(C3 H6 O1)";
        elCounts = FormulaUtils.parseCSDFormula(formula);
        assertEquals(
                   "Au 0.0 1.0 0.0 0.0 1.0 0.0 1.0 1.0 "
                 + "C 16.0 8.0 3.0 32.0 8.0 6.0 40.0 46.0 "
                 + "H 36.0 0.0 6.0 72.0 0.0 12.0 72.0 84.0 "
                 + "N 1.0 4.0 0.0 2.0 4.0 0.0 6.0 6.0 "
                 + "O 0.0 0.0 1.0 0.0 0.0 2.0 0.0 2.0 "
                 + "S 0.0 4.0 0.0 0.0 4.0 0.0 4.0 4.0",
                convertToString(elCounts));
        
        formula = "F5 Pd1,0.25(Br),0.5(Cl2)";
        elCounts = FormulaUtils.parseCSDFormula(formula);
        assertEquals(
                  "Br 0.0 1.0 0.0 0.0 1.0 0.0 1.0 1.0 "
                + "Cl 0.0 0.0 2.0 0.0 0.0 2.0 0.0 2.0 "
                +  "F 5.0 0.0 0.0 5.0 0.0 0.0 5.0 5.0 "
                + "Pd 1.0 0.0 0.0 1.0 0.0 0.0 1.0 1.0",
                convertToString(elCounts));
        
        formula = "(H2 O)n,6n(N),2n(C),2n(F2)";
        elCounts = FormulaUtils.parseCSDFormula(formula);
        assertEquals(
                  "C 0.0 0.0 1.0 0.0 0.0 0.0 2.0 0.0 0.0 0.0 0.0 0.0 0.0 2.0 4.0 6.0 8.0 10.0 2.0 4.0 6.0 8.0 10.0 "
                + "F 0.0 0.0 0.0 2.0 0.0 0.0 0.0 4.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 4.0 8.0 12.0 16.0 20.0 "
                + "H 2.0 0.0 0.0 0.0 2.0 0.0 0.0 0.0 2.0 4.0 6.0 8.0 10.0 2.0 4.0 6.0 8.0 10.0 2.0 4.0 6.0 8.0 10.0 "
                + "N 0.0 1.0 0.0 0.0 0.0 6.0 0.0 0.0 6.0 12.0 18.0 24.0 30.0 6.0 12.0 18.0 24.0 30.0 6.0 12.0 18.0 24.0 30.0 "
                + "O 1.0 0.0 0.0 0.0 1.0 0.0 0.0 0.0 1.0 2.0 3.0 4.0 5.0 1.0 2.0 3.0 4.0 5.0 1.0 2.0 3.0 4.0 5.0",
                convertToString(elCounts));
    }
    
//------------------------------------------------------------------------------
    
    private String convertSimpleToString(Map<String,Double> elCounts)
    {
        List<String> els = new ArrayList<String>();
        els.addAll(elCounts.keySet());
        Collections.sort(els);
        StringBuilder sb = new StringBuilder();
        for (String el : els)
        {
            sb.append(" ").append(el);
            sb.append(GenUtils.getEnglishFormattedDecimal(" #.#", 1, elCounts.get(el)));
        }
        return sb.toString().trim();
    }
    
//------------------------------------------------------------------------------
    
    private String convertToString(Map<String, ArrayList<Double>> elCounts)
    {
        List<String> els = new ArrayList<String>();
        els.addAll(elCounts.keySet());
        Collections.sort(els);
        StringBuilder sb = new StringBuilder();
        for (String el : els)
        {
            sb.append(" ").append(el);
            for (Double v : elCounts.get(el))
            {
                sb.append(GenUtils.getEnglishFormattedDecimal(" #.#", 1, v));
            }
        }
        return sb.toString().trim();
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testCompareFormulaAndElementalAnalysis() throws Exception
    {
        String formula = "H2 O";
        IAtomContainer mol = builder.newAtomContainer();
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("O"));
        assertTrue(FormulaUtils.compareFormulaAndElementalAnalysis(formula, mol));
        
        mol.addAtom(new Atom("O"));
        assertFalse(FormulaUtils.compareFormulaAndElementalAnalysis(formula, mol));
        
        formula = "3(H2 O)";
        mol = builder.newAtomContainer();
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("O"));
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("O"));
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("O"));
        assertTrue(FormulaUtils.compareFormulaAndElementalAnalysis(formula, mol));
        
        formula = "C6 H6,2(H2 O)"; // Only largest molecule present!
        mol = builder.newAtomContainer();
        mol.addAtom(new Atom("C"));
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("C"));
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("C"));
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("C"));
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("C"));
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("C"));
        mol.addAtom(new Atom("H"));
        assertTrue(FormulaUtils.compareFormulaAndElementalAnalysis(formula, mol));

        formula = "(C6 H6)n,2n(H2 O),n(C2 H4)"; // all mols
        mol = builder.newAtomContainer();
        mol.addAtom(new Atom("C"));
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("C"));
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("C"));
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("C"));
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("C"));
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("C"));
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("O"));
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("O"));
        mol.addAtom(new Atom("C"));
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("C"));
        mol.addAtom(new Atom("H"));
        mol.addAtom(new Atom("H"));
        assertTrue(FormulaUtils.compareFormulaAndElementalAnalysis(formula, mol));
        
    }
    
//------------------------------------------------------------------------------

}
