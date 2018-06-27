package molecule;

import java.io.Serializable;

/**
 * Each attachment point is annotated by the number (position) of the atom
 * in the molecule, the number of bonds it is associated with, the current
 * number of bonds it is still allowed to form. Where applicable further
 * information in the form of the set of reaction classes is also added.
 * @author Vishwesh Venkatraman
 */


public class DENOPTIMAttachmentPoint implements Serializable
{
    private int atomPostionNumber;
    /*
     * the original number of connections of this atom
     */
    private int atomConnections; 

    /*
     * the current free connections
     */
    private int apConnections; 
    
    /*
     * The class associated with the AP
     */
    private String apClass;
    
    /*
     * The direction vector reprsenting the bond direction
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
     * @return the atom number position corresponding to the fragment
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
