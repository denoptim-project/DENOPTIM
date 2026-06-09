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

package denoptim.graph.rings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.Bond;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.graph.APClass;
import denoptim.graph.DGraph;
import denoptim.graph.DGraphTest;
import denoptim.graph.Edge;
import denoptim.graph.Edge.BondType;
import denoptim.graph.EmptyVertex;
import denoptim.graph.Fragment;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;
import denoptim.molecularmodeling.ThreeDimTreeBuilder;
import denoptim.utils.Randomizer;

/**
 * Unit test for PathSubGraph
 * 
 * @author Marco Foscato
 */

public class PathSubGraphTest {
    
  //------------------------------------------------------------------------------
    /**
     * Build a graph meant to be used in unit tests. The returned graph has
     * the following structure:
     * <pre>
     *              C-C-C-C      N
     *              |     |     / \
     * RCV--[O-O]--[C--C--C]--[N---N]--RCV
     *        
     * </pre>
     * 
     * @return a new instance of the test graph.
     * @throws DENOPTIMException 
     */
    public static DGraph makeTestGraphA() throws DENOPTIMException 
    {
        APClass apc = APClass.make("A",0);
        
        DGraph graph = new DGraph();

        IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();

        IAtomContainer iacA = builder.newAtomContainer();
        IAtom iaA1 = new Atom("O",new Point3d(0,0,0));
        IAtom iaA2 = new Atom("O",new Point3d(1,0,0));
        iacA.addAtom(iaA1);
        iacA.addAtom(iaA2);
        iacA.addBond(new Bond(iaA1, iaA2, IBond.Order.SINGLE));
        Fragment vA = new Fragment(0,iacA,BBType.FRAGMENT);
        vA.addAP(0,new Point3d(0,-1,0),apc);
        vA.addAP(1,new Point3d(2,0,0),apc);
        
        IAtomContainer iacB = builder.newAtomContainer();
        IAtom iaB1 = new Atom("C",new Point3d(0,0,0));
        IAtom iaB2 = new Atom("C",new Point3d(1,0,0));
        IAtom iaB3 = new Atom("C",new Point3d(1,-0.33,0));
        IAtom iaB4 = new Atom("C",new Point3d(1,-0.66,0));
        IAtom iaB5 = new Atom("C",new Point3d(1,-1,0));
        IAtom iaB6 = new Atom("C",new Point3d(0,-1,0));
        IAtom iaB7 = new Atom("C",new Point3d(0,-0.5,0));
        iacB.addAtom(iaB1);
        iacB.addAtom(iaB2);
        iacB.addAtom(iaB3);
        iacB.addAtom(iaB4);
        iacB.addAtom(iaB5);
        iacB.addAtom(iaB6);
        iacB.addAtom(iaB7);
        iacB.addBond(new Bond(iaB1, iaB2, IBond.Order.SINGLE));
        iacB.addBond(new Bond(iaB2, iaB3, IBond.Order.SINGLE));
        iacB.addBond(new Bond(iaB3, iaB4, IBond.Order.SINGLE));
        iacB.addBond(new Bond(iaB4, iaB5, IBond.Order.SINGLE));
        iacB.addBond(new Bond(iaB5, iaB6, IBond.Order.SINGLE));
        iacB.addBond(new Bond(iaB6, iaB7, IBond.Order.SINGLE));
        iacB.addBond(new Bond(iaB1, iaB7, IBond.Order.SINGLE));
        Fragment vB = new Fragment(1,iacB,BBType.FRAGMENT);
        vB.addAP(0,new Point3d(-1,0,0),apc);
        vB.addAP(5,new Point3d(-1,-1,0),apc);
        vB.addAP(6,new Point3d(1,1,0),apc);
        
        IAtomContainer iacC = builder.newAtomContainer();
        IAtom iaC1 = new Atom("N",new Point3d(0,0,0));
        IAtom iaC2 = new Atom("N",new Point3d(-1,0,0));
        IAtom iaC3 = new Atom("N",new Point3d(0,-1,0));
        iacC.addAtom(iaC1);
        iacC.addAtom(iaC2);
        iacC.addAtom(iaC3);
        iacC.addBond(new Bond(iaC1, iaC2, IBond.Order.SINGLE));
        iacC.addBond(new Bond(iaC2, iaC3, IBond.Order.SINGLE));
        iacC.addBond(new Bond(iaC1, iaC3, IBond.Order.SINGLE));
        Fragment vC = new Fragment(2,iacC,BBType.FRAGMENT);
        vC.addAP(0,new Point3d(1,0,0),apc);
        vC.addAP(1,new Point3d(-1,1,0),apc);
        vC.addAP(2,new Point3d(-1,-1,0),apc);
        
        IAtomContainer iacG = builder.newAtomContainer();
        IAtom iaG1 = new Atom("P",new Point3d(0,0,0));
        IAtom iaG2 = new Atom("P",new Point3d(0,0,1));
        IAtom iaG3 = new Atom("P",new Point3d(0,0,2));
        iacG.addAtom(iaG1);
        iacG.addAtom(iaG2);
        iacG.addAtom(iaG3);
        iacG.addBond(new Bond(iaG1, iaG2, IBond.Order.SINGLE));
        iacG.addBond(new Bond(iaG2, iaG3, IBond.Order.SINGLE));
        Fragment vG = new Fragment(5,iacG,BBType.FRAGMENT);
        vG.addAP(0,new Point3d(1,0,0),apc);
        
        IAtomContainer iacD = builder.newAtomContainer();
        iacD.addAtom(new PseudoAtom("ATN",new Point3d(0,0,0)));
        Fragment vD = new Fragment(3,iacD,BBType.FRAGMENT);
        vD.addAP(0,new Point3d(-1,0,0),APClass.make("ATneutral",0));
        vD.setAsRCV(true);
        
        IAtomContainer iacE = builder.newAtomContainer();
        iacE.addAtom(new PseudoAtom("ATN",new Point3d(0,0,0)));
        Fragment vE = new Fragment(4,iacE,BBType.FRAGMENT);
        vE.addAP(0, new Point3d(-1,0,0),APClass.make("ATneutral",0));
        vE.setAsRCV(true);
    
        graph.addVertex(vA);
        graph.addVertex(vD);
        graph.addVertex(vB);
        graph.addVertex(vC);
        graph.addVertex(vG);
        graph.addVertex(vE);
        graph.addEdge(new Edge(vA.getAP(0), vD.getAP(0), BondType.SINGLE));
        graph.addEdge(new Edge(vA.getAP(1), vB.getAP(0), BondType.SINGLE));
        graph.addEdge(new Edge(vB.getAP(1), vC.getAP(0), BondType.SINGLE));
        graph.addEdge(new Edge(vC.getAP(2), vG.getAP(0), BondType.SINGLE));
        graph.addEdge(new Edge(vC.getAP(1), vE.getAP(0), BondType.SINGLE));
       
        return graph;
    }
    
//------------------------------------------------------------------------------

    /**
     * Build a graph meant to be used in unit tests. The returned graph has
     * the following structure:
     * <pre>
     * RCV--[O]--[C]--[N]--RCV
     *        
     * </pre>
     * 
     * @return a new instance of the test graph.
     * @throws DENOPTIMException 
     */
    public static DGraph makeTestGraphB() throws DENOPTIMException 
    {
        APClass apc = APClass.make("A",0,BondType.SINGLE);
        
        DGraph graph = new DGraph();

        IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();

        IAtomContainer iacA = builder.newAtomContainer();
        IAtom iaA1 = new Atom("O",new Point3d(0,0,0));
        iacA.addAtom(iaA1);
        Fragment vA = new Fragment(0,iacA,BBType.FRAGMENT);
        vA.addAP(0,new Point3d(0,-1,0),apc);
        vA.addAP(0,new Point3d(1,-1,0),apc);
        
        IAtomContainer iacB = builder.newAtomContainer();
        IAtom iaB1 = new Atom("C",new Point3d(0,0,0));
        iacB.addAtom(iaB1);
        Fragment vB = new Fragment(1,iacB,BBType.FRAGMENT);
        vB.addAP(0,new Point3d(-1,0,0),apc);
        vB.addAP(0,new Point3d(-1,-1,0),apc);
        
        IAtomContainer iacC = builder.newAtomContainer();
        IAtom iaC1 = new Atom("N",new Point3d(0,0,0));
        iacC.addAtom(iaC1);
        Fragment vC = new Fragment(2,iacC,BBType.FRAGMENT);
        vC.addAP(0,new Point3d(1,0,0),apc);
        vC.addAP(0,new Point3d(1,1,0),apc);
        
        IAtomContainer iacG = builder.newAtomContainer();
        IAtom iaG1 = new Atom("P",new Point3d(0,0,0));
        iacG.addAtom(iaG1);
        Fragment vG = new Fragment(5,iacG,BBType.FRAGMENT);
        vG.addAP(0,new Point3d(1,0,0),apc);
        vG.addAP(0,new Point3d(-1,1,0),apc);
    
        IAtomContainer iacD = builder.newAtomContainer();
        iacD.addAtom(new PseudoAtom("ATN",new Point3d(0,0,0)));
        Fragment vD = new Fragment(3,iacD,BBType.FRAGMENT);
        vD.addAP(0,new Point3d(-1,0,0),APClass.make("ATneutral",0));
        vD.setAsRCV(true);
        
        IAtomContainer iacE = builder.newAtomContainer();
        iacE.addAtom(new PseudoAtom("ATN",new Point3d(0,0,0)));
        Fragment vE = new Fragment(4,iacE,BBType.FRAGMENT);
        vE.addAP(0, new Point3d(-1,0,0),APClass.make("ATneutral",0));
        vE.setAsRCV(true);
    
        graph.addVertex(vA);
        graph.addVertex(vD);
        graph.addVertex(vB);
        graph.addVertex(vC);
        graph.addVertex(vG);
        graph.addVertex(vE);
        graph.addEdge(new Edge(vA.getAP(0), vD.getAP(0), BondType.SINGLE));
        graph.addEdge(new Edge(vA.getAP(1), vB.getAP(0), BondType.SINGLE));
        graph.addEdge(new Edge(vB.getAP(1), vC.getAP(0), BondType.SINGLE));
        graph.addEdge(new Edge(vC.getAP(1), vG.getAP(0), BondType.SINGLE));
        graph.addEdge(new Edge(vG.getAP(1), vE.getAP(0), BondType.SINGLE));
        
        return graph;
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testMakePathSubGraph_sameVrtx() throws Exception 
    {
        DGraph g = makeTestGraphA();
        
        PathSubGraph path = new PathSubGraph(g.getVertexAtPosition(5),
                g.getVertexAtPosition(5));

        assertEquals(1,path.getVertecesPath().size(), 
            "Number of vertices in the path");
        assertEquals(g.getVertexAtPosition(5),path.getVertecesPath().get(0),
            "First vertex in the path is the fifth vertex in the graph.");
    }

 //------------------------------------------------------------------------------
    
    @Test
    public void testMakePathSubGraph_sameBranch() throws Exception 
    {
        DGraph g = makeTestGraphA();
        
        PathSubGraph path = new PathSubGraph(g.getVertexAtPosition(2),
            g.getVertexAtPosition(3));

        assertEquals(2,path.getVertecesPath().size(), 
            "Number of vertices in the path");
        assertEquals(1,path.getEdgesPath().size(), 
            "Number of edges in the path");
        assertEquals(g.getVertexAtPosition(2),path.getVertecesPath().get(0),
            "First vertex in the path");
        assertEquals(g.getVertexAtPosition(3),path.getVertecesPath().get(1),
            "Second vertex in the path");

        path = new PathSubGraph(g.getVertexAtPosition(5),
            g.getVertexAtPosition(2));

        assertEquals(3,path.getVertecesPath().size(), 
             "Number of vertices in the path");
        assertEquals(2,path.getEdgesPath().size(), 
                 "Number of edges in the path");
        assertEquals(g.getVertexAtPosition(5),path.getVertecesPath().get(0),
            "First vertex in the path");
        assertEquals(g.getVertexAtPosition(3),path.getVertecesPath().get(1),
            "Second vertex in the path");
        assertEquals(g.getVertexAtPosition(2),path.getVertecesPath().get(2),
            "Third vertex in the path");
    }
    
    
//------------------------------------------------------------------------------
    
    @Test
    public void testMakePathSubGraph_differentBranch() throws Exception 
    {
        FragmentSpace fs = DGraphTest.prepare();
        DGraph g = DGraphTest.makeTestGraphO_B(fs);
        
        PathSubGraph path = new PathSubGraph(g.getVertexAtPosition(7),
                g.getVertexAtPosition(8));

        assertEquals(5,path.getVertecesPath().size(), 
            "Number of vertices in the path");
    }

//------------------------------------------------------------------------------
    
    @Test
    public void testMakePathSubGraph_givenAdjacency() throws Exception 
    {
        DGraph g = new DGraph();
        EmptyVertex v1 = new EmptyVertex(1);
        v1.addAP(APClass.make("A",0));
        v1.addAP(APClass.make("A",1));
        EmptyVertex v2 = new EmptyVertex(2);
        v2.addAP(APClass.make("B",0));
        v2.addAP(APClass.make("B",1));
        EmptyVertex v3 = new EmptyVertex(3);
        v3.addAP(APClass.make("C",0));
        v3.addAP(APClass.make("C",1));
        EmptyVertex v6 = new EmptyVertex(6);
        v6.addAP(APClass.make("Z",0));
        v6.addAP(APClass.make("X",1));
        g.addVertex(new EmptyVertex(0));
        g.addVertex(v1);
        g.addVertex(v2);
        g.addVertex(v3);
        g.addVertex(new EmptyVertex(4));
        g.addVertex(new EmptyVertex(5));
        g.addVertex(v6);
        
        // Same vertex
        PathSubGraph path = new PathSubGraph(g.getVertexAtPosition(0),
                g.getVertexAtPosition(0));

        assertEquals(1,path.getVertecesPath().size(), 
            "Number of vertices in the path");
        assertEquals(0,path.getEdgesPath().size(), 
                    "Number of edges in the path");
        assertEquals(g.getVertexAtPosition(0),path.getVertecesPath().get(0),
            "Vertex in path");
                
        // Non-connected vertexes
        assertThrows(DENOPTIMException.class, () -> new PathSubGraph(
            g.getVertexAtPosition(0), g.getVertexAtPosition(1)));
        assertThrows(DENOPTIMException.class, () -> new PathSubGraph(
            g.getVertexAtPosition(1), g.getVertexAtPosition(0)));

        // Non-connected with irrelevant jump rules
        Map<Vertex, List<Vertex>> adjacency = new HashMap<>();
        adjacency.put(g.getVertexAtPosition(3), Arrays.asList(
            g.getVertexAtPosition(1),g.getVertexAtPosition(4)));
            
        assertThrows(DENOPTIMException.class, () -> new PathSubGraph(
            g.getVertexAtPosition(0), g.getVertexAtPosition(1),adjacency));
        assertThrows(DENOPTIMException.class, () -> new PathSubGraph(
            g.getVertexAtPosition(1), g.getVertexAtPosition(0),adjacency));

        // Non-connected with adjacency rule
        Map<Vertex, List<Vertex>> adjacency2 = new HashMap<>();
        adjacency2.computeIfAbsent(g.getVertexAtPosition(0), k -> new ArrayList<Vertex>())
        .add(g.getVertexAtPosition(1));
        adjacency2.computeIfAbsent(g.getVertexAtPosition(1), k -> new ArrayList<Vertex>())
        .add(g.getVertexAtPosition(0));
        adjacency2.computeIfAbsent(g.getVertexAtPosition(1), k -> new ArrayList<Vertex>())
        .add(g.getVertexAtPosition(4));
        adjacency2.computeIfAbsent(g.getVertexAtPosition(4), k -> new ArrayList<Vertex>())
        .add(g.getVertexAtPosition(1));

        path = new PathSubGraph(g.getVertexAtPosition(0),
                g.getVertexAtPosition(4), adjacency2);

        assertEquals(3,path.getVertecesPath().size(), 
            "Number of vertices in the path");
        assertEquals(2,path.getEdgesPath().size(), 
                    "Number of edges in the path");
        for (Edge e : path.getEdgesPath()) {
            assertNull(e, "Edge is supposed to be null in path driven only by adjacency rule");
        }

        // Mixing adjacency and actual edges
        g.addEdge(new Edge(v1.getAP(0), v2.getAP(1), BondType.SINGLE));
        g.addEdge(new Edge(v2.getAP(0), v3.getAP(0), BondType.TRIPLE));
        g.addEdge(new Edge(v1.getAP(1), v6.getAP(1), BondType.DOUBLE));

        // edges are present, but we require not to use them
        assertThrows(DENOPTIMException.class, () -> new PathSubGraph(
            g.getVertexAtPosition(4), v3, adjacency2, false));

        // now we do require to use them
        path = new PathSubGraph(g.getVertexAtPosition(4), v3, adjacency2, true);

        assertEquals(4,path.getVertecesPath().size(), 
            "Number of vertices in the path");
        assertEquals(3,path.getEdgesPath().size(), 
                    "Number of edges in the path");
        List<Edge> edges = path.getEdgesPath();
        assertNull(edges.get(0), "Edge is supposed to be null in path driven only by adjacency rule");
        assertNotNull(edges.get(1), "Edge is supposed to be not null in path driven by adjacency and actual edges");
        assertNotNull(edges.get(2), "Edge is supposed to be not null in path driven by adjacency and actual edges");
        assertEquals(BondType.SINGLE, edges.get(1).getBondType(), "Bond type in the path");
        assertEquals(BondType.TRIPLE, edges.get(2).getBondType(), "Bond type in the path");
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testMakePathSubGraph_withIAC() throws Exception 
    {
        DGraph gA = makeTestGraphA();
        DGraph gB = makeTestGraphB();
        
        Logger logger = Logger.getLogger("DummyLogger");
        Randomizer rng = new Randomizer();
        
        ThreeDimTreeBuilder t3d = new ThreeDimTreeBuilder(logger,rng);
        t3d.setAlignBBsIn3D(false); //3D not needed
        IAtomContainer molA = t3d.convertGraphTo3DAtomContainer(gA,true);
        IAtomContainer molB = t3d.convertGraphTo3DAtomContainer(gB,true);
        
        PathSubGraph pA = new PathSubGraph(gA.getVertexAtPosition(1),
                gA.getVertexAtPosition(5));
        pA.makeMolecularRepresentation(molA, false, logger, rng);
        PathSubGraph pB = new PathSubGraph(gB.getVertexAtPosition(1),
                gB.getVertexAtPosition(5));
        pB.makeMolecularRepresentation(molB, false, logger, rng);
        
        IAtomContainer iacA = pA.getMolecularRepresentation();
        assertEquals(14,iacA.getAtomCount(), "Atom count in the path");
        
        IAtomContainer iacB = pB.getMolecularRepresentation();
        assertEquals(6,iacB.getAtomCount(), "Atom count in the path");
    }
	
//------------------------------------------------------------------------------

}
