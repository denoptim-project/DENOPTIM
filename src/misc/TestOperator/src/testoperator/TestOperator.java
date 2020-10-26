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

import denoptim.utils.GenUtils;
import denoptim.utils.GraphUtils;
import denoptim.utils.GraphConversionTool;
import denoptim.io.DenoptimIO; 
import denoptim.molecule.DENOPTIMGraph;
import denoptim.exception.DENOPTIMException;
import denoptimga.DENOPTIMGraphOperations;
import denoptimga.EAUtils;

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
            
            switch (TestOperatorParameters.operatorToTest)
            {
                case XOVER:
                    runXOver();
                    break;
                case MUTATION:
                    runMutation();
                    break;
            }
         
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
    
    //TODO-V3
    private static void runMutation() throws DENOPTIMException
    {
        DENOPTIMGraph g = 
                GraphConversionTool.getGraphFromString(DenoptimIO.readSDFFile(
                                       TestOperatorParameters.inpFileM).get(0)
                                         .getProperty("GraphENC").toString());

    
        System.out.println("Initial graphs: ");
        System.out.println(g);
        System.out.println(" ");
    
        EAUtils.performMutation(g);
        
        //TODO-V3 need a way to control what fragment will be introduced
    }
    
//------------------------------------------------------------------------------
    
    private static void runXOver() throws DENOPTIMException
    {
        
        DENOPTIMGraph male = 
                GraphConversionTool.getGraphFromString(DenoptimIO.readSDFFile(
                                       TestOperatorParameters.inpFileM).get(0)
                                         .getProperty("GraphENC").toString());
        DENOPTIMGraph female = 
                GraphConversionTool.getGraphFromString(DenoptimIO.readSDFFile(
                                       TestOperatorParameters.inpFileF).get(0)
                                         .getProperty("GraphENC").toString());
    
        System.out.println("Initial graphs: ");
        System.out.println("v:"+TestOperatorParameters.mvid
                             +" of MALE: "+male);
        System.out.println(" ");
        System.out.println("v:"+TestOperatorParameters.fvid
                             +" of FEMALE: "+female);
        System.out.println(" ");
    
        // Remember position of vertex chosen for xover
        int ivmale = male.getIndexOfVertex(TestOperatorParameters.mvid);
        int ivfemale = female.getIndexOfVertex(TestOperatorParameters.fvid);
             // Ensure uniqueness on vertexID
        male.renumberGraphVertices();
        male.renumberGraphVertices();
    
        // Get new vid of chosen frags
        int newmvid = male.getVertexAtPosition(ivmale).getVertexId();
        int newfvid = female.getVertexAtPosition(ivfemale).getVertexId();
    
        // do crossover
        System.out.println("Initial graphs now with unique vertexID: ");
        System.out.println("v:"+TestOperatorParameters.mvid+" (now:"+newmvid
                             +") of MALE: "+male);
        System.out.println(" ");
        System.out.println("v:"+TestOperatorParameters.fvid+" (now:"+newfvid
                             +") of FEMALE: "+female);
        System.out.println(" ");
    
        DENOPTIMGraphOperations.performCrossover(male,newmvid,
                                                   female,newfvid,true);
    
        System.out.println("Result of crossover:");
        System.out.println(" ");
        System.out.println("MALE: "+male);
        System.out.println("FEMALE: "+female);
        System.out.println(" ");
    
        DenoptimIO.writeMolecule(TestOperatorParameters.outFileM, 
                 GraphConversionTool.convertGraphToMolecule(male,true),false);
        DenoptimIO.writeMolecule(TestOperatorParameters.outFileF, 
               GraphConversionTool.convertGraphToMolecule(female,true),false);
    }

//------------------------------------------------------------------------------

}
