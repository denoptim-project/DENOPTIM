package denoptim.molecularmodeling;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.Bond;

import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.graph.APClass;
import denoptim.graph.Edge;
import denoptim.graph.Edge.BondType;
import denoptim.graph.Fragment;
import denoptim.graph.DGraph;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;
import denoptim.molecularmodeling.ThreeDimTreeBuilder;
import denoptim.graph.EmptyVertex;

/**
 * Unit test for TreeBuilder3D
 * 
 * @author Marco Foscato
 */

public class ThreeDimTreeBuilderTest
{
	
//------------------------------------------------------------------------------
	
    @Test
    public void testConversionTo3dTree() throws Exception
    {
        APClass a0 = APClass.make("a",0,BondType.SINGLE);
        APClass b0 = APClass.make("b",0,BondType.SINGLE);
        APClass h0 = APClass.make("h",0,BondType.SINGLE);
        APClass ap0 = APClass.make("ATplus:0");
        APClass am0 = APClass.make("ATminus:0");
        
        HashMap<APClass,ArrayList<APClass>> cpMap = 
                new HashMap<APClass,ArrayList<APClass>>();
        cpMap.put(a0, new ArrayList<APClass>(Arrays.asList(a0, b0)));
        HashMap<APClass,APClass> capMap = new HashMap<APClass,APClass>();
        capMap.put(a0, h0);
        HashSet<APClass> forbEnds = new HashSet<APClass>();
        forbEnds.add(b0);
        
        
    	Fragment frg1 = new Fragment();
    	IAtom a1 = new Atom("C", new Point3d(new double[]{0.0, 0.0, 0.0}));
    	IAtom a2 = new Atom("C", new Point3d(new double[]{1.0, 0.0, 0.0}));
    	frg1.addAtom(a1);
    	frg1.addAtom(a2);
    	frg1.addBond(new Bond(a1, a2));
    	frg1.addAP(0, a0, new Point3d(new double[]{0.0, 0.0, 1.0}));
    	frg1.addAP(1, a0, new Point3d(new double[]{1.0, 1.0, 1.0}));
    	frg1.projectAPsToProperties(); 
    	
    	Fragment frg2 = new Fragment();
    	IAtom a3 = new Atom("C", new Point3d(new double[]{0.0, 0.0, 0.0}));
    	frg2.addAtom(a3);
    	frg2.addAP(0, a0, new Point3d(new double[]{0.0, 1.0, 1.0}));
    	frg2.addAP(0, b0, new Point3d(new double[]{0.0, 1.0, -1.0}));   
    	frg2.projectAPsToProperties(); 
    	
        Fragment frg3 = new Fragment();
        IAtom a7 = new Atom("C", new Point3d(new double[]{2.0, 0.0, 0.0}));
        IAtom a8 = new Atom("O", new Point3d(new double[]{3.0, 0.0, 0.0}));
        frg3.addAtom(a7);
        frg3.addAtom(a8);
        frg3.addBond(new Bond(a7, a8));
        frg3.addAP(0, a0, new Point3d(new double[]{2.0, 1.0, 1.0}));
        frg3.addAP(0, a0, new Point3d(new double[]{2.0, 1.0, -1.0}));  
        frg3.addAP(0, b0, new Point3d(new double[]{3.0, 1.0, -1.0})); 
        frg3.projectAPsToProperties(); 

    	Fragment rca1 = new Fragment();
    	IAtom a4 = new PseudoAtom("ATP", new Point3d(
    	        new double[]{0.0, 0.0, 0.0}));
    	rca1.addAtom(a4);
    	rca1.addAP(0, ap0, new Point3d(new double[]{0.0, 1.0, 1.0}));
    	rca1.projectAPsToProperties(); 
    	
    	Fragment rca2 = new Fragment();
    	IAtom a5 = new PseudoAtom("ATM", new Point3d(
    	        new double[]{1.0, 0.0, 0.0}));
    	rca2.addAtom(a5);
    	rca2.addAP(0, am0, new Point3d(new double[]{0.0, 1.0, 1.0}));
    	rca2.projectAPsToProperties(); 
    	
    	Fragment cap = new Fragment();
    	IAtom a6 = new Atom("H", new Point3d(new double[]{0.0, 0.0, 0.0}));
    	cap.addAtom(a6);
    	cap.addAP(0, h0, new Point3d(new double[]{0.0, 1.0, 1.0}));
    	cap.projectAPsToProperties(); 
    	
    	ArrayList<Vertex> scaff = new ArrayList<Vertex>();
    	scaff.add(frg1);
    	ArrayList<Vertex> frags = new ArrayList<Vertex>();
    	frags.add(frg2);
    	frags.add(rca1);
    	frags.add(rca2);
    	frags.add(frg3);
    	ArrayList<Vertex> caps = new ArrayList<Vertex>();
    	caps.add(cap);
    	
    	FragmentSpaceParameters fsp = new FragmentSpaceParameters();
        FragmentSpace fs = new FragmentSpace(fsp, scaff, frags, caps,
                cpMap, capMap, forbEnds, cpMap);
        fs.setAPclassBasedApproach(true);
    	
    	DGraph g1 = new DGraph();
    	Vertex v1 = Vertex.newVertexFromLibrary(1, 0, 
    	        BBType.SCAFFOLD, fs);
    	Vertex v2 = Vertex.newVertexFromLibrary(2, 0, 
                BBType.FRAGMENT, fs);
    	Vertex v3 = Vertex.newVertexFromLibrary(3, 2, 
                BBType.FRAGMENT, fs);
    	Vertex v4 = Vertex.newVertexFromLibrary(4, 1, 
                BBType.FRAGMENT, fs);
        g1.addVertex(v1);
        g1.addVertex(v2);
        g1.addVertex(v3);
        g1.addVertex(v4);
        
        Edge e1 = new Edge(v1.getAP(1), v2.getAP(0), 
                BondType.SINGLE);
        Edge e2 = new Edge(v2.getAP(1), v3.getAP(0), 
                BondType.SINGLE);
        Edge e3 = new Edge(v1.getAP(0), v4.getAP(0), 
                BondType.SINGLE);
        g1.addEdge(e1);
        g1.addEdge(e2);
        g1.addEdge(e3);
        
        g1.addRing(v3, v4);
        
    	ThreeDimTreeBuilder t3d = new ThreeDimTreeBuilder();
    	
    	IAtomContainer mol = t3d.convertGraphTo3DAtomContainer(g1,false);
    	assertEquals(4, mol.getBondCount(), "Number of bonds without the "
    			+ "cyclic one");
    	assertEquals(5, mol.getAtomCount(), "Number of atoms in cyclic molecule"
    			+ " before forming ring");
    	
    	mol = t3d.convertGraphTo3DAtomContainer(g1,true);
    	// NB: no RCAs anymore, so two atoms less than before (the RCAs), 
    	// and two bonds less than the moment when we have made 
    	// the ring-closing bond but we have not yet removed the RCAs. 
    	// Basically we add 1 bond and remove 2.
    	assertEquals(3, mol.getBondCount(), "Number of bonds, including the "
    			+ "cyclic one");
    	assertEquals(3, mol.getAtomCount(), "Number of atoms in cyclic "
    			+ "molecule after removal of RCAs");
    	
    	
        DGraph g2 = new DGraph();
        Vertex v1b = Vertex.newVertexFromLibrary(1, 0, 
                BBType.SCAFFOLD, fs);
        Vertex v2b = Vertex.newVertexFromLibrary(2, 0, 
                BBType.FRAGMENT, fs);
        Vertex v3b = Vertex.newVertexFromLibrary(3, 2, 
                BBType.FRAGMENT, fs);
        Vertex v4b = Vertex.newVertexFromLibrary(4, 1, 
                BBType.FRAGMENT, fs);
        g2.addVertex(v1b);
        g2.addVertex(v2b);
        g2.addVertex(v3b);
        g2.addVertex(v4b);
        
        Edge e1b = new Edge(v1b.getAP(1), v2b.getAP(0), 
                BondType.SINGLE);
        Edge e2b = new Edge(v2b.getAP(1), v3b.getAP(0), 
                BondType.SINGLE);
        Edge e3b = new Edge(v1b.getAP(0), v4b.getAP(0), 
                BondType.SINGLE);
        g2.addEdge(e1b);
        g2.addEdge(e2b);
        g2.addEdge(e3b);
    	
    	IAtomContainer acyclicMol = t3d.convertGraphTo3DAtomContainer(
    			g2,true);
    	assertEquals(4, acyclicMol.getBondCount(), "Number of bonds in acyclic "
    			+ "graph with RCAs");
    	assertEquals(5, acyclicMol.getAtomCount(), "Number of atoms in acyclic "
    			+ "molecule before forming ring");
    	
    	
        DGraph g3 = new DGraph();
        EmptyVertex v1c = new EmptyVertex(1);
        v1c.addAP(a0);
        v1c.addAP(a0);
        Vertex v2c = Vertex.newVertexFromLibrary(2, 0, 
                BBType.FRAGMENT, fs);
        Vertex v3c = Vertex.newVertexFromLibrary(3, 2, 
                BBType.FRAGMENT, fs);
        Vertex v4c = Vertex.newVertexFromLibrary(4, 1, 
                BBType.FRAGMENT, fs);
        g3.addVertex(v1c);
        g3.addVertex(v2c);
        g3.addVertex(v3c);
        g3.addVertex(v4c);
        
        Edge e1c = new Edge(v1c.getAP(1), v2c.getAP(0), 
                BondType.SINGLE);
        Edge e2c = new Edge(v2c.getAP(1), v3c.getAP(0), 
                BondType.SINGLE);
        Edge e3c = new Edge(v1c.getAP(0), v4c.getAP(0), 
                BondType.SINGLE);
        g3.addEdge(e1c);
        g3.addEdge(e2c);
        g3.addEdge(e3c);
        
        IAtomContainer molFromEmptyScaff = t3d.convertGraphTo3DAtomContainer(
                g3,true);
        assertEquals(1, molFromEmptyScaff.getBondCount(), "Number of bonds in "
                + "mol with empty scaffold");
        assertEquals(3, molFromEmptyScaff.getAtomCount(), "Number of atoms in "
                + "mol with empty scaffold");
        
        
        DGraph g4 = new DGraph();
        EmptyVertex v1d = new EmptyVertex(1);
        v1d.addAP(a0);
        v1d.addAP(a0);
        EmptyVertex v2d = new EmptyVertex(2);
        v2d.addAP(a0);
        v2d.addAP(b0);
        Vertex v3d = Vertex.newVertexFromLibrary(3, 3, 
                BBType.FRAGMENT, fs);
        Vertex v4d = Vertex.newVertexFromLibrary(4, 0, 
                BBType.FRAGMENT, fs);
        EmptyVertex v5d = new EmptyVertex(5);
        v5d.addAP(b0);
        v5d.addAP(b0);
        Vertex v6d = Vertex.newVertexFromLibrary(6, 0, 
                BBType.FRAGMENT, fs);
        Vertex v7d = Vertex.newVertexFromLibrary(7, 0, 
                BBType.FRAGMENT, fs);
        g4.addVertex(v1d);
        g4.addVertex(v2d);
        g4.addVertex(v3d);
        g4.addVertex(v4d);
        g4.addVertex(v5d);
        g4.addVertex(v6d);
        g4.addVertex(v7d);
        
        Edge e1d = new Edge(v1d.getAP(1), v2d.getAP(0), 
                BondType.SINGLE);
        Edge e2d = new Edge(v2d.getAP(1), v7d.getAP(1), 
                BondType.SINGLE);
        Edge e3d = new Edge(v1d.getAP(0), v3d.getAP(0), 
                BondType.SINGLE);
        Edge e4d = new Edge(v3d.getAP(1), v4d.getAP(0), 
                BondType.SINGLE);
        Edge e5d = new Edge(v3d.getAP(2), v5d.getAP(1), 
                BondType.SINGLE);
        Edge e6d = new Edge(v5d.getAP(0), v6d.getAP(1), 
                BondType.SINGLE);
        g4.addEdge(e1d);
        g4.addEdge(e2d);
        g4.addEdge(e3d);
        g4.addEdge(e4d);
        g4.addEdge(e5d);
        g4.addEdge(e6d);
        
        IAtomContainer molWithEmptyNodes = t3d.convertGraphTo3DAtomContainer(
                g4,true);
        assertEquals(2, molWithEmptyNodes.getBondCount(), "Number of bonds in "
                + "mol with empty scaffold and other nodes");
        assertEquals(5, molWithEmptyNodes.getAtomCount(), "Number of atoms in "
                + "mol with empty scaffold and other nodes");
    }
    
//------------------------------------------------------------------------------
    
}
