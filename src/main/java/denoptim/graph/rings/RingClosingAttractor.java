/*
 *   DENOPTIM
 *   Copyright (C) 2019 Marco Foscato <marco.foscato@uib.no>
 * 
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package denoptim.graph.rings;

import java.util.HashMap;
import java.util.Map;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.graph.APClass;
import denoptim.graph.Edge.BondType;
import denoptim.graph.Ring;
import denoptim.utils.MoleculeUtils;


/**
 * The RingClosingAttractor represent the available valence/connection that
 * allows to close a ring. The RingClosingAttractor is a fragment that
 * has one real attachment point and a virtual one reserved for forming a
 * new bond with another RingClosingAttractor that belongs to
 * the same atom chain thus forming a ring.
 *
 * @author Marco Foscato 
 */

public class RingClosingAttractor
{
    /**
     * Parameter A for points in 1,1 relationship
     */
    final Map<String,Double> paramAR11 = new HashMap<String,Double>() {
        /**
         * Version ID
         */
        private static final long serialVersionUID = 1L;

        {
            put("ATP", 1.0);
            put("ATM", 1.0);
            put("ATN", 1.0);
        };
    };

    /**
     * Parameter B for points in 1,1 relationship
     */
    final Map<String,Double> paramBR11 = new HashMap<String,Double>() {
        /**
         * Version ID
         */
        private static final long serialVersionUID = 1L;
        {
            put("ATP", 1.0);
            put("ATM", 1.0);
            put("ATN", 1.0);
        };
    };

    /**
     * Parameter A for points in 1,2 relationship
     */
    final Map<String,Double> paramAR12 = new HashMap<String,Double>() {
        /**
         * Version ID
         */
        private static final long serialVersionUID = 1L;
        {
            put("ATP", 5.0);
            put("ATM", 5.0);
            put("ATN", 5.0);
        };
    };

    /**
     * Parameter B for points in 1,2 relationship
     */
    final Map<String,Double> paramBR12 = new HashMap<String,Double>() {
        /**
         * Version ID
         */
        private static final long serialVersionUID = 1L;
        {
            put("ATP", 7.0);
            put("ATM", 7.0);
            put("ATN", 7.0);
        };  
    };

    /**
     * Parameter A for 1,1 interaction
     */
    private Double attA11 = 0.0;
 
    /**
     * Parameter B for 1,1 interaction
     */
    private Double attB11 = 0.0;

    /**
     * Parameter A for 1,2 interaction
     */
    private Double attA12 = 0.0;

    /**
     * Parameter B for 1,2 interaction
     */
    private Double attB12 = 0.0;

    /**
     * Type of this attractor
     */
    private String attType = "none";

    /**
     * Pseudo atom representing RingClosingAttractor in molecule
     */
    private IAtom atm;

    /**
     * Atom hosting the RingClosingAttractor
     */
    private IAtom src;

    /**
     * Class of the Attachment Point represented by this RCA
     */
    private APClass apClass;
    
    /**
     * Type of ring-closing bond that this ring-closing attractor is there to 
     * create.
     */
    private BondType bndTyp;
    
    /**
     * Reference to the graph {@link Ring} that represent the intention to 
     * close a ring of vertices in DENOPTIM's graph representation of the
     * chemical object.
     */
    private Ring ringUser;

    /**
     * Flag: this RingClosingAttractor is used to close a ring
     */ 
    private boolean used = false;


//-----------------------------------------------------------------------------

    /**
     * Constructor for an empty RingClosingAttractor
     */

    public RingClosingAttractor()
    {
        this.atm = null;
    }

//-----------------------------------------------------------------------------

    /**
     * Constructor for a {@link RingClosingAttractor} corresponding to an atom 
     * in an {@link IAtomContainer}. 
     * Note that any atom can be used to construct a 
     * {@link RingClosingAttractor},
     * but only the elemental symbol stored in 
     * {@link DENOPTIMConstants#RCATYPEMAP} are recognized and trigger the 
     * perception of the candidate atom as a {@link RingClosingAttractor}.
     * In which case the {@link #isAttractor()} method will return 
     * <code>true</code>.
     * @param atm the atom candidate to become a {@link RingClosingAttractor}.
     * @param mol the molecule containing the candidate atom.
     */

    public RingClosingAttractor(IAtom atm, IAtomContainer mol)
    {
        this.atm = atm;
        for (String atyp : DENOPTIMConstants.RCATYPEMAP.keySet())
        {
            if (MoleculeUtils.getSymbolOrLabel(atm).equals(atyp))
            {
                this.attType = atyp;
                this.attA11 = paramAR11.get(atyp);
                this.attA12 = paramAR12.get(atyp);
                this.attB11 = paramBR11.get(atyp);
                this.attB12 = paramBR12.get(atyp);
                break;
            }
        }
        // We can try to build RCAs from any atom, but only those atoms that
        // really are RCAs do have the information we are extracting here
        if (isAttractor())
        {
            //Well, any atom might have neighbors, but RCA only have 1. So
            // the assumption is valid for RCAs.
            this.src = mol.getConnectedAtomsList(atm).get(0);
            // But, there are properties that can be found only in relation to
            // actual RCAs (whether used to make a ring, or not)
            this.apClass = (APClass) atm.getProperty(
                    DENOPTIMConstants.RCAPROPAPCTORCA);
            this.bndTyp = (BondType) atm.getProperty(
                    DENOPTIMConstants.RCAPROPCHORDBNDTYP);
            this.ringUser = (Ring) atm.getProperty(
                    DENOPTIMConstants.RCAPROPRINGUSER);
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Checks whether the constructed RingClosingAttractor does corresponds
     * to a RingClosingAttractor in the molecular representation.
     * @return <code>true</code> if this is a real RingClosingAttractor
     */

    public boolean isAttractor()
    {
        return (this.attType.equals("none")) ? false : true;
    }

//-----------------------------------------------------------------------------

    /**
     * Evaluate compatibility between this RingClosingAttractor and another one
     * @return <code>true</code> is this and the other RingClosingAttractor are
     * compatible.
     */

    public boolean isCompatible(RingClosingAttractor other)
    {
        return DENOPTIMConstants.RCATYPEMAP.get(this.attType).equals(
								other.attType);
    }

//-----------------------------------------------------------------------------

    /**
     * Get the type of RingClosingAttractor.
     * @return the type of RingClosingAttractor.
     */

    public String getType()
    {
        return attType;
    }

//-----------------------------------------------------------------------------

    /**
     * Get the atom corresponding to this RingClosingAttractor in the molecular
     * representation.
     * @return the dummy atom
     */

    public IAtom getIAtom()
    {
        return atm;
    }
    
//-----------------------------------------------------------------------------

    /**
     * Change the reference to the atom in the molecular representation.
     * @param atm the new reference
     */

    public void setIAtom(IAtom atm)
    {
        this.atm = atm;
    }

//-----------------------------------------------------------------------------

    /**
     * Get the atom in the parent fragment that holds the attachment point
     * occupied by this RingClosingAttractor.
     * @return the atom to which this attractor is attached
     */

    public IAtom getSrcAtom()
    {
        return src;
    }

//-----------------------------------------------------------------------------

    /**
     * Get the class of the Attachment Point occupied (i.e., in the parent 
     * fragment) by this RingClosingAttractor, if any.
     * @return the attachment point class
     */

    public APClass getApClass()
    {
        return apClass;
    }
    
 //-----------------------------------------------------------------------------

    /**
     * Get the type of bond this attractor is meant to close.
     * @return the type of ring-closing bond.
     */

    public BondType getRCBondType()
    {
        return bndTyp;
    }
    
 //-----------------------------------------------------------------------------

    /**
     * Get the reference to the graph representation of the ring this attractor
     * is meant to close.
     * @return the ring in the graph representation, or null.
     */

    public Ring getRingUser()
    {
        return ringUser;
    }

//-----------------------------------------------------------------------------

    /**
     * Set this RingClosingAttractor to 'used'. Once the 'used' flag is set
     * this RingClosingAttractor cannot be used to close rings any more
     */

    public void setUsed()
    {
        this.used = true;
    }

//-----------------------------------------------------------------------------

    /**
     * Check if this RingClosingAttractor has been used to
     * close a ring and is not available any more.
     * @return <code>true</code> if already used
     */

    public boolean isUsed()
    {
        return used;
    }

//-----------------------------------------------------------------------------

    /** 
     * @return the parameter A for interactions with atoms/RCA in 1,1
     * relationship.
     */

    public Double getParamA11()
    {
        return attA11;
    }

//-----------------------------------------------------------------------------

    /** 
     * @return the parameter B for interactions with atoms/RCA in 1,1
     * relationship.
     */

    public Double getParamB11()
    {
        return attB11;
    }

//-----------------------------------------------------------------------------

    /** 
     * @return the parameter A for interactions with atoms/RCA in 1,2
     * relationship.
     */

    public Double getParamA12()
    {
        return attA12;
    }

//-----------------------------------------------------------------------------

    /** 
     * @return the parameter B for interactions with atoms/RCA in 1,2
     * relationship.
     */

    public Double getParamB12()
    {
        return attB12;
    }

//-----------------------------------------------------------------------------

    /**
     * @return the string representation of this attractor
     */

    @Override public String toString()
    {
        String s = "RingClosingAttractor (Type:" + this.attType + " "
                    + "APClass:" + this.apClass + " "
                    + "Used:" + this.used + " "
                    + "Atm: " + MoleculeUtils.getSymbolOrLabel(this.atm) 
                    + " SrcAtm: " + MoleculeUtils.getSymbolOrLabel(this.src)
                    + ")";
        return s;
    }

//-----------------------------------------------------------------------------
}
