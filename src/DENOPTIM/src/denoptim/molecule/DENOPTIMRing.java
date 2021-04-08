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
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSerializationContext;

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
    private ArrayList<DENOPTIMVertex> verteces;

    /**
     * Bond type (i.e., bond order) to be used between head and tail vertices
     */
    private BondType bndTyp = BondType.UNDEFINED; 

//------------------------------------------------------------------------------

    public DENOPTIMRing()
    {
        verteces = new ArrayList<DENOPTIMVertex>();
    }
    
//------------------------------------------------------------------------------
    
    public DENOPTIMRing(ArrayList<DENOPTIMVertex> m_verteces)
    {
        verteces = m_verteces;
    }
    
//------------------------------------------------------------------------------

    /**
     * Append a <code>DENOPTIMVertex</code> to the list
     */

    public void addVertex(DENOPTIMVertex v)
    {
        verteces.add(v);
    }
    
//------------------------------------------------------------------------------

    /**
     * @return the first vertex in the list
     * of <code>DENOPTIMVertex</code>s involved in this ring
     */

    public DENOPTIMVertex getHeadVertex()
    {
        return verteces.get(0);
    }

//------------------------------------------------------------------------------

    /**
     * @return the last vertex in the list
     * of <code>DENOPTIMVertex</code>s involved in this ring
     */

    public DENOPTIMVertex getTailVertex()
    {
        return verteces.get(verteces.size() - 1);
    }

//------------------------------------------------------------------------------

    /**
     * @return the vertex at the given position in the list
     * of <code>DENOPTIMVertex</code>s involved in this ring
     */

    public DENOPTIMVertex getVertexAtPosition(int i)
    {
	if (i>=verteces.size() || i<0)
	{
	    return null;
	}
        return verteces.get(i);
    }

//------------------------------------------------------------------------------

    /**
     * @return the size of the list
     * of <code>DENOPTIMVertex</code>s involved in this ring
     */

    public int getSize()
    {
        return verteces.size();
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
        return verteces.contains(v);
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
	for (DENOPTIMVertex v : verteces)
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
    public String toString()
    {
        return "DENOPTIMRing [verteces=" + verteces + "]";
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
        return jsonObject;
    }
  }

//------------------------------------------------------------------------------



}
