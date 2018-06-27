package exception;

/**
 *
 * @author Vishwesh Venkatraman
 */
public class DENOPTIMException extends Exception
{
//------------------------------------------------------------------------------
    /**
     * Creates a new instance of <code>DENOPTIMException</code> without message.
     */
    public DENOPTIMException() 
    {
    }
    
//------------------------------------------------------------------------------    

    /**
     * Constructs a new DENOPTIMException with the given message and the
     * Exception as cause.
     *
     * @param err for the constructed exception
     * @param cause the Throwable that triggered the DENOPTIMException
     */
    public DENOPTIMException(String err, Throwable cause) 
    {
        super(err, cause);
    }

//------------------------------------------------------------------------------        

    /**
     * Constructs an instance of <code>DENOPTIMException</code> with the 
     * specified cause.
     * 
     * @param cause another exception (throwable).
     */
    public DENOPTIMException(Throwable cause) 
    {
        super(cause);
    }    
    
//------------------------------------------------------------------------------    
    

    /**
     * Constructs a new DENOPTIMException with the given message 
     *
     * @param err for the constructed exception
     */

    public DENOPTIMException(String err)
    {
        super(err);     
    }
    
//------------------------------------------------------------------------------
}
