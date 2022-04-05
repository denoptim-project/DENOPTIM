package denoptim.programs.graphlisthandler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.logging.Level;

import denoptim.exception.DENOPTIMException;
import denoptim.files.FileUtils;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.graph.DENOPTIMGraph;
import denoptim.graph.rings.RingClosureParameters;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.programs.RunTimeParameters;
import denoptim.programs.RunTimeParameters.ParametersType;


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
    protected final String STRINGFORMATLABEL = "STRING";
    protected final String SERFORMATLABEL = "SER";
    protected final String SDFFORMATLABEL = "SDF";
    private String inGraphsFormat = STRINGFORMATLABEL; //Default

    /**
     * Input graphs: first list
     */
    protected ArrayList<DENOPTIMGraph> inGraphsA =
                             new ArrayList<DENOPTIMGraph>();
    
    /**
     * Input graphs: second list
     */
    protected ArrayList<DENOPTIMGraph> inGraphsB =
                             new ArrayList<DENOPTIMGraph>();

    /**
     * File with output graphs
     */
    private String outGraphsFile = null;
    private String outGraphsFormat = STRINGFORMATLABEL; //Default
    
//-----------------------------------------------------------------------------
    
    /**
     * Constructor
     */
    public GraphListsHandlerParameters()
    {
        this(ParametersType.GLH_PARAMS);
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Constructor
     */
    private GraphListsHandlerParameters(ParametersType paramType)
    {
        super(paramType);
    }

//-----------------------------------------------------------------------------

    public String getOutFile()
    {
        return outGraphsFile;
    }

//-----------------------------------------------------------------------------

    public String getInFormat()
    {
        return inGraphsFormat;
    }

//-----------------------------------------------------------------------------

    public String getOutFormat()
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
        case "GRAPHLISTS-WORKDIR=":
            workDir = value;
            break;
        case "GRAPHLISTS-INPUTGRAPHS-A=":
            inGraphsFileA = value;
            break;
        case "GRAPHLISTS-INPUTGRAPHS-B=":
            inGraphsFileB = value;
            break;
        case "GRAPHLISTS-OUTPUTGRAPHS=":
            outGraphsFile = value;
            break;
        case "GRAPHLISTS-INPUTGRAPHSFORMAT=":
            inGraphsFormat = value.toUpperCase();
            break;
        case "GRAPHLISTS-OUTPUTGRAPHSFORMAT=":
            outGraphsFormat = value.toUpperCase();
            break;
        case "GRAPHLISTS-LOGFILE=":
            logFile = value;
            break;
        case "GRAPHLISTS-VERBOSITY=":
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
            msg = "Ouput file '" + outGraphsFile + "' exists aleary!";
            throw new DENOPTIMException(msg);
        }

        if (inGraphsFormat != null
            && !inGraphsFormat.equals(STRINGFORMATLABEL)
            && !inGraphsFormat.equals(SDFFORMATLABEL))
        {
            msg = " The format for providing input graph must be either '"
                  + STRINGFORMATLABEL + "' (default), or '"
                  + SDFFORMATLABEL +"'."
                  + "Unable to understand '" + inGraphsFormat + "'.";
            throw new DENOPTIMException(msg);
        }
        else if (inGraphsFormat.equals(STRINGFORMATLABEL))
        {
            msg = "When in graphs are given as '"+ STRINGFORMATLABEL
                  + "' existing symmetry relations between vertices belonging "
                  + "to the in graphs are NOT perceived. Symmetry may only "
                  + "be enforced starting from the first new layer of "
                  + "vertices.";
            DENOPTIMLogger.appLogger.log(Level.WARNING,msg);
        }
        else if (inGraphsFormat.equals(SERFORMATLABEL))
        {
            msg = "For now, only one serialized DENOPTIMGraph can by "
                  + "given as user-defined input graph using format '"
                  + SERFORMATLABEL + "'.";
            DENOPTIMLogger.appLogger.log(Level.WARNING,msg);
        }

        if (outGraphsFormat != null
            && !outGraphsFormat.equals("SDF")
            && !outGraphsFormat.equals(STRINGFORMATLABEL)
            && !outGraphsFormat.equals(SERFORMATLABEL))
        {
            msg = " The format chosed for output graphs must be either '"
                  + STRINGFORMATLABEL + "' (default) for human readable "
                  + "strings, or '" + SERFORMATLABEL
                  + "' for serialized objects. "
                  + "Unable to understand '" + inGraphsFormat + "'.";
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
                String msg = "Ouput file '" + outGraphsFile +
                                                             "' exists aleary!";
                throw new DENOPTIMException(msg);
            }
        }

        try
        {
            switch (inGraphsFormat)
            {
                case (STRINGFORMATLABEL):
                {
                    inGraphsA = DenoptimIO.readDENOPTIMGraphsFromTxtFile(
                            inGraphsFileA);
                    inGraphsB = DenoptimIO.readDENOPTIMGraphsFromTxtFile(
                            inGraphsFileB);
                    break;
                }

                case (SDFFORMATLABEL):
                {
                    inGraphsA = DenoptimIO.readDENOPTIMGraphsFromSDFile(
                            inGraphsFileA);
                    inGraphsB = DenoptimIO.readDENOPTIMGraphsFromSDFile(
                            inGraphsFileB);
                    break;
                }

                default:
                {
                    String msg = "'" + inGraphsFormat + "'"
                                         + " is not a valid format for graphs.";
                    throw new DENOPTIMException(msg);
                }
            }
        }
        catch (Throwable t)
        {
            String msg = "Cannot read in graphs from " + inGraphsFileA +" or " 
                    + inGraphsFileB;
            DENOPTIMLogger.appLogger.log(Level.INFO,msg);
            throw new DENOPTIMException(msg,t);
        }

        try
        {
            DENOPTIMLogger.getInstance().setupLogger(logFile);
        }
        catch (IOException ioe)
        {
            throw new DENOPTIMException(ioe);
        }
    }

//------------------------------------------------------------------------------

}
