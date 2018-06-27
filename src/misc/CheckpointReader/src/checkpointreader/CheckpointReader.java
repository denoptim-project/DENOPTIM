package checkpointreader;

import io.DenoptimIO; 
import exception.DENOPTIMException;

import fragspaceexplorer.FSECheckPoint;
import fragspaceexplorer.FSEUtils;


/**
 * Conversion tool for serialized <code>FSECheckPoint</code>.
 * @author Marco Foscato
 */

public class CheckpointReader
{

//------------------------------------------------------------------------------    
    /**
     * @param args the command line arguments. For now only the name of the 
     * checkpoint file to read.
     */
    public static void main(String[] args)
    {
        if (args.length < 1)
        {
            System.err.println("Usage: java -jar ChechpointReader.jar <filename>.chk");
            System.exit(-1);
	}

	String fileName = args[0];
	if (DenoptimIO.checkExists(fileName))
	{
	    try	
	    {
	        FSECheckPoint chk = FSEUtils.deserializeCheckpoint(fileName);
	        System.out.println(" ");
	        System.out.println(chk);
	        System.out.println(" ");
	    }
	    catch (Throwable t)
	    {
		System.err.println("ERROR! Unable to deserialize checkpoint.");
		t.printStackTrace();
	    }
	}
	else
	{
	    System.err.println("ERROR! File '" + fileName + "' not found!");
	    System.exit(-1);
	}

        System.exit(0);
    }

//------------------------------------------------------------------------------

}
