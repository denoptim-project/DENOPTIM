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

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import denoptim.constants.DENOPTIMConstants;
import denoptim.main.Main.RunType;

public class CLIOptions extends Options
{
    /**
     * Version ID
     */
    private static final long serialVersionUID = 3L;
    
    /**
     * Option requesting the printing of the help message.
     */
    public static Option help;
    
    /**
     * Option requesting only the printing of the version.
     */
    public static Option version;
    
    /**
     * Option controlling the type of run.
     */
    public static Option run;
    
    /**
     * The only, static instance of this class
     */
    private static final CLIOptions instance = new CLIOptions();
    
//------------------------------------------------------------------------------    
    
    private CLIOptions() 
    {
        help = new Option("h","help",false, "Print help message.");
        help.setRequired(false);
        this.addOption(help);
        
        run = new Option("r","run",true, "Request a specific type of "
                + "run. Choose among:" + DENOPTIMConstants.EOL
                + RunType.getRunTypesForUser());
        run.setRequired(false);
        this.addOption(run);
        
        version = new Option("v","version",false, "Print denoptim version.");
        version.setRequired(false);
        this.addOption(version);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Gets the singleton instance of this class.
     * @return
     */
    public static CLIOptions getInstance()
    {
        return instance;
    }
    
//------------------------------------------------------------------------------
    
}
