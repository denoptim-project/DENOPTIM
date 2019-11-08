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
import java.util.logging.Level;
import java.util.concurrent.Callable;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.Atom;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.io.MDLV3000Reader;

import org.apache.commons.io.FileUtils;

import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMMolecule;
import denoptim.task.ProcessHandler;
import denoptim.utils.DENOPTIMMoleculeUtils;

/**
 *
 * @author Vishwesh Venkatraman and Marco Foscato
 */
public class FTask implements Callable
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
    
    public String getId()
    {
        return id;
    }

//------------------------------------------------------------------------------
    
    public FTask(String m_molName, DENOPTIMGraph m_molGraph, String m_inchi,
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
    }

//------------------------------------------------------------------------------
    
    @Override
    public Object call() throws DENOPTIMException, Exception
    {
        DENOPTIMMolecule newmol = new DENOPTIMMolecule();
        String fsep = System.getProperty("file.separator");

        String molFinalFile = workDir + fsep + molName + "_FIT.sdf";
        String molINITFile = workDir + fsep + molName + "_I.sdf";


        String molImgfile = workDir + fsep + molName + ".png";
        String msg = id + " " + molName + " " + molsmiles + " " + molinchi
                + " :- Attempting fitness evaluation..." + "\n";

        DENOPTIMLogger.appLogger.info(msg);

        // set the graph
        molInit.setProperty(CDKConstants.TITLE, molName);
        molInit.setProperty("GCODE", molGraph.getGraphId());
        molInit.setProperty("InChi", molinchi);
        molInit.setProperty("SMILES", molsmiles);
        molInit.setProperty("GraphENC", molGraph.toString());
        
        if (molGraph.getMsg() != null)
        {
            molInit.setProperty("GraphMsg", molGraph.getMsg());
        }

        // write the 2D file
        DenoptimIO.writeMolecule(molINITFile, molInit, false);

        String shell = System.getenv("SHELL");
        StringBuilder cmdStr = new StringBuilder();
        cmdStr.append(shell).append(" ").append(GAParameters.fitnessEvalScript)
                .append(" ").append(molINITFile).append(" ").append(molFinalFile)
                .append(" ").append(workDir).append(" ").append(id)
                .append(" ").append(fileUID);
        
        DENOPTIMLogger.appLogger.log(Level.INFO, "Executing: {0}", cmdStr);

        ph_sc = new ProcessHandler(cmdStr.toString(), id);
        try
        {
            ph_sc.runProcess();
            cmdStr.setLength(0);

            if (ph_sc.getExitCode() != 0)
            {
                hasException = true;
                msg = "Failed to execute shell script " 
			+ GAParameters.fitnessEvalScript
                        + " on " + molINITFile;
		errMsg = msg;
                DENOPTIMLogger.appLogger.severe(msg);
                DENOPTIMLogger.appLogger.severe(ph_sc.getErrorOutput());
                throw new DENOPTIMException(msg);
            }

            // read the molecular model returned by the fitness provider
	    IAtomContainer mol3DFinal = new AtomContainer();
	    boolean unreadable=false;
	    try
	    {
                mol3DFinal = DenoptimIO.readSingleSDFFile(molFinalFile);
		if (mol3DFinal.isEmpty())
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
		
		// make a copy of theunreadable FIT file that will be replaced
		String molFFileCp = workDir + fsep + molName 
							  + "_UnreadbleFIT.sdf";
		FileUtils.copyFile(new File(molFinalFile), 
				   new File(molFFileCp));
		FileUtils.deleteQuietly(new File(molFinalFile));

		// make a readable FIT file with minimal data
		mol3DFinal = new AtomContainer();
		mol3DFinal.addAtom(new Atom("H"));
		mol3DFinal.setProperty(CDKConstants.TITLE, molName);
		mol3DFinal.setProperty("MOL_ERROR", 
			   "#FTask: Unable to retrive dats. See " + molFFileCp);
                mol3DFinal.setProperty("GCODE", molGraph.getGraphId());
                mol3DFinal.setProperty("GraphENC", molGraph.toString());
		DenoptimIO.writeMolecule(molFinalFile, mol3DFinal, false);
	    }

            if (mol3DFinal.getProperty("MOL_ERROR") != null)
            {
                msg = "Structure " + molName + " has an error.";
                DENOPTIMLogger.appLogger.info(msg);
                completed = true;

                synchronized (numtry)
                {
                    numtry++;
                }
                molGraph.cleanup();
                mol3DFinal.removeAllElements();
                molInit.removeAllElements();
                mol3DFinal = null;
                return "FAIL";
            }

            if (mol3DFinal.getProperty("FITNESS") != null)
            {
                String fitprp = mol3DFinal.getProperty("FITNESS").toString();
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

                mol3DFinal.setProperty("GCODE", molGraph.getGraphId());                
                mol3DFinal.setProperty("SMILES", molsmiles);
                mol3DFinal.setProperty("GraphENC", molGraph.toString());
                if (molGraph.getMsg() != null)
                {
                    mol3DFinal.setProperty("GraphMsg", molGraph.getMsg());
                }
                DenoptimIO.writeMolecule(molFinalFile, mol3DFinal, false);

                newmol.setMoleculeFitness(fitVal);
                if (mol3DFinal.getProperty("UID") != null)
                {
                    newmol.setMoleculeUID(mol3DFinal.getProperty("UID").toString());
                }
                else
                newmol.setMoleculeUID(molinchi);
                newmol.setMoleculeSmiles(molsmiles);
                newmol.setMoleculeGraph(molGraph);
                newmol.setMoleculeFile(molFinalFile);

                // add the newmol to the population list
                synchronized (curPopln)
                {
                    curPopln.add(newmol);
                }
                synchronized (numtry)
                {
                    numtry--;
                }
                
                
                // image creation
                if (GAParameters.getGraphicsCreationStatus())
                {
                    try
                    {
                        DENOPTIMMoleculeUtils.moleculeToPNG(mol3DFinal, molImgfile);
                        newmol.setImageFile(molImgfile);
                    }
                    catch (Exception ex)
                    {
                        newmol.setImageFile(null);
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
        return "PASS";
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
    
    private void readTextFormat(String infile) throws DENOPTIMException
    {
        BufferedReader br = null;
        String line;

        int cline = -1;
        boolean fndFitness = false;
        StringBuilder sbCoords = new StringBuilder();
        ArrayList<ArrayList<Double>> desc = new ArrayList<>();

        try
        {
            br = new BufferedReader(new FileReader(infile));

            while ((line = br.readLine()) != null)
            {
                if ((line.trim()).length() == 0)
                {
                    continue;
                }

                if (line.toUpperCase().startsWith("FITNESS"))
                {
                    cline = 1;
                    fndFitness = true;
                    continue;
                }

                if (line.toUpperCase().startsWith("COORDINATES"))
                {
                    cline = 2;
                    continue;
                }

                if (line.toUpperCase().startsWith("DESCRIPTORS"))
                {
                    cline = 3;
                    continue;
                }

                if (cline == 1)
                {

                }

                if (cline == 2)
                {
                    sbCoords.append(line.trim());
                    continue;
                }

                if (cline == 3)
                {
                    String[] arr = line.split("\\s+");
                    ArrayList<Double> dc = new ArrayList<>();
                    for (int i = 0; i < arr.length; i++)
                    {
                        dc.add(Double.parseDouble(arr[i]));
                    }
                    desc.add(dc);
                }

            }

            if (cline == -1)
            {
                throw new DENOPTIMException("No relevant data found in file.");
            }

            if (!fndFitness)
            {
                throw new DENOPTIMException("No FITNESS data found in file.");
            }

        }
        catch (NumberFormatException | IOException nfe)
        {
            throw new DENOPTIMException(nfe);
        }
        finally
        {
            try
            {
                if (br != null)
                {
                    br.close();
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }
    }

//------------------------------------------------------------------------------
    
    public IAtomContainer readCoordinates(String data) throws DENOPTIMException
    {
        MDLV3000Reader mdlreader = null;
        IAtomContainer molecule = DefaultChemObjectBuilder.getInstance().newInstance(IAtomContainer.class);

        try
        {
            mdlreader = new MDLV3000Reader(new StringReader(data));
            molecule = mdlreader.read(molecule);
            //System.err.println(molecule.getAtomCount());
        }
        catch (CDKException cdke)
        {
            throw new DENOPTIMException(cdke);
        }
        finally
        {
            try
            {
                if (mdlreader != null)
                {
                    mdlreader.close();
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }

        return molecule;
    }

//------------------------------------------------------------------------------
}
