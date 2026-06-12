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

package denoptim.graph;

/**
 * Query for searching {@link AttachmentPoint}s.
 */
public class AttachmentPointQuery 
{
    /**
     * Query on the unique identifier of the target attachment point, or null.
     */
    private Long apID = null;

    /**
     * Query on the index of the target attachment point, or null.
     */
    private Integer apIndex = null;

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

    /**
     * Query defining the attachment point linked by an edge to the target
     * attachment point, or null.
     */
    private AttachmentPointQuery linkedAPQuery = null;

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty query.
     */
    public AttachmentPointQuery()
    {
    }

//------------------------------------------------------------------------------

    /**
     * Constructor from individual criteria.
     * @param apID the query on the unique identifier of the target attachment point, or null.
     * @param apIndex the query on the index of the target attachment point, or null.
     * @param apClass the query on the {@link APClass} of the target attachment point, or null.
     * @param vertexQuery the query on the vertex owning the target attachment point, or null.
     * @param edgeQuery the query on the {@link Edge} using the target attachment point, or null.
     * @param linkedAPQuery the query on the attachment point linked by an edge to the target attachment point, or null.
     */
    public AttachmentPointQuery(Long apID, 
        Integer apIndex, APClass apClass, VertexQuery vertexQuery, 
        EdgeQuery edgeQuery, AttachmentPointQuery linkedAPQuery)
    {
        this.apID = apID;
        this.apIndex = apIndex;
        this.apClass = apClass;
        this.vertexQuery = vertexQuery;
        this.edgeQuery = edgeQuery;
        this.linkedAPQuery = linkedAPQuery;
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
        if (apID != null && ap.getID() != apID)
        {
            return false;
        }

        if (apIndex != null
                && ap.getIndexInOwner() != apIndex)
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

        if (linkedAPQuery != null)
        {
            AttachmentPoint linkedAP = ap.getLinkedAP();
            if (linkedAP == null || !linkedAPQuery.matches(linkedAP))
            {
                return false;
            }
        }
        return true;
    }

//------------------------------------------------------------------------------
}
