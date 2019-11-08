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
