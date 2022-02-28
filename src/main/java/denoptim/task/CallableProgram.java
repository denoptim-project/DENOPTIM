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

import denoptim.exception.DENOPTIMException;

/**
 * The entry point for programs that can be executed by DENOPTIM main
 * class.
 * @author Marco Foscato
 */
public abstract class CallableProgram implements Runnable
{
    //TODO-GG delete class
    /**
     * List of arguments.
     */
    protected String[] args;
    
    /**
     * The execution that caused the program to stop, or null.
     */
    protected Throwable exception;
    
//------------------------------------------------------------------------------

    /**
     * Creates and configures the program.
     * @param args parameters used for configuration.
     * @throws DENOPTIMException if the arguments do not allow to configure the
     * genetic algorithm.
     */
    public CallableProgram(String[] args) throws DENOPTIMException
    {
        this.args = args;
        if (args.length < 1)
        {
            throw new DENOPTIMException("Cannot run. Need at least one argument"
                    + "to run a DENOPTIM program.");
        }
    }

//------------------------------------------------------------------------------    
    
    public abstract void run();
    
//------------------------------------------------------------------------------

    /**
     * Returns <code>true</code> if an exception has stopped this program.
     * @return <code>true</code> if an exception has stopped this program.
     */
    public boolean hasException()
    {
        return exception != null;
    }

//------------------------------------------------------------------------------
    
    /**
     * Returns the exception that stopped this program, if any.
     * @return the exception that stopped this program, or null.
     */
    public Throwable getException()
    {
        return exception;
    }
    
//------------------------------------------------------------------------------        

}
