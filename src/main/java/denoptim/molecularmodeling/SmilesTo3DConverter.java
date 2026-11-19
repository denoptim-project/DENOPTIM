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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.io.IChemObjectReader;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.modeling.builder3d.ModelBuilder3D;
import org.openscience.cdk.modeling.builder3d.TemplateHandler3D;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import denoptim.exception.DENOPTIMException;
import denoptim.integration.rdkit.RdkitServerClient;
import denoptim.utils.MoleculeUtils;


/**
 * Converts a SMILES string into one three-dimensional molecular model.
 * The single instance can be called from several threads.
 * CDK engine keeps one
 * shared 3D builder, template handler, atom-type matcher, and hydrogen adder,
 * and {@link SmilesParser} is not safe for overlapping parses, so each
 * CDK conversion runs to completion before the next one starts.
 * The CACTVS engine sends one HTTP request per call to
 * {@code https://cactus.nci.nih.gov} and does not use that lock.
 *
 * @author Marco Foscato
 */
public class SmilesTo3DConverter
{
    /**
     * Generator used to embed the SMILES in 3D.
     */
    public enum Engine
    {
        /**
         * Use RDKit via the {@link RdkitServerClient}.
         */
        RDKIT,

        /**
         * CDK engines uses {@link ModelBuilder3D} to make **ONLY** a 3D guess.
         */
        CDK,

        /**
         * NCI Chemical Identifier Resolver at
         * {@code https://cactus.nci.nih.gov}. Needs a network connection.
         * This is the same request Jmol makes for {@code load $SMILES}.
         */
        CACTVS;
    }

    /**
     * Resolver request used by Jmol's {@code load $} command.
     * {@code %s} is the SMILES with the characters Jmol encodes replaced.
     */
    private static final String CACTUS_URL =
            "https://cactus.nci.nih.gov/chemical/structure/%s/file?format=sdf&get3d=true";

    /**
     * How long one CACTVS request waits for the resolver, in milliseconds.
     */
    private static final int CACTUS_TIMEOUT_MS = 30000;

    /**
     * The only instance of this class.
     */
    private static final SmilesTo3DConverter INSTANCE = new SmilesTo3DConverter();

    /**
     * Serializes use of CDK engine objects that are shared for the whole process.
     */
    private final Object cdkEngineLock = new Object();

    /**
     * Builder of the atom containers returned to callers.
     */
    private final IChemObjectBuilder builder =
            SilentChemObjectBuilder.getInstance();

    private SmilesTo3DConverter()
    {
    }

//------------------------------------------------------------------------------

    /**
     * @return the single instance of this class.
     */
    public static SmilesTo3DConverter getInstance()
    {
        return INSTANCE;
    }

//------------------------------------------------------------------------------

    /**
     * Builds a 3D model of the molecule described by {@code smiles}.
     * The returned container is a new object on every call. Its atoms,
     * including explicit hydrogens, carry {@code Point3d} coordinates.
     * Isomeric SMILES selects the stereoisomer; a SMILES without stereo
     * still receives one 3D embedding. The SMILES must describe one
     * connected component.
     *
     * @param smiles the molecule to convert.
     * @param method the 3D generator to use.
     * @return the molecule with 3D coordinates.
     * @throws DENOPTIMException when {@code smiles} or {@code method} is
     * missing, or when the chosen generator cannot build the model.
     */
    public IAtomContainer smilesTo3DMolecule(String smiles, Engine engine)
            throws DENOPTIMException
    {
        return smilesTo3DMolecule(smiles, engine, 12345);
    }

//------------------------------------------------------------------------------

    /**
     * Builds a 3D model of the molecule described by {@code smiles}.
     * The returned container is a new object on every call. Its atoms,
     * including explicit hydrogens, carry {@code Point3d} coordinates.
     * Isomeric SMILES selects the stereoisomer; a SMILES without stereo
     * still receives one 3D embedding. The SMILES must describe one
     * connected component.
     *
     * @param smiles the molecule to convert.
     * @param method the 3D generator to use.
     * @return the molecule with 3D coordinates.
     * @throws DENOPTIMException when {@code smiles} or {@code method} is
     * missing, or when the chosen generator cannot build the model.
     */
    public IAtomContainer smilesTo3DMolecule(String smiles, Engine engine,
        int randomSeed) throws DENOPTIMException
    {
        if (smiles == null || smiles.isBlank())
        {
            throw new DENOPTIMException("SMILES string is empty.");
        }
        if (engine == null)
        {
            throw new DENOPTIMException("3D generation method is null.");
        }
        
        switch (engine)
        {
            case RDKIT:
                return smilesTo3DByRDKitEngine(smiles, randomSeed);
            case CDK:
                return smilesTo3DByCDKEngine(smiles);
            case CACTVS:
                return smilesTo3DByCactus(smiles);
            default:
                throw new DENOPTIMException("Unknown 3D generation engine: "
                        + engine);
        }
    }

//------------------------------------------------------------------------------

    private IAtomContainer smilesTo3DByRDKitEngine(String smiles, int randomSeed)
        throws DENOPTIMException
    {
        RdkitServerClient client = new RdkitServerClient();
        IAtomContainer mol;
        try
        {
            mol = client.smilesTo3DMolecule(smiles, randomSeed);
        } catch (DENOPTIMException e) {
            throw new DENOPTIMException("Failed to build a 3D model from "
                    + "SMILES '" + smiles + "' with engine RDKit.", e);
        }
        return mol;
    }

//------------------------------------------------------------------------------

    private IAtomContainer smilesTo3DByCDKEngine(String smiles)
        throws DENOPTIMException
    {
        synchronized (cdkEngineLock)
        {
            try
            {
                //
                // WARNING! No energy minimazion or what-sp-ever refining of the
                // geometry. This produced only a 3D guess geometry!
                //
                SmilesParser smilesParser = new SmilesParser(builder);
                IAtomContainer mol = smilesParser.parseSmiles(smiles);
                AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
                CDKHydrogenAdder.getInstance(builder).addImplicitHydrogens(mol);
                AtomContainerManipulator.convertImplicitToExplicitHydrogens(mol);

                ModelBuilder3D mb3d = ModelBuilder3D.getInstance(
                        TemplateHandler3D.getInstance(),
                        "mmff94",
                        builder);
                mol = mb3d.generate3DCoordinates(mol, false);
                for (IAtom atom : mol.atoms())
                {
                    if (atom.getPoint3d() == null)
                    {
                        throw new DENOPTIMException("3D generation with CDK engine "
                                + "left atom " + MoleculeUtils.getAtomRef(atom, mol)
                                + " from SMILES '"
                                + smiles + "' without 3D coordinates.");
                    }
                }
                return mol;
            }
            catch (DENOPTIMException e)
            {
                throw e;
            }
            catch (CDKException | CloneNotSupportedException | IOException e)
            {
                throw new DENOPTIMException("Failed to build a 3D model from "
                        + "SMILES '" + smiles + "' with engine CDK engine.", e);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Asks {@code https://cactus.nci.nih.gov} for one 3D SDF. The characters
     * encoded in the path are the same set Jmol encodes for {@code load $}.
     */
    private IAtomContainer smilesTo3DByCactus(String smiles)
            throws DENOPTIMException
    {
        String url = String.format(CACTUS_URL, escapeForCactus(smiles));
        HttpURLConnection connection = null;
        try
        {
            connection = (HttpURLConnection) URI.create(url).toURL()
                    .openConnection();
            connection.setConnectTimeout(CACTUS_TIMEOUT_MS);
            connection.setReadTimeout(CACTUS_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(true);
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK)
            {
                throw new DENOPTIMException("CACTUS returned HTTP " + status
                        + " for SMILES '" + smiles + "'.");
            }
            String molblock;
            try (InputStream input = connection.getInputStream())
            {
                molblock = new String(input.readAllBytes(),
                        StandardCharsets.UTF_8);
            }
            if (!molblock.contains("V2000"))
            {
                throw new DENOPTIMException("CACTUS returned no 3D model for "
                        + "SMILES '" + smiles + "'.");
            }
            IAtomContainer mol = readMolblock(molblock, smiles);
            for (IAtom atom : mol.atoms())
            {
                if (atom.getPoint3d() == null)
                {
                    throw new DENOPTIMException("CACTUS left atom "
                            + MoleculeUtils.getAtomRef(atom, mol)
                            + " from SMILES '" + smiles
                            + "' without 3D coordinates.");
                }
            }
            return mol;
        }
        catch (DENOPTIMException e)
        {
            throw e;
        }
        catch (IOException | IllegalArgumentException e)
        {
            throw new DENOPTIMException("Could not reach "
                    + "https://cactus.nci.nih.gov for SMILES '" + smiles
                    + "'.", e);
        }
        finally
        {
            if (connection != null)
            {
                connection.disconnect();
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Encodes the characters Jmol's {@code PT.escapeUrl} encodes. Forward
     * slashes stay as path separators because the resolver rejects
     * {@code %2F}.
     */
    private static String escapeForCactus(String smiles)
    {
        return smiles.replace("\n", "")
                .replace("%", "%25")
                .replace("#", "%23")
                .replace("[", "%5B")
                .replace("\\", "%5C")
                .replace("]", "%5D")
                .replace(" ", "%20");
    }

//------------------------------------------------------------------------------

    private IAtomContainer readMolblock(String molblock, String smiles)
            throws DENOPTIMException
    {
        try (MDLV2000Reader reader = new MDLV2000Reader(
                new StringReader(molblock), IChemObjectReader.Mode.RELAXED))
        {
            return reader.read(builder.newAtomContainer());
        }
        catch (CDKException | IOException e)
        {
            throw new DENOPTIMException("Could not read the 3D model CACTUS "
                    + "returned for SMILES '" + smiles + "'.", e);
        }
    }

//------------------------------------------------------------------------------

}
