package denoptimga;

/**
 * Identifier of a counter. A printable description is given by method
 * {@link CounterID#getDescription()}.
 */

public enum CounterID 
{
    //GENERATEDGRAPHS, 
    NEWCANDIDATEATTEMPTS, 
    XOVERATTEMPTS, FAILEDXOVERATTEMPTS, XOVERPARENTSEARCH,
    MUTATTEMTS, FAILEDMUTATTEMTS, MUTPARENTSEARCH,
    BUILDANEWATTEMPTS, FAILEDBUILDATTEMPTS,
    FITNESSEVALS, FAILEDFITNESSEVALS;
    
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
        
        MUTATTEMTS.description = "Attempts to do build graph by mutation";
        FAILEDMUTATTEMTS.description = "Failed attempts to do build a graph "
                + "by Mutation";
        MUTPARENTSEARCH.description = "Attempts to find a parent that supports "
                + " mutation";
        
        BUILDANEWATTEMPTS.description = "Attempts to do build graph from "
                + "scratch";
        FAILEDBUILDATTEMPTS.description = "Failed attempts to do build a graph "
                + "from scratch";
        
        FITNESSEVALS.description = "Number of fitness evaluations";
        FAILEDFITNESSEVALS.description = "Number of failed fitness evaluations";
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
