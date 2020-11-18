package denoptim.fitness;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.jsp.el.ELException;
import javax.servlet.jsp.el.VariableResolver;

import org.apache.commons.el.ExpressionEvaluatorImpl;

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

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;

/**
 * DENOPTIM's (internal) fitness provider class calculates CDK descriptors for a 
 * given chemical thing, and combines the descriptors to calculate a single
 * numerical results (i.e., the fitness) according to an equation.
 * 
 * @author Marco Foscato
 */

public class FitnessProvider 
{
	/**
	 * The engine that collects and calculates descriptors
	 */
	protected DescriptorEngine engine;
	
	/**
	 * The collection of descriptors to consider
	 */
	private List<DescriptorForFitness> descriptors;
	
	/**
	 * The equation used to calculate the fitness value
	 */
	private String expression;
	
	
//------------------------------------------------------------------------------

	/**
	 * Constructs an instance that will calculated the fitness according to
	 * the given parameters.
	 */
	
	public FitnessProvider(List<DescriptorForFitness> descriptors, String expression)
	{
		this.descriptors = descriptors;
		this.expression = expression;
		
		// We use an empty list here because the instances of the descriptors 
		// are taken from the parameters, rather than built by the constructor
		// of the engine. So we only need to instantiate the engine.
		engine = new DescriptorEngine(new ArrayList<String>());
		
		List<IDescriptor> iDescs = new ArrayList<IDescriptor>();
        List<DescriptorSpecification> specs = 
        		new ArrayList<DescriptorSpecification>();
		for (DescriptorForFitness d : descriptors)
		{
			IDescriptor impl = d.implementation;
			iDescs.add(impl);
			specs.add(impl.getSpecification());
		}
		
		/* 
		// In alternative, we could give only the classNames and let the engine 
		// instantiate the descriptor instances.
		List<String> classNames = new ArrayList<String>();
		for (Descriptor d : descriptors)
		{
			classNames.add(d.className);
		}
		
		engine = new DescriptorEngine(classNames);
		
		List<IDescriptor> iDescs =  engine.instantiateDescriptors(classNames);
        List<DescriptorSpecification> specs = 
        		engine.initializeSpecifications(iDescs);
		for (int i=0; i<descriptors.size(); i++)
		{
			Descriptor d = descriptors.get(i);
			if (d.specs!=null)
			{
				specs.set(i, d.specs);
			}
		}
		*/
        
	    engine.setDescriptorInstances(iDescs);
	    engine.setDescriptorSpecifications(specs);
	}
	
//------------------------------------------------------------------------------

	/**
	 * Calculated the fitness according to the current configuration. The values
	 * of the descriptors, as well as the fitness value, are added to the
	 * properties of the atom container.
	 * @param iac the chemical object to evaluate.
	 * @return the final value of the fitness.
	 * @throws Exception if an error occurs during calculation of the descriptor
	 * or any initial configuration was missing/wrong.
	 */
	
	public double getFitness(IAtomContainer iac) throws Exception 
	{
		if (engine == null)
		{
			throw new DENOPTIMException("Internal fitness provider has not been"
					+ " configured.");
		}
	
		// Calculate all descriptors. The results are put in the properties of
		// the IAtomContainer (as DescriptorValue identified by 
		// DescriptorSpecification keys) and we later translate these into
		// plain human readable strings.
		engine.process(iac);
		
		// Collect numerical values needed to calculate the fitness
		HashMap<String,Double> valuesMap = new HashMap<String,Double>();
        for (int i=0; i<engine.getDescriptorInstances().size(); i++)
        {
        	DescriptorForFitness descriptor = descriptors.get(i);
        	IDescriptor desc = engine.getDescriptorInstances().get(i);
        	
        	String descName = descriptor.shortName;
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
            	int id = descriptor.resultId;
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
            	int id = descriptor.resultId;
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
        
        // Calculate the fitness from the expression and descriptor values
		ExpressionEvaluatorImpl evaluator = new ExpressionEvaluatorImpl();
		VariableResolver resolver = new VariableResolver() {
			@Override
			public Double resolveVariable(String varName) throws ELException {
				Double value = null;
				if (!valuesMap.containsKey(varName))
				{
					throw new ELException("Variable '" + varName 
							+ "' cannot be resolved");
				} else {
					value = valuesMap.get(varName);
				}
				return value;
			}
		};
		
		double fitness = (double) evaluator.evaluate(expression, Double.class, 
				resolver, null);
		iac.setProperty(DENOPTIMConstants.FITNESSTAG,fitness);
		return fitness;
	}

//------------------------------------------------------------------------------
	
}
