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

package denoptim.molecule;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Class representing a list of IDs pointing to instances that are related by
 * some conventional criterion that is referred as the "symmetry". 
 * Note that the actual criterion that defined a "symmetry" relation is not 
 * defined here. So, this list does not know what is the criterion that puts
 * its entries in relation with each other. Moreover, the list does not know
 * what the entries are, just their numerical identifier.
 *
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */

public class SymmetricSet implements Serializable,Cloneable
{
    private ArrayList<Integer> symIds;

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty "set" (a list, actually).
     */
    public SymmetricSet()
    {
        symIds = new ArrayList<Integer>();
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for a symmetric list of identifiers from the list itself.
     * @param symIds the list of identifiers.
     */
    public SymmetricSet(ArrayList<Integer> symIds)
    {
        this.symIds = new ArrayList<>(symIds);
    }

//------------------------------------------------------------------------------

    /**
     * Return the list of symmetric identifiers.
     * @return the list of symmetric identifiers .
     */
    public ArrayList<Integer> getList()
    {
        return symIds;
    }

//------------------------------------------------------------------------------

    /**
     * Return a specific identifier contained in this "set" (a list in reality).
     * @param i the desired entry number.
     * @return the identifier.
     */
    public int get(int i)
    {
        return symIds.get(i);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Checks whether the given identifiers is contained in this set.
     * @param m_val the identifiers to look for.
     * @return <code>true</code> if the identifiers is contained in this set.
     */
    public boolean contains(Integer m_val)
    {
        return symIds.contains(m_val);
    }

//------------------------------------------------------------------------------

    /**
     * Adds an identifiers to this list.
     * @param id the identifiers to add.
     */
    public void add(int id)
    {
    	if (!symIds.contains(id))
    	{
    	    symIds.add(id);
    	}
    }

//------------------------------------------------------------------------------

    /**
     * Removes the given identifier from the list
     * @param i the identifiers to be removed
     */
    public void remove(Integer i)
    {
        symIds.remove((Integer) i);
        if (symIds.contains(i))
        {
            symIds.remove((Integer) i);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Return the number of identifiers in this list.
     * @return the number of identifiers in this list.
     */
    public int size()
    {
        return symIds.size();	
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns a deep-copy of this set.
     * @return a deep-copy.
     */
    public SymmetricSet clone()
    {
        SymmetricSet c = new SymmetricSet();
        for (Integer i : symIds)
        {
            c.add(Integer.parseInt(i.toString()));
        }
        return c;
    }

//------------------------------------------------------------------------------

    public String toString()
    {
    	StringBuilder sb = new StringBuilder();
    	sb.append("SymmetricSet [symIds=");
    	sb.append(symIds.toString()).append("]");
    	return sb.toString();
    }

//------------------------------------------------------------------------------

}
