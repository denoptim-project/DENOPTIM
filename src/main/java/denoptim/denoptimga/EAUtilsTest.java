package denoptimga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.graph.APClass;
import denoptim.graph.Candidate;
import denoptim.graph.DENOPTIMAttachmentPoint;
import denoptim.graph.DENOPTIMEdge.BondType;
import denoptim.graph.DENOPTIMGraph;
import denoptim.graph.DENOPTIMGraphTest;
import denoptim.graph.DENOPTIMTemplate;
import denoptim.graph.DENOPTIMTemplate.ContractLevel;
import denoptim.graph.DENOPTIMVertex;
import denoptim.graph.DENOPTIMVertex.BBType;
import denoptim.logging.Monitor;
import denoptim.graph.EmptyVertex;
import denoptim.utils.MutationType;
import denoptim.utils.RandomUtils;
import denoptimga.EAUtils.CandidateSource;

/**
 * Unit test
 * 
 * @author Marco Foscato
 */

public class EAUtilsTest
{
    
    private static APClass APCA, APCB, APCC;
    private static String a="A", b="B", c="C";
    
//------------------------------------------------------------------------------
    
    private void prepareFragmentSpace() throws DENOPTIMException
    {
        APCA = APClass.make(a, 0,BondType.SINGLE);
        APCB = APClass.make(b, 1,BondType.SINGLE);
        APCC = APClass.make(c, 2,BondType.SINGLE);
        
        HashMap<APClass,ArrayList<APClass>> cpMap = 
                new HashMap<APClass,ArrayList<APClass>>();
        ArrayList<APClass> lstA = new ArrayList<APClass>();
        lstA.add(APCA);
        cpMap.put(APCA, lstA);
        ArrayList<APClass> lstB = new ArrayList<APClass>();
        lstB.add(APCB);
        lstB.add(APCC);
        cpMap.put(APCB, lstB);
        ArrayList<APClass> lstC = new ArrayList<APClass>();
        lstC.add(APCB);
        lstC.add(APCC);
        cpMap.put(APCC, lstC);
        
        /* Compatibility matrix
         * 
         *      |  A  |  B  |  C  |
         *    -------------------------
         *    A |  T  |     |     |   
         *    -------------------------
         *    B |     |  T  |  T  |   
         *    -------------------------
         *    C |     |  T  |  T  |   
         *    -------------------------
         */
        
        HashMap<APClass,APClass> capMap = new HashMap<APClass,APClass>();
        HashSet<APClass> forbEnds = new HashSet<APClass>();
        
        FragmentSpace.setCompatibilityMatrix(cpMap);
        FragmentSpace.setCappingMap(capMap);
        FragmentSpace.setForbiddenEndList(forbEnds);
        FragmentSpace.setAPclassBasedApproach(true);
        
        FragmentSpace.setScaffoldLibrary(new ArrayList<DENOPTIMVertex>());
        FragmentSpace.setFragmentLibrary(new ArrayList<DENOPTIMVertex>());
        
        EmptyVertex v1 = new EmptyVertex();
        v1.setBuildingBlockType(BBType.FRAGMENT);
        v1.addAP(APCB);
        v1.addAP(APCB);
        FragmentSpace.appendVertexToLibrary(v1, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        EmptyVertex v2 = new EmptyVertex();
        v2.setBuildingBlockType(BBType.FRAGMENT);
        v2.addAP(APCC);
        v2.addAP(APCC);
        FragmentSpace.appendVertexToLibrary(v2, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        EmptyVertex rcv = new EmptyVertex();
        rcv.setBuildingBlockType(BBType.FRAGMENT);
        rcv.addAP(APCC);
        rcv.setAsRCV(true);
        FragmentSpace.appendVertexToLibrary(rcv, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        DENOPTIMGraph graphForTemplate = new DENOPTIMGraph();
        DENOPTIMVertex vg1 = DENOPTIMVertex.newVertexFromLibrary(0,
                        BBType.FRAGMENT);
        graphForTemplate.addVertex(vg1);
        for (int i=1; i<6; i++)
        {
            DENOPTIMVertex vgi = DENOPTIMVertex.newVertexFromLibrary(0,
                    BBType.FRAGMENT);
            DENOPTIMVertex vgi_1 = graphForTemplate.getVertexAtPosition(i-1);
            graphForTemplate.appendVertexOnAP(vgi_1.getAP(1), vgi.getAP(0));
        }
        DENOPTIMVertex rcv1 = DENOPTIMVertex.newVertexFromLibrary(2,
                BBType.FRAGMENT);
        graphForTemplate.appendVertexOnAP(
                graphForTemplate.getVertexAtPosition(5).getAP(1),rcv1.getAP(0));
        DENOPTIMVertex rcv2 = DENOPTIMVertex.newVertexFromLibrary(2,
                BBType.FRAGMENT);
        graphForTemplate.appendVertexOnAP(
                graphForTemplate.getVertexAtPosition(0).getAP(0),rcv2.getAP(0));
        graphForTemplate.addRing(rcv1, rcv2);
        
        DENOPTIMTemplate template = new DENOPTIMTemplate(BBType.SCAFFOLD);
        template.setInnerGraph(graphForTemplate);
        template.setContractLevel(ContractLevel.FIXED_STRUCT);
        FragmentSpace.appendVertexToLibrary(template, BBType.SCAFFOLD,
                FragmentSpace.getScaffoldLibrary());
    }

//------------------------------------------------------------------------------

    @Test
    public void testBuildGraphFromTemplateScaffold() throws Exception
    {      
        prepareFragmentSpace();
        
        DENOPTIMGraph g = EAUtils.buildGraph();
        
        if (g == null)
            assertTrue(false,"faild construction of graph");
        
        DENOPTIMTemplate t = (DENOPTIMTemplate) g.getVertexAtPosition(0);
        
        //NB: we want to not allow access to the inner graph from outside the 
        // template, but this means we cannot easily explore the inner graph.
        // Looking at the mutation sites is a dirty trick to get a look inside 
        // this template, that has contract level "free" (meaning that the 
        // content of the template can change).
        
        boolean foundChange = false;
        for (DENOPTIMVertex v : t.getMutationSites())
        {
            if (v.getBuildingBlockId() != 0)
                foundChange = true;
        }
        assertTrue(foundChange,"The initial inner graph has changed.");
    }
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
        assertTrue(Math.abs(0.5 - p)<t, "Scheme 2 on ap2");
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
    
    @Test
    public void testChooseNumberOfSitesToMutate() throws Exception
    {
        double[] weights = new double[] {0,0,0,0,1};
        assertEquals(4,EAUtils.chooseNumberOfSitesToMutate(weights,1.0));
        assertEquals(4,EAUtils.chooseNumberOfSitesToMutate(weights,0.00000001));
        
        weights = new double[] {1,0,0,0,1};
        assertEquals(4,EAUtils.chooseNumberOfSitesToMutate(weights,1.0));
        assertEquals(0,EAUtils.chooseNumberOfSitesToMutate(weights,0.00000001));
        
        weights = new double[] {1,1,1,1,1};
        assertEquals(4,EAUtils.chooseNumberOfSitesToMutate(weights,1.0));
        assertEquals(4,EAUtils.chooseNumberOfSitesToMutate(weights,0.800001));
        assertEquals(3,EAUtils.chooseNumberOfSitesToMutate(weights,0.799999));
        assertEquals(3,EAUtils.chooseNumberOfSitesToMutate(weights,0.600001));
        assertEquals(2,EAUtils.chooseNumberOfSitesToMutate(weights,0.599999));
        assertEquals(2,EAUtils.chooseNumberOfSitesToMutate(weights,0.400001));
        assertEquals(1,EAUtils.chooseNumberOfSitesToMutate(weights,0.399999));
        assertEquals(1,EAUtils.chooseNumberOfSitesToMutate(weights,0.200001));
        assertEquals(0,EAUtils.chooseNumberOfSitesToMutate(weights,0.199999));
        assertEquals(0,EAUtils.chooseNumberOfSitesToMutate(weights,0.000001));
    }
    
//------------------------------------------------------------------------------
    
}
