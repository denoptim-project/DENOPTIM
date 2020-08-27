package denoptim.molecule;

import java.util.ArrayList;
import java.util.List;

import denoptim.constants.DENOPTIMConstants;
import denoptim.fragspace.FragmentSpace;
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;

/**
 * Templates are a variation on fragments that has a number of attachment points and an interior graph. The attachment
 * points are constant and always available (i.e. uncapped), but the interior graph can vary.
 * The constant attachment points makes it easy to control the chemical environment in which the Template is embedded
 * while the interior graph provides the flexibility to change the content between these attachment points.
 * The degree to which the interior graph can vary is dependent on the contract level of the Template, which can be
 * provided upon instantiation.
 */
public class DENOPTIMTemplate extends AtomContainer implements IAtomContainer {

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

    private DENOPTIMTemplate() {
        super();
    }


    /**
     * @param exteriorAPs Attachment points that are always present and vacant.
     * @return Template with an (initially empty) unconstrained interior graph.
     */
    private static DENOPTIMTemplate getEmptyTemplate(List<DENOPTIMAttachmentPoint> exteriorAPs) {
        DENOPTIMTemplate template = new DENOPTIMTemplate();
        return template;
    }


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

    public ArrayList<DENOPTIMAttachmentPoint> getCurrentAPs() {
        return interiorGraph.getVertexAtPosition(0).getAttachmentPoints();
    }

    public static DENOPTIMTemplate getTestTemplate() {
	DENOPTIMTemplate template = new DENOPTIMTemplate();
        Atom anAtom = new Atom("C");
        // anAtom.setProperty(DENOPTIMConstants.APTAG, );
        template.addAtom(anAtom);
        template.setProperty(DENOPTIMConstants.APTAG, "1:1");
        ArrayList apList = new ArrayList(1);
        apList.add(new DENOPTIMAttachmentPoint(0, 1, 1));
        template.interiorGraph.addVertex(new DENOPTIMVertex(-1, 1, apList, 1));
        template.setProperty(DENOPTIMConstants.APCVTAG, "1#c0:0:1.1%0.0%0.0");
        // Add attachment points as property
        return template;
    }

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
}

