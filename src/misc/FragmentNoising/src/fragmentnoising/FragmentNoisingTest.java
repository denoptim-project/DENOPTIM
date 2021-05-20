package fragmentnoising;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.random.MersenneTwister;
import org.junit.jupiter.api.Test;

import denoptim.utils.RandomUtils;

class FragmentNoisingTest {

	@Test
	void testAddNoise() 
	{
		double noiseLevel = 0.0025;
		RandomUtils.initialiseRNG(102938475);
        MersenneTwister rng = RandomUtils.getRNG();
        List<Double> vals = new ArrayList<Double>();
        vals.add(1.000000000);
        vals.add(10.000000000);
        vals.add(100.000000000);
        vals.add(1000.000000000);
        vals.add(10000.000000000);
        vals.add(1.000000000);
        vals.add(1.000000000);
        vals.add(1.000000000);
        vals.add(1.000000000);
        vals.add(1.000000000);
        for (Double v : vals)
        {
	        double nv = FragmentNoising.addNoise(v, rng, noiseLevel);
	        double d = Math.abs(nv-v);
	        System.out.println("v: "+v+" nv: "+nv+" d: "+d);
	        assertTrue(d<(noiseLevel+0.0000001));
        }
	}
}
