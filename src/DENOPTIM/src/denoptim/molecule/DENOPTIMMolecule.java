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

package denoptim.molecule;

import java.io.Serializable;
import java.io.File;

import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.utils.GraphConversionTool;


/**
 * A molecular object with additional data and tags. Additional data includes
 * the DENOPTIM graph representation, fitness/error, and possibly other stuff.
 */

//TODO-V3 rename to Candidate

public class DENOPTIMMolecule implements Comparable<DENOPTIMMolecule>, 
Serializable, Cloneable
{
    /**
	 * Version UID
	 */
	private static final long serialVersionUID = -3132192038061270220L;

	/**
     * Graph representation
     */
    private DENOPTIMGraph graph;
    
    /**
     * Unique identifier
     */
    private String uid;
	
    /**
     * SMILES representation
     */
    private String smiles;
    
    /**
     * fitness value
     */
    private double fitness;
    
    /**
     * Pathname to SDF file
     */
    private String sdfFile;

    /**
     * Pathname to an image (i.e., PNG file)
     */
    private String imgFile;
    
    /**
     * Any comments
     */
    private String comment;
    
    /**
     * Error that prevented calculation of the fitness
     */
    private String error;
    
    /**
     * Flag signalling the presence of a fitness value associated
     */
    private boolean hasFitness;
    
    /**
     * ID of the generation this molecule belong to (or -1)
     */
    private int generationId = -1;
    
    /**
     * Name of this candidate (not guaranteed to be unique)
     */
    private String name;
    
    /**
     * Level that generated this graph in fragment space exploration
     */
    private int level;

    
//------------------------------------------------------------------------------

    public DENOPTIMMolecule()
    {
        uid = "UNDEFINED";
        smiles = "UNDEFINED";
        fitness = 0; //This is stupid... only needed by compareTo. TODO: change!
        hasFitness = false;
    }
    
//------------------------------------------------------------------------------
    
    //TODO-V3: reorder to make consistent
    public DENOPTIMMolecule(DENOPTIMGraph m_molGraph, String m_molUID, 
                            String m_molSmiles, double m_molFitness)
    {
        graph = m_molGraph;
        uid = m_molUID;
        smiles = m_molSmiles;
        fitness = m_molFitness;
        hasFitness = true;
   }

//------------------------------------------------------------------------------
	
    //TODO-V3: reorder to make consistent
    public DENOPTIMMolecule(String m_molFile, DENOPTIMGraph m_molGraph, 
                            String m_molUID, String m_molSmiles, 
                            double m_molFitness)
    {
        graph = m_molGraph;
        uid = m_molUID;
        smiles = m_molSmiles;
        fitness = m_molFitness;
        sdfFile = m_molFile;
        hasFitness = true;
    }
//------------------------------------------------------------------------------

    public DENOPTIMMolecule(String name, DENOPTIMGraph graph, String uid, 
              String smiles, String molFile, String imgFile,
              String comment, int generationId, int level)
    {
        this.name = name;
        this.graph = graph;
        this.uid = uid;
        this.smiles = smiles;
        this.sdfFile = molFile;
        this.imgFile = imgFile;
        this.comment = comment;
        this.generationId = generationId;
        this.level = level; 
    }
    
//------------------------------------------------------------------------------
    
    public DENOPTIMMolecule(IAtomContainer iac, boolean useFragSpace) 
    		throws DENOPTIMException
    {
    	this(iac, useFragSpace, false);
    }
    
//------------------------------------------------------------------------------
    
    public DENOPTIMMolecule(IAtomContainer iac, boolean useFragSpace, 
    		boolean allowNoUID) throws DENOPTIMException
    {
    	// Initialize, then we try to take info from IAtomContainer
        this.uid = "UNDEFINED";
        this.smiles = "UNDEFINED";
        this.fitness = 0; //This is stupid... only needed by compareTo. TODO: change!
        this.hasFitness = false;
		
		this.name = "noname";
		if (iac.getProperty(CDKConstants.TITLE) != null)
		{
			this.name = iac.getProperty(CDKConstants.TITLE).toString();
		}
		
        if (iac.getProperty(DENOPTIMConstants.MOLERRORTAG) != null)
        {
        	this.error = iac.getProperty(
        			DENOPTIMConstants.MOLERRORTAG).toString();
        }

        if (iac.getProperty(DENOPTIMConstants.FITNESSTAG) != null)
        {
            String fitprp = iac.getProperty(
            		DENOPTIMConstants.FITNESSTAG).toString();
            double fitVal = Double.parseDouble(fitprp);
            if (Double.isNaN(fitVal))
            {
                String msg = "Cannot build DENOPTIMMolecule from "
                		+ "IAtomContainer: Fitness value is NaN!";
                throw new DENOPTIMException(msg);
            }
            this.fitness = fitVal;
            this.hasFitness = true;
        }
        
        if (iac.getProperty(DENOPTIMConstants.GRAPHLEVELTAG) != null)
        {
        	this.level = Integer.parseInt(iac.getProperty(
        			DENOPTIMConstants.GRAPHLEVELTAG).toString());
        }
        
        if (iac.getProperty(DENOPTIMConstants.SMILESTAG) != null)
        {
        	this.smiles = iac.getProperty(
        			DENOPTIMConstants.SMILESTAG).toString();
        }

        try
        {
            this.uid = iac.getProperty(
            		DENOPTIMConstants.UNIQUEIDTAG).toString();
        } catch (Exception e) {
        	if (allowNoUID)
        	{
        		this.uid = "noUID";
        	} else {
        		throw new DENOPTIMException("Could not read UID to make "
        				+ "DENOPTIMMolecule.", e);
        	}
        }
        
        try
        {
            //Something very similar is done also in DenoptimIO
            Object graphEnc = iac.getProperty(DENOPTIMConstants.GRAPHTAG);
            Object json = iac.getProperty(DENOPTIMConstants.GRAPHJSONTAG);
            if (graphEnc == null && json == null) {
                throw new DENOPTIMException("Attempt to load graph to "
                        + "DENOPTIMMolecule but the IAtomContainer "
                        + "has neither '" + DENOPTIMConstants.GRAPHTAG
                        + "' nor '" + DENOPTIMConstants.GRAPHJSONTAG);
            } else if (json != null) {
                String js = json.toString();
                this.graph = DENOPTIMGraph.fromJson(js);
            } else {
                this.graph = GraphConversionTool.getGraphFromString(
                        graphEnc.toString().trim(), useFragSpace);
            }
        } catch (Exception e) {
        	throw new DENOPTIMException("Could not read Graph to make "
        			+ "DENOPTIMMolecule.", e);
        }
        if (iac.getProperty(DENOPTIMConstants.GMSGTAG) != null)
        {
            this.comment = iac.getProperty(
            		DENOPTIMConstants.GMSGTAG).toString();
        }
    }

//------------------------------------------------------------------------------
    
    public void setComments(String str)
    {
        this.comment = str;
    }
    
//------------------------------------------------------------------------------
    
    public String getComments()
    {
        return comment;
    }    
	
//------------------------------------------------------------------------------

    public void setSDFFile(String molFile)
    {
        this.sdfFile = molFile;
    }	
    
//------------------------------------------------------------------------------

    public void setImageFile(String imgFile)
    {
        this.imgFile = imgFile;
    }

//------------------------------------------------------------------------------

    public void setGraph(DENOPTIMGraph graph)
    {
        this.graph = graph;
    }

//------------------------------------------------------------------------------

    public void setUID(String uid)
    {
        this.uid = uid;
    }

//------------------------------------------------------------------------------

    public void setSmiles(String smiles)
    {
        this.smiles = smiles;
    }
   
//------------------------------------------------------------------------------

    public void setFitness(double fitness)
    {
        this.fitness = fitness;
        this.hasFitness = true;
    }
    
//------------------------------------------------------------------------------

    public void setError(String error)
    {
        this.error = error;
    }
    
//------------------------------------------------------------------------------

    public void setName(String name)
    {
        this.name = name;
    }
    
//------------------------------------------------------------------------------

    public String getName()
    {
        return name;
    }
    
//------------------------------------------------------------------------------

    public String getError()
    {
        return error;
    }
    
//------------------------------------------------------------------------------

    public String getUID()
    {
        return uid;
    }

//------------------------------------------------------------------------------

    public String getSmiles()
    {
        return smiles;
    }
    
//------------------------------------------------------------------------------

    public String getSDFFile()
    {
        return sdfFile;
    }
    
//------------------------------------------------------------------------------

    public String getImageFile()
    {
        return imgFile;
    }

//------------------------------------------------------------------------------

    public DENOPTIMGraph getGraph()
    {
        return graph;
    }

//------------------------------------------------------------------------------

    public double getFitness()
    {
        return fitness;
    }
    
//------------------------------------------------------------------------------
    
    public boolean hasFitness()
    {
    	return hasFitness;
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

    // The compareTo method compares the receiving object with the specified 
    // object and returns a negative integer, 0, or a positive integer depending 
    // on whether the receiving object is less than, equal to, or greater than 
    // the specified object.

    @Override
    public int compareTo(DENOPTIMMolecule other)
    {
        if (this.fitness > other.fitness)
            return 1;
        else if (this.fitness < other.fitness)
            return -1;
        return 0;
    }

//------------------------------------------------------------------------------

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(16);
        String mname = new File(sdfFile).getName();
        if (mname != null)
            sb.append(String.format("%-20s", mname));
        
        sb.append(String.format("%-20s", this.graph.getGraphId()));
        sb.append(String.format("%-30s", uid));
        sb.append(String.format("%12.3f", fitness));

        return sb.toString();
    }

//------------------------------------------------------------------------------    
    
    public void cleanup()
    {
        if (graph != null)
            graph.cleanup();
    }

//------------------------------------------------------------------------------        

    public DENOPTIMMolecule clone()
    {
        DENOPTIMMolecule c = new DENOPTIMMolecule(name, graph.clone(), uid,
                smiles, sdfFile, imgFile, comment, generationId,level);
        if (hasFitness)
        {
            c.setFitness(fitness);
        }
        if (error!=null)
        {
            c.setError(error);
        }
        return c;
    }

//------------------------------------------------------------------------------        
    
}
