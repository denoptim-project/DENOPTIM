package denoptimga;

import java.util.logging.Level;

import exception.DENOPTIMException;
import logging.DENOPTIMLogger;
import utils.GenUtils;

/**
 *
 * @author Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no>
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
