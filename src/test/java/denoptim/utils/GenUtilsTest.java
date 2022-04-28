package denoptim.utils;

/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no>
 *   and Marco Foscato <marco.foscato@uib.no>
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Unit test
 * 
 * @author Marco Foscato
 */

public class GenUtilsTest
{
//------------------------------------------------------------------------------
    
    @Test
    public void testUnionOfSets() throws Exception
    {
        List<Set<Integer>> list = new ArrayList<Set<Integer>>();
        list.add(new HashSet<Integer>(Arrays.asList(1,2,3)));
        list.add(new HashSet<Integer>(Arrays.asList(7,9)));
        list.add(new HashSet<Integer>(Arrays.asList(8,10)));
        list.add(new HashSet<Integer>(Arrays.asList(2,3,4)));
        list.add(new HashSet<Integer>(Arrays.asList(4,5,6)));
        list.add(new HashSet<Integer>(Arrays.asList(7,8)));
        list.add(new HashSet<Integer>(Arrays.asList(1)));
        list.add(new HashSet<Integer>(Arrays.asList(10)));
        list.add(new HashSet<Integer>(Arrays.asList(11)));
        list.add(new HashSet<Integer>(Arrays.asList(11)));
        list.add(new HashSet<Integer>(Arrays.asList(7,8)));
        list.add(new HashSet<Integer>(Arrays.asList(11)));

        Set<Integer> expectedOne = new HashSet<Integer>(Arrays.asList(
                1,2,3,4,5,6));
        Set<Integer> expectedTwo = new HashSet<Integer>(Arrays.asList(
                7,8,9,10));
        Set<Integer> expectedThree = new HashSet<Integer>(Arrays.asList(
                11));
        
        GenUtils.unionOfIntersectingSets(list);
        assertEquals(3,list.size(),"Number of united sets");
        assertEquals(expectedOne,list.get(0),"Elements in first set");
        assertEquals(expectedTwo,list.get(1),"Elements in second set");
        assertEquals(expectedThree,list.get(2),"Elements in third set");
    }
    
//------------------------------------------------------------------------------
    
}
