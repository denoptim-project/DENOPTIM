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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import denoptim.constants.DENOPTIMConstants;
import denoptim.fragspace.FragmentSpace;
import denoptim.molecule.DENOPTIMEdge.BondType;

/**
 * Unit test for DENOPTIMAttachmentPoint
 * 
 * @author Marco Foscato
 */

public class APClassTest
{
	
//-----------------------------------------------------------------------------
	
    @Test
    public void testIsValidAPSubCLassString() throws Exception
    {
        assertFalse(APClass.isValidAPSubCLassString("1.1"),"Double");
        assertFalse(APClass.isValidAPSubCLassString("-1"),"Signed(-)");
        assertFalse(APClass.isValidAPSubCLassString("+1"),"Signed(+)");
        assertFalse(APClass.isValidAPSubCLassString("1a"),"Literal");
        assertFalse(APClass.isValidAPSubCLassString("a1"),"Literal");
        assertTrue(APClass.isValidAPSubCLassString("1"),"Good one");
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testIsValidAPRuleString() throws Exception
    {
        assertFalse(APClass.isValidAPRuleString("1.1"),"Double");
        assertFalse(APClass.isValidAPRuleString("asd" 
        + DENOPTIMConstants.SEPARATORAPPROPSCL+"affg"),"With separator");
        assertFalse(APClass.isValidAPRuleString("+asd"),"Signed");
        assertFalse(APClass.isValidAPRuleString(""),"empty");
        assertFalse(APClass.isValidAPRuleString(null),"null");
        assertFalse(APClass.isValidAPRuleString("qwe et"),"space");
        assertTrue(APClass.isValidAPRuleString("qwerty"),"Good one");
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testEquals() throws Exception
    {
        APClass a = new APClass("a:1");
        APClass b = new APClass("a:1");
        assertTrue(a.equals(b));
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testCompareTo() throws Exception
    {
        APClass a = new APClass("ab:2");
        APClass b = new APClass("ab:1");
        APClass c = new APClass("dc:1");
        APClass d = new APClass("ef:0");
        List<APClass> l = new ArrayList<APClass>(Arrays.asList(d,a,b,c));
        Collections.sort(l);
        List<APClass> ref = new ArrayList<APClass>(Arrays.asList(b,a,c,d));
        for (int i=0; i<l.size(); i++)
        {
            APClass el=l.get(i);
            APClass er=ref.get(i);
            assertEquals(er,el," Entries "+er+" != "+el);
        }
    }
    
//------------------------------------------------------------------------------
}
