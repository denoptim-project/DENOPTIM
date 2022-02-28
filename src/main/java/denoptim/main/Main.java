package denoptim.main;

import java.awt.EventQueue;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.concurrent.ExecutionException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

import denoptim.constants.DENOPTIMConstants;
import denoptim.denoptimga.DenoptimGA;
import denoptim.exception.ExceptionUtils;
import denoptim.files.FileFormat;
import denoptim.files.FileUtils;
import denoptim.fragspaceexplorer.FragSpaceExplorer;
import denoptim.geneopsrunner.GeneOpsRunner;
import denoptim.graphlisthandler.GraphListsHandler;
import denoptim.gui.GUI;
import denoptim.logging.Version;
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
         * Run the genetic algorithm with {@link DenoptimGA}
         */
        GA, 
        
        /**
         * Run a combinatorial generation of candidates with 
         * {@link FragSpaceExplorer}.
         */
        FSE, 
        
        /**
         * Launch the graphical user interface {@link denoptim.gui.GUI}
         */
        GUI, 
        
        /**
         * Only prints help or version and close program.
         */
        DRY,
        
        /**
         * Run a stand-alone genetic operation
         */
        GO,
        
        /**
         * Run a comparison of lists of graphs.
         */
        CLG;
        
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
            GA.description = "Genetic Algorithm";
            GO.description = "stand-alone Genetic Operation";
            GUI.description = "Graphycal User Interface";
            CLG.description = "comparison of graph lists";
            
            DRY.isCLIEnabled = false;
            FSE.isCLIEnabled = true;
            GA.isCLIEnabled = true;
            GO.isCLIEnabled = true;
            GUI.isCLIEnabled = true;
            CLG.isCLIEnabled = true;
            
            DRY.programTaskImpl = null;
            FSE.programTaskImpl = FragSpaceExplorer.class;
            GA.programTaskImpl = DenoptimGA.class;
            GO.programTaskImpl = GeneOpsRunner.class;
            GUI.programTaskImpl = GUI.class;
            CLG.programTaskImpl = GraphListsHandler.class;
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
                if (RunType.DRY.equals(rt))
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
        
        // We instantiate also the task manager, even if it might not be used.
        // This is to pre-start the tasks and get a more reliable queue status
        // at any given time after this point.
        StaticTaskManager.getInstance();
        
        // Now, we deal with proper program runs
        if (ProgramTask.class.isAssignableFrom(
                behavior.runType.getProgramTaskImpl()))
        {
            File inpFile = new File(behavior.cmd.getOptionValue(CLIOptions.input));
            File wDir = inpFile.getParentFile();
            runProgramTask(behavior.runType.getProgramTaskImpl(), inpFile, wDir);
            terminate();
        } else if (RunType.GUI.equals(behavior.runType)) {
            ensureFileIsReadable(behavior.cmd);
            EventQueue.invokeLater(new GUI(behavior.cmd));
        } else {
            reportError("You requested a run of type '" + behavior.runType 
                    + "', but I found no such implementation. "
                    + "Please, report this to the authors.", 1);
        }
    }

//------------------------------------------------------------------------------
    
    /**
     * Creates a task for the given class
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
     * Use this method to avoid launching a GUI in the attempt to open a file 
     * with unrecognized format.
     * @param cmd the command line containing the option 
     * {@link CLIOptions#input}.
     */
    private static void ensureFileIsReadable(CommandLine cmd)
    {
        if (!cmd.hasOption(CLIOptions.input))
            return;
        
        String pathname = cmd.getOptionValue(CLIOptions.input);
        FileFormat format = FileFormat.UNRECOGNIZED;
        try {
            format = FileUtils.detectFileFormat(new File(pathname));
        } catch (Throwable t) {
            // Ignore: if anything goes wrong well still get UNRECOGNIZED which
            // triggers the error.
        }
        if (FileFormat.UNRECOGNIZED.equals(format))
        {
            reportError("Could not open file '" + pathname + "' because its "
                    + "format is not recognized.", 1);
        }
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
        CommandLineParser parser = new CLIOptionParser();
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
        switch (args.length)
        {
            case 0:
                result = new Behavior(RunType.GUI, null, 0, null, null);
                break;
                
            case 1:
                String pathname = args[0];
                if (!FileUtils.checkExists(pathname))
                {
                    String errMsg = "File " + pathname + " not found!";
                    result = new Behavior(null, null, 1, null, errMsg);
                    break;
                }
                result = new Behavior(RunType.GUI, cmd, 0, null, null);
                break;
                
            default:
                if (cmd.hasOption(CLIOptions.input))
                {
                    String pathName = cmd.getOptionValue(CLIOptions.input);
                    if (!FileUtils.checkExists(pathName))
                    {
                        String errMsg = "File '" + pathName + "' not found!";
                        result = new Behavior(null, null, 1, null, errMsg);
                        break;
                    }
                }
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
                    
                    if (!rt.equals(RunType.GUI) 
                            && !cmd.hasOption(CLIOptions.input))
                    {
                        String errMsg = "Option '-" 
                                + CLIOptions.input.getOpt() 
                                + "' is needed to specify the input, "
                                + "but was not found. Please, modify "
                                + "your command. ";
                        result = new Behavior(null, null, 1, null, errMsg);
                    } else {
                        if (rt.isCLIEnabled())
                        {
                            result = new Behavior(rt, cmd);
                        } else {
                            result = new Behavior(null, null, 1, null, 
                                    "RunType '"+rt+"' is not enabled from CLI. "
                                    + "Please, contacts the developers.");
                        }
                    }
                } else {
                    if (cmd.hasOption(CLIOptions.input))
                    {
                        result = new Behavior(RunType.GUI, cmd);
                    } else {
                        String helpMsg = getHelpString();
                        String errMsg = "No meaningful command line option. "
                                + "Nothing to do. Exiting.";
                        result = new Behavior(null, null, 1, helpMsg, errMsg);
                    }
                }
                break;
        }
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
                + "interface (GUI) without opening any specific file. "
                + "Alternatively, to open the file/folder named <filename> in "
                + "the GUI the two following commands are equivalent: "
                + DENOPTIMConstants.EOL
                + "  denoptim -" + CLIOptions.run.getOpt() 
                + " GUI -f <filename>"
                + DENOPTIMConstants.EOL
                + "  denoptim <filename> -" + CLIOptions.run.getOpt() + " GUI",
                true);
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
