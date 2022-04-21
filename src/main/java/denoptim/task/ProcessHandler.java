/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no>
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

package denoptim.task;

import java.io.IOException;

import denoptim.exception.DENOPTIMException;

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

    public ProcessHandler(String cmdStr, String id)
    {
        this.cmdStr = cmdStr;
        this.id = id;
    }

//------------------------------------------------------------------------------

    /**
     * Run the process associated with the command.
     * http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html?page=4
     * For any Process, the input and error streams must read even if the data
     * written to these streams is not used by the application. The generally
     * accepted solution for this problem is a stream gobbler thread that does
     * nothing but consume data from an input stream until stopped.
     * @throws DENOPTIMException 
     * @throws Exception
     */

    public void runProcess() throws DENOPTIMException
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
            if (proc!=null)
                proc.destroy();
            throw new DENOPTIMException(iae);
        }
        catch(NullPointerException npe)
        {
            if (proc!=null)
                proc.destroy();
            throw  new DENOPTIMException(npe);
        }
        catch(IOException ioe)
        {
            if (proc!=null)
                proc.destroy();
            throw  new DENOPTIMException(ioe);
        }
        catch(Exception e)
        {
            if (proc!=null)
                proc.destroy();
            throw  new DENOPTIMException(e);
        }
        finally
        {
            if (proc != null)
            {
                try {
					proc.getOutputStream().close();
					proc.getInputStream().close();
					proc.getErrorStream().close();
					proc.destroy();
				} catch (IOException e) {
					e.printStackTrace();
				}
            }
        }
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
        catch (Throwable t)
        {
            // do nothing
        }
    }

//------------------------------------------------------------------------------

}
