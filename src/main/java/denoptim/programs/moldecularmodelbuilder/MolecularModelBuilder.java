/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no> and
 *   Marco Foscato <marco.foscato@uib.no>
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

package denoptim.programs.moldecularmodelbuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.graph.Candidate;
import denoptim.graph.DENOPTIMGraph;
import denoptim.integration.tinker.TinkerException;
import denoptim.io.DenoptimIO;
import denoptim.molecularmodeling.MultiMolecularModelBuilder;
import denoptim.programs.denovo.GAParameters;
import denoptim.task.ProgramTask;

/**
 * Builder of molecular models. This program constructs a three-dimensional
 * molecular model by converting a given DENOPTIM {@link Candidate}.
 * 
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class MolecularModelBuilder extends ProgramTask
{

//------------------------------------------------------------------------------
    
    /**
     * Creates and configures the program task.
     * @param configFile the file containing the configuration parameters.
     * @param workDir the file system location from which to run the program.
     */
    public MolecularModelBuilder(File configFile, File workDir)
    {
        super(configFile,workDir);
    }
    
//------------------------------------------------------------------------------

    @Override
    public void runProgram() throws Throwable
    {
        MMBuilderParameters mmbParams = new MMBuilderParameters();
        if (workDir != null)
        {
            mmbParams.setWorkDirectory(workDir.getAbsolutePath());
        }
        
        mmbParams.readParameterFile(configFilePathName.getAbsolutePath());
        mmbParams.checkParameters();
        mmbParams.processParameters();
        mmbParams.printParameters();
        
        // read the input molecule
        Candidate candidate = DenoptimIO.readCandidates(
                new File(mmbParams.getInputSDFFile()), true).get(0);
        DENOPTIMGraph grph = candidate.getGraph();
        String mname = candidate.getName();
        Map<Object,Object> properties = candidate.getChemicalRepresentation()
                .getProperties();
            
        MultiMolecularModelBuilder mbuild = 
                new MultiMolecularModelBuilder(mname, grph, mmbParams);

        boolean normalTerm = false;
        try {
            ArrayList<IAtomContainer> nmols = mbuild.buildMulti3DStructure();
            for (int i = 0; i<nmols.size(); i++)
            {
                //NB: here we reset the IAC properties, so, any property that
                // should be passed on to the future should be copied.
                String propVIDs = nmols.get(i).getProperty(
                        DENOPTIMConstants.ATMPROPVERTEXID).toString();
                Object propMolErr = nmols.get(i).getProperty(
                        DENOPTIMConstants.MOLERRORTAG);
                nmols.get(i).setProperties(properties);
                nmols.get(i).setProperty(
                        DENOPTIMConstants.ATMPROPVERTEXID, propVIDs);
                if (propMolErr != null)
                {
                    nmols.get(i).setProperty(DENOPTIMConstants.MOLERRORTAG, 
                            propMolErr.toString());
                }
            }
            DenoptimIO.writeSDFFile(mmbParams.getOutputSDFFile(), nmols);
            normalTerm = true;
        } catch (TinkerException te)
        {
            String msg = "ERROR! Tinker failed on task '" + te.taskName 
                    + "'!";
            if (te.solution != "")
            {
                msg = msg + NL + te.solution;
            }
            System.out.println(msg);
        } 
        catch (Exception de)
        {
            de.printStackTrace(System.err);
        } 
        if (normalTerm)
        {
            System.out.println("MolecularModelBuilder terminated normally!");
        }
    }
}
