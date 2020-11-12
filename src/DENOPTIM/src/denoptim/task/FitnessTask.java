/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no> and
 *   Marco Foscato <marco.foscato@uib.no>
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

package denoptim.task;

import java.util.logging.Level;

import denoptim.exception.DENOPTIMException;
import denoptim.fitness.FitnessParameters;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.utils.TaskUtils;

/**
 * Task that represents the call for a assessing a given graph with any 
 * fitness provider.
 */

public abstract class FitnessTask extends Task
{
    protected final DENOPTIMGraph molGraph;

//------------------------------------------------------------------------------
    
    public FitnessTask(DENOPTIMGraph molGraph)
    {
    	super(TaskUtils.getUniqueTaskIndex());
        this.molGraph = molGraph;
    }
    
//------------------------------------------------------------------------------

    protected void executeFitnessProvider(String inFile, String outFile, 
    		String uidFile) throws Throwable
    {
        
        //TODO: deal with internal fitness and other kinds of fitness
    	
        // build command
        StringBuilder sb = new StringBuilder();
        sb.append(FitnessParameters.getExternalFitnessProviderInterpreter());
        sb.append(" ").append(FitnessParameters.getExternalFitnessProvider())
              .append(" ").append(inFile)
              .append(" ").append(outFile)
              .append(" ").append(workDir)
              .append(" ").append(id)
              .append(" ").append(uidFile);

        String msg = "Calling fitness provider: => " + sb + NL;
        DENOPTIMLogger.appLogger.log(Level.INFO, msg);

        // run the process
        processHandler = new ProcessHandler(sb.toString(), 
        		Integer.toString(id));

        processHandler.runProcess();
        if (processHandler.getExitCode() != 0)
        {
            msg = "Failed to execute fitness provider " 
                + FitnessParameters.getExternalFitnessProviderInterpreter()
                    .toString()
		        + " command '"
		        + FitnessParameters.getExternalFitnessProvider()
		        + "' on " + inFile;
            DENOPTIMLogger.appLogger.severe(msg);
            DENOPTIMLogger.appLogger.severe(processHandler.getErrorOutput());
            throw new DENOPTIMException(msg);
        }
        processHandler = null;
    }

//------------------------------------------------------------------------------

}
