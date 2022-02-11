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

package testoperator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import denoptim.exception.DENOPTIMException;
import denoptim.files.FileUtils;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.rings.RingClosureParameters;
import denoptim.utils.MutationType;
import denoptim.utils.RandomUtils;
import denoptimga.GAParameters;


/**
 * Parameters controlling execution of TestOperator.
 * 
 * @author Marco Foscato
 */

public class TestOperatorParameters
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
     * Seed for generation of pseudo-random numbers
     */
    protected static long randomSeed = 1234567890L;
    
    /**
     * Testable Operators
     */
    protected enum Operator {MUTATION,XOVER}
    
    /**
     * Chosen operator
     */
    protected static Operator operatorToTest = Operator.XOVER;

    /**
     * Target vertex ID for mutation
     */
    protected static int[] mutationTarget;
    
    /**
     * Target attachment point ID for mutation (AP belonging already to the 
     * graph). Zero-based index.
     */
    protected static int idTargetAP = -1;
    
    /**
     * Type of mutation to perform
     */
    protected static MutationType mutationType;
    
    /**
     * The given vertex index.  Zero-based index.
     * Used whenever a vertex id has to be given. For
     * example, when specifying how to mutate a graph. 
     */
    protected static int idNewVrt = -1;
    
    /**
     * The given attachment point index. 
     * Used whenever an AP id has to be given. 
     * For example, when specifying how to mutate a graph. Zero-based index.
     */
    protected static int idNewAP = -1;
    
    /**
     * Input File male
     */
    protected static String inpFileM;

    /**
     * Input File female
     */
    protected static String inpFileF;

    /**
     * Male VertedID (not index) on which perform xover
     */
    protected static int mvid;

    /**
     * Male AP index on which perform xover
     */
    protected static int mapid;

    /**
     * Female VertexID (not index) on which perform xover
     */
    protected static int fvid;

    /**
     * Female AP index on which perform xover
     */
    protected static int fapid;

    /**
     * Output file male
     */
    protected static String outFileM;

    /**
     * Output file female
     */
    protected static String outFileF;


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

                if (line.toUpperCase().startsWith("TESTGENOPS-"))
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
                
                if (line.toUpperCase().startsWith("GA-"))
                {
                    GAParameters.interpretKeyword(line);
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
            case "TESTGENOPS-OP=":
                operatorToTest = Operator.valueOf(value);
                break;
            case "TESTGENOPS-INPFILE=":
                inpFileM = value;
                break;
            case "TESTGENOPS-OUTFILE=":
                outFileM = value;
                break;
            case "TESTGENOPS-MUTATIONTARGET=":
                String[] parts = value.split(",");
                mutationTarget = new int[parts.length];
                for (int i=0; i<parts.length; i++)
                {
                    mutationTarget[i] = Integer.parseInt(parts[i].trim());
                }
                break;
            case "TESTGENOPS-APIDONTARGETVERTEX=":
                idTargetAP = Integer.parseInt(value);
                break;
            case "TESTGENOPS-MUTATIONTYPE=":
                mutationType = MutationType.valueOf(value);
                break;
            case "TESTGENOPS-NEWVERTEXMOLID=":
                idNewVrt = Integer.parseInt(value);
                break;
            case "TESTGENOPS-NEWAPID=":
                idNewAP = Integer.parseInt(value);
                break;
            case "TESTGENOPS-WORKDIR=":
                workDir = value;
                break;
            case "TESTGENOPS-INPFILEMALE=":
                inpFileM = value;
                break;
            case "TESTGENOPS-INPFILEFEMALE=":
    	        inpFileF = value;
                break;
            case "TESTGENOPS-VERTEXMALE=":
                mvid = Integer.parseInt(value);
                break;
            case "TESTGENOPS-APMALE=":
                mapid = Integer.parseInt(value);
                break;
            case "TESTGENOPS-VERTEXFEMALE=":
                fvid = Integer.parseInt(value);
                break;
            case "TESTGENOPS-APFEMALE=":
                fapid = Integer.parseInt(value);
                break;
            case "TESTGENOPS-OUTFILEMALE=":
                outFileM = value;
                break;
            case "TESTGENOPS-OUTFILEFEMALE=":
                outFileF = value;
                break;
            default:
                 msg = "Keyword " + key + " is not a known TestOperator-"
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

        if (!FileUtils.checkExists(inpFileM))
        {
            msg = "Input file '" + inpFileM + "' not found.";
            throw new DENOPTIMException(msg);
        }

        if (operatorToTest == Operator.XOVER
                && !FileUtils.checkExists(inpFileF))
        {
            msg = "Input file '" + inpFileF + "' not found.";
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
        RandomUtils.initialiseRNG(randomSeed);
        
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
