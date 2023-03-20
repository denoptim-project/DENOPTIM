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

package denoptim.graph;


/**
 * A light-weight candidate is a very low-demanding collection of data upon
 * a specific candidate item. This does NOT include the {@link DGraph}
 * or any molecular representation. The purpose of this class is to encapsulate 
 * only the lowest possible amount of information needed manipulate the item
 * in a larger list of items (e.g., when plotting evolution plots).
 * Note that only the name and UID are used to check for equality.
 */

public class CandidateLW
{
    /**
     * Unique identifier of this candidate.
     */
    private String uid;
    
    /**
     * Name of this candidate (not guaranteed to be unique).
     */
    private String name;
    
    /**
     * Pathname to file defining this item in detail.
     */
    private String pathNameToFile;

    /**
     * fThe fitness value or null;
     */
    private Double fitness;
    
    /**
     * Error that prevented calculation of the fitness or null.
     */
    private String error;

    /**
     * ID of the generation this molecule belong to (or null)
     */
    private Integer generationId;
    
    /**
     * Level that generated this graph in fragment space exploration (or null)
     */
    private Integer level;
    
    /**
     * A statement defining how this item was generated.
     */
    private String generatingSource;
    
//------------------------------------------------------------------------------
    
    public CandidateLW(String uid, String name, String pathNameToFile)
    {
        this.uid = uid;
        this.name = name;
        this.pathNameToFile = pathNameToFile;
    }
    
//------------------------------------------------------------------------------
 
    public void setFitness(double fitness)
    {
        this.fitness = fitness;
    }
    
//------------------------------------------------------------------------------

    public double getFitness()
    {
        return fitness;
    }
    
//------------------------------------------------------------------------------

    public void setError(String error)
    {
        this.error = error;
    }
    
//------------------------------------------------------------------------------

    public String getError()
    {
        return error;
    }
    
//------------------------------------------------------------------------------

    public String getName()
    {
        return name;
    }
    
//------------------------------------------------------------------------------

    public String getUid()
    {
        return uid;
    }
    
//------------------------------------------------------------------------------

    public String getPathToFile()
    {
        return pathNameToFile;
    }

//------------------------------------------------------------------------------
    
    public boolean hasFitness()
    {
    	return fitness != null;
    }

//------------------------------------------------------------------------------
    
    public void setGeneration(int genId)
    {
	    generationId = genId;
	}
    
//------------------------------------------------------------------------------
    
    public int getGeneration()
    {
	    return generationId;
	}
    
//------------------------------------------------------------------------------
    
    public void setGeneratingSource(String source)
    {
        generatingSource = source;
    }
    
//------------------------------------------------------------------------------
    
    public String getGeneratingSource()
    {
        return generatingSource;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Sets level that generated this graph in a fragment space 
     * exploration experiment.
     * @param lev the level index
     */
    public void setLevel(int lev)
    {
    	level = lev;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the level that generated this graph in a fragment space 
     * exploration experiment.
     * @return the level that generated this graph in a fragment space 
     * exploration experiment.
     */
	public int getLevel() 
	{
		return level;
	}
	
//------------------------------------------------------------------------------
	
	@Override
	public boolean equals(Object o)
	{
	    if (o== null)
            return false;
        
        if (o == this)
            return true;
        
        if (o.getClass() != getClass())
            return false;
         
        CandidateLW other = (CandidateLW) o;
        

        if (!this.name.equals(other.name))
            return false;
        
        if (!this.uid.equals(other.uid))
            return false;
        
        return true;
	}

//------------------------------------------------------------------------------        
    
}
