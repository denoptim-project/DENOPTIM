package denoptimga;

import denoptim.molecule.*;
import org.junit.jupiter.api.Test;

import denoptim.molecule.DENOPTIMVertex.BBType;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test
 * 
 * @author Marco Foscato
 */

public class EAUtilsTest
{

//------------------------------------------------------------------------------
	
    @Test
    public void testCrowdingProbability() throws Exception
    {
        DENOPTIMGraph g = DENOPTIMGraphTest.makeTestGraphA();
        double t = 0.001;
        double p = 0.0;
        for (DENOPTIMAttachmentPoint ap : g.getAttachmentPoints())
        {
            p = EAUtils.getCrowdingProbability(ap,3,1.0,1.0,1.0);
            assertTrue(Math.abs(1.0 - p)<t,
                    "Scheme 3 should return always 1.0 but was "+p);
        }
        DENOPTIMAttachmentPoint ap3 = g.getVertexAtPosition(0).getAP(3);
        p = EAUtils.getCrowdingProbability(ap3,0,1.0,10,1.0);
        assertTrue(Math.abs(1.0 - p)<t, "Scheme 0 on ap3: 1.0 != "+p);
        p = EAUtils.getCrowdingProbability(ap3,1,1.0,10,1.0);
        assertTrue(Math.abs(1.0 - p)<t, "Scheme 1 on ap3: 1.0 != "+p);
        p = EAUtils.getCrowdingProbability(ap3,2,1.0,10,1.0);
        assertTrue(Math.abs(1.0 - p)<t, "Scheme 2 on ap3: 1.0 != "+p);
        
        DENOPTIMAttachmentPoint ap2 = g.getVertexAtPosition(0).getAP(2);
        p = EAUtils.getCrowdingProbability(ap2,2,1.0,10,1.0);
        assertTrue(Math.abs(0.0 - p)<t, "Scheme 2 on ap2");
    }
  
//------------------------------------------------------------------------------
    
    @Test
    public void testSelectNonScaffoldNonCapVertex() throws Exception
    {
        DENOPTIMGraph g = new DENOPTIMGraph();
        EmptyVertex s = new EmptyVertex();
        s.addAP();
        s.addAP();
        s.setBuildingBlockType(BBType.SCAFFOLD);
        
        EmptyVertex v = new EmptyVertex();
        v.addAP();
        v.addAP();
        v.addAP();
        v.addAP();
        v.setBuildingBlockType(BBType.FRAGMENT);

        EmptyVertex c1 = new EmptyVertex();
        c1.addAP();
        c1.setBuildingBlockType(BBType.CAP);

        EmptyVertex c2 = new EmptyVertex();
        c2.addAP();
        c2.setBuildingBlockType(BBType.CAP);

        EmptyVertex c3 = new EmptyVertex();
        c3.addAP();
        c3.setBuildingBlockType(BBType.CAP);

        EmptyVertex c4 = new EmptyVertex();
        c4.addAP();
        c4.setBuildingBlockType(BBType.CAP);
        
        g.addVertex(s);
        g.appendVertexOnAP(s.getAP(0), v.getAP(0));
        g.appendVertexOnAP(s.getAP(1), c1.getAP(0));
        g.appendVertexOnAP(v.getAP(1), c2.getAP(0));
        g.appendVertexOnAP(v.getAP(2), c3.getAP(0));
        g.appendVertexOnAP(v.getAP(3), c4.getAP(0));
        
        int expected = g.indexOf(v);
        for (int i=0; i<5; i++)
        {
            int chosen = EAUtils.selectNonScaffoldNonCapVertex(g);
            assertEquals(expected, chosen, "Index of the only choosable " +
                    "vertex");
        }
    }
    
//------------------------------------------------------------------------------
    
}
