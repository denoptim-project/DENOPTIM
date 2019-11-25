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
import java.util.ArrayList;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;

/**
 * Each attachment point is annotated by the number (position) of the atom
 * in the molecule, the number of bonds it is associated with, the current
 * number of bonds it is still allowed to form. Where applicable further
 * information in the form of the set of reaction classes is also added.
 * 
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */


public class DENOPTIMAttachmentPoint implements Serializable
{
	/**
	 * The index of the source atom in the atom list of the fragment
	 */
	//TODO: clarify is this is this 0- or 1-based?
    private int atomPostionNumber;
    
    /**
     * the original number of connections of this atom
     */
    private int atomConnections; 

    /**
     * the current free connections
     */
    private int apConnections; 
    
    /**
     * The cutting rule that generated this AP (the main APClass)
     */
    private String apRule;
    
    /**
     * The direction index of the cutting rule that generated this AP 
     * (the subClass)
     */
    private int apSubClass;
    
    /**
     * The class associated with the AP
     */
    private String apClass;
    
    /**
     * The direction vector representing the bond direction
     */
    private double[] dirVec; 


//------------------------------------------------------------------------------

    public DENOPTIMAttachmentPoint()
    {
        atomPostionNumber = 0;
        atomConnections = 0;
        apConnections = 0;
        apClass = "";
    }

//------------------------------------------------------------------------------

    public DENOPTIMAttachmentPoint(int m_AtomPosNum, int m_atomConnections,
                                                            int m_apConnections)
    {
        atomPostionNumber = m_AtomPosNum;
        atomConnections = m_atomConnections;
        apConnections = m_apConnections;
        apClass = "";
    }
    
//------------------------------------------------------------------------------
    
    public DENOPTIMAttachmentPoint(int m_AtomPosNum, int m_atomConnections,
                                        int m_apConnections, double[] m_dirVec )
    {
        atomPostionNumber = m_AtomPosNum;
        atomConnections = m_atomConnections;
        apConnections = m_apConnections;
        apClass = "";
        
        dirVec = new double[3];
        System.arraycopy(m_dirVec, 0, dirVec, 0, m_dirVec.length);        
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Construct an attachment point based on the formatted string 
     * representation. The format is the one used in SDF files.
     * @param str the formatted string.
     * @throws DENOPTIMException
     */
    public DENOPTIMAttachmentPoint(String str) throws DENOPTIMException
    {
        try 
        {
            String[] parts = str.split(
            		DENOPTIMConstants.SEPARATORAPPROPAAP);
            this.atomPostionNumber = Integer.parseInt(parts[0]);

            String[] details = parts[1].split(
            		DENOPTIMConstants.SEPARATORAPPROPSCL);
            this.apRule = details[0];
            this.apSubClass = Integer.parseInt(details[1]);
            this.apClass = this.apRule + Integer.toString(this.apSubClass);

            String[] coord = details[2].split(
            		DENOPTIMConstants.SEPARATORAPPROPXYZ);
            double[] pointer = new double[]{};
            pointer[0] = Double.parseDouble(coord[0]);
            pointer[1] = Double.parseDouble(coord[1]);
            pointer[2] = Double.parseDouble(coord[2]);
            this.dirVec = pointer;
	    } catch (Throwable t) {
			throw new DENOPTIMException("Cannot construct AP from string '" 
						+ str + "'");
	    }
    }

//------------------------------------------------------------------------------

    /**
     * @return the original number of connections
     */
    public int getAtmConnections()
    {
    	return atomConnections;
    }    

//------------------------------------------------------------------------------

    /**
     * @return the current free connections associated with the atom
     */
    public int getAPConnections()
    {
        return apConnections;
    }

//------------------------------------------------------------------------------

    /**
     * @return the index of the source atom in the atom list of the fragment
     */
    public int getAtomPositionNumber()
    {
        return atomPostionNumber;
    }

//------------------------------------------------------------------------------

    public void setAtomPositionNumber(int m_atomPostionNumber)
    {
        atomPostionNumber = m_atomPostionNumber;
    }

//------------------------------------------------------------------------------

    public void setAPConnections(int m_apConnections)
    {
        apConnections = m_apConnections;
    }
    
//------------------------------------------------------------------------------
    
    public void setDirectionVector(double[] m_dirVec)
    {
        dirVec = new double[3];
        System.arraycopy(m_dirVec, 0, dirVec, 0, m_dirVec.length);  
    }

//------------------------------------------------------------------------------

    public void setAPClass(String m_class)
    {
        apClass = m_class;
    }

//------------------------------------------------------------------------------

    public String getAPClass()
    {
        return apClass;
    }
//------------------------------------------------------------------------------

    public double[] getDirectionVector()
    {
        return dirVec;
    }    

//------------------------------------------------------------------------------

    /**
     * resets the connections of this AP to that of the atom it represents
     */

    public void resetAPConnections()
    {
        setAPConnections(atomConnections);
    }

//------------------------------------------------------------------------------

    public void updateAPConnections(int delta)
    {
        apConnections = apConnections + delta;
    }

//------------------------------------------------------------------------------

    /**
     * Check availability of free connections
     * @return <code>true</code> if the attachment point has free connections
     */

    public boolean isAvailable()
    {
        return apConnections > 0;
    }

//------------------------------------------------------------------------------

    public boolean isClassEnabled()
    {
        return apClass.length() > 0;
    }
    
//------------------------------------------------------------------------------
    
    public int compareTo(DENOPTIMAttachmentPoint other)
    {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        if (this == other)
            return EQUAL;

        //Compare CLASS name
        String cl = this.getAPClass();
        String othercl = other.getAPClass();
        int res = cl.compareTo(othercl);
        if (res < 0)
            return BEFORE;
        else if (res > 0)
            return AFTER;
        
        int tBO = FragmentSpace.getBondOrderForAPClass(this.getAPClass());
        int oBO = FragmentSpace.getBondOrderForAPClass(other.getAPClass());
        if (tBO != oBO)
        {
        	System.out.println("WARNING: Unexpected difference in Bond Order "
        			+ "while CLASS is equal!");
        }
        
        //Compare Direction Vector if AtomID is equal
        double[] thisVec = this.getDirectionVector();
        double[] otherVec = other.getDirectionVector();

        if (thisVec[0] < otherVec[0])
            return BEFORE;
        else if (thisVec[0] > otherVec[0])
            return AFTER;

        if (thisVec[1] < otherVec[1])
            return BEFORE;
        else if (thisVec[1] > otherVec[1])
            return AFTER;
        
        if (thisVec[2] < otherVec[2])
            return BEFORE;
        else if (thisVec[2] > otherVec[2])
            return AFTER;
        
        assert this.equals(other) : 
        	"DENOPTIMAttachmentPoint.compareTo inconsistent with equals.";
        
        return EQUAL;
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
			sb.append(atomPostionNumber);
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

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(64);
        sb.append(atomPostionNumber).append("|");
	sb.append(atomConnections).append("|");
        sb.append(apConnections);
        if (apClass.length() > 0)
        {
            sb.append("|");
            sb.append(apClass);
            if (dirVec != null)
            {
                sb.append("|");
                sb.append(dirVec[0]).append(", ").append(dirVec[1]).append(", ").append(dirVec[2]);
            }
        }
        return sb.toString();
    }
    
//------------------------------------------------------------------------------
}
