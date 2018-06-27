package task;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * See http://www.javaworld.com/jw-12-2000/jw-1229-traps.html?page=4
 * @author Vishwesh Venkatraman
 */
class StreamGobbler extends Thread
{
    InputStream is;
    String type;
    StringBuilder sb;
    
//------------------------------------------------------------------------------    
    
    StreamGobbler(InputStream is, String type)
    {
        this.is = is;
        this.type = type;
        this.sb = new StringBuilder();
    }
    
//------------------------------------------------------------------------------    
    
    public String getMessages()
    {
        return sb.toString();
    }
    
//------------------------------------------------------------------------------    
    
    @Override
    public void run()
    {
        try
        {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null)
                sb.append(type).append("> ").append(line).append("\n");

            br.close();
        }
        catch (IOException ioe)
        {
        }
    }

//------------------------------------------------------------------------------
}
