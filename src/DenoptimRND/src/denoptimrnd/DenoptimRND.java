package denoptimrnd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.logging.Level;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.StopWatch;

import constants.DENOPTIMConstants;
import io.DenoptimIO;
import molecule.DENOPTIMGraph;
import molecule.DENOPTIMMolecule;
import utils.GenUtils;
import utils.EvaluatedGraphsCollection;
import utils.GraphUtils;
import utils.TaskUtils;
import fragspace.FragmentSpace;
import fragspace.FragmentSpaceParameters;
import utils.RandomUtils;
import exception.DENOPTIMException;
import logging.DENOPTIMLogger;

/**
 * Evolve a population using only random selection of new graphs.
 * The new graphs can only be taken from a collection of previously evaluated
 * graphs.
 *
 * @author Marco Foscato
 */

public class DenoptimRND
{

    private static final String fsep = System.getProperty("file.separator");

//------------------------------------------------------------------------------

    public static void printUsage()
    {
        System.err.println("Usage: java -jar DenoptimRND.jar ConfigFile");
        System.exit(-1);
    }

//------------------------------------------------------------------------------    
    
    /**
     * Main program 
     * @param args the command line arguments
     */

    public static void main(String[] args)
    {
        if (args.length != 1)
        {
            printUsage();
        }

        String configFile = args[0];
        
        try
        {
            RNDParameters.readParameterFile(configFile);
            RNDParameters.checkParameters();
            RNDParameters.processParameters();
            RNDParameters.printParameters();

            run();
        }
        catch (DENOPTIMException de)
        {
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
    /**
     * Runs the evolutionary experiment
     */

    private static void run() throws DENOPTIMException
    {
        StopWatch watch = new StopWatch();
             watch.start();
        
        StringBuilder sb = new StringBuilder(32);

        int ndigits = String.valueOf(
                               RNDParameters.getNumberOfGenerations()).length();
        sb.append(RNDParameters.getDataDirectory()).append(fsep).append("Gen")
                        .append(GenUtils.getPaddedString(ndigits, 0));
        String genDir = sb.toString();
        sb.setLength(0);
        // create the directory for the current generation
        DenoptimIO.createDirectory(genDir);

        // create the population

        // placeholder for the population members
        ArrayList<DENOPTIMMolecule> molPopulation = new ArrayList<>();
        
	// Create first population, if not already complete
        initializePopulation(molPopulation, genDir);

	// Store summary for first population
        sb.append(genDir).append(fsep).append("Gen")
                   .append(GenUtils.getPaddedString(ndigits, 0)).append(".txt");
        String genOutfile = sb.toString();
        sb.setLength(0);
        RNDEAUtils.outputPopulationDetails(molPopulation, genOutfile);
        
        // increment this value if the population is stagnating over a number of
        // generations
        int numStag = 0, curGen = 1;

        while (curGen <= RNDParameters.getNumberOfGenerations())
        {
            DENOPTIMLogger.appLogger.log(Level.INFO,
                                        "Starting Generation {0}\n", curGen);

            sb.append(RNDParameters.getDataDirectory()).append(fsep)
	       .append("Gen").append(GenUtils.getPaddedString(ndigits, curGen));

            // create a directory for the current generation
            genDir = sb.toString();
            sb.setLength(0);
            DenoptimIO.createDirectory(genDir);

            // create a new generation
            // update the population, by replacing weakest members
            // evolve returns true if members in the population were replaced
            // changes to the population should ideally change the variance

            if (!evolvePopulation(molPopulation, genDir))
            {
                numStag++;
                DENOPTIMLogger.appLogger.log(Level.INFO,
                    "No change in population in Generation {0}\n", curGen);
            }
            else
            {
                numStag = 0;
                DENOPTIMLogger.appLogger.log(Level.INFO,
                    "New molecules introduced in Generation {0}\n", curGen);
            }


            sb.append(genDir).append(fsep).append("Gen")
              .append(GenUtils.getPaddedString(ndigits, curGen)).append(".txt");

            // dump population details to file
            genOutfile = sb.toString();
            sb.setLength(0);

            RNDEAUtils.outputPopulationDetails(molPopulation, genOutfile);
            
            DENOPTIMLogger.appLogger.log(Level.INFO,"Generation {0}" 
		+ " completed\n"
                + "----------------------------------------"
                + "----------------------------------------\n", curGen);

            // check for stagnation
            if (numStag >= RNDParameters.getNumberOfConvergenceGenerations())
            {
                // write a log message
                DENOPTIMLogger.appLogger.log(Level.WARNING,
                "No change in population over {0} iterations. Stopping RND. \n",
								       numStag);
                break;
            }

            curGen++;
        }

        genDir = RNDParameters.getDataDirectory() + fsep + "Final";
        DenoptimIO.createDirectory(genDir);

        RNDEAUtils.outputFinalResults(molPopulation, genDir);
        
        cleanup(molPopulation);

        watch.stop();

        DENOPTIMLogger.appLogger.log(Level.INFO, "Overall time: {0}.\n",
                                                            watch.toString());

        DENOPTIMLogger.appLogger.info("DENOPTIM RND run completed.\n");
    }

//------------------------------------------------------------------------------

    /**
     * Generate children that are not already among the current population
     * @param molPopulation
     * @param genDir
     * @return <code>true</code> if new valid molecules are produced such that
     * the population is updated with fitter structures
     * @throws DENOPTIMException
     */

    private static boolean evolvePopulation
	              (ArrayList<DENOPTIMMolecule> molPopulation, String genDir)
                                                        throws DENOPTIMException
    {
	ArrayList<String> oldUIDs = new ArrayList<String>();
        for (DENOPTIMMolecule mol : molPopulation)
        {
            oldUIDs.add(mol.getMoleculeUID());
        }

        int n = RNDParameters.getNumberOfChildren() + molPopulation.size();

        while (molPopulation.size() < n)
        {
            DENOPTIMMolecule dm = EvaluatedGraphsCollection.getRandomEntry(1);
            molPopulation.add(dm);
            if (molPopulation.size() == n)
            {
                break;
            }
        }

        StringBuilder sb = new StringBuilder(256);
	sb.append("Added " + (molPopulation.size()-oldUIDs.size()) 
						 + " randomly selected graphs");
        DENOPTIMLogger.appLogger.info(sb.toString());

        // sort the population
        Collections.sort(molPopulation, Collections.reverseOrder());

        if (RNDParameters.getReplacementStrategy() == 1)
        {

            int k = molPopulation.size();

            // trim the population to the desired size
            for (int l=RNDParameters.getPopulationSize(); l<k; l++)
            {
                molPopulation.get(l).cleanup();
            }
            
            molPopulation.subList(RNDParameters.getPopulationSize(), k).clear();
        }
        
        // check if the new population contains a molecule from the children
        // produced. If yes, return true
        boolean updated = false;
        
        for (DENOPTIMMolecule mol : molPopulation)
        {
            if (!oldUIDs.contains(mol.getMoleculeUID()))
            {
                updated = true;
                break;
            }
        }
        
        oldUIDs.clear();
        return updated;
    }

//------------------------------------------------------------------------------

    /**
     * creates a population of molecules
     * In creating the population of specified size (say n).
     * If the desired population size is not achieved, the
     * program throws and exception.
     * @param molPopulation the population to be initialized
     * @param genDir output directory associated with the current population
     * @throws DENOPTIMException
     */

    private static void initializePopulation
		      (ArrayList<DENOPTIMMolecule> molPopulation, String genDir)
						        throws DENOPTIMException
    {
        final int MAX_TRIES = RNDParameters.getPopulationSize() * 
                                             RNDParameters.getMaxTriesFactor();
        int t = 0;

        int npop = RNDParameters.getPopulationSize();

	// First check if the previously loaded population has right size...
        if (molPopulation.size() == npop)
        {
            Collections.sort(molPopulation, Collections.reverseOrder());
            return;
        }
        // ...or is too large
        if (molPopulation.size() > npop)
        {
            Collections.sort(molPopulation, Collections.reverseOrder());
            int k = molPopulation.size();
            
            // trim the population to the desired size
            for (int l=RNDParameters.getPopulationSize(); l<k; l++)
            {
                molPopulation.get(l).cleanup();
            }
             // trim the population to the desired size
            molPopulation.subList(npop, k).clear();
        }

	// Then, try to append new members to the population
        while (molPopulation.size() < RNDParameters.getPopulationSize())
        {
	    // Prevent never-ending loop
            if (t == MAX_TRIES)
	    {
                break;
	    }
	    t+=1;

            // Pick a graph from the list of known (and evaluated) graphs
            DENOPTIMMolecule dm = EvaluatedGraphsCollection.getRandomEntry(1);
            molPopulation.add(dm);

	    // Stop appending to the population
            if (molPopulation.size() == RNDParameters.getPopulationSize())
	    {
                break;
            }
        }

        if (t == MAX_TRIES)
        {
            DENOPTIMLogger.appLogger.log(Level.SEVERE,
                    "Unable to initialize molecules in {0} attempts.\n", t);

            throw new DENOPTIMException("Unable to initialize molecules in " +
                            t + " attempts.");
        }

        molPopulation.trimToSize();

        // sort population by fitness
        Collections.sort(molPopulation, Collections.reverseOrder());
    }

//------------------------------------------------------------------------------
    
    private static void cleanup(ArrayList<DENOPTIMMolecule> popln)
    {
        for (DENOPTIMMolecule mol:popln)
            mol.cleanup();
        popln.clear();
    }
    
//------------------------------------------------------------------------------

}
