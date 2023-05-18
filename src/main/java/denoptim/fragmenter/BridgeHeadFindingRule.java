package denoptim.fragmenter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.isomorphism.Mappings;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.graph.APClass;
import denoptim.utils.ManySMARTSQuery;
import denoptim.utils.MoleculeUtils;

/**
 * SMARTS-based rules to identify potential bridge head atoms for ring fusion 
 * operations.
 * @author Marco Foscato 
 */

public class BridgeHeadFindingRule
{
    /**
     * Rule name. Usually a human-readable string giving a hint on what this 
     * rule is supposed to identify or represent.
     */
    private String ruleName;

    /**
     *  SMARTS query matching the substructure of interest 
     */
    private String smarts;
    
    /**
     * The indexes of the atoms that can be bridge head in the matches 
     * substructure.
     */
    private int[] bridgeHeadPositions;


//------------------------------------------------------------------------------

    /**
     * Constructs a new rule defined by the given arguments.
     * @param name the string identifying the name of this rule.
     * @param smarts the SMARTS string meant to match substructures that
     * pertain this rule.
     * @param bridgeHeadPositions the identification of potential bridge-head
     * atoms in the substructure matched by the SMARTS query.
     */
    public BridgeHeadFindingRule(String name, String smarts, 
            int[] bridgeHeadPositions)
    {
        this.ruleName = name;
        this.smarts = smarts;
        this.bridgeHeadPositions = bridgeHeadPositions;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the name of this rule.
     */
    public String getName()
    {
        return ruleName;
    }    

//------------------------------------------------------------------------------
    
    /**
     * @return the SMARTS for matching substructures.
     */
   public String getSMARTS()
   {
       return smarts;
   }
  
//------------------------------------------------------------------------------
     
     /**
      * @return the positions of the potential bridge head atoms in the
      * substructure.
      */
    public int[] getBridgeHeadPositions()
    {
        return bridgeHeadPositions;
    }

//------------------------------------------------------------------------------

}
