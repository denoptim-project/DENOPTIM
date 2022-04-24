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

package denoptim.fragspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import denoptim.exception.DENOPTIMException;
import denoptim.graph.APClass;
import denoptim.graph.APMapping;
import denoptim.graph.DGraph;
import denoptim.graph.Edge;
import denoptim.graph.EmptyVertex;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;

/**
 * Unit test
 * 
 * @author Marco Foscato
 */

public class APMapFinderTest
{
    private static APClass APCA, APCB, APCC, APCD, APCE;
    
//------------------------------------------------------------------------------
    
    private FragmentSpace prepare() throws DENOPTIMException
    {
        APCA = APClass.make("A", 0);
        APCB = APClass.make("B", 0);
        APCC = APClass.make("C", 0);
        APCD = APClass.make("D", 99);
        APCE = APClass.make("E", 13);
        
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
        
        FragmentSpaceParameters fsp = new FragmentSpaceParameters();
        FragmentSpace fs = new FragmentSpace(fsp,
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(), 
                cpMap, capMap, forbEnds, cpMap);
        fs.setAPclassBasedApproach(true);
        
        return fs;
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testAPMapFinder_Constrained() throws Exception
    {
        FragmentSpace fs = prepare();
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
        APMapFinder apmf = new APMapFinder(fs,vA,vB,constrain,true,false,false);
        
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
        FragmentSpace fs = prepare();
        EmptyVertex vA = new EmptyVertex();
        vA.setBuildingBlockType(BBType.FRAGMENT);
        vA.addAP(APCA);

        EmptyVertex vB = new EmptyVertex();
        vB.setBuildingBlockType(BBType.FRAGMENT);
        vB.addAP(APCA);
        
        // NB "all" in "ConstrainAll" means that a single constraint covers all APs
        APMapping constrain = new APMapping();
        constrain.put(vA.getAP(0), vB.getAP(0));
        APMapFinder apmf = new APMapFinder(fs,vA,vB,constrain,true,false,false);
        
        assertTrue(apmf.foundMapping());
        assertEquals(1, apmf.getAllAPMappings().size());
        assertTrue(apmf.getChosenAPMapping().containsKey(vA.getAP(0)));
        assertEquals(vB.getAP(0), apmf.getChosenAPMapping().get(vA.getAP(0)));
    }

//------------------------------------------------------------------------------
    
    @Test
    public void testAPMapFinder() throws Exception
    {
        FragmentSpace fs = prepare();
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
        
        APMapFinder apmf = new APMapFinder(fs, vA, vB, true);
        
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
        apmf = new APMapFinder(fs, vA, vB, null, true, true, true);
        
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
        
        DGraph g = new DGraph();
        g.addVertex(vA);
        g.addVertex(vB);
        g.addVertex(vC);
        g.addVertex(vD);
        g.addEdge(new Edge(vA.getAP(0), vC.getAP(0)));
        g.addEdge(new Edge(vA.getAP(1), vD.getAP(1)));
        g.addEdge(new Edge(vB.getAP(1), vC.getAP(1)));
        g.addEdge(new Edge(vB.getAP(0), vD.getAP(0)));
        
        // NB: when APs are used the mapping is much more restrictive,
        // because it is forced to respect the APClass compatibility rules.

        apmf = new APMapFinder(fs, vA, vB, true);
        
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
        
        DGraph g2 = new DGraph();
        g2.addVertex(vF);
        g2.addVertex(vG);
        g2.addEdge(new Edge(vF.getAP(0), vG.getAP(0)));
        
        apmf = new APMapFinder(fs, vF, vG, true);
        
        assertTrue(apmf.foundMapping());
        assertEquals(vG.getAP(0), apmf.getChosenAPMapping().get(vF.getAP(0)));
    }
    
//------------------------------------------------------------------------------
    
}
