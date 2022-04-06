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

package denoptim.combinatorial;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fitness.FitnessParameters;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.fragspace.FragsCombination;
import denoptim.fragspace.IdFragmentAndAP;
import denoptim.graph.APClass;
import denoptim.graph.Candidate;
import denoptim.graph.DENOPTIMGraph;
import denoptim.graph.DENOPTIMVertex;
import denoptim.graph.SymmetricSet;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecularmodeling.ThreeDimTreeBuilder;
import denoptim.programs.RunTimeParameters.ParametersType;
import denoptim.task.FitnessTask;
import denoptim.utils.DENOPTIMMoleculeUtils;
import denoptim.utils.GenUtils;
import denoptim.utils.GraphConversionTool;
import denoptim.utils.GraphUtils;
import denoptim.utils.ObjectPair;


/**
 * Task that builds a graph by appending a given combination of fragments onto
 * a given list of attachment points of a given root graph.
 *
 * @author Marco Foscato
 */

public class GraphBuildingTask extends FitnessTask
{
    /**
     * The graph ID of the root graph
     */
    private int rootId;

    /**
     * GraphID: may be from the original graph or from its latest generated 
     * cyclic alternative.
     */
    private int graphId;

    /**
     * The active level of modification
     */
    private int level;

    /**
     * Pointer defining the active combination for this task
     */
    private FragsCombination fragsToAdd;

    /**
     * Number of subtasks
     */
    private int nSubTasks = 0;

    /**
     * Vector of indexes identifying the next combination of fragments.
     * This is used only to store info needed to make checkpoint files.
     */
    private ArrayList<Integer> nextIds;
    
    /**
     * Tool for generating 3D models assembling 3D building blocks.
     */
    private ThreeDimTreeBuilder tb3d;
    
    /**
     * Collection of settings controlling the execution of the task
     */
    private CEBLParameters ceblSettings = null;

//------------------------------------------------------------------------------
   
    /**
     * Constructor
     */
    public GraphBuildingTask(CEBLParameters settings, DENOPTIMGraph molGraph,
    		FragsCombination fragsToAdd, int level, String workDir, 
    		int verbosity) throws DENOPTIMException
    {
        super((FitnessParameters) settings.getParameters(
                ParametersType.FIT_PARAMS),
                new Candidate(molGraph.clone()));
        this.ceblSettings = settings;
        dGraph.setGraphId(GraphUtils.getUniqueGraphIndex());
        rootId = molGraph.getGraphId();
        graphId = dGraph.getGraphId();
        this.workDir = new File(workDir);
        this.fragsToAdd = fragsToAdd;
        this.level = level;  
        this.verbosity = verbosity;
    }
    
//------------------------------------------------------------------------------

    /**
     * @return the total number of serial subtasks performed by this task.
     */

    public int getNumberOfSubTasks()
    {
        return nSubTasks;
    }

//------------------------------------------------------------------------------

    /**
     * @return the the level from which this graph has been generated
     */

    public int getLevel()
    {
        return level;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the set of indeces that identify the position of the
     * the (next) combination of fragment in the space of combinations.
     */

    public ArrayList<Integer> getNextIds()
    {
        return nextIds;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the graphID of the original graph or of the latest cyclic 
     * alternative.
     */

    public int getGraphId()
    {
        return graphId;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the graphID of the root graph.
     */

    public int getRootId()
    {
        return rootId;
    }

//------------------------------------------------------------------------------

    /**
     * Set the set of indeces that identify the position of the
     * the (next) combination of fragment in the space of combinations.
     */

    public void setNextIds(ArrayList<Integer> nextIds) throws DENOPTIMException
    {
        try
        {
            this.nextIds = (ArrayList<Integer>) nextIds.clone();
        }
        catch (Throwable t)
        {
            throw new DENOPTIMException(t);
        }
    }

//------------------------------------------------------------------------------
   
    /**
     * Calls the task
     */
 
    @Override
    public Object call() throws Exception
    {
        try
        {
            String msg = "Call GraphBuildingTask " + id 
                         + " (Lev:" + level + ", comb:" + nextIds + ")";
            
            if (verbosity > 1)
            {
                msg = msg + DENOPTIMConstants.EOL + " - Fragsments to add: ";
                for (IdFragmentAndAP src : fragsToAdd.keySet())
                {
                    msg = msg + DENOPTIMConstants.EOL 
                          + "   "+src+" - "+fragsToAdd.get(src);
                }
                msg = msg + DENOPTIMConstants.EOL + " - RootGraph: " + dGraph;
            }
            if (verbosity > 0)
            {
                msg = msg + NL;
                DENOPTIMLogger.appLogger.info(msg);
            }
            
            // Initialize the 3d model builder
            tb3d = new ThreeDimTreeBuilder();
            if (!fitnessSettings.make3dTree())
            {
            	tb3d.setAlidnBBsIn3D(false);
            }

            // Extend graph as requested
            Map<Integer,SymmetricSet> newSymSets = 
                                            new HashMap<Integer,SymmetricSet>();
            for (IdFragmentAndAP srcAp : fragsToAdd.keySet())
            {
                int sVId = srcAp.getVertexId();
                int sFId = srcAp.getVertexMolId();
                DENOPTIMVertex.BBType sFTyp = srcAp.getVertexMolType();
                int sApId = srcAp.getApId();
                DENOPTIMVertex srcVrtx = dGraph.getVertexWithId(sVId);
                
                APClass sCls = srcVrtx.getAttachmentPoints().get(
                        sApId).getAPClass();
    
                IdFragmentAndAP trgAp = fragsToAdd.get(srcAp);
                int tVId = trgAp.getVertexId();
                int tFId = trgAp.getVertexMolId();
                DENOPTIMVertex.BBType tFTyp = trgAp.getVertexMolType(); 
                int tApId = trgAp.getApId();
        
                // type "NONE" is used to represent unused AP
                if (tFTyp == DENOPTIMVertex.BBType.NONE 
                        || tFTyp == DENOPTIMVertex.BBType.UNDEFINED)
                {
                    continue;
                }

                // record symmetric relations between vertices
                int tSymSetID = trgAp.getVrtSymSetId();
                if (newSymSets.containsKey(tSymSetID))
                {
                    newSymSets.get(tSymSetID).add(tVId);
                }
                else
                {
                    SymmetricSet ss = new SymmetricSet();
                    ss.add(tVId);
                    newSymSets.put(tSymSetID,ss);
                }
    
                DENOPTIMVertex trgVrtx = DENOPTIMVertex.newVertexFromLibrary(
                        tVId, tFId, tFTyp);
                
                dGraph.appendVertexOnAP(srcVrtx.getAP(sApId), 
                        trgVrtx.getAP(tApId));
            }

            // Append new symmetric sets
            for (Integer ssId : newSymSets.keySet())
            {
                SymmetricSet ss = newSymSets.get(ssId);
                if (ss.size() > 1)
                {
                    dGraph.addSymmetricSetOfVertices(ss);
                }
            }

            // Evaluate graph
            Object[] res = dGraph.evaluateGraph(
                    (FragmentSpaceParameters) ceblSettings.getParameters(
                    ParametersType.FS_PARAMS));
            if (res == null) // null is used to indicate an unacceptable graph
            {
                if (verbosity > 1)
                {
                    msg = "Graph task "+id+" got null from evaluation.";
                    DENOPTIMLogger.appLogger.info(msg);
                }

                nSubTasks = 1;

                // Store graph
                CEBLUtils.storeGraphOfLevel(ceblSettings, dGraph.clone(), level,
                        rootId, nextIds);
            }
            else
            {
                // We don't add capping groups as they are among the candidate
                // fragments to be put in each AP.
                // If a graph still holds unused APs that should be capped,
                // then such graph is an unfinished molecular entity that
                // will be further grown, so no need to perceive rings or
                // submit any external task.
                boolean needsCaps = false;
                if (FragmentSpace.useAPclassBasedApproach())
                {
                    needsCaps = dGraph.graphNeedsCappingGroups();
                }

                ArrayList<DENOPTIMGraph> altCyclicGraphs = new ArrayList<DENOPTIMGraph>();
                if (!needsCaps)
                {
                    altCyclicGraphs = dGraph.makeAllGraphsWithDifferentRingSets(
                            ceblSettings);
                }
                int sz = altCyclicGraphs.size();
                
                if (sz>0 && !needsCaps)
                {
                    nSubTasks = sz;

                    if (verbosity > 0)
                    {
                        msg = "Graph " + dGraph.getGraphId() 
                              + " is replaced by " + sz
                              + " cyclic alternatives.";
                        DENOPTIMLogger.appLogger.info(msg); 
                    }

                    // WARNING! If cyclic versions of dGraph are available,
                    // we IGNORE the acyclic original. This is because,
                    // if at all possible, the acyclic graph is built anyway
                    // using capping groups instead of ring closing attractors.
    
                    // prepare log message
                    String lst = "[";
                    for (int ig = 0; ig<sz-1; ig++)
                    {
                        lst = lst + altCyclicGraphs.get(ig).getGraphId()+", ";
                    }
                    lst = lst + altCyclicGraphs.get(sz-1).getGraphId() + "]";
    
                    // process all alternative graphs
                    for (int ig = 0; ig<altCyclicGraphs.size(); ig++)
                    {
                        DENOPTIMGraph g = altCyclicGraphs.get(ig);
                        int gId = g.getGraphId();
                        
                        if (verbosity > 0)
                        {
                            msg = "Graph " + gId
                                  + " is cyclic alternative "
                                  + (ig+1) + "/" + altCyclicGraphs.size()
                                  + " " + lst;
                            DENOPTIMLogger.appLogger.info(msg);
                        }

                        // Prepare vector of results
                        // NB: in FSE we add also the ID of the root graph in 
                        // this array
                        Object[] altRes = new Object[5];

                        try 
                        {
                            // Prepare molecular representation
                        	DENOPTIMGraph gWithNoRCVs = g.clone();
                        	//NB: this replaces unused RCVs with capping groups
                        	GraphConversionTool.replaceUnusedRCVsWithCapps(gWithNoRCVs);
                        	IAtomContainer mol = 
                        	        tb3d.convertGraphTo3DAtomContainer(
	                                    gWithNoRCVs,true);
                            
                            // Level that generated this graph
                            altRes[4] = level;
                            
                            // Parent graph
                            altRes[3] = rootId;

                            altRes[2] = mol;
        
                            // Prepare SMILES
                            String smiles = 
                                DENOPTIMMoleculeUtils.getSMILESForMolecule(mol);
                            if (smiles == null)
                            {
                                smiles = "FAIL: NO SMILES GENERATED";
                            }
                            altRes[1] = smiles;
        
                            // Prepare INCHI                    
                            ObjectPair pr = 
                                 DENOPTIMMoleculeUtils.getInChIForMolecule(mol);
                            if (pr.getFirst() == null)
                            {
                                pr.setFirst("UNDEFINED_INCHI");
                            }
                            altRes[0] = pr.getFirst();
                            
                            // Store graph
                            CEBLUtils.storeGraphOfLevel(ceblSettings, g.clone(), 
                                    level, rootId, nextIds);
                            graphId = gId;
    
                            // Optionally perform external task
                            if (ceblSettings.submitFitnessTask())
                            {
                                sendToFitnessProvider(altRes);
                            }
                        }
                        catch (Throwable t)
                        {
                            msg = "Exception while working on cyclic graph "+g;
                            throw new Throwable(msg,t);
                        }
                    }
                }
                else
                {
                    nSubTasks = 1;

                    // Store graph
                    DENOPTIMGraph gClone = dGraph.clone();
                    CEBLUtils.storeGraphOfLevel(ceblSettings, gClone, level, rootId, 
                            nextIds);
                    
                    // Optionally improve the molecular representation, which
                    // is otherwise only given by the collection of building
                    // blocks (not aligned, nor roto-translated)
                	if (fitnessSettings.make3dTree())
                	{
                	    //NB: this replaces unused RCVs with capping groups
                        GraphConversionTool.replaceUnusedRCVsWithCapps(gClone);
                        IAtomContainer mol = 
                                tb3d.convertGraphTo3DAtomContainer(
                                        gClone,true);
                        res[2] = mol;
                	}
                   
                    // Optionally perform external task 
                    if (ceblSettings.submitFitnessTask() && !needsCaps)
                    {
                    	Object[] fseRes = new Object[5];
                    	fseRes[0] = res[0];
                    	fseRes[1] = res[1];
                    	fseRes[2] = res[2];
                    	fseRes[3] = rootId;
                    	fseRes[4] = level;
                    	sendToFitnessProvider(fseRes);
                    }
                }
            }
        }
        catch (Throwable t)
        {
            // This is a trick to transmit the exception to the parent thread
            // and store it as a property of the present task
            hasException = true;
            thrownExc = t;
            throw new Exception(t);
        }

        completed = true;
        return "PASS";
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @param res the vector containing the results from the evaluation of the
     * graph representation
     */

    private void sendToFitnessProvider(Object[] res) throws Throwable
    {
        String molinchi = res[0].toString().trim();
        String molsmiles = res[1].toString().trim();
        fitProvMol = (IAtomContainer) res[2];
        String parentGraphId = res[3].toString().trim();
        String level = res[4].toString().trim();
        fitProvMol.setProperty(DENOPTIMConstants.INCHIKEYTAG, molinchi);
        fitProvMol.setProperty(DENOPTIMConstants.UNIQUEIDTAG, molinchi);
        fitProvMol.setProperty(DENOPTIMConstants.SMILESTAG, molsmiles);
        fitProvMol.setProperty(DENOPTIMConstants.PARENTGRAPHTAG, parentGraphId);
        fitProvMol.setProperty(DENOPTIMConstants.GRAPHLEVELTAG, level);
        String molName = DENOPTIMConstants.FITFILENAMEPREFIX 
        		+ GenUtils.getPaddedString(DENOPTIMConstants.MOLDIGITS,
                                           GraphUtils.getUniqueMoleculeIndex());
        fitProvMol.setProperty(CDKConstants.TITLE, molName);
        fitProvInputFile = workDir + SEP + molName 
        		+ DENOPTIMConstants.FITFILENAMEEXTIN;
        fitProvOutFile = workDir + SEP + molName 
        		+ DENOPTIMConstants.FITFILENAMEEXTOUT;
        fitProvPNGFile = workDir + SEP + molName 
                + DENOPTIMConstants.CANDIDATE2DEXTENSION;
        fitProvUIDFile = ceblSettings.getUIDFileName();
        
        runFitnessProvider();
    }

//------------------------------------------------------------------------------
    
}