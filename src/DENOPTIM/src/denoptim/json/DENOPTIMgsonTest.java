package denoptim.json;

/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no>
 *   and Marco Foscato <marco.foscato@uib.no>
 * 
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import static denoptim.utils.DENOPTIMMoleculeUtils.getPoint3d;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.vecmath.Point2d;
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

import denoptim.graph.DENOPTIMFragment;
import denoptim.graph.DENOPTIMFragmentTest;
import denoptim.graph.DENOPTIMGraph;
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
        assertEquals(frag.getNumberOfAPs(), fragFromJSON.getNumberOfAPs(), 
                "Number of APS");
        assertEquals(frag.getSymmetricAPSets().size(), 
                fragFromJSON.getSymmetricAPSets().size(), 
                "Number of SymAPs sets");
        assertEquals(frag.isRCV(), fragFromJSON.isRCV(), 
                "RCV flag");
        assertNotEquals(frag.hashCode(), fragFromJSON.hashCode(), 
                "Hash code");
        assertEquals(frag.getBuildingBlockId(),
                ((DENOPTIMFragment)fragFromJSON).getBuildingBlockId(), 
                "Building block ID");
        assertEquals(frag.getBuildingBlockType(),
                ((DENOPTIMFragment)fragFromJSON).getBuildingBlockType(), 
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
