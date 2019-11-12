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

package fragspaceexplorer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.io.SingletonFileAccess;
import denoptim.molecule.DENOPTIMGraph;


/**
 * Helper methods for the exploration of the fragment space.
 *
 * @author Marco Foscato
 */

public class FSEUtils
{

//------------------------------------------------------------------------------

    /**
     * @return the pathname of the folder where serialized graphs of a level 
     * are stored
     */

    public static String getNameOfStorageDir(int level)
    {
        String dirName = FSEParameters.getDBRoot()
                          + DENOPTIMConstants.FSEP
                          + FSEParameters.DIRNAMEROOT
                          + level;
        return dirName;
    }

//------------------------------------------------------------------------------

    /** 
     * @return the graphId from the name of the storage file
     */

    public static int getGraphIdFromStorageFile(String fileName) 
							throws DENOPTIMException
    {
	String msg ="";
	if (fileName.contains(Pattern.quote(FSEParameters.FILENAMEROOT)) && 
            fileName.contains(Pattern.quote(".")))
	{
	     msg = "Failed attempt to extract a graphId from String '" 
			 + fileName + "'";
	    throw new DENOPTIMException(msg);
	}
	String[] p1 = fileName.split(Pattern.quote(FSEParameters.FILENAMEROOT));
	String[] p2 = p1[1].split(Pattern.quote("."));
	int graphId = -1;
	try
	{
	    graphId = Integer.parseInt(p2[0]);
	}
	catch (Throwable t)
	{
	    throw new DENOPTIMException(msg);
	}
	return graphId;
    }

//------------------------------------------------------------------------------

    /**
     * @return the basename of a FragSpaceExplorer storage file
     */
   
    public static String getBaseNameOfStorageFile(int graphId)
    {
	String baseName = FSEParameters.FILENAMEROOT
                          + graphId + "."
                          + FSEParameters.FILENAMEEXT;
	return baseName;
    }

//------------------------------------------------------------------------------

    /**
     * @return the pathname of a FragSpaceExplorer storage file
     */

    public static String getNameOfStorageFile(int level, int graphId)
    {
        String fileName = FSEParameters.getDBRoot()
                          + DENOPTIMConstants.FSEP
                          + FSEParameters.DIRNAMEROOT
                          + level
                          + DENOPTIMConstants.FSEP
			  + getBaseNameOfStorageFile(graphId);
        return fileName;
    }

//------------------------------------------------------------------------------

    /**
     * @return the pathname of a FragSpaceExplorer storage file where
     */

    public static String getNameOfStorageIndexFile(int level)
    {
        String fileName = FSEParameters.getDBRoot()
                          + DENOPTIMConstants.FSEP
                          + FSEParameters.DIRNAMEROOT
                          + level
                          + DENOPTIMConstants.FSEP
                          + FSEParameters.DIRNAMEROOT
                          + level + ".txt";
        return fileName;
    }

//------------------------------------------------------------------------------

    /**
     * Serilize all <code>DENOPTIMGraph</code>s to file.
     * The pathname of the output file is given by the value of 
     * <code>level</code> and the parameters from <code>FSEParameters</code>.
     * @param lstGraphs
     * @param level
     */

    protected static void storeAllGraphsOfLevel(
                                  ArrayList<DENOPTIMGraph> lstGraphs, int level)
                                                        throws DENOPTIMException
    {
        for (DENOPTIMGraph g : lstGraphs)
        {
	    //NOTE: the arraylist is supposed to hold the indeces used to 
	    //      create the next combination of fragments, but this method
	    //      is used only for graphs built with the base scaffolds or 
	    //      the nacked root graphs. Therefore there is no set of indeces
	    //      to store and we fed the method with an empty array.
	    // NOTE2: the root Id is set to zero for the same reason.
            storeGraphOfLevel(g,level,0,new ArrayList());
        }
    }

//------------------------------------------------------------------------------

    /**
     * Serialize a <code>DENOPTIMGraph</code> to a file.
     * The pathname of the output file is given by the value of 
     * <code>level</code> and the parameters from <code>FSEParameters</code>.
     * @param graph the graph to store
     * @param level the level of modification from which the graph is generated
     * @param rootId the ID of the root graph used to build the graph
     * @param nextIds the set of indeces used to generate the next combination
     * of fragment. 
     */

    protected static void storeGraphOfLevel(DENOPTIMGraph graph, int level, 
                            int rootId, ArrayList<Integer> nextIds) 
							throws DENOPTIMException
    {
	String outDir = getNameOfStorageDir(level);
	if (!DenoptimIO.checkExists(outDir))
	{
	    try
	    {
		FileUtils.forceMkdir(new File(outDir));
	    }
	    catch (Throwable t)
	    {
		String msg = "Cannot create folder " + outDir;
		throw new DENOPTIMException(msg,t);
	    }
	}

        String fileSer = getNameOfStorageFile(level,graph.getGraphId());
	String indexFile = getNameOfStorageIndexFile(level);
	String indexLine = graph.toString() + " => " + graph.getGraphId() + " " 
			   + rootId + " " + nextIds;
	SingletonFileAccess.getInstance().serializeToFile(fileSer,graph,true);
	SingletonFileAccess.getInstance().writeData(indexFile,indexLine,true);
    }

//------------------------------------------------------------------------------

    /**
     * Store the checkpoint in a serialized form
     */

    protected static void serializeCheckPoint() throws DENOPTIMException
    {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try
        {
            fos = new FileOutputStream(FSEParameters.getCheckPointName(),false);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(FSEParameters.getCheckPoint());
            oos.close();
        }
        catch (Throwable t)
        {
            throw new DENOPTIMException("Cannot serialize checkpoint.", t);
        }
        finally
        {
            try
            {
                fos.flush();
                fos.close();
                fos = null;
            }
            catch (Throwable t)
            {
                throw new DENOPTIMException("cannot close FileOutputStream",t);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Convertes a binary file into the corresponding checkpoint object
     * @param file the pathname of the file to convert
     */

    public static FSECheckPoint deserializeCheckpoint(String file)
							throws DENOPTIMException
    {
        FSECheckPoint chkpt = null;
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try
        {
            fis = new FileInputStream(new File(file));
            ois = new ObjectInputStream(fis);
            chkpt = (FSECheckPoint) ois.readObject();
            ois.close();
        }
        catch (Throwable t)
        {
            throw new DENOPTIMException(t);
        }
        finally
        {
            try
            {
                fis.close();
            }
            catch (Throwable t)
            {
                throw new DENOPTIMException(t);
            }
	}
	return chkpt;
    }

//------------------------------------------------------------------------------

} 
