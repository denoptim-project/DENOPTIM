package denoptim.molecule;

import java.util.ArrayList;

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

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.DENOPTIMFragment.BBType;
import denoptim.utils.GraphUtils;
import denoptim.utils.RandomUtils;
import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for DENOPTIMTemplate
 * 
 */

public class DENOPTIMTemplateTest
{
    
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
    public void testGetAttachmentPointsReturnsAPsWithTemplateAsOwner() {
        DENOPTIMTemplate template = new DENOPTIMTemplate(BBType.NONE);
        EmptyVertex v = new EmptyVertex();
        try {
            template.addAP(0, 1, 1);
            v.addAP(0, 1, 1);
        } catch (DENOPTIMException e) {
            fail("unexpected exception");
        }
        DENOPTIMGraph innerGraph = new DENOPTIMGraph();
        innerGraph.addVertex(v);
        template.setInnerGraph(innerGraph);

        int totalAPCount = 1;
        for (int i = 0; i < totalAPCount; i++) {
            DENOPTIMVertex actualOwner = template.getAttachmentPoints().get(i)
                    .getOwner();
            assertEquals(template, actualOwner);
        }
    }

//------------------------------------------------------------------------------

    @Test
    public void testGetAttachmentPointsReturnsCorrectNumberOfAPs() {
        // Einar: Prevents nullpointer exception later
        RandomUtils.initialiseRNG(13);
        DENOPTIMTemplate template = new DENOPTIMTemplate(BBType.NONE);
        int requiredAPCount = 2;
        int atmPos = 0;
        int atmConns = 1;
        int apConns = 1;
        EmptyVertex v1 = new EmptyVertex();
        EmptyVertex v2 = new EmptyVertex();
        int v1APCount = 3;
        int v2APCount = 2;
        try {
            for (int i = 0; i < requiredAPCount; i++) {
                template.addAP(atmPos, atmConns, apConns);
            }
            for (int i = 0; i < v1APCount; i++) {
                v1.addAP(atmPos, atmConns, apConns);
            }
            for (int i = 0; i < v2APCount; i++) {
                v2.addAP(atmPos, atmConns, apConns);
            }
        } catch (DENOPTIMException e) {
            fail("unexpected exception");
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
    public void testSetInnerGraphThrowsIllegalArgExcIfGraphIncompatibleWithRequiredAPs()
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

        DENOPTIMTemplate template = new DENOPTIMTemplate(BBType.NONE);
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
        testSameAtomConnections(template, innerGraph);
        testSameApConnections(template, innerGraph);
        testSameDirVec(template, innerGraph);
        testSameAPClass(template, innerGraph);
    }

    private void testSameAPClass(DENOPTIMTemplate t, DENOPTIMGraph innerGraph) {
        DENOPTIMAttachmentPoint ap = innerGraph.getVertexAtPosition(0).getAP(1);
        try {
            ap.setAPClass(innerGraph.getVertexAtPosition(0).getAP(0).getAPClass());
            assertThrows(IllegalArgumentException.class,
                    () -> t.setInnerGraph(innerGraph));
        } catch (DENOPTIMException e) {
            fail("Expected " + IllegalArgumentException.class + ", but was " + e.getClass());
        }
    }

    private void testSameDirVec(DENOPTIMTemplate t, DENOPTIMGraph innerGraph) {
        DENOPTIMAttachmentPoint ap = innerGraph.getVertexAtPosition(0).getAP(1);
        double[] correctDirVec = ap.getDirectionVector();
        correctDirVec[1] = -correctDirVec[1];
        assertThrows(IllegalArgumentException.class,
                () -> t.setInnerGraph(innerGraph));
    }

    private void testSameApConnections(DENOPTIMTemplate t,
                                       DENOPTIMGraph innerGraph) {
        DENOPTIMAttachmentPoint ap = innerGraph.getVertexAtPosition(0).getAP(0);
        int correctApConnections = ap.getFreeConnections();
        ap.setFreeConnections(correctApConnections + 1);
        assertThrows(IllegalArgumentException.class,
                () -> t.setInnerGraph(innerGraph));
    }

    private void testSameAtomConnections(DENOPTIMTemplate t,
                                         DENOPTIMGraph innerGraph) {
        DENOPTIMAttachmentPoint ap = innerGraph.getVertexAtPosition(0).getAP(0);
        int correctAtomConnections = ap.getTotalConnections();
        ap.setTotalConnections(correctAtomConnections + 1);
        assertThrows(IllegalArgumentException.class,
                () -> t.setInnerGraph(innerGraph));
    }

    private void testAtLeastSameNumberOfAPs(DENOPTIMTemplate t,
                                            int expNumberOfAPs) {
        DENOPTIMVertex v = new EmptyVertex();
        for (int i = 0; i < expNumberOfAPs - 1; i++) {
            try {
                v.addAP();
            } catch (DENOPTIMException e) {
                fail("unexpected exception");
            }
        }
        DENOPTIMGraph innerGraph = new DENOPTIMGraph();
        innerGraph.addVertex(v);
        assertThrows(IllegalArgumentException.class,
                () -> t.setInnerGraph(innerGraph));
    }

//------------------------------------------------------------------------------

    @Test
    public void testCallingAddAPAfterSetInnerGraphThrowsDENOPTIMExc() {
        DENOPTIMTemplate t = new DENOPTIMTemplate(BBType.NONE);
        DENOPTIMGraph g = new DENOPTIMGraph();
        t.setInnerGraph(g);
        assertThrows(DENOPTIMException.class, () -> t.addAP(0, 1, 1));
    }

//------------------------------------------------------------------------------

    @Test
    public void testAtomPropertyVertexIdReturnsTemplateId() {
        IAtom a = new Atom("C");
        IAtomContainer c = new AtomContainer();
        c.addAtom(a);
        DENOPTIMVertex v = null;
        try {
            v = new DENOPTIMFragment(c, BBType.NONE);
        } catch (DENOPTIMException e) {
            fail("unexpected exception thrown");
        }
        v.setVertexId(0);
        DENOPTIMGraph g = new DENOPTIMGraph();
        g.addVertex(v);
        DENOPTIMTemplate t = new DENOPTIMTemplate(BBType.NONE);
        t.setVertexId(1);
        t.setInnerGraph(g);

        int expected = t.getVertexId();
        int actual = t
                .getInnerGraph()
                .getVertexAtPosition(0)
                .getIAtomContainer()
                .getAtom(0)
                .getProperty(DENOPTIMConstants.ATMPROPVERTEXID);
        assertEquals(expected, actual);
    }

}
