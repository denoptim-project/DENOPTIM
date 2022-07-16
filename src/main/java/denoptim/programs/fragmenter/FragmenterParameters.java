/*
 *   DENOPTIM
 *   Copyright (C) 2019 Marco Foscato <marco.foscato@uib.no>
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

package denoptim.programs.fragmenter;

import java.io.File;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import denoptim.combinatorial.CEBLUtils;
import denoptim.combinatorial.CheckPoint;
import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.files.FileFormat;
import denoptim.files.FileUtils;
import denoptim.graph.DGraph;
import denoptim.io.DenoptimIO;
import denoptim.logging.StaticLogger;
import denoptim.programs.RunTimeParameters;


/**
 * Parameters controlling execution of the frqgmenter.
 * 
 * @author Marco Foscato
 */

public class FragmenterParameters extends RunTimeParameters
{
    /**
     * Pathname to the file containing the structures of the molecules 
     * to fragment.
     */
    private String structuresFile;
    
    /**
     * Pathname to the file containing the formulae of the molecules 
     * to fragment
     */
    private String formulaeFile;
    
    /**
     * Pathname to the file containing the cutting rules.
     */
    private String cutRulesFile;
    
    /**
     * List of cutting rules
     */
    List<CuttingRule> cuttingRules;
    
    /**
     * List of known 'any-atom' SMARTS queries. We keep this information to
     * by-pass any attempt to search for any atom.
     */
    List<String> anyAtomQuesties;
    
    /**
     * Number of parallel tasks to run.
     */
    private int numParallelTasks = 1;


//-----------------------------------------------------------------------------
    
    /**
     * Constructor
     */
    public FragmenterParameters()
    {
        super(ParametersType.FRG_PARAMS);
    }

//-----------------------------------------------------------------------------

    /**
     * @return the number of parallel tasks to run.
     */
    public int getNumTasks()
    {
        return numParallelTasks;
    }

//-----------------------------------------------------------------------------

    /**
     * Processes a keyword/value pair and assign the related parameters.
     * @param key the keyword as string
     * @param value the value as a string
     * @throws DENOPTIMException
     */
    public void interpretKeyword(String key, String value)
            throws DENOPTIMException
    {
        String msg = "";
        switch (key.toUpperCase())
        {
            case "WORKDIR=":
                workDir = value;
                break;
                
            case "STRUCTURESFILE=":
                structuresFile = value;
                break;

            case "FORMULATXTFILE=":
                formulaeFile = value;
                break;

            case "CUTTINGRULESFILE=":
                cutRulesFile = value;
                break;
                
/*
            case "=":
                = value;
                break;
  */              
            case "NUMOFPROCESSORS=":
                try
                {
                    numParallelTasks = Integer.parseInt(value);
                }
                catch (Throwable t)
                {
                    msg = "Unable to understand value " + key + "'" + value + "'";
                    throw new DENOPTIMException(msg);
                }
                break;
                
            case "VERBOSITY=":
                try
                {
                    verbosity = Integer.parseInt(value);
                }
                catch (Throwable t)
                {
                    msg = "Unable to understand value " + key + "'" + value + "'";
                    throw new DENOPTIMException(msg);
                }
                break;
                
            default:
                 msg = "Keyword " + key + " is not a known Fragmenter-" 
                		 + "related keyword. Check input files.";
                throw new DENOPTIMException(msg);
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Evaluate consistency of input parameters.
     * @throws DENOPTIMException
     */
    
    public void checkParameters() throws DENOPTIMException
    {
    	if (!workDir.equals(System.getProperty("user.dir")))
    	{
    	    ensureFileExists(workDir);
    	}
    	ensureFileExistsIfSet(structuresFile);
    	ensureFileExistsIfSet(cutRulesFile);
    	ensureFileExistsIfSet(formulaeFile);
    	
    	
    	checkOtherParameters();
    }

//----------------------------------------------------------------------------

    /**
     * Processes all parameters and initialize related objects.
     * @throws DENOPTIMException
     */

    public void processParameters() throws DENOPTIMException
    {
        if (isMaster)
            createWorkingDirectory();
        
        anyAtomQuesties = new ArrayList<String>();
        cuttingRules = new ArrayList<CuttingRule>();
        DenoptimIO.readCuttingRules(new File(cutRulesFile), anyAtomQuesties, 
                cuttingRules);
		
        processOtherParameters();
       
		if (isMaster)
		{
            StaticLogger.appLogger.log(Level.INFO, "Program log file: " 
                    + logFile + DENOPTIMConstants.EOL 
                    + "Output files associated with the current run are "
                    + "located in " + workDir);
		}
    }
    
//------------------------------------------------------------------------------
    
    private void createWorkingDirectory()
    {
        String curDir = workDir;
        String fileSep = System.getProperty("file.separator");
        boolean success = false;
        while (!success)
        {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddkkmmss");
            String str = "FRG" + sdf.format(new Date());
            workDir = curDir + fileSep + str;
            success = FileUtils.createDirectory(workDir);
        }
        FileUtils.addToRecentFiles(workDir, FileFormat.FRG_RUN);
        logFile = workDir + ".log";
    }

//------------------------------------------------------------------------------

    /**
     * Returns the list of parameters in a string with newline characters as
     * delimiters.
     * @return the list of parameters in a string with newline characters as
     * delimiters.
     */
    public String getPrintedList()
    {
        StringBuilder sb = new StringBuilder(1024);
        sb.append(" " + paramTypeName() + " ").append(NL);
        for (Field f : this.getClass().getDeclaredFields()) 
        {
            try
            {
                sb.append(f.getName()).append(" = ").append(
                            f.get(this)).append(NL);
            }
            catch (Throwable t)
            {
                sb.append("ERROR! Unable to print " + paramTypeName() 
                        + " parameters. Cause: " + t);
                break;
            }
        }
        for (RunTimeParameters otherCollector : otherParameters.values())
        {
            sb.append(otherCollector.getPrintedList());
        }
        return sb.toString();
    }

//----------------------------------------------------------------------------

}
