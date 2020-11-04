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

import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IBond.Order;

/**
 * This class represents the edge between 2 fragments (vertices)
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class DENOPTIMEdge implements Serializable,Cloneable
{
    /**
     * Attachment point at source end
     */
    private DENOPTIMAttachmentPoint srcAp;

    /**
     * Attachment point at target end
     */
    private DENOPTIMAttachmentPoint trgAp;

    /**
     * The vertex id of the source fragment
     */
    private int srcVertex;
    
    /**
     * the vertex id of the destination fragment
     */
    private int trgVertex; 
    
    /**
     * the index of the attachment point in the list of DAPs associated
     * with the source fragment
     */
    private final int srcApIndex;
    
    /**
     * the index of the attachment point in the list of DAPs associated
     * with the target fragment
     */
    private final int trgApIndex;

    /**
     * The bond type associated with the connection between the fragments
     */
    private BondType bondType = BondType.UNDEFINED;
    
    /**
     * The class associated with the source AP
     */
    private String srcApClass;
    
    /**
     * The class associated with the target AP
     */
    private String trgApClass;

//------------------------------------------------------------------------------

    /**
     * Constructor for an edge
     * @param srcAp attachment point at source end
     * @param trgAp attachment point at target end
     */
    public DENOPTIMEdge(DENOPTIMAttachmentPoint srcAp,
                        DENOPTIMAttachmentPoint trgAp, int srcVertex,
                        int trgVertex, int srcApIndex, int trgApIndex) {
        this(srcAp, trgAp, srcVertex, trgVertex, srcApIndex, trgApIndex,
                BondType.SINGLE);
    }

//------------------------------------------------------------------------------

    public DENOPTIMEdge(DENOPTIMAttachmentPoint srcAp,
                        DENOPTIMAttachmentPoint trgAp, int srcVertex,
                        int trgVertex, int srcApIndex, int trgApIndex,
                        BondType bondType)
    {
        this(srcVertex, trgVertex, srcApIndex, trgApIndex, bondType, "", "");
        this.srcAp = srcAp;
        this.trgAp = trgAp;
    }

//------------------------------------------------------------------------------
    
    /**
     * Constructor for an edge from all parameters
     * @param srcVertex vertex ID of the source vertex
     * @param trgVertex vertex ID of the target vertex
     * @param srcApIndex index of the AP on the source vertex
     * @param trgApIndex index of the AP on the target vertex
     * @param bondType the bond type
     */
    public DENOPTIMEdge(int srcVertex, int trgVertex, int srcApIndex, int trgApIndex,
            BondType bondType)
    {
        this(srcVertex, trgVertex, srcApIndex, trgApIndex, bondType, "", "");
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Constructor for an edge from all parameters
     * @param srcVertex vertex ID of the source vertex
     * @param trgVertex vertex ID of the target vertex
     * @param srcApIndex index of the AP on the source vertex
     * @param trgApIndex index of the AP on the target vertex
     * @param bondType the bond type
     * @param m_srcAPClass the AP class on the source attachment point
     * @param m_trgAPClass the AP class on the target attachment point
     */
    public DENOPTIMEdge(int srcVertex, int trgVertex, int srcApIndex, int trgApIndex,
            BondType bondType, String m_srcAPClass, String m_trgAPClass)
    {
        this.srcVertex = srcVertex;
        this.trgVertex = trgVertex;
        this.srcApIndex = srcApIndex;
        this.trgApIndex = trgApIndex;
        this.bondType = bondType;
        this.srcApClass = m_srcAPClass;
        this.trgApClass = m_trgAPClass;
    }
    
//------------------------------------------------------------------------------

    public void setSrcVertex(int srcVertex)
    {
        this.srcVertex = srcVertex;
    }
    
//------------------------------------------------------------------------------

    public void setTrgVertex(int trgVertex)
    {
        this.trgVertex = trgVertex;
    }

//------------------------------------------------------------------------------

    public int getSrcVertex()
    {
        return srcVertex;
    }
    
//------------------------------------------------------------------------------

    public int getSrcApIndex()
    {
        return srcApIndex;
    }
    
//------------------------------------------------------------------------------

    public int getTrgApIndex()
    {
        return trgApIndex;
    }        

//------------------------------------------------------------------------------

    public int getTrgVertex()
    {
        return trgVertex;
    }
    
//------------------------------------------------------------------------------
    
    public String getSrcApClass()
    {
        return srcApClass;
    }
    
//------------------------------------------------------------------------------
    
    public String getTrgApClass()
    {
        return trgApClass;
    }    
    
//------------------------------------------------------------------------------
    
    public void setSrcApClass(String m_rcn)
    {
        srcApClass = m_rcn;
    }
    
//------------------------------------------------------------------------------
    
    public void setTrgApClass(String m_rcn)
    {
        trgApClass = m_rcn;
    }    
    

//------------------------------------------------------------------------------

    public BondType getBondType()
    {
        return bondType;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Return a deep-copy
     * @return a deep copy
     */
    public DENOPTIMEdge clone()
    {
        return new DENOPTIMEdge(srcVertex, trgVertex, srcApIndex, trgApIndex,
                bondType, srcApClass, trgApClass);
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
    public boolean sameAs(DENOPTIMEdge other, StringBuilder reason)
    {
    	if (this.getSrcApIndex() != other.getSrcApIndex())
    	{
    		reason.append("Different source atom ("+this.getSrcApIndex()+":"
    						+other.getSrcApIndex()+"); ");
    		return false;
    	}
    	if (this.getTrgApIndex() != other.getTrgApIndex())
    	{
    		reason.append("Different target atom ("+this.getTrgApIndex()+":"
					+other.getTrgApIndex()+"); ");
    		return false;
    	}
    	if (!this.getSrcApClass().equals(other.getSrcApClass()))
    	{
    		reason.append("Different source APClass ("
    				+this.getSrcApClass()+":"
					+other.getSrcApClass()+"); ");
    		return false;
    	}
    	if (!this.getTrgApClass().equals(other.getTrgApClass()))
    	{
    		reason.append("Different target APClass ("
    				+this.getTrgApClass()+":"
					+other.getTrgApClass()+"); ");
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

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(64);
        sb.append(srcVertex).append("_").append(srcApIndex).append("_").
                append(trgVertex).append("_").append(trgApIndex).append("_").
                append(bondType.toOldString());
        if (srcApClass.length() > 0 && trgApClass.length() > 0)
            sb.append("_").append(srcApClass).append("_").append(trgApClass);

        return sb.toString();
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

        private int valenceUsed = -1;

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

            //TODO del
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
                case -1:
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
}
