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

package denoptim.programs.fragmenter;

import java.io.File;
import java.util.List;

import denoptim.combinatorial.CombinatorialExplorerByLayer;
import denoptim.fragmenter.ConformerExtractorTask;
import denoptim.fragmenter.ParallelFragmentationAlgorithm;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;
import denoptim.io.DenoptimIO;
import denoptim.task.ProgramTask;


/**
 * Tool to create fragments by chopping 2D/3D chemical structures.
 *
 * @author Marco Foscato
 */

public class Fragmenter extends ProgramTask
{
    private  ParallelFragmentationAlgorithm fragAlgorithm = null;

//------------------------------------------------------------------------------
    
    /**
     * Creates and configures the program task.
     * @param configFile the file containing the configuration parameters.
     * @param workDir the file system location from which to run the program.
     */
    public Fragmenter(File configFile, File workDir)
    {
        super(configFile, workDir);
    }
  
//------------------------------------------------------------------------------
    
    @Override
    public void runProgram() throws Throwable
    {
        FragmenterParameters settings = new FragmenterParameters();
        if (workDir != null)
        {
            settings.setWorkDirectory(workDir.getAbsolutePath());
        }
        settings.readParameterFile(configFilePathName.getAbsolutePath());
        settings.checkParameters();
        settings.processParameters();
        settings.startProgramSpecificLogger(loggerIdentifier);
        settings.printParameters();
        
        if (settings.isStandaloneFragmentClustering())
        {

            List<Vertex> fragments = DenoptimIO.readVertexes(
                    new File(settings.getStructuresFile()), BBType.UNDEFINED);
            ConformerExtractorTask cet = new ConformerExtractorTask(fragments,
                    settings);
            cet.call();
        } else {
            fragAlgorithm = new ParallelFragmentationAlgorithm(settings);
            fragAlgorithm.run();
        }
        
        stopLogger();
    }
    
//------------------------------------------------------------------------------

    protected void handleThrowable()
    {
        if (fragAlgorithm != null)
        {
            fragAlgorithm.stopRun();
        }
        super.handleThrowable();
    }
      
//------------------------------------------------------------------------------  

}
