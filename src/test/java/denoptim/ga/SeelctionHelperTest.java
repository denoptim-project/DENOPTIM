package denoptim.ga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import denoptim.graph.Candidate;
import denoptim.programs.RunTimeParameters;
import denoptim.programs.denovo.GAParameters;
import denoptim.utils.Randomizer;;

/**
 * Unit test
 * 
 * @author Marcello Costamagna
 */

public class SeelctionHelperTest
{
//------------------------------------------------------------------------------

    @SuppressWarnings("null")
    @Test
    public void testPerformSUS() {
        // Create a sample population of candidates
        List<Candidate> population = new ArrayList<>();
        Double[] fitnesses = {3.0, 2.0, 1.1, -1.3, -2.4};
        for (int i=0; i<fitnesses.length; i++)
        {
            population.add(new Candidate());
            population.get(i).setFitness(fitnesses[i]);
        }

        // Set the number of individuals to select
        int sz1 = 1;
        int sz2 = 2;
 
        // Create a settings object with a randomizer 
        GAParameters settings = new GAParameters();
        settings.setRandomizer(new Randomizer());

        // Perform SUS and obtain the selection
        Candidate[] selection1 = SelectionHelper.performSUS(population, sz1, settings);
        Candidate[] selection2 = SelectionHelper.performSUS(population, sz2, settings);
        

        // Check if the selection size is as expected
        assertEquals(sz1, selection1.length);
        assertEquals(sz2, selection2.length);

        // Check if the selected candidates are part of the population
//        for (Candidate candidate : selection) {
//            assertTrue(population.contains(candidate));
//        }

    }
    
}
