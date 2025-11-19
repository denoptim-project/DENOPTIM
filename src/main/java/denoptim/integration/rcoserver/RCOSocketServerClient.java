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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import denoptim.exception.DENOPTIMException;
import denoptim.molecularmodeling.ChemicalObjectModel;
import denoptim.molecularmodeling.zmatrix.ZMatrix;
import denoptim.molecularmodeling.zmatrix.ZMatrixAtom;


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
    public static synchronized RCOSocketServerClient getInstance(String hostname, Integer port) 
    {
        if (instance == null) {
            instance = new RCOSocketServerClient(hostname, port);
        }
        return instance;
    }

//------------------------------------------------------------------------------

    /**
     * Gets the singleton instance of RCOSocketServerClient if it has been initialized.
     * @return The singleton instance
     * @throws IllegalStateException if the instance has not been initialized yet
     */
    public static RCOSocketServerClient getInstance() 
    {
        if (instance == null) {
            throw new IllegalStateException("RCOSocketServerClient has not been initialized. "
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

    public void optimizeRingClosingConformation(ChemicalObjectModel chemObj, 
        List<int[]> rcpTerms, List<int[]> rotatableBonds, Logger logger) 
        throws IOException, JsonSyntaxException, DENOPTIMException
    {
    	if (chemObj.getNumberRotatableBonds() == 0)
    	{
    	    logger.log(Level.FINE, "No rotatable bond: skipping "
    	            + " conformational search.");
    	    return;
    	}

        String requestAsJSONString = formulateRequest(chemObj.getZMatrix(), rcpTerms, rotatableBonds);
        System.out.println(requestAsJSONString);
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

        // Update local molecular representation with output from PSSROT
        JsonArray cartesianCoordinates = answer.get(
                "Cartesian_coordinates").getAsJsonArray();
        //TODO put the min the chemObj
        
        JsonArray zmatrixArray = answer.get("zmatrix").getAsJsonArray();
        for (int i = 0; i < zmatrixArray.size(); i++)
        {
            JsonElement element = zmatrixArray.get(i);
            JsonObject zmatrixObject = element.getAsJsonObject();
            ZMatrixAtom zatm = chemObj.getZMatrix().getAtom(i);
            zatm.setBondLength(
                zmatrixObject.get("bond_length").getAsDouble());
            zatm.setAngleValue(
                    zmatrixObject.get("angle").getAsDouble());
            zatm.setAngle2Value(
                zmatrixObject.get("dihedral").getAsDouble());
            zatm.setChiralFlag(
                zmatrixObject.get("chirality").getAsInt());
            // NB: no change is expected on the reference atoms used to define
            // the internal coordinates.
        }
        chemObj.updateXYZFromINT();
    }

//------------------------------------------------------------------------------

    /**
     * Formulates the request to be sent to the socket server.
     * @param zmat the zMatrix representation
     * @param rcpTerms The list of ring-closing potential energy terms (RCP terms)
     * @param rotatableBonds The list of bonds that can be twisted
     * @return The request as a JSON string.
     */
    public String formulateRequest(ZMatrix zmat, List<int[]> rcpTerms, List<int[]> rotatableBonds) throws IOException
    {
        String rcpTermsLine = "";
        for(int[] rcpTerm : rcpTerms)
        {
            rcpTermsLine = rcpTermsLine + " " + rcpTerm[0] + " " + rcpTerm[1];
        }
        String rotatableBondsLine = "";
        for(int[] rotatableBond : rotatableBonds)
        {
            rotatableBondsLine = rotatableBondsLine + " " + rotatableBond[0] + " " + rotatableBond[1];
        }
         
        JsonObject jsonObj = new JsonObject();
        jsonObj.add("zmatrix", getZMatrixAsJsonArray(zmat));
        jsonObj.addProperty("rcp_terms", rcpTermsLine);
        jsonObj.addProperty("bonds_data", rotatableBondsLine);
        jsonObj.addProperty("version", version);


        System.out.println("rcpTermsLine: " + rcpTermsLine);
        System.out.println("rotatableBondsLine: " + rotatableBondsLine);
        
        return jsonConverter.toJson(jsonObj);
    }

//------------------------------------------------------------------------------

    /**
     * Gets a JsonArray representation of the Z-matrix. Note that this is 
     * meant to produce 1-based indices for the atoms.
     * @param zmat The TinkerMolecule object to be processed.
     * @return the Z-matrix as a JsonArray.
     */
    private JsonArray getZMatrixAsJsonArray(ZMatrix zmat)
    {
        JsonArray zmatrixArray = new JsonArray();
        int i = 0;
        for(ZMatrixAtom atom : zmat.getAtoms())
        {   
            i++;
            JsonObject atomObj = new JsonObject();
            atomObj.addProperty("id", atom.getId());
            atomObj.addProperty("element", atom.getSymbol());
            atomObj.addProperty("atomi_type", atom.getType());
            atomObj.addProperty("bond_ref", zmat.getBondRefAtomIndex(i));
            atomObj.addProperty("bond_length", zmat.getBondLength(i));
            atomObj.addProperty("angle_ref", zmat.getAngleRefAtomIndex(i));
            atomObj.addProperty("angle", zmat.getAngleValue(i));
            atomObj.addProperty("dihedral_ref", zmat.getAngle2RefAtomIndex(i));
            atomObj.addProperty("dihedral", zmat.getAngle2Value(i));
            atomObj.addProperty("chirality", zmat.getChiralFlag(i));
            zmatrixArray.add(atomObj);
        }
        return zmatrixArray;
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
            if (!(answer.has("STATUS") && answer.get("STATUS").getAsString().equals("SUCCESS")))
            {
                throw new Error("Socket server replied without "
                        + "including " + "STATUS" + " member. " 
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
