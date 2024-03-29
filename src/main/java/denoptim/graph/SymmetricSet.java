/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no> and
 *   Marco Foscato <marco.foscato@uib.no>
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

package denoptim.graph;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Class representing a list of references pointing to instances that are 
 * related by some conventional criterion that is referred to as "symmetry
 * relation". 
 * Note that the actual criterion that defined a "symmetry" relation is not 
 * defined here. So, this list does not know what is the criterion that puts
 * its entries in relation with each other. Moreover, the list does not know
 * what the entries are.
 *
 * @author Marco Foscato
 */

// NB: keep it a List, not a Set: having the order helps comparison.

public class SymmetricSet<T> extends ArrayList<T>
{

    /**
     * Version ID
     */
    private static final long serialVersionUID = 4L;
    
//------------------------------------------------------------------------------

    /**
     * Adds an item to this list, if not already present
     * @param item the item to add.
     * @return as for {@link Collection#add(Object)}
     */
    @Override
    public boolean add(T item)
    {
    	if (!contains(item))
    	{
    	    return super.add(item);
    	} else {
    	    return false;
    	}
    }

//------------------------------------------------------------------------------

}
