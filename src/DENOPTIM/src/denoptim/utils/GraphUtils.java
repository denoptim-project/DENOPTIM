/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no> and
 *   Marco Foscato <marco.foscato@uib.no>
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

package denoptim.utils;

import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import org.openscience.cdk.graph.ConnectivityChecker;
import org.apache.commons.math3.random.MersenneTwister;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtom;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMEdge;
import denoptim.molecule.DENOPTIMFragment;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMRing;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.molecule.IGraphBuildingBlock;
import denoptim.molecule.SymmetricSet;
import denoptim.rings.ClosableChain;
import denoptim.rings.CyclicGraphHandler;
import denoptim.rings.RingClosureParameters;

import java.util.logging.Level;


/**
 * Utilities for graphs.
 *
 * @author Vishwesh Venkatraman 
 * @author Marco Foscato
 */
public class GraphUtils
{
    private static AtomicInteger vertexCounter = new AtomicInteger(1);
    private static AtomicInteger graphCounter = new AtomicInteger(1);
    private static AtomicInteger molCounter = new AtomicInteger(1);

    private static boolean debug = false;

//------------------------------------------------------------------------------

    /**
     * Reset the unique vertex counter to the given value. In order to keep the
     * uniqueness on the index, this method accepts only reset values
     * that are higher that the current one. Attempts to reset to lower values 
     * return an exception.
     * @param val the new value for the counter. This value will be given to
     * next call of the getUniqueVertexIndex method.
     * @throws DENOPTIMException if the reset value is lower than the current
     * value of the index.
     */

    public static void resetUniqueVertexCounter(int val) 
                                                        throws DENOPTIMException
    {
        if (vertexCounter.get() >= val)
        {
            String msg = "Attempt to reser the unique vertex ID using "
                         + val + " while the current value is "
                         + vertexCounter.get();
            throw new DENOPTIMException(msg);
        }
        vertexCounter = new AtomicInteger(val);
    }

//-----------------------------------------------------------------------------

    /**
     * Unique counter for the number of graph vertices generated.
     * @return the new vertex id (number)
     */

    public static synchronized int getUniqueVertexIndex()
    {
        return vertexCounter.getAndIncrement();
    }

//------------------------------------------------------------------------------

    /**
     * Connects 2 vertices by an edge based on reaction type
     * @param vtxA source vertex
     * @param vtxB target vertex
     * @param iA index of Attachment point in first vertex
     * @param iB index of Attachment point in second vertex
     * @param srcRcn the reaction scheme at the source
     * @param trgRcn the reaction scheme at the target
     * @return DENOPTIMEdge 
     */

    public static DENOPTIMEdge connectVertices(DENOPTIMVertex vtxA,
                                DENOPTIMVertex vtxB, int iA, int iB, 
                                String srcRcn, String trgRcn)
    {
        //System.err.println("Connecting vertices RCN");
        DENOPTIMAttachmentPoint dap_A = vtxA.getAttachmentPoints().get(iA);
        DENOPTIMAttachmentPoint dap_B = vtxB.getAttachmentPoints().get(iB);
        
        if (dap_A.isAvailable() && dap_B.isAvailable())
        {
            //System.err.println("Available APs");
            String rname = trgRcn.substring(0, trgRcn.indexOf(':'));

            // look up the reaction bond order table
            int bndOrder = FragmentSpace.getBondOrderMap().get(rname);
            //System.err.println("Bond: " + bndOrder + " " + srcRcn + " " + trgRcn);

            // create a new edge
            DENOPTIMEdge edge = new DENOPTIMEdge(vtxA.getVertexId(), vtxB.getVertexId(),
                                                     iA, iB, bndOrder);
            edge.setSourceReaction(srcRcn);
            edge.setTargetReaction(trgRcn);

            // update the attachment point info
            dap_A.updateFreeConnections(-bndOrder); // decrement the connections
            dap_B.updateFreeConnections(-bndOrder); // decrement the connections

            return edge;
        } else {
            System.err.println("ERROR! Attempt to make edge using unavailable AP!"
                    + System.getProperty("line.separator")
                    + "Vertex: "+vtxA.getVertexId()
                    +" AP-A(available:"+dap_A.isAvailable()+"): "+dap_A
                    + System.getProperty("line.separator")
                    + "Vertex: "+vtxB.getVertexId()
                    +" AP-B(available:"+dap_B.isAvailable()+"): "+dap_B);
        }
        return null;
    }

//------------------------------------------------------------------------------

    /**
     * Get the id of the vertex attached at the specified attachment point
     * index of the given vertex
     * @param molGraph
     * @param curVertex
     * @param dapIdx
     * @return id of the vertex
     */

    public static int getVertexForAP(DENOPTIMGraph molGraph,
                                        DENOPTIMVertex curVertex, int dapIdx)
    {
        // loop thru the edges with the children and identify the vertex
        // associated
        int vid = -1;
        ArrayList<DENOPTIMEdge> edges = molGraph.getEdgeList();
        for (int i=0; i<edges.size(); i++)
        {
            DENOPTIMEdge edge = edges.get(i);
            if (edge.getSourceVertex() == curVertex.getVertexId())
            {
                if (edge.getSourceDAP() == dapIdx)
                {
                    vid = edge.getTargetVertex();
                    break;
                }
            }
        }

        return vid;
    }

//------------------------------------------------------------------------------
    
    /**
     * @Deprecated
     */
    public static void updateVertexCounter(int num)
    {
        vertexCounter.set(num);
    }
    
//------------------------------------------------------------------------------        

    /**
     * Update the level at which the new vertices have been added. This 
     * is generally applicable for a crossover or for a substitution operation
     * @param lstVert
     * @param lvl 
     */

    public static void updateLevels(ArrayList<DENOPTIMVertex> lstVert, int lvl)
    {
        int levRoot = lstVert.get(0).getLevel();
        int correction = lvl - levRoot;
        for (int i=0; i<lstVert.size(); i++)
        {
            lstVert.get(i).setLevel(lstVert.get(i).getLevel() + correction);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Reset the unique graph counter to the given value. In order to keep the
     * uniqueness on the index, this method accepts only reset values
     * that are higher that the current one. Attempts to reset to lower values
     * return an exception.
     * @param val the new value for the counter. This value will be given to
     * next call of the getUniqueGraphIndex method.
     * @throws DENOPTIMException if the reset value is lower than the current
     * value of the index.
     */

    public static void resetUniqueGraphCounter(int val) throws DENOPTIMException
    {
        if (graphCounter.get() >= val)
        {
            String msg = "Attempt to reser the unique graph ID using "
                         + val + " while the current value is "
                         + graphCounter.get();
            throw new DENOPTIMException(msg);
        }
        graphCounter = new AtomicInteger(val);
    }

//------------------------------------------------------------------------------
    
    /**
     * Unique counter for the number of graphs generated.
     * @return a new Graph id (number)
     */

    public static synchronized int getUniqueGraphIndex()
    {
        return graphCounter.getAndIncrement();
    }

//------------------------------------------------------------------------------

    /**
     * Reset the unique mol counter to the given value. In order to keep the
     * uniqueness on the index, this method accepts only reset values
     * that are higher that the current one. Attempts to reset to lower values
     * return an exception.
     * @param val the new value for the counter. This value will be given to
     * next call of the getUniqueMoleculeIndex method.
     * @throws DENOPTIMException if the reset value is lower than the current
     * value of the index.
     */

    public static void resetUniqueMoleculeCounter(int val) 
                                                        throws DENOPTIMException
    {
        if (molCounter.get() >= val)
        {
            String msg = "Attempt to reser the unique mol ID using "
                         + val + " while the current value is "
                         + molCounter.get();
            throw new DENOPTIMException(msg);
        }
        molCounter = new AtomicInteger(val);
    }

//------------------------------------------------------------------------------
    
    /**
     * Unique counter for the number of molecules generated.
     * @return the new molecule id (number)
     */

    public static synchronized int getUniqueMoleculeIndex()
    {
        return molCounter.getAndIncrement();
    }

//------------------------------------------------------------------------------

    /**
     * Finalizes a candidate chemical entity by evaluating its
     * <code>DENOPTIMGraph</code> and assigning preliminary molecular, 
     * SMILES and INCHI representations. 
     * The chemical entity is evaluated also against the
     * criteria defined by <code>FragmentSpaceParameters</code> and 
     * <code>RingClosureParameters</code>.
     * <b>WARNING</b> Although Cartesian coordinates are assigned to each atom
     * and pseudo-atom in the molecular representation ,
     * such coordinates do <b>NOT</b> represent a valid 3D model.
     * As a consequence stereochemical descriptors in the INCHI representation
     * are not consistent with the actual arrangement of fragments.
     * @param molGraph the molecular graph representation
     * @return an object array containing the inchi code, the SMILES string
     *         and the 2D representation of the molecule.
     *         <code>null</code> is returned if any check or conversion fails.
     * @throws DENOPTIMException
     */

    public static Object[] evaluateGraph(DENOPTIMGraph molGraph)
                                                      throws DENOPTIMException
    {
        // check for null graph
        if (molGraph == null)
        {
            String msg = "Evaluation of graph: input graph is null!";
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            return null;
        }

        // calculate the molecule representation        
        IAtomContainer mol = GraphConversionTool.convertGraphToMolecule(molGraph, true);
        if (mol == null)
        {
            String msg ="Evaluation of graph: graph-to-mol returned null!"
                                                          + molGraph.toString();
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            return null;
        }

        // check if the molecule is connected
        boolean isConnected = ConnectivityChecker.isConnected(mol);
        if (!isConnected)
        {
            String msg = "Evaluation of graph: Not all connected"
                                                          + molGraph.toString();
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            return null;
        }

        // SMILES
        String smiles = null;
        smiles = DENOPTIMMoleculeUtils.getSMILESForMolecule(mol);
        if (smiles == null)
        {
            String msg = "Evaluation of graph: SMILES is null! "
                                                          + molGraph.toString();
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            smiles = "FAIL: NO SMILES GENERATED";
        }
        // if by chance the smiles indicates a disconnected molecule
        if (smiles.contains("."))
        {
            String msg = "Evaluation of graph: SMILES contains \".\"" + smiles;
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            return null;
        }

        // criteria from definition of Fragment space
        // 1A) number of heavy atoms
        if (FragmentSpaceParameters.getMaxHeavyAtom() > 0)
        {
            if (DENOPTIMMoleculeUtils.getHeavyAtomCount(mol) >
                                      FragmentSpaceParameters.getMaxHeavyAtom())
            {
                String msg = "Evaluation of graph: Max atoms constraint "
                                                       + " violated: " + smiles;
                DENOPTIMLogger.appLogger.log(Level.INFO, msg);
                return null;
            }
        }

        // 1B) molecular weight
        double mw = DENOPTIMMoleculeUtils.getMolecularWeight(mol);
        if (FragmentSpaceParameters.getMaxMW() > 0)
        {
            if (mw > FragmentSpaceParameters.getMaxMW())
            {
                String msg = "Evaluation of graph: Molecular weight "
                            + "constraint violated: " + smiles + " | MW: " + mw;
                DENOPTIMLogger.appLogger.log(Level.INFO, msg);
                return null;
            }
        }
        mol.setProperty("MOL_WT", mw);

        // 1C) number of rotatable bonds
        int nrot = DENOPTIMMoleculeUtils.getNumberOfRotatableBonds(mol);
        if (FragmentSpaceParameters.getMaxRotatableBond() > 0)
        {
            if (nrot > FragmentSpaceParameters.getMaxRotatableBond())
            {
                String msg = "Evaluation of graph: Max rotatable bonds "
                                              + "constraint violated: "+ smiles;
                DENOPTIMLogger.appLogger.log(Level.INFO, msg);
                return null;
            }
        }
        mol.setProperty("ROT_BND", nrot);
        
        // 1D) unacceptable free APs
        if (FragmentSpace.useAPclassBasedApproach())
        {
            if (foundForbiddenEnd(molGraph))
            {
                String msg = "Evaluation of graph: forbidden end in graph!";
                DENOPTIMLogger.appLogger.log(Level.INFO, msg);
                return null;
            }
        }

        // criteria from settings of ring closures
        if (RingClosureParameters.allowRingClosures())
        {
            // Count rings and RCAs
            int nPossRings = 0;
            Set<String> doneType = new HashSet<>();
            Map<String,String> rcaTypes = DENOPTIMConstants.RCATYPEMAP;
            for (String rcaTyp : rcaTypes.keySet())
            {
                if (doneType.contains(rcaTyp))
                {
                    continue;
                }

                int nThisType = 0;
                int nCompType = 0;
                for (IAtom atm : mol.atoms())
                {
                    if (atm.getSymbol().equals(rcaTyp))
                    {
                        nThisType++;
                    }
                    else if (atm.getSymbol().equals(rcaTypes.get(rcaTyp)))
                    {
                        nCompType++;
                    }
                }

                // check number of rca per type
                if (nThisType > RingClosureParameters.getMaxRcaPerType() ||
                         nCompType > RingClosureParameters.getMaxRcaPerType())
                {
                    String msg = "Evaluation of graph: too many RCAs! "
                                  + rcaTyp + ":" + nThisType + " "
                                  + rcaTypes.get(rcaTyp) + ":" + nCompType;
                    DENOPTIMLogger.appLogger.log(Level.INFO, msg);
                    return null;
                }
                if (nThisType < RingClosureParameters.getMinRcaPerType() ||
                         nCompType < RingClosureParameters.getMinRcaPerType())
                {
                    String msg = "Evaluation of graph: too few RCAs! "
                                  + rcaTyp + ":" + nThisType + " "
                                  + rcaTypes.get(rcaTyp) + ":" + nCompType;
                    DENOPTIMLogger.appLogger.log(Level.INFO, msg);
                    return null;
                }

                nPossRings = nPossRings + Math.min(nThisType, nCompType);
                doneType.add(rcaTyp);
                doneType.add(rcaTypes.get(rcaTyp));
            }
            if (nPossRings < RingClosureParameters.getMinRingClosures())
            {
                String msg = "Evaluation of graph: too few ring candidates";
                DENOPTIMLogger.appLogger.log(Level.INFO, msg);
                return null;
            }
        }

        // get the smiles/Inchi representation
        ObjectPair pr = DENOPTIMMoleculeUtils.getInchiForMolecule(mol);
        if (pr.getFirst() == null)
        {
            String msg = "Evaluation of graph: INCHI is null!";
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            pr.setFirst("UNDEFINED_INCHI");
        }

        Object[] res = new Object[3];
        res[0] = pr.getFirst(); // inchi
        res[1] = smiles; // smiles
        res[2] = mol;

        return res;
    }

//------------------------------------------------------------------------------

    /**
     * Evaluates the possibility of closing rings in a given graph and if
     * any ring can be closed, then it randomly chooses one of the combinations
     * of ring closures
     * that involves the highest number of new rings.
     * @param res an object array containing the inchi code, the smiles string
     * and the 2D representation of the molecule. This object can be
     * <code>null</code> if inchi/smiles/2D conversion fails.
     * @param molGraph the <code>DENOPTIMGraph</code> on which rings are to
     * be identified
     * @return <code>true</code> unless no ring can be set up even if required
     * @throws denoptim.exception.DENOPTIMException
     */

    public static boolean setupRings(Object[] res, DENOPTIMGraph molGraph)
                                                        throws DENOPTIMException
    {
        boolean rcnEnabled = FragmentSpace.useAPclassBasedApproach();
        if (!rcnEnabled)
            return true;

        boolean evaluateRings = RingClosureParameters.allowRingClosures();
        if (!evaluateRings)
            return true;

        // get a atoms/bonds molecular representation (no 3D needed)
        IAtomContainer mol = GraphConversionTool.convertGraphToMolecule(molGraph,false);

        // Set rotatability property as property of IBond
        ArrayList<ObjectPair> rotBonds =
                                  RotationalSpaceUtils.defineRotatableBonds(mol,
                                   FragmentSpaceParameters.getRotSpaceDefFile(),
                                                                    true, true);

        // perceive possibility to close rings...
        CyclicGraphHandler cgh = new CyclicGraphHandler(
                                             FragmentSpace.getScaffoldLibrary(),
                                             FragmentSpace.getFragmentLibrary(),
                                              FragmentSpace.getCappingLibrary(),
                                      FragmentSpace.getRCCompatibilityMatrix());
        // ...and choose one
        Set<DENOPTIMRing> combsOfRings = cgh.getRandomCombinationOfRings(mol,
                                                                      molGraph);

        // Assign the rings to the graph
        if (combsOfRings.size() > 0)
        {
            for (DENOPTIMRing ring : combsOfRings)
            {
                molGraph.addRing(ring);
            }
        }

        // Update the IAtomContainer representation
        DENOPTIMMoleculeUtils.removeRCA(mol,molGraph);
        res[2] = mol;

        // Update the SMILES representation
        String molsmiles = DENOPTIMMoleculeUtils.getSMILESForMolecule(mol);
        if (molsmiles == null)
        {
            String msg = "Evaluation of graph: SMILES is null! "
                                                          + molGraph.toString();
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            molsmiles = "FAIL: NO SMILES GENERATED";
        }
        res[1] = molsmiles;

        // Update the INCHI key representation
        ObjectPair pr = DENOPTIMMoleculeUtils.getInchiForMolecule(mol);
        if (pr.getFirst() == null)
        {
            String msg = "Evaluation of graph: INCHI is null!";
            DENOPTIMLogger.appLogger.log(Level.INFO, msg);
            pr.setFirst("UNDEFINED");
        }
        res[0] = pr.getFirst();

        return true;
    }

//------------------------------------------------------------------------------

    /**
     * Evaluates the possibility of closing rings in a given graph and 
     * generates all alternative graphs resulting by different combinations of
     * rings
     * @param molGraph the <code>DENOPTIMGraph</code> on which rings are to
     * be identified
     * @return <code>true</code> unless no ring can be set up even if required
     * @throws denoptim.exception.DENOPTIMException
     */
    public static ArrayList<DENOPTIMGraph> makeAllGraphsWithDifferentRingSets(
                                                         DENOPTIMGraph molGraph)
                                                        throws DENOPTIMException
    {
        ArrayList<DENOPTIMGraph> lstGraphs = new ArrayList<>();

        boolean rcnEnabled = FragmentSpace.useAPclassBasedApproach();
        if (!rcnEnabled)
            return lstGraphs;

        boolean evaluateRings = RingClosureParameters.allowRingClosures();
        if (!evaluateRings)
            return lstGraphs;

        // get a atoms/bonds molecular representation (no 3D needed)
        IAtomContainer mol = GraphConversionTool.convertGraphToMolecule(molGraph,false);
        
        // Set rotatability property as property of IBond
        ArrayList<ObjectPair> rotBonds =
                                  RotationalSpaceUtils.defineRotatableBonds(mol,
                                   FragmentSpaceParameters.getRotSpaceDefFile(),
                                                                    true, true);

        // get the set of possible RCA combinations = ring closures
        CyclicGraphHandler cgh = new CyclicGraphHandler(
                                             FragmentSpace.getScaffoldLibrary(),
                                             FragmentSpace.getFragmentLibrary(),
                                              FragmentSpace.getCappingLibrary(),
                                      FragmentSpace.getRCCompatibilityMatrix());
         ArrayList<Set<DENOPTIMRing>> allCombsOfRings =
                               cgh.getPossibleCombinationOfRings(mol, molGraph);
         
        // Keep closable chains that are relevant for chelate formation
        if (RingClosureParameters.buildChelatesMode())
        {
            ArrayList<Set<DENOPTIMRing>> toRemove = new ArrayList<>();
            for (Set<DENOPTIMRing> setRings : allCombsOfRings)
            {
                if (!cgh.checkChelatesGraph(molGraph,setRings))
                {
                    toRemove.add(setRings);
                }
            }
            allCombsOfRings.removeAll(toRemove);
        }

        // prepare output graphs
        for (Set<DENOPTIMRing> ringSet : allCombsOfRings)
        {
            // clone root graph
          //TODO-V3 get rid of serialization-based deep copying
            DENOPTIMGraph newGraph = (DENOPTIMGraph) DenoptimIO.deepCopy(
                                                                      molGraph);
            HashMap<Integer,Integer> vRenum = newGraph.renumberVerticesGetMap();
            newGraph.setGraphId(getUniqueGraphIndex());

            // add rings
            for (DENOPTIMRing oldRing : ringSet)
            {
                DENOPTIMRing newRing = new DENOPTIMRing();
                for (int i=0; i<oldRing.getSize(); i++)
                {
                    int oldVId = oldRing.getVertexAtPosition(i).getVertexId();
                    int newVId = vRenum.get(oldVId);
                    newRing.addVertex(newGraph.getVertexWithId(newVId));
                }
                newRing.setBondType(oldRing.getBondType());
                newGraph.addRing(newRing);
            }

            // store 
            lstGraphs.add(newGraph);
        }

        return lstGraphs;
    }

//------------------------------------------------------------------------------

    /**
     * Check if there are forbidden ends: free attachment points that are not
     * suitable for capping and not allowed to stay unused.
     *
     * @param molGraph the Graph representation of the molecule
     * @return <code>true</code> if a forbidden end is found
     */

    public static boolean foundForbiddenEnd(DENOPTIMGraph molGraph)
    {
        ArrayList<DENOPTIMVertex> vertices = molGraph.getVertexList();
        Set<String> classOfForbEnds = FragmentSpace.getForbiddenEndList();
        boolean found = false;
        for (DENOPTIMVertex vtx : vertices)
        {
            ArrayList<DENOPTIMAttachmentPoint> daps = vtx.getAttachmentPoints();
            for (DENOPTIMAttachmentPoint dp : daps)
            {
                if (dp.isAvailable())
                {
                    String apClass = dp.getAPClass();
                    if (classOfForbEnds.contains(apClass))
                    {
                        found = true;
                        String msg = "Forbidden free AP for Vertex: "
                            + vtx.getVertexId() + " "
                            + vtx.toString()
                            + "\n"+ molGraph+" \n "
                            + " AP class: " + apClass;
                        DENOPTIMLogger.appLogger.log(Level.WARNING, msg);
                        break;
                    }
                }
            }
            if (found)
            {
                break;
            }
        }

        return found;
    }

//------------------------------------------------------------------------------

    /**
     * Collect the list of closable paths involving the given scaffold
     * @param scaf
     * @param libraryOfRCCs
     * @return 
     * @throws denoptim.exception.DENOPTIMException 
     */

//TODO del if not used. Should be replaced by a more efficient approach
// using the map of ClosableChains per fragment MolID/type

    public static ArrayList<ClosableChain> getClosableVertexChainsFromDB(
                        DENOPTIMVertex scaf,
                        HashMap<String,ArrayList<String>> libraryOfRCCs)
                                                throws DENOPTIMException
//                String rccIndexFile,
//                String rccRootFolder) throws DENOPTIMException
    {
        ArrayList<ClosableChain> clbChains = new ArrayList<>();

        for (String chainId : libraryOfRCCs.keySet())
        {
            String closability = libraryOfRCCs.get(chainId).get(1);
            if (closability.equals("T"))
            {
                ClosableChain cc = new ClosableChain(chainId);
                int pos = cc.involvesVertex(scaf);
                if (pos != -1)
                {
                    clbChains.add(cc);
                }
            }
        }

        return clbChains;
    }

//------------------------------------------------------------------------------    

    /**
     * Compare two DENOPTIMGraphs. It returns true only if the two graphs, 
     * which can be either equal or different spanning trees, project the same 
     * arrangement of connected verteces. Note that the connections
     * may be DENOPTIMEdges or cyclic bonds that result only from the 
     * definition of DENOPTIMRings.
     * TODO: this first implementation is to be further tested and get mature.
     * @param gA the first graph
     * @param gB the second graph
     * @return <code>true</code> if the two graphs define the same arrangement 
     * of verteces and connections.
     * @throws denoptim.exception.DENOPTIMException
     */

     public static boolean equivalentGraphs(DENOPTIMGraph gA, DENOPTIMGraph gB)
                                                        throws DENOPTIMException
     {
        boolean result = false;

        // fast comparison of number of verteces and rings
        if ((gA.getRingCount() != gB.getRingCount()) ||
            (gA.getVertexCount() != gB.getVertexCount()))
        {
            return false;
        }

        ArrayList<Integer> visitedA = new ArrayList<Integer>();
        ArrayList<Integer> visitedB = new ArrayList<Integer>();
        result = exploreGraphs(gA, gB, 0, 0, -1, -1, visitedA, visitedB, 
                                                                false, false);

        return result;
     }

//------------------------------------------------------------------------------

    /**
     * This method evaluates equality of two given verteces each belonging to
     * a given graph. If verteces are found to be equal the method moves
     * (recursively) to one of the neighbour verteces connected by edge or
     * by cyclic bond (that is, DENOPTIMRing).
     * @param gA the first graph (A)
     * @param gB the second graph (B)
     * @param cvA index current vertex on graph A
     * @param cvB index current vertex on graph B
     * @param ceA index corrent edge on A
     * @param ceB index corrent edge on B
     * @param visitedA the list of visited IDs on A
     * @param visitedB the list of visited IDs on B
     * @param backwardA true if the direction of the exploration on A is
     * towards the scaffold
     * @param backwardB true if the direction of the exploration on B is
     * towards the scaffold
     * @return <code>true</code> if the current pair of verteces and the whole
     * branches starting from them, descibe the same arrangement of verteces.
     */

     private static boolean exploreGraphs(DENOPTIMGraph gA, 
                                   DENOPTIMGraph gB,
                                   int cvA,
                                   int cvB,
                                   int ceA,
                                   int ceB,
                                   ArrayList<Integer> visitedA,
                                   ArrayList<Integer> visitedB,
                                   boolean backwardA,
                                   boolean backwardB)
                                                throws DENOPTIMException        
    {
        boolean areEqual = false;

        DENOPTIMVertex vA = gA.getVertexAtPosition(cvA);
        DENOPTIMVertex vB = gB.getVertexAtPosition(cvB);

        ArrayList<Integer> addedToVisitedA = new ArrayList<Integer>();
        ArrayList<Integer> addedToVisitedB = new ArrayList<Integer>();

        // Check vA as ring closing attractor (RCA) vertex
        ArrayList<DENOPTIMRing> ringsOnVA = new ArrayList<DENOPTIMRing>();
        for (DENOPTIMRing r : gA.getRings())
        {
            if (vA == r.getHeadVertex() || vA == r.getTailVertex())
            {
                ringsOnVA.add(r);
            }
        }
        if (ringsOnVA.size() > 1)
        {
            throw new DENOPTIMException("Unecpected vertex being head/tail of "
                                        + "more than one DENOPTIMRing. Check "
                                        + "graph: " + gA);
        }
        // Check vB as ring closing attractor (RCA) vertex
        ArrayList<DENOPTIMRing> ringsOnVB = new ArrayList<>();
        for (DENOPTIMRing r : gB.getRings())
        {
            if (vB == r.getHeadVertex() || vB == r.getTailVertex())
            {
                ringsOnVB.add(r);
            }
        }
        if (ringsOnVB.size() > 1)
        {
            throw new DENOPTIMException("Unecpected vertex being head/tail of "
                                        + "more than one DENOPTIMRing. Check "
                                        + "graph: " + gB);
        }

        // In case of RCA vertex move to the next non-RCA vertex in the ring
        int apVAToBack = -1;
        boolean nextDirOnA = backwardA;
        if (ringsOnVA.size() == 1)
        {
            DENOPTIMRing r = ringsOnVA.get(0);
            if (vA == r.getHeadVertex())
            {
                visitedA.add(vA.getVertexId());
                addedToVisitedA.add(vA.getVertexId());
                visitedA.add(r.getTailVertex().getVertexId());
                addedToVisitedA.add(r.getTailVertex().getVertexId());
                vA = r.getVertexAtPosition(r.getSize() - 2);
                ceA = gA.getIndexOfEdgeWithParent(vA.getVertexId());
                apVAToBack = gA.getEdgeAtPosition(ceA).getTargetDAP();
            }
            else if (vA == r.getTailVertex())
            {
                visitedA.add(vA.getVertexId());
                addedToVisitedA.add(vA.getVertexId());
                visitedA.add(r.getHeadVertex().getVertexId());
                addedToVisitedA.add(r.getHeadVertex().getVertexId());
                vA = r.getVertexAtPosition(1);
                ceA = gA.getIndexOfEdgeWithParent(
                                        r.getHeadVertex().getVertexId());
                apVAToBack = gA.getEdgeAtPosition(ceA).getSourceDAP();
            }
            nextDirOnA = true;
        }
        else
        {
            if (ceA >= 0)
            {
                if (backwardA)
                {
                    apVAToBack = gA.getEdgeAtPosition(ceA).getSourceDAP();
                }
                else
                {
                    apVAToBack = gA.getEdgeAtPosition(ceA).getTargetDAP();
                }
            }
        }
        // In case of RCA vertex move to the next non-RCA vertex in the ring
        int apVBToBack = -1;
        boolean nextDirOnB = backwardB;
        if (ringsOnVB.size() == 1)
        {
            DENOPTIMRing r = ringsOnVB.get(0);
            if (vB == r.getHeadVertex())
            {
                visitedB.add(vB.getVertexId());
                addedToVisitedB.add(vB.getVertexId());
                visitedB.add(r.getTailVertex().getVertexId());
                addedToVisitedB.add(r.getTailVertex().getVertexId());
                vB = r.getVertexAtPosition(r.getSize() - 2);
                ceB = gB.getIndexOfEdgeWithParent(vB.getVertexId());
                apVBToBack = gB.getEdgeAtPosition(ceB).getTargetDAP();
            }
            else if (vB == r.getTailVertex())
            {
                visitedB.add(vB.getVertexId());
                addedToVisitedB.add(vB.getVertexId());
                visitedB.add(r.getHeadVertex().getVertexId());
                addedToVisitedB.add(r.getHeadVertex().getVertexId());
                vB = r.getVertexAtPosition(1);
                ceB = gB.getIndexOfEdgeWithParent(
                                        r.getHeadVertex().getVertexId());
                apVBToBack = gB.getEdgeAtPosition(ceB).getSourceDAP();
            }
            nextDirOnB = true;
        }
        else
        {
            if (ceB >= 0)
            {
                if (backwardB)
                {
                    apVBToBack = gB.getEdgeAtPosition(ceB).getSourceDAP();
                }
                else
                {
                    apVBToBack = gB.getEdgeAtPosition(ceB).getTargetDAP();
                }
            }
        }

        // check if already visited
        if (visitedA.contains(vA.getVertexId()) && 
            visitedB.contains(vB.getVertexId()))
        {
            return true;
        }
        else if ((visitedA.contains(vA.getVertexId()) &&
                   !visitedB.contains(vB.getVertexId())) ||
                 (!visitedA.contains(vA.getVertexId()) &&
                   visitedB.contains(vB.getVertexId())))
        {
            return false;
        }

        // add to list of visited
        visitedA.add(vA.getVertexId());
        addedToVisitedA.add(vA.getVertexId());
        visitedB.add(vB.getVertexId());
        addedToVisitedB.add(vB.getVertexId());

        // Compare the current fragments
        boolean sameAPtoHere = false;
        if (apVAToBack<0 && apVBToBack<0)
        {
            // There is no edge before the scaffold
            sameAPtoHere = true;
        }
        else
        {
            sameAPtoHere = (apVAToBack == apVBToBack);
        }

        if (vA.sameAs(vB, new StringBuilder()) && sameAPtoHere)
        {
            // move to next level
            boolean inner = true;
            for (int nextAp=0; nextAp<vA.getAttachmentPoints().size(); nextAp++)
            {
                if (nextAp == apVAToBack)
                {
                    continue;
                }

                int nextVrtxAID = -1;
                int nextVrtxBID = -1;
                int nextEdgeAID = -1;
                int nextEdgeBID = -1;

                boolean branchAIsOver = true;
                boolean branchBIsOver = true;

                if (nextDirOnA)
                {
                    nextEdgeAID = gA.getIndexOfEdgeWithParent(vA.getVertexId());
                    DENOPTIMEdge e = gA.getEdgeAtPosition(nextEdgeAID);
                    nextVrtxAID = gA.getIndexOfVertex(e.getSourceVertex());
                    int nextVrtxAVrtxID = gA.getVertexAtPosition(nextVrtxAID).getVertexId();
                    if (visitedA.contains(nextVrtxAVrtxID))
                    {
                        nextDirOnA = false;
                    }
                    else
                    {
                        branchAIsOver = false;
                    }
                }
                if (!nextDirOnA)
                {
                    for (int ie=0; ie<gA.getEdgeCount(); ie++)
                    {
                        DENOPTIMEdge e = gA.getEdgeAtPosition(ie);
                        if (e.getSourceVertex() == vA.getVertexId() &&
                            e.getSourceDAP() == nextAp)
                        {
                            nextEdgeAID = ie;
                            nextVrtxAID =  gA.getIndexOfVertex(
                                                        e.getTargetVertex());
                            branchAIsOver = false;
                            break;
                        }
                    }
                }

                if (nextDirOnB)
                {
                    nextEdgeBID = gB.getIndexOfEdgeWithParent(vB.getVertexId());
                    DENOPTIMEdge e = gB.getEdgeAtPosition(nextEdgeBID);
                    nextVrtxBID = gB.getIndexOfVertex(e.getSourceVertex());
                    int nextVrtxBVrtxID = gB.getVertexAtPosition(nextVrtxBID).getVertexId();
                    if (visitedB.contains(nextVrtxBVrtxID))
                    {
                        nextDirOnB = false;
                    }
                    else
                    {
                        branchBIsOver = false;
                    }
                }
                if (!nextDirOnB)
                {
                    for (int ie=0; ie<gB.getEdgeCount(); ie++)
                    {
                        DENOPTIMEdge e = gB.getEdgeAtPosition(ie);
                        if (e.getSourceVertex() == vB.getVertexId() &&
                            e.getSourceDAP() == nextAp)
                        {
                            nextEdgeBID = ie;
                            nextVrtxBID = gB.getIndexOfVertex(
                                                        e.getTargetVertex());
                            branchBIsOver = false;
                            break;
                        }
                    }
                }

                if (branchAIsOver && branchBIsOver)
                {
                    return true;
                }
                else if ((branchAIsOver && !branchBIsOver) ||
                         (!branchAIsOver && branchBIsOver))
                {
                    return false;
                }
                
                inner = exploreGraphs(gA, gB, 
                                      nextVrtxAID, nextVrtxBID,
                                      nextEdgeAID, nextEdgeBID,
                                      visitedA, visitedB,
                                      nextDirOnA, nextDirOnB);

                if (!inner)
                {
                    break;
                }                
            }
            if (inner)
            {
                areEqual = true;
            }
        }

        if (!areEqual)
        {
            // Reset flag of visited
            visitedA.removeAll(addedToVisitedA);
            visitedB.removeAll(addedToVisitedB);
        }

        return areEqual;
    }

//------------------------------------------------------------------------------
    
    /**
     * connects 2 vertices based on their free AP connections
     * @param a vertex
     * @param b vertex
     * @return edge connecting the vertices
     */

    public static DENOPTIMEdge connectVertices(DENOPTIMVertex a,
                                                    DENOPTIMVertex b)
    {
        ArrayList<Integer> apA = a.getFreeAPList();
        ArrayList<Integer> apB = b.getFreeAPList();

        if (apA.isEmpty() || apB.isEmpty())
            return null;

        // select random APs - these are the indices in the list
        MersenneTwister rng = RandomUtils.getRNG();
        
        
        //int iA = apA.get(GAParameters.getRNG().nextInt(apA.size()));
        //int iB = apB.get(GAParameters.getRNG().nextInt(apB.size()));
        int iA = apA.get(rng.nextInt(apA.size()));
        int iB = apB.get(rng.nextInt(apB.size()));

        DENOPTIMAttachmentPoint dap_A = a.getAttachmentPoints().get(iA);
        DENOPTIMAttachmentPoint dap_B = b.getAttachmentPoints().get(iB);

        // if no reaction/class specific info available set to single bond
        int bndOrder = 1;

        // create a new edge
        DENOPTIMEdge edge = new DENOPTIMEdge(a.getVertexId(), b.getVertexId(),
                                                     iA, iB, bndOrder);

        // update the attachment point info
        dap_A.updateFreeConnections(-bndOrder); // decrement the connections
        dap_B.updateFreeConnections(-bndOrder); // decrement the connections

        return edge;
    }

//------------------------------------------------------------------------------

    /**
     * Attaches the specified fragment to the vertex using the specified pair
     * of AP.
     * @param molGraph
     * @param curVertex the vertex to which the fragment is to be attached
     * @param srcAPIdx index of the AP at which the fragment is to be attached
     * @param fId the fragment id in the library
     * @param fTyp the type of fragment (0: scaffold, 1: fragment, 2: cap)
     * @param trgAPIdx index of the AP on the incoming fragment
     * @return the id of the new vertex created
     * @throws DENOPTIMException
     */
    
    public static int attachNewFragmentAtAPWithAP (DENOPTIMGraph molGraph, 
                                                      DENOPTIMVertex curVertex,
                                                      int srcAPIdx, 
                                                      int fId, 
                                                      int fTyp,
                                                      int trgAPIdx) 
                                                      throws DENOPTIMException
    {
        DENOPTIMVertex incomingVertex = DENOPTIMVertex.newFragVertex(fId, fTyp);

        int lvl = curVertex.getLevel();
        incomingVertex.setLevel(lvl+1);
        
        
        //TODO-V3: check it this is really not needed anymore
        
        // get molecular representation of incoming fragment
        //IGraphBuildingBlock mol = FragmentSpace.getFragmentLibrary().get(fId);

        // identify the symmetric APs if any for this fragment vertex
        /*
    	if (bb instanceof DENOPTIMFragment)
    	{
    		IAtomContainer iac = ((DENOPTIMFragment) bb).getAtomContainer();
            ArrayList<SymmetricSet> lstCompatible = new ArrayList<>();
            for (int i = 0; i< daps.size()-1; i++)
            {
                ArrayList<Integer> lst = new ArrayList<>();
                Integer i1 = i;
                lst.add(i1);

                boolean alreadyFound = false;
                for (SymmetricSet previousSS : lstCompatible)
                {
                    if (previousSS.contains(i1))
                    {
                        alreadyFound = true;
                        break;
                    }
                }

                if (alreadyFound)
                {
                    continue;
                }

                DENOPTIMAttachmentPoint d1 = daps.get(i);
                for (int j = i+1; j< daps.size(); j++)
                {
                    DENOPTIMAttachmentPoint d2 = daps.get(j);
                    if (isCompatible(iac, d1.getAtomPositionNumber(),
                                                    d2.getAtomPositionNumber()))
                    {
                        // check if reactions are compatible
                        if (isFragmentClassCompatible(d1, d2))
                        {
                            Integer i2 = j;
                            lst.add(i2);
                        }
                    }
                }

                if (lst.size() > 1)
                {
                    lstCompatible.add(new SymmetricSet(lst));
                }
            }

            return lstCompatible;
        } else if (bb instanceof DENOPTIMTemplate) {
    	    return new ArrayList<>();
        }
    	DENOPTIMLogger.appLogger.log(Level.WARNING, "getMatchingAP returns null, but should not");
    	return null;
    	*/
        
        //TODO-V3: this should not be needed anymore: symmetry should come from the vertex
        /*
        ArrayList<SymmetricSet> simAP = mol.getSymmetricAPsSets();
        fragVertex.setSymmetricAP(simAP);
        */
        
        // identify the src AP (on the current vertex)
        ArrayList<DENOPTIMAttachmentPoint> curAPs =
                                                curVertex.getAttachmentPoints();
        DENOPTIMAttachmentPoint srcAP = curAPs.get(srcAPIdx);
        String srcAPCls = srcAP.getAPClass();
        
        // identify the target AP (on the appended vertex)
        DENOPTIMAttachmentPoint trgAP = incomingVertex.getAttachmentPoints()
                .get(trgAPIdx);

        String trgAPCls = trgAP.getAPClass();

        // create the new DENOPTIMEdge
        DENOPTIMEdge edge;
        edge = connectVertices(curVertex, incomingVertex, srcAPIdx, trgAPIdx, 
                                                            srcAPCls, trgAPCls);

        if (edge != null)
        {
            // update graph
            molGraph.addVertex(incomingVertex);
            molGraph.addEdge(edge);

            return incomingVertex.getVertexId();
        }

        return -1;
    }

//------------------------------------------------------------------------------

    /**
     * Append a graph (incoming=I) onto another graph (receiving=R).
     * This method ignores symmetry.
     * @param molGraph the receiving graph R, or parent
     * @param parentVrtx the source vertex of R on which I will be attached
     * @param parentAPIdx the attachment point on R's vertex to be
     * used to attach I
     * @param subGraph the incoming graph I, or child
     * @param childVrtx the vertex of I that is to be connected to R
     * @param childAPIdx the index of the attachment point on the vertex of I
     * that is to be connected to R
     * @param bndType the bond type between R and I
     */

    public static void appendGraphOnGraph(DENOPTIMGraph molGraph,
                                     DENOPTIMVertex parentVrtx,
                                     int parentAPIdx,
                                     DENOPTIMGraph subGraph,
                                     DENOPTIMVertex childVrtx,
                                     int childAPIdx,
                                     int bndType)
                                                        throws DENOPTIMException
    {
        appendGraphOnGraph(molGraph, parentVrtx, parentAPIdx,
                                       subGraph, childVrtx, childAPIdx, bndType,
                                    new HashMap<Integer,SymmetricSet>(), false);
    }

//------------------------------------------------------------------------------

    /**
     * Append a graph (incoming=I) onto another graph (receiving=R).
     * This method ignores symmetry.
     * @param molGraph the receiving graph R, or parent
     * @param parentVrtx the source vertix of R on which I will be attached
     * @param parentAPIdx the attachment point on R's verticx to be
     * used to attach I
     * @param subGraph the incoming graph I, or child
     * @param childVrtx the vertex of I that is to be connected to R
     * @param childAPIdx the index of the atachment point on the vertex of I
     * that is to be connected to R
     * @param bndType the bond type between R and I
     * @param symmetry use <code>true</code> to reproduce the same operation
     * onto symmetric parters and generate the relative symmetric sets.
     */

    public static void appendGraphOnGraph(DENOPTIMGraph molGraph,
                                     DENOPTIMVertex parentVrtx,
                                     int parentAPIdx,
                                     DENOPTIMGraph subGraph,
                                     DENOPTIMVertex childVrtx,
                                     int childAPIdx,
                                     int bndType,
                                     boolean symmetry)
                                                        throws DENOPTIMException
    {
        appendGraphOnGraph(molGraph, parentVrtx, parentAPIdx,
                                       subGraph, childVrtx, childAPIdx, bndType,
                                 new HashMap<Integer,SymmetricSet>(), symmetry);
    }
 
//------------------------------------------------------------------------------

    /**
     * Append a graph (incoming=I) onto another graph (receiving=R). 
     * Can append one or more copyes of the same graph. The corresponding
     * vertex and attachment point ID for each connection are given in
     * separated arrays.
     * @param molGraph the receiving graph R, or parent
     * @param parentVrtxs the list of source vertices of R on which a copy
     * of I is to be attached.
     * @param parentAPIdx the list of attachment points on R's vertices to be
     * used to attach I 
     * @param subGraph the incoming graph I, or child
     * @param childVrtx the vertex of I that is to be connected to R
     * @param childAPIdx the index of the atachment point on the vertex of I 
     * that is to be connected to R
     * @param bndType the bond type between R and I
     * @param onAllSymmAPs set to <code>true</code> to require the same graph I
     * to be attached on all available and symmetric APs on the same vertex of 
     * the AP indicated in the list.
     */
    
    public static void appendGraphOnGraph(DENOPTIMGraph molGraph,
                                     ArrayList<DENOPTIMVertex> parentVrtxs,
                                     ArrayList<Integer> parentAPIdx,
                                     DENOPTIMGraph subGraph,
                                     DENOPTIMVertex childVrtx,
                                     int childAPIdx,
                                     int bndType,
                                     boolean onAllSymmAPs)
                                                        throws DENOPTIMException
    {
        // Collector for symmetries created by appending copyes of subGraph
        Map<Integer,SymmetricSet> newSymSets = 
                                            new HashMap<Integer,SymmetricSet>();

        // Repeat append for each parent vertex while collecting symmetries
        for (int i=0; i<parentVrtxs.size(); i++)
        {
            appendGraphOnGraph(molGraph, parentVrtxs.get(i), parentAPIdx.get(i),
                                       subGraph, childVrtx, childAPIdx, bndType,
                                                     newSymSets, onAllSymmAPs);
        }
    }

//------------------------------------------------------------------------------
    
    /**
     * Append a graph (incoming=I) onto another graph (receiving=R). 
     * @param molGraph the receiving graph R, or parent
     * @param parentVrtx the vertex of R on which the a copy
     * of I is to be attached.
     * @param parentAPIdx the attachment point on R's vertex to be
     * used to attach I 
     * @param subGraph the incoming graph I, or child
     * @param childVrtx the vertex of I that is to be connected to R
     * @param childAPIdx the index of the attachment point on the vertex of I 
     * that is to be connected to R
     * @param bndType the bond type between R and I
     * @param newSymSets this parameter is only used to keep track
     * of the symmetric copies of I. Simply provide an empty data structure.
     * @param onAllSymmAPs set to <code>true</code> to require the same graph I
     * to be attached on all available and symmetric APs on the same vertex of 
     * the AP indicated in the list.
     */
    
    public static void appendGraphOnGraph(DENOPTIMGraph molGraph,
                                     DENOPTIMVertex parentVrtx,
                                     int parentAPIdx,
                                     DENOPTIMGraph subGraph,
                                     DENOPTIMVertex childVrtx,
                                     int childAPIdx,
                                     int bndType,
                                     Map<Integer,SymmetricSet> newSymSets,
                                     boolean onAllSymmAPs)
                                                        throws DENOPTIMException
    {
        SymmetricSet symAPs = parentVrtx.getSymmetricAPs(parentAPIdx);
        if (symAPs != null && onAllSymmAPs)
        {
            ArrayList<Integer> apLst = symAPs.getList();
            for (int i=0; i<apLst.size(); i++)
            {
                int idx = apLst.get(i);
                if (!parentVrtx.getAttachmentPoints().get(idx).isAvailable())
                {
                    continue;
                }
                appendGraphOnAP(molGraph, parentVrtx, idx,
                         subGraph, childVrtx, childAPIdx, bndType, newSymSets);
            }
        }
        else
        {
            appendGraphOnAP(molGraph, parentVrtx, parentAPIdx,
                         subGraph, childVrtx, childAPIdx, bndType, newSymSets);
        }
    }

//------------------------------------------------------------------------------
    
    /**
     * Append a subgraph (subGraph) to an existing graph (molGraph) specifying 
     * which vertex and attachment point to use for the connection.
     * Does not project on symmetrically related vertexes or
     * attachment points. No change in symmetric sets, apart from importing 
     * those sets that are already defined in the subgraph.
     * @param molGraph the receiving graph R, or parent
     * @param parentVrtx the vertex of R on which a copy
     * of I is to be attached.
     * @param parentAPIdx the attachment point on R's vertex to be
     * used to attach I 
     * @param subGraph the incoming graph I, or child graph.
     * @param childVrtx the vertex of I that is to be connected to R
     * @param childAPIdx the index of the attachment point on the vertex of I 
     * that is to be connected to R
     * @param bndType the bond type between R and I
     * @param newSymSets of symmetric sets. This parameter is only used to keep
     *               track
     * of the symmetric copies of I. Simply provide an empty data structure.
     */

    public static void appendGraphOnAP(DENOPTIMGraph molGraph,
                                        DENOPTIMVertex parentVrtx, 
                                        int parentAPIdx, 
                                        DENOPTIMGraph subGraph, 
                                        DENOPTIMVertex childVrtx, 
                                        int childAPIdx, 
                                        int bndType, 
                                        Map<Integer,SymmetricSet> newSymSets)
                                                    throws DENOPTIMException
    {
        // Clone and renumber the subgraph to ensure uniqueness
        DENOPTIMGraph sgClone = subGraph.clone();
        
        //DENOPTIMGraph sgClone = (DENOPTIMGraph) DenoptimIO.deepCopy(subGraph);
        sgClone.renumberGraphVertices();
               
        // Make the connection between molGraph and subGraph
        DENOPTIMVertex cvClone = sgClone.getVertexAtPosition(
                            subGraph.getIndexOfVertex(childVrtx.getVertexId()));

        DENOPTIMAttachmentPoint dap_Parent =
                              parentVrtx.getAttachmentPoints().get(parentAPIdx);
        DENOPTIMAttachmentPoint dap_Child =
                                  cvClone.getAttachmentPoints().get(childAPIdx);
        
        DENOPTIMEdge edge = null;
        if (FragmentSpace.useAPclassBasedApproach())
        {
            String rcnP = dap_Parent.getAPClass();
            String rcnC = dap_Child.getAPClass();
            edge = connectVertices(parentVrtx, cvClone, parentAPIdx, childAPIdx,
                                                                    rcnP, rcnC);
        }
        else
        {
            edge = new DENOPTIMEdge(parentVrtx.getVertexId(), 
                       cvClone.getVertexId(), parentAPIdx, childAPIdx, bndType);
            // decrement the num. of available connections
            dap_Parent.updateFreeConnections(-bndType); 
            dap_Child.updateFreeConnections(-bndType); 
        }
        if (edge == null)
        {
            String msg = "Program Bug in appendGraphOnAP: No edge created.";
            DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
            throw new DENOPTIMException(msg);
        }
        molGraph.addEdge(edge);

        // Import all vertices from cloned subgraph, i.e., sgClone
        for (int i=0; i<sgClone.getVertexList().size(); i++)
        {
            DENOPTIMVertex clonV = sgClone.getVertexList().get(i);
            DENOPTIMVertex origV = subGraph.getVertexList().get(i);

            molGraph.addVertex(sgClone.getVertexList().get(i));

            // also need to tmp store pointers to symmetric vertexes
            // TODO: check. Why is this working on subGraph and not on sgClone?
            if (subGraph.hasSymmetryInvolvingVertex(origV.getVertexId()))
            {
                if (newSymSets.containsKey(origV.getVertexId()))
                {
                    newSymSets.get(origV.getVertexId()).add(
                                                           clonV.getVertexId());
                }
                else
                {
                    newSymSets.put(origV.getVertexId(),
                               sgClone.getSymSetForVertexID(
                                 sgClone.getVertexList().get(i).getVertexId()));
                }
            }
            else
            {
                if (newSymSets.containsKey(origV.getVertexId()))
                {
                    newSymSets.get(origV.getVertexId()).add(
                                                           clonV.getVertexId());
                }
                else
                {
                    SymmetricSet ss = new SymmetricSet();
                    ss.add(clonV.getVertexId());
                    newSymSets.put(origV.getVertexId(),ss);
                }
            }
        }
        // Import all edges from cloned subgraph
        for (int i=0; i<sgClone.getEdgeList().size(); i++)
        {
            molGraph.addEdge(sgClone.getEdgeList().get(i));
        }
        // Import all rings from cloned subgraph
        for (int i=0; i<sgClone.getRings().size(); i++)
        {
            molGraph.addRing(sgClone.getRings().get(i));
        }

        // project tmp symmetric set into final symmetric sets
        Set<SymmetricSet> doneTmpSymSets = new HashSet<SymmetricSet>();
        for (Map.Entry<Integer,SymmetricSet> e : newSymSets.entrySet())
        {
            SymmetricSet tmpSS = e.getValue();
            if (doneTmpSymSets.contains(tmpSS))
            {
                continue;
            }
            doneTmpSymSets.add(tmpSS);
            boolean done = false;
            // NB: no need to check all entries of tmpSS: the first is enough
            SymmetricSet oldSS;
            Iterator<SymmetricSet> iter = molGraph.getSymSetsIterator();
            while (iter.hasNext())
            {
                oldSS = iter.next();
                if (oldSS.contains(tmpSS.getList().get(0)))
                {
                    done = true;
                    for (Integer symVrtID : tmpSS.getList())
                    {
                        // NB this adds only if not already contained
                        oldSS.add(symVrtID); 
                    }
                    break;
                }
            }
            if (!done)
            {
                if (tmpSS.size() <= 1)
                {
                    // tmpSS has always at leas one entry: the initial vrtId
                    continue;
                }
                //Move tmpSS into a new SS on molGraph
                SymmetricSet newSS = new SymmetricSet();
                for (Integer symVrtID : tmpSS.getList())
                {
                    newSS.add(symVrtID);
                }
                molGraph.addSymmetricSetOfVertices(newSS);
            }
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Search a graph for vertices that match the criteria defined in a query
     * vertex.
     * @param graph the graph to be searched
     * @param query the query
     * @param onlyOneSymm if <code>true</code> then only one match will be 
     * collected for each symmetric set of partners.
     * @param verbosity the verbosity level
     * @return the list of matches
     */
    public static ArrayList<Integer> findVerticesIds(DENOPTIMGraph graph, 
                  DENOPTIMVertexQuery query, boolean onlyOneSymm, int verbosity)
    {
        ArrayList<Integer> matches = new ArrayList<Integer>();
        for (DENOPTIMVertex v : findVertices(graph,query,onlyOneSymm,verbosity))
        {
            matches.add(v.getVertexId());
        }
        return matches;
    }

//-----------------------------------------------------------------------------

    /**
     * Filters a list of vertices according to a query.
     * vertex.
     * @param graph the graph to be searched
     * @param dnQuery the query
     * @param onlyOneSymm if <code>true</code> then only one match will be 
     * collected for each symmetric set of partners.
     * @param verbosity the verbosity level
     * @return the list of matched vertices
     */

    public static ArrayList<DENOPTIMVertex> findVertices(DENOPTIMGraph graph,
                DENOPTIMVertexQuery dnQuery, boolean onlyOneSymm, int verbosity)
    {
        DENOPTIMVertex vQuery = dnQuery.getVrtxQuery();
        DENOPTIMEdge eInQuery = dnQuery.getInEdgeQuery();
        DENOPTIMEdge eOutQuery = dnQuery.getOutEdgeQuery();
        
        ArrayList<DENOPTIMVertex> matches = new ArrayList<DENOPTIMVertex>();
        matches.addAll(graph.getVertexList());

        if (verbosity > 1)
        {
            System.out.println("Searching vertices - candidates: " + matches);
        }

        //Check condition vertex ID
        int query = vQuery.getVertexId();
        if (query > -1) //-1 would be the wildcard
        {
            ArrayList<DENOPTIMVertex> newLst = new ArrayList<DENOPTIMVertex>();
            for (DENOPTIMVertex v : matches)
            {
                if (v.getVertexId() == query)
                {
                    newLst.add(v);
                }
            }
            matches = newLst;
        }

        if (verbosity > 2)
        {
            System.out.println("After ID-based rule: " + matches);
        }

        //Check condition fragment ID
        if (vQuery instanceof DENOPTIMFragment)
        {
            query = ((DENOPTIMFragment) vQuery).getMolId();
            if (query > -1) //-1 would be the wildcard
            {
                ArrayList<DENOPTIMVertex> newLst = new ArrayList<DENOPTIMVertex>();
                for (DENOPTIMVertex v : matches)
                {
                    if (v instanceof DENOPTIMFragment == false)
                    {
                        continue;
                    }
                    if (((DENOPTIMFragment) v).getMolId() == query)
                    {
                        newLst.add(v);
                    }
                }
                matches = newLst;
            }

            if (verbosity > 2)
            {
                System.out.println("After MolID-based rule: " + matches);
            }
    
            //Check condition fragment type
            query = ((DENOPTIMFragment) vQuery).getFragmentType();
            if (query > -1) //-1 would be the wildcard
            {
                ArrayList<DENOPTIMVertex> newLst = new ArrayList<DENOPTIMVertex>();
                for (DENOPTIMVertex v : matches)
                {
                    if (v instanceof DENOPTIMFragment == false)
                    {
                        continue;
                    }
                    if (((DENOPTIMFragment) v).getFragmentType() == query)
                    {
                        newLst.add(v);
                    }
                }
                matches = newLst;
            }
    
            if (verbosity > 2)
            {
                System.out.println("After Frag-type rule: " + matches);
            }
        }

        //Check condition level of vertex
        query = vQuery.getLevel();
        if (query > -2) //-2 would be the wildcard
        {
            ArrayList<DENOPTIMVertex> newLst = new ArrayList<DENOPTIMVertex>();
            for (DENOPTIMVertex v : matches)
            {
                if (v.getLevel() == query)
                {
                    newLst.add(v);
                }
            }
            matches = newLst;
        }

        if (verbosity > 1)
        {
            System.out.println("After Vertex-based rules: " + matches);
        }

        //Incoming connections (candidate vertex is the target)
        if (eInQuery != null)
        {
            //Check condition target AP
            query = eInQuery.getTargetDAP();
            if (query > -1)
            {
                ArrayList<DENOPTIMVertex> newLst = 
                                               new ArrayList<DENOPTIMVertex>();
                for (DENOPTIMVertex v : matches)
                {
		    if (graph.getIndexOfEdgeWithParent(v.getVertexId()) < 0)
		    {
			continue;
		    }
                    DENOPTIMEdge e = graph.getEdgeWithParent(v.getVertexId());
                    if (e!=null && e.getTargetDAP() == query)
                    {
                        newLst.add(v);
                    }
                }
                matches = newLst;
            }

            if (verbosity > 2)
            {
                System.out.println("After OutEdge-srcAP rule: "+matches);
            }
    
            //Check condition bond type
            query = eInQuery.getBondType();
            if (query > -1)
            {
                ArrayList<DENOPTIMVertex> newLst = 
                                               new ArrayList<DENOPTIMVertex>();
                for (DENOPTIMVertex v : matches)
                {
                    if (graph.getIndexOfEdgeWithParent(v.getVertexId()) < 0)
                    {
                        continue;
                    }
                    DENOPTIMEdge e = graph.getEdgeWithParent(v.getVertexId());
                    if (e!=null && e.getBondType() == query)
                    {
                        newLst.add(v);
                    }
                }
                matches = newLst;
            }

            if (verbosity > 2)
            {
                System.out.println("After InEdge-bond rule: "+matches);
            }
    
            //Check condition AP class
            String squery = eInQuery.getTargetReaction();
            if (!squery.equals("*"))
            {
                ArrayList<DENOPTIMVertex> newLst = 
                                               new ArrayList<DENOPTIMVertex>();
                for (DENOPTIMVertex v : matches)
                {
                    if (graph.getIndexOfEdgeWithParent(v.getVertexId()) < 0)
                    {
                        continue;
                    }
                    DENOPTIMEdge e = graph.getEdgeWithParent(v.getVertexId());
                    if (e!=null && e.getTargetReaction().equals(squery))
                    {
                        newLst.add(v);
                    }
                }
                matches = newLst;
            }
            squery = eInQuery.getSourceReaction();
            if (!squery.equals("*"))
            {
                ArrayList<DENOPTIMVertex> newLst =
                                               new ArrayList<DENOPTIMVertex>();
                for (DENOPTIMVertex v : matches)
                {
                    if (graph.getIndexOfEdgeWithParent(v.getVertexId()) < 0)
                    {
                        continue;
                    }
                    DENOPTIMEdge e = graph.getEdgeWithParent(v.getVertexId());
                    if (e!=null && e.getSourceReaction().equals(squery))
                    {
                        newLst.add(v);
                    }
                }
                matches = newLst;
            }
        }

        if (verbosity > 1)
        {
            System.out.println("After InEdge-based rules: " + matches);
        }

        //Outcoming connections (candidate vertex is the source)
        if (eOutQuery != null)
        {
            //Check condition target AP
            query = eOutQuery.getSourceDAP();
            if (query > -1)
            {
                ArrayList<DENOPTIMVertex> newLst = 
                                               new ArrayList<DENOPTIMVertex>();
                for (DENOPTIMVertex v : matches)
                {
                    for (DENOPTIMEdge e : graph.getEdgesWithChild(
                                                              v.getVertexId()))
                    {
                        if (e.getSourceDAP() == query)
                        {
                            newLst.add(v);
                            break;
                        }
                    }
                }
                matches = newLst;
            }

            if (verbosity > 2)
            {
                System.out.println("After OutEdge-srcAP rule: "+matches);
            }

            //Check condition bond type
            query = eOutQuery.getBondType();
            if (query > -1)
            {
                ArrayList<DENOPTIMVertex> newLst = 
                                               new ArrayList<DENOPTIMVertex>();
                for (DENOPTIMVertex v : matches)
                {
                    for (DENOPTIMEdge e : graph.getEdgesWithChild(
                                                              v.getVertexId()))
                    {
                        if (e.getBondType() == query)
                        {
                            newLst.add(v);
                            break;
                        }
                    }
                }
                matches = newLst;
            }

            if (verbosity > 2)
            {
                System.out.println("After OutEdge-bond rule: "+matches);
            }

            //Check condition AP class
            String squery = eOutQuery.getTargetReaction();
            if (!squery.equals("*"))
            {
                ArrayList<DENOPTIMVertex> newLst =
                                               new ArrayList<DENOPTIMVertex>();
                for (DENOPTIMVertex v : matches)
                {
                    for (DENOPTIMEdge e : graph.getEdgesWithChild(
                                                              v.getVertexId()))
                    {
                        if (e.getTargetReaction().equals(squery))
                        {
                            newLst.add(v);
                            break;
                        }
                    }
                }
                matches = newLst;
            }
            squery = eOutQuery.getSourceReaction();
            if (!squery.equals("*"))
            {
                ArrayList<DENOPTIMVertex> newLst = 
                                               new ArrayList<DENOPTIMVertex>();
                for (DENOPTIMVertex v : matches)
                {
                    for (DENOPTIMEdge e : graph.getEdgesWithChild(
                                                              v.getVertexId()))
                    {
                        if (e.getSourceReaction().equals(squery))
                        {
                            newLst.add(v);
                            break;
                        }
                    }
                }
                matches = newLst;
            }
        }

        if (verbosity > 1)
        {
            System.out.println("After OutEdge-based rule: " + matches);
        }

        // Identify symmetiric sets and keep only one member
	removeSymmetryRedundance(graph,matches);

        if (verbosity > 1)
        {
            System.out.println("Final Matches (after symmetry): " + matches);
        }

        return matches;
    }

//------------------------------------------------------------------------------

    /**
     * Edit a graph according to a given list of edit tasks.
     * @param graph the graph to edit
     * @param edits the list of edit tasks
     * @param symmetry if <code>true</code> the symmetry is enforced
     * @param verbosity the verbosity level
     * @return the modified graph
     */

    public static DENOPTIMGraph editGraph(DENOPTIMGraph graph, 
            ArrayList<DENOPTIMGraphEdit> edits, boolean symmetry, int verbosity)
                                                        throws DENOPTIMException
    {
		//Make sure there is no clash with vertex IDs
		int maxId = graph.getMaxVertexId();
		if (vertexCounter.get() <= maxId)
		{
		    try
		    {
		        resetUniqueVertexCounter(maxId+1);
		    }
		    catch (Throwable t)
		    {
			maxId = vertexCounter.getAndIncrement();
		    }
		}

		//TODO-V3 get rid of serialization-based deep copying
        DENOPTIMGraph modGraph = (DENOPTIMGraph) DenoptimIO.deepCopy(graph);
        
        for (DENOPTIMGraphEdit edit : edits)
        {
            String task = edit.getType();
            switch (task.toUpperCase())
            {
                case (DENOPTIMGraphEdit.REPLACECHILD):
                {
                    DENOPTIMEdge e =  edit.getFocusEdge();
                    DENOPTIMGraph inGraph = edit.getIncomingGraph();
                    DENOPTIMVertexQuery query = new DENOPTIMVertexQuery(
                                                         edit.getFocusVertex(),
                                                                          null,
                                                           edit.getFocusEdge());
                    ArrayList<Integer> matches = GraphUtils.findVerticesIds(
                                             modGraph,query,symmetry,verbosity);
                    for (int pid : matches)
                    {
                        int wantedApID = edit.getFocusEdge().getSourceDAP();
                        String wantedApCl = 
                                        edit.getFocusEdge().getSourceReaction();
						ArrayList<Integer> symmUnqChilds = 
								modGraph.getChildVertices(pid);
						if (symmetry)
						{
						    removeSymmetryRedundantIds(modGraph,symmUnqChilds);
						}
                        for (int cid : symmUnqChilds)
                        {
                            // Apply the query on the src AP on the focus vertex
                            // -1 id the wildcard
                            int srcApId = modGraph.getEdgeWithParent(
                                                            cid).getSourceDAP();
                            if (wantedApID>-1 && wantedApID != srcApId)
                            {
                                continue;
                            }
                            // Apply the query on the AP Class 
                            String srcApCl = modGraph.getEdgeWithParent(
                                                       cid).getSourceReaction();
                            if (!wantedApCl.equals("*") 
                                && !wantedApCl.equals(srcApCl))
                            {
                                continue;
                            }
                            modGraph.deleteVertex(cid,symmetry);
                            int wantedTrgApId = e.getTargetDAP();
                            int trgApLstSize = inGraph.getVertexWithId(
                                           e.getTargetVertex()).getNumberOfAP();
                            if (wantedTrgApId >= trgApLstSize)
                            {
                                String msg = "Request to use AP number " 
                                + wantedTrgApId + " but only " + trgApLstSize
                                + " are found in the designated vertex.";
                                throw new DENOPTIMException(msg);
                            }
                            GraphUtils.appendGraphOnGraph(modGraph,
                                                 modGraph.getVertexWithId(pid),
                                                                       srcApId,
                                                                       inGraph,
                                  inGraph.getVertexWithId(e.getTargetVertex()),
                                                                 wantedTrgApId,
                                                               e.getBondType(),
                                                                      symmetry);
                        }
                    }
                    break;
                }
                case (DENOPTIMGraphEdit.DELETEVERTEX):
                {
                    DENOPTIMVertexQuery query =
                                 new DENOPTIMVertexQuery(edit.getFocusVertex(),
							   edit.getFocusEdge());
                    ArrayList<Integer> matches = GraphUtils.findVerticesIds(
                                             modGraph,query,symmetry,verbosity);
                    for (int vid : matches)
                    {
                        modGraph.deleteVertex(vid,symmetry);
                    }
                    break;
                }
            }
        }
        return modGraph;
    }

//-----------------------------------------------------------------------------

    /**
     * Remove all but one of the symmetry-related partners in a list.
     * @param graph the graph to which the vertices IDs belong
     * @param list the list of vertex IDs to be purged
     */

    public static void removeSymmetryRedundantIds(DENOPTIMGraph graph,
                                                        ArrayList<Integer> list)
    {
	ArrayList<DENOPTIMVertex> vList = new ArrayList<DENOPTIMVertex>();
	for (int vid : list)
	{
	    vList.add(graph.getVertexWithId(vid));
	}
	removeSymmetryRedundance(graph,vList);
	list.clear();
	for (DENOPTIMVertex v : vList)
	{
	    list.add(v.getVertexId());
	}
    }

//-----------------------------------------------------------------------------

    /**
     * Remove all but one of the symmetry-related partners in a list
     * @param graph the graph to which the vertices belong
     * @param vList the list of vertices to be purged
     */

    public static void removeSymmetryRedundance(
			    DENOPTIMGraph graph, ArrayList<DENOPTIMVertex> list)
    {
        ArrayList<DENOPTIMVertex> symRedundnt = new ArrayList<DENOPTIMVertex>();
        Iterator<SymmetricSet> itSymm = graph.getSymSetsIterator();
        while (itSymm.hasNext())
        {
            SymmetricSet ss = itSymm.next();
            for (DENOPTIMVertex v : list)
            {
                int vid = v.getVertexId();
                if (symRedundnt.contains(v))
                {
                    continue;
                }
                if (ss.contains(vid))
                {
                    for (Integer idVrtInSS : ss.getList())
                    {
                        if (idVrtInSS != vid)
                        {
                            symRedundnt.add(graph.getVertexWithId(idVrtInSS));
                        }
                    }
                }
            }
        }
        for (DENOPTIMVertex v : symRedundnt)
        {
            list.remove(v);
        }
    }

//------------------------------------------------------------------------------

}
