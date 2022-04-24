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

import java.util.ArrayList;
import java.util.Arrays;

import org.openscience.cdk.qsar.IDescriptor;

/**
 * A variable in the expression defining the fitness. This is typically 
 * obtained by computing a descriptor with any of the available 
 * implementations. Since
 * <ul>
 * <li>the calculation of the descriptor can be tuned by means of parameters,
 * and </li>
 * <li>the numerical value can be chosen to be atom/bond specific,</li>
 * </ul>
 * this class collects all the information that is needed to configure 
 * a descriptor calculation, process the result (e.g. take only specific values 
 * among those produced by the descriptor implementation), and keep a relation 
 * between such value and the variable name in the fitness defining expression.
 */

public class Variable
{
    /**
     * The string used to represent this variable in the fitness defining 
     * expression
     */
    private String varName;
    
    /**
     * The short name of the descriptor implementation.
     */
    private String descName;
    
    /**
     * SMARTS strings used to select atom/bond specific values.
     */
    protected ArrayList<String> smarts;
    
    /**
     * Definition of custom parameters for the configuration of the descriptor
     * implementation.
     */
    protected String[] params;
    
    /**
     * The value computed
     */
    protected Double value;
    
//------------------------------------------------------------------------------
    
    /**
     * Constructs a named variable.
     * @param varName
     */
    public Variable(String varName)
    {
        this.varName = varName;
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns an atom specific variable, i.e., a variable the value of which
     * needs to be obtained from the calculation of the given descriptor
     * on a specific atom that is defined by the given
     * SMARTS string.
     * @param varName the name of the variable
     * @param descName the name of the descriptor as defined in any 
     * implementation of {@link IDescriptor}.
     * @param smartsIdentifier the SMARTS that identifies the atom/s to consider
     * for the calculation of the descriptor and thus the value.
     * @return the variable instance.
     */
    
    public static Variable atomSpecific(String varName, String descName, 
            String smartsIdentifier)
    {
        ArrayList<String> smarts = new ArrayList<String>(Arrays.asList(
                smartsIdentifier.split("\\s+")));
        Variable v = new Variable(varName);
        v.setDescriptorName(descName);
        v.setSMARTS(smarts);
        return v;
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns an parameterized variable, i.e., a variable the value of which
     * needs to be obtained from the calculation of the given descriptor
     * according to the given parameters.
     * @param varName the name of the variable
     * @param descName the name of the descriptor as defined in any 
     * implementation of {@link IDescriptor}.
     * @param paramsStr string defining all the parameters for the calculation 
     * of the descriptor.
     * @return the variable instance.
     */
    
    public static Variable parametrized(String varName, String descName, 
            String paramsStr)
    {
        Variable v = new Variable(varName);
        v.setDescriptorName(descName);
        v.setDescriptorParameters(paramsStr.split(", +"));
        return v;
    }
 
//------------------------------------------------------------------------------
    
    /**
     * Get the name of this variable, i.e., the string used in the 
     * fitness-defining expression.
     * @return the string used in the fitness-defining expression.
     */
    public String getName()
    {
        return varName;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Get the short name of the descriptor implementation
     * @return the short name of the descriptor implementation
     */
    public String getDescriptorName()
    {
        return descName;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Set the short name of the descriptor implementation.
     * @param descName the short name of the descriptor implementation.
     */
    public void setDescriptorName(String descName)
    {
        this.descName = descName;
    }

//------------------------------------------------------------------------------
 
    /**
     * Set the list of smarts used to identify atom/bond-specific values.
     * @param smarts the list of SMARTS string
     */
    public void setSMARTS(ArrayList<String> smarts)
    {
        this.smarts = smarts;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Set the list of strings defining customisable parameter for the 
     * descriptor implementation. The order is expected to be that of the
     * parameters as given by {@link IDescriptor#getParameterNames()}.
     * @param params the list of strings
     */
    public void setDescriptorParameters(String[] params)
    {
        this.params = params;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns a human readable string.
     */
    public String toString()
    {
        String s = "Variable [varName:"+varName+", descName:"+descName
                +", smarts:"+smarts+", pararams:";
        if (params == null)
        {
            s = s + "null";
        } else {
            s = s + "[";
            for (int i=0; i<(params.length-1); i++)
                s = s + params[i] + ", ";
            s = s + params[params.length-1] + "]";
        }
        s = s + "]";
        return s;
    }
    
//------------------------------------------------------------------------------
    
}
