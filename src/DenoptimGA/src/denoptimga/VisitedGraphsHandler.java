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

package denoptimga;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.molecule.DENOPTIMGraph;
import denoptim.utils.GraphConversionTool;


/**
 * This tool scans the list of previously visited DENOPTIMGraphs and
 * compares each member with a candidate new member that is added to the list
 * if it corresponds to a novel graph that is not equivalent to any previously
 * reported graph.
 *
 * WARNING! Under development. Not tested!
 *
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class VisitedGraphsHandler
{
    
    private final java.util.logging.Logger LOGGER = 
       java.util.logging.Logger.getLogger(VisitedGraphsHandler.class.getName());
    
    private String graphLibFile = "";
    private ArrayList<IAtomContainer> libScaff;
    private ArrayList<IAtomContainer> libFrag;
    private ArrayList<IAtomContainer> libCap;
    private HashMap<String, Integer> mp;

//------------------------------------------------------------------------------

    /**
     * Constructor 
     * @param graphLibFile the file containing the list of previous graphs
     * @param libScaff
     * @param libFrag
     * @param libCap
     */
    public VisitedGraphsHandler(String graphLibFile,
                                        ArrayList<IAtomContainer> libScaff,
                                        ArrayList<IAtomContainer> libFrag,
                                        ArrayList<IAtomContainer> libCap,
                                        HashMap<String, Integer> mp)
    {
        this.graphLibFile = graphLibFile;
        this.libScaff = libScaff;
        this.libFrag = libFrag;
        this.libCap = libCap;
	    this.mp = mp;
    }

//------------------------------------------------------------------------------

    /**
     * Compares the candidate graph with the graphs in the library and, if
     * the candidate graph is new and not equivalent to those found in the 
     * library, appends the new candidate to the list
     * @param candGraph the candidate new graph
     * @return <code>true</code> if the graph is appended to the list.
     */
    public boolean appendGraph(DENOPTIMGraph candGraph) {
        boolean appended = false;

        RandomAccessFile rafile = null; // The file we'll lock
        FileChannel channel = null; // The channel to the file
        FileLock lock = null; // The lock object we hold
        File lockfile = null;

        try {
            lockfile = new File(graphLibFile);
            // Creates a random access file stream to read from, and 
            // optionally to write to
            // Create a FileChannel that can read and write that file.
            // Note that we rely on the java.io package to open the file,
            // in read/write mode, and then just get a channel from it.
            // This will create the file if it doesn't exit. We'll arrange
            // for it to be deleted below, if we succeed in locking it.
            rafile = new RandomAccessFile(lockfile, "rw");

            channel = rafile.getChannel();

            // Try to get an exclusive lock on the file.
            // This method will return a lock or null, but will not block.
            // See also FileChannel.lock() for a blocking variant.

            while (true) {
                // Attempts to acquire an exclusive lock on this channel's file
                // (returns null or throws
                // an exception if the file is already locked.
                try {
                    lock = channel.tryLock();
                    if (lock != null)
                        break;
                } catch (OverlappingFileLockException e) {
                    // thrown when an attempt is made to acquire a lock on a a
                    // file that overlaps
                    // a region already locked by the same JVM or when another
                    // thread is already
                    // waiting to lock an overlapping region of the same file
                    System.err.println("Overlapping File Lock Error: "
                            + e.getMessage());
                }
            }

            boolean found = false;
            for (String line; (line = rafile.readLine()) != null; ) {
                if (line.trim().length() == 0)
                    continue;

                DENOPTIMGraph graphInLib =
                        GraphConversionTool.getGraphFromString(line);

                if (candGraph.sameAs(graphInLib, new StringBuilder())) {
                    found = true;
                    ;
                    break;
                }
            }

            // do all the checks and update the keyfile as required
            // move the cursor to the end of the file
            if (!found) {
                rafile.seek(channel.position());
                rafile.writeBytes(candGraph.toString() + "\n");
                channel.force(true);
                appended = true;
            }
        }
        catch (Exception de)
        {
            LOGGER.log(Level.SEVERE, null, de); 
        }
        finally
        {
            try 
            {
                // close the channel
                if (channel != null)
                    channel.close();
                if (rafile != null)
                    rafile.close();
                // release the lock
                if (lock != null && lock.isValid())
                    lock.release();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return appended;
    }
    
//------------------------------------------------------------------------------
    
}
