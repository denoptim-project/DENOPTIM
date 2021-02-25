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

package serconverter;

import java.util.ArrayList;

import org.openscience.cdk.interfaces.IAtomContainer;

import java.io.File;

import denoptim.utils.DENOPTIMMoleculeUtils;
import denoptim.utils.GenUtils;
import denoptim.utils.GraphConversionTool;
import denoptim.io.DenoptimIO; 
import denoptim.molecule.DENOPTIMGraph;
import denoptim.threedim.TreeBuilder3D;
import denoptim.exception.DENOPTIMException;
import denoptim.fitness.FitnessParameters;
import denoptim.fragspace.FragmentSpace;

/**
 * Conversion tool for serialized <code>DENOPTIMGraph</code>s orjects.
 * @author Marco Foscato
 */

public class SerConverter
{

//------------------------------------------------------------------------------    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        if (args.length < 1)
        {
            System.err.println("Usage: java -jar SerConverter.jar paramsFile");
            System.exit(-1);
        }

        try
        {
		    SerConvParameters.readParameterFile(args[0]);
		    SerConvParameters.checkParameters();
		    SerConvParameters.processParameters();
	           
	        DENOPTIMGraph graph = DenoptimIO.deserializeDENOPTIMGraph(
	        		new File(SerConvParameters.inpFile));
		    
		    switch (SerConvParameters.outFormat)
		    {
			case "TXT":
			    String str = graph.toString();
			    DenoptimIO.writeData(SerConvParameters.outFile,str,false);
			    break;
			case "SDF":
	            // Prepare molecular representation
	        	IAtomContainer mol;
	        	if (SerConvParameters.make3DTree)
	        	{
		        	try {
		        		TreeBuilder3D tb3d = new TreeBuilder3D();
		                mol = tb3d.convertGraphTo3DAtomContainer(graph,true);
		        	} catch (Throwable t) {
		        		System.out.println("WARNING! Failed to build 3D-tree "
		        				+ "like molecular gepmetry. Hind on cause: " 
		        				+ t.getMessage());
		        		mol = GraphConversionTool.convertGraphToMolecule(graph, 
		        				true);
		        	}
	        	} else {
	        		mol = GraphConversionTool.convertGraphToMolecule(graph, 
	        				true);
	        	}
	        	
			    DenoptimIO.writeMolecule(SerConvParameters.outFile, mol, false);
			    break;
		    }
        }
        catch (DENOPTIMException de)
        {
            GenUtils.printExceptionChain(de);
            System.exit(-1);
        }

        System.exit(0);
    }

//------------------------------------------------------------------------------

}
