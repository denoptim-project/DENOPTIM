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

package testoperator;

import java.util.ArrayList;
import java.io.File;

import utils.GenUtils;
import utils.GraphUtils;
import utils.GraphConversionTool;
import io.DenoptimIO; 
import molecule.DENOPTIMGraph;
import exception.DENOPTIMException;
import denoptimga.DENOPTIMGraphOperations;

/**
 * Tool to test genetic operators
 *
 * @author Marco Foscato
 */

public class TestOperator
{

//------------------------------------------------------------------------------    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        if (args.length < 1)
        {
            System.err.println("Usage: java -jar TestOperator.jar paramsFile");
            System.exit(-1);
        }

        try
        {
            TestOperatorParameters.readParameterFile(args[0]);
            TestOperatorParameters.checkParameters();
            TestOperatorParameters.processParameters();
           
            DENOPTIMGraph male = 
                  GraphConversionTool.getGraphFromString(DenoptimIO.readSDFFile(
                                         TestOperatorParameters.inpFileM).get(0)
                                           .getProperty("GraphENC").toString());
            DENOPTIMGraph female = 
                  GraphConversionTool.getGraphFromString(DenoptimIO.readSDFFile(
                                         TestOperatorParameters.inpFileF).get(0)
                                           .getProperty("GraphENC").toString());

// TODO: adapt for general operations

            System.out.println("Testing crossover between: ");
            System.out.println(" ");
            System.out.println("v:"+TestOperatorParameters.mvid
                               +" of MALE: "+male);
            System.out.println(" ");
            System.out.println("v:"+TestOperatorParameters.fvid
                               +" of FEMALE: "+female);
            System.out.println(" ");

            // Remember position of vertex chosen for xover
            int ivmale = male.getIndexOfVertex(TestOperatorParameters.mvid);
            int ivfemale = female.getIndexOfVertex(TestOperatorParameters.fvid);

            // Ensure uniqeness on vertexID
            GraphUtils.renumberGraphVertices(male);
            GraphUtils.renumberGraphVertices(female);

            // Get new vid of chosen frags
            int newmvid = male.getVertexAtPosition(ivmale).getVertexId();
            int newfvid = female.getVertexAtPosition(ivfemale).getVertexId();

            // do crossover
            System.out.println("Testing crossover between: ");
            System.out.println(" ");
            System.out.println("v:"+TestOperatorParameters.mvid+" (now:"+newmvid
                               +") of MALE: "+male);
            System.out.println(" ");
            System.out.println("v:"+TestOperatorParameters.fvid+" (now:"+newfvid
                               +") of FEMALE: "+female);
            System.out.println(" ");

            DENOPTIMGraphOperations.performCrossover(male,newmvid,
                                                     female,newfvid);

            System.out.println("Result of crossover:");
            System.out.println(" ");
            System.out.println("MALE: "+male);
            System.out.println("FEMALE: "+female);
            System.out.println(" ");

            DenoptimIO.writeMolecule(TestOperatorParameters.outFileM, 
                   GraphConversionTool.convertGraphToMolecule(male,true),false);
            DenoptimIO.writeMolecule(TestOperatorParameters.outFileF, 
                 GraphConversionTool.convertGraphToMolecule(female,true),false);

            System.out.println("TestOperator run completed");
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
