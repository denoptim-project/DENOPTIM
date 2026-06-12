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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import denoptim.exception.DENOPTIMException;
import denoptim.graph.Edge.BondType;
import denoptim.utils.AttachmentPointQuery;

/**
 * Unit tests for {@link EdgeQuery#matches(Edge)}.
 */
public class EdgeQueryTest
{
    private DGraph graph;

    @BeforeEach
    public void setUp() throws DENOPTIMException
    {
        graph = DGraphTest.makeTestGraphA2();
    }

//------------------------------------------------------------------------------

    @Test
    public void testMatchesNullEdge()
    {
        EdgeQuery query = EdgeQuery.make(null, null, null, null, null, null, null);
        assertFalse(query.matches(null));
    }

//------------------------------------------------------------------------------

    @Test
    public void testMatchesAllNullCriteria()
    {
        EdgeQuery query = EdgeQuery.make(null, null, null, null, null, null, null);
        Edge edgeV0V1 = graph.getVertexAtPosition(1).getEdgeToParent();
        Edge edgeEV4EV5 = graph.getVertexAtPosition(5).getEdgeToParent();
        assertTrue(query.matches(edgeV0V1));
        assertTrue(query.matches(edgeEV4EV5));
    }

//------------------------------------------------------------------------------

    @Test
    public void testMatchesSourceAndTargetVertices()
    {
        Vertex v1 = graph.getVertexAtPosition(0);
        Vertex v2 = graph.getVertexAtPosition(1);
        Vertex v7 = graph.getVertexAtPosition(5); //NB: vertexes are intentionally disordered!
        Vertex v5 = graph.getVertexAtPosition(6); //NB: vertexes are intentionally disordered!
        Edge edgeV0V1 = v2.getEdgeToParent();
        Edge edgeEV5EV7 = v7.getEdgeToParent();

        EdgeQuery match = EdgeQuery.make(v1.getVertexId(), v2.getVertexId(),
                null, null, null, null, null);
        assertTrue(match.matches(edgeV0V1));

        EdgeQuery matchSrcOnly = EdgeQuery.make(v5.getVertexId(), null,
                null, null, null, null, null);
        assertTrue(matchSrcOnly.matches(edgeEV5EV7));

        EdgeQuery matchTrgOnly = EdgeQuery.make(null, v7.getVertexId(),
                null, null, null, null, null);
        assertTrue(matchTrgOnly.matches(edgeEV5EV7));

        EdgeQuery wrongTarget = EdgeQuery.make(v1.getVertexId(), v7.getVertexId(),
                null, null, null, null, null);
        assertFalse(wrongTarget.matches(edgeEV5EV7));
    }

//------------------------------------------------------------------------------

    @Test
    public void testMatchesAttachmentPointIndices()
    {
        Vertex v2 = graph.getVertexAtPosition(1);
        Vertex v3 = graph.getVertexAtPosition(2);
        Vertex v5 = graph.getVertexAtPosition(6); //NB: vertexes are intentionally disordered!
        Edge edgeV1V2 = v2.getEdgeToParent(); // ap#0 to ap#0
        Edge edgeV1V3 = v3.getEdgeToParent(); // ap#1 to ap#0
        Edge edgeV1V5 = v5.getEdgeToParent(); // ap#2 to ap#1
        EdgeQuery match = EdgeQuery.make(null, null, 0, null, null, null, null);
        assertTrue(match.matches(edgeV1V2));
        assertFalse(match.matches(edgeV1V5));
        match = EdgeQuery.make(null, null, null, 0, null, null, null);
        assertTrue(match.matches(edgeV1V2));
        assertFalse(match.matches(edgeV1V5));
        match = EdgeQuery.make(null, null, 1, null, null, null, null);
        assertTrue(match.matches(edgeV1V3));
        assertFalse(match.matches(edgeV1V5));
        match = EdgeQuery.make(null, null, null, 1, null, null, null);
        assertTrue(match.matches(edgeV1V5));
        assertFalse(match.matches(edgeV1V3));
        match = EdgeQuery.make(null, null, 2, 1, null, null, null);
        assertTrue(match.matches(edgeV1V5));
        assertFalse(match.matches(edgeV1V3));
    }

//------------------------------------------------------------------------------

    @Test
    public void testMatchesBondType()
    {
        Vertex v2 = graph.getVertexAtPosition(1);
        Vertex v3 = graph.getVertexAtPosition(2);
        Edge edgeV1V2 = v2.getEdgeToParent(); //  triple
        Edge edgeV1V3 = v3.getEdgeToParent(); // single
        EdgeQuery triple = EdgeQuery.make(null, null, null, null,
                BondType.TRIPLE, null, null);
        EdgeQuery single = EdgeQuery.make(null, null, null, null,
                BondType.SINGLE, null, null);

        assertTrue(triple.matches(edgeV1V2));
        assertFalse(triple.matches(edgeV1V3));
        assertTrue(single.matches(edgeV1V3));
        assertFalse(single.matches(edgeV1V2));
    }

//------------------------------------------------------------------------------

    @Test
    public void testMatchesAPClasses() throws DENOPTIMException
    {
        Vertex v2 = graph.getVertexAtPosition(1);
        Vertex v3 = graph.getVertexAtPosition(2);
        Edge edgeV1V2 = v2.getEdgeToParent(); // A:0  -> B:1
        Edge edgeV1V3 = v3.getEdgeToParent(); // B:1 -> C:0
        EdgeQuery srcClass = EdgeQuery.make(null, null, null, null, null,
                APClass.make("A",0), null);
        EdgeQuery trgClass = EdgeQuery.make(null, null, null, null, null, null,
                APClass.make("B",1));

        assertTrue(srcClass.matches(edgeV1V2));
        assertTrue(trgClass.matches(edgeV1V2));
        assertFalse(trgClass.matches(edgeV1V3));
    }

//------------------------------------------------------------------------------

    @Test
    public void testMatchesCombinedCriteria() throws DENOPTIMException
    {
        Vertex v1 = graph.getVertexAtPosition(0);
        Vertex v2 = graph.getVertexAtPosition(1);
        Vertex v3 = graph.getVertexAtPosition(2);
        Edge edgeV1V2 = v2.getEdgeToParent(); // ap#0 to ap#0, A:0  -> B:1, triple
        Edge edgeV1V3 = v3.getEdgeToParent(); // ap#1 to ap#0, B:1 -> C:0, single
        EdgeQuery query = EdgeQuery.make(v1.getVertexId(), v2.getVertexId(),
                0, 0, 
                BondType.TRIPLE, 
                APClass.make("A",0), 
                APClass.make("B",1));
        assertTrue(query.matches(edgeV1V2));
        assertFalse(query.matches(edgeV1V3));
        EdgeQuery almost = EdgeQuery.make(v1.getVertexId(), v2.getVertexId(),
                0, 0, 
                BondType.SINGLE, //This is the difference!
                APClass.make("A",0), 
                APClass.make("B",1));
        assertFalse(almost.matches(edgeV1V2));
        assertFalse(query.matches(edgeV1V3));
    }

//------------------------------------------------------------------------------

    @Test
    public void testFindEdgesUsesMatches()
    {
        EdgeQuery query = EdgeQuery.make(null, null,
                null, 0, null, null, null);

        int directMatches = 0;
        for (Edge e : graph.getEdgeList())
        {
            if (query.matches(e))
            {
                directMatches++;
            }
        }

        assertEquals(5, directMatches);
        assertEquals(directMatches,
                graph.findEdges(query,
                        java.util.logging.Logger.getLogger("test")).size());
    }

//------------------------------------------------------------------------------

    @Test
    public void testMatchesNestedQueries() throws DENOPTIMException
    {
        Vertex v1 = graph.getVertexAtPosition(0);
        Vertex v2 = graph.getVertexAtPosition(1);
        Edge edgeV1V2 = v2.getEdgeToParent();

        VertexQuery srcVertex = new VertexQuery(v1.getVertexId(), null, null,
                null, null, null);
        VertexQuery trgVertex = new VertexQuery(v2.getVertexId(), null, null,
                null, null, null);
        AttachmentPointQuery srcAp = new AttachmentPointQuery(null, 0,
                APClass.make("A", 0), null, null);
        AttachmentPointQuery trgAp = new AttachmentPointQuery(null, 0,
                APClass.make("B", 1), null, null);

        EdgeQuery query = new EdgeQuery(srcVertex, trgVertex, srcAp, trgAp,
                BondType.TRIPLE);
        assertTrue(query.matches(edgeV1V2));
        assertFalse(query.matches(graph.getVertexAtPosition(2).getEdgeToParent()));
    }

//------------------------------------------------------------------------------
}
