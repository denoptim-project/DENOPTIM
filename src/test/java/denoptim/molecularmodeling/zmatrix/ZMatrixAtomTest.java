/*
 *   DENOPTIM
 *   Copyright (C) 2024 Marco Foscato <marco.foscato@uib.no>
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

package denoptim.molecularmodeling.zmatrix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit test for ZMatrixAtom
 * 
 * @author Marco Foscato
 */

public class ZMatrixAtomTest
{
//------------------------------------------------------------------------------
    
    public static ZMatrixAtom getTestZMatrixAtom()
    {
        ZMatrixAtom atom1 = new ZMatrixAtom(0, "C", "C.3", null, null, null,
                1.5, 109.47, 180.0, 0);
        ZMatrixAtom atom2 = new ZMatrixAtom(1, "C", "C.3", null, null, null,
                1.5, 109.47, 180.0, 0);
        ZMatrixAtom atom3 = new ZMatrixAtom(2, "C", "C.3", null, null, null,
                1.5, 109.47, 180.0, 0);
        ZMatrixAtom atom4 = new ZMatrixAtom(3, "H", "H.1", atom1, atom2, atom3, 
        1.0, 109.47, 180.0, -1);
        return atom4;
    }

//------------------------------------------------------------------------------
    
    @Test
    public void testEquals() throws Exception
    {
        ZMatrixAtom atom1 = getTestZMatrixAtom();
        ZMatrixAtom atom2 = getTestZMatrixAtom();

        assertEquals(atom1, atom1);
        assertEquals(atom1, atom2);
        assertNotEquals(atom1, null);

        atom2.setId(1);
        assertNotEquals(atom1, atom2);

        atom2 = getTestZMatrixAtom();
        atom2.setSymbol("Cl");
        assertNotEquals(atom1, atom2);

        atom2 = getTestZMatrixAtom();
        atom2.setType("XX.3");
        assertNotEquals(atom1, atom2);

        atom2 = getTestZMatrixAtom();
        atom2.setBondLength(2.0);
        assertNotEquals(atom1, atom2);

        atom2 = getTestZMatrixAtom();
        atom2.setAngleValue(120.0);
        assertNotEquals(atom1, atom2);

        atom2 = getTestZMatrixAtom();
        atom2.setAngle2Value(120.0);
        assertNotEquals(atom1, atom2);

        atom2 = getTestZMatrixAtom();
        atom2.setChiralFlag(1);
        assertNotEquals(atom1, atom2);

        atom2 = getTestZMatrixAtom();
        atom2.setBondRefAtom(atom1);
        assertNotEquals(atom1, atom2);

        atom2 = getTestZMatrixAtom();
        atom2.setAngleRefAtom(atom1);
        assertNotEquals(atom1, atom2);

        atom2 = getTestZMatrixAtom();
        atom2.setAngle2RefAtom(atom1);
        assertNotEquals(atom1, atom2);
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testZMatrixAtomHashCode() throws Exception
    {
        ZMatrixAtom atom1 = new ZMatrixAtom(0, "C", "C.3", null, null, null,
                1.5, 109.47, 180.0, 0);
        ZMatrixAtom atom2 = new ZMatrixAtom(0, "C", "C.3", null, null, null,
                1.5, 109.47, 180.0, 0);
        
        assertEquals(atom1.hashCode(), atom2.hashCode(), 
                "Equal atoms should have same hash code");

        atom2 = getTestZMatrixAtom();
        atom2.setId(1);
        assertNotEquals(atom1.hashCode(), atom2.hashCode());
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testZMatrixBondEquals() throws Exception
    {
        ZMatrixAtom atom1 = new ZMatrixAtom(0, "C", "C.3", null, null, null,
                null, null, null, null);
        ZMatrixAtom atom2 = new ZMatrixAtom(1, "C", "C.3", null, null, null,
                null, null, null, null);
        ZMatrixAtom atom3 = new ZMatrixAtom(2, "H", "H.1", null, null, null,
                null, null, null, null);
        
        ZMatrixBond bond1 = new ZMatrixBond(atom1, atom2);
        ZMatrixBond bond2 = new ZMatrixBond(atom1, atom2);
        ZMatrixBond bond3 = new ZMatrixBond(atom2, atom1); // Reversed order
        ZMatrixBond bond4 = new ZMatrixBond(atom1, atom3);
        
        assertTrue(bond1.equals(bond2), "Identical bonds should be equal");
        assertTrue(bond1.equals(bond3), 
                "Bonds with reversed atom order should be equal (undirected)");
        assertFalse(bond1.equals(bond4), 
                "Bonds with different atoms should not be equal");
        assertTrue(bond1.equals(bond1), "Bond should equal itself");
        assertFalse(bond1.equals(null), "Bond should not equal null");
        assertFalse(bond1.equals("not a bond"), "Bond should not equal different type");
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testZMatrixBondHashCode() throws Exception
    {
        ZMatrixAtom atom1 = new ZMatrixAtom(0, "C", "C.3", null, null, null,
                null, null, null, null);
        ZMatrixAtom atom2 = new ZMatrixAtom(1, "C", "C.3", null, null, null,
                null, null, null, null);
        
        ZMatrixBond bond1 = new ZMatrixBond(atom1, atom2);
        ZMatrixBond bond2 = new ZMatrixBond(atom2, atom1); // Reversed order
        
        assertEquals(bond1.hashCode(), bond2.hashCode(), 
                "Bonds with reversed order should have same hash code");
    }

//------------------------------------------------------------------------------
    
}

