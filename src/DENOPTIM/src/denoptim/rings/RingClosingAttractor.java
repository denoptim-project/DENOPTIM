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

package denoptim.rings;

import java.util.Map;
import java.util.HashMap;

import org.openscience.cdk.Atom;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.molecule.APClass;


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
     * Parameter A for points in 11 relationship
     */
    final Map<String,Double> paramAR11 = new HashMap<String,Double>() {
        {
            put("ATP", 1.0);
            put("ATM", 1.0);
            put("ATN", 1.0);
        };
    };

    /**
     * Parameter B for points in 11 relationship
     */
    final Map<String,Double> paramBR11 = new HashMap<String,Double>() {
        {
            put("ATP", 1.0);
            put("ATM", 1.0);
            put("ATN", 1.0);
        };
    };

    /**
     * Parameter A for points in 12 relationship
     */
    final Map<String,Double> paramAR12 = new HashMap<String,Double>() {
        {
            put("ATP", 5.0);
            put("ATM", 5.0);
            put("ATN", 5.0);
        };
    };

    /**
     * Parameter B for points in 12 relationship
     */
    final Map<String,Double> paramBR12 = new HashMap<String,Double>() {
        {
            put("ATP", 7.0);
            put("ATM", 7.0);
            put("ATN", 7.0);
        };  
    };

    /**
     * Parameter A for 1-1 interaction
     */
    private Double attA11 = 0.0;
 
    /**
     * Parameter B for 1-1 interaction
     */
    private Double attB11 = 0.0;

    /**
     * Parameter A for 1-2 interaction
     */
    private Double attA12 = 0.0;

    /**
     * Parameter B for 1-2 interaction
     */
    private Double attB12 = 0.0;

    /**
     * Type of this attractor
     */
    private String attType = "none";

    /**
     * Pseuso atom representing RingClosingAttractor in molecule
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
     * Flag: this RingClosingAttractor is used to close a ring
     */ 
    private boolean used = false;

    // Verbosity level
    private static int verbosity = RingClosureParameters.getVerbosity();


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
     * Constructor for a RingClosingAttractor corresponding to an atom in a
     * molecular object. Note that any atom can be used to contuct a
     * RingClosingAttractor but only those deriving from ring closing fragments
     * (which are identified by the atom symbol) are real RingClosingAttractor.
     * After creation it is then suggested to check whether the resulting
     * object is a real RingClosingAttractor by the use of method
     * {@link #isAttractor() isAttractor}.
     *
     * @param atm the selected atom 
     * @param mol the molecule containg the atom
     */

    public RingClosingAttractor(IAtom atm, IAtomContainer mol)
    {
        this.atm = atm;
        this.src = mol.getConnectedAtomsList(atm).get(0);
        for (String atyp : DENOPTIMConstants.RCATYPEMAP.keySet())
        {
            if (atm.getSymbol().equals(atyp))
            {
                this.attType = atyp;
                this.attA11 = paramAR11.get(atyp);
                this.attA12 = paramAR12.get(atyp);
                this.attB11 = paramBR11.get(atyp);
                this.attB12 = paramBR12.get(atyp);
                break;
            }
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
        return this.attType;
    }

//-----------------------------------------------------------------------------

    /**
     * Get the atom corresponding to this RingClosingAttractor in the molecular
     * representation.
     * @return the dummy atom
     */

    public IAtom getIAtom()
    {
        return this.atm;
    }

//-----------------------------------------------------------------------------

    /**
     * Get the atom in the parent fragment that holds the attachment point
     * occupied by this RingClosingAttractor.
     * @return the atom to which this attractor is attached
     */

    public IAtom getSrcAtom()
    {
        return this.src;
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
     * Set the class of the Attachment Point occupied by this 
     * RingClosingAttractor, if any.
     * @param apclass2 the string format of the attachment point class
     */

    public void setApClass(APClass apclass2)
    {
        this.apClass = apclass2;
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
        return this.used;
    }

//-----------------------------------------------------------------------------

    /** 
     * @return the parameter A for interactions with atoms/RCA in 1-1
     * relationship.
     */

    public Double getParamA11()
    {
        return attA11;
    }

//-----------------------------------------------------------------------------

    /** 
     * @return the parameter B for interactions with atoms/RCA in 1-1
     * relationship.
     */

    public Double getParamB11()
    {
        return attB11;
    }

//-----------------------------------------------------------------------------

    /** 
     * @return the parameter A for interactions with atoms/RCA in 1-2
     * relationship.
     */

    public Double getParamA12()
    {
        return attA12;
    }

//-----------------------------------------------------------------------------

    /** 
     * @return the parameter B for interactions with atoms/RCA in 1-2
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
                    + "Used:" + this.used + " ";
	if (verbosity > 2)
	{
            s = s + "Atm: " + this.atm.toString() 
		  + " SrcAtm: " + this.src.toString();
	}
	else
	{
	    s = s + "Atm: " + this.atm.getSymbol() 
		  + " SrcAtm: " + this.src.getSymbol();
	}
	s = s + ")";

        return s;
    }

//-----------------------------------------------------------------------------
}
