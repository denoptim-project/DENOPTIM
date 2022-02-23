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

package denoptim.denoptimga;

import java.util.ArrayList;
import java.util.Arrays;

import denoptim.graph.Candidate;
import denoptim.utils.RandomUtils;


/**
 *
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
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
     * @param population the ensemble of individuals to choose from
     * @param sz number of individuals to select
     * @return list of selected individuals 
     */

    protected static Candidate[] performTournamentSelection(
                                    ArrayList<Candidate> population,
                                    int sz)
    {
        Candidate[] selection = new Candidate[sz];
        for (int i=0; i<sz; i++)
        {
            // Pick two candidates at random.
            Candidate p1 = RandomUtils.randomlyChooseOne(population);
            Candidate p2 = RandomUtils.randomlyChooseOne(population);

            // Use a random value to decide weather to select the fitter individual
            // or the weaker one.
            boolean selectFitter = RandomUtils.nextBoolean();
            
            if (selectFitter)
            {
                // Select the fitter candidate.
                selection[i] = p1.getFitness() > p2.getFitness() ? p1 : p2;
            }
            else
            {
                // Select the weaker candidate.
                selection[i] = p2.getFitness() > p1.getFitness() ? p1 : p2;
            }
        }
        return selection;
    }

//------------------------------------------------------------------------------

    /**
     * Randomly select k individuals from the population
     * @param population the ensemble of individuals to choose from
     * @param sz number of individuals to select
     * @return list of indices of individuals in the population
     */
    protected static Candidate[] performRandomSelection(
                                    ArrayList<Candidate> population,
                                    int sz)
    {
        Candidate[] selection = new Candidate[sz];
        for (int i=0; i<sz; i++)
            selection[i] = RandomUtils.randomlyChooseOne(population);

        return selection;
    }

//------------------------------------------------------------------------------

    /**
     * Stochastic Uniform Sampling
     * Note: this implementation is based on the WATCHMAKER framework
     * http://watchmaker.uncommons.org/
     * @param population the ensemble of individuals to choose from
     * @param sz number of individuals to select
     * @return list of indices of individuals in the population
     */
    protected static Candidate[] performSUS(ArrayList<Candidate> population, 
            int sz)
    {
        int k = population.size();
        Candidate[] selection = new Candidate[sz];
        // Calculate the sum of all fitness values.
        double aggregateFitness = 0;

        for (int i=0; i<k; i++)
        {
            aggregateFitness += population.get(i).getFitness();
        }


        // Pick a random offset between 0 and 1 as the starting point for selection.
        double startOffset = RandomUtils.nextDouble();
        double cumulativeExpectation = 0;
        int index = 0;
        int c = 0;
        for (int i=0; i<k; i++)
        {
            // Calculate the number of times this candidate is expected to
            // be selected on average and add it to the cumulative total
            // of expected frequencies.
            cumulativeExpectation += population.get(i).getFitness()
                                    / aggregateFitness * sz;

            // If f is the expected frequency, the candidate will be selected at
            // least as often as floor(f) and at most as often as ceil(f). The
            // actual count depends on the random starting offset.
            while (cumulativeExpectation > startOffset + index)
            {
                selection[c] = population.get(i);
                c++;
                index++;
            }
        }
        
        return selection;
    }

//------------------------------------------------------------------------------

    /**
     * Roulette wheel selection is implemented as follows:
     * 1. Sum the fitness of all the population members. TF (total fitness).
     * 2. Generate a random number r, between 0 and TF.
     * 3. Return the first population member whose fitness added to the preceding
     *    population members is greater than or equal to r.
     * Note: this implementation is based on the WATCHMAKER framework
     * http://watchmaker.uncommons.org/
     * @param population the ensemble of individuals to choose from
     * @param sz number of individuals to select
     * @return list of indices of individuals in the population
     */

    protected static Candidate[] performRWS(ArrayList<Candidate> population,
                                    int sz)
    {
        int k = population.size();
        Candidate[] selection = new Candidate[sz];

        double[] cumulativeFitnesses = new double[k];
        cumulativeFitnesses[0] = population.get(0).getFitness();

        for (int i=1; i<k; i++)
        {
            double fitness = population.get(i).getFitness();

            cumulativeFitnesses[i] = cumulativeFitnesses[i-1] + fitness;
        }

        for (int i=0; i<sz; i++)
        {
            double randomFitness = RandomUtils.nextDouble() *
                        cumulativeFitnesses[cumulativeFitnesses.length-1];
            int index = Arrays.binarySearch(cumulativeFitnesses, randomFitness);
            if (index < 0)
            {
                // Convert negative insertion point to array index.
                index = Math.abs(index + 1);
            }
            selection[i] = population.get(index);
        }
        
        return selection;
    }
    
//------------------------------------------------------------------------------
}
