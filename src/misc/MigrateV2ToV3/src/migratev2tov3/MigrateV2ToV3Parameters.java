package migratev2tov3;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import denoptim.exception.DENOPTIMException;
import denoptim.files.FileUtils;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.graph.DENOPTIMGraph.StringFormat;
import denoptim.rings.RingClosureParameters;

/**
 * Parameters controlling execution of StringConverter.
 * 
 * @author Marco Foscato
 */

public class MigrateV2ToV3Parameters
{
    /**
     * Flag indicating that at least one parameter has been defined
     */
    protected static boolean paramsInUse = false;

    /**
     * Working directory
     */
    protected static String workDir = ".";
    
    /**
     * Input File
     */
    protected static String inpFile;

    /**
     * Output file 
     */
    protected static String outFile;
    
    /**
     * Verbosity level
     */
    protected static int verbosity = 1;


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

                if (line.toUpperCase().startsWith("MIGRATEV2TOV3-"))
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
        paramsInUse = true;
        String msg = "";
        switch (key.toUpperCase())
        {
            case "MIGRATEV2TOV3-VERBOSITY=":
                verbosity = Integer.parseInt(value);
                break;
            case "MIGRATEV2TOV3-INPUTFILE=":
                inpFile = value;
                break;
            case "MIGRATEV2TOV3-OUTPUTFILE=":
                outFile = value;
                break;
            default:
                 msg = "Keyword " + key + " is not a known StringConverter-"
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
        if (!paramsInUse)
        {
            return;
        }

        if (!workDir.equals(".") && !FileUtils.checkExists(workDir))
        {
           msg = "Directory " + workDir + " not found. Please specify an "
             + "existing directory.";
           throw new DENOPTIMException(msg);
        }

        if (!FileUtils.checkExists(inpFile))
        {
            msg = "Input file '" + inpFile + "' not found.";
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
