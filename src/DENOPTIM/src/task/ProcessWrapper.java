package task;

/**
 *
 * @author Vishwesh Venkatraman
 */
public class ProcessWrapper extends Thread
{
    private final Process fProcess;
    private Integer fExitCode;
    private StreamGobbler errorGobbler;
    private StreamGobbler outputGobbler;

//------------------------------------------------------------------------------


    public String getErrorMessages()
    {
        return this.errorGobbler.getMessages();
    }

//------------------------------------------------------------------------------

    public String getOutputMessages()
    {
        return this.outputGobbler.getMessages();
    }

//------------------------------------------------------------------------------

    public Integer getfExitCode()
    {
	return fExitCode;
    }

//------------------------------------------------------------------------------

    public ProcessWrapper(Process fProcess)
    {
	this.fProcess = fProcess;
    }

//------------------------------------------------------------------------------

    @Override
    public void run()
    {
	try
        {
            // Any error message?
            errorGobbler = new StreamGobbler(fProcess.getErrorStream(), "ERR");

            // Any output?
            outputGobbler = new StreamGobbler(fProcess.getInputStream(), "OUT");

            errorGobbler.start();
            outputGobbler.start();

            errorGobbler.join();
            outputGobbler.join();

            fExitCode = fProcess.waitFor();
	}
        catch (InterruptedException e)
        {
            //System.err.println("InterruptedException for Process, " + fProcess.toString() + " e = " + e);
        }
    }

//------------------------------------------------------------------------------
}