package denoptim.molecule;

import java.util.ArrayList;

import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.fragspace.FragmentSpace;
import denoptim.utils.FragmentUtils;

/**
 * Interface for any sort of graph building block. {@link DENOPTIMGraph}s are
 * made of {@link DENOPTIMEdge}s connecting {@link DENOPTIMVertex}ex, and the 
 * latter are pointers to building blocks that must only respect this very 
 * contract.
 */

public interface IGraphBuildingBlock extends Cloneable
{
	
    /**
     * Returns the complete list of attachment points present on this building 
     * block.
     * @return the list of APs.
     */
    public ArrayList<DENOPTIMAttachmentPoint> getAPs();
	
    /**
     * Returns the list of attachment point classes.
     * @return the list of APClasses.
     */
    public ArrayList<String> getAllAPClassess();

    /**
     * Returns the number of attachment points.
     * @return the number of attachment points.
     */
    public int getAPCount();
    
    /**
     * Returns a deep copy.
     * @return a deep copy.
     */
    public IGraphBuildingBlock clone();
    
    //TODO: add a method that would return the list of symmetry matching APs
    /*
     The method would be meant to replace the following lines that are currently found in a few places.
     
        IAtomContainer mol = FragmentSpace.getScaffoldLibrary().get(scafIdx);
        ArrayList<SymmetricSet> symAPs = FragmentUtils.getMatchingAP(mol,scafAPs);
     */
    
    //TODO: we might also need get/set Property like for the AtomContainer
    
}
