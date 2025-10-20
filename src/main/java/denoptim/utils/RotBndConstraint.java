package denoptim.utils;

/**
 * The definition of a constraint applying to rotatable bonds
 */
public class RotBndConstraint
{

    /**
     * @param value the value to set
     */
    public void setValue(double value)
    {
        this.value = value;
    }

    /**
     * A name to identify this constraint among others
     */
    private String name = "";
    
    /**
     * SMARTS wueries identifying the 4-tuple of atoms definign the dihedral 
     * constraint
     */
    private String smarts = "";
    
    /**
     * The value of the dihedral mapped by the SMARTS
     */
    private double value = 0.0;

//------------------------------------------------------------------------------
    
    public RotBndConstraint(String name, String smarts, double value)
    {
        this.name = name;
        this.smarts = smarts;
        this.value = value;
    }

//------------------------------------------------------------------------------

    /**
     * @return the name
     */
    public String getName()
    {
        return name;
    }

//------------------------------------------------------------------------------

    /**
     * @return the smarts
     */
    public String getSmarts()
    {
        return smarts;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return the value
     */
    public double getValue()
    {
        return value;
    }
    
//------------------------------------------------------------------------------
}
