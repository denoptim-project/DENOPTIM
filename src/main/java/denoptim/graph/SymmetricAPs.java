package denoptim.graph;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * A collection of {@link AttachmentPoint}s that are related by a relation 
 * that we call "symmetry", even though this class does not define what such 
 * relation is.
 * Therefore, any elsewhere-defined relation may characterize the items
 * collected in instances of this class.
 */
public class SymmetricAPs extends SymmetricSet<AttachmentPoint>
{
    
//------------------------------------------------------------------------------
    
    public SymmetricAPs()
    {
        super();
    }
    
//------------------------------------------------------------------------------
    
    public SymmetricAPs(List<AttachmentPoint> symAPs)
    {
        super();
        this.addAll(symAPs);
    }
    
//------------------------------------------------------------------------------

    public static class SymmetricAPsSerializer
    implements JsonSerializer<SymmetricAPs>
    {
        @Override
        public JsonElement serialize(SymmetricAPs list, Type typeOfSrc,
              JsonSerializationContext context)
        {
            List<Integer> ids = new ArrayList<Integer>();
            for (AttachmentPoint ap : list)
            {
                ids.add(ap.getID());
            }
            return context.serialize(ids);
        }
    }
    
    //NB: deserialization is done in the DENOPTIMVertexDeserializer
    
//------------------------------------------------------------------------------

}
