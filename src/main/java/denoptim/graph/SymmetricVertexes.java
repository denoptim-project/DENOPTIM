package denoptim.graph;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * A collection of {@link Vertex}s that are related by a relation that we call
 * "symmetry", even though this class does not define what such relation is.
 * Therefore, any elsewhere-defined relation may characterize the items
 * collected in instances of this class.
 */
public class SymmetricVertexes extends SymmetricSet<Vertex>
{

    /**
     * Version ID
     */
    private static final long serialVersionUID = 4L;
    
//------------------------------------------------------------------------------
    
    public SymmetricVertexes()
    {
        super();
    }
    
//------------------------------------------------------------------------------
    
    public SymmetricVertexes(List<Vertex> selVrtxs)
    {
        super();
        this.addAll(selVrtxs);
    }
  
//------------------------------------------------------------------------------

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("SymmetricVertexes [");
        for (int i=0; i<this.size(); i++)
        {
            sb.append(this.get(i).getVertexId());
            if (i<(this.size()-1))
                sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
    
//------------------------------------------------------------------------------

    public static class SymmetricVertexesSerializer
    implements JsonSerializer<SymmetricVertexes>
    {
        @Override
        public JsonElement serialize(SymmetricVertexes list, Type typeOfSrc,
              JsonSerializationContext context)
        {
            List<Long> vertexIDs = new ArrayList<Long>();
            for (Vertex v : list)
            {
                vertexIDs.add(v.getVertexId());
            }
            return context.serialize(vertexIDs);
        }
    }
    
    //NB: deserialization is done in the Graph deserializer

//------------------------------------------------------------------------------
    
}
