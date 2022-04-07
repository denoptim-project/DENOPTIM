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

package denoptim.programs.fitnessevaluator;

import java.io.File;

import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.exception.DENOPTIMException;
import denoptim.fitness.FitnessParameters;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.graph.Candidate;
import denoptim.graph.DGraph;
import denoptim.molecularmodeling.ThreeDimTreeBuilder;
import denoptim.programs.RunTimeParameters.ParametersType;
import denoptim.task.FitnessTask;
import denoptim.utils.GraphConversionTool;

/**
 * Task that calls the fitness provider
 */

public class FitnessEvaluationTask extends FitnessTask
{
    /**
     * Collection of settings controlling the execution of the task
     */
    private FRParameters frSettings = null;
    
    /**
     * Fragment space in use.
     */
    private FragmentSpace fragSpace;
    
//------------------------------------------------------------------------------
    
    /**
     * @param molGraph the DENOPTIM representation of the entity to evaluate
     * @param iac the molecular representation of the entity to evaluate
     * @param workDir where files will be placed.
     * @param outFileName filename of the output file.
     */
    public FitnessEvaluationTask(FRParameters settings, 
            DGraph molGraph, IAtomContainer iac, 
            String workDir, String outFileName)
    {
    	super((FitnessParameters) settings.getParameters(
                ParametersType.FIT_PARAMS), new Candidate(molGraph));
    	FragmentSpaceParameters fsParams = new FragmentSpaceParameters();
        if (settings.containsParameters(ParametersType.FS_PARAMS))
        {
            fsParams = (FragmentSpaceParameters)settings.getParameters(
                    ParametersType.FS_PARAMS);
        }
        this.fragSpace = fsParams.getFragmentSpace();
        this.workDir = new File(workDir);
        this.frSettings = settings;
        fitProvMol = iac;
        fitProvOutFile = outFileName;
    }

//------------------------------------------------------------------------------
    
    @Override
    public Object call() throws DENOPTIMException, Exception
    {
        // Optionally improve the molecular representation, which
        // is otherwise only given by the collection of building
        // blocks (not aligned, nor roto-translated)
        if (fitnessSettings.make3dTree())
        {
            ThreeDimTreeBuilder tb3d = new ThreeDimTreeBuilder();
            
            try {
                DGraph gWithNoRCVs = dGraph.clone();
                
                //NB: this replaces unused RCVs with capping groups
                GraphConversionTool.replaceUnusedRCVsWithCapps(gWithNoRCVs,
                        fragSpace);
                
                // To get a proper molecular representation we need
                // 1) build a 3d tree
                // 2) remove RCAs
                // 3) remove dummy in multi-hapto
                // 4) remove dummy in linearities
                // 5) set atom properties that are expected by CDK classes (for
                //    example, the number of implicit atoms).
                // All this should be done within the TreeBuilder3D and 
                // controlled by flags. Obviously, if we remove all these 
                // functional dummy atoms, then we cannot use them anymore,
                // So: is are there cases where we need to keep them?
                // We can always rebuild the 3d-tree (with Dummy atoms) if
                // we need the get it back. Thus, for the moment I do not see
                // a reason for keeping Du in the molecular representation,
                // but potential down-stream effects have to be evaluated.
                IAtomContainer mol = tb3d.convertGraphTo3DAtomContainer(
                        gWithNoRCVs,true);
                fitProvMol = mol;
            } catch (Throwable t) {
                //we have it already from before
            }
        }
        
        // Run the fitness provider, whatever that is
        try
        {
            runFitnessProvider();
        }
        catch (Throwable ex)
        {
            hasException = true;
            errMsg = "Exception while running fitness provider";
            thrownExc = ex;
            ex.printStackTrace();
            throw new DENOPTIMException(ex);
        }
        
        if (frSettings.addTemplatesToLibraries)
        {   
            fragSpace.addFusedRingsToFragmentLibrary(result.getGraph(),
                    true, true, fitProvMol);
        }

        completed = true;
        return result;
    }

//------------------------------------------------------------------------------
    
}
