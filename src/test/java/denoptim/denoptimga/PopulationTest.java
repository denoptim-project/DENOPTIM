package denoptim.denoptimga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.graph.APClass;
import denoptim.graph.Candidate;
import denoptim.graph.DENOPTIMEdge;
import denoptim.graph.DENOPTIMEdge.BondType;
import denoptim.graph.DENOPTIMTemplate.ContractLevel;
import denoptim.graph.DENOPTIMGraph;
import denoptim.graph.DENOPTIMTemplate;
import denoptim.graph.DENOPTIMVertex;
import denoptim.graph.DENOPTIMVertex.BBType;
import denoptim.logging.Monitor;
import denoptim.graph.EmptyVertex;
import denoptim.io.DenoptimIO;


/**
 * Unit test
 * 
 * @author Marco Foscato
 */

public class PopulationTest
{
    private static APClass APCA, APCB, APCC, APCD;
    
//------------------------------------------------------------------------------
    
    /*
     * We do not do @BeforeEach because this method must be static to be used
     * in other unit tests.
     */
    static void prepare() throws DENOPTIMException
    {   
        APCA = APClass.make("A", 0, BondType.SINGLE);
        APCB = APClass.make("B", 0, BondType.SINGLE);
        APCC = APClass.make("C", 0, BondType.SINGLE);
        APCD = APClass.make("D", 99, BondType.DOUBLE);
        
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
        lstC.add(APCC);
        cpMap.put(APCC, lstC);
        ArrayList<APClass> lstD = new ArrayList<APClass>();
        lstD.add(APCD);
        cpMap.put(APCD, lstD);
        
        
        /* Compatibility matrix
         * 
         *      |  A  |  B  |  C  | D |
         *    -------------------------
         *    A |  T  |     |     |   |
         *    -------------------------
         *    B |     |  T  |  T  |   |
         *    -------------------------
         *    C |     |     |  T  |   |
         *    -------------------------
         *    D |     |     |     | T |
         */
        
        HashMap<APClass,APClass> capMap = new HashMap<APClass,APClass>();
        HashSet<APClass> forbEnds = new HashSet<APClass>();
        
        FragmentSpace.setCompatibilityMatrix(cpMap);
        FragmentSpace.setCappingMap(capMap);
        FragmentSpace.setForbiddenEndList(forbEnds);
        FragmentSpace.setAPclassBasedApproach(true);
    }

//------------------------------------------------------------------------------
    
    @Test
    public void testXOverCompatibility() throws Exception
    {
        prepare();
        Population pop = new Population();
        
        DENOPTIMGraph g1 = makeGraphA();
        // We give uniquefying properties to the vertexes so that they
        // are not seen as v.sameAs(o).
        String k = "Uniquefying";
        int counter = 0;
        for (int i=0; i<g1.getVertexCount(); i++)
        {
            DENOPTIMVertex v = g1.getVertexAtPosition(i);
            v.setUniquefyingProperty(k);
            v.setProperty(k, counter);
            counter++;
        }
        Candidate c1 = new Candidate("C1",g1);
        pop.add(c1);
        
        DENOPTIMGraph g2 = makeGraphB();
        for (int i=0; i<g2.getVertexCount(); i++)
        {
            DENOPTIMVertex v = g2.getVertexAtPosition(i);
            v.setUniquefyingProperty(k);
            v.setProperty(k, counter);
            counter++;
        }
        Candidate c2 = new Candidate("C2",g2);
        pop.add(c2);
        
        DENOPTIMGraph g3 = makeGraphB();
        for (int i=0; i<g3.getVertexCount(); i++)
        {
            DENOPTIMVertex v = g3.getVertexAtPosition(i);
            v.setUniquefyingProperty(k);
            v.setProperty(k, counter);
            counter++;
        }
        Candidate c3 = new Candidate("C3",g3);
        pop.add(c3);
        
        DENOPTIMGraph g4 = makeGraphC();
        for (int i=0; i<g4.getVertexCount(); i++)
        {
            DENOPTIMVertex v = g4.getVertexAtPosition(i);
            v.setUniquefyingProperty(k);
            v.setProperty(k, counter);
            counter++;
        }
        Candidate c4 = new Candidate("C4",g4);
        pop.add(c4);
        
        DENOPTIMGraph g5 = makeGraphD();
        for (int i=0; i<g5.getVertexCount(); i++)
        {
            DENOPTIMVertex v = g5.getVertexAtPosition(i);
            v.setUniquefyingProperty(k);
            v.setProperty(k, counter);
            counter++;
        }
        Candidate c5 = new Candidate("C5",g5);
        pop.add(c5);

        ArrayList<Candidate> partnersForCBUTTA = pop.getXoverPartners(c1, 
                new ArrayList<Candidate>(Arrays.asList(c3)));
        
        ArrayList<Candidate> partnersForC1 = pop.getXoverPartners(c1, 
                new ArrayList<Candidate>(Arrays.asList(c1,c2,c3,c4,c5)));
        ArrayList<Candidate> partnersForC2 = pop.getXoverPartners(c2, 
                new ArrayList<Candidate>(Arrays.asList(c1,c2,c3,c4,c5)));
        ArrayList<Candidate> partnersForC3 = pop.getXoverPartners(c3, 
                new ArrayList<Candidate>(Arrays.asList(c1,c2,c3,c4,c5)));
        ArrayList<Candidate> partnersForC4 = pop.getXoverPartners(c4, 
                new ArrayList<Candidate>(Arrays.asList(c1,c2,c3,c4,c5)));
        ArrayList<Candidate> partnersForC5 = pop.getXoverPartners(c5, 
                new ArrayList<Candidate>(Arrays.asList(c1,c2,c3,c4,c5)));

        Map<Candidate,Map<Candidate,Integer>> expected = 
                new HashMap<Candidate,Map<Candidate,Integer>>();
        Map<Candidate,Integer> expectedForC1 = new HashMap<Candidate,Integer>();
        expectedForC1.put(c2, 9);
        expectedForC1.put(c3, 9);
        expectedForC1.put(c4, 3);
        expected.put(c1, expectedForC1);
        Map<Candidate,Integer> expectedForC2 = new HashMap<Candidate,Integer>();
        expectedForC2.put(c1, 9);
        //expectedForC2.put(c3, 4); NO xover between candidates with same graph!
        expectedForC2.put(c4, 2);
        expected.put(c2, expectedForC2);
        Map<Candidate,Integer> expectedForC3 = new HashMap<Candidate,Integer>();
        expectedForC3.put(c1, 9);
        //expectedForC3.put(c2, 4); NO xover between candidates with same graph!
        expectedForC3.put(c4, 2);
        expected.put(c3, expectedForC3);
        Map<Candidate,Integer> expectedForC4 = new HashMap<Candidate,Integer>();
        expectedForC4.put(c1, 3);
        expectedForC4.put(c2, 2);
        expectedForC4.put(c3, 2);
        expected.put(c4, expectedForC4);
        
        compareSizeOfSites(c1,expectedForC1,partnersForC1,pop);
        compareSizeOfSites(c2,expectedForC2,partnersForC2,pop);
        compareSizeOfSites(c3,expectedForC3,partnersForC3,pop);
        compareSizeOfSites(c4,expectedForC4,partnersForC4,pop);
        assertEquals(partnersForC5.size(), 0, "Wrong umber of partners for C5");
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testClone() throws Exception
    {
        prepare();
        Population pop = new Population();
        
        DENOPTIMGraph g1 = makeGraphA();
        Candidate c1 = new Candidate("C1",g1);
        pop.add(c1);
        
        DENOPTIMGraph g2 = makeGraphB();
        Candidate c2 = new Candidate("C2",g2);
        pop.add(c2);
        
        //Addingptoperty to uniquefy a pair of vertexes
        String k = "Uniquefying";
        g1.getVertexAtPosition(1).setUniquefyingProperty(k);
        g1.getVertexAtPosition(1).setProperty(k, 123);
        g2.getVertexAtPosition(1).setUniquefyingProperty(k);
        g2.getVertexAtPosition(1).setProperty(k, 456);
        
        ArrayList<Candidate> partnersForC1 = pop.getXoverPartners(c1, 
                new ArrayList<Candidate>(Arrays.asList(c1,c2)));
        ArrayList<Candidate> partnersForC2 = pop.getXoverPartners(c2, 
                new ArrayList<Candidate>(Arrays.asList(c1,c2)));
        
        Map<Candidate,Map<Candidate,Integer>> expected = 
                new HashMap<Candidate,Map<Candidate,Integer>>();
        Map<Candidate,Integer> expectedForC1 = new HashMap<Candidate,Integer>();
        expectedForC1.put(c2, 9);
        expected.put(c1, expectedForC1);
        Map<Candidate,Integer> expectedForC2 = new HashMap<Candidate,Integer>();
        expectedForC2.put(c1, 9);
        expected.put(c2, expectedForC2);
        
        Population clonedPop = pop.clone();
        
        compareSizeOfSites(c1,expectedForC1,partnersForC1,clonedPop);
        compareSizeOfSites(c2,expectedForC2,partnersForC2,clonedPop);
        
        compareSitesLists(pop.getXoverSites(c1, c2),
                clonedPop.getXoverSites(c1, c2));
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Assumes the two lists have equal size
     */
    private void compareSitesLists(List<XoverSite> listA,
            List<XoverSite> listB)
    {
        for (int i=0; i<listA.size(); i++)
        {
            assertTrue(listA.get(i).equals(listB.get(i)));
        }
    }
    
//------------------------------------------------------------------------------
    
    private void compareSizeOfSites(Candidate parentA,
            Map<Candidate, Integer> expectedForC1, 
            ArrayList<Candidate> partnersForC1, Population pop)
    {
        for (Candidate c : expectedForC1.keySet())
        {
            assertTrue(partnersForC1.contains(c), "Missing XOver compatibility "
                    + "between '" + parentA.getName() + "' and '" + c.getName() 
                    + "'.");
            assertEquals(expectedForC1.get(c).intValue(), 
                    pop.getXoverSites(parentA, c).size(), 
                    "Wrong number of sites between '" + parentA.getName() 
                    + "' and '" + c.getName() + "'.");
        }
    }

//------------------------------------------------------------------------------

    /**
     * Produced a graph like this:
     * <pre>
     *  -(A)v0(A)-(A)v1(A)-(A)v2(A)-(A)v3(B)-(B)v4(B)-(B)v5(B)-
     * </pre>
     * 
     * You must run {@link #prepare()} before asking this class for any graph.
     */
    static DENOPTIMGraph makeGraphA() throws DENOPTIMException
    {
        DENOPTIMGraph graphA = new DENOPTIMGraph();
        EmptyVertex v0 = new EmptyVertex(0);
        v0.setBuildingBlockType(BBType.SCAFFOLD);
        v0.addAP(APCA);
        v0.addAP(APCA);
        graphA.addVertex(v0);
        EmptyVertex v1 = new EmptyVertex(1);
        v1.addAP(APCA);
        v1.addAP(APCA);
        graphA.addVertex(v1);
        EmptyVertex v2 = new EmptyVertex(2);
        v2.addAP(APCA);
        v2.addAP(APCA);
        graphA.addVertex(v2);
        EmptyVertex v3 = new EmptyVertex(3);
        v3.addAP(APCA);
        v3.addAP(APCB);
        graphA.addVertex(v3);
        EmptyVertex v4 = new EmptyVertex(4);
        v4.addAP(APCB);
        v4.addAP(APCB);
        graphA.addVertex(v4);
        EmptyVertex v5 = new EmptyVertex(5);
        v5.addAP(APCB);
        v5.addAP(APCB);
        graphA.addVertex(v5);

        graphA.addEdge(new DENOPTIMEdge(v0.getAP(1), v1.getAP(0)));
        graphA.addEdge(new DENOPTIMEdge(v1.getAP(1), v2.getAP(0)));
        graphA.addEdge(new DENOPTIMEdge(v2.getAP(1), v3.getAP(0)));
        graphA.addEdge(new DENOPTIMEdge(v3.getAP(1), v4.getAP(0)));
        graphA.addEdge(new DENOPTIMEdge(v4.getAP(1), v5.getAP(0)));
        
        graphA.renumberGraphVertices();
        
        return graphA;
    }
    
//------------------------------------------------------------------------------

    /**
     * Produced a graph like this:
     * <pre>
     * v0(A)-(A)v1(A)-(A)v2
     * </pre>
     * You must run {@link #prepare()} before asking this class for any graph.
     */
    static DENOPTIMGraph makeGraphB() throws DENOPTIMException
    {
        DENOPTIMGraph graphB = new DENOPTIMGraph();
        EmptyVertex v0 = new EmptyVertex(0);
        v0.setBuildingBlockType(BBType.SCAFFOLD);
        v0.addAP(APCA);
        graphB.addVertex(v0);
        EmptyVertex v1 = new EmptyVertex(1);
        v1.addAP(APCA);
        v1.addAP(APCA);
        graphB.addVertex(v1);
        EmptyVertex v2 = new EmptyVertex(2);
        v2.addAP(APCA);
        graphB.addVertex(v2);       

        graphB.addEdge(new DENOPTIMEdge(v0.getAP(0), v1.getAP(0)));
        graphB.addEdge(new DENOPTIMEdge(v1.getAP(1), v2.getAP(0)));

        graphB.renumberGraphVertices();
        
        return graphB;
    }
    
//------------------------------------------------------------------------------

    /**
     * Produced a graph like this:
     * <pre>
     * -(C)v0(C)-(C)v1(A)-(A)v2
     * </pre>
     * You must run {@link #prepare()} before asking this class for any graph.
     */
    static DENOPTIMGraph makeGraphC() throws DENOPTIMException
    {
        DENOPTIMGraph graphC = new DENOPTIMGraph();
        EmptyVertex v0 = new EmptyVertex(0);
        v0.setBuildingBlockType(BBType.SCAFFOLD);
        v0.addAP(APCC);
        v0.addAP(APCC);
        graphC.addVertex(v0);
        EmptyVertex v1 = new EmptyVertex(1);
        v1.addAP(APCC);
        v1.addAP(APCA);
        graphC.addVertex(v1);
        EmptyVertex v2 = new EmptyVertex(2);
        v2.addAP(APCA);
        graphC.addVertex(v2);       

        graphC.addEdge(new DENOPTIMEdge(v0.getAP(1), v1.getAP(0)));
        graphC.addEdge(new DENOPTIMEdge(v1.getAP(1), v2.getAP(0)));

        graphC.renumberGraphVertices();
        
        return graphC;
    }
    
//------------------------------------------------------------------------------

    /**
     * Produced a graph like this:
     * <pre>
     * v0(D)-(D)v1
     * </pre>
     * You must run {@link #prepare()} before asking this class for any graph.
     */
    static DENOPTIMGraph makeGraphD() throws DENOPTIMException
    {
        DENOPTIMGraph graphD = new DENOPTIMGraph();
        EmptyVertex v0 = new EmptyVertex(0);
        v0.setBuildingBlockType(BBType.SCAFFOLD);
        v0.addAP(APCD);
        graphD.addVertex(v0);
        EmptyVertex v1 = new EmptyVertex(1);
        v1.addAP(APCD);  

        graphD.addEdge(new DENOPTIMEdge(v0.getAP(0), v1.getAP(0)));

        graphD.renumberGraphVertices();
        
        return graphD;
    }
    
//------------------------------------------------------------------------------

    /**
     * Produced a graph like this:
     * <pre>
     *  v0(B)-(B)v1(A)-(A)v2(B)-(B)v3(A)-(A)v4(B)-(B)v5
     * </pre>
     * You must run {@link #prepare()} before asking this class for any graph.
     */
    static DENOPTIMGraph makeGraphE() throws DENOPTIMException
    {
        DENOPTIMGraph graphA = new DENOPTIMGraph();
        EmptyVertex v0 = new EmptyVertex(0);
        v0.setBuildingBlockType(BBType.SCAFFOLD);
        v0.addAP(APCB);
        graphA.addVertex(v0);
        EmptyVertex v1 = new EmptyVertex();
        v1.addAP(APCB);
        v1.addAP(APCA);
        graphA.addVertex(v1);
        EmptyVertex v2 = new EmptyVertex();
        v2.addAP(APCA);
        v2.addAP(APCB);
        graphA.addVertex(v2);
        EmptyVertex v3 = new EmptyVertex();
        v3.addAP(APCB);
        v3.addAP(APCA);
        graphA.addVertex(v3);
        EmptyVertex v4 = new EmptyVertex();
        v4.addAP(APCA);
        v4.addAP(APCB);
        graphA.addVertex(v4);
        EmptyVertex v5 = new EmptyVertex();
        v5.addAP(APCB);
        graphA.addVertex(v5);

        graphA.addEdge(new DENOPTIMEdge(v0.getAP(0), v1.getAP(0)));
        graphA.addEdge(new DENOPTIMEdge(v1.getAP(1), v2.getAP(0)));
        graphA.addEdge(new DENOPTIMEdge(v2.getAP(1), v3.getAP(0)));
        graphA.addEdge(new DENOPTIMEdge(v3.getAP(1), v4.getAP(0)));
        graphA.addEdge(new DENOPTIMEdge(v4.getAP(1), v5.getAP(0)));
        
        return graphA;
    }
    
//------------------------------------------------------------------------------

    /**
     * Produced a graph like this:
     * <pre>
     *  -(A)v0(A)-(A)v1(A)-(A)v2(A)-(A)v3(A)-(A)v4(A)-(A)v5(A)-(A)v6(A)-(A)v7(A)-
     * </pre>
     * 
     * You must run {@link #prepare()} before asking this class for any graph.
     */
    static DENOPTIMGraph makeGraphF() throws DENOPTIMException
    {
        DENOPTIMGraph graphA = new DENOPTIMGraph();
        EmptyVertex v0 = new EmptyVertex(0);
        v0.setBuildingBlockType(BBType.SCAFFOLD);
        v0.addAP(APCA);
        v0.addAP(APCA);
        graphA.addVertex(v0);
        EmptyVertex v1 = new EmptyVertex(1);
        v1.addAP(APCA);
        v1.addAP(APCA);
        graphA.addVertex(v1);
        EmptyVertex v2 = new EmptyVertex(2);
        v2.addAP(APCA);
        v2.addAP(APCA);
        graphA.addVertex(v2);
        EmptyVertex v3 = new EmptyVertex(3);
        v3.addAP(APCA);
        v3.addAP(APCA);
        graphA.addVertex(v3);
        EmptyVertex v4 = new EmptyVertex(4);
        v4.addAP(APCA);
        v4.addAP(APCA);
        graphA.addVertex(v4);
        EmptyVertex v5 = new EmptyVertex(5);
        v5.addAP(APCA);
        v5.addAP(APCA);
        graphA.addVertex(v5);
        EmptyVertex v6 = new EmptyVertex(6);
        v6.addAP(APCA);
        v6.addAP(APCA);
        graphA.addVertex(v6);
        EmptyVertex v7 = new EmptyVertex(7);
        v7.addAP(APCA);
        v7.addAP(APCA);
        graphA.addVertex(v7);

        graphA.addEdge(new DENOPTIMEdge(v0.getAP(1), v1.getAP(0)));
        graphA.addEdge(new DENOPTIMEdge(v1.getAP(1), v2.getAP(0)));
        graphA.addEdge(new DENOPTIMEdge(v2.getAP(1), v3.getAP(0)));
        graphA.addEdge(new DENOPTIMEdge(v3.getAP(1), v4.getAP(0)));
        graphA.addEdge(new DENOPTIMEdge(v4.getAP(1), v5.getAP(0)));
        graphA.addEdge(new DENOPTIMEdge(v5.getAP(1), v6.getAP(0)));
        graphA.addEdge(new DENOPTIMEdge(v6.getAP(1), v7.getAP(0)));
        
        graphA.renumberGraphVertices();
        
        return graphA;
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetMinMax() throws Exception
    {
        prepare();
        Population pop = new Population();
        
        DENOPTIMGraph g1 = makeGraphA();
        Candidate c1 = new Candidate("C1",g1);
        c1.setFitness(-1.0);
        pop.add(c1);
        
        DENOPTIMGraph g2 = makeGraphB();
        Candidate c2 = new Candidate("C2",g2);
        c2.setFitness(0.02);
        pop.add(c2);
        
        DENOPTIMGraph g3 = makeGraphB();
        Candidate c3 = new Candidate("C3",g3);
        c3.setFitness(2.0);
        pop.add(c3);
        
        DENOPTIMGraph g4 = makeGraphC();
        Candidate c4 = new Candidate("C4",g4);
        c4.setFitness(0.5);
        pop.add(c4);
        
        DENOPTIMGraph g5 = makeGraphD();
        Candidate c5 = new Candidate("C5",g5);
        c5.setFitness(-0.5);
        pop.add(c5);
        
        double trsh = 0.001;
        assertTrue(Math.abs(pop.getMinFitness()-(-1.0)) < trsh, 
                "getting min fitness");
        assertTrue(Math.abs(pop.getMaxFitness()-(2.0)) < trsh, 
                "getting max fitness");
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testIsInPercentile() throws Exception
    {
        prepare();
        Population pop = new Population();
        
        DENOPTIMGraph g1 = makeGraphA();
        Candidate c1 = new Candidate("C1",g1);
        c1.setFitness(20.0);
        pop.add(c1);
        
        DENOPTIMGraph g2 = makeGraphB();
        Candidate c2 = new Candidate("C2",g2);
        c2.setFitness(120);
        pop.add(c2);
        
        assertTrue(pop.isWithinPercentile(116, 0.05), "116 is in 5%");
        assertTrue(pop.isWithinPercentile(70.1, 0.5), "70.1 is in 50%");
        assertFalse(pop.isWithinPercentile(69, 0.5), "69 is not in 50%");
        assertFalse(pop.isWithinPercentile(114, 0.05), "114 is not in 5%");
        
        pop = new Population();
        
        DENOPTIMGraph g1b = makeGraphA();
        Candidate c1b = new Candidate("C1",g1b);
        c1b.setFitness(-20.0);
        pop.add(c1b);
        
        DENOPTIMGraph g2b = makeGraphB();
        Candidate c2b = new Candidate("C2",g2b);
        c2b.setFitness(80);
        pop.add(c2b);
        
        assertTrue(pop.isWithinPercentile(76, 0.05), "76 is in 5%");
        assertTrue(pop.isWithinPercentile(30.1, 0.5), "30.1 is in 50%");
        assertFalse(pop.isWithinPercentile(29, 0.5), "29 is not in 50%");
        assertFalse(pop.isWithinPercentile(74, 0.05), "74 is not in 5%");
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testPopulationVersion() throws Exception
    {
        prepare();
        Population pop = new Population();
        int v0 = pop.getVersionID();
        
        DENOPTIMGraph g1 = makeGraphA();
        Candidate c1 = new Candidate("C1",g1);
        pop.add(c1);
        int v1 = pop.getVersionID();
        assertTrue(v1>v0,"Version change 1");
        
        DENOPTIMGraph g2 = makeGraphB();
        Candidate c2 = new Candidate("C2",g2);
        pop.add(c2);
        int v2 = pop.getVersionID();
        assertTrue(v2>v1,"Version change 2");
        
        DENOPTIMGraph g3 = makeGraphB();
        Candidate c3 = new Candidate("C3",g3);
        pop.add(c3);
        int v3 = pop.getVersionID();
        assertTrue(v3>v2,"Version change 3");
        
        pop.remove(c1);
        int v4 = pop.getVersionID();
        assertTrue(v4>v3,"Version change 4");
        
        DENOPTIMGraph g4 = makeGraphB();
        Candidate c4 = new Candidate("C4",g4);
        pop.add(0,c4);
        int v5 = pop.getVersionID();
        assertTrue(v5>v4,"Version change 5");
        
        DENOPTIMGraph g5 = makeGraphC();
        Candidate c5 = new Candidate("C5",g5);
        pop.set(1,c5);
        int v6 = pop.getVersionID();
        assertTrue(v6>v5,"Version change 6");        
    }
    
//------------------------------------------------------------------------------
    
    //TODO-gg @Test
    public void testGetXoverSites_FixedStructureTmpls() throws Exception
    {
        prepare();
        Population population = new Population();

        DENOPTIMGraph[] pair = getPairOfTestGraphsB();
        DENOPTIMGraph gA = pair[0];
        DENOPTIMGraph gB = pair[1];
        
        Candidate cA = new Candidate("CA",gA);
        population.add(cA);        

        Candidate cB = new Candidate("CB",gB);
        population.add(cB);
        
        ArrayList<Candidate> partners = population.getXoverPartners(cA, 
                new ArrayList<Candidate>(Arrays.asList(cB)));
        assertTrue(partners.contains(cB));
        
        //TODO-gg del
        DenoptimIO.writeGraphToSDF(new File("/tmp/graphA.sdf"), gA, false);
        DenoptimIO.writeGraphToSDF(new File("/tmp/graphB.sdf"), gB, false);
        
        List<XoverSite> subGraphSeeds = population.getXoverSites(cA,cB);
        assertEquals(0,subGraphSeeds.size());
        
        
        /*
        // We hard-code the selection of a pair of xover sites as to choose
        // sites that are inside the template vertexes, AND sites that are
        // peculiar. This one, is be a place where xover that preserve the 
        // structure is not possible
        DENOPTIMVertex subGraphSeedA = subGraphSeeds.get(4)[0];
        DENOPTIMVertex subGraphSeedB = subGraphSeeds.get(4)[1];
        
        // We do as many independent attempts as the number of possible end points
        // This, just to test multiple possibilities.
        for (int i=0; i<sizeXoverSiteList; i++)
        {
            List<List<DENOPTIMVertex>> subGraphEnds = 
                    population.getSwappableSubGraphEnds(cA,cB,subGraphSeedA.getGraphOwner(),subGraphSeedA,
                            subGraphSeedB.getGraphOwner(),subGraphSeedB, null);
            
            DENOPTIMGraph subGraphA = gA.extractSubgraph(subGraphSeedA, 
                    subGraphEnds.get(0), false);
            DENOPTIMGraph subGraphB = gB.extractSubgraph(subGraphSeedB,
                    subGraphEnds.get(1),false);
            
            assertEquals(subGraphA.getVertexCount(),subGraphB.getVertexCount());
        }
        */
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Builds a pair of graphs that contain templates with fixed structure
     * contract.
     * 
     * First Graph structure:
     * <pre>
     *               (C)--(C)-v1
     *              /
     * v0-(A)--(A)-T1-(B)--(B)-v2-(C)--(C)-v3
     * </pre>
     * where template T1 is
     * <pre>
     *                     
     *    (A)-v0-(A)--(A)-v1-(B)--(B)-v2-(C)
     *         |           |
     *        (A)         (A)--(A)-v5
     *         |                    |
     *         |                   chord
     *         |                    |
     *        (A)         (A)--(A)-v6
     *         |           |
     *        v3-(A)--(A)-v4-(B)
     * </pre>
     * 
     * The second Graph structure:
     * <pre>
     * v0-(A)--(A)-T2-(B)--(B)-v2-(C)--(C)-v3
     * </pre>
     * where template T2 is
     * <pre>
     *                     
     *    (A)-v0-(A)--(A)-v1-(B)--(B)-v2
     *         |           
     *        (A) 
     *         |                 
     *        (A)         (A)--(A)-v5
     *         |           |
     *        v3-(A)--(A)-v4-(B)
     * </pre>
     */
    private DENOPTIMGraph[] getPairOfTestGraphsB() throws Exception
    {   
        // Prepare special building block: template T1
        EmptyVertex v0 = new EmptyVertex(0);
        v0.addAP(APCA);
        v0.addAP(APCA);
        v0.addAP(APCA);
        v0.setProperty("Label", "tv0");
        
        EmptyVertex v1 = new EmptyVertex(1);
        v1.addAP(APCA);
        v1.addAP(APCA);
        v1.addAP(APCB);
        v1.setProperty("Label", "tv1");
        
        EmptyVertex v2 = new EmptyVertex(2);
        v2.addAP(APCB);
        v2.addAP(APCC);
        v2.setProperty("Label", "tv2");
        
        EmptyVertex v3 = new EmptyVertex(3);
        v3.addAP(APCA);
        v3.addAP(APCA);
        v3.setProperty("Label", "tv3");

        EmptyVertex v4 = new EmptyVertex(4);
        v4.addAP(APCA);
        v4.addAP(APCB);
        v4.addAP(APCA);
        v4.setProperty("Label", "tv4");
        
        EmptyVertex v5 = new EmptyVertex(5);
        v5.addAP(APCA);
        v5.setProperty("Label", "tv5");
        v5.setAsRCV(true);
        
        EmptyVertex v6 = new EmptyVertex(6);
        v6.addAP(APCA);
        v6.setProperty("Label", "tv6");
        v6.setAsRCV(true);
        
        DENOPTIMGraph g = new DENOPTIMGraph();
        g.addVertex(v0);
        g.setGraphId(-1);
        g.appendVertexOnAP(v0.getAP(1), v1.getAP(0));
        g.appendVertexOnAP(v1.getAP(2), v2.getAP(0));
        g.appendVertexOnAP(v1.getAP(1), v5.getAP(0));
        g.appendVertexOnAP(v0.getAP(0), v3.getAP(0));
        g.appendVertexOnAP(v3.getAP(1), v4.getAP(0));
        g.appendVertexOnAP(v4.getAP(2), v6.getAP(0));
        g.addRing(v5, v6, BondType.TRIPLE);
        
        DENOPTIMTemplate t1 = new DENOPTIMTemplate(BBType.NONE);
        t1.setInnerGraph(g);
        t1.setProperty("Label", "t1");
        t1.setContractLevel(ContractLevel.FIXED_STRUCT);
        
        // Assemble the first graph: graphA
        EmptyVertex m0 = new EmptyVertex(100);
        m0.addAP(APCA);
        m0.setProperty("Label", "m100");
        
        EmptyVertex m1 = new EmptyVertex(101);
        m1.addAP(APCC);
        m1.setProperty("Label", "m101");
        
        EmptyVertex m2 = new EmptyVertex(102);
        m2.addAP(APCB);
        m2.addAP(APCC);
        m2.setProperty("Label", "m102");
        
        EmptyVertex m3 = new EmptyVertex(103);
        m3.addAP(APCC);
        m3.setProperty("Label", "m103");
        
        DENOPTIMGraph graphA = new DENOPTIMGraph();
        graphA.addVertex(m0);
        graphA.appendVertexOnAP(m0.getAP(0), t1.getAP(0)); // A on T1
        graphA.appendVertexOnAP(t1.getAP(1), m1.getAP(0)); // C on T1
        graphA.appendVertexOnAP(t1.getAP(2), m2.getAP(0));
        graphA.appendVertexOnAP(m2.getAP(1), m3.getAP(0));
        graphA.setGraphId(11111);
        
        // Done with GraphA!
        
        // Prepare special building block: template T2
        EmptyVertex w0 = new EmptyVertex(0);
        w0.addAP(APCA);
        w0.addAP(APCA);
        w0.addAP(APCA);
        w0.setProperty("Label", "tw0");
        
        EmptyVertex w1 = new EmptyVertex(1);
        w1.addAP(APCA);
        w1.addAP(APCB);
        w1.setProperty("Label", "tw1");
        
        EmptyVertex w2 = new EmptyVertex(2);
        w2.addAP(APCB);
        w2.setProperty("Label", "tw2");
        
        EmptyVertex w3 = new EmptyVertex(3);
        w3.addAP(APCA);
        w3.addAP(APCA);
        w3.setProperty("Label", "tw3");

        EmptyVertex w4 = new EmptyVertex(4);
        w4.addAP(APCA);
        w4.addAP(APCB);
        w4.addAP(APCA);
        w4.setProperty("Label", "tw4");
        
        EmptyVertex w5 = new EmptyVertex(5);
        w5.addAP(APCA);
        w5.setProperty("Label", "tw5");
        
        DENOPTIMGraph g2 = new DENOPTIMGraph();
        g2.addVertex(w0);
        g2.setGraphId(-1);
        g2.appendVertexOnAP(w0.getAP(1), w1.getAP(0));
        g2.appendVertexOnAP(w1.getAP(1), w2.getAP(0));
        g2.appendVertexOnAP(w0.getAP(0), w3.getAP(0));
        g2.appendVertexOnAP(w3.getAP(1), w4.getAP(0));
        g2.appendVertexOnAP(w4.getAP(2), w5.getAP(0));

        DENOPTIMTemplate t2 = new DENOPTIMTemplate(BBType.NONE);
        t2.setInnerGraph(g2);
        t2.setProperty("Label", "t2");
        t2.setContractLevel(ContractLevel.FIXED_STRUCT);
        
        // Assemble the first graph: graphA
        EmptyVertex n0 = new EmptyVertex(200);
        n0.addAP(APCA);
        n0.setProperty("Label", "m200");
        
        EmptyVertex n2 = new EmptyVertex(202);
        n2.addAP(APCB);
        n2.addAP(APCC);
        n2.setProperty("Label", "m202");
        
        EmptyVertex n3 = new EmptyVertex(203);
        n3.addAP(APCC);
        n3.setProperty("Label", "m203");
        
        DENOPTIMGraph graphB = new DENOPTIMGraph();
        graphB.addVertex(n0);
        graphB.appendVertexOnAP(n0.getAP(0), t2.getAP(0)); // A on T2
        graphB.appendVertexOnAP(t2.getAP(1), n2.getAP(0)); // B on T2
        graphB.appendVertexOnAP(n2.getAP(1), n3.getAP(0));
        graphB.setGraphId(22222);
        
        // Done with GraphB!
        
        DENOPTIMGraph[] pair = new DENOPTIMGraph[2];
        pair[0] = graphA;
        pair[1] = graphB;
        
        return pair;
    }
 
//------------------------------------------------------------------------------

}
