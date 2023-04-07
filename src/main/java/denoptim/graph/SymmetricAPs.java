package denoptim.graph;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    /**
     * Version ID
     */
    private static final long serialVersionUID = 4L;
    
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

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("SymmetricAPs [");
        for (int i=0; i<this.size(); i++)
        {
            sb.append(this.get(i).getID());
            if (i<(this.size()-1))
                sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Identifies a set of symmetric APs that, although it contains references 
     * to different instances of {@link AttachmentPoint}s, it is analogous to 
     * this one in the sense defined by 
     * {@link SymmetricAPs#sameAs(SymmetricAPs)} method.
     * @param others
     * @return the first found {@link SymmetricAPs} that is same as this one or 
     * <code>null</code> if no such object is found in the given set.
     */
    public SymmetricAPs getSameAs(Set<SymmetricAPs> others)
    {
        for (SymmetricAPs other : others)
            if (sameAs(other))
                return other;
        return null;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Identifies any set of symmetric APs that, although it contains references 
     * to different instances of {@link AttachmentPoint}s, it is analogous to 
     * this one in the sense defined by 
     * {@link SymmetricAPs#sameAs(SymmetricAPs)} method.
     * @param others
     * @return the first found {@link SymmetricAPs} that is same as this one or 
     * <code>null</code> if no such object is found in the given set.
     */
    public Set<SymmetricAPs> getAllSameAs(Set<SymmetricAPs> others)
    {
        Set<SymmetricAPs> result = new HashSet<SymmetricAPs>();
        for (SymmetricAPs other : others)
            if (sameAs(other))
                result.add(other);
        return result;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Checks if this collection {@link SymmetricAPs} is analogous to the 
     * other one, i.e., they contain {@link AttachmentPoint}s that 
     * satisfy the {@link AttachmentPoint#sameAs(AttachmentPoint)} method in
     * the same order.
     * @param other
     * @return <code>true</code> if this and other are same (not equal!).
     */
    public boolean sameAs(SymmetricAPs other)
    {
        if (this.size()!=other.size())
            return false;
        for (int i=0; i<this.size(); i++)
        {
            if (!this.get(i).sameAs(other.get(i)))
                return false;
        }
        return true;
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
