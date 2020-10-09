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

import org.junit.jupiter.api.Test;

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
    	ArrayList<DENOPTIMAttachmentPoint> apsA = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	apsA.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	apsA.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	apsA.add(new DENOPTIMAttachmentPoint(2, 1, 1));
    	apsA.add(new DENOPTIMAttachmentPoint(3, 1, 1));
    	DENOPTIMVertex vA = new EmptyVertex(0, apsA);
    	
    	ArrayList<DENOPTIMAttachmentPoint> apsB = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	apsB.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	apsB.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	apsB.add(new DENOPTIMAttachmentPoint(2, 1, 1));
    	apsB.add(new DENOPTIMAttachmentPoint(3, 1, 1));
    	//NB: vertex ID must be ignores by the sameAs method
    	DENOPTIMVertex vB = new EmptyVertex(90, apsB);

    	assertTrue(vA.sameAs(vB, reason));	
    }
    
//------------------------------------------------------------------------------
	
    @Test
    public void testSameAs_DiffAPConnection() throws Exception
    {
    	ArrayList<DENOPTIMAttachmentPoint> apsA = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	apsA.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	apsA.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	apsA.add(new DENOPTIMAttachmentPoint(2, 1, 1));
    	apsA.add(new DENOPTIMAttachmentPoint(3, 1, 1));
    	DENOPTIMVertex vA = new EmptyVertex(0, apsA);
    	
    	ArrayList<DENOPTIMAttachmentPoint> apsB = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	apsB.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	apsB.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	apsB.add(new DENOPTIMAttachmentPoint(2, 1, 1));
    	apsB.add(new DENOPTIMAttachmentPoint(3, 1, 2)); //diff
    	//NB: vertex ID must be ignores by the sameAs method
    	DENOPTIMVertex vB = new EmptyVertex(90, apsB);

    	assertFalse(vA.sameAs(vB, reason));	
    }
    
//------------------------------------------------------------------------------
	
    @Test
    public void testSameAs_DiffAPNum() throws Exception
    {
    	ArrayList<DENOPTIMAttachmentPoint> apsA = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	apsA.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	apsA.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	apsA.add(new DENOPTIMAttachmentPoint(2, 1, 1));
    	apsA.add(new DENOPTIMAttachmentPoint(3, 1, 1));
    	DENOPTIMVertex vA = new EmptyVertex(0, apsA);
    	
    	ArrayList<DENOPTIMAttachmentPoint> apsB = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	apsB.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	apsB.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	apsB.add(new DENOPTIMAttachmentPoint(2, 1, 1));
    	//NB: vertex ID must be ignores by the sameAs method
    	DENOPTIMVertex vB = new EmptyVertex(90, apsB);

    	assertFalse(vA.sameAs(vB, reason));	
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testClone() throws Exception
    {
        ArrayList<DENOPTIMAttachmentPoint> apsA = 
                new ArrayList<DENOPTIMAttachmentPoint>();
        apsA.add(new DENOPTIMAttachmentPoint(1, 1, 1));
        apsA.add(new DENOPTIMAttachmentPoint(2, 2, 1));
        apsA.add(new DENOPTIMAttachmentPoint(3, 2, 1));
        DENOPTIMVertex v = new EmptyVertex(0, apsA);
        v.setLevel(26);
        
        DENOPTIMVertex c = v.clone();
        
        assertEquals(v.getVertexId(), c.getVertexId(), "Vertex ID");
        assertEquals(v.getNumberOfAP(), c.getNumberOfAP(), "Number of APS");
        assertEquals(v.getSymmetricAPSets().size(), 
                c.getSymmetricAPSets().size(), "Number of SymAPs sets");
        assertEquals(v.getLevel(), c.getLevel(), "Level");
        assertEquals(v.isRCV(), c.isRCV(), "RCV flag");
        assertNotEquals(v.hashCode(), c.hashCode(), "Hash code");       
    }
    
//------------------------------------------------------------------------------
}
