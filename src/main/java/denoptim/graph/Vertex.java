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

package denoptim.graph;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.openscience.cdk.interfaces.IAtomContainer;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.json.DENOPTIMgson;
import denoptim.logging.StaticLogger;
import denoptim.utils.GraphUtils;
import denoptim.utils.MutationType;

/**
 * A vertex is a data structure that has an identity and holds a 
 * list of {@link AttachmentPoint}s.
 * 
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public abstract class Vertex implements Cloneable
{
    /**
     * Graph that includes this vertex
     */
    private DGraph owner;

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
    protected BBType buildingBlockType = Vertex.BBType.UNDEFINED;

    /*
     * Flag indicating that this as a ring closing vertex
     */
    private boolean isRCV;
    
    /**
     * Map of customizable properties
     */
    protected Map<Object, Object> properties;
    
    /**
     * List of properties required to make 
     * {@link Vertex#sameAs(Vertex, StringBuilder)} method
     * return <code>false</code> when property values differ.
     */
    protected Set<String> uniquefyingPropertyKeys = new HashSet<String>();

    /**
     * List of mutations that we can perform on this vertex
     */
    private List<MutationType> allowedMutationTypes = 
            new ArrayList<MutationType>(Arrays.asList(MutationType.values()));
    
    /**
     * Field distinguishing subclasses of {@link Vertex} when 
     * deserializing JSON representations.
     */
    protected final VertexType vertexType;
    // NB: Don't make static or Gson will ignore it!

    /**
     * Unique identified for vertices
     */
    //TODO-gg keep or trash?
    public AtomicInteger vrtxID = new AtomicInteger(0);
    
    /**
     * 
     */
    public enum VertexType {
        MolecularFragment,
        EmptyVertex,
        Template,
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty vertex.
     */
    public Vertex(VertexType vertexType)
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
    public Vertex(VertexType vertexType, int id)
    {
        this(vertexType);
        vertexId = id;
    }
    
//------------------------------------------------------------------------------

    /**
     * Builds a new molecular fragment kind of vertex.
     * @param bbId 0-based index of building block in the library
     * @param bbt the type of building block 0:scaffold, 1:fragment, 
     * 2:capping group
     * @throws DENOPTIMException 
     */
    public static Vertex newVertexFromLibrary(int bbId, 
            Vertex.BBType bbt, FragmentSpace fragSpace) 
                    throws DENOPTIMException
    {
        return newVertexFromLibrary(GraphUtils.getUniqueVertexIndex(), bbId, bbt,
                fragSpace);
    }
    
//------------------------------------------------------------------------------

    /**
     * Make a new vertex that is a copy of a vertex in the fragment space.
     * @param vertexId unique identified of the vertex
     * @param bbId 0-based index of building block in the library
     * @param bbt the type of building block
     * @throws DENOPTIMException 
     */
    public static Vertex newVertexFromLibrary(int vertexId, int bbId, 
            Vertex.BBType bbt, FragmentSpace fragSpace) 
                    throws DENOPTIMException
    {   
        // The actual type of vertex
        // returned by this method depends on the what we get from the
        // FragmentSpace.getVertexFromLibrary call
        Vertex v = fragSpace.getVertexFromLibrary(bbt,bbId);
        v.setVertexId(vertexId);
        
        v.setAsRCV(v.getNumberOfAPs() == 1
                && APClass.RCAAPCLASSSET.contains(
                        v.getAttachmentPoints().get(0).getAPClass()));
        
        return v;
    }

//------------------------------------------------------------------------------

    public abstract ArrayList<AttachmentPoint> getAttachmentPoints();

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

    public Vertex.BBType getBuildingBlockType()
    {
        return buildingBlockType;
    }
    
//------------------------------------------------------------------------------

    public void setBuildingBlockType(Vertex.BBType buildingBlockType)
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
        for (AttachmentPoint ap : getAttachmentPoints()) 
        {
            if (ap.isAvailable())
                n++;
        }
        return n;
    }
    
//------------------------------------------------------------------------------

    /**
     * Gets attachment points that are availability throughout 
     * the graph level, i.e., checks also across the inner graph template 
     * boundary. This method does account for embedding of the vertex in a 
     * template, i.e., APs can be available in the graph owning this vertex,
     * but if the graph is itself the inner graph of a template, the AP is 
     * then projected on the template's surface and used to make an edge that 
     * uses the template as a single vertex. To ignore this possibility and 
     * consider only edges that belong to the graph owning this vertex, use
     * {@link Vertex#getFreeAPCount()}.
     * @return the APs of this vertex that are not used by any edge, 
     * whether within
     * the graph owning this vertex (if any) or within a graph owning the
     * template embedding the graph that owns this vertex.
     */
    public ArrayList<AttachmentPoint> getFreeAPThroughout()
    {
        ArrayList<AttachmentPoint> lst = 
                new ArrayList<AttachmentPoint>();
        for (AttachmentPoint ap : getAttachmentPoints()) 
        {
            if (ap.isAvailableThroughout())
                lst.add(ap);
        }
        return lst;
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
     * {@link Vertex#getFreeAPCount()}.
     * @return the number of APs that are not used by any edge, whether within
     * the graph owning this vertex (if any) or within a graph owning the
     * template embedding the graph that owns this vertex.
     */
    public int getFreeAPCountThroughout()
    {
        return getFreeAPThroughout().size();
    }
    
//------------------------------------------------------------------------------

    /**
     * Gets attachment points that are used by capping groups. 
     * This method does NOT account for embedding of the vertex in a 
     * template, i.e., APs can be available in the graph owning this vertex,
     * but if the graph is itself the inner graph of a template, the AP is 
     * then projected on the template's surface and can be used. 
     * To account for this possibility use
     * {@link Vertex#getCappedAPsThroughout()}.
     * @return the APs of this vertex that are used to bind a {@link BBType#CAP} 
     * vertex.
     */
    public ArrayList<AttachmentPoint> getCappedAPs()
    {
        ArrayList<AttachmentPoint> lst = 
                new ArrayList<AttachmentPoint>();
        for (AttachmentPoint ap : getAttachmentPoints()) 
        {
            if (!ap.isAvailable())
            {
                AttachmentPoint linkedAP = ap.getLinkedAP();
                if (linkedAP.getOwner().getBuildingBlockType() == BBType.CAP)
                    lst.add(ap);
            }
        }
        return lst;
    }
    
//------------------------------------------------------------------------------

    /**
     * Gets attachment points that are used by capping groups throughout 
     * the graph levels, i.e., checks also across the inner graph template 
     * boundary. This method does account for embedding of the vertex in a 
     * template, i.e., APs can be available in the graph owning this vertex,
     * but if the graph is itself the inner graph of a template, the AP is 
     * then projected on the template's surface and used to make an edge that 
     * uses the template as a single vertex. To ignore this possibility and 
     * consider only edges that belong to the graph owning this vertex, use
     * {@link Vertex#getCappedAPs()}.
     * @return the APs of this vertex that are used to bind a {@link BBType#CAP} 
     * vertex, 
     * whether within the graph owning this vertex (if any) 
     * or within a graph owning the
     * template embedding the graph that owns this vertex.
     */
    public ArrayList<AttachmentPoint> getCappedAPsThroughout()
    {
        ArrayList<AttachmentPoint> lst = 
                new ArrayList<AttachmentPoint>();
        for (AttachmentPoint ap : getAttachmentPoints()) 
        {
            if (!ap.isAvailableThroughout())
            {
                AttachmentPoint linkedAP = ap.getLinkedAPThroughout();
                if (linkedAP.getOwner().getBuildingBlockType() == BBType.CAP)
                    lst.add(ap);
            }
        }
        return lst;
    }

//------------------------------------------------------------------------------

    /**
     * Counts the number of attachment points that are used by {@link BBType#CAP} 
     * vertex.  This method does account for embedding of the vertex in a 
     * template, i.e., APs can be available in the graph owning this vertex,
     * but if the graph is itself the inner graph of a template, the AP is 
     * then projected on the template's surface and used to make an edge that 
     * uses the template as a single vertex.
     * @return the number of APs of this vertex that are used to bind a 
     * {@link BBType#CAP} vertex, 
     * whether within the graph owning this vertex (if any) 
     * or within a graph owning the
     * template embedding the graph that owns this vertex.
     */
    public int getCappedAPCountThroughout()
    {
        return getCappedAPsThroughout().size();
    }
    
//------------------------------------------------------------------------------

    public boolean hasFreeAP()
    {
        for (AttachmentPoint ap : getAttachmentPoints()) 
        {
            if (ap.isAvailableThroughout())
                return true;
        }
        return false;
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
                + buildingBlockType.toOldInt();
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
    public abstract Vertex clone();
    
//------------------------------------------------------------------------------
    
    /**
     * Compares this and another vertex ignoring vertex IDs.
     * @param other
     * @param reason string builder used to build the message clarifying the 
     * reason for returning <code>false</code>.
     * @return <code>true</code> if the two vertices represent the same graph
     * node even if the vertex IDs are different.
     */
    //TODO-gg make abstract
    public boolean sameAs(Vertex other, StringBuilder reason)
    {
        if (this.getClass() == other.getClass())
        {
            if (this instanceof Fragment)
            {
                return ((Fragment) this).sameAs(
                        (Fragment) other, reason);
            } else if (this instanceof EmptyVertex) {
                return ((EmptyVertex) this).sameAs(
                        (EmptyVertex) other, reason);
            } else if (this instanceof Template) {
                return ((Template) this).sameAs(
                        (Template) other, reason);
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
    public boolean sameVertexFeatures(Vertex other, 
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
        
        // Order of APs must be the same
        for (int i=0; i<this.getNumberOfAPs(); i++)
        {
            AttachmentPoint apT = this.getAP(i);
            AttachmentPoint apO = other.getAP(i);
            if (!apT.sameAs(apO))
            {
                reason.append("Difference in AP "+i+": "+apT+" vs "+apO);
                return false;
            }
        }
        // The following fails for vertexes v1 and v2 like these:
        // (APC_A)<--v1-->(APC_A)
        // (APC_A)<--v2-->(APC_A)
        // Because the AP0 on v1 will be compared to AP1 on v2 and their 
        // different position in the list of APs will make them be non-same.
        /*
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
        */
    	
    	return true;
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
        for (AttachmentPoint ap : getAttachmentPoints())
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
        for (AttachmentPoint ap : getAttachmentPoints())
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

    public void setGraphOwner(DGraph owner)
    {
        this.owner = owner;
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the graph this vertex belongs to or null.
     * @return the graph this vertex belongs to or null.
     */
    public DGraph getGraphOwner()
    {
        return owner;
    }
    
//------------------------------------------------------------------------------

    /**
     * A list of mutation sites from within this vertex.
     * @return the list of vertexes that allow any mutation type.
     */
    public List<Vertex> getMutationSites()
    {
        return getMutationSites(new ArrayList<MutationType>());
    }

//------------------------------------------------------------------------------

    /**
     * A list of mutation sites from within this vertex.
     * @param ignoredTypes a collection of mutation types to ignore. Vertexes
     * that allow only ignored types of mutation will
     * not be considered mutation sites.
     * @return the list of vertexes that allow any non-ignored mutation type.
     */
    public abstract List<Vertex> getMutationSites(
            List<MutationType> ignoredTypes);

//------------------------------------------------------------------------------

    public void setMutationTypes(List<MutationType> lst)
    {
        allowedMutationTypes = lst;
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the mutation types that are constitutionally configures for this
     * vertex irrespectively on the graph or the context in which this vertex
     * is included.
     * @return the list of allowed mutation types as configured upon vertex 
     * constitution.
     */
    protected List<MutationType> getUnfilteredMutationTypes()
    {
        return allowedMutationTypes;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Removes the specified mutation type.
     * @param mt the element to remove.
     * @return <code>true</code> if this list contained the specified element.
     */
    public boolean removeMutationType(MutationType mt)
    {
        return allowedMutationTypes.remove(mt);
    }

//------------------------------------------------------------------------------

    /**
     * Returns the list of mutation types.
     * @return  the list of mutation types.
     */
    public List<MutationType> getMutationTypes()
    {
        return getMutationTypes(new ArrayList<MutationType>());
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the list of mutation types.
     * @param excludedTypes list of mutation types to purge from before 
     * returning the list.
     * @return the purged list of mutation types.
     */
    public List<MutationType> getMutationTypes(List<MutationType> excludedTypes)
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
            
            // Cannot extend vertex that has no truly free AP, but can do it
            // on APs that are used by capping groups.
            if ((getFreeAPCountThroughout() + getCappedAPs().size()) == 0)
                filteredTypes.remove(MutationType.EXTEND);
            
            // Cannot remove the only vertex of a graph
            if (owner.getVertexCount()==0)
                filteredTypes.remove(MutationType.DELETE);
            
            // Cannot start removal of chain from a branching vertex.
            long nonCap = getAttachmentPoints().size() 
                    - getCappedAPCountThroughout()
                    - getFreeAPCountThroughout();
            if (nonCap > 2)
                filteredTypes.remove(MutationType.DELETECHAIN);
        }
        
        if (getAttachmentPoints().size()-getFreeAPCountThroughout() < 2)
        {
            filteredTypes.remove(MutationType.DELETELINK);
        }
        
        if (BBType.SCAFFOLD == getBuildingBlockType())
        {
            filteredTypes.remove(MutationType.DELETECHAIN);
            filteredTypes.remove(MutationType.DELETELINK);
            filteredTypes.remove(MutationType.CHANGELINK);
            filteredTypes.remove(MutationType.CHANGEBRANCH);
            filteredTypes.remove(MutationType.DELETE);
        }
        
        filteredTypes.removeAll(excludedTypes);
        
        return filteredTypes;
    }
    
//------------------------------------------------------------------------------

    /**
     * Get attachment point i on this vertex
     * @param i index of attachment point on this vertex
     * @return attachment point i on this vertex
     */
    public AttachmentPoint getAP(int i) {
        return getAttachmentPoints().get(i);
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the position of the given AP in the list of APs of this vertex
     * @param ap the AP to find in the list of APs
     * @return the index (0-n) of <code>ap</code> or -1 if that AP does not 
     * belong to this vertex.
     */
    public int getIndexOfAP(AttachmentPoint ap)
    {
        for (int i=0; i<getAttachmentPoints().size(); i++)
        {
            AttachmentPoint candAp = getAttachmentPoints().get(i);
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

    public Edge getEdgeToParent() {
        for (AttachmentPoint ap : getAttachmentPoints())
        {
            Edge user = ap.getEdgeUser();
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

    public Vertex getParent() 
    {
        for (AttachmentPoint ap : getAttachmentPoints())
        {
            Edge user = ap.getEdgeUser();
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
     * this vertex and returns the list of attachment point on child vertexes 
     * that form an edge with any of the APs of this vertex.
     * Searches also beyond template boundaries, i.e., an AP can be free in the 
     * graph owning this vertex and be projected of the surface of the template
     * that embeds such graph, so that the apparently free AP can be used in the
     * outside of the template (a.k.a., beyond template's boundaries).
     * @return the list of APs on child vertices 
     * (can be empty list, but not null)
     */

    public ArrayList<AttachmentPoint> getAPsFromChildren() 
    {
        ArrayList<AttachmentPoint> apsOnChildren = 
                new ArrayList<AttachmentPoint>();
        for (AttachmentPoint ap : getAttachmentPoints())
        {
            Edge user = ap.getEdgeUserThroughout();
            if (user == null)
                continue;
            
            if (ap == user.getSrcAPThroughout())
            {
                apsOnChildren.add(user.getTrgAPThroughout());
            }
        }
        return apsOnChildren;
    }
    
//------------------------------------------------------------------------------

    /**
     * Looks into the edges that use any of the APs that belong to 
     * this vertex and returns the list of vertices which are target of any 
     * edge departing from this vertex. Only the directly connected children
     * are considered (no recursion). This method does cross template 
     * boundaries, thus it includes also children belonging to uprooted graph, 
     * but does not get into embedded graph at the child side, i.e., each child 
     * is the outermost recursion levels.
     * @return the list of child vertices (can be empty list, but not null)
     */

    public ArrayList<Vertex> getChildrenThroughout() 
    {
        ArrayList<Vertex> children = new ArrayList<Vertex>();
        for (AttachmentPoint ap : getAttachmentPoints())
        {
            Edge user = ap.getEdgeUserThroughout();
            if (user == null)
                continue;
            
            if (ap.isSrcInUserThroughout())
            {
                children.add(user.getTrgAP().getOwner());
            }
        }
        return children;
    }
    
//------------------------------------------------------------------------------

    /**
     * Looks into the edges that use any of the APs that belong to 
     * this vertex and returns the list of vertices which are target of any 
     * edge departing from this vertex. Only the directly connected children
     * are considered (no recursion). This method does not cross template 
     * boundaries, thus all children belong to the same graph.
     * @return the list of child vertices (can be empty list, but not null)
     */

    public ArrayList<Vertex> getChilddren() 
    {
        ArrayList<Vertex> children = new ArrayList<Vertex>();
        for (AttachmentPoint ap : getAttachmentPoints())
        {
            // NB: this is meant to NOT cross template boundaries, so that all
            // children do belong to the same graph.
            Edge user = ap.getEdgeUser();
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
    
    /**
     * Add the given key among the properties that are checked for equality when
     * comparing vertexes with the 
     * {@link Vertex#sameAs(Vertex, StringBuilder)} method.
     * @param key
     */
    public void setUniquefyingProperty(String key)
    {
        uniquefyingPropertyKeys.add(key);
    }
    
//------------------------------------------------------------------------------
    
    public Map<Object, Object> getProperties()
    {
        return properties;
    }
    
//------------------------------------------------------------------------------
    
    public boolean hasProperty(Object property)
    {
        if (properties==null)
            return false;
        
        return properties.containsKey(property);
    }
    
//------------------------------------------------------------------------------
    
    public Object getProperty(Object property)
    {
        if (properties == null)
        {
            return null;
        } else {
            return properties.get(property);
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
    
//-----------------------------------------------------------------------------
    
    public void removeProperty(Object key)
    {
        if (properties == null)
            return;
        
        properties.remove(key);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Copies all the string-based properties and properties defined in the
     * {@link Vertex#uniquefyingPropertyKeys} set.
     * @return the map of cloned properties.
     */
    protected Map<Object, Object> copyStringBasedProperties()
    {
        if (properties==null)
            return null;
        
        Map<Object, Object> copy = new HashMap<Object, Object>();
        for (Object k : properties.keySet())
        {
            Object v = properties.get(k);
            if ((k instanceof String && v instanceof String)
                    || (uniquefyingPropertyKeys!=null 
                        && uniquefyingPropertyKeys.contains(k)))
            {
                copy.put(k,v);
            }
        }
        return copy;
    }
    
//-----------------------------------------------------------------------------
    
    public void setProperties(Map<Object, Object> properties)
    {
        this.properties = properties;
    }
    
//------------------------------------------------------------------------------
    
    public static Vertex fromJson(String json)
    {
        Gson gson = DENOPTIMgson.getReader();
        return gson.fromJson(json, Vertex.class);
    }

//------------------------------------------------------------------------------
    
    /**
     * Returns the value of the vertex type. Note the returned object is 
     * independent from the object holding the information about the type of
     * this vertex.
     * @return the value of the vertex type
     */
    public VertexType getVertexType()
    {
        return VertexType.valueOf(vertexType.name());
    }
    
//------------------------------------------------------------------------------

    public static class DENOPTIMVertexDeserializer 
    implements JsonDeserializer<Vertex>
    {
        @Override
        public Vertex deserialize(JsonElement json, Type typeOfT,
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
                    Template t = Template.fromJson(
                            jsonObject.toString());
                    return t;
                }
                
                case MolecularFragment:
                {
                    Fragment f = Fragment.fromJson(
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
    public String[] getPathIDs(AttachmentPoint apA,
            AttachmentPoint apB)
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
     * Processes an {@link IAtomContainer} and builds a {@link Vertex} 
     * that is an instance of {@link Fragment}. 
     * This method does not consider any JSON definition that might be 
     * embedded in the container properties. 
     * This because this method is meant to convert an {@link IAtomContainer} 
     * with {@link AttachmentPoint}s defined in the 
     * {@link IAtomContainer}'s properties.
     * @param iac the atom containers.
     * @param bbt the type of building block.
     * @return the vertex.
     * @throws DENOPTIMException if the atom container could not be converted 
     * into a {@link Fragment}.
     */

    public static Vertex convertIACToVertex(IAtomContainer iac, 
            Vertex.BBType bbt) throws DENOPTIMException
    {
        Vertex v = new Fragment(iac, bbt);
        v.setAsRCV(v.getNumberOfAPs() == 1
                && APClass.RCAAPCLASSSET.contains(
                        v.getAttachmentPoints().get(0).getAPClass()));
        return v;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Created a {@link Vertex} from the SDF representation, i.e., from
     * an {@link IAtomContainer}. 
     * @param mol the container to parse.
     * @param reader a converted able to deserialize a vertex from a JSON string.
     * @param bbt the type of building block we want to generate.
     * @return a vertex 
     */
    
    public static Vertex parseVertexFromSDFFormat(IAtomContainer mol,
            Gson reader, BBType bbt) throws DENOPTIMException
    {
        Vertex v = null;
        try
        {
            Object json = mol.getProperty(DENOPTIMConstants.VERTEXJSONTAG);
            if (json != null) {
                v = reader.fromJson(json.toString(),Vertex.class);
            } else {
                json = mol.getProperty(DENOPTIMConstants.GRAPHJSONTAG);
                if (json != null)
                {
                    //TODO-gg get rid of this
                    StaticLogger.appLogger.log(Level.WARNING, "Attempt to "
                        + "read a vertex from "
                        + "a SDF file that contains a JSON definition of a "
                        + "graph. This should not be intentional, so we'll "
                        + "read the atom container ignoring the JSON "
                        + "definition of a graph.");
                }
                v = Vertex.convertIACToVertex(mol, bbt);
            }
        } catch (JsonSyntaxException | DENOPTIMException e)
        {
            throw new DENOPTIMException("Unable to parse IAtomContainer to "
                    + "create vertex .",e);
        }
        v.setBuildingBlockType(bbt);
        return v;
    }
    

//------------------------------------------------------------------------------
    
    /**
     * Checks if this and another vertex are directly connected by an edge 
     * within the same graph recursion level, i.e., both vertexes must belong
     * to the same graph.
     * @param other 
     * @return <code>true</code>
     */
    public boolean connectedTo(Vertex other)
    {
        return this.getParent() == other || this == other.getParent();
    }

//------------------------------------------------------------------------------
    
    /**
     * Finds the edge between this and the other vertex, if it exists.
     * @param other the vertex we expect to be linked to this vertex.
     * @return the edge or null if no connection exists between the two.
     */
    public Edge getEdgeWith(Vertex other)
    {
        if (this.owner != null && other.owner !=null
                && this.owner == other.owner)
        {
            for (AttachmentPoint ap : getAttachmentPoints())
            {
                AttachmentPoint linkedAp = ap.getLinkedAP();
                if (linkedAp == null)
                    continue;
                if (linkedAp.getOwner()==other)
                    return ap.getEdgeUser();
            }
        }
        return null;
    }
    
//------------------------------------------------------------------------------
    
}
