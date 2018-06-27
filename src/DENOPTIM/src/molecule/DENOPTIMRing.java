package molecule;

import java.util.ArrayList;
import java.io.Serializable;

/**
 * This class represents the closure of a ring in a spamming tree
 *
 * @author Marco Foscato
 */
public class DENOPTIMRing implements Serializable 
{
    /**
     * List of <code>DENOPTIMVertex</code> involved in the ring. 
     */
    private ArrayList<DENOPTIMVertex> verteces;

    /**
     * Bond type (i.e., bond order) to be used between head and tail vertices
     */
    private int bndTyp = 1; 

//------------------------------------------------------------------------------

    public DENOPTIMRing()
    {
        verteces = new ArrayList<DENOPTIMVertex>();
    }
    
//------------------------------------------------------------------------------
    
    public DENOPTIMRing(ArrayList<DENOPTIMVertex> m_verteces)
    {
        verteces = m_verteces;
    }
    
//------------------------------------------------------------------------------

    /**
     * Append a <code>DENOPTIMVertex</code> to the list
     */

    public void addVertex(DENOPTIMVertex v)
    {
        verteces.add(v);
    }
    
//------------------------------------------------------------------------------

    /**
     * @return the first vertex in the list
     * of <code>DENOPTIMVertex</code>s involved in this ring
     */

    public DENOPTIMVertex getHeadVertex()
    {
        return verteces.get(0);
    }

//------------------------------------------------------------------------------

    /**
     * @return the last vertex in the list
     * of <code>DENOPTIMVertex</code>s involved in this ring
     */

    public DENOPTIMVertex getTailVertex()
    {
        return verteces.get(verteces.size() - 1);
    }

//------------------------------------------------------------------------------

    /**
     * @return the vertex at the given position in the list
     * of <code>DENOPTIMVertex</code>s involved in this ring
     */

    public DENOPTIMVertex getVertexAtPosition(int i)
    {
	if (i>=verteces.size() || i<0)
	{
	    return null;
	}
        return verteces.get(i);
    }

//------------------------------------------------------------------------------

    /**
     * @return the size of the list
     * of <code>DENOPTIMVertex</code>s involved in this ring
     */

    public int getSize()
    {
	return verteces.size();
    }

//------------------------------------------------------------------------------

    /**
     * @return the bond type (i.e., bond order) of the chord connecting
     * the head and the tail vertices
     */

    public int getBondType()
    {
        return bndTyp;
    }

//------------------------------------------------------------------------------

    /**
     * Set the bond type (i.e., bond order) of the chord connecting
     * the head and the tail vertices
     */

    public void setBondType(int bndType)
    {
        this.bndTyp = bndTyp;
    }

//------------------------------------------------------------------------------

    /**
     * Checks whether a given vertex is part of this ring.
     * @param v the candidate <code>DENOPTIMVertex</code>
     * @return <code>true</code> if the given vertex is contained in the list
     * of <code>DENOPTIMVertex</code>s involved in this ring
     */

    public boolean contains(DENOPTIMVertex v)
    {
        return verteces.contains(v);
    }

//------------------------------------------------------------------------------

    /**
     * Checks whether a given vertex is part of this ring.
     * @param vid the ID of the candidate <code>DENOPTIMVertex</code>
     * @return <code>true</code> if the list
     * of <code>DENOPTIMVertex</code>s involved in this ring contains the given
     * vertex ID
     */

    public boolean containsID(int vid)
    {
	boolean result = false;
	for (DENOPTIMVertex v : verteces)
	{
	    if (v.getVertexId() == vid)
	    {
		result = true;
		break;
	    }
	}
	return result;
    }
    
//------------------------------------------------------------------------------

    /**
     * @return the string representation of this ring
     */

    @Override
    public String toString()
    {
        return "DENOPTIMRing [verteces=" + verteces + "]";
    }

//------------------------------------------------------------------------------    
}
