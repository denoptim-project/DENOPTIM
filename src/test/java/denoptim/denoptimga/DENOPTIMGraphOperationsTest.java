package denoptimga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.vecmath.Point3d;

import org.jgrapht.alg.util.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.graph.APClass;
import denoptim.graph.DENOPTIMAttachmentPoint;
import denoptim.graph.DENOPTIMEdge.BondType;
import denoptim.graph.DENOPTIMFragment;
import denoptim.graph.DENOPTIMGraph;
import denoptim.graph.DENOPTIMRing;
import denoptim.graph.DENOPTIMVertex;
import denoptim.graph.DENOPTIMVertex.BBType;
import denoptim.graph.EmptyVertex;
import denoptim.graph.GraphPattern;
import denoptim.utils.GraphUtils;

/**
 * Unit test
 * 
 * @author Marco Foscato
 */

public class DENOPTIMGraphOperationsTest {

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
        DENOPTIMGraph g = getThreeCycle();

        List<DENOPTIMGraph> subgraphs = g.extractPattern(GraphPattern.RING);

        assertEquals(1, subgraphs.size());
        DENOPTIMGraph actual = subgraphs.get(0);
        DENOPTIMGraph expected = g;

        assertEquals(expected.getVertexCount(), actual.getVertexCount());
        assertEquals(expected.getEdgeCount(), actual.getEdgeCount());
        assertEquals(1, actual.getRingCount());

        assertTrue(DENOPTIMGraph.compareGraphNodes(expected.getSourceVertex(),
                expected, actual.getSourceVertex(), actual));
    }

//------------------------------------------------------------------------------

    @Test
    public void testExtractPattern_returnsEmptyListIfNoRings() 
            throws Throwable
    {
        DENOPTIMGraph g = getThreeCycle();
        g.removeRing(g.getRings().get(0));
        List<DENOPTIMGraph> subgraphs = g.extractPattern(GraphPattern.RING);

        assertEquals(0, subgraphs.size());
    }

//------------------------------------------------------------------------------

    @Test
    public void testExtractPattern_fusedRings() throws Throwable 
    {
        ExtractPatternCase testCase = getFusedRings();

        List<DENOPTIMGraph> subgraphs = testCase.g.extractPattern(GraphPattern.RING);

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
        BiFunction<String, Boolean, DENOPTIMVertex> vertexSupplier =
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
        List<DENOPTIMVertex> vertices = Stream.of(
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

        DENOPTIMGraph g = null;
        try
        {
            g = buildGraph(vertices, edges);
        } catch (DENOPTIMException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        g.renumberGraphVertices();
        DENOPTIMGraph.setScaffold(vertices.get(0));
        addRings(vertices, g);
        Set<DENOPTIMGraph> expectedSubgraphs = getExpectedSubgraphs(g);
        return new ExtractPatternCase(g, 2, expectedSubgraphs);
    }

//------------------------------------------------------------------------------

    private DENOPTIMGraph buildGraph(List<DENOPTIMVertex> vertices,
                                     List<List<Integer>> edges) 
                                             throws DENOPTIMException {
        DENOPTIMGraph g = new DENOPTIMGraph();
        g.addVertex(vertices.get(0));
        for (int i = 0; i < edges.size(); i++) {
            DENOPTIMVertex srcVertex = vertices.get(i);
            for (Integer adj : edges.get(i)) {
                DENOPTIMVertex trgVertex = vertices.get(adj);

                DENOPTIMAttachmentPoint srcAP = srcVertex
                        .getAttachmentPoints()
                        .stream()
                        .filter(ap -> ap.getEdgeUser() == null)
                        .findFirst()
                        .get();
                DENOPTIMAttachmentPoint trgAP = trgVertex
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

    private void addRings(List<DENOPTIMVertex> vertices, DENOPTIMGraph g) {
        List<List<DENOPTIMVertex>> ringVertices = Stream.of(
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

        for (List<DENOPTIMVertex> vs : ringVertices) {
            DENOPTIMRing r = new DENOPTIMRing();
            for (DENOPTIMVertex v : vs) {
                r.addVertex(v);
            }
            g.addRing(r);
        }
    }

//------------------------------------------------------------------------------

    private Set<DENOPTIMGraph> getExpectedSubgraphs(DENOPTIMGraph graph) {
        List<Set<Integer>> keepVertices = Stream.of(
                Stream.of(0, 1, 3, 4, 5, 6),
                Stream.of(8, 9, 10, 11, 12))
                .map(indices -> indices.collect(Collectors.toSet()))
                .collect(Collectors.toList());

        List<DENOPTIMGraph> expectedSubgraphs = new ArrayList<>(2);
        for (Set<Integer> keepVertex : keepVertices) {
            DENOPTIMGraph expSubgraph = graph.clone();
            List<DENOPTIMVertex> vertices = expSubgraph.getVertexList();
            Set<DENOPTIMVertex> removeVertices = IntStream
                    .range(0, vertices.size())
                    .filter(i -> !keepVertex.contains(i))
                    .mapToObj(vertices::get)
                    .collect(Collectors.toSet());

            for (DENOPTIMVertex removeVertex : removeVertices) {
                expSubgraph.removeVertex(removeVertex);
            }
            expectedSubgraphs.add(expSubgraph);
        }

        return new HashSet<>(expectedSubgraphs);
    }

//------------------------------------------------------------------------------

    private DENOPTIMVertex buildFragment(String elementSymbol, int apCount,
            boolean isRCV)
    {
        try
        {
            IAtomContainer atomContainer = chemBuilder.newAtomContainer();
            IAtom oxygen = chemBuilder.newAtom();
            oxygen.setSymbol(elementSymbol);
            atomContainer.addAtom(oxygen);
    
            DENOPTIMFragment v = new DENOPTIMFragment(
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
    private DENOPTIMGraph getThreeCycle() throws DENOPTIMException 
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

        DENOPTIMGraph g = new DENOPTIMGraph();
        g.addVertex(v1);
        g.appendVertexOnAP(v1.getAP(0), rcv1.getAP(0));
        g.appendVertexOnAP(v1.getAP(1), rcv2.getAP(0));

        DENOPTIMRing r = new DENOPTIMRing(new ArrayList<>(
                Arrays.asList(rcv1, v1, rcv2)));
        g.addRing(r);

        g.renumberGraphVertices();
        return g;
    }

//------------------------------------------------------------------------------

    private static final class ExtractPatternCase 
    {
        final DENOPTIMGraph g;
        final int expectedSize;
        final Set<DENOPTIMGraph> expectedGraphs;
        final Comparator<DENOPTIMGraph> graphComparator = (gA, gB) ->
                gA.sameAs(gB, new StringBuilder()) ? 0 : -1;

        private ExtractPatternCase(DENOPTIMGraph g, int expectedSize,
                                         Set<DENOPTIMGraph> expectedGraphs) {
            this.g = g;
            this.expectedSize = expectedSize;
            this.expectedGraphs = expectedGraphs;
        }

        private boolean matchesExpected(Collection<DENOPTIMGraph> actuals) {
            if (actuals.size() != expectedSize) {
                return false;
            }
                
            Set<DENOPTIMGraph> unmatchedGraphs = new HashSet<>(expectedGraphs);
            for (DENOPTIMGraph g : actuals) 
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
}