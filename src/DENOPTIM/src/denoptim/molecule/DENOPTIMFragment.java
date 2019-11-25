package denoptim.molecule;

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

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

import net.sf.jniinchi.INCHI_RET;

import org.openscience.cdk.Atom;
import org.openscience.cdk.Bond;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IBond.Order;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smsd.Isomorphism;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.inchi.InChIGenerator;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;

/**
 * Class representing a continuously connected portion of molecular object
 * holding attachment points.
 * 
 * @author Marco Foscato
 */

public class DENOPTIMFragment extends AtomContainer implements IAtomContainer
{ 	
    /**
	 * Version UID
	 */
	private static final long serialVersionUID = 4415462924969433010L;

//-----------------------------------------------------------------------------

    /**
     * Constructor of an empty fragment
     */
    
    public DENOPTIMFragment()
    {
        super();
    }
    
//-----------------------------------------------------------------------------

    /**
     * Constructor from another atom container, which has APs only as 
     * molecular properties.
     * @param mol
     * @throws DENOPTIMException 
     */
    
    public DENOPTIMFragment(IAtomContainer mol) throws DENOPTIMException
    {
        super(mol);
        movePropertyToAP();
    }

//-----------------------------------------------------------------------------


    public void setAP(int srcAtmId, String apClass, Point3d vector)
    {
        //TODO
    }

//-----------------------------------------------------------------------------

    /**
     * Returns the number of APs currently defined.
     * @return the number of APs
     */
    
    public int getAPCount()
    {
    	int num = 0;
        for (int atmId = 0; atmId<this.getAtomCount(); atmId++)
        {
            IAtom atm = this.getAtom(atmId);
            @SuppressWarnings("unchecked")
			ArrayList<DENOPTIMAttachmentPoint> apsOnAtm = 
            		(ArrayList<DENOPTIMAttachmentPoint>) atm.getProperty(
            				DENOPTIMConstants.APTAG);
            num = num + apsOnAtm.size();
        }
    	return num;
    }

//-----------------------------------------------------------------------------
    
    /**
     * Changes the properties of each APs as to reflect the current atom list.
     * DENOPTIMAttachmentPoint include the index of their source atom, and this
     * method updates such indexes to reflect the current atom list.
     * This method is needed upon reordering of the atom list.
     */
    
    public void updateAPs()
    {
        for (int atmId = 0; atmId<this.getAtomCount(); atmId++)
        {
            IAtom atm = this.getAtom(atmId);
            @SuppressWarnings("unchecked")
			ArrayList<DENOPTIMAttachmentPoint> apsOnAtm = 
            		(ArrayList<DENOPTIMAttachmentPoint>) atm.getProperty(
            				DENOPTIMConstants.APTAG);
            for (int i = 0; i < apsOnAtm.size(); i++)
            {
                DENOPTIMAttachmentPoint ap = apsOnAtm.get(i);
                ap.setAtomPositionNumber(atmId);
            }
        }
    }

//-----------------------------------------------------------------------------
    
    /**
     * Collects APs as molecular property. Use this to save the fragment in an
     * SDF file.
     * Converts the internal notation defining APs (i.e., APs are stored in
     * as atom-specific properties) to the standard DENOPTIM formalism (i.e.,
     * APs are collected in a molecular property).
     * @return the list of APs. Note that these APs cannot respond to changes
     * in the atom list!
     */
    
    private ArrayList<DENOPTIMAttachmentPoint> getCurrentAPs()
    {
    	updateAPs();
    	
    	ArrayList<DENOPTIMAttachmentPoint> allAPs = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	
        for (IAtom atm : this.atoms())
        {
	        @SuppressWarnings("unchecked")
			ArrayList<DENOPTIMAttachmentPoint> apsOnAtm = 
	        		(ArrayList<DENOPTIMAttachmentPoint>) atm.getProperty(
	        				DENOPTIMConstants.APTAG);
	        for (int i = 0; i<apsOnAtm.size(); i++)
	        {
	            allAPs.add(apsOnAtm.get(i));
	        }
        }

        //Reorder according to DENOPTIMAttachmentPoint priority
        Collections.sort(allAPs, new DENOPTIMAttachmentPointComparator());
        
        return allAPs;
    }
    
//-----------------------------------------------------------------------------
    
    private void movePropertyToAP() throws DENOPTIMException
    {

	    String allAtomsProp = "";
	    try {
            allAtomsProp = this.getProperty(
            		DENOPTIMConstants.APCVTAG).toString();
        } catch (Throwable t ) {
            throw new DENOPTIMException("AtomContainer has no '" 
        + DENOPTIMConstants.APCVTAG + "' tag!");
        }

	    ArrayList<DENOPTIMAttachmentPoint> allAPs = 
	    		new ArrayList<DENOPTIMAttachmentPoint>();
	    String[] atomsPRop = allAtomsProp.split(
	    		DENOPTIMConstants.SEPARATORAPPROPATMS);
	    for (int i = 0; i< atomsPRop.length; i++)
	    {
			String onThisAtm = atomsPRop[i];
			if (onThisAtm.contains(DENOPTIMConstants.SEPARATORAPPROPATMS))
			{
			    String[] moreAPonThisAtm = onThisAtm.split(
			    		DENOPTIMConstants.SEPARATORAPPROPAPS);
			    DENOPTIMAttachmentPoint ap = 
			    		new DENOPTIMAttachmentPoint(moreAPonThisAtm[0]);
			    int atmID = ap.getAtomPositionNumber();
			    //DENOPTIM's format used [1 to n+1] instead of [0 to n]
			    atmID = atmID-1;
			    ap.setAtomPositionNumber(atmID);
	            allAPs.add(ap);
			    for (int j = 1; j<moreAPonThisAtm.length; j++ )
			    {
					DENOPTIMAttachmentPoint apMany = 
							new DENOPTIMAttachmentPoint(atmID 
									+ DENOPTIMConstants.SEPARATORAPPROPAAP
									+ moreAPonThisAtm[j]);
					allAPs.add(apMany);
			    }
			} 
			else 
			{
			    DENOPTIMAttachmentPoint ap = 
			    		new DENOPTIMAttachmentPoint(onThisAtm);
			    int atmID = ap.getAtomPositionNumber();
                //DENOPTIM's format used [1 to n+1] instead of [0 to n]
                atmID = atmID-1;
			    ap.setAtomPositionNumber(atmID);
			    allAPs.add(ap);
			}
	    }

		//Write attachment points also on the atoms
        for (int i = 0; i < allAPs.size(); i++)
        {
            DENOPTIMAttachmentPoint ap = allAPs.get(i);
            int atmID = ap.getAtomPositionNumber();
            IAtom atm = this.getAtom(atmID);
            try {
                @SuppressWarnings("unchecked")
				ArrayList<DENOPTIMAttachmentPoint> oldAPs = 
                		(ArrayList<DENOPTIMAttachmentPoint>) atm.getProperty(
                				DENOPTIMConstants.APTAG);
                oldAPs.add(ap);
                atm.setProperty(DENOPTIMConstants.APTAG,oldAPs);
            } catch (Throwable t ) {
                ArrayList<DENOPTIMAttachmentPoint> aps = 
                		new ArrayList<DENOPTIMAttachmentPoint>();
                aps.add(ap);
                atm.setProperty(DENOPTIMConstants.APTAG,aps);
            }
        } 
    }

//-----------------------------------------------------------------------------
    
    public void moveAPsToProperties(String format)
    {

        //WARNING! Here we use enumeration 1-to-n instead of 0-to-(n-1)
        //         to produce a file readable by DENOPTIM

    	String apClass = "";
        String apBond = "";
        for (IAtom atm : this.atoms())
        {
        	int atmID = this.getAtomNumber(atm);
        	
	        @SuppressWarnings("unchecked")
			ArrayList<DENOPTIMAttachmentPoint> apsOnAtm = 
	        		(ArrayList<DENOPTIMAttachmentPoint>) atm.getProperty(
	        				DENOPTIMConstants.APTAG);
	        
	        boolean firstCL = true;
	        for (int i = 0; i<apsOnAtm.size(); i++)
	        {
			    DENOPTIMAttachmentPoint ap = apsOnAtm.get(i);
	
			    //Build SDF property "CLASS"
			    String stingAPP = ""; //String Attachment Point Property
			    if (firstCL)
			    {
					firstCL = false;
					stingAPP = ap.getSingleAPStringSDF(true);
					if (apClass.equals(""))
					{
			            stingAPP = stingAPP.substring(1,stingAPP.length());
					}
			    } 
			    else 
			    {
			    	stingAPP = ap.getSingleAPStringSDF(false);
			    }
			    apClass = apClass + stingAPP;
	
			    //Build SDF property "ATTACHMENT_POINT"
			    int BndOrd = FragmentSpace.getBondOrderForAPClass(
			    		ap.getAPClass());
			    String sBO = Integer.toString(BndOrd);
			    String stBnd = " " + Integer.toString(atmID+1)+":"+sBO;
			    if (apBond.equals(""))
			    {
	                stBnd = stBnd.substring(1,stBnd.length());
			    }
			    apBond = apBond + stBnd;
			}
	    }

        this.setProperty(DENOPTIMConstants.APCVTAG,apClass);
        this.setProperty(DENOPTIMConstants.APTAG,apBond);
    }

//-----------------------------------------------------------------------------
    
}
