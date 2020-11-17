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

package fragspaceexplorer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fitness.FitnessParameters;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.APClass;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMEdge;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.rings.RingClosureParameters;


/**
 * Parameters controlling execution of FragSpaceExplorer.
 * 
 * @author Marco Foscato
 */

public class FSEParameters
{
    /**
     * Flag indicating that at least one parameter has been defined
     */
    private static boolean fseParamsInUse = false;

    /**
     * Working directory
     */
    private static String workDir = ".";

    /**
     * Log file
     */
    private static String logFile = "FSE.log";

    /**
     * File with user defined list of root graphs
     */
    private static String rootGraphsFile = null;
    
    private static boolean useGivenRoots = false;
    
    private static String rootGraphsFormat = 
    		DENOPTIMConstants.GRAPHFORMATSTRING; //Default

    /**
     * User defined list of root graphs
     */
    private static ArrayList<DENOPTIMGraph> rootGraphs;

    /**
     * Unique identifier file
     */
    private static String uidFile = "UID.txt";

    /**
     * Flag: optionally perform an external task for each accepted graph
     */
    private static boolean externalTask = false;

    /**
     * Folder containing <code>DENOPTIMGraph</code>s sorted by level
     * and reported as <code>String</code>s.
     */
    private static String dbRootDir = ".";  

    /**
     * Number of processors
     */
    private static int numCPU = 1;

    /**
     * Maximum wait for completion of a level (millisec)
     */
    private static long maxWait = 600000L; //Default 10 min

    /**
     * Time step between each check for completion of a level (millisec)
     */
    private static long waitStep = 5000L; //Default 5 sec
 
    /**
     * Maximum level accepted: number of frag-frag bond from the origin 
     * (i.e., the scaffolds)
     */
    private static int maxLevel = 2;

    /**
     * Number of combinations between every check out of the current position
     * into a checkpoint file.
     */
    private static int chkptStep = 100;

    /**
     * Checkpoint for restarting an interrupted FSE run
     */
    private static FSECheckPoint chkpt;

    /**
     * Name of checkpoint file for restarting an interrupted FSE run
     */
    private static String chkptFile = null;

    /**
     * Flag defining a restart from checkpoint file
     */
    private static boolean chkptRestart = false;

    /**
     * Flag requiring generation of the checkpoint files for the testing suite.
     * When <code>true</code> allows to stop the asyncronous, parallized 
     * exploration of a fragment space so that checkpoint files and 
     * serialized graphs can be generated and used to prepare the test suite.
     */
    private static boolean prepareChkAndSerForTests = false;

    /**
     * Verbosity level
     */
    private static int verbosity = 0;


//-----------------------------------------------------------------------------

    public static boolean fseParamsInUse()
    {
        return fseParamsInUse;
    }

//-----------------------------------------------------------------------------

    public static String getWorkDirectory()
    {
	return workDir;
    }

//-----------------------------------------------------------------------------

    public static String getLogFileName()
    {
        return logFile;
    }

//-----------------------------------------------------------------------------

    public static ArrayList<DENOPTIMGraph> getRootGraphs()
    {
        return rootGraphs;
    }

//-----------------------------------------------------------------------------

    public static String getUIDFileName()
    {
        return uidFile;
    }

//-----------------------------------------------------------------------------

    public static boolean submitExternalTask()
    {
	return externalTask;
    }

//-----------------------------------------------------------------------------

    public static String getDBRoot()
    {
	return dbRootDir;
    }

//-----------------------------------------------------------------------------

    public static int getNumberOfCPU()
    {
        return numCPU;
    }

//-----------------------------------------------------------------------------

    public static long getMaxWait()
    {
	return maxWait;
    }

//-----------------------------------------------------------------------------

    public static long getWaitStep()
    {
	return waitStep;
    }

//-----------------------------------------------------------------------------

    public static int getMaxLevel()
    {
        return maxLevel;
    }

//-----------------------------------------------------------------------------

    public static boolean useGivenRoots()
    {
	return useGivenRoots;
    }

//-----------------------------------------------------------------------------

    public static int getCheckPointStep()
    {
	return chkptStep;
    }

//-----------------------------------------------------------------------------

    public static String getCheckPointName()
    {
        return chkptFile;
    }

//-----------------------------------------------------------------------------

    public static FSECheckPoint getCheckPoint()
    {
	return chkpt;
    }

//-----------------------------------------------------------------------------

    public static boolean restartFromCheckPoint()
    {
	return chkptRestart;
    }

//-----------------------------------------------------------------------------

    public static boolean prepareFilesForTests()
    {
	return prepareChkAndSerForTests;
    }

//-----------------------------------------------------------------------------

    public static int getVerbosity()
    {
	return verbosity;
    }

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

                if (line.toUpperCase().startsWith("FSE-"))
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
                
                if (line.toUpperCase().startsWith("FP-"))
                {
                    FitnessParameters.interpretKeyword(line);
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
        case "FSE-WORKDIR=":
            workDir = value;
            break;
		case "FSE-ROOTGRAPHS=":
		    rootGraphsFile = value;
		    useGivenRoots = true;
		    break;
        case "FSE-ROOTGRAPHSFORMAT=":
            rootGraphsFormat = value.toUpperCase();
            break;
        case "FSE-UIDFILE=":
            uidFile = value;
            break;
		case "FSE-RESTARTFROMCHECKPOINT=":
		    chkptFile = value;
		    chkptRestart = true;
		    break;
		case "FSE-DEVEL-PREPAREFILESFORTESTS=":
		    prepareChkAndSerForTests = true;
		    break;
        case "FSE-CHECKPOINTSTEPLENGTH=":
            try
            {
                chkptStep = Integer.parseInt(value);
            }
            catch (Throwable t)
            {
                msg = "Unable to understand value " + key + "'" + value + "'";
                throw new DENOPTIMException(msg);
            }
            break;
		case "FSE-DBROOTFOLDER=":
		    dbRootDir = value;
		    break;
        case "FSE-NUMOFPROCESSORS=":
            try
            {
                numCPU = Integer.parseInt(value);
            }
            catch (Throwable t)
            {
                msg = "Unable to understand value " + key + "'" + value + "'";
                throw new DENOPTIMException(msg);
            }
            break;
        case "FSE-MAXWAIT=":
            try
            {
                maxWait = Long.parseLong(value, 10) * 1000L;
            }
            catch (Throwable t)
            {
                msg = "Unable to understand value " + key + "'" + value + "'";
                throw new DENOPTIMException(msg);
            }
            break;
        case "FSE-WAITSTEP=":
            try
            {
                waitStep = Long.parseLong(value, 10) * 1000L;
            }
            catch (Throwable t)
            {
                msg = "Unable to understand value " + key + "'" + value + "'";
                throw new DENOPTIMException(msg);
            }
            break;
        case "FSE-MAXLEVEL=":
            try
            {
                maxLevel = Integer.parseInt(value);
            }
            catch (Throwable t)
            {
                msg = "Unable to understand value " + key + "'" + value + "'";
                throw new DENOPTIMException(msg);
            }
            break;
        case "FSE-VERBOSITY=":
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
             msg = "Keyword " + key + " is not a known FragmentSpaceExplorer-"
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

	if (!dbRootDir.equals(workDir) && 
					!DenoptimIO.checkExists(dbRootDir))
	{
	    msg = "Directory " + dbRootDir + " not found. "
		  + "Please specify an existing directory where to put "
                  + "the DB of generated DENOPTIMGraphs.";
	   throw new DENOPTIMException(msg);
	}

	if (rootGraphsFile != null && !DenoptimIO.checkExists(rootGraphsFile))
	{
	    msg = "File with root graphs not found. Check " + rootGraphsFile;
            throw new DENOPTIMException(msg);
	}

	if (rootGraphsFormat != null 
	    && !rootGraphsFormat.equals(DENOPTIMConstants.GRAPHFORMATSTRING)
	    && !rootGraphsFormat.equals(DENOPTIMConstants.GRAPHFORMATBYTE))
        {
            msg = " The format for providing root graph must be either '" 
		  + DENOPTIMConstants.GRAPHFORMATSTRING + "' (default) for human readable "
		  + "strings, or '" + DENOPTIMConstants.GRAPHFORMATBYTE 
		  + "' for serialized objects. "
		  + "Unable to understand '" + rootGraphsFormat + "'.";
            throw new DENOPTIMException(msg);
        }
	else if (rootGraphsFormat.equals(DENOPTIMConstants.GRAPHFORMATSTRING))
	{
	    msg = "When root graphs are given as '"+ DENOPTIMConstants.GRAPHFORMATSTRING 
		  + "' existing symmetry relations between vertices belonging "
		  + "to the root graphs are NOT perceived. Symmetry may only "
		  + "be enforced starting from the first new layer of "
		  + "vertices.";
            DENOPTIMLogger.appLogger.log(Level.WARNING,msg);
	}
	else if (rootGraphsFormat.equals(DENOPTIMConstants.GRAPHFORMATBYTE))
	{
	    msg = "For now, only one serialized DENOPTIMGraph can by "
		  + "given as user-defined root graph using format '" 
		  + DENOPTIMConstants.GRAPHFORMATBYTE + "'.";
            DENOPTIMLogger.appLogger.log(Level.WARNING,msg);
	}

	if (numCPU <= 0 )
	{
	    msg = "Number of processors (" + numCPU + ") is not valid. "
		  + "Setting its value to 1.";
	    numCPU = 1;
	    DENOPTIMLogger.appLogger.info(msg);
	}

        if (maxLevel < 0)
        {
            msg = "The maximum level must be larger than zero.";
            throw new DENOPTIMException(msg);
        }

	if (chkptFile != null && !DenoptimIO.checkExists(chkptFile))
        {
            msg = "Checkpoint file " + chkptFile + " not found. ";
            throw new DENOPTIMException(msg);
        }

	if (chkptStep < 2)
	{
            msg = "The minimum acceptable value for the number of submitted "
		  + "tasks between two checkpoint files is 2. Change your "
		  + "input";
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
        boolean success = false;
		String curDir = workDir;
		if (curDir.equals("."))
		{
	            curDir = System.getProperty("user.dir");
		}
        String fileSep = System.getProperty("file.separator");
        while (!success)
        {
            SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyyhhmmss");
            String str = "FSE" + sdf.format(new Date());
            workDir = curDir + fileSep + str;
            success = DenoptimIO.createDirectory(workDir);
        }
		if (dbRootDir.equals("."))
		{
		    dbRootDir = workDir;
		}
		logFile = workDir + ".log";

        try
        {
            DENOPTIMLogger.getInstance().setupLogger(logFile);
        }
        catch (IOException ioe)
        {
            throw new DENOPTIMException(ioe);
        }

        if (FragmentSpaceParameters.fsParamsInUse())
        {
            FragmentSpaceParameters.processParameters();
        }

        if (RingClosureParameters.allowRingClosures())
        {
            RingClosureParameters.processParameters();
        }
        
        if (FitnessParameters.fitParamsInUse())
        {
            FitnessParameters.processParameters();
            externalTask = true;
        }
        
		if (useGivenRoots)
		{
            try
            {
				if (rootGraphsFormat.equals(DENOPTIMConstants.GRAPHFORMATSTRING))
				{
                    rootGraphs = DenoptimIO.readDENOPTIMGraphsFromFile(
								rootGraphsFile,true);
				}
				else if (rootGraphsFormat.equals(DENOPTIMConstants.GRAPHFORMATBYTE))
				{
				    rootGraphs = new ArrayList<DENOPTIMGraph>();
				    //TODO get arraylist of graphs or accept multiple files
				    DENOPTIMGraph g = DenoptimIO.deserializeDENOPTIMGraph(
								      new File(rootGraphsFile));
				    rootGraphs.add(g);
				}
				else
				{
				    String msg = "'" + rootGraphsFormat + "'"  
					  + " is not a valid format for graphs.";
				    throw new DENOPTIMException(msg);
				}
            }
            catch (Throwable t)
            {
                String msg = "Cannot read root graphs from " + rootGraphsFile;
                DENOPTIMLogger.appLogger.log(Level.INFO,msg);
                throw new DENOPTIMException(msg,t);
            }
		}
	
		if (chkptRestart)
		{
		    chkpt = FSEUtils.deserializeCheckpoint(chkptFile);
		}
        else
        {
	    chkpt = new FSECheckPoint();
            chkptFile = workDir + ".chk";
        }

        System.err.println("Program log file: " + logFile);
        System.err.println("Output files associated with the current run are " +
                                "located in " + workDir);
    }

//----------------------------------------------------------------------------

    /**
     * Print all parameters. 
     */

    public static void printParameters()
    {
		if (!fseParamsInUse)
		{
		    return;
		}
        String eol = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder(1024);
        sb.append(" FSEParameters ").append(eol);
        for (Field f : FSEParameters.class.getDeclaredFields()) 
        {
            try
            {
                sb.append(f.getName()).append(" = ").append(
                            f.get(FSEParameters.class)).append(eol);
            }
            catch (Throwable t)
            {
                sb.append("ERROR! Unable to print FSEParameters. Cause: " + t);
                break;
            }
        }
        DENOPTIMLogger.appLogger.info(sb.toString());
        sb.setLength(0);

        FragmentSpaceParameters.printParameters();
        RingClosureParameters.printParameters();
        FitnessParameters.printParameters();
    }

//----------------------------------------------------------------------------

}
