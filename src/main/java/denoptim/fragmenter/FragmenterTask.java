/*
 *   DENOPTIM
 *   Copyright (C) 2022 Marco Foscato <marco.foscato@uib.no>
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

package denoptim.fragmenter;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import denoptim.io.DenoptimIO;
import denoptim.programs.fragmenter.FragmenterParameters;
import denoptim.task.Task;
import denoptim.utils.TaskUtils;

/**
 * Task that performs the various steps in the process that prepares chemical 
 * structured to be chopped, chops them, and post-process the resulting 
 * fragments. Each of the steps may or may not be part of the actual workflow,
 * depending on the configurations given upon construction of the task.
 * Note that preliminary adaptation of the molecular representations is
 * expected to be done before calling this task. So the systems to fragment
 * should have been already be processed by 
 * {@link FragmenterTools#prepareMolToFragmentation(org.openscience.cdk.interfaces.IAtomContainer, FragmenterParameters, int)}
 * before starting this task.
 */

public class FragmenterTask extends Task
{   
    /**
     * File containing the input for this task (i.e., a structure file)
     */
    private File inputFile;
    
    /**
     * File containing the latest results, though not the final results. For
     * instance, the results of an intermediate step in the task.
     */
    private File preliminaryResults;
    
    /**
     * The data structure holding the results of this task.
     */
    protected String results = null;
   
    /**
     * Settings for the calculation of the fitness
     */
    protected FragmenterParameters settings;
    
    /**
     * Logger for this task.
     */
    private Logger logger = null;
    
    /**
     * Pathname to thread-specific log.
     */
    private String logFilePathname = "unset";

//------------------------------------------------------------------------------
    
    /**
     * Create a task by giving the input file (i.e., containing structures to 
     * work with) and the configuration of the task. Note  we start a 
     * task-specific logger that prints to a task-specific file.
     * @param inputFile the source of structures to work with.
     * @param settings the configuration of the task.
     * @param id identifier of the thread running this task.
     * @throws SecurityException
     * @throws IOException
     */
    public FragmenterTask(File inputFile, FragmenterParameters settings,
            int id) throws SecurityException, IOException
    {
    	super(TaskUtils.getUniqueTaskIndex());
    	this.id = id;
    	this.inputFile = inputFile;
    	this.settings = settings;
    	this.logger = Logger.getLogger("FragmenterTask-"+id);
    	
    	//Create the task-specific logger
        int n = logger.getHandlers().length;
        for (int i=0; i<n; i++)
        {
            logger.removeHandler(logger.getHandlers()[0]);
        }

        this.logFilePathname = getLogFileName(settings,id);
        FileHandler fileHdlr = new FileHandler(logFilePathname);
        SimpleFormatter formatterTxt = new SimpleFormatter();
        fileHdlr.setFormatter(formatterTxt);
        logger.setUseParentHandlers(false);
        logger.addHandler(fileHdlr);
        logger.setLevel(settings.getLogger().getLevel());
        String header = "Started logging for FragmenterTask-"+id ;
        logger.log(Level.INFO,header);
    }
    
//------------------------------------------------------------------------------

    /**
     * Builds the pathname of the structure file meant to be the input for this
     * task.
     * @param settings settings we work with.
     * @param i the index of the thread
     * @return the pathname
     */
    static String getInputFileName(FragmenterParameters settings, int i)
    {
        return settings.getWorkDirectory() + DenoptimIO.FS 
                + "structuresBatch-" + i + ".sdf";
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return the pathname of the log file.
     */
    public String getLogFilePathname()
    {
        return logFilePathname;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return the pathname to the file collecting results
     */
    public String getResultFile()
    {
        return results;
    }
    
//------------------------------------------------------------------------------

    /**
     * Builds the pathname of the structure file meant to hold structures
     * that survive the comparison of structure's elemental analysis against the
     * declared molecular formula.
     * @param settings settings we work with.
     * @param i the index of the thread
     * @return the pathname
     */
    static String getConfirmedFormulaFileName(FragmenterParameters settings, int i)
    {
        return settings.getWorkDirectory() + DenoptimIO.FS 
                + "structuresNoMissingAtoms-" + i + ".sdf";
    }
    
//------------------------------------------------------------------------------

    /**
     * Builds the pathname of the structure file meant to hold structures
     * that survive the pre-filtering step, if any.
     * @param settings settings we work with.
     * @param i the index of the thread
     * @return the pathname
     */
    static String getPreFilteredFileName(FragmenterParameters settings, int i)
    {
        return settings.getWorkDirectory() + DenoptimIO.FS 
                + "structuresPreFiltered-" + i + ".sdf";
    }
    
//------------------------------------------------------------------------------

    /**
     * Builds the pathname of the structure file meant to hold fragments 
     * resulting from this task.
     * @param settings settings we work with.
     * @param i the index of the thread
     * @return the pathname
     */
    static String getFragmentsFileName(FragmenterParameters settings, int i)
    {
        return settings.getWorkDirectory() + DenoptimIO.FS 
                + "Fragments-" + i + ".sdf";
    }
    
//------------------------------------------------------------------------------

    /**
     * Builds the pathname of the structure file meant to hold fragments 
     * resulting from all tasks.
     * @param settings settings we work with.
     * @return the pathname
     */
    static String getFragmentsFileName(FragmenterParameters settings)
    {
        return settings.getWorkDirectory() + DenoptimIO.FS 
                + "Fragments.sdf";
    }
    
//------------------------------------------------------------------------------

    /**
     * Builds the pathname of the structure file meant to hold results that
     * are not necessarily fragments.
     * @param settings settings we work with.
     * @return the pathname
     */
    static String getResultsFileName(FragmenterParameters settings)
    {
        return settings.getWorkDirectory() + DenoptimIO.FS 
                + "Results.sdf";
    }
    
//------------------------------------------------------------------------------

    /**
     * Builds the pathname of the log file for this task.
     * @param settings settings we work with.
     * @param i the index of the thread
     * @return the pathname
     */
    private static String getLogFileName(FragmenterParameters settings, int i)
    {
        return settings.getWorkDirectory() + DenoptimIO.FS 
                + "FragmenterTask-" + i + ".log";
    }

//------------------------------------------------------------------------------
    
    /**
     * Performs the whatever work has to be done by this task.
     */
 
    @Override
    public Object call() throws Exception
    {
        // Preliminary check for missing atoms by elemental analysis
        if (settings.doCheckFormula())
        {
            logger.log(Level.INFO,"Starting elemental analysis");
            File newResultsFile = new File(getConfirmedFormulaFileName(settings,
                    id));
            FragmenterTools.checkElementalAnalysisAgainstFormula(inputFile,
                    newResultsFile, logger);
            preliminaryResults = newResultsFile;
        } else {
            preliminaryResults = inputFile;
        }

        // Pre-fragmentation filter
        if (settings.doPreFilter())
        {
            logger.log(Level.INFO,"Pre-filtering structures");
            File newResultsFile = new File(getPreFilteredFileName(settings, id));
            FragmenterTools.filterStrucutresBySMARTS(inputFile, 
                    settings.getPreFiltrationSMARTS(), newResultsFile, logger);
            preliminaryResults = newResultsFile;
        }

        // Fragmentation of structures
        if (settings.doFragmentation())
        {
            logger.log(Level.INFO,"Fragmentation of structures");
            File newResultsFile = new File(getFragmentsFileName(settings, id));
            boolean producedSomething = FragmenterTools.fragmentation(inputFile, 
                    settings, newResultsFile,logger);
            if (!producedSomething)
            {
                completed = true;
                return null;
            }
            preliminaryResults = newResultsFile;
        }
        
        if (!settings.doFragmentation() && settings.doFiltering())
        {
            logger.log(Level.INFO,"Filtering fragments");
            File newResultsFile = new File(getFragmentsFileName(settings, id));
            FragmenterTools.manageFragmentCollection(inputFile, settings, 
                    newResultsFile, logger);
            preliminaryResults = newResultsFile;
        }
        
        // Final message
        results = preliminaryResults.getAbsolutePath();
        if (settings.getNumTasks()>1)
        {
            logger.log(Level.INFO,"Fragmenter task " + id + " completed.");
        } else {
            logger.log(Level.INFO,"Results available in "+results);
        }
        
        // We stop the logger's file handler to remove the lock file.
        for (Handler h : logger.getHandlers()) 
        {
            if (h instanceof FileHandler) {
                logger.removeHandler(h);
                h.close();
            }
        }
        
        completed = true;
        return results;
    }

//------------------------------------------------------------------------------

}
