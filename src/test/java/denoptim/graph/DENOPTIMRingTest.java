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

public class DENOPTIMRingTest {
	
//------------------------------------------------------------------------------

    @Test
    public void testUndirectedComparison() throws Exception 
    {
        List<DENOPTIMVertex> lst = new ArrayList<DENOPTIMVertex>();
        for (int i=0; i<5; i++)
        {
            EmptyVertex v = new EmptyVertex();
            v.setBuildingBlockId(i);
            lst.add(v);
        }
        DENOPTIMRing r = new DENOPTIMRing(lst);
        
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
        List<DENOPTIMVertex> lst = new ArrayList<DENOPTIMVertex>();
        for (int i=0; i<5; i++)
        {
            EmptyVertex v = new EmptyVertex();
            v.setBuildingBlockId(i);
            lst.add(v);
        }
        DENOPTIMRing r = new DENOPTIMRing(lst);
        
        assertEquals(3,r.getDistance(lst.get(0), lst.get(3)));
        assertEquals(3,r.getDistance(lst.get(3), lst.get(0)));
    }
    
    
//------------------------------------------------------------------------------

    @Test
    public void testGetCloserVertex() throws Exception 
    {
        List<DENOPTIMVertex> lst = new ArrayList<DENOPTIMVertex>();
        for (int i=0; i<10; i++)
        {
            EmptyVertex v = new EmptyVertex();
            v.setBuildingBlockId(i);
            lst.add(v);
        }
        DENOPTIMRing r = new DENOPTIMRing(lst);
        
        DENOPTIMVertex v = r.getCloserTo(r.getVertexAtPosition(4),
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
