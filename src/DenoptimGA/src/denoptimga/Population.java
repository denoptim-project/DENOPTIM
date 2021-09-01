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

package denoptimga;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import denoptim.fragspace.FragmentSpace;
import denoptim.molecule.Candidate;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMVertex;

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

    public Population clone()
    {
        Population clone = new Population();

        for (Candidate c : this)
        {
            clone.add(c.clone());
        }
        
        //TODO: clone relations
        
        return clone;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * A data structure collecting crossover-compatible sites. This class wants
     * to collect information like 
     * "these two candidates are cannot do crossover" (i.e., non compatible) or 
     * "they can do crossover (i.e., compatible) 
     * and here is the list of crossover sites".
     * This data structure user a map of sorted maps. This to ensure
     * reproducibility in the generation of list of keys for the inner map.
     */
    private class XoverCompatibilitySites
    {
        private SortedMap<Candidate,
            SortedMap<Candidate,List<DENOPTIMVertex[]>>> data;
        
        /**
         * Initialises an empty data structure.
         */
        public XoverCompatibilitySites()
        {
            data = new TreeMap<Candidate,SortedMap<Candidate, 
                    List<DENOPTIMVertex[]>>>(new CandidatesComparator());
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
                SortedMap<Candidate,List<DENOPTIMVertex[]>> toC1 = 
                        new TreeMap<Candidate,List<DENOPTIMVertex[]>>(
                                new CandidatesComparator());
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
                SortedMap<Candidate,List<DENOPTIMVertex[]>> toC2 = 
                        new TreeMap<Candidate,List<DENOPTIMVertex[]>>(
                                new CandidatesComparator());
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
            for (SortedMap<Candidate, List<DENOPTIMVertex[]>> m : data.values())
            {
                m.remove(c);
            }
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Comparator meant to compare candidates when these are used as maps' keys
     */
    private class CandidatesComparator implements Comparator<Candidate>
    {
        @Override
        public int compare(Candidate o1, Candidate o2)
        {
            return o1.hashCode() - o2.hashCode();
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
    
}
