package denoptim.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import java.util.List;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.silent.Bond;

import denoptim.constants.DENOPTIMConstants;
import denoptim.graph.Edge.BondType;
import denoptim.graph.Vertex.BBType;

/**
 * Unit test for DENOPTIMAttachmentPoint
 * 
 * @author Marco Foscato
 */

public class AttachmentPointTest
{
	private final String APRULE = "MyRule";
	private final String APSUBRULE = "1";
	private final String APCLASS = APRULE
			+ DENOPTIMConstants.SEPARATORAPPROPSCL + APSUBRULE;
	private final Point3d DIRVEC = new Point3d(1.1, 2.2, 3.3);

//-----------------------------------------------------------------------------
    
    @Test
    public void testParsingofSdfAPString() throws Exception
    {
        String[] altStrings = {
                "123#myrule:0:DOUBLE:1.0%2.0%-3.0",
                "123#myrule:0:1.0%2.0%-3.0",
                "123#myrule:0:UNDEFINED",
                "123#myrule:0"};
        
        //NB: we will be changing the bond type of the static APClass
        BondType[] expectedBT = {BondType.DOUBLE,
                APClass.DEFAULTBT,
                BondType.UNDEFINED,
                APClass.DEFAULTBT};
        
        boolean[] check3d = {true, true, false, false};
        
        for (int i=0; i< altStrings.length; i++)
        {
            Object[] o = AttachmentPoint.processSdfString(altStrings[i]);
            
            assertTrue(Integer.class.isInstance(o[0]));
            int atmId = (int) o[0];
            assertEquals(122, atmId); //NB: 0-based from 1-based in SDF string
            
            assertTrue(o[1] instanceof APClass);
            APClass apc = (APClass) o[1];
            assertEquals(expectedBT[i], apc.getBondType());
            
            
            if (check3d[i])
            {
                assertTrue(o[2] instanceof Point3d);
                Point3d p3d = (Point3d)o[2];
                double thrl = 0.0001;
                assertTrue(Math.abs(p3d.x - 1.0) < thrl);
                assertTrue(Math.abs(p3d.y - 2.0) < thrl);
                assertTrue(Math.abs(p3d.z + 3.0) < thrl);
            } else {
                assertEquals(null,o[2]);
            }
        }
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testGetEmbeddedAP() throws Exception
    {   
        EmptyVertex vA1 = new EmptyVertex(0);
        vA1.addAP();
        vA1.addAP();
        vA1.addAP();
        vA1.addAP();
        vA1.addAP();
        vA1.addAP();
        vA1.addAP();
        List<AttachmentPoint> deepAPs1 = vA1.getAttachmentPoints();
        EmptyVertex vB1 = new EmptyVertex(1);
        vB1.addAP();
        
        DGraph gL01 = new DGraph();
        gL01.addVertex(vA1);
        gL01.appendVertexOnAP(vA1.getAP(0), vB1.getAP(0));
        
        Template tL01 = new Template(BBType.NONE);
        tL01.setInnerGraph(gL01);

        Vertex old1 = tL01;
        
        int[] expected1 = {6,5,4,3,2,1};
        
        Edge e1 = gL01.getEdgeAtPosition(0); //there is only 1 edge
        AttachmentPoint srcAP1 = e1.getSrcAPThroughout();
        AttachmentPoint trgAP1 = e1.getTrgAPThroughout();
        assertTrue(deepAPs1.contains(srcAP1),"srcAP is deep");
        assertTrue(trgAP1==vB1.getAP(0),"trgAP is on surface");
        checkIdentityOfEmbeddedAP(expected1[0],deepAPs1,old1);
        
        List<Vertex> addedVertexes1 = new ArrayList<Vertex>();
        addedVertexes1.add(vB1);
        for (int i=1; i<6; i++)
        {
            EmptyVertex vNew = new EmptyVertex(1);
            vNew.addAP();
            DGraph gNew = new DGraph();
            gNew.addVertex(old1);
            gNew.appendVertexOnAP(old1.getAP(0), vNew.getAP(0));
            addedVertexes1.add(vNew);
            
            e1 = gNew.getEdgeAtPosition(0); //there is only 1 edge
            srcAP1 = e1.getSrcAPThroughout();
            trgAP1 = e1.getTrgAPThroughout();
            assertTrue(deepAPs1.contains(srcAP1),"srcAP is deep");
            assertTrue(trgAP1==vNew.getAP(0),"trgAP is on surface");
            
            Template template = new Template(BBType.NONE);
            template.setInnerGraph(gNew);
            old1 = template;

            checkIdentityOfEmbeddedAP(expected1[i],deepAPs1,old1);
        }
        
        int nTotAvailAPs1 = 0;
        int correntLinks1 = 0;
        for (AttachmentPoint deepAP : vA1.getAttachmentPoints())
        {
            if (deepAP.isAvailableThroughout())
            {
                nTotAvailAPs1++;
            } else {
                Vertex linkedOwner = deepAP.getLinkedAPThroughout().getOwner();
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
        vA2.addAP();
        vA2.addAP();
        vA2.addAP();
        vA2.addAP();
        vA2.addAP();
        vA2.addAP();
        vA2.addAP();
        List<AttachmentPoint> deepAPs2 = vA2.getAttachmentPoints();
        EmptyVertex vB2 = new EmptyVertex(1);
        vB2.addAP();
        
        DGraph gL02 = new DGraph();
        
        //NB: here we pick the other vertex as source (w.r.t. gL01)
        gL02.addVertex(vB2);
        
        //NB: here is the different direction
        gL02.appendVertexOnAP(vB2.getAP(0),vA2.getAP(0));
        
        Template tL02 = new Template(BBType.NONE);
        tL02.setInnerGraph(gL02);

        Vertex old2 = tL02;
        
        int[] expected2 = {6,5,4,3,2,1};
        
        Edge e2 = gL02.getEdgeAtPosition(0); //there is only 1 edge
        AttachmentPoint srcAP2 = e2.getSrcAPThroughout();
        AttachmentPoint trgAP2 = e2.getTrgAPThroughout();
        assertTrue(deepAPs2.contains(trgAP2),"trgAP is deep");
        assertTrue(srcAP2==vB2.getAP(0),"srcAP is on surface");
        checkIdentityOfEmbeddedAP(expected2[0],deepAPs2,old2);
        
        List<Vertex> addedVertexes2 = new ArrayList<Vertex>();
        addedVertexes2.add(vB2);
        for (int i=1; i<6; i++)
        {
            EmptyVertex vNew = new EmptyVertex(1);
            vNew.addAP();
            DGraph gNew = new DGraph();
            
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
            
            Template template = new Template(BBType.NONE);
            template.setInnerGraph(gNew);
            old2 = template;

            checkIdentityOfEmbeddedAP(expected2[i],deepAPs2,old2);
        }
        
        int nTotAvailAPs = 0;
        int correntLinks = 0;
        for (AttachmentPoint deepAP : vA2.getAttachmentPoints())
        {
            if (deepAP.isAvailableThroughout())
            {
                nTotAvailAPs++;
            } else {
                Vertex linkedOwner = deepAP.getLinkedAPThroughout().getOwner();
                if (addedVertexes2.contains(linkedOwner))
                    correntLinks++;
            }
        }
        assertEquals(1,nTotAvailAPs,"total number deep available");
        assertEquals(addedVertexes2.size(),correntLinks,"number links to layers");
    }
    
//-----------------------------------------------------------------------------
    
    private void checkIdentityOfEmbeddedAP(int expectedMAtches,
            List<AttachmentPoint> deepAPs, Vertex v)
    {
        int nfound=0;
        for (AttachmentPoint outAP : v.getAttachmentPoints())
        {
            AttachmentPoint ap = outAP.getEmbeddedAP();
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
        EmptyVertex vA = new EmptyVertex(0);
        vA.addAP();
        vA.addAP();
        vA.addAP();
        vA.addAP();
        vA.addAP();
        vA.addAP();
        vA.addAP();
        EmptyVertex vB = new EmptyVertex(1);
        vB.addAP();
        vB.addAP();
        
        DGraph gL0 = new DGraph();
        gL0.addVertex(vA);
        gL0.appendVertexOnAP(vA.getAP(0), vB.getAP(1));
        
        Template tL0 = new Template(BBType.NONE);
        tL0.setInnerGraph(gL0);
        
        int[] expectedN = {6,6,6,6};
        int[] expectedNThroughout = {6,5,4,3};
        
        checkAvailNT(expectedN[0], expectedNThroughout[0], 0, vA);

        Vertex old = tL0;
        for (int i=1; i<4; i++)
        {
            EmptyVertex vNew = new EmptyVertex(1);
            vNew.addAP();
            DGraph gNew = new DGraph();
            gNew.addVertex(old);
            gNew.appendVertexOnAP(old.getAP(0), vNew.getAP(0));
            Template template = new Template(BBType.NONE);
            template.setInnerGraph(gNew);
            checkAvailNT(expectedN[i], expectedNThroughout[i], i, vA);
            old = template;
        }
	}
	
//-----------------------------------------------------------------------------
	
    private void checkAvailNT(int expN, int expNTm, int level, Vertex v)
    {
        int n = 0;
        int nThroughout = 0;
        for (AttachmentPoint apA : v.getAttachmentPoints())
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
        EmptyVertex vA = new EmptyVertex(0);
        vA.addAP();
        vA.addAP();
        vA.addAP();
        vA.addAP();
        vA.addAP();
        
        DGraph gL0 = new DGraph();
        gL0.addVertex(vA);
        Template tL0 = new Template(BBType.NONE);
        tL0.setInnerGraph(gL0);
        
        List<Vertex> newVrtxs = new ArrayList<Vertex>();
        
        Vertex old = tL0;
        for (int i=0; i<5; i++)
        {
            EmptyVertex vNew = new EmptyVertex(1);
            vNew.addAP();
            newVrtxs.add(vNew);
            DGraph gNew = new DGraph();
            gNew.addVertex(old);
            gNew.appendVertexOnAP(old.getAP(0), vNew.getAP(0));
            Template template = new Template(BBType.NONE);
            template.setInnerGraph(gNew);
            checkGetEdgeUserThroughput(vA, newVrtxs);
            old = template;
        }
    }
    
//-----------------------------------------------------------------------------
    
    private void checkGetEdgeUserThroughput(Vertex v,
            List<Vertex> onion)
    {
        int i = -1;
        for (AttachmentPoint apA : v.getAttachmentPoints())
        {
            i++;
            assertTrue(apA.isAvailable(), "APs of vA are all free within the "
                    + "graph owning vA.");
            Edge e = apA.getEdgeUserThroughout();
            if (e != null)
            {
                AttachmentPoint inAP = e.getSrcAP();
                while (true)
                {
                    Vertex src = inAP.getOwner();
                    if (src instanceof Template)
                    {
                        inAP = ((Template) src).getInnerAPFromOuterAP(
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
        DGraph g = new DGraph();
        EmptyVertex v1 = new EmptyVertex();
        v1.addAP(APClass.make(APCLASS));
        v1.addAP(APClass.make(APCLASS));
        AttachmentPoint ap1A = v1.getAP(0);
        AttachmentPoint ap1B = v1.getAP(1);
        EmptyVertex v2 = new EmptyVertex();
        v2.addAP(APClass.make(APCLASS));
        v2.addAP(APClass.make(APCLASS));
        AttachmentPoint ap2A = v2.getAP(0);
        AttachmentPoint ap2B = v2.getAP(1);
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
	    DGraph g = new DGraph();
	    EmptyVertex v1 = new EmptyVertex();
        v1.addAP(APClass.make(APCLASS));
        v1.addAP(APClass.make(APCLASS));
        AttachmentPoint ap1A = v1.getAP(0);
        AttachmentPoint ap1B = v1.getAP(1);
        EmptyVertex v2 = new EmptyVertex();
        v2.addAP(APClass.make(APCLASS));
        v2.addAP(APClass.make(APCLASS));
        AttachmentPoint ap2A = v2.getAP(0);
        AttachmentPoint ap2B = v2.getAP(1);
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
        Fragment dummyVertex = new Fragment();
        dummyVertex.addAtom(new Atom("C"));
		dummyVertex.addAP(0, DIRVEC, APClass.make(APCLASS));
		AttachmentPoint ap = dummyVertex.getAP(0);
    	
    	String str2 = ap.getSingleAPStringSDF(true);
    	AttachmentPoint ap2 = new AttachmentPoint(dummyVertex
				, str2);
    	
    	assertEquals(ap.getSingleAPStringSDF(true),
    			ap2.getSingleAPStringSDF(true));
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testConstructorsAndSDFStringNoDirVec() throws Exception
    {
        Fragment dummyVertex = new Fragment();
        dummyVertex.addAtom(new Atom("C"));
		dummyVertex.addAP(0, DIRVEC, APClass.make(APCLASS));
		AttachmentPoint ap = dummyVertex.getAP(0);

    	String str2 = ap.getSingleAPStringSDF(true);
    	AttachmentPoint ap2 = new AttachmentPoint(dummyVertex
				, str2);
    	
    	String str3 = (0+1) + DENOPTIMConstants.SEPARATORAPPROPAAP
    			+ ap.getSingleAPStringSDF(false);
    	AttachmentPoint ap3 = new AttachmentPoint(dummyVertex
				, str3);
    	
    	assertEquals(ap.getSingleAPStringSDF(true),
    			ap2.getSingleAPStringSDF(true));
    	assertEquals(ap2.toStringNoId(),ap3.toStringNoId());
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testSortAPs() throws Exception
    {
        Fragment dummyVertex = new Fragment();
        dummyVertex.addAtom(new Atom("C"));
        dummyVertex.addAtom(new Atom("C"));
        dummyVertex.addAtom(new Atom("C"));
        dummyVertex.addAtom(new Atom("C"));
        dummyVertex.addAtom(new Atom("C"));
        dummyVertex.addAtom(new Atom("C"));
        dummyVertex.addAtom(new Atom("C"));
    	dummyVertex.addAP(0);
    	dummyVertex.addAP(0);
		AttachmentPoint ap1 = dummyVertex.getAP(0);
		AttachmentPoint ap2 = dummyVertex.getAP(1);

		dummyVertex.addAP(4, DIRVEC, APClass.make("AA:0"));
		AttachmentPoint ap3 = dummyVertex.getAP(2);

		dummyVertex.addAP(4, DIRVEC, APClass.make("AA:1"));
		AttachmentPoint ap4 = dummyVertex.getAP(3);

		dummyVertex.addAP(5, new Point3d(1.1, 2.2, 3.3),
				APClass.make(APCLASS));
		AttachmentPoint ap5 = dummyVertex.getAP(4);

		dummyVertex.addAP(5, new Point3d(2.2, 2.2, 3.3),
				APClass.make(APCLASS));
		AttachmentPoint ap6 = dummyVertex.getAP(5);

    	List<AttachmentPoint> list = dummyVertex.getAttachmentPoints();
    	
    	list.sort(new AttachmentPointComparator());
    	
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
        EmptyVertex dummyVertex = new EmptyVertex();
		dummyVertex.addAP();
		dummyVertex.addAP();
		AttachmentPoint apA = dummyVertex.getAP(0);
		AttachmentPoint apB = dummyVertex.getAP(1);
		
		Vertex clone = dummyVertex.clone();
        AttachmentPoint apAc = clone.getAP(0);
        AttachmentPoint apBc = clone.getAP(1);

    	assertEquals(-1,apA.compareTo(apB),"Comparison driven by ID.");
    	assertTrue(apA.sameAs(apAc),"SameAs ignores ID.");
        assertTrue(apB.sameAs(apBc),"SameAs ignores ID (2).");
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testSameAs_DiffSrcAtm()
    {
        EmptyVertex dummyVertex = new EmptyVertex();
		dummyVertex.addAP();
		dummyVertex.addAP();
    	AttachmentPoint apA = dummyVertex.getAP(0);
    	AttachmentPoint apB = dummyVertex.getAP(1);

    	assertFalse(apA.sameAs(apB));
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testSameAs_DiffAPClass() throws Exception
    {
        EmptyVertex dummyVertex = new EmptyVertex();
    	dummyVertex.addAP(APClass.make("classA:0"));
        AttachmentPoint apA = dummyVertex.getAP(0);    	

        Vertex clone = dummyVertex.clone();
        AttachmentPoint apAc = clone.getAP(0);
        apAc.setAPClass(APClass.make("classB:0"));

		assertFalse(apA.sameAs(apAc));
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testClone() throws Exception
    {
        EmptyVertex ev = new EmptyVertex();
        ev.addAP(APClass.make(APCLASS));
        
        
        EmptyVertex ev2 = new EmptyVertex();
        ev2.addAP(APClass.make(APCLASS));
        AttachmentPoint ev2Ap = ev2.getAP(0);
        
        AttachmentPoint orig = ev.getAP(0);
        Point3d p3d = new Point3d(0.1, 0.2, -0.3);
        orig.setDirectionVector(p3d);
        
        Edge e = new Edge(ev2Ap, orig);
        
        AttachmentPoint clone = orig.clone();

        assertEquals(orig.getAPClass(),clone.getAPClass(),"APClass");
        assertEquals(orig.getAtomPositionNumber(),
                clone.getAtomPositionNumber(),"AtomPositionNumber");
        assertEquals(orig.getDirectionVector(),
                clone.getDirectionVector(),"DirectionVector");
        assertNotEquals(orig.getID(),clone.getID(),"ID");
        assertEquals(orig.getOwner(),
                clone.getOwner(),"Owner");
        assertEquals(e,orig.getEdgeUser(),"Edge user in original");
        assertEquals(null,clone.getEdgeUser(),"Edge user in clone");
        
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testHasSameSrcAtom() throws Exception
    {
        Fragment v1 = new Fragment();
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
        
        AttachmentPoint ap0 = v1.getAP(0);
        AttachmentPoint ap1 = v1.getAP(1);
        AttachmentPoint ap2 = v1.getAP(2);
        
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
        
        Fragment v2 = v1.clone();
        DGraph g2 = new DGraph();  
        g2.addVertex(v2);
        Template t2 = new Template(BBType.FRAGMENT);
        t2.setInnerGraph(g2);
        
        ap0 = t2.getAP(0);
        ap1 = t2.getAP(1);
        ap2 = t2.getAP(2);
        
        assertTrue(ap0.hasSameSrcAtom(ap1),"Intra-Template (A)");
        assertTrue(ap1.hasSameSrcAtom(ap0),"Intra-Template (B)");
        assertFalse(ap0.hasSameSrcAtom(ap2),"Intra-Template (C)");
        assertFalse(ap2.hasSameSrcAtom(ap0),"Intra-Template (D)");
    }   
    
//------------------------------------------------------------------------------
    
    @Test
    public void testHasConnectedSrcAtom() throws Exception
    {
        Fragment v1 = new Fragment();
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
        
        AttachmentPoint ap0 = v1.getAP(0);
        AttachmentPoint ap1 = v1.getAP(1);
        AttachmentPoint ap2 = v1.getAP(2);
        AttachmentPoint ap3 = v1.getAP(3);
        
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
        
        Fragment v2 = v1.clone();
        DGraph g2 = new DGraph();  
        g2.addVertex(v2);
        Template t2 = new Template(BBType.FRAGMENT);
        t2.setInnerGraph(g2);
        
        ap0 = t2.getAP(0);
        ap1 = t2.getAP(1);
        ap2 = t2.getAP(2);
        ap3 = t2.getAP(3);
        
        assertTrue(ap0.hasConnectedSrcAtom(ap2),"Intra-Template (E)");
        assertTrue(ap2.hasConnectedSrcAtom(ap0),"Intra-Template (F)");
        assertFalse(ap1.hasConnectedSrcAtom(ap3),"Intra-Template (G)");
        assertFalse(ap3.hasConnectedSrcAtom(ap1),"Intra-Template (H)");
        
        Fragment v3a = v1.clone();
        Fragment v3b = v1.clone();
        v3b.setVertexId(v3a.getVertexId()+1);
        DGraph g3 = new DGraph();  
        g3.addVertex(v3a);
        g3.addVertex(v3b);
        g3.addEdge(new Edge(v3a.getAP(1), v3b.getAP(0)));
        
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
        
        Template t3 = new Template(BBType.FRAGMENT);
        t3.setInnerGraph(g3);
        
        assertTrue(t3.getAP(0).hasConnectedSrcAtom(t3.getAP(3)), 
                "Through edge intra-template (A)");
        assertTrue(t3.getAP(0).hasConnectedSrcAtom(t3.getAP(1)), 
                "Through edge intra-template (B)");
        assertFalse(t3.getAP(0).hasConnectedSrcAtom(t3.getAP(2)), 
                "Through edge intra-template (C)");
        assertFalse(t3.getAP(0).hasConnectedSrcAtom(t3.getAP(4)), 
                "Through edge intra-template (D)");
        
        Template t4 = t2.clone();
        t4.setVertexId(t2.getVertexId()+1);
        DGraph g5 = new DGraph();
        g5.addVertex(t2);
        g5.addVertex(t4);
        g5.addEdge(new Edge(t2.getAP(1), t4.getAP(0)));
        
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
