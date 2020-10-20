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

package denoptim.fragspace;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.molecule.DENOPTIMEdge.BondType;
import denoptim.molecule.DENOPTIMFragment;
import denoptim.molecule.DENOPTIMFragment.BBType;
import denoptim.molecule.DENOPTIMVertex;


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
     * Flag defining use of AP class-based approach
     */
    private static boolean apClassBasedApproch = false;

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
     * Flag signalling the use of graph templates
     */
    protected static boolean useTemplates = false;
    
    /**
     * TMP flag selecting type of test template
     */
    protected static boolean useCyclicTemplate = false;

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
    
    //TODO-V3 tmp code
    public static boolean hasTemplates()
    {
        return useTemplates;
    }
    //TODO-V3 tmp code
    public static boolean useCyclicTemplate()
    {
        return useCyclicTemplate;
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
            
        //NB: this is supposed to be without "=" sign because it's a parameter
        // without value
        case "FS-ENFORCESYMMETRY":
    	    enforceSymmetry = true;
    	    break;
    	    
    	//TODO-V3: this is temporary stuff needed to test templates
        case "FS-USETEMPLATES=":
            useTemplates = true;
            break;
        case "FS-USECYCLICTEMPLATE=":
            useTemplates = true;
            useCyclicTemplate = true;
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
    	    break;
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

    /**
     * Read the information collected in the parameters stored in this class
     * and create the fragment space accordingly.
     * @throws DENOPTIMException
     */
    public static void processParameters() throws DENOPTIMException
    {
        FragmentSpace.defineFragmentSpace(scaffoldLibFile, fragmentLibFile, 
                cappingLibFile, compMatrixFile, rcCompMatrixFile, 
                symmConstraintsMap); 
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
