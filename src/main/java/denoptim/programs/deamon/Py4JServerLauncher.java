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

package denoptim.programs.deamon;

import java.io.File;

import denoptim.task.ProgramTask;

/**
 * A program that start a Py4J gateway server, which listens to calls from 
 * Python.
 * @author Marco Foscato
 */

public class Py4JServerLauncher extends ProgramTask
{

//------------------------------------------------------------------------------
    
    /**
     * Creates and configures the program task.
     * @param configFile the file containing the configuration parameters.
     * @param workDir the file system location from which to run the program.
     */
    public Py4JServerLauncher()
    {
        super(null, null);
    }
  
//------------------------------------------------------------------------------
    
    @Override
    public void runProgram() throws Throwable
    {
        //TODO
        System.out.println("TODO stat server");
    }
    
//------------------------------------------------------------------------------

    protected void handleThrowable()
    {
        super.handleThrowable();
    }
      
//------------------------------------------------------------------------------  

}
