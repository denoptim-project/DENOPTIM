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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.silent.Bond;

import denoptim.fragspace.FragmentSpace;
import denoptim.graph.APClass;
import denoptim.graph.DENOPTIMEdge;
import denoptim.graph.DENOPTIMEdge.BondType;
import denoptim.graph.DENOPTIMFragment;
import denoptim.graph.DENOPTIMGraph;
import denoptim.graph.DENOPTIMVertex;
import denoptim.graph.DENOPTIMVertex.BBType;

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
        
    	DENOPTIMFragment frg1 = new DENOPTIMFragment();
    	IAtom a1 = new Atom("C", new Point3d(new double[]{0.0, 0.0, 0.0}));
    	IAtom a2 = new Atom("C", new Point3d(new double[]{1.0, 0.0, 0.0}));
    	frg1.addAtom(a1);
    	frg1.addAtom(a2);
    	frg1.addBond(new Bond(a1, a2));
    	frg1.addAP(0, a0, new Point3d(new double[]{0.0, 0.0, 1.0}));
    	frg1.addAP(1, a0, new Point3d(new double[]{1.0, 1.0, 1.0}));
    	frg1.projectAPsToProperties(); 
    	
    	DENOPTIMFragment frg2 = new DENOPTIMFragment();
    	IAtom a3 = new Atom("C", new Point3d(new double[]{0.0, 0.0, 0.0}));
    	frg2.addAtom(a3);
    	frg2.addAP(0, a0, new Point3d(new double[]{0.0, 1.0, 1.0}));
    	frg2.addAP(0, b0, new Point3d(new double[]{0.0, 1.0, -1.0}));   
    	frg2.projectAPsToProperties(); 

    	DENOPTIMFragment rca1 = new DENOPTIMFragment();
    	IAtom a4 = new PseudoAtom("ATP", new Point3d(
    	        new double[]{0.0, 0.0, 0.0}));
    	rca1.addAtom(a4);
    	rca1.addAP(0, ap0, new Point3d(new double[]{0.0, 1.0, 1.0}));
    	rca1.projectAPsToProperties(); 
    	
    	DENOPTIMFragment rca2 = new DENOPTIMFragment();
    	IAtom a5 = new PseudoAtom("ATM", new Point3d(
    	        new double[]{1.0, 0.0, 0.0}));
    	rca2.addAtom(a5);
    	rca2.addAP(0, am0, new Point3d(new double[]{0.0, 1.0, 1.0}));
    	rca2.projectAPsToProperties(); 
    	
    	DENOPTIMFragment cap = new DENOPTIMFragment();
    	IAtom a6 = new Atom("H", new Point3d(new double[]{0.0, 0.0, 0.0}));
    	cap.addAtom(a6);
    	cap.addAP(0, h0, new Point3d(new double[]{0.0, 1.0, 1.0}));
    	cap.projectAPsToProperties(); 
    	
    	ArrayList<DENOPTIMVertex> scaff = new ArrayList<DENOPTIMVertex>();
    	scaff.add(frg1);
    	ArrayList<DENOPTIMVertex> frags = new ArrayList<DENOPTIMVertex>();
    	frags.add(frg2);
    	frags.add(rca1);
    	frags.add(rca2);
    	ArrayList<DENOPTIMVertex> caps = new ArrayList<DENOPTIMVertex>();
    	caps.add(cap);
    	
    	FragmentSpace.defineFragmentSpace(scaff,frags,caps,cpMap,capMap,
    			forbEnds,null);
    	
    	DENOPTIMGraph dg = new DENOPTIMGraph();
    	DENOPTIMVertex v1 = DENOPTIMVertex.newVertexFromLibrary(1, 0, 
    	        BBType.SCAFFOLD);
    	DENOPTIMVertex v2 = DENOPTIMVertex.newVertexFromLibrary(2, 0, 
                BBType.FRAGMENT);
    	DENOPTIMVertex v3 = DENOPTIMVertex.newVertexFromLibrary(3, 2, 
                BBType.FRAGMENT);
    	DENOPTIMVertex v4 = DENOPTIMVertex.newVertexFromLibrary(4, 1, 
                BBType.FRAGMENT);
        dg.addVertex(v1);
        dg.addVertex(v2);
        dg.addVertex(v3);
        dg.addVertex(v4);
        
        DENOPTIMEdge e1 = new DENOPTIMEdge(v1.getAP(1), v2.getAP(0), 
                BondType.SINGLE);
        DENOPTIMEdge e2 = new DENOPTIMEdge(v2.getAP(1), v3.getAP(0), 
                BondType.SINGLE);
        DENOPTIMEdge e3 = new DENOPTIMEdge(v1.getAP(0), v4.getAP(0), 
                BondType.SINGLE);
        dg.addEdge(e1);
        dg.addEdge(e2);
        dg.addEdge(e3);
        
        dg.addRing(v3, v4);
        
        //NB: this replaces unused RCVs with capping groups
        GraphConversionTool.replaceUnusedRCVsWithCapps(dg);
        
        assertEquals(4, dg.getVertexCount(), "Number of vertexes after "
                + "removal of 0 unused RCVs.");
        assertEquals(3, dg.getEdgeCount(), "Number of edges after "
                + "removal of 0 unused RCVs.");
    	
        DENOPTIMGraph acyclicGraph = new DENOPTIMGraph();
        DENOPTIMVertex v1b = DENOPTIMVertex.newVertexFromLibrary(1, 0, 
                BBType.SCAFFOLD);
        DENOPTIMVertex v2b = DENOPTIMVertex.newVertexFromLibrary(2, 0, 
                BBType.FRAGMENT);
        DENOPTIMVertex v3b = DENOPTIMVertex.newVertexFromLibrary(3, 2, 
                BBType.FRAGMENT);
        DENOPTIMVertex v4b = DENOPTIMVertex.newVertexFromLibrary(4, 1, 
                BBType.FRAGMENT);
        acyclicGraph.addVertex(v1b);
        acyclicGraph.addVertex(v2b);
        acyclicGraph.addVertex(v3b);
        acyclicGraph.addVertex(v4b);
        
        DENOPTIMEdge e1b = new DENOPTIMEdge(v1b.getAP(1), v2b.getAP(0), 
                BondType.SINGLE);
        DENOPTIMEdge e2b = new DENOPTIMEdge(v2b.getAP(1), v3b.getAP(0), 
                BondType.SINGLE);
        DENOPTIMEdge e3b = new DENOPTIMEdge(v1b.getAP(0), v4b.getAP(0), 
                BondType.SINGLE);
        acyclicGraph.addEdge(e1b);
        acyclicGraph.addEdge(e2b);
        acyclicGraph.addEdge(e3b);
    	
        //NB: this replaces unused RCVs with capping groups
        GraphConversionTool.replaceUnusedRCVsWithCapps(acyclicGraph);
       
        assertEquals(0, acyclicGraph.getRCVertices().size(), "Number of RCVs after "
                + "removal of 2 unused RCVs.");
        assertEquals(3, acyclicGraph.getVertexCount(), "Number of vertexes after "
                + "removal of 2 unused RCVs.");
        assertEquals(2, acyclicGraph.getEdgeCount(), "Number of edges after "
                + "removal of 2 unused RCVs.");
    }
    
//------------------------------------------------------------------------------

}
