/*
 *   DENOPTIM
 *   Copyright (C) 2026 Marco Foscato <marco.foscato@uib.no>
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 */

package denoptim.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import denoptim.exception.DENOPTIMException;
import denoptim.graph.Vertex.BBType;
import denoptim.graph.Vertex.VertexType;

/**
 * Unit tests for {@link VertexQuery#matches(Vertex)}.
 */
public class VertexQueryTest
{
    private DGraph graph;

    @BeforeEach
    public void setUp() throws DENOPTIMException
    {
        graph = DGraphTest.makeTestGraphA2();
    }

//------------------------------------------------------------------------------

    @Test
    public void testMatchesAllNullCriteria()
    {
        VertexQuery query = new VertexQuery(null, null, null, null, null,
                null, null);
        for (Vertex v : graph.getVertexList())
        {
            assertTrue(query.matches(v));
        }
    }

//------------------------------------------------------------------------------

    @Test
    public void testMatchesVertexId()
    {
        Vertex v2 = graph.getVertexAtPosition(1);
        Vertex v7 = graph.getVertexAtPosition(5); //NB: vertexes are intentionally disordered!
        VertexQuery query = new VertexQuery(v7.getVertexId(), null, null, null,
                null, null, null);
        assertTrue(query.matches(v7));
        assertFalse(query.matches(v2));
    }

//------------------------------------------------------------------------------

    @Test
    public void testMatchesVertexType()
    {
        Vertex v1 = graph.getVertexAtPosition(0);
        Vertex v2 = graph.getVertexAtPosition(1);
        Vertex v7 = graph.getVertexAtPosition(5); //NB: vertexes are intentionally disordered!
        VertexQuery query = new VertexQuery(null, VertexType.EmptyVertex, null,
                null, null, null, null);
        assertTrue(query.matches(v7));
        assertFalse(query.matches(v1));
        assertFalse(query.matches(v2));
    }

//------------------------------------------------------------------------------

    @Test
    public void testMatchesBuildingBlockTypeAndId()
    {
        Vertex v1 = graph.getVertexAtPosition(0);
        Vertex v2 = graph.getVertexAtPosition(1);
        Vertex v7 = graph.getVertexAtPosition(5); //NB: vertexes are intentionally disordered!
        VertexQuery byType = new VertexQuery(null, null, BBType.SCAFFOLD, null, null,
                null, null);
        assertTrue(byType.matches(v1));
        assertFalse(byType.matches(v2));

        VertexQuery byId = new VertexQuery(null, null, null, 1, null, null,
                null);
        assertTrue(byId.matches(v2));
        assertFalse(byId.matches(v1));
    }

//------------------------------------------------------------------------------

    @Test
    public void testMatchesLevel()
    {
        Vertex v1 = graph.getVertexAtPosition(0);
        Vertex v2 = graph.getVertexAtPosition(1);
        Vertex v7 = graph.getVertexAtPosition(5); //NB: vertexes are intentionally disordered!
        VertexQuery query = new VertexQuery(null, null, null, null, -1,
                null, null);
        assertTrue(query.matches(v1));
        assertFalse(query.matches(v2));
        assertFalse(query.matches(v7));
        query = new VertexQuery(null, null, null, null, 1,
            null, null);
        assertTrue(query.matches(v7));
        assertFalse(query.matches(v1));
        assertFalse(query.matches(v2));
    }

//------------------------------------------------------------------------------

    @Test
    public void testMatchesIncomingEdgeQuery() throws DENOPTIMException
    {
        Vertex v1 = graph.getVertexAtPosition(0);
        Vertex v2 = graph.getVertexAtPosition(1);
        Vertex v4 = graph.getVertexAtPosition(3);
        EdgeQuery incoming = EdgeQuery.make(null, null, null, null,
                null, APClass.make("B",1), null);
        VertexQuery query = new VertexQuery(null, null, null, null, null,
                incoming, null);

        assertTrue(query.matches(v4));
        assertFalse(query.matches(v1));
        assertFalse(query.matches(v2));
    }

//------------------------------------------------------------------------------

    @Test
    public void testMatchesOutgoingEdgeQuery() throws DENOPTIMException
    {
        Vertex v1 = graph.getVertexAtPosition(0);
        Vertex v2 = graph.getVertexAtPosition(1);
        Vertex v4 = graph.getVertexAtPosition(3);
        EdgeQuery outgoing = EdgeQuery.make(null, null, null, null,
                null, APClass.make("B",1), null);
        VertexQuery query = new VertexQuery(null, null, null, null, null, null,
                outgoing);

        assertTrue(query.matches(v1));
        assertTrue(query.matches(v2));
        assertFalse(query.matches(v4));
    }

//------------------------------------------------------------------------------

    @Test
    public void testMatchesIncomingAndOutgoingEdgeQueries() throws DENOPTIMException
    {
        Vertex v1 = graph.getVertexAtPosition(0);
        Vertex v2 = graph.getVertexAtPosition(1);
        Vertex v4 = graph.getVertexAtPosition(3);
        EdgeQuery incoming = EdgeQuery.make(null, null, null, null, null,
             null, APClass.make("B",1));
        EdgeQuery outgoing = EdgeQuery.make(null, null, 1, null, null,
             APClass.make("B",1), null);
        VertexQuery query = new VertexQuery(null, null, null, null, null,
                incoming, outgoing);

        assertTrue(query.matches(v2));
        assertFalse(query.matches(v1));
        assertFalse(query.matches(v4));
    }

//------------------------------------------------------------------------------

    @Test
    public void testEmptyEdgeQueryFiltersVertices()
    {
        EdgeQuery emptyEdgeQuery = EdgeQuery.make(null, null, null, null, null,
                null, null);
        VertexQuery query = new VertexQuery(null, null, null, null, null,
                emptyEdgeQuery, emptyEdgeQuery);

        List<Vertex> matched = new ArrayList<>();
        for (Vertex v : graph.getVertexList())
        {
            if (query.matches(v))
            {
                matched.add(v);
            }
        }
        // Should match all non-root and non-leaf vertices
        assertEquals(2, matched.size());
    }

//------------------------------------------------------------------------------

    @Test
    public void testFindVerticesUsesMatches()
    {
        VertexQuery query = new VertexQuery(null, null, BBType.UNDEFINED,
             null, 0, null, null);
        Logger logger = Logger.getLogger("test");

        List<Vertex> fromFind = graph.findVertices(query, false, logger);
        List<Vertex> fromMatches = new ArrayList<>();
        for (Vertex v : graph.getVertexList())
        {
            if (query.matches(v))
            {
                fromMatches.add(v);
            }
        }

        assertEquals(2, fromFind.size());
        assertEquals(2, fromMatches.size());

        assertTrue(fromMatches.containsAll(fromFind));
        assertTrue(fromFind.containsAll(fromMatches));
    }

//------------------------------------------------------------------------------

}
