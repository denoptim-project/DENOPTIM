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


/**
 * This class represents a subgraph feature that defined the structure of a 
 * graph. Namely, it represents either a {@link DENOPTIMVertex}
 * of a {@link DENOPTIMAttachmentPoint} that needs to exist for the structure 
 * of a subgraph to be retained.
 */

public class Node 
{
    /**
     * Reference to the vertex we might be representing
     */
    private DENOPTIMVertex vertex = null;
    
    /**
     * Invariant representation used to compare
     */
    public String invariant = null;

    /**
     * Property if {@link DENOPTIMVertex} used to store the reference to the
     * corresponding {@link Node}.
     */
    public static final String REFTOVERTEXKERNEL = "REFTOVERTEXKERNEL";
    
//------------------------------------------------------------------------------
    
    public Node(DENOPTIMVertex v) {
        this.vertex = v;
        v.setProperty(REFTOVERTEXKERNEL, this);
        if (v.isRCV())
            this.invariant = v.getClass().getSimpleName() + "RCV";
        else
            this.invariant = v.getClass().getSimpleName();
    }
    
//------------------------------------------------------------------------------
    
    public Node(DENOPTIMAttachmentPoint ap) {
        this.vertex = null;
        this.invariant = ap.getAPClass().toString();
    }
    
//------------------------------------------------------------------------------
    
    public DENOPTIMVertex getDNPVertex()
    {
        return vertex;
    }

//------------------------------------------------------------------------------
 
    public int compare(Node other)
    {
        if (this.vertex==null && other.vertex!=null)
            return -1;
        else if (this.vertex!=null && other.vertex==null)
            return +1;
        
        return this.invariant.compareTo(other.invariant);
    }

//------------------------------------------------------------------------------

    @Override
    public String toString()
    {
        return invariant;
    }

//------------------------------------------------------------------------------

}
