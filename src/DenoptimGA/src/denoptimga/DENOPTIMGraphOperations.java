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

package denoptimga;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.math3.random.MersenneTwister;
import org.openscience.cdk.isomorphism.mcss.RMap;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.IdFragmentAndAP;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.APClass;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMEdge;
import denoptim.molecule.DENOPTIMEdge.BondType;
import denoptim.molecule.DENOPTIMFragment.BBType;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.molecule.SymmetricSet;
import denoptim.rings.ChainLink;
import denoptim.rings.ClosableChain;
import denoptim.rings.RingClosureParameters;
import denoptim.utils.GraphUtils;
import denoptim.utils.MutationType;
import denoptim.utils.RandomUtils;

/**
 *
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */

public class DENOPTIMGraphOperations
{

    // set true to debug
    private static boolean debug = false;

//------------------------------------------------------------------------------

    /**
     * Identify pair of vertices that are suitable for crossover: 
     * @param male <code>DENOPTIMGraph</code> of one member (the male) of the
     * parents
     * @param female <code>DENOPTIMGraph</code> of one member (the female) of
     * the parents
     * @return a pair of vertex ids (male and female) that will be the crossover
     * points
     */

    protected static RMap locateCompatibleXOverPoints(DENOPTIMGraph male,
                                                    DENOPTIMGraph female)
    {
        ArrayList<RMap> xpairs = new ArrayList<>();

        for (int i=0; i<male.getVertexCount(); i++)
        {
            BBType mtype = male.getVertexAtPosition(i).getFragmentType();
            int mvid = male.getVertexAtPosition(i).getVertexId();
            int mfragid = male.getVertexAtPosition(i).getMolId();

            //System.out.println("Male Fragment ID: "+mvid+" type: "+mtype+" ffragid: "+mfragid);

            // if the fragment is a capping group or the scaffold itself ignore
            if (mtype == BBType.SCAFFOLD || mtype == BBType.CAP)
                continue;

            // get edge toward parent vertex
            // get the edge where the vertex in question is the destination vertex
            // or end vertex. the source vertex then will be the crossover point
            int medgeid = male.getIndexOfEdgeWithParent(mvid);
            DENOPTIMEdge medge = male.getEdgeAtPosition(medgeid);

            for (int j=0; j<female.getVertexCount(); j++)
            {
                BBType ftype = female.getVertexAtPosition(j).getFragmentType();
                int fvid = female.getVertexAtPosition(j).getVertexId();
                int ffragid = female.getVertexAtPosition(j).getMolId();
                

                //System.out.println("Female Fragment ID: "+fvid+" type: "+ftype+" ffragid: "+ffragid);

                if (ftype == BBType.SCAFFOLD || ftype == BBType.CAP)
                    continue;

                // fragment ids should not match or we get the same molecule
                if (mfragid == ffragid)
                    continue;

                // get edge toward parent vertex
                int fedgeid = female.getIndexOfEdgeWithParent(fvid);
                DENOPTIMEdge fedge = female.getEdgeAtPosition(fedgeid);
                
                //Check condition for considering this combination
                if (isCrossoverPossible(medge, fedge))
                {
                    // add the pair of vertex (one for the male, one for the female)
                    RMap rp = new RMap(mvid, fvid);
                    xpairs.add(rp);
                }
            }
        }

        if (xpairs.isEmpty())
            return null;
        else if (xpairs.size() == 1)
            return xpairs.get(0);
        else
        {
            MersenneTwister mtrand = RandomUtils.getRNG();
            //int idx = GAParameters.getRNG().nextInt(xpairs.size());
            int idx = mtrand.nextInt(xpairs.size());
            return xpairs.get(idx);
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Evaluate AP class-compatibility of a pair of edges with respect to
     * crossover.
     * The condition for performing AP class-compatible crossover is that the
     * AP class on source vertex of edge A is compatible with the AP class on 
     * target vertex of edge B, and that AP class on source vertex of edgeB is
     * compatible with the AP class on target vertex of edge A.
     * @param eA first edge of the pair
     * @param eB second edge of the pair
     * @return <code>true</code> if the condition is satisfied
     */
    private static boolean isCrossoverPossible(DENOPTIMEdge eA, DENOPTIMEdge eB)
    {
        APClass apClassSrcA = eA.getSrcAPClass();
        APClass apClassTrgA = eA.getTrgAPClass();
        APClass apClassSrcB = eB.getSrcAPClass();
        APClass apClassTrgB = eB.getTrgAPClass();
        
        if (apClassSrcA.isCPMapCompatibleWith(apClassTrgB))
        {
            if (apClassSrcB.isCPMapCompatibleWith(apClassTrgA))
            {
                return true;
            }
        }
        return false;
    }

//------------------------------------------------------------------------------

    /**
     * Substitutes a vertex. Deletes the given vertex from the graph that owns
     * it, and removes any child vertex (i.e., reachable from the given vertex
     * by a directed path). Then it tries to extent the graph from the the
     * parent vertex (i.e., the one that was originally holding the given 
     * vertex). Moreover, additional extension may occur on
     * any available attachment point of the parent vertex.
     * @param vertex to mutate (the given vertex).
     * @return <code>true</code> if substitution is successful
     * @throws DENOPTIMException
     */

    protected static boolean substituteFragment(DENOPTIMVertex vertex)
                                                    throws DENOPTIMException
    {
        int vid = vertex.getVertexId();
        DENOPTIMGraph graph = vertex.getGraphOwner();

        // first get the edge with the parent
        int eidx = graph.getIndexOfEdgeWithParent(vid);
        if (eidx == -1)
        {
            String msg = "Program Bug in substituteFragment: Unable to locate "
                    + "parent edge for vertex "+vertex+" in graph "+graph;
            DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
            throw new DENOPTIMException(msg);
        }

        // vertex id of the parent
        int pvid = graph.getEdgeAtPosition(eidx).getSrcVertex();
        DENOPTIMVertex pvertex = graph.getVertexWithId(pvid);

        // Need to remember symmetry because we are deleting the symm. vertices
        boolean symmetry = graph.hasSymmetryInvolvingVertex(vid);
        
        // delete the vertex and its children and all its symmetric partners
        deleteFragment(vertex);
        
        // extend the graph at this vertex but without recursion
        return extendGraph(graph, pvertex, false, symmetry);
    }

//------------------------------------------------------------------------------

    /**
     * Deletion mutation removes the vertex and also the
     * symmetric partners on its parent.
     * @param vertex
     * @return <code>true</code> if deletion is successful
     * @throws DENOPTIMException
     */

    //TODO-V3 distinguish between 
    // 1) removing a vertex and the branch starting on it
    // 2) removing a vertex and try to glue the child branch to the parent one
    
    protected static boolean deleteFragment(DENOPTIMVertex vertex)
                                                    throws DENOPTIMException
    {
        int vid = vertex.getVertexId();
        DENOPTIMGraph molGraph = vertex.getGraphOwner();

        if (molGraph.hasSymmetryInvolvingVertex(vid))
        {
            ArrayList<Integer> toRemove = new ArrayList<Integer>();
            for (int i=0; i<molGraph.getSymSetForVertexID(vid).size(); i++)
            {
                int svid = molGraph.getSymSetForVertexID(vid).getList().get(i); 
                toRemove.add(svid);
            }
            for (Integer svid : toRemove)
            {
                molGraph.removeBranchStartingAt(svid);
            }
        }
        else
        {
            molGraph.removeBranchStartingAt(vid);
        }

        if (molGraph.getVertexWithId(vid) == null && molGraph.getVertexCount() > 1)
            return true;
        return false;
    }

//------------------------------------------------------------------------------

    /**
     * function that will keep extending the graph. The
     * probability of addition depends on the growth probability scheme.
     *
     * @param molGraph molecular graph
     * @param curVertex vertex to which further fragments will be appended
     * @param extend if <code>true</code>, then the graph will be grown further 
     * (recursive mode)
     * @param symmetryOnAps if <code>true</code>, then symmetry will be applied
     * on the APs, no matter what. This is mostly needed to retain symmetry 
     * when performing mutations on the root vertex of a symmetric graph.
     * @throws exception.DENOPTIMException
     * @return <code>true</code> if the graph has been modified
     */
     
  //TODO-V3: with the vertex owner in place the parameter molGraph becomes redundant: remove molGraph parameter
  
    protected static boolean extendGraph(DENOPTIMGraph molGraph,
                                         DENOPTIMVertex curVertex, 
                                         boolean extend, 
                                         boolean symmetryOnAp) 
                                                        throws DENOPTIMException
    {   
        // return true if the append has been successful
        boolean status = false;

        // check if the fragment has available APs
        if (!curVertex.hasFreeAP())
        {
            if (debug)
                System.err.println("Cannot extend graph that has no free AP!");
            return status;
        }
        
        int lvl = curVertex.getLevel();
        int curVrtId = curVertex.getVertexId();
        int grphId = molGraph.getGraphId();

        if (debug)
        {
            System.err.println("---> Extending Graph " + grphId
                               + " on vertex " + curVrtId
                               + " which is in level " + lvl);
            System.err.println("     Grap: "+ molGraph);
        }

        ArrayList<Integer> addedVertices = new ArrayList<>();

        ArrayList<DENOPTIMAttachmentPoint> lstDaps = 
                                                curVertex.getAttachmentPoints();
        for (int apId=0; apId<lstDaps.size(); apId++)
        {
            if (debug)
            {
                System.err.println("Evaluating growth on AP-" + apId + " of "
                        + "vertex "+ curVrtId);
            }

            DENOPTIMAttachmentPoint curDap = lstDaps.get(apId);

            // is it possible to extend on this AP?
            if (!curDap.isAvailable())
            {
                if (debug)
                {
                    System.err.println("AP is aready in use.");
                }
                continue;
            }

            // Do we want to extend the graph at this AP?
            double growthProb = EAUtils.getGrowthProbabilityAtLevel(lvl);
            boolean fgrow =  RandomUtils.nextBoolean(growthProb);
            if (debug)
            {
                System.err.println("Growth probab. on this AP:" + growthProb);
            }
            if (!fgrow)
            {
                if (debug)
                {
                    System.err.println("Decided not to grow on this AP!");
                }
                continue;
            }

            // Apply closability bias in selection of next fragment
//TODO copy also DENOPTIMRing into growing graph or use denoptim.molecule.DENOPTIMTemplate
// which is yet to be developed.
            if (RingClosureParameters.allowRingClosures() && 
                      RingClosureParameters.selectFragmentsFromClosableChains())
            {
                boolean successful = attachFragmentInClosableChain(
                                                                 curVertex, 
                                                                 apId, 
                                                                 molGraph, 
                                                                 addedVertices);
                if (successful)
                {
                    continue;
                }
            }

            // find a compatible combination of fragment and AP
            IdFragmentAndAP chosenFrgAndAp = getFrgApForSrcAp(curVertex, apId);
            int fid = chosenFrgAndAp.getVertexMolId();
            if (fid == -1)
            {
                if (debug)
                {
                    System.err.println("No compatible fragment found.");
                }
                continue;
            }

            // Decide on symmetric substitution within this vertex...
            boolean cpOnSymAPs = applySymmetry(curDap.getAPClass());
            SymmetricSet symAPs = new SymmetricSet();
            if (curVertex.hasSymmetricAP() && (cpOnSymAPs || symmetryOnAp))
            {
                symAPs = curVertex.getSymmetricAPs(apId);
				if (symAPs != null)
				{
                    if (debug)
                    {
                        System.err.println("Applying intra-fragment symmetric "
                                          + "substitution over APs: " + symAPs);
                    }
                }
				else
				{
				    symAPs = new SymmetricSet();
				    symAPs.add(apId);
				}
            }
            else
            {
                symAPs.add(apId);
            }

            // ...and inherit symmetry from previous levels
            boolean cpOnSymVrts = molGraph.hasSymmetryInvolvingVertex(curVrtId);
            SymmetricSet symVerts = new SymmetricSet();
            if (cpOnSymVrts)
            {
                symVerts = molGraph.getSymSetForVertexID(curVrtId);
                if (debug)
                {
                    System.err.println("Inheriting symmetry in vertices "
                                                                    + symVerts);
                }
            }
            else
            {
                symVerts.add(curVrtId);
            }

            // loop on all symmetric vertices, but can be only one.
            SymmetricSet newSymSetOfVertices = new SymmetricSet();
            for (Integer parVrtId : symVerts.getList())
            {
                DENOPTIMVertex parVrt = molGraph.getVertexWithId(parVrtId);
                String typ = "single";
                if (cpOnSymAPs || cpOnSymVrts)
                {
                    typ = "symmetric";
                }
                for (int si=0; si<symAPs.size(); si++)
                {
                    int symApId = symAPs.get(si);
                    if (!parVrt.getAttachmentPoints().get(
                                                        symApId).isAvailable())
                    {
                        continue;
                    }

                    // Finally add the fragment on a symmetric AP
                    int newVrtId = attachNewFragmentAtAP(molGraph, parVrt,
                                                        symApId,chosenFrgAndAp);
                    if (newVrtId != -1)
                    {
                        if (debug)
                        {
                            System.err.println("Added fragment " + newVrtId 
                                    + " on " + typ + " AP-" + symApId 
                                    + " of vertex " + parVrtId);
                        }
                    }
                    else
                    {
                        String msg = "BUG: Unsuccesfull fragment add on "
                                        + typ + " AP. Please, report this bug. "
                                        + "Growing graph: " + molGraph 
                                        + System.getProperty("line.separator") 
                                        + "Vertex/AP on growing graph: " 
                                        + chosenFrgAndAp;
                        DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                        throw new DENOPTIMException(msg);
                    }

                    addedVertices.add(newVrtId);
                    newSymSetOfVertices.add(newVrtId);
                }
            }

            // If any, store symmetry of new vertices in the graph
            if (newSymSetOfVertices.size() > 1)
            {
                molGraph.addSymmetricSetOfVertices(newSymSetOfVertices);
            }
        } // end loop over APs

        if (extend)
        {
            if (debug)
            {
                System.err.println("New level on graphID: " + grphId);
            }

            // attempt to further extend each of the newly added vertices
            for (int i=0; i<addedVertices.size(); i++)
            {
                int vid = addedVertices.get(i);
                DENOPTIMVertex fragVertex = molGraph.getVertexWithId(vid);
                extendGraph(molGraph, fragVertex, extend, symmetryOnAp);
            }
        }

        if (addedVertices.size() > 0)
            status = true;

        return status;
    }

//------------------------------------------------------------------------------

    /**
     * Attaches the specified fragment to the vertex
     * @param molGraph
     * @param curVertex the vertex to which a fragment is to be attached
     * @param dapIdx index of the AP at which the fragment is to be attached
     * @param chosenFrgAndAp index identifying the incoming fragment
     * @return the id of the new vertex created
     * @throws DENOPTIMException
     */
    
    protected static int attachNewFragmentAtAP
                              (DENOPTIMGraph molGraph, DENOPTIMVertex curVertex,
                                    int dapIdx, IdFragmentAndAP chosenFrgAndAp)
                                                        throws DENOPTIMException
    {
        // Define the new vertex
        int fid = chosenFrgAndAp.getVertexMolId();
        int nvid = GraphUtils.getUniqueVertexIndex();
        DENOPTIMVertex fragVertex = DENOPTIMVertex.newVertexFromLibrary(nvid, 
                fid, BBType.FRAGMENT);
        
        // update the level of the vertex based on its parent
        int lvl = curVertex.getLevel();
        fragVertex.setLevel(lvl+1);
        
        //TODO-V3: check that symmetry is indeed defined in the vertex upon creation
        
        /*
        DENOPTIMVertex mol = FragmentSpace.getFragmentLibrary().get(fid);
        ArrayList<SymmetricSet> simAP = mol.getSymmetricAPsSets();
        fragVertex.setSymmetricAP(simAP);
        */
        
        // get source: where the new fragment is going to be attached
        ArrayList<DENOPTIMAttachmentPoint> lstDaps =
                                                curVertex.getAttachmentPoints();
        DENOPTIMAttachmentPoint curDap = lstDaps.get(dapIdx);

        // make new edge connecting the current vertex with the new one
        DENOPTIMEdge edge;
        if (!FragmentSpace.useAPclassBasedApproach())
        {
            // connect a randomly selected AP of this fragment
            // to the current vertex
            edge = curVertex.connectVertices(fragVertex);
        }
        else
        {
            int fapidx = chosenFrgAndAp.getApId();
            APClass srcAPC = curDap.getAPClass();
            APClass trgAPC = fragVertex.getAttachmentPoints().get(fapidx).getAPClass();

            edge = curVertex.connectVertices(
                    fragVertex,
                    dapIdx,
                    fapidx,
                    srcAPC,
                    trgAPC);
        }

        if (edge != null)
        {
            // add the fragment as a vertex
            molGraph.addVertex(fragVertex);

            molGraph.addEdge(edge);

            return fragVertex.getVertexId();
        }

        return -1;
    }

//------------------------------------------------------------------------------

    /**
     * Select a compatible fragment for the given attachment point.
     * Compatibility can either be class based or based on the free connections
     * @param curVertex the source graph vertex
     * @param dapidx the attachment point index on the src vertex
     * @return the vector of indeces identifying the molId (fragment index) 
     * os a fragment with a compatible attachment point, and the index of such 
     * attachment point.
     * @throws DENOPTIMException
     */


    protected static IdFragmentAndAP getFrgApForSrcAp(DENOPTIMVertex curVertex, 
                                           int dapidx) throws DENOPTIMException
    {
        ArrayList<DENOPTIMAttachmentPoint> lstDaps =
                                              curVertex.getAttachmentPoints();
        DENOPTIMAttachmentPoint curDap = lstDaps.get(dapidx);
        
        // Initialize with an empty pointer
        IdFragmentAndAP res = new IdFragmentAndAP(-1, -1, BBType.FRAGMENT, -1, 
                -1, -1);
        if (!FragmentSpace.useAPclassBasedApproach())
        {
            //TODO-V3 get rid of EAUtils and use fragment space
            int fid = EAUtils.selectRandomFragment();
            res = new IdFragmentAndAP(-1,fid,BBType.FRAGMENT,-1,-1,-1);
        }
        else
        {
            ArrayList<IdFragmentAndAP> candidates = 
                    FragmentSpace.getFragAPsCompatibleWithClass(
                    curDap.getAPClass());
            if (candidates.size() > 0)
            {
                res = RandomUtils.randomlyChooseOne(candidates);
            }
        }
        return res;
    }

//------------------------------------------------------------------------------

    protected static boolean attachFragmentInClosableChain(
                                               DENOPTIMVertex curVertex, 
                                               int dapidx,
                                               DENOPTIMGraph molGraph,
                                               ArrayList<Integer> addedVertices)
                                                        throws DENOPTIMException
    {

//TODO evaluate this
/*
DENOPTIM/src/utils/GraphUtils.getClosableVertexChainsFromDB
*/

        boolean res = false;

        // Get candidate fragments
        ArrayList<FragForClosabChains> lscFfCc = getFragmentForClosableChain(
                                                                     curVertex,
                                                                        dapidx,
                                                                      molGraph);

        // Here we can get:
        // 1) a selection of fragments that allow to close rings
        // 2) an empty list because no fragments allow to close rings
        // 3) "-1" as molID that means there are closable chains that
        //    terminate at the current level, so we let the standard 
        //    routine proceeds selecting an extension of the chain
        //    or cap this end.

        // Choose a candidate and attach it to the graph
        int numCands = lscFfCc.size();
        if (numCands > 0)
        {
            MersenneTwister rng = RandomUtils.getRNG();
            int chosenId = rng.nextInt(numCands);
            FragForClosabChains chosenFfCc = lscFfCc.get(chosenId);
            ArrayList<Integer> newFragIds = chosenFfCc.getFragIDs();
            int molIdNewFrag = newFragIds.get(0);
            BBType typeNewFrag = BBType.parseInt(newFragIds.get(1));
            int dapNewFrag = newFragIds.get(2);
//TODO del
if(debug)
{
    System.out.println("Closable Chains involving frag/AP: "+newFragIds);
    int iii = 0;
    for (ClosableChain cc : chosenFfCc.getCompatibleCC())
    {
        iii++;
    System.out.println(iii+"  "+cc);
    }
}
            if (molIdNewFrag != -1)
            {
//TODO del
if(debug)
    System.out.println("Before attach: "+molGraph);
                int newvid = GraphUtils.attachNewFragmentAtAPWithAP(molGraph,
                                                                  curVertex,
                                                                   dapidx,
                                                                   molIdNewFrag,
                                                                   typeNewFrag,
                                                                   dapNewFrag);
//TODO del
if(debug)
    System.out.println("After attach: "+molGraph);

                if (newvid != -1)
                {
                    addedVertices.add(newvid);

//TODO del
if(debug)
    System.out.println("Before update: "+molGraph.getClosableChains().size());
                    // update list of candidate closable chains
                    molGraph.getClosableChains().removeAll(
                            chosenFfCc.getIncompatibleCC());
//TODO del
if(debug)
    System.out.println("After update: "+molGraph.getClosableChains().size());

                    if (applySymmetry(curVertex.getAttachmentPoints().get(
                            dapidx).getAPClass()))
                    {
//TODO
//TODO: implement symmetric substitution with closability bias
//TODO
                    }
                    res = true;
                }
                else
                {
                    String msg = "BUG: Incorrect vertex num. Contact author.";
                    DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                    throw new DENOPTIMException(msg);
                }
            }
        }

        return res;
    }

//------------------------------------------------------------------------------

    /**
     * Private class representig a selected closable chain of fragments
     */

    private static class FragForClosabChains
    {
        private ArrayList<ClosableChain> compatChains;
        private ArrayList<ClosableChain> incompatChains;
        private ArrayList<Integer> fragIds;

        //----------------------------------------------------------------------

        public FragForClosabChains(ArrayList<ClosableChain> compatChains,
                                   ArrayList<ClosableChain> incompatChains,
                                   ArrayList<Integer> fragIds)
        {
            this.compatChains = compatChains;
            this.incompatChains = incompatChains;
            this.fragIds = fragIds;
        }

        //----------------------------------------------------------------------

        public void addCompatibleCC(ClosableChain icc)
        {
            compatChains.add(icc);
        }

        //----------------------------------------------------------------------

        public void addIncompatibleCC(ClosableChain icc)
        {
            incompatChains.add(icc);
        }

        //----------------------------------------------------------------------

        public ArrayList<ClosableChain> getCompatibleCC()
        {
            return compatChains;
        }

        //----------------------------------------------------------------------

        public ArrayList<ClosableChain> getIncompatibleCC()
        {
            return incompatChains;
        }

        //----------------------------------------------------------------------

        public ArrayList<Integer> getFragIDs()
        {
            return fragIds;
        }

        //----------------------------------------------------------------------
    }

//------------------------------------------------------------------------------

    /**
     * Method to select fragments that increase the likeliness of generating
     * closable chains.
     * TODO: only works for scaffold. neet to make it working for any type
     * of fragment
     */

    protected static ArrayList<FragForClosabChains> getFragmentForClosableChain(
                                                       DENOPTIMVertex curVertex,
                                                       int dapidx,
                                                       DENOPTIMGraph molGraph)
                                                       throws DENOPTIMException
    {
        // Select candidate fragments respecting the closability conditions
        ArrayList<FragForClosabChains> lstChosenFfCc = 
                                        new ArrayList<FragForClosabChains>();
        Set<ArrayList<Integer>> candidateFrags = 
                                              new HashSet<ArrayList<Integer>>();

        if (molGraph.getClosableChains().size() == 0)
        {
            return lstChosenFfCc;            
        }

        if (curVertex.getFragmentType() == BBType.SCAFFOLD)  
        {
            for (ClosableChain cc : molGraph.getClosableChains())
            {
//TODO del 
if (debug)
{
    System.out.println(" ");
    System.out.println("dapidx:"+dapidx+" curVertex"+curVertex);
    System.out.println("ClosableChain: " + cc);
}

                int posInCc = cc.involvesVertex(curVertex);
                ChainLink cLink = cc.getLink(posInCc);
                int nfid = -1;
                BBType nfty = BBType.UNDEFINED;
                int nfap = -1;

                if (cLink.getApIdToLeft() != dapidx && 
                    cLink.getApIdToRight() != dapidx)
                {
                    // Chain does not involve AP dapidx
//TODO del
if(debug)
    System.out.println("HERE: dapidx:"+dapidx+" curVertex"+curVertex+" apL:"+cLink.getApIdToLeft()+" apR:"+cLink.getApIdToRight()+" "+posInCc);
                    continue;

                }

                if (cLink.getApIdToRight() == dapidx)
                {
                    if (cc.getSize() > (posInCc+1))
                    {
//TODO del
if(debug)
    System.out.println("A: "+dapidx+" apL:"+cLink.getApIdToLeft()+" apR:"+cLink.getApIdToRight()+" "+posInCc);

                        // cLink is NOT the rightmost chain link
                        ChainLink nextChainLink = cc.getLink(posInCc+1);
                        nfid = nextChainLink.getMolID();
                        nfty = nextChainLink.getFragType();
                        nfap = nextChainLink.getApIdToLeft();
                    }
                    else
                    {
//TODO del
if(debug)
    System.out.println("B: "+dapidx+" apL:"+cLink.getApIdToLeft()+" apR:"+cLink.getApIdToRight()+" "+posInCc);

                        // cLink is the rightmost chain link
                        // closability bias suggest NO fragment
                    }
                }
                else if (cLink.getApIdToLeft() == dapidx)
                {
                    if ((posInCc-1) >= 0)
                    {
//TODO del
if(debug)
    System.out.println("C: "+dapidx+" apL:"+cLink.getApIdToLeft()+" apR:"+cLink.getApIdToRight()+" "+posInCc);

                        // cLink is NOT the leftmost chain link
                        ChainLink nextChainLink = cc.getLink(posInCc-1);
                        nfid = nextChainLink.getMolID();
                        nfty = nextChainLink.getFragType();
                        nfap = nextChainLink.getApIdToRight();
                    }
                    else
                    {
//TODO del
if(debug)
    System.out.println("D: "+dapidx+" apL:"+cLink.getApIdToLeft()+" apR:"+cLink.getApIdToRight()+" "+posInCc);

                        // cLink is the leftmost chain link
                        // closability bias suggest NO fragment
                    }
                }

                ArrayList<Integer> eligibleFrgId = new ArrayList<Integer>();
                eligibleFrgId.add(nfid);
                eligibleFrgId.add(nfty.toOldInt());
                eligibleFrgId.add(nfap);
                boolean found = false;
                for (FragForClosabChains ffcc : lstChosenFfCc)
                {
                    int fidA = ffcc.getFragIDs().get(0);
                    BBType ftyA = BBType.parseInt(ffcc.getFragIDs().get(1));
                    int fapA = ffcc.getFragIDs().get(2);
                    if (nfid==fidA && nfty==ftyA && nfap==fapA)
                    {
                        found = true;
                        ffcc.getCompatibleCC().add(cc);
                    }
                    else
                    {
                        ffcc.getIncompatibleCC().add(cc);
                    }
                }
                if (!found)
                {
                    ArrayList<ClosableChain> compatChains =
                                                new ArrayList<ClosableChain>();
                    ArrayList<ClosableChain> incompatChains =
                                                new ArrayList<ClosableChain>();
                    for (FragForClosabChains otherFfCc : lstChosenFfCc)
                    {
                        incompatChains.addAll(otherFfCc.getCompatibleCC());
                    }
                    FragForClosabChains newChosenCc = new FragForClosabChains(
                                                                compatChains,
                                                                incompatChains,
                                                                eligibleFrgId);

                    newChosenCc.addCompatibleCC(cc);
                    lstChosenFfCc.add(newChosenCc);
                }

//TODO del 
if(debug)
{
    System.out.println("eligibleFrgId: "+eligibleFrgId);
    System.out.println("lstChosenFfCc.size: "+lstChosenFfCc.size());
    int iiii = 0;
    for (FragForClosabChains ffcc : lstChosenFfCc)
    {
      iiii++;
      System.out.println(iiii+" "+ffcc.getFragIDs()+" "+ffcc.getCompatibleCC().size()+ " "+ffcc.getIncompatibleCC().size());
    }
}
            }
        }
        else
        {
            DENOPTIMVertex parent = molGraph.getParent(curVertex.getVertexId());
            DENOPTIMEdge edge = molGraph.getEdgeAtPosition(
                                molGraph.getIndexOfEdgeWithParent(
                                             curVertex.getVertexId()));
            int prntId = parent.getMolId();
            BBType prntTyp = parent.getFragmentType();
            int prntAp = edge.getSrcAPID();
            int chidAp = edge.getTrgAPID();
            for (ClosableChain cc : molGraph.getClosableChains())
            {
//TODO del 
if(debug)
{
    System.out.println(" ");
    System.out.println("dapidx:"+dapidx+" curVertex"+curVertex);
    System.out.println("2-ClosableChain: " + cc);
}
                int posInCc = cc.involvesVertexAndAP(curVertex, dapidx, chidAp);

                if (posInCc == -1)
                {
                    // closable chain does not span this combination
                    // of vertex and APs
//TODO del
if(debug)
    System.out.println("2-HERE: dapidx:"+dapidx+" curVertex"+curVertex+" chidAp:"+chidAp);

                    continue;
                }

                ChainLink cLink = cc.getLink(posInCc);
                int nfid = -1;
                BBType nfty = BBType.UNDEFINED;
                int nfap = -1;

                int posScaffInCc = cc.getTurningPoint();

//TODO del
if(debug)
    System.out.println("posInCc: "+posInCc+" posScaffInCc: "+posScaffInCc);

                if (posInCc > posScaffInCc)
                {
                    ChainLink parentLink = cc.getLink(posInCc - 1);
                    int pLnkId = parentLink.getMolID();
                    BBType pLnkTyp = parentLink.getFragType();
                    int pLnkAp = parentLink.getApIdToRight();
//TODO del
if(debug)
{
    System.out.println("SCAFF is LEFT");
    System.out.println("parent leftside: "+parentLink); 
}
                    if (pLnkId==prntId && pLnkTyp==prntTyp && pLnkAp == prntAp  &&
                        cLink.getApIdToLeft() == chidAp)
                    {
                        if (cc.getSize() > (posInCc+1))
                        {
//TODO del
if(debug)
    System.out.println("2A: "+dapidx+" apL:"+cLink.getApIdToLeft()+" apR:"+cLink.getApIdToRight()+" "+posInCc);

                            ChainLink nextChainLink = cc.getLink(posInCc + 1);
                            nfid = nextChainLink.getMolID();
                            nfty = nextChainLink.getFragType();
                            nfap = nextChainLink.getApIdToLeft();
                        }
                        else
                        {
//TODO del
if(debug)
    System.out.println("2B: "+dapidx+" apL:"+cLink.getApIdToLeft()+" apR:"+cLink.getApIdToRight()+" "+posInCc);

                            // cLink is the rightmost chain link
                            // closability bias suggests NO fragment
                        }
                    }
                    else
                    {
                        // different parent link
//TODO del
if(debug)
{
    System.out.println("01: "+dapidx+" apL:"+cLink.getApIdToLeft()+" apR:"+cLink.getApIdToRight()+" "+posInCc);
    System.out.println("prntId: "+prntId+", prntTyp:"+prntTyp+", prntAp:"+prntAp+", chidAp:"+chidAp);
    System.out.println("pLnkId: "+pLnkId+", pLnkTyp:"+pLnkTyp+", pLnkAp:"+pLnkAp+", cLink.getApIdToLeft():"+cLink.getApIdToLeft());
    System.out.println("DIFFERENT PARENT LINK");
}
                        continue;
                    }
                }
                else if (posInCc < posScaffInCc)
                {
                    ChainLink parentLink = cc.getLink(posInCc + 1);
                    int pLnkId = parentLink.getMolID();
                    BBType pLnkTyp = parentLink.getFragType();
                    int pLnkAp = parentLink.getApIdToLeft();
//TODO del
if(debug)
{
    System.out.println("SCAFF is RIGHT");
    System.out.println("parent rightside: "+parentLink);
}
                    if (pLnkId==prntId && pLnkTyp==prntTyp && pLnkAp == prntAp  &&
                        cLink.getApIdToRight() == chidAp)
                    {
                        if ((posInCc-1) >= 0)
                        {
//TODO del
if(debug)
    System.out.println("2C: "+dapidx+" apL:"+cLink.getApIdToLeft()+" apR:"+cLink.getApIdToRight()+" "+posInCc);
                            ChainLink nextChainLink = cc.getLink(posInCc - 1);
                            nfid = nextChainLink.getMolID();
                            nfty = nextChainLink.getFragType();
                            nfap = nextChainLink.getApIdToRight();
                        }
                        else
                        {
//TODO del
if(debug)
    System.out.println("2D: "+dapidx+" apL:"+cLink.getApIdToLeft()+" apR:"+cLink.getApIdToRight()+" "+posInCc);

                            // cLink is the leftmost chain link
                            // closability bias suggest NO fragment
                        }
                    }
                    else
                    {
                        // different parent link
//TODO del
if(debug)
{
    System.out.println("02: "+dapidx+" apL:"+cLink.getApIdToLeft()+" apR:"+cLink.getApIdToRight()+" "+posInCc);
    System.out.println("prntId: "+prntId+", prntTyp:"+prntTyp+", prntAp:"+prntAp+", chidAp:"+chidAp);
    System.out.println("pLnkId: "+pLnkId+", pLnkTyp:"+pLnkTyp+", pLnkAp:"+pLnkAp+", cLink.getApIdToRight():"+cLink.getApIdToRight());
    System.out.println("DIFFERENT PARENT LINK");
}
                        continue;

                    }
                }
                // There is no case posInCc==posScaffInCc because is treated 
                // in the first IF block of this method

                ArrayList<Integer> eligibleFrgId = new ArrayList<Integer>();
                eligibleFrgId.add(nfid);
                eligibleFrgId.add(nfty.toOldInt());
                eligibleFrgId.add(nfap);
                boolean found = false;
                for (FragForClosabChains ffcc : lstChosenFfCc)
                {
                    int fidA = ffcc.getFragIDs().get(0);
                    BBType ftyA = BBType.parseInt(ffcc.getFragIDs().get(1));
                    int fapA = ffcc.getFragIDs().get(2);
                    if (nfid==fidA && nfty==ftyA && nfap==fapA)
                    {
                        found = true;
                        ffcc.getCompatibleCC().add(cc);
                    }
                    else
                    {
                        ffcc.getIncompatibleCC().add(cc);
                    }
                }
                if (!found)
                {
                    ArrayList<ClosableChain> compatChains =
                                                new ArrayList<ClosableChain>();
                    ArrayList<ClosableChain> incompatChains =
                                                new ArrayList<ClosableChain>();
                    for (FragForClosabChains otherFfCc : lstChosenFfCc)
                    {
                        incompatChains.addAll(otherFfCc.getCompatibleCC());
                    }
                    FragForClosabChains newChosenCc = new FragForClosabChains(
                                                                compatChains,
                                                                incompatChains,
                                                                eligibleFrgId);

                    newChosenCc.addCompatibleCC(cc);
                    lstChosenFfCc.add(newChosenCc);
                }
//TODO del 
if(debug)
{
    System.out.println("eligibleFrgId: "+eligibleFrgId);
    System.out.println("lstChosenFfCc.size: "+lstChosenFfCc.size());
    int iiii = 0;
    for (FragForClosabChains ffcc : lstChosenFfCc)
    {
      iiii++;
      System.out.println(iiii+" "+ffcc.getFragIDs()+" "+ffcc.getCompatibleCC().size()+ " "+ffcc.getIncompatibleCC().size());
    }
}
            }
        }

        return lstChosenFfCc;
    }

//------------------------------------------------------------------------------

    /**
     * Selects proper verices and performs crossover between two graphs
     * @param male the first Graph
     * @param female the second Graph
     * @return <code>true</code> if a new graph has been successfully produced
     * @throws DENOPTIMException 
     */

    public static boolean performCrossover(DENOPTIMGraph male,
                                DENOPTIMGraph female) throws DENOPTIMException
    {

        // This is done to maintain a unique vertex-id mapping
        male.renumberGraphVertices();
        female.renumberGraphVertices();

        if(debug)
        {
            System.err.println("DBUG: performCrossover " + male.getGraphId() +
                                                " " + female.getGraphId());
            System.err.println("DBUG: MALE(renum): " + male);
            System.err.println("DBUG: FEMALE(renum): " + female);
        }

        // select vertices for crossover
        int mvid, fvid;
        if (FragmentSpace.useAPclassBasedApproach())
        {
            // remove the capping groups
            male.removeCappingGroups();
            female.removeCappingGroups();
            
            // select vertices with compatible parent class
            RMap rp = locateCompatibleXOverPoints(male, female);
            //System.err.println("XOVER POINTS " + rp.toString());
            
            mvid = rp.getId1();
            fvid = rp.getId2();
        }
        else
        {
            MersenneTwister rng = RandomUtils.getRNG();
            // select randomly
            //int k1 = GAParameters.getRNG().nextInt(male.getVertexCount());
            //int k2 = GAParameters.getRNG().nextInt(female.getVertexCount());
            
            int k1 = rng.nextInt(male.getVertexCount());
            int k2 = rng.nextInt(female.getVertexCount());

            if (k1 == 0)
                k1 = k1 + 1; // not selecting the first vertex
            if (k2 == 0)
                k2 = k2 + 1; // not selecting the first vertex
            mvid = male.getVertexAtPosition(k1).getVertexId();
            fvid = female.getVertexAtPosition(k2).getVertexId();
        }

        return performCrossover(male,mvid,female,fvid);
    }

//------------------------------------------------------------------------------

    /**
     * Performs crossover between two graphs on a given pair of vertexIDs
     * @param male the first Graph
     * @param female the second Graph
     * @param mvid vertexID of the root vertex of the branch of male to exchange
     * @param fvid vertexID of the root vertex of the branch of female to
     * exchange
     * @par
     * @return <code>true</code> if a new graph has been successfully produced
     * @throws DENOPTIMException
     */

    public static boolean performCrossover(DENOPTIMGraph male, int mvid,
                        DENOPTIMGraph female, int fvid) throws DENOPTIMException
    {
        return performCrossover(male, mvid, female, fvid, false);
    }

//------------------------------------------------------------------------------

    /**
     * Performs crossover between two graphs on a given pair of vertexIDs
     * @param male the first Graph
     * @param female the second Graph
     * @param mvid vertexID of the root vertex of the branch of male to exchange
     * @param fvid vertexID of the root vertex of the branch of female to
     * exchange
     * @return <code>true</code> if a new graph has been successfully produced
     * @throws DENOPTIMException
     */

    public static boolean performCrossover(DENOPTIMGraph male, int mvid,
                        DENOPTIMGraph female, int fvid, boolean debug) 
                                throws DENOPTIMException
    {
        //TODO-V3 massive use of pointers (i.e., integers pointing to a position 
        // in a list of things) should be replaced with use of references
        
        if(debug)
        {
            System.err.println("Crossover on vertices " + mvid + " " + fvid);
            System.err.println("Male graph:   "+male);
            System.err.println("Female graph: "+female);
        }

        // get details about crossover points
        DENOPTIMVertex mvert = male.getVertexWithId(mvid);
        DENOPTIMVertex fvert = female.getVertexWithId(fvid);
        int eidxM = male.getIndexOfEdgeWithParent(mvid);
        int eidxF = female.getIndexOfEdgeWithParent(fvid);
        DENOPTIMEdge eM = male.getEdgeAtPosition(eidxM);
        DENOPTIMEdge eF = female.getEdgeAtPosition(eidxF);
        int apidxMP = eM.getSrcAPID(); // ap index of the male parent
        int apidxMC = eM.getTrgAPID(); // ap index of the male
        int apidxFC = eF.getTrgAPID(); // ap index of the female
        int apidxFP = eF.getSrcAPID(); // ap index of the female parent
        BondType bndOrder = eM.getBondType();

        if(debug)
        {
            System.err.println("Male XOVER vertex: " + mvert);
            System.err.println("Female XOVER vertex: " + fvert);
        }

        // Identify all verteces symmetric to the ones chosen for xover
        // Xover is to be projected on each of these
        
        ArrayList<Integer> symVrtIDs_M = 
                male.getSymSetForVertexID(mvert.getVertexId()).getList();
        ArrayList<Integer> symVrtIDs_F = 
                female.getSymSetForVertexID(fvert.getVertexId()).getList();
        
        if (debug)
        {
            System.out.println("DBUG: MALE sym sites: "+symVrtIDs_M);

            System.out.println("DBUG: FEMALE sym sites: "+symVrtIDs_F);
        }
        
        // MALE: Find all parent verteces and AP indeces where the incoming 
        // graph will have to be placed
        ArrayList<DENOPTIMVertex> symParVertM = new ArrayList<DENOPTIMVertex>();
        ArrayList<Integer> symmParAPidxM = new ArrayList<Integer>();
        ArrayList<Integer> toRemoveFromM = new ArrayList<Integer>();
        for (int i=0; i<symVrtIDs_M.size(); i++)
        {
            int svid = symVrtIDs_M.get(i);
            if (svid == mvid)
            {
                // We keep only the branch of the vertex identified for crossover
                continue;
            }
            // Store information on where the symmetric vertex is attached
            DENOPTIMEdge se = male.getEdgeWithParent(svid);
            DENOPTIMVertex spv = male.getParent(svid);
            symParVertM.add(spv);
            symmParAPidxM.add(se.getSrcAPID());
            toRemoveFromM.add(svid);
        }
        for (Integer svid : toRemoveFromM)
        {
            male.removeBranchStartingAt(svid);
        }
        // Include also the chosen vertex (and AP), but do NOT remove it
        symParVertM.add(male.getParent(mvid));        
        symmParAPidxM.add(apidxMP);
        if (debug)
        {
            System.out.println("DEBUG: MALE After removal of symm:");
            System.out.println("DEBUG: "+male);
        }

        // FEMALE Find all parent verteces and AP indeces where the incoming
        // graph will have to be placed
        ArrayList<DENOPTIMVertex> symParVertF = new ArrayList<DENOPTIMVertex>();
        ArrayList<Integer> symmParAPidxF = new ArrayList<Integer>();
        ArrayList<Integer> toRemoveFromF = new ArrayList<Integer>();
        for (int i=0; i<symVrtIDs_F.size(); i++)
        {
            int svid = symVrtIDs_F.get(i);
            if (svid == fvid)
            {
                // We keep only the branch of the vertex identified for crossover
                continue;
            }
            // Store information on where the symmetric vertex is attached
            DENOPTIMEdge se = female.getEdgeWithParent(svid);
            DENOPTIMVertex spv = female.getParent(svid);
            symParVertF.add(spv);
            symmParAPidxF.add(se.getSrcAPID());
            toRemoveFromF.add(svid);
        }
        for (Integer svid : toRemoveFromF)
        {
            female.removeBranchStartingAt(svid);
        }
        // Include also the chosen vertex (and AP), but do NOT remove it
        symParVertF.add(female.getParent(fvid));        
        symmParAPidxF.add(apidxFP);
        if (debug)
        {
            System.out.println("DEBUG: FEMALE After removal of symm:");
            System.out.println("DEBUG: "+female);
        }

        // record levels: we'll need to set them in the incoming subgraphs
        int lvl_male = mvert.getLevel();
        int lvl_female = fvert.getLevel();

        // extract subgraphs (i.e., branches of graphs that will be exchanged)
        if (debug)
        {
            System.out.println("DBUG: MALE sites for FEMALE subGraph:");
            for (int i=0; i<symParVertM.size(); i++)
            {
                System.out.println("     v:"  + symParVertM.get(i) + 
                                                   " ap:"+symmParAPidxM.get(i));
            }
            System.out.println("DBUG: FEMALE sites for MALE subGraph:");
            for (int i=0; i<symParVertF.size(); i++)
            {
                System.out.println("     v:"  + symParVertF.get(i) + 
                                                   " ap:"+symmParAPidxF.get(i));
            }
            System.out.println("DBUG: MALE before extraction: "+male);
            System.out.println("DBUG: FEMALE before extraction: "+female);
        }
        DENOPTIMGraph subG_M =  male.extractSubgraph(mvid);
        DENOPTIMGraph subG_F =  female.extractSubgraph(fvid);
        if (debug)
        {
            System.out.println("DBUG: subGraph from male: "+subG_M);
            System.out.println("DBUG: MALE after extraction: "+male);
            System.out.println("DBUG: subGraph from female: "+subG_F);
            System.out.println("DBUG: FEMALE after extraction: "+female);
        }

        // set the level of each branch according to the destination graph
        subG_M.updateLevels(lvl_female);
        subG_F.updateLevels(lvl_male);

        // attach the subgraph from M/F onto F/M in all symmetry related APs
        male.appendGraphOnGraph(symParVertM, symmParAPidxM, subG_F,
                subG_F.getVertexAtPosition(0), apidxFC, bndOrder, true);
        female.appendGraphOnGraph(symParVertF, symmParAPidxF, subG_M,
                subG_M.getVertexAtPosition(0), apidxMC, bndOrder, true);

        return true;
    }

//------------------------------------------------------------------------------
    
    /**
     * Decides whether to apply constitutional symmetry or not. Application
     * of this type of symmetry implies that the operation on the graph/molecule
     * is to be performed on all the attachment point/verteces related to each
     * other by "constitutional symmetry". Such type symmetry is defined by
     * {@link DENOPTIM.utils.FragmentUtils.getMatchingAP getMatchingAP} 
     * for a single fragment, and symmetric verices in a graph are found by
     * {@link DENOPTIM.utils.GraphUtils.getSymmetricVertices getSymmetricVertices}.
     * This method takes into account the effect of the symmetric subtitution 
     * probability, and the symmetry-related keywords.
     * @param apClass the attachment point class.
     * @return <code>true</code> if symmetry is to be applied
     */

    protected static boolean applySymmetry(APClass apClass)
    {
        boolean r = false;
        if (FragmentSpace.imposeSymmetryOnAPsOfClass(apClass))
        {
            r = true;
        }
        else
        {
            r = RandomUtils.nextBoolean(GAParameters.getSymmetryProbability());
        }
        return r;
    }
   
//------------------------------------------------------------------------------
    
    /**
     * Tries to do mutate the given graph. The mutation site and type are 
     * chosen randomly according the the possibilities declared by the graph.
     * The graph that owns the vertex will be altered and
     * the original structure and content of the graph will be lost.
     * @param graph the graph to mutate.
     * @return <code>true</code> if the mutation is successful.
     * @throws DENOPTIMException
     */
    
    public static boolean performMutation(DENOPTIMGraph graph)
                                                    throws DENOPTIMException
    {  
        // Get vertexes that can be mutated: they can be part of subgraphs 
        // embedded in templates
        Set<DENOPTIMVertex> mutable = graph.getMutableSites();
        if (mutable.size() == 0)
        {
            String msg = "Graph has no mutable site. Mutation aborted.";
            DENOPTIMLogger.appLogger.info(msg);
            return false;
        }
        
        return performMutation(RandomUtils.randomlyChooseOne(mutable));
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Tries to do mutate the given vertex. We assume the vertex to belong to a 
     * graph, if not no mutation is done. The mutation type is
     * chosen randomly according the the possibilities declared by the vertex.
     * The graph that owns the vertex will be altered and
     * the original structure and content of the graph will be lost.
     * @param vertex the vertex to mutate.
     * @return <code>true</code> if the mutation is successful.
     * @throws DENOPTIMException
     */
    
    public static boolean performMutation(DENOPTIMVertex vertex) 
            throws DENOPTIMException
    {  
        if (vertex.getGraphOwner() == null)
        {
            DENOPTIMLogger.appLogger.info("Vertex has no owner - "
                    + "Mutation aborted");
            return false;
        }
        MutationType mType = RandomUtils.randomlyChooseOne(
                vertex.getMutationTypes());
        return performMutation(vertex, mType);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Mutates the given vertex according to the given mutation type, if
     * possible. Vertex that do not belong to any graph or that do not
     * allow the requested kind of mutation, are not mutated.
     * The graph that owns the vertex ill be altered and
     * the original structure and content of the graph will be lost. 
     * @param vertex the vertex to mutate.
     * @param mType the type of mutation to perform.
     * @return <code>true</code> if the mutation is successful.
     * @throws DENOPTIMException
     */
    
    public static boolean performMutation(DENOPTIMVertex vertex, 
            MutationType mType) throws DENOPTIMException
    {   
        // Deal with nonsensical calls
        if (vertex.getGraphOwner() == null)
        {
            DENOPTIMLogger.appLogger.info("Vertex has no owner - "
                    + "Mutation aborted");
            return false;
        }
        if (!vertex.getMutationTypes().contains(mType))
        {
            DENOPTIMLogger.appLogger.info("Vertex does not allow mutation type "
                    + "'" + mType + "' - Mutation aborted");
            return false;
        }
        
        int graphId = vertex.getGraphOwner().getGraphId();
        
        boolean done = false;
        switch (mType) 
        {
            case CHANGEBRANCH:
                done = substituteFragment(vertex);
                break;
                
            case EXTEND:
                vertex.getGraphOwner().removeCappingGroupsOn(vertex);
                done = extendGraph(vertex.getGraphOwner(), vertex, 
                        false, false);
                break;
                
            case DELETE:
                done = deleteFragment(vertex);
                break;
        }
        
        String msg = "Mutation '" + mType.toString() + "' on vertex " +
        vertex.toString() + " (graph " + graphId+"): ";
        if (done)
            msg = msg + "done";
        else
            msg = msg + "unsuccessful";
        
        DENOPTIMLogger.appLogger.info(msg);
        
        return done;
    }

//------------------------------------------------------------------------------

}
