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

import java.io.File;

import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMTemplate;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.threedim.ThreeDimTreeBuilder;
import denoptim.utils.GenUtils;
import denoptim.utils.GraphConversionTool;
import denoptim.utils.MutationType;
import denoptimga.DENOPTIMGraphOperations;
import denoptimga.Monitor;

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
            de.printStackTrace(System.err);
            System.exit(-1);
        }

        System.exit(0);
    }
    
//------------------------------------------------------------------------------
    
    private static void runMutation() throws DENOPTIMException
    {
        DENOPTIMGraph g = null;
        try
        {
            g = DenoptimIO.readDENOPTIMGraphsFromFile(
                    new File(TestOperatorParameters.inpFileM), true).get(0);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    
        System.out.println("Initial graphs: ");
        System.out.println(g);
        System.out.println(" ");
        MutationType mt = TestOperatorParameters.mutationType;
        
        DENOPTIMVertex v = null;
        int[] vidv = TestOperatorParameters.mutationTarget;
        String str = "";
        if (vidv.length>1)
        {
            for (int i=(vidv.length-1); i>-1; i--)
            {
                if (i==vidv.length-1)
                {
                    str = "[" + vidv[i] + "]";
                } else {
                    str = "[" + str + " " + vidv[i] + "] ";
                }
            }
            System.out.println("Attempting mutation '" + mt + "' on deep "
                    + "vertex " + str);
            DENOPTIMVertex outerVertex = null;
            DENOPTIMGraph innerGraph = g;
            for (int i=0; i<vidv.length; i++)
            {
                if (outerVertex != null && outerVertex instanceof DENOPTIMTemplate)
                {
                    innerGraph = ((DENOPTIMTemplate) outerVertex).getInnerGraph();
                }
                outerVertex = innerGraph.getVertexWithId(vidv[i]);
                if (outerVertex == null)
                {
                    System.out.println("VertexID '" + vidv[i] +  "' not found "
                            + "in graph "+innerGraph);
                    System.exit(-1);
                }
            }
            v = outerVertex;
        } else {
            int vid = vidv[0];
            System.out.println("Attempting mutation '" + mt + "' on vertex " 
                    + vidv[0]);
            v = g.getVertexWithId(vid);
            if (v == null)
            {
                System.out.println("VertexID '" +vid +  "' not found in graph " 
                        + g);
                System.exit(-1);
            }
        }
         
        int apID = TestOperatorParameters.idNewAP;
        if (mt==MutationType.ADDLINK)
        {
            apID = TestOperatorParameters.idTargetAP;
            if (apID<0)
                throw new DENOPTIMException("ID of target AP is negative.");
        }
        
        // NB: last boolean asks to ignore the growth probability
        DENOPTIMGraphOperations.performMutation(v,mt,true,
                TestOperatorParameters.idNewVrt, apID, new Monitor());

        System.out.println("Result of mutation:");
        System.out.println(g);
        System.out.println(" ");
    
        ThreeDimTreeBuilder t3d = new ThreeDimTreeBuilder();
        IAtomContainer iac = t3d.convertGraphTo3DAtomContainer(g, true);
        DenoptimIO.writeMolecule(TestOperatorParameters.outFileM, iac, false);
    }
    
//------------------------------------------------------------------------------
    
    private static void runXOver() throws DENOPTIMException
    {
        DENOPTIMGraph male = null;
        DENOPTIMGraph female = null;
        try
        {
            male = DenoptimIO.readDENOPTIMGraphsFromFile(
                    new File(TestOperatorParameters.inpFileM), true).get(0);
            female = DenoptimIO.readDENOPTIMGraphsFromFile(
                    new File(TestOperatorParameters.inpFileF), true).get(0);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        
        for (DENOPTIMVertex v : male.getVertexList()) {
            System.out.println("Vertex with id: " + v.getVertexId() + "is " +
                    "class " + v.getClass().getName());
        }
        for (DENOPTIMVertex v : female.getVertexList()) {
            System.out.println("Vertex with id: " + v.getVertexId() + "is " +
                    "class " + v.getClass().getName());
        }

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
        female.renumberGraphVertices();
    
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
    
        ThreeDimTreeBuilder t3d = new ThreeDimTreeBuilder();
        IAtomContainer iacM = t3d.convertGraphTo3DAtomContainer(male,true);
        IAtomContainer iacF = t3d.convertGraphTo3DAtomContainer(female,true);
        DenoptimIO.writeMolecule(TestOperatorParameters.outFileM, iacM, false);
        DenoptimIO.writeMolecule(TestOperatorParameters.outFileF, iacF, false);
    }

//------------------------------------------------------------------------------

}
