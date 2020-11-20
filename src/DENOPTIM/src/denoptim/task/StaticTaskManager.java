package denoptim.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Manager for tasks submitted by the GUI. The main purpose is to launch and
 * manage calls placed upon requests coming from the GUI and aimed at
 * running DENOPTIM's main classes in DenoptimGS and FragSpaceExplorer.
 * 
 * @author Marco Foscato
 */

public class StaticTaskManager
{
	/**
	 * The only, static instance of this class
	 */
	private static final StaticTaskManager instance = new StaticTaskManager();
	
	/**
	 * The executor of threads
	 */
	private static ThreadPoolExecutor tpe; 
	
	/**
	 * List that collects the tasks that were submitted
	 */
    private static ArrayList<Task> submitted = new ArrayList<Task>();

    /**
     * Maps the relation between a task that is submitted and its future result
     */
	private static Map<Task,Future<?>> subToFutureMap = 
			new HashMap<Task,Future<?>>();
	
	/**
	 * Number of threads
	 */
	private static final int maxthreads = 2;
	
	/**
	 * Queue size
	 */
	private static final int queueSize = 10;
    
//------------------------------------------------------------------------------
    
    private StaticTaskManager()
    {
    	System.out.println("INITIALIZING STATICTASK MANAGER");
    	tpe = new ThreadPoolExecutor(maxthreads, maxthreads, Long.MAX_VALUE, 
    			TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(queueSize));
    	
    	// What to do in case of filled-up resources and queue rejecting tasks
        tpe.setRejectedExecutionHandler(new RejectedExecutionHandler()
        {
            @Override
            public void rejectedExecution(Runnable r,
            		ThreadPoolExecutor executor)
            {
                try
                {
                	// re-submit to the queue any rejected job
                    executor.getQueue().put(r);
                }
                catch (InterruptedException ex)
                {
                    ex.printStackTrace();
                }
            }
        });
        
        // What to do in case of shutdown signal
        Runtime.getRuntime().addShutdownHook(new ShutDownHook());
        
        // Initialize empty threads
        tpe.prestartAllCoreThreads();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Shutdown hook that stops all child tasks upon shutdown of JavaVM.
     */

    private class ShutDownHook extends Thread
    {
        @Override
        public void run()
        {
            tpe.shutdown();
            try
            {
                if (!tpe.awaitTermination(1, TimeUnit.SECONDS))
                {
                    tpe.shutdownNow(); // Cancel running asks
                }
            }
            catch (InterruptedException ie)
            {
                for (int i=0; i< submitted.size(); i++)
                {
                    Task task = submitted.get(i);
                    Future<?> expectation = subToFutureMap.get(task);
                    expectation.cancel(true);
                    task.stopTask();
                }
                submitted.clear();
                subToFutureMap.clear();
                tpe.purge();
                tpe.getQueue().clear();
                tpe.shutdownNow();
                
                // and stop possibly alive thread
                Thread.currentThread().interrupt();
            }
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Gets the singleton instance of this class.
     * @return
     */
    public static StaticTaskManager getInstance()
    {
    	return instance;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return a string with HTML code, but no html head/tail tags. This string 
     * is meant to be inserted in a larger string with head/tail tags.
     */
    public static String getQueueSnapshot()
    {
    	String s = "Approximate queue status:<ul>"
    			+ "<li>Max # running = " + tpe.getPoolSize() + "</li>"
    			+ "<li>Running tasks = " + tpe.getActiveCount() + "</li>"
    	    	+ "<li>Queue Size    = " + tpe.getQueue().size()+ "</li></ul>";
    	return s;
    }
	
//------------------------------------------------------------------------------
    
    public static void submit(Task task)
    {
    	submitted.add(task);
    	Future<?> future = tpe.submit(task);
    	subToFutureMap.put(task,future);
    }
    
//------------------------------------------------------------------------------
    
    public static void stop(Task task)
    {
    	if (submitted.contains(task))
    	{
    		task.stopTask();
    		subToFutureMap.get(task).cancel(true);
    		subToFutureMap.remove(task);
    	}
    }
    
//------------------------------------------------------------------------------
    

	public static void stopAll() 
	{
		for (Task task : submitted)
		{
			stop(task);
		}
	}

//------------------------------------------------------------------------------
    
}
