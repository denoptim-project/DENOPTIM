/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no>
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

package denoptim.graph;

import denoptim.graph.Edge.BondType;
import denoptim.utils.AttachmentPointQuery;

/**
 * A query for edges: a list of properties that target edges should possess in 
 * order to match this query.
 */
public class EdgeQuery
{
    /** 
     * Query for src vertex.
     */
    private VertexQuery srcVertexQuery = null;

    /**
     * Query for trg vertex.
     */
    private VertexQuery trgVertexQuery = null;

    /**
     * Query for src AP.
     */
    private AttachmentPointQuery srcAPQuery = null;

    /**
     * Query for trg AP.
     */
    private AttachmentPointQuery trgAPQuery = null;

    /**
     * The bond type associated with the connection between the fragments
     */
    private BondType bondType = null;
    
//------------------------------------------------------------------------------

    /**
     * Constructor from nested vertex and attachment point queries.
     * @param srcVertexQuery query for the source vertex, or null
     * @param trgVertexQuery query for the target vertex, or null
     * @param srcAPQuery query for the source attachment point, or null
     * @param trgAPQuery query for the target attachment point, or null
     * @param bondType query for the bond type, or null
     */
    public EdgeQuery(VertexQuery srcVertexQuery, VertexQuery trgVertexQuery,
            AttachmentPointQuery srcAPQuery, AttachmentPointQuery trgAPQuery,
            BondType bondType)
    {
        this.srcVertexQuery = srcVertexQuery;
        this.trgVertexQuery = trgVertexQuery;
        this.srcAPQuery = srcAPQuery;
        this.trgAPQuery = trgAPQuery;
        this.bondType = bondType;
    }

//------------------------------------------------------------------------------

    /**
     * Builds an edge query from the legacy flat parameters.
     */
    public static EdgeQuery make(Long srcVertexId, Long trgVertexId,
            Integer srcAPIdx, Integer trgAPIdx,
            BondType bondType, APClass srcAPC, APClass trgAPC)
    {
        VertexQuery srcVq = srcVertexId == null ? null
                : new VertexQuery(srcVertexId, null, null, null, null, null);
        VertexQuery trgVq = trgVertexId == null ? null
                : new VertexQuery(trgVertexId, null, null, null, null, null);
        AttachmentPointQuery srcApQ = (srcAPIdx == null && srcAPC == null)
                ? null
                : new AttachmentPointQuery(null, srcAPIdx, srcAPC, null, null);
        AttachmentPointQuery trgApQ = (trgAPIdx == null && trgAPC == null)
                ? null
                : new AttachmentPointQuery(null, trgAPIdx, trgAPC, null, null);
        return new EdgeQuery(srcVq, trgVq, srcApQ, trgApQ, bondType);
    }
    
//------------------------------------------------------------------------------

    /**
     * Tests whether the given edge satisfies this query.
     * @param e the edge to test
     * @return {@code true} if the edge matches, {@code false} otherwise
     */
    public boolean matches(Edge e)
    {
        if (e == null)
        {
            return false;
        }

        if (bondType != null && e.getBondType() != bondType)
        {
            return false;
        }

        if (srcVertexQuery != null
                && !srcVertexQuery.matches(e.getSrcAP().getOwner()))
        {
            return false;
        }

        if (trgVertexQuery != null
                && !trgVertexQuery.matches(e.getTrgAP().getOwner()))
        {
            return false;
        }

        if (srcAPQuery != null && !srcAPQuery.matches(e.getSrcAP()))
        {
            return false;
        }

        if (trgAPQuery != null && !trgAPQuery.matches(e.getTrgAP()))
        {
            return false;
        }

        return true;
    }

//------------------------------------------------------------------------------    
}
