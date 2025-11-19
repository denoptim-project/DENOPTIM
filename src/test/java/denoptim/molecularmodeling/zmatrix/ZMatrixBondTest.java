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

public class ZMatrixBondTest
{
//------------------------------------------------------------------------------
    
    public ZMatrixBond getTestZMatrixBond()
    {
        ZMatrixAtom atom1 = new ZMatrixAtom(0, "C", "C.3", null, null, null,
                null, null, null, null);
        ZMatrixAtom atom2 = new ZMatrixAtom(1, "H", "H.3", null, null, null,
                null, null, null, null);
        return new ZMatrixBond(atom1, atom2);
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testEquals() throws Exception
    {
        ZMatrixBond bond1 = getTestZMatrixBond();
        ZMatrixBond bond2 = getTestZMatrixBond();
        
        assertEquals(bond1, bond1);
        assertEquals(bond1, bond2);
        assertNotEquals(bond1, null);

        bond2.setAtm1(bond2.getAtm2());
        assertNotEquals(bond1, bond2);
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

