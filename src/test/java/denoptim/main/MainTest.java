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

package denoptim.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import denoptim.io.DenoptimIO;
import denoptim.logging.Version;
import denoptim.main.Main.RunType;
import denoptim.programs.RunTimeParameters.ParametersType;


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
        DenoptimIO.writeData(inputPathName, 
                ParametersType.GA_PARAMS.getKeywordRoot(), false);
        String inputPathName2 = tempDir.getAbsolutePath() + SEP + "input2.par";
        DenoptimIO.writeData(inputPathName2, 
                ParametersType.GA_PARAMS.getKeywordRoot(), false);

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
        assertTrue(b.helpMsg.startsWith(Version.VERSION), "Version msg");
        
        //
        // Testing the request for a specific type of run
        //
        b = Main.defineProgramBehavior(new String[] {
                "-"+CLIOptions.run.getOpt(), "GA", inputPathName});
        assertEquals(0, b.exitStatus, "Exit status");
        assertEquals(RunType.GA, b.runType, "Type of run");
        assertTrue(b.cmd.getArgList().contains(inputPathName),"Input file");

        b = Main.defineProgramBehavior(new String[] {
                "-"+CLIOptions.run.getOpt(), "gA", inputPathName});
        assertEquals(0, b.exitStatus, "Exit status");
        assertEquals(RunType.GA, b.runType, "Type of run");
        
        b = Main.defineProgramBehavior(new String[] { 
                "-"+CLIOptions.run.getOpt(), "GUI"});
        assertEquals(1, b.exitStatus, "Exit status");
        assertTrue(b.errorMsg.contains("not enabled from CLI"), "Error Msg");
        
        
        b = Main.defineProgramBehavior(new String[] {inputPathName, 
                "-"+CLIOptions.run.getOpt(), "FSE"});
        assertEquals(0, b.exitStatus, "Exit status");
        assertEquals(RunType.FSE, b.runType, "Type of run");
        assertTrue(b.cmd.getArgList().contains(inputPathName),"Input file");
        
        //
        // Testing the request for a specific type of run (wrong request)
        //
        b = Main.defineProgramBehavior(new String[] {
                "-"+CLIOptions.run.getOpt(), "GAG", inputPathName});
        assertEquals(1, b.exitStatus, "Exit status");
        assertTrue(b.errorMsg.contains(CLIOptions.run.getLongOpt() + " option"),
                "Illegal run type");
        
        //
        // Test unrecognized options
        //
        b = Main.defineProgramBehavior(new String[] {inputPathName, 
                "-"+CLIOptions.run.getOpt(), "FSE", "--something"});
        //System.out.println(b.helpMsg);
        //System.out.println(b.errorMsg);
        assertEquals(1, b.exitStatus, "Exit status");
        assertTrue(b.helpMsg.contains("usage:"), "Help Msg");
        assertTrue(b.errorMsg.contains("Unrecognized option"), "Error Msg");
        
        //
        // Test non-existing input file
        //
        b = Main.defineProgramBehavior(new String[] {inputPathName+"_missing"});
        assertEquals(1, b.exitStatus, "Exit status");
        assertTrue(b.errorMsg.contains("not found"), "Error Msg");
        
        b = Main.defineProgramBehavior(new String[] {inputPathName+"_missing"});
        assertEquals(1, b.exitStatus, "Exit status");
        assertTrue(b.errorMsg.contains("not found"), "Error Msg");
        
        //
        //Test multiple input files
        //
        b = Main.defineProgramBehavior(new String[] {inputPathName,
                inputPathName2});
        assertEquals(0, b.exitStatus, "Exit status");
        assertEquals(RunType.GUI, b.runType, "Type of run");
        assertTrue(b.cmd.getArgList().contains(inputPathName),"Input file");
        assertTrue(b.cmd.getArgList().contains(inputPathName2),"Input file");

    }
}
