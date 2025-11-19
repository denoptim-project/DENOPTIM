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

package denoptim.molecularmodeling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.logging.Logger;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.graph.DGraph;
import denoptim.molecularmodeling.zmatrix.ZMatrix;
import denoptim.molecularmodeling.zmatrix.ZMatrixAtom;

/**
 * Unit test for ChemicalObjectModel
 * 
 * @author Marco Foscato
 */

public class ChemicalObjectModelTest
{
    private Logger logger = Logger.getLogger("TestLogger");
    
//------------------------------------------------------------------------------
    
    @Test
    public void testUpdateXYZFromINT_FirstAtomAtOrigin() throws Exception
    {
        // Create a simple molecule with one atom
        IAtomContainer fmol = SilentChemObjectBuilder.getInstance().newAtomContainer();
        IAtom atom0 = new Atom("H");
        fmol.addAtom(atom0);
        
        ZMatrix zmat = new ZMatrix();
        ZMatrixAtom zAtom0 = new ZMatrixAtom(
                0, "H", "H.1", null, null, null, 
                null, null, null, null);
        zmat.addAtom(zAtom0);
        
        DGraph molGraph = new DGraph();
        ChemicalObjectModel model = new ChemicalObjectModel(
                molGraph, fmol, zmat, "test", 
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                logger);
        
        model.updateXYZFromINT();
        
        Point3d coord0 = fmol.getAtom(0).getPoint3d();
        assertNotNull(coord0);
        assertEquals(0.0, coord0.x, 1e-10, "First atom X coordinate");
        assertEquals(0.0, coord0.y, 1e-10, "First atom Y coordinate");
        assertEquals(0.0, coord0.z, 1e-10, "First atom Z coordinate");
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testUpdateXYZFromINT_SecondAtomAlongZAxis() throws Exception
    {
        // Create a molecule with two atoms - second along Z axis
        IAtomContainer fmol = SilentChemObjectBuilder.getInstance().newAtomContainer();
        IAtom atom0 = new Atom("H");
        IAtom atom1 = new Atom("H");
        fmol.addAtom(atom0);
        fmol.addAtom(atom1);
        
        ZMatrix zmat = new ZMatrix();
        ZMatrixAtom zAtom0 = new ZMatrixAtom(
                0, "H", "H.1", null, null, null, 
                null, null, null, null);
        ZMatrixAtom zAtom1 = new ZMatrixAtom(
                1, "H", "H.1", zAtom0, null, null,
                1.5, null, null, null);
        zmat.addAtom(zAtom0);
        zmat.addAtom(zAtom1);
        
        DGraph molGraph = new DGraph();
        ChemicalObjectModel model = new ChemicalObjectModel(
                molGraph, fmol, zmat, "test", 
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                logger);
        
        model.updateXYZFromINT();
        
        Point3d coord0 = fmol.getAtom(0).getPoint3d();
        Point3d coord1 = fmol.getAtom(1).getPoint3d();
        
        assertEquals(0.0, coord0.x, 1e-10, "First atom X");
        assertEquals(0.0, coord0.y, 1e-10, "First atom Y");
        assertEquals(0.0, coord0.z, 1e-10, "First atom Z");
        
        assertEquals(0.0, coord1.x, 1e-10, "Second atom X");
        assertEquals(0.0, coord1.y, 1e-10, "Second atom Y");
        assertEquals(1.5, coord1.z, 1e-10, "Second atom Z (bond length)");
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testUpdateXYZFromINT_ThirdAtomWithAngle() throws Exception
    {
        // Create a molecule with three atoms - third with bond and angle
        IAtomContainer fmol = SilentChemObjectBuilder.getInstance().newAtomContainer();
        IAtom atom0 = new Atom("H");
        IAtom atom1 = new Atom("O");
        IAtom atom2 = new Atom("H");
        fmol.addAtom(atom0);
        fmol.addAtom(atom1);
        fmol.addAtom(atom2);
        
        ZMatrix zmat = new ZMatrix();
        ZMatrixAtom zAtom0 = new ZMatrixAtom(
                0, "H", "H.1", null, null, null, 
                null, null, null, null);
        ZMatrixAtom zAtom1 = new ZMatrixAtom(
                1, "O", "O.2", zAtom0, null, null,
                1.0, null, null, null);
        ZMatrixAtom zAtom2 = new ZMatrixAtom(
                2, "H", "H.1", zAtom1, zAtom0, null,
                1.0, 109.5, null, null);
        zmat.addAtom(zAtom0);
        zmat.addAtom(zAtom1);
        zmat.addAtom(zAtom2);
        
        DGraph molGraph = new DGraph();
        ChemicalObjectModel model = new ChemicalObjectModel(
                molGraph, fmol, zmat, "test", 
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                logger);
        
        model.updateXYZFromINT();
        
        Point3d coord0 = fmol.getAtom(0).getPoint3d();
        Point3d coord1 = fmol.getAtom(1).getPoint3d();
        Point3d coord2 = fmol.getAtom(2).getPoint3d();
        
        // First atom at origin
        assertEquals(0.0, coord0.x, 1e-10);
        assertEquals(0.0, coord0.y, 1e-10);
        assertEquals(0.0, coord0.z, 1e-10);
        
        // Second atom along Z axis
        assertEquals(0.0, coord1.x, 1e-10);
        assertEquals(0.0, coord1.y, 1e-10);
        assertEquals(1.0, coord1.z, 1e-10);
        
        // Third atom should have non-zero X coordinate due to angle
        assertTrue(Math.abs(coord2.x) > 1e-6, "Third atom should have X != 0");
        assertEquals(0.0, coord2.y, 1e-10, "Third atom Y should be 0");
        assertTrue(coord2.z > 0, "Third atom Z should be positive");
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testUpdateXYZFromINT_FourthAtomWithDihedral() throws Exception
    {
        // Create a molecule with four atoms - fourth with dihedral (chirality = 0)
        IAtomContainer fmol = SilentChemObjectBuilder.getInstance().newAtomContainer();
        IAtom atom0 = new Atom("C");
        IAtom atom1 = new Atom("C");
        IAtom atom2 = new Atom("H");
        IAtom atom3 = new Atom("H");
        fmol.addAtom(atom0);
        fmol.addAtom(atom1);
        fmol.addAtom(atom2);
        fmol.addAtom(atom3);
        
        ZMatrix zmat = new ZMatrix();
        ZMatrixAtom zAtom0 = new ZMatrixAtom(
                0, "C", "C.3", null, null, null, 
                null, null, null, null);
        ZMatrixAtom zAtom1 = new ZMatrixAtom(
                1, "C", "C.3", zAtom0, null, null,
                1.5, null, null, null);
        ZMatrixAtom zAtom2 = new ZMatrixAtom(
                2, "H", "H.1", zAtom1, zAtom0, null,
                1.1, 109.5, null, null);
        ZMatrixAtom zAtom3 = new ZMatrixAtom(
                3, "H", "H.1", zAtom2, zAtom1, zAtom0,
                1.1, 109.5, 60.0, 0);
        zmat.addAtom(zAtom0);
        zmat.addAtom(zAtom1);
        zmat.addAtom(zAtom2);
        zmat.addAtom(zAtom3);
        
        DGraph molGraph = new DGraph();
        ChemicalObjectModel model = new ChemicalObjectModel(
                molGraph, fmol, zmat, "test", 
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                logger);
        
        model.updateXYZFromINT();
        
        // Verify all atoms have coordinates
        for (int i = 0; i < 4; i++)
        {
            Point3d coord = fmol.getAtom(i).getPoint3d();
            assertNotNull(coord, "Atom " + i + " should have coordinates");
        }
        
        // Fourth atom should have all three coordinates non-zero
        Point3d coord3 = fmol.getAtom(3).getPoint3d();
        assertTrue(Math.abs(coord3.x) > 1e-6 || Math.abs(coord3.y) > 1e-6 
                || Math.abs(coord3.z) > 1e-6, 
                "Fourth atom should have non-zero coordinates");
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testUpdateXYZFromINT_ChiralityOne() throws Exception
    {
        // Test with chirality = 1
        IAtomContainer fmol = SilentChemObjectBuilder.getInstance().newAtomContainer();
        IAtom atom0 = new Atom("C");
        IAtom atom1 = new Atom("C");
        IAtom atom2 = new Atom("H");
        IAtom atom3 = new Atom("H");
        fmol.addAtom(atom0);
        fmol.addAtom(atom1);
        fmol.addAtom(atom2);
        fmol.addAtom(atom3);
        
        ZMatrix zmat = new ZMatrix();
        ZMatrixAtom zAtom0 = new ZMatrixAtom(
                0, "C", "C.3", null, null, null, 
                null, null, null, null);
        ZMatrixAtom zAtom1 = new ZMatrixAtom(
                1, "C", "C.3", zAtom0, null, null,
                1.5, null, null, null);
        ZMatrixAtom zAtom2 = new ZMatrixAtom(
                2, "H", "H.1", zAtom1, zAtom0, null,
                1.1, 109.5, null, null);
        ZMatrixAtom zAtom3 = new ZMatrixAtom(
                3, "H", "H.1", zAtom2, zAtom1, zAtom0,
                1.1, 109.5, 60.0, 1); // chirality = 1
        zmat.addAtom(zAtom0);
        zmat.addAtom(zAtom1);
        zmat.addAtom(zAtom2);
        zmat.addAtom(zAtom3);
        
        DGraph molGraph = new DGraph();
        ChemicalObjectModel model = new ChemicalObjectModel(
                molGraph, fmol, zmat, "test", 
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                logger);
        
        model.updateXYZFromINT();
        
        // Verify all atoms have coordinates
        Point3d coord3 = fmol.getAtom(3).getPoint3d();
        assertNotNull(coord3, "Atom 3 should have coordinates");
        assertTrue(Math.abs(coord3.x) > 1e-6 || Math.abs(coord3.y) > 1e-6 
                || Math.abs(coord3.z) > 1e-6, 
                "Atom 3 should have non-zero coordinates");
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testUpdateXYZFromINT_ChiralityMinusOne() throws Exception
    {
        // Test with chirality = -1
        IAtomContainer fmol = SilentChemObjectBuilder.getInstance().newAtomContainer();
        IAtom atom0 = new Atom("C");
        IAtom atom1 = new Atom("C");
        IAtom atom2 = new Atom("H");
        IAtom atom3 = new Atom("H");
        fmol.addAtom(atom0);
        fmol.addAtom(atom1);
        fmol.addAtom(atom2);
        fmol.addAtom(atom3);
        
        ZMatrix zmat = new ZMatrix();
        ZMatrixAtom zAtom0 = new ZMatrixAtom(
                0, "C", "C.3", null, null, null, 
                null, null, null, null);
        ZMatrixAtom zAtom1 = new ZMatrixAtom(
                1, "C", "C.3", zAtom0, null, null,
                1.5, null, null, null);
        ZMatrixAtom zAtom2 = new ZMatrixAtom(
                2, "H", "H.1", zAtom1, zAtom0, null,
                1.1, 109.5, null, null);
        ZMatrixAtom zAtom3 = new ZMatrixAtom(
                3, "H", "H.1", zAtom2, zAtom1, zAtom0,
                1.1, 109.5, 60.0, -1); // chirality = -1
        zmat.addAtom(zAtom0);
        zmat.addAtom(zAtom1);
        zmat.addAtom(zAtom2);
        zmat.addAtom(zAtom3);
        
        DGraph molGraph = new DGraph();
        ChemicalObjectModel model = new ChemicalObjectModel(
                molGraph, fmol, zmat, "test", 
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                logger);
        
        model.updateXYZFromINT();
        
        // Verify all atoms have coordinates
        Point3d coord3 = fmol.getAtom(3).getPoint3d();
        assertNotNull(coord3, "Atom 3 should have coordinates");
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testUpdateXYZFromINT() throws Exception
    {
        IAtomContainer fmol = SilentChemObjectBuilder.getInstance().newAtomContainer();
        IAtom atom0 = new Atom("C");
        IAtom atom1 = new Atom("C");
        IAtom atom2 = new Atom("H");
        IAtom atom3 = new Atom("H");
        IAtom atom4 = new Atom("P");
        IAtom atom5 = new Atom("O");
        IAtom atom6 = new Atom("Cl");
        IAtom atom7 = new Atom("H");
        fmol.addAtom(atom0);
        fmol.addAtom(atom1);
        fmol.addAtom(atom2);
        fmol.addAtom(atom3);
        fmol.addAtom(atom4);
        fmol.addAtom(atom5);
        fmol.addAtom(atom6);
        fmol.addAtom(atom7);

        ZMatrix zmat = new ZMatrix();
        ZMatrixAtom zAtom0 = new ZMatrixAtom(
                0, "C", "C.3", null, null, null, 
                null, null, null, null);
        ZMatrixAtom zAtom1 = new ZMatrixAtom(
                1, "C", "C.3", zAtom0, null, null,
                1.0, null, null, null);
        ZMatrixAtom zAtom2 = new ZMatrixAtom(
                2, "H", "H.1", zAtom1, zAtom0, null,
                1.0, 90.0, null, null);
        ZMatrixAtom zAtom3 = new ZMatrixAtom(
                3, "H", "H.1", zAtom2, zAtom1, zAtom0,
                1.0, 90.0, 90.0, 0);
        ZMatrixAtom zAtom4 = new ZMatrixAtom(
                4, "P", "P.3", zAtom3, zAtom2, zAtom1,
                1.0, 90.0, -90.0, 0);
        ZMatrixAtom zAtom5 = new ZMatrixAtom(
                5, "O", "O.3", zAtom4, zAtom3, zAtom2,
                1.0, 90.0, 180.0, 0);
        ZMatrixAtom zAtom6 = new ZMatrixAtom(
                6, "Cl", "Cl.1", zAtom4, zAtom3, zAtom5,
                1.0, 90.0, 90.0, 1);
        ZMatrixAtom zAtom7 = new ZMatrixAtom(
                7, "H", "H.1", zAtom4, zAtom3, zAtom5,
                1.0, 90.0, 90.0, -1);

        zmat.addAtom(zAtom0);
        zmat.addAtom(zAtom1);
        zmat.addAtom(zAtom2);
        zmat.addAtom(zAtom3);
        zmat.addAtom(zAtom4);
        zmat.addAtom(zAtom5);
        zmat.addAtom(zAtom6);
        zmat.addAtom(zAtom7);

        DGraph molGraph = new DGraph();
        ChemicalObjectModel model = new ChemicalObjectModel(
                molGraph, fmol, zmat, "test", 
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                logger);

        model.updateXYZFromINT();

        Point3d coord0 = fmol.getAtom(0).getPoint3d();
        Point3d coord1 = fmol.getAtom(1).getPoint3d();
        Point3d coord2 = fmol.getAtom(2).getPoint3d();
        Point3d coord3 = fmol.getAtom(3).getPoint3d();
        Point3d coord4 = fmol.getAtom(4).getPoint3d();
        Point3d coord5 = fmol.getAtom(5).getPoint3d();
        Point3d coord6 = fmol.getAtom(6).getPoint3d();
        Point3d coord7 = fmol.getAtom(7).getPoint3d();
        
        assertEquals(0.0, coord0.x, DENOPTIMConstants.FLOATCOMPARISONTOLERANCE);
        assertEquals(0.0, coord0.y, DENOPTIMConstants.FLOATCOMPARISONTOLERANCE);
        assertEquals(0.0, coord0.z, DENOPTIMConstants.FLOATCOMPARISONTOLERANCE);

        assertEquals(0.0, coord1.x, DENOPTIMConstants.FLOATCOMPARISONTOLERANCE);
        assertEquals(0.0, coord1.y, DENOPTIMConstants.FLOATCOMPARISONTOLERANCE);
        assertEquals(1.0, coord1.z, DENOPTIMConstants.FLOATCOMPARISONTOLERANCE);

        assertEquals(1.0, coord2.x, DENOPTIMConstants.FLOATCOMPARISONTOLERANCE);
        assertEquals(0.0, coord2.y, DENOPTIMConstants.FLOATCOMPARISONTOLERANCE);
        assertEquals(1.0, coord2.z, DENOPTIMConstants.FLOATCOMPARISONTOLERANCE);

        assertEquals(1.0, coord3.x, DENOPTIMConstants.FLOATCOMPARISONTOLERANCE);
        assertEquals(1.0, coord3.y, DENOPTIMConstants.FLOATCOMPARISONTOLERANCE);
        assertEquals(1.0, coord3.z, DENOPTIMConstants.FLOATCOMPARISONTOLERANCE);

        assertEquals(1.0, coord4.x, DENOPTIMConstants.FLOATCOMPARISONTOLERANCE);
        assertEquals(1.0, coord4.y, DENOPTIMConstants.FLOATCOMPARISONTOLERANCE);
        assertEquals(0.0, coord4.z, DENOPTIMConstants.FLOATCOMPARISONTOLERANCE);

        assertEquals(1.0, coord5.x, DENOPTIMConstants.FLOATCOMPARISONTOLERANCE);
        assertEquals(2.0, coord5.y, DENOPTIMConstants.FLOATCOMPARISONTOLERANCE);
        assertEquals(0.0, coord5.z, DENOPTIMConstants.FLOATCOMPARISONTOLERANCE);

        assertEquals(0.0, coord6.x, DENOPTIMConstants.FLOATCOMPARISONTOLERANCE);
        assertEquals(1.0, coord6.y, DENOPTIMConstants.FLOATCOMPARISONTOLERANCE);
        assertEquals(0.0, coord6.z, DENOPTIMConstants.FLOATCOMPARISONTOLERANCE);

        assertEquals(2.0, coord7.x, DENOPTIMConstants.FLOATCOMPARISONTOLERANCE);
        assertEquals(1.0, coord7.y, DENOPTIMConstants.FLOATCOMPARISONTOLERANCE);
        assertEquals(0.0, coord7.z, DENOPTIMConstants.FLOATCOMPARISONTOLERANCE);
    }
    
//------------------------------------------------------------------------------
    
}

