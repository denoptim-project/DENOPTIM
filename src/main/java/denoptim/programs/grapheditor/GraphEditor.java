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

package denoptim.programs.grapheditor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import denoptim.graph.DGraph;
import denoptim.io.DenoptimIO;
import denoptim.task.ProgramTask;


/**
 * Tool for editing {@link DGraph}s.
 *
 * @author Marco Foscato
 */

public class GraphEditor extends ProgramTask
{

//------------------------------------------------------------------------------
    
    /**
     * Creates and configures the program task.
     * @param configFile the file containing the configuration parameters.
     * @param workDir the file system location from which to run the program.
     */
    public GraphEditor(File configFile, File workDir)
    {
        super(configFile,workDir);
    }
    
//------------------------------------------------------------------------------

    @Override
    public void runProgram() throws Throwable
    {
        GraphEdParameters geParams = new GraphEdParameters();
        geParams.readParameterFile(configFilePathName.getAbsolutePath());
        geParams.checkParameters();
        geParams.processParameters();
        geParams.startProgramSpecificLogger(loggerIdentifier, false); //to STDOUT
        geParams.printParameters();
        
        // We might need the fragment space to read the input graphs with 
        // string-based encoding. Therefore, we read the graph after.
        geParams.readInputGraphs();

        List<DGraph> modGraphs = new ArrayList<DGraph>();
        int i = -1;
        for (DGraph graph : geParams.getInputGraphs())
        {
            i++;
            DGraph modGraph = graph.editGraph(
                    geParams.getGraphEditTasks(),
                    geParams.symmetryFlag(),
                    geParams.getLogger());
            modGraphs.add(modGraph);
        }
        DenoptimIO.writeGraphsToFile(new File(geParams.getOutFile()), 
                geParams.getOutFormat(), modGraphs, geParams.getLogger(),
                geParams.getRandomizer());
    }
    
//-----------------------------------------------------------------------------
    
}
