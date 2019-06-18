package rings;

import java.io.Serializable;

/**
 * ChainLink represents a vertex in a closable chain.
 * This data structure stores information as to the corrersponding
 * molecule ID in the library of fragments, the type of fragment 
 * and the identify of the AP linking to the chain link before and after
 *
 * @author Marco Foscato 
 */

public class ChainLink implements Cloneable, Serializable
{
    /**
     * Fragment ID in library
     */
    private int molID; 

    /**
     * Fragment type
     */
    private int ftype;

    /**
     * Index of AP towards left
     */
    private int apLeft;

    /**
     * Index of AP towards right
     */
    private int apRight;

    /**
     * Total number of APs on this link
     */
    private int numAPs;


//-----------------------------------------------------------------------------

    /**
     *  Constructs an empty ChainLink
     */

    public ChainLink()
    {
    }

//-----------------------------------------------------------------------------

    /**
     *  Constructs a ChainLink from the involved points
     */

    public ChainLink(int molID, int ftype, int apLeft, int apRight)
    {
        this.molID = molID;
        this.ftype = ftype;
        this.apLeft = apLeft;
        this.apRight = apRight;
    }

//-----------------------------------------------------------------------------

    /**
     * @return the index of the molecule in the fragment library
     */

    public int getMolID()
    {
        return molID;
    }

//-----------------------------------------------------------------------------

    /**
     * @return the type of fragment library
     */

    public int getFragType()
    {
        return ftype;
    }

//-----------------------------------------------------------------------------

    /**
     * @return the index of the attachment point derected to the left side
     * of the chain from this link
     */
    
    public int getApIdToLeft()
    {
        return apLeft;
    }

//-----------------------------------------------------------------------------

    /**
     * @return the index of the attachment point derected to the right side
     * of the chain from this link
     */

    public int getApIdToRight()
    {
        return apRight;
    }

//-----------------------------------------------------------------------------

    /**
     * @return the string representation of this ChainLink
     */

    public String toString()
    {
	String str = " ChainLink[molID: " + molID + ", ftype: " + ftype
		      + ", apLeft: " + apLeft + ", apRight: " + apRight + "]";
	return str;
    }

//-----------------------------------------------------------------------------
}

