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

package denoptim.fragspace;

import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.utils.FragmentUtils;


/**
 * Class defining the fragment space
 * 
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */
public class FragmentSpace
{
    /**
     * Data structure containing the molecular representation of
     * building blocks: scaffolds section - fragments that can be used
     * as seed to grow a new molecule.
     */
    private static ArrayList<IAtomContainer> scaffoldLib = null;

    /**
     * Data structure containing the molecular representation of
     * building blocks: fragment section - fragments for general use
     */
    private static ArrayList<IAtomContainer> fragmentLib = null;

    /**
     * Data structure containing the molecular representation of
     * building blocks: capping group section - fragments with only
     * one attachment point used to saturate unused attachment
     * points on a graph.
     */
    private static ArrayList<IAtomContainer> cappingLib = null;

    /**
     * Data structure that stored the true entries of the 
     * attachment point classes compatibility matrix
     */
    private static HashMap<String, ArrayList<String>> compatMap;

    /**
     * Data structure that stores compatible APclasses for joining APs 
     * in ring-closing bonds. Symmetric, purpose specific
     * compatibility matrix.
     */
    private static HashMap<String, ArrayList<String>> rcCompatMap;

    /**
     * Data structure that stores the correspondence between bond order
     * and attachment point class.
     */
    private static HashMap<String, Integer> bondOrderMap;

    /**
     * Data structure that stores the AP-classes to be used to cap unused
     * APS on the growing molecule.
     */
    private static HashMap<String, String> cappingMap;

    /**
     * Data structure that stores AP classes that cannot be held unused
     */
    private static ArrayList<String> forbiddenEndList;

    /**
     * Clusters of fragments based on the number of APs
     */
    private static HashMap<Integer, ArrayList<Integer>> fragPoolPerNumAP;

    /**
     * List of APclasses per each fragment
     */
    private static HashMap<Integer, ArrayList<String>> apClassesPerFrag;

    /** 
     * Clusters of fragments'AP based on AP classes
     */
    private static HashMap<String, ArrayList<ArrayList<Integer>>> 
							    fragsApsPerApClass;

    /**
     * APclass-specific constraints to constitutionsl symmetry
     */
    private static HashMap<String, Double> symmConstraints;


//------------------------------------------------------------------------------

    /**
     * Return the molecular representation of a fragment of any type (i.e., 
     * scaffold, fragment, capping).
     * @param frgTyp the type of fragment - selects the library from which the
     * fragment is taken
     * @param molIdx the index (enumeration from 1 to n+1) of the fragment in
     * the proper library, which is defied by the type of fragment
     * @return the molecular representation of the fragment
     * @throws DENOPTIMException
     */

    public static IAtomContainer getFragment(int frgTyp, int molIdx) 
						        throws DENOPTIMException
    {
	String msg = "";
	if (fragmentLib == null || scaffoldLib == null || cappingLib == null)
	{
	    msg = "Cannot retrieve fragments before defining the FragmentSpace";
	    throw new DENOPTIMException(msg);
	}
        IAtomContainer iac = null, molClone = null;
	switch (frgTyp)
	{
	case 0:
	    if (molIdx < scaffoldLib.size())
	    {
		iac = scaffoldLib.get(molIdx);	
	    }
	    else
	    {
		msg = "Mismatch between scaffold molIdx and size of the library"
		      + ". MolId: " + molIdx + " FragType: " + frgTyp;
                DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
		throw new DENOPTIMException(msg);
	    }
	    break;
	case 1:
            if (molIdx < fragmentLib.size())
            {
                iac = fragmentLib.get(molIdx);
            }
            else
            {
                msg = "Mismatch between fragment molIdx and size of the library"
                      + ". MolId: " + molIdx + " FragType: " + frgTyp;
                DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                throw new DENOPTIMException(msg);
            }
	    break;
        case 2:
            if (molIdx < cappingLib.size())
            {
                iac = cappingLib.get(molIdx);
            }
            else
            {
                msg = "Mismatch between capping group molIdx and size of the "
                      + "library. MolId: " + molIdx + " FragType: " + frgTyp;
                DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                throw new DENOPTIMException(msg);
            }
            break;
	default:
	    msg = "Unknown type of fragment '" + frgTyp + "'.";
            DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
	    throw new DENOPTIMException(msg);
	}
	
        try
        {
            molClone = (IAtomContainer) iac.clone();
        }
        catch (CloneNotSupportedException cnse)
        {
            throw new DENOPTIMException(cnse);
        }

        return molClone;
    }

//------------------------------------------------------------------------------

    public static ArrayList<IAtomContainer> getScaffoldLibrary()
    {
        return scaffoldLib;
    }

//------------------------------------------------------------------------------

    public static ArrayList<IAtomContainer> getFragmentLibrary()
    {
        return fragmentLib;
    }

//------------------------------------------------------------------------------

    public static ArrayList<IAtomContainer> getCappingLibrary()
    {
        return cappingLib;
    }

//------------------------------------------------------------------------------

    /**
     * @param query the APClass of the attachment point on the capping group
     * @return all the capping groups which have the given APclass
     */
    public static ArrayList<Integer> getCappingGroupsWithAPClass(String query)
    {
	ArrayList<Integer> selected = new ArrayList<>();
	for (int i=0; i<cappingLib.size(); i++)
	{
	    String apc = "";
	    try 
	    {
		apc = FragmentUtils.getAPForFragment(i,2).get(0).getAPClass();
		if (apc.equals(query))
		{
		    selected.add(i);
		}
	    }
	    catch (DENOPTIMException de)
	    {
		// nothing
	    } 
	}
	return selected;	
    }

//------------------------------------------------------------------------------

    /**
     * Load info from a compatibility matrix file.
     * This method imports information such as the compatibility matrix,
     * bond order map, and forbidden ends from
     * a compatibility matrix file (i.e., a formatted text file using DENOPTIM
     * keyword. This overrides any previous setting of such information in this
     * FragmentSpace.
     * @param inFile the pathname of the compatibility matrix file
     */

    public static void importCompatibilityMatrixFromFile(String inFile) 
							throws DENOPTIMException
    {
            setCompatibilityMatrix(new HashMap<String,ArrayList<String>>());
            setBondOrderMap(new HashMap<String,Integer>());
            setCappingMap(new HashMap<String,String>());
            setForbiddenEndList(new ArrayList<String>());
            DenoptimIO.readCompatibilityMatrix(inFile,
						compatMap,
						bondOrderMap,
						cappingMap,
						forbiddenEndList);
    }

//------------------------------------------------------------------------------

    /**
     * Load info for ring closures compatibilities from a compatibility matrix 
     * file.
     * @param inFile the pathname of the RC-compatibility matrix file
     */

    public static void importRCCompatibilityMatrixFromFile(String inFile)
                                                        throws DENOPTIMException
    {
            setRCCompatibilityMatrix(new HashMap<String,ArrayList<String>>());
            DenoptimIO.readRCCompatibilityMatrix(inFile,rcCompatMap);
    }

//------------------------------------------------------------------------------

    public static HashMap<String, ArrayList<String>> getCompatibilityMatrix()
    {
        return compatMap;
    }

//------------------------------------------------------------------------------

    public static ArrayList<String> getCompatibleAPClasses(String query)
    {
	return compatMap.get(query);
    } 

//------------------------------------------------------------------------------

    /**
     * Returns the compatibility matrix for ring closing fragment-fragment
     * connections or <code>null</code> if not provided in the parameters file.
     * @return 
     */

    public static HashMap<String,ArrayList<String>> getRCCompatibilityMatrix()
    {
        return rcCompatMap;
    }

//------------------------------------------------------------------------------

    public static HashMap<String, Integer> getBondOrderMap()
    {
        return bondOrderMap;
    }

//------------------------------------------------------------------------------

   /**
    * Returns the bond order for the given APClass. 
    * If the Fragment space is 
    * not defined, that is, the bond order map is null, then it returns
    * bond order 1.
    * @param apclass the APclass to be converted into bond order
    * @param the bond order
    */
    public static Integer getBondOrderForAPClass(String apclass)
    {
        if (FragmentSpace.getBondOrderMap() == null)
        {
            String msg = "Attemting to get bond order, but no "
                       + "FragmentSpace defined (i.e., null BondOrderMap). "
                       + "Assuming bond order one.";
            DENOPTIMLogger.appLogger.log(Level.WARNING, msg);
	    return new Integer(1);
        }
        else
        {
            return FragmentSpace.getBondOrderMap().get(apclass);
        }
    }

//------------------------------------------------------------------------------

    public static HashMap<String, String> getCappingMap()
    {
        return cappingMap;
    }

//------------------------------------------------------------------------------

    /**
     * @param srcApClass the attachment point class of the attachment point to
     * be capped
     * @return the APClass of the capping group or null
     */

    public static String getCappingClass(String srcApClass)
    {
	return cappingMap.get(srcApClass);
    }

//------------------------------------------------------------------------------

    public static ArrayList<String> getForbiddenEndList()
    {
        return forbiddenEndList;
    }

//------------------------------------------------------------------------------

    /**
     * Return the set of APClasses that used in the compatibility matrix
     * for the growing graph APs.
     * Note these APClasses do include subclasses.
     * For example, for AP with class <code>MyAPClass:0</code> the 
     * <code>0</code> is the subclass.
     * @return the lst of APClasses
     */

    public static Set<String> getAllAPClassesFromCPMap()
    {
        return FragmentSpace.getCompatibilityMatrix().keySet();
    }

//------------------------------------------------------------------------------

    /**
     * Return the set of APClasses that are defined in the bond order map.
     * Note the APClasses in the bond order map fo not include the subclass.
     * For example, for AP with class <code>MyAPClass:0</code> the map
     * stores only <code>MyAPClass</code>.
     * @return the lst of APClasses
     */
    
    public static Set<String> getAllAPClassesFromBOMap()
    {
	return FragmentSpace.getBondOrderMap().keySet();
    }

//------------------------------------------------------------------------------

    public static HashMap<Integer,ArrayList<Integer>> getMapOfFragsPerNumAps()
    {
	return fragPoolPerNumAP;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the list of fragments with given number of APs
     * @param nAps the number of attachment points
     * @return the list of fragments as indexes in the library of fragments.
     */

    public static ArrayList<Integer> getFragsWithNumAps(int nAps)
    {
	ArrayList<Integer> lst = new ArrayList<>();
	if (fragPoolPerNumAP.containsKey(nAps))
	{
	    lst = fragPoolPerNumAP.get(nAps);
	}
        return lst;
    }

//------------------------------------------------------------------------------
   
    public static HashMap<Integer,ArrayList<String>> 
						    getMapAPClassesPerFragment()
    {
	return apClassesPerFrag;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the APclasses associated with a given fragment.
     * @param fragId the index pf the fragment in the library
     * @return the list of APclasses found of the fragment
     */

    public static ArrayList<String> getAPClassesPerFragment(int fragId)
    {
        return apClassesPerFrag.get(fragId);
    }

//------------------------------------------------------------------------------

    public static HashMap<String,ArrayList<ArrayList<Integer>>> 
						      getMapFragsAPsPerAPClass()
    {
	return fragsApsPerApClass;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the list of attachment points with the given class. Multiple
     * APs can be found for each fragment, thus this method returns pairs of
     * integers identifying the fragment (first integer in the pair) and the
     * AP (second index in the pair).
     * @param apclass 
     * @return the list of pairs of indeces representing respectively the 
     * fragment and its AP.
     */

    public static ArrayList<ArrayList<Integer>> getFragsWithAPClass(
								 String apclass)
    {
	ArrayList<ArrayList<Integer>> lst = new ArrayList<>();
	if (fragsApsPerApClass.containsKey(apclass))
	{
	    lst = fragsApsPerApClass.get(apclass); 
	}
        return lst;
    }

//------------------------------------------------------------------------------

    /**
     * Checks if the symmetry settings impose use of symmetry on attachment 
     * points of the given AP class. The value returned is the result 
     * of the action of symmetry-related keyword affecting the definition of
     * this FragmentSpace.
     * @param apClass the attachment point class
     * @return <code>true<code> if symmetry has the applyed on APs of the
     * given class
     */

    public static boolean imposeSymmetryOnAPsOfClass(String apClass)
    {
	boolean res = true;
        if (hasSymmetryConstrain(apClass))
        {
            if (getSymmetryConstrain(apClass) <
                             (1.0 - DENOPTIMConstants.FLOATCOMPARISONTOLERANCE))
            {
                res = false;
            }
        }
        else
        {
            if (!FragmentSpaceParameters.enforceSymmetry())
            {
                res = false;
            }
        }
	return res;
    }
     

//------------------------------------------------------------------------------

    /**
     * Checks if there is a constraint on the constitutionsl 
     * symmetry probability for the given AP class.
     * @param apClass the attachment point class
     * @return <code>true<code> if there is a constraint on the constitutionsl
     * symmetry probability for the given AP class.
     */

    public static boolean hasSymmetryConstrain(String apClass)
    {
        return symmConstraints.containsKey(apClass);
    }

//------------------------------------------------------------------------------

    /**
     * Return the constitutional symmetry constrain for the given APclass,
     * or null.
     * The constrain is
     * a fixed probability that is not dependent on the distance from the
     * root of the DENOPTIMGraph (i.e. the level).
     * @param apClass the attachment point class
     * @return the constrained value of the symmetric subtitution probability
     * (0.0 - 1.0).
     */

    public static double getSymmetryConstrain(String apClass)
    {
	return symmConstraints.get(apClass);
    }

//------------------------------------------------------------------------------

    public static void setScaffoldLibrary(ArrayList<IAtomContainer> lib)
    {
	scaffoldLib = lib;
    }

//------------------------------------------------------------------------------

    public static void setFragmentLibrary(ArrayList<IAtomContainer> lib)
    {
	fragmentLib = lib;
    }

//------------------------------------------------------------------------------

    public static void setCappingLibrary(ArrayList<IAtomContainer> lib)
    {
	cappingLib = lib;
    }

//------------------------------------------------------------------------------

    public static void setCompatibilityMatrix(
					HashMap<String,ArrayList<String>> map)
    {
        compatMap = map;
    }

//------------------------------------------------------------------------------

    public static void setRCCompatibilityMatrix(
					HashMap<String,ArrayList<String>> map)
    {
       rcCompatMap = map;
    }

//------------------------------------------------------------------------------

    public static void setBondOrderMap(HashMap<String, Integer> map)
    {
        bondOrderMap = map;
    }

//------------------------------------------------------------------------------

    public static void setCappingMap(HashMap<String, String> map)
    {
        cappingMap = map;
    }

//------------------------------------------------------------------------------

    public static void setForbiddenEndList(ArrayList<String> lst)
    {
        forbiddenEndList = lst;
    }

//------------------------------------------------------------------------------

    public static void setFragPoolPerNumAP(HashMap<Integer,ArrayList<Integer>>
									    map)
    {
	fragPoolPerNumAP = map;
    }

//------------------------------------------------------------------------------

    public static void setFragsApsPerApClass(
			      HashMap<String,ArrayList<ArrayList<Integer>>> map)
    {
        fragsApsPerApClass = map;
    }

//------------------------------------------------------------------------------

    public static void setAPClassesPerFrag(HashMap<Integer,ArrayList<String>> 
									    map)
    {
	apClassesPerFrag = map;
    }

//------------------------------------------------------------------------------

    public static void setSymmConstraints(HashMap<String,Double> map)
    {
	symmConstraints = map;
    }

//------------------------------------------------------------------------------

}