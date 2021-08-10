/*
 *   DENOPTIM
 *   Copyright (C) 2019 Marco Foscato <marco.foscato@uib.no>
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

package fitnessrunner;

import java.util.ArrayList;
import java.util.logging.Level;

import denoptim.exception.DENOPTIMException;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.task.FitnessTask;
import denoptim.utils.GenUtils;
import denoptimga.DenoptimGA;
import fragspaceexplorer.FSEParameters;


/**
 * Stand-alone fitness provider main class. This class implements a main method 
 * that allows to run a fitness evaluation in a stand-alone. The configuration
 * of the fitness provider is given as a DENOPTIM params input file, in all 
 * equal to the parameters file used by {@link DenoptimGA} 
 * and {@link FitnessRunner}.
 * 
 * @author Marco Foscato
 */

public class FitnessRunner
{

//------------------------------------------------------------------------------

    /**
     * Prints the syntax to execute
     */

    public static void printUsage()
    {
        System.err.println("Usage: java -jar FitnessRunner.jar ConfigFile "
        		+ "[workDir]");
    }

//------------------------------------------------------------------------------ 
    
    /**
     * @param args the command line arguments
     * @throws DENOPTIMException 
     */

    public static void main(String[] args) throws DENOPTIMException
    {
        if (args.length < 1)
        {
            printUsage();
            throw new DENOPTIMException("Cannot run FitnessRunner. Need "
            		+ "at least one argument, i.e., the parameters file.");
        }
        
        FRParameters.resetParameters();
        
        String configFile = args[0];
        if (args.length > 1)
        {
            FRParameters.workDir = args[1];
        }
        
        FPRunner runner = null;
        try
        {
        	FRParameters.readParameterFile(configFile);
            FRParameters.checkParameters();
            FRParameters.processParameters();
            FRParameters.printParameters();
            
            runner = new FPRunner();
            runner.run();    
        }
        catch (Throwable t)
        {
    	    if (runner != null)
    	    {
    	        runner.stopRun();
    	    }
            DENOPTIMLogger.appLogger.log(Level.SEVERE, "Error occured", t);
            GenUtils.printExceptionChain(t);
            throw new DENOPTIMException("Error in FitnessRunner run.", t);
        }
        
        // normal completion: do NOT call System exit(0) as we might be calling
        // this main from another thread, which would be killed as well.
    }
    
//------------------------------------------------------------------------------        
}
