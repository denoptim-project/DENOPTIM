package denoptim.graph;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.graph.DENOPTIMEdge.BondType;

public class APClass implements Cloneable,Comparable<APClass>
{   
    /**
     * Version UID
     */
    private static final long serialVersionUID = 3L;

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
   
    /**
     * Set unique APClasses
     */
    public static Set<APClass> uniqueAPClasses = new HashSet<APClass>();
    
    /**
     * Synchronisation lock. Used to guard alteration of the set of unique
     * APClasses.
     */
    private final static Object uniqueAPClassesLock = new Object();

    /**
     * Recognised attachment point classes of RingClosingAttractor
     */
    public static final Set<APClass> RCAAPCLASSSET = 
    	    new HashSet<APClass>(){{
    	        APClass a = getUnique("ATplus", 0, BondType.ANY);
    	        add(a);
    	        synchronized (uniqueAPClassesLock)
                {
    	            uniqueAPClasses.add(a);
                }
    	        
    	        APClass b = getUnique("ATminus", 0, BondType.ANY);
                add(b);
                synchronized (uniqueAPClassesLock)
                {
                    uniqueAPClasses.add(b);
                }
                
                APClass c = getUnique("ATneutral", 0, BondType.ANY);
                add(c);
                synchronized (uniqueAPClassesLock)
                {
                    uniqueAPClasses.add(c);
                }
                }};
    
    /**
     * Bond type to use when converting edge users into formal bonds
     */
    private BondType bndTyp = BondType.UNDEFINED; //default for all but RCAs

//------------------------------------------------------------------------------

    /**
     * Constructor for an empty APClass
     */
    public APClass() 
    {}

//------------------------------------------------------------------------------

    /**
     * Creates an APClass if it does not exist already, or returns the 
     * reference to the existing instance.
     */
    public static APClass make(String ruleAndSubclass) throws DENOPTIMException 
    { 
        if (!isValidAPClassString(ruleAndSubclass))
        {
            throw new DENOPTIMException("Attempt to use APClass '" 
                        + ruleAndSubclass
                        + "' that does not respect syntax <rule>"
                        + DENOPTIMConstants.SEPARATORAPPROPSCL + "<subClass>.");
        }
        String[] parts = ruleAndSubclass.split(
                DENOPTIMConstants.SEPARATORAPPROPSCL);
        return make(parts[0], Integer.parseInt(parts[1]));
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for an APClass with default bond type (i.e., 
     * {@link BondType#UNDEFINED}).
     * Checks if there is already a instance with the given rule name and 
     * subclass, if not it 
     * created one. In either case, returns the reference to that instance of 
     * APClass.
     * @param rule the APClass rule, i.e., a string identifier that typically
     * corresponds to the name of the cutting rule used to break a bond.
     * @param subClass the integer identifier of the "side" of the bond broken
     * to make an attachment point.
     * @throws DENOPTIMException 
     */
    public static APClass make(String rule, int subClass) 
            throws DENOPTIMException 
    {
        //TODO-gg this is only for converting files to V3
        if (FragmentSpace.isDefined())
        {
            if (FragmentSpace.getBondOrderMap().containsKey(rule))
            {
                return make(rule, subClass, FragmentSpace.getBondOrderMap().get(rule));
            } else {
                return make(rule, subClass, BondType.UNDEFINED);
            }
        } else {
            return make(rule, subClass, BondType.UNDEFINED);
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor for a fully defined APClass. 
     * Checks if there is already a instance with the given members, if not it 
     * created one. In either case, returns the reference to that instance of 
     * APClass.
     * @param rule the APClass rule, i.e., a string identifier that typically
     * corresponds to the name of the cutting rule used to break a bond.
     * @param subClass the integer identifier of the "side" of the bond broken
     * to make an attachment point.
     * @param bt the bond type to be used when converting edges using APs of
     * this APClass into bonds, if any.
     * @throws DENOPTIMException 
     */
    public static APClass make(String rule, int subClass, BondType bt) 
            throws DENOPTIMException 
    {
        if (isValidAPRuleString(rule)) {        
            return getUnique(rule, subClass, bt);
        } else {
            throw new DENOPTIMException("Invalid sttempt to make APClass out "
                    + "of '" + rule + "' and '" + subClass + "'.");
        }
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Checks if there is already a instance with the given members, if not it 
     * created one. In either case, returns the reference to that instance of 
     * APClass.
     * @param rule
     * @param subClass
     * @return reference to the APClass instance with the given members.
     */
    private static APClass getUnique(String rule, int subClass, BondType bt)
    {
        APClass newApc = new APClass();
        synchronized (uniqueAPClassesLock)
        {
            boolean found = false;
            for (APClass existingApc : uniqueAPClasses)
            {
                if (existingApc.getRule().equals(rule)
                        && existingApc.getSubClass()==subClass)
                {
                    newApc = existingApc;
                    found = true;
                    break;
                }
            }
            if (!found)
            {
                newApc.setRule(rule);
                newApc.setSubClass(subClass);
                newApc.setBondType(bt);
            } else {
                // NB: the default bond type for RCAs is different, so we do
                // not overwrite it.
                if (bt != newApc.bndTyp && !RCAAPCLASSSET.contains(newApc))
                {
                    System.out.println("WARNING! Changing bond order of "
                            + "APClass " + newApc + " (" + newApc.bndTyp 
                            + ") with " + bt);
                    newApc.setBondType(bt);
                }
            }
            uniqueAPClasses.add(newApc);
        }
        return newApc;
    }
    
//------------------------------------------------------------------------------
    
    private void setBondType(BondType bt) {
        this.bndTyp = bt;
    }

//------------------------------------------------------------------------------
    
    private void setRule(String rule) {
        this.rule = rule;
    }
    
//------------------------------------------------------------------------------
    
    private void setSubClass(int sumClass) {
        this.subClass = sumClass;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the list of the names of all APClasses.
     * @return
     */
    public static List<String> getAllAPClassesAsString()
    {
        List<String> names = new ArrayList<String>();
        for (APClass apc : uniqueAPClasses)
        {
            names.add(apc.toString());
        }
        Collections.sort(names);
        return names;
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
     * @return the secondary part of this APClass, which typically corresponds
     * to the discriminating factor that distinguishes between asymmetric 
     * fragments.
     */
    public int getSubClass() {
        return subClass;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return the bond type associated with this APClass
     */
    public BondType getBondType() {
        return bndTyp;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return a string with a format compatible with reporting APClasses in
     * text files (e.g., compatibility matrix files) and SDF files (e.g., 
     * SDF with fragments).
     */
    //TODO-gg avoid to use it to make SDF strings
    @Deprecated 
    public String toString() {
        return rule + DENOPTIMConstants.SEPARATORAPPROPSCL 
        + Integer.toString(subClass);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return a string with a format compatible with reporting APClasses in
     * text files (e.g., compatibility matrix files) and SDF files (e.g., 
     * SDF with fragments).
     */
    public String toSDFString() {
        return rule + DENOPTIMConstants.SEPARATORAPPROPSCL 
                + Integer.toString(subClass)
                + DENOPTIMConstants.SEPARATORAPPROPSCL
                + bndTyp;
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
    
    /**
     * Check compatibility as defined in the compatibility matrix considering
     * this AP as source and the other as target.
     * @param other
     * @return <code>true</code> is APs of these two classes are allowed to
     * form new vertex-vertex connections where this AP is source and other is
     * target.
     */
    public boolean isCPMapCompatibleWith(APClass other)
    {
        return FragmentSpace.getCompatibleAPClasses(this).contains(other);
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
    
    /**
     * WARNING: this method does NOT clone! It just returns the reference to 
     * this. We have this method to avoid any attempt to actual cloning of an 
     * APClass.
     */
    @Override
    public APClass clone() {
        return this;
    }
    
//------------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof APClass)) {
            return false;
        }
        APClass c = (APClass) o;
        return this.hashCode() == o.hashCode();
    }
    
//------------------------------------------------------------------------------

    public static class APClassDeserializer 
    implements JsonDeserializer<APClass>
    {
        @Override
        public APClass deserialize(JsonElement json, Type typeOfT,
                JsonDeserializationContext context) throws JsonParseException
        {
            JsonObject jo = json.getAsJsonObject();
            APClass apc = null;
            if (jo.has("bndTyp"))
            {
                apc = getUnique(jo.get("rule").getAsString(),
                    jo.get("subClass").getAsInt(),
                    context.deserialize(jo.get("bndTyp"),BondType.class));
            } else {
                //TODO-gg possibly only for conversion to V3
                String rule = jo.get("rule").getAsString();
                int subClass = jo.get("subClass").getAsInt();
                boolean found = false;
                for (APClass existingApc : uniqueAPClasses)
                {
                    if (existingApc.getRule().equals(rule)
                            && existingApc.getSubClass()==subClass)
                    {
                        apc = existingApc;
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    System.out.println("WARNING! Setting UNDEFINED for "+rule+":"+subClass);
                    apc = getUnique(jo.get("rule").getAsString(),
                        jo.get("subClass").getAsInt(),
                        BondType.UNDEFINED);
                }
            }
            return apc;
        }
    }

//------------------------------------------------------------------------------

}
    
