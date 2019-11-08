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
        System.err.println("Usage: java -jar DenoptimGA.jar ConfigFile");
        System.exit(-1);
    }

//------------------------------------------------------------------------------    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        // TODO code application logic here
        if (args.length < 1)
        {
            printUsage();
        }

        String configFile = args[0];
        
        EvolutionaryAlgorithm evoGA;
        ParallelEvolutionaryAlgorithm pGA = null;
        try
        {
            GAParameters.readParameterFile(configFile);
            GAParameters.checkParameters();
            GAParameters.processParameters();
            GAParameters.printParameters();
            
            if (GAParameters.parallelizationScheme == 1)
            {
                System.err.println("Using synchronous parallelization scheme.");
                evoGA = new EvolutionaryAlgorithm();
                evoGA.runGA();
            }
            else
            {
                System.err.println("Using asynchronous parallelization scheme.");
                pGA = new ParallelEvolutionaryAlgorithm();
                pGA.runGA();
            }

        }
        catch (DENOPTIMException de)
        {
            if (pGA != null)
            {
                pGA.stopRun();
            }
            DENOPTIMLogger.appLogger.log(Level.SEVERE, "Error occured", de);
            GenUtils.printExceptionChain(de);
            System.exit(-1);
        }
        catch (Exception e)
        {
            DENOPTIMLogger.appLogger.log(Level.SEVERE, "Error occured", e);
            GenUtils.printExceptionChain(e);
            System.exit(-1);
        }

        // normal completion
        System.exit(0);
    }
    
//------------------------------------------------------------------------------        
}
