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
import denoptim.utils.GenUtils;
import denoptim.utils.GraphUtils;
import denoptim.utils.RandomUtils;
import denoptim.utils.TaskUtils;

/**
 *
 * @author Vishwesh Venkatraman
 */
public class ParallelEvolutionaryAlgorithm
{

    //TODO-V3 change to Executor and initialize based on GAParameters.parallelizationScheme

    final List<Future<Object>> futures;
    final ArrayList<OffspringEvaluationTask> submitted;
    
    final ThreadPoolExecutor tcons;

    private Throwable ex;
   
    private final String fsep = System.getProperty("file.separator");
 
    public ParallelEvolutionaryAlgorithm()
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
                //System.err.println("Calling shutdown.");
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
        for (OffspringEvaluationTask tsk : submitted)
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

    public void runGA() throws DENOPTIMException
    {
        StopWatch watch = new StopWatch();
        watch.start();

        // start the threads
        tcons.prestartAllCoreThreads();
        
        StringBuilder sb = new StringBuilder(32);

        int ndigits = String.valueOf(GAParameters.getNumberOfGenerations()).length();
        sb.append(GAParameters.getDataDirectory()).append(fsep).append("Gen")
                        .append(GenUtils.getPaddedString(ndigits, 0));
        
        String genDir = sb.toString();
        sb.setLength(0);
        // create the directory for the current generation
        DenoptimIO.createDirectory(genDir);

        // store all inchi keys
        HashSet<String> lstUID = new HashSet<>(1024);

        // first collect UIDs of previously known individuals
        if (!GAParameters.getUIDFileIn().equals(""))
        {
            EAUtils.readUID(GAParameters.getUIDFileIn(),lstUID);
            EAUtils.writeUID(GAParameters.getUIDFileOut(),lstUID,false); //overwrite
        }

        // placeholder for the molecules
        ArrayList<Candidate> molPopulation = new ArrayList<>();

        // then, get the molecules from the initial population file 
        String inifile = GAParameters.getInitialPopulationFile();
        if (inifile.length() > 0)
        {
            EAUtils.getPopulationFromFile(inifile, molPopulation, lstUID, genDir);
            String msg = "Read " + molPopulation.size() + " molecules from " + inifile;
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
        }

        // we are done with initial UIDs
        lstUID.clear();
        lstUID = null;

        initializePopulation(molPopulation, genDir);
        
        sb.append(genDir).append(fsep).append("Gen")
            .append(GenUtils.getPaddedString(ndigits, 0)).append(".txt");
        String genOutfile = sb.toString();
        sb.setLength(0);

        EAUtils.outputPopulationDetails(molPopulation, genOutfile);
        
        double sdev = EAUtils.getPopulationSD(molPopulation);
        if (sdev < 0.0001)
        {
            String msg = "Fitness values have little or no difference. STDDEV="
                            + String.format("%.6f", sdev);
            DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
            cleanup(molPopulation);
            return;
        }

        // increment this value if the population is stagnating over a number of
        // generations
        int numStag = 0, curGen = 1;

        while (curGen <= GAParameters.getNumberOfGenerations())
        {
            DENOPTIMLogger.appLogger.log(Level.INFO,
                                        "Starting Generation {0}\n", curGen);

            // create a directory for the current generation
            sb.append(GAParameters.getDataDirectory()).append(fsep).append("Gen")
                    .append(GenUtils.getPaddedString(ndigits, curGen));
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
                    "Fitter molecules introduced in Generation {0}\n", curGen);
            }

            sb.append(genDir).append(fsep).append("Gen").append(
                    GenUtils.getPaddedString(ndigits, curGen)).append(".txt");

            // dump population details to file
            genOutfile = sb.toString();
            sb.setLength(0);

            EAUtils.outputPopulationDetails(molPopulation, genOutfile);
            
            DENOPTIMLogger.appLogger.log(
                    Level.INFO,"Generation {0}" + " completed\n"
                            + "----------------------------------------"
                            + "----------------------------------------\n",
                            curGen);

            // check for stagnation
            if (numStag >= GAParameters.getNumberOfConvergenceGenerations())
            {
                // write a log message
                DENOPTIMLogger.appLogger.log(Level.WARNING,"No change in "
                        + "population over {0} iterations. Stopping EA. \n",
                        numStag);
                break;
            }

            curGen++;
        }

        // shutdown threadpool
        tcons.shutdown();

        try
        {
            // wait for pending tasks to finish
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



        // sort the population
        Collections.sort(molPopulation, Collections.reverseOrder());
        if (GAParameters.getReplacementStrategy() == 1)
        {

            int k = molPopulation.size();

            // trim the population to the desired size
            molPopulation.subList(GAParameters.getPopulationSize(), k).clear();
        }


        genDir = GAParameters.getDataDirectory() + fsep + "Final";
        DenoptimIO.createDirectory(genDir);


        EAUtils.outputFinalResults(molPopulation, genDir);
        
        cleanup(molPopulation);

        watch.stop();

        DENOPTIMLogger.appLogger.log(Level.INFO, "Overall time: {0}.\n",
                                                            watch.toString());

        DENOPTIMLogger.appLogger.info("DENOPTIM EA run completed.\n");
    }

//------------------------------------------------------------------------------

    /**
     * generate children that are not already among the current population
     * they should be valid molecules that do not violate constraints
     * @param molPopulation
     * @param lstInchi
     * @param genDir
     * @return <code>true</code> if new valid molecules are produced such that
     * the population is updated with fitter structures
     * @throws DENOPTIMException
     */
    private boolean evolvePopulation(ArrayList<Candidate> molPopulation,
                                String genDir) throws DENOPTIMException
    {
        // temporary store for inchi codes
        ArrayList<String> inchisInPop = EAUtils.getInchiCodes(molPopulation);

        // keep a clone of the current population for the parents to be
        // chosen from
        ArrayList<Candidate> clone_popln;
        synchronized (molPopulation)
        {
            clone_popln = new ArrayList<Candidate>();
            for (Candidate m : molPopulation)
            {
                clone_popln.add(m.clone());
            }
        }

        int newPopSize = GAParameters.getNumberOfChildren() + clone_popln.size();

        int f0 = 0, f1 = 0, f2 = 0;

        Integer numTries = 0;
        int MAX_TRIES = GAParameters.getMaxTriesFactor();

        String molName, inchi, smiles;

        int Xop = -1, Mop = -1, Bop = -1;
        int MAX_EVOLVE_ATTEMPTS = 10;

        try
        {
            //TODO-V3: adapt to allow synchronous AND asynchronous parallelization scheme
            /*
             * Need to "pause" the submission of tasks when there are no more
             * seats for new children, and launch only one of the xover kids when 
             * there is only 1 seat for a new children. 
             * Then, wait for completion of the previously submitted tasks, and
             * if any fails, create only the new tasks needed to fulfil
             * GAParameters.getNumberOfChildren().
             */
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
                    if (numTries == MAX_TRIES)
                    {
                        cleanupCompleted(tcons, futures, submitted);
                        break;
                    }
                }

                synchronized (molPopulation)
                {
                    if (molPopulation.size() == newPopSize 
                            || molPopulation.size() > newPopSize)
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
                                           genDir, molPopulation,
                                           numTries,GAParameters.getUIDFileOut());

                    //TODO-V3 make submission dependent on GAParameters.parallelizationScheme? 
                    // so to remove the duplicated code in classes EvolutionaryAlgorithm and ParallelEvolutionaryAlgorithm
                    submitted.add(task);
                    futures.add(tcons.submit(task));

                    synchronized (molPopulation)
                    {
                        if (molPopulation.size() == newPopSize 
                                || molPopulation.size() > newPopSize)
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
                                           cmol, genDir, molPopulation,
                                           numTries,GAParameters.getUIDFileOut());

                        //TODO-V3 make submission dependent on GAParameters.parallelizationScheme? 
                        // so to remove the duplicated code in classes EvolutionaryAlgorithm and ParallelEvolutionaryAlgorithm
                        submitted.add(task1);
                        futures.add(tcons.submit(task1));

                        synchronized (molPopulation)
                        {
                            if (molPopulation.size() == newPopSize 
                                    || molPopulation.size() > newPopSize)
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

                        int taskId = TaskUtils.getUniqueTaskIndex();

                        smiles = res2[1].toString().trim();
                        IAtomContainer cmol = (IAtomContainer) res2[2];

                        OffspringEvaluationTask task2 = new OffspringEvaluationTask(molName, graph2, inchi, smiles,
                                                cmol, genDir, molPopulation,
                                                numTries, 
                                                GAParameters.getUIDFileOut());

                        //TODO-V3 make submission dependent on GAParameters.parallelizationScheme? 
                        // so to remove the duplicated code in classes EvolutionaryAlgorithm and ParallelEvolutionaryAlgorithm
                        
                        submitted.add(task2);
                        futures.add(tcons.submit(task2));

                        synchronized (molPopulation)
                        {
                            if (molPopulation.size() == newPopSize || molPopulation.size() > newPopSize)
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
                                                cmol, genDir, molPopulation,
                                                numTries, 
                                                GAParameters.getUIDFileOut());

                        //TODO-V3 make submission dependent on GAParameters.parallelizationScheme? 
                        // so to remove the duplicated code in classes EvolutionaryAlgorithm and ParallelEvolutionaryAlgorithm
                        
                        submitted.add(task3);
                        futures.add(tcons.submit(task3));

                        synchronized (molPopulation)
                        {
                            if (molPopulation.size() == newPopSize || molPopulation.size() > newPopSize)
                            {
                                break;
                            }
                        }
                    }
                }
            } // end while
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
        synchronized (molPopulation)
        {
            Collections.sort(molPopulation, Collections.reverseOrder());
        }
        
        if (GAParameters.getReplacementStrategy() == 1)
        {
            synchronized(molPopulation)
            {
                int k = molPopulation.size();

                // trim the population to the desired size
                for (int l=GAParameters.getPopulationSize(); l<k; l++)
                {
                    molPopulation.get(l).cleanup();
                }

                // trim the population to the desired size
                molPopulation.subList(
                        GAParameters.getPopulationSize(), k).clear();
            }
        }
        
        cleanup(clone_popln);

        // check if the new population contains a children produced here
        // If yes, return true
        
        boolean updated = false;
        for (int i=0; i<molPopulation.size(); i++)
        {
            if (!inchisInPop.contains(molPopulation.get(i).getUID()))
            {
                updated = true;
                break;
            }
        }
        
        inchisInPop.clear();
        return updated;
    }

//------------------------------------------------------------------------------

    private void initializePopulation(ArrayList<Candidate> population,
                String genDir) throws DENOPTIMException
    {
        final int MAX_TRIES = GAParameters.getPopulationSize() * 
                                              GAParameters.getMaxTriesFactor();

        if (population.size() == GAParameters.getPopulationSize())
        {
            synchronized (population)
            {
                Collections.sort(population, Collections.reverseOrder());
            }
            return;
        }
        else if (GAParameters.getReplacementStrategy() == 1)
        {
            if (population.size() > GAParameters.getPopulationSize())
            {
                // sort the population
                synchronized (population)
                {
                    Collections.sort(population, Collections.reverseOrder());
                    int k = population.size();
                    
                    // trim the population to the desired size
                    for (int l=GAParameters.getPopulationSize(); l<k; l++)
                    {
                        population.get(l).cleanup();
                    }
    
                    // trim the population to the desired size
                    population.subList(
                            GAParameters.getPopulationSize(),k).clear();
                }
            }
        }

        Integer numTries = 0;

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
//                      MF: the cleanup method removed also uncompleted tasks
//                      causing their results to be forgotten.
                        cleanupCompleted(tcons, futures, submitted);
//                      cleanup(tcons, futures, submitted);
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


                // generate a random graph
                DENOPTIMGraph molGraph = EAUtils.buildGraph();
                if (molGraph == null)
                {
                    synchronized(numTries)
                    {
                        numTries++;
                    }
                    continue;
                }
                // check if the graph is valid
                
                Object[] res = EAUtils.evaluateGraph(molGraph);

                if (res != null)
                {
                    if (!EAUtils.setupRings(res,molGraph))
                    {
                        res = null;
                    }
                }
                
                // Check if the chosen combination gives rise to forbidden ends
                //TODO-V3 this should be considered already when making the list of
                // possible combination of rings
                for (DENOPTIMVertex rcv : molGraph.getFreeRCVertices())
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
                    molGraph.cleanup();
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
                        if(numTries > 0)
                            numTries--;
                    }
                }
                    

                // Create the task
                String inchi = res[0].toString().trim();
                String molName = "M" + GenUtils.getPaddedString(
                		DENOPTIMConstants.MOLDIGITS,
                		GraphUtils.getUniqueMoleculeIndex());

                String smiles = res[1].toString().trim();
                IAtomContainer cmol = (IAtomContainer) res[2];

                OffspringEvaluationTask task = new OffspringEvaluationTask(
                        molName, molGraph, inchi, smiles, cmol, 
                        genDir, population, numTries,
                        GAParameters.getUIDFileOut());
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


        if (numTries == MAX_TRIES)
        {
            stopRun();

            DENOPTIMLogger.appLogger.log(Level.SEVERE,
                    "Unable to initialize molecules in {0} attempts.\n", numTries);
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

    private void cleanup(ThreadPoolExecutor tcons, List<Future<Object>> futures,
                            ArrayList<OffspringEvaluationTask> submitted)
    {
        for (Future<Object> f : futures)
        {
            f.cancel(true);
        }

        for (OffspringEvaluationTask tsk: submitted)
        {
            tsk.stopTask();
        }

        submitted.clear();

        tcons.getQueue().clear();
    }

//------------------------------------------------------------------------------

    private void cleanupCompleted(ThreadPoolExecutor tcons,
                                  List<Future<Object>> futures,
                                      ArrayList<OffspringEvaluationTask> submitted)
    {
        ArrayList<Integer> completed = new ArrayList<>();

        for (int ift=0; ift<submitted.size(); ift++)
        {
            if (submitted.get(ift).isCompleted())
                completed.add(ift);
        }

        for (int ift=completed.size(); ift>0; ift--)
        {
            submitted.remove(submitted.get(ift-1));
            futures.remove(futures.get(ift-1));
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
