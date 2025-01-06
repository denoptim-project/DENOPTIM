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

package denoptim.programs.mol2graph;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.exception.DENOPTIMException;
import denoptim.files.FileFormat;
import denoptim.files.FileUtils;
import denoptim.fragmenter.ScaffoldingPolicy;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.io.DenoptimIO;
import denoptim.programs.RunTimeParameters;
import denoptim.programs.fragmenter.CuttingRule;
import denoptim.programs.fragmenter.FragmenterParameters;


/**
 * Parameters controlling execution of GraphEditor.
 * 
 * @author Marco Foscato
 */

public class Mol2GraphParameters extends RunTimeParameters
{   
    /**
     * File with input graphs
     */
    private String inFile;

    /**
     * Input molecular objects
     */
    private List<IAtomContainer> inMols;

    /**
     * File with output graphs
     */
    private String outGraphsFile = null;
    private FileFormat outGraphsFormat = FileFormat.GRAPHSDF;;

//-----------------------------------------------------------------------------
    
    /**
     * Constructor
     */
    public Mol2GraphParameters()
    {
        super(ParametersType.M2G_PARAMS);
    }

//-----------------------------------------------------------------------------

      public int getInputMolsCount()
      {
          return inMols.size();
      }

//-----------------------------------------------------------------------------

    public IAtomContainer getInputMol(int i)
    {
        return inMols.get(i);
    }

//-----------------------------------------------------------------------------

    public String getOutFile()
    {
        return outGraphsFile;
    }
    
//-----------------------------------------------------------------------------
    
    public FileFormat getOutFormat()
    {
        return outGraphsFormat;
    }
    
//-----------------------------------------------------------------------------
    
    public List<CuttingRule> getCuttingRules()
    {
        ensureFragmenterParams();
        FragmenterParameters frgParams = (FragmenterParameters) 
                getParameters(ParametersType.FRG_PARAMS);
        return frgParams.getCuttingRules();
    }
    
//-----------------------------------------------------------------------------
    
    public ScaffoldingPolicy getScaffoldingPolicy()
    {
        ensureFragmenterParams();
        FragmenterParameters frgParams = (FragmenterParameters) 
                getParameters(ParametersType.FRG_PARAMS);
        return frgParams.getScaffoldingPolicy();
    }
    
//-----------------------------------------------------------------------------
    
    public double getLinearAngleLimit()
    {
        ensureFragmenterParams();
        FragmenterParameters frgParams = (FragmenterParameters) 
                getParameters(ParametersType.FRG_PARAMS);
        return frgParams.getLinearAngleLimit();
    }
    
//-----------------------------------------------------------------------------
    
    public FragmentSpace getFragmentSpace()
    {
        ensureFragSpareParams();
        FragmentSpaceParameters fsParams = (FragmentSpaceParameters) 
                getParameters(ParametersType.FS_PARAMS);
        return fsParams.getFragmentSpace();
    }
    
//-----------------------------------------------------------------------------
    
    private void ensureFragmenterParams()
    {
        if (!containsParameters(ParametersType.FRG_PARAMS))
        {
            setParameters(new FragmenterParameters());
        }
    }
    
//-----------------------------------------------------------------------------
    
    private void ensureFragSpareParams()
    {
        if (!containsParameters(ParametersType.FS_PARAMS))
        {
            setParameters(new FragmentSpaceParameters());
        }
    }
    
//-----------------------------------------------------------------------------

    /**
     * Processes a keyword/value pair and assign the related parameters.
     * @param key the keyword as string
     * @param value the value as a string
     * @throws DENOPTIMException
     */

    public void interpretKeyword(String key, String value)
            throws DENOPTIMException
    {
        String msg = "";
        switch (key.toUpperCase())
        {
            case "INPUTFILE=":
                inFile = value;
                break;
            case "WORKDIR=":
                workDir = value;
                break;
            case "OUTPUTGRAPHS=":
                outGraphsFile = value;
                break;
            case "OUTPUTGRAPHSFORMAT=":
                switch (value.toUpperCase())
                {
                    case "SDF":
                        outGraphsFormat = FileFormat.GRAPHSDF;
                        break;
                    case "JSON":
                        outGraphsFormat = FileFormat.GRAPHJSON;
                        break;
                    default:
                        outGraphsFormat = FileFormat.valueOf(value.toUpperCase());
                }
                break;
        	case "LOGFILE=":
        	    logFile = value;
                break;
            case "VERBOSITY=":
                try
                {
                    verbosity = Integer.parseInt(value);
                }
                catch (Throwable t)
                {
                    msg = "Unable to understand value " + key + "'" + value + "'";
                    throw new DENOPTIMException(msg);
                }
                break;
            default:
                 msg = "Keyword " + key + " is not a known GraphEditor-"
                                           + "related keyword. Check input files.";
                throw new DENOPTIMException(msg);
        }
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

//-----------------------------------------------------------------------------

    /**
     * Evaluate consistency of input parameters.
     * @throws DENOPTIMException
     */

    public void checkParameters() throws DENOPTIMException
    {
        String msg = "";
        if (!workDir.equals(".") && !FileUtils.checkExists(workDir))
        {
           msg = "Directory " + workDir + " not found. Please specify an "
                 + "existing directory.";
           throw new DENOPTIMException(msg);
        }

        if (inFile == null)
        {
            msg = "Input file not defined. Check you input.";
            throw new DENOPTIMException(msg);
	    }
        else if (inFile != null && !FileUtils.checkExists(inFile))
        {
            msg = "File with input molecules not found. Check " + inFile;
            throw new DENOPTIMException(msg);
        }

        if (outGraphsFile != null && FileUtils.checkExists(outGraphsFile))
        {
            msg = "Ouput file '" + outGraphsFile + "' exists already!";
            throw new DENOPTIMException(msg);
        }

        checkOtherParameters();
    }

//----------------------------------------------------------------------------

    /**
     * Processes all parameters and initialize related objects.
     * @throws DENOPTIMException
     */

    public void processParameters() throws DENOPTIMException 
    {
        try {
            inMols = DenoptimIO.readAllAtomContainers(new File(inFile));
        } catch (Exception e) {
            throw new DENOPTIMException("Cannot import molecules from '" 
                    + inFile + "'.", e);
        }
        
        processOtherParameters();
        
        ensureFragmenterParams();
        ensureFragSpareParams();
        
        FragmenterParameters frgParams = (FragmenterParameters) 
                getParameters(ParametersType.FRG_PARAMS);
        
        if (frgParams.getCuttingRules() == null ||
                frgParams.getCuttingRules().isEmpty())
        {
            List<CuttingRule> defaultCutRules = new ArrayList<CuttingRule>();
            try {
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new InputStreamReader(this
                            .getClass().getClassLoader().getResourceAsStream(
                                            "data/cutting_rules")));
                    DenoptimIO.readCuttingRules(reader, defaultCutRules, 
                            "bundled jar");
                    frgParams.setCuttingRules(defaultCutRules);
                } finally {
                    if (reader!=null)
                        reader.close();
                }
            } catch (Exception e )
            {
                throw new DENOPTIMException("Cannot load default cutting "
                        + "rules.", e);
            }
        }
    }
    
//-----------------------------------------------------------------------------

}
