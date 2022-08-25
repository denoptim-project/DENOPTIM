package denoptim.fragmenter;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import denoptim.fragmenter.FragmentClusterer.DistanceAsRMSD;
import denoptim.graph.APClass;
import denoptim.graph.Fragment;
import denoptim.graph.Vertex.BBType;

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
