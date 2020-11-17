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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;

/**
 * Unit test for APClass
 * 
 * @author Marco Foscato
 */

public class APClassTest
{
//-----------------------------------------------------------------------------
    
    @Test
    public void testConstructor() throws Exception
    {
        assertThrows(DENOPTIMException.class, () -> {APClass.make("r7: :f9");}, 
                "Wrong syntax for whole APClass string");
        assertThrows(DENOPTIMException.class, () -> {APClass.make("r ",0);}, 
                "Wrong syntax for 'rule' string");
    }
    
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
        APClass a = APClass.make("a:1");
        APClass b = APClass.make("a:1");
        assertTrue(a.equals(b));
        assertTrue(a==b,"a==b operator");
        
        APClass c = APClass.make("a",1);
        assertTrue(a.equals(c));
        assertTrue(c.equals(b));
        assertTrue(a==c,"a==c operator");
        assertTrue(c==b,"c==b operator");
        
        APClass d = APClass.make("a:0");
        APClass e = APClass.make("b:1");
        assertFalse(a.equals(d));
        assertFalse(a.equals(e));
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testListContains() throws Exception
    {
        APClass a = APClass.make("a:1");
        APClass b = APClass.make("a:1");
        APClass c = APClass.make("a",1);
        APClass d = a;
        
        List<APClass> list = new ArrayList<APClass>();
        list.add(a);
        assertTrue(list.contains(a),"List contains A");
        assertTrue(list.contains(d),"List contains B");
        assertTrue(list.contains(c),"List contains C");
        assertTrue(list.contains(b),"List contains D");
        
        Map<APClass,APClass> map = new HashMap<APClass,APClass>();
        map.put(a,b);
        map.put(c,d);
        assertTrue(map.containsKey(a),"Map contains A");
        assertTrue(map.containsKey(b),"Map contains B");
        assertTrue(map.containsKey(c),"Map contains C");
        assertTrue(map.containsKey(d),"Map contains D");
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testCompareTo() throws Exception
    {
        APClass a = APClass.make("ab:2");
        APClass b = APClass.make("ab:1");
        APClass c = APClass.make("dc:1");
        APClass d = APClass.make("ef:0");
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
