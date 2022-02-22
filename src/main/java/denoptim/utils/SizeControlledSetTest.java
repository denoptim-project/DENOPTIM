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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import denoptim.io.DenoptimIO;

/**
 * Unit test for SizeControlledSet
 * 
 * @author Marco Foscato
 */

public class SizeControlledSetTest
{
    private final String SEP = System.getProperty("file.separator");

    @TempDir
    File tempDir;
    
//------------------------------------------------------------------------------
    
    @Test
    public void test() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        String memoryFile = tempDir.getAbsolutePath() + SEP + "test_memOnDisk";
        String allData = tempDir.getAbsolutePath() + SEP + "test_allData";
        
        SizeControlledSet scs = new SizeControlledSet(10, memoryFile, allData);
        
        int tot = 20;
        String base = "entry";
        
        for (int i=0; i<tot; i++)
        {
            String s = base+i;
            assertTrue(scs.addNewUniqueEntry(s),"Adding "+s);
            assertEquals(i+1,scs.size(),"Size of SCS after adding "+i);
        }
        
        for (int i=0; i<tot; i++)
        {
            String s = base+i;
            assertTrue(scs.contains(s),"Contains "+s);
            assertFalse(scs.addNewUniqueEntry(s),"OverLoading "+s);
        }
        
        int j=-1;
        for (String line : DenoptimIO.readList(allData))
        {
            j++;
            assertEquals(line,base+j,"Line saves in all data file");
        }
    }
    
//------------------------------------------------------------------------------

}
