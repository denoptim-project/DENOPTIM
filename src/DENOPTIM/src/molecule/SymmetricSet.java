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
