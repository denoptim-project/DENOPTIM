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

package denoptim.programs.genetweeker;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import denoptim.exception.DENOPTIMException;
import denoptim.files.FileUtils;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.ga.GAParameters;
import denoptim.graph.rings.RingClosureParameters;
import denoptim.programs.RunTimeParameters;
import denoptim.programs.RunTimeParameters.ParametersType;
import denoptim.utils.CrossoverType;
import denoptim.utils.MutationType;
import denoptim.utils.RandomUtils;


/**
 * Parameters controlling execution of TestOperator.
 * 
 * @author Marco Foscato
 */

public class GeneOpsRunnerParameters extends RunTimeParameters
{
    /**
     * Seed for generation of pseudo-random numbers
     */
    protected long randomSeed = 1234567890L;
    
    /**
     * Testable Operators. {@link #XOVER} is an alias for {@link #CROSSOVER}
     */
    protected enum Operator {MUTATION,XOVER,CROSSOVER}
    
    /**
     * Chosen operator
     */
    protected Operator operatorToTest = Operator.XOVER;

    /**
     * Target vertex ID for mutation. Multiple values indicate embedding into 
     * nested graphs.
     */
    protected int[] mutationTarget;
    
    /**
     * Target attachment point ID for mutation (AP belonging already to the 
     * graph). Zero-based index.
     */
    protected int idTargetAP = -1;
    
    /**
     * Type of mutation to perform
     */
    protected MutationType mutationType;
    
    /**
     * The given vertex index.  Zero-based index.
     * Used whenever a vertex id has to be given. For
     * example, when specifying how to mutate a graph. 
     */
    protected int idNewVrt = -1;
    
    /**
     * The given attachment point index. 
     * Used whenever an AP id has to be given. 
     * For example, when specifying how to mutate a graph. Zero-based index.
     */
    protected int idNewAP = -1;
    
    /**
     * Input File male
     */
    protected String inpFileM;

    /**
     * Input File female
     */
    protected String inpFileF;
    
    /**
     * Type of crossover to perform
     */
    protected CrossoverType xoverType = CrossoverType.BRANCH;
    
    /**
     * Maximum length of the subgraph that is swapped in a crossover operation
     */
    protected int maxSwappableChainLength = Integer.MAX_VALUE;

    /**
     * Male VertedID (not index) on which perform xover. 
     * Multiple values indicate embedding into nested graphs.
     */
    protected int[] xoverSrcMale;
    
    /**
     * Male VertedID (not index) that represent the end of the subgraph that
     * is swapped by crossover.
     * Multiple values indicate embedding into nested graphs.
     */
    protected List<int[]> xoverSubGraphEndMale = new ArrayList<int[]>();

    /**
     * Male AP index on which perform xover
     */
    protected int mapid;

    /**
     * Female VertexID (not index) on which perform xover.
     * Multiple values indicate embedding into nested graphs.
     */
    protected int[] xoverSrcFemale;
    
    /**
     * Female VertedID (not index) that represent the end of the subgraph that
     * is swapped by crossover.
     * Multiple values indicate embedding into nested graphs.
     */
    protected List<int[]> xoverSubGraphEndFemale = new ArrayList<int[]>();

    /**
     * Female AP index on which perform xover
     */
    protected int fapid;

    /**
     * Output file male
     */
    protected String outFileM;

    /**
     * Output file female
     */
    protected String outFileF;
    
//-----------------------------------------------------------------------------
    
    /**
     * Constructor
     */
    public GeneOpsRunnerParameters()
    {
        super(ParametersType.GO_PARAMS);
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
            case "OP=":
                operatorToTest = Operator.valueOf(value.toUpperCase());
                break;
            case "INPFILE=":
                inpFileM = value;
                break;
            case "OUTFILE=":
                outFileM = value;
                break;
            case "MUTATIONTARGET=":
            {
                String[] parts = value.split(",");
                mutationTarget = new int[parts.length];
                for (int i=0; i<parts.length; i++)
                {
                    mutationTarget[i] = Integer.parseInt(parts[i].trim());
                }
                break;
            }
            case "APIDONTARGETVERTEX=":
                idTargetAP = Integer.parseInt(value);
                break;
            case "MUTATIONTYPE=":
                mutationType = MutationType.valueOf(value);
                break;
            case "NEWVERTEXMOLID=":
                idNewVrt = Integer.parseInt(value);
                break;
            case "NEWAPID=":
                idNewAP = Integer.parseInt(value);
                break;
            case "WORKDIR=":
                workDir = value;
                break;
            case "INPFILEMALE=":
                inpFileM = value;
                break;
            case "INPFILEFEMALE=":
    	        inpFileF = value;
                break;
            case "VERTEXMALE=":
            {
                String[] parts = value.split(",");
                xoverSrcMale = new int[parts.length];
                for (int i=0; i<parts.length; i++)
                {
                    xoverSrcMale[i] = Integer.parseInt(parts[i].trim());
                }
                break;
            }
            case "SUBGRAPHENDMALE=":
            {
                String[] parts = value.split(",");
                int[] idList = new int[parts.length];
                for (int i=0; i<parts.length; i++)
                {
                    idList[i] = Integer.parseInt(parts[i].trim());
                }
                xoverSubGraphEndMale.add(idList);
                break;
            }
            case "SUBGRAPHENDFEMALE=":
            {
                String[] parts = value.split(",");
                int[] idList = new int[parts.length];
                for (int i=0; i<parts.length; i++)
                {
                    idList[i] = Integer.parseInt(parts[i].trim());
                }
                xoverSubGraphEndFemale.add(idList);
                break;
            }
            case "APMALE=":
                mapid = Integer.parseInt(value);
                break;
            case "XOVERTYPE=":
                xoverType = CrossoverType.valueOf(value);
                break;
            case "CROSSOVERTYPE=":
                xoverType = CrossoverType.valueOf(value);
                break;
            case "VERTEXFEMALE=":
            {
                String[] parts = value.split(",");
                xoverSrcFemale = new int[parts.length];
                for (int i=0; i<parts.length; i++)
                {
                    xoverSrcFemale[i] = Integer.parseInt(parts[i].trim());
                }
                break;
            }
            case "APFEMALE=":
                fapid = Integer.parseInt(value);
                break;
            case "OUTFILEMALE=":
                outFileM = value;
                break;
            case "OUTFILEFEMALE=":
                outFileF = value;
                break;
            default:
                 msg = "Keyword " + key + " is not a known TestOperator-"
                                       + "related keyword. Check input files.";
                 throw new DENOPTIMException(msg);
        }
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

        if (!FileUtils.checkExists(inpFileM))
        {
            msg = "Input file '" + inpFileM + "' not found.";
            throw new DENOPTIMException(msg);
        }

        if (operatorToTest == Operator.XOVER
                && !FileUtils.checkExists(inpFileF))
        {
            msg = "Input file '" + inpFileF + "' not found.";
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
        RandomUtils.initialiseRNG(randomSeed);
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

//----------------------------------------------------------------------------

}
