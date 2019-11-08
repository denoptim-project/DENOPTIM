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

package preparemopac;

import denoptim.exception.DENOPTIMException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import denoptim.io.DenoptimIO;
import denoptim.task.ProcessHandler;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;




/**
 *
 * @author Vishwesh Venkatraman
 */
public class PrepareMOPAC
{

    private static final Logger LOGGER = Logger.getLogger(PrepareMOPAC.class.getName());
    private String OBABEL_EXE = "";
    private ArrayList<String> MOPAC_KEYWORDS = null;
    private String inpSDF = "";
    private String outMOP = "";
    private String kwdsFile = "";
    private String id;
    

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        if (args.length < 1)
        {
            System.err.println("Usage: java PrepareMOPACFile paramFile");
            System.exit(-1);
        }

        String paramFile = args[0];
        
        PrepareMOPAC pmf = new PrepareMOPAC();
        
        try
        {
            pmf.readParameters(paramFile);
            pmf.readMOPACKeywords(pmf.kwdsFile);
            IAtomContainer mol = DenoptimIO.readSingleSDFFile(pmf.inpSDF);
            DenoptimIO.writeMolecule(pmf.inpSDF, mol, false);
            
            // calculate charge
            double charge = AtomContainerManipulator.getTotalFormalCharge(mol);

            String keywds = pmf.MOPAC_KEYWORDS.get(0);

            if (Math.abs(charge) > 0)
            {
                keywds  += " CHARGE=" + charge;
            }
            
		
            String cmdStr = pmf.OBABEL_EXE + " -isdf " + 
                    pmf.inpSDF + " -omopin -O " + pmf.outMOP + " -xk " + keywds;
            
            ProcessHandler ph_sc = new ProcessHandler(cmdStr, pmf.id);
        
            ph_sc.runProcess();

            if (ph_sc.getExitCode() != 0)
            {
                String msg = "Failed to execute command " + cmdStr;
                LOGGER.log(Level.SEVERE, msg);
                LOGGER.log(Level.SEVERE, ph_sc.getErrorOutput());
                System.exit(-1);
            }
            
            if (pmf.MOPAC_KEYWORDS.size() > 1)
            {
                StringBuilder sb = new StringBuilder(64);

                for (int i=1; i<pmf.MOPAC_KEYWORDS.size(); i++)
                {
                    sb.append("\n");
                    sb.append(pmf.MOPAC_KEYWORDS.get(i));
                }

                DenoptimIO.writeData(pmf.outMOP, sb.toString(), true);
            }
            
        }
        catch (DENOPTIMException ioe)
        {
            LOGGER.log(Level.SEVERE, null, ioe);
            System.exit(-1);
        }
        catch (Exception ex)
        {
            LOGGER.log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
        
        
        System.exit(0);        
    }

//------------------------------------------------------------------------------	
    
    private void readParameters(String filename) throws DENOPTIMException
    {
        BufferedReader br = null;
        String option, line;

        try
        {
            br = new BufferedReader(new FileReader(filename));
            while ((line = br.readLine()) != null)
            {
                line = line.trim();
                if (line.length() == 0)
                {
                    continue;
                }
                
                if (line.startsWith("#"))
                {
                    continue;
                }
                
                if (line.toUpperCase().startsWith("TOOLOPENBABEL"))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        OBABEL_EXE = option.trim();
                    }
                    continue;
                }
                
                if (line.toUpperCase().startsWith("INPSDF"))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        inpSDF = option.trim();
                    }
                    continue;
                }
                
                if (line.toUpperCase().startsWith("MOPFILE"))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        outMOP = option.trim();
                    }
                    continue;
                }
                
                if (line.toUpperCase().startsWith("KEYWORDS"))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        kwdsFile = option.trim();
                    }
                    continue;
                }
                
                if (line.toUpperCase().startsWith("TASKID"))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        id = option.trim();
                    }
                    continue;
                }
            }

        }
        catch (IOException ioe)
        {
            throw new DENOPTIMException(ioe);
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
    
    private void readMOPACKeywords(String filename) throws DENOPTIMException
    {
        BufferedReader br = null;
        String line = null;

        MOPAC_KEYWORDS = new ArrayList<>();

        try
        {
            br = new BufferedReader(new FileReader(filename));
            while ((line = br.readLine()) != null)
            {
                if ((line.trim()).length() == 0)
                {
                    continue;
                }
                if (line.startsWith("#"))
                {
                    continue;
                }
                MOPAC_KEYWORDS.add(line.trim());
            }
        }
        catch (IOException ioe)
        {
            throw new DENOPTIMException(ioe);
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

}
