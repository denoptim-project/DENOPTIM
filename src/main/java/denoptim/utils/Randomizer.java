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

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Collection;

import javax.vecmath.Point3d;

import org.apache.commons.math3.random.MersenneTwister;

import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;

/**
 * Tool to generate random numbers and random decisions.
 */

public class Randomizer
{
    /**
     * Seed used to control the generation of random numbers and decisions. 
     * This allows to make these random numbers/decisions reproducible.
     */
    private long rndSeed = 0L;
    
    /**
     * The implementation of the pseudo-random number generation.
     */
    private MersenneTwister mt = null;
    
    /**
     * local flag used only to enable highly detailed logging.
     */
    private final boolean debug = false;
    
//------------------------------------------------------------------------------
    
    /**
     * Constructor
     */
    public Randomizer()
    {
        initialiseRNG();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Constructor that specifies the random seed
     */
    public Randomizer(long seed)
    {
        initialiseRNG(seed);
    }

//------------------------------------------------------------------------------

    /**
     * Sets the random seed
     * @param value the new value
     */
    private void setSeed(long value)
    {
        rndSeed = value;
    }

//------------------------------------------------------------------------------

    /**
     * @return the current random seed.
     */
    public long getSeed()
    {
        return rndSeed;
    }
    
//------------------------------------------------------------------------------

    /**
     * Initializes this random number generator (RNG) using a random seed that 
     * is generated on-the-fly randomly.
     */
    public void initialiseRNG()
    {
        initialiseSeed();
        mt = new MersenneTwister(rndSeed);
    }
    
//------------------------------------------------------------------------------

    /**
     * Initialized this random number generator using the given seed.
     * @param seed the seed to be used.
     */
    public void initialiseRNG(long seed)
    {
        setSeed(seed);
        mt = new MersenneTwister(rndSeed);
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the random number generator. Ensures there is an initialized one.
     * @return the random number generator
     */
    private MersenneTwister getRNG()
    {
        if (mt == null)
        {
            initialiseRNG();
        }
        return mt;
    }
    
//------------------------------------------------------------------------------

    /**
     * Utility to debug: writes some log in file '/tmp/rng_debug_log'
     */
    private void print(Object val, String type)
    {
        String sss = "asked for "+ type + " "+val.toString();
        if (true)
        {
            Exception ex = new Exception();
            for (int i=0; i<5;i++)
            {
                String cn = ex.getStackTrace()[i].getClassName();
                if (!cn.contains("RandomUtils"))
                {
                    sss = sss + ex.getStackTrace()[i].getClassName() + ":" 
                            + ex.getStackTrace()[i].getLineNumber()+" ";
                }
            }
        }
        
        try
        {
            DenoptimIO.writeData("/tmp/rng_debug_log",sss,true);
        } catch (DENOPTIMException e)
        {
            e.printStackTrace();
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the next pseudo-random, uniformly distributed double value between
     * 0.0 and 1.0 from this random number generator's sequence.
     * @return the next double between 0.0 and 1.0;
     */
    public double nextDouble()
    {
        double d = getRNG().nextDouble();
        if (debug)
            print(d,"double");
        return d;
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns a pseudo-random, uniformly distributed int value between 0 
     * (inclusive) and the specified value (exclusive), drawn from this random 
     * number generator's sequence.
     * @param i the bound on the random number to be returned. Must be positive.
     * @return the next integer between 0 and the specified value.
     */
    public int nextInt(int i)
    {
        int r = getRNG().nextInt(i);
        if (debug)
            print(r,"int");
        return r;
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the next pseudo-random, uniformly distributed boolean value from 
     * this random number generator's sequence.
     * @return the next boolean.
     */
    public boolean nextBoolean()
    {
        boolean r = getRNG().nextBoolean();
        if (debug)
            print(r,"boolean");
        return r;
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns whether the next pseudo-random, uniformly distributed double 
     * is lower than the specified value.
     * @param prob the bound on the random double.
     * @return <code>true</code> is the next double is lower than the specified 
     * value.
     */
    public boolean nextBoolean(double prob)
    {
        return nextDouble() < prob;
    }
    
//------------------------------------------------------------------------------
    /**
     * Returns a point in three-dimensional space with a random set of 
     * coordinates, the absolute value of which is at most the value given as
     * argument.
     * @param maxAbsValue the maximum absolute value of any Cartesian coordinate.
     * @return a point in Cartesian space with a random position.
     */
    
    public Point3d getNoisyPoint(double maxAbsValue)
    {
        double xFactor = nextDouble();
        double yFactor = nextDouble();
        double zFactor = nextDouble();
        double xSign = Double.valueOf(nextInt(2));
        double ySign = Double.valueOf(nextInt(2));
        double zSign = Double.valueOf(nextInt(2));

        return new Point3d(maxAbsValue*xFactor*xSign,
                maxAbsValue*yFactor*ySign,
                maxAbsValue*zFactor*zSign);
    }
      
//------------------------------------------------------------------------------
    
    /**
     * Chooses one member among the given collection. Works on either ordered
     * or unordered collections. However, be aware! If the choice you hare 
     * asking this method to make has to be random but
     * reproducible, i.e., controlled by the random seed that configures the 
     * random number generation, so that independent runs of pseudo-random
     * experiments will produce the same numerical outcome, then the collection
     * must have a given order. A type <b>Set</b> is permitted by is not
     * compatible with the reproducibility requirement.
     */
    
    public <T> T randomlyChooseOne(Collection<T> c)
    {
        int chosen = nextInt(c.size());
        int i=0;
        T chosenObj = null;
        for (T o : c)
        {
            if (i == chosen)
            {
                chosenObj = o;
            }
            i++;
        }
        return chosenObj;
    }

//------------------------------------------------------------------------------

    private void initialiseSeed()
    {
        // WARNING: The SecureRandom implementation in Linux is usable only
        // once, then it becomes terribly slow: 1-2 minutes to get a seed!!!
        
        /*
        SecureRandom sec = new SecureRandom();
        byte[] sbuf = sec.generateSeed(8);
        ByteBuffer bb = ByteBuffer.wrap(sbuf);
        rndSeed = bb.getLong();
        */
        rndSeed = System.currentTimeMillis();
    }

//------------------------------------------------------------------------------

}
