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

package molecule;

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

    public SymmetricSet()
    {
        symVrtxIds = new ArrayList<Integer>();
    }

//------------------------------------------------------------------------------

    public SymmetricSet(ArrayList<Integer> m_lst)
    {
        symVrtxIds = new ArrayList<>(m_lst);
    }

//------------------------------------------------------------------------------

    public ArrayList<Integer> getList()
    {
        return symVrtxIds;
    }

//------------------------------------------------------------------------------

    public int get(int i)
    {
        return symVrtxIds.get(i);
    }
    
//------------------------------------------------------------------------------
    
    public boolean contains(Integer m_val)
    {
        return symVrtxIds.contains(m_val);
    }

//------------------------------------------------------------------------------

    public void add(int id)
    {
	if (!symVrtxIds.contains(id))
	{
	    symVrtxIds.add(id);
	}
    }

//------------------------------------------------------------------------------

    /**
     * Removed the given vertexID from the list
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
