package denoptim.programs.fragmenter;

import java.util.ArrayList;

import org.openscience.cdk.ChemObject;
import org.openscience.cdk.tools.periodictable.PeriodicTable;

import denoptim.constants.DENOPTIMConstants;
import denoptim.graph.APClass;

/**
 * A cutting rule with three SMARTS queries (atom 1, bond, atom2) and options.
 * A cutting rule is an ordered list of strings that, once put together in
 * the natural order, form the complete SMARTS query for one (or more) bonds 
 * between atom pairs. The order in the list of strings defines what 'first' 
 * and 'second' (or 0 and 1) mean in the the rest of the documentation, 
 * i.e., 'first' refers to anything pertaining atom 1, 
 * and 'second' pertains atom 2.
 *
 * @author Marco Foscato 
 */

public class CuttingRule
{
    /**
     * Rule name. Usually a human-readable string giving a hint on what this 
     * cutting rule is supposed to cut or represent.
     */
    private String ruleName;
    
    /**
     * First APClass derived from this rule.
     */
    private APClass apc0;
    
    /**
     * Second APClass derived from this rule.
     */
    private APClass apc1;

    /**
     *  SMARTS query matching the first atom. 
     */
    private String smartsAtm0;
    
    /**
     * SMARTS query matching the second atom.
     */
    private String smartsAtm1;
    
    /**
     * SMARTS query matching the bond between first and second atom.
     */
    private String smartsBnd;

    /**
     * Priority index of this rule. The lower the value, the earlier this rule
     * is used and the more dominant it is over other rules.
     */
    private int priority;

    /**
     * Additional Options
     */
    private ArrayList<String> opts;


//------------------------------------------------------------------------------

    /**
     * Constructor for a cutting rule.
     * @param ruleName name of the rule
     * @param smartsAtm0 the part of the SMARTS query pertaining the first atom.
     * @param smartsAtm1 the part of the SMARTS query pertaining the second atom.
     * @param smartsBnd the part of the SMARTS query pertaining the bond.
     * @param priority the priority index
     * @param opts any additional options.
     */
    public CuttingRule(String ruleName, String smartsAtm0, String smartsAtm1, 
            String smartsBnd, int priority, ArrayList<String> opts)
    {
        this.ruleName = ruleName;
        this.smartsAtm0 = smartsAtm0;
        this.smartsAtm1 = smartsAtm1;
        this.smartsBnd = smartsBnd;
        this.priority = priority;
        this.opts = opts;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the name of the cutting rule
     */
    public String getName()
    {
        return ruleName;
    }
    

//------------------------------------------------------------------------------
    
    /**
     * @return the priority index of this rule.
     */
   public int getPriority()
   {
       return priority;
   }

//------------------------------------------------------------------------------
//TODO remove 2 methods
    /**
     * Returns the name of SubClass0
     */
    public String getSubClassName0()
    {
        return apc0.toString(); //TODO del
    }

//------------------------------------------------------------------------------

    /**
     * Returns the name of SubClass1
     */
    public String getSubClassName1()
    {
        if (this.isSymmetric())
            return apc0.toString(); //TODO del
	else
	    return apc1.toString(); //TODO del
    }

//------------------------------------------------------------------------------

    /**
     * Get the AP class with sub class 0
     */
    public APClass getAPClass0()
    {
        return apc0;
    }

//------------------------------------------------------------------------------

    /**
     * Get the AP class with sub class 1
     */
    public APClass getAPClass1()
    {
    	if (this.isSymmetric())
    	    return apc0;
    	else
            return apc1;
    }

//------------------------------------------------------------------------------

    /**
     * Get complementary class
     */
    public APClass getComplementaryAPClass(APClass apc)
    {
    	if (this.isSymmetric() && apc.equals(apc0))
    	{
    	    return apc0;
    	} else if (!this.isSymmetric() && apc.equals(apc0)) {
    	    return apc1;
        } else if (!this.isSymmetric() && apc.equals(apc1)) {
            return apc0;
    	} else {
    	    return null;
    	}
    }

//------------------------------------------------------------------------------

    /**
     * Returns the SMARTS query of the whole rule
     */
    public String getWholeSMARTSRule()
    {
        return smartsAtm0+smartsBnd+smartsAtm1;
    }

//------------------------------------------------------------------------------

    /**
     * Get the SMARTS query of the first atom (SubClass 0)
     */
    public String getSMARTSAtom0()
    {
        return smartsAtm0;
    }

//------------------------------------------------------------------------------

    /**
     * Get the SMARTS query of the second atom (SubClass 1)
     */
    public String getSMARTSAtom1()
    {
        return smartsAtm1;
    }

//------------------------------------------------------------------------------

    /**
     * Get the SMARTS query of the bond.
     */
    public String getSMARTSBnd()
    {
        return smartsBnd;
    }

//------------------------------------------------------------------------------

    /**
     * @return true if this rule has further options.
     */
    public boolean hasOptions()
    {
        if (opts.size() > 0)
            return true;
        else
            return false;
    }

//------------------------------------------------------------------------------

    /**
     * @return true if this rule matches multihapto ligands.
     */
    public boolean isHAPTO()
    {
        if (opts.contains("HAPTO"))
            return true;
        else
            return false;
    }

//------------------------------------------------------------------------------

    /**
     * @return true if this is a symmetric rule (atom SMARTS coincide)
     */
    public boolean isSymmetric()
    {
        if (smartsAtm0.equals(smartsAtm1))
            return true;
        else
            return false;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the list of options.
     */
    public ArrayList<String> getOptions()
    {
        return opts;
    }

//------------------------------------------------------------------------------

    /**
     * Tries to identify the bond order of the matched bond by searching for the
     * corresponding, non-negated characters.
     */
    public int getBondOrder()
    {
    	int res = -1;
    
    	//Easy case
    	String s = smartsBnd;
    	if (s.contains("!@"))
    	{
    	    s = s.substring(0,s.indexOf("!@"));
    	}
    	if (s.equals("-"))
    	    res = 1;
    	else if (s.equals("="))
    	    res = 2;
    	else if (s.equals("#"))
            res = 3;
        return res;
    
    	// well, in general it's not so easy...
        //TODO make it more general.
    }

//------------------------------------------------------------------------------

    /**
     * @return <code>true</code> if the rule involves metals.
     */
    public boolean involvesMetal()
    {
    	for (String el : DENOPTIMConstants.ALL_METALS)
    	{
    	    if (smartsAtm0.contains(el))
    	    {
    		return true;
    	    }
    	}

        //analyze atom 1
        for (String el : DENOPTIMConstants.ALL_METALS)
        {
            if (smartsAtm1.contains(el))
            {
                return true;
            }
        }
        return false;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the string representing this rule.
     */
    public String toString()
    {
        String str = ruleName+"_"+smartsAtm0+smartsBnd+smartsAtm1+
				"_priority:"+priority+
				"_opts:"+opts;
        return str;
    }

//------------------------------------------------------------------------------

}
