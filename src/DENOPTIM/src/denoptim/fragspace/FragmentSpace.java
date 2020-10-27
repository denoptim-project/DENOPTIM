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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.DENOPTIMEdge.BondType;
import denoptim.molecule.DENOPTIMFragment;
import denoptim.molecule.DENOPTIMFragment.BBType;
import denoptim.molecule.DENOPTIMTemplate;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.rings.RingClosureParameters;
import denoptim.utils.GraphUtils;


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
     * as seeds to grow a new molecule. 
     * WARNING! The objects stores in the library do not have a 
     * meaningful value for the two indexes representing the type
     * of building block and the position of the list of all building blocks
     * of that type.
     */
    private static ArrayList<DENOPTIMVertex> scaffoldLib = null;

    /**
     * Data structure containing the molecular representation of
     * building blocks: fragment section - fragments for general use.
     * WARNING! The objects stores in the library do not have a 
     * meaningful value for the two indexes representing the type
     * of building block and the position of the list of all building blocks
     * of that type.
     */
    private static ArrayList<DENOPTIMVertex> fragmentLib = null;

    /**
     * Data structure containing the molecular representation of
     * building blocks: capping group section - fragments with only
     * one attachment point used to saturate unused attachment
     * points on a graph. 
     * WARNING! The objects stores in the library do not have a 
     * meaningful value for the two indexes representing the type
     * of building block and the position of the list of all building blocks
     * of that type.
     */
    private static ArrayList<DENOPTIMVertex> cappingLib = null;

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
    private static HashMap<String, BondType> bondOrderMap;

    /**
     * Data structure that stores the AP-classes to be used to cap unused
     * APS on the growing molecule.
     */
    private static HashMap<String, String> cappingMap;

    /**
     * Data structure that stores AP classes that cannot be held unused
     */
    private static Set<String> forbiddenEndList;

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
     * APclass-specific constraints to constitutional symmetry
     */
    private static HashMap<String, Double> symmConstraints;
    
    /**
     * FLag defining use of AP class-based approach
     */
    protected static boolean apClassBasedApproch = false;
    
    /**
     * Flag signalling that this fragment space was built and validated
     */
    private static boolean isValid = false;
    
    /**
     * Index used to keep the order in a list of attachment points
     */
    public static AtomicInteger apID = new AtomicInteger();

//------------------------------------------------------------------------------
    
    /**
     * Define all components of a fragment space that implements the attachment
     * point class-approach.
     * @param scaffFile pathname to library of fragments used to start 
     * the 
     * construction of
     * any new graph (i.e., seed or root fragments, a.k.a. scaffolds).
     * @param fragFile pathname to the library of fragments for general purpose.
     * @param capFile pathname to the library of single-AP fragments used to 
     * cap free attachment 
     * points (i.e., the capping groups).
     * @param cpmFile pathname to the compatibility matrix, bond type mapping, 
     * capping, and forbidden ends rules.
     * @param rspmFile the APClass compatibility matrix for ring closures.
     * @param symCntrMap the map of symmetry constraints
     * @throws DENOPTIMException
     */
    public static void defineFragmentSpace(String scaffFile, String fragFile,
            String capFile, String cpmFile) throws DENOPTIMException
    {
        defineFragmentSpace(scaffFile, fragFile, capFile, cpmFile,"", 
                new HashMap<String, Double>());
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Define all components of a fragment space that implements the attachment
     * point class-approach.
     * @param scaffFile pathname to library of fragments used to start 
     * the 
     * construction of
     * any new graph (i.e., seed or root fragments, a.k.a. scaffolds).
     * @param fragFile pathname to the library of fragments for general purpose.
     * @param capFile pathname to the library of single-AP fragments used to 
     * cap free attachment 
     * points (i.e., the capping groups).
     * @param cpmFile pathname to the compatibility matrix, bond type mapping, 
     * capping, and forbidden ends rules.
     * @param rspmFile the APClass compatibility matrix for ring closures.
     * @param symCntrMap the map of symmetry constraints
     * @throws DENOPTIMException
     */
    public static void defineFragmentSpace(String scaffFile, String fragFile,
            String capFile, String cpmFile, String rcpmFile, 
            HashMap<String, Double> symCntrMap) 
                    throws DENOPTIMException
    {
        HashMap<String,ArrayList<String>> cpMap = 
                new HashMap<String,ArrayList<String>>();
        HashMap<String,BondType> boMap = new HashMap<String,BondType>();
        HashMap<String,String> capMap = new HashMap<String,String>();
        HashSet<String> forbEnds = new HashSet<String>();
        if (cpmFile.length() > 0)
        {
            DenoptimIO.readCompatibilityMatrix(cpmFile,
                        cpMap,
                        boMap,
                        capMap,
                        forbEnds);
            apClassBasedApproch = true;
        }
        setCompatibilityMatrix(cpMap);
        setBondOrderMap(boMap);
        setCappingMap(capMap);
        setForbiddenEndList(forbEnds);

        setSymmConstraints(symCntrMap);
        
        if (rcpmFile != null && rcpmFile.length() > 0)
        {
            HashMap<String,ArrayList<String>> rcCpMap = 
                    new HashMap<String,ArrayList<String>>();
            DenoptimIO.readRCCompatibilityMatrix(rcpmFile,rcCpMap);
            setRCCompatibilityMatrix(rcCpMap);
        }
        
        setScaffoldLibrary(convertsIACsToVertexes(
                DenoptimIO.readInLibraryOfFragments(
                        scaffFile,"scaffold"),BBType.SCAFFOLD));
        
        setFragmentLibrary(convertsIACsToVertexes(
        DenoptimIO.readInLibraryOfFragments(fragFile,"fragment"),
        BBType.FRAGMENT));
        
        if (capFile.length() > 0)
        {
            setCappingLibrary(convertsIACsToVertexes(
                    DenoptimIO.readInLibraryOfFragments(capFile,
                    "capping group"),BBType.CAP));
        }
        
        isValid = true;
        
        //TODO-V3: remove: tmp code just for devel phase
        if (FragmentSpaceParameters.useTemplates)
        {
            scaffoldLib = new ArrayList<>();
            scaffoldLib.add(DENOPTIMTemplate.getTestScaffoldTemplate());
            
            fragmentLib.add(DENOPTIMTemplate.getTestFragmentTemplate());
            
            System.err.println("WARNING! Running TEMP CODE: Replaced scaffold "
                    + "lib with single test template. Also appending one "
                    + "template to the library of fragments.");
        }
        
        FragmentSpaceUtils.groupAndClassifyFragments(useAPclassBasedApproach());
    }
    
//------------------------------------------------------------------------------

    /**
     * Processes a list of atom containers and builds a list of vertexes.
     * @param iacs the list of atom containers.
     * @return the list of vertexes.
     * @throws DENOPTIMException
     */
    
    //TODO-V3: adapt to templates.
    
    private static ArrayList<DENOPTIMVertex> convertsIACsToVertexes(
            ArrayList<IAtomContainer> iacs, BBType bbt) throws DENOPTIMException
    {
        ArrayList<DENOPTIMVertex> list = new ArrayList<DENOPTIMVertex>();
        for (IAtomContainer iac : iacs)
        {
            list.add(new DENOPTIMFragment(iac,bbt));
        }
        return list;
    }
    
//------------------------------------------------------------------------------

    /**
     * Checks for valid definition of this fragment space
     * @return <code>true</code> if this fragment space has been defined
     */
    public static boolean isDefined()
    {
        return isValid;
    }
    
//------------------------------------------------------------------------------

    /**
     * Check usage of APClass-based approach, i.e., uses attachment points with 
     * annotated data (i.e., the APClass) to evaluate compatibilities between
     * attachment points.
     * @return <code>true</code> if this fragment space makes use of 
     * APClass-based approach
     */
    public static boolean useAPclassBasedApproach()
    {
        return apClassBasedApproch;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Search for a specific AP on a specific fragment and finds out its class.
     * @param the identified of a specific attachment point.
     * @return the AP class or null
     */
    public static String getAPClassForFragment(IdFragmentAndAP apId)
    {
        String cls = null;
        try
        {
            DENOPTIMVertex frg = FragmentSpace.getVertexFromLibrary(
                        apId.getVertexMolType(), apId.getVertexMolId());
            cls = frg.getAttachmentPoints().get(apId.getApId()).getAPClass();
        }
        catch (Throwable t)
        {
            cls = null;
        }
                        
        return cls;
    }

//------------------------------------------------------------------------------

    /**
     * Returns a clone of the requested building block. The type of vertex
     * returned depends on the type stored in the library.
     * 
     * @param fTyp the type of building block. This basically selects the 
     * sub library from which the building block is taken: 0 for scaffold (i.e.,
     * building blocks that can be used to start a new graph), 1 for
     * standard building blocks (i.e., can be used freely to grow or modify an 
     * existing graph), or 2 for capping group (i.e., can be used only to 
     * saturate attachment points that cannot remain unused in a finished
     * graph).
     * @param bbIdx the index (0-based) of the building block in
     * the corresponding library defied by the type of building 
     * block 'bbType'
     * @return a clone of the chosen building block.
     * @throws DENOPTIMException when the given indexes cannot be used, for
     * example, any of the indexes is out of range.  
     */

    public static DENOPTIMVertex getVertexFromLibrary(BBType fTyp, int bbIdx) 
                                                        throws DENOPTIMException
    {
        // WARNING! This is were we first assign the bbTyp and bbIdx to
        // a vertex 'taken' from the library. Note that 'taken' means that we 
        // get a slightly modified copy of the vertex. In particular, the
        // objects stores in the library do not have a meaningful value for the
        // two indexes, so after we make a deep copy of them we assign the 
        // indexes according to bbTyp and bbIdx.
        
        String msg = "";
        if (fragmentLib == null || scaffoldLib == null || cappingLib == null)
        {
            msg = "Cannot retrieve fragments before defining the FragmentSpace";
            throw new DENOPTIMException(msg);
        }
        DENOPTIMVertex originalVrtx = null;
        switch (fTyp)
        {
        case SCAFFOLD:
            if (bbIdx < scaffoldLib.size())
            {
                    originalVrtx = scaffoldLib.get(bbIdx);        
            }
            else
            {
                msg = "Mismatch between scaffold bbIdx and size of the library"
                      + ". MolId: " + bbIdx + " FragType: " + fTyp;
                DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                throw new DENOPTIMException(msg);
            }
            break;
            
        case FRAGMENT:
            if (bbIdx < fragmentLib.size())
            {
                originalVrtx = fragmentLib.get(bbIdx);
            }
            else
            {
                msg = "Mismatch between fragment bbIdx and size of the "
                                + "library" + ". MolId: " + bbIdx 
                                + " FragType: " + fTyp;
                DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                throw new DENOPTIMException(msg);
            }
            break;
            
        case CAP:
            if (bbIdx < cappingLib.size())
            {
                originalVrtx = cappingLib.get(bbIdx);
            }
            else
            {
                msg = "Mismatch between capping group bbIdx and size "
                                + "of the library. MolId: " + bbIdx 
                                + " FragType: " + fTyp;
                DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                throw new DENOPTIMException(msg);
            }
            break;

        default:
            msg = "Unknown type of fragment '" + fTyp + "'.";
            DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
            throw new DENOPTIMException(msg);
        }
        
        DENOPTIMVertex clone = originalVrtx.clone();
        
        //TODO-V3: is there a better way to do this in a type-agnostic way?
        if (clone instanceof DENOPTIMFragment)
        {
            ((DENOPTIMFragment) clone).setMolId(bbIdx);
        }
        //TODO-V3 keep it or trash it?
        else if (clone instanceof DENOPTIMTemplate)
        {
            ((DENOPTIMTemplate) clone).setMolId(bbIdx);
        }
        else 
        {
            System.err.println("WARNING! Recovering a vertex that is neither an"
                    + " instance of fragment nor a template. "
                    + "Not setting bbType and bbIdx.");
        }
                
        return clone;
    }

//------------------------------------------------------------------------------

    public static ArrayList<DENOPTIMVertex> getScaffoldLibrary()
    {
        return scaffoldLib;
    }

//------------------------------------------------------------------------------

    public static ArrayList<DENOPTIMVertex> getFragmentLibrary()
    {
        return fragmentLib;
    }

//------------------------------------------------------------------------------

    public static ArrayList<DENOPTIMVertex> getCappingLibrary()
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
            apc = getVertexFromLibrary(BBType.CAP, i)
                    .getAttachmentPoints().get(0).getAPClass();
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
        setBondOrderMap(new HashMap<String,BondType>());
        setCappingMap(new HashMap<String,String>());
        setForbiddenEndList(new HashSet<String>());
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

    public static HashMap<String, BondType> getBondOrderMap()
    {
        return bondOrderMap;
    }

//------------------------------------------------------------------------------

   /**
    * Returns the bond order for the given APClass, if defined.
    * @param apclass the APclass to be converted into bond order
    * @return the bond order as an integer, or 1 if either the
    * Fragment space is not defined, that is, the bond order map is 
    * <code>null</code>, or a fully defined map does not include any mapping 
    * for the given APClass.
    */
    public static BondType getBondOrderForAPClass(String apclass)
    {
        String apRule = apclass.split(DENOPTIMConstants.SEPARATORAPPROPSCL)[0];
        if (bondOrderMap == null)
        {
            String msg = "Attempting to get bond order, but no "
                       + "FragmentSpace defined (i.e., null BondOrderMap). "
                       + "Assuming bond order one.";
            DENOPTIMLogger.appLogger.log(Level.WARNING, msg);
            
            Exception e = new Exception(msg);
            e.printStackTrace();
            
            return BondType.UNDEFINED;
        }
        else 
        {
            return bondOrderMap.getOrDefault(apRule, BondType.UNDEFINED);
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

    public static Set<String> getForbiddenEndList()
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
     * Returns the list of attachment points with the given class. The returned
     * identifiers have <code>vertex_id</code>=-1 
     * because these APs are only on the individual 
     * fragments held in the library and do not belong to any graph.
     * @param apclass 
     * @return the list of AP identifiers.
     */

    public static ArrayList<IdFragmentAndAP> getFragsWithAPClass(String apclass)
    {
        ArrayList<IdFragmentAndAP> lst = new ArrayList<IdFragmentAndAP>();
        
        if (fragsApsPerApClass.containsKey(apclass))
        {
            for (ArrayList<Integer> idxs : fragsApsPerApClass.get(apclass))
            {
                IdFragmentAndAP apId = new IdFragmentAndAP(-1, //vertexId
                                                   idxs.get(0), //MolId,
                                                   BBType.FRAGMENT,
                                                   idxs.get(1), //ApId
                                                   -1, //noVSym
                                                   -1);//noAPSym
                lst.add(apId);
            }
        }
        return lst;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Searches for all building blocks that are compatible with the given 
     * list of APs.
     * @param srcAPs the identifiers of APs meant to hold any of the desired
     * fragments.
     * @return a list of fragments.
     */
    public static ArrayList<DENOPTIMVertex> getFragmentsCompatibleWithTheseAPs(
                    ArrayList<IdFragmentAndAP> srcAPs)
    {
        // First we get all possible APs on any fragment
        ArrayList<IdFragmentAndAP> compatFragAps = 
                            FragmentSpace.getFragAPsCompatibleWithTheseAPs(srcAPs);
        
        // then keep unique fragment identifiers, and store unique
        Set<Integer> compatFragIds = new HashSet<Integer>();
        for (IdFragmentAndAP apId : compatFragAps)
        {
            compatFragIds.add(apId.getVertexMolId());
        }
        
        // Then we pack-up the selected list of fragments
        ArrayList<DENOPTIMVertex> compatFrags = 
                        new ArrayList<DENOPTIMVertex>();
        for (Integer fid : compatFragIds)
        {
            try {
                compatFrags.add(FragmentSpace.getVertexFromLibrary(
                            BBType.FRAGMENT, fid));
            } catch (DENOPTIMException e) {
                System.err.println("Exception while trying to get fragment '"+fid+"'!");
                e.printStackTrace();
            }
        }
        
        return compatFrags;
    }
    
//------------------------------------------------------------------------------
   
    /**
     * Searches for all APs that are compatible with the given list of APs.
     * @param srcAPs the identifiers of APs meant to hold any of the desired
     * fragments.
     * @return a list of identifiers for APs on fragments in the library.
     */
    public static ArrayList<IdFragmentAndAP> getFragAPsCompatibleWithTheseAPs(
                    ArrayList<IdFragmentAndAP> srcAPs)
    {
        ArrayList<IdFragmentAndAP> compFrAps = 
                new ArrayList<IdFragmentAndAP>();
        boolean first = true;
        for (IdFragmentAndAP apId : srcAPs)
        {
            String srcApCls = getAPClassForFragment(apId);
            ArrayList<IdFragmentAndAP> compForOne = 
                             getFragAPsCompatibleWithClass(srcApCls);

            if (first)
            {
                compFrAps.addAll(compForOne);
                first = false;
                continue;
            }
            
            ArrayList<IdFragmentAndAP> toKeep = 
                    new ArrayList<IdFragmentAndAP>();
            for (IdFragmentAndAP candAp : compFrAps)
            {
                for (IdFragmentAndAP newId : compForOne)
                {
                    if (newId.sameFragAndAp(candAp))
                    {
                            toKeep.add(candAp);
                            break;
                    }
                }
            }
            
            compFrAps = toKeep;
            
            if (compFrAps.size()==0)
            {
                    break;
            }
        }

        return compFrAps;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the list of attachment points found in the fragment 
     * space and that are compatible with a given AP class. 
     * Multiple APs can be found for each fragment.
     * @param srcApCls the AP class for which we want compatible APs.
     */
    
    public static ArrayList<IdFragmentAndAP> getFragAPsCompatibleWithClass(
                    String srcApCls)
    {
        ArrayList<IdFragmentAndAP> compatFragAps = 
                new ArrayList<IdFragmentAndAP>();
        
        // Take the compatible AP classes
        ArrayList<String> compatApClasses = 
             FragmentSpace.getCompatibleAPClasses(srcApCls);
        
        // Find all APs with any compatible class
        if (compatApClasses != null)
        {
            for (String compClass : compatApClasses)
            {
                compatFragAps.addAll(
                        FragmentSpace.getFragsWithAPClass(compClass));
            }
        }
        
        //TODO-V3: keep or trash?
        if (compatFragAps.size()==0)
        {
            System.out.println("WARNING: No compatible AP found in the "
                    + "fragment space for APClass '" + srcApCls + "'.");
        }
        
        return compatFragAps;
    }

//------------------------------------------------------------------------------

    /**
     * Checks if the symmetry settings impose use of symmetry on attachment 
     * points of the given AP class. The value returned is the result 
     * of the action of symmetry-related keyword affecting the definition of
     * this FragmentSpace.
     * @param apClass the attachment point class
     * @return <code>true<code> if symmetry has the applied on APs of the
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
     * Checks if there is a constraint on the constitutional 
     * symmetry probability for the given AP class.
     * @param apClass the attachment point class
     * @return <code>true<code> if there is a constraint on the constitutional
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
     * @return the constrained value of the symmetric substitution probability
     * (0.0 - 1.0).
     */

    public static double getSymmetryConstrain(String apClass)
    {
        return symmConstraints.get(apClass);
    }

//------------------------------------------------------------------------------

    public static void setScaffoldLibrary(ArrayList<DENOPTIMVertex> lib)
    {
        scaffoldLib = lib;
    }

//------------------------------------------------------------------------------

    public static void setFragmentLibrary(ArrayList<DENOPTIMVertex> lib)
    {
        fragmentLib = lib;
    }

//------------------------------------------------------------------------------

    public static void setCappingLibrary(ArrayList<DENOPTIMVertex> lib)
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

    public static void setBondOrderMap(HashMap<String, BondType> map)
    {
        bondOrderMap = map;
    }

//------------------------------------------------------------------------------

    public static void setCappingMap(HashMap<String, String> map)
    {
        cappingMap = map;
    }

//------------------------------------------------------------------------------

    public static void setForbiddenEndList(Set<String> lst)
    {
        forbiddenEndList = lst;
    }

//------------------------------------------------------------------------------

    public static void setFragPoolPerNumAP(
            HashMap<Integer,ArrayList<Integer>> map)
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

    public static void setAPClassesPerFrag(
            HashMap<Integer,ArrayList<String>> map)
    {
        apClassesPerFrag = map;
    }

//------------------------------------------------------------------------------

    public static void setSymmConstraints(HashMap<String,Double> map)
    {
        symmConstraints = map;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Clears all settings of this fragment space. All fields changed to
     * <code>null</code>.
     */
    public static void clearAll()
    {        
        scaffoldLib = null;
        fragmentLib = null;
        cappingLib = null;
        compatMap = null;
        rcCompatMap = null;
        bondOrderMap= null;
        cappingMap = null;
        forbiddenEndList = null;
        fragPoolPerNumAP = null;
        apClassesPerFrag = null;
        fragsApsPerApClass = null;
        symmConstraints = null;
        isValid = false;
    }
    
//------------------------------------------------------------------------------

}
