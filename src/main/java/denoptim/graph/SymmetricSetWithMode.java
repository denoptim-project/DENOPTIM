package denoptim.graph;

/**
 * Class coupling a reference to a {@link SymmetricSet} 
 * with a string that we call "mode" and can is used to store any sort of 
 * information.
 * The use case for this class is to be the identifier for different uses (i.e.,
 * different "modes") of the same {@link SymmetricSet}. In fact, this class is 
 * meant to return the same hash when two instances link to the same 
 * {@link SymmetricSet} and mode string.
 */
public class SymmetricSetWithMode
{
    protected SymmetricSet symItems;
    protected String mode;
    
//------------------------------------------------------------------------------
    
    public SymmetricSetWithMode(SymmetricSet symItems, String mode)
    {
        this.symItems = symItems;
        this.mode = mode;
    }
    
//------------------------------------------------------------------------------
       
    @Override
    public boolean equals(Object o)
    {
        if (o == null)
            return false;
        
        if (o == this)
            return true;
       
        if (o.getClass() != getClass())
        return false;
        
        SymmetricSetWithMode other = (SymmetricSetWithMode) o;
        
        if (!this.symItems.equals(other.symItems))
            return false;
        
        return this.mode.equals(other.mode);
    }
    
//------------------------------------------------------------------------------
    
    @Override
    public int hashCode() 
    {
        return symItems.hashCode() + mode.hashCode();
    }
    
//------------------------------------------------------------------------------
    
}