package denoptim.molecule;

import java.util.ArrayList;

/**
 * Interface for any sort of graph building block. {@link DENOPTIMGraph}s are
 * made of {@link DENOPTIMEdge}s connecting {@link DENOPTIMVertex}ex, and the 
 * latter are pointers to building blocks that must only respect this very 
 * contract.
 */

//TODO-V3 delete class

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

    
    /**
     * Returns the list of attachment points that can be treated the same
     * way (i.e., symmetry-related attachment points). 
     * Note that symmetry, in this context, is
     * a defined by the topological environment surrounding the attachment 
     * point and has nothing to to with symmetry in three-dimensional space.  
     * @return the list of symmetry-related attachment points.
     */
    public ArrayList<SymmetricSet> getSymmetricAPsSets();
    
    //TODO: we might also need get/set Property like for the AtomContainer
    
}
