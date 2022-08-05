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
public class FragmentClustererTest
{

    /**
     * Private builder of atom containers
     */
    private IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
    
    /**
     * Random number generator
     */
    private Randomizer rng = new Randomizer(1L);

//------------------------------------------------------------------------------
    
    @Test
    public void testDistanceAsRMSD()
    {
        DistanceAsRMSD measure = new DistanceAsRMSD();
        
        double[] pA = new double[] {0,0,0,   0,0,0};
        double[] pB = new double[] {0,0,0,   1,0,0};
        double[] pC = new double[] {0,0,0,   2,0,0};

        // NB: this RMSD upon alignement not distance!
        double val = measure.compute(pA, pB); 
        assertTrue(Math.abs(val - 0.5) < 0.0001);
        assertTrue(Math.abs(measure.compute(pB, pA) - 0.5) < 0.0001);
        assertTrue(Math.abs(measure.compute(pA, pA) - 0) < 0.0001);
        assertTrue(Math.abs(measure.compute(pB, pB) - 0) < 0.0001);
        
        assertTrue(Math.abs(measure.compute(pA, pC) - 1.0) < 0.0001);
        assertTrue(Math.abs(measure.compute(pC, pA) - 1.0) < 0.0001);

        assertTrue(Math.abs(measure.compute(pB, pC) - 0.5) < 0.0001);
        assertTrue(Math.abs(measure.compute(pC, pB) - 0.5) < 0.0001);
        
        pA = new double[] {0,0,0,   0,0,0};
        pB = new double[] {0,0,0,   0,1,0};
        pC = new double[] {0,0,0,   0,2,0};
        assertTrue(Math.abs(measure.compute(pA, pB) - 0.5) < 0.0001);
        assertTrue(Math.abs(measure.compute(pB, pA) - 0.5) < 0.0001);
        assertTrue(Math.abs(measure.compute(pA, pA) - 0) < 0.0001);
        assertTrue(Math.abs(measure.compute(pB, pB) - 0) < 0.0001);
        assertTrue(Math.abs(measure.compute(pA, pC) - 1.0) < 0.0001);
        assertTrue(Math.abs(measure.compute(pC, pA) - 1.0) < 0.0001);
        assertTrue(Math.abs(measure.compute(pB, pC) - 0.5) < 0.0001);
        assertTrue(Math.abs(measure.compute(pC, pB) - 0.5) < 0.0001);
        
        pA = new double[] {0,0,0,   0,0,0};
        pB = new double[] {0,0,0,   0,0,1};
        pC = new double[] {0,0,0,   0,0,2};
        assertTrue(Math.abs(measure.compute(pA, pB) - 0.5) < 0.0001);
        assertTrue(Math.abs(measure.compute(pB, pA) - 0.5) < 0.0001);
        assertTrue(Math.abs(measure.compute(pA, pA) - 0) < 0.0001);
        assertTrue(Math.abs(measure.compute(pB, pB) - 0) < 0.0001);
        assertTrue(Math.abs(measure.compute(pA, pC) - 1.0) < 0.0001);
        assertTrue(Math.abs(measure.compute(pC, pA) - 1.0) < 0.0001);
        assertTrue(Math.abs(measure.compute(pB, pC) - 0.5) < 0.0001);
        assertTrue(Math.abs(measure.compute(pC, pB) - 0.5) < 0.0001);

        pA = new double[] {0,0,0,   0,0,0};
        pB = new double[] {0,0,1,   0,0,0};
        pC = new double[] {0,0,2,   0,0,0};
        assertTrue(Math.abs(measure.compute(pA, pB) - 0.5) < 0.0001);
        assertTrue(Math.abs(measure.compute(pB, pA) - 0.5) < 0.0001);
        assertTrue(Math.abs(measure.compute(pA, pA) - 0) < 0.0001);
        assertTrue(Math.abs(measure.compute(pB, pB) - 0) < 0.0001);
        assertTrue(Math.abs(measure.compute(pA, pC) - 1.0) < 0.0001);
        assertTrue(Math.abs(measure.compute(pC, pA) - 1.0) < 0.0001);
        assertTrue(Math.abs(measure.compute(pB, pC) - 0.5) < 0.0001);
        assertTrue(Math.abs(measure.compute(pC, pB) - 0.5) < 0.0001);
        
        pA = new double[] {0,0,0,   1,0,0};
        pB = new double[] {0,0,0,   6,0,0};
        assertTrue(Math.abs(measure.compute(pA, pB) - 2.5) < 0.0001);
        assertTrue(Math.abs(measure.compute(pB, pA) - 2.5) < 0.0001);
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetRMSDStatsOfNoisyDistorsions()
    {
        double[] center = new double[] {
                0,0,0,
                1,0,0,
                0,1,0,
                0,0,1};
        SummaryStatistics s1 = FragmentClusterer.getRMSDStatsOfNoisyDistorsions(
                center, 100, 0.1);
        double[] center2 = new double[] {
                0,0,0,
                0,0,10,
                0,20,0,
                0,0,20};
        SummaryStatistics s2 = FragmentClusterer.getRMSDStatsOfNoisyDistorsions(
                center2, 100, 0.1);
        assertTrue(Math.abs(s1.getMean()-s2.getMean()) < 0.05);
        
        center = new double[] {
                0,0,0,
                1,0,0,
                0,1,0,
                0,0,1,
                -1,0,0,
                0,-1,0,
                0,0,-1,
                1,0,0,
                0,1,0,
                0,0,1,
                -1,0,0,
                0,-1,0,
                0,0,-1,
                1,0,0,
                0,1,0,
                0,0,1,
                -1,0,0,
                0,-1,0,
                0,0,-1};
        SummaryStatistics s3 = FragmentClusterer.getRMSDStatsOfNoisyDistorsions(
                center, 100, 0.1);
        center2 = new double[] {
                0,0,0,
                20,0,0,
                0,20,0,
                0,0,20,
                -20,0,0,
                0,-20,0,
                0,0,-20,
                60,0,0,
                0,60,0,
                0,0,60,
                -60,0,0,
                0,-60,0,
                0,0,-60,
                80,0,0,
                0,80,0,
                0,0,80,
                -80,0,0,
                0,-80,0,
                0,0,-80};
        SummaryStatistics s4 = FragmentClusterer.getRMSDStatsOfNoisyDistorsions(
                center2, 100, 0.1);
        assertTrue(Math.abs(s3.getMean()-s4.getMean()) < 0.05);
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testCluster() throws Exception
    {
        double noise = 0.25; 
        // NB: the distortions are [-noise,noise] and is uniformely distributed
        // So, the extreme cases have same probability of no distortion
        
        // Build first set of fragment (should all be in one cluster)
        List<ClusterableFragment> sample = new ArrayList<ClusterableFragment>();
        Point3d[] pointsA = new Point3d[] {
                new Point3d(-0.4574,-0.0273,0.3953), 
                new Point3d(1.2914,-0.0103,-0.0437),
                new Point3d(-1.0737,-1.1960,-0.1490),
                new Point3d(-0.5595,-0.0346,1.4805),
                new Point3d(-1.0796,1.1129,-0.1241)};
        for (int i=0; i<10; i++)
        {
            IAtomContainer mol = builder.newAtomContainer();
            mol.addAtom(new Atom("C", getNoisyPoint(pointsA[0],noise)));
            mol.addAtom(new Atom("H", getNoisyPoint(pointsA[1],noise)));
            mol.addAtom(new Atom("O", getNoisyPoint(pointsA[2],noise)));
            mol.addBond(0,1,IBond.Order.SINGLE);
            mol.addBond(0,2,IBond.Order.SINGLE);
            Fragment frag = new Fragment(mol, BBType.UNDEFINED);
            frag.addAP(0, APClass.make("A:0"), getNoisyPoint(pointsA[3],noise));
            frag.addAP(0, APClass.make("B:0"), getNoisyPoint(pointsA[4],noise));
            ClusterableFragment cf = new ClusterableFragment(frag);
            cf.setNaturalNodeOrder();
            sample.add(cf);
        }
        
        FragmenterParameters settings = new FragmenterParameters();
        
        //TODO-gg remove
        settings.startConsoleLogger("ClustererTest");
        settings.setVerbosity(2);
        
        FragmentClusterer fc = new FragmentClusterer(sample,settings);
        // NB: unimodal distribution is assumed as a start and "confirmed"
        // empirically only by the small amount of change in the elbow plot.
        fc.cluster();
        assertEquals(1,fc.getClusters().size());
        
        Point3d[] pointsB = new Point3d[] {
                new Point3d(0.4574,-0.0273,0.3953), 
                new Point3d(-1.2914,-0.0103,-0.0437),
                new Point3d(1.0737,-1.1960,-0.1490),
                new Point3d(0.5595,-0.0346,1.4805),
                new Point3d(1.0796,1.1129,-0.1241)};
        for (int i=0; i<5; i++)
        {
            IAtomContainer mol = builder.newAtomContainer();
            mol.addAtom(new Atom("C", getNoisyPoint(pointsB[0],noise)));
            mol.addAtom(new Atom("H", getNoisyPoint(pointsB[1],noise)));
            mol.addAtom(new Atom("O", getNoisyPoint(pointsB[2],noise)));
            mol.addBond(0,1,IBond.Order.SINGLE);
            mol.addBond(0,2,IBond.Order.SINGLE);
            Fragment frag = new Fragment(mol, BBType.UNDEFINED);
            frag.addAP(0, APClass.make("A:0"), getNoisyPoint(pointsB[3],noise));
            frag.addAP(0, APClass.make("B:0"), getNoisyPoint(pointsB[4],noise));
            ClusterableFragment cf = new ClusterableFragment(frag);
            cf.setNaturalNodeOrder();
            sample.add(cf);
        }
        
        fc = new FragmentClusterer(sample,settings);
        fc.cluster();
        assertEquals(2,fc.getClusters().size());
        
        Point3d[] pointsC = new Point3d[] {
                new Point3d(0,0,0), 
                new Point3d(2,0,0),
                new Point3d(0,2,0),
                new Point3d(-2,0,0),
                new Point3d(0,-2,0)};
        for (int i=0; i<5; i++)
        {
            IAtomContainer mol = builder.newAtomContainer();
            mol.addAtom(new Atom("C", getNoisyPoint(pointsC[0],noise)));
            mol.addAtom(new Atom("H", getNoisyPoint(pointsC[1],noise)));
            mol.addAtom(new Atom("O", getNoisyPoint(pointsC[2],noise)));
            mol.addBond(0,1,IBond.Order.SINGLE);
            mol.addBond(0,2,IBond.Order.SINGLE);
            Fragment frag = new Fragment(mol, BBType.UNDEFINED);
            frag.addAP(0, APClass.make("A:0"), getNoisyPoint(pointsC[3],noise));
            frag.addAP(0, APClass.make("B:0"), getNoisyPoint(pointsC[4],noise));
            ClusterableFragment cf = new ClusterableFragment(frag);
            cf.setNaturalNodeOrder();
            sample.add(cf);
        }
                
        fc = new FragmentClusterer(sample,settings);
        fc.cluster();
        assertEquals(3,fc.getClusters().size());
        
        Point3d[] pointsD = new Point3d[] {
                new Point3d(5,0,0), 
                new Point3d(6.5,0,0),
                new Point3d(8,0,0),
                new Point3d(5,1,0),
                new Point3d(0,0,0)};
        for (int i=0; i<5; i++)
        {
            IAtomContainer mol = builder.newAtomContainer();
            mol.addAtom(new Atom("C", getNoisyPoint(pointsD[0],noise)));
            mol.addAtom(new Atom("H", getNoisyPoint(pointsD[1],noise)));
            mol.addAtom(new Atom("O", getNoisyPoint(pointsD[2],noise)));
            mol.addBond(0,1,IBond.Order.SINGLE);
            mol.addBond(0,2,IBond.Order.SINGLE);
            Fragment frag = new Fragment(mol, BBType.UNDEFINED);
            frag.addAP(0, APClass.make("A:0"), getNoisyPoint(pointsD[3],noise));
            frag.addAP(0, APClass.make("B:0"), getNoisyPoint(pointsD[4],noise));
            ClusterableFragment cf = new ClusterableFragment(frag);
            cf.setNaturalNodeOrder();
            sample.add(cf);
        }
                
        fc = new FragmentClusterer(sample,settings);
        fc.cluster();
        assertEquals(4,fc.getClusters().size());
        
        Point3d[] pointsE = new Point3d[] {
                new Point3d(5,0,0), 
                new Point3d(6.5,0,0),
                new Point3d(8,0,0),
                new Point3d(5,1,0),
                new Point3d(7,0,0)};
        for (int i=0; i<5; i++)
        {
            IAtomContainer mol = builder.newAtomContainer();
            mol.addAtom(new Atom("C", getNoisyPoint(pointsE[0],noise)));
            mol.addAtom(new Atom("H", getNoisyPoint(pointsE[1],noise)));
            mol.addAtom(new Atom("O", getNoisyPoint(pointsE[2],noise)));
            mol.addBond(0,1,IBond.Order.SINGLE);
            mol.addBond(0,2,IBond.Order.SINGLE);
            Fragment frag = new Fragment(mol, BBType.UNDEFINED);
            frag.addAP(0, APClass.make("A:0"), getNoisyPoint(pointsE[3],noise));
            frag.addAP(0, APClass.make("B:0"), getNoisyPoint(pointsE[4],noise));
            ClusterableFragment cf = new ClusterableFragment(frag);
            cf.setNaturalNodeOrder();
            sample.add(cf);
        }
                
        fc = new FragmentClusterer(sample,settings);
        fc.cluster();
        assertEquals(5,fc.getClusters().size());
        
        Point3d[] pointsF = new Point3d[] {
                new Point3d(4,0,0), 
                new Point3d(6.5,0,0),
                new Point3d(8,0,0),
                new Point3d(3,0,0),
                new Point3d(7,0,0)};
        for (int i=0; i<5; i++)
        {
            IAtomContainer mol = builder.newAtomContainer();
            mol.addAtom(new Atom("C", getNoisyPoint(pointsF[0],noise)));
            mol.addAtom(new Atom("H", getNoisyPoint(pointsF[1],noise)));
            mol.addAtom(new Atom("O", getNoisyPoint(pointsF[2],noise)));
            mol.addBond(0,1,IBond.Order.SINGLE);
            mol.addBond(0,2,IBond.Order.SINGLE);
            Fragment frag = new Fragment(mol, BBType.UNDEFINED);
            frag.addAP(0, APClass.make("A:0"), getNoisyPoint(pointsF[3],noise));
            frag.addAP(0, APClass.make("B:0"), getNoisyPoint(pointsF[4],noise));
            ClusterableFragment cf = new ClusterableFragment(frag);
            cf.setNaturalNodeOrder();
            sample.add(cf);
        }
                
        fc = new FragmentClusterer(sample,settings);
        fc.cluster();
        assertEquals(6,fc.getClusters().size());
        
        // Keep this code: it might be useful to look at the geometries.
        
        ArrayList<Vertex> lstVrtx = new ArrayList<Vertex>();
        for (ClusterableFragment cf : sample)
            lstVrtx.add(cf.getOriginalFragment());
        DenoptimIO.writeVertexesToFile(new File("/tmp/cf.sdf"), FileFormat.VRTXSDF, lstVrtx, false);
        
        //TODO-gg see what the centroids look like! 
        List<Fragment> centroids = fc.getClusterCentroids();
        for (int i=0; i<fc.getClusters().size(); i++)
        {
            ArrayList<IAtomContainer> mols = new ArrayList<IAtomContainer>();
            for (ClusterableFragment cf : fc.getClusters().get(i).getPoints())
            {
                mols.add(getMol(cf.getTransformedCopy()));
            }
            DenoptimIO.writeSDFFile("/tmp/cluster_"+i+".sdf", mols);
            
            
            ArrayList<IAtomContainer> molsTrs = new ArrayList<IAtomContainer>();
            for (Fragment f : fc.getTransformedClusters().get(i))
            {
                molsTrs.add(getMol(f));
            }
            DenoptimIO.writeSDFFile("/tmp/cluster_"+i+"_trns.sdf", molsTrs);
            
            IAtomContainer center = getMol(centroids.get(i));
            DenoptimIO.writeSDFFile("/tmp/cluster_"+i+"_centroid.sdf", center);
        }
        
    }
    
    //TODO-gg del 
    private IAtomContainer getMol(Fragment frag)
    {
        IAtomContainer mol = builder.newAtomContainer();
        for (IAtom a : frag.atoms())
            mol.addAtom(new Atom(a.getSymbol(),a.getPoint3d()));
        for (AttachmentPoint ap : frag.getAttachmentPoints())
            mol.addAtom(new PseudoAtom("W",ap.getDirectionVector()));
        return mol;
    }
    
//-----------------------------------------------------------------------------

    @Test
    public void testCluster2() throws Exception
    {
        double noise = 0.25; 
        // NB: the distortions are [-noise,noise] and is uniformely distributed
        // So, the extreme cases have same probability of no distortion
        
        List<ClusterableFragment> sample = new ArrayList<ClusterableFragment>();

        Point3d[] points = new Point3d[] {
                new Point3d(0,0,0), 
                new Point3d(1,0,0)};
        for (int i=0; i<5; i++)
        {
            IAtomContainer mol = builder.newAtomContainer();
            mol.addAtom(new Atom("C", getNoisyPoint(points[0],noise)));
            Fragment frag = new Fragment(mol, BBType.UNDEFINED);
            frag.addAP(0, APClass.make("A:0"), getNoisyPoint(points[1],noise));
            ClusterableFragment cf = new ClusterableFragment(frag);
            cf.setNaturalNodeOrder();
            sample.add(cf);
        }
        
        FragmenterParameters settings = new FragmenterParameters();
        
        //TODO-gg remove
        //settings.startConsoleLogger("ClustererTest");
        //settings.setVerbosity(2);
        
        FragmentClusterer fc = new FragmentClusterer(sample,settings);
        fc.cluster();
        assertEquals(1,fc.getClusters().size());
        
        
        // Adding same points that are just placed elsewhere in space
        points = new Point3d[] {
                new Point3d(0,0,-1), 
                new Point3d(0,0,-2)};
        for (int i=0; i<5; i++)
        {
            IAtomContainer mol = builder.newAtomContainer();
            mol.addAtom(new Atom("C", getNoisyPoint(points[0],noise)));
            Fragment frag = new Fragment(mol, BBType.UNDEFINED);
            frag.addAP(0, APClass.make("A:0"), getNoisyPoint(points[1],noise));
            ClusterableFragment cf = new ClusterableFragment(frag);
            cf.setNaturalNodeOrder();
            sample.add(cf);
        }
        fc = new FragmentClusterer(sample,settings);
        fc.cluster();
        assertEquals(1,fc.getClusters().size());
        
        // Add more clusters (each different from each other and the old ones)
        for (int k=2; k<10; k++)
        {
            points = new Point3d[] {
                    new Point3d(k,0,0), 
                    new Point3d(0,0,0)};
            for (int i=0; i<5; i++)
            {
                IAtomContainer mol = builder.newAtomContainer();
                mol.addAtom(new Atom("C", getNoisyPoint(points[0],noise)));
                Fragment frag = new Fragment(mol, BBType.UNDEFINED);
                frag.addAP(0, APClass.make("A:0"), getNoisyPoint(points[1],noise));
                ClusterableFragment cf = new ClusterableFragment(frag);
                cf.setNaturalNodeOrder();
                sample.add(cf);
            }
            fc = new FragmentClusterer(sample,settings);
            fc.cluster();
            assertEquals(k,fc.getClusters().size());
        }
        
        
        // Keep this code: it might be useful to look at the geometries.
        /*
        ArrayList<Vertex> lstVrtx = new ArrayList<Vertex>();
        for (ClusterableFragment cf : sample)
            lstVrtx.add(cf.getOriginalFragment());
        DenoptimIO.writeVertexesToFile(new File("/tmp/cf.sdf"), FileFormat.VRTXSDF, lstVrtx, false);
        */
    }

//------------------------------------------------------------------------------
    
    /**
     * The noise magnitude is to be interpreted as half of the overall magnitude.
     * And the distribution of noise is uniform in 
     * [-noiseMagnitude,noiseMagnitude].
     */
    private Point3d getNoisyPoint(Point3d p, double noiseMagnitude)
    {
        Point3d noise = rng.getNoisyPoint(noiseMagnitude);
        return new Point3d(p.x+noise.x, p.y+noise.y, p.z+noise.z);
    }
    
//------------------------------------------------------------------------------

}
