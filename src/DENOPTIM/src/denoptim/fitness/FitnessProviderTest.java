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
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
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
    	List<String> classNames = new ArrayList<String>();
    	classNames.add("org.openscience.cdk.qsar.descriptors.molecular."
    			+ "ZagrebIndexDescriptor");
    	classNames.add("org.openscience.cdk.qsar.descriptors.molecular."
    			+ "AtomCountDescriptor");
		DescriptorEngine engine = new DescriptorEngine(classNames,null);
		List<IDescriptor> iDescs =  engine.instantiateDescriptors(classNames);
    	
    	List<DescriptorForFitness> descriptors = 
    	        new ArrayList<DescriptorForFitness>();
    	for (int i=0; i<iDescs.size(); i++)
    	{
    		IDescriptor iDesc = iDescs.get(i);
    		DescriptorForFitness dv = new DescriptorForFitness(
    		        iDesc.getDescriptorNames()[0],
    				classNames.get(i), iDesc, 0);
    		descriptors.add(dv);
    	}
    	    	
    	FitnessProvider fp = new FitnessProvider(descriptors,"no eq needed");
    	
    	assertEquals(2, fp.engine.getDescriptorInstances().size(),
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
    	
    	List<String> classNames = new ArrayList<String>();
    	classNames.add("org.openscience.cdk.qsar.descriptors.molecular."
    			+ "ZagrebIndexDescriptor");
    	classNames.add("org.openscience.cdk.qsar.descriptors.molecular."
    			+ "AtomCountDescriptor");
		DescriptorEngine engine = new DescriptorEngine(classNames,null);
		List<IDescriptor> iDescs =  engine.instantiateDescriptors(classNames);
    	
    	List<DescriptorForFitness> descriptors = 
    	        new ArrayList<DescriptorForFitness>();
    	for (int i=0; i<iDescs.size(); i++)
    	{
    		IDescriptor iDesc = iDescs.get(i);
    		DescriptorForFitness dv = new DescriptorForFitness(
    		        iDesc.getDescriptorNames()[0],
    				classNames.get(i), iDesc, 0);
    		descriptors.add(dv);
    	}
    	    	
    	String expression = "${" + descriptors.get(0).shortName 
        		+" + " + descriptors.get(1).shortName + "}";
    	
    	FitnessProvider fp = new FitnessProvider(descriptors,expression);
    	double fitness = fp.getFitness(mol);
        
        // The descriptors values must be already in the mol properties map
        Map<Object, Object> props = mol.getProperties();
        // 6 properties: title, descpSpec, descSpec, Zagreb val, nAtom val, fitness
        assertEquals(6,props.size(),"Number of properties in processed mol");
        List<Object> keys = new ArrayList<Object>();
        for (Object k : props.keySet()) 
        {
        	keys.add(k);
        }
        String snFirts = descriptors.get(0).getShortName();
        String snSecond = descriptors.get(1).getShortName();
        if (props.get(snFirts).toString().equals("10.0"))
        {
        	assertEquals("12.0",props.get(snSecond).toString(), 
        			"Unexpected descriptor value (A)");
        } else if (props.get(snSecond).toString().equals("10.0"))
        {
        	assertEquals("10.0",props.get(snFirts).toString(), 
        			"Unexpected descriptor value (B)");
        } else {
        	assertTrue(false, "Unexpected descriptor value (C)");
        }
        
        double trsh = 0.001;
        assertTrue(Math.abs(22.0 - fitness) < trsh, 
        		"Fitness value should be 22.0 but is " + fitness);
    }
 
//------------------------------------------------------------------------------

    @Test
    public void testGetFitnessWithParametrizedDescriptors() throws Exception
    {
        // Construct a descriptor implementation
        List<String> classNames = new ArrayList<String>();
        classNames.add("denoptim.fitness.descriptors.TanimotoMolSimilarity");
        DescriptorEngine engine = new DescriptorEngine(classNames,null);
        List<IDescriptor> iDescs =  engine.instantiateDescriptors(classNames);
        IDescriptor iDesc = new TanimotoMolSimilarity();
        
        //Customise parameters used to calculate descriptors
        SmilesParser sp = new SmilesParser(
                SilentChemObjectBuilder.getInstance());
        IAtomContainer ref = sp.parseSmiles("CNC(=O)c1cc(OC)ccc1");
        PubchemFingerprinter fpMaker = new PubchemFingerprinter(
            DefaultChemObjectBuilder.getInstance());
        IBitFingerprint fpRef = fpMaker.getBitFingerprint(ref);
        Object[] params = {fpRef, fpMaker};
        iDesc.setParameters(params);
        
        //Configure fitness provider
        DescriptorForFitness dff = new DescriptorForFitness(
                iDesc.getDescriptorNames()[0],
                iDesc.getClass().getName(), iDesc, 0);
        List<DescriptorForFitness> descriptors = 
                new ArrayList<DescriptorForFitness>();
        descriptors.add(dff);
        String expression = "${" + descriptors.get(0).shortName + "}";
        FitnessProvider fp = new FitnessProvider(descriptors,expression);
        
        //Construct a molecule to be evaluated by the fitness provider
        IAtomContainer mol =  sp.parseSmiles("COc1ccccc1");
        
        //Calculate fitness
        double fitness = fp.getFitness(mol);
        
        //Get the result and check it
        Object propObj = mol.getProperty("FITNESS");
        assertTrue(propObj!=null,"Fitness is not null.");
        double trsh = 0.001;
        assertTrue(Math.abs(0.6 - fitness) < trsh, 
                "Fitness value should be 0.6 but is " + fitness);
    }
 
//------------------------------------------------------------------------------

}
