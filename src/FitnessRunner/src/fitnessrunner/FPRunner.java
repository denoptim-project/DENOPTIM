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

package fitnessrunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.apache.commons.lang3.time.StopWatch;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.Candidate;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.task.FitnessTask;
import denoptimga.DenoptimGA;
import fragspaceexplorer.FragSpaceExplorer;


/**
 * Runs a fitness provider task as defined in the static parameters. This class
 * is meant to execute fitness provider tasks from the GUI or the CLI in a
 * stand-alone fashion, i.e., without running {@link DenoptimGA} or 
 * {@link FragSpaceExplorer}.
 *
 * @author Marco Foscato
 */

public class FPRunner
{
    /**
     * Storage of references to the submitted subtasks as <code>Future</code>
     */
    final List<Future<Object>> futures;

    /**
     * Storage of references to the submitted subtasks.
     */
    final ArrayList<FitnessTask> submitted;

    /**
     * Asynchronous tasks manager 
     */
    final ThreadPoolExecutor tpe;

    private Throwable thrownByTask;


//-----------------------------------------------------------------------------

    /**
     * Constructor
     */

    public FPRunner()
    {
        futures = new ArrayList<>(1);
        submitted = new ArrayList<>(1);

        tpe = new ThreadPoolExecutor(1, 1, Long.MAX_VALUE, TimeUnit.NANOSECONDS,
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
                    ex.printStackTrace();
                    String msg = "EXCEPTION in rejectedExecution.";
                    DENOPTIMLogger.appLogger.log(Level.WARNING,msg);
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
        for (FitnessTask tsk : submitted)
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
     * Create and run the fitness task.
     * @throws Exception 
     */

    public void run() throws Exception
    {
        String msg = "";
        StopWatch watch = new StopWatch();
        watch.start();
        
        //WARNING: assumption of SDF format and single entry!
        
        DENOPTIMGraph graph = DenoptimIO.readDENOPTIMGraphsFromFile(
                FRParameters.getInputFile(), FragmentSpace.isDefined()).get(0);
        IAtomContainer iac = DenoptimIO.readSDFFile(
                FRParameters.getInputFile().getAbsolutePath()).get(0);

        tpe.prestartAllCoreThreads();
  
        FitnessEvaluationTask task = new FitnessEvaluationTask(graph, iac, 
                FRParameters.getWorkDirectory(), 
                FRParameters.getOutputFile().getAbsolutePath());

        submitted.add(task);
        futures.add(tpe.submit(task));
            
        tpe.shutdown();

        watch.stop();
        msg = "Overall time: " + watch.toString() + ". " 
            + DENOPTIMConstants.EOL
            + "FitnessRunner run completed." + DENOPTIMConstants.EOL;
        DENOPTIMLogger.appLogger.log(Level.INFO, msg);
    }

//------------------------------------------------------------------------------

    /**
     * clean all reference to submitted tasks
     */

    private void cleanup(ThreadPoolExecutor tpe, List<Future<Object>> futures,
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
        tpe.getQueue().clear();
    }

//------------------------------------------------------------------------------    

}
