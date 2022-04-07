package denoptim.ga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import denoptim.ga.XoverSite;
import denoptim.graph.DENOPTIMAttachmentPoint;
import denoptim.graph.DENOPTIMGraph;
import denoptim.graph.DENOPTIMGraphTest;
import denoptim.graph.DENOPTIMTemplate;
import denoptim.graph.DENOPTIMVertex;
import denoptim.utils.CrossoverType;

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
        DENOPTIMGraphTest.prepare();
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

        List<DENOPTIMVertex> lstA = new ArrayList<DENOPTIMVertex>();
        lstA.add(gEmeddedA.getVertexAtPosition(1));
        lstA.add(gEmeddedA.getVertexAtPosition(2));
        List<DENOPTIMAttachmentPoint> lstNeedyAPsA =
                new ArrayList<DENOPTIMAttachmentPoint>();
        lstNeedyAPsA.add(lstA.get(0).getAP(2));
        List<DENOPTIMVertex> lstB = new ArrayList<DENOPTIMVertex>();
        lstB.add(gEmeddedB.getVertexAtPosition(1));
        lstB.add(gEmeddedB.getVertexAtPosition(2));
        List<DENOPTIMAttachmentPoint> lstNeedyAPsB =
                new ArrayList<DENOPTIMAttachmentPoint>();
        lstNeedyAPsB.add(lstB.get(0).getAP(2));
        
        XoverSite xos = new XoverSite(lstA, lstNeedyAPsA, lstB, lstNeedyAPsB, 
                CrossoverType.BRANCH);
        
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
        
        assertEquals(xosOnClone.getA().get(0).getAP(2),
                xosOnClone.getAPsNeedingMappingA().get(0));
        assertEquals(xosOnClone.getB().get(0).getAP(2),
                xosOnClone.getAPsNeedingMappingB().get(0));
    }

//------------------------------------------------------------------------------
}