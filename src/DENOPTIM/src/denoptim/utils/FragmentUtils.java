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

import java.util.*;

import denoptim.molecule.*;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtom;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.logging.DENOPTIMLogger;

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

    /**
     * Compare attachment points based on the AP class
     * @param A attachment point information
     * @param B attachment point information
     * @return <code>true</code> if the points have the same class or
     * else <code>false</code>
     */

    public static boolean isFragmentClassCompatible(DENOPTIMAttachmentPoint A,
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

    public static boolean isCompatible(IAtomContainer mol, int a1, int a2)
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
