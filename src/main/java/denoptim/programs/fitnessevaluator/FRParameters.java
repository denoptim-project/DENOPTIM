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

package denoptim.programs.fitnessevaluator;

import java.io.File;
import java.lang.reflect.Field;

import denoptim.exception.DENOPTIMException;
import denoptim.files.FileFormat;
import denoptim.files.FileUtils;
import denoptim.programs.RunTimeParameters;


/**
 * Parameters controlling execution of FitnessRunner.
 * 
 * @author Marco Foscato
 */

public class FRParameters extends RunTimeParameters
{
    /**
     * File with input for fitness provider
     */
    private File inpFile = null;
    
    /**
     * File where the results of the fitness evaluation will be printed
     */
    private File outFile = new File("output.sdf");
    
    /**
     * Flag controlling attempt to add templates to building block libraries
     */
    protected boolean addTemplatesToLibraries = false;
    
//-----------------------------------------------------------------------------
    
    /**
     * Constructor
     * @param paramType
     */
    public FRParameters()
    {
        super(ParametersType.FR_PARAMS);
    }
   
//-----------------------------------------------------------------------------

    public File getInputFile()
    {
        return inpFile;
    }
    
//-----------------------------------------------------------------------------

    public File getOutputFile()
    {
        return outFile;
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
		case "INPUT=":
		    inpFile = new File(value);
		    break;
        case "OUTPUT=":
            outFile = new File(value);
            break;
        case "EXTRACTTEMPLATES":
            addTemplatesToLibraries = true;
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
             msg = "Keyword " + key + " is not a known " 
            		 + "related keyword for FitnessRunner. Check input files.";
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
        String msg = "";
        if (!workDir.equals(".") && !FileUtils.checkExists(workDir))
    	{
    	   msg = "Directory '" + workDir + "' not found. Please specify an "
    		 + "existing directory.";
    	   throw new DENOPTIMException(msg);
    	}

    	if (inpFile != null && !inpFile.exists())
    	{
    	    msg = "File with input data not found. Check " + inpFile;
                throw new DENOPTIMException(msg);
    	}
        
        if (outFile != null && outFile.exists())
        {
            msg = "File meant for output ("
                    + outFile.getAbsolutePath()
                    + ")already exists and we do not overwrite.";
            throw new DENOPTIMException(msg);
        }
        checkOtherParameters();
    }

//----------------------------------------------------------------------------

    /**
     * Processes all parameters and initialize related objects.
     * @throws DENOPTIMException
     */

    public void processParameters() throws DENOPTIMException
    {
        FileUtils.addToRecentFiles(outFile, FileFormat.GRAPHSDF);
        processOtherParameters();
        
        System.err.println("Output files associated with the current run are " 
        + "located in " + workDir);
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
