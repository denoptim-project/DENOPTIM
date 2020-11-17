package denoptim.molecule;

import java.io.Serializable;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;

public class APClass implements Cloneable,Comparable<APClass>,Serializable
{
    /**
     * Version UID
     */
    private static final long serialVersionUID = 6888932496975573345L;

    /**
     * The main feature of the APClass. This would usually correspond to the
     * cutting rule that generated APs belonging to this class.
     */
    private String rule;
    
    /**
     * The secondary feature of the APClass. This would usually distinguish 
     * the two asymmetric sides of a bond but upon fragmentation
     */
    private int subClass;

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty APClass
     */
    public APClass() {
    }

//------------------------------------------------------------------------------

    /**
     * Constructor for a fully defined APClass
     * @throws DENOPTIMException 
     */
    public APClass(String ruleAndSunClass) throws DENOPTIMException {
        
        if (!isValidAPClassString(ruleAndSunClass))
        {
            throw new DENOPTIMException("Attempt to use APClass '" 
                        + ruleAndSunClass
                        + "' that does not respect syntax <rule>"
                        + DENOPTIMConstants.SEPARATORAPPROPSCL + "<subClass>.");
        }
        String[] parts = ruleAndSunClass.split(
                DENOPTIMConstants.SEPARATORAPPROPSCL);
        this.rule = parts[0];
        this.subClass = Integer.parseInt(parts[1]);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for a fully defined APClass
     * @throws DENOPTIMException 
     */
    public APClass(String rule, int subClass) throws DENOPTIMException {
        if (isValidAPRuleString(rule)) {        
            this.rule = rule;
            this.subClass = subClass;
        } else {
            throw new DENOPTIMException("Invalid sttempt to make APClass out "
                    + "of '" + rule + "' and '" + subClass + "'.");
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return the main part of this APClass, which typically corresponds to 
     * name of the fragmentation rule that generated attachment point with this
     * APClass.
     */
    public String getRule() {
        return rule;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return a string with a format compatible with reporting APClasses in
     * text files (e.g., compatibility matrix files) and SDF files (e.g., 
     * SDF with fragments).
     */
    public String toString() {
        return rule + DENOPTIMConstants.SEPARATORAPPROPSCL 
        + Integer.toString(subClass);
    }

//------------------------------------------------------------------------------
    
    /**
     * Evaluate the given string as a candidate for attachment point subclass,
     * i.e., the attachment point class component discriminating the two sides 
     * of a bond being broken to yield two attachment points.
     * @param s the string to evaluate
     * @return <code>true</code> if the given string can be used as 
     */
    
    public static boolean isValidAPSubCLassString(String s)
    {
        return s.matches("^[0-9]*$");
    }
    
//------------------------------------------------------------------------------

    /**
     * Evaluates the given string as a candidate attachment point rule, i.e., 
     * as name of a fragmentation rule that generates attachment points.
     * @param s the string to evaluate
     * @return <code>true</code> if the given string can be used as attachment
     * point rule.
     */
    
    public static boolean isValidAPRuleString(String s)
    {
        if (s == null)
            return false;
        return s.matches("^[a-zA-Z0-9_-]+$");
    }

//------------------------------------------------------------------------------
    
    /**
     * Evaluate is a candidate string can be used as APClass. This method checks
     * whether the string reflects the expected syntax of an APClass string
     * @return <code>true</code> if the given string can be used as attachment
     * point class.
     */
    
    public static boolean isValidAPClassString(String s)
    {
    	if (!s.matches("^[a-z,A-Z,0-9].*"))
    		return false;
    	
    	if (!s.matches(".*[0-9]$"))
    		return false;
    	
    	if (s.contains(" "))
    		return false;
    	
    	if (!s.contains(DENOPTIMConstants.SEPARATORAPPROPSCL))
    		return false;
    	
    	int numSep = 0;
    	for (int i=0; i<s.length(); i++)
    	{
    		if (s.charAt(i) == DENOPTIMConstants.SEPARATORAPPROPSCL.charAt(0))
    		{
    			numSep++;
    		}
    	}
    	if (numSep != 1)
    	    return false;
    	
    	String apRule = s.split(DENOPTIMConstants.SEPARATORAPPROPSCL)[0];
    	String sub = s.split(DENOPTIMConstants.SEPARATORAPPROPSCL)[1];
        
        return APClass.isValidAPRuleString(apRule) 
                && APClass.isValidAPSubCLassString(sub);
    }

//------------------------------------------------------------------------------
    
    @Override
    public int compareTo(APClass o)
    {
        int ruleComparison = this.rule.compareTo(o.rule);
        if (ruleComparison == 0) {
            return Integer.compare(this.subClass, o.subClass);
        } else {
            return ruleComparison;
        }
    }
    
//------------------------------------------------------------------------------
    
    @Override
    public APClass clone() {
        APClass c = null;
        try
        {
            c = new APClass(rule,subClass);
        } catch (DENOPTIMException e)
        {
            //This cannot happen upon cloning
        }
        return c;
    }
    
//------------------------------------------------------------------------------
    
    public boolean equals(APClass o) {
        return this.rule.equals(o.rule) && this.subClass == o.subClass;
    }
    
    
//------------------------------------------------------------------------------
    
}
    
