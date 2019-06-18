/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no>
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

import java.util.logging.Level;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;


import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.interfaces.IAtomContainer;

import io.DenoptimIO;
import exception.DENOPTIMException;
import logging.DENOPTIMLogger;
import molecule.DENOPTIMGraph;
import molecule.DENOPTIMMolecule;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.io.MDLV3000Reader;
import task.DENOPTIMTask;
import task.ProcessHandler;
import utils.DENOPTIMMoleculeUtils;



/**
 *
 * @author Vishwesh Venkatraman
 */
public class FitnessTask extends DENOPTIMTask
{
    private final String molName;
    private final DENOPTIMGraph molGraph;
    private final String molsmiles;
    private final IAtomContainer molInit;
    private final String molinchi;
    private final String workDir;
    private boolean completed;
    private final String fileUID;

    private ProcessHandler ph_sc;


//------------------------------------------------------------------------------

    public FitnessTask(String m_molName, DENOPTIMGraph m_molGraph, String m_inchi,
                    String m_smiles, IAtomContainer m_iac, String m_dir, int m_Id,
                    String m_fileUID)
    {
        molName = m_molName;
        workDir = m_dir;
        molinchi = m_inchi;
        molInit = m_iac;
        molsmiles = m_smiles;
        molGraph = m_molGraph;
        fileUID = m_fileUID;
        super.setId(m_Id);
    }

//------------------------------------------------------------------------------

    /**
     *
     * @return
     * @throws DENOPTIMException
     */
    
    @Override
    public Object call() throws DENOPTIMException
    {
        DENOPTIMMolecule newmol = new DENOPTIMMolecule();
        String fsep = System.getProperty("file.separator");

        String molFinalFile = workDir + fsep + molName + "_FIT.sdf";
        String molInitFile = workDir + fsep + molName + "_I.sdf";
        
        String molImgfile = workDir + fsep + molName + ".png";
        
        String msg = super.getId() + " " + molName + " " + molsmiles + " " + molinchi +
                        " :- Attempting fitness evaluation..." + "\n";
        DENOPTIMLogger.appLogger.info(msg);

        // set the graph
        molInit.setProperty(CDKConstants.TITLE, molName);
        molInit.setProperty("GCODE", molGraph.getGraphId());
        molInit.setProperty("UID", molinchi);
        molInit.setProperty("SMILES", molsmiles);
        molInit.setProperty("GraphENC", molGraph.toString());
        if (molGraph.getMsg() != null)
        {
            molInit.setProperty("GraphMsg", molGraph.getMsg());
        } 
        

        // write the 2D file
        DenoptimIO.writeMolecule(molInitFile, molInit, false);

        StringBuilder cmdStr = new StringBuilder();
        
        cmdStr.append(System.getenv("SHELL")).append(" ")
            .append(GAParameters.fitnessEvalScript)
            .append(" ").append(molInitFile).append(" ").append(molFinalFile)
            .append(" ").append(workDir).append(" ").append(super.getId())
            .append(" ").append(fileUID);


        DENOPTIMLogger.appLogger.log(Level.INFO, "Executing: {0}", cmdStr);

        String id = super.getId() + "";
        ph_sc = new ProcessHandler(cmdStr.toString(), id);

        try
        {
            ph_sc.runProcess();

            if (ph_sc.getExitCode() != 0)
            {
                msg = "Failed to execute shell script " + GAParameters.fitnessEvalScript +
                    " on " + molInitFile;
                DENOPTIMLogger.appLogger.severe(msg);
                DENOPTIMLogger.appLogger.severe(ph_sc.getErrorOutput());
                throw new DENOPTIMException(msg);
            }
            
// MF: it is better to let the shell script decide whether to keep the input file
//            DenoptimIO.deleteFile(molInitFile);
            cmdStr.setLength(0);

            // read the conformation (lowest energy)
            IAtomContainer mol3DFinal = DenoptimIO.readSingleSDFFile(molFinalFile);

            if (mol3DFinal.getProperty("MOL_ERROR") != null)
            {
                msg = "Structure " + molName + " has an error.";
                DENOPTIMLogger.appLogger.info(msg);
                completed = true;
                molGraph.cleanup();
                mol3DFinal.removeAllElements();
                molInit.removeAllElements();
                return newmol;
            }

            if (mol3DFinal.getProperty("FITNESS") != null)
            {
                String fitprp = mol3DFinal.getProperty("FITNESS").toString();
                double fitVal = Double.parseDouble(fitprp);

                if (Double.isNaN(fitVal))
                {
                    msg = "Fitness value is NaN for " + molName;
                    DENOPTIMLogger.appLogger.severe(msg);
                    throw new DENOPTIMException(msg);
                }

                
                mol3DFinal.setProperty("GCODE", molGraph.getGraphId());
                mol3DFinal.setProperty("SMILES", molsmiles);
                mol3DFinal.setProperty("GraphENC", molGraph.toString());
                if (molGraph.getMsg() != null)
                {
                    mol3DFinal.setProperty("GraphMsg", molGraph.getMsg());
                    DenoptimIO.writeMolecule(molFinalFile, mol3DFinal, false);
                }

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
            }
            else
            {
                msg = "Could not find \"FITNESS\" tag in file: " + molFinalFile;
                DENOPTIMLogger.appLogger.severe(msg);
                throw new DENOPTIMException(msg);
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
            
            ph_sc = null;
        }
        catch (Exception ex)
        {
            throw new DENOPTIMException(ex);
        }

        // read fitness, AD, the 3D coordinates
        // FILE format for this should be as follows

        // 2/3 column format for the fitness where the first column must have the
        // word "FITNESS" followed by the fitness value and optionally an
        // estimate of the AD (in case you are using a QSPR model)
        // FITNESS: VAL1 VAL2

        // the 3D coordinates if these have been generated, in XYZ format
        // if 2D then nothing. The presence of these must be indicated by the
        // tag "COORDINATES"
        // COORDINATES
        // A X1 Y1 Z1
        // . .  .   .
        // A XN YN ZN

        // If a QSPR model was used, you can optionally list the descriptors
        // calculated for the new molecule
        // DESC
        // D1 D2 ... DN

        // Alternatively, an SDF containing the coordinates, fitness/AD and the
        // descriptors. Fitness must be entered with the tag "FITNESS"

        completed = true;
        return newmol;
    }

//------------------------------------------------------------------------------

    @Override
    public void stopTask()
    {
        if (completed)
           return;
        System.err.println("Calling stop on Process: " + super.getId());
        if (ph_sc != null)
            ph_sc.stopProcess();
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
                    for (int i=0; i<arr.length; i++)
                        dc.add(Double.parseDouble(arr[i]));
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
