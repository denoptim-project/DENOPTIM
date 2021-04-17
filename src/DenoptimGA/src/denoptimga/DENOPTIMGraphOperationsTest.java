package denoptimga;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.molecule.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

import javax.vecmath.Point3d;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static denoptim.molecule.DENOPTIMEdge.*;
import static denoptim.molecule.DENOPTIMVertex.*;
import static org.junit.jupiter.api.Assertions.*;

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
            DEFAULT_APCLASS = APClass.make("norule", 0);
        } catch (DENOPTIMException e) {
            e.printStackTrace();
        }
    }

//------------------------------------------------------------------------------

    @Test
    public void testExtractPattern_singleRingSystem() {
        try {
            DENOPTIMGraph g = getThreeCycle();

            List<DENOPTIMGraph> subgraphs = DENOPTIMGraphOperations
                    .extractPattern(g, GraphPattern.RING);

            assertEquals(1, subgraphs.size());
            DENOPTIMGraph actual = subgraphs.get(0);
            DENOPTIMGraph expected = g;

            assertEquals(expected.getVertexCount(), actual.getVertexCount());
            assertEquals(expected.getEdgeCount(), actual.getEdgeCount());
            assertEquals(1, actual.getRingCount());

            assertTrue(DENOPTIMGraph.compareGraphNodes(getScaffold(expected),
                    expected, getScaffold(actual), actual));
        } catch (Throwable e) {
            e.printStackTrace();
            fail("Unexpected exception thrown.");
        }
    }

//------------------------------------------------------------------------------

    @Test
    public void testExtractPattern_returnsEmptyListIfNoRings() {
        try {
            DENOPTIMGraph g = getThreeCycle();
            g.removeRing(g.getRings().get(0));
            List<DENOPTIMGraph> subgraphs =
                    DENOPTIMGraphOperations.extractPattern(g, GraphPattern.RING);

            assertEquals(0, subgraphs.size());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception thrown.");
        }
    }

//------------------------------------------------------------------------------

    @Test
    public void testExtractPattern_ringWithArm() {
        try {
            DENOPTIMGraph g = getThreeCycleWithArm();

            List<DENOPTIMGraph> subgraphs =
                    DENOPTIMGraphOperations.extractPattern(g,GraphPattern.RING);

            assertEquals(1, subgraphs.size());

            DENOPTIMGraph actual = subgraphs.get(0);

            assertEquals(g.getEdgeCount() - 1, actual.getEdgeCount());
            assertEquals(g.getVertexCount() - 1, actual.getVertexCount());
            assertEquals(1, actual.getRingCount());

            DENOPTIMGraph expected = getExpectedResult();
            assertTrue(DENOPTIMGraph.compareGraphNodes(getScaffold(expected),
                    expected, getScaffold(actual), actual));
        } catch (Throwable t) {
            t.printStackTrace();
            fail("Unexpected exception thrown.");
        }
    }

//------------------------------------------------------------------------------

    /**
     * Returns the expected result from calling extractPattern on single ring
     * with arm.
     */
    private DENOPTIMGraph getExpectedResult() throws Throwable {
        DENOPTIMGraph expected = getThreeCycleWithArm();
        expected.renumberGraphVertices();
        DENOPTIMVertex oldScaffold = getScaffold(expected);
        DENOPTIMAttachmentPoint toRemove = oldScaffold.getAP(0)
                .getEdgeUser().getTrgAP();
        expected.removeVertex(oldScaffold);
        DENOPTIMGraph.setScaffold(toRemove.getOwner());
        return expected;
    }

//------------------------------------------------------------------------------

    @Test
    public void testExtractPattern_twoSeparatedRings() {
        try {
            DENOPTIMGraph g = getSeparatedCycles();

            List<DENOPTIMGraph> subgraphs =
                    DENOPTIMGraphOperations.extractPattern(g,GraphPattern.RING);

            assertEquals(2, subgraphs.size());

            DENOPTIMGraph actualCycle3 = subgraphs.get(0);
            DENOPTIMGraph actualCycle4 = subgraphs.get(1);
            if (actualCycle3.getVertexCount() != 3) {
                actualCycle3 = actualCycle4;
                actualCycle4 = subgraphs.get(0);
            }

            DENOPTIMGraph expectCycle3 = getExpectedThreeCycle();
            DENOPTIMGraph expectCycle4 = getExpectedFourCycle();

            assertTrue(DENOPTIMGraph.compareGraphNodes(
                    getScaffold(expectCycle3), expectCycle3,
                    getScaffold(actualCycle3), actualCycle3));

            assertTrue(DENOPTIMGraph.compareGraphNodes(
                    getScaffold(expectCycle4), expectCycle4,
                    getScaffold(actualCycle4), actualCycle4));

        } catch (Throwable t) {
            t.printStackTrace();
            fail("Unexpected exception thrown.");
        }

    }

//------------------------------------------------------------------------------

    private DENOPTIMGraph getExpectedFourCycle() throws Throwable {
        DENOPTIMGraph cycle4 = getFourCycle();
        getScaffold(cycle4).addAP(-1, 1, 1);
        return cycle4;
    }

//------------------------------------------------------------------------------

    private DENOPTIMGraph getExpectedThreeCycle() throws Throwable {
        DENOPTIMGraph cycle3 = getThreeCycle();
        getScaffold(cycle3).addAP(-1, 1, 1);
        return cycle3;
    }

//------------------------------------------------------------------------------

    @Test
    public void testExtractPattern_fusedRings() {
        ExtractPatternCase testCase = getFusedRings();

        List<DENOPTIMGraph> subgraphs = DENOPTIMGraphOperations
                .extractPattern(testCase.g, GraphPattern.RING);

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
     *     1
     *     |   ↑
     * 0 - 2 - 5 →        ← 10 - |
     * .   |   .       ↑    .    |
     * .   3 - 6 - 7 - 8 - 11 - 12 →
     * .   |   .       |   .     ↓
     * . . 4 . .       9 . .
     */
    private ExtractPatternCase getFusedRings() {
        Function<String, DENOPTIMVertex> vertexSupplier = s -> {
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
            return buildFragment(s, apCount);
        };

        /* We label the vertices in order of top left to bottom right. */
        List<DENOPTIMVertex> vertices = Stream.of(
                "O",
                "Cl", "C", "N", "N",
                "C", "C",
                "O",
                "C", "O",
                "N", "C",
                "C"
        ).map(vertexSupplier).collect(Collectors.toList());

        /* Here we specify the connections between atoms. Entry i lists
        vertex i's neighbors. Entries are separated by a -1. Previously
        connected vertices are not connected twice. Chords are not connected. */
        List<Integer> edges = Arrays.asList(
                2, -1,
                2, -1,
                3, 5, -1,
                4, 6, -1,
                -1,
                -1,
                7, -1,
                8, -1,
                9, 11, -1,
                -1,
                12, -1,
                12, -1
        );

        DENOPTIMGraph g = buildGraph(vertices, edges);
        g.renumberGraphVertices();
        DENOPTIMGraph.setScaffold(vertices.get(0));
        addRings(vertices, g);
        Set<DENOPTIMGraph> expectedSubgraphs = getExpectedSubgraphs(vertices, g);
        return new ExtractPatternCase(g, 2, expectedSubgraphs);
    }

//------------------------------------------------------------------------------

    private DENOPTIMGraph buildGraph(List<DENOPTIMVertex> vertices,
                                     List<Integer> edges) {
        DENOPTIMGraph g = new DENOPTIMGraph();
        g.addVertex(vertices.get(0));

        HashMap<String, BondType> bondMap = new HashMap<>();
        bondMap.put(String.valueOf(DEFAULT_APCLASS), BondType.SINGLE);
        FragmentSpace.setBondOrderMap(bondMap);

        for (int j = 0, i = 0; j < edges.size(); j++) {
            int adj = edges.get(j);
            if (adj == -1) {
                i++;
            } else {
                DENOPTIMVertex srcVertex = vertices.get(i);
                DENOPTIMVertex trgVertex = vertices.get(adj);

                DENOPTIMAttachmentPoint srcAP = srcVertex
                        .getAttachmentPoints()
                        .stream()
                        .filter(ap -> ap.getEdgeUser() != null)
                        .findFirst()
                        .get();
                DENOPTIMAttachmentPoint trgAP = trgVertex
                        .getAttachmentPoints()
                        .stream()
                        .filter(ap -> ap.getEdgeUser() != null)
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
                Arrays.asList(0, 2, 3, 4),
                Arrays.asList(5, 2, 3, 4),
                Arrays.asList(10, 12, 11),
                Arrays.asList(9, 8, 11))
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

    private Set<DENOPTIMGraph> getExpectedSubgraphs(
            List<DENOPTIMVertex> vertices, DENOPTIMGraph g) {
        Set<DENOPTIMGraph> expectedSubgraphs = new HashSet<>();

        List<Set<Integer>> keepVertices = Stream.of(
                Stream.of(0, 2, 3, 4, 5),
                Stream.of(8, 9, 10, 11, 12))
                .map(indices -> indices.collect(Collectors.toSet()))
                .collect(Collectors.toList());

        for (Set<Integer> keepVertex : keepVertices) {
            DENOPTIMGraph expSubgraph = g.clone();
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
        return expectedSubgraphs;
    }

//------------------------------------------------------------------------------

    private DENOPTIMVertex buildFragment(String elementSymbol, int apCount) {
        try {
            IAtomContainer atomContainer = chemBuilder.newAtomContainer();
            IAtom oxygen = chemBuilder.newAtom();
            oxygen.setSymbol(elementSymbol);
            atomContainer.addAtom(oxygen);

            DENOPTIMFragment v = new DENOPTIMFragment(-1, atomContainer,
                    BBType.FRAGMENT);
            for (int i = 0; i < apCount; i++) {
                v.addAP(0, DEFAULT_APCLASS, getRandomVector(), 1);
            }
            return v;
        } catch (DENOPTIMException e) {
            e.printStackTrace();
        }
        return null;
    }

//------------------------------------------------------------------------------

    private Point3d getRandomVector() {
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
    private DENOPTIMGraph getThreeCycle() throws DENOPTIMException {
        DENOPTIMVertex v1 = new EmptyVertex(0);
        v1.setLevel(-1);
        DENOPTIMVertex rcv1 = new EmptyVertex(1, new ArrayList<>(),
                new ArrayList<>(), true);
        DENOPTIMVertex rcv2 = new EmptyVertex(2, new ArrayList<>(),
                new ArrayList<>(), true);

        List<DENOPTIMVertex> vertices = Arrays.asList(v1, rcv1, rcv2);
        for (DENOPTIMVertex v : vertices) {
            v.setBuildingBlockType(BBType.FRAGMENT);
            v.addAP(-1, 1, 1);
        }
        // Need an additional AP on v1
        v1.addAP(-1, 1, 1);

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

    /**
     * Graph which is a ring with an arm jutting out. The S marks the
     * scaffold vertex.
     * The graph looks like:
     *         S
     *         |
     *    /--- V ----\
     *   /            \
     * RCV -(chord)- RCV
     */
    private DENOPTIMGraph getThreeCycleWithArm() throws Throwable {
        DENOPTIMVertex arm = new EmptyVertex();
        arm.addAP(-1, 1, 1);
        arm.setLevel(-1);
        DENOPTIMGraph g = new DENOPTIMGraph();
        g.addVertex(arm);
        g.renumberGraphVertices();

        DENOPTIMGraph ring = getThreeCycle();
        DENOPTIMVertex ringScaffold = getScaffold(ring);
        ringScaffold.addAP(-1, 1, 1);

        g.appendGraphOnAP(arm, 0, ring, ringScaffold,
                ringScaffold.getNumberOfAPs() - 1,
                BondType.SINGLE, new HashMap<>());

        DENOPTIMGraph.setScaffold(arm);

        g.renumberGraphVertices();
        return g;
    }

//------------------------------------------------------------------------------

    /**
     * A connected 3- and 4-cycle connected, but no sharing any vertices.
     * S marks the scaffold vertex. The graph looks like:
     *  RCV ---|
     *   |     |
     *(chord)  S -- V ----------- V
     *   |     |    |             |
     *  RCV ---|   RCV -(chord)- RCV
     */
    private DENOPTIMGraph getSeparatedCycles() throws Throwable {
        DENOPTIMGraph cycle3 = getThreeCycle();
        DENOPTIMGraph cycle4 = getFourCycle();

        DENOPTIMVertex scaff = getScaffold(cycle3);
        scaff.addAP(-1, 1, 1);

        DENOPTIMVertex connectTo = getScaffold(cycle4);
        connectTo.addAP(-1, 1, 1);

        cycle3.appendGraphOnGraph(scaff, scaff.getNumberOfAPs() - 1, cycle4,
                connectTo, connectTo.getNumberOfAPs() - 1,
                BondType.SINGLE, new HashMap<>(), false);

        DENOPTIMGraph.setScaffold(scaff);

        return cycle3;
    }

//------------------------------------------------------------------------------

    /**
     * Returns a 4-cycle. S marks the scaffold vertex:
     *  S ----------- V
     *  |             |
     * RCV -(chord)- RCV
     */
    private DENOPTIMGraph getFourCycle() throws DENOPTIMException {
        DENOPTIMVertex scaff = new EmptyVertex();
        DENOPTIMVertex v = new EmptyVertex();
        DENOPTIMVertex[] nonRCVs = new DENOPTIMVertex[]{scaff, v};
        for (DENOPTIMVertex vertex : nonRCVs) {
            vertex.setBuildingBlockType(BBType.FRAGMENT);
            for (int i = 0; i < 2; i++) {
                vertex.addAP(-1, 1, 1);
            }
        }

        DENOPTIMVertex rcvS = new EmptyVertex(-1, new ArrayList<>(),
                new ArrayList<>(), true);
        DENOPTIMVertex rcvV = new EmptyVertex(-1, new ArrayList<>(),
                new ArrayList<>(), true);
        DENOPTIMVertex[] rcvs = new DENOPTIMVertex[]{rcvS, rcvV};
        for (DENOPTIMVertex vertex : rcvs) {
            vertex.setBuildingBlockType(BBType.FRAGMENT);
            vertex.addAP(-1, 1, 1);
        }

        DENOPTIMGraph g = new DENOPTIMGraph();
        g.addVertex(scaff);
        g.appendVertexOnAP(scaff.getAP(0), rcvS.getAP(0));
        g.appendVertexOnAP(scaff.getAP(1), v.getAP(1));
        g.appendVertexOnAP(v.getAP(0), rcvV.getAP(0));

        g.renumberGraphVertices();
        DENOPTIMGraph.setScaffold(scaff);

        DENOPTIMRing r = new DENOPTIMRing(new ArrayList(Arrays.asList(rcvS,
                scaff, v, rcvV)));
        g.addRing(r);

        return g;
    }

//------------------------------------------------------------------------------

    private DENOPTIMVertex getScaffold(DENOPTIMGraph g) throws Throwable {
        return g
                .getVertexList()
                .stream()
                .filter(v -> v.getLevel() == -1)
                .findFirst()
                .orElseThrow((Supplier<Throwable>) () ->
                        new IllegalArgumentException("No vertex at level -1"));
    }

//------------------------------------------------------------------------------

    private static final class ExtractPatternCase {
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

        private boolean matchesExpected(Collection<DENOPTIMGraph> actual) {
            if (actual.size() != expectedSize) {
                return false;
            }

            Set<DENOPTIMGraph> actualsRemoved = new HashSet<>(expectedGraphs);
            for (DENOPTIMGraph g : actual) {
                if (expectedGraphs.stream().anyMatch(exp ->
                        graphComparator.compare(g, exp) == 0)) {
                    actualsRemoved = actualsRemoved
                            .stream()
                            .filter(exp -> graphComparator.compare(g, exp) == 0)
                            .collect(Collectors.toSet());
                } else {
                    return false;
                }
            }

            // Check that no graphs are missing from actual
            return actualsRemoved.size() == 0;
        }
    }

//------------------------------------------------------------------------------

}