package  graphlistshandler;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import denoptim.graph.DENOPTIMGraph;
import denoptim.logging.DENOPTIMLogger;


/**
 * Tool for handling lists of graphs.
 *
 * @author Marco Foscato
 */

public class GraphListsHandler
{

//------------------------------------------------------------------------------

    /**
     * Prints the syntax to execute
     */

    public static void printUsage()
    {
        System.err.println("Usage: java -jar GraphListsHandler.jar ConfigFile");
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
            GraphListsHandlerParameters.readParameterFile(configFile);
            GraphListsHandlerParameters.checkParameters();
            GraphListsHandlerParameters.processParameters();
            GraphListsHandlerParameters.printParameters();

            Set<DENOPTIMGraph> matchedA = new HashSet<DENOPTIMGraph>();
            Set<DENOPTIMGraph> matchedB = new HashSet<DENOPTIMGraph>();
            
            int i = -1;
            for (DENOPTIMGraph gA :  GraphListsHandlerParameters.inGraphsA)
            {
                i++;
                int j = -1;
                for (DENOPTIMGraph gB :  GraphListsHandlerParameters.inGraphsB)
                {
                    j++;
                    
                    System.out.println(" ");
                    System.out.println("-> Comparing "+i+" and "+j);
                    if (gA.isIsomorphicTo(gB))
                    {
                        System.out.println(" SAME!");
                        matchedA.add(gA);
                        matchedB.add(gB);
                        break;
                    } else {
                        System.out.println(" Different");
                    }
                }
            }
            
            System.out.println(" ");
            System.out.println(" #Matches in list A: "+matchedA.size()+"/"
                    +GraphListsHandlerParameters.inGraphsA.size());
            System.out.println(" #Matches in list B: "+matchedB.size()+"/"
                    +GraphListsHandlerParameters.inGraphsB.size());
            
            System.out.println(" ");
            System.out.println(" ===> Un-matches in list A");
            int ii = -1;
            for (DENOPTIMGraph gA :  GraphListsHandlerParameters.inGraphsA)
            {
                ii++;
                if (matchedA.contains(gA))
                {
                    continue;
                }
                System.out.println(" ");
                System.out.println("Entry in original list #"+ii);
                System.out.println(gA);
            }
            
            System.out.println(" ");
            System.out.println(" ===> Un-matches in list B");
            int jj = -1;
            for (DENOPTIMGraph gB :  GraphListsHandlerParameters.inGraphsB)
            {
                jj++;
                if (matchedB.contains(gB))
                {
                    continue;
                }
                System.out.println(" ");
                System.out.println("Entry in original list #"+jj);
                System.out.println(gB);
            }
        }
        catch (Exception e)
        {
            DENOPTIMLogger.appLogger.log(Level.SEVERE, "Error occured", e);
            e.printStackTrace(System.err);
            System.exit(-1);
        }

        // normal completion
        DENOPTIMLogger.appLogger.log(Level.SEVERE,
                   "========= GraphListsHandler run completed =========");
        System.exit(0);
    }
    
//------------------------------------------------------------------------------

}
