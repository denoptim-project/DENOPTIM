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

package denoptim.exception;

/**
 *
 * @author Vishwesh Venkatraman
 */
public class DENOPTIMException extends Exception
{
//------------------------------------------------------------------------------
    /**
     * Creates a new instance of <code>DENOPTIMException</code> without message.
     */
    public DENOPTIMException() 
    {
    }
    
//------------------------------------------------------------------------------    

    /**
     * Constructs a new DENOPTIMException with the given message and the
     * Exception as cause.
     *
     * @param err for the constructed exception
     * @param cause the Throwable that triggered the DENOPTIMException
     */
    public DENOPTIMException(String err, Throwable cause) 
    {
        super(err, cause);
    }

//------------------------------------------------------------------------------        

    /**
     * Constructs an instance of <code>DENOPTIMException</code> with the 
     * specified cause.
     * 
     * @param cause another exception (throwable).
     */
    public DENOPTIMException(Throwable cause) 
    {
        super(cause);
    }    
    
//------------------------------------------------------------------------------    
    

    /**
     * Constructs a new DENOPTIMException with the given message 
     *
     * @param err for the constructed exception
     */

    public DENOPTIMException(String err)
    {
        super(err);     
    }
    
//------------------------------------------------------------------------------
}
