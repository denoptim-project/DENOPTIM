package denoptim.fragmenter;

/**
 * Defines how to define the scaffold vertex of a graph.
 */
public enum ScaffoldingPolicy {
    LARGEST_FRAGMENT,
    ELEMENT;
    
    /**
     * Label defining additional details, such as which label to search for in 
     * case of elemental symbol-based scaffolding policy.
     */
    public String label = null;
    
}
