package denoptim.molecule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.molecule.DENOPTIMFragment.BBType;
import denoptim.utils.GraphConversionTool;
import denoptim.utils.GraphUtils;
import denoptim.utils.MutationType;

import org.apache.commons.math3.util.OpenIntToDoubleHashMap.Iterator;
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;

/**
 * A template is a contract that defines subgraph features that can go from 
 * a list of attachment points, via a partially defined subgraph structure, to
 * a fully defined subgraph (i.e., graph structure + identify of each vertex).
 */

// TODO: remove, later. 
//  Template are variation on fragments that has a number of attachment points and an interior graph. The attachment
//  points are constant and always available (i.e. uncapped), but the interior graph can vary.
//  The constant attachment points makes it easy to control the chemical environment in which the Template is embedded
//  while the interior graph provides the flexibility to change the content between these attachment points.
//  The degree to which the interior graph can vary is dependent on the contract level of the Template, which can be
//  provided upon instantiation.
//

public class DENOPTIMTemplate extends DENOPTIMVertex
{
    /**
     * Version UID
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * Index of the graph building block contained in the vertex
     */
    private int buildingBlockId = -99; //Initialised to meaningless value
    
    /*
     * Building block type distinguished among scaffolds, fragments, and capping
     * groups, if defined.
     */
    private BBType buildingBlockType = BBType.UNDEFINED;

    /**
     * interior graph of the template that can be constrained in various ways depending on the
     * <code>contractLevel</code>
     */
    private DENOPTIMGraph interiorGraph = new DENOPTIMGraph();

    /**
     * Denotes the constants in the template. It can take values from 0 to 2 and each level promises to uphold the
     * contracts of previous levels. 0 means exterior attachment points are constant, 1 means the subgraph structure is
     * constant, and 2 means the vertices are constant.
     */
    //TODO-V3: make enum
    private int contractLevel = 0;
    private List<DENOPTIMAttachmentPoint> exteriorAPs;

//------------------------------------------------------------------------------

    public DENOPTIMTemplate(BBType bbType)
    {
        super();
        setBuildingBlockType(bbType);
    }

//------------------------------------------------------------------------------

    /**
     * @param exteriorAPs Attachment points that are always present and vacant.
     * @return Template with an (initially empty) unconstrained interior graph.
     */
    private static DENOPTIMTemplate getEmptyTemplate(List<DENOPTIMAttachmentPoint> exteriorAPs) 
    {
        DENOPTIMTemplate template = new DENOPTIMTemplate(BBType.UNDEFINED);
        return template;
    }
    
//------------------------------------------------------------------------------

    /**
     *
     * @return the id of the molecule
     */
    public int getMolId()
    {
        return buildingBlockId;
    }

//------------------------------------------------------------------------------

    public void setMolId(int m_molId)
    {
        buildingBlockId = m_molId;
    }

//------------------------------------------------------------------------------

    /**
     *
     * @return <code>true</code> if vertex is a fragment
     */
    public BBType getFragmentType()
    {
        return buildingBlockType;
    }
    
//------------------------------------------------------------------------------

    private void setBuildingBlockType(BBType fType)
    {
        buildingBlockType = fType;
    }


//------------------------------------------------------------------------------

    /**
     * @return Template with a random, unconstrained interior graph. The interior graph consists of one fragment.
     * The exterior attachment points are those of the initial fragment.
     */
    
    /*
    public static DENOPTIMTemplate getRandomTemplate() {
        // TODO: Implement method
        int fragmentLibrarySize = FragmentSpace.getFragmentLibrary().size();
        int molecularId = (int) (Math.random() * fragmentLibrarySize);
        try {
            IAtomContainer exteriorAPs = FragmentSpace.getFragment(1, molecularId);
        } catch (DENOPTIMException e) {
            System.err.println(e.getMessage());
        }
        return new DENOPTIMTemplate(0, molecularId, );
    }
    */
    
//------------------------------------------------------------------------------

    /**
     * Method meant for devel phase only.
     */
    //TODO-V3 Remove.
    
    public static DENOPTIMTemplate getTestTemplate(int contractLevel) 
    {
        DENOPTIMTemplate template = new DENOPTIMTemplate(BBType.UNDEFINED);
        DENOPTIMVertex vrtx = new EmptyVertex(0);
        vrtx.addAttachmentPoint(new DENOPTIMAttachmentPoint(vrtx,0,1,1));
        vrtx.addAttachmentPoint(new DENOPTIMAttachmentPoint(vrtx,1,1,1));

        DENOPTIMVertex vrtx2 = new EmptyVertex(1);
        vrtx2.addAttachmentPoint(new DENOPTIMAttachmentPoint(vrtx2,0,1,1));
        vrtx2.addAttachmentPoint(new DENOPTIMAttachmentPoint(vrtx2,1,1,1));

        template.interiorGraph.addVertex(vrtx);
        template.interiorGraph.addVertex(vrtx2);
        template.interiorGraph.addEdge(new DENOPTIMEdge(vrtx.getAP(0),
                vrtx2.getAP(1), 0,1,0,1));
        
        //Fully frozen
        template.contractLevel = contractLevel;
        return template;
    }

//------------------------------------------------------------------------------

    /**
     * Method meant for devel phase only. 
     */
    //TODO-V3 Remove.

    
    // WARNING! This is only meant to return a "scaffold" type
    
    public static DENOPTIMTemplate getTestScaffoldTemplate() 
    {
        if (FragmentSpaceParameters.useCyclicTemplate())
            return getCyclicTemplate(BBType.SCAFFOLD);
        else
            return getAcyclicTemplate(BBType.SCAFFOLD);
    }
    
//------------------------------------------------------------------------------

    /**
     * Method meant for devel phase only. 
     */
    //TODO-V3 Remove.

    
    // WARNING! This is only meant to return a "scaffold" type
    
    public static DENOPTIMTemplate getTestFragmentTemplate() 
    {
        if (FragmentSpaceParameters.useCyclicTemplate())
            return getCyclicTemplate(BBType.FRAGMENT);
        else
            return getAcyclicTemplate(BBType.FRAGMENT);
    }  
    
//------------------------------------------------------------------------------
    
    //TODO-V3: this will be removed/replaced
    private static DENOPTIMTemplate getAcyclicTemplate(BBType bbt)
    {
        DENOPTIMTemplate template = new DENOPTIMTemplate(bbt);
        
        // Adding fully defined vertexes (they point to an actual fragment
        DENOPTIMVertex vA = DENOPTIMVertex.newVertexFromLibrary(
                GraphUtils.getUniqueVertexIndex(), 0, BBType.FRAGMENT);
        DENOPTIMVertex vB = DENOPTIMVertex.newVertexFromLibrary(
                GraphUtils.getUniqueVertexIndex(), 1, BBType.FRAGMENT);
        template.interiorGraph.addVertex(vA);
        template.interiorGraph.addVertex(vB);
        
        // Make and add a fully defined edge that connects the two vertexes
        DENOPTIMEdge eAB = vA.connectVertices(vB, 0, 1);
        //DENOPTIMEdge eAB = GraphUtils.connectVertices(vA,vB,0,1,srcAPClass,trgAPClass);

        template.interiorGraph.addEdge(eAB);
        
        
        /*
        System.out.println("TEMPLATE's inner graph: "+template.interiorGraph);
        System.out.println("TEMPLATE's APs: ");
        //for (DENOPTIMAttachmentPoint ap : template.getAttachmentPoints())
        //    System.out.println("  "+ap+" free="+ap.isAvailable());
        */
        
        template.freezeTemplate();
        
        return template;
    }
    
//------------------------------------------------------------------------------
    
    //TODO-V3: this will be removed/replaced
    
    private static DENOPTIMTemplate getCyclicTemplate(BBType bbt) 
    {
        // Here we build a graph with ring and two unused APs (the *):
        //
        //   H    H H    * H    H H    *
        //    \  /   \  /   \  /   \  /
        //     vA-----vB-----vC-----vD
        //      |                   |
        //     vRCV1...(chord)....vRCV2 
        //
        
        
        //TODO: back to fragment
        //DENOPTIMTemplate template = new DENOPTIMTemplate(BBType.FRAGMENT);
        DENOPTIMTemplate template = new DENOPTIMTemplate(bbt);
        
        DENOPTIMVertex vA = DENOPTIMVertex.newVertexFromLibrary(
                GraphUtils.getUniqueVertexIndex(), 0, BBType.FRAGMENT);
        DENOPTIMVertex vB = DENOPTIMVertex.newVertexFromLibrary(
                GraphUtils.getUniqueVertexIndex(), 0, BBType.FRAGMENT);
        DENOPTIMVertex vC = DENOPTIMVertex.newVertexFromLibrary(
                GraphUtils.getUniqueVertexIndex(), 0, BBType.FRAGMENT);
        DENOPTIMVertex vD = DENOPTIMVertex.newVertexFromLibrary(
                GraphUtils.getUniqueVertexIndex(), 0, BBType.FRAGMENT);
        DENOPTIMVertex vRCV1 = DENOPTIMVertex.newVertexFromLibrary(
                GraphUtils.getUniqueVertexIndex(), 4, BBType.FRAGMENT);
        DENOPTIMVertex vRCV2 = DENOPTIMVertex.newVertexFromLibrary(
                GraphUtils.getUniqueVertexIndex(), 5, BBType.FRAGMENT);
        
        DENOPTIMVertex vH1 = DENOPTIMVertex.newVertexFromLibrary(
                GraphUtils.getUniqueVertexIndex(), 0, BBType.CAP);
        DENOPTIMVertex vH2 = DENOPTIMVertex.newVertexFromLibrary(
                GraphUtils.getUniqueVertexIndex(), 0, BBType.CAP);
        DENOPTIMVertex vH3 = DENOPTIMVertex.newVertexFromLibrary(
                GraphUtils.getUniqueVertexIndex(), 0, BBType.CAP);
        DENOPTIMVertex vH4 = DENOPTIMVertex.newVertexFromLibrary(
                GraphUtils.getUniqueVertexIndex(), 0, BBType.CAP);
        DENOPTIMVertex vH5 = DENOPTIMVertex.newVertexFromLibrary(
                GraphUtils.getUniqueVertexIndex(), 0, BBType.CAP);
        DENOPTIMVertex vH6 = DENOPTIMVertex.newVertexFromLibrary(
                GraphUtils.getUniqueVertexIndex(), 0, BBType.CAP);
        
        template.interiorGraph.addVertex(vA);
        template.interiorGraph.addVertex(vB);
        template.interiorGraph.addVertex(vC);
        template.interiorGraph.addVertex(vD);
        template.interiorGraph.addVertex(vRCV1);
        template.interiorGraph.addVertex(vRCV2);
        template.interiorGraph.addVertex(vH1);
        template.interiorGraph.addVertex(vH2);
        template.interiorGraph.addVertex(vH3);
        template.interiorGraph.addVertex(vH4);
        template.interiorGraph.addVertex(vH5);
        template.interiorGraph.addVertex(vH6);
        
        DENOPTIMEdge eARcv = vA.connectVertices(vRCV1, 3, 0);
        DENOPTIMEdge eAB = vA.connectVertices(vB, 0, 3);
        DENOPTIMEdge eBC = vB.connectVertices(vC, 0, 3);
        DENOPTIMEdge eCD = vC.connectVertices(vD, 0, 3);
        DENOPTIMEdge eDRcv = vD.connectVertices(vRCV2, 0, 0);

        DENOPTIMEdge eAH1 = vA.connectVertices(vH1, 1, 0);
        DENOPTIMEdge eBH2 = vB.connectVertices(vH2, 1, 0);
        DENOPTIMEdge eCH3 = vC.connectVertices(vH3, 1, 0);
        DENOPTIMEdge eDH4 = vD.connectVertices(vH4, 1, 0);
        DENOPTIMEdge eAH5 = vA.connectVertices(vH5, 2, 0);
        DENOPTIMEdge eCH6 = vC.connectVertices(vH6, 2, 0);
        
        template.interiorGraph.addEdge(eARcv);
        template.interiorGraph.addEdge(eAB);
        template.interiorGraph.addEdge(eBC);
        template.interiorGraph.addEdge(eCD);
        template.interiorGraph.addEdge(eDRcv);
        template.interiorGraph.addEdge(eAH1);
        template.interiorGraph.addEdge(eBH2);
        template.interiorGraph.addEdge(eCH3);
        template.interiorGraph.addEdge(eDH4);
        template.interiorGraph.addEdge(eAH5);
        template.interiorGraph.addEdge(eCH6);
        
        DENOPTIMRing r = new DENOPTIMRing(new ArrayList<DENOPTIMVertex>(
                Arrays.asList(vRCV1, vA, vB, vC, vD, vRCV2)));
        template.interiorGraph.addRing(r);
        

        /*
        System.out.println("(F) TEMPLATE's inner graph: "+template.interiorGraph);
        System.out.println("(F) TEMPLATE's #HeavyAtoms: "+template.getHeavyAtomsCount());
        System.out.println("(F) TEMPLATE's #APs: "+template.getAttachmentPoints().size());
        //for (DENOPTIMAttachmentPoint ap : template.getAttachmentPoints())
        //    System.out.println("  "+ap+" free="+ap.isAvailable());
        */
        
        template.freezeTemplate();
        
        return template;
    }

//------------------------------------------------------------------------------

    /**
     * Promotes the contract level of this template to the highest value, i.e. makes the template unable to change after
     * calling this method.
     * @return true if already frozen. Else false.
     */
    public boolean freezeTemplate() {
        boolean isFrozen = contractLevel == 2;
        contractLevel = 2;
        return isFrozen;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the list of attachment points
     * @return the list of APs
     */
    public ArrayList<DENOPTIMAttachmentPoint> getAPs() 
    {
        return interiorGraph.getAvailableAPs();
    }

//------------------------------------------------------------------------------
    
    /**
     * Returns a deep copy of this template
     * @return the deep copy
     */
    public DENOPTIMTemplate clone()
    {
        DENOPTIMTemplate c;
        
    	//TODO-V3: implement deep cloning. Now it just building a new test template
        if (this.buildingBlockType == BBType.SCAFFOLD)
        {
    	    c = getTestScaffoldTemplate();
        } else {
            c = getTestFragmentTemplate();
        }
        
        c.setVertexId(this.getVertexId());
        
        return c;
    }
    
//-----------------------------------------------------------------------------
    
    //TODO: this will probably change, now it is useful for debugging
    public DENOPTIMGraph getEmbeddedGraph()
    {
        return interiorGraph;
    }
    
//-----------------------------------------------------------------------------
    
    protected void setInteriorGraph(DENOPTIMGraph graph) 
    {
        //TODO-V3 is this condition really sensible?
        if (!graph.getAvailableAPs().containsAll(exteriorAPs)) 
        {
            throw new IllegalArgumentException("Graph must have vacant APs " +
                    "same as exteriorAPs");
        }
    }
    
//-----------------------------------------------------------------------------

    protected void setExteriorAPs(List<DENOPTIMAttachmentPoint> exteriorAPs) {
        this.exteriorAPs = exteriorAPs;
    }

//-----------------------------------------------------------------------------
    
    @Override
    public ArrayList<DENOPTIMAttachmentPoint> getAttachmentPoints()
    {
        return interiorGraph.getAttachmentPoints();
    }
    
//-----------------------------------------------------------------------------
    
    // NB: since the list of APs of a template depends on the embedded graph,
    // setting a list of APs on a template should not be needed/useful. 
    // However, we need a way to set the constrained (i.e., required) APs that
    // a template must guarantee.
    // What we want to do on a template is to define a the constrains w.r.t.
    // the list of APs, rather than set the sets of list of APs itself.
    @Override
    public void setAttachmentPoints(ArrayList<DENOPTIMAttachmentPoint> lstAP)
    {
        // TODO Auto-generated method stub
    }

//-----------------------------------------------------------------------------
    
    //NB: since the symmetry depends on the embedded graph, what we want to do 
    // on a template is define a symmetry constrain rather than set the sets of 
    // symmetric vertices
    @Override
    protected void setSymmetricAPSets(ArrayList<SymmetricSet> m_Sap)
    {
        // TODO Auto-generated method stub
        
    }
    
//-----------------------------------------------------------------------------
    
    @Override
    public ArrayList<SymmetricSet> getSymmetricAPSets()
    {
        //TODO-V3: this cannot yet be done on a template because the 
        // SymmetricSet class holds only one set of integer identifiers that 
        // can be used to identify symmetric things like vertexes in a graph,
        // or APs belonging to the SAME vertex. However, the symmetric APs on 
        // a template can belong to different vertexes, meaning that 
        // identifying these APs with indexes requires at least two sets of
        // indexes (one for the vertex, one for the AP).
        // For the moment, we cannot return a sensible ArrayList<SymmetricSet>
        // thus we return an empty one.
        return new ArrayList<SymmetricSet>();
    }

//-----------------------------------------------------------------------------
       
    @Override
    public int getHeavyAtomsCount()
    {
        return interiorGraph.getHeavyAtomsCount();
    }

//-----------------------------------------------------------------------------
    
    @Override
    public boolean containsAtoms()
    {
        return interiorGraph.containsAtoms();
    }

//-----------------------------------------------------------------------------
    
    @Override
    public IAtomContainer getIAtomContainer()
    {
        try
        {
            return GraphConversionTool.convertGraphToMolecule(interiorGraph, true);
        } catch (DENOPTIMException e)
        {
            e.printStackTrace();
        }
        return null;
    }

//------------------------------------------------------------------------------

    @Override
    public Set<DENOPTIMVertex> getMutationSites()
    {
        Set<DENOPTIMVertex> set = new HashSet<DENOPTIMVertex>();
        
        // I doubt templates will ever be used as capping groups, but
        // just in case, then they are not considered mutable sites
        if (buildingBlockType == BBType.CAP
                || buildingBlockType == BBType.SCAFFOLD)
        {
            return set;
        }

        switch (contractLevel)
        { 
            case 2:
            {
                set.add(this);
                break;
            }
            
            default:
            {
                for (DENOPTIMVertex v : interiorGraph.gVertices)
                {
                    set.addAll(v.getMutationSites());
                }
                break;
            }
        }
        return set;
    }
    
//------------------------------------------------------------------------------
    
}

