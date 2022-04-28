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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import denoptim.graph.Edge.BondType;
import denoptim.graph.Vertex.BBType;
import denoptim.graph.simplified.UndirectedEdge;

/**
 * Unit test for UndirectedEdge
 */

public class UndirectedEdgeRelationTest {
	
//------------------------------------------------------------------------------

    @Test
    public void testUndirectedComparison() throws Exception {
        EmptyVertex vA = new EmptyVertex();
        vA.setBuildingBlockId(1);
        vA.setBuildingBlockType(BBType.FRAGMENT);
        vA.addAP();
        vA.addAP();
        AttachmentPoint apA1 = vA.getAP(0);
        AttachmentPoint apA2 = vA.getAP(1);
        
        EmptyVertex vB = new EmptyVertex();
        vB.setBuildingBlockId(2);
        vB.setBuildingBlockType(BBType.FRAGMENT);
        vB.addAP();
        vB.addAP();
        vB.addAP();
        AttachmentPoint apB1 = vB.getAP(1);
        AttachmentPoint apB2 = vB.getAP(2);
        
        EmptyVertex vS = new EmptyVertex();
        vS.setBuildingBlockId(0);
        vS.setBuildingBlockType(BBType.SCAFFOLD);
        vS.addAP();
        vS.addAP();
        AttachmentPoint apS = vS.getAP(0);
        
        EmptyVertex vC = new EmptyVertex();
        vC.setBuildingBlockId(0);
        vC.setBuildingBlockType(BBType.CAP);
        vC.addAP();
        vC.addAP();
        AttachmentPoint apC = vC.getAP(0);
        

        UndirectedEdge ue1 = new UndirectedEdge(apA1, apB2, BondType.UNDEFINED);
        UndirectedEdge ue2 = new UndirectedEdge(apB2, apA1, BondType.UNDEFINED);
        assertEquals(0,ue1.compare(ue1),"Self-comparison");
        assertEquals(0,ue1.compare(ue2),"Inverse edges should be equal (A)");
        assertEquals(0,ue2.compare(ue1),"Inverse edges should be equal (B)");
        

        UndirectedEdge ues = new UndirectedEdge(apA1, apS, BondType.UNDEFINED);
        UndirectedEdge uec = new UndirectedEdge(apA1, apC, BondType.UNDEFINED);
        assertEquals(1,ue1.compare(ues),"Ranking (A)");
        assertEquals(-1,ues.compare(ue1),"Ranking (Arev)");
        assertEquals(-1,ue1.compare(uec),"Ranking (B)");
        assertEquals(1,uec.compare(ue1),"Ranking (Brev)");
        assertEquals(1,uec.compare(ues),"Ranking (C)");
        assertEquals(-1,ues.compare(uec),"Ranking (Crev)");
        
        
        UndirectedEdge ue41 = new UndirectedEdge(apA1, apB1, BondType.SINGLE);
        UndirectedEdge ue42 = new UndirectedEdge(apA2, apB2, BondType.SINGLE);
        assertEquals(-1,ue41.compare(ue42),
                "Different APs lead to different edge (A)");
        assertEquals(1,ue42.compare(ue41),
                "Different APs lead to different edge (B)");
        

        UndirectedEdge ue51 = new UndirectedEdge(apA1,apB1, BondType.SINGLE);
        UndirectedEdge ue52 = new UndirectedEdge(apA1,apB1, BondType.UNDEFINED);
        assertTrue(0 < ue51.compare(ue52),
                "Different bond types lead to different edge (A)");
        assertTrue(0 > ue52.compare(ue51),
                "Different bond types lead to different edge (B)");
    }
	
//------------------------------------------------------------------------------

}
