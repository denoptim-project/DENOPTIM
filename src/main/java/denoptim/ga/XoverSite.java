package denoptim.ga;

import java.util.ArrayList;
import java.util.List;

import denoptim.exception.DENOPTIMException;
import denoptim.graph.DENOPTIMAttachmentPoint;
import denoptim.graph.DENOPTIMGraph;
import denoptim.graph.DENOPTIMTemplate;
import denoptim.graph.DENOPTIMVertex;
import denoptim.utils.CrossoverType;

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
     * List of attachment points on subgraph B and that need to be replaced by 
     * a corresponding
     * attachment point in case of crossover. These APs might be used or free,
     * which essentially means they are either required somehow, or are used in
     * an outer level, i.e., projected onto an embedding template and used from
     * there.
     */
    private List<DENOPTIMAttachmentPoint> needyAPsOnA = null;

    /**
     * The other of the two subgraphs.
     */
    private List<DENOPTIMVertex> subGraphB = null;
    
    /**
     * List of attachment points on subgraph A and that need to be replaced by 
     * a corresponding
     * attachment point in case of crossover. These APs might be used or free,
     * which essentially means they are either required somehow, or are used in
     * an outer level, i.e., projected onto an embedding template and used from
     * there.
     */
    private List<DENOPTIMAttachmentPoint> needyAPsOnB = null;
    
    /**
     * Type of crossover
     */
    private CrossoverType xoverType = null;
    
//------------------------------------------------------------------------------
    
    /**
     * Initiate an empty data structure
     */
    public XoverSite()
    {
        subGraphA = new ArrayList<DENOPTIMVertex>();
        subGraphB = new ArrayList<DENOPTIMVertex>();
        needyAPsOnA = new ArrayList<DENOPTIMAttachmentPoint>();
        needyAPsOnB = new ArrayList<DENOPTIMAttachmentPoint>();
    }

//------------------------------------------------------------------------------
    
    /**
     * Create a site by listing the vertexes that belong to each of the 
     * subgraphs that should be swapped by a crossover operation.
     * @param subGraphA the vertexes defining a subgraph to be swapped. The first
     * vertex must be the deepest one, i.e., the source of a directed spanning 
     * tree.
     * @param subGraphB the vertexes defining another subgraph to be swapped.
     * The first
     * vertex must be the deepest one, i.e., the source of a directed spanning 
     * tree.
     * @param xoverType the type of crossover.
     */
    public XoverSite(List<DENOPTIMVertex> subGraphA, 
            List<DENOPTIMVertex> subGraphB,
            CrossoverType xoverType)
    {
        this.subGraphA = new ArrayList<DENOPTIMVertex>(subGraphA);
        this.subGraphB = new ArrayList<DENOPTIMVertex>(subGraphB);
        DENOPTIMGraph gA = subGraphA.get(0).getGraphOwner();
        this.needyAPsOnA = gA.getInterfaceAPs(subGraphA);
        DENOPTIMGraph gB = subGraphB.get(0).getGraphOwner();
        this.needyAPsOnB = gB.getInterfaceAPs(subGraphB);
        this.xoverType = xoverType;
    }

//------------------------------------------------------------------------------
    
    /**
     * Create a site by listing the vertexes that belong to each of the 
     * subgraphs that should be swapped by a crossover operation.
     * @param subGraphA the vertexes defining a subgraph to be swapped. The first
     * vertex must be the deepest one, i.e., the source of a directed spanning 
     * tree.
     * @param needyAPsOnA a subset of the attachment points in the first 
     * subgraph that are required to have a mapping in the second subgraph.
     * @param subGraphB the vertexes defining another subgraph to be swapped.
     * The first
     * vertex must be the deepest one, i.e., the source of a directed spanning 
     * tree.
     * @param needyAPsOnB a subset of the attachment points in the second 
     * subgraph that are required to have an AP mapping in the first subgraph.
     * @param xoverType the type of crossover.
     */
    public XoverSite(List<DENOPTIMVertex> subGraphA, 
            List<DENOPTIMAttachmentPoint> needyAPsOnA,
            List<DENOPTIMVertex> subGraphB, 
            List<DENOPTIMAttachmentPoint> needyAPsOnB,
            CrossoverType xoverType)
    {
        this.subGraphA = new ArrayList<DENOPTIMVertex>(subGraphA);
        this.subGraphB = new ArrayList<DENOPTIMVertex>(subGraphB);
        this.needyAPsOnA = new ArrayList<DENOPTIMAttachmentPoint>(needyAPsOnA);
        this.needyAPsOnB = new ArrayList<DENOPTIMAttachmentPoint>(needyAPsOnB);
        this.xoverType = xoverType;
    }

//------------------------------------------------------------------------------
    
    /**
     * Creates a new {@link XoverSite} that considers the opposite order of
     * candidates of this one.
     */
    public XoverSite createMirror()
    {
        XoverSite mirror = new XoverSite();
        mirror.subGraphA = new ArrayList<DENOPTIMVertex>(this.subGraphB);
        mirror.subGraphB = new ArrayList<DENOPTIMVertex>(this.subGraphA);
        mirror.needyAPsOnA = new ArrayList<DENOPTIMAttachmentPoint>(
                this.needyAPsOnB);
        mirror.needyAPsOnB = new ArrayList<DENOPTIMAttachmentPoint>(
                this.needyAPsOnA);
        mirror.xoverType = xoverType;
        return mirror;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Creates a new {@link XoverSite} that contains the same information of
     * this one, i.e., a shallow copy.
     */
    public XoverSite clone()
    {
        XoverSite clone = new XoverSite();
        clone.subGraphA = new ArrayList<DENOPTIMVertex>(this.subGraphA);
        clone.subGraphB = new ArrayList<DENOPTIMVertex>(this.subGraphB);
        clone.needyAPsOnA = new ArrayList<DENOPTIMAttachmentPoint>(
                this.needyAPsOnA);
        clone.needyAPsOnB = new ArrayList<DENOPTIMAttachmentPoint>(
                this.needyAPsOnB);
        clone.xoverType = xoverType;
        return clone;
    }

//------------------------------------------------------------------------------
    
    /**
     * Returns the collection of vertexes belonging to the first subgraph.
     * @return the collection of vertexes belonging to the first subgraph.
     */
    public List<DENOPTIMVertex> getA()
    {
        return subGraphA;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the collection of vertexes belonging to the second subgraph.
     * @return the collection of vertexes belonging to the second subgraph.
     */
    public List<DENOPTIMVertex> getB()
    {
        return subGraphB;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the collection of attachment points that need to be mapped 
     * in order to achieve a valid crossover that swaps the subgraphs and 
     * respects the requirements of each side.
     * @return the collection of "needy" attachment points belonging to the 
     * first subgraph.
     */
    public List<DENOPTIMAttachmentPoint> getAPsNeedingMappingA()
    {
        return needyAPsOnA;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the collection of attachment points that need to be mapped 
     * in order to achieve a valid crossover that swaps the subgraphs and 
     * respects the requirements of each side.
     * @return the collection of "needy" attachment points belonging to the 
     * second subgraph.
     */
    public List<DENOPTIMAttachmentPoint> getAPsNeedingMappingB()
    {
        return needyAPsOnB;
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the type of crossover.
     * @return the type of crossover.
     */
    public CrossoverType getType()
    {
        return xoverType;
    }
    
//------------------------------------------------------------------------------

    /**
     * Compares this and another instance.
     * @param other
     * @return <code>true</code> is the two include lists of vertexes that have
     * the same vertex in the same order (i.e., {@link List#equals} are true
     * for each collections individually).
     */
    @Override
    public boolean equals(Object o)
    {
        if (! (o instanceof XoverSite))
            return false;
        XoverSite other = (XoverSite) o;
        return this.xoverType==other.xoverType
                && this.subGraphA.equals(other.subGraphA) 
                && this.subGraphB.equals(other.subGraphB)
                && this.needyAPsOnA.equals(other.needyAPsOnA)
                && this.needyAPsOnB.equals(other.needyAPsOnB);
    }

//------------------------------------------------------------------------------
    
    /**
     * Creates a new instance of this class that contains the list of vertexes
     * that correspond to those contained in this {@link XoverSite} 
     * but with references to the vertexes of an entirely new pair of graphs 
     * made as clones of the original graphs. Note that since the graphs can be
     * embedded in templates, the entire embedding structure is cloned as well.
     * @return an analog yet independent crossover site.
     * @throws DENOPTIMException 
     */
    public XoverSite projectToClonedGraphs() throws DENOPTIMException
    {
        DENOPTIMGraph gA = subGraphA.get(0).getGraphOwner(); 
        DENOPTIMGraph gB = subGraphB.get(0).getGraphOwner();
        List<DENOPTIMTemplate> embeddingPathA = gA.getEmbeddingPath();
        List<DENOPTIMTemplate> embeddingPathB = gB.getEmbeddingPath();
        
        DENOPTIMGraph cloneOutermostGraphA, cloneA;
        DENOPTIMGraph cloneOutermostGraphB, cloneB;
        if (embeddingPathA.size()==0) 
        {
            cloneOutermostGraphA = gA.clone();
            cloneA = cloneOutermostGraphA;
        } else {
            cloneOutermostGraphA = embeddingPathA.get(0)
                    .getGraphOwner().clone();
            cloneA = DENOPTIMGraph.getEmbeddedGraphInClone(cloneOutermostGraphA, 
                    embeddingPathA.get(0).getGraphOwner(), embeddingPathA);
        }
        if (embeddingPathB.size()==0) 
        {
            cloneOutermostGraphB = gB.clone();
            cloneB = cloneOutermostGraphB;
        } else {
            cloneOutermostGraphB = embeddingPathB.get(0)
                    .getGraphOwner().clone();
            cloneB = DENOPTIMGraph.getEmbeddedGraphInClone(cloneOutermostGraphB, 
                    embeddingPathB.get(0).getGraphOwner(), embeddingPathB);
        }
        
        cloneA.renumberGraphVertices();
        cloneB.renumberGraphVertices();
        List<DENOPTIMVertex> refsOnCloneA = new ArrayList<DENOPTIMVertex>();
        List<DENOPTIMAttachmentPoint> needyOnCloneA = 
                new ArrayList<DENOPTIMAttachmentPoint>();
        for (DENOPTIMVertex vA : subGraphA)
        {
            DENOPTIMVertex vCloneA = cloneA.getVertexAtPosition(
                    vA.getGraphOwner().indexOf(vA));
            refsOnCloneA.add(vCloneA);
            for (DENOPTIMAttachmentPoint ap : vA.getAttachmentPoints())
            {
                if (needyAPsOnA.contains(ap))
                {
                    needyOnCloneA.add(vCloneA.getAP(ap.getIndexInOwner()));
                }
            }
        }
        List<DENOPTIMVertex> refsOnCloneB = new ArrayList<DENOPTIMVertex>();
        List<DENOPTIMAttachmentPoint> needyOnCloneB = 
                new ArrayList<DENOPTIMAttachmentPoint>();
        for (DENOPTIMVertex vB : subGraphB)
        {
            DENOPTIMVertex vCloneB = cloneB.getVertexAtPosition(
                    vB.getGraphOwner().indexOf(vB));
            refsOnCloneB.add(vCloneB);
            for (DENOPTIMAttachmentPoint ap : vB.getAttachmentPoints())
            {
                if (needyAPsOnB.contains(ap))
                {
                    needyOnCloneB.add(vCloneB.getAP(ap.getIndexInOwner()));
                }
            }
        }
        XoverSite xos = new XoverSite(refsOnCloneA, needyOnCloneA,
                refsOnCloneB, needyOnCloneB, xoverType);
        return xos;
    }
    
//------------------------------------------------------------------------------

    /**
     * Produced a string for showing what this object is.
     */
    public String toString()
    {
        return "[XoverSite: "+subGraphA+", "+subGraphB+"]";
    }
    
//------------------------------------------------------------------------------

}
