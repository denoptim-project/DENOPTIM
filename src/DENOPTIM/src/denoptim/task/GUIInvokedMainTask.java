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

import denoptim.exception.DENOPTIMException;
import denoptim.utils.TaskUtils;

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
    	//TODO log of del
    	System.out.println("Calling " + implName +" ("
    			+ " id="+id+", configFile="
    			+ configFilePathName + ", workSpace=" + workDir + ")");
    	
    	String[] args = new String[] {configFilePathName, workDir};
    	
    	try {
			mainCaller(args);
		} catch (DENOPTIMException e) {
			e.printStackTrace();
		}
    	
    	//TODO log of del
    	System.out.println(implName + " id="+id+" done!");
		return null;
    }
    
//------------------------------------------------------------------------------ 

    protected abstract void mainCaller(String[] args) throws DENOPTIMException;
    
//------------------------------------------------------------------------------

}
