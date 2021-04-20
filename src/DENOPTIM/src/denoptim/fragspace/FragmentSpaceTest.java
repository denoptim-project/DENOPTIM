package denoptim.fragspace;

/*
 *   DENOPTIM
 *   Copyright (C) 2019 Marco Foscato <marco.foscato@uib.no>
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.Bond;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMEdge;
import denoptim.molecule.DENOPTIMFragment;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMRing;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.molecule.SymmetricSet;

/**
 * Unit test for fragment space
 * 
 * @author Marco Foscato
 */

public class FragmentSpaceTest
{
	private final String APSUBRULE = "0";
	private final String APCS = "apc-S"
			+ DENOPTIMConstants.SEPARATORAPPROPSCL + APSUBRULE;
	private final String APC1 = "apc-1"
			+ DENOPTIMConstants.SEPARATORAPPROPSCL + APSUBRULE;
	private final String APC2 = "apc-2"
			+ DENOPTIMConstants.SEPARATORAPPROPSCL + APSUBRULE;
	private final String APC3 = "apc-3"
			+ DENOPTIMConstants.SEPARATORAPPROPSCL + APSUBRULE;
	private final String APCC1 = "cap-1"
			+ DENOPTIMConstants.SEPARATORAPPROPSCL + APSUBRULE;
	private final String APCC2 = "cap-1"
			+ DENOPTIMConstants.SEPARATORAPPROPSCL + APSUBRULE;
	
//------------------------------------------------------------------------------
	
	private void buildFragmentSpace() throws DENOPTIMException
	{
    	ArrayList<IAtomContainer> fragLib = new ArrayList<IAtomContainer>();
    	DENOPTIMFragment frg1 = new DENOPTIMFragment();
    	Atom a1 = new Atom("C", new Point3d(new double[]{0.0, 1.1, 2.2}));
    	Atom a2 = new Atom("C", new Point3d(new double[]{1.0, 1.1, 2.2}));
    	Atom a3 = new Atom("C", new Point3d(new double[]{2.0, 1.1, 2.2}));
    	frg1.addAtom(a1);
    	frg1.addAtom(a2);
    	frg1.addAtom(a3);
    	frg1.addBond(new Bond(a1, a2));
    	frg1.addBond(new Bond(a2, a3));
    	frg1.addAP(2, APC1, new Point3d(new double[]{0.0, 2.2, 3.3}));
    	frg1.addAP(2, APC1, new Point3d(new double[]{0.0, 0.0, 3.3}));
    	frg1.addAP(2, APC2, new Point3d(new double[]{0.0, 0.0, 1.1}));
    	frg1.addAP(0, APC3, new Point3d(new double[]{3.0, 0.0, 3.3}));
    	IAtomContainer f1 = new AtomContainer(frg1);
    	fragLib.add(f1);
    	
        DENOPTIMFragment frg2 = new DENOPTIMFragment();
        Atom a21 = new Atom("N", new Point3d(new double[]{0.0, 1.1, 2.2}));
        Atom a22 = new Atom("H", new Point3d(new double[]{1.0, 1.1, 2.2}));
        frg2.addAtom(a21);
        frg2.addAtom(a22);
        frg2.addBond(new Bond(a21, a22));
        frg2.addAP(1, APC2, new Point3d(new double[]{0.0, 2.2, 3.3}));
        frg2.addAP(1, APC2, new Point3d(new double[]{0.0, 0.0, 3.3}));
        IAtomContainer f2 = new AtomContainer(frg2);
        fragLib.add(f2);
        
        DENOPTIMFragment frg3 = new DENOPTIMFragment();
        Atom a31 = new Atom("P", new Point3d(new double[]{0.0, 1.1, 2.2}));
        frg3.addAtom(a31);
        frg3.addAP(0, APC1, new Point3d(new double[]{0.0, 2.2, 3.3}));
        frg3.addAP(0, APC2, new Point3d(new double[]{0.0, 0.0, 3.3}));
        frg3.addAP(0, APC3, new Point3d(new double[]{0.0, 0.0, 1.1}));
        IAtomContainer f3 = new AtomContainer(frg3);
        fragLib.add(f3);
        
    	ArrayList<IAtomContainer> scaffLib = new ArrayList<IAtomContainer>();
        DENOPTIMFragment frg4 = new DENOPTIMFragment();
        Atom a41 = new Atom("O", new Point3d(new double[]{0.0, 1.1, 2.2}));
        Atom a42 = new Atom("C", new Point3d(new double[]{1.0, 1.1, 2.2}));
        Atom a43 = new Atom("Ru", new Point3d(new double[]{2.0, 1.1, 2.2}));
        frg4.addAtom(a41);
        frg4.addAtom(a42);
        frg4.addAtom(a43);
        frg4.addBond(new Bond(a41, a42));
        frg4.addBond(new Bond(a42, a43));
        frg4.addAP(2, APCS, new Point3d(new double[]{0.0, 2.2, 3.3}));
        frg4.addAP(2, APCS, new Point3d(new double[]{0.0, 0.0, 3.3}));
        frg4.addAP(2, APCS, new Point3d(new double[]{0.0, 0.0, 1.1}));
        frg4.addAP(0, APCS, new Point3d(new double[]{3.0, 0.0, 3.3}));
        IAtomContainer f4 = new AtomContainer(frg4);
        scaffLib.add(f4);
        
        DENOPTIMFragment frg5 = new DENOPTIMFragment();
        Atom a51 = new Atom("Zn", new Point3d(new double[]{5.0, 1.1, 2.2}));
        frg5.addAtom(a51);
        frg5.addAP(0, APCS, new Point3d(new double[]{5.0, 2.2, 3.3}));
        frg5.addAP(0, APCS, new Point3d(new double[]{5.0, 0.0, 3.3}));
        frg5.addAP(0, APCS, new Point3d(new double[]{5.0, 0.0, 1.1}));
        IAtomContainer f5 = new AtomContainer(frg5);
        scaffLib.add(f5);
        
        ArrayList<IAtomContainer> cappLib = new ArrayList<IAtomContainer>();
        DENOPTIMFragment frg6 = new DENOPTIMFragment();
        Atom a61 = new Atom("H", new Point3d(new double[]{10.0, 1.1, 2.2}));
        frg6.addAtom(a61);
        frg6.addAP(0, APCC1, new Point3d(new double[]{13.0, 0.0, 3.3}));
        IAtomContainer f6 = new AtomContainer(frg6);
        cappLib.add(f6);
        
        DENOPTIMFragment frg7 = new DENOPTIMFragment();
        Atom a71 = new Atom("Cl", new Point3d(new double[]{10.0, 1.1, 2.2}));
        frg7.addAtom(a71);
        frg7.addAP(0, APCC2, new Point3d(new double[]{13.0, 0.0, 3.3}));
        IAtomContainer f7 = new AtomContainer(frg7);
        cappLib.add(f7);

    	HashMap<String,ArrayList<String>> cpMap = new HashMap<String,ArrayList<String>>();
    	ArrayList<String> lst1 = new ArrayList<String>();
    	lst1.add(APC1);
    	lst1.add(APC2);
    	cpMap.put(APCS, lst1);
    	ArrayList<String> lst2 = new ArrayList<String>();
    	lst2.add(APC2);
    	cpMap.put(APC1, lst2);
    	ArrayList<String> lst3 = new ArrayList<String>();
    	lst3.add(APC2);
    	lst3.add(APC3);
    	cpMap.put(APC2, lst3);
    	
    	HashMap<String,Integer> boMap = new HashMap<String,Integer>();
    	boMap.put(APCS,1);
    	boMap.put(APC1,1);
    	boMap.put(APC2,1);
    	boMap.put(APC3,1);
    	boMap.put(APCC1,1);
    	boMap.put(APCC1,1);
    	
    	HashMap<String,String> capMap = new HashMap<String,String>();
    	capMap.put(APCS, APCC2);
    	capMap.put(APC1, APCC1);
    	capMap.put(APC2, APCC1);

    	HashSet<String> ends = new HashSet<String>();
    	ends.add(APC3);
    	
    	HashMap<String,ArrayList<String>> rcCpMap = 
    			new HashMap<String,ArrayList<String>>();

    	FragmentSpace.defineFragmentSpace(scaffLib,fragLib,cappLib,cpMap,boMap,
    			capMap,ends,rcCpMap);
	}
	
//-----------------------------------------------------------------------------    	
	
    @Test
    public void testGetFragsWithAPClass() throws Exception
    {
    	buildFragmentSpace();
    	ArrayList<IdFragmentAndAP> l = FragmentSpace.getFragsWithAPClass(APC2);
    	assertEquals(4,l.size(),"Wrong size of AP IDs with given APClass.");
    	
    	
    	IdFragmentAndAP ref1 = new IdFragmentAndAP(-1,0,1,2,-1,-1);
    	IdFragmentAndAP ref2 = new IdFragmentAndAP(-1,1,1,0,-1,-1);
    	IdFragmentAndAP ref3 = new IdFragmentAndAP(-1,1,1,1,-1,-1);
    	IdFragmentAndAP ref4 = new IdFragmentAndAP(-1,2,1,1,-1,-1);
    	
    	boolean found1 = false;
    	boolean found2 = false;
    	boolean found3 = false;
    	boolean found4 = false;
    	for (IdFragmentAndAP id : l)
    	{
    		if (id.sameFragAndAp(ref1))
    			found1 = true;
    		if (id.sameFragAndAp(ref2))
    			found2 = true;
    		if (id.sameFragAndAp(ref3))
    			found3 = true;
    		if (id.sameFragAndAp(ref4))
    			found4 = true;
    	}
    	
    	assertTrue(found1, "Missing first AP ID.");
    	assertTrue(found2, "Missing second AP ID.");
    	assertTrue(found3, "Missing third AP ID.");
    	assertTrue(found4, "Missing fourth AP ID.");
    	
    	FragmentSpace.clearAll();
    }

//-----------------------------------------------------------------------------    	
    	
    @Test
    public void testGetFragAPsCompatibleWithClass() throws Exception
    {
    	buildFragmentSpace();
    	
    	ArrayList<IdFragmentAndAP> lst = 
    			FragmentSpace.getFragAPsCompatibleWithClass(APC1);
    	
    	assertEquals(4,lst.size(),"Size of compatible APs list is wrong.");
    	
    	IdFragmentAndAP ref1 = new IdFragmentAndAP(-1,0,1,2,-1,-1);
    	IdFragmentAndAP ref2 = new IdFragmentAndAP(-1,1,1,0,-1,-1);
    	IdFragmentAndAP ref3 = new IdFragmentAndAP(-1,1,1,1,-1,-1);
    	IdFragmentAndAP ref4 = new IdFragmentAndAP(-1,2,1,1,-1,-1);
    	
    	boolean found1 = false;
    	boolean found2 = false;
    	boolean found3 = false;
    	boolean found4 = false;
    	for (IdFragmentAndAP id : lst)
    	{
    		if (id.sameFragAndAp(ref1))
    			found1 = true;
    		if (id.sameFragAndAp(ref2))
    			found2 = true;
    		if (id.sameFragAndAp(ref3))
    			found3 = true;
    		if (id.sameFragAndAp(ref4))
    			found4 = true;
    	}
    	
    	assertTrue(found1, "Missing first AP ID.");
    	assertTrue(found2, "Missing second AP ID.");
    	assertTrue(found3, "Missing third AP ID.");
    	assertTrue(found4, "Missing fourth AP ID.");
    	
    	FragmentSpace.clearAll();
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetFragAPsCompatibleWithTheseAPs() throws Exception
    {
    	buildFragmentSpace();
    	IdFragmentAndAP src1 = new IdFragmentAndAP(-1,2,1,0,-1,-1);
    	IdFragmentAndAP src2 = new IdFragmentAndAP(-1,2,1,1,-1,-1);
    	ArrayList<IdFragmentAndAP> srcAPs = new ArrayList<IdFragmentAndAP>();
    	srcAPs.add(src1);
    	srcAPs.add(src2);
    	
    	ArrayList<IdFragmentAndAP> lst = 
    			FragmentSpace.getFragAPsCompatibleWithTheseAPs(srcAPs);
    	
    	assertEquals(4,lst.size(),"Size of compatible APs list is wrong.");
    	
    	IdFragmentAndAP ref1 = new IdFragmentAndAP(-1,0,1,2,-1,-1);
    	IdFragmentAndAP ref2 = new IdFragmentAndAP(-1,1,1,0,-1,-1);
    	IdFragmentAndAP ref3 = new IdFragmentAndAP(-1,1,1,1,-1,-1);
    	IdFragmentAndAP ref4 = new IdFragmentAndAP(-1,2,1,1,-1,-1);
    	
    	boolean found1 = false;
    	boolean found2 = false;
    	boolean found3 = false;
    	boolean found4 = false;
    	for (IdFragmentAndAP id : lst)
    	{
    		if (id.sameFragAndAp(ref1))
    			found1 = true;
    		if (id.sameFragAndAp(ref2))
    			found2 = true;
    		if (id.sameFragAndAp(ref3))
    			found3 = true;
    		if (id.sameFragAndAp(ref4))
    			found4 = true;
    	}
    	
    	assertTrue(found1, "Missing first AP ID.");
    	assertTrue(found2, "Missing second AP ID.");
    	assertTrue(found3, "Missing third AP ID.");
    	assertTrue(found4, "Missing fourth AP ID.");
    	
    	FragmentSpace.clearAll();
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetFragmentsCompatibleWithTheseAPs() throws Exception
    {
    	buildFragmentSpace();
    	IdFragmentAndAP src1 = new IdFragmentAndAP(-1,2,1,0,-1,-1);
    	IdFragmentAndAP src2 = new IdFragmentAndAP(-1,2,1,1,-1,-1);
    	ArrayList<IdFragmentAndAP> srcAPs = new ArrayList<IdFragmentAndAP>();
    	srcAPs.add(src1);
    	srcAPs.add(src2);
    	
    	ArrayList<IAtomContainer> lst = 
    			FragmentSpace.getFragmentsCompatibleWithTheseAPs(srcAPs);
    	
    	assertEquals(3,lst.size(),"Wrong number of compatible fragments.");
    	
    	FragmentSpace.clearAll();	
    }
    
//------------------------------------------------------------------------------
    
}
