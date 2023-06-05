/*
 *   DENOPTIM
 *   Copyright (C) 2022 Marco Foscato <marco.foscato@uib.no>
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package denoptim.graph;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Attachment point mapping where keys are sorted by natural ordering, i.e.,
 * by {@link AttachmentPoint#compareTo(AttachmentPoint)}. Since there can be
 * attachment points that have the same identifier, but are indeed diverse 
 * instances, this class overwrites the {@link TreeMap#put(Object, Object)}
 * method to ensure that keys <code>a</code> and <code>b</code> that 
 * return <code>false</code> at <code>a == b</code> are updated to acquire 
 * unique identifiers.
 */

public class APTreeMap extends LinkedHashMap<AttachmentPoint, AttachmentPoint>
{
    /**
     * Version identifier
     */
    private static final long serialVersionUID = 3L;

    /**
     * Method that serializes this class without creating loops of references.
     * This is done by using the attachment point identifier of the keys.
     * For this reason the IDs of the keys are expected to be unique. If a
     * duplicate ID is found, this triggers an error.
     */
    public static class APMapSerializer implements JsonSerializer<APTreeMap>
    {
        @Override
        public JsonElement serialize(APTreeMap apmap, Type typeOfSrc,
                JsonSerializationContext context)
        {
            TreeMap<Integer,AttachmentPoint> jsonableMap = new TreeMap<>();
            Set<Integer> foundIDs = new HashSet<Integer>();
            for (Entry<AttachmentPoint, AttachmentPoint> entry
                    : apmap.entrySet())
            {
                int keyID = entry.getKey().getID();
                if (foundIDs.contains(keyID))
                    throw new Error("Found diplicate AP ID for APs that are "
                            + "expected to have unique IDs.");
                jsonableMap.put(keyID, entry.getValue());
            }
            return context.serialize(jsonableMap);
        }
    }
    
//------------------------------------------------------------------------------
    
    @Override
    public AttachmentPoint put(AttachmentPoint key, AttachmentPoint value) 
    {
        return super.put(key, value);
    }

//------------------------------------------------------------------------------
    
    @Override
    public AttachmentPoint remove(Object key) 
    {
        return super.remove(key);
    }
    
//------------------------------------------------------------------------------
  
    @Override
    public AttachmentPoint get(Object key) 
    {
        return super.get(key);
    }
    
//------------------------------------------------------------------------------
}
