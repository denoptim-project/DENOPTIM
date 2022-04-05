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

import denoptim.graph.DENOPTIMEdge.BondType;

/**
 * This class represents an edge that is undirected and ignores attachment
 * points. This class represents a connection between {@link Node}s that
 * may or may not be {@link DENOPTIMVertex}s.
 */

public class NodeConnection 
{
    /**
     * The bond type associated with the connection between the fragments
     */
    private BondType bondType = BondType.UNDEFINED;

//------------------------------------------------------------------------------
    
    /**
     * Constructor for an undirected edge. This edge does not make the APs
     * unavailable.
     * @param apA one of the attachment points connected by this edge
     * @param apB another of the attachment points connected by this edge
     * @param bondType defines what kind of bond type this edge should be 
     * converted to when converting a graph into a chemical representation.
     */
    
    public NodeConnection(BondType bondType) {
        this.bondType = bondType;
    }
    
//------------------------------------------------------------------------------
 
    public int compare(NodeConnection other)
    {
        return this.bondType.compareTo(other.bondType);
    }
    
//------------------------------------------------------------------------------

}
