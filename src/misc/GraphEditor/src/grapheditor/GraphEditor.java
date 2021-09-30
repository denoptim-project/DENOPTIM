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

package grapheditor;

import java.util.logging.Level;

import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.threedim.ThreeDimTreeBuilder;
import denoptim.utils.GenUtils;
import denoptim.utils.GraphConversionTool;


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
