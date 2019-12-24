package denoptim.molecule;

/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no>
 *   and Marco Foscato <marco.foscato@uib.no>
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * Unit test for DENOPTIMGraph
 * 
 * @author Marco Foscato
 */

public class DENOPTIMGraphTest
{

	
//------------------------------------------------------------------------------
	
    @Test
    public void testRemoveVertex() throws Exception
    {
    	DENOPTIMGraph graph = new DENOPTIMGraph();
    	ArrayList<DENOPTIMAttachmentPoint> aps0 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps0.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps0.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	aps0.add(new DENOPTIMAttachmentPoint(2, 1, 1));
    	DENOPTIMVertex v0 = new DENOPTIMVertex(0, 0, aps0, 0);
    	graph.addVertex(v0);
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps1 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps1.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps1.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v1 = new DENOPTIMVertex(1, 0, aps1, 1);
    	graph.addVertex(v1);
    	graph.addEdge(new DENOPTIMEdge(0, 1, 0, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps2 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps2.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps2.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v2 = new DENOPTIMVertex(2, 0, aps2, 1);
    	graph.addVertex(v2);
    	graph.addEdge(new DENOPTIMEdge(1, 2, 1, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps3 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps3.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	DENOPTIMVertex v3 = new DENOPTIMVertex(3, 0, aps3, 1);
    	graph.addVertex(v3);
    	graph.addEdge(new DENOPTIMEdge(2, 3, 1, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps4 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps4.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps4.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	aps4.add(new DENOPTIMAttachmentPoint(2, 1, 1));
    	DENOPTIMVertex v4 = new DENOPTIMVertex(4, 0, aps4, 1);
    	graph.addVertex(v4);
    	graph.addEdge(new DENOPTIMEdge(0, 4, 1, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps5 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps5.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	DENOPTIMVertex v5 = new DENOPTIMVertex(5, 0, aps5, 1);
    	graph.addVertex(v5);
    	graph.addEdge(new DENOPTIMEdge(4, 5, 1, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps6 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps6.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	DENOPTIMVertex v6 = new DENOPTIMVertex(6, 0, aps6, 1);
    	graph.addVertex(v6);
    	graph.addEdge(new DENOPTIMEdge(0, 6, 2, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps7 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps7.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	DENOPTIMVertex v7 = new DENOPTIMVertex(7, 0, aps7, 1);
    	graph.addVertex(v7);
    	graph.addEdge(new DENOPTIMEdge(4, 7, 2, 0, 1));
    	
    	graph.addRing(new DENOPTIMRing(new ArrayList<DENOPTIMVertex>(
    			Arrays.asList(v5, v4, v0, v1, v2, v3))));
    	
    	graph.addRing(new DENOPTIMRing(new ArrayList<DENOPTIMVertex>(
    			Arrays.asList(v6, v0, v4, v7))));
    	
    	graph.addSymmetricSetOfVertices(new SymmetricSet(
    			new ArrayList<Integer>(Arrays.asList(3,5))));
    	
    	graph.addSymmetricSetOfVertices(new SymmetricSet(
    			new ArrayList<Integer>(Arrays.asList(6,7))));
    	
    	// Current string encoding this graph is
    	String graphEnc = "0 0_1_0_0,1_1_1_0,2_1_1_0,3_1_1_0,4_1_1_0,5_1_1_0,"
    			+ "6_1_1_0,7_1_1_0, 0_0_1_0_1,1_1_2_0_1,2_1_3_0_1,0_1_4_0_1,"
    			+ "4_1_5_0_1,0_2_6_0_1,4_2_7_0_1, "
    			+ "DENOPTIMRing [verteces=[5_1_1_0, 4_1_1_0, 0_1_0_0, 1_1_1_0,"
    			+ " 2_1_1_0, 3_1_1_0]] DENOPTIMRing [verteces=[6_1_1_0,"
    			+ " 0_1_0_0, 4_1_1_0, 7_1_1_0]] "
    			+ "SymmetricSet [symVrtxIds=[3, 5]] "
    			+ "SymmetricSet [symVrtxIds=[6, 7]]";
    	
    	int numV = graph.getVertexCount();
    	int numE = graph.getEdgeCount();
    	int numS = graph.getSymmetricSetCount();
    	int numR = graph.getRingCount();
    	
    	graph.removeVertex(v5);
 
    	int numVa = graph.getVertexCount();
    	int numEa = graph.getEdgeCount();
    	int numSa = graph.getSymmetricSetCount();
    	int numRa = graph.getRingCount();
    	
    	assertEquals(numVa,numV-1);
    	assertEquals(numEa,numE-1);
    	assertEquals(numSa,numS-1);
    	assertEquals(numRa,numR-1);
    	
    	graph.removeVertex(v3);
    	
    	int numVb = graph.getVertexCount();
    	int numEb = graph.getEdgeCount();
    	int numSb = graph.getSymmetricSetCount();
    	int numRb = graph.getRingCount();
    	
    	assertEquals(numVb,numVa-1);
    	assertEquals(numEb,numEa-1);
    	assertEquals(numSb,numSa);
    	assertEquals(numRb,numRa);
    	
    	graph.removeVertex(v4); // non terminal vertex
    	
    	int numVc = graph.getVertexCount();
    	int numEc = graph.getEdgeCount();
    	int numSc = graph.getSymmetricSetCount();
    	int numRc = graph.getRingCount();
    	
    	assertEquals(numVc,numVb-1);
    	assertEquals(numEc,numEb-2);
    	assertEquals(numSc,numSb);
    	assertEquals(numRc,numRb-1);

    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testSameAs_Equal() throws Exception
    {
    	DENOPTIMGraph graphA = new DENOPTIMGraph();
    	ArrayList<DENOPTIMAttachmentPoint> aps0 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps0.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps0.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	aps0.add(new DENOPTIMAttachmentPoint(2, 1, 1));
    	DENOPTIMVertex v0 = new DENOPTIMVertex(0, 0, aps0, 0);
    	graphA.addVertex(v0);
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps1 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps1.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps1.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v1 = new DENOPTIMVertex(1, 0, aps1, 1);
    	graphA.addVertex(v1);
    	graphA.addEdge(new DENOPTIMEdge(0, 1, 0, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps2 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps2.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps2.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v2 = new DENOPTIMVertex(2, 0, aps2, 1);
    	graphA.addVertex(v2);
    	graphA.addEdge(new DENOPTIMEdge(1, 2, 1, 0, 1));
    	
    	// Other graph, but is the same graph
    	
    	DENOPTIMGraph graphB = new DENOPTIMGraph();
    	ArrayList<DENOPTIMAttachmentPoint> aps0B = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps0B.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps0B.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	aps0B.add(new DENOPTIMAttachmentPoint(2, 1, 1));
    	DENOPTIMVertex v0B = new DENOPTIMVertex(90, 0, aps0, 0);
    	graphB.addVertex(v0B);
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps1B = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps1B.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps1B.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v1B = new DENOPTIMVertex(91, 0, aps1B, 1);
    	graphB.addVertex(v1B);
    	graphB.addEdge(new DENOPTIMEdge(90, 91, 0, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps2B = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps2B.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps2B.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v2B = new DENOPTIMVertex(92, 0, aps2B, 1);
    	graphB.addVertex(v2B);
    	graphB.addEdge(new DENOPTIMEdge(91, 92, 1, 0, 1));
    	
    	/*
    	System.out.println("Graphs");
    	System.out.println(graphA);
    	System.out.println(graphB);
    	*/
    	
    	assertTrue (graphA.sameAs(graphB));	
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testSameAs_DiffVertex() throws Exception
    {
    	DENOPTIMGraph graphA = new DENOPTIMGraph();
    	ArrayList<DENOPTIMAttachmentPoint> aps0 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps0.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps0.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	aps0.add(new DENOPTIMAttachmentPoint(2, 1, 1));
    	DENOPTIMVertex v0 = new DENOPTIMVertex(0, 0, aps0, 0);
    	graphA.addVertex(v0);
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps1 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps1.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps1.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v1 = new DENOPTIMVertex(1, 0, aps1, 1);
    	graphA.addVertex(v1);
    	graphA.addEdge(new DENOPTIMEdge(0, 1, 0, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps2 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps2.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps2.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v2 = new DENOPTIMVertex(2, 0, aps2, 1);
    	graphA.addVertex(v2);
    	graphA.addEdge(new DENOPTIMEdge(1, 2, 1, 0, 1));
    	
    	// Other graph
    	
    	DENOPTIMGraph graphB = new DENOPTIMGraph();
    	ArrayList<DENOPTIMAttachmentPoint> aps0B = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps0B.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps0B.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	aps0B.add(new DENOPTIMAttachmentPoint(2, 1, 1));
    	DENOPTIMVertex v0B = new DENOPTIMVertex(90, 0, aps0, 0);
    	graphB.addVertex(v0B);
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps1B = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps1B.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps1B.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v1B = new DENOPTIMVertex(91, 3, aps1B, 1); //diff molId
    	graphB.addVertex(v1B);
    	graphB.addEdge(new DENOPTIMEdge(90, 91, 0, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps2B = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps2B.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps2B.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v2B = new DENOPTIMVertex(92, 0, aps2B, 1);
    	graphB.addVertex(v2B);
    	graphB.addEdge(new DENOPTIMEdge(91, 92, 1, 0, 1));
    	
    	/*
    	System.out.println("Graphs");
    	System.out.println(graphA);
    	System.out.println(graphB);
    	*/
    	
    	assertFalse(graphA.sameAs(graphB));	
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testSameAs_SameSymmSet() throws Exception
    {
    	DENOPTIMGraph graphA = new DENOPTIMGraph();
    	ArrayList<DENOPTIMAttachmentPoint> aps0 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps0.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps0.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	aps0.add(new DENOPTIMAttachmentPoint(2, 1, 1));
    	aps0.add(new DENOPTIMAttachmentPoint(3, 1, 1));
    	DENOPTIMVertex v0 = new DENOPTIMVertex(0, 0, aps0, 0);
    	graphA.addVertex(v0);
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps1 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps1.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps1.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v1 = new DENOPTIMVertex(1, 0, aps1, 1);
    	graphA.addVertex(v1);
    	graphA.addEdge(new DENOPTIMEdge(0, 1, 0, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps2 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps2.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps2.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v2 = new DENOPTIMVertex(2, 0, aps2, 1);
    	graphA.addVertex(v2);
    	graphA.addEdge(new DENOPTIMEdge(0, 2, 1, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps3 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps3.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps3.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v3 = new DENOPTIMVertex(3, 0, aps3, 1);
    	graphA.addVertex(v3);
    	graphA.addEdge(new DENOPTIMEdge(0, 3, 2, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps4 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps4.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps4.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v4 = new DENOPTIMVertex(4, 0, aps4, 1);
    	graphA.addVertex(v4);
    	graphA.addEdge(new DENOPTIMEdge(0, 4, 3, 0, 1));
    	
    	SymmetricSet ssA = new SymmetricSet();
    	ssA.add(1);
    	ssA.add(2);
    	graphA.addSymmetricSetOfVertices(ssA);
    	SymmetricSet ssA2 = new SymmetricSet();
    	ssA2.add(3);
    	ssA2.add(4);
    	graphA.addSymmetricSetOfVertices(ssA2);
    	
    	// Other
    	
    	DENOPTIMGraph graphB = new DENOPTIMGraph();
    	ArrayList<DENOPTIMAttachmentPoint> aps0b = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps0b.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps0b.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	aps0b.add(new DENOPTIMAttachmentPoint(2, 1, 1));
    	aps0b.add(new DENOPTIMAttachmentPoint(3, 1, 1));
    	DENOPTIMVertex v0b = new DENOPTIMVertex(90, 0, aps0b, 0);
    	graphB.addVertex(v0b);
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps1b = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps1b.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps1b.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v1b = new DENOPTIMVertex(91, 0, aps1b, 1);
    	graphB.addVertex(v1b);
    	graphB.addEdge(new DENOPTIMEdge(90, 91, 0, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps2b = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps2b.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps2b.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v2b = new DENOPTIMVertex(92, 0, aps2b, 1);
    	graphB.addVertex(v2b);
    	graphB.addEdge(new DENOPTIMEdge(90, 92, 1, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps3b = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps3b.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps3b.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v3b = new DENOPTIMVertex(93, 0, aps3b, 1);
    	graphB.addVertex(v3b);
    	graphB.addEdge(new DENOPTIMEdge(90, 93, 2, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps4b = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps4b.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps4b.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v4b = new DENOPTIMVertex(94, 0, aps4b, 1);
    	graphB.addVertex(v4b);
    	graphB.addEdge(new DENOPTIMEdge(90, 94, 3, 0, 1));
    	
    	SymmetricSet ssB2 = new SymmetricSet();
    	ssB2.add(93);
    	ssB2.add(94);
    	graphB.addSymmetricSetOfVertices(ssB2);
    	SymmetricSet ssB = new SymmetricSet();
    	ssB.add(91);
    	ssB.add(92);
    	graphB.addSymmetricSetOfVertices(ssB);
    	
    	/*
    	System.out.println("Graphs Same SS");
    	System.out.println(graphA);
    	System.out.println(graphB);
    	*/
    	
    	assertTrue(graphA.sameAs(graphB));	
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testSameAs_DiffSymmSet() throws Exception
    {
    	DENOPTIMGraph graphA = new DENOPTIMGraph();
    	ArrayList<DENOPTIMAttachmentPoint> aps0 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps0.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps0.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	aps0.add(new DENOPTIMAttachmentPoint(2, 1, 1));
    	aps0.add(new DENOPTIMAttachmentPoint(3, 1, 1));
    	DENOPTIMVertex v0 = new DENOPTIMVertex(0, 0, aps0, 0);
    	graphA.addVertex(v0);
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps1 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps1.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps1.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v1 = new DENOPTIMVertex(1, 0, aps1, 1);
    	graphA.addVertex(v1);
    	graphA.addEdge(new DENOPTIMEdge(0, 1, 0, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps2 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps2.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps2.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v2 = new DENOPTIMVertex(2, 0, aps2, 1);
    	graphA.addVertex(v2);
    	graphA.addEdge(new DENOPTIMEdge(0, 2, 1, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps3 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps3.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps3.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v3 = new DENOPTIMVertex(3, 0, aps3, 1);
    	graphA.addVertex(v3);
    	graphA.addEdge(new DENOPTIMEdge(0, 3, 2, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps4 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps4.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps4.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v4 = new DENOPTIMVertex(4, 0, aps4, 1);
    	graphA.addVertex(v4);
    	graphA.addEdge(new DENOPTIMEdge(0, 4, 3, 0, 1));
    	
    	SymmetricSet ssA = new SymmetricSet();
    	ssA.add(1);                            //difference
    	ssA.add(2);                            //difference
    	graphA.addSymmetricSetOfVertices(ssA);
    	SymmetricSet ssA2 = new SymmetricSet();
    	ssA2.add(3);                           //difference
    	ssA2.add(4);                           //difference
    	graphA.addSymmetricSetOfVertices(ssA2);
    	
    	// Other
    	
    	DENOPTIMGraph graphB = new DENOPTIMGraph();
    	ArrayList<DENOPTIMAttachmentPoint> aps0b = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps0b.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps0b.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	aps0b.add(new DENOPTIMAttachmentPoint(2, 1, 1));
    	aps0b.add(new DENOPTIMAttachmentPoint(3, 1, 1));
    	DENOPTIMVertex v0b = new DENOPTIMVertex(0, 0, aps0b, 0);
    	graphB.addVertex(v0b);
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps1b = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps1b.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps1b.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v1b = new DENOPTIMVertex(1, 0, aps1b, 1);
    	graphB.addVertex(v1b);
    	graphB.addEdge(new DENOPTIMEdge(0, 1, 0, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps2b = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps2b.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps2b.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v2b = new DENOPTIMVertex(2, 0, aps2b, 1);
    	graphB.addVertex(v2b);
    	graphB.addEdge(new DENOPTIMEdge(0, 2, 1, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps3b = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps3b.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps3b.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v3b = new DENOPTIMVertex(3, 0, aps3b, 1);
    	graphB.addVertex(v3b);
    	graphB.addEdge(new DENOPTIMEdge(0, 3, 2, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps4b = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps4b.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps4b.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v4b = new DENOPTIMVertex(4, 0, aps4b, 1);
    	graphB.addVertex(v4b);
    	graphB.addEdge(new DENOPTIMEdge(0, 4, 3, 0, 1));
    	
    	SymmetricSet ssB = new SymmetricSet();
    	ssB.add(1);                           //difference
    	ssB.add(3);                           //difference
    	graphB.addSymmetricSetOfVertices(ssB);
    	SymmetricSet ssB2 = new SymmetricSet();
    	ssB2.add(2);                           //difference
    	ssB2.add(4);                           //difference
    	graphB.addSymmetricSetOfVertices(ssB2);
    	
    	/*
    	System.out.println("Graphs DIFF SS");
    	System.out.println(graphA);
    	System.out.println(graphB);
    	*/
    	
    	assertFalse(graphA.sameAs(graphB));	
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testSameAs_SameRings() throws Exception
    {
    	DENOPTIMGraph graphA = new DENOPTIMGraph();
    	ArrayList<DENOPTIMAttachmentPoint> aps0 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps0.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps0.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	aps0.add(new DENOPTIMAttachmentPoint(2, 1, 1));
    	aps0.add(new DENOPTIMAttachmentPoint(3, 1, 1));
    	DENOPTIMVertex v0 = new DENOPTIMVertex(0, 0, aps0, 0);
    	graphA.addVertex(v0);
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps1 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps1.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps1.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v1 = new DENOPTIMVertex(1, 0, aps1, 1);
    	graphA.addVertex(v1);
    	graphA.addEdge(new DENOPTIMEdge(0, 1, 0, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps2 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps2.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps2.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v2 = new DENOPTIMVertex(2, 0, aps2, 1);
    	graphA.addVertex(v2);
    	graphA.addEdge(new DENOPTIMEdge(0, 2, 1, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps3 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps3.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps3.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v3 = new DENOPTIMVertex(3, 0, aps3, 1);
    	graphA.addVertex(v3);
    	graphA.addEdge(new DENOPTIMEdge(0, 3, 2, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps4 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps4.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps4.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v4 = new DENOPTIMVertex(4, 0, aps4, 1);
    	graphA.addVertex(v4);
    	graphA.addEdge(new DENOPTIMEdge(0, 4, 3, 0, 1));
    	
    	ArrayList<DENOPTIMVertex> vrA = new ArrayList<DENOPTIMVertex>();
    	vrA.add(v1);
    	vrA.add(v0);
    	vrA.add(v2);
    	DENOPTIMRing rA = new DENOPTIMRing(vrA);
    	graphA.addRing(rA);
    	ArrayList<DENOPTIMVertex> vrA2 = new ArrayList<DENOPTIMVertex>();
    	vrA2.add(v3);
    	vrA2.add(v0);
    	vrA2.add(v4);
    	DENOPTIMRing rA2 = new DENOPTIMRing(vrA2);
    	graphA.addRing(rA2);
    	
    	
    	// Other
    	
    	DENOPTIMGraph graphB = new DENOPTIMGraph();
    	ArrayList<DENOPTIMAttachmentPoint> aps0b = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps0b.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps0b.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	aps0b.add(new DENOPTIMAttachmentPoint(2, 1, 1));
    	aps0b.add(new DENOPTIMAttachmentPoint(3, 1, 1));
    	DENOPTIMVertex v0b = new DENOPTIMVertex(90, 0, aps0b, 0);
    	graphB.addVertex(v0b);
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps1b = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps1b.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps1b.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v1b = new DENOPTIMVertex(91, 0, aps1b, 1);
    	graphB.addVertex(v1b);
    	graphB.addEdge(new DENOPTIMEdge(90, 91, 0, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps2b = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps2b.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps2b.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v2b = new DENOPTIMVertex(92, 0, aps2b, 1);
    	graphB.addVertex(v2b);
    	graphB.addEdge(new DENOPTIMEdge(90, 92, 1, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps3b = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps3b.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps3b.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v3b = new DENOPTIMVertex(93, 0, aps3b, 1);
    	graphB.addVertex(v3b);
    	graphB.addEdge(new DENOPTIMEdge(90, 93, 2, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps4b = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps4b.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps4b.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v4b = new DENOPTIMVertex(94, 0, aps4b, 1);
    	graphB.addVertex(v4b);
    	graphB.addEdge(new DENOPTIMEdge(90, 94, 3, 0, 1));
    	
    	ArrayList<DENOPTIMVertex> vrB = new ArrayList<DENOPTIMVertex>();
    	vrB.add(v1b);
    	vrB.add(v0b);
    	vrB.add(v2b);
    	DENOPTIMRing rB = new DENOPTIMRing(vrB);
    	graphB.addRing(rB);
    	ArrayList<DENOPTIMVertex> vrB2 = new ArrayList<DENOPTIMVertex>();
    	vrB2.add(v3b);
    	vrB2.add(v0b);
    	vrB2.add(v4b);
    	DENOPTIMRing rB2 = new DENOPTIMRing(vrB2);
    	graphB.addRing(rB2);
    	
    	/*
    	System.out.println("Graphs Same Rings");
    	System.out.println(graphA);
    	System.out.println(graphB);
    	*/
    	
    	assertTrue(graphA.sameAs(graphB));	
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testSameAs_DisorderRings() throws Exception
    {
    	DENOPTIMGraph graphA = new DENOPTIMGraph();
    	ArrayList<DENOPTIMAttachmentPoint> aps0 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps0.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps0.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	aps0.add(new DENOPTIMAttachmentPoint(2, 1, 1));
    	aps0.add(new DENOPTIMAttachmentPoint(3, 1, 1));
    	DENOPTIMVertex v0 = new DENOPTIMVertex(0, 0, aps0, 0);
    	graphA.addVertex(v0);
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps1 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps1.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps1.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v1 = new DENOPTIMVertex(1, 1, aps1, 1);
    	graphA.addVertex(v1);
    	graphA.addEdge(new DENOPTIMEdge(0, 1, 0, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps2 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps2.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps2.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v2 = new DENOPTIMVertex(2, 2, aps2, 1);
    	graphA.addVertex(v2);
    	graphA.addEdge(new DENOPTIMEdge(0, 2, 1, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps3 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps3.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps3.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v3 = new DENOPTIMVertex(3, 3, aps3, 1);
    	graphA.addVertex(v3);
    	graphA.addEdge(new DENOPTIMEdge(0, 3, 2, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps4 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps4.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps4.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v4 = new DENOPTIMVertex(4, 4, aps4, 1);
    	graphA.addVertex(v4);
    	graphA.addEdge(new DENOPTIMEdge(0, 4, 3, 0, 1));
    	
    	ArrayList<DENOPTIMVertex> vrA = new ArrayList<DENOPTIMVertex>();
    	vrA.add(v1);
    	vrA.add(v0);
    	vrA.add(v2);
    	DENOPTIMRing rA = new DENOPTIMRing(vrA);
    	graphA.addRing(rA);
    	ArrayList<DENOPTIMVertex> vrA2 = new ArrayList<DENOPTIMVertex>();
    	vrA2.add(v3);
    	vrA2.add(v0);
    	vrA2.add(v4);
    	DENOPTIMRing rA2 = new DENOPTIMRing(vrA2);
    	graphA.addRing(rA2);
    	
    	
    	// Other
    	
    	DENOPTIMGraph graphB = new DENOPTIMGraph();
    	ArrayList<DENOPTIMAttachmentPoint> aps0b = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps0b.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps0b.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	aps0b.add(new DENOPTIMAttachmentPoint(2, 1, 1));
    	aps0b.add(new DENOPTIMAttachmentPoint(3, 1, 1));
    	DENOPTIMVertex v0b = new DENOPTIMVertex(90, 0, aps0b, 0);
    	graphB.addVertex(v0b);
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps1b = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps1b.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps1b.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v1b = new DENOPTIMVertex(91, 1, aps1b, 1);
    	graphB.addVertex(v1b);
    	graphB.addEdge(new DENOPTIMEdge(90, 91, 0, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps2b = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps2b.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps2b.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v2b = new DENOPTIMVertex(92, 2, aps2b, 1);
    	graphB.addVertex(v2b);
    	graphB.addEdge(new DENOPTIMEdge(90, 92, 1, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps3b = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps3b.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps3b.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v3b = new DENOPTIMVertex(93, 3, aps3b, 1);
    	graphB.addVertex(v3b);
    	graphB.addEdge(new DENOPTIMEdge(90, 93, 2, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps4b = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps4b.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps4b.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v4b = new DENOPTIMVertex(94, 4, aps4b, 1);
    	graphB.addVertex(v4b);
    	graphB.addEdge(new DENOPTIMEdge(90, 94, 3, 0, 1));
    	
    	ArrayList<DENOPTIMVertex> vrB = new ArrayList<DENOPTIMVertex>();
    	vrB.add(v1b);
    	vrB.add(v0b);
    	vrB.add(v2b);
    	DENOPTIMRing rB = new DENOPTIMRing(vrB);
    	graphB.addRing(rB);
    	ArrayList<DENOPTIMVertex> vrB2 = new ArrayList<DENOPTIMVertex>();
    	vrB2.add(v4b);
    	vrB2.add(v0b);
    	vrB2.add(v3b);
    	DENOPTIMRing rB2 = new DENOPTIMRing(vrB2);
    	graphB.addRing(rB2);
    	
    	/*
    	System.out.println("Graphs Disordered Rings");
    	System.out.println(graphA);
    	System.out.println(graphB);
    	*/
    	
    	assertTrue(graphA.sameAs(graphB));	
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testSameAs_DiffRings() throws Exception
    {
    	DENOPTIMGraph graphA = new DENOPTIMGraph();
    	ArrayList<DENOPTIMAttachmentPoint> aps0 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps0.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps0.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	aps0.add(new DENOPTIMAttachmentPoint(2, 1, 1));
    	aps0.add(new DENOPTIMAttachmentPoint(3, 1, 1));
    	DENOPTIMVertex v0 = new DENOPTIMVertex(0, 0, aps0, 0);
    	graphA.addVertex(v0);
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps1 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps1.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps1.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v1 = new DENOPTIMVertex(1, 1, aps1, 1);
    	graphA.addVertex(v1);
    	graphA.addEdge(new DENOPTIMEdge(0, 1, 0, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps2 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps2.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps2.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v2 = new DENOPTIMVertex(2, 2, aps2, 1);
    	graphA.addVertex(v2);
    	graphA.addEdge(new DENOPTIMEdge(0, 2, 1, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps3 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps3.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps3.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v3 = new DENOPTIMVertex(3, 3, aps3, 1);
    	graphA.addVertex(v3);
    	graphA.addEdge(new DENOPTIMEdge(0, 3, 2, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps4 = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps4.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps4.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v4 = new DENOPTIMVertex(4, 4, aps4, 1);
    	graphA.addVertex(v4);
    	graphA.addEdge(new DENOPTIMEdge(0, 4, 3, 0, 1));
    	
    	ArrayList<DENOPTIMVertex> vrA = new ArrayList<DENOPTIMVertex>();
    	vrA.add(v1);
    	vrA.add(v0);
    	vrA.add(v2);
    	DENOPTIMRing rA = new DENOPTIMRing(vrA);
    	graphA.addRing(rA);
    	ArrayList<DENOPTIMVertex> vrA2 = new ArrayList<DENOPTIMVertex>();
    	vrA2.add(v3);
    	vrA2.add(v0);
    	vrA2.add(v4);
    	DENOPTIMRing rA2 = new DENOPTIMRing(vrA2);
    	graphA.addRing(rA2);
    	
    	
    	// Other
    	
    	DENOPTIMGraph graphB = new DENOPTIMGraph();
    	ArrayList<DENOPTIMAttachmentPoint> aps0b = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps0b.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps0b.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	aps0b.add(new DENOPTIMAttachmentPoint(2, 1, 1));
    	aps0b.add(new DENOPTIMAttachmentPoint(3, 1, 1));
    	DENOPTIMVertex v0b = new DENOPTIMVertex(90, 0, aps0b, 0);
    	graphB.addVertex(v0b);
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps1b = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps1b.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps1b.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v1b = new DENOPTIMVertex(91, 1, aps1b, 1);
    	graphB.addVertex(v1b);
    	graphB.addEdge(new DENOPTIMEdge(90, 91, 0, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps2b = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps2b.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps2b.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v2b = new DENOPTIMVertex(92, 2, aps2b, 1);
    	graphB.addVertex(v2b);
    	graphB.addEdge(new DENOPTIMEdge(90, 92, 1, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps3b = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps3b.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps3b.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v3b = new DENOPTIMVertex(93, 3, aps3b, 1);
    	graphB.addVertex(v3b);
    	graphB.addEdge(new DENOPTIMEdge(90, 93, 2, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps4b = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	aps4b.add(new DENOPTIMAttachmentPoint(0, 1, 1));
    	aps4b.add(new DENOPTIMAttachmentPoint(1, 1, 1));
    	DENOPTIMVertex v4b = new DENOPTIMVertex(94, 4, aps4b, 1);
    	graphB.addVertex(v4b);
    	graphB.addEdge(new DENOPTIMEdge(90, 94, 3, 0, 1));
    	
    	ArrayList<DENOPTIMVertex> vrB = new ArrayList<DENOPTIMVertex>();
    	vrB.add(v1b);
    	vrB.add(v0b);
    	vrB.add(v4b);
    	DENOPTIMRing rB = new DENOPTIMRing(vrB);
    	graphB.addRing(rB);
    	ArrayList<DENOPTIMVertex> vrB2 = new ArrayList<DENOPTIMVertex>();
    	vrB2.add(v2b);
    	vrB2.add(v0b);
    	vrB2.add(v3b);
    	DENOPTIMRing rB2 = new DENOPTIMRing(vrB2);
    	graphB.addRing(rB2);
    	
    	
    	System.out.println("Graphs DIFF Rings");
    	System.out.println(graphA);
    	System.out.println(graphB);
    	
    	
    	assertFalse(graphA.sameAs(graphB));	
    }
    
//------------------------------------------------------------------------------
}
