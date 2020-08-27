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
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMMolecule;
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
    final List<Future<String>> futures;
    final ArrayList<FTask> submitted;
    final ThreadPoolExecutor tcons;
   
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
        for (FTask tsk:submitted)
        {
            if (tsk.foundException())
            {
                hasprobs = true;
	        DENOPTIMLogger.appLogger.log(Level.SEVERE, "problems in " 
							      + tsk.toString());
		DENOPTIMLogger.appLogger.log(Level.SEVERE,
							 tsk.getErrorMessage());
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

        // create a fragment pool based on the number of attachment points
        if (!FragmentSpace.useAPclassBasedApproach()) {
            EAUtils.poolFragments(FragmentSpace.getFragmentLibrary());
        }
        else
        {
            // create a fragment pool based on the reactions each fragment
            // is associated with
            EAUtils.poolAPClasses(FragmentSpace.getFragmentLibrary());
        }

        // store all inchi keys
        HashSet<String> lstUID = new HashSet<>(1024);

        // first collect UIDs of previously known individuals
	if (!GAParameters.getUIDFileIn().equals(""))
	{
//TODO: if same file avoid re-write
            EAUtils.readUID(GAParameters.getUIDFileIn(),lstUID);
            EAUtils.writeUID(GAParameters.getUIDFileOut(),lstUID,false); //overwrite
	}

        // placeholder for the molecules
        ArrayList<DENOPTIMMolecule> molPopulation = new ArrayList<>();

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

            sb.append(genDir).append(fsep).append("Gen")
                .append(GenUtils.getPaddedString(ndigits, curGen)).append(".txt");

            // dump population details to file
            genOutfile = sb.toString();
            sb.setLength(0);

            EAUtils.outputPopulationDetails(molPopulation, genOutfile);
            
            DENOPTIMLogger.appLogger.log(Level.INFO,"Generation {0}" + " completed\n"
                            + "----------------------------------------"
                            + "----------------------------------------\n", curGen);

            // check for stagnation
            if (numStag >= GAParameters.getNumberOfConvergenceGenerations())
            {
                // write a log message
                DENOPTIMLogger.appLogger.log(Level.WARNING,
                    "No change in population over {0} iterations. Stopping EA. \n",
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
    private boolean evolvePopulation(ArrayList<DENOPTIMMolecule> molPopulation,
                                String genDir) throws DENOPTIMException
    {
        // temporary store for inchi codes
        ArrayList<String> codes = EAUtils.getInchiCodes(molPopulation);

        //double sdev_old = EAUtils.getPopulationSD(molPopulation);
        
        // keep a clone of the current population for the parents to be
        // chosen from
        ArrayList<DENOPTIMMolecule> clone_popln;

        synchronized (molPopulation)
        {
            clone_popln =
                (ArrayList<DENOPTIMMolecule>) DenoptimIO.deepCopy(molPopulation);
        }

        int n = GAParameters.getNumberOfChildren() + clone_popln.size();

        int f0 = 0, f1 = 0, f2 = 0;

        Integer numtry = 0;
        int MAX_TRIES = GAParameters.getMaxTriesFactor();

        //System.err.println("EvolvePopulation " + genDir);
        String molName, inchi, smiles;

        int Xop = -1, Mop = -1, Bop = -1;
        int MAX_EVOLVE_ATTEMPTS = 10;

        try
        {
            while (true)
            {
                if (checkForException())
                {
                    stopRun();
                    throw new DENOPTIMException("Errors found during execution.");
                }

                synchronized (numtry)
                {
                    if (numtry == MAX_TRIES)
                    {
//                        MF: the cleanup method removed also uncompleted tasks
//                        causing their results to be forgotten.
                        cleanupCompleted(tcons, futures, submitted);
//                      cleanup(tcons, futures, submitted);
                        break;
                    }
                }

                synchronized (molPopulation)
                {
                    //System.err.println("PSIZE: " + molPopulation.size());
                    if (molPopulation.size() == n || molPopulation.size() > n)
                    {
                        break;
                    }
                }

                DENOPTIMGraph graph1 = null, graph2 = null, graph3 = null,
                    graph4 = null;
                Xop = -1;
                Mop = -1;
                Bop = -1;


                //System.err.println("Selected parents: " + i1 + " " + i2);

                if (RandomUtils.nextBoolean(GAParameters.getCrossoverProbability()))
                //if (GenUtils.nextBoolean(GAParameters.getRNG(), 
                //        GAParameters.getCrossoverProbability()))
                {
                    int numatt = 0;
                    int i1 = -1, i2 = -1;
                    boolean foundPars = false;

                    while (numatt < MAX_EVOLVE_ATTEMPTS)
                    {
                        int parents[] = EAUtils.selectParents(clone_popln);
                        if (parents[0] == -1 || parents[1] == -1)
                        {
                            DENOPTIMLogger.appLogger.info("Failed to identify compatible parents for crossover/mutation.");
                            continue;
                        }

                        // perform crossover
                        if (parents[0] == parents[1])
                        {
                            DENOPTIMLogger.appLogger.info("Crossover has indentical partners.");
                            numatt++;
                            continue;
                        }

                        numatt = 0;
                        i1 = parents[0];
                        i2 = parents[1];
                        foundPars = true;
                        break;
                    }

                    if (foundPars)
                    {
                        String molid1 = FilenameUtils.getBaseName(clone_popln.get(i1).getMoleculeFile());
                        String molid2 = FilenameUtils.getBaseName(clone_popln.get(i2).getMoleculeFile());

                        int gid1 = clone_popln.get(i1).getMoleculeGraph().getGraphId();
                        int gid2 = clone_popln.get(i2).getMoleculeGraph().getGraphId();


                        //System.err.println("MALE: " + clone_popln.get(i1).getMoleculeGraph().toString());
                        //System.err.println("FEMALE: " + clone_popln.get(i2).getMoleculeGraph().toString());


                        // clone the parents
                        graph1 = (DENOPTIMGraph) DenoptimIO.deepCopy
                                        (clone_popln.get(i1).getMoleculeGraph());
                        graph2 = (DENOPTIMGraph) DenoptimIO.deepCopy
                                        (clone_popln.get(i2).getMoleculeGraph());

                        f0 += 2;

                        if (DENOPTIMGraphOperations.performCrossover(graph1, graph2))
                        {
                            graph1.setGraphId(GraphUtils.getUniqueGraphIndex());
                            graph2.setGraphId(GraphUtils.getUniqueGraphIndex());
                            EAUtils.addCappingGroup(graph1);
                            EAUtils.addCappingGroup(graph2);
                            Xop = 1;

                            graph1.setMsg("Xover: " + molid1 + "|" + gid1 +
                                                "=" + molid2 + "|" + gid2);
                            graph2.setMsg("Xover: " + molid1 + "|" + gid1 +
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

                if (RandomUtils.nextBoolean(GAParameters.getMutationProbability()))
                //if (GenUtils.nextBoolean(GAParameters.getRNG(), 
                //    GAParameters.getMutationProbability()))
                {
                    // select mutation
                    // select a random parent from the pool

                    int numatt = 0;
                    int i3 = -1;
                    boolean foundPars = false;
                    while (numatt < MAX_EVOLVE_ATTEMPTS)
                    {
                        i3 = EAUtils.selectSingleParent(clone_popln);
                        if (i3 == -1)
                        {
                            DENOPTIMLogger.appLogger.info("Invalid parent selection.");
                            numatt++;
                            continue;
                        }
                        foundPars = true;
                        break;
                    }

                    if (foundPars)
                    {
                        //System.err.println("SELECTING MUTATION");
                        graph3 = (DENOPTIMGraph) DenoptimIO.deepCopy
                                            (clone_popln.get(i3).getMoleculeGraph());
                        f1 += 1;

                        String molid3 = FilenameUtils.getBaseName(clone_popln.get(i3).getMoleculeFile());
                        int gid3 = clone_popln.get(i3).getMoleculeGraph().getGraphId();

                        if (EAUtils.performMutation(graph3))
                        {
                            graph3.setGraphId(GraphUtils.getUniqueGraphIndex());
                            Mop = 1;
                            graph3.setMsg("Mutation: " + molid3 + "|" + gid3);
                        }
                        EAUtils.addCappingGroup(graph3);
                    }
                    else
                    {
                        if (graph3 != null)
                        {
                            graph3.cleanup();
                        }
                        graph3 = null;
                    }
                }

                if (Xop == -1 && Mop == -1)
                {
                    f2++;
                    graph4 = EAUtils.buildGraph();

                    if (graph4 != null)
                    {
                        Bop = 1;
                        graph4.setMsg("NEW");
                    }
                }

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

                    if (res == null)
                    {
                        synchronized(numtry)
                        {
                            numtry++;
                        }
                        continue;
                    }
                    else
                    {
                        synchronized(numtry)
                        {
                            numtry = 0;
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
                    molName = "M" + GenUtils.getPaddedString(DENOPTIMConstants.MOLDIGITS,
                                            GraphUtils.getUniqueMoleculeIndex());

                    int taskId = TaskUtils.getUniqueTaskIndex();

                    smiles = res[1].toString().trim();
                    IAtomContainer cmol = (IAtomContainer) res[2];

                    FTask task = new FTask(molName, graph4, inchi, smiles, cmol,
                                           genDir, taskId, molPopulation,
                                           numtry,GAParameters.getUIDFileOut());

                    submitted.add(task);
                    futures.add(tcons.submit(task));

                    synchronized (molPopulation)
                    {
                        //System.err.println("PSIZE: " + molPopulation.size());

                        if (molPopulation.size() == n || molPopulation.size() > n)
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

                        if (res1 == null)
                        {
                            synchronized(numtry)
                            {
                                numtry++;
                            }
                            continue;
                        }
                        else
                        {
                            synchronized(numtry)
                            {
                                numtry = 0;
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
                        molName = "M" +
                                GenUtils.getPaddedString(DENOPTIMConstants.MOLDIGITS,
                                                GraphUtils.getUniqueMoleculeIndex());

                        int taskId = TaskUtils.getUniqueTaskIndex();

                        smiles = res1[1].toString().trim();
                        IAtomContainer cmol = (IAtomContainer) res1[2];

                        FTask task1 = new FTask(molName, graph1, inchi, smiles,
                                           cmol, genDir, taskId, molPopulation,
                                           numtry,GAParameters.getUIDFileOut());

                        submitted.add(task1);
                        futures.add(tcons.submit(task1));

                        synchronized (molPopulation)
                        {
                            if (molPopulation.size() == n || molPopulation.size() > n)
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

                        if (res2 == null)
                        {
                            synchronized(numtry)
                            {
                                numtry++;
                            }
                            continue;
                        }
                        else
                        {
                            synchronized(numtry)
                            {
                                numtry = 0;
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
                        molName = "M" +
                                GenUtils.getPaddedString(DENOPTIMConstants.MOLDIGITS,
                                                GraphUtils.getUniqueMoleculeIndex());

                        int taskId = TaskUtils.getUniqueTaskIndex();

                        smiles = res2[1].toString().trim();
                        IAtomContainer cmol = (IAtomContainer) res2[2];

                        FTask task2 = new FTask(molName, graph2, inchi, smiles,
                                                cmol, genDir, taskId, molPopulation,
                                                numtry, 
                                                GAParameters.getUIDFileOut());

                        submitted.add(task2);
                        futures.add(tcons.submit(task2));

                        synchronized (molPopulation)
                        {
                            if (molPopulation.size() == n || molPopulation.size() > n)
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

                        if (res3 == null)
                        {
                            synchronized(numtry)
                            {
                                numtry++;
                            }
                            continue;
                        }
                        else
                        {
                            synchronized(numtry)
                            {
                                numtry = 0;
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

                        int taskId = TaskUtils.getUniqueTaskIndex();

                        smiles = res3[1].toString().trim();
                        IAtomContainer cmol = (IAtomContainer) res3[2];

                        FTask task3 = new FTask(molName, graph3, inchi, smiles,
                                                cmol, genDir, taskId, molPopulation,
                                                numtry, 
                                                GAParameters.getUIDFileOut());

                        submitted.add(task3);
                        futures.add(tcons.submit(task3));

                        synchronized (molPopulation)
                        {
                            if (molPopulation.size() == n || molPopulation.size() > n)
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

//        System.err.println("After (*X*).");
//        for (FitnessTask z : submitted)
//        {
//            System.err.println(z.toString());
//        }

        StringBuilder sb = new StringBuilder(256);
        sb.append("Crossover Attempted: ").append(f0).append("\n");
        sb.append("Mutation Attempted: ").append(f1).append("\n");
        sb.append("New Molecule Attempted: ").append(f2).append("\n");

        DENOPTIMLogger.appLogger.info(sb.toString());
        sb.setLength(0);

        // sort the population
        Collections.sort(molPopulation, Collections.reverseOrder());


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
                molPopulation.subList(GAParameters.getPopulationSize(), k).clear();
            }
        }
        
        cleanup(clone_popln);

        // check if the new population contains a molecule from the children
        // produced. If yes, return true
        
        boolean updated = false;
        for (int i=0; i<molPopulation.size(); i++)
        {
            if (!codes.contains(molPopulation.get(i).getMoleculeUID()))
            {
                updated = true;
                break;
            }
        }
        
        //double sdev_new = EAUtils.getPopulationSD(molPopulation);
        //if (Math.abs(sdev_new - sdev_old) < 0.01)
        //    updated = false;
        
        codes.clear();
        return updated;
    }

//------------------------------------------------------------------------------

    private void initializePopulation(ArrayList<DENOPTIMMolecule> molPopulation,
                String genDir) throws DENOPTIMException
    {
        final int MAX_TRIES = GAParameters.getPopulationSize() * 
                                              GAParameters.getMaxTriesFactor();

        if (molPopulation.size() == GAParameters.getPopulationSize())
        {
            Collections.sort(molPopulation, Collections.reverseOrder());
            return;
        }
        else if (GAParameters.getReplacementStrategy() == 1)
        {
            if (molPopulation.size() > GAParameters.getPopulationSize())
            {
                // sort the population
                Collections.sort(molPopulation, Collections.reverseOrder());
                int k = molPopulation.size();
                
                // trim the population to the desired size
                for (int l=GAParameters.getPopulationSize(); l<k; l++)
                {
                    molPopulation.get(l).cleanup();
                }

                // trim the population to the desired size
                molPopulation.subList(GAParameters.getPopulationSize(), k).clear();
            }
        }

        Integer numtry = 0;

        try
        {
            while (true)
            {
                if (checkForException())
                {
                    stopRun();
                    throw new DENOPTIMException("Errors found during execution.");
                }
                synchronized (numtry)
                {
                    if (numtry == MAX_TRIES)
                    {
//                      MF: the cleanup method removed also uncompleted tasks
//                      causing their results to be forgotten.
                        cleanupCompleted(tcons, futures, submitted);
//                      cleanup(tcons, futures, submitted);
                        break;
                    }
                }

                synchronized (molPopulation)
                {
                    //System.err.println("PSIZE: " + molPopulation.size());
                    if (molPopulation.size() == GAParameters.getPopulationSize() ||
                            molPopulation.size() > GAParameters.getPopulationSize())
                    {
                        break;
                    }
                }


                // generate a random graph
                DENOPTIMGraph molGraph = EAUtils.buildGraph();
                //System.err.println(molGraph.toString());

                if (molGraph == null)
                {
                    synchronized(numtry)
                    {
                        numtry++;
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

                if (res == null)
                {
                    molGraph.cleanup();
                    synchronized(numtry)
                    {
                        numtry++;
                    }
                    continue;
                }
                else
                {
                    synchronized(numtry)
                    {
                        if(numtry > 0)
                            numtry--;
                    }
                }
                    

                // Create the task
                // check if the molinchi has been encountered before
                String inchi = res[0].toString().trim();
                
                /*
                if (lstInchi.contains(inchi))
                {
                    synchronized(numtry)
                    {
                        numtry++;
                    }
                    continue;
                }
                else
                {
                    lstInchi.add(inchi);
                    synchronized(numtry)
                    {
                        numtry = 0;
                    }
                }
                */

                // file extensions will be added later
                String molName = "M" +
                        GenUtils.getPaddedString(DENOPTIMConstants.MOLDIGITS,
                                    GraphUtils.getUniqueMoleculeIndex());

                int taskId = TaskUtils.getUniqueTaskIndex();

                String smiles = res[1].toString().trim();
                IAtomContainer cmol = (IAtomContainer) res[2];

                FTask task = new FTask(molName, molGraph, inchi, smiles, cmol,
                                        genDir, taskId, molPopulation, numtry,
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


        if (numtry == MAX_TRIES)
        {
            stopRun();

            DENOPTIMLogger.appLogger.log(Level.SEVERE,
                    "Unable to initialize molecules in {0} attempts.\n", numtry);
            throw new DENOPTIMException("Unable to initialize molecules in " +
                            numtry + " attempts.");
        }

        molPopulation.trimToSize();

        // sort population by fitness
        Collections.sort(molPopulation, Collections.reverseOrder());
    }

//------------------------------------------------------------------------------

    private void cleanup(ThreadPoolExecutor tcons, List<Future<String>> futures,
                            ArrayList<FTask> submitted)
    {
        for (Future f : futures)
        {
            f.cancel(true);
        }

        for (FTask tsk: submitted)
        {
            tsk.stopTask();
        }

        submitted.clear();

        tcons.getQueue().clear();
    }

//------------------------------------------------------------------------------

    private void cleanupCompleted(ThreadPoolExecutor tcons,
                                  List<Future<String>> futures,
                                      ArrayList<FTask> submitted)
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

    private void cleanup(ArrayList<DENOPTIMMolecule> popln)
    {
        for (DENOPTIMMolecule mol:popln)
            mol.cleanup();
        popln.clear();
    }
    
//------------------------------------------------------------------------------    

}
