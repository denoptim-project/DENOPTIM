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
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.Bond;

import denoptim.constants.DENOPTIMConstants;
import denoptim.fragspace.FragmentSpace;
import denoptim.io.DenoptimIO;
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
	
//------------------------------------------------------------------------------
	
    @Test
    public void testHandlingAPsAsObjOrProperty() throws Exception
    {
    	DENOPTIMFragment frg1 = new DENOPTIMFragment();
    	Atom a1 = new Atom("C", new Point3d(new double[]{0.0, 1.1, 2.2}));
    	Atom a2 = new Atom("C", new Point3d(new double[]{1.0, 1.1, 2.2}));
    	Atom a3 = new Atom("C", new Point3d(new double[]{2.0, 1.1, 2.2}));
    	frg1.addAtom(a1);
    	frg1.addAtom(a2);
    	frg1.addAtom(a3);
    	frg1.addBond(new Bond(a1, a2));
    	frg1.addBond(new Bond(a2, a3));
    	frg1.addAP(2, APCLASS, new Point3d(new double[]{0.0, 2.2, 3.3}));
    	frg1.addAP(2, APCLASS, new Point3d(new double[]{0.0, 0.0, 3.3}));
    	frg1.addAP(2, APCLASS, new Point3d(new double[]{0.0, 0.0, 1.1}));
    	frg1.addAP(0, APCLASS, new Point3d(new double[]{3.0, 0.0, 3.3}));
    	
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
    	
        //TODO del
    	/*
        System.out.println("FRAGSPACE: "+FragmentSpace.isDefined());
        System.out.println("FRAGSPACE: "+FragmentSpace.getBondOrderMap());
        */
    	
    	assertEquals(frg1.getAPCount(),frg2.getAPCount(),"Equality of #AP");
    	assertEquals(frg1.getAPCountOnAtom(0),frg2.getAPCountOnAtom(0),
    	        "Equality of #AP-on-atom");
    	
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testConversionToIAC() throws Exception
    {
    	// WARNING: the conversion does not project the atom properties into
    	// molecular properties. So the APs do not appear in the mol properties.
    	
    	DENOPTIMFragment frg1 = new DENOPTIMFragment();
    	Atom a1 = new Atom("C", new Point3d(new double[]{0.0, 1.1, 2.2}));
    	Atom a2 = new Atom("C", new Point3d(new double[]{1.0, 1.1, 2.2}));
    	Atom a3 = new Atom("C", new Point3d(new double[]{2.0, 1.1, 2.2}));
    	frg1.addAtom(a1);
    	frg1.addAtom(a2);
    	frg1.addAtom(a3);
    	frg1.addBond(new Bond(a1, a2));
    	frg1.addBond(new Bond(a2, a3));
    	frg1.addAP(2, APCLASS, new Point3d(new double[]{0.0, 2.2, 3.3}));
    	frg1.addAP(2, APCLASS, new Point3d(new double[]{0.0, 0.0, 3.3}));
    	frg1.addAP(2, APCLASS, new Point3d(new double[]{0.0, 0.0, 1.1}));
    	frg1.addAP(0, APCLASS, new Point3d(new double[]{3.0, 0.0, 3.3}));
    	
    	IAtomContainer iac = new AtomContainer(frg1.getIAtomContainer());
    	DENOPTIMFragment frg2 = new DENOPTIMFragment(iac);
    	
    	assertEquals(4,frg1.getAPCount(),"Size if frg1");
    	assertEquals(4,frg2.getAPCount(),"Size if frg2");
    	assertEquals(1,frg1.getAPCountOnAtom(0),"Size if frg1 atm0");
    	assertEquals(1,frg2.getAPCountOnAtom(0),"Size if frg2 atm0");
    	assertEquals(3,frg1.getAPCountOnAtom(2),"Size if frg1 atm2");
    	assertEquals(3,frg2.getAPCountOnAtom(2),"Size if frg2 atm2");
    }
    
//------------------------------------------------------------------------------
}
