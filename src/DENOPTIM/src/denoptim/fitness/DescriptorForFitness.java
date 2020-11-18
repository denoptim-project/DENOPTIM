package denoptim.fitness;

import org.openscience.cdk.qsar.DescriptorSpecification;
import org.openscience.cdk.qsar.IDescriptor;

/**
 * This is a reference to a specific descriptor value. Not the numerical result,
 * but the identity of the value.
 */

public class DescriptorForFitness 
{
	/**
	 * ClassName pointing to the implementation of this descriptor's calculator
	 */
	protected String className;
	
	/**
	 * Implementation of the descriptor's calculator
	 */
	protected IDescriptor implementation;

	/**
	 * Descriptor short name. Used to identify this descriptor in equations
	 */
	protected String shortName;
	
	/**
	 * Pointer to a specific results among those that are produced by the 
	 * calculation of this descriptor, or 0 for descriptors that produce a 
	 * single value.
	 */
	protected int resultId = 0;

//------------------------------------------------------------------------------

	public DescriptorForFitness(String shortName, String className, 
			IDescriptor implementation, int resultId)
	{
		this.shortName = shortName;
		this.className = className;
		this.implementation = implementation;
		this.resultId = resultId;
	}
	
//------------------------------------------------------------------------------
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("DescriptorForFitness [shortName:").append(shortName);
		sb.append(", className:").append(className);
		sb.append(", resultId:").append(resultId);
		DescriptorSpecification specs = implementation.getSpecification();
		sb.append(", specReference:").append(
				specs.getSpecificationReference());
		sb.append(", implTitle:").append(
				specs.getImplementationTitle());
		sb.append(", implId:").append(
				specs.getImplementationIdentifier());
		sb.append(", implVendor:").append(
				specs.getImplementationVendor()).append("]");
		return sb.toString();
	}

//------------------------------------------------------------------------------
	
}
