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

package denoptim.files;

import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;

/**
 * Singleton for synchronizing multi-thread safe file access
 *
 * @author Marco Foscato
 */

public class SingletonFileAccess
{

    private static final SingletonFileAccess sfa = new SingletonFileAccess();

//------------------------------------------------------------------------------

    private SingletonFileAccess()
    {
	super();
    }

//------------------------------------------------------------------------------

    /**
     * Write data as an unformatted string to a text file.
     *
     * @param filename 
     * @param data 
     * @param append if <code>true</code> requires to append to file
     * @throws DENOPTIMException
     */
    public synchronized void writeData(String fileName, String data,
								 boolean append)
							throws DENOPTIMException
    {
        DenoptimIO.writeData(fileName, data, append);
    }

//------------------------------------------------------------------------------

    /**
     * Serialize object to a given file.
     *
     * @param filename
     * @param obj the object to be serialized
     * @param append if <code>true</code> requires to append to file
     * @throws DENOPTIMException
     */
    public synchronized void serializeToFile(String fileName, Object obj,
                                                                 boolean append)
                                                        throws DENOPTIMException
    {
        DenoptimIO.serializeToFile(fileName, obj, append);
    }
//------------------------------------------------------------------------------

    /**
     * Returns the single instance of this class
     */

    public static SingletonFileAccess getInstance() 
    {
        return sfa;
    }

//------------------------------------------------------------------------------

}
