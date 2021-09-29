package denoptim.molecule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
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

import denoptim.fragspace.FragmentSpace;
import denoptim.io.DenoptimIO;
import denoptim.utils.DENOPTIMMoleculeUtils;
import denoptim.utils.GenUtils;
import denoptim.utils.MutationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import denoptim.exception.DENOPTIMException;
import denoptim.molecule.DENOPTIMVertex.BBType;
import denoptim.utils.RandomUtils;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.geometry.alignment.KabschAlignment;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

import javax.vecmath.Point3d;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for DENOPTIMTemplate
 */

public class DENOPTIMTemplateTest
{
    final long SEED = 13;
    Random rng = new Random(SEED);
    IChemObjectBuilder chemBuilder = DefaultChemObjectBuilder.getInstance();

    @TempDir
    File tempDir;
    
    private final String NL = System.getProperty("line.separator");
    private final String SEP = System.getProperty("file.separator");
    
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
        g.appendVertexOnAP(vA.getAP(0), vB.getAP(1));
        g.appendVertexOnAP(vB.getAP(0), vC.getAP(1));
        g.appendVertexOnAP(vA.getAP(1), vRcvA.getAP(0));
        g.appendVertexOnAP(vC.getAP(0), vRcvC.getAP(0));
        
        DENOPTIMRing r = new DENOPTIMRing(new ArrayList<DENOPTIMVertex>(
                Arrays.asList(vRcvA, vA, vB, vC, vRcvC)));
        g.addRing(r);
        
        DENOPTIMTemplate t = new DENOPTIMTemplate(BBType.NONE);
        //TODO-v3 add required APs and check they are cloned properly
        t.setInnerGraph(g);
        t.freezeTemplate();
        
        DENOPTIMTemplate c = t.clone();
        
        assertEquals(t.getFreeAPCount(),c.getFreeAPCount(),
                "Different #free APs");
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
        assertTrue(oriIG.hashCode() != cloIG.hashCode(),
                "InnerGraph graph hash");
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
        g.appendVertexOnAP(ch2Frag.getAP(0), nestedTemp.getAP(0));
        
        DENOPTIMTemplate outerTemp = new DENOPTIMTemplate(BBType.FRAGMENT);
        outerTemp.setInnerGraph(g);

        return outerTemp;
    }
    
//------------------------------------------------------------------------------

    /**
     * @return 0D building block (no 3D coords!)
     * @throws DENOPTIMException
     */
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

    /**
     * The coordinates hard coded in this method must not be changed because
     * they are needed to reproduce results in {@link #testGetIAtomContainer()}.
     * @return 3D building block CH2 with two APs both on C.
     * @throws DENOPTIMException
     */
    private DENOPTIMVertex getCH2Fragment() throws DENOPTIMException {
        IAtomContainer atomContainer = chemBuilder.newAtomContainer();
        String[] elements = new String[]{"C", "H", "H"};
        Point3d[] atmCoords = new Point3d[]{
                new Point3d(0.0, 0.0, 0.0),
                new Point3d(0.0000, -0.8900, -0.6293),
                new Point3d(0.0000, 0.8900, -0.6293),
        };
        for (int i=0; i<elements.length; i++) {
            IAtom atom = chemBuilder.newAtom();
            atom.setSymbol(elements[i]);
            atom.setPoint3d(atmCoords[i]);
            atomContainer.addAtom(atom);
        }
        atomContainer.addBond(0, 1, IBond.Order.SINGLE);
        atomContainer.addBond(0, 2, IBond.Order.SINGLE);

        DENOPTIMFragment v = new DENOPTIMFragment(2, atomContainer,
                BBType.FRAGMENT);
        
        Point3d[] apCoords = new Point3d[]{
                new Point3d(-0.8900, 0.0000, 0.6293),
                new Point3d(0.8900, 0.0000, 0.6293),
        };
        for (int i = 0; i < 2; i++) {

            APClass apClass = APClass.make("c", 0);

            FragmentSpace.getBondOrderMap().put(apClass.getRule(),
                    DENOPTIMEdge.BondType.SINGLE);

            v.addAP(0, apClass,apCoords[i],1);
        }
        return v;
    }
    
//------------------------------------------------------------------------------

    /**
     * The coordinates hard coded in this method must not be changed because
     * they are needed to reproduce results in {@link #testGetIAtomContainer()}.
     * @return 3D building block C(=O)N with three APs: one on C and two on N.
     * @throws DENOPTIMException
     */
    private DENOPTIMVertex getAmideFragment() throws DENOPTIMException {
        IAtomContainer atomContainer = chemBuilder.newAtomContainer();
        String[] elements = new String[]{"C", "O", "N"};
        Point3d[] atmCoords = new Point3d[]{
                new Point3d(0.6748, -0.1898, -0.0043),
                new Point3d(1.0460, -1.3431, -0.0581),
                new Point3d(-0.6427, 0.0945, -0.0002),
        };
        for (int i=0; i<elements.length; i++) {
            IAtom atom = chemBuilder.newAtom();
            atom.setSymbol(elements[i]);
            atom.setPoint3d(atmCoords[i]);
            atomContainer.addAtom(atom);
        }
        atomContainer.addBond(0, 1, IBond.Order.DOUBLE);
        atomContainer.addBond(0, 2, IBond.Order.SINGLE);

        DENOPTIMFragment v = new DENOPTIMFragment(2, atomContainer,
                BBType.FRAGMENT);
        
        Point3d[] apCoords = new Point3d[]{
                new Point3d(1.6864, 0.9254, 0.0580),
                new Point3d(-1.6259, -0.9902, 0.0560),
                new Point3d(-1.0915, 1.4881, -0.0518),
        };
        int[] srcAtm = new int[] {0, 2, 2};
        APClass[] apClass = new APClass[] {
                APClass.make("Camide", 0),
                APClass.make("NAmide", 0),
                APClass.make("NAmide", 0)};
        for (int i = 0; i < 3; i++) {

            FragmentSpace.getBondOrderMap().put(apClass[i].getRule(),
                    DENOPTIMEdge.BondType.SINGLE);

            v.addAP(srcAtm[i], apClass[i],apCoords[i], 1);
        }
        return v;
    }

//------------------------------------------------------------------------------

    /**
     * @return 0D (no 3D coords!) for OH
     * @throws DENOPTIMException
     */
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
    public void testGetAttachmentPoints_returnsCorrectNumberOfAPs() 
            throws DENOPTIMException 
    {
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
        DENOPTIMGraph innerGraph = new DENOPTIMGraph();
        innerGraph.addVertex(v1);
        innerGraph.appendVertexOnAP(v1.getAP(0), v2.getAP(0));
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
        // Unsure if inner APs should be required to have the same direction
        // vectors as required APs.
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

    @Test
    public void testAddAP_after_setInnerGraph_throwsException() {
        DENOPTIMTemplate t = new DENOPTIMTemplate(BBType.NONE);
        DENOPTIMGraph g = new DENOPTIMGraph();
        t.setInnerGraph(g);
        assertThrows(IllegalArgumentException.class, () -> t.addAP(0, 1, 1));
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
    
    private DENOPTIMTemplate getTestAmideTemplate() throws DENOPTIMException
    {
        DENOPTIMVertex v1 = getCH2Fragment();
        DENOPTIMVertex v2 = getCH2Fragment();
        DENOPTIMVertex v3 = getCH2Fragment();
        DENOPTIMVertex v4 = getCH2Fragment();
        DENOPTIMVertex v5 = getAmideFragment();
        
        DENOPTIMGraph g = new DENOPTIMGraph();
        g.addVertex(v1);
        g.appendVertexOnAP(v1.getAP(0), v5.getAP(0));
        g.appendVertexOnAP(v1.getAP(1), v2.getAP(1));
        g.appendVertexOnAP(v2.getAP(0), v3.getAP(1));
        g.appendVertexOnAP(v3.getAP(0), v4.getAP(0));
        
        DENOPTIMTemplate t = new DENOPTIMTemplate(BBType.UNDEFINED);
        t.setInnerGraph(g);
        
        return t;
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetIAtomContainer() throws Exception
    {
        DENOPTIMTemplate t = getTestAmideTemplate();
        
        IAtomContainer mol = t.getIAtomContainer();
        
        String[] elements = new String[]{"C", "H", "N", "O"};
        int[] expected = new int[]{5, 8, 1, 1};
        for (int i=0; i<elements.length; i++)
        {
            assertEquals(expected[i],
                    DENOPTIMMoleculeUtils.countAtomsOfElement(mol, elements[i]),
                    "Number of '" + elements[i] + "'.");
        }
        
        String refGeometry = NL + 
                "  CDK     04132117333D" + NL + 
                NL + 
                " 15 14  0  0  0  0  0  0  0  0999 V2000" + NL + 
                "    0.0000    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    0.0000   -0.8900   -0.6293 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    0.0000    0.8900   -0.6293 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "   -1.0602    0.0000    0.7497 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "   -1.4136   -1.0211    1.3004 O   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "   -1.7681    1.1349    0.9158 N   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    0.8900    0.0000    0.6293 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    0.8900   -0.8900    1.2586 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    0.8900    0.8900    1.2586 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    1.7800    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    2.3733   -0.8900    0.2098 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    2.3733    0.8900    0.2098 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    1.4834    0.0000   -1.0489 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    0.8901   -0.8900   -1.2587 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    0.8901    0.8900   -1.2587 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "  1  2  1  0  0  0  0" + NL + 
                "  1  3  1  0  0  0  0" + NL + 
                "  4  5  2  0  0  0  0" + NL + 
                "  4  6  1  0  0  0  0" + NL + 
                "  1  4  1  0  0  0  0" + NL + 
                "  7  8  1  0  0  0  0" + NL + 
                "  7  9  1  0  0  0  0" + NL + 
                "  1  7  1  0  0  0  0" + NL + 
                " 10 11  1  0  0  0  0" + NL + 
                " 10 12  1  0  0  0  0" + NL + 
                "  7 10  1  0  0  0  0" + NL + 
                " 13 14  1  0  0  0  0" + NL + 
                " 13 15  1  0  0  0  0" + NL + 
                " 10 13  1  0  0  0  0" + NL + 
                "M  END" + NL;

        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        String fileName = tempDir.getAbsolutePath() + SEP + "refMol.sdf";
        DenoptimIO.writeData(fileName, refGeometry, false);
        
        IAtomContainer refMol = DenoptimIO.readMoleculeData(fileName).get(0);
        
        KabschAlignment sa = null;
        try {
           sa = new KabschAlignment(refMol,mol);
           sa.align();
        } catch (CDKException e){
            e.printStackTrace();
            fail("KabschAlignment failed: "+e.getMessage());
        }
        assertTrue(sa.getRMSD()<0.0001,"RMSD between generated and expected "
                + "geometry");
        
        // This check is done ignoring AP order
        Point3d[] expectedAPHeads = new Point3d[] {
                new Point3d(-2.8954,1.1648,1.8510),
                new Point3d(-1.4101,2.3385,0.1613),
                new Point3d(2.3734,0.0000,-1.6782)};
        for (DENOPTIMAttachmentPoint ap : t.getAttachmentPoints())
        {
            Point3d apHead = new Point3d(ap.getDirectionVector());
            boolean found = false;
            double[] dists = new double[3];
            for (int i=0; i<expectedAPHeads.length; i++)
            {
                Point3d expectedAPHead = expectedAPHeads[i];
                double dist = apHead.distance(expectedAPHead);
                dists[i] = dist;
                if (dist<0.001)
                {
                    found = true;
                    break;
                }
            }
            assertTrue(found,"Inconsistent placement of outer AP (errors: "  
                    + dists[0] + ", " + dists[1] + ", " + dists[2] + "). "
                    + "AP: "+ap);
        }
    }
    
//------------------------------------------------------------------------------
    
    private DENOPTIMTemplate getTemplateDeepTest() throws DENOPTIMException
    {
        DENOPTIMTemplate v1 = null;
        for (int i =0; i<10; i++)
        {
            if (v1 == null)
            {
                v1 = getTestAmideTemplate();
            }
            DENOPTIMVertex v2 = getCH2Fragment();
            v2.setVertexId(100+i);
            DENOPTIMGraph g = new DENOPTIMGraph();
            g.addVertex(v1);
            g.appendVertexOnAP(v1.getAP(0), v2.getAP(0));
            DENOPTIMTemplate t = new DENOPTIMTemplate(BBType.UNDEFINED);
            t.setInnerGraph(g);
            v1 = t;
        }
        return v1;
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetIAtomContainer_DeepVertex() throws Exception
    {
        DENOPTIMTemplate t = getTemplateDeepTest();
        
        IAtomContainer mol = t.getIAtomContainer();
        
        String[] elements = new String[]{"C", "H", "N", "O"};
        int[] expected = new int[]{15, 28, 1, 1};
        for (int i=0; i<elements.length; i++)
        {
            assertEquals(expected[i],
                    DENOPTIMMoleculeUtils.countAtomsOfElement(mol, elements[i]),
                    "Number of '" + elements[i] + "'.");
        }
        
        String refGeometry = NL + 
                "  CDK     04132117333D" + NL + 
                NL + 
                " 45 44  0  0  0  0  0  0  0  0999 V2000" + NL + 
                "    0.0000    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    0.0000   -0.8900   -0.6293 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    0.0000    0.8900   -0.6293 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "   -1.0602    0.0000    0.7497 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "   -1.4136   -1.0211    1.3004 O   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "   -1.7681    1.1349    0.9158 N   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    0.8900    0.0000    0.6293 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    0.8900   -0.8900    1.2586 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    0.8900    0.8900    1.2586 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    1.7800    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    2.3733   -0.8900    0.2098 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    2.3733    0.8900    0.2098 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    1.4834    0.0000   -1.0489 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    0.8901   -0.8900   -1.2587 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    0.8901    0.8900   -1.2587 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    2.3734    0.0000   -1.6782 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    2.3734   -0.8900   -2.3075 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    2.3734    0.8900   -2.3075 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "   -2.7511    1.1609    1.7313 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "   -2.4728    0.6487    2.6523 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "   -3.0101    2.1958    1.9552 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "   -1.4559    2.1845    0.2578 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "   -1.2473    1.9156   -0.7777 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "   -2.2838    2.8928    0.2896 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    3.2634    0.0000   -1.0489 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    3.8567   -0.8900   -1.2587 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    3.8567    0.8900   -1.2587 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "   -3.6091    0.6606    1.2823 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "   -4.2319    0.2324    2.0677 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "   -4.1916    1.3822    0.7094 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "   -0.5703    2.6407    0.7002 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    0.3217    2.1322    0.3340 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "   -0.5275    3.6941    0.4234 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    2.9668    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    3.3624   -0.8900    0.4894 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    3.3624    0.8900    0.4894 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "   -3.2617   -0.1330    0.6208 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "   -4.0122   -0.9222    0.5749 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "   -3.0987    0.2719   -0.3780 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "   -0.6194    2.5519    1.7855 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "   -0.2733    1.5626    2.0851 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    0.0148    3.3122    2.2413 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    1.8790    0.0000    0.0700 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    1.5495   -0.8900    0.6061 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "    1.5495    0.8900    0.6061 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL + 
                "  1  2  1  0  0  0  0" + NL + 
                "  1  3  1  0  0  0  0" + NL + 
                "  4  5  2  0  0  0  0" + NL + 
                "  4  6  1  0  0  0  0" + NL + 
                "  1  4  1  0  0  0  0" + NL + 
                "  7  8  1  0  0  0  0" + NL + 
                "  7  9  1  0  0  0  0" + NL + 
                "  1  7  1  0  0  0  0" + NL + 
                " 10 11  1  0  0  0  0" + NL + 
                " 10 12  1  0  0  0  0" + NL + 
                "  7 10  1  0  0  0  0" + NL + 
                " 13 14  1  0  0  0  0" + NL + 
                " 13 15  1  0  0  0  0" + NL + 
                " 10 13  1  0  0  0  0" + NL + 
                " 16 17  1  0  0  0  0" + NL + 
                " 16 18  1  0  0  0  0" + NL + 
                " 13 16  1  0  0  0  0" + NL + 
                " 19 20  1  0  0  0  0" + NL + 
                " 19 21  1  0  0  0  0" + NL + 
                "  6 19  1  0  0  0  0" + NL + 
                " 22 23  1  0  0  0  0" + NL + 
                " 22 24  1  0  0  0  0" + NL + 
                "  6 22  1  0  0  0  0" + NL + 
                " 25 26  1  0  0  0  0" + NL + 
                " 25 27  1  0  0  0  0" + NL + 
                " 16 25  1  0  0  0  0" + NL + 
                " 28 29  1  0  0  0  0" + NL + 
                " 28 30  1  0  0  0  0" + NL + 
                " 19 28  1  0  0  0  0" + NL + 
                " 31 32  1  0  0  0  0" + NL + 
                " 31 33  1  0  0  0  0" + NL + 
                " 22 31  1  0  0  0  0" + NL + 
                " 34 35  1  0  0  0  0" + NL + 
                " 34 36  1  0  0  0  0" + NL + 
                " 25 34  1  0  0  0  0" + NL + 
                " 37 38  1  0  0  0  0" + NL + 
                " 37 39  1  0  0  0  0" + NL + 
                " 28 37  1  0  0  0  0" + NL + 
                " 40 41  1  0  0  0  0" + NL + 
                " 40 42  1  0  0  0  0" + NL + 
                " 31 40  1  0  0  0  0" + NL + 
                " 43 44  1  0  0  0  0" + NL + 
                " 43 45  1  0  0  0  0" + NL + 
                " 34 43  1  0  0  0  0" + NL + 
                "M  END";

        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        String fileName = tempDir.getAbsolutePath() + SEP + "refMol.sdf";
        DenoptimIO.writeData(fileName, refGeometry, false);
        
        IAtomContainer refMol = DenoptimIO.readMoleculeData(fileName).get(0);
        
        KabschAlignment sa = null;
        try {
           sa = new KabschAlignment(refMol,mol);
           sa.align();
        } catch (CDKException e){
            e.printStackTrace();
            fail("KabschAlignment failed: "+e.getMessage());
        }
        assertTrue(sa.getRMSD()<0.0001,"RMSD between generated and expected "
                + "geometry");
        
        // This check is done ignoring AP order
        Point3d[] expectedAPHeads = new Point3d[] {
                new Point3d(-2.3270,-0.5423,1.0039),
                new Point3d(-1.6488,2.6919,2.1153),
                new Point3d(1.4504,0.0000,-0.9322)};
        for (DENOPTIMAttachmentPoint ap : t.getAttachmentPoints())
        {
            Point3d apHead = new Point3d(ap.getDirectionVector());
            boolean found = false;
            double[] dists = new double[3];
            for (int i=0; i<expectedAPHeads.length; i++)
            {
                Point3d expectedAPHead = expectedAPHeads[i];
                double dist = apHead.distance(expectedAPHead);
                dists[i] = dist;
                if (dist<0.001)
                {
                    found = true;
                    break;
                }
            }
            assertTrue(found,"Inconsistent placement of outer AP (errors: "  
                    + dists[0] + ", " + dists[1] + ", " + dists[2] + "). "
                    + "AP: "+ap);
        }
    }
}
