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

package denoptim.ga;

import java.io.File;
import java.util.logging.Level;

import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fitness.FitnessParameters;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.graph.Candidate;
import denoptim.graph.DGraph;
import denoptim.logging.CounterID;
import denoptim.logging.StaticLogger;
import denoptim.logging.Monitor;
import denoptim.molecularmodeling.ThreeDimTreeBuilder;
import denoptim.programs.RunTimeParameters.ParametersType;
import denoptim.programs.denovo.GAParameters;
import denoptim.task.FitnessTask;
import denoptim.utils.GraphConversionTool;

/**
 * Task that calls the fitness provider for an offspring that can become a
 * member of the current population.
 */

public class OffspringEvaluationTask extends FitnessTask
{
    private final String molName;
    private volatile Population population;
    private volatile Monitor mnt;
    
    /**
     * Parameters controlling GA experiments
     */
    private GAParameters gaSettings;
    
    /**
     * The fragment space
     */
    private FragmentSpace fragSpace;
    
//------------------------------------------------------------------------------
    
    public OffspringEvaluationTask(GAParameters gaSettings, 
            Candidate offspring, String workDir,
            Population popln, Monitor mnt, String fileUID)
    {
        super(((FitnessParameters) gaSettings.getParameters(
                ParametersType.FIT_PARAMS)), offspring);
        this.gaSettings = gaSettings;
        FragmentSpaceParameters fsParams = new FragmentSpaceParameters();
        if (gaSettings.containsParameters(ParametersType.FS_PARAMS))
        {
            fsParams = (FragmentSpaceParameters)gaSettings.getParameters(
                    ParametersType.FS_PARAMS);
        }
        this.fragSpace = fsParams.getFragmentSpace();
        this.molName = offspring.getName();
        this.workDir = new File(workDir);
        this.fitProvMol = offspring.getChemicalRepresentation();
        this.population = popln;
        this.mnt = mnt;
        
        result.setName(this.molName);
        result.setUID(offspring.getUID());
        result.setSmiles(offspring.getSmiles());
        
        // Define pathnames to files used/produced by fitness provider
        fitProvOutFile = this.workDir + SEP + this.molName + 
                DENOPTIMConstants.FITFILENAMEEXTOUT;
        fitProvInputFile = this.workDir + SEP + this.molName + 
                DENOPTIMConstants.FITFILENAMEEXTIN;
        fitProvPNGFile = this.workDir + SEP + this.molName + 
                DENOPTIMConstants.CANDIDATE2DEXTENSION;
        fitProvUIDFile = fileUID;
    }

//------------------------------------------------------------------------------
    
    @Override
    public Object call() throws DENOPTIMException, Exception
    {     
        mnt.increase(CounterID.FITNESSEVALS);
          
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
            	// So: are there cases where we need to keep them?
            	// We can always rebuild the 3d-tree (with Dummy atoms) if
            	// we need to get it back. Thus, for the moment I do not see
            	// a reason for keeping them in the molecular representation,
            	// but potential down-stream effects have to be evaluated.
                IAtomContainer mol = tb3d.convertGraphTo3DAtomContainer(
                        gWithNoRCVs,true);
                fitProvMol = mol;
        	} catch (Throwable t) {
        		//we have it already from before
        	}
        }
        fitProvMol.setProperty(CDKConstants.TITLE, molName);
        fitProvMol.setProperty(DENOPTIMConstants.SMILESTAG, result.getSmiles());
        fitProvMol.setProperty(DENOPTIMConstants.INCHIKEYTAG, result.getUID());
        fitProvMol.setProperty(DENOPTIMConstants.GCODETAG, 
        		dGraph.getGraphId());
        fitProvMol.setProperty(DENOPTIMConstants.UNIQUEIDTAG, 
        		result.getUID());
        fitProvMol.setProperty(DENOPTIMConstants.GRAPHTAG, dGraph.toString());
        fitProvMol.setProperty(DENOPTIMConstants.GRAPHJSONTAG, dGraph.toJson());
        if (dGraph.getLocalMsg() != null)
        {
        	fitProvMol.setProperty(DENOPTIMConstants.GMSGTAG, dGraph.getLocalMsg());
        }
        
        // Run the fitness provider, whatever that is (internal or external)
        try
        {
            // Note that in here the molecular representation of the offspring 
            // is altered and the original references to fitProvMol will be lost.
            runFitnessProvider();
        }
        catch (Throwable ex)
        {
            mnt.increase(CounterID.FAILEDFITNESSEVALS);
            hasException = true;
            errMsg = "Exception while running fitness provider";
            thrownExc = ex;
            ex.printStackTrace();
            throw new DENOPTIMException(ex);
        }

        if (result.getError() != null)
        {
            mnt.increase(CounterID.FAILEDFITNESSEVALS);
        }

        if (result.hasFitness())
        {
            boolean isWithinBestPrcentile = false;
        	if (population != null)
        	{
	            synchronized (population)
	            {
	                gaSettings.getLogger().log(Level.INFO, 
	            			"Adding {0} to population", molName);
	                population.add(result);
	                isWithinBestPrcentile = population.isWithinPercentile(
                            result.getFitness(),
                            gaSettings.getSaveRingSystemsFitnessThreshold());
                }
        	}

        	if ((gaSettings.getSaveRingSystemsAsTemplatesNonScaff()
        	        || gaSettings.getSaveRingSystemsAsTemplatesScaff())
        	    && isWithinBestPrcentile)
        	{   
        	    fragSpace.addFusedRingsToFragmentLibrary(result.getGraph(),
                        gaSettings.getSaveRingSystemsAsTemplatesScaff(),
                        gaSettings.getSaveRingSystemsAsTemplatesNonScaff(),
                        fitProvMol);
        	}
        }
        completed = true;
        return result;
    }

//------------------------------------------------------------------------------
}
