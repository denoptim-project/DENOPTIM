/*
 *   DENOPTIM
 *   Copyright (C) 2026 Marco Foscato <marco.foscato@uib.no>
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

package denoptim.integration.molgenerators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.vecmath.Point3d;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.graph.APClass;
import denoptim.graph.Candidate;
import denoptim.graph.DGraph;
import denoptim.graph.EmptyVertex;
import denoptim.graph.Fragment;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;
import denoptim.io.DenoptimIO;
import denoptim.logging.Monitor;
import denoptim.programs.denovo.GAParameters;
import denoptim.programs.fragmenter.CuttingRule;
import denoptim.programs.fragmenter.FragmenterParameters;
import denoptim.utils.MoleculeUtils;
import denoptim.utils.Randomizer;

/**
 * Unit test for {@link ReinventServerClient}.
 * 
 * @author Marco Foscato
 */

public class ReinventServerClientTest
{
    private static IChemObjectBuilder cdkBuilder = 
        DefaultChemObjectBuilder.getInstance();

    private static SmilesParser smilesParser = new SmilesParser(cdkBuilder);

//------------------------------------------------------------------------------

    @Test
    public void testGetScaffoldsSMILES_noFragmentSpace() throws Exception
    {
        ReinventServerClient client = newClient();
        GAParameters settings = new GAParameters();

        assertNull(client.getSMILESToDGraphMapForScaffolds(settings));
    }

//------------------------------------------------------------------------------

    @Test
    public void testGetScaffoldsSMILES_emptyLibrary() throws Exception
    {
        ReinventServerClient client = newClient();
        GAParameters settings = settingsWithScaffolds(new ArrayList<Vertex>());

        assertNull(client.getSMILESToDGraphMapForScaffolds(settings));
    }

//------------------------------------------------------------------------------

    @Test
    public void testGetScaffoldsSMILES_skipsScaffoldWithoutAtoms() throws Exception
    {
        ReinventServerClient client = newClient();
        EmptyVertex empty = new EmptyVertex();
        empty.setBuildingBlockType(BBType.SCAFFOLD);
        List<Vertex> scaffolds = new ArrayList<Vertex>();
        scaffolds.add(empty);
        GAParameters settings = settingsWithScaffolds(scaffolds);

        assertNull(client.getSMILESToDGraphMapForScaffolds(settings));
    }

//------------------------------------------------------------------------------

    @Test
    public void testGetScaffoldsSMILES_returnsRequestedCount() throws Exception
    {
        ReinventServerClient client = newClient();
        List<Vertex> scaffolds = new ArrayList<Vertex>();
        scaffolds.add(makeSingleAtomVertex("C", 4));
        scaffolds.add(makeSingleAtomVertex("O", 2));
        GAParameters settings = settingsWithScaffolds(scaffolds);
        settings.getRandomizer().initialiseRNG(1L);

        Map<String, DGraph> smilesDGraphMap = client.getSMILESToDGraphMapForScaffolds(settings);
        List<String> smiles = new ArrayList<>(smilesDGraphMap.keySet());

        assertNotNull(smiles);
        assertEquals(2, smiles.size());
        int nC = 0;
        int nO = 0;
        for (String one : smiles)
        {
            assertNotNull(one);
            assertFalse(one.isBlank());
            boolean containsEither = false;
            if (one.contains("C"))
            {
                nC++;
                containsEither = true;
                assertEquals(4, StringUtils.countMatches(one, "*"));
                assertEquals(1, StringUtils.countMatches(one, "C"));
            }
            if (one.contains("O"))
            {
                nO++;
                containsEither = true;
                assertEquals(2, StringUtils.countMatches(one, "*"));
                assertEquals(1, StringUtils.countMatches(one, "O"));
            }
            assertTrue(containsEither, "SMILES string contains no carbon or oxygen: " + one);
        }
        assertEquals(1, nC);
        assertEquals(1, nO);
    }

//------------------------------------------------------------------------------

    private static ReinventServerClient newClient() throws Exception
    {
        Constructor<ReinventServerClient> ctor = ReinventServerClient.class
                .getDeclaredConstructor(String.class, Integer.class);
        ctor.setAccessible(true);
        return ctor.newInstance("localhost", 1);
    }

//------------------------------------------------------------------------------

    private static GAParameters settingsWithScaffolds(List<Vertex> scaffolds)
            throws DENOPTIMException
    {
        FragmentSpaceParameters fsp = new FragmentSpaceParameters();
        new FragmentSpace(fsp,
                scaffolds,
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(),
                new HashMap<APClass,List<APClass>>(),
                new HashMap<APClass,APClass>(),
                new HashSet<APClass>(),
                new HashMap<APClass,List<APClass>>());
        GAParameters settings = new GAParameters();
        settings.setParameters(fsp);
        settings.setRandomizer(new Randomizer(1L));
        return settings;
    }

//------------------------------------------------------------------------------

    private static Vertex makeSingleAtomVertex(String elSymbol, int nAPs) throws DENOPTIMException
    {
        Fragment scaffold = new Fragment();
        IAtom atm = new Atom(elSymbol, new Point3d(new double[]{0.0, 0.0, 0.0}));
        scaffold.addAtom(atm);
        for (int i = 0; i < nAPs; i++)
        {
            scaffold.addAP(0, APClass.make("scaffold", i),
                    new Point3d(new double[]{0.0, 0.0, 1.0 + i}));
        }
        scaffold.projectAPsToProperties();
        return scaffold;
    }

//------------------------------------------------------------------------------

    @Test
    public void testCalculateNumMoleculesRequested() throws Exception
    {
        List<String> inputs = null;

        int n = 10;
        assertEquals(10, ReinventServerClient.calculateNumMoleculesRequested(inputs, n));

        inputs = new ArrayList<String>();
        assertEquals(10, ReinventServerClient.calculateNumMoleculesRequested(inputs, n));

        inputs.add("foo");
        inputs.add("bar");
        inputs.add("bar");
        assertEquals(4, ReinventServerClient.calculateNumMoleculesRequested(inputs, n));

        inputs.add("foo");
        inputs.add("bar"); // 5 input in total
        assertEquals(1, ReinventServerClient.calculateNumMoleculesRequested(inputs, 1));
    }

//------------------------------------------------------------------------------

    @Test
    public void testParseSmilesToCandidate_invalidSmiles() throws Exception
    {
        ReinventServerClient client = clientReadyToFragment();

        assertNull(client.parseSmilesToCandidate("not a smiles", null, 0));
    }

//------------------------------------------------------------------------------

    @Test
    public void testParseSmilesToCandidate_ethane() throws Exception
    {
        ReinventServerClient client = clientReadyToFragment();

        Candidate candidate = client.parseSmilesToCandidate("CC", null, 0);

        assertNotNull(candidate);
        assertNotNull(candidate.getGraph());
        // NB: there is a C-C cutting rule in action!
        assertEquals(2, candidate.getGraph().getVertexCount());
        assertEquals(1, candidate.getGraph().getEdgeCount());
        assertEquals("REINVENT_DE_NOVO", candidate.getGraph().getLocalMsg());
        assertFalse(candidate.getSmiles().isBlank());
    }

//------------------------------------------------------------------------------

    @Test
    public void testParseSmilesToCandidate_w_scaffold() throws Exception
    {
        ReinventServerClient client = clientReadyToFragment();

        // Create the scaffold graph
        DGraph scaffoldGraph = new DGraph();
        IAtomContainer scaffoldIAC = smilesParser.parseSmiles("CC1C=CC1");
        MoleculeUtils.explicitHydrogens(scaffoldIAC);
        // Replace the first H with an AP
        IAtom hAtom = null;
        IAtom cAtom = null;
        for (IAtom atom : scaffoldIAC.atoms())
        {
            if (atom.getSymbol().equals("H"))
            {
                hAtom = atom;
                cAtom = scaffoldIAC.getConnectedAtomsList(hAtom).get(0);
                break;
            }
        }
        scaffoldIAC.removeAtom(hAtom);
        Fragment scaffoldFragment = new Fragment(scaffoldIAC, BBType.SCAFFOLD);
        scaffoldFragment.addAP(scaffoldIAC.indexOf(cAtom), 
            APClass.make("scaffold", 0), 
            new Point3d(0.0, 0.0, 0.0));
        scaffoldFragment.projectAPsToProperties();
        scaffoldGraph.addVertex(scaffoldFragment);
        scaffoldGraph.addCapOnAllFreeAPs(Arrays.asList(scaffoldFragment), 
            ReinventServerClient.getAPPlaceHolder(0));

        Candidate candidate = client.parseSmilesToCandidate("CC(=O)CCC1C=CC1", scaffoldGraph, 0);

        //TODO-gg del
        FragmenterParameters frgParams = new FragmenterParameters();
        DenoptimIO.writeGraphToSDF(new File("/tmp/cand.sdf"), candidate.getGraph(), 
        false,  frgParams.getLogger(), frgParams.getRandomizer());

        assertNotNull(candidate);
        assertNotNull(candidate.getGraph());
        // NB: there is a C-C cutting rule in action!
        assertEquals(5, candidate.getGraph().getVertexCount());
        assertEquals(4, candidate.getGraph().getEdgeCount());
    }

//------------------------------------------------------------------------------

    private static ReinventServerClient clientReadyToFragment() throws Exception
    {
        ReinventServerClient client = newClient();
        GAParameters settings = new GAParameters();
        FragmentSpace fragmentSpace = new FragmentSpace();
        settings.setParameters(new FragmentSpaceParameters(fragmentSpace));

        FragmenterParameters frgParams = new FragmenterParameters();
        List<CuttingRule> rules = new ArrayList<CuttingRule>();
        rules.add(new CuttingRule("C-C", "[#6]", "[#6]", "-", 0,
                new ArrayList<String>()));
        frgParams.setCuttingRules(rules);
        settings.setParameters(frgParams);

        setField(client, "gaSettings", settings);
        setField(client, "mnt", new Monitor());
        return client;
    }

//------------------------------------------------------------------------------

    private static void setField(Object target, String name, Object value)
            throws Exception
    {
        Field field = ReinventServerClient.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

//------------------------------------------------------------------------------
}
