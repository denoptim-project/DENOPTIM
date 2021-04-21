package denoptimga;

import denoptim.exception.DENOPTIMException;
import denoptim.fitness.FitnessParameters;
import denoptim.fragspace.FragmentSpace;
import denoptim.molecule.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

import javax.vecmath.Point3d;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static denoptim.molecule.DENOPTIMVertex.*;
import static org.junit.jupiter.api.Assertions.*;

public class OffspringEvaluationTaskTest {

    private static APClass APCLASS;

    private final Random rng = new Random();

    @BeforeAll
    static void SetUpClass() {
        try {
            APCLASS = APClass.make("norule:0");
        } catch (DENOPTIMException e) {
            e.printStackTrace();
        }
    }

//------------------------------------------------------------------------------

    /**
     * Check that the following graph's fused ring gets added to the fragment
     * space. Dots are chords
     *   ↑         ↑
     * ← C1 - C2 - C3 →
     *   .  / |    ↓
     *   C4 . C5 →
     *   ↓    ↓
     */
    @Test
    public void testFusedRingAddedToFragmentLibrary() {
        try {
            TestCase testCase = getTestCase();
            OffspringEvaluationTask task =
                    getOffspringEvaluationTask(testCase.graph);

            ArrayList<DENOPTIMVertex> fragLib = FragmentSpace.getFragmentLibrary();
            assertEquals(0, fragLib.size());

            task.call();

            assertEquals(1, fragLib.size());
            DENOPTIMVertex actual = fragLib.get(0);
            assertTrue(testCase.expected.sameAs(actual, new StringBuilder()));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception thrown.");
        }
    }

//------------------------------------------------------------------------------

    private OffspringEvaluationTask getOffspringEvaluationTask(DENOPTIMGraph g) throws DENOPTIMException {
        g.setGraphId(-1);
        g.setLocalMsg(null);

        FitnessParameters.interpretKeyword("FP-NO3DTREEMODEL");
        FitnessParameters.interpretKeyword("FP-EQUATION=${0.0}");

        IAtomContainer dummyContainer = DefaultChemObjectBuilder.getInstance()
                .newAtomContainer();

        return new OffspringEvaluationTask( "molName", g, "InChI", "SMILES",
                dummyContainer, "wrkDir", null, 1, "fileUID");
    }

//------------------------------------------------------------------------------

    private TestCase getTestCase() throws DENOPTIMException {
        List<DENOPTIMVertex> carbons = IntStream
                .range(0, 4)
                .mapToObj(i -> getCarbonVertex())
                .collect(Collectors.toList());

        DENOPTIMGraph g = new DENOPTIMGraph();
        DENOPTIMVertex c1 = carbons.get(0), c2 = carbons.get(1),
                c4 = carbons.get(2), c5 = carbons.get(3);
        DENOPTIMVertex c3 = getCarbonVertex();

        g.addVertex(c1);
        g.appendVertexOnAP(c1.getAP(0), c2.getAP(0));
        g.appendVertexOnAP(c2.getAP(1), c3.getAP(0));
        g.appendVertexOnAP(c2.getAP(2), c5.getAP(0));
        g.appendVertexOnAP(c2.getAP(3), c4.getAP(0));

        DENOPTIMRing r124 = new DENOPTIMRing(Arrays.asList(c1, c2, c4));
        DENOPTIMRing r425 = new DENOPTIMRing(Arrays.asList(c4, c2, c5));
        g.addRing(r124);
        g.addRing(r425);
        
        g.renumberGraphVertices();

        DENOPTIMTemplate t = getExpectedTemplate(g, c3);

        return new TestCase(g, t);
    }

//------------------------------------------------------------------------------

    private DENOPTIMTemplate getExpectedTemplate(DENOPTIMGraph g,
                                                 DENOPTIMVertex c3) {
        DENOPTIMGraph innerGraph = g.clone();
        innerGraph.renumberGraphVertices();
        DENOPTIMVertex c3Inner = innerGraph.getVertexAtPosition(g
                .getIndexOfVertex(c3.getVertexId()));
        innerGraph.removeVertex(c3Inner);

        DENOPTIMTemplate t = new DENOPTIMTemplate(BBType.FRAGMENT);
        t.setInnerGraph(innerGraph);
        return t;
    }

//------------------------------------------------------------------------------

    private DENOPTIMFragment getCarbonVertex() {
        try {
            IChemObjectBuilder builder = DefaultChemObjectBuilder.getInstance();
            IAtom carbon = builder.newAtom();
            carbon.setSymbol("C");
            IAtomContainer mol = builder.newAtomContainer();
            mol.addAtom(carbon);
            DENOPTIMFragment v = new DENOPTIMFragment(-1, mol,
                    BBType.FRAGMENT, true);
            for (int i = 0; i < 4; i++) {
                v.addAP(0, APCLASS, getRandomVector(), 1);
            }
            FragmentSpace.getBondOrderMap().put(APCLASS.getRule(),
                    DENOPTIMEdge.BondType.SINGLE);
            return v;
        } catch (DENOPTIMException e) {
            e.printStackTrace();
            fail("Unexpected exception thrown.");
        }
        return null;
    }

//------------------------------------------------------------------------------

    private Point3d getRandomVector() {
        int precision = 10 * 10 * 10 * 10;

        Supplier<Double> randomCoord = () ->
                (double) (Math.round(rng.nextDouble() * (double) precision)) /
                        ((double) precision);

        return new Point3d(randomCoord.get(), randomCoord.get(),
                randomCoord.get());
    }

//------------------------------------------------------------------------------

    private static final class TestCase {
        final DENOPTIMGraph graph;
        final DENOPTIMTemplate expected;

        TestCase(DENOPTIMGraph g, DENOPTIMTemplate expected) {
            this.graph = g;
            this.expected = expected;
        }
    }
}
