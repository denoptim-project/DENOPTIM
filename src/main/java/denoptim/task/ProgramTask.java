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
import java.util.logging.Level;

import denoptim.exception.DENOPTIMException;
import denoptim.files.FileFormat;
import denoptim.files.FileUtils;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
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
    }
    
//------------------------------------------------------------------------------

    /**
     * This method redirects the callable functionality to an abstract method
     * to be specified by the implementations of this abstract class.
     */
    @Override
    public Object call()
    {
    	String implName = this.getClass().getSimpleName();
    	
    	//TODO log or del
    	System.out.println("Calling " + implName +" ("
    			+ " id="+id+", configFile="
    			+ configFilePathName + ", workSpace=" + workDir + ")");
    	
		try
        {
            runProgram();
            // This string is meant for the log of the GUI, i.e., the terminal
            System.out.println("Completed " + implName + " id="+id);
        } catch (Throwable t)
        {
            thrownExc = t;
            handleThrowable();
        }
		
    	if (notify)
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
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Method that can be called to create a text file with the error triggered 
     * by any {@link Throwable} that can be thrown by the execution of the 
     * program. The file names "ERROR" will be created in the working directory
     * specified to this program task.
     */
    public void printErrorToFile()
    {
        String implName = this.getClass().getSimpleName();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        thrownExc.printStackTrace(pw);
        String errFile = workDir + SEP + "ERROR";
        
        // This string is meant for the log of the GUI, i.e., the terminal
        System.out.println("ERROR reported in "+errFile);
        try {
            DenoptimIO.writeData(errFile, sw.toString(), false);
            System.out.println(implName + " id="+id+" returned an error. "
                    + "See '" + errFile + "'");
        } catch (DENOPTIMException e) {
            thrownExc.printStackTrace();
            System.out.println(implName + " id="+id+" returned an error. "
                    + "Inspect the log before this line.");
        }
        
        FileUtils.addToRecentFiles(errFile, FileFormat.TXT);
        
        // NB: the static logger should have been set by the main we called
        DENOPTIMLogger.appLogger.log(Level.SEVERE, "Error occured.");
    }
    
//------------------------------------------------------------------------------ 

    protected abstract void runProgram() throws Throwable;
    
//------------------------------------------------------------------------------

}
