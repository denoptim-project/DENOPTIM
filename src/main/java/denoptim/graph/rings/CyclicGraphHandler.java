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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.vecmath.Point3d;

import org.openscience.cdk.Bond;
import org.openscience.cdk.graph.ShortestPaths;
import org.openscience.cdk.graph.matrix.TopologicalMatrix;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.graph.APClass;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.DGraph;
import denoptim.graph.Edge;
import denoptim.graph.Edge.BondType;
import denoptim.graph.EmptyVertex;
import denoptim.graph.Fragment;
import denoptim.graph.Ring;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;
import denoptim.utils.ManySMARTSQuery;
import denoptim.utils.MoleculeUtils;
import denoptim.utils.ObjectPair;
import denoptim.utils.RingClosingUtils;


/**
 * This is a tool to identify and manage vertices' connections not included
 * in the {@link DGraph}, which is a spanning tree, thus connections that 
 * identify cyclic paths in the graph.
 * The three dimensional features of the vertices can be taken into account
 * while defining the closability of a candidate chain.
 * Since this is a computationally demanding task, we make use of serialized
 * data that is recovered from previous experiments, updated on the fly,
 * and stored for future used.
 *
 * @author Marco Foscato 
 */

public class CyclicGraphHandler
{
    /**
     * variables needed by recursive methods
     */
    private int recCount = 0;
    private int maxLng = 0;
    
    /**
     * Parameters 
     */
    private RingClosureParameters settings;
    
    /**
     * Fragment space definition
     */
    private FragmentSpace fragSpace;
    
    /**
     * Logger to use
     */
    private Logger logger;
    
    /**
     * New line character
     */
    private static final String NL = DENOPTIMConstants.EOL; 
    
//-----------------------------------------------------------------------------

    /**
     * Constructor from data structure. 
     * @param libScaff the library of scaffolds
     * @param libFrag the library of fragments
     * @param libCap the library of capping groups
     */

    public CyclicGraphHandler(RingClosureParameters settings, 
            FragmentSpace fragSpace) 
    {
        this.settings = settings;
        this.logger = settings.getLogger();
        this.fragSpace = fragSpace;
    }
    
//-----------------------------------------------------------------------------

    /**
     * Identifies a random combination of ring closing paths and returns it as 
     * list of DENOPTIMRings ready to be appended to a DENOPTIMGraph.
     * @param inMol the molecule
     * @param molGraph the molecular graph
     * @param maxRingClosures maximum number of ring closures to perform.
     * @return the selected combination of closable paths 
     */

    public List<Ring> getRandomCombinationOfRings(IAtomContainer inMol,
            DGraph molGraph, int maxRingClosures) throws DENOPTIMException
    {
        RandomCombOfRingsIterator iter = new RandomCombOfRingsIterator(inMol,
                molGraph, maxRingClosures, fragSpace, settings);
        List<Ring> combOfRings = iter.next();
        logger.log(Level.FINE,"Random combOfRings: "+combOfRings);
        return combOfRings;
    }

//-----------------------------------------------------------------------------

    /**
     * Identifies all possible ring closing paths and returns them as list of 
     * DENOPTIMRings ready to be appended to a DENOPTIMGraph.
     * @param mol the molecule
     * @param molGraph the molecular graph
     * @return the candidate closable paths in the given graph
     */

    //TODO: this should use most of the same code of RandomCombOfRingsIterator
    
    public ArrayList<List<Ring>> getPossibleCombinationOfRings(
            IAtomContainer mol, DGraph molGraph)
                    throws DENOPTIMException
    {
        // All the candidate paths 
        Map<ObjectPair,PathSubGraph> allGoodPaths = 
                new HashMap<ObjectPair,PathSubGraph>();
        ArrayList<Vertex> rcaVertLst = molGraph.getFreeRCVertices();
        
        // Get manager of ring size problems
        RingSizeManager rsm = new RingSizeManager(fragSpace, settings);
        rsm.initialize(mol, molGraph);

        // identify compatible pairs of RCA vertices
        Map<Vertex,ArrayList<Vertex>> compatMap =
                       new HashMap<Vertex,ArrayList<Vertex>>();
        for (int i=0; i<rcaVertLst.size(); i++)
        {
            Vertex vI = rcaVertLst.get(i);
            for (int j=i+1; j<rcaVertLst.size(); j++)
            {
                Vertex vJ = rcaVertLst.get(j);
                if (!rsm.getCompatibilityOfPair(vI,vJ))
                {
                    logger.log(Level.FINE, "Rejecting RC-incompatible pair " 
                                + vI + " "+ vJ);
                    continue;
                }
                
                // make the new candidate RCA pair
                PathSubGraph subGraph = new PathSubGraph(vI, vJ, molGraph);
                logger.log(Level.FINE, "Evaluating closability of path " 
                + subGraph);
                boolean keepRcaPair = PathClosabilityTools.isCloseable(subGraph, 
                        mol, settings);

                if (!keepRcaPair)
                {
                    logger.log(Level.FINE, "Rejecting RCA pair");
                    continue;
                }

                // finally store this pair as a compatible pair
                logger.log(Level.FINE, "All compatibility criteria satisfied: "
                        + "Storing verified RCA pair");

                // Store the information that the two vertex are compatible
                if (compatMap.containsKey(vI))
                {
                    compatMap.get(vI).add(vJ);
                }
                else
                {
                    ArrayList<Vertex> lst =
                                            new ArrayList<Vertex>();
                    lst.add(vJ);
                    compatMap.put(vI,lst);
                }
                if (compatMap.containsKey(vJ))
                {
                    compatMap.get(vJ).add(vI);
                }
                else
                {
                    ArrayList<Vertex> lst =
                                            new ArrayList<Vertex>();
                    lst.add(vI);
                    compatMap.put(vJ,lst);
                }

                // store the RCA pair for further use
                ObjectPair compatPair;
                if (vI.getVertexId() > vJ.getVertexId())
                {
                    compatPair = new ObjectPair(vI,vJ);
                }
                else
                {
                    compatPair = new ObjectPair(vJ,vI);
                }
                allGoodPaths.put(compatPair,subGraph);
            }
        }

        logger.log(Level.FINE, "Compatibility Map for RCAs: "+NL+compatMap);

        // Identify paths that share bonds (interdependent paths)
        Map<IBond,List<PathSubGraph>> interdepPaths =
                                new HashMap<IBond,List<PathSubGraph>>();
        if (settings.checkInterdependentChains())
        {
            for (PathSubGraph rpA : allGoodPaths.values())
            {
                for (PathSubGraph rpB : allGoodPaths.values())
                {
                    if (rpA == rpB)
                    {
                        continue;
                    }
    
                    Vertex hA = rpA.getHeadVertex();
                    Vertex tA = rpA.getTailVertex();
                    Vertex hB = rpB.getHeadVertex();
                    Vertex tB = rpB.getTailVertex();
    
                    if ((hA == hB || hA == tB) || (tA == hB || tA == tB))
                    {
                        continue;
                    }
    
                    for (IBond bnd : rpA.getBondPath())
                    {
                        // ignore non-rotatable bonds
                        // NOTE: here we assume that rotatable bonds have been
                        // identified before.
                        Object rotFlg = bnd.getProperty(
                                          DENOPTIMConstants.BONDPROPROTATABLE);
                        if (rotFlg==null || !Boolean.valueOf(rotFlg.toString()))
                        {
                            continue;
                        }
    
                        if (rpB.getBondPath().contains(bnd))
                        {
                            if (interdepPaths.containsKey(bnd))
                            {
                                interdepPaths.get(bnd).add(rpA);
                                interdepPaths.get(bnd).add(rpB);
                            }
                            else
                            {
                                List<PathSubGraph> paths = 
                                                   new ArrayList<PathSubGraph>();
                                paths.add(rpA);
                                paths.add(rpB);
                                interdepPaths.put(bnd,paths);
                            }
                        }
                    }
                }
            }
            StringBuilder sb = new StringBuilder();
            for (IBond bnd : interdepPaths.keySet())
            {
                sb.append("Interdependent paths for bond " + bnd);
                List<PathSubGraph> sop = interdepPaths.get(bnd);
                for (PathSubGraph p : sop)
                {
                    sb.append(NL + "  ->  vA: "+p.getHeadVertex()
                                      +" vB: "+p.getTailVertex());
                }
            }
            logger.log(Level.FINE, sb.toString());
        }

        // Generate all combinations of compatible, closable paths
        ArrayList<ObjectPair> lstPairs = new ArrayList<ObjectPair>();
        ArrayList<Long> usedId = new ArrayList<Long>();
        ArrayList<Vertex> sortedKeys = new ArrayList<Vertex>();
        for (Vertex keyVert : compatMap.keySet())
        {
            sortedKeys.add(keyVert);
        }

        // All possible ring closing paths will be stored here
        ArrayList<List<Ring>> allCombsOfRings =
                                     new ArrayList<List<Ring>>();
        combineCompatPathSubGraphs(0,
                sortedKeys,
                compatMap,
                lstPairs,
                usedId,
                allGoodPaths,
                interdepPaths,
                allCombsOfRings);
        logger.log(Level.FINE, "All possible combination of rings: " + 
                                                             allCombsOfRings);
        return allCombsOfRings;
    }

//-----------------------------------------------------------------------------

    /**
     * Recursive method to identify all the combination of rings.
     */

    private boolean combineCompatPathSubGraphs(int ii0,
                       ArrayList<Vertex> sortedKeys,
                       Map<Vertex,ArrayList<Vertex>> compatMap,
                       ArrayList<ObjectPair> lstPairs,
                       ArrayList<Long> usedId,
                       Map<ObjectPair,PathSubGraph> allGoodPaths,
                       Map<IBond,List<PathSubGraph>> interdepPaths,
                       ArrayList<List<Ring>> allCombsOfRings)
                                                      throws DENOPTIMException
    {
        int objId = this.hashCode();
        String recLab = new String(new char[recCount]).replace("\0", "-");
        
        StringBuilder sb = new StringBuilder();
        sb.append(objId+"-"+recLab+"> Begin of new recursion: "+recCount+NL);
        sb.append(objId+"-"+recLab+"> sortedKeys= "+sortedKeys+NL);
        sb.append(objId+"-"+recLab+"> usedId= "+usedId+NL);
        sb.append(objId+"-"+recLab+"> ii0= "+ii0+NL);
        sb.append(objId+"-"+recLab+"> lstPairs= "+lstPairs+NL);
        sb.append(objId+"-"+recLab+"> compatMap= "+compatMap+NL);
        sb.append(objId+"-"+recLab+"> allCombsOfRings"+NL);
        for (List<Ring> ringSet : allCombsOfRings)
        {
            sb.append("        "+ringSet+NL);
        }
        logger.log(Level.FINEST, sb.toString()); 

        boolean inFound = false;
        boolean addedNew = false;
        for (int ii=ii0; ii<sortedKeys.size(); ii++)
        {
            Vertex vi = sortedKeys.get(ii);
            long vIdI = vi.getVertexId();

            logger.log(Level.FINEST, objId+"-"+recLab+"> vIdI= "+vIdI);

            if (usedId.contains(vIdI))
            {
                continue;
            }

            for (Vertex vj : compatMap.get(vi))
            {
                long vIdJ = vj.getVertexId();

                logger.log(Level.FINEST, objId+"-"+recLab+"> vIdJ= "+vIdJ);

                if (usedId.contains(vIdJ) || usedId.contains(vIdI))
                {
                    continue;
                }

                ObjectPair op;
                if (vIdI > vIdJ)
                {
                    op = new ObjectPair(vi,vj);
                }
                else
                {
                    op = new ObjectPair(vj,vi);
                }
                lstPairs.add(op);
                usedId.add(vIdI);
                usedId.add(vIdJ);

                if (lstPairs.size() > maxLng)
                {
                    maxLng = lstPairs.size();
                }
                recCount++;
                inFound = combineCompatPathSubGraphs(ii+1,
                                        sortedKeys,
                                        compatMap,
                                        lstPairs,
                                        usedId,
                                        allGoodPaths,
                                        interdepPaths,
                                        allCombsOfRings);
                recCount--;

                logger.log(Level.FINEST, objId+"-"+recLab+"> lstPairs.size() "
                        + "& maxLng= "+lstPairs.size() +" " +maxLng); 

                if (!inFound && lstPairs.size() == maxLng)
                {
                    logger.log(Level.FINEST, objId+"-"+recLab+"> in A");

                    boolean closable = true;
                    if (settings.checkInterdependentChains() &&
                               hasInterdependentPaths(lstPairs, interdepPaths))
                    {
                        closable = checkClosabilityOfInterdependentPaths(
                                                        lstPairs,
                                                        interdepPaths,
                                                        allGoodPaths);
                    }
                    if (closable)
                    {
                        logger.log(Level.FINEST, objId+"-"+recLab+"> in B");

                        List<Ring> ringsComb = new ArrayList<Ring>();
                        for (ObjectPair opFinal : lstPairs)
                        {
                            PathSubGraph path = allGoodPaths.get(opFinal);
                            ArrayList<Vertex> arrLst = 
                                               new ArrayList<Vertex>();
                            arrLst.addAll(path.getVertecesPath());

                            Ring ring = new Ring(arrLst);

                            List<Edge> es = path.getEdgesPath();
                            BondType btH = es.get(0).getBondType();
                            BondType btT = es.get(es.size()-1).getBondType();
                            if (btH != btT)
                            {
                                String s = "Attempt to close rings is not "
                                + "compatible to the different bond type "
                                + "specified by the head and tail APs: (" 
                                + btH + "!=" + btT + " for vertices " 
                                + path.getHeadVertex() + " " 
                                + path.getTailVertex() + ")"; 
                                throw new DENOPTIMException(s);
                            }
                            ring.setBondType(btH);

                            ringsComb.add(ring);

                            logger.log(Level.FINEST, objId+"-"+recLab
                                    +"> added ringComb: "+ring);
                        }

                        boolean notNewCmb = false;
                        for(List<Ring> oldCmb : allCombsOfRings)
                        {
                            StringBuilder sb2 = new StringBuilder();
                            sb2.append(objId+"-"+recLab
                                    +"> Comparing ring sets: ");
                            sb2.append("o> old: "+oldCmb);
                            sb2.append("o> new: "+ringsComb);
                            logger.log(Level.FINEST, sb2.toString());

                            notNewCmb = RingClosingUtils.areSameRingsSet(oldCmb,
                                                                     ringsComb);
                            
                            logger.log(Level.FINEST,"o> result: "+notNewCmb);

                            if (notNewCmb)
                            {
                                break;
                            }
                        }

                        if (!notNewCmb)
                        {
                            logger.log(Level.FINEST, objId+"-"+recLab
                                    +"> adding to all combs of ring.");
                            allCombsOfRings.add(ringsComb);
                            addedNew = true;
                        }
                    }
                }
                if (!inFound)
                {
                    logger.log(Level.FINEST, objId+"-"+recLab+"> in C");

                    ArrayList<ObjectPair> toDel = new ArrayList<ObjectPair>();
                    for (int ir = recCount; ir<lstPairs.size(); ir++)
                    {
                        ObjectPair opToRemove = lstPairs.get(ir);
                        Vertex delVA = (Vertex) opToRemove.getFirst();
                        Vertex delVB = (Vertex) opToRemove.getSecond();
                        usedId.remove(usedId.indexOf(delVA.getVertexId()));
                        usedId.remove(usedId.indexOf(delVB.getVertexId()));
                        toDel.add(opToRemove);
                    }
                    lstPairs.removeAll(toDel);

                    logger.log(Level.FINEST, objId+"-"+recLab
                            +"> in C: after removal usedId: "+usedId+NL
                            +objId+"-"+recLab
                            +"> in C: after removal lstPairs: "+lstPairs);
                }

                if (lstPairs.contains(op))
                {
                    logger.log(Level.FINEST, objId+"-"+recLab+"> in D");

                    lstPairs.remove(op);
                    usedId.remove(usedId.indexOf(vIdI));
                    usedId.remove(usedId.indexOf(vIdJ));

                    logger.log(Level.FINEST, objId+"-"+recLab
                            +"> in D: after removal usedId: "+usedId
                            +objId+"-"+recLab
                            +"> in D: after removal lstPairs: "+lstPairs);
                }
            }
        }
        
        logger.log(Level.FINEST, objId+"-"+recLab+"> returning= "+addedNew);

        return addedNew;
    }

//-----------------------------------------------------------------------------

    /**
     * Checks whether the combination of RCA's pairs leads to interdependent 
     * paths, that is, paths that share one or more bonds. 
     */

    private boolean hasInterdependentPaths(ArrayList<ObjectPair> lstPairs,
                                    Map<IBond,List<PathSubGraph>> interdepPaths)
    {
        boolean result = false;
        
        for (IBond bnd : interdepPaths.keySet())
        {
            List<PathSubGraph> psgSet = interdepPaths.get(bnd);
            for (PathSubGraph psg : psgSet)
            {
                Vertex va1 = psg.getHeadVertex();
                Vertex va2 = psg.getTailVertex();

                for (ObjectPair op : lstPairs)
                {
                    Vertex vb1 = (Vertex) op.getFirst();
                    Vertex vb2 = (Vertex) op.getSecond();
                
                    if ((va1 == vb1 && va2 == vb2) ||
                        (va1 == vb2 && va2 == vb1))
                    {
                        result = true;
                        break;
                    }
                }
                if (result)
                    break;
            }
            if (result)
                break;
        }
        return result;
    }

//-----------------------------------------------------------------------------

    /**
     * This method checks
     * whether the interdependent paths are simultaneously closable.
     * @return true is all interdependent sets are closable
     */

    private boolean checkClosabilityOfInterdependentPaths(
                                ArrayList<ObjectPair> lstPairs,
                                Map<IBond,List<PathSubGraph>> interdepPaths,
                                Map<ObjectPair,PathSubGraph> allGoodPaths)
    {
        // Identify the interdependent sets of paths
        List<ArrayList<ObjectPair>> listOfIntrDepPaths = 
                new ArrayList<ArrayList<ObjectPair>>();
        for (IBond bnd : interdepPaths.keySet())
        {
            ArrayList<ObjectPair> locSop = new ArrayList<ObjectPair>();
            List<PathSubGraph> psgSet = interdepPaths.get(bnd);
            for (PathSubGraph psg : psgSet)
            {
                Vertex va1 = psg.getHeadVertex();
                Vertex va2 = psg.getTailVertex();

                for (ObjectPair op : lstPairs)
                {
                    Vertex vb1 = (Vertex) op.getFirst();
                    Vertex vb2 = (Vertex) op.getSecond();

                    if ((va1 == vb1 && va2 == vb2) ||
                        (va1 == vb2 && va2 == vb1))
                    {
                        locSop.add(op);
                    }
                }
            }
            if (locSop.size() > 1)
            {
                listOfIntrDepPaths.add(locSop);
            }
        }

        // Verify simultaneous closeness of each group of interdependent paths
        boolean closable = true;
        for (ArrayList<ObjectPair> grpIntrdepPaths : listOfIntrDepPaths)
        {
            // Per each closable conf. of the first path stores the paths 
            // that have a simultaneously closable conf.
            Map<ClosableConf,List<ObjectPair>> mapOcPathsWithCC = 
                                 new HashMap<ClosableConf,List<ObjectPair>>();

            // Per each closable conf. of a path lists the simultaneously 
            // closable confs of the other interdependent paths
            Map<ClosableConf,List<ClosableConf>> mapOfClosableConfs =
                                 new HashMap<ClosableConf,List<ClosableConf>>();

            // For the first path just put all closable conf in the list
            ObjectPair firstOp = grpIntrdepPaths.get(0);
            PathSubGraph firstPsg = allGoodPaths.get(firstOp);
            RingClosingConformations firstRcc = firstPsg.getRCC();

            for (ArrayList<Double> ccAngls : firstRcc.getListOfConformations())
            {
                ClosableConf cc = new ClosableConf(firstPsg.getBondPath(), 
                                                   ccAngls);
                // Container for cc of other paths
                List<ClosableConf> scc = new ArrayList<ClosableConf>();
                mapOfClosableConfs.put(cc,scc);
                // Container for other paths
                List<ObjectPair> sop = new ArrayList<ObjectPair>();
                sop.add(firstOp);
                mapOcPathsWithCC.put(cc,sop);                
            }

            // For other paths put only the closable confs (CC) that are
            // simultaneously closable with previously listed CCs
            for (int iOp=1; iOp<grpIntrdepPaths.size(); iOp++)
            {
                ObjectPair locOp = grpIntrdepPaths.get(iOp);
                PathSubGraph locPath = allGoodPaths.get(locOp);
                RingClosingConformations rccOfLocPath = locPath.getRCC();

                // try with each closable conf of the local path
                for (ArrayList<Double> ccAngles : 
                                         rccOfLocPath.getListOfConformations())
                {
                    ClosableConf locCC = new ClosableConf(
                                              locPath.getBondPath(), ccAngles);
                    // Compare the locCC with CC of the first path
                    for (ClosableConf frstCC : mapOfClosableConfs.keySet())
                    {
                        if (frstCC.shareBond(locCC) && 
                                                  frstCC.canCoexistWith(locCC))
                        {
                            // CC of loc path is simultaneously closable with
                            // this CC of first path (frstCC)
                            if (mapOfClosableConfs.get(frstCC).size() == 0)
                            {
                                // the list of CC simultaneously closable with
                                // the key CC (frstCC) is empty, so just
                                // add locCC for future needs
                                mapOfClosableConfs.get(frstCC).add(locCC);
                                // add to Set wont add if is already there
                                mapOcPathsWithCC.get(frstCC).add(locOp);
                            }
                            else
                            {
                                // the list of CC simultaneously closable with
                                // the key CC (frstCC) has already one or more
                                // entries: we need to compare this locCC with
                                // all these entries
                                boolean canCoexistWithAll = true;
                                for (ClosableConf lstCC : 
                                                mapOfClosableConfs.get(frstCC))
                                {
                                    if (lstCC.shareBond(locCC) && 
                                                  !lstCC.canCoexistWith(locCC))
                                    {
                                        // Sorry, locCC cannot coexist
                                        canCoexistWithAll = false;
                                        break;
                                    }
                                }
                                if (canCoexistWithAll)
                                {
                                    mapOfClosableConfs.get(frstCC).add(locCC);
                                    // add to Set wont add if is already there
                                    mapOcPathsWithCC.get(frstCC).add(locOp); 
                                }
                            }
                        }
                    }
                }

                boolean goon = true;
                for (ClosableConf frstCC : mapOfClosableConfs.keySet())
                {
                    if (!mapOcPathsWithCC.get(frstCC).contains(locOp))
                    {
                        goon = false;
                        break;
                    }
                }
                if (!goon)
                {
                    break;
                }
            }

            // Now check if there is at least one set of simultaneously
            // closable conformations for all members of this group
            // of interdependent paths
            boolean foundOneSetFullyClosable = false;
            for (ClosableConf cc : mapOfClosableConfs.keySet())
            {
                int numPathWithSimCC = mapOcPathsWithCC.get(cc).size();
                int numInterdepPaths = grpIntrdepPaths.size();
                if (numPathWithSimCC == numInterdepPaths)
                {
                    foundOneSetFullyClosable = true;
                    break;
                }
            }
            if (!foundOneSetFullyClosable)
            {
                closable = false;
                break;
            }
        }

        return  closable;
    }

//-----------------------------------------------------------------------------

    /**
     * Utility class to handle the simultaneous closeness condition.
     * It represent a set of bonds with the dihedral angle defining the torsion
     * around each of the bonds.
     */

    private class ClosableConf
    {
        private List<IBond> bonds;
        private ArrayList<Double> angs;

        //---------------------------------------------------------------------
        
        public ClosableConf(List<IBond> bonds, ArrayList<Double> dihedrals)
        {
            this.bonds = bonds;
            this.angs = dihedrals;
        }

        //---------------------------------------------------------------------

        public boolean shareBond(ClosableConf other)
        {
            boolean shareBnd = false;
            for (IBond tBnd : this.bonds)
            {
                if (other.bonds.contains(tBnd))
                {
                    shareBnd = true;
                    break;
                }
            }
            return shareBnd;
        }

        //---------------------------------------------------------------------
        public boolean canCoexistWith(ClosableConf other)
        {
            boolean canCoexist = true;
            double thrs = settings.getPathConfSearchStep() / 2.0;
            for (int i=0; i<this.bonds.size(); i++)
            {
                IBond tBnd = this.bonds.get(i);
                for (int j=0; j<other.bonds.size(); j++)
                {
                    IBond oBnd = other.bonds.get(j);
                    if (tBnd == oBnd)
                    {
                        double diff = this.angs.get(i) - other.angs.get(j);
                        diff = Math.abs(diff);
                        if (diff > thrs)
                        {
                            canCoexist = false;
                            break;
                        }
                    }
                }
                if (!canCoexist)
                    break;
            }

            return canCoexist;
        }

        //---------------------------------------------------------------------
        public String toString()
        {
            String s = " ClosableConf [nBonds: " + bonds.size()
                        + " bonds: ";
            for (IBond bnd : bonds)
            {
                s = s + bnd.getAtom(0).getSymbol() + "-" 
                      + bnd.getAtom(1).getSymbol() + " ";
            }

            s = s + " dihedrals: " + angs + "]";

            return s;
        }
    }
    
//-----------------------------------------------------------------------------

    /**
     * Evaluates the combination of a DENOPTIMGraph and a set of DENOPTIMRings
     * and decides whether it's a proper candidate for the generation of a
     * chelating ligand.
     * @return <code>true</code> it this system is a good candidate
     */

    public boolean checkChelatesGraph(DGraph molGraph, List<Ring> ringsSet)
    {
        logger.log(Level.FINE, "Checking conditions for chelates");

//TODO: here we assume that the scaffold is a metal and the first layer of
// vertices (level = 0) are the coordinating atoms.
// Also, the APclass of the ap connecting the candidate orphan is hard coded!
// This is a temporary solution. Need a general approach and tunable by
// options/parameters

        for (Vertex vert : molGraph.getVertexList())
        {
            long vId = vert.getVertexId();
            
            Fragment vertFrag = null;
            if (vert instanceof Fragment)
                vertFrag = (Fragment) vert;
                

            // check for orphan coordinating atoms:
            // they have RCAs but none of them is included in a rings
            int levelOfVert = molGraph.getLevel(vert);
            if (levelOfVert == 0 
                    && vertFrag.getBuildingBlockType() == BBType.FRAGMENT)
            {
                Edge edgeToParnt = molGraph.getEdgeWithParent(vId);
                APClass apClassToScaffold = edgeToParnt.getTrgAPClass();
                if (settings.metalCoordinatingAPClasses.contains(
                        apClassToScaffold))
                {
                    continue;
                }
                
                boolean isOrphan = false;
                for (Vertex cVrtx : vert.getChilddren())
                {
                    if (cVrtx.isRCV() && !molGraph.isVertexInRing(cVrtx))
                    {
                        isOrphan = true;
                        break;
                    }
                }
                if (isOrphan)
                {
                    logger.log(Level.FINE, "Found orphan: " + vert 
                                                    + " RingSet: " + ringsSet);
                    return false;
                }
            }

            // check for not fully coordinating multidentate bridges
//TODO: make the full-denticity requirement optional for same/all APclasses
            if (levelOfVert > 0)
            {
                Map<String,ArrayList<Vertex>> rcasOnThisVertex =
                               new HashMap<String,ArrayList<Vertex>>();
                for (Vertex cVrtx : vert.getChilddren())
                {
                    if (cVrtx.isRCV())
                    {
                        AttachmentPoint ap =
                                        cVrtx.getAttachmentPoints().get(0);
                        String apCls = ap.getAPClass().toString();
                        if (rcasOnThisVertex.keySet().contains(apCls))
                        {
                            rcasOnThisVertex.get(apCls).add(cVrtx);
                        }
                        else
                        {
                            ArrayList<Vertex> sameClsRCA =
                                               new ArrayList<Vertex>();
                            sameClsRCA.add(cVrtx);
                            rcasOnThisVertex.put(apCls,sameClsRCA);
                        }
                    }
                }

                for (String apCls : rcasOnThisVertex.keySet())
                {
                    int usedDenticity = 0;
                    for (Vertex rcaVrtx : rcasOnThisVertex.get(apCls))
                    {
                        if (molGraph.isVertexInRing(rcaVrtx))
                        {
                            usedDenticity++;
                        }
                    }

                    if (usedDenticity < rcasOnThisVertex.get(apCls).size())
                    {
                        logger.log(Level.FINE, "Full-denticity is not "
                                  + "satisfied for apclas: " + apCls
                                  + "in vertex " + vert 
                                  + " with set of rings " + ringsSet
                                  + "check graph: " + molGraph);
                        return false;
                    }
                }
            }
        }
        
        return true;
    }

//-----------------------------------------------------------------------------

}
