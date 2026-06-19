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
        EdgeQuery query = new EdgeQuery(null, null, null, null, null);
        assertFalse(query.matches(null));
    }

//------------------------------------------------------------------------------

    @Test
    public void testMatchesAllNullCriteria()
    {
        EdgeQuery query = new EdgeQuery(null, null, null, null, null);
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

        EdgeQuery match = new EdgeQuery(
            new VertexQuery(v1.getVertexId(), null, null, null, null, null, null, null),
            new VertexQuery(v2.getVertexId(), null, null, null, null, null, null, null),
                null, null, null);
        assertTrue(match.matches(edgeV0V1));

        EdgeQuery matchSrcOnly = new EdgeQuery(new VertexQuery(v5.getVertexId(), null, null, null, null, null, null, null),
                null, null, null, null);
        assertTrue(matchSrcOnly.matches(edgeEV5EV7));

        EdgeQuery matchTrgOnly = new EdgeQuery(null, new VertexQuery(v7.getVertexId(), null, null, null, null, null, null, null),
                null, null, null);
        assertTrue(matchTrgOnly.matches(edgeEV5EV7));

        EdgeQuery wrongTarget = new EdgeQuery(new VertexQuery(v1.getVertexId(), null, null, null, null, null, null, null),
                new VertexQuery(v7.getVertexId(), null, null, null, null, null, null, null),
                null, null, null);
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

        AttachmentPointQuery ap0 = new AttachmentPointQuery(null, 0, null, null, null, null);
        AttachmentPointQuery ap1 = new AttachmentPointQuery(null, 1, null, null, null, null);
        AttachmentPointQuery ap2 = new AttachmentPointQuery(null, 2, null, null, null, null);
        EdgeQuery match = new EdgeQuery(null, null, ap0, null, null);
        assertTrue(match.matches(edgeV1V2));
        assertFalse(match.matches(edgeV1V5));
        match = new EdgeQuery(null, null, ap0, null, null);
        assertTrue(match.matches(edgeV1V2));
        assertFalse(match.matches(edgeV1V5));
        match = new EdgeQuery(null, null, ap1, null, null);
        assertTrue(match.matches(edgeV1V3));
        assertFalse(match.matches(edgeV1V5));
        match = new EdgeQuery(null, null, null, ap1, null);
        assertTrue(match.matches(edgeV1V5));
        assertFalse(match.matches(edgeV1V3));
        match = new EdgeQuery(null, null, ap2, ap1, null);
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
        EdgeQuery triple = new EdgeQuery(null, null, null, null,
                BondType.TRIPLE);
        EdgeQuery single = new EdgeQuery(null, null, null, null,
                BondType.SINGLE);

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
        EdgeQuery srcClass = new EdgeQuery(null, null, 
            new AttachmentPointQuery(null, null, APClass.make("A",0), null, null, null),
            null, null);
        EdgeQuery trgClass = new EdgeQuery(null, null, null, 
            new AttachmentPointQuery(null, null, APClass.make("B",1), null, null, null), null);

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
        EdgeQuery query = new EdgeQuery(
            new VertexQuery(v1.getVertexId(), null, null, null, null, null, null, null),
            new VertexQuery(v2.getVertexId(), null, null, null, null, null, null, null),
            new AttachmentPointQuery(null, 0, APClass.make("A",0), null, null, null),
            new AttachmentPointQuery(null, 0, APClass.make("B",1), null, null, null),
            BondType.TRIPLE);
        assertTrue(query.matches(edgeV1V2));
        assertFalse(query.matches(edgeV1V3));
        EdgeQuery almost = new EdgeQuery(
            new VertexQuery(v1.getVertexId(), null, null, null, null, null, null, null),
            new VertexQuery(v2.getVertexId(), null, null, null, null, null, null, null),
            new AttachmentPointQuery(null, 0, APClass.make("A",0), null, null, null),
            new AttachmentPointQuery(null, 0, APClass.make("B",1), null, null, null),
            BondType.SINGLE);
        assertFalse(almost.matches(edgeV1V2));
        assertFalse(query.matches(edgeV1V3));
    }

//------------------------------------------------------------------------------

    @Test
    public void testFindEdgesUsesMatches()
    {
        EdgeQuery query = new EdgeQuery(null, null, null, 
            new AttachmentPointQuery(null, 0, null, null, null, null),
            null);

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
                null, null, null, null, null);
        VertexQuery trgVertex = new VertexQuery(v2.getVertexId(), null, null,
                null, null, null, null, null);
        AttachmentPointQuery srcAp = new AttachmentPointQuery(null, 0,
                APClass.make("A", 0), null, null, null);
        AttachmentPointQuery trgAp = new AttachmentPointQuery(null, 0,
                APClass.make("B", 1), null, null, null);

        EdgeQuery query = new EdgeQuery(srcVertex, trgVertex, srcAp, trgAp,
                BondType.TRIPLE);
        assertTrue(query.matches(edgeV1V2));
        assertFalse(query.matches(graph.getVertexAtPosition(2).getEdgeToParent()));
    }

//------------------------------------------------------------------------------
}
