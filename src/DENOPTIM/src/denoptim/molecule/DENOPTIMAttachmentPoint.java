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

package denoptim.molecule;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.vecmath.Point3d;

import org.openscience.cdk.interfaces.IAtom;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.utils.DENOPTIMMoleculeUtils;

/**
 * Each attachment point is annotated by the number (position) of the atom
 * in the molecule, the number of bonds it is associated with, the current
 * number of bonds it is still allowed to form. Where applicable further
 * information in the form of the set of reaction classes is also added.
 * 
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */


public class DENOPTIMAttachmentPoint implements Serializable, Cloneable, 
    Comparable<DENOPTIMAttachmentPoint>
{
    /**
     * Version UID
     */
    private static final long serialVersionUID = -3680248393705286640L;
    
    /**
     * Index used to keep the order in a list of attachment points
     */
    private int id;
    
	/**
	 * The index of the source atom in the atom list of the fragment (0-based)
	 */
    private int atomPositionNumber;
    
    /**
     * The index of the source atom in the atom list of the entire molecule 
     * (0-based)
     */
    private int atomPositionNumberInMol;
    
    //TODO-V3 remove
    /**
     * the original number of connections of this atom
     */
    private int totalConnections; 


    //TODO-V3 remove
    /**
     * the current free connections
     */
    private int freeConnections; 
    
    /**
     * The attachment point class
     */
    private APClass apClass;
    
    /**
     * The direction vector representing the bond direction
     */
    private double[] dirVec;

    /**
     * The vertex to which this AP is attached to.
     */
    private DENOPTIMVertex owner;
    
    /**
     * The edge that is using this AP, if any
     */
    private DENOPTIMEdge user;


//------------------------------------------------------------------------------

    /**
     * Constructor for undefined DENOPTIMAttachmentPoint
     * @deprecated Use DENOPTIMVertex.addAP(...) instead
     */
    @Deprecated
    public DENOPTIMAttachmentPoint(DENOPTIMVertex owner) 
    {
        this.owner = owner;
        atomPositionNumber = 0;
        totalConnections = 0;
        freeConnections = 0;
        id = FragmentSpace.apID.getAndIncrement();
    }

//------------------------------------------------------------------------------

    /**
     * Constructor
     * @param atomPositionNumber the index of the source atom (0-based)
     * @param atomConnections the total number of connections
     * @param apConnections the number of free connections
     * @deprecated Use DENOPTIMVertex.addAP(...) instead
     */
    @Deprecated
    public DENOPTIMAttachmentPoint(DENOPTIMVertex owner, int atomPositionNumber,
                                   int atomConnections,int apConnections) 
    {
        this(owner);
        this.atomPositionNumber = atomPositionNumber;
        totalConnections = atomConnections;
        freeConnections = apConnections;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Constructor
     * @param atomPositionNumber the index of the source atom (0-based)
     * @param atomConnections the total number of connections
     * @param apConnections the number of free connections
     * @param dirVec the AP direction vector end (the beginning are the
     * coords of the source atom). This array must have 3 entries.
     * @deprecated Use DENOPTIMVertex.addAP(...) instead
     */
    @Deprecated
    private DENOPTIMAttachmentPoint(DENOPTIMVertex owner, int atomPositionNumber,
                                   int atomConnections, int apConnections,
                                   double[] dirVec) 
    {
        this(owner, atomPositionNumber, atomConnections, apConnections);
        this.dirVec = new double[3];
        System.arraycopy(dirVec, 0, this.dirVec, 0, dirVec.length);
    }

//------------------------------------------------------------------------------

    /**
     * Constructor
     * @param atomPosNum the index of the source atom (0-based)
     * @param atomConnections the total number of connections
     * @param apConnections the number of free connections
     * @param apClass the APClass
     * @deprecated Use DENOPTIMVertex.addAP(...) instead
     */
    @Deprecated
    public DENOPTIMAttachmentPoint(DENOPTIMVertex owner, int atomPosNum,
                                   int atomConnections, int apConnections,
                                   APClass apClass) {
        this(owner, atomPosNum, atomConnections, apConnections,
                new double[]{0.0, 0.0, 0.0}, apClass);
    }

    
//------------------------------------------------------------------------------
    
    /**
     * Constructor
     * @param atomPosNum the index of the source atom (0-based)
     * @param atomConnections the total number of connections
     * @param apConnections the number of free connections
     * @param dirVec the AP direction vector end (the beginning at the coords
     *               of the source atom). This must array have 3 entries.
     * @param apClass the APClass
     * @deprecated Use DENOPTIMVertex.addAP(...) instead
     */
    @Deprecated
    public DENOPTIMAttachmentPoint(DENOPTIMVertex owner, int atomPosNum,
                                   int atomConnections, int apConnections,
                                   double[] dirVec, APClass apClass) 
    {
        this(owner, atomPosNum, atomConnections, apConnections, dirVec);
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
    public DENOPTIMAttachmentPoint(DENOPTIMVertex owner, String str)
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
                    //APClass:subclass but no direction vector
                    break;
                    
                case 3:
                    //APClass:subclass:direction_vector
                    break;
                    
                default:
                    throw new DENOPTIMException("Unable to split APClass, "
                            + "subclass, and coordinates");
            }
            
            this.apClass = APClass.make(details[0],Integer.parseInt(details[1]));
            
            if (details.length == 3)
            {
                String[] coord = details[2].split(
                        Pattern.quote(DENOPTIMConstants.SEPARATORAPPROPXYZ)); 
                
                if (coord.length == 3)
                {
                    this.dirVec = new double[3];
                    this.dirVec[0] = Double.parseDouble(coord[0]);
                    this.dirVec[1] = Double.parseDouble(coord[1]);
                    this.dirVec[2] = Double.parseDouble(coord[2]);
                }
            }
            this.freeConnections = FragmentSpace.getBondOrderForAPClass(
                    apClass.getRule()).getValence();
            this.totalConnections = this.freeConnections;
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
     * Return the total number of connections that can be generated by this AP.
     * This practically corresponds to the max valence this AP can occupy.
     * This is useful for attachment points that do not follow
     * the APClass-based formalism.
     * @return the max number of connections
     */
    public int getTotalConnections()
    {
        return totalConnections;
    }    

//------------------------------------------------------------------------------

    /**
     * Return the number of unoccupied connections. This practically corresponds
     * to the number of unsaturated valences that can still be used on this 
     * attachment point. This is useful for attachment points that do not follow
     * the APClass-based formalism.
     * @return the current free connections associated with the atom
     */
    public int getFreeConnections()
    {
        return freeConnections;
    }

//------------------------------------------------------------------------------

    /**
     * The index of the source atom in the atom list of the fragment. 
     * The index is reported considering 0-based enumeration.
     * @return the index of the source atom in the atom list of the fragment
     */
    public int getAtomPositionNumber()
    {
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
     * Set the number of unoccupied connections. This practically corresponds
     * to the number of unsaturated valences that can still be used on this 
     * attachment point. This is useful for attachment points that do not follow
     * the APClass-based formalism.
     * @param freeConnections
     */
    public void setFreeConnections(int freeConnections)
    {
        this.freeConnections = freeConnections;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Sets the end of the 3D vector defining the direction of the AP in 3D.
     * Note that the source of this vector is the source atom, and that the 
     * entries of <code>dirVec</code> are referred to the same origin as
     * the coordinates of the source atom.
     * @param dirVec the coordinates of the 3D point defining the end of the
     * direction vector
     */
    public void setDirectionVector(double[] dirVec)
    {
        this.dirVec = new double[3];
        System.arraycopy(dirVec, 0, this.dirVec, 0, dirVec.length);
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
    
    public void setAPClass(APClass apClass) throws DENOPTIMException
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
    public double[] getDirectionVector()
    {
        return dirVec;
    }
    
//------------------------------------------------------------------------------

    /**
     * Change the number of free connections by the given delta.
     * @param delta the amount of change.
     */
    public void updateFreeConnections(int delta)
    {
        freeConnections = freeConnections + delta;
    }

//------------------------------------------------------------------------------

    /**
     * Check availability of this attachment point. Does not account for
     * embedding of the vertex in a template, i.e., this AP can be available
     * in the graph owning the vertex this AP belongs to, but if the graph is 
     * itself the inner graph of a template, the AP might be projected on the
     * template's surface and used to make an edge at that level. To account for
     * such possibility use 
     * {@link DENOPTIMAttachmentPoint#isAvailableThroughout()}
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
     * {@link DENOPTIMAttachmentPoint#isAvailable()}.
     * @return <code>true</code> if the attachment point has no user.
     */

    public boolean isAvailableThroughout()
    {
        if (user == null)
        {
            if (owner.getGraphOwner() != null 
                    && owner.getGraphOwner().templateJacket != null)
            {
               return owner.getGraphOwner().templateJacket
                    .getOuterAPFromInnerAP(this).isAvailableThroughout();
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
    public int compareTo(DENOPTIMAttachmentPoint other)
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
    public int comparePropertiesTo(DENOPTIMAttachmentPoint other)
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
            double[] thisVec = this.getDirectionVector();
            double[] otherVec = other.getDirectionVector();
    
            if (thisVec[0] < otherVec[0])
            {
                return BEFORE;
            }
            else if (thisVec[0] > otherVec[0])
                return AFTER;
    
            if (thisVec[1] < otherVec[1])
            {
                return BEFORE;
                
            }
            else if (thisVec[1] > otherVec[1])
                return AFTER;
            if (thisVec.length > 2 && otherVec.length > 2)
            {
                if (thisVec[2] < otherVec[2])
                {
                    return BEFORE;
                }
                else if (thisVec[2] > otherVec[2])
                    return AFTER;
            }
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
    public boolean sameAs(DENOPTIMAttachmentPoint other)
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
     * @return <code>true</code> is the two APs have the same features.
     */
    public boolean sameAs(DENOPTIMAttachmentPoint other, StringBuilder reason)
    {
        if (this.getAtomPositionNumber() != other.getAtomPositionNumber())
        {
            reason.append("Different source atom for APs ("
                    + this.getAtomPositionNumber() + ","
                    + other.getAtomPositionNumber() + ");");
            return false;
        }
        
        if (this.getTotalConnections() != other.getTotalConnections())
        {
            reason.append("Different total number of connections APs;");
            return false;
        }

        if (this.getFreeConnections() != other.getFreeConnections())
        {
            reason.append("Different number of free connections APs;");
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
        
        if (this.getDirectionVector()!=null && other.getDirectionVector()!=null)
        {
            boolean different = false;
            double trslh = 0.001;
            for (int i=0; i<3; i++)
            {
                if (Math.abs(this.getDirectionVector()[i]
                        -other.getDirectionVector()[i])
                        > trslh)
                {
                    different = true;
                    break;
                }
            }
           
            if (different)
            {
                reason.append("Different direction vector");
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
    
    public DENOPTIMAttachmentPoint clone()
    {   
        if (apClass == null)
        {
            if (dirVec == null)
            {
                return new DENOPTIMAttachmentPoint(
                        getOwner(),
                        atomPositionNumber,
                        totalConnections,
                        freeConnections
                );
            } else {
                return new DENOPTIMAttachmentPoint(
                        getOwner(),
                        atomPositionNumber,
                        totalConnections,
                        freeConnections,
                        dirVec
                );
            }
        } else {
            if (dirVec == null) {
                return new DENOPTIMAttachmentPoint(
                        getOwner(),
                        atomPositionNumber,
                        totalConnections,
                        freeConnections,
                        apClass.clone()
                );
            }
            return new DENOPTIMAttachmentPoint(
                    getOwner(),
                    atomPositionNumber,
                    totalConnections,
                    freeConnections,
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
        sb.append(apClass);
        if (dirVec != null)
        {
            DecimalFormat digits = new DecimalFormat("###.####");
            digits.setMinimumFractionDigits(4);
            sb.append(DENOPTIMConstants.SEPARATORAPPROPSCL);
            sb.append(digits.format(dirVec[0]));
            sb.append(DENOPTIMConstants.SEPARATORAPPROPXYZ);
            sb.append(digits.format(dirVec[1]));
            sb.append(DENOPTIMConstants.SEPARATORAPPROPXYZ);
            sb.append(digits.format(dirVec[2]));
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
        pars.put("totalConnections",totalConnections);
        pars.put("freeConnections",freeConnections);
        if (apClass != null)
        {
            pars.put("apClass",apClass);
        }
        if (dirVec != null)
        {
            pars.put("dirVec.x",dirVec[0]);
            pars.put("dirVec.y",dirVec[1]);
            pars.put("dirVec.z",dirVec[2]);
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
        pars.put("totalConnections",totalConnections);
        pars.put("freeConnections",freeConnections);
        if (apClass != null)
        {
            pars.put("apClass",apClass);
        }
        if (dirVec != null)
        {
            pars.put("dirVec.x",dirVec[0]);
            pars.put("dirVec.y",dirVec[1]);
            pars.put("dirVec.z",dirVec[2]);
        }

        return pars.toString();
    }
    
//-----------------------------------------------------------------------------

    //TODO-V3: we should not be given the possibility to change the AP's owner
    // However, this is currently needed because of the DNOPTIMVertex.addAP(...)
    
    /**
     * Sets the reference to the vertex the owns this attachment point.
     * @param owner the vertex that own this attachment point.
     */
    public void setOwner(DENOPTIMVertex owner)
    {
        this.owner = owner;
    }
    
//-----------------------------------------------------------------------------

    public DENOPTIMVertex getOwner() {
        return owner;
    }

//-----------------------------------------------------------------------------
    
    /**
     * Sets the reference to the edge that is using this attachment point.
     * @param edge the user
     */
    public void setUser(DENOPTIMEdge edge) {
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
     * owners exist. See {@link DENOPTIMAttachmentPoint#getEdgeUserThroughout()}
     * to crossing the template boundary.
     * @return the edge that is using this AP.
     */
    public DENOPTIMEdge getEdgeUser() {
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
     * embedding. See {@link DENOPTIMAttachmentPoint#getEdgeUser()}
     * to remain within the template boundary.
     * @return the edge that is using this AP.
     */

    public DENOPTIMEdge getEdgeUserThroughout()
    {
        if (user == null)
        {
            if (owner != null 
                    && owner.getGraphOwner() != null 
                    && owner.getGraphOwner().templateJacket != null)
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
    public DENOPTIMAttachmentPoint getEmbeddedAP()
    {
        if (owner != null && owner instanceof DENOPTIMTemplate)
        {
            DENOPTIMAttachmentPoint embeddedAP = 
                    ((DENOPTIMTemplate)owner).getInnerAPFromOuterAP(this);
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
    public DENOPTIMAttachmentPoint getLinkedAP() 
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
     * user or in any user that might be
     * external to the template embedding the graph where this AP is directly
     * reachable.
     * @return the AP linked with this AP, or null;
     */
    public DENOPTIMAttachmentPoint getLinkedAPThroughout() 
    {
        DENOPTIMEdge user = getEdgeUserThroughout();
        if (user == null)
        {
            return null;
        }
        if (user.getSrcAPThroughout() == this){
            return user.getTrgAP();
        } else if (user.getTrgAPThroughout() == this)
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
        DENOPTIMEdge user = getEdgeUserThroughout();
        if (user == null)
        {
            return false;
        }
        return user.getSrcAPThroughout() == this;
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

    public void setTotalConnections(int totalConnections) {
        this.totalConnections = totalConnections;
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

//-----------------------------------------------------------------------------

}
