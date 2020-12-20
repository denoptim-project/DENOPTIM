package denoptim.threedim;

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
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.Bond;

import denoptim.fragspace.FragmentSpace;
import denoptim.io.DenoptimIO;
import denoptim.molecule.APClass;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMEdge;
import denoptim.molecule.DENOPTIMEdge.BondType;
import denoptim.molecule.DENOPTIMFragment;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.utils.GraphConversionTool;

/**
 * Unit test for TreeBuilder3D
 * 
 * @author Marco Foscato
 */

public class TreeBuilder3DTest
{
	
//------------------------------------------------------------------------------
	
    @Test
    public void testConversionTo3dTree() throws Exception
    {
        APClass a0 = APClass.make("a:0");
        APClass b0 = APClass.make("b:0");
        APClass h0 = APClass.make("h:0");
        APClass ap0 = APClass.make("ATplus:0");
        APClass am0 = APClass.make("ATminus:0");
        
    	DENOPTIMFragment frg1 = new DENOPTIMFragment();
    	Atom a1 = new Atom("C", new Point3d(new double[]{0.0, 0.0, 0.0}));
    	Atom a2 = new Atom("C", new Point3d(new double[]{1.0, 0.0, 0.0}));
    	frg1.addAtom(a1);
    	frg1.addAtom(a2);
    	frg1.addBond(new Bond(a1, a2));
    	frg1.addAP(0, a0, new Point3d(new double[]{0.0, 1.0, 1.0}), 1);
    	frg1.addAP(1, a0, new Point3d(new double[]{1.0, 1.0, 1.0}), 1);
    	frg1.projectAPsToProperties(); 
    	
    	DENOPTIMFragment frg2 = new DENOPTIMFragment();
    	Atom a3 = new Atom("C", new Point3d(new double[]{0.0, 0.0, 0.0}));
    	frg2.addAtom(a3);
    	frg2.addAP(0, a0, new Point3d(new double[]{0.0, 1.0, 1.0}), 1);
    	frg2.addAP(0, b0, new Point3d(new double[]{0.0, -1.0, -1.0}), 1);   
    	frg2.projectAPsToProperties(); 

    	DENOPTIMFragment rca1 = new DENOPTIMFragment();
    	Atom a4 = new Atom("ATP", new Point3d(new double[]{0.0, 0.0, 0.0}));
    	rca1.addAtom(a4);
    	rca1.addAP(0, ap0, new Point3d(new double[]{0.0, 1.0, 1.0}), 1);
    	rca1.projectAPsToProperties(); 
    	
    	DENOPTIMFragment rca2 = new DENOPTIMFragment();
    	Atom a5 = new Atom("ATM", new Point3d(new double[]{1.0, 0.0, 0.0}));
    	rca2.addAtom(a5);
    	rca2.addAP(0, am0, new Point3d(new double[]{0.0, 1.0, 1.0}), 1);
    	rca2.projectAPsToProperties(); 
    	
    	DENOPTIMFragment cap = new DENOPTIMFragment();
    	Atom a6 = new Atom("H", new Point3d(new double[]{0.0, 0.0, 0.0}));
    	cap.addAtom(a6);
    	cap.addAP(0, h0, new Point3d(new double[]{0.0, 1.0, 1.0}), 1);
    	cap.projectAPsToProperties(); 
    	
    	ArrayList<DENOPTIMVertex> scaff = new ArrayList<DENOPTIMVertex>();
    	scaff.add(frg1);
    	ArrayList<DENOPTIMVertex> frags = new ArrayList<DENOPTIMVertex>();
    	frags.add(frg2);
    	frags.add(rca1);
    	frags.add(rca2);
    	ArrayList<DENOPTIMVertex> caps = new ArrayList<DENOPTIMVertex>();
    	caps.add(cap);
    	
    	HashMap<APClass,ArrayList<APClass>> cpMap = 
    			new HashMap<APClass,ArrayList<APClass>>();
    	cpMap.put(a0, new ArrayList<APClass>(Arrays.asList(a0, b0)));
		HashMap<String,BondType> boMap = new HashMap<String,BondType>();
		boMap.put("a", BondType.SINGLE);
		boMap.put("b", BondType.SINGLE);
		boMap.put("h", BondType.SINGLE);
		boMap.put("ATplus", BondType.SINGLE);
		boMap.put("ATminus", BondType.SINGLE);
		HashMap<APClass,APClass> capMap = new HashMap<APClass,APClass>();
		capMap.put(a0, h0);
		HashSet<APClass> forbEnds = new HashSet<APClass>();
		forbEnds.add(b0);
    	
    	FragmentSpace.defineFragmentSpace(scaff,frags,caps,cpMap,boMap,capMap,
    			forbEnds,null);
    	
    	//TODO-V3: build graph programmatically!!!
    	String graphStr = "1 1_1_0_-1,2_1_1_0,3_3_1_1,4_2_1_0, "
    			+ "1_1_2_0_1_a:0_a:0,"
    			+ "2_1_3_0_1_b:0_ATminus:0,"
    			+ "1_0_4_0_1_a:0_ATplus:0, "
    			+ "DENOPTIMRing [verteces="
    			+ "[3_3_1_1, 2_1_1_0, 1_1_0_-1, 4_2_1_0]]";
    	DENOPTIMGraph dg = GraphConversionTool.getGraphFromString(graphStr,true);

    	TreeBuilder3D t3d = new TreeBuilder3D();
    	
    	IAtomContainer mol = t3d.convertGraphTo3DAtomContainer(dg,false);
    	assertEquals(4, mol.getBondCount(), "Number of bonds without the "
    			+ "cyclic one");
    	assertEquals(5, mol.getAtomCount(), "Number of atoms in cyclic molecule"
    			+ " before forming ring");
    	
    	mol = t3d.convertGraphTo3DAtomContainer(dg,true);
    	// NB: no RCAs anymore, so two atoms less than before, and two bonds
    	// less than the moment when we have made the ring-closing bond but we
    	// have not yet removed the RCAs. Basically we add 1 bond and remove 2
    	assertEquals(3, mol.getBondCount(), "Number of bonds, including the "
    			+ "cyclic one");
    	assertEquals(3, mol.getAtomCount(), "Number of atoms in cyclic "
    			+ "molecule after removal of RCAs");
    	
    	graphStr = "1 1_1_0_-1,2_1_1_0,3_3_1_1,4_2_1_0, "
    			+ "1_1_2_0_1_a:0_a:0,"
    			+ "2_1_3_0_1_b:0_ATminus:0,"
    			+ "1_0_4_0_1_a:0_ATplus:0, ";
    	DENOPTIMGraph acyclicGraph = GraphConversionTool.getGraphFromString(
    			graphStr,true);
    	
    	IAtomContainer acyclicMol = t3d.convertGraphTo3DAtomContainer(
    			acyclicGraph,false);
    	assertEquals(4, acyclicMol.getBondCount(), "Number of bonds in acyclic "
    			+ "graph with RCAs");
    	assertEquals(5, acyclicMol.getAtomCount(), "Number of atoms in acyclic "
    			+ "molecule before forming ring");
    	
    	acyclicMol = t3d.convertGraphTo3DAtomContainer(acyclicGraph,true);
    	assertEquals(3, acyclicMol.getBondCount(), "Number of bonds, after "
    			+ "replacing RCA with capping");
    	assertEquals(4, acyclicMol.getAtomCount(), "Number of atoms in acyclic "
    			+ "molecule replacing forming ring");
    }
    
//------------------------------------------------------------------------------
}
