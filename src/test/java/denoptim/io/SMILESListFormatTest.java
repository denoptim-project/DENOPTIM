package denoptim.io;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.io.formats.IChemFormatMatcher.MatchResult;

/**
 * Unit test for SMILES List Format.
 * 
 * @author Marcello Costamagna
 */

public class SMILESListFormatTest 
{
    
//------------------------------------------------------------------------------
    
    @Test
    public void testMatches() throws Exception {
        List<String> lines = new ArrayList<String>();
        lines.add("blabla");
        lines.add("blabla");
        lines.add("blabla");
        
        SMILESListFormat lsf = new SMILESListFormat();
        MatchResult result = lsf.matches(lines);
        assertTrue(result.matched());
        
        lines.add("bla bla");
        result = lsf.matches(lines);
        assertFalse(result.matched());
        
        lines = new ArrayList<String>();
        lines.add(" blabla");
        lines.add("blabla");
        lines.add("blabla");
        
        result = lsf.matches(lines);
        assertFalse(result.matched());
        
        lines = new ArrayList<String>();
        lines.add("blabla");
        lines.add("blabla ");
        lines.add("blabla");
        
        result = lsf.matches(lines);
        assertFalse(result.matched());
    }
}

//------------------------------------------------------------------------------
