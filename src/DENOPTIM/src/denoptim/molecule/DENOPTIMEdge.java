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

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSerializationContext;

import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IBond.Order;

import denoptim.fragspace.FragmentSpace;
import denoptim.utils.GenUtils;

/**
 * This class represents the edge between two vertices.
 */

public class DENOPTIMEdge implements Serializable
{
    /**
     * Attachment point at source end
     */
    private DENOPTIMAttachmentPoint srcAP;

    /**
     * Attachment point at target end
     */
    private DENOPTIMAttachmentPoint trgAP;

    /**
     * The bond type associated with the connection between the fragments
     */
    private BondType bondType = BondType.UNDEFINED;


//------------------------------------------------------------------------------
    
    /**
     * Constructor for an edge that connects two APs. The number of 
     * connections available in the APs is reduced upon creation of the edge 
     * and according the the bond type.
     * @param srcAP attachment point at source end
     * @param trgAP attachment point at target end
     * @param bondType defines what kind of bond type this edge should be 
     * converted to when converting a graph into a chemical representation.
     */
    
    public DENOPTIMEdge(DENOPTIMAttachmentPoint srcAP,
                          DENOPTIMAttachmentPoint trgAP, BondType bondType) {
        this.srcAP = srcAP;
        this.trgAP = trgAP;
        this.bondType = bondType;
        this.srcAP.updateFreeConnections(-bondType.getValence());
        this.trgAP.updateFreeConnections(-bondType.getValence());
        this.srcAP.setUser(this);
        this.trgAP.setUser(this);
    }
      
//------------------------------------------------------------------------------
      
    /**
     * Constructor for an edge that connects two APs. We assume a single bond.
     * The number of 
     * connections available in the APs is reduced upon creation of the edge 
     * and according the the bond type.
     * @param srcAP attachment point at source end
     * @param trgAP attachment point at target end
     */
    
    public DENOPTIMEdge(DENOPTIMAttachmentPoint srcAP,
                          DENOPTIMAttachmentPoint trgAP) {
        this(srcAP, trgAP, FragmentSpace.getBondOrderForAPClass(
                  srcAP.getAPClass().toString()));
    }
     
//------------------------------------------------------------------------------

    public DENOPTIMAttachmentPoint getSrcAP()
    {
        return srcAP;
    }
    
//------------------------------------------------------------------------------

    public DENOPTIMAttachmentPoint getTrgAP()
    {
        return trgAP;
    }
    
//------------------------------------------------------------------------------

    public int getSrcVertex()
    {
        return srcAP.getOwner().getVertexId();
    }
    
//------------------------------------------------------------------------------

    public int getSrcAPID()
    {
        return srcAP.getOwner().getIndexOfAP(srcAP);
    }
    
//------------------------------------------------------------------------------

    public int getTrgAPID()
    {
        return trgAP.getOwner().getIndexOfAP(trgAP);
    }        

//------------------------------------------------------------------------------

    public int getTrgVertex()
    {
        return trgAP.getOwner().getVertexId();
    }
    
//------------------------------------------------------------------------------

    //TODO-M7 del
    public void setSrcVertex(int vid)
    {
        srcAP.getOwner().setVertexId(vid);
    }

    public void setTrgVertex(int vid)
    {
        trgAP.getOwner().setVertexId(vid);
    }
//------------------------------------------------------------------------------
    
    public APClass getSrcAPClass()
    {
        return srcAP.getAPClass();
    }
    
//------------------------------------------------------------------------------
    
    public APClass getTrgAPClass()
    {
        return trgAP.getAPClass();
    }

//------------------------------------------------------------------------------

    public BondType getBondType()
    {
        return bondType;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Compares this and another edge ignoring edge and vertex IDs
     * @param other edge to compare against
     * @param reason string builder used to build the message clarifying the 
     * reason for returning <code>false</code>.
     * @return <code>true</code> if the two edges represent the same connection
     * even if the vertex IDs are different.
     */
    
//TODO-M7: use sameAs for APs
    public boolean sameAs(DENOPTIMEdge other, StringBuilder reason)
    {
    	if (this.getSrcAPID() != other.getSrcAPID())
    	{
    		reason.append("Different source atom ("+this.getSrcAPID()+":"
    						+other.getSrcAPID()+"); ");
    		return false;
    	}
    	if (this.getTrgAPID() != other.getTrgAPID())
    	{
    		reason.append("Different target atom ("+this.getTrgAPID()+":"
					+other.getTrgAPID()+"); ");
    		return false;
    	}
    	if (!this.getSrcAPClass().equals(other.getSrcAPClass()))
    	{
    		reason.append("Different source APClass ("
    				+this.getSrcAPClass()+":"
					+other.getSrcAPClass()+"); ");
    		return false;
    	}
    	if (!this.getTrgAPClass().equals(other.getTrgAPClass()))
    	{
    		reason.append("Different target APClass ("
    				+this.getTrgAPClass()+":"
					+other.getTrgAPClass()+"); ");
    		return false;
    	}
    	if (this.getBondType() != (other.getBondType()))
    	{
    		reason.append("Different bond type ("+this.getBondType()+":"
					+other.getBondType()+"); ");
    		return false;
    	}
    	return true;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Compares this and another edge ignoring the directionality of both, i.e.,
     * as if both edges were undirected. Ranking and comparison is based on an
     * invariant lexicographic string that combines, for each side of the edge, 
     * the following information:
     * <ol>
     * <li>type of the building block reached,</li>
     * <li>the ID of the building block,</li>
     * <li>the index of the attachment point.</li>
     * </ol>
     * Only for edges that link equivalent building blocks via the corresponding
     * APs (i.e., edges belonging to the same invariant class), the bond type
     * is considered as the final comparison criterion.
     * 
     * @param other edge to compare with this.
     * @return a negative integer, zero, or a positive integer as this object is
     * less than, equal to, or greater than the specified object.
     */
    public int compareAsUndirected(DENOPTIMEdge other)
    {
        DENOPTIMVertex tvA = srcAP.getOwner();
        DENOPTIMVertex tvB = trgAP.getOwner();
        DENOPTIMVertex ovA = other.srcAP.getOwner();
        DENOPTIMVertex ovB = other.trgAP.getOwner();
        
        String invariantTA = tvA.getBuildingBlockType().toOldInt() +
                GenUtils.getPaddedString(6,tvA.getBuildingBlockId()) +
                GenUtils.getPaddedString(4,srcAP.getIndexInOwner());
        
        String invariantTB = tvB.getBuildingBlockType().toOldInt() +
                GenUtils.getPaddedString(6,tvB.getBuildingBlockId()) +
                GenUtils.getPaddedString(4,trgAP.getIndexInOwner());
        
        String invariantOA = ovA.getBuildingBlockType().toOldInt() +
                GenUtils.getPaddedString(6,ovA.getBuildingBlockId()) +
                GenUtils.getPaddedString(4,other.srcAP.getIndexInOwner());
        
        String invariantOB = ovB.getBuildingBlockType().toOldInt() +
                GenUtils.getPaddedString(6,ovB.getBuildingBlockId()) +
                GenUtils.getPaddedString(4,other.trgAP.getIndexInOwner());
                
        String invariantThis = invariantTA + invariantTB;
        if (invariantTA.compareTo(invariantTB) > 0)
            invariantThis = invariantTB + invariantTA;
        
        String invariantOther = invariantOA + invariantOB;
        if (invariantOA.compareTo(invariantOB) > 0)
            invariantOther = invariantOB + invariantOA;
        
        int resultIgnoringBondType = invariantThis.compareTo(invariantOther);
        
        if (resultIgnoringBondType == 0)
        {
            return this.getBondType().compareTo(other.getBondType());
        } else {
            return resultIgnoringBondType;
        }
    }

//------------------------------------------------------------------------------

    @Override
    public String toString()
    {
        DENOPTIMVertex srcVertex = srcAP.getOwner();
        int srcAPID = this.getSrcAPID();
        DENOPTIMVertex trgVertex = trgAP.getOwner();
        int trgAPID = this.getTrgAPID();
        
        StringBuilder sb = new StringBuilder(64);
        sb.append(srcVertex.getVertexId()).append("_")
                .append(srcAPID).append("_").
                append(trgVertex.getVertexId()).append("_")
                .append(trgAPID).append("_").
                append(bondType.toOldString());
        if (srcAP.getAPClass()!=null && trgAP.getAPClass()!=null)
        {
            sb.append("_").append(srcAP.getAPClass()).append("_").append(
                    trgAP.getAPClass());
        }
        
        return sb.toString();
    }

//------------------------------------------------------------------------------

    /**
     * Exchanges source and target vertices and respective APs of this edge.
     */
    public void flipEdge() {
        DENOPTIMAttachmentPoint newTrgAP = getSrcAP();
        DENOPTIMAttachmentPoint newSrcAP = getTrgAP();
        DENOPTIMVertex newTrgVertex = newTrgAP.getOwner();
        DENOPTIMVertex newSrcVertex = newSrcAP.getOwner();
        srcAP = newSrcAP;
        trgAP = newTrgAP;
        setSrcVertex(newSrcVertex.getVertexId());
        setTrgVertex(newTrgVertex.getVertexId());
    }

//------------------------------------------------------------------------------

    /**
     * Possible chemical bond types an edge can represent.
     */
    public enum BondType {

        NONE, UNDEFINED, ANY, SINGLE, DOUBLE, TRIPLE, QUADRUPLE;

        //TODO-V3: this is to be consistent with old "int-based" internal
        // convention. Eventually, we'll not need this anymore.
        private String oldString = "1";

        private int valenceUsed = 0;

        private IBond.Order bo = null;

        static {
            ANY.bo = IBond.Order.SINGLE;
            SINGLE.bo = IBond.Order.SINGLE;
            DOUBLE.bo = IBond.Order.DOUBLE;
            TRIPLE.bo = IBond.Order.TRIPLE;
            QUADRUPLE.bo = IBond.Order.QUADRUPLE;

            SINGLE.valenceUsed = 1;
            DOUBLE.valenceUsed = 2;
            TRIPLE.valenceUsed = 3;
            QUADRUPLE.valenceUsed = 4;

            //TODO-V3 del
            SINGLE.oldString = "1";
            DOUBLE.oldString = "2";
            TRIPLE.oldString = "3";
            QUADRUPLE.oldString = "4";
        }

        /**
         * Checks if it is possible to convert this edge type into a CDK bond.
         * @return <code>true</code> if this can be converted to a CDK bond.
         */
        public boolean hasCDKAnalogue() {
            return (bo != null);
        }

        /**
         * @return the CDK {@link IBond.Order} represented by this edge type.
         */
        public Order getCDKOrder() {
            return bo;
        }

        /**
         * This method exists only to retain compatibility with old int-based
         * notation.
         * @return a string representation of the bond type
         */
        @Deprecated
        public String toOldString() {
            return oldString;
        }

        /**
         * @param i int to be parsed
         * @return the corresponding bond type, if known, or UNDEFINED.
         */
        public static BondType parseInt(int i)
        {
            switch (i)
            {
                case 0:
                    return NONE;
                case 1:
                    return SINGLE;
                case 2:
                    return DOUBLE;
                case 3:
                    return TRIPLE;
                case 4:
                    return QUADRUPLE;
                case 8:
                    return ANY;
                default:
                    return UNDEFINED;
            }
        }

        /**
         * @param string to be parsed
         * @return the corresponding bond type, if known, or UNDEFINED.
         */
        public static BondType parseStr(String string)
        {
            switch (string.trim())
            {
                case "1":
                    return SINGLE;
                case "2":
                    return DOUBLE;
                case "3":
                    return TRIPLE;
                case "4":
                    return QUADRUPLE;
                case "8":
                    return ANY;
                default:
                    return UNDEFINED;
            }
        }

        /**
         * @return the number of valences occupied by the bond analogue
         */
        public int getValence()
        {
            return valenceUsed;
        }
    }
    
//------------------------------------------------------------------------------

    public static class DENOPTIMEdgeSerializer 
    implements JsonSerializer<DENOPTIMEdge>
    {
        @Override
        public JsonElement serialize(DENOPTIMEdge edge, Type typeOfSrc,
              JsonSerializationContext context)
        {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("srcAPID", edge.getSrcAP().getID());
            jsonObject.addProperty("trgAPID", edge.getTrgAP().getID());
            jsonObject.add("bondType", context.serialize(edge.getBondType()));
            return jsonObject;
        }
    }

//------------------------------------------------------------------------------

}
