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

/**
 * This class represents the edge between 2 fragments (vertices)
 * @author Vishwesh Venkatraman
 */
public class DENOPTIMEdge implements Serializable,Cloneable
{
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
    private int srcDAP;
    
    /**
     * the index of the attachment point in the list of DAPs associated
     * with the target fragment
     */
    private int trgDAP;

    /**
     * The bond type associated with the connection between the fragments
     */
    private int bondType;
    
    /**
     * The class associated with the source AP
     */
    private String srcRcn;
    
    /**
     * The class associated with the target AP
     */
    private String trgRcn;

//------------------------------------------------------------------------------
    
    /**
     * Constructor for an edge from all parameters
     * @param m_src vertex ID of the source vertex
     * @param m_trg vertex ID of the target vertex
     * @param m_srcDAP index of the AP on the source vertex
     * @param m_trgDAP index of the AP on the target vertex
     * @param m_btype the bond type
     */
    public DENOPTIMEdge(int m_src, int m_trg, int m_srcDAP, int m_trgDAP, 
                                                                    int m_btype)
    {
        srcVertex = m_src;
        trgVertex = m_trg;
        srcDAP = m_srcDAP;
        trgDAP = m_trgDAP;
        bondType = m_btype;  
        trgRcn = "";
        srcRcn = "";
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Constructor for an edge from all parameters
     * @param m_src vertex ID of the source vertex
     * @param m_trg vertex ID of the target vertex
     * @param m_srcDAP index of the AP on the source vertex
     * @param m_trgDAP index of the AP on the target vertex
     * @param m_btype the bond type
     * @param m_srcAPClass the AP class on the source attachment point
     * @param m_trgAPClass the AP class on the target attachment point
     */
    public DENOPTIMEdge(int m_src, int m_trg, int m_srcDAP, int m_trgDAP,
            int m_btype, String m_srcAPClass, String m_trgAPClass)
    {
        srcVertex = m_src;
        trgVertex = m_trg;
        srcDAP = m_srcDAP;
        trgDAP = m_trgDAP;
        bondType = m_btype;  
        srcRcn = m_srcAPClass;
        trgRcn = m_trgAPClass;
    }
    
//------------------------------------------------------------------------------

    public void setSourceVertex(int m_src)
    {
        srcVertex = m_src;
    }
    
//------------------------------------------------------------------------------

    public void setSourceDAP(int m_srcDAP)
    {
        srcDAP = m_srcDAP;
    }
    
//------------------------------------------------------------------------------

    public void setTargetDAP(int m_trgDAP)
    {
        trgDAP = m_trgDAP;
    }        

//------------------------------------------------------------------------------

    public void setTargetVertex(int m_trg)
    {
        trgVertex = m_trg;
    }

//------------------------------------------------------------------------------

    public void setBondType(int m_btype)
    {
        bondType = m_btype; 
    }
    
//------------------------------------------------------------------------------

    public int getSourceVertex()
    {
        return srcVertex;
    }
    
//------------------------------------------------------------------------------

    public int getSourceDAP()
    {
        return srcDAP;
    }
    
//------------------------------------------------------------------------------

    public int getTargetDAP()
    {
        return trgDAP;
    }        

//------------------------------------------------------------------------------

    public int getTargetVertex()
    {
        return trgVertex;
    }
    
//------------------------------------------------------------------------------
    
    public String getSourceReaction()
    {
        return srcRcn;
    }
    
//------------------------------------------------------------------------------
    
    public String getTargetReaction()
    {
        return trgRcn;
    }    
    
//------------------------------------------------------------------------------
    
    public void setSourceReaction(String m_rcn)
    {
        srcRcn = m_rcn;
    }
    
//------------------------------------------------------------------------------
    
    public void setTargetReaction(String m_rcn)
    {
        trgRcn = m_rcn;
    }    
    

//------------------------------------------------------------------------------

    public int getBondType()
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
        DENOPTIMEdge c = new DENOPTIMEdge(srcVertex, trgVertex, srcDAP, trgDAP, 
                bondType, srcRcn, trgRcn);
        return c;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Compares this and another edge ignoring edge and vertex IDs
     * @param other
     * @param reason string builder used to build the message clarifying the 
     * reason for returning <code>false</code>.
     * @return <code>true</code> if the two edges represent the same connection
     * even if the vertex IDs are different.
     */
    public boolean sameAs(DENOPTIMEdge other, StringBuilder reason)
    {
    	if (this.getSourceDAP() != other.getSourceDAP())
    	{
    		reason.append("Different source atom ("+this.getSourceDAP()+":"
    						+other.getSourceDAP()+"); ");
    		return false;
    	}
    	if (this.getTargetDAP() != other.getTargetDAP())
    	{
    		reason.append("Different target atom ("+this.getTargetDAP()+":"
					+other.getTargetDAP()+"); ");
    		return false;
    	}
    	if (!this.getSourceReaction().equals(other.getSourceReaction()))
    	{
    		reason.append("Different source APClass ("
    				+this.getSourceReaction()+":"
					+other.getSourceReaction()+"); ");
    		return false;
    	}
    	if (!this.getTargetReaction().equals(other.getTargetReaction()))
    	{
    		reason.append("Different target APClass ("
    				+this.getTargetReaction()+":"
					+other.getTargetReaction()+"); ");
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
        sb.append(srcVertex).append("_").append(srcDAP).append("_").
                append(trgVertex).append("_").append(trgDAP).append("_").
                append(bondType);
        if (srcRcn.length() > 0 && trgRcn.length() > 0)
            sb.append("_").append(srcRcn).append("_").append(trgRcn);

        return sb.toString();
    }

//------------------------------------------------------------------------------    
}
