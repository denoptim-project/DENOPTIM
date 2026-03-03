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

import static org.junit.Assert.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

/**
 * Unit test for ZMatrix
 * 
 * @author Marco Foscato
 */

public class ZMatrixTest
{
    
//------------------------------------------------------------------------------
    
    public ZMatrix getTestZMatrix() throws Exception
    {
        ZMatrix zMatrix = new ZMatrix();
        
        ZMatrixAtom atom0 = new ZMatrixAtom(
                0, "H", "H.1", null, null, null, 
                null, null, null, null);
        
        ZMatrixAtom atom1 = new ZMatrixAtom(
                1, "O", "O.2", atom0, null, null,
                1.5, null, null, null);
        
        ZMatrixAtom atom2 = new ZMatrixAtom(
                2, "C", "C.3", atom1, atom0, null,
                1.4, 109.5, null, null);
        
        ZMatrixAtom atom3 = new ZMatrixAtom(
                3, "H", "H.1", atom2, atom1, atom0,
                1.4, 109.5, 60.0, 0);
        
        ZMatrixAtom atom4 = new ZMatrixAtom(
                4, "H", "H.1", atom2, atom1, atom3,
                1.4, 109.5, 109.0, 1);

        ZMatrixAtom atom5 = new ZMatrixAtom(
                5, "H", "H.1", atom4, atom2, atom1,
                1.4, 109.5, 0.0, 0);

        zMatrix.addAtom(atom0);
        zMatrix.addAtom(atom1);
        zMatrix.addAtom(atom2);
        zMatrix.addAtom(atom3);
        zMatrix.addAtom(atom4);
        zMatrix.addAtom(atom5);

        zMatrix.addBond(atom0, atom1);
        zMatrix.addBond(atom1, atom2);
        zMatrix.addBond(atom2, atom3);
        zMatrix.addBond(atom2, atom4);
        zMatrix.addBond(atom2, atom5);

        return zMatrix;
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testAddAtom() throws Exception
    {
        ZMatrix zMatrix = getTestZMatrix();

        assertEquals(6, zMatrix.getAtomCount());
        assertEquals(5, zMatrix.getBondCount());

        ZMatrixAtom atom1 = zMatrix.getAtom(1);
        
        //NB: this is a duplicate atom!
        zMatrix.addAtom(atom1);

        assertEquals(7, zMatrix.getAtomCount());
        assertEquals(5, zMatrix.getBondCount());
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testRemoveAtom() throws Exception
    {
        ZMatrix zMatrix = getTestZMatrix();
     
        zMatrix.removeAtom(zMatrix.getAtom(2));
        assertEquals(5, zMatrix.getAtomCount());
        assertEquals(1, zMatrix.getBondCount());
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetIndex() throws Exception
    {
        ZMatrix zMatrix = getTestZMatrix();

        assertEquals(0, zMatrix.getIndex(zMatrix.getAtom(0)));
        assertEquals(1, zMatrix.getIndex(zMatrix.getAtom(1)));
        assertEquals(2, zMatrix.getIndex(zMatrix.getAtom(2)));
        
        ZMatrixAtom atomNotInList = new ZMatrixAtom(
                99, "H", "H.1", null, null, null, 0.0, 0.0, 0.0, 0);
        assertEquals(-1, zMatrix.getIndex(atomNotInList));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetBondRefAtomIndex() throws Exception
    {
        ZMatrix zMatrix = getTestZMatrix();

        assertEquals(-1, zMatrix.getBondRefAtomIndex(0));
        assertEquals(0, zMatrix.getBondRefAtomIndex(1));
        assertEquals(1, zMatrix.getBondRefAtomIndex(2));
        assertEquals(4, zMatrix.getBondRefAtomIndex(5));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetAngleRefAtomIndex() throws Exception
    {
        ZMatrix zMatrix = getTestZMatrix();

        assertEquals(-1, zMatrix.getAngleRefAtomIndex(0));
        assertEquals(-1, zMatrix.getAngleRefAtomIndex(1));
        assertEquals(0, zMatrix.getAngleRefAtomIndex(2));
        assertEquals(2, zMatrix.getAngleRefAtomIndex(5));
    }

//------------------------------------------------------------------------------
    
    @Test
    public void testGetAngle2RefAtomIndex() throws Exception
    {
        ZMatrix zMatrix = getTestZMatrix();

        assertEquals(-1, zMatrix.getAngle2RefAtomIndex(0));
        assertEquals(-1, zMatrix.getAngle2RefAtomIndex(1));
        assertEquals(-1, zMatrix.getAngle2RefAtomIndex(2));
        assertEquals(3, zMatrix.getAngle2RefAtomIndex(4));
        assertEquals(1, zMatrix.getAngle2RefAtomIndex(5));
    }

//------------------------------------------------------------------------------
    
    @Test
    public void testGetChiralFlag() throws Exception
    {
        ZMatrix zMatrix = getTestZMatrix();

        assertNull(zMatrix.getChiralFlag(0));
        assertNull(zMatrix.getChiralFlag(1));
        assertNull(zMatrix.getChiralFlag(2));
        assertEquals(0, zMatrix.getChiralFlag(3));
        assertEquals(1, zMatrix.getChiralFlag(4));
        assertEquals(0, zMatrix.getChiralFlag(5));
    }

//------------------------------------------------------------------------------
    
    @Test
    public void testGetIdAndSetId() throws Exception
    {
        ZMatrix zMatrix = getTestZMatrix();
        assertNull(zMatrix.getId());
        
        zMatrix.setId("test-zmatrix-1");
        assertEquals("test-zmatrix-1", zMatrix.getId());
        
        zMatrix.setId("another-id");
        assertEquals("another-id", zMatrix.getId());
        
        zMatrix.setId(null);
        assertNull(zMatrix.getId());
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testAddBond() throws Exception
    {
        ZMatrix zMatrix = getTestZMatrix();
        assertEquals(5, zMatrix.getBondCount());

        zMatrix.addBond(zMatrix.getAtom(0), zMatrix.getAtom(2));
        assertEquals(6, zMatrix.getBondCount());
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testDelBond() throws Exception
    {
        ZMatrix zMatrix = getTestZMatrix();
        assertEquals(5, zMatrix.getBondCount());
        
        zMatrix.delBond(zMatrix.getAtom(0), zMatrix.getAtom(1));
        assertEquals(4, zMatrix.getBondCount());
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetBondsToAdd() throws Exception
    {
        ZMatrix zMatrix = getTestZMatrix();
        
        List<int[]> bondsToAdd = zMatrix.getBondsToAdd();

        assertEquals(1, bondsToAdd.size());

        int[] bondToAdd = bondsToAdd.get(0);
        assertEquals(2, Math.min(bondToAdd[0], bondToAdd[1]));
        assertEquals(5, Math.max(bondToAdd[0], bondToAdd[1]));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetBondsToDel() throws Exception
    {
        ZMatrix zMatrix = getTestZMatrix();
        List<int[]> bondsToDel = zMatrix.getBondsToDel();
        
        assertEquals(1, bondsToDel.size());

        int[] bondToDel = bondsToDel.get(0);
        assertEquals(4, Math.min(bondToDel[0], bondToDel[1]));
        assertEquals(5, Math.max(bondToDel[0], bondToDel[1]));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testClone() throws Exception
    {
        ZMatrix original = getTestZMatrix();
        
        ZMatrix cloned = original.clone();
        
        assertNotNull(cloned);
        assertEquals(original.getId(), cloned.getId());
        assertEquals(original.getAtomCount(), cloned.getAtomCount());
        assertEquals(original.getBondCount(), cloned.getBondCount());
        
        // Verify atom properties are preserved
        for (int i = 0; i < original.getAtomCount(); i++)
        {   
            ZMatrixAtom originalAtom = original.getAtom(i);
            ZMatrixAtom clonedAtom = cloned.getAtom(i);
            assertEquals(originalAtom.getId(), clonedAtom.getId());
            assertEquals(originalAtom.getSymbol(), clonedAtom.getSymbol());
            assertEquals(originalAtom.getType(), clonedAtom.getType());
        }

        for (int i = 0; i < original.getBondCount(); i++)
        {
            ZMatrixBond originalBond = original.getBond(i);
            ZMatrixBond clonedBond = cloned.getBond(i);
            assertEquals(originalBond.getAtm1().getId(), clonedBond.getAtm1().getId());
            assertEquals(originalBond.getAtm2().getId(), clonedBond.getAtm2().getId());
        }
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetBondData() throws Exception
    {
        ZMatrix zMatrix = getTestZMatrix();
        List<int[]> bondData = zMatrix.getBondData();
        assertEquals(5, bondData.size());
        
        int[] bond = bondData.get(0);
        assertEquals(0, bond[0]);
        assertEquals(1, bond[1]);
        // Note: getBondData returns only [id1, id2], no bond order
    }
    
//------------------------------------------------------------------------------
        
    @Test
    public void testZMatrixEquals() throws Exception
    {
        ZMatrix zmat1 = getTestZMatrix();
        ZMatrix zmat2 = getTestZMatrix();

        assertEquals(zmat1, zmat1);
        assertEquals(zmat1, zmat2);
        assertNotEquals(zmat1, null);

        zmat2.setId("Different");
        assertNotEquals(zmat1, zmat2);

        zmat2 = getTestZMatrix();
        zmat2.addAtom(new ZMatrixAtom(6, "H", "H.1", null, null, null, 0.0, 0.0, 0.0, 0));
        assertNotEquals(zmat1, zmat2);

        zmat2 = getTestZMatrix();
        zmat2.addBond(zmat2.getAtom(0), zmat2.getAtom(2));
        assertNotEquals(zmat1, zmat2);

        zmat2 = getTestZMatrix();
        zmat2.delBond(zmat2.getAtom(0), zmat2.getAtom(1));
        assertNotEquals(zmat1, zmat2);
    }
        
//------------------------------------------------------------------------------
        
    @Test
    public void testZMatrixHashCode() throws Exception
    {
        ZMatrix zmat1 = getTestZMatrix();
        ZMatrix zmat2 = getTestZMatrix();
        
        assertEquals(zmat1.hashCode(), zmat2.hashCode(), 
                "Equal ZMatrices should have same hash code");

        zmat2.setId("Different");
        assertNotEquals(zmat1.hashCode(), zmat2.hashCode());
    }
        
//------------------------------------------------------------------------------
    
    @Test
    public void testGetZMatrixFromIAC() throws Exception
    {
        IAtomContainer mol = SilentChemObjectBuilder.getInstance().newAtomContainer();
        IAtom atom0 = new Atom("C", new Point3d(0.0, 0.0, 0.0));
        IAtom atom1 = new Atom("C", new Point3d(0.0, 0.0, 1.0));
        IAtom atom2 = new Atom("H", new Point3d(1.0, 0.0, 1.0));
        IAtom atom3 = new Atom("H", new Point3d(1.0, 1.0, 1.0));
        IAtom atom4 = new Atom("P", new Point3d(1.0, 1.0, 0.0));
        IAtom atom5 = new Atom("O", new Point3d(1.0, 2.0, 0.0));
        IAtom atom6 = new Atom("Cl", new Point3d(0.0, 1.0, 0.0));
        IAtom atom7 = new Atom("H", new Point3d(2.0, 1.0, 0.0));
        mol.addAtom(atom0);
        mol.addAtom(atom1);
        mol.addAtom(atom2);
        mol.addAtom(atom3);
        mol.addAtom(atom4);
        mol.addAtom(atom5);
        mol.addAtom(atom6);
        mol.addAtom(atom7);
        mol.addBond(0, 1, IBond.Order.SINGLE);
        mol.addBond(1, 2, IBond.Order.SINGLE);
        mol.addBond(2, 3, IBond.Order.SINGLE);
        mol.addBond(3, 4, IBond.Order.SINGLE);
        mol.addBond(4, 5, IBond.Order.SINGLE);
        mol.addBond(4, 6, IBond.Order.SINGLE);
        mol.addBond(4, 7, IBond.Order.SINGLE);

        ZMatrix zmat = ZMatrix.getZMatrixFromIAC(mol);

        assertEquals(8, zmat.getAtomCount());
        assertEquals(7, zmat.getBondCount());
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testUsesProperDihedral() throws Exception
    {
        ZMatrix zmat = getTestZMatrix();
        assertFalse(zmat.usesProperDihedral(0, 0));
        assertFalse(zmat.usesProperDihedral(0, 1));
        assertFalse(zmat.usesProperDihedral(1, 1));
        assertFalse(zmat.usesProperDihedral(0, 0));
        assertTrue(zmat.usesProperDihedral(1, 2));
        assertTrue(zmat.usesProperDihedral(2, 1));
        assertTrue(zmat.usesProperDihedral(2, 4));
        assertTrue(zmat.usesProperDihedral(4, 2));
        assertFalse(zmat.usesProperDihedral(4, 5));
        assertFalse(zmat.usesProperDihedral(5, 4));
        assertFalse(zmat.usesProperDihedral(4, 4));
        assertFalse(zmat.usesProperDihedral(5, 5));
        assertFalse(zmat.usesProperDihedral(3, 5));
        assertFalse(zmat.usesProperDihedral(4, 3)); 
        assertFalse(zmat.usesProperDihedral(1, 5));       
    }

//------------------------------------------------------------------------------
}

