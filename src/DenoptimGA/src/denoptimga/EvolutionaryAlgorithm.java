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

import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMMolecule;

import denoptim.task.DENOPTIMTask;
import denoptim.task.DENOPTIMTaskManager;
import denoptim.utils.GenUtils;
import denoptim.utils.GraphUtils;
import denoptim.utils.TaskUtils;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.utils.RandomUtils;



/**
 *
 * @author Vishwesh Venkatraman
 */


public class EvolutionaryAlgorithm
{
    private final String fsep = System.getProperty("file.separator");

    public void runGA() throws DENOPTIMException
    {
        StopWatch watch = new StopWatch();
             watch.start();
        
        StringBuilder sb = new StringBuilder(32);

        int ndigits = String.valueOf(GAParameters.getNumberOfGenerations()).length();
        sb.append(GAParameters.getDataDirectory()).append(fsep).append("Gen")
                        .append(GenUtils.getPaddedString(ndigits, 0));
        String genDir = sb.toString();
        sb.setLength(0);
        // create the directory for the current generation
        DenoptimIO.createDirectory(genDir);

        // create a fragment pool based on the number of attachment points
        if (!FragmentSpaceParameters.useAPclassBasedApproach())
            EAUtils.poolFragments(FragmentSpace.getFragmentLibrary());
        else
        {
            // create a fragment pool based on the reactions each fragment
            // is associated with
            EAUtils.poolAPClasses(FragmentSpace.getFragmentLibrary());
        }

        // create the population

        // store all unique molecule ids (can be inchi codes)
        HashSet<String> lstUID = new HashSet<>(1024);

        // first collect UIDs of previously known individuals
        if (!GAParameters.getUIDFileIn().equals(""))
        {
            EAUtils.readUID(GAParameters.getUIDFileIn(),lstUID);
            EAUtils.writeUID(GAParameters.getUIDFileOut(),lstUID,false);
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
                    "New molecules introduced in Generation {0}\n", curGen);
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

        ArrayList<DENOPTIMTask> tasks = new ArrayList<>();
        
        double sdev_old = EAUtils.getPopulationSD(molPopulation);
        

        // keep a clone of the current population for the parents to be
        // chosen from
        ArrayList<DENOPTIMMolecule> clone_popln =
                (ArrayList<DENOPTIMMolecule>) DenoptimIO.deepCopy(molPopulation);

        int Xop = -1, Mop = -1, Bop = -1;

        int MAX_EVOLVE_ATTEMPTS = 10;


        int n = GAParameters.getNumberOfChildren() + molPopulation.size();

        int f0 = 0, f1 = 0, f2 = 0;


        while (molPopulation.size() < n)
        {
            DENOPTIMGraph graph1 = null, graph2 = null, graph3 = null,
                    graph4 = null;
            Xop = -1;
            Mop = -1;
            Bop = -1;

            //System.err.println("Selected parents: " + i1 + " " + i2);

            // Do CROSSOVER if probabilistically true
            if (RandomUtils.nextBoolean(GAParameters.getCrossoverProbability()))
            //if (GenUtils.nextBoolean(GAParameters.getRNG(), GAParameters.getCrossoverProbability()))
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

                    //System.err.println("MALE: " + molPopulation.get(i1).getMoleculeGraph().toString());
                    //System.err.println("FEMALE: " + molPopulation.get(i2).getMoleculeGraph().toString());

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
            //if (GenUtils.nextBoolean(GAParameters.getRNG(), GAParameters.getMutationProbability()))
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
                if (graph4 != null)
                {
                    Object[] res = EAUtils.evaluateGraph(graph4);

                    if (res != null)
                    {
                        if (!EAUtils.setupRings(res,graph4))
                        {
                            res = null;
                        }
                    }

//TODO: to test more thoroughly
/*
                    if (res != null)
                    {
                            VisitedGraphsHandler vgh = new VisitedGraphsHandler(
                                        GAParameters.getVisitedGraphsFile(),
                                        GAParameters.getScaffoldLibrary(),
                                        FragmentSpace.getFragmentLibrary(),
                                        GAParameters.getCappingLibrary(),
                                        GAParameters.getBondOrderMap());

                        if (!vgh.appendGraph(graph4))
                        {
                            res = null;
                        }
                    }
*/
                                
                    if (res == null)
                    {
                        graph4.cleanup();
                        graph4 = null;
                    }
                    else if (addTask(tasks, molPopulation.size(), graph4, res, genDir, n))
                    {
                        ArrayList<DENOPTIMMolecule> results =
                                DENOPTIMTaskManager.executeTasks(tasks,
                                                GAParameters.getNumberOfCPU());
                        tasks.clear();

                        if (results != null && results.size() > 0)
                        {
                            molPopulation.addAll(results);
                        }

                        if (molPopulation.size() == n)
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

//TODO: to test more thoroughly
/*
                    if (res1 != null)
                    {
                        VisitedGraphsHandler vgh = new VisitedGraphsHandler(
                                        GAParameters.getVisitedGraphsFile(),
                                        GAParameters.getScaffoldLibrary(),
                                        FragmentSpace.getFragmentLibrary(),
                                        GAParameters.getCappingLibrary(),
                                        GAParameters.getBondOrderMap());

                        if (!vgh.appendGraph(graph1))
                        {
                            res1 = null;
                        }
                    }
*/
                    if (res1 == null)
                    {
                        graph1.cleanup();
                        graph1 = null;
                    }
                    else if (addTask(tasks, molPopulation.size(), graph1, res1, genDir, n))
                    {
                        ArrayList<DENOPTIMMolecule> results =
                                DENOPTIMTaskManager.executeTasks(tasks,
                                                GAParameters.getNumberOfCPU());
                        tasks.clear();
                        if (results != null && results.size() > 0)
                        {
                            molPopulation.addAll(results);
                        }

                        if (molPopulation.size() == n)
                            break;
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


//TODO: to test more thoroughly
/*
                    if (res2 != null)
                    {
                        VisitedGraphsHandler vgh = new VisitedGraphsHandler(
                                        GAParameters.getVisitedGraphsFile(),
                                        GAParameters.getScaffoldLibrary(),
                                        FragmentSpace.getFragmentLibrary(),
                                        GAParameters.getCappingLibrary(),
                                        GAParameters.getBondOrderMap());

                        if (!vgh.appendGraph(graph2))
                        {
                            res2 = null;
                        }
                    }
*/

                    if (res2 == null)
                    {
                        graph2.cleanup();
                        graph2 = null;
                    }
                    else if (addTask(tasks, molPopulation.size(), graph2, res2, genDir, n))
                    {
                        ArrayList<DENOPTIMMolecule> results =
                                DENOPTIMTaskManager.executeTasks(tasks,
                                                GAParameters.getNumberOfCPU());
                        tasks.clear();

                        if (results != null && results.size() > 0)
                        {
                            molPopulation.addAll(results);
                        }

                        if (molPopulation.size() == n)
                            break;
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


//TODO: to test more thoroughly
/*
                    if (res3 != null)
                    {
                        VisitedGraphsHandler vgh = new VisitedGraphsHandler(
                                        GAParameters.getVisitedGraphsFile(),
                                        GAParameters.getScaffoldLibrary(),
                                        FragmentSpace.getFragmentLibrary(),
                                        GAParameters.getCappingLibrary(),
                                        GAParameters.getBondOrderMap());

                        if (!vgh.appendGraph(graph3))
                        {
                            res3 = null;
                        }
                    }
*/

                    if (res3 == null)
                    {
                        graph3.cleanup();
                        graph3 = null;
                    }
                    else if (addTask(tasks, molPopulation.size(), graph3, res3, genDir, n))
                    {
                        ArrayList<DENOPTIMMolecule> results =
                                DENOPTIMTaskManager.executeTasks(tasks,
                                                GAParameters.getNumberOfCPU());
                        tasks.clear();

                        if (results != null && results.size() > 0)
                        {
                            molPopulation.addAll(results);
                        }

                        if (molPopulation.size() == n)
                            break;
                    }
                }
            }
        } // end while

        StringBuilder sb = new StringBuilder(256);
        sb.append("Crossover Attempted: ").append(f0).append("\n");
        sb.append("Mutation Attempted: ").append(f1).append("\n");
        sb.append("New Molecule Attempted: ").append(f2).append("\n");

        DENOPTIMLogger.appLogger.info(sb.toString());

        // sort the population
        Collections.sort(molPopulation, Collections.reverseOrder());

        if (GAParameters.getReplacementStrategy() == 1)
        {

            int k = molPopulation.size();

            // trim the population to the desired size
            for (int l=GAParameters.getPopulationSize(); l<k; l++)
            {
                molPopulation.get(l).cleanup();
            }
            
            molPopulation.subList(GAParameters.getPopulationSize(), k).clear();
        }
        
        cleanup(clone_popln);
        tasks.clear();

        // check if the new population contains a molecule from the children
        // produced. If yes, return true
        boolean updated = false;
        
        for (DENOPTIMMolecule mol : molPopulation)
        {
            if (!codes.contains(mol.getMoleculeUID()))
            {
                updated = true;
                break;
            }
        }
        
        codes.clear();
        return updated;
    }

//------------------------------------------------------------------------------

    /**
     * @param population of molecules
     * @param current task queue
     * @param inchicode, smiles and 2D representation - array
     * @param the current working directory for storing output results
     * @return <code>true</code> if task list has sufficient number of tasks
     *         for parallel processing
     */

    private boolean addTask(ArrayList<DENOPTIMTask> tasks, int cursize,
                            DENOPTIMGraph molGraph, Object[] res, String wrkDir,
                            int n) throws DENOPTIMException
    {
        if (res == null)
            return false;
        
        // check if the molinchi has been encountered before
        String inchi = "";
        if (res[0] != null)
            inchi = res[0].toString().trim();

        // file extensions will be added later
        String molName = "M" + GenUtils.getPaddedString(
                                                   DENOPTIMConstants.MOLDIGITS,
                                           GraphUtils.getUniqueMoleculeIndex());

        int taskId = TaskUtils.getUniqueTaskIndex();

        String smiles = "";
        if (res[1] != null)
            smiles = res[1].toString().trim();

        DENOPTIMTask task = new FitnessTask(molName, molGraph,
                                inchi, smiles, (IAtomContainer) res[2],
                                wrkDir, taskId, GAParameters.getUIDFileOut());

        tasks.add(task);

        // check if there are enough jobs in the queue for processing
        //int p = tasks.size() % GAParameters.getNumberOfCPU();
        int q = Math.abs(cursize - n);

        return tasks.size() >= q;
    }


//------------------------------------------------------------------------------

    /**
     * creates a population of molecules
     * In creating the population of specified size (say n), 3*n trials are
     * conducted. If the desired population size is not achieved, the
     * program throws and exception.
     * @param molPopulation the population to be initialized
     * @param genDir output directory associated with the current population
     * @throws DENOPTIMException
     */

    private void initializePopulation(ArrayList<DENOPTIMMolecule> molPopulation,
                                        String genDir) throws DENOPTIMException
    {
        final int MAX_TRIES = GAParameters.getPopulationSize() * 
                                             GAParameters.getMaxTriesFactor();
        int t = 0;

        int npop = GAParameters.getPopulationSize();

        if (molPopulation.size() == npop)
        {
            Collections.sort(molPopulation, Collections.reverseOrder());
            return;
        }
        else if (GAParameters.getReplacementStrategy() == 1)
        {
            if (molPopulation.size() > npop)
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
                molPopulation.subList(npop, k).clear();
            }
        }

        ArrayList<DENOPTIMTask> tasks = new ArrayList<>();

        while (molPopulation.size() < GAParameters.getPopulationSize())
        {
            if (t == MAX_TRIES)
                break;

            // generate a random graph
            DENOPTIMGraph molGraph = EAUtils.buildGraph();
            //System.err.println(molGraph.toString());
            if (molGraph == null)
                continue;

            // check if the graph is valid
            Object[] res = EAUtils.evaluateGraph(molGraph);

            if (res != null)
            {
                if (!EAUtils.setupRings(res,molGraph))
                {
                    res = null;
                }
            }

//TODO: to test more thoroughly and combine with more efficient storage of 
// the visited graphs
/*
            if (res != null)
            {
                VisitedGraphsHandler vgh = new VisitedGraphsHandler(
                        "/scratch/test_visitedGraphs/OLDGRAPHS.txt",
//TODO back to this                    GAParameters.getVisitedGraphsFile(),
                                GAParameters.getScaffoldLibrary(),
                                FragmentSpace.getFragmentLibrary(),
                                GAParameters.getCappingLibrary(),
                                GAParameters.getBondOrderMap());
*/
/*
String strGraph1 = ADD HERE THE STRING REPRESENTATION OF A GRAPH FOR TESTING
DENOPTIMGraph graphTest1 = GraphUtils.getGraphFromString(strGraph1,
                                GAParameters.getScaffoldLibrary(),
                                FragmentSpace.getFragmentLibrary(),
                                GAParameters.getCappingLibrary(),
                                GAParameters.getBondOrderMap());

boolean test1 = vgh.appendGraph(graphTest1);
System.out.println("END OF TEST 1: "+test1);
GenUtils.pause();




                if (!vgh.appendGraph(molGraph))
                {
//TODO del
System.out.println("=== Graph has a duplicate in database ==========");
GenUtils.pause();
                    res = null;
                }
            }
*/

            if (res == null)
            {
                molGraph.cleanup();
                continue;
            }

            // add to the task list for further processing
            if (addTask(tasks, molPopulation.size(), molGraph, res, genDir, npop))
            {
                //System.err.println("Launching batch of: " + tasks.size());
                // decrement tries
                t += tasks.size();

                ArrayList<DENOPTIMMolecule> results =
                        DENOPTIMTaskManager.executeTasks(tasks,
                                            GAParameters.getNumberOfCPU());
                tasks.clear();

                // add results to the population
                if (results != null && results.size() > 0)
                {
                    molPopulation.addAll(results);
                    // decrement tries
                    if (t > 0)
                        t -= tasks.size();
                }

                if (molPopulation.size() == GAParameters.getPopulationSize())
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
    
    private void cleanup(ArrayList<DENOPTIMMolecule> popln)
    {
        for (DENOPTIMMolecule mol:popln)
            mol.cleanup();
        popln.clear();
    }
    
//------------------------------------------------------------------------------    

}
