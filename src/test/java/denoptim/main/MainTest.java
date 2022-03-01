package denoptim.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import denoptim.io.DenoptimIO;
import denoptim.logging.Version;
import denoptim.main.Main.RunType;


public class MainTest
{
    private final String SEP = System.getProperty("file.separator");

    @TempDir
    File tempDir;
    
//------------------------------------------------------------------------------

    @Test
    public void testDefineProgramBehavior() throws Exception 
    {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory");
        String inputPathName = tempDir.getAbsolutePath() + SEP + "input.par";
        DenoptimIO.writeData(inputPathName, "data", false);

        //
        // Simplest call (launch GUI)
        //
        Behavior b = Main.defineProgramBehavior(new String[] {});
        assertEquals(0, b.exitStatus, "Exit status");
        assertEquals(RunType.GUI, b.runType, "Type of run");
        
        //
        // Single argument call with no further program run.
        //
        b = Main.defineProgramBehavior(new String[] {
                "-"+CLIOptions.help.getOpt() });
        assertEquals(0, b.exitStatus, "Exit status");
        assertTrue(b.helpMsg.contains("usage:"), "Help Msg");
        b = Main.defineProgramBehavior(new String[] {
                "--"+CLIOptions.version.getLongOpt() });
        assertEquals(0, b.exitStatus, "Exit status");
        assertTrue(b.helpMsg.startsWith("V"+Version.MAJOR), "Version msg");
        
        //
        // Testing the request for a specific type of run
        //
        b = Main.defineProgramBehavior(new String[] {
                "-"+CLIOptions.run.getOpt(), "GA", 
                "-"+CLIOptions.input.getOpt(), inputPathName});
        assertEquals(0, b.exitStatus, "Exit status");
        assertEquals(RunType.GA, b.runType, "Type of run");
        assertEquals(inputPathName, b.cmd.getOptionValue(CLIOptions.input), 
                "Parameter");

        b = Main.defineProgramBehavior(new String[] {
                "-"+CLIOptions.run.getOpt(), "gA", 
                "-"+CLIOptions.input.getOpt(), inputPathName});
        assertEquals(0, b.exitStatus, "Exit status");
        assertEquals(RunType.GA, b.runType, "Type of run");
        
        
        b = Main.defineProgramBehavior(new String[] {
                "-"+CLIOptions.input.getOpt(), inputPathName, 
                "-"+CLIOptions.run.getOpt(), "FSE"});
        assertEquals(0, b.exitStatus, "Exit status");
        assertEquals(RunType.FSE, b.runType, "Type of run");
        assertEquals(inputPathName, b.cmd.getOptionValue(CLIOptions.input), 
                "Parameter");
        
        //
        // Testing the request for a specific type of run (wrong request)
        //
        b = Main.defineProgramBehavior(new String[] {
                "-"+CLIOptions.run.getOpt(), "GAG", 
                "-"+CLIOptions.input.getOpt(), inputPathName});
        assertEquals(1, b.exitStatus, "Exit status");
        assertTrue(b.errorMsg.contains(CLIOptions.run.getLongOpt() + " option"),
                "Illegal run type");
        
        //
        // Test unrecognized options
        //
        b = Main.defineProgramBehavior(new String[] {
                "-"+CLIOptions.input.getOpt(), inputPathName, 
                "-"+CLIOptions.run.getOpt(), "FSE", "--something"});
        //System.out.println(b.helpMsg);
        //System.out.println(b.errorMsg);
        assertEquals(1, b.exitStatus, "Exit status");
        assertTrue(b.helpMsg.contains("usage:"), "Help Msg");
        assertTrue(b.errorMsg.contains("Unrecognized option"), "Error Msg");
        
        //
        // Test non-existing input file
        //
        b = Main.defineProgramBehavior(new String[] {
                "-"+CLIOptions.input.getOpt(), inputPathName+"_missing", 
                "-"+CLIOptions.run.getOpt(), "GUI"});
        //System.out.println(b.helpMsg);
        //System.out.println(b.errorMsg);
        assertEquals(1, b.exitStatus, "Exit status");
        assertTrue(b.errorMsg.contains("not found"), "Error Msg");
        
        b = Main.defineProgramBehavior(new String[] {
                "-"+CLIOptions.input.getOpt(), inputPathName+"_missing"});
        //System.out.println(b.helpMsg);
        //System.out.println(b.errorMsg);
        assertEquals(1, b.exitStatus, "Exit status");
        assertTrue(b.errorMsg.contains("not found"), "Error Msg");
        
    }
}
