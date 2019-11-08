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

package denoptimrnd;

import java.lang.reflect.Field;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.text.DateFormat;
import java.util.logging.Level;

import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.logging.Version;
import denoptim.rings.RingClosureParameters;
import denoptim.fragspace.FragmentSpaceParameters;
import org.apache.commons.io.FileUtils;
import denoptim.utils.RandomUtils;


/**
 * Parameters collector for evolution solely based on random selection.
 *
 * @author Marco Foscato
 */
public class RNDParameters
{
    /**
     * Pathname to the working directory for the current run
     */
    protected static String dataDir = "";

    /**
     * Pathname of user defined parameters
     */
    protected static String paramFile = "";

    /**
     * Pathname of the text file collecting all possible graphs.
     */
    protected static String allGraphsFile = "";

    /**
     * Pathname of the file where the individuals unique identifiers will be 
     * recorded.
     */
    protected static String uidFileOut = "";

    /**
     * Default name of the UIDFileOut
     */
    private static final String DEFUIDFILEOUTNAME = "MOLUID.txt";

    /**
     * Pathname of log file (STDOUT)
     */
    protected static String logFile = "";

    /**
     * Size of the population 
     */
    protected static int populationSize = 50;

    /**
     * Number of children (i.e., new offspring) to be produced in each 
     * generation
     */
    protected static int numOfChildren = 5;

    /**
     * Number of identical generations before convergence is reached
     */
    protected static int numConvGen = 5;

    /**
     * Maximum number of generations to run for
     */
    protected static int numGenerations = 100;

    /**
     * Factor controlling the maximum number of attempts to build a graph
     * so that the maximum number of attempts = factor * population size
     */
    protected static int maxTriesPerPop = 25;

    /**
     * Replacemt strategy. 1) replace worst individuals with new ones that are
     * better than the worst, 2) no replacement (the population keeps growing)
     */
    protected static int replacementStrategy = 1;

    /**
     * The seed value for random number generation
     */
    protected static long seed = 0L;

    /**
     * Flag controlling how to sort the population based on the fitness
     */
    protected static boolean sortOrderDecreasing = true;
   
    /**
     * Precision for reporting the value of the fitness
     */
    protected static int precisionLevel = 3;

    /**
     * Print level
     */
    protected static int print_level = 0; 

   
//------------------------------------------------------------------------------

    protected static String getUIDFileOut()
    {
        return uidFileOut;
    }

//------------------------------------------------------------------------------

    protected static int getPrecisionLevel()
    {
        return precisionLevel;
    }
    
//------------------------------------------------------------------------------

    protected static int getPrintLevel()
    {
        return print_level;
    }
    
//------------------------------------------------------------------------------

    protected static boolean isSortOrderDecreasing()
    {
        return sortOrderDecreasing;
    }

//------------------------------------------------------------------------------

    protected static int getMaxTriesFactor()
    {
        return maxTriesPerPop;
    }

//------------------------------------------------------------------------------

    protected static String getDataDirectory()
    {
        return dataDir;
    }

//------------------------------------------------------------------------------

    protected static int getReplacementStrategy()
    {
        return replacementStrategy;
    }

//------------------------------------------------------------------------------

    protected static int getPopulationSize()
    {
        return populationSize;
    }

//------------------------------------------------------------------------------

    protected static int getNumberOfGenerations()
    {
        return numGenerations;
    }

//------------------------------------------------------------------------------

    protected static int getNumberOfConvergenceGenerations()
    {
        return numConvGen;
    }

//------------------------------------------------------------------------------

    protected static int getNumberOfChildren()
    {
        return numOfChildren;
    }

//------------------------------------------------------------------------------

    protected static String getAllGraphsFile()
    {
        return allGraphsFile;
    }

//------------------------------------------------------------------------------

    protected static void printParameters()
    {
        String eol = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder(1024);
        sb.append(Version.message());
        sb.append("# ").append(DateFormat.getDateTimeInstance(
            DateFormat.LONG, DateFormat.LONG).format(new Date())).append(eol);

        sb.append("------------------- DENOPTIM RNDParameters -"
                + "----------------------").append(eol);
        for (Field f : RNDParameters.class.getDeclaredFields())
        {
            try
            {
                sb.append(f.getName()).append(" = ").append(
                                        f.get(RNDParameters.class)).append(eol);
            }
            catch (IllegalArgumentException | IllegalAccessException t)
            {
                sb.append("ERROR! Unable to print RNDParameters.");
                break;
            }
        }
        sb.append("-------------------------------------------" +
                                    "----------------------").append(eol);

        DENOPTIMLogger.appLogger.info(sb.toString());
        sb.setLength(0);

	FragmentSpaceParameters.printParameters();
	RingClosureParameters.printParameters();
    }

//------------------------------------------------------------------------------

    /**
     * Read the parameter file
     * @param infile
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

                if (line.toUpperCase().startsWith("RND-PRECISIONLEVEL="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        precisionLevel = Integer.parseInt(option);
                    }
                    continue;
                }

                if (line.toUpperCase().startsWith("RND-UIDFILEOUT="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        uidFileOut = option;
                    }
                    continue;
                }

                if (line.toUpperCase().startsWith("RND-RANDOMSEED="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        seed = Long.parseLong(option);
                    }
                    continue;
                }
                

                if (line.toUpperCase().startsWith("RND-MAXTRIESPERPOPULATION="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                        maxTriesPerPop  = Integer.parseInt(option);
                }

                if (line.toUpperCase().startsWith("RND-ALLGRAPHSFILE="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        allGraphsFile = option;
                    }
                    continue;
                }

                if (line.toUpperCase().startsWith("RND-PRINTLEVEL="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        print_level = Integer.parseInt(option);
                    }
                    continue;
                }

                if (line.toUpperCase().startsWith("RND-SORTORDER="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        switch (Integer.parseInt(option)) 
                        {
                        // descending order
                            case 1:
                                sortOrderDecreasing = true;
                                break;
                        // ascending order
                            case 2:
                                sortOrderDecreasing = false;
                                break;
                            default:
                                throw new DENOPTIMException(
				      "Incorrect specification of sort order.");
                        }
                    }
                    continue;
                }

                if (line.toUpperCase().startsWith("RND-NUMGENERATIONS="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        numGenerations = Integer.parseInt(option);
                    }
                }

                if (line.toUpperCase().startsWith("RND-NUMCHILDREN="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        RNDParameters.numOfChildren = Integer.parseInt(option);
                    }
                }

                if (line.toUpperCase().startsWith("RND-REPLACEMENTSTRATEGY="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        RNDParameters.replacementStrategy = 
						       Integer.parseInt(option);
                    }
                }

                if (line.toUpperCase().startsWith("RND-POPULATIONSIZE="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        RNDParameters.populationSize = Integer.parseInt(option);
                    }
                }

                if (line.toUpperCase().startsWith("RND-NUMCONVGEN="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        RNDParameters.numConvGen = Integer.parseInt(option);
                    }
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
                paramFile = infile;
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }
        
        option = null;
        line = null;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Create the directory that will store the output of the GA run
     * @throws DENOPTIMException 
     */

    protected static void createWorkingDirectory() throws DENOPTIMException
    {
        boolean success = false;
        String curDir = System.getProperty("user.dir");
        String fileSep = System.getProperty("file.separator");
        while (!success)
        {
            SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyyhhmmss");
            String str = "RUN" + sdf.format(new Date());
            dataDir = curDir + fileSep + str;
            success = DenoptimIO.createDirectory(dataDir);
        }
    }

//------------------------------------------------------------------------------

    // reads files as listed in the parameters
    // other initialization code such as the random number generator
    protected static void processParameters() throws DENOPTIMException
    {
        String fileSep = System.getProperty("file.separator");

        // regardless of the random number seed, the following
        // will always create a new directory
        // inside this directory all further directories
        // will be created
        
        createWorkingDirectory();

        logFile = dataDir + ".log";

	if (uidFileOut.equals(""))
	{
	    uidFileOut = dataDir + fileSep + DEFUIDFILEOUTNAME;
	}

        try
        {
            DENOPTIMLogger.getInstance().setupLogger(logFile);
            FileUtils.copyFileToDirectory(new File(paramFile), 
							     new File(dataDir));
        }
        catch (IOException ioe)
        {
            throw new DENOPTIMException(ioe);
        }

        // set random number generator
        if (seed == 0)
        {
            RandomUtils.initialiseRNG();
            seed = RandomUtils.getSeed();
        }
        else
        {
            RandomUtils.initialiseRNG(seed);
        }

        if (FragmentSpaceParameters.fsParamsInUse())
        {
            FragmentSpaceParameters.processParameters();
        }

	if (RingClosureParameters.allowRingClosures())
	{
	    RingClosureParameters.processParameters();
	}

        System.err.println("Program log file: " + logFile);
        System.err.println("Output files associated with the current run are " +
                                "located in " + dataDir);
    }

//------------------------------------------------------------------------------

    protected static void checkParameters() throws DENOPTIMException
    {
        String error = "";
        if (RNDParameters.populationSize < 10)
        {
	    String msg = "Small population size is allowed only for testing.";
            DENOPTIMLogger.appLogger.log(Level.WARNING,msg);
        }
        if (RNDParameters.numOfChildren <= 0)
        {
            error = "Number of children must be a positive number.";
            throw new DENOPTIMException(error);
        }
        if (RNDParameters.numGenerations <= 0)
        {
            error = "Number of generations must be a positive number.";
            throw new DENOPTIMException(error);
        }
        if (RNDParameters.numConvGen <= 0)
        {
            error = "Number of convergence iterations must be a positive number.";
            throw new DENOPTIMException(error);
        }

        if (allGraphsFile.length() > 0)
        {
            if (!DenoptimIO.checkExists(allGraphsFile))
            {
                error = "Cannot find colleciton of graphs: " + allGraphsFile;
                throw new DENOPTIMException(error);
            }
        }
    }

//------------------------------------------------------------------------------

}
