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
    
    /**
     * Allowed bridge length in number of atoms. This is the allowed length of 
     * the new bridge this rule allows to define between the bridge-head atoms.
     * The length is given in number of actual atoms (no RCA included).
     */
    private int[] allowedBridgeLength;
    
    /**
     * Number of atoms in the existing bridge connecting the bridge-head atoms, 
     * including the bridge-head atoms.
     */
    private int lengthInAtoms;


//------------------------------------------------------------------------------

    /**
     * Constructs a new rule defined by the given arguments.
     * @param name the string identifying the name of this rule.
     * @param smarts the SMARTS string meant to match substructures that
     * pertain this rule.
     * @param bridgeHeadPositions the identification of potential bridge-head
     * atoms in the substructure matched by the SMARTS query.
     * @param lengthInAtoms the number of atoms in the existing bridge 
     * connecting the bridge-head atoms including the bridge-head atoms.
     */
    public BridgeHeadFindingRule(String name, String smarts, 
            int[] bridgeHeadPositions, int[] allowedBridgeLength,
            int lengthInAtoms)
    {
        this.ruleName = name;
        this.smarts = smarts;
        this.bridgeHeadPositions = bridgeHeadPositions;
        this.allowedBridgeLength = allowedBridgeLength;
        this.lengthInAtoms = lengthInAtoms;
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
    
    /**
     * @return the allowed length of 
     * the new bridge this rule allows to define between the bridge-head atoms.
     * This can be <code>null</code>.
     */
   public int[] getAllowedBridgeLength()
   {
       return allowedBridgeLength;
   }
   
 //------------------------------------------------------------------------------
   
   /**
    * @return the allowed length of 
    * the new bridge this rule allows to define between the bridge-head atoms.
    * This cannot be <code>null</code> because if this rule does not specify 
    * any allowed bridge length, then this method generated a return value that 
    * corresponds to saying "any bridge length is allowed as long as it leads 
    * to a ring with size smaller than the given maximum size".
    */
  public int[] getAllowedBridgeLength(int maxRingSize)
  {
      if (allowedBridgeLength!=null)
          return allowedBridgeLength;

      // Any bridge length is allowed
      int[] anyLength = new int[maxRingSize - lengthInAtoms];
      for (int i=1; i<anyLength.length; i++) {
          anyLength[i-1] = i;
      }
      return anyLength;
  }
   
//------------------------------------------------------------------------------
   
   /**
    * @return the number of atoms in the existing bridge, i.e., the number of 
    * atoms between the bridge-head atoms plus 2 (we count also the 
    * bridge-head atoms.
    */
   public int getExistingBridgeLength()
   {
       return lengthInAtoms;
   }

//------------------------------------------------------------------------------

}
