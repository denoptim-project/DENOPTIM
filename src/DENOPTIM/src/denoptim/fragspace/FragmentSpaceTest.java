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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openscience.cdk.Atom;
import org.openscience.cdk.silent.Bond;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.molecule.APClass;
import denoptim.molecule.DENOPTIMEdge.BondType;
import denoptim.molecule.DENOPTIMFragment;
import denoptim.molecule.DENOPTIMFragment.BBType;
import denoptim.molecule.DENOPTIMVertex;

/**
 * Unit test for fragment space
 * 
 * @author Marco Foscato
 */

public class FragmentSpaceTest
{
    private final String SEP = System.getProperty("file.separator");

    @TempDir 
    File tempDir;
    
	private final String APSUBRULE = "0";

    private final String RULAPCS = "apc-S";
    private final String RULAPC1 = "apc-1";
    private final String RULAPC2 = "apc-2";
    private final String RULAPC3 = "apc-3";
    private final String RULAPCC1 = "cap-1";
    private final String RULAPCC2 = "cap-2";
    
    private final BBType BBTFRAG = BBType.FRAGMENT;
    
	private APClass APCS;
	private APClass APC1;
	private APClass APC2;
	private APClass APC3;
	private APClass APCC1;
	private APClass APCC2;

//------------------------------------------------------------------------------
	private void buildFragmentSpace() throws DENOPTIMException
	{
	    assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
	    
        try
        {
            APCS = APClass.make(RULAPCS
                + DENOPTIMConstants.SEPARATORAPPROPSCL + APSUBRULE);
            APC1 = APClass.make(RULAPC1
                + DENOPTIMConstants.SEPARATORAPPROPSCL + APSUBRULE);
            APC2 = APClass.make(RULAPC2
                + DENOPTIMConstants.SEPARATORAPPROPSCL + APSUBRULE);
            APC3 = APClass.make(RULAPC3
                + DENOPTIMConstants.SEPARATORAPPROPSCL + APSUBRULE);
            APCC1 = APClass.make(RULAPCC1
                + DENOPTIMConstants.SEPARATORAPPROPSCL + APSUBRULE);
            APCC2 = APClass.make(RULAPCC2
                + DENOPTIMConstants.SEPARATORAPPROPSCL + APSUBRULE);
        } catch (DENOPTIMException e)
        {
            //This will not happen
        }
	       
        HashMap<String,BondType> boMap = new HashMap<String,BondType>();
        boMap.put(RULAPCS,BondType.SINGLE);
        boMap.put(RULAPC1,BondType.SINGLE);
        boMap.put(RULAPC2,BondType.SINGLE);
        boMap.put(RULAPC3,BondType.SINGLE);
        boMap.put(RULAPCC1,BondType.SINGLE);
        boMap.put(RULAPCC2,BondType.SINGLE);
        
        FragmentSpace.setBondOrderMap(boMap);

        String rootName = tempDir.getAbsolutePath() + SEP;

    	ArrayList<DENOPTIMFragment> fragLib = new ArrayList<DENOPTIMFragment>();
    	DENOPTIMFragment frg1 = new DENOPTIMFragment();
    	Atom a1 = new Atom("C", new Point3d(new double[]{0.0, 1.1, 2.2}));
    	Atom a2 = new Atom("C", new Point3d(new double[]{1.0, 1.1, 2.2}));
    	Atom a3 = new Atom("C", new Point3d(new double[]{2.0, 1.1, 2.2}));
    	frg1.addAtom(a1);
    	frg1.addAtom(a2);
    	frg1.addAtom(a3);
    	frg1.addBond(new Bond(a1, a2));
    	frg1.addBond(new Bond(a2, a3));
    	frg1.addAP(a3, APC1, new Point3d(new double[]{0.0, 2.2, 3.3}));
    	frg1.addAP(a3, APC1, new Point3d(new double[]{0.0, 0.0, 3.3}));
    	frg1.addAP(a3, APC2, new Point3d(new double[]{0.0, 0.0, 1.1}));
    	frg1.addAP(a1, APC3, new Point3d(new double[]{3.0, 0.0, 3.3}));
    	frg1.projectAPsToProperties();
    	fragLib.add(frg1);
    	
        DENOPTIMFragment frg2 = new DENOPTIMFragment();
        Atom a21 = new Atom("N", new Point3d(new double[]{0.0, 1.1, 2.2}));
        Atom a22 = new Atom("H", new Point3d(new double[]{1.0, 1.1, 2.2}));
        frg2.addAtom(a21);
        frg2.addAtom(a22);
        frg2.addBond(new Bond(a21, a22));
        frg2.addAP(a22, APC2, new Point3d(new double[]{0.0, 2.2, 3.3}));
        frg2.addAP(a22, APC2, new Point3d(new double[]{0.0, 0.0, 3.3}));
        frg2.projectAPsToProperties();
        fragLib.add(frg2);
        
        DENOPTIMFragment frg3 = new DENOPTIMFragment();
        Atom a31 = new Atom("P", new Point3d(new double[]{0.0, 1.1, 2.2}));
        frg3.addAtom(a31);
        frg3.addAP(a31, APC1, new Point3d(new double[]{0.0, 2.2, 3.3}));
        frg3.addAP(a31, APC2, new Point3d(new double[]{0.0, 0.0, 3.3}));
        frg3.addAP(a31, APC3, new Point3d(new double[]{0.0, 0.0, 1.1}));
        frg3.projectAPsToProperties();
        fragLib.add(frg3);
        
        String fragLibFile = rootName + "frags.sdf";
        DenoptimIO.writeFragmentSet(fragLibFile, fragLib);
        
    	ArrayList<DENOPTIMFragment> scaffLib = new ArrayList<DENOPTIMFragment>();
        DENOPTIMFragment frg4 = new DENOPTIMFragment();
        Atom a41 = new Atom("O", new Point3d(new double[]{0.0, 1.1, 2.2}));
        Atom a42 = new Atom("C", new Point3d(new double[]{1.0, 1.1, 2.2}));
        Atom a43 = new Atom("Ru", new Point3d(new double[]{2.0, 1.1, 2.2}));
        frg4.addAtom(a41);
        frg4.addAtom(a42);
        frg4.addAtom(a43);
        frg4.addBond(new Bond(a41, a42));
        frg4.addBond(new Bond(a42, a43));
        frg4.addAP(a43, APCS, new Point3d(new double[]{0.0, 2.2, 3.3}));
        frg4.addAP(a43, APCS, new Point3d(new double[]{0.0, 0.0, 3.3}));
        frg4.addAP(a43, APCS, new Point3d(new double[]{0.0, 0.0, 1.1}));
        frg4.addAP(a41, APCS, new Point3d(new double[]{3.0, 0.0, 3.3}));
        frg4.projectAPsToProperties();
        // NB: in the sorted list the last AP is first!
        scaffLib.add(frg4);
        
        DENOPTIMFragment frg5 = new DENOPTIMFragment();
        Atom a51 = new Atom("Zn", new Point3d(new double[]{5.0, 1.1, 2.2}));
        frg5.addAtom(a51);
        frg5.addAP(a51, APCS, new Point3d(new double[]{5.0, 2.2, 3.3}));
        frg5.addAP(a51, APCS, new Point3d(new double[]{5.0, 0.0, 3.3}));
        frg5.addAP(a51, APCS, new Point3d(new double[]{5.0, 0.0, 1.1}));
        frg5.projectAPsToProperties();
        scaffLib.add(frg5);
        
        String scaffLibFile = rootName + "scaff.sdf";
        DenoptimIO.writeFragmentSet(scaffLibFile, scaffLib);
        
        ArrayList<DENOPTIMFragment> cappLib = new ArrayList<DENOPTIMFragment>();
        DENOPTIMFragment frg6 = new DENOPTIMFragment();
        Atom a61 = new Atom("H", new Point3d(new double[]{10.0, 1.1, 2.2}));
        frg6.addAtom(a61);
        frg6.addAP(a61, APCC1, new Point3d(new double[]{13.0, 0.0, 3.3}));
        frg6.projectAPsToProperties();
        cappLib.add(frg6);
        
        DENOPTIMFragment frg7 = new DENOPTIMFragment();
        Atom a71 = new Atom("Cl", new Point3d(new double[]{10.0, 1.1, 2.2}));
        frg7.addAtom(a71);
        frg7.addAP(a71, APCC2, new Point3d(new double[]{13.0, 0.0, 3.3}));
        frg7.projectAPsToProperties();
        cappLib.add(frg7);

        String capLibFile = rootName + "caps.sdf";
        DenoptimIO.writeFragmentSet(capLibFile, cappLib);
        
    	HashMap<APClass,ArrayList<APClass>> cpMap = 
    	        new HashMap<APClass,ArrayList<APClass>>();
    	ArrayList<APClass> lst1 = new ArrayList<APClass>();
    	lst1.add(APC1);
    	lst1.add(APC2);
    	cpMap.put(APCS, lst1);
    	ArrayList<APClass> lst2 = new ArrayList<APClass>();
    	lst2.add(APC2);
    	cpMap.put(APC1, lst2);
    	ArrayList<APClass> lst3 = new ArrayList<APClass>();
    	lst3.add(APC2);
    	lst3.add(APC3);
    	cpMap.put(APC2, lst3);
    	
    	HashMap<APClass,APClass> capMap = new HashMap<APClass,APClass>();
    	capMap.put(APCS, APCC2);
    	capMap.put(APC1, APCC1);
    	capMap.put(APC2, APCC1);

    	HashSet<APClass> ends = new HashSet<APClass>();
    	ends.add(APC3);
    	
    	String cpmFile = rootName + "cpm.dat";
    	DenoptimIO.writeCompatibilityMatrix(cpmFile, cpMap, boMap, capMap,ends);

    	/*
    	//Just in case one day we want to have also an RC-CPMap
    	HashMap<APClass,ArrayList<APClass>> rcCpMap = 
    			new HashMap<APClass,ArrayList<APClass>>();
    	*/
    	
    	FragmentSpace.defineFragmentSpace(scaffLibFile, fragLibFile, capLibFile,
    	        cpmFile);
	}
	
//-----------------------------------------------------------------------------    	
	
    @Test
    public void testGetFragsWithAPClass() throws Exception
    {
    	buildFragmentSpace();
    	ArrayList<IdFragmentAndAP> l = FragmentSpace.getFragsWithAPClass(APC2);
    	assertEquals(4,l.size(),"Wrong size of AP IDs with given APClass.");
    	
    	IdFragmentAndAP ref1 = new IdFragmentAndAP(-1,0,BBTFRAG,3,-1,-1);
    	IdFragmentAndAP ref2 = new IdFragmentAndAP(-1,1,BBTFRAG,0,-1,-1);
    	IdFragmentAndAP ref3 = new IdFragmentAndAP(-1,1,BBTFRAG,1,-1,-1);
    	IdFragmentAndAP ref4 = new IdFragmentAndAP(-1,2,BBTFRAG,1,-1,-1);
    	
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
    	
    	IdFragmentAndAP ref1 = new IdFragmentAndAP(-1,0,BBTFRAG,3,-1,-1);
    	IdFragmentAndAP ref2 = new IdFragmentAndAP(-1,1,BBTFRAG,0,-1,-1);
    	IdFragmentAndAP ref3 = new IdFragmentAndAP(-1,1,BBTFRAG,1,-1,-1);
    	IdFragmentAndAP ref4 = new IdFragmentAndAP(-1,2,BBTFRAG,1,-1,-1);
    	
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
    	IdFragmentAndAP src1 = new IdFragmentAndAP(-1,2,BBTFRAG,0,-1,-1);
    	IdFragmentAndAP src2 = new IdFragmentAndAP(-1,2,BBTFRAG,1,-1,-1);
    	ArrayList<IdFragmentAndAP> srcAPs = new ArrayList<IdFragmentAndAP>();
    	srcAPs.add(src1);
    	srcAPs.add(src2);
    	
    	/*
    	System.out.println("SRC1 "+FragmentSpace.getAPClassForFragment(src1)+" => "
    	+FragmentSpace.getCompatibleAPClasses(FragmentSpace.getAPClassForFragment(src1)));
    	System.out.println("SRC2 "+FragmentSpace.getAPClassForFragment(src2)+" => "
    	+FragmentSpace.getCompatibleAPClasses(FragmentSpace.getAPClassForFragment(src2)));
    	System.out.println("Frags with SRC1: "+FragmentSpace.getFragAPsCompatibleWithClass(
    	        FragmentSpace.getAPClassForFragment(src1)));
        System.out.println("Frags with SRC2: "+FragmentSpace.getFragAPsCompatibleWithClass(
                FragmentSpace.getAPClassForFragment(src2)));
    	*/
    	
    	ArrayList<IdFragmentAndAP> lst = 
    			FragmentSpace.getFragAPsCompatibleWithTheseAPs(srcAPs);
    	
    	assertEquals(4,lst.size(),"Size of compatible APs list is wrong.");
    	
    	IdFragmentAndAP ref1 = new IdFragmentAndAP(-1,0,BBTFRAG,3,-1,-1);
    	IdFragmentAndAP ref2 = new IdFragmentAndAP(-1,1,BBTFRAG,0,-1,-1);
    	IdFragmentAndAP ref3 = new IdFragmentAndAP(-1,1,BBTFRAG,1,-1,-1);
    	IdFragmentAndAP ref4 = new IdFragmentAndAP(-1,2,BBTFRAG,1,-1,-1);
    	
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
    	IdFragmentAndAP src1 = new IdFragmentAndAP(-1,2,BBTFRAG,0,-1,-1);
    	IdFragmentAndAP src2 = new IdFragmentAndAP(-1,2,BBTFRAG,1,-1,-1);
    	ArrayList<IdFragmentAndAP> srcAPs = new ArrayList<IdFragmentAndAP>();
    	srcAPs.add(src1);
    	srcAPs.add(src2);
    	
    	ArrayList<DENOPTIMVertex> lst = 
    			FragmentSpace.getFragmentsCompatibleWithTheseAPs(srcAPs);
    	
    	assertEquals(3,lst.size(),"Wrong number of compatible fragments.");
    	
    	FragmentSpace.clearAll();	
    }
    
//------------------------------------------------------------------------------
    
}
