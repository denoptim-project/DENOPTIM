package serconverter;

import java.util.ArrayList;
import java.io.File;

import utils.GenUtils;
import utils.GraphConversionTool;
import io.DenoptimIO; 
import molecule.DENOPTIMGraph;
import exception.DENOPTIMException;

/**
 * Conversion tool for serialized <code>DENOPTIMGraph</code>s orjects.
 * @author Marco Foscato
 */

public class SerConverter
{

//------------------------------------------------------------------------------    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        if (args.length < 1)
        {
            System.err.println("Usage: java -jar SerConverter.jar paramsFile");
            System.exit(-1);
	}

        try
        {
	    SerConvParameters.readParameterFile(args[0]);
	    SerConvParameters.checkParameters();
	    SerConvParameters.processParameters();
           
            DENOPTIMGraph graph = DenoptimIO.deserializeDENOPTIMGraph(
                                           new File(SerConvParameters.inpFile));
	    
	    switch (SerConvParameters.outFormat)
	    {
		case "TXT":
		    String str = graph.toString();
		    DenoptimIO.writeData(SerConvParameters.outFile,str,false);
		    break;
		case "SDF":
		    DenoptimIO.writeMolecule(SerConvParameters.outFile, 
		         GraphConversionTool.convertGraphToMolecule(graph,true),
									 false);
		    break;
	    }
        }
        catch (DENOPTIMException de)
        {
            GenUtils.printExceptionChain(de);
            System.exit(-1);
        }

        System.exit(0);
    }

//------------------------------------------------------------------------------

}
