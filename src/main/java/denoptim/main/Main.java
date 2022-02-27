package denoptim.main;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

import denoptim.constants.DENOPTIMConstants;
import denoptim.files.FileUtils;
import denoptim.logging.Version;

/**
 * Entry point of any kind of run of the denoptim program.
 */
public class Main
{
    /**
     * Types of runs that can be requested to the denoptim Main class.
     */
    public static enum RunType {GA, FSE, GUI};
    
//------------------------------------------------------------------------------
    
    /**
     * Launches the appropriate program according to the arguments given. Use
     * "-h" to get usage instructions.
     * @param args the list of arguments.
     */
    
    public static void main(String[] args)
    {
        Behavior behavior = defineProgramBehavior(args);
        if (behavior.exitStatus!=0)
            reportError(behavior);
        
        //TODO-gg

        System.exit(0);
    }

//------------------------------------------------------------------------------
    
    /**
     * Does the processing of the application arguments and decides what the
     * program should do.
     * @param args the arguments to process
     * @return the behavior of the program.
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
            return new Behavior(null, null, 0, helpMsg, null);
        }
        
        if (cmd.hasOption(CLIOptions.version))
        {
            return new Behavior(null, null, 0, Version.buildDenoptimHeader(), 
                    null);
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
                result = new Behavior(RunType.GUI, args, 0, null, null);
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
                    RunType rt = RunType.valueOf(
                            cmd.getOptionValue(CLIOptions.run).toString());
                    
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
                        switch (rt)
                        {
                            case GA:
                                result = new Behavior(RunType.GA, args);
                                break;
                                
                            case FSE:
                                result = new Behavior(RunType.FSE, args);
                                break;
                                
                            case GUI:
                                result = new Behavior(RunType.GUI, args);
                                break;
                        }
                    }
                } else {
                    if (cmd.hasOption(CLIOptions.input))
                    {
                        result = new Behavior(RunType.GUI, args);
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
                "", true);
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

}
