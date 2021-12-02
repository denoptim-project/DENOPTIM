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

package denoptim.molecule;

import java.io.Serializable;
import java.util.ArrayList;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSerializationContext;

import denoptim.exception.DENOPTIMException;
import denoptim.molecule.DENOPTIMEdge.BondType;

/**
 * This class represents the closure of a ring in a spanning tree
 *
 * @author Marco Foscato
 */
public class DENOPTIMRing implements Serializable 
{

    /**
     * List of <code>DENOPTIMVertex</code> involved in the ring. 
     */
    private List<DENOPTIMVertex> vertices;

    /**
     * Bond type (i.e., bond order) to be used between head and tail vertices
     */
    private BondType bndTyp = BondType.UNDEFINED; 

//------------------------------------------------------------------------------

    public DENOPTIMRing()
    {
        vertices = new ArrayList<>();
    }
    
//------------------------------------------------------------------------------
    
    public DENOPTIMRing(List<DENOPTIMVertex> vertices)
    {
        this.vertices = vertices;
    }
    
//------------------------------------------------------------------------------

    /**
     * Append a <code>DENOPTIMVertex</code> to the list
     */

    public void addVertex(DENOPTIMVertex v)
    {
        vertices.add(v);
    }
    
//------------------------------------------------------------------------------

    /**
     * @return the first vertex in the list
     * of <code>DENOPTIMVertex</code>s involved in this ring
     */

    public DENOPTIMVertex getHeadVertex()
    {
        return vertices.get(0);
    }

//------------------------------------------------------------------------------

    /**
     * @return the last vertex in the list
     * of <code>DENOPTIMVertex</code>s involved in this ring
     */

    public DENOPTIMVertex getTailVertex()
    {
        return vertices.get(vertices.size() - 1);
    }

//------------------------------------------------------------------------------

    /**
     * @return the vertex at the given position in the list
     * of <code>DENOPTIMVertex</code>s involved in this ring
     */

    public DENOPTIMVertex getVertexAtPosition(int i)
    {
	if (i>= vertices.size() || i<0)
	{
	    return null;
	}
        return vertices.get(i);
    }

//------------------------------------------------------------------------------
    
    /**
     * Returns the index of the first occurrence of the specified element in 
     * this ring, or -1 if this list does not contain the element.
     */
    public int indexOf(DENOPTIMVertex v)
    {
        return vertices.indexOf(v);
    }
    
//------------------------------------------------------------------------------

    /**
     * @return the size of the list
     * of <code>DENOPTIMVertex</code>s involved in this ring
     */

    public int getSize()
    {
        return vertices.size();
    }

//------------------------------------------------------------------------------

    /**
     * @return the bond type (i.e., bond order) of the chord connecting
     * the head and the tail vertices
     */

    public BondType getBondType()
    {
        return bndTyp;
    }

//------------------------------------------------------------------------------

    /**
     * Set the bond type (i.e., bond order) of the chord connecting
     * the head and the tail vertices
     */

    public void setBondType(BondType bndType)
    {
        this.bndTyp = bndType;
    }

//------------------------------------------------------------------------------

    /**
     * Checks whether a given vertex is part of this ring.
     * @param v the candidate <code>DENOPTIMVertex</code>
     * @return <code>true</code> if the given vertex is contained in the list
     * of <code>DENOPTIMVertex</code>s involved in this ring
     */

    public boolean contains(DENOPTIMVertex v)
    {
        return vertices.contains(v);
    }

//------------------------------------------------------------------------------

    /**
     * Checks whether a given vertex is part of this ring.
     * @param vid the ID of the candidate <code>DENOPTIMVertex</code>
     * @return <code>true</code> if the list
     * of <code>DENOPTIMVertex</code>s involved in this ring contains the given
     * vertex ID
     */

    public boolean containsID(int vid)
    {
	boolean result = false;
	for (DENOPTIMVertex v : vertices)
	{
	    if (v.getVertexId() == vid)
	    {
		result = true;
		break;
	    }
	}
	return result;
    }
    
//------------------------------------------------------------------------------

    /**
     * @return the string representation of this ring
     */

    @Override
    public String toString() {
        return "DENOPTIMRing [vertices=" + vertices + "]";
    }

//------------------------------------------------------------------------------

    public List<DENOPTIMVertex> getVertices() {
        return Collections.unmodifiableList(vertices);
    }

//------------------------------------------------------------------------------

    public static class DENOPTIMRingSerializer
    implements JsonSerializer<DENOPTIMRing>
    {
        @Override
        public JsonElement serialize(DENOPTIMRing ring, Type typeOfSrc,
              JsonSerializationContext context)
        {
            JsonObject jsonObject = new JsonObject();
            ArrayList<Integer> vertexIDs = new ArrayList<Integer>();
            for (int i=0; i<ring.getSize(); i++)
            {
                DENOPTIMVertex v = ring.getVertexAtPosition(i);
                vertexIDs.add(v.getVertexId());
            }
            jsonObject.add("vertices",context.serialize(vertexIDs));
            jsonObject.add("bndTyp",context.serialize(ring.getBondType()));
            return jsonObject;
        }
    }
    
//------------------------------------------------------------------------------
    
    public void removeVertex(DENOPTIMVertex oldVrtx)
    {
        int idx = vertices.indexOf(oldVrtx);
        vertices.remove(idx);
    }

//------------------------------------------------------------------------------
    
    /**
     * Replaces a vertex that belong to this ring with a new one.
     * @param oldVrtx the vertex to be replaced.
     * @param newVrtx the vertex the replace the old one with.
     */
    public void replaceVertex(DENOPTIMVertex oldVrtx, DENOPTIMVertex newVrtx)
    {
        int idx = vertices.indexOf(oldVrtx);
        vertices.set(idx, newVrtx);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Adds a vertex to the ring, and in between two defined vertexes.
     * @param newLink vertex to add to this ring.
     * @param vA one of the vertexes in between which the new vertex should
     * in inserted.
     * @param vB one of the vertexes in between which the new vertex should
     * in inserted.
     * @return <code>true</code> if the vertex is inserted, or 
     * <code>false</code> if the operation cannot be performed for any reason,
     * e.g., the vertex is already contained in this ring, or either of the two
     * reference vertexes are not themselves contained here.
     */
    public boolean insertVertex(DENOPTIMVertex newLink, DENOPTIMVertex vA,
            DENOPTIMVertex vB)
    {
        if (this.contains(newLink) || !this.contains(vA) || !this.contains(vB))
            return false;
        
        int idA = vertices.indexOf(vA);
        int idB = vertices.indexOf(vB);
        if (idA < idB)
        {
            vertices.add(idB,newLink);
        } else {
            vertices.add(idA,newLink);
        }
        return true;
    }

//------------------------------------------------------------------------------

    /**
     * Measures how many edges there are between two edges along the sequence of
     * vertexes that defined this fundamental ring.
     * @param v1
     * @param v2
     * @return the distance between the vertexes (i.e., number of edges).
     * @throws DENOPTIMException if either vertex is not part of this ring.
     */
    public int getDistance(DENOPTIMVertex v1, DENOPTIMVertex v2) throws DENOPTIMException
    {
        if (!contains(v1))
            throw new DENOPTIMException("Cannot measure distance along ring "
                    + "because " + v1 + " does not belong ring " + this);
        if (!contains(v2))
            throw new DENOPTIMException("Cannot measure distance along ring "
                    + "because " + v2 + " does not belong ring " + this);
        
        int pV1 = vertices.indexOf(v1);
        int pV2 = vertices.indexOf(v2);
        
        return Math.max(pV1,pV2) - Math.min(pV1, pV2);
    }

//------------------------------------------------------------------------------


}
