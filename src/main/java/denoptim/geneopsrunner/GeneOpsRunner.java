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

package denoptim.geneopsrunner;

import java.io.File;

import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.denoptimga.DENOPTIMGraphOperations;
import denoptim.exception.DENOPTIMException;
import denoptim.graph.DENOPTIMGraph;
import denoptim.graph.DENOPTIMTemplate;
import denoptim.graph.DENOPTIMVertex;
import denoptim.io.DenoptimIO;
import denoptim.logging.Monitor;
import denoptim.task.ProgramTask;
import denoptim.threedim.ThreeDimTreeBuilder;
import denoptim.utils.MutationType;

/**
 * Tool to run genetic operations in a stand-alone fashion, i.e., outside of a
 * genetic algorithm run.
 *
 * @author Marco Foscato
 */

public class GeneOpsRunner extends ProgramTask
{

//------------------------------------------------------------------------------
  
    /**
     * Creates and configures the program task.
     * @param configFile the file containing the configuration parameters.
     * @param workDir the file system location from which to run the program.
     */
    public GeneOpsRunner(File configFile, File workDir)
    {
        super(configFile,workDir);
    }

//------------------------------------------------------------------------------    

    @Override
    public void runProgram() throws Throwable
    {
        GeneOpsRunnerParameters.readParameterFile(
                configFilePathName.getAbsolutePath());
        GeneOpsRunnerParameters.checkParameters();
        GeneOpsRunnerParameters.processParameters();
        
        switch (GeneOpsRunnerParameters.operatorToTest)
        {
            case CROSSOVER:
                runXOver();
                break;
            case XOVER:
                runXOver();
                break;
            case MUTATION:
                runMutation();
                break;
        }
    }
    
//------------------------------------------------------------------------------
    
    private static void runMutation() throws DENOPTIMException
    {
        DENOPTIMGraph graph = null;
        try
        {
            graph = DenoptimIO.readDENOPTIMGraphsFromFile(
                    new File(GeneOpsRunnerParameters.inpFileM)).get(0);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    
        System.out.println("Initial graphs: ");
        System.out.println(graph);
        System.out.println(" ");
        MutationType mt = GeneOpsRunnerParameters.mutationType;
        
        if (mt != null)
        {
            DENOPTIMVertex v = getEmbeddedVertex(GeneOpsRunnerParameters.mutationTarget,
                    graph, "mutation " + mt);
            
            if (v == null)
            {
                return;
            }
             
            int apID = GeneOpsRunnerParameters.idNewAP;
            if (mt==MutationType.ADDLINK)
            {
                apID = GeneOpsRunnerParameters.idTargetAP;
                if (apID<0)
                    throw new DENOPTIMException("ID of target AP is negative. "
                            + "For mutation " + mt + " you should specify also "
                            + "the index of the AP on the mutation target as "
                            + "TESTGENOPS-APIDONTARGETVERTEX.");
            }
            
            // NB: last boolean asks to ignore the growth probability
            DENOPTIMGraphOperations.performMutation(v,mt,true,
                    GeneOpsRunnerParameters.idNewVrt, apID, new Monitor());

        } else {
            System.out.println("Attempting mutation a random mutation on a "
                    + "random vertex");
            DENOPTIMGraphOperations.performMutation(graph, new Monitor());
        }
        System.out.println("Result of mutation:");
        System.out.println(graph);
        System.out.println(" ");
        
        ThreeDimTreeBuilder t3d = new ThreeDimTreeBuilder();
        IAtomContainer iac = t3d.convertGraphTo3DAtomContainer(graph, true);
        DenoptimIO.writeSDFFile(GeneOpsRunnerParameters.outFileM, iac, false);
    }
    
//------------------------------------------------------------------------------
    
    private static void runXOver() throws DENOPTIMException
    {
        DENOPTIMGraph male = null;
        DENOPTIMGraph female = null;
        try
        {
            male = DenoptimIO.readDENOPTIMGraphsFromFile(
                    new File(GeneOpsRunnerParameters.inpFileM)).get(0);
            female = DenoptimIO.readDENOPTIMGraphsFromFile(
                    new File(GeneOpsRunnerParameters.inpFileF)).get(0);
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        System.out.println("Initial graphs: ");
        System.out.println("MALE: "+male);
        System.out.println("FEMALE: "+female);
        System.out.println(" ");
    
        // Remember position of vertex chosen for xover
        DENOPTIMVertex vm = getEmbeddedVertex(GeneOpsRunnerParameters.xoverSrcMale,
                male, "crossover");
        DENOPTIMVertex vf = getEmbeddedVertex(GeneOpsRunnerParameters.xoverSrcFemale,
                female, "crossover");
        
        // Ensure uniqueness on vertexID
        male.renumberGraphVertices();
        female.renumberGraphVertices();
    
        // Get new vid of chosen frags
        int newmvid = vm.getVertexId();
        int newfvid = vf.getVertexId();
    
        // do crossover
        System.out.println(" ");
        System.out.println("Initial graphs now with unique vertexID: ");
        System.out.println("v: "+ newmvid + " of MALE: " + male);
        System.out.println("v:" + newfvid + " of FEMALE: " + female);
        System.out.println(" ");
    

        //TODO
        DenoptimIO.writeGraphToJSON(new File("/tmp/m.json"), male);
        DenoptimIO.writeGraphToJSON(new File("/tmp/f.json"), female);
        
        DENOPTIMGraph[] offspring = DENOPTIMGraphOperations.performCrossover(vm, vf);
        if (offspring[0]==null || offspring[1]==null)
        {
            System.out.println("WARNING: Crossover failed!");
        } else {
            male = offspring[0];
            female = offspring[1];
        }
    
        System.out.println("Result of crossover:");
        System.out.println("MALE: " + male);
        System.out.println("FEMALE: " + female);
        System.out.println(" ");
        
        //TODO
        DenoptimIO.writeGraphToJSON(new File("/tmp/m_xo.json"), male);
        DenoptimIO.writeGraphToJSON(new File("/tmp/f_xo.json"), female);
    
        ThreeDimTreeBuilder t3d = new ThreeDimTreeBuilder();
        IAtomContainer iacM = t3d.convertGraphTo3DAtomContainer(male, true);
        IAtomContainer iacF = t3d.convertGraphTo3DAtomContainer(female, true);
        DenoptimIO.writeSDFFile(GeneOpsRunnerParameters.outFileM, iacM, false);
        DenoptimIO.writeSDFFile(GeneOpsRunnerParameters.outFileF, iacF, false);
    }
    
//------------------------------------------------------------------------------
    
    private static DENOPTIMVertex getEmbeddedVertex(int[] embeddingPath, 
            DENOPTIMGraph graph, String operation)
    {
        String str = "";
        if (embeddingPath != null && embeddingPath.length>1)
        {
            for (int i=(embeddingPath.length-1); i>-1; i--)
            {
                if (i==embeddingPath.length-1)
                {
                    str = "[" + embeddingPath[i] + "]";
                } else {
                    str = "[" + str + " " + embeddingPath[i] + "] ";
                }
            }
            System.out.println("Attempting '" + operation + "' on deep "
                    + "vertex " + str);
            DENOPTIMVertex outerVertex = null;
            DENOPTIMGraph innerGraph = graph;
            for (int i=0; i<embeddingPath.length; i++)
            {
                if (outerVertex != null && outerVertex instanceof DENOPTIMTemplate)
                {
                    innerGraph = ((DENOPTIMTemplate) outerVertex).getInnerGraph();
                }
                outerVertex = innerGraph.getVertexWithId(embeddingPath[i]);
                if (outerVertex == null)
                {
                    System.out.println("VertexID '" + embeddingPath[i] +  
                            "' not found in graph " + innerGraph);
                    return null;
                }
            }
            return outerVertex;
        } else {
            int vid = embeddingPath[0];
            System.out.println("Attempting '" + operation + "' on vertex " 
                    + embeddingPath[0]);
            DENOPTIMVertex v = graph.getVertexWithId(vid);
            if (v == null)
            {
                System.out.println("VertexID '" +vid +  "' not found in graph " 
                        + graph);
                return null;
            }
            return v;
        }
    }

//------------------------------------------------------------------------------

}
