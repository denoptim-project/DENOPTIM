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
import org.openscience.cdk.interfaces.IBond;
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
                new FragmentIsomorphismInspector(a, b, 10000, false); // 10 secs
        inspector.reportTimeoutIncidents = false;
        assertTrue(inspector.isomorphismExists());
        
        inspector = new FragmentIsomorphismInspector(a, b, 10, false); // 0.01 secs
        inspector.reportTimeoutIncidents = false;
        assertFalse(inspector.isomorphismExists());
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testIsomorphismExists() throws Exception
    {
        Fragment v = new Fragment();
        Atom c1 = new Atom("C", new Point3d(1,1,1));
        Atom c2 = new Atom("C", new Point3d(2,2,2));
        Atom o = new Atom("O", new Point3d(3,3,3));
        v.addAtom(c1);
        v.addAtom(c2);
        v.addAtom(o);
        v.addBond(new Bond(c1,c2,IBond.Order.SINGLE));
        v.addBond(new Bond(c2,o,IBond.Order.DOUBLE));
        v.addAP(0, APClass.make("a:0"), new Point3d(0,1,2));
        v.addAP(0, APClass.make("a:0"), new Point3d(0,-1,-2));
        v.addAP(0, APClass.make("a:0"), new Point3d(-1,1,-2));
        v.addAP(1, APClass.make("b:0"), new Point3d(3,3,3));
        
        Fragment v2 = v.clone();
        
        FragmentIsomorphismInspector inspector = 
                new FragmentIsomorphismInspector(v, v2, false);
        assertTrue(inspector.isomorphismExists());
        
        // 3D of atom does not matter
        v2 = v.clone();
        v2.getAtom(0).setPoint3d(new Point3d(6,6,6));
        inspector = new FragmentIsomorphismInspector(v, v2, false);
        assertTrue(inspector.isomorphismExists());
        
        // 3D of AP does not matter
        v2 = v.clone();
        v2.getAP(1).setDirectionVector(new Point3d());
        inspector = new FragmentIsomorphismInspector(v, v2, false);
        assertTrue(inspector.isomorphismExists());
        
        // AP class matters depending on flag
        v2 = v.clone();
        v2.getAP(1).setAPClass(APClass.make("c:0"));
        inspector = new FragmentIsomorphismInspector(v, v2, false);
        assertFalse(inspector.isomorphismExists());
        inspector = new FragmentIsomorphismInspector(v, v2, true);
        assertTrue(inspector.isomorphismExists());
        
        // bonding pattern matters
        v2 = v.clone();
        v2.getAtom(0).getBond(v2.getAtom(1)).setOrder(IBond.Order.TRIPLE);
        inspector = new FragmentIsomorphismInspector(v, v2, false);
        assertFalse(inspector.isomorphismExists());
        v2 = v.clone();
        v2.removeBond(0);
        inspector = new FragmentIsomorphismInspector(v, v2, false);
        assertFalse(inspector.isomorphismExists());

        // AP count matters irrespectively on flag
        v2 = v.clone();
        v2.removeAP(v2.getAP(1));
        inspector = new FragmentIsomorphismInspector(v, v2, false);
        assertFalse(inspector.isomorphismExists());
        inspector = new FragmentIsomorphismInspector(v, v2, true);
        assertFalse(inspector.isomorphismExists());
    }
    
//------------------------------------------------------------------------------
   
}
