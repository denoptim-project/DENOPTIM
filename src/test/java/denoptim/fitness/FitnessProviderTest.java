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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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

import denoptim.constants.DENOPTIMConstants;
import denoptim.fitness.descriptors.TanimotoMolSimilarity;
import denoptim.io.DenoptimIO;

/**
 * Unit test for internal fitness provider.
 * 
 * @author Marco Foscato
 */

public class FitnessProviderTest
{
    private SmilesParser sp;
    private static final String SEP = System.getProperty("file.separator");
    
    @TempDir 
    static File tempDir;
    
    private Logger logger;
    
    @BeforeEach
    private void setUp()
    {
        assertTrue(tempDir.isDirectory(),"Should be a directory ");
        sp = new SmilesParser(SilentChemObjectBuilder.getInstance());
        logger = Logger.getLogger("DummyLogger");
        logger.setLevel(Level.SEVERE);
    }
    
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
    	
    	FitnessProvider fp = new FitnessProvider(descriptors, "no eq needed", 
    	        logger);
    	
    	assertEquals(2, fp.engine.getDescriptorInstances().size(),
    			"Number of descriptors from custom list");
    }
	
//------------------------------------------------------------------------------
	
    @Test
    public void testGetFitness() throws Exception
    {
    	IAtomContainer mol = null;
    	try {
    	     mol  = sp.parseSmiles("C(C)CO");
    	 } catch (InvalidSmilesException e) {
    	     // This cannot happen
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
    	String[] varNames = new String[] {"desc0", "desc1"};
    	for (int i=0; i<iDescs.size(); i++)
    	{
    		IDescriptor iDesc = iDescs.get(i);
    		DescriptorForFitness dff = new DescriptorForFitness(
    		        iDesc.getDescriptorNames()[0],
    				classNames.get(i), iDesc, 0);
    		dff.addDependentVariable(new Variable(varNames[i]));
    		descriptors.add(dff);
    	}
    	
    	String expression = "${" + varNames[0] + " + " + varNames[1] + "}";
    	
    	FitnessProvider fp = new FitnessProvider(descriptors, expression, 
    	        logger);
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
        if (props.get(varNames[0]).toString().equals("10.0"))
        {
        	assertEquals("12.0",props.get(varNames[1]).toString(), 
        			"Unexpected descriptor value (A)");
        } else if (props.get(varNames[1]).toString().equals("10.0"))
        {
        	assertEquals("10.0",props.get(varNames[0]).toString(), 
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
    public void testGetFitnessWithCustomDescriptors() throws Exception
    {
        // Construct a descriptor implementation
        List<String> classNames = new ArrayList<String>();
        classNames.add("denoptim.fitness.descriptors.TanimotoMolSimilarity");
        DescriptorEngine engine = new DescriptorEngine(classNames,null);
        engine.instantiateDescriptors(classNames);
        IDescriptor iDesc = new TanimotoMolSimilarity();
        
        //Customise parameters used to calculate descriptors
        IAtomContainer ref = sp.parseSmiles("CNC(=O)c1cc(OC)ccc1");
        PubchemFingerprinter fpMaker = new PubchemFingerprinter(
            DefaultChemObjectBuilder.getInstance());
        IBitFingerprint fpRef = fpMaker.getBitFingerprint(ref);
        Object[] params = {"PubchemFingerprinter", fpRef};
        iDesc.setParameters(params);
        
        String myVarName = "myVar";
        
        //Configure fitness provider
        DescriptorForFitness dff = new DescriptorForFitness(
                iDesc.getDescriptorNames()[0],
                iDesc.getClass().getName(), iDesc, 0);
        dff.addDependentVariable(new Variable (myVarName));
        List<DescriptorForFitness> descriptors = 
                new ArrayList<DescriptorForFitness>();
        descriptors.add(dff);
        String expression = "${" + myVarName + "}";
        FitnessProvider fp = new FitnessProvider(descriptors, expression, 
                logger);
        
        //Construct a molecule to be evaluated by the fitness provider
        IAtomContainer mol =  sp.parseSmiles("COc1ccccc1");
        
        //Calculate fitness
        double fitness = fp.getFitness(mol);
        
        //Get the result and check it
        Object propObj = mol.getProperty(DENOPTIMConstants.FITNESSTAG);
        assertTrue(propObj!=null,"Fitness is not null.");
        double trsh = 0.001;
        assertTrue(Math.abs(((double) propObj) - fitness) < trsh, 
                "Fitness value should be 0.6 but is " + fitness);
        assertTrue(Math.abs(0.6 - fitness) < trsh, 
                "Fitness value should be 0.6 but is " + fitness);
    }
        
//------------------------------------------------------------------------------

    /**
     * This test is reproducing most of what done in 
     * {@link FitnessParametersTest#testProcessExpressions()} so if both fail
     * the problem is most likely in {@link FitnessParameters}.
     */
    @Test
    public void testGetFitnessWithParametrizedDescriptors() throws Exception
    {
        String fileName = tempDir.getAbsolutePath() + SEP + "ref.sdf";
        IAtomContainer ref = sp.parseSmiles("CNC(=O)c1cc(OC)ccc1");
        DenoptimIO.writeSDFFile(fileName, ref, false);

        FitnessParameters fitPar = new FitnessParameters();
        
        String[] lines = new String[] {
                "FP-Equation=${taniSym + taniBis + 0.02 * Zagreb - aHyb_1 +"
                + " aHyb_2}",
                "FP-DescriptorSpecs=${Variable.atomSpecific('aHyb_1','aHyb','[$([C])]')}",
                "FP-DescriptorSpecs=${Variable.atomSpecific('aHyb_2','aHyb','[$([O])]')}",
                "FP-DescriptorSpecs=${Variable.parametrized('taniSym',"
                    + "'TanimotoSimilarity','PubchemFingerprinter, " 
                    + "FILE:" + fileName + "')}",
                "FP-DescriptorSpecs=${Variable.parametrized('taniBis',"
                    + "'TanimotoSimilarity','GraphOnlyFingerprinter, "
                    + "FILE:" + fileName + "')}"};
        for (int i=0; i<lines.length; i++)
        {
            String line = lines[i];
            fitPar.interpretKeyword(line);
        }
        fitPar.processParameters();
        
        FitnessProvider fp = new FitnessProvider(
                fitPar.getDescriptors(),
                fitPar.getFitnessExpression(),
                logger);
        
        IAtomContainer mol = sp.parseSmiles("COc1ccccc1");
        
        fp.getFitness(mol);
        
        String[] expectedProps = new String[] {DENOPTIMConstants.FITNESSTAG,"Zagreb","taniBis",
                "taniSym","aHyb_1","aHyb_2"};
        double[] expectedValue = new double[] {
                2.5689610, // fitness
                34.000000, // Zagreb
                0.4318000, // taniBis
                0.6000000, // taniSym
                2.1428571, // aHyb_1
                3.0000000 // aHyb_2
        };
        for (int i=0; i<expectedProps.length; i++)
        {
            Object p = mol.getProperty(expectedProps[i]);
            double value = Double.parseDouble(p.toString());
            assertTrue(closeEnough(expectedValue[i], value),
                    "Value of property '" + expectedProps[i] + "' should be "
                            + expectedValue[i] + " but is " + value);
        }
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testGetConstantFitness() throws Exception
    {
        FitnessParameters fitPar = new FitnessParameters();
        fitPar.interpretKeyword("FP-Equation=${1.23456}");
        fitPar.processParameters();
        
        FitnessProvider fp = new FitnessProvider(
                fitPar.getDescriptors(),
                fitPar.getFitnessExpression(),
                logger);
        
        IAtomContainer mol = sp.parseSmiles("COc1ccccc1");
        
        fp.getFitness(mol);
        
        Object prop = mol.getProperty(DENOPTIMConstants.FITNESSTAG);
        assertTrue(prop != null, "Fitness property found in molecule");
        assertTrue(closeEnough(1.23456, Double.parseDouble(prop.toString())),
                "Numerical result (" + Double.parseDouble(prop.toString()) 
                    + ") is correct");
    }
 
//------------------------------------------------------------------------------
    
    private boolean closeEnough(double expected, double actual)
    {
        double threshold = 0.0001;
        double delta = Math.abs(expected-actual);
        return delta < threshold;
    }
    
//------------------------------------------------------------------------------

}
