package denoptim.fitness;

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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import denoptim.io.DenoptimIO;

/**
 * Unit test for parser of fitness-defining expressions.
 * 
 * @author Marco Foscato
 */

public class FitnessExpressionParserTest
{
	
    private SmilesParser sp;
    private static final String SEP = System.getProperty("file.separator");
    
    @TempDir 
    static File tempDir;
    
    @BeforeEach
    private void setUp()
    {
        assertTrue(tempDir.isDirectory(),"Should be a directory ");
        sp = new SmilesParser(SilentChemObjectBuilder.getInstance());
    } 
    
//------------------------------------------------------------------------------
	
    @Test
    public void testProcessExpressions() throws Exception
    {
        String fileName = tempDir.getAbsolutePath() + SEP + "ref.sdf";
        IAtomContainer ref = sp.parseSmiles("CNC(=O)c1cc(OC)ccc1");
        DenoptimIO.writeSDFFile(fileName, ref, false);
        
        FitnessParameters.resetParameters();
        String[] lines = new String[] {
                "FP-Equation=${taniSym + taniBis + 3.3 * Zagreb - aHyb_1 + "
                + "aHyb_2}",
                "FP-DescriptorSpecs=${Variable.atomSpecific('aHyb_1','aHyb','[$([C])]')}",
                "FP-DescriptorSpecs=${Variable.atomSpecific('aHyb_2','aHyb','[$([N])]')}",
                "FP-DescriptorSpecs=${Variable.parametrized('taniSym',"
                    + "'TanimotoSimilarity','PubchemFingerprinter, "
                    + "FILE:" + fileName + "')}",
                "FP-DescriptorSpecs=${Variable.parametrized('taniBis',"
                    + "'TanimotoSimilarity','Fingerprinter, "
                    + "FILE:" + fileName + "')}"};
        for (int i=0; i<lines.length; i++)
        {
            String line = lines[i];
            FitnessParameters.interpretKeyword(line);
        }
        
        FitnessParameters.processParameters();
        List<DescriptorForFitness> descriptors = 
                FitnessParameters.getDescriptors();
        
        assertEquals(4,descriptors.size(),
                "Number of descriptor implementation");
        
        Map<String,Integer> counts = new HashMap<String,Integer>();
        for (DescriptorForFitness d : FitnessParameters.getDescriptors())
        {
            String key = d.getShortName();
            if (counts.containsKey(key))
                counts.put(key,counts.get(key) + 1);
            else
                counts.put(key,1);
        }
        String[] expectedNames = new String[] {"aHyb",
                "TanimotoSimilarity","Zagreb"};
        int[] expectedCount = new int[] {1,2,1};
        for (int i=0; i<3; i++)
        {
            assertEquals(expectedCount[i], counts.get(expectedNames[i]),
                    "Number of " + expectedNames[i] + " implementations");
        }

        boolean foundA = false;
        boolean foundB = false;
        for (int i=0; i<descriptors.size(); i++)
        {
            DescriptorForFitness d = descriptors.get(i);
            String descName = d.getShortName();
            List<Variable> variables = d.getVariables();
            
            switch(descName)
            {
                case "aHyb":
                    assertEquals(2,variables.size(),
                            "Number of variable names for #"+i+" "+descName);
                    boolean foundOne = false;
                    boolean foundTwo = false;
                    for (Variable actual : variables)
                    {
                        if ("aHyb_1".equals(actual.getName()))
                            foundOne = true;
                        if ("aHyb_2".equals(actual.getName()))
                            foundTwo = true;
                    }
                    assertTrue(foundOne, "Found first variable aHyb");
                    assertTrue(foundTwo, "Found second variable aHyb");
                    break;
                    
                case "Zagreb":
                    assertEquals(1,variables.size(),
                            "Number of variable names for #"+i+" "+descName);
                    assertEquals("Zagreb",variables.get(0).getName(),
                            "Name of variable for #"+i);
                    break;
                    
                case "TanimotoSimilarity":
                    assertEquals(1,variables.size(),
                            "Number of variable names for #"+i+" "+descName);
                    if ("taniSym".equals(variables.get(0).getName()))
                        foundA = true;
                    if ("taniBis".equals(variables.get(0).getName()))
                        foundB = true;
                    break;
                
                default:
                    fail("Unexpected descriptor name "+descName);
            }
        }
        assertTrue(foundA, "Tanimoto-variable A");
        assertTrue(foundB, "Tanimoto-variable B");
        
        // Cleanup static fields
        FitnessParameters.resetParameters();
    }
 
//------------------------------------------------------------------------------

}
