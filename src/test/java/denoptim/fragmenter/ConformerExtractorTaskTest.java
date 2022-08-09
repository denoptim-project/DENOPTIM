package denoptim.fragmenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.logging.Logger;

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
import denoptim.graph.APClass;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.FragIsomorphNode;
import denoptim.graph.Fragment;
import denoptim.graph.Vertex.BBType;
import denoptim.programs.fragmenter.CuttingRule;
import denoptim.programs.fragmenter.FragmenterParameters;
import denoptim.programs.fragmenter.MatchedBond;
import denoptim.utils.ManySMARTSQuery;
import denoptim.utils.MoleculeUtils;

/**
 * Unit test for fragmenter's tools.
 */
public class ConformerExtractorTaskTest
{

    /**
     * Private builder of atom containers
     */
    private IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
    
//-----------------------------------------------------------------------------

    @Test
    public void testExtractClusterableFragments() throws Exception
    {
        String isoFamId = "IdOfThisFamily";
        
        IAtomContainer mol1 = builder.newAtomContainer();
        mol1.addAtom(new Atom("C", new Point3d(0,0,0)));
        mol1.addAtom(new Atom("H", new Point3d(1,1,1)));
        mol1.addAtom(new Atom("O", new Point3d(3,1,1)));
        Fragment frag1 = new Fragment(mol1, BBType.UNDEFINED);
        frag1.addAP(0, APClass.make("A:0"), new Point3d(3,0,0));
        frag1.addAP(1, APClass.make("B:0"), new Point3d(3,2,0));
        frag1.setProperty(DENOPTIMConstants.ISOMORPHICFAMILYID, isoFamId);

        Fragment frag1clone = frag1.clone();
        frag1clone.setProperty(DENOPTIMConstants.ISOMORPHICFAMILYID, isoFamId);

        IAtomContainer mol2 = builder.newAtomContainer();
        mol2.addAtom(new Atom("O", new Point3d(3,1,1)));
        mol2.addAtom(new Atom("H", new Point3d(1,1,1)));
        mol2.addAtom(new Atom("C", new Point3d(0,0,0)));
        Fragment frag2 = new Fragment(mol2, BBType.UNDEFINED);
        frag2.addAP(1, APClass.make("B:0"), new Point3d(3,2,0));
        frag2.addAP(2, APClass.make("A:0"), new Point3d(3,0,0));
        frag2.setProperty(DENOPTIMConstants.ISOMORPHICFAMILYID, isoFamId);
        
        IAtomContainer mol3 = builder.newAtomContainer();
        mol3.addAtom(new Atom("O", new Point3d(1,3,1)));
        mol3.addAtom(new Atom("H", new Point3d(1,1,1)));
        mol3.addAtom(new Atom("C", new Point3d(0,0,0)));
        Fragment frag3 = new Fragment(mol3, BBType.UNDEFINED);
        frag3.addAP(1, APClass.make("B:0"), new Point3d(0,3,2));
        frag3.addAP(2, APClass.make("A:0"), new Point3d(0,3,0));
        frag3.setProperty(DENOPTIMConstants.ISOMORPHICFAMILYID, isoFamId);
        
        IAtomContainer mol4 = builder.newAtomContainer();
        mol4.addAtom(new Atom("C", new Point3d(0,0,0)));
        Fragment frag4 = new Fragment(mol3, BBType.UNDEFINED);
        frag4.addAP(0, APClass.make("dummy:0"), new Point3d(0,3,2));
        frag4.setProperty(DENOPTIMConstants.ISOMORPHICFAMILYID, "other");
        
        IAtomContainer mol5 = builder.newAtomContainer();
        mol5.addAtom(new Atom("O", new Point3d(1,3,1)));
        mol5.addAtom(new Atom("H", new Point3d(1,1,1)));
        mol5.addAtom(new Atom("C", new Point3d(0,0,0)));
        Fragment frag5 = new Fragment(mol3, BBType.UNDEFINED);
        frag5.addAP(1, APClass.make("B:0"), new Point3d(0,3,2));
        frag5.addAP(2, APClass.make("A:0"), new Point3d(0,3,0));
        frag5.setProperty(DENOPTIMConstants.ISOMORPHICFAMILYID, "barfoo");
        
        List<IAtomContainer> lst = new ArrayList<IAtomContainer>();
        lst.add(frag1.getIAtomContainer());
        lst.add(frag1clone.getIAtomContainer());
        lst.add(frag4.getIAtomContainer());
        lst.add(frag2.getIAtomContainer());
        lst.add(frag3.getIAtomContainer());
        lst.add(frag5.getIAtomContainer());
        
        List<ClusterableFragment> sample = 
                ConformerExtractorTask.extractClusterableFragments(
                        lst.iterator(), isoFamId, Logger.getLogger("DummyLog"));
        
        // Not all are extracted: two have different isomorphic family ID.
        assertEquals(4,sample.size());
        
        // Order is given by the first fragment
        int j=0;
        for (IAtom atm : frag1.atoms())
        {
            assertEquals(atm.getSymbol(),
                    sample.get(0).getOrderedNodes().get(j).getLabel());
            j++;
        }
        for (AttachmentPoint ap : frag1.getAttachmentPoints())
        {
            assertEquals(ap.getAPClass().toString(),
                    sample.get(0).getOrderedNodes().get(j).getLabel());
            j++;
        }
        
        // All the others should have the same order of atoms/APs
        for (ClusterableFragment cf : sample)
        {
            for (int i=0; i<cf.getOrderedNodes().size(); i++)
            {
                FragIsomorphNode n = cf.getOrderedNodes().get(i);
                assertEquals(sample.get(0).getOrderedNodes().get(i).getLabel(),
                        cf.getOrderedNodes().get(i).getLabel());
            }
        }
    }
   
//-----------------------------------------------------------------------------

}
