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

package denoptim.rings;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import org.apache.commons.math3.random.MersenneTwister;
import org.openscience.cdk.Bond;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.graph.PathTools;
import org.openscience.cdk.graph.matrix.TopologicalMatrix;

import javax.vecmath.Point3d;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.APClass;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMEdge;
import denoptim.molecule.DENOPTIMEdge.BondType;
import denoptim.molecule.DENOPTIMFragment.BBType;
import denoptim.molecule.DENOPTIMFragment;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMRing;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.utils.DENOPTIMMoleculeUtils;
import denoptim.utils.ManySMARTSQuery;
import denoptim.utils.ObjectPair;
import denoptim.utils.RandomUtils;
import denoptim.utils.RingClosingUtils;

import java.util.logging.Level;


/**
 * This is a tool to identify and manage vertices' connections not included
 * in the DENOPTIMGraph, which is a spanning tree, thus connections that 
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
     * Reference to libraries of fragments
     */
    
    private ArrayList<DENOPTIMVertex> libScaff;
    private ArrayList<DENOPTIMVertex> libFrag;
    private ArrayList<DENOPTIMVertex> libCap;

    /**
     * AP-class compatibility matrix for ring closures
     */
    private HashMap<APClass, ArrayList<APClass>> rcCPMap;

    /**
     * variables needed by recursive methods
     */
    private int recCount = 0;
    private int maxLng = 0;

    /**
     * Verbosity level
     */
    private int verbosity = RingClosureParameters.getVerbosity();
    
//-----------------------------------------------------------------------------

    /**
     * Constructor from data structure. 
     * @param libScaff the library of scaffolds
     * @param libFrag the library of fragments
     * @param libCap the library of capping groups
     */

    public CyclicGraphHandler(ArrayList<DENOPTIMVertex> libScaff,
                               ArrayList<DENOPTIMVertex> libFrag,
                               ArrayList<DENOPTIMVertex> libCap,
                               HashMap<APClass, ArrayList<APClass>> hashMap)
    {
        this.libScaff = libScaff;
        this.libFrag = libFrag;
        this.libCap = libCap;
        this.rcCPMap = hashMap;
    }
    
//-----------------------------------------------------------------------------

    /**
     * Identifies a random combination of ring closing paths and returns it as 
     * list of DENOPTIMRings ready to be appended to a DENOPTIMGraph.
     * @param inMol the molecule
     * @param molGraph the molecular graph
     * @return the selected combination of closable paths 
     */

    public Set<DENOPTIMRing> getRandomCombinationOfRings(IAtomContainer inMol,
                                                       DENOPTIMGraph molGraph)
                                                      throws DENOPTIMException
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

        MersenneTwister randomNumGenerator = RandomUtils.getRNG();

        // Get manager of ring size problems
        RingSizeManager rsm = new RingSizeManager();
        rsm.initialize(mol, molGraph);

        // Get weighted list of RCVs
        ArrayList<DENOPTIMVertex> wLstVrtI = rsm.getRSBiasedListOfCandidates();
        
        // Randomly choose the compatible combinations of RCAs and store them
        // as DENOPTIMRings. 
        Set<DENOPTIMRing> combOfRings = new HashSet<DENOPTIMRing>();
        while (wLstVrtI.size() > 0)
        {
            int vIdI = randomNumGenerator.nextInt(wLstVrtI.size());
            DENOPTIMVertex vI = wLstVrtI.get(vIdI);

            if (verbosity > 1)
            {
                System.out.println("Pick I: "+vIdI+" "+vI);
            }
            wLstVrtI.removeAll(Collections.singleton(vI));

            // Create vector of possible choices for second RCV
            ArrayList<DENOPTIMVertex> wLstVrtJ = 
                                          rsm.getRSBiasedListOfCandidates(vI); 
            while (wLstVrtJ.size() > 0)
            {
                int vIdJ = randomNumGenerator.nextInt(wLstVrtJ.size());
                DENOPTIMVertex vJ = wLstVrtJ.get(vIdJ);
                wLstVrtJ.removeAll(Collections.singleton(vJ));

                if (verbosity > 1)
                {
                    System.out.println("Pick J: "+vIdJ+" "+vJ);
                    System.out.println("vI-vJ: "+vI+" "+vJ);
                }

                PathSubGraph path = new PathSubGraph(vI,vJ,molGraph);
                if (evaluatePathClosability(path, inMol))
                {
                    ArrayList<DENOPTIMVertex> arrLst = 
                                              new ArrayList<DENOPTIMVertex>();
                    arrLst.addAll(path.getVertecesPath());                    
                    DENOPTIMRing ring = new DENOPTIMRing(arrLst);

                    BondType bndTypI = 
                           molGraph.getIncidentEdges(vI).get(0).getBondType();
                    BondType bndTypJ = 
                           molGraph.getIncidentEdges(vJ).get(0).getBondType();
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
                    rsm.addRingClosingBond(vI,vJ);
                    rsm.setVertedAsDone(vJ);
                    break;
                }
                else
                {
                    if (verbosity > 1)
                    {
                        System.out.println("Not suitable for ring closure.");
                    }
                }
            }
            rsm.setVertedAsDone(vI);
        }


        if (verbosity > 0)
        {
            System.out.println("Random combOfRings: "+combOfRings);
        }

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

    public ArrayList<Set<DENOPTIMRing>> getPossibleCombinationOfRings(
                                                            IAtomContainer mol,
                                                        DENOPTIMGraph molGraph)
                                                       throws DENOPTIMException
    {
        // All the candidate paths 
        Map<ObjectPair,PathSubGraph> allGoodPaths = 
                                        new HashMap<ObjectPair,PathSubGraph>();
        ArrayList<DENOPTIMVertex> rcaVertLst = molGraph.getFreeRCVertices();
        
        // Get manager of ring size problems
        RingSizeManager rsm = new RingSizeManager();
        rsm.initialize(mol, molGraph);

        // identify compatible pairs of RCA vertices
        Map<DENOPTIMVertex,ArrayList<DENOPTIMVertex>> compatMap =
                       new HashMap<DENOPTIMVertex,ArrayList<DENOPTIMVertex>>();
        for (int i=0; i<rcaVertLst.size(); i++)
        {
            DENOPTIMVertex vI = rcaVertLst.get(i);
            for (int j=i+1; j<rcaVertLst.size(); j++)
            {
                DENOPTIMVertex vJ = rcaVertLst.get(j);
                if (!rsm.getCompatibilityOfPair(vI,vJ))
                {
                    if (verbosity > 1)
                    {
                        System.out.println("Rejecting RC-incompatible pair " 
                                + vI + " "+ vJ);
                    }
                    continue;
                }

                // make the new candidate RCA pair
                PathSubGraph subGraph = new PathSubGraph(vI,vJ,molGraph);
                boolean keepRcaPair = evaluatePathClosability(subGraph, mol);

                if (!keepRcaPair)
                {
                    if (verbosity > 1)
                    {
                        System.out.println("Rejecting RCA pair");
                    }
                    continue;
                }

                // finally store this pair as a compatible pair
                if (verbosity > 1)
                {
                    System.out.println("All compatibility criteria satisfied");
                    System.out.println("Storing verified RCA pair");
                }

                // Store the information that the two vertex are compatible
                if (compatMap.containsKey(vI))
                {
                    compatMap.get(vI).add(vJ);
                }
                else
                {
                    ArrayList<DENOPTIMVertex> lst =
                                            new ArrayList<DENOPTIMVertex>();
                    lst.add(vJ);
                    compatMap.put(vI,lst);
                }
                if (compatMap.containsKey(vJ))
                {
                    compatMap.get(vJ).add(vI);
                }
                else
                {
                    ArrayList<DENOPTIMVertex> lst =
                                            new ArrayList<DENOPTIMVertex>();
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

        if (verbosity > 1)
        {
            System.out.println("Compatibility Map for RCAs: ");
            System.out.println(compatMap);
        }

        // Identify paths that share bonds (interdependent paths)
        Map<IBond,Set<PathSubGraph>> interdepPaths =
                                new HashMap<IBond,Set<PathSubGraph>>();
        if (RingClosureParameters.checkInterdependentChains())
        {
            for (PathSubGraph rpA : allGoodPaths.values())
            {
                for (PathSubGraph rpB : allGoodPaths.values())
                {
                    if (rpA == rpB)
                    {
                        continue;
                    }
    
                    DENOPTIMVertex hA = rpA.getHeadVertex();
                    DENOPTIMVertex tA = rpA.getTailVertex();
                    DENOPTIMVertex hB = rpB.getHeadVertex();
                    DENOPTIMVertex tB = rpB.getTailVertex();
    
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
                                Set<PathSubGraph> paths = 
                                                   new HashSet<PathSubGraph>();
                                paths.add(rpA);
                                paths.add(rpB);
                                interdepPaths.put(bnd,paths);
                            }
                        }
                    }
                }
            }
            if (verbosity > 1)
            {
                for (IBond bnd : interdepPaths.keySet())
                {
                    System.out.println("Interdependent paths for bond " + bnd);
                    Set<PathSubGraph> sop = interdepPaths.get(bnd);
                    for (PathSubGraph p : sop)
                    {
                        System.out.println(" vA: "+p.getHeadVertex()
                                          +" vB: "+p.getTailVertex());
                    }
                }
            }
        }

        // Generate all combinations of compatible, closable paths
        ArrayList<ObjectPair> lstPairs = new ArrayList<ObjectPair>();
        ArrayList<Integer> usedId = new ArrayList<Integer>();
        ArrayList<DENOPTIMVertex> sortedKeys = new ArrayList<DENOPTIMVertex>();
        for (DENOPTIMVertex keyVert : compatMap.keySet())
        {
            sortedKeys.add(keyVert);
        }

        // All possible ring closing paths will be stored here
        ArrayList<Set<DENOPTIMRing>> allCombsOfRings =
                                     new ArrayList<Set<DENOPTIMRing>>();
        
        combineCompatPathSubGraphs(0,
                sortedKeys,
                compatMap,
                lstPairs,
                usedId,
                allGoodPaths,
                interdepPaths,
                allCombsOfRings);

        if (verbosity > 0)
        {
            System.out.println("All possible combination of rings: " + 
                                                             allCombsOfRings);
        }

        return allCombsOfRings;
    }

//-----------------------------------------------------------------------------

    /**
     * Recursive method to identify all the combination of rings.
     */

    private boolean combineCompatPathSubGraphs(int ii0,
                       ArrayList<DENOPTIMVertex> sortedKeys,
                       Map<DENOPTIMVertex,ArrayList<DENOPTIMVertex>> compatMap,
                       ArrayList<ObjectPair> lstPairs,
                       ArrayList<Integer> usedId,
                       Map<ObjectPair,PathSubGraph> allGoodPaths,
                       Map<IBond,Set<PathSubGraph>> interdepPaths,
                       ArrayList<Set<DENOPTIMRing>> allCombsOfRings)
                                                      throws DENOPTIMException
    {
        int objId = this.hashCode();
        String recLab = new String(new char[recCount]).replace("\0", "-");
        boolean debug = false;
        if (debug)
        {
            System.out.println(objId+"-"+recLab+"> Begin of new recursion: "+recCount);
            System.out.println(objId+"-"+recLab+"> sortedKeys= "+sortedKeys);
            System.out.println(objId+"-"+recLab+"> usedId= "+usedId);
            System.out.println(objId+"-"+recLab+"> ii0= "+ii0);
            System.out.println(objId+"-"+recLab+"> lstPairs= "+lstPairs);
            System.out.println(objId+"-"+recLab+"> compatMap= "+compatMap);
            System.out.println(objId+"-"+recLab+"> allCombsOfRings");
            for (Set<DENOPTIMRing> ringSet : allCombsOfRings)
            {
                System.out.println("        "+ringSet);
            }
        }

        boolean inFound = false;
        boolean addedNew = false;
        for (int ii=ii0; ii<sortedKeys.size(); ii++)
        {
            DENOPTIMVertex vi = sortedKeys.get(ii);
            int vIdI = vi.getVertexId();

            if (debug)
                System.out.println(objId+"-"+recLab+"> vIdI= "+vIdI);

            if (usedId.contains(vIdI))
            {
                continue;
            }

            for (DENOPTIMVertex vj : compatMap.get(vi))
            {
                int vIdJ = vj.getVertexId();

                if (debug)
                    System.out.println(objId+"-"+recLab+"> vIdJ= "+vIdJ);

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

                if (debug)
                    System.out.println(objId+"-"+recLab+"> lstPairs.size() & maxLng= "+lstPairs.size() +" " +maxLng); 

                if (!inFound && lstPairs.size() == maxLng)
                {
                    if (debug)
                        System.out.println(objId+"-"+recLab+"> in A");

                    boolean closable = true;
                    if (RingClosureParameters.checkInterdependentChains() &&
                               hasInterdependentPaths(lstPairs, interdepPaths))
                    {
                        closable = checkClosabilityOfInterdependentPaths(
                                                        lstPairs,
                                                        interdepPaths,
                                                        allGoodPaths);
                    }
                    if (closable)
                    {
                        if (debug)
                            System.out.println(objId+"-"+recLab+"> in B");

                        Set<DENOPTIMRing> ringsComb = 
                                                   new HashSet<DENOPTIMRing>();
                        for (ObjectPair opFinal : lstPairs)
                        {
                            PathSubGraph path = allGoodPaths.get(opFinal);
                            ArrayList<DENOPTIMVertex> arrLst = 
                                               new ArrayList<DENOPTIMVertex>();
                            arrLst.addAll(path.getVertecesPath());

                            DENOPTIMRing ring = new DENOPTIMRing(arrLst);

                            List<DENOPTIMEdge> es = path.getEdgesPath();
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

                            if (debug)
                                System.out.println(objId+"-"+recLab+"> added ringComb: "+ring);
                        }

                        boolean notNewCmb = false;
                        for(Set<DENOPTIMRing> oldCmb : allCombsOfRings)
                        {
                            if (debug)
                            {
                                System.out.println(objId+"-"+recLab+"> Comparing ring sets: ");
                                System.out.println("o> old: "+oldCmb);
                                System.out.println("o> new: "+ringsComb);
                            }

                            notNewCmb = RingClosingUtils.areSameRingsSet(oldCmb,
                                                                     ringsComb);
                            
                            if (debug)
                                System.out.println("o> result: "+notNewCmb);

                            if (notNewCmb)
                            {
                                break;
                            }
                        }

                        if (!notNewCmb)
                        {
                            if (debug)
                                System.out.println(objId+"-"+recLab+"> addinf to all combs of ring.");

                            allCombsOfRings.add(ringsComb);
                            addedNew = true;
                        }
                    }
                }
                if (!inFound)
                {
                    if (debug)
                        System.out.println(objId+"-"+recLab+"> in C");

                    ArrayList<ObjectPair> toDel = new ArrayList<ObjectPair>();
                    for (int ir = recCount; ir<lstPairs.size(); ir++)
                    {
                        ObjectPair opToRemove = lstPairs.get(ir);
                        DENOPTIMVertex delVA = (DENOPTIMVertex) 
                                                        opToRemove.getFirst();
                        DENOPTIMVertex delVB = (DENOPTIMVertex) 
                                                        opToRemove.getSecond();
                        usedId.remove(usedId.indexOf(delVA.getVertexId()));
                        usedId.remove(usedId.indexOf(delVB.getVertexId()));
                        toDel.add(opToRemove);
                    }
                    lstPairs.removeAll(toDel);

                    if (debug)
                    {
                        System.out.println(objId+"-"+recLab+"> in C: after removal usedId: "+usedId);
                        System.out.println(objId+"-"+recLab+"> in C: after removal lstPairs: "+lstPairs);
                    }
                }

                if (lstPairs.contains(op))
                {
                    if (debug)
                        System.out.println(objId+"-"+recLab+"> in D");

                    lstPairs.remove(op);
                    usedId.remove(usedId.indexOf(vIdI));
                    usedId.remove(usedId.indexOf(vIdJ));

                    if (debug)
                    {
                        System.out.println(objId+"-"+recLab+"> in D: after removal usedId: "+usedId);
                        System.out.println(objId+"-"+recLab+"> in D: after removal lstPairs: "+lstPairs);
                    }
                }
            }
        }
        
        if (debug)
            System.out.println(objId+"-"+recLab+"> returning= "+addedNew);

        return addedNew;
    }

//-----------------------------------------------------------------------------

    /**
     * Checks whether the combination of RCA's pairs leads to interdependent 
     * paths, that is, paths that share one or more bonds. 
     */

    private boolean hasInterdependentPaths(ArrayList<ObjectPair> lstPairs,
                                    Map<IBond,Set<PathSubGraph>> interdepPaths)
    {
        boolean result = false;
        
        for (IBond bnd : interdepPaths.keySet())
        {
            Set<PathSubGraph> psgSet = interdepPaths.get(bnd);
            for (PathSubGraph psg : psgSet)
            {
                DENOPTIMVertex va1 = psg.getHeadVertex();
                DENOPTIMVertex va2 = psg.getTailVertex();

                for (ObjectPair op : lstPairs)
                {
                    DENOPTIMVertex vb1 = (DENOPTIMVertex) op.getFirst();
                    DENOPTIMVertex vb2 = (DENOPTIMVertex) op.getSecond();
                
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
                                Map<IBond,Set<PathSubGraph>> interdepPaths,
                                Map<ObjectPair,PathSubGraph> allGoodPaths)
    {
        // Identify the interdependent sets of paths
        Set<ArrayList<ObjectPair>> listOfIntrDepPaths = 
                                          new HashSet<ArrayList<ObjectPair>>();
        for (IBond bnd : interdepPaths.keySet())
        {
            ArrayList<ObjectPair> locSop = new ArrayList<ObjectPair>();
            Set<PathSubGraph> psgSet = interdepPaths.get(bnd);
            for (PathSubGraph psg : psgSet)
            {
                DENOPTIMVertex va1 = psg.getHeadVertex();
                DENOPTIMVertex va2 = psg.getTailVertex();

                for (ObjectPair op : lstPairs)
                {
                    DENOPTIMVertex vb1 = (DENOPTIMVertex) op.getFirst();
                    DENOPTIMVertex vb2 = (DENOPTIMVertex) op.getSecond();

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
            Map<ClosableConf,Set<ObjectPair>> mapOcPathsWithCC = 
                                 new HashMap<ClosableConf,Set<ObjectPair>>();

            // Per each closable conf. of a path lists the simultaneously 
            // closable confs of the other interdependent paths
            Map<ClosableConf,Set<ClosableConf>> mapOfClosableConfs =
                                 new HashMap<ClosableConf,Set<ClosableConf>>();

            // For the first path just put all closable conf in the list
            ObjectPair firstOp = grpIntrdepPaths.get(0);
            PathSubGraph firstPsg = allGoodPaths.get(firstOp);
            RingClosingConformations firstRcc = firstPsg.getRCC();

            for (ArrayList<Double> ccAngls : firstRcc.getListOfConformations())
            {
                ClosableConf cc = new ClosableConf(firstPsg.getBondPath(), 
                                                   ccAngls);
                // Container for cc of other paths
                Set<ClosableConf> scc = new HashSet<ClosableConf>();
                mapOfClosableConfs.put(cc,scc);
                // Container for other paths
                Set<ObjectPair> sop = new HashSet<ObjectPair>();
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
        private DENOPTIMGraph graph;

        // Molecular representation of the current system
        private IAtomContainer mol;

        // Topological matrix: contains the number of bonds (shortest path) 
        // separating the two atoms having idex i and j in the current system
        private int[][] topoMat;
        
        // List of Ring Closing Vertices (RCV as DENOPTIMVerex) each containing
        // an available Ring Closing Attractor (RCA)
        private ArrayList<DENOPTIMVertex> lstVert;
        private int sz;

        // Compatibility matrix between pairs of RCAs in the current system.
        private boolean[][] compatibilityOfPairs;

        // Weight factors used to control the likeliness of chosing rings of a
        // given size for the current system
        private ArrayList<Integer> w;

        // Visited flag for RCAs
        private ArrayList<Boolean> done;

        // Map linking the list of vertices and atoms
        private Map<DENOPTIMVertex,ArrayList<Integer>> vIdToAtmId;

        // Parameters setting the bias for selecting rings of given size
        private ArrayList<Integer> ringSizeBias = 
                                       RingClosureParameters.getRingSizeBias();

        //---------------------------------------------------------------------

        public RingSizeManager()
        {
        }

        //---------------------------------------------------------------------

        public void initialize(IAtomContainer origMol, DENOPTIMGraph graph)
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
            vIdToAtmId = DENOPTIMMoleculeUtils.getVertexToAtomIdMap(lstVert,mol);

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

            if (verbosity > 1)
            {
                System.out.println("TopoMat N: " + mol.getAtomCount() + " " 
                                                   + duration + " microsec.");
                int n = mol.getAtomCount();
                System.out.println("Topological matrix (n=" + n + ")");
                for (int i=0; i<n; i++)
                {
                    String l = " ";
                    for (int j=0; j<n; j++)
                    {
                        l = l + " " + topoMat[i][j];
                    }
                    System.out.println(l);
                }
            }
        }

        //---------------------------------------------------------------------

        // NOTE: this method considers the ring size bias and sets also 
        // the weight factors

        private void calculateCompatibilityOfAllRCAPairs() 
                                                       throws DENOPTIMException
        {
            w = new ArrayList<Integer>(Collections.nCopies(sz, 0));
            compatibilityOfPairs = new boolean[sz][sz];
            for (int i=0; i<sz; i++)
            {
                DENOPTIMVertex vI = lstVert.get(i);
                IAtom atmI = mol.getAtom(vIdToAtmId.get(vI).get(0));
                RingClosingAttractor rcaI = new RingClosingAttractor(atmI,mol);
                if (!rcaI.isAttractor())
                {
                    String s = "Attempt to evaluate RCA pair compatibility "
                               + "with a non-RCA end (" + atmI + ").";
                    throw new DENOPTIMException(s);
                }
                for (int j=i+1; j<sz; j++)
                {
                    DENOPTIMVertex vJ = lstVert.get(j);
                    IAtom atmJ = mol.getAtom(vIdToAtmId.get(vJ).get(0));
                    RingClosingAttractor rcaJ = 
                                             new RingClosingAttractor(atmJ,mol);
                    if (!rcaI.isAttractor())
                    {
                        String s = "Attempt to evaluate RCA pair compatibility "
                                   + "with a non-RCA end (" + atmI + ").";
                        throw new DENOPTIMException(s);
                    }
                    if (rcaI.isCompatible(rcaJ) &&
                        evaluateRCVPair(vI,vJ,graph))
                    {
                        //TODO: evluate the use of ShortestPath instead of this
                        int ringSize = topoMat[vIdToAtmId.get(vI).get(0)]
                                              [vIdToAtmId.get(vJ).get(0)] - 1;
                        int szFct = 0;
                        if (ringSize < RingClosureParameters.getMaxRingSize())
                        {
                            szFct = ringSizeBias.get(ringSize);
                        }
                        if (szFct > 0)
                        {
                            compatibilityOfPairs[i][j] = true;
                            compatibilityOfPairs[j][i] = true;
                            w.set(i, w.get(i) + szFct);
                            w.set(j, w.get(j) + szFct);
                        }

                        if (verbosity > 1)
                        {
                            System.out.println(" i:" + i + " j:" + j  
                                     + " size:" + ringSize + " factors:" + w);
                        }
                    }
                }
            }
            
            if (verbosity > 1)
            {
                String s = "RCV pairs compatibility (ring size-biased):";
                System.out.println(s);
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
                    System.out.println(l);
                }
            }
        }

        //---------------------------------------------------------------------

        public ArrayList<DENOPTIMVertex> getRSBiasedListOfCandidates()
        {
            ArrayList<DENOPTIMVertex> wLst = new ArrayList<DENOPTIMVertex>();
            for (int i=0; i<sz; i++)
            {
                if (done.get(i))
                {
                    continue;
                }
                DENOPTIMVertex v = lstVert.get(i);
                for (int j=0; j<w.get(i); j++)
                {
                    wLst.add(v);
                }
            }  

            if (verbosity > 1)
            {
                System.out.println("Ring size-biased list of RCVs:" + wLst);
            }

            return wLst;
        }

        //---------------------------------------------------------------------

        public ArrayList<DENOPTIMVertex> getRSBiasedListOfCandidates(
                                                             DENOPTIMVertex vI)
        {
            int i = lstVert.indexOf(vI);
            ArrayList<DENOPTIMVertex> wLst = new ArrayList<DENOPTIMVertex>();
            for (int j=0; j<sz; j++)
            {
                if (done.get(j) || !compatibilityOfPairs[i][j])
                {
                    continue;
                }

                DENOPTIMVertex vJ = lstVert.get(j);

                int ringSize = topoMat[vIdToAtmId.get(vI).get(0)]
                                      [vIdToAtmId.get(vJ).get(0)] - 1;
                int szFct = 0;
                if (ringSize < RingClosureParameters.getMaxRingSize())
                {
                    szFct = ringSizeBias.get(ringSize);
                }
                for (int z=0; z<szFct; z++)
                {
                    wLst.add(vJ);
                }
            }

            if (verbosity > 1)
            {
                System.out.println("Ring size-biased list of RCVs for " + vI 
                                                               + ": " + wLst);
            }

            return wLst;
        }

        //---------------------------------------------------------------------

        public void addRingClosingBond(DENOPTIMVertex vI, DENOPTIMVertex vJ)
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
            // and that vI and vY are proper RCVs
            
            BondType bndTyp = graph.getIncidentEdges(vI).get(0).getBondType();
            if (bndTyp.hasCDKAnalogue())
            {
                IBond bnd = new Bond(srcI,srcJ);
                bnd.setOrder(bndTyp.getCDKOrder());
                mol.addBond(bnd);
            } else {
                System.out.println("WARNING! Attempt to add ring closing bond "
                        + "did not add any actual chemical bond because the "
                        + "bond type of the chord is '" + bndTyp +"'.");
            }
            
            if (verbosity > 1)
            {
                System.out.println(" ==> UPDATING RingSizeManager <==");
            }

            // Update this RingSizeManager
            fillTopologicalMatrix();
            calculateCompatibilityOfAllRCAPairs();
        }

        //---------------------------------------------------------------------

        public void setVertedAsDone(DENOPTIMVertex v)
        {
            done.set(lstVert.indexOf(v),true);
        }

        //---------------------------------------------------------------------

        public boolean getCompatibilityOfPair(DENOPTIMVertex vI, 
                                                             DENOPTIMVertex vJ)
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
            double thrs = RingClosureParameters.getPathConfSearchStep() / 2.0;
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

    private boolean evaluateRCVPair(DENOPTIMVertex vI, DENOPTIMVertex vJ,
                                                       DENOPTIMGraph molGraph)
                                                      throws DENOPTIMException
    {
        String s = "Evaluation of RCV pair " + vI + " " + vJ + ": ";
        
        System.out.println(" graph in evaluateRCVPair");
        for (DENOPTIMAttachmentPoint ap : molGraph.getAttachmentPoints())
        {
            APClass a = ap.getAPClass();
            System.out.println("  " +ap.getOwner()+ " "+ a + " " + a.hashCode());
        }
        for (DENOPTIMEdge e : molGraph.getEdgeList())
        {
            APClass src = e.getSrcAPClass();
            APClass trg = e.getTrgAPClass();
            System.out.println("  " + e + " "+src.hashCode()+" "+trg.hashCode());
        }   

        // Get details on the first vertex (head)
        int vIdI = vI.getVertexId();
        int edgeIdI = molGraph.getIndexOfEdgeWithParent(vIdI);
        DENOPTIMEdge edgeI = molGraph.getEdgeAtPosition(edgeIdI);
        int srcApIdI = edgeI.getSrcAPID();
        DENOPTIMVertex pvI = molGraph.getParent(vIdI);
        DENOPTIMAttachmentPoint srcApI = pvI.getAttachmentPoints().get(
                                                                    srcApIdI);
        int srcAtmIdI = srcApI.getAtomPositionNumber();
        APClass parentAPClsI = edgeI.getSrcAPClass();

        // Get details on the second vertex (tail)
        int vIdJ = vJ.getVertexId();
        int edgeIdJ = molGraph.getIndexOfEdgeWithParent(vIdJ);
        DENOPTIMEdge edgeJ = molGraph.getEdgeAtPosition(edgeIdJ);
        int srcApIdJ = edgeJ.getSrcAPID();
        DENOPTIMVertex pvJ = molGraph.getParent(vIdJ);
        DENOPTIMAttachmentPoint srcApJ =pvJ.getAttachmentPoints().get(srcApIdJ);
        int srcAtmIdJ = srcApJ.getAtomPositionNumber();
        APClass parentAPClsJ = edgeJ.getSrcAPClass();
        
        // exclude if no entry in RC-Compatibility map
        if (!rcCPMap.containsKey(parentAPClsI))
        {
            if (verbosity > 1)
            {
                System.out.println(s + "RC-CPMap does not contain class (I) "
                        + parentAPClsI + " " + parentAPClsI.hashCode());
            }
            return false;
        }
        ArrayList<APClass> compatClassesI = rcCPMap.get(parentAPClsI);

        // exclude if no entry in RC-Compatibility map
        if (!rcCPMap.containsKey(parentAPClsJ))
        {
            if (verbosity > 1)
            {
                System.out.println(s + "RC-CPMap does not contain class (J) "
                        + parentAPClsJ + " " + parentAPClsJ.hashCode());
            }
            return false;
        }
        ArrayList<APClass> compatClassesJ = rcCPMap.get(parentAPClsJ);

        // exclude loops included within a single vertex 
        if (vI == vJ)
        {
            if (verbosity > 1)
            {
                System.out.println(s + "vI same as vJ: loop not allowed!");
            }
            return false;
        }

        // exclude pairs of RCA-vertices having same src atom
        if (pvI == pvJ && srcAtmIdI == srcAtmIdJ)
        {
            if (verbosity > 1)
            {
                System.out.println(s + "Same src: " + pvI + " " + pvJ
                                  + " " + srcAtmIdI + " " + srcAtmIdJ);
            }
            return false;
        }

        // exclude paths that do not connect APClass compatible ends
        // NOTE that in ring closures the CPMap is symmetric, this
        // also implies that CPMap for ring closure may be different
        // from standard CPMap        
        if (!(compatClassesI.contains(parentAPClsJ) ||
              compatClassesJ.contains(parentAPClsI)))
        {
            if (verbosity > 1)
            {
                System.out.println(s + "APClass not compatible "
                                + parentAPClsJ);
            }
            return false;
        }

        if (verbosity > 1)
        {
            System.out.println(s + "all criteria satisfied.");
        }

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
     * contitution and does not require 3D coordinates.
     * @return <code>true</code> is the path corresponds to a closable chain
     */

    private boolean evaluatePathClosability(PathSubGraph subGraph,
                                  IAtomContainer mol) throws DENOPTIMException
    {
        boolean closable = false;
        switch (RingClosureParameters.getClosabilityEvalMode())
        {
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
        if (verbosity > 0)
        {
            System.out.println(" ");
            System.out.println("Evaluating constitutional closability of "
                               + "path: " + subGraph.getVertecesPath());
        }

        // Get a working copy of the molecular container
        IAtomContainer mol = new AtomContainer();
        try
        {
            mol = inMol.clone();
        }
        catch (CloneNotSupportedException e)
        {
            throw new DENOPTIMException(e);
        }
        DENOPTIMMoleculeUtils.removeRCA(mol);
        
        // Identify atoms of molecular representation that correspond to
        // this path of vertices
        Map<DENOPTIMVertex,ArrayList<Integer>> vIdToAtmId =
                DENOPTIMMoleculeUtils.getVertexToAtomIdMap(
                        (ArrayList<DENOPTIMVertex>) subGraph.getVertecesPath(),
                        mol);
        Set<Integer> atmIdsInVrtxPath = new HashSet<Integer>();
        for (DENOPTIMVertex v : subGraph.getVertecesPath())
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
                    if (atmIdsInVrtxPath.contains(mol.getAtomNumber(nbrAtm)))
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
        IAtom atmH = mol.getAtom(
                             vIdToAtmId.get(subGraph.getHeadVertex()).get(0));
        IAtom atmT = mol.getAtom(
                             vIdToAtmId.get(subGraph.getTailVertex()).get(0));
        IAtom srcH = mol.getConnectedAtomsList(atmH).get(0);
        IAtom srcT = mol.getConnectedAtomsList(atmT).get(0);
        int iSrcH = mol.getAtomNumber(srcH);
        int iSrcT = mol.getAtomNumber(srcT);
        toRemove.add(atmH);
        toRemove.add(atmT);
        
        BondType bndTyp = subGraph.getEdgesPath().get(0).getBondType();
        if (bndTyp.hasCDKAnalogue())
        {
            mol.addBond(iSrcH, iSrcT, bndTyp.getCDKOrder());
        } else {
            System.out.println("WARNING! Attempt to add ring closing bond "
                    + "did not add any actual chemical bond because the "
                    + "bond type of the chord is '" + bndTyp +"'.");
        }

        // Remove atoms
        for (IAtom a : toRemove)
        {
            mol.removeAtomAndConnectedElectronContainers(a);
        }

         if (verbosity > 1)
        {
            System.out.println("Molecular representation of path includes:");
                for (IAtom a : mol.atoms())
            {
                System.out.println("  " + a.getSymbol() 
                            + mol.getAtomNumber(a) + " " + a.getProperties());
            }
        }

        boolean closable = false;

        // Evaluate requirement based on elements contained in the ring
        boolean spanRequiredEls = false;
        Set<String> reqRingEl = RingClosureParameters.getRequiredRingElements();
        if (reqRingEl.size() != 0)
        {
            // Prepare shortst atom path 
            List<IAtom> atomsPath = new ArrayList<IAtom>();
            try {
                atomsPath = PathTools.getShortestPath(mol, srcH, srcT);
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
                if (verbosity  > 1)
                {
                   System.out.println("Candidate ring doesn't involve "
                                                                   + missingEl);
                }
                return false;
            }
            closable = true;
        }

        // Try to find a match for any of the SMARTS queries
        Map<String,String> smarts = 
                      RingClosureParameters.getConstitutionalClosabilityConds();
        if (smarts.size() != 0)
        {
            closable = false;
            ManySMARTSQuery msq = new ManySMARTSQuery(mol,smarts);
            if (msq.hasProblems())
            {
                String msg = "Attempt to match SMARTS for "
                             + "constitution-based ring-closability conditions "
                             + "returned an error! " + msq.getMessage();
                DENOPTIMLogger.appLogger.log(Level.WARNING,msg);
            }
            for (String name : smarts.keySet())
            {
                if (msq.getNumMatchesOfQuery(name) > 0)
                {
                    if (verbosity > 1)
                    {
                        System.out.println("Candidate closable path matches "
                                           + "constitutional closability "
                                           + "criterion: " + smarts.get(name));
                    }
                    closable = true;
                    break;
                }
            }
        }

        if (verbosity > 0)
        {
            System.out.println("Contitutional closability: "+closable);
        }
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
     * contitution and does not require 3D coordinates.
     * @return <code>true</code> is the path corresponds to a closable chain
     */

    private boolean evaluate3DPathClosability(PathSubGraph subGraph, 
                                  IAtomContainer mol) throws DENOPTIMException
    {
        String chainId = subGraph.getChainID();
        if (verbosity > 0)
        {
            System.out.println(" ");
            System.out.println("Evaluating 3D closability of path: " 
                            + subGraph.getVertecesPath()+" ChainID: "+chainId);
        }

        RingClosingConformations rcc;
        boolean closable = false;
        String foundID = RingClosuresArchive.containsChain(subGraph);
        if (foundID != "")
        {
            // Get all info from archive
            closable = RingClosuresArchive.getClosabilityOfChain(foundID);
            rcc = RingClosuresArchive.getRCCsOfChain(foundID);
            if (RingClosureParameters.checkInterdependentChains() && 
                                  RingClosureParameters.doExhaustiveConfSrch())
            {
                subGraph.makeMolecularRepresentation(mol,
                                                     libScaff,
                                                     libFrag,
                                                     libCap,
                                                     false);
                subGraph.setRCC(rcc);        
            }
        }
        else
        {
            // Need to generate 3D molecular representation
            subGraph.makeMolecularRepresentation(mol,
                                                 libScaff,
                                                 libFrag,
                                                 libCap,
                                                 true);
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
            if (verbosity > 0)
            {
                System.out.println("Rotatability: "+rotatability); 
            }
    
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
                                                         closableConfs);

            // store in object graph
            rcc = new RingClosingConformations(chainId, closableConfs);
            subGraph.setRCC(rcc);

            // put ring-closure information in archive for further use
            RingClosuresArchive.storeEntry(chainId,closable,rcc);
        }

        if (verbosity > 0)
        {
            System.out.println("Path closablility: "+closable);
        }

        return closable;
    }
    
//-----------------------------------------------------------------------------

    /**
     * Evaluates the combination of a DENOPTIMGraph and a set of DENOPTIMRings
     * and decides whether it's a proper candidate for the generation of 
     * chelating ligands
     * @return <code>true</code> it this system is a good candidate
     */

    public boolean checkChelatesGraph(DENOPTIMGraph molGraph,
                                      Set<DENOPTIMRing> ringsSet)
    {

        if (verbosity > 0)
        {
            System.out.println("Checking conditions for chelates");
        }

//TODO: here we assume that the scaffold is a metal and the first layer of
// vertices (level = 0) are the coordinating atoms.
// Also, the APclass of the ap connecting the candidate orphan is hard coded!
// This is a temporary solution. Need a general approach and tunable by
// options/parameters

        for (DENOPTIMVertex vert : molGraph.getVertexList())
        {
            int vId = vert.getVertexId();
            
            DENOPTIMFragment vertFrag = null;
            if (vert instanceof DENOPTIMFragment)
                vertFrag = (DENOPTIMFragment) vert;
                

            // check for orphan coordinating atoms:
            // they have RCAs but none of them is included in a rings
            if (vert.getLevel() == 0 
                    && vertFrag.getFragmentType() == BBType.FRAGMENT)
            {

                DENOPTIMEdge edgeToParnt = molGraph.getEdgeAtPosition(
                                       molGraph.getIndexOfEdgeWithParent(vId));
                APClass apClassToScaffold = edgeToParnt.getTrgAPClass();
//TODO-V3: change. hard coded class of ligand
                if (!apClassToScaffold.toString().equals("MAmine:1"))
                {
                    continue;
                }
                
                boolean isOrphan = false;
                ArrayList<Integer> childVertxIDs = 
                                             molGraph.getChildVertices(vId);
                for (Integer cvId : childVertxIDs)
                {
                    DENOPTIMVertex cVrtx = molGraph.getVertexWithId(cvId);
                    if (cVrtx.isRCV() && !molGraph.isVertexInRing(cVrtx))
                    {
                        isOrphan = true;
                        break;
                    }
                }
                if (isOrphan)
                {
                    if (verbosity > 0)
                    {
                        System.out.println("Found orphan: " + vert 
                                                    + " RingSet: " + ringsSet);
                    }
                    return false;
                }
            }

            // check for not fully coordinating multidentate bridges
//TODO: make the full-denticity requirement optional for same/all APclasses
            if (vert.getLevel() > 0)
            {
                ArrayList<Integer> childVertxIDs =
                                               molGraph.getChildVertices(vId);
                Map<String,ArrayList<DENOPTIMVertex>> rcasOnThisVertex =
                               new HashMap<String,ArrayList<DENOPTIMVertex>>();
                for (Integer cvId : childVertxIDs)
                {
                    DENOPTIMVertex cVrtx = molGraph.getVertexWithId(cvId);
                    if (cVrtx.isRCV())
                    {
                        DENOPTIMAttachmentPoint ap =
                                        cVrtx.getAttachmentPoints().get(0);
                        String apCls = ap.getAPClass().toString();
                        if (rcasOnThisVertex.keySet().contains(apCls))
                        {
                            rcasOnThisVertex.get(apCls).add(cVrtx);
                        }
                        else
                        {
                            ArrayList<DENOPTIMVertex> sameClsRCA =
                                               new ArrayList<DENOPTIMVertex>();
                            sameClsRCA.add(cVrtx);
                            rcasOnThisVertex.put(apCls,sameClsRCA);
                        }
                    }
                }

                for (String apCls : rcasOnThisVertex.keySet())
                {
                    int usedDenticity = 0;
                    for (DENOPTIMVertex rcaVrtx : rcasOnThisVertex.get(apCls))
                    {
                        if (molGraph.isVertexInRing(rcaVrtx))
                        {
                            usedDenticity++;
                        }
                    }

                    if (usedDenticity < rcasOnThisVertex.get(apCls).size())
                    {
                        if (verbosity > 0)
                        {
                            System.out.println("Full-denticity is not "
                                  + "satisfied for apclas: " + apCls
                                  + "in vertex " + vert 
                                  + " with set of rings " + ringsSet
                                  + "check graph: " + molGraph);
                        }
                        return false;
                    }
                }
            }
        }
        
        return true;
    }

//-----------------------------------------------------------------------------

}
