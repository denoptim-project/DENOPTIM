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
import denoptim.graph.Edge.BondType;
import denoptim.graph.Vertex.BBType;

/**
 * Unit tests for {@link AttachmentPointQuery#matches(AttachmentPoint)}.
 */
public class AttachmentPointQueryTest
{
    private DGraph graph;
    private Vertex v1;
    private Vertex v3;
    private AttachmentPoint v1Ap0;
    private AttachmentPoint v1Ap1;
    private AttachmentPoint v3Ap0;

    @BeforeEach
    public void setUp()
    {
        graph = DGraphTest.makeTestGraphA2();
        v1 = graph.getVertexAtPosition(0);
        v3 = graph.getVertexAtPosition(2);
        v1Ap0 = v1.getAP(0);
        v1Ap1 = v1.getAP(1);
        v3Ap0 = v3.getAP(0);
    }

//------------------------------------------------------------------------------

    @Test
    public void testMatchesAllNullCriteria()
    {
        AttachmentPointQuery query = new AttachmentPointQuery(null, null, null,
                null, null, null);
        for (AttachmentPoint ap : graph.getAttachmentPoints())
        {
            assertTrue(query.matches(ap));
        }
    }

//------------------------------------------------------------------------------

    @Test
    public void testMatchesAttachmentPointId()
    {
        AttachmentPointQuery query = new AttachmentPointQuery(
                (long) v1Ap0.getID(), null, null, null, null, null);
        assertTrue(query.matches(v1Ap0));
        assertFalse(query.matches(v1Ap1));
    }

//------------------------------------------------------------------------------

    @Test
    public void testMatchesAttachmentPointIndex()
    {
        AttachmentPointQuery query = new AttachmentPointQuery(null, 1, null,
                null, null, null);
        assertTrue(query.matches(v1Ap1));
        assertFalse(query.matches(v1Ap0));
    }

//------------------------------------------------------------------------------

    @Test
    public void testMatchesAPClass() throws DENOPTIMException
    {
        AttachmentPointQuery query = new AttachmentPointQuery(null, null,
                v3Ap0.getAPClass(), null, null, null);
        assertTrue(query.matches(v3Ap0));
        assertFalse(query.matches(v1Ap0));
    }

//------------------------------------------------------------------------------

    @Test
    public void testMatchesNestedVertexQuery()
    {
        VertexQuery vertexQuery = new VertexQuery(null, null, BBType.SCAFFOLD,
                null, null, null, null, null);
        AttachmentPointQuery query = new AttachmentPointQuery(null, null, null,
                vertexQuery, null, null);

        assertTrue(query.matches(v1Ap0));
        assertFalse(query.matches(v3Ap0));
    }

//------------------------------------------------------------------------------

    @Test
    public void testMatchesNestedEdgeQuery()
    {
        Edge userEdge = v1Ap0.getEdgeUser();
        EdgeQuery edgeQuery = new EdgeQuery(
                new VertexQuery(v1.getVertexId(), null, null, null, null, null, null, null),
                new VertexQuery(graph.getVertexAtPosition(1).getVertexId(), null, null, null, null, null, null, null),
                new AttachmentPointQuery(null, 0, null, null, null, null),
                new AttachmentPointQuery(null, 0, null, null, null, null),
                BondType.TRIPLE);
        AttachmentPointQuery query = new AttachmentPointQuery(null, null, null,
                null, edgeQuery, null);

        assertTrue(query.matches(v1Ap0));
        assertFalse(query.matches(v1Ap1));
        assertTrue(edgeQuery.matches(userEdge));
    }

//------------------------------------------------------------------------------

    @Test
    public void testEmptyEdgeQueryRequiresEdge()
    {
        AttachmentPoint usedAp = null;
        AttachmentPoint unusedAp = null;
        for (AttachmentPoint ap : graph.getAttachmentPoints())
        {
            if (!ap.isAvailable())
            {
                usedAp = ap;
            }
            if (ap.isAvailable())
            {
                unusedAp = ap;
            }
            if (usedAp != null && unusedAp != null)
            {
                break;
            }
        }
        EdgeQuery emptyEdgeQuery = new EdgeQuery(null, null, null, null, null);
        AttachmentPointQuery query = new AttachmentPointQuery(null, null, null,
                null, emptyEdgeQuery, null);

        assertFalse(query.matches(unusedAp));
        assertTrue(query.matches(usedAp));
    }

//------------------------------------------------------------------------------

    @Test
    public void testLinkedAPQuery()
    {
        AttachmentPoint usedAp = null;
        AttachmentPoint unusedAp = null;
        for (AttachmentPoint ap : graph.getAttachmentPoints())
        {
            if (!ap.isAvailable())
            {
                usedAp = ap;
            }
            if (ap.isAvailable())
            {
                unusedAp = ap;
            }
            if (usedAp != null && unusedAp != null)
            {
                break;
            }
        }
        AttachmentPoint linkedAp = usedAp.getLinkedAP();

        AttachmentPointQuery linkedAPQuery = new AttachmentPointQuery(
            Long.valueOf(linkedAp.getID()), null, 
            linkedAp.getAPClass(), 
            new VertexQuery(linkedAp.getOwner().getVertexId(), null, null, null, null, null, null, null), 
            null, null);
        AttachmentPointQuery query = new AttachmentPointQuery(null, null, null,
                null, null, linkedAPQuery);

        assertTrue(query.matches(usedAp));
        assertFalse(query.matches(unusedAp));
        assertFalse(query.matches(linkedAp));
    }

//------------------------------------------------------------------------------

    @Test
    public void testFindAPsUsesMatches() throws DENOPTIMException
    {
        VertexQuery vertexQuery = new VertexQuery(null, null, null,
                null, 0, null, null, null);
        AttachmentPointQuery query = new AttachmentPointQuery(null, null,
            APClass.make("B",1), 
            vertexQuery, null, null);
        Logger logger = Logger.getLogger("test");

        List<AttachmentPoint> fromFind = graph.findAPs(query, logger);
        List<AttachmentPoint> fromMatches = new ArrayList<>();
        for (AttachmentPoint ap : graph.getAttachmentPoints())
        {
            if (query.matches(ap))
            {
                fromMatches.add(ap);
            }
        }
        
        assertEquals(2, fromFind.size());
        assertEquals(2, fromMatches.size());

        assertTrue(fromMatches.containsAll(fromFind));
        assertTrue(fromFind.containsAll(fromMatches));
    }

//------------------------------------------------------------------------------

}
