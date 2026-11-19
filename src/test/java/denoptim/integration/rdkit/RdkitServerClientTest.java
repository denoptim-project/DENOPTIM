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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import denoptim.exception.DENOPTIMException;

/**
 * Tests for {@link RdkitServerClient}. The protocol tests use a stand-in
 * socket. The integration test starts the real {@code denoptim_rdkit} server
 * with the Python from the {@code dnp_devel} conda environment.
 *
 * @author Marco Foscato
 */
public class RdkitServerClientTest
{
    private static final String MOLBLOCK =
            "\n"
            + "     RDKit          3D\n"
            + "\n"
            + "  3  2  0  0  0  0  0  0  0  0999 V2000\n"
            + "    0.0000    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
            + "    1.5400    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
            + "   -0.5100    1.3100    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n"
            + "  1  2  1  0  0  0  0\n"
            + "  1  3  1  0  0  0  0\n"
            + "M  END\n";

//------------------------------------------------------------------------------

    @Test
    public void testSmilesTo3DMolecule() throws Exception
    {
        try (ServerSocket server = new ServerSocket(0))
        {
            Thread responder = startResponder(server, request -> {
                assertEquals("smiles_to_3d", request.get("task").getAsString());
                assertEquals("CCO", request.getAsJsonObject("parameters")
                        .get("smiles").getAsString());
                assertEquals(17, request.getAsJsonObject("parameters")
                        .get("randomSeed").getAsInt());
                JsonObject result = new JsonObject();
                result.addProperty("results", MOLBLOCK);
                JsonObject response = new JsonObject();
                response.addProperty("status", "SUCCESS");
                response.add("result", result);
                return response.toString();
            });

            RdkitServerClient client = new RdkitServerClient("127.0.0.1",
                    server.getLocalPort());
            IAtomContainer mol = client.smilesTo3DMolecule("CCO", 17);
            responder.join(5000);

            assertEquals(3, mol.getAtomCount());
            for (IAtom atom : mol.atoms())
            {
                assertNotNull(atom.getPoint3d(), atom.getSymbol());
            }
        }
    }

//------------------------------------------------------------------------------

    @Test
    public void testServerError() throws Exception
    {
        try (ServerSocket server = new ServerSocket(0))
        {
            Thread responder = startResponder(server, request ->
                    "{\"status\":\"ERROR\",\"message\":\"could not embed\"}");

            RdkitServerClient client = new RdkitServerClient("127.0.0.1",
                    server.getLocalPort());
            DENOPTIMException error = assertThrows(DENOPTIMException.class,
                    () -> client.smilesTo3DMolecule("CCO", 17));
            responder.join(5000);

            assertTrue(error.getMessage().contains("could not embed"));
        }
    }

//------------------------------------------------------------------------------

    @Test
    public void testSmilesTo3DMoleculeWithRdkitServer() throws Exception
    {
        Path python = dnpDevelPython();
        assumeTrue(python != null,
                "conda environment dnp_devel was not found");
        Path pythonPath = Path.of("src", "main", "python").toAbsolutePath();
        assumeTrue(Files.isDirectory(pythonPath.resolve("denoptim_rdkit")),
                "RDKit server sources were not found at " + pythonPath);

        int port;
        try (ServerSocket probe = new ServerSocket(0))
        {
            port = probe.getLocalPort();
        }
        ProcessBuilder builder = new ProcessBuilder(python.toString(), "-m",
                "denoptim_rdkit", "--host", "127.0.0.1", "--port",
                Integer.toString(port));
        builder.environment().put("PYTHONPATH", pythonPath.toString());
        builder.redirectErrorStream(true);
        Process server = builder.start();
        StringBuilder serverLog = new StringBuilder();
        Thread logReader = new Thread(() -> appendStream(server.getInputStream(),
                serverLog));
        logReader.setDaemon(true);
        logReader.start();
        try
        {
            assertTrue(waitUntilAccepting(server, port),
                    "RDKit server did not accept connections. " + serverLog);
            RdkitServerClient client = new RdkitServerClient("127.0.0.1", port);
            IAtomContainer mol = client.smilesTo3DMolecule("CCO", 17);
            assertEquals(9, mol.getAtomCount());
            for (IAtom atom : mol.atoms())
            {
                assertNotNull(atom.getPoint3d(), atom.getSymbol());
            }
        }
        finally
        {
            server.destroy();
            server.waitFor(5, TimeUnit.SECONDS);
            if (server.isAlive())
            {
                server.destroyForcibly();
            }
        }
    }

//------------------------------------------------------------------------------

    @FunctionalInterface
    private interface Reply
    {
        String apply(JsonObject request);
    }

//------------------------------------------------------------------------------

    private static Thread startResponder(ServerSocket server, Reply reply)
    {
        Thread thread = new Thread(() -> {
            try (Socket socket = server.accept();
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(),
                                    StandardCharsets.UTF_8));
                    PrintWriter writer = new PrintWriter(socket.getOutputStream(),
                            true, StandardCharsets.UTF_8))
            {
                JsonObject request = JsonParser.parseString(reader.readLine())
                        .getAsJsonObject();
                writer.println(reply.apply(request));
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });
        thread.start();
        return thread;
    }

//------------------------------------------------------------------------------

    /**
     * @return the Python executable of the {@code dnp_devel} conda environment,
     * or {@code null} when that environment cannot be found.
     */
    private static Path dnpDevelPython() throws InterruptedException
    {
        List<Path> bases = new ArrayList<>();
        String prefix = System.getenv("CONDA_PREFIX");
        if (prefix != null && !prefix.isBlank())
        {
            Path condaPrefix = Path.of(prefix);
            if ("dnp_devel".equals(String.valueOf(condaPrefix.getFileName())))
            {
                Path python = pythonExecutable(condaPrefix);
                if (python != null)
                {
                    return python;
                }
            }
            if (condaPrefix.getParent() != null
                    && "envs".equals(String.valueOf(
                            condaPrefix.getParent().getFileName())))
            {
                bases.add(condaPrefix.getParent().getParent());
            }
            else
            {
                bases.add(condaPrefix);
            }
        }
        Path fromConda = condaBase();
        if (fromConda != null)
        {
            bases.add(fromConda);
        }
        Path home = Path.of(System.getProperty("user.home"));
        for (String root : new String[] {"miniforge3", "mambaforge",
                "miniconda3", "anaconda3", "opt/anaconda3"})
        {
            bases.add(home.resolve(root));
        }
        for (Path base : bases)
        {
            if (base == null)
            {
                continue;
            }
            Path python = pythonExecutable(base.resolve("envs")
                    .resolve("dnp_devel"));
            if (python != null)
            {
                return python;
            }
        }
        return null;
    }

//------------------------------------------------------------------------------

    private static Path pythonExecutable(Path environment)
    {
        Path unix = environment.resolve("bin").resolve("python");
        if (Files.isExecutable(unix))
        {
            return unix;
        }
        Path windows = environment.resolve("python.exe");
        if (Files.isExecutable(windows))
        {
            return windows;
        }
        return null;
    }

//------------------------------------------------------------------------------

    private static Path condaBase() throws InterruptedException
    {
        String conda = System.getenv("CONDA_EXE");
        List<String> command = new ArrayList<>();
        if (conda != null && !conda.isBlank())
        {
            command.add(conda);
        }
        else
        {
            command.add("conda");
        }
        command.add("info");
        command.add("--base");
        Process process;
        try
        {
            process = new ProcessBuilder(command).redirectErrorStream(true)
                    .start();
        }
        catch (IOException e)
        {
            return null;
        }
        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                process.getInputStream(), StandardCharsets.UTF_8)))
        {
            output = reader.readLine();
        }
        catch (IOException e)
        {
            return null;
        }
        if (!process.waitFor(10, TimeUnit.SECONDS) || process.exitValue() != 0
                || output == null || output.isBlank())
        {
            return null;
        }
        return Path.of(output.trim());
    }

//------------------------------------------------------------------------------

    private static boolean waitUntilAccepting(Process server, int port)
            throws InterruptedException
    {
        long deadline = System.currentTimeMillis() + 60000;
        while (System.currentTimeMillis() < deadline)
        {
            if (!server.isAlive())
            {
                return false;
            }
            try (Socket socket = new Socket())
            {
                socket.connect(new InetSocketAddress("127.0.0.1", port), 200);
                return true;
            }
            catch (IOException ignored)
            {
                Thread.sleep(100);
            }
        }
        return false;
    }

//------------------------------------------------------------------------------

    private static void appendStream(InputStream input, StringBuilder sink)
    {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                input, StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                sink.append(line).append(System.lineSeparator());
            }
        }
        catch (IOException ignored)
        {
        }
    }

//------------------------------------------------------------------------------

}
