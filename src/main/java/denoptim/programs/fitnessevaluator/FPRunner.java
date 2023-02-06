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

package denoptim.programs.fitnessevaluator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.graph.DGraph;
import denoptim.io.DenoptimIO;
import denoptim.logging.StaticLogger;
import denoptim.programs.combinatorial.FragSpaceExplorer;
import denoptim.programs.denovo.GARunner;
import denoptim.task.FitnessTask;


/**
 * Runs a fitness provider task as defined in the static parameters. This class
 * is meant to execute fitness provider tasks from the GUI or the CLI in a
 * stand-alone fashion, i.e., without running {@link GARunner} or 
 * {@link FragSpaceExplorer}.
 *
 * @author Marco Foscato
 */

public class FPRunner
{
    /**
     * Storage of references to the submitted subtasks as <code>Future</code>.
     */
    final Map<FitnessTask,Future<Object>> futures;

    /**
     * Storage of references to the submitted subtasks.
     */
    final List<FitnessTask> submitted;

    /**
     * Asynchronous tasks manager 
     */
    final ThreadPoolExecutor tpe;
    
    /**
     * The parameters controlling the 
     */
    private FRParameters settings;

    
    /**
     * Number of parallel fitness evaluations we run
     */
    // TODO: use getNumCPU from GAParameters to launch fitness evaluations in parallel.
    private int numThreads = 1;

//-----------------------------------------------------------------------------

    /**
     * Constructor
     */

    public FPRunner(FRParameters settings)
    {
        this.settings = settings;
        futures = new HashMap<FitnessTask,Future<Object>>();
        submitted = new ArrayList<>(numThreads);

        tpe = new ThreadPoolExecutor(numThreads, numThreads, Long.MAX_VALUE, 
                TimeUnit.NANOSECONDS, new ArrayBlockingQueue<Runnable>(1));

        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                tpe.shutdown(); // Disable new tasks from being submitted
                try
                {
                    // Wait a while for existing tasks to terminate
                    if (!tpe.awaitTermination(5, TimeUnit.SECONDS))
                    {
                        tpe.shutdownNow(); // Cancel currently executing tasks
                    }

                    if (!tpe.awaitTermination(10, TimeUnit.SECONDS))
                    {
                        // pool didn't terminate after the second try
                    }
                }
                catch (InterruptedException ie)
                {
                    cleanup();
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
                    ex.printStackTrace();
                    String msg = "EXCEPTION in rejectedExecution.";
                    StaticLogger.appLogger.log(Level.WARNING,msg);
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
        cleanup();
        tpe.shutdown();
    }

//------------------------------------------------------------------------------

    /**
     * Create and run the fitness task.
     * @throws Exception 
     */

    public void run() throws Exception
    {
        String msg = "";
        StopWatch watch = new StopWatch();
        watch.start();
        
        List<DGraph> graphs = DenoptimIO.readDENOPTIMGraphsFromFile(
                settings.getInputFile());
        List<IAtomContainer> iacs = DenoptimIO.readSDFFile(
                settings.getInputFile().getAbsolutePath());
        if (graphs.size() != iacs.size())
        {
            throw new DENOPTIMException("Found " + graphs.size() + " and " 
                    + iacs.size() + " in " + settings.getInputFile());
        }
        
        tpe.prestartAllCoreThreads();
        
        int evaluationCount = 0;
        for (int i=0; i<graphs.size(); i++)
        {
            DGraph graph = graphs.get(i);
            IAtomContainer iac = iacs.get(i);
  
            FitnessEvaluationTask task = new FitnessEvaluationTask(settings,
                    graph, iac, settings.getWorkDirectory(), 
                    settings.getOutputFile().getAbsolutePath()+"_"+i);

            submitted.add(task);
            futures.put(task,tpe.submit(task));
            evaluationCount++;
            if (evaluationCount>(numThreads*2))
            {
                cleanupCompleted();
            }
        }
        
        // wait a bit for pending tasks to finish
        tpe.shutdown();
        tpe.awaitTermination(5, TimeUnit.SECONDS);
        
        for (int i=0; i<graphs.size(); i++)
        {
            String s = DenoptimIO.readText(
                    settings.getOutputFile().getAbsolutePath()+"_"+i);
            // Get rid of trailing newline character
            s = s.substring(0, s.length()-1);
            DenoptimIO.writeData(settings.getOutputFile().getAbsolutePath(), 
                    s, true);
            FileUtils.deleteQuietly(new File(
                    settings.getOutputFile().getAbsolutePath()+"_"+i));
        }
        
        watch.stop();
        String plural = "";
        if (evaluationCount>1)
            plural = "s";
        msg = "Overall time: " + watch.toString() + ". " + DENOPTIMConstants.EOL
                + "Run " + evaluationCount + " evaluation" + plural 
                + " of fitness." + DENOPTIMConstants.EOL
                + "FitnessRunner run completed." + DENOPTIMConstants.EOL;
        StaticLogger.appLogger.log(Level.INFO, msg);
    }
        
//------------------------------------------------------------------------------

    /**
     * Removes only tasks that are marked as completed.
     */
    private void cleanupCompleted()
    {
        List<FitnessTask> completed = new ArrayList<FitnessTask>();

        for (FitnessTask t : submitted)
        {
            if (t.isCompleted())
                completed.add(t);
        }

        for (FitnessTask t : completed)
        {
            submitted.remove(t);
            futures.get(t).cancel(true);
            futures.remove(t);
        }
    }

//------------------------------------------------------------------------------

    /**
     * clean all reference to submitted tasks
     */

    private void cleanup()
    {
        for (FitnessTask tsk: submitted)
        {
            tsk.stopTask();
        }
        for (FitnessTask tsk : futures.keySet())
        {
            futures.get(tsk).cancel(true);
        }
        futures.clear();
        submitted.clear();
        tpe.getQueue().clear();
    }

//------------------------------------------------------------------------------    

}
