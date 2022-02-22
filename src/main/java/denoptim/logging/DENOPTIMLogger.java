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

//TODO: this singleton by design. It will have to be changed into a 
// thread-specific class. However, this change makes sense only is the fragment 
// space becomes thread-specific as well. Now, both logger and fragment space 
// are static objects,
// so running multiple GS experiments on different threads is not possible.

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
    	int n = appLogger.getHandlers().length;
    	for (int i=0; i<n; i++)
    	{
    	    appLogger.removeHandler(appLogger.getHandlers()[0]);
    	}
    	//this commenting is still part of the hack
    	//if (!hasBeenSet)
        //{
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
        //}
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
