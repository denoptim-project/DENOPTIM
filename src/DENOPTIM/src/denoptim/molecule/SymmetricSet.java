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
 *
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */

public class SymmetricSet implements Serializable
{
    private ArrayList<Integer> symVrtxIds;

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty set
     */
    public SymmetricSet()
    {
        symVrtxIds = new ArrayList<Integer>();
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for a symmetric set with a list of vertexes IDs
     * @param m_lst the list of vertexes IDs
     */
    public SymmetricSet(ArrayList<Integer> m_lst)
    {
        symVrtxIds = new ArrayList<>(m_lst);
    }

//------------------------------------------------------------------------------

    /**
     * Return the list of symmetric vertexes IDs
     * @return the list of symmetric vertexes IDs 
     */
    public ArrayList<Integer> getList()
    {
        return symVrtxIds;
    }

//------------------------------------------------------------------------------

    /**
     * Return a specific vertex ID contained in this "set" (a list in reality)
     * @param i the desired entry number
     * @return a vertex ID
     */
    public int get(int i)
    {
        return symVrtxIds.get(i);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Checks whether the given vertex ID is contained in this set
     * @param m_val
     * @return <code>true</code> if the vertex ID is contained in this set
     */
    public boolean contains(Integer m_val)
    {
        return symVrtxIds.contains(m_val);
    }

//------------------------------------------------------------------------------

    /**
     * Adds a vertex ID to this set
     * @param id the vertex ID to add
     */
    public void add(int id)
    {
	if (!symVrtxIds.contains(id))
	{
	    symVrtxIds.add(id);
	}
    }

//------------------------------------------------------------------------------

    /**
     * Removes the given vertexID from the list
     * @param vid the vertexID to be removed
     */
    public void remove(Integer vid)
    {
	symVrtxIds.remove((Integer) vid);
        if (symVrtxIds.contains(vid))
        {
	    symVrtxIds.remove((Integer) vid);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Return the number of vertexes in this set
     * @return the number of vertexes in this set
     */
    public int size()
    {
	return symVrtxIds.size();	
    }

//------------------------------------------------------------------------------

    public String toString()
    {
	StringBuilder sb = new StringBuilder();
	sb.append("SymmetricSet [symVrtxIds=");
	sb.append(symVrtxIds.toString()).append("]");
	return sb.toString();
    }

//------------------------------------------------------------------------------

}
