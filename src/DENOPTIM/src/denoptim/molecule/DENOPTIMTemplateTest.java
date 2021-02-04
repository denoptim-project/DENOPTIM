package denoptim.molecule;

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

import denoptim.exception.DENOPTIMException;
import denoptim.molecule.DENOPTIMFragment.BBType;
import denoptim.utils.RandomUtils;
import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Unit test for DENOPTIMTemplate
 * 
 */

public class DENOPTIMTemplateTest
{

    @Test
    public void testGetAttachmentPointsReturnsAPsWithTemplateAsOwner() {
        DENOPTIMTemplate template = new DENOPTIMTemplate(BBType.NONE);
        template.addAP(0, 1, 1);
        EmptyVertex v = new EmptyVertex();
        v.addAP(0, 1, 1);
        DENOPTIMGraph innerGraph = new DENOPTIMGraph();
        innerGraph.addVertex(v);
        template.setInnerGraph(innerGraph);

        int totalAPCount = 2;
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
        int templateAPCount = 2;
        for (int i = 0; i < templateAPCount; i++) {
            template.addAP(0, 1, 1);
        }
        EmptyVertex v1 = new EmptyVertex();
        int v1APCount = 3;
        for (int i = 0; i < v1APCount; i++) {
            v1.addAP(0, 1, 1);
        }
        EmptyVertex v2 = new EmptyVertex();
        int v2APCount = 2;
        for (int i = 0; i < v2APCount; i++) {
            v2.addAP(0, 1, 1);
        }
        v1.connectVertices(v2);
        DENOPTIMGraph innerGraph = new DENOPTIMGraph();
        innerGraph.addVertex(v1);
        innerGraph.addVertex(v2);
        try {
            template.setInnerGraph(innerGraph);
        } catch (IllegalArgumentException e) {
            // ignore
        }

        // -2 since 2 APs are used to connect v1 and v2.
        int expectedAPCount = templateAPCount + v1APCount + v2APCount - 2;
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
            v.addAP();
        }
        DENOPTIMGraph innerGraph = new DENOPTIMGraph();
        innerGraph.addVertex(v);
        assertThrows(IllegalArgumentException.class,
                () -> t.setInnerGraph(innerGraph));
    }
}
