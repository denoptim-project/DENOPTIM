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

package fragspaceexplorer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;

import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fitness.FitnessParameters;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragsCombination;
import denoptim.fragspace.IdFragmentAndAP;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.APClass;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMEdge;
import denoptim.molecule.DENOPTIMFragment.BBType;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.molecule.IGraphBuildingBlock;
import denoptim.molecule.SymmetricSet;
import denoptim.task.ProcessHandler;
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

public class GraphBuildingTask implements Callable
{
    /**
     * Flag about completion
     */
    private boolean completed = false;

    /**
     * Flag about exception
     */
    private boolean hasException = false;

    /**
     * Exception thrown
     */
    private Throwable thrownExc;

    /**
     * The graph that is expanded
     */
    private DENOPTIMGraph molGraph;

    /**
     * The graph ID of the root graph
     */
    private int rootId;

    /**
     * GraphID: may be from the original graph of from its latest generated 
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
     * A user-assigned id for this task.
     */
    private String id = null;

    /**
     * Number of subtasks
     */
    private int nSubTasks = 0;

    /**
     * Executor for external bash script
     */
    private ProcessHandler ph_sc;

    /**
     * Vector of indeces identifying the next combination of fragments.
     * This is used only to store info needed to make checkpoint files.
     */
    private ArrayList<Integer> nextIds;

    /**
     * Verbosity level
     */
    private int verbosity = FSEParameters.getVerbosity();


//------------------------------------------------------------------------------
 
    /**
     * @return <code>true</code> if an exception has been thrown within this 
     * subtask, which also includes the scenario where the executed external
     * script returned a non-zero exit status.
     */
  
    public boolean foundException()
    {
        return hasException;
    }

//------------------------------------------------------------------------------

    /**
     * @return the exception thrown within this task
     */

    public Throwable getException()
    {
        return thrownExc;
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
     * @return the ID of this task
     */
 
    public String getId()
    {
        return id;
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
     * Constructor
     */
 
    public GraphBuildingTask(int m_Id, DENOPTIMGraph m_molGraph, 
                                     FragsCombination m_fragsToAdd, int m_level)
                                                        throws DENOPTIMException
    {
        id = "" + m_Id;
        molGraph = m_molGraph.clone();
        molGraph.setGraphId(GraphUtils.getUniqueGraphIndex());
        rootId = m_molGraph.getGraphId();
        graphId = molGraph.getGraphId();
        fragsToAdd = m_fragsToAdd;
        level = m_level;      
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
                msg = msg + DENOPTIMConstants.EOL + " - RootGraph: " + molGraph;
            }
            if (verbosity > 0)
            {
                msg = msg +  System.getProperty("line.separator");
                DENOPTIMLogger.appLogger.info(msg);
            }

            // Extend graph as requested
            Map<Integer,SymmetricSet> newSymSets = 
                                            new HashMap<Integer,SymmetricSet>();
            for (IdFragmentAndAP srcAp : fragsToAdd.keySet())
            {
                int sVId = srcAp.getVertexId();
                int sFId = srcAp.getVertexMolId();
                BBType sFTyp = srcAp.getVertexMolType();
                int sApId = srcAp.getApId();
                DENOPTIMVertex srcVrtx = molGraph.getVertexWithId(sVId);
                
                APClass sCls = srcVrtx.getAttachmentPoints().get(
                        sApId).getAPClass();
    
                IdFragmentAndAP trgAp = fragsToAdd.get(srcAp);
                int tVId = trgAp.getVertexId();
                int tFId = trgAp.getVertexMolId();
                BBType tFTyp = trgAp.getVertexMolType(); 
                int tApId = trgAp.getApId();
        
                // type "NONE" is used to represent unused AP
                if (tFTyp == BBType.NONE || tFTyp == BBType.UNDEFINED)
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
    
                //TODO-V3: use a type-agnostic w.r.t vertex constructor
                DENOPTIMVertex trgVrtx = DENOPTIMVertex.newVertexFromLibrary(tVId, tFId, tFTyp);
                
                //TODO-V3: check that symmetry is inherited from the original vertex stored in the library of building blocks.
                /*
                ArrayList<SymmetricSet> symAPs =
                        mol.getSymmetricAPsSets();
                trgVrtx.setSymmetricAP(symAPs);
                */

                trgVrtx.setLevel(srcVrtx.getLevel() + 1);
                
                DENOPTIMEdge edge = srcVrtx.connectVertices(
                        trgVrtx, sApId, tApId);
                if (edge == null)
                {
                    msg = "Unable to make new edge.";
                    DENOPTIMLogger.appLogger.severe(msg);
                    throw new DENOPTIMException(msg);
                }
    
                molGraph.addVertex(trgVrtx);
                molGraph.addEdge(edge);
            }

            // Append new symmetric sets
            for (Integer ssId : newSymSets.keySet())
            {
                SymmetricSet ss = newSymSets.get(ssId);
                if (ss.size() > 1)
                {
                    molGraph.addSymmetricSetOfVertices(ss);
                }
            }

            // Evaluate graph
            Object[] res = molGraph.evaluateGraph();
            if (res == null) // null is used to indicate an unacceptable graph
            {
                if (verbosity > 1)
                {
                    msg = "Graph task "+id+" got null from evaluation.";
                    DENOPTIMLogger.appLogger.info(msg);
                }

                nSubTasks = 1;

                // Store graph
                FSEUtils.storeGraphOfLevel(molGraph.clone(),level,rootId,nextIds);
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
                    needsCaps = molGraph.graphNeedsCappingGroups();
                }

                ArrayList<DENOPTIMGraph> altCyclicGraphs = new ArrayList<DENOPTIMGraph>();
                if (!needsCaps)
                {
                    altCyclicGraphs =
                            molGraph.makeAllGraphsWithDifferentRingSets();
                }
                int sz = altCyclicGraphs.size();
                
                if (sz>0 && !needsCaps)
                {
                    nSubTasks = sz;

                    if (verbosity > 0)
                    {
                        msg = "Graph " + molGraph.getGraphId() 
                              + " is replaced by " + sz
                              + " cyclic alternatives.";
                        DENOPTIMLogger.appLogger.info(msg); 
                    }

                    // WARNING! If cyclic versions of molGraph are available,
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
                            GraphConversionTool gct = new GraphConversionTool();
                            IAtomContainer mol = gct.convertGraphToMolecule(g,
                                                                          true);
                            // Level that generated this graph
                            altRes[4] = level;
                            
                            // Parent graph
                            altRes[3] = rootId;

                            DENOPTIMMoleculeUtils.removeRCA(mol,g);
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
                                 DENOPTIMMoleculeUtils.getInchiForMolecule(mol);
                            if (pr.getFirst() == null)
                            {
                                pr.setFirst("UNDEFINED_INCHI");
                            }
                            altRes[0] = pr.getFirst(); 
                      
                            // Store graph
                            FSEUtils.storeGraphOfLevel(g.clone(),level,rootId,nextIds);
                            graphId = gId;
    
                            // Optionally perform external task
                            if (FSEParameters.submitExternalTask())
                            {
                                executeExternalBASHScript(altRes,gId);
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
                    FSEUtils.storeGraphOfLevel(molGraph.clone(),level,rootId,nextIds);
                   
                    // Optionally perform external task 
                    if (FSEParameters.submitExternalTask() && !needsCaps)
                    {
                    	Object[] fseRes = new Object[5];
                    	fseRes[0] = res [0];
                    	fseRes[1] = res [1];
                    	fseRes[2] = res [2];
                    	fseRes[3] = rootId;
                    	fseRes[4] = level;
                        executeExternalBASHScript(fseRes, molGraph.getGraphId());
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
     * Execute the external script.
     * @param res the vector containing the results from the evaluation of the
     * graph representation
     * @param gId the ID of the graph for which the external task is submitted
     */

    private void executeExternalBASHScript(Object[] res, int gId) 
                                                                throws Throwable
    {
        // prepare variables
        String molinchi = res[0].toString().trim();
        String molsmiles = res[1].toString().trim();
        IAtomContainer molInit = (IAtomContainer) res[2];
        String parentGraphId = res[3].toString().trim();
        String level = res[4].toString().trim();
        molInit.setProperty("InChi", molinchi);
        molInit.setProperty("SMILES", molsmiles);
        molInit.setProperty(DENOPTIMConstants.PARENTGRAPHTAG, parentGraphId);
        molInit.setProperty(DENOPTIMConstants.GRAPHLEVELTAG, level);
        

        String molName = DENOPTIMConstants.FITFILENAMEPREFIX 
        		+ GenUtils.getPaddedString(DENOPTIMConstants.MOLDIGITS,
                                           GraphUtils.getUniqueMoleculeIndex());
        String workDir = FSEParameters.getWorkDirectory();
        String fsep = System.getProperty("file.separator");
        String molInitFile = workDir + fsep + molName 
        		+ DENOPTIMConstants.FITFILENAMEEXTIN;
        String molFinalFile = workDir + fsep + molName 
        		+ DENOPTIMConstants.FITFILENAMEEXTOUT;

        molInit.setProperty(CDKConstants.TITLE, molName);

        // write current graph to file as molecular objects
        DenoptimIO.writeMolecule(molInitFile, molInit, false);

        //TODO: deal with internal fitness and other kinds of fitness

        //TODO change to allow other kinds of external tools (probably merge FitnessTask and FTask and put it under denoptim.fitness package

        // build command
        StringBuilder cmdStr = new StringBuilder();
        String shell = System.getenv("SHELL");
        cmdStr.append(shell).append(" ")
              .append(FitnessParameters.getExternalFitnessProvider())
              .append(" ").append(molInitFile)
              .append(" ").append(molFinalFile)
              .append(" ").append(workDir)
              .append(" ").append(id)
              .append(" ").append(FSEParameters.getUIDFileName());

        if (verbosity > 0)
        {
            DENOPTIMLogger.appLogger.log(Level.INFO, "Executing: {0}", cmdStr);
        }

        // run it
        ph_sc = new ProcessHandler(cmdStr.toString(), id);

        try
        {
            ph_sc.runProcess();
            if (ph_sc.getExitCode() != 0)
            {
                String msg = "Failed to execute "
                             + FitnessParameters.getExternalFitnessProviderInterpreter().toString()
                             + " script '"
                             + FitnessParameters.getExternalFitnessProvider()
                             + "' on " + molInitFile;
                DENOPTIMLogger.appLogger.severe(msg);
                DENOPTIMLogger.appLogger.severe(ph_sc.getErrorOutput());
                throw new DENOPTIMException(msg);
            }
            ph_sc = null;
        }
        catch (Exception ex)
        {
            throw new DENOPTIMException(ex);
        }
    }

//------------------------------------------------------------------------------
   
    /**
     * @return <code>true</code> if the task is completed
     */

    public boolean isCompleted()
    {
        return completed;
    }

//------------------------------------------------------------------------------

    /**
     * Stop the task if not already completed
     */

    public void stopTask()
    {
        if (completed)
        {
            return;
        }
        if (ph_sc != null)
        {
            System.err.println("Calling stop on processes from "
                                                   + "GraphBuildingTask " + id);
            ph_sc.stopProcess();
        }
    }

//------------------------------------------------------------------------------

    /**
     * Returns a string identifying this task for its ID and reporting whether
     * an exception has been thrown and if the tasks is completed.
     */
    
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("GraphBuildingTask [id=").append(id).
           append(", hasException=").append(hasException).
           append(", completed=").append(completed).
           append("] ");
        return sb.toString();
    }

//------------------------------------------------------------------------------
    
}
