/*
 *   DENOPTIM
 *   Copyright (C) 2022 Marco Foscato <marco.foscato@uib.no>
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

package denoptim.programs.graphlisthandler;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;

import denoptim.exception.DENOPTIMException;
import denoptim.files.FileFormat;
import denoptim.files.FileUtils;
import denoptim.graph.DGraph;
import denoptim.io.DenoptimIO;
import denoptim.programs.RunTimeParameters;


/**
 * Parameters controlling execution of GraphEditor.
 *
 * @author Marco Foscato
 */

public class GraphListsHandlerParameters extends RunTimeParameters
{
    /**
     * File with input graphs
     */
    private String inGraphsFileA = null;
    private String inGraphsFileB = null;

    /**
     * Input graphs: first list
     */
    protected ArrayList<DGraph> inGraphsA =
                             new ArrayList<DGraph>();
    
    /**
     * Input graphs: second list
     */
    protected ArrayList<DGraph> inGraphsB =
                             new ArrayList<DGraph>();

    /**
     * File with output graphs
     */
    private String outGraphsFile = null;
    private FileFormat outGraphsFormat = FileFormat.GRAPHSDF; //Default
    
//-----------------------------------------------------------------------------
    
    /**
     * Constructor
     */
    public GraphListsHandlerParameters()
    {
        super(ParametersType.GLH_PARAMS);
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
        case "WORKDIR=":
            workDir = value;
            break;
        case "INPUTGRAPHS-A=":
            inGraphsFileA = value;
            break;
        case "INPUTGRAPHS-B=":
            inGraphsFileB = value;
            break;
        case "OUTPUTGRAPHS=":
            outGraphsFile = value;
            break;
        case "OUTPUTGRAPHSFORMAT=":
            outGraphsFormat = FileFormat.valueOf(value.toUpperCase());
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
             msg = "Keyword " + key + " is not a known GraphListHandler-"
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

//------------------------------------------------------------------------------

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

        if (inGraphsFileA == null || inGraphsFileB == null)
        {
            msg = "Input file with graphs to edit not define. Check you input.";
            throw new DENOPTIMException(msg);
        }
        else if (inGraphsFileA != null 
                && !FileUtils.checkExists(inGraphsFileA))
        {
            msg = "File with input graphs not found. Check " + inGraphsFileA;
            throw new DENOPTIMException(msg);
        }
        else if (inGraphsFileB != null 
                && !FileUtils.checkExists(inGraphsFileB))
        {
            msg = "File with input graphs not found. Check " + inGraphsFileB;
            throw new DENOPTIMException(msg);
        }

        if (outGraphsFile != null && FileUtils.checkExists(outGraphsFile))
        {
            msg = "Output file '" + outGraphsFile + "' exists aleary!";
            throw new DENOPTIMException(msg);
        }

        checkOtherParameters();
    }

//------------------------------------------------------------------------------

    /**
     * Processes all parameters and initialize related objects.
     * @throws DENOPTIMException
     */
    public void processParameters() throws DENOPTIMException
    {
        processOtherParameters();

        if (outGraphsFile == null)
        {
            outGraphsFile = "graphListHandler.output" ;
            if (FileUtils.checkExists(outGraphsFile))
            {
                String msg = "Output file '" + outGraphsFile +
                                                             "' exists aleary!";
                throw new DENOPTIMException(msg);
            }
        }

        try
        {
            inGraphsA = DenoptimIO.readDENOPTIMGraphsFromFile(new File(
                    inGraphsFileA));
            inGraphsB = DenoptimIO.readDENOPTIMGraphsFromFile(new File(
                    inGraphsFileB));
        }
        catch (Throwable t)
        {
            throw new Error("Cannot read in graphs from " + inGraphsFileA +" or " 
                    + inGraphsFileB);
        }
    }

//------------------------------------------------------------------------------

}
