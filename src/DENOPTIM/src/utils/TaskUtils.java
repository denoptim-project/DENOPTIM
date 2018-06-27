package utils;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * Utilities for tasks.
 *
 * @author Marco Foscato
 */

public class TaskUtils
{
    private static AtomicInteger taskCounter = new AtomicInteger(1);
    
//------------------------------------------------------------------------------

    /**
     * Unique counter for tasks
     * @return the new task id (number)
     */

    public static synchronized int getUniqueTaskIndex()
    {
        return taskCounter.getAndIncrement();
    }

//------------------------------------------------------------------------------

}
