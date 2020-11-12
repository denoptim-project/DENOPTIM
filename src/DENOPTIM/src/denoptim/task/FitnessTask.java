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

/**
 * Task that calls the fitness provider.
 */

public class FitnessTask extends DENOPTIMTask
{
    private final String molName;
    private final DENOPTIMGraph molGraph;
    private final String molsmiles;
    private final IAtomContainer molInit;
    private String molinchi;
    private final String workDir;
    private volatile ArrayList<DENOPTIMMolecule> curPopln;
    private volatile Integer numtry;
    private boolean completed = false;
    private boolean hasException = false;
    private String errMsg = "";
    private final String fileUID;

    private final String SEP = System.getProperty("file.separator");
    private final String NL = System.getProperty("line.separator");
    
    /**
     * A user-assigned id for this task.
     */
    private String id = null;

    private ProcessHandler ph_sc;

//------------------------------------------------------------------------------
    
    public boolean foundException()
    {
        return hasException;
    }

//------------------------------------------------------------------------------

    public String getErrorMessage()
    {
        return errMsg;
    }

//------------------------------------------------------------------------------
    
    /**
     * 
     * @param m_molName
     * @param m_molGraph
     * @param m_inchi
     * @param m_smiles
     * @param m_iac
     * @param m_dir
     * @param m_Id
     * @param m_popln reference to the current population. Can be null, in which
     * case this task will not add its entity to the population
     * @param m_try
     * @param m_fileUID
     */
    public FitnessTask(String m_molName, DENOPTIMGraph m_molGraph, String m_inchi,
            String m_smiles, IAtomContainer m_iac, String m_dir, int m_Id,
            ArrayList<DENOPTIMMolecule> m_popln, Integer m_try, String m_fileUID)
    {
        molName = m_molName;
        workDir = m_dir;
        molinchi = m_inchi;
        molInit = m_iac;
        molsmiles = m_smiles;
        molinchi = m_inchi;
        molGraph = m_molGraph;
        id = "" + m_Id;
        curPopln = m_popln;
        fileUID = m_fileUID;
        numtry = m_try;
        
        molInit.setProperty(CDKConstants.TITLE, molName);
        molInit.setProperty("GCODE", molGraph.getGraphId());
        molInit.setProperty("InChi", molinchi);
        molInit.setProperty("SMILES", molsmiles);
        molInit.setProperty("GraphENC", molGraph.toString());
        if (molGraph.getMsg() != null)
        {
            molInit.setProperty("GraphMsg", molGraph.getMsg());
        }
    }

//------------------------------------------------------------------------------
    
    @Override
    public Object call() throws DENOPTIMException, Exception
    {
        DENOPTIMMolecule result = new DENOPTIMMolecule();
        result.setName(molName);
        result.setMoleculeGraph(molGraph);
        result.setMoleculeUID(molinchi);
        result.setMoleculeSmiles(molsmiles);
        
        String molFinalFile = workDir + SEP + molName + "_FIT.sdf";
        String molINITFile = workDir + SEP + molName + "_I.sdf";
        String molImgfile = workDir + SEP + molName + ".png";
        

        result.setMoleculeFile(molFinalFile);

        //TODO change to allow other kinds of external tools (probably merge FitnessTask and FTask and put it under denoptim.fitness package
        // write the input for the fitness provider
        DenoptimIO.writeMolecule(molINITFile, molInit, false);

        String shell = System.getenv("SHELL");
        StringBuilder cmdStr = new StringBuilder();
        cmdStr.append(shell).append(" ")
                .append(FitnessParameters.getExternalFitnessProvider())
                .append(" ").append(molINITFile).append(" ").append(molFinalFile)
                .append(" ").append(workDir).append(" ").append(id)
                .append(" ").append(fileUID);
        
        String msg = "Calling fitness provider: => " + cmdStr + NL;
        DENOPTIMLogger.appLogger.log(Level.INFO, msg);

        ph_sc = new ProcessHandler(cmdStr.toString(), id);
        try
        {
            ph_sc.runProcess();
            cmdStr.setLength(0);

            if (ph_sc.getExitCode() != 0)
            {
                hasException = true;
                msg = "Failed to execute "
                             + System.getenv("SHELL")
                             + " script '"
                             + FitnessParameters.getExternalFitnessProvider()
                             + "' on " + molINITFile;
                errMsg = msg;
                DENOPTIMLogger.appLogger.severe(msg);
                DENOPTIMLogger.appLogger.severe(ph_sc.getErrorOutput());
                throw new DENOPTIMException(msg);
            }

            // read the molecular model returned by the fitness provider
            IAtomContainer processedMol = new AtomContainer();
            boolean unreadable = false;
            try
            {
                processedMol = DenoptimIO.readSingleSDFFile(molFinalFile);
                if (processedMol.isEmpty())
                {
                    unreadable=true;
                }
            }
            catch (Throwable t)
            {
                unreadable=true;
            }

            if (unreadable)
            {
                msg = "Unreadable FIT file for " + molName;
                DENOPTIMLogger.appLogger.log(Level.WARNING, msg);
                
                // make a copy of the unreadable FIT file that will be replaced
                String fitFileCp = workDir + SEP + molName 
                                                          + "_UnreadbleFIT.sdf";
                FileUtils.copyFile(new File(molFinalFile), 
                                   new File(fitFileCp));
                FileUtils.deleteQuietly(new File(molFinalFile));
                
                String err = "#FTask: Unable to retrive data. See " + fitFileCp;

                // make a readable FIT file with minimal data
                processedMol = new AtomContainer();
                processedMol.addAtom(new Atom("H"));
                processedMol.setProperty(CDKConstants.TITLE, molName);
                processedMol.setProperty("MOL_ERROR", err);
                processedMol.setProperty("GCODE", molGraph.getGraphId());
                processedMol.setProperty("GraphENC", molGraph.toString());
                DenoptimIO.writeMolecule(molFinalFile, processedMol, false);
                
                result.setError(err);
                completed = true;
                return result;
            }
            
            if (processedMol.getProperty("UID") != null)
            {
                result.setMoleculeUID(
                		processedMol.getProperty("UID").toString());
            } else
            {
                result.setMoleculeUID(molinchi);
            }

            if (processedMol.getProperty("MOL_ERROR") != null)
            {
            	String err = processedMol.getProperty("MOL_ERROR").toString();
                msg = "Structure " + molName + " has an error ("+err+")";
                DENOPTIMLogger.appLogger.info(msg);
                completed = true;

                synchronized (numtry)
                {
                    numtry++;
                }
                result.setError(err);
                completed = true;
                return result;
            }

            if (processedMol.getProperty("FITNESS") != null)
            {
                String fitprp = processedMol.getProperty("FITNESS").toString();
                double fitVal = 0.0;
                try
                {
                    fitVal = Double.parseDouble(fitprp);
                }
                catch (Throwable t)
                {
                    hasException = true;
                    msg = "Fitness value '" + fitprp + "' of " + molName 
                          + " could not be converted to double.";
                    errMsg = msg;
                    DENOPTIMLogger.appLogger.severe(msg);
                    molGraph.cleanup();
                    throw new DENOPTIMException(msg);
                }

                if (Double.isNaN(fitVal))
                {
                    hasException = true;
                    msg = "Fitness value is NaN for " + molName;
                    errMsg = msg;
                    DENOPTIMLogger.appLogger.severe(msg);
                    molGraph.cleanup();
                    throw new DENOPTIMException(msg);
                }

                processedMol.setProperty("GCODE", molGraph.getGraphId());                
                processedMol.setProperty("SMILES", molsmiles);
                processedMol.setProperty("GraphENC", molGraph.toString());
                if (molGraph.getMsg() != null)
                {
                    processedMol.setProperty("GraphMsg", molGraph.getMsg());
                }
                DenoptimIO.writeMolecule(molFinalFile, processedMol, false);

                result.setMoleculeFitness(fitVal);

                // add the newmol to the population list
                if (curPopln != null)
	            {
	                synchronized (curPopln)
	                {
	                	DENOPTIMLogger.appLogger.log(Level.INFO, 
	                			"Adding {0} to population", molName);
	                    curPopln.add(result);
	                }
	                synchronized (numtry)
	                {
	                    numtry--;
	                }
	            }
                
                // image creation
                if (FitnessParameters.makePictures())
                {
                    try
                    {
                        DENOPTIMMoleculeUtils.moleculeToPNG(processedMol, molImgfile);
                        result.setImageFile(molImgfile);
                    }
                    catch (Exception ex)
                    {
                        result.setImageFile(null);
                        DENOPTIMLogger.appLogger.log(Level.WARNING, 
                            "Unable to create image.{0}", ex.getMessage());
                    }
                }
            }
            else
            {
                hasException = true;
                msg = "Could not find \"FITNESS\" tag in file: " + molFinalFile;
                errMsg = msg;
                DENOPTIMLogger.appLogger.severe(msg);
                throw new DENOPTIMException(msg);
            }
            ph_sc = null;
        }
        catch (Exception ex)
        {
            hasException = true;
            throw new DENOPTIMException(ex);
        }

        completed = true;
        return result;
    }

//------------------------------------------------------------------------------
   
    /**
     * @return <code>true</code> if the task is completed
     */
    public boolean isCompleted()
    {
        return completed;
    }

//------------------------------------------------------------------------------
    
    public void stopTask()
    {
        if (completed)
        {
            return;
        }
        if (ph_sc != null)
        {
            System.err.println("Calling stop on Process: " + id + " " + molName);
            ph_sc.stopProcess();
        }
    }

//------------------------------------------------------------------------------
    
    @Override
    public String toString()
    {
        return id + " " + molName + " " + completed;
    }

//------------------------------------------------------------------------------
}
