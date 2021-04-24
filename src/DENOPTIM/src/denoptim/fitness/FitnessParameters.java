/*
 *   DENOPTIM
 *   Copyright (C) 2019 Marco Foscato <marco.foscato@uib.no>
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

package denoptim.fitness;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.jsp.el.ELException;
import javax.servlet.jsp.el.FunctionMapper;
import javax.servlet.jsp.el.VariableResolver;

import org.apache.commons.el.ExpressionEvaluatorImpl;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.fingerprint.Fingerprinter;
import org.openscience.cdk.fingerprint.IFingerprinter;
import org.openscience.cdk.fingerprint.IBitFingerprint;
import org.openscience.cdk.fingerprint.PubchemFingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.qsar.IDescriptor;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import denoptim.exception.DENOPTIMException;
import denoptim.fitness.descriptors.TanimotoMolSimilarity;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;


/**
 * Parameters defining the fitness providers.
 * 
 * @author Marco Foscato
 */

public class FitnessParameters
{
    /**
     * Flag indicating that at least one FS-parameter has been defined
     */
    private static boolean fitParamsInUse = false;
    
    /**
     * Flag indication we want to use external fitness provider
     */
    private static boolean useExternalFitness = true;

    /**
     * Pathname of an external fitness provider executable
     */
    private static String externalExe = "";

    /**
     * Interpreter for the external fitness provider
     */
    private static String interpreterExternalExe = "bash";

    /**
     * Formulation of the internally provided fitness
     */
    private static String fitnessExpression = "";
    
    
    /**
     * List of custom variable definitions read from input. 
     * These lines are the definition of atom/bond specific 
     * descriptors, and custom parametrised descriptors.
     */
	private static List<String> customVarDescExpressions = 
			new ArrayList<String>();
    
    /**
     * List of variables used in the calculation of the fitness. 
     * For instance, atom/bond specific descriptors, and customly parametrised 
     * descriptors.
     */
    private static List<Variable> variables = new ArrayList<Variable>();
    
    /**
     * The list of descriptors needed to calculate the fitness with internal 
     * fitness provider.
     */
    private static List<DescriptorForFitness> descriptors = 
            new ArrayList<DescriptorForFitness>();
    
    /**
     * Flag controlling production of png graphics for each candidate
     */
    private static boolean makePictures = false;
    
    /**
     * Flag requesting the generation of a 3d-tree model instead of a plain
     * collection of 3d building blocks. In the latter, the coordinated of each
     * fragment are not changed when building the molecular representation that
     * is sent to the fitness provider. Setting this to <code>true</code> asks
     * for roto-translation of each fragment, but does not perform any 
     * energy-driven refinement of the geometry.
     */
    private static boolean make3DTrees = true;
    
    /**
     * Utility for constructing CDK objects
     */
    private static IChemObjectBuilder cdkBuilder = 
            DefaultChemObjectBuilder.getInstance();
    
//------------------------------------------------------------------------------


    public static void resetParameters()
    {
    	fitParamsInUse = false;
    	useExternalFitness = true;
    	externalExe = "";
    	interpreterExternalExe = "bash";
    	fitnessExpression = "";
    	customVarDescExpressions = new ArrayList<String>();
    	descriptors = new ArrayList<DescriptorForFitness>();
    	variables = new ArrayList<Variable>();
    	makePictures = false;
    	make3DTrees = true;
    }
    
//------------------------------------------------------------------------------

    public static boolean fitParamsInUse()
    {
        return fitParamsInUse;
    }

//------------------------------------------------------------------------------

    /**
     * @return <code>true</code> if we are asked to execute an external fitness 
     * provider.
     */
    public static boolean useExternalFitness()
    {
        return useExternalFitness;
    }
    
//------------------------------------------------------------------------------

    /**
     * @return <code>true</code> if generation of the candidate's picture is 
     * required.
     */
    public static boolean makePictures()
    {
        return makePictures;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * @return <code>true</code> if we are asked to make a tree-like 3d 
     * molecular model prior fitness evaluation. The model is built by aligning
     * 3d-building blocks to the attachment point vectors, so there is no 
     * energy refinement.
     */
    public static boolean make3dTree()
    {
    	return make3DTrees;
    }

//------------------------------------------------------------------------------

    /**
     * Gets the pathname of the external executable file.
     * @return the pathname to the external fitness provider.
     */
    public static String getExternalFitnessProvider()
    {
        return externalExe;
    }
    
//------------------------------------------------------------------------------

    /**
     * Gets the interpreter used to run the external fitness provider.
     * @return the interpreter name.
     */
    public static String getExternalFitnessProviderInterpreter()
    {
        return interpreterExternalExe;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return the expression used to calculate the fitness with the internal
     * fitness provider
     */
    public static String getFitnessExpression()
    {
    	return fitnessExpression;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return list of descriptors needed to calculate the fitness
     */
    public static List<DescriptorForFitness> getDescriptors()
    {
    	return descriptors;
    }

//------------------------------------------------------------------------------
    
    public static void interpretKeyword(String line) throws DENOPTIMException
    {
        String key = line.trim();
        String value = "";
        if (line.contains("="))
        {
            key = line.substring(0,line.indexOf("=") + 1).trim();
            value = line.substring(line.indexOf("=") + 1).trim();
        }
        try
        {
            interpretKeyword(key,value);
        }
        catch (DENOPTIMException e)
        {
            throw new DENOPTIMException(e.getMessage()+" Check line "+line);
        }
    }

//------------------------------------------------------------------------------

    public static void interpretKeyword(String key, String value)
                                                      throws DENOPTIMException
    {
        String msg = "";
        switch (key.toUpperCase())
        {
        case "FP-SOURCE=":
        	externalExe = value;
        	fitParamsInUse = true;
            break;
            
        case "FP-INTERPRETER=":
        	interpreterExternalExe = value;
        	fitParamsInUse = true;
            break;
            
        case "FP-EQUATION=":
        	fitnessExpression = value;
        	fitParamsInUse = true;
        	useExternalFitness = false;
            break;
            
        case "FP-DESCRIPTORSPECS=":
        	customVarDescExpressions.add(value);
        	break;
            
        case "FP-MAKEPICTURES":
        	fitParamsInUse = true;
	        makePictures = true;
	        break;
	        
        case "FP-NO3DTREEMODEL":
        	make3DTrees = false;
        	break;

        default:
             msg = "Keyword " + key + " is not a known fitness-related "
             		+ "keyword. Check input files.";
            throw new DENOPTIMException(msg);
        }
    }
	
//------------------------------------------------------------------------------

    /**
     * This method parser the strings defining the fitness as a mathematical
     * expression using descriptors (i.e., values
     * that will eventually be calculated by IDescriptor implementations) 
     * and variables (i.e., values that are derived from descriptors according 
     * to customisable expressions that aims at cutomise the parameters used
     * to calculated the descriptor value, or make the descriptor atom/bond
     * specific). Since
     * the value obtained from the calculation of each single descriptor can be 
     * used for multiple purposes (i.e., as a proper descriptor and as a 
     * component used to calculate the value of a variable) we collect the
     * descriptors and record the relation between each descriptor and any 
     * variables that depend on it.
     * 
     * @throws DENOPTIMException
     * @throws CDKException 
     */
	private static void parseFitnessExpressionToDefineDescriptors() 
	        throws DENOPTIMException
	{
		// Parse expression of the fitness to get the names of all variables
		ExpressionEvaluatorImpl extractor = new ExpressionEvaluatorImpl();
		Set<String> variableNames = new HashSet<String>();
        VariableResolver collectAll = new VariableResolver() {
			@Override
			public Double resolveVariable(String varName) throws ELException {
				variableNames.add(varName);
				return 1.0;
			}
		};
		
		// Here we read the fitness expression to identify all components that 
		// are needed to calculate the fitness value. At this stage, we cannot
		// know whether a string refers to a variable of a descriptor name.
		try {
			//NB: this is not really an evaluation because the variableResolver
			// parse the data the get from the  
			// ExpressionEvaluator
			extractor.evaluate(fitnessExpression, Double.class, collectAll, 
					null);
		} catch (ELException e) {
			throw new DENOPTIMException("ERROR: unable to parse fitness "
					+ "expression.",e);
		}
		// Make all Variables (mostly empty of info, for now)
		for (String varName : variableNames)
		{
		    Variable v = new Variable(varName);
		    //WARNING: we first write the varName as descName, and then we 
		    // overwrite it with the function mapper.
		    v.setDescriptorName(varName);
		    variables.add(v);
		}
		
		// Dummy variable resolver. This is needed to fulfil the requirements of 
		// extractor, but it does nothing.
		//TODO try to use null
        VariableResolver dummyResolver = new VariableResolver() {
            @Override
            public Double resolveVariable(String varName) throws ELException {
                return 1.0;
            }
        };
        // This map allows ExpressionEvaluator to find the methods 
        // implemented here in FitnessParameter class and that are used to 
        // parse the expression string.
        // An example of such method is 'atomSpecific' or 'parametrized'
        FunctionMapper funcsMap = new FunctionMapper() {
            @Override
            public Method resolveFunction(String nameSpace, String methodName) {
                try {
                    //TODO make the methods part of a private inner class
                    return FitnessParameters.class.getMethod(methodName, 
                            String.class, String.class, String.class);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
		// Now, we read the specifics of variables, i.e., any custom 
		// parametrised and/or atom/bond specific descriptor.
		try {
			//NB: this is not really an evaluation because the variableResolver
			// and function map only parse the data they get from the 
			// ExpressionEvaluator.
			for (String variableDefinition : customVarDescExpressions)
			{
			    //NB: 'funcsMap' tells the extractor how to deal with the data
			    // fed into functions 'atomSpecific' and 'parametrized'
				extractor.evaluate(variableDefinition, Double.class, 
				        dummyResolver, funcsMap);
			}
		} catch (ELException e) {
			throw new DENOPTIMException("ERROR: unable to parse fitness "
					+ "expression.",e);
		}
		
		// Also the descriptors that are simply called by name are transformed 
		// into variables
		/*
		for (String s : descriptorsGeneratingVariables)
		{
		    variables.add(new Variable(s,s));
		}
	*/
        // Collect the descriptor implementations that are needed. Note that
		// one descriptor implementation can be used by more than one variable,
		// and the same descriptor implementation (though differently
		// configured) might be requested by different variables. The latter
		// situation is not effective here, because the configuration of the
		// implementations is defined later. Therefore, we get one 
		// descriptor implementation for all its potentially different
		// configurations.
		Set<String> descriptorImplementationNames = new HashSet<String>();
		for (Variable v : variables)
		{
		    //TODO del
		    /*
		    System.out.println("V: "+v.getName()+ " "+v.getDescriptorName());
		    System.out.println("   smarts: "+v.smarts);
		    System.out.println("   params: "+v.params);
		    */
		    descriptorImplementationNames.add(v.getDescriptorName());
		}
		List<DescriptorForFitness> rawDescriptors = 
		        DescriptorUtils.findAllDescriptorImplementations(
		                descriptorImplementationNames);
		
		// This map keeps track of which instances are standard, i.e., not
		// affected by custom parameters.
		Map<String,DescriptorForFitness> standardDescriptors = 
		        new HashMap<String,DescriptorForFitness>();
		
		// Now we create the list of descriptors, each with either standard or 
		// customised configuration, that will be fed to the fitness provider
		// descriptor engine. This list goes into the field 'descriptors'.
		for (int i=0; i<variables.size(); i++)
        {   
		    Variable v = variables.get(i);
		    String varName = v.getName();
		    String descName = v.getDescriptorName();

		    // 'raw' means that it has not yet been configured, so it can be 
		    // used to draft both standard and customised descriptors instances
            DescriptorForFitness rawDff = rawDescriptors.stream()
                    .filter(d -> descName.equals(d.getShortName()))
                    .findAny()
                    .orElse(null);
            if (rawDff==null)
                throw new DENOPTIMException("Exepected lack of a match in the "
                        + "list of descriptors. This should never happen.");
		    
		    if (v.params == null)
		    {   
		        // No custom parameter, so we take (or make) a standard
		        // implementation of the descriptor.
		        if (standardDescriptors.containsKey(rawDff.getShortName()))
		        {
		            standardDescriptors.get(rawDff.getShortName())
		                .addDependentVariable(v);
		        } else {
    		        DescriptorForFitness dff = rawDff.makeCopy();
    		        dff.addDependentVariable(v);
    		        descriptors.add(dff);
    		        standardDescriptors.put(rawDff.getShortName(),dff);
		        }
		    } else {
		        // This variable requires a customised descriptor configuration
		        // So, here a brand new descriptor is made and configured
                DescriptorForFitness dff = rawDff.makeCopy();
                dff.addDependentVariable(v);
                
                IDescriptor impl = dff.implementation;
                String[] parNames = impl.getParameterNames();
                if (parNames.length != v.params.length)
                {
                    throw new DENOPTIMException("Wrong number of parameters in "
                            + "configuration of descriptor '" 
                            + rawDff.getShortName() + "' for variable '" 
                            + varName + "'. Found " + v.params.length + " but "
                            + "the descriptor requires " + parNames.length+".");
                }
                
                // Here we configure the descriptor implementation according
                // to the parameters parsed from the field of the Variable 
                Object[] params = new Object[parNames.length];
                for (int j=0; j<parNames.length; j++)
                {
                    Object parType = impl.getParameterType(parNames[j]);
                    if (parType == null)
                    {
                        throw new DENOPTIMException("Descriptor '" 
                                + rawDff.getShortName() + "' does not specify "
                                + "the type of a parameter.");
                    }
                    Object p = null;
                    if (parType instanceof Integer){
                        p = Integer.parseInt(v.params[j]);
                    } else if (parType instanceof Double) {
                        p = Double.parseDouble(v.params[j]);
                    } else if (parType instanceof Boolean) {
                        p = Boolean.getBoolean(v.params[j]);
                    } else if (parType instanceof Class) {
                        String type = ((Class<?>) parType).getSimpleName();
                        switch (type)
                        {
                            case "IFingerprinter":
                                p = makeIFingerprinter(v.params[j]);
                                break;
                                
                            case "IBitFingerprint":
                                // WARNING! we expect this to be found always
                                // after the corresponding IFingerprinter
                                // parameter.
                                p = makeIBitFingerprint(v.params[j], 
                                        (IFingerprinter) params[j-1]);
                                break;
                                
                            default:
                                throw new DENOPTIMException("Parameter '" 
                                        + parNames[j] + "' for descriptor '"
                                        + descName + "' is requested to be of "
                                        + "type '" + type + "' but no "
                                        + "handling of such type is available "
                                        + "in FitnessParameters. Please, "
                                        + "report this to the develoment "
                                        + "team.");
                        }
                    } else {
                        throw new DENOPTIMException("Parameter '" 
                                + parNames[j] + "' for descriptor '"
                                + descName + "' in an instance of a class"
                                + "that is not expected by "
                                + "in FitnessParameters. Please, "
                                + "report this to the develoment team.");
                    }
                    params[j] = p;
                }
                try
                {
                    impl.setParameters(params);
                } catch (CDKException e)
                {
                    // This should never happen: type and number of the params
                    // is made to fit the request of this method.
                    throw new DENOPTIMException("Wrong set of parameters "
                            + "for descriptor '" + descName
                            + "in FitnessParameters. Please, "
                            + "report this to the develoment team.",e);
                }
                descriptors.add(dff);
		    }
        }
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * For now we only accept a filename from which we read in a molecule
	 * @throws DENOPTIMException 
	 */
    private static IBitFingerprint makeIBitFingerprint(String line, 
            IFingerprinter fingerprinter) throws DENOPTIMException
    {
        String key = "FILE:";
        line = line.trim();
        if (!line.toUpperCase().startsWith(key))
        {
            throw new DENOPTIMException("Presently, parameters of type "
                    + "'IBitFingerprint' can only be generated upon "
                    + "reading a molecule from file. To this end, the "
                    + "definition of the parameter *MUST* start with '"
                    + key + "'. Input line '" + line + "' does not.");
        }
        if (fingerprinter == null)
        {
            throw new IllegalArgumentException("ERROR! Fingerprinter should be "
                    + "created before attempting generation of a fingerprint.");
        }
        String fileName = line.substring(key.length()).trim();
        IBitFingerprint fp = null;
        try
        {
            IAtomContainer ref = DenoptimIO.readSingleSDFFile(fileName);
            fp = fingerprinter.getBitFingerprint(ref);
        } catch (CDKException e)
        {
            throw new DENOPTIMException("ERROR! Unable to read molecule from "
                    + "file '" + fileName + "'.",e);
        }
        return fp;
    }

//------------------------------------------------------------------------------
	
    private static IFingerprinter makeIFingerprinter(String classShortName) 
            throws DENOPTIMException
    {
        IFingerprinter fp = null;
        try
        {
            Class<?> cl = Class.forName("org.openscience.cdk.fingerprint." 
                    + classShortName);
            for (Constructor<?> constructor : cl.getConstructors()) 
            {
                Class<?>[] params = constructor.getParameterTypes();
                if (params.length == 0) 
                {
                    fp = (IFingerprinter) constructor.newInstance();
                } else if (params[0].equals(IChemObjectBuilder.class))
                {
                    //NB potential source of ambiguity on the builder class
                    fp = (IFingerprinter) constructor.newInstance(cdkBuilder);
                }
            }
        } catch (Throwable t)
        {
            throw new DENOPTIMException("Could not make new instance of '" 
                    + classShortName + "'.", t);
        }
        if (fp == null)
        {
            throw new DENOPTIMException("Could not make new instance of '" 
                    + classShortName + "'. No suitable constructor found.");
        }
        return fp;
    }

//------------------------------------------------------------------------------

	/**
	 * Function only used to parse the expressions 
	 * defining atom specific descriptors. Basically, this method takes the
	 * arguments and records hat variables are use each descriptor, and what
	 * SMARTS are used for each variable.
	 * This method is public only because
	 * is must be found by the ExpressionEvaluator, but should not be used
	 * outside the FitnessPArameters class.
	 */
	
	public static Double atomSpecific(String varName, String descName, 
			String smartsIdentifier)
	{
		ArrayList<String> smarts = new ArrayList<String>(Arrays.asList(
				smartsIdentifier.split("\\s+")));
		for (Variable v : variables)
		{
		    if (v.getName().equals(varName))
		    {
		        v.setDescriptorName(descName);
		        v.setSMARTS(smarts);
		    }
		}
		
		return 0.0; //just a dummy number to fulfil the executor expectations
	}
	
//------------------------------------------------------------------------------

    /**
     * Function only used to parse the expressions defining customised
     * parametrisations of descriptors.
     * This method is public only because
     * is must be found by the ExpressionEvaluator, but should not be used
     * outside the FitnessParameters class.
     */
    
    public static Double parametrized(String varName, String descName, 
            String paramsStr)
    {
        String[] params = paramsStr.split(", +");
        for (Variable v : variables)
        {
            if (v.getName().equals(varName))
            {
                v.setDescriptorName(descName);
                v.setDescriptorParameters(params);
            }
        }
        
        return 0.0; //just a dummy number to fulfil the executor expectations
    }
	
//------------------------------------------------------------------------------

    public static void checkParameters() throws DENOPTIMException
    {
        String msg = "";
        if (!fitParamsInUse)
        {
            return;
        }

        if ((externalExe.length() != 0) 
        	&& (!DenoptimIO.checkExists(externalExe)))
        {
            msg = "Cannot find the fitness provider: " + externalExe;
            throw new DENOPTIMException(msg);
        }

        if (interpreterExternalExe.length() != 0)
        {
        	switch (interpreterExternalExe.toUpperCase())
        	{
	        	case "BASH":
	        		break;
	        		
	        	case "PYTHON":
	        		break;
/*
//TODO: add 
	        	case "JAVA":
	        		break;
*/
	        	default:
	        		msg = "Interpreter '" + interpreterExternalExe 
	        										 + "' not available.";
	                throw new DENOPTIMException(msg);
        	}
        }
    }

//------------------------------------------------------------------------------

    public static void processParameters() throws DENOPTIMException
    {
    	if (!fitnessExpression.equals(""))
    	{
    	    // This will take 'fitnessExpression'
        	parseFitnessExpressionToDefineDescriptors();
    	}
    }

//------------------------------------------------------------------------------

    public static void printParameters()
    {
		if (!fitParamsInUse)
		{
		    return;
		}
        String eol = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder(1024);
        sb.append(" FitnessParameters ").append(eol);
        for (Field f : FitnessParameters.class.getDeclaredFields()) 
        {
        	try
            {
                sb.append(f.getName()).append(" = ").append(
                            f.get(FitnessParameters.class)).append(eol);
            }
            catch (Throwable t)
            {
            	t.printStackTrace();
                sb.append("ERROR! Unable to print FitnessParameters.");
                break;
            }
        }
        DENOPTIMLogger.appLogger.info(sb.toString());
        sb.setLength(0);
    }

//------------------------------------------------------------------------------

}
