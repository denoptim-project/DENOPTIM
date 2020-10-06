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

import denoptim.molecule.DENOPTIMEdge;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMVertex;

/**
 * Query for searching vertices
 * @author Marco Foscato
 */

public class DENOPTIMVertexQuery
{ 
    /**
     * Query on the vertex properties
     */
    private final DENOPTIMVertex vQuery;
   
    /**
     * Query on the vertex' incoming connections (i.e., vertex id the target)
     */
    private final DENOPTIMEdge eInQuery;

    /**
     * Query on the vertex' outcoming connections (i.e., vertex id the cource)
     */
    private final DENOPTIMEdge eOutQuery;

//------------------------------------------------------------------------------

    /**
     * Constructor from vertex and edge queries
     * @param v the vertex query (filters candidates based on vertex properties)
     */

    public DENOPTIMVertexQuery(DENOPTIMVertex v)
    {
        this.vQuery = v;
        this.eInQuery = null;
        this.eOutQuery = null;
    }

//------------------------------------------------------------------------------

    /**
     * Constructor from vertex and edge queries
     * @param v the vertex query (filters candidates based on vertex properties)
     * @param eIn the edge query (filters candidates based on the connection of
     * a candidate with the rest of the graph) for incoming connections where
     * the candidate vertex if the target.
     */

    public DENOPTIMVertexQuery(DENOPTIMVertex v, DENOPTIMEdge eIn)
    {
        this.vQuery = v;
        this.eInQuery = eIn;
        this.eOutQuery = null;
    }
	
//------------------------------------------------------------------------------

    /**
     * Constructor from vertex and edge queries
     * @param v the vertex query (filters candidates based on vertex properties)
     * @param eIn the edge query (filters candidates based on the connection of
     * a candidate with the rest of the graph) for incoming connections where
     * the candidate vertex if the target.
     * @param eOut the edge query (filters candidates based on the connection of
     * a candidate with the rest of the graph) for incoming connections where
     * the candidate vertex if the source.
     */

    public DENOPTIMVertexQuery(DENOPTIMVertex v, DENOPTIMEdge eIn, 
							     DENOPTIMEdge eOut)
    {
        this.vQuery = v;
        this.eInQuery = eIn;
        this.eOutQuery = eOut;
    }
	
//------------------------------------------------------------------------------

    public DENOPTIMVertex getVrtxQuery()
    {
	return vQuery;
    }

//------------------------------------------------------------------------------

    public DENOPTIMEdge getInEdgeQuery()
    {
        return eInQuery;
    }

//------------------------------------------------------------------------------

    public DENOPTIMEdge getOutEdgeQuery()
    {
        return eOutQuery;
    }

//------------------------------------------------------------------------------

}
