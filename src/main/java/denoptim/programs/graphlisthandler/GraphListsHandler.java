package  denoptim.programs.graphlisthandler;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import denoptim.graph.DGraph;
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
        glhParams.startProgramSpecificLogger(loggerIdentifier,false); //to STDOUT
        glhParams.printParameters();

        Set<DGraph> matchedA = new HashSet<DGraph>();
        Set<DGraph> matchedB = new HashSet<DGraph>();
        
        int i = -1;
        for (DGraph gA :  glhParams.inGraphsA)
        {
            i++;
            int j = -1;
            for (DGraph gB :  glhParams.inGraphsB)
            {
                j++;
                glhParams.getLogger().log(Level.INFO, NL + "-> Comparing " + i 
                        + " and "+j);
                if (gA.isIsomorphicTo(gB))
                {
                    glhParams.getLogger().log(Level.INFO, " SAME!");
                    matchedA.add(gA);
                    matchedB.add(gB);
                    break;
                } else {
                    glhParams.getLogger().log(Level.INFO, " Different");
                }
            }
        }
        
        glhParams.getLogger().log(Level.INFO, NL + " #Matches in list A: " 
                + matchedA.size()+"/"
                + glhParams.inGraphsA.size());
        glhParams.getLogger().log(Level.INFO, " #Matches in list B: " 
                + matchedB.size()+"/"
                + glhParams.inGraphsB.size());
        
        glhParams.getLogger().log(Level.INFO, NL + " ===> Un-matches in list A");
        int ii = -1;
        for (DGraph gA : glhParams.inGraphsA)
        {
            ii++;
            if (matchedA.contains(gA))
            {
                continue;
            }
            glhParams.getLogger().log(Level.INFO, NL + "Entry in original list "
                    + "#" + ii);
            glhParams.getLogger().log(Level.INFO, gA.toString());
        }
        
        glhParams.getLogger().log(Level.INFO, NL + " ===> Un-matches in list B");
        int jj = -1;
        for (DGraph gB : glhParams.inGraphsB)
        {
            jj++;
            if (matchedB.contains(gB))
            {
                continue;
            }
            glhParams.getLogger().log(Level.INFO, NL + "Entry in original list"
                    + " #" + jj);
            glhParams.getLogger().log(Level.INFO, gB.toString());
        }
    }
    
//------------------------------------------------------------------------------

}
