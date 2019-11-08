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
import java.io.Serializable;

import denoptim.constants.DENOPTIMConstants;


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
     * store the integer id associated with the scaffold/fragment/capping
     */
    private int molId;

    /*
     * attachment points for this Mol
     */
    private ArrayList<DENOPTIMAttachmentPoint> lstAP;

    /*
     * 0-scaffold, 1-special fragment, 2-capping group
     */
    private int fragmentType;

    /*
     * While growing the graph, we associate a level with each vertex where the
     * scaffold has a level -1, while each layer adds 1
     */
    int recursiveLevel;

    /*
     * list of APs that behave in a similar manner when fragments are attached
     * i.e. mirror the operation performed on symmetric set of APs
     */
    private ArrayList<SymmetricSet> lstSymmAP;

    /*
     * Flag indicating this as a ring closing vertex
     */
    private boolean isRCV;


//------------------------------------------------------------------------------

    public DENOPTIMVertex()
    {
        molId = 0;
        lstAP = new ArrayList<>();
        vertexId = 0;
        fragmentType = 0;
        lstSymmAP = new ArrayList<>();
        isRCV = false;
    }

//------------------------------------------------------------------------------

    public DENOPTIMVertex(int m_vid, int m_molId,
            ArrayList<DENOPTIMAttachmentPoint> m_lstAP, int m_fragmentType)
    {
        molId = m_molId;
        lstAP = m_lstAP;
        vertexId = m_vid;
        fragmentType = m_fragmentType;
        lstSymmAP = new ArrayList<>();
        isRCV = false;
	if (lstAP.size()==1 && DENOPTIMConstants.RCAAPCLASSSET.contains(
	    lstAP.get(0).getAPClass()))
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
        return fragmentType;
    }

//------------------------------------------------------------------------------

    /**
     *
     * @return the id of the molecule
     */
    public int getMolId()
    {
        return molId;
    }

//------------------------------------------------------------------------------

    public void setMolId(int m_molId)
    {
        molId = m_molId;
    }

//------------------------------------------------------------------------------

    public ArrayList<DENOPTIMAttachmentPoint> getAttachmentPoints()
    {
        return lstAP;
    }

//------------------------------------------------------------------------------

    public void setAttachmentPoints(ArrayList<DENOPTIMAttachmentPoint> m_lstAP)
    {
        lstAP = m_lstAP;
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
     * TODO: one day change the name of this method!
     * @param m_dapidx
     * @return the list of attahcment point IDs, which include 
     * <code>m_dapidx</code> or <code>null</code> if no partners present
     */

    public SymmetricSet getPartners(int m_dapidx)
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
        return lstAP.size();
    }

//------------------------------------------------------------------------------

    /**
     *
     * @return list of attachment points that have free valences
     */

    public ArrayList<Integer> getFreeAPList()
    {
        ArrayList<Integer> lstAvailableAP = new ArrayList<>();
        for (int i=0; i<lstAP.size(); i++)
        {
            if (lstAP.get(i).isAvailable())
                lstAvailableAP.add(new Integer(i));
        }
        return lstAvailableAP;
    }

//------------------------------------------------------------------------------

    public int getFreeAPCount()
    {
        int n = 0;
        for (int i=0; i<lstAP.size(); i++)
        {
            if (lstAP.get(i).isAvailable())
                n++;
        }
        return n;
    }


//------------------------------------------------------------------------------

    public boolean hasFreeAP()
    {
        for (int i=0; i<lstAP.size(); i++)
        {
            if (lstAP.get(i).isAvailable())
                return true;
        }
        return false;
    }

//------------------------------------------------------------------------------

    public void updateAttachmentPoint(int idx, int delta)
    {
        lstAP.get(idx).updateAPConnections(delta);
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
        sb.append(vertexId).append("_").append((molId+1)).append("_").
                    append(fragmentType).append("_").append(recursiveLevel);
        return sb.toString();
    }

//------------------------------------------------------------------------------
    
    public void cleanup()
    {
        if (lstSymmAP != null)
        {
            lstSymmAP.clear();
        }
        if (lstAP != null)
        {
            lstAP.clear();
        }
    }
    
    
//------------------------------------------------------------------------------    
}
