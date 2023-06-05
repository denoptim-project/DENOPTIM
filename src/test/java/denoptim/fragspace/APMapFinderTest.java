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
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import denoptim.exception.DENOPTIMException;
import denoptim.graph.APClass;
import denoptim.graph.APMapping;
import denoptim.graph.AttachmentPoint;
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
    
    /*
     * Here we test the change of mapping with different APClass compatibility
     * matrixes.
     */
    @Test
    public void testFindMappingCompatibileAPs() throws Exception
    {
        APClass APCO0 = APClass.make("o", 0);
        APClass APCP0 = APClass.make("p", 0);
        APClass APCO1 = APClass.make("o", 1);
        APClass APCP1 = APClass.make("p", 1);
        APClass APCQ0 = APClass.make("q", 0);
        APClass APCQ1 = APClass.make("q", 1);
        
        // Build a pair naked vertexes
        EmptyVertex f0 = new EmptyVertex(0);
        f0.addAP(APCO0);
        f0.addAP(APCP0);
        f0.addAP(APCP1);

        EmptyVertex f1 = new EmptyVertex(0);
        f1.addAP(APCO0);
        f1.addAP(APCP0);
        f1.addAP(APCP1);

        EmptyVertex f2 = new EmptyVertex(0);
        f2.addAP(APCP0);
        f2.addAP(APCQ0);
        f2.addAP(APCQ1);

        EmptyVertex f3 = new EmptyVertex(0);
        f3.addAP(APCQ0);
        f3.addAP(APCQ1);
        
        EmptyVertex f4 = new EmptyVertex(0);
        f4.addAP(APCQ1);
        
        // Build a simple graph
        EmptyVertex v0 = new EmptyVertex(0);
        v0.addAP(APCO0);
        v0.addAP(APCP0);

        EmptyVertex v1 = new EmptyVertex(1);
        v1.addAP(APCO1);
        
        EmptyVertex v2 = new EmptyVertex(2);
        v2.addAP(APCP1);
        
        DGraph gA = new DGraph();
        gA.addVertex(v0);
        gA.appendVertexOnAP(v0.getAP(0), v1.getAP(0));
        gA.appendVertexOnAP(v0.getAP(1), v2.getAP(0));
        

        /*
         * No APClass compatibility: all is compatible with all!
         */
        HashMap<APClass,APClass> capMap = new HashMap<APClass,APClass>();
        HashSet<APClass> forbEnds = new HashSet<APClass>();
        HashMap<APClass,ArrayList<APClass>> cpMap = 
                new HashMap<APClass,ArrayList<APClass>>();
        
        FragmentSpaceParameters fsp = new FragmentSpaceParameters();
        FragmentSpace fs = new FragmentSpace(fsp,
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(), 
                cpMap, capMap, forbEnds, cpMap);
        
        LinkedHashMap<AttachmentPoint,List<AttachmentPoint>> apCompatilities =
                APMapFinder.findMappingCompatibileAPs(
                        v0.getAttachmentPoints(), 
                        f0.getAttachmentPoints(), false, fs);
        
        assertEquals(2, apCompatilities.size()); // must be equal to #APs
        assertEquals(3, apCompatilities.get(v0.getAP(0)).size());
        assertEquals(3, apCompatilities.get(v0.getAP(1)).size());
        
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                f0.getAttachmentPoints(), f0.getAttachmentPoints(), false, fs);
        
        assertEquals(3, apCompatilities.size());
        assertEquals(3, apCompatilities.get(f0.getAP(0)).size());
        assertEquals(3, apCompatilities.get(f0.getAP(1)).size());
        assertEquals(3, apCompatilities.get(f0.getAP(2)).size());
        
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                f0.getAttachmentPoints(), f2.getAttachmentPoints(), false, fs);
        
        assertEquals(3, apCompatilities.size());
        assertEquals(3, apCompatilities.get(f0.getAP(0)).size());
        assertEquals(3, apCompatilities.get(f0.getAP(1)).size());
        assertEquals(3, apCompatilities.get(f0.getAP(2)).size());
        
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                f2.getAttachmentPoints(), f0.getAttachmentPoints(), false, fs);
        
        assertEquals(3, apCompatilities.size());
        assertEquals(3, apCompatilities.get(f2.getAP(0)).size());
        assertEquals(3, apCompatilities.get(f2.getAP(1)).size());
        assertEquals(3, apCompatilities.get(f2.getAP(2)).size());
        
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                v2.getAttachmentPoints(), f0.getAttachmentPoints(), false, fs);
        
        assertEquals(1, apCompatilities.size());
        assertEquals(3, apCompatilities.get(v2.getAP(0)).size());

        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                f0.getAttachmentPoints(), f1.getAttachmentPoints(), false, fs);
        
        assertEquals(3, apCompatilities.size()); // must be equal to #APs
        assertEquals(3, apCompatilities.get(f0.getAP(0)).size());
        assertEquals(3, apCompatilities.get(f0.getAP(1)).size());
        assertEquals(3, apCompatilities.get(f0.getAP(2)).size());

        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                f0.getAttachmentPoints(), f3.getAttachmentPoints(), false, fs);
        
        assertEquals(3, apCompatilities.size()); // must be equal to #APs
        assertEquals(2, apCompatilities.get(f0.getAP(0)).size());
        assertEquals(2, apCompatilities.get(f0.getAP(1)).size());
        assertEquals(2, apCompatilities.get(f0.getAP(2)).size());

        /*
         * Compatibility matrix is empty: AP mapping is guided only by APClasses
         * being equal.
         */

        fs.setAPclassBasedApproach(true);
        
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                        v0.getAttachmentPoints(), 
                        f0.getAttachmentPoints(), false, fs);
        
        assertEquals(2, apCompatilities.size());
        // the sole AP with same APClass
        assertEquals(1, apCompatilities.get(v0.getAP(0)).size()); 
        // the sole AP with same APClass
        assertEquals(1, apCompatilities.get(v0.getAP(1)).size());
        
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                f0.getAttachmentPoints(), f0.getAttachmentPoints(), false, fs);
        
        assertEquals(3, apCompatilities.size());
        assertEquals(1, apCompatilities.get(f0.getAP(0)).size());
        assertEquals(1, apCompatilities.get(f0.getAP(1)).size());
        assertEquals(1, apCompatilities.get(f0.getAP(2)).size());
        
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                f0.getAttachmentPoints(), f0.getAttachmentPoints(), true, fs);
        
        assertEquals(3, apCompatilities.size());
        assertEquals(3, apCompatilities.get(f0.getAP(0)).size());
        assertEquals(3, apCompatilities.get(f0.getAP(1)).size());
        assertEquals(3, apCompatilities.get(f0.getAP(2)).size());
        
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                f0.getAttachmentPoints(), f2.getAttachmentPoints(), false, fs);
        
        assertEquals(1, apCompatilities.size()); //only the one with same APClass
        
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                f2.getAttachmentPoints(), f0.getAttachmentPoints(), false, fs);
        
        assertEquals(1, apCompatilities.size());
        
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                f0.getAttachmentPoints(), f2.getAttachmentPoints(), true, fs);
        
        assertEquals(3, apCompatilities.size());
        assertEquals(3, apCompatilities.get(f0.getAP(0)).size());
        assertEquals(3, apCompatilities.get(f0.getAP(1)).size());
        assertEquals(3, apCompatilities.get(f0.getAP(2)).size());
        
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                f0.getAttachmentPoints(), f1.getAttachmentPoints(), false, fs);
        
        assertEquals(3, apCompatilities.size());
        assertEquals(1, apCompatilities.get(f0.getAP(0)).size());
        assertEquals(1, apCompatilities.get(f0.getAP(1)).size());
        assertEquals(1, apCompatilities.get(f0.getAP(2)).size());
        
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                v0.getAttachmentPoints(), 
                f0.getAttachmentPoints(), false, fs);
        
        assertEquals(2, apCompatilities.size());
        assertEquals(1, apCompatilities.get(v0.getAP(0)).size()); 
        assertEquals(f0.getAP(0), apCompatilities.get(v0.getAP(0)).get(0)); 
        assertEquals(1, apCompatilities.get(v0.getAP(1)).size());
        assertEquals(f0.getAP(1), apCompatilities.get(v0.getAP(1)).get(0)); 
        
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                v2.getAttachmentPoints(), f0.getAttachmentPoints(), false, fs);
        
        assertEquals(1, apCompatilities.size());
        assertEquals(1, apCompatilities.get(v2.getAP(0)).size());
        
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                f0.getAttachmentPoints(), f3.getAttachmentPoints(), false, fs);
        
        assertEquals(0, apCompatilities.size());
        
        /*
         * Filling APClass compatibility (to some extent)
         */
        cpMap = new HashMap<APClass,ArrayList<APClass>>();
        ArrayList<APClass> lstO = new ArrayList<APClass>();
        lstO.add(APCO1);
        cpMap.put(APCO0, lstO);
        ArrayList<APClass> lstP = new ArrayList<APClass>();
        lstP.add(APCO1);
        cpMap.put(APCP0, lstP);
        
        /* Compatibility matrix
         * 
         *       | O0 | O1 | P0 | P1 | Q0 | Q1 | 
         *    -----------------------------------
         *    O0 |    | T  |    |    |    |    | 
         *    -----------------------------------
         *    O1 |    |    |    |    |    |    |  
         *    -----------------------------------
         *    P0 |    | T  |    |    |    |    |   
         *    -----------------------------------
         *    P1 |    |    |    |    |    |    |  
         *    -----------------------------------
         *    Q0 |    |    |    |    |    |    |   
         *    -----------------------------------
         *    Q1 |    |    |    |    |    |    | 
         */
        
        fsp = new FragmentSpaceParameters();
        fs = new FragmentSpace(fsp,
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(), 
                cpMap, capMap, forbEnds, cpMap);
        
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                v0.getAttachmentPoints(), 
                f0.getAttachmentPoints(), false, fs);
        
        assertEquals(2, apCompatilities.size());
        assertEquals(1, apCompatilities.get(v0.getAP(0)).size()); 
        assertEquals(f0.getAP(0), apCompatilities.get(v0.getAP(0)).get(0)); 
        assertEquals(1, apCompatilities.get(v0.getAP(1)).size());
        assertEquals(f0.getAP(1), apCompatilities.get(v0.getAP(1)).get(0)); 
        
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                f0.getAttachmentPoints(), f0.getAttachmentPoints(), false, fs);
        
        assertEquals(3, apCompatilities.size());
        assertEquals(1, apCompatilities.get(f0.getAP(0)).size());
        assertEquals(1, apCompatilities.get(f0.getAP(1)).size());
        assertEquals(1, apCompatilities.get(f0.getAP(2)).size());
        
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                v2.getAttachmentPoints(), f0.getAttachmentPoints(), false, fs);
        
        assertEquals(1, apCompatilities.size());
        assertEquals(1, apCompatilities.get(v2.getAP(0)).size());
        
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                f0.getAttachmentPoints(), f1.getAttachmentPoints(), false, fs);
        
        assertEquals(3, apCompatilities.size());
        assertEquals(1, apCompatilities.get(f0.getAP(0)).size());
        assertEquals(1, apCompatilities.get(f0.getAP(1)).size());
        assertEquals(1, apCompatilities.get(f0.getAP(2)).size());
        
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                f0.getAttachmentPoints(), f3.getAttachmentPoints(), false, fs);
        
        assertEquals(0, apCompatilities.size());
        
        /*
         * Filling APClass compatibility (to some larger extent)
         */
        cpMap = new HashMap<APClass,ArrayList<APClass>>();
        lstO = new ArrayList<APClass>();
        lstO.add(APCO1);
        lstO.add(APCP1);
        cpMap.put(APCO0, lstO);
        lstP = new ArrayList<APClass>();
        lstP.add(APCO1);
        lstP.add(APCP1);
        cpMap.put(APCP0, lstP);
        ArrayList<APClass> lstQ = new ArrayList<APClass>();
        lstQ.add(APCO1);
        lstQ.add(APCP1);
        cpMap.put(APCQ0, lstQ);
        
        /* Compatibility matrix
         * 
         *       | O0 | O1 | P0 | P1 | Q0 | Q1 | 
         *    -----------------------------------
         *    O0 |    | T  |    |  T |    |    | 
         *    -----------------------------------
         *    O1 |    |    |    |    |    |    |  
         *    -----------------------------------
         *    P0 |    | T  |    |  T |    |    |   
         *    -----------------------------------
         *    P1 |    |    |    |    |    |    |  
         *    -----------------------------------
         *    Q0 |    | T  |    |  T |    |    |   
         *    -----------------------------------
         *    Q1 |    |    |    |    |    |    | 
         */
        
        fsp = new FragmentSpaceParameters();
        fs = new FragmentSpace(fsp,
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(), 
                cpMap, capMap, forbEnds, cpMap);
        
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                v0.getAttachmentPoints(), 
                f0.getAttachmentPoints(), false, fs);
        
        assertEquals(2, apCompatilities.size());
        assertEquals(1, apCompatilities.get(v0.getAP(0)).size()); 
        assertEquals(f0.getAP(0), apCompatilities.get(v0.getAP(0)).get(0)); 
        assertEquals(1, apCompatilities.get(v0.getAP(1)).size());
        assertEquals(f0.getAP(1), apCompatilities.get(v0.getAP(1)).get(0)); 
        
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                f0.getAttachmentPoints(), f0.getAttachmentPoints(), false, fs);
        
        assertEquals(3, apCompatilities.size());
        assertEquals(1, apCompatilities.get(f0.getAP(0)).size());
        assertEquals(1, apCompatilities.get(f0.getAP(1)).size());
        assertEquals(1, apCompatilities.get(f0.getAP(2)).size());
        
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                v2.getAttachmentPoints(), f0.getAttachmentPoints(), false, fs);
        
        assertEquals(1, apCompatilities.size());
        assertEquals(1, apCompatilities.get(v2.getAP(0)).size());
        
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                f0.getAttachmentPoints(), f1.getAttachmentPoints(), false, fs);
        
        assertEquals(3, apCompatilities.size());
        assertEquals(1, apCompatilities.get(f0.getAP(0)).size());
        assertEquals(1, apCompatilities.get(f0.getAP(1)).size());
        assertEquals(1, apCompatilities.get(f0.getAP(2)).size());
        
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                f0.getAttachmentPoints(), f3.getAttachmentPoints(), false, fs);

        assertEquals(0, apCompatilities.size());
        
        
        /*
         * Forom here I build exactly the system that I want to test, because 
         * this way it is easier to see what the expected result should be.
         */

        gA = new DGraph();
        
        v0 = new EmptyVertex(0);
        v0.addAP(APCO0);

        v1 = new EmptyVertex(1);
        v1.addAP(APCO1);
        
        gA.addVertex(v0);
        gA.appendVertexOnAP(v0.getAP(0), v1.getAP(0));
        
        DGraph gB = new DGraph();
        
        v2 = new EmptyVertex(2);
        v2.addAP(APCP0);

        EmptyVertex v3 = new EmptyVertex(3);
        v3.addAP(APCP1);
        
        gB.addVertex(v2);
        gB.appendVertexOnAP(v2.getAP(0), v3.getAP(0));
        
        cpMap = new HashMap<APClass,ArrayList<APClass>>();
        lstO = new ArrayList<APClass>();
        lstO.add(APCO1);
        cpMap.put(APCO0, lstO);
        lstP = new ArrayList<APClass>();
        lstP.add(APCP1);
        cpMap.put(APCP0, lstP);
        
        /* Compatibility matrix
         * 
         *       | O0 | O1 | P0 | P1 | Q0 | Q1 | 
         *    -----------------------------------
         *    O0 |    | T  |    |    |    |    | 
         *    -----------------------------------
         *    O1 |    |    |    |    |    |    |  
         *    -----------------------------------
         *    P0 |    |    |    |  T |    |    |   
         *    -----------------------------------
         *    P1 |    |    |    |    |    |    |  
         *    -----------------------------------
         *    Q0 |    |    |    |    |    |    |   
         *    -----------------------------------
         *    Q1 |    |    |    |    |    |    | 
         */        
        
        fsp = new FragmentSpaceParameters();
        fs = new FragmentSpace(fsp,
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(), 
                cpMap, 
                new HashMap<APClass,APClass>(), 
                new HashSet<APClass>(), 
                new HashMap<APClass,ArrayList<APClass>>());
        
        // Both fragment have only one AP and has SRC role, 
        // but CPMap says incompatible
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                        v0.getAttachmentPoints(), 
                        v2.getAttachmentPoints(), false, fs);
        assertEquals(0, apCompatilities.size());
        
        // SRC and TRG roles, 
        // but CPMap says incompatible
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                        v0.getAttachmentPoints(), 
                        v3.getAttachmentPoints(), false, fs);
        assertEquals(0, apCompatilities.size());

        // TRG and SRC roles, 
        // but CPMap says incompatible
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                        v1.getAttachmentPoints(), 
                        v2.getAttachmentPoints(), false, fs);
        assertEquals(0, apCompatilities.size());
        
        // Both fragment have only one AP and has TRG role, 
        // but CPMap says incompatible
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                        v1.getAttachmentPoints(), 
                        v3.getAttachmentPoints(), false, fs);
        assertEquals(0, apCompatilities.size());
        
        // Add Compatibility, but incomplete)
        
        cpMap = new HashMap<APClass,ArrayList<APClass>>();
        lstO = new ArrayList<APClass>();
        lstO.add(APCO1);
        cpMap.put(APCO0, lstO);
        lstP = new ArrayList<APClass>();
        lstP.add(APCO1);
        lstP.add(APCP1);
        cpMap.put(APCP0, lstP);
        
        /* Compatibility matrix
         * 
         *       | O0 | O1 | P0 | P1 | Q0 | Q1 | 
         *    -----------------------------------
         *    O0 |    | T  |    |    |    |    | 
         *    -----------------------------------
         *    O1 |    |    |    |    |    |    |  
         *    -----------------------------------
         *    P0 |    | T  |    |  T |    |    |   
         *    -----------------------------------
         *    P1 |    |    |    |    |    |    |  
         *    -----------------------------------
         *    Q0 |    |    |    |    |    |    |   
         *    -----------------------------------
         *    Q1 |    |    |    |    |    |    | 
         */
        
        fsp = new FragmentSpaceParameters();
        fs = new FragmentSpace(fsp,
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(), 
                cpMap, 
                new HashMap<APClass,APClass>(), 
                new HashSet<APClass>(), 
                new HashMap<APClass,ArrayList<APClass>>());
        
        // Both fragment have only one AP and has SRC role, 
        // but CPMap says incompatible
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                        v0.getAttachmentPoints(), 
                        v2.getAttachmentPoints(), false, fs);
        assertEquals(0, apCompatilities.size());
        
        // SRC and TRG roles, 
        // but CPMap says incompatible
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                        v0.getAttachmentPoints(), 
                        v3.getAttachmentPoints(), false, fs);
        assertEquals(0, apCompatilities.size());

        // TRG and SRC roles, 
        // but CPMap says incompatible
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                        v1.getAttachmentPoints(), 
                        v2.getAttachmentPoints(), false, fs);
        assertEquals(0, apCompatilities.size());
        
        // Both fragment have only one AP and has TRG role, 
        // but CPMap says incompatible
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                        v1.getAttachmentPoints(), 
                        v3.getAttachmentPoints(), false, fs);
        assertEquals(0, apCompatilities.size());
        

        // Add Cross-compatibility
        
        cpMap = new HashMap<APClass,ArrayList<APClass>>();
        lstO = new ArrayList<APClass>();
        lstO.add(APCO1);
        lstO.add(APCP1);
        cpMap.put(APCO0, lstO);
        lstP = new ArrayList<APClass>();
        lstP.add(APCO1);
        lstP.add(APCP1);
        cpMap.put(APCP0, lstP);
        
        /* Compatibility matrix
         * 
         *       | O0 | O1 | P0 | P1 | Q0 | Q1 | 
         *    -----------------------------------
         *    O0 |    | T  |    |  T |    |    | 
         *    -----------------------------------
         *    O1 |    |    |    |    |    |    |  
         *    -----------------------------------
         *    P0 |    | T  |    |  T |    |    |   
         *    -----------------------------------
         *    P1 |    |    |    |    |    |    |  
         *    -----------------------------------
         *    Q0 |    |    |    |    |    |    |   
         *    -----------------------------------
         *    Q1 |    |    |    |    |    |    | 
         */
        
        fsp = new FragmentSpaceParameters();
        fs = new FragmentSpace(fsp,
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(), 
                cpMap, 
                new HashMap<APClass,APClass>(), 
                new HashSet<APClass>(), 
                new HashMap<APClass,ArrayList<APClass>>());
        
        // Both fragment have only one AP and has SRC role, 
        // and CPMap says they are compatible
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                        v0.getAttachmentPoints(), 
                        v2.getAttachmentPoints(), false, fs);
        assertEquals(1, apCompatilities.size());
        assertEquals(1, apCompatilities.get(v0.getAP(0)).size());
        assertEquals(v2.getAP(0), apCompatilities.get(v0.getAP(0)).get(0));
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                        v2.getAttachmentPoints(), 
                        v0.getAttachmentPoints(), false, fs);
        assertEquals(1, apCompatilities.size());
        assertEquals(1, apCompatilities.get(v2.getAP(0)).size());
        assertEquals(v0.getAP(0), apCompatilities.get(v2.getAP(0)).get(0));
        
        // SRC and TRG roles, 
        // but CPMap says incompatible
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                        v0.getAttachmentPoints(), 
                        v3.getAttachmentPoints(), false, fs);
        assertEquals(0, apCompatilities.size());

        // TRG and SRC roles, 
        // but CPMap says incompatible
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                        v1.getAttachmentPoints(), 
                        v2.getAttachmentPoints(), false, fs);
        assertEquals(0, apCompatilities.size());
        
        // Both fragment have only one AP and has TRG role, 
        // and CPMap says they are compatible
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                        v1.getAttachmentPoints(), 
                        v3.getAttachmentPoints(), false, fs);
        assertEquals(1, apCompatilities.size());
        assertEquals(1, apCompatilities.get(v1.getAP(0)).size());
        assertEquals(v3.getAP(0), apCompatilities.get(v1.getAP(0)).get(0));
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                        v3.getAttachmentPoints(), 
                        v1.getAttachmentPoints(), false, fs);
        assertEquals(1, apCompatilities.size());
        assertEquals(1, apCompatilities.get(v3.getAP(0)).size());
        assertEquals(v1.getAP(0), apCompatilities.get(v3.getAP(0)).get(0));
        

        // Add cross and backwards compatibility
        
        cpMap = new HashMap<APClass,ArrayList<APClass>>();
        ArrayList<APClass> lstO0 = new ArrayList<APClass>();
        lstO0.add(APCO1);
        lstO0.add(APCP1);
        lstO0.add(APCP0);
        lstO0.add(APCQ1);
        cpMap.put(APCO0, lstO0);
        ArrayList<APClass> lstP0 = new ArrayList<APClass>();
        lstP0.add(APCO1);
        lstP0.add(APCP1);
        lstP0.add(APCO0);
        cpMap.put(APCP0, lstP0);
        ArrayList<APClass> lstO1 = new ArrayList<APClass>();
        lstO1.add(APCO0);
        lstO1.add(APCP0);
        lstO1.add(APCP1);
        cpMap.put(APCO1, lstO1);
        ArrayList<APClass> lstP1 = new ArrayList<APClass>();
        lstP1.add(APCO0);
        lstP1.add(APCP0);
        lstP1.add(APCO1);
        cpMap.put(APCP1, lstP1);
        ArrayList<APClass> lstQ1 = new ArrayList<APClass>();
        lstQ1.add(APCO0);
        lstQ1.add(APCO1);
        lstQ1.add(APCP0);
        lstQ1.add(APCP1);
        cpMap.put(APCQ1, lstQ1);
        
        /* Compatibility matrix
         * 
         *       | O0 | O1 | P0 | P1 | Q0 | Q1 | 
         *    -----------------------------------
         *    O0 |    | T  |    |  T |    |  T | 
         *    -----------------------------------
         *    O1 |  T |    | T  |    |    |    |  
         *    -----------------------------------
         *    P0 |    | T  |    |  T |    |    |   
         *    -----------------------------------
         *    P1 | T  |    |  T |    |    |    |  
         *    -----------------------------------
         *    Q0 |    |    |    |    |    |    |   
         *    -----------------------------------
         *    Q1 |  T |  T |  T |  T |    |    | 
         */
        
        fsp = new FragmentSpaceParameters();
        fs = new FragmentSpace(fsp,
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(), 
                cpMap, 
                new HashMap<APClass,APClass>(), 
                new HashSet<APClass>(), 
                new HashMap<APClass,ArrayList<APClass>>());
        
        // Both fragment have only one AP and has SRC role, 
        // and CPMap says they are compatible
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                        v0.getAttachmentPoints(), 
                        v2.getAttachmentPoints(), false, fs);
        assertEquals(1, apCompatilities.size());
        assertEquals(1, apCompatilities.get(v0.getAP(0)).size());
        assertEquals(v2.getAP(0), apCompatilities.get(v0.getAP(0)).get(0));
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                        v2.getAttachmentPoints(), 
                        v0.getAttachmentPoints(), false, fs);
        assertEquals(1, apCompatilities.size());
        assertEquals(1, apCompatilities.get(v2.getAP(0)).size());
        assertEquals(v0.getAP(0), apCompatilities.get(v2.getAP(0)).get(0));
        
        // SRC and TRG roles, 
        // but CPMap says incompatible
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                        v0.getAttachmentPoints(), 
                        v3.getAttachmentPoints(), false, fs);
        assertEquals(1, apCompatilities.size());
        assertEquals(1, apCompatilities.get(v0.getAP(0)).size());
        assertEquals(v3.getAP(0), apCompatilities.get(v0.getAP(0)).get(0));
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                        v3.getAttachmentPoints(), 
                        v0.getAttachmentPoints(), false, fs);
        assertEquals(1, apCompatilities.size());
        assertEquals(1, apCompatilities.get(v3.getAP(0)).size());
        assertEquals(v0.getAP(0), apCompatilities.get(v3.getAP(0)).get(0));

        // TRG and SRC roles, 
        // but CPMap says incompatible
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                        v1.getAttachmentPoints(), 
                        v2.getAttachmentPoints(), false, fs);
        assertEquals(1, apCompatilities.size());
        assertEquals(1, apCompatilities.get(v1.getAP(0)).size());
        assertEquals(v2.getAP(0), apCompatilities.get(v1.getAP(0)).get(0));
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                        v2.getAttachmentPoints(), 
                        v1.getAttachmentPoints(), false, fs);
        assertEquals(1, apCompatilities.size());
        assertEquals(1, apCompatilities.get(v2.getAP(0)).size());
        assertEquals(v1.getAP(0), apCompatilities.get(v2.getAP(0)).get(0));
        
        // Both fragment have only one AP and has TRG role, 
        // and CPMap says they are compatible
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                        v1.getAttachmentPoints(), 
                        v3.getAttachmentPoints(), false, fs);
        assertEquals(1, apCompatilities.size());
        assertEquals(1, apCompatilities.get(v1.getAP(0)).size());
        assertEquals(v3.getAP(0), apCompatilities.get(v1.getAP(0)).get(0));
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                v3.getAttachmentPoints(), 
                v1.getAttachmentPoints(), false, fs);
        assertEquals(1, apCompatilities.size());
        assertEquals(1, apCompatilities.get(v3.getAP(0)).size());
        assertEquals(v1.getAP(0), apCompatilities.get(v3.getAP(0)).get(0));

        // One is free other is SRC, and CPMap says compatible
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                        f4.getAttachmentPoints(), 
                        v0.getAttachmentPoints(), false, fs);
        assertEquals(0, apCompatilities.size());
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                        f4.getAttachmentPoints(), 
                        v0.getAttachmentPoints(), true, fs);
        assertEquals(1, apCompatilities.size());
        assertEquals(1, apCompatilities.size());
        assertEquals(1, apCompatilities.get(f4.getAP(0)).size());
        assertEquals(v0.getAP(0), apCompatilities.get(f4.getAP(0)).get(0));
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                        v0.getAttachmentPoints(), 
                        f4.getAttachmentPoints(), false, fs);
        assertEquals(0, apCompatilities.size());
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                        v0.getAttachmentPoints(), 
                        f4.getAttachmentPoints(), true, fs);
        assertEquals(1, apCompatilities.size());
        assertEquals(1, apCompatilities.size());
        assertEquals(1, apCompatilities.get(v0.getAP(0)).size());
        assertEquals(f4.getAP(0), apCompatilities.get(v0.getAP(0)).get(0));
        
        // One is free other is TRG, and CPMap says compatible
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                        f4.getAttachmentPoints(), 
                        v1.getAttachmentPoints(), false, fs);
        assertEquals(0, apCompatilities.size());
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                        f4.getAttachmentPoints(), 
                        v1.getAttachmentPoints(), true, fs);
        assertEquals(1, apCompatilities.size());
        assertEquals(1, apCompatilities.size());
        assertEquals(1, apCompatilities.get(f4.getAP(0)).size());
        assertEquals(v1.getAP(0), apCompatilities.get(f4.getAP(0)).get(0));
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                        v1.getAttachmentPoints(), 
                        f4.getAttachmentPoints(), false, fs);
        assertEquals(0, apCompatilities.size());
        apCompatilities = APMapFinder.findMappingCompatibileAPs(
                        v1.getAttachmentPoints(), 
                        f4.getAttachmentPoints(), true, fs);
        assertEquals(1, apCompatilities.size());
        assertEquals(1, apCompatilities.size());
        assertEquals(1, apCompatilities.get(v1.getAP(0)).size());
        assertEquals(f4.getAP(0), apCompatilities.get(v1.getAP(0)).get(0));

    }
    
//------------------------------------------------------------------------------
    
}
