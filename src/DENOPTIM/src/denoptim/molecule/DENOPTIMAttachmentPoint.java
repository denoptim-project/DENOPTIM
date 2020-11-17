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

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.molecule.DENOPTIMEdge.BondType;

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
    private int atomPostionNumber;
    
    /**
     * the original number of connections of this atom
     */
    private int totalConnections; 

    /**
     * the current free connections
     */
    private int freeConnections; 
    
    /**
     * The attachment point clss
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


//------------------------------------------------------------------------------

    /**
     * Constructor for undefined DENOPTIMAttachmentPoint
     */
    public DENOPTIMAttachmentPoint(DENOPTIMVertex owner) {
        this.owner = owner;
        atomPostionNumber = 0;
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
     */
    public DENOPTIMAttachmentPoint(DENOPTIMVertex owner, int atomPositionNumber,
                                   int atomConnections,int apConnections) {
        this(owner);
        atomPostionNumber = atomPositionNumber;
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
     */
    private DENOPTIMAttachmentPoint(DENOPTIMVertex owner, int atomPositionNumber,
                                   int atomConnections, int apConnections,
                                   double[] dirVec) {
        this(owner, atomPositionNumber, atomConnections, apConnections);
        this.dirVec = new double[3];
        System.arraycopy(dirVec, 0, this.dirVec, 0, dirVec.length);
    }
    
//------------------------------------------------------------------------------
  
    /**
     * Constructor
     * @param atomPosNum the index of the source atom (0-based).
     * @param atomConnections the total number of connections.
     * @param apConnections the number of free connections.
     * @param dirVec the AP direction vector end (the beginning at the coords
     *               of the source atom). This must array have 3 entries.
     * @param apRule the first part of the APClass.
     * @param apSubClass the second part of the APClass.
     * @throws DENOPTIMException 
     */
    public DENOPTIMAttachmentPoint(DENOPTIMVertex owner, int atomPosNum,
                                   int atomConnections, int apConnections,
                                   double[] dirVec, String apRule,
                                   int apSubClass) 
                                           throws DENOPTIMException {
        this(owner, atomPosNum, atomConnections, apConnections, dirVec);
        this.apClass = APClass.make(apRule,apSubClass);
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
     * @throws DENOPTIMException 
     */
    public DENOPTIMAttachmentPoint(DENOPTIMVertex owner, int atomPosNum,
                                   int atomConnections, int apConnections,
                                   double[] dirVec, APClass apClass) 
                                           throws DENOPTIMException {
        this(owner, atomPosNum, atomConnections, apConnections, dirVec);
        this.apClass = apClass;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Construct an attachment point based on the formatted string 
     * representation. The format is the one used in SDF files.
     * @param str the formatted string.
     * @param format is the format of the string. Acceptable values are 
     * <code>SDF</code> for strings coming from SDF file molecular properties,
     * and <code>MAP</code> for strings generated by the <code>toString()</code>
     * method of DENOPTIMAttachmentPoint.
     * @throws DENOPTIMException
     */
    public DENOPTIMAttachmentPoint(DENOPTIMVertex owner, String str,
                                   String format)
            throws DENOPTIMException
    {
        this(owner);
//        id = FragmentSpace.apID.getAndIncrement();
        switch (format)
        {
            case "SDF":
                processSdfString(str);
                break;
                
            case "MAP":
                processMapString(str);
                break;
                
            default:
                throw new DENOPTIMException("Unknown format for string "
                        + "representation of DENOPTIMAttachmentPoint");
        }
    }
    
//-----------------------------------------------------------------------------
    
    private void processMapString(String str) throws DENOPTIMException
    {
        if (str.contains("{") || str.contains("}")
                || str.contains("[") || str.contains("]"))
        {
            throw new DENOPTIMException("Unable to parse APString '" + str 
                    + "'");
        }
        
        String[] params = str.split(",");
        for (int i=0; i<params.length; i++)
        {
            String pvPair = params[i];
            String[] parts = pvPair.split("=");
            if (parts.length != 2)
            {
                throw new DENOPTIMException("Unable to parse APString '" + str 
                        + "'");
            }
            String apc = null;
            int subApc = -1;
            switch (parts[0])
            {
                case "atomPostionNumber":
                    this.atomPostionNumber = Integer.parseInt(parts[1]);
                    break;
                    
                case "totalConnections":
                    this.totalConnections = Integer.parseInt(parts[1]);
                    break;
                
                case "freeConnections":
                    this.freeConnections = Integer.parseInt(parts[1]);
                    break;
                
                case "apRule":
                    apc = parts[1];
                    break;
                
                case "apSubClass":
                    subApc = Integer.parseInt(parts[1]);
                    break;
                    
                /* TODO:del obsolete
                case "apClass":
                    this.apClass = parts[1];
                    break;
                    */
                
                case "dirVec.x":
                    this.dirVec[0] = Double.parseDouble(parts[1]);
                    break;
                
                case "dirVec.y":
                    this.dirVec[1] = Double.parseDouble(parts[1]);
                    break;
                    
                case "dirVec.z":
                    this.dirVec[2] = Double.parseDouble(parts[1]);
                    break;
                
                default:
                    throw new DENOPTIMException("Unable to parse APString '" 
                            + str + "'");                    
            }
            
            if (apc != null)
            {
                this.apClass = APClass.make(apc,subApc);
            }
        }
    }
    
//-----------------------------------------------------------------------------
        
    private void processSdfString(String str) throws DENOPTIMException
    {
        try 
        {
            String[] parts = str.split(
                    Pattern.quote(DENOPTIMConstants.SEPARATORAPPROPAAP));
            
            //WARNING here we convert from 1-based to 0-based index
            this.atomPostionNumber = Integer.parseInt(parts[0])-1;

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
        return atomPostionNumber;
    }

//------------------------------------------------------------------------------

    /**
     * Set the index of the source atom in the list of atoms of the fragment.
     * The index should reflect 0-based enumeration.
     * @param m_atomPostionNumber the index
     */
    public void setAtomPositionNumber(int m_atomPostionNumber)
    {
        atomPostionNumber = m_atomPostionNumber;
    }

//------------------------------------------------------------------------------

    /**
     * Set the number of unoccupied connections. This practically corresponds
     * to the number of unsaturated valences that can still be used on this 
     * attachment point. This is useful for attachment points that do not follow
     * the APClass-based formalism.
     * @param m_freeConnections
     */
    public void setFreeConnections(int m_freeConnections)
    {
        freeConnections = m_freeConnections;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Sets the end of the 3D vector defining the direction of the AP in 3D.
     * Note that the source of this vector is the source atom, and that the 
     * entries of <code>m_dirVec</code> are referred to the same origin as
     * the coordinates of the source atom.
     * @param m_dirVec the coordinates of the 3D point defining the end of the 
     * direction vector
     */
    public void setDirectionVector(double[] m_dirVec)
    {
        dirVec = new double[3];
        System.arraycopy(m_dirVec, 0, dirVec, 0, m_dirVec.length);  
    }

//------------------------------------------------------------------------------

    /**
     * Set the Attachment Point class. The APClass is the combination of a main
     * class (or "rule") and a subclass (or direction).  
     * @param m_class the new APClass.
     */
    
    public void setAPClass(String m_class) throws DENOPTIMException
    {
        apClass = APClass.make(m_class);
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
    
//-----------------------------------------------------------------------------

    /**
     * Returns the APRule component of the given APClass
     * @param apc the APClass to parse
     * @return the APRule
     */
    
    //TODO-V3 move to APClass class
    public static String getAPRule(String apc)
    {
        return apc.split(DENOPTIMConstants.SEPARATORAPPROPSCL)[0];
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
     * Resets the connections of this AP. Makes all connections free.
     */

    public void resetFreeConnections()
    {
        setFreeConnections(totalConnections);
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
     * Check availability of free connections
     * @return <code>true</code> if the attachment point has free connections
     */

    public boolean isAvailable()
    {
        return freeConnections > 0;
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
     * identifier. This is done to retail the order of attachment points when
     * regenerating AP lists multiple times.
     * @param other the attachment point to compare this with.
     * @return an integer that can be used by a comparator
     */
    //TODO-V3 @overwrite
    public int compareTo(DENOPTIMAttachmentPoint other)
    {
        return Integer.compare(this.getID(), other.getID());
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Compares this and given attachment point. This method defines how
     * DENOPTIMAttachmentPoint are sorted.
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
            
            if (thisVec[2] < otherVec[2])
            {
                return BEFORE;
            }
            else if (thisVec[2] > otherVec[2])
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
        
        // Make sure we are consistent with equals method
        assert this.equals(other) : 
            "DENOPTIMAttachmentPoint.compareTo inconsistent with equals.";
        
        return EQUAL;
    }
    
//------------------------------------------------------------------------------

    public boolean equals(DENOPTIMAttachmentPoint other)
    {
        if (this.getAtomPositionNumber() != other.getAtomPositionNumber())
        {
            return false;
        }
        
        if (this.getTotalConnections() != other.getTotalConnections())
        {
            return false;
        }

        if (this.getFreeConnections() != other.getFreeConnections())
        {
            return false;
        }
        
        if (this.getAPClass()!=null && other.getAPClass()!=null)
        {
           
            if (!this.getAPClass().equals(other.getAPClass()))
            {
                return false;
            }
        }
        
        if (this.getDirectionVector()!=null && other.getDirectionVector()!=null)
        {
            double trslh = 0.001;
            if (Math.abs(this.getDirectionVector()[0]
                    -other.getDirectionVector()[0])
                    > trslh)
            {
                return false;
            }
           
            if (Math.abs(this.getDirectionVector()[1]
                    -other.getDirectionVector()[1])
                    > trslh)
            {
                return false;
            }

            if (Math.abs(this.getDirectionVector()[2]
                    -other.getDirectionVector()[2])
                    > trslh)
            {
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
                        atomPostionNumber,
                        totalConnections,
                        freeConnections
                );
            } else {
                return new DENOPTIMAttachmentPoint(
                        getOwner(),
                        atomPostionNumber,
                        totalConnections,
                        freeConnections,
                        dirVec
                );
            }
        } else {
            DENOPTIMAttachmentPoint c = null;
            try
            {
                c = new DENOPTIMAttachmentPoint(
                        getOwner(),
                        atomPostionNumber,
                        totalConnections,
                        freeConnections,
                        dirVec,
                        apClass.clone());
            } catch (DENOPTIMException e)
            {
                // this cannot happen upon cloning
            }
            return c;
        }
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
        StringBuilder sb = new StringBuilder();
        if (isFirst)
        {
            //WARNING! In the mol.property we use 1-to-n+1 instead of 0-to-n
            sb.append(atomPostionNumber+1);
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
     * Produces a string with the information included in this object.
     * @return the string
     */
    
    @Override
    public String toString()
    {
        Map<String,Object> pars = new HashMap<String,Object>();
        pars.put("atomPositionNumber",atomPostionNumber);
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

    /**
     * Compare attachment points based on the reaction types
     * @param other to compare against
     * @return <code>true</code> if the points share a common reaction or more
     */
    // Do not delete this method although unused
    /* 
     MF: Uhm... why should we not remove this?
     I do not see why we would need this method, which is also wrong
     because it is not true that APs with same APClass are always compatible.
     Still, checking for same APClass is done to evaluate whether some APS
     can be classified as symmetric. I guess, that's where this comes from.
     So, do remove this among the TODO-V3 actions.
     
    public boolean isFragmentClassCompatible(DENOPTIMAttachmentPoint other) {
        boolean rcnEnabled = FragmentSpace.useAPclassBasedApproach();
        // if no reaction information is available return true
        if (!rcnEnabled) {
            return true;
        }
        // if both have reaction info
        String strA = getAPClass();
        String strB = other.getAPClass();
        return strA != null
                && strB != null
                && strA.compareToIgnoreCase(strB) == 0;
    }
*/
    
//-----------------------------------------------------------------------------

    public DENOPTIMVertex getOwner() {
        return owner;
    }

//-----------------------------------------------------------------------------

}
