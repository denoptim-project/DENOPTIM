package denoptim.fragspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/*
 *   DENOPTIM
 *   Copyright (C) 2019 Marco Foscato <marco.foscato@uib.no>
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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openscience.cdk.Atom;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.Bond;

import denoptim.exception.DENOPTIMException;
import denoptim.files.FileUtils;
import denoptim.graph.APClass;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.Edge;
import denoptim.graph.Edge.BondType;
import denoptim.graph.Fragment;
import denoptim.graph.DGraph;
import denoptim.graph.Ring;
import denoptim.graph.Template;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;
import denoptim.graph.SymmetricSet;
import denoptim.io.DenoptimIO;
import denoptim.molecularmodeling.ThreeDimTreeBuilder;
import denoptim.utils.MoleculeUtils;
import denoptim.utils.GraphUtils;

/**
 * Unit test for fragment space
 * 
 * @author Marco Foscato
 */

public class FragmentSpaceTest
{
    private static final String SEP = System.getProperty("file.separator");

    @TempDir 
    static File tempDir;

    private final Random rng = new Random();
    
	private static final int APSUBRULE = 0;

    private static final String RULAPCS = "apc-S";
    private static final String RULAPC1 = "apc-1";
    private static final String RULAPC2 = "apc-2";
    private static final String RULAPC3 = "apc-3";
    private static final String RULAPC4 = "apc-4";
    private static final String RULAPC5 = "apc-5";
    private static final String RULAPC6 = "apc-6";
    private static final String RULAPCC1 = "cap-1";
    private static final String RULAPCC2 = "cap-2";
    
    private static final BBType BBTFRAG = BBType.FRAGMENT;
    
	private static APClass APCS;
	private static APClass APC1;
	private static APClass APC2;
	private static APClass APC3;
    private static APClass APC4;
    private static APClass APC5;
    private static APClass APC6;
	private static APClass APCC1;
	private static APClass APCC2;

//------------------------------------------------------------------------------
	
	private FragmentSpaceParameters buildFragmentSpace() throws DENOPTIMException
	{
	    assertTrue(tempDir.isDirectory(),"Should be a directory ");
	    
        try
        {
            APCS = APClass.make(RULAPCS, APSUBRULE, BondType.SINGLE);
            APC1 = APClass.make(RULAPC1, APSUBRULE, BondType.SINGLE);
            APC2 = APClass.make(RULAPC2, APSUBRULE, BondType.SINGLE);
            APC3 = APClass.make(RULAPC3, APSUBRULE, BondType.SINGLE);
            APC4 = APClass.make(RULAPC4, APSUBRULE, BondType.SINGLE);
            APC5 = APClass.make(RULAPC5, APSUBRULE, BondType.SINGLE);
            APC6 = APClass.make(RULAPC6, APSUBRULE, BondType.DOUBLE);
            APCC1 = APClass.make(RULAPCC1, APSUBRULE, BondType.SINGLE);
            APCC2 = APClass.make(RULAPCC2, APSUBRULE, BondType.SINGLE);
        } catch (DENOPTIMException e)
        {
            //This will not happen
        }

        String rootName = tempDir.getAbsolutePath() + SEP;

    	ArrayList<Vertex> fragLib = new ArrayList<Vertex>();
    	Fragment frg1 = new Fragment();
    	Atom a1 = new Atom("C", new Point3d(new double[]{0.0, 1.1, 2.2}));
    	Atom a2 = new Atom("C", new Point3d(new double[]{1.0, 1.1, 2.2}));
    	Atom a3 = new Atom("C", new Point3d(new double[]{2.0, 1.1, 2.2}));
    	frg1.addAtom(a1);
    	frg1.addAtom(a2);
    	frg1.addAtom(a3);
    	frg1.addBond(new Bond(a1, a2));
    	frg1.addBond(new Bond(a2, a3));
    	frg1.addAPOnAtom(a3, APC1, new Point3d(new double[]{0.0, 2.2, 3.3}));
    	frg1.addAPOnAtom(a3, APC1, new Point3d(new double[]{0.0, 0.0, 3.3}));
    	frg1.addAPOnAtom(a3, APC2, new Point3d(new double[]{0.0, 0.0, 1.1}));
    	frg1.addAPOnAtom(a1, APC3, new Point3d(new double[]{3.0, 0.0, 3.3}));
    	frg1.projectAPsToProperties();
    	fragLib.add(frg1);
    	
        Fragment frg2 = new Fragment();
        Atom a21 = new Atom("N", new Point3d(new double[]{0.0, 1.1, 2.2}));
        Atom a22 = new Atom("H", new Point3d(new double[]{1.0, 1.1, 2.2}));
        frg2.addAtom(a21);
        frg2.addAtom(a22);
        frg2.addBond(new Bond(a21, a22));
        frg2.addAPOnAtom(a22, APC2, new Point3d(new double[]{0.0, 2.2, 3.3}));
        frg2.addAPOnAtom(a22, APC2, new Point3d(new double[]{0.0, 0.0, 3.3}));
        frg2.projectAPsToProperties();
        fragLib.add(frg2);
        
        Fragment frg3 = new Fragment();
        Atom a31 = new Atom("P", new Point3d(new double[]{0.0, 1.1, 2.2}));
        frg3.addAtom(a31);
        frg3.addAPOnAtom(a31, APC1, new Point3d(new double[]{0.0, 2.2, 3.3}));
        frg3.addAPOnAtom(a31, APC2, new Point3d(new double[]{0.0, 0.0, 3.3}));
        frg3.addAPOnAtom(a31, APC3, new Point3d(new double[]{0.0, 0.0, 1.1}));
        frg3.projectAPsToProperties();
        fragLib.add(frg3);
        
        Fragment frg8 = new Fragment();
        Atom a81 = new Atom("C", new Point3d(new double[]{0.0, 1.1, -2.2}));
        Atom a82 = new Atom("C", new Point3d(new double[]{1.0, 1.1, -2.2}));
        Atom a83 = new Atom("C", new Point3d(new double[]{2.0, 1.1, -2.2}));
        frg8.addAtom(a81);
        frg8.addAtom(a82);
        frg8.addAtom(a83);
        frg8.addBond(new Bond(a81, a82));
        frg8.addBond(new Bond(a82, a83));
        frg8.addAPOnAtom(a83, APC4, new Point3d(new double[]{0.0, 2.2, -3.3}));
        frg8.addAPOnAtom(a83, APC4, new Point3d(new double[]{0.0, 0.0, -3.3}));
        frg8.addAPOnAtom(a83, APC6, new Point3d(new double[]{0.0, 0.0, -1.1}));
        frg8.addAPOnAtom(a82, APC5, new Point3d(new double[]{1.0, 0.1, -2.2}));
        frg8.addAPOnAtom(a82, APC5, new Point3d(new double[]{1.0, 0.1, -1.2}));
        frg8.addAPOnAtom(a82, APC5, new Point3d(new double[]{1.0, 2.1, -2.2}));
        frg8.addAPOnAtom(a81, APC5, new Point3d(new double[]{3.0, 0.0, -3.3}));
        frg8.projectAPsToProperties();
        fragLib.add(frg8);
        
        String fragLibFile = rootName + "frags.sdf";
        DenoptimIO.writeVertexesToSDF(new File(fragLibFile), fragLib, false);
        
    	ArrayList<Vertex> scaffLib = new ArrayList<Vertex>();
        Fragment scaf0 = new Fragment();
        Atom a41 = new Atom("O", new Point3d(new double[]{0.0, 1.1, 2.2}));
        Atom a42 = new Atom("C", new Point3d(new double[]{1.0, 1.1, 2.2}));
        Atom a43 = new Atom("Ru", new Point3d(new double[]{2.0, 1.1, 2.2}));
        scaf0.addAtom(a41);
        scaf0.addAtom(a42);
        scaf0.addAtom(a43);
        scaf0.addBond(new Bond(a41, a42));
        scaf0.addBond(new Bond(a42, a43));
        scaf0.addAPOnAtom(a43, APCS, new Point3d(new double[]{0.0, 2.2, 3.3}));
        scaf0.addAPOnAtom(a43, APCS, new Point3d(new double[]{0.0, 0.0, 3.3}));
        scaf0.addAPOnAtom(a43, APCS, new Point3d(new double[]{0.0, 0.0, 1.1}));
        scaf0.addAPOnAtom(a41, APCS, new Point3d(new double[]{3.0, 0.0, 3.3}));
        scaf0.projectAPsToProperties();
        scaffLib.add(scaf0);
        
        Fragment scaf1 = new Fragment();
        Atom a51 = new Atom("Zn", new Point3d(new double[]{5.0, 1.1, 2.2}));
        scaf1.addAtom(a51);
        scaf1.addAPOnAtom(a51, APCS, new Point3d(new double[]{5.0, 2.2, 3.3}));
        scaf1.addAPOnAtom(a51, APCS, new Point3d(new double[]{5.0, 0.0, 3.3}));
        scaf1.addAPOnAtom(a51, APCS, new Point3d(new double[]{5.0, 0.0, 1.1}));
        scaf1.projectAPsToProperties();
        scaffLib.add(scaf1);
        
        String scaffLibFile = rootName + "scaff.sdf";
        DenoptimIO.writeVertexesToSDF(new File(scaffLibFile), scaffLib, false);
        
        ArrayList<Vertex> cappLib = new ArrayList<Vertex>();
        Fragment cap1 = new Fragment();
        Atom a61 = new Atom("H", new Point3d(new double[]{10.0, 1.1, 2.2}));
        cap1.addAtom(a61);
        cap1.addAPOnAtom(a61, APCC1, new Point3d(new double[]{13.0, 0.0, 3.3}));
        cap1.projectAPsToProperties();
        cappLib.add(cap1);
        
        Fragment cap2 = new Fragment();
        Atom a71 = new Atom("Cl", new Point3d(new double[]{10.0, 1.1, 2.2}));
        cap2.addAtom(a71);
        cap2.addAPOnAtom(a71, APCC2, new Point3d(new double[]{13.0, 0.0, 3.3}));
        cap2.projectAPsToProperties();
        cappLib.add(cap2);

        String capLibFile = rootName + "caps.sdf";
        DenoptimIO.writeVertexesToSDF(new File(capLibFile), cappLib, false);
        
    	HashMap<APClass,ArrayList<APClass>> cpMap = 
    	        new HashMap<APClass,ArrayList<APClass>>();
    	ArrayList<APClass> lst1 = new ArrayList<APClass>();
    	lst1.add(APC1);
    	lst1.add(APC2);
    	cpMap.put(APCS, lst1);
    	ArrayList<APClass> lst2 = new ArrayList<APClass>();
    	lst2.add(APC2);
    	cpMap.put(APC1, lst2);
    	ArrayList<APClass> lst3 = new ArrayList<APClass>();
    	lst3.add(APC2);
    	lst3.add(APC3);
    	cpMap.put(APC2, lst3);
    	
    	HashMap<APClass,APClass> capMap = new HashMap<APClass,APClass>();
    	capMap.put(APCS, APCC2);
    	capMap.put(APC1, APCC1);
    	capMap.put(APC2, APCC1);

    	HashSet<APClass> ends = new HashSet<APClass>();
    	ends.add(APC3);
    	
    	String cpmFile = rootName + "cpm.dat";
    	DenoptimIO.writeCompatibilityMatrix(cpmFile, cpMap, capMap,ends);

    	/*
    	//Just in case one day we want to have also an RC-CPMap
    	HashMap<APClass,ArrayList<APClass>> rcCpMap = 
    			new HashMap<APClass,ArrayList<APClass>>();
    	*/
    	
    	FragmentSpaceParameters fsp = new FragmentSpaceParameters();
    	FragmentSpace fs = new FragmentSpace(fsp, scaffLibFile, fragLibFile, 
    	        capLibFile, cpmFile);
    	return fsp;
	}
	
//-----------------------------------------------------------------------------        
    
    @Test
    public void testSymmetry() throws Exception
    {
        FragmentSpaceParameters fsp = buildFragmentSpace();
        assertTrue(fsp.getFragmentSpace().isDefined(),"FragmentSpace is defined");
        Vertex v = Vertex.newVertexFromLibrary(
                GraphUtils.getUniqueVertexIndex(),3,BBType.FRAGMENT,
                fsp.getFragmentSpace());
        
        assertEquals(2,v.getSymmetricAPSets().size(),
                "Number of symmetric sets of APs");
        
        Map<APClass,Integer> expectedCount = new HashMap<APClass,Integer>();
        expectedCount.put(APC4, 2);
        expectedCount.put(APC5, 3);
        for (SymmetricSet ss : v.getSymmetricAPSets())
        {
            APClass apc = v.getAP(ss.get(0)).getAPClass();
            assertEquals(expectedCount.get(apc),ss.size(), 
                    "Number of APs in symmetric set for APClass "+apc);
        }
    }
	
//-----------------------------------------------------------------------------    	
	
    @Test
    public void testGetFragsWithAPClass() throws Exception
    {
        FragmentSpaceParameters fsp = buildFragmentSpace();
        FragmentSpace fs = fsp.getFragmentSpace();
        assertTrue(fs.isDefined(),"FragmentSpace is defined");
    	ArrayList<IdFragmentAndAP> l = fs.getFragsWithAPClass(APC2);
    	assertEquals(4,l.size(),"Wrong size of AP IDs with given APClass.");

    	int i = -1;
    	for (IdFragmentAndAP id : l)
    	{
    	    i++;
    	    Vertex v = Vertex.newVertexFromLibrary(-1,
    	            id.getVertexMolId(), id.getVertexMolType(), fs);
    	    assertEquals(APC2, v.getAP(id.getApId()).getAPClass(),
    	            "APClass of "+i);
    	}
    }

//-----------------------------------------------------------------------------    	
    	
    @Test
    public void testGetFragAPsCompatibleWithClass() throws Exception
    {

        FragmentSpaceParameters fsp = buildFragmentSpace();
        FragmentSpace fs = fsp.getFragmentSpace();
        assertTrue(fs.isDefined(),"FragmentSpace is defined");
    	ArrayList<IdFragmentAndAP> lst = fs.getFragAPsCompatibleWithClass(APC1);
    	
    	assertEquals(4,lst.size(),"Size of compatible APs list is wrong.");

    	int i = -1;
        for (IdFragmentAndAP id : lst)
        {
            i++;
            Vertex v = Vertex.newVertexFromLibrary(-1,
                    id.getVertexMolId(), id.getVertexMolType(), fs);
            AttachmentPoint ap = v.getAP(id.getApId());
            assertTrue(APC1.isCPMapCompatibleWith(ap.getAPClass(), fs), 
                    "Incompatible choice at "+i);
        }
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetFragAPsCompatibleWithTheseAPs() throws Exception
    {
        FragmentSpaceParameters fsp = buildFragmentSpace();
        FragmentSpace fs = fsp.getFragmentSpace();
        assertTrue(fs.isDefined(),"FragmentSpace is defined");
    	IdFragmentAndAP src1 = new IdFragmentAndAP(-1,2,BBTFRAG,0,-1,-1);
    	IdFragmentAndAP src2 = new IdFragmentAndAP(-1,2,BBTFRAG,1,-1,-1);
    	ArrayList<IdFragmentAndAP> srcAPs = new ArrayList<IdFragmentAndAP>();
    	srcAPs.add(src1);
    	srcAPs.add(src2);
    	Vertex src1V = Vertex.newVertexFromLibrary(-1,
    	        src1.getVertexMolId(), src1.getVertexMolType(), fs);
    	APClass src1APC = src1V.getAP(src1.getApId()).getAPClass();
        Vertex src2V = Vertex.newVertexFromLibrary(-1,
                src2.getVertexMolId(), src2.getVertexMolType(), fs);
        APClass src2APC = src2V.getAP(src2.getApId()).getAPClass();
    	
    	
    	/*
    	System.out.println("SRC1 "+fs.getAPClassForFragment(src1)+" => "
    	+fs.getCompatibleAPClasses(fs.getAPClassForFragment(src1)));
    	System.out.println("SRC2 "+fs.getAPClassForFragment(src2)+" => "
    	+fs.getCompatibleAPClasses(fs.getAPClassForFragment(src2)));
    	System.out.println("Frags with SRC1: "+fs.getFragAPsCompatibleWithClass(
    	        fs.getAPClassForFragment(src1)));
        System.out.println("Frags with SRC2: "+fs.getFragAPsCompatibleWithClass(
                fs.getAPClassForFragment(src2)));
    	*/
    	
    	ArrayList<IdFragmentAndAP> lst = fs.getFragAPsCompatibleWithTheseAPs(
    	        srcAPs);
    	
    	assertEquals(4,lst.size(),"Size of compatible APs list is wrong.");
    	
        int i = -1;
        for (IdFragmentAndAP id : lst)
        {
            i++;
            Vertex v = Vertex.newVertexFromLibrary(-1,
                    id.getVertexMolId(), id.getVertexMolType(), fs);
            AttachmentPoint ap = v.getAP(id.getApId());
            assertTrue(src1APC.isCPMapCompatibleWith(ap.getAPClass(), fs)
                    || src2APC.isCPMapCompatibleWith(ap.getAPClass(), fs), 
                    "Incompatible choice at "+i);
        }
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testGetFragmentsCompatibleWithTheseAPs() throws Exception
    {
        FragmentSpaceParameters fsp = buildFragmentSpace();
        FragmentSpace fs = fsp.getFragmentSpace();
        assertTrue(fs.isDefined(),"FragmentSpace is defined");
    	IdFragmentAndAP src1 = new IdFragmentAndAP(-1,2,BBTFRAG,0,-1,-1);
    	IdFragmentAndAP src2 = new IdFragmentAndAP(-1,2,BBTFRAG,1,-1,-1);
    	ArrayList<IdFragmentAndAP> srcAPs = new ArrayList<IdFragmentAndAP>();
    	srcAPs.add(src1);
    	srcAPs.add(src2);
    	
    	ArrayList<Vertex> lst = fs.getFragmentsCompatibleWithTheseAPs(
    	        srcAPs);
    	
    	assertEquals(3,lst.size(),"Wrong number of compatible fragments.");	
    }

//------------------------------------------------------------------------------

    /**
     * Check that the following graph's fused ring gets added to the fragment
     * library. Dots are chords
     *   ↑         ↑
     * ← C1 - C2 - C3 →
     *   .  / |    ↓
     *   C4 . C5 →
     *   ↓    ↓
     * @throws Exception 
     */
    @Test
    public void testFusedRingAddedToFragmentLibrary() throws Exception 
    {
        FragmentSpaceParameters fsp = buildFragmentSpace();
        fsp.fragmentLibFile = "dummyFilename_DenoptimTest_Frag";
        fsp.scaffoldLibFile = "dummyFilename_DenoptimTest_Scaff";
        FragmentSpace fs = fsp.getFragmentSpace();
        
        TestCase testCase = getTestCase();

        List<Vertex> fragLib = fs.getFragmentLibrary();
        fragLib.clear();

        fs.addFusedRingsToFragmentLibrary(testCase.graph);

        //Cleanup tmp files
        FileUtils.deleteFile(fsp.getPathnameToAppendedFragments());
        FileUtils.deleteFile(fsp.getPathnameToAppendedScaffolds());
        
        assertEquals(1, fragLib.size());
        Vertex actual = fragLib.get(0);
        StringBuilder sb = new StringBuilder();
        assertTrue(testCase.expected.sameAs(actual, sb),
                "Problem is "+sb.toString());
    }

//------------------------------------------------------------------------------

    /**
     * Checks that a graph with a fused ring containing a scaffold vertex is
     * added to the scaffold library.
     * @throws Exception 
     */
    @Test
    public void testFusedRingAddedToScaffoldLibrary() throws Exception 
    {
        FragmentSpaceParameters fsp = buildFragmentSpace();
        fsp.fragmentLibFile = "dummyFilename_DenoptimTest_Frag";
        fsp.scaffoldLibFile = "dummyFilename_DenoptimTest_Scaff";
        FragmentSpace fs = fsp.getFragmentSpace();
        TestCase testCase = getTestCase();
        testCase.graph.getVertexList().get(0)
                .setBuildingBlockType(BBType.SCAFFOLD);
        testCase.expected.setBuildingBlockType(BBType.SCAFFOLD);
        testCase.expected.getInnerGraph().getVertexList().get(0)
                .setBuildingBlockType(BBType.SCAFFOLD);

        List<Vertex> scaffLib = fs.getScaffoldLibrary();
        scaffLib.clear();

        fs.addFusedRingsToFragmentLibrary(testCase.graph);

        //Cleanup tmp files
        FileUtils.deleteFile(fsp.getPathnameToAppendedFragments());
        FileUtils.deleteFile(fsp.getPathnameToAppendedScaffolds());

        assertEquals(1, scaffLib.size());
        Vertex actual = scaffLib.get(0);
        assertTrue(testCase.expected.sameAs(actual, new StringBuilder()));
    }

//------------------------------------------------------------------------------

    @Test
    public void testFusedRingOnlyAddedOnce() throws DENOPTIMException
    {
        FragmentSpaceParameters fsp = buildFragmentSpace();
        fsp.fragmentLibFile = "dummyFilename_DenoptimTest_Frag";
        fsp.scaffoldLibFile = "dummyFilename_DenoptimTest_Scaff";
        FragmentSpace fs = fsp.getFragmentSpace();
        TestCase testCase = getTestCase();
        final int TRY_ADDING = 10;
        List<DGraph> sameGraphs = IntStream
                .range(0, TRY_ADDING)
                .mapToObj(i -> testCase.graph.clone())
                .peek(t -> {
                    try
                    {
                        t.renumberGraphVertices();
                    } catch (DENOPTIMException e)
                    {
                        e.printStackTrace();
                    }
                })
                .collect(Collectors.toList());

        List<Vertex> fragLib = fs.getFragmentLibrary();
        fragLib.clear();
        
        for (DGraph g : sameGraphs) {
            fs.addFusedRingsToFragmentLibrary(g);
        }

        //Cleanup tmp files
        FileUtils.deleteFile(fsp.getPathnameToAppendedFragments());
        FileUtils.deleteFile(fsp.getPathnameToAppendedScaffolds());
        
        assertEquals(1, fragLib.size());
    }

//------------------------------------------------------------------------------
    
    /**
     * <pre>
     *   ↑         ↑
     * ← C1 - C2 - C3 →
     *   .  / |    ↓
     *   C4 . C5 →
     *   ↓    ↓
     * </pre>
     */
    private TestCase getTestCase() throws DENOPTIMException 
    {
        DGraph g = new DGraph();
        Vertex c1 = getCarbonVertex(), c2 = getCarbonVertex(),
                c3 = getCarbonVertex(), c4 = getCarbonVertex(),
                c5 = getCarbonVertex();

        Vertex rcv14 = getRCV(), rcv41 = getRCV(), rcv45 = getRCV(),
                rcv54 = getRCV();

        g.addVertex(c1);
        g.appendVertexOnAP(c1.getAP(0), c2.getAP(0));
        g.appendVertexOnAP(c1.getAP(1), rcv14.getAP(0));
        g.appendVertexOnAP(c2.getAP(1), c3.getAP(0));
        g.appendVertexOnAP(c2.getAP(2), c5.getAP(0));
        g.appendVertexOnAP(c2.getAP(3), c4.getAP(0));
        g.appendVertexOnAP(c4.getAP(1), rcv41.getAP(0));
        g.appendVertexOnAP(c4.getAP(2), rcv45.getAP(0));
        g.appendVertexOnAP(c5.getAP(1), rcv54.getAP(0));

        Ring r124 = new Ring(Arrays.asList(rcv14, c1, c2, c4,
                rcv41));
        Ring r425 = new Ring(Arrays.asList(rcv45, c4, c2, c5,
                rcv54));
        g.addRing(r124);
        g.addRing(r425);

        g.renumberGraphVertices();

        Template t = getExpectedTemplate(g, c3);

        return new TestCase(g, t);
    }

//------------------------------------------------------------------------------

    private Template getExpectedTemplate(DGraph g,
            Vertex c3) throws DENOPTIMException {
        DGraph innerGraph = g.clone();
        innerGraph.renumberGraphVertices();
        Vertex c3Inner = innerGraph.getVertexAtPosition(g
                .indexOfVertexWithID(c3.getVertexId()));
        innerGraph.removeVertex(c3Inner);

        Template t = new Template(BBType.FRAGMENT);
        t.setInnerGraph(innerGraph);
        t.setBuildingBlockId(0);
        return t;
    }

//------------------------------------------------------------------------------

    private Fragment getCarbonVertex() throws DENOPTIMException
    {
        IChemObjectBuilder builder = DefaultChemObjectBuilder.getInstance();
        IAtom carbon = builder.newAtom();
        carbon.setSymbol("C");
        IAtomContainer mol = builder.newAtomContainer();
        mol.addAtom(carbon);
        Fragment v = new Fragment(
                GraphUtils.getUniqueVertexIndex(), 
                mol,
                BBType.FRAGMENT);
        for (int i = 0; i < 4; i++) {
            v.addAP(0, APC1, getRandomVector());
        }
        return v;
    }

//------------------------------------------------------------------------------

    private Vertex getRCV() throws DENOPTIMException 
    {
        IChemObjectBuilder builder = DefaultChemObjectBuilder
                .getInstance();
        IAtomContainer dummyMol = builder.newAtomContainer();
        IAtom dummyAtom = builder.newAtom();
        dummyMol.addAtom(dummyAtom);
        Fragment rcv = new Fragment(
                GraphUtils.getUniqueVertexIndex(),
                dummyMol, 
                BBType.FRAGMENT,
                true);
        rcv.addAP(0, APC1, getRandomVector());
        return rcv;
    }

//------------------------------------------------------------------------------

    private Point3d getRandomVector() {
        int precision = 10 * 10 * 10 * 10;

        Supplier<Double> randomCoord = () ->
                (double) (Math.round(rng.nextDouble() * (double) precision)) /
                        ((double) precision);

        return new Point3d(randomCoord.get(), randomCoord.get(),
                randomCoord.get());
    }

//------------------------------------------------------------------------------

    private static final class TestCase {
        final DGraph graph;
        final Template expected;

        TestCase(DGraph g, Template expected) {
            this.graph = g;
            this.expected = expected;
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Works with this graph:
     * <pre>
     *            c1 
     *            | 
     *   v1--v2--v3
     *        |   |
     *        |   v11
     *        |   (chord)
     *        |   v10          c3
     *        |   |            |
     *       v4--v6--v9--v5---v7
     *        |           \    |  
     *       c2            \   v12
     *                      |  (chord)
     *                      \  v13
     *                       \ |
     *                        v8-v13
     *                        |
     *                        c4  
     * </pre>
     */
    @Test
    public void testUseWholeMolGeometryForExtractedTemplates() throws Exception
    {   
        FragmentSpaceParameters fsp = buildFragmentSpace();
        fsp.fragmentLibFile = "dummyFilename_DenoptimTest_Frag";
        fsp.scaffoldLibFile = "dummyFilename_DenoptimTest_Scaff";
        FragmentSpace fs = fsp.getFragmentSpace();
        Fragment scaf = new Fragment();
        IAtom s1 = new Atom("C", new Point3d(0,0,0));
        scaf.addAtom(s1);
        scaf.addAPOnAtom(s1, APC2, new Point3d(1,1,0));
        scaf.addAPOnAtom(s1, APC3, new Point3d(1,-1,0));
        scaf.addAPOnAtom(s1, APC4, new Point3d(-1,-1,0));
        scaf.projectAPsToProperties();
        
        fs.appendVertexToLibrary(scaf, BBType.SCAFFOLD, fs.getScaffoldLibrary());
        int sId = fs.getScaffoldLibrary().size() - 1;
        
        Fragment frg = new Fragment();
        IAtom a1 = new Atom("C", new Point3d(0,0,0));
        IAtom a2 = new Atom("C", new Point3d(0,0,1));
        IAtom a3 = new Atom("C", new Point3d(0,0,2));
        frg.addAtom(a1);
        frg.addAtom(a2);
        frg.addAtom(a3);
        frg.addBond(new Bond(a1, a2));
        frg.addBond(new Bond(a2, a3));
        frg.addAPOnAtom(a1, APC2, new Point3d(1,1,2));
        frg.addAPOnAtom(a2, APC3, new Point3d(1,-1,0));
        frg.addAPOnAtom(a3, APC4, new Point3d(-1,-1,0));
        frg.projectAPsToProperties();
        
        fs.appendVertexToLibrary(frg, BBTFRAG, fs.getFragmentLibrary());
        int bbId = fs.getFragmentLibrary().size() - 1;
        
        Fragment rcv = new Fragment();
        IAtom a4 = new PseudoAtom("ATN", new Point3d(0,0,0));
        rcv.addAtom(a4);
        rcv.addAPOnAtom(a4, APClass.make("ATneutral",0), new Point3d(1,0,0));
        rcv.projectAPsToProperties();
        rcv.setAsRCV(true);
        
        fs.appendVertexToLibrary(rcv, BBTFRAG, fs.getFragmentLibrary());
        int rcvId = fs.getFragmentLibrary().size() - 1;
        
        DGraph wholeGraph = new DGraph();
        Vertex v1 = fs.getVertexFromLibrary(BBTFRAG, bbId);
        Vertex v2 = fs.getVertexFromLibrary(BBTFRAG, bbId);
        Vertex v3 = fs.getVertexFromLibrary(BBTFRAG, bbId);
        Vertex v4 = fs.getVertexFromLibrary(BBTFRAG, bbId);
        Vertex v5 = fs.getVertexFromLibrary(BBTFRAG, bbId);
        Vertex v6 = fs.getVertexFromLibrary(BBType.SCAFFOLD,sId);
        Vertex v7 = fs.getVertexFromLibrary(BBTFRAG, bbId);
        Vertex v8 = fs.getVertexFromLibrary(BBTFRAG, bbId);
        Vertex v9 = fs.getVertexFromLibrary(BBTFRAG, bbId);
        Vertex v10 = fs.getVertexFromLibrary(BBTFRAG, rcvId);
        Vertex v11 = fs.getVertexFromLibrary(BBTFRAG, rcvId);
        Vertex v12 = fs.getVertexFromLibrary(BBTFRAG, rcvId);
        Vertex v13 = fs.getVertexFromLibrary(BBTFRAG, rcvId);
        Vertex c1 = fs.getVertexFromLibrary(BBType.CAP, 0);
        Vertex c2 = fs.getVertexFromLibrary(BBType.CAP, 0);
        Vertex c3 = fs.getVertexFromLibrary(BBType.CAP, 0);
        Vertex c4 = fs.getVertexFromLibrary(BBType.CAP, 0);
        
        // Disordered... just for the fun of it. Still, do not change it further
        // as we need to wholeMol to be created consistently
        wholeGraph.addVertex(v7);
        wholeGraph.addVertex(v8);
        wholeGraph.addVertex(v9);
        wholeGraph.addVertex(v6);
        wholeGraph.addVertex(v1);
        wholeGraph.addVertex(v2);
        wholeGraph.addVertex(v3);
        wholeGraph.addVertex(v4);
        wholeGraph.addVertex(v5);
        wholeGraph.addVertex(v10);
        wholeGraph.addVertex(v11);
        wholeGraph.addVertex(v12);
        wholeGraph.addVertex(v13);
        wholeGraph.addVertex(c1);
        wholeGraph.addVertex(c2);
        wholeGraph.addVertex(c3);
        wholeGraph.addVertex(c4);
        wholeGraph.addEdge(new Edge(v6.getAP(0),v4.getAP(1),BondType.SINGLE));
        wholeGraph.addEdge(new Edge(v6.getAP(1),v10.getAP(0),BondType.SINGLE));
        wholeGraph.addEdge(new Edge(v4.getAP(0),v2.getAP(1),BondType.SINGLE));
        wholeGraph.addEdge(new Edge(v2.getAP(0),v3.getAP(0),BondType.SINGLE));
        wholeGraph.addEdge(new Edge(v3.getAP(1),v11.getAP(0),BondType.SINGLE));
        wholeGraph.addEdge(new Edge(v2.getAP(2),v1.getAP(2),BondType.SINGLE));
        wholeGraph.addEdge(new Edge(v6.getAP(2),v9.getAP(0),BondType.SINGLE));
        wholeGraph.addEdge(new Edge(v9.getAP(1),v5.getAP(0),BondType.SINGLE));
        wholeGraph.addEdge(new Edge(v5.getAP(1),v7.getAP(0),BondType.SINGLE));
        wholeGraph.addEdge(new Edge(v5.getAP(2),v8.getAP(0),BondType.SINGLE));
        wholeGraph.addEdge(new Edge(v7.getAP(1),v12.getAP(0),BondType.SINGLE));
        wholeGraph.addEdge(new Edge(v8.getAP(2),v13.getAP(0),BondType.SINGLE));
        wholeGraph.addEdge(new Edge(v3.getAP(2),c1.getAP(0),BondType.SINGLE));
        wholeGraph.addEdge(new Edge(v4.getAP(2),c2.getAP(0),BondType.SINGLE));
        wholeGraph.addEdge(new Edge(v7.getAP(2),c3.getAP(0),BondType.SINGLE));
        wholeGraph.addEdge(new Edge(v8.getAP(1),c4.getAP(0),BondType.SINGLE));
        
        wholeGraph.addRing(v10, v11, BondType.DOUBLE);
        wholeGraph.addRing(v12, v13, BondType.DOUBLE);
        
        ThreeDimTreeBuilder tb3d = new ThreeDimTreeBuilder();
        IAtomContainer wholeMol = tb3d.convertGraphTo3DAtomContainer(
                wholeGraph.clone(), true);
        
        double r = 10;
        for (int i=0; i<wholeMol.getAtomCount(); i++)
        {
            IAtom atm = wholeMol.getAtom(i);
            if (atm instanceof PseudoAtom)
                ((PseudoAtom) atm).setLabel("Du");
            else
                atm.setSymbol("P");
            atm.setPoint3d(new Point3d(r*Math.cos(Math.toRadians(360/34 * i)),
                    r*Math.sin(Math.toRadians(360/34 * i)),0));
        }
        
        int szScafLibPre = fs.getScaffoldLibrary().size();
        int szFragLibPre = fs.getFragmentLibrary().size();
        
        String scafFile = tempDir.getAbsolutePath() + SEP + "newScaf.sdf";
        String fragFile = tempDir.getAbsolutePath() + SEP + "newFrag.sdf";
        fsp.scaffoldLibFile = scafFile;
        fsp.fragmentLibFile = fragFile;

        fs.addFusedRingsToFragmentLibrary(wholeGraph, true, true, wholeMol);
        
        // NB: in here there is cloning of template with mol representation
        assertEquals(szScafLibPre+1,fs.getScaffoldLibrary().size(),
                "Size scaffolds library");
        assertEquals(szFragLibPre+1,fs.getFragmentLibrary().size(),
                "Size fragments library");
        
        Vertex newScaff = fs.getVertexFromLibrary(
                BBType.SCAFFOLD,szScafLibPre); // szScafLibPre+1-1
        assertEquals(4,newScaff.getAttachmentPoints().size(), 
                "#APs on new scaffold");
        int nP = 0;
        for (IAtom a : newScaff.getIAtomContainer().atoms())
        {
            if ("P".equals(MoleculeUtils.getSymbolOrLabel(a)))
                nP++;
        }
        assertEquals(10,nP,"#P in new scaffold");
        
        Vertex newFrag = fs.getVertexFromLibrary(
                BBType.FRAGMENT,szFragLibPre); // szFragLibPre+1-1
        assertEquals(3,newFrag.getAttachmentPoints().size(), 
                "#APs on new fragment");
        nP = 0;
        for (IAtom a : newFrag.getIAtomContainer().atoms())
        {
            if ("P".equals(MoleculeUtils.getSymbolOrLabel(a)))
                nP++;
        }
        assertEquals(9,nP,"#P in new fragment");
        
        //Cleanup tmp files
        FileUtils.deleteFile(fsp.getPathnameToAppendedFragments());
        FileUtils.deleteFile(fsp.getPathnameToAppendedScaffolds());
    }

//------------------------------------------------------------------------------
}
