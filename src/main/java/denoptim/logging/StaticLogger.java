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

package denoptim.logging;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.XMLFormatter;


/**
 * Logger class for DENOPTIM
 * @author Vishwesh Venkatraman
 */

public class StaticLogger 
{
    public static final Logger appLogger = Logger.getLogger("DENOPTIMLogger");
    private static StaticLogger uniqInstance = null;
    
//------------------------------------------------------------------------------
    
    private StaticLogger()
    {
      // Exists only to defeat instantiation.
    }

//------------------------------------------------------------------------------    
    
    public static synchronized StaticLogger getInstance() 
    {  
        if(uniqInstance == null) 
        {  
            uniqInstance = new StaticLogger();
        }
        return uniqInstance;
    }
 
//------------------------------------------------------------------------------

    /**
     * Clone is not supported!
     * @return
     * @throws CloneNotSupportedException
     */
    @Override
    public final Object clone() throws CloneNotSupportedException 
    {
        throw new CloneNotSupportedException();
    }
    
//------------------------------------------------------------------------------  
    
}
