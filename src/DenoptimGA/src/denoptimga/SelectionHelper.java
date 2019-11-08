/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no>
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

import denoptim.molecule.DENOPTIMMolecule;
import org.apache.commons.math3.random.MersenneTwister;


/**
 *
 * @author Vishwesh Venkatraman
 */
public class SelectionHelper
{
    
//------------------------------------------------------------------------------

    /**
     * Select p individuals at random.
     * The individual with the highest fitness becomes the parent.
     * Keeping the tournament size small results in a smaller selection pressure,
     * thus increasing genetic diversity.
     * Note: this implementation is based on the WATCHMAKER framework
     * http://watchmaker.uncommons.org/
     * @param rng
     * @param molPopulation 
     * @param sz size of the mating pool
     * @return list of indices of individuals in the population
     */

    protected static int[] performTournamentSelection(MersenneTwister rng,
                                    ArrayList<DENOPTIMMolecule> molPopulation,
                                    int sz)
    {
        int k = molPopulation.size();
        
        int[] selection = new int[sz];
        

        for (int i=0; i<sz; i++)
        {
            // Pick two candidates at random.
            int p1 = rng.nextInt(k);
            int p2 = rng.nextInt(k);

            // Use a random value to decide wether to select the fitter individual
            // or the weaker one.
            boolean selectFitter = rng.nextBoolean();
            
            if (selectFitter)
            {
                // Select the fitter candidate.
                selection[i] = molPopulation.get(p1).getMoleculeFitness() >
                    molPopulation.get(p2).getMoleculeFitness() ? p1 : p2;
            }
            else
            {
                // Select the weaker candidate.
                selection[i] = molPopulation.get(p2).getMoleculeFitness() >
                    molPopulation.get(p1).getMoleculeFitness() ? p1 : p2;
            }
        }
        
        return selection;
    }

//------------------------------------------------------------------------------

    /**
     * Randomly select k individuals from the population
     * @param rng
     * @param molPopulation  
     * @param sz size of the mating pool
     * @return list of indices of individuals in the population
     */
    protected static int[] performRandomSelection(MersenneTwister rng,
                                    ArrayList<DENOPTIMMolecule> molPopulation,
                                    int sz)
    {
        int[] selection = new int[sz];
        int psize = molPopulation.size();
        for (int i=0; i<sz; i++)
            selection[i] = rng.nextInt(psize);

        return selection;
    }

//------------------------------------------------------------------------------

    /**
     * Stochastic Uniform Sampling
     * Note: this implementation is based on the WATCHMAKER framework
     * http://watchmaker.uncommons.org/
     * @param rng
     * @param molPopulation 
     * @param sz size of the mating pool
     * @return list of indices of individuals in the population
     */
    protected static int[] performSUS(MersenneTwister rng,
                                    ArrayList<DENOPTIMMolecule> molPopulation,
                                    int sz)
    {
        int k = molPopulation.size();
        int[] selection = new int[sz];
        // Calculate the sum of all fitness values.
        double aggregateFitness = 0;

        for (int i=0; i<k; i++)
        {
            aggregateFitness += molPopulation.get(i).getMoleculeFitness();
        }


        // Pick a random offset between 0 and 1 as the starting point for selection.
        double startOffset = rng.nextDouble();
        double cumulativeExpectation = 0;
        int index = 0;
        int c = 0;
        for (int i=0; i<k; i++)
        {
            // Calculate the number of times this candidate is expected to
            // be selected on average and add it to the cumulative total
            // of expected frequencies.
            cumulativeExpectation += molPopulation.get(i).getMoleculeFitness()
                                    / aggregateFitness * sz;

            // If f is the expected frequency, the candidate will be selected at
            // least as often as floor(f) and at most as often as ceil(f). The
            // actual count depends on the random starting offset.
            while (cumulativeExpectation > startOffset + index)
            {
                selection[c] = i;
                c++;
                index++;
            }
        }
        
        return selection;
    }

//------------------------------------------------------------------------------

    /*
     * Roulette wheel selection is implemented as follows:
     * 1. Sum the fitness of all the population members. TF (total fitness).
     * 2. Generate a random number r, between 0 and TF.
     * 3. Return the first population member whose fitness added to the preceding
     *    population members is greater than or equal to r.
     * Note: this implementation is based on the WATCHMAKER framework
     * http://watchmaker.uncommons.org/
     * @param rng
     * @param molPopulation 
     * @param size of the mating pool
     * @return list of indices of individuals in the population
     */

    protected static int[] performRWS(MersenneTwister rng,
                                    ArrayList<DENOPTIMMolecule> molPopulation,
                                    int sz)
    {
        int k = molPopulation.size();
        int[] selection = new int[sz];

        double[] cumulativeFitnesses = new double[k];
        cumulativeFitnesses[0] = molPopulation.get(0).getMoleculeFitness();

        for (int i=1; i<k; i++)
        {
            double fitness = molPopulation.get(i).getMoleculeFitness();

            cumulativeFitnesses[i] = cumulativeFitnesses[i-1] + fitness;
        }

        for (int i=0; i<sz; i++)
        {
            double randomFitness = rng.nextDouble() *
                        cumulativeFitnesses[cumulativeFitnesses.length-1];
            int index = Arrays.binarySearch(cumulativeFitnesses, randomFitness);
            if (index < 0)
            {
                // Convert negative insertion point to array index.
                index = Math.abs(index + 1);
            }
            selection[i] = index;
        }
        
        return selection;
    }
    
//------------------------------------------------------------------------------
}
