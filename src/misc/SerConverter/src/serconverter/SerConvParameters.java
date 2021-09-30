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

package serconverter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.io.DenoptimIO;
import denoptim.rings.RingClosureParameters;


/**
 * Parameters controlling execution of SerConverter.
 * 
 * @author Marco Foscato
 */

public class SerConvParameters
{
    /**
     * Flag indicating that at least one parameter has been defined
     */
    protected static boolean fseParamsInUse = false;

    /**
     * Working directory
     */
    protected static String workDir = ".";

    /**
     * Input File (binary)
     */
    protected static String inpFile;

    /**
     * Output file format
     */
    protected static String outFormat = "TXT";
    
    /**
     * Flag requiring alignement of 3D fragments
     */
    protected static boolean make3DTree = false;

    /**
     * Output file
     */
    protected static String outFile;


//-----------------------------------------------------------------------------

    /**
     * Read the parameter TXT file line by line and interpret its content.
     * @param infile
     * @throws DENOPTIMException
     */

    public static void readParameterFile(String infile) throws DENOPTIMException
    {
        String option, line;
        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(infile));
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

                if (line.toUpperCase().startsWith("SERCONV-"))
                {
                    interpretKeyword(line);
                    continue;
                }

                if (line.toUpperCase().startsWith("FS-"))
                {
                    FragmentSpaceParameters.interpretKeyword(line);
                    continue;
                }

                if (line.toUpperCase().startsWith("RC-"))
                {
                    RingClosureParameters.interpretKeyword(line);
                    continue;
                }
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
                    br = null;
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }

        option = null;
        line = null;
    }

//-----------------------------------------------------------------------------

    /**
     * Processes a string looking for keyword and a possibly associated value.
     * @param line the string to parse
     * @throws DENOPTIMException
     */

    public static void interpretKeyword(String line) throws DENOPTIMException
    {
        String key = line.trim();
        String value = "";
        if (line.contains("="))
        {
            key = line.substring(0,line.indexOf("=") + 1).trim();
            value = line.substring(line.indexOf("=") + 1).trim();
        }
        try
        {
            interpretKeyword(key,value);
        }
        catch (DENOPTIMException e)
        {
            throw new DENOPTIMException(e.getMessage()+" Check line "+line);
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Processes a keyword/value pair and assign the related parameters.
     * @param key the keyword as string
     * @param value the value as a string
     * @throws DENOPTIMException
     */

    public static void interpretKeyword(String key, String value)
                                                      throws DENOPTIMException
    {
        fseParamsInUse = true;
        String msg = "";
        switch (key.toUpperCase())
        {
        case "SERCONV-WORKDIR=":
            workDir = value;
            break;
		case "SERCONV-INPFILE=":
		    inpFile = value;
		    break;
        case "SERCONV-OUTFORMAT=":
            outFormat = value.toUpperCase();
            break;
        case "SERCONV-OUTFILE=":
            outFile = value;
            break;
        case "SERCONV-MAKE3DTREE":
            make3DTree = true;
            break;
        default:
             msg = "Keyword " + key + " is not a known SerConverter-"
                                       + "related keyword. Check input files.";
            throw new DENOPTIMException(msg);
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Evaluate consistency of input parameters.
     * @throws DENOPTIMException
     */

    public static void checkParameters() throws DENOPTIMException
    {
        String msg = "";
        if (!fseParamsInUse)
        {
            return;
        }

	if (!workDir.equals(".") && !DenoptimIO.checkExists(workDir))
	{
	   msg = "Directory " + workDir + " not found. Please specify an "
		 + "existing directory.";
	   throw new DENOPTIMException(msg);
	}

        if (!DenoptimIO.checkExists(inpFile))
        {
            msg = "Input file '" + inpFile + "' not found.";
            throw new DENOPTIMException(msg);
        }

        if (DenoptimIO.checkExists(outFile))
        {
            msg = "Output file '" + outFile + "' already exists.";
            throw new DENOPTIMException(msg);
        }
	
        ArrayList outFormats = new ArrayList<String>();
        outFormats.add("TXT");
        outFormats.add("SDF");
        if (!outFormats.contains(outFormat))
        {
            msg = "Output format '" + outFormat + "' not known. "
                  + "Known formats are: " + outFormats;
            throw new DENOPTIMException(msg);
        }

        if (FragmentSpaceParameters.fsParamsInUse())
        {
            FragmentSpaceParameters.checkParameters();
        }

        if (RingClosureParameters.rcParamsInUse())
        {
            RingClosureParameters.checkParameters();
        }
    }

//----------------------------------------------------------------------------

    /**
     * Processes all parameters and initialize related objects.
     * @throws DENOPTIMException
     */

    public static void processParameters() throws DENOPTIMException
    {
        if (FragmentSpaceParameters.fsParamsInUse())
        {
            FragmentSpaceParameters.processParameters();
        }

        if (RingClosureParameters.allowRingClosures())
        {
            RingClosureParameters.processParameters();
        }
    }

//----------------------------------------------------------------------------

}
