package denoptimga;

import org.junit.jupiter.api.Test;

import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMGraphTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
    
}
