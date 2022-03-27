/*
 *   DENOPTIM
 *   Copyright (C) 2021  Marco Foscato <marco.foscato@uib.no>
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.graph.APMapping;
import denoptim.graph.Candidate;
import denoptim.graph.DENOPTIMAttachmentPoint;
import denoptim.graph.DENOPTIMGraph;
import denoptim.graph.DENOPTIMTemplate.ContractLevel;
import denoptim.graph.DENOPTIMVertex;
import denoptim.io.DenoptimIO;
import denoptim.rings.PathSubGraph;
import denoptim.utils.RandomUtils;

/**
 * A collection of candidates. To speed-up operations such as the selection of
 * parents for crossover, this class holds also compatibility relations between 
 * candidates. The latter are relevant when APClass compatibility rules are
 * in use. Therefore, if {@link FragmentSpace.useAPclassBasedApproach()} returns
 * <code>false</code> the population is just a list of candidates.
 * 
 * @author Marco Foscato
 */

public class Population extends ArrayList<Candidate> implements Cloneable
{

    /**
     * Version UID
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * An integer that changes every time we change the population.
     */
    private AtomicInteger populationUpdate = new AtomicInteger();
    
    /**
     * Crossover compatibility between members
     */
    private XoverSitesAmongCandidates xoverCompatibilities;
   
//------------------------------------------------------------------------------

    public Population()
    {
        super();
        if (FragmentSpace.useAPclassBasedApproach())
        {
            xoverCompatibilities = new XoverSitesAmongCandidates();
        }
    }
    
//------------------------------------------------------------------------------
    
    @Override
    public boolean add(Candidate c)
    {
        boolean result = super.add(c);
        if (result)
            populationUpdate.getAndIncrement();
        return result;
    }
    
//------------------------------------------------------------------------------
    
    @Override
    public void add(int index, Candidate c)
    {
        super.add(index, c);
        populationUpdate.getAndIncrement();
    }
    
//------------------------------------------------------------------------------
    
    @Override
    public Candidate set(int index, Candidate c)
    {
        populationUpdate.getAndIncrement();
        return super.set(index, c);
    }
    
//------------------------------------------------------------------------------
    
    @Override
    public Candidate remove(int index)
    {
        populationUpdate.getAndIncrement();
        return super.remove(index);
    }
    
//------------------------------------------------------------------------------
    
    @Override
    public boolean remove(Object c)
    {
        boolean result = super.remove(c);
        if (result)
            populationUpdate.getAndIncrement();
        return result;
    }
    
//------------------------------------------------------------------------------
    
    @Override
    public boolean removeAll(Collection<?> c)
    {
        boolean result = super.removeAll(c);
        if (result)
            populationUpdate.getAndIncrement();
        return result;
    }
    
//------------------------------------------------------------------------------
    
    @Override
    public boolean retainAll(Collection<?> c)
    {
        boolean result = super.retainAll(c);
        if (result)
            populationUpdate.getAndIncrement();
        return result;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns an integer that represent the current status of the population.
     * Additions, removal or change of a population member triggers change of 
     * the returned value. The integer is a good way to check for population
     * changes without looking at the actual population content.
     * @return an integer representing the version of the population.
     */
    public int getVersionID()
    {
        return populationUpdate.get();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Does not clone the cross-over compatibility relations between each pairs
     * of population members.
     */
    public Population clone()
    {
        Population clone = new Population();

        for (Candidate c : this)
        {
            clone.add(c);
        }
        
        clone.xoverCompatibilities = xoverCompatibilities.clone();
        
        return clone;
    }
    
//------------------------------------------------------------------------------    
    
    /**
     * A data structure collecting crossover-compatible sites. This class wants
     * to collect information like 
     * "these two candidates cannot do crossover" (i.e., non compatible) or 
     * "they can do crossover (i.e., compatible) 
     * and here is the list of crossover sites".
     * This data structure user a {@link LinkedHashMap} to ensure
     * reproducibility in the generation of list of keys for the inner map. The
     * order of the keys is given by insertion order.
     */
    private class XoverSitesAmongCandidates
    {
        private LinkedHashMap<Candidate,
        LinkedHashMap<Candidate,List<XoverSite>>> data;
        
        /**
         * Initializes an empty data structure.
         */
        public XoverSitesAmongCandidates()
        {
            data = new LinkedHashMap<Candidate,LinkedHashMap<Candidate, 
                    List<XoverSite>>>();
        }
        
        /**
         * Creates the entry corresponding to the pair of given candidates.
         * @param c1
         * @param c2
         * @param xoversite list of crossover-compatible sites. The order
         * of the vertexes is expected to be consistent to that of the arguments
         * given to this method.
         */
        public void put(Candidate c1, Candidate c2, 
                List<XoverSite> xoversite)
        {     
            if (data.containsKey(c1))
            {
                data.get(c1).put(c2, xoversite);
            } else {
                LinkedHashMap<Candidate,List<XoverSite>> toC1 = 
                        new LinkedHashMap<Candidate,List<XoverSite>>();
                toC1.put(c2, xoversite);
                data.put(c1, toC1);
            }
            
            List<XoverSite> revPairs = 
                    new ArrayList<XoverSite>();
            for (XoverSite pair : xoversite)
            {
                XoverSite revPair = pair.createMirror();
                revPairs.add(revPair);
            }
            
            if (data.containsKey(c2))
            {
                data.get(c2).put(c1, revPairs);
            } else {
                LinkedHashMap<Candidate,List<XoverSite>> toC2 = 
                        new LinkedHashMap<Candidate,List<XoverSite>>();
                toC2.put(c1, revPairs);
                data.put(c2, toC2);
            }
        }
        
        /**
         * Gets the value corresponding to the pair of keys in the given order.
         * @param c1
         * @param c2
         * @return the list of compatible pairs or null.
         */
        public List<XoverSite> get(Candidate c1, Candidate c2)
        {
            if (data.containsKey(c1))
            {
                return data.get(c1).get(c2);
            } else {
                return null;
            }
        }
        
        /**
         * Returns a list of items that has crossover sites with the given item.
         * @param cA the item that is looking for a crossover partner.
         * @return the list of crossover-compatible items.
         */
        public ArrayList<Candidate> getMembersCompatibleWith(Candidate cA)
        {
            ArrayList<Candidate> compatibleMembers = new ArrayList<Candidate>();
            if (data.keySet().contains(cA))
            {
                for (Candidate cB : data.get(cA).keySet())
                {
                    if (!data.get(cA).get(cB).isEmpty())
                    {
                        compatibleMembers.add(cB);
                    }
                }
            }
            return compatibleMembers;
        }

        /**
         * Check is this data structure contains information about the 
         * combination of the given members. The order of the members does not 
         * matter. 
         * @param memberA a member to be searched for.
         * @param memberB a member to be searched for.
         * @return <code>true</code> if this data structure does contain 
         * information about the compatibility between the two members. Note
         * that such information can be "they are not compatible" or 
         * "they are compatible and here is the list of crossover sites".
         */
        public boolean contains(Candidate memberA, Candidate memberB)
        {            
            return data.keySet().contains(memberA) &&
                    data.get(memberA).containsKey(memberB);
        }

        /**
         * removes all references to the specified candidate.
         * @param c the candidate whose references have to be removed.
         */
        public void remove(Candidate c)
        {
            data.remove(c);
            for (LinkedHashMap<Candidate, List<XoverSite>> m : 
                data.values())
            {
                m.remove(c);
            }
        }
        
        /**
         * Return a somewhat-shallow clone of this object: the map and list are 
         * new objects, but the references to candidates and vertexes will point
         * to the original instances.
         */
        public XoverSitesAmongCandidates clone()
        {
            XoverSitesAmongCandidates cloned = new XoverSitesAmongCandidates();
            for (Candidate c1 : data.keySet())
            {
                LinkedHashMap<Candidate, List<XoverSite>> inner =
                        new LinkedHashMap<Candidate, List<XoverSite>>();
                for (Candidate c2 : data.get(c1).keySet())
                {
                    List<XoverSite> lst = new ArrayList<XoverSite>();
                    for (XoverSite arr : data.get(c1).get(c2))
                    {
                        lst.add(arr.clone());
                    }
                    inner.put(c2,lst);
                }
                cloned.data.put(c1, inner);
            }
            return cloned;
        }
    }

//------------------------------------------------------------------------------
    
    /**
     * Returns a list of population members that can do crossover with the 
     * specified member.
     * @param memberA a member that wants to do crossover and searches for
     * a partner.
     * @param the subset of population members we can consider as eligible
     * parents. 
     * @return the list of crossover-compatible population members.
     */
    public ArrayList<Candidate> getXoverPartners(Candidate memberA,
            ArrayList<Candidate> eligibleParents)
    {   
        DENOPTIMGraph gA = memberA.getGraph();
        
        // Update to make sure we cover any combination of members that has not 
        // been considered before
        for (Candidate memberB : eligibleParents)
        {
            if (memberA == memberB)
            {
                continue;
            }
            
            if (xoverCompatibilities.contains(memberA,memberB))
            {
                continue;
            }
    
            DENOPTIMGraph gB = memberB.getGraph();
            
            if (gA.sameAs(gB, new StringBuilder()))
                continue;
                
            List<DENOPTIMVertex[]> xoverSites = DENOPTIMGraphOperations
                    .locateCompatibleXOverPoints(gA, gB);
 
            //TODO-gg this will change once consistency is achieved wrt xoversites format
            ArrayList<XoverSite> sites = new ArrayList<XoverSite>();
            for (DENOPTIMVertex[] pair : xoverSites)
            {
                List<DENOPTIMVertex> sgA = new ArrayList<DENOPTIMVertex>();
                sgA.add(pair[0]);

                List<DENOPTIMVertex> sgB = new ArrayList<DENOPTIMVertex>();
                sgB.add(pair[1]);
                XoverSite xos = new XoverSite(sgA, sgB);
                sites.add(xos);
            }
            
            xoverCompatibilities.put(memberA, memberB, sites);
        }
        
        return xoverCompatibilities.getMembersCompatibleWith(memberA);
    }

//------------------------------------------------------------------------------
    
    /**
     * Returns a list of crossover sites between the two given parents. The 
     * crossover sites are given using the same order used to specify the 
     * parents. This method should always be run after the 
     * {@link Population#getXoverPartners(Candidate, ArrayList)}, which 
     * populated the crossover compatibility data.
     * @param parentA
     * @param parentB
     * @return the list crossover sites.
     */
    public List<XoverSite> getXoverSites(Candidate parentA,
            Candidate parentB)
    {
        return xoverCompatibilities.get(parentA,parentB);
    }
    
//------------------------------------------------------------------------------

    /**
     * For a pair of candidates (i.e., a pair of graphs), and a pair of valid 
     * crossover points, this method tries to identify a pair of subgraph that 
     * can be swapped between the two graphs. To this end it searches for 
     * subgraph end-points, i.e., vertexes where the subgraph starting with the 
     * vertexes given as parameters will end.
     * This method should always be run after the 
     * {@link Population#getXoverPartners(Candidate, ArrayList)}, which 
     * populated the crossover compatibility data.
     * @param maleCandidate 
     * @param femaleCandidate
     * @param maleGraph
     * @param vertxOnMale
     * @param femaleGraph
     * @param vertxOnFemale
     * @return
     */
    public List<List<DENOPTIMVertex>> getSwappableSubGraphEnds(
            Candidate maleCandidate, Candidate femaleCandidate,
            DENOPTIMGraph maleGraph, DENOPTIMVertex vertxOnMale,
            DENOPTIMGraph femaleGraph, DENOPTIMVertex vertxOnFemale)
    {
        return getSwappableSubGraphEnds(maleCandidate, femaleCandidate, 
                maleGraph, vertxOnMale, femaleGraph, vertxOnFemale,
                null);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * For a pair of candidates (i.e., a pair of graphs), and a pair of valid 
     * crossover points, this method tries to identify a pair of subgraph that 
     * can be swapped between the two graphs. To this end, it searches for 
     * subgraph end-points, i.e., vertexes where the subgraph starting with the 
     * vertexes given as parameters will end.
     * This method should always be run after the 
     * {@link Population#getXoverPartners(Candidate, ArrayList)}, which 
     * populated the crossover compatibility data.
     * @param maleCandidate 
     * @param femaleCandidate
     * @param maleGraph the graph where a subgraph should be swapped (male side).
     * This graph owns <code>vertxOnMale</code>.
     * @param vertxOnMale the vertex defining the beginning of the swappable 
     * subgraph (i.e., the crossover point) on the male.
     * @param femaleGraph the graph where a subgraph should be swapped (female 
     * side). This graph owns <code>vertxOnFemale</code>.
     * @param vertxOnFemale  the vertex defining the beginning of the swappable 
     * subgraph (i.e., the crossover point) on the female.
     * @param sequence integers used to by-pass random choices
     * and ensure reproducibility of the results. Used only for testing, 
     * otherwise use <code>null</code>.
     * @return the list of vertexes that, together with those vertexes given as 
     * parameters (<code>vertxOnMale</code>, <code>vertxOnFemale</code>), 
     * define a subgraph that can be swapped.
     */
    
    //TODO-gg remove candidate AND graph as they should be obtained from the vertexes
    
    protected List<List<DENOPTIMVertex>> getSwappableSubGraphEnds(
            Candidate maleCandidate, Candidate femaleCandidate,
            DENOPTIMGraph maleGraph, DENOPTIMVertex vertxOnMale,
            DENOPTIMGraph femaleGraph, DENOPTIMVertex vertxOnFemale,
            int[] sequence)
    {   
        List<DENOPTIMVertex> childTreeM = new ArrayList<DENOPTIMVertex>();
        maleGraph.getChildrenTree(vertxOnMale, childTreeM);
        List<DENOPTIMVertex> childTreeF = new ArrayList<DENOPTIMVertex>();
        femaleGraph.getChildrenTree(vertxOnFemale, childTreeF);
        // The subGraphEndInM/F are supposed to be included
        List<DENOPTIMVertex> subGraphEndInM = new ArrayList<DENOPTIMVertex>();
        List<DENOPTIMVertex> subGraphEndInF = new ArrayList<DENOPTIMVertex>();
        List<DENOPTIMVertex> alreadyIncludedFromM = new ArrayList<DENOPTIMVertex>();
        List<DENOPTIMVertex> alreadyIncludedFromF = new ArrayList<DENOPTIMVertex>();
        //NB: the vertexes are the target sides of edges where we can do xover.
        List<XoverSite> xoverCompatPairs = new ArrayList<XoverSite>();
        xoverCompatPairs.addAll(getXoverSites(maleCandidate, 
                femaleCandidate));
        int initSize = xoverCompatPairs.size();
        for (int iPair=0; iPair<initSize; iPair++)
        {
            XoverSite pair = null;
            if (sequence==null)
            {
                pair = RandomUtils.randomlyChooseOne(xoverCompatPairs);
                xoverCompatPairs.remove(pair);
            } else {
                // This is only to ensure reproducibility in tests
                if (iPair>=sequence.length)
                    break;
                pair = xoverCompatPairs.get(sequence[iPair]);
            }
            
            // Exclude vertexes that are not downstream to the xover site
            DENOPTIMVertex endOnM = pair.getA().get(0);
            DENOPTIMVertex endOnF = pair.getB().get(0);
            if (!childTreeM.contains(endOnM) || !childTreeF.contains(endOnF))
                continue;
            
            // Ignore vertexes that are already part of the subgraph
            if (alreadyIncludedFromM.contains(endOnM)
                    || alreadyIncludedFromF.contains(endOnF))
                continue;
            
            // Exclude combinations that identify a too short path
            PathSubGraph pathM = new PathSubGraph(vertxOnMale, endOnM, maleGraph);
            PathSubGraph pathF = new PathSubGraph(vertxOnFemale, endOnF, femaleGraph);
            if (pathM.getPathLength()<2 || pathF.getPathLength()<2)
                continue;

            // If any partner is a fixed-structure templates...
            if ((maleGraph.getTemplateJacket()!=null 
                    && maleGraph.getTemplateJacket().getContractLevel()
                    == ContractLevel.FIXED_STRUCT)
                    || (femaleGraph.getTemplateJacket()!=null 
                            && femaleGraph.getTemplateJacket().getContractLevel()
                            == ContractLevel.FIXED_STRUCT))
            {
                //...the two paths should have same length.
                if (pathM.getPathLength()!=pathF.getPathLength())
                    continue;
            }
            
            // OK, these vertexes's parents are usable ends of the subgraph
            // to swap
            subGraphEndInM.add(endOnM.getParent());
            subGraphEndInF.add(endOnF.getParent());
            alreadyIncludedFromM.addAll(pathM.getVertecesPath());
            alreadyIncludedFromF.addAll(pathF.getVertecesPath());
        }
        List<List<DENOPTIMVertex>> result = new ArrayList<List<DENOPTIMVertex>>();
        result.add(subGraphEndInM);
        result.add(subGraphEndInF);
        return result;
    }

//------------------------------------------------------------------------------
    
    /**
     * Removes all the elements exceeding the given size.
     * @param populationSize size to trim down to.
     */
    public void trim(int populationSize)
    {
        int k = this.size();
        for (Candidate c : this.subList(GAParameters.getPopulationSize(), k))
        {
            xoverCompatibilities.remove(c);
        }
        this.subList(GAParameters.getPopulationSize(), k).clear();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Gets the minimum value of the fitness in this population.
     * @return the minimum fitness value in this population
     */
    public double getMinFitness()
    {
        return this.stream()
                .min(Comparator.comparing(Candidate::getFitness))
                .orElse(null)
                .getFitness();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Gets the maximum value of the fitness in this population.
     * @return the maximum fitness value in this population
     */
    public double getMaxFitness()
    {
        return this.stream()
                .max(Comparator.comparing(Candidate::getFitness))
                .orElse(null)
                .getFitness();
    }

//------------------------------------------------------------------------------
    
    /**
     * Checks if a given fitness value if within the given percentile of best 
     * candidates.
     * @param value the value of fitness to compare with the population.
     * @param percentile number in 0-1 range defining the desired percentile.
     * @return <code>true</code> is the value is among the best 
     * <i>percentile</i>% values in the current population, i.e., is larger than
     * min + (100-<i>percentile</i>% * (max-min)).
     */
    public boolean isWithinPercentile(double value, double percentile)
    {
        double min = getMinFitness();
        double max = getMaxFitness();
        double threshold = (1.0 - percentile) * (max-min);
        
        if (value > (threshold+min))
            return true;
        else
            return false;
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the candidate with the given name, if present, or null.
     * @param name the name of the candidate to retrieve
     * @return the candidate with the given name, if present, or null.
     */
    public Candidate getCandidateNamed(String name)
    {
        return this.stream()
                .filter(candidate -> name.equals(candidate.getName()))
                .findAny()
                .orElse(null);
    }
    
//------------------------------------------------------------------------------
    
}
