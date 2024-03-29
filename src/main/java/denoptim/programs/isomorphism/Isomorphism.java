/*
 *   DENOPTIM
 *   Copyright (C) 2019 Marco Foscato <marco.foscato@uib.no>
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

package denoptim.programs.isomorphism;

import java.io.File;
import java.util.logging.Level;

import denoptim.graph.DGraph;
import denoptim.io.DenoptimIO;
import denoptim.task.ProgramTask;

/**
 * Tool to perform isomorphism analysis on {@link DGraph}s.
 *
 * @author Marco Foscato
 */

public class Isomorphism extends ProgramTask
{
    
//------------------------------------------------------------------------------
    
    /**
     * Creates and configures the program task.
     * @param configFile the file containing the configuration parameters.
     * @param workDir the file system location from which to run the program.
     */
    public Isomorphism(File configFile, File workDir)
    {
        super(configFile,workDir);
    }
    
//------------------------------------------------------------------------------

    @Override
    public void runProgram() throws Throwable
    {
        IsomorphismParameters isomParams = new IsomorphismParameters();
        isomParams.readParameterFile(configFilePathName.getAbsolutePath());
        isomParams.checkParameters();
        isomParams.processParameters();
        isomParams.startProgramSpecificLogger(loggerIdentifier, false); //to STDOUT

        DGraph graphA = null;
        DGraph graphB = null;
        try
        {
            graphA = DenoptimIO.readDENOPTIMGraphsFromFile(
                    new File(isomParams.inpFileGraphA)).get(0);
            graphB = DenoptimIO.readDENOPTIMGraphsFromFile(
                    new File(isomParams.inpFileGraphB)).get(0);
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        isomParams.getLogger().log(Level.INFO, 
                "Checking for isomorphism between ");
        isomParams.getLogger().log(Level.INFO, " -> GraphA: " + graphA);
        isomParams.getLogger().log(Level.INFO, " -> GraphB: " + graphB);
    
        if (graphA.isIsomorphicTo(graphB))
        {
            isomParams.getLogger().log(Level.INFO, 
                    "Graphs are DENOPTIM-isomorphic!");
        } else {
            isomParams.getLogger().log(Level.INFO, 
                    "No DENOPTIM-isomorphism found.");
        }
    }

//------------------------------------------------------------------------------

}
