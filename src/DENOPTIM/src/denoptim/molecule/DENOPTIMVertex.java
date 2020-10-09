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

import denoptim.utils.RandomUtils;
import org.apache.commons.math3.random.MersenneTwister;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.io.Serializable;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.logging.DENOPTIMLogger;
import denoptim.utils.GraphUtils;

/**
 * A vertex is a data structure that has an identity and holds a 
 * list of attachment points. The attachment points can be related by
 * an here-undefined relation.
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class DENOPTIMVertex implements Cloneable, Serializable
{
    /**
     * Version UID
     */
    private static final long serialVersionUID = -6093013990421027436L;

    /**
     * Unique identifier associated with the vertex instance
     */
    private int vertexId;
    
    /*
     * attachment points on this vertex
     */
    private ArrayList<DENOPTIMAttachmentPoint> lstAPs;

    /**
     * List of AP sets that are related to each other, so that we 
     * call them "symmetric" (though symmetry is a fuzzy concept here). 
     */
    private ArrayList<SymmetricSet> lstSymAPs;

    /*
     * Flag indicating that this as a ring closing vertex
     */
    private boolean isRCV;
    
    //TODO-V3 remove: get it from the graph
    /*
     * if the level at which this vertex is in a graph
     */
    private int recursiveLevel = -99; //Initialised to meaningless value


//------------------------------------------------------------------------------

    /**
     * Constructor for an empty vertex.
     */
    public DENOPTIMVertex()
    {
        vertexId = -1;
        lstAPs = new ArrayList<DENOPTIMAttachmentPoint>();
        lstSymAPs = new ArrayList<SymmetricSet>();
        isRCV = false;
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for an identified vertex without attachment points.
     */
    public DENOPTIMVertex(int id)
    {
        vertexId = id;
        lstAPs = new ArrayList<DENOPTIMAttachmentPoint>();
        lstSymAPs = new ArrayList<SymmetricSet>();
        isRCV = false;
    }
    
  //------------------------------------------------------------------------------

    /**
     * Constructor for an identified vertex with attachment points.
     */
    public DENOPTIMVertex(int id, ArrayList<DENOPTIMAttachmentPoint> lstAPs)
    {
        this.vertexId = id;
        this.lstAPs = lstAPs;
        this.lstSymAPs = new ArrayList<SymmetricSet>();
        this.isRCV = false;
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for an identified vertex.
     */
    public DENOPTIMVertex(int id, ArrayList<DENOPTIMAttachmentPoint> lstAPs,
            ArrayList<SymmetricSet> lstSymAPs, boolean isRCV)
    {
        this.vertexId = id;
        this.lstAPs = lstAPs;
        this.lstSymAPs = lstSymAPs;
        this.isRCV = isRCV;
    }
    
//------------------------------------------------------------------------------

    /**
     * Builds a new molecular fragment kind of vertex.
     * @param bbId 0-based index of building block in the library
     * @param bbType the type of building block 0:scaffold, 1:fragment, 
     * 2:capping group
     */
    public static DENOPTIMVertex newFragVertex(int bbId, int bbType)
    {
        return newFragVertex(GraphUtils.getUniqueVertexIndex(), bbId, bbType);
    }
    
//------------------------------------------------------------------------------

    /**
     * Builds a new molecular fragment kind of vertex.
     * @param vertexId unique identified of the vertex
     * @param bbId 0-based index of building block in the library
     * @param bbType the type of building block 0:scaffold, 1:fragment, 2:capping group
     */
    public static DENOPTIMVertex newFragVertex(int vertexId, int bbId, int bbType)
    {   
        DENOPTIMVertex v = new DENOPTIMVertex();
        try
        {
            v = FragmentSpace.getVertexFromLibrary(bbType,bbId).clone();
        } catch (DENOPTIMException e)
        {
            e.printStackTrace();
            String msg = "Fatal error! Cannot continue. " + e.getMessage();
            DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
            System.exit(0);
        }
        v.setVertexId(vertexId);
        
        v.setAsRCV(v.getNumberOfAP() == 1 
                && DENOPTIMConstants.RCAAPCLASSSET.contains(
                        v.getAttachmentPoints().get(0).getAPClass()));
        
        return v;
    }

//------------------------------------------------------------------------------

    public ArrayList<DENOPTIMAttachmentPoint> getAttachmentPoints()
    {
        return lstAPs;
    }

//------------------------------------------------------------------------------

    /**
     * Append and attachment point to the list of attachment points on this 
     * vertex.
     * @param ap the attachment point to add
     */
    protected void addAttachmentPoint(DENOPTIMAttachmentPoint ap)
    {
        lstAPs.add(ap);
    }
    
//------------------------------------------------------------------------------

    //TODO-V3: should be protected? Now it's public to allow refilling of AP list
    // as done in GraphConversionTool.getGraphFromString to recover at least
    // some of the entire list of APs of a vertex read-in from a string
    // representation of a graph. Tha's obviously not ideal, so eventually
    // we must get rid of it and move this back to protected.
    public void setAttachmentPoints(ArrayList<DENOPTIMAttachmentPoint> m_lstAP)
    {
        lstAPs = m_lstAP;
    }
    
//------------------------------------------------------------------------------
    
    protected void setAsRCV(boolean isRCV)
    {
        this.isRCV = isRCV;
    }
    
//------------------------------------------------------------------------------

    public void setVertexId(int id)
    {
        this.vertexId = id;
    }

//------------------------------------------------------------------------------

    public int getVertexId()
    {
        return vertexId;
    }

//------------------------------------------------------------------------------

    protected void setSymmetricAPSets(ArrayList<SymmetricSet> m_Sap)
    {
        lstSymAPs = m_Sap;
    }

//------------------------------------------------------------------------------

    public ArrayList<SymmetricSet> getSymmetricAPSets()
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
        return !lstSymAPs.isEmpty();
    }

//------------------------------------------------------------------------------

    @Override
    public String toString()
    {
        return vertexId + "_DUMMY_STRING";
        //return vertexId + "_" + (buildingBlockId + 1) + "_" +
        //        buildingBlockType + "_" + recursiveLevel;
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
    
    @Override
    public DENOPTIMVertex clone()
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
        DENOPTIMVertex clone = new DENOPTIMVertex(vertexId,cLstAPs,cLstSymAPs,isRCV);
        clone.setLevel(this.getLevel());
        
        return clone;
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
                String dapReac = dap.getAPClass();
                if (dapReac.compareToIgnoreCase(cmpReac) == 0)
                {
                    apIdx.add(i);
                }
            }
        }
        return apIdx;
    }
    
//------------------------------------------------------------------------------


    //TODO-V3 remove this tmp stuff

    @Deprecated
    public int getFragmentType()
    {
        System.err.println("ERROR! Attempt to get fragType from vertex");
        return -999;
    }

//------------------------------------------------------------------------------

    //TODO-V3 remove this tmp stuff
    
    @Deprecated
    public int getMolId()
    {
        System.err.println("ERROR! Attempt to get molId from vertex");
        return -999;
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
    
//------------------------------------------------------------------------------

    //TODO-V3 overload this in subclasses
    public IAtomContainer getIAtomContainer()
    {
        // TODO Auto-generated method stub
        return null;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the list of all APClasses on present on this fragment.
     * @return the list of APClassess
     */
    
    public ArrayList<String> getAllAPClasses()
    {
        ArrayList<String> lst = new ArrayList<String>();
        for (DENOPTIMAttachmentPoint ap : lstAPs)
        {
            String apCls = ap.getAPClass();
            if (!lst.contains(apCls))
            {
                lst.add(apCls);
            }
        }
        return lst;
    }
    
//------------------------------------------------------------------------------

    /**
     * Connects this vertex to target by an edge based on reaction type.
     * @param target target vertex
     * @param sourceAPIndex index of Attachment point in source vertex
     * @param targetAPIndex index of Attachment point in target vertex
     * @param srcRcn the reaction scheme at the source
     * @param trgRcn the reaction scheme at the target
     * @return DENOPTIMEdge
     */

    public DENOPTIMEdge connectVertices(DENOPTIMVertex target,
                                        int sourceAPIndex,
                                        int targetAPIndex,
                                        String srcRcn,
                                        String trgRcn
    ) {
        //System.err.println("Connecting vertices RCN");
        DENOPTIMAttachmentPoint sourceAP = getAttachmentPoints()
                .get(sourceAPIndex);
        DENOPTIMAttachmentPoint targetAP = target.getAttachmentPoints()
                .get(targetAPIndex);

        if (sourceAP.isAvailable() && targetAP.isAvailable())
        {
            //System.err.println("Available APs");
            String rname = trgRcn.substring(0, trgRcn.indexOf(':'));

            // look up the reaction bond order table
            int bndOrder = FragmentSpace.getBondOrderMap().get(rname);
            //System.err.println("Bond: " + bndOrder + " " + srcRcn + " " + trgRcn);

            // create a new edge
            DENOPTIMEdge edge = new DENOPTIMEdge(getVertexId(),
                    target.getVertexId(),
                    sourceAPIndex,
                    targetAPIndex,
                    bndOrder
            );
            edge.setSourceReaction(srcRcn);
            edge.setTargetReaction(trgRcn);

            // update the attachment point info
            sourceAP.updateFreeConnections(-bndOrder); // decrement the connections
            targetAP.updateFreeConnections(-bndOrder); // decrement the connections

            return edge;
        } else {
            System.err.println("ERROR! Attempt to make edge using unavailable AP!"
                    + System.getProperty("line.separator")
                    + "Vertex: "+getVertexId()
                    +" AP-A(available:"+sourceAP.isAvailable()+"): "+sourceAP
                    + System.getProperty("line.separator")
                    + "Vertex: "+target.getVertexId()
                    +" AP-B(available:"+targetAP.isAvailable()+"): "+targetAP);
        }
        return null;
    }

//------------------------------------------------------------------------------

    /**
     * connects this vertex to other based on their free AP connections
     * @param other vertex
     * @return edge connecting the vertices
     */

    public DENOPTIMEdge connectVertices(DENOPTIMVertex other)
    {
        ArrayList<Integer> apA = getFreeAPList();
        ArrayList<Integer> apB = other.getFreeAPList();

        if (apA.isEmpty() || apB.isEmpty())
            return null;

        // select random APs - these are the indices in the list
        MersenneTwister rng = RandomUtils.getRNG();


        //int iA = apA.get(GAParameters.getRNG().nextInt(apA.size()));
        //int iB = apB.get(GAParameters.getRNG().nextInt(apB.size()));
        int iA = apA.get(rng.nextInt(apA.size()));
        int iB = apB.get(rng.nextInt(apB.size()));

        DENOPTIMAttachmentPoint dap_A = getAttachmentPoints().get(iA);
        DENOPTIMAttachmentPoint dap_B = other.getAttachmentPoints().get(iB);

        // if no reaction/class specific info available set to single bond
        int bndOrder = 1;

        // create a new edge
        DENOPTIMEdge edge = new DENOPTIMEdge(getVertexId(), other.getVertexId(),
                iA, iB, bndOrder);

        // update the attachment point info
        dap_A.updateFreeConnections(-bndOrder); // decrement the connections
        dap_B.updateFreeConnections(-bndOrder); // decrement the connections

        return edge;
    }
}
