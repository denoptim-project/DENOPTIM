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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.vecmath.Point3d;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
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
import denoptim.files.FileFormat;
import denoptim.fragmenter.FragmenterTools;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.ga.EAUtils;
import denoptim.graph.APClass;
import denoptim.graph.APMapping;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.AttachmentPointQuery;
import denoptim.graph.Candidate;
import denoptim.graph.DGraph;
import denoptim.graph.Fragment;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;
import denoptim.io.DenoptimIO;
import denoptim.logging.Monitor;
import denoptim.molecularmodeling.ThreeDimTreeBuilder;
import denoptim.programs.RunTimeParameters.ParametersType;
import denoptim.programs.denovo.GAParameters;
import denoptim.programs.fragmenter.FragmenterParameters;
import denoptim.utils.GraphUtils;
import denoptim.utils.MoleculeUtils;
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
     * Smiles generator to generate SMILES strings.
     */
    private static final SmilesGenerator CANONICAL_SMGEN = new SmilesGenerator(
        SmiFlavor.Canonical);

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

    /**
     * APClass for placeholder atoms
     */
    private static String APClassPlaceholderName = "PlaceholderAPForReinvent"; 

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
    public List<Candidate> generateCandidates(Map<String, DGraph> smilesDGraphMap, int n)
        throws IOException, JsonSyntaxException, DENOPTIMException
    {
        List<String> inputSmiles = new ArrayList<>(smilesDGraphMap.keySet());

        // Reinvent returns scaffolds SMILES that re not the same as those given as input.
        // So we generate acanonical SMILES to ude it a key
        Map<String, DGraph> canonicalSmilesDGraphMap = new HashMap<>();
        for (Map.Entry<String, DGraph> e : smilesDGraphMap.entrySet())
        {
            String scaffSMI = e.getKey();
            String scaffKey = getCanonicalSMILES(scaffSMI);
            canonicalSmilesDGraphMap.put(scaffKey, e.getValue());
        }

        String requestAsJSONString = formulateRequest(inputSmiles, n);
        
        //This might be useful for debugging to get the actual request placed to the server
        gaSettings.getLogger().log(Level.FINE, 
            "Request to the AI Generator socket server: " + requestAsJSONString);
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

            DGraph scaffoldGraph = null;
            String scaffoldKey = "Scaffold";
            if (molecule.has(scaffoldKey))
            {
                String scaffSMIFromReinvent = molecule.get(scaffoldKey).getAsString();
                String scaffKey = getCanonicalSMILES(scaffSMIFromReinvent);
                scaffoldGraph = canonicalSmilesDGraphMap.get(scaffKey);
            }
            
            Candidate candidate = parseSmilesToCandidate(smiles, scaffoldGraph, i);
            if (candidate != null) {
                candidates.add(candidate);
            }
        }
        
        return candidates;
    }



//------------------------------------------------------------------------------

    /**
     * Gets the canonical SMILES string for a given SMILES string.
     * @param smiles The SMILES string
     * @return The canonical SMILES string
     */
    private String getCanonicalSMILES(String smiles)
    {
        IAtomContainer iac = null;
        try {
            iac = smilesParser.parseSmiles(smiles);
        } catch (InvalidSmilesException e) {
            throw new IllegalArgumentException(
                "Invalid SMILES string for processing with Reinvent: " + smiles, e);
        }
        String canonicalSmiles = null;
        try {
            canonicalSmiles = CANONICAL_SMGEN.create(iac);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Error generating canonical SMILES string for: " + smiles, e);
        }
        return canonicalSmiles;
    }

//------------------------------------------------------------------------------

    /**
     * Converts a SMILES string to a {@link Candidate} object. 
     * @param smiles The SMILES string
     * @param scaffoldGraph The pre-existing scaffold graph structure, if any.
     * @param index The index of the candidate in the list of candidates. 
     * This is only meant for logging.
     * @return The {@link Candidate} object
     */
    private Candidate parseSmilesToCandidate(String smiles, DGraph scaffoldGraph, int index) 
    {
        //TODO-gg del
        System.out.println("SMILES From Reinvent: " + smiles);

        // Get a CDK IAtomContainer object from the SMILES string
        IAtomContainer iac = null;
        try {
            iac = smilesParser.parseSmiles(smiles);
        } catch (InvalidSmilesException e) {
            // Skip results that cannot be converted for whatever reason
            return null;
        }
        if (iac == null) {
            // Skip results that cannot be converted for whatever reason
            return null;
        }

        // Prepare molecular fragmentation
        MoleculeUtils.explicitHydrogens(iac);
        //TODO-ai make it 3D
        /*
        if (gaSettings.getWorkOn3D())
        {
            MoleculeUtils.make3D(iac);
        }
        */

        //TODO-gg move to GAParameters
        FragmentSpaceParameters fsParams = new FragmentSpaceParameters();
        if (gaSettings.containsParameters(ParametersType.FS_PARAMS))
        {
            fsParams = (FragmentSpaceParameters) gaSettings.getParameters(
                    ParametersType.FS_PARAMS);
        }
        FragmentSpace fragSpace = fsParams.getFragmentSpace();
        
        FragmenterParameters frgParams = new FragmenterParameters();
        if (gaSettings.containsParameters(ParametersType.FRG_PARAMS))
        {
            frgParams = (FragmenterParameters) gaSettings.getParameters(
                    ParametersType.FRG_PARAMS);
        }

        DGraph graph = null;
        if (scaffoldGraph == null)
        {
            try {
                graph = FragmenterTools.makeGraphFromFragmentationOfMol(
                    iac, frgParams, fragSpace, false);
            } catch (DENOPTIMException e) {
                //TODO-ai consider counting events in monitor
                return null;
            }
        } else {
            // Run a template-based fragmentation to identify any pre-existing
            // graph structure (scaffold or warheads) ot build on.
            try {
                graph = makeTemplatedGraph(iac, scaffoldGraph);
            } catch (DENOPTIMException e) {
                //TODO-ai consider counting events in monitor
                return null;
            } 

            // Since the scaffoldGraph is decorated with placeholder atoms,
            // we can use the placeholder class to identify attachment points.
            List<AttachmentPoint> placeholdingAPs = new ArrayList<>();
            //TODO-gg consider APClassQuery
            for (AttachmentPoint ap : graph.getAttachmentPoints())
            {
                if (ap.getAPClass().getRule().startsWith(APClassPlaceholderName))
                {
                    placeholdingAPs.add(ap);
                }
            }

            // Keep track of the mapping between placeholdingAPs and their users.
            APMapping placeholderToUserMap = new APMapping();
            for (int i = 0; i < placeholdingAPs.size(); i++)
            {
                placeholderToUserMap.put(placeholdingAPs.get(i), placeholdingAPs.get(i).getLinkedAP());
            }

            // Each placeholdingAPs is on a fragment that we must try to chop further.
            Map<Vertex,DGraph> replacements = new HashMap<>();
            Set<Vertex> visitedFragments = new HashSet<>();
            for (AttachmentPoint placeholdingAP : placeholdingAPs)
            {
                Vertex fragToFragment = placeholdingAP.getOwner();
                if (visitedFragments.contains(fragToFragment)) {
                    continue;
                }

                IAtomContainer fragIAC = fragToFragment.getIAtomContainer();
                DGraph fragGraph = null;
                try {
                    fragGraph = FragmenterTools.makeGraphFromFragmentationOfMol(
                        fragIAC, frgParams, fragSpace, false);
                } catch (DENOPTIMException e) {
                    // Keep the original un-fragmented fragment, so do nothing.
                }
                if (fragGraph != null) {
                    replacements.put(fragToFragment, fragGraph);
                }
                visitedFragments.add(fragToFragment);
            }

            // Replaced un-fragmenter fragment with its graph version.
            for (Map.Entry<Vertex,DGraph> e : replacements.entrySet()) 
            {
                Vertex fragToReplace = e.getKey();
                DGraph incomingGraph = e.getValue();

                List<AttachmentPoint> placeholdingAPsOnIncGraph = new ArrayList<>();
                //TODO-gg consider APClassQuery
                for (AttachmentPoint ap : incomingGraph.getAttachmentPoints())
                {
                    if (ap.getAPClass().getRule().startsWith(APClassPlaceholderName))
                    {
                        placeholdingAPsOnIncGraph.add(ap);
                    }
                }

                APMapping apMapping = new APMapping();
                for (AttachmentPoint apOnIncGraph : placeholdingAPsOnIncGraph)
                {
                    for (AttachmentPoint placeholderAP : placeholderToUserMap.keySet())
                    {
                        if (placeholderAP.getAPClass().equals(apOnIncGraph.getAPClass()))
                        {
                            apMapping.put(apOnIncGraph, placeholderToUserMap.get(placeholderAP));
                            break;
                        }
                    }
                }

                try {
                    graph.replaceSubGraph(Arrays.asList(fragToReplace), incomingGraph, 
                        apMapping, fragSpace);
                } catch (DENOPTIMException e2) {
                    //TODO-ai consider counting events in monitor
                    return null;
                }
            }
        }

        Candidate candidate = null;
        try {
            candidate = EAUtils.graphToCandidate(graph, mnt, gaSettings);
        } catch (DENOPTIMException e) {
            //TODO-ai consider counting events in monitor
            return null;
        } 
        if (candidate != null) {
            candidate.getGraph().setLocalMsg("REINVENT_DE_NOVO");
        }
        return candidate;
    }

//------------------------------------------------------------------------------

    /**
     * Produced a graph using the scaffoldGraph as a template.
     * @param iac the molecule to fragment
     * @param scaffoldGraph the graph template
     * @return the graph produced using the scaffoldGraph as a template.
     * @throws DENOPTIMException 
     */
    private DGraph makeTemplatedGraph(IAtomContainer iac, DGraph scaffoldGraph) throws DENOPTIMException
    {
        //WARNING; here we are ignoring any other fragmentation settings that might be present in the GA settings.
        //TODO-gg consider cloning runtime settings
        FragmenterParameters frgParams = new FragmenterParameters();
        frgParams.setFragmentationTmpls(Arrays.asList(scaffoldGraph));
        DGraph graph = EAUtils.makeGraphFromFragmentationOfMol(iac, frgParams);
        return graph;
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
        // compute the number of molecules to generate so that they are almost equally 
        // distributed among the scaffolds.
        int totalNumMols = calculateNumMoleculesRequested(inputs, n);

        JsonObject jsonObj = deNovoConfig == null
                ? new JsonObject()
                : deNovoConfig.deepCopy();

        // The seed for the random number generator: used it for ensuring reproducibility
        overwriteProperty(jsonObj, new JsonPrimitive(randomizer.nextInt(1000000)),
                "seed");

        // The number of new molecules to generate. This replaces parameters.num_smiles
        // when that element is already present and leaves the other parameters in place.
        overwriteProperty(jsonObj, new JsonPrimitive(totalNumMols), "parameters", "num_smiles");
        
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
     * Calculates the number of molecules to request from the Reinvent socket server.
     * Since Reinvent generates a number of molecules per each input, we want to 
     * adjust the request to the number of inputs to avoid generating too many
     * molecules just to trash them.
     * @param inputs the input strings based on SMILES with attachment points
     * @param n the number of candidates to generate.
     * @return the number of molecules to request.
     */
    static int calculateNumMoleculesRequested(List<String> inputs, int n)
    {
        if (inputs == null || inputs.isEmpty())
        {
            return n;
        }
        int numInputs = inputs.size();
        return (int) (((long) n + numInputs - 1) / numInputs);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        return answer;
    }


//------------------------------------------------------------------------------

    /**
     * Gets the scaffolds SMILES strings from the GA settings.
     * @param settings the GA settings.
     * @return the scaffolds SMILES strings or null if no scaffolds are available.
     */
    public Map<String, DGraph> getSMILESToDGraphMapForScaffolds(GAParameters settings)
    {
        FragmentSpaceParameters fsParams = new FragmentSpaceParameters();
        if (settings.containsParameters(ParametersType.FS_PARAMS))
        {
            fsParams = (FragmentSpaceParameters)settings.getParameters(
                    ParametersType.FS_PARAMS);
        }
        FragmentSpace fragSpace = fsParams.getFragmentSpace();

        Map<String, DGraph> smilesDGraphMap = new HashMap<String, DGraph>();
        if (fragSpace != null && fragSpace.getScaffoldLibrary() != null 
                && !fragSpace.getScaffoldLibrary().isEmpty()) 
        {
            List<Vertex> scaffolds = fragSpace.getScaffoldLibrary();
            for (int i = 0; i < scaffolds.size(); i++)
            {
                Vertex scafVertex = fragSpace.makeVertexFromLLibrary( 
                        BBType.SCAFFOLD, i);

                if (!scafVertex.containsAtoms())
                    continue;

                // Make a graph where all APs on the scaffold are saturated with
                // attachment point placeholder pseudoatom R.
                DGraph graph = new DGraph();
                String smiles = null;
                try {
                    graph.addVertex(scafVertex);
                    addCapOnAllFreeAPs(Arrays.asList(scafVertex));

                    ThreeDimTreeBuilder t3d = new ThreeDimTreeBuilder(settings.getLogger(),
                        settings.getRandomizer());
                    t3d.setAlignBBsIn3D(false);
                    IAtomContainer mol = t3d.convertGraphTo3DAtomContainer(graph,true);
                    
                   smiles= MoleculeUtils.getSMILESForMolecule(mol, true, settings.getLogger());
                } catch (Exception e) {
                    settings.getLogger().log(Level.WARNING, 
                        "Scaffold decorated with AP placeholders "
                        + "could not be converted to SMILES. Skipping scaffold "
                        + "for de novo generation with Reinvent.");
                    continue;
                }
                smilesDGraphMap.put(smiles, graph);
            }
        }

        if (smilesDGraphMap.size() > 0) {
            return smilesDGraphMap;
        }
        return null;
    }

//------------------------------------------------------------------------------

    private void addCapOnAllFreeAPs(List<Vertex> vertexes) throws DENOPTIMException
    {
        int apId = -1;
        for (Vertex curVertex : vertexes)
        {
            for (AttachmentPoint curDap : curVertex.getAttachmentPoints())
            {
                if (curDap.isAvailableThroughout())
                {
                    Vertex capVrtx = getAPPlaceHolder(apId++);
                    curVertex.getGraphOwner().appendVertexOnAP(curDap, capVrtx.getAP(0));
                }
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Gets a placeholder vertex for attachment points.
     * @return the placeholder vertex.
     */
    private Vertex getAPPlaceHolder(int apId)
    {
        Fragment placeHolderFrag = new Fragment();
        PseudoAtom atm = new PseudoAtom("R", 
            new Point3d(new double[]{0.0, 0.0, 0.0}));
        placeHolderFrag.addAtom(atm);
        try {
            // Use apID in the "rule" part of the APClass to avoid having to 
            // match also the subclass.
            placeHolderFrag.addAPOnAtom(atm, APClass.make(APClassPlaceholderName+"_"+apId,0), 
                new Point3d(new double[]{0.0, 0.0, 1.0}));
        } catch (DENOPTIMException e) {
            // This cannot happen unless there are unforeseen changed to APClass
            // basename syntax.
            throw new IllegalArgumentException(
                "Illegal APClass built form 'APClassPlaceholderName'. "
                + "Report this to the developers.", e);
        }
        placeHolderFrag.setVertexId(GraphUtils.getUniqueVertexIndex());
        return placeHolderFrag;
    }

//------------------------------------------------------------------------------

}
