package  denoptim.programs.graphlisthandler;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import denoptim.graph.DENOPTIMGraph;
import denoptim.task.ProgramTask;


/**
 * Tool for handling lists of graphs.
 *
 * @author Marco Foscato
 */

public class GraphListsHandler extends ProgramTask
{

//------------------------------------------------------------------------------
    
    /**
     * Creates and configures the program task.
     * @param configFile the file containing the configuration parameters.
     * @param workDir the file system location from which to run the program.
     */
    public GraphListsHandler(File configFile, File workDir)
    {
        super(configFile,workDir);
    }
    
//------------------------------------------------------------------------------

    @Override
    public void runProgram() throws Throwable
    {
        GraphListsHandlerParameters glhParams = new GraphListsHandlerParameters();
        glhParams.readParameterFile(configFilePathName.getAbsolutePath());
        glhParams.checkParameters();
        glhParams.processParameters();
        glhParams.printParameters();

        Set<DENOPTIMGraph> matchedA = new HashSet<DENOPTIMGraph>();
        Set<DENOPTIMGraph> matchedB = new HashSet<DENOPTIMGraph>();
        
        int i = -1;
        for (DENOPTIMGraph gA :  glhParams.inGraphsA)
        {
            i++;
            int j = -1;
            for (DENOPTIMGraph gB :  glhParams.inGraphsB)
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
                + glhParams.inGraphsA.size());
        System.out.println(" #Matches in list B: "+matchedB.size()+"/"
                + glhParams.inGraphsB.size());
        
        System.out.println(" ");
        System.out.println(" ===> Un-matches in list A");
        int ii = -1;
        for (DENOPTIMGraph gA : glhParams.inGraphsA)
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
        for (DENOPTIMGraph gB : glhParams.inGraphsB)
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
    
//------------------------------------------------------------------------------

}
