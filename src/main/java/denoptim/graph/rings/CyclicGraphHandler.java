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
        // Prepare molecular representation with no dummy atoms
        // NOTE: the connectivity of this molecule is going to be edited 
        // as we identify new candidate DENOPTIMRings. This is done to
        // calculate ring size as if the DENOPTIMRings were closed
        IAtomContainer mol;
        try
        {
            mol = (IAtomContainer) inMol.clone();
        }
        catch (Throwable t)
        {
            throw new DENOPTIMException(t);
        }

        // Get manager of ring size problems
        RingSizeManager rsm = new RingSizeManager(fragSpace);
        
        rsm.initialize(mol, molGraph);

        // Get weighted list of RCVs
        ArrayList<Vertex> wLstVrtI = rsm.getRSBiasedListOfCandidates();
        
        // Randomly choose the compatible combinations of RCAs and store them
        // as DENOPTIMRings. 
        List<Ring> combOfRings = new ArrayList<Ring>();
        while (wLstVrtI.size() > 0)
        {
            // Termination criterion based on maximum number of rings per graph
            if (combOfRings.size() >= maxRingClosures)
                break;
            
            int vIdI = settings.getRandomizer().nextInt(wLstVrtI.size());
            Vertex vI = wLstVrtI.get(vIdI);
            wLstVrtI.removeAll(Collections.singleton(vI));

            // Create vector of possible choices for second RCV
            ArrayList<Vertex> wLstVrtJ = rsm.getRSBiasedListOfCandidates(vI); 
            while (wLstVrtJ.size() > 0)
            {
                int vIdJ = settings.getRandomizer().nextInt(wLstVrtJ.size());
                Vertex vJ = wLstVrtJ.get(vIdJ);
                wLstVrtJ.removeAll(Collections.singleton(vJ));

                PathSubGraph path = new PathSubGraph(vI,vJ,molGraph);
                if (evaluatePathClosability(path, inMol))
                {
                    ArrayList<Vertex> arrLst = new ArrayList<Vertex>();
                    arrLst.addAll(path.getVertecesPath());                    
                    Ring ring = new Ring(arrLst);

                    BondType bndTypI = vI.getEdgeToParent().getBondType();
                    BondType bndTypJ = vJ.getEdgeToParent().getBondType();
                    if (bndTypI != bndTypJ)
                    {
                        String s = "Attempt to close rings is not compatible "
                        + "to the different bond type specified by the "
                        + "head and tail APs: (" + bndTypI + "!=" 
                        + bndTypJ + " for vertices " + vI + " " 
                        + vJ + ")";
                        throw new DENOPTIMException(s);
                    }
                    ring.setBondType(bndTypI);

                    combOfRings.add(ring);

                    wLstVrtI.removeAll(Collections.singleton(vJ));

                    // Update ring sizes according to the newly added bond
                    if (vI instanceof Fragment && vJ instanceof Fragment)
                    {
                        rsm.addRingClosingBond(vI,vJ);
                    }
                    rsm.setVertexAsDone(vJ);
                    break;
                }
            }
            rsm.setVertexAsDone(vI);
        }

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

    public ArrayList<List<Ring>> getPossibleCombinationOfRings(
            IAtomContainer mol, DGraph molGraph)
                    throws DENOPTIMException
    {
        // All the candidate paths 
        Map<ObjectPair,PathSubGraph> allGoodPaths = 
                                        new HashMap<ObjectPair,PathSubGraph>();
        ArrayList<Vertex> rcaVertLst = molGraph.getFreeRCVertices();
        
        // Get manager of ring size problems
        RingSizeManager rsm = new RingSizeManager(fragSpace);
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
                PathSubGraph subGraph = new PathSubGraph(vI,vJ,molGraph);
                logger.log(Level.FINE, "Evaluating closability of path "+subGraph);
                boolean keepRcaPair = evaluatePathClosability(subGraph, mol);

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
        ArrayList<Integer> usedId = new ArrayList<Integer>();
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
                       ArrayList<Integer> usedId,
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
            int vIdI = vi.getVertexId();

            logger.log(Level.FINEST, objId+"-"+recLab+"> vIdI= "+vIdI);

            if (usedId.contains(vIdI))
            {
                continue;
            }

            for (Vertex vj : compatMap.get(vi))
            {
                int vIdJ = vj.getVertexId();

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
     * Utility class to calculate and manage the alternative ring sizes 
     * achievable by formation of DENOPTIMRings.
     */
    private class RingSizeManager
    {
        // The graph representation of the INITIAL system. Note that
        // As we select candidate pairs of RCAs and define candidate 
        // DENOPTIMRings this DENOPTIMGraph will keep representing the 
        // initial system
        private DGraph graph;

        // Molecular representation of the current system
        private IAtomContainer mol;

        // Topological matrix: contains the number of bonds (shortest path) 
        // separating the two atoms having index i and j in the current system
        private int[][] topoMat;
        
        // List of Ring Closing Vertices (RCV as DENOPTIMVerex) each containing
        // an available Ring Closing Attractor (RCA)
        private ArrayList<Vertex> lstVert;
        
        /**
         * Size of the list of available RCAs
         */
        private int sz;

        // Compatibility matrix between pairs of RCAs in the current system.
        private boolean[][] compatibilityOfPairs;

        /**
         * List of weight factors used to control the likeliness of choosing 
         * rings of a given size for the current system
         */
        private ArrayList<Double> weigths;

        /**
         * List of flags defining if an RCA has been "used", i.e., not used to
         * make an actual chord, but used to make a plan to make a chord and
         * update the molecular representation accordingly.
         */
        private ArrayList<Boolean> done;

        // Map linking the list of vertices and atoms
        private Map<Vertex,ArrayList<Integer>> vIdToAtmId;

        // Parameters setting the bias for selecting rings of given size
        private ArrayList<Integer> ringSizeBias = settings.getRingSizeBias();
        
        /**
         * Definition of the fragment space
         */
        private FragmentSpace fragSpace = null;

        //---------------------------------------------------------------------

        public RingSizeManager(FragmentSpace fragSpace)
        {
            this.fragSpace = fragSpace;
        }

        //---------------------------------------------------------------------

        public void initialize(IAtomContainer origMol, DGraph graph)
                                                      throws DENOPTIMException
        {
            // Store current system
            this.graph = graph;
            try
            {
                mol = (IAtomContainer) origMol.clone();
            }
            catch (Throwable t)
            {
                throw new DENOPTIMException(t);
            }

            // Get the list of available RCAs
            lstVert = graph.getFreeRCVertices();
            sz = lstVert.size();

            // Define link between list of vertices and list of atoms
            vIdToAtmId = MoleculeUtils.getVertexToAtomIdMap(lstVert,mol);

            // Build topological matrix
            fillTopologicalMatrix(); 

            // Define compatibility of RCA pairs and weight factors
            calculateCompatibilityOfAllRCAPairs();

            // Initialize vector of flags
            done = new ArrayList<Boolean>(Collections.nCopies(sz,false));
            
        }

        //---------------------------------------------------------------------

        // NOTE: we don't really need the whole topological matrix, so the 
        // calculation of the shortest paths between the RCAs could be more
        // efficient

//TODO evaluate use of ShortestPath to fill only certain entries of topoMat

        private void fillTopologicalMatrix()
        {
            long startTime = System.nanoTime();
            topoMat = TopologicalMatrix.getMatrix(mol);
            long endTime = System.nanoTime();
            long duration = (- startTime + endTime) / (long) 1000.0;

            if (logger.isLoggable(Level.FINE))
            {
                StringBuilder sb = new StringBuilder();
                sb.append("TopoMat N: " + mol.getAtomCount() + " " + duration 
                        + " microsec." + NL);
                int n = mol.getAtomCount();
                sb.append("Topological matrix (n=" + n + ")"+NL);
                for (int i=0; i<n; i++)
                {
                    String l = " ";
                    for (int j=0; j<n; j++)
                    {
                        l = l + " " + topoMat[i][j];
                    }
                    sb.append(l+NL);
                }
                logger.log(Level.FINE, sb.toString());
            }
        }

        //---------------------------------------------------------------------

        // NOTE: this method considers the ring size bias and sets also 
        // the weight factors

        private void calculateCompatibilityOfAllRCAPairs() throws DENOPTIMException
        {
            weigths = new ArrayList<Double>(Collections.nCopies(sz, 0.0));
            compatibilityOfPairs = new boolean[sz][sz];
            for (int i=0; i<sz; i++)
            {
                Vertex vI = lstVert.get(i);
                IAtom atmI = null;
                RingClosingAttractor rcaI = null;
                boolean isAtmI = vI instanceof Fragment;
                if (isAtmI)
                {
                    atmI = mol.getAtom(vIdToAtmId.get(vI).get(0));
                
                    // Dealing with the possibility that RCV is the scaffold
                    if (mol.getConnectedAtomsList(atmI).size()==0)
                        continue;
                    
                    rcaI = new RingClosingAttractor(atmI,mol);
                    if (!rcaI.isAttractor())
                    {
                        String s = "Attempt to evaluate RCA pair compatibility "
                                   + "with a non-RCA end (" + atmI + ").";
                        throw new DENOPTIMException(s);
                    }
                }   
                
                for (int j=i+1; j<sz; j++)
                {
                    Vertex vJ = lstVert.get(j);
                    IAtom atmJ = null;
                    RingClosingAttractor rcaJ = null;
                    boolean isAtmJ = vJ instanceof Fragment;
                    if (isAtmJ)
                    {
                        atmJ = mol.getAtom(vIdToAtmId.get(vJ).get(0));
                        
                        rcaJ = new RingClosingAttractor(atmJ,mol);
                        if (!rcaJ.isAttractor())
                        {
                            String s = "Attempt to evaluate RCA pair compatibility "
                                       + "with a non-RCA end (" + atmJ + ").";
                            throw new DENOPTIMException(s);
                        }
                    }
                    
                    if ((isAtmI && !isAtmJ) || (!isAtmI && isAtmJ))
                    {
                        continue;
                    }
                    if (!isAtmI && !isAtmJ)
                    {
                        // Ring size is ignored when RCVs are empty vertexes
                        compatibilityOfPairs[i][j] = true;
                        compatibilityOfPairs[j][i] = true;
                        weigths.set(i, weigths.get(i) + 1.0);
                        weigths.set(j, weigths.get(j) + 1.0);
                        continue;
                    }
                    
                    if (rcaI.isCompatible(rcaJ) 
                            && evaluateRCVPair(vI,vJ,graph,fragSpace))
                    {
                        //TODO: evaluate the use of ShortestPath instead of this
                        int ringSize = topoMat[vIdToAtmId.get(vI).get(0)]
                                              [vIdToAtmId.get(vJ).get(0)] - 1;
                        int szFct = 0;
                        if (ringSize < settings.getMaxRingSize())
                        {
                            szFct = ringSizeBias.get(ringSize);
                        }
                        if (szFct > 0)
                        {
                            compatibilityOfPairs[i][j] = true;
                            compatibilityOfPairs[j][i] = true;
                            weigths.set(i, weigths.get(i) + szFct);
                            weigths.set(j, weigths.get(j) + szFct);
                        }

                        logger.log(Level.FINE, " i:" + i + " j:" + j + " size:" 
                                + ringSize + " factors:" + weigths);
                    }
                }
            }
            
            if (logger.isLoggable(Level.FINE))
            {
                StringBuilder sb = new StringBuilder();
                sb.append("RCV pairs compatibility (ring size-biased):"+NL);
                for (int i=0; i<sz;i++)
                {
                    String l = " ";
                    for (int j=0; j<sz; j++)
                    {
                        String p = "0";
                        if (compatibilityOfPairs[i][j])
                            p = "1";
                        l = l + " " + p;
                    }
                    sb.append(l+NL);
                }
                logger.log(Level.FINE, sb.toString());
            }
        }

        //---------------------------------------------------------------------

        public ArrayList<Vertex> getRSBiasedListOfCandidates()
        {
            ArrayList<Vertex> wLst = new ArrayList<Vertex>();
            for (int i=0; i<sz; i++)
            {
                if (done.get(i))
                {
                    continue;
                }
                Vertex v = lstVert.get(i);
                for (int j=0; j<weigths.get(i); j++)
                {
                    wLst.add(v);
                }
            }

            logger.log(Level.FINE, "Ring size-biased list of RCVs:" + wLst);
            
            return wLst;
        }

        //---------------------------------------------------------------------

        public ArrayList<Vertex> getRSBiasedListOfCandidates(
                                                             Vertex vI)
        {
            int i = lstVert.indexOf(vI);
            ArrayList<Vertex> wLst = new ArrayList<Vertex>();
            for (int j=0; j<sz; j++)
            {
                if (done.get(j) || !compatibilityOfPairs[i][j])
                {
                    continue;
                }

                Vertex vJ = lstVert.get(j);
                
                if (vI instanceof EmptyVertex && vJ instanceof EmptyVertex)
                {
                    wLst.add(vJ);
                    continue;
                }

                int ringSize = topoMat[vIdToAtmId.get(vI).get(0)]
                                      [vIdToAtmId.get(vJ).get(0)] - 1;
                
                // The likeliness of picking this RCS is given by the ring-size
                // factor, which accounts for the requested bias towards specific
                // ring sizes. The larger the bias, the higher the number of
                // slots occupied by vJ in the list of possible partners, thus
                // the larger the likeliness of picking it when randomly
                // choosing the partner for vI.
                int szFct = 0; //This is the ring-size factor
                if (ringSize < settings.getMaxRingSize())
                {
                    szFct = ringSizeBias.get(ringSize);
                }
                
                // TODO: here we could consider biasing against crowding, but
                // the parameters defining what crowding is are defined in 
                // GAParameters, so we would loose generality of this class.
                
                for (int z=0; z<szFct; z++)
                {
                    wLst.add(vJ);
                }
            }

            logger.log(Level.FINE, "Ring size-biased list of RCVs for " + vI 
                                                               + ": " + wLst);

            return wLst;
        }

        //---------------------------------------------------------------------

        public void addRingClosingBond(Vertex vI, Vertex vJ)
                                                       throws DENOPTIMException
        {
            // Check validity of assumption: RCV contain only one IAtom
            if (vIdToAtmId.get(vI).size()!=1 || vIdToAtmId.get(vJ).size()!=1)
            {
                String s = "Attempt to make ring closing bond between "
                           + "multi-atom Ring Closing Vertices (RCV). "
                           + "For now only single-atom RCVs are expected in "
                           + "this implementation.";
                throw new DENOPTIMException(s);
            }

            // Identify atoms to be bound and make the ring closing bond
            IAtom srcI = mol.getConnectedAtomsList(
                               mol.getAtom(vIdToAtmId.get(vI).get(0))).get(0);
            IAtom srcJ = mol.getConnectedAtomsList(
                               mol.getAtom(vIdToAtmId.get(vJ).get(0))).get(0);

            // Assuming that APClasses on both sides agree on bond order
            // and that vI and vJ are proper RCVs
            
            BondType bndTyp = vI.getEdgeToParent().getBondType();
            if (bndTyp.hasCDKAnalogue())
            {
                IBond bnd = new Bond(srcI,srcJ);
                bnd.setOrder(bndTyp.getCDKOrder());
                mol.addBond(bnd);
            } else {
                logger.log(Level.FINE,"WARNING! Attempt to add ring closing bond "
                        + "did not add any actual chemical bond because the "
                        + "bond type of the chord is '" + bndTyp +"'.");
            }
            
            logger.log(Level.FINEST, " ==> UPDATING RingSizeManager <==");

            // Update this RingSizeManager
            fillTopologicalMatrix();
            calculateCompatibilityOfAllRCAPairs();
        }

        //---------------------------------------------------------------------

        public void setVertexAsDone(Vertex v)
        {
            done.set(lstVert.indexOf(v),true);
        }

        //---------------------------------------------------------------------

        public boolean getCompatibilityOfPair(Vertex vI, 
                                                             Vertex vJ)
        {
            int i = lstVert.indexOf(vI); 
            int j = lstVert.indexOf(vJ);
            return compatibilityOfPairs[i][j];
        }

        //---------------------------------------------------------------------
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
     * Method to analyze a pair of vertices and evaluate whether they respect 
     * the criteria for being the two ends of a candidate closable chain: a 
     * pair of Ring Closing Vertices (RCV).
     *
     * @param vI first vertex
     * @param vJ second vertex
     * @param graph the graph representation
     * @return <code>true</code> is the path satisfies the criteria
     */

    private boolean evaluateRCVPair(Vertex vI, Vertex vJ,
            DGraph molGraph, FragmentSpace fragSpace)
                    throws DENOPTIMException
    {
        String s = "Evaluation of RCV pair " + vI + " " + vJ + ": ";

        // Get details on the first vertex (head)
        int vIdI = vI.getVertexId();
        Edge edgeI = molGraph.getEdgeWithParent(vIdI);
        int srcApIdI = edgeI.getSrcAPID();
        Vertex pvI = molGraph.getParent(vI);
        AttachmentPoint srcApI = pvI.getAttachmentPoints().get(
                                                                    srcApIdI);
        int srcAtmIdI = srcApI.getAtomPositionNumber();
        APClass parentAPClsI = edgeI.getSrcAPClass();

        // Get details on the second vertex (tail)
        int vIdJ = vJ.getVertexId();
        Edge edgeJ = molGraph.getEdgeWithParent(vIdJ);
        int srcApIdJ = edgeJ.getSrcAPID();
        Vertex pvJ = molGraph.getParent(vJ);
        AttachmentPoint srcApJ =pvJ.getAttachmentPoints().get(srcApIdJ);
        int srcAtmIdJ = srcApJ.getAtomPositionNumber();
        APClass parentAPClsJ = edgeJ.getSrcAPClass();
        
        // exclude if no entry in RC-Compatibility map
        if (!fragSpace.getRCCompatibilityMatrix().containsKey(parentAPClsI))
        {
            logger.log(Level.FINE, s + "RC-CPMap does not contain class (I) "
                        + parentAPClsI + " " + parentAPClsI.hashCode());
            return false;
        }
        ArrayList<APClass> compatClassesI = fragSpace.getRCCompatibilityMatrix()
                .get(parentAPClsI);

        // exclude if no entry in RC-Compatibility map
        if (!fragSpace.getRCCompatibilityMatrix().containsKey(parentAPClsJ))
        {
            logger.log(Level.FINE, s + "RC-CPMap does not contain class (J) "
                        + parentAPClsJ + " " + parentAPClsJ.hashCode());
            return false;
        }
        ArrayList<APClass> compatClassesJ = fragSpace.getRCCompatibilityMatrix()
                .get(parentAPClsJ);

        // exclude loops included within a single vertex 
        if (vI == vJ)
        {
            logger.log(Level.FINE, s + "vI same as vJ: loop not allowed!");
            return false;
        }

        // exclude pairs of RCA-vertices having same src atom
        if (pvI == pvJ && srcAtmIdI == srcAtmIdJ)
        {
            logger.log(Level.FINE, s + "Same src: " + pvI + " " + pvJ
                                  + " " + srcAtmIdI + " " + srcAtmIdJ);
            return false;
        }

        // exclude paths that do not connect APClass compatible ends
        // NOTE that in ring closures the CPMap is symmetric, this
        // also implies that CPMap for ring closure may be different
        // from standard CPMap        
        if (!(compatClassesI.contains(parentAPClsJ) ||
              compatClassesJ.contains(parentAPClsI)))
        {
            logger.log(Level.FINE,s + "APClass not compatible "
                                + parentAPClsJ);
            return false;
        }

        logger.log(Level.FINE, s + "all criteria satisfied.");
        
        return true;
    }

//-----------------------------------------------------------------------------

    /**
     * Method to evaluate the closability of a single path in a graph
     * representing a molecule. The criteria defining the closability
     * condition are controlled by the closability evaluation mode
     * (see <code>RingClosureParameters</code>).
     * @param subGraph the subgraph representing the path in the graph
     * @param mol the molecule corresponding to the graph. This
     * <code>IAtomContainer</code> is only used to provide the molecular
     * constitution and does not require 3D coordinates.
     * @return <code>true</code> is the path corresponds to a closable chain
     */

    private boolean evaluatePathClosability(PathSubGraph subGraph,
                                  IAtomContainer mol) throws DENOPTIMException
    {
        boolean closable = false;
        switch (settings.getClosabilityEvalMode())
        {
            case -1:
            	closable = true; // ring size has been evaluated before
            	break;
            case 0:
                closable = evaluateConstitutionalClosability(subGraph,mol);
                break;
            case 1:
                closable = evaluate3DPathClosability(subGraph,mol);
                break;
            case 2:
                closable = evaluateConstitutionalClosability(subGraph,mol) &&
                           evaluate3DPathClosability(subGraph,mol);
                break;
            default:
                String s = "Unrecognized closability evaluation mode";
                throw new DENOPTIMException(s);
        }
        return closable;
    }

//-----------------------------------------------------------------------------

    /**
     * Method to evaluate the closability of a single path considering only
     * its constitution.
     * @param subGraph the subgraph representing the path in the graph
     * @return <code>true</code> is the path is constitutionally closable
     */

    private boolean evaluateConstitutionalClosability(PathSubGraph subGraph,
                                  IAtomContainer inMol) throws DENOPTIMException
    {
        settings.getLogger().log(Level.FINE, "Evaluating constitutional "
                + "closability of path: " + subGraph.getVertecesPath());

        // Get a working copy of the molecular container
        IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
        IAtomContainer mol = builder.newAtomContainer();
        try
        {
            mol = inMol.clone();
        }
        catch (CloneNotSupportedException e)
        {
            throw new DENOPTIMException(e);
        }
        MoleculeUtils.removeRCA(mol);
        
        // Identify atoms of molecular representation that correspond to
        // this path of vertices
        Map<Vertex,ArrayList<Integer>> vIdToAtmId =
                MoleculeUtils.getVertexToAtomIdMap(
                        (ArrayList<Vertex>) subGraph.getVertecesPath(),
                        mol);
        List<Integer> atmIdsInVrtxPath = new ArrayList<Integer>();
        for (Vertex v : subGraph.getVertecesPath())
        {
            atmIdsInVrtxPath.addAll(vIdToAtmId.get(v));
        }

        // Find atoms to remove: keep only the atoms that belong to the 
        // vertex path and their neighbours
        ArrayList<IAtom> toRemove = new ArrayList<IAtom>();
        for (int i=0; i<mol.getAtomCount(); i++)
        {
            if (!atmIdsInVrtxPath.contains(i))
            {
                IAtom candAtm = mol.getAtom(i);
                boolean isNeighbour = false;
                List<IAtom> nbrs = mol.getConnectedAtomsList(candAtm);
                for (IAtom nbrAtm : nbrs)
                {
                    if (atmIdsInVrtxPath.contains(mol.indexOf(nbrAtm)))
                    {
                        isNeighbour = true;
                        break;
                    }
                }
                if (!isNeighbour)
                {
                    toRemove.add(candAtm);
                }
            }
        }
        // Deal with RCAs: head and tail vertices
        IAtom atmH = mol.getAtom(vIdToAtmId.get(subGraph.getHeadVertex()).get(0));
        IAtom atmT = mol.getAtom(vIdToAtmId.get(subGraph.getTailVertex()).get(0));
        IAtom srcH = mol.getConnectedAtomsList(atmH).get(0);
        IAtom srcT = mol.getConnectedAtomsList(atmT).get(0);
        int iSrcH = mol.indexOf(srcH);
        int iSrcT = mol.indexOf(srcT);
        toRemove.add(atmH);
        toRemove.add(atmT);
        
        BondType bndTyp = subGraph.getEdgesPath().get(0).getBondType();
        if (bndTyp.hasCDKAnalogue())
        {
            mol.addBond(iSrcH, iSrcT, bndTyp.getCDKOrder());
        } else {
            settings.getLogger().log(Level.WARNING, 
                    "WARNING! Attempt to add ring closing bond "
                    + "did not add any actual chemical bond because the "
                    + "bond type of the chord is '" + bndTyp +"'.");
        }

        // Remove atoms
        for (IAtom a : toRemove)
        {
            mol.removeAtom(a);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Molecular representation of path includes:");
        for (IAtom a : mol.atoms())
        {
            sb.append("  " + a.getSymbol() + mol.indexOf(a) + " " 
                    + a.getProperties());
        }
        settings.getLogger().log(Level.FINEST, sb.toString());

        boolean closable = false;

        // Evaluate requirement based on elements contained in the ring
        boolean spanRequiredEls = false;
        Set<String> reqRingEl = settings.getRequiredRingElements();
        if (reqRingEl.size() != 0)
        {
            // Prepare shortest atom path 
            List<IAtom> atomsPath = new ArrayList<IAtom>();
            try {
                ShortestPaths sp = new ShortestPaths(mol, srcH);
                atomsPath = new ArrayList<IAtom>(Arrays.asList(sp.atomsTo(srcT)));
            } catch (Throwable t) {
                throw new DENOPTIMException("PathTools Exception: " + t);
            }

            // Look for the required elements
            String missingEl = "";
            for (String el : reqRingEl)
            {
                missingEl = el;
                for (IAtom a : atomsPath)
                {
                    if (a.getSymbol().equals(el))
                    {
                        spanRequiredEls = true;
                        break;
                    }
                }
                if (!spanRequiredEls)
                {
                    break;
                }
            }
            if (!spanRequiredEls)
            {
                settings.getLogger().log(Level.FINER, 
                        "Candidate ring doesn't involve " + missingEl);
                return false;
            }
            closable = true;
        }

        // Try to find a match for any of the SMARTS queries
        Map<String,String> smarts = settings.getConstitutionalClosabilityConds();
        if (smarts.size() != 0)
        {
            closable = false;
            ManySMARTSQuery msq = new ManySMARTSQuery(mol,smarts);
            if (msq.hasProblems())
            {
                String msg = "Attempt to match SMARTS for "
                             + "constitution-based ring-closability conditions "
                             + "returned an error! Ignoring " + msq.getMessage();
                settings.getLogger().log(Level.WARNING,msg);
            }
            for (String name : smarts.keySet())
            {
                if (msq.getNumMatchesOfQuery(name) > 0)
                {
                    settings.getLogger().log(Level.FINER,
                            "Candidate closable path matches constitutional "
                            + "closability criterion: " + smarts.get(name));
                    closable = true;
                    break;
                }
            }
        }

        settings.getLogger().log(Level.FINE, 
                "Contitutional closability: " + closable);
        return closable;
    }

//-----------------------------------------------------------------------------

    /**
     * Method to evaluate the closability of a single path in a graph 
     * representing a molecule. Since this is a computationally demanding
     * task, this method makes use of serialized data that is updated 
     * on the fly.
     * @param subGraph the subgraph representing the path in the graph
     * @param mol the molecule corresponding to the graph. This
     * <code>IAtomContainer</code> is only used to provide the molecular
     * constitution and does not require 3D coordinates.
     * @return <code>true</code> is the path corresponds to a closable chain
     */

    private boolean evaluate3DPathClosability(PathSubGraph subGraph, 
            IAtomContainer mol) throws DENOPTIMException
    {
        String chainId = subGraph.getChainID();
        logger.log(Level.FINE, "Evaluating 3D closability of path: " 
                            + subGraph.getVertecesPath()+" ChainID: "+chainId);
        
        RingClosuresArchive rca = settings.getRingClosuresArchive();
        
        RingClosingConformations rcc;
        boolean closable = false;
        
        String foundID = rca.containsChain(subGraph);
        if (foundID != "")
        {
            // Get all info from archive
            closable = rca.getClosabilityOfChain(foundID);
            rcc = rca.getRCCsOfChain(foundID);
            if (settings.checkInterdependentChains() 
                    && settings.doExhaustiveConfSrch())
            {
                subGraph.makeMolecularRepresentation(mol,false);
                subGraph.setRCC(rcc);        
            }
        }
        else
        {
            // Need to generate 3D molecular representation
            subGraph.makeMolecularRepresentation(mol, true);
            List<IAtom> atomsPath = subGraph.getAtomPath();
            List<IBond> bondsPath = subGraph.getBondPath();

            // Define rotatability
            ArrayList<Boolean> rotatability = new ArrayList<Boolean>();
            for (int i=0; i < bondsPath.size(); i++)
            {
                IBond bnd = bondsPath.get(i);
                Object rotFlag = bnd.getProperty(
                                         DENOPTIMConstants.BONDPROPROTATABLE);
                if (rotFlag == null || !Boolean.valueOf(rotFlag.toString()))
                {
                    rotatability.add(false);
                }
                else
                {
                    rotatability.add(true);
                }
            }
            logger.log(Level.FINE, "Rotatability: "+rotatability); 
            
            // Define initial values of dihedrals
            // NOTE: these angles are not calculated on the atoms of the chain,
            //       but on the reference points that are unique for each bond
            //       This should allow to compare conformations of different
            //       chains that share one or more bonds
            ArrayList<ArrayList<Point3d>> dihRefs = 
                                               subGraph.getDihedralRefPoints();
            if (dihRefs.size() != rotatability.size()-2)
            {
                throw new DENOPTIMException("Number of bonds and number of "
                        + "dihidrals angles are inconsistent in PathSubGraph."
                        + " Contact the author.");
            }

            // find ring closing conformations
            ArrayList<ArrayList<Double>> closableConfs = 
                                        new ArrayList<ArrayList<Double>>();
            closable = RingClosureFinder.evaluateClosability(atomsPath,
                                                         rotatability,
                                                         dihRefs,
                                                         closableConfs,
                                                         settings);

            // store in object graph
            rcc = new RingClosingConformations(chainId, closableConfs);
            subGraph.setRCC(rcc);

            // put ring-closure information in archive for further use
            rca.storeEntry(chainId,closable,rcc);
        }

        logger.log(Level.FINE, "Path closablility: "+closable);
        
        return closable;
    }
    
//-----------------------------------------------------------------------------

    /**
     * Evaluates the combination of a DENOPTIMGraph and a set of DENOPTIMRings
     * and decides whether it's a proper candidate for the generation of a
     * chelating ligand.
     * @return <code>true</code> it this system is a good candidate
     */

    public boolean checkChelatesGraph(DGraph molGraph,
                                      List<Ring> ringsSet)
    {
        logger.log(Level.FINE, "Checking conditions for chelates");

//TODO: here we assume that the scaffold is a metal and the first layer of
// vertices (level = 0) are the coordinating atoms.
// Also, the APclass of the ap connecting the candidate orphan is hard coded!
// This is a temporary solution. Need a general approach and tunable by
// options/parameters

        for (Vertex vert : molGraph.getVertexList())
        {
            int vId = vert.getVertexId();
            
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
