package denoptim.fragspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.jupiter.api.Test;

import denoptim.exception.DENOPTIMException;
import denoptim.graph.APClass;
import denoptim.graph.APMapping;
import denoptim.graph.DENOPTIMAttachmentPoint;
import denoptim.graph.DENOPTIMEdge;
import denoptim.graph.DENOPTIMEdge.BondType;
import denoptim.graph.DENOPTIMGraph;
import denoptim.graph.DENOPTIMVertex;
import denoptim.graph.DENOPTIMVertex.BBType;
import denoptim.graph.EmptyVertex;

/**
 * Unit test
 * 
 * @author Marco Foscato
 */

public class GraphLinkFinderTest
{
    private static APClass APCA, APCB, APCC, APCD, APCE, APCF;
    
//------------------------------------------------------------------------------
    
    private void prepare() throws DENOPTIMException
    {
        APCA = APClass.make("A", 0);
        APCB = APClass.make("B", 0);
        APCC = APClass.make("C", 0);
        APCD = APClass.make("D", 99);
        APCE = APClass.make("E", 13);
        APCF = APClass.make("F", 17);
        
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
        ArrayList<APClass> lstE = new ArrayList<APClass>();
        lstE.add(APCE);
        cpMap.put(APCE, lstE);
        
        
        /* Compatibility matrix
         * 
         *      |  A  |  B  |  C  | D | E | F
         *    ---------------------------------
         *    A |  T  |     |     |   |   |
         *    ---------------------------------
         *    B |     |  T  |  T  |   |   |
         *    ---------------------------------
         *    C |     |  T  |  T  |   |   |
         *    ---------------------------------
         *    D |     |     |     | T |   |
         *    ---------------------------------
         *    E |     |     |     |   | T |
         *    ---------------------------------
         *    F |     |     |     |   |   |
         *    
         *    NB: F has NONE!
         */
        
        HashMap<APClass,APClass> capMap = new HashMap<APClass,APClass>();
        HashSet<APClass> forbEnds = new HashSet<APClass>();
        
        FragmentSpace.setCompatibilityMatrix(cpMap);
        FragmentSpace.setCappingMap(capMap);
        FragmentSpace.setForbiddenEndList(forbEnds);
        FragmentSpace.setAPclassBasedApproach(true);
        
        FragmentSpace.setScaffoldLibrary(new ArrayList<DENOPTIMVertex>());
        FragmentSpace.setFragmentLibrary(new ArrayList<DENOPTIMVertex>());
        
        EmptyVertex s = new EmptyVertex();
        s.setBuildingBlockType(BBType.SCAFFOLD);
        s.addAP(APCA);
        FragmentSpace.appendVertexToLibrary(s, BBType.SCAFFOLD,
                FragmentSpace.getScaffoldLibrary());
        
        EmptyVertex v0 = new EmptyVertex();
        v0.setBuildingBlockType(BBType.FRAGMENT);
        v0.addAP(APCA);
        v0.addAP(APCB);
        v0.addAP(APCB);
        FragmentSpace.appendVertexToLibrary(v0, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        EmptyVertex v1 = new EmptyVertex();
        v1.setBuildingBlockType(BBType.FRAGMENT);
        v1.addAP(APCB);
        FragmentSpace.appendVertexToLibrary(v1, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        EmptyVertex v2 = new EmptyVertex();
        v2.setBuildingBlockType(BBType.FRAGMENT);
        v2.addAP(APCA);
        v2.addAP(APCA);
        FragmentSpace.appendVertexToLibrary(v2, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        EmptyVertex v3 = new EmptyVertex();
        v3.setBuildingBlockType(BBType.FRAGMENT);
        v3.addAP(APCB);
        v3.addAP(APCB);
        FragmentSpace.appendVertexToLibrary(v3, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        EmptyVertex v4 = new EmptyVertex();
        v4.setBuildingBlockType(BBType.FRAGMENT);
        v4.addAP(APCA);
        v4.addAP(APCB);
        v4.addAP(APCB);
        FragmentSpace.appendVertexToLibrary(v4, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        EmptyVertex v5 = new EmptyVertex();
        v5.setBuildingBlockType(BBType.FRAGMENT);
        v5.addAP(APCA);
        v5.addAP(APCC);
        v5.addAP(APCB);
        FragmentSpace.appendVertexToLibrary(v5, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        EmptyVertex v6 = new EmptyVertex();
        v6.setBuildingBlockType(BBType.FRAGMENT);
        v6.addAP(APCD);
        v6.addAP(APCD);
        FragmentSpace.appendVertexToLibrary(v6, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        EmptyVertex v7 = new EmptyVertex();
        v7.setBuildingBlockType(BBType.FRAGMENT);
        v7.addAP(APCA);
        FragmentSpace.appendVertexToLibrary(v7, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        EmptyVertex v8 = new EmptyVertex();
        v8.setBuildingBlockType(BBType.FRAGMENT);
        v8.addAP(APCB);
        FragmentSpace.appendVertexToLibrary(v8, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        EmptyVertex v9 = new EmptyVertex();
        v9.setBuildingBlockType(BBType.FRAGMENT);
        v9.addAP(APCA);
        v9.addAP(APCA);
        v9.addAP(APCB);
        v9.addAP(APCB);
        FragmentSpace.appendVertexToLibrary(v9, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        EmptyVertex v10 = new EmptyVertex();
        v10.setBuildingBlockType(BBType.FRAGMENT);
        v10.addAP(APCA);
        v10.addAP(APCC);
        v10.addAP(APCD);
        v10.addAP(APCB);
        FragmentSpace.appendVertexToLibrary(v10, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        EmptyVertex v11 = new EmptyVertex();
        v11.setBuildingBlockType(BBType.FRAGMENT);
        v11.addAP(APCA);
        v11.addAP(APCD);
        v11.addAP(APCA);
        v11.addAP(APCA);
        v11.addAP(APCC);
        v11.addAP(APCB);
        v11.addAP(APCB);
        v11.addAP(APCB);
        FragmentSpace.appendVertexToLibrary(v11, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        EmptyVertex v12 = new EmptyVertex();
        v12.setBuildingBlockType(BBType.FRAGMENT);
        v12.addAP(APCE);
        v12.addAP(APCE);
        v12.addAP(APCE);
        FragmentSpace.appendVertexToLibrary(v12, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        EmptyVertex v13 = new EmptyVertex();
        v13.setBuildingBlockType(BBType.FRAGMENT);
        v13.addAP(APCF);
        v13.addAP(APCB);
        v13.addAP(APCA);
        FragmentSpace.appendVertexToLibrary(v13, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        FragmentSpaceUtils.groupAndClassifyFragments(true);
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

    /**
     *  Creates a test graph that looks like this: v0(E)-(E)v1
     * @throws DENOPTIMException 
     *  
     */
    private DENOPTIMGraph makeTestGraphE() throws DENOPTIMException
    {
        DENOPTIMGraph graph = new DENOPTIMGraph();
        DENOPTIMVertex v0 = FragmentSpace.getVertexFromLibrary(
                BBType.FRAGMENT,12);
        graph.addVertex(v0);
        DENOPTIMVertex v1 = FragmentSpace.getVertexFromLibrary(
                BBType.FRAGMENT,12);
        graph.addVertex(v1);
        graph.addEdge(new DENOPTIMEdge(v0.getAP(1), v1.getAP(1)));
        graph.renumberGraphVertices();
        return graph;
    }
    
//------------------------------------------------------------------------------

    /**
     *  Creates a test graph that looks like this: v0(F)-(F)v1
     * @throws DENOPTIMException 
     *  
     */
    private DENOPTIMGraph makeTestGraphF() throws DENOPTIMException
    {
        DENOPTIMGraph graph = new DENOPTIMGraph();
        DENOPTIMVertex v0 = FragmentSpace.getVertexFromLibrary(
                BBType.FRAGMENT,13);
        graph.addVertex(v0);
        DENOPTIMVertex v1 = FragmentSpace.getVertexFromLibrary(
                BBType.FRAGMENT,13);
        graph.addVertex(v1);
        graph.addEdge(new DENOPTIMEdge(v0.getAP(0), v1.getAP(0)));
        graph.renumberGraphVertices();
        return graph;
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testLinkFromVertex() throws Exception
    {
        prepare();
        DENOPTIMGraph graph = makeTestGraphA();
        GraphLinkFinder glf = new GraphLinkFinder(graph.getVertexAtPosition(1),
                -1, true); 
        // NB: the boolean makes the search complete (within the limits that
        // prevent combinatorial explosion)
        // NB: "-1" is for "no selection of specific new building block".
        
        // These are the expected numbers of AP mappings found for the three
        // vertexes that can be alternative links. 
        Map<Integer,Integer> expected = new HashMap<Integer,Integer>();
        expected.put(4, 4);
        expected.put(5, 4);
        expected.put(9, 12);
        expected.put(10, 6);
        expected.put(11, 84);
        expected.put(13, 2);
        
        LinkedHashMap<DENOPTIMVertex, List<LinkedHashMap<Integer, Integer>>> allAltLinks = 
                glf.getAllAlternativesFoundInt();
        Set<DENOPTIMVertex> keys = allAltLinks.keySet();
        for (DENOPTIMVertex k : keys)
        {
            int bbId = k.getBuildingBlockId();          
            assertTrue(expected.containsKey(bbId), "Vertex with building block "
                    + "ID '" + bbId + "' should not be among the results.");
            assertEquals(expected.get(bbId), allAltLinks.get(k).size(),
                    "Number of APmapping is wrong for bbId '" + bbId + "'.");
        }
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testLinkFromEdge() throws Exception
    {
        prepare();
        DENOPTIMGraph graph = makeTestGraphA();
        DENOPTIMEdge targetEdge = graph.getEdgeAtPosition(1);
        GraphLinkFinder glf = new GraphLinkFinder(targetEdge, -1, true); 
        // NB: the boolean makes the search complete (within the limits the 
        // prevent combinatorial explosion)
        // NB: "-1" is for "no selection of specific new building block".
        
        // Expected results in terms of <bbId:list of classes>
        Map<Integer,List<APClass>> expected = 
                new HashMap<Integer,List<APClass>>();
        expected.put(0, new ArrayList<APClass>(Arrays.asList(
                APCB,APCB,APCB,APCB,APCB,APCB,APCB,APCB)));
        expected.put(3, new ArrayList<APClass>(Arrays.asList(
                APCB,APCB,APCB,APCB,APCB,APCB,APCB,APCB)));
        expected.put(4, new ArrayList<APClass>(Arrays.asList(
                APCB,APCB,APCB,APCB,APCB,APCB,APCB,APCB)));
        expected.put(5, new ArrayList<APClass>(Arrays.asList(
                APCB,APCC,APCB,APCB,APCB,APCB,APCB,APCC)));
        expected.put(9, new ArrayList<APClass>(Arrays.asList(
                APCB,APCB,APCB,APCB,APCB,APCB,APCB,APCB)));
        expected.put(10, new ArrayList<APClass>(Arrays.asList(
                APCB,APCC,APCB,APCB,APCB,APCB,APCB,APCC)));
        expected.put(11, new ArrayList<APClass>(Arrays.asList(
                APCB,APCC,APCB,APCB,
                APCB,APCC,APCB,APCB,
                APCB,APCC,APCB,APCB,
                APCB,APCB,APCB,APCC,
                APCB,APCB,APCB,APCB,
                APCB,APCB,APCB,APCB,
                APCB,APCB,APCB,APCC,
                APCB,APCB,APCB,APCB,
                APCB,APCB,APCB,APCB,
                APCB,APCB,APCB,APCC,
                APCB,APCB,APCB,APCB,
                APCB,APCB,APCB,APCB)));
        
        ensureConsistencyWithExpectations(expected,
                glf.getAllAlternativesFound());
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testLinkFromEdgeOneMatch() throws Exception
    {
        prepare();
        DENOPTIMGraph graph = makeTestGraphE();
        DENOPTIMEdge targetEdge = graph.getEdgeAtPosition(0);
        GraphLinkFinder glf = new GraphLinkFinder(targetEdge, -1, true); 
        // NB: the boolean makes the search complete (within the limits the 
        // prevent combinatorial explosion)
        // NB: "-1" is for "no selection of specific new building block".
        
        // Expected results in terms of <bbId:list of classes>
        Map<Integer,List<APClass>> expected = 
                new HashMap<Integer,List<APClass>>();
        expected.put(12, new ArrayList<APClass>(Arrays.asList(
                APCE,APCE,APCE,APCE,APCE,APCE,APCE,APCE,
                APCE,APCE,APCE,APCE,APCE,APCE,APCE,APCE,
                APCE,APCE,APCE,APCE,APCE,APCE,APCE,APCE)));
        
        ensureConsistencyWithExpectations(expected,
                glf.getAllAlternativesFound());
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testLinkFromEdgeNoMatch() throws Exception
    {
        prepare();
        DENOPTIMGraph graph = makeTestGraphF();
        DENOPTIMEdge targetEdge = graph.getEdgeAtPosition(0);
        GraphLinkFinder glf = new GraphLinkFinder(targetEdge, -1, true); 
        // NB: the boolean makes the search complete (within the limits the 
        // prevent combinatorial explosion)
        // NB: "-1" is for "no selection of specific new building block".
        
        // Expected results in terms of <bbId:list of classes>
        Map<Integer,List<APClass>> expected = 
                new HashMap<Integer,List<APClass>>();
        
        ensureConsistencyWithExpectations(expected,
                glf.getAllAlternativesFound());
    }
    
//------------------------------------------------------------------------------
    
    private void ensureConsistencyWithExpectations(
            Map<Integer,List<APClass>> expected,
            Map<DENOPTIMVertex, List<APMapping>> actual)
    {
        // Set this to true to print the mappings on stdout
        boolean debug = false;
        
        for (DENOPTIMVertex k : actual.keySet())
        {
            int bbId = k.getBuildingBlockId();
            
            if (debug)
            {
                System.out.println("BBID: "+bbId);
            }
            
            List<APMapping> list = actual.get(k);
            
            assertTrue(expected.containsKey(bbId), "Vertex with building block "
                    + "ID '" + bbId + "' should not among the results.");
            assertEquals(expected.get(bbId).size() / 4, actual.get(k).size(),
                    "Number of APmappings is wrong for bbId '" + bbId + "'.");
            
            int i=-1;
            List<APClass> refList = expected.get(bbId);
            for (APMapping apm : list)
            {
                if (debug)
                {
                    System.out.println("    -> "+apm.toString());
                }
                for (Entry<DENOPTIMAttachmentPoint, DENOPTIMAttachmentPoint> e 
                        : apm.entrySet())
                {
                    if (debug)
                    {
                        System.out.println("       -> "+e.getKey().getAPClass()
                                +" "+e.getValue().getAPClass());
                    }
                    i++;
                    assertEquals(refList.get(i),e.getKey().getAPClass(),
                            "APClass in APMapping for bbId "+bbId+" entry " +i);
                    i++;
                    assertEquals(refList.get(i),e.getValue().getAPClass(),
                            "APClass in APMapping for bbId "+bbId+" entry " +i);
                }
            }
        }
        
    }
    
//------------------------------------------------------------------------------
    
}
