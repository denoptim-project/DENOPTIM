/*
 *   DENOPTIM
 *   Copyright (C) 2019 Marco Foscato <marco.foscato@uib.no>
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

package denoptim.utils;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * Utilities for tasks.
 *
 * @author Marco Foscato
 */

public class TaskUtils
{
    private static AtomicInteger taskCounter = new AtomicInteger(1);
    
//------------------------------------------------------------------------------

    /**
     * Unique counter for tasks
     * @return the new task id (number)
     */

    public static synchronized int getUniqueTaskIndex()
    {
        return taskCounter.getAndIncrement();
    }

//------------------------------------------------------------------------------

}
