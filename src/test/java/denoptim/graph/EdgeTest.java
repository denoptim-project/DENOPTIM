/*
 *   DENOPTIM
 *   Copyright (C) 2022 Marco Foscato <marco.foscato@uib.no>
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

package denoptim.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import denoptim.graph.Edge.BondType;
import denoptim.graph.Vertex.BBType;

/**
 * Unit test for DENOPTIMEdge
 * 
 * @author Marco Foscato
 */

public class EdgeTest {
    private EmptyVertex dummyVertexA;
    private EmptyVertex dummyVertexB;
	private StringBuilder reason = new StringBuilder();
	private AttachmentPoint dummyApA1;
    private AttachmentPoint dummyApA2;
    private AttachmentPoint dummyApB;

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
        AttachmentPoint apA11 = vA1.getAP(0);
        
        EmptyVertex vB1 = new EmptyVertex();
        vB1.setBuildingBlockId(2);
        vB1.setBuildingBlockType(BBType.FRAGMENT);
        vB1.addAP();
        vB1.addAP();
        vB1.addAP();
        AttachmentPoint apB1 = vB1.getAP(2);
        
        EmptyVertex vA2 = new EmptyVertex();
        vA2.setBuildingBlockId(1);
        vA2.setBuildingBlockType(BBType.FRAGMENT);
        vA2.addAP();
        vA2.addAP();
        AttachmentPoint apA21 = vA2.getAP(0);
        
        EmptyVertex vB2 = new EmptyVertex();
        vB2.setBuildingBlockId(2);
        vB2.setBuildingBlockType(BBType.FRAGMENT);
        vB2.addAP();
        vB2.addAP();
        vB2.addAP();
        AttachmentPoint apB2 = vB2.getAP(2);
        
        Edge e1 = new Edge(apA11, apB1, BondType.SINGLE);
        assertEquals(0,e1.compareAsUndirected(e1),"Self-comparison");
        
        Edge e1rev = new Edge(apA21, apB2, BondType.SINGLE);
        assertEquals(0,e1.compareAsUndirected(e1rev),
                "Inverse edges should be equal (A)");
        assertEquals(0,e1rev.compareAsUndirected(e1),
                "Inverse edges should be equal (B)");
        
        
        EmptyVertex vA3a = new EmptyVertex();
        vA3a.setBuildingBlockId(1);
        vA3a.setBuildingBlockType(BBType.FRAGMENT);
        vA3a.addAP();
        vA3a.addAP();
        AttachmentPoint apA31a = vA3a.getAP(0);
        
        EmptyVertex vA3b = new EmptyVertex();
        vA3b.setBuildingBlockId(1);
        vA3b.setBuildingBlockType(BBType.FRAGMENT);
        vA3b.addAP();
        vA3b.addAP();
        AttachmentPoint apA31b = vA3b.getAP(0);
        
        EmptyVertex vA3c = new EmptyVertex();
        vA3c.setBuildingBlockId(1);
        vA3c.setBuildingBlockType(BBType.FRAGMENT);
        vA3c.addAP();
        vA3c.addAP();
        AttachmentPoint apA31c = vA3c.getAP(0);
        
        EmptyVertex vB3 = new EmptyVertex();
        vB3.setBuildingBlockId(2);
        vB3.setBuildingBlockType(BBType.FRAGMENT);
        vB3.addAP();
        vB3.addAP();
        vB3.addAP();
        AttachmentPoint apB3 = vB3.getAP(2);
        
        EmptyVertex vS = new EmptyVertex();
        vS.setBuildingBlockId(0);
        vS.setBuildingBlockType(BBType.SCAFFOLD);
        vS.addAP();
        vS.addAP();
        AttachmentPoint apS1 = vS.getAP(0);
        
        EmptyVertex vC = new EmptyVertex();
        vC.setBuildingBlockId(0);
        vC.setBuildingBlockType(BBType.CAP);
        vC.addAP();
        vC.addAP();
        AttachmentPoint apC1 = vC.getAP(0);
        
        Edge e3f = new Edge(apA31a, apB3, BondType.SINGLE);
        Edge e3s = new Edge(apA31b, apS1, BondType.SINGLE);
        Edge e3c = new Edge(apA31c, apC1, BondType.SINGLE);
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
        AttachmentPoint apA41 = vA4.getAP(0);
        AttachmentPoint apA42 = vA4.getAP(1);
        
        EmptyVertex vB4 = new EmptyVertex();
        vB4.setBuildingBlockId(2);
        vB4.setBuildingBlockType(BBType.FRAGMENT);
        vB4.addAP();
        vB4.addAP();
        vB4.addAP();
        AttachmentPoint apB41 = vB4.getAP(0);
        AttachmentPoint apB42 = vB4.getAP(2);
        
        Edge e41 = new Edge(apA41, apB41, BondType.SINGLE);
        Edge e42 = new Edge(apA42, apB42, BondType.SINGLE);
        assertEquals(-1,e41.compareAsUndirected(e42),
                "Different APs lead to different edge (A)");
        assertEquals(1,e42.compareAsUndirected(e41),
                "Different APs lead to different edge (B)");
        
        
        EmptyVertex vA5 = new EmptyVertex();
        vA5.setBuildingBlockId(3);
        vA5.setBuildingBlockType(BBType.FRAGMENT);
        vA5.addAP();
        vA5.addAP();
        AttachmentPoint apA51 = vA5.getAP(0);

        EmptyVertex vB5 = new EmptyVertex();
        vB5.setBuildingBlockId(24);
        vB5.setBuildingBlockType(BBType.FRAGMENT);
        vB5.addAP();
        vB5.addAP();
        vB5.addAP();
        AttachmentPoint apB51 = vB5.getAP(0);
        
        EmptyVertex vA6 = new EmptyVertex();
        vA6.setBuildingBlockId(3);
        vA6.setBuildingBlockType(BBType.FRAGMENT);
        vA6.addAP();
        vA6.addAP();
        AttachmentPoint apA61 = vA6.getAP(0);

        EmptyVertex vB6 = new EmptyVertex();
        vB6.setBuildingBlockId(24);
        vB6.setBuildingBlockType(BBType.FRAGMENT);
        vB6.addAP();
        vB6.addAP();
        vB6.addAP();
        AttachmentPoint apB61 = vB6.getAP(0);
        
        Edge e5 = new Edge(apA51, apB51, BondType.SINGLE);
        Edge e6 = new Edge(apA61, apB61, BondType.UNDEFINED);
        assertTrue(0 < e5.compareAsUndirected(e6),
                "Different bond types lead to different edge (A)");
        assertTrue(0 > e6.compareAsUndirected(e5),
                "Different bond types lead to different edge (B)");
    }
	
//------------------------------------------------------------------------------

    @Test
    public void testConnectionDeconnectionLoop() throws Exception {
        
        Edge e = new Edge(dummyApA1, dummyApB, BondType.NONE);
        assertEquals(e.getSrcAP().hashCode(), dummyApA1.hashCode(), 
                "SrcAP hashcode");
        assertEquals(e.getTrgAP().hashCode(), dummyApB.hashCode(), 
                "TrgAP hashcode");
        assertEquals(e.hashCode(), dummyApA1.getEdgeUser().hashCode(), 
                "Src AP user hashcode");
        assertEquals(e.hashCode(), dummyApB.getEdgeUser().hashCode(), 
                "Trg AP user hashcode");
        
        DGraph g = new DGraph();
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
	    
		Edge eA = new Edge(dummyApA1, dummyApA2,
				BondType.UNDEFINED);
		Edge eB = new Edge(dummyApA1, dummyApA2,
				BondType.UNDEFINED);

		assertTrue(eA.sameAs(eB, reason));
	}

//------------------------------------------------------------------------------

	@Test
	public void testSameAs_DiffBndTyp() throws Exception {
		Edge eA = new Edge(dummyApA1, dummyApA2,
				BondType.SINGLE);
		Edge eB = new Edge(dummyApA1, dummyApA2,
				BondType.DOUBLE);

		assertFalse(eA.sameAs(eB, reason));
	}

//------------------------------------------------------------------------------
}
