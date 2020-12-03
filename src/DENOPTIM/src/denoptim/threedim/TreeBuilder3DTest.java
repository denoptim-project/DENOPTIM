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

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.Bond;

import denoptim.fragspace.FragmentSpace;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMEdge;
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
    	DENOPTIMFragment frg1 = new DENOPTIMFragment();
    	Atom a1 = new Atom("C", new Point3d(new double[]{0.0, 0.0, 0.0}));
    	Atom a2 = new Atom("C", new Point3d(new double[]{1.0, 0.0, 0.0}));
    	frg1.addAtom(a1);
    	frg1.addAtom(a2);
    	frg1.addBond(new Bond(a1, a2));
    	frg1.addAP(0, "a:0", new Point3d(new double[]{0.0, 1.0, 1.0}));
    	frg1.addAP(1, "a:0", new Point3d(new double[]{1.0, 1.0, 1.0}));
    	frg1.projectAPsToProperties(); 
    	
    	DENOPTIMFragment frg2 = new DENOPTIMFragment();
    	Atom a3 = new Atom("C", new Point3d(new double[]{0.0, 0.0, 0.0}));
    	frg2.addAtom(a3);
    	frg2.addAP(0, "a:0", new Point3d(new double[]{0.0, 1.0, 1.0}));
    	frg2.addAP(0, "a:0", new Point3d(new double[]{0.0, -1.0, -1.0}));   
    	frg2.projectAPsToProperties(); 

    	DENOPTIMFragment rca1 = new DENOPTIMFragment();
    	Atom a4 = new Atom("ATP", new Point3d(new double[]{0.0, 0.0, 0.0}));
    	rca1.addAtom(a4);
    	rca1.addAP(0, "ATplus:0", new Point3d(new double[]{0.0, 1.0, 1.0}));
    	rca1.projectAPsToProperties(); 
    	
    	DENOPTIMFragment rca2 = new DENOPTIMFragment();
    	Atom a5 = new Atom("ATM", new Point3d(new double[]{0.0, 0.0, 0.0}));
    	rca2.addAtom(a5);
    	rca2.addAP(0, "ATminus:0", new Point3d(new double[]{0.0, 1.0, 1.0}));
    	rca2.projectAPsToProperties(); 
    	
    	ArrayList<IAtomContainer> scaff = new ArrayList<IAtomContainer>();
    	scaff.add(frg1);
    	ArrayList<IAtomContainer> frags = new ArrayList<IAtomContainer>();
    	frags.add(frg2);
    	frags.add(rca1);
    	frags.add(rca2);
    	ArrayList<IAtomContainer> caps = new ArrayList<IAtomContainer>();
    	
    	FragmentSpace.defineFragmentSpace(scaff,frags,caps);
    	
    	String graphStr = "1 1_1_0_-1,2_1_1_0,3_3_1_1,4_2_1_0, "
    			+ "1_1_2_0_1_a:0_a:0,"
    			+ "2_1_3_0_1_a:0_ATminus:0,"
    			+ "1_0_4_0_1_a:0_ATplus:0, "
    			+ "DENOPTIMRing [verteces="
    			+ "[3_3_1_1, 2_1_1_0, 1_1_0_-1, 4_2_1_0]]";
    	DENOPTIMGraph dg = GraphConversionTool.getGraphFromString(graphStr,true);

    	TreeBuilder3D t3d = new TreeBuilder3D(scaff,frags,caps);
    	

    	IAtomContainer mol = t3d.convertGraphTo3DAtomContainer(dg,false);
    	assertEquals(4, mol.getBondCount(), "Number of bonds without the "
    			+ "cyclic one");
    	
    	mol = t3d.convertGraphTo3DAtomContainer(dg,true);
    	// NB: no RCAs anymore, so two atoms less than before, and two bonds
    	// less than the moment when we have made the ring-closing bond but we
    	// have not yet removed the RCAs. Basically we add 1 bond and remove 2
    	assertEquals(3, mol.getBondCount(), "Number of bonds, including the "
    			+ "cyclic one");
    }
    
//------------------------------------------------------------------------------
}
