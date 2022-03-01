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
