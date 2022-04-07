package denoptim.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.Bond;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import com.google.gson.Gson;

import denoptim.fragspace.FragmentSpace;
import denoptim.graph.DENOPTIMFragment;
import denoptim.graph.DENOPTIMFragmentTest;
import denoptim.graph.DENOPTIMTemplate;
import denoptim.graph.DENOPTIMTemplateTest;
import denoptim.graph.DENOPTIMVertex;
import denoptim.graph.DENOPTIMVertex.BBType;
import denoptim.utils.DENOPTIMMoleculeUtils;
import denoptim.utils.MutationType;

/**
 * Unit test for DENOPTIMgson
 * 
 * @author Marco Foscato
 */

public class DENOPTIMgsonTest
{
    private IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
    
//------------------------------------------------------------------------------
    
    @Test
    public void testIAtomContainerToJSONAndBack() throws Exception
    {
        IAtomContainer iac = builder.newAtomContainer();
    	IAtom a1 = new Atom("H");
    	a1.setPoint3d(new Point3d(2.624, -4.243, 6.465));
        IAtom a2 = new Atom("Ru");
        a2.setPoint3d(new Point3d(-0.001, 23.943, -0.115));
        IAtom a3 = new PseudoAtom("RCA");
        a3.setPoint3d(new Point3d(0.0, 0.0, 0.0));
    	iac.addAtom(a1);
        iac.addAtom(a2);
        iac.addAtom(a3);
        iac.addBond(new Bond(a1, a2, IBond.Order.SINGLE));
        iac.addBond(new Bond(a1, a3, IBond.Order.TRIPLE));
        iac.addBond(new Bond(a2, a3, IBond.Order.UNSET));
        
        Gson jsonWriter = DENOPTIMgson.getWriter();
        String jsonString = jsonWriter.toJson(iac);
        
        //System.out.println("JSON of iac: "+jsonString);
        
        Gson jsonReader = DENOPTIMgson.getReader();
        IAtomContainer iacFromJSON = jsonReader.fromJson(jsonString,
                IAtomContainer.class);
        
        assertEquals(iac.getAtomCount(),iacFromJSON.getAtomCount(),"#Atoms");
        assertEquals(iac.getBondCount(),iacFromJSON.getBondCount(),"#Bonds");
        for (int i=0; i<iac.getAtomCount(); i++)
        {
            IAtom atmOri = iac.getAtom(i);
            IAtom atmJsn = iacFromJSON.getAtom(i);
            assertEquals(DENOPTIMMoleculeUtils.getSymbolOrLabel(atmOri),
                    DENOPTIMMoleculeUtils.getSymbolOrLabel(atmJsn), 
                    "Symbol atom "+i);
            double distance = DENOPTIMMoleculeUtils.getPoint3d(atmOri).distance(
                    DENOPTIMMoleculeUtils.getPoint3d(atmJsn));
            assertTrue(areCloseEnough(0.0,distance), "Coordinates atom "+i);
        }
        for (int i=0; i<iac.getBondCount(); i++)
        {
            IBond bndOri = iac.getBond(i);
            IBond bndJsn = iacFromJSON.getBond(i);
            assertEquals(iac.indexOf(bndOri.getAtom(0)), 
                    iacFromJSON.indexOf(bndJsn.getAtom(0)), 
                    "1st atom in bond "+i);
            assertEquals(iac.indexOf(bndOri.getAtom(1)), 
                    iacFromJSON.indexOf(bndJsn.getAtom(1)), 
                    "2nd atom in bond "+i);
            assertEquals(bndOri.getOrder(),bndJsn.getOrder(), 
                    "Order in bond "+i);
        }
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testTemplateSerialization() throws Exception
    {
        DENOPTIMTemplate tmpl = DENOPTIMTemplateTest.getTestAmideTemplate();
        tmpl.setBuildingBlockId(-206); //just any number
        tmpl.setBuildingBlockType(BBType.CAP); //just a type easy to spot
        tmpl.removeMutationType(MutationType.CHANGEBRANCH);
        tmpl.removeMutationType(MutationType.CHANGELINK);
        
        Gson jsonWriter = DENOPTIMgson.getWriter();
        String jsonString = jsonWriter.toJson(tmpl);
        
        //System.out.println("JSON of template: "+jsonString);
        
        Gson jsonReader = DENOPTIMgson.getReader();
        DENOPTIMVertex tmplFromJSON = jsonReader.fromJson(jsonString,
                DENOPTIMVertex.class);

        assertEquals(tmpl.getNumberOfAPs(), tmplFromJSON.getNumberOfAPs(),
                "Number of APs");
        assertEquals(tmpl.getAttachmentPoints().size(),
                tmplFromJSON.getAttachmentPoints().size(),
                "Number of APs (B)");
        assertEquals(tmpl.getSymmetricAPSets().size(),
                tmplFromJSON.getSymmetricAPSets().size(),
                "Number of symmetric sets");
        assertEquals(tmpl.getVertexId(), tmplFromJSON.getVertexId(), 
                "Vertex ID");
        assertEquals(tmpl.isRCV(), tmplFromJSON.isRCV(), 
                "RCV flag");
        assertNotEquals(tmpl.hashCode(), tmplFromJSON.hashCode(), 
                "Hash code");
        assertEquals(tmpl.getBuildingBlockId(),
                tmplFromJSON.getBuildingBlockId(), 
                "Building block ID");
        assertEquals(tmpl.getBuildingBlockType(),
                tmplFromJSON.getBuildingBlockType(), 
                "Building block type");
        IAtomContainer iac = tmpl.getIAtomContainer();
        IAtomContainer iacFromJSON = tmplFromJSON.getIAtomContainer();
        for (int i=0; i<tmpl.getIAtomContainer().getAtomCount(); i++)
        {
            IAtom atmOri = iac.getAtom(i);
            IAtom atmJsn = iacFromJSON.getAtom(i);
            assertEquals(DENOPTIMMoleculeUtils.getSymbolOrLabel(atmOri),
                    DENOPTIMMoleculeUtils.getSymbolOrLabel(atmJsn), 
                    "Symbol atom "+i);
            double distance = DENOPTIMMoleculeUtils.getPoint3d(atmOri).distance(
                    DENOPTIMMoleculeUtils.getPoint3d(atmJsn));
            assertTrue(areCloseEnough(0.0,distance), "Coordinates atom "+i);
        }
        for (int i=0; i<iac.getBondCount(); i++)
        {
            IBond bndOri = iac.getBond(i);
            IBond bndJsn = iacFromJSON.getBond(i);
            assertEquals(iac.indexOf(bndOri.getAtom(0)), 
                    iacFromJSON.indexOf(bndJsn.getAtom(0)), 
                    "1st atom in bond "+i);
            assertEquals(iac.indexOf(bndOri.getAtom(1)), 
                    iacFromJSON.indexOf(bndJsn.getAtom(1)), 
                    "2nd atom in bond "+i);
            assertEquals(bndOri.getOrder(),bndJsn.getOrder(), 
                    "Order in bond "+i);
        }
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testMolecularFragmentSerialization() throws Exception
    {
        DENOPTIMFragment frag = DENOPTIMFragmentTest.makeFragment();
        frag.setBuildingBlockId(-206); //just any number
        frag.setBuildingBlockType(BBType.CAP); //just a type easy to spot
        frag.removeMutationType(MutationType.CHANGEBRANCH);
        frag.removeMutationType(MutationType.CHANGELINK);
        
        Gson jsonWriter = DENOPTIMgson.getWriter();
        String jsonString = jsonWriter.toJson(frag);
        
        //System.out.println("JSON of DENOPTIMFragment: "+jsonString);
        
        Gson jsonReader = DENOPTIMgson.getReader();
        DENOPTIMVertex fragFromJSON = jsonReader.fromJson(jsonString,
                DENOPTIMVertex.class);
        
        assertEquals(frag.getNumberOfAPs(), fragFromJSON.getNumberOfAPs(),
                "Number of APs");
        assertEquals(frag.getAPCountOnAtom(0),
                ((DENOPTIMFragment) fragFromJSON).getAPCountOnAtom(0),
                "Size APs on atm0");
        assertEquals(frag.getAPCountOnAtom(2),
                ((DENOPTIMFragment) fragFromJSON).getAPCountOnAtom(2),
                "Size APs on atm2");
        assertEquals(frag.getAttachmentPoints().size(),
                fragFromJSON.getAttachmentPoints().size(),
                "Number of APs (B)");
        assertEquals(frag.getSymmetricAPSets().size(),
                fragFromJSON.getSymmetricAPSets().size(),
                "Number of symmetric sets");
        assertEquals(frag.getSymmetricAPSets().get(0).size(),
                fragFromJSON.getSymmetricAPSets().get(0).size(), 
                "Number of symmetric APs in set");
        assertEquals(frag.getVertexId(), fragFromJSON.getVertexId(), 
                "Vertex ID");
        assertEquals(frag.isRCV(), fragFromJSON.isRCV(), 
                "RCV flag");
        assertNotEquals(frag.hashCode(), fragFromJSON.hashCode(), 
                "Hash code");
        assertEquals(frag.getBuildingBlockId(),
                fragFromJSON.getBuildingBlockId(), 
                "Building block ID");
        assertEquals(frag.getBuildingBlockType(),
                fragFromJSON.getBuildingBlockType(), 
                "Building block type");
        IAtomContainer iac = frag.getIAtomContainer();
        IAtomContainer iacFromJSON = fragFromJSON.getIAtomContainer();
        for (int i=0; i<frag.getIAtomContainer().getAtomCount(); i++)
        {
            IAtom atmOri = iac.getAtom(i);
            IAtom atmJsn = iacFromJSON.getAtom(i);
            assertEquals(DENOPTIMMoleculeUtils.getSymbolOrLabel(atmOri),
                    DENOPTIMMoleculeUtils.getSymbolOrLabel(atmJsn), 
                    "Symbol atom "+i);
            double distance = DENOPTIMMoleculeUtils.getPoint3d(atmOri).distance(
                    DENOPTIMMoleculeUtils.getPoint3d(atmJsn));
            assertTrue(areCloseEnough(0.0,distance), "Coordinates atom "+i);
        }
        for (int i=0; i<iac.getBondCount(); i++)
        {
            IBond bndOri = iac.getBond(i);
            IBond bndJsn = iacFromJSON.getBond(i);
            assertEquals(iac.indexOf(bndOri.getAtom(0)), 
                    iacFromJSON.indexOf(bndJsn.getAtom(0)), 
                    "1st atom in bond "+i);
            assertEquals(iac.indexOf(bndOri.getAtom(1)), 
                    iacFromJSON.indexOf(bndJsn.getAtom(1)), 
                    "2nd atom in bond "+i);
            assertEquals(bndOri.getOrder(),bndJsn.getOrder(), 
                    "Order in bond "+i);
        }
    }
    
//------------------------------------------------------------------------------

    private boolean areCloseEnough(double a, double b)
    {
    	double delta = 0.0000001; //NB: hard-coded threshold
    	return Math.abs(a-b) <= delta;
    }
    
//------------------------------------------------------------------------------

}
