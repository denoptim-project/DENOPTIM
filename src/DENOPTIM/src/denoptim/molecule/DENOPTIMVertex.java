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

package denoptim.molecule;

import java.util.ArrayList;
import java.util.logging.Level;
import java.io.Serializable;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.logging.DENOPTIMLogger;
import denoptim.utils.FragmentUtils;


/**
 * Data structure of a single vertex that contains information as to the
 * list of attachment points, type of this vertex and the index of the
 * molecular representation in a library of fragments of a given type
 * (scaffold, fragment, capping groups).
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class DENOPTIMVertex implements Cloneable, Serializable
{
    /*
     * unique id associated with the vertex
     */
    private int vertexId;

    /*
     * store the integer id associated with the building block
     */
    private int buildingBlockId;

    /*
     * attachment points for this Mol
     */
    private ArrayList<DENOPTIMAttachmentPoint> lstAPs;

    /*
     * Type of building block: 0:scaffold, 1:fragment, 2:capping group
     */
    private int buildingBlockType;

    /*
     * While growing the graph, we associate a level with each vertex where the
     * scaffold has a level -1, while each layer adds 1
     */
    int recursiveLevel;

    /*
     * list of APs that behave in a similar manner when fragments are attached,
     * i.e., mirror the operation performed on symmetric set of APs.
     */
    private ArrayList<SymmetricSet> lstSymmAP;

    /*
     * Flag indicating this as a ring closing vertex
     */
    private boolean isRCV;


//------------------------------------------------------------------------------

    public DENOPTIMVertex()
    {
        buildingBlockId = 0;
        lstAPs = new ArrayList<>();
        vertexId = 0;
        buildingBlockType = 0;
        lstSymmAP = new ArrayList<>();
        isRCV = false;
    }
    
//------------------------------------------------------------------------------

    /**
     * 
     * @param vertexId unique identified of the vertex
     * @param bbId 0-based index of building block in the library
     * @param bbType choose the type of building block 0:scaffold, 1:fragment, 2:capping group
     */
    public DENOPTIMVertex(int vertexId, int bbId, int bbType)
    {
        this.vertexId = vertexId;
        this.buildingBlockId = bbId;
        this.buildingBlockType = bbType;
        IGraphBuildingBlock bb = null;
        try
        {
            bb = FragmentSpace.getFragment(bbType,bbId).clone();
        } catch (DENOPTIMException e)
        {
            e.printStackTrace();
            String msg = "Fatal error! Cannot continue. " + e.getMessage();
            DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
            System.exit(0);
        }
        this.lstAPs = new ArrayList<DENOPTIMAttachmentPoint>();
        for (DENOPTIMAttachmentPoint ap : bb.getAPs())
        {
            this.lstAPs.add(ap.clone());
        }
        this.lstSymmAP = new ArrayList<SymmetricSet>();
        for (SymmetricSet ss : bb.getSymmetricAPsSets())
        {
            this.lstSymmAP.add(ss.clone());
        }
        isRCV = false;
        if (lstAPs.size()==1 && DENOPTIMConstants.RCAAPCLASSSET.contains(
            lstAPs.get(0).getAPClass()))
        {
            isRCV = true;
        }
    }    

//------------------------------------------------------------------------------

    @Deprecated
    public DENOPTIMVertex(int m_vid, int m_molId,
            ArrayList<DENOPTIMAttachmentPoint> m_lstAP, int m_fragmentType)
    {
        buildingBlockId = m_molId;
        lstAPs = m_lstAP;
        vertexId = m_vid;
        buildingBlockType = m_fragmentType;
        lstSymmAP = new ArrayList<>();
        isRCV = false;
		if (lstAPs.size()==1 && DENOPTIMConstants.RCAAPCLASSSET.contains(
		    lstAPs.get(0).getAPClass()))
		{
		    isRCV = true;
		}
    }

//------------------------------------------------------------------------------

    /**
     *
     * @return <code>true</code> if vertex is a fragment
     */

    public int getFragmentType()
    {
        return buildingBlockType;
    }

//------------------------------------------------------------------------------

    /**
     *
     * @return the id of the molecule
     */
    public int getMolId()
    {
        return buildingBlockId;
    }

//------------------------------------------------------------------------------

    public void setMolId(int m_molId)
    {
        buildingBlockId = m_molId;
    }

//------------------------------------------------------------------------------

    public ArrayList<DENOPTIMAttachmentPoint> getAttachmentPoints()
    {
        return lstAPs;
    }

//------------------------------------------------------------------------------

    public void setAttachmentPoints(ArrayList<DENOPTIMAttachmentPoint> m_lstAP)
    {
        lstAPs = m_lstAP;
    }

//------------------------------------------------------------------------------

    public void setVertexId(int m_vid)
    {
        vertexId = m_vid;
    }

//------------------------------------------------------------------------------

    public int getVertexId()
    {
        return vertexId;
    }

//------------------------------------------------------------------------------

    public void setSymmetricAP(ArrayList<SymmetricSet> m_Sap)
    {
        lstSymmAP = m_Sap;
    }

//------------------------------------------------------------------------------

    public ArrayList<SymmetricSet> getSymmetricAP()
    {
        return lstSymmAP;
    }

//------------------------------------------------------------------------------

    /**
     * For the given attachment point index locate the symmetric partners
     * i.e. those with similar environments and class types.
     * @param m_dapidx inded of the attachment point which we want to get
     * the symmetrically related partners of.
     * @return the list of attachment point IDs, which include 
     * <code>m_dapidx</code> or <code>null</code> if no partners present
     */

    public SymmetricSet getSymmetricAPs(int m_dapidx)
    {
        for (int i=0; i<lstSymmAP.size(); i++)
        {
            ArrayList<Integer> lst = lstSymmAP.get(i).getList();
            if (lstSymmAP.get(i).contains(Integer.valueOf(m_dapidx)))
            {
                return lstSymmAP.get(i);
            }
        }
        return null;
    }

//------------------------------------------------------------------------------

    public int getNumberOfAP()
    {
        return lstAPs.size();
    }

//------------------------------------------------------------------------------

    /**
     *
     * @return list of attachment points that have free valences
     */

    public ArrayList<Integer> getFreeAPList()
    {
        ArrayList<Integer> lstAvailableAP = new ArrayList<>();
        for (int i=0; i<lstAPs.size(); i++)
        {
            if (lstAPs.get(i).isAvailable())
                lstAvailableAP.add(new Integer(i));
        }
        return lstAvailableAP;
    }

//------------------------------------------------------------------------------

    public int getFreeAPCount()
    {
        int n = 0;
        for (int i=0; i<lstAPs.size(); i++)
        {
            if (lstAPs.get(i).isAvailable())
                n++;
        }
        return n;
    }


//------------------------------------------------------------------------------

    public boolean hasFreeAP()
    {
        for (int i=0; i<lstAPs.size(); i++)
        {
            if (lstAPs.get(i).isAvailable())
                return true;
        }
        return false;
    }

//------------------------------------------------------------------------------

    public void updateAttachmentPoint(int idx, int delta)
    {
        lstAPs.get(idx).updateFreeConnections(delta);
    }

//------------------------------------------------------------------------------

    public void setLevel(int m_level)
    {
        recursiveLevel = m_level;
    }

//------------------------------------------------------------------------------

    public int getLevel()
    {
        return recursiveLevel;
    }

//------------------------------------------------------------------------------

    /**
     *
     * @return <code>true</code> if vertex is a ring closing vertex
     */

    public boolean isRCV()
    {
        return isRCV;
    }

//------------------------------------------------------------------------------

    /**
     *
     * @return <code>true</code> if vertex has symmetric APs
     */

    public boolean hasSymmetricAP()
    {
        if (lstSymmAP.isEmpty())
            return false;
        return true;
    }

//------------------------------------------------------------------------------

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(64);
        sb.append(vertexId).append("_").append((buildingBlockId+1)).append("_").
                    append(buildingBlockType).append("_").append(recursiveLevel);
        return sb.toString();
    }

//------------------------------------------------------------------------------
    
    public void cleanup()
    {
        if (lstSymmAP != null)
        {
            lstSymmAP.clear();
        }
        if (lstAPs != null)
        {
            lstAPs.clear();
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns a deep-copy of this vertex
     * @return a deep-copy
     */
    public DENOPTIMVertex clone()
    {
        DENOPTIMVertex c = new DENOPTIMVertex(vertexId, buildingBlockId, 
                buildingBlockType);
        return c;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Compares this and another vertex ignoring vertex IDs.
     * @param other
     * @param reason string builder used to build the message clarifying the 
     * reason for returning <code>false</code>.
     * @return <code>true</code> if the two vertexes represent the same graph
     * node even if the vertex IDs are different.
     */
    public boolean sameAs(DENOPTIMVertex other, StringBuilder reason)
    {
    	if (this.getFragmentType() != other.getFragmentType())
    	{
    		reason.append("Different fragment type ("+this.getFragmentType()+":"
					+other.getFragmentType()+"); ");
    		return false;
    	}
    	
    	if (this.getMolId() != other.getMolId())
    	{
    		reason.append("Different molID ("+this.getMolId()+":"
					+other.getMolId()+"); ");
    		return false;
    	}
    	
    	if (this.getFreeAPCount() != other.getFreeAPCount())
    	{
    		reason.append("Different number of free APs ("
    				+this.getFreeAPCount()+":"
					+other.getFreeAPCount()+"); ");
    		return false;
    	}
    	
    	if (this.lstAPs.size() != other.lstAPs.size())
    	{
    		reason.append("Different number of APs ("
    				+this.lstAPs.size()+":"
					+other.lstAPs.size()+"); ");
    		return false;
    	}
    	

    	for (DENOPTIMAttachmentPoint apT : this.lstAPs)
    	{
    		boolean found = false;
    		for (DENOPTIMAttachmentPoint apO : other.lstAPs)
        	{
		    	if (apT.equals(apO))
		    	{
		    		found = true;
		    		break;
		    	}
        	}
    		if (!found)
    		{
    			reason.append("No corresponding AP for "+apT);
    			return false;
    		}
    	}
    	
    	return true;
    }
    
//------------------------------------------------------------------------------    
}
