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

import java.io.File;
import java.util.concurrent.Callable;


/**
 * A task that can throw exceptions.
 */

public abstract class Task implements Callable<Object>
{
	/**
	 * Flag controlling whether this task is expected to notify the static task
	 * manager
	 */
	protected boolean notifyGlobalTaskManager = false;
	
    /**
     * Flag about completion. Should be set to <code>true</code> only by the
     * call() method.
     */
    protected boolean completed = false;

    /**
     * Flag about exception
     */
    protected boolean hasException = false;
    
    /**
     * Lock for addressing synchronization issues.
     */
    public Object lock = new Object();
    
    /**
     * Error message produced by any subtask
     */
    protected String errMsg = "";

    /**
     * Exception thrown
     */
    protected Throwable thrownExc;

    /**
     * A user-assigned id for this task.<br>
     * This id is used as a task identifier when canceling or restarting a task
     * using the remote management functionalities.
     */
    protected int id;
    
    /**
     * Executor for external bash script
     */
    protected ProcessHandler processHandler;
    
    /**
     * The file system location where we want to be placed when doing the work.
     */
    protected File workDir;
    
    /**
     * Verbosity level
     */
    protected int verbosity = 0;

    /**
     * System-dependent file separator
     */
    protected final String SEP = System.getProperty("file.separator");
    
    /**
     * System-dependent line separator (newline)
     */
    protected final String NL = System.getProperty("line.separator");
   
//------------------------------------------------------------------------------

    public Task(final int id)
    {
    	this.id = id;
    }

//------------------------------------------------------------------------------

    public int getId()
    {
        return id;
    }
    
//------------------------------------------------------------------------------

    /**
     * Set the verbosity: i.e., amount of log printed by this class.
     */
    public void setVerbosity(int verbosity)
    {
    	this.verbosity = verbosity;
    }
//------------------------------------------------------------------------------
    
    /**
     * Sets the pathname of the work space, i.e., the location where the task 
     * is supposed to use move to or to threat as the result of "pwd" at runtime
     */
    public void setWorkSpace(File workDir)
    {
    	this.workDir = workDir;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return <code>true</code> if the task is completed
     */
    public boolean isCompleted()
    {
        return completed;
    }

//------------------------------------------------------------------------------
    
    /**
     * @return <code>true</code> if an exception has been thrown within this 
     * subtask, which also includes the scenario where the executed external
     * script returned a non-zero exit status.
     */
    public boolean foundException()
    {
        return hasException;
    }

//------------------------------------------------------------------------------

    /**
     * @return the exception thrown within this task.
     */
    public Throwable getException()
    {
        return thrownExc;
    }
    
//------------------------------------------------------------------------------

    /**
     * @return the error message from this task.
     */
    public String getErrorMessage()
    {
        return errMsg;
    }
    
//------------------------------------------------------------------------------

    /**
     * Stop the task if not already completed.
     */

    public void stopTask()
    {
        if (completed)
        {
            return;
        }
        if (processHandler != null)
        {
            System.err.println("Calling stop on processes from " 
            		+ this.getClass().getName() + " " + id);
            processHandler.stopProcess();
        } else {
        	if (notifyGlobalTaskManager)
        	{
        		StaticTaskManager.subtractDoneTask();
        	}
        	Thread.currentThread().interrupt();
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns a string identifying this task by its ID and reporting whether
     * an exception has been thrown and if the tasks is completed.
     */
    
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getName());
        sb.append(" [id=").append(id);
        sb.append(", hasException=").append(hasException);
        sb.append(", completed=").append(completed).append("] ");
        return sb.toString();
    }
    
//------------------------------------------------------------------------------

	public void setNotify(boolean notify) 
	{
		this.notifyGlobalTaskManager = notify;
	}
    
//------------------------------------------------------------------------------    

}
