package denoptim.denoptimga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.vecmath.Point3d;

import org.jgrapht.alg.util.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.graph.APClass;
import denoptim.graph.DENOPTIMAttachmentPoint;
import denoptim.graph.DENOPTIMFragment;
import denoptim.graph.DENOPTIMGraph;
import denoptim.graph.DENOPTIMGraphTest;
import denoptim.graph.DENOPTIMRing;
import denoptim.graph.DENOPTIMTemplate;
import denoptim.graph.DENOPTIMTemplate.ContractLevel;
import denoptim.graph.DENOPTIMVertex;
import denoptim.graph.DENOPTIMVertex.BBType;
import denoptim.graph.EmptyVertex;
import denoptim.graph.GraphPattern;
import denoptim.io.DenoptimIO;
import denoptim.graph.DENOPTIMEdge.BondType;
import denoptim.utils.CrossoverType;
import denoptim.utils.GraphUtils;

/**
 * Unit test
 * 
 * @author Marco Foscato
 */

public class XoverSiteTest 
{
    
//------------------------------------------------------------------------------

    @Test
    public void testProjectToClonedGraphs() throws Exception
    {
        DENOPTIMGraphTest.prepareFragmentSpace();
        DENOPTIMGraph gA = DENOPTIMGraphTest.makeDeeplyEmbeddedGraph();
        DENOPTIMGraph gB = DENOPTIMGraphTest.makeDeeplyEmbeddedGraph();
        
        // This works only because we know that the graphs  have only one
        // template per level
        List<DENOPTIMTemplate> pathA = new ArrayList<DENOPTIMTemplate>();
        List<DENOPTIMTemplate> pathB = new ArrayList<DENOPTIMTemplate>();
        DENOPTIMGraph refToThisLayerGraphB = gB;
        DENOPTIMTemplate refToThisLayerVrtxB = null;
        DENOPTIMGraph refToThisLayerGraphA = gA;
        DENOPTIMTemplate refToThisLayerVrtxA = null;
        for (int embeddingLevel=0; embeddingLevel<9; embeddingLevel++)
        {
            refToThisLayerVrtxA = (DENOPTIMTemplate) refToThisLayerGraphA
                    .getVertexList().stream()
                        .filter(v -> v instanceof DENOPTIMTemplate)
                        .findAny()
                        .orElse(null);
            pathA.add(refToThisLayerVrtxA);
            refToThisLayerGraphA = refToThisLayerVrtxA.getInnerGraph();
            
            refToThisLayerVrtxB = (DENOPTIMTemplate) refToThisLayerGraphB
                    .getVertexList().stream()
                        .filter(v -> v instanceof DENOPTIMTemplate)
                        .findAny()
                        .orElse(null);
            pathB.add(refToThisLayerVrtxB);
            refToThisLayerGraphB = refToThisLayerVrtxB.getInnerGraph();
        }
        
        DENOPTIMGraph gEmeddedA = pathA.get(8).getInnerGraph();
        DENOPTIMGraph gEmeddedB = pathB.get(8).getInnerGraph();

        ArrayList<DENOPTIMVertex> lstA = new ArrayList<DENOPTIMVertex>();
        lstA.add(gEmeddedA.getVertexAtPosition(1));
        lstA.add(gEmeddedA.getVertexAtPosition(2));
        ArrayList<DENOPTIMVertex> lstB = new ArrayList<DENOPTIMVertex>();
        lstB.add(gEmeddedB.getVertexAtPosition(1));
        lstB.add(gEmeddedB.getVertexAtPosition(2));
        XoverSite xos = new XoverSite(lstA, lstB, CrossoverType.BRANCH);
        
        XoverSite xosOnClone = xos.projectToClonedGraphs();
        
        assertFalse(xos.getA().get(0)==xosOnClone.getA().get(0));
        assertFalse(xos.getB().get(0)==xosOnClone.getB().get(0));
        
        List<DENOPTIMTemplate> pathSiteA = xos.getA().get(0).getGraphOwner()
                .getEmbeddingPath();
        List<DENOPTIMTemplate> pathClonedSiteA = xosOnClone.getA().get(0)
                .getGraphOwner().getEmbeddingPath();
        assertEquals(pathSiteA.size(),pathClonedSiteA.size());
        List<DENOPTIMTemplate> pathSiteB = xos.getB().get(0).getGraphOwner()
                .getEmbeddingPath();
        List<DENOPTIMTemplate> pathClonedSiteB = xosOnClone.getB().get(0)
                .getGraphOwner().getEmbeddingPath();
        assertEquals(pathSiteB.size(),pathClonedSiteB.size());
        
    }

//------------------------------------------------------------------------------
}