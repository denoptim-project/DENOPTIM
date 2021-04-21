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

package denoptimga;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import denoptim.molecule.DENOPTIMTemplate;
import denoptim.molecule.DENOPTIMVertex;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fitness.FitnessParameters;
import denoptim.fragspace.FragmentSpace;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.Candidate;
import denoptim.task.FitnessTask;
import denoptim.threedim.ThreeDimTreeBuilder;
import denoptim.utils.GraphConversionTool;

import static denoptim.molecule.DENOPTIMVertex.*;
import static denoptimga.DENOPTIMGraphOperations.*;

/**
 * Task that calls the fitness provider for an offspring that can become a
 * member of the current population.
 */

public class OffspringEvaluationTask extends FitnessTask
{
    private final String molName;
    private volatile List<Candidate> curPopln;
    private volatile Integer numTry;
    
    /**
     * Tool for generating 3D models assembling 3D building blocks.
     */
    private ThreeDimTreeBuilder tb3d;
    
//------------------------------------------------------------------------------
    
    /**
     * 
     * @param molName
     * @param molGraph
     * @param inchi
     * @param smiles
     * @param workDir
     * @param popln reference to the current population. Can be null, in which
     * case this task will not add its entity to any population.
     * @param numTry
     * @param fileUID
     */
    public OffspringEvaluationTask(String molName, DENOPTIMGraph molGraph,
    		String inchi, String smiles, IAtomContainer iac, String workDir,
            List<Candidate> popln, Integer numTry, String fileUID)
    {
    	super(molGraph);
        this.molName = molName;
        this.workDir = workDir;
        fitProvMol = iac;
        curPopln = popln;
        this.numTry = numTry;
        
        result.setName(this.molName);
        result.setUID(inchi);
        result.setSmiles(smiles);
        
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
    	// Optionally improve the molecular representation, which
        // is otherwise only given by the collection of building
        // blocks (not aligned, nor roto-translated)
        if (FitnessParameters.make3dTree())
        {
        	ThreeDimTreeBuilder tb3d = new ThreeDimTreeBuilder();
        	
            try {
                DENOPTIMGraph gWithNoRCVs = dGraph.clone();
                GraphConversionTool.removeUnusedRCVs(gWithNoRCVs);
            	// To get a proper molecular representation we need
            	// 1) build a 3d tree
            	// 2) remove RCAs
            	// 3) remove dummy in multi-hapto
            	// 4) remove dummy in linearities
            	// All this should be done within the TreeBuilder3D and 
            	// controlled by flags. Obviously, if we remove all these 
            	// functional dummy atoms, then we cannot use them anymore,
            	// So: is are there cases where we need to keep them?
            	// We can always rebuild the 3d-tree (with Dummy atoms) if
            	// we need the get it back. Thus, for the moment I do not see
            	// a reason for keeping the Dus in the molecular representation,
            	// but potential down stream effects have to be evaluated.
                IAtomContainer mol = tb3d.convertGraphTo3DAtomContainer(
                        gWithNoRCVs,true);
                fitProvMol = mol;
        	} catch (Throwable t) {
        		//we have it already from before
        	}
        }
        fitProvMol.setProperty(CDKConstants.TITLE, molName);
        fitProvMol.setProperty("SMILES", result.getSmiles());
        fitProvMol.setProperty("InChi", result.getUID());
        fitProvMol.setProperty(DENOPTIMConstants.GCODETAG, 
        		dGraph.getGraphId());
        fitProvMol.setProperty(DENOPTIMConstants.UNIQUEIDTAG, 
        		result.getUID());
        fitProvMol.setProperty(DENOPTIMConstants.GRAPHTAG, dGraph.toString());
        if (dGraph.getLocalMsg() != null)
        {
        	fitProvMol.setProperty(DENOPTIMConstants.GMSGTAG, dGraph.getLocalMsg());
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

        if (result.getError() == null)
        {
            synchronized (numTry)
            {
                numTry++;
            }
        }

        if (result.hasFitness())
        {
        	if (curPopln != null)
        	{
	            synchronized (curPopln)
	            {
	            	DENOPTIMLogger.appLogger.log(Level.INFO, 
	            			"Adding {0} to population", molName);
	                curPopln.add(result);
	            }
        	}
            synchronized (numTry)
            {
                numTry--;
            }

            FragmentSpace.addRingSystemsToFragmentLibrary(result.getGraph());
        }
        completed = true;
        return result;
    }

//------------------------------------------------------------------------------
}
