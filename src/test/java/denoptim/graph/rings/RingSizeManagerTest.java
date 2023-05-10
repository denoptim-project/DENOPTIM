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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.graph.APClass;
import denoptim.graph.DGraph;
import denoptim.graph.Fragment;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;
import denoptim.molecularmodeling.ThreeDimTreeBuilder;
import denoptim.utils.Randomizer;

/**
 * Unit test for ring size manager.
 * 
 * @author Marco Foscato
 */

public class RingSizeManagerTest 
{
    
//------------------------------------------------------------------------------
    /**
     * Build a graph meant to be used in unit tests. The returned graph has
     * the following structure:
     * <pre>       
     *        RCV_P  RCV_P               RCV_P  RCV_M
     *         |      |                   |      |
     * RCV_M--[O]----[C]--[C]--[C]--[C]--[C]----[N]--RCV_M
     *        vO     vC   vC2  vC3  vC4  vC5    vN
     *  
     * </pre>
     * 
     * @return a new instance of the test graph.
     * @throws DENOPTIMException 
     */
    public static DGraph makeTestGraphA() throws DENOPTIMException 
    {
        APClass apc = APClass.make("A",0);

        IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();

        IAtomContainer iacO = builder.newAtomContainer();
        IAtom aO = new Atom("O",new Point3d(0,0,0));
        iacO.addAtom(aO);
        Fragment vO = new Fragment(0, iacO,BBType.FRAGMENT);
        vO.addAP(0, new Point3d(0,-1,0), apc);
        vO.addAP(0, new Point3d(2,0,0), apc);
        vO.addAP(0, new Point3d(0,1,0), apc);
        
        IAtomContainer iacC = builder.newAtomContainer();
        IAtom aC = new Atom("C",new Point3d(0,0,0));
        iacC.addAtom(aC);
        Fragment vC = new Fragment(1, iacC,BBType.FRAGMENT);
        vC.addAP(0, new Point3d(0,-1,0), apc);
        vC.addAP(0, new Point3d(2,0,0), apc);
        vC.addAP(0, new Point3d(0,1,0), apc);
        
        IAtomContainer iacCd = builder.newAtomContainer();
        IAtom aCd = new Atom("C",new Point3d(0,0,0));
        iacCd.addAtom(aCd);
        Fragment vC2 = new Fragment(2, iacCd,BBType.FRAGMENT);
        vC2.addAP(0, new Point3d(0,-1,0), apc);
        vC2.addAP(0, new Point3d(0,1,0), apc);
        
        Fragment vC3 = vC2.clone();
        vC3.setVertexId(33);
        
        Fragment vC4 = vC2.clone();
        vC4.setVertexId(34);
        
        Fragment vC5 = vC.clone();
        vC5.setVertexId(3);
        
        IAtomContainer iacN = builder.newAtomContainer();
        IAtom aN = new Atom("N",new Point3d(0,0,0));
        iacN.addAtom(aN);
        Fragment vN = new Fragment(4, iacN,BBType.FRAGMENT);
        vN.addAP(0, new Point3d(0,-1,0), apc);
        vN.addAP(0, new Point3d(2,0,0), apc);
        vN.addAP(0, new Point3d(0,1,0), apc);
        
        APClass atMinus = APClass.RCACLASSMINUS;
        
        IAtomContainer iacD = builder.newAtomContainer();
        iacD.addAtom(new PseudoAtom(RingClosingAttractor.RCALABELPERAPCLASS.get(atMinus),
                new Point3d(0,0,0)));
        Fragment rcvM = new Fragment(5, iacD,BBType.FRAGMENT);
        rcvM.addAP(0, new Point3d(-1,0,0), atMinus);
        rcvM.setAsRCV(true);
        
        Fragment rcvM2 = rcvM.clone();
        rcvM2.setVertexId(6);
        
        Fragment rcvM3 = rcvM.clone();
        rcvM3.setVertexId(7);
        
        APClass atPlus = APClass.RCACLASSPLUS;
        
        IAtomContainer iacE = builder.newAtomContainer();
        iacE.addAtom(new PseudoAtom(RingClosingAttractor.RCALABELPERAPCLASS.get(atPlus),
                new Point3d(0,0,0)));
        Fragment rcvP = new Fragment(8, iacE,BBType.FRAGMENT);
        rcvP.addAP(0, new Point3d(-1,0,0), atPlus);
        rcvP.setAsRCV(true);
        
        Fragment rcvP2 = rcvP.clone();
        rcvP2.setVertexId(9);
        
        Fragment rcvP3 = rcvP.clone();
        rcvP3.setVertexId(10);
    
        DGraph graph = new DGraph();
        graph.addVertex(vC);
        graph.appendVertexOnAP(vC.getAP(0), vO.getAP(2));
        graph.appendVertexOnAP(vO.getAP(0), rcvM.getAP(0));
        graph.appendVertexOnAP(vO.getAP(1), rcvP.getAP(0));
        graph.appendVertexOnAP(vC.getAP(1), rcvP2.getAP(0));
        graph.appendVertexOnAP(vC.getAP(2), vC2.getAP(1));
        graph.appendVertexOnAP(vC2.getAP(0), vC3.getAP(0));
        graph.appendVertexOnAP(vC3.getAP(1), vC4.getAP(0));
        graph.appendVertexOnAP(vC4.getAP(1), vC5.getAP(0));
        graph.appendVertexOnAP(vC5.getAP(1), rcvP3.getAP(0));
        graph.appendVertexOnAP(vC5.getAP(2), vN.getAP(0));
        graph.appendVertexOnAP(vN.getAP(1), rcvM2.getAP(0));
        graph.appendVertexOnAP(vN.getAP(2), rcvM3.getAP(0));
        return graph;
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testEvaluateRCVPair() throws Exception 
    {
        // Make the test graph (make it here so we have direct access to vertexes)

        APClass apc = APClass.make("A",0);

        IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();

        IAtomContainer iacO = builder.newAtomContainer();
        IAtom aO = new Atom("O",new Point3d(0,0,0));
        iacO.addAtom(aO);
        Fragment vO = new Fragment(0, iacO,BBType.FRAGMENT);
        vO.addAP(0, new Point3d(0,-1,0), apc);
        vO.addAP(0, new Point3d(2,0,0), apc);
        vO.addAP(0, new Point3d(0,1,0), apc);
        
        IAtomContainer iacC = builder.newAtomContainer();
        IAtom aC = new Atom("C",new Point3d(0,0,0));
        iacC.addAtom(aC);
        Fragment vC = new Fragment(1, iacC,BBType.FRAGMENT);
        vC.addAP(0, new Point3d(0,-1,0), apc);
        vC.addAP(0, new Point3d(2,0,0), apc);
        vC.addAP(0, new Point3d(0,1,0), apc);
        
        IAtomContainer iacCd = builder.newAtomContainer();
        IAtom aCd = new Atom("C",new Point3d(0,0,0));
        iacCd.addAtom(aCd);
        Fragment vC2 = new Fragment(2, iacCd,BBType.FRAGMENT);
        vC2.addAP(0, new Point3d(0,-1,0), apc);
        vC2.addAP(0, new Point3d(0,1,0), apc);
        
        Fragment vC3 = vC2.clone();
        vC3.setVertexId(33);
        
        Fragment vC4 = vC2.clone();
        vC4.setVertexId(34);
        
        Fragment vC5 = vC.clone();
        vC5.setVertexId(3);
        
        IAtomContainer iacN = builder.newAtomContainer();
        IAtom aN = new Atom("N",new Point3d(0,0,0));
        iacN.addAtom(aN);
        Fragment vN = new Fragment(4, iacN,BBType.FRAGMENT);
        vN.addAP(0, new Point3d(0,-1,0), apc);
        vN.addAP(0, new Point3d(2,0,0), apc);
        vN.addAP(0, new Point3d(0,1,0), apc);
        
        APClass atMinus = APClass.RCACLASSMINUS;
        
        IAtomContainer iacD = builder.newAtomContainer();
        iacD.addAtom(new PseudoAtom(RingClosingAttractor.RCALABELPERAPCLASS.get(atMinus),
                new Point3d(0,0,0)));
        Fragment rcvM = new Fragment(5, iacD,BBType.FRAGMENT);
        rcvM.addAP(0, new Point3d(-1,0,0), atMinus);
        rcvM.setAsRCV(true);
        
        Fragment rcvM2 = rcvM.clone();
        rcvM2.setVertexId(6);
        
        Fragment rcvM3 = rcvM.clone();
        rcvM3.setVertexId(7);
        
        APClass atPlus = APClass.RCACLASSPLUS;
        
        IAtomContainer iacE = builder.newAtomContainer();
        iacE.addAtom(new PseudoAtom(RingClosingAttractor.RCALABELPERAPCLASS.get(atPlus),
                new Point3d(0,0,0)));
        Fragment rcvP = new Fragment(8, iacE,BBType.FRAGMENT);
        rcvP.addAP(0, new Point3d(-1,0,0), atPlus);
        rcvP.setAsRCV(true);
        
        Fragment rcvP2 = rcvP.clone();
        rcvP2.setVertexId(9);
        
        Fragment rcvP3 = rcvP.clone();
        rcvP3.setVertexId(10);
    
        DGraph graph = new DGraph();
        graph.addVertex(vC);
        graph.appendVertexOnAP(vC.getAP(0), vO.getAP(2));
        graph.appendVertexOnAP(vO.getAP(0), rcvM.getAP(0));
        graph.appendVertexOnAP(vO.getAP(1), rcvP.getAP(0));
        graph.appendVertexOnAP(vC.getAP(1), rcvP2.getAP(0));
        graph.appendVertexOnAP(vC.getAP(2), vC2.getAP(1));
        graph.appendVertexOnAP(vC2.getAP(0), vC3.getAP(0));
        graph.appendVertexOnAP(vC3.getAP(1), vC4.getAP(0));
        graph.appendVertexOnAP(vC4.getAP(1), vC5.getAP(0));
        graph.appendVertexOnAP(vC5.getAP(1), rcvP3.getAP(0));
        graph.appendVertexOnAP(vC5.getAP(2), vN.getAP(0));
        graph.appendVertexOnAP(vN.getAP(1), rcvM2.getAP(0));
        graph.appendVertexOnAP(vN.getAP(2), rcvM3.getAP(0));
        
        // Prepare all that it is needed to run a ring-size management case

        Logger logger = Logger.getLogger("DummyLogger");
        Randomizer rng = new Randomizer();
        
        //DenoptimIO.writeGraphToSDF(new File("/tmp/graph.sdf"), graph, false, logger, rng);
        
        ThreeDimTreeBuilder t3d = new ThreeDimTreeBuilder(logger,rng);
        t3d.setAlignBBsIn3D(false); //3D not needed
        IAtomContainer molA = t3d.convertGraphTo3DAtomContainer(graph, true);
        
        // 
        // First case: no APClass compatibility. Cannot close any ring!
        //
        FragmentSpaceParameters fsp = new FragmentSpaceParameters();
        FragmentSpace fs = new FragmentSpace(fsp,
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(), 
                new HashMap<APClass,ArrayList<APClass>>(),
                new HashMap<APClass,APClass>(), 
                new HashSet<APClass>(), 
                new HashMap<APClass,ArrayList<APClass>>());
        fs.setAPclassBasedApproach(true);
        
        RingClosureParameters rcParams = new RingClosureParameters();
        
        // uncomment to log on console
        // rcParams.startConsoleLogger("logger");
        // rcParams.setVerbosity(2);
        
        RingSizeManager rsm = new RingSizeManager(fs, rcParams);
        rsm.initialize(molA, graph);
        
        List<Vertex> wLst = rsm.getRSBiasedListOfCandidates();
        assertEquals(0, wLst.size());
        List<Vertex> rcvs = graph.getRCVertices();
        for (int i=0; i<rcvs.size(); i++)
        {
            // NB: we intentionally check also i=j
            for (int j=i; j<rcvs.size(); j++)
            {
                assertFalse(rsm.getCompatibilityOfPair(rcvs.get(i), rcvs.get(j)));
            }
        }
        
        //
        // Second case: AP compatibility allows to make rings of various sizes
        //
        
        APClass apcA = APClass.make("A",0);
        HashMap<APClass,ArrayList<APClass>> cpMap = 
                new HashMap<APClass,ArrayList<APClass>>();
        ArrayList<APClass> lstA = new ArrayList<APClass>();
        lstA.add(apcA);
        cpMap.put(apcA, lstA);
        fs = new FragmentSpace(fsp,
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(), 
                cpMap, 
                new HashMap<APClass,APClass>(), 
                new HashSet<APClass>(), 
                cpMap);
        fs.setAPclassBasedApproach(true);
        
        rsm = new RingSizeManager(fs, rcParams);
        rsm.initialize(molA, graph);
        
        wLst = rsm.getRSBiasedListOfCandidates();
        assertNotEquals(0, wLst.size());
        Map<Vertex,Set<Vertex>> expectedRCCompatibilities = 
                new HashMap<Vertex,Set<Vertex>>();
        expectedRCCompatibilities.put(rcvM, new HashSet<Vertex>());
        expectedRCCompatibilities.get(rcvM).add(rcvP3);
        expectedRCCompatibilities.put(rcvP, new HashSet<Vertex>());
        expectedRCCompatibilities.get(rcvP).add(rcvM2);
        expectedRCCompatibilities.get(rcvP).add(rcvM3);
        expectedRCCompatibilities.put(rcvP2, new HashSet<Vertex>());
        expectedRCCompatibilities.get(rcvP2).add(rcvM2);
        expectedRCCompatibilities.get(rcvP2).add(rcvM3);
        int numCompatibilities = 0;
        for (int i=0; i<rcvs.size(); i++)
        {
            Vertex vI = rcvs.get(i);
            // NB: we intentionally check also i=j
            for (int j=i; j<rcvs.size(); j++)
            {
                Vertex vJ = rcvs.get(j);
                if (rsm.getCompatibilityOfPair(vI, vJ))
                {
                    numCompatibilities++;
                    assertTrue(expectedRCCompatibilities.containsKey(vI));
                    assertTrue(expectedRCCompatibilities.get(vI).contains(vJ));
                }
            }
        }
        assertEquals(5,numCompatibilities);
        
        //
        // Third case: ring-size constrain prevent some combination of RCVs
        //
        List<Integer> biases = new ArrayList<Integer>();
        for (int i=0; i<8; i++)
        {
            biases.add(0); // 0 means prevent rings with this size!
        }
        biases.set(6, 1); // i.e., allows only 6-member rings
        rcParams.setRingSizeBias(biases);
        
        rsm = new RingSizeManager(fs, rcParams);
        rsm.initialize(molA, graph);
        
        wLst = rsm.getRSBiasedListOfCandidates();
        assertNotEquals(0, wLst.size());
        expectedRCCompatibilities = new HashMap<Vertex,Set<Vertex>>();
        expectedRCCompatibilities.put(rcvM, new HashSet<Vertex>());
        expectedRCCompatibilities.get(rcvM).add(rcvP3);
        expectedRCCompatibilities.put(rcvP2, new HashSet<Vertex>());
        expectedRCCompatibilities.get(rcvP2).add(rcvM2);
        expectedRCCompatibilities.get(rcvP2).add(rcvM3);
        numCompatibilities = 0;
        for (int i=0; i<rcvs.size(); i++)
        {
            Vertex vI = rcvs.get(i);
            // NB: we intentionally check also i=j
            for (int j=i; j<rcvs.size(); j++)
            {
                Vertex vJ = rcvs.get(j);
                if (rsm.getCompatibilityOfPair(vI, vJ))
                {
                    numCompatibilities++;
                    assertTrue(expectedRCCompatibilities.containsKey(vI));
                    assertTrue(expectedRCCompatibilities.get(vI).contains(vJ));
                }
            }
        }
        assertEquals(3, numCompatibilities);
        
        //
        // Same as before but with different preference for ring size
        //
        biases = new ArrayList<Integer>();
        for (int i=0; i<8; i++)
        {
            biases.add(0); // 0 means prevent rings with this size!
        }
        biases.set(7, 1); // i.e., allows only 7-member rings
        rcParams.setRingSizeBias(biases);
        
        rsm = new RingSizeManager(fs, rcParams);
        rsm.initialize(molA, graph);
        
        wLst = rsm.getRSBiasedListOfCandidates();
        assertNotEquals(0, wLst.size());
        expectedRCCompatibilities = new HashMap<Vertex,Set<Vertex>>();
        expectedRCCompatibilities.put(rcvP, new HashSet<Vertex>());
        expectedRCCompatibilities.get(rcvP).add(rcvM2);
        expectedRCCompatibilities.get(rcvP).add(rcvM3);
        numCompatibilities = 0;
        for (int i=0; i<rcvs.size(); i++)
        {
            Vertex vI = rcvs.get(i);
            // NB: we intentionally check also i=j
            for (int j=i; j<rcvs.size(); j++)
            {
                Vertex vJ = rcvs.get(j);
                if (rsm.getCompatibilityOfPair(vI, vJ))
                {
                    numCompatibilities++;
                    assertTrue(expectedRCCompatibilities.containsKey(vI));
                    assertTrue(expectedRCCompatibilities.get(vI).contains(vJ));
                }
            }
        }
        assertEquals(2, numCompatibilities);
        
        //
        // Same as before but with unmatched preference
        //
        biases = new ArrayList<Integer>();
        for (int i=0; i<8; i++)
        {
            biases.add(0); // 0 means prevent rings with this size!
        }
        biases.set(3, 1); // i.e., allows only 3-member rings
        rcParams.setRingSizeBias(biases);
        
        rsm = new RingSizeManager(fs, rcParams);
        rsm.initialize(molA, graph);
        
        wLst = rsm.getRSBiasedListOfCandidates();
        assertEquals(0, wLst.size());
        numCompatibilities = 0;
        for (int i=0; i<rcvs.size(); i++)
        {
            Vertex vI = rcvs.get(i);
            // NB: we intentionally check also i=j
            for (int j=i; j<rcvs.size(); j++)
            {
                Vertex vJ = rcvs.get(j);
                if (rsm.getCompatibilityOfPair(vI, vJ))
                {
                    numCompatibilities++;
                }
            }
        }
        assertEquals(0, numCompatibilities);
    }
	
//------------------------------------------------------------------------------

}
