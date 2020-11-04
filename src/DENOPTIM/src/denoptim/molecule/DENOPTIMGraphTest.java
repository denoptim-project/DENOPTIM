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
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.exception.DENOPTIMException;
import denoptim.molecule.DENOPTIMFragment.BBType;

/**
 * Unit test for DENOPTIMGraph
 * 
 * @author Marco Foscato
 */

public class DENOPTIMGraphTest {

	//------------------------------------------------------------------------------
	@Test
	public void testRemoveVertex() throws Exception {
		DENOPTIMGraph graph = new DENOPTIMGraph();
		DENOPTIMVertex v0 = new EmptyVertex(0);
		buildVertexAndConnectToGraph(v0, 3, graph);

		DENOPTIMVertex v1 = new EmptyVertex(1);
		buildVertexAndConnectToGraph(v1, 2, graph);
		graph.addEdge(new DENOPTIMEdge(v0.getAP(0), v1.getAP(0), 0, 1, 0, 0));

		DENOPTIMVertex v2 = new EmptyVertex(2);
		buildVertexAndConnectToGraph(v2, 2, graph);
		graph.addEdge(new DENOPTIMEdge(v1.getAP(1), v2.getAP(0), 1, 2, 1, 0));

		DENOPTIMVertex v3 = new EmptyVertex(3);
		buildVertexAndConnectToGraph(v3, 1, graph);
		graph.addEdge(new DENOPTIMEdge(v2.getAP(1), v3.getAP(0), 2, 3, 1, 0));

		DENOPTIMVertex v4 = new EmptyVertex(4);
		buildVertexAndConnectToGraph(v4, 3, graph);
		graph.addEdge(new DENOPTIMEdge(v0.getAP(1), v4.getAP(0), 0, 4, 1, 0));

		DENOPTIMVertex v5 = new EmptyVertex(5);
		buildVertexAndConnectToGraph(v5, 1, graph);
		graph.addEdge(new DENOPTIMEdge(v4.getAP(1), v5.getAP(0), 4, 5, 1, 0));

		DENOPTIMVertex v6 = new EmptyVertex(6);
		buildVertexAndConnectToGraph(v6, 1, graph);
		graph.addEdge(new DENOPTIMEdge(v0.getAP(2), v6.getAP(0), 0, 6, 2, 0));

		DENOPTIMVertex v7 = new EmptyVertex(7);
		buildVertexAndConnectToGraph(v7, 1, graph);
		graph.addEdge(new DENOPTIMEdge(v4.getAP(2), v7.getAP(0), 4, 7, 2, 0));

		graph.addRing(new DENOPTIMRing(new ArrayList<>(
				Arrays.asList(v5, v4, v0, v1, v2, v3))));

		graph.addRing(new DENOPTIMRing(new ArrayList<>(
				Arrays.asList(v6, v0, v4, v7))));

		graph.addSymmetricSetOfVertices(new SymmetricSet(
				new ArrayList<>(Arrays.asList(3, 5))));

		graph.addSymmetricSetOfVertices(new SymmetricSet(
				new ArrayList<>(Arrays.asList(6, 7))));

		// Current string encoding this graph is
//    	  "0 0_1_0_0,1_1_1_0,2_1_1_0,3_1_1_0,4_1_1_0,5_1_1_0,"
//    			+ "6_1_1_0,7_1_1_0, 0_0_1_0_1,1_1_2_0_1,2_1_3_0_1,0_1_4_0_1,"
//    			+ "4_1_5_0_1,0_2_6_0_1,4_2_7_0_1, "
//    			+ "DENOPTIMRing [verteces=[5_1_1_0, 4_1_1_0, 0_1_0_0, 1_1_1_0,"
//    			+ " 2_1_1_0, 3_1_1_0]] DENOPTIMRing [verteces=[6_1_1_0,"
//    			+ " 0_1_0_0, 4_1_1_0, 7_1_1_0]] "
//    			+ "SymmetricSet [symVrtxIds=[3, 5]] "
//    			+ "SymmetricSet [symVrtxIds=[6, 7]]";

		int numV = graph.getVertexCount();
		int numE = graph.getEdgeCount();
		int numS = graph.getSymmetricSetCount();
		int numR = graph.getRingCount();

		graph.removeVertex(v5);

		int numVa = graph.getVertexCount();
		int numEa = graph.getEdgeCount();
		int numSa = graph.getSymmetricSetCount();
		int numRa = graph.getRingCount();

		assertEquals(numVa, numV - 1);
		assertEquals(numEa, numE - 1);
		assertEquals(numSa, numS - 1);
		assertEquals(numRa, numR - 1);

		graph.removeVertex(v3);

		int numVb = graph.getVertexCount();
		int numEb = graph.getEdgeCount();
		int numSb = graph.getSymmetricSetCount();
		int numRb = graph.getRingCount();

		assertEquals(numVb, numVa - 1);
		assertEquals(numEb, numEa - 1);
		assertEquals(numSb, numSa);
		assertEquals(numRb, numRa);

		graph.removeVertex(v4); // non terminal vertex

		int numVc = graph.getVertexCount();
		int numEc = graph.getEdgeCount();
		int numSc = graph.getSymmetricSetCount();
		int numRc = graph.getRingCount();

		assertEquals(numVc, numVb - 1);
		assertEquals(numEc, numEb - 2);
		assertEquals(numSc, numSb);
		assertEquals(numRc, numRb - 1);

	}

//------------------------------------------------------------------------------

	@Test
	public void testSameAs_Equal() throws Exception {
		DENOPTIMGraph graphA = new DENOPTIMGraph();
		DENOPTIMVertex v0 = new EmptyVertex(0);
		buildVertexAndConnectToGraph(v0, 3, graphA);

		DENOPTIMVertex v1 = new EmptyVertex(1);
		buildVertexAndConnectToGraph(v1, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(0), v1.getAP(0), 0, 1, 0, 0));

		DENOPTIMVertex v2 = new EmptyVertex(2);
		buildVertexAndConnectToGraph(v2, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v1.getAP(1), v2.getAP(0), 1, 2, 1, 0));

		// Other graph, but is the same graph

		DENOPTIMGraph graphB = new DENOPTIMGraph();
		DENOPTIMVertex v90 = new EmptyVertex(90);
		buildVertexAndConnectToGraph(v90, 3, graphB);

		DENOPTIMVertex v91 = new EmptyVertex(91);
		buildVertexAndConnectToGraph(v91, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(0), v91.getAP(0), 90, 91, 0, 0));

		DENOPTIMVertex v92 = new EmptyVertex(92);
		buildVertexAndConnectToGraph(v92, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v91.getAP(1), v92.getAP(0), 91, 92, 1, 0));

    	/*
    	System.out.println("Graphs");
    	System.out.println(graphA);
    	System.out.println(graphB);
    	*/

		StringBuilder reason = new StringBuilder();
		assertTrue(graphA.sameAs(graphB, reason), reason.toString());
	}

//------------------------------------------------------------------------------

	@Test
	public void testSameAs_DiffVertex() throws Exception {
		DENOPTIMGraph graphA = new DENOPTIMGraph();
		DENOPTIMVertex v0 = new EmptyVertex(0);
		buildVertexAndConnectToGraph(v0, 3, graphA);

		DENOPTIMVertex v1 = new EmptyVertex(1);
		buildVertexAndConnectToGraph(v1, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(0), v1.getAP(0), 0, 1, 0, 0));

		DENOPTIMVertex v2 = new EmptyVertex(2);
		buildVertexAndConnectToGraph(v2, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v1.getAP(1), v2.getAP(0), 1, 2, 1, 0));

		// Other graph

		DENOPTIMGraph graphB = new DENOPTIMGraph();
		DENOPTIMVertex v90 = new EmptyVertex(90);
		buildVertexAndConnectToGraph(v90, 3, graphB);

		DENOPTIMVertex v91 = new EmptyVertex(91);
		buildVertexAndConnectToGraph(v91, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(0), v91.getAP(0), 90, 91, 0, 0));

		DENOPTIMVertex v92 = new EmptyVertex(92);
		buildVertexAndConnectToGraph(v92, 3, graphB);
		graphB.addEdge(new DENOPTIMEdge(v91.getAP(1), v92.getAP(0), 91, 92, 1, 0));

    	/*
    	System.out.println("Graphs");
    	System.out.println(graphA);
    	System.out.println(graphB);
    	*/

		StringBuilder reason = new StringBuilder();
		assertFalse(graphA.sameAs(graphB, reason));
	}

//------------------------------------------------------------------------------

	@Test
	public void testSameAs_SameSymmSet() throws Exception {
		DENOPTIMGraph graphA = new DENOPTIMGraph();
		DENOPTIMVertex v0 = new EmptyVertex(0);
		buildVertexAndConnectToGraph(v0, 4, graphA);

		DENOPTIMVertex v1 = new EmptyVertex(1);
		buildVertexAndConnectToGraph(v1, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(0), v1.getAP(0), 0, 1, 0, 0));

		DENOPTIMVertex v2 = new EmptyVertex(2);
		buildVertexAndConnectToGraph(v2, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(1), v2.getAP(0), 0, 2, 1, 0));

		DENOPTIMVertex v3 = new EmptyVertex(3);
		buildVertexAndConnectToGraph(v3, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(2), v3.getAP(0), 0, 3, 2, 0));

		DENOPTIMVertex v4 = new EmptyVertex(4);
		buildVertexAndConnectToGraph(v4, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(3), v4.getAP(0), 0, 4, 3, 0));

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
		DENOPTIMVertex v90 = new EmptyVertex(90);
		buildVertexAndConnectToGraph(v90, 4, graphB);

		DENOPTIMVertex v91 = new EmptyVertex(91);
		buildVertexAndConnectToGraph(v91, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(0), v91.getAP(0), 90, 91, 0, 0));

		DENOPTIMVertex v92 = new EmptyVertex(92);
		buildVertexAndConnectToGraph(v92, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(1), v92.getAP(0), 90, 92, 1, 0));

		DENOPTIMVertex v93 = new EmptyVertex(93);
		buildVertexAndConnectToGraph(v93, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(2), v93.getAP(0), 90, 93, 2, 0));

		DENOPTIMVertex v94 = new EmptyVertex(94);
		buildVertexAndConnectToGraph(v94, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(3), v94.getAP(0), 90, 94, 3, 0));

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

		StringBuilder reason = new StringBuilder();
		assertTrue(graphA.sameAs(graphB, reason));
	}

//------------------------------------------------------------------------------

	@Test
	public void testSameAs_DiffSymmSet() throws Exception {
		DENOPTIMGraph graphA = new DENOPTIMGraph();
		DENOPTIMVertex v0 = new EmptyVertex(0);
		buildVertexAndConnectToGraph(v0, 4, graphA);

		DENOPTIMVertex v1 = new EmptyVertex(1);
		buildVertexAndConnectToGraph(v1, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(0), v1.getAP(0), 0, 1, 0, 0));

		DENOPTIMVertex v2 = new EmptyVertex(2);
		buildVertexAndConnectToGraph(v2, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(1), v2.getAP(0), 0, 2, 1, 0));

		DENOPTIMVertex v3 = new EmptyVertex(3);
		buildVertexAndConnectToGraph(v3, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(2), v3.getAP(0), 0, 3, 2, 0));

		DENOPTIMVertex v4 = new EmptyVertex(4);
		buildVertexAndConnectToGraph(v4, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(3), v4.getAP(0), 0, 4, 3, 0));

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
		DENOPTIMVertex v90 = new EmptyVertex(90);
		buildVertexAndConnectToGraph(v90, 4, graphB);

		DENOPTIMVertex v91 = new EmptyVertex(91);
		buildVertexAndConnectToGraph(v91, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(0), v1.getAP(0), 90, 1, 0, 0));

		DENOPTIMVertex v92 = new EmptyVertex(92);
		buildVertexAndConnectToGraph(v92, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v0.getAP(1), v2.getAP(0), 0, 2, 1, 0));

		DENOPTIMVertex v93 = new EmptyVertex(93);
		buildVertexAndConnectToGraph(v93, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v0.getAP(2), v3.getAP(0), 0, 3, 2, 0));

		DENOPTIMVertex v94 = new EmptyVertex(94);
		buildVertexAndConnectToGraph(v94, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v0.getAP(3), v4.getAP(0), 0, 4, 3, 0));

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

		StringBuilder reason = new StringBuilder();
		assertFalse(graphA.sameAs(graphB, reason));
	}

//------------------------------------------------------------------------------

	@Test
	public void testSameAs_SameRings() throws Exception {
		DENOPTIMGraph graphA = new DENOPTIMGraph();
		DENOPTIMVertex v0 = new EmptyVertex(0);
		buildVertexAndConnectToGraph(v0, 4, graphA);

		DENOPTIMVertex v1 = new EmptyVertex(1);
		buildVertexAndConnectToGraph(v1, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(0), v1.getAP(0), 0, 1, 0, 0));

		DENOPTIMVertex v2 = new EmptyVertex(2);
		buildVertexAndConnectToGraph(v2, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(1), v2.getAP(0), 0, 2, 1, 0));

		DENOPTIMVertex v3 = new EmptyVertex(3);
		buildVertexAndConnectToGraph(v3, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(2), v3.getAP(0), 0, 3, 2, 0));

		DENOPTIMVertex v4 = new EmptyVertex(4);
		buildVertexAndConnectToGraph(v4, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(3), v4.getAP(0), 0, 4, 3, 0));

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
		DENOPTIMVertex v90 = new EmptyVertex(90);
		buildVertexAndConnectToGraph(v90, 4, graphB);

		DENOPTIMVertex v91 = new EmptyVertex(91);
		buildVertexAndConnectToGraph(v91, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(0), v91.getAP(0), 90, 91, 0, 0));

		DENOPTIMVertex v92 = new EmptyVertex(92);
		buildVertexAndConnectToGraph(v92, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(1), v92.getAP(0), 90, 92, 1, 0));

		DENOPTIMVertex v93 = new EmptyVertex(93);
		buildVertexAndConnectToGraph(v93, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(2), v93.getAP(0), 90, 93, 2, 0));

		DENOPTIMVertex v94 = new EmptyVertex(94);
		buildVertexAndConnectToGraph(v94, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(3), v94.getAP(0), 90, 94, 3, 0));

		ArrayList<DENOPTIMVertex> vrB = new ArrayList<DENOPTIMVertex>();
		vrB.add(v91);
		vrB.add(v90);
		vrB.add(v92);
		DENOPTIMRing rB = new DENOPTIMRing(vrB);
		graphB.addRing(rB);
		ArrayList<DENOPTIMVertex> vrB2 = new ArrayList<DENOPTIMVertex>();
		vrB2.add(v93);
		vrB2.add(v90);
		vrB2.add(v94);
		DENOPTIMRing rB2 = new DENOPTIMRing(vrB2);
		graphB.addRing(rB2);

    	/*
    	System.out.println("Graphs Same Rings");
    	System.out.println(graphA);
    	System.out.println(graphB);
    	*/

		StringBuilder reason = new StringBuilder();
		assertTrue(graphA.sameAs(graphB, reason));
	}

//------------------------------------------------------------------------------

	@Test
	public void testSameAs_DisorderRings() throws Exception {
		DENOPTIMGraph graphA = new DENOPTIMGraph();
		DENOPTIMVertex v0 = new EmptyVertex(0);
		buildVertexAndConnectToGraph(v0, 4, graphA);

		DENOPTIMVertex v1 = new EmptyVertex(1);
		buildVertexAndConnectToGraph(v1, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(0), v1.getAP(0), 0, 1, 0, 0));

		DENOPTIMVertex v2 = new EmptyVertex(2);
		buildVertexAndConnectToGraph(v2, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(1), v2.getAP(0), 0, 2, 1, 0));

		DENOPTIMVertex v3 = new EmptyVertex(3);
		buildVertexAndConnectToGraph(v3, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(2), v3.getAP(0), 0, 3, 2, 0));

		DENOPTIMVertex v4 = new EmptyVertex(4);
		buildVertexAndConnectToGraph(v4, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(3), v4.getAP(0), 0, 4, 3, 0));

		ArrayList<DENOPTIMVertex> vrA = new ArrayList<>();
		vrA.add(v1);
		vrA.add(v0);
		vrA.add(v2);
		DENOPTIMRing rA = new DENOPTIMRing(vrA);
		graphA.addRing(rA);
		ArrayList<DENOPTIMVertex> vrA2 = new ArrayList<>();
		vrA2.add(v3);
		vrA2.add(v0);
		vrA2.add(v4);
		DENOPTIMRing rA2 = new DENOPTIMRing(vrA2);
		graphA.addRing(rA2);


		// Other

		DENOPTIMGraph graphB = new DENOPTIMGraph();
		DENOPTIMVertex v90 = new EmptyVertex(90);
		buildVertexAndConnectToGraph(v90, 4, graphB);

		DENOPTIMVertex v91 = new EmptyVertex(91);
		buildVertexAndConnectToGraph(v91, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(0), v91.getAP(0), 90, 91, 0, 0));

		DENOPTIMVertex v92 = new EmptyVertex(92);
		buildVertexAndConnectToGraph(v92, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(1), v92.getAP(0), 90, 92, 1, 0));

		DENOPTIMVertex v93 = new EmptyVertex(93);
		buildVertexAndConnectToGraph(v93, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(2), v93.getAP(0), 90, 93, 2, 0));

		DENOPTIMVertex v94 = new EmptyVertex(94);
		buildVertexAndConnectToGraph(v94, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(3), v94.getAP(0), 90, 94, 3, 0));

		ArrayList<DENOPTIMVertex> vrB = new ArrayList<>();
		vrB.add(v91);
		vrB.add(v90);
		vrB.add(v92);
		DENOPTIMRing rB = new DENOPTIMRing(vrB);
		graphB.addRing(rB);
		ArrayList<DENOPTIMVertex> vrB2 = new ArrayList<>();
		vrB2.add(v94);
		vrB2.add(v90);
		vrB2.add(v93);
		DENOPTIMRing rB2 = new DENOPTIMRing(vrB2);
		graphB.addRing(rB2);

    	/*
    	System.out.println("Graphs Disordered Rings");
    	System.out.println(graphA);
    	System.out.println(graphB);
    	*/

		StringBuilder reason = new StringBuilder();
		assertTrue(graphA.sameAs(graphB, reason));
	}

//------------------------------------------------------------------------------

	@Test
	public void testSameAs_DiffRings() throws Exception {
		DENOPTIMGraph graphA = new DENOPTIMGraph();
		DENOPTIMVertex v0 = new EmptyVertex(0);
		buildVertexAndConnectToGraph(v0, 4, graphA);

		DENOPTIMVertex v1 = new EmptyVertex(1);
		buildVertexAndConnectToGraph(v1, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(0), v1.getAP(0), 0, 1, 0, 0));

		DENOPTIMVertex v2 = new EmptyVertex(2);
		buildVertexAndConnectToGraph(v2, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(1), v2.getAP(0), 0, 2, 1, 0));

		DENOPTIMVertex v3 = new EmptyVertex(3);
		buildVertexAndConnectToGraph(v3, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(2), v3.getAP(0), 0, 3, 2, 0));

		DENOPTIMVertex v4 = new EmptyVertex(4);
		buildVertexAndConnectToGraph(v4, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(3), v4.getAP(0), 0, 4, 3, 0));

		ArrayList<DENOPTIMVertex> vrA = new ArrayList<>();
		vrA.add(v1);
		vrA.add(v0);
		vrA.add(v2);
		DENOPTIMRing rA = new DENOPTIMRing(vrA);
		graphA.addRing(rA);
		ArrayList<DENOPTIMVertex> vrA2 = new ArrayList<>();
		vrA2.add(v3);
		vrA2.add(v0);
		vrA2.add(v4);
		DENOPTIMRing rA2 = new DENOPTIMRing(vrA2);
		graphA.addRing(rA2);

		// Other
		DENOPTIMGraph graphB = new DENOPTIMGraph();
		DENOPTIMVertex v90 = new EmptyVertex(90);
		buildVertexAndConnectToGraph(v90, 4, graphB);

		DENOPTIMVertex v91 = new EmptyVertex(91);
		buildVertexAndConnectToGraph(v91, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(0), v91.getAP(0), 90, 91, 0, 0));

		DENOPTIMVertex v92 = new EmptyVertex(92);
		buildVertexAndConnectToGraph(v92, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(1), v92.getAP(0), 90, 92, 1, 0));

		DENOPTIMVertex v93 = new EmptyVertex(93);
		buildVertexAndConnectToGraph(v93, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(2), v93.getAP(0), 90, 93, 2, 0));

		DENOPTIMVertex v94 = new EmptyVertex(94);
		buildVertexAndConnectToGraph(v94, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(3), v94.getAP(0), 90, 94, 3, 0));

		ArrayList<DENOPTIMVertex> vrB = new ArrayList<>();
		vrB.add(v91);
		vrB.add(v90);
		vrB.add(v94);
		DENOPTIMRing rB = new DENOPTIMRing(vrB);
		graphB.addRing(rB);
		ArrayList<DENOPTIMVertex> vrB2 = new ArrayList<>();
		vrB2.add(v92);
		vrB2.add(v90);
		vrB2.add(v93);
		DENOPTIMRing rB2 = new DENOPTIMRing(vrB2);
		graphB.addRing(rB2);

    	/*
    	System.out.println("Graphs DIFF Rings");
    	System.out.println(graphA);
    	System.out.println(graphB);
    	*/

		StringBuilder reason = new StringBuilder();
		assertFalse(graphA.sameAs(graphB, reason));
	}

//------------------------------------------------------------------------------

	@Test
	public void testGetAvailableAPs_returnsListOfAvailableAPs() {
		DENOPTIMVertex vertex0 = new EmptyVertex(0);
		DENOPTIMAttachmentPoint ap0 = new DENOPTIMAttachmentPoint(vertex0, 0, 1,
				1);
		DENOPTIMAttachmentPoint ap1 = new DENOPTIMAttachmentPoint(vertex0, 0, 1,
				1);

		vertex0.setAttachmentPoints(new ArrayList<>(Arrays.asList(ap0, ap1)));

		DENOPTIMVertex vertex1 = new EmptyVertex(1);
		vertex1.setAttachmentPoints(new ArrayList<>(Arrays.asList(ap0)));

		DENOPTIMEdge edge0 = new DENOPTIMEdge(vertex0.getAP(0),
				vertex1.getAP(0), vertex0.getVertexId(),
				vertex1.getVertexId(), 0, 0);

		DENOPTIMGraph graph = new DENOPTIMGraph();
		graph.addVertex(vertex0);
		graph.addVertex(vertex1);
		graph.addEdge(edge0);

		//TODO-V3 add assert statements
	}

//------------------------------------------------------------------------------

	@Test
	public void testClone() throws DENOPTIMException {
		DENOPTIMGraph graph = new DENOPTIMGraph();
		DENOPTIMVertex v0 = new EmptyVertex(0);
		buildVertexAndConnectToGraph(v0, 3, graph);

		DENOPTIMVertex v1 = new EmptyVertex(1);
		buildVertexAndConnectToGraph(v1, 2, graph);
		graph.addEdge(new DENOPTIMEdge(v0.getAP(0), v1.getAP(0), 0, 1, 0, 0));

		DENOPTIMVertex v2 = new EmptyVertex(2);
		buildVertexAndConnectToGraph(v2, 2, graph);
		graph.addEdge(new DENOPTIMEdge(v1.getAP(1), v2.getAP(0), 1, 2, 1, 0));

		DENOPTIMVertex v3 = new EmptyVertex(3);
		buildVertexAndConnectToGraph(v3, 1, graph);
		graph.addEdge(new DENOPTIMEdge(v2.getAP(1), v3.getAP(0), 2, 3, 1, 0));

		DENOPTIMVertex v4 = new EmptyVertex(4);
		buildVertexAndConnectToGraph(v4, 3, graph);
		graph.addEdge(new DENOPTIMEdge(v0.getAP(1), v4.getAP(0), 0, 4, 1, 0));

		DENOPTIMVertex v5 = new EmptyVertex(5);
		buildVertexAndConnectToGraph(v5, 1, graph);
		graph.addEdge(new DENOPTIMEdge(v4.getAP(1), v5.getAP(0), 4, 5, 1, 0));

		DENOPTIMVertex v6 = new EmptyVertex(6);
		buildVertexAndConnectToGraph(v6, 1, graph);
		graph.addEdge(new DENOPTIMEdge(v0.getAP(2), v6.getAP(0), 0, 6, 2, 0));

		DENOPTIMVertex v7 = new EmptyVertex(7);
		buildVertexAndConnectToGraph(v7, 1, graph);
		graph.addEdge(new DENOPTIMEdge(v4.getAP(2), v7.getAP(0), 4, 7, 2, 0));

		graph.addRing(new DENOPTIMRing(new ArrayList<>(
				Arrays.asList(v5, v4, v0, v1, v2, v3))));

		graph.addRing(new DENOPTIMRing(new ArrayList<>(
				Arrays.asList(v6, v0, v4, v7))));

		graph.addSymmetricSetOfVertices(new SymmetricSet(
				new ArrayList<>(Arrays.asList(3, 5))));

		graph.addSymmetricSetOfVertices(new SymmetricSet(
				new ArrayList<>(Arrays.asList(6, 7))));

		DENOPTIMGraph clone = graph.clone();

		assertEquals(graph.gVertices.size(), clone.gVertices.size(),
				"Number of vertices");
		assertEquals(graph.gEdges.size(), clone.gEdges.size(),
				"Number of Edges");
		assertEquals(graph.gRings.size(), clone.gRings.size(),
				"Number of Rings");
		assertEquals(graph.getSymmetricSetCount(), clone.getSymmetricSetCount(),
				"Number of symmetric sets");
		assertEquals(graph.closableChains.size(), clone.closableChains.size(),
				"Number of closable chains");
		assertEquals(graph.localMsg, clone.localMsg,
				"Local msg");
		assertEquals(graph.graphId, clone.graphId,
				"Graph ID");
	}

//------------------------------------------------------------------------------

	@Test
	public void testGetMutationSites() throws Exception {
		DENOPTIMGraph graph = new DENOPTIMGraph();
		DENOPTIMTemplate tmpl = DENOPTIMTemplate.getTestTemplate(2);
		graph.addVertex(tmpl);

		assertEquals(1, graph.getMutableSites().size(),
				"Size of mutation size list in case of frozen template");

		graph = new DENOPTIMGraph();
		tmpl = DENOPTIMTemplate.getTestTemplate(0);
		graph.addVertex(tmpl);

		assertEquals(2, graph.getMutableSites().size(),
				"Size of mutation size list in case of free template");
	}

//------------------------------------------------------------------------------

	@Test
	public void testRemoveCapping() throws Exception {
		DENOPTIMGraph graph = new DENOPTIMGraph();

		IAtomContainer iac1 = new AtomContainer();
		iac1.addAtom(new Atom("C"));
		DENOPTIMVertex v1 = new DENOPTIMFragment(1, iac1, BBType.SCAFFOLD);
		v1.addAttachmentPoint(new DENOPTIMAttachmentPoint(v1, 0, 1, 1));
		v1.addAttachmentPoint(new DENOPTIMAttachmentPoint(v1, 0, 1, 1));

		IAtomContainer iac2 = new AtomContainer();
		iac2.addAtom(new Atom("O"));
		DENOPTIMVertex v2 = new DENOPTIMFragment(2, iac2, BBType.FRAGMENT);
		v2.addAttachmentPoint(new DENOPTIMAttachmentPoint(v2, 0, 1, 1));
		v2.addAttachmentPoint(new DENOPTIMAttachmentPoint(v2, 0, 1, 1));

		IAtomContainer iac3 = new AtomContainer();
		iac3.addAtom(new Atom("H"));
		DENOPTIMVertex v3 = new DENOPTIMFragment(3, iac3, BBType.CAP);
		v3.addAttachmentPoint(new DENOPTIMAttachmentPoint(v3, 0, 1, 1));

		IAtomContainer iac4 = new AtomContainer();
		iac4.addAtom(new Atom("H"));
		DENOPTIMVertex v4 = new DENOPTIMFragment(4, iac4, BBType.CAP);
		v4.addAttachmentPoint(new DENOPTIMAttachmentPoint(v4, 0, 1, 1));

		graph.addVertex(v1);
		graph.addVertex(v2);
		graph.addVertex(v3);
		graph.addVertex(v4);
		graph.addEdge(new DENOPTIMEdge(v1.getAP(0), v2.getAP(0), 1, 2, 0, 0));
		graph.addEdge(new DENOPTIMEdge(v1.getAP(1), v3.getAP(0), 1, 3, 1, 0));
		graph.addEdge(new DENOPTIMEdge(v2.getAP(1), v4.getAP(0), 2, 4, 1, 0));

		assertEquals(4, graph.getVertexCount(),
				"#vertexes in graph before removal");
		assertTrue(graph == v4.getGraphOwner());

		graph.removeCappingGroupsOn(v2);

		assertEquals(3, graph.getVertexCount(),
				"#vertexes in graph before removal");
		assertFalse(graph.containsVertex(v4),
				"Capping is still contained");
		assertTrue(null == v4.getGraphOwner(),
				"Owner of removed capping group is null");


		DENOPTIMGraph graph2 = new DENOPTIMGraph();

		IAtomContainer iac12 = new AtomContainer();
		iac12.addAtom(new Atom("C"));
		DENOPTIMVertex v21 = new DENOPTIMFragment(21, iac12, BBType.SCAFFOLD);
		v21.addAttachmentPoint(new DENOPTIMAttachmentPoint(v21, 0, 1, 1));
		v21.addAttachmentPoint(new DENOPTIMAttachmentPoint(v21, 0, 1, 1));

		IAtomContainer iac22 = new AtomContainer();
		iac22.addAtom(new Atom("O"));
		DENOPTIMVertex v22 = new DENOPTIMFragment(22, iac22, BBType.FRAGMENT);
		v22.addAttachmentPoint(new DENOPTIMAttachmentPoint(v22, 0, 1, 1));
		v22.addAttachmentPoint(new DENOPTIMAttachmentPoint(v22, 0, 1, 1));

		IAtomContainer iac23 = new AtomContainer();
		iac23.addAtom(new Atom("H"));
		DENOPTIMVertex v23 = new DENOPTIMFragment(23, iac23, BBType.CAP);
		v23.addAttachmentPoint(new DENOPTIMAttachmentPoint(v23, 0, 1, 1));

		IAtomContainer iac24 = new AtomContainer();
		iac24.addAtom(new Atom("H"));
		DENOPTIMVertex v24 = new DENOPTIMFragment(24, iac24, BBType.CAP);
		v24.addAttachmentPoint(new DENOPTIMAttachmentPoint(v24, 0, 1, 1));

		graph2.addVertex(v21);
		graph2.addVertex(v22);
		graph2.addVertex(v23);
		graph2.addVertex(v24);
		graph2.addEdge(new DENOPTIMEdge(v21.getAP(0), v22.getAP(0), 21, 22, 0, 0));
		graph2.addEdge(new DENOPTIMEdge(v21.getAP(1), v23.getAP(0), 21, 23, 1, 0));
		graph2.addEdge(new DENOPTIMEdge(v22.getAP(1), v24.getAP(0), 22, 24, 1, 0));

		assertEquals(4, graph2.getVertexCount(),
				"#vertexes in graph before removal (B)");
		assertTrue(graph2 == v23.getGraphOwner());
		assertTrue(graph2 == v24.getGraphOwner());

		graph2.removeCappingGroups();

		assertEquals(2, graph2.getVertexCount(),
				"#vertexes in graph before removal (B)");
		assertFalse(graph.containsVertex(v24),
				"Capping is still contained (B)");
		assertFalse(graph.containsVertex(v23),
				"Capping is still contained (C)");
		assertTrue(null == v24.getGraphOwner(),
				"Owner of removed capping group is null (B)");
		assertTrue(null == v23.getGraphOwner(),
				"Owner of removed capping group is null (C)");
	}

//------------------------------------------------------------------------------

	private void buildVertexAndConnectToGraph(
			DENOPTIMVertex v, int apCount, DENOPTIMGraph graph) {
		final int ATOM_CONNS = 1;
		final int AP_CONNS = 1;
		ArrayList<DENOPTIMAttachmentPoint> aps = new ArrayList<>();
		for (int atomPos = 0; atomPos < apCount; atomPos++) {
			aps.add(new DENOPTIMAttachmentPoint(v, atomPos, ATOM_CONNS,
					AP_CONNS));
		}
		v.setAttachmentPoints(aps);
		graph.addVertex(v);
//		graph.addEdge(new DENOPTIMEdge(0, 1, 0, 0));
	}
}