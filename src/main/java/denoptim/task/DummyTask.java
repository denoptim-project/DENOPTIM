/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no> and
 *   Marco Foscato <marco.foscato@uib.no>
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

import denoptim.utils.TaskUtils;

/**
 * Task that does nothing more than declaring its existence on stdout and
 * waits some time before terminating.
 * Meant only for testing
 */

public class DummyTask extends Task
{
	private int sleepMillisec = 1000;

//------------------------------------------------------------------------------
    
    public DummyTask(int sleepTimeInSeconds)
    {
    	super(TaskUtils.getUniqueTaskIndex());
    	this.sleepMillisec = sleepTimeInSeconds * 1000;
    }

//------------------------------------------------------------------------------

    @Override
    public Object call()
    {
    	System.out.println("Dummy task "+id+" called...");
    	try {
			Thread.sleep(sleepMillisec);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	System.out.println("Dummy task "+id+" done!");
		return null;
    }
    
//------------------------------------------------------------------------------

}
