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

package denoptim.ga;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.http.TruncatedChunkException;
import org.openscience.cdk.graph.ShortestPaths;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.isomorphism.Mappings;
import org.paukov.combinatorics3.Generator;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragmenter.BridgeHeadFindingRule;
import denoptim.fragspace.APMapFinder;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.fragspace.GraphLinkFinder;
import denoptim.fragspace.IdFragmentAndAP;
import denoptim.graph.APClass;
import denoptim.graph.APMapping;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.DGraph;
import denoptim.graph.Edge;
import denoptim.graph.RelatedAPPair;
import denoptim.graph.Ring;
import denoptim.graph.SymmetricAPs;
import denoptim.graph.SymmetricVertexes;
import denoptim.graph.Template;
import denoptim.graph.Template.ContractLevel;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;
import denoptim.graph.rings.ChainLink;
import denoptim.graph.rings.ClosableChain;
import denoptim.graph.rings.PathSubGraph;
import denoptim.graph.rings.RandomCombOfRingsIterator;
import denoptim.graph.rings.RingClosingAttractor;
import denoptim.graph.rings.RingClosureParameters;
import denoptim.io.DenoptimIO;
import denoptim.logging.CounterID;
import denoptim.logging.Monitor;
import denoptim.molecularmodeling.ThreeDimTreeBuilder;
import denoptim.programs.RunTimeParameters.ParametersType;
import denoptim.programs.denovo.GAParameters;
import denoptim.programs.fragmenter.CuttingRule;
import denoptim.programs.fragmenter.MatchedBond;
import denoptim.utils.CrossoverType;
import denoptim.utils.GraphUtils;
import denoptim.utils.ManySMARTSQuery;
import denoptim.utils.MutationType;
import denoptim.utils.ObjectPair;
import denoptim.utils.Randomizer;

/**
 * Collection of operators meant to alter graphs and associated utilities.
 */

public class GraphOperations
{
    
//------------------------------------------------------------------------------

    /**
     * Identify crossover sites, i.e., subgraphs that can be swapped between 
     * two graphs (i.e., the parents). This method will try to locate as many as 
     * possible crossover sites without falling into combinatorial explosion.
     * @param gA one of the parent graphs.
     * @param gB the other of the parent graphs.
     * @param fragSpace the space of building blocks defining how to generate
     * graphs.
     * @param maxSizeXoverSubGraph the limit to the size of subgraphs that can
     * can be exchanged by crossover.
     * @return the list of pairs of crossover sites.
     * @throws DENOPTIMException 
     */
    public static List<XoverSite> locateCompatibleXOverPoints(
            DGraph graphA, DGraph graphB, FragmentSpace fragSpace, 
            int maxSizeXoverSubGraph) 
                    throws DENOPTIMException
    {
        // First, we identify all the edges that allow crossover, and collect
        // their target vertexes (i.e., all the potential seed vertexes of 
        // subgraphs that crossover could swap.
        List<Vertex[]> compatibleVrtxPairs = new ArrayList<Vertex[]>();
        for (Edge eA : graphA.getEdgeList())
        {
            Vertex vA = eA.getTrgAP().getOwner();
            // We don't do genetic operations on capping vertexes
            if (vA.getBuildingBlockType() == BBType.CAP)
                continue;
            
            for (Edge eB : graphB.getEdgeList())
            {
                Vertex vB = eB.getTrgAP().getOwner();
                // We don't do genetic operations on capping vertexes
                if (vB.getBuildingBlockType() == BBType.CAP)
                    continue;
                
                //Check condition for considering this combination
                if (isCrossoverPossible(eA, eB, fragSpace))
                {
                    Vertex[] pair = new Vertex[]{vA,vB};
                    compatibleVrtxPairs.add(pair);
                }
            }
        }
        
        // The crossover sites are the combination of the above compatible
        // vertexes that define subgraphs respecting the requirements for 
        // being swapped between the two graphs.
        ArrayList<XoverSite> sites = new ArrayList<XoverSite>();
        for (Vertex[] pair : compatibleVrtxPairs)
        {
            Vertex vA = pair[0];
            Vertex vB = pair[1];
            DGraph gA = vA.getGraphOwner();
            DGraph gB = vB.getGraphOwner();
            
            // Here we also identify the branch identity of each descendant
            List<Vertex> descendantsA = new ArrayList<Vertex>();
            gA.getChildrenTree(vA, descendantsA, true);
            List<Vertex> descendantsB = new ArrayList<Vertex>();
            gB.getChildrenTree(vB, descendantsB, true);
            
            // Branches that are isomorphic are not considered for crossover
            DGraph test1 = gA.clone();
            DGraph test2 = gB.clone();
            try
            {
                DGraph subGraph1 = test1.extractSubgraph(gA.indexOf(vA));
                DGraph subGraph2 = test2.extractSubgraph(gB.indexOf(vB));
                if (maxSizeXoverSubGraph >= Math.max(subGraph1.getVertexCount(), 
                        subGraph2.getVertexCount()))
                {
                    if (!subGraph1.isIsomorphicTo(subGraph2))
                    {
                        List<Vertex> branchOnVA = new ArrayList<Vertex>();
                        branchOnVA.add(vA);
                        branchOnVA.addAll(descendantsA);
                        List<Vertex> branchOnVB = new ArrayList<Vertex>();
                        branchOnVB.add(vB);
                        branchOnVB.addAll(descendantsB);
                        
                        checkAndAddXoverSites(fragSpace, branchOnVA, branchOnVB, 
                                CrossoverType.BRANCH, sites);
                    }
                }
            } catch (DENOPTIMException e)
            {
                //We should never end up here.
                e.printStackTrace();
            }
            
            // To limit the number of combination, we first get rid of end-point
            // candidates that cannot be used
            List<Vertex[]> usablePairs = new ArrayList<Vertex[]>();
            
            // To identify subgraphs smaller than the full branch we need to find
            // where such subgraphs end, i.e., the vertexes at the end of
            // such subgraphs (included in them), a.k.a. the subgraph ends.
            // Since these subgraph ends will need to allow connection with the
            // rest of the original graph, they are related to the 
            // already-identified crossover-compatible
            // sites, i.e., they are the parents of the vertexes collected in 
            // compatibleVrtxPairs. Yet, we can combine them
            // * in any number from 1 to all of them,
            // * in any combination of the chosen number of them.
            // Also, note that the ends need not to cover all the branches. So,
            // some combinations will have to cut some branches short while
            // taking some other branches completely till their last leaf.
            for (Vertex[] otherPair : compatibleVrtxPairs)
            {
                // NB: the xover compatible sites are the child vertexes of the
                // subgraph ends. So we need to get the parent
                Vertex nextToEndOnA = otherPair[0];
                Vertex nextToEndOnB = otherPair[1];
                Vertex endOnA = nextToEndOnA.getParent();
                Vertex endOnB = nextToEndOnB.getParent();
                if (endOnA==null || endOnB==null)
                    continue;
                
                // Exclude vertexes that are not downstream to the seed of the subgraph
                if (!descendantsA.contains(endOnA) && endOnA!=vA
                        || !descendantsB.contains(endOnB) && endOnB!=vB)
                    continue;
                
                // If any partner is a fixed-structure templates...
                if ((gA.getTemplateJacket()!=null 
                        && gA.getTemplateJacket().getContractLevel()
                        == ContractLevel.FIXED_STRUCT)
                        || (gB.getTemplateJacket()!=null 
                                && gB.getTemplateJacket().getContractLevel()
                                == ContractLevel.FIXED_STRUCT))
                {
                    //...the two paths must have same length. This would be 
                    // checked anyway later when checking for isostructural
                    // subgraphs, but here it helps reducing the size of the 
                    // combinatorial problem.
                    PathSubGraph pathA = new PathSubGraph(vA, endOnA, gA);
                    PathSubGraph pathB = new PathSubGraph(vB, endOnB, gB);
                    if (pathA.getPathLength()!=pathB.getPathLength())
                        continue;
                }
                Vertex[] pairOfEnds = new Vertex[]{endOnA,endOnB};
                if (!usablePairs.contains(pairOfEnds))
                    usablePairs.add(pairOfEnds);
            }
            
            // We classify the pairs by branch ownership
            TreeMap<String,List<Vertex[]>> sitesByBranchIdA = 
                    new TreeMap<String,List<Vertex[]>>();
            TreeMap<String,List<Vertex[]>> sitesByBranchIdB = 
                    new TreeMap<String,List<Vertex[]>>();
            for (Vertex[] pp : usablePairs)
            {
                String branchIdA = gA.getBranchIdOfVertexAsStr(pp[0]);
                String branchIdB = gB.getBranchIdOfVertexAsStr(pp[1]);
                if (sitesByBranchIdA.containsKey(branchIdA))
                {
                    sitesByBranchIdA.get(branchIdA).add(pp);
                } else {
                    ArrayList<Vertex[]> lst = new ArrayList<Vertex[]>();
                    lst.add(pp);
                    sitesByBranchIdA.put(branchIdA, lst);
                }
                if (sitesByBranchIdB.containsKey(branchIdB))
                {
                    sitesByBranchIdB.get(branchIdB).add(pp);
                } else {
                    ArrayList<Vertex[]> lst = new ArrayList<Vertex[]>();
                    lst.add(pp);
                    sitesByBranchIdB.put(branchIdB, lst);
                }
            }
            
            // The side with the smallest set of branches determines the max
            // number of pairs that can define a subgraph.
            TreeMap<String,List<Vertex[]>> fewestBranchesSide = null;
            if (sitesByBranchIdA.size() <= sitesByBranchIdB.size())
                fewestBranchesSide = sitesByBranchIdA;
            else
                fewestBranchesSide = sitesByBranchIdB;
            
            // Add the empty, i.e., the branch with empty is not cut short.
            for (List<Vertex[]> val : fewestBranchesSide.values())
                val.add(new Vertex[]{null,null});
            
            // Generate the combinations: Cartesian product of multiple lists.
            // NB: this combinatorial generator retains the 
            // sequence of generated subsets (important for reproducibility)
            List<List<Vertex[]>> preCombsOfEnds = Generator.cartesianProduct(
                    fewestBranchesSide.values())
                    .stream()
                    .limit(100000) //Prevent explosion!!!
                    .collect(Collectors.<List<Vertex[]>>toList());
            
            // Remove the 'null,null' place holders that indicate the use of no
            // end-point on a specific branch.
            List<List<Vertex[]>> combsOfEnds = new ArrayList<List<Vertex[]>>();
            for (List<Vertex[]> comb : preCombsOfEnds)
            {
                List<Vertex[]> nullPurgedComb = new ArrayList<Vertex[]>();
                for (Vertex[] inPair : comb)
                {
                    if (inPair[0]!=null && inPair[1]!=null)
                        nullPurgedComb.add(inPair);
                }
                // the case where all entries are null corresponds to use no
                // end point on any branch, which is already considered above.
                if (nullPurgedComb.size()>0)
                    combsOfEnds.add(nullPurgedComb);
            }
            
            // This would be a strategy to parallelize. However, this type of 
            // parallelization is not compatible with ensuring reproducibility.
            // Perhaps, we could guarantee reproducibility by acting on the
            // collector of sites as to ensure the list is sorted
            // at the end of the generation of all possibilities.
            /*
            combsOfEnds
                .parallelStream()
                .limit(50) // Prevent explosion!
                .forEach(c -> processCombinationOfEndPoints(pair, c, sites,
                    fragSpace));
            */
            
            combsOfEnds.stream()
                .limit(50) // Prevent explosion!
                .forEach(c -> processCombinationOfEndPoints(pair, c, sites,
                    fragSpace));
        }
        
        // NB: we consider only templates that are at the same level of embedding
        for (Vertex vA : graphA.getVertexList())
        {
            if (!(vA instanceof Template))
                continue;
            Template tA = (Template) vA;
            
            if (tA.getContractLevel() == ContractLevel.FIXED)
                continue;
            
            for (Vertex vB : graphB.getVertexList())
            {
                if (!(vB instanceof Template))
                    continue;
                Template tB = (Template) vB;
                
                if (tB.getContractLevel() == ContractLevel.FIXED)
                    continue;
                
                for (XoverSite xos : locateCompatibleXOverPoints(
                        tA.getInnerGraph(), tB.getInnerGraph(), fragSpace,
                        maxSizeXoverSubGraph))
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
    private static void processCombinationOfEndPoints(Vertex[] pair,
            List<Vertex[]> cominationOfEnds,
            List<XoverSite> collector, FragmentSpace fragSpace)
    {
        // Empty set corresponds to using the entire branch and subgraph and
        // has been already dealt with at this point
        if (cominationOfEnds.size()==0)
            return;
        
        // This would be a strategy to parallelize. However, this type of 
        // parallelization is not compatible with ensuring reproducibility.
        // Perhaps, we could guarantee reproducibility by acting on the
        // collector as to ensure the list of collected results is sorted
        // at the end of the generation of all possibilities
        /*
        List<List<Vertex[]>> permutsOfEnds = new ArrayList<List<Vertex[]>>();
        Generator.subset(cominationOfEnds)
            .simple()
            .stream()
            .limit(100) // Prevent explosion!
            .forEach(c -> permutsOfEnds.add(c));
        permutsOfEnds
            .parallelStream()
            .forEach(c -> processPermutationOfEndPoints(pair, c, collector,
                    fragSpace));
        */
        
        Generator.permutation(cominationOfEnds)
            .simple()
            .stream()
            .limit(100) // Prevent explosion!
            .forEach(c -> processPermutationOfEndPoints(pair, c, collector,
                    fragSpace));
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
    private static void processPermutationOfEndPoints(Vertex[] pair,
            List<Vertex[]> chosenSequenceOfEndpoints,
            List<XoverSite> collector, FragmentSpace fragSpace)
    {
        Vertex vA = pair[0];
        Vertex vB = pair[1];
        DGraph gA = vA.getGraphOwner();
        DGraph gB = vB.getGraphOwner();
        
        // Exclude overlapping combinations
        boolean exclude = false;
        for (Vertex[] pairA : chosenSequenceOfEndpoints)
        {
            for (Vertex[] pairB : chosenSequenceOfEndpoints)
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
        
        List<Vertex> subGraphEndInA = new ArrayList<Vertex>();
        List<Vertex> subGraphEndInB = new ArrayList<Vertex>();
        List<Vertex> alreadyIncludedFromA = new ArrayList<Vertex>();
        List<Vertex> alreadyIncludedFromB = new ArrayList<Vertex>();
        for (Vertex[] otherPair : chosenSequenceOfEndpoints)
        {
            Vertex endOnA = otherPair[0];
            Vertex endOnB = otherPair[1];
            
            // Ignore vertexes that are already part of the subgraph
            if (alreadyIncludedFromA.contains(endOnA)
                    || alreadyIncludedFromB.contains(endOnB))
                continue;
            
            PathSubGraph pathA = new PathSubGraph(vA, endOnA, gA);
            PathSubGraph pathB = new PathSubGraph(vB, endOnB, gB);
            subGraphEndInA.add(endOnA);
            subGraphEndInB.add(endOnB);
            alreadyIncludedFromA.addAll(pathA.getVertecesPath());
            alreadyIncludedFromB.addAll(pathB.getVertecesPath());
        }
        ArrayList<Vertex> subGraphA = new ArrayList<Vertex>();
        subGraphA.add(vA);
        if (!subGraphEndInA.contains(vA))
            gA.getChildTreeLimited(vA, subGraphA, subGraphEndInA, true);

        ArrayList<Vertex> subGraphB = new ArrayList<Vertex>();
        subGraphB.add(vB);
        if (!subGraphEndInB.contains(vB))
            gB.getChildTreeLimited(vB, subGraphB, subGraphEndInB, true);
        
        // The two subgraphs must not be isomorfic to prevent unproductive crossover
        if (subGraphA.size()>1 && subGraphB.size()>1)
        {
            DGraph subGraphCloneA = gA.extractSubgraph(subGraphA);
            DGraph subGraphCloneB = gB.extractSubgraph(subGraphB);
            if (subGraphCloneA.isIsomorphicTo(subGraphCloneB))
                return;
        } else {
            if (subGraphA.get(0).sameAs(subGraphB.get(0), new StringBuilder()))
                return;
        }
        
        checkAndAddXoverSites(fragSpace, subGraphA, subGraphB, 
                CrossoverType.SUBGRAPH, collector);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Here we check that the given subgraphs have a mapping that allows to swap
     * them, and full fill any other requirement other than not being isomorphic 
     * (which is done in parent methods calling this one). Then, we create the 
     * corresponding crossover site and place it in the collector of such sites.
     * 
     * <b>NB: This method assumes that no crossover can involve seed of the spanning
     * tree (whether scaffold, of anything else).</b>
     */
    private static void checkAndAddXoverSites(FragmentSpace fragSpace,
            List<Vertex> subGraphA, 
            List<Vertex> subGraphB, CrossoverType xoverType,
            List<XoverSite> collector)
    {
        DGraph gOwnerA = subGraphA.get(0).getGraphOwner();
        DGraph gOwnerB = subGraphB.get(0).getGraphOwner();
        
        // What APs need to find a corresponding AP in the other 
        // subgraph in order to allow swapping?
        List<AttachmentPoint> needyAPsA = gOwnerA.getInterfaceAPs(
                subGraphA);
        List<AttachmentPoint> allAPsA = gOwnerA.getSubgraphAPs(
                subGraphA);
        List<AttachmentPoint> needyAPsB = gOwnerB.getInterfaceAPs(
                subGraphB);
        List<AttachmentPoint> allAPsB = gOwnerB.getSubgraphAPs(
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
        Template jacketTmplA = gOwnerA.getTemplateJacket();
        Template jacketTmplB = gOwnerB.getTemplateJacket();
        if (xoverType == CrossoverType.BRANCH 
                && jacketTmplA==null
                && jacketTmplB==null)
        {
            XoverSite xos = new XoverSite(subGraphA, needyAPsA, subGraphB, 
                    needyAPsB, xoverType);
            if (!collector.contains(xos))
                collector.add(xos);
            return;
        }
        
        if ((jacketTmplA!=null && jacketTmplA.getContractLevel()
                ==ContractLevel.FIXED_STRUCT)
                || (jacketTmplB!=null && jacketTmplB.getContractLevel()
                        ==ContractLevel.FIXED_STRUCT))
        {
            // Avoid to alter cyclicity of inner graphs
            for (Vertex v : subGraphA)
                if (v.isRCV() && !gOwnerA.getRingsInvolvingVertex(v).isEmpty())
                    return;
            for (Vertex v : subGraphB)
                if (v.isRCV() && !gOwnerB.getRingsInvolvingVertex(v).isEmpty())
                    return;
            
            // Avoid to change the structure of inner graphs
            DGraph subGraphCloneA = gOwnerA.extractSubgraph(subGraphA);
            DGraph subGraphCloneB = gOwnerB.extractSubgraph(subGraphB);
            if (!subGraphCloneA.isIsostructuralTo(subGraphCloneB))
                return;
        }
        
        // Retain connection to parent to keep directionality of spanning tree!
        // To this end, identify the seed of the subgraphs...
        Vertex seedOnA = gOwnerA.getDeepestAmongThese(subGraphA);
        Vertex seedOnB = gOwnerB.getDeepestAmongThese(subGraphB);
        //...and ensure we use the same APs to link to the parent graph.
        APMapping fixedRootAPs = new APMapping();
        fixedRootAPs.put(seedOnA.getEdgeToParent().getTrgAP(),
                seedOnB.getEdgeToParent().getTrgAP());
            
        APMapFinder apmf = new APMapFinder(fragSpace,
                allAPsA, needyAPsA, allAPsB, needyAPsB,
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
     * @param eA first edge of the pair.
     * @param eB second edge of the pair.
     * @return <code>true</code> if the condition is satisfied.
     */
    private static boolean isCrossoverPossible(Edge eA, Edge eB,
            FragmentSpace fragSpace)
    {
        APClass apClassSrcA = eA.getSrcAPClass();
        APClass apClassTrgA = eA.getTrgAPClass();
        APClass apClassSrcB = eB.getSrcAPClass();
        APClass apClassTrgB = eB.getTrgAPClass();
        
        if (apClassSrcA.isCPMapCompatibleWith(apClassTrgB, fragSpace))
        {
            if (apClassSrcB.isCPMapCompatibleWith(apClassTrgA, fragSpace))
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

    protected static boolean deleteLink(Vertex vertex,
            int chosenVrtxIdx, Monitor mnt, FragmentSpace fragSpace) 
                    throws DENOPTIMException
    {
        DGraph graph = vertex.getGraphOwner();
        boolean done = graph.removeVertexAndWeld(vertex, fragSpace);
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

    protected static boolean substituteLink(Vertex vertex,
            int chosenVrtxIdx, Monitor mnt, FragmentSpace fragSpace)
                    throws DENOPTIMException
    {
        //TODO: for reproducibility, the AP mapping should become an optional
        // parameter: if given we try to use it, if not given, GraphLinkFinder
        // will try to find a suitable mapping.
        
        GraphLinkFinder glf = null;
        if (chosenVrtxIdx<0)
        {
            glf = new GraphLinkFinder(fragSpace, vertex);
        } else {
            glf = new GraphLinkFinder(fragSpace, vertex, chosenVrtxIdx);
        }
        if (!glf.foundAlternativeLink())
        {
            mnt.increase(CounterID.FAILEDMUTATTEMTS_PERFORM_NOCHANGELINK_FIND);
            return false;
        }
        
        DGraph graph = vertex.getGraphOwner();
        boolean done = graph.replaceVertex(vertex,
                glf.getChosenAlternativeLink().getBuildingBlockId(),
                glf.getChosenAlternativeLink().getBuildingBlockType(),
                glf.getChosenAPMapping().toIntMappig(),
                fragSpace);
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
    public static boolean extendLink(Vertex vertex,
            int chosenAPId, Monitor mnt, FragmentSpace fragSpace) 
                    throws DENOPTIMException
    {
        return extendLink(vertex, chosenAPId, -1 , mnt, fragSpace);
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
    public static boolean extendLink(Vertex vertex, int chosenAPId, 
            int chosenNewVrtxId, Monitor mnt, FragmentSpace fragSpace) 
                    throws DENOPTIMException
    {
        AttachmentPoint ap = vertex.getAP(chosenAPId);
        if (ap == null)
        {
            throw new DENOPTIMException("No AP "+chosenAPId+" in vertex "
                    +vertex+".");
        }
        Edge e = ap.getEdgeUserThroughout();
        
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
        return extendLink(e, chosenNewVrtxId, mnt, fragSpace);
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
    public static boolean extendLink(Edge edge, int chosenBBIdx,
            Monitor mnt, FragmentSpace fragSpace) throws DENOPTIMException
    {
        //TODO: for reproducibility, the AP mapping should become an optional
        // parameter: if given we try to use it, if not given we GraphLinkFinder
        // will try to find a suitable mapping.
        
        GraphLinkFinder glf = null;
        if (chosenBBIdx < 0)
        {
            glf = new GraphLinkFinder(fragSpace, edge);
        } else {
            glf = new GraphLinkFinder(fragSpace, edge,chosenBBIdx);
        }
        if (!glf.foundAlternativeLink())
        {
            mnt.increase(CounterID.FAILEDMUTATTEMTS_PERFORM_NOADDLINK_FIND);
            return false;
        }
        
        // Need to convert the mapping to make it independent from the instance
        // or the new link.
        LinkedHashMap<AttachmentPoint,Integer> apMap = 
                new LinkedHashMap<AttachmentPoint,Integer>();
        for (Entry<AttachmentPoint, AttachmentPoint> e : 
            glf.getChosenAPMapping().entrySet())
        {
            apMap.put(e.getKey(), e.getValue().getIndexInOwner());
        }
        
        DGraph graph = edge.getSrcAP().getOwner().getGraphOwner();
        boolean done = graph.insertVertex(edge,
                glf.getChosenAlternativeLink().getBuildingBlockId(),
                glf.getChosenAlternativeLink().getBuildingBlockType(),
                apMap, fragSpace);
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
     * @param maxHeavyAtoms maximum number of heavy atoms.
     * @return <code>true</code> if substitution is successful
     * @throws DENOPTIMException
     */

    protected static boolean rebuildBranch(Vertex vertex,
            boolean force, int chosenVrtxIdx, int chosenApId, 
            GAParameters settings) throws DENOPTIMException
    {
        DGraph g = vertex.getGraphOwner();

        // first get the edge with the parent
        Edge e = vertex.getEdgeToParent();
        if (e == null)
        {
            String msg = "Program Bug in substituteFragment: Unable to locate "
                    + "parent edge for vertex "+vertex+" in graph "+g;
            settings.getLogger().log(Level.SEVERE, msg);
            throw new DENOPTIMException(msg);
        }

        // vertex id of the parent
        long pvid = e.getSrcVertex();
        Vertex parentVrt = g.getVertexWithId(pvid);

        // Need to remember symmetry because we are deleting the symm. vertices
        boolean symmetry = g.hasSymmetryInvolvingVertex(vertex);
        
        // delete the vertex and its children and all its symmetric partners
        deleteFragment(vertex);

        // extend the graph at this vertex but without recursion
        return extendGraph(parentVrt,false,symmetry,force,chosenVrtxIdx,
                chosenApId, settings);
    }

//------------------------------------------------------------------------------

    /**
     * Deletion mutation removes the vertex and also the
     * symmetric partners on its parent.
     * @param vertex
     * @return <code>true</code> if deletion is successful
     * @throws DENOPTIMException
     */
    
    protected static boolean deleteFragment(Vertex vertex)
            throws DENOPTIMException
    {
        long vid = vertex.getVertexId();
        DGraph molGraph = vertex.getGraphOwner();

        if (molGraph.hasSymmetryInvolvingVertex(vertex))
        {
            List<Vertex> toRemove = new ArrayList<Vertex>();
            toRemove.addAll(molGraph.getSymSetForVertex(vertex));
            for (Vertex v : toRemove)
            {
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
    
    protected static boolean deleteChain(Vertex vertex, Monitor mnt,
            FragmentSpace fragSpace) throws DENOPTIMException
    {
        long vid = vertex.getVertexId();
        DGraph molGraph = vertex.getGraphOwner();

        if (molGraph.hasSymmetryInvolvingVertex(vertex))
        {
            List<Vertex> toRemove = new ArrayList<Vertex>();
            toRemove.addAll(molGraph.getSymSetForVertex(vertex));
            for (Vertex v : toRemove)
            {
                if (!v.getMutationTypes(new ArrayList<MutationType>())
                        .contains(MutationType.DELETECHAIN))
                    continue;
                molGraph.removeChainUpToBranching(v, fragSpace);
            }
        }
        else
        {
            molGraph.removeChainUpToBranching(vertex, fragSpace);
        }

        if (molGraph.getVertexWithId(vid) == null && molGraph.getVertexCount() > 1)
            return true;
        return false;
    }
    
//------------------------------------------------------------------------------

    /**
     * Tries to use any free AP of the given vertex to close ring in the graph 
     * by adding a chord.
     * @param vertex the vertex on which we start to close a the ring.
     * @param mnt monitoring tool to keep count of events.
     * @param force use <code>true</code> to by-pass random decisions and force
     * the creation of rings.
     * @param fragSpace the fragment space controlling how we put together
     * building blocks.
     * @param settings the GA settings controlling how we work.
     * @return <code>true</code> when at least one ring was added to the graph.
     * @throws DENOPTIMException
     * 
     */
    
    protected static boolean addRing(Vertex vertex, Monitor mnt, boolean force,
            FragmentSpace fragSpace, GAParameters settings) 
                    throws DENOPTIMException
    {
        // get settings //TODO: this should happen inside RunTimeParameters
        RingClosureParameters rcParams = new RingClosureParameters();
        if (settings.containsParameters(ParametersType.RC_PARAMS))
        {
            rcParams = (RingClosureParameters)settings.getParameters(
                    ParametersType.RC_PARAMS);
        }
        Randomizer rng = settings.getRandomizer();
        
        // First of all we remove capping groups in the graph
        vertex.getGraphOwner().removeCappingGroups();
        
        List<AttachmentPoint> freeeAPs = vertex.getFreeAPThroughout();
        if (freeeAPs.size()==0)
        {
            mnt.increase(CounterID.FAILEDMUTATTEMTS_PERFORM_NOADDRING_NOFREEAP);
            return false;
        }
        DGraph originalGraph = vertex.getGraphOwner();
        DGraph tmpGraph = originalGraph.clone();
        
        Vertex headVrtx = tmpGraph.getVertexAtPosition(originalGraph.indexOf(
                vertex));
        
        // Define the set of rings on the cloned (tmp) graph
        List<Ring> setOfRingsOnTmpGraph = null;
        for (AttachmentPoint srcAP : headVrtx.getFreeAPThroughout())
        {
            APClass apc = srcAP.getAPClass();
            
            // Skip if the APClass is not allowed to form ring closures
            if (!fragSpace.getRCCompatibilityMatrix().containsKey(apc))
            {
                continue;
            }
            List<APClass> rcTrgAPCs = fragSpace.getRCCompatibilityMatrix().get(
                    apc);
            
            // NB: the fragment space may or may not have a RCV for this AP
            Vertex rcvOnSrcAP = null;
            List<Vertex> candidateRCVs = fragSpace.getRCVsForAPClass(apc);
            boolean rcvIsChosenArbitrarily = false;
            if (candidateRCVs.size()>0)
            {
                rcvOnSrcAP = rng.randomlyChooseOne(candidateRCVs);
            } else {
                rcvIsChosenArbitrarily = true;
                rcvOnSrcAP = fragSpace.getPolarizedRCV(true);
            }
            
            // Add the RCV on this AP
            List<Vertex> rcvAddedToGraph = new ArrayList<Vertex>();
            tmpGraph.appendVertexOnAP(srcAP, rcvOnSrcAP.getAP(0));
            rcvAddedToGraph.add(rcvOnSrcAP);
            
            // Add a few RCVs in the rest of the system
            // WARNING: hard-coded max number of attempts. It should not be too
            // high to prevent combinatorial explosion.
            for (int i=0; i<20; i++) 
            {
                // Do it only on APs that APClass-compatible with chord formation
                List<AttachmentPoint> apsToTry = 
                        tmpGraph.getAvailableAPsThroughout();
                int numberOfAttempts = apsToTry.size();
                AttachmentPoint trgAP = null;
                for (int iap=0; iap<numberOfAttempts; iap++)
                {
                    AttachmentPoint candidate = rng.randomlyChooseOne(apsToTry);
                    if (rcTrgAPCs.contains(candidate.getAPClass()))
                    {
                        trgAP = candidate;
                        break;
                    }
                    apsToTry.remove(trgAP);
                }
                if (trgAP==null)
                {
                    // No more AP can be used to form rings with srcAP
                    break;
                }
                
                // Choose type of RCV
                Vertex rcvOnTrgAP = null;
                if (rcvIsChosenArbitrarily)
                {
                    rcvOnTrgAP = fragSpace.getPolarizedRCV(false);
                } else {
                    List<Vertex> candRCVs = fragSpace.getRCVsForAPClass(
                            trgAP.getAPClass());
                    if (candRCVs.size()>0)
                    {
                        APClass requiredAPC = RingClosingAttractor.RCAAPCMAP.get(
                                        rcvOnSrcAP.getAP(0).getAPClass());
                        List<Vertex> keptRCVs = new ArrayList<Vertex>();
                        for (Vertex rcv : candRCVs)
                        {
                            if (requiredAPC.equals(rcv.getAP(0).getAPClass()))
                                keptRCVs.add(rcv);
                        }
                        rcvOnTrgAP = rng.randomlyChooseOne(keptRCVs);
                        if (rcvOnTrgAP==null)
                        {
                            // no RCV is both usable and compatible with the
                            // one already selected for srcAP
                            continue;
                        }
                    } else {
                        // The APClass settings and RCV compatibility rules
                        // do not allow to identify a suitable RCV
                        continue;
                    }
                }
                
                // append RCV
                tmpGraph.appendVertexOnAP(trgAP, rcvOnTrgAP.getAP(0));
                rcvAddedToGraph.add(rcvOnTrgAP);
            }
            if (rcvAddedToGraph.size() < 2)
            {
                // TODO: we could consider counting these to see what is the 
                // most frequent cause of this mutation to fail
                continue;
            }
            
            // Define rings to close in tmp graph
            ThreeDimTreeBuilder t3d = new ThreeDimTreeBuilder(
                    settings.getLogger(), rng);
            t3d.setAlignBBsIn3D(false); //3D not needed
            IAtomContainer mol = t3d.convertGraphTo3DAtomContainer(tmpGraph,true);
            RandomCombOfRingsIterator rCombIter = new RandomCombOfRingsIterator(
                    mol,
                    tmpGraph, 
                    settings.getMaxRingsAddedByMutation(),
                    fragSpace, rcParams);
            
            //TODO: possibility to generate multiple combinations, rank them,  
            // and pick the best one. Though definition of comparator is not
            // unambiguous.
            
            setOfRingsOnTmpGraph = rCombIter.next();
            
            // Termination of search over trgAPs
            if (setOfRingsOnTmpGraph.size()>0)
                break;
            
            // Cleanup before starting new iteration
            for (Vertex toRemove : rcvAddedToGraph)
                tmpGraph.removeVertex(toRemove);
        }
        if (setOfRingsOnTmpGraph==null || setOfRingsOnTmpGraph.size()==0)
        {
            mnt.increase(CounterID.FAILEDMUTATTEMTS_PERFORM_NOADDRING_NORINGCOMB);
            return false;
        }
        
        // Project rings into the actual graph
        boolean done = false;
        for (Ring rOnTmp : setOfRingsOnTmpGraph)
        {
            // Project head RCV and all info to attach it to original graph
            Vertex vToHeadRCV = originalGraph.getVertexAtPosition(
                    tmpGraph.indexOf(rOnTmp.getHeadVertex().getParent()));
            AttachmentPoint apToHeadRCV = vToHeadRCV.getAP(
                    rOnTmp.getHeadVertex().getEdgeToParent().getSrcAP()
                    .getIndexInOwner());
            Vertex headRCV = rOnTmp.getHeadVertex().clone();
            headRCV.setVertexId(GraphUtils.getUniqueVertexIndex());
            
            // And append head RCV on original graph
            originalGraph.appendVertexOnAP(apToHeadRCV, headRCV.getAP(0));
            
            // Project tail RCV and all info to attach it to original graph
            Vertex vToTailRCV = originalGraph.getVertexAtPosition(
                    tmpGraph.indexOf(rOnTmp.getTailVertex().getParent()));
            AttachmentPoint apToTailRCV = vToTailRCV.getAP(
                    rOnTmp.getTailVertex().getEdgeToParent().getSrcAP()
                    .getIndexInOwner());
            Vertex tailRCV = rOnTmp.getHeadVertex().clone();
            tailRCV.setVertexId(GraphUtils.getUniqueVertexIndex());
            
            // And append tail RCV on original graph
            originalGraph.appendVertexOnAP(apToTailRCV, tailRCV.getAP(0));
            
            // Add ring
            originalGraph.addRing(headRCV, tailRCV);
            done = true;
        }
        
        // Restore capping groups
        vertex.getGraphOwner().addCappingGroups(fragSpace);
        
        return done;
    }
    
//------------------------------------------------------------------------------

    /**
     * Tries to add a fused ring using a pair of free APs, one of which on the 
     * given vertex.
     * @param vertex the vertex on which we start to close a the ring.
     * @param mnt monitoring tool to keep count of events.
     * @param force use <code>true</code> to by-pass random decisions and force
     * the creation of rings.
     * @param fragSpace the fragment space controlling how we put together
     * building blocks.
     * @param settings the GA settings controlling how we work.
     * @return <code>true</code> when at least one ring was added to the graph.
     * @throws DENOPTIMException
     * 
     */
    
    protected static boolean addFusedRing(Vertex vertex, Monitor mnt, 
            boolean force, FragmentSpace fragSpace, GAParameters settings) 
                    throws DENOPTIMException
    {
        // get settings //TODO: this should happen inside RunTimeParameters
        RingClosureParameters rcParams = new RingClosureParameters();
        if (settings.containsParameters(ParametersType.RC_PARAMS))
        {
            rcParams = (RingClosureParameters)settings.getParameters(
                    ParametersType.RC_PARAMS);
        }
        
        Randomizer rng = settings.getRandomizer();
        
        // First of all we remove capping groups in the graph
        vertex.getGraphOwner().removeCappingGroups();
        
        List<AttachmentPoint> freeAPs = vertex.getFreeAPThroughout();
        if (freeAPs.size()==0)
        {
            mnt.increase(CounterID.FAILEDMUTATTEMTS_PERFORM_NOADDFUSEDRING_NOFREEAP);
            return false;
        }
        
        DGraph graph = vertex.getGraphOwner();
        
        // Define where to add a bridge. Multiple sites are the result of
        // symmetry, so they all correspond to the same kind of operation
        // performed on symmetry-related sites
        List<List<RelatedAPPair>> candidatePairsSets = 
                EAUtils.searchRingFusionSites(
                    graph, 
                    fragSpace,
                    rcParams,
                    rng.nextBoolean(settings.getSymmetryProbability()),
                    settings.getLogger(), rng);
        if (candidatePairsSets.size()==0)
        {
            mnt.increase(CounterID.FAILEDMUTATTEMTS_PERFORM_NOADDFUSEDRING_NOSITE);
            return false;
        }
        List<RelatedAPPair> chosenPairsSet = rng.randomlyChooseOne(
                candidatePairsSets);

        //TODO-gg add filter by ring size bias
        
        //TODO-gg
        System.out.println("chosenPairsSet: "+chosenPairsSet);
        
        
        // Based on the chosen pair, decide on the number of electrons to use
        // in the incoming fragment that will be used to close the ring
        // the pairs are all the same kind of ring fusion, so just take the 1st
        String elsInHalfFrag = chosenPairsSet.get(0).propID.substring(0,1);
        if (elsInHalfFrag.matches("[a-zA-Z]"))
            elsInHalfFrag = "0";
        String elInIncomingFrag = "0el";
        switch (elsInHalfFrag)
        {
            case "0":
            case "1":
                // no aromaticity to be retained: use non-aromatic chords
                elInIncomingFrag = "0el";
                break;
                
            case "2":
                // Formally we can use any fragment delivering 4n electrons.
                // Effectively, we have only 4-el fragments
                elInIncomingFrag = "4el";
                break;
                
            case "3":
                // We could use a 5-el fragment, but the 8-el aromatic system is
                // so rare that we avoid it. So, we can only use 3-el fragment 
                elInIncomingFrag = "3el";
                break;
                
            case "4":
                // Effectively can only use 2-el fragments
                elInIncomingFrag = "2el";
                break;
                
            case "5":
                // We could use a 3-el fragment, but the 8-el aromatic system is
                // so rare that we avoid it. This could become a tunable config
                elInIncomingFrag = "1el";
                break;
            default:
                throw new Error("Unknown number if electrons in fragment to be "
                        + "used for ring fusion operation.");
        }
        
        // Collect fragment that can be used as ring-closing bridge based on the
        // number of aromatic electrons and number of atoms
        BridgeHeadFindingRule bhfr = (BridgeHeadFindingRule) 
                chosenPairsSet.get(0).property;
        List<Vertex> usableBridges = EAUtils.getUsableAromaticBridges(
                elInIncomingFrag,
                bhfr.getAllowedBridgeLength(),
                fragSpace);
        
        if (usableBridges.size()==0)
        {
            mnt.increase(CounterID.FAILEDMUTATTEMTS_PERFORM_NOADDFUSEDRING_NOBRIDGE);
            return false;
        }
        Vertex incomingVertex = rng.randomlyChooseOne(usableBridges);
        
        // Decide which aps on the bridge are used as head/tail
        List<AttachmentPoint> apsInFusion = new ArrayList<AttachmentPoint>();
        apsInFusion.addAll(incomingVertex.getVerticesWithAPClassStartingWith(
                elInIncomingFrag));
        int[] idApOnBridge = new int[2];
        if (rng.nextBoolean())
        {
            idApOnBridge[0] = apsInFusion.get(0).getIndexInOwner();
            idApOnBridge[1] = apsInFusion.get(1).getIndexInOwner();
        } else {
            idApOnBridge[0] = apsInFusion.get(1).getIndexInOwner();
            idApOnBridge[1] = apsInFusion.get(0).getIndexInOwner();
        }
        
        boolean done = false;
        for (RelatedAPPair pairOfAPs : chosenPairsSet)
        {
            AttachmentPoint apHead = pairOfAPs.apA;
            AttachmentPoint apTail = pairOfAPs.apB;
            
            Vertex bridgeClone = incomingVertex.clone();
            bridgeClone.setVertexId(graph.getMaxVertexId()+1);
            
            graph.appendVertexOnAP(apHead, bridgeClone.getAP(idApOnBridge[0]));
            
            Vertex rcvBridge = fragSpace.getPolarizedRCV(true);
            graph.appendVertexOnAP(bridgeClone.getAP(idApOnBridge[1]),
                    rcvBridge.getAP(0));
            
            Vertex rcvTail = fragSpace.getPolarizedRCV(false);
            graph.appendVertexOnAP(apTail, rcvTail.getAP(0));
            graph.addRing(rcvBridge, rcvTail);
            done = true;
        }
        
        // Restore capping groups
        vertex.getGraphOwner().addCappingGroups(fragSpace);
        
        return done;
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
     
    protected static boolean extendGraph(Vertex curVertex, 
                                         boolean extend, 
                                         boolean symmetryOnAps,
                                         GAParameters settings)
                                                 throws DENOPTIMException
    {
        return extendGraph(curVertex, extend, symmetryOnAps, false, -1, -1,
                settings);
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
     * @param maxHeavyAtoms maximum number of heavy atoms.
     * @throws DENOPTIMException
     * @return <code>true</code> if the graph has been modified
     */

    protected static boolean extendGraph(Vertex curVrtx, 
                                         boolean extend, 
                                         boolean symmetryOnAps,
                                         boolean force,
                                         int chosenVrtxIdx,
                                         int chosenApId,
                                         GAParameters settings) 
                                                        throws DENOPTIMException
    {  
        // get settings //TODO: this should happen inside RunTimeParameters
        RingClosureParameters rcParams = new RingClosureParameters();
        if (settings.containsParameters(ParametersType.RC_PARAMS))
        {
            rcParams = (RingClosureParameters)settings.getParameters(
                    ParametersType.RC_PARAMS);
        }
        FragmentSpaceParameters fsParams = new FragmentSpaceParameters();
        if (settings.containsParameters(ParametersType.FS_PARAMS))
        {
            fsParams = (FragmentSpaceParameters)settings.getParameters(
                    ParametersType.FS_PARAMS);
        }
        int maxHeavyAtoms = fsParams.getMaxHeavyAtom();
        
        // return true if the append has been successful
        boolean status = false;

        // check if the fragment has available APs
        if (!curVrtx.hasFreeAP())
        {
            return status;
        }
        
        DGraph molGraph = curVrtx.getGraphOwner();
        int lvl = molGraph.getLevel(curVrtx);

        ArrayList<Long> addedVertices = new ArrayList<>();

        List<AttachmentPoint> lstDaps = curVrtx.getAttachmentPoints();
        List<AttachmentPoint> toDoAPs = new ArrayList<AttachmentPoint>();
        toDoAPs.addAll(lstDaps);
        for (int i=0; i<lstDaps.size(); i++)
        {
            // WARNING: randomization decouples 'i' from the index of the AP
            // in the vertex's list of APs! So 'i' is just the i-th attempt on
            // the curVertex.
            
            AttachmentPoint ap = settings.getRandomizer().randomlyChooseOne(
                    toDoAPs);
            toDoAPs.remove(ap);
            int apId = ap.getIndexInOwner();
            
            // is it possible to extend on this AP?
            if (!ap.isAvailable())
            {
                continue;
            }

            // NB: this is done also in addRing()
            // Ring closure does not change the size of the molecule, so we
            // give it an extra chance to occur irrespectively on molecular size
            // limit, while still subject of crowdedness probability.
            boolean allowOnlyRingClosure = false;
            if (!force)
            {
                // Decide whether we want to extend the graph at this AP?
                // Note that depending on the criterion (level/molSize) one
                // of these two first factors is 1.0.
                double molSizeProb = EAUtils.getMolSizeProbability(molGraph, 
                        settings);
                double byLevelProb = EAUtils.getGrowthByLevelProbability(lvl,
                        settings);
                double crowdingProb = EAUtils.getCrowdingProbability(ap,
                        settings);
                double extendGraphProb = molSizeProb * byLevelProb * crowdingProb;
                boolean fgrow = settings.getRandomizer().nextBoolean(
                        extendGraphProb);
                if (!fgrow)
                {
                    if (rcParams.allowRingClosures() 
                            && settings.getRandomizer().nextBoolean(byLevelProb 
                                    * crowdingProb))
                    {
                        allowOnlyRingClosure = true;
                    } else {
                        continue;
                    }
                }
            }

            // Apply closability bias in selection of next fragment
            if (!allowOnlyRingClosure 
                    && rcParams.allowRingClosures() 
                    && rcParams.selectFragmentsFromClosableChains())
            {
                boolean successful = attachFragmentInClosableChain(curVrtx,
                        apId, molGraph, addedVertices, settings);
                if (successful)
                {
                    continue;
                }
            }
            
            // find a compatible combination of fragment and AP
            IdFragmentAndAP chosenFrgAndAp = null;
            if (allowOnlyRingClosure)
            {
                // NB: this works only for RCVs that are in the BBSpace, does 
                // not generate a default RCV on-the-fly. So if no RCV is found
                // we'll get a pointer to nothing, which is what we check in the 
                // next IF-block.
                chosenFrgAndAp = getRCVForSrcAp(curVrtx, apId, 
                        fsParams.getFragmentSpace());
            } else {
                chosenFrgAndAp = getFrgApForSrcAp(curVrtx, 
                    apId, chosenVrtxIdx, chosenApId, fsParams.getFragmentSpace());
            }
            int fid = chosenFrgAndAp.getVertexMolId();
            if (fid == -1)
            {
                continue;
            }
            
            // Get the vertex that we'll add to the graph
            Vertex incomingVertex = Vertex.newVertexFromLibrary(-1, 
                            chosenFrgAndAp.getVertexMolId(), 
                            BBType.FRAGMENT, 
                            fsParams.getFragmentSpace());
            
            // Stop if graph is already too big
            if ((curVrtx.getGraphOwner().getHeavyAtomsCount() + 
                    incomingVertex.getHeavyAtomsCount()) > maxHeavyAtoms)
            {
                continue;
            }

            // Decide on symmetric substitution within this vertex...
            boolean cpOnSymAPs = applySymmetry(
                    fsParams.getFragmentSpace().imposeSymmetryOnAPsOfClass(
                            ap.getAPClass()),
                    settings.getSymmetryProbability(),
                    fsParams.getRandomizer());
            SymmetricAPs symAPs = new SymmetricAPs();
            if (curVrtx.getSymmetricAPs(ap).size()!=0
                    && (cpOnSymAPs || symmetryOnAps)
                    && !allowOnlyRingClosure)
            {
                symAPs.addAll(curVrtx.getSymmetricAPs(ap));
                
                // Are symmetric APs rooted on same atom?
                boolean allOnSameSrc = true;
                for (AttachmentPoint symAP : symAPs)
                {
                    if (!symAP.hasSameSrcAtom(ap))
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
                    
                    SymmetricAPs toKeep = new SymmetricAPs();
                    
                    // Start by keeping "ap"
                    toKeep.add(ap);
                    crowdedness = crowdedness + 1;
                    
                    // Pick the accepted value once (used to decide how much
                    // crowdedness we accept)
                    double shot = settings.getRandomizer().nextDouble();
                    
                    // Keep as many as allowed by the crowdedness decision
                    for (AttachmentPoint symAP : symAPs)
                    {
                        if (symAP == ap)
                            continue;
                        
                        double crowdProb = EAUtils.getCrowdingProbability(
                                crowdedness, settings);
                        
                        if (shot > crowdProb)
                            break;
                        
                        toKeep.add(symAP);
                        crowdedness = crowdedness + 1;
                    }
                    
                    // Adjust the list of symmetric APs to work with
                    symAPs = toKeep;
                }
            } else {
                symAPs = new SymmetricAPs();
                symAPs.add(ap);
            }

            // ...and inherit symmetry from previous levels
            boolean cpOnSymVrts = molGraph.hasSymmetryInvolvingVertex(curVrtx);
            SymmetricVertexes symVerts = new SymmetricVertexes();
            if (cpOnSymVrts)
            {
                symVerts = molGraph.getSymSetForVertex(curVrtx);
            }
            else
            {
                symVerts.add(curVrtx);
            }
            
            // Consider size after application of symmetry
            if ((curVrtx.getGraphOwner().getHeavyAtomsCount() + 
                    incomingVertex.getHeavyAtomsCount() * symVerts.size() 
                    * symAPs.size()) > maxHeavyAtoms)
            {
                continue;
            }
            
            // Collects all sym APs: within the vertex and outside it 
            List<AttachmentPoint> allAPsFromSymVerts = new ArrayList<>();
            for (Vertex symVrt : symVerts)
            {
                for (AttachmentPoint apOnVrt : symAPs)
                {
                    AttachmentPoint apOnSymVrt = symVrt.getAP(
                            apOnVrt.getIndexInOwner());
                    // This check is most often not needed, but it prevents that
                    // misleading symmetry relations are used to break APClass
                    // compatibility constraints
                    if (apOnVrt.sameAs(apOnSymVrt)
                            // Also ignore previously found APs
                            && !symAPs.contains(apOnSymVrt))
                    {
                        allAPsFromSymVerts.add(apOnSymVrt);
                    }
                }
            }
            symAPs.addAll(allAPsFromSymVerts);
            
            GraphUtils.ensureVertexIDConsistency(molGraph.getMaxVertexId());

            // loop on all symmetric vertices, but can be only one.
            SymmetricVertexes newSymSetOfVertices = new SymmetricVertexes();
            for (AttachmentPoint symAP : symAPs)
            {
                if (!symAP.isAvailable())
                {
                    continue;
                }

                // Finally add the fragment on a symmetric AP
                long newVrtId = GraphUtils.getUniqueVertexIndex();
                Vertex fragVertex = Vertex.newVertexFromLibrary(newVrtId, 
                                chosenFrgAndAp.getVertexMolId(), 
                                BBType.FRAGMENT,
                                fsParams.getFragmentSpace());
                AttachmentPoint trgAP = fragVertex.getAP(
                        chosenFrgAndAp.getApId());
                
                molGraph.appendVertexOnAP(symAP, trgAP);
                
                addedVertices.add(newVrtId);
                newSymSetOfVertices.add(fragVertex);
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
                long vid = addedVertices.get(i);
                Vertex v = molGraph.getVertexWithId(vid);
                extendGraph(v, extend, symmetryOnAps, settings);
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

    protected static IdFragmentAndAP getFrgApForSrcAp(Vertex curVertex,
            int dapidx, FragmentSpace fragSpace) throws DENOPTIMException
    {
        return getFrgApForSrcAp(curVertex, dapidx, -1, -1, fragSpace);
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

    protected static IdFragmentAndAP getFrgApForSrcAp(Vertex curVertex, 
            int dapidx, int chosenVrtxIdx, int chosenApId, 
            FragmentSpace fragSpace) throws DENOPTIMException
    {
        List<AttachmentPoint> lstDaps = curVertex.getAttachmentPoints();
        AttachmentPoint curDap = lstDaps.get(dapidx);

        // Initialize with an empty pointer
        IdFragmentAndAP res = new IdFragmentAndAP(-1, -1, BBType.FRAGMENT, -1, 
                -1, -1);
        if (!fragSpace.useAPclassBasedApproach())
        {
            int fid = fragSpace.getRandomizer().nextInt(
                    fragSpace.getFragmentLibrary().size());
            res = new IdFragmentAndAP(-1,fid,BBType.FRAGMENT,-1,-1,-1);
        }
        else
        {
            List<IdFragmentAndAP> candidates = 
                    fragSpace.getFragAPsCompatibleWithClass(
                    curDap.getAPClass());
            if (candidates.size() > 0)
            {
                if (chosenVrtxIdx>-1 && chosenApId>-1)
                {
                    // We have asked to force the selection
                    res = new IdFragmentAndAP(-1,chosenVrtxIdx,BBType.FRAGMENT,
                            chosenApId,-1,-1);
                } else {
                    res = fragSpace.getRandomizer().randomlyChooseOne(candidates);
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
     * @param fragSpace the space of building blocks with all the settings.
     * @return the vector of indeces identifying the molId (fragment index) 
     * of a fragment with a compatible attachment point, and the index of such 
     * attachment point.
     * @throws DENOPTIMException
     */

    protected static IdFragmentAndAP getRCVForSrcAp(Vertex curVertex, 
            int dapidx, FragmentSpace fragSpace) throws DENOPTIMException
    {
        AttachmentPoint ap = curVertex.getAP(dapidx);
        
        Randomizer rng = fragSpace.getRandomizer();
        List<Vertex> rcvs = fragSpace.getRCVs();
        Vertex chosen = null;
        if (!fragSpace.useAPclassBasedApproach())
        {
            chosen = rng.randomlyChooseOne(rcvs);
        } else {
            List<Vertex> candidates = fragSpace.getRCVsForAPClass(
                    ap.getAPClass());
            if (candidates.size() > 0)
            {
                chosen = rng.randomlyChooseOne(candidates);
            }
        }
        
        IdFragmentAndAP pointer = new IdFragmentAndAP();
        if (chosen!=null)
            pointer = new IdFragmentAndAP(-1, chosen.getBuildingBlockId(),
                chosen.getBuildingBlockType(), 0, -1, -1);
        return pointer;
    }
    
//------------------------------------------------------------------------------

    protected static boolean attachFragmentInClosableChain(
            Vertex curVertex, int dapidx, DGraph molGraph,
            ArrayList<Long> addedVertices, GAParameters settings)
                    throws DENOPTIMException
    {
        boolean res = false;
        FragmentSpaceParameters fsParams = new FragmentSpaceParameters();
        if (settings.containsParameters(ParametersType.FS_PARAMS))
        {
            fsParams = (FragmentSpaceParameters)settings.getParameters(
                    ParametersType.FS_PARAMS);
        }

        // Get candidate fragments
        ArrayList<FragForClosabChains> lscFfCc = getFragmentForClosableChain(
                curVertex, dapidx, molGraph);

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
            int chosenId = settings.getRandomizer().nextInt(numCands);
            FragForClosabChains chosenFfCc = lscFfCc.get(chosenId);
            ArrayList<Integer> newFragIds = chosenFfCc.getFragIDs();
            int molIdNewFrag = newFragIds.get(0);
            BBType typeNewFrag = BBType.parseInt(newFragIds.get(1));
            int dapNewFrag = newFragIds.get(2);
            if (molIdNewFrag != -1)
            {
                long newvid = GraphUtils.getUniqueVertexIndex();
                Vertex newVrtx = Vertex.newVertexFromLibrary(
                        newvid, molIdNewFrag, typeNewFrag, 
                        fsParams.getFragmentSpace());
                
                molGraph.appendVertexOnAP(curVertex.getAP(dapidx), 
                        newVrtx.getAP(dapNewFrag));
                
                if (newvid != -1)
                {
                    addedVertices.add(newvid);
                    // update list of candidate closable chains
                    molGraph.getClosableChains().removeAll(
                            chosenFfCc.getIncompatibleCC());
                    APClass apc = curVertex.getAttachmentPoints().get(
                            dapidx).getAPClass();
                    if (applySymmetry(
                            fsParams.getFragmentSpace().imposeSymmetryOnAPsOfClass(apc),
                            settings.getSymmetryProbability(),
                            fsParams.getRandomizer()))
                    {
//TODO: implement symmetric substitution with closability bias
                    }
                    res = true;
                }
                else
                {
                    String msg = "BUG: Incorrect vertex num. Contact author.";
                    settings.getLogger().log(Level.SEVERE, msg);
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
                                                       Vertex curVertex,
                                                       int dapidx,
                                                       DGraph molGraph)
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
                        nfid = nextChainLink.getIdx();
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
                        nfid = nextChainLink.getIdx();
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
            Vertex parent = molGraph.getParent(curVertex);
            Edge edge = molGraph.getEdgeWithParent(
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
                    int pLnkId = parentLink.getIdx();
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
                            nfid = nextChainLink.getIdx();
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
     * be ensured by running {@link DGraph#renumberGraphVertices()}.
     * @param site the definition of the crossover site.
     * @throws DENOPTIMException
     */
    public static boolean performCrossover(XoverSite site, 
            FragmentSpace fragSpace) throws DENOPTIMException
    {          
        DGraph gA = site.getA().get(0).getGraphOwner();
        DGraph gB = site.getB().get(0).getGraphOwner();
        
        // All the APs that point away from the subgraph
        List<AttachmentPoint> allAPsOnA = gA.getSubgraphAPs(site.getA());
        List<AttachmentPoint> allAPsOnB = gB.getSubgraphAPs(site.getB());
        
        // The APs that are required to have a mapping for proper crossover,
        // eg. because the change of subgraph needs to retain a super structure.
        List<AttachmentPoint> needyAPsOnA = site.getAPsNeedingMappingA();
        List<AttachmentPoint> needyAPsOnB = site.getAPsNeedingMappingB();
        
        // APs that connects the subgraphs' root to the parent vertex
        AttachmentPoint apToParentA = null;
        AttachmentPoint apToParentB = null;
        for (AttachmentPoint ap : needyAPsOnA)
        {
            if (!ap.isSrcInUserThroughout())
            {
                apToParentA = ap;
                //WARNING: we assume there is one AND only one AP to parent!!!
                break;
            }
        }
        for (AttachmentPoint ap : needyAPsOnB)
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
        APMapFinder apmf = new APMapFinder(fragSpace, 
                allAPsOnA, needyAPsOnA, 
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
        DGraph subGraphA = gA.extractSubgraph(site.getA());
        DGraph subGraphB = gB.extractSubgraph(site.getB());
        
        // Here we create the two AP mappings we need: one for A other for B.
        LinkedHashMap<AttachmentPoint,AttachmentPoint> 
        apMapA = new LinkedHashMap<AttachmentPoint,AttachmentPoint>();
        LinkedHashMap<AttachmentPoint,AttachmentPoint> 
        apMapB = new LinkedHashMap<AttachmentPoint,AttachmentPoint>();
        for (Map.Entry<AttachmentPoint, AttachmentPoint> e : 
            apmf.getChosenAPMapping().entrySet())
        {
            AttachmentPoint apOnA = e.getKey();
            AttachmentPoint apOnB = e.getValue();

            // NB: assumption that vertex IDs are healthy, AND that order of APs
            // is retained upon cloning of the subgraph!
            AttachmentPoint apOnSubGraphA = subGraphA.getVertexWithId(
                    apOnA.getOwner().getVertexId()).getAP(
                            apOnA.getIndexInOwner());
            AttachmentPoint apOnSubGraphB = subGraphB.getVertexWithId(
                    apOnB.getOwner().getVertexId()).getAP(
                            apOnB.getIndexInOwner());
            apMapA.put(apOnA, apOnSubGraphB);
            apMapB.put(apOnB, apOnSubGraphA);
        }
 
        // Now we do the actual replacement of subgraphs
        if (!gA.replaceSubGraph(site.getA(), subGraphB, apMapA, fragSpace))
           return false;
        if (!gB.replaceSubGraph(site.getB(), subGraphA, apMapB, fragSpace))
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
     * @return <code>true</code> if symmetry is to be applied
     */
    protected static boolean applySymmetry(boolean apclassImposed, 
            double symmetryProbability, Randomizer randomizer)
    {
        boolean r = false;
        if (apclassImposed)
        {
            r = true;
        }
        else
        {
            r = randomizer.nextBoolean(symmetryProbability);
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
    public static boolean performMutation(DGraph graph, Monitor mnt, 
            GAParameters settings) throws DENOPTIMException
    {  
        // Get vertices that can be mutated: they can be part of subgraphs
        // embedded in templates. Here, we consider only single-vertexes sites.
        // So sites for ADDRINGFUSION mutation are not added here.
        List<Vertex> mutable = graph.getMutableSites(
                settings.getExcludedMutationTypes());
        // Now, add also the sites that involve multiple vertexes, such sites for
        // ADDFUSEDRING mutation.
        for (List<RelatedAPPair> siteCombination : EAUtils.searchRingFusionSites(
                graph, settings))
        {
            for (RelatedAPPair site : siteCombination)
            {
                Vertex vA = site.apA.getOwner();
                Vertex vB = site.apB.getOwner();
                if (!mutable.contains(vA))
                    mutable.add(vA);
                if (!mutable.contains(vB))
                    mutable.add(vB);
            }
        }
        
        if (mutable.size() == 0)
        {
            mnt.increase(CounterID.FAILEDMUTATTEMTS_PERFORM_NOMUTSITE);
            String msg = "Graph has no mutable site. Mutation aborted.";
            settings.getLogger().info(msg);
            return false;
        }
        boolean doneMutation = true;
        int numberOfMutations = EAUtils.chooseNumberOfSitesToMutate(
                settings.getMultiSiteMutationWeights(), 
                settings.getRandomizer().nextDouble());
        for (int i=0; i<numberOfMutations; i++)
        {
            if (i>0)
            {
                mutable = graph.getMutableSites(
                        settings.getExcludedMutationTypes());
                break;
            }
            Vertex v = settings.getRandomizer().randomlyChooseOne(mutable);
            doneMutation = performMutation(v, mnt, settings);
            if(!doneMutation)
                break;
        }
        return doneMutation;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Tries to do mutate the given vertex. We assume the vertex belongs to a 
     * graph, if not no mutation is done. The mutation type is
     * chosen randomly according to the possibilities declared by the vertex.
     * The graph that owns the vertex will be altered and
     * the original structure and content of the graph will be lost.
     * @param vertex the vertex to mutate.
     * @param mnt the monitor keeping track of what happens in EA operations.
     * This does not affect the mutation. It only measures how many attempts and
     * failures occur.
     * @return <code>true</code> if the mutation is successful.
     * @throws DENOPTIMException
     */
    public static boolean performMutation(Vertex vertex, Monitor mnt, 
            GAParameters settings) throws DENOPTIMException
    {
        List<MutationType> mTypes = vertex.getMutationTypes(
                settings.getExcludedMutationTypes());
        if (mTypes.size() == 0)
        {
            return false;
        }
        MutationType mType = settings.getRandomizer().randomlyChooseOne(mTypes);
        return performMutation(vertex, mType, mnt, settings);
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
    public static boolean performMutation(Vertex vertex, 
            MutationType mType, Monitor mnt, GAParameters settings) 
                    throws DENOPTIMException
    {
        DGraph c = vertex.getGraphOwner().clone();
        int pos = vertex.getGraphOwner().indexOf(vertex);
        try
        {
            return performMutation(vertex, mType, false, -1 ,-1, mnt, settings);
        } catch (IllegalArgumentException|NullPointerException e)
        {
            String debugFile = "failedMutation_" + mType 
                    + "_" + vertex.getVertexId() + "(" + pos + ")_"
                    + settings.timeStamp + ".sdf";
            DenoptimIO.writeGraphToSDF(new File(debugFile), c, false,
                    settings.getLogger(), settings.getRandomizer());
            settings.getLogger().warning("Fatal exception while performing "
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
    public static boolean performMutation(Vertex vertex, 
            MutationType mType, boolean force, int chosenVrtxIdx, 
            int chosenApId, Monitor mnt, GAParameters settings) 
                    throws DENOPTIMException
    {
        FragmentSpaceParameters fsParams = new FragmentSpaceParameters();
        if (settings.containsParameters(ParametersType.FS_PARAMS))
        {
            fsParams = (FragmentSpaceParameters)settings.getParameters(
                    ParametersType.FS_PARAMS);
        }
        
        DGraph graph = vertex.getGraphOwner();
        if (graph == null)
        {
            mnt.increase(CounterID.FAILEDMUTATTEMTS_PERFORM_NOOWNER);
            settings.getLogger().info("Vertex has no owner - "
                    + "Mutation aborted");
            return false;
        }
        if (!vertex.getMutationTypes(settings.getExcludedMutationTypes())
                .contains(mType))
        {
            mnt.increase(CounterID.FAILEDMUTATTEMTS_PERFORM_BADMUTTYPE);
            settings.getLogger().info("Vertex does not allow mutation type "
                    + "'" + mType + "' - Mutation aborted");
            return false;
        }
        
        int graphId = graph.getGraphId();
        int positionOfVertex = graph.indexOf(vertex);
        //NB: since we have renumbered the vertexes, we use the old vertex ID
        // when reporting what vertex is being mutated. Also, note that the
        // identity of the candidate is already in the graph's local msg.
        String mutantOrigin = graph.getLocalMsg() + "|"
                + mType + "|"
                + vertex.getProperty(DENOPTIMConstants.STOREDVID) 
                + " (" + positionOfVertex + ")";
        graph.setLocalMsg(mutantOrigin);
        
        boolean done = false;
        switch (mType) 
        {
            case CHANGEBRANCH:
                done = rebuildBranch(vertex, force, chosenVrtxIdx, chosenApId, 
                        settings);
                if (!done)
                    mnt.increase(
                            CounterID.FAILEDMUTATTEMTS_PERFORM_NOCHANGEBRANCH);
                break;
                
            case CHANGELINK:
                done = substituteLink(vertex, chosenVrtxIdx, mnt, 
                        fsParams.getFragmentSpace());
                if (!done)
                    mnt.increase(
                            CounterID.FAILEDMUTATTEMTS_PERFORM_NOCHANGELINK);
                break;
                
            case DELETELINK:
                done = deleteLink(vertex, chosenApId, mnt, 
                        fsParams.getFragmentSpace());
                if (!done)
                    mnt.increase(
                            CounterID.FAILEDMUTATTEMTS_PERFORM_NOCHANGELINK);
                break;
                
            case DELETECHAIN:
                done = deleteChain(vertex, mnt, fsParams.getFragmentSpace());
                if (!done)
                    mnt.increase(
                            CounterID.FAILEDMUTATTEMTS_PERFORM_NODELETECHAIN);
                break;
                
            case ADDLINK:
                if (chosenApId < 0)
                {
                    List<Integer> candidates = new ArrayList<Integer>();
                    for (Vertex c : vertex.getChilddren())
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
                    chosenApId = settings.getRandomizer().randomlyChooseOne(
                            candidates);
                }
                done = extendLink(vertex, chosenApId, chosenVrtxIdx, mnt,
                        fsParams.getFragmentSpace());
                if (!done)
                    mnt.increase(
                            CounterID.FAILEDMUTATTEMTS_PERFORM_NOADDLINK);
                break;
                
            case ADDRING:
                done = addRing(vertex, mnt, false, fsParams.getFragmentSpace(), 
                        settings);
                if (!done)
                    mnt.increase(CounterID.FAILEDMUTATTEMTS_PERFORM_NOADDRING);
                break;
                
            case ADDFUSEDRING:
                done = addFusedRing(vertex, mnt, false, fsParams.getFragmentSpace(), 
                        settings);
                if (!done)
                    mnt.increase(CounterID.FAILEDMUTATTEMTS_PERFORM_NOADDRING);
                break;
                
            case EXTEND:
                vertex.getGraphOwner().removeCappingGroupsOn(vertex);
                done = extendGraph(vertex, false, false, force, chosenVrtxIdx, 
                        chosenApId, settings);
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
        settings.getLogger().info(msg);

        return done;
    }

//------------------------------------------------------------------------------



}
