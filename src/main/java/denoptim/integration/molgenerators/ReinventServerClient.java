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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import denoptim.exception.DENOPTIMException;
import denoptim.ga.EAUtils;
import denoptim.graph.Candidate;
import denoptim.io.DenoptimIO;
import denoptim.logging.Monitor;
import denoptim.programs.denovo.GAParameters;
import denoptim.utils.Randomizer;


/**
 * Sends the molecular generation requests to a socket server running the Reinvent service.
 * This class follows the singleton pattern.
 */
public class ReinventServerClient
{
    /**
     * Singleton instance
     */
    private static ReinventServerClient instance = null;
    
    /**
     * Version identifier
     */
    //TODO-ai: version is needed to exclude incompatibility between Server and Client
    private final int version = 1;
    
    /**
     * The name of the host or ID address used to communicate with the socket
     * server.
     */
    private String hostname;
    
    /**
     * The identifier of the port used to communicate with the socket server.
     */
    private Integer port;

    /**
     * Converter to and from JSON/Java objects. We use a singleton pattern.
     */
    private Gson jsonConverter = new GsonBuilder().create();

    /**
     * Pathname to file where to record requests sent to the server.
     * If <code>null</code>, no file is written.
     */
    private String requestFileName = null;

    /**
     * Randomizer to control pseudo-number generation at the Rainvent service
     */
    private Randomizer randomizer;

    /**
     * Utility for constructing CDK objects
     */
    private static IChemObjectBuilder cdkBuilder = 
        DefaultChemObjectBuilder.getInstance();

    /**
     * Smiles parser to parse SMILES strings to IAtomContainer objects.
     */
    private static SmilesParser smilesParser = new SmilesParser(cdkBuilder);

    /**
     * Monitor to count events related to ægs run.
     */
    private Monitor mnt;

    /**
     * GA parameters to control the generation of molecules
     */
    private GAParameters gaSettings;

    /**
     * Template configuration for de novo molecualr generation (i.e., unbiased
     * generation of molecules)
     */
    private JsonObject deNovoConfig;

//------------------------------------------------------------------------------
    
    /**
     * Private constructor for singleton pattern.
     */
    private ReinventServerClient(String hostname, Integer port) {
        this.hostname = hostname;
        this.port = port;
    }

//------------------------------------------------------------------------------

    /**
     * Sets the pathname to file where to record requests sent to the server.
     * @param requestFileName The pathname to file
     */
    public void setRecordRequestsFileName(String requestFileName) {
        this.requestFileName = requestFileName;
    }

//------------------------------------------------------------------------------

    /**
     * Gets the singleton instance of RCOSocketServerClient.
     * @param hostname The hostname or IP address of the socket server
     * @param port The port number of the socket server
     * @param randomizer The randomizer to control pseudo-number generation at the Reinvent service
     * @return The singleton instance
     */
    public static synchronized ReinventServerClient getInstance(GAParameters gaSettings, Monitor mnt)
    {
        if (instance == null) {
            instance = new ReinventServerClient(
                gaSettings.getAIEngineServerHostname(), 
                gaSettings.getAIEngineServerPort());
            instance.randomizer = gaSettings.getRandomizer();
            instance.gaSettings = gaSettings;
            instance.deNovoConfig = readReinventConfigurationFile(
                gaSettings.getPathnameReinventDeNovoConfig());
            instance.mnt = mnt;
        }
        return instance;
    }

//------------------------------------------------------------------------------

    /**
     * Parses the Reinvent de novo configuration file and returns a JSON object.
     * The file follows the structure of REINVENT4 configuration file, i.e., 
     * contains a JSON object with properties like 
     * <code>run_type</code>, <code>device</code>, <code>seed</code>, and a 
     * nested <code>parameters</code> object with properties like 
     * <code>num_smiles</code>, <code>model_file</code>, and <code>smiles_file</code>, etc.
     * @param pathname the pathname to the Reinvent de novo configuration file.
     * @return the JSON object representing the configuration.
     */
    private static JsonObject readReinventConfigurationFile(String pathname) 
    {
        try (BufferedReader reader = new BufferedReader(new FileReader(pathname)))
        {
            JsonObject config = new Gson().fromJson(reader, JsonObject.class);
            if (config == null)
            {
                throw new IllegalArgumentException(
                    "Reinvent de novo configuration file is empty: " + pathname);
            }
            return config;
        } catch (IOException | JsonSyntaxException e) {
            throw new IllegalArgumentException(
                "Failed to parse Reinvent de novo configuration file: " 
                + pathname, e);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Gets the singleton instance of {@link ReinventServerClient} 
     * if it has been initialized.
     * @return The singleton instance
     * @throws IllegalStateException if the instance has not been initialized 
     * yet.
     */
    public static ReinventServerClient getInstance() 
    {
        if (instance == null) {
            throw new IllegalStateException(
                    "ReinventServerClient has not been initialized. "
                    + "Call getInstance(hostname, port) first.");
        }
        return instance;
    }

//------------------------------------------------------------------------------

    /**
     * Sets the hostname for the socket server connection.
     * @param hostname The hostname or IP address
     */
    public void setHostname(String hostname) 
    {
        this.hostname = hostname;
    }

//------------------------------------------------------------------------------

    /**
     * Sets the port for the socket server connection.
     * @param port The port number
     */
    public void setPort(Integer port) {
        this.port = port;
    }

//------------------------------------------------------------------------------

    /**
     * Gets the currently configured hostname.
     * @return The hostname
     */
    public String getHostname() {
        return hostname;
    }

//------------------------------------------------------------------------------

    /**
     * Gets the currently configured port.
     * @return The port number
     */
    public Integer getPort() {
        return port;
    }

//------------------------------------------------------------------------------

    /**
     * Generate molecules from scratch.
     */
    public List<Candidate> generateCandidates(List<String> inputs, int n, Logger logger)
                    throws IOException, JsonSyntaxException, DENOPTIMException
    {
        String requestAsJSONString = formulateRequest(inputs, n);
        
        //This might be useful for debugging to get the actual request placed to the server
        logger.log(Level.FINE, "Request to the AI Generator socket server: " + requestAsJSONString);
        if (requestFileName != null) {
            DenoptimIO.writeData(requestFileName, 
                requestAsJSONString+System.lineSeparator(), true);
        }

        /*
        Expected answer structure:

        {
            "status": "SUCCESS",
            "molecules": [
                {
                "SMILES": "O=C(Nc1c(O)ccc(C(=O)O)c1O)c1ccccc1O",
                "SMILES_state": "1",
                "Scaffold": "[*]Nc1c(O)c(C(O)=O)ccc1O",
                "R-groups": "c1ccc(C(=O)*)c(O)c1",
                "NLL": "7.25"
                },
                ...
            ]
        }
        
        */
        
        JsonObject answer = sendRequest(requestAsJSONString);
        for (String requiredMember : new String[] {"status", "molecules"})
        {
            if (!answer.has(requiredMember))
            {
                throw new Error("AI Generator socket server replied without "
                        + "including '" + requiredMember + "' member. " 
                        + "Aborting! " + answer.toString());
            }
        }

        JsonArray molecules = answer.get("molecules").getAsJsonArray();
        List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < molecules.size(); i++)
        {
            JsonObject molecule = molecules.get(i).getAsJsonObject();
            String smiles = molecule.get("SMILES").getAsString();
            
            Candidate candidate = parseSmilesToCandidate(smiles, i);
            if (candidate != null) {
                candidates.add(candidate);
            }
        }
        
        return candidates;
    }

//------------------------------------------------------------------------------

    /**
     * Converts a SMILES string to a {@link Candidate} object.
     * @param smiles The SMILES string
     * @param index The index of the candidate in the list of candidates. 
     * This is only meant for logging.
     * @return The {@link Candidate} object
     */
    private Candidate parseSmilesToCandidate(String smiles, int index) 
    {
        IAtomContainer iac = null;
        try {
            iac = smilesParser.parseSmiles(smiles);
        } catch (InvalidSmilesException e) {
            // Skipp results that cannot be converted for whatever reason
            return null;
        }

        if (iac == null) {
            return null;
        }

        //TODO-ai consider counting events in monitor
        try {
            Candidate candidate = EAUtils.buildCandidateByFragmentingMolecule(iac, 
                mnt, gaSettings, index);
            if (candidate != null) {
                candidate.getGraph().setLocalMsg("REINVENT_DE_NOVO");
            }
            return candidate;
        } catch (DENOPTIMException e) {
            return null;
        }
    }

//------------------------------------------------------------------------------

    /**
     * Formulates the request to be sent to the Reinvent socket server.
     * @param scaffolds input strings based on SMILES with attachment points, but 
     * that may include two warheads in case of linker generation.
     * @param n the number of candidates to generate.
     * @return The request as a JSON string.
     */
    public String formulateRequest(List<String> inputs, 
        int n) throws IOException
    {    
        JsonObject jsonObj = deNovoConfig == null
                ? new JsonObject()
                : deNovoConfig.deepCopy();

        // The seed for the random number generator: used it for ensuring reproducibility
        overwriteProperty(jsonObj, new JsonPrimitive(randomizer.nextInt(1000000)),
                "seed");

        // The number of new molecules to generate. This replaces parameters.num_smiles
        // when that element is already present and leaves the other parameters in place.
        overwriteProperty(jsonObj, new JsonPrimitive(n), "parameters", "num_smiles");
        
        if (inputs != null)
        {
            JsonArray inputsArray = new JsonArray();
            for (String oneInput : inputs)
            {
                inputsArray.add(oneInput);
            }
            jsonObj.add("input_strings", inputsArray);
        }
        return jsonConverter.toJson(jsonObj);
    }

//------------------------------------------------------------------------------

    /**
     * Replaces the element addressed by {@code path} inside {@code root}.
     * The path may have any length, so the element can sit at any nesting
     * level. Intermediate objects that already exist are reused, and missing
     * ones are created. The element itself is replaced as a whole.
     * @param root the object that contains the element.
     * @param value the new value. {@code null} is stored as JSON null.
     * @param path property names from {@code root} down to the element.
     */
    private static void overwriteProperty(JsonObject root, JsonElement value,
            String... path)
    {
        if (path == null || path.length == 0)
        {
            throw new IllegalArgumentException("Property path is empty");
        }
        JsonObject current = root;
        for (int i = 0; i < path.length - 1; i++)
        {
            String key = path[i];
            JsonElement child = current.get(key);
            if (child != null && child.isJsonObject())
            {
                current = child.getAsJsonObject();
            } else {
                JsonObject created = new JsonObject();
                current.add(key, created);
                current = created;
            }
        }
        current.add(path[path.length - 1],
                value == null ? JsonNull.INSTANCE : value);
    }

//------------------------------------------------------------------------------

    /**
     * Sends the given request to the socket server and waits for the answer, 
     * which is then processed and returned.
     * @param requestAsJSONString the request as a JSON string.
     * @return the answer from the socket server as a JSON object.
     * @throws IOException if an I/O error occurs
     */
    public JsonObject sendRequest(String requestAsJSONString) 
            throws IOException, JsonSyntaxException
    {        
        Socket socket;
        try
        {
            socket = new Socket(hostname, port);
        } catch (IOException e1)
        {
            throw new IllegalArgumentException("Could not connect to socket",e1);
        }
        
        Runtime.getRuntime().addShutdownHook(new Thread(){public void run(){
            try {
                socket.close();
            } catch (IOException e) { /* failed */ }
        }});
        
        PrintWriter writerToSocket;
        try
        {
            OutputStream outputSocket = socket.getOutputStream();
            writerToSocket = new PrintWriter(outputSocket, true);
        } catch (IOException e1)
        {
            try
            {
                socket.close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
            throw new IllegalArgumentException("Could not connect to socket",e1);
        }
        
        BufferedReader readerFromSocket;
        try
        {
            InputStream inputFromSocket = socket.getInputStream();
            readerFromSocket = new BufferedReader(
                    new InputStreamReader(inputFromSocket));
        } catch (IOException e1)
        {
            try
            {
                socket.close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
            throw new IllegalArgumentException("Could not read from socket",e1);
        }

        // Here we send the request to the socket
        writerToSocket.println(requestAsJSONString);
        try
        {
            socket.shutdownOutput();
        } catch (IOException e1)
        {
            try
            {
                socket.close();
            } catch (IOException e)
            {
                // At this point the socket is probably closed already...
            }
            throw new IllegalStateException("Could not half-close socket from "
                    + this.getClass().getName(),e1);
        }
        
        // Read and process the answer from the socket
        JsonObject answer = null;
        try {
            answer = jsonConverter.fromJson(readerFromSocket.readLine(), 
                    JsonObject.class);
            if (!(answer.has("status")))
            {
                throw new Error("Socket server replied without "
                        + "including " + "status" + " member. " 
                        + "Something is badly wrong: aborting! " + answer.toString());
            }
            if (!answer.get("status").getAsString().equals("SUCCESS"))
            {
                throw new Error("Socket server replied but with status=" 
                        + answer.get("status") + ". " 
                        + "Something is badly wrong: aborting! " + answer.toString());
            }
        } catch (JsonSyntaxException e) {
            throw new Error("Socket server replied with invalid JSON: " + e.getMessage());
        } catch (IOException e) {
            throw new Error("Error reading from socket: " + e.getMessage());
        }
        
        try
        {
            socket.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return answer;
    }

//------------------------------------------------------------------------------

}
