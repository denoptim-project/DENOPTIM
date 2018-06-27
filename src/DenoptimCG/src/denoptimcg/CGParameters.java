package denoptimcg;

import java.lang.reflect.Field;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import exception.DENOPTIMException;
import io.DenoptimIO;
import utils.GenUtils;
import tinker.TinkerUtils;
import rings.RingClosureParameters;
import fragspace.FragmentSpaceParameters;
import logging.DENOPTIMLogger;

import org.openscience.cdk.interfaces.IAtomContainer;


/**
 *
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class CGParameters
{
    protected static int verbosity = 0;
    protected static boolean debug = false;
    protected static String toolPSSROT;
    protected static String toolXYZINT;
    protected static String toolINTXYZ;
    protected static String paramFile;
    protected static String pssrotFile;
    protected static String rsPssrotFile;
    protected static String keyFile;
    protected static String rsKeyFile;
    protected static boolean keepDummy = false;
    protected static ArrayList<String> pssrotParams_Init;
    protected static ArrayList<String> pssrotParams_Rest;
    protected static ArrayList<String> keyFileParams;
    protected static ArrayList<String> rsPssrotParams_Init;
    protected static ArrayList<String> rsPssrotParams_Rest;
    protected static ArrayList<String> rsKeyFileParams;
    protected static int atomOrderingScheme = 1;
    // store atom types related to tinker
    // these may be outside of what Tinker provides
    protected static HashMap<String, Integer> TINKER_MAP;

    // Log file to which all messages are written to
    protected static String logFile = "";

    // read parameters from console
    protected static int taskID;
    protected static String inpSDFFile;
    protected static String outSDFFile;
    protected static String wrkDir;
    
    // open babel conversion software
    protected static String toolOpenBabel = "";

//------------------------------------------------------------------------------

    public static int getTaskID()
    {
        return taskID;
    }

//------------------------------------------------------------------------------

    public static String getInputSDFFile()
    {
        return inpSDFFile;
    }

//------------------------------------------------------------------------------

    public static String getOutputSDFFile()
    {
        return outSDFFile;
    }

//------------------------------------------------------------------------------

    public static String getOpenBabelTool()
    {
        return toolOpenBabel;
    }

//------------------------------------------------------------------------------

    public static String getPSSROTTool()
    {
        return toolPSSROT;
    }

//------------------------------------------------------------------------------

    public static String getXYZINTTool()
    {
        return toolXYZINT;
    }

//------------------------------------------------------------------------------

    public static String getINTXYZTool()
    {
        return toolINTXYZ;
    }

//------------------------------------------------------------------------------

    public static String getParamFile()
    {
        return paramFile;
    }

//------------------------------------------------------------------------------

    public static ArrayList<String> getInitPSSROTParams()
    {
        return pssrotParams_Init;
    }

//------------------------------------------------------------------------------

    public static ArrayList<String> getRSInitPSSROTParams()
    {
        return rsPssrotParams_Init;
    }

//------------------------------------------------------------------------------

    public static ArrayList<String> getRestPSSROTParams()
    {
        return pssrotParams_Rest;
    }

//------------------------------------------------------------------------------

    public static ArrayList<String> getRSRestPSSROTParams()
    {
        return rsPssrotParams_Rest;
    }

//------------------------------------------------------------------------------

    public static ArrayList<String> getKeyFileParams()
    {
        return keyFileParams;
    }

//------------------------------------------------------------------------------

    public static boolean getKeepDummyFlag()
    {
        return keepDummy;
    }

//------------------------------------------------------------------------------

    public static ArrayList<String> getRSKeyFileParams()
    {
        return rsKeyFileParams;
    }

//------------------------------------------------------------------------------

    public static int getVerbosity()
    {
	return verbosity;
    }

//------------------------------------------------------------------------------

    public static boolean debug()
    {
	return debug;
    }

//------------------------------------------------------------------------------

    public static int getAtomOrderingScheme()
    {
        return atomOrderingScheme;
    }

//------------------------------------------------------------------------------

    public static HashMap<String, Integer> getTinkerMap()
    {
        return TINKER_MAP;
    }

//------------------------------------------------------------------------------

    public static String getWorkingDirectory()
    {
        return wrkDir;
    }

//------------------------------------------------------------------------------

    /**
     * Read the parameter file
     * @param fname
     * @throws DENOPTIMException
     */
    protected static void readParameterFile(String infile) throws DENOPTIMException
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

                if (line.toUpperCase().startsWith("TOOLOPENBABEL="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        toolOpenBabel = option;
                    }
                    continue;
                }

                if (line.toUpperCase().startsWith("PSSROT="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    toolPSSROT = option;
                    continue;
                }

                if (line.toUpperCase().startsWith("XYZINT"))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    toolXYZINT = option;
                    continue;
                }

                if (line.toUpperCase().startsWith("INTXYZ"))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    toolINTXYZ = option;
                    continue;
                }

                if (line.toUpperCase().startsWith("PARAM"))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    paramFile = option;

                    if (paramFile.length() > 0)
                    {
                        TINKER_MAP = TinkerUtils.readTinkerAtomTypes(paramFile);
                    }
                    continue;
                }

                if (line.toUpperCase().startsWith("KEYFILE"))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    keyFile = option;
                    continue;
                }

                if (line.toUpperCase().startsWith("RCKEYFILE"))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    rsKeyFile = option;
                    continue;
                }

                if (line.toUpperCase().startsWith("VERBOSITY"))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    verbosity = Integer.parseInt(option);
                    continue;
                }

                if (line.toUpperCase().startsWith("DEBUG"))
                {
                    debug = true;
                    continue;
                }

                if (line.toUpperCase().startsWith("PSSROTPARAMS"))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    pssrotFile = option;
                    continue;
                }

                if (line.toUpperCase().startsWith("RSPSSROTPARAMS"))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
		    rsPssrotFile = option;
                    continue;
                }

                if (line.toUpperCase().startsWith("KEEPDUMMYATOMS"))
                {
                    keepDummy = true;
                    continue;
                }

                if (line.toUpperCase().startsWith("ATOMORDERINGSCHEME"))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        atomOrderingScheme = Integer.parseInt(option);
                    }
                    continue;
                }

                if (line.toUpperCase().startsWith("INPSDF"))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        inpSDFFile = option;
                    }
                }
                
                if (line.toUpperCase().startsWith("OUTSDF"))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        outSDFFile = option;
                    }
                }
                
                if (line.toUpperCase().startsWith("WRKDIR="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                        wrkDir = option;
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
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }
    }

//------------------------------------------------------------------------------

    public static boolean checkParameters() throws DENOPTIMException
    {
        if (!(toolPSSROT != null && toolXYZINT != null && toolINTXYZ != null &&
                keyFile != null && paramFile != null && pssrotFile != null &&
                rsKeyFile != null && rsPssrotFile != null &&
                atomOrderingScheme > 0 && atomOrderingScheme <= 2 &&
                inpSDFFile != null && outSDFFile != null))
        {
            return false;
        }

        if (FragmentSpaceParameters.fsParamsInUse())
        {
            FragmentSpaceParameters.checkParameters();
        }

        if (RingClosureParameters.rcParamsInUse())
        {
            RingClosureParameters.checkParameters();
        }

        return true;
    }
    
//------------------------------------------------------------------------------

    protected static void processParameters() throws DENOPTIMException
    {
        pssrotParams_Init = new ArrayList<>();
        pssrotParams_Rest = new ArrayList<>();
        keyFileParams = new ArrayList<>();
        rsPssrotParams_Init = new ArrayList<>();
        rsPssrotParams_Rest = new ArrayList<>();
	rsKeyFileParams = new ArrayList<>();
        
        // pssrot params
        CGUtils.readKeyFileParams(keyFile, keyFileParams);
        TinkerUtils.readPSSROTParams(pssrotFile, pssrotParams_Init, 
							     pssrotParams_Rest);
        if (FragmentSpaceParameters.fsParamsInUse())
        {
            FragmentSpaceParameters.processParameters();
        }

        if (RingClosureParameters.allowRingClosures())
        {
            TinkerUtils.readPSSROTParams(rsPssrotFile, rsPssrotParams_Init, 
					                   rsPssrotParams_Rest);
            CGUtils.readKeyFileParams(rsKeyFile, rsKeyFileParams);
            RingClosureParameters.processParameters();
        }
    }

//----------------------------------------------------------------------------

    public static void printParameters()
    {
        String eol = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder(1024);
        sb.append(" CGParameters ").append(eol);
        for (Field f : CGParameters.class.getDeclaredFields())
        {
            try
            {
                sb.append(f.getName()).append(" = ").append(
                                       f.get(CGParameters.class)).append(eol);
            }
            catch (Throwable t)
            {
                sb.append("ERROR! Unable to print CGParameters.");
                break;
            }
        }
        DENOPTIMLogger.appLogger.info(sb.toString());
        sb.setLength(0);

        FragmentSpaceParameters.printParameters();
        RingClosureParameters.printParameters();
    }

//------------------------------------------------------------------------------
}
