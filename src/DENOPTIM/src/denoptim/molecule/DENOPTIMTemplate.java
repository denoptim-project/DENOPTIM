package denoptim.molecule;

import java.util.*;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.molecule.DENOPTIMEdge.BondType;
import denoptim.molecule.DENOPTIMFragment.BBType;
import denoptim.utils.GraphConversionTool;
import denoptim.utils.GraphUtils;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtom;

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
        DENOPTIMVertex vrtx2 = new EmptyVertex(1);
        try {
            vrtx.addAP(0, 1, 1);
            vrtx.addAP(1, 1, 1);

            vrtx2.addAP(0, 1, 1);
            vrtx2.addAP(1, 1, 1);
        } catch (DENOPTIMException e) {
            e.printStackTrace();
        }
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

        DENOPTIMGraph g = new DENOPTIMGraph();
        g.addVertex(vA);
        g.addVertex(vB);
        DENOPTIMEdge eAB = vA.connectVertices(vB, 0, 1);
        g.addEdge(eAB);
        template.setInnerGraph(g);
        
        /*
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

        List<DENOPTIMVertex> frags = Arrays.asList(vA, vB, vC, vD, vRCV1,
                vRCV2);
        
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

        List<DENOPTIMVertex> caps  = Arrays.asList(vH1, vH2, vH3, vH4, vH5,
                vH6);

        List<DENOPTIMVertex> vertices = new ArrayList<>(frags);
        vertices.addAll(caps);
        DENOPTIMGraph g = new DENOPTIMGraph();
        for (DENOPTIMVertex v : vertices) {
            g.addVertex(v);
        }
        
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

        List<DENOPTIMEdge> edges = Arrays.asList(eARcv, eAB, eBC, eCD, eDRcv,
                eAH1, eBH2, eCH3, eDH4, eAH5, eCH6);
        for (DENOPTIMEdge e : edges) {
            g.addEdge(e);
        }
        
        DENOPTIMRing r = new DENOPTIMRing(new ArrayList<DENOPTIMVertex>(
                Arrays.asList(vRCV1, vA, vB, vC, vD, vRCV2)));
        g.addRing(r);

        //TODO-M7 del
        //System.out.println("BEFORE___ TEMPLATE's inner graph (C): "+g);
        
        /*
        System.out.println("(F) TEMPLATE's inner graph: "+template.interiorGraph);
        System.out.println("(F) TEMPLATE's #HeavyAtoms: "+template.getHeavyAtomsCount());
        System.out.println("(F) TEMPLATE's #APs: "+template.getAttachmentPoints().size());
        //for (DENOPTIMAttachmentPoint ap : template.getAttachmentPoints())
        //    System.out.println("  "+ap+" free="+ap.isAvailable());
        */
        
        //TODO-M7 del
        g.renumberGraphVertices();
        template.setInnerGraph(g);
        
      //TODO-M7 del
        /*
        System.out.println("AFTER___ TEMPLATE's inner graph (C): "+template.getInnerGraph());
        for (DENOPTIMAttachmentPoint ap : template.getAttachmentPoints()) {
            System.out.println(ap + "apid=" + ap.getID());
        }
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
        return innerGraph.getAvailableAPs();
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
    
    //TODO-MF possibly relocate this and make private
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
        ArrayList<DENOPTIMAttachmentPoint> outerAPs = g.getAvailableAPs();
        if (outerAPs.size() < requiredAPs.size()) {
            return false;
        }
        /* Compares total connections, free connections, direction vector
        coordinates, and APClass, in that order.
         */
        Comparator<DENOPTIMAttachmentPoint> c = (ap1, ap2) -> {
            int diffTotConn =
                    ap1.getTotalConnections() - ap2.getTotalConnections();
            if (diffTotConn != 0) {
                return diffTotConn;
            }
            int diffFreeConn =
                    ap1.getFreeConnections() - ap2.getFreeConnections();
            if (diffFreeConn != 0) {
                return diffFreeConn;
            }
            double[] dirVec1 = ap1.getDirectionVector();
            double[] dirVec2 = ap2.getDirectionVector();
            for (int i = 0; i < dirVec1.length; i++) {
                double diffDirVec = dirVec1[i] - dirVec2[i];
                if (diffDirVec < 0)  {
                    return -1;
                } else if (diffDirVec > 0) {
                    return 1;
                }
            }
            return ap1.getAPClass().compareTo(ap2.getAPClass());
        };

        outerAPs.sort(c);
        requiredAPs.sort(c);
        int matchesLeft = requiredAPs.size();
        for (int i = 0; matchesLeft != 0 && outerAPs.size() - i >= matchesLeft && i < outerAPs.size(); i++) {
            if (c.compare(outerAPs.get(i), requiredAPs.get(i)) == 0) {
                matchesLeft--;
            }
        }
        return matchesLeft == 0;
    }

//-----------------------------------------------------------------------------

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
        return innerGraph.getHeavyAtomsCount();
    }

//-----------------------------------------------------------------------------
    
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
            IAtomContainer iac = GraphConversionTool
                    .convertGraphToMolecule(innerGraph, true);

            // We have to ensure outer APs has same atom position as inner APs
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
                        // TODO: Remove
                        // System.out.println("Fixed wrong atomposnumbering");
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

            return iac;
        } catch (DENOPTIMException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private DENOPTIMAttachmentPoint getInnerAPFromOuterAP(DENOPTIMAttachmentPoint outerAP) {
        // Very inefficient solution
        for (Map.Entry<DENOPTIMAttachmentPoint, DENOPTIMAttachmentPoint> entry
                : innerToOuterAPs.entrySet()) {
            if (outerAP.equals(entry.getValue())) {
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
     */
    @Override
    public void addAP(DENOPTIMAttachmentPoint ap) throws DENOPTIMException {
        if (getInnerGraph() != null) {
            throw new DENOPTIMException("cannot add more required APs after " +
                    "setting the inner graph");
        }
        ap.setOwner(this);
        requiredAPs.add(ap);
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

