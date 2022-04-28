package denoptim.utils;

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

import org.jgrapht.graph.DefaultUndirectedGraph;
import org.junit.jupiter.api.Test;

import denoptim.ga.PopulationTest;
import denoptim.graph.DGraph;
import denoptim.graph.Template;
import denoptim.graph.simplified.Node;
import denoptim.graph.simplified.NodeConnection;

/**
 * Unit test for GraphConversionTool
 * 
 * @author Marco Foscato
 */

public class GraphConversionToolTest
{
	
//------------------------------------------------------------------------------
    
    @Test
    public void testGetJGraphKernelFromGraph() throws Exception
    {
        PopulationTest.prepare();
        DGraph[] pair = PopulationTest.getPairOfTestGraphsB();
        DGraph gA = pair[0];
        DGraph gB = pair[1];
        DGraph gC = ((Template) gA.getVertexAtPosition(1))
                .getInnerGraph();
        DGraph gD = ((Template) gB.getVertexAtPosition(1))
                .getInnerGraph();
        
        DefaultUndirectedGraph<Node, NodeConnection> gkA = 
                GraphConversionTool.getJGraphKernelFromGraph(gA);
        assertEquals(5,gkA.vertexSet().size());
        assertEquals(4,gkA.edgeSet().size());
        
        DefaultUndirectedGraph<Node, NodeConnection> gkB = 
                GraphConversionTool.getJGraphKernelFromGraph(gB);
        assertEquals(4,gkB.vertexSet().size());
        assertEquals(3,gkB.edgeSet().size());
        
        DefaultUndirectedGraph<Node, NodeConnection> gkC = 
                GraphConversionTool.getJGraphKernelFromGraph(gC);
        assertEquals(8,gkC.vertexSet().size());
        assertEquals(8,gkC.edgeSet().size());
        
        DefaultUndirectedGraph<Node, NodeConnection> gkD = 
                GraphConversionTool.getJGraphKernelFromGraph(gD);
        assertEquals(8,gkD.vertexSet().size());
        assertEquals(7,gkD.edgeSet().size());
        
    }
    
//------------------------------------------------------------------------------

}
