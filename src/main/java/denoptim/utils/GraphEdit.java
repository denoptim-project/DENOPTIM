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

import java.util.LinkedHashMap;

import denoptim.fragspace.FragmentSpace;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.DGraph;
import denoptim.graph.EdgeQuery;
import denoptim.graph.Vertex.BBType;
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
     * Depending on the type of task the vertex it
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
     * another graph (when doing {@link EditTask#REPLACECHILD}).
     */
    private DGraph incomingGraph = null;
    
    /**
     * The identifier of the {@link AttachmentPoint} (AP) of the 
     * {@link GraphEdit#incomingGraph} when attaching such graph to the
     * graph to edit. NB: this is the unique identifier, not any index on a 
     * list.
     */
    private Integer idAPOnIncomingGraph = null;

    /**
     * Index of the building block to use as incoming vertex when performing
     * {@link EditTask#CHANGEVERTEX}. The index refers to the position of the 
     * building blocks in a {@link FragmentSpace} that is expected to be 
     * unambiguously identifiable (there should be only one available).
     */
    private int incomingBBId = -1;

    /**
     * The type of the building block to use as incoming vertex when performing
     * {@link EditTask#CHANGEVERTEX}. The building blocks in expected to
     * be part of a {@link FragmentSpace} that is expected to be 
     * unambiguously identifiable (there should be only one available).
     */
    private BBType incomingBBTyp;

    /**
     * Mapping of {@link AttachmentPoint}s between the current (first entry) and
     * the incoming vertices (second entry) to be enforced when performing
     * {@link EditTask#CHANGEVERTEX}. Values are indexes of APs in the
     * respective AP lists.
     */
    private LinkedHashMap<Integer, Integer> incomingAPMap;
    
    /**
     * Defined the kind of graph editing task.
     */
    public static enum EditTask {
        /** 
         * Replaces any child (or tree of children) of any vertex matching the 
         * vertex query with a given incoming graph that may contain one or 
         * more vertexes.
         */
        REPLACECHILD, 
        
        /**
         * Removes any matching vertex.
         */
        DELETEVERTEX, 
        
        /**
         * Changes any vertex matching the vertex query with the vertex given 
         * as input and using the given AP mapping mask.
         */
        CHANGEVERTEX
    }

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

    public EditTask getType() 
    {
        return task;
    }

//------------------------------------------------------------------------------

    public VertexQuery getVertexQuery() 
    {
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

    public EdgeQuery getEdgeQuery() 
    {
        return edgeQuery;
    }
    
//------------------------------------------------------------------------------

    public int getIncomingBBId()
    {
        return incomingBBId;
    }

//------------------------------------------------------------------------------

    public BBType getIncomingBBType()
    {
        return incomingBBTyp;
    }

//------------------------------------------------------------------------------

    public LinkedHashMap<Integer, Integer> getAPMappig()
    {
        return incomingAPMap;
    }

//------------------------------------------------------------------------------

}