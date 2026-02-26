package denoptim.molecularmodeling.zmatrix;

/**
 * Representation of a bond in the ZMatrix.
 */
public class ZMatrixBond
{
    private ZMatrixAtom atm1;
    private ZMatrixAtom atm2;

//--------------------------------------------------------------------------

    /**
     * Constructor for ZMatrixBond
     * @param atm1 The first atom in the bond
     * @param atm2 The second atom in the bond
     */
    public ZMatrixBond(ZMatrixAtom atm1, ZMatrixAtom atm2)
    {
        this.atm1 = atm1;
        this.atm2 = atm2;
    }

//--------------------------------------------------------------------------
    
    /**
     * Get the first atom in the bond
     * @return The first atom in the bond
     */
    public ZMatrixAtom getAtm1()
    {
        return atm1;
    }
    
//--------------------------------------------------------------------------

    /**
     * Get the second atom in the bond
     * @return The second atom in the bond
     */
    public ZMatrixAtom getAtm2()
    {
        return atm2;
    }
    
//--------------------------------------------------------------------------

    /**
     * Set the first atom in the bond
     * @param atm1 The first atom in the bond to set
     */
    public void setAtm1(ZMatrixAtom atm1)
    {
        this.atm1 = atm1;
    }
    
//--------------------------------------------------------------------------
    
    /**
     * Set the second atom in the bond
     * @param atm2 The second atom in the bond to set
     */
    public void setAtm2(ZMatrixAtom atm2)
    {
        this.atm2 = atm2;
    }

//--------------------------------------------------------------------------

    @Override
    public boolean equals(Object o)
    {
        if (o == null)
            return false;
        
        if (o == this)
            return true;
       
        if (o.getClass() != getClass())
            return false;
        
        ZMatrixBond other = (ZMatrixBond) o;
        return (this.atm1.equals(other.atm1) && this.atm2.equals(other.atm2)) ||
               (this.atm2.equals(other.atm1) && this.atm1.equals(other.atm2));
    }

//--------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        int atm1Id = (atm1 != null) ? atm1.getId() : 0;
        int atm2Id = (atm2 != null) ? atm2.getId() : 0;
        // Use symmetric hash code for undirected bonds
        // Sort IDs to ensure (a1, a2) and (a2, a1) have same hash
        int minId = Math.min(atm1Id, atm2Id);
        int maxId = Math.max(atm1Id, atm2Id);
        return 31 * minId + maxId;
    }

//--------------------------------------------------------------------------

    @Override
    public String toString()
    {
        return atm1.getId() + " " + atm2.getId();
    }

//--------------------------------------------------------------------------
}

