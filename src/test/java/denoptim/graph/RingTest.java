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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit test
 * 
 * @author Marco Foscato
 */

public class RingTest {
	
//------------------------------------------------------------------------------

    @Test
    public void testUndirectedComparison() throws Exception 
    {
        List<Vertex> lst = new ArrayList<Vertex>();
        for (int i=0; i<5; i++)
        {
            EmptyVertex v = new EmptyVertex();
            v.setBuildingBlockId(i);
            lst.add(v);
        }
        Ring r = new Ring(lst);
        
        EmptyVertex newV = new EmptyVertex();
        r.insertVertex(newV, lst.get(0), lst.get(1));
        assertEquals(1,r.indexOf(newV));
        
        EmptyVertex newV2 = new EmptyVertex();
        r.insertVertex(newV2, lst.get(2), lst.get(1));
        assertEquals(2,r.indexOf(newV2));
	}
  
//------------------------------------------------------------------------------

    @Test
    public void testGetDistance() throws Exception 
    {
        List<Vertex> lst = new ArrayList<Vertex>();
        for (int i=0; i<5; i++)
        {
            EmptyVertex v = new EmptyVertex();
            v.setBuildingBlockId(i);
            lst.add(v);
        }
        Ring r = new Ring(lst);
        
        assertEquals(3,r.getDistance(lst.get(0), lst.get(3)));
        assertEquals(3,r.getDistance(lst.get(3), lst.get(0)));
    }
    
    
//------------------------------------------------------------------------------

    @Test
    public void testGetCloserVertex() throws Exception 
    {
        List<Vertex> lst = new ArrayList<Vertex>();
        for (int i=0; i<10; i++)
        {
            EmptyVertex v = new EmptyVertex();
            v.setBuildingBlockId(i);
            lst.add(v);
        }
        Ring r = new Ring(lst);
        
        Vertex v = r.getCloserTo(r.getVertexAtPosition(4),
                r.getVertexAtPosition(4), r.getVertexAtPosition(4));
        assertEquals(r.getVertexAtPosition(4), v);
        

        v = r.getCloserTo(r.getVertexAtPosition(4),
                r.getVertexAtPosition(8), r.getVertexAtPosition(4));
        assertEquals(r.getVertexAtPosition(4), v);
        
        v = r.getCloserTo(r.getVertexAtPosition(4),
                r.getVertexAtPosition(8), r.getVertexAtPosition(5));
        assertEquals(r.getVertexAtPosition(4), v);
        
        v = r.getCloserTo(r.getVertexAtPosition(4),
                r.getVertexAtPosition(8), r.getVertexAtPosition(1));
        assertEquals(r.getVertexAtPosition(4), v);

        
        v = r.getCloserTo(r.getVertexAtPosition(4),
                r.getVertexAtPosition(8), r.getVertexAtPosition(8));
        assertEquals(r.getVertexAtPosition(8), v);
        
        v = r.getCloserTo(r.getVertexAtPosition(4),
                r.getVertexAtPosition(8), r.getVertexAtPosition(7));
        assertEquals(r.getVertexAtPosition(8), v);
        
        v = r.getCloserTo(r.getVertexAtPosition(4),
                r.getVertexAtPosition(8), r.getVertexAtPosition(9));
        assertEquals(r.getVertexAtPosition(8), v);
        

        v = r.getCloserToTail(r.getVertexAtPosition(4),r.getVertexAtPosition(8));
        assertEquals(r.getVertexAtPosition(8), v);
        
        v = r.getCloserToHead(r.getVertexAtPosition(4),r.getVertexAtPosition(8));
        assertEquals(r.getVertexAtPosition(4), v);
    }

//------------------------------------------------------------------------------
}
