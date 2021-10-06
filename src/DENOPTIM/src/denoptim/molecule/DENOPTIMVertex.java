/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no> and
 *   Marco Foscato <marco.foscato@uib.no>
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

package denoptim.molecule;

import java.lang.reflect.Type;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonDeserializer;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.utils.DENOPTIMgson;
import denoptim.utils.GraphUtils;
import denoptim.utils.MutationType;

/**
 * A vertex is a data structure that has an identity and holds a 
 * list of attachment points. The attachment points can be related by
 * an here-undefined relation.
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public abstract class DENOPTIMVertex implements Cloneable, Serializable
{
    /**
     * Version UID
     */
    private static final long serialVersionUID = -6093013990421027436L;
    
    /**
     * Graph that includes this vertex
     */
    private DENOPTIMGraph owner;

    /**
     * Unique identifier associated with the vertex instance
     */
    private int vertexId;
    
    /**
     * Index of this building block in the library of building blocks, or
     * negative if this vertex is not part of a library.
     */
    protected int buildingBlockId = -99;
    
    /**
     * The type of building block. This is used to easily distinguish among
     * building blocks that can be used to start new graphs (i.e., the so-called
     * scaffolds), those that can be use anywhere (i.e., fragments), and 
     * building blocks that can be used to saturate open valences (i.e., the 
     * capping groups).
     */
    
    public enum BBType {
        UNDEFINED, SCAFFOLD, FRAGMENT, CAP, NONE;
        
        private int i = -99;
        
        static {
            NONE.i = -1;
            SCAFFOLD.i = 0;
            FRAGMENT.i = 1;
            CAP.i = 2;
        }
        
        /**
         * Translates the integer into the enum
         * @param i 0:scaffold, 1:fragment, 2:capping group
         * @return the corresponding enum
         */
        public static BBType parseInt(int i) {
            BBType bbt;
            switch (i)
            {
                case 0:
                    bbt = SCAFFOLD;
                    break;
                case 1:
                    bbt = FRAGMENT;
                    break;
                case 2:
                    bbt = CAP;
                    break;
                default:
                    bbt = UNDEFINED;
                    break;
            }
            return bbt;
        }
        
        /**
         * @return 0:scaffold, 1:fragment, 2:capping group
         */
        public int toOldInt()
        {
            return i;
        }
    }
    
    /*
     * Building block type distinguished among types of building blocks:
     * scaffolds, fragments, and capping. 
     * Can be undefined, which is the default.
     */
    protected BBType buildingBlockType = DENOPTIMVertex.BBType.UNDEFINED;

    /*
     * Flag indicating that this as a ring closing vertex
     */
    private boolean isRCV;
    
    //TODO-V3 remove: get it from the graph. It is a property of a vertex in a 
    // graph, not of a vertex in itself. Also, how is the level counted when
    // we are after (or inside) a template?
    /*
     * if the level at which this vertex is in a graph
     */
    private int level = -99; //Initialised to meaningless value
    
    /**
     * Map of customisable properties
     */
    private Map<Object, Object> properties;

    /**
     * List of mutations that we can perform on this vertex
     */
    private List<MutationType> allowedMutationTypes = 
            new ArrayList<MutationType>(Arrays.asList(MutationType.values()));
    
    /**
     * Field distinguishing subclasses of {@link DENOPTIMVertex} when 
     * deserializing JSON representations.
     */
    protected final VertexType vertexType;
    // NB: Don't make static or Gson will ignore it!
    
    public enum VertexType {
        MolecularFragment,
        EmptyVertex,
        Template,
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty vertex.
     */
    public DENOPTIMVertex(VertexType vertexType)
    {
        this.vertexType = vertexType;
        vertexId = GraphUtils.getUniqueVertexIndex();
        isRCV = false;
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for an identified vertex without attachment points.
     * @param id the VertedID of the vertex to construct. Note that this ID 
     * should be unique within a graph. To generate unique IDs either use 
     * {@link GraphUtils#getUniqueVertexIndex()} or use constructor
     * {@link DENOPTIMVertex()}.
     */
    public DENOPTIMVertex(VertexType vertexType, int id)
    {
        this(vertexType);
        vertexId = id;
    }
    
  //------------------------------------------------------------------------------

    /**
     * Constructor for an identified vertex with attachment points.
     * @param id the VertedID of the vertex to construct. Note that this ID 
     * should be unique within a graph. To generate unique IDs use 
     * {@link GraphUtils#getUniqueVertexIndex()}.
     */
    public DENOPTIMVertex(VertexType vertexType, int id, 
            ArrayList<DENOPTIMAttachmentPoint> lstAPs)
    {
        this(vertexType, id);
        setAttachmentPoints(lstAPs);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for an identified vertex.
     * @param id the VertedID of the vertex to construct. Note that this ID 
     * should be unique within a graph. To generate unique IDs use 
     * {@link GraphUtils#getUniqueVertexIndex()}.
     */
    public DENOPTIMVertex(VertexType vertexType, int id,
            ArrayList<DENOPTIMAttachmentPoint> lstAPs,
            ArrayList<SymmetricSet> lstSymAPs, boolean isRCV)
    {
        this(vertexType, id, lstAPs);
        setSymmetricAPSets(lstSymAPs);
        this.isRCV = isRCV;
    }
    
//------------------------------------------------------------------------------

    /**
     * Builds a new molecular fragment kind of vertex.
     * @param bbId 0-based index of building block in the library
     * @param bbt the type of building block 0:scaffold, 1:fragment, 
     * 2:capping group
     * @throws DENOPTIMException 
     */
    public static DENOPTIMVertex newVertexFromLibrary(int bbId, 
            DENOPTIMVertex.BBType bbt) throws DENOPTIMException
    {
        return newVertexFromLibrary(GraphUtils.getUniqueVertexIndex(),bbId,bbt);
    }
    
//------------------------------------------------------------------------------

    /**
     * Make a new vertex that is a copy of a vertex in the fragment space.
     * @param vertexId unique identified of the vertex
     * @param bbId 0-based index of building block in the library
     * @param bbt the type of building block
     * @throws DENOPTIMException 
     */
    public static DENOPTIMVertex newVertexFromLibrary(int vertexId, int bbId, 
            DENOPTIMVertex.BBType bbt) throws DENOPTIMException
    {   
        // The actual type of vertex
        // returned by this method depends on the what we get from the
        // FragmentSpace.getVertexFromLibrary call
        DENOPTIMVertex v = FragmentSpace.getVertexFromLibrary(bbt,bbId);
        v.setVertexId(vertexId);
        
        v.setAsRCV(v.getNumberOfAPs() == 1
                && APClass.RCAAPCLASSSET.contains(
                        v.getAttachmentPoints().get(0).getAPClass()));
        
        return v;
    }

//------------------------------------------------------------------------------

    public abstract ArrayList<DENOPTIMAttachmentPoint> getAttachmentPoints();

//------------------------------------------------------------------------------

    /**
     * Append and attachment point to the list of attachment points on this 
     * vertex.
     * @param ap the attachment point to add
     */
    protected void addAttachmentPoint(DENOPTIMAttachmentPoint ap)
    {
        ap.setOwner(this);
        getAttachmentPoints().add(ap);
    }
    
//------------------------------------------------------------------------------

    //TODO-V3: should be protected? Now it's public to allow refilling of AP list
    // as done in GraphConversionTool.getGraphFromString to recover at least
    // some of the entire list of APs of a vertex read-in from a string
    // representation of a graph. That's obviously not ideal, so eventually
    // we must get rid of it and move this back to protected.
    public abstract void setAttachmentPoints(ArrayList<DENOPTIMAttachmentPoint> lstAP);
    
//------------------------------------------------------------------------------
    
    public void setAsRCV(boolean isRCV)
    {
        this.isRCV = isRCV;
    }
    
//------------------------------------------------------------------------------

    public void setVertexId(int id)
    {
        this.vertexId = id;
    }

//------------------------------------------------------------------------------

    public int getVertexId()
    {
        return vertexId;
    }

//------------------------------------------------------------------------------

    public int getBuildingBlockId()
    {
        return buildingBlockId;
    }

//------------------------------------------------------------------------------

    public void setBuildingBlockId(int buildingBlockId)
    {
        this.buildingBlockId = buildingBlockId;
    }

//------------------------------------------------------------------------------

    public DENOPTIMVertex.BBType getBuildingBlockType()
    {
        return buildingBlockType;
    }
    
//------------------------------------------------------------------------------

    public void setBuildingBlockType(DENOPTIMVertex.BBType buildingBlockType)
    {
        this.buildingBlockType = buildingBlockType;
    }
    
//------------------------------------------------------------------------------

    protected abstract void setSymmetricAPSets(ArrayList<SymmetricSet> sAPs);

//------------------------------------------------------------------------------

    public abstract ArrayList<SymmetricSet> getSymmetricAPSets();

//------------------------------------------------------------------------------

    /**
     * For the given attachment point index locate the symmetric partners
     * i.e. those with similar environments and class types.
     * @param apIdx index of the attachment point which we want to get
     * the symmetrically related partners of.
     * @return the list of attachment point IDs, which include 
     * <code>apIdx</code> or <code>null</code> if no partners present
     */

    public SymmetricSet getSymmetricAPs(int apIdx)
    {
        for (SymmetricSet symmetricSet : getSymmetricAPSets()) 
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
        return getAttachmentPoints().size();
    }

//------------------------------------------------------------------------------

    public int getFreeAPCount()
    {
        int n = 0;
        for (DENOPTIMAttachmentPoint ap : getAttachmentPoints()) 
        {
            if (ap.isAvailable())
                n++;
        }
        return n;
    }

//------------------------------------------------------------------------------

    /**
     * Counts the number of attachment points that are availability throughout 
     * the graph level, i.e., checks also across the inner graph template 
     * boundary. This method does account for embedding of the vertex in a 
     * template, i.e., APs can be available in the graph owning this vertex,
     * but if the graph is itself the inner graph of a template, the AP is 
     * then projected on the template's surface and used to make an edge that 
     * uses the template as a single vertex. To ignore this possibility and 
     * consider only edges that belong to the graph owning this vertex, use
     * {@Link DENOPTIMVertex#getFreeAPCount()}.
     * @return the number of APs that are not used by any edge, whether within
     * the graph owning this vertex (if any) or within a graph owning the
     * template embedding the graph that owns this vertex.
     */
    public int getFreeAPCountThroughout()
    {
        int n = 0;
        for (DENOPTIMAttachmentPoint ap : getAttachmentPoints()) 
        {
            if (ap.isAvailableThroughout())
                n++;
        }
        return n;
    }

//------------------------------------------------------------------------------

    public boolean hasFreeAP()
    {
        for (DENOPTIMAttachmentPoint ap : getAttachmentPoints()) 
        {
            if (ap.isAvailable())
                return true;
        }
        return false;
    }

//------------------------------------------------------------------------------

    public void setLevel(int level)
    {
        this.level = level;
    }

//------------------------------------------------------------------------------

    public int getLevel()
    {
        return level;
    }

//------------------------------------------------------------------------------

    /**
     *
     * @return <code>true</code> if vertex is a ring closing vertex
     */

    public boolean isRCV()
    {
        return isRCV;
    }

//------------------------------------------------------------------------------

    /**
     *
     * @return <code>true</code> if vertex has symmetric APs
     */

    public boolean hasSymmetricAP()
    {
        return !getSymmetricAPSets().isEmpty();
    }

//------------------------------------------------------------------------------

    /**
     * Produces a human readable, short string to represent the vertex by its
     * vertex ID, building block ID (1-based), building block type, and level
     * in the graph (if any). This is the old syntax used up to version 2 for
     * reporting a vertex in the string representation of a graph. Such notation
     * cannot hold all the information needed to define a template, and is,
     * therefore, obsolete. Use JSON format to serialize a graph that may
     * contain templates.
     */
    
    @Override
    public String toString()
    {
        return vertexId  + "_" + (buildingBlockId + 1) + "_" 
                + buildingBlockType.toOldInt() + "_" + level;
    }

//------------------------------------------------------------------------------
    
    public void cleanup()
    {
        if (getSymmetricAPSets() != null)
        {
            getSymmetricAPSets().clear();
        }
        if (getAttachmentPoints() != null)
        {
            getAttachmentPoints().clear();
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns a deep-copy of this vertex. Subclasses override this method.
     * @return a deep-copy
     */
    
    @Override
    public abstract DENOPTIMVertex clone();
    
//------------------------------------------------------------------------------
    
    /**
     * Compares this and another vertex ignoring vertex IDs.
     * @param other
     * @param reason string builder used to build the message clarifying the 
     * reason for returning <code>false</code>.
     * @return <code>true</code> if the two vertices represent the same graph
     * node even if the vertex IDs are different.
     */
    public boolean sameAs(DENOPTIMVertex other, StringBuilder reason)
    {
        if (this.getClass() == other.getClass())
        {
            if (this instanceof DENOPTIMFragment)
            {
                return ((DENOPTIMFragment) this).sameAs(
                        (DENOPTIMFragment) other, reason);
            } else if (this instanceof EmptyVertex) {
                return ((EmptyVertex) this).sameAs(
                        (EmptyVertex) other, reason);
            } else if (this instanceof DENOPTIMTemplate) {
                return ((DENOPTIMTemplate) this).sameAs(
                        (DENOPTIMTemplate) other, reason);
            } else {
                System.err.println("WARNING: Unimplemented sameAs method for "
                        + "vertex subtype '" + this.getClass().getName() + "'");
            }
        } 
        return sameVertexFeatures(other, reason);
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
    public boolean sameVertexFeatures(DENOPTIMVertex other, 
            StringBuilder reason)
    {	
        if (this.getBuildingBlockType() != other.getBuildingBlockType())
        {
            reason.append("Different building block type ("
                    + this.getBuildingBlockType()+":"
                    + other.getBuildingBlockType()+"); ");
            return false;
        }
        
        if (this.getBuildingBlockId() != other.getBuildingBlockId())
        {
            reason.append("Different molID ("+this.getBuildingBlockId()+":"
                    + other.getBuildingBlockId()+"); ");
            return false;
        }
        
        if (this.getFreeAPCount() != other.getFreeAPCount())
        {
            reason.append("Different number of free APs ("
                    +this.getFreeAPCount()+":"
                    +other.getFreeAPCount()+"); ");
            return false;
        }
        
        if (this.getNumberOfAPs() != other.getNumberOfAPs())
        {
            reason.append("Different number of APs ("
                    +this.getNumberOfAPs()+":"
                    +other.getNumberOfAPs()+"); ");
            return false;
        }
        
        for (DENOPTIMAttachmentPoint apT : this.getAttachmentPoints())
        {
            boolean found = false;
            for (DENOPTIMAttachmentPoint apO : other.getAttachmentPoints())
            {
                if (apT.sameAs(apO))
                {
                    found = true;
                    break;
                }
            }
            if (!found)
            {
                reason.append("No corresponding AP for "+apT);
                return false;
            }
        }
    	
    	return true;
    }

//------------------------------------------------------------------------------

    /**
     *
     * @param cmpReac list of APClasses of the attachment point we want to 
     * @return list of indices of the attachment points in vertex that has
     * the corresponding reaction
     */

    public ArrayList<Integer> getCompatibleClassAPIndex(
            APClass cmpReac) {
        ArrayList<DENOPTIMAttachmentPoint> apLst = getAttachmentPoints();
        ArrayList<Integer> apIdx = new ArrayList<>();
        for (int i = 0; i < apLst.size(); i++)
        {
            DENOPTIMAttachmentPoint dap = apLst.get(i);
            if (dap.isAvailable())
            {
                // check if this AP has the compatible reactions
                APClass dapReac = dap.getAPClass();
                if (dapReac.isCPMapCompatibleWith(cmpReac))
                {
                    apIdx.add(i);
                }
            }
        }
        return apIdx;
    }

//------------------------------------------------------------------------------

    public abstract int getHeavyAtomsCount();

//------------------------------------------------------------------------------

    public abstract boolean containsAtoms();
    
//------------------------------------------------------------------------------

    public abstract IAtomContainer getIAtomContainer();

//-----------------------------------------------------------------------------

    /**
     * Returns the list of all APClasses present on this vertex.
     * @return the list of APClassess
     */
    
    public ArrayList<APClass> getAllAPClasses()
    {
        ArrayList<APClass> lst = new ArrayList<APClass>();
        for (DENOPTIMAttachmentPoint ap : getAttachmentPoints())
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

    /**
     * Returns the list of all APClasses present on free attachment point
     * on this vertex.
     * @return the list of APClassess
     */
    
    public ArrayList<APClass> getAllAvailableAPClasses()
    {
        ArrayList<APClass> lst = new ArrayList<APClass>();
        for (DENOPTIMAttachmentPoint ap : getAttachmentPoints())
        {
            if (!ap.isAvailable())
                continue;
            
            APClass apCls = ap.getAPClass();
            if (!lst.contains(apCls))
            {
                lst.add(apCls);
            }
        }
        return lst;
    }
    
//------------------------------------------------------------------------------

    public void resetGraphOwner()
    {
        this.owner = null;
    }
    
//------------------------------------------------------------------------------

    public void setGraphOwner(DENOPTIMGraph owner)
    {
        this.owner = owner;
    }
    
//------------------------------------------------------------------------------

    public DENOPTIMGraph getGraphOwner()
    {
        return owner;
    }

//------------------------------------------------------------------------------

    public abstract List<DENOPTIMVertex> getMutationSites();

//------------------------------------------------------------------------------

    public void setMutationTypes(List<MutationType> lst)
    {
        allowedMutationTypes = lst;
    }
    
//------------------------------------------------------------------------------

    public List<MutationType> getMutationTypes()
    {
        List<MutationType> filteredTypes = new ArrayList<MutationType>(
                allowedMutationTypes);
        if (owner != null)
        {
            // this vertex is part of a graph
            
            // Cannot add/change link on vertex that is not linked
            if (getChilddren().isEmpty())
            {
                filteredTypes.remove(MutationType.ADDLINK);
                filteredTypes.remove(MutationType.CHANGELINK);
            }
            
            // Cannot extend vertex that has no truly free AP
            if (getFreeAPCountThroughout()==0)
                filteredTypes.remove(MutationType.EXTEND);
            
            // Cannot remove the only vertex of a graph
            if (owner.getVertexCount()==0)
                filteredTypes.remove(MutationType.DELETE);
        }
        
        return filteredTypes;
    }
    
//------------------------------------------------------------------------------

    /**
     * Get attachment point i on this vertex
     * @param i index of attachment point on this vertex
     * @return attachment point i on this vertex
     */
    public DENOPTIMAttachmentPoint getAP(int i) {
        return getAttachmentPoints().get(i);
    }

//------------------------------------------------------------------------------

    /**
     * Add ap to the end of the list of this vertex's list of attachment points
     * @param ap attachment point to add to this vertex
     */
    public void addAP(DENOPTIMAttachmentPoint ap) {
        ap.setOwner(this);
        getAttachmentPoints().add(ap);
    }

//------------------------------------------------------------------------------

    /**
     * Adds an attachment point with a dummy APClass and dummy properties.
     * This is used only for testing purposes.
     */
    public void addAP() {
        addAP(0, 0, 0);
    }

//------------------------------------------------------------------------------

    /**
     * Adds an attachment point with a dummy APClass.
     * @param atomPositionNumber the index of the source atom (0-based)
     * @param atomConnections the total number of connections
     * @param apConnections the number of free connections
     */
    public void addAP(int atomPositionNumber, int atomConnections,
                      int apConnections) {
        addAP(atomPositionNumber, atomConnections, apConnections,
                new double[3]);
    }

//------------------------------------------------------------------------------

    /**
     * Adds an attachment point with a dummy APClass.
     * @param atomPositionNumber the index of the source atom (0-based)
     * @param atomConnections the total number of connections
     * @param apConnections the number of free connections
     * @param dirVec the AP direction vector end (the beginning at the coords
     *               of the source atom). This must array have 3 entries.
     */
    public void addAP(int atomPositionNumber, int atomConnections,
                      int apConnections, double[] dirVec) {
        try
        {
            addAP(atomPositionNumber, atomConnections, apConnections, dirVec,
                    APClass.make(""));
        } catch (DENOPTIMException e)
        {
            // We should never reach this point because the make("") will
            // result in an AP with class "noclass:0"
            e.printStackTrace();
        }
    }

//------------------------------------------------------------------------------

    /**
     * Adds an attachment point.
     * @param atomPositionNumber the index of the source atom (0-based)
     * @param atomConnections the total number of connections
     * @param apConnections the number of free connections
     * @param apClass the APClass
     */
    public void addAP(int atomPositionNumber, int atomConnections,
                      int apConnections, APClass apClass) {
        addAP(atomPositionNumber, atomConnections, apConnections,
                new double[3], apClass);
    }

//------------------------------------------------------------------------------

    /**
     * Constructor
     * @param atomPositionNumber the index of the source atom (0-based)
     * @param atomConnections the total number of connections
     * @param apConnections the number of free connections
     * @param dirVec the AP direction vector end (the beginning at the coords
     *               of the source atom). This must array have 3 entries.
     * @param apClass the APClass
     */
    public void addAP(int atomPositionNumber, int atomConnections,
                      int apConnections, double[] dirVec, APClass apClass) {
        addAP(new DENOPTIMAttachmentPoint(this,
                atomPositionNumber, atomConnections, apConnections, dirVec,
                apClass));
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the position of the given AP in the list of APs of this vertex
     * @param ap the AP to find in the list of APs
     * @return the index (0-n) of <code>ap</code> or -1 if that AP does not 
     * belong to this vertex.
     */
    public int getIndexOfAP(DENOPTIMAttachmentPoint ap)
    {
        for (int i=0; i<getAttachmentPoints().size(); i++)
        {
            DENOPTIMAttachmentPoint candAp = getAttachmentPoints().get(i);
            if (candAp == ap)
            {
                return i;
            }
        }
        return -1;
    }
    
//------------------------------------------------------------------------------

    /**
     * Looks into the edges that use any of the APs that belong to 
     * this vertex and returns the edge
     * that has this vertex as the target, i.e., the edge to the parent vertex.
     * We assume there is only one such edge.
     * @return the edge to the parent.
     */

    public DENOPTIMEdge getEdgeToParent() {
        for (DENOPTIMAttachmentPoint ap : getAttachmentPoints())
        {
            DENOPTIMEdge user = ap.getEdgeUser();
            if (user == null)
                continue;
            
            if (ap == user.getTrgAP())
            {
                return user;
            }
        }
        return null;
    }
    
//------------------------------------------------------------------------------

    /**
     * Looks into the edges that use any of the APs that belong to 
     * this vertex and returns the vertex which is the source of the edge
     * in which this vertex is the target.
     * @return the vertex parent to this or null.
     */

    public DENOPTIMVertex getParent() {
        for (DENOPTIMAttachmentPoint ap : getAttachmentPoints())
        {
            DENOPTIMEdge user = ap.getEdgeUser();
            if (user == null)
                continue;
            
            if (ap == user.getTrgAP())
            {
                return user.getSrcAP().getOwner();
            }
        }
        return null;
    }
    
//------------------------------------------------------------------------------

    /**
     * Looks into the edges that use any of the APs that belong to 
     * this vertex and returns the list of vertices which are target of any 
     * edge departing from this vertex. Only the directly connected children
     * are considered (no recursion).
     * @return the list of child vertices (can be empty list, but not null)
     */

    public ArrayList<DENOPTIMVertex> getChilddren() {
        ArrayList<DENOPTIMVertex> children = new ArrayList<DENOPTIMVertex>();
        for (DENOPTIMAttachmentPoint ap : getAttachmentPoints())
        {
            DENOPTIMEdge user = ap.getEdgeUser();
            if (user == null)
                continue;
            
            if (ap == user.getSrcAP())
            {
                children.add(user.getTrgAP().getOwner());
            }
        }
        return children;
    }
    
//------------------------------------------------------------------------------
    
    public Object getProperty(Object description)
    {
        if (properties == null)
        {
            return null;
        } else {
            return properties.get(description);
        }
    }
    
//-----------------------------------------------------------------------------
    
    public void setProperty(Object key, Object property)
    {
        if (properties == null)
        {
            properties = new HashMap<Object, Object>();
        }
        properties.put(key, property);
    }
    
//------------------------------------------------------------------------------
    
    public static DENOPTIMVertex fromJson(String json)
    {
        Gson gson = DENOPTIMgson.getReader();
        return gson.fromJson(json, DENOPTIMVertex.class);
    }

//------------------------------------------------------------------------------

    public static class DENOPTIMVertexDeserializer 
    implements JsonDeserializer<DENOPTIMVertex>
    {
        @Override
        public DENOPTIMVertex deserialize(JsonElement json, Type typeOfT,
                JsonDeserializationContext context) throws JsonParseException
        {
            JsonObject jsonObject = json.getAsJsonObject();
            
            if (!jsonObject.has("vertexType"))
            {
                String msg = "Missing 'vertexType': found a "
                        + "JSON string generated by a previous version and "
                        + "that is no longer compatible. "
                        + "Upgrade the JSON string by adding "
                        + "the member 'vertexType', which can be any of these:";
                for (VertexType v : VertexType.values())
                    msg = msg + " " + v;
                msg = msg + ". ";
                throw new JsonParseException(msg);
            }       

            // Deseralization differs for the types of vertices
            VertexType vt = context.deserialize(jsonObject.get("vertexType"),
                    VertexType.class);
            switch (vt)
            {
                case Template:
                {
                    DENOPTIMTemplate t = DENOPTIMTemplate.fromJson(
                            jsonObject.toString());
                    return t;
                }
                
                case MolecularFragment:
                {
                    DENOPTIMFragment f = DENOPTIMFragment.fromJson(
                            jsonObject.toString());
                    return f;
                }
                
                case EmptyVertex:
                {
                    EmptyVertex ev = EmptyVertex.fromJson(
                            jsonObject.toString());
                    return ev;
                }
                
                default:
                {
                    return null;
                }
            }
        }
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
    public String[] getPathIDs(DENOPTIMAttachmentPoint apA,
            DENOPTIMAttachmentPoint apB)
    {
        String a2b = this.getBuildingBlockId() + "/" 
                + this.getBuildingBlockType() + "/ap"
                + getIndexOfAP(apA) + "ap" + getIndexOfAP(apB) + "_";
        String b2a = this.getBuildingBlockId() + "/" 
                + this.getBuildingBlockType() + "/ap"
                + getIndexOfAP(apB) + "ap" + getIndexOfAP(apA) + "_";
        String[] pair = {a2b,b2a};
        return pair;
    }
    
//------------------------------------------------------------------------------

    /**
     * Processes an atom container and builds a vertex out of it.
     * @param iac the  atom containers.
     * @param bbt the type of building block
     * @return the vertex.
     * @throws DENOPTIMException if the atom container could not be converted 
     * into a {@link DENOPTIMFragment}.
     */
    
    public static DENOPTIMVertex convertIACToVertex(IAtomContainer iac, 
            DENOPTIMVertex.BBType bbt) throws DENOPTIMException
    {
        DENOPTIMVertex v;
        Object jsonGraph = iac.getProperty(DENOPTIMConstants.GRAPHJSONTAG);
        Object jsonVertex = iac.getProperty(DENOPTIMConstants.VERTEXJSONTAG);
        if (jsonGraph != null)
        {
            DENOPTIMTemplate t = new DENOPTIMTemplate(bbt);
            DENOPTIMGraph g = DENOPTIMGraph.fromJson(jsonGraph.toString());
            t.setInnerGraph(g);
            v = t;
        } else if (jsonVertex != null)
        {
            v = fromJson(jsonVertex.toString());
        } else {
            v = new DENOPTIMFragment(iac,bbt);
        }
        v.setAsRCV(v.getNumberOfAPs() == 1
                && APClass.RCAAPCLASSSET.contains(
                        v.getAttachmentPoints().get(0).getAPClass()));
        return v;
    }
    
//------------------------------------------------------------------------------
    
}
