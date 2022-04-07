package denoptim.graph;

import java.lang.reflect.Type;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Attachment point mapping where keys are sorted by natural ordering.
 */

public class APTreeMap
        extends TreeMap<AttachmentPoint, AttachmentPoint>
{

  public static class APMapSerializer
  implements JsonSerializer<APTreeMap>
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


}
