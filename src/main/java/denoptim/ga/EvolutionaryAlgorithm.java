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

package denoptim.ga;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.time.StopWatch;

import denoptim.exception.DENOPTIMException;
import denoptim.exception.ExceptionUtils;
import denoptim.fitness.FitnessParameters;
import denoptim.ga.EAUtils.CandidateSource;
import denoptim.graph.Candidate;
import denoptim.logging.CounterID;
import denoptim.logging.Monitor;
import denoptim.programs.RunTimeParameters.ParametersType;
import denoptim.programs.denovo.GAParameters;
import denoptim.task.FitnessTask;
import denoptim.task.Task;
import denoptim.task.TasksBatchManager;
import denoptim.utils.SizeControlledSet;

/**
 * DENOPTIM's evolutionary algorithm. 
 * <p>This implementation offers two 
 * parallelization schemes for managing the amount and timing of threads 
 * dedicated to the evaluation of candidates. The schemes are named
 * <i>synchronous</i> and <i>asynchronous</i>.</p>
 * <p>The <i>synchronous</i> algorithm generates as many new candidates as 
 * needed to fill-up or evolve the population, and then submits the candidate
 * evaluation threads with a batch-based executor. All submitted threads must
 * terminate before submitting more threads. With this parallelization scheme 
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
 * <p>This <i>asynchronous</i> parallelization scheme removes the waiting for 
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
     * Service watching for external commands. This enables interactive EA runs.
     */
	final ExternalCmdsListener cmdListener;
	
	/**
	 * Storage of unique identifiers encountered by this instance.
	 */
	private SizeControlledSet scs;
	
	/**
	 * Flag signaling this EA was stopped
	 */
    private boolean stopped = false;
    
    /**
     * List of IDs of candidates to be removed from the population. This list
     * is cleared once it content has been processed.
     */
    private Set<String> candidatesToRemove = new HashSet<String>();
    
    /**
     * List of IDs of candidates to be evaluated upon request from the user.
     * There candidates might or might not end up in the populations depending
     * on their performance. This list
     * is cleared once it content has been processed.
     */
    private List<String> candidatesToAdd = new ArrayList<String>();

    /**
     * Flag determining the use of asynchronous parallelization scheme.
     */
    private boolean isAsync = true;
    
    /*
     * Temporary storage of future results produced by fitness evaluation tasks
     * submitted to asynchronous parallelization scheme.
     */
    private List<Future<Object>> futures;
    
    /*
     * Temporary storage of fitness evaluation tasks just submitted to asynchronous 
     * Parallelization scheme.
     */
    private ArrayList<FitnessTask> submitted;
    
    /*
     * Execution service used in asynchronous parallelization scheme.
     */
    private ThreadPoolExecutor tpe;

    /*
     * Issue emerging from a thread submitted by asynchronous parallelization 
     * scheme.
     */
    private Throwable ex;
    
    /**
     * Parameters controlling the execution of this evolutionary algorithm.
     */
    private GAParameters settings;

    /**
     * Program-specific logger
     */
    private Logger logger = null;
    
    private final String NL = System.getProperty("line.separator");

//------------------------------------------------------------------------------

    public EvolutionaryAlgorithm(GAParameters settings, 
            ExternalCmdsListener cmdListener)
    {
        this.settings = settings;
        this.logger = settings.getLogger();
        this.cmdListener = cmdListener;
        
        // There is currently nothing to initialize for the synchronous scheme
        if (settings.getParallelizationScheme() == 1)
        {
            isAsync = false;
        } else {
            isAsync = true;
            futures = new ArrayList<>();
            submitted = new ArrayList<>();
            
            tpe = new ThreadPoolExecutor(settings.getNumberOfCPU(),
                    settings.getNumberOfCPU(), 0L,
                    TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<Runnable>(1));
    
            Runtime.getRuntime().addShutdownHook(new Thread()
            {
                @Override
                public void run()
                {
                    tpe.shutdown(); // Disable new tasks from being submitted
                    try
                    {
                        // Wait a while for existing tasks to terminate
                        if (!tpe.awaitTermination(30, TimeUnit.SECONDS))
                        {
                            tpe.shutdownNow(); //Cancel running tasks
                        }
                        if (!tpe.awaitTermination(60, TimeUnit.SECONDS))
                        {
                            // pool didn't terminate after the second try
                        }
                    }
                    catch (InterruptedException ie)
                    {
                        cleanupAsync(tpe, futures, submitted);
                        // (Re-)Cancel if current thread also interrupted
                        tpe.shutdownNow();
                        // Preserve interrupt status
                        Thread.currentThread().interrupt();
                    }
                }
            });
    
            // by default the ThreadPoolExecutor will throw an exception
            tpe.setRejectedExecutionHandler(new RejectedExecutionHandler()
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
        
        scs = new SizeControlledSet(
                settings.maxUIDMemory, settings.uidMemoryOnDisk, 
                settings.getUIDFileOut());
    }

//------------------------------------------------------------------------------
    
    public void run() throws DENOPTIMException
    {
        cmdListener.setReferenceToRunningEAlgorithm(this);
        
        StopWatch watch = new StopWatch();
        watch.start();
        if (isAsync)
        {
            tpe.prestartAllCoreThreads();
        }
        Monitor mnt = new Monitor();
        mnt.printHeader(settings.getMonitorFile());
        
        // Create initial population of candidates
        EAUtils.createFolderForGeneration(0, settings);
        Population population;
        try
        {
            population = EAUtils.importInitialPopulation(scs, settings);
        } catch (Exception e)
        {
            throw new DENOPTIMException("Unable to import initial population.", 
                    e);
        }
        initializePopulation(population);
        EAUtils.outputPopulationDetails(population, 
                EAUtils.getPathNameToGenerationDetailsFile(0, settings), settings);
        
        // Ensure that there is some variability in fitness values
        double sdev = EAUtils.getPopulationSD(population);
        if (sdev < settings.getMinFitnessSD())
        {
            String msg = "Fitness values have negligible standard deviation "
                    + "(STDDEV=" + String.format("%.6f", sdev) + "). "
                    + "Abbandoning evolutionary algorithm.";
            logger.log(Level.SEVERE, msg);
            population.trim(0);
            return;
        }

        // Start evolution cycles, i.e., generations
        int numStag = 0, genId = 1;
        while (genId <= settings.getNumberOfGenerations())
        {
            logger.log(Level.INFO,"Starting Generation {0}"
                    + NL, genId);

            String txt = "No change";
            try
            {
                if (!evolvePopulation(population, genId))
                {
                    numStag++;
                }
                else
                {
                    numStag = 0;
                    txt = "New members introduced";
                }
            } catch (DENOPTIMException e)
            {
                logger.log(Level.SEVERE, "Exception while running evolutionary"
                        + "algorithm. Details: " + NL 
                        + ExceptionUtils.getStackTraceAsString(e));
                throw e;
            }
            
            logger.log(Level.INFO,txt + " in Generation {0}" 
                    + NL, genId);
            EAUtils.outputPopulationDetails(population, 
                    EAUtils.getPathNameToGenerationDetailsFile(genId, settings),
                    settings);
            
            if (stopped)
            {
                logger.log(Level.SEVERE, 
                        "EA stopped while working on generation {0}. " + NL
                        + "Reporting data for incomplete generation {0}."
                        + NL,genId);
                break;
            } else {
                logger.log(Level.INFO,
                        "Generation {0}" + " completed" + NL
                        + "----------------------------------------"
                        + "----------------------------------------" 
                        + NL, genId);
            }

            if (numStag >= settings.getNumberOfConvergenceGenerations())
            {
                logger.log(Level.WARNING, 
                        "No change in population over {0} iterations. "
                        + "Stopping EA." + NL, numStag);
                break;
            }

            genId++;
        }
        
        if (isAsync)
        {
            tpe.shutdown();
            try
            {
                // wait a bit for pending tasks to finish
                while (!tpe.awaitTermination(5, TimeUnit.SECONDS))
                {
                    // do nothing
                }
            }
            catch (InterruptedException ex)
            {
                //Do nothing
            }
            tpe.shutdown();
        }

        // Sort the population and trim it to desired size
        Collections.sort(population, Collections.reverseOrder());
        if (settings.getReplacementStrategy() == 1)
        {
            population.trim(settings.getPopulationSize());
        }

        // And write final results
        EAUtils.outputFinalResults(population, settings);
        
        // Termination
        population.trim(0);
        watch.stop();
        logger.log(Level.INFO, "Overall time: {0}." + NL,
                watch.toString());
        
        if (stopped)
        {
            logger.info("DENOPTIM EA run stopped." + NL);
        } else {
            logger.info("DENOPTIM EA run completed." + NL);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Fills up the population with candidates build from scratch. 
     * @param population the collection of population members. This is where 
     * pre-existing and newly generated population members will be collected.
     * @throws DENOPTIMException
     */

    private void initializePopulation(Population population) 
            throws DENOPTIMException
    {
        // Deal with existing initial population members
        synchronized (population)
        {
        	if (stopped)
        	{
        		return;
        	}
        	
            Collections.sort(population, Collections.reverseOrder());
            if (settings.getReplacementStrategy() == 1 && 
                    population.size() > settings.getPopulationSize())
            {
                int k = population.size();
                for (int l=settings.getPopulationSize(); l<k; l++)
                {
                    population.get(l).cleanup();
                }
                population.subList(settings.getPopulationSize(),k)
                    .clear();
            }
        }
        if (population.size() == settings.getPopulationSize())
        {
            return;
        }
        
        Monitor mnt = new Monitor("MonitorGen",0,settings.getMonitorFile(),
                settings.getMonitorDumpStep(), settings.dumpMonitor(),
                settings.getLogger());
        
        // Loop creation of candidates until we have created enough new valid 
        // candidates or we have reached the max number of attempts.
        int i=0;
        ArrayList<Task> tasks = new ArrayList<>();
        try 
        {
            while (i < settings.getPopulationSize() *
                    settings.getMaxTriesFactor()) 
            {
                i++;
                
                if (stopped)
                {
                    break;
                }
                
                if (checkForException())
                {
                    stopRun();
                    throw new DENOPTIMException("Errors found during sub tasks "
                            + "execution.", ex);
                }
                
                synchronized (population)
                {
                    if (population.size() >= settings.getPopulationSize())
                        break;
                }
                
                Candidate candidate = EAUtils.buildCandidateFromScratch(mnt,
                        settings);
                  
                if (candidate == null)
                    continue;

                candidate.setGeneration(0);
                
                if (((FitnessParameters)settings.getParameters(
                        ParametersType.FIT_PARAMS)).checkPreFitnessUID())
                {
                    try
                    {
                        if (!scs.addNewUniqueEntry(candidate.getUID()))
                        {
                            mnt.increase(CounterID.DUPLICATEPREFITNESS);
                            continue;
                        }
                    } catch (Exception e) {
                        mnt.increase(
                                CounterID.FAILEDDUPLICATEPREFITNESSDETECTION);
                        continue;
                    }
                }
               
                OffspringEvaluationTask task = new OffspringEvaluationTask(
                        settings,
                        candidate, 
                        EAUtils.getPathNameToGenerationFolder(0,settings), 
                        population, mnt, settings.getUIDFileOut());
                
                // Submission is dependent on the parallelisation scheme
                if (isAsync)
                {
                    submitted.add(task);
                    futures.add(tpe.submit(task));
                } else {
                    tasks.add(task);
                    if (tasks.size() >= Math.abs(
                            population.size() - settings.getPopulationSize())
                            ||
                            //This to avoid the fixed batch size to block the
                            //generation of new candidates for too long
                            i >= (0.1 * settings.getPopulationSize() *
                                    settings.getMaxTriesFactor()))
                    {
                        // Now we have as many tasks as are needed to fill up the 
                        // population. Therefore we can run the execution service.
                        // TasksBatchManager takes the collection of tasks and runs
                        // them in batches of N, where N is given by the
                        // second argument.
                        TasksBatchManager.executeTasks(tasks,
                                settings.getNumberOfCPU());
                        tasks.clear();
                    } else {
                        i = 0;
                    }
                }
            }
        } catch (DENOPTIMException dex)
        {
            if (isAsync)
            {
                cleanupAsync(tpe, futures, submitted);
                tpe.shutdown();
            }
            throw dex;
        }
        catch (Exception ex)
        {
            if (isAsync)
            {
                cleanupAsync(tpe, futures, submitted);
                tpe.shutdown();
            }
            ex.printStackTrace();
            throw new DENOPTIMException(ex);
        }
        
        mnt.printSummary();

        if (i >= (settings.getPopulationSize() * 
                settings.getMaxTriesFactor()))
        {
            if (isAsync)
            {
                cleanupCompleted(tpe, futures, submitted);
                stopRun();
            }
            logger.log(Level.SEVERE,
                    "Unable to initialize molecules in {0} attempts."+NL, i);

            throw new DENOPTIMException("Unable to initialize population in " 
                    + i + " attempts (Population size: " + population.size() 
                    + ", tasks batch size: " + tasks.size() + ").");
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
    private boolean evolvePopulation(Population population, 
            int genId) throws DENOPTIMException
    {
        EAUtils.createFolderForGeneration(genId, settings);
        
        // Take a snapshot of the initial population members. This to exclude
        // that offsprings of this generation become parents in this generation.
        ArrayList<Candidate> eligibleParents = new ArrayList<Candidate>();
        int populationVersion = -1;
        synchronized (population)
        {
            for (Candidate c : population)
            {
                eligibleParents.add(c);
            }
            populationVersion = population.getVersionID();
        }
        
        int newPopSize = settings.getNumberOfChildren() 
                + eligibleParents.size();
        
        int i=0;
        ArrayList<Task> syncronisedTasks = new ArrayList<>();
        Monitor mnt = new Monitor("MonitorGen", genId, 
                settings.getMonitorFile(),settings.getMonitorDumpStep(), 
                settings.dumpMonitor(), settings.getLogger());
        try
        {
            while (i < settings.getPopulationSize() *
                    settings.getMaxTriesFactor()) 
            {
                i++;
                
                if (stopped)
                {
                    break;
                }
                
                if (checkForException())
                {
                    stopRun();
                    throw new DENOPTIMException("Errors found during sub-tasks "
                            + "execution.", ex);
                }
                
                synchronized (population)
                {
                    synchronized (candidatesToRemove)
                    {
                        if (candidatesToRemove.size()>0)
                        {
                            for (String id : candidatesToRemove)
                            {
                                Candidate c = population.getCandidateNamed(id);
                                if (c != null)
                                {
                                    population.remove(c);
                                    eligibleParents.remove(c);
                                }
                            }
                            candidatesToRemove.clear();
                        }
                    }
                }
                
                synchronized (population)
                {
                    if (population.size() >= newPopSize)
                        break;
                }
                
                File srcOfCandidate = null;
                Candidate candidate = null;
                CandidateSource src = CandidateSource.CONSTRUCTION;
                synchronized (candidatesToAdd)
                {
                    if (candidatesToAdd.size()>0)
                    {
                        src = CandidateSource.MANUAL;
                        srcOfCandidate = new File(candidatesToAdd.get(0));
                        candidatesToAdd.remove(0);
                    } else {
                        src = EAUtils.chooseGenerationMethod(settings);
                    }
                }
                
                switch (src)
                {
                    case MANUAL:
                    {
                        candidate = EAUtils.readCandidateFromFile(
                                srcOfCandidate, mnt, settings);
                        if (candidate == null)
                            continue;
                        break;
                    }
                    case CROSSOVER:
                    {
                        candidate = EAUtils.buildCandidateByXOver(
                                eligibleParents, population, mnt, settings);
                        if (candidate == null)
                            continue;
                        break;
                    }
                        
                    case MUTATION:
                    {
                        candidate = EAUtils.buildCandidateByMutation(
                                eligibleParents, mnt, settings);
                        if (candidate == null)
                            continue;
                        break;
                    }
                        
                    case CONSTRUCTION:
                    {
                        candidate = EAUtils.buildCandidateFromScratch(mnt,
                                settings);
                        if (candidate == null)
                            continue;
                        break;
                    }
                }
                
                if (candidate == null)
                    continue;
                
                candidate.setGeneration(genId);
                    
                if (((FitnessParameters)settings.getParameters(
                        ParametersType.FIT_PARAMS)).checkPreFitnessUID())
                {
                    try
                    {
                        if (!scs.addNewUniqueEntry(candidate.getUID()))
                        {
                            mnt.increase(CounterID.DUPLICATEPREFITNESS);
                            continue;
                        }
                    } catch (Exception e) {
                        mnt.increase(
                                CounterID.FAILEDDUPLICATEPREFITNESSDETECTION);
                        continue;
                    }
                }
                
                OffspringEvaluationTask task = new OffspringEvaluationTask(
                        settings,
                        candidate, 
                        EAUtils.getPathNameToGenerationFolder(genId, settings), 
                        population, mnt, settings.getUIDFileOut());
                
                if (isAsync)
                {
                    submitted.add(task);
                    futures.add(tpe.submit(task));
                } else {
                    syncronisedTasks.add(task);
                    if (syncronisedTasks.size() 
                            >= Math.abs(population.size() - newPopSize)
                            ||
                            //This to avoid the fixed batch size to block the
                            //generation of new candidates for too long
                            i >= (0.1 * settings.getPopulationSize() *
                                    settings.getMaxTriesFactor()))
                    {
                        // Now we have as many tasks as are needed to fill up 
                        // the population, or we got sick to wait.
                        // TasksBatchManager takes the collection of tasks and
                        // runs them in batches of N, where N is given by the
                        // second argument.
                        TasksBatchManager.executeTasks(syncronisedTasks,
                                settings.getNumberOfCPU());
                        syncronisedTasks.clear();
                    }
                }
            }
        }
        catch (DENOPTIMException dex)
        {
            if (isAsync)
            {
                cleanupAsync(tpe, futures, submitted);
                tpe.shutdown();
            }
            dex.printStackTrace();
            throw dex;
        }
        catch (Exception ex)
        {
            if (isAsync)
            {
                cleanupAsync(tpe, futures, submitted);
                tpe.shutdown();
            }
            ex.printStackTrace();
            throw new DENOPTIMException(ex);
        }
        
        mnt.printSummary();
        
        if (i >= (settings.getPopulationSize() *
                settings.getMaxTriesFactor()))
        {
            if (isAsync)
            {
                cleanupCompleted(tpe, futures, submitted);
                stopRun();
            }
            logger.log(Level.WARNING,
                    "Reached maximum number of attempts (" + i + ") to "
                    + "generate new candidates in generation " + genId + "." 
                            + NL);
        }

        // sort the population
        synchronized (population)
        {
            Collections.sort(population, Collections.reverseOrder());
            if (settings.getReplacementStrategy() == 1)
            {
                population.trim(settings.getPopulationSize());
            }
        }
        
        eligibleParents = null;
        syncronisedTasks.clear();

        // Check if the population has changed
        int newPopulationVersion = -1;
        synchronized (population)
        {
            newPopulationVersion = population.getVersionID();
        }
        
        return populationVersion != newPopulationVersion;
    }
    
//------------------------------------------------------------------------------

    public void stopRun()
    {
        if (isAsync)
        {
            cleanupAsync(tpe, futures, submitted);
            tpe.shutdown();
        }
        stopped = true;
    }
    
//------------------------------------------------------------------------------

    /**
     * Removes all tasks whether they are completed or not.
     */
    private void cleanupAsync(ThreadPoolExecutor executor, 
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
        executor.getQueue().clear();
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
                boolean interrupt = false;
                synchronized (tsk.lock)
                {
                    if (tsk.foundException())
                    {
                        foundExceptions = true;
                        logger.log(Level.SEVERE, "problems in " 
                          + tsk.toString() + ". ErrorMessage: '" 
                          + tsk.getErrorMessage() + "'. ExceptionInTask: "
                          + tsk.getException());
                        ex = tsk.getException().getCause();
                        interrupt = true;
                    }
                }
                if (interrupt)
                    break;
            }
        } else {
            // We don't really check of exceptions for synchronous scheme
            // because the executor service will detect the exception and stop
            // the experiment.
        }
        return foundExceptions;
    }

//------------------------------------------------------------------------------    

    /**
     * Adds candidate IDs to the list of "to-be-removed" candidates.
     */
    public void removeCandidates(Set<String> candID)
    {
        synchronized (candidatesToRemove)
        {
            candidatesToRemove.addAll(candID);
        }
    }
    
//------------------------------------------------------------------------------    

    /**
     * Adds candidate IDs to the list of "to-be-included" candidates.
     */
    public void addCandidates(Set<String> pathNames)
    {
        synchronized (candidatesToAdd)
        {
            for (String pathName : pathNames)
            {
                if (!candidatesToAdd.contains(pathName))
                    candidatesToAdd.add(pathName);
            }
        }
    }
    
//------------------------------------------------------------------------------    

}
