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

import denoptim.exception.DENOPTIMException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import denoptim.constants.DENOPTIMConstants;
import denoptim.molecule.DENOPTIMEdge.BondType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for DENOPTIMEdge
 * 
 * @author Marco Foscato
 */

public class DENOPTIMEdgeTest {
    private DENOPTIMVertex dummyVertexA;
    private DENOPTIMVertex dummyVertexB;
	private StringBuilder reason = new StringBuilder();
	private DENOPTIMAttachmentPoint dummyApA1;
    private DENOPTIMAttachmentPoint dummyApA2;
    private DENOPTIMAttachmentPoint dummyApB;
	private final String APCSEP = DENOPTIMConstants.SEPARATORAPPROPSCL;

//------------------------------------------------------------------------------

	@BeforeEach
	public void setUpClass() {
		try {
			dummyVertexA = new EmptyVertex();
			dummyVertexA.addAP();
			dummyVertexA.addAP();
			dummyApA1 = dummyVertexA.getAP(0);
			dummyApA2 = dummyVertexA.getAP(1);

			dummyVertexB = new EmptyVertex();
			dummyVertexB.addAP();
			dummyApB = dummyVertexB.getAP(0);
		} catch (DENOPTIMException e ) {
			fail("unexpected exception");
		}
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
