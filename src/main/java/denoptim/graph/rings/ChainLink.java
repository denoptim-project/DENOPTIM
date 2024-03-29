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

package denoptim.graph.rings;

import denoptim.graph.Vertex;

/**
 * ChainLink represents a vertex in a closable chain.
 * This data structure stores information as to the corresponding
 * molecule ID in the library of fragments, the type of fragment 
 * and the identify of the AP linking to the chain link before and after
 *
 * @author Marco Foscato 
 */

public class ChainLink implements Cloneable
{
    /**
     * Fragment ID in library
     */
    private int idx; 

    /**
     * Fragment type
     */
    private Vertex.BBType ftype;

    /**
     * Index of AP towards left
     */
    private int apLeft;

    /**
     * Index of AP towards right
     */
    private int apRight;


//-----------------------------------------------------------------------------

    /**
     *  Constructs an empty ChainLink
     */

    public ChainLink()
    {
    }

//-----------------------------------------------------------------------------

    /**
     *  Constructs a ChainLink from the involved points
     */

    public ChainLink(int molID, Vertex.BBType ftype, int apLeft, int apRight)
    {
        this.idx = molID;
        this.ftype = ftype;
        this.apLeft = apLeft;
        this.apRight = apRight;
    }

//-----------------------------------------------------------------------------

    /**
     * @return the index of the fragment in the fragment library
     */

    public int getIdx()
    {
        return idx;
    }

//-----------------------------------------------------------------------------

    /**
     * @return the type of fragment library
     */

    public Vertex.BBType getFragType()
    {
        return ftype;
    }

//-----------------------------------------------------------------------------

    /**
     * @return the index of the attachment point directed to the left side
     * of the chain from this link
     */
    
    public int getApIdToLeft()
    {
        return apLeft;
    }

//-----------------------------------------------------------------------------

    /**
     * @return the index of the attachment point directed to the right side
     * of the chain from this link
     */

    public int getApIdToRight()
    {
        return apRight;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns a deep-clone
     * @return a deep-clone
     */
    public ChainLink clone()
    {
        ChainLink c = new ChainLink(idx, ftype, apLeft, apRight);
        return c;
    }
    
//-----------------------------------------------------------------------------

    /**
     * @return the string representation of this ChainLink
     */

    public String toString()
    {
        String str = " ChainLink[molID: " + idx + ", ftype: " + ftype
		      + ", apLeft: " + apLeft + ", apRight: " + apRight + "]";
        return str;
    }

//-----------------------------------------------------------------------------
}

