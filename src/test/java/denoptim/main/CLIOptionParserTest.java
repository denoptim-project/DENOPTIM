package denoptim.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.cli.CommandLine;
import org.junit.jupiter.api.Test;

public class CLIOptionParserTest
{
    @Test
    public void testCLIOptionParser() throws Exception 
    {
        CLIOptionParser parser = new CLIOptionParser();
        String[] args = new String[] {};
        CommandLine cmd = parser.parse(CLIOptions.getInstance(), args);
        assertEquals(0,cmd.getArgs().length,"Size of empty command");
        
        // Test single argument
        String filename = "path/filename.dat";
        args = new String[] {filename};
        cmd = parser.parse(CLIOptions.getInstance(), args);
        assertTrue(cmd.hasOption(CLIOptions.input),"Simplified input option");
        
        // Test first argument simplified
        args = new String[] {filename, "-"+CLIOptions.run.getOpt(), "GA"};
        cmd = parser.parse(CLIOptions.getInstance(), args);
        assertTrue(cmd.hasOption(CLIOptions.input),"Simplified input option");
        assertEquals(filename,cmd.getOptionValue(CLIOptions.input),
                "Value of simplified input option");
        assertTrue(cmd.hasOption(CLIOptions.run),"Retain other options");
        assertEquals("GA",cmd.getOptionValue(CLIOptions.run),
                "Value of retained otheroption");
        
        // Test without simplification
        args = new String[] {"-"+CLIOptions.run.getOpt(), "GA", 
                "-"+CLIOptions.input.getOpt(), filename};
        cmd = parser.parse(CLIOptions.getInstance(), args);
        assertTrue(cmd.hasOption(CLIOptions.input),"Standard input option");
        assertEquals(filename,cmd.getOptionValue(CLIOptions.input),
                "Value of standard input option");
        assertTrue(cmd.hasOption(CLIOptions.run),"Standard other options");
        assertEquals("GA",cmd.getOptionValue(CLIOptions.run),
                "Value of Standard other option");
    }
}
