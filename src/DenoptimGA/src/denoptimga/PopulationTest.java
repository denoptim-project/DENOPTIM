package denoptimga;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.io.DenoptimIO;
import denoptim.molecule.APClass;
import denoptim.molecule.Candidate;
import denoptim.molecule.DENOPTIMEdge;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.molecule.DENOPTIMVertex.BBType;
import denoptim.molecule.EmptyVertex;
import denoptim.molecule.DENOPTIMEdge.BondType;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


/**
 * Unit test
 * 
 * @author Marco Foscato
 */

public class PopulationTest
{
    private static APClass APCA, APCB, APCC, APCD;
    
//------------------------------------------------------------------------------
    
    @BeforeEach
    private void prepare() throws DENOPTIMException
    {
        APCA = APClass.make("A", 0);
        APCB = APClass.make("B", 0);
        APCC = APClass.make("C", 0);
        APCD = APClass.make("D", 99);
        
        HashMap<String,BondType> boMap = new HashMap<String,BondType>();
        boMap.put("A",BondType.SINGLE);
        boMap.put("B",BondType.SINGLE);
        boMap.put("C",BondType.SINGLE);
        boMap.put("D",BondType.DOUBLE);
        
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
        
        FragmentSpace.setBondOrderMap(boMap);
        FragmentSpace.setCompatibilityMatrix(cpMap);
        FragmentSpace.setCappingMap(capMap);
        FragmentSpace.setForbiddenEndList(forbEnds);
        FragmentSpace.setAPclassBasedApproach(true);
    }

//------------------------------------------------------------------------------
    
    @Test
    public void testXOverComatibility() throws Exception
    {
        Population pop = new Population();
        
        DENOPTIMGraph g1 = makeGraphA();
        Candidate c1 = new Candidate("C1",g1);
        pop.add(c1);
        
        DENOPTIMGraph g2 = makeGraphB();
        Candidate c2 = new Candidate("C2",g2);
        pop.add(c2);
        
        DENOPTIMGraph g3 = makeGraphB();
        Candidate c3 = new Candidate("C3",g3);
        pop.add(c3);
        
        DENOPTIMGraph g4 = makeGraphC();
        Candidate c4 = new Candidate("C4",g4);
        pop.add(c4);
        
        DENOPTIMGraph g5 = makeGraphD();
        Candidate c5 = new Candidate("C5",g5);
        pop.add(c5);
        
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
        expectedForC1.put(c2, 6);
        expectedForC1.put(c3, 6);
        expectedForC1.put(c4, 3);
        expected.put(c1, expectedForC1);
        Map<Candidate,Integer> expectedForC2 = new HashMap<Candidate,Integer>();
        expectedForC2.put(c1, 6);
        //expectedForC2.put(c3, 4); NO xover between candidates with same graph!
        expectedForC2.put(c4, 2);
        expected.put(c2, expectedForC2);
        Map<Candidate,Integer> expectedForC3 = new HashMap<Candidate,Integer>();
        expectedForC3.put(c1, 6);
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
     *  -(A)v0(A)-(A)v1(A)-(A)v2(A)-(A)v3(B)-(B)v4(B)-(B)v5(B)-
     *  
     */
    private DENOPTIMGraph makeGraphA() throws DENOPTIMException
    {
        DENOPTIMGraph graphA = new DENOPTIMGraph();
        DENOPTIMVertex v0 = new EmptyVertex(0);
        v0.setBuildingBlockType(BBType.SCAFFOLD);
        v0.addAP(0, 1, 1, APCA);
        v0.addAP(0, 1, 1, APCA);
        graphA.addVertex(v0);
        DENOPTIMVertex v1 = new EmptyVertex(1);
        v1.addAP(0, 1, 1, APCA);
        v1.addAP(0, 1, 1, APCA);
        graphA.addVertex(v1);
        DENOPTIMVertex v2 = new EmptyVertex(2);
        v2.addAP(0, 1, 1, APCA);
        v2.addAP(0, 1, 1, APCA);
        graphA.addVertex(v2);
        DENOPTIMVertex v3 = new EmptyVertex(3);
        v3.addAP(0, 1, 1, APCA);
        v3.addAP(0, 1, 1, APCB);
        graphA.addVertex(v3);
        DENOPTIMVertex v4 = new EmptyVertex(4);
        v4.addAP(0, 1, 1, APCB);
        v4.addAP(0, 1, 1, APCB);
        graphA.addVertex(v4);
        DENOPTIMVertex v5 = new EmptyVertex(5);
        v5.addAP(0, 1, 1, APCB);
        v5.addAP(0, 1, 1, APCB);
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
     *  v0(A)-(A)v1(A)-(A)v2
     */
    private DENOPTIMGraph makeGraphB() throws DENOPTIMException
    {
        DENOPTIMGraph graphB = new DENOPTIMGraph();
        DENOPTIMVertex v0 = new EmptyVertex(0);
        v0.setBuildingBlockType(BBType.SCAFFOLD);
        v0.addAP(0, 1, 1, APCA);
        graphB.addVertex(v0);
        DENOPTIMVertex v1 = new EmptyVertex(1);
        v1.addAP(0, 1, 1, APCA);
        v1.addAP(0, 1, 1, APCA);
        graphB.addVertex(v1);
        DENOPTIMVertex v2 = new EmptyVertex(2);
        v2.addAP(0, 1, 1, APCA);
        graphB.addVertex(v2);       

        graphB.addEdge(new DENOPTIMEdge(v0.getAP(0), v1.getAP(0)));
        graphB.addEdge(new DENOPTIMEdge(v1.getAP(1), v2.getAP(0)));

        graphB.renumberGraphVertices();
        
        return graphB;
    }
    
//------------------------------------------------------------------------------

    /**
     *  -(C)v0(C)-(C)v1(A)-(A)v2
     */
    private DENOPTIMGraph makeGraphC() throws DENOPTIMException
    {
        DENOPTIMGraph graphC = new DENOPTIMGraph();
        DENOPTIMVertex v0 = new EmptyVertex(0);
        v0.setBuildingBlockType(BBType.SCAFFOLD);
        v0.addAP(0, 1, 1, APCC);
        v0.addAP(0, 1, 1, APCC);
        graphC.addVertex(v0);
        DENOPTIMVertex v1 = new EmptyVertex(1);
        v1.addAP(0, 1, 1, APCC);
        v1.addAP(0, 1, 1, APCA);
        graphC.addVertex(v1);
        DENOPTIMVertex v2 = new EmptyVertex(2);
        v2.addAP(0, 1, 1, APCA);
        graphC.addVertex(v2);       

        graphC.addEdge(new DENOPTIMEdge(v0.getAP(1), v1.getAP(0)));
        graphC.addEdge(new DENOPTIMEdge(v1.getAP(1), v2.getAP(0)));

        graphC.renumberGraphVertices();
        
        return graphC;
    }
    
//------------------------------------------------------------------------------

    /**
     *  v0(D)-(D)v1
     */
    private DENOPTIMGraph makeGraphD() throws DENOPTIMException
    {
        DENOPTIMGraph graphD = new DENOPTIMGraph();
        DENOPTIMVertex v0 = new EmptyVertex(0);
        v0.setBuildingBlockType(BBType.SCAFFOLD);
        v0.addAP(0, 1, 1, APCD);
        graphD.addVertex(v0);
        DENOPTIMVertex v1 = new EmptyVertex(1);
        v1.addAP(0, 1, 1, APCD);  

        graphD.addEdge(new DENOPTIMEdge(v0.getAP(0), v1.getAP(0)));

        graphD.renumberGraphVertices();
        
        return graphD;
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetMinMax() throws Exception
    {
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
      
}
