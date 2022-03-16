package denoptim.io;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openscience.cdk.Atom;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.files.FileFormat;
import denoptim.files.FileUtils;
import denoptim.files.UndetectedFileFormatException;
import denoptim.graph.APClass;
import denoptim.graph.CandidateLW;
import denoptim.graph.DENOPTIMEdge;
import denoptim.graph.DENOPTIMEdge.BondType;
import denoptim.graph.DENOPTIMFragment;
import denoptim.graph.DENOPTIMFragmentTest;
import denoptim.graph.DENOPTIMGraph;
import denoptim.graph.DENOPTIMRing;
import denoptim.graph.DENOPTIMTemplate;
import denoptim.graph.DENOPTIMTemplateTest;
import denoptim.graph.DENOPTIMVertex;
import denoptim.graph.DENOPTIMVertex.BBType;
import denoptim.graph.EmptyVertex;
import denoptim.graph.SymmetricSet;

/**
 * Unit test for input/output.
 * 
 * @author Marco Foscato
 */

public class DenoptimIOTest 
{

    private static final BondType BT = BondType.SINGLE;

    private final String SEP = System.getProperty("file.separator");
    private final String NL = System.getProperty("line.separator");

    @TempDir
    File tempDir;
    
//------------------------------------------------------------------------------

    @Test
    public void testIOEmptyVertex() throws Exception {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        
        EmptyVertex v = new EmptyVertex();
        Point3d xyz = new Point3d(1.1,-2.2,3.3);
        v.addAP(APClass.make("myClass:0"));
        v.addAP(APClass.make("myClass:1"));
        v.addAP(APClass.make("myClass:2"));
        v.addAP(APClass.make("myClass:3"));
        
        ArrayList<DENOPTIMVertex> initVrtxs = new ArrayList<DENOPTIMVertex>();
        initVrtxs.add(v);

        File tmpFile = new File(tempDir.getAbsolutePath() + SEP + "test.sdf");
        DenoptimIO.writeVertexesToFile(tmpFile, FileFormat.VRTXSDF, initVrtxs);
        ArrayList<DENOPTIMVertex> readInVrtxs = 
                DenoptimIO.readVertexes(tmpFile, BBType.UNDEFINED);
        assertEquals(1,readInVrtxs.size(),"Number of vertexes");
        StringBuilder sb = new StringBuilder();
        assertTrue(v.sameAs(readInVrtxs.get(0),sb),"Same vertex content: " 
                + sb.toString());
        
        tmpFile = new File(tempDir.getAbsolutePath() + SEP + "test.json");
        DenoptimIO.writeVertexesToFile(tmpFile, FileFormat.VRTXJSON, 
                initVrtxs);
        readInVrtxs = DenoptimIO.readVertexes(tmpFile, BBType.UNDEFINED);
        assertEquals(1,readInVrtxs.size(),"Number of vertexes");
        sb = new StringBuilder();
        assertTrue(v.sameAs(readInVrtxs.get(0),sb),"Same vertex content: " 
                + sb.toString());
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testIOMolFragment() throws Exception {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        
        DENOPTIMFragment v = DENOPTIMFragmentTest.makeFragment();
        
        ArrayList<DENOPTIMVertex> initVrtxs = new ArrayList<DENOPTIMVertex>();
        initVrtxs.add(v);

        File tmpFile = new File(tempDir.getAbsolutePath() + SEP + "test.sdf");
        DenoptimIO.writeVertexesToFile(tmpFile, FileFormat.VRTXSDF, initVrtxs);
        ArrayList<DENOPTIMVertex> readInVrtxs = 
                DenoptimIO.readVertexes(tmpFile, BBType.SCAFFOLD);
        assertEquals(1,readInVrtxs.size(),"Number of vertexes");
        StringBuilder sb = new StringBuilder();
        assertTrue(v.sameAs(readInVrtxs.get(0),sb),"Same vertex content: " 
                + sb.toString());
        
        tmpFile = new File(tempDir.getAbsolutePath() + SEP + "test.json");
        DenoptimIO.writeVertexesToFile(tmpFile, FileFormat.VRTXJSON, 
                initVrtxs);
        readInVrtxs = DenoptimIO.readVertexes(tmpFile, BBType.SCAFFOLD);
        assertEquals(1,readInVrtxs.size(),"Number of vertexes");
        sb = new StringBuilder();
        assertTrue(v.sameAs(readInVrtxs.get(0),sb),"Same vertex content: " 
                + sb.toString());
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testIOTemplate() throws Exception {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        
        DENOPTIMTemplate v = DENOPTIMTemplateTest.getTestAmideTemplate();
        
        ArrayList<DENOPTIMVertex> initVrtxs = new ArrayList<DENOPTIMVertex>();
        initVrtxs.add(v);

        File tmpFile = new File(tempDir.getAbsolutePath() + SEP + "test.sdf");
        DenoptimIO.writeVertexesToFile(tmpFile, FileFormat.VRTXSDF, initVrtxs);
        ArrayList<DENOPTIMVertex> readInVrtxs = 
                DenoptimIO.readVertexes(tmpFile, BBType.SCAFFOLD);
        assertEquals(1,readInVrtxs.size(),"Number of vertexes");
        StringBuilder sb = new StringBuilder();
        assertTrue(v.sameAs(readInVrtxs.get(0),sb),"Same vertex content: " 
                + sb.toString());
        
        tmpFile = new File(tempDir.getAbsolutePath() + SEP + "test.json");
        DenoptimIO.writeVertexesToFile(tmpFile, FileFormat.VRTXJSON, 
                initVrtxs);
        readInVrtxs = DenoptimIO.readVertexes(tmpFile, BBType.SCAFFOLD);
        assertEquals(1,readInVrtxs.size(),"Number of vertexes");
        sb = new StringBuilder();
        assertTrue(v.sameAs(readInVrtxs.get(0),sb),"Same vertex content: " 
                + sb.toString());
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testReadLightWeightCandidate() throws Exception {

        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        String pathName = tempDir.getAbsolutePath() + SEP + "test.sdf";
        
        IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
        IAtomContainer iac = builder.newAtomContainer();
        iac.addAtom(new Atom("C"));
        String uid = "mmyUiD";
        String name = "myName";
        double fitness = 999.123;
        String err = "myError";
        String msg = "myMSG";
        int level = 26;
        iac.setProperty(DENOPTIMConstants.UNIQUEIDTAG, uid);
        iac.setProperty(CDKConstants.TITLE, name);
        iac.setProperty(DENOPTIMConstants.FITNESSTAG, fitness);
        iac.setProperty(DENOPTIMConstants.MOLERRORTAG, err);
        iac.setProperty(DENOPTIMConstants.GMSGTAG, msg);
        iac.setProperty(DENOPTIMConstants.GRAPHLEVELTAG, level);
        
        DenoptimIO.writeSDFFile(pathName, iac, false);
        
        IAtomContainer iac2 = builder.newAtomContainer();
        iac.addAtom(new Atom("C"));
        String uid2 = "mmyUiD2";
        String name2 = "myName2";
        double fitness2 = 999.1232;
        String err2 = "myError2";
        String msg2 = "myMSG2";
        int level2 = 262;
        iac2.setProperty(DENOPTIMConstants.UNIQUEIDTAG, uid2);
        iac2.setProperty(CDKConstants.TITLE, name2);
        iac2.setProperty(DENOPTIMConstants.FITNESSTAG, fitness2);
        iac2.setProperty(DENOPTIMConstants.MOLERRORTAG, err2);
        iac2.setProperty(DENOPTIMConstants.GMSGTAG, msg2);
        iac2.setProperty(DENOPTIMConstants.GRAPHLEVELTAG, level2);
        
        DenoptimIO.writeSDFFile(pathName, iac2, true);
        
        List<CandidateLW> cands = DenoptimIO.readLightWeightCandidate(
                new File(pathName));
        
        assertEquals(2,cands.size(), "number of candidates in file");
        assertEquals(uid,cands.get(0).getUid(), "UID 1st");
        assertEquals(uid2,cands.get(1).getUid(), "UID 2nd");
        assertEquals(name,cands.get(0).getName(), "name 1st");
        assertEquals(name2,cands.get(1).getName(), "name 2nd");
        assertTrue(0.001 > Math.abs(fitness- cands.get(0).getFitness()), 
                "fitness 1st");
        assertTrue(0.001 > Math.abs(fitness2 - cands.get(1).getFitness()), 
                "fitness 2nd");
    }
    
//------------------------------------------------------------------------------

	@Test
	public void testSerializeDeserializeDENOPTIMGraphs() throws Exception {
	    assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        String serFile = tempDir.getAbsolutePath() + SEP + "graph.ser";
        String jsonFile = tempDir.getAbsolutePath() + SEP + "graph.json";
        
		DENOPTIMGraph graph = new DENOPTIMGraph();
		EmptyVertex v0 = new EmptyVertex(0);
		buildVertexAndConnectToGraph(v0, 3, graph);

		EmptyVertex v1 = new EmptyVertex(1);
		buildVertexAndConnectToGraph(v1, 2, graph);
		graph.addEdge(new DENOPTIMEdge(v0.getAP(0), v1.getAP(0),BT));

		EmptyVertex v2 = new EmptyVertex(2);
		buildVertexAndConnectToGraph(v2, 2, graph);
		graph.addEdge(new DENOPTIMEdge(v1.getAP(1), v2.getAP(0),BT));

		EmptyVertex v3 = new EmptyVertex(3);
		buildVertexAndConnectToGraph(v3, 1, graph);
		graph.addEdge(new DENOPTIMEdge(v2.getAP(1), v3.getAP(0),BT));

		EmptyVertex v4 = new EmptyVertex(4);
		buildVertexAndConnectToGraph(v4, 3, graph);
		graph.addEdge(new DENOPTIMEdge(v0.getAP(1), v4.getAP(0),BT));

		EmptyVertex v5 = new EmptyVertex(5);
		buildVertexAndConnectToGraph(v5, 1, graph);
		graph.addEdge(new DENOPTIMEdge(v4.getAP(1), v5.getAP(0),BT));

		EmptyVertex v6 = new EmptyVertex(6);
		buildVertexAndConnectToGraph(v6, 1, graph);
		graph.addEdge(new DENOPTIMEdge(v0.getAP(2), v6.getAP(0),BT));

		EmptyVertex v7 = new EmptyVertex(7);
		buildVertexAndConnectToGraph(v7, 1, graph);
		graph.addEdge(new DENOPTIMEdge(v4.getAP(2), v7.getAP(0),BT));

		graph.addRing(new DENOPTIMRing(new ArrayList<>(
				Arrays.asList(v5, v4, v0, v1, v2, v3))));

		graph.addRing(new DENOPTIMRing(new ArrayList<>(
				Arrays.asList(v6, v0, v4, v7))));

		graph.addSymmetricSetOfVertices(new SymmetricSet(
				new ArrayList<>(Arrays.asList(3, 5))));

		graph.addSymmetricSetOfVertices(new SymmetricSet(
				new ArrayList<>(Arrays.asList(6, 7))));

		DenoptimIO.serializeToFile(serFile, graph, false);
        DENOPTIMGraph graphA = DenoptimIO.deserializeDENOPTIMGraph(
                new File(serFile));
        StringBuilder reason = new StringBuilder();
        assertTrue(graph.sameAs(graphA, reason));
        
		DenoptimIO.writeData(jsonFile, graph.toJson(), false);
		DENOPTIMGraph graphJ = DenoptimIO.readDENOPTIMGraphsFromJSONFile(
		        jsonFile).get(0);
		assertNotNull(graphJ,"Graph read from JSON file is null");
		assertTrue(graph.sameAs(graphJ, reason));
	}

//------------------------------------------------------------------------------

	@Test
	public void testReadAllAPClasses() throws Exception {
		DENOPTIMFragment frag = new DENOPTIMFragment();
		IAtom atmC = new Atom("C");
		atmC.setPoint3d(new Point3d(0.0, 0.0, 1.0));
		IAtom atmH = new Atom("H");
		atmH.setPoint3d(new Point3d(0.0, 1.0, 1.0));
		frag.addAtom(atmC);
		frag.addAtom(atmH);
		frag.addAPOnAtom(atmC, APClass.make("classAtmC:5"),
		        new Point3d(1.0, 0.0, 0.0));
		frag.addAPOnAtom(atmC, APClass.make("classAtmC:5"),
		        new Point3d(1.0, 1.0, 0.0));
		frag.addAPOnAtom(atmC, APClass.make("otherClass:0"),
		        new Point3d(-1.0, 0.0, 0.0));
		frag.addAPOnAtom(atmH, APClass.make("classAtmH:1"),
		        new Point3d(1.0, 2.0, 2.0));
		frag.projectAPsToProperties();

		DENOPTIMFragment frag2 = new DENOPTIMFragment();
		IAtom atmO = new Atom("O");
		atmO.setPoint3d(new Point3d(0.0, 0.0, 1.0));
		IAtom atmH2 = new Atom("N");
		atmH.setPoint3d(new Point3d(0.0, 1.0, 1.0));
		frag2.addAtom(atmO);
		frag2.addAtom(atmH2);
		frag2.addAPOnAtom(atmO, APClass.make("apClassO:5"),
		        new Point3d(1.0, 0.0, 0.0));
		frag2.addAPOnAtom(atmO, APClass.make("apClassO:6"),
		        new Point3d(1.0, 1.0, 0.0));
		frag2.addAPOnAtom(atmO, APClass.make("apClassObis:0"),
		        new Point3d(-1.0, 0.0, 0.0));
		frag2.addAPOnAtom(atmH2, APClass.make("classAtmH:1"),
		        new Point3d(1.0, 2.0, 2.0));
		frag2.projectAPsToProperties();

		ArrayList<DENOPTIMVertex> frags = new ArrayList<DENOPTIMVertex>();
		frags.add(frag);
		frags.add(frag2);

		String tmpFile = FileUtils.getTempFolder()
				+ System.getProperty("file.separator") + "frag.sdf";
		DenoptimIO.writeVertexesToSDF(new File(tmpFile), frags, false);

		Set<APClass> allAPC = DenoptimIO.readAllAPClasses(new File(tmpFile));

		assertEquals(6, allAPC.size(), "Size did not match");
		assertTrue(allAPC.contains(APClass.make("apClassObis:0")), 
		        "Contains APClass (1)");
		assertTrue(allAPC.contains(APClass.make("otherClass:0")), 
		        "Contains APClass (2)");
	}

//------------------------------------------------------------------------------

	private void buildVertexAndConnectToGraph(EmptyVertex v, int apCount,
											  DENOPTIMGraph graph) 
											          throws DENOPTIMException {
		for (int atomPos = 0; atomPos < apCount; atomPos++) {
			v.addAP();
		}
		graph.addVertex(v);
	}
    
//------------------------------------------------------------------------------

    @Test
    public void testDetectFileFormat() throws Exception {
        assertTrue(this.tempDir.isDirectory(),"Should be a directory ");
        String pathName = tempDir.getAbsolutePath() + SEP + "graph.sdf";
        File file = new File(pathName);
        final File ffile = new File(pathName);
        
        DenoptimIO.writeData(pathName, "dummy text", false);
        assertThrows(UndetectedFileFormatException.class, 
                () -> FileUtils.detectFileFormat(ffile));
        
        DenoptimIO.writeData(pathName, 
                "> <" + DENOPTIMConstants.APSTAG + ">"+ NL
                + "> <" + DENOPTIMConstants.VERTEXJSONTAG + ">", false);
        assertTrue(FileFormat.VRTXSDF == FileUtils.detectFileFormat(file),
                "Vertex SDF");
        
        DenoptimIO.writeData(pathName, 
                "> <" + DENOPTIMConstants.APSTAG + ">"+ NL
                + "> <" + DENOPTIMConstants.GRAPHJSONTAG + ">", false);
        assertTrue(FileFormat.GRAPHSDF == FileUtils.detectFileFormat(file),
                "Graph SDF");
        
        DenoptimIO.writeData(pathName, "> <" + DENOPTIMConstants.GRAPHJSONTAG 
                + ">", false);
        assertTrue(FileFormat.GRAPHSDF == FileUtils.detectFileFormat(file),
                "Graph SDF");
        
        pathName = tempDir.getAbsolutePath() + SEP + "filename";
        file = new File(pathName);
        
        DenoptimIO.writeData(pathName, "FSE-SOMETING", false);
        assertTrue(FileFormat.FSE_PARAM == FileUtils.detectFileFormat(file),
                "FSE params");
        
        DenoptimIO.writeData(pathName, "GA-SOMETING", false);
        assertTrue(FileFormat.GA_PARAM == FileUtils.detectFileFormat(file),
                "GA params");
        
        DenoptimIO.writeData(pathName, "RCN SOMETING", false);
        assertTrue(FileFormat.COMP_MAP == FileUtils.detectFileFormat(file),
                "Compatibility Matrix (1)");
        
        DenoptimIO.writeData(pathName, "RBO SOMETING", false);
        assertTrue(FileFormat.COMP_MAP == FileUtils.detectFileFormat(file),
                "Compatibility Matrix (2)");
        
        DenoptimIO.writeData(pathName, "CAP SOMETING", false);
        assertTrue(FileFormat.COMP_MAP == FileUtils.detectFileFormat(file),
                "Compatibility Matrix (3)");
        
        String dirName = tempDir.getAbsolutePath() + SEP + "blabla1234";
        String subDirName = dirName + SEP+DENOPTIMConstants.FSEIDXNAMEROOT+"0";
        
        FileUtils.createDirectory(dirName);
        FileUtils.createDirectory(subDirName);
        assertTrue(FileFormat.FSE_RUN == FileUtils.detectFileFormat(
                new File(dirName)), "FSE output folder");
        
        dirName = tempDir.getAbsolutePath() + SEP + "blabla5678";
        subDirName = dirName + SEP+DENOPTIMConstants.GAGENDIRNAMEROOT+"0";
        FileUtils.createDirectory(dirName);
        FileUtils.createDirectory(subDirName);
        assertTrue(FileFormat.GA_RUN == FileUtils.detectFileFormat(
                new File(dirName)), "GA output folder");
    }
}