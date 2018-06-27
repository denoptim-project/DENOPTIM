package utils;

/**
 * For affinity fingerprint clustering
 * @author Vishwesh Venkatraman
 */
public class DENOPTIMAFPair implements Comparable<DENOPTIMAFPair>
{
    private int index;
    private double affinity;
    
//------------------------------------------------------------------------------    
    
    public DENOPTIMAFPair(int m_idx, double m_affinity)
    {
        index = m_idx;
        affinity = m_affinity;
    }
    
//------------------------------------------------------------------------------    
    
    public int getIndex()
    {
        return index;        
    }
    
//------------------------------------------------------------------------------    
    
    public double getAffinity()
    {
        return affinity;
    }
    
//------------------------------------------------------------------------------    
    
    @Override
    public String toString() 
    {
        return index + " " + affinity;
    }
    
//------------------------------------------------------------------------------    
    
    @Override
    public int compareTo(DENOPTIMAFPair B)
    {
        if (this.affinity > B.affinity)
            return 1;
        else if (this.affinity < B.affinity)
            return -1;
        return 0;
    }
    
//------------------------------------------------------------------------------    
    
}
