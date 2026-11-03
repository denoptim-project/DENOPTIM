package denoptim.fragmenter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.graph.APClass;
import denoptim.graph.Fragment;
import denoptim.graph.Vertex.BBType;

/**
 * Unit test for {@link TopoTemplateProducer}.
 */
public class TopoTemplateProducerTest
{
    private final IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();

//------------------------------------------------------------------------------

    @Test
    public void testHDepletedTemplateKeepsContainerAtomOrder() throws DENOPTIMException
    {
        IAtomContainer mol = builder.newAtomContainer();
        mol.addAtom(new Atom("C"));        // 0
        mol.addAtom(new Atom("H"));        // 1 excluded: hydrogen, no AP
        mol.addAtom(new Atom("O"));        // 2
        mol.addAtom(new PseudoAtom("Du")); // 3 excluded: dummy, no AP
        mol.addAtom(new Atom("H"));        // 4 kept: hydrogen that carries an AP
        mol.addAtom(new Atom("N"));        // 5
        mol.addAtom(new PseudoAtom("Du")); // 6 kept: dummy that carries an AP
        mol.addAtom(new Atom("C"));        // 7
        mol.addBond(0, 1, IBond.Order.SINGLE);
        mol.addBond(0, 2, IBond.Order.SINGLE);
        mol.addBond(2, 3, IBond.Order.SINGLE);
        mol.addBond(2, 4, IBond.Order.SINGLE);
        mol.addBond(4, 5, IBond.Order.SINGLE);
        mol.addBond(5, 6, IBond.Order.SINGLE);
        mol.addBond(6, 7, IBond.Order.SINGLE);
        Fragment fragment = new Fragment(mol, BBType.FRAGMENT);
        fragment.addAP(4, APClass.make("ap", 0), new Point3d(0.0, 0.0, 0.0));
        fragment.addAP(6, APClass.make("ap", 0), new Point3d(0.0, 0.0, 0.0));
        fragment.projectAPsToProperties();

        TopoTemplateProducer producer = new TopoTemplateProducer(fragment.getIAtomContainer());

        // The copied dummy keeps CDK's pseudo-atom symbol "R".
        assertEquals(List.of("C", "O", "H", "N", "R", "C"),
                symbolsOf(producer.getTemplateWithBufferShell(0)));
        assertEquals(List.of(0, 2, 4, 5, 6, 7),
                originalIndicesOf(producer.getTemplateWithBufferShell(0)));

        // The hydrogen on atom 0 enters at shell 1. The dummy on atom 2 does not.
        assertEquals(List.of("C", "H", "O", "H", "N", "R", "C"),
                symbolsOf(producer.getTemplateWithBufferShell(1)));
        assertEquals(List.of(0, 1, 2, 4, 5, 6, 7),
                originalIndicesOf(producer.getTemplateWithBufferShell(1)));
    }

//------------------------------------------------------------------------------

    @Test
    public void testBoundaryTemplateGrowsInContainerAtomOrder()
    {
        // Two vertexes joined by the bond between atoms 1 and 3.
        // That bond alone also matches C0-C1, so the critical set grows by the
        // next non-H shell (C0 and O4) until one atom set matches.
        // Atom order is the container order, not the order of a shell expansion.
        IAtomContainer mol = builder.newAtomContainer();
        mol.addAtom(new Atom("C")); // 0 vertex A
        mol.addAtom(new Atom("C")); // 1 vertex A, boundary
        mol.addAtom(new Atom("H")); // 2 vertex A
        mol.addAtom(new Atom("C")); // 3 vertex B, boundary
        mol.addAtom(new Atom("O")); // 4 vertex B
        mol.addAtom(new Atom("H")); // 5 vertex B
        mol.getAtom(0).setProperty(DENOPTIMConstants.ATMPROPVERTEXPATH, "A");
        mol.getAtom(1).setProperty(DENOPTIMConstants.ATMPROPVERTEXPATH, "A");
        mol.getAtom(2).setProperty(DENOPTIMConstants.ATMPROPVERTEXPATH, "A");
        mol.getAtom(3).setProperty(DENOPTIMConstants.ATMPROPVERTEXPATH, "B");
        mol.getAtom(4).setProperty(DENOPTIMConstants.ATMPROPVERTEXPATH, "B");
        mol.getAtom(5).setProperty(DENOPTIMConstants.ATMPROPVERTEXPATH, "B");
        mol.addBond(0, 1, IBond.Order.SINGLE);
        mol.addBond(1, 2, IBond.Order.SINGLE);
        mol.addBond(1, 3, IBond.Order.SINGLE);
        mol.addBond(3, 4, IBond.Order.SINGLE);
        mol.addBond(4, 5, IBond.Order.SINGLE);

        TopoTemplateProducer producer = new TopoTemplateProducer(mol);

        assertEquals(List.of("C", "C", "C", "O"),
                symbolsOf(producer.getTemplateWithBufferShell(0)));
        assertEquals(List.of(0, 1, 3, 4),
                originalIndicesOf(producer.getTemplateWithBufferShell(0)));

        assertEquals(List.of("C", "C", "H", "C", "O", "H"),
                symbolsOf(producer.getTemplateWithBufferShell(1)));
        assertEquals(List.of(0, 1, 2, 3, 4, 5),
                originalIndicesOf(producer.getTemplateWithBufferShell(1)));

        assertEquals(List.of("C", "C", "H", "C", "O", "H"),
                symbolsOf(producer.getTemplateWithBufferShell(2)));
        assertEquals(List.of(0, 1, 2, 3, 4, 5),
                originalIndicesOf(producer.getTemplateWithBufferShell(2)));
    }

//------------------------------------------------------------------------------

    @Test
    public void testUniqueBoundaryPairIsNotExtended()
    {
        // O-N occurs once, so the boundary pair is already one matched atom set.
        IAtomContainer mol = builder.newAtomContainer();
        mol.addAtom(new Atom("C")); // 0 vertex A
        mol.addAtom(new Atom("O")); // 1 vertex A, boundary
        mol.addAtom(new Atom("N")); // 2 vertex B, boundary
        mol.getAtom(0).setProperty(DENOPTIMConstants.ATMPROPVERTEXPATH, "A");
        mol.getAtom(1).setProperty(DENOPTIMConstants.ATMPROPVERTEXPATH, "A");
        mol.getAtom(2).setProperty(DENOPTIMConstants.ATMPROPVERTEXPATH, "B");
        mol.addBond(0, 1, IBond.Order.SINGLE);
        mol.addBond(1, 2, IBond.Order.SINGLE);

        TopoTemplateProducer producer = new TopoTemplateProducer(mol);

        assertEquals(List.of("O", "N"),
                symbolsOf(producer.getTemplateWithBufferShell(0)));
        assertEquals(List.of(1, 2),
                originalIndicesOf(producer.getTemplateWithBufferShell(0)));
    }

//------------------------------------------------------------------------------

    @Test
    public void testRepeatedSubgraphExtendsPastTheBoundary()
    {
        // The boundary bond is C-N, and the same bond occurs again inside one
        // vertex. Those are two matched atom sets, so the critical set grows
        // to the whole non-H skeleton.
        IAtomContainer mol = builder.newAtomContainer();
        mol.addAtom(new Atom("C")); // 0 vertex A, boundary
        mol.addAtom(new Atom("N")); // 1 vertex B, boundary
        mol.addAtom(new Atom("C")); // 2 vertex C
        mol.addAtom(new Atom("N")); // 3 vertex C
        mol.getAtom(0).setProperty(DENOPTIMConstants.ATMPROPVERTEXPATH, "A");
        mol.getAtom(1).setProperty(DENOPTIMConstants.ATMPROPVERTEXPATH, "B");
        mol.getAtom(2).setProperty(DENOPTIMConstants.ATMPROPVERTEXPATH, "C");
        mol.getAtom(3).setProperty(DENOPTIMConstants.ATMPROPVERTEXPATH, "C");
        mol.addBond(0, 1, IBond.Order.SINGLE);
        mol.addBond(2, 3, IBond.Order.SINGLE);

        TopoTemplateProducer producer = new TopoTemplateProducer(mol);

        assertEquals(List.of("C", "N", "C", "N"),
                symbolsOf(producer.getTemplateWithBufferShell(0)));
        assertEquals(List.of(0, 1, 2, 3),
                originalIndicesOf(producer.getTemplateWithBufferShell(0)));
    }

//------------------------------------------------------------------------------

    private static List<String> symbolsOf(IAtomContainer template)
    {
        List<String> symbols = new ArrayList<>();
        for (IAtom atom : template.atoms())
        {
            symbols.add(atom.getSymbol());
        }
        return symbols;
    }

//------------------------------------------------------------------------------

    private static List<Integer> originalIndicesOf(IAtomContainer template)
    {
        List<Integer> indices = new ArrayList<>();
        for (IAtom atom : template.atoms())
        {
            indices.add((Integer) atom.getProperty(
                    TopoTemplateProducer.ATMPROPORIGINALINDEX));
        }
        return indices;
    }

//------------------------------------------------------------------------------
}
