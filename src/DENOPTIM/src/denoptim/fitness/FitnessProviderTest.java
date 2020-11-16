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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

/**
 * Unit test for internal fitness provider.
 * 
 * @author Marco Foscato
 */

public class FitnessProviderTest
{
	
//------------------------------------------------------------------------------
	
    @Test
    public void testConfigureDescriptorsList() throws Exception
    {
    	List<String> descriptorClassNames = new ArrayList<String>(Arrays.asList(
		"org.openscience.cdk.qsar.descriptors.molecular.ZagrebIndexDescriptor",
		"org.openscience.cdk.qsar.descriptors.molecular.AtomCountDescriptor"));
    	    	
    	FitnessProvider.configureDescriptors(descriptorClassNames);
    	
    	assertEquals(2, FitnessProvider.engine.getDescriptorInstances().size(),
    			"Number of descriptors from custom list");
    }
	
//------------------------------------------------------------------------------
	
    @Test
    public void testGetFitness() throws Exception
    {
    	IAtomContainer mol = null;
    	try {
    	     SmilesParser sp = new SmilesParser(
    	    		 SilentChemObjectBuilder.getInstance());
    	     mol  = sp.parseSmiles("C(C)CO");
    	 } catch (InvalidSmilesException e) {
    	     System.err.println(e.getMessage());
    	 }
    	
    	List<String> descriptorsClassNames = new ArrayList<String>(
    			Arrays.asList(
		"org.openscience.cdk.qsar.descriptors.molecular.ZagrebIndexDescriptor",
		"org.openscience.cdk.qsar.descriptors.molecular.AtomCountDescriptor"));
    	
    	FitnessProvider.configureDescriptors(descriptorsClassNames);
    	
        double fitness = FitnessProvider.getFitness(mol);
        
        // The descriptors values must be already in the mol properties map
        Map<Object, Object> props = mol.getProperties();
        assertEquals(2,props.size(),"Number of properties in processed mol");
        List<Object> keys = new ArrayList<Object>();
        for (Object k : props.keySet()) 
        {
        	keys.add(k);
        }
        if (props.get(keys.get(0)).toString().equals("10.0"))
        {
        	assertEquals("12.0",props.get(keys.get(1)).toString(), 
        			"Unexpected descriptor value (A)");
        } else if (props.get(keys.get(1)).toString().equals("10.0"))
        {
        	assertEquals("10.0",props.get(keys.get(0)).toString(), 
        			"Unexpected descriptor value (B)");
        } else {
        	assertTrue(false, "Unexpected descriptor value (C)");
        }
        
        double trsh = 0.001;
        assertTrue(Math.abs(22.0 - fitness) < trsh, 
        		"Fitness value should be 22.0 but is " + fitness);
    }
 
//------------------------------------------------------------------------------

}
