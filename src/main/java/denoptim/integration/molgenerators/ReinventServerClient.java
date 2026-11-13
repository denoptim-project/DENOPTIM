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
import denoptim.fragmenter.FragmenterTools;
import denoptim.fragmenter.ScaffoldingPolicy;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.ga.EAUtils;
import denoptim.graph.APClass;
import denoptim.graph.APMapping;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.AttachmentPointQuery;
import denoptim.graph.DGraph;
import denoptim.graph.Edge.BondType;
import denoptim.graph.Fragment;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;
import denoptim.io.DenoptimIO;
import denoptim.molecularmodeling.ThreeDimTreeBuilder;
import denoptim.programs.RunTimeParameters.ParametersType;
import denoptim.programs.denovo.GAParameters;
import denoptim.programs.fragmenter.FragmenterParameters;
import denoptim.programs.genetweeker.GeneOpsRunnerParameters;
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
     * Kinds of service provided by Reinvent servers.
     */
    public enum ServiceType {
        DE_NOVO,
        GROWTH,
        LINK,
    }

    /**
     * Singleton instances
     */
    private static Map<ServiceType, ReinventServerClient> instances = new HashMap<>();
    
    /**
     * Service type
     */
    private ServiceType serviceType;

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
     * GA parameters to control the generation of molecules
     */
    private GAParameters gaSettings;

    /**
     * Template configuration for de novo molecualr generation (i.e., unbiased
     * generation of molecules)
     */
    private JsonObject configurations;

    /**
     * Basename for APClass of placeholder vertexes
     */
    static final String PLACEHOLDERAPCLASSNAME = "PlaceholderAPForReinvent"; 

    /**
     * Key for the scaffold SMILES in the Reinvent answer
     */
    static final String SCAFFOLD_KEY = "Scaffold";
    
    /**
     * Key for the warheads SMILES in the Reinvent answer
     */
    static final String WARHEADS_KEY = "Warheads";

//------------------------------------------------------------------------------
    
    /**
     * Private constructor for singleton pattern.
     */
    private ReinventServerClient(ServiceType serviceType, String hostname, Integer port) {
        this.serviceType = serviceType;
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
     * @param serviceType The service type
     * @param hostname The hostname or IP address of the socket server
     * @param port The port number of the socket server
     * @param randomizer The randomizer to control pseudo-number generation at the Reinvent service
     * @return The singleton instance
     */
    public static synchronized ReinventServerClient getInstance(
        ServiceType serviceType, GAParameters gaSettings)
    {
        if (instances.get(serviceType) == null) {
            ReinventServerClient client = new ReinventServerClient(
                serviceType,
                gaSettings.getReinventServerHostname(), 
                gaSettings.getReinventServerPort());
            client.randomizer = gaSettings.getRandomizer();
            client.gaSettings = gaSettings;
            switch (serviceType) {
                case DE_NOVO:
                    client.configurations = readReinventConfigurationFile(
                        gaSettings.getReinventDeNovoConfigPathname());
                    break;
                case GROWTH:
                    client.configurations = readReinventConfigurationFile(
                        gaSettings.getReinventGrowthConfigPathname());
                    break;
                case LINK:
                    client.configurations = readReinventConfigurationFile(
                        gaSettings.getReinventLinkersConfigPathname());
                    break;
            }
            instances.put(serviceType, client);
        }
        return instances.get(serviceType);
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
     * Generate graphs from the given mapping of SMILES requests and 
     * corresponding DGraph objects.
     * @param smilesDGraphMap the map of SMILES strings to DGraph objects.
     * @param n the total number of graphs to generate,
     * but the result may contain fewer graphs depending on the hitting of
     * invalid SMILES strings. If the amount of input in smilesDGraphMap is 
     * higher than n, then we randomy pick n results.
     * @return the list of generated graphs, which may be empty.
     * @throws IOException if an I/O error occurs.
     * @throws JsonSyntaxException if the JSON syntax is invalid.
     */

    // smilesDGraphMap.get(key) is called only if smilesDGraphMap is not null and has keys 
    @SuppressWarnings("null") 
    public List<DGraph> generateGraphs(Map<String, List<DGraph>> smilesDGraphMap, int n)
        throws IOException, JsonSyntaxException
    {
        List<String> inputSmiles = new ArrayList<>();
        if (smilesDGraphMap!=null && !smilesDGraphMap.isEmpty()) {
            inputSmiles.addAll(smilesDGraphMap.keySet());
        }

        // Reinvent returns scaffolds SMILES that are not the same as those given as input.
        // So we generate canonical SMILES to use as keys.
        Map<String, List<DGraph>> canonicalSmilesDGraphMap = new HashMap<>();
        for (String scaffSMI : inputSmiles)
        {
            String scaffKey = getCanonicalSMILES(scaffSMI);
            canonicalSmilesDGraphMap.put(scaffKey, smilesDGraphMap.get(scaffSMI));
        }

        String requestAsJSONString = formulateRequest(inputSmiles, n);
        
        //This might be useful for debugging to get the actual request placed to the server
        gaSettings.getLogger().log(Level.FINE, 
            "Request to the Reinvent socket server: " + requestAsJSONString);
        if (requestFileName != null) {
            try {
                DenoptimIO.writeData(requestFileName, 
                    requestAsJSONString+System.lineSeparator(), true);
            } catch (DENOPTIMException e) {
                // Not handled as meant only to debugging
                e.printStackTrace();
            }
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

        or:

        {
            "status": "ERROR",
            "error_code": "PROCESSING_ERROR",
            "error_message": "Not enough molecules generated after..."
            ...
        }
        
        */
        
        JsonObject answer = null;
        
        try {
            answer = sendRequest(requestAsJSONString);
        } catch (ClientException e) {
            // We treat this as a failed attempt, which may be due to a temporary
            // condition, so we return an empty list but do not throw to allow 
            // further attempts.
            gaSettings.getLogger().log(Level.WARNING, 
                "Could not send request to Reinvent socket server: " + e.getMessage());
            return new ArrayList<>();
        }

        // Handle any kind of bad/unsuccessful answer
        if (answer == null)
        {
            // We treat this as a failed attempt, but do not throw to allow 
            // further attempts.
            return new ArrayList<>();
        }
        if (!answer.has("status"))
        {
            throw new Error("Reinvent socket server replied without "
                    + "including 'status' member. " 
                    + "Aborting! " + answer.toString());
        }
        if (answer.get("status").getAsString().equals("ERROR"))
        {
            String errorMessage = "Unknown error";
            if (answer.has("error_message")) {
                errorMessage = answer.get("error_message").getAsString();
            }

            if (errorMessage.contains("Not enough molecules generated after"))
            {
                // We treat this as a failed attempt, but do not throw to allow 
                // further attempts.
                return new ArrayList<>();
            }

            throw new Error("Reinvent socket server replied with unhandled error: '" 
                + errorMessage + "'. Aborting! " + answer.toString());
        }
        if (!answer.get("status").getAsString().equals("SUCCESS"))
        {
            throw new Error("Reinvent socket server replied with unexpected status=" 
                + answer.get("status").getAsString() + ". " 
                + "Aborting! " + answer.toString());
        }
        if (!answer.has("molecules"))
        {
            throw new Error("Reinvent socket server replied without "
                    + "including 'molecules' member. " 
                    + "Aborting! " + answer.toString());
        }

        // Utility to generate pre-computed answers for testing purposes
        if (gaSettings.containsParameters(ParametersType.GO_PARAMS))
        {
            GeneOpsRunnerParameters goParams = (GeneOpsRunnerParameters)
                gaSettings.getParameters(ParametersType.GO_PARAMS);
            String answerFileName = goParams.getReinventServerAnswersPathname();
            if (answerFileName != null && !answerFileName.isEmpty())
            {
                try {
                    DenoptimIO.writeData(answerFileName, answer.toString(), false);
                } catch (DENOPTIMException e) {
                    // Not handled as meant only to debugging
                    e.printStackTrace();
                }
            }
        }

        JsonArray molecules = answer.get("molecules").getAsJsonArray();
        while (molecules.size() > n)
        {
            int iii = randomizer.nextInt(molecules.size());
            molecules.remove(iii);
        }

        List<DGraph> graphs = new ArrayList<>();
        for (int i = 0; i < molecules.size(); i++)
        {
            JsonObject molecule = molecules.get(i).getAsJsonObject();
            String smiles = molecule.get("SMILES").getAsString();

            DGraph graph = null;
            if (molecule.has(SCAFFOLD_KEY) || molecule.has(WARHEADS_KEY))
            {
                String inputSMILES = "unset";
                if (molecule.has(SCAFFOLD_KEY)) {
                    inputSMILES = molecule.get(SCAFFOLD_KEY).getAsString();
                } else if (molecule.has(WARHEADS_KEY))
                {
                    inputSMILES = molecule.get(WARHEADS_KEY).getAsString();
                }
                String inputKey = getCanonicalSMILES(inputSMILES);
                if (!canonicalSmilesDGraphMap.containsKey(inputKey)) 
                {
                    continue;
                }
                graph = parseSmilesToGraph(smiles, 
                    canonicalSmilesDGraphMap.get(inputKey), i);
            } else {
                graph = parseSmilesToGraph(smiles, null, i);
            }
            
            if (graph != null) {
                graphs.add(graph);
            }
        }
        if (graphs.isEmpty()) {
            gaSettings.getLogger().log(Level.WARNING, 
                "Failed to parse graphs from Reinvent server response.");
            return new ArrayList<>();
        }
        
        return graphs;
    }

//------------------------------------------------------------------------------

    /**
     * Exception thrown when the Reinvent socket server cannot be reached.
     */
    private class ClientException extends Exception
    {
        public ClientException(String message)
        {
            super(message);
        }

        public ClientException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Gets the canonical SMILES string for a given SMILES string. Handles also
     * the combined pair of warheads SMILES (i.e., two smiles separated by '|').
     * @param smiles The SMILES string
     * @return The canonical SMILES string
     */
    private String getCanonicalSMILES(String smiles)
    {
        if (smiles.contains("|")) {
            String[] warheadsSmiles = smiles.split("\\|");
            String canonicalWarheadsSmiles = getCanonicalSMILES(warheadsSmiles[0]) 
                + "|" + getCanonicalSMILES(warheadsSmiles[1]);
            return canonicalWarheadsSmiles;
        }

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
     * Converts a SMILES string to a {@link DGraph} considering the template 
     * graph/s, if given.
     * @param smiles The SMILES string
     * @param templateGraphs the template graph/s, if any.
     * @param index The index of the graph in the list of graphs. 
     * This is only meant for logging.
     * @return The graph, or <code>null</code> when the SMILES string cannot be
     * converted.
     */
    DGraph parseSmilesToGraph(String smiles, List<DGraph> templateGraphs, int index) 
    {
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
        if (templateGraphs == null)
        {
            try {
                graph = FragmenterTools.makeGraphFromFragmentationOfMol(
                    iac, frgParams, fragSpace, false);
            } catch (DENOPTIMException e) {
                //TODO-ai consider counting events in monitor
                return null;
            }
        } else {
            if (templateGraphs.size() == 1) {
                graph =parseAtomContainerToGraph(iac, templateGraphs.get(0), 
                    index, fragSpace, frgParams);
            } else if (templateGraphs.size() == 2) {
                graph = parseAtomContainerToLinkedGraph(iac, 
                    templateGraphs.get(0), templateGraphs.get(1), 
                    index, fragSpace, frgParams);
            } else {
                throw new IllegalStateException(
                    "Unexpected number of template graphs: " + templateGraphs.size());
            }
        }
        return graph;
    }

//------------------------------------------------------------------------------

    /**
     * Converts the given {@link IAtomContainer} to a {@link DGraph}.
     * @param iac The {@link IAtomContainer} to convert
     * @param scaffoldGraph The pre-existing graph structures (not null!).
     * @param index The index of the graph in the list of graphs. 
     * This is only meant for logging.    
     * @param fragSpace The {@link FragmentSpace} to use
     * @param frgParams The {@link FragmenterParameters} to use
     * @return The graph, or <code>null</code> when the {@link IAtomContainer} 
     * cannot be converted.
     */
    private DGraph parseAtomContainerToGraph(IAtomContainer iac, DGraph scaffoldGraph, 
        int index, FragmentSpace fragSpace, FragmenterParameters frgParams)
    {
        DGraph graph = null;

        // Run a template-based fragmentation to identify any pre-existing
        // graph structure to build on.
        try {
            graph = makeTemplatedGraph(iac, scaffoldGraph);
        } catch (DENOPTIMException e) {
            //TODO-ai consider counting events in monitor
            return null;
        } 

        // Since the scaffoldGraph is decorated with placeholder fragments,
        // we can use the placeholder class to identify attachment points.
        List<AttachmentPoint> placeholdingAPs = new ArrayList<>();
        //TODO-gg consider APClassQuery
        for (AttachmentPoint ap : graph.getAttachmentPoints())
        {
            if (ap.getAPClass().getRule().startsWith(PLACEHOLDERAPCLASSNAME))
            {
                placeholdingAPs.add(ap);
            }
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

            // TODO: this will benefit from copying of RunTimeParameters
            IAtomContainer fragIAC = fragToFragment.getIAtomContainer();
            DGraph fragGraph = null;
            try {
                FragmenterParameters frgParamsNoScaffolding = new FragmenterParameters();
                frgParamsNoScaffolding.setCuttingRules(frgParams.getCuttingRules());
                frgParamsNoScaffolding.setScaffoldingPolicy(ScaffoldingPolicy.NONE);
                fragGraph = FragmenterTools.makeGraphFromFragmentationOfMol(
                    fragIAC, frgParamsNoScaffolding, fragSpace, false);
            } catch (DENOPTIMException e) {
                // Keep the original un-fragmented fragment, so do nothing.
            }
            if (fragGraph != null && fragGraph.getVertexList().size() > 1) {
                replacements.put(fragToFragment, fragGraph);
            }
            visitedFragments.add(fragToFragment);
        }

        // Replace un-fragmented fragments with the graphs resulting from fragmentation.
        for (Map.Entry<Vertex,DGraph> e : replacements.entrySet()) 
        {
            Vertex fragToReplace = e.getKey();
            DGraph incomingGraph = e.getValue();

            List<AttachmentPoint> placeholdingAPsOnIncGraph = new ArrayList<>();
            //TODO-gg consider APClassQuery
            for (AttachmentPoint ap : incomingGraph.getAttachmentPoints())
            {
                if (ap.getAPClass().getRule().startsWith(PLACEHOLDERAPCLASSNAME))
                {
                    placeholdingAPsOnIncGraph.add(ap);
                }
            }

            APMapping apMapOldToIncoming = new APMapping();
            for (AttachmentPoint apOnIncGraph : placeholdingAPsOnIncGraph)
            {
                for (AttachmentPoint placeholdingAP : placeholdingAPs)
                {
                    if (placeholdingAP.getAPClass().equals(apOnIncGraph.getAPClass()))
                    {
                        apMapOldToIncoming.put(placeholdingAP, apOnIncGraph);
                        break;
                    }
                }
            }

            try {
                graph.replaceSubGraph(Arrays.asList(fragToReplace), incomingGraph, 
                    apMapOldToIncoming, fragSpace);
            } catch (DENOPTIMException e2) {
                //TODO-ai consider counting events in monitor
                return null;
            }
        }

        // Re-assign APClass to reflect the AP on the scaffold graph.
        // This is needed to avoid the placeholding APClass to end up
        // into the final graph and thus pollute future fragmentations
        // of derived graphs.
        for (AttachmentPoint ap : graph.getAttachmentPoints())
        {
            if (ap.getAPClass().getRule().startsWith(PLACEHOLDERAPCLASSNAME))
            {
                if (ap.getLinkedAP() == null) {
                    throw new IllegalStateException("Unexpected free placeholding AP.");
                }
                APClass apcOnScaff = ap.getLinkedAP().getAPClass();
                // WARNING: we assume asymmetric cutting rule!
                int subClass = 1;
                if (apcOnScaff.getSubClass() == 1) {
                    subClass = 0;
                }
                // TODO-ai: consider getting APClass from cutting rules.
                try {
                    ap.setAPClass(APClass.make(apcOnScaff.getRule(), subClass));
                } catch (DENOPTIMException e2) {
                    // Not possible
                    throw new IllegalStateException("Not possible to make APClass: " 
                        + e2.getMessage());
                }
            }
        }
        return graph;
    }

//------------------------------------------------------------------------------

    /**
     * Converts a SMILES string to a {@link DGraph} that doe NOT include the
     * warheads given as parameters.
     * @param smiles The SMILES string
     * @param parentWarhead part of the graph that acts as parent to the linker.
     * @param childWarwead part of the graph that acts as child ofthe linker.
     * @param index The index of the graph in the list of graphs. 
     * This is only meant for logging.
     * @return The graph, or <code>null</code> when the SMILES string cannot be
     * converted.
     */
    private DGraph parseAtomContainerToLinkedGraph(IAtomContainer iac,
        DGraph parentWarhead, DGraph childWarhead, int index,
        FragmentSpace fragSpace, FragmenterParameters frgParams)
    {
        // Run a template-based fragmentation to identify any pre-existing
        // graph structure on the parent side
        DGraph parentSideGraph = null;
        try {
            parentSideGraph = makeTemplatedGraph(iac, parentWarhead);
        } catch (DENOPTIMException e) {
            //TODO-ai consider counting events in monitor
            return null;
        }
        if (parentSideGraph == null) 
        {
            // Cannot identify parent warhead: abbandon
            return null;
        }

        // Identify the whole linker+child vertex Because 
        // of the constraint to only TWO warheads, there is only linker
        // vertex.
        Vertex linkerAndChildVertex = null;
        AttachmentPoint apOfLinkerOnParentGraph = null;
        APClass parentSideAPClass = null;
        try {
            parentSideAPClass = APClass.make("parentSide" + PLACEHOLDERAPCLASSNAME,0);
        } catch (DENOPTIMException e) {
            // Not possible 
            throw new IllegalStateException("Not possible to make parentSideAPClass: " 
                + e.getMessage());
        }
        for (AttachmentPoint ap : parentSideGraph.getAttachmentPoints())
        {
            if (ap.getAPClass().getRule().startsWith(PLACEHOLDERAPCLASSNAME))
            {
                linkerAndChildVertex = ap.getOwner();
                // Rename the APClass to acoid clash with the fragmentation 
                // on the child side.
                ap.setAPClass(parentSideAPClass);
                // And keep track of the AP on the parent graphg:
                // we'll need it to replace the placeholder APs with the linker graph.
                apOfLinkerOnParentGraph = ap;
                break;
            }
        }
        if (linkerAndChildVertex == null) {
            // Cannot identify linker vertex: abbandon
            return null;
        }

        // Fragment the linker+child vertex to isolate the two components.
        DGraph childSideGraph = null;
        try {
            FragmenterParameters frgParsChildSide = new FragmenterParameters();
            frgParsChildSide.setScaffoldingPolicy(ScaffoldingPolicy.NONE);
            frgParsChildSide.setFragmentationTmpls(Arrays.asList(childWarhead));
            childSideGraph = FragmenterTools.makeGraphFromFragmentationOfMol(
                linkerAndChildVertex.getIAtomContainer(), 
                frgParsChildSide, fragSpace, false);
            //TODO-ai vertex types seem wrong, here
        } catch (DENOPTIMException e) {
            // Cannot identify child warhead: abbandon
            return null;
        }
        if (childSideGraph == null) 
        {
            // Cannot identify child warhead: abbandon
            return null;
        }
        
        // Identify the whole linker, which we further fragment. Because 
        // of the constraint to only TWO warheads, there is only one linker
        // vertex, but there could be many from the child warhead.
        Vertex linkerToFragment = null;
        AttachmentPoint linkerToParentApOnChildGraph = childSideGraph.findAPs(
            new AttachmentPointQuery(null, //apID
                null, // apIndex, 
                parentSideAPClass, // apClass, 
                null, // vertexQuery, 
                null, // edgeQuery, 
                null), // linkedAPQuery
                frgParams.getLogger()).get(0);
        linkerToFragment = linkerToParentApOnChildGraph.getOwner();

        // Identify the AP on the child warhead subgraph that will be 
        // appended to the parent+linker graph.
        AttachmentPoint linkerToChildApOnChildGraph = null;
        //TODO-ai: consider APClassQuery
        for (AttachmentPoint ap : childSideGraph.getAttachmentPoints())
        {
            if (ap.getAPClass().getRule().startsWith(PLACEHOLDERAPCLASSNAME))
            {
                // Only one expected
                linkerToChildApOnChildGraph = ap;
                break;
            }
        }
        AttachmentPoint apToLinkerOnChildGraph = linkerToChildApOnChildGraph.getLinkedAP();
        BondType bondTypeChildToLinker = linkerToChildApOnChildGraph.getEdgeUser().getBondType();

        // Now, we can isolate the child warhead from the linker.
        // This is what we'll append to the parent+linker graph.
        childSideGraph.removeVertex(linkerToFragment);

        // Further fragment the linker, if possible.
        // TODO: this will benefit from copying of RunTimeParameters
        IAtomContainer fragIAC = linkerToFragment.getIAtomContainer();
        DGraph linkerGraph = null;
        try {
            FragmenterParameters frgParamsNoScaffolding = new FragmenterParameters();
            frgParamsNoScaffolding.setCuttingRules(frgParams.getCuttingRules());
            frgParamsNoScaffolding.setScaffoldingPolicy(ScaffoldingPolicy.NONE);
            linkerGraph = FragmenterTools.makeGraphFromFragmentationOfMol(
                fragIAC, frgParamsNoScaffolding, fragSpace, false);
        } catch (DENOPTIMException e) {
            // Keep the original un-fragmented fragment, so do nothing.
        }
        if (linkerGraph == null)
        {
            linkerGraph = new DGraph();
            try {
                linkerGraph.addVertex(linkerToFragment.clone());
            } catch (DENOPTIMException e) {
                throw new IllegalStateException("Not possible to clone vertex: " 
                    + e.getMessage());
            }
        }

        /*
        
        This code correctly merges parent+linker+child into a single graph.
        But this is redundant, here, becasue to use the linker we still have to
        explore the resulting graph again to find the APs at the parent/linker and 
        linker/child interfaces. So, I do not see the point of returning a complete 
        graph. Yet, the following code works...

        // Define AP mapping to replace linker in the parent side graph
        APMapping apMapParentToLinker = new APMapping();
        AttachmentPoint apOnLinkerGraph = linkerGraph.findAPs(
            new AttachmentPointQuery(null, //apID
                null, // apIndex, 
                parentSideAPClass, // apClass, 
                null, // vertexQuery, 
                null, // edgeQuery, 
                null), // linkedAPQuery
                frgParams.getLogger()).get(0);
        apMapParentToLinker.put(apOfLinkerOnParentGraph, apOnLinkerGraph);

        // Replace placeholder APs with the linker graph.
        try {
            parentSideGraph.replaceSubGraph(
                Arrays.asList(linkerAndChildVertex), 
                linkerGraph, apMapParentToLinker, fragSpace);
        } catch (DENOPTIMException e) {
            throw new IllegalStateException("Not possible to replace subgraph: " 
                + e.getMessage());
        }

        // Define the AP on the parent+linker graph that will hold the child side.
        AttachmentPoint apOnGrowingGraph = null;
        //TODO-ai: consider APClassQuery
        for (AttachmentPoint ap : parentSideGraph.getAttachmentPoints())
        {
            if (ap.getAPClass().getRule().startsWith(PLACEHOLDERAPCLASSNAME))
            {
                // Only one expected
                apOnGrowingGraph = ap;
                break;
            }
        }

        // Append the child side to the parent+linker graph.
        int vertexCountBefore = parentSideGraph.getVertexList().size();
        try {
            parentSideGraph.appendGraphOnAP(apOnGrowingGraph, apToLinkerOnChildGraph, 
                bondTypeChildToLinker);
        } catch (DENOPTIMException e) {
            throw new IllegalStateException("Not possible to append graph on AP: " 
                + e.getMessage());
        }
        int vertexCountAfter = parentSideGraph.getVertexList().size();
        if (vertexCountAfter <= vertexCountBefore) {
            throw new IllegalStateException("Expected at least 1 new vertex, got " + (vertexCountAfter - vertexCountBefore));
        }

        // Re-assign APClass to reflect the AP on the template graph.
        // This is needed to avoid the placeholding APClasses to end up
        // into the final graph and thus pollute future fragmentations
        // of derived graphs.
        for (AttachmentPoint ap : parentSideGraph.getAttachmentPoints())
        {
            if (ap.getAPClass().getRule().startsWith(PLACEHOLDERAPCLASSNAME)
                || ap.getAPClass().equals(parentSideAPClass))
            {
                if (ap.getLinkedAP() == null) {
                    throw new IllegalStateException("Unexpected free placeholding AP.");
                }
                APClass apcOnTemplate = ap.getLinkedAP().getAPClass();
                // WARNING: we assume asymmetric cutting rule!
                int subClass = 1;
                if (apcOnTemplate.getSubClass() == 1) {
                    subClass = 0;
                }
                // TODO-ai: consider getting APClass from cutting rules.
                try {
                    ap.setAPClass(APClass.make(apcOnTemplate.getRule(), subClass));
                } catch (DENOPTIMException e2) {
                    // Not possible
                    throw new IllegalStateException("Not possible to make APClass: " 
                        + e2.getMessage());
                }
            }
        }

        return parentSideGraph;
        */

        // Re-assign APClass to reflect the AP on the template graph.
        // This is needed to avoid the placeholding APClasses to end up
        // into the final graph and thus pollute future fragmentations
        // of derived graphs.
        for (AttachmentPoint ap : linkerGraph.getAttachmentPoints())
        {
            if (ap.getAPClass().getRule().startsWith(PLACEHOLDERAPCLASSNAME)
                || ap.getAPClass().equals(parentSideAPClass))
            {
                APClass apcOnTemplate = null;
                // Record the matched AP from the input warhead subgraphs: 
                // we'll need it for installing the linker in the correct direction 
                if (ap.getAPClass().getRule().startsWith(PLACEHOLDERAPCLASSNAME))
                {
                    ap.setProperty(AttachmentPoint.APPROPMATCHEDAP, 
                        apToLinkerOnChildGraph.getProperty(
                            AttachmentPoint.APPROPMATCHEDAP));
                    apcOnTemplate = apToLinkerOnChildGraph.getAPClass();
                } else if (ap.getAPClass().equals(parentSideAPClass))
                {
                    ap.setProperty(AttachmentPoint.APPROPMATCHEDAP, 
                        apOfLinkerOnParentGraph.getLinkedAP().getProperty(
                            AttachmentPoint.APPROPMATCHEDAP));
                    apcOnTemplate = apOfLinkerOnParentGraph.getLinkedAP().getAPClass();
                }

                // WARNING: we assume asymmetric cutting rule!
                int subClass = 1;
                if (apcOnTemplate.getSubClass() == 1) {
                    subClass = 0;
                }
                // TODO-ai: consider getting APClass from cutting rules.
                try {
                    ap.setAPClass(APClass.make(apcOnTemplate.getRule(), subClass));
                } catch (DENOPTIMException e2) {
                    // Not possible
                    throw new IllegalStateException("Not possible to make APClass: " 
                        + e2.getMessage());
                }
            }
        }

        return linkerGraph;
    }

//------------------------------------------------------------------------------

    /**
     * Produced a graph using the scaffoldGraph as a template.
     * @param iac the molecule to fragment
     * @param scaffoldGraph the graph template
     * @return the graph produced using the scaffoldGraph as a template.
     * @throws DENOPTIMException 
     */
    private DGraph makeTemplatedGraph(IAtomContainer iac, DGraph scaffoldGraph) 
        throws DENOPTIMException
    {
        // WARNING; here we are ignoring any other fragmentation settings that 
        // might be present in the GA settings.
        //TODO-gg consider cloning runtime settings
        FragmenterParameters frgParams = new FragmenterParameters();
        frgParams.setFragmentationTmpls(Arrays.asList(scaffoldGraph));

        FragmentSpaceParameters fsp = new FragmentSpaceParameters();
        FragmentSpace fragSpace = fsp.getFragmentSpace();

        // NB: we want to retain the AP properties because they give up the
        // mapping between APs in the template graph and APs in the resulting graph.
        DGraph graph = FragmenterTools.makeGraphFromFragmentationOfMol(iac, frgParams, 
            fragSpace, false);

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
    public String formulateRequest(List<String> inputs, int n) throws IOException
    {    
        // compute the number of molecules to generate so that they are almost equally 
        // distributed among the scaffolds.
        int totalNumMols = calculateNumMoleculesRequested(inputs, n);

        JsonObject jsonObj = configurations == null
                ? new JsonObject()
                : configurations.deepCopy();

        // The seed for the random number generator: used it for ensuring reproducibility
        overwriteProperty(jsonObj, new JsonPrimitive(randomizer.nextInt(1000000)),
                "seed");

        // The number of new molecules to generate. This replaces parameters.num_smiles
        // when that element is already present and leaves the other parameters in place.
        overwriteProperty(jsonObj, new JsonPrimitive(totalNumMols), "parameters", "num_smiles");
        
        if (inputs != null && !inputs.isEmpty())
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
     * Calculates the number of molecules to request to the Reinvent socket server.
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
     * @return the answer from the socket server as a JSON object, or 
     * <code>null</code> if we did not get an answer, for example because the 
     * server looses connection or gets shut down.
     * @throws IOException if an I/O error occurs
     * @throws ClientException 
     */
    private JsonObject sendRequest(String requestAsJSONString) 
            throws IOException, JsonSyntaxException, ClientException
    {
        JsonObject answer = null;
        if (gaSettings.containsParameters(ParametersType.GO_PARAMS)) 
        {
            // For testing purposes, this bypasses the actual call to Reinvent's 
            // server by retrieving the answer from the file specified in the settings.
            GeneOpsRunnerParameters goParams = (GeneOpsRunnerParameters) 
                gaSettings.getParameters(ParametersType.GO_PARAMS);
            String answerFileName = goParams.getPrecomputedReinventAnswerFile();
            if (answerFileName != null && !answerFileName.isEmpty()) 
            {
                File answerFile = new File(answerFileName);
                answer = jsonConverter.fromJson(new FileReader(answerFile), 
                    JsonObject.class);
                return answer;
            }
        }

        Socket socket;
        try
        {
            socket = new Socket(hostname, port);
        } catch (IOException e1)
        {
            throw new ClientException("Could not connect to socket", e1);
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
            throw new ClientException("Could not connect to socket",e1);
        }
        
        BufferedReader readerFromSocket;
        try
        {
            InputStream inputFromSocket = socket.getInputStream();
            readerFromSocket = new BufferedReader(new InputStreamReader(inputFromSocket));
        } catch (IOException e1)
        {
            try
            {
                socket.close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
            throw new ClientException("Could not read from socket",e1);
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
            throw new ClientException("Could not half-close socket from "
                    + this.getClass().getName(),e1);
        }
        
        // Read and process the answer from the socket
        try {
            answer = jsonConverter.fromJson(readerFromSocket.readLine(), 
                    JsonObject.class);

            // Answer may be null if the server looses connection or gets shut down.
            // We behave as a filed attempt, but do not throw to allow further attempts.
            if (answer != null)
            {
                if (!(answer.has("status")))
                {
                    throw new Error("Socket server replied without "
                            + "including " + "status" + " member. " 
                            + "Something is badly wrong: aborting! " + answer.toString());
                }
                if (!answer.get("status").getAsString().equals("SUCCESS")
                    && !answer.get("status").getAsString().equals("ERROR"))
                {
                    // Only when the status is neither SUCCESS nor ERROR, we throw an error.
                    throw new Error("Socket server replied but with status=" 
                            + answer.get("status") + ". " 
                            + "Something is badly wrong: aborting! " + answer.toString());
                }
            }
        } catch (JsonSyntaxException e) {
            throw new Error("Socket server replied with invalid JSON: " + e.getMessage());
        } catch (IOException e) {
            throw new ClientException("Error reading from socket.", e);
        }
        
        try
        {
            socket.close();
        } catch (IOException e) {
            throw new ClientException("Could not close socket.", e);
        }
        return answer;
    }

//------------------------------------------------------------------------------

    /**
     * Gets the scaffolds SMILES strings from the GA settings.
     * @param settings the GA settings.
     * @return the scaffolds SMILES strings or null if no scaffolds are available.
     */
    public Map<String, List<DGraph>> getSMILESToDGraphMapForScaffolds(GAParameters settings)
    {
        FragmentSpaceParameters fsParams = new FragmentSpaceParameters();
        if (settings.containsParameters(ParametersType.FS_PARAMS))
        {
            fsParams = (FragmentSpaceParameters)settings.getParameters(
                    ParametersType.FS_PARAMS);
        }
        FragmentSpace fragSpace = fsParams.getFragmentSpace();

        Map<String, List<DGraph>> smilesDGraphMap = new HashMap<String, List<DGraph>>();
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
                    smiles = addAPPlaceHoldersAndGetSMILES(graph, settings);
                    smilesDGraphMap.put(smiles, Arrays.asList(graph));
                } catch (Exception e) {
                    settings.getLogger().log(Level.WARNING, 
                        "Scaffold decorated with AP placeholders "
                        + "could not be converted to SMILES. Skipping scaffold "
                        + "for de novo generation with Reinvent.");
                    continue;
                }
            }
        }

        if (smilesDGraphMap.size() > 0) {
            return smilesDGraphMap;
        }
        return null;
    }

//------------------------------------------------------------------------------

    /**
     * Gets the SMILES strings for the corresponding graph.
     * @param graph the graph to convert to SMILES.
     * @param settings the GA settings.
     * @return the scaffolds SMILES strings or null if no scaffolds are available.
     */
    public String addAPPlaceHoldersAndGetSMILES(DGraph graph, GAParameters settings)
        throws DENOPTIMException
    {
        addCapOnAllFreeAPs(graph);

        ThreeDimTreeBuilder t3d = new ThreeDimTreeBuilder(settings.getLogger(),
            settings.getRandomizer());
        t3d.setAlignBBsIn3D(false);
        IAtomContainer mol = t3d.convertGraphTo3DAtomContainer(graph,true);
        
        return MoleculeUtils.getSMILESForMolecule(mol, true, settings.getLogger());
    }

//------------------------------------------------------------------------------

    private void addCapOnAllFreeAPs(DGraph graph) throws DENOPTIMException
    {
        int apId = -1;
        // NB: we add vertexes, so work on the initial list of vertexes
        List<Vertex> vertices = new ArrayList<>(graph.getVertexList());
        for (Vertex curVertex : vertices)
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
    static Vertex getAPPlaceHolder(int apId)
    {
        Fragment placeHolderFrag = new Fragment();
        PseudoAtom atm = new PseudoAtom("R", 
            new Point3d(new double[]{0.0, 0.0, 0.0}));
        placeHolderFrag.addAtom(atm);
        try {
            // Use apID in the "rule" part of the APClass to avoid having to 
            // match also the subclass.
            placeHolderFrag.addAPOnAtom(atm, APClass.make(
                PLACEHOLDERAPCLASSNAME+"_"+apId,0), 
                new Point3d(new double[]{0.0, 0.0, 1.0}));
        } catch (DENOPTIMException e) {
            // This cannot happen unless there are unforeseen changed to APClass
            // basename syntax.
            throw new IllegalArgumentException(
                "Illegal APClass built form '" 
                + PLACEHOLDERAPCLASSNAME + "'"
                + " with apId=" + apId + ". "
                + "Report this to the developers.", e);
        }
        placeHolderFrag.setVertexId(GraphUtils.getUniqueVertexIndex());
        return placeHolderFrag;
    }

//------------------------------------------------------------------------------

}
