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

import static denoptimga.DENOPTIMGraphOperations.extractPattern;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.files.UndetectedFileFormatException;
import denoptim.graph.APClass;
import denoptim.graph.Candidate;
import denoptim.graph.DENOPTIMAttachmentPoint;
import denoptim.graph.DENOPTIMEdge.BondType;
import denoptim.graph.DENOPTIMGraph;
import denoptim.graph.DENOPTIMTemplate;
import denoptim.graph.DENOPTIMVertex;
import denoptim.graph.DENOPTIMVertex.BBType;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.utils.DENOPTIMMoleculeUtils;
import denoptim.utils.GraphUtils;
import denoptimga.GraphPattern;

/**
 * Class defining the fragment space
 * 
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */

// TODO: this will have to become thread specific if we want to allow multiple 
// GA/FSE runs to occur within the same JVM, but on different threads. A use
// case would be preparing and submitting multiple GA experiments from the GUI

public class FragmentSpace
{
    /**
     * Data structure containing the molecular representation of building
     * blocks: scaffolds section - fragments that can be used as seeds to grow a
     * new molecule. WARNING! The objects stores in the library do not have a
     * meaningful value for the two indexes representing the type of building
     * block and the position of the list of all building blocks of that type.
     */
    private static ArrayList<DENOPTIMVertex> scaffoldLib = null;

    /**
     * Data structure containing the molecular representation of building
     * blocks: fragment section - fragments for general use. WARNING! The
     * objects stores in the library do not have a meaningful value for the two
     * indexes representing the type of building block and the position of the
     * list of all building blocks of that type.
     */
    private static ArrayList<DENOPTIMVertex> fragmentLib = null;

    /**
     * Data structure containing the molecular representation of building
     * blocks: capping group section - fragments with only one attachment point
     * used to saturate unused attachment points on a graph. WARNING! The
     * objects stores in the library do not have a meaningful value for the two
     * indexes representing the type of building block and the position of the
     * list of all building blocks of that type.
     */
    private static ArrayList<DENOPTIMVertex> cappingLib = null;

    /**
     * Data structure that stored the true entries of the attachment point
     * classes compatibility matrix
     */
    private static HashMap<APClass, ArrayList<APClass>> compatMap;

    /**
     * Store references to the Ring-Closing Vertexes found in the library of 
     * fragments.
     */
    private static ArrayList<DENOPTIMVertex> rcvs = 
            new ArrayList<DENOPTIMVertex>();
    
    /**
     * Data structure that stores compatible APclasses for joining APs in
     * ring-closing bonds. Symmetric, purpose specific compatibility matrix.
     */
    private static HashMap<APClass, ArrayList<APClass>> rcCompatMap;

    /**
     * Data structure that stores the correspondence between bond order and
     * attachment point class.
     */
    private static HashMap<String, BondType> bondOrderMap;

    /**
     * Data structure that stores the AP-classes to be used to cap unused APS on
     * the growing molecule.
     */
    private static HashMap<APClass, APClass> cappingMap;
    
    /**
     * Data structure that stores AP classes that cannot be held unused
     */
    private static Set<APClass> forbiddenEndList;

    /**
     * Clusters of fragments based on the number of APs
     */
    private static HashMap<Integer, ArrayList<Integer>> fragPoolPerNumAP;

    /**
     * List of APClasses per each fragment
     */
    private static HashMap<Integer, ArrayList<APClass>> apClassesPerFrag;

    /**
     * Clusters of fragments'AP based on AP classes
     */
    private static HashMap<APClass, ArrayList<ArrayList<Integer>>> fragsApsPerApClass;
    
    /**
     * APclass-specific constraints to constitutional symmetry
     */
    private static HashMap<APClass, Double> symmConstraints;

    /**
     * Flag defining use of AP class-based approach
     */
    protected static boolean apClassBasedApproch = false;

    /**
     * Flag signalling that this fragment space was built and validated
     */
    private static boolean isValid = false;

    /**
     * Unique identified for vertices
     */
    public static AtomicInteger vrtxID = new AtomicInteger(0);
    
    /**
     * Index used to keep the order in a list of attachment points
     */
    public static AtomicInteger apID = new AtomicInteger(0);

//------------------------------------------------------------------------------

    /**
     * Define all components of a fragment space that implements the attachment
     * point class-approach.
     * 
     * @param scaffFile  pathname to library of fragments used to start the
     *                   construction of any new graph (i.e., seed or root
     *                   fragments, a.k.a. scaffolds).
     * @param fragFile   pathname to the library of fragments for general
     *                   purpose.
     * @param capFile    pathname to the library of single-AP fragments used to
     *                   cap free attachment points (i.e., the capping groups).
     * @param cpmFile    pathname to the compatibility matrix, bond type
     *                   mapping, capping, and forbidden ends rules.
     * @throws DENOPTIMException
     */
    public static void defineFragmentSpace(String scaffFile, String fragFile,
            String capFile, String cpmFile) throws DENOPTIMException
    {
        defineFragmentSpace(scaffFile, fragFile, capFile, cpmFile, "",
                new HashMap<APClass, Double>());
    }

//------------------------------------------------------------------------------

    /**
     * Define all components of a fragment space that implements the attachment
     * point class-approach.
     * 
     * @param scaffLib library of fragments used to start the construction of
     *                 any new graph (i.e., seed or root fragments, a.k.a.
     *                 scaffolds).
     * @param fragLib  library of fragments for general purpose.
     * @param cappLib  library of single-AP fragments used to cap free
     *                 attachment points (i.e., the capping groups).
     * @param cpMap    the APClass compatibility map. This data structure is a
     *                 map of the APClass-on-growing-graph (key) to list of
     *                 permitted APClasses on incoming fragment (values).
     * @param boMap    the map of APClass into bond order. This data structure
     *                 is a map of APClass (keys) to bond order as integer
     *                 (values).
     * @param capMap   the capping rules. This data structure is a map of
     *                 APClass-to-cap (keys) to APClass-of-capping-group
     *                 (values).
     * @param forbEnds the list of forbidden ends, i.e., APClasses that cannot
     *                 be left unused neither capped.
     * @param rcCpMap  the APClass compatibility matrix for ring closures.
     * @throws DENOPTIMException
     */
    public static void defineFragmentSpace(ArrayList<DENOPTIMVertex> scaffLib,
            ArrayList<DENOPTIMVertex> fragLib,
            ArrayList<DENOPTIMVertex> cappLib,
            HashMap<APClass, ArrayList<APClass>> cpMap,
            HashMap<String, BondType> boMap, HashMap<APClass, APClass> capMap,
            HashSet<APClass> forbEnds,
            HashMap<APClass, ArrayList<APClass>> rcCpMap)
            throws DENOPTIMException
    {
        setScaffoldLibrary(scaffLib);
        setFragmentLibrary(fragLib);
        setCappingLibrary(cappLib);
        setCompatibilityMatrix(cpMap);
        apClassBasedApproch = true;
        setBondOrderMap(boMap);
        setCappingMap(capMap);
        setForbiddenEndList(forbEnds);
        setRCCompatibilityMatrix(rcCpMap);

        FragmentSpaceUtils.groupAndClassifyFragments(apClassBasedApproch);

        isValid = true;
    }

//------------------------------------------------------------------------------

    /**
     * Define all components of a fragment space that implements the attachment
     * point class-approach.
     * 
     * @param scaffFile  pathname to library of fragments used to start the
     *                   construction of any new graph (i.e., seed or root
     *                   fragments, a.k.a. scaffolds).
     * @param fragFile   pathname to the library of fragments for general
     *                   purpose.
     * @param capFile    pathname to the library of single-AP fragments used to
     *                   cap free attachment points (i.e., the capping groups).
     * @param cpmFile    pathname to the compatibility matrix, bond type
     *                   mapping, capping, and forbidden ends rules.
     * @param rcpmFile   the APClass compatibility matrix for ring closures.
     * @param symCntrMap the map of symmetry constraints
     * @throws DENOPTIMException
     */
    public static void defineFragmentSpace(String scaffFile, String fragFile,
            String capFile, String cpmFile, String rcpmFile,
            HashMap<APClass, Double> symCntrMap) throws DENOPTIMException
    {
        HashMap<APClass, ArrayList<APClass>> cpMap = 
                new HashMap<APClass, ArrayList<APClass>>();
        HashMap<String, BondType> boMap = new HashMap<String, BondType>();
        HashMap<APClass, APClass> capMap = new HashMap<APClass, APClass>();
        HashSet<APClass> forbEnds = new HashSet<APClass>();
        if (cpmFile.length() > 0)
        {
            DenoptimIO.readCompatibilityMatrix(cpmFile, cpMap, boMap, capMap,
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
            HashMap<APClass, ArrayList<APClass>> rcCpMap = 
                    new HashMap<APClass, ArrayList<APClass>>();
            DenoptimIO.readRCCompatibilityMatrix(rcpmFile, rcCpMap);
            setRCCompatibilityMatrix(rcCpMap);
        }

        isValid = true;

        // We load first the capping groups because there should not be any
        // template in there.
        if (capFile.length() > 0)
        {
            cappingLib = new ArrayList<DENOPTIMVertex>();
            try
            {
                DenoptimIO.appendVerticesFromFileToLibrary(new File(capFile),
                        BBType.CAP, cappingLib, true);
            } catch (IllegalArgumentException | UndetectedFileFormatException
                    | IOException | DENOPTIMException e)
            {
                throw new DENOPTIMException("Cound not read library of capping "
                        + "groups from file '" + capFile + "'.", e);
            }
        }
        
        fragmentLib = new ArrayList<DENOPTIMVertex>();
        try
        {
            DenoptimIO.appendVerticesFromFileToLibrary(new File(fragFile),
                    BBType.FRAGMENT, fragmentLib, true);
        } catch (IllegalArgumentException | UndetectedFileFormatException
                | IOException | DENOPTIMException e)
        {
            throw new DENOPTIMException("Cound not read library of fragments "
                    + "from file '" + fragFile + "'.", e);
        }
        
        scaffoldLib = new ArrayList<DENOPTIMVertex>();
        try
        {
            DenoptimIO.appendVerticesFromFileToLibrary(new File(scaffFile),
                    BBType.SCAFFOLD, scaffoldLib, true);
        } catch (IllegalArgumentException | UndetectedFileFormatException
                | IOException | DENOPTIMException e)
        {
            throw new DENOPTIMException("Cound not read library of scaffolds "
                    + "from file '" + fragFile + "'.", e);
        }

        FragmentSpaceUtils.groupAndClassifyFragments(useAPclassBasedApproach());
    }

//------------------------------------------------------------------------------

    /**
     * Checks for valid definition of this fragment space.
     * @return <code>true</code> if this fragment space has been defined
     */
    public static boolean isDefined()
    {
        return isValid;
    }

//------------------------------------------------------------------------------
    
    /**
     * Set the fragment space to behave according to APClass-based approach.
     */
    public static void setAPclassBasedApproach(boolean useAPC)
    {
        apClassBasedApproch = useAPC;
    }
    
//------------------------------------------------------------------------------

    /**
     * Check usage of APClass-based approach, i.e., uses attachment points with
     * annotated data (i.e., the APClass) to evaluate compatibilities between
     * attachment points.
     * 
     * @return <code>true</code> if this fragment space makes use of
     *         APClass-based approach
     */
    public static boolean useAPclassBasedApproach()
    {
        return apClassBasedApproch;
    }

//------------------------------------------------------------------------------

    /**
     * Search for a specific AP on a specific fragment and finds out its class.
     * 
     * @param apId the identified of a specific attachment point.
     * @return the AP class or null
     */

    public static APClass getAPClassForFragment(IdFragmentAndAP apId)
    {
        APClass cls = null;
        try
        {
            DENOPTIMVertex frg = FragmentSpace.getVertexFromLibrary(
                    apId.getVertexMolType(), apId.getVertexMolId());
            cls = frg.getAttachmentPoints().get(apId.getApId()).getAPClass();
        } catch (Throwable t)
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
     * @param bbType the type of building block. This basically selects the 
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

    public static DENOPTIMVertex getVertexFromLibrary(
            DENOPTIMVertex.BBType bbType, int bbIdx) throws DENOPTIMException
    {
        String msg = "";
        switch (bbType)
        {
            case SCAFFOLD:
                if (scaffoldLib == null)
                {
                    msg = "Cannot retrieve scaffolds before initialising the "
                            + "scaffold library.";
                    throw new DENOPTIMException(msg);
                }
                break;
            case FRAGMENT:
                if (fragmentLib == null)
                {
                    msg = "Cannot retrieve fragments before initialising the "
                            + "fragment library.";
                    throw new DENOPTIMException(msg);
                }
                break;
            case CAP:
                if (cappingLib == null)
                {
                    msg = "Cannot retrieve capping groups before initialising"
                            + "the library of capping groups.";
                    throw new DENOPTIMException(msg);
                }
                break;
        }

        DENOPTIMVertex originalVrtx = null;
        switch (bbType)
        {
            case SCAFFOLD:
                if (bbIdx >-1 && bbIdx < scaffoldLib.size())
                {
                    originalVrtx = scaffoldLib.get(bbIdx);        
                }
                else
                {
                    msg = "Mismatch between scaffold bbIdx (" + bbIdx 
                            + ") and size of the library (" + scaffoldLib.size()
                            + "). FragType: " + bbType;
                    DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                    throw new DENOPTIMException(msg);
                }
                break;
                
            case FRAGMENT:
                if (bbIdx >-1 && bbIdx < fragmentLib.size())
                {
                    originalVrtx = fragmentLib.get(bbIdx);
                }
                else
                {
                    msg = "Mismatch between fragment bbIdx (" + bbIdx 
                            + ") and size of the library (" + fragmentLib.size()
                            + "). FragType: " + bbType;
                    DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                    throw new DENOPTIMException(msg);
                }
                break;
                
            case CAP:
                if (bbIdx >-1 && bbIdx < cappingLib.size())
                {
                    originalVrtx = cappingLib.get(bbIdx);
                }
                else
                {
                    msg = "Mismatch between capping group bbIdx " + bbIdx 
                            + ") and size of the library (" + cappingLib.size()
                            + "). FragType: " + bbType;
                    DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                    throw new DENOPTIMException(msg);
                }
                break;
                
            case UNDEFINED:
                msg = "Attempting to take UNDEFINED type of building block from "
                        + "fragment library.";
                DENOPTIMLogger.appLogger.log(Level.WARNING, msg);
                if (bbIdx < fragmentLib.size())
                {
                    originalVrtx = fragmentLib.get(bbIdx);
                }
                else
                {
                    msg = "Mismatch between fragment bbIdx (" + bbIdx 
                            + ") and size of the library (" + fragmentLib.size()
                            + "). FragType: " + bbType;
                    DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                    throw new DENOPTIMException(msg);
                }
                break;
    
            default:
                msg = "Unknown type of fragment '" + bbType + "'.";
                DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                throw new DENOPTIMException(msg);
        }
        DENOPTIMVertex clone = originalVrtx.clone();
        
        clone.setVertexId(GraphUtils.getUniqueVertexIndex());

        clone.setBuildingBlockId(bbIdx);
        if (originalVrtx.getBuildingBlockId() != bbIdx)
        {
            System.err.println("WARNING! Mismatch between building block ID ("
                    + originalVrtx.getBuildingBlockId() + ") and position in "
                    + "the list of building blocks (" + bbIdx + ") for type "
                    + bbType + ".");
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
     * @param capApCls the APClass of the attachment point on the capping group
     * @return all the capping groups which have the given APclass
     */

    public static ArrayList<Integer> getCappingGroupsWithAPClass(
            APClass capApCls)
    {
        ArrayList<Integer> selected = new ArrayList<>();
        for (int i = 0; i < cappingLib.size(); i++)
        {
            APClass apc = null;
            try
            {
                apc = getVertexFromLibrary(DENOPTIMVertex.BBType.CAP, i)
                        .getAttachmentPoints().get(0).getAPClass();
                if (apc.equals(capApCls))
                {
                    selected.add(i);
                }
            } catch (DENOPTIMException de)
            {
                // nothing
            }
        }
        return selected;
    }

//------------------------------------------------------------------------------

    /**
     * Load info from a compatibility matrix file. This method imports
     * information such as the compatibility matrix, bond order map, and
     * forbidden ends from a compatibility matrix file (i.e., a formatted text
     * file using DENOPTIM keyword. This overrides any previous setting of such
     * information in this FragmentSpace.
     * 
     * @param inFile the pathname of the compatibility matrix file
     */

    public static void importCompatibilityMatrixFromFile(String inFile)
            throws DENOPTIMException
    {
        setCompatibilityMatrix(new HashMap<APClass, ArrayList<APClass>>());
        setBondOrderMap(new HashMap<String, BondType>());
        setCappingMap(new HashMap<APClass, APClass>());
        setForbiddenEndList(new HashSet<APClass>());
        DenoptimIO.readCompatibilityMatrix(inFile, compatMap, bondOrderMap,
                cappingMap, forbiddenEndList);
    }

//------------------------------------------------------------------------------

    /**
     * Load info for ring closures compatibilities from a compatibility matrix
     * file.
     * 
     * @param inFile the pathname of the RC-compatibility matrix file
     */

    public static void importRCCompatibilityMatrixFromFile(String inFile)
            throws DENOPTIMException
    {
        setRCCompatibilityMatrix(new HashMap<APClass, ArrayList<APClass>>());
        DenoptimIO.readRCCompatibilityMatrix(inFile, rcCompatMap);
    }

//------------------------------------------------------------------------------

    public static HashMap<APClass, ArrayList<APClass>> getCompatibilityMatrix()
    {
        return compatMap;
    }

//------------------------------------------------------------------------------

    /**
     * Returns a list of APClasses compatible with the given APClass. The
     * compatibility among classes is defined by the compatibility matrix
     * @param apc
     * @return the list of compatible APClasses. Can be empty but not null.
     */
    public static ArrayList<APClass> getCompatibleAPClasses(APClass apc)
    {   
        if (compatMap!= null && compatMap.containsKey(apc))
        {
            return compatMap.get(apc);
        }
        return new ArrayList<APClass>();
    }

//------------------------------------------------------------------------------

    /**
     * Returns the compatibility matrix for ring closing fragment-fragment
     * connections or <code>null</code> if not provided in the parameters file.
     * 
     * @return
     */

    public static HashMap<APClass, ArrayList<APClass>> getRCCompatibilityMatrix()
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
     * 
     * @param apclass the APclass to be converted into bond order.
     * @return the bond order as an integer, or 1 if either the Fragment space
     *         is not defined, that is, the bond order map is <code>null</code>,
     *         or a fully defined map does not include any mapping for the given
     *         APClass.
     */
    public static BondType getBondOrderForAPClass(APClass apc)
    {
        if (apc!= null && BondType.UNDEFINED != apc.getBondType())
            return apc.getBondType();
        
        if (bondOrderMap == null || apc == null)
        {
            String msg = "Attempting to get bond order, but no "
                    + "FragmentSpace defined (i.e., null BondOrderMap). "
                    + "Assuming edge/APClass represents an " 
                    + BondType.UNDEFINED
                    + " bond.";
            DENOPTIMLogger.appLogger.log(Level.WARNING, msg);

            return BondType.UNDEFINED;
        } else
        {
            return bondOrderMap.getOrDefault(apc.getRule(), BondType.UNDEFINED);
        }
    }

//------------------------------------------------------------------------------

    public static HashMap<APClass, APClass> getCappingMap()
    {
        return cappingMap;
    }

//------------------------------------------------------------------------------

    /**
     * @param srcApClass the attachment point class of the attachment point to
     *                   be capped
     * @return the APClass of the capping group or null
     */

    public static APClass getAPClassOfCappingVertex(APClass srcApClass)
    {
        return cappingMap.get(srcApClass);
    }

//------------------------------------------------------------------------------

    public static Set<APClass> getForbiddenEndList()
    {
        return forbiddenEndList;
    }

//------------------------------------------------------------------------------

    /**
     * Return the set of APClasses that used in the compatibility matrix for the
     * growing graph APs. Note these APClasses do include subclasses. For
     * example, for AP with class <code>MyAPClass:0</code> the <code>0</code> is
     * the subclass.
     * 
     * @return the lst of APClasses
     */

    public static Set<APClass> getAllAPClassesFromCPMap()
    {
        return FragmentSpace.getCompatibilityMatrix().keySet();
    }

//------------------------------------------------------------------------------

    /**
     * Return the set of APClasses that are defined in the bond order map. Note
     * the APClasses in the bond order map fo not include the subclass. For
     * example, for AP with class <code>MyAPClass:0</code> the map stores only
     * <code>MyAPClass</code>.
     * 
     * @return the lst of APClasses
     */

    public static Set<String> getAllAPClassesFromBOMap()
    {
        return FragmentSpace.getBondOrderMap().keySet();
    }

//------------------------------------------------------------------------------

    public static HashMap<Integer, ArrayList<Integer>> getMapOfFragsPerNumAps()
    {
        return fragPoolPerNumAP;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the list of fragments with given number of APs
     * 
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

    public static HashMap<Integer, ArrayList<APClass>> getMapAPClassesPerFragment()
    {
        return apClassesPerFrag;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the APclasses associated with a given fragment.
     * 
     * @param fragId the index of the fragment in the library
     * @return the list of APclasses found of the fragment
     */

    public static ArrayList<APClass> getAPClassesPerFragment(int fragId)
    {
        return apClassesPerFrag.get(fragId);
    }

//------------------------------------------------------------------------------

    public static HashMap<APClass, ArrayList<ArrayList<Integer>>> getMapFragsAPsPerAPClass()
    {
        return fragsApsPerApClass;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the list of attachment points with the given class. The returned
     * identifiers have <code>vertex_id</code>=-1 because these APs are only on
     * the individual fragments held in the library and do not belong to any
     * graph.
     * 
     * @param apc
     * @return the list of AP identifiers.
     */

    public static ArrayList<IdFragmentAndAP> getFragsWithAPClass(APClass apc)
    {
        ArrayList<IdFragmentAndAP> lst = new ArrayList<IdFragmentAndAP>();

        if (fragsApsPerApClass.containsKey(apc))
        {
            for (ArrayList<Integer> idxs : fragsApsPerApClass.get(apc))
            {
                IdFragmentAndAP apId = new IdFragmentAndAP(-1, // vertexId
                        idxs.get(0), // MolId,
                        BBType.FRAGMENT, idxs.get(1), // ApId
                        -1, // noVSym
                        -1);// noAPSym
                lst.add(apId);
            }
        }
        return lst;
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the list of attachment points with the given class. 
     * @param apc the attachment point class to search.
     * @return the list of matching attachment points.
     */

    public static ArrayList<DENOPTIMVertex> getVerticesWithAPClass(APClass apc)
    {
        ArrayList<DENOPTIMVertex> lst = new ArrayList<DENOPTIMVertex>();
        
        if (fragsApsPerApClass.containsKey(apc))
        {
            for (ArrayList<Integer> idxs : fragsApsPerApClass.get(apc))
            {
                DENOPTIMVertex v = FragmentSpace.getFragmentLibrary()
                        .get(idxs.get(0));
                lst.add(v);
            }
        }
        return lst;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Searches for all building blocks that are compatible with the given list
     * of APs.
     * 
     * @param srcAPs the identifiers of APs meant to hold any of the desired
     *               fragments.
     * @return a list of fragments.
     */
    public static ArrayList<DENOPTIMVertex> getFragmentsCompatibleWithTheseAPs(
            ArrayList<IdFragmentAndAP> srcAPs)
    {
        // First we get all possible APs on any fragment
        ArrayList<IdFragmentAndAP> compatFragAps = FragmentSpace
                .getFragAPsCompatibleWithTheseAPs(srcAPs);

        // then keep unique fragment identifiers, and store unique
        Set<Integer> compatFragIds = new HashSet<Integer>();
        for (IdFragmentAndAP apId : compatFragAps)
        {
            compatFragIds.add(apId.getVertexMolId());
        }

        // Then we pack-up the selected list of fragments
        ArrayList<DENOPTIMVertex> compatFrags = new ArrayList<DENOPTIMVertex>();
        for (Integer fid : compatFragIds)
        {
            try {
                compatFrags.add(FragmentSpace.getVertexFromLibrary(
                            DENOPTIMVertex.BBType.FRAGMENT, fid));
            } catch (DENOPTIMException e) {
                System.err.println("Exception while trying to get fragment '" 
                        + fid + "'!");
                e.printStackTrace();
            }
        }

        return compatFrags;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Searches for all attachment points that are compatible with the given 
     * list of attachment points.
     * @param srcAPs attachment points meant to results have to be compatible 
     * with.
     * @return a list of compatible attachment points found in the library of
     * fragments.
     */
    public static ArrayList<DENOPTIMAttachmentPoint> getAPsCompatibleWithThese(
                    ArrayList<DENOPTIMAttachmentPoint> srcAPs)
    {
        ArrayList<DENOPTIMAttachmentPoint> compAps = 
                new ArrayList<DENOPTIMAttachmentPoint>();
        boolean first = true;
        for (DENOPTIMAttachmentPoint ap : srcAPs)
        { 
            ArrayList<DENOPTIMAttachmentPoint> compForOne = 
                    getAPsCompatibleWithClass(ap.getAPClass());

            if (first)
            {
                compAps.addAll(compForOne);
                first = false;
                continue;
            }
            
            ArrayList<DENOPTIMAttachmentPoint> toKeep = 
                    new ArrayList<DENOPTIMAttachmentPoint>();
            for (DENOPTIMAttachmentPoint candAp : compAps)
            {
                for (DENOPTIMAttachmentPoint newCand : compForOne)
                {
                    if (newCand == candAp)
                    {
                            toKeep.add(candAp);
                            break;
                    }
                }
            }
            
            compAps = toKeep;
            
            if (compAps.size()==0)
            {
                break;
            }
        }
        return compAps;
    }
    
//------------------------------------------------------------------------------

    /**
     * Searches for all APs that are compatible with the given list of APs.
     * 
     * @param srcAPs the identifiers of APs meant to hold any of the desired
     *               fragments.
     * @return a list of identifiers for APs on fragments in the library.
     */
    public static ArrayList<IdFragmentAndAP> getFragAPsCompatibleWithTheseAPs(
            ArrayList<IdFragmentAndAP> srcAPs)
    {
        ArrayList<IdFragmentAndAP> compFrAps = new ArrayList<IdFragmentAndAP>();
        boolean first = true;
        for (IdFragmentAndAP apId : srcAPs)
        {
            APClass srcApCls = getAPClassForFragment(apId);
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

            if (compFrAps.size() == 0)
            {
                break;
            }
        }

        return compFrAps;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the list of attachment points found in the fragment space and
     * that are compatible with a given AP class. Multiple APs can be found for
     * each fragment.
     * 
     * @param aPC1 the AP class for which we want compatible APs.
     */
    
    public static ArrayList<DENOPTIMAttachmentPoint> getAPsCompatibleWithClass(
                    APClass aPC1)
    {
        ArrayList<DENOPTIMAttachmentPoint> compatAps = 
                new ArrayList<DENOPTIMAttachmentPoint>();
        
        // Take the compatible AP classes
        ArrayList<APClass> compatApClasses = 
             FragmentSpace.getCompatibleAPClasses(aPC1);
        
        // Find all APs with a compatible class
        if (compatApClasses != null)
        {
            for (APClass klass : compatApClasses)
            {
                ArrayList<DENOPTIMVertex> vrtxs = getVerticesWithAPClass(klass);
                for (DENOPTIMVertex v : vrtxs)
                {
                    for (DENOPTIMAttachmentPoint ap : v.getAttachmentPoints())
                    {
                        if (ap.getAPClass() == klass)
                        {
                            compatAps.add(ap);
                        }
                    }
                }
            }
        }
        
        if (compatAps.size()==0)
        {
            DENOPTIMLogger.appLogger.log(Level.WARNING,"No compatible "
                    + "AP found in the fragment space for APClass '" 
                    + aPC1 + "'.");
        }
        
        return compatAps;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the list of attachment points found in the fragment 
     * space and that are compatible with a given AP class. 
     * Multiple APs can be found for each fragment.
     * @param aPC1 the AP class for which we want compatible APs.
     */

    public static ArrayList<IdFragmentAndAP> getFragAPsCompatibleWithClass(
            APClass aPC1)
    {
        ArrayList<IdFragmentAndAP> compatFragAps = 
                new ArrayList<IdFragmentAndAP>();

        // Take the compatible AP classes
        ArrayList<APClass> compatApClasses = FragmentSpace
                .getCompatibleAPClasses(aPC1);

        // Find all APs with any compatible class
        if (compatApClasses != null)
        {
            for (APClass compClass : compatApClasses)
            {
                compatFragAps.addAll(FragmentSpace.getFragsWithAPClass(
                        compClass));
            }
        }

        return compatFragAps;
    }

//------------------------------------------------------------------------------

    /**
     * Checks if the symmetry settings impose use of symmetry on attachment
     * points of the given AP class. The value returned is the result of the
     * action of symmetry-related keyword affecting the definition of this
     * FragmentSpace.
     * 
     * @param apClass the attachment point class
     * @return <code>true<code> if symmetry has the applied on APs of the given
     *         class
     */

    public static boolean imposeSymmetryOnAPsOfClass(APClass apClass)
    {
    	boolean res = true;
        if (hasSymmetryConstrain(apClass))
        {
            if (getSymmetryConstrain(apClass) < (1.0
                    - DENOPTIMConstants.FLOATCOMPARISONTOLERANCE))
            {
                res = false;
            }
        } else
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
     * Checks if there is a constraint on the constitutional symmetry
     * probability for the given AP class.
     * 
     * @param apClass the attachment point class
     * @return <code>true<code> if there is a constraint on the constitutional
     *         symmetry probability for the given AP class.
     */

    public static boolean hasSymmetryConstrain(APClass apClass)
    {
        return symmConstraints.containsKey(apClass);
    }

//------------------------------------------------------------------------------

    /**
     * Return the constitutional symmetry constrain for the given APclass, or
     * null. The constrain is a fixed probability that is not dependent on the
     * distance from the root of the DENOPTIMGraph (i.e. the level).
     * 
     * @param apClass the attachment point class
     * @return the constrained value of the symmetric substitution probability
     *         (0.0 - 1.0).
     */

    public static double getSymmetryConstrain(APClass apClass)
    {
        return symmConstraints.get(apClass);
    }

//------------------------------------------------------------------------------

    public static void setScaffoldLibrary(ArrayList<DENOPTIMVertex> lib)
    {
        scaffoldLib = new ArrayList<DENOPTIMVertex>();
        FragmentSpace.appendVerticesToLibrary(lib,
                BBType.SCAFFOLD, scaffoldLib);
    }

    public static void setFragmentLibrary(ArrayList<DENOPTIMVertex> lib)
    {
        fragmentLib = new ArrayList<DENOPTIMVertex>();
        FragmentSpace.appendVerticesToLibrary(lib,
                BBType.FRAGMENT, fragmentLib);
    }

//------------------------------------------------------------------------------

    public static void setCappingLibrary(ArrayList<DENOPTIMVertex> lib)
    {
        cappingLib = new ArrayList<DENOPTIMVertex>();
        FragmentSpace.appendVerticesToLibrary(lib,
                BBType.CAP, cappingLib);
    }

//------------------------------------------------------------------------------

    public static void setCompatibilityMatrix(
            HashMap<APClass, ArrayList<APClass>> map)
    {
        compatMap = map;
    }

//------------------------------------------------------------------------------

    public static void setRCCompatibilityMatrix(
            HashMap<APClass, ArrayList<APClass>> map)
    {
        rcCompatMap = map;
    }

//------------------------------------------------------------------------------

    public static void setBondOrderMap(HashMap<String, BondType> map)
    {
        bondOrderMap = map;
    }

//------------------------------------------------------------------------------

    public static void setCappingMap(HashMap<APClass, APClass> map)
    {
        cappingMap = map;
    }

//------------------------------------------------------------------------------

    public static void setForbiddenEndList(Set<APClass> lst)
    {
        forbiddenEndList = lst;
    }

//------------------------------------------------------------------------------

    public static void setFragPoolPerNumAP(
            HashMap<Integer, ArrayList<Integer>> map)
    {
        fragPoolPerNumAP = map;
    }

//------------------------------------------------------------------------------

    public static void setFragsApsPerApClass(
            HashMap<APClass, ArrayList<ArrayList<Integer>>> map)
    {
        fragsApsPerApClass = map;
    }

//------------------------------------------------------------------------------

    public static void setAPClassesPerFrag(
            HashMap<Integer, ArrayList<APClass>> map)
    {
        apClassesPerFrag = map;
    }

//------------------------------------------------------------------------------

    public static void setSymmConstraints(HashMap<APClass, Double> map)
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
        bondOrderMap = null;
        cappingMap = null;
        forbiddenEndList = null;
        fragPoolPerNumAP = null;
        apClassesPerFrag = null;
        fragsApsPerApClass = null;
        symmConstraints = null;
        isValid = false;
    }

//------------------------------------------------------------------------------

    /**
     * Takes a list of atom containers and converts it into a list of vertices
     * that are added to a given library. 
     * @param list of atom containers to import.
     * @param bbt the type of building block the vertices should be set to.
     * @param library where to import the vertices to.
     */
    
    public static void appendIACsAsVerticesToLibrary(
            ArrayList<IAtomContainer>list, DENOPTIMVertex.BBType bbt,
            ArrayList<DENOPTIMVertex> library)
    {
        for(IAtomContainer iac : list)
        {
            DENOPTIMVertex v = null;
            try
            {
                v = DENOPTIMVertex.convertIACToVertex(iac,bbt);
            } catch (Throwable e)
            {
                e.printStackTrace();
                System.err.println("ERROR! Could not import "+bbt+". Failed "
                        + "conversion of IAtomContainer to "+bbt+".");
                System.exit(-1);;
            }
            appendVertexToLibrary(v,bbt,library);
        }
        // NB: do not try to add all vertices in one. If there are templates
        // that are built using vertices that are imported in the same run of
        // this method, then the building blocks must be added to the library
        // before the template is added. The latter will, in fact, require
        // to find the building blocks in the library.
    }
    
//------------------------------------------------------------------------------

    /**
     * Takes a list of vertices and add them to a given library. Each vertex
     * is assigned the building block type and ID. 
     * do not try to add all vertices in one. If there are templates
     * that are built using vertices that are imported in the same run of
     * this method, then the building blocks must be added to the library
     * before the template is added. The latter will, in fact, require
     * to find the building blocks in the library.
     * 
     * @param list of vertices to import.
     * @param bbt the type of building block the vertices should be set to.
     * @param library where to import the vertices to.
     */
    
    public static void appendVerticesToLibrary(ArrayList<DENOPTIMVertex> list, 
            DENOPTIMVertex.BBType bbt, ArrayList<DENOPTIMVertex> library)
    {
        for (DENOPTIMVertex v : list)
        {
            appendVertexToLibrary(v,bbt,library);
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Takes a vertex and add it to a given library. Each vertex
     * is assigned the building block type and ID. 
     * 
     * @param v  vertex to import.
     * @param bbt the type of building block the vertex should be set to.
     * @param library where to import the vertex to.
     */
    
    public static void appendVertexToLibrary(DENOPTIMVertex v, 
            DENOPTIMVertex.BBType bbt, ArrayList<DENOPTIMVertex> library)
    {
        v.setBuildingBlockId(library.size());
        v.setBuildingBlockType(bbt);
        library.add(v);
    }
    
//------------------------------------------------------------------------------

    /**
     * Extracts a system of one or more fused rings and adds them to the
     * fragment space if not already present. This method does not
     * create any molecular geometry in the building block that is stores
     * in the library. See {@link FragmentSpace#addFusedRingsToFragmentLibrary(DENOPTIMGraph, boolean, boolean, IAtomContainer)}
     * to include a molecular representation on the stored building block.
     * <b>WARNING</b> 
     * Expanding the libraries of 
     * building blocks on-the-fly has the potential to overload the memory.
     */
    
    //TODO: need something to prevent memory overload: 
    // -> keep only some templates?
    // -> remove those who did not lead to good population members?
    // -> remove redundant? The highest-simmetry principle (i.e., rather than 
    //    keeping a template as it is, we'd like to keep its highest symmetry 
    //    isomorphic) would be the first thing to do.
    
    public static void addFusedRingsToFragmentLibrary(DENOPTIMGraph graph) 
    {
        addFusedRingsToFragmentLibrary(graph,true,true);
    }
    
//------------------------------------------------------------------------------

    /**
     * Extracts a system of one or more fused rings and adds them to the
     * fragment space if not already present. <b>WARNING</b> 
     * Expanding the libraries of 
     * building blocks on-the-fly has the potential to overload the memory.
     * @param graph the graph to analyze and possibly chop.
     * @param addIfScaffold use <code>true</code> to enable saving of new
     * ring systems that contain any scaffold vertexes as new scaffolds.
     * @param addIfFragment use <code>true</code> to enable saving of new
     * ring systems that do NOT contain any scaffold vertexes as new fragments.
     */
    
    public static void addFusedRingsToFragmentLibrary(DENOPTIMGraph graph,
            boolean addIfScaffold, boolean addIfFragment) 
    {
        addFusedRingsToFragmentLibrary(graph, addIfScaffold, addIfFragment, null);
    }
    
//------------------------------------------------------------------------------

    /**
     * Extracts a system of one or more fused rings and adds them to the
     * fragment space if not already present. <b>WARNING</b> 
     * Expanding the libraries of 
     * building blocks on-the-fly has the potential to overload the memory.
     * @param graph the graph to analyze and possibly chop.
     * @param addIfScaffold use <code>true</code> to enable saving of new
     * ring systems that contain any scaffold vertexes as new scaffolds.
     * @param addIfFragment use <code>true</code> to enable saving of new
     * ring systems that do NOT contain any scaffold vertexes as new fragments.
     * @param wholeMol the complete molecular representation of 
     * <code>graph</code>. If this parameter is not null, then we'll try to use
     * the geometry found in this parameter to define the geometry of the 
     * fused-rings templates. The atoms in this container are expected to have 
     * property {@link DENOPTIMConstants#ATMPROPVERTEXID} that defines the 
     * ID of the vertex from which each atom comes from.
     */
    
    //TODO: need something to prevent memory overload: 
    // -> keep only some templates?
    // -> remove those who did not lead to good population members?
    // -> remove redundant? The highest-symmetry principle (i.e., rather than 
    //    keeping a template as it is, we'd like to keep its highest symmetry 
    //    isomorphic) would be the first thing to do.
    
    public static void addFusedRingsToFragmentLibrary(DENOPTIMGraph graph,
            boolean addIfScaffold, boolean addIfFragment, IAtomContainer wholeMol) 
    {
        List<DENOPTIMGraph> subgraphs = null;
        try
        {   
            subgraphs = extractPattern(graph,GraphPattern.RING);
        } catch (DENOPTIMException e1)
        {
            DENOPTIMLogger.appLogger.log(Level.WARNING, "Failed to extract "
                    + "fused ring patters.");
            e1.printStackTrace();
        }

        for (DENOPTIMGraph g : subgraphs) 
        {
            BBType type = g.hasScaffoldTypeVertex() ? 
                    BBType.SCAFFOLD :
                        BBType.FRAGMENT;
            
            if (!addIfFragment && type == BBType.FRAGMENT) 
            {
                continue;
            }
            if (!addIfScaffold && type == BBType.SCAFFOLD)
            {
                continue;
            }

            ArrayList<DENOPTIMVertex> library = type == BBType.FRAGMENT ?
                    FragmentSpace.getFragmentLibrary() :
                        FragmentSpace.getScaffoldLibrary();
            
            synchronized (library)
            {
                if (!hasIsomorph(g, type)) 
                {   
                    //TODO: we should try to transform the template into its isomorphic
                    // with highest symmetry, and define the symmetric sets. This
                    // Enhancement would facilitate the creation of symmetric graphs 
                    // from templates generated on the fly.
                    
                    DENOPTIMTemplate t = new DENOPTIMTemplate(type);
                    t.setInnerGraph(g);
                    
                    boolean has3Dgeometry = false;
                    IAtomContainer subIAC = null;
                    if (wholeMol!=null)
                    {
                        try
                        {
                            subIAC = DENOPTIMMoleculeUtils
                                    .extractIACForSubgraph(wholeMol, g, graph);
                            t.setIAtomContainer(subIAC,true);
                            has3Dgeometry = true;
                        } catch (DENOPTIMException e1)
                        {
                            e1.printStackTrace();
                            ArrayList<DENOPTIMGraph> lst = new ArrayList<>();
                            lst.add(graph);
                            lst.add(g);
                            String forDebugFile = "failedExtractIAC_" 
                            + graph.getGraphId() + ".json";
                            try
                            {
                                DenoptimIO.writeGraphsToJSON(new File(forDebugFile),
                                        lst);
                                System.out.println("WARNING: failed to extract "
                                        + "molecular representation of graph. See "
                                        + "file '" + forDebugFile + "'.");
                            } catch (DENOPTIMException e)
                            {
                                System.out.println("WARNING: failed to extract "
                                        + "molecular representation of graph, "
                                        + "and failed to write graph to file.");
                            }
                            
                        }
                    }
    
                    String msg = "Adding new template (Inner Graph id: " 
                            + t.getInnerGraph().getGraphId() + ") to the "
                            + "library of " + type + "s. The template is "
                            + "generated from graph " + graph.getGraphId();
                    Candidate source = graph.getCandidateOwner();
                    if (source != null)
                        msg = msg + " candidate " + source.getName();
                    else
                        msg = msg + ".";
                    DENOPTIMLogger.appLogger.log(Level.INFO, msg);
                    
                    FragmentSpace.appendVertexToLibrary(t, type, library);
                    if (type == BBType.FRAGMENT)
                    {
                        FragmentSpaceUtils.classifyFragment(t,library.size()-1);
                    }
                    
                    String destFileName = type == BBType.FRAGMENT ?
                            FragmentSpaceParameters
                                    .getPathnameToAppendedFragments() :
                                FragmentSpaceParameters
                                         .getPathnameToAppendedScaffolds();
                    try
                    {
                        if (has3Dgeometry)
                        {
                            DenoptimIO.writeSDFFile(destFileName,subIAC,true);
                        } else {
                            DenoptimIO.writeGraphToSDF(new File(destFileName), 
                                    g, true, false);
                        }
                    } catch (DENOPTIMException e)
                    {
                        e.printStackTrace();
                        System.out.println("WARNING: failed to write newly "
                                + "generated " + type + " to file '" 
                                + destFileName + "'.");
                    }
                }
            }
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Checks if a graph is isomorphic to another template's inner graph in its
     * appropriate fragment space library (inferred from BBType).
     * @param graph to check if has an isomorph in the fragment space.
     * @param type specifying which fragment library to check for
     *             isomorphs in.
     * @return true if there is an isomorph template in the library of the
     * specified type.
     */
    public static boolean hasIsomorph(DENOPTIMGraph graph, BBType type) {
        return (type == BBType.SCAFFOLD ?
                FragmentSpace.getScaffoldLibrary() :
                FragmentSpace.getFragmentLibrary())
                .stream()
                .filter(v -> v instanceof DENOPTIMTemplate)
                .map(t -> (DENOPTIMTemplate) t)
                .map(DENOPTIMTemplate::getInnerGraph)
                .anyMatch(graph::isIsomorphicTo);
    }

//------------------------------------------------------------------------------
    
    /**
     * Adds the reference to a ring-closing vertex (RCV) to the quick-access 
     * list of RCVs known in this building block space.
     * @param v the RCV to add to the list.
     */
    public static void registerRCV(DENOPTIMVertex v)
    {
        rcvs.add(v);
    }
  
//------------------------------------------------------------------------------
    
    /**
     * Returns the list of registered ring-closing vertexes (RCVs).
     * @returnthe list of registered ring-closing vertexes (RCVs).
     */
    public static ArrayList<DENOPTIMVertex> getRCVs()
    {
        return rcvs;
    }

//------------------------------------------------------------------------------
    
}
