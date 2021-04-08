package denoptim.molecule;

import static denoptim.molecule.DENOPTIMVertex.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.*;

/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no>
 *   and Marco Foscato <marco.foscato@uib.no>
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

import denoptim.constants.DENOPTIMConstants;
import denoptim.fragspace.FragmentSpace;
import denoptim.utils.MutationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import denoptim.exception.DENOPTIMException;
import denoptim.molecule.DENOPTIMVertex.BBType;
import denoptim.utils.RandomUtils;
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

import javax.vecmath.Point3d;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for DENOPTIMTemplate
 * 
 */

public class DENOPTIMTemplateTest
{
    final long SEED = 13;
    Random rng = new Random(SEED);
    IChemObjectBuilder chemBuilder = DefaultChemObjectBuilder.getInstance();

    @BeforeEach
    public void setUp() {
        HashMap<String, DENOPTIMEdge.BondType> map = new HashMap<>();
        FragmentSpace.setBondOrderMap(map);
        FragmentSpace.setFragmentLibrary(new ArrayList<>());
        FragmentSpace.setCompatibilityMatrix(new HashMap<>());
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testClone() throws Exception
    {
        DENOPTIMVertex vA = new EmptyVertex(0);
        vA.addAP(0,1,1);
        vA.addAP(0,1,1);
        vA.addAP(0,2,1);
        DENOPTIMVertex vB = new EmptyVertex(1);
        vB.addAP(0,1,1);
        vB.addAP(0,1,1);
        DENOPTIMVertex vC = new EmptyVertex(2);
        vC.addAP(0,1,1);
        vC.addAP(0,1,1);
        vA.addAP(0,1,1);
        DENOPTIMVertex vRcvA = new EmptyVertex(3);
        vRcvA.addAP(0,1,1);
        vRcvA.setAsRCV(true);
        DENOPTIMVertex vRcvC = new EmptyVertex(4);
        vRcvC.addAP(0,1,1);
        vRcvC.setAsRCV(true);
        
        DENOPTIMGraph g = new DENOPTIMGraph();
        g.addVertex(vA);
        g.addVertex(vB);
        g.addVertex(vC);
        g.addVertex(vRcvA);
        g.addVertex(vRcvC);
        
        DENOPTIMEdge eAB = vA.connectVertices(vB, 0, 1);
        DENOPTIMEdge eBC = vB.connectVertices(vC, 0, 1);
        DENOPTIMEdge eARcvA = vA.connectVertices(vRcvA, 1, 0);
        DENOPTIMEdge eCRcvC = vC.connectVertices(vRcvC, 0, 0);
        g.addEdge(eAB);
        g.addEdge(eBC);
        g.addEdge(eARcvA);
        g.addEdge(eCRcvC);
        
        DENOPTIMRing r = new DENOPTIMRing(new ArrayList<DENOPTIMVertex>(
                Arrays.asList(vRcvA, vA, vB, vC, vRcvC)));
        g.addRing(r);
        
        DENOPTIMTemplate t = new DENOPTIMTemplate(BBType.NONE);
        //TODO-v3 add required APs and check they are cloned properly
        t.setInnerGraph(g);
        t.freezeTemplate();
        
        DENOPTIMTemplate c = t.clone();
        
        assertEquals(t.getFreeAPCount(),c.getFreeAPCount(),"Different #free APs");
        for (int i=0; i<t.getFreeAPCount(); i++)
        {
            DENOPTIMAttachmentPoint oriAP = t.getAP(i);
            DENOPTIMAttachmentPoint cloAP = c.getAP(i);
            assertTrue(oriAP.hashCode() != cloAP.hashCode(), "Hashcode of APs");
            assertTrue(oriAP.getAPClass() == cloAP.getAPClass(), "APClass");
            assertTrue(oriAP.getAtomPositionNumber() 
                    == cloAP.getAtomPositionNumber(), "AP AtomSource");
            assertTrue(oriAP.getTotalConnections() 
                    == cloAP.getTotalConnections(), "AP total connections");
        }
        
        DENOPTIMGraph oriIG = t.getInnerGraph();
        DENOPTIMGraph cloIG = c.getInnerGraph();
        assertTrue(oriIG.hashCode() != cloIG.hashCode(),"InnerGraph graph hash");
        assertEquals(oriIG.getVertexCount(),
                cloIG.getVertexCount(),"InnerGraph vertex count");
        for (int i=0; i<oriIG.getVertexCount(); i++)
        {
            DENOPTIMVertex ov = oriIG.getVertexAtPosition(i);
            DENOPTIMVertex cv = cloIG.getVertexAtPosition(i);
            assertTrue(ov.hashCode() != cv.hashCode(),"InnerGraph vertex hash");
        }
        
        assertEquals(oriIG.getRingCount(),
                cloIG.getRingCount(),"InnerGraph ring count");
        assertEquals(oriIG.getRings().get(0).getSize(),
                cloIG.getRings().get(0).getSize(),"InnerGraph ring count");
    }

//------------------------------------------------------------------------------

    @Test
    public void testNestedTemplateCloning() {
        try {
            DENOPTIMTemplate t = getNestedTemplate();
            DENOPTIMTemplate clone = t.clone();
            assertTrue(t.sameAs(clone, new StringBuilder()));
        } catch (DENOPTIMException e) {
            fail("Unexpected exception thrown.");

        }
    }

//------------------------------------------------------------------------------

    /**
     * Creating a template that contains another template with the following
     * structure:
     * |-----------------------|
     * |            |--------| |
     * | * - CH_2 - | * - OH | |
     * |            |--------| |
     * |-----------------------|
     * The box containing the 'OH' represents the nested template and the
     * outermost box represents the outermost template.
     */
    private DENOPTIMTemplate getNestedTemplate() throws DENOPTIMException {
        /* Constructing innermost template */
        DENOPTIMVertex ohFrag = getOHFragment();
        DENOPTIMGraph g = new DENOPTIMGraph();
        g.addVertex(ohFrag);
        DENOPTIMTemplate nestedTemp = new DENOPTIMTemplate(BBType.FRAGMENT);
        nestedTemp.setInnerGraph(g);

        /* Constructing outermost template */
        DENOPTIMVertex ch2Frag = getCH2Fragment();
        g = new DENOPTIMGraph();
        g.addVertex(ch2Frag);
        g.addVertex(nestedTemp);
        DENOPTIMEdge e = ch2Frag.connectVertices(nestedTemp);
        g.addEdge(e);
        DENOPTIMTemplate outerTemp = new DENOPTIMTemplate(BBType.FRAGMENT);
        outerTemp.setInnerGraph(g);

        return outerTemp;
    }

    private DENOPTIMVertex getCH2Fragment() throws DENOPTIMException {
        IAtomContainer atomContainer = chemBuilder.newAtomContainer();
        String[] elements = new String[]{"C", "H", "H"};
        for (String e : elements) {
            IAtom atom = chemBuilder.newAtom();
            atom.setSymbol(e);
            atomContainer.addAtom(atom);
        }
        atomContainer.addBond(0, 1, IBond.Order.SINGLE);
        atomContainer.addBond(0, 2, IBond.Order.SINGLE);

        DENOPTIMFragment v = new DENOPTIMFragment(2, atomContainer,
                BBType.FRAGMENT);
        double precision = 10*10*10*10;
        for (int i = 0; i < 2; i++) {

            APClass apClass = APClass.make("c", 0);

            FragmentSpace.getBondOrderMap().put(apClass.getRule(),
                    DENOPTIMEdge.BondType.SINGLE);

            v.addAP(
                    0,
                    apClass,
                    new Point3d(
                            (double) (Math.round(rng.nextDouble() * precision)) / precision,
                            (double) (Math.round(rng.nextDouble() * precision)) / precision,
                            (double) (Math.round(rng.nextDouble() * precision)) / precision),
                    1
            );
        }
        return v;
    }

    private DENOPTIMVertex getOHFragment() throws DENOPTIMException {
        IAtomContainer atomContainer = chemBuilder.newAtomContainer();
        String[] elements = new String[]{"O", "H"};
        for (String e : elements) {
            IAtom atom = chemBuilder.newAtom();
            atom.setSymbol(e);
            atomContainer.addAtom(atom);
        }

        atomContainer.addBond(0, 1, IBond.Order.SINGLE);

        DENOPTIMFragment v = new DENOPTIMFragment(1, atomContainer,
                BBType.FRAGMENT);
        double precision = 10*10*10*10;
        APClass apClass = APClass.make("o", 0);
        FragmentSpace.getBondOrderMap().put(apClass.getRule(),
                DENOPTIMEdge.BondType.SINGLE);
        v.addAP(
                0,
                apClass,
                new Point3d(
                        (double) (Math.round(rng.nextDouble() * precision)) / precision,
                        (double) (Math.round(rng.nextDouble() * precision)) / precision,
                        (double) (Math.round(rng.nextDouble() * precision)) / precision),
                1
        );
        return v;
    }

//------------------------------------------------------------------------------

    /**
     * This test tests nested template cloning, constructing Fragments and
     * Templates the way it "should" be done (personal opinion of Einar
     * Kjellback).
     * Until we decide to refactor the API we ignore this test. To test nested
     * template cloning in the meantime one can run this test's sister class,
     * the testNestedTemplateCloning-test, which can be found in this unit test
     * class.
     * In lines where the sibling tests differ I have commented what the
     * issues and bugs will appear using this test's API.
     */
    @Disabled("Disabled until we decide to refactor Fragment and Vertex")
    @Test
    public void testNestedTemplateCloningAPI() {
        try {
            DENOPTIMTemplate t = getNestedTemplateAPI();
            DENOPTIMTemplate clone = t.clone();
            assertTrue(t.sameAs(clone, new StringBuilder()));
        } catch (DENOPTIMException e) {
            fail("Unexpected exception thrown.");

        }
    }

//------------------------------------------------------------------------------

    /**
     * Creating a template that contains another template with the following
     * structure:
     * |-----------------------|
     * |            |--------| |
     * | * - CH_2 - | * - OH | |
     * |            |--------| |
     * |-----------------------|
     * The box containing the 'OH' represents the nested template and the
     * outermost box represents the outermost template.
     */
    private DENOPTIMTemplate getNestedTemplateAPI() throws DENOPTIMException {
        /* Constructing innermost template */
        DENOPTIMVertex ohFrag = getOHFragmentAPI();
        DENOPTIMGraph g = new DENOPTIMGraph();
        g.addVertex(ohFrag);
        DENOPTIMTemplate nestedTemp = new DENOPTIMTemplate(BBType.FRAGMENT);
        nestedTemp.setInnerGraph(g);

        /* Constructing outermost template */
        DENOPTIMVertex ch2Frag = getCH2FragmentAPI();
        g = new DENOPTIMGraph();
        g.addVertex(ch2Frag);
        g.addVertex(nestedTemp);
        DENOPTIMEdge e = ch2Frag.connectVertices(nestedTemp);
        g.addEdge(e);
        DENOPTIMTemplate outerTemp = new DENOPTIMTemplate(BBType.FRAGMENT);
        outerTemp.setInnerGraph(g);

        return outerTemp;
    }

//------------------------------------------------------------------------------

    private DENOPTIMVertex getCH2FragmentAPI() throws DENOPTIMException {
        /*
        --Issue 1--
        There are two ways of building a fragment:
            1. Construct the IAtomContainer outside the fragment and inject
               it as a dependency to the Fragment.
            2. Inject an empty IAtomContainer and then build it from within
               the Fragment using identical methods as you would find them on
               IAtomContainer.
        I propose we only allow the first pattern to be used. We should
        therefore remove all methods from Fragment which modify the
        atomContainer. */
        IAtomContainer atomContainer = chemBuilder.newAtomContainer();
        String[] elements = new String[]{"C", "H", "H"};
        for (String e : elements) {
            IAtom atom = chemBuilder.newAtom();
            atom.setSymbol(e);
            atomContainer.addAtom(atom);
        }
        atomContainer.addBond(0, 1, IBond.Order.SINGLE);
        atomContainer.addBond(0, 2, IBond.Order.SINGLE);

        /*
        --Issue 2--
        Fragment and Vertex have different addAP(…) methods with different
        arguments which have different semantics. We need to unify the
        semantics and syntax of these methods before v can have type
        Vertex.

        --Issue 4--
        Using tags on the atomContainer's IAtoms defeats the purpose of
        having a Fragment class. Why have Fragment when we can just store
        information about the Fragment in its IAtomContainer as tags? We
        should commit to storing information about a fragment either in the
        Fragment class field variables or as a number of tags on an
        IAtomContainer. */
        DENOPTIMVertex v = new DENOPTIMFragment(2, atomContainer,
                BBType.FRAGMENT);

        for (int i = 0; i < 2; i++) {
            int srcAtom = 0;
            int totConnections = 1;
            /*
            --Issue 5--
            Cloning an AP rounds the coordinates of the directions vector to
            the four most significant digits. This happens because cloning is
            done by converting the SDF string representation of the original
            AP to a clone. */
            Point3d dirVec = new Point3d(rng.nextDouble(), rng.nextDouble(),
                    rng.nextDouble());
            APClass apClass = APClass.make("c", 0);
            /*
            --Issue 6--
            In the sister test we put the APClass into FragmentSpace
            .bondOrderMap and map it to BondType.SINGLE. APs should not
            have to make references to the FragmentSpace to work.
             */

            /* Method does not exist. See notes above about different
            addAP() constructors on Vertex and Fragment */
//            v.addAP(srcAtom, totConnections, dirVec, apClass);
        }
        return v;
    }

//------------------------------------------------------------------------------

    /* See notes in getCH2FragmentAPI() for change suggestions */
    private DENOPTIMVertex getOHFragmentAPI() throws DENOPTIMException {
        IAtomContainer atomContainer = chemBuilder.newAtomContainer();
        String[] elements = new String[]{"O", "H"};
        for (String e : elements) {
            IAtom atom = chemBuilder.newAtom();
            atom.setSymbol(e);
            atomContainer.addAtom(atom);
        }

        atomContainer.addBond(0, 1, IBond.Order.SINGLE);

        DENOPTIMFragment v = new DENOPTIMFragment(1, atomContainer,
                BBType.FRAGMENT);
        int srcAtom = 0;
        int totConnections = 1;
        Point3d dirVec = new Point3d(rng.nextDouble(), rng.nextDouble(),
                rng.nextDouble());
        APClass apClass = APClass.make("o", 0);
//            v.addAP(srcAtom, totConnections, dirVec, apClass);
        return v;
    }

//------------------------------------------------------------------------------

    @Test
    public void testGetAttachmentPoints_returnsAPsWithTemplateAsOwner() {
        DENOPTIMTemplate template = new DENOPTIMTemplate(BBType.NONE);
        EmptyVertex v = new EmptyVertex();
        template.addAP(0, 1, 1);
        v.addAP(0, 1, 1);
        DENOPTIMGraph innerGraph = new DENOPTIMGraph();
        innerGraph.addVertex(v);
        template.setInnerGraph(innerGraph);

        int totalAPCount = 1;
        for (int i = 0; i < totalAPCount; i++) {
            DENOPTIMVertex actualOwner = template.getAttachmentPoints().get(i)
                    .getOwner();
            assertSame(template, actualOwner);
        }
    }

//------------------------------------------------------------------------------

    @Test
    public void testGetAttachmentPoints_returnsCorrectNumberOfAPs() {
        // Einar: Prevents nullpointer exception later
        RandomUtils.initialiseRNG(13);
        DENOPTIMTemplate template = 
                new DENOPTIMTemplate(BBType.NONE);
        int requiredAPCount = 2;
        int atmPos = 0;
        int atmConns = 1;
        int apConns = 1;
        EmptyVertex v1 = new EmptyVertex();
        EmptyVertex v2 = new EmptyVertex();
        int v1APCount = 3;
        int v2APCount = 2;
        for (int i = 0; i < requiredAPCount; i++) {
            template.addAP(atmPos, atmConns, apConns);
        }
        for (int i = 0; i < v1APCount; i++) {
            v1.addAP(atmPos, atmConns, apConns);
        }
        for (int i = 0; i < v2APCount; i++) {
            v2.addAP(atmPos, atmConns, apConns);
        }
        v1.connectVertices(v2);
        DENOPTIMGraph innerGraph = new DENOPTIMGraph();
        innerGraph.addVertex(v1);
        innerGraph.addVertex(v2);
        template.setInnerGraph(innerGraph);

//        System.err.println(innerGraph.getAvailableAPs().toString());

        // -2 since 2 APs are used to connect v1 and v2.
        int expectedAPCount = v1APCount + v2APCount - 2;
        int actualAPCount = template.getAttachmentPoints().size();
        assertEquals(expectedAPCount, actualAPCount);
    }

//------------------------------------------------------------------------------

    @Test
    public void testSetInnerGraph_throws_on_graph_incompatible_w_requiredAPs()
            throws DENOPTIMException {
        int numberOfAPs = 2;
        List<Integer> atomConnections = Arrays.asList(1, 2);
        List<Integer> ApConnections = Arrays.asList(1, 2);
        List<double[]> dirVecs = Arrays.asList(
                new double[]{1.0, -2.1,3.2},
                new double[]{-2.0, 1.1, -3.2}
        );
        List<APClass> APClasses = Arrays.asList(
                APClass.make("rule1", 0),
                APClass.make("rule2", 1)
        );

        DENOPTIMTemplate template = 
                new DENOPTIMTemplate(BBType.NONE);
        DENOPTIMVertex v = new EmptyVertex();
        for (int i = 0; i < numberOfAPs; i++) {
            template.addAP(-1, atomConnections.get(i),
                    ApConnections.get(i), dirVecs.get(i),
                    APClasses.get(i));
            v.addAP(-1, atomConnections.get(i),
                    ApConnections.get(i), dirVecs.get(i),
                    APClasses.get(i));
        }
        DENOPTIMGraph innerGraph = new DENOPTIMGraph();
        innerGraph.addVertex(v);

        testAtLeastSameNumberOfAPs(template, numberOfAPs);

        // These two shouldn't really be part of the comparison between APs.
//        testSameAtomConnections(template, innerGraph);
//        testSameApConnections(template, innerGraph);

//        testSameDirVec(template, innerGraph);
        testSameAPClass(template, innerGraph);
    }

//------------------------------------------------------------------------------

    private void testSameAPClass(DENOPTIMTemplate t, DENOPTIMGraph innerGraph) {
        DENOPTIMAttachmentPoint ap = innerGraph.getVertexAtPosition(0).getAP(1);
        try {
            ap.setAPClass(
                    innerGraph.getVertexAtPosition(0).getAP(0).getAPClass());
            assertThrows(IllegalArgumentException.class,
                    () -> t.setInnerGraph(innerGraph));
        } catch (Exception e) {
            fail("Expected " + IllegalArgumentException.class + ", but was " 
                    + e.getClass());
        }
    }

//------------------------------------------------------------------------------

    private void testSameDirVec(DENOPTIMTemplate t, DENOPTIMGraph innerGraph) {
        DENOPTIMAttachmentPoint ap = innerGraph.getVertexAtPosition(0).getAP(1);
        double[] correctDirVec = ap.getDirectionVector();
        correctDirVec[1] = -correctDirVec[1];
        assertThrows(IllegalArgumentException.class,
                () -> t.setInnerGraph(innerGraph));
    }

//------------------------------------------------------------------------------

    private void testSameApConnections(DENOPTIMTemplate t,
                                       DENOPTIMGraph innerGraph) {
        DENOPTIMAttachmentPoint ap = innerGraph.getVertexAtPosition(0).getAP(0);
        int correctApConnections = ap.getFreeConnections();
        ap.setFreeConnections(correctApConnections + 1);
        assertThrows(IllegalArgumentException.class,
                () -> t.setInnerGraph(innerGraph));
    }

//------------------------------------------------------------------------------

    private void testSameAtomConnections(DENOPTIMTemplate t,
                                         DENOPTIMGraph innerGraph) {
        DENOPTIMAttachmentPoint ap = innerGraph.getVertexAtPosition(0).getAP(0);
        int correctAtomConnections = ap.getTotalConnections();
        ap.setTotalConnections(correctAtomConnections + 1);
        assertThrows(IllegalArgumentException.class,
                () -> t.setInnerGraph(innerGraph));
    }

//------------------------------------------------------------------------------

    private void testAtLeastSameNumberOfAPs(DENOPTIMTemplate t,
                                            int expNumberOfAPs) {
        DENOPTIMVertex v = new EmptyVertex();
        for (int i = 0; i < expNumberOfAPs - 1; i++) {
            v.addAP();
        }
        DENOPTIMGraph innerGraph = new DENOPTIMGraph();
        innerGraph.addVertex(v);
        assertThrows(IllegalArgumentException.class,
                () -> t.setInnerGraph(innerGraph));
    }

//------------------------------------------------------------------------------

    @Disabled("Disabled until we can find a way to prevent addAP from being " +
            "called after setInnerGraph")
    @Test
    public void testAddAP_after_setInnerGraph_throwsException() {
        DENOPTIMTemplate t = new DENOPTIMTemplate(BBType.NONE);
        DENOPTIMGraph g = new DENOPTIMGraph();
        t.setInnerGraph(g);
        assertThrows(DENOPTIMException.class, () -> t.addAP(0, 1, 1));
    }

//------------------------------------------------------------------------------

    @Test
    public void testChangeBranch_noChangeIfNoSuitableFragmentsAvailable() {
        try {
            DENOPTIMTemplate t = getCH2Template();
            DENOPTIMGraph g = t.getInnerGraph();
            FragmentSpace.getFragmentLibrary().add(getOHFragment());
            boolean mutated = t.mutate(MutationType.CHANGEBRANCH);

            assertFalse(mutated);
            assertEquals(g, t.getInnerGraph());
        } catch (DENOPTIMException e) {
            fail("Unexpected exception thrown.");

        }
    }

//------------------------------------------------------------------------------

    /**
     * Returns a Template with a single fragment. The template has one required
     * AP.
     */
    private DENOPTIMTemplate getCH2Template() throws DENOPTIMException {
        DENOPTIMGraph g = new DENOPTIMGraph();
        DENOPTIMVertex ch2Frag = getCH2Fragment();
        g.addVertex(ch2Frag);

        DENOPTIMTemplate t = new DENOPTIMTemplate(BBType.FRAGMENT);
        t.addAP(ch2Frag.getAP(0));
        t.setInnerGraph(g);
        return t;
    }

//------------------------------------------------------------------------------

    @Test
    public void testChangeBranch_changeIfSuitableFragmentsAvailable() {
        try {
            DENOPTIMVertex ch3 = getCH3Fragment();
            FragmentSpace.getFragmentLibrary().add(ch3);
            DENOPTIMTemplate t = getCH2Template();

            DENOPTIMGraph graphBeforeMutation = t.getInnerGraph();

            DENOPTIMGraph expected = new DENOPTIMGraph();
            expected.addVertex(ch3);

            boolean mutated = t.mutate(MutationType.CHANGEBRANCH);

            DENOPTIMGraph actual = t.getInnerGraph();

            assertTrue(mutated);
            assertFalse(graphBeforeMutation.sameAs(actual,
                    new StringBuilder()));
            assertTrue(expected.sameAs(actual, new StringBuilder()));
        } catch (Exception e) {
            fail("Unexpected exception thrown.");
        }
    }

//------------------------------------------------------------------------------

    private DENOPTIMVertex getCH3Fragment() throws DENOPTIMException {
        IAtomContainer atomContainer = chemBuilder.newAtomContainer();
        String[] elements = new String[]{"C", "H", "H", "H"};
        for (String e : elements) {
            IAtom atom = chemBuilder.newAtom();
            atom.setSymbol(e);
            atomContainer.addAtom(atom);
        }
        atomContainer.addBond(0, 1, IBond.Order.SINGLE);
        atomContainer.addBond(0, 2, IBond.Order.SINGLE);
        atomContainer.addBond(0, 3, IBond.Order.SINGLE);

        DENOPTIMFragment v = new DENOPTIMFragment(3, atomContainer,
                BBType.FRAGMENT);

        APClass apClass = APClass.make("c", 0);
        FragmentSpace.getBondOrderMap().put(apClass.getRule(),
                DENOPTIMEdge.BondType.SINGLE);
        double precision = 10*10*10*10;

        v.addAP(
                0,
                apClass,
                new Point3d(
                        (double) (Math.round(rng.nextDouble() * precision)) / precision,
                        (double) (Math.round(rng.nextDouble() * precision)) / precision,
                        (double) (Math.round(rng.nextDouble() * precision)) / precision),
                1
        );

        return v;
    }

//------------------------------------------------------------------------------

    @Test
    public void testDelete_noChangeIfOnlyOneFragmentInInnerGraph() {
        try {
            DENOPTIMTemplate t = getCH2Template();
            DENOPTIMGraph expected = t.getInnerGraph();

            boolean changed = t.mutate(MutationType.DELETE);

            assertFalse(changed);
            assertTrue(expected.sameAs(t.getInnerGraph(), new StringBuilder()));
        } catch (Exception e) {
            fail("Unexpected exception thrown.");

        }

    }

//------------------------------------------------------------------------------

    @Test
    public void testExtend_noChangeIfNoCompatibleFragmentsAvailable() {
        try {
            FragmentSpace.getFragmentLibrary().add(getOHFragment());
            DENOPTIMTemplate t = getCH2Template();
            DENOPTIMGraph expected = t.getInnerGraph();

            boolean changed = t.mutate(MutationType.EXTEND);

            assertFalse(changed);
            assertTrue(expected.sameAs(t.getInnerGraph(), new StringBuilder()));
        } catch (Exception e) {
            fail("Unexpected exception thrown.");
        }
    }

//------------------------------------------------------------------------------

    @Disabled("Disabled until implemented")
    @Test
    public void testExtend_changeIfCompatibleFragmentAvailable() {
        try {
            DENOPTIMVertex oh = getOHFragment();
            DENOPTIMVertex ch3 = getCH3Fragment();
            HashMap<APClass, ArrayList<APClass>> compMatrix = new HashMap<>();

            APClass ohClass = oh.getAP(0).getAPClass();
            APClass ch2Class = ch3.getAP(0).getAPClass();

            compMatrix.put(ohClass,
                    new ArrayList<>(Collections.singleton(ch2Class)));
            compMatrix.put(ch2Class,
                    new ArrayList<>(Collections.singleton(ohClass)));

            FragmentSpace.setCompatibilityMatrix(compMatrix);
            FragmentSpace.getFragmentLibrary().add(oh);

            DENOPTIMGraph graphBeforeMutation = new DENOPTIMGraph();
            graphBeforeMutation.addVertex(ch3);

            DENOPTIMTemplate t = new DENOPTIMTemplate(BBType.FRAGMENT);
            t.setInnerGraph(graphBeforeMutation);

            boolean changed = t.mutate(MutationType.EXTEND);

            DENOPTIMGraph expected = new DENOPTIMGraph();
            expected.addVertex(ch3);
            expected.appendVertexOnAP(ch3.getAP(0), oh.getAP(0));

            DENOPTIMGraph actual = t.getInnerGraph();

            assertTrue(changed);
            assertFalse(graphBeforeMutation.sameAs(actual, new StringBuilder()));
            assertTrue(expected.sameAs(actual, new StringBuilder()));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception thrown.");
        }
    }

//------------------------------------------------------------------------------

    @Disabled("Disabled until implemented")
    @Test
    public void testMutate_mutatedTemplateStillCompatibleWithParentGraph() {}
}
