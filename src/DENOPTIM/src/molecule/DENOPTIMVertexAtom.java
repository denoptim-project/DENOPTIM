/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no>
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
