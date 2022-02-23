package denoptim.denoptimga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit test
 * 
 * @author Marco Foscato
 */

public class GAParametersTest
{
    
//------------------------------------------------------------------------------
    
    @Test
    public void test() throws Exception
    {
        
        double t = 0.0001;
        GAParameters.interpretKeyword(
                "GA-MULTISITEMUTATIONWEIGHTS=1.1,2.2 , 3.3");
        double[] r = GAParameters.getMultiSiteMutationWeights();
        assertEquals(3,r.length);
        assertTrue(Math.abs(1.1-r[0]) < t);
        assertTrue(Math.abs(2.2-r[1]) < t);
        assertTrue(Math.abs(3.3-r[2]) < t);
        
        GAParameters.interpretKeyword(
                "GA-MULTISITEMUTATIONWEIGHTS=1 2 3 4");
        r = GAParameters.getMultiSiteMutationWeights();
        assertEquals(4,r.length);
        assertTrue(Math.abs(1.0-r[0]) < t);
        assertTrue(Math.abs(2.0-r[1]) < t);
        assertTrue(Math.abs(3.0-r[2]) < t);
    }
    
//------------------------------------------------------------------------------
    
}
