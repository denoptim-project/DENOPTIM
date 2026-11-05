/*
 *   DENOPTIM
 *   Copyright (C) 2026 Marco Foscato <marco.foscato@uib.no>
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import denoptim.graph.Provenance.Parent;
import denoptim.graph.Provenance.ParentLink;
import denoptim.graph.Provenance.Source;
import denoptim.graph.Provenance.Summary;
import denoptim.utils.MutationType;

/**
 * Tests for provenance serialization.
 */

public class ProvenanceTest
{
//------------------------------------------------------------------------------

    @Test
    public void testSimpleSources()
    {
        assertRoundTrip(Provenance.construction());
        assertRoundTrip(Provenance.construction("Reinvent"));
        assertRoundTrip(Provenance.manual());
        assertRoundTrip(Provenance.input());
        assertRoundTrip(Provenance.input("fragmented"));

        assertEquals("{\"source\":\"CONSTRUCTION\",\"attribute\":\"Reinvent\",\"parents\":[]}",
                Provenance.construction("Reinvent").toString());
        assertEquals("{\"source\":\"INPUT\",\"attribute\":\"fragmented\",\"parents\":[]}",
                Provenance.input("fragmented").toString());
        assertFalse(Provenance.construction("Reinvent").toString()
                .contains("\n"));
        assertNull(Provenance.parse(""));
        assertNull(Provenance.parse(null));
        assertNull(Provenance.parse("NEW"));
        assertNull(Provenance.parse("Xover: Gen:0 Cand:A|1|_2 X Gen:0 Cand:B|2|_3"));
    }

//------------------------------------------------------------------------------

    @Test
    public void testCrossoverKeepsOnlyItsParents()
    {
        Parent first = new Parent(0, "M00000001", 12, Arrays.asList(3L, 7L));
        Parent second = new Parent(1, "M00000002", 15, Arrays.asList(1L));
        Provenance provenance = Provenance.crossover(first, second);

        assertEquals(Source.CROSSOVER, provenance.getSource());
        assertEquals(2, provenance.getParents().size());
        assertEquals(0, provenance.getParents().get(0).getGenerationId());
        assertEquals("M00000001",
                provenance.getParents().get(0).getCandidateId());
        assertEquals(12, provenance.getParents().get(0).getGraphId());
        assertEquals(Arrays.asList(3L, 7L),
                provenance.getParents().get(0).getVertexIds());
        assertNull(provenance.getParents().get(0).getMutationType());
        assertRoundTrip(provenance);
    }

//------------------------------------------------------------------------------

    @Test
    public void testMutationRecordsSiteOnNearestParentOnly()
    {
        Parent parent = new Parent(2, "M00000003", 20);
        Provenance provenance = Provenance.mutation(parent)
                .withMutation(MutationType.CHANGEBRANCH, 8L, 3);

        assertEquals(Source.MUTATION, provenance.getSource());
        assertEquals(1, provenance.getParents().size());
        Parent recorded = provenance.getParents().get(0);
        assertEquals(2, recorded.getGenerationId());
        assertEquals("M00000003", recorded.getCandidateId());
        assertEquals(20, recorded.getGraphId());
        assertEquals(Arrays.asList(8L), recorded.getVertexIds());
        assertEquals(Integer.valueOf(3), recorded.getVertexPosition());
        assertEquals(MutationType.CHANGEBRANCH, recorded.getMutationType());
        assertRoundTrip(provenance);
    }

//------------------------------------------------------------------------------

    @Test
    public void testSummaryKeepsSourceParentsAndMutation()
    {
        assertEquals("CONSTRUCTION", Provenance.construction().summary());
        assertEquals("CONSTRUCTION",
                Provenance.construction("Reinvent").summary());
        assertEquals("INPUT", Provenance.input("fragmented").summary());
        assertEquals("CROSSOVER M00000001(0) M00000002(1)", Provenance.crossover(
                new Parent(0, "M00000001", 12, Arrays.asList(3L, 7L)),
                new Parent(1, "M00000002", 15, Arrays.asList(1L))).summary());
        assertEquals("MUTATION CHANGEBRANCH M000111(3)",
                Provenance.mutation(new Parent(3, "M000111", 20))
                        .withMutation(MutationType.CHANGEBRANCH, 8L, 3)
                        .summary());
        assertEquals("MUTATION",
                Provenance.mutation(new Parent(0, null, 1)).summary());
    }

//------------------------------------------------------------------------------

    @Test
    public void testParseSummaryReadsParentLinks()
    {
        Summary crossover = Provenance.parseSummary(
                "CROSSOVER M00000001(0) M00000002(1)");
        assertEquals(Source.CROSSOVER, crossover.getSource());
        assertTrue(crossover.getMutationTypes().isEmpty());
        assertEquals(Arrays.asList(
                new ParentLink("M00000001", 0),
                new ParentLink("M00000002", 1)),
                crossover.getParents());

        Provenance mutation = Provenance.mutation(new Parent(3, "M000111", 20))
                .withMutation(MutationType.CHANGEBRANCH, 8L, 3);
        Summary parsed = Provenance.parseSummary(mutation.summary());
        assertEquals(Source.MUTATION, parsed.getSource());
        assertEquals(Collections.singletonList(MutationType.CHANGEBRANCH),
                parsed.getMutationTypes());
        assertEquals(Collections.singletonList(new ParentLink("M000111", 3)),
                parsed.getParents());

        Summary construction = Provenance.parseSummary("CONSTRUCTION");
        assertEquals(Source.CONSTRUCTION, construction.getSource());
        assertTrue(construction.getParents().isEmpty());

        assertNull(Provenance.parseSummary(null));
        assertNull(Provenance.parseSummary(""));
        assertNull(Provenance.parseSummary("NEW"));
        assertNull(Provenance.parseSummary(
                "MUTATION M000111(3) CHANGEBRANCH"));
    }

//------------------------------------------------------------------------------

    @Test
    public void testMutationSiteIsNotAddedToOtherSources()
    {
        Provenance construction = Provenance.construction()
                .withMutation(MutationType.EXTEND, 4L, 1);
        assertEquals(Provenance.construction(), construction);

        Provenance crossover = Provenance.crossover(
                new Parent(0, "A", 1), new Parent(0, "B", 2));
        assertEquals(crossover, crossover.withMutation(
                MutationType.DELETE, 5L, 0));
    }

//------------------------------------------------------------------------------

    private static void assertRoundTrip(Provenance provenance)
    {
        assertEquals(provenance, Provenance.parse(provenance.toString()));
    }
}
