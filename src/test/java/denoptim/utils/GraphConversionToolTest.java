package denoptim.utils;

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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import javax.vecmath.Point3d;

import org.jgrapht.graph.DefaultUndirectedGraph;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.silent.Bond;

import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.ga.PopulationTest;
import denoptim.graph.APClass;
import denoptim.graph.Edge;
import denoptim.graph.Edge.BondType;
import denoptim.graph.Fragment;
import denoptim.graph.DGraph;
import denoptim.graph.DENOPTIMGraphTest;
import denoptim.graph.Template;
import denoptim.graph.Vertex;
import denoptim.io.DenoptimIO;
import denoptim.graph.Vertex.BBType;
import denoptim.graph.simplified.Node;
import denoptim.graph.simplified.NodeConnection;

/**
 * Unit test for GraphConversionTool
 * 
 * @author Marco Foscato
 */

public class GraphConversionToolTest
{
	
//------------------------------------------------------------------------------
	
    @Test
    public void testRemoveUnusedRCVs() throws Exception
    {
        APClass a0 = APClass.make("a",0,BondType.SINGLE);
        APClass b0 = APClass.make("b",0,BondType.SINGLE);
        APClass h0 = APClass.make("h",0,BondType.SINGLE);
        APClass ap0 = APClass.make("ATplus",0);
        APClass am0 = APClass.make("ATminus",0);
        
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
    	ArrayList<Vertex> caps = new ArrayList<Vertex>();
    	caps.add(cap);
    	
    	FragmentSpaceParameters fsp = new FragmentSpaceParameters();
    	FragmentSpace fs = new FragmentSpace(fsp, scaff, frags, caps,
                cpMap, capMap, forbEnds, cpMap);
        fs.setAPclassBasedApproach(true);
    	
    	DGraph dg = new DGraph();
    	Vertex v1 = Vertex.newVertexFromLibrary(1, 0, 
    	        BBType.SCAFFOLD, fs);
    	Vertex v2 = Vertex.newVertexFromLibrary(2, 0, 
                BBType.FRAGMENT, fs);
    	Vertex v3 = Vertex.newVertexFromLibrary(3, 2, 
                BBType.FRAGMENT, fs);
    	Vertex v4 = Vertex.newVertexFromLibrary(4, 1, 
                BBType.FRAGMENT, fs);
        dg.addVertex(v1);
        dg.addVertex(v2);
        dg.addVertex(v3);
        dg.addVertex(v4);
        
        Edge e1 = new Edge(v1.getAP(1), v2.getAP(0), 
                BondType.SINGLE);
        Edge e2 = new Edge(v2.getAP(1), v3.getAP(0), 
                BondType.SINGLE);
        Edge e3 = new Edge(v1.getAP(0), v4.getAP(0), 
                BondType.SINGLE);
        dg.addEdge(e1);
        dg.addEdge(e2);
        dg.addEdge(e3);
        
        dg.addRing(v3, v4);
        
        //NB: this replaces unused RCVs with capping groups
        GraphConversionTool.replaceUnusedRCVsWithCapps(dg, fs);
        
        assertEquals(4, dg.getVertexCount(), "Number of vertexes after "
                + "removal of 0 unused RCVs.");
        assertEquals(3, dg.getEdgeCount(), "Number of edges after "
                + "removal of 0 unused RCVs.");
    	
        DGraph acyclicGraph = new DGraph();
        Vertex v1b = Vertex.newVertexFromLibrary(1, 0, 
                BBType.SCAFFOLD, fs);
        Vertex v2b = Vertex.newVertexFromLibrary(2, 0, 
                BBType.FRAGMENT, fs);
        Vertex v3b = Vertex.newVertexFromLibrary(3, 2, 
                BBType.FRAGMENT, fs);
        Vertex v4b = Vertex.newVertexFromLibrary(4, 1, 
                BBType.FRAGMENT, fs);
        acyclicGraph.addVertex(v1b);
        acyclicGraph.addVertex(v2b);
        acyclicGraph.addVertex(v3b);
        acyclicGraph.addVertex(v4b);
        
        Edge e1b = new Edge(v1b.getAP(1), v2b.getAP(0), 
                BondType.SINGLE);
        Edge e2b = new Edge(v2b.getAP(1), v3b.getAP(0), 
                BondType.SINGLE);
        Edge e3b = new Edge(v1b.getAP(0), v4b.getAP(0), 
                BondType.SINGLE);
        acyclicGraph.addEdge(e1b);
        acyclicGraph.addEdge(e2b);
        acyclicGraph.addEdge(e3b);
    	
        //NB: this replaces unused RCVs with capping groups
        GraphConversionTool.replaceUnusedRCVsWithCapps(acyclicGraph, fs);
       
        assertEquals(0, acyclicGraph.getRCVertices().size(), "Number of RCVs after "
                + "removal of 2 unused RCVs.");
        assertEquals(3, acyclicGraph.getVertexCount(), "Number of vertexes after "
                + "removal of 2 unused RCVs.");
        assertEquals(2, acyclicGraph.getEdgeCount(), "Number of edges after "
                + "removal of 2 unused RCVs.");
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetJGraphKernelFromGraph() throws Exception
    {
        PopulationTest.prepare();
        DGraph[] pair = PopulationTest.getPairOfTestGraphsB();
        DGraph gA = pair[0];
        DGraph gB = pair[1];
        DGraph gC = ((Template) gA.getVertexAtPosition(1))
                .getInnerGraph();
        DGraph gD = ((Template) gB.getVertexAtPosition(1))
                .getInnerGraph();
        
        DefaultUndirectedGraph<Node, NodeConnection> gkA = 
                GraphConversionTool.getJGraphKernelFromGraph(gA);
        assertEquals(5,gkA.vertexSet().size());
        assertEquals(4,gkA.edgeSet().size());
        
        DefaultUndirectedGraph<Node, NodeConnection> gkB = 
                GraphConversionTool.getJGraphKernelFromGraph(gB);
        assertEquals(4,gkB.vertexSet().size());
        assertEquals(3,gkB.edgeSet().size());
        
        DefaultUndirectedGraph<Node, NodeConnection> gkC = 
                GraphConversionTool.getJGraphKernelFromGraph(gC);
        assertEquals(8,gkC.vertexSet().size());
        assertEquals(8,gkC.edgeSet().size());
        
        DefaultUndirectedGraph<Node, NodeConnection> gkD = 
                GraphConversionTool.getJGraphKernelFromGraph(gD);
        assertEquals(8,gkD.vertexSet().size());
        assertEquals(7,gkD.edgeSet().size());
        
    }
    
//------------------------------------------------------------------------------

}
