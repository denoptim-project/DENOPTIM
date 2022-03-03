package denoptim.main;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import denoptim.constants.DENOPTIMConstants;
import denoptim.main.Main.RunType;

public class CLIOptions extends Options
{
    /**
     * Version ID
     */
    private static final long serialVersionUID = 3L;
    
    /**
     * Option requesting the printing of the help message.
     */
    public static Option help;
    
    /**
     * Option requesting only the printing of the version.
     */
    public static Option version;
    
    /**
     * Option controlling the type of run.
     */
    public static Option run;
    
    /**
     * The only, static instance of this class
     */
    private static final CLIOptions instance = new CLIOptions();
    
//------------------------------------------------------------------------------    
    
    private CLIOptions() 
    {
        help = new Option("h","help",false, "Print help message.");
        help.setRequired(false);
        this.addOption(help);
        
        run = new Option("r","run",true, "Request a specific type of "
                + "run. Choose among:" + DENOPTIMConstants.EOL
                + RunType.getRunTypesForUser());
        run.setRequired(false);
        this.addOption(run);
        
        version = new Option("v","version",false, "Print denoptim version.");
        version.setRequired(false);
        this.addOption(version);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Gets the singleton instance of this class.
     * @return
     */
    public static CLIOptions getInstance()
    {
        return instance;
    }
    
//------------------------------------------------------------------------------
    
}
