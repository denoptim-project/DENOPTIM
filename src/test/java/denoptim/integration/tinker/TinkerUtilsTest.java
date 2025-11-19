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

package denoptim.integration.tinker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import denoptim.molecularmodeling.zmatrix.ZMatrix;
import denoptim.molecularmodeling.zmatrix.ZMatrixAtom;

/**
 * Unit test for TinkerUtils
 * 
 * @author Marco Foscato
 */

public class TinkerUtilsTest
{
    @TempDir
    Path tempDir;
 
//------------------------------------------------------------------------------
    
    @Test
    public void testINTRoundTrip() throws Exception
    {
        ZMatrix zmat = new ZMatrix();
        zmat.setId("TestMolecule");
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
        
        zmat.addBond(0, 1);
        zmat.addBond(1, 2);
        zmat.addBond(2, 3);
        zmat.addBond(5, 4);
        zmat.addBond(7, 4);
        zmat.addBond(6, 4);
        zmat.addBond(0, 6);
        
        File outputFile = tempDir.resolve("output.int").toFile();
        TinkerUtils.writeTinkerINT(outputFile.getAbsolutePath(), zmat);
        
        ZMatrix readInZMat = TinkerUtils.readTinkerINT(outputFile.getAbsolutePath());
        
        assertTrue(readInZMat.equals(zmat));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testWriteTinkerINT() throws Exception
    {
        ZMatrix zmat = new ZMatrix();
        zmat.setId("TestMolecule");
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
        
        zmat.addBond(0, 1);
        zmat.addBond(1, 2);
        zmat.addBond(2, 3);
        zmat.addBond(5, 4);
        zmat.addBond(7, 4);
        zmat.addBond(6, 4);
        zmat.addBond(0, 6);
        
        File outputFile = tempDir.resolve("output.int").toFile();
        TinkerUtils.writeTinkerINT(outputFile.getAbsolutePath(), zmat);
        
        assertTrue(outputFile.exists(), "Output file should be created");
        assertTrue(outputFile.length() > 0, "Output file should not be empty");
        
        // Verify file content by reading it as text and checking individual lines
        String content = Files.readString(outputFile.toPath());
        String[] allLines = content.split("\\r?\\n");
        List<String> strippedLines = new ArrayList<>();
        for (String line : allLines)
        {
            String stripped = line.trim();
            if (!stripped.isEmpty())
            {
                strippedLines.add(stripped);
            }
        }
        
        // Check that specific lines match the expected stripped content
        assertEquals("8  TestMolecule", strippedLines.get(0));
        assertEquals("1  C     C.3", strippedLines.get(1));
        assertEquals("2  C     C.3     1   1.00000", strippedLines.get(2));
        assertEquals("3  H     H.1     2   1.00000     1   90.0000", strippedLines.get(3));
        assertEquals("8  H     H.1     5   1.00000     4   90.0000     6   90.0000    -1", 
                strippedLines.get(8));
        
        // Check bond pairs to add (after blank line)
        int bondSectionStart = -1;
        for (int i = 0; i < strippedLines.size(); i++)
        {
            if (strippedLines.get(i).isEmpty() && i + 1 < strippedLines.size())
            {
                bondSectionStart = i + 1;
                break;
            }
        }
        if (bondSectionStart >= 0)
        {
            assertTrue(strippedLines.contains("1     7"));
            assertTrue(strippedLines.contains("5     4"));
        }
        
    }
    
//------------------------------------------------------------------------------
   
    
//------------------------------------------------------------------------------
    
}

