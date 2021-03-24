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
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;
import java.util.logging.Level;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.math3.random.MersenneTwister;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.DENOPTIMEdge.BondType;
import denoptim.utils.GraphUtils;
import denoptim.utils.MutationType;
import denoptim.utils.RandomUtils;

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
    
    //TODO-V3 remove: get it from the graph
    /*
     * if the level at which this vertex is in a graph
     */
    private int level = -99; //Initialised to meaningless value
    
    /**
     * Map of customizable properties
     */
    private Map<Object, Object> properties;

    /**
     * List of mutations that we can perform on this vertex
     */
    private Set<MutationType> allowedMutationTypes = new HashSet<MutationType>(
            Arrays.asList(MutationType.values()));

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty vertex.
     */
    public DENOPTIMVertex()
    {
        vertexId = -1;
        isRCV = false;
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for an identified vertex without attachment points.
     */
    public DENOPTIMVertex(int id)
    {
        vertexId = id;
        isRCV = false;
    }
    
  //------------------------------------------------------------------------------

    /**
     * Constructor for an identified vertex with attachment points.
     */
    public DENOPTIMVertex(int id, ArrayList<DENOPTIMAttachmentPoint> lstAPs)
    {
        this.vertexId = id;
        setAttachmentPoints(lstAPs);
        this.isRCV = false;
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for an identified vertex.
     */
    public DENOPTIMVertex(int id, ArrayList<DENOPTIMAttachmentPoint> lstAPs,
            ArrayList<SymmetricSet> lstSymAPs, boolean isRCV)
    {
        this.vertexId = id;
        setAttachmentPoints(lstAPs);
        setSymmetricAPSets(lstSymAPs);
        this.isRCV = isRCV;
    }
    
//------------------------------------------------------------------------------

    /**
     * Builds a new molecular fragment kind of vertex.
     * @param bbId 0-based index of building block in the library
     * @param bbt the type of building block 0:scaffold, 1:fragment, 
     * 2:capping group
     */
    public static DENOPTIMVertex newVertexFromLibrary(int bbId, DENOPTIMVertex.BBType bbt)
    {
        return newVertexFromLibrary(GraphUtils.getUniqueVertexIndex(),bbId,bbt);
    }
    
//------------------------------------------------------------------------------

    /**
     * Make a new vertex that is a copy of a vertex in the fragment space.
     * @param vertexId unique identified of the vertex
     * @param bbId 0-based index of building block in the library
     * @param bbt the type of building block
     */
    public static DENOPTIMVertex newVertexFromLibrary(int vertexId, int bbId, 
            DENOPTIMVertex.BBType bbt)
    {   
        // This is just to initialise the vertex. The actual type of vertex
        // returned by this method depends on the what we get from the
        // FragmentSpace.getVertexFromLibrary call
        DENOPTIMVertex v = new EmptyVertex();
        try
        {
            //NB: this returns a clone of the vertex stored in the library
            v = FragmentSpace.getVertexFromLibrary(bbt,bbId);
        } catch (DENOPTIMException e)
        {
            e.printStackTrace();
            String msg = "Fatal error! Cannot continue. " + e.getMessage();
            DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
            System.exit(0);
        }
        v.setVertexId(vertexId);
        
        v.setAsRCV(v.getNumberOfAP() == 1 
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
    
    protected void setAsRCV(boolean isRCV)
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

    protected abstract void setSymmetricAPSets(ArrayList<SymmetricSet> m_Sap);

//------------------------------------------------------------------------------

    public abstract ArrayList<SymmetricSet> getSymmetricAPSets();

//------------------------------------------------------------------------------

    /**
     * For the given attachment point index locate the symmetric partners
     * i.e. those with similar environments and class types.
     * @param m_dapidx inded of the attachment point which we want to get
     * the symmetrically related partners of.
     * @return the list of attachment point IDs, which include 
     * <code>m_dapidx</code> or <code>null</code> if no partners present
     */

    public SymmetricSet getSymmetricAPs(int m_dapidx)
    {
        for (SymmetricSet symmetricSet : getSymmetricAPSets()) 
        {
            if (symmetricSet.contains(m_dapidx)) 
            {
                return symmetricSet;
            }
        }
        return null;
    }

//------------------------------------------------------------------------------

    public int getNumberOfAP()
    {
        return getAttachmentPoints().size();
    }

//------------------------------------------------------------------------------

    /**
     *
     * @return list of attachment points that have free valences
     */

    //TODO-V3 get rid of this. Use references
    @Deprecated
    public ArrayList<Integer> getFreeAPList()
    {
        ArrayList<Integer> lstAvailableAP = new ArrayList<>();
        for (int i=0; i<getAttachmentPoints().size(); i++)
        {
            if (getAttachmentPoints().get(i).isAvailable())
                lstAvailableAP.add(i);
        }
        return lstAvailableAP;
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

    public void setLevel(int m_level)
    {
        level = m_level;
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

    //TODO-V3 this should never be called to make string representation of graph
    // So, do we keep this method?
    
    @Override
    public String toString()
    {
        return vertexId + "_ABS_VERTEX";
        //return vertexId + "_" + (buildingBlockId + 1) + "_" +
        //        buildingBlockType + "_" + recursiveLevel;
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
     * @return <code>true</code> if the two vertexes represent the same graph
     * node even if the vertex IDs are different.
     */
    public boolean sameAs(DENOPTIMVertex other, StringBuilder reason)
    {	
    	if (this.getFreeAPCount() != other.getFreeAPCount())
    	{
    		reason.append("Different number of free APs ("
    				+this.getFreeAPCount()+":"
					+other.getFreeAPCount()+"); ");
    		return false;
    	}
    	
    	if (this.getNumberOfAP() != other.getNumberOfAP())
    	{
    		reason.append("Different number of APs ("
    				+this.getNumberOfAP()+":"
					+other.getNumberOfAP()+"); ");
    		return false;
    	}
    	
    	for (DENOPTIMAttachmentPoint apT : this.getAttachmentPoints())
    	{
    		boolean found = false;
    		for (DENOPTIMAttachmentPoint apO : other.getAttachmentPoints())
        	{
		    	if (apT.equals(apO))
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

    /**
     * Connects this vertex to target by an edge based on reaction type.
     * @param target target vertex
     * @param sourceAPIndex index of Attachment point in source vertex
     * @param targetAPIndex index of Attachment point in target vertex
     * @return DENOPTIMEdge
     */
    
    //TODO-V3 also this is tmp: will be replaced once AP owner will be available

    @Deprecated
    public DENOPTIMEdge connectVertices(DENOPTIMVertex target,
                                        int sourceAPIndex,
                                        int targetAPIndex) 
    {
        DENOPTIMAttachmentPoint sourceAP = getAttachmentPoints()
                .get(sourceAPIndex);
        
        DENOPTIMAttachmentPoint targetAP = target.getAttachmentPoints()
                .get(targetAPIndex);
        
        return new DENOPTIMEdge(sourceAP,targetAP);
    }
    
//------------------------------------------------------------------------------

    /**
     * Connects this vertex to target by an edge based on reaction type.
     * @param target target vertex
     * @param sourceAPIndex index of Attachment point in source vertex
     * @param targetAPIndex index of Attachment point in target vertex
     * @param srcAPC the reaction scheme at the source
     * @param trgAPC the reaction scheme at the target
     * @return DENOPTIMEdge
     */
    
    //TODO-V3 get rid of this once edge constructor will not need all these details
    //TODO-M6 should be able to get rid of it
    @Deprecated
    public DENOPTIMEdge connectVertices(DENOPTIMVertex target,
                                        int sourceAPIndex,
                                        int targetAPIndex,
                                        APClass srcAPC,
                                        APClass trgAPC
    ) {
        //System.err.println("Connecting vertices RCN");
        DENOPTIMAttachmentPoint sourceAP = getAttachmentPoints()
                .get(sourceAPIndex);
        DENOPTIMAttachmentPoint targetAP = target.getAttachmentPoints()
                .get(targetAPIndex);

        if (sourceAP.isAvailable() && targetAP.isAvailable())
        {
            String rname = trgAPC.getRule();

            // look up the reaction bond order table
            BondType bndTyp = FragmentSpace.getBondOrderForAPClass(rname);

            // create a new edge (this also updated AP valence count)
            DENOPTIMEdge edge = new DENOPTIMEdge(sourceAP, targetAP, bndTyp);

            return edge;
        } else {
            System.err.println("ERROR! Attempt to make edge using unavailable "
                    + "APs!"
                    + System.getProperty("line.separator")
                    + "Vertex: "+getVertexId()
                    +" AP-A(available:"+sourceAP.isAvailable()+"): "+sourceAP
                    + System.getProperty("line.separator")
                    + "Vertex: "+target.getVertexId()
                    +" AP-B(available:"+targetAP.isAvailable()+"): "+targetAP);
        }
        return null;
    }

//------------------------------------------------------------------------------

    /**
     * connects this vertex to other using any randomly chosen pair of 
     * attachment points.
     * @param other vertex.
     * @return edge connecting the vertices.
     */

    //TODO-V3 test this in the non-APClass based approach
    @Deprecated
    public DENOPTIMEdge connectVertices(DENOPTIMVertex other)
    {
        ArrayList<Integer> apA = getFreeAPList();
        ArrayList<Integer> apB = other.getFreeAPList();

        if (apA.isEmpty() || apB.isEmpty())
            return null;

        // select random APs - these are the indices in the list
        MersenneTwister rng = RandomUtils.getRNG();

        // int iA = apA.get(GAParameters.getRNG().nextInt(apA.size()));
        // int iB = apB.get(GAParameters.getRNG().nextInt(apB.size()));
        int iA = apA.get(rng.nextInt(apA.size()));
        int iB = apB.get(rng.nextInt(apB.size()));

        DENOPTIMAttachmentPoint dap_A = getAttachmentPoints().get(iA);
        DENOPTIMAttachmentPoint dap_B = other.getAttachmentPoints().get(iB);

        //TODO-V3 test this if block
        
        int chosenBO = 1;
        if (dap_A.getFreeConnections()>1 && dap_B.getFreeConnections()>1)
        {
            int maxBO = Math.max(dap_A.getFreeConnections(), 
                    dap_B.getFreeConnections());
            chosenBO = rng.nextInt(maxBO-1) + 1;
        }
        
        // create a new edge
        DENOPTIMEdge edge = new DENOPTIMEdge(getAP(iA), other.getAP(iB),
                BondType.parseInt(chosenBO));
        
        return edge;
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

    public abstract Set<DENOPTIMVertex> getMutationSites();

//------------------------------------------------------------------------------

    public void setMutationTypes(Set<MutationType> set)
    {
        allowedMutationTypes = set;
    }
    
//------------------------------------------------------------------------------

    public void addMutationType(MutationType mt)
    {
        allowedMutationTypes.add(mt);
    }
    
//------------------------------------------------------------------------------

    public Set<MutationType> getMutationTypes()
    {
        return allowedMutationTypes;
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

    public void addAP() {
        addAP(0, 0, 0);
    }

//------------------------------------------------------------------------------

    /**
     * Constructor
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

    //TODO-V3. This method creates an empty APClass, which is not supposed to
    // exist. It must be possible to define an AP without an APClass!
    // This has to change!
    /**
     * Constructor
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
     * Constructor
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

    //TODO-V3: consider removal. this is just a code stub written to test the
    // possibility of having a custom serializer.
/*
    public static class DENOPTIMVertexSerializer
    implements JsonSerializer<DENOPTIMVertex> {

        @Override
        public JsonElement serialize(DENOPTIMVertex src, Type typeOfSrc,
                JsonSerializationContext context) {

            JsonObject jsonObject = new JsonObject();
            // src.owner creates a loop!
            jsonObject.addProperty("vertexId", src.getVertexId());
            jsonObject.addProperty("isRCV", src.isRCV());
            jsonObject.addProperty("level", src.getLevel());
            jsonObject.add("allowedMutatioTypes",
                context.serialize(src.getMutationTypes()));


            //JsonPrimitive jsonObject = new JsonPrimitive(src.vertexId);
            return jsonObject;
        }
    }
*/
//------------------------------------------------------------------------------

    public static class DENOPTIMVertexDeserializer 
    implements JsonDeserializer<DENOPTIMVertex>
    {
        @Override
        public DENOPTIMVertex deserialize(JsonElement json, Type typeOfT,
                JsonDeserializationContext context) throws JsonParseException
        {
            JsonObject jsonObject = json.getAsJsonObject();

            // Desiralization differs for the types of vertexes
            // First, consider templates
            if (jsonObject.has("innerGraph"))
            {
                //TODO-V3 log or del
                System.out.println("DESERIALIZE Template "
                        + jsonObject.get("vertexId"));

                DENOPTIMTemplate tmpl = context.deserialize(jsonObject,
                        DENOPTIMTemplate.class);

                // Deserialize embedded graph. Such field is excluded by design
                // because if not, the above tmpl = context.deserialize(...)
                // call produces an inner graph with null AP pointers.

                JsonObject innerGraphJson = jsonObject.getAsJsonObject(
                        "innerGraph");
                DENOPTIMGraph innerGraph = DENOPTIMGraph.fromJson(
                        innerGraphJson.toString());
                tmpl.setInnerGraph(innerGraph);

                // Is the following needed? I think so, because we need the IDs
                // of outernAPs to be as defined in the json string.

                //Recover innerToOuter APMap
                Type type = new TypeToken<TreeMap<Integer,
                        DENOPTIMAttachmentPoint>>(){}.getType();
                TreeMap<Integer,DENOPTIMAttachmentPoint> map =
                        context.deserialize(jsonObject.getAsJsonObject(
                                "innerToOuterAPs"), type);
                tmpl.updateInnerToOuter(map);

                return tmpl;
            }
            // Then, molecular fragments
            else if (jsonObject.has("fragmentType"))
            {
                //TODO-V3 log or del
                System.out.println("DESERIALIZE Fragment "
                        + jsonObject.get("vertexId"));

                DENOPTIMVertex v = null;

                //TODO-V3?: serialize AtomContainer2 somehow (as an SDF string?)

                // The serialized fragment does NOT include its molecular
                // representation, which cannot be serialized (so far...)
                DENOPTIMFragment frag = context.deserialize(jsonObject,
                        DENOPTIMFragment.class);
                v = frag;

                // The above has these issues:
                // - mol in null
                // - AP user/owner is null (fixed in graph deserializatrion)
                // - APClass has wrong reference to new class (fixed below)

                if (FragmentSpace.isDefined())
                {
                    // If a fragment space exists, we rebuild the fragment from the
                    // library, and attach to it the serialized data
                    DENOPTIMVertex fragWithMol;
                    try
                    {
                        // NB: this works well also with templates that are in the library
                        // This is, de facto, using the old index-based way to get
                        // the fragment. However, it is so far the only way to
                        // rebuild a fragment with its molecular representation
                        fragWithMol = FragmentSpace.getVertexFromLibrary(
                                frag.getBuildingBlockType(), frag.getBuildingBlockId());
                    } catch (DENOPTIMException e)
                    {
                        throw new JsonParseException("Could not get "
                                + frag.getBuildingBlockType() + " " + frag.getBuildingBlockId()
                                + " from "
                                + "library. " + e.getMessage());
                    }
                    ArrayList<SymmetricSet> cLstSymAPs = new ArrayList<SymmetricSet>();
                    for (SymmetricSet ss : frag.getSymmetricAPSets())
                    {
                        cLstSymAPs.add(ss.clone());
                    }
                    fragWithMol.setMutationTypes(frag.getMutationTypes());
                    fragWithMol.setSymmetricAPSets(cLstSymAPs);
                    fragWithMol.setAsRCV(frag.isRCV());
                    fragWithMol.setVertexId(frag.getVertexId());
                    fragWithMol.setLevel(frag.getLevel());
                    for (int iap=0; iap<frag.getNumberOfAP(); iap++)
                    {
                        DENOPTIMAttachmentPoint oriAP = frag.getAP(iap);
                        DENOPTIMAttachmentPoint newAP = fragWithMol.getAP(iap);
                        newAP.setID(oriAP.getID());
                    }
                    v = fragWithMol;
                } else {
                    System.err.println("WARNING: undefunded fragment space. "
                            + "Templates will contain fragments with no "
                            + "molecular representation. To avoid this, first "
                            + "define the fragment space, and then work with "
                            + "templates.");
                }

                // Fix the reference to unique APClass
                for (DENOPTIMAttachmentPoint ap : v.getAttachmentPoints())
                {
                    try
                    {
                        //TODO: should this be done in an APClassDeserializer?
                        ap.setAPClass(ap.getAPClass().toString());
                    } catch (DENOPTIMException e1)
                    {
                        throw new JsonParseException(e1);
                    }
                }

                // WARNING: other fields, such as 'owner' and AP 'user' are
                // recovered upon deserializing the graph containing this vertex

                return v;
            }
            // Finally, vertexes that are not "molecular" (empty vertex)
            else
            {
                //TODO-V3 log or del
                System.out.println("DESERIALIZE EmptyVertex "
                        + jsonObject.get("vertexId"));
                EmptyVertex ev = EmptyVertex.fromJson(jsonObject.toString());
                return ev;
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
        String a2b = this.getBuildingBlockId() + "/" + this.getBuildingBlockType() + "/ap"
                + getIndexOfAP(apA) + "ap" + getIndexOfAP(apB) + "_";
        String b2a = this.getBuildingBlockId() + "/" + this.getBuildingBlockType() + "/ap"
                + getIndexOfAP(apB) + "ap" + getIndexOfAP(apA) + "_";
        
        String[] pair = {a2b,b2a};
        return pair;
    }
    
//------------------------------------------------------------------------------

    /**
     * Processes an atom containers and builds a vertex out of it.
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
            EmptyVertex ev = EmptyVertex.fromJson(jsonVertex.toString());
            v = ev;
        } else {
            v = new DENOPTIMFragment(iac,bbt);
        }
        
        v.setAsRCV(v.getNumberOfAP() == 1 
                && APClass.RCAAPCLASSSET.contains(
                        v.getAttachmentPoints().get(0).getAPClass()));
        
        return v;
    }
    
//------------------------------------------------------------------------------
    
}
