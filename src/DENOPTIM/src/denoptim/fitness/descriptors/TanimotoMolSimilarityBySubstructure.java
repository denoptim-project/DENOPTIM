package denoptim.fitness.descriptors;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.BitSetFingerprint;
import org.openscience.cdk.fingerprint.PubchemFingerprinter;
import org.openscience.cdk.fingerprint.SubstructureFingerprinter;
import org.openscience.cdk.fingerprint.Fingerprinter;
import org.openscience.cdk.fingerprint.IBitFingerprint;
import org.openscience.cdk.fingerprint.ICountFingerprint;
import org.openscience.cdk.fingerprint.IFingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.qsar.AbstractMolecularDescriptor;
import org.openscience.cdk.qsar.DescriptorSpecification;
import org.openscience.cdk.qsar.DescriptorValue;
import org.openscience.cdk.qsar.IDescriptor;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.qsar.result.DoubleResultType;
import org.openscience.cdk.qsar.result.IDescriptorResult;
import org.openscience.cdk.similarity.Tanimoto;

import denoptim.fitness.IDenoptimDescriptor;


/**
 * Calculates the molecular similarity against a target compound the
 * fingerprint of which is given as parameter, and using a given list of 
 * substructures.
 */

public class TanimotoMolSimilarityBySubstructure extends AbstractMolecularDescriptor 
implements IMolecularDescriptor, IDenoptimDescriptor
{
    //private static ILoggingTool logger = LoggingToolFactory.createLoggingTool(
    //        TanimotoMolSimilarity.class);
    private IBitFingerprint referenceFingerprint;
    private IFingerprinter fingerprinter;
    private String[] substructuressmarts;
    private static final String[] PARAMNAMES = new String[] {
            "substructuressmarts","referenceFingerprint"};

    private static final String[] NAMES  = {"TanimotoSimilarityBySubstructure"};
    
//------------------------------------------------------------------------------
    
    /**
     * Constructor for a TanimotoMolSimilarity object
     */
    public TanimotoMolSimilarityBySubstructure() {}

//------------------------------------------------------------------------------
      
    /**
     * Get the specification attribute of Tanimoto molecular similarity. Given 
     * the dependency on the substructures list and reference fingerprint, the
     * implementation identifier is made dependent on those two parameters. 
     * Consequently resetting the parameter with two new instances of the 
     * substructures and reference fingerprint 
     * (even is effectively equal) will
     * result in two different DescriptorSpecification objects.
     * @return the specification of this descriptor.
     */
    @Override
    public DescriptorSpecification getSpecification()
    {
        String paramID = ""; 
        if (fingerprinter!=null && referenceFingerprint!=null)
        {
            paramID = "" + substructuressmarts.hashCode() 
            + referenceFingerprint.hashCode();
        }
        return new DescriptorSpecification("Denoptim source code", 
                this.getClass().getName(), paramID, "DENOPTIM project");
    }

//------------------------------------------------------------------------------
    
    /**
     * Gets the parameterNames attribute of the 
     * TanimotoMolSimilarityBySubstructure object.
     * @return the parameterNames value
     */
    @Override
    public String[] getParameterNames() {
        return PARAMNAMES;
    }

//------------------------------------------------------------------------------
    
    /** {@inheritDoc} */
    @Override
    public Object getParameterType(String name)
    {
        if (name.equals(PARAMNAMES[1]))
        {
            return IBitFingerprint.class;
        } else if (name.equals(PARAMNAMES[0])) {
            return String[].class;
        } else {
            throw new IllegalArgumentException("No parameter for name: "+name);
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Set the parameters attribute of TanimotoMolSimilarityBySubstructure 
     * object.
     * The descriptor takes two parameters: the array of substructures defining
     * the fingerprint bits,
     * and the fingerprint against which we want to calculate similarity.
     * @param params the array of parameters
     */
    @Override
    public void setParameters(Object[] params) throws CDKException
    {
        if (params.length != 2)
        {
            throw new IllegalArgumentException(""
                    + "TanimotoMolSimilarityBySubstructure requires two "
                    + "parameters");
        }
        if (!(params[1] instanceof IBitFingerprint))
        {
            throw new IllegalArgumentException("Parameter does not implemet "
                    + "IBitFingerprint.");
        }
        if (!(params[0] instanceof String[]))
        {
            throw new IllegalArgumentException("Parameter is not String[] (" 
                    + params[0].getClass().getName() + ").");
        }

        substructuressmarts = ((String[])params[0]);
        fingerprinter = new SubstructureFingerprinter(substructuressmarts);
        referenceFingerprint = (IBitFingerprint) params[1];
    }
    
//------------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public Object[] getParameters()
    {
        Object[] params = new Object[2];
        params[1] = referenceFingerprint;
        params[0] = substructuressmarts;
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
    public DescriptorValue calculate(IAtomContainer mol)
    {
        DoubleResult result;
        if (referenceFingerprint==null)
        {
            throw new IllegalStateException("Reference fingerprint not set. "
                    + "Cannot calculate Tanimoto similarity.");
        }
        if (fingerprinter==null)
        {
            throw new IllegalStateException("Fingerprinter not set. "
                    + "Cannot calculate Tanimoto similarity.");
        }
        
        try
        {
            result = new DoubleResult(Tanimoto.calculate(referenceFingerprint, 
                    fingerprinter.getBitFingerprint(mol)));
        } catch (IllegalArgumentException e)
        {
            e.printStackTrace();
            result = new DoubleResult(Double.NaN);
        } catch (CDKException e)
        {
            e.printStackTrace();
            result = new DoubleResult(Double.NaN);
        }
        
        return new DescriptorValue(getSpecification(),
                getParameterNames(),
                getParameters(),
                result,
                getDescriptorNames());
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
        return "Tanimoto Molecular Similarity By Substructures";
    }
    
//------------------------------------------------------------------------------
    
    /** {@inheritDoc} */
    @Override
    public String getDictionaryDefinition()
    {
        return "The Tanimoto Molecular Similarity By Susbtructure "
                + "considers a given set of substructures to calculate "
                + "the Tanimoto similarity between the "
                + "reference fingerprint given upon definition of the "
                + "descriptor (see parameters), and the fingerprint of a "
                + "molecule given as "
                + "argument when calculating the value of the descriptor. "
                + "Fingerprints are obtained from a new instance of"
                + "<code>SubstructureFingerprinter</code> defined by the "
                + "list of SMARTS strings given "
                + "as parameter <code>" + PARAMNAMES[0] + "</code>.";
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
