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

package denoptimga;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;

import denoptim.exception.DENOPTIMException;
import denoptim.fitness.FitnessParameters;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.io.DenoptimIO;
import denoptim.io.FileFormat;
import denoptim.logging.DENOPTIMLogger;
import denoptim.logging.Version;
import denoptim.rings.RingClosureParameters;
import denoptim.utils.RandomUtils;


/**
 *
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class GAParameters
{
    /**
     * Pathname to the working directory for the current run
     */
    protected static String dataDir = System.getProperty("user.dir");

    /**
     * Pathname of user defined parameters
     */
    protected static String paramFile = "";

    /** 
     * Pathname of the initial population file. The file itself can be an SDF or
     * a list of pathnames
     */
    protected static String initPoplnFile = "";

    /**
     * Pathname of the file with the list of individuals unique identifiers that
     * are initially known.
     */
    protected static String uidFileIn = "";

    /**
     * Pathname of the file where the individuals unique identifiers will be 
     * recorded.
     */
    protected static String uidFileOut = "";
    
    /**
     * Pathname of file where EA monitors dumps are printed
     */
    protected static String monitorFile = "";

    /**
     * Default name of the UIDFileOut
     */
    private static final String DEFUIDFILEOUTNAME = "MOLUID.txt";

    /**
     * Pathname to the file containing the list of previously visited graph
     */
    protected static String visitedGraphsFile = "GRAPHS.txt";

    /**
     * Pathname to the file collecting the failed sdf molecules
     */
    static String failedSDF = "";

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
     * Maximum number of attempts to perform any genetic operation (i.e., either
     * crossover or mutation) on any parents before giving up.
     */
    protected static int maxGeneticOpAttempts = 100;

    /**
     * Replacement strategy: 1) replace worst individuals with new ones that are
     * better than the worst, 2) no replacement (the population keeps growing)
     */
    protected static int replacementStrategy = 1;

    /**
     * Definition of the growth probability function:
     */
    protected static int growthProbabilityScheme = 0;
 
    /**
     * Parameter controlling the growth probability function of types
     * 'EXP_DIFF' and 'TANH'
     */
    protected static double growthMultiplier = 0.5;
 
    /** 
     * Parameters controlling the growth probability function
     * of type 'SIGMA': steepness of the function where p=50%
     */
    protected static double growthSigmaSteepness = 1.0;

    /** 
     * Parameters controlling the growth probability function
     * of type 'SIGMA': level at which p=50% (can be  a float)
     */
    protected static double growthSigmaMiddle = 2.5;
    
    /**
     * Definition of the crowding probability function. By default, the 
     * probability of using an AP that is hosted on an atom that already has an 
     * used AP is 100%.
     */
    protected static int crowdingProbabilityScheme = 3;
 
    /**
     * Parameter controlling the crowding probability function of types
     * 'EXP_DIFF' and 'TANH'
     */
    protected static double crowdingMultiplier = 0.5;
 
    /** 
     * Parameters controlling the crowding probability function
     * of type 'SIGMA': steepness of the function where p=50%
     */
    protected static double crowdingSigmaSteepness = 1.0;

    /** 
     * Parameters controlling the crowding probability function
     * of type 'SIGMA': level at which p=50% (can be  a float)
     */
    protected static double crowdingSigmaMiddle = 2.5;

    /**
     * The probability at which symmetric substitution occurs
     */
    protected static double symmetricSubProbability = 0.8;
    
    /**
     * The relative weight at which mutation is performed
     */
    protected static double mutationWeight = 1.0;
    
    /**
     * The relative weight at which construction from scratch is performed
     */
    protected static double builtAnewWeight = 1.0;
    
    /**
     * The relative weight at which crossover is performed
     */
    protected static double crossoverWeight = 1.0;

    /**
     * Crossover parents selection strategy: integer code
     */
    protected static int xoverSelectionMode = 3;

    /**
     * Crossover parents selection strategy: string
     */
    protected static String strXoverSelectionMode =
            "STOCHASTIC UNIVERSAL SAMPLING";

    /**
     * The seed value for random number generation
     */
    protected static long seed = 0L;
   
    /**
     * Parallelization scheme: synchronous or asynchronous 
     */
    protected static int parallelizationScheme = 1;

    /**
     * Maximum number of parallel tasks
     */
    protected static int numParallelTasks = 0;

    /**
     * Flag controlling how to sort the population based on the fitness
     */
    protected static boolean sortOrderDecreasing = true;

    /**
     * Precision for reporting the value of the fitness
     */
    protected static int precisionLevel = 3;

    /**
     * Monitor dumps step. The EA {@link Monitor} will dump data to file every 
     * this number is number of new attempts to generate candidate. 
     */
    protected static int monitorDumpStep = 50;
    

    /**
     * Print level
     */
    protected static int print_level = 0;
    
//------------------------------------------------------------------------------

    /**
     * Restores the default values of the most important parameters. 
     * Given that this is a static collection of parameters, running subsequent
     * experiments from the GUI ends up reusing parameters from the previous
     * run. This method allows to clean-up old values.
     */
    public static void resetParameters() 
    {
    	dataDir = System.getProperty("user.dir");
    	paramFile = "";
    	initPoplnFile = "";
    	uidFileIn = "";
    	uidFileOut = "";
    	//final: DEFUIDFILEOUTNAME = "MOLUID.txt";
    	visitedGraphsFile = "GRAPHS.txt";
    	failedSDF = "";
    	logFile = "";
    	populationSize = 50;
    	numOfChildren = 5;
    	numConvGen = 5;
    	numGenerations = 100;
    	maxTriesPerPop = 25;
    	maxGeneticOpAttempts = 100;
    	replacementStrategy = 1;
    	growthProbabilityScheme = 0;
    	growthMultiplier = 0.5;
    	growthSigmaSteepness = 1.0;
    	growthSigmaMiddle = 2.5;
    	symmetricSubProbability = 0.8;
    	crossoverWeight = 1.0;
    	mutationWeight = 1.0;
    	builtAnewWeight = 1.0;
    	xoverSelectionMode = 3;
    	strXoverSelectionMode = "STOCHASTIC UNIVERSAL SAMPLING";
    	seed = 0L;
    	parallelizationScheme = 1;
    	numParallelTasks = 0;
    	sortOrderDecreasing = true;
    	precisionLevel = 3;
    	print_level = 0;
    	monitorDumpStep = 50;
    	
        FragmentSpaceParameters.resetParameters();
        RingClosureParameters.resetParameters();
        FitnessParameters.resetParameters();
    }

   
//------------------------------------------------------------------------------

    protected static String getUIDFileIn()
    {
        return uidFileIn;
    }

//------------------------------------------------------------------------------

    protected static String getUIDFileOut()
    {
        return uidFileOut;
    }

//------------------------------------------------------------------------------

    protected static String getVisitedGraphsFile()
    {
        return visitedGraphsFile;
    }
    
//------------------------------------------------------------------------------

    protected static String getMonitorFile()
    {
        return monitorFile;
    }
    
//------------------------------------------------------------------------------
    
    protected static int getMonitorDumpStep()
    {
        return monitorDumpStep;
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

    protected static int getNumberOfCPU()
    {
        return numParallelTasks;
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

    protected static int getMaxGeneticOpAttempts()
    {
        return maxGeneticOpAttempts;
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

    protected static double getCrowdingFactorSteepSigma()
    {
        return crowdingSigmaSteepness;
    }

//------------------------------------------------------------------------------

    protected static double getCrowdingFactorMiddleSigma()
    {
        return crowdingSigmaMiddle;
    }

//------------------------------------------------------------------------------

    protected static double getCrowdingMultiplier()
    {
        return crowdingMultiplier;
    }

//------------------------------------------------------------------------------

    protected static int getCrowdingProbabilityScheme()
    {
        return crowdingProbabilityScheme;
    }
    
//------------------------------------------------------------------------------

    protected static double getGrowthFactorSteepSigma()
    {
        return growthSigmaSteepness;
    }

//------------------------------------------------------------------------------

    protected static double getGrowthFactorMiddleSigma()
    {
        return growthSigmaMiddle;
    }

//------------------------------------------------------------------------------

    protected static double getGrowthMultiplier()
    {
        return growthMultiplier;
    }

//------------------------------------------------------------------------------

    protected static int getGrowthProbabilityScheme()
    {
        return growthProbabilityScheme;
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

    protected static String getSelectionStrategy()
    {
        return strXoverSelectionMode;
    }

//------------------------------------------------------------------------------

    protected static int getSelectionStrategyType()
    {
        return xoverSelectionMode;
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
    protected static double getCrossoverWeight()
    {
        return crossoverWeight;
    }

//------------------------------------------------------------------------------

    protected static double getMutationWeight()
    {
        return mutationWeight;
    }
    
//------------------------------------------------------------------------------

    protected static double getConstructionWeight()
    {
        return builtAnewWeight;
    }

//------------------------------------------------------------------------------

    protected static double getSymmetryProbability()
    {
        return symmetricSubProbability;
    }
    
//------------------------------------------------------------------------------

    protected static String getInitialPopulationFile()
    {
        return initPoplnFile;
    }
    
//------------------------------------------------------------------------------

    protected static void printParameters()
    {
        String eol = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder(1024);
        sb.append(Version.message());
        sb.append("# ").append(DateFormat.getDateTimeInstance(
            DateFormat.LONG, DateFormat.LONG).format(new Date())).append(eol);

        sb.append("------------------- DENOPTIM GAParameters -"
                + "----------------------").append(eol);
        for (Field f : GAParameters.class.getDeclaredFields())
        {
            try
            {
                sb.append(f.getName()).append(" = ").append(
                                         f.get(GAParameters.class)).append(eol);
            }
            catch (IllegalArgumentException | IllegalAccessException t)
            {
                sb.append("ERROR! Unable to print GAParameters.");
                break;
            }
        }
        sb.append("-------------------------------------------" +
                                    "----------------------").append(eol);

        DENOPTIMLogger.appLogger.info(sb.toString());
        sb.setLength(0);

        FragmentSpaceParameters.printParameters();
        RingClosureParameters.printParameters();
        FitnessParameters.printParameters();
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

                if (line.toUpperCase().startsWith("GA-NUMPARALLELTASKS="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        numParallelTasks = Integer.parseInt(option);
                    }
                    continue;
                }
                
                if (line.toUpperCase().startsWith("GA-PARALLELIZATION="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    switch (option.toUpperCase())
                    {
                        case "SYNCHRONOUS":
                            parallelizationScheme = 1;
                            break;
                        case "ASYNCHRONOUS":
                            parallelizationScheme = 2;
                            break;
                        default:
                            throw new DENOPTIMException("Unknown parallelization scheme.");
                    }
                    continue;
                }

                if (line.toUpperCase().startsWith("GA-PRECISIONLEVEL="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        precisionLevel = Integer.parseInt(option);
                    }
                    continue;
                }

                if (line.toUpperCase().startsWith("GA-UIDFILEIN="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        uidFileIn = option;
                    }
                    continue;
                }

                if (line.toUpperCase().startsWith("GA-UIDFILEOUT="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        uidFileOut = option;
                    }
                    continue;
                }
                
                if (line.toUpperCase().startsWith("GA-MONITORFILE="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        monitorFile = option;
                    }
                    continue;
                }
                
                if (line.toUpperCase().startsWith("GA-MONITORDUMPSTEP="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        monitorDumpStep = Integer.parseInt(option);
                    }
                    continue;
                }
                
                if (line.toUpperCase().startsWith("GA-RANDOMSEED="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        seed = Long.parseLong(option);
                    }
                    continue;
                }
                
                if (line.toUpperCase().startsWith("GA-MAXTRIESPERPOPULATION="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                        maxTriesPerPop  = Integer.parseInt(option);
                }
                
                if (line.toUpperCase().startsWith("GA-MAXGENETICOPSATTEMPTS="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                        maxGeneticOpAttempts  = Integer.parseInt(option);
                }

                if (line.toUpperCase().startsWith("GA-INITPOPLNFILE="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        initPoplnFile = option;
                    }
                    continue;
                }

                if (line.toUpperCase().startsWith("GA-PRINTLEVEL="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        print_level = Integer.parseInt(option);
                    }
                    continue;
                }

                if (line.toUpperCase().startsWith("GA-SORTBYINCREASINGFITNESS="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        sortOrderDecreasing = false;
                    }
                    continue;
                }

                if (line.toUpperCase().startsWith("GA-GROWTHMULTIPLIER="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        growthMultiplier = Double.parseDouble(option);
                    }
                }

                if (line.toUpperCase().startsWith("GA-GROWTHSIGMASTEEPNESS="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        growthSigmaSteepness = Double.parseDouble(option);
                    }
                }

                if (line.toUpperCase().startsWith("GA-GROWTHSIGMAMIDDLE="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        growthSigmaMiddle = Double.parseDouble(option);
                    }
                }

                if (line.toUpperCase().startsWith("GA-GROWTHPROBSCHEME="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    growthProbabilityScheme = convertGrowthProbabilityScheme(option);
                }
                
                if (line.toUpperCase().startsWith("GA-CROWDMULTIPLIER="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        crowdingMultiplier = Double.parseDouble(option);
                    }
                }

                if (line.toUpperCase().startsWith("GA-CROWDSIGMASTEEPNESS="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        crowdingSigmaSteepness = Double.parseDouble(option);
                    }
                }

                if (line.toUpperCase().startsWith("GA-CROWDSIGMAMIDDLE="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        crowdingSigmaMiddle = Double.parseDouble(option);
                    }
                }

                if (line.toUpperCase().startsWith("GA-CROWDPROBSCHEME="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    crowdingProbabilityScheme = convertGrowthProbabilityScheme(option);
                }

                if (line.toUpperCase().startsWith("GA-NUMGENERATIONS="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        numGenerations = Integer.parseInt(option);
                    }
                }

                if (line.toUpperCase().startsWith("GA-NUMCHILDREN="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        GAParameters.numOfChildren = Integer.parseInt(option);
                    }
                }

                if (line.toUpperCase().startsWith("GA-CROSSOVERWEIGHT="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        GAParameters.crossoverWeight = Double.parseDouble(option);
                    }
                }

                if (line.toUpperCase().startsWith("GA-MUTATIONWEIGHT="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        GAParameters.mutationWeight = Double.parseDouble(option);
                    }
                }
                
                if (line.toUpperCase().startsWith("GA-CONSTRUCTIONWEIGHT="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        GAParameters.builtAnewWeight = Double.parseDouble(option);
                    }
                }
                
                if (line.toUpperCase().startsWith("GA-REPLACEMENTSTRATEGY="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    switch (option.toUpperCase())
                    {
                        case "NONE":
                        	replacementStrategy = 2;
                            break;
                        case "ELITIST":
                            replacementStrategy = 1;
                            break;
                        default:
                            throw new DENOPTIMException("Unknown replacement strategy.");
                    }
                }

                if (line.toUpperCase().startsWith("GA-POPULATIONSIZE="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        GAParameters.populationSize = Integer.parseInt(option);
                    }
                }

                if (line.toUpperCase().startsWith("GA-NUMCONVGEN="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        GAParameters.numConvGen = Integer.parseInt(option);
                    }
                }

                if (line.toUpperCase().startsWith("GA-XOVERSELECTIONMODE="))
                {
                    option = line.substring(line.indexOf("=") + 1).trim();
                    if (option.length() > 0)
                    {
                        GAParameters.xoverSelectionMode = -1;
                        if (option.compareToIgnoreCase("TS") == 0)
                        {
                            GAParameters.xoverSelectionMode = 1;
                            GAParameters.strXoverSelectionMode = "TOURNAMENT";
                        }
                        if (option.compareToIgnoreCase("RWS") == 0)
                        {
                            GAParameters.xoverSelectionMode = 2;
                            GAParameters.strXoverSelectionMode = "ROULETTE WHEEL";
                        }
                        if (option.compareToIgnoreCase("SUS") == 0)
                        {
                            GAParameters.xoverSelectionMode = 3;
                            GAParameters.strXoverSelectionMode =
                                "STOCHASTIC UNIVERSAL SAMPLING";
                        }
                        if (option.compareToIgnoreCase("RANDOM") == 0)
                        {
                            GAParameters.xoverSelectionMode = 4;
                            GAParameters.strXoverSelectionMode = "RANDOM";
                        }
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
    
    public static int convertGrowthProbabilityScheme(String option) 
    		throws DENOPTIMException 
    {
        int res = 0;
    	switch (option.toUpperCase())
        {
            case "EXP_DIFF":
                res = 0;
                break;
            case "TANH":
                res = 1;
                break;
            case "SIGMA":
                res = 2;
                break;
            case "UNRESTRICTED":
                res = 3;
                break;
            default:
                throw new DENOPTIMException(
                		"Unknown growth probability scheme.");
        }
		return res;
	}
    
//-----------------------------------------------------------------------------

	/**
     * Create the directory that will store the output of the GA run
     * @throws DENOPTIMException 
     */

    protected static void createWorkingDirectory() throws DENOPTIMException
    {
        String fileSep = System.getProperty("file.separator");
        String cdataDir = dataDir;
        boolean success = false;
        while (!success)
        {
            SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyyhhmmss");
            String str = "RUN" + sdf.format(new Date());
            dataDir = cdataDir + fileSep + str;
            success = DenoptimIO.createDirectory(dataDir);
        }
        DenoptimIO.addToRecentFiles(dataDir, FileFormat.GA_RUN);
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
        
        if (monitorFile.equals(""))
        {
            monitorFile = dataDir + ".eaMonitor";
        }

        failedSDF = dataDir + "_FAILED.sdf";

        if (uidFileOut.equals(""))
        {
            uidFileOut = dataDir + fileSep + DEFUIDFILEOUTNAME;
        }

        try
        {
            DENOPTIMLogger.getInstance().setupLogger(logFile);
            FileUtils.copyFileToDirectory(new File(paramFile), new File(dataDir));
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

        int nproc = Runtime.getRuntime().availableProcessors();
        if (numParallelTasks == 0)
        {
            numParallelTasks = nproc;
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
        }
        
        System.err.println("Program log file: " + logFile);
        System.err.println("Output files associated with the current run are " +
                                "located in " + dataDir);
    }

//------------------------------------------------------------------------------

    protected static void checkParameters() throws DENOPTIMException
    {
        String error = "";
        if (GAParameters.populationSize < 10)
        {
            String msg = "Small population size is allowed only for testing.";
            DENOPTIMLogger.appLogger.log(Level.WARNING,msg);
        }
        if (GAParameters.numOfChildren <= 0)
        {
            error = "Number of children must be a positive number.";
            throw new DENOPTIMException(error);
        }
        if (GAParameters.numGenerations <= 0)
        {
            error = "Number of generations must be a positive number.";
            throw new DENOPTIMException(error);
        }
        
        if (GAParameters.numConvGen <= 0)
        {
            error = "Number of convergence iterations must be a positive "
                    + "number.";
            throw new DENOPTIMException(error);
        }
        
        if (GAParameters.symmetricSubProbability < 0. ||
                            GAParameters.symmetricSubProbability > 1.)
        {
            error = "Symmetric molecule probability must be between 0 and 1.";
            throw new DENOPTIMException(error);
        }
        
        if (GAParameters.mutationWeight < 0.)
        {
            error = "Weight of mutation must be a positive number";
            throw new DENOPTIMException(error);
        }
        
        if (GAParameters.crossoverWeight < 0.)
        {
            error = "Weight of crossover must be a positive number";
            throw new DENOPTIMException(error);
        }
        
        if (GAParameters.builtAnewWeight < 0.)
        {
            error = "Weight of construction must be a positive number";
            throw new DENOPTIMException(error);
        }

        if (initPoplnFile.length() > 0)
        {
            if (!DenoptimIO.checkExists(initPoplnFile))
            {
                error = "Cannot find initial population data: " + initPoplnFile;
                throw new DENOPTIMException(error);
            }
        }

        if (replacementStrategy < 0 || replacementStrategy > 2)
        {
            error = "Allowed values for replacementStrategy (1-2)";
            throw new DENOPTIMException(error);
        }

        if (FitnessParameters.fitParamsInUse())
        {
            FitnessParameters.checkParameters();
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

//------------------------------------------------------------------------------

}
