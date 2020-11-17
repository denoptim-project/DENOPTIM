package denoptim.io;

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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.interfaces.IAtom;

import denoptim.fragspace.FragmentSpace;
import denoptim.molecule.APClass;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMEdge;
import denoptim.molecule.DENOPTIMFragment;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMRing;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.molecule.EmptyVertex;
import denoptim.molecule.SymmetricSet;
import denoptim.molecule.DENOPTIMEdge.BondType;

/**
 * Unit test for input/output
 * 
 * @author Marco Foscato
 */

public class DenoptimIOTest {


//------------------------------------------------------------------------------

	@Test
	public void testSerializeDeserializeDENOPTIMGraphs() throws Exception {
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

		String tmpFile = DenoptimIO.getTempFolder()
				+ System.getProperty("file.separator") + "_unit.ser";
		DenoptimIO.serializeToFile(tmpFile, graph, false);

		DENOPTIMGraph graphA = DenoptimIO.deserializeDENOPTIMGraph(
				new File(tmpFile));

		StringBuilder reason = new StringBuilder();
		assertTrue(graph.sameAs(graphA, reason));
	}

//------------------------------------------------------------------------------

	@Test
	public void testReadAllAPClasses() throws Exception {
		// This is just to avoid the warnings about trying to get a bond type
		// when the fragment space in not defined
		HashMap<String, BondType> map = new HashMap<String, BondType>();
		map.put("classAtmC", BondType.SINGLE);
		map.put("otherClass", BondType.SINGLE);
		map.put("classAtmH", BondType.SINGLE);
		map.put("apClassO", BondType.SINGLE);
		map.put("apClassObis", BondType.SINGLE);
		map.put("", BondType.SINGLE);
		map.put("", BondType.SINGLE);
		map.put("", BondType.SINGLE);
		map.put("", BondType.SINGLE);
		map.put("", BondType.SINGLE);
		FragmentSpace.setBondOrderMap(map);

		DENOPTIMFragment frag = new DENOPTIMFragment();
		IAtom atmC = new Atom("C");
		atmC.setPoint3d(new Point3d(0.0, 0.0, 1.0));
		IAtom atmH = new Atom("H");
		atmH.setPoint3d(new Point3d(0.0, 1.0, 1.0));
		frag.addAtom(atmC);
		frag.addAtom(atmH);
		frag.addAP(atmC, APClass.make("classAtmC:5"), 
		        new Point3d(1.0, 0.0, 0.0));
		frag.addAP(atmC, APClass.make("classAtmC:5"), 
		        new Point3d(1.0, 1.0, 0.0));
		frag.addAP(atmC, APClass.make("otherClass:0"), 
		        new Point3d(-1.0, 0.0, 0.0));
		frag.addAP(atmH, APClass.make("classAtmH:1"), 
		        new Point3d(1.0, 2.0, 2.0));
		frag.projectAPsToProperties();

		DENOPTIMFragment frag2 = new DENOPTIMFragment();
		IAtom atmO = new Atom("O");
		atmO.setPoint3d(new Point3d(0.0, 0.0, 1.0));
		IAtom atmH2 = new Atom("N");
		atmH.setPoint3d(new Point3d(0.0, 1.0, 1.0));
		frag2.addAtom(atmO);
		frag2.addAtom(atmH2);
		frag2.addAP(atmO, APClass.make("apClassO:5"), 
		        new Point3d(1.0, 0.0, 0.0));
		frag2.addAP(atmO, APClass.make("apClassO:6"), 
		        new Point3d(1.0, 1.0, 0.0));
		frag2.addAP(atmO, APClass.make("apClassObis:0"), 
		        new Point3d(-1.0, 0.0, 0.0));
		frag2.addAP(atmH2, APClass.make("classAtmH:1"), 
		        new Point3d(1.0, 2.0, 2.0));
		frag2.projectAPsToProperties();

		ArrayList<DENOPTIMFragment> frags = new ArrayList<DENOPTIMFragment>();
		frags.add(frag);
		frags.add(frag2);

		String tmpFile = DenoptimIO.getTempFolder()
				+ System.getProperty("file.separator") + "frag.sdf";
		DenoptimIO.writeFragmentSet(tmpFile, frags);

		Set<APClass> allAPC = DenoptimIO.readAllAPClasses(new File(tmpFile));

		assertEquals(6, allAPC.size(), "Size did not match");
		assertTrue(allAPC.contains(APClass.make("apClassObis:0")), 
		        "Contains APClass (1)");
		assertTrue(allAPC.contains(APClass.make("otherClass:0")), 
		        "Contains APClass (2)");
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