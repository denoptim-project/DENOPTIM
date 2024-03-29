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

/**
 * A class for iterating over sets of ring combinations generated by considering
 * any constrain and setting defined in the fragment space and ring-closure
 * settings and by randomly picking candidates when no criterion can be used to 
 * take an informed decision. Note this iterator loops infinitely, i.e.,
 * <code>hasNext()</code> returns always <code>true</code>.
 */
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
     * Space of building blocks
     */
    private FragmentSpace fragSpace;
    
    /**
     * Parameters related to rings
     */
    private RingClosureParameters settings;
    
//-----------------------------------------------------------------------------
    
    /**
     * Constructs an iterator.
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
        this.fragSpace = fragSpace;
        this.maxRingClosures = maxRingClosures;
        this.originalMol = iac;
        this.molGraph = molGraph;
    }

//-----------------------------------------------------------------------------
    
    /**
     * Always returns <code>true</code> because we allow duplicates.
     */
    @Override
    public boolean hasNext()
    {
        return true;
    }

//-----------------------------------------------------------------------------
    
    @Override
    public List<Ring> next()
    {   
        RingSizeManager rsm = new RingSizeManager(fragSpace, settings);
        rsm.initialize(originalMol, molGraph);
        
        List<Vertex> wLstVrtI = rsm.getRSBiasedListOfCandidates();
        
        // Randomly choose the compatible combinations of RCAs and store them
        // as rings
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
            List<Vertex> wLstVrtJ = rsm.getRSBiasedListOfCandidates(vI); 
            while (wLstVrtJ.size() > 0)
            {
                int vIdJ = settings.getRandomizer().nextInt(wLstVrtJ.size());
                Vertex vJ = wLstVrtJ.get(vIdJ);
                wLstVrtJ.removeAll(Collections.singleton(vJ));

                PathSubGraph path = new PathSubGraph(vI, vJ, molGraph);
                // NB: closability is evaluated on the original
                if (PathClosabilityTools.isCloseable(path, originalMol, settings))
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
                        throw new IllegalArgumentException(s);
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

//-----------------------------------------------------------------------------
    
}
