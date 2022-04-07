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

package denoptim.programs.grapheditor;

import java.io.File;

import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.graph.DENOPTIMGraph;
import denoptim.io.DenoptimIO;
import denoptim.molecularmodeling.ThreeDimTreeBuilder;
import denoptim.programs.RunTimeParameters.ParametersType;
import denoptim.task.ProgramTask;


/**
 * Tool for editing {@link DENOPTIMGraph}s.
 *
 * @author Marco Foscato
 */

public class GraphEditor extends ProgramTask
{
    /**
     * Fragment space in use.
     */
    private FragmentSpace fragSpace;

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
    public void runProgram() throws Throwable
    {
        GraphEdParameters geParams = new GraphEdParameters();
        geParams.readParameterFile(configFilePathName.getAbsolutePath());
        geParams.checkParameters();
        geParams.processParameters();
        geParams.printParameters();
        
        // We might need the fragment space to read the input graphs with 
        // string-based encoding.
        FragmentSpaceParameters fsParams = new FragmentSpaceParameters();
        if (geParams.containsParameters(ParametersType.FS_PARAMS))
        {
            fsParams = (FragmentSpaceParameters)geParams.getParameters(
                    ParametersType.FS_PARAMS);
        }
        this.fragSpace = fsParams.getFragmentSpace();
        geParams.readInputGraphs(fragSpace);

        int i = -1;
        for (DENOPTIMGraph graph : geParams.getInputGraphs())
        {
            i++;
            DENOPTIMGraph modGraph = graph.editGraph(
                    geParams.getGraphEditTasks(),
                    geParams.symmetryFlag(),
                    geParams.getVerbosity()
            );
           
            if (geParams.getVerbosity() > 0)
            {
                System.out.println("Original graph: ");
                System.out.println(graph.toString());
                System.out.println("Modified graph: ");
                System.out.println(modGraph.toString());
            }
            
            //TODO: upgrade to I/O with sepcification of format

            switch (geParams.getOutFormat())
            {
                case ("STRING"):
                {
                    DenoptimIO.writeData(geParams.getOutFile(),
                            modGraph.toString(),true);
                    break;
                }
                case ("SER"):
                {
                    DenoptimIO.serializeToFile(geParams.getOutFile(),
                            modGraph.toString(),true);
                    break;
                }
                case ("SDF"):
                {

                    ThreeDimTreeBuilder t3d = new ThreeDimTreeBuilder();
                    IAtomContainer newMol = t3d
                            .convertGraphTo3DAtomContainer(modGraph,true);
                    if (geParams.getInFormat().equals("SDF"))
                    {
                        IAtomContainer oldMol = geParams.getInpMol(i);
                        if (oldMol.getProperty("cdk:Title") != null)
                        {
                        	String name = oldMol.getProperty(
                                                    "cdk:Title").toString();
                        	newMol.setProperty("cdk:Title",name);
                        }
                    }
                    DenoptimIO.writeSDFFile(geParams.getOutFile(), newMol, true);
                    break;
                }
            }
        }
    }
    
//-----------------------------------------------------------------------------
    
}
