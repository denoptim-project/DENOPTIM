package denoptim.molecule;

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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.Bond;

import denoptim.constants.DENOPTIMConstants;
import denoptim.fragspace.FragmentSpace;
import denoptim.molecule.DENOPTIMEdge.BondType;
import denoptim.molecule.DENOPTIMFragment.BBType;

/**
 * Unit test for DENOPTIMFragment
 * 
 * @author Marco Foscato
 */

public class DENOPTIMFragmentTest
{
	private final String APRULE = "MyRule";
	private final String APSUBRULE = "1";
	private final String APCLASS = APRULE
			+ DENOPTIMConstants.SEPARATORAPPROPSCL + APSUBRULE;
    private final String APCSEP = DENOPTIMConstants.SEPARATORAPPROPSCL;
    
//------------------------------------------------------------------------------
	
    @Test
    public void testHandlingAPsAsObjOrProperty() throws Exception
    {
        // This is just to avoid the warnings about trying to get a bond type
        // when the fragment space in not defined
        HashMap<String, BondType> map = new HashMap<String, BondType>();
        map.put(APRULE,BondType.SINGLE);
        FragmentSpace.setBondOrderMap(map);
        
    	DENOPTIMFragment frg1 = new DENOPTIMFragment();
    	Atom a1 = new Atom("C", new Point3d(new double[]{0.0, 1.1, 2.2}));
    	Atom a2 = new Atom("C", new Point3d(new double[]{1.0, 1.1, 2.2}));
    	Atom a3 = new Atom("C", new Point3d(new double[]{2.0, 1.1, 2.2}));
    	frg1.addAtom(a1);
    	frg1.addAtom(a2);
    	frg1.addAtom(a3);
    	frg1.addBond(new Bond(a1, a2));
    	frg1.addBond(new Bond(a2, a3));
    	frg1.addAP(a3, APClass.make(APCLASS), 
    	        new Point3d(new double[]{0.0, 2.2, 3.3}));
    	frg1.addAP(a3, APClass.make(APCLASS), 
    	        new Point3d(new double[]{0.0, 0.0, 3.3}));
    	frg1.addAP(a3, APClass.make(APCLASS), 
    	        new Point3d(new double[]{0.0, 0.0, 1.1}));
    	frg1.addAP(a1, APClass.make(APCLASS), 
    	        new Point3d(new double[]{3.0, 0.0, 3.3}));
    	
    	frg1.projectAPsToProperties(); 
    	String apStr = frg1.getProperty(DENOPTIMConstants.APTAG).toString();
    	String clsStr = frg1.getProperty(DENOPTIMConstants.APCVTAG).toString();
    	
    	DENOPTIMFragment frg2 = new DENOPTIMFragment();
    	Atom a4 = new Atom("C", new Point3d(new double[]{0.0, 1.1, 2.2}));
    	Atom a5 = new Atom("C", new Point3d(new double[]{1.0, 1.1, 2.2}));
    	Atom a6 = new Atom("C", new Point3d(new double[]{2.0, 1.1, 2.2}));
    	frg2.addAtom(a4);
    	frg2.addAtom(a5);
    	frg2.addAtom(a6);
    	frg2.addBond(new Bond(a4, a5));
    	frg2.addBond(new Bond(a5, a6));
    	frg2.setProperty(DENOPTIMConstants.APTAG, apStr);
    	frg2.setProperty(DENOPTIMConstants.APCVTAG, clsStr);
    	frg2.projectPropertyToAP();
    	
    	assertEquals(frg1.getNumberOfAP(),frg2.getNumberOfAP(),"Equality of #AP");
    	assertEquals(frg1.getAPCountOnAtom(0),frg2.getAPCountOnAtom(0),
    	        "Equality of #AP-on-atom");
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testConversionToIAC() throws Exception
    {
    	// WARNING: the conversion does not project the atom properties into
    	// molecular properties. So the APs do not appear in the mol properties
        // unless we project the APs to properties (see projectAPsToProperties)
        
        // This is just to avoid the warnings about trying to get a bond type
        // when the fragment space in not defined
        HashMap<String, BondType> map = new HashMap<String, BondType>();
        map.put(APRULE,BondType.SINGLE);
        FragmentSpace.setBondOrderMap(map);
    	
        DENOPTIMFragment frg1 = new DENOPTIMFragment();
        Atom a1 = new Atom("C", new Point3d(new double[]{0.0, 1.1, 2.2}));
        Atom a2 = new Atom("O", new Point3d(new double[]{1.0, 1.1, 2.2}));
        Atom a3 = new Atom("C", new Point3d(new double[]{2.0, 1.1, 2.2}));
        frg1.addAtom(a1);
        frg1.addAtom(a2);
        frg1.addAtom(a3);
        frg1.addBond(new Bond(a1, a2));
        frg1.addBond(new Bond(a2, a3));
        frg1.addAP(a1, APClass.make(APCLASS), 
                new Point3d(new double[]{1.0, 2.5, 3.3}));
        frg1.addAP(a1, APClass.make(APRULE+APCSEP+"2"), 
                new Point3d(new double[]{2.0, -2.5, 3.3}));
        frg1.addAP(a1, APClass.make(APRULE+APCSEP+"3"), 
                new Point3d(new double[]{-2.0, -2.5, 3.3}));
        frg1.addAP(a2, APClass.make(APCLASS), 
                new Point3d(new double[]{2.5, 2.5, 3.3}));
        frg1.addAP(a3, APClass.make(APCLASS), 
                new Point3d(new double[]{3.0, 2.5, 3.3}));
        frg1.addAP(a3, APClass.make(APRULE+APCSEP+"2"), 
                new Point3d(new double[]{4.0, -2.5, 3.3}));
        frg1.addAP(a3, APClass.make(APRULE+APCSEP+"4"), 
                new Point3d(new double[]{-4.0, -2.5, 3.3}));
        frg1.projectAPsToProperties();

        //System.out.println("Initial Symm set: "+orig.getSymmetricAPSets());
        
        IAtomContainer iac = frg1.getIAtomContainer();
        
        DENOPTIMFragment frg2 = new DENOPTIMFragment(iac,BBType.UNDEFINED);
        
        assertEquals(7,frg1.getNumberOfAP(),"#APs in frg1");
        assertEquals(7,frg2.getNumberOfAP(),"#APs in frg2");
        assertEquals(3,frg1.getAPCountOnAtom(0),"#APs in frg1 atm0");
        assertEquals(3,frg2.getAPCountOnAtom(0),"#APs in frg2 atm0");
        assertEquals(3,frg1.getAPCountOnAtom(2),"#APs in frg1 atm2");
        assertEquals(3,frg2.getAPCountOnAtom(2),"#APs in frg2 atm2");
        assertEquals(2,frg1.getSymmetricAPSets().size(),"#SymmAPSets in frg1");
        assertEquals(2,frg2.getSymmetricAPSets().size(),"#SymmAPSets in frg2");
        assertTrue(frg1.getSymmetricAPs(0).contains(4),"SymmSet [0,4] in frg1");
        assertTrue(frg2.getSymmetricAPs(0).contains(4),"SymmSet [0,4] in frg2");
        assertTrue(frg1.getSymmetricAPs(1).contains(5),"SymmSet [1,5] in frg1");
        assertTrue(frg2.getSymmetricAPs(1).contains(5),"SymmSet [1,5] in frg2");
    }

//------------------------------------------------------------------------------

    @Test
    public void testClone() throws Exception
    {
        // This is just to avoid the warnings about trying to get a bond type
        // when the fragment space in not defined
        HashMap<String, BondType> map = new HashMap<String, BondType>();
        map.put(APRULE,BondType.SINGLE);
        FragmentSpace.setBondOrderMap(map);
        
        DENOPTIMFragment v = new DENOPTIMFragment();
        Atom a1 = new Atom("C", new Point3d(new double[]{0.0, 1.1, 2.2}));
        Atom a2 = new Atom("C", new Point3d(new double[]{1.0, 1.1, 2.2}));
        Atom a3 = new Atom("C", new Point3d(new double[]{2.0, 1.1, 2.2}));
        v.addAtom(a1);
        v.addAtom(a2);
        v.addAtom(a3);
        v.addBond(new Bond(a1, a2));
        v.addBond(new Bond(a2, a3));
        v.addAP(a3, APClass.make(APCLASS), 
                new Point3d(new double[]{0.0, 2.2, 3.3}));
        v.addAP(a3, APClass.make(APCLASS), 
                new Point3d(new double[]{0.0, 0.0, 3.3}));
        v.addAP(a3, APClass.make(APCLASS), 
                new Point3d(new double[]{0.0, 0.0, 1.1}));
        v.addAP(a1, APClass.make(APCLASS), 
                new Point3d(new double[]{3.0, 0.0, 3.3}));
        
        ArrayList<SymmetricSet> ssaps = new ArrayList<SymmetricSet>();
        ssaps.add(new SymmetricSet(new ArrayList<Integer>(
                Arrays.asList(0,1,2))));
        v.setSymmetricAPSets(ssaps);
        v.setVertexId(18);
        v.setLevel(26);
        v.setAsRCV(true);
        v.setBuildingBlockType(BBType.SCAFFOLD);
        
        DENOPTIMVertex c = v.clone();
        
        assertEquals(4,((DENOPTIMFragment) c).getNumberOfAP(),"Number of APs");
        assertEquals(1,((DENOPTIMFragment) c).getAPCountOnAtom(0),
                "Size APs on atm0");
        assertEquals(3,((DENOPTIMFragment) c).getAPCountOnAtom(2),
                "Size APs on atm2");
        assertEquals(4,c.getAttachmentPoints().size(),"Number of APs (B)");
        assertEquals(1,c.getSymmetricAPSets().size(), "Number of symmetric sets");
        assertEquals(3,c.getSymmetricAPSets().get(0).size(), 
                "Number of symmetric APs in set");
        assertEquals(v.getVertexId(), c.getVertexId(), "Vertex ID");
        assertEquals(v.getNumberOfAP(), c.getNumberOfAP(), "Number of APS");
        assertEquals(v.getSymmetricAPSets().size(), 
                c.getSymmetricAPSets().size(), "Number of SymAPs sets");
        assertEquals(v.getLevel(), c.getLevel(), "Level");
        assertEquals(v.isRCV(), c.isRCV(), "RCV flag");
        assertNotEquals(v.hashCode(), c.hashCode(), "Hash code");  
        assertEquals(v.getFragmentType(),
                ((DENOPTIMFragment)c).getFragmentType(), "Building bloc ktype");
    }
    
//------------------------------------------------------------------------------


    @Test
    public void testGetMutationSites() throws Exception
    {
        IAtomContainer iac = new AtomContainer();
        DENOPTIMFragment v = new DENOPTIMFragment(iac,BBType.FRAGMENT);
        assertEquals(1,v.getMutationSites().size(),
                "Fragments return themselves as mutable sites.");
        v = new DENOPTIMFragment(iac,BBType.SCAFFOLD);
        assertEquals(0,v.getMutationSites().size(),
                "Scaffolds so not return any mutable site.");
        v = new DENOPTIMFragment(iac,BBType.CAP);
        assertEquals(0,v.getMutationSites().size(),
                "Capping groups so not return any mutable site.");
        v = new DENOPTIMFragment(iac,BBType.UNDEFINED);
        assertEquals(1,v.getMutationSites().size(),
                "Undefined building block return themselves as mutable sites.");
        v = new DENOPTIMFragment(iac,BBType.NONE);
        assertEquals(1,v.getMutationSites().size(),
                "'None' building block return themselves as mutable sites.");
    }
    
//------------------------------------------------------------------------------

}
