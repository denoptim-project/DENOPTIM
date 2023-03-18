/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no> and
 *   Marco Foscato <marco.foscato@uib.no>
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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.graph.DGraph;
import denoptim.graph.Vertex;


/**
 * Utilities for graphs.
 *
 * @author Vishwesh Venkatraman 
 * @author Marco Foscato
 */
public class GraphUtils
{
    public static AtomicLong vertexCounter = new AtomicLong(1);
    private static AtomicInteger graphCounter = new AtomicInteger(1);
    private static AtomicInteger molCounter = new AtomicInteger(1);

//------------------------------------------------------------------------------
    
    /**
     * Method used to ensure consistency between internal atomic integer and 
     * vertex id from imported graphs. The latter can be greater than the first, 
     * thus requiring a reset of the atomic integer to a new, larger than 
     * before, value.
     * @param g
     * @throws DENOPTIMException
     */
    public static synchronized void ensureVertexIDConsistency(long l) 
            throws DENOPTIMException
    {   
        if (GraphUtils.getUniqueVertexIndex() <= l)
        {
            GraphUtils.resetUniqueVertexCounter(l+1);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Reset the unique vertex counter to the given value. In order to keep the
     * uniqueness on the index, this method accepts only reset values
     * that are higher that the current one. Attempts to reset to lower values 
     * return an exception.
     * @param l the new value for the counter. This value will be given to
     * next call of the getUniqueVertexIndex method.
     * @throws DENOPTIMException if the reset value is lower than the current
     * value of the index.
     */

    public static synchronized void resetUniqueVertexCounter(long l)
            throws DENOPTIMException
    {
        if (vertexCounter.get() >= l)
        {
            String msg = "Attempt to reset the unique vertex ID using "
                         + l + " while the current value is "
                         + vertexCounter.get();
            throw new DENOPTIMException(msg);
        }
        vertexCounter = new AtomicLong(l);
    }

//-----------------------------------------------------------------------------

    /**
     * Unique counter for the number of graph vertices generated.
     * @return the new vertex id (number)
     */

    public static synchronized long getUniqueVertexIndex()
    {
        if (vertexCounter.get() >= Long.MAX_VALUE-10)
            throw new Error("Reached maximum value for "
                    + "Vertex identifier. It is highly likely that you should "
                    + "not be in this situation as it means you have generated "
                    + "too many vertices. Contact the authors is you really "
                    + "think you need vertex identifiers bigger than 2^64.");
        return vertexCounter.getAndIncrement();
    }

//------------------------------------------------------------------------------

    /**
     * Reset the unique graph counter to the given value. In order to keep the
     * uniqueness on the index, this method accepts only reset values
     * that are higher that the current one. Attempts to reset to lower values
     * return an exception.
     * @param val the new value for the counter. This value will be given to
     * next call of the getUniqueGraphIndex method.
     * @throws DENOPTIMException if the reset value is lower than the current
     * value of the index.
     */

    public static synchronized void resetUniqueGraphCounter(int val) 
            throws DENOPTIMException
    {
        if (graphCounter.get() >= val)
        {
            String msg = "Attempt to reset the unique graph ID using "
                         + val + " while the current value is "
                         + graphCounter.get();
            throw new DENOPTIMException(msg);
        }
        graphCounter = new AtomicInteger(val);
    }

//------------------------------------------------------------------------------
    
    /**
     * Unique counter for the number of graphs generated.
     * @return a new Graph id (number)
     * @throws DENOPTIMException 
     */

    public static synchronized int getUniqueGraphIndex()
    {
        if (graphCounter.get() >= Integer.MAX_VALUE-10)
            throw new Error("Reached maximum value for "
                    + "Graph identifier. Please contact the authors to "
                    + "request use of 'long' IDs.");
        return graphCounter.getAndIncrement();
    }

//------------------------------------------------------------------------------

    /**
     * Reset the unique mol counter to the given value. In order to keep the
     * uniqueness on the index, this method accepts only reset values
     * that are higher that the current one. Attempts to reset to lower values
     * return an exception.
     * @param val the new value for the counter. This value will be given to
     * next call of the getUniqueMoleculeIndex method.
     * @throws DENOPTIMException if the reset value is lower than the current
     * value of the index.
     */

    public static synchronized void resetUniqueMoleculeCounter(int val) 
                                                        throws DENOPTIMException
    {
        if (molCounter.get() >= val)
        {
            String msg = "Attempt to reser the unique mol ID using "
                         + val + " while the current value is "
                         + molCounter.get();
            throw new DENOPTIMException(msg);
        }
        molCounter = new AtomicInteger(val);
    }

//------------------------------------------------------------------------------
    
    /**
     * Unique counter for the number of molecules generated.
     * @return the new molecule id (number)
     */

    public static synchronized int getUniqueMoleculeIndex()
    {
        if (molCounter.get() >= Integer.MAX_VALUE-10)
            throw new Error("Reached maximum value for "
                    + "Molecule identifier. Please contact the authors to "
                    + "request use of 'long' IDs.");
        return molCounter.getAndIncrement();
    }
  
//------------------------------------------------------------------------------

    public static void writeSDFFields(IAtomContainer iac, DGraph g)
            throws DENOPTIMException
    {
        iac.setProperty(DENOPTIMConstants.GCODETAG, g.getGraphId());
        iac.setProperty(DENOPTIMConstants.GRAPHTAG, g.toString());
        iac.setProperty(DENOPTIMConstants.GRAPHJSONTAG, g.toJson());
        if (g.getLocalMsg() != null
                && !g.getLocalMsg().toString().equals(""))
        {
            iac.setProperty(DENOPTIMConstants.PROVENANCE,g.getLocalMsg());
        }
    }
    
//------------------------------------------------------------------------------

    public static String getLabel(Vertex v)
    {
        if (!v.hasProperty("Label"))
            return "";
        return v.getGraphOwner().getGraphId()+"@"+v.getProperty(
                "Label").toString();
    }
    
//------------------------------------------------------------------------------
    
    public static String getLabel(DGraph g, int vIdx)
    {
        if (!g.getVertexAtPosition(vIdx).hasProperty("Label"))
            return "";
        return g.getGraphId() + "@" + g.getVertexAtPosition(vIdx).getProperty(
                "Label").toString();
    }
    
//------------------------------------------------------------------------------

}
