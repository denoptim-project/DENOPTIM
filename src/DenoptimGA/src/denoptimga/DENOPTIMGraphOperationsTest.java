package denoptimga;

import denoptim.molecule.*;
import org.junit.jupiter.api.Test;

import java.util.*;

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

            List<DENOPTIMGraph> subgraphs = DENOPTIMGraphOperations
                    .extractPattern(g, GraphPattern.RING);

            assertEquals(1, subgraphs.size());
            DENOPTIMGraph actual = subgraphs.get(0);
            DENOPTIMGraph expected = g;
            DENOPTIMVertex scaffold = null;
            for (DENOPTIMVertex v : actual.getVertexList()) {
                // Only the scaffold isn't an RCV in this test
                if (!v.isRCV()) {
                    scaffold = v;
                    break;
                }
            }

            assertEquals(expected.getVertexCount(), actual.getVertexCount());
            assertEquals(expected.getEdgeCount(), actual.getEdgeCount());

            Map<DENOPTIMVertex, DENOPTIMVertex> map = new HashMap<>();
            map.put(v1, scaffold);
            assertTrue(DENOPTIMGraph.compareGraphNodes(v1, expected,
                    scaffold, actual, map, new StringBuilder()));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception thrown.");
        }
    }
    
//------------------------------------------------------------------------------
 
    
//------------------------------------------------------------------------------
    
}
