package denoptimga;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.logging.Level;

import exception.DENOPTIMException;
import io.DenoptimIO;
import logging.DENOPTIMLogger;
import molecule.DENOPTIMGraph;
import utils.ObjectPair;
import molecule.DENOPTIMAttachmentPoint;
import molecule.DENOPTIMEdge;
import molecule.DENOPTIMVertex;
import molecule.SymmetricSet;
import utils.GraphUtils;
import utils.FragmentUtils;
import rings.ClosableChain;
import rings.ChainLink;
import rings.RingClosureParameters;
import fragspace.IdFragmentAndAP;
import fragspace.FragmentSpace;
import fragspace.FragmentSpaceParameters;
import org.apache.commons.math3.random.MersenneTwister;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.isomorphism.mcss.RMap;
import utils.RandomUtils;

/**
 *
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */

public class DENOPTIMGraphOperations
{

    // set tu true to debug
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
            int mtype = male.getVertexAtPosition(i).getFragmentType();
            int mvid = male.getVertexAtPosition(i).getVertexId();
            int mfragid = male.getVertexAtPosition(i).getMolId();

            //System.out.println("Male Fragment ID: "+mvid+" type: "+mtype+" ffragid: "+mfragid);

            // if the fragment is a capping group or the scaffold itself ignore
            if (mtype == 0 || mtype == 2)
                continue;

            // get edge toward parent vertex
            // get the edge where the vertex in question is the destination vertex
            // or end vertex. the source vertex then will be the crossover point
            int medgeid = male.getIndexOfEdgeWithParent(mvid);
            DENOPTIMEdge medge = male.getEdgeAtPosition(medgeid);

            for (int j=0; j<female.getVertexCount(); j++)
            {
                int ftype = female.getVertexAtPosition(j).getFragmentType();
                int fvid = female.getVertexAtPosition(j).getVertexId();
                int ffragid = female.getVertexAtPosition(j).getMolId();
                

                //System.out.println("Female Fragment ID: "+fvid+" type: "+ftype+" ffragid: "+ffragid);

                if (ftype == 0 || ftype == 2)
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
        String apClassSrcA = eA.getSourceReaction();
        String apClassTrgA = eA.getTargetReaction();
        String apClassSrcB = eB.getSourceReaction();
        String apClassTrgB = eB.getTargetReaction();
        

        if (isCompatible(apClassSrcA, apClassTrgB))
        {
            if (isCompatible(apClassSrcB, apClassTrgA))
            {
                return true;
            }
        }
        return false;
    }

//------------------------------------------------------------------------------

    /**
     * Check the compatibility between two classes. Note that, due to the non 
     * symmetric nature of the compatibility matrix, the result for
     * isCompatible(A,B) may be different from isCompatible(B,A)
     * @param parentAPclass class of the attachment point (AP) on the parent
     * vertex (inner level)
     * @param childAPclass class of the attachment point (AP) on the child
     * vertex (outer level)
     * @return <code>true</code> if the combination corresponds to a true entry
     * in the compatibility matrix meaning that the two classes, in the 
     * specified order, are compatible
     */

    private static boolean isCompatible(String parentAPclass, 
                                                          String childAPclass)
    {
        ArrayList<String> compatibleClasses = 
                    FragmentSpace.getCompatibilityMatrix().get(parentAPclass);
        return compatibleClasses.contains(childAPclass);
    }

//----------------------------------------------------------------------------

    /**
     * for the vertex in question, disconnect all of its edges, update
     * valences of other vertices, replace the fragment id of the vertex
     * in question and then rejoin bonds. A new fragment may be added at
     * any available attachment point
     * @param molGraph graph of the molecule
     * @param curVertex 
     * @return <code>true</code> if substitution is successful
     * @throws DENOPTIMException
     */

    protected static boolean substituteFragment
                            (DENOPTIMGraph molGraph, DENOPTIMVertex curVertex)
                                                    throws DENOPTIMException
    {
        int vid = curVertex.getVertexId();

        // first get the edge with the parent
        int eidx = molGraph.getIndexOfEdgeWithParent(vid);
        if (eidx == -1)
        {
            String msg = "Program Bug in substituteFragment: " +
                                            "Unable to locate parent edge.";
            DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
            throw new DENOPTIMException(msg);
        }

        // vertex id of the parent
        int pvid = molGraph.getEdgeAtPosition(eidx).getSourceVertex();
        DENOPTIMVertex pvertex = molGraph.getVertexWithId(pvid);

        // Need to remember symmetry because we are deleting the symm. vertices
        boolean symmetry = molGraph.hasSymmetryInvolvingVertex(vid);

        // delete the vertex and its children and all its simmetryc partners
        deleteFragment(molGraph, curVertex);

        // extend the graph at this vertex but without recursion
        return extendGraph(molGraph, pvertex, false, symmetry);
    }

//------------------------------------------------------------------------------

    /**
     * Deletion mutation removes the vertex and also the
     * symmetric partners on its parent.
     * @param molGraph
     * @param curVertex
     * @return <code>true</code> if deletion is successful
     * @throws DENOPTIMException
     */

    protected static boolean deleteFragment
                            (DENOPTIMGraph molGraph, DENOPTIMVertex curVertex)
                                                    throws DENOPTIMException
    {
        int vid = curVertex.getVertexId();

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
                GraphUtils.deleteVertex(molGraph, svid);
            }
        }
        else
        {
            GraphUtils.deleteVertex(molGraph, vid);
        }

        if (molGraph.getVertexWithId(vid) == null && molGraph.getVertexCount() > 1)
            return true;
        return false;
    }

//------------------------------------------------------------------------------

    /**
     * function that will keep extending the graph
     * probability of addition depends on the growth probability
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
            return status;

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

        MersenneTwister mtrand = RandomUtils.getRNG();

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
            double growthProb = EAUtils.getLevelProbability(lvl+1);
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
//TODO copy also DENOPTIMRing into growing graph or use DENOPTIMTemplate
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
                symAPs = curVertex.getPartners(apId);
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
                                        + typ + " AP. Please, report this bug.";
                        DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                        throw new DENOPTIMException(msg);
                    }

                    addedVertices.add(newVrtId);
                    newSymSetOfVertices.add(newVrtId);
                }
            }

            // If any, store symmmetry of new vertices in the graph
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
     * @param chosenFrgAndAp indecex identifying the incoming fragment
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
        ArrayList<DENOPTIMAttachmentPoint> fragAP =
                                         FragmentUtils.getAPForFragment(fid, 1);
        int nvid = GraphUtils.getUniqueVertexIndex();
        DENOPTIMVertex fragVertex = new DENOPTIMVertex(nvid, fid, fragAP, 1);
        // update the level of the vertex based on its parent
        int lvl = curVertex.getLevel();
        fragVertex.setLevel(lvl+1);
        // identify the symmetric APs if any for this fragment vertex
        IAtomContainer mol = FragmentSpace.getFragmentLibrary().get(fid);
        ArrayList<SymmetricSet> simAP = FragmentUtils.getMatchingAP(mol,fragAP);
        fragVertex.setSymmetricAP(simAP);

        // get source: where the new fragment is going to be attached
        ArrayList<DENOPTIMAttachmentPoint> lstDaps =
                                                curVertex.getAttachmentPoints();
        DENOPTIMAttachmentPoint curDap = lstDaps.get(dapIdx);

        // make new edge connecting the current vertex with the new one
        DENOPTIMEdge edge;
        if (!FragmentSpaceParameters.useAPclassBasedApproach())
        {
            // connect a randomly selected AP of this fragment
            // to the current vertex
            edge = GraphUtils.connectVertices(curVertex, fragVertex);
        }
        else
        {
            int fapidx = chosenFrgAndAp.getApId();
            String rcn = curDap.getAPClass(); //on the src
            String cmpReac = fragAP.get(fapidx).getAPClass();

            edge = GraphUtils.connectVertices(curVertex, fragVertex, dapIdx, 
                                                          fapidx, rcn, cmpReac);
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
        
        IdFragmentAndAP res;
        if (!FragmentSpaceParameters.useAPclassBasedApproach())
        {
            int fid = EAUtils.selectRandomFragment();
            res = new IdFragmentAndAP(-1,fid,1,-1,-1,-1);
        }
        else
        {
            res = EAUtils.selectClassBasedFragment(curDap);
        }
        return res;
    }

//------------------------------------------------------------------------------

    protected static boolean  attachFragmentInClosableChain(
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
        //    routine proceede selecting an extension of the chain
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
            int typeNewFrag = newFragIds.get(1);
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

        if (curVertex.getFragmentType() == 0)  
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
                int nfty = -1;
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
                eligibleFrgId.add(nfty);
                eligibleFrgId.add(nfap);
                boolean found = false;
                for (FragForClosabChains ffcc : lstChosenFfCc)
                {
                    int fidA = ffcc.getFragIDs().get(0);
                    int ftyA = ffcc.getFragIDs().get(1);
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
            int prntTyp = parent.getFragmentType();
            int prntAp = edge.getSourceDAP();
            int chidAp = edge.getTargetDAP();
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
                int nfty = -1;
                int nfap = -1;

                int posScaffInCc = cc.getTurningPoint();

//TODO del
if(debug)
    System.out.println("posInCc: "+posInCc+" posScaffInCc: "+posScaffInCc);

                if (posInCc > posScaffInCc)
                {
                    ChainLink parentLink = cc.getLink(posInCc - 1);
                    int pLnkId = parentLink.getMolID();
                    int pLnkTyp = parentLink.getFragType();
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
                    int pLnkTyp = parentLink.getFragType();
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
                eligibleFrgId.add(nfty);
                eligibleFrgId.add(nfap);
                boolean found = false;
                for (FragForClosabChains ffcc : lstChosenFfCc)
                {
                    int fidA = ffcc.getFragIDs().get(0);
                    int ftyA = ffcc.getFragIDs().get(1);
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
        GraphUtils.renumberGraphVertices(male);
        GraphUtils.renumberGraphVertices(female);

        if(debug)
        {
            System.err.println("DBUG: performCrossover " + male.getGraphId() +
                                                " " + female.getGraphId());
            System.err.println("DBUG: MALE(renum): " + male);
            System.err.println("DBUG: FEMALE(renum): " + female);
        }

        // select vertices for crossover
        int mvid, fvid;
        if (FragmentSpaceParameters.useAPclassBasedApproach())
        {
            // remove the capping groups
            GraphUtils.removeCappingGroups(male);
            GraphUtils.removeCappingGroups(female);
            
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
     * @param mvid vertexID of the root vertex of the branch of male to echange
     * @param mfvid vertexID of the root vertex of the branch of female to 
     * echange
     * @return <code>true</code> if a new graph has been successfully produced
     * @throws DENOPTIMException
     */

    public static boolean performCrossover(DENOPTIMGraph male, int mvid,
                        DENOPTIMGraph female, int fvid) throws DENOPTIMException
    {
        if(debug)
        {
            System.err.println("Crossover on vertices " + mvid + " " + fvid);
        }

        // get details about crosover points
        DENOPTIMVertex mvert = male.getVertexWithId(mvid);
        DENOPTIMVertex fvert = female.getVertexWithId(fvid);
        int eidxM = male.getIndexOfEdgeWithParent(mvid);
        int eidxF = female.getIndexOfEdgeWithParent(fvid);
        DENOPTIMEdge eM = male.getEdgeAtPosition(eidxM);
        DENOPTIMEdge eF = female.getEdgeAtPosition(eidxF);
        int apidxMP = eM.getSourceDAP(); // ap index of the male parent
        int apidxMC = eM.getTargetDAP(); // ap index of the male
        int apidxFC = eF.getTargetDAP(); // ap index of the female
        int apidxFP = eF.getSourceDAP(); // ap index of the female parent
        int bndOrder = eM.getBondType();

        if(debug)
        {
            int fragid_M = mvert.getMolId();
            int fragid_F = fvert.getMolId();
            System.err.println("Male XOVER frag   molID: " + fragid_M);
            System.err.println("Female XOVER frag molID: " + fragid_F);
        }

        // Identify all verteces symmetric to the ones chosen for xover
        // Xover is to be projected on each of these
	// TODO: evaluate whether to use the symmetricSets already in the graph
	// instead of re-determinimg the symmetry from scratch
        ArrayList<Integer> symVrtIDs_M = 
                        GraphUtils.getSymmetricVertices(male, mvert,
                             FragmentSpaceParameters.useAPclassBasedApproach());
        ArrayList<Integer> symVrtIDs_F =
                        GraphUtils.getSymmetricVertices(female, fvert,
                            FragmentSpaceParameters.useAPclassBasedApproach());

        // MALE: Find all parent verteces and AP indeces where the incoming 
        // graph will have to be placed
        ArrayList<DENOPTIMVertex> symParVertM = new ArrayList<DENOPTIMVertex>();
        ArrayList<Integer> symmParAPidxM = new ArrayList<Integer>();
        for (int i=0; i<symVrtIDs_M.size(); i++)
        {
            int svid = symVrtIDs_M.get(i);
            // Store information on where the symmetric vertex is attached
            DENOPTIMEdge se = male.getEdgeWithParent(svid);
            DENOPTIMVertex spv = male.getParent(svid);
            symParVertM.add(spv);
            symmParAPidxM.add(se.getSourceDAP());
            //Delete the symmetric vertex 
            GraphUtils.deleteVertex(male, svid);
        }
        // Include also the chosen vertex (and AP), but do NOT remove it
        symParVertM.add(male.getParent(mvid));        
        symmParAPidxM.add(apidxMP);

        // FEMALE Find all parent verteces and AP indeces where the incoming
        // graph will have to be placed
        ArrayList<DENOPTIMVertex> symParVertF = new ArrayList<DENOPTIMVertex>();
        ArrayList<Integer> symmParAPidxF = new ArrayList<Integer>();
        for (int i=0; i<symVrtIDs_F.size(); i++)
        {
            int svid = symVrtIDs_F.get(i);
            // Store information on where the symmetric vertex is attached
            DENOPTIMEdge se = female.getEdgeWithParent(svid);
            DENOPTIMVertex spv = female.getParent(svid);
            symParVertF.add(spv);
            symmParAPidxF.add(se.getSourceDAP());
            //Delete the symmetric vertex
            GraphUtils.deleteVertex(female, svid);
        }
        // Include also the chosen vertex (and AP), but do NOT remove it
        symParVertF.add(female.getParent(fvid));        
        symmParAPidxF.add(apidxFP);

        // record levels: we'll need to set them in the incoming subgraphs
        int lvl_male = male.getVertexWithId(mvid).getLevel();
        int lvl_female = female.getVertexWithId(fvid).getLevel();

        // extract subgraphs (i.e., branched of graphs that will be exchanged)
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
        DENOPTIMGraph subG_M =  GraphUtils.extractSubgraph(male,mvid);
        DENOPTIMGraph subG_F =  GraphUtils.extractSubgraph(female,fvid);
        if (debug)
        {
            System.out.println("DBUG: subGraph from male: "+subG_M);
            System.out.println("DBUG: MALE after extraction: "+male);
            System.out.println("DBUG: subGraph from female: "+subG_F);
            System.out.println("DBUG: FEMALE after extraction: "+female);
        }

        // set the level of each branch according to the destination graph
        GraphUtils.updateLevels(subG_M.getVertexList(), lvl_female);
        GraphUtils.updateLevels(subG_F.getVertexList(), lvl_male);

        // attach the subgraph from M/F onto F/M in all symmetry related APs
        GraphUtils.appendGraphOnGraph(male, symParVertM, symmParAPidxM, 
                subG_F, subG_F.getVertexAtPosition(0), apidxFC, bndOrder, true);
        GraphUtils.appendGraphOnGraph(female, symParVertF, symmParAPidxF, 
                subG_M, subG_M.getVertexAtPosition(0), apidxMC, bndOrder, true);

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

    protected static boolean applySymmetry(String apClass)
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

}
