package denoptim.denoptimga;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import denoptim.graph.APMapping;
import denoptim.graph.DENOPTIMAttachmentPoint;
import denoptim.graph.DENOPTIMVertex;

/**
 * This class collects the data identifying the subgraphs that would be swapped
 * by a crossover event.
 */
public class XoverSite implements Cloneable
{
    /**
     * One of the two subgraphs.
     */
    private List<DENOPTIMVertex> subGraphA = null;

    /**
     * The other of the two subgraphs.
     */
    private List<DENOPTIMVertex> subGraphB = null;
    
    /**
     * Initiate an empty data structure
     */
    public XoverSite()
    {
        subGraphA = new ArrayList<DENOPTIMVertex>();
        subGraphB = new ArrayList<DENOPTIMVertex>();
    }
    
    /**
     * Create a site by listing the vertexes that belong to each of the 
     * subgraphs that should be swapped by a crossover operation.
     * @param subGraphA the vertexes defining a subgraph to be swapped.
     * @param subGraphB the vertexes defining another subgraph to be swapped.
     */
    public XoverSite(List<DENOPTIMVertex> subGraphA, List<DENOPTIMVertex> subGraphB)
    {
        this.subGraphA = new ArrayList<DENOPTIMVertex>(subGraphA);
        this.subGraphB = new ArrayList<DENOPTIMVertex>(subGraphB);
    }

    /**
     * Creates a new {@link XoverSite} that considers the opposite order of
     * candidates of this one.
     */
    public XoverSite createMirror()
    {
        XoverSite mirror = new XoverSite();
        mirror.subGraphA = new ArrayList<DENOPTIMVertex>(this.subGraphB);
        mirror.subGraphB = new ArrayList<DENOPTIMVertex>(this.subGraphA);
        return mirror;
    }
    
    /**
     * Creates a new {@link XoverSite} that contains the same information of
     * this one, i.e., a shallow copy.
     */
    public XoverSite clone()
    {
        XoverSite clone = new XoverSite();
        clone.subGraphA = new ArrayList<DENOPTIMVertex>(this.subGraphA);
        clone.subGraphB = new ArrayList<DENOPTIMVertex>(this.subGraphB);
        return clone;
    }

    /**
     * Returns the collection of vertexes belonging to the first subgraph.
     */
    public List<DENOPTIMVertex> getA()
    {
        return subGraphA;
    }
    
    /**
     * Returns the collection of vertexes belonging to the second subgraph.
     */
    public List<DENOPTIMVertex> getB()
    {
        return subGraphB;
    }
    
    public boolean equals(XoverSite other)
    {
        return this.subGraphA.equals(other.subGraphA) 
                && this.subGraphA.equals(other.subGraphA);
    }
    
//------------------------------------------------------------------------------
    
}
