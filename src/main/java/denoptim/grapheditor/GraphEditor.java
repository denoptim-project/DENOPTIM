/*
 *   DENOPTIM
 *   Copyright (C) 2019 Marco Foscato <marco.foscato@uib.no>
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

package denoptim.grapheditor;

import java.io.File;
import java.util.logging.Level;

import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.exception.DENOPTIMException;
import denoptim.graph.DENOPTIMGraph;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.task.ProgramTask;
import denoptim.threedim.ThreeDimTreeBuilder;


/**
 * Tool for editing {@link DENOPTIMGraph}s.
 *
 * @author Marco Foscato
 */

public class GraphEditor extends ProgramTask
{

//------------------------------------------------------------------------------
    
    /**
     * Creates and configures the program task.
     * @param configFile the file containing the configuration parameters.
     * @param workDir the file system location from which to run the program.
     */
    public GraphEditor(File configFile, File workDir)
    {
        super(configFile,workDir);
    }
    
//------------------------------------------------------------------------------

    @Override
    public void runProgram()
    {   
        try
        {
            GraphEdParameters.readParameterFile(
                    configFilePathName.getAbsolutePath());
            GraphEdParameters.checkParameters();
            GraphEdParameters.processParameters();
            GraphEdParameters.printParameters();

            int i = -1;
            for (DENOPTIMGraph graph : GraphEdParameters.getInputGraphs())
            {
                i++;
                DENOPTIMGraph modGraph = graph.editGraph(
                                         GraphEdParameters.getGraphEditTasks(),
                                              GraphEdParameters.symmetryFlag(),
                                             GraphEdParameters.getVerbosity()
                );
               
                if (GraphEdParameters.getVerbosity() > 0)
                {
                    System.out.println("Original graph: ");
                    System.out.println(graph.toString());
                    System.out.println("Modified graph: ");
                    System.out.println(modGraph.toString());
                }
                
                //TODO: upgrade to I/O with sepcification of format

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

                        ThreeDimTreeBuilder t3d = new ThreeDimTreeBuilder();
                        IAtomContainer newMol = t3d
                                .convertGraphTo3DAtomContainer(modGraph,true);
                        if (GraphEdParameters.getInFormat().equals("SDF"))
                        {
                            IAtomContainer oldMol = 
                                                 GraphEdParameters.getInpMol(i);
                            if (oldMol.getProperty("cdk:Title") != null)
                            {
                            	String name = oldMol.getProperty(
                                                        "cdk:Title").toString();
                            	newMol.setProperty("cdk:Title",name);
                            }
                        }
                        DenoptimIO.writeSDFFile(GraphEdParameters.getOutFile(),
                                                                   newMol,true);
                        break;
                    }
                }
            }
        }
        catch (Exception e)
        {
            DENOPTIMLogger.appLogger.log(Level.SEVERE, "Error occured", e);
            e.printStackTrace(System.err);
            thrownExc = new DENOPTIMException("Error in GraphEditor run", e);
        }

        // normal completion
        DENOPTIMLogger.appLogger.log(Level.SEVERE, 
			       "========= GraphEditor run completed =========");
    }
    
//-----------------------------------------------------------------------------
}
