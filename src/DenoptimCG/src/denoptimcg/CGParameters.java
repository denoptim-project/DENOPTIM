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
 * Parameters for the conformer generator (3D builder).
 *
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class CGParameters
{
    /**
     * Flag indicating that at least one parameter has been defined
     */
    private static boolean cgParamsInUse = false;

    /**
     * Verbosity level
     */
    protected static int verbosity = 0;

    /**
     * Flag controlling debug verbosity
     */
    protected static boolean debug = false;

    /**
     * Pathname to Tinker's pssrot executable
     */
    protected static String toolPSSROT;

    /**
     * Pathname to Tinker's xyzint executable
     */
    protected static String toolXYZINT;

    /**
     * Pathname to Tinker's intxyz executable
     */
    protected static String toolINTXYZ;

    /**
     * Pathname to OpenBabel executable
     */
    protected static String toolOpenBabel = "";

    /**
     * Flag controlling removal of dummy atoms from output geometry
     */
    protected static boolean keepDummy = false;

    /**
     * Pathname to force field parameters file for Tinker
     */
    protected static String paramFile;

    /**
     * Pathname to parameters file for PSS part of Tinker's PSSROT
     */
    protected static String pssrotFile;

    /**
     * Pathname to parameters file for PSS part of Tinker's ring-closing PSSROT
     */
    protected static String rsPssrotFile;

    /**
     * Pathname to keywords file for Tinker's conformational search
     */
    protected static String keyFile;

    /**
     * Pathname to keywords file for Tinker's ring-closing conformational search
     */
    protected static String rsKeyFile;

    /**
     * Parameters for PSS part of Tinker's PSSROT step
     */
    protected static ArrayList<String> pssrotParams_Init;

    /**
     * Parameters for linear search part of Tinker's PSSROT step
     */
    protected static ArrayList<String> pssrotParams_Rest;

    /**
     * Keywords for Tinker's conformational search
     */
    protected static ArrayList<String> keyFileParams;

    /**
     * Parameters for PSS part of Tinker's ring-closing PSSROT step
     */
    protected static ArrayList<String> rsPssrotParams_Init;

    /**
     * Parameters for linear search part of Tinker's ring-closing PSSROT step
     */
    protected static ArrayList<String> rsPssrotParams_Rest;

    /**
     * Keywords for Tinker's ring-closing conformational search
     */
    protected static ArrayList<String> rsKeyFileParams;

    /**
     * Flag controlling the critetion used to reorder atom lists.
     * 1: branch-oriented (completes a branch before moving to the next one).
     * 2: layer-oriented )completes a layer bevore moving to the next one).
     */
    protected static int atomOrderingScheme = 1;

    /**
     * Atom type map
     */    
    protected static HashMap<String, Integer> TINKER_MAP;

    /**
     * Log file //TODO: keep or get rid of it?
     */
    protected static String logFile = "";

    /**
     * Unique task identifier
     */
    protected static int taskID;

    /**
     * Pathname of input SDF file
     */
    protected static String inpSDFFile;

    /**
     * Pathname of ouput SDF file
     */
    protected static String outSDFFile;

    /**
     * Pathname to current working directory
     */
    protected static String wrkDir;
    

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
    protected static void readParameterFile(String infile) 
                                                        throws DENOPTIMException
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

                if (line.toUpperCase().startsWith("CG-"))
                {
                    CGParameters.interpretKeyword(line);
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
        cgParamsInUse = true;
        String msg = "";
        switch (key.toUpperCase())
        {
        case "CG-TOOLOPENBABEL=":
            toolOpenBabel = value;
            break;
        case "CG-PSSROT=":            
            toolPSSROT = value;
            break;
        case "CG-XYZINT=":
            toolXYZINT = value;
            break;
        case "CG-INTXYZ=":
            toolINTXYZ = value;
            break;
        case "CG-PARAM=":
            paramFile = value;
            break;
        case "CG-KEYFILE=":
            keyFile = value;
            break;
        case "CG-RCKEYFILE=":
            rsKeyFile = value;
            break;
        case "CG-VERBOSITY=":
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
        case "CG-DEBUG=":
            debug = true;
            break;
        case "CG-PSSROTPARAMS=":
            pssrotFile = value;
            break;
        case "CG-RSPSSROTPARAMS=":
            rsPssrotFile = value;
            break;
        case "CG-KEEPDUMMYATOMS=":
            keepDummy = true;
            break;
        case "CG-ATOMORDERINGSCHEME=":
            try
            {
                atomOrderingScheme = Integer.parseInt(value);
            }
            catch (Throwable t)
            {
                msg = "Unable to understand value " + key + "'" + value + "'";
                throw new DENOPTIMException(msg);
            }
            break;
        case "CG-INPSDF=":
            inpSDFFile = value;
            break;
        case "CG-OUTSDF=":
            outSDFFile = value;
            break;
        case "CG-WRKDIR=":
            wrkDir = value;
            break;
/*
        case "CG-=":
            = value;
            break;
*/

        default:
             msg = "Keyword " + key + " is not a known DenoptimCG-"
                                       + "related keyword. Check input files.";
            throw new DENOPTIMException(msg);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Check all parameters.
     */ 

    public static void checkParameters() throws DENOPTIMException
    {
        String msg = "ERROR: unacceptable value for parameter in DenoptimCG. ";
        if (inpSDFFile == null)
        {
           msg = msg + "inpSDFFile is '" + inpSDFFile + "'";
           throw new DENOPTIMException(msg);
        }
        if (outSDFFile == null)
        {
           msg = msg + "outSDFFile is '" + outSDFFile + "'";
           throw new DENOPTIMException(msg);
        }
        if (toolPSSROT == null)
        {
           msg = msg + "toolPSSROT is '" + toolPSSROT + "'";
           throw new DENOPTIMException(msg);
        }
        if (toolXYZINT == null)
        {
           msg = msg + "toolXYZINT is '" + toolXYZINT + "'";
           throw new DENOPTIMException(msg);
        }
        if (toolINTXYZ == null)
        {
           msg = msg + "toolINTXYZ is '" + toolINTXYZ + "'";
           throw new DENOPTIMException(msg);
        }
        if (keyFile == null)
        {
           msg = msg + "keyFile is '" + keyFile + "'";
           throw new DENOPTIMException(msg);
        }
        if (paramFile == null || paramFile.length() == 0)
        {
           msg = msg + "paramFile is '" + paramFile + "'";
           throw new DENOPTIMException(msg);
        }
        if (pssrotFile == null)
        {
           msg = msg + "pssrotFile is '" + pssrotFile + "'";
           throw new DENOPTIMException(msg);
        }
        if (atomOrderingScheme < 1 || atomOrderingScheme > 2)
        {
           msg = msg + "atomOrderingScheme is '" + atomOrderingScheme + "'";
           throw new DENOPTIMException(msg);
        }

        if (FragmentSpaceParameters.fsParamsInUse())
        {
            FragmentSpaceParameters.checkParameters();
        }

        if (RingClosureParameters.rcParamsInUse())
        {
            RingClosureParameters.checkParameters();
            if (RingClosureParameters.allowRingClosures())
            {
                if (rsPssrotFile == null)
                {
                   msg = msg + "rsPssrotFile is '" + rsPssrotFile + "'";
                   throw new DENOPTIMException(msg);
                }
                if (rsKeyFile == null)
                {
                   msg = msg + "rsKeyFile is '" + rsKeyFile + "'";
                   throw new DENOPTIMException(msg);
                }
            }
        }
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
        
        CGUtils.readKeyFileParams(keyFile, keyFileParams);
        TinkerUtils.readPSSROTParams(pssrotFile, pssrotParams_Init, 
                                                             pssrotParams_Rest);
        TINKER_MAP = TinkerUtils.readTinkerAtomTypes(paramFile);

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
