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
		ArrayList<DENOPTIMAttachmentPoint> aps0 = new ArrayList<>();
		DENOPTIMVertex v0 = new EmptyVertex(0);
		aps0.add(new DENOPTIMAttachmentPoint(v0, 0, 1, 1));
		aps0.add(new DENOPTIMAttachmentPoint(v0, 1, 1, 1));
		aps0.add(new DENOPTIMAttachmentPoint(v0, 2, 1, 1));
		v0.setAttachmentPoints(aps0);
		graph.addVertex(v0);

		ArrayList<DENOPTIMAttachmentPoint> aps1 = new ArrayList<>();
		DENOPTIMVertex v1 = new EmptyVertex(1);
		aps1.add(new DENOPTIMAttachmentPoint(v1, 0, 1, 1));
		aps1.add(new DENOPTIMAttachmentPoint(v1, 1, 1, 1));
		v1.setAttachmentPoints(aps1);
		graph.addVertex(v1);
		graph.addEdge(new DENOPTIMEdge(0, 1, 0, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps2 = new ArrayList<>();
		DENOPTIMVertex v2 = new EmptyVertex(2);
		aps2.add(new DENOPTIMAttachmentPoint(v2, 0, 1, 1));
		aps2.add(new DENOPTIMAttachmentPoint(v2, 1, 1, 1));
		v2.setAttachmentPoints(aps2);
		graph.addVertex(v2);
		graph.addEdge(new DENOPTIMEdge(1, 2, 1, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps3 = new ArrayList<>();
		DENOPTIMVertex v3 = new EmptyVertex(3);
		aps3.add(new DENOPTIMAttachmentPoint(v3, 0, 1, 1));
		v3.setAttachmentPoints(aps3);
		graph.addVertex(v3);
		graph.addEdge(new DENOPTIMEdge(2, 3, 1, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps4 = new ArrayList<>();
		DENOPTIMVertex v4 = new EmptyVertex(4);
		aps4.add(new DENOPTIMAttachmentPoint(v4, 0, 1, 1));
		aps4.add(new DENOPTIMAttachmentPoint(v4, 1, 1, 1));
		aps4.add(new DENOPTIMAttachmentPoint(v4, 2, 1, 1));
		v4.setAttachmentPoints(aps4);
		graph.addVertex(v4);
		graph.addEdge(new DENOPTIMEdge(0, 4, 1, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps5 = new ArrayList<>();
		DENOPTIMVertex v5 = new EmptyVertex(5);
		aps5.add(new DENOPTIMAttachmentPoint(v5, 0, 1, 1));
		v5.setAttachmentPoints(aps5);
		graph.addVertex(v5);
		graph.addEdge(new DENOPTIMEdge(4, 5, 1, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps6 = new ArrayList<>();
		DENOPTIMVertex v6 = new EmptyVertex(6);
		aps6.add(new DENOPTIMAttachmentPoint(v6, 0, 1, 1));
		v6.setAttachmentPoints(aps6);
		graph.addVertex(v6);
		graph.addEdge(new DENOPTIMEdge(0, 6, 2, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps7 = new ArrayList<>();
		DENOPTIMVertex v7 = new EmptyVertex(7);
		aps7.add(new DENOPTIMAttachmentPoint(v7, 0, 1, 1));
		v7.setAttachmentPoints(aps7);
		graph.addVertex(v7);
		graph.addEdge(new DENOPTIMEdge(4, 7, 2, 0));

		graph.addRing(new DENOPTIMRing(new ArrayList<DENOPTIMVertex>(
				Arrays.asList(v5, v4, v0, v1, v2, v3))));

		graph.addRing(new DENOPTIMRing(new ArrayList<DENOPTIMVertex>(
				Arrays.asList(v6, v0, v4, v7))));

		graph.addSymmetricSetOfVertices(new SymmetricSet(
				new ArrayList<Integer>(Arrays.asList(3, 5))));

		graph.addSymmetricSetOfVertices(new SymmetricSet(
				new ArrayList<Integer>(Arrays.asList(6, 7))));

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
		ArrayList<DENOPTIMAttachmentPoint> aps0 = new ArrayList<>();
		DENOPTIMVertex v0 = new EmptyVertex(0);
		aps0.add(new DENOPTIMAttachmentPoint(v0, 0, 1, 1));
		aps0.add(new DENOPTIMAttachmentPoint(v0, 1, 1, 1));
		aps0.add(new DENOPTIMAttachmentPoint(v0, 2, 1, 1));
		v0.setAttachmentPoints(aps0);
		graphA.addVertex(v0);

		ArrayList<DENOPTIMAttachmentPoint> aps1 = new ArrayList<>();
		DENOPTIMVertex v1 = new EmptyVertex(1);
		aps1.add(new DENOPTIMAttachmentPoint(v1, 0, 1, 1));
		aps1.add(new DENOPTIMAttachmentPoint(v1, 1, 1, 1));
		v1.setAttachmentPoints(aps1);
		graphA.addVertex(v1);
		graphA.addEdge(new DENOPTIMEdge(0, 1, 0, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps2 = new ArrayList<>();
		DENOPTIMVertex v2 = new EmptyVertex(2);
		aps2.add(new DENOPTIMAttachmentPoint(v2, 0, 1, 1));
		aps2.add(new DENOPTIMAttachmentPoint(v2, 1, 1, 1));
		v2.setAttachmentPoints(aps2);
		graphA.addVertex(v2);
		graphA.addEdge(new DENOPTIMEdge(1, 2, 1, 0));

		// Other graph, but is the same graph

		DENOPTIMGraph graphB = new DENOPTIMGraph();
        ArrayList<DENOPTIMAttachmentPoint> aps0B = new ArrayList<>();
        DENOPTIMVertex v0B = new EmptyVertex(90);
        aps0B.add(new DENOPTIMAttachmentPoint(v0B, 0, 1, 1));
        aps0B.add(new DENOPTIMAttachmentPoint(v0B, 1, 1, 1));
        aps0B.add(new DENOPTIMAttachmentPoint(v0B, 2, 1, 1));
        v0B.setAttachmentPoints(aps0B);
        graphB.addVertex(v0B);
		ArrayList<DENOPTIMAttachmentPoint> aps1B = new ArrayList<>();
		DENOPTIMVertex v1B = new EmptyVertex(91);
		aps1B.add(new DENOPTIMAttachmentPoint(v1B, 0, 1, 1));
		aps1B.add(new DENOPTIMAttachmentPoint(v1B, 1, 1, 1));
		v1B.setAttachmentPoints(aps1B);
		graphB.addVertex(v1B);
		graphB.addEdge(new DENOPTIMEdge(90, 91, 0, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps2B = new ArrayList<>();
		DENOPTIMVertex v2B = new EmptyVertex(92);
		aps2B.add(new DENOPTIMAttachmentPoint(v2B, 0, 1, 1));
		aps2B.add(new DENOPTIMAttachmentPoint(v2B, 1, 1, 1));
		v2B.setAttachmentPoints(aps2B);
		graphB.addVertex(v2B);
		graphB.addEdge(new DENOPTIMEdge(91, 92, 1, 0));

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
		ArrayList<DENOPTIMAttachmentPoint> aps0 = new ArrayList<>();
		DENOPTIMVertex v0 = new EmptyVertex(0);
		aps0.add(new DENOPTIMAttachmentPoint(v0, 0, 1, 1));
		aps0.add(new DENOPTIMAttachmentPoint(v0, 1, 1, 1));
		aps0.add(new DENOPTIMAttachmentPoint(v0, 2, 1, 1));
		v0.setAttachmentPoints(aps0);
		graphA.addVertex(v0);

		ArrayList<DENOPTIMAttachmentPoint> aps1 = new ArrayList<>();
		DENOPTIMVertex v1 = new EmptyVertex(1);
		aps1.add(new DENOPTIMAttachmentPoint(v1, 0, 1, 1));
		aps1.add(new DENOPTIMAttachmentPoint(v1, 1, 1, 1));
		v1.setAttachmentPoints(aps1);
		graphA.addVertex(v1);
		graphA.addEdge(new DENOPTIMEdge(0, 1, 0, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps2 = new ArrayList<>();
		DENOPTIMVertex v2 = new EmptyVertex(2);
		aps2.add(new DENOPTIMAttachmentPoint(v2, 0, 1, 1));
		aps2.add(new DENOPTIMAttachmentPoint(v2, 1, 1, 1));
		v2.setAttachmentPoints(aps2);
		graphA.addVertex(v2);
		graphA.addEdge(new DENOPTIMEdge(1, 2, 1, 0));

		// Other graph

		DENOPTIMGraph graphB = new DENOPTIMGraph();
		ArrayList<DENOPTIMAttachmentPoint> aps0B = new ArrayList<>();
		DENOPTIMVertex v0B = new EmptyVertex(90);
		aps0B.add(new DENOPTIMAttachmentPoint(v0B, 0, 1, 1));
		aps0B.add(new DENOPTIMAttachmentPoint(v0B, 1, 1, 1));
		aps0B.add(new DENOPTIMAttachmentPoint(v0B, 2, 1, 1));
		v0B.setAttachmentPoints(aps0B);
		graphB.addVertex(v0B);

		ArrayList<DENOPTIMAttachmentPoint> aps1B = new ArrayList<>();
		DENOPTIMVertex v1B = new EmptyVertex(91);
		aps1B.add(new DENOPTIMAttachmentPoint(v1B, 0, 1, 1));
		aps1B.add(new DENOPTIMAttachmentPoint(v1B, 1, 1, 1));
		v1B.setAttachmentPoints(aps1B);
		graphB.addVertex(v1B);
		graphB.addEdge(new DENOPTIMEdge(90, 91, 0, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps2B = new ArrayList<>();
		DENOPTIMVertex v2B = new EmptyVertex(92);
		aps2B.add(new DENOPTIMAttachmentPoint(v2B, 0, 1, 1));
		aps2B.add(new DENOPTIMAttachmentPoint(v2B, 1, 1, 1));
		aps2B.add(new DENOPTIMAttachmentPoint(v2B, 1, 1, 1));
		v2B.setAttachmentPoints(aps2B);
		graphB.addVertex(v2B);
		graphB.addEdge(new DENOPTIMEdge(91, 92, 1, 0));
    	
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
		ArrayList<DENOPTIMAttachmentPoint> aps0 = new ArrayList<>();
		DENOPTIMVertex v0 = new EmptyVertex(0);
		aps0.add(new DENOPTIMAttachmentPoint(v0, 0, 1, 1));
		aps0.add(new DENOPTIMAttachmentPoint(v0, 1, 1, 1));
		aps0.add(new DENOPTIMAttachmentPoint(v0, 2, 1, 1));
		aps0.add(new DENOPTIMAttachmentPoint(v0, 3, 1, 1));
		v0.setAttachmentPoints(aps0);
		graphA.addVertex(v0);

		ArrayList<DENOPTIMAttachmentPoint> aps1 = new ArrayList<>();
		DENOPTIMVertex v1 = new EmptyVertex(1);
		aps1.add(new DENOPTIMAttachmentPoint(v1, 0, 1, 1));
		aps1.add(new DENOPTIMAttachmentPoint(v1, 1, 1, 1));
		v1.setAttachmentPoints(aps1);
		graphA.addVertex(v1);
		graphA.addEdge(new DENOPTIMEdge(0, 1, 0, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps2 = new ArrayList<>();
		DENOPTIMVertex v2 = new EmptyVertex(2);
		aps2.add(new DENOPTIMAttachmentPoint(v2, 0, 1, 1));
		aps2.add(new DENOPTIMAttachmentPoint(v2, 1, 1, 1));
		v2.setAttachmentPoints(aps2);
		graphA.addVertex(v2);
		graphA.addEdge(new DENOPTIMEdge(0, 2, 1, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps3 = new ArrayList<>();
		DENOPTIMVertex v3 = new EmptyVertex(3);
		aps3.add(new DENOPTIMAttachmentPoint(v3, 0, 1, 1));
		aps3.add(new DENOPTIMAttachmentPoint(v3, 1, 1, 1));
		v3.setAttachmentPoints(aps3);
		graphA.addVertex(v3);
		graphA.addEdge(new DENOPTIMEdge(0, 3, 2, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps4 = new ArrayList<>();
		DENOPTIMVertex v4 = new EmptyVertex(4);
		aps4.add(new DENOPTIMAttachmentPoint(v4, 0, 1, 1));
		aps4.add(new DENOPTIMAttachmentPoint(v4, 1, 1, 1));
		v4.setAttachmentPoints(aps4);
		graphA.addVertex(v4);
		graphA.addEdge(new DENOPTIMEdge(0, 4, 3, 0));

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
		ArrayList<DENOPTIMAttachmentPoint> aps0b = new ArrayList<>();
		DENOPTIMVertex v0b = new EmptyVertex(90);
		aps0b.add(new DENOPTIMAttachmentPoint(v0b, 0, 1, 1));
		aps0b.add(new DENOPTIMAttachmentPoint(v0b, 1, 1, 1));
		aps0b.add(new DENOPTIMAttachmentPoint(v0b, 2, 1, 1));
		aps0b.add(new DENOPTIMAttachmentPoint(v0b, 3, 1, 1));
		v0b.setAttachmentPoints(aps0b);
		graphB.addVertex(v0b);

		ArrayList<DENOPTIMAttachmentPoint> aps1b = new ArrayList<>();
		DENOPTIMVertex v1b = new EmptyVertex(91);
		aps1b.add(new DENOPTIMAttachmentPoint(v1b, 0, 1, 1));
		aps1b.add(new DENOPTIMAttachmentPoint(v1b, 1, 1, 1));
		v1b.setAttachmentPoints(aps1b);
		graphB.addVertex(v1b);
		graphB.addEdge(new DENOPTIMEdge(90, 91, 0, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps2b = new ArrayList<>();
		DENOPTIMVertex v2b = new EmptyVertex(92);
		aps2b.add(new DENOPTIMAttachmentPoint(v2b, 0, 1, 1));
		aps2b.add(new DENOPTIMAttachmentPoint(v2b, 1, 1, 1));
		v2b.setAttachmentPoints(aps2b);
		graphB.addVertex(v2b);
		graphB.addEdge(new DENOPTIMEdge(90, 92, 1, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps3b = new ArrayList<>();
		DENOPTIMVertex v3b = new EmptyVertex(93);
		aps3b.add(new DENOPTIMAttachmentPoint(v3b, 0, 1, 1));
		aps3b.add(new DENOPTIMAttachmentPoint(v3b, 1, 1, 1));
		v3b.setAttachmentPoints(aps3b);
		graphB.addVertex(v3b);
		graphB.addEdge(new DENOPTIMEdge(90, 93, 2, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps4b = new ArrayList<>();
		DENOPTIMVertex v4b = new EmptyVertex(94);
		aps4b.add(new DENOPTIMAttachmentPoint(v4b, 0, 1, 1));
		aps4b.add(new DENOPTIMAttachmentPoint(v4b, 1, 1, 1));
		v4b.setAttachmentPoints(aps4b);
		graphB.addVertex(v4b);
		graphB.addEdge(new DENOPTIMEdge(90, 94, 3, 0));

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
		ArrayList<DENOPTIMAttachmentPoint> aps0 = new ArrayList<>();
		DENOPTIMVertex v0 = new EmptyVertex(0);
		aps0.add(new DENOPTIMAttachmentPoint(v0, 0, 1, 1));
		aps0.add(new DENOPTIMAttachmentPoint(v0, 1, 1, 1));
		aps0.add(new DENOPTIMAttachmentPoint(v0, 2, 1, 1));
		aps0.add(new DENOPTIMAttachmentPoint(v0, 3, 1, 1));
		v0.setAttachmentPoints(aps0);
		graphA.addVertex(v0);

		ArrayList<DENOPTIMAttachmentPoint> aps1 = new ArrayList<>();
		DENOPTIMVertex v1 = new EmptyVertex(1);
		aps1.add(new DENOPTIMAttachmentPoint(v1, 0, 1, 1));
		aps1.add(new DENOPTIMAttachmentPoint(v1, 1, 1, 1));
		v1.setAttachmentPoints(aps1);
		graphA.addVertex(v1);
		graphA.addEdge(new DENOPTIMEdge(0, 1, 0, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps2 = new ArrayList<>();
		DENOPTIMVertex v2 = new EmptyVertex(2);
		aps2.add(new DENOPTIMAttachmentPoint(v2, 0, 1, 1));
		aps2.add(new DENOPTIMAttachmentPoint(v2, 1, 1, 1));
		v2.setAttachmentPoints(aps2);
		graphA.addVertex(v2);
		graphA.addEdge(new DENOPTIMEdge(0, 2, 1, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps3 = new ArrayList<>();
		DENOPTIMVertex v3 = new EmptyVertex(3);
		aps3.add(new DENOPTIMAttachmentPoint(v3, 0, 1, 1));
		aps3.add(new DENOPTIMAttachmentPoint(v3, 1, 1, 1));
		v3.setAttachmentPoints(aps3);
		graphA.addVertex(v3);
		graphA.addEdge(new DENOPTIMEdge(0, 3, 2, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps4 = new ArrayList<>();
		DENOPTIMVertex v4 = new EmptyVertex(4);
		aps4.add(new DENOPTIMAttachmentPoint(v4, 0, 1, 1));
		aps4.add(new DENOPTIMAttachmentPoint(v4, 1, 1, 1));
		v4.setAttachmentPoints(aps4);
		graphA.addVertex(v4);
		graphA.addEdge(new DENOPTIMEdge(0, 4, 3, 0));

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
		ArrayList<DENOPTIMAttachmentPoint> aps0b = new ArrayList<>();
		DENOPTIMVertex v0b = new EmptyVertex(0);
		aps0b.add(new DENOPTIMAttachmentPoint(v0b, 0, 1, 1));
		aps0b.add(new DENOPTIMAttachmentPoint(v0b, 1, 1, 1));
		aps0b.add(new DENOPTIMAttachmentPoint(v0b, 2, 1, 1));
		aps0b.add(new DENOPTIMAttachmentPoint(v0b, 3, 1, 1));
		v0b.setAttachmentPoints(aps0b);
		graphB.addVertex(v0b);

		ArrayList<DENOPTIMAttachmentPoint> aps1b = new ArrayList<>();
		DENOPTIMVertex v1b = new EmptyVertex(1);
		aps1b.add(new DENOPTIMAttachmentPoint(v1b, 0, 1, 1));
		aps1b.add(new DENOPTIMAttachmentPoint(v1b, 1, 1, 1));
		v1b.setAttachmentPoints(aps1b);
		graphB.addVertex(v1b);
		graphB.addEdge(new DENOPTIMEdge(0, 1, 0, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps2b = new ArrayList<>();
		DENOPTIMVertex v2b = new EmptyVertex(2);
		aps2b.add(new DENOPTIMAttachmentPoint(v2b, 0, 1, 1));
		aps2b.add(new DENOPTIMAttachmentPoint(v2b, 1, 1, 1));
		v2b.setAttachmentPoints(aps2b);
		graphB.addVertex(v2b);
		graphB.addEdge(new DENOPTIMEdge(0, 2, 1, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps3b = new ArrayList<>();
		DENOPTIMVertex v3b = new EmptyVertex(3);
		aps3b.add(new DENOPTIMAttachmentPoint(v3b, 0, 1, 1));
		aps3b.add(new DENOPTIMAttachmentPoint(v3b, 1, 1, 1));
		v3b.setAttachmentPoints(aps3b);
		graphB.addVertex(v3b);
		graphB.addEdge(new DENOPTIMEdge(0, 3, 2, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps4b = new ArrayList<>();
		DENOPTIMVertex v4b = new EmptyVertex(4);
		aps4b.add(new DENOPTIMAttachmentPoint(v4b, 0, 1, 1));
		aps4b.add(new DENOPTIMAttachmentPoint(v4b, 1, 1, 1));
		v4b.setAttachmentPoints(aps4b);
		graphB.addVertex(v4b);
		graphB.addEdge(new DENOPTIMEdge(0, 4, 3, 0));

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
		ArrayList<DENOPTIMAttachmentPoint> aps0 = new ArrayList<>();
		DENOPTIMVertex v0 = new EmptyVertex(0);
		aps0.add(new DENOPTIMAttachmentPoint(v0, 0, 1, 1));
		aps0.add(new DENOPTIMAttachmentPoint(v0, 1, 1, 1));
		aps0.add(new DENOPTIMAttachmentPoint(v0, 2, 1, 1));
		aps0.add(new DENOPTIMAttachmentPoint(v0, 3, 1, 1));
		v0.setAttachmentPoints(aps0);
		graphA.addVertex(v0);

		ArrayList<DENOPTIMAttachmentPoint> aps1 = new ArrayList<>();
		DENOPTIMVertex v1 = new EmptyVertex(1);
		aps1.add(new DENOPTIMAttachmentPoint(v1, 0, 1, 1));
		aps1.add(new DENOPTIMAttachmentPoint(v1, 1, 1, 1));
		v1.setAttachmentPoints(aps1);
		graphA.addVertex(v1);
		graphA.addEdge(new DENOPTIMEdge(0, 1, 0, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps2 = new ArrayList<>();
		DENOPTIMVertex v2 = new EmptyVertex(2);
		aps2.add(new DENOPTIMAttachmentPoint(v2, 0, 1, 1));
		aps2.add(new DENOPTIMAttachmentPoint(v2, 1, 1, 1));
		v2.setAttachmentPoints(aps2);
		graphA.addVertex(v2);
		graphA.addEdge(new DENOPTIMEdge(0, 2, 1, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps3 = new ArrayList<>();
		DENOPTIMVertex v3 = new EmptyVertex(3);
		aps3.add(new DENOPTIMAttachmentPoint(v3, 0, 1, 1));
		aps3.add(new DENOPTIMAttachmentPoint(v3, 1, 1, 1));
		v3.setAttachmentPoints(aps3);
		graphA.addVertex(v3);
		graphA.addEdge(new DENOPTIMEdge(0, 3, 2, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps4 = new ArrayList<>();
		DENOPTIMVertex v4 = new EmptyVertex(4);
		aps4.add(new DENOPTIMAttachmentPoint(v4, 0, 1, 1));
		aps4.add(new DENOPTIMAttachmentPoint(v4, 1, 1, 1));
		v4.setAttachmentPoints(aps4);
		graphA.addVertex(v4);
		graphA.addEdge(new DENOPTIMEdge(0, 4, 3, 0));

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
		ArrayList<DENOPTIMAttachmentPoint> aps0b = new ArrayList<>();
		DENOPTIMVertex v0b = new EmptyVertex(90);
		aps0b.add(new DENOPTIMAttachmentPoint(v0b, 0, 1, 1));
		aps0b.add(new DENOPTIMAttachmentPoint(v0b, 1, 1, 1));
		aps0b.add(new DENOPTIMAttachmentPoint(v0b, 2, 1, 1));
		aps0b.add(new DENOPTIMAttachmentPoint(v0b, 3, 1, 1));
		v0b.setAttachmentPoints(aps0b);
		graphB.addVertex(v0b);

		ArrayList<DENOPTIMAttachmentPoint> aps1b = new ArrayList<>();
		DENOPTIMVertex v1b = new EmptyVertex(91);
		aps1b.add(new DENOPTIMAttachmentPoint(v1b, 0, 1, 1));
		aps1b.add(new DENOPTIMAttachmentPoint(v1b, 1, 1, 1));
		v1b.setAttachmentPoints(aps1b);
		graphB.addVertex(v1b);
		graphB.addEdge(new DENOPTIMEdge(90, 91, 0, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps2b = new ArrayList<>();
		DENOPTIMVertex v2b = new EmptyVertex(92);
		aps2b.add(new DENOPTIMAttachmentPoint(v2b, 0, 1, 1));
		aps2b.add(new DENOPTIMAttachmentPoint(v2b, 1, 1, 1));
		v2b.setAttachmentPoints(aps2b);
		graphB.addVertex(v2b);
		graphB.addEdge(new DENOPTIMEdge(90, 92, 1, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps3b = new ArrayList<>();
		DENOPTIMVertex v3b = new EmptyVertex(93);
		aps3b.add(new DENOPTIMAttachmentPoint(v3b, 0, 1, 1));
		aps3b.add(new DENOPTIMAttachmentPoint(v3b, 1, 1, 1));
		v3b.setAttachmentPoints(aps3b);
		graphB.addVertex(v3b);
		graphB.addEdge(new DENOPTIMEdge(90, 93, 2, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps4b = new ArrayList<>();
		DENOPTIMVertex v4b = new EmptyVertex(94);
		aps4b.add(new DENOPTIMAttachmentPoint(v4b, 0, 1, 1));
		aps4b.add(new DENOPTIMAttachmentPoint(v4b, 1, 1, 1));
		v4b.setAttachmentPoints(aps4b);
		graphB.addVertex(v4b);
		graphB.addEdge(new DENOPTIMEdge(90, 94, 3, 0));

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

		StringBuilder reason = new StringBuilder();
		assertTrue(graphA.sameAs(graphB, reason));
	}

//------------------------------------------------------------------------------

	@Test
	public void testSameAs_DisorderRings() throws Exception {
		DENOPTIMGraph graphA = new DENOPTIMGraph();
		ArrayList<DENOPTIMAttachmentPoint> aps0 = new ArrayList<>();
		DENOPTIMVertex v0 = new EmptyVertex(0);
		aps0.add(new DENOPTIMAttachmentPoint(v0, 0, 1, 1));
		aps0.add(new DENOPTIMAttachmentPoint(v0, 1, 1, 1));
		aps0.add(new DENOPTIMAttachmentPoint(v0, 2, 1, 1));
		aps0.add(new DENOPTIMAttachmentPoint(v0, 3, 1, 1));
		v0.setAttachmentPoints(aps0);
		graphA.addVertex(v0);

		ArrayList<DENOPTIMAttachmentPoint> aps1 = new ArrayList<>();
		DENOPTIMVertex v1 = new EmptyVertex(1);
		aps1.add(new DENOPTIMAttachmentPoint(v1, 0, 1, 1));
		aps1.add(new DENOPTIMAttachmentPoint(v1, 1, 1, 1));
		v1.setAttachmentPoints(aps1);
		graphA.addVertex(v1);
		graphA.addEdge(new DENOPTIMEdge(0, 1, 0, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps2 = new ArrayList<>();
		DENOPTIMVertex v2 = new EmptyVertex(2);
		aps2.add(new DENOPTIMAttachmentPoint(v2, 0, 1, 1));
		aps2.add(new DENOPTIMAttachmentPoint(v2, 1, 1, 1));
		v2.setAttachmentPoints(aps2);
		graphA.addVertex(v2);
		graphA.addEdge(new DENOPTIMEdge(0, 2, 1, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps3 = new ArrayList<>();
		DENOPTIMVertex v3 = new EmptyVertex(3);
		aps3.add(new DENOPTIMAttachmentPoint(v3, 0, 1, 1));
		aps3.add(new DENOPTIMAttachmentPoint(v3, 1, 1, 1));
		v3.setAttachmentPoints(aps3);
		graphA.addVertex(v3);
		graphA.addEdge(new DENOPTIMEdge(0, 3, 2, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps4 = new ArrayList<>();
		DENOPTIMVertex v4 = new EmptyVertex(4);
		aps4.add(new DENOPTIMAttachmentPoint(v4, 0, 1, 1));
		aps4.add(new DENOPTIMAttachmentPoint(v4, 1, 1, 1));
		v4.setAttachmentPoints(aps4);
		graphA.addVertex(v4);
		graphA.addEdge(new DENOPTIMEdge(0, 4, 3, 0));

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
		ArrayList<DENOPTIMAttachmentPoint> aps0b = new ArrayList<>();
		DENOPTIMVertex v0b = new EmptyVertex(90);
		aps0b.add(new DENOPTIMAttachmentPoint(v0b, 0, 1, 1));
		aps0b.add(new DENOPTIMAttachmentPoint(v0b, 1, 1, 1));
		aps0b.add(new DENOPTIMAttachmentPoint(v0b, 2, 1, 1));
		aps0b.add(new DENOPTIMAttachmentPoint(v0b, 3, 1, 1));
		v0b.setAttachmentPoints(aps0b);
		graphB.addVertex(v0b);

		ArrayList<DENOPTIMAttachmentPoint> aps1b = new ArrayList<>();
		DENOPTIMVertex v1b = new EmptyVertex(91);
		aps1b.add(new DENOPTIMAttachmentPoint(v1b, 0, 1, 1));
		aps1b.add(new DENOPTIMAttachmentPoint(v1b, 1, 1, 1));
		v1b.setAttachmentPoints(aps1b);
		graphB.addVertex(v1b);
		graphB.addEdge(new DENOPTIMEdge(90, 91, 0, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps2b = new ArrayList<>();
		DENOPTIMVertex v2b = new EmptyVertex(92);
		aps2b.add(new DENOPTIMAttachmentPoint(v2b, 0, 1, 1));
		aps2b.add(new DENOPTIMAttachmentPoint(v2b, 1, 1, 1));
		v2b.setAttachmentPoints(aps2b);
		graphB.addVertex(v2b);
		graphB.addEdge(new DENOPTIMEdge(90, 92, 1, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps3b = new ArrayList<>();
		DENOPTIMVertex v3b = new EmptyVertex(93);
		aps3b.add(new DENOPTIMAttachmentPoint(v3b, 0, 1, 1));
		aps3b.add(new DENOPTIMAttachmentPoint(v3b, 1, 1, 1));
		v3b.setAttachmentPoints(aps3b);
		graphB.addVertex(v3b);
		graphB.addEdge(new DENOPTIMEdge(90, 93, 2, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps4b = new ArrayList<>();
		DENOPTIMVertex v4b = new EmptyVertex(94);
		aps4b.add(new DENOPTIMAttachmentPoint(v4b, 0, 1, 1));
		aps4b.add(new DENOPTIMAttachmentPoint(v4b, 1, 1, 1));
		v4b.setAttachmentPoints(aps4b);
		graphB.addVertex(v4b);
		graphB.addEdge(new DENOPTIMEdge(90, 94, 3, 0));

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

		StringBuilder reason = new StringBuilder();
		assertTrue(graphA.sameAs(graphB, reason));
	}

//------------------------------------------------------------------------------

	@Test
	public void testSameAs_DiffRings() throws Exception {
		DENOPTIMGraph graphA = new DENOPTIMGraph();
		ArrayList<DENOPTIMAttachmentPoint> aps0 = new ArrayList<>();
		DENOPTIMVertex v0 = new EmptyVertex(0);
		aps0.add(new DENOPTIMAttachmentPoint(v0, 0, 1, 1));
		aps0.add(new DENOPTIMAttachmentPoint(v0, 1, 1, 1));
		aps0.add(new DENOPTIMAttachmentPoint(v0, 2, 1, 1));
		aps0.add(new DENOPTIMAttachmentPoint(v0, 3, 1, 1));
		v0.setAttachmentPoints(aps0);
		graphA.addVertex(v0);

		ArrayList<DENOPTIMAttachmentPoint> aps1 = new ArrayList<>();
		DENOPTIMVertex v1 = new EmptyVertex(1);
		aps1.add(new DENOPTIMAttachmentPoint(v1, 0, 1, 1));
		aps1.add(new DENOPTIMAttachmentPoint(v1, 1, 1, 1));
		v1.setAttachmentPoints(aps1);
		graphA.addVertex(v1);
		graphA.addEdge(new DENOPTIMEdge(0, 1, 0, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps2 = new ArrayList<>();
		DENOPTIMVertex v2 = new EmptyVertex(2);
		aps2.add(new DENOPTIMAttachmentPoint(v2, 0, 1, 1));
		aps2.add(new DENOPTIMAttachmentPoint(v2, 1, 1, 1));
		v2.setAttachmentPoints(aps2);
		graphA.addVertex(v2);
		graphA.addEdge(new DENOPTIMEdge(0, 2, 1, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps3 = new ArrayList<>();
		DENOPTIMVertex v3 = new EmptyVertex(3);
		aps3.add(new DENOPTIMAttachmentPoint(v3, 0, 1, 1));
		aps3.add(new DENOPTIMAttachmentPoint(v3, 1, 1, 1));
		v3.setAttachmentPoints(aps3);
		graphA.addVertex(v3);
		graphA.addEdge(new DENOPTIMEdge(0, 3, 2, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps4 = new ArrayList<>();
		DENOPTIMVertex v4 = new EmptyVertex(4);
		aps4.add(new DENOPTIMAttachmentPoint(v4, 0, 1, 1));
		aps4.add(new DENOPTIMAttachmentPoint(v4, 1, 1, 1));
		v4.setAttachmentPoints(aps4);
		graphA.addVertex(v4);
		graphA.addEdge(new DENOPTIMEdge(0, 4, 3, 0));

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
		ArrayList<DENOPTIMAttachmentPoint> aps0b = new ArrayList<>();
		DENOPTIMVertex v0b = new EmptyVertex(90);
		aps0b.add(new DENOPTIMAttachmentPoint(v0b, 0, 1, 1));
		aps0b.add(new DENOPTIMAttachmentPoint(v0b, 1, 1, 1));
		aps0b.add(new DENOPTIMAttachmentPoint(v0b, 2, 1, 1));
		aps0b.add(new DENOPTIMAttachmentPoint(v0b, 3, 1, 1));
		v0b.setAttachmentPoints(aps0b);
		graphB.addVertex(v0b);

		ArrayList<DENOPTIMAttachmentPoint> aps1b = new ArrayList<>();
		DENOPTIMVertex v1b = new EmptyVertex(91);
		aps1b.add(new DENOPTIMAttachmentPoint(v1b, 0, 1, 1));
		aps1b.add(new DENOPTIMAttachmentPoint(v1b, 1, 1, 1));
		v1b.setAttachmentPoints(aps1b);
		graphB.addVertex(v1b);
		graphB.addEdge(new DENOPTIMEdge(90, 91, 0, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps2b = new ArrayList<>();
		DENOPTIMVertex v2b = new EmptyVertex(92);
		aps2b.add(new DENOPTIMAttachmentPoint(v2b, 0, 1, 1));
		aps2b.add(new DENOPTIMAttachmentPoint(v2b, 1, 1, 1));
		v2b.setAttachmentPoints(aps2b);
		graphB.addVertex(v2b);
		graphB.addEdge(new DENOPTIMEdge(90, 92, 1, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps3b = new ArrayList<>();
		DENOPTIMVertex v3b = new EmptyVertex(93);
		aps3b.add(new DENOPTIMAttachmentPoint(v3b, 0, 1, 1));
		aps3b.add(new DENOPTIMAttachmentPoint(v3b, 1, 1, 1));
		v3b.setAttachmentPoints(aps3b);
		graphB.addVertex(v3b);
		graphB.addEdge(new DENOPTIMEdge(90, 93, 2, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps4b = new ArrayList<>();
		DENOPTIMVertex v4b = new EmptyVertex(94);
		aps4b.add(new DENOPTIMAttachmentPoint(v4b, 0, 1, 1));
		aps4b.add(new DENOPTIMAttachmentPoint(v4b, 1, 1, 1));
		v4b.setAttachmentPoints(aps4b);
		graphB.addVertex(v4b);
		graphB.addEdge(new DENOPTIMEdge(90, 94, 3, 0));

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

    	DENOPTIMEdge edge0 = new DENOPTIMEdge(vertex0.getVertexId(),
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
		ArrayList<DENOPTIMAttachmentPoint> aps0 = new ArrayList<>();
		DENOPTIMVertex v0 = new EmptyVertex(0);
		aps0.add(new DENOPTIMAttachmentPoint(v0, 0, 1, 1));
		aps0.add(new DENOPTIMAttachmentPoint(v0, 1, 1, 1));
		aps0.add(new DENOPTIMAttachmentPoint(v0, 2, 1, 1));
		v0.setAttachmentPoints(aps0);
		graph.addVertex(v0);

		ArrayList<DENOPTIMAttachmentPoint> aps1 = new ArrayList<>();
		DENOPTIMVertex v1 = new EmptyVertex(1);
		aps1.add(new DENOPTIMAttachmentPoint(v1, 0, 1, 1));
		aps1.add(new DENOPTIMAttachmentPoint(v1, 1, 1, 1));
		v1.setAttachmentPoints(aps1);
		graph.addVertex(v1);
		graph.addEdge(new DENOPTIMEdge(0, 1, 0, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps2 = new ArrayList<>();
		DENOPTIMVertex v2 = new EmptyVertex(2);
		aps2.add(new DENOPTIMAttachmentPoint(v2, 0, 1, 1));
		aps2.add(new DENOPTIMAttachmentPoint(v2, 1, 1, 1));
		v2.setAttachmentPoints(aps2);
		graph.addVertex(v2);
		graph.addEdge(new DENOPTIMEdge(1, 2, 1, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps3 = new ArrayList<>();
		DENOPTIMVertex v3 = new EmptyVertex(3);
		aps3.add(new DENOPTIMAttachmentPoint(v3, 0, 1, 1));
		v3.setAttachmentPoints(aps3);
		graph.addVertex(v3);
		graph.addEdge(new DENOPTIMEdge(2, 3, 1, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps4 = new ArrayList<>();
		DENOPTIMVertex v4 = new EmptyVertex(4);
		aps4.add(new DENOPTIMAttachmentPoint(v4, 0, 1, 1));
		aps4.add(new DENOPTIMAttachmentPoint(v4, 1, 1, 1));
		aps4.add(new DENOPTIMAttachmentPoint(v4, 2, 1, 1));
		v4.setAttachmentPoints(aps4);
		graph.addVertex(v4);
		graph.addEdge(new DENOPTIMEdge(0, 4, 1, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps5 = new ArrayList<>();
		DENOPTIMVertex v5 = new EmptyVertex(5);
		aps5.add(new DENOPTIMAttachmentPoint(v5, 0, 1, 1));
		v5.setAttachmentPoints(aps5);
		graph.addVertex(v5);
		graph.addEdge(new DENOPTIMEdge(4, 5, 1, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps6 = new ArrayList<>();
		DENOPTIMVertex v6 = new EmptyVertex(6);
		aps6.add(new DENOPTIMAttachmentPoint(v6, 0, 1, 1));
		v6.setAttachmentPoints(aps6);
		graph.addVertex(v6);
		graph.addEdge(new DENOPTIMEdge(0, 6, 2, 0));

		ArrayList<DENOPTIMAttachmentPoint> aps7 = new ArrayList<>();
		DENOPTIMVertex v7 = new EmptyVertex(7);
		aps7.add(new DENOPTIMAttachmentPoint(v7, 0, 1, 1));
		v7.setAttachmentPoints(aps7);
		graph.addVertex(v7);
		graph.addEdge(new DENOPTIMEdge(4, 7, 2, 0));

		graph.addRing(new DENOPTIMRing(new ArrayList<DENOPTIMVertex>(
				Arrays.asList(v5, v4, v0, v1, v2, v3))));

		graph.addRing(new DENOPTIMRing(new ArrayList<DENOPTIMVertex>(
				Arrays.asList(v6, v0, v4, v7))));

		graph.addSymmetricSetOfVertices(new SymmetricSet(
				new ArrayList<Integer>(Arrays.asList(3, 5))));

		graph.addSymmetricSetOfVertices(new SymmetricSet(
				new ArrayList<Integer>(Arrays.asList(6, 7))));

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
    public void testGetMutationSites() throws Exception
    {
        DENOPTIMGraph graph = new DENOPTIMGraph();
        DENOPTIMTemplate tmpl = DENOPTIMTemplate.getTestTemplate(2);
        graph.addVertex(tmpl);
        
        assertEquals(1,graph.getMutableSites().size(),
                "Size of mutation size list in case of frozen template");
       
        graph = new DENOPTIMGraph();
        tmpl = DENOPTIMTemplate.getTestTemplate(0);
        graph.addVertex(tmpl);
        
        assertEquals(2,graph.getMutableSites().size(),
                "Size of mutation size list in case of free template");
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testRemoveCapping() throws Exception
    {
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
        graph.addEdge(new DENOPTIMEdge(1, 2, 0, 0));
        graph.addEdge(new DENOPTIMEdge(1, 3, 1, 0));
        graph.addEdge(new DENOPTIMEdge(2, 4, 1, 0));
        
        assertEquals(4,graph.getVertexCount(),
                "#vertexes in graph before removal");
        assertTrue(graph == v4.getGraphOwner());
        
        graph.removeCappingGroupsOn(v2);

        assertEquals(3,graph.getVertexCount(),
                "#vertexes in graph before removal");
        assertFalse(graph.containsVertex(v4), 
                "Capping is still contained");
        assertTrue(null == v4.getGraphOwner(), 
                "Owner of removed capping group is null");
        
        
        DENOPTIMGraph graph2 = new DENOPTIMGraph();
        
        IAtomContainer iac12 = new AtomContainer();
        iac12.addAtom(new Atom("C"));
        DENOPTIMVertex v12 = new DENOPTIMFragment(21, iac12, BBType.SCAFFOLD);
        v12.addAttachmentPoint(new DENOPTIMAttachmentPoint(v12, 0, 1, 1));
        v12.addAttachmentPoint(new DENOPTIMAttachmentPoint(v12, 0, 1, 1));

        IAtomContainer iac22 = new AtomContainer();
        iac22.addAtom(new Atom("O"));
        DENOPTIMVertex v22 = new DENOPTIMFragment(22, iac22, BBType.FRAGMENT);
        v22.addAttachmentPoint(new DENOPTIMAttachmentPoint(v22, 0, 1, 1));
        v22.addAttachmentPoint(new DENOPTIMAttachmentPoint(v22, 0, 1, 1));
        
        IAtomContainer iac32 = new AtomContainer();
        iac32.addAtom(new Atom("H"));
        DENOPTIMVertex v32 = new DENOPTIMFragment(23, iac32, BBType.CAP);
        v32.addAttachmentPoint(new DENOPTIMAttachmentPoint(v32, 0, 1, 1));
        
        IAtomContainer iac42 = new AtomContainer();
        iac42.addAtom(new Atom("H"));
        DENOPTIMVertex v42 = new DENOPTIMFragment(24, iac42, BBType.CAP);
        v42.addAttachmentPoint(new DENOPTIMAttachmentPoint(v42, 0, 1, 1));
        
        graph2.addVertex(v12);
        graph2.addVertex(v22);
        graph2.addVertex(v32);
        graph2.addVertex(v42);
        graph2.addEdge(new DENOPTIMEdge(21, 22, 0, 0));
        graph2.addEdge(new DENOPTIMEdge(21, 23, 1, 0));
        graph2.addEdge(new DENOPTIMEdge(22, 24, 1, 0));
        
        assertEquals(4,graph2.getVertexCount(),
                "#vertexes in graph before removal (B)");
        assertTrue(graph2 == v32.getGraphOwner());
        assertTrue(graph2 == v42.getGraphOwner());
        
        graph2.removeCappingGroups();

        assertEquals(2,graph2.getVertexCount(),
                "#vertexes in graph before removal (B)");
        assertFalse(graph.containsVertex(v42), 
                "Capping is still contained (B)");
        assertFalse(graph.containsVertex(v32), 
                "Capping is still contained (C)");
        assertTrue(null == v42.getGraphOwner(), 
                "Owner of removed capping group is null (B)");
        assertTrue(null == v32.getGraphOwner(), 
                "Owner of removed capping group is null (C)");
    }
    
//------------------------------------------------------------------------------

}
