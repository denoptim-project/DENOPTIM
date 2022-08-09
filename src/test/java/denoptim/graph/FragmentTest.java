package denoptim.graph;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.Bond;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.utils.DummyAtomHandler;

/**
 * Unit test for DENOPTIMFragment
 * 
 * @author Marco Foscato
 */

public class FragmentTest
{
	private static final String APRULE = "MyRule";
	private static final String APSUBRULE = "1";
	private static final String APCLASS = APRULE
			+ DENOPTIMConstants.SEPARATORAPPROPSCL + APSUBRULE;
    private static final String APCSEP = DENOPTIMConstants.SEPARATORAPPROPSCL;
    
//------------------------------------------------------------------------------
	
    @Test
    public void testHandlingAPsAsObjOrProperty() throws Exception
    {
    	Fragment frg1 = new Fragment();
    	Atom a1 = new Atom("C", new Point3d(new double[]{0.0, 1.1, 2.2}));
    	Atom a2 = new Atom("C", new Point3d(new double[]{1.0, 1.1, 2.2}));
    	Atom a3 = new Atom("C", new Point3d(new double[]{2.0, 1.1, 2.2}));
    	frg1.addAtom(a1);
    	frg1.addAtom(a2);
    	frg1.addAtom(a3);
    	frg1.addBond(new Bond(a1, a2));
    	frg1.addBond(new Bond(a2, a3));
    	frg1.addAPOnAtom(a3, APClass.make(APCLASS),
    	        new Point3d(new double[]{0.0, 2.2, 3.3}));
    	frg1.addAPOnAtom(a3, APClass.make(APCLASS),
    	        new Point3d(new double[]{0.0, 0.0, 3.3}));
    	frg1.addAPOnAtom(a3, APClass.make(APCLASS),
    	        new Point3d(new double[]{0.0, 0.0, 1.1}));
    	frg1.addAPOnAtom(a1, APClass.make(APCLASS),
    	        new Point3d(new double[]{3.0, 0.0, 3.3}));
    	
    	frg1.projectAPsToProperties(); 
    	String clsStr = frg1.getProperty(DENOPTIMConstants.APSTAG).toString();
    	
    	Fragment frg2 = new Fragment();
    	Atom a4 = new Atom("C", new Point3d(new double[]{0.0, 1.1, 2.2}));
    	Atom a5 = new Atom("C", new Point3d(new double[]{1.0, 1.1, 2.2}));
    	Atom a6 = new Atom("C", new Point3d(new double[]{2.0, 1.1, 2.2}));
    	frg2.addAtom(a4);
    	frg2.addAtom(a5);
    	frg2.addAtom(a6);
    	frg2.addBond(new Bond(a4, a5));
    	frg2.addBond(new Bond(a5, a6));
    	frg2.setProperty(DENOPTIMConstants.APSTAG, clsStr);
    	frg2.projectPropertyToAP();
    	
    	assertEquals(frg1.getNumberOfAPs(),frg2.getNumberOfAPs(),
    	        "Equality of #AP");
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

        Fragment frg1 = new Fragment();
        Atom a1 = new Atom("C", new Point3d(new double[]{0.0, 1.1, 2.2}));
        Atom a2 = new Atom("O", new Point3d(new double[]{1.0, 1.1, 2.2}));
        Atom a3 = new Atom("C", new Point3d(new double[]{2.0, 1.1, 2.2}));
        frg1.addAtom(a1);
        frg1.addAtom(a2);
        frg1.addAtom(a3);
        frg1.addBond(new Bond(a1, a2));
        frg1.addBond(new Bond(a2, a3));
        frg1.addAPOnAtom(a1, APClass.make(APCLASS),              // 0
                new Point3d(new double[]{1.0, 2.5, 3.3}));
        frg1.addAPOnAtom(a1, APClass.make(APRULE+APCSEP+"2"),    // 1
                new Point3d(new double[]{2.0, -2.5, 3.3}));
        frg1.addAPOnAtom(a1, APClass.make(APRULE+APCSEP+"3"),    // 2
                new Point3d(new double[]{-2.0, -2.5, 3.3}));
        frg1.addAPOnAtom(a2, APClass.make(APCLASS),              // 3
                new Point3d(new double[]{2.5, 2.5, 3.3}));
        frg1.addAPOnAtom(a3, APClass.make(APCLASS),              // 4
                new Point3d(new double[]{3.0, 2.5, 3.3}));
        frg1.addAPOnAtom(a3, APClass.make(APRULE+APCSEP+"2"),    // 5
                new Point3d(new double[]{4.0, -2.5, 3.3}));
        frg1.addAPOnAtom(a3, APClass.make(APRULE+APCSEP+"4"),    // 6
                new Point3d(new double[]{-4.0, -2.5, 3.3}));
        frg1.projectAPsToProperties();
        
        IAtomContainer iac = frg1.getIAtomContainer();
        
        Fragment frg2 = new Fragment(iac, 
                Vertex.BBType.UNDEFINED);
        
        assertEquals(7,frg1.getNumberOfAPs(),"#APs in frg1");
        assertEquals(7,frg2.getNumberOfAPs(),"#APs in frg2");
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
    
    public static Fragment makeFragment() throws DENOPTIMException
    {
        Fragment v = new Fragment();
        Atom a1 = new Atom("C", new Point3d(new double[]{0.0, 1.1, 2.2}));
        Atom a2 = new Atom("C", new Point3d(new double[]{1.0, 1.1, 2.2}));
        Atom a3 = new Atom("C", new Point3d(new double[]{2.0, 1.1, 2.2}));
        v.addAtom(a1);
        v.addAtom(a2);
        v.addAtom(a3);
        v.addBond(new Bond(a1, a2));
        v.addBond(new Bond(a2, a3));
        v.addAPOnAtom(a3, APClass.make(APCLASS),
                new Point3d(new double[]{0.0, 2.2, 3.3}));
        v.addAPOnAtom(a3, APClass.make(APCLASS),
                new Point3d(new double[]{0.0, 0.0, 3.3}));
        v.addAPOnAtom(a3, APClass.make(APCLASS),
                new Point3d(new double[]{0.0, 0.0, 1.1}));
        v.addAPOnAtom(a1, APClass.make(APCLASS),
                new Point3d(new double[]{3.0, 0.0, 3.3}));
        
        ArrayList<SymmetricSet> ssaps = new ArrayList<SymmetricSet>();
        ssaps.add(new SymmetricSet(new ArrayList<Integer>(
                Arrays.asList(0,1,2)))); 
        //NB: customised symmetry set that does not correspond to the
        // definition of symmetry as perceived automatically by
        // DENOPTIMFragment.identifySymmetryRelatedAPSets
        // This because we want to test if the symmetric set is properly
        // serialized/deserialized.
        v.setSymmetricAPSets(ssaps);
        v.setVertexId(18);
        v.setAsRCV(true);
        //NB thy bond type is check by other methods: do not change it.
        v.setBuildingBlockType(Vertex.BBType.SCAFFOLD);
        
        return v;
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testClone() throws Exception
    {   
        Fragment v = makeFragment();
        v.setProperty("PROPNAME","PROVALUE");
        
        Vertex c = v.clone();
        
        assertEquals(4,((Fragment) c).getNumberOfAPs(),
                "Number of APs");
        assertEquals(1,((Fragment) c).getAPCountOnAtom(0),
                "Size APs on atm0");
        assertEquals(3,((Fragment) c).getAPCountOnAtom(2),
                "Size APs on atm2");
        assertEquals(4,c.getAttachmentPoints().size(),
                "Number of APs (B)");
        assertEquals(1,c.getSymmetricAPSets().size(), 
                "Number of symmetric sets");
        assertEquals(3,c.getSymmetricAPSets().get(0).size(), 
                "Number of symmetric APs in set");
        assertEquals(v.getVertexId(), c.getVertexId(), 
                "Vertex ID");
        assertEquals(v.getNumberOfAPs(), c.getNumberOfAPs(), 
                "Number of APS");
        assertEquals(v.getSymmetricAPSets().size(), 
                c.getSymmetricAPSets().size(), 
                "Number of SymAPs sets");
        assertEquals(v.isRCV(), c.isRCV(), 
                "RCV flag");
        assertNotEquals(v.hashCode(), c.hashCode(), 
                "Hash code");  
        assertEquals(v.getBuildingBlockType(),
                ((Fragment)c).getBuildingBlockType(), 
                "Building bloc ktype");
        assertEquals("PROVALUE",c.getProperty("PROPNAME"));
    }
    
//------------------------------------------------------------------------------
    
    public static Fragment makeFragmentA() throws DENOPTIMException
    {
        Fragment v = new Fragment();
        Atom a1 = new Atom("C", new Point3d(new double[]{0.0, 1.1, 2.2}));
        Atom a2 = new Atom("O", new Point3d(new double[]{1.0, 1.1, 2.2}));
        Atom a3 = new Atom("C", new Point3d(new double[]{2.0, 1.1, 2.2}));
        v.addAtom(a1);
        v.addAtom(a2);
        v.addAtom(a3);
        v.addBond(new Bond(a1, a2));
        v.addBond(new Bond(a2, a3));
        v.addAPOnAtom(a3, APClass.make("apc:1"),
                new Point3d(new double[]{0.0, 2.2, 3.3}));
        v.addAPOnAtom(a3, APClass.make("apc:2"),
                new Point3d(new double[]{0.0, 0.0, 3.3}));
        v.addAPOnAtom(a3, APClass.make("foo:0"),
                new Point3d(new double[]{0.0, 0.0, 1.1}));
        v.addAPOnAtom(a1, APClass.make("foo:0"),
                new Point3d(new double[]{3.0, 0.0, 3.3}));
        return v;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Differs from the fragment produced by {@link #makeFragmentA()} only by
     * the identity of one atom.
     */
    
    public static Fragment makeFragmentB() throws DENOPTIMException
    {
        Fragment v = new Fragment();
        Atom a1 = new Atom("C", new Point3d(new double[]{0.0, 1.1, 2.2}));
        Atom a2 = new Atom("O", new Point3d(new double[]{1.0, 1.1, 2.2}));
        Atom a3 = new Atom("H", new Point3d(new double[]{2.0, 1.1, 2.2})); // <<= here is the difference
        v.addAtom(a1);
        v.addAtom(a2);
        v.addAtom(a3);
        v.addBond(new Bond(a1, a2));
        v.addBond(new Bond(a2, a3));
        v.addAPOnAtom(a3, APClass.make("apc:1"),
                new Point3d(new double[]{0.0, 2.2, 3.3}));
        v.addAPOnAtom(a3, APClass.make("apc:2"),
                new Point3d(new double[]{0.0, 0.0, 3.3}));
        v.addAPOnAtom(a3, APClass.make("foo:0"),
                new Point3d(new double[]{0.0, 0.0, 1.1}));
        v.addAPOnAtom(a1, APClass.make("foo:0"),
                new Point3d(new double[]{3.0, 0.0, 3.3}));
        return v;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Differs from the fragment produced by {@link #makeFragmentA()} only by
     * the one {@link APCLass}. 
     */
    
    public static Fragment makeFragmentC() throws DENOPTIMException
    {
        Fragment v = new Fragment();
        Atom a1 = new Atom("C", new Point3d(new double[]{0.0, 1.1, 2.2}));
        Atom a2 = new Atom("O", new Point3d(new double[]{1.0, 1.1, 2.2}));
        Atom a3 = new Atom("C", new Point3d(new double[]{2.0, 1.1, 2.2}));
        v.addAtom(a1);
        v.addAtom(a2);
        v.addAtom(a3);
        v.addBond(new Bond(a1, a2));
        v.addBond(new Bond(a2, a3));
        v.addAPOnAtom(a3, APClass.make("apc:1"),
                new Point3d(new double[]{0.0, 2.2, 3.3}));
        v.addAPOnAtom(a3, APClass.make("apc:2"),
                new Point3d(new double[]{0.0, 0.0, 3.3}));
        v.addAPOnAtom(a3, APClass.make("foo:0"),
                new Point3d(new double[]{0.0, 0.0, 1.1}));
        v.addAPOnAtom(a1, APClass.make("foo:1"),  // <<= here is the difference
                new Point3d(new double[]{3.0, 0.0, 3.3}));
        return v;
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testIsomorphicTo() throws Exception
    {
        Fragment vA = makeFragmentA();
        Fragment vB = makeFragmentB();
        Fragment vC = makeFragmentC();
        assertTrue(vA.isIsomorphicTo(vA));
        assertTrue(vB.isIsomorphicTo(vB));
        assertTrue(vC.isIsomorphicTo(vC));
        
        assertFalse(vA.isIsomorphicTo(vB));
        assertFalse(vB.isIsomorphicTo(vA));
        assertFalse(vA.isIsomorphicTo(vC));
        assertFalse(vB.isIsomorphicTo(vC));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testIsomorphicInLinear() throws Exception
    {
        Fragment vA = makeFragmentA();
        DummyAtomHandler.addDummiesOnLinearities((Fragment) vA, 170.0);
        Fragment vB = makeFragmentA();

        assertTrue(vA.isIsomorphicTo(vA));
        assertTrue(vA.isIsomorphicTo(vB));
        assertTrue(vB.isIsomorphicTo(vA));
    }
    
//------------------------------------------------------------------------------

    
}
