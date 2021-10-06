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

package denoptimrnd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.math3.random.MersenneTwister;

import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.Candidate;
import denoptim.utils.GenUtils;
import denoptim.utils.RandomUtils;

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

    /**
     * Colection of DENOPTIM representations of
     * graphs with fitness.
     */
    private static ArrayList<Candidate> allEvaluatedGraphs =
                                              new ArrayList<Candidate>();

    /**
     * Comment used to flag previously used graphs
     */
    private static String usedFlag = "ALREADY_USED";

    /**
     * Number of used graphs
     */
    private static int numUsed = 0;

    /**
     * Number of available graphs
     */
    private static int numAvail = 0;

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
	
	    // Load Known graphs
	    // WARNING! This burns-up the memory
	    addEvaluatedGraphs(RNDEAUtils.readGraphsWithFitnessFromFile(
					     RNDParameters.getAllGraphsFile()));

	    // Run evolution
            run();
        }
        catch (DENOPTIMException de)
        {
            DENOPTIMLogger.appLogger.log(Level.SEVERE, "Error occured", de);
            de.printStackTrace(System.err);
            System.exit(-1);
        }
        catch (Exception e)
        {
            DENOPTIMLogger.appLogger.log(Level.SEVERE, "Error occured", e);
            e.printStackTrace(System.err);
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
        ArrayList<Candidate> molPopulation = new ArrayList<>();
        
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
	              (ArrayList<Candidate> molPopulation, String genDir)
                                                        throws DENOPTIMException
    {
	ArrayList<String> oldUIDs = new ArrayList<String>();
        for (Candidate mol : molPopulation)
        {
            oldUIDs.add(mol.getUID());
        }

        int n = RNDParameters.getNumberOfChildren() + molPopulation.size();

        while (molPopulation.size() < n)
        {
            Candidate dm = getRandomEntry();
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
        
        for (Candidate mol : molPopulation)
        {
            if (!oldUIDs.contains(mol.getUID()))
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
     * of specified size.
     * If the desired population size is not achieved, the
     * program throws and exception.
     * @param molPopulation the population to be initialized
     * @param genDir output directory associated with the current population
     * @throws DENOPTIMException
     */

    private static void initializePopulation
		      (ArrayList<Candidate> molPopulation, String genDir)
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
            Candidate dm = getRandomEntry();
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
    
    private static void cleanup(ArrayList<Candidate> popln)
    {
        for (Candidate mol:popln)
            mol.cleanup();
        popln.clear();
    }

//------------------------------------------------------------------------------

    /**
     * Append all entries to the collection
     * @param dmols the entities to append
     */

    private static void addEvaluatedGraphs(ArrayList<Candidate> dmols)
    {
	allEvaluatedGraphs.addAll(dmols);
	numAvail = allEvaluatedGraphs.size();
    }

//------------------------------------------------------------------------------

    /**
     * Select an evaluated graph that was not selected before
     */

    private static Candidate getRandomEntry() throws DENOPTIMException
    {
        Candidate newOne = null;

        if (numUsed == numAvail)
        {
            throw new DENOPTIMException("ERROR! Not enough graphs in the "
                                           + "collection of evaluated graphs.");
        }
	
        while (numUsed < numAvail)
        {
            int rndNum = RandomUtils.nextInt(numAvail);
            String str = allEvaluatedGraphs.get(rndNum).getComments();
            if (str==null || (str!=null && !str.equals(usedFlag)))
            {
                newOne = allEvaluatedGraphs.get(rndNum);
                allEvaluatedGraphs.get(rndNum).setComments(usedFlag);
                numUsed+=1;
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("Selecting graph %d ",rndNum));
                sb.append(newOne.getUID());
                sb.append(String.format(" (Usage %d/%d)",numUsed,numAvail));
                DENOPTIMLogger.appLogger.log(Level.INFO,sb.toString());
                break;
            }
        }
        return newOne;
    }
    
//------------------------------------------------------------------------------

}
