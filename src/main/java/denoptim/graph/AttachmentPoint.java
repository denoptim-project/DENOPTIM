/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no>
 *   and Marco Foscato <marco.foscato@uib.no>
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

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.vecmath.Point3d;

import org.openscience.cdk.interfaces.IAtom;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.graph.Edge.BondType;
import denoptim.utils.GraphUtils;

/**
 * An attachment point (AP) is a possibility to attach a {@link Vertex} 
 * onto the vertex
 * holding the AP (i.e., the owner of the AP), this way forming a new 
 * {@link Edge} (i.e., the user of the AP).
 * It can be annotated with the index (position) of an atom
 * in the molecule, which is called the attachment point's source atom,
 * three-dimensional information in the form of Cartesian coordinates,
 * and information on the kind of attachment point in terms of {@link APClass}.
 * 
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */


public class AttachmentPoint implements Cloneable, 
    Comparable<AttachmentPoint>
{
    /**
     * Index used to keep the order in a list of attachment points
     */
    private int id;
    
	/**
	 * The index of the source atom in the atom list of the fragment (0-based)
	 */
    private int atomPositionNumber = -1;
    
    /**
     * The index of the source atom in the atom list of the entire molecule 
     * (0-based)
     */
    private int atomPositionNumberInMol = -1;
    
    /**
     * The attachment point class
     */
    private APClass apClass;
    
    /**
     * The direction vector representing the bond direction
     */
    private Point3d dirVec;

    /**
     * The vertex to which this AP is attached to.
     */
    private Vertex owner;
    
    /**
     * The edge that is using this AP, if any
     */
    private Edge user;
    
    /**
     * Map of customisable properties
     */
    private Map<Object, Object> properties;

//------------------------------------------------------------------------------

    /**
     * Constructor for undefined DENOPTIMAttachmentPoint
     * @param owner the vertex that holds the attachment point under creation.
     */
    
    //TODO: since APs can be on any vertex, and vertexes are not required to
    // contain atoms, the information of which atom is an AP rooted should be
    // stored and managed by the implementation of vertex that do contain atoms.
    // The DENOPTIMFragment should thus be charged with keeping the reference to
    // the atom that holds the AP.
    
    public AttachmentPoint(Vertex owner) 
    {
        this.owner = owner;
        id = GraphUtils.getUniqueAPIndex();
    }

//------------------------------------------------------------------------------

    /**
     * Constructor
     * @param owner the vertex that holds the attachment point under creation.
     * @param atomPositionNumber the index of the source atom (0-based)
     */
    public AttachmentPoint(Vertex owner, int atomPositionNumber) 
    {
        this(owner);
        this.atomPositionNumber = atomPositionNumber;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Constructor
     * @param owner the vertex that holds the attachment point under creation.
     * @param atomPositionNumber the index of the source atom (0-based)
     * @param dirVec the AP direction vector end (the beginning are the
     * coords of the source atom). This array must have 3 entries.
     */
    private AttachmentPoint(Vertex owner, int atomPositionNumber,
            Point3d dirVec) 
    {
        this(owner, atomPositionNumber);
        if (dirVec != null)
        {
            this.dirVec = new Point3d(dirVec.x, dirVec.y, dirVec.z);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Constructor
     * @param owner the vertex that holds the attachment point under creation.
     * @param atomPosNum the index of the source atom (0-based)
     * @param apClass the APClass
     */
    public AttachmentPoint(Vertex owner, int atomPosNum,
                                   APClass apClass) {
        this(owner, atomPosNum, null, apClass);
    }

    
//------------------------------------------------------------------------------
    
    /**
     * Constructor
     * @param owner the vertex that holds the attachment point under creation.
     * @param atomPosNum the index of the source atom (0-based)
     * @param dirVec the AP direction vector end (the beginning at the 
     * coordinates of the source atom). This must array have 3 entries.
     * @param apClass the APClass
     */
    public AttachmentPoint(Vertex owner, int atomPosNum, 
            Point3d dirVec, APClass apClass) 
    {
        this(owner, atomPosNum, dirVec);
        this.apClass = apClass;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Construct an attachment point based on the formatted string 
     * representation. The format is the one used in SDF files.
     * @param owner the vertex this AP is owned by.
     * @param str the formatted string.
     * @throws DENOPTIMException
     */
    public AttachmentPoint(Vertex owner, String str)
            throws DENOPTIMException
    {
        this(owner);
        processSdfString(str);
    }
 
//-----------------------------------------------------------------------------
        
    private void processSdfString(String str) throws DENOPTIMException
    {
        try 
        {
            String[] parts = str.split(
                    Pattern.quote(DENOPTIMConstants.SEPARATORAPPROPAAP));
            
            //WARNING here we convert from 1-based to 0-based index
            this.atomPositionNumber = Integer.parseInt(parts[0])-1;

            String[] details = parts[1].split(
                    Pattern.quote(DENOPTIMConstants.SEPARATORAPPROPSCL));
            switch (details.length)
            {
                case 2:
                {
                    //OK, APClass:subclass but no direction vector and no bnd type
                    this.apClass = APClass.make(details[0],Integer.parseInt(details[1]));
                    
                    String[] coord = details[2].split(
                            Pattern.quote(DENOPTIMConstants.SEPARATORAPPROPXYZ)); 
                    
                    if (coord.length == 3)
                    {
                        this.dirVec = new Point3d(Double.parseDouble(coord[0]),
                                Double.parseDouble(coord[1]),
                                Double.parseDouble(coord[2]));
                    }
                    break;
                }
                    
                case 3:
                {
                    //OK, APClass:subclass:direction_vector
                    this.apClass = APClass.make(details[0],Integer.parseInt(details[1]));
                    
                    String[] coord = details[2].split(
                            Pattern.quote(DENOPTIMConstants.SEPARATORAPPROPXYZ)); 
                    
                    if (coord.length == 3)
                    {
                        this.dirVec = new Point3d(Double.parseDouble(coord[0]),
                                Double.parseDouble(coord[1]),
                                Double.parseDouble(coord[2]));
                    }
                    break;
                }
                    
                case 4:
                {
                    //OK, new format that includes bond type
                    this.apClass = APClass.make(details[0],
                            Integer.parseInt(details[1]),
                            BondType.valueOf(details[2]));
                    
                    String[] coord = details[3].split(
                            Pattern.quote(DENOPTIMConstants.SEPARATORAPPROPXYZ)); 
                    
                    if (coord.length == 3)
                    {
                        this.dirVec = new Point3d(Double.parseDouble(coord[0]),
                                Double.parseDouble(coord[1]),
                                Double.parseDouble(coord[2]));
                    }
                    break;
                }
                    
                default:
                    throw new DENOPTIMException("Unable to split APClass, "
                            + "subclass, and coordinates");
            }
        } catch (Throwable t) {
            t.printStackTrace();
            throw new DENOPTIMException("Cannot construct AP from string '" 
                        + str + "'",t);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Returns a unique integer that is used to sort list of attachment points.
     * This index follows the order in which attachment points were generated.
     */
    public int getID()
    {
        return id;
    }
    
//------------------------------------------------------------------------------

    /**
     * Sets the unique integer that is used to sort list of attachment points.
     * <b>WARNING</b>: we do not check for uniqueness of the given value.
     */
    public void setID(int id)
    {
        this.id = id;
    }

//------------------------------------------------------------------------------

    /**
     * The index of the source atom in the atom list of the fragment. 
     * The index is reported considering 0-based enumeration.
     * @return the index of the source atom in the atom list of the fragment
     */
    public int getAtomPositionNumber()
    {
        if (owner!=null && owner instanceof EmptyVertex)
            return -1;
        return atomPositionNumber;
    }

//------------------------------------------------------------------------------

    /**
     * Set the index of the source atom in the list of atoms of the fragment.
     * The index should reflect 0-based enumeration.
     * @param atomPositionNumber the index
     */
    public void setAtomPositionNumber(int atomPositionNumber)
    {
        this.atomPositionNumber = atomPositionNumber;
    }
    
//------------------------------------------------------------------------------

    /**
     * The index of the source atom in the atom list of the entire molecule. 
     * The index is reported considering 0-based enumeration.
     * @return the index of the source atom in the atom list of the entire
     * molecule
     */
    public int getAtomPositionNumberInMol()
    {
        if (owner!=null && owner instanceof EmptyVertex)
            return -1;
        return atomPositionNumberInMol;
    }

//------------------------------------------------------------------------------

    /**
     * Set the index of the source atom in the list of atoms of the entire 
     * molecule.
     * The index should reflect 0-based enumeration.
     * @param atomPositionNumberInMol the index
     */
    public void setAtomPositionNumberInMol(int atomPositionNumberInMol)
    {
        this.atomPositionNumberInMol = atomPositionNumberInMol;
    }

//------------------------------------------------------------------------------
    
    /**
     * Returns the bond type preferred by this attachment point as defined by 
     * the {@link APClass}, or null if {@link APClass} is null.
     * @return the bond type preferred by this attachment point as defined by 
     * the {@link APClass}, or null if {@link APClass} is null.
     */
    public BondType getBondType()
    {
        if (apClass == null)
            return APClass.DEFAULTBT;
        else
            return apClass.getBondType();
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Sets the end of the 3D vector defining the direction of the AP in 3D.
     * Note that the source of this vector is the source atom, and that the 
     * entries of <code>dirVec</code> are referred to the same origin as
     * the coordinates of the source atom.
     * @param dirVec the coordinates of the 3D point defining the end of the
     * direction vector.
     */
    public void setDirectionVector(Point3d dirVec)
    {
        if (dirVec == null)
            this.dirVec = null;
        else
            this.dirVec = new Point3d(dirVec.x, dirVec.y, dirVec.z);
    }

//------------------------------------------------------------------------------

    /**
     * Set the Attachment Point class. The APClass is the combination of a main
     * class (or "rule") and a subclass (or direction).  
     * @param apClass the new APClass.
     */
    
    public void setAPClass(String apClass) throws DENOPTIMException
    {
        this.apClass = APClass.make(apClass);
    }
    
//------------------------------------------------------------------------------

    /**
     * Set the Attachment Point class. 
     * @param apClass the new APClass.
     */
    
    public void setAPClass(APClass apClass)
    {
        this.apClass = apClass;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the Attachment Point class.
     * @return the APClass or null.
     */
    public APClass getAPClass()
    {
        return apClass;
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the end of the direction vector. The coordinates are referred to
     * the same reference system of the fragment holding this AP.
     * @return the direction vector.
     */
    public Point3d getDirectionVector()
    {
        return dirVec;
    }

//------------------------------------------------------------------------------

    /**
     * Check availability of this attachment point. Does not account for
     * embedding of the vertex in a template, i.e., this AP can be available
     * in the graph owning the vertex this AP belongs to, but if the graph is 
     * itself the inner graph of a template, the AP might be projected on the
     * template's surface and used to make an edge at that level. To account for
     * such possibility use 
     * {@link AttachmentPoint#isAvailableThroughout()}
     * @return <code>true</code> if the attachment point has no user.
     */

    public boolean isAvailable()
    {
        return user == null;
    }
    
//------------------------------------------------------------------------------

    /**
     * Check availability of this attachment point throughout the graph level, 
     * i.e., check also across the inner graph template boundary. 
     * This method does account for
     * embedding of the vertex in a template, i.e., this AP can be available
     * in the graph owning the vertex this AP belongs to, but if the graph is 
     * itself the inner graph of a template, the AP is then projected on the
     * template's surface and used to make an edge that uses the template as a 
     * single vertex. To ignore this possibility and consider only edges
     * that belong to the graph owning the AP's vertex, use 
     * {@link AttachmentPoint#isAvailable()}.
     * @return <code>true</code> if the attachment point has no user.
     */

    public boolean isAvailableThroughout()
    {
        if (user == null)
        {
            if (owner != null && owner.getGraphOwner() != null 
                    && owner.getGraphOwner().templateJacket != null)
            {
                AttachmentPoint apProjOnTemplateSurface =
                        owner.getGraphOwner().templateJacket
                        .getOuterAPFromInnerAP(this);
                if (apProjOnTemplateSurface != null)
                {
                    return apProjOnTemplateSurface.isAvailableThroughout();
                } else {
                    throw new IllegalStateException("Available AP inside a"
                            + "template should have a projection on the "
                            + "surface of the template, "
                            + "but it has none. "
                            + "Please, report this to the authors.");
                }
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

//------------------------------------------------------------------------------

    /**
     * Checks if this attachment point respects the APClass-based approach.
     * @return <code>true</code> if an APClass is defined.
     */
    public boolean isClassEnabled()
    {
        return apClass != null;
    }
    
//------------------------------------------------------------------------------

    /**
     * Compares this and another attachment points based on their unique 
     * identifier. This is done to retain the order of attachment points when
     * regenerating AP lists multiple times.
     * @param other the attachment point to compare this with.
     * @return an integer that can be used by a comparator
     */
    @Override
    public int compareTo(AttachmentPoint other)
    {
        return Integer.compare(this.getID(), other.getID());
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Compares this and given attachment point. This method defines how
     * DENOPTIMAttachmentPoint are sorted not by natural order, but by 
     * consistency of properties.
     * @param other
     * @return an integer that can be used by a comparator.
     */
    public int comparePropertiesTo(AttachmentPoint other)
    {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        if (this == other)
            return EQUAL;
        
        if (this.sameAs(other))
            return EQUAL;
        
        //Compare source atom
        if (this.getAtomPositionNumber() < other.getAtomPositionNumber())
            return BEFORE;
        else if (this.getAtomPositionNumber() > other.getAtomPositionNumber())
            return AFTER;

        //Compare APClass
        if (this.getAPClass()!=null && other.getAPClass()!=null)
        {
            int res = this.apClass.compareTo(other.apClass);
            if (res != 0)
                return res;
        }
        
        //Compare Direction Vector if AtomID is equal
        if (this.getDirectionVector() != null 
                && other.getDirectionVector() != null)
        {
            Point3d thisVec = this.getDirectionVector();
            Point3d otherVec = other.getDirectionVector();
    
            if (thisVec.x < otherVec.x)
            {
                return BEFORE;
            } else if (thisVec.x > otherVec.x)
                return AFTER;
    
            if (thisVec.y < otherVec.y)
            {
                return BEFORE;
                
            } else if (thisVec.y > otherVec.y)
                return AFTER;
            
            if (thisVec.z < otherVec.z)
            {
                return BEFORE;
            } else if (thisVec.z > otherVec.z)
                return AFTER;
        }
        else
        {
            if (this.getDirectionVector() != null)
            {
                return AFTER;
            }
            else
            {
                if (other.getDirectionVector() != null)
                {
                    return BEFORE;
                }
            }
        }
        
        // We should have returned already
        System.err.println("DENOPTIMAttachmentPoint.comparePropertiesTo "
                + "inconsistent with sameAs. Please report this bug.");
        
        return EQUAL;
    }

//------------------------------------------------------------------------------

    /**
     * Compares the features of this and another attachment point and decides if
     * the two are same. 
     * Does not check hashcodes, so this is not an <code>equals</code> method.
     * @param other AP to compare with this AP.
     * @return <code>true</code> is the two APs have the same features.
     */
    public boolean sameAs(AttachmentPoint other)
    {
        return sameAs(other, new StringBuilder());
    }
    
//------------------------------------------------------------------------------

    /**
     * Compares the features of this and another attachment point and decides if
     * the two are same. 
     * Does not check hashcodes, so this is not an <code>equals</code> method.
     * @param other AP to compare with this AP.
     * @param reason string builder used to build the message clarifying the 
     * reason for returning <code>false</code>.
     * @return <code>true</code> if the two APs have the same features.
     */
    public boolean sameAs(AttachmentPoint other, StringBuilder reason)
    {
        if (this.getAtomPositionNumber() != other.getAtomPositionNumber())
        {
            reason.append("Different source atom for APs ("
                    + this.getAtomPositionNumber() + ","
                    + other.getAtomPositionNumber() + ");");
            return false;
        }
        
        if (this.getIndexInOwner() != other.getIndexInOwner())
        {
            reason.append("Different index on list of APs ("
                    + this.getIndexInOwner() + ","
                    + other.getIndexInOwner() + ");");
            return false;
        }
        
        if (this.getAPClass()!=null && other.getAPClass()!=null)
        {
            if (!this.getAPClass().equals(other.getAPClass()))
            {
                reason.append("Different APClass ("
                        + this.getAPClass() + ","
                        + other.getAPClass() + ");");
                return false;
            }
        }
        
        return true;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns a deep clone of this attachment point
     * @return a deep clone
     */
    
    public AttachmentPoint clone()
    {   
        if (apClass == null)
        {
            if (dirVec == null)
            {
                return new AttachmentPoint(
                        getOwner(),
                        atomPositionNumber
                );
            } else {
                return new AttachmentPoint(
                        getOwner(),
                        atomPositionNumber,
                        dirVec
                );
            }
        } else {
            if (dirVec == null) {
                return new AttachmentPoint(
                        getOwner(),
                        atomPositionNumber,
                        apClass.clone()
                );
            }
            return new AttachmentPoint(
                    getOwner(),
                    atomPositionNumber,
                    dirVec,
                    apClass.clone());
        }
    }

//------------------------------------------------------------------------------

    /**
     * Prepare a string for writing this AP in a fragment SDF file.
     * Only DENOPTIM's format is currently supported and we assume three 
     * coordinates are used to define the direction vector.
     * @param isFirst use <code>true</code> if this is the first AP among those
     * on the same source atom. When you give <code>false</code> the atom ID and
     * the first separator are omitted.
     * @param srcAtmID the index to use to identify the source atom. Use this to
     *  write something different from what reported in 
     *  {@Link DENOPTIMAttachmentPoint#atomPositionNumber}, or use negative
     *  value to ignore this argument.
     * @returns a string with APClass and coordinated of the AP direction vector
     **/

    public String getSingleAPStringSDF(boolean isFirst, int srcAtmID)
    {
        StringBuilder sb = new StringBuilder();
        if (isFirst)
        {
            //WARNING! In the mol.property we use 1-to-n+1 instead of 0-to-n
            int atmIdx = atomPositionNumber + 1;
            if (srcAtmID>0)
            {
                atmIdx = srcAtmID;
            }
           
            sb.append(atmIdx);
            sb.append(DENOPTIMConstants.SEPARATORAPPROPAAP);
        }
        sb.append(apClass.toSDFString());
        if (dirVec != null)
        {
            DecimalFormat digits = new DecimalFormat("###.####");
            digits.setMinimumFractionDigits(4);
            sb.append(DENOPTIMConstants.SEPARATORAPPROPSCL);
            sb.append(digits.format(dirVec.x));
            sb.append(DENOPTIMConstants.SEPARATORAPPROPXYZ);
            sb.append(digits.format(dirVec.y));
            sb.append(DENOPTIMConstants.SEPARATORAPPROPXYZ);
            sb.append(digits.format(dirVec.z));
        }
        return sb.toString();
    }
    
//------------------------------------------------------------------------------

    /**
     * Prepare a string for writing this AP in a fragment SDF file.
     * Only DENOPTIM's format is currently supported and we assume three 
     * coordinates are used to define the direction vector.
     * @param isFirst set <code>true</code> is this is the first AP among those
     * on the same source atom. When you give <code>false</code> the atom ID and
     * the first separator are omitted.
     * @returns a string with APClass and coordinated of the AP direction vector
     **/

    public String getSingleAPStringSDF(boolean isFirst)
    {
        return getSingleAPStringSDF(isFirst, -1);
    }
    
//------------------------------------------------------------------------------

    /**
     * Produces a string with the information included in this object.
     * @return the string
     */
    
    public String toStringNoId()
    {
        Map<String,Object> pars = new HashMap<String,Object>();
        pars.put("atomPositionNumber", atomPositionNumber);
        if (apClass != null)
        {
            pars.put("apClass",apClass);
        }
        if (dirVec != null)
        {
            pars.put("dirVec.x",dirVec.x);
            pars.put("dirVec.y",dirVec.y);
            pars.put("dirVec.z",dirVec.z);
        }

        return pars.toString();
    }

//------------------------------------------------------------------------------

    /**
     * Produces a string with the information included in this object.
     * @return the string
     */
    
    @Override
    public String toString()
    {
        Map<String,Object> pars = new HashMap<String,Object>();
        pars.put("atomPositionNumber", atomPositionNumber);
        pars.put("id", id);
        if (apClass != null)
        {
            pars.put("apClass",apClass);
        }
        if (dirVec != null)
        {
            pars.put("dirVec.x",dirVec.x);
            pars.put("dirVec.y",dirVec.y);
            pars.put("dirVec.z",dirVec.z);
        }

        return pars.toString();
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Sets the reference to the vertex that owns this attachment point. 
     * This method should be used with caution as it has the capability of
     * altering graphs!
     * @param owner the vertex that own this attachment point.
     */
    public void setOwner(Vertex owner) {
        this.owner = owner;
    }
    
//-----------------------------------------------------------------------------

    public Vertex getOwner() {
        return owner;
    }

//-----------------------------------------------------------------------------
    
    /**
     * Sets the reference to the edge that is using this attachment point.
     * @param edge the user
     */
    public void setUser(Edge edge) {
        this.user = edge;
    }

//-----------------------------------------------------------------------------
    
    /**
     * Gets the edge that is using this AP, or null if no edge is using this AP.
     * Does NOT account for embedding of the vertex in a template, i.e., 
     * this AP can be available in the graph (if any) owning the vertex this AP 
     * belongs to, but if the graph is 
     * itself the inner graph of a template, the AP is then projected on the
     * template's surface and might be used to make an edge that uses the 
     * template as a single vertex. This method considers only any edge user
     * that belongs to the graph owning the vertex that own this AP, if any such
     * owners exist. See {@link AttachmentPoint#getEdgeUserThroughout()}
     * to crossing the template boundary.
     * @return the edge that is using this AP.
     */
    public Edge getEdgeUser() {
        return user;
    }
    
//------------------------------------------------------------------------------

    /**
     * Gets the edge that is using this AP, or null if no edge is using this AP.
     * This method does account for embedding of the vertex in a template, i.e., 
     * this AP can be available in the graph (if any) owning the vertex this AP 
     * belongs to, but if the graph is 
     * itself the inner graph of a template, the AP is then projected on the
     * template's surface and might be used to make an edge that uses the 
     * template as a single vertex. This method considers any level of template
     * embedding. See {@link AttachmentPoint#getEdgeUser()}
     * to remain within the template boundary.
     * @return the edge that is using this AP.
     */

    public Edge getEdgeUserThroughout()
    {
        if (user == null)
        {
            if (owner != null 
                    && owner.getGraphOwner() != null 
                    && owner.getGraphOwner().templateJacket != null
                    && owner.getGraphOwner().templateJacket
                    .getOuterAPFromInnerAP(this) != null)
            {
               return owner.getGraphOwner().templateJacket
                    .getOuterAPFromInnerAP(this).getEdgeUserThroughout();
            } 
        } 
        return user;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * For vertexes that are templates this method returns the attachment point
     * that is embedded in the template's inner graph. Effectively, it returns
     * the original object of which this object is a projection on the templates
     * surface.
     * @return this attachment point (for vertexes that are no templates) or
     * the original attachment point this is a projection of (for templates).
     */
    public AttachmentPoint getEmbeddedAP()
    {
        if (owner != null && owner instanceof Template)
        {
            AttachmentPoint embeddedAP = 
                    ((Template)owner).getInnerAPFromOuterAP(this);
            //NB: if embeddedAP has no further embedding it returns itself
            return embeddedAP.getEmbeddedAP();
        } else {
            return this;
        }
    }
    
//-----------------------------------------------------------------------------
     
    /**
     * Gets the attachment point (AP) that is connected to this AP via the edge 
     * user.
     * @return the AP linked with this AP, or null;
     */
    public AttachmentPoint getLinkedAP() 
    {
        if (user == null)
            return null;
        
        if (user.getSrcAP() == this)
        {
            return user.getTrgAP();
        } else if (user.getTrgAP() == this)
        {
            return user.getSrcAP();
        }
        return null;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Gets the attachment point (AP) that is connected to this AP via the edge 
     * user or in any edge user that might be
     * external to the template embedding the graph where this AP is directly
     * reachable. Note that the AP that is used in the edge might not be the 
     * deepest image of that AP, i.e., the AP returned by this method might be
     * a projection of a more deeply embedded AP, which you can get by using the
     * {@link AttachmentPoint#getEmbeddedAP()} method.
     * @return the AP linked with this AP, or null;
     */
    public AttachmentPoint getLinkedAPThroughout() 
    {
        Edge user = getEdgeUserThroughout();
        if (user == null)
        {
            return null;
        }
        if (user.getSrcAPThroughout() == this.getEmbeddedAP() 
                || user.getSrcAP() == this)
        {
            return user.getTrgAP();
        } else if (user.getTrgAPThroughout() == this.getEmbeddedAP() 
                || user.getTrgAP() == this)
        {
            return user.getSrcAP();
        }
        return null;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Checks the role of this AP in the user or in any user that might be
     * external to the template embedding the graph where this AP is directly
     * reachable.
     * @return <code>true</code> if a user exists and this AP is the src AP in
     * that edge. Otherwise, this method returns <code >false</code> without
     * discriminating if this AP is free, i.e., the user is null, or this AP is 
     * the trg in the edge user.
     */
    public boolean isSrcInUserThroughout()
    {
        Edge user = getEdgeUserThroughout();
        if (user == null)
        {
            return false;
        }
        return user.getSrcAPThroughout() == this || user.getSrcAP() == this;
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Checks the role of this AP in the user.
     * @return <code>true</code> if a user exists and this AP is the src AP in
     * that edge. Otherwise, this method returns <code >false</code> without
     * discriminating if this AP is free, i.e., the user is null, or this AP is 
     * the trg in the edge user.
     */
    public boolean isSrcInUser()
    {
        return user != null && user.getSrcAP() == this;
    }

//-----------------------------------------------------------------------------

    /**
     * @return the current index of this AP in the list of APs of the owner, or
     * -1 if the owner is null.
     */
    public int getIndexInOwner()
    {
        if (owner == null)
        {
            return -1;
        }
        return owner.getIndexOfAP(this);
    }
    
//------------------------------------------------------------------------------
    
    public boolean hasSameSrcAtom(AttachmentPoint other)
    {
        AttachmentPoint deepThis = getEmbeddedAP();
        AttachmentPoint deepOther = other.getEmbeddedAP();
        Vertex deepOwnerT = deepThis.getOwner();
        Vertex deepOwnerO = deepOther.getOwner();
        
        if (deepOwnerT instanceof Fragment
                && deepOwnerO instanceof Fragment)
        {
            IAtom srcThis = ((Fragment) deepOwnerT).getAtomHoldingAP(
                    deepThis);
            IAtom srcOther = ((Fragment) deepOwnerO).getAtomHoldingAP(
                    deepOther);
            
            if (srcThis == srcOther)
            {
                return true;
            }
        }
        return false;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Checks if this and another APs are rooted on atoms that are bonded in
     * any way other than a possible connection resulting by an edge made in
     * between this and the other AP.
     * 
     * @param other the other aP to consider.
     * @return <code>true</code> is the two APs are rooted on atoms that 
     * participate in a bond either within a molecular fragment or across
     * a graph edge.
     */
    public boolean hasConnectedSrcAtom(AttachmentPoint other)
    {
        AttachmentPoint deepThis = getEmbeddedAP();
        AttachmentPoint deepOther = other.getEmbeddedAP();
        Vertex deepOwnerT = deepThis.getOwner();
        Vertex deepOwnerO = deepOther.getOwner();
        
        if (deepOwnerT instanceof Fragment
                && deepOwnerO instanceof Fragment)
        {
            IAtom srcThis = ((Fragment) deepOwnerT).getAtomHoldingAP(
                    deepThis);
            IAtom srcOther = ((Fragment) deepOwnerO).getAtomHoldingAP(
                    deepOther);
            
            if (srcThis.getContainer() == srcOther.getContainer())
            {
                if (srcThis.getContainer().getConnectedAtomsList(srcThis)
                        .contains(srcOther))
                {
                    return true;
                }
            } else {
                // If the two atoms are not directly connected, they
                // might be connected via an edge in the graph.
                List<AttachmentPoint> apsOnSrcThis = 
                        ((Fragment)deepOwnerT).getAPsFromAtom(srcThis);
                List<AttachmentPoint> apsOnSrcOther = 
                        ((Fragment)deepOwnerT).getAPsFromAtom(srcOther);
                for (AttachmentPoint apOnSrcThis : apsOnSrcThis)
                {
                    // We ignore the connection deepThis-to-deepOther
                    if (apOnSrcThis.isAvailableThroughout() 
                            || apOnSrcThis==deepThis)
                        continue;
                    
                    AttachmentPoint linkedAP = 
                            apOnSrcThis.getLinkedAPThroughout().getEmbeddedAP();
                    for (AttachmentPoint apOnSrcOther : apsOnSrcOther)
                    {
                        if (apOnSrcOther.isAvailableThroughout() 
                                || apOnSrcOther==deepOther)
                            continue;
                        if (linkedAP==apOnSrcOther)
                        {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
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

//-----------------------------------------------------------------------------
    
    /**
     * Prepares the two strings that can be used to define 
     * {@link AttachmentPoint}s in SDF files.
     * @param apsPerIndex a map of {@link AttachmentPoint}s grouped by 
     * index. The index may or may not be an index of an existing atom (i.e.,
     * we do not use it as such, but we just place if in the text-representation
     * of the AP. This index is supposed to be 0-based (i.e., in this method it 
     * is transformed in 1-based).
     * @return the string meant the be the value of the
     * {@link DENOPTIMConstants#APSTAG} tag in SDF file.
     */
    
    // WARNING: here is a place where we still assume a fixed order of APs
    // In fact, the order in which we process the keys is given by the comparable
    // class Integer, i.e., the APs are reported in SDF following the ordering
    // of the respective source atoms.
    // To solve the issue of ordering we could drop the format in which we 
    // collect APs by source atom (i.e., 1#firstAP,second-AP) and allow for
    // a one-to-one (sorted) list of APs where "1#firstAP,second-AP" becomes
    // "1#firstAP 1#second-AP"
    
    public static String getAPDefinitionsForSDF(
            LinkedHashMap<Integer, List<AttachmentPoint>> apsPerIndex)
    {   
        String s = "";
        for (Integer ii : apsPerIndex.keySet())
        {
            //WARNING: here is the 1-based criterion implemented also for
            // fake atom IDs!
            int atmID = ii+1;
    
            List<AttachmentPoint> apsOnAtm = apsPerIndex.get(ii);
            
            boolean firstCL = true;
            for (int i = 0; i<apsOnAtm.size(); i++)
            {
                AttachmentPoint ap = apsOnAtm.get(i);
                
                //Build SDF property DENOPTIMConstants.APCVTAG
                String stingAPP = ""; //String Attachment Point Property
                if (firstCL)
                {
                    firstCL = false;
                    stingAPP = ap.getSingleAPStringSDF(true,atmID);
                } else {
                    stingAPP = DENOPTIMConstants.SEPARATORAPPROPAPS 
                            + ap.getSingleAPStringSDF(false,atmID);
                }
                s = s + stingAPP;
            }
            s = s + DENOPTIMConstants.SEPARATORAPPROPATMS;
        }
        return s;
    }
    
//-----------------------------------------------------------------------------

}
