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

package denoptim.fragspaceexplorer;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;


/**
 * Object collecting information needed to restart a FragSpaceExplorer job.
 * The unique indexes are stored aiming to maintain the uniqueness property
 * when restarting a job. Thus no vertex/graph/molecule will be found 
 * in the run generating a checkpoint with the ID stored in that
 * checkpoint.<br>
 * The checkpoint also stores the ID of the latest safely completed
 * graph: a graph for which the corresponding task was completed when the 
 * checkpoint has been created, and of which all preceding tasks were also
 * completed. 
 * 
 * @author Marco Foscato
 */

public class FSECheckPoint implements Serializable
{
    /**
     * Level
     */
    private int level = -1;

    /** 
     * Unique vertex index
     */
    private int unqVrtId = -1;

    /**
     * Unique graph index
     */
    private int unqGraphId = -1;

    /**
     * Unique molecule index
     */
    private int unqMolId = -1;

    /**
     * ID of the root graph used to build this graph
     */
    private int rootId = -1;

    /**
     * ID of safely completed graph
     */
    private int graphId = -1;

    /**
     * Set of indeces for the next iteration in combination of frags 
     */
    private ArrayList<Integer> nextIds;


//-----------------------------------------------------------------------------

    public FSECheckPoint()
    {
        nextIds = new ArrayList<Integer>();
    }

//-----------------------------------------------------------------------------

    public int getLevel()
    {
	return level;
    }

//-----------------------------------------------------------------------------

    public int getUnqVrtId()
    {
        return unqVrtId;
    }

//-----------------------------------------------------------------------------

    public int getUnqGraphId()
    {
        return unqGraphId;
    }

//-----------------------------------------------------------------------------

    public int getUnqMolId()
    {
        return unqMolId;
    }

//-----------------------------------------------------------------------------

    public ArrayList<Integer> getNextIds()
    {
	return nextIds;
    }

//-----------------------------------------------------------------------------

    public int getLatestSafelyCompletedGraphId()
    {
	return graphId;
    }

//-----------------------------------------------------------------------------

    public int getRootId()
    {
	return rootId;
    }

//-----------------------------------------------------------------------------

    /**
     * @param file the name of the binary file containing a serialized 
     * <code>DENOPTIMGraph</code>.
     * @return <code>true</code> if the graph from the given file has been 
     * fully explored before.
     */
    
    public boolean serFileAlreadyUsed(String filename)
    {
	File candFile = new File(filename);
	File doneFile = new File(FSEUtils.getBaseNameOfStorageFile(rootId));
	int res = candFile.compareTo(doneFile);
	if (res < 0)
	{
	    return true;
	}
	return false;
    }

//-----------------------------------------------------------------------------

    /**
     * Set the indeces that identify the combination of fragments next to the
     * latest one that has been properly processed.
     */

    public void setNextIds(ArrayList<Integer> nextIds)
    {
	this.nextIds = nextIds;
    }

//-----------------------------------------------------------------------------

    /**
     * Set the current level
     */

    public void setLevel(int level)
    {
	this.level = level;
    }

//-----------------------------------------------------------------------------

    /**
     * Set the restart value for the unique vertex ID
     */

    public void setUnqVrtId(int val)
    {
        unqVrtId = val;
    }

//-----------------------------------------------------------------------------

    /**
     * Set the restart value for the unique graph ID
     */

    public void setUnqGraphId(int val)
    {
        unqGraphId = val;
    }

//-----------------------------------------------------------------------------

    /**
     * Set the restart value for the unique molecule ID
     */

    public void setUnqMolId(int val)
    {
        unqMolId = val;
    }

//-----------------------------------------------------------------------------

    /**
     * Set the graphId of the safely completed graph.
     */

    public void setSafelyCompletedGraphId(int val)
    {
        graphId = val;
    }

//-----------------------------------------------------------------------------

    /**
     * Set the graph ID of the root graph used to build this task
     */

    public void setRootId(int val)
    {
	rootId = val;
    }

//-----------------------------------------------------------------------------
   
    @Override
    public String toString()
    {
	StringBuilder sb = new StringBuilder();
	sb.append("FSECheckPoint[");
	sb.append("level=").append(level);
	sb.append(", unqVrtId=").append(unqVrtId);
	sb.append(", unqGraphId=").append(unqGraphId);
	sb.append(", unqMolId=").append(unqMolId);
	sb.append(", graphId=").append(graphId);
	sb.append(", rootId=").append(rootId);
	sb.append(", nextIds=").append(nextIds);
	return sb.toString();
    }

//-----------------------------------------------------------------------------

}
