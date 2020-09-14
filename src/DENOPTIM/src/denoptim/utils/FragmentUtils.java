/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no> and
 *   Marco Foscato <marco.foscato@uib.no>
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

package denoptim.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtom;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMFragment;
import denoptim.molecule.IGraphBuildingBlock;
import denoptim.molecule.SymmetricSet;

import java.util.logging.Level;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;


/**
 *
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class FragmentUtils
{

//------------------------------------------------------------------------------

    /**
     * For the given molecule find the compatible list of classes (reactions)
     * with respect to its attachment points. 
     * @Deprecated: use DEOPTIMFragment.getAllAPClassess()
     * @param mol
     * @return list of classes (reactions)
     */

	@Deprecated
    public static ArrayList<String> getClassesForFragment(IAtomContainer mol)
    {
        String apProperty = 
			  mol.getProperty(DENOPTIMConstants.APCVTAG).toString();
        ArrayList<String> lstReac = new ArrayList<>();
        String[] tmpArr = apProperty.split("\\s+");
        for (int j=0; j<tmpArr.length; j++)
        {
            int idx = tmpArr[j].indexOf("#");
            String rcn = tmpArr[j].substring(idx+1);

            if (rcn.contains(","))
            {
                String[] st2 = rcn.split(",");
                for (int i=0; i<st2.length; i++)
                {
                    String[] st3 = st2[i].split(":");
                    String rc = st3[0] + ":" + st3[1];
                    lstReac.add(rc);
                }
            }
            else
            {
                String[] st3 = rcn.split(":");
                String rc = st3[0] + ":" + st3[1];
                lstReac.add(rc);
            }
        }

        return lstReac;
    }

//------------------------------------------------------------------------------

    /**
     * Parses the attachment point information associated with a molecule.
     * Where applicable, each AP must correspond to a class/reaction.
     * If multiple classes are involved, multiple attachments are created.
     * @param idx the index of the fragment in the library (0-based).
     * @param ftype the type of fragment (scaffold, fragment, capping group)
     * as integer
     * @return a clone of the list of <code>DENOPTIMAttachmentPoint</code>
     * @throws DENOPTIMException
     */

    public static  ArrayList<DENOPTIMAttachmentPoint> getAPForFragment(int idx ,
                                             int ftype) throws DENOPTIMException
    {
    	IGraphBuildingBlock mol = FragmentSpace.getFragment(ftype,idx);
    	ArrayList<DENOPTIMAttachmentPoint> origList = mol.getAPs();
    	ArrayList<DENOPTIMAttachmentPoint> clList = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	for (int i=0; i<origList.size(); i++)
    	{
    		clList.add(origList.get(i).clone());
    	}
        return clList;
    }

//------------------------------------------------------------------------------

    /**
     * Parses the attachment point information associated with a molecule.
     * Where applicable, each AP must correspond to only a single
     * class/reaction.
     * If multiple classes are involved, multiple attachments are created.
     * @param mol
     * @return a list of <code>DENOPTIMAttachmentPoint</code>
     * @throws denoptim.exception.DENOPTIMException
     */

    public static ArrayList<DENOPTIMAttachmentPoint> getAPForFragment(
    		IAtomContainer mol) throws DENOPTIMException
    { 

//TODO: need to update in relation with DENOPTIMFragment class methods
// probably this is all superfluous and should be replaced by translator in DENOPTIMFragment
// or we just keep this one

        String apProperty = mol.getProperty(DENOPTIMConstants.APTAG).toString();
        String[] tmpArr = apProperty.split("\\s+");

        Object reacObj = mol.getProperty(DENOPTIMConstants.APCVTAG);

        ArrayList<DENOPTIMAttachmentPoint> lstAP = new ArrayList<>();

        if (reacObj == null)
        {
            // No class info
            for (int j=0; j<tmpArr.length; j++)
            {
                String[] st = tmpArr[j].split(":");
                if (!GenUtils.isNumeric(st[1]))
                {
		    String msg = "Invalid " + DENOPTIMConstants.APTAG + " tag.";
                    DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                    throw new DENOPTIMException(msg);
                }
                if (!GenUtils.isNumeric(st[0]))
                {
                    String msg = "Invalid " + DENOPTIMConstants.APTAG + " tag.";
                    DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                    throw new DENOPTIMException(msg);
                }
                int l = Integer.parseInt(st[1]);
                int atmno = Integer.parseInt(st[0]) - 1;
                for (int k=1; k<=l; k++)
                {
                    // create an AP object
                    DENOPTIMAttachmentPoint dap =
                                    new DENOPTIMAttachmentPoint(atmno, k, k);
                    lstAP.add(dap);
                }
            }
        }
        else
        {
            // if ap-class information is available 
        	// we can ignore the ATTACHMENT_POINT info
            HashMap<Integer, Integer> apMap = new HashMap<>();

            for (int j=0; j<tmpArr.length; j++)
            {
                String[] st = tmpArr[j].split(":");
                if (!GenUtils.isNumeric(st[1]))
                {
                    String msg = "Invalid " + DENOPTIMConstants.APTAG + " tag";
                    DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                    throw new DENOPTIMException(msg);
                }
                if (!GenUtils.isNumeric(st[0]))
                {
                    String msg = "Invalid " + DENOPTIMConstants.APTAG + " data";
                    DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                    throw new DENOPTIMException(msg);
                }
                int l = Integer.parseInt(st[1]);
                int atmno = Integer.parseInt(st[0]) - 1;
                if (apMap.containsKey(atmno))
                {
                    int val = apMap.get(atmno) + l;
                    apMap.put(atmno, val);
                }
                else
                {
                    apMap.put(atmno, l);
                }
            }

            // here we create the list of APs based on the
            apProperty = reacObj.toString();
            String[] tmpReac = apProperty.split("\\s+");

            // sum of the ap class bond orders must match the attachment point
            // bond order value

            for (int j=0; j<tmpReac.length; j++)
            {
                String str = tmpReac[j];
                String[] st1 = str.split("#");


                if (!GenUtils.isNumeric(st1[0]))
                {
                    String msg = "Invalid " + DENOPTIMConstants.APCVTAG 
				+ " data";
                    DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                    throw new DENOPTIMException(msg);
                }

                int atomidx = Integer.parseInt(st1[0]) - 1;
                String rcstr = st1[1];

                if (rcstr.contains(","))
                {
                    // multiple APs on this atom
                    String[] st2 = rcstr.split(",");
                    for (int i=0; i<st2.length; i++)
                    {
                        // to extract the direction vectors
                        String[] st3 = st2[i].split(":");
                        if (!GenUtils.isNumeric(st3[1]))
                        {
                            String msg = "Invalid " + DENOPTIMConstants.APCVTAG
				+ " data";
                            DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                            throw new DENOPTIMException(msg);
                        }

                        String rc = st3[0] + ":" + st3[1];
                        //System.err.println("RCN: " + rc);
                        //System.err.println("st: " + st3[0]);
                        Integer apobj = 
				   FragmentSpace.getBondOrderForAPClass(st3[0]);
                        if (apobj == null)
                        {
                            String msg = "No bond order data found for "
                                				       + st3[0];
                            DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                            throw new DENOPTIMException(msg);
                        }
                        int apcon = apobj;

                        DENOPTIMAttachmentPoint dap =
                            new DENOPTIMAttachmentPoint(atomidx, apcon, apcon);
                        dap.setAPClass(rc);


                        if (st3.length > 2)
                        {
                            String[] st4 = st3[2].split("%");
                            if (st4.length != 3)
                            {
                                String msg = "Invalid " 
				+ DENOPTIMConstants.APCVTAG + " data";
                                DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                                throw new DENOPTIMException(msg);
                            }
                            double[] dirvec = new double[3];
                            for(int k=0;k<3;k++)
                            {
                                dirvec[k] = Double.parseDouble(st4[k]);
                            }
                            dap.setDirectionVector(dirvec);
                        }

                        lstAP.add(dap);
                    }
                }
                else
                {
                    String[] st3 = rcstr.split(":");
                    String rc = st3[0] + ":" + st3[1];

                    if (!GenUtils.isNumeric(st3[1]))
                    {
                        String msg = "Invalid " + DENOPTIMConstants.APCVTAG 
			+ " data";
                        DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                        throw new DENOPTIMException(msg);
                    }


                    Integer apobj = 
				  FragmentSpace.getBondOrderForAPClass(st3[0]);

                    if (apobj == null)
                    {
                        String msg = "No bond order data found for " + st3[0];
                        DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                        throw new DENOPTIMException(msg);
                    }

                    int apcon = apobj;


                    DENOPTIMAttachmentPoint dap =
                            new DENOPTIMAttachmentPoint(atomidx, apcon, apcon);
                    dap.setAPClass(rc);

                    if (st3.length == 3)
                    {
                        String[] st4 = st3[2].split("%");
                        if (st4.length != 3)
                        {
                            String msg = "Invalid " + DENOPTIMConstants.APCVTAG 
				+ " data";
                            DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                            throw new DENOPTIMException(msg);
                        }
                        double[] dirvec = new double[3];
                        for(int k=0;k<3;k++)
                        {
                            dirvec[k] = Double.parseDouble(st4[k]);
                        }
                        dap.setDirectionVector(dirvec);
                    }
                    lstAP.add(dap);
                }
            }
	    try
	    {
		try
		{
                    checkAPInfo(apMap, lstAP);
		}
		catch (DENOPTIMException ide)
		{
		    String msg = "Check AP definitions: \n"
			+ mol.getProperty(DENOPTIMConstants.APTAG).toString()
		        + "\n"
			+ mol.getProperty(DENOPTIMConstants.APCVTAG).toString();
                    DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                    throw new DENOPTIMException(msg);
		}
	    }
	    catch (DENOPTIMException de)
	    {
		throw de;
	    }
        }

        return lstAP;
    }

//------------------------------------------------------------------------------

    /**
     * Check if the info DENOPTIMConstants.APTAG and 
     * DENOPTIMConstants.APCVTAG of a fragments
     * are in agreement; typically in terms of the number of APs identified
     * for each atom. The atom indices bearing the APs must have the same number
     * identified in both tags.
     * @param apMap the attachment point info per atom which only gives the
     * number of APs associated
     * @param lstAP list of attachment points bearing the 
     * DENOPTIMConstants.APCVTAG info
     * @throws DENOPTIMException
     */

    private static void checkAPInfo(HashMap<Integer, Integer> apMap,
                        ArrayList<DENOPTIMAttachmentPoint> lstAP)
                        			        throws DENOPTIMException
    {
        Iterator it = apMap.entrySet().iterator();
        
        while (it.hasNext())
        {
            Map.Entry pairs = (Map.Entry)it.next();
            //System.err.println(pairs.getKey() + " = " + pairs.getValue());
            int atomidx = ((Integer)pairs.getKey());
            int atomcon = ((Integer)pairs.getValue());

            // check the number of connections for this atom
            int l = 0;

            for (DENOPTIMAttachmentPoint dp:lstAP)
            {
                //System.err.println(dp.toString());
                if (dp.getAtomPositionNumber() == atomidx)
                {
                    l += dp.getFreeConnections();
                }
            }

            //System.err.println(atomidx + " " + atomcon + " : " + l);

            if (l != atomcon)
            {
                String msg = "Mismatch between "
			   + DENOPTIMConstants.APCVTAG + " and " 
		           + DENOPTIMConstants.APTAG + " tags.";
                DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                throw new DENOPTIMException(msg);
            }
        }        
    }
    
//------------------------------------------------------------------------------
    
    //TODO: check if we really need this. Possibly get rid of it
    
    public static ArrayList<SymmetricSet> getMatchingAP(IGraphBuildingBlock bb,
            ArrayList<DENOPTIMAttachmentPoint> daps)
    {
    	if (bb instanceof DENOPTIMFragment)
    	{
    		IAtomContainer iac = ((DENOPTIMFragment) bb).getAtomContainer();
    		return getMatchingAP(iac, daps);
    	}
    	return null;
    }

//------------------------------------------------------------------------------

    /**
     * In the given molecule, the method identifies similar atom environments
     * which also have similar attachment point attributes (bond/reaction
     * types)
     * @param mol the scaffold molecule, for which similar atom environments 
     * at the attachment points are to be identified
     * @param daps the list of APs for the scaffold
     * @return a list of indices of the APs that match
     */

    public static ArrayList<SymmetricSet> getMatchingAP(IAtomContainer mol,
                                        ArrayList<DENOPTIMAttachmentPoint> daps)
    {
        ArrayList<SymmetricSet> lstCompatible = new ArrayList<>();
        for (int i=0; i<daps.size()-1; i++)
        {
            ArrayList<Integer> lst = new ArrayList<>();
            Integer i1 = i;
            lst.add(i1);

    	    boolean alreadyFound = false;
    	    for (SymmetricSet previousSS : lstCompatible)
    	    {
        		if (previousSS.contains(i1))
        		{
        		    alreadyFound = true;
        		    break;
        		}
    	    }
    	    
    	    if (alreadyFound)
    	    {
    	        continue;
    	    }

            DENOPTIMAttachmentPoint d1 = daps.get(i);
            for (int j=i+1; j<daps.size(); j++)
            {
                DENOPTIMAttachmentPoint d2 = daps.get(j);
                if (isCompatible(mol, d1.getAtomPositionNumber(),
                                                d2.getAtomPositionNumber()))
                {
                    // check if reactions are compatible
                    if (isFragmentClassCompatible(d1, d2))
                    {
                        Integer i2 = j;
                        lst.add(i2);
                    }
                }
            }

            if (lst.size() > 1)
    	    {
                lstCompatible.add(new SymmetricSet(lst));
    	    }
        }

        return lstCompatible;
    }

//------------------------------------------------------------------------------

    /**
     * Compare attachment points based on the AP class
     * @param A attachment point information
     * @param B attachment point information
     * @return <code>true</code> if the points have the same class or
     * else <code>false</code>
     */

    private static boolean isFragmentClassCompatible(DENOPTIMAttachmentPoint A,
                                                      DENOPTIMAttachmentPoint B)
    {
        String strA = A.getAPClass();
        String strB = B.getAPClass();
        if (strA != null && strB != null)
        {
            if (strA.compareToIgnoreCase(strB) == 0)
                    return true;
        }
        else
        {        
            return true;
        }

        return false;
    }
    
//------------------------------------------------------------------------------

    /**
     * Checks if the atoms at the given positions have similar environments
     * i.e. are similar in atom types etc.
     * @param mol
     * @param a1 atom position
     * @param a2 atom position
     * @return <code>true</code> if atoms have similar environments
     */

    private static boolean isCompatible(IAtomContainer mol, int a1, int a2)
    {
        // check atom types
        IAtom atm1 = mol.getAtom(a1);
        IAtom atm2 = mol.getAtom(a2);

        if (atm1.getSymbol().compareTo(atm2.getSymbol()) != 0)
            return false;

        // check connected bonds
        if (mol.getConnectedBondsCount(atm1)!=mol.getConnectedBondsCount(atm2))
            return false;


        // check connected atoms
        if (mol.getConnectedAtomsCount(atm1)!=mol.getConnectedAtomsCount(atm2))
            return false;

        List<IAtom> la1 = mol.getConnectedAtomsList(atm2);
        List<IAtom> la2 = mol.getConnectedAtomsList(atm2);

        int k = 0;
        for (int i=0; i<la1.size(); i++)
        {
            IAtom b1 = la1.get(i);
            for (int j=0; j<la2.size(); j++)
            {
                IAtom b2 = la2.get(j);
                if (b1.getSymbol().compareTo(b2.getSymbol()) == 0)
                {
                    k++;
                    break;
                }
            }
        }

        return k == la1.size();
    }

//------------------------------------------------------------------------------

    public static String getFragmentType(int ftype)
    {
        String strFrg;
        switch (ftype) 
        {
            case 0:
                strFrg = "SCAFFOLD";
                break;
            case 1:
                strFrg = "FRAGMENT";
                break;
            default:
                strFrg = "CAPPING GROUPS";
                break;
        }

        return strFrg;
    }
    
   //-----------------------------------------------------------------------------
    
    /**
     * Return the 3D coordinates, if present.
     * If only 2D coords exist, then it returns the 2D projected in 3D space.
     * If neither 3D nor 2D are present returns [0, 0, 0].
     * @param atm the atom to analyze.
     * @return a not null.
     */
    public static Point3d getPoint3d(IAtom atm)
    {
    	Point3d p = atm.getPoint3d();
    	
    	if (p == null)
    	{
    		Point2d p2d = atm.getPoint2d();
    		if (p2d == null)
    		{
    			p = new Point3d(0.0, 0.0, 0.0);
    		}
    		else
    		{
    			p = new Point3d(p2d.x, p2d.y, 0.0);
    		}
    	}
    	return p;
    }

//------------------------------------------------------------------------------

}
