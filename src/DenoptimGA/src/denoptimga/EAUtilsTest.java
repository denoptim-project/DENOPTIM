package denoptimga;

import denoptim.molecule.*;
import org.junit.jupiter.api.Test;

import denoptim.molecule.DENOPTIMVertex.BBType;
import denoptim.utils.RandomUtils;
import denoptimga.EAUtils.CandidateSource;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test
 * 
 * @author Marco Foscato
 */

public class EAUtilsTest
{
  //------------------------------------------------------------------------------

    @Test
    public void testAvoidRedundantXOver() throws Exception
    {   
        DENOPTIMGraph g1 = new DENOPTIMGraph();
        EmptyVertex s = new EmptyVertex();
        s.addAP();
        s.addAP();
        s.setBuildingBlockType(BBType.SCAFFOLD);
        
        EmptyVertex v = new EmptyVertex();
        v.addAP();
        v.setBuildingBlockType(BBType.FRAGMENT);
        
        g1.addVertex(s);
        g1.appendVertexOnAP(s.getAP(0), v.getAP(0));
        
        Candidate c1 = new Candidate(g1); 
        
        DENOPTIMGraph g2 = g1.clone();
        Candidate c2 = new Candidate(g2);
        
        ArrayList<Candidate> eligibleParents = new ArrayList<Candidate>();
        eligibleParents.add(c1);
        eligibleParents.add(c2);
        Population population = new Population();
        population.add(c1);
        population.add(c2);
        Monitor mnt = new Monitor();
        
        Candidate offspring = EAUtils.buildCandidateByXOver(eligibleParents, 
                population, mnt);
        
        assertTrue(offspring==null, "Redudnat xover is not done");
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testCandidateGenerationMethod() throws Exception
    {
        int ix = 0, im=0, ic=0, tot=1000;
        double wx = 2, wm = 0.6, wc=0.05;
        double wtot = wx + wm + wc;
        for (int i=0; i<tot; i++)
        {
            switch (EAUtils.pickNewCandidateGenerationMode(wx,wm,wc))
            {
                case CROSSOVER:
                    ix++;
                    break;
                case MUTATION:
                    im++;
                    break;
                case CONSTRUCTION:
                    ic++;
                    break;
            }
        }
        double x = ((double)ix) / tot;
        double m = ((double)im) / tot;
        double c = ((double)ic) / tot;
        
        double thld = 0.05;
        
        assertTrue(Math.abs(x-(wx/wtot)) < thld, "#Xover cases are off!");
        assertTrue(Math.abs(m-(wm/wtot)) < thld, "#Mutation cases are off!");
        assertTrue(Math.abs(c-(wc/wtot)) < thld, "#Built cases are off!");
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testCandidateGenerationMethod2() throws Exception
    {
        int tot = 100000;
        long seed = 1234567;
        long otherSeed = 987654321;
        double wx = 2, wm = 0.6, wc=0.05;
        
        RandomUtils.initialiseRNG(seed);
        List<CandidateSource> resultsA = new ArrayList<CandidateSource>();
        for (int i=0; i<tot; i++)
        {
            resultsA.add(EAUtils.pickNewCandidateGenerationMode(wx,wm,wc));
        }
        
        RandomUtils.initialiseRNG(otherSeed);
        List<CandidateSource> resultsB = new ArrayList<CandidateSource>();
        for (int i=0; i<tot; i++)
        {
            resultsB.add(EAUtils.pickNewCandidateGenerationMode(wx,wm,wc));
        }
        
        RandomUtils.initialiseRNG(seed);
        List<CandidateSource> resultsC = new ArrayList<CandidateSource>();
        for (int i=0; i<tot; i++)
        {
            resultsC.add(EAUtils.pickNewCandidateGenerationMode(wx,wm,wc));
        }
        
        boolean different = false;
        for (int i=0; i<tot; i++)
        {
            if (resultsA.get(i) != resultsB.get(i))
            {
                different = true;
                break;
            }
        }
        assertTrue(different);
        
        for (int i=0; i<tot; i++)
        {
            assertEquals(resultsA.get(i),resultsC.get(i),
                    "Inconsistent sequence of random decisions");
        }
    }
        
//------------------------------------------------------------------------------
	
    @Test
    public void testCrowdingProbability() throws Exception
    {
        DENOPTIMGraph g = DENOPTIMGraphTest.makeTestGraphA();
        double t = 0.001;
        double p = 0.0;
        for (DENOPTIMAttachmentPoint ap : g.getAttachmentPoints())
        {
            p = EAUtils.getCrowdingProbability(ap,3,1.0,1.0,1.0);
            assertTrue(Math.abs(1.0 - p)<t,
                    "Scheme 3 should return always 1.0 but was "+p);
        }
        DENOPTIMAttachmentPoint ap3 = g.getVertexAtPosition(0).getAP(3);
        p = EAUtils.getCrowdingProbability(ap3,0,1.0,10,1.0);
        assertTrue(Math.abs(1.0 - p)<t, "Scheme 0 on ap3: 1.0 != "+p);
        p = EAUtils.getCrowdingProbability(ap3,1,1.0,10,1.0);
        assertTrue(Math.abs(1.0 - p)<t, "Scheme 1 on ap3: 1.0 != "+p);
        p = EAUtils.getCrowdingProbability(ap3,2,1.0,10,1.0);
        assertTrue(Math.abs(1.0 - p)<t, "Scheme 2 on ap3: 1.0 != "+p);
        
        DENOPTIMAttachmentPoint ap2 = g.getVertexAtPosition(0).getAP(2);
        p = EAUtils.getCrowdingProbability(ap2,2,1.0,10,1.0);
        assertTrue(Math.abs(0.0 - p)<t, "Scheme 2 on ap2");
    }
  
//------------------------------------------------------------------------------
    
    @Test
    public void testSelectNonScaffoldNonCapVertex() throws Exception
    {
        DENOPTIMGraph g = new DENOPTIMGraph();
        EmptyVertex s = new EmptyVertex();
        s.addAP();
        s.addAP();
        s.setBuildingBlockType(BBType.SCAFFOLD);
        
        EmptyVertex v = new EmptyVertex();
        v.addAP();
        v.addAP();
        v.addAP();
        v.addAP();
        v.setBuildingBlockType(BBType.FRAGMENT);

        EmptyVertex c1 = new EmptyVertex();
        c1.addAP();
        c1.setBuildingBlockType(BBType.CAP);

        EmptyVertex c2 = new EmptyVertex();
        c2.addAP();
        c2.setBuildingBlockType(BBType.CAP);

        EmptyVertex c3 = new EmptyVertex();
        c3.addAP();
        c3.setBuildingBlockType(BBType.CAP);

        EmptyVertex c4 = new EmptyVertex();
        c4.addAP();
        c4.setBuildingBlockType(BBType.CAP);
        
        g.addVertex(s);
        g.appendVertexOnAP(s.getAP(0), v.getAP(0));
        g.appendVertexOnAP(s.getAP(1), c1.getAP(0));
        g.appendVertexOnAP(v.getAP(1), c2.getAP(0));
        g.appendVertexOnAP(v.getAP(2), c3.getAP(0));
        g.appendVertexOnAP(v.getAP(3), c4.getAP(0));
        
        DENOPTIMVertex expected = v;
        for (int i=0; i<5; i++)
        {
            DENOPTIMVertex chosen = EAUtils.selectNonScaffoldNonCapVertex(g);
            assertEquals(expected, chosen, "Index of the only choosable " +
                    "vertex");
        }
    }
    
//------------------------------------------------------------------------------
    
}
