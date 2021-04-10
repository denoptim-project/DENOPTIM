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

package isomorphism;

import java.io.File;

import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.utils.GenUtils;
import denoptim.utils.GraphConversionTool;
import denoptim.utils.MutationType;
import denoptimga.DENOPTIMGraphOperations;

/**
 * Tool to test perform isomorphism analysis on DENOPTIMGreaphs.
 *
 * @author Marco Foscato
 */

public class Isomorphism
{

//------------------------------------------------------------------------------    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        if (args.length < 1)
        {
            System.err.println("Usage: java -jar Isomorphism.jar paramsFile");
            System.exit(-1);
        }

        try
        {
            IsomorphismParameters.readParameterFile(args[0]);
            IsomorphismParameters.checkParameters();
            IsomorphismParameters.processParameters();
            
            checkIsomorphism();
            
            System.out.println("Isomorphism run completed");
        }
        catch (DENOPTIMException de)
        {
            GenUtils.printExceptionChain(de);
            System.exit(-1);
        }

        System.exit(0);
    }

//------------------------------------------------------------------------------
    
    private static void checkIsomorphism() throws DENOPTIMException
    {
        DENOPTIMGraph graphA = null;
        DENOPTIMGraph graphB = null;
        try
        {
            graphA = DenoptimIO.readDENOPTIMGraphsFromFile(
                    new File(IsomorphismParameters.inpFileGraphA),true).get(0);
            graphB = DenoptimIO.readDENOPTIMGraphsFromFile(
                    new File(IsomorphismParameters.inpFileGraphB),true).get(0);
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        System.out.println("Checking for isomorphism between ");
        System.out.println(" -> GraphA: "+graphA);
        System.out.println(" -> GraphB: "+graphB);
    
        if (graphA.isIsomorphicTo(graphB))
        {
            System.out.println("Graphs are DENOPTIM-isomorphic!");
        } else {
            System.out.println("No DENOPTIM-isomorphism found.");
        }
    }

//------------------------------------------------------------------------------

}
