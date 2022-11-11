package denoptim.io;

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openscience.cdk.Atom;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.io.formats.IChemFormatMatcher.MatchResult;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.files.FileFormat;
import denoptim.files.FileUtils;
import denoptim.files.UndetectedFileFormatException;
import denoptim.ga.EAUtils;
import denoptim.ga.Population;
import denoptim.graph.APClass;
import denoptim.graph.Candidate;
import denoptim.graph.CandidateLW;
import denoptim.graph.DGraph;
import denoptim.graph.Edge;
import denoptim.graph.Edge.BondType;
import denoptim.graph.EmptyVertex;
import denoptim.graph.Fragment;
import denoptim.graph.FragmentTest;
import denoptim.graph.Ring;
import denoptim.graph.SymmetricSet;
import denoptim.graph.Template;
import denoptim.graph.TemplateTest;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;
import denoptim.programs.RunTimeParameters.ParametersType;
import denoptim.programs.denovo.GAParameters;
import denoptim.programs.fragmenter.CuttingRule;

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
