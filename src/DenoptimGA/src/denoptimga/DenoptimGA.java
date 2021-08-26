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

package denoptimga;

import java.util.logging.Level;

import denoptim.exception.DENOPTIMException;
import denoptim.logging.DENOPTIMLogger;
import denoptim.utils.GenUtils;

/**
 *
 * @author Vishwesh Venkatraman
 */
public class DenoptimGA
{

//------------------------------------------------------------------------------

    public static void printUsage()
    {
        System.err.println("Usage: java -jar DenoptimGA.jar ConfigFile "
        		+ "[workDir]");
    }

//------------------------------------------------------------------------------    
    
    /**
     * @param args the command line arguments
     * @throws DENOPTIMException in any case of not-normal termination
     */
    public static void main(String[] args) throws DENOPTIMException
    {
        if (args.length < 1)
        {
            printUsage();
            throw new DENOPTIMException("Cannot run. Need at least one argument"
            		+ "to run DenoptimGA main method.");
        }
        
    	//needed by static parameters, and in case of subsequent runs in the same JVM
    	GAParameters.resetParameters(); 

        String configFile = args[0];
        if (args.length > 1)
        {
        	GAParameters.dataDir = args[1];
        }

        EvolutionaryAlgorithm ea = null;
        try
        {	
            GAParameters.readParameterFile(configFile);
            GAParameters.checkParameters();
            GAParameters.processParameters();
            GAParameters.printParameters();
            
            ea = new EvolutionaryAlgorithm();
            ea.run();
        }
        catch (Throwable t)
        {
            if (ea != null)
                ea.stopRun();
            
            DENOPTIMLogger.appLogger.log(Level.SEVERE, "Error occurred", t);
            GenUtils.printExceptionChain(t);
            
            throw new DENOPTIMException("Error in DenoptimGA run.", t);
        }

        // normal completion: do NOT call System exit(0) as we might be calling
        // this main from another thread, which would be killed as well.
    }
    
//------------------------------------------------------------------------------        

}
