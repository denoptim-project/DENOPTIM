package denoptim.utils;

/**
 * Types of mutation defined in relation to what happens to the target vertex
 * (i.e., the actual mutation site), and the child vertexes, i.e., any vertexes 
 * reachable by a directed path from the target vertex.
 */

public enum MutationType {
    
    /**
     * Removes the target vertex and all child vertexes.
     */
    DELETE,
    
    /**
     * append vertexes on the target vertex according to substitution 
     * probability.
     */
    EXTEND,
    
    /**
     * Replace the target vertex and any of the child vertexes. 
     * The effect on the child vertexes is decided by substitution probability.
     */
    CHANGEBRANCH,
    
    /**
     * Replace the target vertex but keep as much as possible of the child 
     * graph structure. Respects constraints defined in the target vertex, and
     * projects these constraints in the new vertex.
     */
    //TODO-V3 CHANGELINK

}
