package denoptim.fragmenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 *   DENOPTIM
 *   Copyright (C) 2019 Marco Foscato <marco.foscato@uib.no>
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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openscience.cdk.Atom;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.Bond;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.files.FileUtils;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.fragspace.IdFragmentAndAP;
import denoptim.graph.APClass;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.DGraph;
import denoptim.graph.Edge;
import denoptim.graph.Edge.BondType;
import denoptim.graph.Fragment;
import denoptim.graph.Ring;
import denoptim.graph.SymmetricSet;
import denoptim.graph.Template;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;
import denoptim.io.DenoptimIO;
import denoptim.molecularmodeling.ThreeDimTreeBuilder;
import denoptim.programs.fragmenter.FragmenterParameters;
import denoptim.utils.GraphUtils;
import denoptim.utils.MoleculeUtils;

/**
 * Unit test for fragmenter's components.
 * 
 * @author Marco Foscato
 */

public class ParallelFragmentationAlgorithmTest
{
    private static final String SEP = System.getProperty("file.separator");
    private static final String NL = System.getProperty("line.separator");
    
    /**
     * Private builder of atom containers
     */
    private IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();

    @TempDir 
    static File tempDir;

//------------------------------------------------------------------------------
	
    @Test
	public void testSplitInputForThreads() throws Exception
	{
	    assertTrue(tempDir.isDirectory(),"Should be a directory ");
	    String structureFile = tempDir.getAbsolutePath() + SEP + "mols.sdf";
        String formulaeFile = tempDir.getAbsolutePath() + SEP + "forms.txt";

        StringBuilder formulaeTextBuilder = new StringBuilder();
        ArrayList<IAtomContainer> mols = new ArrayList<IAtomContainer>();
        for (int i=0; i<7; i++)
        {
            IAtomContainer mol = builder.newAtomContainer();
            mol.addAtom(new Atom("C"));
            mols.add(mol);
            
            formulaeTextBuilder.append("REFCODE: mol"+i).append(NL).append(NL);
            formulaeTextBuilder.append("Chemical").append(NL);
            formulaeTextBuilder.append("  Formula:       formula-"+i).append(NL);
            formulaeTextBuilder.append(NL);
        }
        
        DenoptimIO.writeSDFFile(structureFile, mols);
        DenoptimIO.writeData(formulaeFile, formulaeTextBuilder.toString(), false);
        
        FragmenterParameters settings = new FragmenterParameters();
        settings.setWorkDirectory(tempDir.getAbsolutePath());
        settings.setStructuresFile(structureFile);
        settings.setFormulaeFile(formulaeFile);
        settings.setCheckFormula(true);
        settings.setNumTasks(3);
        settings.checkParameters();
        settings.processParameters();
        
        ParallelFragmentationAlgorithm.splitInputForThreads(settings);
        
        int[] expectedEntries = {3, 2, 2};
        for (int i=0; i<3; i++)
        {
            String newStructureFile = ParallelFragmentationAlgorithm
                    .getStructureFileNameBatch(settings, i);
            File structFile = new File(newStructureFile);
            assertTrue(newStructureFile.contains(tempDir.getAbsolutePath()));
            assertTrue(structFile.exists());

            ArrayList<String> lines = DenoptimIO.readList(newStructureFile);
            int n = 0;
            for (String line : lines)
                if (line.contains((CharSequence) DENOPTIMConstants.FORMULASTR))
                    n++;
            assertEquals(expectedEntries[i],n);
        }
	}

//------------------------------------------------------------------------------
}
