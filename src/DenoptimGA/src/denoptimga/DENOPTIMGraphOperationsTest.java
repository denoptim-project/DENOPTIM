package denoptimga;

import com.google.common.base.Suppliers;
import denoptim.exception.DENOPTIMException;
import denoptim.molecule.*;
import org.junit.jupiter.api.Test;
import uk.ac.ebi.beam.Graph;

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
            DENOPTIMGraph g = getSingleRingGraph();

            List<DENOPTIMGraph> subgraphs = DENOPTIMGraphOperations
                    .extractPattern(g, GraphPattern.RING);

            assertEquals(1, subgraphs.size());
            DENOPTIMGraph actual = subgraphs.get(0);
            DENOPTIMGraph expected = g;

            assertEquals(expected.getVertexCount(), actual.getVertexCount());
            assertEquals(expected.getEdgeCount(), actual.getEdgeCount());

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
        DENOPTIMGraph g = getSingleRingGraph();
        g.removeRing(g.getRings().get(0));
        List<DENOPTIMGraph> subgraphs =
                DENOPTIMGraphOperations.extractPattern(g, GraphPattern.RING);

        assertEquals(0, subgraphs.size());
    }

//------------------------------------------------------------------------------

    /**
     * Graph where ring does not contain the scaffold vertex of the original
     * graph. Graph looks like
     *         V
     *         |
     *    /--- V ----\
     *   /            \
     * RCV -(chord)- RCV
     */
    @Test
    public void testExtractPattern_ringOnScaffold() {
        try {
            DENOPTIMVertex scaffold = new EmptyVertex();
            scaffold.addAP(-1, 1, 1);
            scaffold.setLevel(-1);
            DENOPTIMGraph g = new DENOPTIMGraph();
            g.addVertex(scaffold);

            DENOPTIMGraph ring = getSingleRingGraph();
            DENOPTIMVertex ringScaffold = getScaffold(ring);
            ringScaffold.addAP(-1, 1, 1);

            g.appendGraphOnGraph(scaffold, 0, ring, ringScaffold,
                    ringScaffold.getNumberOfAPs() - 1,
                    DENOPTIMEdge.BondType.SINGLE, new HashMap<>(), false);

            g.renumberGraphVertices();

            List<DENOPTIMGraph> subgraphs =
                    DENOPTIMGraphOperations.extractPattern(g,GraphPattern.RING);

            assertEquals(1, subgraphs.size());

            DENOPTIMGraph actual = subgraphs.get(0);

            assertEquals(g.getEdgeCount() - 1, actual.getEdgeCount());
            assertEquals(g.getVertexCount() - 1, actual.getVertexCount());
            assertTrue(DENOPTIMGraph.compareGraphNodes(ringScaffold, g,
                    getScaffold(actual), actual));

        } catch (Throwable t) {
            t.printStackTrace();
            fail("Unexpected exception thrown.");
        }
    }

//------------------------------------------------------------------------------

    /**
     * Returns a graph that looks like this:
     *    /--- V ---\
     *   /           \
     * RCV -(chord)- RCV
     *
     * @return the graph above
     */
    private DENOPTIMGraph getSingleRingGraph() {
        try {
            DENOPTIMVertex v1 = new EmptyVertex(0);
            v1.setLevel(-1);
            DENOPTIMVertex rcv1 = new EmptyVertex(1, new ArrayList<>(),
                    new ArrayList<>(), true);
            DENOPTIMVertex rcv2 = new EmptyVertex(2, new ArrayList<>(),
                    new ArrayList<>(), true);

            APClass apClass = APClass.make("rule", 0);

            List<DENOPTIMVertex> vertices = Arrays.asList(v1, rcv1, rcv2);
            for (DENOPTIMVertex v : vertices) {
                v.setBuildingBlockType(BBType.FRAGMENT);
                v.addAP(-1, 1, 1, apClass);
            }
            // Need an additional AP on v1
            v1.addAP(-1, 1, 1, apClass);

            DENOPTIMGraph g = new DENOPTIMGraph();
            g.addVertex(v1);
            g.appendVertexOnAP(v1.getAP(0), rcv1.getAP(0));
            g.appendVertexOnAP(v1.getAP(1), rcv2.getAP(0));

            DENOPTIMRing r = new DENOPTIMRing(new ArrayList<>(
                    Arrays.asList(rcv1, v1, rcv2)));
            g.addRing(r);

            g.renumberGraphVertices();
            return g;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

//------------------------------------------------------------------------------

    private DENOPTIMVertex getScaffold(DENOPTIMGraph g) throws Throwable {
        return g
                .getVertexList()
                .stream()
                .filter(v -> v.getLevel() == -1)
                .findFirst()
                .orElseThrow((Supplier<Throwable>) () ->
                        new IllegalArgumentException("No scaffold at level -1"));
    }
}