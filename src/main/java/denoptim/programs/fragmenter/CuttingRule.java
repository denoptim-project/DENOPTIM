package denoptim.programs.fragmenter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openscience.cdk.ChemObject;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IRingSet;
import org.openscience.cdk.isomorphism.Mappings;
import org.openscience.cdk.ringsearch.AllRingsFinder;
import org.openscience.cdk.tools.periodictable.PeriodicTable;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.graph.APClass;
import denoptim.utils.ManySMARTSQuery;
import denoptim.utils.MoleculeUtils;

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
     * @throws DENOPTIMException 
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
        try
        {
            apc0 = APClass.make(ruleName + DENOPTIMConstants.SEPARATORAPPROPSCL
                    + "0");
            apc1 = APClass.make(ruleName + DENOPTIMConstants.SEPARATORAPPROPSCL 
                    + "1");
        } catch (DENOPTIMException e)
        {
            //cannot happen
        }
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
    
    /**
     * Checks if a given bond satisfies the additional options of this rule
     * beyond the matching of the SMARTS queries.
     * @param matchedBond the bond to test.
     * @param logger where any log should be directed.
     * @return <code>true</code> if the bond satisfies the condition.
     */
    public Boolean satisfiesOptions(MatchedBond matchedBond, Logger logger)
    {
        IAtom atmS = matchedBond.getAtmSubClass0();
        IAtom atmT = matchedBond.getAtmSubClass1();
        IAtomContainer mol = atmS.getContainer();
        int idxSInMol = mol.indexOf(atmS);
        int idxTInMol = mol.indexOf(atmT);
        
        boolean hasHapto = false;
        boolean checkRings = false;
        int minSzRing = -1;
        boolean checkOMRings = false;
        int minSzOMRing = -1;
        for (String opt : opts)
        {
            if (opt.startsWith("HAPTO"))
                hasHapto = true;
            
            if (opt.startsWith("RING>"))
            {
                checkRings = true;
                minSzRing = Integer.parseInt(opt.replace("RING>","").trim());
                
            } else if (opt.startsWith("OMRING"))
            {
                checkOMRings = true;
                minSzOMRing = Integer.parseInt(opt.replace("OMRING>","").trim());
            }
        }
        
        // from here it is all about OM/Ring, and these are both incompatible 
        // with HAPTO. So if we have HAPTO we should not test OM/Ring-options
        if (hasHapto)
            return true;
        
        // Build SMARTS queries for rings as large as needed to test criteria
        Map<String,String> allSmarts = new HashMap<String,String>();
        StringBuilder smartsBuilder = new StringBuilder();
        // "ring size" i = 1
        smartsBuilder.append(this.getSMARTSAtom0());
        smartsBuilder.append("1");
        smartsBuilder.append(this.getSMARTSBnd());
        // "ring size" i=2
        smartsBuilder.append(this.getSMARTSAtom1());
        smartsBuilder.append("~");
        // "ring size" from 3 and on
        for (int i=3; i<Math.max(minSzRing, minSzOMRing)+1; i++)
        {
            smartsBuilder.append("[*]~");
            allSmarts.put("ring"+i, smartsBuilder.toString()+"1");
        }
        
        // Find all rings matching the queries
        ManySMARTSQuery msq = new ManySMARTSQuery(mol, allSmarts);
        if (msq.hasProblems())
        {
            if (logger!=null)
            {
                logger.log(Level.WARNING, "Problem matching SMARTS for OM/RING "
                        + "options. Ignoring bond for which we cannot check if "
                        + "we satisfy OM/RING options. " + msq.getMessage());
            }
            return false;
        }
        
        for (int ringSize=3; ringSize<Math.max(minSzRing, minSzOMRing)+1; ringSize++)
        {
            String smartsName = "ring"+ringSize;
            if (msq.getNumMatchesOfQuery(smartsName) == 0)
            {
                continue;
            }
            
            // Get atoms matching cutting rule queries
            Mappings atomsInAllRings = msq.getMatchesOfSMARTS(smartsName);
            for (int[] atmsInOneRing : atomsInAllRings) 
            {
                if (atmsInOneRing[0]==idxSInMol && atmsInOneRing[1]==idxTInMol)
                {
                    boolean isOMRing = false;
                    for (int j=0; j<atmsInOneRing.length; j++)
                    {
                        IAtom atmInRing = mol.getAtom(atmsInOneRing[j]);
                        
                        if (MoleculeUtils.isElement(atmInRing) 
                                && DENOPTIMConstants.ALL_METALS.contains(
                                        atmInRing.getSymbol()))
                        {
                            isOMRing = true;
                            break;
                        }
                    }
                    if (!isOMRing && checkRings && ringSize<=minSzRing)
                    {

                        logger.log(Level.FINEST,"Bond between " + idxSInMol 
                                + " and " + idxTInMol + " matches SMARTS of "
                                + "cutting rule '" + this.ruleName 
                                + "', but does not satisfy "
                                + "RING>" + minSzRing + " as it is part of "
                                + "a " + ringSize + "-member organic-only ring.");
                        return false;
                    }
                    if (isOMRing && checkOMRings && ringSize<=minSzOMRing)
                    {
                        logger.log(Level.FINEST,"Bond between " + idxSInMol 
                                + " and " + idxTInMol + " matches SMARTS of "
                                + "cutting rule '" + this.ruleName 
                                + "', but does not satisfy "
                                + "OMRING>" + minSzOMRing + " as it is part of "
                                + "a " + ringSize + "-member ring including a "
                                + "metal.");
                        return false;
                    }
                }
            }
        }
        return true;
    }

//------------------------------------------------------------------------------

}
