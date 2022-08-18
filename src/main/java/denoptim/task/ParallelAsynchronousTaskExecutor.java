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

package denoptim.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.time.StopWatch;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;


/**
 * Runs tasks parallel in asynchronous fashion.
 *
 * @author Marco Foscato
 */

abstract public class ParallelAsynchronousTaskExecutor
{
    /**
     * Storage of references to the submitted subtasks as <code>Future</code>
     */
    protected final List<Future<Object>> futures;

    /**
     * Storage of references to the submitted subtasks.
     */
    protected final ArrayList<Task> submitted;

    /**
     * Asynchronous tasks manager 
     */
    final ThreadPoolExecutor tpe;

    /**
     * If any, here we stores the exception returned by a subtask
     */
    private Throwable thrownByTask;
    
    /**
     * Logger
     */
    private Logger logger;

    
//-----------------------------------------------------------------------------

    /**
     * Constructor
     */

    public ParallelAsynchronousTaskExecutor(int numberOfTasks, Logger logger)
    {
        this.logger = logger;
        futures = new ArrayList<>();
        submitted = new ArrayList<>();

        tpe = new ThreadPoolExecutor(numberOfTasks,
                numberOfTasks,
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
                    logger.log(Level.WARNING,msg);
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
    protected boolean subtaskHasException()
    {
        boolean hasException = false;
        for (Task tsk : submitted)
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
     * @return the trace of the problem occurred in the first subtask that was
     * detected to have a problem.
     */
    protected Throwable getExceptionFromSubTask()
    {
        return thrownByTask;
    }

//------------------------------------------------------------------------------

    /**
     * Check for completion of all subtasks
     * @return <code>true</code> if all subtasks are completed
     */
    protected boolean allTasksCompleted()
    {
        boolean allDone = true;
        for (Task tsk : submitted)
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
     * Run the parallelized task. 
     */

    public void run() throws DENOPTIMException, IOException
    {
        String msg = "";
        StopWatch watch = new StopWatch();
        watch.start();
        
        if (!doPreFlightOperations())
            return;
        
        // Start parallel threads for generating and/or manipulating fragments
        tpe.prestartAllCoreThreads();
        
        createAndSubmitTasks();
        
        // This sounds weird when reading the doc, but the following does wait
        // for the threads to complete.
        // TODO-gg try ExecutorCompletionService
        
        // shutdown thread pool
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
        
        if (!doPostFlightOperations())
            return;
        
        // closing messages
        watch.stop();
        msg = "Overall time: " + watch.toString() + ". " 
            + DENOPTIMConstants.EOL
            + this.getClass().getSimpleName() + " run completed." 
            + DENOPTIMConstants.EOL;
        logger.log(Level.INFO, msg);
    }

//------------------------------------------------------------------------------
    
    protected void submitTask(Task task, String logFilePathname)
    {
        submitted.add(task);
        futures.add(tpe.submit(task));
        String msg = task.getClass().getSimpleName() + " "
                + task.getId() + " submitted.";
        if (logFilePathname!=null && !logFilePathname.isBlank())
            msg = msg + " Log file: " + logFilePathname;
        logger.log(Level.INFO, msg);
    }
    
//------------------------------------------------------------------------------

    /**
     * Implementations of this method must call the 
     * {@link #submitTask(Task, String)}
     * method to actually send the task to the executor and eventually start it.
     */
    abstract protected void createAndSubmitTasks();

//------------------------------------------------------------------------------
    
    abstract protected boolean doPostFlightOperations();
    
//------------------------------------------------------------------------------

    abstract protected boolean doPreFlightOperations();

//------------------------------------------------------------------------------
    
    /**
     * clean all reference to submitted tasks
     */
    private void cleanup(ThreadPoolExecutor tpe, List<Future<Object>> futures,
                            ArrayList<Task> submitted)
    {
        for (Future<Object> f : futures)
        {
            f.cancel(true);
        }

        for (Task tsk: submitted)
        {
            tsk.stopTask();
        }
        submitted.clear();
        tpe.getQueue().clear();
    }

//------------------------------------------------------------------------------    

}
