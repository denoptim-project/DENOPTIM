/*
 *   DENOPTIM
 *   Copyright (C) 2026 Marco Foscato <marco.foscato@uib.no>
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

import denoptim.graph.APClass;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.Edge;
import denoptim.graph.EdgeQuery;
import denoptim.graph.Vertex;
import denoptim.graph.VertexQuery;

/**
 * Query for searching {@link AttachmentPoint}s.
 */

public class AttachmentPointQuery 
{
    /**
     * Query on the unique identifier of the target attachment point, or null.
     */
    private Long attachmentPointId = null;

    /**
     * Query on the index of the target attachment point, or null.
     */
    private Integer attachmentPointIndex = null;

    /**
     * Query on the {@link APClass} of the target attachment point, or null.
     */
    private APClass apClass = null;

    /**
     * Query on vertex owning the target attachment point, or null.
     */
    private VertexQuery vertexQuery = null;

    /**
     * Query on the {@link Edge} using the target attachment point, or null.
     */
    private EdgeQuery edgeQuery = null;
    

//------------------------------------------------------------------------------

    public AttachmentPointQuery(Long attachmentPointId, 
        Integer attachmentPointIndex, APClass apClass, VertexQuery vertexQuery, 
        EdgeQuery edgeQuery)
    {
        this.attachmentPointId = attachmentPointId;
        this.attachmentPointIndex = attachmentPointIndex;
        this.apClass = apClass;
        this.vertexQuery = vertexQuery;
        this.edgeQuery = edgeQuery;
    }

//------------------------------------------------------------------------------

    /**
     * Tests whether the given attachment point satisfies all non-null criteria
     * in this query.
     * @param ap the attachment point to test
     * @return {@code true} if the attachment point matches, {@code false}
     * otherwise
     */
    public boolean matches(AttachmentPoint ap)
    {
        if (attachmentPointId != null && ap.getID() != attachmentPointId)
        {
            return false;
        }

        if (attachmentPointIndex != null
                && ap.getIndexInOwner() != attachmentPointIndex)
        {
            return false;
        }

        if (apClass != null)
        {
            if (ap.getAPClass() == null || !ap.getAPClass().equals(apClass))
            {
                return false;
            }
        }

        if (vertexQuery != null)
        {
            Vertex owner = ap.getOwner();
            if (owner == null
                    || !vertexQuery.matches(owner))
            {
                return false;
            }
        }

        if (edgeQuery != null)
        {
            Edge user = ap.getEdgeUser();
            if (user == null || !edgeQuery.matches(user))
            {
                return false;
            }
        }

        return true;
    }

//------------------------------------------------------------------------------
}
