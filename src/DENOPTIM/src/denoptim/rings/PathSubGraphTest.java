package denoptim.rings;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;

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
import denoptim.graph.DENOPTIMEdge;
import denoptim.graph.DENOPTIMEdge.BondType;
import denoptim.graph.DENOPTIMFragment;
import denoptim.graph.DENOPTIMGraph;
import denoptim.graph.DENOPTIMVertex.BBType;
import denoptim.threedim.ThreeDimTreeBuilder;

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
    public static DENOPTIMGraph makeTestGraphA() throws DENOPTIMException 
    {
        APClass apc = APClass.make("A",0);
        HashMap<String, BondType> map = new HashMap<>();
        map.put(apc.getRule(),BondType.SINGLE);
        FragmentSpace.setBondOrderMap(map);
        
        DENOPTIMGraph graph = new DENOPTIMGraph();

        IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();

        IAtomContainer iacA = builder.newAtomContainer();
        IAtom iaA1 = new Atom("O",new Point3d(0,0,0));
        IAtom iaA2 = new Atom("O",new Point3d(1,0,0));
        iacA.addAtom(iaA1);
        iacA.addAtom(iaA2);
        iacA.addBond(new Bond(iaA1, iaA2, IBond.Order.SINGLE));
        DENOPTIMFragment vA = new DENOPTIMFragment(0,iacA,BBType.FRAGMENT);
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
        DENOPTIMFragment vB = new DENOPTIMFragment(1,iacB,BBType.FRAGMENT);
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
        DENOPTIMFragment vC = new DENOPTIMFragment(2,iacC,BBType.FRAGMENT);
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
        DENOPTIMFragment vG = new DENOPTIMFragment(5,iacG,BBType.FRAGMENT);
        vG.addAP(0,new Point3d(1,0,0),apc);
        
    
        IAtomContainer iacD = builder.newAtomContainer();
        iacD.addAtom(new PseudoAtom("ATN",new Point3d(0,0,0)));
        DENOPTIMFragment vD = new DENOPTIMFragment(3,iacD,BBType.FRAGMENT);
        vD.addAP(0,new Point3d(-1,0,0),APClass.make("ATneutral",0));
        vD.setAsRCV(true);
        
        IAtomContainer iacE = builder.newAtomContainer();
        iacE.addAtom(new PseudoAtom("ATN",new Point3d(0,0,0)));
        DENOPTIMFragment vE = new DENOPTIMFragment(4,iacE,BBType.FRAGMENT);
        vE.addAP(0, new Point3d(-1,0,0),APClass.make("ATneutral",0));
        vE.setAsRCV(true);
    
        graph.addVertex(vA);
        graph.addVertex(vD);
        graph.addVertex(vB);
        graph.addVertex(vC);
        graph.addVertex(vG);
        graph.addVertex(vE);
        graph.addEdge(new DENOPTIMEdge(vA.getAP(0), vD.getAP(0), BondType.SINGLE));
        graph.addEdge(new DENOPTIMEdge(vA.getAP(1), vB.getAP(0), BondType.SINGLE));
        graph.addEdge(new DENOPTIMEdge(vB.getAP(1), vC.getAP(0), BondType.SINGLE));
        graph.addEdge(new DENOPTIMEdge(vC.getAP(2), vG.getAP(0), BondType.SINGLE));
        graph.addEdge(new DENOPTIMEdge(vC.getAP(1), vE.getAP(0), BondType.SINGLE));
       
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
    public static DENOPTIMGraph makeTestGraphB() throws DENOPTIMException 
    {
        APClass apc = APClass.make("A",0);
        HashMap<String, BondType> map = new HashMap<>();
        map.put(apc.getRule(),BondType.SINGLE);
        FragmentSpace.setBondOrderMap(map);
        
        DENOPTIMGraph graph = new DENOPTIMGraph();

        IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();

        IAtomContainer iacA = builder.newAtomContainer();
        IAtom iaA1 = new Atom("O",new Point3d(0,0,0));
        iacA.addAtom(iaA1);
        DENOPTIMFragment vA = new DENOPTIMFragment(0,iacA,BBType.FRAGMENT);
        vA.addAP(0,new Point3d(0,-1,0),apc);
        vA.addAP(0,new Point3d(1,-1,0),apc);
        
        IAtomContainer iacB = builder.newAtomContainer();
        IAtom iaB1 = new Atom("C",new Point3d(0,0,0));
        iacB.addAtom(iaB1);
        DENOPTIMFragment vB = new DENOPTIMFragment(1,iacB,BBType.FRAGMENT);
        vB.addAP(0,new Point3d(-1,0,0),apc);
        vB.addAP(0,new Point3d(-1,-1,0),apc);
        
        IAtomContainer iacC = builder.newAtomContainer();
        IAtom iaC1 = new Atom("N",new Point3d(0,0,0));
        iacC.addAtom(iaC1);
        DENOPTIMFragment vC = new DENOPTIMFragment(2,iacC,BBType.FRAGMENT);
        vC.addAP(0,new Point3d(1,0,0),apc);
        vC.addAP(0,new Point3d(1,1,0),apc);
        
        IAtomContainer iacG = builder.newAtomContainer();
        IAtom iaG1 = new Atom("P",new Point3d(0,0,0));
        iacG.addAtom(iaG1);
        DENOPTIMFragment vG = new DENOPTIMFragment(5,iacG,BBType.FRAGMENT);
        vG.addAP(0,new Point3d(1,0,0),apc);
        vG.addAP(0,new Point3d(-1,1,0),apc);
    
        IAtomContainer iacD = builder.newAtomContainer();
        iacD.addAtom(new PseudoAtom("ATN",new Point3d(0,0,0)));
        DENOPTIMFragment vD = new DENOPTIMFragment(3,iacD,BBType.FRAGMENT);
        vD.addAP(0,new Point3d(-1,0,0),APClass.make("ATneutral",0));
        vD.setAsRCV(true);
        
        IAtomContainer iacE = builder.newAtomContainer();
        iacE.addAtom(new PseudoAtom("ATN",new Point3d(0,0,0)));
        DENOPTIMFragment vE = new DENOPTIMFragment(4,iacE,BBType.FRAGMENT);
        vE.addAP(0, new Point3d(-1,0,0),APClass.make("ATneutral",0));
        vE.setAsRCV(true);
    
        graph.addVertex(vA);
        graph.addVertex(vD);
        graph.addVertex(vB);
        graph.addVertex(vC);
        graph.addVertex(vG);
        graph.addVertex(vE);
        graph.addEdge(new DENOPTIMEdge(vA.getAP(0), vD.getAP(0), BondType.SINGLE));
        graph.addEdge(new DENOPTIMEdge(vA.getAP(1), vB.getAP(0), BondType.SINGLE));
        graph.addEdge(new DENOPTIMEdge(vB.getAP(1), vC.getAP(0), BondType.SINGLE));
        graph.addEdge(new DENOPTIMEdge(vC.getAP(1), vG.getAP(0), BondType.SINGLE));
        graph.addEdge(new DENOPTIMEdge(vG.getAP(1), vE.getAP(0), BondType.SINGLE));
        
        // Use this just to verify identify of the graph
        /*
        ArrayList<DENOPTIMFragment> frags = new ArrayList<>();
        frags.add(vA);
        frags.add(vB);
        frags.add(vC);
        frags.add(vD);
        frags.add(vE);
        DenoptimIO.writeFragmentSet("/tmp/frags.sdf", frags);
        System.out.println("WRITING TEST GRAPH B");
        DenoptimIO.writeGraphsToFile(new File("/tmp/test_graph_B"), 
                FileFormat.GRAPHSDF, 
                new ArrayList<DENOPTIMGraph>(Arrays.asList(graph)));
*/
        return graph;
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testMakePathSubGraph() throws Exception 
    {
        DENOPTIMGraph gA = makeTestGraphA();
        DENOPTIMGraph gB = makeTestGraphB();
        
        ThreeDimTreeBuilder t3d = new ThreeDimTreeBuilder();
        t3d.setAlidnBBsIn3D(false); //£D not needed
        IAtomContainer molA = t3d.convertGraphTo3DAtomContainer(gA,true);
        IAtomContainer molB = t3d.convertGraphTo3DAtomContainer(gB,true);
        
        PathSubGraph pA = new PathSubGraph(gA.getVertexAtPosition(1),
                gA.getVertexAtPosition(5),gA);
        pA.makeMolecularRepresentation(molA, false);
        PathSubGraph pB = new PathSubGraph(gB.getVertexAtPosition(1),
                gB.getVertexAtPosition(5),gB);
        pB.makeMolecularRepresentation(molB, false);
        
        IAtomContainer iacA = pA.getMolecularRepresentation();
        assertEquals(14,iacA.getAtomCount(), "Atom count in the path");
        
        IAtomContainer iacB = pB.getMolecularRepresentation();
        assertEquals(6,iacB.getAtomCount(), "Atom count in the path");
    }
	
//------------------------------------------------------------------------------

}
