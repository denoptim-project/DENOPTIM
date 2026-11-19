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

package denoptim.molecularmodeling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.exception.DENOPTIMException;
import denoptim.molecularmodeling.SmilesTo3DConverter.Engine;

/**
 * Unit tests for {@link SmilesTo3DConverter}.
 *
 * @author Marco Foscato
 */

public class SmilesTo3DConverterTest
{

//------------------------------------------------------------------------------

    @Test
    public void testSingleton()
    {
        assertSame(SmilesTo3DConverter.getInstance(),
                SmilesTo3DConverter.getInstance());
    }

//------------------------------------------------------------------------------

    @Test
    public void testSmilesTo3DMolecule() throws DENOPTIMException
    {
        IAtomContainer mol = SmilesTo3DConverter.getInstance()
                .smilesTo3DMolecule("CCO", Engine.CDK);
        assertEquals(9, mol.getAtomCount());
        assertHas3DCoordinates(mol);
    }

//------------------------------------------------------------------------------

    @Test
    public void testCactusEngine() throws Exception
    {
        IAtomContainer mol;
        try
        {
            mol = SmilesTo3DConverter.getInstance()
                    .smilesTo3DMolecule("CCO", Engine.CACTVS);
        }
        catch (DENOPTIMException e)
        {
            assumeTrue(!(causeIs(e, IOException.class)),
                    "CACTUS is not reachable: " + e.getMessage());
            throw e;
        }
        assertEquals(9, mol.getAtomCount());
        assertHas3DCoordinates(mol);
    }

//------------------------------------------------------------------------------

    @Test
    public void testConcurrentCalls() throws Exception
    {
        SmilesTo3DConverter converter = SmilesTo3DConverter.getInstance();
        String[] smiles = new String[] {"CCO", "c1ccccc1", "CC(=O)O", "CCO"};

        ExecutorService pool = Executors.newFixedThreadPool(smiles.length);
        try
        {
            List<Future<IAtomContainer>> tasks = new ArrayList<>();
            for (int i = 0; i < smiles.length; i++)
            {
                String oneSmiles = smiles[i];
                tasks.add(pool.submit(new Callable<IAtomContainer>()
                {
                    @Override
                    public IAtomContainer call() throws Exception
                    {
                        return converter.smilesTo3DMolecule(oneSmiles, Engine.CDK);
                    }
                }));
            }
            for (Future<IAtomContainer> task : tasks)
            {
                IAtomContainer mol = task.get();
                assertNotNull(mol);
                assertTrue(mol.getAtomCount() > 0);
                assertHas3DCoordinates(mol);
            }
        }
        finally
        {
            pool.shutdownNow();
        }
    }

//------------------------------------------------------------------------------

    private static boolean causeIs(Throwable error, Class<?> type)
    {
        Throwable cause = error;
        while (cause != null)
        {
            if (type.isInstance(cause))
            {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

//------------------------------------------------------------------------------

    private static void assertHas3DCoordinates(IAtomContainer mol)
    {
        for (IAtom atom : mol.atoms())
        {
            assertNotNull(atom.getPoint3d(), atom.getSymbol());
        }
    }

//------------------------------------------------------------------------------

}
