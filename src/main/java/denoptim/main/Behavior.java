package denoptim.main;


import denoptim.main.Main.RunType;

/**
 * Represents the behavior of the program at start-up. This class in mostly 
 * meant to test the functionality of denoptim's main launcher.
 */
public class Behavior
{
    /**
     * The help message
     */
    protected String helpMsg = "";
    
    /**
     * The error message
     */
    protected String errorMsg = "";

    /**
     * A non-zero value means some error has occurred and the program will 
     * terminate.
     */
    protected int exitStatus = 0;
    
    /**
     * The type of run that is requested.
     */
    protected RunType runType = null;
    
    /**
     * The arguments
     */
    protected String[] params = null;
    
//------------------------------------------------------------------------------

    /**
     * Creates a behavior
     * @param runType the type of run.
     * @param params the arguments for the run.
     * @param exitStatus any non-zero corresponds to an error.
     * @param errorMsg the error message, if any.
     */
    public Behavior(RunType runType, String[] params, int exitStatus, 
            String helpMsg, String errorMsg)
    {
        this.helpMsg = helpMsg;
        this.errorMsg = errorMsg;
        this.exitStatus = exitStatus;
        this.runType = runType;
        this.params = params;
    }
    
//------------------------------------------------------------------------------

    /**
     * Creates a behavior.
     * @param runType the type of run.
     * @param params the arguments for the run.
     */
    public Behavior(RunType runType, String[] params)
    {
        this(runType, params, 0, null, null);
    }
    
//------------------------------------------------------------------------------
    
}
