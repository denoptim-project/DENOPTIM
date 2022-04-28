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
