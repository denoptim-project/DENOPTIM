/*
 *   DENOPTIM
 *   Copyright (C) 2019 Marco Foscato <marco.foscato@uib.no>
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

package checkpointreader;

import denoptim.files.FileUtils;
import fragspaceexplorer.FSECheckPoint;
import fragspaceexplorer.FSEUtils;


/**
 * Conversion tool for serialized <code>FSECheckPoint</code>.
 * @author Marco Foscato
 */

public class CheckpointReader
{

//------------------------------------------------------------------------------    
    /**
     * @param args the command line arguments. For now only the name of the 
     * checkpoint file to read.
     */
    public static void main(String[] args)
    {
        if (args.length < 1)
        {
            System.err.println("Usage: java -jar ChechpointReader.jar <filename>.chk");
            System.exit(-1);
	}

	String fileName = args[0];
	if (FileUtils.checkExists(fileName))
	{
	    try	
	    {
	        FSECheckPoint chk = FSEUtils.deserializeCheckpoint(fileName);
	        System.out.println(" ");
	        System.out.println(chk);
	        System.out.println(" ");
	    }
	    catch (Throwable t)
	    {
		System.err.println("ERROR! Unable to deserialize checkpoint.");
		t.printStackTrace();
	    }
	}
	else
	{
	    System.err.println("ERROR! File '" + fileName + "' not found!");
	    System.exit(-1);
	}

        System.exit(0);
    }

//------------------------------------------------------------------------------

}
