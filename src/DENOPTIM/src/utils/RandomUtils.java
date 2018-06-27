package utils;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import org.apache.commons.math3.random.MersenneTwister;

/**
 *
 * @author vishwesv
 */
public class RandomUtils
{
    private static long RNDSEED = 0L;
    private static MersenneTwister MTRAND = null;

//------------------------------------------------------------------------------

    private static void setSeed(long value)
    {
        RNDSEED = value;
    }

//------------------------------------------------------------------------------

    public static long getSeed()
    {
        return RNDSEED;
    }
    
//------------------------------------------------------------------------------

    public static void initialiseRNG()
    {
        initialiseSeed();
        MTRAND = new MersenneTwister(RNDSEED);
    }
    
//------------------------------------------------------------------------------

    public static void initialiseRNG(long seed)
    {
        setSeed(seed);
        MTRAND = new MersenneTwister(RNDSEED);
    }
    
//------------------------------------------------------------------------------

    public static MersenneTwister getRNG()
    {
        return MTRAND;
    }

//------------------------------------------------------------------------------

    private static void initialiseSeed()
    {
        SecureRandom sec = new SecureRandom();
        byte[] sbuf = sec.generateSeed(8);
        ByteBuffer bb = ByteBuffer.wrap(sbuf);
        RNDSEED = bb.getLong();
    }

//------------------------------------------------------------------------------

    public static boolean nextBoolean(double prob)
    {
        if (prob == 0.0)
            return false;
        else if (prob == 1.0)
            return true;
        return MTRAND.nextDouble() < prob;
    }

//------------------------------------------------------------------------------

}
