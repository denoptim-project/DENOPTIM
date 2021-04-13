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

package denoptim.molecule;

import java.io.Serializable;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSerializationContext;

import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IBond.Order;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.io.DenoptimIO;
import denoptim.molecule.DENOPTIMEdge.BondType;
import denoptim.molecule.DENOPTIMVertex.BBType;
import denoptim.utils.GenUtils;

/**
 * This class represents an undirected version of the edge between two vertices.
 * However, it does not behave as a DENOPTIMEdge since it does not interfere
 * with the available state of the attachment points.
 */

public class UndirectedEdgeRelation 
{
    /**
     * Attachment point A
     */
    private DENOPTIMAttachmentPoint apA;

    /**
     * Attachment point B
     */
    private DENOPTIMAttachmentPoint apB;

    /**
     * The bond type associated with the connection between the fragments
     */
    private BondType bondType = BondType.UNDEFINED;


//------------------------------------------------------------------------------
    
    /**
     * Constructor for an undirected edge. This edge does not make the APs
     * unavailable.
     * @param apA one of the attachment points connected by this edge
     * @param apB another of the attachment points connected by this edge
     * @param bondType defines what kind of bond type this edge should be 
     * converted to when converting a graph into a chemical representation.
     */
    
    public UndirectedEdgeRelation(DENOPTIMAttachmentPoint apA,
                          DENOPTIMAttachmentPoint apB, BondType bondType) {
        this.apA = apA;
        this.apB = apB;
        this.bondType = bondType;
    }
      
//------------------------------------------------------------------------------
      
    /**
     * Constructor for an undirected edge. This edge does not make the APs
     * unavailable. Bond type is inferred from
     * the first attachment point.
     * @param apA one of the attachment points connected by this edge
     * @param apB another of the attachment points connected by this edge
     */
    
    public UndirectedEdgeRelation(DENOPTIMAttachmentPoint apA, 
            DENOPTIMAttachmentPoint apB) {
        this(apA, apB, FragmentSpace.getBondOrderForAPClass(
                  apA.getAPClass().toString()));
    }
    
//------------------------------------------------------------------------------
 
    public int compare(UndirectedEdgeRelation other)
    {
        DENOPTIMVertex tvA = apA.getOwner();
        DENOPTIMVertex tvB = apB.getOwner();
        DENOPTIMVertex ovA = other.apA.getOwner();
        DENOPTIMVertex ovB = other.apB.getOwner();
        
        String invariantTA = tvA.getBuildingBlockType().toOldInt() +
                GenUtils.getPaddedString(6,tvA.getBuildingBlockId()) +
                GenUtils.getPaddedString(4,apA.getIndexInOwner());
        
        String invariantTB = tvB.getBuildingBlockType().toOldInt() +
                GenUtils.getPaddedString(6,tvB.getBuildingBlockId()) +
                GenUtils.getPaddedString(4,apB.getIndexInOwner());
        
        String invariantOA = ovA.getBuildingBlockType().toOldInt() +
                GenUtils.getPaddedString(6,ovA.getBuildingBlockId()) +
                GenUtils.getPaddedString(4,other.apA.getIndexInOwner());
        
        String invariantOB = ovB.getBuildingBlockType().toOldInt() +
                GenUtils.getPaddedString(6,ovB.getBuildingBlockId()) +
                GenUtils.getPaddedString(4,other.apB.getIndexInOwner());
                
        String invariantThis = invariantTA + invariantTB;
        if (invariantTA.compareTo(invariantTB) > 0)
            invariantThis = invariantTB + invariantTA;
        
        String invariantOther = invariantOA + invariantOB;
        if (invariantOA.compareTo(invariantOB) > 0)
            invariantOther = invariantOB + invariantOA;
        
        int resultIgnoringBondType = invariantThis.compareTo(invariantOther);
        
        if (resultIgnoringBondType == 0)
        {
            return this.bondType.compareTo(other.bondType);
        } else {
            return resultIgnoringBondType;
        }
    }

//------------------------------------------------------------------------------

    @Override
    public String toString()
    {
        DENOPTIMVertex vA = apA.getOwner();
        DENOPTIMVertex vB = apB.getOwner();
        
        StringBuilder sb = new StringBuilder(64);
        sb.append("v" + vA.getVertexId()).append("_ap")
                .append(apA.getIndexInOwner()).append("_")
                .append("v" + vB.getVertexId()).append("_ap")
                .append(apB.getIndexInOwner()).append("_")
                .append(bondType);
        
        return sb.toString();
    }

//------------------------------------------------------------------------------

}
