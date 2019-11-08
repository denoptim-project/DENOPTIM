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

import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.SimpleFormatter;
import java.util.logging.XMLFormatter;
import java.io.IOException;
import java.util.logging.Level;


/**
 * Logger class for DENOPTIM
 * @author Vishwesh Venkatraman
 */

public class DENOPTIMLogger 
{
    public static final Logger appLogger = Logger.getLogger("DENOPTIMLogger");
    private static DENOPTIMLogger uniqInstance = null;
    private static boolean hasBeenSet = false;
    
//------------------------------------------------------------------------------
    
    private DENOPTIMLogger()
    {
      // Exists only to defeat instantiation.
    }

//------------------------------------------------------------------------------    
    
    public static synchronized DENOPTIMLogger getInstance() 
    {  
        if(uniqInstance == null) 
        {  
            uniqInstance = new DENOPTIMLogger();
        }
        return uniqInstance;
    }
    
//------------------------------------------------------------------------------
    
    public void setupLogger(String logFile) throws IOException
    {
        if (!hasBeenSet)
        {
            boolean toFile = false;
            FileHandler fileHdlr = new FileHandler(logFile);
            if (logFile.endsWith(".html"))
            {
                Formatter formatterHTML = new HTMLLogFormatter();
                fileHdlr.setFormatter(formatterHTML);
                toFile = true;
            }
            else if(logFile.endsWith(".log") || logFile.endsWith(".txt"))
            {
                SimpleFormatter formatterTxt = new SimpleFormatter();
                fileHdlr.setFormatter(formatterTxt);
                toFile = true;
            }
            else if(logFile.endsWith(".xml"))
            {
                XMLFormatter formatterXML = new XMLFormatter();
                fileHdlr.setFormatter(formatterXML);
                toFile = true;
            }
            if (toFile)
            {
                appLogger.setUseParentHandlers(false);
                appLogger.addHandler(fileHdlr);
            }
            appLogger.setLevel(Level.FINEST);
            hasBeenSet = true;
        }
    }

//------------------------------------------------------------------------------
    // avoid cloning

    /**
     *
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
