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

package denoptim.programs.moldecularmodelbuilder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

import denoptim.exception.DENOPTIMException;
import denoptim.files.FileUtils;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.graph.rings.RingClosureParameters;
import denoptim.integration.tinker.TinkerUtils;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecularmodeling.MMBuilderUtils;
import denoptim.programs.RunTimeParameters;

/**
 * Parameters for the conformer generator (3D builder).
 *
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class MMBuilderParameters extends RunTimeParameters
{
    /**
     * Flag controlling debug verbosity
     */
    protected boolean debug = false;

    /**
     * Pathname to Tinker's pssrot executable
     */
    protected String toolPSSROT;

    /**
     * Pathname to Tinker's xyzint executable
     */
    protected String toolXYZINT;

    /**
     * Pathname to Tinker's intxyz executable
     */
    protected String toolINTXYZ;

    /**
     * Flag controlling removal of dummy atoms from output geometry
     */
    protected boolean keepDummy = false;

    /**
     * Pathname to force field parameters file for Tinker
     */
    protected String forceFieldFile;

    /**
     * Pathname to parameters file for PSS part of Tinker's PSSROT
     */
    protected String pssrotFile;

    /**
     * Pathname to parameters file for PSS part of Tinker's ring-closing PSSROT
     */
    protected String rsPssrotFile;

    /**
     * Pathname to keywords file for Tinker's conformational search
     */
    protected String keyFile;

    /**
     * Pathname to keywords file for Tinker's ring-closing conformational search
     */
    protected String rsKeyFile;

    /**
     * Parameters for PSS part of Tinker's PSSROT step
     */
    protected ArrayList<String> pssrotParams_Init;

    /**
     * Parameters for linear search part of Tinker's PSSROT step
     */
    protected ArrayList<String> pssrotParams_Rest;

    /**
     * Keywords for Tinker's conformational search
     */
    protected ArrayList<String> keyFileParams;

    /**
     * Parameters for PSS part of Tinker's ring-closing PSSROT step
     */
    protected ArrayList<String> rsPssrotParams_Init;

    /**
     * Parameters for linear search part of Tinker's ring-closing PSSROT step
     */
    protected ArrayList<String> rsPssrotParams_Rest;

    /**
     * Keywords for Tinker's ring-closing conformational search
     */
    protected ArrayList<String> rsKeyFileParams;

    /**
     * Flag controlling the criterion used to reorder atom lists.
     * 1: branch-oriented (completes a branch before moving to the next one).
     * 2: layer-oriented) completes a layer before moving to the next one).
     */
    protected int atomOrderingScheme = 1;

    /**
     * Atom type map
     */    
    protected HashMap<String, Integer> TINKER_MAP;

    /**
     * Unique task identifier
     */
    protected int taskID;

    /**
     * Pathname of input SDF file
     */
    protected String inpSDFFile;

    /**
     * Pathname of ouput SDF file
     */
    protected String outSDFFile;
    

//------------------------------------------------------------------------------
    
    /**
     * Constructor
     */
    public MMBuilderParameters()
    {
        super(ParametersType.MMB_PARAM);
    }

//------------------------------------------------------------------------------

    public int getTaskID()
    {
        return taskID;
    }

//------------------------------------------------------------------------------

    public String getInputSDFFile()
    {
        return inpSDFFile;
    }

//------------------------------------------------------------------------------

    public String getOutputSDFFile()
    {
        return outSDFFile;
    }

//------------------------------------------------------------------------------

    public String getPSSROTTool()
    {
        return toolPSSROT;
    }

//------------------------------------------------------------------------------

    public String getXYZINTTool()
    {
        return toolXYZINT;
    }

//------------------------------------------------------------------------------

    public String getINTXYZTool()
    {
        return toolINTXYZ;
    }

//------------------------------------------------------------------------------

    public String getParamFile()
    {
        return forceFieldFile;
    }

//------------------------------------------------------------------------------

    public ArrayList<String> getInitPSSROTParams()
    {
        return pssrotParams_Init;
    }

//------------------------------------------------------------------------------

    public ArrayList<String> getRSInitPSSROTParams()
    {
        return rsPssrotParams_Init;
    }

//------------------------------------------------------------------------------

    public ArrayList<String> getRestPSSROTParams()
    {
        return pssrotParams_Rest;
    }

//------------------------------------------------------------------------------

    public ArrayList<String> getRSRestPSSROTParams()
    {
        return rsPssrotParams_Rest;
    }

//------------------------------------------------------------------------------

    public ArrayList<String> getKeyFileParams()
    {
        return keyFileParams;
    }

//------------------------------------------------------------------------------

    public boolean getKeepDummyFlag()
    {
        return keepDummy;
    }

//------------------------------------------------------------------------------

    public ArrayList<String> getRSKeyFileParams()
    {
        return rsKeyFileParams;
    }

//------------------------------------------------------------------------------

    public int getVerbosity()
    {
        return verbosity;
    }

//------------------------------------------------------------------------------

    public boolean debug()
    {
        return debug;
    }

//------------------------------------------------------------------------------

    public int getAtomOrderingScheme()
    {
        return atomOrderingScheme;
    }

//------------------------------------------------------------------------------

    public HashMap<String, Integer> getTinkerMap()
    {
        return TINKER_MAP;
    }

//------------------------------------------------------------------------------

    public String getWorkingDirectory()
    {
        return workDir;
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
        case "TOOLPSSROT=":            
            toolPSSROT = value;
            break;
        case "TOOLXYZINT=":
            toolXYZINT = value;
            break;
        case "TOOLINTXYZ=":
            toolINTXYZ = value;
            break;
        case "FORCEFIELDFILE=":
            forceFieldFile = value;
            break;
        case "KEYFILE=":
            keyFile = value;
            break;
        case "RCKEYFILE=":
            rsKeyFile = value;
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
        case "DEBUG=":
            debug = true;
            break;
        case "PSSROTPARAMS=":
            pssrotFile = value;
            break;
        case "RCPSSROTPARAMS=":
            rsPssrotFile = value;
            break;
        case "KEEPDUMMYATOMS=":
            keepDummy = true;
            break;
        case "ATOMORDERINGSCHEME=":
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
        case "INPSDF=":
            inpSDFFile = value;
            break;
        case "OUTSDF=":
            outSDFFile = value;
            break;
        case "WORKDIR=":
            workDir = value;
            break;
/*
        case "=":
            = value;
            break;
*/

        default:
             msg = "Keyword " + key + " is not a known keyword for the "
                     + "3d-molecular model builder. Check input files.";
            throw new DENOPTIMException(msg);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Check all parameters.
     */ 

    public void checkParameters() throws DENOPTIMException
    {
		checkNotNull("wrkDir",workDir,"WORKDIR");
		checkFileExists(workDir);

        checkNotNull("inpSDFFile",inpSDFFile,"INPSDF");
        checkFileExists(inpSDFFile);

        checkNotNull("outSDFFile",outSDFFile,"OUTSDF");

        checkNotNull("toolPSSROT",toolPSSROT,"TOOLPSSROT");
        checkFileExists(toolPSSROT);

        checkNotNull("toolXYZINT",toolXYZINT,"TOOLXYZINT");
        checkFileExists(toolXYZINT);

        checkNotNull("toolINTXYZ",toolINTXYZ,"TOOLINTXYZ");
        checkFileExists(toolINTXYZ);

        checkNotNull("forceFieldFile",forceFieldFile,"FORCEFIELDFILE");
        checkFileExists(forceFieldFile);

        checkNotNull("keyFile",keyFile,"KEYFILE");
        checkFileExists(keyFile);

        checkNotNull("pssrotFile",pssrotFile,"PSSROTPARAMS");
        checkFileExists(pssrotFile);


        if (atomOrderingScheme < 1 || atomOrderingScheme > 2)
        {
            System.out.println("ERROR! Parameter 'atomOrderingScheme' can only "
				+ "be 1 or 2");
            System.exit(-1);
        }

        checkOtherParameters();
        /*
//TODO-gg 
        if (RingClosureParameters.rcParamsInUse())
        {
            RingClosureParameters.checkParameters();
            if (RingClosureParameters.allowRingClosures())
            {
        	checkNotNull("rsPssrotFile",rsPssrotFile,"RCPSSROTPARAMS");
        	checkFileExists(rsPssrotFile);

        	checkNotNull("rsKeyFile",rsKeyFile,"RCKEYFILE");
        	checkFileExists(rsKeyFile);
            }
        }
        */
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
    
//------------------------------------------------------------------------------

    public void processParameters() throws DENOPTIMException
    {
        pssrotParams_Init = new ArrayList<>();
        pssrotParams_Rest = new ArrayList<>();
        keyFileParams = new ArrayList<>();
        rsPssrotParams_Init = new ArrayList<>();
        rsPssrotParams_Rest = new ArrayList<>();
        rsKeyFileParams = new ArrayList<>();
        
        MMBuilderUtils.readKeyFileParams(keyFile, keyFileParams);
        TinkerUtils.readPSSROTParams(pssrotFile, pssrotParams_Init, 
                                                             pssrotParams_Rest);
        TINKER_MAP = TinkerUtils.readTinkerAtomTypes(forceFieldFile);

        if (otherParameters.containsKey(ParametersType.RC_PARAMS))
        {
            TinkerUtils.readPSSROTParams(rsPssrotFile, rsPssrotParams_Init, 
                                                           rsPssrotParams_Rest);
            MMBuilderUtils.readKeyFileParams(rsKeyFile, rsKeyFileParams);
        }
        processOtherParameters();
    }

//------------------------------------------------------------------------------
}
