package denoptim.fragspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.jupiter.api.Test;

import denoptim.exception.DENOPTIMException;
import denoptim.graph.APClass;
import denoptim.graph.APMapping;
import denoptim.graph.DENOPTIMAttachmentPoint;
import denoptim.graph.DENOPTIMEdge;
import denoptim.graph.DENOPTIMGraph;
import denoptim.graph.DENOPTIMVertex;
import denoptim.graph.DENOPTIMVertex.BBType;
import denoptim.graph.EmptyVertex;

/**
 * Unit test
 * 
 * @author Marco Foscato
 */

public class APMapFinderTest
{
    private static APClass APCA, APCB, APCC, APCD, APCE, APCF;
    
//------------------------------------------------------------------------------
    
    private void prepare() throws DENOPTIMException
    {
        APCA = APClass.make("A", 0);
        APCB = APClass.make("B", 0);
        APCC = APClass.make("C", 0);
        APCD = APClass.make("D", 99);
        APCE = APClass.make("E", 13);
        APCF = APClass.make("F", 17);
        
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
        ArrayList<APClass> lstE = new ArrayList<APClass>();
        lstE.add(APCE);
        cpMap.put(APCE, lstE);
        
        
        /* Compatibility matrix
         * 
         *      |  A  |  B  |  C  | D | E | F
         *    ---------------------------------
         *    A |  T  |     |     |   |   |
         *    ---------------------------------
         *    B |     |  T  |  T  |   |   |
         *    ---------------------------------
         *    C |     |  T  |  T  |   |   |
         *    ---------------------------------
         *    D |     |     |     | T |   |
         *    ---------------------------------
         *    E |     |     |     |   | T |
         *    ---------------------------------
         *    F |     |     |     |   |   |
         *    
         *    NB: F has NONE!
         */
        
        HashMap<APClass,APClass> capMap = new HashMap<APClass,APClass>();
        HashSet<APClass> forbEnds = new HashSet<APClass>();
        
        FragmentSpace.setCompatibilityMatrix(cpMap);
        FragmentSpace.setCappingMap(capMap);
        FragmentSpace.setForbiddenEndList(forbEnds);
        FragmentSpace.setAPclassBasedApproach(true);
        
        FragmentSpace.setScaffoldLibrary(new ArrayList<DENOPTIMVertex>());
        FragmentSpace.setFragmentLibrary(new ArrayList<DENOPTIMVertex>());
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testAPMapFinder_Constrained() throws Exception
    {
        prepare();
        EmptyVertex vA = new EmptyVertex();
        vA.setBuildingBlockType(BBType.FRAGMENT);
        vA.addAP(APCA);
        vA.addAP(APCA);
        vA.addAP(APCA);
        vA.addAP(APCD);

        EmptyVertex vB = new EmptyVertex();
        vB.setBuildingBlockType(BBType.FRAGMENT);
        vB.addAP(APCD);
        vB.addAP(APCD);
        vB.addAP(APCA);
        vB.addAP(APCA);
        vB.addAP(APCA);
        
        APMapping constrain = new APMapping();
        constrain.put(vA.getAP(2), vB.getAP(4));
        APMapFinder apmf = new APMapFinder(vA, vB, constrain, true, false, false);
        
        assertTrue(apmf.foundMapping());
        assertEquals(21, apmf.getAllAPMappings().size());
        for (APMapping apm : apmf.getAllAPMappings())
        {
            assertTrue(apm.containsKey(vA.getAP(2)));
            assertEquals(vB.getAP(4), apm.get(vA.getAP(2)));
        }
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testAPMapFinder_ConstrainAll() throws Exception
    {
        prepare();
        EmptyVertex vA = new EmptyVertex();
        vA.setBuildingBlockType(BBType.FRAGMENT);
        vA.addAP(APCA);

        EmptyVertex vB = new EmptyVertex();
        vB.setBuildingBlockType(BBType.FRAGMENT);
        vB.addAP(APCA);
        
        // NB "all" in "ConstrainAll" means that a single constraint covers all APs
        APMapping constrain = new APMapping();
        constrain.put(vA.getAP(0), vB.getAP(0));
        APMapFinder apmf = new APMapFinder(vA, vB, constrain, true, false, false);
        
        assertTrue(apmf.foundMapping());
        assertEquals(1, apmf.getAllAPMappings().size());
        assertTrue(apmf.getChosenAPMapping().containsKey(vA.getAP(0)));
        assertEquals(vB.getAP(0), apmf.getChosenAPMapping().get(vA.getAP(0)));
    }

//------------------------------------------------------------------------------
    
    @Test
    public void testAPMapFinder() throws Exception
    {
        prepare();
        EmptyVertex vA = new EmptyVertex();
        vA.setBuildingBlockType(BBType.FRAGMENT);
        vA.addAP(APCA);
        vA.addAP(APCD);

        EmptyVertex vB = new EmptyVertex();
        vB.setBuildingBlockType(BBType.FRAGMENT);
        vB.addAP(APCA);
        vB.addAP(APCD);
        
        // NB: when APs are NOT used, the mapping is permissive and is happy
        // to find "a" AP to map another AP with, irrespectively on the APClass
        
        APMapFinder apmf = new APMapFinder(vA, vB, true);
        
        assertTrue(apmf.foundMapping());
        assertEquals(6, apmf.getAllAPMappings().size());
        assertEquals(4, apmf.getAllAPMappings()
                .stream().filter(m -> m.size()==1)
                .count());
        assertEquals(2, apmf.getAllAPMappings()
                .stream().filter(m -> m.size()==2)
                .count());
        assertEquals(4, apmf.getAllAPMappings()
            .stream()
            .filter(m ->  m.containsKey(vA.getAP(0)))
            .count());
        assertEquals(2, apmf.getAllAPMappings()
                .stream()
                .filter(m -> m.size()==2)
                .filter(m ->  m.containsKey(vA.getAP(0)))
                .count());
        
        //: we can restrict to all mappings that cover all APs on first vertex
        apmf = new APMapFinder(vA, vB, null, true, true, true);
        
        assertTrue(apmf.foundMapping());
        assertEquals(2, apmf.getAllAPMappings().size());
        
        // testing for handling of used APs
        
        EmptyVertex vC = new EmptyVertex();
        vC.setBuildingBlockType(BBType.FRAGMENT);
        vC.addAP(APCA);
        vC.addAP(APCD);

        EmptyVertex vD = new EmptyVertex();
        vD.setBuildingBlockType(BBType.FRAGMENT);
        vD.addAP(APCA);
        vD.addAP(APCD);
        
        DENOPTIMGraph g = new DENOPTIMGraph();
        g.addVertex(vA);
        g.addVertex(vB);
        g.addVertex(vC);
        g.addVertex(vD);
        g.addEdge(new DENOPTIMEdge(vA.getAP(0), vC.getAP(0)));
        g.addEdge(new DENOPTIMEdge(vA.getAP(1), vD.getAP(1)));
        g.addEdge(new DENOPTIMEdge(vB.getAP(1), vC.getAP(1)));
        g.addEdge(new DENOPTIMEdge(vB.getAP(0), vD.getAP(0)));
        
        // NB: when APs are used the mapping is much more restrictive,
        // because it is forced to respect the APClass compatibility rules.

        apmf = new APMapFinder(vA, vB, true);
        
        assertTrue(apmf.foundMapping());
        assertEquals(1, apmf.getAllAPMappings().size());
        assertEquals(1, apmf.getAllAPMappings()
                .stream().filter(m -> m.size()==2)
                .count());
        assertEquals(1, apmf.getAllAPMappings()
            .stream()
            .filter(m ->  m.containsKey(vA.getAP(0)))
            .count());

        // When considering different APClasses, the compatibility matrix matters.
        
        EmptyVertex vF = new EmptyVertex();
        vF.setBuildingBlockType(BBType.FRAGMENT);
        vF.addAP(APCB);

        EmptyVertex vG = new EmptyVertex();
        vG.setBuildingBlockType(BBType.FRAGMENT);
        vG.addAP(APCC);
        
        DENOPTIMGraph g2 = new DENOPTIMGraph();
        g2.addVertex(vF);
        g2.addVertex(vG);
        g2.addEdge(new DENOPTIMEdge(vF.getAP(0), vG.getAP(0)));
        
        apmf = new APMapFinder(vF, vG, true);
        
        assertTrue(apmf.foundMapping());
        assertEquals(vG.getAP(0), apmf.getChosenAPMapping().get(vF.getAP(0)));
    }
    
//------------------------------------------------------------------------------
    
}
