package denoptimga;

import denoptim.exception.DENOPTIMException;
import denoptim.molecule.*;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Supplier;

import static denoptim.molecule.DENOPTIMVertex.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test
 * 
 * @author Marco Foscato
 */

public class DENOPTIMGraphOperationsTest {

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
                DENOPTIMEdge.BondType.SINGLE, new HashMap<>());

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
                DENOPTIMEdge.BondType.SINGLE, new HashMap<>(), false);

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
}