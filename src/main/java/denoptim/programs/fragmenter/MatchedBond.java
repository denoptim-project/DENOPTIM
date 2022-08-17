package denoptim.programs.fragmenter;

import java.util.logging.Logger;

import org.openscience.cdk.interfaces.IAtom;

public class MatchedBond
{
    /**
     * Atom matching the first SMARTS query of the {@link CuttingRule}.
     */
    private IAtom atm0;
    
    /**
     * Atom matching the second SMARTS query of the {@link CuttingRule}.
     */
    private IAtom atm1;

    /**
     * Flag signaling the the classes on the two sided of the bond are supposed
     * to be equal (i.e., symmetric match).
     */
    private boolean isSymm;

    /**
     * The cutting rules that matched this bond
     */
    private CuttingRule rule;
    
    /**
     * Flag indicating that we have checked the additional option from the 
     * cutting rule (otherwise this flag is <code>null></code>) 
     * and that those criteria are satisfied (<code>true</code>) or
     * not (<code>false></code>).
     */
    private Boolean satisfiesRuleOptions = null;


//------------------------------------------------------------------------------

    /**
     * Creates an instance by defining its components. Does not check 
     * whether the bond between the two atoms satisfies any further critierion
     * beyond SMARTS queries. To test those use 
     * {@link #satisfiesRuleOptions(Logger)}.
     * @param atm0 first atom involved in the bond.
     * @param atm1 second atom involved in the bond
     * @param rule the cutting rules that defines SMARTS queries matching the 
     * two atoms and the bond between them.
     */
    public MatchedBond(IAtom atm0, IAtom atm1, CuttingRule rule)
    {
        this.atm0 = atm0;
        this.atm1 = atm1;
        this.isSymm = rule.isSymmetric();
        this.rule = rule;
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the name of the cutting rule this bond matches
     */
    public CuttingRule getRule()
    {
        return rule;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the atom matching subclass '1'
     */
    public IAtom getAtmSubClass1()
    {
        return atm1;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the atom matching subclass '0'
     */
    public IAtom getAtmSubClass0()
    {
        return atm0;
    }

//------------------------------------------------------------------------------

    /**
     * @return true if this bond had the same subclass on both atoms
     */
    public boolean hasSymmetricSubClass()
    {
        return isSymm;
    }

//------------------------------------------------------------------------------

    /**
     * Checks if this bond satisfies all the option of the cutting rule.
     * Beyond the matching of SMARTS queries, cutting rules can have further
     * options that restrict the matches. This method asks the cutting rule to
     * verify those options.
     * @param logger where to direct any log.
     * @return <code>true</code> if all options are satisfied by this bond.
     */
    public boolean satisfiesRuleOptions(Logger logger)
    {
        if (satisfiesRuleOptions==null)
            satisfiesRuleOptions = rule.satisfiesOptions(this, logger);
        return satisfiesRuleOptions;
    }

//------------------------------------------------------------------------------

}
