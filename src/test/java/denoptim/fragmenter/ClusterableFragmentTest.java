package denoptim.fragmenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.vecmath.Point3d;

import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.isomorphism.Mappings;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import denoptim.constants.DENOPTIMConstants;
import denoptim.files.FileFormat;
import denoptim.fragmenter.FragmentClusterer.DistanceAsRMSD;
import denoptim.graph.APClass;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.FragIsomorphNode;
import denoptim.graph.Fragment;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;
import denoptim.io.DenoptimIO;
import denoptim.programs.fragmenter.CuttingRule;
import denoptim.programs.fragmenter.FragmenterParameters;
import denoptim.programs.fragmenter.MatchedBond;
import denoptim.utils.ManySMARTSQuery;
import denoptim.utils.MoleculeUtils;
import denoptim.utils.Randomizer;
import uk.ac.ebi.beam.Bond;

/**
 * Unit test for fragmenter's tools.
 */
public class ClusterableFragmentTest
{

    /**
     * Private builder of atom containers
     */
    private IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();

//------------------------------------------------------------------------------
    
    @Test
    public void testSetNaturalNodeOrder() throws Exception
    {
        IAtomContainer mol = builder.newAtomContainer();
        mol.addAtom(new Atom("C", new Point3d(1,0,0)));
        mol.addAtom(new Atom("H", new Point3d(2,0,0)));
        mol.addAtom(new Atom("O", new Point3d(3,0,0)));
        mol.addBond(0,1,IBond.Order.SINGLE);
        mol.addBond(0,2,IBond.Order.SINGLE);
        Fragment frag = new Fragment(mol, BBType.UNDEFINED);
        frag.addAP(2, APClass.make("A:0"), new Point3d(4,0,0));
        frag.addAP(1, APClass.make("B:0"), new Point3d(5,0,0));
        frag.addAP(0, APClass.make("B:0"), new Point3d(6,0,0));
        frag.addAP(2, APClass.make("B:0"), new Point3d(7,0,0));
        ClusterableFragment cf = new ClusterableFragment(frag);
        cf.setNaturalNodeOrder();
        
        double value = 0.0;
        for (FragIsomorphNode n : cf.getOrderedNodes())
        {
            value += 1.0;
            assertTrue(Math.abs(value-n.getPoint3d().x) < 0.0001);
        }
    }

//------------------------------------------------------------------------------
    
    @Test
    public void testGetTransformedCopy() throws Exception
    {
        IAtomContainer mol = builder.newAtomContainer();
        mol.addAtom(new Atom("C", new Point3d(1,0,0)));
        mol.addAtom(new Atom("H", new Point3d(2,0,0)));
        mol.addAtom(new Atom("O", new Point3d(3,0,0)));
        mol.addBond(0,1,IBond.Order.SINGLE);
        mol.addBond(0,2,IBond.Order.SINGLE);
        Fragment frag = new Fragment(mol, BBType.UNDEFINED);
        frag.addAP(2, APClass.make("A:0"), new Point3d(4,0,0));
        frag.addAP(1, APClass.make("B:0"), new Point3d(5,0,0));
        frag.addAP(0, APClass.make("B:0"), new Point3d(6,0,0));
        frag.addAP(2, APClass.make("B:0"), new Point3d(7,0,0));
        ClusterableFragment cf = new ClusterableFragment(frag);
        cf.setNaturalNodeOrder();
        
        double[] newCoords = new double[] {-1,-1,-1, -2,-2,-2, -3,-3,-3, 
                -4,-4,-4, -5,-5,-5, -6,-6,-6, -7,-7,-7};
        cf.setCoordsVector(newCoords);
        
        Fragment fragCopy = cf.getTransformedCopy();
        
        assertFalse(frag == fragCopy);
        for (int i=0; i<fragCopy.getAtomCount(); i++)
        {
            IAtom atm = fragCopy.getAtom(i);
            assertTrue(Math.abs(atm.getPoint3d().x - newCoords[i*3])<0.0001);
            assertTrue(Math.abs(atm.getPoint3d().y - newCoords[1+i*3])<0.0001);
            assertTrue(Math.abs(atm.getPoint3d().z - newCoords[2+i*3])<0.0001);
        }
        for (int i=0; i<fragCopy.getNumberOfAPs(); i++)
        {
            AttachmentPoint ap = fragCopy.getAP(i);
            assertTrue(Math.abs(ap.getDirectionVector().x 
                    - newCoords[fragCopy.getAtomCount()*3+i*3])<0.0001);
            assertTrue(Math.abs(ap.getDirectionVector().y 
                    - newCoords[fragCopy.getAtomCount()*3+1+i*3])<0.0001);
            assertTrue(Math.abs(ap.getDirectionVector().z 
                    - newCoords[fragCopy.getAtomCount()*3+2+i*3])<0.0001);
        }
    }
    
//------------------------------------------------------------------------------

}
