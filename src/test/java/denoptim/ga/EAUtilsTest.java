package denoptim.ga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.ga.EAUtils;
import denoptim.ga.Population;
import denoptim.ga.EAUtils.CandidateSource;
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
import denoptim.graph.rings.PathSubGraph;
import denoptim.graph.EmptyVertex;
import denoptim.io.DenoptimIO;
import denoptim.logging.Monitor;
import denoptim.utils.RandomUtils;

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
        
        DENOPTIMGraph g = EAUtils.buildGraph(new GAParameters());
        
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
        Population population = new Population(new GAParameters());
        population.add(c1);
        population.add(c2);
        Monitor mnt = new Monitor();
        
        Candidate offspring = EAUtils.buildCandidateByXOver(eligibleParents, 
                population, mnt, new GAParameters());
        
        assertTrue(offspring==null, "Redundat xover is not done");
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testBuildByXOver_SubGraph() throws Exception
    {
        PopulationTest.prepare();
        GAParameters gaparams = new GAParameters();
        Population population = new Population(gaparams);
        
        /*
         * -(A)v0(A)-(A)v1(A)-(A)v2(A)-(A)v3(B)-(B)v4(B)-(B)v5(B)-
         */
        DENOPTIMGraph gA = PopulationTest.makeGraphA();
        Candidate cA = new Candidate("CA",gA);
        cA.setFitness(1.23);
        population.add(cA);
        
        /*
         * v0(B)-(B)v1(A)-(A)v2(B)-(B)v3(A)-(A)v4(B)-(B)v5
         */
        DENOPTIMGraph gE = PopulationTest.makeGraphE();
        Candidate cE = new Candidate("CE",gE);
        cE.setFitness(2.34);
        population.add(cE);
        
        ArrayList<Candidate> eligibleParents = new ArrayList<Candidate>();
        eligibleParents.add(cA);
        eligibleParents.add(cE);

        Monitor mnt = new Monitor();
        
        Candidate offspring0 = EAUtils.buildCandidateByXOver(eligibleParents, 
                population, mnt, new int[]{0,1}, 3, 0, gaparams);
        
        Candidate offspring1 = EAUtils.buildCandidateByXOver(eligibleParents, 
                population, mnt, new int[]{0,1}, 3, 1, gaparams);
    
        DENOPTIMGraph g0 = offspring0.getGraph();
        assertEquals(4,g0.getVertexCount());
        assertEquals(3,g0.getEdgeCount());
        int maxLength = -1;
        for (DENOPTIMVertex v : g0.getVertexList())
        {
            ArrayList<DENOPTIMVertex> childTree = new ArrayList<DENOPTIMVertex>();
            g0.getChildrenTree(v, childTree);
            if (childTree.size()>maxLength)
            {
                maxLength = childTree.size();
            }
        }
        assertEquals(3,maxLength);

        DENOPTIMGraph g1 = offspring1.getGraph();
        assertEquals(8,g1.getVertexCount());
        assertEquals(7,g1.getEdgeCount());
        maxLength = -1;
        for (DENOPTIMVertex v : g1.getVertexList())
        {
            ArrayList<DENOPTIMVertex> childTree = new ArrayList<DENOPTIMVertex>();
            g1.getChildrenTree(v, childTree);
            if (childTree.size()>maxLength)
            {
                maxLength = childTree.size();
            }
        }
        assertEquals(7,maxLength);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * NB: the graphs from methods {@link #getPairOfTestGraphsB()} and
     * {@link #getPairOfTestGraphsBxo()} and  
     * {@link #getPairOfTestGraphsBxoxo()} are a sequence resulting from 
     * crossover operations. Note that the order of APs in on the templates
     * changes as a result of the crossover. For this reason, the backwards 
     * crossover of the graphs from {@link #getPairOfTestGraphsBxo()} does not
     * produce the graphs from {@link #getPairOfTestGraphsB()}, but
     * those from {@link #getPairOfTestGraphsBxoxo()}.
     */
    @Test
    public void testBuildByXOver_Embedded_Free() throws Exception
    {
        PopulationTest.prepare();
        GAParameters gaparams = new GAParameters();
        Population population = new Population(gaparams);

        DENOPTIMGraph[] pair = PopulationTest.getPairOfTestGraphsB();
        DENOPTIMGraph gA = pair[0];
        DENOPTIMGraph gB = pair[1];
        ((DENOPTIMTemplate)gA.getVertexAtPosition(1)).setContractLevel(
                ContractLevel.FREE);
        ((DENOPTIMTemplate)gB.getVertexAtPosition(1)).setContractLevel(
                ContractLevel.FREE);
        
        Candidate cA = new Candidate("CA",gA);
        population.add(cA);        

        Candidate cB = new Candidate("CB",gB);
        population.add(cB);
        
        ArrayList<Candidate> eligibleParents = new ArrayList<Candidate>();
        eligibleParents.add(cA);
        eligibleParents.add(cB);

        Monitor mnt = new Monitor();
        
        Candidate offspring0 = EAUtils.buildCandidateByXOver(eligibleParents, 
                population, mnt, new int[]{0,1}, 8, 0, gaparams);
        
        Candidate offspring1 = EAUtils.buildCandidateByXOver(eligibleParents, 
                population, mnt, new int[]{0,1}, 8, 1, gaparams);
        
        DENOPTIMGraph g0xo = offspring0.getGraph();
        DENOPTIMGraph g1xo = offspring1.getGraph();
        
        DENOPTIMGraph[] expectedPair = PopulationTest.getPairOfTestGraphsBxo();
        DENOPTIMGraph expected0 = expectedPair[0];
        DENOPTIMGraph expected1 = expectedPair[1];
        ((DENOPTIMTemplate)expected0.getVertexAtPosition(1)).setContractLevel(
                ContractLevel.FREE);
        ((DENOPTIMTemplate)expected1.getVertexAtPosition(1)).setContractLevel(
                ContractLevel.FREE);
        
        assertTrue(expected0.sameAs(g0xo, new StringBuilder()));
        assertTrue(expected1.sameAs(g1xo, new StringBuilder()));
    }
    
//------------------------------------------------------------------------------
    
    /**
     * NB: the graphs from methods {@link #getPairOfTestGraphsB()} and
     * {@link #getPairOfTestGraphsBxo()} and  
     * {@link #getPairOfTestGraphsBxoxo()} are a sequence resulting from 
     * crossover operations. Note that the order of APs in on the templates
     * changes as a result of the crossover. For this reason, the backwards 
     * crossover of the graphs from {@link #getPairOfTestGraphsBxo()} does not
     * produce the graphs from {@link #getPairOfTestGraphsB()}, but
     * those from {@link #getPairOfTestGraphsBxoxo()}.
     */
    @Test
    public void testBuildByXOver_Embedded_FreeBackwards() throws Exception
    {
        PopulationTest.prepare();
        GAParameters gaparams = new GAParameters();
        Population population = new Population(gaparams);

        DENOPTIMGraph[] pair = PopulationTest.getPairOfTestGraphsBxo();
        DENOPTIMGraph gA = pair[0];
        DENOPTIMGraph gB = pair[1];
        ((DENOPTIMTemplate)gA.getVertexAtPosition(1)).setContractLevel(
                ContractLevel.FREE);
        ((DENOPTIMTemplate)gB.getVertexAtPosition(1)).setContractLevel(
                ContractLevel.FREE);
        
        Candidate cA = new Candidate("CA",gA);
        population.add(cA);        

        Candidate cB = new Candidate("CB",gB);
        population.add(cB);
        
        ArrayList<Candidate> eligibleParents = new ArrayList<Candidate>();
        eligibleParents.add(cA);
        eligibleParents.add(cB);

        Monitor mnt = new Monitor();
        
        Candidate offspring0 = EAUtils.buildCandidateByXOver(eligibleParents, 
                population, mnt, new int[]{0,1}, 17, 0, gaparams);
        
        Candidate offspring1 = EAUtils.buildCandidateByXOver(eligibleParents, 
                population, mnt, new int[]{0,1}, 17, 1, gaparams);
        
        DENOPTIMGraph g0xo = offspring0.getGraph();
        DENOPTIMGraph g1xo = offspring1.getGraph();
        
        DENOPTIMGraph[] expectedPair = PopulationTest.getPairOfTestGraphsBxoxo();
        DENOPTIMGraph expected0 = expectedPair[0];
        DENOPTIMGraph expected1 = expectedPair[1];
        ((DENOPTIMTemplate)expected0.getVertexAtPosition(1)).setContractLevel(
                ContractLevel.FREE);
        ((DENOPTIMTemplate)expected1.getVertexAtPosition(1)).setContractLevel(
                ContractLevel.FREE);
        
        assertTrue(expected0.sameAs(g0xo, new StringBuilder()));
        assertTrue(expected1.sameAs(g1xo, new StringBuilder()));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testBuildByXOver_Embedded_FixedStructure() throws Exception
    {
        PopulationTest.prepare();
        GAParameters gaparams = new GAParameters();
        Population population = new Population(gaparams);

        DENOPTIMGraph[] pair = PopulationTest.getPairOfTestGraphsB();
        DENOPTIMGraph gA = pair[0];
        DENOPTIMGraph gB = pair[1];
        DENOPTIMTemplate embeddedTmplA = (DENOPTIMTemplate) gA.getVertexAtPosition(1);
        embeddedTmplA.setContractLevel(ContractLevel.FIXED_STRUCT);
        DENOPTIMGraph embeddedGraphA = embeddedTmplA.getInnerGraph();
        DENOPTIMTemplate embeddedTmplB = (DENOPTIMTemplate) gB.getVertexAtPosition(1);
        embeddedTmplB.setContractLevel(ContractLevel.FIXED_STRUCT);
        DENOPTIMGraph embeddedGraphB = embeddedTmplB.getInnerGraph();
        
        // Make vertexes unique so, even though they are empty, they will be 
        // seen as non equal and crossover will be seen as non-redundant.
        String propName = "uniquefier";
        int i=0;
        for (DENOPTIMVertex v : embeddedGraphA.getVertexList())
        {
            v.setUniquefyingProperty(propName);
            v.setProperty(propName, i);
            i++;
        }
        
        Candidate cA = new Candidate("CA",gA);
        population.add(cA);
        Candidate cB = new Candidate("CB",gB);
        population.add(cB);
        ArrayList<Candidate> eligibleParents = new ArrayList<Candidate>();
        eligibleParents.add(cA);
        eligibleParents.add(cB);

        Monitor mnt = new Monitor();
        boolean embeddedGraphHasBeenAlteredA = false;
        boolean embeddedGraphHasBeenAlteredB = false;
        for (int ixo=0; ixo<11; ixo++)
        {
            Candidate offspring0 = null;
            Candidate offspring1 = null;
            try
            {
                offspring0 = EAUtils.buildCandidateByXOver(eligibleParents, 
                        population, mnt, new int[]{0,1}, ixo, 0, gaparams);
                offspring1 = EAUtils.buildCandidateByXOver(eligibleParents, 
                        population, mnt, new int[]{0,1}, ixo, 1, gaparams);
            } catch (IndexOutOfBoundsException e)
            {
                if (e.getMessage().contains("Index 10 out of bounds"))
                {
                    // All good! We intentionally triggered this exception to 
                    // verify that the list of xover points has the right size.
                    break;
                }
                throw e;
            }
            
            DENOPTIMGraph g0xo = offspring0.getGraph();
            DENOPTIMGraph g1xo = offspring1.getGraph();
            DENOPTIMTemplate t0 = null;
            DENOPTIMTemplate t1 = null;
            for (DENOPTIMVertex v : g0xo.getVertexList())
            {
                if (v instanceof DENOPTIMTemplate)
                {
                    t0 = (DENOPTIMTemplate) v;
                    break;
                }
            }
            for (DENOPTIMVertex v : g1xo.getVertexList())
            {
                if (v instanceof DENOPTIMTemplate)
                {
                    t1 = (DENOPTIMTemplate) v;
                    break;
                }
            }
            assertNotNull(t0);
            assertNotNull(t1);
            
            DENOPTIMGraph embeddedGraph0 = t0.getInnerGraph();
            DENOPTIMGraph embeddedGraph1 = t1.getInnerGraph();
            
            // Ensure there has been a change
            if (!embeddedGraphA.isIsomorphicTo(embeddedGraph0) 
                    && !embeddedGraphA.isIsomorphicTo(embeddedGraph1))
            {
                embeddedGraphHasBeenAlteredA = true;
            }
            if (!embeddedGraphB.isIsomorphicTo(embeddedGraph0) 
                    && !embeddedGraphB.isIsomorphicTo(embeddedGraph1))
            {
                embeddedGraphHasBeenAlteredB = true;
            }
            
            // Ensure consistency with "fixed-structure" contract
            assertTrue(embeddedGraphA.isIsostructuralTo(embeddedGraph0)
                    || embeddedGraphB.isIsostructuralTo(embeddedGraph0));
            assertTrue(embeddedGraphA.isIsostructuralTo(embeddedGraph1)
                    || embeddedGraphB.isIsostructuralTo(embeddedGraph1));
        }
        assertTrue(embeddedGraphHasBeenAlteredA);
        assertTrue(embeddedGraphHasBeenAlteredB);
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
