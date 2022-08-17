package denoptim.fragmenter;

import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import denoptim.graph.APClass;
import denoptim.graph.Fragment;
import denoptim.graph.Vertex.BBType;

/**
 * Unit test for fragmenter's tools.
 */
public class FragmentAlignementTest
{

    /**
     * Private builder of atom containers
     */
    private IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
    
//-----------------------------------------------------------------------------

    @Test
    public void testGetMinimumRMSD() throws Exception
    {
        IAtomContainer mol1 = builder.newAtomContainer();
        mol1.addAtom(new Atom("C", new Point3d(0,0,0)));
        mol1.addAtom(new Atom("H", new Point3d(1,1,1)));
        mol1.addAtom(new Atom("O", new Point3d(3,1,1)));
        Fragment frag1 = new Fragment(mol1, BBType.UNDEFINED);
        frag1.addAP(0, APClass.make("dummy:0"), new Point3d(3,0,0));
        frag1.addAP(1, APClass.make("dummy:0"), new Point3d(3,2,0));

        Fragment frag1clone = frag1.clone();
        
        FragmentAlignement fa = new FragmentAlignement(frag1, frag1clone);
        assertTrue(0.0001>fa.getMinimumRMSD());

        IAtomContainer mol2 = builder.newAtomContainer();
        mol2.addAtom(new Atom("O", new Point3d(3,1,1)));
        mol2.addAtom(new Atom("H", new Point3d(1,1,1)));
        mol2.addAtom(new Atom("C", new Point3d(0,0,0)));
        Fragment frag2 = new Fragment(mol2, BBType.UNDEFINED);
        frag2.addAP(1, APClass.make("dummy:0"), new Point3d(3,2,0));
        frag2.addAP(2, APClass.make("dummy:0"), new Point3d(3,0,0));
        
        fa = new FragmentAlignement(frag1, frag2);
        assertTrue(0.0001>fa.getMinimumRMSD());
        
        IAtomContainer mol3 = builder.newAtomContainer();
        mol3.addAtom(new Atom("O", new Point3d(1,3,1)));
        mol3.addAtom(new Atom("H", new Point3d(1,1,1)));
        mol3.addAtom(new Atom("C", new Point3d(0,0,0)));
        Fragment frag3 = new Fragment(mol3, BBType.UNDEFINED);
        frag3.addAP(1, APClass.make("dummy:0"), new Point3d(0,3,2));
        frag3.addAP(2, APClass.make("dummy:0"), new Point3d(0,3,0));
        
        fa = new FragmentAlignement(frag1, frag3);
        assertTrue(0.0001>fa.getMinimumRMSD());
        
        IAtomContainer mol4 = builder.newAtomContainer();
        mol4.addAtom(new Atom("O", new Point3d(3,1,1)));
        mol4.addAtom(new Atom("H", new Point3d(1,1,1)));
        mol4.addAtom(new Atom("C", new Point3d(1,0,0))); // difference here
        Fragment frag4 = new Fragment(mol4, BBType.UNDEFINED);
        frag4.addAP(1, APClass.make("dummy:0"), new Point3d(3,2,0));
        frag4.addAP(2, APClass.make("dummy:0"), new Point3d(3,0,0));

        fa = new FragmentAlignement(frag1, frag4);
        assertTrue(0.1<fa.getMinimumRMSD());

        IAtomContainer mol5 = builder.newAtomContainer();
        mol5.addAtom(new Atom("O", new Point3d(3,1,1)));
        mol5.addAtom(new Atom("H", new Point3d(1,1,1)));
        mol5.addAtom(new Atom("C", new Point3d(0,0,0)));
        Fragment frag5 = new Fragment(mol5, BBType.UNDEFINED);
        frag5.addAP(1, APClass.make("dummy:0"), new Point3d(3,2,0));
        frag5.addAP(2, APClass.make("dummy:0"), new Point3d(3,3,0)); // difference here

        fa = new FragmentAlignement(frag1, frag5);
        assertTrue(0.1<fa.getMinimumRMSD());
    }
   
//-----------------------------------------------------------------------------

}
