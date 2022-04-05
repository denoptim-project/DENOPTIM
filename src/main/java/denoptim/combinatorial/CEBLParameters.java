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

package denoptim.combinatorial;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;

import denoptim.exception.DENOPTIMException;
import denoptim.files.FileFormat;
import denoptim.files.FileUtils;
import denoptim.graph.DENOPTIMGraph;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.programs.RunTimeParameters;


/**
 * Parameters controlling execution of the combinatorial algorithm for 
 * exploration of a fragment space by layer (CEBL).
 * 
 * @author Marco Foscato
 */

public class CEBLParameters extends RunTimeParameters
{
    /**
     * File with user defined list of root graphs
     */
    private String rootGraphsFile = null;
    
    /**
     * Flag declaring that generation of graphs will use a given list of graphs 
     * as starting points for the exploration.
     */
    private boolean useGivenRoots = false;
   
    /**
     * User defined list of root graphs
     */
    private ArrayList<DENOPTIMGraph> rootGraphs;

    /**
     * Unique identifier file
     */
    private String uidFile = "UID.txt";

    /**
     * Flag: optionally perform an evaluation of the fitness/descriptors on
     *  each accepted graph
     */
    private boolean runFitnessTask = false;

    /**
     * Folder containing <code>DENOPTIMGraph</code>s sorted by level
     * and reported as <code>String</code>s.
     */
    private String dbRootDir = ".";

    /**
     * Number of processors
     */
    private int numCPU = 1;

    /**
     * Maximum wait for completion of a level (millisec)
     */
    private long maxWait = 600000L; //Default 10 min

    /**
     * Time step between each check for completion of a level (millisec)
     */
    private long waitStep = 5000L; //Default 5 sec
 
    /**
     * Maximum level accepted: number of frag-frag bond from the origin 
     * (i.e., the scaffolds)
     */
    private int maxLevel = 2;

    /**
     * Number of combinations between every check out of the current position
     * into a checkpoint file.
     */
    private int chkptStep = 100;

    /**
     * Checkpoint for restarting an interrupted FSE run
     */
    private FSECheckPoint chkpt = null;

    /**
     * Name of checkpoint file for restarting an interrupted FSE run
     */
    private String chkptFile = null;

    /**
     * Flag defining a restart from checkpoint file
     */
    private boolean chkptRestart = false;

    /**
     * Flag requiring generation of the checkpoint files for the testing suite.
     * When <code>true</code> allows to stop the asyncronous, parallelized 
     * exploration of a fragment space so that checkpoint files and 
     * serialized graphs can be generated and used to prepare the test suite.
     */
    private boolean prepareChkAndSerForTests = false;

//-----------------------------------------------------------------------------
    
    /**
     * Constructor
     */
    public CEBLParameters()
    {
        this(ParametersType.CEBL_PARAMS);
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Constructor
     */
    private CEBLParameters(ParametersType paramType)
    {
        super(paramType);
    }

//-----------------------------------------------------------------------------

    public ArrayList<DENOPTIMGraph> getRootGraphs()
    {
        return rootGraphs;
    }

//-----------------------------------------------------------------------------

    public String getUIDFileName()
    {
        return uidFile;
    }

//-----------------------------------------------------------------------------

    public boolean submitFitnessTask()
    {
	    return runFitnessTask;
    }

//-----------------------------------------------------------------------------

    public String getDBRoot()
    {
	    return dbRootDir;
    }
    
//-----------------------------------------------------------------------------
    
    public void setDBRoot(String pathname)
    {
        this.dbRootDir = pathname;
    }

//-----------------------------------------------------------------------------

    public int getNumberOfCPU()
    {
        return numCPU;
    }

//-----------------------------------------------------------------------------

    public long getMaxWait()
    {
	    return maxWait;
    }

//-----------------------------------------------------------------------------

    public long getWaitStep()
    {
	    return waitStep;
    }

//-----------------------------------------------------------------------------

    public int getMaxLevel()
    {
        return maxLevel;
    }

//-----------------------------------------------------------------------------

    public boolean useGivenRoots()
    {
	    return useGivenRoots;
    }

//-----------------------------------------------------------------------------

    public int getCheckPointStep()
    {
	    return chkptStep;
    }

//-----------------------------------------------------------------------------

    public String getCheckPointName()
    {
        return chkptFile;
    }

//-----------------------------------------------------------------------------

    public FSECheckPoint getCheckPoint()
    {
	    return chkpt;
    }

//-----------------------------------------------------------------------------

    public boolean restartFromCheckPoint()
    {
	    return chkptRestart;
    }

//-----------------------------------------------------------------------------

    public boolean prepareFilesForTests()
    {
	    return prepareChkAndSerForTests;
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
		case "ROOTGRAPHS=":
		    rootGraphsFile = value;
		    useGivenRoots = true;
		    break;
        case "UIDFILE=":
            uidFile = value;
            break;
		case "RESTARTFROMCHECKPOINT=":
		    chkptFile = value;
		    chkptRestart = true;
		    break;
		case "DEVEL-PREPAREFILESFORTESTS=":
		    prepareChkAndSerForTests = true;
		    break;
        case "CHECKPOINTSTEPLENGTH=":
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
		case "DBROOTFOLDER=":
			//NB: this key 'DBROOTFOLDER' is hard coded also in CombinatorialExplorerByLayer
		    dbRootDir = value;
		    break;
        case "NUMOFPROCESSORS=":
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
        case "MAXWAIT=":
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
        case "WAITSTEP=":
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
        case "MAXLEVEL=":
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
    
    public void checkParameters() throws DENOPTIMException
    {
        String msg = "";

    	if (!workDir.equals(".") && !FileUtils.checkExists(workDir))
    	{
    	    msg = "Directory '" + workDir + "' not found. Please specify an "
    	 	 + "existing directory.";
    	    throw new DENOPTIMException(msg);
    	}
    
    	if (!dbRootDir.equals(workDir) && !FileUtils.checkExists(dbRootDir))
    	{
    	    msg = "Directory '" + dbRootDir + "' not found. "
    		  + "Please specify an existing directory where to put "
                      + "the DB of generated DENOPTIMGraphs.";
    	   throw new DENOPTIMException(msg);
    	}
    
    	if (rootGraphsFile != null && !FileUtils.checkExists(rootGraphsFile))
    	{
    	    msg = "File with root graphs not found. Check " + rootGraphsFile;
                throw new DENOPTIMException(msg);
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

	    if (chkptFile != null && !FileUtils.checkExists(chkptFile))
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
    	
    	checkOtherParameters();
    }

//----------------------------------------------------------------------------

    /**
     * Processes all parameters and initialize related objects.
     * @throws DENOPTIMException
     */

    public void processParameters() throws DENOPTIMException
    {
		String curDir = workDir;
        String fileSep = System.getProperty("file.separator");
        boolean success = false;
        while (!success)
        {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddkkmmss");
            String str = "FSE" + sdf.format(new Date());
            workDir = curDir + fileSep + str;
            success = FileUtils.createDirectory(workDir);
        }
        FileUtils.addToRecentFiles(workDir, FileFormat.FSE_RUN);
		if (dbRootDir.equals(".") || dbRootDir.equals(""))
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
        
        processOtherParameters();
        
        if (otherParameters.containsKey(ParametersType.FIT_PARAMS))
        {
            runFitnessTask = true;
        }
        
		if (useGivenRoots)
		{
            try
            {
                rootGraphs = DenoptimIO.readDENOPTIMGraphsFromFile(
                        new File(rootGraphsFile));
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
		    chkpt = CEBLUtils.deserializeCheckpoint(chkptFile);
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

}
