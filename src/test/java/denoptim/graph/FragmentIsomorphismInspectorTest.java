package denoptim.graph;

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.silent.Bond;

import denoptim.exception.DENOPTIMException;

/**
 * Unit test for isomorphism inspector.
 * 
 * @author Marco Foscato
 */

public class FragmentIsomorphismInspectorTest
{
    
//------------------------------------------------------------------------------
    
    public static Fragment makePathologicalFragment(int size) throws DENOPTIMException
    {
        Fragment v = new Fragment();
        for (int i=0; i<size; i++)
        {
            Atom a = new Atom("C", new Point3d());
            v.addAtom(a);
            for (int j=0; j<i; j++)
            {
                v.addBond(new Bond(a,v.getAtom(j)));
            }
            v.addAPOnAtom(a, APClass.make("apc:1"), new Point3d());
        }
        return v;
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testTimeout() throws Exception
    {
        Fragment a = makePathologicalFragment(300);
        Fragment b = makePathologicalFragment(300);
            
        FragmentIsomorphismInspector inspector = 
                new FragmentIsomorphismInspector(a,b, 10000); // 10 secs
        inspector.reportTimeoutIncidents = false;
        assertTrue(inspector.isomorphismExists());
        
        inspector = new FragmentIsomorphismInspector(a,b, 10); // 0.01 secs
        inspector.reportTimeoutIncidents = false;
        assertFalse(inspector.isomorphismExists());
    }
    
//------------------------------------------------------------------------------
   
}
