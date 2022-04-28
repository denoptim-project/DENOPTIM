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

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;

import denoptim.exception.DENOPTIMException;
import denoptim.files.FileUtils;
import denoptim.graph.APClass;
import denoptim.programs.RunTimeParameters;


/**
 * Parameters defining the fragment space
 * 
 * @author Marco Foscato
 */

public class FragmentSpaceParameters extends RunTimeParameters
{
    /**
     * Pathname of the file containing the molecular representation of
     * building blocks: scaffolds section - fragments that can be used
     * as seed to grow a new molecule.
     */
    protected String scaffoldLibFile = "";

    /**
     * PathName of the file containing the molecular representation of
     * building blocks: fragment section - fragments for general use
     */
    protected String fragmentLibFile = "";

    /**
     * Pathname of the file containing the molecular representation of
     * building blocks: capping group section - fragments with only
     * one attachment point used to saturate unused attachment
     * points on a graph.
     */
    protected String cappingLibFile = "";

    /**
     * Pathname of the file containing the compatibility matrix, bond
     * order to AP-class relation, and forbidden ends list.
     */
    protected String compMatrixFile = "";

    /**
     * Pathname of the file containing the RC-compatibility matrix
     */
    protected String rcCompMatrixFile = "";

    /**
     * Rotatable bonds definition file
     */
    protected String rotBndsFile = "";

    /**
     * Flag defining use of AP class-based approach
     */
    private boolean apClassBasedApproch = false;

    /**
     * Maximum number of heavy (non-hydrogen) atoms accepted
     */
    protected int maxHeavyAtom = 100;

    /**
     * Maximum number of rotatable bonds accepted
     */
    protected int maxRotatableBond = 20;

    /**
     * Maximum molecular weight accepted
     */
    protected double maxMW = 500;

    /**
     * Flag enforcing constitutional symmetry
     */
    protected boolean enforceSymmetry = false;

    /**
     * Flag for application of selected constitutional symmetry constraints
     */
    protected boolean symmetryConstraints = false;

    /**
     * List of constitutional symmetry constraints.
     */
    private HashMap<APClass, Double> symmConstraintsMap = 
            new HashMap<APClass, Double>();
    
    private FragmentSpace buildingBlocksSpace = null;
    
//------------------------------------------------------------------------------

    /**
     * Constructor
     */
    private FragmentSpaceParameters(ParametersType paramType)
    {
        super(paramType);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructor
     */
    public FragmentSpaceParameters()
    {
        this(ParametersType.FS_PARAMS);
    }

//------------------------------------------------------------------------------

    /**
     * Constructor of a default set of parameters coupled with a given fragment 
     * space.
     * @param fs
     */
    public FragmentSpaceParameters(FragmentSpace fs)
    {
        this(ParametersType.FS_PARAMS);
        buildingBlocksSpace = fs;
    }
    
//------------------------------------------------------------------------------

    public int getMaxHeavyAtom()
    {
        return maxHeavyAtom;
    }

//------------------------------------------------------------------------------

    public int getMaxRotatableBond()
    {
        return maxRotatableBond;
    }

//------------------------------------------------------------------------------

    public double getMaxMW()
    {
        return maxMW;
    }

//------------------------------------------------------------------------------
    
    public boolean enforceSymmetry()
    {
    	return enforceSymmetry;
    }

//------------------------------------------------------------------------------

    public boolean symmetryConstraints()
    {
        return symmetryConstraints;
    }

//------------------------------------------------------------------------------

    public String getRotSpaceDefFile()
    {
        return rotBndsFile;
    }
    
//------------------------------------------------------------------------------
    
    public String getPathnameToAppendedFragments()
    {
        File libFile = new File(fragmentLibFile);
        return libFile.getAbsolutePath() + "_addedFragments.sdf";
    }
    
//------------------------------------------------------------------------------
    
    public String getPathnameToAppendedScaffolds()
    {
        File libFile = new File(scaffoldLibFile);
        return libFile.getAbsolutePath() + "_addedScaffolds.sdf";
    }
    
//------------------------------------------------------------------------------
    
    public FragmentSpace getFragmentSpace()
    {
        return buildingBlocksSpace;
    }

//------------------------------------------------------------------------------

    public void interpretKeyword(String key, String value) 
            throws DENOPTIMException
    {
        String msg = "";
        switch (key.toUpperCase())
        {
        case "SCAFFOLDLIBFILE=":
            scaffoldLibFile = value;
            break;
        case "FRAGMENTLIBFILE=":
            fragmentLibFile = value;
            break;
        case "CAPPINGFRAGMENTLIBFILE=":
            cappingLibFile = value;
            break;
        case "COMPMATRIXFILE=":
            compMatrixFile = value;
            break;
        case "RCCOMPMATRIXFILE=":
            rcCompMatrixFile = value;
            break;
        case "ROTBONDSDEFFILE=":
            rotBndsFile = value;
            break;
        case "MAXHEAVYATOM=":
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
        case "MAXROTATABLEBOND=":
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
        case "MAXMW=":
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
        case "ENFORCESYMMETRY":
            enforceSymmetry = true;
            break;
    	case "CONSTRAINSYMMETRY=":
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
        		    APClass apClass = APClass.make(words[0]);
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
    	case "VERBOSITY=":
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

    public void checkParameters() throws DENOPTIMException
    {
        String msg = "";
        if (scaffoldLibFile.length() == 0)
        {
            msg = "No scaffold library file specified.";
            throw new DENOPTIMException(msg);
        }
        if (!FileUtils.checkExists(scaffoldLibFile))
        {
            msg = "Cannot find the scaffold library: " + scaffoldLibFile;
            throw new DENOPTIMException(msg);
        }

        if (fragmentLibFile.length() == 0)
        {
            msg = "No fragment library file specified.";
            throw new DENOPTIMException(msg);
        }
        if (!FileUtils.checkExists(fragmentLibFile))
        {
            msg = "Cannot find the fragment library: " +  fragmentLibFile;
            throw new DENOPTIMException(msg);
        }

        if (cappingLibFile.length() > 0)
        {
            if (!FileUtils.checkExists(cappingLibFile))
            {
                msg = "Cannot find the library of capping groups: "
                      + cappingLibFile;
                throw new DENOPTIMException(msg);
            }
        }

        if (compMatrixFile.length() > 0)
        {
            if (!FileUtils.checkExists(compMatrixFile))
            {
                msg = "Cannot find the compatibility matrix file: " 
                      + compMatrixFile;
                throw new DENOPTIMException(msg);
            }
        }

        if (rcCompMatrixFile.length() > 0)
        {
            if (!FileUtils.checkExists(rcCompMatrixFile))
            {
                msg = "Cannot find the ring-closures compatibility matrix "
                      + "file: " + rcCompMatrixFile;
                throw new DENOPTIMException(msg);
            }
        }

        if (rotBndsFile.length()>0 && !FileUtils.checkExists(rotBndsFile))
        {
            msg = "Cannot find file with definitions of rotatable bonds: " 
                  + rotBndsFile;
            throw new DENOPTIMException(msg);
        }
        
        checkOtherParameters();
    }
    
//------------------------------------------------------------------------------

    /**
     * Read the information collected in the parameters stored in this class
     * and create the fragment space accordingly.
     * @throws DENOPTIMException
     */
    public void processParameters() throws DENOPTIMException
    {
        buildingBlocksSpace = new FragmentSpace(this, 
                scaffoldLibFile, fragmentLibFile, cappingLibFile, 
                compMatrixFile, rcCompMatrixFile, 
                symmConstraintsMap);
        processOtherParameters();
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the list of parameters in a string with newline characters as
     * delimiters.
     * @return the list of parameters in a string with newline characters as
     * delimiters.
     */
    public String getPrintedList()
    {
        StringBuilder sb = new StringBuilder(1024);
        sb.append(" " + paramTypeName() + " ").append(NL);
        for (Field f : this.getClass().getDeclaredFields()) 
        {
            try
            {
                sb.append(f.getName()).append(" = ").append(
                            f.get(this)).append(NL);
            }
            catch (Throwable t)
            {
                sb.append("ERROR! Unable to print " + paramTypeName() 
                        + " parameters. Cause: " + t);
                break;
            }
        }
        for (RunTimeParameters otherCollector : otherParameters.values())
        {
            sb.append(otherCollector.getPrintedList());
        }
        return sb.toString();
    }

//------------------------------------------------------------------------------

    /**
     * Sets the fragment space linked to these parameters. This method should 
     * be used only in unit test to by-pass the creation of a
     * {@link FragmentSpace} from parameters.
     * @param fragmentSpace
     */
    public void setFragmentSpace(FragmentSpace fragmentSpace)
    {
        buildingBlocksSpace = fragmentSpace;
    }

//------------------------------------------------------------------------------

}
