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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.silent.Bond;

import denoptim.constants.DENOPTIMConstants;
import denoptim.fragspace.FragmentSpace;
import denoptim.molecule.DENOPTIMEdge.BondType;
import denoptim.molecule.DENOPTIMFragment.BBType;

/**
 * Unit test for DENOPTIMVertex
 * 
 * @author Marco Foscato
 */

public class DENOPTIMVertexTest
{
	private StringBuilder reason = new StringBuilder();
	
//------------------------------------------------------------------------------
	
    @Test
    public void testSameAs_Equal() throws Exception
    {
    	ArrayList<DENOPTIMAttachmentPoint> apsA = new ArrayList<>();
        DENOPTIMVertex vA = new EmptyVertex(0);
        apsA.add(new DENOPTIMAttachmentPoint(vA, 0, 1, 1));
        apsA.add(new DENOPTIMAttachmentPoint(vA, 1, 1, 1));
        apsA.add(new DENOPTIMAttachmentPoint(vA, 2, 1, 1));
        apsA.add(new DENOPTIMAttachmentPoint(vA, 3, 1, 1));
        vA.setAttachmentPoints(apsA);

    	ArrayList<DENOPTIMAttachmentPoint> apsB = new ArrayList<>();
        DENOPTIMVertex vB = new EmptyVertex(90);
        apsB.add(new DENOPTIMAttachmentPoint(vB, 0, 1, 1));
        apsB.add(new DENOPTIMAttachmentPoint(vB, 1, 1, 1));
        apsB.add(new DENOPTIMAttachmentPoint(vB, 2, 1, 1));
        apsB.add(new DENOPTIMAttachmentPoint(vB, 3, 1, 1));
        vB.setAttachmentPoints(apsB);
        //NB: vertex ID must be ignores by the sameAs method

    	assertTrue(vA.sameAs(vB, reason));	
    }
    
//------------------------------------------------------------------------------
	
    @Test
    public void testSameAs_DiffAPConnection() throws Exception
    {
    	ArrayList<DENOPTIMAttachmentPoint> apsA = new ArrayList<>();
        DENOPTIMVertex vA = new EmptyVertex(0);
        apsA.add(new DENOPTIMAttachmentPoint(vA, 0, 1, 1));
        apsA.add(new DENOPTIMAttachmentPoint(vA, 1, 1, 1));
        apsA.add(new DENOPTIMAttachmentPoint(vA, 2, 1, 1));
        apsA.add(new DENOPTIMAttachmentPoint(vA, 3, 1, 1));
        vA.setAttachmentPoints(apsA);

    	ArrayList<DENOPTIMAttachmentPoint> apsB = new ArrayList<>();
        DENOPTIMVertex vB = new EmptyVertex(90);
        apsB.add(new DENOPTIMAttachmentPoint(vB, 0, 1, 1));
        apsB.add(new DENOPTIMAttachmentPoint(vB, 1, 1, 1));
        apsB.add(new DENOPTIMAttachmentPoint(vB, 2, 1, 1));
        apsB.add(new DENOPTIMAttachmentPoint(vB, 3, 1, 2)); //diff
        vB.setAttachmentPoints(apsB);
        //NB: vertex ID must be ignores by the sameAs method

    	assertFalse(vA.sameAs(vB, reason));	
    }
    
//------------------------------------------------------------------------------
	
    @Test
    public void testSameAs_DiffAPNum() throws Exception
    {
    	ArrayList<DENOPTIMAttachmentPoint> apsA = new ArrayList<>();
        DENOPTIMVertex vA = new EmptyVertex(0, apsA);
        apsA.add(new DENOPTIMAttachmentPoint(vA, 0, 1, 1));
        apsA.add(new DENOPTIMAttachmentPoint(vA, 1, 1, 1));
        apsA.add(new DENOPTIMAttachmentPoint(vA, 2, 1, 1));
        apsA.add(new DENOPTIMAttachmentPoint(vA, 3, 1, 1));
        vA.setAttachmentPoints(apsA);

    	ArrayList<DENOPTIMAttachmentPoint> apsB = new ArrayList<>();
        DENOPTIMVertex vB = new EmptyVertex(90);
        apsB.add(new DENOPTIMAttachmentPoint(vB, 0, 1, 1));
        apsB.add(new DENOPTIMAttachmentPoint(vB, 1, 1, 1));
        apsB.add(new DENOPTIMAttachmentPoint(vB, 2, 1, 1));
        vB.setAttachmentPoints(apsB);
        //NB: vertex ID must be ignores by the sameAs method

    	assertFalse(vA.sameAs(vB, reason));	
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testClone() throws Exception
    {
        // This is just to avoid the warnings about trying to get a bond type
        // when the fragment space in not defined
        String APRULE = "apc";
        HashMap<String, BondType> map = new HashMap<String, BondType>();
        map.put(APRULE,BondType.DOUBLE);
        FragmentSpace.setBondOrderMap(map);

        ArrayList<DENOPTIMAttachmentPoint> apsA = new ArrayList<>();
        DENOPTIMVertex v = new EmptyVertex(0);
        apsA.add(new DENOPTIMAttachmentPoint(v, 1, 1, 1));
        apsA.add(new DENOPTIMAttachmentPoint(v, 2, 2, 1));
        apsA.add(new DENOPTIMAttachmentPoint(v, 3, 2, 1));
        v.setAttachmentPoints(apsA);
        v.setLevel(26);
        
        DENOPTIMVertex c = v.clone();
        
        assertEquals(v.getVertexId(), c.getVertexId(), "Vertex ID");
        assertEquals(v.getNumberOfAP(), c.getNumberOfAP(), "Number of APS");
        assertEquals(v.getSymmetricAPSets().size(), 
                c.getSymmetricAPSets().size(), "Number of SymAPs sets");
        assertEquals(v.getLevel(), c.getLevel(), "Level");
        assertEquals(v.isRCV(), c.isRCV(), "RCV flag");
        assertNotEquals(v.hashCode(), c.hashCode(), "Hash code"); 
        
        
        v = new DENOPTIMFragment();
        Atom a1 = new Atom("C", new Point3d(new double[]{0.0, 1.1, 2.2}));
        Atom a2 = new Atom("C", new Point3d(new double[]{1.0, 1.1, 2.2}));
        Atom a3 = new Atom("C", new Point3d(new double[]{2.0, 1.1, 2.2}));
        ((DENOPTIMFragment) v).addAtom(a1);
        ((DENOPTIMFragment) v).addAtom(a2);
        ((DENOPTIMFragment) v).addAtom(a3);
        ((DENOPTIMFragment) v).addBond(new Bond(a1, a2));
        ((DENOPTIMFragment) v).addBond(new Bond(a2, a3));
        String APCLASS = APRULE + DENOPTIMConstants.SEPARATORAPPROPSCL +"0";
        ((DENOPTIMFragment) v).addAP(a3, APClass.make(APCLASS), new Point3d(
                new double[]{0.0, 2.2, 3.3}));
        ((DENOPTIMFragment) v).addAP(a3, APClass.make(APCLASS), new Point3d(
                new double[]{0.0, 0.0, 3.3}));
        ((DENOPTIMFragment) v).addAP(a3, APClass.make(APCLASS), new Point3d(
                new double[]{0.0, 0.0, 1.1}));
        ((DENOPTIMFragment) v).addAP(a1, APClass.make(APCLASS), new Point3d(
                new double[]{3.0, 0.0, 3.3}));
        v.setLevel(62);
        
        c = v.clone();
        
        assertEquals(v.getVertexId(), c.getVertexId(), "Vertex ID");
        assertEquals(v.getNumberOfAP(), c.getNumberOfAP(), "Number of APS");
        assertEquals(v.getSymmetricAPSets().size(), 
                c.getSymmetricAPSets().size(), "Number of SymAPs sets");
        assertEquals(v.getLevel(), c.getLevel(), "Level");
        assertEquals(v.isRCV(), c.isRCV(), "RCV flag");
        assertNotEquals(v.hashCode(), c.hashCode(), "Hash code");
        assertEquals(v.getAllAPClasses(),c.getAllAPClasses(),"APClass list");
        assertEquals(v.getAllAPClasses().get(0).hashCode(),
                c.getAllAPClasses().get(0).hashCode(),"APClass hash code");
    }
    
//------------------------------------------------------------------------------
}
