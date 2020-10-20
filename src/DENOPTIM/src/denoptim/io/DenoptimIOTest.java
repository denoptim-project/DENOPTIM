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
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.interfaces.IAtom;

import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMEdge;
import denoptim.molecule.DENOPTIMFragment;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMRing;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.molecule.EmptyVertex;
import denoptim.molecule.SymmetricSet;

/**
 * Unit test for input/output
 * 
 * @author Marco Foscato
 */

public class DenoptimIOTest
{

	
//------------------------------------------------------------------------------
	
    @Test
    public void testSerializeDeserializeDENOPTIMGraphs() throws Exception
    {
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
    	graph.addEdge(new DENOPTIMEdge(0, 1, 0, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps2 = new ArrayList<>();
		DENOPTIMVertex v2 = new EmptyVertex(2);
		aps2.add(new DENOPTIMAttachmentPoint(v2, 0, 1, 1));
		aps2.add(new DENOPTIMAttachmentPoint(v2, 1, 1, 1));
		v2.setAttachmentPoints(aps2);
    	graph.addVertex(v2);
    	graph.addEdge(new DENOPTIMEdge(1, 2, 1, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps3 = new ArrayList<>();
		DENOPTIMVertex v3 = new EmptyVertex(3);
		aps3.add(new DENOPTIMAttachmentPoint(v3, 0, 1, 1));
		v3.setAttachmentPoints(aps3);
    	graph.addVertex(v3);
    	graph.addEdge(new DENOPTIMEdge(2, 3, 1, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps4 = new ArrayList<>();
		DENOPTIMVertex v4 = new EmptyVertex(4);
		aps4.add(new DENOPTIMAttachmentPoint(v4, 0, 1, 1));
		aps4.add(new DENOPTIMAttachmentPoint(v4, 1, 1, 1));
		aps4.add(new DENOPTIMAttachmentPoint(v4, 2, 1, 1));
		v4.setAttachmentPoints(aps4);
		graph.addVertex(v4);
		graph.addEdge(new DENOPTIMEdge(0, 4, 1, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps5 = new ArrayList<>();
		DENOPTIMVertex v5 = new EmptyVertex(5);
		aps5.add(new DENOPTIMAttachmentPoint(v5, 0, 1, 1));
		v5.setAttachmentPoints(aps5);
    	graph.addVertex(v5);
    	graph.addEdge(new DENOPTIMEdge(4, 5, 1, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps6 = new ArrayList<>();
		DENOPTIMVertex v6 = new EmptyVertex(6);
		aps6.add(new DENOPTIMAttachmentPoint(v6, 0, 1, 1));
		v6.setAttachmentPoints(aps6);
    	graph.addVertex(v6);
    	graph.addEdge(new DENOPTIMEdge(0, 6, 2, 0, 1));
    	
    	ArrayList<DENOPTIMAttachmentPoint> aps7 = new ArrayList<>();
		DENOPTIMVertex v7 = new EmptyVertex(7);
		aps7.add(new DENOPTIMAttachmentPoint(v7, 0, 1, 1));
		v7.setAttachmentPoints(aps7);
    	graph.addVertex(v7);
    	graph.addEdge(new DENOPTIMEdge(4, 7, 2, 0, 1));
    	
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
    public void testReadAllAPClasses() throws Exception
    {
    	DENOPTIMFragment frag = new DENOPTIMFragment();
    	IAtom atmC = new Atom("C");
    	atmC.setPoint3d(new Point3d(0.0, 0.0, 1.0));
    	IAtom atmH = new Atom("H");
    	atmH.setPoint3d(new Point3d(0.0, 1.0, 1.0));
    	frag.addAtom(atmC);
    	frag.addAtom(atmH);
    	frag.addAP(atmC, "classAtmC:5", new Point3d(1.0, 0.0, 0.0));
    	frag.addAP(atmC, "classAtmC:5", new Point3d(1.0, 1.0, 0.0));
    	frag.addAP(atmC, "otherClass:0", new Point3d(-1.0, 0.0, 0.0));
    	frag.addAP(atmH, "classAtmH:1", new Point3d(1.0, 2.0, 2.0));
    	frag.projectAPsToProperties();
    	
    	DENOPTIMFragment frag2 = new DENOPTIMFragment();
    	IAtom atmO = new Atom("O");
    	atmO.setPoint3d(new Point3d(0.0, 0.0, 1.0));
    	IAtom atmH2 = new Atom("N");
    	atmH.setPoint3d(new Point3d(0.0, 1.0, 1.0));
    	frag2.addAtom(atmO);
    	frag2.addAtom(atmH2);
    	frag2.addAP(atmO, "apClassO:5", new Point3d(1.0, 0.0, 0.0));
    	frag2.addAP(atmO, "apClassO:6", new Point3d(1.0, 1.0, 0.0));
    	frag2.addAP(atmO, "apClassObis:0", new Point3d(-1.0, 0.0, 0.0));
    	frag2.addAP(atmH2, "classAtmH:1", new Point3d(1.0, 2.0, 2.0)); 	
    	frag2.projectAPsToProperties();
    	
    	ArrayList<DENOPTIMFragment> frags = new ArrayList<DENOPTIMFragment>();
    	frags.add(frag);
    	frags.add(frag2);
    	
    	String tmpFile = DenoptimIO.getTempFolder() 
    			+ System.getProperty("file.separator") + "frag.sdf";
    	DenoptimIO.writeFragmentSet(tmpFile, frags);
    	
    	Set<String> allAPC = DenoptimIO.readAllAPClasses(new File(tmpFile));
    	
    	assertEquals(6,allAPC.size(),"Size did not match");
    	assertTrue(allAPC.contains("apClassObis:0"),"Contains failed (1)");
    	assertTrue(allAPC.contains("otherClass:0"),"Contains failed (2)");
    }
    
//------------------------------------------------------------------------------
    
}
