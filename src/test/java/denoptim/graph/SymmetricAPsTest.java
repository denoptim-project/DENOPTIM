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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;

public class SymmetricAPsTest
{
	
//------------------------------------------------------------------------------

    @Test
    public void testSameAs() throws Exception 
    {
        Vertex v1 = FragmentTest.makeFragmentA();
        Vertex v2 = FragmentTest.makeFragmentA();
        Fragment v3 = (Fragment)FragmentTest.makeFragmentA();
        v3.addAPOnAtom(v3.getAtom(0), APClass.make("foo:0"),
                new Point3d(new double[]{3.0, -1.0, 3.3}));
        
        assertEquals(1, v1.getSymmetricAPSets().size());
        assertEquals(1, v2.getSymmetricAPSets().size());
        assertEquals(1, v3.getSymmetricAPSets().size());
        SymmetricAPs s1 = v1.getSymmetricAPSets().get(0);
        SymmetricAPs s2 = v2.getSymmetricAPSets().get(0);
        SymmetricAPs s3 = v3.getSymmetricAPSets().get(0);
        
        //Make sure APs are different instances
        assertFalse(s1.get(0)==s2.get(0)); 
        
        assertTrue(s1.sameAs(s1));
        assertTrue(s1.sameAs(s2));
        assertFalse(s1.sameAs(s3));
        
        s2.get(0).setAPClass(APClass.make("different:0"));
        assertFalse(s1.sameAs(s2));
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testGetSameAsThis() throws Exception 
    {
        Vertex v1 = FragmentTest.makeFragmentA();
        
        // SymmetricAPs with 3 APs
        Fragment v3 = (Fragment)FragmentTest.makeFragmentA();
        v3.addAPOnAtom(v3.getAtom(0), APClass.make("foo:0"),
                new Point3d(new double[]{3.0, -1.0, 3.3}));
        
        // Two SymmetricAPs: one line in v1 one different
        Fragment v4 = (Fragment)FragmentTest.makeFragmentA();
        v4.addAPOnAtom(v4.getAtom(2), APClass.make("apc:2"),
                new Point3d(new double[]{0.0, 0.0, 3.3}));
        
        assertEquals(1, v1.getSymmetricAPSets().size());
        assertEquals(1, v3.getSymmetricAPSets().size());
        assertEquals(2, v4.getSymmetricAPSets().size());
        SymmetricAPs s1 = v1.getSymmetricAPSets().get(0);
        SymmetricAPs s3 = v3.getSymmetricAPSets().get(0);
        SymmetricAPs s4a = v4.getSymmetricAPSets().get(0);
        SymmetricAPs s4b = v4.getSymmetricAPSets().get(1);
        
        Set<SymmetricAPs> others = new HashSet<SymmetricAPs>();
        others.add(s3);
        others.add(s4a);
        others.add(s4b);
        assertTrue(s4b == s1.getSameAs(others));
        
        others = new HashSet<SymmetricAPs>();
        others.add(s3);
        others.add(s4a);
        assertNull(s1.getSameAs(others));
        
        others = new HashSet<SymmetricAPs>();
        others.add(s1);
        others.add(s3);
        assertTrue(s1 == s1.getSameAs(others));
    }
    
//------------------------------------------------------------------------------
    
}
