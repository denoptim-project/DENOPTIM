package io;

import exception.DENOPTIMException;

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
