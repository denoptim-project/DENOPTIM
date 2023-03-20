/*
 *   DENOPTIM
 *   Copyright (C) 2022 Marco Foscato <marco.foscato@uib.no>
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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.IBitFingerprint;
import org.openscience.cdk.fingerprint.IFingerprinter;
import org.openscience.cdk.fingerprint.ShortestPathFingerprinter;
import org.openscience.cdk.fingerprint.SubstructureFingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.qsar.IDescriptor;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import denoptim.exception.DENOPTIMException;
import denoptim.fitness.descriptors.TanimotoMolSimilarity;
import denoptim.fitness.descriptors.TanimotoMolSimilarityBySubstructure;
import denoptim.io.DenoptimIO;
import jakarta.el.BeanELResolver;
import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.el.FunctionMapper;
import jakarta.el.MethodExpression;
import jakarta.el.ValueExpression;
import jakarta.el.VariableMapper;

/**
 * Class parsing fitness expression by means of Expression Language.
 */
public class FitnessExpressionParser
{  
    /**
     * List of variables used in the calculation of the fitness. 
     * For instance, atom/bond specific descriptors, and custom parameterized 
     * descriptors.
     */
    private List<Variable> variables = new ArrayList<Variable>();
    
    /**
     * The list of descriptors needed to calculate the variables that are
     * used to calculate the fitness with the internal fitness provider.
     */
    private List<DescriptorForFitness> descriptors = 
            new ArrayList<DescriptorForFitness>();
    
//------------------------------------------------------------------------------

    /**
     * Creates a new parser.
     */
    public FitnessExpressionParser() {}

//------------------------------------------------------------------------------

    /**
     * Parses the given expressions.
     * Since
     * the value obtained from the calculation of each single descriptor can be 
     * used for multiple purposes (i.e., as a proper descriptor and as a 
     * component used to calculate the value of more than one variable) 
     * we collect the
     * descriptors and record the relation between each descriptor and any 
     * variables that depend on it.
     * @param fitnessExpression the expression defining how to calculate the 
     * fitness from named variables.
     * @param customVarDescExpressions the collection of functions defining how
     * to calculate the numerical value of the variables. Any variable that is 
     * not defined in this list is assumes to correspond to the value that
     * is calculated by a descriptor implementation reachable in the classpath
     * and offering a descriptor value named after the variable name. E.g.,
     * if the fitness expression contains <code>Zagreb</code>, we assume that
     * the descriptor <code>Zagreb</code> can be calculated by a 
     * {@link IDescriptor} implementation visible in the class path.
     * @throws DENOPTIMException 
     */
    public void parse(String fitnessExpression, 
            List<String> customVarDescExpressions) throws DENOPTIMException
    {
        // First, parse the definitions of custom variables
        ExpressionFactory expFactory = ExpressionFactory.newInstance();
        CustomVariableDefiningContext cvdc = new CustomVariableDefiningContext();
        for (String variableDefinition : customVarDescExpressions)
        {
            // NB the replacement of the Windows separator is a workaround for
            // the fact that the ELParser sees that separator as a special
            // character that I could not manage to escape. The result of
            // such interpretation of \ (or '\\' as it is actually returned
            // from the System.getProperty("file.separator") method) makes
            // parsing of pathnames as expression components completely wrong.
            // Below we re-introduce Windows separators, if we need to.
            boolean useWindowsFileSeparator = variableDefinition.contains("\\");
            String modVarDef = variableDefinition;
            if (useWindowsFileSeparator)
                modVarDef = variableDefinition.replace("\\", "/");
            
            MethodExpression me = expFactory.createMethodExpression(cvdc, 
                    modVarDef, Variable.class, null);
            Variable v = (Variable) me.invoke(cvdc, null);
            
            if (useWindowsFileSeparator)
            {
                for (int ipar=0; ipar<v.params.length; ipar++)
                {
                    v.params[ipar] = v.params[ipar].replace("/", "\\");
                }
            }
            variables.add(v);
        }
        
        // Then, read the fitness expression to identify all variables in it.
        // That is, all those that have not been already defined as custom 
        // variables.
        VariableDefiningContext ncc = new VariableDefiningContext(variables);
        // NB: Double.class is needed to respect the contract (null cannot be used)
        expFactory.createValueExpression(ncc, fitnessExpression, Double.class);
        
        // Then, collects descriptors that are needed to map variable names into
        // values. Note that one descriptor implementation can be used by more 
        // than one variable,
        // and the same descriptor implementation (though differently
        // configured) might be requested by different variables. The latter
        // situation is not effective here, because the configuration of the
        // implementations is defined later. Therefore, we get one 
        // descriptor implementation for all its potentially different
        // configurations.
        Set<String> descriptorImplementationNames = new HashSet<String>();
        for (Variable v : variables)
        {
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
        // customized configuration, that will be fed to the fitness provider
        // descriptor engine. This list goes into the field 'descriptors'.
        for (int i=0; i<variables.size(); i++)
        {   
            Variable v = variables.get(i);
            String varName = v.getName();
            String descName = v.getDescriptorName();

            // 'raw' means that it has not yet been configured, so it can be 
            // used to draft both standard and customized descriptors instances
            DescriptorForFitness rawDff = rawDescriptors.stream()
                    .filter(d -> descName.equals(d.getShortName()))
                    .findAny()
                    .orElse(null);
            if (rawDff==null)
                throw new DENOPTIMException("Looking for descriptor '" 
                        + descName + "' that should be used to produce "
                        + "variable '" + varName
                        + "', but no match found in the list of descriptor "
                        + "implementations. "
                        + "Check the name of the descriptor you are trying to "
                        + "use.");
           
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
                // This variable requires a customized descriptor configuration
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
                    } else if (parType instanceof String) {
                        p = v.params[j];
                    } else if (parType instanceof Class) {
                        //TODO Change: this part lacks generality!
                        String type = ((Class<?>) parType).getSimpleName();
                        switch (type)
                        {       
                            case "IBitFingerprint":
                                if (impl instanceof TanimotoMolSimilarityBySubstructure)
                                {
                                    // WARNING! We expect this to be found always
                                    // after the parameter providing a pathname
                                    // to a file with substructure smarts.
                                    p = makeIBitFingerprintBySubstructure(
                                            v.params[j],((String[])params[j-1]));
                                } else if (impl instanceof TanimotoMolSimilarity) {
                                    // WARNING! we expect this to be found always
                                    // after the corresponding IFingerprinter
                                    // parameter.
                                    p = makeIBitFingerprint(v.params[j], 
                                            makeIFingerprinter(params[j-1].toString()));
                                } else {
                                    throw new DENOPTIMException("Descriptor '" 
                                            + rawDff.getShortName() + "' is "
                                            + "not expected to need an "
                                            + "IBitFingerprint parameter.");
                                }
                                break;
                                
                            case "String[]":
                                p = makeStringArray(v.params[j]);
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
     * In this context we only need to know that "Variable" is mapped to the
     * class {@link Variable}
     */
    
    private class CustomVariableDefiningContext extends ELContext
    {
        private FunctionMapper fm = null;

        /**
         * Mapper that always returns a new blank instance of {@link Variable}.
         */
        private VariableMapper vm = new VariableMapper() {
            @SuppressWarnings("serial")
            @Override
            public ValueExpression resolveVariable(String variable) {
                ValueExpression ve = new ValueExpression() {

                    @Override
                    public Object getValue(ELContext context)
                    {
                        Variable v = new Variable("BlankVariable");
                        return v;
                    }

                    @Override
                    public void setValue(ELContext context, Object value)
                    {
                        //NOPE!
                    }

                    @Override
                    public boolean isReadOnly(ELContext context)
                    {
                        return true;
                    }

                    @Override
                    public Class<?> getType(ELContext context)
                    {
                        return Variable.class;
                    }

                    @Override
                    public Class<?> getExpectedType()
                    {
                        return Variable.class;
                    }

                    @Override
                    public String getExpressionString()
                    {
                        return null;
                    }

                    @Override
                    public boolean equals(Object obj)
                    {
                        return false;
                    }

                    @Override
                    public int hashCode()
                    {
                        return 0;
                    }

                    @Override
                    public boolean isLiteralText()
                    {
                        return false;
                    }};
                return ve;
            }

            @Override
            public ValueExpression setVariable(String variable, 
                    ValueExpression expression) {
                        return null;
            }
        };
        
        /**
         * Resolver exploiting JavaBean contract.
         */
        private ELResolver resolver = new BeanELResolver();

        @Override
        public ELResolver getELResolver()
        {
            return resolver;
        }

        @Override
        public FunctionMapper getFunctionMapper()
        {
            return fm;
        }

        @Override
        public VariableMapper getVariableMapper()
        {
            return vm;
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * This is the context on which we read a fitness expression, and parse it
     * to identify all variables in the expression. The variables are collected
     * in a list
     */
    private class VariableDefiningContext extends ELContext
    {
        /**
         * Reference to where we collect all the created variables.
         */
        private List<Variable> variables;
        
        private FunctionMapper fm = null;

        /**
         * This mapper does not map variables, but it creates an instance of 
         * {@link Variable} named with the given string, and it adds the 
         * instance to the list of known variables, if no variable with the 
         * same name is already there.
         * Variable names are expected to correspond to the name of
         * descriptors
         */
        private VariableMapper vm = new VariableMapper() {
            /**
             * Does not resolve variables, but it creates a corresponding 
             * {@link Variable} instance if this does not exist already.
             */
            @Override
            public ValueExpression resolveVariable(String variableName) {
                for (Variable existing : variables)
                {
                    if (variableName.equals(existing.getName()))
                            return null;
                }
                Variable v = new Variable(variableName);
                v.setDescriptorName(variableName);
                variables.add(v);
                return null;
            }

            @Override
            public ValueExpression setVariable(String variable, 
                    ValueExpression expression) {
                        return null;
            }
        };
        
        /**
         * Resolver exploiting JavaBean contract.
         */
        private ELResolver resolver = new BeanELResolver();
        
        /**
         * Constructor specifying the destination of the variables created when
         * parsing the expression.
         * @param variables destination of the created variable instances.
         */
        public VariableDefiningContext(List<Variable> variables)
        {
            this.variables = variables;
        }

        @Override
        public ELResolver getELResolver()
        {
            return resolver;
        }

        @Override
        public FunctionMapper getFunctionMapper()
        {
            return fm;
        }

        @Override
        public VariableMapper getVariableMapper()
        {
            return vm;
        }
    }
    
//------------------------------------------------------------------------------
    
    private String[] makeStringArray(String line) throws DENOPTIMException
    {
        String key = "FILE:";
        line = line.trim();
        if (!line.toUpperCase().startsWith(key))
        {
            throw new DENOPTIMException("Presently, parameters of type "
                    + "'String[]' can only be generated upon "
                    + "reading a text lines from file. To this end, the "
                    + "definition of the parameter *MUST* start with '"
                    + key + "'. Input line '" + line + "' does not.");
        }
        
        String fileName = line.substring(key.length()).trim();
        ArrayList<String> lst = DenoptimIO.readList(fileName);
        String[] arr = new String[lst.size()];
        arr = lst.toArray(arr);
        return arr;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * For now we only accept a filename from which we read in a molecule
     * @throws DENOPTIMException 
     */
    private IBitFingerprint makeIBitFingerprintBySubstructure(String line, 
            String[] smarts) throws DENOPTIMException
    {
        IFingerprinter fingerprinter = new SubstructureFingerprinter(smarts);
        return makeIBitFingerprint(line, fingerprinter);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * For now we only accept a filename from which we read in a molecule
     * @throws DENOPTIMException 
     */
    private IBitFingerprint makeIBitFingerprint(String line, 
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
            IAtomContainer refMol = DenoptimIO.readAllAtomContainers(
                    new File(fileName)).get(0);
            
            if (fingerprinter instanceof ShortestPathFingerprinter)
            {
                try
                {
                    AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(
                            refMol);
                } catch (CDKException e1)
                {
                    throw new DENOPTIMException("Could not assign atom types "
                            + "to calculate fingerprint of reference molecule.",
                            e1);
                }
            }
            
            fp = fingerprinter.getBitFingerprint(refMol);
        } catch (Exception e)
        {
            throw new DENOPTIMException("ERROR! Unable to read molecule from "
                    + "file '" + fileName + "'.",e);
        }
        return fp;
    }

//------------------------------------------------------------------------------
    
    private IFingerprinter makeIFingerprinter(String classShortName) 
            throws DENOPTIMException
    {   
        IFingerprinter fp = null;
        try
        {
            fp = TanimotoMolSimilarity.makeIFingerprinter(classShortName);
        } catch (Throwable t)
        {
            throw new DENOPTIMException("Could not make new instance of '" 
                    + classShortName + "'.", t);
        }
        return fp;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the list of variables in the expression of the fitness.
     * @return the list of variables in the expression of the fitness.
     */
    public List<Variable> getVariables()
    {
        return variables;
    }

//------------------------------------------------------------------------------
    
    /**
     * Returns the list of descriptors needed to compute the numerical values of
     * the variables in the expression of the fitness.
     * @return the list of descriptors needed to calculate the numerical values of
     * the variables in the expression of the fitness.
     */
    public List<DescriptorForFitness> getDescriptors()
    {
        return descriptors;
    }

//------------------------------------------------------------------------------
    
}
