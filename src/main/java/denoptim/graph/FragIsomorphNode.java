package denoptim.graph;

import org.openscience.cdk.interfaces.IAtom;

import denoptim.utils.MoleculeUtils;

public class FragIsomorphNode
{
    /**
     * Elemental symbol or name used to identify the node content.
     */
    String label = "none";
    
    /**
     * Reference to the object that originally generated this node.
     */
    Object original;
    
    /**
     * <code>true</code> if the original object was an atom.
     */
    boolean isAtm = true;

//------------------------------------------------------------------------------
    
    public FragIsomorphNode(IAtom atm)
    {
        this.original = atm;
        this.label = MoleculeUtils.getSymbolOrLabel(atm);
        this.isAtm = true;
    }
  
//------------------------------------------------------------------------------
    
    public FragIsomorphNode(AttachmentPoint ap)
    {
        this.original = ap;
        this.label = ap.getAPClass().toString();
        this.isAtm = false;
    }

//------------------------------------------------------------------------------
    
    public String getLabel()
    {
        return label;
    }

//------------------------------------------------------------------------------
    
    public Object getOriginal()
    {
        return original;
    }

//------------------------------------------------------------------------------
    
    public boolean isAtm()
    {
        return isAtm;
    }

//------------------------------------------------------------------------------
    
}
