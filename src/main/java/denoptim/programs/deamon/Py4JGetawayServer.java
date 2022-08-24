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

package denoptim.programs.deamon;
import denoptim.io.DenoptimIO;

import py4j.GatewayServer;

/**
 * A tool that start a Py4J gateway server that can listens to calls from 
 * Python and translate JAVA objects to make their functionality available to 
 * in the Python environment.
 * 
 * @author Marco Foscato
 */

public class Py4JGetawayServer
{
    /**
     * The actual server we launch, or null if not launched yet.
     */
    private GatewayServer server;
    
    /**
     * Pathname from which data was loaded, or null if no data has been loaded.
     */
    private String pathname = null;

    /**
     * Any DENOPTIM-related data loaded from file, if any.
     */
    private Object data = null;
  
//------------------------------------------------------------------------------
    
    /**
     * Starts a gateway server using this class as entry point, which then
     * becomes the interpreter of any data fed-in by the 
     * {@link #loadData(String)} method.
     */
    public static void launch()
    {
        Py4JGetawayServer launcher = new Py4JGetawayServer();
        GatewayServer gatewayServer = new GatewayServer(launcher);
        gatewayServer.start();
        launcher.setServer(gatewayServer);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Reads any DENOPTIM-kind of data from the given pathname.
     * @param pathname the pathname of the file to read.
     * @return the data found in the file.
     * @throws Exception if anything goes wrong with the reading of the data.
     */
    public Object loadData(String pathname) throws Exception
    {
        this.pathname = pathname;
        System.out.println("Loading data from '" + pathname + "'");
        data = DenoptimIO.readDENOPTIMData(pathname);
        return data;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return the current value of the loaded data, if any.
     */
    public Object getData()
    {
        return data;
    }

//------------------------------------------------------------------------------
    
    /**
     * @return the pathname from which data was imported, or null if no data 
     * has been loaded yet.
     */
    public String getSourcePathName()
    {
        return pathname;
    }
    
//------------------------------------------------------------------------------  

    /**
     * Stops the server.
     */
    public void shutdown()
    {
        if (server != null)
            server.shutdown();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Sets the reference to the launched server.
     * @param server
     */
    private void setServer(GatewayServer server)
    {
        this.server = server;
    }
    
//------------------------------------------------------------------------------  

}
