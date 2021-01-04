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
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.utils.TaskUtils;
import denoptimga.GAParameters;

/**
 * Task that runs any of the main methods in the denoptim project, such as 
 * DenoptimGA and FragSpaceExplorer from within the GUI.
 */

public abstract class GUIInvokedMainTask extends Task
{
	private String configFilePathName = "";

//------------------------------------------------------------------------------
    
    public GUIInvokedMainTask()
    {
    	super(TaskUtils.getUniqueTaskIndex());
    }

//------------------------------------------------------------------------------

    public void setConfigFile(File configFile)
    {
    	setConfigFile(configFile.getAbsolutePath());
    }
    
//------------------------------------------------------------------------------

    public void setConfigFile(String configFilePathName)
    {
    	this.configFilePathName = configFilePathName;
    }
    
//------------------------------------------------------------------------------

    @Override
    public Object call()
    {
    	String implName = this.getClass().getSimpleName();
    	//TODO log or del
    	System.out.println("Calling " + implName +" ("
    			+ " id="+id+", configFile="
    			+ configFilePathName + ", workSpace=" + workDir + ")");
    	
    	String[] args = new String[] {configFilePathName, workDir};
    	
    	try {
			mainCaller(args);
	    	System.out.println(implName + " id="+id+" done!");
		} catch (Throwable t) {
        	StringWriter sw = new StringWriter();
        	PrintWriter pw = new PrintWriter(sw);
        	t.printStackTrace(pw);
        	String errFile = workDir + SEP + "ERROR";
        	System.out.println("ERROR reported in "+errFile);
            try {
				DenoptimIO.writeData(errFile, sw.toString(), false);
	        	System.out.println(implName + " id="+id+" returned an error. "
	        			+ "See '" + errFile + "'");
			} catch (DENOPTIMException e) {
				t.printStackTrace();
	        	System.out.println(implName + " id="+id+" returned an error. "
	        			+ "Inspect the log before this line.");
			}
            // NB: the static logger should have been set by the main we called
            DENOPTIMLogger.appLogger.log(Level.SEVERE, "Error occured.");
		}
    	

    	if (notify)
    	{
    		StaticTaskManager.subtractDoneTask();
    	}
    	
		return null;
    }
    
//------------------------------------------------------------------------------ 

    protected abstract void mainCaller(String[] args) throws DENOPTIMException;
    
//------------------------------------------------------------------------------

}
