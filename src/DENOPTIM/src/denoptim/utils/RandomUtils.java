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
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.math3.random.MersenneTwister;

import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;

/**
 * Toolbox for random number generation.
 */

public class RandomUtils
{
    private static long rndSeed = 0L;
    private static MersenneTwister mt = null;
    
    //TODO-GG del
    static ArrayList<String> refTxt;
    static AtomicInteger i = new AtomicInteger(0);

//------------------------------------------------------------------------------

    private static void setSeed(long value)
    {
        rndSeed = value;
    }

//------------------------------------------------------------------------------

    public static long getSeed()
    {
        return rndSeed;
    }
    
//------------------------------------------------------------------------------

    public static void initialiseRNG()
    {
        initialiseSeed();
        mt = new MersenneTwister(rndSeed);
    }
    
//------------------------------------------------------------------------------

    public static void initialiseRNG(long seed)
    {
        setSeed(seed);
        mt = new MersenneTwister(rndSeed);
        
        //TODO-GG del
        /*
        try
        {
            refTxt = DenoptimIO.readList("/tmp/test_Sync/f_3/method");
        } catch (DENOPTIMException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        */
    }
    
//------------------------------------------------------------------------------

    public static MersenneTwister getRNG()
    {
        if (mt == null)
        {
            initialiseRNG();
        }
        return mt;
    }
    
//------------------------------------------------------------------------------

    /**
     * Utility to debug
     */
  //TODO-GG del
    private static void print(Object val, String type)
    {
        /*
        int id = i.getAndIncrement();
        String sss = "asked for "+ type + " "+val.toString();
        if (true)
        {
            Exception ex = new Exception();
            for (int i=0; i<5;i++)
            {
                String cn = ex.getStackTrace()[i].getClassName();
                if (!cn.contains("RandomUtils"))
                {
                    sss = sss + ex.getStackTrace()[i].getClassName()+":"+ex.getStackTrace()[i].getLineNumber()+" ";
                }
            }
        }
       
        System.out.println(sss);
        try
        {
            DenoptimIO.writeData("/tmp/method",sss,true);
            
        } catch (DENOPTIMException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        */
    }
    
//------------------------------------------------------------------------------

    public static double nextDouble()
    {
        double d = getRNG().nextDouble();
        //TODO-GG del
        print(d,"double");
        return d;
    }
    
//------------------------------------------------------------------------------

    public static int nextInt(int i)
    {
        int r = getRNG().nextInt(i);
      //TODO-GG del
        print(r,"int");
        return r;
    }
    
//------------------------------------------------------------------------------

    public static boolean nextBoolean()
    {
        boolean r = getRNG().nextBoolean();
      //TODO-GG del
        print(r,"boolean");
        return r;
    }
    
//------------------------------------------------------------------------------

      public static boolean nextBoolean(double prob)
      {
          double d = nextDouble();
        //TODO-GG del
          print(d,"double-2");
          
          return d < prob;
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
    
    public static <T> T randomlyChooseOne(Collection<T> c)
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

    private static void initialiseSeed()
    {
        SecureRandom sec = new SecureRandom();
        byte[] sbuf = sec.generateSeed(8);
        ByteBuffer bb = ByteBuffer.wrap(sbuf);
        rndSeed = bb.getLong();
    }

//------------------------------------------------------------------------------

}
