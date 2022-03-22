package denoptim.utils;

/**
 * Types of crossover defined
 */

public enum CrossoverType {
    
    /**
     * Swaps the entire branch starting from a given vertex.
     */
    BRANCH,
    
    /**
     * Swaps a portion of a branch trying to retain cyclicity.
     */
    SUBGRAPH

}
