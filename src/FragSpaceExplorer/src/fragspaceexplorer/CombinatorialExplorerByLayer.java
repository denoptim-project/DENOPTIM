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

package fragspaceexplorer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragsCombination;
import denoptim.fragspace.FragsCombinationIterator;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.APClass;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMEdge;
import denoptim.molecule.DENOPTIMFragment.BBType;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.molecule.IGraphBuildingBlock;
import denoptim.molecule.SymmetricSet;
import denoptim.utils.GraphUtils;
import denoptim.utils.TaskUtils;


/**
 * Generates all combinators of fragments by means of asynchronous threads.
 * Combinations are produced by layer of fragments on root graphs, which can
 * be single fragments (i.e., scaffolds) or complex graphs.
 *
 * @author Marco Foscato
 */

public class CombinatorialExplorerByLayer
{
    /**
     * Storage of references to the submitted subtasks as <code>Future</code>
     */
    final List<Future<String>> futures;

    /**
     * Storage of references to the submitted subtasks.
     */
    final ArrayList<GraphBuildingTask> submitted;

    /**
     * Asynchronous tasks manager 
     */
    final ThreadPoolExecutor tpe;

    /**
     * Verbosity level
     */
    private int verbosity = FSEParameters.getVerbosity();

    /**
     * Flag indicating to restart from checkpoint file
     */
    private boolean restartFromChkPt =  FSEParameters.restartFromCheckPoint();

    /**
     * Number of serialized graphs recovered from previous run database
     * upon restart from checkpoint file
     */
    private int serFromChkRestart = 0;

    /**
     * If any, here we stores the exception returned by a subtask
     */
    private Throwable thrownByTask;

    /**
     * Flag identifying the first iteration after restart from checkpoint
     */
    private boolean firstAfterRestart = false;


//-----------------------------------------------------------------------------

    /**
     * Constructor
     */

    public CombinatorialExplorerByLayer()
    {
        futures = new ArrayList<>();
        submitted = new ArrayList<>();

        tpe = new ThreadPoolExecutor(FSEParameters.getNumberOfCPU(),
                                       FSEParameters.getNumberOfCPU(), 
                                       Long.MAX_VALUE,
                                       TimeUnit.NANOSECONDS,
                                       //TimeUnit.MILLISECONDS,
                                       new ArrayBlockingQueue<Runnable>(1));

        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                //System.err.println("Calling shutdown.");
                tpe.shutdown(); // Disable new tasks from being submitted
                try
                {
                    // Wait a while for existing tasks to terminate
                    if (!tpe.awaitTermination(30, TimeUnit.SECONDS))
                    {
                        tpe.shutdownNow(); // Cancel currently executing tasks
                    }

                    if (!tpe.awaitTermination(60, TimeUnit.SECONDS))
                    {
                        // pool didn't terminate after the second try
                    }
                }
                catch (InterruptedException ie)
                {
                    cleanup(tpe, futures, submitted);
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
                    if (verbosity > 1)
                    {
                        ex.printStackTrace();
                        String msg = "EXCEPTION in rejectedExecution.";
                        DENOPTIMLogger.appLogger.log(Level.WARNING,msg);
                    }
                }
            }
        });
    }

//------------------------------------------------------------------------------

    /**
     * Stops all subtasks and shutdown executor
     */

    public void stopRun()
    {
        cleanup(tpe, futures, submitted);
        tpe.shutdown();
    }

//------------------------------------------------------------------------------

    /**
     * Looks for exceptions in the subtasks and, if any, store its reference
     * locally to allow reporting it back from the main thread.
     * @return <code>true</code> if any of the subtasks has thrown an exception
     */

    private boolean subtaskHasException()
    {
        boolean hasException = false;
        for (GraphBuildingTask tsk : submitted)
        {
            if (tsk.foundException())
            {
                hasException = true;
                thrownByTask = tsk.getException();
                break;
            }
        }

        return hasException;
    }

//------------------------------------------------------------------------------

    /**
     * Check for completion of all subtasks
     * @return <code>true</code> if all subtasks are completed
     */

    private boolean allTasksCompleted()
    {
        boolean allDone = true;
        for (GraphBuildingTask tsk : submitted)
        {
            if (!tsk.isCompleted())
            {
                allDone = false;
                break;
            }
        }

        return allDone;
    }

//------------------------------------------------------------------------------

    /**
     * Identify the task preceding the earliest non-completed task and use it to
     * create a checkpoint file
     */

    private void makeCheckPoint() throws DENOPTIMException
    {
        for (int i=0; i<submitted.size(); i++)
        {
            GraphBuildingTask tsk = submitted.get(i);
            int idToChkPt = -1;
            boolean storeChkPt = false;
            if (!tsk.isCompleted() && i>0)
            {
                if (submitted.get(i-1).isCompleted())
                {
                    // The last completed task preceeding the first uncompleted
                    idToChkPt = i-1;
                    storeChkPt = true;
                }
            }
            else if (tsk.isCompleted() && (i==submitted.size()-1))
            {
                idToChkPt = i;
                storeChkPt = true;
            }
            if (storeChkPt)
            {
                tsk = submitted.get(idToChkPt);
                FSECheckPoint chk = FSEParameters.getCheckPoint();
                chk.setSafelyCompletedGraphId(tsk.getGraphId());
                chk.setRootId(tsk.getRootId());
                chk.setNextIds(tsk.getNextIds());
                chk.setLevel(tsk.getLevel());
                chk.setUnqVrtId(GraphUtils.getUniqueVertexIndex());
                chk.setUnqGraphId(GraphUtils.getUniqueGraphIndex());
                chk.setUnqMolId(GraphUtils.getUniqueMoleculeIndex());
                FSEUtils.serializeCheckPoint();
                break;
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * @return the number of subtasks submitted, which includes both the
     * tasks submitted by the executor (i.e., child tasks) and any other tasks 
     * possibly submitted by such child tasks.
     */

    private int countSubTasks()
    {
        int tot = 0;
        for (GraphBuildingTask tsk : submitted)
        {
            tot = tot + tsk.getNumberOfSubTasks();
        }

        return tot;
    }

//------------------------------------------------------------------------------

    /**
     * Run the combinatorial exploration 
     */

    public void runPCE() throws DENOPTIMException
    {
        String msg = "";
        StopWatch watch = new StopWatch();
        watch.start();

        tpe.prestartAllCoreThreads();
  
        int level = -1;
        if (restartFromChkPt)
        {
            firstAfterRestart = true;
            FSECheckPoint chk = FSEParameters.getCheckPoint();
            level = chk.getLevel();
            GraphUtils.resetUniqueVertexCounter(chk.getUnqVrtId());
            GraphUtils.resetUniqueGraphCounter(chk.getUnqGraphId());
            GraphUtils.resetUniqueMoleculeCounter(chk.getUnqMolId());

            msg = "Restarting FragSpaseExplorer from checkpoint file. "
                  + DENOPTIMConstants.EOL
                  + "All graphs with ID higher than "
                  + chk.getLatestSafelyCompletedGraphId()
                  + " are now being re-generated. Be aware that this "
                  + "incluses graphs managed either by "
                  + "partially executed or completed tasks of the previous "
                  + "run. "
                  + DENOPTIMConstants.EOL
                  + "To avoid duplicates, you should remove from the results "
                  + "of the previos run all graphs with ID higher than "
                  + chk.getLatestSafelyCompletedGraphId()
                  + ". You can find them in the "
                  + "index file ('" + DENOPTIMConstants.FSEIDXNAMEROOT + level
                  + ".txt'). ";
            DENOPTIMLogger.appLogger.log(Level.WARNING,msg);

            Collection<File> lst = FileUtils.listFiles(
                                  new File(FSEUtils.getNameOfStorageDir(level)),
                                       new String[] {DENOPTIMConstants.SERGFILENAMEEXT},
                                                                         false);
		    // Keep only safely completed serialized graphs
		    serFromChkRestart = lst.size();
		    for (File f : lst)
		    {
				String fName = f.getName();
				int serGrphID = Integer.parseInt(fName.substring(
						DENOPTIMConstants.SERGFILENAMEROOT.length(),
		        fName.length()-DENOPTIMConstants.SERGFILENAMEEXT.length()-1));
				if (serGrphID > chk.getLatestSafelyCompletedGraphId())
				{
				    msg = "Removing non-safely completed graph '" + fName + "'";
		                    DENOPTIMLogger.appLogger.log(Level.WARNING,msg);
				    serFromChkRestart--;
				    DenoptimIO.deleteFile(FSEUtils.getNameOfStorageDir(level) 
							              + File.separator + fName);
				}
		    }
        }

        boolean interrupted = false;
        while (level <= FSEParameters.getMaxLevel())
        {
            msg = "Starting exploration of level " + level; 
            DENOPTIMLogger.appLogger.log(Level.INFO,msg);

            int numSubTasks = exploreCombinationsAtGivenLevel(level);

            if (numSubTasks > 0)
            {
                // wait for pending tasks to finish
                long startTime = System.currentTimeMillis();
                while (true)
                {
                    long endTime = System.currentTimeMillis();
                    long millis = (endTime - startTime);
                    if (allTasksCompleted())
                    {
                        Collection<File> lst = FileUtils.listFiles(
                                  new File(FSEUtils.getNameOfStorageDir(level)),
                                       new String[] {DENOPTIMConstants.SERGFILENAMEEXT},
                                                                         false);
                        int outCount = lst.size() - serFromChkRestart;
                        int totSubmSubTasks = countSubTasks();
                        if (outCount != totSubmSubTasks  &&  level > -1)
                        {
                            msg = "Mismatch between the number of submitted "
                                  + "tasks and the reported graphs ("
                                  + totSubmSubTasks + "/" + outCount + ")";
                            DENOPTIMLogger.appLogger.log(Level.SEVERE,msg);
                            throw new DENOPTIMException(msg);
                        }
                        break;
                    }
                    else 
                    {
                        if (subtaskHasException())
                        {
                            stopRun();
                            msg = "Exception in submitted task.";
                            throw new DENOPTIMException(msg,thrownByTask);
                        }
                    }

                    if (verbosity > 0)
                    {
                        msg = "Waiting for completion of level " + level;
                        msg = msg + String.format(" (elapsed %d min, %d sec)", 
                                        TimeUnit.MILLISECONDS.toMinutes(millis),
                                         TimeUnit.MILLISECONDS.toSeconds(millis)
                                                   - TimeUnit.MINUTES.toSeconds(
                                      TimeUnit.MILLISECONDS.toMinutes(millis)));
                        DENOPTIMLogger.appLogger.log(Level.INFO,msg);
                    }
    
                    if (millis > FSEParameters.getMaxWait())
                    {
                        stopRun();
                        msg = "Timeout reached: stopping all subtasks.";
                        DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                        interrupted = true;
                        break;
                    }

                    try
                    {
                        Thread.sleep(FSEParameters.getWaitStep());
                    }
                    catch (Throwable t)
                    {
                        throw new DENOPTIMException(t);
                    }
                }
            }

            // Clean queue
            cleanup(tpe, futures, submitted);

            // Level and all tasks completed
            if (interrupted)
            {
                break;
            }
            else
            {
                msg = "Exploration of level " + level + " "
                    + "completed" + DENOPTIMConstants.EOL 
                    + "----------------------------------------"
                    + "----------------------------------------" 
                    + DENOPTIMConstants.EOL;
                DENOPTIMLogger.appLogger.log(Level.INFO,msg);
            }

            // Increment level index
            level++;

            // Check for possibility of building another level
            boolean noRoot = false;
            if (numSubTasks == 0)
            {
                // Needed to perceive prev.lev. when restarting from checkpoint
                try
                {
                    Collection<File> lstRootsForNextLev = FileUtils.listFiles(
                                new File(FSEUtils.getNameOfStorageDir(level-1)),
                                       new String[] {DENOPTIMConstants.SERGFILENAMEEXT},
                                                                         false);
                    if (lstRootsForNextLev.size() == 0)
                    {
                        noRoot = true;
                    }
                }
                catch (Throwable t)
                {
                    noRoot = true;
                }
            }
            if (noRoot)
            {
                msg = "Previous level did not return any extendable graph. "
                    + "Terminating exploration." + DENOPTIMConstants.EOL
                    + "----------------------------------------"
                    + "----------------------------------------" 
                    + DENOPTIMConstants.EOL;
                DENOPTIMLogger.appLogger.log(Level.INFO,msg);
                break;
            }

            if (restartFromChkPt)
            {
                restartFromChkPt = false;
            }
        }

        // shutdown thread pool
        tpe.shutdown();

        // closing messages
        watch.stop();
        msg = "Overall time: " + watch.toString() + ". " 
            + DENOPTIMConstants.EOL
            + "FragSpaceExplorer run completed." + DENOPTIMConstants.EOL;
        DENOPTIMLogger.appLogger.log(Level.INFO, msg);
    }

//------------------------------------------------------------------------------

    /**
     * Generate graphs by exploring all combination of fragments an a given
     * level of a growing graph. Note that in this context the 'level' is a 
     * is related to the initial root graph, which is considered to be level -1
     * no matter its size.
     * @param level the current relative level
     * @return the number of submitted tasks
     * @throws DENOPTIMException
     */
    private int exploreCombinationsAtGivenLevel(int level) 
                                                        throws DENOPTIMException
    {
        // The very first level only has the scaffold or the user defined roots
        if (level == -1)
        {
            ArrayList<DENOPTIMGraph> scafLevel = new ArrayList<DENOPTIMGraph>();
            if (FSEParameters.useGivenRoots())
            {
                // User defined root graphs
                if (verbosity > 0)
                {
                    String msg = "User defined root graphs are treated as "
                           + "if they were scaffolds. "
                           + "That is, the 'level' of the available attachment "
                           + "points is considered to be '-1' "
                           + "no matter what is the actual level of such APs "
                           + "in the root graph.";
                    DENOPTIMLogger.appLogger.log(Level.WARNING, msg);
                }
                for (DENOPTIMGraph rootGraph : FSEParameters.getRootGraphs())
                {
                    // Vertex ID in root can be whatever and we ignore them
                    // when setting new vertex IDs. To do this, we change sign
                    // to the old IDs to avoid them clashing with the new ones,
                    rootGraph.changeSignToVertexID();
                    // ...and renumber starting from vertex ID = 1.
                    rootGraph.renumberGraphVertices();
                    rootGraph.setGraphId(GraphUtils.getUniqueGraphIndex());
                    scafLevel.add(rootGraph);
                }
            }
            else
            {
                // Make the root graphs from the scaffolds
                for (int i=0; i<FragmentSpace.getScaffoldLibrary().size(); i++)
                {
                    scafLevel.add(startNewGraphFromScaffold(i));
                }
            }
            
            // Store them
            FSEUtils.storeAllGraphsOfLevel(scafLevel,level);

            // All done
            return scafLevel.size();
        }

        // Iterate through the previous level
        String msg = "";
        int numSubTasks = 0;
        int cntRoot = 0;
        int total = 0;
        int itersFromChkPt = 0;
        String prevLevDirName = FSEUtils.getNameOfStorageDir(level-1);
        File prevLevDir = new File(prevLevDirName);
        if (!DenoptimIO.checkExists(prevLevDirName))
        {
             msg = "Previous level folder '" + prevLevDirName + "' not found!";
            throw new DENOPTIMException(msg);
        }
        Collection<File> files = FileUtils.listFiles(new File(prevLevDirName),
                       new String[] {DENOPTIMConstants.SERGFILENAMEEXT}, false);
        ArrayList<File> lstFiles = new ArrayList<File>(files);
        Collections.sort(lstFiles);
        for (File file : lstFiles) 
        {
            cntRoot++;
            if (restartFromChkPt && 
               FSEParameters.getCheckPoint().serFileAlreadyUsed(file.getName()))
            {
                continue;
            }

            DENOPTIMGraph rootGraph = DenoptimIO.deserializeDENOPTIMGraph(file);
            
            // Get combination factory
            FragsCombinationIterator fcf = new FragsCombinationIterator(
                                                                     rootGraph);

            if (restartFromChkPt && firstAfterRestart)
            {
                firstAfterRestart = false;
                fcf.setStartingPoint(
                                    FSEParameters.getCheckPoint().getNextIds());
            }

            // Print summary
            if (verbosity > 0)
            {
                StringBuilder sb = new StringBuilder(512);
                sb.append("Root: " + file.getName() + DENOPTIMConstants.EOL);
                sb.append(" - #Usable APs on root = "); 
                sb.append(fcf.getNumRootAPs() + DENOPTIMConstants.EOL);
                sb.append(" - Size of candidates sets = "); 
                sb.append(fcf.getSizesOfCandidateSets()+DENOPTIMConstants.EOL);
                sb.append(" - Total #Combinations = ");
                sb.append(fcf.getTotNumbCombs() + DENOPTIMConstants.EOL);
                sb.append(" - Root graph: " + DENOPTIMConstants.EOL+rootGraph);
                msg = sb.toString() + DENOPTIMConstants.EOL;
                DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            }

            // Iterate over all combinations 
            try
            {
                while (fcf.hasNext())
                {
                    if (subtaskHasException())
                    {
                        stopRun();
                        msg = "Exception in submitted task.";
                        throw new DENOPTIMException(msg,thrownByTask);
                    }

                    int tId = TaskUtils.getUniqueTaskIndex();
                    FragsCombination fragsToAdd = fcf.next();

                    GraphBuildingTask task = new GraphBuildingTask(tId,
                                                              rootGraph.clone(),
                                                                     fragsToAdd,
                                                                         level);

                    ArrayList<Integer> nextIds = fcf.getNextIds();
                    task.setNextIds(nextIds);

                    submitted.add(task);
                    futures.add(tpe.submit(task));
                    numSubTasks++;
                    if (itersFromChkPt >= FSEParameters.getCheckPointStep())
                    {
                        itersFromChkPt = 0;
                        makeCheckPoint();
                    }
                    itersFromChkPt++;

                    // Code meant only for preparation of checkpoint files
				    // The two following variables define at which point in the
				    // exploration of the space we want to stop.
				    int maxL = 2;
				    int maxI = 50;
		            if (FSEParameters.prepareFilesForTests())
				    {
		            	System.out.println("Wait until "+level+"=="+maxL+" and "
				                 +(total+fcf.getNumGeneratedCombs())+"=="+maxI);
						if (level>=maxL && 
						    (total+fcf.getNumGeneratedCombs()>=maxI))
			            {
						    String key = "NONE";
			                System.out.println("Execution stopped: now waiting "
							 + " for checkpoint file to mature");
						    int iWait=0;
						    int nEqual = 0;
						    ArrayList<Integer> oldIds =new ArrayList<Integer>();
						    ArrayList<Integer> nowIds =new ArrayList<Integer>();
			                            makeCheckPoint();
						    oldIds.addAll(
						            FSEParameters.getCheckPoint().getNextIds());
		                    while (true)
		                    {
								iWait++;
								Thread.sleep(1000); // in millisec
								if (iWait > 120)
								{
							            System.out.println("NOT CONVERGED");
							            System.exit(1);
								}
				                                makeCheckPoint();
								nowIds.clear();
								nowIds.addAll(
								FSEParameters.getCheckPoint().getNextIds());
								System.out.println(oldIds + " " + nowIds 
										   + " nEqualChecks:" + nEqual);
								boolean converged = true;
								if (nowIds.size() != oldIds.size())
								{
								    converged = false;
								}
								else
								{
								    for (int iId=0; iId<nowIds.size(); iId++)
								    {
								        if (nowIds.get(iId) != oldIds.get(iId))
								        {
										    nEqual = 0;
										    converged = false;
										    break;
								        }
								    }
								}
								if (!converged)
								{
							            oldIds.clear();
								    oldIds.addAll(nowIds);
								    continue;
								}
								nEqual++;
								if (nEqual >= 10)
								{
								    break;
								}
						    }
						    System.out.println("Stopped with converged "
								         + "checkpoint IDs: " + nowIds);
						    System.exit(0);
		                }
		            }
		        }
		    }
            catch (DENOPTIMException dex)
            {
                cleanup(tpe, futures, submitted);
                tpe.shutdown();
                throw dex;
            }
            catch (Exception ex)
            {
                cleanup(tpe, futures, submitted);
                tpe.shutdown();
                throw new DENOPTIMException(ex);
            }

            if (verbosity > 0)
            {
                msg = fcf.getNumGeneratedCombs() + "/"
                      + fcf.getTotNumbCombs() + " combination generated "
                      + "for level " + level + " of graph " + cntRoot;
                if (verbosity > 1)
                {
                    msg = msg  + " => " + rootGraph;
                }
                DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            }
            total = total + fcf.getNumGeneratedCombs();
        }

        msg = "Total number of combination of fragments generated "
              + "for level " + level + " = " + total;
        DENOPTIMLogger.appLogger.log(Level.INFO, msg);

/*
        // Code meant only to generate checkpoint file at the end of a level
        if (FSEParameters.prepareFilesForTests())
        {
            System.out.println("Last submissions for level "+level+". Wait for "
                                + "completion of the tasks.");
            while (true)
            {
                makeCheckPoint();
                System.out.println("Ctrl-Z once converged: " +
				    FSEParameters.getCheckPoint().getNextIds());
                GenUtils.pause();
            }
        }
*/

        return numSubTasks;
    }

//------------------------------------------------------------------------------

    /**
     * @param scafIdx the molID (i.e., the index of the molecule in the library
     * of scaffolds)
     * @return a graph containing only the scaffold of which the molID is given
     */
    private DENOPTIMGraph startNewGraphFromScaffold(int scafIdx) 
                                                        throws DENOPTIMException
    {
        DENOPTIMGraph molGraph = new DENOPTIMGraph();
        molGraph.setGraphId(GraphUtils.getUniqueGraphIndex());

        //TODO-V3: use a vertex constructor that is type-agnostic
        DENOPTIMVertex scafVertex = DENOPTIMVertex.newVertexFromLibrary(
                GraphUtils.getUniqueVertexIndex(),scafIdx,BBType.SCAFFOLD);

        // as in DenoptimGA, though level=-1 is a bit misleading
        scafVertex.setLevel(-1);

        //TODO-V3: check that symmetry is inherited from the original vertex stored in the library of building blocks.
        
        /*
        ArrayList<SymmetricSet> symAPs =
                mol.getSymmetricAPsSets();
        scafVertex.setSymmetricAP(symAPs);
        */

        // add the scaffold as a vertex
        molGraph.addVertex(scafVertex);
        molGraph.setMsg("NEW");
        
        return molGraph;
    }

//------------------------------------------------------------------------------

    /**
     * clean all reference to submitted tasks
     */

    private void cleanup(ThreadPoolExecutor tpe, List<Future<String>> futures,
                            ArrayList<GraphBuildingTask> submitted)
    {
        for (Future<String> f : futures)
        {
            f.cancel(true);
        }

        for (GraphBuildingTask tsk: submitted)
        {
            tsk.stopTask();
        }

        submitted.clear();

        tpe.getQueue().clear();
    }

//------------------------------------------------------------------------------    
}
