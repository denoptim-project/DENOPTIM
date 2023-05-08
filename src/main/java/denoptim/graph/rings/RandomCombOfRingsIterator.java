package denoptim.graph.rings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.graph.DGraph;
import denoptim.graph.Fragment;
import denoptim.graph.Ring;
import denoptim.graph.Vertex;
import denoptim.graph.Edge.BondType;

public class RandomCombOfRingsIterator implements Iterator<List<Ring>>
{
    /**
     * Chemical representation of the system we work with
     */
    private IAtomContainer originalMol;
    
    /**
     * Graph representation of the system we work with
     */
    private DGraph molGraph;
    
    /**
     * Max number of ring closures to consider in a valid combination of rings.
     */
    private int maxRingClosures = -1;
    
    /**
     * Tool managing ring size problems
     */
    private RingSizeManager rsm;
    
    /**
     * Parameters related to rings
     */
    private RingClosureParameters settings;
    
    /**
     * Flag recording we have found a next.
     */
    private boolean hasNext = false;
    
    /**
     * Weighted list of RCVs
     */
    private ArrayList<Vertex> wLstVrtI;
    
//-----------------------------------------------------------------------------
    
    /**
     * 
     * @param inMol
     * @param molGraph
     * @param maxRingClosures
     * @throws DENOPTIMException 
     */
    
    public RandomCombOfRingsIterator(IAtomContainer iac,
            DGraph molGraph, int maxRingClosures, FragmentSpace fragSpace, 
            RingClosureParameters rcParams) throws DENOPTIMException
    {
        this.settings = rcParams;
        this.maxRingClosures = maxRingClosures;
        this.originalMol = iac;
        this.molGraph = molGraph;
        
        // Prepare molecular representation with no dummy atoms
        // NOTE: the connectivity of this molecule is going to be edited 
        // as we identify new candidate rings. This is done to
        // calculate ring size as if the candidate rings were closed
        IAtomContainer mol;
        try
        {
            mol = (IAtomContainer) iac.clone();
        }
        catch (Throwable t)
        {
            throw new DENOPTIMException(t);
        }
        this.molGraph = molGraph;

        rsm = new RingSizeManager(fragSpace, rcParams);
        rsm.initialize(mol, molGraph);

        // Get weighted list of RCVs
        wLstVrtI = rsm.getRSBiasedListOfCandidates();
    }

//-----------------------------------------------------------------------------
    
    @Override
    public boolean hasNext()
    {
        return hasNext;
    }

//-----------------------------------------------------------------------------
    
    @Override
    public List<Ring> next()
    {
        //TODO-gg re-initialize RSM?
        
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

                PathSubGraph path = new PathSubGraph(vI, vJ, molGraph);
                if (PathClosabilityTools.isCloseable(path, mol, settings)) //TODO-gg of ininital Mol?
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
        return combOfRings;
    }

}
