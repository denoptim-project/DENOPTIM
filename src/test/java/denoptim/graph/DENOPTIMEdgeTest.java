package denoptim.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import denoptim.graph.DENOPTIMEdge.BondType;
import denoptim.graph.DENOPTIMVertex.BBType;

/**
 * Unit test for DENOPTIMEdge
 * 
 * @author Marco Foscato
 */

public class DENOPTIMEdgeTest {
    private EmptyVertex dummyVertexA;
    private EmptyVertex dummyVertexB;
	private StringBuilder reason = new StringBuilder();
	private DENOPTIMAttachmentPoint dummyApA1;
    private DENOPTIMAttachmentPoint dummyApA2;
    private DENOPTIMAttachmentPoint dummyApB;

//------------------------------------------------------------------------------

	@BeforeEach
	public void setUpClass() {
		dummyVertexA = new EmptyVertex();
		dummyVertexA.addAP();
		dummyVertexA.addAP();
		dummyApA1 = dummyVertexA.getAP(0);
		dummyApA2 = dummyVertexA.getAP(1);

		dummyVertexB = new EmptyVertex();
		dummyVertexB.addAP();
		dummyApB = dummyVertexB.getAP(0);
	}
	
//------------------------------------------------------------------------------

    @Test
    public void testUndirectedComparison() throws Exception {
        EmptyVertex vA1 = new EmptyVertex();
        vA1.setBuildingBlockId(1);
        vA1.setBuildingBlockType(BBType.FRAGMENT);
        vA1.addAP();
        vA1.addAP();
        DENOPTIMAttachmentPoint apA11 = vA1.getAP(0);
        
        EmptyVertex vB1 = new EmptyVertex();
        vB1.setBuildingBlockId(2);
        vB1.setBuildingBlockType(BBType.FRAGMENT);
        vB1.addAP();
        vB1.addAP();
        vB1.addAP();
        DENOPTIMAttachmentPoint apB1 = vB1.getAP(2);
        
        EmptyVertex vA2 = new EmptyVertex();
        vA2.setBuildingBlockId(1);
        vA2.setBuildingBlockType(BBType.FRAGMENT);
        vA2.addAP();
        vA2.addAP();
        DENOPTIMAttachmentPoint apA21 = vA2.getAP(0);
        
        EmptyVertex vB2 = new EmptyVertex();
        vB2.setBuildingBlockId(2);
        vB2.setBuildingBlockType(BBType.FRAGMENT);
        vB2.addAP();
        vB2.addAP();
        vB2.addAP();
        DENOPTIMAttachmentPoint apB2 = vB2.getAP(2);
        
        DENOPTIMEdge e1 = new DENOPTIMEdge(apA11, apB1, BondType.SINGLE);
        assertEquals(0,e1.compareAsUndirected(e1),"Self-comparison");
        
        DENOPTIMEdge e1rev = new DENOPTIMEdge(apA21, apB2, BondType.SINGLE);
        assertEquals(0,e1.compareAsUndirected(e1rev),
                "Inverse edges should be equal (A)");
        assertEquals(0,e1rev.compareAsUndirected(e1),
                "Inverse edges should be equal (B)");
        
        
        EmptyVertex vA3a = new EmptyVertex();
        vA3a.setBuildingBlockId(1);
        vA3a.setBuildingBlockType(BBType.FRAGMENT);
        vA3a.addAP();
        vA3a.addAP();
        DENOPTIMAttachmentPoint apA31a = vA3a.getAP(0);
        
        EmptyVertex vA3b = new EmptyVertex();
        vA3b.setBuildingBlockId(1);
        vA3b.setBuildingBlockType(BBType.FRAGMENT);
        vA3b.addAP();
        vA3b.addAP();
        DENOPTIMAttachmentPoint apA31b = vA3b.getAP(0);
        
        EmptyVertex vA3c = new EmptyVertex();
        vA3c.setBuildingBlockId(1);
        vA3c.setBuildingBlockType(BBType.FRAGMENT);
        vA3c.addAP();
        vA3c.addAP();
        DENOPTIMAttachmentPoint apA31c = vA3c.getAP(0);
        
        EmptyVertex vB3 = new EmptyVertex();
        vB3.setBuildingBlockId(2);
        vB3.setBuildingBlockType(BBType.FRAGMENT);
        vB3.addAP();
        vB3.addAP();
        vB3.addAP();
        DENOPTIMAttachmentPoint apB3 = vB3.getAP(2);
        
        EmptyVertex vS = new EmptyVertex();
        vS.setBuildingBlockId(0);
        vS.setBuildingBlockType(BBType.SCAFFOLD);
        vS.addAP();
        vS.addAP();
        DENOPTIMAttachmentPoint apS1 = vS.getAP(0);
        
        EmptyVertex vC = new EmptyVertex();
        vC.setBuildingBlockId(0);
        vC.setBuildingBlockType(BBType.CAP);
        vC.addAP();
        vC.addAP();
        DENOPTIMAttachmentPoint apC1 = vC.getAP(0);
        
        DENOPTIMEdge e3f = new DENOPTIMEdge(apA31a, apB3, BondType.SINGLE);
        DENOPTIMEdge e3s = new DENOPTIMEdge(apA31b, apS1, BondType.SINGLE);
        DENOPTIMEdge e3c = new DENOPTIMEdge(apA31c, apC1, BondType.SINGLE);
        assertEquals(1,e3f.compareAsUndirected(e3s),"Ranking (A)");
        assertEquals(-1,e3s.compareAsUndirected(e3f),"Ranking (Arev)");
        assertEquals(-1,e3f.compareAsUndirected(e3c),"Ranking (B)");
        assertEquals(1,e3c.compareAsUndirected(e3f),"Ranking (Brev)");
        assertEquals(1,e3c.compareAsUndirected(e3s),"Ranking (C)");
        assertEquals(-1,e3s.compareAsUndirected(e3c),"Ranking (Crev)");
        
        
        EmptyVertex vA4 = new EmptyVertex();
        vA4.setBuildingBlockId(1);
        vA4.setBuildingBlockType(BBType.FRAGMENT);
        vA4.addAP();
        vA4.addAP();
        DENOPTIMAttachmentPoint apA41 = vA4.getAP(0);
        DENOPTIMAttachmentPoint apA42 = vA4.getAP(1);
        
        EmptyVertex vB4 = new EmptyVertex();
        vB4.setBuildingBlockId(2);
        vB4.setBuildingBlockType(BBType.FRAGMENT);
        vB4.addAP();
        vB4.addAP();
        vB4.addAP();
        DENOPTIMAttachmentPoint apB41 = vB4.getAP(0);
        DENOPTIMAttachmentPoint apB42 = vB4.getAP(2);
        
        DENOPTIMEdge e41 = new DENOPTIMEdge(apA41, apB41, BondType.SINGLE);
        DENOPTIMEdge e42 = new DENOPTIMEdge(apA42, apB42, BondType.SINGLE);
        assertEquals(-1,e41.compareAsUndirected(e42),
                "Different APs lead to different edge (A)");
        assertEquals(1,e42.compareAsUndirected(e41),
                "Different APs lead to different edge (B)");
        
        
        EmptyVertex vA5 = new EmptyVertex();
        vA5.setBuildingBlockId(3);
        vA5.setBuildingBlockType(BBType.FRAGMENT);
        vA5.addAP();
        vA5.addAP();
        DENOPTIMAttachmentPoint apA51 = vA5.getAP(0);

        EmptyVertex vB5 = new EmptyVertex();
        vB5.setBuildingBlockId(24);
        vB5.setBuildingBlockType(BBType.FRAGMENT);
        vB5.addAP();
        vB5.addAP();
        vB5.addAP();
        DENOPTIMAttachmentPoint apB51 = vB5.getAP(0);
        
        EmptyVertex vA6 = new EmptyVertex();
        vA6.setBuildingBlockId(3);
        vA6.setBuildingBlockType(BBType.FRAGMENT);
        vA6.addAP();
        vA6.addAP();
        DENOPTIMAttachmentPoint apA61 = vA6.getAP(0);

        EmptyVertex vB6 = new EmptyVertex();
        vB6.setBuildingBlockId(24);
        vB6.setBuildingBlockType(BBType.FRAGMENT);
        vB6.addAP();
        vB6.addAP();
        vB6.addAP();
        DENOPTIMAttachmentPoint apB61 = vB6.getAP(0);
        
        DENOPTIMEdge e5 = new DENOPTIMEdge(apA51, apB51, BondType.SINGLE);
        DENOPTIMEdge e6 = new DENOPTIMEdge(apA61, apB61, BondType.UNDEFINED);
        assertTrue(0 < e5.compareAsUndirected(e6),
                "Different bond types lead to different edge (A)");
        assertTrue(0 > e6.compareAsUndirected(e5),
                "Different bond types lead to different edge (B)");
    }
	
//------------------------------------------------------------------------------

    @Test
    public void testConnectionDeconnectionLoop() throws Exception {
        
        DENOPTIMEdge e = new DENOPTIMEdge(dummyApA1, dummyApB, BondType.NONE);
        assertEquals(e.getSrcAP().hashCode(), dummyApA1.hashCode(), 
                "SrcAP hashcode");
        assertEquals(e.getTrgAP().hashCode(), dummyApB.hashCode(), 
                "TrgAP hashcode");
        assertEquals(e.hashCode(), dummyApA1.getEdgeUser().hashCode(), 
                "Src AP user hashcode");
        assertEquals(e.hashCode(), dummyApB.getEdgeUser().hashCode(), 
                "Trg AP user hashcode");
        
        DENOPTIMGraph g = new DENOPTIMGraph();
        g.addVertex(dummyVertexA);
        g.addVertex(dummyVertexB);
        g.addEdge(e);
        
        g.removeEdge(e);
        
        assertNull(dummyApA1.getEdgeUser(), "Src AP user removed");
        assertNull(dummyApB.getEdgeUser(), "Trg AP user removed");
    }
	
//------------------------------------------------------------------------------

	@Test
	public void testSameAs_Equal() throws Exception {
	    
		DENOPTIMEdge eA = new DENOPTIMEdge(dummyApA1, dummyApA2,
				BondType.UNDEFINED);
		DENOPTIMEdge eB = new DENOPTIMEdge(dummyApA1, dummyApA2,
				BondType.UNDEFINED);

		assertTrue(eA.sameAs(eB, reason));
	}

//------------------------------------------------------------------------------

	@Test
	public void testSameAs_DiffBndTyp() throws Exception {
		DENOPTIMEdge eA = new DENOPTIMEdge(dummyApA1, dummyApA2,
				BondType.SINGLE);
		DENOPTIMEdge eB = new DENOPTIMEdge(dummyApA1, dummyApA2,
				BondType.DOUBLE);

		assertFalse(eA.sameAs(eB, reason));
	}

//------------------------------------------------------------------------------
}
