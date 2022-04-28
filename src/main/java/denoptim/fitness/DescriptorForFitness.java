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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.IImplementationSpecification;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.qsar.IDescriptor;

import denoptim.exception.DENOPTIMException;

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
     * Variables that use values calculated by this descriptor.
     */
    protected List<Variable> variables = new ArrayList<Variable>();
	
	/**
	 * Pointer to a specific results among those that are produced by the 
	 * calculation of this descriptor, or 0 for descriptors that produce a 
	 * single value.
	 */
	protected int resultId = 0;
	
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
	
    /**
     * Utility for constructing CDK objects
     */
    private static IChemObjectBuilder cdkBuilder = 
            DefaultChemObjectBuilder.getInstance();
	
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
			IDescriptor implementation, int resultId,
			String[] dictClasses, String dictDefinition, String dictTitle)
	{
		this(shortName, className, implementation, resultId);
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
	 * Overwrites the list of variables using this descriptor.
	 * @param variables the new list of variable.
	 */
	public void setVariables(List<Variable> variables)
	{
	    this.variables = variables;
	}
	
//------------------------------------------------------------------------------

    /**
     * Get the variables that make use of values produced by this descriptor.
     * @return the list of variables.
     */
    public List<Variable> getVariables()
    {
        return variables;
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
		sb.append(", variables:[");
		for (Variable v : variables)
		{
		    sb.append(v.getName() + ", ");
	    }
		sb.append("]");
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
	 * This is a sort of cloning that returns a new DescriptorForFitness 
	 * with the same field content of this one (i.e., deep copy), 
	 * but a shallow copy of the list of variables, and a null implementation.
	 * The latter will have to be instantiated 
	 * elsewhere.
	 * @return a clone with null descriptor implementation
	 */

	public DescriptorForFitness cloneAllButImpl() 
	{
	    DescriptorForFitness dff = new DescriptorForFitness(shortName, 
				className, null, resultId, dictClasses, 
				dictDefinition, dictTitle);
	    dff.setVariables(new ArrayList<Variable>(variables));
	    return dff;
	}

//------------------------------------------------------------------------------
	
	/**
	 * Copy this descriptor and created an independent instance of the
	 * underlying descriptor implementation.
	 * @throws DENOPTIMException
	 */
	public DescriptorForFitness makeCopy() throws DENOPTIMException
	{
	    DescriptorForFitness clone = this.cloneAllButImpl();
        clone.implementation = newDescriptorImplementation(this);
        return clone;
	}
	
//------------------------------------------------------------------------------
	
    private static IDescriptor newDescriptorImplementation(
            DescriptorForFitness oldParent) throws DENOPTIMException
    {
        String className = oldParent.getImplementation().getClass().getName();
        IDescriptor descriptor = null;
        try
        {
            Class<?> cl = Class.forName(className);
            for (Constructor<?> constructor : cl.getConstructors()) 
            {
                Class<?>[] params = constructor.getParameterTypes();
                if (params.length == 0) 
                {
                    descriptor = (IDescriptor) constructor.newInstance();
                } else if (params[0].equals(IChemObjectBuilder.class))
                {
                    //NB potential source of ambiguity on the builder class
                    descriptor = (IDescriptor) constructor.newInstance(cdkBuilder);
                }
            }
        } catch (Throwable t)
        {
            throw new DENOPTIMException("Could not make new instance of '" 
                    + className + "'.", t);
        }
        if (descriptor == null)
        {
            throw new DENOPTIMException("Could not make new instance of '" 
                    + className + "'. No suitable constructor found.");
        }
        descriptor.initialise(cdkBuilder);
        return descriptor;
    }

//------------------------------------------------------------------------------
    
    /**
     * Append the reference to a variable that used data produced by the 
     * calculation of this descriptor.
     * @param v the reference to the variable.
     */
    public void addDependentVariable(Variable v)
    {
        variables.add(v);
    }

//------------------------------------------------------------------------------
	
}
