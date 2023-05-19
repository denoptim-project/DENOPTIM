/*
 *   DENOPTIM
 *   Copyright (C) 2022 Marco Foscato <marco.foscato@uib.no>
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

package denoptim.ga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.vecmath.Point3d;

import org.jgrapht.alg.util.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.silent.Bond;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.graph.APClass;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.DGraph;
import denoptim.graph.Edge.BondType;
import denoptim.graph.EmptyVertex;
import denoptim.graph.Fragment;
import denoptim.graph.GraphPattern;
import denoptim.graph.Ring;
import denoptim.graph.SymmetricVertexes;
import denoptim.graph.Template;
import denoptim.graph.Template.ContractLevel;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;
import denoptim.graph.rings.RingClosingAttractor;
import denoptim.graph.rings.RingClosureParameters;
import denoptim.io.DenoptimIO;
import denoptim.logging.Monitor;
import denoptim.molecularmodeling.ThreeDimTreeBuilder;
import denoptim.programs.denovo.GAParameters;
import denoptim.utils.GraphUtils;
import denoptim.utils.MoleculeUtils;
import denoptim.utils.Randomizer;

/**
 * Unit test
 * 
 * @author Marco Foscato
 */

public class GraphOperationsTest {

    private static APClass APCA, APCB, APCC, APCD;
    
    IChemObjectBuilder chemBuilder = DefaultChemObjectBuilder.getInstance();
    private final Random rng = new Random();
    private static APClass DEFAULT_APCLASS;

//------------------------------------------------------------------------------

    @BeforeAll
    static void setUpClass() {
        try {
            DEFAULT_APCLASS = APClass.make("norule:0");
        } catch (DENOPTIMException e) {
            e.printStackTrace();
        }
    }

//------------------------------------------------------------------------------

    @Test
    public void testExtractPattern_singleRingSystem() throws Throwable
    {
        DGraph g = getThreeCycle();

        List<DGraph> subgraphs = g.extractPattern(GraphPattern.RING);

        assertEquals(1, subgraphs.size());
        DGraph actual = subgraphs.get(0);
        DGraph expected = g;

        assertEquals(expected.getVertexCount(), actual.getVertexCount());
        assertEquals(expected.getEdgeCount(), actual.getEdgeCount());
        assertEquals(1, actual.getRingCount());

        assertTrue(DGraph.compareGraphNodes(expected.getSourceVertex(),
                expected, actual.getSourceVertex(), actual));
    }

//------------------------------------------------------------------------------

    @Test
    public void testExtractPattern_returnsEmptyListIfNoRings() 
            throws Throwable
    {
        DGraph g = getThreeCycle();
        g.removeRing(g.getRings().get(0));
        List<DGraph> subgraphs = g.extractPattern(GraphPattern.RING);

        assertEquals(0, subgraphs.size());
    }

//------------------------------------------------------------------------------

    @Test
    public void testExtractPattern_fusedRings() throws Throwable 
    {
        ExtractPatternCase testCase = getFusedRings();

        List<DGraph> subgraphs = testCase.g.extractPattern(GraphPattern.RING);

        assertTrue(testCase.matchesExpected(subgraphs));
    }

//------------------------------------------------------------------------------

    /**
     * Returns a molecule consisting of two pairs of fused rings connected
     * by an oxygen atom. The dots represents chords. The molecule looks as
     * follows:
     *     Cl
     *     |   ↑
     * O - C - C →       ← N - |
     * .   |   .       ↑   .   |
     * .   N - C - O - C - C - C →
     * .   |   .       |   .   ↓
     * . . N . .       O . .
     *
     * The atoms are labelled in order of the leftmost then topmost.
     *     2
     *     |   ↑
     * 0 - 1 - 4 →        ← 12 - |
     * .   |   .       ↑    .    |
     * .   3 - 6 - 7 - 8 - 10 - 11 →
     * .   |   .       |   .     ↓
     * . . 5 . .       9 . .
     */
    private ExtractPatternCase getFusedRings() throws Throwable
    {
        BiFunction<String, Boolean, Vertex> vertexSupplier =
                (s, isRCV) -> {
            int apCount = 0;
            switch (s) {
                case "Cl":
                    apCount = 1;
                    break;
                case "O":
                    apCount = 2;
                    break;
                case "N":
                    apCount = 3;
                    break;
                case "C":
                    apCount = 4;
                    break;
            }
            return buildFragment(s, apCount, isRCV);
        };

        /* We label the vertices in order of top left to bottom right. */
        List<Vertex> vertices = Stream.of(
                new Pair<>("O", true), new Pair<>("C", false),
                new Pair<>("Cl", false), new Pair<>("N",false),
                new Pair<>("C", true), new Pair<>("N", true),
                new Pair<>("C", true), new Pair<>("O", false),
                new Pair<>("C", false), new Pair<>("O", true),
                new Pair<>("C", true), new Pair<>("C", false),
                new Pair<>("N", true)
        ).map(p -> vertexSupplier.apply(p.getFirst(), p.getSecond()))
                .collect(Collectors.toList());

        /* Here we specify the connections between atoms. Previously
        connected vertices are not connected twice. Chords are not connected. */
        List<List<Integer>> edges = Arrays.asList(
                Arrays.asList(1),
                Arrays.asList(2, 3, 4),
                Arrays.asList(),
                Arrays.asList(5, 6),
                Arrays.asList(),
                Arrays.asList(),
                Arrays.asList(7),
                Arrays.asList(8),
                Arrays.asList(9, 10),
                Arrays.asList(),
                Arrays.asList(11),
                Arrays.asList(12)
        );

        DGraph g = null;
        try
        {
            g = buildGraph(vertices, edges);
        } catch (DENOPTIMException e)
        {
            e.printStackTrace();
        }
        g.renumberGraphVertices();
        DGraph.setScaffold(vertices.get(0));
        addRings(vertices, g);
        Set<DGraph> expectedSubgraphs = getExpectedSubgraphs(g);
        return new ExtractPatternCase(g, 2, expectedSubgraphs);
    }

//------------------------------------------------------------------------------

    private DGraph buildGraph(List<Vertex> vertices,
                                     List<List<Integer>> edges) 
                                             throws DENOPTIMException {
        DGraph g = new DGraph();
        g.addVertex(vertices.get(0));
        for (int i = 0; i < edges.size(); i++) {
            Vertex srcVertex = vertices.get(i);
            for (Integer adj : edges.get(i)) {
                Vertex trgVertex = vertices.get(adj);

                AttachmentPoint srcAP = srcVertex
                        .getAttachmentPoints()
                        .stream()
                        .filter(ap -> ap.getEdgeUser() == null)
                        .findFirst()
                        .get();
                AttachmentPoint trgAP = trgVertex
                        .getAttachmentPoints()
                        .stream()
                        .filter(ap -> ap.getEdgeUser() == null)
                        .findFirst()
                        .get();

                try {
                    g.appendVertexOnAP(srcAP, trgAP);
                } catch (DENOPTIMException e) {
                    // Should not happen
                    e.printStackTrace();
                }
            }
        }
        return g;
    }

//------------------------------------------------------------------------------

    private void addRings(List<Vertex> vertices, DGraph g) {
        List<List<Vertex>> ringVertices = Stream.of(
                Arrays.asList(0, 1, 3, 5),
                Arrays.asList(4, 1, 3, 6),
                Arrays.asList(6, 3, 5),
                Arrays.asList(9, 8, 10),
                Arrays.asList(10, 11, 12))
                .map(indices -> indices
                        .stream()
                        .map(vertices::get)
                        .collect(Collectors.toList())
                )
                .collect(Collectors.toList());

        for (List<Vertex> vs : ringVertices) {
            Ring r = new Ring();
            for (Vertex v : vs) {
                r.addVertex(v);
            }
            g.addRing(r);
        }
    }

//------------------------------------------------------------------------------

    private Set<DGraph> getExpectedSubgraphs(DGraph graph) {
        List<Set<Integer>> keepVertices = Stream.of(
                Stream.of(0, 1, 3, 4, 5, 6),
                Stream.of(8, 9, 10, 11, 12))
                .map(indices -> indices.collect(Collectors.toSet()))
                .collect(Collectors.toList());

        List<DGraph> expectedSubgraphs = new ArrayList<>(2);
        for (Set<Integer> keepVertex : keepVertices) {
            DGraph expSubgraph = graph.clone();
            List<Vertex> vertices = expSubgraph.getVertexList();
            Set<Vertex> removeVertices = IntStream
                    .range(0, vertices.size())
                    .filter(i -> !keepVertex.contains(i))
                    .mapToObj(vertices::get)
                    .collect(Collectors.toSet());

            for (Vertex removeVertex : removeVertices) {
                expSubgraph.removeVertex(removeVertex);
            }
            expectedSubgraphs.add(expSubgraph);
        }

        return new HashSet<>(expectedSubgraphs);
    }

//------------------------------------------------------------------------------

    private Vertex buildFragment(String elementSymbol, int apCount,
            boolean isRCV)
    {
        try
        {
            IAtomContainer atomContainer = chemBuilder.newAtomContainer();
            IAtom oxygen = chemBuilder.newAtom();
            oxygen.setSymbol(elementSymbol);
            atomContainer.addAtom(oxygen);
    
            Fragment v = new Fragment(
                    GraphUtils.getUniqueVertexIndex(), atomContainer,
                    BBType.FRAGMENT, isRCV);
            for (int i = 0; i < apCount; i++) 
            {
                v.addAP(0, DEFAULT_APCLASS, getRandomVector());
            }
            return v;
        } catch (Throwable t)
        {
            return null;
        }
   }

//------------------------------------------------------------------------------

    private Point3d getRandomVector() 
    {
        double precision = 10*10*10*10;
        return new Point3d(
                (double) (Math.round(rng.nextDouble() * precision)) / precision,
                (double) (Math.round(rng.nextDouble() * precision)) / precision,
                (double) (Math.round(rng.nextDouble() * precision)) / precision
        );
    }

//------------------------------------------------------------------------------

    /**
     * Returns a 3-cycle. The S marks the scaffold vertex:
     *    /--- S ---\
     *   /           \
     * RCV -(chord)- RCV
     */
    private DGraph getThreeCycle() throws DENOPTIMException 
    {
        EmptyVertex v1 = new EmptyVertex(0);
        EmptyVertex rcv1 = new EmptyVertex(1, new ArrayList<>(),
                new ArrayList<>(), true);
        EmptyVertex rcv2 = new EmptyVertex(2, new ArrayList<>(),
                new ArrayList<>(), true);

        List<EmptyVertex> vertices = Arrays.asList(v1, rcv1, rcv2);
        for (EmptyVertex v : vertices) {
            v.setBuildingBlockType(BBType.FRAGMENT);
            v.addAP();
        }
        // Need an additional AP on v1
        v1.addAP();

        DGraph g = new DGraph();
        g.addVertex(v1);
        g.appendVertexOnAP(v1.getAP(0), rcv1.getAP(0));
        g.appendVertexOnAP(v1.getAP(1), rcv2.getAP(0));

        Ring r = new Ring(new ArrayList<>(
                Arrays.asList(rcv1, v1, rcv2)));
        g.addRing(r);

        g.renumberGraphVertices();
        return g;
    }

//------------------------------------------------------------------------------

    private static final class ExtractPatternCase 
    {
        final DGraph g;
        final int expectedSize;
        final Set<DGraph> expectedGraphs;
        final Comparator<DGraph> graphComparator = (gA, gB) ->
                gA.sameAs(gB, new StringBuilder()) ? 0 : -1;

        private ExtractPatternCase(DGraph g, int expectedSize,
                                         Set<DGraph> expectedGraphs) {
            this.g = g;
            this.expectedSize = expectedSize;
            this.expectedGraphs = expectedGraphs;
        }

        private boolean matchesExpected(Collection<DGraph> actuals) {
            if (actuals.size() != expectedSize) {
                return false;
            }
                
            Set<DGraph> unmatchedGraphs = new HashSet<>(expectedGraphs);
            for (DGraph g : actuals) 
            {
                boolean hasMatch = expectedGraphs
                        .stream()
                        .anyMatch(exp -> graphComparator.compare(g, exp) == 0);
                if (hasMatch) {
                    unmatchedGraphs = unmatchedGraphs
                            .stream()
                            .filter(exp -> graphComparator.compare(g, exp) != 0)
                            .collect(Collectors.toSet());
                } else {
                    return false;
                }
            }

            // Check that no graphs are missing from actual
            return unmatchedGraphs.size() == 0;
        }
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testLocateCompatibleXOverPoints() throws Exception
    {
        FragmentSpace fragSpace = prepare();
        DGraph[] pair = getPairOfTestGraphs();
        DGraph graphA = pair[0];
        DGraph graphB = pair[1];
        
        Template t1 = (Template) graphA.getVertexAtPosition(1);
        Template t2 = (Template) graphB.getVertexAtPosition(1);
        // Making some empty vertexes unique to enable swapping (otherwise they
        // are seen as the same node and excluded from xover sites list)
        Vertex v5A = t1.getInnerGraph().getVertexAtPosition(5);
        String k = "Uniquefier";
        v5A.setUniquefyingProperty(k);
        v5A.setProperty(k, "123");
        Vertex v3B = t2.getInnerGraph().getVertexAtPosition(3);
        v3B.setUniquefyingProperty(k);
        v3B.setProperty(k, "789");
        
        // Prepare the input and expected output
        List<ContractLevel> contracts = new ArrayList<ContractLevel>();
        contracts.add(ContractLevel.FREE);
        contracts.add(ContractLevel.FIXED_STRUCT);
        contracts.add(ContractLevel.FIXED);
        
        List<Integer> expectedNumberOfSites = new ArrayList<Integer>();
        expectedNumberOfSites.add(17);
        expectedNumberOfSites.add(12); // Only two allowed xovers
        expectedNumberOfSites.add(10); // No crossover inside template
        
        List<Set<String>> expectedInvariants = new ArrayList<Set<String>>();
        // This code is generated by this very  method. See below.
        Set<String> invariantFREEContract = new HashSet<String>();
        invariantFREEContract.add(""+GraphUtils.getLabel(graphA,1)+"_"+GraphUtils.getLabel(graphA,4)+"_"+GraphUtils.getLabel(graphA,2)+"_"+GraphUtils.getLabel(graphA,3)+"_"+GraphUtils.getLabel(graphA,5)+"_"+"@@@_"+GraphUtils.getLabel(graphB,1)+"_"+GraphUtils.getLabel(graphB,5)+"_"+GraphUtils.getLabel(graphB,2)+"_"+GraphUtils.getLabel(graphB,3)+"_"+GraphUtils.getLabel(graphB,4)+"_");
        invariantFREEContract.add(""+GraphUtils.getLabel(graphA,1)+"_"+GraphUtils.getLabel(graphA,4)+"_"+GraphUtils.getLabel(graphA,2)+"_"+GraphUtils.getLabel(graphA,5)+"_"+"@@@_"+GraphUtils.getLabel(graphB,1)+"_"+GraphUtils.getLabel(graphB,5)+"_"+GraphUtils.getLabel(graphB,2)+"_");
        invariantFREEContract.add(""+GraphUtils.getLabel(graphA,1)+"_"+GraphUtils.getLabel(graphA,4)+"_"+GraphUtils.getLabel(graphA,2)+"_"+GraphUtils.getLabel(graphA,3)+"_"+GraphUtils.getLabel(graphA,5)+"_"+"@@@_"+GraphUtils.getLabel(graphB,2)+"_"+GraphUtils.getLabel(graphB,3)+"_"+GraphUtils.getLabel(graphB,4)+"_");
        invariantFREEContract.add(""+GraphUtils.getLabel(graphA,1)+"_"+GraphUtils.getLabel(graphA,4)+"_"+GraphUtils.getLabel(graphA,2)+"_"+GraphUtils.getLabel(graphA,5)+"_"+"@@@_"+GraphUtils.getLabel(graphB,2)+"_");
        invariantFREEContract.add(""+GraphUtils.getLabel(graphA,2)+"_"+GraphUtils.getLabel(graphA,3)+"_"+"@@@_"+GraphUtils.getLabel(graphB,1)+"_"+GraphUtils.getLabel(graphB,5)+"_"+GraphUtils.getLabel(graphB,2)+"_"+GraphUtils.getLabel(graphB,3)+"_"+GraphUtils.getLabel(graphB,4)+"_");
        invariantFREEContract.add(""+GraphUtils.getLabel(graphA,2)+"_"+"@@@_"+GraphUtils.getLabel(graphB,1)+"_"+GraphUtils.getLabel(graphB,5)+"_"+GraphUtils.getLabel(graphB,2)+"_");
        invariantFREEContract.add(""+GraphUtils.getLabel(graphA,2)+"_"+GraphUtils.getLabel(graphA,3)+"_"+"@@@_"+GraphUtils.getLabel(graphB,2)+"_"+GraphUtils.getLabel(graphB,3)+"_"+GraphUtils.getLabel(graphB,4)+"_");
        invariantFREEContract.add(""+GraphUtils.getLabel(graphA,3)+"_"+"@@@_"+GraphUtils.getLabel(graphB,3)+"_"+GraphUtils.getLabel(graphB,4)+"_");
        invariantFREEContract.add(""+GraphUtils.getLabel(graphA,5)+"_"+"@@@_"+GraphUtils.getLabel(graphB,1)+"_"+GraphUtils.getLabel(graphB,5)+"_"+GraphUtils.getLabel(graphB,2)+"_"+GraphUtils.getLabel(graphB,3)+"_"+GraphUtils.getLabel(graphB,4)+"_");
        invariantFREEContract.add(""+GraphUtils.getLabel(graphA,5)+"_"+"@@@_"+GraphUtils.getLabel(graphB,2)+"_"+GraphUtils.getLabel(graphB,3)+"_"+GraphUtils.getLabel(graphB,4)+"_");
        invariantFREEContract.add(""+GraphUtils.getLabel(t1.getInnerGraph(),2)+"_"+GraphUtils.getLabel(t1.getInnerGraph(),3)+"_"+"@@@_"+GraphUtils.getLabel(t2.getInnerGraph(),1)+"_"+GraphUtils.getLabel(t2.getInnerGraph(),2)+"_"+GraphUtils.getLabel(t2.getInnerGraph(),3)+"_");
        invariantFREEContract.add(""+GraphUtils.getLabel(t1.getInnerGraph(),2)+"_"+"@@@_"+GraphUtils.getLabel(t2.getInnerGraph(),1)+"_"+GraphUtils.getLabel(t2.getInnerGraph(),2)+"_");
        invariantFREEContract.add(""+GraphUtils.getLabel(t1.getInnerGraph(),2)+"_"+GraphUtils.getLabel(t1.getInnerGraph(),3)+"_"+"@@@_"+GraphUtils.getLabel(t2.getInnerGraph(),2)+"_"+GraphUtils.getLabel(t2.getInnerGraph(),3)+"_");
        invariantFREEContract.add(""+GraphUtils.getLabel(t1.getInnerGraph(),2)+"_"+"@@@_"+GraphUtils.getLabel(t2.getInnerGraph(),2)+"_");
        invariantFREEContract.add(""+GraphUtils.getLabel(t1.getInnerGraph(),3)+"_"+"@@@_"+GraphUtils.getLabel(t2.getInnerGraph(),3)+"_");
        invariantFREEContract.add(""+GraphUtils.getLabel(t1.getInnerGraph(),4)+"_"+GraphUtils.getLabel(t1.getInnerGraph(),5)+"_"+"@@@_"+GraphUtils.getLabel(t2.getInnerGraph(),3)+"_");
        invariantFREEContract.add(""+GraphUtils.getLabel(t1.getInnerGraph(),5)+"_"+"@@@_"+GraphUtils.getLabel(t2.getInnerGraph(),3)+"_");
        expectedInvariants.add(invariantFREEContract);
        Set<String> invariantFIXED_STRUCTContract = new HashSet<String>();
        invariantFIXED_STRUCTContract.add(""+GraphUtils.getLabel(graphA,1)+"_"+GraphUtils.getLabel(graphA,4)+"_"+GraphUtils.getLabel(graphA,2)+"_"+GraphUtils.getLabel(graphA,3)+"_"+GraphUtils.getLabel(graphA,5)+"_"+"@@@_"+GraphUtils.getLabel(graphB,1)+"_"+GraphUtils.getLabel(graphB,5)+"_"+GraphUtils.getLabel(graphB,2)+"_"+GraphUtils.getLabel(graphB,3)+"_"+GraphUtils.getLabel(graphB,4)+"_");
        invariantFIXED_STRUCTContract.add(""+GraphUtils.getLabel(graphA,1)+"_"+GraphUtils.getLabel(graphA,4)+"_"+GraphUtils.getLabel(graphA,2)+"_"+GraphUtils.getLabel(graphA,5)+"_"+"@@@_"+GraphUtils.getLabel(graphB,1)+"_"+GraphUtils.getLabel(graphB,5)+"_"+GraphUtils.getLabel(graphB,2)+"_");
        invariantFIXED_STRUCTContract.add(""+GraphUtils.getLabel(graphA,1)+"_"+GraphUtils.getLabel(graphA,4)+"_"+GraphUtils.getLabel(graphA,2)+"_"+GraphUtils.getLabel(graphA,3)+"_"+GraphUtils.getLabel(graphA,5)+"_"+"@@@_"+GraphUtils.getLabel(graphB,2)+"_"+GraphUtils.getLabel(graphB,3)+"_"+GraphUtils.getLabel(graphB,4)+"_");
        invariantFIXED_STRUCTContract.add(""+GraphUtils.getLabel(graphA,1)+"_"+GraphUtils.getLabel(graphA,4)+"_"+GraphUtils.getLabel(graphA,2)+"_"+GraphUtils.getLabel(graphA,5)+"_"+"@@@_"+GraphUtils.getLabel(graphB,2)+"_");
        invariantFIXED_STRUCTContract.add(""+GraphUtils.getLabel(graphA,2)+"_"+GraphUtils.getLabel(graphA,3)+"_"+"@@@_"+GraphUtils.getLabel(graphB,1)+"_"+GraphUtils.getLabel(graphB,5)+"_"+GraphUtils.getLabel(graphB,2)+"_"+GraphUtils.getLabel(graphB,3)+"_"+GraphUtils.getLabel(graphB,4)+"_");
        invariantFIXED_STRUCTContract.add(""+GraphUtils.getLabel(graphA,2)+"_"+"@@@_"+GraphUtils.getLabel(graphB,1)+"_"+GraphUtils.getLabel(graphB,5)+"_"+GraphUtils.getLabel(graphB,2)+"_");
        invariantFIXED_STRUCTContract.add(""+GraphUtils.getLabel(graphA,2)+"_"+GraphUtils.getLabel(graphA,3)+"_"+"@@@_"+GraphUtils.getLabel(graphB,2)+"_"+GraphUtils.getLabel(graphB,3)+"_"+GraphUtils.getLabel(graphB,4)+"_");
        invariantFIXED_STRUCTContract.add(""+GraphUtils.getLabel(graphA,3)+"_"+"@@@_"+GraphUtils.getLabel(graphB,3)+"_"+GraphUtils.getLabel(graphB,4)+"_");
        invariantFIXED_STRUCTContract.add(""+GraphUtils.getLabel(graphA,5)+"_"+"@@@_"+GraphUtils.getLabel(graphB,1)+"_"+GraphUtils.getLabel(graphB,5)+"_"+GraphUtils.getLabel(graphB,2)+"_"+GraphUtils.getLabel(graphB,3)+"_"+GraphUtils.getLabel(graphB,4)+"_");
        invariantFIXED_STRUCTContract.add(""+GraphUtils.getLabel(graphA,5)+"_"+"@@@_"+GraphUtils.getLabel(graphB,2)+"_"+GraphUtils.getLabel(graphB,3)+"_"+GraphUtils.getLabel(graphB,4)+"_");
        invariantFIXED_STRUCTContract.add(""+GraphUtils.getLabel(t1.getInnerGraph(),3)+"_"+"@@@_"+GraphUtils.getLabel(t2.getInnerGraph(),3)+"_");
        invariantFIXED_STRUCTContract.add(""+GraphUtils.getLabel(t1.getInnerGraph(),5)+"_"+"@@@_"+GraphUtils.getLabel(t2.getInnerGraph(),3)+"_");
        expectedInvariants.add(invariantFIXED_STRUCTContract);
        Set<String> invariantFIXEDContract = new HashSet<String>();
        invariantFIXEDContract.add(""+GraphUtils.getLabel(graphA,1)+"_"+GraphUtils.getLabel(graphA,4)+"_"+GraphUtils.getLabel(graphA,2)+"_"+GraphUtils.getLabel(graphA,3)+"_"+GraphUtils.getLabel(graphA,5)+"_"+"@@@_"+GraphUtils.getLabel(graphB,1)+"_"+GraphUtils.getLabel(graphB,5)+"_"+GraphUtils.getLabel(graphB,2)+"_"+GraphUtils.getLabel(graphB,3)+"_"+GraphUtils.getLabel(graphB,4)+"_");
        invariantFIXEDContract.add(""+GraphUtils.getLabel(graphA,1)+"_"+GraphUtils.getLabel(graphA,4)+"_"+GraphUtils.getLabel(graphA,2)+"_"+GraphUtils.getLabel(graphA,5)+"_"+"@@@_"+GraphUtils.getLabel(graphB,1)+"_"+GraphUtils.getLabel(graphB,5)+"_"+GraphUtils.getLabel(graphB,2)+"_");
        invariantFIXEDContract.add(""+GraphUtils.getLabel(graphA,1)+"_"+GraphUtils.getLabel(graphA,4)+"_"+GraphUtils.getLabel(graphA,2)+"_"+GraphUtils.getLabel(graphA,3)+"_"+GraphUtils.getLabel(graphA,5)+"_"+"@@@_"+GraphUtils.getLabel(graphB,2)+"_"+GraphUtils.getLabel(graphB,3)+"_"+GraphUtils.getLabel(graphB,4)+"_");
        invariantFIXEDContract.add(""+GraphUtils.getLabel(graphA,1)+"_"+GraphUtils.getLabel(graphA,4)+"_"+GraphUtils.getLabel(graphA,2)+"_"+GraphUtils.getLabel(graphA,5)+"_"+"@@@_"+GraphUtils.getLabel(graphB,2)+"_");
        invariantFIXEDContract.add(""+GraphUtils.getLabel(graphA,2)+"_"+GraphUtils.getLabel(graphA,3)+"_"+"@@@_"+GraphUtils.getLabel(graphB,1)+"_"+GraphUtils.getLabel(graphB,5)+"_"+GraphUtils.getLabel(graphB,2)+"_"+GraphUtils.getLabel(graphB,3)+"_"+GraphUtils.getLabel(graphB,4)+"_");
        invariantFIXEDContract.add(""+GraphUtils.getLabel(graphA,2)+"_"+"@@@_"+GraphUtils.getLabel(graphB,1)+"_"+GraphUtils.getLabel(graphB,5)+"_"+GraphUtils.getLabel(graphB,2)+"_");
        invariantFIXEDContract.add(""+GraphUtils.getLabel(graphA,2)+"_"+GraphUtils.getLabel(graphA,3)+"_"+"@@@_"+GraphUtils.getLabel(graphB,2)+"_"+GraphUtils.getLabel(graphB,3)+"_"+GraphUtils.getLabel(graphB,4)+"_");
        invariantFIXEDContract.add(""+GraphUtils.getLabel(graphA,3)+"_"+"@@@_"+GraphUtils.getLabel(graphB,3)+"_"+GraphUtils.getLabel(graphB,4)+"_");
        invariantFIXEDContract.add(""+GraphUtils.getLabel(graphA,5)+"_"+"@@@_"+GraphUtils.getLabel(graphB,1)+"_"+GraphUtils.getLabel(graphB,5)+"_"+GraphUtils.getLabel(graphB,2)+"_"+GraphUtils.getLabel(graphB,3)+"_"+GraphUtils.getLabel(graphB,4)+"_");
        invariantFIXEDContract.add(""+GraphUtils.getLabel(graphA,5)+"_"+"@@@_"+GraphUtils.getLabel(graphB,2)+"_"+GraphUtils.getLabel(graphB,3)+"_"+GraphUtils.getLabel(graphB,4)+"_");
        expectedInvariants.add(invariantFIXEDContract);
            
        for (int i=0; i<contracts.size(); i++)
        {   
            t1.setContractLevel(contracts.get(i));
            t2.setContractLevel(contracts.get(i));
            
            List<XoverSite> xoverSites = 
                    GraphOperations.locateCompatibleXOverPoints(graphA, graphB, 
                            fragSpace, 100);
            
            assertEquals(expectedNumberOfSites.get(i), xoverSites.size());
    
            // NB: this is code that is unlocked only in development phase to
            // generate the code that defined the expected invariant. 
            // Here, we exploit the fact that every vertex has a unique label as a 
            // property and the combination of sites generates an invariant.
            // To generate this labels programmatically the following code is used
            // but only after having checked manually.
            boolean writeCode = false; //NB: make this true to generate the code!
            if (writeCode)
            {
                String varName = "invariant" + contracts.get(i) + "Contract";
                System.out.println("Set<String> " + varName + " = new HashSet<String>();");
                for (XoverSite x : xoverSites)
                {
                    String s = "\"\"";
                    for (Vertex v : x.getA())
                    {
                        String g = "";
                        if (v.getGraphOwner()==graphA)
                            g = "graphA";
                        else if (v.getGraphOwner()==graphB)
                            g = "graphB";
                        else if (v.getGraphOwner()==t1.getInnerGraph())
                            g = "t1.getInnerGraph()";
                        else if (v.getGraphOwner()==t2.getInnerGraph())
                            g = "t2.getInnerGraph()";
                        else
                            g = "noGraph";
                        
                        s = s + "+GraphUtils.getLabel("+g+","+v.getGraphOwner().indexOf(v)+")+\"_\"";
                    }
                    s = s + "+\"@@@_\"";
                    for (Vertex v : x.getB())
                    {
                        String g = "";
                        if (v.getGraphOwner()==graphA)
                            g = "graphA";
                        else if (v.getGraphOwner()==graphB)
                            g = "graphB";
                        else if (v.getGraphOwner()==t1.getInnerGraph())
                            g = "t1.getInnerGraph()";
                        else if (v.getGraphOwner()==t2.getInnerGraph())
                            g = "t2.getInnerGraph()";
                        else
                            g = "noGraph";
                        
                        s = s + "+GraphUtils.getLabel("+g+","+v.getGraphOwner().indexOf(v)+")+\"_\"";
                    }
                    System.out.println(varName + ".add("+s+");");
                }
                System.out.println("expectedInvariants.add("+varName+");");
            }
            
            for (XoverSite site : xoverSites)
            {
                String label = "";
                for (Vertex v : site.getA())
                {
                    label = label + GraphUtils.getLabel(v.getGraphOwner(),
                            v.getGraphOwner().indexOf(v)) + "_";
                }
                label = label + "@@@_";
                for (Vertex v : site.getB())
                {
                    label = label + GraphUtils.getLabel(v.getGraphOwner(),
                            v.getGraphOwner().indexOf(v)) + "_";
                }
                assertTrue(expectedInvariants.get(i).contains(label), 
                        "For contract " + i + " (" + contracts.get(i) 
                        + ") - Missing label: "+label);
            }
        }
    }
  
//------------------------------------------------------------------------------
    
    /**
     * Generates a pair of graphs that include templates with free content. 
     * The first graph is
     * <pre>
     *                (A)--(A)-m5
     *               /
     *  m1-(A)--(A)-T1-(A)--(A)-m2-(B)--(B)-m3
     *               \
     *                (C)--(C)-m4
     * </pre>
     * where template 'T1' is:
     * <pre> 
     *     (A)         (C)
     *    /           /
     *  tv0-(A)--(A)-tv1-(B)--(C)-tv2-(A)--(A)-tv3-(A)-
     *                \
     *                 (A)--(A)-tv4-(A)--(A)-tv5-(A)-
     * </pre>
     * 
     * And the second graph is
     * <pre>
     *  f1-(A)--(A)-T1-(A)--(A)-f2-(B)--(B)-f3-(C)--(C)-f4
     *               \
     *                (C)--(C)-f5
     * </pre>
     * where template 'T2' is:
     * <pre> 
     *         (C)
     *        /
     *  -(A)-tw1-(B)--(C)-tw2-(B)--(B)-tw3-(A)-(A)-tw4-(A)
     * </pre>
     */
    private DGraph[] getPairOfTestGraphs() throws Exception
    {
        prepare();
        
        // Prepare special building block: template T1
        EmptyVertex v0 = new EmptyVertex(0);
        v0.addAP(APCA);
        v0.addAP(APCA);
        v0.setProperty("Label", "tv0");
        
        EmptyVertex v1 = new EmptyVertex(1);
        v1.addAP(APCA);
        v1.addAP(APCA);
        v1.addAP(APCB);
        v1.addAP(APCC);
        v1.setProperty("Label", "tv1");
        
        EmptyVertex v2 = new EmptyVertex(2);
        v2.addAP(APCA);
        v2.addAP(APCC);
        v2.setProperty("Label", "tv2");
        
        EmptyVertex v3 = new EmptyVertex(3);
        v3.addAP(APCA);
        v3.addAP(APCA);
        v3.setProperty("Label", "tv3");

        EmptyVertex v4 = new EmptyVertex(4);
        v4.addAP(APCA);
        v4.addAP(APCA);
        v4.setProperty("Label", "tv4");
        
        EmptyVertex v5 = new EmptyVertex(5);
        v5.addAP(APCA);
        v5.addAP(APCA);
        v5.setProperty("Label", "tv5");
        
        DGraph g = new DGraph();
        g.addVertex(v0);
        g.setGraphId(-1);
        g.appendVertexOnAP(v0.getAP(0), v1.getAP(0));
        g.appendVertexOnAP(v1.getAP(2), v2.getAP(1));
        g.appendVertexOnAP(v2.getAP(0), v3.getAP(0));
        g.appendVertexOnAP(v1.getAP(1), v4.getAP(1));
        g.appendVertexOnAP(v4.getAP(0), v5.getAP(1));
        
        Template t1 = new Template(BBType.NONE);
        t1.setInnerGraph(g);
        t1.setProperty("Label", "t1");
        t1.setContractLevel(ContractLevel.FREE);
        
        // Assemble the first graph: graphA
        
        EmptyVertex m1 = new EmptyVertex(101);
        m1.addAP(APCA);
        m1.setProperty("Label", "m101");
        
        EmptyVertex m2 = new EmptyVertex(102);
        m2.addAP(APCA);
        m2.addAP(APCB);
        m2.setProperty("Label", "m102");
        
        EmptyVertex m3 = new EmptyVertex(103);
        m3.addAP(APCB);
        m3.setProperty("Label", "m103");

        EmptyVertex m4 = new EmptyVertex(104);
        m4.addAP(APCC);
        m4.setProperty("Label", "m104");

        EmptyVertex m5 = new EmptyVertex(105);
        m5.addAP(APCA);
        m5.setProperty("Label", "m105");
        
        DGraph graphA = new DGraph();
        graphA.addVertex(m1);
        graphA.appendVertexOnAP(m1.getAP(0), t1.getAP(0));
        graphA.appendVertexOnAP(t1.getAP(2), m2.getAP(0));
        graphA.appendVertexOnAP(m2.getAP(1), m3.getAP(0));
        graphA.appendVertexOnAP(t1.getAP(1), m4.getAP(0));
        graphA.appendVertexOnAP(t1.getAP(3), m5.getAP(0));

        graphA.setGraphId(11111);
        
        //Prepare special building block: template T2
        EmptyVertex w1 = new EmptyVertex(11);
        w1.addAP(APCA);
        w1.addAP(APCB);
        w1.addAP(APCC);
        w1.setProperty("Label", "tw11");
        
        EmptyVertex w2 = new EmptyVertex(12);
        w2.addAP(APCB);
        w2.addAP(APCC);
        w2.setProperty("Label", "tw12");
        
        EmptyVertex w3 = new EmptyVertex(13);
        w3.addAP(APCA);
        w3.addAP(APCB);
        w3.setProperty("Label", "tw13");
        
        EmptyVertex w4 = new EmptyVertex(14);
        w4.addAP(APCA);
        w4.addAP(APCA);
        w4.setProperty("Label", "tw14");
        
        DGraph g2 = new DGraph();
        g2.addVertex(w1);
        g2.appendVertexOnAP(w1.getAP(1), w2.getAP(1));
        g2.appendVertexOnAP(w2.getAP(0), w3.getAP(1));
        g2.appendVertexOnAP(w3.getAP(0), w4.getAP(0));
        g2.setGraphId(-2);
        
        Template t2 = new Template(BBType.NONE);
        t2.setInnerGraph(g2);
        t2.setProperty("Label", "t2");
        t2.setContractLevel(ContractLevel.FREE);
        
        // Assemble the second graph: graphB
        
        EmptyVertex f1 = new EmptyVertex(1001);
        f1.addAP(APCA);
        f1.setProperty("Label", "f1001");
        
        EmptyVertex f2 = new EmptyVertex(1002);
        f2.addAP(APCA);
        f2.addAP(APCB);
        f2.setProperty("Label", "f1002");
        
        EmptyVertex f3 = new EmptyVertex(1003);
        f3.addAP(APCB);
        f3.addAP(APCC);
        f3.setProperty("Label", "f1003");

        EmptyVertex f4 = new EmptyVertex(1004);
        f4.addAP(APCC);
        f4.setProperty("Label", "f1004");

        EmptyVertex f5 = new EmptyVertex(1005);
        f5.addAP(APCC);
        f5.setProperty("Label", "f1005");
        
        DGraph graphB = new DGraph();
        graphB.addVertex(f1);
        graphB.appendVertexOnAP(f1.getAP(0), t2.getAP(0));
        graphB.appendVertexOnAP(t2.getAP(2), f2.getAP(0));
        graphB.appendVertexOnAP(f2.getAP(1), f3.getAP(0));
        graphB.appendVertexOnAP(f3.getAP(1), f4.getAP(0));
        graphB.appendVertexOnAP(t2.getAP(1), f5.getAP(0));
        graphB.setGraphId(22222);
        
        DGraph[] pair = new DGraph[2];
        pair[0] = graphA;
        pair[1] = graphB;
        
        return pair;
    }
    
//------------------------------------------------------------------------------

    /**
     * Sets the compatibility matrix (src -> trg);
     * 
     * <pre>
     *      |  A  |  B  |  C  |  D  |
     *    ---------------------------
     *    A |  T  |     |     |     |
     *    ---------------------------
     *    B |     |  T  |  T  |     |
     *    ---------------------------
     *    C |     |     |  T   |     |
     *    ---------------------------
     *    D |     |     |     |  T  |
     * </pre>
     */
    private FragmentSpace prepare() throws Exception
    {
        // Prepare APClass compatibility rules
        APCA = APClass.make("A", 0);
        APCB = APClass.make("B", 0);
        APCC = APClass.make("C", 0);
        APCD = APClass.make("D", 0);
        
        HashMap<APClass,ArrayList<APClass>> cpMap = 
                new HashMap<APClass,ArrayList<APClass>>();
        ArrayList<APClass> lstA = new ArrayList<APClass>();
        lstA.add(APCA);
        cpMap.put(APCA, lstA);
        ArrayList<APClass> lstB = new ArrayList<APClass>();
        lstB.add(APCB);
        lstB.add(APCC);
        cpMap.put(APCB, lstB);
        ArrayList<APClass> lstC = new ArrayList<APClass>();
        lstC.add(APCC);
        cpMap.put(APCC, lstC);
        ArrayList<APClass> lstD = new ArrayList<APClass>();
        lstD.add(APCD);
        cpMap.put(APCD, lstD);
        
        HashMap<APClass,APClass> capMap = new HashMap<APClass,APClass>();
        HashSet<APClass> forbEnds = new HashSet<APClass>();
        
        FragmentSpaceParameters fsp = new FragmentSpaceParameters();
        FragmentSpace fs = new FragmentSpace(fsp,
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(), 
                cpMap, capMap, forbEnds, cpMap);
        fs.setAPclassBasedApproach(true);
        
        return fs;
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testAddRing() throws Exception
    {
        /* This is the graph we work with
         *      
         *         *      *                   *     RCV_M
         *         |      |                   |      |
         * RCV_P--[O]----[C]--[C]--[C]--[C]--[C]----[N]--RCV_M
         *        vO     vC   vC2  vC3  vC4  vC5    vN
         *  
         */
        
        APClass apc = APClass.make("A", 0);

        IAtomContainer iacO = chemBuilder.newAtomContainer();
        IAtom aO = new Atom("O",new Point3d(0,0,0));
        iacO.addAtom(aO);
        Fragment vO = new Fragment(0, iacO,BBType.FRAGMENT);
        vO.addAP(0, new Point3d(0,-1,0), apc);
        vO.addAP(0, new Point3d(2,0,0), apc);
        vO.addAP(0, new Point3d(0,1,0), apc);
        
        IAtomContainer iacC = chemBuilder.newAtomContainer();
        IAtom aC = new Atom("C",new Point3d(0,0,0));
        iacC.addAtom(aC);
        Fragment vC = new Fragment(1, iacC,BBType.FRAGMENT);
        vC.addAP(0, new Point3d(0,-1,0), apc);
        vC.addAP(0, new Point3d(2,0,0), apc);
        vC.addAP(0, new Point3d(0,1,0), apc);
        
        IAtomContainer iacCd = chemBuilder.newAtomContainer();
        IAtom aCd = new Atom("C",new Point3d(0,0,0));
        iacCd.addAtom(aCd);
        Fragment vC2 = new Fragment(2, iacCd,BBType.FRAGMENT);
        vC2.addAP(0, new Point3d(0,-1,0), apc);
        vC2.addAP(0, new Point3d(0,1,0), apc);
        
        Fragment vC3 = vC2.clone();
        vC3.setVertexId(33);
        
        Fragment vC4 = vC2.clone();
        vC4.setVertexId(34);
        
        Fragment vC5 = vC.clone();
        vC5.setVertexId(3);
        
        IAtomContainer iacN = chemBuilder.newAtomContainer();
        IAtom aN = new Atom("N",new Point3d(0,0,0));
        iacN.addAtom(aN);
        Fragment vN = new Fragment(4, iacN,BBType.FRAGMENT);
        vN.addAP(0, new Point3d(0,-1,0), apc);
        vN.addAP(0, new Point3d(2,0,0), apc);
        vN.addAP(0, new Point3d(0,1,0), apc);
        
        APClass atMinus = APClass.RCACLASSMINUS;
        
        IAtomContainer iacD = chemBuilder.newAtomContainer();
        iacD.addAtom(new PseudoAtom(RingClosingAttractor.RCALABELPERAPCLASS.get(atMinus),
                new Point3d(0,0,0)));
        Fragment rcvM = new Fragment(6, iacD,BBType.FRAGMENT);
        rcvM.addAP(0, new Point3d(-1,0,0), atMinus);
        rcvM.setAsRCV(true);
        
        Fragment rcvM2 = rcvM.clone();
        rcvM2.setVertexId(7);
        
        APClass atPlus = APClass.RCACLASSPLUS;
        
        IAtomContainer iacE = chemBuilder.newAtomContainer();
        iacE.addAtom(new PseudoAtom(RingClosingAttractor.RCALABELPERAPCLASS.get(atPlus),
                new Point3d(0,0,0)));
        Fragment rcvP = new Fragment(8, iacE,BBType.FRAGMENT);
        rcvP.addAP(0, new Point3d(-1,0,0), atPlus);
        rcvP.setAsRCV(true);
    
        DGraph graph = new DGraph();
        graph.addVertex(vC);
        graph.appendVertexOnAP(vC.getAP(0), vO.getAP(2));
        graph.appendVertexOnAP(vO.getAP(1), rcvP.getAP(0));
        graph.appendVertexOnAP(vC.getAP(2), vC2.getAP(1));
        graph.appendVertexOnAP(vC2.getAP(0), vC3.getAP(0));
        graph.appendVertexOnAP(vC3.getAP(1), vC4.getAP(0));
        graph.appendVertexOnAP(vC4.getAP(1), vC5.getAP(0));
        graph.appendVertexOnAP(vC5.getAP(2), vN.getAP(0));
        graph.appendVertexOnAP(vN.getAP(1), rcvM.getAP(0));
        graph.appendVertexOnAP(vN.getAP(2), rcvM2.getAP(0));
        
        // Prepare environment to run graph operation. First, a monitor of events
        Monitor mnt = new Monitor();
        
        // Then, a fragment space that allows ring closures
        HashMap<APClass,ArrayList<APClass>> cpMap = 
                new HashMap<APClass,ArrayList<APClass>>();
        ArrayList<APClass> lstA = new ArrayList<APClass>();
        lstA.add(apc);
        cpMap.put(apc, lstA);
        FragmentSpaceParameters fsp = new FragmentSpaceParameters();
        FragmentSpace fs = new FragmentSpace(fsp,
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(), 
                cpMap, 
                new HashMap<APClass,APClass>(), 
                new HashSet<APClass>(),
                cpMap);
        fs.setAPclassBasedApproach(true);
        
        // Then, settings of ring-closing machinery
        RingClosureParameters rcParams = new RingClosureParameters();
        List<Integer> biases = new ArrayList<Integer>();
        for (int i=0; i<rcParams.getMaxRingSize(); i++)
        {
            biases.add(0); // 0 means ring-closure not allowed
        }
        biases.set(5, 1);
        rcParams.setRingSizeBias(biases);
        
        // Then, some GA-parameters
        GAParameters gaParams = new GAParameters();
        gaParams.setParameters(rcParams);
        
        // All ready, do the ring-adding mutation
        assertEquals(0, graph.getRingCount());
        GraphOperations.addRing(vC5, mnt, true, fs, gaParams);
        assertEquals(1, graph.getRingCount());
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testAddFusedRings() throws Exception
    {
        APClass apcA = APClass.make("apcA:1");
        APClass hyd = APClass.make("hyd:1");
        
        // Prepare environment to run graph operation. First, a monitor of events
        Monitor mnt = new Monitor();
        
        ArrayList<Vertex> libFrags = new ArrayList<Vertex>();
        
        APClass APC4EL = APClass.make("4el:0");
        Fragment bridge4el = new Fragment(4);
        IAtom a4_0 = new Atom("Si", new Point3d());
        IAtom a4_1 = new Atom("Si", new Point3d());
        IAtom a4_2 = new Atom("Si", new Point3d());
        IAtom a4_3 = new Atom("Si", new Point3d());
        bridge4el.addAtom(a4_0);
        bridge4el.addAtom(a4_1);
        bridge4el.addAtom(a4_2);
        bridge4el.addAtom(a4_3);
        bridge4el.addBond(new Bond(a4_0, a4_1, IBond.Order.DOUBLE));
        bridge4el.addBond(new Bond(a4_1, a4_2, IBond.Order.SINGLE));
        bridge4el.addBond(new Bond(a4_2, a4_3, IBond.Order.DOUBLE));
        bridge4el.addAP(0, new Point3d(), APC4EL);
        bridge4el.addAP(3, new Point3d(), APC4EL);
        bridge4el.addAP(0, new Point3d(), apcA);
        bridge4el.addAP(1, new Point3d(), apcA);
        bridge4el.addAP(2, new Point3d(), apcA);
        bridge4el.addAP(3, new Point3d(), apcA);
        libFrags.add(bridge4el);
        
        HashMap<APClass,APClass> cappingRules = new HashMap<APClass,APClass>();
        cappingRules.put(apcA, hyd);
        
        ArrayList<Vertex> cappingGroups = new ArrayList<Vertex>();
        Fragment capH = new Fragment();
        capH.addAtom(new Atom("H", new Point3d()));
        capH.addAP(0, new Point3d(1.0, 0, 0), hyd);
        cappingGroups.add(capH);
        
        HashMap<APClass,ArrayList<APClass>> rcCpMap = 
                new HashMap<APClass,ArrayList<APClass>>();
        rcCpMap.put(apcA, new ArrayList<APClass>(Arrays.asList(APC4EL)));
        
        FragmentSpaceParameters fsp = new FragmentSpaceParameters();
        FragmentSpace fs = new FragmentSpace(fsp,
                new ArrayList<Vertex>(),
                libFrags,
                cappingGroups, 
                new HashMap<APClass,ArrayList<APClass>>(), 
                cappingRules, 
                new HashSet<APClass>(),
                rcCpMap); 
        //NB: a non-empty rcCpMap is enough to trigger the automated recognition
        // that we are within the class-based approach.
        
        // Then, some GA-parameters
        GAParameters gaParams = new GAParameters();
        gaParams.setParameters(new RingClosureParameters());
        gaParams.setRandomizer(new Randomizer(1L));
        
        SmilesParser parser = new SmilesParser(chemBuilder);
        IAtomContainer benzene = parser.parseSmiles("c1ccccc1");
        MoleculeUtils.explicitHydrogens(benzene);
        StructureDiagramGenerator sdg = new StructureDiagramGenerator();
        sdg.generateCoordinates(benzene);
        Fragment benzeneScaffold = new Fragment(benzene, BBType.FRAGMENT);
        for (int i= 0; i<6; i++)
        {
            EAUtilsTest.replaceHatomWithAP(benzeneScaffold, i, apcA);
        }
        benzeneScaffold.setVertexId(123);
        DGraph graph = new DGraph();
        graph.addVertex(benzeneScaffold);

        assertEquals(0, graph.getRingCount());
        GraphOperations.addFusedRing(benzeneScaffold, mnt, true, fs, gaParams);
        
        assertEquals(3, graph.getRingCount());
        int numSiAtoms = 0;
        for (Vertex v : graph.getVertexList())
        {
            numSiAtoms = numSiAtoms + MoleculeUtils.countAtomsOfElement(
                    v.getIAtomContainer(), "Si");
        }
        assertEquals(12, numSiAtoms);
    }
    
//------------------------------------------------------------------------------
    
    /*
     * Here we test whether the method detects that extension should be done in 
     * all symmetric location, including APs belonging to the same vertex and
     * outside of it.
     */
    
    @Test
    public void testExtendGraph() throws Exception
    {   
        APClass APCA = APClass.make("A", 0);
        APClass APCB = APClass.make("B", 0);
        
        Fragment vC1 = new Fragment();
        Atom ac1 = new Atom("C", new Point3d());
        vC1.addAtom(ac1);
        vC1.addAP(0, APCB, new Point3d(1.1, 0.0, 0.0));
        vC1.addAP(0, APCB, new Point3d(1.1, 1.0, 0.0));
        vC1.addAP(0, APCB, new Point3d(1.1, 1.0, 2.0));
        vC1.addAP(0, APCB, new Point3d(1.1,-1.0, 2.0));

        Fragment vC3 = new Fragment();
        Atom ac31 = new Atom("C", new Point3d(0.0, 2.0, 0.0));
        Atom ac32 = new Atom("C", new Point3d(0.0, 0.0, 2.0));
        Atom ac33 = new Atom("C", new Point3d(0.0, 0.0, 0.0));
        vC3.addAtom(ac31);
        vC3.addAtom(ac32);
        vC3.addAtom(ac33);
        vC3.addBond(new Bond(ac31, ac32));
        vC3.addBond(new Bond(ac32, ac33));
        vC3.addBond(new Bond(ac33, ac31));
        vC3.addAP(0, APCA, new Point3d(0.0, 2.0, -1.0));
        vC3.addAP(1, APCA, new Point3d(-1.0,-1.0, 3.0));
        vC3.addAP(1, APCA, new Point3d(1.0, -1.0, 3.0));
        vC3.addAP(2, APCA, new Point3d(-1.0,-1.0, 1.0));
        vC3.addAP(2, APCA, new Point3d(1.0, -1.0, 1.0));
        
        Fragment vCl = new Fragment();
        Atom acl = new Atom("Cl", new Point3d());
        vCl.addAtom(acl);
        vCl.addAP(0, APCB, new Point3d(1.1, 0.0, 0.0));
        
        Fragment vN = new Fragment();
        Atom aN = new Atom("N", new Point3d());
        vN.addAtom(aN);
        vN.addAP(0, APCB, new Point3d(1.1, 0.0, 0.0));
        vN.addAP(0, APCA, new Point3d(1.1, 1.0, 0.0));
        vN.addAP(0, APCB, new Point3d(1.1, 1.0, 2.0));
        
        ArrayList<Vertex> fragments = new ArrayList<Vertex>();
        fragments.add(vC1);
        fragments.add(vC3);
        fragments.add(vCl);
        fragments.add(vN);
        
        // Use clones of the vertexes to simulate the construction from BBSpace
        Vertex cvC1a = vC1.clone();
        cvC1a.setVertexId(1);
        Vertex cvC1b = vC1.clone();
        cvC1b.setVertexId(2);
        Vertex cvC3a = vC3.clone();
        cvC3a.setVertexId(3);
        Vertex cvC3b = vC3.clone();
        cvC3b.setVertexId(4);
        Vertex cvN = vN.clone();
        cvN.setVertexId(0);
        
        /* This is the graph we work with ('*' is a free AP)
         *    
         *      *   *
         *     /   /
         *  *-C---C-*
         *     \ /
         *      C
         *     /           *
         *    /           /
         *   N(root)-----C-*
         *    \           \    
         *     \           *
         *      C---C-*
         *       \ / \
         *        C   *
         *       / \
         *    *-C   *
         *     / \
         *    *   *
         *    
         */
        
        DGraph graph = new DGraph();
        graph.addVertex(cvN);
        graph.appendVertexOnAP(cvN.getAP(0), cvC3a.getAP(0));
        graph.appendVertexOnAP(cvN.getAP(1), cvC1a.getAP(0));
        graph.appendVertexOnAP(cvN.getAP(2), cvC3b.getAP(0));
        graph.appendVertexOnAP(cvC3b.getAP(2), cvC1b.getAP(0));
        graph.addSymmetricSetOfVertices(new SymmetricVertexes(Arrays.asList(
                cvC3a, cvC3b)));
        
        //Logger logger = Logger.getLogger("DummyLogger");
        //Randomizer rng = new Randomizer();
        //DenoptimIO.writeGraphToSDF(new File("/tmp/graph.sdf"), graph, false, logger, rng);
        
        HashMap<APClass,ArrayList<APClass>> cpMap = 
                new HashMap<APClass,ArrayList<APClass>>();
        ArrayList<APClass> lstA = new ArrayList<APClass>();
        lstA.add(APCB);
        cpMap.put(APCA, lstA);
        ArrayList<APClass> lstB = new ArrayList<APClass>();
        lstA.add(APCA);
        cpMap.put(APCB, lstB);
        
        FragmentSpaceParameters fsParams = new FragmentSpaceParameters();
        FragmentSpace fs = new FragmentSpace(fsParams,
                new ArrayList<Vertex>(),
                fragments,
                new ArrayList<Vertex>(), 
                cpMap, 
                new HashMap<APClass,APClass>(),
                new HashSet<APClass>(),
                new HashMap<APClass,ArrayList<APClass>>());
        fs.setAPclassBasedApproach(true);
        
        GAParameters gaParams = new GAParameters();
        gaParams.setParameters(fsParams);

        assertEquals(5, graph.getVertexCount());

        GraphOperations.extendGraph(cvC3a, false, true, true, 2, 0, gaParams);
        
        assertEquals(12, graph.getVertexCount());
        assertEquals(2, graph.getSymmetricSetCount());
        assertEquals(2, graph.getSymVerticesForVertex(cvC3a).size());
        assertEquals(7, graph.getSymVerticesForVertex(
                graph.getVertexAtPosition(graph.getVertexCount()-1)).size());
    }
    
//------------------------------------------------------------------------------
    
}