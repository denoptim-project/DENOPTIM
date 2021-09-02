package denoptim.fragspace;

import denoptim.exception.DENOPTIMException;
import denoptim.molecule.*;
import denoptim.molecule.DENOPTIMEdge.BondType;

import org.junit.jupiter.api.Test;

import denoptim.molecule.DENOPTIMVertex.BBType;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test
 * 
 * @author Marco Foscato
 */

public class GraphLinkFinderTest
{
    private static APClass APCA, APCB, APCC, APCD;
    
//------------------------------------------------------------------------------
    
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
        lstC.add(APCB);
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
         *    C |     |  T  |  T  |   |
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
        
        FragmentSpace.setScaffoldLibrary(new ArrayList<DENOPTIMVertex>());
        FragmentSpace.setFragmentLibrary(new ArrayList<DENOPTIMVertex>());
        
        DENOPTIMVertex s = new EmptyVertex();
        s.setBuildingBlockType(BBType.SCAFFOLD);
        s.addAP(0, 1, 1, APCA);
        FragmentSpace.appendVertexToLibrary(s, BBType.SCAFFOLD,
                FragmentSpace.getScaffoldLibrary());
        
        DENOPTIMVertex v0 = new EmptyVertex();
        v0.setBuildingBlockType(BBType.FRAGMENT);
        v0.addAP(0, 1, 1, APCA);
        v0.addAP(0, 1, 1, APCB);
        v0.addAP(0, 1, 1, APCB);
        FragmentSpace.appendVertexToLibrary(v0, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        DENOPTIMVertex v1 = new EmptyVertex();
        v1.setBuildingBlockType(BBType.FRAGMENT);
        v1.addAP(0, 1, 1, APCB);
        FragmentSpace.appendVertexToLibrary(v1, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        DENOPTIMVertex v2 = new EmptyVertex();
        v2.setBuildingBlockType(BBType.FRAGMENT);
        v2.addAP(0, 1, 1, APCA);
        v2.addAP(0, 1, 1, APCA);
        FragmentSpace.appendVertexToLibrary(v2, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        DENOPTIMVertex v3 = new EmptyVertex();
        v3.setBuildingBlockType(BBType.FRAGMENT);
        v3.addAP(0, 1, 1, APCB);
        v3.addAP(0, 1, 1, APCB);
        FragmentSpace.appendVertexToLibrary(v3, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        DENOPTIMVertex v4 = new EmptyVertex();
        v4.setBuildingBlockType(BBType.FRAGMENT);
        v4.addAP(0, 1, 1, APCA);
        v4.addAP(0, 1, 1, APCB);
        v4.addAP(0, 1, 1, APCB);
        FragmentSpace.appendVertexToLibrary(v4, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        DENOPTIMVertex v5 = new EmptyVertex();
        v5.setBuildingBlockType(BBType.FRAGMENT);
        v5.addAP(0, 1, 1, APCA);
        v5.addAP(0, 1, 1, APCC);
        v5.addAP(0, 1, 1, APCB);
        FragmentSpace.appendVertexToLibrary(v5, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        DENOPTIMVertex v6 = new EmptyVertex();
        v6.setBuildingBlockType(BBType.FRAGMENT);
        v6.addAP(0, 1, 1, APCD);
        v6.addAP(0, 1, 1, APCD);
        FragmentSpace.appendVertexToLibrary(v6, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        DENOPTIMVertex v7 = new EmptyVertex();
        v7.setBuildingBlockType(BBType.FRAGMENT);
        v7.addAP(0, 1, 1, APCA);
        FragmentSpace.appendVertexToLibrary(v7, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        DENOPTIMVertex v8 = new EmptyVertex();
        v8.setBuildingBlockType(BBType.FRAGMENT);
        v8.addAP(0, 1, 1, APCB);
        FragmentSpace.appendVertexToLibrary(v8, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        DENOPTIMVertex v9 = new EmptyVertex();
        v9.setBuildingBlockType(BBType.FRAGMENT);
        v9.addAP(0, 1, 1, APCA);
        v9.addAP(0, 1, 1, APCA);
        v9.addAP(0, 1, 1, APCB);
        v9.addAP(0, 1, 1, APCB);
        FragmentSpace.appendVertexToLibrary(v9, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        DENOPTIMVertex v10 = new EmptyVertex();
        v10.setBuildingBlockType(BBType.FRAGMENT);
        v10.addAP(0, 1, 1, APCA);
        v10.addAP(0, 1, 1, APCC);
        v10.addAP(0, 1, 1, APCD);
        v10.addAP(0, 1, 1, APCB);
        FragmentSpace.appendVertexToLibrary(v10, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        DENOPTIMVertex v11 = new EmptyVertex();
        v11.setBuildingBlockType(BBType.FRAGMENT);
        v11.addAP(0, 1, 1, APCA);
        v11.addAP(0, 1, 1, APCD);
        v11.addAP(0, 1, 1, APCA);
        v11.addAP(0, 1, 1, APCA);
        v11.addAP(0, 1, 1, APCC);
        v11.addAP(0, 1, 1, APCB);
        v11.addAP(0, 1, 1, APCB);
        v11.addAP(0, 1, 1, APCB);
        FragmentSpace.appendVertexToLibrary(v11, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
    }

//------------------------------------------------------------------------------

    /**
     *  Creates a test graph that looks like this: v0(A)-(A)v1(B)-(C)v2
     * @throws DENOPTIMException 
     *  
     */
    private DENOPTIMGraph makeTestGraphA() throws DENOPTIMException
    {
        DENOPTIMGraph graph = new DENOPTIMGraph();
        DENOPTIMVertex s = FragmentSpace.getVertexFromLibrary(
                BBType.SCAFFOLD,0);
        graph.addVertex(s);
        DENOPTIMVertex v0 = FragmentSpace.getVertexFromLibrary(
                BBType.FRAGMENT,0);
        graph.addVertex(v0);
        DENOPTIMVertex v1 = FragmentSpace.getVertexFromLibrary(
                BBType.FRAGMENT,1);
        graph.addVertex(v1);
        graph.addEdge(new DENOPTIMEdge(s.getAP(0), v0.getAP(0)));
        graph.addEdge(new DENOPTIMEdge(v0.getAP(1), v1.getAP(0)));
        graph.renumberGraphVertices();
        return graph;
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testSimple() throws Exception
    {
        prepare();
        DENOPTIMGraph graph = makeTestGraphA();
        GraphLinkFinder glf = new GraphLinkFinder(graph.getVertexAtPosition(1),
                true); 
        // NB: the boolean makes the search complete (within the limits the 
        // prevent combinatorial explosion)
        
        // These are the expected numbers of AP mappings found for the three
        // vertexes that can be alternative links. 
        Map<Integer,Integer> expected = new HashMap<Integer,Integer>();
        expected.put(4, 2);
        expected.put(9, 4);
        expected.put(11, 18);
        
        Map<DENOPTIMVertex, List<Map<Integer, Integer>>> allAltLinks = 
                glf.getAllAlternativesFound();
        Set<DENOPTIMVertex> keys = allAltLinks.keySet();
        for (DENOPTIMVertex k : keys)
        {
            int bbId = k.getBuildingBlockId();
            assertTrue(expected.containsKey(bbId), "Vertex with building block "
                    + "ID '" + bbId + "' should not among the results.");
            assertEquals(expected.get(bbId), allAltLinks.get(k).size(),
                    "Number of APmapping is wrong for bbId '" + bbId + "'.");
            //System.out.println(" -> "+k.getBuildingBlockType()+" "+
            //        k.getBuildingBlockId()+": "+glf.allCompatLinks.get(k));
        }
    }
    
//------------------------------------------------------------------------------
    
}
