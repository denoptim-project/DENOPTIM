package denoptim.fitness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.qsar.DescriptorEngine;
import org.openscience.cdk.qsar.DescriptorSpecification;
import org.openscience.cdk.qsar.DescriptorValue;
import org.openscience.cdk.qsar.IDescriptor;
import org.openscience.cdk.qsar.result.DoubleArrayResult;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.qsar.result.IDescriptorResult;
import org.openscience.cdk.qsar.result.IntegerArrayResult;
import org.openscience.cdk.qsar.result.IntegerResult;

import denoptim.exception.DENOPTIMException;

/**
 * DENOPTIM's (internal) fitness provider class calculates CDK descriptors for a 
 * given chemical thing, and combines the descriptors to calculate a single
 * numerical results (i.e., the fitness) according to an equation given in the
 * {@link FitnessParameters}.
 * 
 * @author Marco Foscato
 */

public class FitnessProvider 
{
	/**
	 * The engine that collects and calculates descriptors
	 */
	protected static DescriptorEngine engine;
	
//------------------------------------------------------------------------------

	/**
	 * Configures the internal fitness provider according to the given 
	 * parameters.
	 */
	
	//TODO use our Descriptor class
	public static void configureDescriptors(List<String> classNames)
	{
		engine = new DescriptorEngine(classNames);
		List<IDescriptor> descriptors =  engine.instantiateDescriptors(
				classNames);
        List<DescriptorSpecification> specs = engine.initializeSpecifications(
        		descriptors);
	    engine.setDescriptorInstances(descriptors);
	    engine.setDescriptorSpecifications(specs);
	}
	
//------------------------------------------------------------------------------

	/**
	 * Configures the internal fitness provider according to the given 
	 * parameters.
	 */
	
	public static void configureDescriptors(List<String> classNames, 
			List<DescriptorSpecification> specs)
	{
		engine = new DescriptorEngine(classNames);
		List<IDescriptor> iDescs =  engine.instantiateDescriptors(classNames);
	    engine.setDescriptorInstances(iDescs);
	    engine.setDescriptorSpecifications(specs);
	}
	
//------------------------------------------------------------------------------

	/**
	 * Calculated the fitness according to the current configuration. Before
	 * calling this method, make sure you have called either 
	 * {@link FitnessProvider#configureDefault()} or 
	 * {@link FitnessProvider#configureCustom()} with appropriate arguments.
	 * @param iac the chemical object to evaluate.
	 * @return the final value of the fitness.
	 * @throws Exception if an error occurs during calculation of the descriptor
	 * or any initial configuration was missing/wrong.
	 */
	
	public static double getFitness(IAtomContainer iac) throws Exception 
	{
		if (engine == null)
		{
			throw new DENOPTIMException("Internal fitness provider has not been"
					+ " configured.");
		}
	
		// Calculate all descriptors. The results are put in the properties of
		// the IAtomContainer (as DescriptorValue identified by 
		// DescriptorSpecification keys)
		engine.process(iac);
		
		// Collect numerical values needed to calculate the fitness
		Map<String,Double> valuesMap = new HashMap<String,Double>();
        for (int i=0; i<engine.getDescriptorInstances().size(); i++)
        {
        	IDescriptor desc = engine.getDescriptorInstances().get(i);
        	
        	String descName = "Desc-"+i; //TODO get from fitness equation
        	double val = Double.NaN;
        	
        	DescriptorSpecification descSpec = 
        			engine.getDescriptorSpecifications().get(i);
        	DescriptorValue value = (DescriptorValue) iac.getProperty(descSpec);
        	if (value == null)
        	{
        		throw new Exception("Null value from calcualation of descriptor"
        				+ " " + descName);
        	}
        	IDescriptorResult result = value.getValue();
        	if (result == null)
        	{
        		throw new Exception("Null result from calcualation of "
        				+ "descriptor " + descName);
        	}
        	
            if (result instanceof DoubleResult) 
            {
                val = ((DoubleResult) result).doubleValue();
            } else if (result instanceof IntegerResult) 
            {
                val = ((IntegerResult) result).intValue();
            } else if (result instanceof DoubleArrayResult) 
            {
            	DoubleArrayResult a = (DoubleArrayResult) result;
            	int id = 0; //TODO take from settings
            	if (id >= a.length())
            	{
            		throw new Exception("Value ID out of range for descriptor "
            				+ descName);
            	}
            	val = a.get(id);
            	
            	//We also keep track of the entire vector
            	List<String> list = new ArrayList<String>();
                for (int j=0; j<a.length(); j++) 
                {
                    list.add(String.valueOf(a.get(j)));
                }
                iac.setProperty(desc.getClass().getName(),list);
            } else if (result instanceof IntegerArrayResult) 
            {
            	IntegerArrayResult a = (IntegerArrayResult) result;
            	int id = 0; //TODO take from settings
            	if (id >= a.length())
            	{
            		throw new Exception("Value ID out of range for descriptor "
            				+ descName);
            	}
            	val = a.get(id);

            	//We also keep track of the entire vector
            	List<String> list = new ArrayList<String>();
                for (int j=0; j<a.length(); j++) 
                {
                    list.add(String.valueOf(a.get(j)));
                }
                iac.setProperty(desc.getClass().getName(),list);
            }
            
    		// We also transform the single values into human-readable results
            iac.getProperties().remove(descSpec);
            iac.setProperty(descName,val);
            valuesMap.put(descName,val);
        }
        
		
        
		//TODO: del
        for (Entry<String, Double> e : valuesMap.entrySet())
        {
        	System.out.println(" ---> "+e.getKey()+": "+e.getValue());
        }
        

		// Use given equation to calculate the overall fitness
		double fitness = 0.206;
		//TODO: implement this
		
		return fitness;
	}

}
