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

import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.APClass;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMEdge;
import denoptim.molecule.DENOPTIMFragment;
import denoptim.molecule.DENOPTIMFragment.BBType;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.molecule.SymmetricSet;
import denoptim.rings.ClosableChain;

import java.util.logging.Level;


/**
 * Utilities for graphs.
 *
 * @author Vishwesh Venkatraman 
 * @author Marco Foscato
 */
public class GraphUtils
{
    public static AtomicInteger vertexCounter = new AtomicInteger(1);
    private static AtomicInteger graphCounter = new AtomicInteger(1);
    private static AtomicInteger molCounter = new AtomicInteger(1);

    private static boolean debug = false;

//------------------------------------------------------------------------------

    /**
     * Reset the unique vertex counter to the given value. In order to keep the
     * uniqueness on the index, this method accepts only reset values
     * that are higher that the current one. Attempts to reset to lower values 
     * return an exception.
     * @param val the new value for the counter. This value will be given to
     * next call of the getUniqueVertexIndex method.
     * @throws DENOPTIMException if the reset value is lower than the current
     * value of the index.
     */

    public static void resetUniqueVertexCounter(int val)
                                                        throws DENOPTIMException
    {
        if (vertexCounter.get() >= val)
        {
            String msg = "Attempt to reser the unique vertex ID using "
                         + val + " while the current value is "
                         + vertexCounter.get();
            throw new DENOPTIMException(msg);
        }
        vertexCounter = new AtomicInteger(val);
    }

//-----------------------------------------------------------------------------

    /**
     * Unique counter for the number of graph vertices generated.
     * @return the new vertex id (number)
     */

    public static synchronized int getUniqueVertexIndex()
    {
        return vertexCounter.getAndIncrement();
    }

//------------------------------------------------------------------------------

    /**
     * @Deprecated
     */
    public static void updateVertexCounter(int num)
    {
        vertexCounter.set(num);
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

    public static void resetUniqueGraphCounter(int val) throws DENOPTIMException
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
     */

    public static synchronized int getUniqueGraphIndex()
    {
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

    public static void resetUniqueMoleculeCounter(int val) 
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
        return molCounter.getAndIncrement();
    }

//------------------------------------------------------------------------------

    /**
     * Collect the list of closable paths involving the given scaffold
     * @param scaf
     * @param libraryOfRCCs
     * @return getClosableVertexChainsFromDB
     * @throws denoptim.exception.DENOPTIMException 
     */

//TODO del if not used. Should be replaced by a more efficient approach
// using the map of ClosableChains per fragment MolID/type

    public static ArrayList<ClosableChain> getClosableVertexChainsFromDB(
                        DENOPTIMVertex scaf,
                        HashMap<String,ArrayList<String>> libraryOfRCCs)
                                                throws DENOPTIMException
//                String rccIndexFile,
//                String rccRootFolder) throws DENOPTIMException
    {
        ArrayList<ClosableChain> clbChains = new ArrayList<>();

        for (String chainId : libraryOfRCCs.keySet())
        {
            String closability = libraryOfRCCs.get(chainId).get(1);
            if (closability.equals("T"))
            {
                ClosableChain cc = new ClosableChain(chainId);
                int pos = cc.involvesVertex(scaf);
                if (pos != -1)
                {
                    clbChains.add(cc);
                }
            }
        }

        return clbChains;
    }

//------------------------------------------------------------------------------

    /**
     * Attaches the specified fragment to the vertex using the specified pair
     * of AP.
     * @param molGraph
     * @param curVertex the vertex to which the fragment is to be attached
     * @param srcAPIdx index of the AP at which the fragment is to be attached
     * @param fId the fragment id in the library
     * @param fTyp the type of fragment (0: scaffold, 1: fragment, 2: cap)
     * @param trgAPIdx index of the AP on the incoming fragment
     * @return the id of the new vertex created
     * @throws DENOPTIMException
     */
    
    public static int attachNewFragmentAtAPWithAP (DENOPTIMGraph molGraph,
                                                      DENOPTIMVertex curVertex,
                                                      int srcAPIdx, 
                                                      int fId, 
                                                      BBType fTyp,
                                                      int trgAPIdx) 
                                                      throws DENOPTIMException
    {
        DENOPTIMVertex incomingVertex = DENOPTIMVertex.newVertexFromLibrary(fId,
                fTyp);

        int lvl = curVertex.getLevel();
        incomingVertex.setLevel(lvl+1);
        
        
        //TODO-V3: check it this is really not needed anymore
        
        // get molecular representation of incoming fragment
        //IGraphBuildingBlock mol = FragmentSpace.getFragmentLibrary().get(fId);

        // identify the symmetric APs if any for this fragment vertex
        /*
    	if (bb instanceof DENOPTIMFragment)
    	{
    		IAtomContainer iac = ((DENOPTIMFragment) bb).getAtomContainer();
            ArrayList<SymmetricSet> lstCompatible = new ArrayList<>();
            for (int i = 0; i< daps.size()-1; i++)
            {
                ArrayList<Integer> lst = new ArrayList<>();
                Integer i1 = i;
                lst.add(i1);

                boolean alreadyFound = false;
                for (SymmetricSet previousSS : lstCompatible)
                {
                    if (previousSS.contains(i1))
                    {
                        alreadyFound = true;
                        break;
                    }
                }

                if (alreadyFound)
                {
                    continue;
                }

                DENOPTIMAttachmentPoint d1 = daps.get(i);
                for (int j = i+1; j< daps.size(); j++)
                {
                    DENOPTIMAttachmentPoint d2 = daps.get(j);
                    if (isCompatible(iac, d1.getAtomPositionNumber(),
                                                    d2.getAtomPositionNumber()))
                    {
                        // check if reactions are compatible
                        if (isFragmentClassCompatible(d1, d2))
                        {
                            Integer i2 = j;
                            lst.add(i2);
                        }
                    }
                }

                if (lst.size() > 1)
                {
                    lstCompatible.add(new SymmetricSet(lst));
                }
            }

            return lstCompatible;
        } else if (bb instanceof DENOPTIMTemplate) {
    	    return new ArrayList<>();
        }
    	DENOPTIMLogger.appLogger.log(Level.WARNING, "getMatchingAP returns null, but should not");
    	return null;
    	*/
        
        //TODO-V3: this should not be needed anymore: symmetry should come from the vertex
        /*
        ArrayList<SymmetricSet> simAP = mol.getSymmetricAPsSets();
        fragVertex.setSymmetricAP(simAP);
        */
        
        // identify the src AP (on the current vertex)
        ArrayList<DENOPTIMAttachmentPoint> curAPs =
                                                curVertex.getAttachmentPoints();
        DENOPTIMAttachmentPoint srcAP = curAPs.get(srcAPIdx);
        APClass srcAPCls = srcAP.getAPClass();
        
        // identify the target AP (on the appended vertex)
        DENOPTIMAttachmentPoint trgAP = incomingVertex.getAttachmentPoints()
                .get(trgAPIdx);

        APClass trgAPCls = trgAP.getAPClass();

        // create the new DENOPTIMEdge
        DENOPTIMEdge edge;
        edge = curVertex.connectVertices(
                incomingVertex,
                srcAPIdx,
                trgAPIdx,
                srcAPCls,
                trgAPCls
        );

        if (edge != null)
        {
            // update graph
            molGraph.addVertex(incomingVertex);
            molGraph.addEdge(edge);

            return incomingVertex.getVertexId();
        }

        return -1;
    }

//-----------------------------------------------------------------------------
}
