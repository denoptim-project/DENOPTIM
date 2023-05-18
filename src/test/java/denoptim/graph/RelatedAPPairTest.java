package denoptim.graph;


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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class RelatedAPPairTest
{
	
//-----------------------------------------------------------------------------
    
    @Test
    public void testEquals() throws Exception
    {
        EmptyVertex v = new EmptyVertex();
        v.addAP(APClass.make("apc:0"));
        v.addAP(APClass.make("apc:0"));
        v.addAP(APClass.make("apc:1"));
        
        RelatedAPPair pA = new RelatedAPPair(v.getAP(0), v.getAP(1), "prop");
        RelatedAPPair pB = new RelatedAPPair(v.getAP(0), v.getAP(1), "prop");
        
        assertTrue(pA.equals(pA));
        assertTrue(pA.equals(pB));
        assertTrue(pB.equals(pA));
        assertFalse(pA.equals(null));
        
        pB = new RelatedAPPair(v.getAP(0), v.getAP(2), "prop");
        assertTrue(pB.equals(pB));
        assertFalse(pA.equals(pB));
        assertFalse(pB.equals(pA));
        
        pB = new RelatedAPPair(v.getAP(1), v.getAP(0), "prop");
        assertTrue(pB.equals(pB));
        assertFalse(pA.equals(pB));
        assertFalse(pB.equals(pA));
        
        pB = new RelatedAPPair(v.getAP(0), v.getAP(1), "different");
        assertTrue(pB.equals(pB));
        assertFalse(pA.equals(pB));
        assertFalse(pB.equals(pA));
    }
    
//------------------------------------------------------------------------------
    
}
