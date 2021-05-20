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

package fragmentnoising;

import java.util.ArrayList;

import javax.vecmath.Point3d;

import org.apache.commons.math3.random.MersenneTwister;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.io.File;

import denoptim.utils.DENOPTIMMoleculeUtils;
import denoptim.utils.FragmentUtils;
import denoptim.utils.GenUtils;
import denoptim.utils.GraphUtils;
import denoptim.utils.RandomUtils;
import denoptim.utils.GraphConversionTool;
import denoptim.io.DenoptimIO;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.exception.DENOPTIMException;
import denoptimga.DENOPTIMGraphOperations;

/**
 * Tool to add noise to fragments and attachment points
 *
 * @author Marco Foscato
 */

public class FragmentNoising
{

//------------------------------------------------------------------------------    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        if (args.length != 3)
        {
            System.err.println("Usage: java -jar FragmentNoising.jar "
            		+ "fragments.sdf amountOfnoise output.sdf");
            System.exit(-1);
        }
        
        RandomUtils.initialiseRNG(1234567890);
        MersenneTwister rng = RandomUtils.getRNG();
        
        try
        {
        	String fileName = args[0];
        	double noiseLevel = Double.parseDouble(args[1]);
        	String outfile = args[2];
        	
        	ArrayList<IAtomContainer> fragLib = 
        			DenoptimIO.readInLibraryOfFragments(fileName, "fragments");
        	
        	int idx = 0;
        	for (IAtomContainer mol : fragLib)
        	{
        		// Alter atom positions
        		for (IAtom atm : mol.atoms())
        		{
        			Point3d p = atm.getPoint3d();
        			atm.setPoint3d(new Point3d(
        					addNoise(p.x, rng, noiseLevel),
        					addNoise(p.y, rng, noiseLevel),
        					addNoise(p.z, rng, noiseLevel)));
        		}
        		
        		//TODO: altering of AP vectors
	    		
        		// Clear identifiers and comments
        		mol.removeProperty("cdk:Title");
        		mol.removeProperty("cdk:Remark");
        	}
           
        	DenoptimIO.writeMoleculeSet(outfile, fragLib);
            System.out.println("FragmentNoising run completed");
        }
        catch (DENOPTIMException de)
        {
            GenUtils.printExceptionChain(de);
            System.exit(-1);
        }

        System.exit(0);
    }

//------------------------------------------------------------------------------

    public static double addNoise(double value, MersenneTwister rng, double noiseLevel)
    {
    	double sign = rng.nextBoolean() ? -1.0 : 1.0;
    	double noisyValue = value + sign*(rng.nextDouble()*(noiseLevel));
    	return noisyValue;
    }
}
