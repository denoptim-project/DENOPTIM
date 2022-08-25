package denoptim.graph;

import org.openscience.cdk.interfaces.IBond;

public class FragIsomorphEdge
{
    /**
     * Bond order or name used to identify the edge type.
     */
    String label = "notABond";
    
    /**
     * Reference to the object that originally generated this edge.
     */
    Object original;
    
    /**
     * <code>true</code> if the original object was a bond.
     */
    boolean isBond = false;

//------------------------------------------------------------------------------
    
    /**
     * Constructor for an edge representing a bond.
     * @param bnd the bond to represent
     */
    public FragIsomorphEdge(IBond bnd)
    {
        this.label = bnd.getOrder().toString();
        this.original = bnd;
        this.isBond = true;
    }

//------------------------------------------------------------------------------
    
    /**
     * Constructor for an empty edge not representing a bond.
     */
    public FragIsomorphEdge()
    {}

//------------------------------------------------------------------------------
    
}
