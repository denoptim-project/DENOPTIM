/*
 *   DENOPTIM
 *   Copyright (C) 2022 Marco Foscato <marco.foscato@uib.no>
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

package denoptim.ga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import denoptim.exception.DENOPTIMException;
import denoptim.files.FileFormat;
import denoptim.fragmenter.ScaffoldingPolicy;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.ga.EAUtils.CandidateSource;
import denoptim.graph.APClass;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.Candidate;
import denoptim.graph.DGraph;
import denoptim.graph.DGraphTest;
import denoptim.graph.Edge.BondType;
import denoptim.graph.EmptyVertex;
import denoptim.graph.GraphPattern;
import denoptim.graph.Template;
import denoptim.graph.Template.ContractLevel;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;
import denoptim.io.DenoptimIO;
import denoptim.logging.Monitor;
import denoptim.programs.denovo.GAParameters;
import denoptim.programs.fragmenter.CuttingRule;
import denoptim.utils.Randomizer;

/**
 * Unit test
 * 
 * @author Marco Foscato
 */

public class EAUtilsTest
{
    
    private static APClass APCA, APCB, APCC;
    private static String a="A", b="B", c="C";

    private static final String SEP = System.getProperty("file.separator");
    private static final String NL = System.getProperty("line.separator");

    @TempDir 
    static File tempDir;
    
//------------------------------------------------------------------------------
    
    private FragmentSpaceParameters prepare() throws DENOPTIMException
    {
        APCA = APClass.make(a, 0,BondType.SINGLE);
        APCB = APClass.make(b, 1,BondType.SINGLE);
        APCC = APClass.make(c, 2,BondType.SINGLE);
        
        HashMap<APClass,ArrayList<APClass>> cpMap = 
                new HashMap<APClass,ArrayList<APClass>>();
        ArrayList<APClass> lstA = new ArrayList<APClass>();
        lstA.add(APCA);
        cpMap.put(APCA, lstA);
        ArrayList<APClass> lstB = new ArrayList<APClass>();
        lstB.add(APCB);
        lstB.add(APCC);
        cpMap.put(APCB, lstB);
        ArrayList<APClass> lstC = new ArrayList<APClass>();
        lstC.add(APCB);
        lstC.add(APCC);
        cpMap.put(APCC, lstC);
        
        /* Compatibility matrix
         * 
         *      |  A  |  B  |  C  |
         *    -------------------------
         *    A |  T  |     |     |   
         *    -------------------------
         *    B |     |  T  |  T  |   
         *    -------------------------
         *    C |     |  T  |  T  |   
         *    -------------------------
         */
        
        HashMap<APClass,APClass> capMap = new HashMap<APClass,APClass>();
        HashSet<APClass> forbEnds = new HashSet<APClass>();
        
        FragmentSpaceParameters fsp = new FragmentSpaceParameters();
        FragmentSpace fs = new FragmentSpace(fsp,
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(),
                new ArrayList<Vertex>(), 
                cpMap, capMap, forbEnds, cpMap);
        fs.setAPclassBasedApproach(true);
        
        EmptyVertex v1 = new EmptyVertex();
        v1.setBuildingBlockType(BBType.FRAGMENT);
        v1.addAP(APCB);
        v1.addAP(APCB);
        fs.appendVertexToLibrary(v1, BBType.FRAGMENT, fs.getFragmentLibrary());
        
        EmptyVertex v2 = new EmptyVertex();
        v2.setBuildingBlockType(BBType.FRAGMENT);
        v2.addAP(APCC);
        v2.addAP(APCC);
        fs.appendVertexToLibrary(v2, BBType.FRAGMENT, fs.getFragmentLibrary());
        
        EmptyVertex rcv = new EmptyVertex();
        rcv.setBuildingBlockType(BBType.FRAGMENT);
        rcv.addAP(APCC);
        rcv.setAsRCV(true);
        fs.appendVertexToLibrary(rcv, BBType.FRAGMENT, fs.getFragmentLibrary());
        
        DGraph graphForTemplate = new DGraph();
        Vertex vg1 = Vertex.newVertexFromLibrary(0,
                        BBType.FRAGMENT, fs);
        graphForTemplate.addVertex(vg1);
        for (int i=1; i<6; i++)
        {
            Vertex vgi = Vertex.newVertexFromLibrary(0,
                    BBType.FRAGMENT, fs);
            Vertex vgi_1 = graphForTemplate.getVertexAtPosition(i-1);
            graphForTemplate.appendVertexOnAP(vgi_1.getAP(1), vgi.getAP(0));
        }
        Vertex rcv1 = Vertex.newVertexFromLibrary(2,
                BBType.FRAGMENT, fs);
        graphForTemplate.appendVertexOnAP(
                graphForTemplate.getVertexAtPosition(5).getAP(1),rcv1.getAP(0));
        Vertex rcv2 = Vertex.newVertexFromLibrary(2,
                BBType.FRAGMENT, fs);
        graphForTemplate.appendVertexOnAP(
                graphForTemplate.getVertexAtPosition(0).getAP(0),rcv2.getAP(0));
        graphForTemplate.addRing(rcv1, rcv2);
        
        Template template = new Template(BBType.SCAFFOLD);
        template.setInnerGraph(graphForTemplate);
        template.setContractLevel(ContractLevel.FIXED_STRUCT);
        fs.appendVertexToLibrary(template, BBType.SCAFFOLD,
                fs.getScaffoldLibrary());
        
        return fsp;
    }

//------------------------------------------------------------------------------

    @Test
    public void testBuildGraphFromTemplateScaffold() throws Exception
    {
        FragmentSpaceParameters fsp = prepare();
        GAParameters gaParams = new GAParameters();
        gaParams.setParameters(fsp);
        DGraph g = EAUtils.buildGraph(gaParams);
        
        if (g == null)
            assertTrue(false,"faild construction of graph");
        
        Template t = (Template) g.getVertexAtPosition(0);
        
        //NB: we want to not allow access to the inner graph from outside the 
        // template, but this means we cannot easily explore the inner graph.
        // Looking at the mutation sites is a dirty trick to get a look inside 
        // this template, that has contract level "free" (meaning that the 
        // content of the template can change).
        
        boolean foundChange = false;
        for (Vertex v : t.getMutationSites())
        {
            if (v.getBuildingBlockId() != 0)
                foundChange = true;
        }
        assertTrue(foundChange,"The initial inner graph has changed.");
    }

//------------------------------------------------------------------------------

    @Test
    public void testAvoidRedundantXOver() throws Exception
    {   
        DGraph g1 = new DGraph();
        EmptyVertex s = new EmptyVertex();
        s.addAP();
        s.addAP();
        s.setBuildingBlockType(BBType.SCAFFOLD);
        
        EmptyVertex v = new EmptyVertex();
        v.addAP();
        v.setBuildingBlockType(BBType.FRAGMENT);
        
        g1.addVertex(s);
        g1.appendVertexOnAP(s.getAP(0), v.getAP(0));
        
        Candidate c1 = new Candidate(g1); 
        
        DGraph g2 = g1.clone();
        Candidate c2 = new Candidate(g2);
        
        FragmentSpaceParameters fsp = prepare();
        GAParameters gaParams = new GAParameters();
        gaParams.setParameters(fsp);
        
        ArrayList<Candidate> eligibleParents = new ArrayList<Candidate>();
        eligibleParents.add(c1);
        eligibleParents.add(c2);
        Population population = new Population(gaParams);
        population.add(c1);
        population.add(c2);
        Monitor mnt = new Monitor();
        
        Candidate offspring = EAUtils.buildCandidateByXOver(eligibleParents, 
                population, mnt, gaParams);
        
        assertTrue(offspring==null, "Redundat xover is not done");
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testBuildByXOver_SubGraph() throws Exception
    {
        GAParameters gaparams = PopulationTest.prepare();
        Population population = new Population(gaparams);
        
        /*
         * -(A)v0(A)-(A)v1(A)-(A)v2(A)-(A)v3(B)-(B)v4(B)-(B)v5(B)-
         */
        DGraph gA = PopulationTest.makeGraphA();
        Candidate cA = new Candidate("CA",gA);
        cA.setFitness(1.23);
        population.add(cA);
        
        /*
         * v0(B)-(B)v1(A)-(A)v2(B)-(B)v3(A)-(A)v4(B)-(B)v5
         */
        DGraph gE = PopulationTest.makeGraphE();
        Candidate cE = new Candidate("CE",gE);
        cE.setFitness(2.34);
        population.add(cE);
        
        ArrayList<Candidate> eligibleParents = new ArrayList<Candidate>();
        eligibleParents.add(cA);
        eligibleParents.add(cE);

        Monitor mnt = new Monitor();
        
        Candidate offspring0 = EAUtils.buildCandidateByXOver(eligibleParents, 
                population, mnt, new int[]{0,1}, 3, 0, gaparams);
        
        Candidate offspring1 = EAUtils.buildCandidateByXOver(eligibleParents, 
                population, mnt, new int[]{0,1}, 3, 1, gaparams);
    
        DGraph g0 = offspring0.getGraph();
        assertEquals(4,g0.getVertexCount());
        assertEquals(3,g0.getEdgeCount());
        int maxLength = -1;
        for (Vertex v : g0.getVertexList())
        {
            ArrayList<Vertex> childTree = new ArrayList<Vertex>();
            g0.getChildrenTree(v, childTree);
            if (childTree.size()>maxLength)
            {
                maxLength = childTree.size();
            }
        }
        assertEquals(3,maxLength);

        DGraph g1 = offspring1.getGraph();
        assertEquals(8,g1.getVertexCount());
        assertEquals(7,g1.getEdgeCount());
        maxLength = -1;
        for (Vertex v : g1.getVertexList())
        {
            ArrayList<Vertex> childTree = new ArrayList<Vertex>();
            g1.getChildrenTree(v, childTree);
            if (childTree.size()>maxLength)
            {
                maxLength = childTree.size();
            }
        }
        assertEquals(7,maxLength);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * NB: the graphs from methods {@link #getPairOfTestGraphsB()} and
     * {@link #getPairOfTestGraphsBxo()} and  
     * {@link #getPairOfTestGraphsBxoxo()} are a sequence resulting from 
     * crossover operations. Note that the order of APs in on the templates
     * changes as a result of the crossover. For this reason, the backwards 
     * crossover of the graphs from {@link #getPairOfTestGraphsBxo()} does not
     * produce the graphs from {@link #getPairOfTestGraphsB()}, but
     * those from {@link #getPairOfTestGraphsBxoxo()}.
     */
    @Test
    public void testBuildByXOver_Embedded_Free() throws Exception
    {
        GAParameters gaparams = PopulationTest.prepare();
        Population population = new Population(gaparams);

        DGraph[] pair = PopulationTest.getPairOfTestGraphsB();
        DGraph gA = pair[0];
        DGraph gB = pair[1];
        ((Template)gA.getVertexAtPosition(1)).setContractLevel(
                ContractLevel.FREE);
        ((Template)gB.getVertexAtPosition(1)).setContractLevel(
                ContractLevel.FREE);
        
        Candidate cA = new Candidate("CA",gA);
        population.add(cA);        

        Candidate cB = new Candidate("CB",gB);
        population.add(cB);
        
        ArrayList<Candidate> eligibleParents = new ArrayList<Candidate>();
        eligibleParents.add(cA);
        eligibleParents.add(cB);

        Monitor mnt = new Monitor();
        
        Candidate offspring0 = EAUtils.buildCandidateByXOver(eligibleParents, 
                population, mnt, new int[]{0,1}, 8, 0, gaparams);
        
        Candidate offspring1 = EAUtils.buildCandidateByXOver(eligibleParents, 
                population, mnt, new int[]{0,1}, 8, 1, gaparams);
        
        DGraph g0xo = offspring0.getGraph();
        DGraph g1xo = offspring1.getGraph();
        
        DGraph[] expectedPair = PopulationTest.getPairOfTestGraphsBxo();
        DGraph expected0 = expectedPair[0];
        DGraph expected1 = expectedPair[1];
        ((Template)expected0.getVertexAtPosition(1)).setContractLevel(
                ContractLevel.FREE);
        ((Template)expected1.getVertexAtPosition(1)).setContractLevel(
                ContractLevel.FREE);
        
        assertTrue(expected0.sameAs(g0xo, new StringBuilder()));
        assertTrue(expected1.sameAs(g1xo, new StringBuilder()));
    }
    
//------------------------------------------------------------------------------
    
    /**
     * NB: the graphs from methods {@link #getPairOfTestGraphsB()} and
     * {@link #getPairOfTestGraphsBxo()} and  
     * {@link #getPairOfTestGraphsBxoxo()} are a sequence resulting from 
     * crossover operations. Note that the order of APs in on the templates
     * changes as a result of the crossover. For this reason, the backwards 
     * crossover of the graphs from {@link #getPairOfTestGraphsBxo()} does not
     * produce the graphs from {@link #getPairOfTestGraphsB()}, but
     * those from {@link #getPairOfTestGraphsBxoxo()}.
     */
    @Test
    public void testBuildByXOver_Embedded_FreeBackwards() throws Exception
    {
        GAParameters gaparams = PopulationTest.prepare();
        Population population = new Population(gaparams);

        DGraph[] pair = PopulationTest.getPairOfTestGraphsBxo();
        DGraph gA = pair[0];
        DGraph gB = pair[1];
        ((Template)gA.getVertexAtPosition(1)).setContractLevel(
                ContractLevel.FREE);
        ((Template)gB.getVertexAtPosition(1)).setContractLevel(
                ContractLevel.FREE);
        
        Candidate cA = new Candidate("CA",gA);
        population.add(cA);        

        Candidate cB = new Candidate("CB",gB);
        population.add(cB);
        
        ArrayList<Candidate> eligibleParents = new ArrayList<Candidate>();
        eligibleParents.add(cA);
        eligibleParents.add(cB);

        Monitor mnt = new Monitor();
        
        Candidate offspring0 = EAUtils.buildCandidateByXOver(eligibleParents, 
                population, mnt, new int[]{0,1}, 17, 0, gaparams);
        
        Candidate offspring1 = EAUtils.buildCandidateByXOver(eligibleParents, 
                population, mnt, new int[]{0,1}, 17, 1, gaparams);
        
        DGraph g0xo = offspring0.getGraph();
        DGraph g1xo = offspring1.getGraph();
        
        DGraph[] expectedPair = PopulationTest.getPairOfTestGraphsBxoxo();
        DGraph expected0 = expectedPair[0];
        DGraph expected1 = expectedPair[1];
        ((Template)expected0.getVertexAtPosition(1)).setContractLevel(
                ContractLevel.FREE);
        ((Template)expected1.getVertexAtPosition(1)).setContractLevel(
                ContractLevel.FREE);
        
        assertTrue(expected0.sameAs(g0xo, new StringBuilder()));
        assertTrue(expected1.sameAs(g1xo, new StringBuilder()));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testBuildByXOver_Embedded_FixedStructure() throws Exception
    {
        GAParameters gaparams = PopulationTest.prepare();
        Population population = new Population(gaparams);

        DGraph[] pair = PopulationTest.getPairOfTestGraphsB();
        DGraph gA = pair[0];
        DGraph gB = pair[1];
        Template embeddedTmplA = (Template) gA.getVertexAtPosition(1);
        embeddedTmplA.setContractLevel(ContractLevel.FIXED_STRUCT);
        DGraph embeddedGraphA = embeddedTmplA.getInnerGraph();
        Template embeddedTmplB = (Template) gB.getVertexAtPosition(1);
        embeddedTmplB.setContractLevel(ContractLevel.FIXED_STRUCT);
        DGraph embeddedGraphB = embeddedTmplB.getInnerGraph();
        
        // Make vertexes unique so, even though they are empty, they will be 
        // seen as non equal and crossover will be seen as non-redundant.
        String propName = "uniquefier";
        int i=0;
        for (Vertex v : embeddedGraphA.getVertexList())
        {
            v.setUniquefyingProperty(propName);
            v.setProperty(propName, i);
            i++;
        }
        
        Candidate cA = new Candidate("CA",gA);
        population.add(cA);
        Candidate cB = new Candidate("CB",gB);
        population.add(cB);
        ArrayList<Candidate> eligibleParents = new ArrayList<Candidate>();
        eligibleParents.add(cA);
        eligibleParents.add(cB);

        Monitor mnt = new Monitor();
        boolean embeddedGraphHasBeenAlteredA = false;
        boolean embeddedGraphHasBeenAlteredB = false;
        for (int ixo=0; ixo<11; ixo++)
        {
            Candidate offspring0 = null;
            Candidate offspring1 = null;
            try
            {
                offspring0 = EAUtils.buildCandidateByXOver(eligibleParents, 
                        population, mnt, new int[]{0,1}, ixo, 0, gaparams);
                offspring1 = EAUtils.buildCandidateByXOver(eligibleParents, 
                        population, mnt, new int[]{0,1}, ixo, 1, gaparams);
            } catch (IndexOutOfBoundsException e)
            {
                if (e.getMessage().contains("Index 10 out of bounds"))
                {
                    // All good! We intentionally triggered this exception to 
                    // verify that the list of xover points has the right size.
                    break;
                }
                throw e;
            }
            
            DGraph g0xo = offspring0.getGraph();
            DGraph g1xo = offspring1.getGraph();
            Template t0 = null;
            Template t1 = null;
            for (Vertex v : g0xo.getVertexList())
            {
                if (v instanceof Template)
                {
                    t0 = (Template) v;
                    break;
                }
            }
            for (Vertex v : g1xo.getVertexList())
            {
                if (v instanceof Template)
                {
                    t1 = (Template) v;
                    break;
                }
            }
            assertNotNull(t0);
            assertNotNull(t1);
            
            DGraph embeddedGraph0 = t0.getInnerGraph();
            DGraph embeddedGraph1 = t1.getInnerGraph();
            
            // Ensure there has been a change
            if (!embeddedGraphA.isIsomorphicTo(embeddedGraph0) 
                    && !embeddedGraphA.isIsomorphicTo(embeddedGraph1))
            {
                embeddedGraphHasBeenAlteredA = true;
            }
            if (!embeddedGraphB.isIsomorphicTo(embeddedGraph0) 
                    && !embeddedGraphB.isIsomorphicTo(embeddedGraph1))
            {
                embeddedGraphHasBeenAlteredB = true;
            }
            
            // Ensure consistency with "fixed-structure" contract
            assertTrue(embeddedGraphA.isIsostructuralTo(embeddedGraph0)
                    || embeddedGraphB.isIsostructuralTo(embeddedGraph0));
            assertTrue(embeddedGraphA.isIsostructuralTo(embeddedGraph1)
                    || embeddedGraphB.isIsostructuralTo(embeddedGraph1));
        }
        assertTrue(embeddedGraphHasBeenAlteredA);
        assertTrue(embeddedGraphHasBeenAlteredB);
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testCandidateGenerationMethod() throws Exception
    {
        int ix = 0, im=0, ic=0, tot=1000;
        double wx = 2, wm = 0.6, wc=0.05;
        double wtot = wx + wm + wc;
        Randomizer rng = new Randomizer();
        for (int i=0; i<tot; i++)
        {
            CandidateSource mode = EAUtils.pickNewCandidateGenerationMode(
                    wx, wm, wc, rng);
            switch (mode)
            {
                case CROSSOVER:
                    ix++;
                    break;
                case MUTATION:
                    im++;
                    break;
                case CONSTRUCTION:
                    ic++;
                    break;
                default:
                    assertTrue(false,"Unexpected generation mode "+mode);
                    break;
            }
        }
        double x = ((double)ix) / tot;
        double m = ((double)im) / tot;
        double c = ((double)ic) / tot;
        
        double thld = 0.05;
        
        assertTrue(Math.abs(x-(wx/wtot)) < thld, "#Xover cases are off!");
        assertTrue(Math.abs(m-(wm/wtot)) < thld, "#Mutation cases are off!");
        assertTrue(Math.abs(c-(wc/wtot)) < thld, "#Built cases are off!");
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testCandidateGenerationMethodReproducibility() throws Exception
    {
        int tot = 100000;
        long seed = 1234567;
        long otherSeed = 987654321;
        double wx = 2, wm = 0.6, wc=0.05;
        
        Randomizer rng = new Randomizer(seed);
        List<CandidateSource> resultsA = new ArrayList<CandidateSource>();
        for (int i=0; i<tot; i++)
        {
            resultsA.add(EAUtils.pickNewCandidateGenerationMode(wx,wm,wc,rng));
        }
        
        Randomizer rng2 = new Randomizer(otherSeed);
        List<CandidateSource> resultsB = new ArrayList<CandidateSource>();
        for (int i=0; i<tot; i++)
        {
            resultsB.add(EAUtils.pickNewCandidateGenerationMode(wx,wm,wc,rng2));
        }
        
        rng = new Randomizer(seed);
        List<CandidateSource> resultsC = new ArrayList<CandidateSource>();
        for (int i=0; i<tot; i++)
        {
            resultsC.add(EAUtils.pickNewCandidateGenerationMode(wx,wm,wc,rng));
        }
        
        boolean different = false;
        for (int i=0; i<tot; i++)
        {
            if (resultsA.get(i) != resultsB.get(i))
            {
                different = true;
                break;
            }
        }
        assertTrue(different);
        
        for (int i=0; i<tot; i++)
        {
            assertEquals(resultsA.get(i),resultsC.get(i),
                    "Inconsistent sequence of random decisions");
        }
    }
        
//------------------------------------------------------------------------------
	
    @Test
    public void testCrowdingProbability() throws Exception
    {
        DGraph g = DGraphTest.makeTestGraphA();
        double t = 0.001;
        double p = 0.0;
        for (AttachmentPoint ap : g.getAttachmentPoints())
        {
            p = EAUtils.getCrowdingProbability(ap,3,1.0,1.0,1.0);
            assertTrue(Math.abs(1.0 - p)<t,
                    "Scheme 3 should return always 1.0 but was "+p);
        }
        AttachmentPoint ap3 = g.getVertexAtPosition(0).getAP(3);
        p = EAUtils.getCrowdingProbability(ap3,0,1.0,10,1.0);
        assertTrue(Math.abs(1.0 - p)<t, "Scheme 0 on ap3: 1.0 != "+p);
        p = EAUtils.getCrowdingProbability(ap3,1,1.0,10,1.0);
        assertTrue(Math.abs(1.0 - p)<t, "Scheme 1 on ap3: 1.0 != "+p);
        p = EAUtils.getCrowdingProbability(ap3,2,1.0,10,1.0);
        assertTrue(Math.abs(1.0 - p)<t, "Scheme 2 on ap3: 1.0 != "+p);
        
        AttachmentPoint ap2 = g.getVertexAtPosition(0).getAP(2);
        p = EAUtils.getCrowdingProbability(ap2,2,1.0,10,1.0);
        assertTrue(Math.abs(0.5 - p)<t, "Scheme 2 on ap2");
    }
  
//------------------------------------------------------------------------------
    
    @Test
    public void testSelectNonScaffoldNonCapVertex() throws Exception
    {
        DGraph g = new DGraph();
        EmptyVertex s = new EmptyVertex();
        s.addAP();
        s.addAP();
        s.setBuildingBlockType(BBType.SCAFFOLD);
        
        EmptyVertex v = new EmptyVertex();
        v.addAP();
        v.addAP();
        v.addAP();
        v.addAP();
        v.setBuildingBlockType(BBType.FRAGMENT);

        EmptyVertex c1 = new EmptyVertex();
        c1.addAP();
        c1.setBuildingBlockType(BBType.CAP);

        EmptyVertex c2 = new EmptyVertex();
        c2.addAP();
        c2.setBuildingBlockType(BBType.CAP);

        EmptyVertex c3 = new EmptyVertex();
        c3.addAP();
        c3.setBuildingBlockType(BBType.CAP);

        EmptyVertex c4 = new EmptyVertex();
        c4.addAP();
        c4.setBuildingBlockType(BBType.CAP);
        
        g.addVertex(s);
        g.appendVertexOnAP(s.getAP(0), v.getAP(0));
        g.appendVertexOnAP(s.getAP(1), c1.getAP(0));
        g.appendVertexOnAP(v.getAP(1), c2.getAP(0));
        g.appendVertexOnAP(v.getAP(2), c3.getAP(0));
        g.appendVertexOnAP(v.getAP(3), c4.getAP(0));
        
        Randomizer rng = new Randomizer(1234L);
        Vertex expected = v;
        for (int i=0; i<5; i++)
        {
            Vertex chosen = EAUtils.selectNonScaffoldNonCapVertex(g,rng);
            assertEquals(expected, chosen, "Index of the only choosable vertex");
        }
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testChooseNumberOfSitesToMutate() throws Exception
    {
        double[] weights = new double[] {0,0,0,0,1};
        assertEquals(4,EAUtils.chooseNumberOfSitesToMutate(weights,1.0));
        assertEquals(4,EAUtils.chooseNumberOfSitesToMutate(weights,0.00000001));
        
        weights = new double[] {1,0,0,0,1};
        assertEquals(4,EAUtils.chooseNumberOfSitesToMutate(weights,1.0));
        assertEquals(0,EAUtils.chooseNumberOfSitesToMutate(weights,0.00000001));
        
        weights = new double[] {1,1,1,1,1};
        assertEquals(4,EAUtils.chooseNumberOfSitesToMutate(weights,1.0));
        assertEquals(4,EAUtils.chooseNumberOfSitesToMutate(weights,0.800001));
        assertEquals(3,EAUtils.chooseNumberOfSitesToMutate(weights,0.799999));
        assertEquals(3,EAUtils.chooseNumberOfSitesToMutate(weights,0.600001));
        assertEquals(2,EAUtils.chooseNumberOfSitesToMutate(weights,0.599999));
        assertEquals(2,EAUtils.chooseNumberOfSitesToMutate(weights,0.400001));
        assertEquals(1,EAUtils.chooseNumberOfSitesToMutate(weights,0.399999));
        assertEquals(1,EAUtils.chooseNumberOfSitesToMutate(weights,0.200001));
        assertEquals(0,EAUtils.chooseNumberOfSitesToMutate(weights,0.199999));
        assertEquals(0,EAUtils.chooseNumberOfSitesToMutate(weights,0.000001));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testMakeGraphFromFragmentationOfMol() throws Exception
    {
        // We use a hard-coded molecule to ensure the 3D geometry is fixed
        assertTrue(tempDir.isDirectory(),"Should be a directory ");
        String structureFile = tempDir.getAbsolutePath() + SEP + "mol.sdf";
        DenoptimIO.writeData(structureFile,
                "" + NL
                + " OpenBabel03302310043D" + NL
                + "" + NL
                + " 33 35  0  0  0  0  0  0  0  0999 V2000" + NL
                + "   -1.5455    1.4965    3.4529 O   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -1.1783    1.0876    2.3182 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -1.8597   -0.0221    1.6796 N   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -3.0535   -0.6083    2.2978 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "    0.0153    1.7383    1.6547 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -0.1038    1.5947    0.1342 C   0  0  1  0  0  0  0  0  0  0  0  0" + NL
                + "    0.9646    2.2439   -0.5585 O   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "    2.0315    1.3700   -0.8940 C   0  0  2  0  0  0  0  0  0  0  0  0" + NL
                + "    3.0842    2.1330   -1.6910 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "    3.5659    3.1894   -0.9391 F   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "    4.1344    1.2898   -2.0067 F   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "    2.5295    2.6237   -2.8597 F   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "    1.5529    0.2692   -1.6691 O   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "    0.4699   -0.4528   -1.1881 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -0.3192    0.1414   -0.2151 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -1.2683   -0.6053    0.4932 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -1.5222   -1.9320    0.0906 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -0.8092   -2.5038   -0.9766 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -1.1169   -3.8201   -1.3518 O   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -0.4725   -4.5263   -2.4070 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "    0.2040   -1.7556   -1.6127 C   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -3.7666   -0.9444    1.5155 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -3.5865    0.1389    2.9228 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -2.7606   -1.4709    2.9321 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "    0.9449    1.2474    2.0144 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "    0.0550    2.8163    1.9227 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -1.0337    2.1265   -0.1667 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "    2.5075    0.9860    0.0372 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -2.2462   -2.5403    0.6157 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -0.6162   -3.9902   -3.3689 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "   -0.9204   -5.5374   -2.4930 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "    0.6107   -4.6349   -2.1878 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "    0.8117   -2.1861   -2.3969 H   0  0  0  0  0  0  0  0  0  0  0  0" + NL
                + "  1  2  2  0  0  0  0" + NL
                + "  2  3  1  0  0  0  0" + NL
                + "  2  5  1  0  0  0  0" + NL
                + "  3  4  1  0  0  0  0" + NL
                + "  4 22  1  0  0  0  0" + NL
                + "  4 23  1  0  0  0  0" + NL
                + "  4 24  1  0  0  0  0" + NL
                + "  5  6  1  0  0  0  0" + NL
                + "  5 25  1  0  0  0  0" + NL
                + "  5 26  1  0  0  0  0" + NL
                + "  6  7  1  0  0  0  0" + NL
                + "  6 27  1  6  0  0  0" + NL
                + "  7  8  1  0  0  0  0" + NL
                + "  8  9  1  0  0  0  0" + NL
                + "  8 13  1  0  0  0  0" + NL
                + "  8 28  1  1  0  0  0" + NL
                + "  9 10  1  0  0  0  0" + NL
                + "  9 11  1  0  0  0  0" + NL
                + "  9 12  1  0  0  0  0" + NL
                + " 13 14  1  0  0  0  0" + NL
                + " 14 15  2  0  0  0  0" + NL
                + " 15 16  1  0  0  0  0" + NL
                + " 15  6  1  0  0  0  0" + NL
                + " 16 17  2  0  0  0  0" + NL
                + " 16  3  1  0  0  0  0" + NL
                + " 17 18  1  0  0  0  0" + NL
                + " 17 29  1  0  0  0  0" + NL
                + " 18 19  1  0  0  0  0" + NL
                + " 18 21  2  0  0  0  0" + NL
                + " 19 20  1  0  0  0  0" + NL
                + " 20 30  1  0  0  0  0" + NL
                + " 20 31  1  0  0  0  0" + NL
                + " 20 32  1  0  0  0  0" + NL
                + " 21 14  1  0  0  0  0" + NL
                + " 21 33  1  0  0  0  0" + NL
                + "M  END" + NL
                + "$$$$", false);
        IAtomContainer mol = DenoptimIO.readSDFFile(structureFile).get(0);
        GAParameters settings = new GAParameters();
        
        List<CuttingRule> cuttingRules = new ArrayList<CuttingRule>();
        cuttingRules.add(new CuttingRule("cC", "[c]", "[C]", "~", 0, 
                new ArrayList<String>()));
        cuttingRules.add(new CuttingRule("cN", "[c]", "[#7]", "~", 1, 
                new ArrayList<String>()));
        cuttingRules.add(new CuttingRule("cO", "[c]", "[#8]", "~", 2, 
                new ArrayList<String>()));
        cuttingRules.add(new CuttingRule("OC", "[O]", "[C]", "-", 3, 
                new ArrayList<String>()));
        cuttingRules.add(new CuttingRule("CF", "[C]", "[F]", "-", 4, 
                new ArrayList<String>()));
        cuttingRules.add(new CuttingRule("NC", "[N]", "[C]", "-", 5, 
                new ArrayList<String>()));
        
        DGraph graph = EAUtils.makeGraphFromFragmentationOfMol(mol,
                cuttingRules, settings.getLogger(), 
                ScaffoldingPolicy.LARGEST_FRAGMENT);
        
        assertEquals(16, graph.getVertexCount());
        assertEquals(15, graph.getEdgeCount());
        assertEquals(2, graph.getRingCount());
        assertEquals(0, graph.getVertexList()
                .stream()
                .filter(v -> v instanceof Template)
                .count());
        
        DGraph graphWithTemplate = graph.embedPatternsInTemplates(
                GraphPattern.RING, new FragmentSpace());
        
        assertEquals(7, graphWithTemplate.getVertexCount());
        assertEquals(6, graphWithTemplate.getEdgeCount());
        assertEquals(0, graphWithTemplate.getRingCount());
        List<Vertex> templates = graphWithTemplate.getVertexList()
                .stream()
                .filter(v -> v instanceof Template)
                .collect(Collectors.toList());
        assertEquals(1, templates.size());
        Template tmpl = (Template) templates.get(0);
        assertEquals(BBType.SCAFFOLD, tmpl.getBuildingBlockType());
        assertEquals(2, tmpl.getInnerGraph().getRingCount());
    }
    
//------------------------------------------------------------------------------
    
}
