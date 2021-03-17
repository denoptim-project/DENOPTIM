package denoptim.molecule;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSerializationContext;

/**
 * Attachment point mapping.
 */
public class APMap
        extends TreeMap<DENOPTIMAttachmentPoint, DENOPTIMAttachmentPoint>
{

  public static class APMapSerializer
  implements JsonSerializer<APMap>
  {
      @Override
      public JsonElement serialize(APMap apmap, Type typeOfSrc,
              JsonSerializationContext context)
      {
          TreeMap<Integer,DENOPTIMAttachmentPoint> jsonableMap = new TreeMap<>();
          for (Entry<DENOPTIMAttachmentPoint, DENOPTIMAttachmentPoint> entry
                  : apmap.entrySet())
          {
              jsonableMap.put(entry.getKey().getID(), entry.getValue());
          }

          return context.serialize(jsonableMap);
      }
  }


}
