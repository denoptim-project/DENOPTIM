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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.molecule.DENOPTIMFragment.BBType;
import denoptim.utils.GraphUtils;
import denoptim.utils.RandomUtils;
import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.jupiter.api.BeforeAll;
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
        // Prevents nullpointer exception later
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
        template.setInnerGraph(innerGraph);

        // -2 since 2 APs are used to connect v1 and v2.
        int expectedAPCount = templateAPCount + v1APCount + v2APCount - 2;
        int actualAPCount = template.getAttachmentPoints().size();
        assertEquals(expectedAPCount, actualAPCount);
    }

//------------------------------------------------------------------------------

    @Ignore // "Unfinished test"
    public void testSetInnerGraphThrowsIllegalArgExcIfRequiredAPsNotPresentOnGraph() throws DENOPTIMException {
        IAtomContainer singleCarbon = new AtomContainer();
        singleCarbon.addAtom(new Atom("C"));
        DENOPTIMFragment v1 = new DENOPTIMFragment(singleCarbon,
                BBType.FRAGMENT);

        IAtomContainer singleNitrogen = new AtomContainer();
        singleNitrogen.addAtom(new Atom("N"));
        DENOPTIMFragment v2 = new DENOPTIMFragment(singleNitrogen,
                BBType.FRAGMENT);

        DENOPTIMTemplate template = new DENOPTIMTemplate(BBType.NONE);

    }
}
