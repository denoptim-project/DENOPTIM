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

import java.util.List;

import denoptim.graph.Vertex.BBType;
import denoptim.graph.Vertex.VertexType;

/**
 * Query for searching vertices.
 */

public class VertexQuery
{ 
    /**
     * Query on unique identifier or null.
     */
    private Long vertexId = null;
    
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
     * List of attachment point queries on the vertex
     */
    private List<AttachmentPointQuery> apQueries = null;

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
     * Constructor from empty queries.
     */
    public VertexQuery()
    {
    }

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
     * @param apQueries the list of attachment point queries on the vertex, or null.
     * @param eIn the edge query (filters candidates based on the connection of
     * a candidate with the rest of the graph) for incoming connections where
     * the candidate vertex if the target.
     * @param eOut the edge query (filters candidates based on the connection of
     * a candidate with the rest of the graph) for outgoing connections where
     * the candidate vertex if the source.
     */

    public VertexQuery(Long vID, VertexType vType, BBType bbType, 
            Integer bbID, Integer level, 
            List<AttachmentPointQuery> apQueries,
            EdgeQuery eIn, EdgeQuery eOut)
    {
        this.vertexId = vID;
        this.vertexType = vType;
        this.buildingBlockType = bbType;
        this.buildingBlockId = bbID;
        this.level = level;
        this.apQueries = apQueries;
        this.incomingEdgeQuery = eIn;
        this.outgoingEdgeQuery = eOut;
    }

//------------------------------------------------------------------------------

    /**
     * Tests whether the given vertex satisfies all non-null criteria in this
     * query.
     * @param v the vertex to test
     * @return {@code true} if the vertex matches, {@code false} otherwise
     */
    public boolean matches(Vertex v)
    {
        if (vertexId != null && v.getVertexId() != vertexId)
        {
            return false;
        }

        if (vertexType != null && v.getVertexType() != vertexType)
        {
            return false;
        }

        if (buildingBlockType != null && v.getBuildingBlockType() != buildingBlockType)
        {
            return false;
        }

        if (buildingBlockId != null && v.getBuildingBlockId() != buildingBlockId)
        {
            return false;
        }

        if (level != null)
        {
            if (v.getGraphOwner() == null)
            {
                return false;
            }
            int vertexLevel = v.getGraphOwner().getLevel(v);
            if (vertexLevel != level)
            {
                return false;
            }
        }
        if (apQueries != null)
        {
            for (AttachmentPointQuery apQuery : apQueries)
            {
                boolean matches = false;
                for (AttachmentPoint ap : v.getAttachmentPoints())
                {
                    if (apQuery.matches(ap))
                    {
                        matches = true;
                        break;
                    }
                }
                if (!matches)
                {
                    return false;
                }
            }
        }

        if (incomingEdgeQuery != null
                && !matchesAnyEdge(v, incomingEdgeQuery, true))
        {
            return false;
        }

        if (outgoingEdgeQuery != null
                && !matchesAnyEdge(v, outgoingEdgeQuery, false))
        {
            return false;
        }

        return true;
    }

//------------------------------------------------------------------------------

    private static boolean matchesAnyEdge(Vertex v, EdgeQuery edgeQuery,
            boolean incoming)
    {
        DGraph graph = v.getGraphOwner();
        if (graph == null)
        {
            return false;
        }
        if (incoming)
        {
            for (Edge e : graph.getEdgesWithTrg(v))
            {
                if (edgeQuery.matches(e))
                {
                    return true;
                }
            }
        } else {
            for (Edge e : graph.getEdgesWithSrc(v))
            {
                if (edgeQuery.matches(e))
                {
                    return true;
                }
            }
        }
        return false;
    }

//------------------------------------------------------------------------------

}
