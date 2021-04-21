package denoptim.fitness;

import org.openscience.cdk.qsar.DescriptorEngine;

/**
 * This interface forces descriptors that are not defined in the CDK ontology
 * to provide information that would otherwise be found in the ontology.
 */
public interface IDenoptimDescriptor
{
    
    /**
     * Gets the title of this descriptor as it should be in the dictionary
     * @return the title
     */
    public String getDictionaryTitle();
    
    /**
     * Get a string that describes the descriptor in detail. Might contain
     * mathematical formulation.
     * @see {@link  DescriptorEngine}
     * @return the description of this descriptor, possibly containing equations
     * that clarify how it is calculated.
     */
    public String getDictionaryDefinition();
    
    /**
     * Get the classification of this descriptor. A descriptor can belong to
     * one or more classes simultaneously.
     * @return
     */
    public String[] getDictionaryClass();
}
