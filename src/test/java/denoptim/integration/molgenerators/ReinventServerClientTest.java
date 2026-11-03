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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.vecmath.Point3d;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.interfaces.IAtom;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.graph.APClass;
import denoptim.graph.DGraph;
import denoptim.graph.EmptyVertex;
import denoptim.graph.Fragment;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;
import denoptim.programs.denovo.GAParameters;
import denoptim.utils.Randomizer;

/**
 * Unit test for {@link ReinventServerClient}.
 * 
 * @author Marco Foscato
 */

public class ReinventServerClientTest
{

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
}
