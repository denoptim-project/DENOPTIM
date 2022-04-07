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

package denoptim.combinatorial;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.files.SingletonFileAccess;
import denoptim.graph.DENOPTIMGraph;
import denoptim.io.DenoptimIO;


/**
 * Helper methods for the exploration of the fragment space.
 *
 * @author Marco Foscato
 */

public class CEBLUtils
{

//------------------------------------------------------------------------------

    /**
     * @return the pathname of the folder where serialized graphs of a level 
     * are stored
     */

    public static String getNameOfStorageDir(CEBLParameters settings, int level)
    {
        String dirName = settings.getDBRoot()
                          + DENOPTIMConstants.FSEP
                          + DENOPTIMConstants.FSEIDXNAMEROOT
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
        if (fileName.contains(Pattern.quote(DENOPTIMConstants.SERGFILENAMEROOT)) && 
            fileName.contains(Pattern.quote(".")))
        {
             msg = "Failed attempt to extract a graphId from String '" 
                         + fileName + "'";
            throw new DENOPTIMException(msg);
        }
        String[] p1 = fileName.split(Pattern.quote(DENOPTIMConstants.SERGFILENAMEROOT));
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
        String baseName = DENOPTIMConstants.SERGFILENAMEROOT
                          + graphId + "."
                          + DENOPTIMConstants.SERGFILENAMEEXT;
        return baseName;
    }

//------------------------------------------------------------------------------

    /**
     * @return the pathname of a FragSpaceExplorer storage file
     */

    public static String getNameOfStorageFile(CEBLParameters settings, int level,
            int graphId)
    {
        String fileName = settings.getDBRoot()
                          + DENOPTIMConstants.FSEP
                          + DENOPTIMConstants.FSEIDXNAMEROOT
                          + level
                          + DENOPTIMConstants.FSEP
                          + getBaseNameOfStorageFile(graphId);
        return fileName;
    }

//------------------------------------------------------------------------------

    /**
     * @return the pathname of a FragSpaceExplorer storage file where
     */

    public static String getNameOfStorageIndexFile(CEBLParameters settings, 
            int level)
    {
        String fileName = settings.getDBRoot()
                          + DENOPTIMConstants.FSEP
                          + DENOPTIMConstants.FSEIDXNAMEROOT
                          + level
                          + DENOPTIMConstants.FSEP
                          + DENOPTIMConstants.FSEIDXNAMEROOT
                          + level + ".txt";
        return fileName;
    }

//------------------------------------------------------------------------------

    /**
     * Serialize all <code>DENOPTIMGraph</code>s to file.
     * The pathname of the output file is given by the value of 
     * <code>level</code> and the parameters from <code>FSEParameters</code>.
     * @param lstGraphs
     * @param level
     */

    protected static void storeAllGraphsOfLevel(CEBLParameters settings, 
            ArrayList<DENOPTIMGraph> lstGraphs, int level) throws DENOPTIMException
    {
        for (DENOPTIMGraph g : lstGraphs)
              {
      	    //NOTE: the arraylist is supposed to hold the indeces used to 
      	    //      create the next combination of fragments, but this method
      	    //      is used only for graphs built with the base scaffolds or 
      	    //      the naked root graphs. Therefore there is no set of indeces
      	    //      to store and we fed the method with an empty array.
      	    // NOTE2: the root Id is set to zero for the same reason.
            
            DENOPTIMGraph c = g.clone();
            
            storeGraphOfLevel(settings, c,level,0,new ArrayList<Integer>());
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
     * @param nextIds the set of indexes used to generate the next combination
     * of fragment. 
     */

    protected static void storeGraphOfLevel(CEBLParameters settings, 
            DENOPTIMGraph graph, int level, int rootId, 
            ArrayList<Integer> nextIds) throws DENOPTIMException
    {
        String outDir = getNameOfStorageDir(settings, level);
        if (!denoptim.files.FileUtils.checkExists(outDir))
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

        String fileSer = getNameOfStorageFile(settings, level, 
                graph.getGraphId());
        String indexFile = getNameOfStorageIndexFile(settings, level);
        String indexLine = graph.toString() + " => " + graph.getGraphId() + " " 
                           + rootId + " " + nextIds;
        
        SingletonFileAccess.getInstance().writeData(fileSer, graph.toJson(),
                false);
        SingletonFileAccess.getInstance().writeData(indexFile, indexLine, true);
    }

//------------------------------------------------------------------------------

    /**
     * Store the checkpoint in a text file with json format.
     */

    protected static void serializeCheckPoint(CEBLParameters settings) 
            throws DENOPTIMException
    {
        Gson writer = new GsonBuilder().setPrettyPrinting().create();
        DenoptimIO.writeData(settings.getCheckPointName(), writer.toJson(
                settings.getCheckPoint()), false);
    }

//------------------------------------------------------------------------------

    /**
     * Converts a text file into the corresponding checkpoint object.
     * @param file the pathname of the file to convert.
     */

    public static CheckPoint deserializeCheckpoint(String file)
            throws DENOPTIMException
    {
        String s = DenoptimIO.readText(file);
        Gson writer = new GsonBuilder().create();
        CheckPoint chkpt = writer.fromJson(s, CheckPoint.class);
        return chkpt;
    }

//------------------------------------------------------------------------------

} 
