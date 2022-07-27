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

package denoptim.main;


import org.apache.commons.cli.CommandLine;

import denoptim.main.Main.RunType;

/**
 * Represents the behavior of the program at start-up. This class is mostly 
 * meant to test the functionality of denoptim's main launcher.
 */
public class Behavior
{
    /**
     * The help message
     */
    protected String helpMsg = "";
    
    /**
     * The error message
     */
    protected String errorMsg = "";

    /**
     * A non-zero value means some error has occurred and the program will 
     * terminate.
     */
    protected int exitStatus = 0;
    
    /**
     * The type of run that is requested.
     */
    protected RunType runType = null;
    
    /**
     * The parsed command line arguments.
     */
    protected CommandLine cmd = null;
    
//------------------------------------------------------------------------------

    /**
     * Creates a behavior
     * @param runType the type of run.
     * @param cmd the command line arguments for the run.
     * @param exitStatus any non-zero corresponds to an error.
     * @param errorMsg the error message, if any.
     */
    public Behavior(RunType runType, CommandLine cmd, int exitStatus, 
            String helpMsg, String errorMsg)
    {
        this.helpMsg = helpMsg;
        this.errorMsg = errorMsg;
        this.exitStatus = exitStatus;
        this.runType = runType;
        this.cmd = cmd;
    }
    
//------------------------------------------------------------------------------

    /**
     * Creates a behavior.
     * @param runType the type of run.
     * @param params the arguments for the run.
     */
    public Behavior(RunType runType, CommandLine cmd)
    {
        this(runType, cmd, 0, null, null);
    }
    
//------------------------------------------------------------------------------
    
}
