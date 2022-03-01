/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no>
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

package denoptim.denoptimga;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import denoptim.exception.DENOPTIMException;
import denoptim.logging.DENOPTIMLogger;
import denoptim.task.ProgramTask;

/**
 * The genetic algorithms entry point.
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class DenoptimGA extends ProgramTask
{

//------------------------------------------------------------------------------
    
    /**
     * Creates and configures the program task.
     * @param configFile the file containing the configuration parameters.
     * @param workDir the file system location from which to run the program.
     */
    public DenoptimGA(File configFile, File workDir)
    {
        super(configFile,workDir);
    }
    
//------------------------------------------------------------------------------

    @Override
    public void runProgram()
    {   
        //TODO: get rid of this one parameters are not static anymore.
    	//needed by static parameters, and in case of subsequent runs in the same JVM
    	GAParameters.resetParameters(); 

        if (workDir != null)
        {
        	GAParameters.setWorkingDirectory(workDir.getAbsolutePath());
        }
        
        ExecutorService executor = null;
        Future<?> futureWatchers = null;
        
        EvolutionaryAlgorithm ea = null;
        ExternalCmdsListener ecl = null;
        try
        {	
            GAParameters.readParameterFile(configFilePathName.getAbsolutePath());
            GAParameters.checkParameters();
            GAParameters.processParameters();
            GAParameters.printParameters();
            
            ecl = new ExternalCmdsListener(
            		Paths.get(GAParameters.getInterfaceDir()));
            executor = Executors.newSingleThreadExecutor();
            futureWatchers = executor.submit(ecl);
            executor.shutdown();
            
            ea = new EvolutionaryAlgorithm(ecl);
            ea.run();
        }
        catch (Throwable t)
        {
            if (ea != null)
            {
                ea.stopRun();
            }
            stopExternalCmdListener(ecl,executor,futureWatchers);
            DENOPTIMLogger.appLogger.log(Level.SEVERE, "Error occurred", t);
            t.printStackTrace(System.err);
            thrownExc = new DENOPTIMException("Error in DenoptimGA run.", t);
        }

        stopExternalCmdListener(ecl,executor,futureWatchers);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Stop the service that waits for instructions from the outside world.
     * @param ecl
     * @param executor
     * @param futureWatchers
     */
	private static void stopExternalCmdListener(ExternalCmdsListener ecl,
	        ExecutorService executor, Future<?> futureWatchers) 
	{
        if (executor != null)
        {
            try {
				executor.awaitTermination(2, TimeUnit.SECONDS);
				ecl.closeWatcher();
                futureWatchers.cancel(true);
                executor.awaitTermination(1, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				// we'll kill it anyway
			} catch (IOException e) {
				// we'll kill it anyway
			}
            executor.shutdownNow();
            executor = null;
        }
	}
    
//------------------------------------------------------------------------------        

}
