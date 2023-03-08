/*
 *   DENOPTIM
 *   Copyright (C) 2022 Marco Foscato <marco.foscato@uib.no>
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

package denoptim.fitness.descriptors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.qsar.AbstractMolecularDescriptor;
import org.openscience.cdk.qsar.DescriptorSpecification;
import org.openscience.cdk.qsar.DescriptorValue;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.qsar.result.DoubleResultType;
import org.openscience.cdk.qsar.result.IDescriptorResult;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import denoptim.fitness.IDenoptimDescriptor;


/**
 * Sends the request to produce a numerical descriptor to a defined socket and 
 * receives back the response. JSON format is used for communicating information
 * in both directions. This class follows this convention:
 * <ul>
 * <li>sends JSON string with member {@value #KEYJSONMEMBERSMILES} containing the 
 * SMILES of the candidate. The string is terminated by newline.</li>
 * <li>expects to receive a JSON with either member {@value #KEYJSONMEMBERSCORE}
 * containing one descriptor score as a float (double), or 
 * {@value KEYJSONMEMBERERR} containing any error occurred on the server side.
 * </li>
 * </ul>
 */

// WARNING: any change to the format convention for the communication to the 
// socket server must be reflected in the RequestHandler of the unit test.

public class SocketProvidedDescriptor extends AbstractMolecularDescriptor 
implements IMolecularDescriptor, IDenoptimDescriptor
{

    /**
     * The key of the JSON member defining the SMILES of the candidate for which
     * the socket server should produce descriptor.
     */
    public final static String KEYJSONMEMBERSMILES = "SMILES";
    
    /**
     * The key of the JSON member defining the score/s for the descriptor
     * calculated
     */
    public final static String KEYJSONMEMBERSCORE = "SCORE";
    
    /**
     * The key of the JSON member defining an error in the calculation of the 
     * score.
     */
    public final static String KEYJSONMEMBERERR = "ERROR";
    
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
     * Name of the input parameters
     */
    private static final String[] PARAMNAMES = new String[] {
            "hostname","port"};

    /**
     * NAme of the descriptor produced by this class
     */
    private static final String[] NAMES  = {"SocketProvidedDescriptor"};

//------------------------------------------------------------------------------
    
    /**
     * Constructor for a SocketProvidedDescriptor object
     */
    public SocketProvidedDescriptor() {}

//------------------------------------------------------------------------------
      
    /**
     * Get the specification attribute of socket-based descriptor provider.
     * @return the specification of this descriptor.
     */
    @Override
    public DescriptorSpecification getSpecification()
    {
        String paramID = ""; 
        if (hostname!=null && port!=null)
        {
            paramID = "" + hostname + port;
        }
        return new DescriptorSpecification("Denoptim source code", 
                this.getClass().getName(), paramID, "DENOPTIM project");
    }

//------------------------------------------------------------------------------
    
    /**
     * Gets the parameterNames attribute of the TanimotoMolSimilarity object.
     * @return the parameterNames value
     */
    @Override
    public String[] getParameterNames() {
        return PARAMNAMES;
    }

//------------------------------------------------------------------------------
    
    /** {@inheritDoc} */
    @Override
    public Object getParameterType(String name)
    {
        if (name.equals(PARAMNAMES[1])) //port
        {
            return 0;
        } else if (name.equals(PARAMNAMES[0])) // hostname
        {
            return "";
        } else {
            throw new IllegalArgumentException("No parameter for name: "+name);
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Set the parameters attributes.
     * The descriptor takes two parameters: the host name and the port number.
     * @param params the array of parameters
     */
    @Override
    public void setParameters(Object[] params) throws CDKException
    {
        if (params.length != 2)
        {
            throw new IllegalArgumentException("SocketProvidedDescriptor only "
                    + "expects two parameter");
        }
        if (!(params[0] instanceof String))
        {
            throw new IllegalArgumentException("Parameter is not String (" 
                    + params[0].getClass().getName() + ").");
        }
        if (!(params[1] instanceof Integer))
        {
            throw new IllegalArgumentException("Parameter is not Integer (" 
                    + params[0].getClass().getName() + ").");
        }

        hostname = (String) params[0];
        port = (Integer) params[1];
    }

//------------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public Object[] getParameters()
    {
        Object[] params = new Object[2];
        params[0] = hostname;
        params[1] = port;
        return params;
    }

//------------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String[] getDescriptorNames()
    {
        return NAMES;
    }

//------------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public DescriptorValue calculate(IAtomContainer mol)
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
        
        JsonObject jsonObj = new JsonObject();
        Object smilesProp = mol.getProperty("SMILES");
        if (smilesProp==null)
        {
            try
            {
                socket.close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
            throw new IllegalArgumentException("AtomContainers fed to " 
                    + this.getClass().getName() + " are expected to contain "
                            + "property '" + KEYJSONMEMBERSMILES 
                            + "', but it was not found.");
        }
        jsonObj.addProperty("SMILES", smilesProp.toString());
        
        // We use this because we one a "ugly" json (i.e., not pretty-printed, 
        // i.e., using indentation and multiple lines).
        Gson jsonConverted = new GsonBuilder().create();
        
        // Here we send the request to the socket
        writerToSocket.println(jsonConverted.toJson(jsonObj) 
                + System.getProperty("line.separator"));
        
        JsonObject answer = null;
        DoubleResult result;
        try {
            answer = jsonConverted.fromJson(readerFromSocket.readLine(), 
                    JsonObject.class);
            if (answer.has(KEYJSONMEMBERSCORE))
            {
                double value = Double.parseDouble(
                        answer.get(KEYJSONMEMBERSCORE).toString());
                result = new DoubleResult(value);
            } else if (answer.has(KEYJSONMEMBERERR)) {
                //TODO-gg use log
                //System.err.println(KEYJSONMEMBERERR + " from socket server.");
                result = new DoubleResult(Double.NaN);
            } else {
                //TODO-gg use log
                /*
                System.err.println("Socket server replied without providing "
                        + "neither " + KEYJSONMEMBERSCORE + " nor "
                        + KEYJSONMEMBERERR + " member.");
                */
                result = new DoubleResult(Double.NaN);
            }
        } catch (JsonSyntaxException | IOException e) {
            e.printStackTrace();
            result = new DoubleResult(Double.NaN);
        }
        
        try
        {
            socket.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        
        return new DescriptorValue(getSpecification(),
                getParameterNames(),
                getParameters(),
                result,
                getDescriptorNames());
    }

//------------------------------------------------------------------------------
   
    /** {@inheritDoc} */
    @Override
    public IDescriptorResult getDescriptorResultType()
    {
        return new DoubleResultType();
    }

//------------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String getDictionaryTitle()
    {
        return "Socket-provided descriptor";
    }
    
//------------------------------------------------------------------------------
    
    /** {@inheritDoc} */
    @Override
    public String getDictionaryDefinition()
    {
        return "We ignore how the descriptor is calculated. We only know we "
                + "can ask a socket server for a score. The connection can be "
                + "parametrized so that we can define that a server is "
                + "reachable at host name <code>" + PARAMNAMES[0] + "</code>, "
                + "and port <code>" + PARAMNAMES[1] + "</code>. By convention, "
                + "the communication deploys a JSON string terminated by a "
                + "new-line character. Such format is expected for both "
                + "the request for a score (the request generated by "
                + "DENOPTIM) and the answer to such request (produced by the "
                + "server). The requst contains always the SMILES of the "
                + "candidate (e.g., "
                + "<code>{\"SMILES\": \"CCO\"}\\n</code>). The answer may "
                + "contain a <code>SCORE</code> or an <code>ERROR</code> "
                + "(e.g., <code>{\"SCORE\": 1.23}\\n</code>). Failure in the "
                + "communication protocol will produce a <code>NaN</code> "
                + "score.";
    }

//------------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String[] getDictionaryClass()
    {
        return new String[] {"molecular"};
    }

//------------------------------------------------------------------------------

}
