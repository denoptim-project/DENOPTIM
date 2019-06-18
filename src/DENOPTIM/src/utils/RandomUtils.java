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
