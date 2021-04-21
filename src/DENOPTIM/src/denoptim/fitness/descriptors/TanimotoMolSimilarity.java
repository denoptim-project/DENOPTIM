package denoptim.fitness.descriptors;

import java.util.HashMap;
import java.util.Map;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.qsar.AbstractMolecularDescriptor;
import org.openscience.cdk.qsar.DescriptorSpecification;
import org.openscience.cdk.qsar.DescriptorValue;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.result.DoubleResultType;
import org.openscience.cdk.qsar.result.IDescriptorResult;
import org.openscience.cdk.tools.ILoggingTool;
import org.openscience.cdk.tools.LoggingToolFactory;

import denoptim.fitness.IDenoptimDescriptor;


/**
 * Calculates the molecular similarity against a target compound that is defined
 * by a given parameter.
 */

public class TanimotoMolSimilarity extends AbstractMolecularDescriptor 
implements IMolecularDescriptor, IDenoptimDescriptor 
{
   //TODO private static ILoggingTool logger = LoggingToolFactory.createLoggingTool(TanimotoMolSimilarity.class);
    private IAtomContainer referenceMol;
    private static final String[] PARAMNAMES = new String[] {
            "referenceMolecule"};

    private static final String[] NAMES  = {"TanimotoSimilarity"}; 

//------------------------------------------------------------------------------
    
    /**
     * Constructor for a TanimotoMolSimilarity object
     */
    public TanimotoMolSimilarity() {}

//------------------------------------------------------------------------------
      
    /**
     * 
     */
    @Override
    public void initialise(IChemObjectBuilder builder)
    {
        // TODO Auto-generated method stub
        
    }

//------------------------------------------------------------------------------
      
    /**
     * Get the specification attribute of Tanimoto molecular similarity.
     * @return the specification of this descriptor.
     */
    @Override
    public DescriptorSpecification getSpecification()
    {
        return new DescriptorSpecification("Denoptim source code", 
                this.getClass().getName(), "DENOPTIM project");
    }

//------------------------------------------------------------------------------
    
    /**
     * Gets the parameterNames attribute of the TanimotoMolSimilarity object.
     * @return the parameterNames value
     */
    @Override
    public String[] getParameterNames() {
        return PARAMNAMES;
    }

//------------------------------------------------------------------------------
    
    /**
     * Get the type of the parameter specified by the name argument.
     * @return the type or null if the parameter name is not known.
     */
    @Override
    public Object getParameterType(String name)
    {
        if (name.equals(PARAMNAMES[0]))
        {
            return IAtomContainer.class;
        } else {
            throw new IllegalArgumentException("No parameter for name: "+name);
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Set the parameters attribute of TanimotoMolSimilarity object.
     * The descriptor takes one parameter, i.e., the molecule against witch 
     * we want to calculate the molecular similarity.
     * @param params the array of parameters
     */
    @Override
    public void setParameters(Object[] params) throws CDKException
    {
        if (params.length > 1)
        {
            throw new IllegalArgumentException("TanimotoMolSimilarity only "
                    + "expects one parameter");
        }
        if (!(params[0] instanceof IAtomContainer))
        {
            throw new IllegalArgumentException("Expected parameter of type " 
                    + getParameterType(PARAMNAMES[0]));
        }
        referenceMol = (IAtomContainer) params[0];
    }

//------------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public Object[] getParameters()
    {
        Object[] params = new Object[1];
        params[0] = referenceMol;
        return params;
    }

//------------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String[] getDescriptorNames()
    {
        return NAMES;
    }

//------------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public DescriptorValue calculate(IAtomContainer container)
    {
        // TODO Auto-generated method stub
        return null;
    }

//------------------------------------------------------------------------------
   
    /** {@inheritDoc} */
    @Override
    public IDescriptorResult getDescriptorResultType()
    {
        return new DoubleResultType();
    }

//------------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String getDictionaryTitle()
    {
        return "Tanimoto Molecular Similarity";
    }
    
//------------------------------------------------------------------------------
    
    /** {@inheritDoc} */
    @Override
    public String getDictionaryDefinition()
    {
        return "The Tanimoto Molecular Similarity is calculated between the "
                + "a molecule given as reference upon definition of the "
                + "descriptor (see parameter), and a molecule given as "
                + "argument when calculating the value of the descriptor.";
        //TODO: add which fingerprints are used
    }

//------------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String[] getDictionaryClass()
    {
        return new String[] {"molecular"};
    }

//------------------------------------------------------------------------------

}
