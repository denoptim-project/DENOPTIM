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
import denoptim.task.FitnessTask;
import denoptim.task.Task;
import denoptim.task.TasksBatchManager;
import denoptim.utils.GenUtils;
import denoptim.utils.GraphUtils;
import denoptim.utils.RandomUtils;
import denoptimga.EAUtils.CandidateSource;



/**
 * DENOPTIM's evolutionary algorithm. 
 * <p>This implementation offers two 
 * parallelisation schemes for managing the amount and timing of threads 
 * dedicated to the evaluation of candidates. The schemes are named
 * <i>synchronous</i> and <i>asynchronous</i>.</p>
 * <p>The <i>synchronous</i> algorithm generates as many new candidates as 
 * needed to fill-up or evolve the population, and then submits the candidate
 * evaluation threads with a batch-based executor. All submitted threads must
 * terminate before submitting more threads. With this parallelisation scheme 
 * all candidates initiated at
 * a given generation can only become part of that very generation. 
 * This, however, is only possible when accepting a somewhat inefficient
 * use of the resources every time a generation is about to be completed. 
 * To illustrate: consider the moment when the are N free seats in the 
 * population (i.e., candidates still to build and evaluate) 
 * and M potentially available seats for candidate evaluation threads,
 * where N&lt;M. The algorithm will generate N candidates, and M-N 
 * thread seats will remain idle. The situation is repeated a few times until
 * fewer and fewer threads are submitted and the last population member 
 * (or new candidate population member) has been evaluated.</p>
 * <p>This <i>asynchronous</i> parallelisation scheme removes the waiting for 
 * completion of a batch of threads by keeping always as many active fitness 
 * evaluation threads as the max number of such threads, which is controllable 
 * in the {@link GAParameters}. Even towards the end of a generation, all 
 * threads will be used to evaluate candidates. The latter may thus exceed the
 * number of candidates needed to complete the current generation.
 * It is thus possible that a candidate designed in generation <i>I</i> becomes
 * a member of generation <i>J</i>, where <i>J &ge; I</i>.</p>
 * 
 *  
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */


public class EvolutionaryAlgorithm
{
    /**
     * Flag determining the use of asynchronous parallelization scheme.
     */
    private boolean isAsync = true;
    
    /*
     * Temp. storage of future results produced by fitness evaluation tasks
     * submitted to asynchronous parallelisation scheme.
     */
    private List<Future<Object>> futures;
    
    /*
     * Temp. storage of fitness evaluation tasks just submitted to asynchronous 
     * parallelisation scheme.
     */
    private ArrayList<FitnessTask> submitted;
    
    /*
     * Execution service used in asynchronous parallelisation scheme.
     */
    private ThreadPoolExecutor tcons;

    /*
     * Issue emerging from a thread submitted by asynchronous parallelisation 
     * scheme.
     */
    private Throwable ex;
    
    private final String NL = System.getProperty("line.separator");

//------------------------------------------------------------------------------
    
    public EvolutionaryAlgorithm()
    {
        // There is currently nothing to initialise for the synchronous scheme
        if (GAParameters.parallelizationScheme == 1)
        {
            isAsync = false;
        } else {
            isAsync = true;
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
                            tcons.shutdownNow(); //Cancel running tasks
                        }
                        if (!tcons.awaitTermination(60, TimeUnit.SECONDS))
                        {
                            // pool didn't terminate after the second try
                        }
                    }
                    catch (InterruptedException ie)
                    {
                        cleanupAsync(tcons, futures, submitted);
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
                public void rejectedExecution(Runnable r, 
                        ThreadPoolExecutor executor)
                {
                    try
                    {
                        // this will block if the queue is full
                        executor.getQueue().put(r);
                    }
                    catch (InterruptedException ex)
                    {
                        //nothing, really
                    }
                }
            });
        }
    }

//------------------------------------------------------------------------------
    
    public void run() throws DENOPTIMException
    {
        StopWatch watch = new StopWatch();
        watch.start();
        if (isAsync)
        {
            tcons.prestartAllCoreThreads();
        }
            
        
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
            emptyThisList(population);
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
        
        if (isAsync)
        {
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
        emptyThisList(population);
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
        
        Monitor mnt = new Monitor("Generation 0");
        
        // Loop creation of candidates until we have created enough new valid 
        // candidates or we have reached the max number of attempts.
        int i=0;
        ArrayList<Task> tasks = new ArrayList<>();
        try 
        {
            while (i < GAParameters.getPopulationSize() *
                    GAParameters.getMaxTriesFactor()) 
            {
                i++;
                
                //TODO-GG checking for exceptions only in asym?
                if (checkForException())
                {
                    stopRun();
                    throw new DENOPTIMException("Errors found during sub tasks "
                            + "execution.", ex);
                }
                
                synchronized (population)
                {
                    if (population.size() >= GAParameters.getPopulationSize())
                        break;
                }
                
                Candidate candidate = EAUtils.buildCandidateFromScratch(mnt);
                  
                if (candidate == null)
                    continue;
               
                OffspringEvaluationTask task = new OffspringEvaluationTask(
                        candidate, EAUtils.getPathNameToGenerationFolder(0), 
                        population, mnt, GAParameters.getUIDFileOut());
                
                // Submission is dependent on the parallelisation scheme
                if (isAsync)
                {
                    //TODO-GG del
                    System.out.println("INI: Submitting  "+candidate.getName());
                    
                    submitted.add(task);
                    futures.add(tcons.submit(task));
                } else {
                    tasks.add(task);
                    if (tasks.size() >= Math.abs(
                            population.size() - GAParameters.getPopulationSize()))
                    {
                        //TODO-GG del
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
            }
        } catch (DENOPTIMException dex)
        {
            if (isAsync)
            {
                cleanupAsync(tcons, futures, submitted);
                tcons.shutdown();
            }
            throw dex;
        }
        catch (Exception ex)
        {
            if (isAsync)
            {
                cleanupAsync(tcons, futures, submitted);
                tcons.shutdown();
            }
            throw new DENOPTIMException(ex);
        }
        
        mnt.printSummary();

        if (i >= (GAParameters.getPopulationSize() * 
                GAParameters.getMaxTriesFactor()))
        {
            if (isAsync)
            {
                cleanupCompleted(tcons, futures, submitted);
                stopRun();
            }
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
        ArrayList<Candidate> origPop;
        synchronized (population)
        {
            origPop = new ArrayList<Candidate>();
            for (Candidate m : population)
            {
                origPop.add(m.clone());
            }
        }
        ArrayList<String> initUIDs = EAUtils.getUniqueIdentifiers(origPop);
        
        int newPopSize = GAParameters.getNumberOfChildren() + origPop.size();
        
        int i=0;
        ArrayList<Task> syncronisedTasks = new ArrayList<>();
        Monitor mnt = new Monitor("Generation " + genId);
        try
        {
            while (i < GAParameters.getPopulationSize() *
                    GAParameters.getMaxTriesFactor()) 
            {
                i++;
                
                //TODO-GG checking for exceptions only in async?
                if (checkForException())
                {
                    stopRun();
                    throw new DENOPTIMException("Errors found during sub-tasks "
                            + "execution.", ex);
                }
                
                synchronized (population)
                {
                    if (population.size() >= newPopSize)
                        break;
                }
                
                Candidate candidate = null;
                switch (EAUtils.chooseGenerationMethos())
                {
                    case CROSSOVER:
                    {
                        candidate = EAUtils.buildCandidateByXOver(origPop, mnt);
                        if (candidate == null)
                            continue;
                        break;
                    }
                        
                    case MUTATION:
                    {
                        candidate = EAUtils.buildCandidateByMutation(origPop, 
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
    
                OffspringEvaluationTask task = new OffspringEvaluationTask(
                        candidate, EAUtils.getPathNameToGenerationFolder(genId), 
                        population, mnt, GAParameters.getUIDFileOut());
                
                if (isAsync)
                {
                    //TODO-GG del
                    System.out.println("INI: Submitting  "+candidate.getName());
                    
                    submitted.add(task);
                    futures.add(tcons.submit(task));
                } else {
                    syncronisedTasks.add(task);
                    
                    if (syncronisedTasks.size() 
                            >= Math.abs(population.size() - newPopSize))
                    {
                        //TODO-GG
                        System.out.println("EVO: Submitting Batch of " + syncronisedTasks.size());
                        
                        // Now we have as many tasks as are needed to fill up 
                        // the population. Therefore, we can run the execution 
                        // service.
                        // TasksBatchManager takes the collection of tasks and
                        // runs them in batches of N, where N is given by the
                        // second argument.
                        TasksBatchManager.executeTasks(syncronisedTasks,
                                GAParameters.getNumberOfCPU());
                        syncronisedTasks.clear();
                    }
                }
            }
        }
        catch (DENOPTIMException dex)
        {
            if (isAsync)
            {
                cleanupAsync(tcons, futures, submitted);
                tcons.shutdown();
            }
            dex.printStackTrace();
            throw dex;
        }
        catch (Exception ex)
        {
            if (isAsync)
            {
                cleanupAsync(tcons, futures, submitted);
                tcons.shutdown();
            }
            ex.printStackTrace();
            throw new DENOPTIMException(ex);
        }
        
        mnt.printSummary();
        
        if (i >= (GAParameters.getPopulationSize() *
                GAParameters.getMaxTriesFactor()))
        {
            if (isAsync)
            {
                cleanupCompleted(tcons, futures, submitted);
                stopRun();
            }
            DENOPTIMLogger.appLogger.log(Level.WARNING,
                    "Reached maximum number of attempts (" + i + ") to "
                    + "generate new candidates in generation " + genId + "." 
                            + NL);
        }

        // sort the population
        synchronized (population)
        {
            Collections.sort(population, Collections.reverseOrder());
            if (GAParameters.getReplacementStrategy() == 1)
            {
                // trim the population to the desired size
                int k = population.size();
                for (int l=GAParameters.getPopulationSize(); l<k; l++)
                {
                    population.get(l).cleanup();
                }
                population.subList(GAParameters.getPopulationSize(), k).clear();
            }
        }
        
        emptyThisList(origPop);
        syncronisedTasks.clear();

        // Check if the population has changed
        boolean hasChanged = false;
        for (Candidate mol : population)
        {
            if (!initUIDs.contains(mol.getUID()))
            {
                hasChanged = true;
                break;
            }
        }
        initUIDs.clear();
        
        return hasChanged;
    }
    
//------------------------------------------------------------------------------

    public void stopRun()
    {
        if (isAsync)
        {
            cleanupAsync(tcons, futures, submitted);
            tcons.shutdown();
        }
    }

//------------------------------------------------------------------------------
    
    /**
     * Clears the content of each object in the given list of candidates, and
     * clears the list too.
     */
    private void emptyThisList(ArrayList<Candidate> list)
    {
        for (Candidate mol : list)
            mol.cleanup();
        list.clear();
    }
    
//------------------------------------------------------------------------------

    /**
     * Removes all tasks whether they are completed or not.
     */
    private void cleanupAsync(ThreadPoolExecutor tcons, 
            List<Future<Object>> futures, ArrayList<FitnessTask> submitted)
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
            List<Future<Object>> futures, ArrayList<FitnessTask> submitted)
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
            //NB: futures should be cleaned by garbage collection
        }
    }

//------------------------------------------------------------------------------

    private boolean checkForException()
    {
        boolean foundExceptions = false;
        if (isAsync)
        {
            for (FitnessTask tsk : submitted)
            {
                if (tsk.foundException())
                {
                    foundExceptions = true;
                    DENOPTIMLogger.appLogger.log(Level.SEVERE, "problems in " 
                      + tsk.toString() + ". ErrorMessage: '" + tsk.getErrorMessage() 
                      + "'. ExceptionInTask: "+tsk.getException());
                    ex = tsk.getException().getCause();
                    break;
                }
            }
        } else {
            // We don't really check of exceptions for synchronous scheme
            // TODO-V3: check what happens is a task hits an exception in sync!
        }

        return foundExceptions;
    }
    
//------------------------------------------------------------------------------    

}
