package grapheditor;

import java.util.logging.Level;
import java.util.ArrayList;

import org.openscience.cdk.interfaces.IAtomContainer;

import exception.DENOPTIMException;
import logging.DENOPTIMLogger;
import utils.GenUtils;
import utils.GraphUtils;
import utils.GraphConversionTool;
import molecule.DENOPTIMGraph;
import io.DenoptimIO;


/**
 * Tool for editing DENOPTIMGraphs.
 *
 * @author Marco Foscato
 */

public class GraphEditor
{

//------------------------------------------------------------------------------

    /**
     * Prints the syntax to execute
     */

    public static void printUsage()
    {
        System.err.println("Usage: java -jar GraphEditor.jar ConfigFile");
        System.exit(-1);
    }

//------------------------------------------------------------------------------    
    /**
     * @param args the command line arguments
     */

    public static void main(String[] args)
    {
        if (args.length < 1)
        {
            printUsage();
        }

        String configFile = args[0];
        
        try
        {
            GraphEdParameters.readParameterFile(configFile);
            GraphEdParameters.checkParameters();
            GraphEdParameters.processParameters();
            GraphEdParameters.printParameters();

            int i = -1;
            for (DENOPTIMGraph graph : GraphEdParameters.getInputGraphs())
            {
                i++;
                DENOPTIMGraph modGraph = GraphUtils.editGraph(graph,
                                         GraphEdParameters.getGraphEditTasks(),
                                              GraphEdParameters.symmetryFlag(),
                                             GraphEdParameters.getVerbosity());
               
                if (GraphEdParameters.getVerbosity() > 0)
                {
                    System.out.println("Original graph: ");
                    System.out.println(graph.toString());
                    System.out.println("Modified graph: ");
                    System.out.println(modGraph.toString());
                }

                switch (GraphEdParameters.getOutFormat())
                {
                    case (GraphEdParameters.STRINGFORMATLABEL):
                    {
                        DenoptimIO.writeData(GraphEdParameters.getOutFile(),
                                                      modGraph.toString(),true);
                        break;
                    }
                    case (GraphEdParameters.SERFORMATLABEL):
                    {
                        DenoptimIO.serializeToFile(
                                                GraphEdParameters.getOutFile(),
                                                      modGraph.toString(),true);
                        break;
                    }
                    case ("SDF"):
                    {
                        IAtomContainer newMol = 
                           GraphConversionTool.convertGraphToMolecule(modGraph,
                                                                          true);
                        if (GraphEdParameters.getInFormat().equals("SDF"))
                        {
                            IAtomContainer oldMol = 
                                                 GraphEdParameters.getInpMol(i);
                            String name = oldMol.getProperty(
                                                        "cdk:Title").toString();
                            newMol.setProperty("cdk:Title",name);
                        }
                        DenoptimIO.writeMolecule(GraphEdParameters.getOutFile(),
                                                                   newMol,true);
                        break;
                    }
                }
            }
        }
        catch (Exception e)
        {
            DENOPTIMLogger.appLogger.log(Level.SEVERE, "Error occured", e);
            GenUtils.printExceptionChain(e);
            System.exit(-1);
        }

        // normal completion
	DENOPTIMLogger.appLogger.log(Level.SEVERE, 
			       "========= GraphEditor run completed =========");
        System.exit(0);
    }
    
//-----------------------------------------------------------------------------
}
