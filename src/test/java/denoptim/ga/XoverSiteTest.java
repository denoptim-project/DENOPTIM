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
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import denoptim.graph.AttachmentPoint;
import denoptim.graph.DGraph;
import denoptim.graph.DGraphTest;
import denoptim.graph.Template;
import denoptim.graph.Vertex;
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
        DGraphTest.prepare();
        DGraph gA = DGraphTest.makeDeeplyEmbeddedGraph();
        DGraph gB = DGraphTest.makeDeeplyEmbeddedGraph();
        
        // This works only because we know that the graphs  have only one
        // template per level
        List<Template> pathA = new ArrayList<Template>();
        List<Template> pathB = new ArrayList<Template>();
        DGraph refToThisLayerGraphB = gB;
        Template refToThisLayerVrtxB = null;
        DGraph refToThisLayerGraphA = gA;
        Template refToThisLayerVrtxA = null;
        for (int embeddingLevel=0; embeddingLevel<9; embeddingLevel++)
        {
            refToThisLayerVrtxA = (Template) refToThisLayerGraphA
                    .getVertexList().stream()
                        .filter(v -> v instanceof Template)
                        .findAny()
                        .orElse(null);
            pathA.add(refToThisLayerVrtxA);
            refToThisLayerGraphA = refToThisLayerVrtxA.getInnerGraph();
            
            refToThisLayerVrtxB = (Template) refToThisLayerGraphB
                    .getVertexList().stream()
                        .filter(v -> v instanceof Template)
                        .findAny()
                        .orElse(null);
            pathB.add(refToThisLayerVrtxB);
            refToThisLayerGraphB = refToThisLayerVrtxB.getInnerGraph();
        }
        
        DGraph gEmeddedA = pathA.get(8).getInnerGraph();
        DGraph gEmeddedB = pathB.get(8).getInnerGraph();

        List<Vertex> lstA = new ArrayList<Vertex>();
        lstA.add(gEmeddedA.getVertexAtPosition(1));
        lstA.add(gEmeddedA.getVertexAtPosition(2));
        List<AttachmentPoint> lstNeedyAPsA =
                new ArrayList<AttachmentPoint>();
        lstNeedyAPsA.add(lstA.get(0).getAP(2));
        List<Vertex> lstB = new ArrayList<Vertex>();
        lstB.add(gEmeddedB.getVertexAtPosition(1));
        lstB.add(gEmeddedB.getVertexAtPosition(2));
        List<AttachmentPoint> lstNeedyAPsB =
                new ArrayList<AttachmentPoint>();
        lstNeedyAPsB.add(lstB.get(0).getAP(2));
        
        XoverSite xos = new XoverSite(lstA, lstNeedyAPsA, lstB, lstNeedyAPsB, 
                CrossoverType.BRANCH);
        
        XoverSite xosOnClone = xos.projectToClonedGraphs();
        
        assertFalse(xos.getA().get(0)==xosOnClone.getA().get(0));
        assertFalse(xos.getB().get(0)==xosOnClone.getB().get(0));
        
        List<Template> pathSiteA = xos.getA().get(0).getGraphOwner()
                .getEmbeddingPath();
        List<Template> pathClonedSiteA = xosOnClone.getA().get(0)
                .getGraphOwner().getEmbeddingPath();
        assertEquals(pathSiteA.size(),pathClonedSiteA.size());
        List<Template> pathSiteB = xos.getB().get(0).getGraphOwner()
                .getEmbeddingPath();
        List<Template> pathClonedSiteB = xosOnClone.getB().get(0)
                .getGraphOwner().getEmbeddingPath();
        assertEquals(pathSiteB.size(),pathClonedSiteB.size());
        
        assertEquals(xosOnClone.getA().get(0).getAP(2),
                xosOnClone.getAPsNeedingMappingA().get(0));
        assertEquals(xosOnClone.getB().get(0).getAP(2),
                xosOnClone.getAPsNeedingMappingB().get(0));
    }

//------------------------------------------------------------------------------
}