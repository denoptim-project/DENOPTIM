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

package denoptim.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import denoptim.graph.CandidateLW;

/**
 * Tests for the lineage and posterity drawn on the evolution plot.
 */

public class GUIInspectGARunTest
{
//------------------------------------------------------------------------------

    @Test
    public void testLineageFollowsParentsUntilNoneRemain()
    {
        CandidateLW root = candidate("M0", 0, 1.0, "CONSTRUCTION");
        CandidateLW mutant = candidate("M1", 1, 2.0,
                "MUTATION CHANGEBRANCH M0(0)");
        CandidateLW child = candidate("M2", 2, 3.0,
                "CROSSOVER M0(0) M1(1)");
        Map<Integer,Map<String,CandidateLW>> index = index(root, mutant, child);

        List<double[][]> segments = GUIInspectGARun.lineageSegments(child,
                index);

        assertEquals(3, segments.size());
        assertSegment(segments.get(0), 2, 3.0, 0, 1.0);
        assertSegment(segments.get(1), 2, 3.0, 1, 2.0);
        assertSegment(segments.get(2), 1, 2.0, 0, 1.0);

        List<CandidateLW> members = GUIInspectGARun.lineageMembers(child,
                index);
        assertEquals(3, members.size());
        assertEquals("M0", members.get(0).getName());
        assertEquals("M1", members.get(1).getName());
        assertEquals("M2", members.get(2).getName());
    }

//------------------------------------------------------------------------------

    @Test
    public void testLineageStopsWhenParentIsNotLoaded()
    {
        CandidateLW child = candidate("M2", 2, 3.0,
                "MUTATION CHANGEBRANCH M9(0)");

        List<double[][]> segments = GUIInspectGARun.lineageSegments(child,
                index(child));

        assertEquals(0, segments.size());
    }

//------------------------------------------------------------------------------

    @Test
    public void testPosterityKeepsDirectMutationAndCrossoverOffspring()
    {
        CandidateLW selected = candidate("M0", 0, 1.0, "CONSTRUCTION");
        CandidateLW sameGeneration = candidate("M9", 0, 1.5,
                "MUTATION CHANGEBRANCH M0(0)");
        CandidateLW mutant = candidate("M1", 1, 2.0,
                "MUTATION CHANGEBRANCH M0(0)");
        CandidateLW crossover = candidate("M2", 2, 3.0,
                "CROSSOVER M0(0) M1(1)");
        CandidateLW grandchild = candidate("M3", 3, 4.0,
                "MUTATION EXTEND M1(1)");
        CandidateLW other = candidate("M4", 1, 2.5,
                "MUTATION CHANGEBRANCH M8(0)");
        CandidateLW built = candidate("M5", 1, 2.2, "CONSTRUCTION");
        Map<Integer,Map<String,CandidateLW>> candidates = index(selected,
                sameGeneration, mutant, crossover, grandchild, other, built);

        GUIInspectGARun.PosterityLines lines = GUIInspectGARun.posteritySegments(
                selected, candidates);

        assertEquals(1, lines.mutations.size());
        assertEquals("M1", lines.mutationCandidates.get(0).getName());
        assertSegment(lines.mutations.get(0), 1, 2.0, 0, 1.0);
        assertEquals(1, lines.crossovers.size());
        assertEquals("M2", lines.crossoverCandidates.get(0).getName());
        assertSegment(lines.crossovers.get(0), 2, 3.0, 0, 1.0);

        GUIInspectGARun.PosterityLines fromMutant =
                GUIInspectGARun.posteritySegments(mutant, candidates);
        assertEquals(1, fromMutant.mutations.size());
        assertSegment(fromMutant.mutations.get(0), 3, 4.0, 1, 2.0);
        assertEquals(1, fromMutant.crossovers.size());
        assertSegment(fromMutant.crossovers.get(0), 2, 3.0, 1, 2.0);
        assertTrue(GUIInspectGARun.posteritySegments(grandchild, candidates)
                .mutations.isEmpty());
    }

//------------------------------------------------------------------------------

    private static void assertSegment(double[][] segment, int childGeneration,
            double childFitness, int parentGeneration, double parentFitness)
    {
        assertEquals(childGeneration, segment[0][0]);
        assertEquals(childFitness, segment[1][0]);
        assertEquals(parentGeneration, segment[0][1]);
        assertEquals(parentFitness, segment[1][1]);
    }

//------------------------------------------------------------------------------

    private static CandidateLW candidate(String name, int generation,
            double fitness, String generatingSource)
    {
        CandidateLW candidate = new CandidateLW(name, name, name + ".sdf");
        candidate.setGeneration(generation);
        candidate.setFitness(fitness);
        candidate.setGeneratingSource(generatingSource);
        return candidate;
    }

//------------------------------------------------------------------------------

    private static Map<Integer,Map<String,CandidateLW>> index(
            CandidateLW... candidates)
    {
        Map<Integer,Map<String,CandidateLW>> index =
                new HashMap<Integer,Map<String,CandidateLW>>();
        for (CandidateLW candidate : candidates)
        {
            index.computeIfAbsent(candidate.getGeneration(),
                    key -> new HashMap<String,CandidateLW>())
                    .put(candidate.getName(), candidate);
        }
        return index;
    }
}
