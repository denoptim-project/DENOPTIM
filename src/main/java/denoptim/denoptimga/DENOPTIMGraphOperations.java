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

package denoptim.denoptimga;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import org.paukov.combinatorics3.Generator;

import com.google.common.collect.Sets;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.APMapFinder;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.fragspace.GraphLinkFinder;
import denoptim.fragspace.IdFragmentAndAP;
import denoptim.graph.APClass;
import denoptim.graph.APMapping;
import denoptim.graph.Candidate;
import denoptim.graph.DENOPTIMAttachmentPoint;
import denoptim.graph.DENOPTIMEdge;
import denoptim.graph.DENOPTIMGraph;
import denoptim.graph.DENOPTIMTemplate;
import denoptim.graph.DENOPTIMTemplate.ContractLevel;
import denoptim.graph.DENOPTIMVertex;
import denoptim.graph.DENOPTIMVertex.BBType;
import denoptim.graph.SymmetricSet;
import denoptim.io.DenoptimIO;
import denoptim.logging.CounterID;
import denoptim.logging.DENOPTIMLogger;
import denoptim.logging.Monitor;
import denoptim.rings.ChainLink;
import denoptim.rings.ClosableChain;
import denoptim.rings.PathSubGraph;
import denoptim.rings.RingClosureParameters;
import denoptim.utils.CrossoverType;
import denoptim.utils.GraphUtils;
import denoptim.utils.MutationType;
import denoptim.utils.RandomUtils;

/**
 * Collection of operators meant to alter graphs and associated utilities.
 */

public class DENOPTIMGraphOperations
{
    
//------------------------------------------------------------------------------

    /**
     * Identify crossover sites, i.e., subgraphs that can be swapped between 
     * two graphs (i.e., the parents). This method will try to locate as many as 
     * possible crossover sites without falling into combinatorial explosion.
     * @param gA one of the parent graphs.
     * @param gB the other of the parent graphs.
     * @return the list of pairs of crossover sites.
     * @throws DENOPTIMException 
     */
    protected static List<XoverSite> locateCompatibleXOverPoints(
            DENOPTIMGraph graphA, DENOPTIMGraph graphB) 
                    throws DENOPTIMException
    {
        // First, we identify all the edges that allow crossover, and collect
        // their target vertexes (i.e., all the potential seed vertexes of 
        // subgraphs that crossover could swap.
        List<DENOPTIMVertex[]> compatibleVrtxPairs = new ArrayList<DENOPTIMVertex[]>();
        for (DENOPTIMEdge eA : graphA.getEdgeList())
        {
            DENOPTIMVertex vA = eA.getTrgAP().getOwner();
            // We don't do genetic operations on capping vertexes
            if (vA.getBuildingBlockType() == BBType.CAP)
                continue;
            
            for (DENOPTIMEdge eB : graphB.getEdgeList())
            {
                DENOPTIMVertex vB = eB.getTrgAP().getOwner();
                // We don't do genetic operations on capping vertexes
                if (vB.getBuildingBlockType() == BBType.CAP)
                    continue;
                
                //Check condition for considering this combination
                if (isCrossoverPossible(eA, eB))
                {
                    DENOPTIMVertex[] pair = new DENOPTIMVertex[]{vA,vB};
                    compatibleVrtxPairs.add(pair);
                }
            }
        }
        
        //TODO-gg: consider including also the first vertex if not a scaffold
        
        // The crossover sites are the combination of the above sites that
        // define subgraphs that respect the requirements for being swapped 
        // between the two graphs.
        ArrayList<XoverSite> sites = new ArrayList<XoverSite>();
        for (DENOPTIMVertex[] pair : compatibleVrtxPairs)
        {
            DENOPTIMVertex vA = pair[0];
            DENOPTIMVertex vB = pair[1];
            DENOPTIMGraph gA = vA.getGraphOwner();
            DENOPTIMGraph gB = vB.getGraphOwner();
            
            List<DENOPTIMVertex> descendantsA = new ArrayList<DENOPTIMVertex>();
            gA.getChildrenTree(vA, descendantsA);
            List<DENOPTIMVertex> descendantsB = new ArrayList<DENOPTIMVertex>();
            gB.getChildrenTree(vB, descendantsB);
            
            // Branches that are isomorfic are not considered for crossover
            DENOPTIMGraph test1 = gA.clone();
            DENOPTIMGraph test2 = gB.clone();
            try
            {
                DENOPTIMGraph subGraph1 = test1.extractSubgraph(gA.indexOf(vA));
                DENOPTIMGraph subGraph2 = test2.extractSubgraph(gB.indexOf(vB));
                if (!subGraph1.isIsomorphicTo(subGraph2))
                {
                  //TODO-gg: any criterion to respect (template frozen?)
                    List<DENOPTIMVertex> branchOnVA = new ArrayList<DENOPTIMVertex>();
                    branchOnVA.add(vA);
                    branchOnVA.addAll(descendantsA);
                    List<DENOPTIMVertex> branchOnVB = new ArrayList<DENOPTIMVertex>();
                    branchOnVB.add(vB);
                    branchOnVB.addAll(descendantsB);
                    
                    checkAndAddXoverSites(branchOnVA, branchOnVB, 
                            CrossoverType.BRANCH, sites);
                }
            } catch (DENOPTIMException e)
            {
                //We should never end up here.
                e.printStackTrace();
            }
            
            // To identify subgraphs smaller than the full branch we need to find
            // where such subgraphs end, i.e., the vertexes at the end of
            // such subgraphs (included in them), a.k.a. the subgraph ends.
            // Since these subgraph ends will need to allow connection with the
            // rest of the original graph, they are indeed crossover-compatible
            // sites, i.e., they are the parents of the vertexes collected in 
            // compatibleVrtxPairs. Yet, we can combine them
            // * in any number from 1 to all of them (outer loop)
            // * in any combination of the chosen number of them (inner loop)
            // Also, note that the ends need not to cover all the branches. So,
            // some combinations will have to cut some branches short while
            // taking some other branches completely till their last leaf.
            
            // To limit the number of combination, we first get rid of endpoint
            // candidates that cannot be used
            List<DENOPTIMVertex[]> combinablePairs = new ArrayList<DENOPTIMVertex[]>();
            for (DENOPTIMVertex[] otherPair : compatibleVrtxPairs)
            {
                // Exclude vertexes that are not downstream to the seed of the subgraph
                DENOPTIMVertex endOnA = otherPair[0];
                DENOPTIMVertex endOnB = otherPair[1];
                if (!descendantsA.contains(endOnA)
                        || !descendantsB.contains(endOnB))
                    continue;
                
                // This is only the path between the seed and one of the 
                // possibly many subgraph endpoints
                PathSubGraph pathA = new PathSubGraph(vA, endOnA, gA);
                PathSubGraph pathB = new PathSubGraph(vB, endOnB, gB);
                
                // If any partner is a fixed-structure templates...
                if ((gA.getTemplateJacket()!=null 
                        && gA.getTemplateJacket().getContractLevel()
                        == ContractLevel.FIXED_STRUCT)
                        || (gB.getTemplateJacket()!=null 
                                && gB.getTemplateJacket().getContractLevel()
                                == ContractLevel.FIXED_STRUCT))
                {
                    //...the two paths must have same length.
                    if (pathA.getPathLength()!=pathB.getPathLength())
                        continue;
                }
                if (!combinablePairs.contains(otherPair))
                    combinablePairs.add(otherPair);
            }
            
            // NB: use this combinatorial generator because it retains the 
            // sequence of generated subsets (important for reproducibility)
            Generator.subset(combinablePairs)
                .simple()
                .stream()
                .limit(500) // Prevent explosion!
                .forEach(c -> processCombinationOfEndPoints(pair,c,sites));
        }
        
        // NB: we consider only templates that are at the same level of embedding
        for (DENOPTIMVertex vA : graphA.getVertexList())
        {
            if (!(vA instanceof DENOPTIMTemplate))
                continue;
            DENOPTIMTemplate tA = (DENOPTIMTemplate) vA;
            
            if (tA.getContractLevel() == ContractLevel.FIXED)
                continue;
            
            for (DENOPTIMVertex vB : graphB.getVertexList())
            {
                if (!(vB instanceof DENOPTIMTemplate))
                    continue;
                DENOPTIMTemplate tB = (DENOPTIMTemplate) vB;
                
                if (tB.getContractLevel() == ContractLevel.FIXED)
                    continue;
                
                for (XoverSite xos : locateCompatibleXOverPoints(
                        tA.getInnerGraph(), tB.getInnerGraph()))
                {
                    if (!sites.contains(xos))
                        sites.add(xos);
                }
            }
        }
        
        return sites;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Given a pair of seed vertexes that define where a pair of subgraphs stars 
     * in the corresponding original graphs, this method evaluates the use of
     * a given list of candidate subgraph end points. When it finds a pair of 
     * subgraphs that can be used to do crossover it stores them as a
     * {@link XoverSite} in the given collector.
     * @param pair the seed vertexes in each of the graph
     * @param cominationOfEnds the combination of subgraph end points to be 
     * evaluated. The order of the entries does not matter as we will consider
     * the permutations of this list.
     * @param collector this is where the crossover sites are stored.
     */
    private static void processCombinationOfEndPoints(DENOPTIMVertex[] pair,
            List<DENOPTIMVertex[]> cominationOfEnds,
            List<XoverSite> collector)
    {
        // Empty set corresponds to using the entire branch and subgraph and
        // has been already dealt with at this point
        if (cominationOfEnds.size()==0)
            return;
        
        Generator.permutation(cominationOfEnds)
            .simple()
            .stream()
            .limit(500) // Prevent explosion!
            .forEach(c -> processPermutationOfEndPoints(pair, c, collector));
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Given a pair of seed vertexes that define where a pair of subgraphs stars 
     * in the corresponding original graphs, this method evaluates the use of
     * a given <b>ordered</b> list of candidate subgraph end points. 
     * When it finds a pair of 
     * subgraphs that can be used to do crossover it stores them as a
     * {@link XoverSite} in the given collector.
     * @param pair the seed vertexes in each of the graph
     * @param chosenSequenceOfEndpoints the specific permutation of subgraph end
     * points to be evaluated.
     * @param collector this is where the crossover sites are stored.
     */
    private static void processPermutationOfEndPoints(DENOPTIMVertex[] pair,
            List<DENOPTIMVertex[]> chosenSequenceOfEndpoints,
            List<XoverSite> collector)
    {
        DENOPTIMVertex vA = pair[0];
        DENOPTIMVertex vB = pair[1];
        DENOPTIMGraph gA = vA.getGraphOwner();
        DENOPTIMGraph gB = vB.getGraphOwner();
        
        // Exclude overlapping combinations
        boolean exclude = false;
        for (DENOPTIMVertex[] pairA : chosenSequenceOfEndpoints)
        {
            for (DENOPTIMVertex[] pairB : chosenSequenceOfEndpoints)
            {
                if (pairA==pairB)
                    continue;
                
                if (pairA[0]==pairB[0] || pairA[1]==pairB[1])
                {
                    exclude = true;
                    break;
                }
            }
            if (exclude)
                break;
        }
        if (exclude)
            return;
        
        List<DENOPTIMVertex> subGraphEndInA = new ArrayList<DENOPTIMVertex>();
        List<DENOPTIMVertex> subGraphEndInB = new ArrayList<DENOPTIMVertex>();
        List<DENOPTIMVertex> alreadyIncludedFromA = new ArrayList<DENOPTIMVertex>();
        List<DENOPTIMVertex> alreadyIncludedFromB = new ArrayList<DENOPTIMVertex>();
        for (DENOPTIMVertex[] otherPair : chosenSequenceOfEndpoints)
        {
            DENOPTIMVertex endOnA = otherPair[0];
            DENOPTIMVertex endOnB = otherPair[1];
            
            // Ignore vertexes that are already part of the subgraph
            if (alreadyIncludedFromA.contains(endOnA)
                    || alreadyIncludedFromB.contains(endOnB))
                continue;
            
            PathSubGraph pathA = new PathSubGraph(vA, 
                    endOnA.getParent(), gA);
            PathSubGraph pathB = new PathSubGraph(vB, 
                    endOnB.getParent(), gB);
            subGraphEndInA.add(endOnA.getParent());
            subGraphEndInB.add(endOnB.getParent());
            alreadyIncludedFromA.addAll(pathA.getVertecesPath());
            alreadyIncludedFromB.addAll(pathB.getVertecesPath());
        }
        ArrayList<DENOPTIMVertex> subGraphA = new ArrayList<DENOPTIMVertex>();
        subGraphA.add(vA);
        if (!subGraphEndInA.contains(vA))
            gA.getChildTreeLimited(vA, subGraphA, subGraphEndInA, true);

        ArrayList<DENOPTIMVertex> subGraphB = new ArrayList<DENOPTIMVertex>();
        subGraphB.add(vB);
        if (!subGraphEndInB.contains(vB))
            gB.getChildTreeLimited(vB, subGraphB, subGraphEndInB, true);
        
        // The two subgraphs must not be isomorfic to prevent unproductive crossover
        DENOPTIMGraph subGraphCloneA = gA.extractSubgraph(subGraphA);
        DENOPTIMGraph subGraphCloneB = gB.extractSubgraph(subGraphB);
        if (subGraphCloneA.isIsomorphicTo(subGraphCloneB))
            return;
        
        //TODO-gg for fixed.structure templates the two subgraphs
        // must have same structure.
        
        checkAndAddXoverSites(subGraphA, subGraphB, 
                CrossoverType.SUBGRAPH, collector);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Here we check that the given subgraphs have a mapping that allows to swap
     * them, and we create the corresponding crossover site in the collector of 
     * such sites.
     * NB: This method assumes that no crossover can involve seed of the spanning
     * tree (whether scaffold, of anything else.
     */
    private static void checkAndAddXoverSites(List<DENOPTIMVertex> subGraphA, 
            List<DENOPTIMVertex> subGraphB, CrossoverType xoverType,
            List<XoverSite> collector)
    {
        DENOPTIMGraph gOwnerA = subGraphA.get(0).getGraphOwner();
        DENOPTIMGraph gOwnerB = subGraphB.get(0).getGraphOwner();
        
        // What APs need to find a corresponding AP in the other 
        // subgraph in order to allow swapping?
        List<DENOPTIMAttachmentPoint> needyAPsA = gOwnerA.getInterfaceAPs(
                subGraphA);
        List<DENOPTIMAttachmentPoint> allAPsA = gOwnerA.getSubgraphAPs(
                subGraphA);
        List<DENOPTIMAttachmentPoint> needyAPsB = gOwnerB.getInterfaceAPs(
                subGraphB);
        List<DENOPTIMAttachmentPoint> allAPsB = gOwnerB.getSubgraphAPs(
                subGraphB);
        if (allAPsA.size() < needyAPsB.size()
                || allAPsB.size() < needyAPsA.size())
        {
            // Impossible to satisfy needy APs. Crossover site is not usable.
            return;
        }
        
        // Shortcut: since the compatibility of one AP that is needed to do 
        // branch swapping is guaranteed by the identification of the seeds of 
        // swappable subgraphs, we can avoid searching for a valid A mapping 
        // when the graphs are not embedded. This because any AP that is not
        // the one connecting the subgraph to the parent can be left free or
        // removed without side effects on templates that embed the graph 
        // because there is no such template.
        if (xoverType == CrossoverType.BRANCH 
                && gOwnerA.getTemplateJacket()==null
                && gOwnerB.getTemplateJacket()==null)
        {
            XoverSite xos = new XoverSite(subGraphA, needyAPsA, subGraphB, 
                    needyAPsB, xoverType);
            if (!collector.contains(xos))
                collector.add(xos);
            return;
        }
        
        //TODO-gg mapping must ensure all needy APs are satisfied!!!!
        
        // Retain connection to parent to keep directionality of spanning tree!
        
        // NB: this assumes that no crossover can involve seed of the spanning
        // tree (whether scaffold, of anything else)
        // TODO: we could get rid of the assumption that the first in the list
        // is the deepest among the vertexes.
        DENOPTIMVertex seedOnA = subGraphA.get(0);
        DENOPTIMVertex seedOnB = subGraphB.get(0);
        
        APMapping fixedRootAPs = new APMapping();
        fixedRootAPs.put(seedOnA.getEdgeToParent().getTrgAP(),
                seedOnB.getEdgeToParent().getTrgAP());
            
        APMapFinder apmf = new APMapFinder(allAPsA, needyAPsA, allAPsB, needyAPsB,
                fixedRootAPs, 
                false,  // false: we stop at the first good mapping
                true,   // true: only complete mapping
                false); // false: free APs are not compatible by default
        if (apmf.foundMapping())
        {
            XoverSite xos = new XoverSite(subGraphA, needyAPsA, 
                    subGraphB, needyAPsB, xoverType);
            if (!collector.contains(xos))
                collector.add(xos);
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
     * Removes a vertex while merging as many of the child branches into the
     * parent vertex.
     * @param vertex the vertex to remove.
     * @param mnt the monitor keeping track of what happens in EA operations.
     * This does not affect the mutation. It only measures how many attempts and
     * failures occur.
     * @return <code>true</code> if the mutation is successful.
     * @throws DENOPTIMException
     */

    protected static boolean deleteLink(DENOPTIMVertex vertex,
            int chosenVrtxIdx, Monitor mnt) throws DENOPTIMException
    {
        DENOPTIMGraph graph = vertex.getGraphOwner();
        boolean done = graph.removeVertexAndWeld(vertex);
        if (!done)
        {
            mnt.increase(CounterID.FAILEDMUTATTEMTS_PERFORM_NODELLINK_EDIT);
        }
        return done;
    }
    
//------------------------------------------------------------------------------

    /**
     * Substitutes a vertex while keeping its surrounding.
     * Does not replace with same vertex (i.e., new vertex will have different 
     * building block ID, but might look the same wrt content and properties).
     * @param vertex the vertex to replace with another one.
     * @param chosenVrtxIdx if greater than or equals to zero, 
     * sets the choice of the incoming vertex to the 
     * given index. AP mapping between old and new vertex is not controllable
     * in this method.
     * @param mnt the monitor keeping track of what happens in EA operations.
     * This does not affect the mutation. It only measures how many attempts and
     * failures occur.
     * @return <code>true</code> if the mutation is successful.
     * @throws DENOPTIMException
     */

    protected static boolean substituteLink(DENOPTIMVertex vertex,
            int chosenVrtxIdx, Monitor mnt) throws DENOPTIMException
    {
        //TODO: for reproducibility, the AP mapping should become an optional
        // parameter: if given we try to use it, if not given, GraphLinkFinder
        // will try to find a suitable mapping.
        
        GraphLinkFinder glf = null;
        if (chosenVrtxIdx<0)
        {
            glf = new GraphLinkFinder(vertex);
        } else {
            glf = new GraphLinkFinder(vertex,chosenVrtxIdx);
        }
        if (!glf.foundAlternativeLink())
        {
            mnt.increase(CounterID.FAILEDMUTATTEMTS_PERFORM_NOCHANGELINK_FIND);
            return false;
        }

        DENOPTIMGraph graph = vertex.getGraphOwner();
        boolean done = graph.replaceVertex(vertex,
                glf.getChosenAlternativeLink().getBuildingBlockId(),
                glf.getChosenAlternativeLink().getBuildingBlockType(),
                glf.getChosenAPMapping().toIntMappig());
        if (!done)
        {
            mnt.increase(
                    CounterID.FAILEDMUTATTEMTS_PERFORM_NOCHANGELINK_EDIT);
        }
        return done;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Inserts a vertex in between two connected vertexes that are identified by
     * the one vertex holding the "source" end of the edge, and the identifier
     * of the attachment point used by that edge. Effectively, this method
     * asks to replace an edge with two edges with a new vertex in between.
     * @param vertex holding the "source" end of the edge to
     * @param chosenAPId the identifier of the AP on the vertex.
     * @param mnt the monitor keeping track of what happens in EA operations.
     * This does not affect the mutation. It only measures how many attempts and
     * failures occur.
     * @return <code>true</code> if the mutation is successful.
     * @throws DENOPTIMException if the request cannot be performed because the
     * vertex is not the source of an edge at the given AP, or because such AP 
     * does not exist on the given vertex.
     */
    public static boolean extendLink(DENOPTIMVertex vertex,
            int chosenAPId, Monitor mnt) throws DENOPTIMException
    {
        return extendLink(vertex, chosenAPId, -1 , mnt);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Inserts a vertex in between two connected vertexes that are identified by
     * the one vertex holding the "source" end of the edge, and the identifier
     * of the attachment point used by that edge. Effectively, this method
     * asks to replace an edge with two edges with a new vertex in between.
     * @param vertex holding the "source" end of the edge to
     * @param chosenAPId the identifier of the AP on the vertex.
     * @param chosenNewVrtxId the identifier of the new building block to 
     * insert in the list.
     * @param mnt the monitor keeping track of what happens in EA operations.
     * This does not affect the mutation. It only measures how many attempts and
     * failures occur.
     * @return <code>true</code> if the mutation is successful.
     * @throws DENOPTIMException if the request cannot be performed because the
     * vertex is not the source of an edge at the given AP, or because such AP 
     * does not exist on the given vertex.
     */
    public static boolean extendLink(DENOPTIMVertex vertex,
            int chosenAPId, int chosenNewVrtxId, Monitor mnt) 
                    throws DENOPTIMException
    {
        DENOPTIMAttachmentPoint ap = vertex.getAP(chosenAPId);
        if (ap == null)
        {
            throw new DENOPTIMException("No AP "+chosenAPId+" in vertex "
                    +vertex+".");
        }
        DENOPTIMEdge e = ap.getEdgeUserThroughout();
        
        if (e == null)
        {
            throw new DENOPTIMException("AP "+chosenAPId+" in vertex "
                    +vertex+" has no edge user.");
        }
        if (e.getSrcAP().getOwner() != vertex)
        {
            throw new DENOPTIMException("Request to extend a link from a child "
                    + "vertex (AP "+chosenAPId+" of vertex "+vertex+").");
        }
        return extendLink(e, chosenNewVrtxId, mnt);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Replace an edge with two edges with a new vertex in between, thus 
     * inserting a vertex in between two directly connected vertexes, which will
     * no longer be directly connected.
     * @param vertex holding the "source" end of the edge to
     * @param chosenBBIdx the identifier of the building block to insert.
     * @param mnt the monitor keeping track of what happens in EA operations.
     * This does not affect the mutation. It only measures how many attempts and
     * failures occur.
     * @return <code>true</code> if the mutation is successful.
     * @throws DENOPTIMException
     */
    public static boolean extendLink(DENOPTIMEdge edge, int chosenBBIdx,
            Monitor mnt) throws DENOPTIMException
    {
        //TODO: for reproducibility, the AP mapping should become an optional
        // parameter: if given we try to use it, if not given we GraphLinkFinder
        // will try to find a suitable mapping.
        
        GraphLinkFinder glf = null;
        if (chosenBBIdx < 0)
        {
            glf = new GraphLinkFinder(edge);
        } else {
            glf = new GraphLinkFinder(edge,chosenBBIdx);
        }
        if (!glf.foundAlternativeLink())
        {
            mnt.increase(CounterID.FAILEDMUTATTEMTS_PERFORM_NOADDLINK_FIND);
            return false;
        }
        
        // Need to convert the mapping to make it independent from the instance
        // or the new link.
        LinkedHashMap<DENOPTIMAttachmentPoint,Integer> apMap = 
                new LinkedHashMap<DENOPTIMAttachmentPoint,Integer>();
        for (Entry<DENOPTIMAttachmentPoint, DENOPTIMAttachmentPoint> e : 
            glf.getChosenAPMapping().entrySet())
        {
            apMap.put(e.getKey(), e.getValue().getIndexInOwner());
        }
        
        DENOPTIMGraph graph = edge.getSrcAP().getOwner().getGraphOwner();
        boolean done = graph.insertVertex(edge,
                glf.getChosenAlternativeLink().getBuildingBlockId(),
                glf.getChosenAlternativeLink().getBuildingBlockType(),
                apMap);
        if (!done)
        {
            mnt.increase(
                    CounterID.FAILEDMUTATTEMTS_PERFORM_NOADDLINK_EDIT);
        }
        return done;
    }

//------------------------------------------------------------------------------

    /**
     * Substitutes a vertex and any child branch. 
     * Deletes the given vertex from the graph that owns
     * it, and removes any child vertex (i.e., reachable from the given vertex
     * by a directed path). Then it tries to extent the graph from the
     * parent vertex (i.e., the one that was originally holding the given 
     * vertex). Moreover, additional extension may occur on
     * any available attachment point of the parent vertex.
     * @param vertex to mutate (the given vertex).
     * @param force set to <code>true</code> to ignore growth probability.
     * @param chosenVrtxIdx if greater than or equals to zero, 
     * sets the choice of the vertex to the 
     * given index.
     * @param chosenApId if greater than or equals to zero, 
     * sets the choice of the AP to the 
     * given index.
     * @return <code>true</code> if substitution is successful
     * @throws DENOPTIMException
     */

    protected static boolean rebuildBranch(DENOPTIMVertex vertex,
            boolean force, int chosenVrtxIdx, int chosenApId)
                                                    throws DENOPTIMException
    {
        DENOPTIMGraph g = vertex.getGraphOwner();

        // first get the edge with the parent
        DENOPTIMEdge e = vertex.getEdgeToParent();
        if (e == null)
        {
            String msg = "Program Bug in substituteFragment: Unable to locate "
                    + "parent edge for vertex "+vertex+" in graph "+g;
            DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
            throw new DENOPTIMException(msg);
        }

        // vertex id of the parent
        int pvid = e.getSrcVertex();
        DENOPTIMVertex parentVrt = g.getVertexWithId(pvid);

        // Need to remember symmetry because we are deleting the symm. vertices
        boolean symmetry = g.hasSymmetryInvolvingVertex(vertex);
        
        // delete the vertex and its children and all its symmetric partners
        deleteFragment(vertex);

        // extend the graph at this vertex but without recursion
        return extendGraph(parentVrt,false,symmetry,force,chosenVrtxIdx,
                chosenApId);
    }

//------------------------------------------------------------------------------

    /**
     * Deletion mutation removes the vertex and also the
     * symmetric partners on its parent.
     * @param vertex
     * @return <code>true</code> if deletion is successful
     * @throws DENOPTIMException
     */
    
    protected static boolean deleteFragment(DENOPTIMVertex vertex)
                                                    throws DENOPTIMException
    {
        int vid = vertex.getVertexId();
        DENOPTIMGraph molGraph = vertex.getGraphOwner();

        if (molGraph.hasSymmetryInvolvingVertex(vertex))
        {
            ArrayList<Integer> toRemove = new ArrayList<Integer>();
            for (int i=0; i<molGraph.getSymSetForVertexID(vid).size(); i++)
            {
                int svid = molGraph.getSymSetForVertexID(vid).getList().get(i); 
                toRemove.add(svid);
            }
            for (Integer svid : toRemove)
            {
                DENOPTIMVertex v = molGraph.getVertexWithId(svid);
                if (v == null)
                    continue;
                molGraph.removeBranchStartingAt(v);
            }
        }
        else
        {
            molGraph.removeBranchStartingAt(vertex);
        }

        if (molGraph.getVertexWithId(vid) == null && molGraph.getVertexCount() > 1)
            return true;
        
        return false;
    }
    
//------------------------------------------------------------------------------

    /**
     * Deletes the given vertex and all other vertexes that are not connected to
     * more than 2 non-capping group vertexes. Effectively, it removes an entire
     * branch (in acyclic graphs) or the part of a cyclic graph between two 
     * branching points (in a ring, thus opening the ring wide). The same is 
     * done on symmetric partners on its parent.
     * @param vertex the vertex at which the deletion begins.
     * @param mnt monitoring tool to keep count of events.
     * @return <code>true</code> if deletion is successful
     * @throws DENOPTIMException
     */
    
    protected static boolean deleteChain(DENOPTIMVertex vertex, Monitor mnt)
                                                    throws DENOPTIMException
    {
        int vid = vertex.getVertexId();
        DENOPTIMGraph molGraph = vertex.getGraphOwner();

        if (molGraph.hasSymmetryInvolvingVertex(vertex))
        {
            ArrayList<Integer> toRemove = new ArrayList<Integer>();
            for (int i=0; i<molGraph.getSymSetForVertexID(vid).size(); i++)
            {
                int svid = molGraph.getSymSetForVertexID(vid).getList().get(i);
                toRemove.add(svid);
            }
            for (Integer svid : toRemove)
            {
                DENOPTIMVertex v = molGraph.getVertexWithId(svid);
                if (v == null || !v.getMutationTypes(new ArrayList<MutationType>())
                        .contains(MutationType.DELETECHAIN))
                    continue;
                molGraph.removeChainUpToBranching(v);
            }
        }
        else
        {
            molGraph.removeChainUpToBranching(vertex);
        }

        if (molGraph.getVertexWithId(vid) == null && molGraph.getVertexCount() > 1)
            return true;
        return false;
    }

//------------------------------------------------------------------------------

    /**
     * function that will keep extending the graph according to the 
     * growth/substitution probability.
     *
     * @param curVertex vertex to which further fragments will be appended
     * @param extend if <code>true</code>, then the graph will be grown further 
     * (recursive mode)
     * @param symmetryOnAps if <code>true</code>, then symmetry will be applied
     * on the APs, no matter what. This is mostly needed to retain symmetry 
     * when performing mutations on the root vertex of a symmetric graph.
     * @throws DENOPTIMException
     * @return <code>true</code> if the graph has been modified
     */
     
    protected static boolean extendGraph(DENOPTIMVertex curVertex, 
                                         boolean extend, 
                                         boolean symmetryOnAps)
                                                        throws DENOPTIMException
    {
        return extendGraph(curVertex,extend,symmetryOnAps,false,-1,-1);
    }
    
//------------------------------------------------------------------------------

    /**
     * function that will keep extending the graph. The
     * probability of addition depends on the growth probability scheme.
     *
     * @param curVrtx vertex to which further fragments will be appended
     * @param extend if <code>true</code>, then the graph will be grown further 
     * (recursive mode)
     * @param symmetryOnAps if <code>true</code>, then symmetry will be applied
     * on the APs, no matter what. This is mostly needed to retain symmetry 
     * when performing mutations on the root vertex of a symmetric graph.
     * @param force set to <code>true</code> to ignore growth probability.
     * @param chosenVrtxIdx if greater than or equals to zero, 
     * sets the choice of the vertex to the 
     * given index. This selection applies only to the first extension, not to
     * further recursions.
     * @param chosenApId if greater than or equals to zero, sets the choice
     *                   of the AP to the given index. This selection applies
     *                   only to the first extension, not to further recursions.
     * @throws DENOPTIMException
     * @return <code>true</code> if the graph has been modified
     */

    protected static boolean extendGraph(DENOPTIMVertex curVrtx, 
                                         boolean extend, 
                                         boolean symmetryOnAps,
                                         boolean force,
                                         int chosenVrtxIdx,
                                         int chosenApId) 
                                                        throws DENOPTIMException
    {  
        // return true if the append has been successful
        boolean status = false;

        // check if the fragment has available APs
        if (!curVrtx.hasFreeAP())
        {
            return status;
        }
        
        int curVrtId = curVrtx.getVertexId();
        DENOPTIMGraph molGraph = curVrtx.getGraphOwner();
        int lvl = molGraph.getLevel(curVrtx);
        int grphId = molGraph.getGraphId();

        ArrayList<Integer> addedVertices = new ArrayList<>();

        ArrayList<DENOPTIMAttachmentPoint> lstDaps = 
                                                curVrtx.getAttachmentPoints();
        List<DENOPTIMAttachmentPoint> toDoAPs = 
                new ArrayList<DENOPTIMAttachmentPoint>();
        toDoAPs.addAll(lstDaps);
        for (int i=0; i<lstDaps.size(); i++)
        {
            // WARNING: randomisation decouples 'i' from the index of the AP
            // in the vertex's list of APs! So 'i' is just the i-th attempt on
            // the curVertex.
            
            DENOPTIMAttachmentPoint ap = 
                    RandomUtils.randomlyChooseOne(toDoAPs);
            toDoAPs.remove(ap);
            int apId = ap.getIndexInOwner();
            
            // is it possible to extend on this AP?
            if (!ap.isAvailable())
            {
                continue;
            }

            // Ring closure does not change the size of the molecule, so we
            // give it an extra chance to occur irrespectively on molecular size
            // limit, while still subject of crowdedness probability.
            boolean allowOnlyRingClosure = false;
            if (!force)
            {
                // Decide whether we want to extend the graph at this AP?
                // Note that depending on the criterion (level/molSize) one
                // of these two first factors is 1.0.
                double molSizeProb = EAUtils.getMolSizeProbability(molGraph);
                double byLevelProb = EAUtils.getGrowthByLevelProbability(lvl);
                double crowdingProb = EAUtils.getCrowdingProbability(ap);
                double extendGraphProb = molSizeProb * byLevelProb * crowdingProb;
                boolean fgrow = RandomUtils.nextBoolean(extendGraphProb);
                if (!fgrow)
                {
                    if (RingClosureParameters.allowRingClosures() 
                            && RandomUtils.nextBoolean(byLevelProb * crowdingProb))
                    {
                        allowOnlyRingClosure = true;
                    } else {
                        continue;
                    }
                }
            }

            // Apply closability bias in selection of next fragment
            if (!allowOnlyRingClosure && RingClosureParameters.allowRingClosures() && 
                      RingClosureParameters.selectFragmentsFromClosableChains())
            {
                boolean successful = attachFragmentInClosableChain(curVrtx,
                        apId, molGraph, addedVertices);
                if (successful)
                {
                    continue;
                }
            }
            
            // find a compatible combination of fragment and AP
            IdFragmentAndAP chosenFrgAndAp = null;
            if (allowOnlyRingClosure)
            {
                chosenFrgAndAp = getRCVForSrcAp(curVrtx,apId);
            } else {
                chosenFrgAndAp = getFrgApForSrcAp(curVrtx, 
                    apId, chosenVrtxIdx, chosenApId);
            }
            int fid = chosenFrgAndAp.getVertexMolId();
            if (fid == -1)
            {
                continue;
            }
            
            // Stop if graph is already too big
            DENOPTIMVertex incomingVertex = 
                    DENOPTIMVertex.newVertexFromLibrary(-1, 
                            chosenFrgAndAp.getVertexMolId(), 
                            BBType.FRAGMENT);
            if ((curVrtx.getGraphOwner().getHeavyAtomsCount() + 
                    incomingVertex.getHeavyAtomsCount()) > 
                        FragmentSpaceParameters.getMaxHeavyAtom())
            {
                continue;
            }

            // Decide on symmetric substitution within this vertex...
            boolean cpOnSymAPs = applySymmetry(ap.getAPClass());
            SymmetricSet symAPs = new SymmetricSet();
            if (curVrtx.hasSymmetricAP() 
                    && (cpOnSymAPs || symmetryOnAps)
                    && !allowOnlyRingClosure)
            {
                symAPs = curVrtx.getSymmetricAPs(apId);
				if (symAPs != null)
				{   
                    // Are symmetric APs rooted on same atom?
                    boolean allOnSameSrc = true;
                    for (Integer symApId : symAPs.getList())
                    {
                        if (!curVrtx.getAttachmentPoints().get(symApId)
                                .hasSameSrcAtom(ap))
                        {
                            allOnSameSrc = false;
                            break;
                        }
                    }
                    
                    if (allOnSameSrc)
                    {
                        // If the APs are rooted on the same src atom, we want to
                        // apply the crowdedness probability to avoid over crowded
                        // atoms
                        
                        int crowdedness = EAUtils.getCrowdedness(ap);
                        
                        SymmetricSet toKeep = new SymmetricSet();
                        
                        
                        // Start by keeping "ap"
                        toKeep.add(apId);
                        crowdedness = crowdedness + 1;
                        
                        // Pick the accepted value once (used to decide how much
                        // crowdedness we accept)
                        double shot = RandomUtils.nextDouble();
                        
                        // Keep as many as allowed by the crowdedness decision
                        for (Integer symApId : symAPs.getList())
                        {
                            if (symApId.compareTo(apId) == 0)
                                continue;
                            
                            double crowdProb = EAUtils.getCrowdingProbability(
                                    crowdedness);
                            
                            if (shot > crowdProb)
                                break;
                            
                            toKeep.add(symApId);
                            crowdedness = crowdedness + 1;
                        }
                        
                        // Adjust the list of symmetric APs to work with
                        symAPs = toKeep;
                    }
                }
				else
				{
				    symAPs = new SymmetricSet();
				    symAPs.add(apId);
				}
            } else {
                symAPs.add(apId);
            }

            // ...and inherit symmetry from previous levels
            boolean cpOnSymVrts = molGraph.hasSymmetryInvolvingVertex(curVrtx);
            SymmetricSet symVerts = new SymmetricSet();
            if (cpOnSymVrts)
            {
                symVerts = molGraph.getSymSetForVertexID(curVrtId);
            }
            else
            {
                symVerts.add(curVrtId);
            }
            
            // Consider size after application of symmetry
            if ((curVrtx.getGraphOwner().getHeavyAtomsCount() + 
                    incomingVertex.getHeavyAtomsCount()*symVerts.size()*symAPs.size()) > 
                        FragmentSpaceParameters.getMaxHeavyAtom())
            {
                continue;
            }
            
            GraphUtils.ensureVertexIDConsistency(molGraph.getMaxVertexId());

            // loop on all symmetric vertices, but can be only one.
            SymmetricSet newSymSetOfVertices = new SymmetricSet();
            for (Integer parVrtId : symVerts.getList())
            {
                DENOPTIMVertex parVrt = molGraph.getVertexWithId(parVrtId);
                
                for (int si=0; si<symAPs.size(); si++)
                {
                    int symApId = symAPs.get(si);
                    DENOPTIMAttachmentPoint symAP = parVrt.getAttachmentPoints()
                            .get(symApId);
                    
                    if (!symAP.isAvailable())
                    {
                        continue;
                    }

                    // Finally add the fragment on a symmetric AP
                    int newVrtId = GraphUtils.getUniqueVertexIndex();
                    DENOPTIMVertex fragVertex = 
                            DENOPTIMVertex.newVertexFromLibrary(newVrtId, 
                                    chosenFrgAndAp.getVertexMolId(), 
                                    BBType.FRAGMENT);
                    DENOPTIMAttachmentPoint trgAP = fragVertex.getAP(
                            chosenFrgAndAp.getApId());
                    
                    molGraph.appendVertexOnAP(symAP, trgAP);
                    
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
            // attempt to further extend each of the newly added vertices
            for (int i=0; i<addedVertices.size(); i++)
            {
                int vid = addedVertices.get(i);
                DENOPTIMVertex v = molGraph.getVertexWithId(vid);
                extendGraph(v, extend, symmetryOnAps);
            }
        }

        if (addedVertices.size() > 0)
            status = true;

        return status;
    }
    
//------------------------------------------------------------------------------

    /**
     * Select a compatible fragment for the given attachment point.
     * Compatibility can either be class based or based on the free connections
     * @param curVertex the source graph vertex
     * @param dapidx the attachment point index on the src vertex
     * @return the vector of indexes identifying the molId (fragment index) 
     * of a fragment with a compatible attachment point, and the index of such 
     * attachment point.
     * @throws DENOPTIMException
     */

    protected static IdFragmentAndAP getFrgApForSrcAp(DENOPTIMVertex curVertex, 
                                           int dapidx) throws DENOPTIMException
    {
        return getFrgApForSrcAp(curVertex, dapidx, -1, -1);
    }
    
//------------------------------------------------------------------------------

    /**
     * Select a compatible fragment for the given attachment point.
     * Compatibility can either be class based or based on the free connections
     * @param curVertex the source graph vertex
     * @param dapidx the attachment point index on the src vertex
     * @param chosenVrtxIdx if greater than or equals to zero, 
     * sets the choice of the incoming vertex to the 
     * given index.
     * @param chosenApId if greater than or equals to zero, 
     * sets the choice of the AP (of the incoming vertex) to the 
     * given index.
     * @return the vector of indeces identifying the molId (fragment index) 
     * of a fragment with a compatible attachment point, and the index of such 
     * attachment point.
     * @throws DENOPTIMException
     */

    protected static IdFragmentAndAP getFrgApForSrcAp(DENOPTIMVertex curVertex, 
            int dapidx, int chosenVrtxIdx, int chosenApId) 
                    throws DENOPTIMException
    {
        ArrayList<DENOPTIMAttachmentPoint> lstDaps =
                                              curVertex.getAttachmentPoints();
        DENOPTIMAttachmentPoint curDap = lstDaps.get(dapidx);

        // Initialize with an empty pointer
        IdFragmentAndAP res = new IdFragmentAndAP(-1, -1, BBType.FRAGMENT, -1, 
                -1, -1);
        if (!FragmentSpace.useAPclassBasedApproach())
        {
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
                if (chosenVrtxIdx>-1 && chosenApId>-1)
                {
                    // We have asked to force the selection
                    res = new IdFragmentAndAP(-1,chosenVrtxIdx,BBType.FRAGMENT,
                            chosenApId,-1,-1);
                } else {
                    res = RandomUtils.randomlyChooseOne(candidates);
                }
            }
        }
        return res;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Select a compatible ring-closing vertex for the given attachment point.
     * @param curVertex the source graph vertex
     * @param dapidx the attachment point index on the src vertex
     * @return the vector of indeces identifying the molId (fragment index) 
     * of a fragment with a compatible attachment point, and the index of such 
     * attachment point.
     * @throws DENOPTIMException
     */

    protected static IdFragmentAndAP getRCVForSrcAp(DENOPTIMVertex curVertex, 
            int dapidx) throws DENOPTIMException
    {
        DENOPTIMAttachmentPoint curDap = curVertex.getAP(dapidx);

        // Initialize with an empty pointer
        IdFragmentAndAP res = new IdFragmentAndAP(-1, -1, BBType.FRAGMENT, -1, 
                -1, -1);
        
        ArrayList<DENOPTIMVertex> rcvs = FragmentSpace.getRCVs();
        DENOPTIMVertex chosen = null;
        if (!FragmentSpace.useAPclassBasedApproach())
        {
            chosen = RandomUtils.randomlyChooseOne(rcvs);
            res = new IdFragmentAndAP(-1,chosen.getBuildingBlockId(),
                    chosen.getBuildingBlockType(),0,-1,-1);
        }
        else
        {
            ArrayList<IdFragmentAndAP> candidates = new ArrayList<IdFragmentAndAP>();
            for (DENOPTIMVertex v : rcvs)
            {
                if (curDap.getAPClass().isCPMapCompatibleWith(
                        v.getAP(0).getAPClass()))
                {
                    candidates.add(new IdFragmentAndAP(-1,v.getBuildingBlockId(),
                            v.getBuildingBlockType(),0,-1,-1));
                }
            }
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
            int chosenId = RandomUtils.nextInt(numCands);
            FragForClosabChains chosenFfCc = lscFfCc.get(chosenId);
            ArrayList<Integer> newFragIds = chosenFfCc.getFragIDs();
            int molIdNewFrag = newFragIds.get(0);
            BBType typeNewFrag = BBType.parseInt(newFragIds.get(1));
            int dapNewFrag = newFragIds.get(2);
            if (molIdNewFrag != -1)
            {
                int newvid = GraphUtils.getUniqueVertexIndex();
                DENOPTIMVertex newVrtx = DENOPTIMVertex.newVertexFromLibrary(
                        newvid, molIdNewFrag, typeNewFrag);
                
                molGraph.appendVertexOnAP(curVertex.getAP(dapidx), 
                        newVrtx.getAP(dapNewFrag));
                
                if (newvid != -1)
                {
                    addedVertices.add(newvid);
                    // update list of candidate closable chains
                    molGraph.getClosableChains().removeAll(
                            chosenFfCc.getIncompatibleCC());
                    if (applySymmetry(curVertex.getAttachmentPoints().get(
                            dapidx).getAPClass()))
                    {
//TODO: implement symmetric substitution with closability bias
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
     * Private class representing a selected closable chain of fragments
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
     * closable chains. Only works for scaffold.
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

        if (molGraph.getClosableChains().size() == 0)
        {
            return lstChosenFfCc;            
        }

        if (curVertex.getBuildingBlockType() == BBType.SCAFFOLD)  
        {
            for (ClosableChain cc : molGraph.getClosableChains())
            {
                int posInCc = cc.involvesVertex(curVertex);
                ChainLink cLink = cc.getLink(posInCc);
                int nfid = -1;
                BBType nfty = BBType.UNDEFINED;
                int nfap = -1;

                if (cLink.getApIdToLeft() != dapidx && 
                    cLink.getApIdToRight() != dapidx)
                {
                    // Chain does not involve AP dapidx
                    continue;
                }

                if (cLink.getApIdToRight() == dapidx)
                {
                    if (cc.getSize() > (posInCc+1))
                    {
                        // cLink is NOT the rightmost chain link
                        ChainLink nextChainLink = cc.getLink(posInCc+1);
                        nfid = nextChainLink.getMolID();
                        nfty = nextChainLink.getFragType();
                        nfap = nextChainLink.getApIdToLeft();
                    }
                    else
                    {
                        // cLink is the rightmost chain link
                        // closability bias suggest NO fragment
                    }
                }
                else if (cLink.getApIdToLeft() == dapidx)
                {
                    if ((posInCc-1) >= 0)
                    {
                        // cLink is NOT the leftmost chain link
                        ChainLink nextChainLink = cc.getLink(posInCc-1);
                        nfid = nextChainLink.getMolID();
                        nfty = nextChainLink.getFragType();
                        nfap = nextChainLink.getApIdToRight();
                    }
                    else
                    {
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
            }
        }
        else
        {
            DENOPTIMVertex parent = molGraph.getParent(curVertex);
            DENOPTIMEdge edge = molGraph.getEdgeWithParent(
                    curVertex.getVertexId());
            int prntId = parent.getBuildingBlockId();
            BBType prntTyp = parent.getBuildingBlockType();
            int prntAp = edge.getSrcAPID();
            int chidAp = edge.getTrgAPID();
            for (ClosableChain cc : molGraph.getClosableChains())
            {
                int posInCc = cc.involvesVertexAndAP(curVertex, dapidx, chidAp);

                if (posInCc == -1)
                {
                    // closable chain does not span this combination
                    // of vertex and APs
                    continue;
                }

                ChainLink cLink = cc.getLink(posInCc);
                int nfid = -1;
                BBType nfty = BBType.UNDEFINED;
                int nfap = -1;

                List<Integer> altertnativeDirections = new ArrayList<>();
                altertnativeDirections.add(-1);
                altertnativeDirections.add(+1);
                for (int altDir : altertnativeDirections)
                {
                    ChainLink parentLink = cc.getLink(posInCc + altDir);
                    int pLnkId = parentLink.getMolID();
                    BBType pLnkTyp = parentLink.getFragType();
                    
                    int pLnkAp = -1;
                    int cApId = -1;
                    if (altDir>0)
                    {
                        pLnkAp = parentLink.getApIdToRight();
                        cApId = cLink.getApIdToLeft();
                    } else {
                        pLnkAp = parentLink.getApIdToLeft();
                        cApId = cLink.getApIdToRight();
                    }
                    if (pLnkId==prntId && pLnkTyp==prntTyp && pLnkAp == prntAp  &&
                            cApId == chidAp)
                    {
                        if (cc.getSize() > (posInCc+altDir)
                                && (posInCc+altDir) >= 0)
                        {
                            ChainLink nextChainLink = cc.getLink(posInCc + altDir);
                            nfid = nextChainLink.getMolID();
                            nfty = nextChainLink.getFragType();
                            nfap = nextChainLink.getApIdToLeft();
                        }
                        else
                        {
                            // cLink is the rightmost chain link
                            // closability bias suggests NO fragment
                        }
                    }
                    else
                    {
                        // different parent link
                        continue;
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
            }
        }
        return lstChosenFfCc;
    }
    
//------------------------------------------------------------------------------

    /**
     * Performs the crossover that swaps the two subgraphs defining the given
     * {@link XoverSite}. 
     * The operation is performed on the graphs that own the vertexes referred
     * in the {@link XoverSite}, so the original version of the graphs is lost.
     * We expect the graphs to have an healthy set of vertex IDs. This can 
     * be ensured by running {@link DENOPTIMGraph#renumberGraphVertices()}.
     * @param site the definition of the crossover site.
     * @throws DENOPTIMException
     */
    public static boolean performCrossover(XoverSite site) throws DENOPTIMException
    {          
        DENOPTIMGraph gA = site.getA().get(0).getGraphOwner();
        DENOPTIMGraph gB = site.getB().get(0).getGraphOwner();
        
        // All the APs that point away from the subgraph
        List<DENOPTIMAttachmentPoint> allAPsOnA = gA.getSubgraphAPs(site.getA());
        List<DENOPTIMAttachmentPoint> allAPsOnB = gB.getSubgraphAPs(site.getB());
        
        // The APs that are required to have a mapping for proper crossover,
        // eg. because the change of subgraph needs to retain a super structure.
        List<DENOPTIMAttachmentPoint> needyAPsOnA = site.getAPsNeedingMappingA();
        List<DENOPTIMAttachmentPoint> needyAPsOnB = site.getAPsNeedingMappingB();
        
        // APs that connects the subgraphs' root to the parent vertex
        DENOPTIMAttachmentPoint apToParentA = null;
        DENOPTIMAttachmentPoint apToParentB = null;
        for (DENOPTIMAttachmentPoint ap : needyAPsOnA)
        {
            if (!ap.isSrcInUserThroughout())
            {
                apToParentA = ap;
                //WARNING: we assume there is one AND only one AP to parent!!!
                break;
            }
        }
        for (DENOPTIMAttachmentPoint ap : needyAPsOnB)
        {
            if (!ap.isSrcInUserThroughout())
            {
                apToParentB = ap;
                //WARNING: we assume there is one AND only one AP to parent!!!
                break;
            }
        }
        if (apToParentA==null || apToParentB==null)
        {
            throw new DENOPTIMException("Could not identify any attachment "
                    + "point connecting a subgraph to the rest of the graph. "
                    + "This violates assumption that crossover does not "
                    + "involve scaffold or vertexes without parent.");
        }
        // Check if the subgraphs can be used with reversed edge direction, or
        // bias the AP mapping to use the original source vertexes.
        APMapping fixedRootAPs = null;
        //TODO: REACTIVATE ONCE REVERSION of the subgrsph's spanning tree is in place.
        //if (!subG_M.isReversible() || !subG_F.isReversible())
        if (true)
        {
            // Constrain the AP mapping so that the AP originally used to
            // connect to the parent vertex, will also be used the same way.
            // Thus, forces retention of edge direction within the subgraph.
            fixedRootAPs = new APMapping();
            fixedRootAPs.put(apToParentA, apToParentB);
        }
        
        // Find an AP mapping that satisfies both root-vertex constrain and
        // need to ensure the mapping of needy APs
        APMapFinder apmf = new APMapFinder(allAPsOnA, needyAPsOnA, 
                allAPsOnB, needyAPsOnB, fixedRootAPs, 
                false,  // false means stop at the first compatible mapping.
                false,  // false means we do not require complete mapping.
                false); // false means free AP are not considered compatible.
        if (!apmf.foundMapping())
        {
            // Since the xover site has been detected by searching for compatible
            // sites that do have a mapping there should always be at least one 
            // mapping and this exception should never occur.
            throw new DENOPTIMException("Missing AP mapping for known XoverSite.");
        }
        
        // To replace each subgraph in the original graphs, we need to 
        // map the APs on the original A/B graph with those in the 
        // corresponding incoming subgraph, which are clones of the original:
        DENOPTIMGraph subGraphA = gA.extractSubgraph(site.getA());
        DENOPTIMGraph subGraphB = gB.extractSubgraph(site.getB());
        
        // Here we create the two AP mappings we need: one for A other for B.
        LinkedHashMap<DENOPTIMAttachmentPoint,DENOPTIMAttachmentPoint> 
        apMapA = new LinkedHashMap<DENOPTIMAttachmentPoint,DENOPTIMAttachmentPoint>();
        LinkedHashMap<DENOPTIMAttachmentPoint,DENOPTIMAttachmentPoint> 
        apMapB = new LinkedHashMap<DENOPTIMAttachmentPoint,DENOPTIMAttachmentPoint>();
        for (Map.Entry<DENOPTIMAttachmentPoint, DENOPTIMAttachmentPoint> e : 
            apmf.getChosenAPMapping().entrySet())
        {
            DENOPTIMAttachmentPoint apOnA = e.getKey();
            DENOPTIMAttachmentPoint apOnB = e.getValue();

            // NB: assumption that vertex IDs are healthy, AND that order of APs
            // is retained upon cloning of the subgraph!
            DENOPTIMAttachmentPoint apOnSubGraphA = subGraphA.getVertexWithId(
                    apOnA.getOwner().getVertexId()).getAP(
                            apOnA.getIndexInOwner());
            DENOPTIMAttachmentPoint apOnSubGraphB = subGraphB.getVertexWithId(
                    apOnB.getOwner().getVertexId()).getAP(
                            apOnB.getIndexInOwner());
            apMapA.put(apOnA, apOnSubGraphB);
            apMapB.put(apOnB, apOnSubGraphA);
        }
 
        // Now we do the actual replacement of subgraphs
        if (!gA.replaceSubGraph(site.getA(), subGraphB, apMapA))
           return false;
        if (!gB.replaceSubGraph(site.getB(), subGraphA, apMapB))
            return false;
        
        return true;
    }

//------------------------------------------------------------------------------
    
    /**
     * Decides whether to apply constitutional symmetry or not. Application
     * of this type of symmetry implies that the operation on the graph/molecule
     * is to be performed on all the attachment point/vertices related to each
     * other by "constitutional symmetry". Such type symmetry is defined by
     * {@link DENOPTIM.utils.FragmentUtils.getMatchingAP getMatchingAP} 
     * for a single fragment, and symmetric vertices in a graph are found by
     * {@link DENOPTIM.utils.GraphUtils.getSymmetricVertices getSymmetricVertices}.
     * This method takes into account the effect of the symmetric substitution 
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
     * chosen randomly according to the possibilities declared by the graph.
     * The graph that owns the vertex will be altered and
     * the original structure and content of the graph will be lost.
     * @param graph the graph to mutate.
     * @param mnt the monitor keeping track of what happens in EA operations.
     * This does not affect the mutation. It only measures how many attempts and
     * failures occur.
     * @return <code>true</code> if the mutation is successful.
     * @throws DENOPTIMException
     */
    public static boolean performMutation(DENOPTIMGraph graph, Monitor mnt)
            throws DENOPTIMException
    {  
        // Get vertices that can be mutated: they can be part of subgraphs
        // embedded in templates
        List<DENOPTIMVertex> mutable = graph.getMutableSites(
                GAParameters.getExcludedMutationTypes());
        if (mutable.size() == 0)
        {
            mnt.increase(CounterID.FAILEDMUTATTEMTS_PERFORM_NOMUTSITE);
            String msg = "Graph has no mutable site. Mutation aborted.";
            DENOPTIMLogger.appLogger.info(msg);
            return false;
        }
        boolean doneMutation = true;
        int numberOfMutations = EAUtils.chooseNumberOfSitesToMutate(
                GAParameters.getMultiSiteMutationWeights(), 
                RandomUtils.nextDouble());
        for (int i=0; i<numberOfMutations; i++)
        {
            if (i>0)
            {
                mutable = graph.getMutableSites(
                        GAParameters.getExcludedMutationTypes());
                break;
            }
            DENOPTIMVertex v = RandomUtils.randomlyChooseOne(mutable);
            doneMutation = performMutation(v,mnt);
            if(!doneMutation)
                break;
        }
        return doneMutation;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Tries to do mutate the given vertex. We assume the vertex to belong to a 
     * graph, if not no mutation is done. The mutation type is
     * chosen randomly according the the possibilities declared by the vertex.
     * The graph that owns the vertex will be altered and
     * the original structure and content of the graph will be lost.
     * @param vertex the vertex to mutate.
     * @param mnt the monitor keeping track of what happens in EA operations.
     * This does not affect the mutation. It only measures how many attempts and
     * failures occur.
     * @return <code>true</code> if the mutation is successful.
     * @throws DENOPTIMException
     */
    public static boolean performMutation(DENOPTIMVertex vertex, Monitor mnt) 
            throws DENOPTIMException
    {
        List<MutationType> mTypes = vertex.getMutationTypes(
                GAParameters.getExcludedMutationTypes());
        if (mTypes.size() == 0)
        {
            return false;
        }
        MutationType mType = RandomUtils.randomlyChooseOne(mTypes);
        return performMutation(vertex, mType, mnt);
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
            MutationType mType, Monitor mnt) throws DENOPTIMException
    {
        DENOPTIMGraph c = vertex.getGraphOwner().clone();
        int pos = vertex.getGraphOwner().indexOf(vertex);
        try
        {
            return performMutation(vertex, mType, false, -1 ,-1, mnt);
        } catch (IllegalArgumentException|NullPointerException e)
        {
            String debugFile = "failedMutation_" + mType 
                    + "_" + vertex.getVertexId() + "(" + pos + ")_"
                    + GAParameters.timeStamp + ".sdf";
            DenoptimIO.writeGraphToSDF(new File(debugFile), c, false);
            DENOPTIMLogger.appLogger.warning("Fatal exception while performing "
                    + "mutation. See file '" + debugFile + "' to reproduce the "
                    + "problem.");
            throw e;
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Mutates the given vertex according to the given mutation type, if
     * possible. Vertex that do not belong to any graph or that do not
     * allow the requested kind of mutation, are not mutated.
     * The graph that owns the vertex will be altered and
     * the original structure and content of the graph will be lost. 
     * @param vertex the vertex to mutate.
     * @param mType the type of mutation to perform.
     * @param force set to <code>true</code> to ignore growth probability. 
     * @param chosenVrtxIdx if greater than or equals to zero, 
     * sets the choice of the incoming vertex to the 
     * given index. This parameter is ignored for mutation types that do not
     * define an incoming vertex (i.e., {@link MutationType#DELETE}).
     * @param chosenApId if greater than or equals to zero, 
     * sets the choice of the AP to the 
     * given index. 
     * Depending on the type of mutation,
     * the AP can be on the incoming vertex (i.e., the vertex that will be added
     * to the graph) or on the target vertex (i.e., the vertex that already 
     * belongs to the graph and that is the focus of the mutation).
     * This parameter is ignored for mutation types that do not
     * define an incoming vertex (i.e., {@link MutationType#DELETE}).
     * @return <code>true</code> if the mutation is successful.
     * @throws DENOPTIMException
     */
    public static boolean performMutation(DENOPTIMVertex vertex, 
            MutationType mType, boolean force, int chosenVrtxIdx, 
            int chosenApId, Monitor mnt) throws DENOPTIMException
    {
        DENOPTIMGraph graph = vertex.getGraphOwner();
        if (graph == null)
        {
            mnt.increase(CounterID.FAILEDMUTATTEMTS_PERFORM_NOOWNER);
            DENOPTIMLogger.appLogger.info("Vertex has no owner - "
                    + "Mutation aborted");
            return false;
        }
        if (!vertex.getMutationTypes(GAParameters.getExcludedMutationTypes())
                .contains(mType))
        {
            mnt.increase(CounterID.FAILEDMUTATTEMTS_PERFORM_BADMUTTYPE);
            DENOPTIMLogger.appLogger.info("Vertex does not allow mutation type "
                    + "'" + mType + "' - Mutation aborted");
            return false;
        }
        
        int graphId = graph.getGraphId();
        int positionOfVertex = graph.indexOf(vertex);
        //NB: since we have renumbered the vertexes, we use the old vertex ID
        // when reporting what vertex is being mutated.
        graph.setLocalMsg(graph.getLocalMsg() + " " + mType + " " 
                + vertex.getProperty(DENOPTIMConstants.STOREDVID) 
                + "(" + positionOfVertex+")");
        
        boolean done = false;
        switch (mType) 
        {
            case CHANGEBRANCH:
                done = rebuildBranch(vertex, force, chosenVrtxIdx, chosenApId);
                if (!done)
                    mnt.increase(
                            CounterID.FAILEDMUTATTEMTS_PERFORM_NOCHANGEBRANCH);
                break;
                
            case CHANGELINK:
                done = substituteLink(vertex, chosenVrtxIdx, mnt);
                if (!done)
                    mnt.increase(
                            CounterID.FAILEDMUTATTEMTS_PERFORM_NOCHANGELINK);
                break;
                
            case DELETELINK:
                done = deleteLink(vertex, chosenApId, mnt);
                if (!done)
                    mnt.increase(
                            CounterID.FAILEDMUTATTEMTS_PERFORM_NOCHANGELINK);
                break;
                
            case DELETECHAIN:
                done = deleteChain(vertex, mnt);
                if (!done)
                    mnt.increase(
                            CounterID.FAILEDMUTATTEMTS_PERFORM_NODELETECHAIN);
                break;
                
            case ADDLINK:
                if (chosenApId < 0)
                {
                    List<Integer> candidates = new ArrayList<Integer>();
                    for (DENOPTIMVertex c : vertex.getChilddren())
                    {
                        candidates.add(c.getEdgeToParent().getSrcAP()
                                .getIndexInOwner());
                    }
                    if (candidates.size() == 0)
                    {
                        mnt.increase(
                                CounterID.FAILEDMUTATTEMTS_PERFORM_NOADDLINK);
                    break;
                    }   
                    chosenApId = RandomUtils.randomlyChooseOne(candidates);
                }
                done = extendLink(vertex, chosenApId, chosenVrtxIdx, mnt);
                if (!done)
                    mnt.increase(
                            CounterID.FAILEDMUTATTEMTS_PERFORM_NOADDLINK);
                break;
                
            case EXTEND:
                vertex.getGraphOwner().removeCappingGroupsOn(vertex);
                done = extendGraph(vertex, false, false, force, chosenVrtxIdx, 
                        chosenApId);
                if (!done)
                    mnt.increase(CounterID.FAILEDMUTATTEMTS_PERFORM_NOEXTEND);
                break;
                
            case DELETE:
                done = deleteFragment(vertex);
                if (!done)
                    mnt.increase(CounterID.FAILEDMUTATTEMTS_PERFORM_NODELETE);
                break;
        }
        
        String msg = "Mutation '" + mType.toString() + "' on vertex " 
                + vertex.toString() + " (position " + positionOfVertex
                + " in graph " + graphId+"): ";
        if (done)
        {
            msg = msg + "done";
            
            // Triggers reconstruction of the molecular representation of
            // templates upon changes of the embedded graph
            if (graph.getTemplateJacket() != null)
            {
                graph.getTemplateJacket().clearIAtomContainer();
            }
        } else {
            msg = msg + "unsuccessful";
        }
        DENOPTIMLogger.appLogger.info(msg);

        return done;
    }

//------------------------------------------------------------------------------



}
