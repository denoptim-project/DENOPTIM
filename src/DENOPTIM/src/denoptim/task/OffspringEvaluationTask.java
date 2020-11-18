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

package denoptim.task;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.exception.DENOPTIMException;
import denoptim.fitness.FitnessParameters;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMMolecule;
import denoptim.utils.DENOPTIMMoleculeUtils;
import denoptim.utils.TaskUtils;

/**
 * Task that calls the fitness provider for an offspring that can become a
 * member of the current population.
 */

public class OffspringEvaluationTask extends FitnessTask
{
    private final String molName;
    private volatile ArrayList<DENOPTIMMolecule> curPopln;
    private volatile Integer numtry;
    
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
        
        fitProvMol.setProperty(CDKConstants.TITLE, molName);
        fitProvMol.setProperty("GCODE", dGraph.getGraphId());
        fitProvMol.setProperty("InChi", inchi);
        fitProvMol.setProperty("SMILES", smiles);
        fitProvMol.setProperty("GraphENC", dGraph.toString());
        if (dGraph.getMsg() != null)
        {
        	fitProvMol.setProperty("GraphMsg", dGraph.getMsg());
        }
        
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
