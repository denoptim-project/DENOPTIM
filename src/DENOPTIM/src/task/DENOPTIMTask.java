package task;

import java.util.concurrent.Callable;

/**
 * This class must be extended by those that perform a fitness evaluation
 * @author Vishwesh Venkatraman
 */
public abstract class DENOPTIMTask implements Callable<Object>
{
    /**
     * The result of the task execution.
     */
    private Object result = null;

    /**
     * A user-assigned id for this task.<br>
     * This id is used as a task identifier when cancelling or restarting a task
     * using the remote management functionalities.
     */
    private int id;

//------------------------------------------------------------------------------

    /**
     * Default constructor.
     */
    public DENOPTIMTask()
    {
    }

//------------------------------------------------------------------------------

    public Object getResult()
    {
        return result;
    }

//------------------------------------------------------------------------------

    public void setResult(final Object  result)
    {
        this.result = result;
    }

//------------------------------------------------------------------------------

    public int getId()
    {
        return id;
    }

//------------------------------------------------------------------------------

    public void setId(final int id)
    {
        this.id = id;
    }

//------------------------------------------------------------------------------
    
    public abstract void stopTask();
    
//------------------------------------------------------------------------------    


}
