/*
 *   DENOPTIM
 *   Copyright (C) 2025 Marco Foscato <marco.foscato@uib.no>
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

package denoptim.integration.rcoserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.vecmath.Point3d;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import denoptim.exception.DENOPTIMException;
import denoptim.graph.rings.RingClosingAttractor;
import denoptim.integration.tinker.TinkerUtils;
import denoptim.molecularmodeling.ChemicalObjectModel;
import denoptim.molecularmodeling.zmatrix.ZMatrix;
import denoptim.molecularmodeling.zmatrix.ZMatrixAtom;
import denoptim.utils.ObjectPair;


/**
 * Sends the request to produce a socket server running the RingClosingMM service.
 * This performs conformational search, possibly biased to induce ring-closing conformations.
 * The service is provided by the <a href="https://github.com/denoptim-project/RingClosingMM">RingClosingMM</a> 
 * socket server, which we assume to be running.
 * This class follows the singleton pattern.
 */

public class RCOSocketServerClient
{
    /**
     * Singleton instance
     */
    private static RCOSocketServerClient instance = null;
    
    /**
     * Version identifier
     */
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

//------------------------------------------------------------------------------
    
    /**
     * Private constructor for singleton pattern.
     */
    private RCOSocketServerClient(String hostname, Integer port) {
        this.hostname = hostname;
        this.port = port;
    }

//------------------------------------------------------------------------------

    /**
     * Gets the singleton instance of RCOSocketServerClient.
     * @param hostname The hostname or IP address of the socket server
     * @param port The port number of the socket server
     * @return The singleton instance
     */
    public static synchronized RCOSocketServerClient getInstance(String hostname, 
            Integer port) 
    {
        if (instance == null) {
            instance = new RCOSocketServerClient(hostname, port);
        }
        return instance;
    }

//------------------------------------------------------------------------------

    /**
     * Gets the singleton instance of RCOSocketServerClient if it has been 
     * initialized.
     * @return The singleton instance
     * @throws IllegalStateException if the instance has not been initialized 
     * yet.
     */
    public static RCOSocketServerClient getInstance() 
    {
        if (instance == null) {
            throw new IllegalStateException(
                    "RCOSocketServerClient has not been initialized. "
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
     * Runs a conformational optimization using the services provided by the 
     * socket server configured for this instance. This wrapper does not 
     * consider the possibility to close rings defined by 
     * {@link RingClosingAttractor} combinations.
     * @param chemObj the definition of the chemical system to work with.
     * @param logger logging tool
     * @throws IOException
     * @throws JsonSyntaxException
     * @throws DENOPTIMException
     */
    public void runConformationalOptimization(ChemicalObjectModel chemObj,
            Logger logger)
                    throws IOException, JsonSyntaxException, DENOPTIMException
    {
        runConformationalOptimization(chemObj, null, logger);
    }
        
//------------------------------------------------------------------------------

    /**
     * Runs a conformational optimization using the services provided by the 
     * socket server configured for this instance.
     * @param chemObj the definition of the chemical system to work with.
     * @param rcaCombination if not <code>null</code> and not empty, biases
     * the conformational search towards conformations closing the rings defined
     * by each given pair of {@link RingClosingAttractor}s.
     * @param logger logging tool
     * @throws IOException
     * @throws JsonSyntaxException
     * @throws DENOPTIMException
     */
    public void runConformationalOptimization(ChemicalObjectModel chemObj, 
            Set<ObjectPair> rcaCombination, Logger logger)
                    throws IOException, JsonSyntaxException, DENOPTIMException
    {
    	if (chemObj.getNumberRotatableBonds() == 0)
    	{
    	    logger.log(Level.FINE, "No rotatable bond: skipping "
    	            + " conformational search.");
    	    return;
    	}
    	
        List<int[]> rcpTerms = new ArrayList<int[]>();
        if (rcaCombination!=null && rcaCombination.size()>0)
        {
            for (ObjectPair op : rcaCombination)
            {
                RingClosingAttractor rcaA = (RingClosingAttractor) op.getFirst();
                RingClosingAttractor rcaB = (RingClosingAttractor) op.getSecond();
                int iZMatRcaA = chemObj.getZMatIdxOfRCA(rcaA);
                int iZMatRcaB = chemObj.getZMatIdxOfRCA(rcaB);
                int iZMatSrcA = chemObj.getZMatIdxOfRCASrc(rcaA);
                int iZMatSrcB = chemObj.getZMatIdxOfRCASrc(rcaB);
                rcpTerms.add(new int[] {iZMatRcaA, iZMatSrcB});
                rcpTerms.add(new int[] {iZMatRcaB, iZMatSrcA});
            }
        }

        List<int[]> rotatableBonds = new ArrayList<int[]>();
        for(ObjectPair bondedAtms : chemObj.getRotatableBonds())
        {
            int t1 = ((Integer)bondedAtms.getFirst()).intValue();
            int t2 = ((Integer)bondedAtms.getSecond()).intValue();
            rotatableBonds.add(new int[] {t1, t2});
        }

        List<int[]> allBonds = new ArrayList<int[]>();
        for(int[] bondedAtmIds : chemObj.getZMatrix().getBondData())
        {
            int t1 = bondedAtmIds[0];
            int t2 = bondedAtmIds[1];
            allBonds.add(new int[] {t1, t2});
        }
        
        String requestAsJSONString = formulateRequest(chemObj.getZMatrix(), 
                rcpTerms, rotatableBonds, allBonds);
        
        //This might be useful for debugging to get the actual request placed to the server
        //TODO-gg comment out
        System.out.println(requestAsJSONString);
        //TinkerUtils.writeTinkerINT("/tmp/zmat.int", chemObj.getZMatrix());
        
        JsonObject answer = sendRequest(requestAsJSONString);
        for (String requiredMember : new String[] {"Cartesian_coordinates", "zmatrix"})
        {
            if (!answer.has(requiredMember))
            {
                throw new Error("Socket server replied without "
                        + "including '" + requiredMember + "' member. " 
                        + "Aborting! " + answer.toString());
            }
        }

        // Update local molecular representation with output from the conf. search
        JsonArray cartesianCoordinates = answer.get(
                "Cartesian_coordinates").getAsJsonArray();
        List<Point3d> newCoordinates = new ArrayList<>();
        for (int i = 0; i < cartesianCoordinates.size(); i++)
        {
            JsonArray element = cartesianCoordinates.get(i).getAsJsonArray();
            newCoordinates.add(new Point3d(
                    element.get(0).getAsDouble(), 
                    element.get(1).getAsDouble(), 
                    element.get(2).getAsDouble()));
        }
        chemObj.updateXYZ(newCoordinates);
        
        JsonArray zmatrixArrayAsJson = answer.get("zmatrix").getAsJsonArray();
        for (int i = 0; i < zmatrixArrayAsJson.size(); i++)
        {
            JsonElement element = zmatrixArrayAsJson.get(i);
            JsonObject zmatrixObject = element.getAsJsonObject();
            ZMatrixAtom zatm = chemObj.getZMatrix().getAtom(i);
            // NB: no change is expected on the reference atoms used to define
            // the internal coordinates.
            if (i>0)
            {
                zatm.setBondLength(
                    zmatrixObject.get("bond_length").getAsDouble());
                if (i>1)
                {
                    zatm.setAngleValue(
                            zmatrixObject.get("angle").getAsDouble());
                    if (i>3)
                    {
                        zatm.setAngle2Value(
                            zmatrixObject.get("dihedral").getAsDouble());
                        zatm.setChiralFlag(
                            zmatrixObject.get("chirality").getAsInt());
                    }
                }
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Formulates the request to be sent to the socket server. Since the socket 
     * works with chemical conventions, not with Java or Python conventions,
     * the request has to be formulated with 1-based indexing. This method takes
     * care of
     * @param zmat the zMatrix representation
     * @param rcpTerms The list of ring-closing potential energy terms (RCP terms)
     * (0-based)
     * @param rotatableBonds The list of bonds that can be twisted. Each bond 
     * is defined by the two 0-based indexes of the bonded atoms.
     * @param allBonds The list of all bonds. Each bond 
     * is defined by the two 0-based indexes of the bonded atoms.
     * @return The request as a JSON string.
     */
    public String formulateRequest(ZMatrix zmat, List<int[]> rcpTerms, 
            List<int[]> rotatableBonds, List<int[]> allBonds) throws IOException
    {    
        JsonObject jsonObj = new JsonObject();
        jsonObj.add("zmatrix", getZMatrixAsJsonArray(zmat));
        jsonObj.add("rcp_terms", convertIntArrayListToJsonArray(rcpTerms));
        jsonObj.add("rotatable_bonds", convertIntArrayListToJsonArray(rotatableBonds));
        jsonObj.add("bonds_data", convertIntArrayListToJsonArray(allBonds));
        jsonObj.addProperty("version", version);
        
        return jsonConverter.toJson(jsonObj);
    }

//------------------------------------------------------------------------------

    /**
     * Gets a JsonArray representation of the Z-matrix. Note that this is 
     * meant to produce 1-based indices for the atoms.
     * @param zmat The TinkerMolecule object to be processed.
     * @return the Z-matrix as a JsonArray.
     */
    public static JsonArray getZMatrixAsJsonArray(ZMatrix zmat)
    {
        JsonArray zmatrixArray = new JsonArray();
        for(int i = 0; i<zmat.getAtomCount(); i++)
        {
            ZMatrixAtom atom = zmat.getAtom(i);
            JsonObject atomObj = new JsonObject();
            atomObj.addProperty("id", atom.getId()+1);
            atomObj.addProperty("element", atom.getSymbol());
            if (zmat.getBondRefAtomIndex(i) > -1)
            {
                atomObj.addProperty("bond_ref", zmat.getBondRefAtomIndex(i)+1);
            }
            if (zmat.getBondLength(i) != null)
            {
                atomObj.addProperty("bond_length", zmat.getBondLength(i));
            }
            if (zmat.getAngleRefAtomIndex(i) > -1)
            {
                atomObj.addProperty("angle_ref", zmat.getAngleRefAtomIndex(i)+1);
            }
            if (zmat.getAngleValue(i) != null)
            {
                atomObj.addProperty("angle", zmat.getAngleValue(i));
            }
            if (zmat.getAngle2RefAtomIndex(i) > -1)
            {
                atomObj.addProperty("dihedral_ref", zmat.getAngle2RefAtomIndex(i)+1);
            }
            if (zmat.getAngle2Value(i) != null)
            {
                atomObj.addProperty("dihedral", zmat.getAngle2Value(i));
            }
            if (zmat.getChiralFlag(i) != null)
            {
                atomObj.addProperty("chirality", zmat.getChiralFlag(i));
            }
            zmatrixArray.add(atomObj);
        }
        return zmatrixArray;
    }

//------------------------------------------------------------------------------

    /**
     * Converts a List of 0-based int arrays to a JsonArray containing 1-based
     * ints.
     * @param intArrayList The list of int arrays to convert
     * @return A JsonArray where each element is a JsonArray of integers
     */
    private static JsonArray convertIntArrayListToJsonArray(List<int[]> intArrayList)
    {
        JsonArray jsonArray = new JsonArray();
        for (int[] intArray : intArrayList)
        {
            JsonArray innerArray = new JsonArray();
            for (int value : intArray)
            {
                innerArray.add(value+1);
            }
            jsonArray.add(innerArray);
        }
        return jsonArray;
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
            if (!(answer.has("STATUS")))
            {

                throw new Error("Socket server replied without "
                        + "including " + "STATUS" + " member. " 
                        + "Something is badly wrong: aborting! " + answer.toString());
            }
            if (!answer.get("STATUS").getAsString().equals("SUCCESS"))
            {
                throw new Error("Socket server replied but with STATUS=" 
                        + answer.get("STATUS") + ". " 
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
