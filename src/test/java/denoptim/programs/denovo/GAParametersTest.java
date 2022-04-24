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

package denoptim.programs.denovo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.condition.OS.WINDOWS;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.io.TempDir;

import denoptim.files.FileUtils;
import denoptim.io.DenoptimIO;
import denoptim.programs.RunTimeParameters.ParametersType;

/**
 * Unit test
 * 
 * @author Marco Foscato
 */

public class GAParametersTest
{
    private final String SEP = System.getProperty("file.separator");

    @TempDir
    File tempDir;

//------------------------------------------------------------------------------
    
    @Test
    public void testYesNoTrueFalseKeyword() throws Exception
    {
        GAParameters gaParams = new GAParameters();
        gaParams.interpretKeyword("GA-MUTATEDGRAPHFAILTOLERANT=F");
        assertFalse(gaParams.mutatedGraphFailedEvalTolerant);
        gaParams.interpretKeyword("GA-MUTATEDGRAPHFAILTOLERANT=T");
        assertTrue(gaParams.mutatedGraphFailedEvalTolerant);
        gaParams.interpretKeyword("GA-MUTATEDGRAPHFAILTOLERANT=false");
        assertFalse(gaParams.mutatedGraphFailedEvalTolerant);
        gaParams.interpretKeyword("GA-MUTATEDGRAPHFAILTOLERANT=true");
        assertTrue(gaParams.mutatedGraphFailedEvalTolerant);
        gaParams.interpretKeyword("GA-MUTATEDGRAPHFAILTOLERANT=n");
        assertFalse(gaParams.mutatedGraphFailedEvalTolerant);
        gaParams.interpretKeyword("GA-MUTATEDGRAPHFAILTOLERANT=y");
        assertTrue(gaParams.mutatedGraphFailedEvalTolerant);
        gaParams.interpretKeyword("GA-MUTATEDGRAPHFAILTOLERANT=no");
        assertFalse(gaParams.mutatedGraphFailedEvalTolerant);
        gaParams.interpretKeyword("GA-MUTATEDGRAPHFAILTOLERANT=yes");
        assertTrue(gaParams.mutatedGraphFailedEvalTolerant);
    } 
    
//------------------------------------------------------------------------------
    
    @Test
    public void testInterpretationOfKeywords() throws Exception
    {
        double t = 0.0001;
        GAParameters gaparams = new GAParameters();
        gaparams.interpretKeyword(
                "GA-MULTISITEMUTATIONWEIGHTS=1.1,2.2 , 3.3");
        double[] r = gaparams.getMultiSiteMutationWeights();
        assertEquals(3,r.length);
        assertTrue(Math.abs(1.1-r[0]) < t);
        assertTrue(Math.abs(2.2-r[1]) < t);
        assertTrue(Math.abs(3.3-r[2]) < t);
        
        gaparams.interpretKeyword(
                "GA-MULTISITEMUTATIONWEIGHTS=1 2 3 4");
        r = gaparams.getMultiSiteMutationWeights();
        assertEquals(4,r.length);
        assertTrue(Math.abs(1.0-r[0]) < t);
        assertTrue(Math.abs(2.0-r[1]) < t);
        assertTrue(Math.abs(3.0-r[2]) < t);
    }
    
//------------------------------------------------------------------------------
    
    @Test
    @DisabledOnOs(WINDOWS)
    public void testLogging() throws Exception
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        
        GAParameters gaParams = new GAParameters();
        String uniqueString = "UNIQUESTRING@LOG";
        int[] expectedCounts = new int[] {0, 0, 1, 2, 3, 4, 5, 6, 6, 6};
        
        for (int i=0; i<10; i++)
        {
            int verbosity = i-4;
            String pathname = tempDir.getAbsolutePath() + SEP + i;
            FileUtils.createDirectory(pathname);
            gaParams.setWorkingDirectory(pathname);
            gaParams.readParameterLine(ParametersType.GA_PARAMS.getKeywordRoot()
                    +"VERBOSITY="+verbosity);
            gaParams.processParameters();
            gaParams.startProgramSpecificLogger("UnitTestLogger");
            assertNotNull(gaParams.getLogger());
            gaParams.getLogger().log(Level.SEVERE,"msg-severe "+uniqueString);
            gaParams.getLogger().log(Level.WARNING,"msg-warning "+uniqueString);
            gaParams.getLogger().log(Level.INFO,"msg-info "+uniqueString);
            gaParams.getLogger().log(Level.FINE,"msg-fine "+uniqueString);
            gaParams.getLogger().log(Level.FINER,"msg-finer "+uniqueString);
            gaParams.getLogger().log(Level.FINEST,"msg-finest "+uniqueString);
            String logFileString = gaParams.getLogFilePathname();
            assertTrue(FileUtils.checkExists(logFileString));
            String dataFolder = gaParams.getDataDirectory();
            assertTrue(FileUtils.checkExists(dataFolder));
            ArrayList<String> lines = DenoptimIO.readList(logFileString, true);
            int counter = 0;
            for (String line : lines)
            {
                if (line.contains(uniqueString))
                    counter++;
            }
            assertEquals(expectedCounts[i],counter);
        }
    }
    
//------------------------------------------------------------------------------
    
}
