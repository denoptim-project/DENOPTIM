package denoptim.fitness.descriptors;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.fingerprint.Fingerprinter;
import org.openscience.cdk.fingerprint.IBitFingerprint;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.smiles.SmilesParser;

/**
 * Unit test for descriptor TanimotoMolSimilarity
 * 
 * @author Marco Foscato
 */

public class TanimotoMolSimilarityTest
{
    private TanimotoMolSimilarity descriptor;
    
    @BeforeEach
    public void setUp() throws Exception
    {
        Constructor<TanimotoMolSimilarity> defaultConstructor = 
                TanimotoMolSimilarity.class.getConstructor();
        this.descriptor = defaultConstructor.newInstance();
        this.descriptor.initialise(DefaultChemObjectBuilder.getInstance());
    }
	
//------------------------------------------------------------------------------
		
	@Test
	public void testTanimotoMolSimilarity() throws Exception
	{
        SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        IAtomContainer ref = sp.parseSmiles("CNC(=O)c1cc(OC)ccc1");
        Fingerprinter fpMaker = new Fingerprinter();
        IBitFingerprint fpRef = fpMaker.getBitFingerprint(ref);
        
        Object[] params = {fpRef};
        descriptor.setParameters(params);
        
        IAtomContainer mol1 = sp.parseSmiles("COc1ccccc1");
        double value = ((DoubleResult) descriptor.calculate(mol1).getValue())
                .doubleValue();
        assertTrue(closeEnough(0.32758,value), "Tanimoto similarity with mol1: "
                + value);

        IAtomContainer mol2 = sp.parseSmiles("P");
        value = ((DoubleResult) descriptor.calculate(mol2).getValue())
                .doubleValue();
        assertTrue(closeEnough(0.0,value), "Tanimoto similarity with mol2: "
                + value);

        IAtomContainer mol3 = sp.parseSmiles("COc1cc(C(=O)NC)ccc1");
        value = ((DoubleResult) descriptor.calculate(mol3).getValue())
                .doubleValue();
        assertTrue(closeEnough(1.0,value), "Tanimoto similarity with mol3: "
                + value);
	}
	
//------------------------------------------------------------------------------
	
	private boolean closeEnough(double expected, double actual)
	{
	    double threshold = 0.0001;
	    double delta = Math.abs(expected-actual);
	    return delta < threshold;
	}
 
//------------------------------------------------------------------------------

}
