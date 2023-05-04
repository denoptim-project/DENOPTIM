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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.smiles.SmilesParser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

/**
 * Unit test for descriptor SocketProvidedDescriptor.
 * 
 * @author Marco Foscato
 */

public class SocketProvidedDescriptorTest
{
    private SocketProvidedDescriptor descriptor;
    private MySocketServer server;
    private Gson jsonConverted;
    
    private static final String HOSTNAME = "localhost";
    

//------------------------------------------------------------------------------
    
    @BeforeEach
    public void setUpSertver() throws Exception
    {
        // We create a socket server the SocketProvidedDescriptor can talk to.
        server = new MySocketServer();
        server.startServer();
        int port = server.getPort();
        
        jsonConverted = new GsonBuilder().create();
        
        Constructor<SocketProvidedDescriptor> defaultConstructor = 
                SocketProvidedDescriptor.class.getConstructor();
        this.descriptor = defaultConstructor.newInstance();
        this.descriptor.setParameters(new Object[] {HOSTNAME, port});
    }

//------------------------------------------------------------------------------
    
    @AfterEach
    public void ClossServer() throws Exception
    {
        server.stopServer();
    }

//------------------------------------------------------------------------------

    /*
     * The dummy server we create and try to communicate with.
     */
    private class MySocketServer extends Thread
    {
        private ServerSocket server;

        public void startServer()
        {
            try
            {
                server = new ServerSocket(0);
                
                Runtime.getRuntime().addShutdownHook(new Thread(){
                    public void run()
                    {
                        try {
                            server.close();
                        } catch (IOException e) { /* failed */ }
                    }
                });
                this.start();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        
        public int getPort()
        {
            return server.getLocalPort();
        }

        public void stopServer()
        {
            try
            {
                server.close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
            this.interrupt();
        }

        @Override
        public void run()
        {
            while(!server.isClosed())
            {
                try
                {
                    RequestHandler handler = new RequestHandler(
                            server.accept());
                    handler.start();
                }
                catch (IOException e)
                {
                    // There is some lag between the closing of the server and
                    // the notification it is closed, se we end up here when
                    // the unit test if finishing and we are closing up the 
                    // socket.
                    //e.printStackTrace();
                }
            }
        }
    }

    /*
     * Thread handling any requests on the server side.
     */
    class RequestHandler extends Thread
    {
        private Socket socket;
        RequestHandler(Socket socket)
        {
            this.socket = socket;
        }

        @Override
        public void run()
        {
            try
            {
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));
                
                // Read request
                StringBuilder sb = new StringBuilder();
                String line = in.readLine();
                while(line != null && line.length()>0)
                {
                    sb.append(line).append(System.getProperty("line.separator"));
                    line = in.readLine();
                }
                
                //
                // WARNING: this is where the format of the request is evaluated.
                // Any change to the format convention should be reflected here.
                //
                
                // Evaluate request format
                String jsonStr = sb.toString();
                JsonObject request = null;
                try {
                    request = jsonConverted.fromJson(jsonStr, JsonObject.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                    assertFalse(true,"JsonSyntaxException unpon converting "
                            + "requst to socket server");
                }
                String smiKey = SocketProvidedDescriptor.KEYJSONMEMBERSMILES;
                assertTrue(request.has(smiKey), "JSON request has no " + smiKey);
                
                //
                // WARNING: this assumes consistency between this class and the 
                // unit test method.
                //
                
                // Prepare an answer that differs based on the request
                String smiles = request.get(smiKey).getAsString();
                long count = smiles.chars().filter(c -> c == 'c').count();
                double score = Math.pow((double) count,2.5);
                
                JsonObject jsonAnswer = new JsonObject();
                if (score>0.1)
                {
                    jsonAnswer.addProperty(
                            SocketProvidedDescriptor.KEYJSONMEMBERSCORE, score);
                } else {
                    jsonAnswer.addProperty(
                            SocketProvidedDescriptor.KEYJSONMEMBERERR,
                            "#SocketServer: fake error.");
                }
                
                // Send response
                PrintWriter out = new PrintWriter(socket.getOutputStream());
                out.println(jsonConverted.toJson(jsonAnswer));
                out.flush();
                
                // Do not close our connection: we'll be reusing it
                //in.close();
                //out.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
    
//------------------------------------------------------------------------------
		
	@Test
	public void testSocketProvidedDescriptor() throws Exception
	{
        SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        
        String smiles = "O";
        IAtomContainer mol1 = sp.parseSmiles(smiles);
        mol1.setProperty("SMILES", smiles);
        double value = ((DoubleResult) descriptor.calculate(mol1).getValue())
                .doubleValue();
        double expected = Double.NaN;
        assertTrue(Double.isNaN(value), "Wrong socket-provided "
                + "descriptor: expected " + expected + ", found "+ value + "(0)");
        
        mol1.setProperty("SMILES", "c");
        value = ((DoubleResult) descriptor.calculate(mol1).getValue())
                .doubleValue();
        expected = 1.0;
        assertTrue(closeEnough(expected, value), "Wrong socket-provided "
                + "descriptor: expected " + expected + ", found "+ value + "(1)");
        
        mol1.setProperty("SMILES", "ccc");
        value = ((DoubleResult) descriptor.calculate(mol1).getValue())
                .doubleValue();
        expected = 15.5884;
        assertTrue(closeEnough(expected, value), "Wrong socket-provided "
                + "descriptor: expected " + expected + ", found "+ value + "(2)");
        
        smiles = "COc1ccccc1";
        mol1 = sp.parseSmiles(smiles);
        mol1.setProperty("SMILES", smiles);
        value = ((DoubleResult) descriptor.calculate(mol1).getValue())
                .doubleValue();
        expected = 88.1816;
        assertTrue(closeEnough(expected, value), "Wrong socket-provided "
                + "descriptor: expected " + expected + ", found "+ value + "(3)");
	}
	
//------------------------------------------------------------------------------
	
	private boolean closeEnough(double expected, double actual)
	{
	    double threshold = 0.01;
	    double delta = Math.abs(expected-actual);
	    return delta < threshold;
	}
 
//------------------------------------------------------------------------------

}
