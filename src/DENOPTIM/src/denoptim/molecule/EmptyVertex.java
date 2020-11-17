package denoptim.molecule;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtomContainer;

/**
 * An empty vertex has the behaviours of a vertex, but has no molecular
 * within. It has attachment points, as well as relations between those
 * attachment points, but has no atoms.
 *
 */
public class EmptyVertex extends DENOPTIMVertex
{

    /**
     * Version UID
     */
    private static final long serialVersionUID = 1L;

    /*
     * attachment points on this vertex
     */
    private ArrayList<DENOPTIMAttachmentPoint> lstAPs;

    /**
     * List of AP sets that are related to each other, so that we
     * call them "symmetric" (though symmetry is a fuzzy concept here).
     */
    private ArrayList<SymmetricSet> lstSymAPs;


//------------------------------------------------------------------------------

    /**
     * Constructor for an empty vertex.
     */
    public EmptyVertex()
    {
        super(-1);
        lstAPs = new ArrayList<DENOPTIMAttachmentPoint>();
        lstSymAPs = new ArrayList<SymmetricSet>();
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for an identified vertex without attachment points.
     */
    public EmptyVertex(int id)
    {
        super(id);
        lstAPs = new ArrayList<DENOPTIMAttachmentPoint>();
        lstSymAPs = new ArrayList<SymmetricSet>();
    }

  //------------------------------------------------------------------------------

    /**
     * Constructor for an identified vertex with attachment points.
     */
    public EmptyVertex(int id, ArrayList<DENOPTIMAttachmentPoint> lstAPs)
    {
        super(id);
        this.lstAPs = lstAPs;
        this.lstSymAPs = new ArrayList<SymmetricSet>();
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for an identified vertex.
     */
    public EmptyVertex(int id, ArrayList<DENOPTIMAttachmentPoint> lstAPs,
            ArrayList<SymmetricSet> lstSymAPs, boolean isRCV)
    {
        super(id);
        this.lstAPs = lstAPs;
        this.lstSymAPs = lstSymAPs;
        setAsRCV(isRCV);
    }

//------------------------------------------------------------------------------

    public ArrayList<DENOPTIMAttachmentPoint> getAttachmentPoints()
    {
        return lstAPs;
    }

//------------------------------------------------------------------------------

    public void setSymmetricAP(ArrayList<SymmetricSet> m_Sap)
    {
        lstSymAPs = m_Sap;
    }

//------------------------------------------------------------------------------

    public ArrayList<SymmetricSet> getSymmetricAP()
    {
        return lstSymAPs;
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

    @Override
    public SymmetricSet getSymmetricAPs(int m_dapidx)
    {
        for (SymmetricSet symmetricSet : lstSymAPs)
        {
            if (symmetricSet.contains(m_dapidx))
            {
                return symmetricSet;
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
                lstAvailableAP.add(i);
        }
        return lstAvailableAP;
    }

//------------------------------------------------------------------------------

    public int getFreeAPCount()
    {
        int n = 0;
        for (DENOPTIMAttachmentPoint denoptimAttachmentPoint : lstAPs) {
            if (denoptimAttachmentPoint.isAvailable())
                n++;
        }
        return n;
    }

//------------------------------------------------------------------------------

    public boolean hasFreeAP()
    {
        for (DENOPTIMAttachmentPoint denoptimAttachmentPoint : lstAPs) {
            if (denoptimAttachmentPoint.isAvailable())
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

    /**
     *
     * @return <code>true</code> if vertex has symmetric APs
     */

    public boolean hasSymmetricAP()
    {
        return !lstSymAPs.isEmpty();
    }

//------------------------------------------------------------------------------

    @Override
    public String toString()
    {
        return getVertexId() + "_EmptyVertex";
    }

//------------------------------------------------------------------------------

    public void cleanup()
    {
        if (lstSymAPs != null)
        {
            lstSymAPs.clear();
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
    public EmptyVertex clone()
    {
        ArrayList<DENOPTIMAttachmentPoint> cLstAPs =
                new ArrayList<DENOPTIMAttachmentPoint>();
        for (DENOPTIMAttachmentPoint ap : lstAPs)
        {
            cLstAPs.add(ap.clone());
        }

        ArrayList<SymmetricSet> cLstSymAPs = new ArrayList<SymmetricSet>();
        for (SymmetricSet ss : lstSymAPs)
        {
            cLstSymAPs.add(ss.clone());
        }

        EmptyVertex c = new EmptyVertex(getVertexId(),cLstAPs,cLstSymAPs,isRCV());
        c.setLevel(this.getLevel());
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

    @Override
    public boolean sameAs(DENOPTIMVertex other, StringBuilder reason)
    {
        if (this.getFreeAPCount() != other.getFreeAPCount())
        {
            reason.append("Different number of free APs ("
                    +this.getFreeAPCount()+":"
                    +other.getFreeAPCount()+"); ");
            return false;
        }

        if (this.getNumberOfAP() != other.getNumberOfAP())
        {
            reason.append("Different number of APs ("
                    +this.lstAPs.size()+":"
                    +other.getNumberOfAP()+"); ");
            return false;
        }


        for (DENOPTIMAttachmentPoint apT : this.lstAPs)
        {
            boolean found = false;
            for (DENOPTIMAttachmentPoint apO : other.getAttachmentPoints())
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

    /**
     *
     * @param cmpReac list of reactions of the source vertex attachment point
     * @return list of indices of the attachment points in vertex that has
     * the corresponding reaction
     */

    public ArrayList<Integer> getCompatibleClassAPIndex(
            String cmpReac
    ) {
        ArrayList<DENOPTIMAttachmentPoint> apLst = getAttachmentPoints();
        ArrayList<Integer> apIdx = new ArrayList<>();
        for (int i = 0; i < apLst.size(); i++)
        {
            DENOPTIMAttachmentPoint dap = apLst.get(i);
            if (dap.isAvailable())
            {
                // check if this AP has the compatible reactions
                String dapReac = dap.getAPClass().toString();
                if (dapReac.compareToIgnoreCase(cmpReac) == 0)
                {
                    apIdx.add(i);
                }
            }
        }
        return apIdx;
    }

//------------------------------------------------------------------------------

    public int getHeavyAtomsCount()
    {
        return 0;
    }

//------------------------------------------------------------------------------

    public boolean containsAtoms()
    {
        return false;
    }
    

//-----------------------------------------------------------------------------

      @Override
      public IAtomContainer getIAtomContainer()
      {
          return null;
      }

//-----------------------------------------------------------------------------

    /**
     * Returns the list of all APClasses on present on this fragment.
     * @return the list of APClassess
     */

    public ArrayList<APClass> getAllAPClasses()
    {
        ArrayList<APClass> lst = new ArrayList<APClass>();
        for (DENOPTIMAttachmentPoint ap : lstAPs)
        {
            APClass apCls = ap.getAPClass();
            if (!lst.contains(apCls))
            {
                lst.add(apCls);
            }
        }
        return lst;
    }

//-----------------------------------------------------------------------------

    @Override
    public void setAttachmentPoints(ArrayList<DENOPTIMAttachmentPoint> lstAP)
    {
        this.lstAPs = lstAP;
    }

//-----------------------------------------------------------------------------

    @Override
    protected void setSymmetricAPSets(ArrayList<SymmetricSet> lstSymAPs)
    {
        this.lstSymAPs = lstSymAPs;
    }

//-----------------------------------------------------------------------------

    @Override
    public ArrayList<SymmetricSet> getSymmetricAPSets()
    {
        return lstSymAPs;
    }

//------------------------------------------------------------------------------

    @Override
    public Set<DENOPTIMVertex> getMutationSites()
    {
        Set<DENOPTIMVertex> set = new HashSet<DENOPTIMVertex>();
        set.add(this);
        return set;
    }

//------------------------------------------------------------------------------

}
