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

package denoptim.utils;

import java.io.Serializable;
import java.util.Objects;

/**
 * This class is the equivalent of the Pair data structure used in C++
 * Although <code>AbstractMap.SimpleImmutableEntry<K,V>>/code> is available
 * it does not have a setValue method. 
 * @author Vishwesh Venkatraman
 */
public class ObjectPair implements Serializable
{
    private Object o1;
    private Object o2;
    
//------------------------------------------------------------------------------    
    
    public ObjectPair()
    {
        o1 = null;
        o2 = null;
    }

//------------------------------------------------------------------------------    
    
    public ObjectPair(Object m_o1, Object m_o2) 
    { 
        o1 = m_o1; 
        o2 = m_o2; 
    }

//------------------------------------------------------------------------------
 
    public boolean isSame(Object o1, Object o2) 
    {
        return Objects.equals(o1, o2);
    }

//------------------------------------------------------------------------------
 
    public Object getFirst() 
    { 
        return o1;
    }
    
//------------------------------------------------------------------------------
    
    public Object getSecond() 
    { 
        return o2; 
    }
    
//------------------------------------------------------------------------------
 
    public void setFirst(Object o) 
    { 
        o1 = o; 
    }
    
//------------------------------------------------------------------------------
    
    public void setSecond(Object o) 
    { 
        o2 = o; 
    }
    
//------------------------------------------------------------------------------
 
    @Override
    public boolean equals(Object obj) 
    {
        if (obj == null) 
            return false;

        if (!(obj instanceof ObjectPair))
            return false;

        ObjectPair p = (ObjectPair)obj;
        return (isSame(p.o1, this.o1) && isSame(p.o2, this.o2));
    }
    
//------------------------------------------------------------------------------    
    
    @Override
    public int hashCode() 
    { 
        return ((o1 == null ? 0 : o1.hashCode()) ^
                (o2 == null ? 0 : o2.hashCode()));
    }

//------------------------------------------------------------------------------
 
    @Override
    public String toString()
    {
        return "DENOPTIMPair{"+o1+", "+o2+"}";
    }
    
//------------------------------------------------------------------------------    
 
}
