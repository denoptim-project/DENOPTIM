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

package denoptim.graph;

import denoptim.graph.Vertex.BBType;
import denoptim.graph.Vertex.VertexType;

/**
 * Query for searching vertices.
 * @author Marco Foscato
 */

public class VertexQuery
{ 
    /**
     * Query on unique identifier or null.
     */
    private Integer vertexId = null;
    
    /**
     * Query on building block in the library of building blocks, or null.
     */
    private Integer buildingBlockId = null;
    
    /**
     * Query on building block type or null.
     */
    private BBType buildingBlockType = null;
    
    /**
     * Query on type of vertex
     */
    private VertexType vertexType = null;
   
    /**
     * Query about the level of the vertex
     */
    private Integer level = null;

    /**
     * Query on the vertex' incoming connections (i.e., vertex id the target)
     */
    private EdgeQuery incomingEdgeQuery;

    /**
     * Query on the vertex' out coming connections (i.e., vertex id the source)
     */
    private EdgeQuery outgoingEdgeQuery;

//------------------------------------------------------------------------------

    /**
     * Constructor from vertex and edge queries
     * @param vID the query on vertex's unique identifier, or null.
     * @param vType the query on vertex's type (i.e., {@link EmptyVertex}, 
     * {@link Fragment}, or {@link Template}), or null.
     * @param bbType the query on vertex's building block type, or null,
     * @param bbID  the query on vertex's building block ID in the library of 
     * hit type, or null.
     * @param level the level of the vertices to match, or null. Remember level
     * is an integer that starts from -1.
     * @param eIn the edge query (filters candidates based on the connection of
     * a candidate with the rest of the graph) for incoming connections where
     * the candidate vertex if the target.
     */

    public VertexQuery(Integer vID, VertexType vType, BBType bbType, 
            Integer bbID, Integer level, EdgeQuery eIn)
    {
        this.vertexId = vID;
        this.vertexType = vType;
        this.buildingBlockType = bbType;
        this.buildingBlockId = bbID;
        this.level = level;
        this.incomingEdgeQuery = eIn;
        this.outgoingEdgeQuery = null;
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor from vertex and edge queries
     * @param eIn the edge query (filters candidates based on the connection of
     * a candidate with the rest of the graph) for incoming connections where
     * the candidate vertex if the target.
     * @param vID the query on vertex's unique identifier, or null.
     * @param vType the query on vertex's type (i.e., {@link EmptyVertex}, 
     * {@link Fragment}, or {@link Template}), or null.
     * @param bbType the query on vertex's building block type, or null,
     * @param bbID  the query on vertex's building block ID in the library of 
     * hit type, or null.
     * @param eOut the edge query (filters candidates based on the connection of
     * a candidate with the rest of the graph) for incoming connections where
     * the candidate vertex if the source.
     */

    public VertexQuery(Integer vID, VertexType vType, BBType bbType, 
            Integer bbID, Integer level, EdgeQuery eIn, EdgeQuery eOut)
    {
        this(vID, vType, bbType, bbID, level, eIn);
        this.outgoingEdgeQuery = eOut;
    }
	
//------------------------------------------------------------------------------

    public VertexType getVertexTypeQuery()
    {
    	return vertexType;
    }
    
//------------------------------------------------------------------------------

    public Integer getVertexIDQuery()
    {
        return vertexId;
    }

//------------------------------------------------------------------------------

    public BBType getVertexBBTypeQuery()
    {
        return buildingBlockType;
    }

//------------------------------------------------------------------------------

    public Integer getVertexBBIDQuery()
    {
        return buildingBlockId;
    }

//------------------------------------------------------------------------------
    
    public Integer getVertexLevelQuery()
    {
        return level;
    }
    
//------------------------------------------------------------------------------

    public EdgeQuery getInEdgeQuery()
    {
        return incomingEdgeQuery;
    }

//------------------------------------------------------------------------------

    public EdgeQuery getOutEdgeQuery()
    {
        return outgoingEdgeQuery;
    }

//------------------------------------------------------------------------------

}
