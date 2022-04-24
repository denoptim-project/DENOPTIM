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

package denoptim.fitness;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit test for descriptor utils.
 * 
 * @author Marco Foscato
 */

public class DescriptorUtilsTest
{

//------------------------------------------------------------------------------
    
    @Test
    public void testFindDemoptimDescriptors() throws Exception
    {
        List<String> l = DescriptorUtils.getClassNamesToDenoptimDescriptors();
        assertTrue(l.size() > 0,"Denoptim classes found");
    }
	
//------------------------------------------------------------------------------
		
	@Test
	public void testFindCDKDescriptors() throws Exception
	{
		List<String> list = DescriptorUtils.getClassNamesToCDKDescriptors();
		assertTrue(list.size() > 0,"CDK classes found");
	}
 
//------------------------------------------------------------------------------

}
