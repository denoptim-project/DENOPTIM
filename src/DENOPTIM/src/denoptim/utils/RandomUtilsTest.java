package denoptim.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit test
 * 
 * @author Marco Foscato
 */

public class RandomUtilsTest
{
    
//------------------------------------------------------------------------------

    @Test
    public void testCandidateGenerationMethod() throws Exception
    {
        double thrsld = 0.0000001;
        int tot = 10000000;
        long seed = 1234567;
        
        RandomUtils.initialiseRNG(seed);
        double[] resA = new double[tot];
        for (int i=0; i<tot; i++)
        {
            resA[i] = RandomUtils.nextDouble();
        }
        
        RandomUtils.initialiseRNG(seed);
        double[] resB = new double[tot];
        for (int i=0; i<tot; i++)
        {
            resB[i] = RandomUtils.nextDouble();
        }
        
        for (int i=0; i<tot; i++)
        {
            assertTrue(thrsld > Math.abs(resA[i] - resB[i]),
                    "Inconsistent sequence of random doubles");
        }
        
    }
    
//------------------------------------------------------------------------------
    
}
