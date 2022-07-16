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

package denoptim.main;

import java.awt.EventQueue;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.ExceptionUtils;
import denoptim.files.FileFormat;
import denoptim.files.FileUtils;
import denoptim.gui.GUI;
import denoptim.logging.Version;
import denoptim.programs.combinatorial.FragSpaceExplorer;
import denoptim.programs.denovo.GARunner;
import denoptim.programs.fitnessevaluator.FitnessRunner;
import denoptim.programs.fragmenter.Fragmenter;
import denoptim.programs.genetweeker.GeneOpsRunner;
import denoptim.programs.grapheditor.GraphEditor;
import denoptim.programs.graphlisthandler.GraphListsHandler;
import denoptim.programs.isomorphism.Isomorphism;
import denoptim.programs.moldecularmodelbuilder.MolecularModelBuilder;
import denoptim.task.ProgramTask;
import denoptim.task.StaticTaskManager;

/**
 * Entry point of any kind of run of the denoptim program.
 */
public class Main
{
    /**
     * Types of runs that can be requested to the denoptim Main class.
     */
    public static enum RunType {
        /**
         * Launch the graphical user interface {@link denoptim.gui.GUI}
         */
        GUI, 
        
        /**
         * Run the genetic algorithm with {@link DenoptimGA}
         */
        GA, 
        
        /**
         * Run a combinatorial generation of candidates with 
         * {@link FragSpaceExplorer}.
         */
        FSE, 
        
        /**
         * stand-alone fitness evaluation
         */
        FIT,
        
        /**
         * Run the stand-alone conversion of graph into a three-dimensional 
         * molecular model
         */
        B3D,
        
        /**
         * Only prints help or version and close program.
         */
        DRY,
        
        /**
         * Run a stand-alone genetic operation
         */
        GO,
        
        /**
         * Run a stand-alone graph editing task
         */
        GE,
        
        /**
         * Run a stand-alone test for graph isomorphism
         */
        GI,
        
        /**
         * Run a comparison of lists of graphs.
         */
        CLG,
        
        /**
         * Run a fragmentation task
         */
        FRG;
        
        // NB: to define a new run type: 
        //  1) add the enum alternative. The order is somewhat related to the
        //     importance (i.e., common use) of a run type.
        //  2) set the value of "description" in the static block below
        //  3) set the value of "isCLIEnabled" in the static block below
        //  4) set the value of "programTaskImpl" in the static block below
        //  5) consider whether a new parameter file format should be added
        //     in FileFormats.
        
        /**
         * The implementation of {@link ProgramTask} capable of this run type.
         */
        private Class<?> programTaskImpl;
        
        /**
         * A short description of the run type.
         */
        private String description = "";
        
        /**
         * Flag indicating if this run type can be called from CLI.
         */
        private boolean isCLIEnabled = true;
        
        static {
            DRY.description = "Dry run";
            FSE.description = "combinatorial Fragment Space Exploration";
            FIT.description = "stand-alone FITness evaluation";
            GA.description = "Genetic Algorithm";
            GE.description = "stand-alone Graph Editing task";
            GI.description = "stand-alone Graph Isomorphism analysis";
            GO.description = "stand-alone Genetic Operation";
            GUI.description = "Graphycal User Interface";
            CLG.description = "Comparison of Lists of Graphs";
            B3D.description = "stand-alone build a 3D molecular model from a "
                    + "graph";
            FRG.description = "Fragmentation and fragment managment.";
            
            DRY.isCLIEnabled = false;
            FSE.isCLIEnabled = true;
            FIT.isCLIEnabled = true;
            GA.isCLIEnabled = true;
            GE.isCLIEnabled = true;
            GI.isCLIEnabled = true;
            GO.isCLIEnabled = true;
            GUI.isCLIEnabled = false;
            CLG.isCLIEnabled = true;
            B3D.isCLIEnabled = true;
            FRG.isCLIEnabled = true;
            
            DRY.programTaskImpl = null;
            FSE.programTaskImpl = FragSpaceExplorer.class;
            FIT.programTaskImpl = FitnessRunner.class;
            GA.programTaskImpl = GARunner.class;
            GE.programTaskImpl = GraphEditor.class;
            GI.programTaskImpl = Isomorphism.class;
            GO.programTaskImpl = GeneOpsRunner.class;
            GUI.programTaskImpl = GUI.class;
            CLG.programTaskImpl = GraphListsHandler.class;
            B3D.programTaskImpl = MolecularModelBuilder.class;
            FRG.programTaskImpl = Fragmenter.class;
        }

        /**
         * Returns the class that implements the program with this type of run.
         * @return the class that implements the program with this type of run.
         */
        public Class<?> getProgramTaskImpl()
        {
            return programTaskImpl;
        }
        
        /**
         * Returns a string that contains a textual list (e.g., "A, B, and C")
         * of the possible types that can be requested by a user.
         * @return a textual list (e.g., "A, B, and C").
         */
        public static String getRunTypesForUser()
        {
            String s = "";
            String separator = DENOPTIMConstants.EOL;
            String last = ""; //in case the last entry should use " and "
            for (int i=0; i<RunType.values().length; i++)
            {
                RunType rt = RunType.values()[i];
                if (!rt.isCLIEnabled)
                    continue; //This run type should not be visible by the user
                if (i>0 && i < RunType.values().length-2)
                    s = s + " '" + rt.toString() + "' for "+rt.description+ ",";
                else if (i==0)
                    s = "'" + rt.toString() + "' for "+rt.description+",";
                else
                    s = s + last+"'"+rt.toString()+"' for "+rt.description+".";
                s = s + separator;
            }
            return s;
        }

        /**
         * Returns <code>true</code> if this run type can be requested from the 
         * CLI.
         * @return <code>true</code> if this run type can be requested from the 
         * CLI.
         */
        boolean isCLIEnabled()
        {
            return isCLIEnabled;
        }
    };
    
//------------------------------------------------------------------------------
    
    /**
     * Launches the appropriate program according to the arguments given. Use
     * "-h" to print usage instructions.
     * @param args the list of arguments.
     */
    
    public static void main(String[] args)
    {
        // First, we try to understand what the user wants to do.
        Behavior behavior = defineProgramBehavior(args);
        
        // In case of inconsistent requests, we report the error and stop
        if (behavior.exitStatus!=0)
            reportError(behavior);
        
        // Deal with simple requests for information
        if (RunType.DRY.equals(behavior.runType))
        {
            System.out.println(behavior.helpMsg);
            System.exit(0);
        }
        
        List<String> inputFiles = behavior.cmd.getArgList();
        
        // We instantiate also the task manager, even if it might not be used.
        // This is to pre-start the tasks and get a more reliable queue status
        // at any given time after this point.
        StaticTaskManager.getInstance();
        
        // Now, we deal with proper program runs
        if (ProgramTask.class.isAssignableFrom(
                behavior.runType.getProgramTaskImpl()))
        {
            if (inputFiles.size()>1)
            {    
                reportError("Only one input file allowed when requesting run "
                        + behavior.runType + ". Found " + inputFiles.size()
                        + " files: " + inputFiles, 1);
            }
            File inpFile = new File(inputFiles.get(0));
            File wDir = inpFile.getParentFile();
            if (wDir==null)
            {
                wDir = new File(System.getProperty("user.dir"));
            }
            runProgramTask(behavior.runType.getProgramTaskImpl(), inpFile, wDir);
            terminate();
        } else if (RunType.GUI.equals(behavior.runType)) {
            EventQueue.invokeLater(new GUI(behavior.cmd));
        } else {
            reportError("You requested a run of type '" + behavior.runType 
                    + "', but I found no such implementation. "
                    + "Please, report this to the authors.", 1);
        }
    }

//------------------------------------------------------------------------------
    
    /**
     * Creates a task for the given class.
     * @param taskClass
     * @param inputFile
     * @param workDir
     * @throws SecurityException 
     * @throws NoSuchMethodException 
     */
    private static void runProgramTask(Class<?> taskClass, File inputFile,
            File workDir)
    {
        if (!ProgramTask.class.isAssignableFrom(taskClass))
        {
            reportError("Attempt to create a program task for class '" 
                    + taskClass.getSimpleName() + "', but such class is not a "
                    + "extension of '" + ProgramTask.class.getSimpleName() 
                    + "'.", 1);
        }

        ProgramTask task = null;
        try
        {
            Constructor<?> taskConstructor = taskClass.getConstructor(File.class, 
                    File.class);
            task = (ProgramTask) taskConstructor.newInstance(inputFile, 
                    workDir);
        } catch (Exception e)
        {
            reportError("Could not create a program task for " 
                    + taskClass.getSimpleName() + DENOPTIMConstants.EOL
                    + ". Details: " + DENOPTIMConstants.EOL
                    + ExceptionUtils.getStackTraceAsString(e), 1);
        }
        
        try
        {
            StaticTaskManager.submitAndWait(task);
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        } catch (ExecutionException e)
        {
            e.printStackTrace();
        }
    }

//------------------------------------------------------------------------------
    
    /**
     * Checks the file exists and format is recognized.
     * @param file file to check.
     * @param behavior this is where we write error messages.
     */
    private static Behavior ensureFileExistsAndIsReadable(File file, 
            Behavior behavior)
    {
        if (!file.exists())
        {
            return new Behavior(behavior.runType, behavior.cmd, 1, null,
                    "File '"+ file.getAbsolutePath() +"' not found.");
        }
        FileFormat format = FileFormat.UNRECOGNIZED;
        try {
            format = FileUtils.detectFileFormat(file);
        } catch (Throwable t) {
            // Ignore: if anything goes wrong well still get UNRECOGNIZED which
            // triggers the error.
        }
        if (FileFormat.UNRECOGNIZED.equals(format))
        {
            return new Behavior(behavior.runType, behavior.cmd, 1, null,
                    "Could not open file '" + file.getAbsolutePath() 
                    + "' because its format is not recognized.");
        }
        return behavior;
    }

//------------------------------------------------------------------------------
    
    /**
     * Does the processing of the application arguments and decides what the
     * program should do. Note that this method already checks if the pathname
     * given for the {@link CLIOptions#input} option is valid.
     * @param args the arguments to process
     * @return the behavior of the program as inferred from the given arguments.
     */
    protected static Behavior defineProgramBehavior(String[] args)
    {   
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        
        try {
            cmd = parser.parse(CLIOptions.getInstance(), args);
        } catch (ParseException e) {
            String helpMsg = getHelpString();
            String errMsg = "Unable to parse command-line arguments. "
                    + "Please, check your input! " + DENOPTIMConstants.EOL
                    + "Details: " + e.getMessage();
            return new Behavior(null, null, 1, helpMsg, errMsg);
        }
        
        if (cmd.hasOption(CLIOptions.help))
        {
            String helpMsg = getHelpString();
            return new Behavior(RunType.DRY, null, 0, helpMsg, null);
        }
        
        if (cmd.hasOption(CLIOptions.version))
        {
            return new Behavior(RunType.DRY, null, 0, Version.VERSION, null);
        } 
        
        Behavior result = null;
        if (cmd.getOptions().length==0)
            result = new Behavior(RunType.GUI, cmd, 0, null, null);
        else {
            if (cmd.hasOption(CLIOptions.run))
            {
                String rts = cmd.getOptionValue(CLIOptions.run).toString();
                RunType rt = null;
                try
                {
                    rt = RunType.valueOf(rts.toUpperCase());
                } catch (Exception e)
                {
                    String errMsg = "Unacceptable value for "
                            + CLIOptions.run.getLongOpt() + " option: "
                            + "'" + rts + "'. Please, modify "
                            + "your command. ";
                    return new Behavior(null, null, 1, null, errMsg);
                }
            
                if (rt.isCLIEnabled())
                {
                    result = new Behavior(rt, cmd);
                } else {
                    result = new Behavior(null, null, 1, null, 
                            "RunType '"+rt+"' is not enabled from CLI. "
                            + "Please, contacts the developers.");
                }
            } else {
                reportError("Command line has no " + CLIOptions.run + " option, "
                        + "but more than zero options. Please, contact the "
                        + "developers.", 1);
            }
        }

        List<String> inputFiles = cmd.getArgList();
        for (String pathname : inputFiles)
            result = ensureFileExistsAndIsReadable(new File(pathname), result);
        
        return result;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Prints the help message on a string.
     * @return the string ready to be printed.
     */
    private static String getHelpString()
    {   
        HelpFormatter formatter = new HelpFormatter();
        StringWriter out = new StringWriter();
        PrintWriter pw = new PrintWriter(out);
        
        formatter.printHelp(pw, 80, "denoptim", DENOPTIMConstants.EOL, 
                CLIOptions.getInstance(), 
                formatter.getLeftPadding(), formatter.getDescPadding(), 
                DENOPTIMConstants.EOL +
                "Run without arguments to launch the graphical user "
                + "interface (GUI) without opening any file. ", true);
        pw.flush();
        return out.toString();
    }
    
//------------------------------------------------------------------------------

    /**
     * Prints an error message on standard output and then stops the Virtual 
     * Machine with an exit code.
     * @param msg the message explaining the type of error. This string will be
     * printed after "ERROR! ". 
     * @param exitCode the exit code for the virtual machine.
     */
    private static void reportError(String msg, int exitCode)
    {
        System.out.println(DENOPTIMConstants.EOL
                + "ERROR! " + msg 
                + DENOPTIMConstants.EOL);
        System.exit(exitCode);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Prints an error message on standard output, possibly after the help
     * message, and then stops the Virtual Machine with an exit code.
     */
    private static void reportError(Behavior behavior)
    {
        if (behavior.helpMsg!=null)
            System.out.println(behavior.helpMsg);
        reportError(behavior.errorMsg,behavior.exitStatus);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Stops services and halts the Virtual Machine.
     */
    private static void terminate()
    {
        try {
            StaticTaskManager.stopAll();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("StaticTaskManager had problems stopping. "
                    + "Forcing termination.");
        }
        Runtime.getRuntime().halt(0);
    }
    
//------------------------------------------------------------------------------

}
