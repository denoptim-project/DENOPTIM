package task;

import java.io.IOException;

/**
 *
 * @author Vishwesh Venkatraman
 */
public class ProcessHandler
{
    private int exitCode;
    private transient Process proc = null;

    /**
     * Content of the standard output for the process.
     */
    private String standardOutput;

    /**
     * Content of the error output for the process.
     */
    private String errorOutput;

    /**
     * A user-assigned id for this task.
     */
    private String id = null;

    private String cmdStr = null;


//------------------------------------------------------------------------------

    public ProcessHandler(String m_str, String m_id)
    {
        cmdStr = m_str;
        id = m_id;
    }

//------------------------------------------------------------------------------

    /**
     * Run the process associated with the command.
     * http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html?page=4
     * For any Process, the input and error streams must read even if the data
     * written to these streams is not used by the application. The generally
     * accepted solution for this problem is a stream gobbler thread that does
     * nothing but consume data from an input stream until stopped.
     * @throws Exception
     */

    public void runProcess() throws Exception
    {
        try
        {
            ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", cmdStr);
            proc = pb.start();

            Runtime.getRuntime().addShutdownHook(new Thread()
            {
                @Override
                public void run()
                {
                    //System.out.println("Running Shutdown Hook");
                    proc.destroy();
                }
            });

            // Any error message?
            StreamGobbler errorGobbler =
                    new StreamGobbler(proc.getErrorStream(), "ERR");

            // Any output?
            StreamGobbler outputGobbler =
                    new StreamGobbler(proc.getInputStream(), "OUT");


            errorGobbler.start();
            outputGobbler.start();

            errorGobbler.join();
            outputGobbler.join();

            exitCode = proc.waitFor();

            standardOutput = outputGobbler.getMessages();
            outputGobbler.sb.setLength(0);
            errorOutput = errorGobbler.getMessages();
            errorGobbler.sb.setLength(0);

        }
        catch(IllegalArgumentException iae)
        {
            //iae.printStackTrace();
            //System.out.println("Error: " + iae.getMessage());
            proc.destroy();
            throw iae;
        }
        catch(NullPointerException npe)
        {
            //ie.printStackTrace();
            System.out.println("Error: " + npe.getMessage());
            proc.destroy();
            throw npe;
        }
        catch(IOException ioe)
        {
            //ioe.printStackTrace();
            //System.err.println("Error: " + ioe.getMessage());
            proc.destroy();
            throw ioe;
        }
        catch(Exception e)
        {
            //e.printStackTrace();
            //System.err.println("Error: " + e.getMessage());
            proc.destroy();
            throw e;
        }
        finally
        {
            if (proc != null)
            {
                proc.getOutputStream().close();
                proc.getInputStream().close();
                proc.getErrorStream().close();
                proc.destroy();
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Time in milliseconds that we will wait for the process
     * to complete before timing out and killing it.
     * @param fTimeout
     * @throws Exception 
     */
    
    public void runProcess(long fTimeout) throws Exception
    {
        try
        {
            ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", cmdStr);
            proc = pb.start();

            Runtime.getRuntime().addShutdownHook(new Thread()
            {
                @Override
                public void run()
                {
                    //System.out.println("Running Shutdown Hook");
                    proc.destroy();
                }
            });

            ProcessWrapper processWrapper = new ProcessWrapper(proc);
            processWrapper.start();

            try
            {
                processWrapper.join(fTimeout);

                // Check for an exit code from the process
                if (processWrapper.getfExitCode() != null)
                {
                    exitCode = processWrapper.getfExitCode();
                }
                else
                {
                    // Set our exit code to 1
                    exitCode = 1;
                    proc.destroy();
                }
            }
            catch (InterruptedException e)
            {
                processWrapper.interrupt();
                Thread.currentThread().interrupt();
                proc.getOutputStream().close();
                proc.getInputStream().close();
                proc.getErrorStream().close();
                proc.destroy();
            }

            standardOutput = "";
            errorOutput = "";

        }
        catch(IllegalArgumentException iae)
        {
            //iae.printStackTrace();
            //System.out.println("Error: " + iae.getMessage());
            proc.destroy();
            throw iae;
        }
        catch(NullPointerException npe)
        {
            //ie.printStackTrace();
            System.out.println("Error: " + npe.getMessage());
            proc.destroy();
            throw npe;
        }
        catch(IOException ioe)
        {
            //ioe.printStackTrace();
            //System.err.println("Error: " + ioe.getMessage());
            proc.destroy();
            throw ioe;
        }
        catch(Exception e)
        {
            //e.printStackTrace();
            //System.err.println("Error: " + e.getMessage());
            proc.destroy();
            throw e;
        }
        finally
        {
            if (proc != null)
            {
                proc.getOutputStream().close();
                proc.getInputStream().close();
                proc.getErrorStream().close();
                proc.destroy();
            }
        }
    }


//------------------------------------------------------------------------------

    /**
     * Get the content of the standard output for the process.
     *
     * @return the output as a string.
     */
    public String getStandardOutput()
    {
        return standardOutput;
    }

//------------------------------------------------------------------------------

    /**
     * Get the content of the error output for the process.
     *
     * @return the output as a string.
     */
    public String getId()
    {
        return id;
    }

//------------------------------------------------------------------------------

    /**
     * Get the content of the error output for the process.
     *
     * @return the output as a string.
     */
    public String getErrorOutput()
    {
        return errorOutput;
    }

//------------------------------------------------------------------------------

    /**
     * Get the exit code returned by the sub-process.
     * @return the value of the exit code returned by the sub-process.
     * A negative value indicates the process was never launched or never returned.
     * @see java.lang.Process#waitFor()
     */
    public int getExitCode()
    {
        return exitCode;
    }

//------------------------------------------------------------------------------

    public void stopProcess()
    {
        try
        {
            if (proc != null)
            {
                proc.getOutputStream().close();
                proc.getInputStream().close();
                proc.getErrorStream().close();
                proc.destroy();
            }
        }
        catch (IOException ioe)
        {

        }
    }

//------------------------------------------------------------------------------

}
