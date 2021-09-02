package denoptim.utils;

/**
 * Types of mutation defined in relation to what happens to the target vertex
 * (i.e., the actual mutation site), and the child vertices, i.e., any vertices
 * reachable by a directed path from the target vertex.
 */

public enum MutationType {
    
    /**
     * Removes the target vertex and all child vertices.
     */
    DELETE,
    
    /**
     * append vertices on the target vertex according to substitution
     * probability.
     */
    EXTEND,
    
    /**
     * Replace the target vertex and any of the child vertices.
     * The effect on the child vertices is decided by substitution probability.
     */
    CHANGEBRANCH,
    
    /**
     * Replace the target vertex keeping all the child structure.
     */
    CHANGELINK

}
