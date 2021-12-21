package denoptim.integration.tinker;

/**
 * Exceptions resulting from a failure of Tinker.
 */

public class TinkerException extends Exception
{

    /**
     * Version ID
     */
    private static final long serialVersionUID = 3L;

    /**
     * Identification of the task that caused the failure
     */
    public String taskName = "notSpecified";
    
    /**
     * Proposed solution to the failure, or empty string.
     */
    public String solution = "";
    
//------------------------------------------------------------------------------
    
    /**
     * Constructor for an exception resulting from a failure of Tinker.
     * @param errMsg the error message from Tinker.
     * @param taskName an identification of the task that Tinker was supposed to
     * perform when it failed.
     */
    public TinkerException(String errMsg, String taskName)
    {
        super(errMsg);
        this.taskName = taskName;
        interpreteMsg();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Reads the message and trying to find a match in the map of solutions to 
     * known problems.
     */
    private void interpreteMsg()
    {
        for (String simpthon : TinkerConstants.KNOWNERRORS.keySet())
        {
            if (getMessage().contains(simpthon))
            {
                solution = solution + System.getProperty("line.separator")
                            + TinkerConstants.KNOWNERRORS.get(simpthon);
            }
        }
    }
    
//------------------------------------------------------------------------------
    
}
