package denoptim.molecule;

import java.util.ArrayList;
import java.util.List;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.utils.GraphConversionTool;
import denoptim.utils.GraphUtils;

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
    
    //TODO-V3 to enum
    /*
     * 0:scaffold, 1:fragment, 2:capping group
     */
    private int buildingBlockType = -99; //Initialised to meaningless value

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
    private int contractLevel = 0;
    private List<DENOPTIMAttachmentPoint> exteriorAPs;

//------------------------------------------------------------------------------

    public DENOPTIMTemplate()
    {
        super();
    }

//------------------------------------------------------------------------------

    /**
     * @param exteriorAPs Attachment points that are always present and vacant.
     * @return Template with an (initially empty) unconstrained interior graph.
     */
    private static DENOPTIMTemplate getEmptyTemplate(List<DENOPTIMAttachmentPoint> exteriorAPs) 
    {
        DENOPTIMTemplate template = new DENOPTIMTemplate();
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
    public int getFragmentType()
    {
        return buildingBlockType;
    }
    
//------------------------------------------------------------------------------

    public void setFragmentType(int fType)
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

    public static DENOPTIMTemplate getTestTemplate() 
    {
        DENOPTIMTemplate template = new DENOPTIMTemplate();
        
        

        // Adding fully defined vertexes (they point to an actual fragment
        DENOPTIMVertex vA = DENOPTIMVertex.newVertexFromLibrary(
                GraphUtils.getUniqueVertexIndex(), 0, 1);
        DENOPTIMVertex vB = DENOPTIMVertex.newVertexFromLibrary(
                GraphUtils.getUniqueVertexIndex(), 1, 1);
        template.interiorGraph.addVertex(vA);
        template.interiorGraph.addVertex(vB);
        
        // Make and add a fully defined edge that connects the two vertexes
        DENOPTIMAttachmentPoint apOnA = vA.getAttachmentPoints().get(0);
        DENOPTIMAttachmentPoint apOnB = vB.getAttachmentPoints().get(1);
        String srcAPClass = apOnA.getAPClass();
        String trgAPClass = apOnB.getAPClass();
        DENOPTIMEdge eAB = vA.connectVertices(vB,0,1,srcAPClass,trgAPClass);

        template.interiorGraph.addEdge(eAB);
        

        //TODO del
        System.out.println("TEMPLATE's inner graph: "+template.interiorGraph);
        System.out.println("TEMPLATE's APs: ");
        for (DENOPTIMAttachmentPoint ap : template.getAttachmentPoints())
            System.out.println("  "+ap+" free="+ap.isAvailable());
        
        
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
    	//TODO-V3: implement deep cloning. Now it just building a new test template
    	return getTestTemplate();
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

//-----------------------------------------------------------------------------
}

