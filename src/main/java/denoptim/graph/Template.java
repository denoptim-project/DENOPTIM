/*
 *   DENOPTIM
 *   Copyright (C) 2022 Marco Foscato <marco.foscato@uib.no>
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package denoptim.graph;

import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.TreeMap;

import javax.vecmath.Point3d;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.graph.Edge.BondType;
import denoptim.json.DENOPTIMgson;
import denoptim.molecularmodeling.ThreeDimTreeBuilder;
import denoptim.utils.MoleculeUtils;
import denoptim.utils.MutationType;
import denoptim.utils.Randomizer;

/**
 * <p>A template is a {@link Vertex} that contains a {@link DGraph}. The
 * content of the template, i.e., the embedded {@link DGraph}, 
 * is subject to constraints that are defined by
 * the {@link ContractLevel} of the template and may or may not allow changes
 * on the embedded {@link DGraph}.</p>
 * <p>The embedded {@link DGraph} projects a reflection of
 * any available {@link AttachmentPoint} to the surface of the template. The 
 * inner {@link AttachmentPoint}s, i.e., those owned by the embedded 
 * {@link DGraph}, are not the same instances as the outer ones, i.e., those 
 * on the surface of the template. However, this class keeps a one-to-one 
 * mapping that is made available by the {@link #innerToOuterAPs} method. 
 * The distinction of inner and outer {@link AttachmentPoint}s determines the
 * existence of a barrier between the inside and the outside of a template.
 * Methods mark with the word "Throughout" are used for operations that
 * go across such barrier. </p>
 * <p>For example, consider an {@link AttachmentPoint} in the 
 * embedded {@link DGraph} (i.e., the inner {@link AttachmentPoint}.
 * The {@link AttachmentPoint#isAvailable()} method
 * returns <code>true</code> even if its extra-template reflection (i.e., the 
 * outer {@link AttachmentPoint}) is used to make an edge to the template.
 * Instead, the {@link AttachmentPoint#isAvailableThroughout()} returns 
 * <code>false</code> because it is capable of crossing the template barrier.
 * </p>
 */

public class Template extends Vertex
{
    /**
     * Graph that is embedded in this vertex.
     */
    private DGraph innerGraph;
    
    /**
     * Molecular representation of the content of this template. This filed is
     * meant to speed up handling of template's molecular analog. 
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
     */
    public enum ContractLevel {
        /**
         * Inner graphs are free to change within 
         * the confines of the required APs
         */
        FREE,
        
        /**
         * Inner graphs are effectively equivalent 
         * to the DENOPTIMFragment class, as no change in the inner structure is
         * allowed.
         */
        FIXED,
        
        /**
         * Inner graph keep the same structure, but the identify of vertexes
         * can change. 
         * Effectively this contract allows only CHANGELINK mutation.
         */
        FIXED_STRUCT
    }

    private List<AttachmentPoint> requiredAPs = new ArrayList<>();

    private APTreeMap innerToOuterAPs;

    
//------------------------------------------------------------------------------

    public Template(Vertex.BBType bbType)
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
    public String[] getPathIDs(AttachmentPoint apA,
            AttachmentPoint apB)
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
    
    public static Template getTestTemplate(ContractLevel contractLevel) 
            throws DENOPTIMException 
    {
        Template template = new Template(
                Vertex.BBType.UNDEFINED);
        EmptyVertex vrtx = new EmptyVertex(0);
        EmptyVertex vrtx2 = new EmptyVertex(1);
    
        vrtx.addAP();
        vrtx.addAP();

        vrtx2.addAP();
        vrtx2.addAP();
        DGraph g = new DGraph();
        g.addVertex(vrtx);
        g.addVertex(vrtx2);
        g.addEdge(new Edge(vrtx.getAP(0),
            vrtx2.getAP(1), BondType.SINGLE));
        template.setInnerGraph(g);

        template.contractLevel = contractLevel;
        return template;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the contract level of this template, i.e., to what extent the 
     * content of this template can be changed.
     * @return the contract level of this template.
     */
    public ContractLevel getContractLevel()
    {
        return contractLevel;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Imposes the given contract to this template.
     * @param contract the contract to impose on this template.
     */
    public void setContractLevel(ContractLevel contract)
    {
        if (contract == ContractLevel.FIXED_STRUCT)
            updateMutTypeToFixedSTructure();
        
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
    public Template clone()
    {
        Template c = new Template(this.getBuildingBlockType());
        
        c.setVertexId(this.getVertexId());
        c.setBuildingBlockId(this.getBuildingBlockId());
        c.contractLevel = this.contractLevel;
        c.setMutationTypes(this.getUnfilteredMutationTypes());

        for (AttachmentPoint ap : this.requiredAPs)
        {
            c.addRequiredAP(ap.getDirectionVector(), ap.getAPClass());
        }
        c.setInnerGraph(this.getInnerGraph().clone());
        if (this.mol!=null)
        {
            //NB: We cannot use setIAtomContainer because it implies using 
            // referenced to graph's AP which have been cloned, so the references
            // are broken.
            try
            {
                c.mol = MoleculeUtils.makeSameAs(mol);
                for (int i=0; i<this.getAttachmentPoints().size(); i++)
                {
                    AttachmentPoint thisOutAP = 
                            this.getAttachmentPoints().get(i);
                    AttachmentPoint cloneOutAP = 
                            c.getAttachmentPoints().get(i);
                    cloneOutAP.setDirectionVector(new Point3d(
                            thisOutAP.getDirectionVector()));
                    cloneOutAP.setAtomPositionNumber(
                            thisOutAP.getAtomPositionNumber());
                }
                
            } catch (DENOPTIMException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        c.setProperties(this.copyStringBasedProperties());
        if (uniquefyingPropertyKeys!=null)
            c.uniquefyingPropertyKeys.addAll(uniquefyingPropertyKeys);
        return c;
    }
    
//-----------------------------------------------------------------------------
    
    public DGraph getInnerGraph()
    {
        return innerGraph;
    }

//-----------------------------------------------------------------------------

    public void setInnerGraph(DGraph innerGraph) 
            throws IllegalArgumentException 
    {
        // NB: if you change anything here, remember that we can modify the
        // inner graph, so the change you are implementing here might need to be
        // reflected in any place where the inner graph is modified (e.g.,
        // DENOPTIMGraph.insertSingleVertex()
        
        clearIAtomContainer();
        if (!isValidInnerGraph(innerGraph)) {
            throw new IllegalArgumentException("inner graph does not have all" +
                    " required APs");
        }
        this.innerGraph = innerGraph;
        innerGraph.setTemplateJacket(this);
        this.innerToOuterAPs = new APTreeMap();

        //TODO: we might need to remove unused RCVs from inner graph.
        // Such RCVs cannot be used outside the template.
        
        for (AttachmentPoint innerAP : innerGraph.getAvailableAPs()) {
            addInnerToOuterAPMapping(innerAP);
        }
    }

//-----------------------------------------------------------------------------
    
    private void updateInnerToOuter(TreeMap<Integer,AttachmentPoint> map)
    {
        clearIAtomContainer();
        this.innerToOuterAPs = new APTreeMap();
        for (Entry<Integer, AttachmentPoint> e : map.entrySet())
        {
            AttachmentPoint innerAP = innerGraph.getAPWithId(e.getKey());
            AttachmentPoint outerAP = e.getValue();
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
    public void addInnerToOuterAPMapping(AttachmentPoint newInnerAP)
    {
        if (innerToOuterAPs.containsKey(newInnerAP))
        {
            return;
        }
        AttachmentPoint outerAP = newInnerAP.clone();
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
     * (i.e., innerAPs). This method does change the attributes of the outerAP 
     * to reflect the change on innerAP, i.e., the {@link APClass} of innerAP 
     * is assigned to the outerAP.
     * If there is now mapping for the oldInnerAP,
     * then nothing happens.
     * @param oldInnerAP the inner AP to be changed
     * @param newInnerAP the inner AP to change the old one with.
     */
    public void updateInnerApID(AttachmentPoint oldInnerAP, 
            AttachmentPoint newInnerAP)
    {   
        if (!innerToOuterAPs.containsKey(oldInnerAP))
        {
            return;
        }
        AttachmentPoint outerAP = innerToOuterAPs.get(oldInnerAP);
        outerAP.setAPClass(newInnerAP.getAPClass());
        
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
    public void removeProjectionOfInnerAP(AttachmentPoint oldInnerAP) 
            throws DENOPTIMException
    {
        if (!innerToOuterAPs.containsKey(oldInnerAP))
        {
            return;
        }
        AttachmentPoint outer = innerToOuterAPs.get(oldInnerAP);
        if (!outer.isAvailable())
        {
            AttachmentPoint linkedAP = outer.getLinkedAP();
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
    
    private boolean isValidInnerGraph(DGraph g) 
    {
        if (requiredAPs.size()==0)
            return true;
        
        List<AttachmentPoint> innerAPs = g.getAvailableAPs();
        if (innerAPs.size() < getRequiredAPs().size()) {
            return false;
        }
        Comparator<AttachmentPoint> apClassComparator
                = Comparator.comparing(AttachmentPoint::getAPClass,
                        Comparator.nullsLast(Comparator.naturalOrder()));
        innerAPs.sort(apClassComparator);
        List<AttachmentPoint> reqAPs = getRequiredAPs();
        reqAPs.sort(apClassComparator);
        int matchesLeft = reqAPs.size();
        for (int i = 0, j = 0; matchesLeft > 0 && i < innerAPs.size(); i++) 
        {
            if (apClassComparator.compare(innerAPs.get(i), reqAPs.get(j)) == 0) 
            {
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
    public ArrayList<AttachmentPoint> getAttachmentPoints()
    {
        if (innerToOuterAPs == null)
            return new ArrayList<>();
        else
            return new ArrayList<>(innerToOuterAPs.values());
    }

//-----------------------------------------------------------------------------
    
    /**
     * This method exists by contract, but does not do anything because the
     * concept of setting the symmetric set of APs in a template is not
     * defined yet.
     */
    @Override
    protected void setSymmetricAPSets(ArrayList<SymmetricSet> sAPs)
    {
        // Do nothing... for now.   
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * The {@link SymmetricSet} produced from this method contain indexes of the
     * {@link AttachmentPoint}s in the list returned by 
     * {@link #getAttachmentPoints()}.
     */
    @Override
    public ArrayList<SymmetricSet> getSymmetricAPSets()
    {
        ArrayList<SymmetricSet> allSymSets = new ArrayList<SymmetricSet>();
        
        List<AttachmentPoint> doneAPs = new ArrayList<AttachmentPoint>();
        for (AttachmentPoint innerAP : innerToOuterAPs.keySet())
        {   
            if (doneAPs.contains(innerAP))
                continue;
            
            SymmetricSet symSetForThisAP = new SymmetricSet();
            
            Vertex vrtx = innerAP.getOwner();
            int innerAPIdx = innerAP.getIndexInOwner();
            SymmetricSet sAPsOnVrtx = vrtx.getSymmetricAPs(innerAPIdx);
            if (sAPsOnVrtx!=null)
            {
                for (int apIdx : sAPsOnVrtx.getList())
                {
                    AttachmentPoint symInnerAP = vrtx.getAP(apIdx);
                    if (doneAPs.contains(symInnerAP))
                        continue;
                    if (innerToOuterAPs.containsKey(symInnerAP))
                    {
                        symSetForThisAP.add(getIndedOfInnerAP(symInnerAP));
                        doneAPs.add(symInnerAP);
                    }
                }
            }
            
            List<Vertex> symVrtxs = innerGraph.getSymVertexesForVertex(vrtx);
            for (Vertex symVrtx : symVrtxs)
            {
                //NB: we assume that this is the same vertex type as vrtx, but 
                // a different instance. Thus the list of APs should be a match
                // and we reuse the same index 'innerAPIdx'.
                AttachmentPoint innerApOnSymVrtx = symVrtx.getAP(innerAPIdx);
                if (doneAPs.contains(innerApOnSymVrtx))
                    continue;
                
                SymmetricSet sAPsOnSymVrtx = symVrtx.getSymmetricAPs(innerAPIdx);
                if (sAPsOnSymVrtx!=null)
                {
                    for (int apIdxOnSymVrtx : sAPsOnSymVrtx.getList())
                    {
                        AttachmentPoint symInnerAPOnSymVrtx = symVrtx.getAP(
                                apIdxOnSymVrtx);
                        if (doneAPs.contains(symInnerAPOnSymVrtx))
                            continue;
                        if (innerToOuterAPs.containsKey(symInnerAPOnSymVrtx))
                        {
                            symSetForThisAP.add(getIndedOfInnerAP
                                    (symInnerAPOnSymVrtx));
                            doneAPs.add(symInnerAPOnSymVrtx);
                        }
                    }
                } else {
                    // We need to add the AP at innerAPIdx anyway because even
                    // it it does not have symmetric APs on its vertex owner it
                    // is symmetric to the vrtx by means of the two vertexes 
                    // being members of the same symmetric set of vertexes.
                    if (innerToOuterAPs.containsKey(innerApOnSymVrtx))
                    {
                        symSetForThisAP.add(getIndedOfInnerAP(innerApOnSymVrtx));
                        doneAPs.add(innerApOnSymVrtx);
                    }
                    if (!doneAPs.contains(innerAP))
                    {
                        symSetForThisAP.add(getIndedOfInnerAP(innerAP));
                        doneAPs.add(innerAP);
                    }
                }
            }
            if (symSetForThisAP.size()>1)
                allSymSets.add(symSetForThisAP);
        }
        return allSymSets;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Returns the index of the given AP in the sorted iterations over the
     * keys of the mapping between inner and outer APs.
     * The map is sorted, so the index should not change unless there are 
     * changes in the list of APs.
     */
    //TODO-gg typo
    private int getIndedOfInnerAP(AttachmentPoint ap)
    {
        int innerApIdx = -1;
        for (AttachmentPoint innerAP : innerToOuterAPs.keySet())
        {
            innerApIdx++;
            if (innerAP==ap)
            {
                return innerApIdx;
            }
            
        }
        return -1;
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
        //NB: the mol!=null allows templates read-in from file to be displayed
        // even if their graph's vertexes are empty because they were 
        // deserialized from json. Another good reason for having atoms defined
        // the json format...
        return mol!=null 
                || innerGraph != null ? innerGraph.containsAtoms() : false;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Removes the molecular representation
     */
    public void clearIAtomContainer()
    {
        this.mol = null;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Attaches a molecular representation to this template. Note that changes 
     * to this Template have the power to trigger the request to update the
     * molecular representation that we set here, thus removing the 
     * representation we define in this method.
     * @param mol the atom container to be used as molecular representation of 
     * this template.
     * @param updateAPsAccordingToIAC flag that prevents actions when the IAC 
     * does not have the details needed to update the APs on the template. 
     * For instance, when cloning a template or reading in a template with IAC
     * from file. In these cases we only want to add a molecular representation
     * and we set this flag to <code>false</code>.
     * @throws DENOPTIMException 
     */
    public void setIAtomContainer(IAtomContainer mol, 
            boolean updateAPsAccordingToIAC) throws DENOPTIMException
    { 
        //Collects all the links to APs
        Map<AttachmentPoint,AttachmentPoint> 
            apInnerGraphToApOnMol = new HashMap<>();
        if (updateAPsAccordingToIAC)
        {
            for (IAtom a : mol.atoms())
            {
                Object p = a.getProperty(DENOPTIMConstants.ATMPROPAPS);
                if (p == null)
                {
                    continue;
                }
                @SuppressWarnings("unchecked")
                ArrayList<AttachmentPoint> apLst = 
                    (ArrayList<AttachmentPoint>) p;
                for (AttachmentPoint apOnMol : apLst)
                {
                    Object o = apOnMol.getProperty(DENOPTIMConstants.LINKAPS);
                    if (o==null)
                    {
                        throw new DENOPTIMException("Unexpected null link to AP.");
                    }
                    AttachmentPoint linkedAPOnGraph = 
                            (AttachmentPoint) o;
                    apInnerGraphToApOnMol.put(linkedAPOnGraph, apOnMol);
                }
            }
        }
            
        // We have to ensure outer APs point to the correct source atom in
        // the atom list of the entire molecular representation of the 
        // templates.
        // And we collects the outer APs per atom at the same time
        LinkedHashMap<Integer,List<AttachmentPoint>> apsPerAtom = 
                new LinkedHashMap<>();
        for (AttachmentPoint outAP : getAttachmentPoints()) 
        {
            AttachmentPoint inAPOnGraph = getInnerAPFromOuterAP(outAP);
            int atmIndexInMol = outAP.getAtomPositionNumber();
            if (updateAPsAccordingToIAC 
                    && apInnerGraphToApOnMol.containsKey(inAPOnGraph))
            {
                AttachmentPoint apOnMol = apInnerGraphToApOnMol.get(inAPOnGraph);
                outAP.setDirectionVector(apOnMol.getDirectionVector());
                atmIndexInMol = apOnMol.getAtomPositionNumber(); //yes, not InMol!
            }
            outAP.setAtomPositionNumber(atmIndexInMol);
            if (apsPerAtom.containsKey(atmIndexInMol))
            {
                apsPerAtom.get(atmIndexInMol).add(outAP);
            } else {
                List<AttachmentPoint> list = 
                        new ArrayList<AttachmentPoint>();
                list.add(outAP);
                apsPerAtom.put(atmIndexInMol, list);
            }
        }

        for (int i = 0; i < mol.getAtomCount(); i++) {
            IAtom a = mol.getAtom(i);
            a.setProperty(DENOPTIMConstants.ATMPROPORIGINALATMID, i);
            a.setProperty(DENOPTIMConstants.ATMPROPVERTEXID,
                    getVertexId());
        }
        
        // Prepare SDF-like string for atom container. 0-based to 1-based
        // index conversion done in here
        mol.setProperty(DENOPTIMConstants.APSTAG, 
                AttachmentPoint.getAPDefinitionsForSDF(apsPerAtom));
        
        mol.setProperty(DENOPTIMConstants.VERTEXJSONTAG,this.toJson());
        
        mol.removeProperty(DENOPTIMConstants.GRAPHJSONTAG);
        mol.removeProperty(DENOPTIMConstants.GRAPHTAG);
        mol.removeProperty(DENOPTIMConstants.GMSGTAG);
        mol.removeProperty(DENOPTIMConstants.GCODETAG);
        
        this.mol = mol;
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
        Logger logger = Logger.getLogger("DummyLogger");
        Randomizer rng = new Randomizer();
        boolean removeUsedRCAs = true;
        return getIAtomContainer(logger, rng, removeUsedRCAs, false);
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * The molecular representation, if any, is generated by this method and 
     * stored until further changes in the content of this template.
     * Successive calls of this method (i.e., prior to any other modification in
     * this template's content) return the result stored in the first run 
     * occurred after the last edit of this template's content.
     * @param logger tool dealing with log messages
     * @param rng random number generator and decision tool.
     * @param removeUsedRCAs use <code>true</code> to remove the ring closing 
     * attractors and replace them with a ring-closing bond according to the 
     * bond type defined by the ring.
     * @param rebuild use <code>true</code> to ignore any previously stored 
     * chemical representation of this vertex and re-build from scratch.
     * @return the molecular representation of the content of this template.
     */
    @Override
    public IAtomContainer getIAtomContainer(Logger logger, 
            Randomizer rng, boolean removeUsedRCAs, boolean rebuild)
    {
        if (mol!=null && !rebuild)
        {
            return mol;
        }
        try
        {
            ThreeDimTreeBuilder t3b = new ThreeDimTreeBuilder(logger ,rng);
            IAtomContainer iac = t3b.convertGraphTo3DAtomContainer(
                    innerGraph, removeUsedRCAs);
            
            // We have to ensure outer APs point to the correct source atom in
            // the atom list of the entire molecular representation of the 
            // templates.
            // And we collects the outer APs per atom at the same time
            LinkedHashMap<Integer,List<AttachmentPoint>> apsPerAtom = 
                    new LinkedHashMap<>();
            for (AttachmentPoint outAP : getAttachmentPoints()) 
            {
                AttachmentPoint inAP = getInnerAPFromOuterAP(outAP);
                if (inAP.getDirectionVector()==null)
                {
                    outAP.setDirectionVector(null);
                } else {
                    outAP.setDirectionVector(inAP.getDirectionVector());
                }
                int atmIndexInMol = inAP.getAtomPositionNumberInMol();
                outAP.setAtomPositionNumber(atmIndexInMol);
                if (apsPerAtom.containsKey(atmIndexInMol))
                {
                    apsPerAtom.get(atmIndexInMol).add(outAP);
                } else {
                    List<AttachmentPoint> list = 
                            new ArrayList<AttachmentPoint>();
                    list.add(outAP);
                    apsPerAtom.put(atmIndexInMol, list);
                }
            }
            
            // Prepare SDF-like string for atom container. 0-based to 1-based
            // index conversion done in here
            iac.setProperty(DENOPTIMConstants.APSTAG, 
                    AttachmentPoint.getAPDefinitionsForSDF(apsPerAtom));
            
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
    
    public AttachmentPoint getInnerAPFromOuterAP(
            AttachmentPoint outerAP) {
        
        // TODO: Check if another solution exists that can remove nested
        //  for-loop. Suggestion: make an outerToInnerAPMap.
        
        // Very inefficient solution
        for (Map.Entry<AttachmentPoint, AttachmentPoint> entry
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
    
    public AttachmentPoint getOuterAPFromInnerAP(
            AttachmentPoint innerAP) 
    {
        return innerToOuterAPs.get(innerAP);
    }
    
//------------------------------------------------------------------------------
    
    @Override
    public List<MutationType> getMutationTypes(List<MutationType> ignoredTypes)
    {   
        if (getBuildingBlockType() == Vertex.BBType.SCAFFOLD)
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
            
            scaffCompatTypes.removeAll(ignoredTypes);
            return scaffCompatTypes;
        }
        return super.getMutationTypes(ignoredTypes);
    }

//------------------------------------------------------------------------------

    /**
     * A list of mutation sites from within this vertex. This method sets the 
     * mutation sites of the embedded vertexes according to the 
     * {@link Template#contractLevel} of
     * this template.
     * @param ignoredTypes a collection of mutation types to ignore. Vertexes
     * that allow only ignored types of mutation will
     * not be considered mutation sites.
     * @return the list of vertexes that allow any non-ignored mutation type.
     */
    
    @Override
    public List<Vertex> getMutationSites(List<MutationType> ignoredTypes)
    {
        List<Vertex> lst = new ArrayList<Vertex>();
        // capping groups are not considered mutable sites
        if (getBuildingBlockType() == Vertex.BBType.CAP)
        {
            return lst;
        }
        
        switch (contractLevel) 
        {
            case FIXED:
                if (getMutationTypes(ignoredTypes).size()>0)
                    lst.add(this);
                break;
                
            case FIXED_STRUCT:
                updateMutTypeToFixedSTructure();
                for (Vertex v : innerGraph.gVertices) 
                {
                    lst.addAll(v.getMutationSites(ignoredTypes));
                }
                break;
                
            case FREE:
                for (Vertex v : innerGraph.gVertices) 
                {
                    lst.addAll(v.getMutationSites(ignoredTypes));
                }
                break;
        }
        return lst;
    }
    
//------------------------------------------------------------------------------
    
    private void updateMutTypeToFixedSTructure()
    {
        List<MutationType> toBeRemoved = new ArrayList<MutationType>();
        toBeRemoved.add(MutationType.DELETE);
        toBeRemoved.add(MutationType.DELETECHAIN);
        toBeRemoved.add(MutationType.CHANGEBRANCH);
        toBeRemoved.add(MutationType.ADDLINK);
        toBeRemoved.add(MutationType.DELETELINK);
        toBeRemoved.add(MutationType.EXTEND);
        for (Vertex v : innerGraph.gVertices) 
        {
            for (MutationType mt : toBeRemoved)
                v.removeMutationType(mt);
            
            if (v instanceof Template)
                ((Template) v).setContractLevel(
                        ContractLevel.FIXED_STRUCT);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Adds attachment point (AP) to the list of required APs on this template
     * @param ap attachment point to require from this template
     * @throws DENOPTIMException 
     */
    public void addRequiredAP(Point3d pt, APClass apClass) {
        clearIAtomContainer();
        if (getInnerGraph() != null) {
            throw new IllegalArgumentException("cannot add more required APs " +
                    "after setting the inner graph");
        }
        AttachmentPoint ap = new AttachmentPoint(this, -1, pt, 
                apClass);
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
    public boolean sameAs(Template other, StringBuilder reason)
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
            for (AttachmentPoint tAP : this.requiredAPs)
            {
                for (AttachmentPoint oAP : other.requiredAPs)
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

    private List<AttachmentPoint> getRequiredAPs() {
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
    
    public static Template fromJson(String json)
    {
        Gson gson = DENOPTIMgson.getReader();
        
        // This deserializes many "easy" fields, but not the embedded graph
        // which is not "easy" as it need its own deserializer to 
        // recreate all the references to APs/edges/vertexes.
        Template t = gson.fromJson(json, Template.class);
        
        // Now, recover the missing bits (if present) from the original string
        JsonObject jsonObject = (JsonObject) Streams.parse(new JsonReader(
                new StringReader(json)));
       
        if (jsonObject.has("innerGraph"))
        {
            JsonObject innerGraphJson = jsonObject.getAsJsonObject(
                    "innerGraph");
            DGraph innerGraph = DGraph.fromJson(innerGraphJson.toString());
            t.setInnerGraph(innerGraph);

            if (jsonObject.has("innerToOuterAPs"))
            {
                Type type = new TypeToken<TreeMap<Integer,
                        AttachmentPoint>>(){}.getType();
                TreeMap<Integer,AttachmentPoint> map = gson.fromJson(
                        jsonObject.getAsJsonObject("innerToOuterAPs"), type);
                t.updateInnerToOuter(map);
            }
        }
        
        for (AttachmentPoint ap : t.getAttachmentPoints())
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

