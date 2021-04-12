package denoptimga;

import com.google.common.base.Suppliers;
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

public class DENOPTIMGraphOperationsTest
{

//------------------------------------------------------------------------------

    /**
     * Simplest test case with the following graph:
     *    /--- v ---\
     *   /           \
     * RCV -(chord)- RCV
     */
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
                    expected, getScaffold(actual), actual, new StringBuilder()));
        } catch (Throwable e) {
            e.printStackTrace();
            fail("Unexpected exception thrown.");
        }
    }

    private DENOPTIMGraph getSingleRingGraph() throws DENOPTIMException {
        DENOPTIMVertex v1 = new EmptyVertex(0);
        v1.setBuildingBlockType(BBType.SCAFFOLD);
        v1.setLevel(-1);
        DENOPTIMVertex rcv1 = new EmptyVertex(1, new ArrayList<>(),
                new ArrayList<>(), true);
        rcv1.setBuildingBlockType(BBType.FRAGMENT);
        DENOPTIMVertex rcv2 = new EmptyVertex(2, new ArrayList<>(),
                new ArrayList<>(), true);
        rcv2.setBuildingBlockType(BBType.FRAGMENT);

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

//------------------------------------------------------------------------------

    /**
     * Graph where ring does not contain the scaffold vertex of the original
     * graph. Graph looks like
     *     (scaffold)
     *         |
     *    /--- v ----\
     *   /            \
     * RCV -(chord)- RCV
     */
    @Test
    public void testExtractPattern_ringOnScaffold() {
        
    }
    
}
