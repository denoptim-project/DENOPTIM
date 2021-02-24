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

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fitness.FitnessParameters;
import denoptim.fragspace.FragmentSpace;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMMolecule;
import denoptim.task.FitnessTask;
import denoptim.threedim.TreeBuilder3D;
import denoptim.utils.DENOPTIMMoleculeUtils;
import denoptim.utils.TaskUtils;
import fragspaceexplorer.FSEParameters;

/**
 * Task that calls the fitness provider for an offspring that can become a
 * member of the current population.
 */

public class OffspringEvaluationTask extends FitnessTask
{
    private final String molName;
    private volatile ArrayList<DENOPTIMMolecule> curPopln;
    private volatile Integer numtry;
    
    /**
     * Tool for generating 3D models assembling 3D building blocks.
     */
    private TreeBuilder3D tb3d;
    
//------------------------------------------------------------------------------
    
    /**
     * 
     * @param m_molName
     * @param m_molGraph
     * @param m_inchi
     * @param m_smiles
     * @param m_dir
     * @param m_Id
     * @param m_popln reference to the current population. Can be null, in which
     * case this task will not add its entity to any population.
     * @param m_try
     * @param m_fileUID
     */
    public OffspringEvaluationTask(String m_molName, DENOPTIMGraph m_molGraph, 
    		String inchi,
            String smiles, IAtomContainer m_iac, String m_dir,
            ArrayList<DENOPTIMMolecule> m_popln, Integer m_try, String m_fileUID)
    {
    	super(m_molGraph);
        molName = m_molName;
        workDir = m_dir;
        fitProvMol = m_iac;
        curPopln = m_popln;
        numtry = m_try;
        
        result.setName(molName);
        result.setMoleculeUID(inchi);
        result.setMoleculeSmiles(smiles);
        
        // Define pathnames to files used/produced by fitness provider
        //TODO use constants
        fitProvOutFile = workDir + SEP + molName + "_FIT.sdf";
        fitProvInputFile = workDir + SEP + molName + "_I.sdf";
        fitProvPNGFile = workDir + SEP + molName + ".png";
        fitProvUIDFile = m_fileUID;
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
        	TreeBuilder3D tb3d = new TreeBuilder3D(
        			FragmentSpace.getScaffoldLibrary(),
        			FragmentSpace.getFragmentLibrary(),
        			FragmentSpace.getCappingLibrary());
        	
            try {
            	// TODO-V3
            	// To get a proper molecular representation we need
            	// 1) build a 3d tree
            	// 2) remove RCAs
            	// 3) remove dummy in multy hapto
            	// 4) remove dummy in linearities
            	// All this should be done within the TreeBuilder3D and 
            	// controlled by flags. Obviously, if we remove all these 
            	// functional dummy atoms, then we cannot use them anymore,
            	// So: is are there cases where we need to keep them?
            	// We can always rebuild the 3d-tree (with Dummy atoms) if
            	// we need the get it back. Thus, for the moment I do not see
            	// a reason for keeping the Dus in the molecular representation,
            	// but potential down stream effects have to be evaluated.
                IAtomContainer mol = tb3d.convertGraphTo3DAtomContainer(dGraph,true);
                fitProvMol = mol;
        	} catch (Throwable t) {
        		//we have it already from before
        	}
        }
        fitProvMol.setProperty(CDKConstants.TITLE, molName);
        fitProvMol.setProperty("SMILES", result.getMoleculeSmiles());
        fitProvMol.setProperty("InChi", result.getMoleculeUID());
        fitProvMol.setProperty(DENOPTIMConstants.GCODETAG, 
        		dGraph.getGraphId());
        fitProvMol.setProperty(DENOPTIMConstants.UNIQUEIDTAG, 
        		result.getMoleculeUID());
        fitProvMol.setProperty(DENOPTIMConstants.GRAPHTAG, dGraph.toString());
        if (dGraph.getMsg() != null)
        {
        	fitProvMol.setProperty(DENOPTIMConstants.GMSGTAG, dGraph.getMsg());
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
            synchronized (numtry)
            {
                numtry++;
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
            synchronized (numtry)
            {
                numtry--;
            }
        }
        completed = true;
        return result;
    }

//------------------------------------------------------------------------------

}
