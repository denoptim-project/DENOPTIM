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
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.StopWatch;
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
import denoptim.task.FitnessTask;
import denoptim.task.Task;
import denoptim.utils.GenUtils;
import denoptim.utils.GraphUtils;
import denoptim.utils.RandomUtils;
import denoptim.utils.TaskUtils;

/**
 * DENOPTIM's asynchronous evolutionary algorithm. Asynchronous means that there is
 * no waiting for the completion of a generation before starting the generation 
 * on a new offspring, and that the generation owning an offspring is decided by 
 * when the fitness evaluation of that offspring is concluded. It is thus possible 
 * for an offspring to be designed as a child of generation <i>N</i> and become
 * a member of generation <i>N+M</i>. In a synchronous algorithms 
 * (see {@link EvolutionaryAlgorithmSynchronous}, it is always guaranteed that <i>M = 1</i>.
 * In the asynchronous algorithm, <i>M</i> is only guaranteed to be greater than 0.
 * 
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class EvolutionaryAlgorithmAsynchronous
{

    //TODO-V3 change to Executor and initialize based on GAParameters.parallelizationScheme

    final List<Future<Object>> futures;
    final ArrayList<FitnessTask> submitted;
    
    final ThreadPoolExecutor tcons;

    private Throwable ex;
   
    private final String NL = System.getProperty("line.separator");
    
//------------------------------------------------------------------------------
 
    public EvolutionaryAlgorithmAsynchronous()
    {
        futures = new ArrayList<>();
        submitted = new ArrayList<>();

        tcons = new ThreadPoolExecutor(GAParameters.getNumberOfCPU(),
                                GAParameters.getNumberOfCPU(), 0L,
                                TimeUnit.MILLISECONDS,
                                new ArrayBlockingQueue<Runnable>(1));

        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                tcons.shutdown(); // Disable new tasks from being submitted
                try
                {
                    // Wait a while for existing tasks to terminate
                    if (!tcons.awaitTermination(30, TimeUnit.SECONDS))
                    {
                        tcons.shutdownNow(); // Cancel currently executing tasks
                    }
                    if (!tcons.awaitTermination(60, TimeUnit.SECONDS))
                    {
                        // pool didn't terminate after the second try
                    }
                }
                catch (InterruptedException ie)
                {
                    cleanup(tcons, futures, submitted);
                    // (Re-)Cancel if current thread also interrupted
                    tcons.shutdownNow();
                    // Preserve interrupt status
                    Thread.currentThread().interrupt();
                }
            }
        });

        // by default the ThreadPoolExecutor will throw an exception
        tcons.setRejectedExecutionHandler(new RejectedExecutionHandler()
        {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor)
            {
                try
                {
                    // this will block if the queue is full
                    executor.getQueue().put(r);
                }
                catch (InterruptedException ex)
                {
                    //Logger.getLogger(EvolutionaryAlgorithm.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

//------------------------------------------------------------------------------

    public void stopRun()
    {
        cleanup(tcons, futures, submitted);
        tcons.shutdown();
    }

//------------------------------------------------------------------------------

    private boolean checkForException()
    {
        boolean hasprobs = false;
        for (FitnessTask tsk : submitted)
        {
            if (tsk.foundException())
            {
                hasprobs = true;
                DENOPTIMLogger.appLogger.log(Level.SEVERE, "problems in " 
                + tsk.toString() + ". ErrorMessage: '" + tsk.getErrorMessage() 
                + "'. ExceptionInTask: "+tsk.getException());
                ex = tsk.getException().getCause();
                break;
            }
        }

        return hasprobs;
    }

//------------------------------------------------------------------------------

    public void run() throws DENOPTIMException
    {
        StopWatch watch = new StopWatch();
        watch.start();
        tcons.prestartAllCoreThreads();
        
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
                    + NL,genId);

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

        // Start shutdown operations
        tcons.shutdown();
        try
        {
            // wait a bit for pending tasks to finish
            while (!tcons.awaitTermination(5, TimeUnit.SECONDS))
            {
                // do nothing
            }
        }
        catch (InterruptedException ex)
        {
            DENOPTIMLogger.appLogger.log(Level.SEVERE, null, ex);
            throw new DENOPTIMException (ex);
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

    private void initializePopulation(ArrayList<Candidate> population) 
            throws DENOPTIMException
    {   
        // Deal with existing initial population members
        synchronized (population)
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
        }
        if (population.size() == GAParameters.getPopulationSize())
        {
            return;
        }
        
        Monitor mnt = new Monitor("Initial population");

        // Loop creation of candidates until we have created enough new valid 
        // candidates or we have reached the max number of attempts.
        int i = 0;
        try
        {
            while (i < GAParameters.getPopulationSize() *
                    GAParameters.getMaxTriesFactor()) 
            {
                i++;
                
                if (checkForException())
                {
                    stopRun();
                    throw new DENOPTIMException("Errors found during sub tasks "
                            + "execution.", ex);
                }

                synchronized (population)
                {
                    if (population.size() >= GAParameters.getPopulationSize())
                    {
                        break;
                    }
                }

                Candidate candidate = EAUtils.buildCandidateFromScratch(mnt);
                
                if (candidate == null)
                    continue;
                
                OffspringEvaluationTask task = new OffspringEvaluationTask(
                        candidate, EAUtils.getPathNameToGenerationFolder(0), 
                        population, mnt, GAParameters.getUIDFileOut());
                
                submitted.add(task);
                futures.add(tcons.submit(task));
            }
        }
        catch (DENOPTIMException dex)
        {
            cleanup(tcons, futures, submitted);
            tcons.shutdown();
            throw dex;
        }
        catch (Exception ex)
        {
            cleanup(tcons, futures, submitted);
            tcons.shutdown();
            throw new DENOPTIMException(ex);
        }
        
        mnt.printSummary();

        if (i >= (GAParameters.getPopulationSize() * 
                GAParameters.getMaxTriesFactor()))
        {
            cleanupCompleted(tcons, futures, submitted);
            stopRun();
            DENOPTIMLogger.appLogger.log(Level.SEVERE,
                    "Unable to initialize molecules in {0} attempts."+NL, i);

            throw new DENOPTIMException("Unable to initialize molecules in " +
                            i + " attempts.");
        }

        synchronized (population)
        {
            // NB: this does not remove any item from the list
            population.trimToSize();
            
            Collections.sort(population, Collections.reverseOrder());
        }
    }

//------------------------------------------------------------------------------

    /**
     * Generate children that are not already among the current population members.
     * @param population the list of items to be evolved.
     * @param genId the generation number.
     * @return <code>true</code> if new valid molecules are produced such that
     * the population is updated with fitter structures
     * @throws DENOPTIMException
     */
    private boolean evolvePopulation(ArrayList<Candidate> population, int genId) 
            throws DENOPTIMException
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
        ArrayList<String> initUIDs = EAUtils.getUniqueIdentifiers(clone_popln);

        int newPopSize = GAParameters.getNumberOfChildren() + clone_popln.size();

        int f0 = 0, f1 = 0, f2 = 0;

        Integer numTries = 0;
        int MAX_TRIES = GAParameters.getMaxTriesFactor();

        String molName, inchi, smiles;

        int Xop = -1, Mop = -1, Bop = -1;
        int MAX_EVOLVE_ATTEMPTS = 10;
        
        int graphsCreated = 0;
        int GraphCreationLogIntervall = 20;

        int i=0;
        ArrayList<Task> tasks = new ArrayList<>();
        Monitor mnt = new Monitor("Generation " + genId);
        try
        {
            while (i < GAParameters.getPopulationSize() *
                    GAParameters.getMaxTriesFactor()) 
            {
                i++;
                
                if (checkForException())
                {
                    stopRun();
                    throw new DENOPTIMException("Errors found during sub-tasks "
                    		+ "execution.", ex);
                }

                synchronized (population)
                {
                    if (population.size() >= newPopSize)
                    {
                        break;
                    }
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

                OffspringEvaluationTask task = new OffspringEvaluationTask(candidate, 
                        EAUtils.getPathNameToGenerationFolder(genId), 
                        population, mnt, GAParameters.getUIDFileOut());
                
                /*
                    OffspringEvaluationTask task = new OffspringEvaluationTask(molName, graph4, inchi, smiles, cmol,
                            EAUtils.getPathNameToGenerationFolder(genId), population,
                                           numTries,GAParameters.getUIDFileOut());
                */
                //TODO-V3 make submission dependent on GAParameters.parallelizationScheme? 
                // so to remove the duplicated code in classes EvolutionaryAlgorithm and ParallelEvolutionaryAlgorithm
                submitted.add(task);
                futures.add(tcons.submit(task));

                //TODO-GG remove duplicate check?
                synchronized (population)
                {
                    if (population.size() >= newPopSize)
                    {
                        break;
                    }
                }
            } // end while
            
            // Remove completed tasks: fixed memory leak
            cleanupCompleted(tcons, futures, submitted);
        }
        catch (DENOPTIMException dex)
        {
            cleanup(tcons, futures, submitted);
            tcons.shutdown();
            dex.printStackTrace();
            throw dex;
        }
        catch (Exception ex)
        {
            cleanup(tcons, futures, submitted);
            tcons.shutdown();
            ex.printStackTrace();
            throw new DENOPTIMException(ex);
        }

        mnt.printSummary();

        // sort the population
        synchronized (population)
        {
            Collections.sort(population, Collections.reverseOrder());
        }
        
        if (GAParameters.getReplacementStrategy() == 1)
        {
            synchronized(population)
            {
                int k = population.size();

                // trim the population to the desired size
                for (int l=GAParameters.getPopulationSize(); l<k; l++)
                {
                    population.get(l).cleanup();
                }
                population.subList(
                        GAParameters.getPopulationSize(), k).clear();
            }
        }
        
        cleanup(clone_popln);

        // check if the new population contains a children produced here
        // If yes, return true
        
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

    /**
     * Removes all tasks whether they are completed or not.
     */
    private void cleanup(ThreadPoolExecutor tcons, List<Future<Object>> futures,
                            ArrayList<FitnessTask> submitted)
    {
        for (Future<Object> f : futures)
        {
            f.cancel(true);
        }

        for (FitnessTask tsk: submitted)
        {
            tsk.stopTask();
        }
        submitted.clear();
        tcons.getQueue().clear();
    }

//------------------------------------------------------------------------------

    /**
     * Removes only tasks that are marked as completed.
     */
    private void cleanupCompleted(ThreadPoolExecutor tcons,
            List<Future<Object>> futures, 
            ArrayList<FitnessTask> submitted)
    {
        ArrayList<FitnessTask> completed = new ArrayList<FitnessTask>();

        for (FitnessTask t : submitted)
        {
            if (t.isCompleted())
                completed.add(t);
        }

        for (FitnessTask t : completed)
        {
            submitted.remove(t);
            futures.remove(t); //FIXME
        }
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
