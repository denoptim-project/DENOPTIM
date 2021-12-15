package denoptim.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import denoptim.graph.DENOPTIMEdge.BondType;
import denoptim.graph.DENOPTIMVertex.BBType;

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
        DENOPTIMAttachmentPoint apA1 = vA.getAP(0);
        DENOPTIMAttachmentPoint apA2 = vA.getAP(1);
        
        EmptyVertex vB = new EmptyVertex();
        vB.setBuildingBlockId(2);
        vB.setBuildingBlockType(BBType.FRAGMENT);
        vB.addAP();
        vB.addAP();
        vB.addAP();
        DENOPTIMAttachmentPoint apB1 = vB.getAP(1);
        DENOPTIMAttachmentPoint apB2 = vB.getAP(2);
        
        EmptyVertex vS = new EmptyVertex();
        vS.setBuildingBlockId(0);
        vS.setBuildingBlockType(BBType.SCAFFOLD);
        vS.addAP();
        vS.addAP();
        DENOPTIMAttachmentPoint apS = vS.getAP(0);
        
        EmptyVertex vC = new EmptyVertex();
        vC.setBuildingBlockId(0);
        vC.setBuildingBlockType(BBType.CAP);
        vC.addAP();
        vC.addAP();
        DENOPTIMAttachmentPoint apC = vC.getAP(0);
        

        UndirectedEdgeRelation ue1 = new UndirectedEdgeRelation(apA1, apB2, BondType.UNDEFINED);
        UndirectedEdgeRelation ue2 = new UndirectedEdgeRelation(apB2, apA1, BondType.UNDEFINED);
        assertEquals(0,ue1.compare(ue1),"Self-comparison");
        assertEquals(0,ue1.compare(ue2),"Inverse edges should be equal (A)");
        assertEquals(0,ue2.compare(ue1),"Inverse edges should be equal (B)");
        

        UndirectedEdgeRelation ues = new UndirectedEdgeRelation(apA1, apS, BondType.UNDEFINED);
        UndirectedEdgeRelation uec = new UndirectedEdgeRelation(apA1, apC, BondType.UNDEFINED);
        assertEquals(1,ue1.compare(ues),"Ranking (A)");
        assertEquals(-1,ues.compare(ue1),"Ranking (Arev)");
        assertEquals(-1,ue1.compare(uec),"Ranking (B)");
        assertEquals(1,uec.compare(ue1),"Ranking (Brev)");
        assertEquals(1,uec.compare(ues),"Ranking (C)");
        assertEquals(-1,ues.compare(uec),"Ranking (Crev)");
        
        
        UndirectedEdgeRelation ue41 = new UndirectedEdgeRelation(apA1, apB1, BondType.SINGLE);
        UndirectedEdgeRelation ue42 = new UndirectedEdgeRelation(apA2, apB2, BondType.SINGLE);
        assertEquals(-1,ue41.compare(ue42),
                "Different APs lead to different edge (A)");
        assertEquals(1,ue42.compare(ue41),
                "Different APs lead to different edge (B)");
        

        UndirectedEdgeRelation ue51 = new UndirectedEdgeRelation(apA1,apB1, BondType.SINGLE);
        UndirectedEdgeRelation ue52 = new UndirectedEdgeRelation(apA1,apB1, BondType.UNDEFINED);
        assertTrue(0 < ue51.compare(ue52),
                "Different bond types lead to different edge (A)");
        assertTrue(0 > ue52.compare(ue51),
                "Different bond types lead to different edge (B)");
    }
	
//------------------------------------------------------------------------------

}
