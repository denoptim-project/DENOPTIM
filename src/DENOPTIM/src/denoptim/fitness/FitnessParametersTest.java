package denoptim.fitness;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.fingerprint.IBitFingerprint;
import org.openscience.cdk.fingerprint.PubchemFingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.qsar.DescriptorEngine;
import org.openscience.cdk.qsar.IDescriptor;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import denoptim.fitness.descriptors.TanimotoMolSimilarity;
import denoptim.io.DenoptimIO;

/**
 * Unit test for fitness parameters' handler
 * 
 * @author Marco Foscato
 */

public class FitnessParametersTest
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
        DenoptimIO.writeMolecule(fileName, ref, false);
        
        FitnessParameters.resetParameters();
        String[] lines = new String[] {
                "FP-Equation=${taniSym + 3.3 * Zagreb - aHyb_1 + aHyb_2}",
                "FP-DescriptorSpecs=${atomSpecific('aHyb_1','aHyb','[$([C])]')}",
                "FP-DescriptorSpecs=${atomSpecific('aHyb_2','aHyb','[$([N])]')}",
                "FP-DescriptorSpecs=${parametrized('taniSym',"
                    + "'TanimotoSimilarity','PubchemFingerprinter, "
                    + "FILE:" + fileName + "')}",
                "FP-DescriptorSpecs=${parametrized('taniBis',"
                    + "'TanimotoSimilarity','Fingerprinter, "
                    + "FILE:" + fileName + "')}"};
        for (int i=0; i<lines.length; i++)
        {
            String line = lines[i];
            FitnessParameters.interpretKeyword(line);
        }
        FitnessParameters.processParameters();
        
        assertEquals(4,FitnessParameters.getDescriptors().size(),
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

        List<List<String>> expectedVarNames = new ArrayList<List<String>>();
        expectedVarNames.add(new ArrayList<>(Arrays.asList(
                "aHyb_1","aHyb_2")));
        expectedVarNames.add(new ArrayList<>(Arrays.asList("taniSym")));
        expectedVarNames.add(new ArrayList<>(Arrays.asList("taniBis")));
        expectedVarNames.add(new ArrayList<>(Arrays.asList("Zagreb")));
        List<DescriptorForFitness> descriptors = 
                FitnessParameters.getDescriptors();
        for (int i=0; i<descriptors.size(); i++)
        {
            DescriptorForFitness d = descriptors.get(i);
            String descName = d.getShortName();
            List<String> varNames = d.getVariableNames();
            assertEquals(expectedVarNames.get(i).size(), varNames.size(), 
                    "Number of variable names for #"+i+" "+descName);
            for (int j=0; j<varNames.size(); j++)
            {
                assertEquals(expectedVarNames.get(i).get(j), varNames.get(j),
                        "Variable name #"+j);
            }
        }
        
        // Cleanup static fields
        FitnessParameters.resetParameters();
    }
 
//------------------------------------------------------------------------------

}
