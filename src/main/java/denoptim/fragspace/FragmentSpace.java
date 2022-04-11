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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.files.UndetectedFileFormatException;
import denoptim.graph.APClass;
import denoptim.graph.APMapping;
import denoptim.graph.Candidate;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.DGraph;
import denoptim.graph.Template;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;
import denoptim.graph.GraphPattern;
import denoptim.io.DenoptimIO;
import denoptim.utils.MoleculeUtils;
import denoptim.utils.GraphUtils;
import denoptim.utils.Randomizer;

/**
 * Class defining a space of building blocks. The space comprises of the lists 
 * of building blocks and the rules governing the connection of building blocks 
 * to forms a {@link DGraph}.
 * 
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */

public class FragmentSpace
{
    /**
     * Data structure containing the molecular representation of building
     * blocks: scaffolds section - fragments that can be used as seeds to grow a
     * new molecule. WARNING! The objects stores in the library do not have a
     * meaningful value for the two indexes representing the type of building
     * block and the position of the list of all building blocks of that type.
     */
    private ArrayList<Vertex> scaffoldLib = null;

    /**
     * Data structure containing the molecular representation of building
     * blocks: fragment section - fragments for general use. WARNING! The
     * objects stores in the library do not have a meaningful value for the two
     * indexes representing the type of building block and the position of the
     * list of all building blocks of that type.
     */
    private ArrayList<Vertex> fragmentLib = null;

    /**
     * Data structure containing the molecular representation of building
     * blocks: capping group section - fragments with only one attachment point
     * used to saturate unused attachment points on a graph. WARNING! The
     * objects stores in the library do not have a meaningful value for the two
     * indexes representing the type of building block and the position of the
     * list of all building blocks of that type.
     */
    private ArrayList<Vertex> cappingLib = null;

    /**
     * Data structure that stored the true entries of the attachment point
     * classes compatibility matrix
     */
    private HashMap<APClass, ArrayList<APClass>> apClassCompatibilityMatrix;

    /**
     * Store references to the Ring-Closing Vertexes found in the library of 
     * fragments.
     */
    private ArrayList<Vertex> rcvs = new ArrayList<Vertex>();
    
    /**
     * Data structure that stores compatible APclasses for joining APs in
     * ring-closing bonds. Symmetric, purpose specific compatibility matrix.
     */
    private HashMap<APClass, ArrayList<APClass>> rcCompatMap;

    /**
     * Data structure that stores the AP-classes to be used to cap unused APS on
     * the growing molecule.
     */
    private HashMap<APClass, APClass> cappingMap;
    
    /**
     * Data structure that stores AP classes that cannot be held unused
     */
    private Set<APClass> forbiddenEndList;

    /**
     * Clusters of fragments based on the number of APs
     */
    private HashMap<Integer, ArrayList<Integer>> fragPoolPerNumAP;

    /**
     * List of APClasses per each fragment
     */
    private HashMap<Integer, ArrayList<APClass>> apClassesPerFrag;

    /**
     * Clusters of fragments'AP based on AP classes
     */
    private HashMap<APClass, ArrayList<ArrayList<Integer>>> fragsApsPerApClass;
    
    /**
     * APclass-specific constraints to constitutional symmetry
     */
    private HashMap<APClass, Double> symmConstraints;

    /**
     * Flag defining use of AP class-based approach
     */
    private boolean apClassBasedApproch = false;

    /**
     * Flag signaling that this fragment space was built and validated
     */
    private boolean isValid = false;

    /**
     * Settings used to configure this fragment space. Contains the default
     * values unless its value is reassigned.
     */
    private FragmentSpaceParameters settings = null;

//------------------------------------------------------------------------------
    
    /**
     * Creates an empty fragment space, which is marked as <i>invalid</i>. Such 
     * fragment space has very limited functionality, but can be used to 
     * manipulate information related to fragment spaces, such as, handling
     * compatibility rules even without having any defined vertex in it.
     */
    public FragmentSpace()
    {
        fragmentLib = new ArrayList<Vertex>();
        scaffoldLib = new ArrayList<Vertex>();
        cappingLib = new ArrayList<Vertex>();
        settings = new FragmentSpaceParameters(this);
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
     * @throws DENOPTIMException
     */
    public FragmentSpace(FragmentSpaceParameters settings, String scaffFile, 
            String fragFile, String capFile, String cpmFile) 
                    throws DENOPTIMException
    {
        this(settings, scaffFile, fragFile, capFile, cpmFile, "", 
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
     * @param capMap   the capping rules. This data structure is a map of
     *                 APClass-to-cap (keys) to APClass-of-capping-group
     *                 (values).
     * @param forbEnds the list of forbidden ends, i.e., APClasses that cannot
     *                 be left unused neither capped.
     * @param rcCpMap  the APClass compatibility matrix for ring closures.
     * @throws DENOPTIMException
     */
    public FragmentSpace(FragmentSpaceParameters settings,
            ArrayList<Vertex> scaffLib,
            ArrayList<Vertex> fragLib,
            ArrayList<Vertex> cappLib,
            HashMap<APClass, ArrayList<APClass>> cpMap,
            HashMap<APClass, APClass> capMap,
            HashSet<APClass> forbEnds,
            HashMap<APClass, ArrayList<APClass>> rcCpMap)
            throws DENOPTIMException
    {
        define(settings, scaffLib, fragLib, cappLib, cpMap, capMap, forbEnds, 
                rcCpMap, null);
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
     * @param capMap   the capping rules. This data structure is a map of
     *                 APClass-to-cap (keys) to APClass-of-capping-group
     *                 (values).
     * @param forbEnds the list of forbidden ends, i.e., APClasses that cannot
     *                 be left unused neither capped.
     * @param rcCpMap  the APClass compatibility matrix for ring closures.
     * @param symCntrMap map of symmetry probability constraints.
     * @throws DENOPTIMException
     */
    public FragmentSpace(FragmentSpaceParameters settings,
            ArrayList<Vertex> scaffLib,
            ArrayList<Vertex> fragLib,
            ArrayList<Vertex> cappLib,
            HashMap<APClass, ArrayList<APClass>> cpMap,
            HashMap<APClass, APClass> capMap,
            HashSet<APClass> forbEnds,
            HashMap<APClass, ArrayList<APClass>> rcCpMap,
            HashMap<APClass, Double> symCntrMap)
            throws DENOPTIMException
    {
        define(settings, scaffLib, fragLib, cappLib, cpMap, capMap, forbEnds, 
                rcCpMap, symCntrMap);
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
     * @param cpmFile    pathname to the compatibility matrix, capping, and 
     * forbidden ends rules.
     * @param rcpmFile   the APClass compatibility matrix for ring closures.
     * @param symCntrMap map of symmetry probability constraints.
     * @throws DENOPTIMException
     */
    public FragmentSpace(FragmentSpaceParameters settings, String scaffFile, 
            String fragFile, String capFile, String cpmFile, String rcpmFile,
            HashMap<APClass, Double> symCntrMap) throws DENOPTIMException
    {   
        HashMap<APClass, ArrayList<APClass>> cpMap = 
                new HashMap<APClass, ArrayList<APClass>>();
        HashMap<APClass, APClass> capMap = new HashMap<APClass, APClass>();
        HashSet<APClass> forbEnds = new HashSet<APClass>();
        if (cpmFile.length() > 0)
        {
            DenoptimIO.readCompatibilityMatrix(cpmFile, cpMap, capMap,
                    forbEnds);
        }

        HashMap<APClass, ArrayList<APClass>> rcCpMap = 
                new HashMap<APClass, ArrayList<APClass>>();
        if (rcpmFile != null && rcpmFile.length() > 0)
        {
            DenoptimIO.readRCCompatibilityMatrix(rcpmFile, rcCpMap);
        }
        
        ArrayList<Vertex> cappLib = new ArrayList<Vertex>();
        if (capFile.length() > 0)
        {
            try
            {
                cappLib = DenoptimIO.readVertexes(new File(capFile),
                        BBType.CAP);
                for (int i=0; i<cappLib.size(); i++)
                {
                    cappLib.get(i).setBuildingBlockId(i);
                }
            } catch (IllegalArgumentException | UndetectedFileFormatException
                    | IOException | DENOPTIMException e)
            {
                throw new DENOPTIMException("Cound not read library of capping "
                        + "groups from file '" + capFile + "'.", e);
            }
        }
        
        ArrayList<Vertex> fragLib = new ArrayList<Vertex>();
        try
        {
            fragLib = DenoptimIO.readVertexes(new File(fragFile),
                    BBType.FRAGMENT);
            for (int i=0; i<fragLib.size(); i++)
            {
                fragLib.get(i).setBuildingBlockId(i);
            }
        } catch (IllegalArgumentException | UndetectedFileFormatException
                | IOException | DENOPTIMException e)
        {
            throw new DENOPTIMException("Cound not read library of fragments "
                    + "from file '" + fragFile + "'.", e);
        }
        
        ArrayList<Vertex> scaffLib = new ArrayList<Vertex>();
        try
        {
            scaffLib = DenoptimIO.readVertexes(new File(scaffFile),
                    BBType.SCAFFOLD);
            for (int i=0; i<scaffLib.size(); i++)
            {
                scaffLib.get(i).setBuildingBlockId(i);
            }
        } catch (IllegalArgumentException | UndetectedFileFormatException
                | IOException | DENOPTIMException e)
        {
            throw new DENOPTIMException("Cound not read library of scaffolds "
                    + "from file '" + fragFile + "'.", e);
        }

        define(settings, scaffLib, fragLib, cappLib, cpMap, capMap, forbEnds, 
                rcCpMap, symCntrMap);
    }
    
//------------------------------------------------------------------------------

    /**
     * Define all components of this fragment space.
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
     * @param capMap   the capping rules. This data structure is a map of
     *                 APClass-to-cap (keys) to APClass-of-capping-group
     *                 (values).
     * @param forbEnds the list of forbidden ends, i.e., APClasses that cannot
     *                 be left unused neither capped.
     * @param rcCpMap  the APClass compatibility matrix for ring closures.
     * @param symCntrMap map of symmetry probability constraints.
     * @throws DENOPTIMException
     */
    private void define(FragmentSpaceParameters settings,
            ArrayList<Vertex> scaffLib,
            ArrayList<Vertex> fragLib,
            ArrayList<Vertex> cappLib,
            HashMap<APClass, ArrayList<APClass>> cpMap,
            HashMap<APClass, APClass> capMap,
            HashSet<APClass> forbEnds,
            HashMap<APClass, ArrayList<APClass>> rcCpMap,
            HashMap<APClass, Double> symCntrMap)
            throws DENOPTIMException
    {
        this.settings = settings;
        settings.setFragmentSpace(this);
        
        setScaffoldLibrary(scaffLib);
        setFragmentLibrary(fragLib);
        setCappingLibrary(cappLib);
        setCompatibilityMatrix(cpMap);
        apClassBasedApproch = cpMap.size()>0;
        setCappingMap(capMap);
        setForbiddenEndList(forbEnds);
        setRCCompatibilityMatrix(rcCpMap);
        setSymmConstraints(symCntrMap);

        groupAndClassifyFragments(apClassBasedApproch);

        isValid = true;
    }

//------------------------------------------------------------------------------

    /**
     * Checks for valid definition of this fragment space.
     * @return <code>true</code> if this fragment space has been defined
     */
    public boolean isDefined()
    {
        return isValid;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the program-specific randomizer that is associated with this 
     * program-specific fragment space.
     * @return the program-specific randomizer.
     */
    public Randomizer getRandomizer()
    {
        return settings.getRandomizer();
    }

//------------------------------------------------------------------------------
    
    /**
     * Set the fragment space to behave according to APClass-based approach.
     */
    public void setAPclassBasedApproach(boolean useAPC)
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
    public boolean useAPclassBasedApproach()
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

    public APClass getAPClassForFragment(IdFragmentAndAP apId)
    {
        APClass cls = null;
        try
        {
            Vertex frg = this.getVertexFromLibrary(
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

    public Vertex getVertexFromLibrary(
            Vertex.BBType bbType, int bbIdx) throws DENOPTIMException
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
                
            default:
                throw new DENOPTIMException("Cannot find building block of "
                        + "type '" + bbType + "' in the fragment space.");
        }

        Vertex originalVrtx = null;
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
                    settings.getLogger().log(Level.SEVERE, msg);
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
                    settings.getLogger().log(Level.SEVERE, msg);
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
                    settings.getLogger().log(Level.SEVERE, msg);
                    throw new DENOPTIMException(msg);
                }
                break;
                
            case UNDEFINED:
                msg = "Attempting to take UNDEFINED type of building block from "
                        + "fragment library.";
                settings.getLogger().log(Level.WARNING, msg);
                if (bbIdx < fragmentLib.size())
                {
                    originalVrtx = fragmentLib.get(bbIdx);
                }
                else
                {
                    msg = "Mismatch between fragment bbIdx (" + bbIdx 
                            + ") and size of the library (" + fragmentLib.size()
                            + "). FragType: " + bbType;
                    settings.getLogger().log(Level.SEVERE, msg);
                    throw new DENOPTIMException(msg);
                }
                break;
    
            default:
                msg = "Unknown type of fragment '" + bbType + "'.";
                settings.getLogger().log(Level.SEVERE, msg);
                throw new DENOPTIMException(msg);
        }
        Vertex clone = originalVrtx.clone();
        
        clone.setVertexId(GraphUtils.getUniqueVertexIndex());

        clone.setBuildingBlockId(bbIdx);
        if (originalVrtx.getBuildingBlockId() != bbIdx)
        {
            settings.getLogger().log(Level.WARNING, "Mismatch between building "
                    + "block ID ("
                    + originalVrtx.getBuildingBlockId() + ") and position in "
                    + "the list of building blocks (" + bbIdx + ") for type "
                    + bbType + ".");
        }
        return clone;
    }
    
//------------------------------------------------------------------------------

    /**
     * Select a compatible capping group for the given APClass.
     * @param rcnCap the class of the attachment point to be capped.
     * @return the index of capping group
     */

    public int getCappingFragment(APClass rcnCap)
    {
        if (rcnCap == null)
            return -1;

        ArrayList<Integer> reacFrags = getCompatibleCappingFragments(rcnCap);

        int fapidx = -1;
        if (reacFrags.size() > 0)
        {
            fapidx = settings.getRandomizer().randomlyChooseOne(reacFrags);
        }

        return fapidx;
    }

//------------------------------------------------------------------------------

    /**
     * Retrieve a list of compatible capping groups
     * @param cmpReac
     * @return a list of compatible capping groups
     */

    public ArrayList<Integer> getCompatibleCappingFragments(
            APClass cmpReac)
    {
        ArrayList<Integer> lstFragIdx = new ArrayList<>();
        for (int i=0; i<cappingLib.size(); i++)
        {
                Vertex mol = getCappingLibrary().get(i);
            ArrayList<APClass> lstRcn = mol.getAllAPClasses();
            if (lstRcn.contains(cmpReac))
                lstFragIdx.add(i);
        }
        return lstFragIdx;
    }
    
//------------------------------------------------------------------------------

    /**
     * Randomly select a scaffold and return a fully configured clone of it
     * @return a scaffold-type building block ready to be used in a new graph.
     * @throws DENOPTIMException 
     */
    public Vertex makeRandomScaffold()
    {
        int chosenIdx = settings.getRandomizer().nextInt(scaffoldLib.size());
        Vertex scaffold = null;
        try
        {
            scaffold = Vertex.newVertexFromLibrary(
                    GraphUtils.getUniqueVertexIndex(),chosenIdx, 
                    BBType.SCAFFOLD, this);
        } catch (DENOPTIMException e)
        {
            //This cannot happen!
        }
        return scaffold;
    }

//------------------------------------------------------------------------------

    public ArrayList<Vertex> getScaffoldLibrary()
    {
        return scaffoldLib;
    }

//------------------------------------------------------------------------------

    public ArrayList<Vertex> getFragmentLibrary()
    {
        return fragmentLib;
    }

//------------------------------------------------------------------------------

    public ArrayList<Vertex> getCappingLibrary()
    {
        return cappingLib;
    }

//------------------------------------------------------------------------------

    /**
     * @param capApCls the APClass of the attachment point on the capping group
     * @return all the capping groups which have the given APclass
     */

    public ArrayList<Integer> getCappingGroupsWithAPClass(
            APClass capApCls)
    {
        ArrayList<Integer> selected = new ArrayList<>();
        for (int i = 0; i < cappingLib.size(); i++)
        {
            APClass apc = null;
            try
            {
                apc = getVertexFromLibrary(Vertex.BBType.CAP, i)
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

    public void importCompatibilityMatrixFromFile(String inFile)
            throws DENOPTIMException
    {
        setCompatibilityMatrix(new HashMap<APClass, ArrayList<APClass>>());
        setCappingMap(new HashMap<APClass, APClass>());
        setForbiddenEndList(new HashSet<APClass>());
        DenoptimIO.readCompatibilityMatrix(inFile, apClassCompatibilityMatrix,
                cappingMap, forbiddenEndList);
    }

//------------------------------------------------------------------------------

    /**
     * Load info for ring closures compatibilities from a compatibility matrix
     * file.
     * 
     * @param inFile the pathname of the RC-compatibility matrix file
     */

    public void importRCCompatibilityMatrixFromFile(String inFile)
            throws DENOPTIMException
    {
        setRCCompatibilityMatrix(new HashMap<APClass, ArrayList<APClass>>());
        DenoptimIO.readRCCompatibilityMatrix(inFile, rcCompatMap);
    }

//------------------------------------------------------------------------------

    public HashMap<APClass, ArrayList<APClass>> getCompatibilityMatrix()
    {
        return apClassCompatibilityMatrix;
    }

//------------------------------------------------------------------------------

    /**
     * Returns a list of APClasses compatible with the given APClass. The
     * compatibility among classes is defined by the compatibility matrix
     * @param apc
     * @return the list of compatible APClasses. Can be empty but not null.
     */
    public ArrayList<APClass> getCompatibleAPClasses(APClass apc)
    {   
        if (apClassCompatibilityMatrix!= null && apClassCompatibilityMatrix.containsKey(apc))
        {
            return apClassCompatibilityMatrix.get(apc);
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

    public HashMap<APClass, ArrayList<APClass>> getRCCompatibilityMatrix()
    {
        return rcCompatMap;
    }

//------------------------------------------------------------------------------

    public HashMap<APClass, APClass> getCappingMap()
    {
        return cappingMap;
    }

//------------------------------------------------------------------------------

    /**
     * @param srcApClass the attachment point class of the attachment point to
     *                   be capped
     * @return the APClass of the capping group or null
     */

    public APClass getAPClassOfCappingVertex(APClass srcApClass)
    {
        return cappingMap.get(srcApClass);
    }

//------------------------------------------------------------------------------

    public Set<APClass> getForbiddenEndList()
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

    public Set<APClass> getAllAPClassesFromCPMap()
    {
        return apClassCompatibilityMatrix.keySet();
    }

//------------------------------------------------------------------------------

    public HashMap<Integer, ArrayList<Integer>> getMapOfFragsPerNumAps()
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

    public ArrayList<Integer> getFragsWithNumAps(int nAps)
    {
        ArrayList<Integer> lst = new ArrayList<>();
        if (fragPoolPerNumAP.containsKey(nAps))
        {
            lst = fragPoolPerNumAP.get(nAps);
        }
        return lst;
    }

//------------------------------------------------------------------------------

    public HashMap<Integer, ArrayList<APClass>> getMapAPClassesPerFragment()
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

    public ArrayList<APClass> getAPClassesPerFragment(int fragId)
    {
        return apClassesPerFrag.get(fragId);
    }

//------------------------------------------------------------------------------

    public HashMap<APClass, ArrayList<ArrayList<Integer>>> getMapFragsAPsPerAPClass()
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

    public ArrayList<IdFragmentAndAP> getFragsWithAPClass(APClass apc)
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

    public ArrayList<Vertex> getVerticesWithAPClass(APClass apc)
    {
        ArrayList<Vertex> lst = new ArrayList<Vertex>();
        
        if (fragsApsPerApClass.containsKey(apc))
        {
            for (ArrayList<Integer> idxs : fragsApsPerApClass.get(apc))
            {
                Vertex v = fragmentLib.get(idxs.get(0));
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
    public ArrayList<Vertex> getFragmentsCompatibleWithTheseAPs(
            ArrayList<IdFragmentAndAP> srcAPs)
    {
        // First we get all possible APs on any fragment
        ArrayList<IdFragmentAndAP> compatFragAps = 
                getFragAPsCompatibleWithTheseAPs(srcAPs);

        // then keep unique fragment identifiers, and store unique
        Set<Integer> compatFragIds = new HashSet<Integer>();
        for (IdFragmentAndAP apId : compatFragAps)
        {
            compatFragIds.add(apId.getVertexMolId());
        }

        // Then we pack-up the selected list of fragments
        ArrayList<Vertex> compatFrags = new ArrayList<Vertex>();
        for (Integer fid : compatFragIds)
        {
            try {
                compatFrags.add(getVertexFromLibrary(BBType.FRAGMENT, fid));
            } catch (DENOPTIMException e) {
                settings.getLogger().log(Level.WARNING, "Exception while trying "
                        + "to get fragment '" + fid + "'!");
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
    public ArrayList<AttachmentPoint> getAPsCompatibleWithThese(
                    ArrayList<AttachmentPoint> srcAPs)
    {
        ArrayList<AttachmentPoint> compAps = 
                new ArrayList<AttachmentPoint>();
        boolean first = true;
        for (AttachmentPoint ap : srcAPs)
        { 
            ArrayList<AttachmentPoint> compForOne = 
                    getAPsCompatibleWithClass(ap.getAPClass());

            if (first)
            {
                compAps.addAll(compForOne);
                first = false;
                continue;
            }
            
            ArrayList<AttachmentPoint> toKeep = 
                    new ArrayList<AttachmentPoint>();
            for (AttachmentPoint candAp : compAps)
            {
                for (AttachmentPoint newCand : compForOne)
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
    public ArrayList<IdFragmentAndAP> getFragAPsCompatibleWithTheseAPs(
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
    
    public ArrayList<AttachmentPoint> getAPsCompatibleWithClass(
                    APClass aPC1)
    {
        ArrayList<AttachmentPoint> compatAps = 
                new ArrayList<AttachmentPoint>();
        
        // Take the compatible AP classes
        ArrayList<APClass> compatApClasses = getCompatibleAPClasses(aPC1);
        
        // Find all APs with a compatible class
        if (compatApClasses != null)
        {
            for (APClass klass : compatApClasses)
            {
                ArrayList<Vertex> vrtxs = getVerticesWithAPClass(klass);
                for (Vertex v : vrtxs)
                {
                    for (AttachmentPoint ap : v.getAttachmentPoints())
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
            settings.getLogger().log(Level.WARNING,"No compatible "
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

    public ArrayList<IdFragmentAndAP> getFragAPsCompatibleWithClass(
            APClass aPC1)
    {
        ArrayList<IdFragmentAndAP> compatFragAps = 
                new ArrayList<IdFragmentAndAP>();

        // Take the compatible AP classes
        ArrayList<APClass> compatApClasses = getCompatibleAPClasses(aPC1);

        // Find all APs with any compatible class
        if (compatApClasses != null)
        {
            for (APClass compClass : compatApClasses)
            {
                compatFragAps.addAll(getFragsWithAPClass(compClass));
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

    public boolean imposeSymmetryOnAPsOfClass(APClass apClass)
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
            if (!settings.enforceSymmetry())
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

    public boolean hasSymmetryConstrain(APClass apClass)
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

    public double getSymmetryConstrain(APClass apClass)
    {
        return symmConstraints.get(apClass);
    }

//------------------------------------------------------------------------------

    public void setScaffoldLibrary(ArrayList<Vertex> lib)
    {
        scaffoldLib = new ArrayList<Vertex>();
        appendVerticesToLibrary(lib, BBType.SCAFFOLD, scaffoldLib);
    }

    public void setFragmentLibrary(ArrayList<Vertex> lib)
    {
        fragmentLib = new ArrayList<Vertex>();
        appendVerticesToLibrary(lib, BBType.FRAGMENT, fragmentLib);
    }

//------------------------------------------------------------------------------

    public void setCappingLibrary(ArrayList<Vertex> lib)
    {
        cappingLib = new ArrayList<Vertex>();
        appendVerticesToLibrary(lib, BBType.CAP, cappingLib);
    }

//------------------------------------------------------------------------------

    public void setCompatibilityMatrix(HashMap<APClass, ArrayList<APClass>> map)
    {
        apClassCompatibilityMatrix = map;
    }

//------------------------------------------------------------------------------

    public void setRCCompatibilityMatrix(HashMap<APClass, 
            ArrayList<APClass>> map)
    {
        rcCompatMap = map;
    }

//------------------------------------------------------------------------------

    public void setCappingMap(HashMap<APClass, APClass> map)
    {
        cappingMap = map;
    }

//------------------------------------------------------------------------------

    public void setForbiddenEndList(Set<APClass> lst)
    {
        forbiddenEndList = lst;
    }

//------------------------------------------------------------------------------

    public void setFragPoolPerNumAP(
            HashMap<Integer, ArrayList<Integer>> map)
    {
        fragPoolPerNumAP = map;
    }

//------------------------------------------------------------------------------

    public void setFragsApsPerApClass(HashMap<APClass, 
            ArrayList<ArrayList<Integer>>> map)
    {
        fragsApsPerApClass = map;
    }

//------------------------------------------------------------------------------

    public void setAPClassesPerFrag(
            HashMap<Integer, ArrayList<APClass>> map)
    {
        apClassesPerFrag = map;
    }

//------------------------------------------------------------------------------

    public void setSymmConstraints(HashMap<APClass, Double> map)
    {
        symmConstraints = map;
    }

//------------------------------------------------------------------------------

    /**
     * Clears all settings of this fragment space. All fields changed to
     * <code>null</code>.
     */
    public void clearAll()
    {
        scaffoldLib = null;
        fragmentLib = null;
        cappingLib = null;
        apClassCompatibilityMatrix = null;
        rcCompatMap = null;
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
     * Takes a list of vertices and add them to a given library. Each vertex
     * is assigned the building block type and ID. 
     * 
     * @param list of vertices to import.
     * @param bbt the type of building block the vertices should be set to.
     * @param library where to import the vertices to.
     */
    
    public void appendVerticesToLibrary(ArrayList<Vertex> list, 
            Vertex.BBType bbt, ArrayList<Vertex> library)
    {
        for (Vertex v : list)
        {
            appendVertexToLibrary(v,bbt,library);
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Takes a vertex and add it to a given library. Each vertex
     * is assigned the building block type and ID. 
     * 
     * @param v vertex to import.
     * @param bbt the type of building block the vertex should be set to.
     * @param library where to import the vertex to.
     */
    
    public void appendVertexToLibrary(Vertex v, 
            Vertex.BBType bbt, ArrayList<Vertex> library)
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
     * in the library. See {@link FragmentSpace#addFusedRingsToFragmentLibrary(DGraph, boolean, boolean, IAtomContainer)}
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
    
    public void addFusedRingsToFragmentLibrary(DGraph graph) 
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
    
    public void addFusedRingsToFragmentLibrary(DGraph graph,
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
    
    public void addFusedRingsToFragmentLibrary(DGraph graph,
            boolean addIfScaffold, boolean addIfFragment, 
            IAtomContainer wholeMol) 
    {
        List<DGraph> subgraphs = null;
        try
        {   
            subgraphs = graph.extractPattern(GraphPattern.RING);
        } catch (DENOPTIMException e1)
        {
            settings.getLogger().log(Level.WARNING,"Failed to extract "
                    + "fused ring patters.");
            e1.printStackTrace();
        }

        for (DGraph g : subgraphs) 
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

            ArrayList<Vertex> library = type == BBType.FRAGMENT ?
                    fragmentLib : scaffoldLib;
            
            synchronized (library)
            {
                if (!hasIsomorph(g, type)) 
                {   
                    //TODO: try to transform the template into its isomorphic
                    // with highest symmetry, and define the symmetric sets. 
                    // Such enhancement would facilitate the creation of 
                    // ymmetric graphs from templates generated on the fly.
                    
                    Template t = new Template(type);
                    t.setInnerGraph(g);
                    
                    boolean has3Dgeometry = false;
                    IAtomContainer subIAC = null;
                    if (wholeMol!=null)
                    {
                        try
                        {
                            subIAC = MoleculeUtils.extractIACForSubgraph(
                                    wholeMol, g, graph, settings.getLogger(),
                                    settings.getRandomizer());
                            t.setIAtomContainer(subIAC,true);
                            has3Dgeometry = true;
                        } catch (DENOPTIMException e1)
                        {
                            e1.printStackTrace();
                            ArrayList<DGraph> lst = new ArrayList<>();
                            lst.add(graph);
                            lst.add(g);
                            String forDebugFile = "failedExtractIAC_" 
                            + graph.getGraphId() + ".json";
                            try
                            {
                                DenoptimIO.writeGraphsToJSON(
                                        new File(forDebugFile), lst);
                                settings.getLogger().log(Level.WARNING, 
                                        "WARNING: failed to extract "
                                        + "molecular representation of graph. "
                                        + "See file '" + forDebugFile + "'.");
                            } catch (DENOPTIMException e)
                            {
                                settings.getLogger().log(Level.WARNING,
                                        "WARNING: failed to extract "
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
                    settings.getLogger().log(Level.INFO, msg);
                    
                    appendVertexToLibrary(t, type, library);
                    if (type == BBType.FRAGMENT)
                    {
                        classifyFragment(t,library.size()-1);
                    }
                    
                    String destFileName = type == BBType.FRAGMENT ?
                            settings.getPathnameToAppendedFragments() :
                                settings.getPathnameToAppendedScaffolds();
                    try
                    {
                        if (has3Dgeometry)
                        {
                            DenoptimIO.writeSDFFile(destFileName,subIAC,true);
                        } else {
                            DenoptimIO.writeGraphToSDF(new File(destFileName), 
                                    g, true, false, settings.getLogger(),
                                    settings.getRandomizer());
                        }
                    } catch (DENOPTIMException e)
                    {
                        e.printStackTrace();
                        settings.getLogger().log(Level.WARNING, "WARNING: "
                                + "failed to write newly "
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
    public boolean hasIsomorph(DGraph graph, BBType type) {
        return (type == BBType.SCAFFOLD ? scaffoldLib : fragmentLib)
                .stream()
                .filter(v -> v instanceof Template)
                .map(t -> (Template) t)
                .map(Template::getInnerGraph)
                .anyMatch(graph::isIsomorphicTo);
    }

//------------------------------------------------------------------------------
    
    /**
     * Adds the reference to a ring-closing vertex (RCV) to the quick-access 
     * list of RCVs known in this building block space.
     * @param v the RCV to add to the list.
     */
    public void registerRCV(Vertex v)
    {
        rcvs.add(v);
    }
  
//------------------------------------------------------------------------------
    
    /**
     * Returns the list of registered ring-closing vertexes (RCVs).
     * @return the list of registered ring-closing vertexes (RCVs).
     */
    public ArrayList<Vertex> getRCVs()
    {
        return rcvs;
    }
    
//------------------------------------------------------------------------------

    /**
     * Given two lists of APs this method maps the APClass-compatibilities 
     * from between the two lists considering the APs in the first list the 
     * for the role of source AP in the hypothetical edge.
     * @param listA list of candidate source APs
     * @param listB list of candidate target APs
     * @param maxCombinations a maximum limit; if reached we are happy we give
     * up finding more combination.
     * @return 
     */
    public List<APMapping> mapAPClassCompatibilities(
            List<AttachmentPoint> listA, 
            List<AttachmentPoint> listB, int maxCombinations)
    {
        Map<AttachmentPoint,List<AttachmentPoint>> apCompatilities =
                new HashMap<AttachmentPoint,List<AttachmentPoint>>();
        
        for (AttachmentPoint apA : listA)
        {
            for (AttachmentPoint apB : listB)
            {  
                boolean compatible = false;
                if (useAPclassBasedApproach())
                {   
                    if (apA.getAPClass().isCPMapCompatibleWith(apB.getAPClass(),
                            this))
                    {
                        compatible = true;
                    }
                } else {
                    compatible = true;
                }
                if (compatible)
                {
                    if (apCompatilities.containsKey(apA))
                    {
                        apCompatilities.get(apA).add(apB);
                    } else {
                        List<AttachmentPoint> lst = 
                                new ArrayList<AttachmentPoint>();
                        lst.add(apB);
                        apCompatilities.put(apA,lst);
                    }
                }
            }
        }
        
        // This is used only to keep a sorted list of the map keys
        List<AttachmentPoint> keys = 
                new ArrayList<AttachmentPoint>(
                        apCompatilities.keySet());
        
        // Get all possible combinations of compatible AP pairs
        List<APMapping> apMappings = new ArrayList<APMapping>();
        if (keys.size() > 0)
        {
            int currentKey = 0;
            APMapping currentMapping = new APMapping();
            Boolean stopped = FragmentSpaceUtils.recursiveCombiner(keys, currentKey, 
                    apCompatilities, currentMapping, apMappings, true, 
                    maxCombinations);
        }
        
        return apMappings;
    }

//------------------------------------------------------------------------------
    
    /**
     * Classify a fragment in terms of the number of APs and possibly their 
     * type (AP-Class).
     * @param frg the building block to classify
     * @param id the index of the fragment in the library
     * @throws DENOPTIMException
     */
    
    public void classifyFragment(Vertex frg,int fragId)
    {   
    	// Classify according to number of APs
        int nAps = frg.getFreeAPCount();
    	if (nAps != 0)
    	{
            if (getMapOfFragsPerNumAps().containsKey(nAps))
            {
                getFragsWithNumAps(nAps).add(fragId);
            }
            else
            {
                ArrayList<Integer> lst = new ArrayList<>();
                lst.add(fragId);
                getMapOfFragsPerNumAps().put(nAps,lst);
            }
    	}
    
    	if (useAPclassBasedApproach())
    	{
    	    // Collect classes per fragment
    	    ArrayList<APClass> lstAPC = frg.getAllAPClasses();
            getMapAPClassesPerFragment().put(fragId,lstAPC);
            
    	    // Classify according to AP-Classes
            ArrayList<AttachmentPoint> lstAPs = 
                    frg.getAttachmentPoints();
            
    	    for (int j=0; j<lstAPs.size(); j++)
    	    {
    	        AttachmentPoint ap = lstAPs.get(j);
    			ArrayList<Integer> apId = new ArrayList<Integer>();
    			apId.add(fragId);
    			apId.add(j);
    			APClass cls = ap.getAPClass();
    			
    			if (!ap.isAvailable())
    			{
    			    continue;
    			}
    			
    		    if (getMapFragsAPsPerAPClass().containsKey(cls))
    			{
    			    getMapFragsAPsPerAPClass().get(cls).add(apId);
    			}
    			else
    			{
    			    ArrayList<ArrayList<Integer>> outLst = 
    						    new ArrayList<ArrayList<Integer>>();
    			    outLst.add(apId);
    			    getMapFragsAPsPerAPClass().put(cls,outLst);
    			}
    	    }
    	    
    	    if (frg.isRCV())
    	        registerRCV(frg);
    	}
    }


//------------------------------------------------------------------------------

    /**
     * Performs grouping and classification operations on the library of
     * building blocks of {@link BBType#FRAGMENT}.
     * @param apClassBasedApproch <code>true</code> if you are using class based
     * approach
     */
    public void groupAndClassifyFragments(boolean apClassBasedApproch)
            throws DENOPTIMException
    {	
    	setFragPoolPerNumAP(new HashMap<Integer,ArrayList<Integer>>());
    	if (apClassBasedApproch)
    	{
    	    setFragsApsPerApClass(
    	            new HashMap<APClass,ArrayList<ArrayList<Integer>>>());
    	    setAPClassesPerFrag(new HashMap<Integer,ArrayList<APClass>>());
    	}
    	for (int j=0; j<getFragmentLibrary().size(); j++)
    	{
    		Vertex frag = getFragmentLibrary().get(j);
    	    classifyFragment(frag,j);
    	}
    }

//------------------------------------------------------------------------------
    
}
