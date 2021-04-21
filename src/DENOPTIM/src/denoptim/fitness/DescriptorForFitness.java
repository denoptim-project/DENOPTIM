package denoptim.fitness;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openscience.cdk.IImplementationSpecification;
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
	 * Descriptor short name.
	 */
	protected String shortName;
	
	/**
	 * Variable name. Used to identify variables calculated from this 
	 * descriptor in equations. The list must contain only the shortName 
	 * unless we have atom/bond specific descriptor
	 */
	protected List<String> varNames = new ArrayList<String>();
	
	/**
	 * Pointer to a specific results among those that are produced by the 
	 * calculation of this descriptor, or 0 for descriptors that produce a 
	 * single value.
	 */
	protected int resultId = 0;
	
	/**
	 * SMARTS used to identify the atom/bonds in case of atom/bond specific
	 * variable names. The keys are variable names, the values are lists of
	 * smarts as strings.
	 */
	protected Map<String,ArrayList<String>> smarts = 
			new HashMap<String,ArrayList<String>>();
	
	/**
	 * The type of descriptor as define in the descriptor dictionary.
	 */
	protected String dictType = null;
	
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
		this.varNames.add(shortName);
		this.className = className;
		this.implementation = implementation;
		this.resultId = resultId;
	}
	
//------------------------------------------------------------------------------

	private DescriptorForFitness(String shortName, String className, 
			int resultId, List<String> varNames, 
			Map<String,ArrayList<String>> smarts, String dictType,
			String[] dictClasses, String dictDefinition, String dictTitle)
	{
		this.shortName = shortName;
		this.varNames = varNames;
		this.className = className;
		//this.implementation does have to stay null
		this.resultId = resultId;
		this.smarts = smarts;
		//this.dictType = dictType;
		this.dictClasses = dictClasses;
		this.dictTitle = dictTitle;
	}
//------------------------------------------------------------------------------

	public DescriptorForFitness(String shortName, String className, 
			IDescriptor implementation, int resultId, String dictType,
			String[] dictClasses, String dictDefinition, String dictTitle)
	{
		this(shortName, className, implementation, resultId);
		//this.dictType = dictType;
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

	/**
	 * The varName differs from the shortName only for atom/bond specific
	 * parameters
	 * @return
	 */
	public List<String> getVariableNames()
	{
		return varNames;
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
/*
	public String getDictType()
	{
		return dictType;
	}
*/	
//------------------------------------------------------------------------------

	public String getDictTitle()
	{
		return dictTitle;
	}
	
//------------------------------------------------------------------------------

	public String getDictDefinition()
	{
		return dictDefinition;
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
		sb.append(", varName:").append(varNames);		
		sb.append(", className:").append(className);
		sb.append(", resultId:").append(resultId);
		IImplementationSpecification specs = implementation.getSpecification();
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
	
	/**
	 * Utility only meant to print some info
	 */
	public String getDictString() 
	{
		String NL = System.getProperty("line.separator");
		StringBuilder sb = new StringBuilder();
		sb.append("Titile: ").append(dictTitle).append(NL);
		//sb.append("Type: ").append(dictType).append(NL);
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
	
	/**
	 * This is a sort of clooning that builds a new DescriptorForFitness with 
	 * the fields on this one, but a null implementation. The latter will
	 * have to be instatiated elsewhere.
	 * @return
	 */

	public DescriptorForFitness cloneAllButImpl() 
	{
		// NB: this private constructor by-passes the implementation!
		DescriptorForFitness newDff = new DescriptorForFitness(shortName, 
				className, resultId, varNames, smarts, dictType, dictClasses, 
				dictDefinition, dictTitle);
		return newDff;
	}

//------------------------------------------------------------------------------
	
}
