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

package denoptim.utils;

import denoptim.graph.AttachmentPoint;
import denoptim.graph.DGraph;
import denoptim.graph.EdgeQuery;
import denoptim.graph.VertexQuery;


/**
 * Definition of a graph editing task.
 *
 * @author Marco Foscato
 */

public class GraphEdit 
{
    /**
     * Type of editing task
     */
    private EditTask task = null;

    /**
     * Query identifying the vertex that is the center of our attention when
     * performing the graph editing task. 
     * Depending on the type of task the vertex in
     * may or may not be altered. For example, if the task is 
     * {@link EditTask#DELETEVERTEX}, then the vertex in focus is the vertex 
     * that will be deleted.
     */
    private VertexQuery vertexQuery = null;

    /**
     * Query identifying the edge that is the center of our attention when
     * performing the graph editing task.
     */
    private EdgeQuery edgeQuery = null;
    
    /**
     * The incoming graph for tasks that involve appending a subgraph onto
     * another graph.
     */
    private DGraph incomingGraph = null;
    
    /**
     * The identifier of the {@link AttachmentPoint} (AP) of the 
     * {@link GraphEdit#incomingGraph} when attaching such graph to the
     * graph to edit. NB: this is the unique identifier, not any index on a 
     * list.
     */
    private Integer idAPOnIncomingGraph = null;
    

    public static enum EditTask {REPLACECHILD, DELETEVERTEX}

//------------------------------------------------------------------------------
    
    public Integer getIncomingAPId()
    {
        return idAPOnIncomingGraph;
    }

//------------------------------------------------------------------------------
    
    public void setAP(int apId)
    {
        this.idAPOnIncomingGraph = apId;
    }

//------------------------------------------------------------------------------

    public void setIncomingGraph(DGraph incomingGraph)
    {
        this.incomingGraph = incomingGraph;
    }
    
//------------------------------------------------------------------------------
    
    public DGraph getIncomingGraph()
    {
        return incomingGraph;
    }
    
//------------------------------------------------------------------------------

    public GraphEdit(EditTask task)
    {
        this.task = task;
    }

//------------------------------------------------------------------------------

    public EditTask getType() {
        return task;
    }

//------------------------------------------------------------------------------

    public VertexQuery getVertexQuery() {
        return vertexQuery;
    }

//------------------------------------------------------------------------------
      
    public void setVertexQuery(VertexQuery vertexQuery)
    {
        this.vertexQuery = vertexQuery;
    }

//------------------------------------------------------------------------------

    public void setEdgeQuery(EdgeQuery edgeQuery)
    {
        this.edgeQuery = edgeQuery;
    }

//------------------------------------------------------------------------------

    public EdgeQuery getEdgeQuery() {
        return edgeQuery;
    }

//------------------------------------------------------------------------------

}