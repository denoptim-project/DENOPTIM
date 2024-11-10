package denoptim.graph;

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
import java.util.List;
import java.util.logging.Logger;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;

import com.google.gson.Gson;

import denoptim.constants.DENOPTIMConstants;
import denoptim.json.DENOPTIMgson;
import denoptim.utils.GraphUtils;
import denoptim.utils.MutationType;
import denoptim.utils.Randomizer;


/**
 * An empty vertex has the behaviors of a vertex, but has no molecular 
 * structure. 
 * It has attachment points, as well as relations between those
 * attachment points, but has no atoms.
 *
 */
public class EmptyVertex extends Vertex
{
    /**
     * Attachment points on this vertex
     */
    private List<AttachmentPoint> lstAPs;

    /**
     * List of AP sets that are related to each other, so that we
     * call them "symmetric" (though symmetry is a fuzzy concept here).
     */
    private List<SymmetricAPs> lstSymAPs;

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty vertex.
     */
    public EmptyVertex()
    {
        super(VertexType.EmptyVertex, GraphUtils.getUniqueVertexIndex());
        lstAPs = new ArrayList<AttachmentPoint>();
        lstSymAPs = new ArrayList<SymmetricAPs>();
    }

//------------------------------------------------------------------------------

      /**
       * Constructor for an identified vertex without attachment points.
       * @param id the VertedID of the vertex to construct. Note that this ID 
       * should be unique within a graph. To generate unique IDs either use 
       * {@link GraphUtils#getUniqueVertexIndex()} or use constructor
       * {@link EmptyVertex()}.
       */
      public EmptyVertex(BBType type)
      {
          super(VertexType.EmptyVertex);
          lstAPs = new ArrayList<AttachmentPoint>();
          lstSymAPs = new ArrayList<SymmetricAPs>();
          buildingBlockType = type;
      }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for an identified vertex without attachment points.
     * @param id the VertedID of the vertex to construct. Note that this ID 
     * should be unique within a graph. To generate unique IDs either use 
     * {@link GraphUtils#getUniqueVertexIndex()} or use constructor
     * {@link EmptyVertex()}.
     */
    public EmptyVertex(long id)
    {
        super(VertexType.EmptyVertex, id);
        lstAPs = new ArrayList<AttachmentPoint>();
        lstSymAPs = new ArrayList<SymmetricAPs>();
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
    public EmptyVertex(long id, ArrayList<AttachmentPoint> lstAPs,
            ArrayList<SymmetricAPs> lstSymAPs, boolean isRCV)
    {
        super(VertexType.EmptyVertex, id);
        this.lstAPs = lstAPs;
        this.lstSymAPs = lstSymAPs;
        setAsRCV(isRCV);
    }

//------------------------------------------------------------------------------

    public List<AttachmentPoint> getAttachmentPoints()
    {
        return lstAPs;
    }

//------------------------------------------------------------------------------

    public void setSymmetricAP(List<SymmetricAPs> sAPs)
    {
        lstSymAPs = sAPs;
    }

//------------------------------------------------------------------------------

    public List<SymmetricAPs> getSymmetricAP()
    {
        return lstSymAPs;
    }

//------------------------------------------------------------------------------

    /**
     * For the given attachment point index locate the symmetric partners
     * i.e. those with similar environments and class types.
     * @param ap index of the attachment point which we want to get
     * the symmetrically related partners of.
     * @return the list of attachment point IDs, which include
     * <code>apIdx</code> or <code>null</code> if no partners present
     */

    @Override
    public SymmetricAPs getSymmetricAPs(AttachmentPoint ap)
    {
        for (SymmetricAPs symmetricSet : lstSymAPs)
        {
            if (symmetricSet.contains(ap))
            {
                return symmetricSet;
            }
        }
        return new SymmetricAPs();
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
        for (AttachmentPoint denoptimAttachmentPoint : lstAPs) {
            if (denoptimAttachmentPoint.isAvailable())
                n++;
        }
        return n;
    }

//------------------------------------------------------------------------------

    public boolean hasFreeAP()
    {
        for (AttachmentPoint denoptimAttachmentPoint : lstAPs) {
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
        c.setAsRCV(this.isRCV());
        c.setBuildingBlockId(this.getBuildingBlockId());
        c.setBuildingBlockType(this.getBuildingBlockType());
        
        c.setMutationTypes(this.getUnfilteredMutationTypes());
        
        for (AttachmentPoint ap : lstAPs)
        {
            AttachmentPoint cAp = new AttachmentPoint(c, -1, null, 
                    ap.getAPClass());
            cAp.setCutId(ap.getCutId());
            cAp.setID(ap.getID());
            c.lstAPs.add(cAp);
        }

        List<SymmetricAPs> cLstSymAPs = new ArrayList<SymmetricAPs>();
        for (SymmetricAPs symAPs : lstSymAPs)
        {
            SymmetricAPs cSymAPs = new SymmetricAPs();
            for (AttachmentPoint ap : symAPs)
            {
                cSymAPs.add(c.getAP(ap.getIndexInOwner()));
            }
            cLstSymAPs.add(cSymAPs);
        }
        
        c.setSymmetricAPSets(cLstSymAPs);
        c.setProperties(this.copyStringBasedProperties());
        if (uniquefyingPropertyKeys!=null)
            c.uniquefyingPropertyKeys.addAll(uniquefyingPropertyKeys);
        return c;
    }
    
//------------------------------------------------------------------------------

    /**
     * Adds an attachment point with no {@link APClass} or other attribute.
     * This is used only for testing purposes.
     */
    public void addAP() {
        AttachmentPoint ap = new AttachmentPoint(this);
        getAttachmentPoints().add(ap);
    }
      
//------------------------------------------------------------------------------

      /**
       * Adds an attachment point with the specified {@link APClass}.
       * @param apClass the APClass
       */
      public void addAP(APClass apClass) {
          AttachmentPoint ap = new AttachmentPoint(this,
                  -1, null, apClass);
          getAttachmentPoints().add(ap);
      }
      
//------------------------------------------------------------------------------

    /**
     * Compares this and another vertex ignoring vertex IDs. A difference in 
     * any property that has been marked as an uniquefying property with the
     * {} method makes this method return false.
     * @param other
     * @param reason string builder used to build the message clarifying the
     * reason for returning <code>false</code>.
     * @return <code>true</code> if the two vertices represent the same graph
     * node even if the vertex IDs are different.
     */

    public boolean sameAs(EmptyVertex other, StringBuilder reason)
    {
        if (this.uniquefyingPropertyKeys.size()!=0
                || other.uniquefyingPropertyKeys.size()!=0)
        {
            if (!this.uniquefyingPropertyKeys.equals(other.uniquefyingPropertyKeys))
                return false;
            
            for (String k : this.uniquefyingPropertyKeys)
            {
                if (!this.properties.get(k).equals(other.properties.get(k)))
                    return false;
            }
        }
        return sameVertexFeatures(other, reason);
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
     * @return an empty atom container that contains the definition of the
     * vertex in the JSON-formatted property.
     */
    
    @Override
    public IAtomContainer getIAtomContainer()
    {
        IAtomContainer iac = new AtomContainer();
        iac.setProperty(DENOPTIMConstants.APSTAG, "");
        iac.setProperty(DENOPTIMConstants.VERTEXJSONTAG,this.toJson());
        
        return iac;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Although empty vertex do not contain atoms, by definitions, we allow
     * the generation of an SDF representation that uses an empty atom list.
     * However, this method is ignoring all the parameters and calling
     * {@link #getIAtomContainer()}
     */
    @Override
    public IAtomContainer getIAtomContainer(Logger logger, 
            Randomizer rng, boolean removeUsedRCAs, boolean rebuild)
    {
        return getIAtomContainer();
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
        for (AttachmentPoint ap : ev.getAttachmentPoints())
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
        for (AttachmentPoint ap : lstAPs)
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
    protected void setSymmetricAPSets(List<SymmetricAPs> sAPs)
    {
        this.lstSymAPs = sAPs;
    }
    
//-----------------------------------------------------------------------------

    @Override
    protected void addSymmetricAPSet(SymmetricAPs symAPs)
    {
        if (this.lstSymAPs==null)
        {
            this.lstSymAPs = new ArrayList<SymmetricAPs>();
        }
        this.lstSymAPs.add(symAPs);
    }

//-----------------------------------------------------------------------------

    @Override
    public List<SymmetricAPs> getSymmetricAPSets()
    {
        return lstSymAPs;
    }

//------------------------------------------------------------------------------

    /**
     * A list of mutation sites from within this vertex.
     * @param ignoredTypes a collection of mutation types to ignore. vertices
     * that allow only ignored types of mutation will
     * not be considered mutation sites.
     * @return the list of vertices that allow any non-ignored mutation type.
     */
    
    @Override
    public List<Vertex> getMutationSites(List<MutationType> ignoredTypes)
    {
        List<Vertex> lst = new ArrayList<Vertex>();
        switch (getBuildingBlockType())
        {
            case CAP:
                break;
                
            case SCAFFOLD:
                break;
                
            default:
                if (getMutationTypes(ignoredTypes).size()>0)
                    lst.add(this);
                break;
        }
        return lst;
    }
    
//------------------------------------------------------------------------------

}
