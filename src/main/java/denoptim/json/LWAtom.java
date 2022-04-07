package denoptim.json;

import javax.vecmath.Point3d;

import org.openscience.cdk.Atom;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.interfaces.IAtom;

import denoptim.utils.MoleculeUtils;

/**
 * A light-weight atom representation to facilitate json serialization of 
 * {@link IAtom}.
 *  
 * @author Marco Foscato
 */

public class LWAtom
{
    /**
     * The elemental symbol of this center, i.e., an atom or a pseudo-atom.
     */
    protected String elSymbol = "";
    
    /**
     * Cartesian coordinates of this center
     */
    protected Point3d p3d = null;
    
//------------------------------------------------------------------------------
    
    /**
     * Constructor
     * @param elSymbol the elemental symbol
     * @param p3d the Cartesian coordinated of this center
     */
    public LWAtom(String elSymbol, Point3d p3d)
    {
        this.elSymbol = elSymbol;
        this.p3d = p3d;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns a CDK representation of this center.
     * @return a CDK representation of this center.
     */
    public IAtom toIAtom()
    {
        IAtom atm = null;
        if (MoleculeUtils.isElement(elSymbol))
        {
            atm = new Atom(elSymbol);
        } else {
            atm = new PseudoAtom(elSymbol);
        }
        atm.setPoint3d(new Point3d(p3d));
        return atm;
    }
    
//------------------------------------------------------------------------------
    
}
