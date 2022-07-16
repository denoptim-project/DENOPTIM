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

package denoptim.fragmenter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.fragspace.FragsCombination;
import denoptim.fragspace.FragsCombinationIterator;
import denoptim.fragspace.IdFragmentAndAP;
import denoptim.graph.DGraph;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;
import denoptim.io.DenoptimIO;
import denoptim.programs.RunTimeParameters.ParametersType;
import denoptim.programs.combinatorial.CEBLParameters;
import denoptim.programs.fragmenter.FragmenterParameters;
import denoptim.utils.GraphUtils;


/**
 * fragments a list of chemical systems by running parallel fragmentation tasks.
 *
 * @author Marco Foscato
 */

public class ParallelFragmentationAlgorithm
{
    /**
     * Storage of references to the submitted subtasks as <code>Future</code>
     */
    final List<Future<Object>> futures;

    /**
     * Storage of references to the submitted subtasks.
     */
    final ArrayList<FragmentationTask> submitted;

    /**
     * Asynchronous tasks manager 
     */
    final ThreadPoolExecutor tpe;

    /**
     * If any, here we stores the exception returned by a subtask
     */
    private Throwable thrownByTask;
    
    /**
     * All settings controlling the tasks executed by this class.
     */
    private FragmenterParameters settings = null;

    /**
     * Settings and definition of the fragment space
     */
    private FragmentSpaceParameters fsSettings = null;

    
//-----------------------------------------------------------------------------

    /**
     * Constructor
     */

    public ParallelFragmentationAlgorithm(FragmenterParameters settings)
    {
        this.settings = settings;
        
        fsSettings = new FragmentSpaceParameters();
        if (settings.containsParameters(ParametersType.FS_PARAMS))
        {
            fsSettings = (FragmentSpaceParameters)settings.getParameters(
                    ParametersType.FS_PARAMS);
        }
        
        futures = new ArrayList<>();
        submitted = new ArrayList<>();

        tpe = new ThreadPoolExecutor(settings.getNumTasks(),
                settings.getNumTasks(),
                Long.MAX_VALUE,
                TimeUnit.NANOSECONDS,
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
                    ex.printStackTrace();
                    String msg = "EXCEPTION in rejectedExecution.";
                    settings.getLogger().log(Level.WARNING,msg);
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
        for (FragmentationTask tsk : submitted)
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
        for (FragmentationTask tsk : submitted)
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
     * Run the combinatorial exploration 
     */

    public void run() throws DENOPTIMException
    {
        String msg = "";
        StopWatch watch = new StopWatch();
        watch.start();

        tpe.prestartAllCoreThreads();
  
        
        //TODO

        // shutdown thread pool
        tpe.shutdown();

        // closing messages
        watch.stop();
        msg = "Overall time: " + watch.toString() + ". " 
            + DENOPTIMConstants.EOL
            + "FragSpaceExplorer run completed." + DENOPTIMConstants.EOL;
        settings.getLogger().log(Level.INFO, msg);
    }

//------------------------------------------------------------------------------

    /**
     * clean all reference to submitted tasks
     */

    private void cleanup(ThreadPoolExecutor tpe, List<Future<Object>> futures,
                            ArrayList<FragmentationTask> submitted)
    {
        for (Future<Object> f : futures)
        {
            f.cancel(true);
        }

        for (FragmentationTask tsk: submitted)
        {
            tsk.stopTask();
        }

        submitted.clear();

        tpe.getQueue().clear();
    }

//------------------------------------------------------------------------------    

}
