package molecule;

import java.util.Map;
import java.util.HashMap;

/**
 *
 * @author Vishwesh Venkatraman
 */
public class DENOPTIMVertexAtom 
{
    private int vertexId;
    private final HashMap<Integer, Integer> anum_map;
    
//------------------------------------------------------------------------------
    
    public DENOPTIMVertexAtom(int m_vertexId, 
                                        HashMap<Integer, Integer> m_anum_map)
    {
        vertexId = m_vertexId;
        anum_map = m_anum_map;
    }
    
//------------------------------------------------------------------------------

    public int lookupMatchingAtomNumber(int anum)
    {
        for (Map.Entry<Integer, Integer> entry : anum_map.entrySet())
        {
            if (entry.getKey() == anum)
            {
                return entry.getValue();
            }
        }
        return -1;
    }

//------------------------------------------------------------------------------

    public int getVertexId()
    {
        return vertexId;
    }

//------------------------------------------------------------------------------

    public void setVertexId(int m_vertexId)
    {
        vertexId = m_vertexId;
    }
    
//------------------------------------------------------------------------------    
    
    public void printDetails()
    {
        System.err.println("VERTEX: " + vertexId);
        System.err.println(anum_map.toString());
        System.err.println("----------------");        
    }
    
//------------------------------------------------------------------------------        
    
    public void cleanup()
    {
        if (anum_map != null)
        {
            anum_map.clear();
        }
    }
    
//------------------------------------------------------------------------------            
}
