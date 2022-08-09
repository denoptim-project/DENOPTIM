package denoptim.programs.fragmenter;

import org.openscience.cdk.interfaces.IAtom;

import denoptim.graph.APClass;

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


//------------------------------------------------------------------------------

    public MatchedBond()
    {
        this.isSymm = false;
    }

//------------------------------------------------------------------------------

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

}
