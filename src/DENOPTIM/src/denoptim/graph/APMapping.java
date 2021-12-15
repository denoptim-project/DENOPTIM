package denoptim.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import denoptim.exception.DENOPTIMException;

/**
 * Class representing a mapping between attachment points (APs). The relations 
 * have a given order, which is that with which they are put in this map
 * (i.e., this class extends {@link LinkedHashMap}). 
 * The mapping can contain any number of one-to-one relations between APs,
 * and each such relation is sorted: all APs present as keys are assumed to 
 * be related by the same context (e.g., they belong to the same vertex), 
 * and all APs in the value position are related by the same context
 * (e.g., they belong to the same vertex).
 * 
 * @author Marco Foscato
 *
 */

public class APMapping extends LinkedHashMap<DENOPTIMAttachmentPoint, DENOPTIMAttachmentPoint>
    implements Cloneable
{
    /**
     * Version UID
     */
    private static final long serialVersionUID = 1L;

//------------------------------------------------------------------------------

    /**
     * Creates a mapping that links no pair of APs.
     */
    public APMapping()
    {
        super();
    }
    
//------------------------------------------------------------------------------

    /**
     * Produced an index-based version of this mapping where each index
     * represents the attachment point as its index in the owner vertex
     * (i.e., the integer is the result of 
     * {@link DENOPTIMVertex#getIndexInOwner()}. 
     * For the indexes to work properly, all the 1st/2nd APs in the mapping 
     * pairs, must consistently belong to the same vertex. 
     * @return an index-based analogue of this mapping.
     * @throws DENOPTIMException if APs belong to different owners so the
     * int-based mapping cannot be produced.
     */
    public Map<Integer, Integer> toIntMappig() throws DENOPTIMException
    {
        Map<Integer, Integer> apMap = new HashMap<Integer, Integer>();
     
        if (this.isEmpty())
            return apMap;
        
        DENOPTIMVertex ownerKey = null;
        DENOPTIMVertex ownerVal = null;
        
        for (DENOPTIMAttachmentPoint key : this.keySet())
        {
            if (ownerKey != null && key.getOwner() != ownerKey)
            {
                throw new DENOPTIMException("Owner of AP " 
                        + key.getID() + " is not vertex " 
                        + ownerKey.getVertexId() + ". APMapping cannot be "
                        + "converted to int-mapping.");
            }
            DENOPTIMAttachmentPoint val = this.get(key);
            if (ownerVal != null && val.getOwner() != ownerVal)
            {
                throw new IllegalStateException("Owner of AP " 
                        + val.getID() + " is not vertex " 
                        + ownerKey.getVertexId() + ". APMapping cannot be "
                        + "converted to int-mapping.");
            }
            apMap.put(key.getIndexInOwner(),val.getIndexInOwner());
            
            ownerKey = key.getOwner();
            ownerVal = val.getOwner();
        }
        return apMap;
    }

//------------------------------------------------------------------------------
    
    /**
     * Check if this mapping contains all the key attachment points (APs)
     * in the 1st position of its AP pairs.
     * @param keys the list of key APs.
     * @return <code>true</code> if this mapping contains all the key APs
     * and they are all in the 1st position of a pair.
     */
    public boolean containsAllKeys(ArrayList<DENOPTIMAttachmentPoint> keys)
    {
        return this.keySet().containsAll(keys);
    }
    
//------------------------------------------------------------------------------

    /**
     * Shallow cloning.
     */
    @Override
    public APMapping clone()
    {
        APMapping c = new APMapping();
        for (DENOPTIMAttachmentPoint key : this.keySet())
        {
            c.put(key, this.get(key));
        }
        return c;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Produces a human readable string based on the AP IDs.
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        for (DENOPTIMAttachmentPoint key : this.keySet())
        {
            sb.append(key.getID()+"-"+this.get(key).getID()+" ");
        }
        return sb.toString();
    }
    
//------------------------------------------------------------------------------

}