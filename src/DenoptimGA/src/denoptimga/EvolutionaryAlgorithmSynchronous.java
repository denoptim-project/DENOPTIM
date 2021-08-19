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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.logging.Level;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.math3.random.MersenneTwister;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.APClass;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.Candidate;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.task.Task;
import denoptim.task.TasksBatchManager;
import denoptim.utils.GenUtils;
import denoptim.utils.GraphUtils;
import denoptim.utils.RandomUtils;
import denoptimga.EAUtils.CandidateSource;



/**
 * DENOPTIM's synchronous evolutionary algorithm. Compared to the 
 * {@link EvolutionaryAlgorithmAsynchronous}, this <i>synchronous</i> algorithm
 * waits for completion of all the attempts to make new candidates, and to 
 * complete a generation, before starting to build more candidates.
 * <p>With this parallelisation scheme there are no candidates that are in 
 * surplus when a generation is finished, and all candidates initiated at
 * a given generation can only become part of that very generation.</p>
 * <p>This, however, is only possible when accepting a somewhat inefficient
 * use of the resources. To illustrate: consider the moment when the are 
 * fewer free seats in the population
 * , say N,
 * than available computational resources dedicated to offspring evaluation 
 * , say M (where M>N). The algorithm will generate N candidates, and M-N 
 * thread seats will remain idle.</p>
 * 
 *  
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */


public class EvolutionaryAlgorithmSynchronous
{

    private final String NL = System.getProperty("line.separator");

//------------------------------------------------------------------------------
    
    public void run() throws DENOPTIMException
    {
        StopWatch watch = new StopWatch();
        watch.start();
        
        // Create initial population of candidates
        EAUtils.createFolderForGeneration(0);
        ArrayList<Candidate> population = EAUtils.importInitialPopulation();
        initializePopulation(population);
        EAUtils.outputPopulationDetails(population, 
                EAUtils.getPathNameToGenerationDetailsFile(0));
        
        // Ensure that there is some variability in fitness values
        double sdev = EAUtils.getPopulationSD(population);
        if (sdev < 0.000001) ///TODO: use a parameter to replace hard-coded threshold?
        {
            String msg = "Fitness values have negligible standard deviation (STDDEV="
                            + String.format("%.6f", sdev) + "). Abbandoning "
                                    + "evolutionary algorithm.";
            DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
            cleanup(population);
            return;
        }

        // Start evolution cycles, i.e., generations
        int numStag = 0, genId = 1;
        while (genId <= GAParameters.getNumberOfGenerations())
        {
            DENOPTIMLogger.appLogger.log(Level.INFO,"Starting Generation {0}"
                    + NL, genId);

            String txt = "No change";
            if (!evolvePopulation(population, genId))
            {
                numStag++;
            }
            else
            {
                numStag = 0;
                txt = "New members introduced";
            }
            DENOPTIMLogger.appLogger.log(Level.INFO,txt + " in Generation {0}" 
                    + NL, genId);
            EAUtils.outputPopulationDetails(population, 
                    EAUtils.getPathNameToGenerationDetailsFile(genId));
            DENOPTIMLogger.appLogger.log(
                    Level.INFO,"Generation {0}" + " completed" + NL
                            + "----------------------------------------"
                            + "----------------------------------------" + NL,
                            genId);

            if (numStag >= GAParameters.getNumberOfConvergenceGenerations())
            {
                DENOPTIMLogger.appLogger.log(Level.WARNING, 
                        "No change in population over {0} iterations. "
                        + "Stopping EA." + NL, numStag);
                break;
            }

            genId++;
        }

        // Sort the population and trim it to desired size
        Collections.sort(population, Collections.reverseOrder());
        if (GAParameters.getReplacementStrategy() == 1)
        {
            int k = population.size();
            population.subList(GAParameters.getPopulationSize(), k).clear();
        }

        // And write final results
        EAUtils.outputFinalResults(population);
        
        // Termination
        cleanup(population);
        watch.stop();
        DENOPTIMLogger.appLogger.log(Level.INFO, "Overall time: {0}." + NL,
                watch.toString());
        DENOPTIMLogger.appLogger.info("DENOPTIM EA run completed." + NL);
    }

//------------------------------------------------------------------------------

    /**
     * Fills up the population with candidates build from scratch. 
     * @param population the collection of population members. This is where 
     * pre-existing and newly generated population members will be collected.
     * @throws DENOPTIMException
     */

    private void initializePopulation(ArrayList<Candidate> population) 
            throws DENOPTIMException
    {
        Collections.sort(population, Collections.reverseOrder());
        if (GAParameters.getReplacementStrategy() == 1 && 
                population.size() > GAParameters.getPopulationSize())
        {
            int k = population.size();
            for (int l=GAParameters.getPopulationSize(); l<k; l++)
            {
                population.get(l).cleanup();
            }
            population.subList(GAParameters.getPopulationSize(),k)
                .clear();
        }
        if (population.size() == GAParameters.getPopulationSize())
        {
            return;
        }
        
        Monitor mnt = new Monitor("Generation 0");
        
        // Loop creation of candidates until we have created enough new valid 
        // candidates or we have reached the max number of attempts.
        int i=0;
        ArrayList<Task> tasks = new ArrayList<>();
        while (i < GAParameters.getPopulationSize() *
                GAParameters.getMaxTriesFactor()) 
        {
            i++;
            //TODO checking for exceptions?
            
            if (population.size() >= GAParameters.getPopulationSize())
            {
                break;
            }
            
            Candidate candidate = EAUtils.buildCandidateFromScratch(mnt);
              
            if (candidate == null)
                continue;
           
            Task task = new OffspringEvaluationTask(candidate, 
                    EAUtils.getPathNameToGenerationFolder(0), population, 
                    mnt, GAParameters.getUIDFileOut());
            
            tasks.add(task);
            if (tasks.size() >= Math.abs(
                    population.size() - GAParameters.getPopulationSize()))
            {
                //TODO-GG
                System.out.println("INI: Submitting Batch of " + tasks.size());
                
                // Now we have as many tasks as are needed to fill up the 
                // population. Therefore we can run the execution service.
                // TasksBatchManager takes the collection of tasks and runs
                // them in batches of N, where N is given by the
                // second argument.
                TasksBatchManager.executeTasks(tasks,
                        GAParameters.getNumberOfCPU());
                tasks.clear();
            }
        }
        
        mnt.printSummary();

        if (i >= (GAParameters.getPopulationSize() * 
                GAParameters.getMaxTriesFactor()))
        {
            DENOPTIMLogger.appLogger.log(Level.SEVERE,
                    "Unable to initialize molecules in {0} attempts."+NL, i);

            throw new DENOPTIMException("Unable to initialize molecules in " +
                            i + " attempts.");
        }

        // NB: this does not remove any item from the list
        population.trimToSize();
        
        Collections.sort(population, Collections.reverseOrder());
    }

//------------------------------------------------------------------------------

    /**
     * Generates a given number of new candidate items and adds them to the 
     * current population. One run of this method corresponds to a generation.
     * @param population the list of items to be evolved.
     * @param genId the number identifying this generation in the history of 
     * the population.
     * @throws DENOPTIMException
     */
    private boolean evolvePopulation(ArrayList<Candidate> population, 
            int genId) throws DENOPTIMException
    {
        EAUtils.createFolderForGeneration(genId);
        
        // Take a snapshot of the initial population
        ArrayList<Candidate> clone_popln;
        synchronized (population)
        {
            clone_popln = new ArrayList<Candidate>();
            for (Candidate m : population)
            {
                clone_popln.add(m.clone());
            }
        }
        ArrayList<String> initUIDs = EAUtils.getUniqueIdentifiers(population);
        
        int newPopSize = GAParameters.getNumberOfChildren() + population.size();
        
        int i=0;
        ArrayList<Task> tasks = new ArrayList<>();
        Monitor mnt = new Monitor("Generation " + genId);
        while (i < GAParameters.getPopulationSize() *
                GAParameters.getMaxTriesFactor()) 
        {
            i++;
            
            //TODO checking for exceptions?
            
            if (population.size() >= newPopSize)
            {
                break;
            }
            
            Candidate candidate = null;
            switch (EAUtils.chooseGenerationMethos())
            {
                case CROSSOVER:
                {
                    candidate = EAUtils.buildCandidateByXOver(clone_popln, mnt);
                    if (candidate == null)
                        continue;
                    break;
                }
                    
                case MUTATION:
                {
                    candidate = EAUtils.buildCandidateByMutation(clone_popln, 
                            mnt);
                    if (candidate == null)
                        continue;
                    break;
                }
                    
                case CONSTRUCTION:
                {
                    candidate = EAUtils.buildCandidateFromScratch(mnt);
                    if (candidate == null)
                        continue;
                    break;
                }
            }
            
            if (candidate == null)
                continue;

            Task task = new OffspringEvaluationTask(candidate, 
                    EAUtils.getPathNameToGenerationFolder(genId), 
                    population, mnt, GAParameters.getUIDFileOut());
            
            tasks.add(task);
            
            if (tasks.size() >= Math.abs(population.size() - newPopSize))
            {
                //TODO-GG
                System.out.println("EVO: Submitting Batch of " + tasks.size());
                
                // Now we have as many tasks as are needed to fill up the 
                // population. Therefore we can run the execution service.
                // TasksBatchManager takes the collection of tasks and runs
                // them in batches of N, where N is given by the
                // second argument.
                TasksBatchManager.executeTasks(tasks,
                        GAParameters.getNumberOfCPU());
                tasks.clear();
            }
        }
        
        mnt.printSummary();

        // sort the population
        Collections.sort(population, Collections.reverseOrder());

        if (GAParameters.getReplacementStrategy() == 1)
        {

            int k = population.size();

            // trim the population to the desired size
            for (int l=GAParameters.getPopulationSize(); l<k; l++)
            {
                population.get(l).cleanup();
            }
            
            population.subList(GAParameters.getPopulationSize(), k).clear();
        }
        
        cleanup(clone_popln);
        tasks.clear();

        // check if the new population contains a molecule from the children
        // produced. If yes, return true
        boolean updated = false;
        
        for (Candidate mol : population)
        {
            if (!initUIDs.contains(mol.getUID()))
            {
                updated = true;
                break;
            }
        }
        
        initUIDs.clear();
        return updated;
    }

//------------------------------------------------------------------------------
    
    private void cleanup(ArrayList<Candidate> popln)
    {
        for (Candidate mol:popln)
            mol.cleanup();
        popln.clear();
    }
    
//------------------------------------------------------------------------------    

}
