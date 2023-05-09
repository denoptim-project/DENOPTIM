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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.graph.APClass;
import denoptim.graph.DGraph;
import denoptim.graph.Fragment;
import denoptim.graph.Ring;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;
import denoptim.io.DenoptimIO;
import denoptim.molecularmodeling.ThreeDimTreeBuilder;
import denoptim.utils.Randomizer;

/**
 * Unit test for the iterator over random combination of rings.
 * 
 * @author Marco Foscato
 */

public class RandomCombOfRingIteratorTest 
{
    
//------------------------------------------------------------------------------
    
    @Test
    public void testEvaluateConstitutionalClosability() throws Exception 
    {
        /*
         * Make the test graph (make it here so we have direct access to vertexes)
         *      
         *        RCV_P  RCV_P          RCV_M             RCV_P  RCV_M
         *         |      |              |                  |      |
         * RCV_M--[O]----[C]--[C]--[C]--[C]--[C]--[C]--[C]--[C]----[N]--RCV_M
         *        vO     vC   vC2  vC3  vC4  vC5  vC6  cV7  vC8    vN
         *                                    |
         *                                   RCV_P
         */
        
        APClass apc = APClass.make("A", 0);

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
        
        Fragment vC4 = vC.clone();
        vC4.setVertexId(34);
        
        Fragment vC5 = vC.clone();
        vC5.setVertexId(35);

        Fragment vC6 = vC2.clone();
        vC6.setVertexId(36);
        
        Fragment vC7 = vC2.clone();
        vC7.setVertexId(37);
        
        Fragment vC8 = vC.clone();
        vC8.setVertexId(3);
        
        IAtomContainer iacN = builder.newAtomContainer();
        IAtom aN = new Atom("N",new Point3d(0,0,0));
        iacN.addAtom(aN);
        Fragment vN = new Fragment(4, iacN,BBType.FRAGMENT);
        vN.addAP(0, new Point3d(0,-1,0), apc);
        vN.addAP(0, new Point3d(2,0,0), apc);
        vN.addAP(0, new Point3d(0,1,0), apc);
        
        APClass atMinus = APClass.make(APClass.ATMINUS, 0);
        
        IAtomContainer iacD = builder.newAtomContainer();
        iacD.addAtom(new PseudoAtom(APClass.RCALABELPERAPCLASS.get(atMinus),
                new Point3d(0,0,0)));
        Fragment rcvM = new Fragment(5, iacD,BBType.FRAGMENT);
        rcvM.addAP(0, new Point3d(-1,0,0), atMinus);
        rcvM.setAsRCV(true);
        
        Fragment rcvM2 = rcvM.clone();
        rcvM2.setVertexId(6);
        
        Fragment rcvM3 = rcvM.clone();
        rcvM3.setVertexId(7);
        
        Fragment rcvM4 = rcvM.clone();
        rcvM4.setVertexId(11);
        
        APClass atPlus = APClass.make(APClass.ATPLUS, 0);
        
        IAtomContainer iacE = builder.newAtomContainer();
        iacE.addAtom(new PseudoAtom(APClass.RCALABELPERAPCLASS.get(atPlus),
                new Point3d(0,0,0)));
        Fragment rcvP = new Fragment(8, iacE,BBType.FRAGMENT);
        rcvP.addAP(0, new Point3d(-1,0,0), atPlus);
        rcvP.setAsRCV(true);
        
        Fragment rcvP2 = rcvP.clone();
        rcvP2.setVertexId(9);
        
        Fragment rcvP3 = rcvP.clone();
        rcvP3.setVertexId(10);
        
        Fragment rcvP4 = rcvP.clone();
        rcvP4.setVertexId(12);
    
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
        graph.appendVertexOnAP(vC4.getAP(2), rcvM4.getAP(0));
        graph.appendVertexOnAP(vC5.getAP(1), vC6.getAP(0));
        graph.appendVertexOnAP(vC5.getAP(2), rcvP4.getAP(0));
        graph.appendVertexOnAP(vC6.getAP(1), vC7.getAP(0));
        graph.appendVertexOnAP(vC7.getAP(1), vC8.getAP(0));
        graph.appendVertexOnAP(vC8.getAP(1), rcvP3.getAP(0));
        graph.appendVertexOnAP(vC8.getAP(2), vN.getAP(0));
        graph.appendVertexOnAP(vN.getAP(1), rcvM2.getAP(0));
        graph.appendVertexOnAP(vN.getAP(2), rcvM3.getAP(0));
        
        // Prepare all that it is needed to run a ring-size management case

        Logger logger = Logger.getLogger("DummyLogger");
        Randomizer rng = new Randomizer();
        
        //DenoptimIO.writeGraphToSDF(new File("/tmp/graph.sdf"), graph, false, logger, rng);
        
        ThreeDimTreeBuilder t3d = new ThreeDimTreeBuilder(logger, rng);
        t3d.setAlignBBsIn3D(false); //3D not needed
        IAtomContainer mol = t3d.convertGraphTo3DAtomContainer(graph, true);
       
        // Fragment space allows all pairs of complementary RCVs to form chords
        HashMap<APClass,ArrayList<APClass>> cpMap = 
                new HashMap<APClass,ArrayList<APClass>>();
        ArrayList<APClass> lstA = new ArrayList<APClass>();
        lstA.add(apc);
        cpMap.put(apc, lstA);
        FragmentSpaceParameters fsp = new FragmentSpaceParameters();
        FragmentSpace fs = new FragmentSpace(fsp,
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(), 
                cpMap, 
                new HashMap<APClass,APClass>(), 
                new HashSet<APClass>(),
                cpMap);
        fs.setAPclassBasedApproach(true);
        
        // Allow formation of 5- to 7-membered rings
        RingClosureParameters rcParams = new RingClosureParameters();
        List<Integer> biases = new ArrayList<Integer>();
        for (int i=0; i<4; i++)
        {
            biases.add(0); // 0 meand not allowed
        }
        for (int i=4; i<8; i++)
        {
            biases.add(1); // 1 means allow with weight = 1
        }
        for (int i=8; i<10; i++)
        {
            biases.add(0); // 0 meand not allowed
        }
        rcParams.setRingSizeBias(biases);
        
        // uncomment to log on console
        //rcParams.startConsoleLogger("logger");
        //rcParams.setVerbosity(2);
        
        RandomCombOfRingsIterator iter = new RandomCombOfRingsIterator(mol,
                graph, 
                6, // max number of rings we are allowed to close
                fs, rcParams);
        
        List<Integer> foundRingSizes = new ArrayList<Integer>();
        int maxCheckedSize = 15;
        for (int i=0; i<maxCheckedSize; i++)
        {
            foundRingSizes.add(0);
        }
        boolean log = false;
        for (int i=0; i<50; i++)
        {
            List<Ring> rings = iter.next();
            assertTrue(rings.size()>1);
            for (Ring ring : rings)
            {
                foundRingSizes.set(ring.getSize(), foundRingSizes.get(ring.getSize())+1);
            }
            
            if (log)
            {
                System.out.println("Combination "+i);
                for (Ring ring : rings)
                {
                    System.out.println(" -> " + ring.getHeadVertex().getVertexId()
                            + "---" + ring.getTailVertex().getVertexId()
                            + " (" + ring.getSize() + ")");
                }
                System.out.println(" ");
            }
        }
        
        if (log)
        {
            System.out.println("RingSizes");
            for (int i=0; i<maxCheckedSize; i++)
                System.out.println("  "+i+" = "+foundRingSizes.get(i));
        }
        
        // NB: the counts include the RCA atoms, so the actual ring size is N-2
        for (int i=0; i<6; i++)
        {
            assertTrue(foundRingSizes.get(i) == 0);
        }
        assertTrue(foundRingSizes.get(6) > 0); // 4-membered rings
        assertTrue(foundRingSizes.get(7) > 0);
        assertTrue(foundRingSizes.get(8) > 0);
        assertTrue(foundRingSizes.get(9) == 0);
        assertTrue(foundRingSizes.get(10) == 0);
        assertTrue(foundRingSizes.get(11) > 0);
        assertTrue(foundRingSizes.get(12) > 0);
        assertTrue(foundRingSizes.get(13) == 0);
        assertTrue(foundRingSizes.get(14) == 0);
    }
	
//------------------------------------------------------------------------------

}
