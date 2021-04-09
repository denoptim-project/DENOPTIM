package denoptim.molecule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.silent.Bond;

import denoptim.constants.DENOPTIMConstants;
import denoptim.fragspace.FragmentSpace;
import denoptim.molecule.DENOPTIMEdge.BondType;

/**
 * Unit test for DENOPTIMVertex
 * 
 * @author Marco Foscato
 */

public class DENOPTIMVertexTest
{
	private StringBuilder reason = new StringBuilder();
	
//------------------------------------------------------------------------------
	
    @Test
    public void testSameAs_Equal()
    {
        DENOPTIMVertex vA = new EmptyVertex(0);
        DENOPTIMVertex vB = new EmptyVertex(90);
        vA.addAP(0, 1, 1);
        vA.addAP(1, 1, 1);
        vA.addAP(2, 1, 1);
        vA.addAP(3, 1, 1);

        vB.addAP(0, 1, 1);
        vB.addAP(1, 1, 1);
        vB.addAP(2, 1, 1);
        vB.addAP(3, 1, 1);
        //NB: vertex ID must be ignores by the sameAs method

    	assertTrue(vA.sameAs(vB, reason));	
    }
    
//------------------------------------------------------------------------------
	
    @Test
    public void testSameAs_DiffAPConnection()
    {
        DENOPTIMVertex vA = new EmptyVertex(0);
        vA.addAP(0, 1, 1);
        vA.addAP(1, 1, 1);
        vA.addAP(2, 1, 1);
        vA.addAP(3, 1, 1);

        DENOPTIMVertex vB = new EmptyVertex(90);
        vB.addAP(0, 1, 1);
        vB.addAP(1, 1, 1);
        vB.addAP(2, 1, 1);
        vB.addAP(3, 1, 2); //dif
        //NB: vertex ID must be ignores by the sameAs method

        assertFalse(vA.sameAs(vB, reason));
    }
    
//------------------------------------------------------------------------------
	
    @Test
    public void testSameAs_DiffAPNum()
    {
        DENOPTIMVertex vA = new EmptyVertex(0);
        vA.addAP(0, 1, 1);
        vA.addAP(1, 1, 1);
        vA.addAP(2, 1, 1);
        vA.addAP(3, 1, 1);

        DENOPTIMVertex vB = new EmptyVertex(90);
        vB.addAP(0, 1, 1);
        vB.addAP(1, 1, 1);
        vB.addAP(2, 1, 1);
        //NB: vertex ID must be ignores by the sameAs method

        assertFalse(vA.sameAs(vB, reason));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testClone() throws Exception
    {
        // This is just to avoid the warnings about trying to get a bond type
        // when the fragment space in not defined
        String APRULE = "apc";
        HashMap<String, BondType> map = new HashMap<>();
        map.put(APRULE,BondType.DOUBLE);
        FragmentSpace.setBondOrderMap(map);

        DENOPTIMVertex v = new EmptyVertex(0);
        v.addAP(1, 1, 1);
        v.addAP(2, 2, 1);
        v.addAP(3, 2, 1);
        v.setLevel(26);
        
        DENOPTIMVertex c = v.clone();
        
        assertEquals(v.getVertexId(), c.getVertexId(), "Vertex ID");
        assertEquals(v.getNumberOfAPs(), c.getNumberOfAPs(), "Number of APS");
        assertEquals(v.getSymmetricAPSets().size(), 
                c.getSymmetricAPSets().size(), "Number of SymAPs sets");
        assertEquals(v.getLevel(), c.getLevel(), "Level");
        assertEquals(v.isRCV(), c.isRCV(), "RCV flag");
        assertNotEquals(v.hashCode(), c.hashCode(), "Hash code"); 
        
        
        v = new DENOPTIMFragment();
        Atom a1 = new Atom("C", new Point3d(new double[]{0.0, 1.1, 2.2}));
        Atom a2 = new Atom("C", new Point3d(new double[]{1.0, 1.1, 2.2}));
        Atom a3 = new Atom("C", new Point3d(new double[]{2.0, 1.1, 2.2}));
        ((DENOPTIMFragment) v).addAtom(a1);
        ((DENOPTIMFragment) v).addAtom(a2);
        ((DENOPTIMFragment) v).addAtom(a3);
        ((DENOPTIMFragment) v).addBond(new Bond(a1, a2));
        ((DENOPTIMFragment) v).addBond(new Bond(a2, a3));
        String APCLASS = APRULE + DENOPTIMConstants.SEPARATORAPPROPSCL +"0";
        ((DENOPTIMFragment) v).addAPOnAtom(a3, APClass.make(APCLASS), new Point3d(
                new double[]{0.0, 2.2, 3.3}));
        ((DENOPTIMFragment) v).addAPOnAtom(a3, APClass.make(APCLASS), new Point3d(
                new double[]{0.0, 0.0, 3.3}));
        ((DENOPTIMFragment) v).addAPOnAtom(a3, APClass.make(APCLASS), new Point3d(
                new double[]{0.0, 0.0, 1.1}));
        ((DENOPTIMFragment) v).addAPOnAtom(a1, APClass.make(APCLASS), new Point3d(
                new double[]{3.0, 0.0, 3.3}));
        v.setLevel(62);
        
        c = v.clone();
        
        assertEquals(v.getVertexId(), c.getVertexId(), "Vertex ID");
        assertEquals(v.getNumberOfAPs(), c.getNumberOfAPs(), "Number of APS");
        assertEquals(v.getSymmetricAPSets().size(), 
                c.getSymmetricAPSets().size(), "Number of SymAPs sets");
        assertEquals(v.getLevel(), c.getLevel(), "Level");
        assertEquals(v.isRCV(), c.isRCV(), "RCV flag");
        assertNotEquals(v.hashCode(), c.hashCode(), "Hash code");
        assertEquals(v.getAllAPClasses(),c.getAllAPClasses(),"APClass list");
        assertEquals(v.getAllAPClasses().get(0).hashCode(),
                c.getAllAPClasses().get(0).hashCode(),"APClass hash code");
    }
    
//------------------------------------------------------------------------------
}
