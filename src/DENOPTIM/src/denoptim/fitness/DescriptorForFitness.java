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
	
	/**
	 * The type of descriptor as define in the descriptor dictionary.
	 */
	protected String dictType;
	
	/**
	 * The class(es) of descriptor as define in the descriptor dictionary.
	 */
	protected String[] dictClasses;
	
	/**
	 * The Definition of descriptor as define in the descriptor dictionary.
	 */
	protected String dictDefinition;
	
	/**
	 * The title of descriptor as define in the descriptor dictionary.
	 */
	protected String dictTitle;
	
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

	public DescriptorForFitness(String shortName, String className, 
			IDescriptor implementation, int resultId, String dictType,
			String[] dictClasses, String dictDefinition, String dictTitle)
	{
		this(shortName, className, implementation, resultId);
		this.dictType = dictType;
		this.dictClasses = dictClasses;
		this.dictDefinition = dictDefinition;
		this.dictTitle = dictTitle;
	}
	
//------------------------------------------------------------------------------
	
	public String getShortName()
	{
		return shortName;
	}
	
//------------------------------------------------------------------------------

	public String getClassName()
	{
		return className;
	}
	
//------------------------------------------------------------------------------

	public IDescriptor getImplementation()
	{
		return implementation;
	}
	
//------------------------------------------------------------------------------

	public String[] getDictClasses()
	{
		return dictClasses;
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
	
	//TODO improve: now it is only meant to print on stdout
	public String getDictString() 
	{
		String NL = System.getProperty("line.separator");
		StringBuilder sb = new StringBuilder();
		sb.append("Titile: ").append(dictTitle).append(NL);
		sb.append("Type: ").append(dictType).append(NL);
		sb.append("Definition: ").append(dictDefinition).append(NL);
		sb.append("Classes: ");
		if (dictClasses != null)
		{
			for (String c : dictClasses)
			{
			    sb.append(c).append(" ");
			}
		}
		sb.append(NL);
		
		return sb.toString();
	}

//------------------------------------------------------------------------------
	
}
