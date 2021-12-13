package denoptim.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.silent.Bond;

import denoptim.constants.DENOPTIMConstants;
import denoptim.fragspace.FragmentSpace;
import denoptim.graph.DENOPTIMEdge.BondType;
import denoptim.graph.DENOPTIMVertex.BBType;

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
	private final Point3d DIRVEC = new Point3d(1.1, 2.2, 3.3);
	private final EmptyVertex dummyVertex = new EmptyVertex();
	
//-----------------------------------------------------------------------------
    
    @Test
    public void testGetEmbeddedAP() throws Exception
    {
        // This is just to avoid the warnings about trying to get a bond type
        // when the fragment space in not defined
        HashMap<String, BondType> map = new HashMap<>();
        map.put(APRULE,BondType.SINGLE);
        FragmentSpace.setBondOrderMap(map);
        
        EmptyVertex vA1 = new EmptyVertex(0);
        vA1.addAP(0);
        vA1.addAP(1);
        vA1.addAP(2);
        vA1.addAP(3);
        vA1.addAP(4);
        vA1.addAP(5);
        vA1.addAP(6);
        ArrayList<DENOPTIMAttachmentPoint> deepAPs1 = vA1.getAttachmentPoints();
        EmptyVertex vB1 = new EmptyVertex(1);
        vB1.addAP(0);
        
        DENOPTIMGraph gL01 = new DENOPTIMGraph();
        gL01.addVertex(vA1);
        gL01.appendVertexOnAP(vA1.getAP(0), vB1.getAP(0));
        
        DENOPTIMTemplate tL01 = new DENOPTIMTemplate(BBType.NONE);
        tL01.setInnerGraph(gL01);

        DENOPTIMVertex old1 = tL01;
        
        int[] expected1 = {6,5,4,3,2,1};
        
        DENOPTIMEdge e1 = gL01.getEdgeAtPosition(0); //there is only 1 edge
        DENOPTIMAttachmentPoint srcAP1 = e1.getSrcAPThroughout();
        DENOPTIMAttachmentPoint trgAP1 = e1.getTrgAPThroughout();
        assertTrue(deepAPs1.contains(srcAP1),"srcAP is deep");
        assertTrue(trgAP1==vB1.getAP(0),"trgAP is on surface");
        checkIdentityOfEmbeddedAP(expected1[0],deepAPs1,old1);
        
        List<DENOPTIMVertex> addedVertexes1 = new ArrayList<DENOPTIMVertex>();
        addedVertexes1.add(vB1);
        for (int i=1; i<6; i++)
        {
            EmptyVertex vNew = new EmptyVertex(1);
            vNew.addAP(0);
            DENOPTIMGraph gNew = new DENOPTIMGraph();
            gNew.addVertex(old1);
            gNew.appendVertexOnAP(old1.getAP(0), vNew.getAP(0));
            addedVertexes1.add(vNew);
            
            e1 = gNew.getEdgeAtPosition(0); //there is only 1 edge
            srcAP1 = e1.getSrcAPThroughout();
            trgAP1 = e1.getTrgAPThroughout();
            assertTrue(deepAPs1.contains(srcAP1),"srcAP is deep");
            assertTrue(trgAP1==vNew.getAP(0),"trgAP is on surface");
            
            DENOPTIMTemplate template = new DENOPTIMTemplate(BBType.NONE);
            template.setInnerGraph(gNew);
            old1 = template;

            checkIdentityOfEmbeddedAP(expected1[i],deepAPs1,old1);
        }
        
        int nTotAvailAPs1 = 0;
        int correntLinks1 = 0;
        for (DENOPTIMAttachmentPoint deepAP : vA1.getAttachmentPoints())
        {
            if (deepAP.isAvailableThroughout())
            {
                nTotAvailAPs1++;
            } else {
                DENOPTIMVertex linkedOwner = deepAP.getLinkedAPThroughout().getOwner();
                if (addedVertexes1.contains(linkedOwner))
                    correntLinks1++;
            }
        }
        assertEquals(1,nTotAvailAPs1,"total number deep available");
        assertEquals(addedVertexes1.size(),correntLinks1,"number links to layers");
        
        //
        // Now we do the same by building the graph in the opposite direction
        //
        
        EmptyVertex vA2 = new EmptyVertex();
        vA2.addAP(0);
        vA2.addAP(1);
        vA2.addAP(2);
        vA2.addAP(3);
        vA2.addAP(4);
        vA2.addAP(5);
        vA2.addAP(6);
        ArrayList<DENOPTIMAttachmentPoint> deepAPs2 = vA2.getAttachmentPoints();
        EmptyVertex vB2 = new EmptyVertex(1);
        vB2.addAP(0);
        
        DENOPTIMGraph gL02 = new DENOPTIMGraph();
        
        //NB: here we pick the other vertex as source (w.r.t. gL01)
        gL02.addVertex(vB2);
        
        //NB: here is the different direction
        gL02.appendVertexOnAP(vB2.getAP(0),vA2.getAP(0));
        
        DENOPTIMTemplate tL02 = new DENOPTIMTemplate(BBType.NONE);
        tL02.setInnerGraph(gL02);

        DENOPTIMVertex old2 = tL02;
        
        int[] expected2 = {6,5,4,3,2,1};
        
        DENOPTIMEdge e2 = gL02.getEdgeAtPosition(0); //there is only 1 edge
        DENOPTIMAttachmentPoint srcAP2 = e2.getSrcAPThroughout();
        DENOPTIMAttachmentPoint trgAP2 = e2.getTrgAPThroughout();
        assertTrue(deepAPs2.contains(trgAP2),"trgAP is deep");
        assertTrue(srcAP2==vB2.getAP(0),"srcAP is on surface");
        checkIdentityOfEmbeddedAP(expected2[0],deepAPs2,old2);
        
        List<DENOPTIMVertex> addedVertexes2 = new ArrayList<DENOPTIMVertex>();
        addedVertexes2.add(vB2);
        for (int i=1; i<6; i++)
        {
            EmptyVertex vNew = new EmptyVertex(1);
            vNew.addAP(0);
            DENOPTIMGraph gNew = new DENOPTIMGraph();
            
            //NB: here we pick the other vertex as source
            gNew.addVertex(vNew);
            
            //NB: here is the different direction
            gNew.appendVertexOnAP(vNew.getAP(0), old2.getAP(0));
            addedVertexes2.add(vNew);
            
            e2 = gNew.getEdgeAtPosition(0); //there is only 1 edge
            srcAP2 = e2.getSrcAPThroughout();
            trgAP2 = e2.getTrgAPThroughout();
            assertTrue(deepAPs2.contains(trgAP2),"trgAP is deep");
            assertTrue(srcAP2==vNew.getAP(0),"srcAP is on surface");
            
            DENOPTIMTemplate template = new DENOPTIMTemplate(BBType.NONE);
            template.setInnerGraph(gNew);
            old2 = template;

            checkIdentityOfEmbeddedAP(expected2[i],deepAPs2,old2);
        }
        
        int nTotAvailAPs = 0;
        int correntLinks = 0;
        for (DENOPTIMAttachmentPoint deepAP : vA2.getAttachmentPoints())
        {
            if (deepAP.isAvailableThroughout())
            {
                nTotAvailAPs++;
            } else {
                DENOPTIMVertex linkedOwner = deepAP.getLinkedAPThroughout().getOwner();
                if (addedVertexes2.contains(linkedOwner))
                    correntLinks++;
            }
        }
        assertEquals(1,nTotAvailAPs,"total number deep available");
        assertEquals(addedVertexes2.size(),correntLinks,"number links to layers");
    }
    
//-----------------------------------------------------------------------------
    
    private void checkIdentityOfEmbeddedAP(int expectedMAtches,
            ArrayList<DENOPTIMAttachmentPoint> deepAPs, DENOPTIMVertex v)
    {
        int nfound=0;
        for (DENOPTIMAttachmentPoint outAP : v.getAttachmentPoints())
        {
            DENOPTIMAttachmentPoint ap = outAP.getEmbeddedAP();
            if (deepAPs.contains(ap))
            {
                nfound++;
                continue;
            }
        }
        assertEquals(expectedMAtches,nfound,"number of deep-traced APs");
    }
	
//-----------------------------------------------------------------------------
	
	@Test
	public void testAvailableThrougout() throws Exception
	{
        // This is just to avoid the warnings about trying to get a bond type
        // when the fragment space in not defined
        HashMap<String, BondType> map = new HashMap<>();
        map.put(APRULE,BondType.SINGLE);
        FragmentSpace.setBondOrderMap(map);
        
        EmptyVertex vA = new EmptyVertex(0);
        vA.addAP(0);
        vA.addAP(1);
        vA.addAP(2);
        vA.addAP(3);
        vA.addAP(4);
        vA.addAP(5);
        vA.addAP(6);
        EmptyVertex vB = new EmptyVertex(1);
        vB.addAP(0);
        vB.addAP(0);
        
        DENOPTIMGraph gL0 = new DENOPTIMGraph();
        gL0.addVertex(vA);
        gL0.appendVertexOnAP(vA.getAP(0), vB.getAP(1));
        
        DENOPTIMTemplate tL0 = new DENOPTIMTemplate(BBType.NONE);
        tL0.setInnerGraph(gL0);
        
        int[] expectedN = {6,6,6,6};
        int[] expectedNThroughout = {6,5,4,3};
        
        checkAvailNT(expectedN[0], expectedNThroughout[0], 0, vA);

        DENOPTIMVertex old = tL0;
        for (int i=1; i<4; i++)
        {
            EmptyVertex vNew = new EmptyVertex(1);
            vNew.addAP(0);
            DENOPTIMGraph gNew = new DENOPTIMGraph();
            gNew.addVertex(old);
            gNew.appendVertexOnAP(old.getAP(0), vNew.getAP(0));
            DENOPTIMTemplate template = new DENOPTIMTemplate(BBType.NONE);
            template.setInnerGraph(gNew);
            checkAvailNT(expectedN[i], expectedNThroughout[i], i, vA);
            old = template;
        }
	}
	
//-----------------------------------------------------------------------------
	
    private void checkAvailNT(int expN, int expNTm, int level, DENOPTIMVertex v)
    {
        int n = 0;
        int nThroughout = 0;
        for (DENOPTIMAttachmentPoint apA : v.getAttachmentPoints())
        {
            if (apA.isAvailable())
                n++;
            if (apA.isAvailableThroughout())
                nThroughout++;
        }
        assertEquals(expN,n,"Number of level-available ("+level+"");
        assertEquals(expNTm,nThroughout,"Number of throughout-available ("+level+")");
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testGetEdbeUserThrougout() throws Exception
    {
        // This is just to avoid the warnings about trying to get a bond type
        // when the fragment space in not defined
        HashMap<String, BondType> map = new HashMap<>();
        map.put(APRULE,BondType.SINGLE);
        FragmentSpace.setBondOrderMap(map);
        
        EmptyVertex vA = new EmptyVertex(0);
        vA.addAP(0);
        vA.addAP(1);
        vA.addAP(2);
        vA.addAP(3);
        vA.addAP(4);
        
        DENOPTIMGraph gL0 = new DENOPTIMGraph();
        gL0.addVertex(vA);
        DENOPTIMTemplate tL0 = new DENOPTIMTemplate(BBType.NONE);
        tL0.setInnerGraph(gL0);
        
        List<DENOPTIMVertex> newVrtxs = new ArrayList<DENOPTIMVertex>();
        
        DENOPTIMVertex old = tL0;
        for (int i=0; i<5; i++)
        {
            EmptyVertex vNew = new EmptyVertex(1);
            vNew.addAP(i);
            newVrtxs.add(vNew);
            DENOPTIMGraph gNew = new DENOPTIMGraph();
            gNew.addVertex(old);
            gNew.appendVertexOnAP(old.getAP(0), vNew.getAP(0));
            DENOPTIMTemplate template = new DENOPTIMTemplate(BBType.NONE);
            template.setInnerGraph(gNew);
            checkGetEdgeUserThroughput(vA, newVrtxs);
            old = template;
        }
    }
    
//-----------------------------------------------------------------------------
    
    private void checkGetEdgeUserThroughput(DENOPTIMVertex v,
            List<DENOPTIMVertex> onion)
    {
        int i = -1;
        for (DENOPTIMAttachmentPoint apA : v.getAttachmentPoints())
        {
            i++;
            assertTrue(apA.isAvailable(), "APs of vA are all free within the "
                    + "graph owning vA.");
            DENOPTIMEdge e = apA.getEdgeUserThroughout();
            if (e != null)
            {
                DENOPTIMAttachmentPoint inAP = e.getSrcAP();
                while (true)
                {
                    DENOPTIMVertex src = inAP.getOwner();
                    if (src instanceof DENOPTIMTemplate)
                    {
                        inAP = ((DENOPTIMTemplate) src).getInnerAPFromOuterAP(
                                inAP);
                    } else {
                        break;
                    }
                }
                assertEquals(apA,inAP,"Src AP identity");
                assertEquals(onion.get(i).getAP(0),e.getTrgAP(), "Trg AP identity");
            }
        }
    }
    
//-----------------------------------------------------------------------------
	
	@Test
	public void testIsSrcInUser() throws Exception
	{
	    // This is just to avoid the warnings about trying to get a bond type
        // when the fragment space in not defined
        HashMap<String, BondType> map = new HashMap<>();
        map.put(APRULE,BondType.SINGLE);
        FragmentSpace.setBondOrderMap(map);
        
        DENOPTIMGraph g = new DENOPTIMGraph();
        EmptyVertex v1 = new EmptyVertex();
        v1.addAP(ATMID, DIRVEC, APClass.make(APCLASS));
        v1.addAP(ATMID, DIRVEC, APClass.make(APCLASS));
        DENOPTIMAttachmentPoint ap1A = v1.getAP(0);
        DENOPTIMAttachmentPoint ap1B = v1.getAP(1);
        EmptyVertex v2 = new EmptyVertex();
        v2.addAP(ATMID, DIRVEC, APClass.make(APCLASS));
        v2.addAP(ATMID, DIRVEC, APClass.make(APCLASS));
        DENOPTIMAttachmentPoint ap2A = v2.getAP(0);
        DENOPTIMAttachmentPoint ap2B = v2.getAP(1);
        g.addVertex(v1);
        g.appendVertexOnAP(ap1A, ap2B);
        
        assertTrue(ap1A.isSrcInUser(), "Check AP used as src.");
        assertFalse(ap2B.isSrcInUser(), "Check AP used as trg.");
        assertFalse(ap1B.isSrcInUser(), "Check AP free on src side.");
        assertFalse(ap2A.isSrcInUser(), "Check AP free on trg side.");
	}

//-----------------------------------------------------------------------------
	
	@Test
	public void testGetLinkedAP() throws Exception
	{
	    // This is just to avoid the warnings about trying to get a bond type
        // when the fragment space in not defined
        HashMap<String, BondType> map = new HashMap<>();
        map.put(APRULE,BondType.SINGLE);
        FragmentSpace.setBondOrderMap(map);
        
	    DENOPTIMGraph g = new DENOPTIMGraph();
	    EmptyVertex v1 = new EmptyVertex();
        v1.addAP(ATMID, DIRVEC, APClass.make(APCLASS));
        v1.addAP(ATMID, DIRVEC, APClass.make(APCLASS));
        DENOPTIMAttachmentPoint ap1A = v1.getAP(0);
        DENOPTIMAttachmentPoint ap1B = v1.getAP(1);
        EmptyVertex v2 = new EmptyVertex();
        v2.addAP(ATMID, DIRVEC, APClass.make(APCLASS));
        v2.addAP(ATMID, DIRVEC, APClass.make(APCLASS));
        DENOPTIMAttachmentPoint ap2A = v2.getAP(0);
        DENOPTIMAttachmentPoint ap2B = v2.getAP(1);
        g.addVertex(v1);
        g.appendVertexOnAP(ap1A, ap2B);
        
        assertTrue(ap1A.getLinkedAP() == ap2B, "Get AP on other side of ap1A");
        assertTrue(ap2B.getLinkedAP() == ap1A, "Get AP on other dice of ap2");
        assertNull(ap1B.getLinkedAP(), "Free AP 1B should return null");
        assertNull(ap2A.getLinkedAP(), "Free AP 2A should return null");
	}
	
//-----------------------------------------------------------------------------
	
    @Test
    public void testConstructorsAndSDFString() throws Exception
    {
        // This is just to avoid the warnings about trying to get a bond type
        // when the fragment space in not defined
        HashMap<String, BondType> map = new HashMap<>();
        map.put(APRULE,BondType.SINGLE);
        FragmentSpace.setBondOrderMap(map);

		dummyVertex.addAP(ATMID, DIRVEC, APClass.make(APCLASS));
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
    	assertEquals(ap2.toStringNoId(),ap3.toStringNoId());
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testConstructorsAndSDFStringNoDirVec() throws Exception
    {
		dummyVertex.addAP(ATMID, DIRVEC, APClass.make(APCLASS));
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
    	assertEquals(ap2.toStringNoId(),ap3.toStringNoId());
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testSortAPs() throws Exception
    {
    	dummyVertex.addAP(0);
    	dummyVertex.addAP(0);
		DENOPTIMAttachmentPoint ap1 = dummyVertex.getAP(0);
		DENOPTIMAttachmentPoint ap2 = dummyVertex.getAP(1);

		dummyVertex.addAP(4, DIRVEC, APClass.make("AA:0"));
		DENOPTIMAttachmentPoint ap3 = dummyVertex.getAP(2);

		dummyVertex.addAP(4, DIRVEC, APClass.make("AA:1"));
		DENOPTIMAttachmentPoint ap4 = dummyVertex.getAP(3);

		dummyVertex.addAP(5, new Point3d(1.1, 2.2, 3.3),
				APClass.make(APCLASS));
		DENOPTIMAttachmentPoint ap5 = dummyVertex.getAP(4);

		dummyVertex.addAP(5, new Point3d(2.2, 2.2, 3.3),
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
		dummyVertex.addAP(1);
		dummyVertex.addAP(1);
		DENOPTIMAttachmentPoint apA = dummyVertex.getAP(0);
		DENOPTIMAttachmentPoint apB = dummyVertex.getAP(1);

    	assertEquals(-1,apA.compareTo(apB),"Comparison driven by ID.");
    	assertTrue(apA.sameAs(apB),"SameAs ignores ID.");
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testSameAs_DiffSrcAtm()
    {
		dummyVertex.addAP(1);
		dummyVertex.addAP(2);
    	DENOPTIMAttachmentPoint apA = dummyVertex.getAP(0);
    	DENOPTIMAttachmentPoint apB = dummyVertex.getAP(1);

    	assertFalse(apA.sameAs(apB));
    }

//-----------------------------------------------------------------------------
    
    @Test
    public void testSameAs_SameAPClass() throws Exception
    {
		APClass apClass = APClass.make("classA:0");
		dummyVertex.addAP(1,apClass);
    	dummyVertex.addAP(1,apClass);
    	DENOPTIMAttachmentPoint apA = dummyVertex.getAP(0);
    	DENOPTIMAttachmentPoint apB = dummyVertex.getAP(1);

    	assertTrue(apA.sameAs(apB));
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testSameAs_DiffAPClass() throws Exception
    {
    	dummyVertex.addAP(1,APClass.make("classA:0"));
    	dummyVertex.addAP(1,APClass.make("classB:0"));

		DENOPTIMAttachmentPoint apA = dummyVertex.getAP(0);
		DENOPTIMAttachmentPoint apB = dummyVertex.getAP(1);
		assertFalse(apA.sameAs(apB));
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testClone() throws Exception
    {
        dummyVertex.addAP(1,APClass.make(APCLASS));
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
    
    @Test
    public void testHasSameSrcAtom() throws Exception
    {
        // This is just to avoid the warnings about trying to get a bond type
        // when the fragment space in not defined
        HashMap<String, BondType> map = new HashMap<>();
        map.put(APRULE,BondType.SINGLE);
        FragmentSpace.setBondOrderMap(map);
        
        DENOPTIMFragment v1 = new DENOPTIMFragment();
        Atom a1 = new Atom("C");
        Atom a2 = new Atom("C");
        Atom a3 = new Atom("C");
        v1.addAtom(a1);
        v1.addAtom(a2);
        v1.addAtom(a3);
        v1.addBond(new Bond(a1, a2));
        v1.addBond(new Bond(a2, a3));
        v1.addAPOnAtom(a1, APClass.make(APCLASS), new Point3d());
        v1.addAPOnAtom(a1, APClass.make(APCLASS), new Point3d());
        v1.addAPOnAtom(a2, APClass.make(APCLASS), new Point3d());
        v1.addAPOnAtom(a3, APClass.make(APCLASS), new Point3d());
        
        DENOPTIMAttachmentPoint ap0 = v1.getAP(0);
        DENOPTIMAttachmentPoint ap1 = v1.getAP(1);
        DENOPTIMAttachmentPoint ap2 = v1.getAP(2);
        DENOPTIMAttachmentPoint ap3 = v1.getAP(3);
        
        /*
         *  ap0 
         *   \
         *   a1----a2---a3
         *   /      \    \
         *  ap1     ap2   ap3
         */
        
        assertTrue(ap0.hasSameSrcAtom(ap1),"Intra-fragment (A)");
        assertTrue(ap1.hasSameSrcAtom(ap0),"Intra-fragment (B)");
        assertFalse(ap0.hasSameSrcAtom(ap2),"Intra-fragment (C)");
        assertFalse(ap2.hasSameSrcAtom(ap0),"Intra-fragment (D)");
        
        DENOPTIMFragment v2 = v1.clone();
        DENOPTIMGraph g2 = new DENOPTIMGraph();  
        g2.addVertex(v2);
        DENOPTIMTemplate t2 = new DENOPTIMTemplate(BBType.FRAGMENT);
        t2.setInnerGraph(g2);
        
        ap0 = t2.getAP(0);
        ap1 = t2.getAP(1);
        ap2 = t2.getAP(2);
        ap3 = t2.getAP(3);
        
        assertTrue(ap0.hasSameSrcAtom(ap1),"Intra-Template (A)");
        assertTrue(ap1.hasSameSrcAtom(ap0),"Intra-Template (B)");
        assertFalse(ap0.hasSameSrcAtom(ap2),"Intra-Template (C)");
        assertFalse(ap2.hasSameSrcAtom(ap0),"Intra-Template (D)");
    }   
    
//------------------------------------------------------------------------------
    
    @Test
    public void testHasConnectedSrcAtom() throws Exception
    {
        // This is just to avoid the warnings about trying to get a bond type
        // when the fragment space in not defined
        HashMap<String, BondType> map = new HashMap<>();
        map.put(APRULE,BondType.SINGLE);
        FragmentSpace.setBondOrderMap(map);
        
        DENOPTIMFragment v1 = new DENOPTIMFragment();
        Atom a1 = new Atom("C");
        Atom a2 = new Atom("C");
        Atom a3 = new Atom("C");
        v1.addAtom(a1);
        v1.addAtom(a2);
        v1.addAtom(a3);
        v1.addBond(new Bond(a1, a2));
        v1.addBond(new Bond(a2, a3));
        v1.addAPOnAtom(a1, APClass.make(APCLASS), new Point3d());
        v1.addAPOnAtom(a1, APClass.make(APCLASS), new Point3d());
        v1.addAPOnAtom(a2, APClass.make(APCLASS), new Point3d());
        v1.addAPOnAtom(a3, APClass.make(APCLASS), new Point3d());
        
        DENOPTIMAttachmentPoint ap0 = v1.getAP(0);
        DENOPTIMAttachmentPoint ap1 = v1.getAP(1);
        DENOPTIMAttachmentPoint ap2 = v1.getAP(2);
        DENOPTIMAttachmentPoint ap3 = v1.getAP(3);
        
        /*
         *  ap0 
         *   \
         *   a1----a2---a3
         *   /      \    \
         *  ap1     ap2   ap3
         */

        assertTrue(ap0.hasConnectedSrcAtom(ap2),"Intra-fragment (E)");
        assertTrue(ap2.hasConnectedSrcAtom(ap0),"Intra-fragment (F)");
        assertFalse(ap1.hasConnectedSrcAtom(ap3),"Intra-fragment (G)");
        assertFalse(ap3.hasConnectedSrcAtom(ap1),"Intra-fragment (H)");
        
        DENOPTIMFragment v2 = v1.clone();
        DENOPTIMGraph g2 = new DENOPTIMGraph();  
        g2.addVertex(v2);
        DENOPTIMTemplate t2 = new DENOPTIMTemplate(BBType.FRAGMENT);
        t2.setInnerGraph(g2);
        
        ap0 = t2.getAP(0);
        ap1 = t2.getAP(1);
        ap2 = t2.getAP(2);
        ap3 = t2.getAP(3);
        
        assertTrue(ap0.hasConnectedSrcAtom(ap2),"Intra-Template (E)");
        assertTrue(ap2.hasConnectedSrcAtom(ap0),"Intra-Template (F)");
        assertFalse(ap1.hasConnectedSrcAtom(ap3),"Intra-Template (G)");
        assertFalse(ap3.hasConnectedSrcAtom(ap1),"Intra-Template (H)");
        
        DENOPTIMFragment v3a = v1.clone();
        DENOPTIMFragment v3b = v1.clone();
        v3b.setVertexId(v3a.getVertexId()+1);
        DENOPTIMGraph g3 = new DENOPTIMGraph();  
        g3.addVertex(v3a);
        g3.addVertex(v3b);
        g3.addEdge(new DENOPTIMEdge(v3a.getAP(1), v3b.getAP(0)));
        
        /*
         *  ap0 
         *   \
         *   a1---a2---a3          v3a
         *   /     \    \
         *  ap1    ap2   ap3
         *   |
         *  ap0 
         *   \
         *   a1---a2---a3          v3b
         *   /     \    \
         *  ap1    ap2   ap3
         *
         */
        
        assertTrue(v3a.getAP(0).hasConnectedSrcAtom(v3b.getAP(1)), 
                "Through edge (A)"); // The two APs are not in the edge
        assertFalse(v3a.getAP(1).hasConnectedSrcAtom(v3b.getAP(0)), 
                "Through edge (B)"); //The two APs are those making the edge
        
        DENOPTIMTemplate t3 = new DENOPTIMTemplate(BBType.FRAGMENT);
        t3.setInnerGraph(g3);
        
        assertTrue(t3.getAP(0).hasConnectedSrcAtom(t3.getAP(3)), 
                "Through edge intra-template (A)");
        assertTrue(t3.getAP(0).hasConnectedSrcAtom(t3.getAP(1)), 
                "Through edge intra-template (B)");
        assertFalse(t3.getAP(0).hasConnectedSrcAtom(t3.getAP(2)), 
                "Through edge intra-template (C)");
        assertFalse(t3.getAP(0).hasConnectedSrcAtom(t3.getAP(4)), 
                "Through edge intra-template (D)");
        
        DENOPTIMTemplate t4 = t2.clone();
        t4.setVertexId(t2.getVertexId()+1);
        DENOPTIMGraph g5 = new DENOPTIMGraph();
        g5.addVertex(t2);
        g5.addVertex(t4);
        g5.addEdge(new DENOPTIMEdge(t2.getAP(1), t4.getAP(0)));
        
        /*
         * {
         *  ap0 
         *   \
         *   a1---a2---a3          t2
         *   /     \    \
         *  ap1    ap2   ap3
         *  }
         *   |
         *  {
         *  ap0 
         *   \
         *   a1---a2---a3          t4
         *   /     \    \
         *  ap1    ap2   ap3
         *  }
         */
        
        assertTrue(t2.getAP(0).hasConnectedSrcAtom(t4.getAP(1)), 
                "Through deep edge (A)"); // The two APs are not in the edge
        assertFalse(t2.getAP(1).hasConnectedSrcAtom(t4.getAP(0)), 
                "Through deep edge (B)"); //The two APs are those making the edge
    }
    
//------------------------------------------------------------------------------
    
}
