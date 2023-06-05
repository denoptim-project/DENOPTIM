/*
 *   DENOPTIM
 *   Copyright (C) 2022 Marco Foscato <marco.foscato@uib.no>
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
import denoptim.graph.AttachmentPoint;
import denoptim.graph.DGraph;
import denoptim.graph.Edge;
import denoptim.graph.EmptyVertex;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;

/**
 * Unit test
 * 
 * @author Marco Foscato
 */

public class GraphLinkFinderTest
{
    private static APClass APCA, APCB, APCC, APCD, APCE, APCF;
    
//------------------------------------------------------------------------------
    
    private FragmentSpace prepare() throws DENOPTIMException
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
        
        FragmentSpaceParameters fsp = new FragmentSpaceParameters();
        FragmentSpace fs = new FragmentSpace(fsp,
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(), 
                cpMap, capMap, forbEnds, cpMap);
        fs.setAPclassBasedApproach(true);
        
        EmptyVertex s = new EmptyVertex();
        s.setBuildingBlockType(BBType.SCAFFOLD);
        s.addAP(APCA);
        fs.appendVertexToLibrary(s, BBType.SCAFFOLD, fs.getScaffoldLibrary());
        
        EmptyVertex v0 = new EmptyVertex();
        v0.setBuildingBlockType(BBType.FRAGMENT);
        v0.addAP(APCA);
        v0.addAP(APCB);
        v0.addAP(APCB);
        fs.appendVertexToLibrary(v0, BBType.FRAGMENT, fs.getFragmentLibrary());
        
        EmptyVertex v1 = new EmptyVertex();
        v1.setBuildingBlockType(BBType.FRAGMENT);
        v1.addAP(APCB);
        fs.appendVertexToLibrary(v1, BBType.FRAGMENT, fs.getFragmentLibrary());
        
        EmptyVertex v2 = new EmptyVertex();
        v2.setBuildingBlockType(BBType.FRAGMENT);
        v2.addAP(APCA);
        v2.addAP(APCA);
        fs.appendVertexToLibrary(v2, BBType.FRAGMENT,fs.getFragmentLibrary());
        
        EmptyVertex v3 = new EmptyVertex();
        v3.setBuildingBlockType(BBType.FRAGMENT);
        v3.addAP(APCB);
        v3.addAP(APCB);
        fs.appendVertexToLibrary(v3, BBType.FRAGMENT, fs.getFragmentLibrary());
        
        EmptyVertex v4 = new EmptyVertex();
        v4.setBuildingBlockType(BBType.FRAGMENT);
        v4.addAP(APCA);
        v4.addAP(APCB);
        v4.addAP(APCB);
        fs.appendVertexToLibrary(v4, BBType.FRAGMENT, fs.getFragmentLibrary());
        
        EmptyVertex v5 = new EmptyVertex();
        v5.setBuildingBlockType(BBType.FRAGMENT);
        v5.addAP(APCA);
        v5.addAP(APCC);
        v5.addAP(APCB);
        fs.appendVertexToLibrary(v5, BBType.FRAGMENT, fs.getFragmentLibrary());
        
        EmptyVertex v6 = new EmptyVertex();
        v6.setBuildingBlockType(BBType.FRAGMENT);
        v6.addAP(APCD);
        v6.addAP(APCD);
        fs.appendVertexToLibrary(v6, BBType.FRAGMENT, fs.getFragmentLibrary());
        
        EmptyVertex v7 = new EmptyVertex();
        v7.setBuildingBlockType(BBType.FRAGMENT);
        v7.addAP(APCA);
        fs.appendVertexToLibrary(v7, BBType.FRAGMENT, fs.getFragmentLibrary());
        
        EmptyVertex v8 = new EmptyVertex();
        v8.setBuildingBlockType(BBType.FRAGMENT);
        v8.addAP(APCB);
        fs.appendVertexToLibrary(v8, BBType.FRAGMENT, fs.getFragmentLibrary());
        
        EmptyVertex v9 = new EmptyVertex();
        v9.setBuildingBlockType(BBType.FRAGMENT);
        v9.addAP(APCA);
        v9.addAP(APCA);
        v9.addAP(APCB);
        v9.addAP(APCB);
        fs.appendVertexToLibrary(v9, BBType.FRAGMENT, fs.getFragmentLibrary());
        
        EmptyVertex v10 = new EmptyVertex();
        v10.setBuildingBlockType(BBType.FRAGMENT);
        v10.addAP(APCA);
        v10.addAP(APCC);
        v10.addAP(APCD);
        v10.addAP(APCB);
        fs.appendVertexToLibrary(v10, BBType.FRAGMENT, fs.getFragmentLibrary());
        
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
        fs.appendVertexToLibrary(v11, BBType.FRAGMENT, fs.getFragmentLibrary());
        
        EmptyVertex v12 = new EmptyVertex();
        v12.setBuildingBlockType(BBType.FRAGMENT);
        v12.addAP(APCE);
        v12.addAP(APCE);
        v12.addAP(APCE);
        fs.appendVertexToLibrary(v12, BBType.FRAGMENT, fs.getFragmentLibrary());
        
        EmptyVertex v13 = new EmptyVertex();
        v13.setBuildingBlockType(BBType.FRAGMENT);
        v13.addAP(APCF);
        v13.addAP(APCB);
        v13.addAP(APCA);
        fs.appendVertexToLibrary(v13, BBType.FRAGMENT, fs.getFragmentLibrary());
        fs.groupAndClassifyFragments(true);
        
        return fs;
    }

//------------------------------------------------------------------------------

    /**
     *  Creates a test graph that looks like this: v0(A)-(A)v1(B)-(B)v2
     * @throws DENOPTIMException 
     *  
     */
    private DGraph makeTestGraphA() throws DENOPTIMException
    {
        FragmentSpace fs = prepare();
        DGraph graph = new DGraph();
        Vertex s = fs.getVertexFromLibrary(BBType.SCAFFOLD,0);
        graph.addVertex(s);
        Vertex v0 = fs.getVertexFromLibrary(BBType.FRAGMENT,0);
        graph.addVertex(v0);
        Vertex v1 = fs.getVertexFromLibrary(BBType.FRAGMENT,1);
        graph.addVertex(v1);
        graph.addEdge(new Edge(s.getAP(0), v0.getAP(0)));
        graph.addEdge(new Edge(v0.getAP(1), v1.getAP(0)));
        graph.renumberGraphVertices();
        return graph;
    }
    
//------------------------------------------------------------------------------

    /**
     *  Creates a test graph that looks like this: v0(E)-(E)v1
     * @throws DENOPTIMException 
     *  
     */
    private DGraph makeTestGraphE() throws DENOPTIMException
    {
        FragmentSpace fs = prepare();
        DGraph graph = new DGraph();
        Vertex v0 = fs.getVertexFromLibrary(BBType.FRAGMENT,12);
        graph.addVertex(v0);
        Vertex v1 = fs.getVertexFromLibrary(BBType.FRAGMENT,12);
        graph.addVertex(v1);
        graph.addEdge(new Edge(v0.getAP(1), v1.getAP(1)));
        graph.renumberGraphVertices();
        return graph;
    }
    
//------------------------------------------------------------------------------

    /**
     *  Creates a test graph that looks like this: v0(F)-(F)v1
     * @throws DENOPTIMException 
     *  
     */
    private DGraph makeTestGraphF() throws DENOPTIMException
    {
        FragmentSpace fs = prepare();
        DGraph graph = new DGraph();
        Vertex v0 = fs.getVertexFromLibrary(BBType.FRAGMENT,13);
        graph.addVertex(v0);
        Vertex v1 = fs.getVertexFromLibrary(BBType.FRAGMENT,13);
        graph.addVertex(v1);
        graph.addEdge(new Edge(v0.getAP(0), v1.getAP(0)));
        graph.renumberGraphVertices();
        return graph;
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testLinkFromVertex() throws Exception
    {
        FragmentSpace fs = prepare();
        DGraph graph = makeTestGraphA();
        GraphLinkFinder glf = new GraphLinkFinder(fs,
                graph.getVertexAtPosition(1), -1, true); 
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
        
        LinkedHashMap<Vertex, List<APMapping>> allAltLinks = 
                glf.getAllAlternativesFound();
        Set<Vertex> keys = allAltLinks.keySet();
        for (Vertex k : keys)
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
        FragmentSpace fs = prepare();
        DGraph graph = makeTestGraphA();
        Edge targetEdge = graph.getEdgeAtPosition(1);
        GraphLinkFinder glf = new GraphLinkFinder(fs, targetEdge, -1, true); 
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
        FragmentSpace fs = prepare();
        DGraph graph = makeTestGraphE();
        Edge targetEdge = graph.getEdgeAtPosition(0);
        GraphLinkFinder glf = new GraphLinkFinder(fs, targetEdge, -1, true); 
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
        FragmentSpace fs = prepare();
        DGraph graph = makeTestGraphF();
        Edge targetEdge = graph.getEdgeAtPosition(0);
        GraphLinkFinder glf = new GraphLinkFinder(fs, targetEdge, -1, true); 
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
            Map<Vertex, List<APMapping>> actual)
    {
        // Set this to true to print the mappings on stdout
        boolean debug = false;
        
        for (Vertex k : actual.keySet())
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
                for (Entry<AttachmentPoint, AttachmentPoint> e 
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
