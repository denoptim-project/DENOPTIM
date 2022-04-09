package denoptim.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

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
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.graph.Edge.BondType;
import denoptim.graph.Template.ContractLevel;
import denoptim.graph.Vertex.BBType;
import denoptim.graph.Vertex.VertexType;
import denoptim.graph.rings.PathSubGraph;
import denoptim.io.DenoptimIO;
import denoptim.utils.MutationType;


/**
 * Unit test for DENOPTIMGraph
 * 
 * @author Marco Foscato
 */

public class DGraphTest {
    
    private static APClass APCA, APCB, APCC, APCD, CAPP;
    private static String a="A", b="B", c="C", d="D", cap="cap";
    
//------------------------------------------------------------------------------
    
    public static FragmentSpace prepare() throws DENOPTIMException
    {
        APCA = APClass.make(a, 0);
        APCB = APClass.make(b, 0);
        APCC = APClass.make(c, 0);
        APCD = APClass.make(d, 99);
        CAPP = APClass.make(cap, 1);
        
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
        FragmentSpaceParameters fsp = new FragmentSpaceParameters();
        FragmentSpace fs = new FragmentSpace(fsp,
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(), 
                cpMap, capMap, forbEnds, cpMap);
        fs.setAPclassBasedApproach(true);
        
        EmptyVertex s0 = new EmptyVertex();
        s0.setBuildingBlockType(BBType.SCAFFOLD);
        s0.addAP(APCA);
        s0.addAP(APCA);
        fs.appendVertexToLibrary(s0, BBType.SCAFFOLD, fs.getScaffoldLibrary());
        
        EmptyVertex s1 = new EmptyVertex();
        s1.setBuildingBlockType(BBType.SCAFFOLD);
        s1.addAP(APCA);
        s1.addAP(APCA);
        s1.addAP(APCD);
        fs.appendVertexToLibrary(s1, BBType.SCAFFOLD, fs.getScaffoldLibrary());
        
        EmptyVertex v0 = new EmptyVertex();
        v0.setBuildingBlockType(BBType.FRAGMENT);
        v0.addAP(APCA);
        v0.addAP(APCB);
        v0.addAP(APCA);
        fs.appendVertexToLibrary(v0, BBType.FRAGMENT, fs.getFragmentLibrary());
        
        EmptyVertex v1 = new EmptyVertex();
        v1.setBuildingBlockType(BBType.FRAGMENT);
        v1.addAP(APCA);
        v1.addAP(APCB);
        v1.addAP(APCA);
        v1.addAP(APCB);
        v1.addAP(APCC);
        fs.appendVertexToLibrary(v1, BBType.FRAGMENT, fs.getFragmentLibrary());
        
        EmptyVertex v2 = new EmptyVertex();
        v2.setBuildingBlockType(BBType.FRAGMENT);
        v2.addAP(APCB);
        fs.appendVertexToLibrary(v2, BBType.FRAGMENT, fs.getFragmentLibrary());
        
        EmptyVertex v3 = new EmptyVertex();
        v3.setBuildingBlockType(BBType.FRAGMENT);
        v3.addAP(APCD);
        v3.addAP(APCD);
        fs.appendVertexToLibrary(v3, BBType.FRAGMENT, fs.getFragmentLibrary());
       
        EmptyVertex v4 = new EmptyVertex();
        v4.setBuildingBlockType(BBType.FRAGMENT);
        v4.addAP(APCC);
        v4.addAP(APCB);
        v4.addAP(APCB);
        v4.addAP(APCA);
        v4.addAP(APCA);
        fs.appendVertexToLibrary(v4, BBType.FRAGMENT, fs.getFragmentLibrary());
        
        EmptyVertex v5 = new EmptyVertex();
        v5.setBuildingBlockType(BBType.FRAGMENT);
        v5.addAP(APCB);
        v5.addAP(APCD);
        fs.appendVertexToLibrary(v5, BBType.FRAGMENT, fs.getFragmentLibrary());
        
        EmptyVertex v6 = new EmptyVertex();
        v6.setBuildingBlockType(BBType.FRAGMENT);
        v6.addAP(APCC);
        v6.addAP(APCD);
        fs.appendVertexToLibrary(v6, BBType.FRAGMENT, fs.getFragmentLibrary());
        
        EmptyVertex v7 = new EmptyVertex();
        v7.setBuildingBlockType(BBType.FRAGMENT);
        v7.setAsRCV(true);
        v7.addAP(APClass.make("ATneutral:0", BondType.ANY));
        fs.appendVertexToLibrary(v7, BBType.FRAGMENT, fs.getFragmentLibrary());

        EmptyVertex c0 = new EmptyVertex();
        c0.setBuildingBlockType(BBType.CAP);
        c0.addAP(CAPP);
        fs.appendVertexToLibrary(c0, BBType.CAP, fs.getCappingLibrary());
        
        return fs;
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
    private DGraph makeTestGraphB(FragmentSpace fs) 
            throws DENOPTIMException
    {
        DGraph graph = new DGraph();
        Vertex s = Vertex.newVertexFromLibrary(0,
                BBType.SCAFFOLD, fs);
        graph.addVertex(s);
        Vertex v1a = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v1a);
        Vertex v2a = Vertex.newVertexFromLibrary(2,
                BBType.FRAGMENT, fs);
        graph.addVertex(v2a);
        Vertex v2a_bis = Vertex.newVertexFromLibrary(2,
                BBType.FRAGMENT, fs);
        graph.addVertex(v2a_bis);
        Vertex v3a = Vertex.newVertexFromLibrary(3,
                BBType.FRAGMENT, fs);
        graph.addVertex(v3a);
        Vertex v1b = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v1b);
        Vertex v2b = Vertex.newVertexFromLibrary(2,
                BBType.FRAGMENT, fs);
        graph.addVertex(v2b);
        Vertex v2b_bis = Vertex.newVertexFromLibrary(2,
                BBType.FRAGMENT, fs);
        graph.addVertex(v2b_bis);
        Vertex v3b = Vertex.newVertexFromLibrary(3,
                BBType.FRAGMENT, fs);
        graph.addVertex(v3b);
        graph.addEdge(new Edge(s.getAP(0), v1a.getAP(0)));
        graph.addEdge(new Edge(v1a.getAP(1), v2a.getAP(0)));
        graph.addEdge(new Edge(v1a.getAP(3), v2a_bis.getAP(0)));
        graph.addEdge(new Edge(v1a.getAP(4), v3a.getAP(0)));
        graph.addEdge(new Edge(s.getAP(1), v1b.getAP(0)));
        graph.addEdge(new Edge(v1b.getAP(1), v2b.getAP(0)));
        graph.addEdge(new Edge(v1b.getAP(3), v2b_bis.getAP(0)));
        graph.addEdge(new Edge(v1b.getAP(4), v3b.getAP(0)));
        
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
    private DGraph makeTestGraphC(FragmentSpace fs) 
            throws DENOPTIMException
    {
        DGraph graph = new DGraph();
        Vertex s = Vertex.newVertexFromLibrary(0,
                BBType.SCAFFOLD, fs);
        graph.addVertex(s);
        Vertex v1a = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.appendVertexOnAP(s.getAP(0), v1a.getAP(2));
        Vertex v2a = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.appendVertexOnAP(v1a.getAP(3), v2a.getAP(1));
        Vertex v3a = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.appendVertexOnAP(v2a.getAP(3), v3a.getAP(1));
        Vertex v4a = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.appendVertexOnAP(v3a.getAP(3), v4a.getAP(1));
        Vertex v5a = Vertex.newVertexFromLibrary(0,
                BBType.FRAGMENT, fs);
        graph.appendVertexOnAP(v2a.getAP(2), v5a.getAP(0));
        
        Vertex v1b = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.appendVertexOnAP(s.getAP(1), v1b.getAP(2));
        Vertex v2b = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.appendVertexOnAP(v1b.getAP(3), v2b.getAP(1));
        Vertex v3b = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.appendVertexOnAP(v2b.getAP(3), v3b.getAP(1));
        Vertex v4b = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.appendVertexOnAP(v3b.getAP(3), v4b.getAP(1));
        Vertex v5b = Vertex.newVertexFromLibrary(0,
                BBType.FRAGMENT, fs);
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
     *        (C)-(C)-v6-(D)--(B)-v7
     *        /                    |
     *       /                   chord
     *      |                      |
     *      | (B)-(C)-v6-(D)--(B)-v7
     *      |/
     *  (A)-v1-(B)-(C)-v6-(D)--(B)-v7
     *       |                     |
     *      (A)                    |
     *       |                   chord
     *      (A)                    |
     *    scaffold(D)----------(B)-v7
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
    private DGraph makeTestGraphD(FragmentSpace fs) 
            throws DENOPTIMException
    {
        DGraph graph = new DGraph();
        Vertex s = Vertex.newVertexFromLibrary(1,
                BBType.SCAFFOLD, fs);
        graph.addVertex(s);
        Vertex v1a = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v1a);
        Vertex v6a = Vertex.newVertexFromLibrary(6,
                BBType.FRAGMENT, fs);
        graph.addVertex(v6a);
        Vertex v6a_bis = Vertex.newVertexFromLibrary(6,
                BBType.FRAGMENT, fs);
        graph.addVertex(v6a_bis);
        Vertex v6a_tris = Vertex.newVertexFromLibrary(6,
                BBType.FRAGMENT, fs);
        graph.addVertex(v6a_tris);
        Vertex v7a = Vertex.newVertexFromLibrary(7,
                BBType.FRAGMENT, fs);
        graph.addVertex(v7a);
        Vertex v7a_bis = Vertex.newVertexFromLibrary(7,
                BBType.FRAGMENT, fs);
        graph.addVertex(v7a_bis);
        Vertex v7a_tris = Vertex.newVertexFromLibrary(7,
                BBType.FRAGMENT, fs);
        graph.addVertex(v7a_tris);
        
        Vertex v7a_quat = Vertex.newVertexFromLibrary(7,
                BBType.FRAGMENT, fs);
        graph.addVertex(v7a_quat);
        
        Vertex v1c = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v1c);
        
        Vertex v1b = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v1b);
        Vertex v2b = Vertex.newVertexFromLibrary(2,
                BBType.FRAGMENT, fs);
        graph.addVertex(v2b);
        Vertex v2b_bis = Vertex.newVertexFromLibrary(2,
                BBType.FRAGMENT, fs);
        graph.addVertex(v2b_bis);
        Vertex v3b = Vertex.newVertexFromLibrary(3,
                BBType.FRAGMENT, fs);
        graph.addVertex(v3b);
        graph.addEdge(new Edge(s.getAP(0), v1a.getAP(0)));
        graph.addEdge(new Edge(v1a.getAP(1), v6a.getAP(0)));
        graph.addEdge(new Edge(v1a.getAP(3), v6a_bis.getAP(0)));
        graph.addEdge(new Edge(v1a.getAP(4), v6a_tris.getAP(0)));
        graph.addEdge(new Edge(v6a.getAP(1), v7a.getAP(0)));
        graph.addEdge(new Edge(v6a_bis.getAP(1), v7a_bis.getAP(0)));
        graph.addEdge(new Edge(v6a_tris.getAP(1), v7a_tris.getAP(0)));
        graph.addEdge(new Edge(s.getAP(2), v7a_quat.getAP(0)));
        graph.addEdge(new Edge(s.getAP(1), v1c.getAP(0)));
        graph.addEdge(new Edge(v1c.getAP(2), v1b.getAP(0)));
        graph.addEdge(new Edge(v1b.getAP(1), v2b.getAP(0)));
        graph.addEdge(new Edge(v1b.getAP(3), v2b_bis.getAP(0)));
        graph.addEdge(new Edge(v1b.getAP(4), v3b.getAP(0)));
        
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
     *        (C)-(C)-v6-(D)--(B)-v7
     *        /                    |
     *       /                   chord
     *      |                      |
     *      | (B)-(C)-v6-(D)--(B)-v7
     *      |/
     *  (A)-v1-(B)-(C)-v6-(D)--(B)-v7
     *   </pre>
     *   
     */
    private DGraph makeTestGraphDSub1(FragmentSpace fs) 
            throws DENOPTIMException
    {
        DGraph graph = new DGraph();
        Vertex v1a = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v1a);
        Vertex v6a = Vertex.newVertexFromLibrary(6,
                BBType.FRAGMENT, fs);
        graph.addVertex(v6a);
        Vertex v6a_bis = Vertex.newVertexFromLibrary(6,
                BBType.FRAGMENT, fs);
        graph.addVertex(v6a_bis);
        Vertex v6a_tris = Vertex.newVertexFromLibrary(6,
                BBType.FRAGMENT, fs);
        graph.addVertex(v6a_tris);
        Vertex v7a = Vertex.newVertexFromLibrary(7,
                BBType.FRAGMENT, fs);
        graph.addVertex(v7a);
        Vertex v7a_bis = Vertex.newVertexFromLibrary(7,
                BBType.FRAGMENT, fs);
        graph.addVertex(v7a_bis);
        Vertex v7a_tris = Vertex.newVertexFromLibrary(7,
                BBType.FRAGMENT, fs);
        graph.addVertex(v7a_tris);
        
        graph.addEdge(new Edge(v1a.getAP(1), v6a.getAP(0)));
        graph.addEdge(new Edge(v1a.getAP(3), v6a_bis.getAP(0)));
        graph.addEdge(new Edge(v1a.getAP(4), v6a_tris.getAP(0)));
        graph.addEdge(new Edge(v6a.getAP(1), v7a.getAP(0)));
        graph.addEdge(new Edge(v6a_bis.getAP(1), v7a_bis.getAP(0)));
        graph.addEdge(new Edge(v6a_tris.getAP(1), v7a_tris.getAP(0)));
        
        graph.addRing(v7a_bis, v7a_tris);
        
        graph.renumberGraphVertices();
        return graph;
    }
    
//------------------------------------------------------------------------------

    /**
     *  Creates a test graph that looks like this: 
     * 
     *  <pre>
     *        0(A)
     *       /
     * 2(A)-v1-1(B)-(B)-v2
     *      |\
     *      | 3(B)-(B)-v2
     *      \
     *       4(C)-(C)-v3
     *   </pre>
     *   
     */
    private DGraph makeTestGraphDSub2(FragmentSpace fs) 
            throws DENOPTIMException
    {
        DGraph graph = new DGraph();
        Vertex v1b = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v1b);
        Vertex v2b = Vertex.newVertexFromLibrary(2,
                BBType.FRAGMENT, fs);
        graph.addVertex(v2b);
        Vertex v2b_bis = Vertex.newVertexFromLibrary(2,
                BBType.FRAGMENT, fs);
        graph.addVertex(v2b_bis);
        Vertex v3b = Vertex.newVertexFromLibrary(3,
                BBType.FRAGMENT, fs);
        graph.addVertex(v3b);
        graph.addEdge(new Edge(v1b.getAP(1), v2b.getAP(0)));
        graph.addEdge(new Edge(v1b.getAP(3), v2b_bis.getAP(0)));
        graph.addEdge(new Edge(v1b.getAP(4), v3b.getAP(0)));
        
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

     * 2(A)-v1-1(B)-2(A)-v1(*)-1(B)-2(A)-v1(**)
     *      | \           |\
     *      |  |          | 3(B)-2(A)-v1(**)
     *      |  |
     *      |  3(B)-2(A)-v1(*)-1(B)-2(A)-v1
     *      |             |\
     *      |             | 3(B)-2(A)-v1
     *      \
     *       4(C)-2(A)-v1
     *   </pre>
     *  
     *  where only those marked with a star are included in the symmetric sets.
     */
    private DGraph makeTestGraphE(FragmentSpace fs) 
            throws DENOPTIMException
    {
        DGraph graph = new DGraph();
        Vertex v1b = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v1b);
        //First layer
        Vertex v2b = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v2b);
        Vertex v2b_bis = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v2b_bis);
        Vertex v2b_tris = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v2b_tris);
        graph.addEdge(new Edge(v1b.getAP(1), v2b.getAP(2)));
        graph.addEdge(new Edge(v1b.getAP(3), v2b_bis.getAP(2)));
        graph.addEdge(new Edge(v1b.getAP(4), v2b_tris.getAP(2)));
        //Second layer
        Vertex v3b_1 = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v3b_1);
        Vertex v3b_2 = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v3b_2);
        Vertex v3b_3 = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v3b_3);
        Vertex v3b_4 = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v3b_4);
        graph.addEdge(new Edge(v2b.getAP(1), v3b_1.getAP(2)));
        graph.addEdge(new Edge(v2b.getAP(3), v3b_2.getAP(2)));
        graph.addEdge(new Edge(v2b_bis.getAP(1), v3b_3.getAP(2)));
        graph.addEdge(new Edge(v2b_bis.getAP(3), v3b_4.getAP(2)));
        
        ArrayList<Integer> symFirstLayer = new ArrayList<Integer>();
        symFirstLayer.add(v2b.getVertexId());
        symFirstLayer.add(v2b_bis.getVertexId());
        graph.addSymmetricSetOfVertices(new SymmetricSet(symFirstLayer));
        
        ArrayList<Integer> symSecondLayer = new ArrayList<Integer>();
        symSecondLayer.add(v3b_1.getVertexId());
        symSecondLayer.add(v3b_2.getVertexId());
        graph.addSymmetricSetOfVertices(new SymmetricSet(symSecondLayer));
        
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
     *      |  |
     *      | /     (B)  (A)           (B) 
     *      |/         \/              /
     *  (A)-v1-(B)-(B)-v1-(A)--(A)-v1--(B)
     *       |          |          |\
     *      (A)         |          | (C)
     *                  |         (A)  
     *                  |
     *                  (C)--(C)-v6-(D)
     *                            
     *                            
     *                            
     *   </pre>
     *   
     */
    private DGraph makeTestGraphF(FragmentSpace fs) 
            throws DENOPTIMException
    {
        DGraph graph = new DGraph();
        Vertex v1a = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v1a);
        Vertex v6a = Vertex.newVertexFromLibrary(6,
                BBType.FRAGMENT, fs);
        graph.addVertex(v6a);
        Vertex v6a_bis = Vertex.newVertexFromLibrary(6,
                BBType.FRAGMENT, fs);
        graph.addVertex(v6a_bis);
        Vertex v7a = Vertex.newVertexFromLibrary(7,
                BBType.FRAGMENT, fs);
        graph.addVertex(v7a);
        Vertex v7a_bis = Vertex.newVertexFromLibrary(7,
                BBType.FRAGMENT, fs);
        graph.addVertex(v7a_bis);
        
        Vertex v1b = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v1b);
        Vertex v1c = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v1c);
        Vertex v6a_tris = Vertex.newVertexFromLibrary(6,
                BBType.FRAGMENT, fs);
        graph.addVertex(v6a_tris);
        graph.addEdge(new Edge(v1a.getAP(1), v6a.getAP(0)));
        graph.addEdge(new Edge(v1a.getAP(4), v6a_bis.getAP(0)));
        graph.addEdge(new Edge(v6a.getAP(1), v7a.getAP(0)));
        graph.addEdge(new Edge(v6a_bis.getAP(1), v7a_bis.getAP(0)));
        graph.addEdge(new Edge(v1a.getAP(3), v1b.getAP(1)));
        graph.addEdge(new Edge(v1b.getAP(2), v1c.getAP(0)));
        graph.addEdge(new Edge(v1b.getAP(4), v6a_tris.getAP(0)));
        
        graph.addRing(v7a, v7a_bis);
        
        ArrayList<Integer> symA = new ArrayList<Integer>();
        symA.add(v6a.getVertexId());
        symA.add(v6a_bis.getVertexId());
        graph.addSymmetricSetOfVertices(new SymmetricSet(symA));
        
        ArrayList<Integer> symB = new ArrayList<Integer>();
        symB.add(v7a.getVertexId());
        symB.add(v7a_bis.getVertexId());
        graph.addSymmetricSetOfVertices(new SymmetricSet(symB));
        
        graph.renumberGraphVertices();
        return graph;
    }
    
//------------------------------------------------------------------------------

    /**
     *  Creates a test graph that looks like this: 
     * 
     *  <pre>
     *  
     *        (C)-(C)-v6-(D)--(B)-v7
     *        /                    |
     *       /                   chord
     *      |                      |
     *      | (B)-(C)-v6-(D)--(B)-v7  
     *      |  |            --------------(C)-v6-(D)--(B)-v7  
     *      |  /           /                              |
     *      | /     (A)  (B)           (B)                |
     *      |/         \/              /                chord
     *  (A)-v1-(B)-(B)-v1-(A)--(A)-v1--(B)                | 
     *       |          |          |\                     |
     *      (A)         |          | (C)--(C)-v6-(D)--(B)-v7 
     *       |          |          |
     *       |          |         (A)--(C)-v6-(D)--(B)-v7  
     *       |          |                              |
     *       |          |                            chord
     *       |          |                              |
     *       |          ---------(C)--(C)-v6-(D)--(B)--v7                
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
     *       
     *   </pre>
     *   This is the result of replacing two vertexes in graph D with Graph F
     *   and welding connections.
     */
    private DGraph makeTestGraphG(FragmentSpace fs) 
            throws DENOPTIMException
    {
        DGraph graph = new DGraph();
        Vertex v1a = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v1a);
        Vertex v6a = Vertex.newVertexFromLibrary(6,
                BBType.FRAGMENT, fs);
        graph.addVertex(v6a);
        Vertex v6a_bis = Vertex.newVertexFromLibrary(6,
                BBType.FRAGMENT, fs);
        graph.addVertex(v6a_bis);
        Vertex v7a = Vertex.newVertexFromLibrary(7,
                BBType.FRAGMENT, fs);
        graph.addVertex(v7a);
        Vertex v7a_bis = Vertex.newVertexFromLibrary(7,
                BBType.FRAGMENT, fs);
        graph.addVertex(v7a_bis);
        
        Vertex v1b = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v1b);
        Vertex v1c = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v1c);
        Vertex v6a_tris = Vertex.newVertexFromLibrary(6,
                BBType.FRAGMENT, fs);
        graph.addVertex(v6a_tris);
        
        graph.addEdge(new Edge(v1a.getAP(1), v6a.getAP(0)));
        graph.addEdge(new Edge(v1a.getAP(4), v6a_bis.getAP(0)));
        graph.addEdge(new Edge(v6a.getAP(1), v7a.getAP(0)));
        graph.addEdge(new Edge(v6a_bis.getAP(1), v7a_bis.getAP(0)));
        graph.addEdge(new Edge(v1a.getAP(3), v1b.getAP(1)));
        graph.addEdge(new Edge(v1b.getAP(2), v1c.getAP(0)));
        graph.addEdge(new Edge(v1b.getAP(4), v6a_tris.getAP(0)));
        
        graph.addRing(v7a, v7a_bis);
        
        ArrayList<Integer> symA = new ArrayList<Integer>();
        symA.add(v6a.getVertexId());
        symA.add(v6a_bis.getVertexId());
        graph.addSymmetricSetOfVertices(new SymmetricSet(symA));
        
        ArrayList<Integer> symB = new ArrayList<Integer>();
        symB.add(v7a.getVertexId());
        symB.add(v7a_bis.getVertexId());
        graph.addSymmetricSetOfVertices(new SymmetricSet(symB));
        
        // up to here it is graph F. Now come the pieces of graph D
        
        Vertex v6a_d = Vertex.newVertexFromLibrary(6,
                BBType.FRAGMENT, fs);
        graph.addVertex(v6a_d);
        Vertex v6a_d_bis = Vertex.newVertexFromLibrary(6,
                BBType.FRAGMENT, fs);
        graph.addVertex(v6a_d_bis);
        Vertex v6a_d_tris = Vertex.newVertexFromLibrary(6,
                BBType.FRAGMENT, fs);
        graph.addVertex(v6a_d_tris);
        Vertex v7a_d = Vertex.newVertexFromLibrary(7,
                BBType.FRAGMENT, fs);
        graph.addVertex(v7a_d);
        Vertex v7a_d_bis = Vertex.newVertexFromLibrary(7,
                BBType.FRAGMENT, fs);
        graph.addVertex(v7a_d_bis);
        Vertex v7a_d_tris = Vertex.newVertexFromLibrary(7,
                BBType.FRAGMENT, fs);
        graph.addVertex(v7a_d_tris);

        Vertex v7a_d_quat = Vertex.newVertexFromLibrary(7,
                BBType.FRAGMENT, fs);
        graph.addVertex(v7a_d_quat);

        Vertex v1c_d = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v1c_d);

        Vertex v1b_d = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v1b_d);
        Vertex v2b_d = Vertex.newVertexFromLibrary(2,
                BBType.FRAGMENT, fs);
        graph.addVertex(v2b_d);
        Vertex v2b_d_bis = Vertex.newVertexFromLibrary(2,
                BBType.FRAGMENT, fs);
        graph.addVertex(v2b_d_bis);
        Vertex v3b_d = Vertex.newVertexFromLibrary(3,
                BBType.FRAGMENT, fs);
        graph.addVertex(v3b_d);
        graph.addEdge(new Edge(v1c.getAP(2), v6a_d.getAP(0)));
        graph.addEdge(new Edge(v1b.getAP(3), v6a_d_bis.getAP(0)));
        graph.addEdge(new Edge(v1c.getAP(4), v6a_d_tris.getAP(0)));
        graph.addEdge(new Edge(v6a_d.getAP(1), v7a_d.getAP(0)));
        graph.addEdge(new Edge(v6a_d_bis.getAP(1), v7a_d_bis.getAP(0)));
        graph.addEdge(new Edge(v6a_d_tris.getAP(1), v7a_d_tris.getAP(0)));
        graph.addEdge(new Edge(v6a_tris.getAP(1), v7a_d_quat.getAP(0)));
        graph.addEdge(new Edge(v1a.getAP(2), v1c_d.getAP(0)));
        graph.addEdge(new Edge(v1c_d.getAP(2), v1b_d.getAP(0)));
        graph.addEdge(new Edge(v1b_d.getAP(1), v2b_d.getAP(0)));
        graph.addEdge(new Edge(v1b_d.getAP(3), v2b_d_bis.getAP(0)));
        graph.addEdge(new Edge(v1b_d.getAP(4), v3b_d.getAP(0)));

        graph.addRing(v7a_d, v7a_d_quat);
        graph.addRing(v7a_d_bis, v7a_d_tris);

        ArrayList<Integer> symC = new ArrayList<Integer>();
        symC.add(v2b_d.getVertexId());
        symC.add(v2b_d_bis.getVertexId());
        
        graph.addSymmetricSetOfVertices(new SymmetricSet(symC));
        return graph;
    }
    
//------------------------------------------------------------------------------
    
    /**
     *  Creates a test graph that looks like this: 
     * 
     *  <pre>
     *        (C)-(C)-v6-(D)--(B)-v7
     *        /                    |
     *       /                   chord
     *      |                      |
     *      | (B)-(C)-v6-(D)--(B)-v7
     *      |/
     *  (A)-v1-(B)
     *       \
     *        (A)
     *   </pre>
     *   
     */
    private DGraph makeTestGraphH(FragmentSpace fs) 
            throws DENOPTIMException
    {
        DGraph graph = new DGraph();
        Vertex v1a = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v1a);
        Vertex v6a_bis = Vertex.newVertexFromLibrary(6,
                BBType.FRAGMENT, fs);
        graph.addVertex(v6a_bis);
        Vertex v6a_tris = Vertex.newVertexFromLibrary(6,
                BBType.FRAGMENT, fs);
        graph.addVertex(v6a_tris);
        Vertex v7a_bis = Vertex.newVertexFromLibrary(7,
                BBType.FRAGMENT, fs);
        graph.addVertex(v7a_bis);
        Vertex v7a_tris = Vertex.newVertexFromLibrary(7,
                BBType.FRAGMENT, fs);
        graph.addVertex(v7a_tris);
        
        graph.addEdge(new Edge(v1a.getAP(3), v6a_bis.getAP(0)));
        graph.addEdge(new Edge(v1a.getAP(4), v6a_tris.getAP(0)));
        graph.addEdge(new Edge(v6a_bis.getAP(1), v7a_bis.getAP(0)));
        graph.addEdge(new Edge(v6a_tris.getAP(1), v7a_tris.getAP(0)));
        
        graph.addRing(v7a_bis, v7a_tris);
        
        graph.renumberGraphVertices();
        return graph;
    }
    
//------------------------------------------------------------------------------
    
    /**
     *  Creates a test graph that looks like this: 
     * 
     *  <pre>
     *        (A)-(C)-v6-(D)--(B)-v7
     *        /                    |
     *       /                   chord
     *      |                      |
     *      | (B)-(C)-v6-(D)--(B)-v7
     *      |/
     *  template-(A)
     *   </pre>
     *   
     */
    private DGraph makeTestGraphI(FragmentSpace fs) 
            throws Exception
    {
        DGraph innerGraph = makeTestGraphH(fs);
        Template tmpl = new Template(BBType.FRAGMENT);
        tmpl.setInnerGraph(innerGraph);
        
        DGraph graph = new DGraph();
        graph.addVertex(tmpl);
        Vertex v6 = Vertex.newVertexFromLibrary(6,
                BBType.FRAGMENT, fs);
        graph.addVertex(v6);
        Vertex v7 = Vertex.newVertexFromLibrary(7,
                BBType.FRAGMENT, fs);
        graph.addVertex(v7);
        Vertex v6a = Vertex.newVertexFromLibrary(6,
                BBType.FRAGMENT, fs);
        graph.addVertex(v6a);
        Vertex v7a = Vertex.newVertexFromLibrary(7,
                BBType.FRAGMENT, fs);
        graph.addVertex(v7a);
        
        graph.addEdge(new Edge(tmpl.getAP(0), v6.getAP(0)));
        graph.addEdge(new Edge(v6.getAP(1), v7.getAP(0)));
        graph.addEdge(new Edge(tmpl.getAP(1), v6a.getAP(0)));
        graph.addEdge(new Edge(v6a.getAP(1), v7a.getAP(0)));
        
        graph.addRing(v7, v7a);
        
        graph.renumberGraphVertices();
        return graph;
    }
    
//------------------------------------------------------------------------------

    /**
     *  Creates a test graph that looks like this: 
     * 
     *  <pre>              
     *                     0(A)
     *                    /
     *        0(A)--2(A)-v1-1(B)
     *       /           | \
     * 2(A)-v1-1(B)       \  3(B)
     *      |\             4(C)
     *      | 3(B)
     *      \
     *       4(C)
     *   </pre>
     *   
     */
    private DGraph makeTestGraphJ(FragmentSpace fs) 
            throws DENOPTIMException
    {
        DGraph graph = new DGraph();
        Vertex v1 = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v1);
        Vertex v1b = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v1b);
        graph.addEdge(new Edge(v1.getAP(0), v1b.getAP(2)));
        graph.renumberGraphVertices();
        return graph;
    }
    
//------------------------------------------------------------------------------
    
    /**
     *  Creates a test graph that looks like this: 
     * 
     *  <pre>
     *        (C)-(C)-v6-(D)--(C)-v1-(capped)
     *       /
     *      | (B)-(C)-v6*-(D)--(C)-v1**-(capped)
     *      |/
     *  (A)-v1-(B)-(C)-v6*-(D)--(C)-v1**-(capped)
 
     *   </pre>
     *   where vertexes marked with either * or ** are in symmetric sets, and
     *   "capped" means all APs have been used by capping groups.
     */
    private DGraph makeTestGraphK(FragmentSpace fs) 
            throws DENOPTIMException
    {
        DGraph graph = new DGraph();
        Vertex v1a = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v1a);
        Vertex v6a = Vertex.newVertexFromLibrary(6,
                BBType.FRAGMENT, fs);
        graph.addVertex(v6a);
        Vertex v6a_bis = Vertex.newVertexFromLibrary(6,
                BBType.FRAGMENT, fs);
        graph.addVertex(v6a_bis);
        Vertex v6a_tris = Vertex.newVertexFromLibrary(6,
                BBType.FRAGMENT, fs);
        graph.addVertex(v6a_tris);

        Vertex v1a_2 = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v1a_2);
        Vertex v1a_2_bis = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v1a_2_bis);
        Vertex v1a_2_tris = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v1a_2_tris);
        
        List<Vertex> vToCap = new ArrayList<Vertex>();
        vToCap.add(v1a);
        vToCap.add(v1a_2);
        vToCap.add(v1a_2_bis);
        vToCap.add(v1a_2_tris);
        for (Vertex v : vToCap)
        {
            for (AttachmentPoint ap : v.getAttachmentPoints())
            {
                if (ap.isAvailable())
                {
                    Vertex cap =  Vertex.newVertexFromLibrary(0,
                            BBType.CAP, fs);
                    graph.appendVertexOnAP(ap, cap.getAP(0));
                }
            }
        }
        
        graph.addEdge(new Edge(v1a.getAP(1), v6a.getAP(0)));
        graph.addEdge(new Edge(v1a.getAP(3), v6a_bis.getAP(0)));
        graph.addEdge(new Edge(v1a.getAP(4), v6a_tris.getAP(0)));
        graph.addEdge(new Edge(v6a.getAP(1), v1a_2.getAP(1)));
        graph.addEdge(new Edge(v6a_bis.getAP(1), v1a_2_bis.getAP(1)));
        graph.addEdge(new Edge(v6a_tris.getAP(1), v1a_2_tris.getAP(1)));
        
        ArrayList<Integer> symA = new ArrayList<Integer>();
        symA.add(v6a.getVertexId());
        symA.add(v6a_bis.getVertexId());
        graph.addSymmetricSetOfVertices(new SymmetricSet(symA));
        
        ArrayList<Integer> symB = new ArrayList<Integer>();
        symB.add(v1a_2.getVertexId());
        symB.add(v1a_2_bis.getVertexId());
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
    private DGraph makeDisorderedGraph(FragmentSpace fs) 
            throws DENOPTIMException
    {
        DGraph graph = new DGraph();
        Vertex v1 = Vertex.newVertexFromLibrary(3,
                BBType.FRAGMENT, fs);
        graph.addVertex(v1);
        Vertex v2 = Vertex.newVertexFromLibrary(3,
                BBType.FRAGMENT, fs);
        graph.addVertex(v2);
        Vertex v3 = Vertex.newVertexFromLibrary(3,
                BBType.FRAGMENT, fs);
        graph.addVertex(v3);
        Vertex v4 = Vertex.newVertexFromLibrary(3,
                BBType.FRAGMENT, fs);
        graph.addVertex(v4);
        Vertex v5 = Vertex.newVertexFromLibrary(3,
                BBType.FRAGMENT, fs);
        graph.addVertex(v5);
        
        graph.addEdge(new Edge(v4.getAP(0), v1.getAP(0)));
        graph.addEdge(new Edge(v3.getAP(1), v4.getAP(1)));
        graph.addEdge(new Edge(v5.getAP(0), v3.getAP(0)));
        graph.addEdge(new Edge(v1.getAP(1), v2.getAP(0)));
        return graph;
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns a graph that contains a 10-layered recursive structure.
     * Before running this you must run 
     * {@link DGraphTest#prepare()}
     */
    public static DGraph makeDeeplyEmbeddedGraph() throws DENOPTIMException
    {
        EmptyVertex vOut = new EmptyVertex(0);
        vOut.addAP(APCA);

        DGraph gOut = new DGraph();
        gOut.addVertex(vOut);
        
        DGraph refToPrevGraph = gOut;
        Vertex refToPRevVrtx = vOut;
        for (int embeddingLevel=0; embeddingLevel<10; embeddingLevel++)
        {
            EmptyVertex v0 = new EmptyVertex(0+100*embeddingLevel);
            v0.addAP(APCA);
            v0.addAP(APCB);
            v0.addAP(APCC);
            EmptyVertex v1 = new EmptyVertex(1+100*embeddingLevel);
            v1.addAP(APCB);
            v1.addAP(APCA);
            v1.addAP(APCD);
            EmptyVertex v2 = new EmptyVertex(2+100*embeddingLevel);
            v2.addAP(APCB);
            DGraph g = new DGraph();
            g.addVertex(v0);
            g.appendVertexOnAP(v0.getAP(1), v1.getAP(0));
            g.appendVertexOnAP(v1.getAP(1), v2.getAP(0));
            Template t = new Template(BBType.UNDEFINED);
            t.setInnerGraph(g);
            
            refToPrevGraph.appendVertexOnAP(refToPRevVrtx.getAP(0), t.getAP(1));
            
            refToPrevGraph = g;
            refToPRevVrtx = v0;
        }
        return gOut;
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetEmbeddingPath() throws Exception
    {
        prepare();
        DGraph gOut = makeDeeplyEmbeddedGraph();
        
        List<Template> expected = new ArrayList<Template>();
        DGraph refToThisLayerGraph = gOut;
        Template refToThisLayerVrtx = null;
        for (int embeddingLevel=0; embeddingLevel<10; embeddingLevel++)
        {
            refToThisLayerVrtx = (Template) refToThisLayerGraph
                    .getVertexList().stream()
                        .filter(v -> v instanceof Template)
                        .findAny()
                        .orElse(null);
            expected.add(refToThisLayerVrtx);
            refToThisLayerGraph = refToThisLayerVrtx.getInnerGraph();
        }
        
        assertEquals(new ArrayList<Template>(),gOut.getEmbeddingPath(),
                "Embedding path of graph that is not embedded");
        List<Template> path = refToThisLayerGraph.getEmbeddingPath();
        assertEquals(expected, path, "Path of deepest embedded graph");
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetEmbeddedGraphInClone() throws Exception
    {
        prepare();
        DGraph gA = makeDeeplyEmbeddedGraph();
        DGraph gB = gA.clone();
        
        // This works only because we know that the graphs  have only one
        // template per level
        List<Template> expectedPathA = new ArrayList<Template>();
        List<Template> expectedPathB = new ArrayList<Template>();
        DGraph refToThisLayerGraphB = gB;
        Template refToThisLayerVrtxB = null;
        DGraph refToThisLayerGraphA = gA;
        Template refToThisLayerVrtxA = null;
        for (int embeddingLevel=0; embeddingLevel<9; embeddingLevel++)
        {
            refToThisLayerVrtxA = (Template) refToThisLayerGraphA
                    .getVertexList().stream()
                        .filter(v -> v instanceof Template)
                        .findAny()
                        .orElse(null);
            expectedPathA.add(refToThisLayerVrtxA);
            refToThisLayerGraphA = refToThisLayerVrtxA.getInnerGraph();
            
            refToThisLayerVrtxB = (Template) refToThisLayerGraphB
                    .getVertexList().stream()
                        .filter(v -> v instanceof Template)
                        .findAny()
                        .orElse(null);
            expectedPathB.add(refToThisLayerVrtxB);
            refToThisLayerGraphB = refToThisLayerVrtxB.getInnerGraph();
        }

        DGraph expectedEmbeddedA = expectedPathA.get(8).getInnerGraph();
        DGraph expectedEmbeddedB = expectedPathB.get(8).getInnerGraph();
        
        DGraph embeddedFoundB = DGraph.getEmbeddedGraphInClone(gB, 
                gA, expectedPathA);
        
        assertEquals(expectedEmbeddedB,embeddedFoundB);
        
        List<Template> pathFoundB = embeddedFoundB.getEmbeddingPath();
        assertEquals(expectedPathB,pathFoundB);
        
        DGraph embeddedFoundA = DGraph.getEmbeddedGraphInClone(gA, 
                gB, expectedPathB);
        assertEquals(expectedEmbeddedA,embeddedFoundA);
        
        List<Template> pathFoundA = embeddedFoundA.getEmbeddingPath();
        assertEquals(expectedPathA,pathFoundA);
    }

//------------------------------------------------------------------------------
    
    @Test
    public void testGetSourceVertex() throws Exception
    {
        FragmentSpace fs = prepare();
        DGraph g = makeDisorderedGraph(fs);
        Vertex src = g.getSourceVertex();
        assertEquals(g.getVertexAtPosition(4),src,
                "Inconsistent source vertex");

        DGraph g2 = makeTestGraphC(fs);
        Vertex src2 = g2.getSourceVertex();
        assertEquals(g2.getVertexAtPosition(0),src2,
                "Inconsistent source vertex");
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetLevel() throws Exception
    {
        FragmentSpace fs = prepare();
        DGraph g = makeDisorderedGraph(fs);
        int[] expected = new int[] {2,3,0,1,-1};
        for (int i=0; i<g.getVertexCount(); i++)
        {
            assertEquals(expected[i],g.getLevel(g.getVertexAtPosition(i)),
                    "Wrong level for vertex at position "+i);
        }
        
        DGraph g2 = makeTestGraphC(fs);
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
        FragmentSpace fs = prepare();
        DGraph g = makeTestGraphB(fs);
        
        Vertex v1 = g.getVertexAtPosition(1);
        
        LinkedHashMap<Integer, Integer> apMap = 
                new LinkedHashMap<Integer,Integer>();
        apMap.put(0, 4); 
        apMap.put(1, 1);
        apMap.put(3, 2);
        apMap.put(4, 0);
        
        int chosenBBId = 4;
        BBType choosenBBTyp = BBType.FRAGMENT;
        
        boolean res = g.replaceVertex(v1, chosenBBId, choosenBBTyp, apMap, fs);
        
        assertTrue(res,"ReplaceVertex return value.");
        assertFalse(g.containsVertex(v1),"v1 is still part of graph");
        int numVertexesWithGoodBBId = 0;
        int numEdgesWithS = 0;
        int numEdgesWith2 = 0;
        int numEdgesWith3 = 0;
        for (Vertex v : g.gVertices)
        {
            if (v.getBuildingBlockType() == choosenBBTyp 
                    && v.getBuildingBlockId() == chosenBBId)
            {
                numVertexesWithGoodBBId++;
                
                for (AttachmentPoint ap : v.getAttachmentPoints())
                {
                    if (!ap.isAvailable())
                    {
                        Vertex nextVrtx = ap.getLinkedAP().getOwner();
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
        
        //
        // 
        //
        DGraph g2 = makeTestGraphB(fs);
        
        Vertex v2 = g2.getVertexAtPosition(2);
        
        SymmetricSet origSS = g2.getSymSetForVertex(v2).clone();
        Set<Integer> oldVertexIds = new HashSet<Integer>();
        for (Vertex v : g2.getVertexList())
            oldVertexIds.add(v.getVertexId());
        
        LinkedHashMap<Integer,Integer> apMap2 = 
                new LinkedHashMap<Integer,Integer>();
        apMap2.put(0, 1);
        
        int chosenBBId2 = 5;
        BBType choosenBBTyp2 = BBType.FRAGMENT;
        
        boolean res2 = g2.replaceVertex(v2, chosenBBId2, choosenBBTyp2, apMap2, 
                fs);
        
        assertTrue(res2,"ReplaceVertex return value (2).");
        assertFalse(g2.containsVertex(v2),"v2 is still part of graph");
        int numVertexesWithGoodBBId2 = 0;
        int numEdgesWith1 = 0;
        for (Vertex v : g2.gVertices)
        {
            if (v.getBuildingBlockType() == choosenBBTyp2 
                    && v.getBuildingBlockId() == chosenBBId2)
            {
                numVertexesWithGoodBBId2++;
                
                for (AttachmentPoint ap : v.getAttachmentPoints())
                {
                    if (!ap.isAvailable())
                    {
                        Vertex nextVrtx = ap.getLinkedAP().getOwner();
                        if (nextVrtx.getBuildingBlockId() == 1)
                            numEdgesWith1++;
                    }
                }
            }
        }
        assertEquals(4,numVertexesWithGoodBBId2,"Number of new links.");
        assertEquals(4,numEdgesWith1,"Number of new edges with scaffold.");
        assertEquals(2,g2.getSymmetricSetCount(),"Number of symmetric sets.");
        boolean found = false;
        boolean foundOldVertexId = false;
        Iterator<SymmetricSet> iterSS = g2.getSymSetsIterator();
        while (iterSS.hasNext())
        {
            SymmetricSet ss = iterSS.next();
            if (ss.size() == origSS.size())
            {
                found = true;
                for (Integer vid : ss.getList())
                {
                    if (oldVertexIds.contains(vid))
                        foundOldVertexId = true;
                }
            }
        }
        assertTrue(found,"could not find old symmetric set");
        assertFalse(foundOldVertexId,"found old vertex id in new symmetric set");
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testReplaceVertex_inRing() throws Exception
    {
        FragmentSpace fs = prepare();
        DGraph g = makeTestGraphD(fs);
        
        Vertex v = g.getVertexAtPosition(1);
        
        LinkedHashMap<Integer, Integer> apMap = 
                new LinkedHashMap<Integer,Integer>();
        apMap.put(0, 0); 
        apMap.put(1, 1);
        apMap.put(2, 2);
        apMap.put(3, 3);
        apMap.put(4, 4);
        
        boolean res = g.replaceVertex(v, 1, BBType.FRAGMENT, apMap, fs);
        assertTrue(res);
        
        DGraph g2 = makeTestGraphD(fs);
        assertTrue(g.isIsomorphicTo(g2));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testReplaceSubGraph() throws Exception
    {
        FragmentSpace fs = prepare();
        DGraph g = makeTestGraphD(fs);
        
        List<Vertex> vrtxsToReplace = new ArrayList<Vertex>();
        vrtxsToReplace.add(g.getVertexAtPosition(0));
        vrtxsToReplace.add(g.getVertexAtPosition(1));
        
        DGraph incomingSubGraph = makeTestGraphF(fs);
        
        LinkedHashMap<AttachmentPoint, AttachmentPoint> apMap = 
                new LinkedHashMap<AttachmentPoint,AttachmentPoint>();
        apMap.put(g.getVertexAtPosition(0).getAP(1), //A:0
                incomingSubGraph.getVertexAtPosition(0).getAP(2)); //A:0
        apMap.put(g.getVertexAtPosition(0).getAP(2), //D:0
                incomingSubGraph.getVertexAtPosition(7).getAP(1)); //D:0
        apMap.put(g.getVertexAtPosition(1).getAP(1), //B:0
                incomingSubGraph.getVertexAtPosition(6).getAP(2)); //B:0
        apMap.put(g.getVertexAtPosition(1).getAP(4), //C:0
                incomingSubGraph.getVertexAtPosition(6).getAP(4)); //C:0
        apMap.put(g.getVertexAtPosition(1).getAP(3), //B:0
                incomingSubGraph.getVertexAtPosition(5).getAP(3)); //B:0
        incomingSubGraph.reassignSymmetricLabels();
        
        boolean res = g.replaceSingleSubGraph(vrtxsToReplace, incomingSubGraph, apMap);
        assertTrue(res);
        
        g.convertSymmetricLabelsToSymmetricSets();
        
        assertEquals(3,g.getRingCount(),"number of rings");
        assertEquals(3,g.getSymmetricSetCount(),"number of symmetric sets");
        assertEquals(incomingSubGraph.getVertexAtPosition(0),g.getSourceVertex(),
                "graph's surce vertex");
        //NB: the position of the vertexes has changes by -2. so vertex 3 is now at 1
        PathSubGraph pathA = new PathSubGraph(g.getVertexAtPosition(1),
                g.getVertexAtPosition(2), g); // length was 2
        assertEquals(3,pathA.getEdgesPath().size(),"path within a ring (A)");
        PathSubGraph pathB = new PathSubGraph(g.getVertexAtPosition(3),
                g.getVertexAtPosition(6), g); // length was 4
        assertEquals(5,pathB.getEdgesPath().size(),"path within a ring (B)");
        PathSubGraph pathC = new PathSubGraph(g.getVertexAtPosition(7),
                g.getVertexAtPosition(6), g); // length was 2
        assertEquals(4,pathC.getEdgesPath().size(),"path within a ring (C)");
        
        DGraph expected = makeTestGraphG(fs);
        assertTrue(g.isIsomorphicTo(expected),"isomforphic to expected");
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testReplaceSubGraph_inTemplate() throws Exception
    {
        FragmentSpace fs = prepare();
        DGraph g = makeTestGraphI(fs);
        
        DGraph innerGraph = ((Template) g.getVertexAtPosition(0))
                .getInnerGraph();
        
        List<Vertex> vrtxsToReplace = new ArrayList<Vertex>();
        vrtxsToReplace.add(innerGraph.getVertexAtPosition(0));
        
        DGraph incomingSubGraph = makeTestGraphJ(fs);
        
        LinkedHashMap<AttachmentPoint, AttachmentPoint> apMap = 
                new LinkedHashMap<AttachmentPoint,AttachmentPoint>();
        // first two are those needed within the template
        apMap.put(vrtxsToReplace.get(0).getAP(3), //B:0
                incomingSubGraph.getVertexAtPosition(0).getAP(1)); //B:0
        apMap.put(vrtxsToReplace.get(0).getAP(4), //C:0
                incomingSubGraph.getVertexAtPosition(1).getAP(4)); //C:0
        // second two are those projected and used outside template
        apMap.put(vrtxsToReplace.get(0).getAP(0), //A:0
                incomingSubGraph.getVertexAtPosition(0).getAP(2)); //A:0
        apMap.put(vrtxsToReplace.get(0).getAP(1), //B:0
                incomingSubGraph.getVertexAtPosition(0).getAP(3)); //B:0

        boolean res = innerGraph.replaceSingleSubGraph(vrtxsToReplace, 
                incomingSubGraph, apMap);
        assertTrue(res);
        
        assertEquals(5,g.getVertexCount(),"Vertex in outer graph");
        assertEquals(1,g.getRingCount(),"Rings in outer graph");
        assertEquals(4,g.getAvailableAPs().size(),"Free APs outer graph");
        Ring r = g.getRings().get(0);
        assertEquals(4,r.getDistance(r.getHeadVertex(),r.getTailVertex()),
                "Distance Head-Tail in ring of outer graph");
        
        DGraph innerGraphAfter = 
                ((Template) g.getVertexAtPosition(0)).getInnerGraph();
        assertEquals(6,innerGraphAfter.getVertexCount(),"Vertex in inner graph");
        assertEquals(1,innerGraphAfter.getRingCount(),"Rings in inner graph");
        assertEquals(6,innerGraphAfter.getAvailableAPs().size(),"Free APs inner graph");
        Ring ri = innerGraphAfter.getRings().get(0);
        assertEquals(5,ri.getDistance(ri.getHeadVertex(),ri.getTailVertex()),
                "Distance Head-Tail in ring of inner graph");
    }
    
//------------------------------------------------------------------------------
	
    @Test
	public void testRemoveVertex() throws Exception {
		DGraph graph = new DGraph();
		EmptyVertex v0 = new EmptyVertex(0);
		buildVertexAndConnectToGraph(v0, 3, graph);

		EmptyVertex v1 = new EmptyVertex(1);
		buildVertexAndConnectToGraph(v1, 2, graph);
		graph.addEdge(new Edge(v0.getAP(0), v1.getAP(0)));

		EmptyVertex v2 = new EmptyVertex(2);
		buildVertexAndConnectToGraph(v2, 2, graph);
		graph.addEdge(new Edge(v1.getAP(1), v2.getAP(0)));

		EmptyVertex v3 = new EmptyVertex(3);
		buildVertexAndConnectToGraph(v3, 1, graph);
		graph.addEdge(new Edge(v2.getAP(1), v3.getAP(0)));

		EmptyVertex v4 = new EmptyVertex(4);
		buildVertexAndConnectToGraph(v4, 3, graph);
		graph.addEdge(new Edge(v0.getAP(1), v4.getAP(0)));

		EmptyVertex v5 = new EmptyVertex(5);
		buildVertexAndConnectToGraph(v5, 1, graph);
		graph.addEdge(new Edge(v4.getAP(1), v5.getAP(0)));

		EmptyVertex v6 = new EmptyVertex(6);
		buildVertexAndConnectToGraph(v6, 1, graph);
		graph.addEdge(new Edge(v0.getAP(2), v6.getAP(0)));

		EmptyVertex v7 = new EmptyVertex(7);
		buildVertexAndConnectToGraph(v7, 1, graph);
		graph.addEdge(new Edge(v4.getAP(2), v7.getAP(0)));

		graph.addRing(new Ring(new ArrayList<>(
				Arrays.asList(v5, v4, v0, v1, v2, v3))));

		graph.addRing(new Ring(new ArrayList<>(
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
		DGraph graphA = new DGraph();
		EmptyVertex v0 = new EmptyVertex(0);
		buildVertexAndConnectToGraph(v0, 3, graphA);

		EmptyVertex v1 = new EmptyVertex(1);
		buildVertexAndConnectToGraph(v1, 2, graphA);
		graphA.addEdge(new Edge(v0.getAP(0), v1.getAP(0)));

		EmptyVertex v2 = new EmptyVertex(2);
		buildVertexAndConnectToGraph(v2, 2, graphA);
		graphA.addEdge(new Edge(v1.getAP(1), v2.getAP(0)));

		// Other graph, but is the same graph

		DGraph graphB = new DGraph();
		EmptyVertex v90 = new EmptyVertex(90);
		buildVertexAndConnectToGraph(v90, 3, graphB);

		EmptyVertex v91 = new EmptyVertex(91);
		buildVertexAndConnectToGraph(v91, 2, graphB);
		graphB.addEdge(new Edge(v90.getAP(0), v91.getAP(0)));

		EmptyVertex v92 = new EmptyVertex(92);
		buildVertexAndConnectToGraph(v92, 2, graphB);
		graphB.addEdge(new Edge(v91.getAP(1), v92.getAP(0)));

		StringBuilder reason = new StringBuilder();
		assertTrue(graphA.sameAs(graphB, reason), reason.toString());
	}

//------------------------------------------------------------------------------

	@Test
	public void testSameAs_DiffVertex() throws DENOPTIMException
	{
		DGraph graphA = new DGraph();
		EmptyVertex v0 = new EmptyVertex(0);
		buildVertexAndConnectToGraph(v0, 3, graphA);

		EmptyVertex v1 = new EmptyVertex(1);
		buildVertexAndConnectToGraph(v1, 2, graphA);
		graphA.addEdge(new Edge(v0.getAP(0), v1.getAP(0)));

		EmptyVertex v2 = new EmptyVertex(2);
		buildVertexAndConnectToGraph(v2, 2, graphA);
		graphA.addEdge(new Edge(v1.getAP(1), v2.getAP(0)));

		// Other graph

		DGraph graphB = new DGraph();
		EmptyVertex v90 = new EmptyVertex(90);
		buildVertexAndConnectToGraph(v90, 3, graphB);

		EmptyVertex v91 = new EmptyVertex(91);
		buildVertexAndConnectToGraph(v91, 2, graphB);
		graphB.addEdge(new Edge(v90.getAP(0), v91.getAP(0)));

		EmptyVertex v92 = new EmptyVertex(92);
		buildVertexAndConnectToGraph(v92, 3, graphB);
		graphB.addEdge(new Edge(v91.getAP(1), v92.getAP(0)));

		StringBuilder reason = new StringBuilder();
		assertFalse(graphA.sameAs(graphB, reason));
	}

//------------------------------------------------------------------------------

	@Test
	public void testSameAs_SameSymmSet() throws Exception {
		DGraph graphA = new DGraph();
		EmptyVertex v0 = new EmptyVertex(0);
		buildVertexAndConnectToGraph(v0, 4, graphA);

		EmptyVertex v1 = new EmptyVertex(1);
		buildVertexAndConnectToGraph(v1, 2, graphA);
		graphA.addEdge(new Edge(v0.getAP(0), v1.getAP(0)));

		EmptyVertex v2 = new EmptyVertex(2);
		buildVertexAndConnectToGraph(v2, 2, graphA);
		graphA.addEdge(new Edge(v0.getAP(1), v2.getAP(0)));

		EmptyVertex v3 = new EmptyVertex(3);
		buildVertexAndConnectToGraph(v3, 2, graphA);
		graphA.addEdge(new Edge(v0.getAP(2), v3.getAP(0)));

		EmptyVertex v4 = new EmptyVertex(4);
		buildVertexAndConnectToGraph(v4, 2, graphA);
		graphA.addEdge(new Edge(v0.getAP(3), v4.getAP(0)));

		SymmetricSet ssA = new SymmetricSet();
		ssA.add(1);
		ssA.add(2);
		graphA.addSymmetricSetOfVertices(ssA);
		SymmetricSet ssA2 = new SymmetricSet();
		ssA2.add(3);
		ssA2.add(4);
		graphA.addSymmetricSetOfVertices(ssA2);

		// Other

		DGraph graphB = new DGraph();
		EmptyVertex v90 = new EmptyVertex(90);
		buildVertexAndConnectToGraph(v90, 4, graphB);

		EmptyVertex v91 = new EmptyVertex(91);
		buildVertexAndConnectToGraph(v91, 2, graphB);
		graphB.addEdge(new Edge(v90.getAP(0), v91.getAP(0)));

		EmptyVertex v92 = new EmptyVertex(92);
		buildVertexAndConnectToGraph(v92, 2, graphB);
		graphB.addEdge(new Edge(v90.getAP(1), v92.getAP(0)));

		EmptyVertex v93 = new EmptyVertex(93);
		buildVertexAndConnectToGraph(v93, 2, graphB);
		graphB.addEdge(new Edge(v90.getAP(2), v93.getAP(0)));

		EmptyVertex v94 = new EmptyVertex(94);
		buildVertexAndConnectToGraph(v94, 2, graphB);
		graphB.addEdge(new Edge(v90.getAP(3), v94.getAP(0)));

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
		DGraph graphA = new DGraph();
		EmptyVertex v0 = new EmptyVertex(0);
		buildVertexAndConnectToGraph(v0, 4, graphA);

		EmptyVertex v1 = new EmptyVertex(1);
		buildVertexAndConnectToGraph(v1, 2, graphA);
		graphA.addEdge(new Edge(v0.getAP(0), v1.getAP(0)));

		EmptyVertex v2 = new EmptyVertex(2);
		buildVertexAndConnectToGraph(v2, 2, graphA);
		graphA.addEdge(new Edge(v0.getAP(1), v2.getAP(0)));

		EmptyVertex v3 = new EmptyVertex(3);
		buildVertexAndConnectToGraph(v3, 2, graphA);
		graphA.addEdge(new Edge(v0.getAP(2), v3.getAP(0)));

		EmptyVertex v4 = new EmptyVertex(4);
		buildVertexAndConnectToGraph(v4, 2, graphA);
		graphA.addEdge(new Edge(v0.getAP(3), v4.getAP(0)));

		SymmetricSet ssA = new SymmetricSet();
		ssA.add(1);                            //difference
		ssA.add(2);                            //difference
		graphA.addSymmetricSetOfVertices(ssA);
		SymmetricSet ssA2 = new SymmetricSet();
		ssA2.add(3);                           //difference
		ssA2.add(4);                           //difference
		graphA.addSymmetricSetOfVertices(ssA2);

		// Other

		DGraph graphB = new DGraph();
		EmptyVertex v90 = new EmptyVertex(90);
		buildVertexAndConnectToGraph(v90, 4, graphB);

		EmptyVertex v91 = new EmptyVertex(91);
		buildVertexAndConnectToGraph(v91, 2, graphB);
		graphB.addEdge(new Edge(v90.getAP(0), v1.getAP(0)));

		EmptyVertex v92 = new EmptyVertex(92);
		buildVertexAndConnectToGraph(v92, 2, graphB);
		graphB.addEdge(new Edge(v0.getAP(1), v2.getAP(0)));

		EmptyVertex v93 = new EmptyVertex(93);
		buildVertexAndConnectToGraph(v93, 2, graphB);
		graphB.addEdge(new Edge(v0.getAP(2), v3.getAP(0)));

		EmptyVertex v94 = new EmptyVertex(94);
		buildVertexAndConnectToGraph(v94, 2, graphB);
		graphB.addEdge(new Edge(v0.getAP(3), v4.getAP(0)));

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
		DGraph graphA = new DGraph();
		EmptyVertex v0 = new EmptyVertex(0);
		buildVertexAndConnectToGraph(v0, 4, graphA);

		EmptyVertex v1 = new EmptyVertex(1);
		buildVertexAndConnectToGraph(v1, 2, graphA);
		graphA.addEdge(new Edge(v0.getAP(0), v1.getAP(0)));

		EmptyVertex v2 = new EmptyVertex(2);
		buildVertexAndConnectToGraph(v2, 2, graphA);
		graphA.addEdge(new Edge(v0.getAP(1), v2.getAP(0)));

		EmptyVertex v3 = new EmptyVertex(3);
		buildVertexAndConnectToGraph(v3, 2, graphA);
		graphA.addEdge(new Edge(v0.getAP(2), v3.getAP(0)));

		EmptyVertex v4 = new EmptyVertex(4);
		buildVertexAndConnectToGraph(v4, 2, graphA);
		graphA.addEdge(new Edge(v0.getAP(3), v4.getAP(0)));

		ArrayList<Vertex> vrA = new ArrayList<Vertex>();
		vrA.add(v1);
		vrA.add(v0);
		vrA.add(v2);
		Ring rA = new Ring(vrA);
		graphA.addRing(rA);
		ArrayList<Vertex> vrA2 = new ArrayList<Vertex>();
		vrA2.add(v3);
		vrA2.add(v0);
		vrA2.add(v4);
		Ring rA2 = new Ring(vrA2);
		graphA.addRing(rA2);


		// Other

		DGraph graphB = new DGraph();
		EmptyVertex v90 = new EmptyVertex(90);
		buildVertexAndConnectToGraph(v90, 4, graphB);

		EmptyVertex v91 = new EmptyVertex(91);
		buildVertexAndConnectToGraph(v91, 2, graphB);
		graphB.addEdge(new Edge(v90.getAP(0), v91.getAP(0)));

		EmptyVertex v92 = new EmptyVertex(92);
		buildVertexAndConnectToGraph(v92, 2, graphB);
		graphB.addEdge(new Edge(v90.getAP(1), v92.getAP(0)));

		EmptyVertex v93 = new EmptyVertex(93);
		buildVertexAndConnectToGraph(v93, 2, graphB);
		graphB.addEdge(new Edge(v90.getAP(2), v93.getAP(0)));

		EmptyVertex v94 = new EmptyVertex(94);
		buildVertexAndConnectToGraph(v94, 2, graphB);
		graphB.addEdge(new Edge(v90.getAP(3), v94.getAP(0)));

		ArrayList<Vertex> vrB = new ArrayList<Vertex>();
		vrB.add(v91);
		vrB.add(v90);
		vrB.add(v92);
		Ring rB = new Ring(vrB);
		graphB.addRing(rB);
		ArrayList<Vertex> vrB2 = new ArrayList<Vertex>();
		vrB2.add(v93);
		vrB2.add(v90);
		vrB2.add(v94);
		Ring rB2 = new Ring(vrB2);
		graphB.addRing(rB2);

		StringBuilder reason = new StringBuilder();
		assertTrue(graphA.sameAs(graphB, reason));
	}

//------------------------------------------------------------------------------

	@Test
	public void testSameAs_DisorderRings() throws Exception 
	{
		DGraph graphA = new DGraph();
		EmptyVertex v0 = new EmptyVertex(0);
		buildVertexAndConnectToGraph(v0, 4, graphA);

		EmptyVertex v1 = new EmptyVertex(1);
		buildVertexAndConnectToGraph(v1, 2, graphA);
		graphA.addEdge(new Edge(v0.getAP(0), v1.getAP(0)));

		EmptyVertex v2 = new EmptyVertex(2);
		buildVertexAndConnectToGraph(v2, 2, graphA);
		graphA.addEdge(new Edge(v0.getAP(1), v2.getAP(0)));

		EmptyVertex v3 = new EmptyVertex(3);
		buildVertexAndConnectToGraph(v3, 2, graphA);
		graphA.addEdge(new Edge(v0.getAP(2), v3.getAP(0)));

		EmptyVertex v4 = new EmptyVertex(4);
		buildVertexAndConnectToGraph(v4, 2, graphA);
		graphA.addEdge(new Edge(v0.getAP(3), v4.getAP(0)));

		ArrayList<Vertex> vrA = new ArrayList<>();
		vrA.add(v1);
		vrA.add(v0);
		vrA.add(v2);
		Ring rA = new Ring(vrA);
		graphA.addRing(rA);
		ArrayList<Vertex> vrA2 = new ArrayList<>();
		vrA2.add(v3);
		vrA2.add(v0);
		vrA2.add(v4);
		Ring rA2 = new Ring(vrA2);
		graphA.addRing(rA2);


		// Other

		DGraph graphB = new DGraph();
		EmptyVertex v90 = new EmptyVertex(90);
		buildVertexAndConnectToGraph(v90, 4, graphB);

		EmptyVertex v91 = new EmptyVertex(91);
		buildVertexAndConnectToGraph(v91, 2, graphB);
		graphB.addEdge(new Edge(v90.getAP(0), v91.getAP(0)));

		EmptyVertex v92 = new EmptyVertex(92);
		buildVertexAndConnectToGraph(v92, 2, graphB);
		graphB.addEdge(new Edge(v90.getAP(1), v92.getAP(0)));

		EmptyVertex v93 = new EmptyVertex(93);
		buildVertexAndConnectToGraph(v93, 2, graphB);
		graphB.addEdge(new Edge(v90.getAP(2), v93.getAP(0)));

		EmptyVertex v94 = new EmptyVertex(94);
		buildVertexAndConnectToGraph(v94, 2, graphB);
		graphB.addEdge(new Edge(v90.getAP(3), v94.getAP(0)));

		ArrayList<Vertex> vrB = new ArrayList<>();
		vrB.add(v91);
		vrB.add(v90);
		vrB.add(v92);
		Ring rB = new Ring(vrB);
		graphB.addRing(rB);
		ArrayList<Vertex> vrB2 = new ArrayList<>();
		vrB2.add(v94);
		vrB2.add(v90);
		vrB2.add(v93);
		Ring rB2 = new Ring(vrB2);
		graphB.addRing(rB2);

		StringBuilder reason = new StringBuilder();
		assertTrue(graphA.sameAs(graphB, reason));
	}

//------------------------------------------------------------------------------

	@Test
	public void testSameAs_DiffRings() throws DENOPTIMException
	{
		DGraph graphA = new DGraph();
		EmptyVertex v0 = new EmptyVertex(0);
		buildVertexAndConnectToGraph(v0, 4, graphA);

		EmptyVertex v1 = new EmptyVertex(1);
		buildVertexAndConnectToGraph(v1, 2, graphA);
		graphA.addEdge(new Edge(v0.getAP(0), v1.getAP(0)));

		EmptyVertex v2 = new EmptyVertex(2);
		buildVertexAndConnectToGraph(v2, 2, graphA);
		graphA.addEdge(new Edge(v0.getAP(1), v2.getAP(0)));

		EmptyVertex v3 = new EmptyVertex(3);
		buildVertexAndConnectToGraph(v3, 2, graphA);
		graphA.addEdge(new Edge(v0.getAP(2), v3.getAP(0)));

		EmptyVertex v4 = new EmptyVertex(4);
		buildVertexAndConnectToGraph(v4, 2, graphA);
		graphA.addEdge(new Edge(v0.getAP(3), v4.getAP(0)));

		ArrayList<Vertex> vrA = new ArrayList<>();
		vrA.add(v1);
		vrA.add(v0);
		vrA.add(v2);
		Ring rA = new Ring(vrA);
		graphA.addRing(rA);
		ArrayList<Vertex> vrA2 = new ArrayList<>();
		vrA2.add(v3);
		vrA2.add(v0);
		vrA2.add(v4);
		Ring rA2 = new Ring(vrA2);
		graphA.addRing(rA2);

		// Other
		DGraph graphB = new DGraph();
		EmptyVertex v90 = new EmptyVertex(90);
		buildVertexAndConnectToGraph(v90, 4, graphB);

		EmptyVertex v91 = new EmptyVertex(91);
		buildVertexAndConnectToGraph(v91, 2, graphB);
		graphB.addEdge(new Edge(v90.getAP(0), v91.getAP(0)));

		EmptyVertex v92 = new EmptyVertex(92);
		buildVertexAndConnectToGraph(v92, 2, graphB);
		graphB.addEdge(new Edge(v90.getAP(1), v92.getAP(0)));

		EmptyVertex v93 = new EmptyVertex(93);
		buildVertexAndConnectToGraph(v93, 2, graphB);
		graphB.addEdge(new Edge(v90.getAP(2), v93.getAP(0)));

		EmptyVertex v94 = new EmptyVertex(94);
		buildVertexAndConnectToGraph(v94, 2, graphB);
		graphB.addEdge(new Edge(v90.getAP(3), v94.getAP(0)));

		ArrayList<Vertex> vrB = new ArrayList<>();
		vrB.add(v91);
		vrB.add(v90);
		vrB.add(v94);
		Ring rB = new Ring(vrB);
		graphB.addRing(rB);
		ArrayList<Vertex> vrB2 = new ArrayList<>();
		vrB2.add(v92);
		vrB2.add(v90);
		vrB2.add(v93);
		Ring rB2 = new Ring(vrB2);
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

		vertex0.addAP();
		vertex0.addAP();
		vertex1.addAP();

		Edge edge0 = new Edge(vertex0.getAP(0),
		        vertex1.getAP(0));

		DGraph graph = new DGraph();
		graph.addVertex(vertex0);
		graph.addVertex(vertex1);
		graph.addEdge(edge0);

		List<AttachmentPoint> lst = graph.getAvailableAPs();
		assertEquals(1,lst.size(), "Size of list");
		assertEquals(vertex0.getVertexId(),lst.get(0).getOwner().getVertexId(),
		        "ID of the vertex holding the available AP.");
	}

//------------------------------------------------------------------------------

	@Test
	public void testClone() throws DENOPTIMException {
		DGraph graph = new DGraph();
		EmptyVertex v0 = new EmptyVertex(0);
		buildVertexAndConnectToGraph(v0, 3, graph);

		EmptyVertex v1 = new EmptyVertex(1);
		buildVertexAndConnectToGraph(v1, 2, graph);
		graph.addEdge(new Edge(v0.getAP(0), v1.getAP(0)));

		EmptyVertex v2 = new EmptyVertex(2);
		buildVertexAndConnectToGraph(v2, 2, graph);
		graph.addEdge(new Edge(v1.getAP(1), v2.getAP(0)));

		EmptyVertex v3 = new EmptyVertex(3);
		buildVertexAndConnectToGraph(v3, 1, graph);
		graph.addEdge(new Edge(v2.getAP(1), v3.getAP(0)));

		EmptyVertex v4 = new EmptyVertex(4);
		buildVertexAndConnectToGraph(v4, 3, graph);
		graph.addEdge(new Edge(v0.getAP(1), v4.getAP(0)));

		EmptyVertex v5 = new EmptyVertex(5);
		buildVertexAndConnectToGraph(v5, 1, graph);
		graph.addEdge(new Edge(v4.getAP(1), v5.getAP(0)));

		EmptyVertex v6 = new EmptyVertex(6);
		buildVertexAndConnectToGraph(v6, 1, graph);
		graph.addEdge(new Edge(v0.getAP(2), v6.getAP(0)));

		EmptyVertex v7 = new EmptyVertex(7);
		buildVertexAndConnectToGraph(v7, 1, graph);
		graph.addEdge(new Edge(v4.getAP(2), v7.getAP(0)));

		graph.addRing(new Ring(new ArrayList<>(
				Arrays.asList(v5, v4, v0, v1, v2, v3))));

		graph.addRing(new Ring(new ArrayList<>(
				Arrays.asList(v6, v0, v4, v7))));

		graph.addSymmetricSetOfVertices(new SymmetricSet(
				new ArrayList<>(Arrays.asList(3, 5))));

		graph.addSymmetricSetOfVertices(new SymmetricSet(
				new ArrayList<>(Arrays.asList(6, 7))));
		
		DGraph clone = graph.clone();
        
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
		    Vertex vg = graph.getVertexAtPosition(iv);
		    Vertex vc = clone.getVertexAtPosition(iv);
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
		DGraph graph = new DGraph();
		Template tmpl = Template.getTestTemplate(
		        ContractLevel.FIXED);
		graph.addVertex(tmpl);

		assertEquals(1, graph.getMutableSites().size(),
				"Size of mutation list in case of frozen template");

		graph = new DGraph();
		tmpl = Template.getTestTemplate(ContractLevel.FREE);
		graph.addVertex(tmpl);

		assertEquals(2, graph.getMutableSites().size(),
				"Size of mutation list in case of free template");
		
        assertEquals(0, graph.getMutableSites(new ArrayList<>(
                Arrays.asList(MutationType.values()))).size(),
                "No sites if all is ignored");
	}
	
//------------------------------------------------------------------------------
    
    /**
     * Build a graph meant to be used in unit tests. The returned graph has
     * only the scaffold.
     * @return a new instance of the test graph.
     */
    private static DGraph makeTestGraph0(FragmentSpace fs) 
            throws Exception
    {
        DGraph graph = new DGraph();
        Vertex s = Vertex.newVertexFromLibrary(0,
                BBType.SCAFFOLD, fs);
        graph.addVertex(s);
        return graph;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Build a graph meant to be used in unit tests. The returned graph has
     * only the scaffold and one vertex, plus the edge
     * @return a new instance of the test graph.
     */
    private static DGraph makeTestGraph1(FragmentSpace fs) 
            throws Exception
    {
        DGraph graph = new DGraph();
        Vertex s = Vertex.newVertexFromLibrary(1,
                BBType.SCAFFOLD, fs);
        graph.addVertex(s);
        Vertex v1a = Vertex.newVertexFromLibrary(1,
                BBType.FRAGMENT, fs);
        graph.addVertex(v1a);

        graph.addEdge(new Edge(s.getAP(0), v1a.getAP(0)));
        
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
	public static DGraph makeTestGraphA() 
	{
        DGraph graph = new DGraph();
    
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
            
            Fragment v1 = new Fragment(1, iac1, BBType.SCAFFOLD);
            v1.addAP(0);
            v1.addAP(0);
            v1.addAP(0);
            v1.addAP(1);
        
            IAtomContainer iac2 = builder.newAtomContainer();
            iac2.addAtom(new Atom("O"));
            Fragment v2 = new Fragment(2, iac2, BBType.FRAGMENT);
            v2.addAP(0);
            v2.addAP(0);
        
            IAtomContainer iac3 = builder.newAtomContainer();
            iac3.addAtom(new Atom("H"));
            Fragment v3 = new Fragment(3, iac3, 
                    BBType.CAP);
            v3.addAP(0);
        
            IAtomContainer iac4 = builder.newAtomContainer();
            iac4.addAtom(new Atom("H"));
            Fragment v4 = new Fragment(4, iac4, 
                    BBType.CAP);
            v4.addAP(0);
        
            graph.addVertex(v1);
            graph.addVertex(v2);
            graph.addVertex(v3);
            graph.addVertex(v4);
            graph.addEdge(new Edge(v1.getAP(0), v2.getAP(0)));
            graph.addEdge(new Edge(v1.getAP(1), v3.getAP(0)));
            graph.addEdge(new Edge(v2.getAP(1), v4.getAP(0)));
            
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
    
    /**
     * Build a graph meant to be used in unit tests. 
     * The structure of the graph is the following:
     * <pre>
     *          [ap1-EV1-ap0]-[ap0-EV2]
     *         /
     *        ap2
     *       /
     * [C1-C0-ap0]-[ap0-O-ap1]-[ap0-H]
     *  |    \
     *  ap3   ap1
     *  |       \
     *  |        [ap0-H]
     *  [ap0-EV3] </pre>
     *  
     * Contrary to test graph A, this graph contains:
     * <ul>
     * <li>mixture of vertex types</li>
     * <li>APClasses</li>
     * </ul>
     * @return a new instance of the test graph.
     */
    public static DGraph makeTestGraphA2() 
    {
        DGraph graph = new DGraph();

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
            
            Fragment v1 = new Fragment(1, iac1, BBType.SCAFFOLD);
            v1.addAP(0, APClass.make(a,0));
            v1.addAP(0, APClass.make(b,1));
            v1.addAP(0, APClass.make(b,1));
            v1.addAP(1, APClass.make(a,0));
            v1.setBuildingBlockId(0);
        
            IAtomContainer iac2 = builder.newAtomContainer();
            iac2.addAtom(new Atom("O"));
            Fragment v2 = new Fragment(2, iac2, BBType.FRAGMENT);
            v2.addAP(0, APClass.make(b,1));
            v2.addAP(0, APClass.make(b,1));
            v2.setBuildingBlockId(1);
        
            IAtomContainer iac3 = builder.newAtomContainer();
            iac3.addAtom(new Atom("H"));
            Fragment v3 = new Fragment(3, iac3, BBType.CAP);
            v3.addAP(0, APClass.make(c,1));
            v3.setBuildingBlockId(2);
        
            IAtomContainer iac4 = builder.newAtomContainer();
            iac4.addAtom(new Atom("H"));
            Fragment v4 = new Fragment(4, iac4, BBType.CAP);
            v4.addAP(0, APClass.make(c,1));
            v4.setBuildingBlockId(3);
            
            EmptyVertex ev1 = new EmptyVertex();
            ev1.addAP(APClass.make(d,0));
            ev1.addAP(APClass.make(d,0));
            
            EmptyVertex ev2 = new EmptyVertex();
            ev2.addAP(APClass.make(d,0));
            ev2.addAP(APClass.make(d,0));
            
            EmptyVertex ev3 = new EmptyVertex();
            ev3.addAP(APClass.make(a,1));
            ev3.addAP(APClass.make(a,1));
        
            graph.addVertex(v1);
            graph.addVertex(v2);
            graph.addVertex(v3);
            graph.addVertex(v4);
            graph.addVertex(ev3); //These are disordered on purpose
            graph.addVertex(ev2);
            graph.addVertex(ev1);
            graph.addEdge(new Edge(v1.getAP(0), v2.getAP(0), 
                    BondType.TRIPLE));
            graph.addEdge(new Edge(v1.getAP(1), v3.getAP(0), 
                    BondType.SINGLE));
            graph.addEdge(new Edge(v2.getAP(1), v4.getAP(0)));
            graph.addEdge(new Edge(v1.getAP(2), ev1.getAP(1), 
                    BondType.SINGLE));
            graph.addEdge(new Edge(v1.getAP(3), ev3.getAP(0), 
                    BondType.NONE));
            graph.addEdge(new Edge(ev1.getAP(0), ev2.getAP(0), 
                    BondType.TRIPLE));
            
            // Use this just to verify identify of the graph
            /*
            System.out.println("WRITING TEST GRAPH A2 in /tmp/test_graph_A2.json");
            DenoptimIO.writeGraphToJSON(new File("/tmp/test_graph_A2.json"),
                    graph);
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
	public void testRemoveCapping() throws Exception 
	{
		DGraph graph = new DGraph();

		IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
		IAtomContainer iac1 = builder.newAtomContainer();
		iac1.addAtom(new Atom("C"));
		Fragment v1 = new Fragment(1, iac1, BBType.SCAFFOLD);
		v1.addAP(0);
		v1.addAP(0);

		IAtomContainer iac2 = builder.newAtomContainer();
		iac2.addAtom(new Atom("O"));
		Fragment v2 = new Fragment(2, iac2, BBType.FRAGMENT);
		v2.addAP(0);
		v2.addAP(0);

		IAtomContainer iac3 = builder.newAtomContainer();
		iac3.addAtom(new Atom("H"));
		Fragment v3 = new Fragment(3, iac3, BBType.CAP);
		v3.addAP(0);

		IAtomContainer iac4 = builder.newAtomContainer();
		iac4.addAtom(new Atom("H"));
		Fragment v4 = new Fragment(4, iac4, BBType.CAP);
		v4.addAP(0);

		graph.addVertex(v1);
		graph.addVertex(v2);
		graph.addVertex(v3);
		graph.addVertex(v4);
		graph.addEdge(new Edge(v1.getAP(0), v2.getAP(0)));
		graph.addEdge(new Edge(v1.getAP(1), v3.getAP(0)));
		graph.addEdge(new Edge(v2.getAP(1), v4.getAP(0)));

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

		DGraph graph2 = new DGraph();

		IAtomContainer iac12 = builder.newAtomContainer();
		iac12.addAtom(new Atom("C"));
		Fragment v21 = new Fragment(21, iac12, BBType.SCAFFOLD);
		v21.addAP(0);
		v21.addAP(0);

		IAtomContainer iac22 = builder.newAtomContainer();
		iac22.addAtom(new Atom("O"));
		Fragment v22 = new Fragment(22, iac22, BBType.FRAGMENT);
		v22.addAP(0);
		v22.addAP(0);

		IAtomContainer iac23 = builder.newAtomContainer();
		iac23.addAtom(new Atom("H"));
		Fragment v23 = new Fragment(23, iac23, BBType.CAP);
		v23.addAP(0);

		IAtomContainer iac24 = builder.newAtomContainer();
		iac24.addAtom(new Atom("H"));
		Fragment v24 = new Fragment(24, iac24, BBType.CAP);
		v24.addAP(0);

		graph2.addVertex(v21);
		graph2.addVertex(v22);
		graph2.addVertex(v23);
		graph2.addVertex(v24);
		graph2.addEdge(new Edge(v21.getAP(0), v22.getAP(0)));
		graph2.addEdge(new Edge(v21.getAP(1), v23.getAP(0)));
		graph2.addEdge(new Edge(v22.getAP(1), v24.getAP(0)));

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
	        DGraph graph) throws DENOPTIMException 
	{
		for (int atomPos = 0; atomPos < apCount; atomPos++) {
			v.addAP();
		}
		graph.addVertex(v);
	}
	
//------------------------------------------------------------------------------

	@Test
	public void testFromToJSON() throws Exception 
	{
	    DGraph graph = new DGraph();
        
	    EmptyVertex v0 = new EmptyVertex(0);
        buildVertexAndConnectToGraph(v0, 3, graph);

        EmptyVertex v1 = new EmptyVertex(1);
        buildVertexAndConnectToGraph(v1, 2, graph);
        graph.addEdge(new Edge(v0.getAP(0), v1.getAP(0)));

        EmptyVertex v2 = new EmptyVertex(2);
        buildVertexAndConnectToGraph(v2, 2, graph);
        graph.addEdge(new Edge(v1.getAP(1), v2.getAP(0)));

        EmptyVertex v3 = new EmptyVertex(3);
        buildVertexAndConnectToGraph(v3, 1, graph);
        graph.addEdge(new Edge(v2.getAP(1), v3.getAP(0)));

        EmptyVertex v4 = new EmptyVertex(4);
        buildVertexAndConnectToGraph(v4, 3, graph);
        graph.addEdge(new Edge(v0.getAP(1), v4.getAP(0)));

        EmptyVertex v5 = new EmptyVertex(5);
        buildVertexAndConnectToGraph(v5, 1, graph);
        graph.addEdge(new Edge(v4.getAP(1), v5.getAP(0)));

        EmptyVertex v6 = new EmptyVertex(6);
        buildVertexAndConnectToGraph(v6, 1, graph);
        graph.addEdge(new Edge(v0.getAP(2), v6.getAP(0)));

        EmptyVertex v7 = new EmptyVertex(7);
        buildVertexAndConnectToGraph(v7, 1, graph);
        graph.addEdge(new Edge(v4.getAP(2), v7.getAP(0)));

        graph.addRing(new Ring(new ArrayList<>(
                Arrays.asList(v5, v4, v0, v1, v2, v3))));

        graph.addRing(new Ring(new ArrayList<>(
                Arrays.asList(v6, v0, v4, v7))));

        graph.addSymmetricSetOfVertices(new SymmetricSet(
                new ArrayList<>(Arrays.asList(3, 5))));

        graph.addSymmetricSetOfVertices(new SymmetricSet(
                new ArrayList<>(Arrays.asList(6, 7))));
        
        String json1 = graph.toJson();
        
        DGraph g2 = DGraph.fromJson(json1);
        String json2 = g2.toJson();
        
        assertTrue(json1.equals(json2), "Round-trip via JSON is successful");
	}
	
//-----------------------------------------------------------------------------
    
    @Test
    public void testGraphIsomorphism() throws Exception 
    {
        FragmentSpace fs = prepare();
        
        DGraph gAempty = new DGraph();
        DGraph gBempty = new DGraph();

        assertTrue(gAempty.isIsomorphicTo(gAempty),
                "self isomorphism on empty graph");
        assertTrue(gAempty.isIsomorphicTo(gBempty),
                "isomorphism on empty graphs");
        
        DGraph g01 = makeTestGraph0(fs);
        DGraph g02 = makeTestGraph0(fs);
        assertTrue(g01.isIsomorphicTo(g02),
                "single-vertex graph");
        assertTrue(g01.isIsomorphicTo(g01.clone()),
                "single-vertex graph vs clone");
        
        DGraph g03 = new DGraph();
        EmptyVertex v1 = new EmptyVertex();
        v1.addAP();
        v1.addAP();
        String k ="MyKey";
        v1.setUniquefyingProperty(k);
        v1.setProperty(k, 123);
        g03.addVertex(v1);
        EmptyVertex v2 = new EmptyVertex();
        v2.addAP();
        v2.addAP();
        v2.setUniquefyingProperty(k);
        v2.setProperty(k, 456);
        g03.appendVertexOnAP(v1.getAP(0), v2.getAP(0));
        
        DGraph g04 = g03.clone();
        assertTrue(g03.isIsomorphicTo(g04),
                "graph with empty vertexes and same properties");
        
        v2.setProperty(k, 999);
        assertFalse(g03.isIsomorphicTo(g04),
                "graph with empty vertexes and different properties");
        
        DGraph g11 = makeTestGraph1(fs);
        DGraph g12 = makeTestGraph1(fs);
        assertTrue(g11.isIsomorphicTo(g12),"two-vertex graph");
        assertTrue(g11.isIsomorphicTo(g11.clone()),"two-vertex graph vs clone");
        
        DGraph gD1 = makeTestGraphD(fs);
        DGraph gD2 = makeTestGraphD(fs);
        assertTrue(gD1.isIsomorphicTo(gD2),"two of same graph D");
        assertFalse(gD1.isIsomorphicTo(gAempty),"graph D vs empty graph");
        assertFalse(gAempty.isIsomorphicTo(gD1),"empty graph vs graph D");
        
        DGraph gB1 = makeTestGraphB(fs);
        assertFalse(gD1.isIsomorphicTo(gB1),"graph D vs graph B");
        
        DGraph gB2 = gB1.clone();
        assertTrue(gB1.isIsomorphicTo(gB2),"graph B vs its clone");
    }
    
//-----------------------------------------------------------------------------
	
	@Test
    public void testExtractSubgraph() throws Exception 
	{
	    FragmentSpace fs = prepare();
        DGraph graph = makeTestGraphD(fs);
        DGraph graphOriginal = makeTestGraphD(fs);
        
        // This takes all graph
        DGraph subGraph = graph.extractSubgraph(
                graph.getVertexAtPosition(0));
        assertTrue(subGraph.isIsomorphicTo(graph), "complete subgraph");
        assertTrue(graph.isIsomorphicTo(graphOriginal), 
                "Original stays the same");
        
        // This takes a subgraph with a ring
        DGraph subGraph1 = graph.extractSubgraph(
                graph.getVertexAtPosition(1));
        DGraph expected1 = makeTestGraphDSub1(fs);
        
        assertTrue(subGraph1.isIsomorphicTo(expected1), "Subgraph1");
        assertEquals(0,subGraph1.getSymmetricSetCount(),
                "SymmetricSets in subGraph1");
        assertEquals(1,subGraph1.getRingCount(),"Rings in subGraph1");
        assertTrue(graph.isIsomorphicTo(graphOriginal), 
                "Original stays the same");
       
        // This takes a subgraph with symmetric set, but no rings
        DGraph subGraph2 = graph.extractSubgraph(
                graph.getVertexAtPosition(10));
        DGraph expected2 = makeTestGraphDSub2(fs);
        
        assertTrue(subGraph2.isIsomorphicTo(expected2), "Subgraph2");
        assertEquals(1,subGraph2.getSymmetricSetCount(),
                "SymmetricSets in subGraph2");
        assertEquals(0,subGraph2.getRingCount(),"Rings in subGraph2");
        assertTrue(graph.isIsomorphicTo(graphOriginal), 
                "Original stays the same");
	}

//-----------------------------------------------------------------------------
	
	@Test
	public void testFindVertex() throws Exception
	{
	    DGraph g = makeTestGraphA2();
	    
	    List<VertexQuery> allQueries = new ArrayList<VertexQuery>();
	    List<List<Vertex>> allExpected = 
	            new ArrayList<List<Vertex>>();
	    
	    VertexQuery q0 = new VertexQuery(null, null, null, null, null, null, null);
	    List<Vertex> e0 = new ArrayList<Vertex>();
	    e0.addAll(g.getVertexList());
	    allQueries.add(q0);
	    allExpected.add(e0);
	    
        VertexQuery q1 = new VertexQuery(
                g.getVertexAtPosition(5).getVertexId(), 
                null, null, null, null, null, null);
        List<Vertex> e1 = new ArrayList<Vertex>();
        e1.add(g.getVertexAtPosition(5));
        allQueries.add(q1);
        allExpected.add(e1);
        
        VertexQuery q2 = new VertexQuery(
                g.getVertexAtPosition(2).getVertexId(), 
                null, null, null, null, null, null);
        List<Vertex> e2 = new ArrayList<Vertex>();
        e2.add(g.getVertexAtPosition(2));
        allQueries.add(q2);
        allExpected.add(e2);
        
        VertexQuery q3 = new VertexQuery(null, VertexType.EmptyVertex, 
                null, null, null, null, null);
        List<Vertex> e3 = new ArrayList<Vertex>();
        e3.add(g.getVertexAtPosition(4));
        e3.add(g.getVertexAtPosition(5));
        e3.add(g.getVertexAtPosition(6));
        allQueries.add(q3);
        allExpected.add(e3);
        
        VertexQuery q4 = new VertexQuery(null, null, BBType.CAP,
                null, null, null, null);
        List<Vertex> e4 = new ArrayList<Vertex>();
        e4.add(g.getVertexAtPosition(2));
        e4.add(g.getVertexAtPosition(3));
        allQueries.add(q4);
        allExpected.add(e4);
        
        VertexQuery q5 = new VertexQuery(null, null, null, 2,
                null, null, null);
        List<Vertex> e5 = new ArrayList<Vertex>();
        e5.add(g.getVertexAtPosition(2));
        allQueries.add(q5);
        allExpected.add(e5);
        
        VertexQuery q6 = new VertexQuery(null, null, null, null, 1,
                null, null);
        List<Vertex> e6 = new ArrayList<Vertex>();
        e6.add(g.getVertexAtPosition(3));
        e6.add(g.getVertexAtPosition(5));
        allQueries.add(q6);
        allExpected.add(e6);
        
        //
        // From here: test filters acting on incoming edge
        //
        
        EdgeQuery eq7 = new EdgeQuery(null, null, null, null, null, null, null);
        VertexQuery q7 = new VertexQuery(null, null, null, null, null, 
                eq7, eq7);
        List<Vertex> e7 = new ArrayList<Vertex>();
        e7.addAll(g.getVertexList());
        allQueries.add(q7);
        allExpected.add(e7);
        
        EdgeQuery eq8 = new EdgeQuery(g.getVertexAtPosition(1).getVertexId(), 
                null, null, null, null, null, null);
        VertexQuery q8 = new VertexQuery(null, null, null, null, null, 
                eq8, null);
        List<Vertex> e8 = new ArrayList<Vertex>();
        e8.add(g.getVertexAtPosition(3));
        allQueries.add(q8);
        allExpected.add(e8);
        
        //NB: the trg vertex ID on the incoming vertex is NOT considered
        // because it is a redundant condition that should be expressed as
        // the VertexQuery vID argument.
        EdgeQuery eq9 = new EdgeQuery(null, 
                g.getVertexAtPosition(5).getVertexId(), 
                null, null, null, null, null);
        VertexQuery q9 = new VertexQuery(null, null, null, null, null, 
                eq9, null);
        List<Vertex> e9 = new ArrayList<Vertex>();
        e9.addAll(g.getVertexList());
        allQueries.add(q9);
        allExpected.add(e9);
        
        EdgeQuery eq10 = new EdgeQuery(null, null, 1, null, null, null, null);
        VertexQuery q10 = new VertexQuery(null, null, null, null, null, 
                eq10, null);
        List<Vertex> e10 = new ArrayList<Vertex>();
        e10.add(g.getVertexAtPosition(2));
        e10.add(g.getVertexAtPosition(3));
        allQueries.add(q10);
        allExpected.add(e10);
        
        EdgeQuery eq11 = new EdgeQuery(null, null, null, 0, null, null, null);
        VertexQuery q11 = new VertexQuery(null, null, null, null, null, 
                eq11, null);
        List<Vertex> e11 = new ArrayList<Vertex>();
        e11.add(g.getVertexAtPosition(1));
        e11.add(g.getVertexAtPosition(2));
        e11.add(g.getVertexAtPosition(3));
        e11.add(g.getVertexAtPosition(4));
        e11.add(g.getVertexAtPosition(5));       
        allQueries.add(q11);
        allExpected.add(e11);
        
        EdgeQuery eq12 = new EdgeQuery(null, null, null, null,
                BondType.TRIPLE, null, null);
        VertexQuery q12 = new VertexQuery(null, null, null, null, null, 
                eq12, null);
        List<Vertex> e12 = new ArrayList<Vertex>();
        e12.add(g.getVertexAtPosition(1));
        e12.add(g.getVertexAtPosition(5));
        allQueries.add(q12);
        allExpected.add(e12);
        
        EdgeQuery eq13 = new EdgeQuery(null, null, null, null, null, 
                APClass.make(b,1), null);
        VertexQuery q13 = new VertexQuery(null, null, null, null, null, 
                eq13, null);
        List<Vertex> e13 = new ArrayList<Vertex>();
        e13.add(g.getVertexAtPosition(2));
        e13.add(g.getVertexAtPosition(3));
        e13.add(g.getVertexAtPosition(6));
        allQueries.add(q13);
        allExpected.add(e13);
        
        EdgeQuery eq14 = new EdgeQuery(null, null, null, null, null, null,
                APClass.make(c,1));
        VertexQuery q14 = new VertexQuery(null, null, null, null, null, 
                eq14, null);
        List<Vertex> e14 = new ArrayList<Vertex>();
        e14.add(g.getVertexAtPosition(2));
        e14.add(g.getVertexAtPosition(3));
        allQueries.add(q14);
        allExpected.add(e14);
        
        //
        // From here: test filters acting on outgoing edge
        //
        
        //NB: the src vertex ID on the outging vertex is NOT considered
        // because it is a redundant condition that should be expressed as
        // the VertexQuery vID argument.
        EdgeQuery eq15 = new EdgeQuery(g.getVertexAtPosition(3).getVertexId(), 
                null, null, null, null, null, null);
        VertexQuery q15 = new VertexQuery(null, null, null, null, null, null,
                eq15);
        List<Vertex> e15 = new ArrayList<Vertex>();
        e15.addAll(g.getVertexList());
        allQueries.add(q15);
        allExpected.add(e15);
        
        EdgeQuery eq16 = new EdgeQuery(null,
                g.getVertexAtPosition(3).getVertexId(),
                null, null, null, null, null);
        VertexQuery q16 = new VertexQuery(null, null, null, null, null, null,
                eq16);
        List<Vertex> e16 = new ArrayList<Vertex>();
        e16.add(g.getVertexAtPosition(1));
        allQueries.add(q16);
        allExpected.add(e16);

        EdgeQuery eq17 = new EdgeQuery(null, null, 1, null, null, null, null);
        VertexQuery q17 = new VertexQuery(null, null, null, null, null, null,
                eq17);
        List<Vertex> e17 = new ArrayList<Vertex>();
        e17.add(g.getVertexAtPosition(0));
        e17.add(g.getVertexAtPosition(1));
        allQueries.add(q17);
        allExpected.add(e17);
  
        EdgeQuery eq18 = new EdgeQuery(null, null, null, 0, null, null, null);
        VertexQuery q18 = new VertexQuery(null, null, null, null, null, null,
                eq18);
        List<Vertex> e18 = new ArrayList<Vertex>();
        e18.add(g.getVertexAtPosition(0));
        e18.add(g.getVertexAtPosition(1));
        e18.add(g.getVertexAtPosition(6));
        allQueries.add(q18);
        allExpected.add(e18);

        EdgeQuery eq19 = new EdgeQuery(null, null, null, null,
                BondType.TRIPLE, null, null);
        VertexQuery q19 = new VertexQuery(null, null, null, null, null, null,
                eq19);
        List<Vertex> e19 = new ArrayList<Vertex>();
        e19.add(g.getVertexAtPosition(0));
        e19.add(g.getVertexAtPosition(6));
        allQueries.add(q19);
        allExpected.add(e19);
        
        EdgeQuery eq20 = new EdgeQuery(null, null, null, null, null,
                APClass.make(b,1), null);
        VertexQuery q20 = new VertexQuery(null, null, null, null, null, null,
                eq20);
        List<Vertex> e20 = new ArrayList<Vertex>();
        e20.add(g.getVertexAtPosition(0));
        e20.add(g.getVertexAtPosition(1));
        allQueries.add(q20);
        allExpected.add(e20);

        EdgeQuery eq21 = new EdgeQuery(null, null, null, null, null, null,
                APClass.make(d,0));
        VertexQuery q21 = new VertexQuery(null, null, null, null, null, null,
                eq21);
        List<Vertex> e21 = new ArrayList<Vertex>();
        e21.add(g.getVertexAtPosition(0));
        e21.add(g.getVertexAtPosition(6));
        allQueries.add(q21);
        allExpected.add(e21);
        
        //
        // From here: test combinations
        //
       
        EdgeQuery eq22in = new EdgeQuery(null, null, null, 0, null, null, null);
        EdgeQuery eq22out = new EdgeQuery(null, null, 1,null, null, null, null);
        VertexQuery q22 = new VertexQuery(null, null, null, null, null, 
                eq22in, eq22out);
        List<Vertex> e22 = new ArrayList<Vertex>();
        e22.add(g.getVertexAtPosition(1));      
        allQueries.add(q22);
        allExpected.add(e22);
        
        EdgeQuery eq23 = new EdgeQuery(null, null, null, null, BondType.TRIPLE, 
                null, null);
        VertexQuery q23 = new VertexQuery(null, VertexType.MolecularFragment, 
                null, null, null, eq23, null);
        List<Vertex> e23 = new ArrayList<Vertex>();
        e23.add(g.getVertexAtPosition(1));
        allQueries.add(q23);
        allExpected.add(e23);
	    
	    for (int i=0; i<allQueries.size(); i++)
	    {
	        List<Vertex> matches = g.findVertices(allQueries.get(i),
	                Logger.getLogger("DummyLogger"));
	        assertEquals(allExpected.get(i).size(),matches.size(),
	                "Different number of matched vertexes ("+i+")");
    	    assertTrue(allExpected.get(i).containsAll(matches),
    	            "Inconsistent matches ("+i+")");
	    }
	}

//------------------------------------------------------------------------------
	
    
    @Test
    public void testSymmetricSetLabels() throws Exception
    {
        FragmentSpace fs = prepare();
        DGraph g = makeTestGraphB(fs);
        
        g.reassignSymmetricLabels();
        
        Map<Object,Integer> countsPerLabel = new HashMap<Object,Integer>();
        for (Vertex v : g.getVertexList())
        {
            Object label = v.getProperty(DENOPTIMConstants.VRTSYMMSETID);
            if (countsPerLabel.containsKey(label))
                countsPerLabel.put(label,countsPerLabel.get(label)+1);
            else
                countsPerLabel.put(label,1);
        }
        
        assertEquals(1,countsPerLabel.get(g.getVertexAtPosition(0).getProperty(
                DENOPTIMConstants.VRTSYMMSETID)));
        assertEquals(2,countsPerLabel.get(g.getVertexAtPosition(1).getProperty(
                DENOPTIMConstants.VRTSYMMSETID)));
        assertEquals(4,countsPerLabel.get(g.getVertexAtPosition(2).getProperty(
                DENOPTIMConstants.VRTSYMMSETID)));
        assertEquals(4,countsPerLabel.get(g.getVertexAtPosition(3).getProperty(
                DENOPTIMConstants.VRTSYMMSETID)));
        assertEquals(1,countsPerLabel.get(g.getVertexAtPosition(4).getProperty(
                DENOPTIMConstants.VRTSYMMSETID)));
        assertEquals(1,countsPerLabel.get(g.getVertexAtPosition(8).getProperty(
                DENOPTIMConstants.VRTSYMMSETID)));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testConvertSymmetricLabelsToSymmetricSets() throws Exception
    {
        FragmentSpace fs = prepare();
        DGraph g = makeTestGraphE(fs);
        g.getVertexAtPosition(5).setProperty(DENOPTIMConstants.VRTSYMMSETID, 
                "1234-ABC");
        g.getVertexAtPosition(6).setProperty(DENOPTIMConstants.VRTSYMMSETID, 
                "1234-ABC");
        g.getVertexAtPosition(7).setProperty(DENOPTIMConstants.VRTSYMMSETID, 
                "1234-ABC");
        
        g.convertSymmetricLabelsToSymmetricSets();
        
        assertEquals(2,g.getSymmetricSetCount(),"number of sets");
        boolean foundA = false;
        boolean foundB = false;
        Iterator<SymmetricSet> iter = g.getSymSetsIterator();
        while (iter.hasNext())
        {
            SymmetricSet ss = iter.next();
            if (ss.size() == 2)
                foundA = true;
            if (ss.size() == 4)
                foundB = true;
        }
        assertTrue(foundA,"Found 2-membered set");
        assertTrue(foundB,"Found 4-membered set");
        
        DGraph g2 = makeTestGraphE(fs);
        g2.getVertexAtPosition(6).setProperty(DENOPTIMConstants.VRTSYMMSETID, 
                "1234-ABC");
        g2.getVertexAtPosition(7).setProperty(DENOPTIMConstants.VRTSYMMSETID, 
                "1234-ABC");
        
        g2.convertSymmetricLabelsToSymmetricSets();
        
        assertEquals(3,g2.getSymmetricSetCount(),"number of sets");
        Iterator<SymmetricSet> iter2 = g2.getSymSetsIterator();
        while (iter2.hasNext())
        {
            SymmetricSet ss = iter2.next();
            assertEquals(2,ss.size(),"side of each symmetric sets.");
        }
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetSymmetricSubGraphs() throws Exception
    {
        FragmentSpace fs = prepare();
        DGraph g = makeTestGraphF(fs);
        List<Vertex> sg = new ArrayList<Vertex>();
        sg.add(g.getVertexAtPosition(1));
        sg.add(g.getVertexAtPosition(3));
        
        List<List<Vertex>> symSubGraphs = g.getSymmetricSubGraphs(sg);
        
        assertEquals(2,symSubGraphs.size(),"Number of subgraphs");
        assertEquals(g.getVertexAtPosition(1).getVertexId(),
                symSubGraphs.get(0).get(0).getVertexId());
        assertEquals(g.getVertexAtPosition(3).getVertexId(),
                symSubGraphs.get(0).get(1).getVertexId());
        assertEquals(g.getVertexAtPosition(2).getVertexId(),
                symSubGraphs.get(1).get(0).getVertexId());
        assertEquals(g.getVertexAtPosition(4).getVertexId(),
                symSubGraphs.get(1).get(1).getVertexId());
        

        DGraph g2 = makeTestGraphK(fs);
        List<Vertex> sg2 = new ArrayList<Vertex>();
        sg2.add(g2.getVertexAtPosition(1));
        sg2.add(g2.getVertexAtPosition(4));
        sg2.add(g2.getVertexAtPosition(12));
        
        /*
         * In the second part we check that capping groups are not included.
         */
        
        boolean exceptionWhenCappingIsIncluded = false;
        try
        {
            symSubGraphs = g2.getSymmetricSubGraphs(sg2);
        } catch (DENOPTIMException e)
        {
            if (e.getMessage().contains("Capping groups must not be part of "
                    + "symmetric subgraphs"))
                exceptionWhenCappingIsIncluded = true;
        }
        assertTrue(exceptionWhenCappingIsIncluded,
                "Capping groups trigger exception");
        
        sg2 = new ArrayList<Vertex>();
        sg2.add(g2.getVertexAtPosition(1));
        sg2.add(g2.getVertexAtPosition(4));
        symSubGraphs = g2.getSymmetricSubGraphs(sg2);
        assertEquals(2,symSubGraphs.size());
        assertEquals(2,symSubGraphs.get(0).size());
        assertEquals(2,symSubGraphs.get(1).size());
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetChildrenTree() throws Exception
    {
        FragmentSpace fs = prepare();
        DGraph g = makeTestGraphD(fs);
        
        List<Vertex> childTree = new ArrayList<Vertex>();
        g.getChildrenTree(g.getVertexAtPosition(1), childTree);
        assertEquals(6,childTree.size());
        assertTrue(childTree.contains(g.getVertexAtPosition(2)));
        assertTrue(childTree.contains(g.getVertexAtPosition(3)));
        assertTrue(childTree.contains(g.getVertexAtPosition(4)));
        assertTrue(childTree.contains(g.getVertexAtPosition(5)));
        assertTrue(childTree.contains(g.getVertexAtPosition(6)));
        assertTrue(childTree.contains(g.getVertexAtPosition(7)));
        
        childTree = new ArrayList<Vertex>();
        g.getChildrenTree(g.getVertexAtPosition(9), childTree, 1, false);
        assertEquals(1,childTree.size());
        assertTrue(childTree.contains(g.getVertexAtPosition(10)));
        
        childTree = new ArrayList<Vertex>();
        g.getChildrenTree(g.getVertexAtPosition(9), childTree, 2, false);
        assertEquals(4,childTree.size());
        assertTrue(childTree.contains(g.getVertexAtPosition(10)));
        assertTrue(childTree.contains(g.getVertexAtPosition(11)));
        assertTrue(childTree.contains(g.getVertexAtPosition(12)));
        assertTrue(childTree.contains(g.getVertexAtPosition(13)));
        
        childTree = new ArrayList<Vertex>();
        g.getChildrenTree(g.getVertexAtPosition(1), childTree, Integer.MAX_VALUE,
                true);
        assertEquals(3,childTree.size());
        assertTrue(childTree.contains(g.getVertexAtPosition(2)));
        assertTrue(childTree.contains(g.getVertexAtPosition(3)));
        assertTrue(childTree.contains(g.getVertexAtPosition(4)));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetInterfaceAPs() throws Exception
    {
        FragmentSpace fs = prepare();
        DGraph g = makeTestGraphI(fs);
        
        List<Vertex> subGraph = new ArrayList<Vertex>();
        subGraph.add(g.getVertexAtPosition(0));
        subGraph.add(g.getVertexAtPosition(1));
        
        List<AttachmentPoint> expected = new ArrayList<AttachmentPoint>();
        expected.add(g.getVertexAtPosition(0).getAP(1));
        expected.add(g.getVertexAtPosition(1).getAP(1));
        
        List<AttachmentPoint> interfaceAPs = g.getInterfaceAPs(subGraph);
        
        assertEquals(expected,interfaceAPs);
        
        DGraph innerGraph = ((Template) g.getVertexAtPosition(0))
                .getInnerGraph();
        
        subGraph = new ArrayList<Vertex>();
        subGraph.add(innerGraph.getVertexAtPosition(0));
        subGraph.add(innerGraph.getVertexAtPosition(1));
        
        expected = new ArrayList<AttachmentPoint>();
        expected.add(innerGraph.getVertexAtPosition(0).getAP(0));
        expected.add(innerGraph.getVertexAtPosition(0).getAP(1));
        expected.add(innerGraph.getVertexAtPosition(0).getAP(4));
        expected.add(innerGraph.getVertexAtPosition(1).getAP(1));
        
        interfaceAPs = innerGraph.getInterfaceAPs(subGraph);
        
        assertEquals(expected,interfaceAPs);
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetSubgraphAPs() throws Exception
    {
        FragmentSpace fs = prepare();
        DGraph g = makeTestGraphI(fs);
        
        DGraph innerGraph = ((Template) g.getVertexAtPosition(0))
                .getInnerGraph();
        
        List<Vertex> subGraph = new ArrayList<Vertex>();
        subGraph.add(innerGraph.getVertexAtPosition(0));
        subGraph.add(innerGraph.getVertexAtPosition(1));
        
        List<AttachmentPoint>expected = new ArrayList<AttachmentPoint>();
        expected.add(innerGraph.getVertexAtPosition(0).getAP(0));
        expected.add(innerGraph.getVertexAtPosition(0).getAP(1));
        expected.add(innerGraph.getVertexAtPosition(0).getAP(2));
        expected.add(innerGraph.getVertexAtPosition(0).getAP(4));
        expected.add(innerGraph.getVertexAtPosition(1).getAP(1));
        
        List<AttachmentPoint> interfaceAPs = innerGraph.getSubgraphAPs(
                subGraph);
        
        assertEquals(expected,interfaceAPs);
    }
    
//------------------------------------------------------------------------------
    
    public DGraph[] makeIsostructuralGraphs() throws Exception
    {
        String unqProp = "UNQPROP";
        EmptyVertex rcvA1 = new EmptyVertex(11002);
        rcvA1.addAP(APCA);
        rcvA1.setAsRCV(true);
        EmptyVertex rcvA2 = new EmptyVertex(11003);
        rcvA2.addAP(APCA);
        rcvA2.setAsRCV(true);
        EmptyVertex vA0 = new EmptyVertex(10000);
        vA0.addAP(APCA);
        vA0.addAP(APCA);
        vA0.addAP(APCB);
        vA0.setUniquefyingProperty(unqProp);
        vA0.setProperty(unqProp, 111);
        EmptyVertex vA1 = new EmptyVertex(10001);
        vA1.addAP(APCA);
        vA1.addAP(APCA);
        vA1.setUniquefyingProperty(unqProp);
        vA1.setProperty(unqProp, 222);
        EmptyVertex vA2= new EmptyVertex(10002);
        vA2.addAP(APCA);
        vA2.addAP(APCA);
        EmptyVertex vA3 = new EmptyVertex(10003);
        vA3.addAP(APCA);
        vA3.addAP(APCA);
        vA3.addAP(APCB);
        DGraph g3A = new DGraph();
        g3A.addVertex(vA0);
        g3A.appendVertexOnAP(vA0.getAP(0), vA1.getAP(0));
        g3A.appendVertexOnAP(vA1.getAP(1), vA2.getAP(0));
        g3A.appendVertexOnAP(vA2.getAP(1), vA3.getAP(0));
        g3A.appendVertexOnAP(vA3.getAP(1), rcvA1.getAP(0));
        g3A.appendVertexOnAP(vA0.getAP(1), rcvA2.getAP(0));
        g3A.addRing(rcvA1, rcvA2);
        
        // now build  second
        EmptyVertex rcvB1 = new EmptyVertex(21002);
        rcvB1.addAP(APCA);
        rcvB1.setAsRCV(true);
        EmptyVertex rcvB2 = new EmptyVertex(21003);
        rcvB2.addAP(APCA);
        rcvB2.setAsRCV(true);
        EmptyVertex vB0 = new EmptyVertex(20000);
        vB0.addAP(APCA);
        vB0.addAP(APCA);
        vB0.addAP(APCB);
        vA0.setUniquefyingProperty(unqProp);
        vA0.setProperty(unqProp, 333);
        EmptyVertex vB1 = new EmptyVertex(20001);
        vB1.addAP(APCA);
        vB1.addAP(APCA);
        vA0.setUniquefyingProperty(unqProp);
        vA0.setProperty(unqProp, 444);
        EmptyVertex vB2= new EmptyVertex(20002);
        vB2.addAP(APCA);
        vB2.addAP(APCA);
        EmptyVertex vB3 = new EmptyVertex(20003);
        vB3.addAP(APCA);
        vB3.addAP(APCA);
        vB3.addAP(APCB);
        DGraph g3B = new DGraph();
        g3B.addVertex(vB1);
        g3B.appendVertexOnAP(vB1.getAP(1), vB2.getAP(0));
        g3B.appendVertexOnAP(vB2.getAP(1), vB3.getAP(0));
        g3B.appendVertexOnAP(vB3.getAP(1), vB0.getAP(1));
        g3B.appendVertexOnAP(vB0.getAP(0), rcvB1.getAP(0));
        g3B.appendVertexOnAP(vB1.getAP(0), rcvB2.getAP(0));
        g3B.addRing(rcvB1, rcvB2);
        
        DGraph[] pair = new DGraph[2];
        pair[0] = g3A;
        pair[1] = g3B;
        return pair;
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testIsIsostructuralTo() throws Exception
    {
        FragmentSpace fs = prepare();
        
        DGraph gAempty = new DGraph();
        DGraph gBempty = new DGraph();

        assertTrue(gAempty.isIsostructuralTo(gAempty));
        assertTrue(gAempty.isIsostructuralTo(gBempty));
        
        DGraph g01 = makeTestGraph0(fs);
        DGraph g02 = makeTestGraph0(fs);
        assertTrue(g01.isIsostructuralTo(g02));
        assertTrue(g01.isIsostructuralTo(g01.clone()));
        
        DGraph g11 = makeTestGraph1(fs);
        DGraph g12 = makeTestGraph1(fs);
        assertTrue(g11.isIsostructuralTo(g12));
        assertFalse(g11.isIsostructuralTo(g01));
        assertFalse(g11.isIsostructuralTo(g02));
        assertTrue(g11.isIsostructuralTo(g11.clone()));
        
        DGraph gD1 = makeTestGraphD(fs);
        DGraph gD2 = makeTestGraphD(fs);
        assertTrue(gD1.isIsostructuralTo(gD2));
        assertFalse(gD1.isIsostructuralTo(gAempty));
        assertFalse(gAempty.isIsostructuralTo(gD1));
        
        DGraph gB1 = makeTestGraphB(fs);
        assertFalse(gD1.isIsostructuralTo(gB1));
        
        DGraph gB2 = gB1.clone();
        assertTrue(gB1.isIsostructuralTo(gB2));
        
        // Up to here we checked consistency with isIsomorficTo().
        // Now, make a pair of graphs that are not isomorphic by replacing one 
        // of the vertexes with a new vertex that does not satisfy sameAs()
        DGraph g1 = makeTestGraphDSub1(fs);
        DGraph g2 = makeTestGraphDSub1(fs);
        
        EmptyVertex v1 = new EmptyVertex(10001);
        v1.addAP(APCD);
        v1.addAP(APCC);
        v1.setUniquefyingProperty("blabla123");
        v1.setProperty("blabla123", 123);
        
        DGraph incomingSubGraph = new DGraph();
        incomingSubGraph.addVertex(v1);
        
        LinkedHashMap<AttachmentPoint,AttachmentPoint> apMap =
                new LinkedHashMap<AttachmentPoint,AttachmentPoint>();
        apMap.put(g2.getVertexAtPosition(1).getAP(0), v1.getAP(1));
        apMap.put(g2.getVertexAtPosition(1).getAP(1), v1.getAP(0));
        
        List<Vertex> toReplace = new ArrayList<Vertex>();
		toReplace.add(g2.getVertexAtPosition(1));
        g2.replaceSingleSubGraph(toReplace, incomingSubGraph, apMap);
        
        assertFalse(g1.isIsomorphicTo(g2));
        assertTrue(g1.isIsostructuralTo(g2));
        
        // Change structure by adding a bifurcation
        EmptyVertex v2 = new EmptyVertex(10002);
        v2.addAP(APCD);
        v2.addAP(APCC);
        v2.addAP(APCC);
        v2.setUniquefyingProperty("blabla456");
        v2.setProperty("blabla456", 456);
        incomingSubGraph = new DGraph();
        incomingSubGraph.addVertex(v2);
        apMap = new LinkedHashMap<AttachmentPoint,AttachmentPoint>();
        apMap.put(g2.getVertexAtPosition(2).getAP(0), v1.getAP(1));
        apMap.put(g2.getVertexAtPosition(2).getAP(1), v1.getAP(0));
        toReplace = new ArrayList<Vertex>();
        toReplace.add(g2.getVertexAtPosition(2));
        g2.replaceSingleSubGraph(toReplace, incomingSubGraph, apMap);
        
        assertFalse(g1.isIsomorphicTo(g2));
        assertFalse(g1.isIsostructuralTo(g2));
        
        // Change graph retaining structure: move ring closure along ring.
        DGraph[] pair = makeIsostructuralGraphs();
        DGraph gisA = pair[0];
        DGraph gisB = pair[1];
        assertFalse(gisA.isIsomorphicTo(gisB));
        assertTrue(gisA.isIsostructuralTo(gisB));
    }
    
//------------------------------------------------------------------------------
	
}
