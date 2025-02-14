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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.ga.EAUtils;
import denoptim.ga.GraphOperations;
import denoptim.ga.XoverSite;
import denoptim.graph.DGraph;
import denoptim.graph.Template;
import denoptim.graph.Vertex;
import denoptim.graph.rings.RingClosureParameters;
import denoptim.io.DenoptimIO;
import denoptim.logging.CounterID;
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
    
    /**
     * Parameters for  genetic algorithm.
     */
    private GAParameters gaParams;
    
    /**
     * Program-specific logger
     */
    private Logger logger = null;

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
        goParams.startProgramSpecificLogger(loggerIdentifier, false); //to STDOUT
        goParams.printParameters();
        
        this.logger = goParams.getLogger();
        this.settings = goParams;       
        FragmentSpaceParameters fsParams = new FragmentSpaceParameters();
        if (settings.containsParameters(ParametersType.FS_PARAMS))
        {
            fsParams = (FragmentSpaceParameters)settings.getParameters(
                    ParametersType.FS_PARAMS);
        }
        this.fragSpace = fsParams.getFragmentSpace();
        if (settings.containsParameters(ParametersType.GA_PARAMS))
        {
            this.gaParams = (GAParameters) settings.getParameters(
                    ParametersType.GA_PARAMS);
        } else {
            this.gaParams = new GAParameters();
        }
        
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
        DGraph graph = null;
        try
        {
            graph = DenoptimIO.readDENOPTIMGraphsFromFile(
                    new File(settings.inpFileM)).get(0);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    
        logger.log(Level.INFO, "Initial graphs: " + NL + graph.toString() + NL);
        MutationType mt = settings.mutationType;
        
        //Just in case we used some keyword to set something that affects 
        //mutation operations
        GAParameters gaParams = new GAParameters();
        if (settings.containsParameters(ParametersType.GA_PARAMS))
        {
            gaParams = ((GAParameters) settings.getParameters(
                    ParametersType.GA_PARAMS));
        }
        if (settings.containsParameters(ParametersType.FS_PARAMS))
        {
            gaParams.setParameters(((FragmentSpaceParameters) 
                    settings.getParameters(ParametersType.FS_PARAMS)));
        }
        if (settings.containsParameters(ParametersType.RC_PARAMS))
        {
            gaParams.setParameters(((RingClosureParameters) 
                    settings.getParameters(ParametersType.RC_PARAMS)));
        }
        
        boolean done = false;
        if (mt != null)
        {
            Vertex v = getEmbeddedVertex(settings.mutationTarget,
                    graph, "mutation " + mt);
            
            if (v == null)
            {
                List<Vertex> mutable = graph.getSelectedMutableSites(mt);
                if (mutable.size()==0)
                {
                    logger.log(Level.INFO, "Graph has no mutable site of type '"
                            + mt + "'. Mutation aborted.");
                    return;
                }
                v = gaParams.getRandomizer().randomlyChooseOne(mutable);
                logger.log(Level.INFO, "Randomly choosed vertex as target "
                        + "for mutation '" + mt + "': " + v);
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
            
            v.setProperty(DENOPTIMConstants.STOREDVID, v.getVertexId());
            
            // NB: last boolean asks to ignore the growth probability
            done = GraphOperations.performMutation(v, mt, true,
                    settings.idNewVrt, apID, new Monitor(), gaParams);
        } else {
            logger.log(Level.INFO, "Attempting mutation a random mutation on a "
                    + "random vertex");
            done = GraphOperations.performMutation(graph, new Monitor(), gaParams);
        }
        
        if (done)
        {
            logger.log(Level.INFO, "Result of mutation:" + NL + graph.toString()+NL);
        } 
        ThreeDimTreeBuilder t3d = new ThreeDimTreeBuilder(
                settings.getLogger(),
                settings.getRandomizer());
        IAtomContainer iac = t3d.convertGraphTo3DAtomContainer(graph, true);
        DenoptimIO.writeSDFFile(settings.outFileM, iac, false);
    }
    
//------------------------------------------------------------------------------
    
    private void runXOver() throws DENOPTIMException
    {
        DGraph male = null;
        DGraph female = null;
        try
        {
            male = DenoptimIO.readDENOPTIMGraphsFromFile(
                    new File(settings.inpFileM)).get(0);
            female = DenoptimIO.readDENOPTIMGraphsFromFile(
                    new File(settings.inpFileF)).get(0);
        } catch (Exception e)
        {
            e.printStackTrace();
            return;
        }

        logger.log(Level.INFO, "Initial graphs: "+NL
                +"MALE: "+male+NL
                +"FEMALE: "+female);
    
        // Identify the crossover operation to perform
        XoverSite xos = null;
        if (settings.xoverSrcMale!=null && settings.xoverSrcFemale!=null)
        {
            Vertex vm = getEmbeddedVertex(settings.xoverSrcMale,
                    male, "crossover");
            Vertex vf = getEmbeddedVertex(settings.xoverSrcFemale,
                    female, "crossover");
            
            CrossoverType xoverType = CrossoverType.BRANCH;
            if (settings.xoverSubGraphEndMale.size()!=0)
                xoverType = CrossoverType.SUBGRAPH;
            
            List<Vertex> subGraphA = new ArrayList<Vertex>();
            subGraphA.add(vm);
            male.getChildTreeLimited(vm, subGraphA, getSubGraphEnds(male,
                    settings.xoverSubGraphEndMale, "crossover"));
            List<Vertex> subGraphB = new ArrayList<Vertex>();
            subGraphB.add(vf);
            female.getChildTreeLimited(vf, subGraphB, getSubGraphEnds(female,
                    settings.xoverSubGraphEndFemale, "crossover"));
    
            xos = new XoverSite(subGraphA, subGraphB, xoverType);
            
            // Ensure uniqueness on vertexID
            male.renumberGraphVertices();
            female.renumberGraphVertices();
        
            logger.log(Level.INFO,NL+"Initial graphs now with unique vertexID: "
                    +NL+ "v: "+ vm.getVertexId() + " of MALE: " + male
                    +NL+ "v:" + vf.getVertexId() + " of FEMALE: " + female);
        } else {
            logger.log(Level.INFO, "Attempting crossover on a site detected "
                    + "on-the-fly");
            List<XoverSite> sites = GraphOperations.locateCompatibleXOverPoints(
                    male, female, fragSpace, gaParams.maxXOverableSubGraphSize);
            if (sites.isEmpty())
            {
                logger.log(Level.WARNING, "No crossover site detected.");
                return;
            } else {
                logger.log(Level.INFO, "Randombly choosing among "
                        + sites.size() + " xover sites.");
            }
            xos = settings.getRandomizer().randomlyChooseOne(sites);
        }
        
        GraphOperations.performCrossover(xos, fragSpace);
    
        logger.log(Level.INFO, NL + "Result of crossover:"
                + NL + "MALE: " + male
                + NL + "FEMALE: " + female);
    
        ThreeDimTreeBuilder t3d = new ThreeDimTreeBuilder(
                settings.getLogger(),
                settings.getRandomizer());
        IAtomContainer iacM = t3d.convertGraphTo3DAtomContainer(male, true);
        IAtomContainer iacF = t3d.convertGraphTo3DAtomContainer(female, true);
        DenoptimIO.writeSDFFile(settings.outFileM, iacM, false);
        DenoptimIO.writeSDFFile(settings.outFileF, iacF, false);
    }
    
//------------------------------------------------------------------------------
    
    private Set<Vertex> getSubGraphEnds(DGraph graph, 
            List<int[]> embeddingPaths, String operation)
    {
        Set<Vertex> result = new HashSet<Vertex>();
        for (int[] embeddingPath : embeddingPaths)
        {
            result.add(getEmbeddedVertex(embeddingPath, graph, operation));
        }
        return result;
    }

//------------------------------------------------------------------------------
    
    private Vertex getEmbeddedVertex(int[] embeddingPath, 
            DGraph graph, String operation)
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
            logger.log(Level.INFO, "Attempting '" + operation + "' on deep "
                    + "vertex " + str);
            Vertex outerVertex = null;
            DGraph innerGraph = graph;
            for (int i=0; i<embeddingPath.length; i++)
            {
                if (outerVertex != null && outerVertex instanceof Template)
                {
                    innerGraph = ((Template) outerVertex).getInnerGraph();
                }
                outerVertex = innerGraph.getVertexWithId(embeddingPath[i]);
                if (outerVertex == null)
                {
                    logger.log(Level.INFO, "VertexID '" + embeddingPath[i] +  
                            "' not found in graph " + innerGraph);
                    return null;
                }
            }
            return outerVertex;
        } else {
            if (embeddingPath==null)
            {
                logger.log(Level.INFO, "No mutation site specified.");
                return null;
            }
            int vid = embeddingPath[0];
            logger.log(Level.INFO, "Attempting '" + operation + "' on vertex " 
                    + embeddingPath[0]);
            Vertex v = graph.getVertexWithId(vid);
            if (v == null)
            {
                logger.log(Level.INFO, "VertexID '" + vid +  "' not found in "
                        + "graph " + graph);
                return null;
            }
            return v;
        }
    }

//------------------------------------------------------------------------------

}
