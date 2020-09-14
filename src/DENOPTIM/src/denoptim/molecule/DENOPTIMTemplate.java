package denoptim.molecule;

import java.util.ArrayList;
import java.util.List;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.utils.GraphUtils;

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

public class DENOPTIMTemplate extends DENOPTIMVertex implements IGraphBuildingBlock 
{
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

//------------------------------------------------------------------------------

    private DENOPTIMTemplate() 
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

    public ArrayList<DENOPTIMAttachmentPoint> getCurrentAPs() 
    {
        return interiorGraph.getVertexAtPosition(0).getAttachmentPoints();
    }

//------------------------------------------------------------------------------

    /*
     * Method meant for devel phase only. 
     */
    //TODO: Remove.

    public static DENOPTIMTemplate getTestTemplate() 
    {
        DENOPTIMTemplate template = new DENOPTIMTemplate();

        
        // Adding fully defined vertexes (they point to an actual fragment
        DENOPTIMVertex vA = new DENOPTIMVertex(
                GraphUtils.getUniqueVertexIndex(), 0, 1);
        DENOPTIMVertex vB = new DENOPTIMVertex(
                GraphUtils.getUniqueVertexIndex(), 1, 1);
        template.interiorGraph.addVertex(vA);
        template.interiorGraph.addVertex(vB);
        
        // Make and add a fully defined edge that connects the two vertexes
        DENOPTIMAttachmentPoint apOnA = vA.getAttachmentPoints().get(0);
        DENOPTIMAttachmentPoint apOnB = vB.getAttachmentPoints().get(1);
        String srcAPClass = apOnA.getAPClass();
        String trgAPClass = apOnA.getAPClass();
        DENOPTIMEdge eAB = new DENOPTIMEdge(vA.getVertexId(), vB.getVertexId(), 
                0, 1, FragmentSpace.getBondOrderForAPClass(apOnA.getAPRule()), 
                srcAPClass, trgAPClass);
        template.interiorGraph.addEdge(eAB);
        
        System.out.println("TEMPLATE's inner graph: "+template.interiorGraph);
        
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
    	//TODO: probably this will change
        return interiorGraph.getVertexAtPosition(0).getAttachmentPoints();
    }

//------------------------------------------------------------------------------
    
    /**
     * Returns the list of attachment points
     * @return the list of APs
     */
    public DENOPTIMTemplate clone()
    {
    	//TODO: implement deep cloning. Now it just returns the original ;-)
    	return this;
    }

//------------------------------------------------------------------------------
    
    //TODO
	public ArrayList<String> getAllAPClassess() {
		ArrayList<String> l = new ArrayList<String>();
		l.add("templateAPClass");
		return l;
	}

//------------------------------------------------------------------------------

	//TODO
	public int getAPCount() {
		return 1;
	}

//------------------------------------------------------------------------------

    @Override
    public ArrayList<SymmetricSet> getSymmetricAPsSets()
    {
        // TODO Now this returns an empty list, but it will have to detect
        // the symmetric sets in the underlying graph
        return new ArrayList<SymmetricSet>();
    }
    
//-----------------------------------------------------------------------------
    
    //TODO: this will probably change, not it is useful for debugging
    public DENOPTIMGraph getEmbeddeGraph()
    {
        return interiorGraph;
    }
    
//-----------------------------------------------------------------------------
}

