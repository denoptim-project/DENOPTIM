package denoptim.molecule;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.io.File;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import denoptim.utils.MutationType;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.io.DenoptimIO;
import denoptim.molecule.DENOPTIMEdge.BondType;
import denoptim.rings.PathSubGraph;
import denoptim.threedim.ThreeDimTreeBuilder;
import denoptim.utils.DENOPTIMgson;
import denoptim.utils.GraphConversionTool;
import denoptim.utils.GraphUtils;

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
     * Graph that is embedded in this vertex.
     */
    private DENOPTIMGraph innerGraph;
    
    /**
     * Molecular representation of the content of this template. This filed is
     * meant to speed up handling of template's molecular analogue. 
     * In particular, rather than recalculating the molecular representation
     * multiple times, we make it once 
     * (upon calling {@link #getIAtomContainer()}) and we store the result until
     * further changes in the content of the template.
     */
    private IAtomContainer mol = null;
    
    /**
     * Denotes the constants in the template.
     */
    private ContractLevel contractLevel = ContractLevel.FIXED;
    
    /**
     * Enum specifying to what extent the template's inner graph can be changed.
     * <ul>
     * <li>{@link ContractLevel#FIXED} inner graphs are effectively equivalent 
     * to the DENOPTIMFragment class, as no change in the inner structure is
     * allowed.</il>
     * <li>{@link ContractLevel#FREE} inner graphs are free to change within 
     * the confines of the required APs.</il>
     * </ul>
     */
    public enum ContractLevel {
        FREE,
        FIXED
        //FIXED_STRUCT, //TO-BE-DEVELOPED
        /*
        <li>{@link ContractLevel#FIXED_STRUCT} will keep a constant 
        * inter-connectivity between vertices, but the content at each vertex may 
        * vary. this contract does not guarantee that the AP classes connecting 
        * inner
        * graph vertices will remain the same as the content at vertices change.
        * </il>
        */
    }

    private List<DENOPTIMAttachmentPoint> requiredAPs = new ArrayList<>();

    private APTreeMap innerToOuterAPs;

    
//------------------------------------------------------------------------------

    public DENOPTIMTemplate(DENOPTIMVertex.BBType bbType)
    {
        super(VertexType.Template);
        setBuildingBlockType(bbType);
    }

//------------------------------------------------------------------------------
    
    /**
     * Produces a pair of strings that identify the "path" between two given
     * attachment points. The two strings represent one the reverse path of
     * the other. So they identify the path when starting from each of the 
     * two APs.
     * @param apA
     * @param apB
     * @return a pair of strings that identify the "path" between two given
     * attachment points.
     */
    
    @Override
    public String[] getPathIDs(DENOPTIMAttachmentPoint apA,
            DENOPTIMAttachmentPoint apB)
    {
        String a2b = this.getBuildingBlockId() + "/" + this.getBuildingBlockType() + "/ap"
                + getIndexOfAP(apA) + "ap" + getIndexOfAP(apB) + "_";
        String b2a = this.getBuildingBlockId() + "/" + this.getBuildingBlockType() + "/ap"
                + getIndexOfAP(apB) + "ap" + getIndexOfAP(apA) + "_";

        return new String[]{a2b,b2a};
    }
    
//------------------------------------------------------------------------------

    /**
     * Method meant for devel phase only.
     * @throws DENOPTIMException 
     */
    
    public static DENOPTIMTemplate getTestTemplate(ContractLevel contractLevel) 
            throws DENOPTIMException 
    {
        DENOPTIMTemplate template = new DENOPTIMTemplate(
                DENOPTIMVertex.BBType.UNDEFINED);
        DENOPTIMVertex vrtx = new EmptyVertex(0);
        DENOPTIMVertex vrtx2 = new EmptyVertex(1);
    
        vrtx.addAP(0, 1, 1);
        vrtx.addAP(1, 1, 1);

        vrtx2.addAP(0, 1, 1);
        vrtx2.addAP(1, 1, 1);
        DENOPTIMGraph g = new DENOPTIMGraph();
        g.addVertex(vrtx);
        g.addVertex(vrtx2);
        g.addEdge(new DENOPTIMEdge(vrtx.getAP(0),
            vrtx2.getAP(1), BondType.SINGLE));
        template.setInnerGraph(g);

        //Fully frozen
        template.contractLevel = contractLevel;
        return template;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Imposes the given contract to this template.
     * @param contract the contract to impose on this template.
     */
    public void setContractLevel(ContractLevel contract)
    {
        this.contractLevel = contract;
    }

//------------------------------------------------------------------------------

    /**
     * Promotes the contract level of this template to the most constrained one
     * (i.e., {@link ContractLevel#FIXED}), 
     * i.e. makes the template unable to change after
     * calling this method.
     * @return true if already frozen. Else false.
     */
    public boolean freezeTemplate() {
        boolean isFrozen = contractLevel == ContractLevel.FIXED;
        contractLevel = ContractLevel.FIXED;
        return isFrozen;
    }

//------------------------------------------------------------------------------
    
    /**
     * Returns a deep copy of this template
     * @return the deep copy
     */
    public DENOPTIMTemplate clone()
    {
        DENOPTIMTemplate c = new DENOPTIMTemplate(this.getBuildingBlockType());
        
        c.setVertexId(this.getVertexId());
        c.setBuildingBlockId(this.getBuildingBlockId());
        c.contractLevel = this.contractLevel;
        c.setMutationTypes(this.getUnfilteredMutationTypes());

        for (DENOPTIMAttachmentPoint oriAP : this.requiredAPs)
        {
            c.addAP(oriAP.clone());
        }
        c.setInnerGraph(this.getInnerGraph().clone());
        
        return c;
    }
    
//-----------------------------------------------------------------------------
    
    //TODO: this will probably change, now it is useful for debugging
    public DENOPTIMGraph getInnerGraph()
    {
        return innerGraph;
    }

//-----------------------------------------------------------------------------

    public void setInnerGraph(DENOPTIMGraph innerGraph) 
            throws IllegalArgumentException 
    {
        // NB: if you change anything here, remember that we can modify the
        // inner graph, so the change you are implementing here might need to be
        // reflected in any place where the inner graph is modified (e.g.,
        // DENOPTIMGraph.insertSingleVertex()
        
        mol = null;
        if (!isValidInnerGraph(innerGraph)) {
            throw new IllegalArgumentException("inner graph does not have all" +
                    " required APs");
        }
        this.innerGraph = innerGraph;
        innerGraph.setTemplateJacket(this);
        this.innerToOuterAPs = new APTreeMap();
        for (DENOPTIMAttachmentPoint innerAP : innerGraph.getAvailableAPs()) {
            addInnerToOuterAPMapping(innerAP);
        }
    }

//-----------------------------------------------------------------------------
    
    //TODO-V3 possibly relocate this and make private
    public void updateInnerToOuter(TreeMap<Integer,DENOPTIMAttachmentPoint> map)
    {
        mol = null;
        this.innerToOuterAPs = new APTreeMap();
        for (Entry<Integer, DENOPTIMAttachmentPoint> e : map.entrySet())
        {
            DENOPTIMAttachmentPoint innerAP = innerGraph.getAPWithId(e.getKey());
            DENOPTIMAttachmentPoint outerAP = e.getValue();
            outerAP.setOwner(this);
            this.innerToOuterAPs.put(innerAP, outerAP);
        }
    }

//-----------------------------------------------------------------------------
    
    /**
     * Adds the projection of an AP in the template's inner graph (i.e., 
     * innerAP) to the list of APs visible from outside the template (i.e., the
     * outerAPs). The AP created on the template's surface is a clone of that
     * given as argument. If the given AP already has a mapping, nothing 
     * happens.
     * @param newInnerAP the inner AP to project on template's surface.
     */
    public void addInnerToOuterAPMapping(DENOPTIMAttachmentPoint newInnerAP)
    {
        if (innerToOuterAPs.containsKey(newInnerAP))
        {
            return;
        }
        DENOPTIMAttachmentPoint outerAP = newInnerAP.clone();
        outerAP.setOwner(this);
        innerToOuterAPs.put(newInnerAP, outerAP);
        // Recursion on nesting templates to add projections of the AP
        if (getGraphOwner() != null && getGraphOwner().templateJacket != null)
        {
            getGraphOwner().templateJacket.addInnerToOuterAPMapping(outerAP); 
        }
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Replaces a given link between APs on the surface of this template (i.e., 
     * outerAP) and the corresponding APs in the embedded graph 
     * (i.e., innerAPs). This method does not change anything about the outerAP;
     * it changes only the inner AP. If there is now mapping for the oldInnerAP,
     * then nothing happens.
     * @param oldInnerAP the inner AP to be changed
     * @param newInnerAP the inner AP to change the old one with.
     */
    public void updateInnerApID(DENOPTIMAttachmentPoint oldInnerAP, 
            DENOPTIMAttachmentPoint newInnerAP)
    {   
        if (!innerToOuterAPs.containsKey(oldInnerAP))
        {
            return;
        }
        DENOPTIMAttachmentPoint outerAP = innerToOuterAPs.get(oldInnerAP);
        innerToOuterAPs.remove(oldInnerAP);
        innerToOuterAPs.put(newInnerAP, outerAP);
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Removes the mapping of the given inner AP from this template's surface,
     * if such mapping exists. If the old innerAP maps to an outer AP that is
     * used, then the edge user and any vertex reachable from it are removed.
     * @param oldInnerAP the inner AP of the mapping to remove.
     */
    public void removeProjectionOfInnerAP(DENOPTIMAttachmentPoint oldInnerAP) 
            throws DENOPTIMException
    {
        if (!innerToOuterAPs.containsKey(oldInnerAP))
        {
            return;
        }
        DENOPTIMAttachmentPoint outer = innerToOuterAPs.get(oldInnerAP);
        if (!outer.isAvailable())
        {
            DENOPTIMAttachmentPoint linkedAP = outer.getLinkedAP();
            getGraphOwner().removeBranchStartingAt(linkedAP.getOwner());
        }
        // Recursion on nesting templates to remove all projections of the AP
        if (getGraphOwner() != null && getGraphOwner().templateJacket != null)
        {
            getGraphOwner().templateJacket.removeProjectionOfInnerAP(outer); 
        }
        innerToOuterAPs.remove(oldInnerAP);
    }
    
//-----------------------------------------------------------------------------
    
    private boolean isValidInnerGraph(DENOPTIMGraph g) 
    {
        List<DENOPTIMAttachmentPoint> innerAPs = g.getAvailableAPs();
        if (innerAPs.size() < getRequiredAPs().size()) {
            return false;
        }

        /* TODO-V3: is sorting needed?
        * Answer from Einar: Let n be the number of attachment points on the
        * graph. If we don't sort then this takes O(n²). If we sort then
        * O(nlog(n)) + O(n) = O(nlog(n)).
        * The question is actually on whether this sorting is compatible with
        * the assumption that the list of APs does not change order.
        */
        Comparator<DENOPTIMAttachmentPoint> apClassComparator
                = Comparator.comparing(DENOPTIMAttachmentPoint::getAPClass);
        innerAPs.sort(apClassComparator);
        List<DENOPTIMAttachmentPoint> reqAPs = getRequiredAPs();
        reqAPs.sort(apClassComparator);
        int matchesLeft = reqAPs.size();
        for (int i = 0, j = 0; matchesLeft > 0 && i < innerAPs.size(); i++) {
            if (apClassComparator.compare(innerAPs.get(i), reqAPs.get(j)) == 0) {
                matchesLeft--;
                j++;
            }
        }
        return matchesLeft == 0;
    }

//-----------------------------------------------------------------------------

    /**
     * Return the list of attachment points visible from outside the template, 
     * i.e., the so-called outer APs. Each outer AP is a projection of an AP
     * present in the embedded graph, i.e., inner AP.
     * @return the list of outer AP
     */
    @Override
    public ArrayList<DENOPTIMAttachmentPoint> getAttachmentPoints()
    {
        if (innerToOuterAPs == null)
            return new ArrayList<>();
        else
            return new ArrayList<>(innerToOuterAPs.values());
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
        mol = null;
        // TODO Auto-generated method stub
    }

//-----------------------------------------------------------------------------
    
    //NB: since the symmetry depends on the embedded graph, what we want to do 
    // on a template is define a symmetry constrain rather than set the sets of 
    // symmetric vertices
    @Override
    protected void setSymmetricAPSets(ArrayList<SymmetricSet> sAPs)
    {
        // TODO Auto-generated method stub
        
    }
    
//-----------------------------------------------------------------------------
    
    @Override
    public ArrayList<SymmetricSet> getSymmetricAPSets()
    {
        //TODO-V3: this cannot yet be done on a template because the 
        // SymmetricSet class holds only one set of integer identifiers that 
        // can be used to identify symmetric things like vertices in a graph,
        // or APs belonging to the SAME vertex. However, the symmetric APs on 
        // a template can belong to different vertices, meaning that
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
        return innerGraph.getHeavyAtomsCount();
    }

//-----------------------------------------------------------------------------
    
    //TODO-V3: test this. it fails for read-in templates!
    
    @Override
    public boolean containsAtoms()
    {
        return innerGraph.containsAtoms();
    }

//-----------------------------------------------------------------------------
    
    /**
     * The molecular representation, if any, is generated by this method and 
     * stored until further changes in the content of this template.
     * Successive calls of this method (i.e., prior to any other modification in
     * this template's content) return the result stored in the first run 
     * occurred after the last edit of this template's content.
     * @return the molecular representation of the content of this template.
     */
    @Override
    public IAtomContainer getIAtomContainer()
    {   
        if (mol != null)
        {
            return mol;
        }
        try
        {
            //TODO-V3 we might need to remove unused RCVs from inner graph.
            // Such RCVs cannot be used outside the template.
            
            ThreeDimTreeBuilder t3b = new ThreeDimTreeBuilder();
            IAtomContainer iac = t3b.convertGraphTo3DAtomContainer(
                    innerGraph, true);
            
            // We have to ensure outer APs point to the correct source atom in
            // the atom list of the entire molecular representation of the 
            // templates.
            // And we collects the outer APs per atom at the same time
            Map<Integer,List<DENOPTIMAttachmentPoint>> apsPerAtom = 
                    new TreeMap<>();
            for (DENOPTIMAttachmentPoint outAP : getAttachmentPoints()) {
                DENOPTIMAttachmentPoint inAP = getInnerAPFromOuterAP(outAP);
                outAP.setDirectionVector(inAP.getDirectionVector());
                int atmIndexInMol = inAP.getAtomPositionNumberInMol();
                outAP.setAtomPositionNumber(atmIndexInMol);
                if (apsPerAtom.containsKey(atmIndexInMol))
                {
                    apsPerAtom.get(atmIndexInMol).add(outAP);
                } else {
                    List<DENOPTIMAttachmentPoint> list = 
                            new ArrayList<DENOPTIMAttachmentPoint>();
                    list.add(outAP);
                    apsPerAtom.put(atmIndexInMol, list);
                }
            }

            for (int i = 0; i < iac.getAtomCount(); i++) {
                IAtom a = iac.getAtom(i);
                a.setProperty(DENOPTIMConstants.ATMPROPORIGINALATMID, i);
                a.setProperty(DENOPTIMConstants.ATMPROPVERTEXID,
                        getVertexId());
            }
            
            // Prepare SDF-like string for atom container
            // Done as in  DENOPTIMFragment.projectAPsToProperties
            String propAPClass = "";
            String propAttchPnt = "";
            for (Integer ii : apsPerAtom.keySet())
            {
                //WARNING: here is the 1-based criterion implemented
                int atmID = ii+1;
                
                List<DENOPTIMAttachmentPoint> apsOnAtm = apsPerAtom.get(ii);
                
                boolean firstCL = true;
                for (int i = 0; i<apsOnAtm.size(); i++)
                {
                    DENOPTIMAttachmentPoint ap = apsOnAtm.get(i);
        
                    //Build SDF property DENOPTIMConstants.APCVTAG
                    String stingAPP = ""; //String Attachment Point Property
                    if (firstCL)
                    {
                        firstCL = false;
                        stingAPP = ap.getSingleAPStringSDF(true);
                    } 
                    else 
                    {
                        stingAPP = DENOPTIMConstants.SEPARATORAPPROPAPS 
                                + ap.getSingleAPStringSDF(false);
                    }
                    propAPClass = propAPClass + stingAPP;
        
                    //Build SDF property DENOPTIMConstants.APTAG
                    String sBO = FragmentSpace.getBondOrderForAPClass(
                            ap.getAPClass().toString()).toOldString();
                    String stBnd = " " + atmID +":"+sBO;
                    if (propAttchPnt.equals(""))
                    {
                        stBnd = stBnd.substring(1);
                    }
                    propAttchPnt = propAttchPnt + stBnd;
                }
                propAPClass = propAPClass + DENOPTIMConstants.SEPARATORAPPROPATMS;
            }

            iac.setProperty(DENOPTIMConstants.APCVTAG,propAPClass);
            iac.setProperty(DENOPTIMConstants.APTAG,propAttchPnt);
            iac.setProperty(DENOPTIMConstants.VERTEXJSONTAG,this.toJson());
            
            iac.removeProperty(DENOPTIMConstants.GRAPHJSONTAG);
            iac.removeProperty(DENOPTIMConstants.GRAPHTAG);
            iac.removeProperty(DENOPTIMConstants.GMSGTAG);
            iac.removeProperty(DENOPTIMConstants.GCODETAG);
            
            // We store the result in a field of this instance
            mol = iac;
            return mol;
        } catch (DENOPTIMException e)
        {
            //TODO: deal with the situation: report error or state given 
            // assumption that allows to go on
            e.printStackTrace();
        }
        return null;
    }

//-----------------------------------------------------------------------------
    
    public DENOPTIMAttachmentPoint getInnerAPFromOuterAP(
            DENOPTIMAttachmentPoint outerAP) {
        
        // TODO: Check if another solution exists that can remove nested
        //  for-loop. Suggestion: make an outerToInnerAPMap.
        
        // Very inefficient solution
        for (Map.Entry<DENOPTIMAttachmentPoint, DENOPTIMAttachmentPoint> entry
                : innerToOuterAPs.entrySet()) 
        {
            if (outerAP == entry.getValue()) 
            {
                return entry.getKey();
            }
        }
        return null;
    }
    
//-----------------------------------------------------------------------------
    
    public DENOPTIMAttachmentPoint getOuterAPFromInnerAP(
            DENOPTIMAttachmentPoint innerAP) 
    {
        return innerToOuterAPs.get(innerAP);
    }
    
//------------------------------------------------------------------------------

    @Override
    public List<MutationType> getMutationTypes()
    {   
        if (getBuildingBlockType() == DENOPTIMVertex.BBType.SCAFFOLD)
        {
            List<MutationType> scaffCompatTypes = new ArrayList<MutationType>();
            if (getNumberOfAPs() != 0)
            {
                scaffCompatTypes.add(MutationType.EXTEND);
                scaffCompatTypes.add(MutationType.CHANGELINK);
                if (!getChilddren().isEmpty())
                {
                    scaffCompatTypes.add(MutationType.ADDLINK);
                }
            }
            
            // NB: to freeze graph structure while allowing mutation of
            // single vertexes, i.e., retain the graph topology, it is 
            // sufficient to remove RCVs from the list of mutation sites, and 
            // do only CHANGELINK. See 'cyclicpeptide' test case.
            
            return scaffCompatTypes;
        }
        return super.getMutationTypes();
    }

//------------------------------------------------------------------------------

    @Override
    public List<DENOPTIMVertex> getMutationSites()
    {
        List<DENOPTIMVertex> lst = new ArrayList<DENOPTIMVertex>();
        // capping groups not considered as mutable sites
        if (getBuildingBlockType() == DENOPTIMVertex.BBType.CAP)
        {
            return lst;
        }

        BBType bbt = getBuildingBlockType();
        
        switch (contractLevel) 
        {
            case FIXED:
                if (getMutationTypes().size() > 0)
                    lst.add(this);
                break;
                
            case FREE:
                for (DENOPTIMVertex v : innerGraph.gVertices) 
                {
                    lst.addAll(v.getMutationSites());
                }
                break;
        }
        return lst;
    }

//------------------------------------------------------------------------------

    /**
     * Adds attachment point (AP) to the list of required APs on this template
     * @param ap attachment point to require from this template
     * @throws DENOPTIMException 
     */
    @Override
    public void addAP(DENOPTIMAttachmentPoint ap) {
        mol = null;
        if (getInnerGraph() != null) {
            throw new IllegalArgumentException("cannot add more required APs " +
                    "after setting the inner graph");
        }
        ap.setOwner(this);
        requiredAPs.add(ap);
    }

//------------------------------------------------------------------------------
    
    /**
     * Compares this and another template ignoring vertex IDs.
     * @param other
     * @param reason string builder used to build the message clarifying the 
     * reason for returning <code>false</code>.
     * @return <code>true</code> if the two templates have the same content 
     * even if the vertex IDs are different.
     */
    public boolean sameAs(DENOPTIMTemplate other, StringBuilder reason)
    {   
        if (this.contractLevel != other.contractLevel)
        {
            reason.append("Different contract level (" 
                    + this.getBuildingBlockId()+":"
                    + other.getBuildingBlockId()+"); ");
            return false;
        }

        if (this.requiredAPs.size() == other.requiredAPs.size()) 
        {
            for (DENOPTIMAttachmentPoint tAP : this.requiredAPs)
            {
                for (DENOPTIMAttachmentPoint oAP : other.requiredAPs)
                {
                    if (!tAP.sameAs(oAP)) 
                    {
                        reason.append("No required AP corresponding to "+tAP);
                        return false;
                    }
                }
            }
        } else {
            reason.append("Different size of required APs(" 
                    + this.requiredAPs.size()+":"
                    + other.requiredAPs.size()+"); ");
            return false;
        }
        
        if (!this.getInnerGraph().sameAs(other.getInnerGraph(),reason))
        {
            return false;
        }
        
        return sameVertexFeatures(other, reason);
    }

//------------------------------------------------------------------------------

    /**
     * Executes the requested mutation type on the inner graph of this
     * template using available fragments from the fragment space.
     * @param type Mutation type
     * @return True if a change occurred.
     * @throws DENOPTIMException 
     */
    
    //TODO-V3: this is used only in test class. It should not be needed.
    
    public boolean mutate(MutationType type) throws DENOPTIMException 
    {
        mol = null;
        
        /*
        Note: We limit ourselves to the following situation
        - The inner graph consists of exactly 1 fragment.
        - The template is not connected to any other vertices.
        - We ignore symmetry.
        - We ignore rings.
        - We disallow the production of nested templates.

        These limitations will be removed once we add more unit tests and
        this note should be updated accordingly as unit tests are added (and
        pass).
         */

        boolean conditionNotSupported = getInnerGraph().getVertexCount() > 1;
        if (conditionNotSupported) {
            throw new UnsupportedOperationException("Mutation type " +
                    type.toString() + " currently unsupported");
        }

        /* Use the DENOPTIMGraphOperations class */

        if (type == MutationType.DELETE
                && getInnerGraph().getVertexCount() <= 1) {
            return false;
        } else if (type == MutationType.CHANGEBRANCH) {
            for (DENOPTIMVertex v : FragmentSpace.getFragmentLibrary()) {
                if (v instanceof DENOPTIMFragment) {
                    DENOPTIMFragment f = (DENOPTIMFragment) v;
                    int matches = countRequiredAPs(f);
                    if (matches == requiredAPs.size()) {
                        DENOPTIMGraph g = new DENOPTIMGraph();
                        g.addVertex(f);
                        this.setInnerGraph(g);
                        return true;
                    }
                }
            }
        } else if (type == MutationType.EXTEND) {
            List<DENOPTIMAttachmentPoint> compAPs = FragmentSpace
                    .getAPsCompatibleWithThese(getAttachmentPoints());
            if (compAPs.size() > 0) {
                Random rand = new Random();
                DENOPTIMAttachmentPoint connectTo = compAPs.get(rand.nextInt(
                        compAPs.size()));

            }
        }
        return false;
    }

//------------------------------------------------------------------------------

    /**
     * Counts the number of required APs that match with the APs on a
     * fragment. Two APs match if they have the same APClass.
     * @param f Fragment to match against
     * @return The number of required APs present on fragment argument.
     */
    private int countRequiredAPs(DENOPTIMFragment f) {
        List<DENOPTIMAttachmentPoint> reqAPs = getRequiredAPs();
        List<DENOPTIMAttachmentPoint> fragAPs = f
                .getAttachmentPoints()
                .stream()
                .map(DENOPTIMAttachmentPoint::clone)
                .collect(Collectors.toList());

        Comparator<DENOPTIMAttachmentPoint> apClassComparator
                = Comparator.comparing(DENOPTIMAttachmentPoint::getAPClass);

        fragAPs.sort(apClassComparator);

        // TODO: 06.04.2021 make sure requiredAPs is always sorted
        reqAPs.sort(apClassComparator);

        int matchesLeft = reqAPs.size();
        for (int i = 0, j = 0; matchesLeft > 0 && i < fragAPs.size(); i++) {
            if (apClassComparator.compare(fragAPs.get(i), reqAPs.get(j)) == 0) {
                matchesLeft--;
                j++;
            }
        }
        return requiredAPs.size() - matchesLeft;
    }

//------------------------------------------------------------------------------

    private List<DENOPTIMAttachmentPoint> getRequiredAPs() {
        return requiredAPs;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Produces a string that represents this vertex and that adheres to the 
     * JSON format.
     * @return the JSON format as a single string
     */
    
    public String toJson()
    {    
        Gson gson = DENOPTIMgson.getWriter();
        String jsonOutput = gson.toJson(this);
        return jsonOutput;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Reads a JSON string and returns an instance of this class.
     * @param json the string to parse.
     * @return a new instance of this class.
     */
    
    public static DENOPTIMTemplate fromJson(String json)
    {
        Gson gson = DENOPTIMgson.getReader();
        
        // This deserializes many "easy" fields, but not the embedded graph
        // which is not "easy" as it need its own deserializer to 
        // recreate all the references to APs/edges/vertexes.
        DENOPTIMTemplate t = gson.fromJson(json, DENOPTIMTemplate.class);
        
        // Now, recover the missing bits (if present) from the original string
        JsonObject jsonObject = (JsonObject) Streams.parse(new JsonReader(
                new StringReader(json)));
       
        if (jsonObject.has("innerGraph"))
        {
            JsonObject innerGraphJson = jsonObject.getAsJsonObject(
                    "innerGraph");
            DENOPTIMGraph innerGraph = DENOPTIMGraph.fromJson(
                    innerGraphJson.toString());
            t.setInnerGraph(innerGraph);

            if (jsonObject.has("innerToOuterAPs"))
            {
                Type type = new TypeToken<TreeMap<Integer,
                        DENOPTIMAttachmentPoint>>(){}.getType();
                TreeMap<Integer,DENOPTIMAttachmentPoint> map =
                        gson.fromJson(jsonObject.getAsJsonObject(
                                "innerToOuterAPs"), type);
                t.updateInnerToOuter(map);
            }
        }
        
        for (DENOPTIMAttachmentPoint ap : t.getAttachmentPoints())
        {
            ap.setOwner(t);
        }
        
        // WARNING: other fields, such as 'owner' and AP 'user' are
        // recovered upon deserializing the graph containing this 
        // vertex, if any
        
        return t;
    }
    
//------------------------------------------------------------------------------
    
}

