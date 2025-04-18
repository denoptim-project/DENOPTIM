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

package denoptim.utils;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;

/**
 * Unit test
 * 
 * @author Marco Foscato
 */

public class RandomizerTest
{
    
//------------------------------------------------------------------------------

    @Test
    public void testRandomizerReproducibility() throws Exception
    {
        double thrsld = 0.0000001;
        int tot = 10000000;
        long seed = 1234567;
        Randomizer rngA = new Randomizer(seed);
        
        double[] resA = new double[tot];
        for (int i=0; i<tot; i++)
        {
            resA[i] = rngA.nextDouble();
        }

        Randomizer rngB = new Randomizer(seed);
        double[] resB = new double[tot];
        for (int i=0; i<tot; i++)
        {
            resB[i] = rngB.nextDouble();
        }
        
        for (int i=0; i<tot; i++)
        {
            assertTrue(thrsld > Math.abs(resA[i] - resB[i]),
                    "Inconsistent sequence of random doubles");
        }
    }
    
//------------------------------------------------------------------------------
    
    @Test 
    public void tesGetNoisyPoint() throws Exception
    {
        Randomizer rng = new Randomizer(123);
        double maxVal = 12.34;
        for (int i=0; i<10; i++)
        {
            Point3d p = rng.getNoisyPoint(maxVal);
            assertTrue(maxVal >= Math.abs(p.x));
            assertTrue(maxVal >= Math.abs(p.y));
            assertTrue(maxVal >= Math.abs(p.z));
        }
    }
    
//------------------------------------------------------------------------------
    
    @Test 
    public void testRandomlyChooseOne() throws Exception
    {
        Randomizer rng = new Randomizer(123);
        List<Integer> list = new ArrayList<Integer>();
        
        // On empty list
        for (int i=0; i<3; i++)
        {
            assertNull(rng.randomlyChooseOne(list));
        }
        
        // On filled list
        for (int i=0; i<10; i++)
        {
            list.add(i);
        }
        for (int i=0; i<20; i++)
        {
            Integer chosen = rng.randomlyChooseOne(list);
            assertTrue(chosen >= 0);
            assertTrue(chosen < 10);
        }
        
       
    }
    
//------------------------------------------------------------------------------
    
}
