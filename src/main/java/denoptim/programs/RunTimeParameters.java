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

package denoptim.programs;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import denoptim.exception.DENOPTIMException;
import denoptim.files.FileUtils;
import denoptim.fitness.FitnessParameters;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.graph.rings.RingClosureParameters;
import denoptim.main.Main.RunType;
import denoptim.programs.combinatorial.CEBLParameters;
import denoptim.programs.denovo.GAParameters;
import denoptim.programs.fitnessevaluator.FRParameters;
import denoptim.programs.fragmenter.FragmenterParameters;
import denoptim.programs.genetweeker.GeneOpsRunnerParameters;
import denoptim.programs.grapheditor.GraphEdParameters;
import denoptim.programs.graphlisthandler.GraphListsHandlerParameters;
import denoptim.programs.isomorphism.IsomorphismParameters;
import denoptim.programs.mol2graph.Mol2GraphParameters;
import denoptim.programs.moldecularmodelbuilder.MMBuilderParameters;
import denoptim.utils.Randomizer;


/**
 * Collection of parameters controlling the behavior of the software. 
 * These parameters have a default value and are optionally defined at startup 
 * by reading in input parameter file.
 * Parameters are collected in a hierarchical structure so that those parameters
 * that determine the primary behavior of the software, i.e., define the 
 * {@link RunType}, determine also the type of the main parameter collector, 
 * i.e., any one of the implementations of this class. Other parameters, such as
 * those defining the settings of functions used by the main program, are 
 * collected as secondary parameters that can be accessed via the primary one
 * by means of the {@link #getParameters(ParametersType)} method.
 * 
 * @author Marco Foscato
 */

public abstract class RunTimeParameters
{
    /**
     * Flag signaling this is the master collection of parameters. 
     * The master collection is the one with the type that corresponds to the 
     * type of program task. A parameters collection is master
     * if it may contain other collections without being itself contained in
     * any other collection.
     */
    protected boolean isMaster = true;
    
    /**
     * Working directory
     */
    protected String workDir = System.getProperty("user.dir");

    /**
     * Log file
     */
    protected String logFile = "unset";
    
    /**
     * Program-specific logger. Note that initialization errors in input 
     * parameters are detected prior to starting the logger. Also, the logger is
     * initialized to avoid null during unit tests. In any proper run, this
     * logger should be overwritten by a call to 
     * {@link #startProgramSpecificLogger(String)}.
     */
    private Logger logger = Logger.getLogger("DummyLogger");
    
    /**
     * Program-specific random numbers and random decisions generator.
     */
    private Randomizer rng = null;
    
    /**
     * Verbosity level for logger. This is used to help the user
     * setting the {@link Level} of the {@link Logger} without knowing the
     * names of the logging levels.
     */
    protected int verbosity = 0;
    
    /**
     * Collection of other parameters by type.
     */
    protected Map<ParametersType, RunTimeParameters> otherParameters = 
            new HashMap<ParametersType, RunTimeParameters>();
    
    /**
     * The type of parameters collected in this instance.
     */
    private ParametersType paramType = null;
    
    /**
     * Identifier of the type of parameters 
     */
    public static enum ParametersType { 
        /**
         * Parameters pertaining the combinatorial exploration by layer.
         */
        CEBL_PARAMS,
        
        /**
         * Parameters pertaining the genetic algorithm.
         */
        GA_PARAMS,
        
        /**
         * Parameters pertaining the definition of the fragment space.
         */
        FS_PARAMS,
        
        /**
         * Parameters pertaining to ring closures in graphs.
         */
        RC_PARAMS,
        
        /**
         * Parameters pertaining the calculation of fitness (i.e., the fitness
         * provider).
         */
        FIT_PARAMS, 
        
        /**
         * Parameters pertaining the construction of three-dimensional molecular
         * models using the Tinker-based molecular mechanics approach.
         */
        MMB_PARAM, 
        
        /**
         * Parameters controlling a stand-alone fitness evaluation run.
         */
        FR_PARAMS, 
        
        /**
         * Parameters controlling stand-alone run of genetic operators.
         */
        GO_PARAMS, 
        
        /**
         * Parameters controlling the stand-alone editing of graphs.
         */
        GE_PARAMS,
        
        /**
         * Parameters controlling the stand-alone management of list of graphs.
         */
        GLH_PARAMS,
        
        /**
         * Parameters controlling the stand-alone detection of graph isomorphism.
         */
        ISO_PARAMS, 
        
        /**
         * Parameters controlling the fragmenter.
         */
        FRG_PARAMS,
        
        /**
         * Parameters controlling molecule-to-graph conversion
         */
        M2G_PARAMS;
        
        /**
         * The root of any keyword that is meant to be used to set any of the
         * parameters belonging to this class.
         */
        private String keywordRoot;
        
        /**
         * Implementation that supports this type of parameters.
         */
        private Class<?> implementation;
        
        static {
            CEBL_PARAMS.keywordRoot = "FSE-";
            GA_PARAMS.keywordRoot = "GA-";
            FS_PARAMS.keywordRoot = "FS-";
            FRG_PARAMS.keywordRoot = "FRG-";
            RC_PARAMS.keywordRoot = "RC-";
            FIT_PARAMS.keywordRoot = "FP-";
            FR_PARAMS.keywordRoot = "FR-";
            MMB_PARAM.keywordRoot = "3DB-";
            GO_PARAMS.keywordRoot = "TESTGENOPS-";
            GE_PARAMS.keywordRoot = "GRAPHEDIT-";
            GLH_PARAMS.keywordRoot = "GRAPHLISTS-";
            ISO_PARAMS.keywordRoot = "ISOMORPHISM-";
            M2G_PARAMS.keywordRoot = "M2G-";
            
            CEBL_PARAMS.implementation = CEBLParameters.class;
            GA_PARAMS.implementation = GAParameters.class;
            FS_PARAMS.implementation = FragmentSpaceParameters.class;
            FRG_PARAMS.implementation = FragmenterParameters.class;
            RC_PARAMS.implementation = RingClosureParameters.class;
            FIT_PARAMS.implementation = FitnessParameters.class;
            FR_PARAMS.implementation = FRParameters.class;
            MMB_PARAM.implementation = MMBuilderParameters.class;
            GO_PARAMS.implementation = GeneOpsRunnerParameters.class;
            GE_PARAMS.implementation = GraphEdParameters.class;
            GLH_PARAMS.implementation = GraphListsHandlerParameters.class;
            ISO_PARAMS.implementation = IsomorphismParameters.class;
            M2G_PARAMS.implementation = Mol2GraphParameters.class;
        }

        /**
         * @return the class implementation for this parameter type
         */
        public Class<?> getImplementation()
        {
            return implementation;
        }
        
        /**
         * @return the keyword root that triggers interpretation of a string
         * as a keyword pertaining to this type of parameters.
         */
        public String getKeywordRoot()
        {
            return keywordRoot;
        }
    }
    
    /*
     * TODO: if we want a general Parameter class it will have to define
     * - unique name of the parameter
     * - java type used to collect the value
     * - default value
     * - description of what the parameter is or what it controls
     * - code for parsing the value from input files
     * - code for checking
     * - core for processing the parameter
     * - what else?
     * 
     *  Challenge: what about parameters that depend on other parameters?
     *  Need to control order of processing the parameters, in some occasions.
     */
    
    
    /**
     * New line character
     */
    public final String NL = System.getProperty("line.separator");
    
//-----------------------------------------------------------------------------
    
    /**
     * Constructor
     * @param paramType the type of parameters this instance is meant to collect.
     */
    public RunTimeParameters(ParametersType paramType)
    {
        this.paramType = paramType;
        
        /*
         * This is the default logger "DummyLogger". It should be overwritten
         * by a call to startProgramSpecificLogger
         */
        this.logger.setLevel(Level.SEVERE); 
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Returns a string defining the type the parameters collected here.
     * @return a string defining the type the parameters collected here.
     */
    public String paramTypeName()
    {
        return paramType.toString();
    }

//-----------------------------------------------------------------------------

    /**
     * Gets the pathname to the working directory.
     * @return the pathname
     */
    public String getWorkDirectory()
    {
        return workDir;
    }
    
//-----------------------------------------------------------------------------

    /**
     * Gets the pathname to the working directory.
     * @param pathname the new value of the working directory pathname.
     */
    public void setWorkDirectory(String pathname)
    {
        this.workDir = pathname;
    }

//-----------------------------------------------------------------------------

    /**
     * Gets the pathname to the log file.
     * @return
     */
    public String getLogFilePathname()
    {
        return logFile;
    }
    
//-----------------------------------------------------------------------------

    /**
     * Sets the pathname to the log file.
     * @param pathname the new value of pathname to the log file.
     */
    public void setLogFilePathname(String pathname)
    {
        this.workDir = pathname;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Get the name of the program specific logger.
     */
    public Logger getLogger()
    {
        return logger;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Set the name of the program specific logger. This method should only be 
     * used by subclasses that need to set the logger for embedded parameters 
     * collections.
     * @param logger the new logger.
     */
    private void setLogger(Logger logger)
    {
        this.logger = logger;
        for (RunTimeParameters innerParams : otherParameters.values())
        {
            innerParams.setLogger(logger);
        }
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Starts a logger with the given name. 
     * The name is saved among the parameters and the logger can be obtained 
     * from static {@link Logger#getLogger(String)} method using the value of 
     * <code>loggerIdentifier</code> or by {@link RunTimeParameters#getLogger()}.
     * All parameter collectors embedded in this one will inherit the logger.
     * By default the logger that is created does dump its log into the 
     * pathname identified by the {@link RunTimeParameters#logFile} field of
     * this instance.
     * @param loggerIdentifier the string identifying the program-specific logger.
     * @throws IOException 
     * @throws SecurityException 
     */
    public Logger startProgramSpecificLogger(String loggerIdentifier) 
            throws SecurityException, IOException
    { 
        return startProgramSpecificLogger(loggerIdentifier, true);
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Starts a logger with the given name. 
     * The name is saved among the parameters and the logger can be obtained 
     * from static {@link Logger#getLogger(String)} method using the value of 
     * <code>loggerIdentifier</code> or by {@link RunTimeParameters#getLogger()}.
     * All parameter collectors embedded in this one will inherit the logger.
     * @param loggerIdentifier the string identifying the program-specific logger.
     * @param toLogFile with <code>true</code> we dump the log to a file. The
     * pathname of such file must have been configured in this collection of
     * parameters as the {@link RunTimeParameters#logFile} field. With 
     * <code>false</code> we log on standard output.
     * @throws IOException 
     * @throws SecurityException 
     */
    public Logger startProgramSpecificLogger(String loggerIdentifier, 
            boolean toLogFile) 
            throws SecurityException, IOException
    {
        logger = Logger.getLogger(loggerIdentifier);
        
        int n = logger.getHandlers().length;
        for (int i=0; i<n; i++)
        {
            logger.removeHandler(logger.getHandlers()[0]);
        }

        if (toLogFile)
        {
            FileHandler fileHdlr = new FileHandler(logFile);
            SimpleFormatter formatterTxt = new SimpleFormatter();
            fileHdlr.setFormatter(formatterTxt);
            logger.setUseParentHandlers(false);
            logger.addHandler(fileHdlr);
            logger.setLevel(Level.INFO);
            String header = "Started logging for " + loggerIdentifier;
            logger.log(Level.INFO,header);
        } else {
            logger.addHandler(new StreamHandler(System.out, 
                    new SimpleFormatter()));
        }
        
        if (verbosity!=0)
        {
            logger.setLevel(verbosityTologLevel());
            for (int iH=0; iH<logger.getHandlers().length; iH++)
            {
                logger.getHandlers()[iH].setLevel(verbosityTologLevel());
            }
        }
        
        for (RunTimeParameters innerParams : otherParameters.values())
        {
            innerParams.setLogger(logger);
        }
        
        return logger;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Starts a program-specific logger that prints to System.err stream.
     * @param loggerIdentifier
     * @return
     */
    public Logger startConsoleLogger(String loggerIdentifier)
    {
        logger = Logger.getLogger(loggerIdentifier);
        
        int n = logger.getHandlers().length;
        for (int i=0; i<n; i++)
        {
            logger.removeHandler(logger.getHandlers()[0]);
        }
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter());
        handler.setLevel(Level.INFO);
        logger.addHandler(handler);
        
        if (verbosity!=0)
        {
            logger.setLevel(verbosityTologLevel());
            for (int iH=0; iH<logger.getHandlers().length; iH++)
            {
                logger.getHandlers()[iH].setLevel(verbosityTologLevel());
            }
        }
        
        for (RunTimeParameters innerParams : otherParameters.values())
        {
            innerParams.setLogger(logger);
        }
        
        return logger;
    }
    
//-----------------------------------------------------------------------------

    /**
     * Reads a string searching for any common way to say either yes/true 
     * (including shorthand t/y) or no/false (including shorthand f/n either).
     * This method is case insensitive, and the string is trimmed.
     * @param s the string to interpret.
     * @return <code>true</code> for 'true/yes'.
     */
    public static boolean readYesNoTrueFalse(String s)
    {
        boolean result = false;
        String value = s.trim().toUpperCase();
        if (value.equals("YES") 
                || value.equals("Y")
                || value.equals("T")
                || value.equals("TRUE"))
        {
            result = true;
        } else if (value.equals("NO") 
                || value.equals("N")
                || value.equals("F")
                || value.equals("FALSE"))
        {
            result = false;   
        }
        return result;
    }
    
//-----------------------------------------------------------------------------
    
    private Level verbosityTologLevel()
    {
        int rebased = verbosity+3;
        switch (rebased)
        {
            case 0:
                return Level.OFF;
            case 1:
                return Level.SEVERE;
            case 2:
                return Level.WARNING;
            case 3:
                return Level.INFO;
            case 4:
                return Level.FINE;
            case 5:
                return Level.FINER;
            case 6:
                return Level.FINEST;
            default:
                // NB: Level.ALL does not actually allow all log. Don't use it.
                if (rebased>6)
                    return Level.FINEST;
                else
                    return Level.OFF;
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the level of verbosity, i.e., the amount of log that we want to 
     * print.
     * @return the level of verbosity.
     */
    public int getVerbosity()
    {
	    return verbosity;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Set the level of verbosity. If any associated logger exists, the level is
     * set accordingly. The change affects all embedded sets of parameters.
     * The integer is translated in a {@link Level}
     * so that -3 (or lower) corresponds to {@link Level#OFF},
     * 0 is the normal {@link Level#INFO},
     * and 3 (or higher) corresponds to {@link Level#FINEST}. 
     */
    public void setVerbosity(int l)
    {
        this.verbosity = l;
        if (logger!=null)
        {
            logger.setLevel(verbosityTologLevel());
            for (int iH=0; iH<logger.getHandlers().length; iH++)
            {
                logger.getHandlers()[iH].setLevel(verbosityTologLevel());
            }
        }
        
        for (RunTimeParameters innerParams : otherParameters.values())
        {
            innerParams.setVerbosity(l);
        }
    }

//-----------------------------------------------------------------------------
    
    /**
     * Returns the current program-specific randomizer. If no such tool has been 
     * configured, then it creates one using a new and uncontrollable random
     * seed.
     * @return the current and program-specific tool for random number and 
     * random decision generation.
     */
    public Randomizer getRandomizer()
    {
        if (rng==null)
        {
            for (RunTimeParameters innerParams : otherParameters.values())
            {
                if (innerParams.rng!=null)
                {
                    rng = innerParams.rng;
                    return innerParams.rng;
                }
            }
            
            rng = new Randomizer();
            for (RunTimeParameters innerParams : otherParameters.values())
            {
                innerParams.setRandomizer(rng);
            }
        }
        return rng;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Returns the seed 
     * @return
     */
    public long getRandomSeed()
    {
        return rng.getSeed();
    }

//-----------------------------------------------------------------------------
    
    /**
     * Sets the randomizer. This method should only be 
     * used by subclasses that need to set the randomizer in embedded parameters 
     * collections.
     */
    public void setRandomizer(Randomizer rng)
    {
        this.rng = rng;
        for (RunTimeParameters innerParams : otherParameters.values())
        {
            innerParams.setRandomizer(rng);
        }
    }

//-----------------------------------------------------------------------------
    
    /**
     * Starts a program specific randomizer, i.e., a tool for generating
     * random numbers and taking random decisions. 
     * @param seed the random seed.
     */
    public Randomizer startRandomizer()
    {
        rng = new Randomizer();
        for (RunTimeParameters innerParams : otherParameters.values())
        {
            innerParams.setRandomizer(rng);
        }
        return rng;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Starts a program specific randomizer, i.e., a tool for generating
     * random numbers and taking random decisions. 
     * @param seed the random seed.
     */
    public Randomizer startRandomizer(long seed)
    {
        rng = new Randomizer(seed);
        for (RunTimeParameters innerParams : otherParameters.values())
        {
            innerParams.setRandomizer(rng);
        }
        return rng;
    }
    
//-----------------------------------------------------------------------------

    /**
     * Read the parameter TXT file line by line and interpret its content.
     * @param infile
     * @throws DENOPTIMException
     */
    public void readParameterFile(String infile) throws DENOPTIMException
    {
        String line;
        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(infile));
            while ((line = br.readLine()) != null)
            {
                readParameterLine(line);
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
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * @param line the line to try to interpret.
     * @throws DENOPTIMException if the parameter could be read but not 
     * interpreted, i.e., any wrong format of syntax.
     */
    public void readParameterLine(String line) throws DENOPTIMException
    {
        if ((line.trim()).length() == 0)
            return;

        if (line.startsWith("#")) //commented out lines
            return;
        
        if (line.toUpperCase().startsWith(paramType.keywordRoot))
        {
            interpretKeyword(line);
        } else {
            for (ParametersType parType : ParametersType.values())
            {
                if (line.toUpperCase().startsWith(parType.keywordRoot))
                {
                    if (!otherParameters.containsKey(parType))
                    {
                        RunTimeParameters otherParams = 
                                RunTimeParameters.getInstanceFor(parType);
                        otherParams.isMaster = false;
                        otherParameters.put(parType, otherParams);
                    }
                    otherParameters.get(parType).interpretKeyword(line);
                }
            }
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Builds the implementation of this class suitable to allocate parameters
     * of the given type.
     * @param paramType the type of parameters to allocate in the instance to
     * create.
     * @return the instance of the proper type.
     */
    private static RunTimeParameters getInstanceFor(ParametersType paramType)
    {
        RunTimeParameters instance = null;
        try
        {
            Constructor<?> c = paramType.getImplementation().getConstructor();
            instance = (RunTimeParameters) c.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            // This should never happen because the constructor has to be there.
        }
        return instance;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @param type the type of parameter to search for.
     * @return <code>true</code> if the parameter is found among those 
     * collections of embedded parameters that are contained in this instance.
     */
    public boolean containsParameters(ParametersType type)
    {
        return otherParameters.containsKey(type);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @param type the type of embedded parameters to search for.
     * @return the requested parameter, or null.
     */
    public RunTimeParameters getParameters(ParametersType type)
    {
        return otherParameters.get(type);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @param otherParams the parameters to add/set as other in this collection.
     */
    public void setParameters(RunTimeParameters otherParams)
    {
        otherParams.isMaster = false;
        otherParameters.put(otherParams.paramType, otherParams);
    }
    
//------------------------------------------------------------------------------
    
// TODO: consider this mechanism for retrieving parameters once all parameters
//       will be of type Parameter.
//
//    /**
//     * @param type the type of embedded parameters to search for.
//     * @return the requested parameter, or null.
//     */
//    public RunTimeParameters getParameter(ParametersType type, 
//            String parName)
//    {
//        if (!containsParameters(type))
//        {
//            otherParams.isMaster = false;
//            otherParameters.put(type, getInstanceFor(type)); 
//        }
//
//        return otherParameters.get(type).get(parName);
//    }
//    
////----------------------------------------------------------------------------
//    
//    public Parameter get(String parameterName)
//    {
//        return ...;
//    }

//-----------------------------------------------------------------------------

    /**
     * Processes a string looking for keyword and a possibly associated value.
     * @param line the string to parse
     * @throws DENOPTIMException
     */
    public void interpretKeyword(String line) throws DENOPTIMException
    {
        String key = line.trim();
        String value = "";
        if (line.contains("="))
        {
            key = line.substring(paramType.keywordRoot.length(),
                    line.indexOf("=") + 1).trim();
            value = line.substring(line.indexOf("=") + 1).trim();
        } else {
            key = line.substring(paramType.keywordRoot.length());
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
     * @throws DENOPTIMException is the parameter cannot be configured from the 
     * given value.
     */
    public abstract void interpretKeyword(String key, String value) 
            throws DENOPTIMException;
    
//-----------------------------------------------------------------------------

    /**
     * Evaluate consistency of input parameters.
     * @throws DENOPTIMException
     */
    public abstract void checkParameters() throws DENOPTIMException;

//------------------------------------------------------------------------------

    /**
     * Checks any of the parameter collections contained in this instance.
     * @throws DENOPTIMException
     */
    protected void checkOtherParameters() throws DENOPTIMException
    {
        for (RunTimeParameters otherCollector : otherParameters.values())
        {
            otherCollector.checkParameters();
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Processes all parameters and initialize related objects.
     * @throws DENOPTIMException
     */
    public abstract void processParameters() throws DENOPTIMException;
        
//------------------------------------------------------------------------------

    /**
     * Processes any of the parameter collections contained in this instance.
     * @throws DENOPTIMException
     */
    protected void processOtherParameters() throws DENOPTIMException
    {
        for (RunTimeParameters otherCollector : otherParameters.values())
        {
            otherCollector.processParameters();
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the list of parameters in a string with newline characters as
     * delimiters.
     * @return the list of parameters in a string with newline characters as
     * delimiters.
     */
    //NB: this must be abstract because the superclass cannot look into the
    // private fields of the subclass. When the parameters will be stored as
    // objects rather then fields, then we should be able to have the 
    // public String getPrintedList only in this class.
    public abstract String getPrintedList();
    /*
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
    */
    
//----------------------------------------------------------------------------

    /**
     * Print all parameters. 
     */

    public void printParameters()
    {
        StringBuilder sb = new StringBuilder(1024);
        sb.append(getPrintedList()).append(NL);
        sb.append("-------------------------------------------"
                + "----------------------").append(NL);
        logger.log(Level.INFO,sb.toString());
    }

  //------------------------------------------------------------------------------

    /**
     * Ensures a pathname is not empty nor null and that it does lead to an 
     * existing file or triggers an error.
     * This is meant for checking initialization settings and does not print in
     * the program specific log file.
     */
    protected void ensureFileExistsIfSet(String pathname)
    {
        if (pathname == null || pathname.isBlank())
            return;
                    
        if (!FileUtils.checkExists(pathname))
        {
            String msg = "ERROR! File '" + pathname + "' not found!";
            throw new Error(msg);
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Ensures a pathname does lead to an existing file or triggers an error.
     * This is meant for checking initialization settings and does not print in
     * the program specific log file.
     */
    protected void ensureFileExists(String pathname)
    {
        if (!FileUtils.checkExists(pathname))
        {
            String msg = "ERROR! File '" + pathname + "' not found!";
            throw new Error(msg);
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Ensures that a parameter is not null or triggers an error.
     * This is meant for checking initialization settings and does not print in
     * the program specific log file.
     */
    protected void ensureNotNull(String paramName, String param, String paramKey)
    {
        if (param == null)
        {
            String msg = "ERROR! Parameter '" + paramName + "' is null! "
                + "Please, add '" + paramType.keywordRoot + paramKey 
                + "' to the input parameters.";
            throw new Error(msg);
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Ensures that a parameter is a positive number (x>=0) or triggers an error.
     * This is meant for checking initialization settings and does not print in
     * the program specific log file.
     */
    protected void ensureIsPositive(String paramName, int value, String paramKey)
    {
        if (value <= 0)
        {
            String msg = "ERROR! Parameter '" + paramName + "' not striktly "
                + "positive (" + value + "). "
                + "Please, use a positive value for '" + paramType.keywordRoot 
                + paramKey + "'.";
            throw new Error(msg);
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Ensures that a parameter is a positive number (x>=0) or triggers an error.
     * This is meant for checking initialization settings and does not print in
     * the program specific log file.
     */
    protected void ensureIsPositiveOrZero(String paramName, int value, 
            String paramKey)
    {
        if (value < 0)
        {
            String msg = "ERROR! Parameter '" + paramName + "' is negative ("
                + value + "). "
                + "Please, use a positive value for '" + paramType.keywordRoot 
                + paramKey + "'.";
            throw new Error(msg);
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Ensures that a parameter is within a range or triggers an error.
     * This is meant for checking initialization settings and does not print in
     * the program specific log file.
     */
    protected void ensureInRange(String paramName, int value, int min, int max, 
            String paramKey)
    {
        if (max < value || min > value)
        {
            String msg = "ERROR! Parameter '" + paramName + "' is not in range"
                + min + " < x < " + max + " (value: " + value + "). "
                + "Please, correct the value of '" + paramType.keywordRoot 
                + paramKey + "'.";
            throw new Error(msg);
        }
    }

//----------------------------------------------------------------------------

}
