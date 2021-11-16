package denoptim.molecule;

import java.lang.reflect.Type;

/*
 *   DENOPTIM
 *   Copyright (C) 2019 Marco Foscato <marco.foscato@uib.no>
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import denoptim.constants.DENOPTIMConstants;
import denoptim.fragspace.FragmentSpace;
import denoptim.io.DenoptimIO;
import denoptim.molecule.DENOPTIMVertex.VertexType;
import denoptim.utils.DENOPTIMgson;
import denoptim.utils.GraphUtils;


/**
 * An empty vertex has the behaviours of a vertex, but has no molecular 
 * structure. 
 * It has attachment points, as well as relations between those
 * attachment points, but has no atoms.
 *
 */
public class EmptyVertex extends DENOPTIMVertex
{

    /**
     * Version UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Attachment points on this vertex
     */
    private ArrayList<DENOPTIMAttachmentPoint> lstAPs;

    /**
     * List of AP sets that are related to each other, so that we
     * call them "symmetric" (though symmetry is a fuzzy concept here).
     */
    private ArrayList<SymmetricSet> lstSymAPs;

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty vertex.
     */
    public EmptyVertex()
    {
        super(VertexType.EmptyVertex, GraphUtils.getUniqueVertexIndex());
        lstAPs = new ArrayList<DENOPTIMAttachmentPoint>();
        lstSymAPs = new ArrayList<SymmetricSet>();
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for an identified vertex without attachment points.
     * @param id the VertedID of the vertex to construct. Note that this ID 
     * should be unique within a graph. To generate unique IDs either use 
     * {@link GraphUtils#getUniqueVertexIndex()} or use constructor
     * {@link EmptyVertex()}.
     */
    public EmptyVertex(int id)
    {
        super(VertexType.EmptyVertex, id);
        lstAPs = new ArrayList<DENOPTIMAttachmentPoint>();
        lstSymAPs = new ArrayList<SymmetricSet>();
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for an identified vertex with attachment points.
     * @param id the VertedID of the vertex to construct. Note that this ID 
     * should be unique within a graph. To generate unique IDs either use 
     * @param lstAPs the list of attachment point on this vertex.
     */
    public EmptyVertex(int id, ArrayList<DENOPTIMAttachmentPoint> lstAPs)
    {
        super(VertexType.EmptyVertex, id);
        this.lstAPs = lstAPs;
        this.lstSymAPs = new ArrayList<SymmetricSet>();
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for an identified vertex.
     * @param id the VertedID of the vertex to construct. Note that this ID 
     * should be unique within a graph. To generate unique IDs either use 
     * @param lstAPs the list of attachment point on this vertex.
     * @param lstSymAPs the list of symmetric sets of APs.
     * @param isRCV set <code>true</code> to mark this vertex as a 
     * ring-closing vertex.
     */
    public EmptyVertex(int id, ArrayList<DENOPTIMAttachmentPoint> lstAPs,
            ArrayList<SymmetricSet> lstSymAPs, boolean isRCV)
    {
        super(VertexType.EmptyVertex, id);
        this.lstAPs = lstAPs;
        this.lstSymAPs = lstSymAPs;
        setAsRCV(isRCV);
    }

//------------------------------------------------------------------------------

    public ArrayList<DENOPTIMAttachmentPoint> getAttachmentPoints()
    {
        return lstAPs;
    }

//------------------------------------------------------------------------------

    public void setSymmetricAP(ArrayList<SymmetricSet> sAPs)
    {
        lstSymAPs = sAPs;
    }

//------------------------------------------------------------------------------

    public ArrayList<SymmetricSet> getSymmetricAP()
    {
        return lstSymAPs;
    }

//------------------------------------------------------------------------------

    /**
     * For the given attachment point index locate the symmetric partners
     * i.e. those with similar environments and class types.
     * @param apIdx index of the attachment point which we want to get
     * the symmetrically related partners of.
     * @return the list of attachment point IDs, which include
     * <code>apIdx</code> or <code>null</code> if no partners present
     */

    @Override
    public SymmetricSet getSymmetricAPs(int apIdx)
    {
        for (SymmetricSet symmetricSet : lstSymAPs)
        {
            if (symmetricSet.contains(apIdx))
            {
                return symmetricSet;
            }
        }
        return null;
    }

//------------------------------------------------------------------------------

    public int getNumberOfAPs()
    {
        return lstAPs.size();
    }

//------------------------------------------------------------------------------

    /**
     *
     * @return list of attachment points that have free valences
     */

    public ArrayList<Integer> getFreeAPList()
    {
        ArrayList<Integer> lstAvailableAP = new ArrayList<>();
        for (int i=0; i<lstAPs.size(); i++)
        {
            if (lstAPs.get(i).isAvailable())
                lstAvailableAP.add(i);
        }
        return lstAvailableAP;
    }

//------------------------------------------------------------------------------

    public int getFreeAPCount()
    {
        int n = 0;
        for (DENOPTIMAttachmentPoint denoptimAttachmentPoint : lstAPs) {
            if (denoptimAttachmentPoint.isAvailable())
                n++;
        }
        return n;
    }

//------------------------------------------------------------------------------

    public boolean hasFreeAP()
    {
        for (DENOPTIMAttachmentPoint denoptimAttachmentPoint : lstAPs) {
            if (denoptimAttachmentPoint.isAvailable())
                return true;
        }
        return false;
    }

//------------------------------------------------------------------------------

    /**
     *
     * @return <code>true</code> if vertex has symmetric APs
     */

    public boolean hasSymmetricAP()
    {
        return !lstSymAPs.isEmpty();
    }

//------------------------------------------------------------------------------

    @Override
    public String toString()
    {
        return getVertexId() + "_EmptyVertex";
    }

//------------------------------------------------------------------------------

    public void cleanup()
    {
        if (lstSymAPs != null)
        {
            lstSymAPs.clear();
        }
        if (lstAPs != null)
        {
            lstAPs.clear();
        }
    }

//------------------------------------------------------------------------------

    /**
     * Returns a deep-copy of this vertex
     * @return a deep-copy
     */
    @Override
    public EmptyVertex clone()
    {
        EmptyVertex c = new EmptyVertex(getVertexId());
        c.setLevel(this.getLevel());
        c.setAsRCV(this.isRCV());
        c.setBuildingBlockId(this.getBuildingBlockId());
        c.setBuildingBlockType(this.getBuildingBlockType());
        
        c.setMutationTypes(this.getUnfilteredMutationTypes());
        
        for (DENOPTIMAttachmentPoint ap : lstAPs)
        {
            c.addAP(ap.getAtomPositionNumber(),
                    ap.getDirectionVector(),
                    ap.getAPClass());
        }

        ArrayList<SymmetricSet> cLstSymAPs = new ArrayList<SymmetricSet>();
        for (SymmetricSet ss : lstSymAPs)
        {
            cLstSymAPs.add(ss.clone());
        }
        c.setSymmetricAPSets(cLstSymAPs);

        return c;
    }
    
//------------------------------------------------------------------------------

    /**
     * Adds an attachment point with a dummy APClass and dummy properties.
     * This is used only for testing purposes.
     */
    public void addAP() {
        addAP(0);
    }

//------------------------------------------------------------------------------

    /**
     * Adds an attachment point with a dummy APClass.
     * @param atomPositionNumber the index of the source atom (0-based)
     */
    public void addAP(int atomPositionNumber) {
        DENOPTIMAttachmentPoint ap = new DENOPTIMAttachmentPoint(this, 
                atomPositionNumber);
        getAttachmentPoints().add(ap);
    }

//------------------------------------------------------------------------------

    /**
     * Adds an attachment point.
     * @param atomPositionNumber the index of the source atom (0-based)
     * @param apClass the APClass
     */
    public void addAP(int atomPositionNumber, APClass apClass) {
        addAP(atomPositionNumber, new double[]{0.0, 0.0, 0.0}, apClass);
    }

//------------------------------------------------------------------------------

    /**
     * Adds an attachment point.
     * @param atomPositionNumber the index of the source atom (0-based)
     * @param dirVec the AP direction vector end (the beginning at the 
     * coordinates of the source atom). This must array have 3 entries.
     * @param apClass the APClass
     */
    public void addAP(int atomPositionNumber, double[] dirVec, APClass apClass) {
        DENOPTIMAttachmentPoint ap = new DENOPTIMAttachmentPoint(this,
                atomPositionNumber, dirVec, apClass);
        getAttachmentPoints().add(ap);
    }

//------------------------------------------------------------------------------

    /**
     * Compares this and another vertex ignoring vertex IDs.
     * @param other
     * @param reason string builder used to build the message clarifying the
     * reason for returning <code>false</code>.
     * @return <code>true</code> if the two vertices represent the same graph
     * node even if the vertex IDs are different.
     */

    public boolean sameAs(EmptyVertex other, StringBuilder reason)
    {
        return sameVertexFeatures(other, reason);
    }

//------------------------------------------------------------------------------

    /**
     *
     * @param cmpReac list of reactions of the source vertex attachment point
     * @return list of indices of the attachment points in vertex that has
     * the corresponding reaction
     */

    public ArrayList<Integer> getCompatibleClassAPIndex(
            String cmpReac) 
    {
        ArrayList<DENOPTIMAttachmentPoint> apLst = getAttachmentPoints();
        ArrayList<Integer> apIdx = new ArrayList<>();
        for (int i = 0; i < apLst.size(); i++)
        {
            DENOPTIMAttachmentPoint dap = apLst.get(i);
            if (dap.isAvailable())
            {
                // check if this AP has the compatible reactions
                String dapReac = dap.getAPClass().toString();
                if (dapReac.compareToIgnoreCase(cmpReac) == 0)
                {
                    apIdx.add(i);
                }
            }
        }
        return apIdx;
    }

//------------------------------------------------------------------------------

    public int getHeavyAtomsCount()
    {
        return 0;
    }

//------------------------------------------------------------------------------

    public boolean containsAtoms()
    {
        return false;
    }
    

//-----------------------------------------------------------------------------

    /**
     * Although empty vertex do not contain atoms, by definitions, we allow
     * the generation of an SDF representation that uses an empty atom list.
     * @return an empty atom container with attachment points rooted on 
     * non-existing atoms, i.e., atom source index is negative.
     */
    
    @Override
    public IAtomContainer getIAtomContainer()
    {
        IAtomContainer iac = new AtomContainer();
        
        // We need to get the APs sorted by pseudo-atom source ID. This
        // to write the CLASS and ATTACHMENT_PPOINT fields of the SDF.
        
        LinkedHashMap<Integer,List<DENOPTIMAttachmentPoint>> apsPerAtom = 
                new LinkedHashMap<>();
        for (DENOPTIMAttachmentPoint ap : getAttachmentPoints()) 
        {
            int atmSrcId = ap.getAtomPositionNumber();
            if (apsPerAtom.containsKey(atmSrcId))
            {
                apsPerAtom.get(atmSrcId).add(ap);
            } else {
                List<DENOPTIMAttachmentPoint> list = 
                        new ArrayList<DENOPTIMAttachmentPoint>();
                list.add(ap);
                apsPerAtom.put(atmSrcId, list);
            }
        }
        iac.setProperty(DENOPTIMConstants.APSTAG, 
                DenoptimIO.getAPDefinitionsForSDF(apsPerAtom));
        iac.setProperty(DENOPTIMConstants.VERTEXJSONTAG,this.toJson());
        
        return iac;
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
    
    public static EmptyVertex fromJson(String json)
    {   
        Gson gson = DENOPTIMgson.getReader();
        EmptyVertex ev = gson.fromJson(json, EmptyVertex.class);
        for (DENOPTIMAttachmentPoint ap : ev.getAttachmentPoints())
        {
            ap.setOwner(ev);
        }
        return ev;
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the list of all APClasses on present on this fragment.
     * @return the list of APClassess
     */

    public ArrayList<APClass> getAllAPClasses()
    {
        ArrayList<APClass> lst = new ArrayList<APClass>();
        for (DENOPTIMAttachmentPoint ap : lstAPs)
        {
            APClass apCls = ap.getAPClass();
            if (!lst.contains(apCls))
            {
                lst.add(apCls);
            }
        }
        return lst;
    }

//-----------------------------------------------------------------------------

    @Override
    public void setAttachmentPoints(ArrayList<DENOPTIMAttachmentPoint> lstAP)
    {
        this.lstAPs = lstAP;
    }

//-----------------------------------------------------------------------------

    @Override
    protected void setSymmetricAPSets(ArrayList<SymmetricSet> sAPs)
    {
        this.lstSymAPs = sAPs;
    }

//-----------------------------------------------------------------------------

    @Override
    public ArrayList<SymmetricSet> getSymmetricAPSets()
    {
        return lstSymAPs;
    }

//------------------------------------------------------------------------------

    @Override
    public List<DENOPTIMVertex> getMutationSites()
    {
        List<DENOPTIMVertex> lst = new ArrayList<DENOPTIMVertex>();
        switch (getBuildingBlockType())
        {
            case CAP:
                break;
                
            case SCAFFOLD:
                break;
                
            default:
                if (getMutationTypes().size()>0)
                    lst.add(this);
                break;
        }
        return lst;
    }
    
//------------------------------------------------------------------------------

}
