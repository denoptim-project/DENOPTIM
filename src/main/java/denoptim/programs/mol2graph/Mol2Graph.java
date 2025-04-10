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

package denoptim.programs.mol2graph;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.ga.EAUtils;
import denoptim.graph.DGraph;
import denoptim.graph.rings.RingClosureParameters;
import denoptim.io.DenoptimIO;
import denoptim.programs.RunTimeParameters.ParametersType;
import denoptim.programs.fragmenter.FragmenterParameters;
import denoptim.task.ProgramTask;


/**
 * Tool for creating {@link DGraph}s from molecules.
 *
 * @author Marco Foscato
 */

public class Mol2Graph extends ProgramTask
{

//------------------------------------------------------------------------------
    
    /**
     * Creates and configures the program task.
     * @param configFile the file containing the configuration parameters.
     * @param workDir the file system location from which to run the program.
     */
    public Mol2Graph(File configFile, File workDir)
    {
        super(configFile,workDir);
    }
    
//------------------------------------------------------------------------------

    @Override
    public void runProgram() throws Throwable
    {
        Mol2GraphParameters m2gParams = new Mol2GraphParameters();
        m2gParams.readParameterFile(configFilePathName.getAbsolutePath());
        m2gParams.checkParameters();
        m2gParams.processParameters();
        m2gParams.startProgramSpecificLogger(loggerIdentifier, false); //to STDOUT
        
        List<DGraph> graphs = new ArrayList<DGraph>();
        for (int i=0; i<m2gParams.getInputMolsCount(); i++)
        {
            IAtomContainer mol = m2gParams.getInputMol(i);
            DGraph graph = null;
            try {
                graph = EAUtils.makeGraphFromFragmentationOfMol(mol, 
                        m2gParams.getCuttingRules(), 
                        m2gParams.getLogger(),
                        m2gParams.getScaffoldingPolicy(),
                        m2gParams.getLinearAngleLimit(),
                        m2gParams.embedRingsInTemplate(),
                        m2gParams.getEmbeddedRingsContract(),
                        m2gParams.getFragmentSpace(),
                        null); // monitor is not used here
            } catch (DENOPTIMException de)
            {
                m2gParams.getLogger().log(Level.SEVERE, "Unable to convert "
                        + "molecule " + i + " to DENOPTIM graph. " 
                        + de.getMessage());
                return;
            }
            
            graphs.add(graph);
        }
        
        if (graphs.size()>1)
        {
            DenoptimIO.writeGraphsToFile(new File(m2gParams.getOutFile()), 
                m2gParams.getOutFormat(), graphs, m2gParams.getLogger(),
                m2gParams.getRandomizer());
        } else {
            DenoptimIO.writeGraphToFile(new File(m2gParams.getOutFile()), 
                    m2gParams.getOutFormat(), graphs.get(0), m2gParams.getLogger(),
                    m2gParams.getRandomizer());
        }
    }
    
//-----------------------------------------------------------------------------
    
}
