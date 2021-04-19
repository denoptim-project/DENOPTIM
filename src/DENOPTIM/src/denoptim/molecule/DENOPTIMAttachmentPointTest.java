package denoptim.molecule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import denoptim.constants.DENOPTIMConstants;
import denoptim.fragspace.FragmentSpace;
import denoptim.molecule.DENOPTIMEdge.BondType;

/**
 * Unit test for DENOPTIMAttachmentPoint
 * 
 * @author Marco Foscato
 */

public class DENOPTIMAttachmentPointTest
{
	private final int ATMID = 6;
	private final int APCONN = 3;
	private final String APRULE = "MyRule";
	private final String APSUBRULE = "1";
	private final String APCLASS = APRULE
			+ DENOPTIMConstants.SEPARATORAPPROPSCL + APSUBRULE;
	private final double[] DIRVEC = new double[]{1.1, 2.2, 3.3};
	private final EmptyVertex dummyVertex = new EmptyVertex();
	
//-----------------------------------------------------------------------------
	
    @Test
    public void testConstructorsAndSDFString() throws Exception
    {
        // This is just to avoid the warnings about trying to get a bond type
        // when the fragment space in not defined
        HashMap<String, BondType> map = new HashMap<>();
        map.put(APRULE,BondType.SINGLE);
        FragmentSpace.setBondOrderMap(map);

		dummyVertex.addAP(ATMID, APCONN, APCONN, DIRVEC, APClass.make(APCLASS));
		DENOPTIMAttachmentPoint ap = dummyVertex.getAP(0);
    	
    	String str2 = ap.getSingleAPStringSDF(true);
    	DENOPTIMAttachmentPoint ap2 = new DENOPTIMAttachmentPoint(dummyVertex
				, str2);
    	
    	String str3 = (ATMID+1) + DENOPTIMConstants.SEPARATORAPPROPAAP
    			+ ap.getSingleAPStringSDF(false);
    	DENOPTIMAttachmentPoint ap3 = new DENOPTIMAttachmentPoint(dummyVertex
				, str3);
    	
    	assertEquals(ap.getSingleAPStringSDF(true),
    			ap2.getSingleAPStringSDF(true));
    	assertEquals(ap2.toString(),ap3.toString());
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testConstructorsAndSDFStringNoDirVec() throws Exception
    {
		dummyVertex.addAP(ATMID, APCONN, APCONN, APClass.make(APCLASS));
		DENOPTIMAttachmentPoint ap = dummyVertex.getAP(0);

    	String str2 = ap.getSingleAPStringSDF(true);
    	DENOPTIMAttachmentPoint ap2 = new DENOPTIMAttachmentPoint(dummyVertex
				, str2);
    	
    	String str3 = (ATMID+1) + DENOPTIMConstants.SEPARATORAPPROPAAP
    			+ ap.getSingleAPStringSDF(false);
    	DENOPTIMAttachmentPoint ap3 = new DENOPTIMAttachmentPoint(dummyVertex
				, str3);
    	
    	assertEquals(ap.getSingleAPStringSDF(true),
    			ap2.getSingleAPStringSDF(true));
    	assertEquals(ap2.toString(),ap3.toString());
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testSortAPs() throws Exception
    {
    	dummyVertex.addAP(0,2,1);
    	dummyVertex.addAP(0,1,1);
		DENOPTIMAttachmentPoint ap1 = dummyVertex.getAP(0);
		DENOPTIMAttachmentPoint ap2 = dummyVertex.getAP(1);

		dummyVertex.addAP(4, APCONN, APCONN, DIRVEC, APClass.make("AA:0"));
		DENOPTIMAttachmentPoint ap3 = dummyVertex.getAP(2);

		dummyVertex.addAP(4, APCONN, APCONN, DIRVEC, APClass.make("AA:1"));
		DENOPTIMAttachmentPoint ap4 = dummyVertex.getAP(3);

		dummyVertex.addAP(5, APCONN, APCONN, new double[]{1.1, 2.2, 3.3},
				APClass.make(APCLASS));
		DENOPTIMAttachmentPoint ap5 = dummyVertex.getAP(4);

		dummyVertex.addAP(5, APCONN, APCONN, new double[]{2.2, 2.2, 3.3},
				APClass.make(APCLASS));
		DENOPTIMAttachmentPoint ap6 = dummyVertex.getAP(5);

    	ArrayList<DENOPTIMAttachmentPoint> list =
				dummyVertex.getAttachmentPoints();
    	
    	list.sort(new DENOPTIMAttachmentPointComparator());
    	
    	assertEquals(list.get(0),ap1);
    	assertEquals(list.get(1),ap2);
    	assertEquals(list.get(2),ap3);
    	assertEquals(list.get(3),ap4);
    	assertEquals(list.get(4),ap5);
    	assertEquals(list.get(5),ap6);
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testSameAs()
    {
		dummyVertex.addAP(1, 2, 1);
		dummyVertex.addAP(1, 2, 1);
		DENOPTIMAttachmentPoint apA = dummyVertex.getAP(0);
		DENOPTIMAttachmentPoint apB = dummyVertex.getAP(1);

    	assertEquals(-1,apA.compareTo(apB),"Comparison driven by ID.");
    	assertTrue(apA.sameAs(apB),"SameAs ignores ID.");
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testSameAs_DiffSrcAtm()
    {
		dummyVertex.addAP(1, 2, 1);
		dummyVertex.addAP(2, 2, 1);
    	DENOPTIMAttachmentPoint apA = dummyVertex.getAP(0);
    	DENOPTIMAttachmentPoint apB = dummyVertex.getAP(1);

    	assertFalse(apA.sameAs(apB));
    }

//-----------------------------------------------------------------------------
    
    @Test
    public void testSameAs_SameAPClass() throws Exception
    {
		APClass apClass = APClass.make("classA:0");
		dummyVertex.addAP(1, 2, 1, apClass);
    	dummyVertex.addAP(1, 2, 1, apClass);
    	DENOPTIMAttachmentPoint apA = dummyVertex.getAP(0);
    	DENOPTIMAttachmentPoint apB = dummyVertex.getAP(1);

    	assertTrue(apA.sameAs(apB));
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testSameAs_DiffAPClass() throws Exception
    {
    	dummyVertex.addAP(1, 2, 1, APClass.make("classA:0"));
    	dummyVertex.addAP(1, 2, 1, APClass.make("classB:0"));

		DENOPTIMAttachmentPoint apA = dummyVertex.getAP(0);
		DENOPTIMAttachmentPoint apB = dummyVertex.getAP(1);
		assertFalse(apA.sameAs(apB));
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testClone() throws Exception
    {
        dummyVertex.addAP(1, 2, 1, APClass.make(APCLASS));
        DENOPTIMAttachmentPoint orig = dummyVertex.getAP(
                dummyVertex.getNumberOfAPs()-1);
        
        DENOPTIMAttachmentPoint clone = orig.clone();

        /* This may not always work as hashing only guarantees that if
        objectA == objectB then objectA.hashCode() == objectB.hashCode(). I.e
        two objects with the same hash code need not be equal.*/
        assertEquals(clone.getAPClass().hashCode(),
                orig.getAPClass().hashCode(),"Hashcode of cloned APClass");
    }
    
//------------------------------------------------------------------------------
    
}
