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

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import denoptim.exception.DENOPTIMException;
import denoptim.files.FileFormat;
import denoptim.files.FileUtils;
import denoptim.io.DenoptimIO;
import denoptim.logging.StaticLogger;
import denoptim.utils.TaskUtils;

/**
 * Task structure for any of the main programs in the denoptim project, such as 
 * genetic algorithm and combinatorial explored of fragment spaces. 
 * Any implementation of this class must define a {@link #runProgram()} method
 * that runs the actual program implementation.
 */

public abstract class ProgramTask extends Task
{
    
    /**
     * File containing configuration parameters for the program task.
     */
    protected File configFilePathName;
    
    /**
     * Identifier of this program's logger
     */
    protected String loggerIdentifier = "none";
    
//------------------------------------------------------------------------------
    
    /**
     * Creates and configures the program task.
     * @param configFile the file containing the configuration parameters.
     * @param workDir the file system location from which to run the program.
     */
    public ProgramTask(File configFile, File workDir)
    {
        super(TaskUtils.getUniqueTaskIndex());
        this.configFilePathName = configFile;
        this.workDir = workDir;
        loggerIdentifier = this.getClass().getSimpleName() + "-" + id;
    }
    
//------------------------------------------------------------------------------

    /**
     * This method redirects the callable functionality to an abstract method
     * (namely {@link ProgramTask#runProgram()}) to be specified by the 
     * implementations of this abstract class.
     */
    @Override
    public Object call()
    {	
    	StaticLogger.appLogger.log(Level.INFO, "Starting " + loggerIdentifier 
    	        + " (input="
    			+ configFilePathName + ", workSpace=" + workDir + ")");
		try
        {
            runProgram();
            StaticLogger.appLogger.log(Level.INFO, "Completed " 
                    + loggerIdentifier);
        } catch (Throwable t)
        {
            thrownExc = t;
            handleThrowable();
        }
		
    	if (notifyGlobalTaskManager)
    	{
    		StaticTaskManager.subtractDoneTask();
    	}
    	
		return null;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Method to handle any {@link Throwable} originated from the 
     * {@link #runProgram()} method. This method can be overwritten to alter the
     * behavior in case of specific needs.
     */
    protected void handleThrowable()
    {
        printErrorToFile();
        stopLogger();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Stops the program-specific logger and releases the lock file on the 
     * logfile.
     */
    protected void stopLogger()
    {
        // The logger is garbage-collected once we leave this thread, but we 
        // must stop the file handler gracefully.
        Logger logger = Logger.getLogger(loggerIdentifier);
        for (Handler h : logger.getHandlers())
            h.close();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Method that can be called to create a text file with the error triggered 
     * by any {@link Throwable} that can be thrown by the execution of the 
     * program. The file names "ERROR" will be created in the working directory
     * specified to this {@link ProgramTask}.
     */
    public void printErrorToFile()
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        thrownExc.printStackTrace(pw);
        String errFile = workDir + SEP + "ERROR";
        
        try {
            DenoptimIO.writeData(errFile, sw.toString(), false);
            StaticLogger.appLogger.log(Level.SEVERE, 
                    "ERROR occurred in " + loggerIdentifier + ". "
                    + "Details in "+errFile);
        } catch (DENOPTIMException e) {
            StaticLogger.appLogger.log(Level.SEVERE, 
                    "ERROR occurred in " + loggerIdentifier + ". Details are "
                            + "reported here since we could not write to file '"
                            + errFile + "'. ");
            thrownExc.printStackTrace();
        }
        FileUtils.addToRecentFiles(errFile, FileFormat.TXT);
    }
    
//------------------------------------------------------------------------------ 

    protected abstract void runProgram() throws Throwable;
    
//------------------------------------------------------------------------------

}
