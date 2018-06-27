package denoptimcg;

/**
 *
 * @author Vishwesh Venkatraman
 * This data structure stores the connected atoms and the corresponding attachment 
 * point indices
 */
public class ICAPVec 
{
    private int icIdA; // the position of atomA in the IC list
    private int icIdB; // the position of atomB in the IC list
    private int vecIdxA; // the index of the AP vector associated with A
    private int vecIdxB; // the index of the AP vector associated with B
    
//------------------------------------------------------------------------------    
    
    public ICAPVec()
    {
        
    }
    
//------------------------------------------------------------------------------
    
    public ICAPVec(int m_icIdA, int m_icIdB, int m_vecIdxA, int m_vecIdxB)
    {
        icIdA = m_icIdA;
        icIdB = m_icIdB;
        vecIdxA = m_vecIdxA;
        vecIdxB = m_vecIdxB;
    }
    
//------------------------------------------------------------------------------    
    
    protected int getFirstAP()
    {
        return vecIdxA;
    }
    
//------------------------------------------------------------------------------        
    
    protected int getSecondAP()
    {
        return vecIdxB;
    }
    
//------------------------------------------------------------------------------            
    
    protected int getFirstIC()
    {
        return icIdA;
    }
    
//------------------------------------------------------------------------------        
    
    protected int getSecondIC()
    {
        return icIdB;
    }
    
//------------------------------------------------------------------------------            
    
    @Override
    public String toString()
    {
        String str = icIdA + ":" + vecIdxA + " | " + icIdB + ":" + vecIdxB + "\n";
        return str;
    }
    
//------------------------------------------------------------------------------                
    
}
