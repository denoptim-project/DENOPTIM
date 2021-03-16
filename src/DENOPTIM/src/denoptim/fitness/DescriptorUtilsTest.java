package denoptim.fitness;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit test for descriptor utils
 * 
 * @author Marco Foscato
 */

public class DescriptorUtilsTest
{
	
//------------------------------------------------------------------------------
		
	@Test
	public void testFindCDK() throws Exception
	{
		List<String> list = DescriptorUtils.getClassNamesToCDKDescriptors();
		assertTrue(list.size() > 0,"CDK classes found");
	}
 
//------------------------------------------------------------------------------

}
