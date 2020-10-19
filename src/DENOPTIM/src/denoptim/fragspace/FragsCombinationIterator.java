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

package denoptim.fragspace;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

import denoptim.exception.DENOPTIMException;
import denoptim.molecule.DENOPTIMFragment.BBType;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.molecule.SymmetricSet;
import denoptim.utils.GraphUtils;


/**
 * Factory of combination of fragments. This tool is meant to generate 
 * combinations of fragments one after the other (sequentially) in a 
 * low-memory usage fashion, that is, without 
 * generating and storing the complete list of combinations.
 *
 * @author Marco Foscato
 */

public class FragsCombinationIterator 
{

    /**
     * Flag defining whether at list one more combination can be produced
     */
    private boolean finished = false;

    /**
     * The root graph (may be a single fragment or an extended graph)
     */
    private DENOPTIMGraph rootGraph;

    /**
     * List of all attachment points on the root graph (source APs)
     */
    private ArrayList<IdFragmentAndAP> allSrcAps =
                                               new ArrayList<IdFragmentAndAP>();

    /**
     * List of attachment points on the root graph (source APs) for which there 
     * are one or more candidate destinies (i.e., append a frag, cap, or leave 
     * empty).
     */
    private ArrayList<IdFragmentAndAP> actvSrcAps =
                                               new ArrayList<IdFragmentAndAP>();

    /**
     * Map of all possible matches for each source AP 
     */
    private Map<IdFragmentAndAP,ArrayList<IdFragmentAndAP>> candFragsPerAP =
                      new HashMap<IdFragmentAndAP,ArrayList<IdFragmentAndAP>>();

    /**
     * Set of indeces for the next iteration
     */
    private ArrayList<Integer> nextIds = new ArrayList<Integer>();

    /**
     * Sizes of the candidates set for each source AP
     */
    private ArrayList<Integer> totCandsPerAP = new ArrayList<Integer>();

    /**
     * Total number for combinations
     */
    private int totCombs = 0;

    /**
     * Current number of generated combinations
     */
    private int numbGenCombs = 0;

    /**
     * Verbosity lvel
     */
    private int verbosity = FragmentSpaceParameters.getVerbosity();


//------------------------------------------------------------------------------

    /**
     * Constructs a new factory for generating all the combination of fragments 
     * that can be attached on a given root graph.
     * @param rootGraph 
     * @throws DENOPTIMException
     */

    public FragsCombinationIterator(DENOPTIMGraph rootGraph) 
                                                        throws DENOPTIMException
    {
        this.rootGraph = rootGraph;

        // Identify all candidate source APs: free APs on the root graph/vertex.
        // In case of symmetry, store only the first among the symmetric 
        // vertices in the graph, and the first AP among the symmetric APs
        // in a vertex
        for (DENOPTIMVertex v : this.rootGraph.getVertexList())
        {
            int vIdx = v.getVertexId();
            //TODO-V3 need to use something else
            int vMolId = v.getMolId();
            BBType vMolTyp = v.getFragmentType();
           
            // deal with symmetric sets of vertices
            boolean keepThisVertex = true;
            if ((FragmentSpaceParameters.enforceSymmetry()
                                            && this.rootGraph.hasSymmetricAP())
                || (FragmentSpaceParameters.symmetryConstraints()))
            {
                keepThisVertex = false;
                boolean isInSymSet = false;
                Iterator<SymmetricSet> it = this.rootGraph.getSymSetsIterator();
                while (it.hasNext())
                {
                    SymmetricSet ss = it.next();
                    if (ss.contains(vIdx))
                    {
                        isInSymSet = true;
                        if (ss.getList().indexOf(vIdx) == 0)
                        {
                            keepThisVertex = true;
                        }
                        break;
                    }
                }
                if (!isInSymSet)
                {
                    keepThisVertex = true;
                }
            }
            if (!keepThisVertex)
            {
                continue;
            }

            for (Integer apIdx : v.getFreeAPList())
            {
                // deal with symmetric sets of APs on this vertex
                boolean keepThisAP = true;
                if ((FragmentSpaceParameters.enforceSymmetry() ||
                     FragmentSpaceParameters.symmetryConstraints()) 
                                                          && v.hasSymmetricAP())
                {
                    keepThisAP = false;
                    boolean isInSymSet = false;
                    for (SymmetricSet ss : v.getSymmetricAPSets())
                    {
                        if (ss.contains(apIdx))
                        {
                            String apClass = v.getAttachmentPoints().get(
                                                            apIdx).getAPClass();
                            if (!FragmentSpace.imposeSymmetryOnAPsOfClass(
                                                                       apClass))
                            {
                                continue;
                            }

                            isInSymSet = true;
                            if (ss.getList().indexOf(apIdx) == 0)
                            {
                                keepThisAP = true;
                            }
                            break;
                        }
                    }
                    if (!isInSymSet)
                    {
                        keepThisAP = true;
                    }
                }
                if (!keepThisAP)
                {
                    continue;
                }

                IdFragmentAndAP idFragAp = new IdFragmentAndAP(vIdx, vMolId,
                                                        vMolTyp, apIdx, -1, -1);
                allSrcAps.add(idFragAp);
            }
        }

        // Collect all possibilities (frags, caps, entry) for each free AP
        for (IdFragmentAndAP candSrcAp : allSrcAps)
        {
            BBType fTyp = candSrcAp.getVertexMolType();
            int fIdx = candSrcAp.getVertexMolId();
            int apId = candSrcAp.getApId();
            DENOPTIMVertex frag = FragmentSpace.getVertexFromLibrary(fTyp, fIdx); 
            String srcApCls = frag.getAttachmentPoints().get(apId).getAPClass();

            // Create data structure for candidates 
            ArrayList<IdFragmentAndAP> candsForThisSrc = 
                                               new ArrayList<IdFragmentAndAP>();

            // Get all compatible fragments
            ArrayList<IdFragmentAndAP> compatFragAps = 
                           FragmentSpace.getFragAPsCompatibleWithClass(srcApCls);

            for (IdFragmentAndAP compatApId : compatFragAps)
            {
                int vid = GraphUtils.getUniqueVertexIndex();
                IdFragmentAndAP trgFrgAp = new IdFragmentAndAP(vid, //vertexId
                		            compatApId.getVertexMolId(), //MolId,
                                    BBType.FRAGMENT,
                                    compatApId.getApId(), //ApId
							        -1, //noVSym
							        -1);//noAPSym

                candsForThisSrc.add(trgFrgAp);
            }

            // Get other possible destinies for the free AP
            // NOTE: in principle there should be only ONE capping group, but
            //       this is made to work also in case of more than one group.
            String capApCls = FragmentSpace.getCappingClass(srcApCls);
            if (capApCls != null)
            {
                // Use of capping groups
                // NOTE: in principle there should be only ONE capping group,
                //       but this is made to work also in case of more than 
                //       one group.
                ArrayList<Integer> capGrpIds =
                            FragmentSpace.getCappingGroupsWithAPClass(capApCls);
                for (Integer i : capGrpIds)
                {
                    int vid = GraphUtils.getUniqueVertexIndex();
                    IdFragmentAndAP trgFrgAp = new IdFragmentAndAP(vid,//vertxId
                                                                    i,//MolId,
                                                                    BBType.CAP,
                                                                    0,//ApId
                                								   -1,//noVSym
                                								   -1);//noAPSym
                    candsForThisSrc.add(trgFrgAp);
                }
            }
            else
            {
                // No capping, so can we also leave the AP empty?
                if (!FragmentSpace.getForbiddenEndList().contains(srcApCls))
                {
                    // An empty pointer is used to represent the possibility of
                    // having no fragment
                    IdFragmentAndAP trgFrgAp = new IdFragmentAndAP();
                    candsForThisSrc.add(trgFrgAp);
                }
            }

            // Store info on this src AP
            if (candsForThisSrc.size() > 0)
            {
                // Store this FragAp as an active src Ap
                actvSrcAps.add(candSrcAp);

                // Store the reference to the candidates
                candFragsPerAP.put(candSrcAp,candsForThisSrc);

                // Store the size of the set of candidates
                totCandsPerAP.add(candFragsPerAP.get(candSrcAp).size());

                // Initialize index
                nextIds.add(0);
            }
        }

        if (verbosity > 1)
        {
            System.out.println("Initializing iterator over combs of frags:");
            for (IdFragmentAndAP candSrcAp : allSrcAps)
            {
                System.out.println(" -> Possibilities for "+candSrcAp);
                for (IdFragmentAndAP i : candFragsPerAP.get(candSrcAp))
                {
                    System.out.println(" ---> " + i);
                }
            }
        }

        // Calculate to total number of combinations
        boolean emptySets = true;
        for (int curSrcApIdx=0; curSrcApIdx<actvSrcAps.size(); curSrcApIdx++)
        {
            if (totCandsPerAP.get(curSrcApIdx) > 0)
            {
                emptySets = false;
                break;
            }
        }
        if (!emptySets)
        {
            totCombs = 1;
            for (int srcApIdx=0; srcApIdx<actvSrcAps.size(); srcApIdx++)
            {
                int factor;
                if (totCandsPerAP.get(srcApIdx) <= 1)
                {
                    factor = 1;
                }
                else
                {
                    factor = totCandsPerAP.get(srcApIdx);
                }
                totCombs = totCombs * factor;
            }
        }
        else
        {
            finished = true;
        }
    }

//------------------------------------------------------------------------------

    /**
     * Sets the starting point for the iterator. This method is meant for
     * restarting the iterator from where it terminated in a previous run. 
     * The Ids are generated by the iterator itself: do not set the Ids by hand.
     * @param nextIds the set of indeces for the next iteration. 
     */

    public void setStartingPoint(ArrayList<Integer> nextIds)
    {
        this.nextIds = nextIds;

        // Check for completion
        finished = true;
        for (int i=0; i<nextIds.size(); i++)
        {
            if (nextIds.get(i)+1 < totCandsPerAP.get(i))
            {
                 finished = false;
                 break;
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Returns the list of indeces used to calculate the next iteration
     */

    public ArrayList<Integer> getNextIds()
    {
        return nextIds;
    }

//------------------------------------------------------------------------------

    /**
     * Generate the next combination of fragments
     * @return the next <code>FragsCombination</code>
     * @throws NoSuchElementException if no more combinations can be produced 
     */

    public FragsCombination next() throws NoSuchElementException 
    {
        if (finished)
        {
            throw new NoSuchElementException();
        }

        if (verbosity > 1)
        {
            System.out.println("Calculating next frag combination (id:"
                               + nextIds + ", size:" + totCandsPerAP + ")"); 
        }

	// While defining the combination of fragments we aso keep track
	// of which incoming fragments are related by symmetry (i.e., we
	// set the symmetric set ID for each incoming vertex) to allow
	// an easy update of the graph's SymmetricSet list
        FragsCombination currentComb = new FragsCombination();
        ArrayList<Integer> currentIds = new ArrayList<Integer>();
        String msg = "";
        for (int curSrcApIdx=0; curSrcApIdx<actvSrcAps.size(); curSrcApIdx++)
        {
            IdFragmentAndAP src = actvSrcAps.get(curSrcApIdx);
	    src.setVrtSymSetId(curSrcApIdx);
            ArrayList<IdFragmentAndAP> locCandsList = candFragsPerAP.get(src);
	    for (IdFragmentAndAP fap : locCandsList)
	    {
		fap.setVrtSymSetId(curSrcApIdx);
	    }
            int locTotCands = candFragsPerAP.get(src).size();
            int locCurCandId = nextIds.get(curSrcApIdx);

            if (verbosity > 2)
            {
                msg = "Setting frag for AP " + curSrcApIdx + " (" + src + ")";
                System.out.println(msg);
            }

            // Decide whether the candidate for the current position 
            // has to be changed
            boolean changeCurIdx = true;
            for (int fIdx=curSrcApIdx+1; fIdx<actvSrcAps.size(); fIdx++)
            {
                if (nextIds.get(fIdx) < totCandsPerAP.get(fIdx))
                {
                    changeCurIdx = false;
                    break;
                }
            }  

            // Non-last srcAP 
            if ((curSrcApIdx+1) < actvSrcAps.size())
            {
                if (!changeCurIdx)
                {
                    if (verbosity > 2)
                    {
                        msg = "Non-last src AP; fixed on " + locCurCandId;
                        System.out.println(msg);
                    }
                    currentIds.add(locCurCandId);
                    currentComb.put(src,locCandsList.get(locCurCandId));
                }
                else
                {
                    if (locCurCandId+1 < locTotCands)
                    {
                        // increment the ids for this src AP
                        locCurCandId++;
                        nextIds.set(curSrcApIdx,locCurCandId);
                        // and restart all the idx on all srcAP that follow
                        for (int futureSrcAp=curSrcApIdx+1; 
                                   futureSrcAp<actvSrcAps.size(); futureSrcAp++)
                        {
                            if (verbosity > 2)
                            {
                                msg = "Restart idx on src AP " + futureSrcAp; 
                                System.out.println(msg);
                            }
                            nextIds.set(futureSrcAp,0);
                        }

                        if (verbosity > 2)
                        {
                            msg = "Non-last src AP; new idx = " + locCurCandId; 
                            System.out.println(msg);
                        }
                        currentIds.add(locCurCandId);
                        currentComb.put(src,locCandsList.get(locCurCandId));
                    }
                    else
                    {
                        // Restart idx for this src AP and increment the
                        // closest activable index
                        // To do this, we increase the local id so it goes 
                        // beyind the maximum (it's going to be zeroed later)...
                        locCurCandId++;
                        nextIds.set(curSrcApIdx,locCurCandId);
                        // ...and rerun the previous iteration on srcAPs 
                        if (verbosity > 2)
                        {
                            msg = "Step BACK from src AP " + curSrcApIdx; 
                            System.out.println(msg);
                        }
                        currentIds.remove(currentIds.size()-1);
                        curSrcApIdx = curSrcApIdx - 2; //it get +1 from loop
                    }
                }
            }
            // last srcAP; always try to change the candidate for this AP
            else if ((curSrcApIdx+1) == actvSrcAps.size())
            {
                if (verbosity > 2)
                {
                    msg = "Last src AP; new idx = " + locCurCandId; 
                    System.out.println(msg);
                }
                currentIds.add(locCurCandId);
                currentComb.put(src,locCandsList.get(locCurCandId));
                locCurCandId++;
                nextIds.set(curSrcApIdx,locCurCandId);

                // Check for completion
                finished = true;
                for (int i=0; i<nextIds.size(); i++)
                {
                    int nextId = nextIds.get(i)+1;
                    if (i == curSrcApIdx)
                    {
                        nextId = nextId - 1; //has been incremented above
                    } 
                    if (nextId < totCandsPerAP.get(i))
                    {
                        finished = false;
                        break;
                    }
                }
                if(verbosity > 2)
                {
                    msg = "Check completion on " + nextIds + ": " + finished;
                    System.out.println(msg);
                }
            }
        }

        numbGenCombs++;

        if (verbosity > 2)
        {
            System.out.println("Combination before applying symmetry: ");
            for (Map.Entry<IdFragmentAndAP,IdFragmentAndAP> entry : 
                                                         currentComb.entrySet())
            {
                System.out.println("  "+entry);
            }
        }

        // Project selection onto symmetric positions
        if (FragmentSpaceParameters.enforceSymmetry() 
            || FragmentSpaceParameters.symmetryConstraints())
        {
            Map<IdFragmentAndAP,IdFragmentAndAP> pairsToAdd = 
                                new HashMap<IdFragmentAndAP,IdFragmentAndAP>();

            // deal with symmetric APs within the same vertex
            for (IdFragmentAndAP srcFrgAp : currentComb.keySet())
            {
                int srcVrtId = srcFrgAp.getVertexId();
                int srcApId = srcFrgAp.getApId();
                DENOPTIMVertex v = this.rootGraph.getVertexWithId(srcVrtId);
                if (!v.hasSymmetricAP())
                {
                    continue;
                }
                for (SymmetricSet ss : v.getSymmetricAPSets())
                {
                    if (ss.contains(srcApId))
                    {
                        String apClass = v.getAttachmentPoints().get(
                                                          srcApId).getAPClass();
                        if (!FragmentSpace.imposeSymmetryOnAPsOfClass(apClass))
                        {
                            continue;
                        }

                        IdFragmentAndAP trgFrgAp = currentComb.get(srcFrgAp);
                        for (int i=1; i<ss.getList().size(); i++)
                        {
                            int symSrcApId = ss.getList().get(i);

                            if (!v.getAttachmentPoints().get(
                                                      symSrcApId).isAvailable())
                            {
                                continue;
                            }

                            IdFragmentAndAP symSrcFrgAp = new IdFragmentAndAP(
                                                                       srcVrtId,
                                                                   v.getMolId(),
                                                            v.getFragmentType(),
                                                                     symSrcApId,
						      srcFrgAp.getVrtSymSetId(),
							                    -1);

                            IdFragmentAndAP symTrgFrgAp = new IdFragmentAndAP(
                                              GraphUtils.getUniqueVertexIndex(),
                                                      trgFrgAp.getVertexMolId(),
                                                    trgFrgAp.getVertexMolType(),
                                                             trgFrgAp.getApId(),
						      trgFrgAp.getVrtSymSetId(),
									    -1);

                            pairsToAdd.put(symSrcFrgAp,symTrgFrgAp);
                        }
                        break;
                    }
                }
            }

            // Store projection onto symmetric APs of the same vertex
            for (Map.Entry<IdFragmentAndAP,IdFragmentAndAP> entry : 
                                                          pairsToAdd.entrySet())
            {
                currentComb.put(entry.getKey(), entry.getValue());
            }
            pairsToAdd.clear();

            // deal with symmetric vertices
            if (this.rootGraph.hasSymmetricAP())
            {
                for (IdFragmentAndAP srcFrgAp : currentComb.keySet())
                {
                    int srcVrtId = srcFrgAp.getVertexId();
                    Iterator<SymmetricSet> it = 
					    this.rootGraph.getSymSetsIterator();
                    while (it.hasNext())
                    {
                        SymmetricSet ss = it.next();
                        if (ss.contains(srcVrtId))
                        {
                            IdFragmentAndAP trgFrgAp =currentComb.get(srcFrgAp);
                            for (int i=1; i<ss.getList().size(); i++)
                            {
                                int symSrcVrtID = ss.getList().get(i);
                                DENOPTIMVertex symVertex = 
                                    this.rootGraph.getVertexWithId(symSrcVrtID);
                                    
                                IdFragmentAndAP symSrcFrgAp = 
                                                           new IdFragmentAndAP(
                                                                    symSrcVrtID,
                                                           symVertex.getMolId(),
                                                    symVertex.getFragmentType(),
                                                             srcFrgAp.getApId(),
						      srcFrgAp.getVrtSymSetId(),
							                    -1);
                            
                                IdFragmentAndAP symTrgFrgAp = 
                                                           new IdFragmentAndAP(
                                              GraphUtils.getUniqueVertexIndex(),
                                                      trgFrgAp.getVertexMolId(),
                                                    trgFrgAp.getVertexMolType(),
                                                             trgFrgAp.getApId(),
						      trgFrgAp.getVrtSymSetId(),
							                    -1);
    
                                pairsToAdd.put(symSrcFrgAp,symTrgFrgAp);
                            }
                            break;
                        }
                    }
                }

                // Store projection onto symmetric vertices
                for (Map.Entry<IdFragmentAndAP,IdFragmentAndAP> entry :
                                                          pairsToAdd.entrySet())
                {
                    currentComb.put(entry.getKey(), entry.getValue());
                }
                pairsToAdd.clear();
            }
        }

        if (verbosity > 2)
        {
            System.out.println("Final combination of frags/APs: ");
            for (Map.Entry<IdFragmentAndAP,IdFragmentAndAP> entry : 
                                                         currentComb.entrySet())
            {
                System.out.println("  "+entry);
            }
//            System.out.println("very last currentIds: "+currentIds+" Next: "+nextIds+" "+numbGenCombs);
}

        return currentComb;
    }

//------------------------------------------------------------------------------

    /**
     * @return <code>true</code> if at least one more combinations can be
     * produced
     */

    public boolean hasNext()
    {
        if (finished)
        {
            return false;
        }
        else
        {
            return true;
        }
    }

//------------------------------------------------------------------------------

    /**
     * @return the number of attachment points un the root system.
     */

    public int getNumRootAPs()
    {

        return actvSrcAps.size();
    }

//------------------------------------------------------------------------------

    /**
     * @return the total number of <code>FragsCombination</code>s
     * that can be generated from the set of candidates set.
     */

    public int getTotNumbCombs()
    {
        return totCombs;
    }

//------------------------------------------------------------------------------

    /**
     * @return the current total number of <code>FragsCombination</code>s
     * generated
     */

    public int getNumGeneratedCombs()
    {
        return numbGenCombs;
    }

//------------------------------------------------------------------------------

    /**
     * @return the set of candidate attachment points per each of the 
     * usable attachment point on the root system.
     */

    public Map<IdFragmentAndAP,ArrayList<IdFragmentAndAP>> getCandidatesMap()
    {
        return candFragsPerAP;
    }

//------------------------------------------------------------------------------

    /**
     * @return the vector with the size of the set of candidates per each 
     * usable attachment point
     */

    public ArrayList<Integer> getSizesOfCandidateSets()
    {
        return totCandsPerAP;
    }

//------------------------------------------------------------------------------

}
