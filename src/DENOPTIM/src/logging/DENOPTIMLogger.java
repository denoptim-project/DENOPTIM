package logging;

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
