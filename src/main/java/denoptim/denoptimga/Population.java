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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import denoptim.fragspace.FragmentSpace;
import denoptim.graph.Candidate;
import denoptim.graph.DENOPTIMGraph;
import denoptim.graph.DENOPTIMVertex;

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
    private XoverCompatibilitySites xoverCompatibilities;
   
//------------------------------------------------------------------------------

    public Population()
    {
        super();
        if (FragmentSpace.useAPclassBasedApproach())
        {
            xoverCompatibilities = new XoverCompatibilitySites();
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
    private class XoverCompatibilitySites
    {
        private LinkedHashMap<Candidate,
        LinkedHashMap<Candidate,List<DENOPTIMVertex[]>>> data;
        
        /**
         * Initialises an empty data structure.
         */
        public XoverCompatibilitySites()
        {
            data = new LinkedHashMap<Candidate,LinkedHashMap<Candidate, 
                    List<DENOPTIMVertex[]>>>();
        }
        
        /**
         * Creates the entry corresponding to the pair of given candidates.
         * @param c1
         * @param c2
         * @param pairs list of crossover-compatible pairs of vertexes. 
         * The order is
         * of the vertexes is expected to be consistent to that of the arguments
         * given to this method.
         */
        public void put(Candidate c1, Candidate c2, 
                List<DENOPTIMVertex[]> pairs)
        {     
            if (data.containsKey(c1))
            {
                data.get(c1).put(c2, pairs);
            } else {
                LinkedHashMap<Candidate,List<DENOPTIMVertex[]>> toC1 = 
                        new LinkedHashMap<Candidate,List<DENOPTIMVertex[]>>();
                toC1.put(c2, pairs);
                data.put(c1, toC1);
            }
            
            List<DENOPTIMVertex[]> revPairs = 
                    new ArrayList<DENOPTIMVertex[]>();
            for (DENOPTIMVertex[] pair : pairs)
            {
                DENOPTIMVertex[] revPair = pair.clone();
                Collections.reverse(Arrays.asList(revPair));
                revPairs.add(revPair);
            }
            
            if (data.containsKey(c2))
            {
                data.get(c2).put(c1, revPairs);
            } else {
                LinkedHashMap<Candidate,List<DENOPTIMVertex[]>> toC2 = 
                        new LinkedHashMap<Candidate,List<DENOPTIMVertex[]>>();
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
        public List<DENOPTIMVertex[]> get(Candidate c1, Candidate c2)
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
            for (LinkedHashMap<Candidate, List<DENOPTIMVertex[]>> m : 
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
        public XoverCompatibilitySites clone()
        {
            XoverCompatibilitySites cloned = new XoverCompatibilitySites();
            for (Candidate c1 : data.keySet())
            {
                LinkedHashMap<Candidate, List<DENOPTIMVertex[]>> inner =
                        new LinkedHashMap<Candidate, List<DENOPTIMVertex[]>>();
                for (Candidate c2 : data.get(c1).keySet())
                {
                    List<DENOPTIMVertex[]> lst = new ArrayList<DENOPTIMVertex[]>();
                    for (DENOPTIMVertex[] arr : data.get(c1).get(c2))
                    {
                        lst.add(Arrays.copyOf(arr,arr.length));
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
        
        // First, update to cover any combination of members that has not been 
        // considered before
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
   
            xoverCompatibilities.put(memberA, memberB, xoverSites);
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
    public List<DENOPTIMVertex[]> getXoverSites(Candidate parentA,
            Candidate parentB)
    {
        return xoverCompatibilities.get(parentA,parentB);
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
