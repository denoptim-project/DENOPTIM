package denoptim.graph;

import java.lang.reflect.Type;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import denoptim.exception.DENOPTIMException;
import denoptim.utils.GraphUtils;

/**
 * Attachment point mapping where keys are sorted by natural ordering, i.e.,
 * by {@link AttachmentPoint#compareTo(AttachmentPoint)}. Since there can be
 * attachment points that have the same identifier, but are indeed diverse 
 * instances, this class overwrites the {@link TreeMap#put(Object, Object)}
 * method to ensure that keys <code>a</code> and <code>b</code> that 
 * return <code>false</code> at <code>a == b</code> are updates to acquire 
 * unique identifiers.
 */

public class APTreeMap extends TreeMap<AttachmentPoint, AttachmentPoint>
{

    /**
     * Version identifier
     */
    private static final long serialVersionUID = 3L;
    
    /**
     * Maximum identifier for attachment points
     */
    private int maxID = Integer.MIN_VALUE;

    /**
     * Method that serializes this class without creating loops of references.
     * This is done by using the attachment point identifier of the keys.
     * For this reason the IDs of the keys are kept unique by the 
     * {@link APTreeMap#put(AttachmentPoint, AttachmentPoint)} method.
     */
    public static class APMapSerializer implements JsonSerializer<APTreeMap>
    {
        @Override
        public JsonElement serialize(APTreeMap apmap, Type typeOfSrc,
                JsonSerializationContext context)
        {
            TreeMap<Integer,AttachmentPoint> jsonableMap = new TreeMap<>();
            for (Entry<AttachmentPoint, AttachmentPoint> entry
                    : apmap.entrySet())
            {
                jsonableMap.put(entry.getKey().getID(), entry.getValue());
            }

            return context.serialize(jsonableMap);
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old
     * value is replaced. If the map contains a key with the same value for
     * {@link AttachmentPoint#getID()} of the given key, then the given key 
     * is assigned a new ID if it is the same reference as the key in the map
     * (i.e., is <code>key_in_map == given_key</code> returns <code>true</code>).
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     *
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with {@code key}.)
     */
    
    @Override
    public AttachmentPoint put(AttachmentPoint key, AttachmentPoint value) 
    {
        AttachmentPoint possiblyOverlappingKey = keySet().stream()
                .filter(k -> k.compareTo(key)==0)
                .findFirst()
                .orElse(null);
        if (possiblyOverlappingKey != null
                && possiblyOverlappingKey != key)
        {
            try
            {
                GraphUtils.resetUniqueAPCounter(maxID+1);
            } catch (DENOPTIMException e)
            {
                // This happens if the  atomic integer behind that is 
                // behind GraphUtils.getUniqueAPIndex() is already ahead of 
                // maxID, in which case we do not need to reset it.
            }
            key.setID(GraphUtils.getUniqueAPIndex());
        }
        if (key.getID()>maxID)
            maxID = key.getID();
        return super.put(key, value);
    }
    
//------------------------------------------------------------------------------
}
