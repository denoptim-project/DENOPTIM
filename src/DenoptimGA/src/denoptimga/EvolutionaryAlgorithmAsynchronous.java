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
 * (see {@link EvolutionaryAlgorithm}, it is always guaranteed that <i>M = 1</i>.
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
    private final String fsep = System.getProperty("file.separator");
    
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
            DENOPTIMLogger.appLogger.log(Level.INFO,"Starting Generation {0}"+NL, 
                    genId);

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
            DENOPTIMLogger.appLogger.log(Level.INFO,txt + " in Generation {0}"+NL, genId);
            EAUtils.outputPopulationDetails(population, 
                    EAUtils.getPathNameToGenerationDetailsFile(genId));
            DENOPTIMLogger.appLogger.log(
                    Level.INFO,"Generation {0}" + " completed"+NL
                            + "----------------------------------------"
                            + "----------------------------------------"+NL,
                            genId);

            if (numStag >= GAParameters.getNumberOfConvergenceGenerations())
            {
                DENOPTIMLogger.appLogger.log(Level.WARNING,"No change in "
                        + "population over {0} iterations. Stopping EA."+NL,
                        numStag);
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
        DENOPTIMLogger.appLogger.log(Level.INFO, "Overall time: {0}."+NL,
                                                            watch.toString());
        DENOPTIMLogger.appLogger.info("DENOPTIM EA run completed."+NL);
    }

//------------------------------------------------------------------------------

    /**
     * Generate children that are not already among the current population members.
     * @param population the current population
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
        ArrayList<String> initialUIDs = EAUtils.getUniqueIdentifiers(clone_popln);

        int newPopSize = GAParameters.getNumberOfChildren() + clone_popln.size();

        int f0 = 0, f1 = 0, f2 = 0;

        Integer numTries = 0;
        int MAX_TRIES = GAParameters.getMaxTriesFactor();

        String molName, inchi, smiles;

        int Xop = -1, Mop = -1, Bop = -1;
        int MAX_EVOLVE_ATTEMPTS = 10;
        
        int graphsCreated = 0;
        int GraphCreationLogIntervall = 20;

        try
        {
            while (true)
            {
                if (checkForException())
                {
                    stopRun();
                    throw new DENOPTIMException("Errors found during sub-tasks "
                    		+ "execution.", ex);
                }

                synchronized (numTries)
                {
                    if (numTries >= MAX_TRIES)
                    {
                        break;
                    }
                }

                synchronized (population)
                {
                    if (population.size() >= newPopSize)
                    {
                        break;
                    }
                }

                DENOPTIMGraph graph1 = null, graph2 = null, graph3 = null,
                    graph4 = null;
                Xop = -1;
                Mop = -1;
                Bop = -1;

                //TODO-V3: balance decision so that the 
                // 100% = %XOVER + %MUT + %NEW 
                // use random number to decide which operation to do.
                
                if (RandomUtils.nextBoolean(
                        GAParameters.getCrossoverProbability()))
                {
                    int numatt = 0;
                    Candidate male = null, female = null;
                    int mvid = -1, fvid = -1;
                    boolean foundPars = false;
                    while (numatt < MAX_EVOLVE_ATTEMPTS)
                    {
                        if (FragmentSpace.useAPclassBasedApproach())
                        {
                            DENOPTIMVertex[] pair = EAUtils.performFBCC(
                                    clone_popln);
                            
                            if (pair == null)
                            {
                                DENOPTIMLogger.appLogger.info("Failed to identify "
                                        + "compatible parents for crossover.");
                                numatt++;
                                continue;
                            }
                            male = pair[0].getGraphOwner().getCandidateOwner();
                            female = pair[1].getGraphOwner().getCandidateOwner();
                            mvid = pair[0].getGraphOwner().indexOf(pair[0]);
                            fvid = pair[1].getGraphOwner().indexOf(pair[1]);
                        } else {
                            int parents[] = EAUtils.selectBasedOnFitness(
                                    clone_popln, 2);
                            if (parents[0] == -1 || parents[1] == -1)
                            {
                                DENOPTIMLogger.appLogger.info("Failed to identify "
                                        + "parents for crossover.");
                                numatt++;
                                continue;
                            }
                            male = clone_popln.get(parents[0]);
                            female = clone_popln.get(parents[1]);
                            mvid = EAUtils.selectNonScaffoldNonCapVertex(
                                    male.getGraph());
                            fvid = EAUtils.selectNonScaffoldNonCapVertex(
                                    female.getGraph());
                        }
                        foundPars = true;
                        break;
                    }

                    if (foundPars)
                    {
                        String molid1 = FilenameUtils.getBaseName(
                                male.getSDFFile());
                        String molid2 = FilenameUtils.getBaseName(
                                female.getSDFFile());

                        int gid1 = male.getGraph().getGraphId();
                        int gid2 = female.getGraph().getGraphId();
                        
                        graph1 = male.getGraph().clone();
                        graph2 = female.getGraph().clone();
                        
                        graph1.renumberGraphVertices();
                        graph2.renumberGraphVertices();

                        f0 += 2;

                        if (DENOPTIMGraphOperations.performCrossover(graph1, 
                                graph1.getVertexAtPosition(mvid).getVertexId(),
                                graph2,
                                graph2.getVertexAtPosition(fvid).getVertexId()))
                        {
                            graph1.setGraphId(GraphUtils.getUniqueGraphIndex());
                            graph2.setGraphId(GraphUtils.getUniqueGraphIndex());
                            EAUtils.addCappingGroup(graph1);
                            EAUtils.addCappingGroup(graph2);
                            Xop = 1;
                            
                            graph1.setLocalMsg("Xover: " + molid1 + "|" + gid1 +
                                                "=" + molid2 + "|" + gid2);
                            graph2.setLocalMsg("Xover: " + molid1 + "|" + gid1 +
                                                "=" + molid2 + "|" + gid2);
                            
                            graphsCreated = graphsCreated + 2;
                        }
                        else
                        {
                            if (graph1 != null)
                            {
                                graph1.cleanup();
                            }
                            if (graph2 != null)
                            {
                                graph2.cleanup();
                            }
                            graph1 = null; graph2 = null;
                        }
                    }
                }

                if (RandomUtils.nextBoolean(
                        GAParameters.getMutationProbability()))
                {
                    int numatt = 0;
                    int i3 = -1;
                    boolean foundPars = false;
                    while (numatt < MAX_EVOLVE_ATTEMPTS)
                    {
                        i3 = EAUtils.selectBasedOnFitness(clone_popln,1)[0];
                        if (i3 == -1)
                        {
                            DENOPTIMLogger.appLogger.info("Invalid mutable "
                                    + "candidate selection.");
                            numatt++;
                            continue;
                        }
                        foundPars = true;
                        break;
                    }
                     
                    if (foundPars)
                    {
                        graph3 = clone_popln.get(i3).getGraph().clone();
                        f1 += 1;

                        String molid3 = FilenameUtils.getBaseName(
                                clone_popln.get(i3).getSDFFile());
                        int gid3 = clone_popln.get(i3).getGraph()
                                .getGraphId();

                        if (DENOPTIMGraphOperations.performMutation(graph3))
                        {
                            graph3.setGraphId(GraphUtils.getUniqueGraphIndex());
                            Mop = 1;
                            graph3.setLocalMsg("Mutation: " + molid3 + "|" + gid3);
                        }
                        EAUtils.addCappingGroup(graph3);
                        
                        graphsCreated++;
                    }
                    else
                    {
                        graph3 = null;
                    }
                }

                // ... if neither xover not mutation has been done, we build a 
                // new graph from scratch
                if (Xop == -1 && Mop == -1)
                {
                    f2++;
                    graph4 = EAUtils.buildGraph();

                    if (graph4 != null)
                    {
                        Bop = 1;
                        graph4.setLocalMsg("NEW");
                        
                        graphsCreated++;
                    }
                }

                // Bop can be != 1 due to impossibility of building a new graph?
                if (Bop == 1)
                {
                    Object[] res = EAUtils.evaluateGraph(graph4);

                    if (res != null)
                    {
                        if (!EAUtils.setupRings(res,graph4))
                        {
                            res = null;
                        }
                    }
                    
                    // Check if the chosen combination gives rise to forbidden ends
                    //TODO-V3 this should be considered already when making the list of
                    // possible combination of rings
                    for (DENOPTIMVertex rcv : graph4.getFreeRCVertices())
                    {
                        APClass apc = rcv.getEdgeToParent().getSrcAP().getAPClass();
                        if (FragmentSpace.getCappingMap().get(apc)==null 
                                && FragmentSpace.getForbiddenEndList().contains(apc))
                    	{
                    		res = null;
                    	}
                    }

                    if (res == null)
                    {
                        synchronized(numTries)
                        {
                            numTries++;
                        }
                        continue;
                    }
                    else
                    {
                        synchronized(numTries)
                        {
                            numTries = 0;
                        }
                    }

                    // Create the task
                    // check if the molinchi has been encountered before
                    inchi = res[0].toString().trim();

//                    if (lstUID.contains(inchi))
//                    {
//                        synchronized(numtry)
//                        {
//                            numtry++;
//                        }
//                        continue;
//                    }
//                    else
//                    {
//                        lstUID.add(inchi);
//                        synchronized(numtry)
//                        {
//                            numtry = 0;
//                        }
//                    }

                    // file extensions will be added later
                    molName = "M" + GenUtils.getPaddedString(
                            DENOPTIMConstants.MOLDIGITS,
                            GraphUtils.getUniqueMoleculeIndex());

                    smiles = res[1].toString().trim();
                    IAtomContainer cmol = (IAtomContainer) res[2];

                    OffspringEvaluationTask task = new OffspringEvaluationTask(molName, graph4, inchi, smiles, cmol,
                            EAUtils.getPathNameToGenerationFolder(genId), population,
                                           numTries,GAParameters.getUIDFileOut());

                    //TODO-V3 make submission dependent on GAParameters.parallelizationScheme? 
                    // so to remove the duplicated code in classes EvolutionaryAlgorithm and ParallelEvolutionaryAlgorithm
                    submitted.add(task);
                    futures.add(tcons.submit(task));

                    synchronized (population)
                    {
                        if (population.size() == newPopSize 
                                || population.size() > newPopSize)
                        {
                            break;
                        }
                    }

                }
                
                if (Xop == 1)
                {
                    if (graph1 != null)
                    {
                        Object[] res1 = EAUtils.evaluateGraph(graph1);

                        if (res1 != null)
                        {
                            if (!EAUtils.setupRings(res1,graph1))
                            {
                                res1 = null;
                            }
                        }
                        
                        // Check if the chosen combination gives rise to forbidden ends
                        //TODO-V3 this should be considered already when making the list of
                        // possible combination of rings
                        for (DENOPTIMVertex rcv : graph1.getFreeRCVertices())
                        {
                            APClass apc = rcv.getEdgeToParent().getSrcAP().getAPClass();
                            if (FragmentSpace.getCappingMap().get(apc)==null 
                                    && FragmentSpace.getForbiddenEndList().contains(apc))
                        	{
                        		res1 = null;
                        	}
                        }

                        if (res1 == null)
                        {
                            synchronized(numTries)
                            {
                                numTries++;
                            }
                            continue;
                        }
                        else
                        {
                            synchronized(numTries)
                            {
                                numTries = 0;
                            }
                        }

                        // Create the task
                        // check if the molinchi has been encountered before
                        inchi = res1[0].toString().trim();

//                        if (lstUID.contains(inchi))
//                        {
//                            synchronized(numtry)
//                            {
//                                numtry++;
//                            }
//                            continue;
//                        }
//                        else
//                        {
//                            lstUID.add(inchi);
//                            synchronized(numtry)
//                            {
//                                numtry = 0;
//                            }
//                        }

                        // file extensions will be added later
                        molName = "M" + GenUtils.getPaddedString(
                                DENOPTIMConstants.MOLDIGITS,
                                GraphUtils.getUniqueMoleculeIndex());

                        smiles = res1[1].toString().trim();
                        IAtomContainer cmol = (IAtomContainer) res1[2];

                        OffspringEvaluationTask task1 = new OffspringEvaluationTask(molName, graph1, inchi, smiles,
                                           cmol, EAUtils.getPathNameToGenerationFolder(genId), population,
                                           numTries,GAParameters.getUIDFileOut());

                        //TODO-V3 make submission dependent on GAParameters.parallelizationScheme? 
                        // so to remove the duplicated code in classes EvolutionaryAlgorithm and ParallelEvolutionaryAlgorithm
                        submitted.add(task1);
                        futures.add(tcons.submit(task1));

                        synchronized (population)
                        {
                            if (population.size() == newPopSize 
                                    || population.size() > newPopSize)
                            {
                                break;
                            }
                        }
                    }


                    // add graph2
                    if (graph2 != null)
                    {
                        Object[] res2 = EAUtils.evaluateGraph(graph2);

                        if (res2 != null)
                        {
                            if (!EAUtils.setupRings(res2,graph2))
                            {
                                res2 = null;
                            }
                        }
                        
                        // Check if the chosen combination gives rise to forbidden ends
                        //TODO-V3 this should be considered already when making the list of
                        // possible combination of rings
                        for (DENOPTIMVertex rcv : graph2.getFreeRCVertices())
                        {
                            APClass apc = rcv.getEdgeToParent().getSrcAP().getAPClass();
                            if (FragmentSpace.getCappingMap().get(apc)==null 
                                    && FragmentSpace.getForbiddenEndList().contains(apc))
                        	{
                        		res2 = null;
                        	}
                        }

                        if (res2 == null)
                        {
                            synchronized(numTries)
                            {
                                numTries++;
                            }
                            continue;
                        }
                        else
                        {
                            synchronized(numTries)
                            {
                                numTries = 0;
                            }
                        }

                        // Create the task
                        // check if the molinchi has been encountered before
                        inchi = res2[0].toString().trim();

//                        if (lstUID.contains(inchi))
//                        {
//                            synchronized(numtry)
//                            {
//                                numtry++;
//                            }
//                            continue;
//                        }
//                        else
//                        {
//                            lstUID.add(inchi);
//                            synchronized(numtry)
//                            {
//                                numtry = 0;
//                            }
//                        }

                        // file extensions will be added later
                        molName = "M" + GenUtils.getPaddedString(
                                DENOPTIMConstants.MOLDIGITS,
                                GraphUtils.getUniqueMoleculeIndex());

                        smiles = res2[1].toString().trim();
                        IAtomContainer cmol = (IAtomContainer) res2[2];

                        OffspringEvaluationTask task2 = new OffspringEvaluationTask(molName, graph2, inchi, smiles,
                                                cmol, EAUtils.getPathNameToGenerationFolder(genId), population,
                                                numTries, 
                                                GAParameters.getUIDFileOut());

                        //TODO-V3 make submission dependent on GAParameters.parallelizationScheme? 
                        // so to remove the duplicated code in classes EvolutionaryAlgorithm and ParallelEvolutionaryAlgorithm
                        
                        submitted.add(task2);
                        futures.add(tcons.submit(task2));

                        synchronized (population)
                        {
                            if (population.size() == newPopSize || population.size() > newPopSize)
                            {
                                break;
                            }
                        }
                    }
                }

                if (Mop == 1)
                {
                    // add graph3
                    if (graph3 != null)
                    {
                        Object[] res3 = EAUtils.evaluateGraph(graph3);

                        if (res3 != null)
                        {
                            if (!EAUtils.setupRings(res3,graph3))
                            {
                                res3 = null;
                            }
                        }
                        
                        // Check if the chosen combination gives rise to forbidden ends
                        //TODO-V3 this should be considered already when making the list of
                        // possible combination of rings
                        for (DENOPTIMVertex rcv : graph3.getFreeRCVertices())
                        {
                            APClass apc = rcv.getEdgeToParent().getSrcAP().getAPClass();
                            if (FragmentSpace.getCappingMap().get(apc)==null 
                                    && FragmentSpace.getForbiddenEndList().contains(apc))
                        	{
                        		res3 = null;
                        	}
                        }

                        if (res3 == null)
                        {
                            synchronized(numTries)
                            {
                                numTries++;
                            }
                            continue;
                        }
                        else
                        {
                            synchronized(numTries)
                            {
                                numTries = 0;
                            }
                        }

                        // Create the task
                        // check if the molinchi has been encountered before
                        inchi = res3[0].toString().trim();

//                        if (lstUID.contains(inchi))
//                        {
//                            synchronized(numtry)
//                            {
//                                numtry++;
//                            }
//                            continue;
//                        }
//                        else
//                        {
//                            lstUID.add(inchi);
//                            synchronized(numtry)
//                            {
//                                numtry = 0;
//                            }
//                        }

                        // file extensions will be added later
                        molName = "M" +
                                GenUtils.getPaddedString(DENOPTIMConstants.MOLDIGITS,
                                                GraphUtils.getUniqueMoleculeIndex());
                        smiles = res3[1].toString().trim();
                        IAtomContainer cmol = (IAtomContainer) res3[2];

                        OffspringEvaluationTask task3 = new OffspringEvaluationTask(molName, graph3, inchi, smiles,
                                                cmol, EAUtils.getPathNameToGenerationFolder(genId), population,
                                                numTries, 
                                                GAParameters.getUIDFileOut());

                        //TODO-V3 make submission dependent on GAParameters.parallelizationScheme? 
                        // so to remove the duplicated code in classes EvolutionaryAlgorithm and ParallelEvolutionaryAlgorithm
                        
                        submitted.add(task3);
                        futures.add(tcons.submit(task3));

                        synchronized (population)
                        {
                            if (population.size() == newPopSize || population.size() > newPopSize)
                            {
                                break;
                            }
                        }
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

        StringBuilder sb = new StringBuilder(256);
        sb.append("Crossover Attempted: ").append(f0).append("\n");
        sb.append("Mutation Attempted: ").append(f1).append("\n");
        sb.append("New Molecule Attempted: ").append(f2).append("\n");

        DENOPTIMLogger.appLogger.info(sb.toString());
        sb.setLength(0);

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
        for (int i=0; i<population.size(); i++)
        {
            if (!initialUIDs.contains(population.get(i).getUID()))
            {
                updated = true;
                break;
            }
        }
        
        initialUIDs.clear();
        return updated;
    }

//------------------------------------------------------------------------------

    private void initializePopulation(ArrayList<Candidate> population) 
            throws DENOPTIMException
    {
        final int MAX_TRIES = GAParameters.getPopulationSize() *
                GAParameters.getMaxTriesFactor();
        Integer numTries = 0;

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

        try
        {
            while (true)
            {
                if (checkForException())
                {
                    stopRun();
                    throw new DENOPTIMException("Errors found during sub tasks "
                    		+ "execution.", ex);
                }
                synchronized (numTries)
                {
                    if (numTries == MAX_TRIES)
                    {
                        cleanupCompleted(tcons, futures, submitted);
                        break;
                    }
                }

                synchronized (population)
                {
                    if (population.size() >= GAParameters.getPopulationSize())
                    {
                        break;
                    }
                }

                Candidate candidate = EAUtils.buildCandidateFromScratch(
                        numTries);
                if (candidate == null)
                    continue;
                
                OffspringEvaluationTask task = new OffspringEvaluationTask(
                        candidate, EAUtils.getPathNameToGenerationFolder(0), 
                        population, numTries, GAParameters.getUIDFileOut());
                
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

        if (numTries >= MAX_TRIES)
        {
            stopRun();

            DENOPTIMLogger.appLogger.log(Level.SEVERE,
                    "Unable to initialize molecules in {0} attempts.\n", 
                    numTries);
            throw new DENOPTIMException("Unable to initialize molecules in " +
                            numTries + " attempts.");
        }

        synchronized (population)
        {
            population.trimToSize();
            Collections.sort(population, Collections.reverseOrder());
        }
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
