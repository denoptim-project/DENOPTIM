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

import static denoptim.utils.DENOPTIMMoleculeUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.interfaces.IAtom;

/**
 * Unit test for DENOPTIMMoleculeUtils
 * 
 * @author Marco Foscato
 */

public class DENOPTIMMoleculeUtilsTest
{
	
    @Test
    public void testGetPoint3d() throws Exception
    {
    	
    	IAtom a = new Atom();
    	assertTrue(areCloseEnough(getPoint3d(a).x,0.0));
    	assertTrue(areCloseEnough(getPoint3d(a).y,0.0));
    	assertTrue(areCloseEnough(getPoint3d(a).z,0.0));
    	
    	a.setPoint2d(new Point2d(2.6, -4.2));
    	assertTrue(areCloseEnough(getPoint3d(a).x,2.6));
    	assertTrue(areCloseEnough(getPoint3d(a).y,-4.2));
    	assertTrue(areCloseEnough(getPoint3d(a).z,0.0));
    	
    	a.setPoint3d(new Point3d(2.6, -4.2, 6.4));
    	assertTrue(areCloseEnough(getPoint3d(a).x,2.6));
    	assertTrue(areCloseEnough(getPoint3d(a).y,-4.2));
    	assertTrue(areCloseEnough(getPoint3d(a).z,6.4));
    	
    	assertFalse(areCloseEnough(1.00001, 1.00002));
    }
    
    private boolean areCloseEnough(double a, double b)
    {
    	double delta = 0.0000001;
    	return Math.abs(a-b) <= delta;
    }
    
//------------------------------------------------------------------------------
}
