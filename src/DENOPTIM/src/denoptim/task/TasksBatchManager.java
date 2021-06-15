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

package denoptim.task;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.List;

import denoptim.exception.DENOPTIMException;
import denoptim.molecule.DENOPTIMMolecule;


/**
 * Class that manages the submission of a batch of tasks.
 * @author Vishwesh Venkatraman
 */
public class TasksBatchManager
{
	private static ArrayList<Task> taskList;
	private static ExecutorService eservice;
	private static CompletionService<Object> cservice;
	private static List<Future<Object>> futures;

//------------------------------------------------------------------------------

    /**
     * Execute the list of tasks
     * @param tasks
     * @param numOfProcessors
     * @return if successful return the list of molecules with fitness
     * @throws DENOPTIMException
     */

    public static ArrayList<DENOPTIMMolecule>
            executeTasks(final ArrayList<Task> tasks, int numOfProcessors)
                                                        throws DENOPTIMException
    {
    	taskList = tasks;
        int numOfJobs = tasks.size();

        int n = Math.min(numOfJobs, numOfProcessors);

        // create a pool with a fixed number of threads that are reused.
        // Here the number of available processors is passed to the factory so
        // the ExecutorService is created with as many threads in the pool as
        // available processors.
        
        eservice = Executors.newFixedThreadPool(n);
        cservice = new ExecutorCompletionService<>(eservice);
        futures = new ArrayList<>();

        for (int i=0; i<numOfJobs; i++)
        {
            futures.add(cservice.submit(tasks.get(i)));
        }

        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                eservice.shutdown(); // Disable new tasks from being submitted
                try
                {
                    // Wait a while for existing tasks to terminate
                    if (!eservice.awaitTermination(30, TimeUnit.SECONDS))
                    {
                        eservice.shutdownNow(); // Cancel currently executing tasks
                    }

                    if (!eservice.awaitTermination(60, TimeUnit.SECONDS))
                    {
                        // pool didn't terminate after the second try
                    }
                }
                catch (InterruptedException ie)
                {
                    for (Task tsk : tasks)
                    {
                        tsk.stopTask();
                    }
                    
                    tasks.clear();

                    for (Future<Object> f : futures)
                    {
                        f.cancel(true);
                    }
                    
                    // (Re-)Cancel if current thread also interrupted
                    eservice.shutdownNow();
                    // Preserve interrupt status
                    Thread.currentThread().interrupt();
                }
            }
        });


        ArrayList<DENOPTIMMolecule> results = new ArrayList<>();

        try
        {
            for (int i=0; i<tasks.size(); i++)
            {
            	if (cservice.take().isCancelled())
            	{
            		continue;
            	}
                DENOPTIMMolecule taskResult =
                                    (DENOPTIMMolecule) cservice.take().get();
                if (!taskResult.getMoleculeUID().equals("UNDEFINED"))
                {
                    results.add(taskResult);
                }
                else 
                {
                    taskResult.cleanup();
                    taskResult = null;
                }
            }
        }
        catch (InterruptedException ie)
        {
            // (Re-)Cancel if current thread also interrupted
            eservice.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
            throw new DENOPTIMException(ie);
        }
        catch (ExecutionException ee)
        {
            throw new DENOPTIMException(ee);
        }
        finally
        {
            eservice.shutdown();
        }

        return results;
    }

//------------------------------------------------------------------------------
    
	public static void stop() 
	{
         try
         {
    		 eservice.shutdown(); // Disable new tasks from being submitted
             for (Task tsk : taskList)
             {
                 tsk.stopTask();
             }
        	 for (Future<Object> f : futures)
             {
                 f.cancel(true);
             }
        	 
             // Wait a while for existing tasks to terminate
             if (!eservice.awaitTermination(5, TimeUnit.SECONDS))
             {
                 eservice.shutdownNow(); // Cancel currently executing tasks
             }
         }
         catch (InterruptedException ie)
         {   
             // (Re-)Cancel if current thread also interrupted
             eservice.shutdownNow();
         }
	}

//------------------------------------------------------------------------------

}
