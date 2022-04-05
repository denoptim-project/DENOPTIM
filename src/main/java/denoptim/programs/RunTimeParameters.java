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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import denoptim.combinatorial.CEBLParameters;
import denoptim.exception.DENOPTIMException;
import denoptim.files.FileFormat;
import denoptim.files.FileUtils;
import denoptim.fitness.FitnessParameters;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.ga.GAParameters;
import denoptim.graph.DENOPTIMGraph;
import denoptim.graph.rings.RingClosureParameters;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.programs.fitnessevaluator.FRParameters;
import denoptim.programs.genetweeker.GeneOpsRunnerParameters;
import denoptim.programs.grapheditor.GraphEdParameters;
import denoptim.programs.graphlisthandler.GraphListsHandlerParameters;
import denoptim.programs.isomorphism.IsomorphismParameters;
import denoptim.programs.moldecularmodelbuilder.MMBuilderParameters;
import denoptim.task.ProgramTask;


/**
 * Parameters controlling execution of the combinatorial algorithm for 
 * exploration of a fragment space by layer (CEBL).
 * 
 * @author Marco Foscato
 */

public abstract class RunTimeParameters
{
    /**
     * Working directory
     */
    protected String workDir = System.getProperty("user.dir");

    /**
     * Log file
     */
    protected String logFile = "denoptim.log";

    /**
     * Verbosity level
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
         * Parameters pertaining the combinatorial exploration by layer
         */
        CEBL_PARAMS,
        
        /**
         * Parameters pertaining the genetic algorithm
         */
        GA_PARAMS,
        
        /**
         * Parameters pertaining the definition of the fragment space
         */
        FS_PARAMS,
        
        /**
         * Parameters pertaining to ring closures in graphs
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
        ISO_PARAMS;
        
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
            RC_PARAMS.keywordRoot = "RC-";
            FIT_PARAMS.keywordRoot = "FP-";
            FR_PARAMS.keywordRoot = "FR-";
            MMB_PARAM.keywordRoot = "CG-";
            GO_PARAMS.keywordRoot = "TESTGENOPS-";
            GE_PARAMS.keywordRoot = "GRAPHEDIT-";
            GLH_PARAMS.keywordRoot = "GRAPHLISTS-";
            ISO_PARAMS.keywordRoot = "ISOMORPHISM-";
            
            CEBL_PARAMS.implementation = CEBLParameters.class;
            GA_PARAMS.implementation = GAParameters.class;
            FS_PARAMS.implementation = FragmentSpaceParameters.class;
            RC_PARAMS.implementation = RingClosureParameters.class;
            FIT_PARAMS.implementation = FitnessParameters.class;
            FR_PARAMS.implementation = FRParameters.class;
            MMB_PARAM.implementation = MMBuilderParameters.class;
            GO_PARAMS.implementation = GeneOpsRunnerParameters.class;
            GE_PARAMS.implementation = GraphEdParameters.class;
            GLH_PARAMS.implementation = GraphListsHandlerParameters.class;
            ISO_PARAMS.implementation = IsomorphismParameters.class;
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
    
    /**
     * New line character
     */
    protected final String NL = System.getProperty("line.separator");
    
//-----------------------------------------------------------------------------
    
    /**
     * Constructor
     * @param paramType the type of parameters this instance is meant to collect.
     */
    public RunTimeParameters(ParametersType paramType)
    {
        this.paramType = paramType;
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
    public String getLogFileName()
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
     * Returns the level of verbosity, i.e., the amount of log that we want to 
     * print.
     * @return the level of verbosity.
     */
    public int getVerbosity()
    {
	    return verbosity;
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
                if ((line.trim()).length() == 0)
                    continue;

                if (line.startsWith("#")) //commented out lines
                    continue;
                
                if (line.toUpperCase().startsWith(paramType.keywordRoot))
                {
                    interpretKeyword(line);
                    continue;
                } else {
                    for (ParametersType parType : ParametersType.values())
                    {
                        if (line.toUpperCase().startsWith(parType.keywordRoot))
                        {
                            if (!otherParameters.containsKey(parType))
                            {
                                otherParameters.put(parType, 
                                        RunTimeParameters.getInstanceFor(
                                                paramType));
                            }
                            otherParameters.get(parType).interpretKeyword(line);
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
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
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
            Constructor<?> constructor = paramType.getImplementation()
                    .getConstructor(ParametersType.class);
            instance = (RunTimeParameters) constructor.newInstance(paramType);
        } catch (Exception e) {
            e.printStackTrace();
            //This should never happen
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
    public String getPrintedList()
    {
        StringBuilder sb = new StringBuilder(1024);
        sb.append(" " + paramType + " ").append(NL);
        for (Field f : RunTimeParameters.class.getDeclaredFields()) 
        {
            try
            {
                sb.append(f.getName()).append(" = ").append(
                            f.get(RunTimeParameters.class)).append(NL);
            }
            catch (Throwable t)
            {
                sb.append("ERROR! Unable to print " + paramType 
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
    
//----------------------------------------------------------------------------

    /**
     * Print all parameters. 
     */

    public void printParameters()
    {
        StringBuilder sb = new StringBuilder(1024);
        sb.append(getPrintedList());
        sb.append("-------------------------------------------"
                + "----------------------").append(NL);
        DENOPTIMLogger.appLogger.info(sb.toString());
    }
    
//------------------------------------------------------------------------------

    /**
     * Ensures a pathname does lead to an existing file or stops with error
     */

    protected void checkFileExists(String pathname)
    {
        if (!FileUtils.checkExists(pathname))
        {
            System.out.println("ERROR! File '" + pathname + "' not found!");
            System.exit(-1);
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Ensures that a parameter is not null or stops with error message.
     */

    protected void checkNotNull(String paramName, String param, String paramKey)
    {
        if (param == null)
        {
            System.out.println("ERROR! Parameter '" + paramName + "' is null! "
                + "Please, add '" + paramType.keywordRoot + paramKey 
                + "' to the input parameters.");
            System.exit(-1);
        }
    }

//----------------------------------------------------------------------------

}
