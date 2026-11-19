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

package denoptim.integration.rdkit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.io.IChemObjectReader;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import denoptim.exception.DENOPTIMException;


/**
 * Client for the RDKit socket server in {@code src/main/python/denoptim_rdkit}.
 * Sending a request is independent of which task is requested. Each public
 * method is one task: it fills in that task's parameters and reads that
 * task's result. Further tasks are further methods that call
 * {@link #submit(String, JsonObject)}.
 * <p>
 * One call opens one connection, writes one JSON line, and reads one JSON
 * line. Calls from several threads do not share a connection.
 *
 * @author Marco Foscato
 */
public class RdkitServerClient
{
    /**
     * Default hostname for the RDKit server
     */
    public static final String DEFAULT_HOST = "127.0.0.1";

    /**
     * Default port for the RDKit server
     */
    public static final int DEFAULT_PORT = 56100;

    /**
     * Server address.
     */
    private final String hostname;

    /**
     * Server port.
     */
    private final int port;

    /**
     * How long one call waits for the server, in milliseconds.
     */
    private static final int SOCKET_TIMEOUT_MS = 120000;

    /**
     * Name of the task result member
     */
    private static final String RESULTS_MEMBER_NAME = "results";

    /**
     * Name of the task to build a 3D model from a {@code SMILES} string.
     */
    private static final String TASK_SMILES_TO_3D = "smiles_to_3d";

    private final Gson json = new GsonBuilder().create();

    private final IChemObjectBuilder builder =
            SilentChemObjectBuilder.getInstance();

//------------------------------------------------------------------------------

    /**
     * Constructor.
     */
    public RdkitServerClient()
    {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }

//------------------------------------------------------------------------------

    /**
     * Constructor.
     * @param hostname server address.
     * @param port server port.
     */
    public RdkitServerClient(String hostname, int port)
    {
        this.hostname = hostname;
        this.port = port;
    }

//------------------------------------------------------------------------------

    /**
     * Builds one 3D model for a {@code SMILES} string. Explicit hydrogens added 
     * by RDKit. Every atom in the returned container
     * carries a {@code Point3d}.
     *
     * @param smiles the {@code SMILES} string to convert.
     * @param randomSeed seed for the RDKit embedding. The same seed and
     * SMILES produce the same conformation.
     * @return the molecule with 3D coordinates.
     * @throws DENOPTIMException when {@code smiles} is empty, the server
     * cannot be reached, or RDKit cannot build the 3D model.
     */
    public IAtomContainer smilesTo3DMolecule(String smiles, int randomSeed)
            throws DENOPTIMException
    {
        if (smiles == null || smiles.isBlank())
        {
            throw new DENOPTIMException("SMILES string is empty.");
        }
        JsonObject parameters = new JsonObject();
        parameters.addProperty("smiles", smiles);
        parameters.addProperty("randomSeed", randomSeed);
        JsonObject result = submit(TASK_SMILES_TO_3D, parameters);
        if (result == null || !result.has(RESULTS_MEMBER_NAME)
                || !result.get(RESULTS_MEMBER_NAME).isJsonPrimitive())
        {
            throw new DENOPTIMException("RDKit task '" + TASK_SMILES_TO_3D
                    + "' returned no results.");
        }
        return readMolblock(result.get(RESULTS_MEMBER_NAME).getAsString(), smiles);
    }

//------------------------------------------------------------------------------

    /**
     * Sends one task to the server and returns its {@code result} object.
     * The connection, the JSON envelope, and the status check are the same
     * for every task.
     *
     * @param task name registered by the Python server.
     * @param parameters task arguments. The server receives this object as
     * the {@code parameters} member. May be empty, not {@code null}.
     * @return the {@code result} member of a successful response.
     * @throws DENOPTIMException when the server cannot be used or reports an
     * error.
     */
    JsonObject submit(String task, JsonObject parameters)
            throws DENOPTIMException
    {
        JsonObject request = new JsonObject();
        request.addProperty("task", task);
        request.add("parameters", parameters);

        String responseLine;
        try (Socket socket = new Socket())
        {
            socket.connect(new InetSocketAddress(hostname, port),
                    SOCKET_TIMEOUT_MS);
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true,
                    StandardCharsets.UTF_8);
            writer.println(json.toJson(request));
            socket.shutdownOutput();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), StandardCharsets.UTF_8));
            responseLine = reader.readLine();
        }
        catch (IOException e)
        {
            throw new DENOPTIMException("Could not complete RDKit task '"
                    + task + "' at " + hostname + ":" + port + ".", e);
        }

        if (responseLine == null || responseLine.isBlank())
        {
            throw new DENOPTIMException("RDKit server closed the connection "
                    + "without a response to task '" + task + "'.");
        }

        JsonObject response;
        try
        {
            response = json.fromJson(responseLine, JsonObject.class);
        }
        catch (JsonSyntaxException e)
        {
            throw new DENOPTIMException("RDKit server returned invalid JSON "
                    + "for task '" + task + "'.", e);
        }
        if (response == null || !response.has("status")
                || !response.get("status").isJsonPrimitive())
        {
            throw new DENOPTIMException("RDKit server response to task '"
                    + task + "' has no status.");
        }
        String status = response.get("status").getAsString();
        if ("ERROR".equals(status))
        {
            String message = response.has("message")
                    ? response.get("message").getAsString()
                    : "RDKit task '" + task + "' failed.";
            throw new DENOPTIMException(message);
        }
        if (!"SUCCESS".equals(status))
        {
            throw new DENOPTIMException("RDKit server returned status '"
                    + status + "' for task '" + task + "'.");
        }
        if (!response.has("result") || !response.get("result").isJsonObject())
        {
            throw new DENOPTIMException("RDKit task '" + task
                    + "' succeeded without a result object.");
        }
        return response.getAsJsonObject("result");
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
            throw new DENOPTIMException("Could not read the 3D model RDKit "
                    + "built for SMILES '" + smiles + "'.", e);
        }
    }

//------------------------------------------------------------------------------

}
