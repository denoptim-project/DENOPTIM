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

package denoptim.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;

public class SymmetricSetWithModeTest
{
	
//------------------------------------------------------------------------------

    @Test
    public void testEquals() throws Exception 
    {
        Vertex v1 = FragmentTest.makeFragmentA();
        Vertex v2 = FragmentTest.makeFragmentA();
        SymmetricAPs s1 = v1.getSymmetricAPSets().get(0);
        SymmetricAPs s2 = v2.getSymmetricAPSets().get(0);
        
        SymmetricSetWithMode sm1 = new SymmetricSetWithMode(s1, "foo");
        SymmetricSetWithMode sm2 = new SymmetricSetWithMode(s1, "foo");
        assertTrue(sm1.equals(sm1));
        assertTrue(sm1.equals(sm2));
        assertTrue(sm2.equals(sm1));
        assertFalse(sm1==sm2);
        
        sm2.mode = "changed";
        assertFalse(sm1.equals(sm2));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testHashCode() throws Exception 
    {
        Vertex v1 = FragmentTest.makeFragmentA();
        Vertex v2 = FragmentTest.makeFragmentA();
        SymmetricAPs s1 = v1.getSymmetricAPSets().get(0);
        SymmetricAPs s2 = v2.getSymmetricAPSets().get(0);
        
        SymmetricSetWithMode sm1 = new SymmetricSetWithMode(s1, "foo");
        SymmetricSetWithMode sm2 = new SymmetricSetWithMode(s1, "foo");
        
        int hc1 = sm1.hashCode();
        int hc2 = sm2.hashCode();
        assertEquals(hc1, hc2);

        sm2.mode = "changed";
        hc2 = sm2.hashCode();
        assertNotEquals(hc1, hc2);
    }
    
//------------------------------------------------------------------------------
    
}
