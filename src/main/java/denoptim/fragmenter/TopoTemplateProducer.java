package denoptim.fragmenter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import denoptim.constants.DENOPTIMConstants;
import denoptim.utils.DummyAtomHandler;
import denoptim.utils.MoleculeUtils;

public class TopoTemplateProducer 
{
    
    /**
     * topology-critical atoms
     */
    Set<IAtom> topoCriticalAtoms = new HashSet<>();

    /**
     * The original {@link IAtomContainer} to produce a topology-critical template for.
     */
    IAtomContainer originalIAC;

    /**
     * Flag recording whether we could only produce a H-depleted template.
     */
    boolean produceHDepleted = false;

//-----------------------------------------------------------------------------

    /**
     * Constructor.
     * @param originalIAC the original {@link IAtomContainer} to produce a 
     * topology-critical template for.
     */
    public TopoTemplateProducer(IAtomContainer originalIAC) 
    {
        this.originalIAC = originalIAC;
        initialize();
    }

//-----------------------------------------------------------------------------

    private void initialize() 
    {
        // Find all boundary atoms: atoms connected to atoms with different vertex IDs
        Set<IAtom> boundaryAtoms = new HashSet<>();
        Map<IAtom, Long> atomToVertexId = new HashMap<>();
        Set<Long> visitedVertexIds = new HashSet<>();
        Set<Long> visitedVertexIdsBoundaries = new HashSet<>();
        Set<IAtom> atomsWithAPs = new HashSet<>();

        // collect vertex IDs and identify boundary atoms
        for (IAtom atom : originalIAC.atoms())
        {
            Object vidProp = atom.getProperty(DENOPTIMConstants.ATMPROPVERTEXID);
            if (vidProp == null)
            {
                // If no vertex ID, keep all atoms (can't optimize)
                produceHDepleted = true;
                vidProp = -1L;
            }
            Long vid = Long.parseLong(vidProp.toString());
            visitedVertexIds.add(vid);
            atomToVertexId.put(atom, vid);

            Object apsProp = atom.getProperty(DENOPTIMConstants.ATMPROPAPS);
            if (apsProp != null)
            {
                atomsWithAPs.add(atom);
            }

            // Check neighbors for different vertex IDs
            List<IAtom> neighbors = originalIAC.getConnectedAtomsList(atom);
            for (IAtom neighbor : neighbors)
            {
                Object nbrVidProp = neighbor.getProperty(DENOPTIMConstants.ATMPROPVERTEXID);
                if (nbrVidProp != null)
                {
                    Long nbrVid = Long.parseLong(nbrVidProp.toString());
                    if (!vid.equals(nbrVid))
                    {
                        boundaryAtoms.add(atom);
                        boundaryAtoms.add(neighbor);
                        visitedVertexIdsBoundaries.add(vid);
                        visitedVertexIdsBoundaries.add(nbrVid);
                        break;
                    }
                }
            }
        }

        // NB: is we have a single vertex in the template, then there are no boundary atom!
        // We'll go forward with the H-depleted version.

        // Ensure we have visited all vertexes, or H-depleted version is needed
        visitedVertexIds.removeAll(visitedVertexIdsBoundaries);
        if (!visitedVertexIds.isEmpty())
        {
            produceHDepleted = true;
        }
        
        // If no boundary atoms found, all atoms have same vertex ID, 
        // fold to using H-depleted version
        if (boundaryAtoms.isEmpty())
        {
            produceHDepleted = true;
        }

        if (produceHDepleted)
        {
            // We did not manage to find a sensible subset possibly because the input
            // is a disconnected graph, or s single-vertex graph.
            // So, we make a simplified version removing all H and dummy atoms
            for (IAtom atom : originalIAC.atoms())
            {
                // DUmmy atoms that are not AP sources are not considered
                if (MoleculeUtils.getSymbolOrLabel(atom).equals(DENOPTIMConstants.DUMMYATMSYMBOL))
                {
                    if (atomsWithAPs.contains(atom))
                    {
                        topoCriticalAtoms.add(atom);
                    } else {
                        continue;
                    }
                }
                if (atom.getSymbol().equals("H"))
                {
                    if (atomsWithAPs.contains(atom))
                    {
                        topoCriticalAtoms.add(atom);
                    }
                } else {
                    topoCriticalAtoms.add(atom);
                }
            }
        } else {
            // Use the boundaries to identify the smallest set of atoms needed
            // to identify matching topology. 
            // For each pair of boundary atoms, find shortest path
            List<IAtom> boundaryList = new ArrayList<>(boundaryAtoms);
            for (int i = 0; i < boundaryList.size(); i++)
            {
                for (int j = i + 1; j < boundaryList.size(); j++)
                {
                    IAtom start = boundaryList.get(i);
                    IAtom end = boundaryList.get(j);
                    
                    // Find shortest path between these boundary atoms
                    //
                    // WARNING: this method can be very costly for large molecules.
                    //
                    List<IAtom> path = MoleculeUtils.findShortestPath(originalIAC, 
                        start, end, atomToVertexId);
                    if (path != null)
                    {
                        topoCriticalAtoms.addAll(path);
                    }
                }
            }
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Produced a new {@link IAtomContainer} containing all the atoms needed to
     * define the topology of the original graph plus all the atoms that are 
     * connected by the number of bonds fedined by the buffer shell size.
     * @param bufferShellSize the number of bonds to consider when defining the
     * depth of the buffer shell around the topology-critical atoms.
     * @return a new {@link IAtomContainer} containing all the atoms needed to
     * define the topology of the original graph plus all the atoms that are 
     * connected by the number of bonds fedined by the buffer shell size.
     */
    public IAtomContainer getTemplateWithBufferShell(int bufferShellSize) 
    {
        IAtomContainer reduced = SilentChemObjectBuilder.getInstance().newAtomContainer();

        // Define the atoms to keep considering topology-critical atoms and the buffer shell
        Set<IAtom> atomsToKeep = new HashSet<>(topoCriticalAtoms);
        Set<IAtom> thisLevelAtoms = new HashSet<>(topoCriticalAtoms);
        for (int i = 0; i < bufferShellSize; i++)
        {
            if (atomsToKeep.size() == originalIAC.getAtomCount())
            {
                // No more atoms can be added
                break;
            }
            Set<IAtom> nextLevelAtoms = new HashSet<>();
            for (IAtom atm : thisLevelAtoms)
            {
                List<IAtom> neighbors = originalIAC.getConnectedAtomsList(atm);
                for (IAtom neighbor : neighbors)
                {
                    if (!atomsToKeep.contains(neighbor))
                    {
                        // Exclude any pseudo atom
                        if (!MoleculeUtils.isElement(neighbor))
                        {
                            continue;
                        }
                        atomsToKeep.add(neighbor);
                        nextLevelAtoms.add(neighbor);
                    }
                }
            }
            thisLevelAtoms = nextLevelAtoms;
        }
            
        // Add atoms with original index stored as property
        Map<IAtom, IAtom> originalToReduced = new HashMap<>(); // original atom -> reduced atom
        for (IAtom originalAtom : atomsToKeep)
        {
            IAtom reducedAtom = originalAtom.getBuilder().newInstance(
                IAtom.class, originalAtom);
            reduced.addAtom(reducedAtom);
            originalToReduced.put(originalAtom, reducedAtom);
            
            // Store the original atom index as a property
            int originalIndex = originalIAC.indexOf(originalAtom);
            reducedAtom.setProperty("DENOPTIM_ORIGINAL_ATOM_INDEX", originalIndex);
            
            // Copy all other properties
            for (Object key : originalAtom.getProperties().keySet())
            {
                if (!key.equals("DENOPTIM_ORIGINAL_ATOM_INDEX"))
                {
                    reducedAtom.setProperty(key, originalAtom.getProperty(key));
                }
            }
        }
        
        // Add bonds between kept atoms
        for (IBond bond : originalIAC.bonds())
        {
            IAtom atom1 = bond.getAtom(0);
            IAtom atom2 = bond.getAtom(1);
            
            if (atomsToKeep.contains(atom1) && atomsToKeep.contains(atom2))
            {
                IBond newBond = bond.getBuilder().newInstance(IBond.class, 
                        originalToReduced.get(atom1), originalToReduced.get(atom2), bond.getOrder());
                reduced.addBond(newBond);
            }
        }
        
        return reduced; 
    }

//-----------------------------------------------------------------------------
}
