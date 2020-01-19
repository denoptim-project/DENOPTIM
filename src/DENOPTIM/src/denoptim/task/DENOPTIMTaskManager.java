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
 * Class that manages the tasks and executes the required processes
 * @author Vishwesh Venkatraman
 */
public class DENOPTIMTaskManager
{

//------------------------------------------------------------------------------

    /**
     * Execute the list of tasks
     * @param tasks
     * @param numOfProcessors
     * @return if successful return the list of molecules with fitness
     * @throws DENOPTIMException
     */

    public static ArrayList<DENOPTIMMolecule>
            executeTasks(final ArrayList<DENOPTIMTask> tasks, int numOfProcessors)
                                                        throws DENOPTIMException
    {
        int numOfJobs = tasks.size();

        int n = Math.min(numOfJobs, numOfProcessors);

        // create a pool with a fixed number of threads that are reused.
        // Here the number of available processors is passed to the factory so
        // the ExecutorService is created with as many threads in the pool as
        // available processors.
        
        final ExecutorService eservice = Executors.newFixedThreadPool(n);
        CompletionService<Object> cservice = new ExecutorCompletionService<>(eservice);

        final List<Future<Object>> futures = new ArrayList<>();

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
                    for (DENOPTIMTask tsk : tasks)
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


}
