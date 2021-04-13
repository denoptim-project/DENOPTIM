package denoptim.molecule;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import denoptim.utils.MutationType;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.io.DenoptimIO;
import denoptim.molecule.DENOPTIMEdge.BondType;
import denoptim.rings.PathSubGraph;
import denoptim.threedim.ThreeDimTreeBuilder;
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
     * Graph that is embedded in this vertex. This field differentiates a
     * template from any other subclass of {@link DENOPTIMVertex}, and its name
     * is used in {@link DENOPTIMVertexDeserializer} to deserialize JSON string.
     */
    private DENOPTIMGraph innerGraph;

    /**
     * Denotes the constants in the template. It can take values from 0 to 2 and each level promises to uphold the
     * contracts of previous levels. 0 means exterior attachment points are constant, 1 means the subgraph structure is
     * constant, and 2 means the vertices are constant.
     */
    //TODO-V3: make enum
    private int contractLevel = 0;

    private List<DENOPTIMAttachmentPoint> requiredAPs = new ArrayList<>();

    private APMap innerToOuterAPs;

//------------------------------------------------------------------------------

    public DENOPTIMTemplate(DENOPTIMVertex.BBType bbType)
    {
        super();
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
     * Produces a subgraph that represents the path between two given
     * attachment points.
     * @param apA
     * @param apB
     * @return a path that goes from apA to apB.
     */

    public PathSubGraph getPath(DENOPTIMAttachmentPoint apA,
            DENOPTIMAttachmentPoint apB)
    {
        //TODO-V3: just use the PathSubGraph constructor. 
        // But is this needed in template?
        return null;
    }
    
//------------------------------------------------------------------------------

    /**
     * Method meant for devel phase only.
     */
    //TODO-V3 Remove.
    
    public static DENOPTIMTemplate getTestTemplate(int contractLevel) 
    {
        DENOPTIMTemplate template = new DENOPTIMTemplate(DENOPTIMVertex.BBType.UNDEFINED);
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
     * Returns a deep copy of this template
     * @return the deep copy
     */
    public DENOPTIMTemplate clone()
    {
        DENOPTIMTemplate c = new DENOPTIMTemplate(this.getBuildingBlockType());
        
        c.setVertexId(this.getVertexId());
        c.setBuildingBlockId(this.getBuildingBlockId());
        c.contractLevel = this.contractLevel;

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

    public void setInnerGraph(DENOPTIMGraph innerGraph) throws IllegalArgumentException {
        // if (!compatibleWithOldInnerGraph(innerGraph))
        if (!isValidInnerGraph(innerGraph)) {
            throw new IllegalArgumentException("inner graph does not have all" +
                    " required APs");
        }
        this.innerGraph = innerGraph;
        this.innerToOuterAPs = new APMap();
        for (DENOPTIMAttachmentPoint innerAP : innerGraph.getAvailableAPs()) {
            DENOPTIMAttachmentPoint outerAP = innerAP.clone();
            outerAP.setOwner(this);
            innerToOuterAPs.put(innerAP, outerAP);
        }
    }

//-----------------------------------------------------------------------------
    
    //TODO-V3 possibly relocate this and make private
    public void updateInnerToOuter(TreeMap<Integer,DENOPTIMAttachmentPoint> map)
    {
        this.innerToOuterAPs = new APMap();
        for (Entry<Integer, DENOPTIMAttachmentPoint> e : map.entrySet())
        {
            DENOPTIMAttachmentPoint innerAP = innerGraph.getAPWithId(e.getKey());
            DENOPTIMAttachmentPoint outerAP = e.getValue();
            outerAP.setOwner(this);
            this.innerToOuterAPs.put(innerAP, outerAP);
        }
    }

//-----------------------------------------------------------------------------
    
    private boolean isValidInnerGraph(DENOPTIMGraph g) {
        List<DENOPTIMAttachmentPoint> innerAPs = g.getAvailableAPs();
        if (innerAPs.size() < getRequiredAPs().size()) {
            return false;
        }

        /* TODO-V3: is sorting needed?
        * Answer from Einar: Let n be the number of attachment points on the
        * graph. If we don't sort then this takes O(n²). If we sort then
        * O(nlog(n)) + O(n) = O(nlog(n)).
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

    //TODO-V3: add documentation. In particular define whether we return inner of outer APs
    
    @Override
    public ArrayList<DENOPTIMAttachmentPoint> getAttachmentPoints()
    {
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
    
    @Override
    public IAtomContainer getIAtomContainer()
    {
        // TODO: Check if another solution exists that can remove nested
        //  for-loop. Suggestion: make an outerToInnerAPMap.
        try
        {
            //TODO-V3 we might need to remove unused RCVs from inner graph.
            // Such RCVs cannot be used outside the template.
            ThreeDimTreeBuilder t3b = new ThreeDimTreeBuilder();
            IAtomContainer iac = t3b.convertGraphTo3DAtomContainer(
                    innerGraph, true);
            
            // We have to ensure outer APs has same atom position as inner APs
            // And we collects the outer APs per atom at the same time
            Map<Integer,List<DENOPTIMAttachmentPoint>> apsPerAtom = new TreeMap<>();
            for (DENOPTIMAttachmentPoint outerAP : getAttachmentPoints()) {
                // There probably exists a better solution to this
                DENOPTIMAttachmentPoint innerAP =
                        getInnerAPFromOuterAP(outerAP);
                int innerAtmId = innerAP.getAtomPositionNumber();
                int innerVrtxId = innerAP.getOwner().getVertexId();
                for (int i = 0; i < iac.getAtomCount(); i++) {
                    IAtom atom = iac.getAtom(i);
                    int originalVrtxId =
                            atom.getProperty(DENOPTIMConstants.ATMPROPVERTEXID);
                    int innerSrcAtmId =
                            atom.getProperty(DENOPTIMConstants.ATMPROPORIGINALATMID);
                    if (innerAtmId == innerSrcAtmId && innerVrtxId == originalVrtxId) {
                        outerAP.setAtomPositionNumber(i);
                        if (apsPerAtom.containsKey(i))
                        {
                            apsPerAtom.get(i).add(outerAP);
                        } else {
                            List<DENOPTIMAttachmentPoint> list = 
                                    new ArrayList<DENOPTIMAttachmentPoint>();
                            list.add(outerAP);
                            apsPerAtom.put(i, list);
                        }
                    }
                }
            }

            for (int i = 0; i < iac.getAtomCount(); i++) {
                IAtom a = iac.getAtom(i);
                a.setProperty(DENOPTIMConstants.ATMPROPORIGINALATMID, i);
                a.setProperty(DENOPTIMConstants.ATMPROPVERTEXID,
                        getVertexId());
            }

            /*
            int vid = (int) atom.getProperty(
                    DENOPTIMConstants.ATMPROPVERTEXID);
            int iatm = (int) atom.getProperty(
                    DENOPTIMConstants.ATMPROPORIGINALATMID);
            if (atmSrcMap.containsKey(vid))
            {
                atmSrcMap.get(vid).put(iatm, l);
            } else {
                TreeMap<Integer,Integer> atmPositionInVrtxAndInMol =
                        new TreeMap<Integer,Integer>();
                atmPositionInVrtxAndInMol.put(iatm, l);
                atmSrcMap.put(vid, atmPositionInVrtxAndInMol);
            }
            l++;
            */
            
            iac.setProperty(DENOPTIMConstants.GRAPHJSONTAG,innerGraph.toJson());
            
            // This is largerly as done in  DENOPTIMFragment.projectAPsToProperties
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
        
                    //Build SDF property "CLASS"
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
        
                    //Build SDF property "ATTACHMENT_POINT"
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
            
            return iac;
        } catch (DENOPTIMException e)
        {
            //TODO: deal with the situation: report error or state given assumption that allows to go on
            e.printStackTrace();
        }
        return null;
    }

//-----------------------------------------------------------------------------
    
    public DENOPTIMAttachmentPoint getInnerAPFromOuterAP(
            DENOPTIMAttachmentPoint outerAP) {
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

//------------------------------------------------------------------------------

    @Override
    public Set<DENOPTIMVertex> getMutationSites()
    {
        Set<DENOPTIMVertex> set = new HashSet<DENOPTIMVertex>();
        
        // I doubt templates will ever be used as capping groups, but
        // just in case, then they are not considered mutable sites
        if (getBuildingBlockType() == DENOPTIMVertex.BBType.CAP
                || getBuildingBlockType() == DENOPTIMVertex.BBType.SCAFFOLD)
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
                for (DENOPTIMVertex v : innerGraph.gVertices)
                {
                    set.addAll(v.getMutationSites());
                }
                break;
            }
        }
        return set;
    }

//------------------------------------------------------------------------------

    /**
     * Adds ap to the list of required APs on this template
     * @param ap attachment point to require from this template
     * @throws DENOPTIMException 
     */
    @Override
    public void addAP(DENOPTIMAttachmentPoint ap) {
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
                    if (tAP.comparePropertiesTo(oAP) != 0) 
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
     */
    public boolean mutate(MutationType type) {
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
     * Enum specifying the whether a template's inner graph is completely
     * fixed (FIXED), fixed structure (FIXED_STRUCT), or is free (FREE).
     *
     * FIXED inner graphs are effectively equivalent to the DENOPTIMFragment
     * class.
     * FIXED_STRUCT inner graphs will keep a constant inter-connectivity
     * between vertices, but the content at each vertex may vary.
     * FIXED_STRUCT does not guarantee that the AP classes connecting inner
     * graph vertices will remain the same as the content at vertices change.
     * FREE inner graphs are free to change however they want both in content
     * and inter-connectivity, within the confines of the required APs.
     */
    public enum ContractLevel {
        FREE,
        FIXED_STRUCT,
        FIXED
    }
    
//------------------------------------------------------------------------------

}

