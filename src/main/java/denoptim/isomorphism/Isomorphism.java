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

package denoptim.isomorphism;

import java.io.File;

import denoptim.exception.DENOPTIMException;
import denoptim.graph.DENOPTIMGraph;
import denoptim.io.DenoptimIO;
import denoptim.task.ProgramTask;

/**
 * Tool to perform isomorphism analysis on {@link DENOPTIMGraph}s.
 *
 * @author Marco Foscato
 */

public class Isomorphism extends ProgramTask
{
    
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
        IsomorphismParameters.readParameterFile(
                configFilePathName.getAbsolutePath());
        IsomorphismParameters.checkParameters();
        IsomorphismParameters.processParameters();
        
        checkIsomorphism();
    }

//------------------------------------------------------------------------------
    
    private static void checkIsomorphism() throws DENOPTIMException
    {
        DENOPTIMGraph graphA = null;
        DENOPTIMGraph graphB = null;
        try
        {
            graphA = DenoptimIO.readDENOPTIMGraphsFromFile(
                    new File(IsomorphismParameters.inpFileGraphA)).get(0);
            graphB = DenoptimIO.readDENOPTIMGraphsFromFile(
                    new File(IsomorphismParameters.inpFileGraphB)).get(0);
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        System.out.println("Checking for isomorphism between ");
        System.out.println(" -> GraphA: "+graphA);
        System.out.println(" -> GraphB: "+graphB);
    
        if (graphA.isIsomorphicTo(graphB))
        {
            System.out.println("Graphs are DENOPTIM-isomorphic!");
        } else {
            System.out.println("No DENOPTIM-isomorphism found.");
        }
    }

//------------------------------------------------------------------------------

}
