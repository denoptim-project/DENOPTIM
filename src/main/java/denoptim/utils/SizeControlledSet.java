package denoptim.utils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import denoptim.exception.DENOPTIMException;
import denoptim.files.FileUtils;
import denoptim.io.DenoptimIO;

/**
 * Class meant to collect unique strings without leading to memory overflow.
 * This class wraps a synchronised set and controls the size of such set.
 * If we need to deal with more elements than the maximum size, then the 
 * entries that do not fit in the maximum size are written to disk and dealt 
 * with by I/O operations.
 */

public class SizeControlledSet
{
    /**
     * Maximum size of the set. If there is need to use more entries, the 
     * entries that do not fit are stored on disk.
     */
    private int maxSize;
    
    /**
     * The file used to deal with entries that do not fit in the
     * maximum size of this set.
     */
    private File dataOnDisk;
    
    /**
     * The file used to store all entries on disk
     */
    private File allData;
    
    /**
     * Flag signalling the use of the disk.
     */
    private boolean usingDisk = false;
    
    /**
     * Number of entries written to file
     */
    private int entriesInFile = 0;
    
    /**
     * The actual data collection
     */
    private Set<String> data;
    
//------------------------------------------------------------------------------
    
    /**
     * Constructor for a size-controlled storage of unique Strings.
     * @param maxSize the maximum size of entries to keep in the memory.
     * @param memoryFile the pathname to a non-existing file that might be used to 
     * store entries on disk in case the maximum size is not sufficient.
     * @param allUIDsFile the pathname to a file where all entries are 
     * collected. It can be null, in which case we do not write every entry to
     * file.
     */
    public SizeControlledSet(int maxSize, String memoryFile, String allUIDsFile)
    {
        this.maxSize = maxSize;
        this.dataOnDisk = new File(memoryFile);
        if (allUIDsFile!=null)
            this.allData = new File(allUIDsFile);
        data = Collections.synchronizedSet(new HashSet<String>());
    }
    
//------------------------------------------------------------------------------

    /**
     * Checks if the given entry is already container in the set of known 
     * entries and, if not, adds it to the set. This method adds the entry to 
     * memory or disk depending on the maximum memory footprint defined at 
     * construction time.
     * @param entry the entry to search for and, possibly, to add to the set.
     * @return <code>true</code> if the set did not already contain the entry, 
     * which was then added.
     * @throws IOException when handling of the memory written on disk returns
     * exception.
     */
    public synchronized boolean addNewUniqueEntry(String entry) throws IOException
    {
        synchronized (data)
        {
            boolean wasNew = false;
            if (usingDisk)
            {
                if (data.contains(entry))
                {
                    return false;
                }
                wasNew = !FileUtils.isLineInTxtFile(entry, dataOnDisk, true);
                if (wasNew)
                {
                    entriesInFile++;
                }
            } else {
                wasNew = data.add(entry);
                if (data.size()>=maxSize)
                    usingDisk = true;
            }
            if (wasNew && allData!=null)
            {
                try
                {
                    DenoptimIO.writeData(allData.getAbsolutePath(), entry,
                            true);
                } catch (DENOPTIMException e)
                {
                    throw ((IOException) e.getCause());
                }
            }
            return wasNew;
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Checks if an entry is contained in this collection.
     * @param entry the entry to search for.
     * @return  <code>true</code> if the entry is already present in the 
     * collection.
     * @throws IOException when handling of the memory written on disk returns
     * exception.
     */
    public synchronized boolean contains(String entry) throws IOException
    {
        synchronized (data)
        {
            boolean foundInMemory = data.contains(entry);
            boolean foundInDisk = false;
            if (usingDisk && !foundInMemory)
            {
                foundInDisk = FileUtils.isLineInTxtFile(entry, dataOnDisk, 
                        false);
            }
            return foundInMemory || foundInDisk;
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the number of unique entries.
     * @return the number of unique entries.
     */
    public synchronized int size()
    {
        synchronized (data)
        {
            return data.size() + entriesInFile;
        }
    }

//------------------------------------------------------------------------------

}
