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

import denoptim.graph.Edge.BondType;

/**
 * A query for edges: a list of properties that target edges should possess in 
 * order to match this query.
 */
public class EdgeQuery
{

    /**
     * The vertex id of the source fragment
     */
    private Long srcVertexId = null; 
    
    /**
     * the vertex id of the destination fragment
     */
    private Long trgVertexId = null; 
    
    /**
     * The index of the attachment point in the list of DAPs associated
     * with the source fragment
     */
    private Integer srcAPID = null;
    
    /**
     * The index of the attachment point in the list of DAPs associated
     * with the target fragment
     */
    private Integer trgAPID = null;

    /**
     * The bond type associated with the connection between the fragments
     */
    private BondType bondType = null;
    
    /**
     * The class associated with the source AP
     */
    private APClass srcAPC = null;
    
    /**
     * The class associated with the target AP
     */
    private APClass trgAPC = null;
    
//------------------------------------------------------------------------------
    
    /**
     * Constructor for an edge from all parameters
     * @param srcVertexId vertex ID of the source vertex
     * @param trgVertexId vertex ID of the target vertex
     * @param srcAPID index of the {@link AttachmentPoint} on the source
     *  vertex
     * @param trgAPID index of the {@link AttachmentPoint} on the target
     *  vertex
     * @param btype the bond type
     * @param srcAPC the {@link APClass} on the source 
     * {@link AttachmentPoint} .
     * @param trgAPC the {@link APClass} on the target 
     * {@link AttachmentPoint} .
     */
    public EdgeQuery(Long srcVertexId, Long trgVertexId, 
            Integer srcAPID, Integer trgAPID, 
            BondType bt, APClass srcAPC, APClass trgAPC)
    {
        this.srcVertexId = srcVertexId;
        this.trgVertexId = trgVertexId;
        this.srcAPID = srcAPID;
        this.trgAPID = trgAPID;
        this.bondType = bt;
        this.srcAPC = srcAPC;
        this.trgAPC = trgAPC;
    }
    
//------------------------------------------------------------------------------

    public Long getSourceVertexId()
    {
        return srcVertexId;
    }
    
//------------------------------------------------------------------------------

    public Integer getSourceAPIdx()
    {
        return srcAPID;
    }
    
//------------------------------------------------------------------------------

    public Integer getTargetAPIdx()
    {
        return trgAPID;
    }        

//------------------------------------------------------------------------------

    public Long getTargetVertexId()
    {
        return trgVertexId;
    }
    
//------------------------------------------------------------------------------
    
    public APClass getSourceAPClass()
    {
        return srcAPC;
    }
    
//------------------------------------------------------------------------------
    
    public APClass getTargetAPClass()
    {
        return trgAPC;
    }       
    
//------------------------------------------------------------------------------

    public BondType getBondType()
    {
        return bondType;
    }

//------------------------------------------------------------------------------    
}
