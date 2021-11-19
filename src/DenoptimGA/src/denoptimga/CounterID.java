package denoptimga;

/**
 * Identifier of a counter. A printable description is given by method
 * {@link CounterID#getDescription()}.
 */

public enum CounterID 
{
    //GENERATEDGRAPHS, 
    NEWCANDIDATEATTEMPTS, 
    
    XOVERATTEMPTS, 
    XOVERPARENTSEARCH,
    FAILEDXOVERATTEMPTS,
    FAILEDXOVERATTEMPTS_FINDPARENTS, 
    FAILEDXOVERATTEMPTS_PERFORM,
    FAILEDXOVERATTEMPTS_SETUPRINGS, 
    FAILEDXOVERATTEMPTS_EVAL, 
    FAILEDXOVERATTEMPTS_FORBENDS,
    
    MUTATTEMPTS, 
    MUTPARENTSEARCH, 
    FAILEDMUTATTEMTS,
    FAILEDMUTATTEMTS_PERFORM,
    FAILEDMUTATTEMTS_PERFORM_NOMUTSITE,
    FAILEDMUTATTEMTS_PERFORM_NOOWNER,
    FAILEDMUTATTEMTS_PERFORM_BADMUTTYPE,
    FAILEDMUTATTEMTS_PERFORM_NOCHANGEBRANCH,
    FAILEDMUTATTEMTS_PERFORM_NOCHANGELINK,
    FAILEDMUTATTEMTS_PERFORM_NOCHANGELINK_FIND,
    FAILEDMUTATTEMTS_PERFORM_NOCHANGELINK_EDIT,
    FAILEDMUTATTEMTS_PERFORM_NODELLINK_FINDPARENT,
    FAILEDMUTATTEMTS_PERFORM_NODELLINK_EDIT,
    FAILEDMUTATTEMTS_PERFORM_NOADDLINK,
    FAILEDMUTATTEMTS_PERFORM_NOADDLINK_FIND,
    FAILEDMUTATTEMTS_PERFORM_NOADDLINK_EDIT,
    FAILEDMUTATTEMTS_PERFORM_NOEXTEND,
    FAILEDMUTATTEMTS_PERFORM_NODELETE,
    FAILEDMUTATTEMTS_SETUPRINGS, 
    FAILEDMUTATTEMTS_EVAL, 
    FAILEDMUTATTEMTS_FORBENDS,
    
    BUILDANEWATTEMPTS, 
    FAILEDBUILDATTEMPTS, 
    FAILEDBUILDATTEMPTS_GRAPHBUILD,
    FAILEDBUILDATTEMPTS_EVAL, 
    FAILEDBUILDATTEMPTS_SETUPRINGS, 
    FAILEDBUILDATTEMPTS_FORBIDENDS,
    
    MANUALADDATTEMPTS,
    FAILEDMANUALADDATTEMPTS, 
    FAILEDMANUALADDATTEMPTS_EVAL, 
    
    FITNESSEVALS, FAILEDFITNESSEVALS,
    
    DUPLICATEPREFITNESS,
    FAILEDDUPLICATEPREFITNESSDETECTION;
    
    private String description = "";
    
    static {
        //GENERATEDGRAPHS.description = "Graphs generated";
        
        NEWCANDIDATEATTEMPTS.description = "Attempts to generate a new "
                + "candidate";
        
        XOVERATTEMPTS.description = "Attempts to build graph by crossover";
        FAILEDXOVERATTEMPTS.description = "Failed attempts to do build a graph "
                + "by crossover";
        XOVERPARENTSEARCH.description = "Attempts to find a pairs of parents "
                + "compatible with crossover.";
        FAILEDXOVERATTEMPTS_FINDPARENTS.description = "Failed attemtps to find "
                + "crossover partners";
        FAILEDXOVERATTEMPTS_PERFORM.description = "Failed crossover operations "
                + "on compatible parents";
        FAILEDXOVERATTEMPTS_SETUPRINGS.description = "Failed attempts to setup "
                + "rings in a crossover offspring";
        FAILEDXOVERATTEMPTS_EVAL.description = "Failed attempt to pass graph "
                + "evaluation test from crossover offspring";
        FAILEDXOVERATTEMPTS_FORBENDS.description = "Crossover offsprings that "
                + "let to forbidden ends";
        
        MUTATTEMPTS.description = "Attempts to do build graph by mutation";
        FAILEDMUTATTEMTS.description = "Failed attempts to do build a graph "
                + "by Mutation";
        MUTPARENTSEARCH.description = "Attempts to find a parent that supports "
                + " mutation";
        FAILEDMUTATTEMTS_PERFORM.description = "Failed mutation operation of "
                + "parent "
                + "that supports mutation.";
        FAILEDMUTATTEMTS_PERFORM_NOMUTSITE.description = "Mutation cannot be "
                + "done because graph declares no mutation site";
        FAILEDMUTATTEMTS_PERFORM_NOOWNER.description = "Mutation cannot take "
                + "place on a vertex that has no owner";
        FAILEDMUTATTEMTS_PERFORM_BADMUTTYPE.description = "Mutation type is "
                + "not availaable on the requested vertex";
        FAILEDMUTATTEMTS_PERFORM_NOCHANGEBRANCH.description = "Mutation did "
                + "not replace the branch of a graph";
        FAILEDMUTATTEMTS_PERFORM_NOCHANGELINK.description = "Mutation did not "
                + "replace a vertex in a chain";
        FAILEDMUTATTEMTS_PERFORM_NOCHANGELINK_FIND.description = "Failed to "
                + "find an alternative link vertex";
        FAILEDMUTATTEMTS_PERFORM_NOCHANGELINK_EDIT.description = "Failed to "
                + "replace old link with new one";
        FAILEDMUTATTEMTS_PERFORM_NODELLINK_FINDPARENT.description = "Failed to "
                + "identify the parent of a link selected for removal.";
        FAILEDMUTATTEMTS_PERFORM_NODELLINK_EDIT.description = "Failed to "
                + "remove vertex and weld remaining parts.";
        FAILEDMUTATTEMTS_PERFORM_NOADDLINK.description = "Mutation did not "
                + "introduce a verted between a pairs of previously "
                + "connected vertexes.";
        FAILEDMUTATTEMTS_PERFORM_NOADDLINK_FIND.description = "Failed to "
                + "find an linking vertex";
        FAILEDMUTATTEMTS_PERFORM_NOADDLINK_EDIT.description = "Failed to "
                + "replace edge with with new vertex and edges";
        FAILEDMUTATTEMTS_PERFORM_NOEXTEND.description = "Mutation did not "
                + "extend the graph";
        FAILEDMUTATTEMTS_PERFORM_NODELETE.description = "Mutation did not "
                + "delete vertex";
        FAILEDMUTATTEMTS_SETUPRINGS.description = "Failed attempts to setup "
                + "rings "
                + "in a mutated offspring";
        FAILEDMUTATTEMTS_EVAL.description = "Failed attempt to pass graph "
                + "evaluation test from mutated offspring";
        FAILEDMUTATTEMTS_FORBENDS.description = "Mutated offsprings that led "
                + "to forbidden ends";
        
        BUILDANEWATTEMPTS.description = "Attempts to do build graph from "
                + "scratch";
        FAILEDBUILDATTEMPTS.description = "Failed attempts to do build a graph "
                + "from scratch";
        FAILEDBUILDATTEMPTS_GRAPHBUILD.description = "Failed attempts to "
                + "generate a graph";
        FAILEDBUILDATTEMPTS_EVAL.description = "Failed attempts to pass graph "
                + "evaluation test from newly built graphs";
        FAILEDBUILDATTEMPTS_SETUPRINGS.description = "Failed attempt to setup "
                + "rings in a newly generated graph";
        FAILEDBUILDATTEMPTS_FORBIDENDS.description = "Construction of new "
                + "graphs that ped to forbidden ends";
        
        
        FITNESSEVALS.description = "Number of fitness evaluations";
        FAILEDFITNESSEVALS.description = "Number of failed fitness evaluations";
        
        DUPLICATEPREFITNESS.description = "Number of candidates with duplicate "
                + "UID detected prior to considering their fitness evaluation.";
        FAILEDDUPLICATEPREFITNESSDETECTION.description = "Number of failed "
                + "attempts to compare UID with known UIDs prior to considering "
                + "the fitness evaluation of a candidate.";
    }
    
    /**
     * @return a printable description of what this counter identifier 
     * refers to.
     */
    public String getDescription()
    {
        return description;
    }
}
