package denoptim.graph;

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
    public String property;
    
//------------------------------------------------------------------------------
    
    /**
     * Construct a new relation between two APs.
     * @param apA The first of the APs.
     * @param apB The second of the APs.
     * @param property the string identifying the property putting in relation
     * the two APS.
     */
    public RelatedAPPair(AttachmentPoint apA, AttachmentPoint apB, 
            String property)
    {
        this.apA = apA;
        this.apB = apB;
        this.property = property;
    }
    
//------------------------------------------------------------------------------
    
}