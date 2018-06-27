package fragspace;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;

import constants.DENOPTIMConstants;
import exception.DENOPTIMException;
import io.DenoptimIO;
import logging.DENOPTIMLogger;
import utils.GenUtils;

import org.openscience.cdk.interfaces.IAtomContainer;


/**
 * Parameters defining the fragment space
 * 
 * @author Marco Foscato
 */

public class FragmentSpaceParameters
{
    /**
     * Flag indicating that at least one FS-parameter has been defined
     */
    protected static boolean fsParamsInUse = false;

    /**
     * Pathname of the file containing the molecular representation of
     * building blocks: scaffolds section - fragments that can be used
     * as seed to grow a new molecule.
     */
    protected static String scaffoldLibFile = "";

    /**
     * PathName of the file containing the molecular representation of
     * building blocks: fragment section - fragments for general use
     */
    protected static String fragmentLibFile = "";

    /**
     * Pathname of the file containing the molecular representation of
     * building blocks: capping group section - fragments with only
     * one attachment point used to saturate unused attachment
     * points on a graph.
     */
    protected static String cappingLibFile = "";

    /**
     * Pathname of the file containing the compatibility matrix, bond
     * order to AP-class relation, and forbidden ends list.
     */
    protected static String compMatrixFile = "";

    /**
     * Pathname of the file containing the RC-compatibility matrix
     */
    protected static String rcCompMatrixFile = "";

    /**
     * Rotatable bonds definition file
     */
    protected static String rotBndsFile = "";

    /**
     * FLag defining use of AP class-based approach
     */
    protected static boolean apClassBasedApproch = false;

    /**
     * Maximum number of heavy (non-hydrogen) atoms accepted
     */
    protected static int maxHeavyAtom = 100;

    /**
     * Maximum number of rotatable bonds accepted
     */
    protected static int maxRotatableBond = 20;

    /**
     * Maximum molecular weight accepted
     */
    protected static double maxMW = 500;

    /**
     * Flag enforcing constitutional symmetry
     */
    protected static boolean enforceSymmetry = false;

    /**
     * Flag for application of selected constitutional symmetry constraints
     */
    protected static boolean symmetryConstraints = false;

    /**
     * List of constitutional symmetry constraints
     */
    protected static HashMap<String, Double> symmConstraintsMap = 
						  new HashMap<String, Double>();

    /**
     * Verbosity level
     */
    protected static int verbosity = 0;

//------------------------------------------------------------------------------

    public static boolean fsParamsInUse()
    {
        return fsParamsInUse;
    }

//------------------------------------------------------------------------------

    public static boolean useAPclassBasedApproach()
    {
        return apClassBasedApproch;
    }

//------------------------------------------------------------------------------

    public static int getMaxHeavyAtom()
    {
        return maxHeavyAtom;
    }

//------------------------------------------------------------------------------

    public static int getMaxRotatableBond()
    {
        return maxRotatableBond;
    }

//------------------------------------------------------------------------------

    public static double getMaxMW()
    {
        return maxMW;
    }

//------------------------------------------------------------------------------
    
    public static boolean enforceSymmetry()
    {
	return enforceSymmetry;
    }

//------------------------------------------------------------------------------

    public static boolean symmetryConstraints()
    {
        return symmetryConstraints;
    }

//------------------------------------------------------------------------------

    public static int getVerbosity()
    {
	return verbosity;
    }

//------------------------------------------------------------------------------

    public static String getRotSpaceDefFile()
    {
        return rotBndsFile;
    }

//------------------------------------------------------------------------------
    
    public static void interpretKeyword(String line) throws DENOPTIMException
    {
        String key = line.trim();
        String value = "";
        if (line.contains("="))
        {
            key = line.substring(0,line.indexOf("=") + 1).trim();
            value = line.substring(line.indexOf("=") + 1).trim();
        }
        try
        {
            interpretKeyword(key,value);
        }
        catch (DENOPTIMException e)
        {
            throw new DENOPTIMException(e.getMessage()+" Check line "+line);
        }
    }

//------------------------------------------------------------------------------

    public static void interpretKeyword(String key, String value)
                                                      throws DENOPTIMException
    {
        fsParamsInUse = true;
        String msg = "";
        switch (key.toUpperCase())
        {
        case "FS-SCAFFOLDLIBFILE=":
            scaffoldLibFile = value;
            break;
        case "FS-FRAGMENTLIBFILE=":
            fragmentLibFile = value;
            break;
        case "FS-CAPPINGFRAGMENTLIBFILE=":
            cappingLibFile = value;
            break;
        case "FS-COMPMATRIXFILE=":
            compMatrixFile = value;
            break;
        case "FS-RCCOMPMATRIXFILE=":
            rcCompMatrixFile = value;
            break;
        case "FS-ROTBONDSDEFFILE=":
            rotBndsFile = value;
            break;
        case "FS-MAXHEAVYATOM=":
            try
            {
                if (value.length() > 0)
                    maxHeavyAtom = Integer.parseInt(value);
            }
            catch (Throwable t)
            {
                msg = "Unable to understand value '" + value + "'";
                throw new DENOPTIMException(msg);
            }
            break;
        case "FS-MAXROTATABLEBOND=":
            try
            {
                if (value.length() > 0)
                    maxRotatableBond = Integer.parseInt(value);
            }
            catch (Throwable t)
            {
                msg = "Unable to understand value '" + value + "'";
                throw new DENOPTIMException(msg);
            }
            break;
        case "FS-MAXMW=":
            try
            {
                if (value.length() > 0)
                    maxMW = Double.parseDouble(value);
            }
            catch (Throwable t)
            {
                msg = "Unable to understand value '" + value + "'";
                throw new DENOPTIMException(msg);
            }
            break;
        case "FS-ENFORCESYMMETRY":
	    enforceSymmetry = true;
	    break;
	case "FS-CONSTRAINSYMMETRY=":
	    symmetryConstraints = true;
	    try
	    {
                if (value.length() > 0)
		{
		    String[] words = value.trim().split("\\s+");
		    if (words.length != 2)
		    {
                        msg = "Keyword " + key + " requires two arguments: " 
                              + "[APClass (String)] [probability (Double)].";
                        throw new DENOPTIMException(msg);
		    }
		    String apClass = words[0];
		    double prob = 0.0;
		    try
		    {
			prob = Double.parseDouble(words[1]);
		    }
		    catch (Throwable t2)
		    {
			msg = "Unable to convert '" + words[1] + "' into a "
			      + "double.";
                        throw new DENOPTIMException(msg);
		    }
                    symmConstraintsMap.put(apClass,prob);
		}
		else
		{
		    msg = "Keyword '" + key + "' requires two arguments: " 
			         + "[APClass (String)] [probability (Double)].";
                    throw new DENOPTIMException(msg);
		}
	    }
	    catch (Throwable t)
	    {
		if (msg.equals(""))
		{
		    msg = "Unable to understand value '" + value + "'";
		}
                throw new DENOPTIMException(msg);
	    }
	    break;
	case "FS-VERBOSITY=":
	    try
	    {
	        verbosity = Integer.parseInt(value);
	    }
	    catch (Throwable t)
	    {
		msg = "Unable to understand value '" + value + "'";
                throw new DENOPTIMException(msg);
	    }
        default:
             msg = "Keyword " + key + " is not a known fragment space-"
                                          + "related keyword. Check input files.";
            throw new DENOPTIMException(msg);
        }
    }

//------------------------------------------------------------------------------

    public static void checkParameters() throws DENOPTIMException
    {
        String msg = "";
        if (!fsParamsInUse)
        {
            return;
        }

        if (scaffoldLibFile.length() == 0)
        {
            msg = "No scaffold library file specified.";
            throw new DENOPTIMException(msg);
        }
        if (!DenoptimIO.checkExists(scaffoldLibFile))
        {
            msg = "Cannot find the scaffold library: " + scaffoldLibFile;
            throw new DENOPTIMException(msg);
        }

        if (fragmentLibFile.length() == 0)
        {
            msg = "No fragment library file specified.";
            throw new DENOPTIMException(msg);
        }
        if (!DenoptimIO.checkExists(fragmentLibFile))
        {
            msg = "Cannot find the fragment library: " +  fragmentLibFile;
            throw new DENOPTIMException(msg);
        }

        if (cappingLibFile.length() > 0)
        {
            if (!DenoptimIO.checkExists(cappingLibFile))
            {
                msg = "Cannot find the library of capping groups: "
                      + cappingLibFile;
                throw new DENOPTIMException(msg);
            }
        }

        if (compMatrixFile.length() > 0)
        {
            if (!DenoptimIO.checkExists(compMatrixFile))
            {
                msg = "Cannot find the compatibility matrix file: " 
                      + compMatrixFile;
                throw new DENOPTIMException(msg);
            }
        }

        if (rcCompMatrixFile.length() > 0)
        {
            if (!DenoptimIO.checkExists(rcCompMatrixFile))
            {
                msg = "Cannot find the ring-closures compatibility matrix "
                      + "file: " + rcCompMatrixFile;
                throw new DENOPTIMException(msg);
            }
        }

        if (rotBndsFile.length()>0 && !DenoptimIO.checkExists(rotBndsFile))
        {
            msg = "Cannot find file with definitions of rotatable bonds: " 
                  + rotBndsFile;
            throw new DENOPTIMException(msg);
        }
    }

//------------------------------------------------------------------------------

    public static void processParameters() throws DENOPTIMException
    {
        // library of scaffolds
        FragmentSpace.setScaffoldLibrary(
                                DenoptimIO.readMoleculeData(scaffoldLibFile));
        Iterator<IAtomContainer> scafI = 
                                 FragmentSpace.getScaffoldLibrary().iterator(); 
        int i = 0;
        while (scafI.hasNext())
        {
            IAtomContainer mol = scafI.next();
            i++;
            Object ap = mol.getProperty(DENOPTIMConstants.APTAG);
            if (ap == null)
            {
                DENOPTIMLogger.appLogger.log(Level.WARNING,
                     "No attachment point information for scaffold {0}", (i));
                scafI.remove();
            }
        }
        if (FragmentSpace.getScaffoldLibrary().isEmpty())
        {
            throw new DENOPTIMException("Scaffold library has no entries.");
        }

        // library of fragments
        FragmentSpace.setFragmentLibrary(
                                DenoptimIO.readMoleculeData(fragmentLibFile));
        Iterator<IAtomContainer> fragI = 
                                 FragmentSpace.getFragmentLibrary().iterator();
        i = 0;
        while (fragI.hasNext())
        {
            IAtomContainer mol = fragI.next();
            i++;
            Object ap = mol.getProperty(DENOPTIMConstants.APTAG);
            if (ap == null)
            {
                DENOPTIMLogger.appLogger.log(Level.WARNING,
                     "No attachment point information for fragment {0}", (i));
                fragI.remove();
            }
        }
        if (FragmentSpace.getFragmentLibrary().isEmpty())
        {
            throw new DENOPTIMException("Fragment library has no entries.");
        }

        // library of capping groups
        if (cappingLibFile.length() > 0)
        {
            FragmentSpace.setCappingLibrary(
                                DenoptimIO.readMoleculeData(cappingLibFile));
            Iterator<IAtomContainer> cappI =
                                 FragmentSpace.getCappingLibrary().iterator(); 
            i = 0;
            while (cappI.hasNext())
            {
                IAtomContainer mol = cappI.next();
                i++;
                Object ap = mol.getProperty(DENOPTIMConstants.APTAG);
                if (ap == null)
                {
                    DENOPTIMLogger.appLogger.log(Level.WARNING,
                   "No attachment point information for capp. grp. {0}", (i));
                    cappI.remove();
                }
            }
            if (FragmentSpace.getCappingLibrary().isEmpty())
            {
                throw new DENOPTIMException("Capping library has no "
                                                               + " entries.");
            }
        }

	// compatibility matrix
        if (compMatrixFile.length() > 0)
        {
            FragmentSpace.setCompatibilityMatrix(
				       new HashMap<String,ArrayList<String>>());
            FragmentSpace.setBondOrderMap(new HashMap<String,Integer>());
            FragmentSpace.setCappingMap(new HashMap<String,String>());
            FragmentSpace.setForbiddenEndList(new ArrayList<String>());
            DenoptimIO.readCompatibilityMatrix(compMatrixFile, 
                                        FragmentSpace.getCompatibilityMatrix(),
                                        FragmentSpace.getBondOrderMap(),
                                        FragmentSpace.getCappingMap(),
                                        FragmentSpace.getForbiddenEndList());
            apClassBasedApproch = true;
        }

	// compatibility matrix for ring closures
        if (rcCompMatrixFile != null && rcCompMatrixFile.length() > 0)
        {
            FragmentSpace.setRCCompatibilityMatrix(
				       new HashMap<String,ArrayList<String>>());
            DenoptimIO.readRCCompatibilityMatrix(rcCompMatrixFile,
                                    FragmentSpace.getRCCompatibilityMatrix());
        }

	// constitutionsl symmetry constraints
	FragmentSpace.setSymmConstraints(symmConstraintsMap);
	
	// grouping of fragments
	FragmentSpace.setFragPoolPerNumAP(
				     new HashMap<Integer,ArrayList<Integer>>());
	if (apClassBasedApproch)
	{
	    FragmentSpace.setFragsApsPerApClass(
			   new HashMap<String,ArrayList<ArrayList<Integer>>>());
	    FragmentSpace.setAPClassesPerFrag(
				      new HashMap<Integer,ArrayList<String>>());
	}
	for (int j=0; j<FragmentSpace.getFragmentLibrary().size(); j++)
	{
	    IAtomContainer frag = FragmentSpace.getFragmentLibrary().get(j);
	    FragmentSpaceUtils.classifyFragment(frag,1,j);
	}


    }

//------------------------------------------------------------------------------

    public static void printParameters()
    {
	if (!fsParamsInUse)
	{
	    return;
	}
        String eol = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder(1024);
        sb.append(" FragmentSpaceParameters ").append(eol);
        for (Field f : FragmentSpaceParameters.class.getDeclaredFields()) 
        {
            try
            {
                sb.append(f.getName()).append(" = ").append(
                            f.get(FragmentSpaceParameters.class)).append(eol);
            }
            catch (Throwable t)
            {
                sb.append("ERROR! Unable to print FragmentSpaceParameters.");
                break;
            }
        }
        DENOPTIMLogger.appLogger.info(sb.toString());
        sb.setLength(0);
    }

//------------------------------------------------------------------------------

}
