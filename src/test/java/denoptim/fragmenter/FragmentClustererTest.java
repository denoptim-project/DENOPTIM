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
   
  //-----------------------------------------------------------------------------

    @Test
    public void testCluster() throws Exception
    {
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
            mol.addAtom(new Atom("C", getNoisyPoint(pointsA[0])));
            mol.addAtom(new Atom("H", getNoisyPoint(pointsA[1])));
            mol.addAtom(new Atom("O", getNoisyPoint(pointsA[2])));
            mol.addBond(0,1,IBond.Order.SINGLE);
            mol.addBond(0,2,IBond.Order.SINGLE);
            Fragment frag = new Fragment(mol, BBType.UNDEFINED);
            frag.addAP(0, APClass.make("A:0"), getNoisyPoint(pointsA[3]));
            frag.addAP(0, APClass.make("B:0"), getNoisyPoint(pointsA[4]));
            ClusterableFragment cf = new ClusterableFragment(frag);
            cf.setOrderOfNodes(getNatualrNodeOrder(frag));
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
        assertEquals(1,fc.getBestSet().size());
        
        Point3d[] pointsB = new Point3d[] {
                new Point3d(0.4574,-0.0273,0.3953), 
                new Point3d(-1.2914,-0.0103,-0.0437),
                new Point3d(1.0737,-1.1960,-0.1490),
                new Point3d(0.5595,-0.0346,1.4805),
                new Point3d(1.0796,1.1129,-0.1241)};
        for (int i=0; i<5; i++)
        {
            IAtomContainer mol = builder.newAtomContainer();
            mol.addAtom(new Atom("C", getNoisyPoint(pointsB[0])));
            mol.addAtom(new Atom("H", getNoisyPoint(pointsB[1])));
            mol.addAtom(new Atom("O", getNoisyPoint(pointsB[2])));
            mol.addBond(0,1,IBond.Order.SINGLE);
            mol.addBond(0,2,IBond.Order.SINGLE);
            Fragment frag = new Fragment(mol, BBType.UNDEFINED);
            frag.addAP(0, APClass.make("A:0"), getNoisyPoint(pointsB[3]));
            frag.addAP(0, APClass.make("B:0"), getNoisyPoint(pointsB[4]));
            ClusterableFragment cf = new ClusterableFragment(frag);
            cf.setOrderOfNodes(getNatualrNodeOrder(frag));
            sample.add(cf);
        }
        
        fc = new FragmentClusterer(sample,settings);
        fc.cluster();
        assertEquals(2,fc.getBestSet().size());
        
        Point3d[] pointsC = new Point3d[] {
                new Point3d(0,0,0), 
                new Point3d(2,0,0),
                new Point3d(0,2,0),
                new Point3d(-2,0,0),
                new Point3d(0,-2,0)};
        for (int i=0; i<3; i++)
        {
            IAtomContainer mol = builder.newAtomContainer();
            mol.addAtom(new Atom("C", getNoisyPoint(pointsC[0])));
            mol.addAtom(new Atom("H", getNoisyPoint(pointsC[1])));
            mol.addAtom(new Atom("O", getNoisyPoint(pointsC[2])));
            mol.addBond(0,1,IBond.Order.SINGLE);
            mol.addBond(0,2,IBond.Order.SINGLE);
            Fragment frag = new Fragment(mol, BBType.UNDEFINED);
            frag.addAP(0, APClass.make("A:0"), getNoisyPoint(pointsC[3]));
            frag.addAP(0, APClass.make("B:0"), getNoisyPoint(pointsC[4]));
            ClusterableFragment cf = new ClusterableFragment(frag);
            cf.setOrderOfNodes(getNatualrNodeOrder(frag));
            sample.add(cf);
        }
                
        fc = new FragmentClusterer(sample,settings);
        fc.cluster();
        assertEquals(3,fc.getBestSet().size());
        
        Point3d[] pointsD = new Point3d[] {
                new Point3d(5,0,0), 
                new Point3d(6.5,0,0),
                new Point3d(8,0,0),
                new Point3d(5,1,0),
                new Point3d(0,0,0)};
        for (int i=0; i<3; i++)
        {
            IAtomContainer mol = builder.newAtomContainer();
            mol.addAtom(new Atom("C", getNoisyPoint(pointsD[0])));
            mol.addAtom(new Atom("H", getNoisyPoint(pointsD[1])));
            mol.addAtom(new Atom("O", getNoisyPoint(pointsD[2])));
            mol.addBond(0,1,IBond.Order.SINGLE);
            mol.addBond(0,2,IBond.Order.SINGLE);
            Fragment frag = new Fragment(mol, BBType.UNDEFINED);
            frag.addAP(0, APClass.make("A:0"), getNoisyPoint(pointsD[3]));
            frag.addAP(0, APClass.make("B:0"), getNoisyPoint(pointsD[4]));
            ClusterableFragment cf = new ClusterableFragment(frag);
            cf.setOrderOfNodes(getNatualrNodeOrder(frag));
            sample.add(cf);
        }
                
        fc = new FragmentClusterer(sample,settings);
        fc.cluster();
        assertEquals(4,fc.getBestSet().size());
        
        Point3d[] pointsE = new Point3d[] {
                new Point3d(5,0,0), 
                new Point3d(6.5,0,0),
                new Point3d(8,0,0),
                new Point3d(5,1,0),
                new Point3d(7,0,0)};
        for (int i=0; i<3; i++)
        {
            IAtomContainer mol = builder.newAtomContainer();
            mol.addAtom(new Atom("C", getNoisyPoint(pointsE[0])));
            mol.addAtom(new Atom("H", getNoisyPoint(pointsE[1])));
            mol.addAtom(new Atom("O", getNoisyPoint(pointsE[2])));
            mol.addBond(0,1,IBond.Order.SINGLE);
            mol.addBond(0,2,IBond.Order.SINGLE);
            Fragment frag = new Fragment(mol, BBType.UNDEFINED);
            frag.addAP(0, APClass.make("A:0"), getNoisyPoint(pointsE[3]));
            frag.addAP(0, APClass.make("B:0"), getNoisyPoint(pointsE[4]));
            ClusterableFragment cf = new ClusterableFragment(frag);
            cf.setOrderOfNodes(getNatualrNodeOrder(frag));
            sample.add(cf);
        }
                
        fc = new FragmentClusterer(sample,settings);
        fc.cluster();
        assertEquals(5,fc.getBestSet().size());
        
        Point3d[] pointsF = new Point3d[] {
                new Point3d(4,0,0), 
                new Point3d(6.5,0,0),
                new Point3d(8,0,0),
                new Point3d(3,0,0),
                new Point3d(7,0,0)};
        for (int i=0; i<3; i++)
        {
            IAtomContainer mol = builder.newAtomContainer();
            mol.addAtom(new Atom("C", getNoisyPoint(pointsF[0])));
            mol.addAtom(new Atom("H", getNoisyPoint(pointsF[1])));
            mol.addAtom(new Atom("O", getNoisyPoint(pointsF[2])));
            mol.addBond(0,1,IBond.Order.SINGLE);
            mol.addBond(0,2,IBond.Order.SINGLE);
            Fragment frag = new Fragment(mol, BBType.UNDEFINED);
            frag.addAP(0, APClass.make("A:0"), getNoisyPoint(pointsF[3]));
            frag.addAP(0, APClass.make("B:0"), getNoisyPoint(pointsF[4]));
            ClusterableFragment cf = new ClusterableFragment(frag);
            cf.setOrderOfNodes(getNatualrNodeOrder(frag));
            sample.add(cf);
        }
                
        fc = new FragmentClusterer(sample,settings);
        fc.cluster();
        assertEquals(6,fc.getBestSet().size());
        
        // Keep this code: it might be useful to look at the geometries.
        /*
        ArrayList<Vertex> lstVrtx = new ArrayList<Vertex>();
        for (ClusterableFragment cf : sample)
            lstVrtx.add(cf.getOriginalFragment());
        DenoptimIO.writeVertexesToFile(new File("/tmp/cf.sdf"), FileFormat.VRTXSDF, lstVrtx, false);
        */
    }
    
//-----------------------------------------------------------------------------

    @Test
    public void testCluster2() throws Exception
    {
        double noise = 0.0001;
        List<ClusterableFragment> sample = new ArrayList<ClusterableFragment>();

        Point3d[] pointsA = new Point3d[] {
                new Point3d(0,0,0), 
                new Point3d(1,0,0)};
        for (int i=0; i<10; i++)
        {
            IAtomContainer mol = builder.newAtomContainer();
            mol.addAtom(new Atom("C", getNoisyPoint(pointsA[0])));
            Fragment frag = new Fragment(mol, BBType.UNDEFINED);
            frag.addAP(0, APClass.make("A:0"), getNoisyPoint(pointsA[1],noise));
            ClusterableFragment cf = new ClusterableFragment(frag);
            cf.setOrderOfNodes(getNatualrNodeOrder(frag));
            sample.add(cf);
        }
        
        FragmenterParameters settings = new FragmenterParameters();
        
        //TODO-gg remove
        settings.startConsoleLogger("ClustererTest");
        settings.setVerbosity(2);
        
        FragmentClusterer fc = new FragmentClusterer(sample,settings);
        fc.cluster();
        assertEquals(1,fc.getBestSet().size());
        
        for (int k=2; k<10; k++)
        {
            Point3d[] pointsB = new Point3d[] {
                    new Point3d(0,0,0), 
                    new Point3d(k,0,0)};
            for (int i=0; i<10; i++)
            {
                IAtomContainer mol = builder.newAtomContainer();
                mol.addAtom(new Atom("C", getNoisyPoint(pointsB[0])));
                Fragment frag = new Fragment(mol, BBType.UNDEFINED);
                frag.addAP(0, APClass.make("A:0"), getNoisyPoint(pointsB[1],noise));
                ClusterableFragment cf = new ClusterableFragment(frag);
                cf.setOrderOfNodes(getNatualrNodeOrder(frag));
                sample.add(cf);
            }
            
            fc = new FragmentClusterer(sample,settings);
            fc.cluster();
            assertEquals(k,fc.getBestSet().size());
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
    
    @Test
    public void testFindElbow() throws Exception
    {
        double[] x = new double[] {1,  2,    3,      4,     5,      6};
        double[] y = new double[] {18, 0.01, 0.0075, 0.0060, 0.0035, 0.0015};
        assertEquals(2, FragmentClusterer.findElbow(x,y));
        
        x = new double[] {1,  2,   3,  4,  5,      6};
        y = new double[] {20, 18, 16, 14, 10, 0.0015};
        assertEquals(6, FragmentClusterer.findElbow(x,y));
        
        x = new double[] {1,  2,   3,   4, 5, 6, 7,   8,   9,   10, 11};
        y = new double[] {12, 7, 3.5, 2.2, 2, 2, 1, 1.5, 0.75, 0.5,  0};
        assertEquals(4, FragmentClusterer.findElbow(x,y));
        
        x = new double[] {1,  2,   3,    4, 5, 6, 7,   8,   9,   10, 11};
        y = new double[] {10, 11, 3.5, 2.2, 2, 2, 1, 1.5, 0.75, 0.5,  0};
        assertEquals(4, FragmentClusterer.findElbow(x,y));
        
        x = new double[] {1,  2,   3,  4, 5, 6, 7,   8,   9,   10, 11};
        y = new double[] {11, 10, 11, 10, 2, 2, 1, 1.5, 0.75, 0.5,  0};
        assertEquals(5, FragmentClusterer.findElbow(x,y));
        
        x = new double[] {1,  2};
        y = new double[] {11, 10};
        assertEquals(2, FragmentClusterer.findElbow(x,y));
        
        x = new double[] {1};
        y = new double[] {11};
        assertEquals(1, FragmentClusterer.findElbow(x,y));
    }
    
//------------------------------------------------------------------------------
    
    /**
     * This method is needed to bypass the finding of a consistent node order 
     * via detection of the graph isomorphism.
     */
    private List<FragIsomorphNode> getNatualrNodeOrder(Fragment frag)
    {
        List<FragIsomorphNode> naturalOrder = new ArrayList<FragIsomorphNode>();
        Set<FragIsomorphNode> nodeSet = frag.getJGraphFragIsomorphism().vertexSet();
        for (IAtom atm : frag.atoms())
        {
            for (FragIsomorphNode n : nodeSet)
            {
                if (n.getOriginal() == atm)
                {
                    naturalOrder.add(n);
                    break;
                }
            }
        }
        for (AttachmentPoint ap : frag.getAttachmentPoints())
        {
            for (FragIsomorphNode n : nodeSet)
            {
                if (n.getOriginal() == ap)
                {
                    naturalOrder.add(n);
                    break;
                }
            }
        }
        return naturalOrder;
    }

//------------------------------------------------------------------------------
    
    private Point3d getNoisyPoint(Point3d p, double noiseMagnitude)
    {
        Point3d noise = rng.getNoisyPoint(noiseMagnitude);
        return new Point3d(p.x+noise.x, p.y+noise.y, p.z+noise.z);
    }
    
//------------------------------------------------------------------------------
   
    /*
     * Max noise magnitude is hard-coded
     */
    private Point3d getNoisyPoint(Point3d p)
    {
        return getNoisyPoint(p, 0.5);
    }
    
//------------------------------------------------------------------------------

}
