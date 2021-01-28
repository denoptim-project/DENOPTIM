package denoptim.fitness;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.servlet.jsp.el.ELException;
import javax.servlet.jsp.el.VariableResolver;

import org.apache.commons.el.ExpressionEvaluatorImpl;
import org.openscience.cdk.IImplementationSpecification;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.isomorphism.Mappings;
import org.openscience.cdk.qsar.DescriptorEngine;
import org.openscience.cdk.qsar.DescriptorValue;
import org.openscience.cdk.qsar.IAtomPairDescriptor;
import org.openscience.cdk.qsar.IAtomicDescriptor;
import org.openscience.cdk.qsar.IBondDescriptor;
import org.openscience.cdk.qsar.IDescriptor;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.result.DoubleArrayResult;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.qsar.result.IDescriptorResult;
import org.openscience.cdk.qsar.result.IntegerArrayResult;
import org.openscience.cdk.qsar.result.IntegerResult;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.logging.DENOPTIMLogger;
import denoptim.utils.DENOPTIMMathUtils;
import denoptim.utils.DummyAtomHandler;
import denoptim.utils.ManySMARTSQuery;

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
		this.expression = expression;
	
		// We cannot use the list<DescriptorsForFitness> parameter directly
		// otherwise we hit concurrent modification exception because multiple
		// threads will run the exact same instance of the implementation
		this.descriptors = new ArrayList<DescriptorForFitness>();

		// Take classnames from the parameter, and make new instances of DFF
		ArrayList<String> classnames = new ArrayList<String>();
		for (int i=0; i<descriptors.size(); i++)
		{
			DescriptorForFitness dff = descriptors.get(i);
			classnames.add(dff.getClassName());
			this.descriptors.add(dff.cloneAllButImpl());
		}
		
		// Now we instatiate new instances of the descriptors implementations
		engine = new DescriptorEngine(classnames,null);
		List<IDescriptor> newInstances = engine.getDescriptorInstances();
		for (int i=0; i<this.descriptors.size(); i++)
		{
			this.descriptors.get(i).implementation = newInstances.get(i);
		}
        List<IImplementationSpecification> newSpecs = 
        		engine.getDescriptorSpecifications();
		engine.setDescriptorInstances(newInstances);
	    engine.setDescriptorSpecifications(newSpecs);
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
		boolean debug = false; // for debug only!
		
		if (engine == null)
		{
			throw new DENOPTIMException("Internal fitness provider has not been"
					+ " configured.");
		}
		
		// Preparation of the chemical representation
		
		// TODO-V3 add possibility to submit external molecular modeling task
		
		// Cleanup: remove dummy atoms
		DummyAtomHandler dah = new DummyAtomHandler(
				DENOPTIMConstants.DUMMYATMSYMBOL);
		iac = dah.removeDummyInHapto(iac);
		iac = dah.removeDummy(iac);
		
		// Calculate all descriptors. 
		// The results are put in the properties of
		// the IAtomContainer (as DescriptorValue identified by 
		// DescriptorSpecification keys) and we later translate these into
		// plain human readable strings.
		engine.process(iac);
		
		if (debug) System.out.println("Descriptor instances: "+engine.getDescriptorInstances().size());
		
		// Collect numerical values needed to calculate the fitness
		HashMap<String,Double> valuesMap = new HashMap<String,Double>();
        for (int i=0; i<engine.getDescriptorInstances().size(); i++)
        {
        	DescriptorForFitness descriptor = descriptors.get(i);
        	IDescriptor desc = engine.getDescriptorInstances().get(i);
        	
        	String descName = descriptor.shortName;
        	if (debug) System.out.println("Working on descriptor '"+descName+"'");
        	
        	IImplementationSpecification descSpec = 
        			engine.getDescriptorSpecifications().get(i);
        	
        	// Identify specific atom and bonds
        	Map<String, String> smarts = new HashMap<String, String>();
        	for (String varName : descriptor.getVariableNames())
        	{
        		if (debug) System.out.println("Processing varName = '"+varName+"'");
        		if (descriptor.smarts.containsKey(varName))
        		{
        			if (descriptor.smarts.get(varName).size()!=1)
        			{
        				throw new DENOPTIMException("Handling of multiple "
        						+ "SMARTS identifiers is not implemented yet. "
        						+ "Please, let the DENOPTIM developers know "
        						+ "about your interest in this "
        						+ "functionality.");
        				/*
	        			int iSmarts=-1;
	        			for (String oneSmarts : descriptor.smarts.get(varName))
	        			{
	        				iSmarts++;
	        				smarts.put(varName+"@"+iSmarts, oneSmarts);
	        			}
	        			*/
        			}
        			smarts.put(varName, descriptor.smarts.get(varName).get(0));
        		}
        	}
        	
        	Map<String, Mappings> allMatches = new HashMap<String, Mappings>();
        	if (smarts.size() != 0)
        	{
	        	ManySMARTSQuery msq = new ManySMARTSQuery(iac, smarts);
	            if (msq.hasProblems())
	            {
	                String msg = "WARNING! Problems while searching for "
	                		+ "specific atoms/bonds using SMARTS: " 
	                		+ msq.getMessage();
	                throw new DENOPTIMException(msg,msq.getProblem());
	            }
	            allMatches = msq.getAllMatches();
        	}
        	
        	if (debug) System.out.println("Getting value from descriptor "+i+" "+descName);
        	
        	//Molecular/Atomic/bond descriptors are stored accordingly
        	DescriptorValue value = null;
        	if (desc instanceof IMolecularDescriptor)
        	{
        		// The value might be an array, but the DescriptorForFitness
        		// known which entry to take, and for each entry the descName
        		// is unique. Thus, no loop here.
        		value = (DescriptorValue) iac.getProperty(descSpec);
        		double val = processValue(descName, descriptor, desc, descSpec, 
        				value, descName, iac);
        		valuesMap.put(descName, val);
                iac.setProperty(descName,val);
                iac.getProperties().remove(descSpec);
        	} else if (desc instanceof IAtomicDescriptor) {
        		for (String varName : descriptor.getVariableNames())
        		{
        			Mappings hits = allMatches.get(varName);
        			if (hits==null)
        			{
        				String msg = "No hits for SMARTS of " + varName + ": "
        						+ "setting variable value to 0.0";
        				DENOPTIMLogger.appLogger.log(Level.WARNING ,msg);
        				valuesMap.put(varName, 0.0);
        				continue;
        			}
        			if (debug) System.out.println("AtomIDs contributing to "+varName+":"+hits);
        			if (hits.count() > 1)
        			{
        				String msg = "Multiple hits with SMARTS identifier for "
        						+ varName + ". Taking average of all values.";
        				DENOPTIMLogger.appLogger.log(Level.WARNING ,msg);
        			}
        			int valCounter = -1;
        			List<Double> vals = new ArrayList<Double>();
                    for (int[] singleMatch : hits)
                    {
                    	if (singleMatch.length != 1)
                    	{
                    		String msg = "Multiple entries in a single hit "
                    				+ "with SMARTS identifier for "
            						+ varName + ". Taking average of values.";
            				DENOPTIMLogger.appLogger.log(Level.WARNING ,msg);
                    	}
                    	for (Integer atmId : singleMatch)
                    	{
		        			IAtom atm = iac.getAtom(atmId);
		        			value = (DescriptorValue) atm.getProperty(descSpec);
		        			double val = processValue(descName, descriptor, 
		        					 desc, descSpec, value, varName, iac);
		        			vals.add(val);
		        			valCounter++;
		                    iac.setProperty(varName+"_"+valCounter,val);
                    	}
                    }
                    if (debug) System.out.println("Values contributing to "+varName+": "+vals);
                    double overallValue = DENOPTIMMathUtils.mean(vals);
                    valuesMap.put(varName, overallValue);
                    iac.setProperty(varName,overallValue);
        		}
        	} else if (desc instanceof IBondDescriptor) {
        		for (String varName : descriptor.getVariableNames())
        		{
        			Mappings hits = allMatches.get(varName);
        			if (hits==null)
        			{
        				String msg = "No hits for SMARTS of " + varName + ": "
        						+ "setting variable value to 0.0";
        				DENOPTIMLogger.appLogger.log(Level.WARNING ,msg);
        				valuesMap.put(varName, 0.0);
        				continue;
        			}
        			if (debug) System.out.println("AtomIDs contributing to "+varName+":"+hits);
        			if (hits.count() > 1)
        			{
        				String msg = "Multiple hits with SMARTS identifier for "
        						+ varName + ". Taking average of all values.";
        				DENOPTIMLogger.appLogger.log(Level.WARNING ,msg);
        			}
        			int valCounter = -1;
        			List<Double> vals = new ArrayList<Double>();
                    for (int[] singleMatch : hits)
                    {
                    	if (singleMatch.length != 2)
                    	{
                    		String msg = "Number of entries is != 2 for a "
                    				+ "single hit with SMARTS identifier for "
            						+ varName + ". I do not know how to deal "
            						+ "with this.";
                    		throw new DENOPTIMException(msg);
                    	}
                    	IBond bnd = iac.getBond(iac.getAtom(singleMatch[0]),
                    			iac.getAtom(singleMatch[1]));
                    	value = (DescriptorValue) bnd.getProperty(descSpec);
		        		double val = processValue(descName, descriptor, 
		        					 desc, descSpec, value, varName, iac);
		        		vals.add(val);
		        		valCounter++;
	                    iac.setProperty(varName+"_"+valCounter,val);
                    }
                    if (debug) System.out.println("Values contributing to "+varName+": "+vals);
                    double overallValue = DENOPTIMMathUtils.mean(vals);
                    valuesMap.put(varName, overallValue);
                    iac.setProperty(varName,overallValue);
        		}
        	} else if (desc instanceof IAtomPairDescriptor) {
        		throw new Exception("AtomPair-kind of descriptors are not yet "
        				+ " usable. Upgrade the code. ");
        		//TODO: implement this part...
        	} else {
        		throw new Exception("Type of descriptor "+ descName + " is "
        				+ "unknown. Cannot understand if it should be threated "
        				+ "as molecular, atomic, or bond descriptor.");
        	}
        }
        
        if (debug) System.out.println("VARIABLES: "+valuesMap);
        
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

	/**
	 * Takes the value and checks that it is all good, then processes the value 
	 * to extract the result defined by the DescriptorForFitness, puts a 
	 * human-readable version in the molecular representation, and the numerical
	 * value for fitness calculation in the appropriate map.
	 * @throws Exception 
	 */
	private double processValue(String descName, DescriptorForFitness descriptor,
			IDescriptor implementation, 
			IImplementationSpecification descSpec, DescriptorValue value,
			String varName, IAtomContainer iac) throws Exception 
	{
       	if (value == null)
    	{
    		throw new Exception("Null value from calcualation of descriptor"
    				+ " " + descName + " (for variable '" + varName + "'");
    	}
    	IDescriptorResult result = value.getValue();
    	if (result == null)
    	{
    		throw new Exception("Null result from calcualation of "
    				+ "descriptor " + descName + "(for variable '" 
    				+ varName + "'");
    	}
    	
    	double valueToFitness = Double.NaN;
    	
        if (result instanceof DoubleResult) 
        {
            valueToFitness = ((DoubleResult) result).doubleValue();
        } else if (result instanceof IntegerResult) 
        {
            valueToFitness = ((IntegerResult)result).intValue();
        } else if (result instanceof DoubleArrayResult) 
        {
        	DoubleArrayResult a = (DoubleArrayResult) result;
        	int id = descriptor.resultId;
        	if (id >= a.length())
        	{
        		throw new Exception("Value ID out of range for descriptor "
        				+ descName);
        	}
        	valueToFitness = a.get(id);
        	
        	//We also keep track of the entire vector
        	List<String> list = new ArrayList<String>();
            for (int j=0; j<a.length(); j++) 
            {
                list.add(String.valueOf(a.get(j)));
            }
            iac.setProperty(implementation.getClass().getName(),list);
        } else if (result instanceof IntegerArrayResult) 
        {
        	IntegerArrayResult array = (IntegerArrayResult) result;
        	int id = descriptor.resultId;
        	if (id >= array.length())
        	{
        		throw new Exception("Value ID out of range for descriptor "
        				+ descName);
        	}
        	valueToFitness = array.get(id);

        	//We also keep track of the entire vector
        	List<String> list = new ArrayList<String>();
            for (int j=0; j<array.length(); j++) 
            {
                list.add(String.valueOf(array.get(j)));
            }
            iac.setProperty(implementation.getClass().getName(),list);
        }
       
        return valueToFitness;
	}

//------------------------------------------------------------------------------
	
}
