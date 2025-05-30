package denoptim.graph;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class representing a pair of {@link AttachmentPoint}s related by some 
 * property that is defined by string.
 */
public class RelatedAPPair
{
    /**
     * The first of the APs.
     */
    public AttachmentPoint apA;
    
    /**
     * The second of the APs.
     */
    public AttachmentPoint apB;
    
    /**
     * The property defining the relation between the two APs.
     */
    public Object property;
    
    /**
     * String-like version of the property for comparison purposes.
     */
    public String propID;
    
    
//------------------------------------------------------------------------------
    
    /**
     * Construct a new relation between two APs.
     * @param apA The first of the APs.
     * @param apB The second of the APs.
     * @param property the string identifying the property putting in relation
     * the two APS.
     */
    public RelatedAPPair(AttachmentPoint apA, AttachmentPoint apB, 
            Object property, String propID)
    {
        this.apA = apA;
        this.apB = apB;
        this.property = property;
        this.propID = propID;
    }
    
//------------------------------------------------------------------------------

    @Override
    public boolean equals(Object o)
    {
        if (o == null)
            return false;
        
        if (o == this)
            return true;
       
        if (o.getClass() != getClass())
        return false;
        
        RelatedAPPair other = (RelatedAPPair) o;
        
        if (!this.apA.equals(other.apA))
            return false;
        
        if (!this.apB.equals(other.apB))
            return false;

        if (!this.propID.equals(other.propID))
            return false;
        
        return this.property.equals(other.property);
    }
    
//------------------------------------------------------------------------------

    @Override
    public String toString()
    {
        return apA.getAtomPositionNumber() + ":" + apA.getID() + "-" 
                + apB.getAtomPositionNumber() + ":" + apB.getID()
                + "_" + property;
    }
    
//------------------------------------------------------------------------------
    
}