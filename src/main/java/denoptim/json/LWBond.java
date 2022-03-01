package denoptim.json;

import org.openscience.cdk.interfaces.IBond;

/**
 * A light-weight bond representation to facilitate json serialization of 
 * {@link IBond}. It consider only connections that can be defined between two
 * centers, i.e., two atoms or pseudo-atoms.
 *  
 * @author Marco Foscato
 */
public class LWBond
{
    /**
     * 0-based index of the centers, i.e., atoms or pseudo-atoms, connected by
     * this bond.
     */
    protected int[] atomIds = new int[2];
    
    /**
     * Type of bond
     */
    protected IBond.Order bo;
    
//------------------------------------------------------------------------------
    
    
    public LWBond(int atmIdA, int atmIdB, IBond.Order bo)
    {
        this.atomIds[0] = atmIdA;
        this.atomIds[1] = atmIdB;
        this.bo = bo;
    }
    
//------------------------------------------------------------------------------
    
}
