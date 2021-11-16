package denoptim.molecule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.Bond;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.io.DenoptimIO;
import denoptim.molecule.DENOPTIMEdge.BondType;
import denoptim.molecule.DENOPTIMTemplate.ContractLevel;
import denoptim.molecule.DENOPTIMVertex.BBType;


/**
 * Unit test for DENOPTIMGraph
 * 
 * @author Marco Foscato
 */

public class DENOPTIMGraphTest {
    
    private final String APRULE = "MyRule";
    private final String APSUBRULE = "1";
    private final String APCLASS = APRULE
            + DENOPTIMConstants.SEPARATORAPPROPSCL + APSUBRULE;
    
    private static APClass APCA, APCB, APCC, APCD;
    private static String a="A", b="B", c="C", d="D";
    
//------------------------------------------------------------------------------
    
    @BeforeEach
    public void setBoMap()
    {
        //This is just to aboid printing the warning about unset bond order map
        HashMap<String,BondType> boMap = new HashMap<String,BondType>();
        boMap.put(APRULE,BondType.SINGLE);
        FragmentSpace.setBondOrderMap(boMap);
    }
    
//------------------------------------------------------------------------------
    
    private void prepareFragmentSpace() throws DENOPTIMException
    {
        APCA = APClass.make(a, 0);
        APCB = APClass.make(b, 0);
        APCC = APClass.make(c, 0);
        APCD = APClass.make(d, 99);
        
        HashMap<String,BondType> boMap = new HashMap<String,BondType>();
        boMap.put(a,BondType.SINGLE);
        boMap.put(b,BondType.SINGLE);
        boMap.put(c,BondType.SINGLE);
        boMap.put(d,BondType.DOUBLE);
        
        HashMap<APClass,ArrayList<APClass>> cpMap = 
                new HashMap<APClass,ArrayList<APClass>>();
        ArrayList<APClass> lstA = new ArrayList<APClass>();
        lstA.add(APCA);
        cpMap.put(APCA, lstA);
        ArrayList<APClass> lstB = new ArrayList<APClass>();
        lstB.add(APCB);
        lstB.add(APCC);
        cpMap.put(APCB, lstB);
        ArrayList<APClass> lstC = new ArrayList<APClass>();
        lstC.add(APCB);
        lstC.add(APCC);
        cpMap.put(APCC, lstC);
        ArrayList<APClass> lstD = new ArrayList<APClass>();
        lstD.add(APCD);
        cpMap.put(APCD, lstD);
        
        
        /* Compatibility matrix
         * 
         *      |  A  |  B  |  C  | D |
         *    -------------------------
         *    A |  T  |     |     |   |
         *    -------------------------
         *    B |     |  T  |  T  |   |
         *    -------------------------
         *    C |     |  T  |  T  |   |
         *    -------------------------
         *    D |     |     |     | T |
         */
        
        HashMap<APClass,APClass> capMap = new HashMap<APClass,APClass>();
        HashSet<APClass> forbEnds = new HashSet<APClass>();
        
        FragmentSpace.setBondOrderMap(boMap);
        FragmentSpace.setCompatibilityMatrix(cpMap);
        FragmentSpace.setCappingMap(capMap);
        FragmentSpace.setForbiddenEndList(forbEnds);
        FragmentSpace.setAPclassBasedApproach(true);
        
        FragmentSpace.setScaffoldLibrary(new ArrayList<DENOPTIMVertex>());
        FragmentSpace.setFragmentLibrary(new ArrayList<DENOPTIMVertex>());
        
        EmptyVertex s0 = new EmptyVertex();
        s0.setBuildingBlockType(BBType.SCAFFOLD);
        s0.addAP(0,APCA);
        s0.addAP(0,APCA);
        FragmentSpace.appendVertexToLibrary(s0, BBType.SCAFFOLD,
                FragmentSpace.getScaffoldLibrary());
        
        EmptyVertex s1 = new EmptyVertex();
        s1.setBuildingBlockType(BBType.SCAFFOLD);
        s1.addAP(0,APCA);
        s1.addAP(0,APCA);
        s1.addAP(0,APCD);
        FragmentSpace.appendVertexToLibrary(s1, BBType.SCAFFOLD,
                FragmentSpace.getScaffoldLibrary());
        
        EmptyVertex v0 = new EmptyVertex();
        v0.setBuildingBlockType(BBType.FRAGMENT);
        v0.addAP(0,APCA);
        v0.addAP(0,APCB);
        v0.addAP(0,APCA);
        FragmentSpace.appendVertexToLibrary(v0, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        EmptyVertex v1 = new EmptyVertex();
        v1.setBuildingBlockType(BBType.FRAGMENT);
        v1.addAP(0,APCA);
        v1.addAP(0,APCB);
        v1.addAP(0,APCA);
        v1.addAP(0,APCB);
        v1.addAP(0,APCC);
        FragmentSpace.appendVertexToLibrary(v1, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        EmptyVertex v2 = new EmptyVertex();
        v2.setBuildingBlockType(BBType.FRAGMENT);
        v2.addAP(0,APCB);
        FragmentSpace.appendVertexToLibrary(v2, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        EmptyVertex v3 = new EmptyVertex();
        v3.setBuildingBlockType(BBType.FRAGMENT);
        v3.addAP(0,APCD);
        v3.addAP(0,APCD);
        FragmentSpace.appendVertexToLibrary(v3, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
       
        EmptyVertex v4 = new EmptyVertex();
        v4.setBuildingBlockType(BBType.FRAGMENT);
        v4.addAP(0,APCC);
        v4.addAP(0,APCB);
        v4.addAP(0,APCB);
        v4.addAP(0,APCA);
        v4.addAP(0,APCA);
        FragmentSpace.appendVertexToLibrary(v4, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        EmptyVertex v5 = new EmptyVertex();
        v5.setBuildingBlockType(BBType.FRAGMENT);
        v5.addAP(0,APCB);
        v5.addAP(0,APCD);
        FragmentSpace.appendVertexToLibrary(v5, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        EmptyVertex v6 = new EmptyVertex();
        v6.setBuildingBlockType(BBType.FRAGMENT);
        v6.addAP(0,APCC);
        v6.addAP(0,APCD);
        FragmentSpace.appendVertexToLibrary(v6, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
        EmptyVertex v7 = new EmptyVertex();
        v7.setBuildingBlockType(BBType.FRAGMENT);
        v7.setAsRCV(true);
        v7.addAP(0,APCB);
        FragmentSpace.appendVertexToLibrary(v7, BBType.FRAGMENT,
                FragmentSpace.getFragmentLibrary());
        
    }

//------------------------------------------------------------------------------

    /**
     *  Creates a test graph that looks like this: 
     * 
     *  <pre>
     *        (C)-(C)-v3
     *       /
     *      | (B)-(B)-v2
     *      |/
     *  (A)-v1-(B)-(B)-v2
     *       |
     *      (A)
     *       |
     *      (A)
     *    scaffold
     *      (A)
     *       |
     *      0(A)
     *       |
     * 2(A)-v1-1(B)-(B)-v2
     *      |\
     *      | 3(B)-(B)-v2
     *      \
     *       4(C)-(C)-v3
     *   </pre>
     *   
     */
    private DENOPTIMGraph makeTestGraphB() throws DENOPTIMException
    {
        DENOPTIMGraph graph = new DENOPTIMGraph();
        DENOPTIMVertex s = DENOPTIMVertex.newVertexFromLibrary(0,
                BBType.SCAFFOLD);
        graph.addVertex(s);
        DENOPTIMVertex v1a = DENOPTIMVertex.newVertexFromLibrary(1,
                BBType.FRAGMENT);
        graph.addVertex(v1a);
        DENOPTIMVertex v2a = DENOPTIMVertex.newVertexFromLibrary(2,
                BBType.FRAGMENT);
        graph.addVertex(v2a);
        DENOPTIMVertex v2a_bis = DENOPTIMVertex.newVertexFromLibrary(2,
                BBType.FRAGMENT);
        graph.addVertex(v2a_bis);
        DENOPTIMVertex v3a = DENOPTIMVertex.newVertexFromLibrary(3,
                BBType.FRAGMENT);
        graph.addVertex(v3a);
        DENOPTIMVertex v1b = DENOPTIMVertex.newVertexFromLibrary(1,
                BBType.FRAGMENT);
        graph.addVertex(v1b);
        DENOPTIMVertex v2b = DENOPTIMVertex.newVertexFromLibrary(2,
                BBType.FRAGMENT);
        graph.addVertex(v2b);
        DENOPTIMVertex v2b_bis = DENOPTIMVertex.newVertexFromLibrary(2,
                BBType.FRAGMENT);
        graph.addVertex(v2b_bis);
        DENOPTIMVertex v3b = DENOPTIMVertex.newVertexFromLibrary(3,
                BBType.FRAGMENT);
        graph.addVertex(v3b);
        graph.addEdge(new DENOPTIMEdge(s.getAP(0), v1a.getAP(0)));
        graph.addEdge(new DENOPTIMEdge(v1a.getAP(1), v2a.getAP(0)));
        graph.addEdge(new DENOPTIMEdge(v1a.getAP(3), v2a_bis.getAP(0)));
        graph.addEdge(new DENOPTIMEdge(v1a.getAP(4), v3a.getAP(0)));
        graph.addEdge(new DENOPTIMEdge(s.getAP(1), v1b.getAP(0)));
        graph.addEdge(new DENOPTIMEdge(v1b.getAP(1), v2b.getAP(0)));
        graph.addEdge(new DENOPTIMEdge(v1b.getAP(3), v2b_bis.getAP(0)));
        graph.addEdge(new DENOPTIMEdge(v1b.getAP(4), v3b.getAP(0)));
        
        ArrayList<Integer> symA = new ArrayList<Integer>();
        symA.add(v1a.getVertexId());
        symA.add(v1b.getVertexId());
        graph.addSymmetricSetOfVertices(new SymmetricSet(symA));
        
        ArrayList<Integer> symB = new ArrayList<Integer>();
        symB.add(v2a.getVertexId());
        symB.add(v2a_bis.getVertexId());
        symB.add(v2b.getVertexId());
        symB.add(v2b_bis.getVertexId());
        graph.addSymmetricSetOfVertices(new SymmetricSet(symB));
        
        graph.renumberGraphVertices();
        return graph;
    }
    
//------------------------------------------------------------------------------

    /**
     *  Creates a test graph that looks like this: 
     * 
     *  <pre>
     *      v1-(B)-(B)-v1-(B)-(B)-v1-(B)-(B)-v1
     *       |           \
     *      (A)           (A)-(A)-v0
     *       |
     *      (A)
     *    scaffold
     *      (A)
     *       |
     *      0(A)
     *       |
     *       v1-1(B)-(B)-v1-(B)-(B)-v1-(B)-(B)-v1
     *   </pre>
     *   
     */
    private DENOPTIMGraph makeTestGraphC() throws DENOPTIMException
    {
        DENOPTIMGraph graph = new DENOPTIMGraph();
        DENOPTIMVertex s = DENOPTIMVertex.newVertexFromLibrary(0,
                BBType.SCAFFOLD);
        graph.addVertex(s);
        DENOPTIMVertex v1a = DENOPTIMVertex.newVertexFromLibrary(1,
                BBType.FRAGMENT);
        graph.appendVertexOnAP(s.getAP(0), v1a.getAP(2));
        DENOPTIMVertex v2a = DENOPTIMVertex.newVertexFromLibrary(1,
                BBType.FRAGMENT);
        graph.appendVertexOnAP(v1a.getAP(3), v2a.getAP(1));
        DENOPTIMVertex v3a = DENOPTIMVertex.newVertexFromLibrary(1,
                BBType.FRAGMENT);
        graph.appendVertexOnAP(v2a.getAP(3), v3a.getAP(1));
        DENOPTIMVertex v4a = DENOPTIMVertex.newVertexFromLibrary(1,
                BBType.FRAGMENT);
        graph.appendVertexOnAP(v3a.getAP(3), v4a.getAP(1));
        DENOPTIMVertex v5a = DENOPTIMVertex.newVertexFromLibrary(0,
                BBType.FRAGMENT);
        graph.appendVertexOnAP(v2a.getAP(2), v5a.getAP(0));
        
        DENOPTIMVertex v1b = DENOPTIMVertex.newVertexFromLibrary(1,
                BBType.FRAGMENT);
        graph.appendVertexOnAP(s.getAP(1), v1b.getAP(2));
        DENOPTIMVertex v2b = DENOPTIMVertex.newVertexFromLibrary(1,
                BBType.FRAGMENT);
        graph.appendVertexOnAP(v1b.getAP(3), v2b.getAP(1));
        DENOPTIMVertex v3b = DENOPTIMVertex.newVertexFromLibrary(1,
                BBType.FRAGMENT);
        graph.appendVertexOnAP(v2b.getAP(3), v3b.getAP(1));
        DENOPTIMVertex v4b = DENOPTIMVertex.newVertexFromLibrary(1,
                BBType.FRAGMENT);
        graph.appendVertexOnAP(v3b.getAP(3), v4b.getAP(1));
        DENOPTIMVertex v5b = DENOPTIMVertex.newVertexFromLibrary(0,
                BBType.FRAGMENT);
        graph.appendVertexOnAP(v2b.getAP(2), v5b.getAP(0));
        
        ArrayList<Integer> sym1 = new ArrayList<Integer>();
        sym1.add(v1a.getVertexId());
        sym1.add(v1b.getVertexId());
        graph.addSymmetricSetOfVertices(new SymmetricSet(sym1));
        ArrayList<Integer> sym2 = new ArrayList<Integer>();
        sym2.add(v2a.getVertexId());
        sym2.add(v2b.getVertexId());
        graph.addSymmetricSetOfVertices(new SymmetricSet(sym2));
        ArrayList<Integer> sym3 = new ArrayList<Integer>();
        sym3.add(v3a.getVertexId());
        sym3.add(v3b.getVertexId());
        graph.addSymmetricSetOfVertices(new SymmetricSet(sym3));
        ArrayList<Integer> sym4 = new ArrayList<Integer>();
        sym4.add(v4a.getVertexId());
        sym4.add(v4b.getVertexId());
        graph.addSymmetricSetOfVertices(new SymmetricSet(sym4));
        
        graph.renumberGraphVertices();
        return graph;
    }
    
//------------------------------------------------------------------------------

    /**
     *  Creates a test graph that looks like this: 
     * 
     *  <pre>
     *        (C)-(C)-v6-(D)--(D)-v7
     *        /                    |
     *       /                   chord
     *      |                      |
     *      | (B)-(C)-v6-(D)--(D)-v7
     *      |/
     *  (A)-v1-(B)-(C)-v6-(D)--(D)-v7
     *       |                     |
     *      (A)                    |
     *       |                   chord
     *      (A)                    |
     *    scaffold(D)----------(D)-v7
     *      (A)
     *       |
     *      0(A)
     *       |
     *      v1
     *       |
     *      2(A)
     *       |
     *      0(A)
     *       |
     * 2(A)-v1-1(B)-(B)-v2
     *      |\
     *      | 3(B)-(B)-v2
     *      \
     *       4(C)-(C)-v3
     *   </pre>
     *   
     */
    private DENOPTIMGraph makeTestGraphD() throws DENOPTIMException
    {
        DENOPTIMGraph graph = new DENOPTIMGraph();
        DENOPTIMVertex s = DENOPTIMVertex.newVertexFromLibrary(1,
                BBType.SCAFFOLD);
        graph.addVertex(s);
        DENOPTIMVertex v1a = DENOPTIMVertex.newVertexFromLibrary(1,
                BBType.FRAGMENT);
        graph.addVertex(v1a);
        DENOPTIMVertex v6a = DENOPTIMVertex.newVertexFromLibrary(6,
                BBType.FRAGMENT);
        graph.addVertex(v6a);
        DENOPTIMVertex v6a_bis = DENOPTIMVertex.newVertexFromLibrary(6,
                BBType.FRAGMENT);
        graph.addVertex(v6a_bis);
        DENOPTIMVertex v6a_tris = DENOPTIMVertex.newVertexFromLibrary(6,
                BBType.FRAGMENT);
        graph.addVertex(v6a_tris);
        DENOPTIMVertex v7a = DENOPTIMVertex.newVertexFromLibrary(7,
                BBType.FRAGMENT);
        graph.addVertex(v7a);
        DENOPTIMVertex v7a_bis = DENOPTIMVertex.newVertexFromLibrary(7,
                BBType.FRAGMENT);
        graph.addVertex(v7a_bis);
        DENOPTIMVertex v7a_tris = DENOPTIMVertex.newVertexFromLibrary(7,
                BBType.FRAGMENT);
        graph.addVertex(v7a_tris);
        
        DENOPTIMVertex v7a_quat = DENOPTIMVertex.newVertexFromLibrary(7,
                BBType.FRAGMENT);
        graph.addVertex(v7a_quat);
        
        DENOPTIMVertex v1c = DENOPTIMVertex.newVertexFromLibrary(1,
                BBType.FRAGMENT);
        graph.addVertex(v1c);
        
        DENOPTIMVertex v1b = DENOPTIMVertex.newVertexFromLibrary(1,
                BBType.FRAGMENT);
        graph.addVertex(v1b);
        DENOPTIMVertex v2b = DENOPTIMVertex.newVertexFromLibrary(2,
                BBType.FRAGMENT);
        graph.addVertex(v2b);
        DENOPTIMVertex v2b_bis = DENOPTIMVertex.newVertexFromLibrary(2,
                BBType.FRAGMENT);
        graph.addVertex(v2b_bis);
        DENOPTIMVertex v3b = DENOPTIMVertex.newVertexFromLibrary(3,
                BBType.FRAGMENT);
        graph.addVertex(v3b);
        graph.addEdge(new DENOPTIMEdge(s.getAP(0), v1a.getAP(0)));
        graph.addEdge(new DENOPTIMEdge(v1a.getAP(1), v6a.getAP(0)));
        graph.addEdge(new DENOPTIMEdge(v1a.getAP(3), v6a_bis.getAP(0)));
        graph.addEdge(new DENOPTIMEdge(v1a.getAP(4), v6a_tris.getAP(0)));
        graph.addEdge(new DENOPTIMEdge(v6a.getAP(1), v7a.getAP(0)));
        graph.addEdge(new DENOPTIMEdge(v6a_bis.getAP(1), v7a_bis.getAP(0)));
        graph.addEdge(new DENOPTIMEdge(v6a_tris.getAP(1), v7a_tris.getAP(0)));
        graph.addEdge(new DENOPTIMEdge(s.getAP(2), v7a_quat.getAP(0)));
        graph.addEdge(new DENOPTIMEdge(s.getAP(1), v1c.getAP(0)));
        graph.addEdge(new DENOPTIMEdge(v1c.getAP(2), v1b.getAP(0)));
        graph.addEdge(new DENOPTIMEdge(v1b.getAP(1), v2b.getAP(0)));
        graph.addEdge(new DENOPTIMEdge(v1b.getAP(3), v2b_bis.getAP(0)));
        graph.addEdge(new DENOPTIMEdge(v1b.getAP(4), v3b.getAP(0)));
        
        graph.addRing(v7a, v7a_quat);
        graph.addRing(v7a_bis, v7a_tris);
        
        ArrayList<Integer> symA = new ArrayList<Integer>();
        symA.add(v1a.getVertexId());
        symA.add(v1c.getVertexId());
        graph.addSymmetricSetOfVertices(new SymmetricSet(symA));
        
        ArrayList<Integer> symB = new ArrayList<Integer>();
        symB.add(v2b.getVertexId());
        symB.add(v2b_bis.getVertexId());
        graph.addSymmetricSetOfVertices(new SymmetricSet(symB));
        
        graph.renumberGraphVertices();
        return graph;
    }
    
//------------------------------------------------------------------------------
    
    /**
     *  Creates a test graph that looks like this: 
     * 
     *  <pre>
     *        (C)-(C)-v6-(D)--(D)-v7
     *        /                    |
     *       /                   chord
     *      |                      |
     *      | (B)-(C)-v6-(D)--(D)-v7
     *      |/
     *  (A)-v1-(B)-(C)-v6-(D)--(D)-v7
 
     *   </pre>
     *   
     */
    private DENOPTIMGraph makeTestGraphDSub1() throws DENOPTIMException
    {
        DENOPTIMGraph graph = new DENOPTIMGraph();
        DENOPTIMVertex v1a = DENOPTIMVertex.newVertexFromLibrary(1,
                BBType.FRAGMENT);
        graph.addVertex(v1a);
        DENOPTIMVertex v6a = DENOPTIMVertex.newVertexFromLibrary(6,
                BBType.FRAGMENT);
        graph.addVertex(v6a);
        DENOPTIMVertex v6a_bis = DENOPTIMVertex.newVertexFromLibrary(6,
                BBType.FRAGMENT);
        graph.addVertex(v6a_bis);
        DENOPTIMVertex v6a_tris = DENOPTIMVertex.newVertexFromLibrary(6,
                BBType.FRAGMENT);
        graph.addVertex(v6a_tris);
        DENOPTIMVertex v7a = DENOPTIMVertex.newVertexFromLibrary(7,
                BBType.FRAGMENT);
        graph.addVertex(v7a);
        DENOPTIMVertex v7a_bis = DENOPTIMVertex.newVertexFromLibrary(7,
                BBType.FRAGMENT);
        graph.addVertex(v7a_bis);
        DENOPTIMVertex v7a_tris = DENOPTIMVertex.newVertexFromLibrary(7,
                BBType.FRAGMENT);
        graph.addVertex(v7a_tris);
        
        graph.addEdge(new DENOPTIMEdge(v1a.getAP(1), v6a.getAP(0)));
        graph.addEdge(new DENOPTIMEdge(v1a.getAP(3), v6a_bis.getAP(0)));
        graph.addEdge(new DENOPTIMEdge(v1a.getAP(4), v6a_tris.getAP(0)));
        graph.addEdge(new DENOPTIMEdge(v6a.getAP(1), v7a.getAP(0)));
        graph.addEdge(new DENOPTIMEdge(v6a_bis.getAP(1), v7a_bis.getAP(0)));
        graph.addEdge(new DENOPTIMEdge(v6a_tris.getAP(1), v7a_tris.getAP(0)));
        
        graph.addRing(v7a_bis, v7a_tris);
        
        graph.renumberGraphVertices();
        return graph;
    }
    
//------------------------------------------------------------------------------

    /**
     *  Creates a test graph that looks like this: 
     * 
     *  <pre>

     * 2(A)-v1-1(B)-(B)-v2
     *      |\
     *      | 3(B)-(B)-v2
     *      \
     *       4(C)-(C)-v3
     *   </pre>
     *   
     */
    private DENOPTIMGraph makeTestGraphDSub2() throws DENOPTIMException
    {
        DENOPTIMGraph graph = new DENOPTIMGraph();
        DENOPTIMVertex v1b = DENOPTIMVertex.newVertexFromLibrary(1,
                BBType.FRAGMENT);
        graph.addVertex(v1b);
        DENOPTIMVertex v2b = DENOPTIMVertex.newVertexFromLibrary(2,
                BBType.FRAGMENT);
        graph.addVertex(v2b);
        DENOPTIMVertex v2b_bis = DENOPTIMVertex.newVertexFromLibrary(2,
                BBType.FRAGMENT);
        graph.addVertex(v2b_bis);
        DENOPTIMVertex v3b = DENOPTIMVertex.newVertexFromLibrary(3,
                BBType.FRAGMENT);
        graph.addVertex(v3b);
        graph.addEdge(new DENOPTIMEdge(v1b.getAP(1), v2b.getAP(0)));
        graph.addEdge(new DENOPTIMEdge(v1b.getAP(3), v2b_bis.getAP(0)));
        graph.addEdge(new DENOPTIMEdge(v1b.getAP(4), v3b.getAP(0)));
        
        ArrayList<Integer> symB = new ArrayList<Integer>();
        symB.add(v2b.getVertexId());
        symB.add(v2b_bis.getVertexId());
        graph.addSymmetricSetOfVertices(new SymmetricSet(symB));
        
        graph.renumberGraphVertices();
        return graph;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Makes a graph with disordered list of vertexes, i.e., the first vertex is
     * not the source.
     * <pre>
     * 
     * v5-0-->0-v3-1-->1-v4-0-->0-v1-1-->0-v2
     * 
     * </pre>
     * @return a disordered graph
     * @throws DENOPTIMException
     */
    private DENOPTIMGraph makeDisorderedGraph() throws DENOPTIMException
    {
        DENOPTIMGraph graph = new DENOPTIMGraph();
        DENOPTIMVertex v1 = DENOPTIMVertex.newVertexFromLibrary(3,
                BBType.FRAGMENT);
        graph.addVertex(v1);
        DENOPTIMVertex v2 = DENOPTIMVertex.newVertexFromLibrary(3,
                BBType.FRAGMENT);
        graph.addVertex(v2);
        DENOPTIMVertex v3 = DENOPTIMVertex.newVertexFromLibrary(3,
                BBType.FRAGMENT);
        graph.addVertex(v3);
        DENOPTIMVertex v4 = DENOPTIMVertex.newVertexFromLibrary(3,
                BBType.FRAGMENT);
        graph.addVertex(v4);
        DENOPTIMVertex v5 = DENOPTIMVertex.newVertexFromLibrary(3,
                BBType.FRAGMENT);
        graph.addVertex(v5);
        
        graph.addEdge(new DENOPTIMEdge(v4.getAP(0), v1.getAP(0)));
        graph.addEdge(new DENOPTIMEdge(v3.getAP(1), v4.getAP(1)));
        graph.addEdge(new DENOPTIMEdge(v5.getAP(0), v3.getAP(0)));
        graph.addEdge(new DENOPTIMEdge(v1.getAP(1), v2.getAP(0)));
        return graph;
    }

//------------------------------------------------------------------------------
    
    @Test
    public void testGetSourceVertex() throws Exception
    {
        prepareFragmentSpace();
        DENOPTIMGraph g = makeDisorderedGraph();
        DENOPTIMVertex src = g.getSourceVertex();
        assertEquals(g.getVertexAtPosition(4),src,
                "Inconsistent source vertex");

        DENOPTIMGraph g2 = makeTestGraphC();
        DENOPTIMVertex src2 = g2.getSourceVertex();
        assertEquals(g2.getVertexAtPosition(0),src2,
                "Inconsistent source vertex");
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetLevel() throws Exception
    {
        prepareFragmentSpace();
        DENOPTIMGraph g = makeDisorderedGraph();
        int[] expected = new int[] {2,3,0,1,-1};
        for (int i=0; i<g.getVertexCount(); i++)
        {
            assertEquals(expected[i],g.getLevel(g.getVertexAtPosition(i)),
                    "Wrong level for vertex at position "+i);
        }
        
        DENOPTIMGraph g2 = makeTestGraphC();
        expected = new int[] {-1,0,1,2,3,2,0,1,2,3,2};
        for (int i=0; i<g2.getVertexCount(); i++)
        {
            assertEquals(expected[i],g2.getLevel(g2.getVertexAtPosition(i)),
                    "Bis: Wrong level for vertex at position "+i);
        }
    }   
   
//------------------------------------------------------------------------------
    
    @Test
    public void testReplaceVertex() throws Exception
    {
        prepareFragmentSpace();
        DENOPTIMGraph g = makeTestGraphB();
        
        DENOPTIMVertex v1 = g.getVertexAtPosition(1);
        
        Map<Integer,Integer> apMap = new HashMap<Integer,Integer>();
        apMap.put(0, 4); 
        apMap.put(1, 1);
        apMap.put(3, 2);
        apMap.put(4, 0);
        
        int chosenBBId = 4;
        BBType choosenBBTyp = BBType.FRAGMENT;
        
        boolean res = g.replaceVertex(v1, chosenBBId, choosenBBTyp, apMap);
        
        assertTrue(res,"ReplaceVertex return value.");
        assertFalse(g.containsVertex(v1),"v1 is still part of graph");
        int numVertexesWithGoodBBId = 0;
        int numEdgesWithS = 0;
        int numEdgesWith2 = 0;
        int numEdgesWith3 = 0;
        for (DENOPTIMVertex v : g.gVertices)
        {
            if (v.getBuildingBlockType() == choosenBBTyp 
                    && v.getBuildingBlockId() == chosenBBId)
            {
                numVertexesWithGoodBBId++;
                
                for (DENOPTIMAttachmentPoint ap : v.getAttachmentPoints())
                {
                    if (!ap.isAvailable())
                    {
                        DENOPTIMVertex nextVrtx = ap.getLinkedAP().getOwner();
                        if (nextVrtx.getBuildingBlockType() == BBType.SCAFFOLD)
                        {
                            numEdgesWithS++;
                        } else {
                            switch (nextVrtx.getBuildingBlockId())
                            {
                                case 2:
                                    numEdgesWith2++;
                                    break;
                                case 3:
                                    numEdgesWith3++;
                                    break;
                            }
                        }
                    }
                }
            }
        }
        assertEquals(2,numVertexesWithGoodBBId,"Number of new links.");
        assertEquals(2,numEdgesWithS,"Number of new edges with scaffold.");
        assertEquals(4,numEdgesWith2,"Number of new edges with v2a/b.");
        assertEquals(2,numEdgesWith3,"Number of new edges with v3a/b.");
        
        DENOPTIMGraph g2 = makeTestGraphB();
        
        DENOPTIMVertex v2 = g2.getVertexAtPosition(2);
        
        Map<Integer,Integer> apMap2 = new HashMap<Integer,Integer>();
        apMap2.put(0, 1);
        
        int chosenBBId2 = 5;
        BBType choosenBBTyp2 = BBType.FRAGMENT;
        
        boolean res2 = g2.replaceVertex(v2, chosenBBId2, choosenBBTyp2, apMap2);
        
        assertTrue(res2,"ReplaceVertex return value (2).");
        assertFalse(g2.containsVertex(v2),"v2 is still part of graph");
        int numVertexesWithGoodBBId2 = 0;
        int numEdgesWith1 = 0;
        for (DENOPTIMVertex v : g2.gVertices)
        {
            if (v.getBuildingBlockType() == choosenBBTyp2 
                    && v.getBuildingBlockId() == chosenBBId2)
            {
                numVertexesWithGoodBBId2++;
                
                for (DENOPTIMAttachmentPoint ap : v.getAttachmentPoints())
                {
                    if (!ap.isAvailable())
                    {
                        DENOPTIMVertex nextVrtx = ap.getLinkedAP().getOwner();
                        if (nextVrtx.getBuildingBlockId() == 1)
                            numEdgesWith1++;
                    }
                }
            }
        }
        assertEquals(4,numVertexesWithGoodBBId2,"Number of new links.");
        assertEquals(4,numEdgesWith1,"Number of new edges with scaffold.");
    }
    
//------------------------------------------------------------------------------
	
    @Test
	public void testRemoveVertex() throws Exception {
		DENOPTIMGraph graph = new DENOPTIMGraph();
		EmptyVertex v0 = new EmptyVertex(0);
		buildVertexAndConnectToGraph(v0, 3, graph);

		EmptyVertex v1 = new EmptyVertex(1);
		buildVertexAndConnectToGraph(v1, 2, graph);
		graph.addEdge(new DENOPTIMEdge(v0.getAP(0), v1.getAP(0)));

		EmptyVertex v2 = new EmptyVertex(2);
		buildVertexAndConnectToGraph(v2, 2, graph);
		graph.addEdge(new DENOPTIMEdge(v1.getAP(1), v2.getAP(0)));

		EmptyVertex v3 = new EmptyVertex(3);
		buildVertexAndConnectToGraph(v3, 1, graph);
		graph.addEdge(new DENOPTIMEdge(v2.getAP(1), v3.getAP(0)));

		EmptyVertex v4 = new EmptyVertex(4);
		buildVertexAndConnectToGraph(v4, 3, graph);
		graph.addEdge(new DENOPTIMEdge(v0.getAP(1), v4.getAP(0)));

		EmptyVertex v5 = new EmptyVertex(5);
		buildVertexAndConnectToGraph(v5, 1, graph);
		graph.addEdge(new DENOPTIMEdge(v4.getAP(1), v5.getAP(0)));

		EmptyVertex v6 = new EmptyVertex(6);
		buildVertexAndConnectToGraph(v6, 1, graph);
		graph.addEdge(new DENOPTIMEdge(v0.getAP(2), v6.getAP(0)));

		EmptyVertex v7 = new EmptyVertex(7);
		buildVertexAndConnectToGraph(v7, 1, graph);
		graph.addEdge(new DENOPTIMEdge(v4.getAP(2), v7.getAP(0)));

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
	public void testSameAs_Equal() throws DENOPTIMException
	{
		DENOPTIMGraph graphA = new DENOPTIMGraph();
		EmptyVertex v0 = new EmptyVertex(0);
		buildVertexAndConnectToGraph(v0, 3, graphA);

		EmptyVertex v1 = new EmptyVertex(1);
		buildVertexAndConnectToGraph(v1, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(0), v1.getAP(0)));

		EmptyVertex v2 = new EmptyVertex(2);
		buildVertexAndConnectToGraph(v2, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v1.getAP(1), v2.getAP(0)));

		// Other graph, but is the same graph

		DENOPTIMGraph graphB = new DENOPTIMGraph();
		EmptyVertex v90 = new EmptyVertex(90);
		buildVertexAndConnectToGraph(v90, 3, graphB);

		EmptyVertex v91 = new EmptyVertex(91);
		buildVertexAndConnectToGraph(v91, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(0), v91.getAP(0)));

		EmptyVertex v92 = new EmptyVertex(92);
		buildVertexAndConnectToGraph(v92, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v91.getAP(1), v92.getAP(0)));

		StringBuilder reason = new StringBuilder();
		assertTrue(graphA.sameAs(graphB, reason), reason.toString());
	}

//------------------------------------------------------------------------------

	@Test
	public void testSameAs_DiffVertex() throws DENOPTIMException
	{
		DENOPTIMGraph graphA = new DENOPTIMGraph();
		EmptyVertex v0 = new EmptyVertex(0);
		buildVertexAndConnectToGraph(v0, 3, graphA);

		EmptyVertex v1 = new EmptyVertex(1);
		buildVertexAndConnectToGraph(v1, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(0), v1.getAP(0)));

		EmptyVertex v2 = new EmptyVertex(2);
		buildVertexAndConnectToGraph(v2, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v1.getAP(1), v2.getAP(0)));

		// Other graph

		DENOPTIMGraph graphB = new DENOPTIMGraph();
		EmptyVertex v90 = new EmptyVertex(90);
		buildVertexAndConnectToGraph(v90, 3, graphB);

		EmptyVertex v91 = new EmptyVertex(91);
		buildVertexAndConnectToGraph(v91, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(0), v91.getAP(0)));

		EmptyVertex v92 = new EmptyVertex(92);
		buildVertexAndConnectToGraph(v92, 3, graphB);
		graphB.addEdge(new DENOPTIMEdge(v91.getAP(1), v92.getAP(0)));

		StringBuilder reason = new StringBuilder();
		assertFalse(graphA.sameAs(graphB, reason));
	}

//------------------------------------------------------------------------------

	@Test
	public void testSameAs_SameSymmSet() throws Exception {
		DENOPTIMGraph graphA = new DENOPTIMGraph();
		EmptyVertex v0 = new EmptyVertex(0);
		buildVertexAndConnectToGraph(v0, 4, graphA);

		EmptyVertex v1 = new EmptyVertex(1);
		buildVertexAndConnectToGraph(v1, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(0), v1.getAP(0)));

		EmptyVertex v2 = new EmptyVertex(2);
		buildVertexAndConnectToGraph(v2, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(1), v2.getAP(0)));

		EmptyVertex v3 = new EmptyVertex(3);
		buildVertexAndConnectToGraph(v3, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(2), v3.getAP(0)));

		EmptyVertex v4 = new EmptyVertex(4);
		buildVertexAndConnectToGraph(v4, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(3), v4.getAP(0)));

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
		EmptyVertex v90 = new EmptyVertex(90);
		buildVertexAndConnectToGraph(v90, 4, graphB);

		EmptyVertex v91 = new EmptyVertex(91);
		buildVertexAndConnectToGraph(v91, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(0), v91.getAP(0)));

		EmptyVertex v92 = new EmptyVertex(92);
		buildVertexAndConnectToGraph(v92, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(1), v92.getAP(0)));

		EmptyVertex v93 = new EmptyVertex(93);
		buildVertexAndConnectToGraph(v93, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(2), v93.getAP(0)));

		EmptyVertex v94 = new EmptyVertex(94);
		buildVertexAndConnectToGraph(v94, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(3), v94.getAP(0)));

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
		EmptyVertex v0 = new EmptyVertex(0);
		buildVertexAndConnectToGraph(v0, 4, graphA);

		EmptyVertex v1 = new EmptyVertex(1);
		buildVertexAndConnectToGraph(v1, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(0), v1.getAP(0)));

		EmptyVertex v2 = new EmptyVertex(2);
		buildVertexAndConnectToGraph(v2, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(1), v2.getAP(0)));

		EmptyVertex v3 = new EmptyVertex(3);
		buildVertexAndConnectToGraph(v3, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(2), v3.getAP(0)));

		EmptyVertex v4 = new EmptyVertex(4);
		buildVertexAndConnectToGraph(v4, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(3), v4.getAP(0)));

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
		EmptyVertex v90 = new EmptyVertex(90);
		buildVertexAndConnectToGraph(v90, 4, graphB);

		EmptyVertex v91 = new EmptyVertex(91);
		buildVertexAndConnectToGraph(v91, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(0), v1.getAP(0)));

		EmptyVertex v92 = new EmptyVertex(92);
		buildVertexAndConnectToGraph(v92, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v0.getAP(1), v2.getAP(0)));

		EmptyVertex v93 = new EmptyVertex(93);
		buildVertexAndConnectToGraph(v93, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v0.getAP(2), v3.getAP(0)));

		EmptyVertex v94 = new EmptyVertex(94);
		buildVertexAndConnectToGraph(v94, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v0.getAP(3), v4.getAP(0)));

		SymmetricSet ssB = new SymmetricSet();
		ssB.add(1);                           //difference
		ssB.add(3);                           //difference
		graphB.addSymmetricSetOfVertices(ssB);
		SymmetricSet ssB2 = new SymmetricSet();
		ssB2.add(2);                           //difference
		ssB2.add(4);                           //difference
		graphB.addSymmetricSetOfVertices(ssB2);

		StringBuilder reason = new StringBuilder();
		assertFalse(graphA.sameAs(graphB, reason));
	}

//------------------------------------------------------------------------------

	@Test
	public void testSameAs_SameRings() throws DENOPTIMException
	{
		DENOPTIMGraph graphA = new DENOPTIMGraph();
		EmptyVertex v0 = new EmptyVertex(0);
		buildVertexAndConnectToGraph(v0, 4, graphA);

		EmptyVertex v1 = new EmptyVertex(1);
		buildVertexAndConnectToGraph(v1, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(0), v1.getAP(0)));

		EmptyVertex v2 = new EmptyVertex(2);
		buildVertexAndConnectToGraph(v2, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(1), v2.getAP(0)));

		EmptyVertex v3 = new EmptyVertex(3);
		buildVertexAndConnectToGraph(v3, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(2), v3.getAP(0)));

		EmptyVertex v4 = new EmptyVertex(4);
		buildVertexAndConnectToGraph(v4, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(3), v4.getAP(0)));

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
		EmptyVertex v90 = new EmptyVertex(90);
		buildVertexAndConnectToGraph(v90, 4, graphB);

		EmptyVertex v91 = new EmptyVertex(91);
		buildVertexAndConnectToGraph(v91, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(0), v91.getAP(0)));

		EmptyVertex v92 = new EmptyVertex(92);
		buildVertexAndConnectToGraph(v92, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(1), v92.getAP(0)));

		EmptyVertex v93 = new EmptyVertex(93);
		buildVertexAndConnectToGraph(v93, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(2), v93.getAP(0)));

		EmptyVertex v94 = new EmptyVertex(94);
		buildVertexAndConnectToGraph(v94, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(3), v94.getAP(0)));

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

		StringBuilder reason = new StringBuilder();
		assertTrue(graphA.sameAs(graphB, reason));
	}

//------------------------------------------------------------------------------

	@Test
	public void testSameAs_DisorderRings() throws Exception 
	{
		DENOPTIMGraph graphA = new DENOPTIMGraph();
		EmptyVertex v0 = new EmptyVertex(0);
		buildVertexAndConnectToGraph(v0, 4, graphA);

		EmptyVertex v1 = new EmptyVertex(1);
		buildVertexAndConnectToGraph(v1, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(0), v1.getAP(0)));

		EmptyVertex v2 = new EmptyVertex(2);
		buildVertexAndConnectToGraph(v2, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(1), v2.getAP(0)));

		EmptyVertex v3 = new EmptyVertex(3);
		buildVertexAndConnectToGraph(v3, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(2), v3.getAP(0)));

		EmptyVertex v4 = new EmptyVertex(4);
		buildVertexAndConnectToGraph(v4, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(3), v4.getAP(0)));

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
		EmptyVertex v90 = new EmptyVertex(90);
		buildVertexAndConnectToGraph(v90, 4, graphB);

		EmptyVertex v91 = new EmptyVertex(91);
		buildVertexAndConnectToGraph(v91, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(0), v91.getAP(0)));

		EmptyVertex v92 = new EmptyVertex(92);
		buildVertexAndConnectToGraph(v92, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(1), v92.getAP(0)));

		EmptyVertex v93 = new EmptyVertex(93);
		buildVertexAndConnectToGraph(v93, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(2), v93.getAP(0)));

		EmptyVertex v94 = new EmptyVertex(94);
		buildVertexAndConnectToGraph(v94, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(3), v94.getAP(0)));

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

		StringBuilder reason = new StringBuilder();
		assertTrue(graphA.sameAs(graphB, reason));
	}

//------------------------------------------------------------------------------

	@Test
	public void testSameAs_DiffRings() throws DENOPTIMException
	{
		DENOPTIMGraph graphA = new DENOPTIMGraph();
		EmptyVertex v0 = new EmptyVertex(0);
		buildVertexAndConnectToGraph(v0, 4, graphA);

		EmptyVertex v1 = new EmptyVertex(1);
		buildVertexAndConnectToGraph(v1, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(0), v1.getAP(0)));

		EmptyVertex v2 = new EmptyVertex(2);
		buildVertexAndConnectToGraph(v2, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(1), v2.getAP(0)));

		EmptyVertex v3 = new EmptyVertex(3);
		buildVertexAndConnectToGraph(v3, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(2), v3.getAP(0)));

		EmptyVertex v4 = new EmptyVertex(4);
		buildVertexAndConnectToGraph(v4, 2, graphA);
		graphA.addEdge(new DENOPTIMEdge(v0.getAP(3), v4.getAP(0)));

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
		EmptyVertex v90 = new EmptyVertex(90);
		buildVertexAndConnectToGraph(v90, 4, graphB);

		EmptyVertex v91 = new EmptyVertex(91);
		buildVertexAndConnectToGraph(v91, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(0), v91.getAP(0)));

		EmptyVertex v92 = new EmptyVertex(92);
		buildVertexAndConnectToGraph(v92, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(1), v92.getAP(0)));

		EmptyVertex v93 = new EmptyVertex(93);
		buildVertexAndConnectToGraph(v93, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(2), v93.getAP(0)));

		EmptyVertex v94 = new EmptyVertex(94);
		buildVertexAndConnectToGraph(v94, 2, graphB);
		graphB.addEdge(new DENOPTIMEdge(v90.getAP(3), v94.getAP(0)));

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

		StringBuilder reason = new StringBuilder();
		assertFalse(graphA.sameAs(graphB, reason));
	}

//------------------------------------------------------------------------------

	@Test
	public void testGetAvailableAPs_returnsListOfAvailableAPs() 
	        throws DENOPTIMException 
	{
	    EmptyVertex vertex0 = new EmptyVertex(0);
	    EmptyVertex vertex1 = new EmptyVertex(1);

		vertex0.addAP(0);
		vertex0.addAP(0);
		vertex1.addAP(0);

		DENOPTIMEdge edge0 = new DENOPTIMEdge(vertex0.getAP(0),
		        vertex1.getAP(0));

		DENOPTIMGraph graph = new DENOPTIMGraph();
		graph.addVertex(vertex0);
		graph.addVertex(vertex1);
		graph.addEdge(edge0);

		List<DENOPTIMAttachmentPoint> lst = graph.getAvailableAPs();
		assertEquals(1,lst.size(), "Size of list");
		assertEquals(vertex0.getVertexId(),lst.get(0).getOwner().getVertexId(),
		        "ID of the vertex holding the available AP.");
	}

//------------------------------------------------------------------------------

	@Test
	public void testClone() throws DENOPTIMException {
		DENOPTIMGraph graph = new DENOPTIMGraph();
		EmptyVertex v0 = new EmptyVertex(0);
		buildVertexAndConnectToGraph(v0, 3, graph);

		EmptyVertex v1 = new EmptyVertex(1);
		buildVertexAndConnectToGraph(v1, 2, graph);
		graph.addEdge(new DENOPTIMEdge(v0.getAP(0), v1.getAP(0)));

		EmptyVertex v2 = new EmptyVertex(2);
		buildVertexAndConnectToGraph(v2, 2, graph);
		graph.addEdge(new DENOPTIMEdge(v1.getAP(1), v2.getAP(0)));

		EmptyVertex v3 = new EmptyVertex(3);
		buildVertexAndConnectToGraph(v3, 1, graph);
		graph.addEdge(new DENOPTIMEdge(v2.getAP(1), v3.getAP(0)));

		EmptyVertex v4 = new EmptyVertex(4);
		buildVertexAndConnectToGraph(v4, 3, graph);
		graph.addEdge(new DENOPTIMEdge(v0.getAP(1), v4.getAP(0)));

		EmptyVertex v5 = new EmptyVertex(5);
		buildVertexAndConnectToGraph(v5, 1, graph);
		graph.addEdge(new DENOPTIMEdge(v4.getAP(1), v5.getAP(0)));

		EmptyVertex v6 = new EmptyVertex(6);
		buildVertexAndConnectToGraph(v6, 1, graph);
		graph.addEdge(new DENOPTIMEdge(v0.getAP(2), v6.getAP(0)));

		EmptyVertex v7 = new EmptyVertex(7);
		buildVertexAndConnectToGraph(v7, 1, graph);
		graph.addEdge(new DENOPTIMEdge(v4.getAP(2), v7.getAP(0)));

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
		
		for (int iv=0; iv<graph.getVertexCount(); iv++)
		{
		    DENOPTIMVertex vg = graph.getVertexAtPosition(iv);
		    DENOPTIMVertex vc = clone.getVertexAtPosition(iv);
		    int hashVG = vg.hashCode();
		    int hashVC = vc.hashCode();
            
		    for (int iap = 0; iap<vg.getNumberOfAPs(); iap++)
		    {
		        assertEquals(vg.getAP(iap).getOwner().hashCode(), hashVG, 
		                "Reference to vertex owner in ap " + iap + " vertex " 
		                        + iv + "(G)");
                assertEquals(vc.getAP(iap).getOwner().hashCode(), hashVC, 
                        "Reference to vertex owner in ap " + iap + " vertex " 
                                + iv + " (C)");
		        assertNotEquals(vc.getAP(iap).getOwner().hashCode(),
		        vg.getAP(iap).getOwner().hashCode(),
		        "Owner of AP "+iap+" in vertex "+iv);
		    }
		}          
	}

//------------------------------------------------------------------------------

	@Test
	public void testGetMutationSites() throws DENOPTIMException 
	{
		DENOPTIMGraph graph = new DENOPTIMGraph();
		DENOPTIMTemplate tmpl = DENOPTIMTemplate.getTestTemplate(
		        ContractLevel.FIXED);
		graph.addVertex(tmpl);

		assertEquals(1, graph.getMutableSites().size(),
				"Size of mutation size list in case of frozen template");

		graph = new DENOPTIMGraph();
		tmpl = DENOPTIMTemplate.getTestTemplate(ContractLevel.FREE);
		graph.addVertex(tmpl);

		assertEquals(2, graph.getMutableSites().size(),
				"Size of mutation size list in case of free template");
	}
	
//------------------------------------------------------------------------------
    
    /**
     * Build a graph meant to be used in unit tests. The returned graph has
     * only the scaffold.
     * @return a new instance of the test graph.
     */
    private static DENOPTIMGraph makeTestGraph0() throws Exception
    {
        DENOPTIMGraph graph = new DENOPTIMGraph();
        DENOPTIMVertex s = DENOPTIMVertex.newVertexFromLibrary(0,
                BBType.SCAFFOLD);
        graph.addVertex(s);
        return graph;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Build a graph meant to be used in unit tests. The returned graph has
     * only the scaffold and one vertex, plus the edge
     * @return a new instance of the test graph.
     */
    private static DENOPTIMGraph makeTestGraph1() throws Exception
    {
        DENOPTIMGraph graph = new DENOPTIMGraph();
        DENOPTIMVertex s = DENOPTIMVertex.newVertexFromLibrary(1,
                BBType.SCAFFOLD);
        graph.addVertex(s);
        DENOPTIMVertex v1a = DENOPTIMVertex.newVertexFromLibrary(1,
                BBType.FRAGMENT);
        graph.addVertex(v1a);

        graph.addEdge(new DENOPTIMEdge(s.getAP(0), v1a.getAP(0)));
        
        return graph;
    }
	
//------------------------------------------------------------------------------
	
	/**
	 * Build a graph meant to be used in unit tests. The returned graph has
	 * the following structure:
	 * <pre>
	 *        (free)
	 *        ap2
	 *       /
	 * [C1-C0-ap0]-[ap0-O-ap1]-[ap0-H]
	 *  |    \
	 *  ap3   ap1
	 *  |       \
	 * (free)    [ap0-H]</pre>
	 * 
	 * @return a new instance of the test graph.
	 */
	public static DENOPTIMGraph makeTestGraphA() 
	{
        DENOPTIMGraph graph = new DENOPTIMGraph();
    
        // If we cannot make the test graph, something is deeeeeply wrong and
        // a bugfix is needed.
        try {
            IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
            IAtomContainer iac1 = builder.newAtomContainer();
            IAtom ia1 = new Atom("C");
            IAtom ia2 = new Atom("C");
            iac1.addAtom(ia1);
            iac1.addAtom(ia2);
            iac1.addBond(new Bond(ia1, ia2, IBond.Order.SINGLE));
            
            DENOPTIMFragment v1 = new DENOPTIMFragment(1, iac1, 
                    BBType.SCAFFOLD);
            v1.addAP(0);
            v1.addAP(0);
            v1.addAP(0);
            v1.addAP(1);
        
            IAtomContainer iac2 = builder.newAtomContainer();
            iac2.addAtom(new Atom("O"));
            DENOPTIMFragment v2 = new DENOPTIMFragment(2, iac2, 
                    BBType.FRAGMENT);
            v2.addAP(0);
            v2.addAP(0);
        
            IAtomContainer iac3 = builder.newAtomContainer();
            iac3.addAtom(new Atom("H"));
            DENOPTIMFragment v3 = new DENOPTIMFragment(3, iac3, 
                    BBType.CAP);
            v3.addAP(0);
        
            IAtomContainer iac4 = builder.newAtomContainer();
            iac4.addAtom(new Atom("H"));
            DENOPTIMFragment v4 = new DENOPTIMFragment(4, iac4, 
                    BBType.CAP);
            v4.addAP(0);
        
            graph.addVertex(v1);
            graph.addVertex(v2);
            graph.addVertex(v3);
            graph.addVertex(v4);
            graph.addEdge(new DENOPTIMEdge(v1.getAP(0), v2.getAP(0)));
            graph.addEdge(new DENOPTIMEdge(v1.getAP(1), v3.getAP(0)));
            graph.addEdge(new DENOPTIMEdge(v2.getAP(1), v4.getAP(0)));
            
            // Use this just to verify identify of the graph
            /*
                System.out.println("WRITING TEST GRAPH A");
                DenoptimIO.writeGraphsToFile(new File("/tmp/test_graph_A"), 
                        FileFormat.GRAPHJSON, 
                        new ArrayList<DENOPTIMGraph>(Arrays.asList(graph)));
            */
        } catch (Throwable t)
        {
            t.printStackTrace();
            System.err.println("FATAL ERROR! Could not make test graph (A). "
                    + "Please, report this to the development team.");
            System.exit(-1);
        }
        
        return graph;
	}

//------------------------------------------------------------------------------

	@Test
	public void testRemoveCapping() throws Exception {
	    
		DENOPTIMGraph graph = new DENOPTIMGraph();

		IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
		IAtomContainer iac1 = builder.newAtomContainer();
		iac1.addAtom(new Atom("C"));
		DENOPTIMFragment v1 = new DENOPTIMFragment(1, iac1, BBType.SCAFFOLD);
		v1.addAP(1);
		v1.addAP(1);

		IAtomContainer iac2 = builder.newAtomContainer();
		iac2.addAtom(new Atom("O"));
		DENOPTIMFragment v2 = new DENOPTIMFragment(2, iac2, BBType.FRAGMENT);
		v2.addAP(1);
		v2.addAP(1);

		IAtomContainer iac3 = builder.newAtomContainer();
		iac3.addAtom(new Atom("H"));
		DENOPTIMFragment v3 = new DENOPTIMFragment(3, iac3, BBType.CAP);
		v3.addAP(1);

		IAtomContainer iac4 = builder.newAtomContainer();
		iac4.addAtom(new Atom("H"));
		DENOPTIMFragment v4 = new DENOPTIMFragment(4, iac4, BBType.CAP);
		v4.addAP(1);

		graph.addVertex(v1);
		graph.addVertex(v2);
		graph.addVertex(v3);
		graph.addVertex(v4);
		graph.addEdge(new DENOPTIMEdge(v1.getAP(0), v2.getAP(0)));
		graph.addEdge(new DENOPTIMEdge(v1.getAP(1), v3.getAP(0)));
		graph.addEdge(new DENOPTIMEdge(v2.getAP(1), v4.getAP(0)));

		assertEquals(4, graph.getVertexCount(),
				"#vertices in graph before removal");
		assertTrue(graph == v4.getGraphOwner());

		graph.removeCappingGroupsOn(v2);

		assertEquals(3, graph.getVertexCount(),
				"#vertices in graph before removal");
		assertFalse(graph.containsVertex(v4),
				"Capping is still contained");
		assertTrue(null == v4.getGraphOwner(),
				"Owner of removed capping group is null");

		DENOPTIMGraph graph2 = new DENOPTIMGraph();

		IAtomContainer iac12 = builder.newAtomContainer();
		iac12.addAtom(new Atom("C"));
		DENOPTIMFragment v21 = new DENOPTIMFragment(21, iac12, BBType.SCAFFOLD);
		v21.addAP(0);
		v21.addAP(0);

		IAtomContainer iac22 = builder.newAtomContainer();
		iac22.addAtom(new Atom("O"));
		DENOPTIMFragment v22 = new DENOPTIMFragment(22, iac22, BBType.FRAGMENT);
		v22.addAP(0);
		v22.addAP(0);

		IAtomContainer iac23 = builder.newAtomContainer();
		iac23.addAtom(new Atom("H"));
		DENOPTIMFragment v23 = new DENOPTIMFragment(23, iac23, BBType.CAP);
		v23.addAP(0);

		IAtomContainer iac24 = builder.newAtomContainer();
		iac24.addAtom(new Atom("H"));
		DENOPTIMFragment v24 = new DENOPTIMFragment(24, iac24, BBType.CAP);
		v24.addAP(0);

		graph2.addVertex(v21);
		graph2.addVertex(v22);
		graph2.addVertex(v23);
		graph2.addVertex(v24);
		graph2.addEdge(new DENOPTIMEdge(v21.getAP(0), v22.getAP(0)));
		graph2.addEdge(new DENOPTIMEdge(v21.getAP(1), v23.getAP(0)));
		graph2.addEdge(new DENOPTIMEdge(v22.getAP(1), v24.getAP(0)));

		assertEquals(4, graph2.getVertexCount(),
				"#vertices in graph before removal (B)");
		assertTrue(graph2 == v23.getGraphOwner());
		assertTrue(graph2 == v24.getGraphOwner());

		graph2.removeCappingGroups();

		assertEquals(2, graph2.getVertexCount(),
				"#vertices in graph before removal (B)");
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

	private void buildVertexAndConnectToGraph(EmptyVertex v, int apCount,
	        DENOPTIMGraph graph) throws DENOPTIMException 
	{
		for (int atomPos = 0; atomPos < apCount; atomPos++) {
			v.addAP(atomPos);
		}
		graph.addVertex(v);
	}
	
//------------------------------------------------------------------------------

	@Test
	public void testFromToJSON() throws Exception 
	{
	    DENOPTIMGraph graph = new DENOPTIMGraph();
        
	    EmptyVertex v0 = new EmptyVertex(0);
        buildVertexAndConnectToGraph(v0, 3, graph);

        EmptyVertex v1 = new EmptyVertex(1);
        buildVertexAndConnectToGraph(v1, 2, graph);
        graph.addEdge(new DENOPTIMEdge(v0.getAP(0), v1.getAP(0)));

        EmptyVertex v2 = new EmptyVertex(2);
        buildVertexAndConnectToGraph(v2, 2, graph);
        graph.addEdge(new DENOPTIMEdge(v1.getAP(1), v2.getAP(0)));

        EmptyVertex v3 = new EmptyVertex(3);
        buildVertexAndConnectToGraph(v3, 1, graph);
        graph.addEdge(new DENOPTIMEdge(v2.getAP(1), v3.getAP(0)));

        EmptyVertex v4 = new EmptyVertex(4);
        buildVertexAndConnectToGraph(v4, 3, graph);
        graph.addEdge(new DENOPTIMEdge(v0.getAP(1), v4.getAP(0)));

        EmptyVertex v5 = new EmptyVertex(5);
        buildVertexAndConnectToGraph(v5, 1, graph);
        graph.addEdge(new DENOPTIMEdge(v4.getAP(1), v5.getAP(0)));

        EmptyVertex v6 = new EmptyVertex(6);
        buildVertexAndConnectToGraph(v6, 1, graph);
        graph.addEdge(new DENOPTIMEdge(v0.getAP(2), v6.getAP(0)));

        EmptyVertex v7 = new EmptyVertex(7);
        buildVertexAndConnectToGraph(v7, 1, graph);
        graph.addEdge(new DENOPTIMEdge(v4.getAP(2), v7.getAP(0)));

        graph.addRing(new DENOPTIMRing(new ArrayList<>(
                Arrays.asList(v5, v4, v0, v1, v2, v3))));

        graph.addRing(new DENOPTIMRing(new ArrayList<>(
                Arrays.asList(v6, v0, v4, v7))));

        graph.addSymmetricSetOfVertices(new SymmetricSet(
                new ArrayList<>(Arrays.asList(3, 5))));

        graph.addSymmetricSetOfVertices(new SymmetricSet(
                new ArrayList<>(Arrays.asList(6, 7))));
        
        String json1 = graph.toJson();
        
        DENOPTIMGraph g2 = DENOPTIMGraph.fromJson(json1);
        String json2 = g2.toJson();
        
        assertTrue(json1.equals(json2), "Round-trip via JSON is successful");
	}
	
//-----------------------------------------------------------------------------
    
    @Test
    public void testGraphIsomorphism() throws Exception 
    {
        prepareFragmentSpace();
        
        DENOPTIMGraph gAempty = new DENOPTIMGraph();
        DENOPTIMGraph gBempty = new DENOPTIMGraph();

        assertTrue(gAempty.isIsomorphicTo(gAempty),
                "self isomorphism on empty graph");
        assertTrue(gAempty.isIsomorphicTo(gBempty),
                "isomorphism on empty graphs");
        
        DENOPTIMGraph g01 = makeTestGraph0();
        DENOPTIMGraph g02 = makeTestGraph0();
        assertTrue(g01.isIsomorphicTo(g02),
                "single-vertex graph");
        assertTrue(g01.isIsomorphicTo(g01.clone()),
                "single-vertex graph vs clone");
        
        DENOPTIMGraph g11 = makeTestGraph1();
        DENOPTIMGraph g12 = makeTestGraph1();
        assertTrue(g11.isIsomorphicTo(g12),"two-vertex graph");
        assertTrue(g11.isIsomorphicTo(g11.clone()),"two-vertex graph vs clone");
        
        DENOPTIMGraph gD1 = makeTestGraphD();
        DENOPTIMGraph gD2 = makeTestGraphD();
        assertTrue(gD1.isIsomorphicTo(gD2),"two of same graph D");
        assertFalse(gD1.isIsomorphicTo(gAempty),"graph D vs empty graph");
        assertFalse(gAempty.isIsomorphicTo(gD1),"empty graph vs graph D");
        
        DENOPTIMGraph gB1 = makeTestGraphB();
        assertFalse(gD1.isIsomorphicTo(gB1),"graph D vs graph B");
        
        DENOPTIMGraph gB2 = gB1.clone();
        assertTrue(gB1.isIsomorphicTo(gB2),"graph B vs its clone");
    }

    
//-----------------------------------------------------------------------------
	
	@Test
    public void testExtractSubgraph() throws Exception 
	{
        prepareFragmentSpace();
        DENOPTIMGraph graph = makeTestGraphD();
        DENOPTIMGraph graphOriginal = makeTestGraphD();
        
        // This takes all graph
        DENOPTIMGraph subGraph = graph.extractSubgraph(
                graph.getVertexAtPosition(0));
        assertTrue(subGraph.isIsomorphicTo(graph), "complete subgraph");
        assertTrue(graph.isIsomorphicTo(graphOriginal), 
                "Original stays the same");
        
        // This takes a subgraph with a ring
        DENOPTIMGraph subGraph1 = graph.extractSubgraph(
                graph.getVertexAtPosition(1));
        DENOPTIMGraph expected1 = makeTestGraphDSub1();
        
        assertTrue(subGraph1.isIsomorphicTo(expected1), "Subgraph1");
        assertEquals(0,subGraph1.getSymmetricSetCount(),
                "SymmetricSets in subGraph1");
        assertEquals(1,subGraph1.getRingCount(),"Rings in subGraph1");
        assertTrue(graph.isIsomorphicTo(graphOriginal), 
                "Original stays the same");
       
        // This takes a subgraph with symmetric set, but no rings
        DENOPTIMGraph subGraph2 = graph.extractSubgraph(
                graph.getVertexAtPosition(10));
        DENOPTIMGraph expected2 = makeTestGraphDSub2();
        
        assertTrue(subGraph2.isIsomorphicTo(expected2), "Subgraph2");
        assertEquals(1,subGraph2.getSymmetricSetCount(),
                "SymmetricSets in subGraph2");
        assertEquals(0,subGraph2.getRingCount(),"Rings in subGraph2");
        assertTrue(graph.isIsomorphicTo(graphOriginal), 
                "Original stays the same");
	}
	
//-----------------------------------------------------------------------------
	
}