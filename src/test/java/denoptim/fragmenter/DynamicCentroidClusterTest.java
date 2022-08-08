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

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

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
public class DynamicCentroidClusterTest
{

    /**
     * Private builder of atom containers
     */
    private IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();

//------------------------------------------------------------------------------
    
    @Test
    public void testGetCentroid() throws Exception
    {
        List<ClusterableFragment> sample = new ArrayList<ClusterableFragment>();
        
        IAtomContainer mol0 = builder.newAtomContainer();
        mol0.addAtom(new Atom("C", new Point3d(0,0,0)));
        mol0.addAtom(new Atom("H", new Point3d(2,0,0)));
        mol0.addBond(0,1,IBond.Order.SINGLE);
        Fragment frag0 = new Fragment(mol0, BBType.UNDEFINED);
        frag0.addAP(0, APClass.make("A:0"), new Point3d(-2,0,0));
        ClusterableFragment cf0 = new ClusterableFragment(frag0);
        cf0.setNaturalNodeOrder();
        sample.add(cf0);
        
        IAtomContainer mol1 = builder.newAtomContainer();
        mol1.addAtom(new Atom("C", new Point3d(0,0,0)));
        mol1.addAtom(new Atom("H", new Point3d(1,0,0)));
        mol1.addBond(0,1,IBond.Order.SINGLE);
        Fragment frag1 = new Fragment(mol1, BBType.UNDEFINED);
        frag1.addAP(0, APClass.make("A:0"), new Point3d(-1,0,0));
        ClusterableFragment cf1 = new ClusterableFragment(frag1);
        cf1.setNaturalNodeOrder();
        sample.add(cf1);
        
        IAtomContainer mol2 = builder.newAtomContainer();
        mol2.addAtom(new Atom("C", new Point3d(0,0,0)));
        mol2.addAtom(new Atom("H", new Point3d(1.5,0,0)));
        mol2.addBond(0,1,IBond.Order.SINGLE);
        Fragment frag2 = new Fragment(mol2, BBType.UNDEFINED);
        frag2.addAP(0, APClass.make("A:0"), new Point3d(-1.5,0,0));
        ClusterableFragment cf2 = new ClusterableFragment(frag2);
        cf2.setNaturalNodeOrder();
        sample.add(cf2);
        
        DynamicCentroidCluster cluster = new DynamicCentroidCluster();
        cluster.addPoint(cf0);
        cluster.addPoint(cf1);
        cluster.addPoint(cf2);
        
        ClusterableFragment centroid = cluster.getCentroid();
        
        DistanceAsRMSD measure = new DistanceAsRMSD();
        assertTrue(measure.compute(centroid.getPoint(), cf2.getPoint())<0.0001);
        assertTrue(centroid != cf2);
    }

//------------------------------------------------------------------------------
    
    @Test
    public void testGetNearestToCentroid() throws Exception
    {
        List<ClusterableFragment> sample = new ArrayList<ClusterableFragment>();
        
        IAtomContainer mol0 = builder.newAtomContainer();
        mol0.addAtom(new Atom("C", new Point3d(0,0,0)));
        mol0.addAtom(new Atom("H", new Point3d(2,0,0)));
        mol0.addBond(0,1,IBond.Order.SINGLE);
        Fragment frag0 = new Fragment(mol0, BBType.UNDEFINED);
        frag0.addAP(0, APClass.make("A:0"), new Point3d(-2,0,0));
        ClusterableFragment cf0 = new ClusterableFragment(frag0);
        cf0.setNaturalNodeOrder();
        sample.add(cf0);
        
        IAtomContainer mol1 = builder.newAtomContainer();
        mol1.addAtom(new Atom("C", new Point3d(0,0,0)));
        mol1.addAtom(new Atom("H", new Point3d(1,0,0)));
        mol1.addBond(0,1,IBond.Order.SINGLE);
        Fragment frag1 = new Fragment(mol1, BBType.UNDEFINED);
        frag1.addAP(0, APClass.make("A:0"), new Point3d(-1,0,0));
        ClusterableFragment cf1 = new ClusterableFragment(frag1);
        cf1.setNaturalNodeOrder();
        sample.add(cf1);
        
        IAtomContainer mol2 = builder.newAtomContainer();
        mol2.addAtom(new Atom("C", new Point3d(0,0,0)));
        mol2.addAtom(new Atom("H", new Point3d(1.5,0,0)));
        mol2.addBond(0,1,IBond.Order.SINGLE);
        Fragment frag2 = new Fragment(mol2, BBType.UNDEFINED);
        frag2.addAP(0, APClass.make("A:0"), new Point3d(-1.5,0,0));
        ClusterableFragment cf2 = new ClusterableFragment(frag2);
        cf2.setNaturalNodeOrder();
        sample.add(cf2);
        
        DynamicCentroidCluster cluster = new DynamicCentroidCluster();
        cluster.addPoint(cf0);
        cluster.addPoint(cf1);
        cluster.addPoint(cf2);
        

        DistanceAsRMSD measure = new DistanceAsRMSD();
        ClusterableFragment nearest = cluster.getNearestToCentroid(measure);
        assertTrue(measure.compute(nearest.getPoint(), cf2.getPoint())<0.0001);
        assertTrue(nearest == cf2);
    }
    
//------------------------------------------------------------------------------

}
