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

package denoptim.programs.fitnessevaluator;

import java.io.File;

import denoptim.ga.DenoptimGA;
import denoptim.programs.combinatorial.FragSpaceExplorer;
import denoptim.task.ProgramTask;


/**
 * Stand-alone fitness provider. This class implements a method 
 * that allows to run a fitness evaluation in a stand-alone fashion. 
 * The configuration of the fitness provider is given as a DENOPTIM's input 
 * parameter file, that is in all equal to the parameters file used by 
 * {@link DenoptimGA} and {@link FragSpaceExplorer}.
 * 
 * @author Marco Foscato
 */

public class FitnessRunner extends ProgramTask
{
    private FPRunner runner = null;

//------------------------------------------------------------------------------ 

    /**
     * Creates and configures the program task.
     * @param configFile the file containing the configuration parameters.
     * @param workDir the file system location from which to run the program.
     */
    public FitnessRunner(File configFile, File workDir)
    {
        super(configFile, workDir);
    }
    
//------------------------------------------------------------------------------

    @Override
    public void runProgram() throws Throwable
    {
        FRParameters frParams = new FRParameters();
        
        if (workDir != null)
        {
            frParams.setWorkDirectory(workDir.getAbsolutePath());
        }
        
        frParams.readParameterFile(configFilePathName.getAbsolutePath());
        frParams.checkParameters();
        frParams.processParameters();
        frParams.printParameters();
        
        runner = new FPRunner(frParams);
        runner.run();
    }
    
//------------------------------------------------------------------------------

    protected void handleThrowable()
    {
        if (runner != null)
        {
            runner.stopRun();
        }
        super.handleThrowable();
    }
    
//------------------------------------------------------------------------------        
}
