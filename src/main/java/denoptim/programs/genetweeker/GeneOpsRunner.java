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

package denoptim.programs.genetweeker;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.ga.DENOPTIMGraphOperations;
import denoptim.ga.XoverSite;
import denoptim.graph.DENOPTIMGraph;
import denoptim.graph.DENOPTIMTemplate;
import denoptim.graph.DENOPTIMVertex;
import denoptim.io.DenoptimIO;
import denoptim.logging.Monitor;
import denoptim.molecularmodeling.ThreeDimTreeBuilder;
import denoptim.programs.RunTimeParameters.ParametersType;
import denoptim.programs.denovo.GAParameters;
import denoptim.task.ProgramTask;
import denoptim.utils.CrossoverType;
import denoptim.utils.MutationType;

/**
 * Tool to run genetic operations in a stand-alone fashion, i.e., outside of a
 * genetic algorithm run.
 *
 * @author Marco Foscato
 */

public class GeneOpsRunner extends ProgramTask
{
    
    /**
     * Settings from input parameters
     */
    private GeneOpsRunnerParameters settings;
    
    /**
     * Fragment space in use
     */
    private FragmentSpace fragSpace;

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
        GeneOpsRunnerParameters goParams = new GeneOpsRunnerParameters();
        goParams.readParameterFile(configFilePathName.getAbsolutePath());
        goParams.checkParameters();
        goParams.processParameters();

        this.settings = goParams;       
        FragmentSpaceParameters fsParams = new FragmentSpaceParameters();
        if (settings.containsParameters(ParametersType.FS_PARAMS))
        {
            fsParams = (FragmentSpaceParameters)settings.getParameters(
                    ParametersType.FS_PARAMS);
        }
        this.fragSpace = fsParams.getFragmentSpace();
        
        switch (goParams.operatorToTest)
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
    
    private void runMutation() throws DENOPTIMException
    {
        DENOPTIMGraph graph = null;
        try
        {
            graph = DenoptimIO.readDENOPTIMGraphsFromFile(
                    new File(settings.inpFileM)).get(0);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    
        System.out.println("Initial graphs: ");
        System.out.println(graph);
        System.out.println(" ");
        MutationType mt = settings.mutationType;
        
        //Just in case we used some keyword to set something that affects 
        //mutation operations
        GAParameters gaParams = new GAParameters();
        if (settings.containsParameters(ParametersType.GA_PARAMS))
        {
            gaParams = ((GAParameters) settings.getParameters(
                    ParametersType.GA_PARAMS));
        }
        gaParams.setParameters(((FragmentSpaceParameters) settings.getParameters(
                    ParametersType.FS_PARAMS)));
        
        if (mt != null)
        {
            DENOPTIMVertex v = getEmbeddedVertex(settings.mutationTarget,
                    graph, "mutation " + mt);
            
            if (v == null)
            {
                return;
            }
             
            int apID = settings.idNewAP;
            if (mt==MutationType.ADDLINK)
            {
                apID = settings.idTargetAP;
                if (apID<0)
                    throw new DENOPTIMException("ID of target AP is negative. "
                            + "For mutation " + mt + " you should specify also "
                            + "the index of the AP on the mutation target as "
                            + "TESTGENOPS-APIDONTARGETVERTEX.");
            }
            
            // NB: last boolean asks to ignore the growth probability
            DENOPTIMGraphOperations.performMutation(v,mt,true,
                    settings.idNewVrt, apID, new Monitor(),gaParams);

        } else {
            System.out.println("Attempting mutation a random mutation on a "
                    + "random vertex");
            DENOPTIMGraphOperations.performMutation(graph,new Monitor(),gaParams);
        }
        System.out.println("Result of mutation:");
        System.out.println(graph);
        System.out.println(" ");
        
        ThreeDimTreeBuilder t3d = new ThreeDimTreeBuilder();
        IAtomContainer iac = t3d.convertGraphTo3DAtomContainer(graph, true);
        DenoptimIO.writeSDFFile(settings.outFileM, iac, false);
    }
    
//------------------------------------------------------------------------------
    
    private void runXOver() throws DENOPTIMException
    {
        DENOPTIMGraph male = null;
        DENOPTIMGraph female = null;
        try
        {
            male = DenoptimIO.readDENOPTIMGraphsFromFile(
                    new File(settings.inpFileM)).get(0);
            female = DenoptimIO.readDENOPTIMGraphsFromFile(
                    new File(settings.inpFileF)).get(0);
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        System.out.println("Initial graphs: ");
        System.out.println("MALE: "+male);
        System.out.println("FEMALE: "+female);
        System.out.println(" ");
    
        // Identify the crossover operation to perform
        DENOPTIMVertex vm = getEmbeddedVertex(settings.xoverSrcMale,
                male, "crossover");
        DENOPTIMVertex vf = getEmbeddedVertex(settings.xoverSrcFemale,
                female, "crossover");
        
        CrossoverType xoverType = CrossoverType.BRANCH;
        if (settings.xoverSubGraphEndMale.size()!=0)
            xoverType = CrossoverType.SUBGRAPH;
        
        List<DENOPTIMVertex> subGraphA = new ArrayList<DENOPTIMVertex>();
        subGraphA.add(vm);
        male.getChildTreeLimited(vm, subGraphA, getSubGraphEnds(male,
                settings.xoverSubGraphEndMale, "crossover"));
        List<DENOPTIMVertex> subGraphB = new ArrayList<DENOPTIMVertex>();
        subGraphB.add(vf);
        female.getChildTreeLimited(vf, subGraphB, getSubGraphEnds(female,
                settings.xoverSubGraphEndFemale, "crossover"));

        XoverSite xos = new XoverSite(subGraphA, subGraphB, xoverType);
        
        // Ensure uniqueness on vertexID
        male.renumberGraphVertices();
        female.renumberGraphVertices();
    
        System.out.println(" ");
        System.out.println("Initial graphs now with unique vertexID: ");
        System.out.println("v: "+ vm.getVertexId() + " of MALE: " + male);
        System.out.println("v:" + vf.getVertexId() + " of FEMALE: " + female);
        System.out.println(" ");
        
        DENOPTIMGraphOperations.performCrossover(xos, fragSpace);
    
        System.out.println("Result of crossover:");
        System.out.println("MALE: " + male);
        System.out.println("FEMALE: " + female);
        System.out.println(" ");
    
        ThreeDimTreeBuilder t3d = new ThreeDimTreeBuilder();
        IAtomContainer iacM = t3d.convertGraphTo3DAtomContainer(male, true);
        IAtomContainer iacF = t3d.convertGraphTo3DAtomContainer(female, true);
        DenoptimIO.writeSDFFile(settings.outFileM, iacM, false);
        DenoptimIO.writeSDFFile(settings.outFileF, iacF, false);
    }
    
//------------------------------------------------------------------------------
    
    private Set<DENOPTIMVertex> getSubGraphEnds(DENOPTIMGraph graph, 
            List<int[]> embeddingPaths, String operation)
    {
        Set<DENOPTIMVertex> result = new HashSet<DENOPTIMVertex>();
        for (int[] embeddingPath : embeddingPaths)
        {
            result.add(getEmbeddedVertex(embeddingPath, graph, operation));
        }
        return result;
    }

//------------------------------------------------------------------------------
    
    private DENOPTIMVertex getEmbeddedVertex(int[] embeddingPath, 
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
