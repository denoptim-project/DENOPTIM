package denoptim.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import denoptim.io.DenoptimIO;
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
        // Testing the request for a specific type of run
        //
        b = Main.defineProgramBehavior(new String[] {
                "-r", "GA", "-f", inputPathName});
        assertEquals(0, b.exitStatus, "Exit status");
        assertEquals(RunType.GA, b.runType, "Type of run");
        assertEquals(inputPathName, b.params[3], "Parameter");
        
        b = Main.defineProgramBehavior(new String[] {
                "-f", inputPathName, "-r", "FSE"});
        assertEquals(0, b.exitStatus, "Exit status");
        assertEquals(RunType.FSE, b.runType, "Type of run");
        assertEquals(inputPathName, b.params[1], "Parameter");
        
        //
        // Test unrecognized options
        //
        b = Main.defineProgramBehavior(new String[] {
                "-f", inputPathName, "-r", "FSE", "-something"});
        //System.out.println(b.helpMsg);
        //System.out.println(b.errorMsg);
        assertEquals(1, b.exitStatus, "Exit status");
        assertTrue(b.helpMsg.contains("usage:"), "Help Msg");
        assertTrue(b.errorMsg.contains("Unrecognized option"), "Error Msg");
        
        //
        // Test non-existing input file
        //
        b = Main.defineProgramBehavior(new String[] {
                "-f", inputPathName+"_missing", "-r", "GUI"});
        //System.out.println(b.helpMsg);
        //System.out.println(b.errorMsg);
        assertEquals(1, b.exitStatus, "Exit status");
        assertTrue(b.errorMsg.contains("not found"), "Error Msg");
        
        b = Main.defineProgramBehavior(new String[] {
                "-f", inputPathName+"_missing"});
        //System.out.println(b.helpMsg);
        //System.out.println(b.errorMsg);
        assertEquals(1, b.exitStatus, "Exit status");
        assertTrue(b.errorMsg.contains("not found"), "Error Msg");
        
    }
}
