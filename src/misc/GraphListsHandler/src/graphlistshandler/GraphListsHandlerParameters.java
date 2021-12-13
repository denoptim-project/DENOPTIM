package graphlistshandler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.logging.Level;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.graph.DENOPTIMGraph;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.rings.RingClosureParameters;


/**
 * Parameters controlling execution of GraphEditor.
 *
 * @author Marco Foscato
 */

public class GraphListsHandlerParameters
{
    /**
     * Flag indicating that at least one parameter has been defined
     */
    private static boolean grEdParamsInUse = false;

    /**
     * Working directory
     */
    private static String workDir = ".";

    /**
     * Log file
     */
    private static String logFile = "GraphEd.log";

    /**
     * File with input graphs
     */
    private static String inGraphsFileA = null;
    private static String inGraphsFileB = null;
    protected static final String STRINGFORMATLABEL = "STRING";
    protected static final String SERFORMATLABEL = "SER";
    protected static final String SDFFORMATLABEL = "SDF";
    private static String inGraphsFormat = STRINGFORMATLABEL; //Default

    /**
     * Input graphs: first list
     */
    protected static ArrayList<DENOPTIMGraph> inGraphsA =
                             new ArrayList<DENOPTIMGraph>();
    
    /**
     * Input graphs: second list
     */
    protected static ArrayList<DENOPTIMGraph> inGraphsB =
                             new ArrayList<DENOPTIMGraph>();

    /**
     * File with output graphs
     */
    private static String outGraphsFile = null;
    private static String outGraphsFormat = STRINGFORMATLABEL; //Default


    /**
     * Verbosity level
     */
    private static int verbosity = 0;


//-----------------------------------------------------------------------------

    public static boolean grEdParamsInUse()
    {
        return grEdParamsInUse;
    }

//-----------------------------------------------------------------------------

    public static String getWorkDirectory()
    {
        return workDir;
    }

//-----------------------------------------------------------------------------

    public static String getLogFileName()
    {
        return logFile;
    }

//-----------------------------------------------------------------------------

    public static int getVerbosity()
    {
        return verbosity;
    }

//-----------------------------------------------------------------------------

    public static String getOutFile()
    {
        return outGraphsFile;
    }

//-----------------------------------------------------------------------------

    public static String getInFormat()
    {
        return inGraphsFormat;
    }

//-----------------------------------------------------------------------------

    public static String getOutFormat()
    {
        return outGraphsFormat;
    }

//-----------------------------------------------------------------------------

    /**
     * Read the parameter TXT file line by line and interpret its content.
     * @param infile
     * @throws DENOPTIMException
     */

    public static void readParameterFile(String infile) throws DENOPTIMException
    {
        String option, line;
        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(infile));
            while ((line = br.readLine()) != null)
            {
                if ((line.trim()).length() == 0)
                {
                    continue;
                }

                if (line.startsWith("#"))
                {
                    continue;
                }

                if (line.toUpperCase().startsWith("FS-"))
                {
                    FragmentSpaceParameters.interpretKeyword(line);
                    continue;
                }

                if (line.toUpperCase().startsWith("RC-"))
                {
                    RingClosureParameters.interpretKeyword(line);
                    continue;
                }

                if (line.toUpperCase().startsWith("GRAPHLISTS-"))
                {
                    interpretKeyword(line);
                    continue;
                }
            }
        }
        catch (NumberFormatException | IOException nfe)
        {
            throw new DENOPTIMException(nfe);
        }
        finally
        {
            try
            {
                if (br != null)
                {
                    br.close();
                    br = null;
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }

        option = null;
        line = null;
    }

//-----------------------------------------------------------------------------

    /**
     * Processes a string looking for keyword and a possibly associated value.
     * @param line the string to parse
     * @throws DENOPTIMException
     */

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

//-----------------------------------------------------------------------------

    /**
     * Processes a keyword/value pair and assign the related parameters.
     * @param key the keyword as string
     * @param value the value as a string
     * @throws DENOPTIMException
     */

    public static void interpretKeyword(String key, String value)
                                                      throws DENOPTIMException
    {
        grEdParamsInUse = true;
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

    public static void checkParameters() throws DENOPTIMException
    {
        String msg = "";
        if (!grEdParamsInUse)
        {
            return;
        }

        if (!workDir.equals(".") && !DenoptimIO.checkExists(workDir))
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
                && !DenoptimIO.checkExists(inGraphsFileA))
        {
            msg = "File with input graphs not found. Check " + inGraphsFileA;
            throw new DENOPTIMException(msg);
        }
        else if (inGraphsFileB != null 
                && !DenoptimIO.checkExists(inGraphsFileB))
        {
            msg = "File with input graphs not found. Check " + inGraphsFileB;
            throw new DENOPTIMException(msg);
        }

        if (outGraphsFile != null && DenoptimIO.checkExists(outGraphsFile))
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

        if (FragmentSpaceParameters.fsParamsInUse())
        {
            FragmentSpaceParameters.checkParameters();
        }

        if (RingClosureParameters.rcParamsInUse())
        {
            RingClosureParameters.checkParameters();
        }
    }

//------------------------------------------------------------------------------

    /**
     * Processes all parameters and initialize related objects.
     * @throws DENOPTIMException
     */

    public static void processParameters() throws DENOPTIMException
    {
        if (FragmentSpaceParameters.fsParamsInUse())
        {
            FragmentSpaceParameters.processParameters();
        }

        if (RingClosureParameters.allowRingClosures())
        {
            RingClosureParameters.processParameters();
        }

        if (outGraphsFile == null)
        {
            outGraphsFile = "graphListHandler.output" ;
            if (DenoptimIO.checkExists(outGraphsFile))
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
                            inGraphsFileA,true);
                    inGraphsB = DenoptimIO.readDENOPTIMGraphsFromTxtFile(
                            inGraphsFileB,true);
                    break;
                }

                case (SDFFORMATLABEL):
                {
                    inGraphsA = DenoptimIO.readDENOPTIMGraphsFromSDFile(
                            inGraphsFileA,true);
                    inGraphsB = DenoptimIO.readDENOPTIMGraphsFromSDFile(
                            inGraphsFileB,true);
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

    /**
     * Print all parameters.
     */

    public static void printParameters()
    {
        if (!grEdParamsInUse)
        {
            return;
        }
        String eol = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder(1024);
        sb.append(" GraphListsHandlerParameters ").append(eol);
        for (Field f : GraphListsHandlerParameters.class.getDeclaredFields())
        {
            try
            {
                sb.append(f.getName()).append(" = ").append(
                        f.get(GraphListsHandlerParameters.class)).append(eol);
            }
            catch (Throwable t)
            {
                sb.append("ERROR! Unable to print "
                        + "GraphListsHandlerParameters. Cause: " + t);
                break;
            }
        }
        DENOPTIMLogger.appLogger.info(sb.toString());
        sb.setLength(0);

        FragmentSpaceParameters.printParameters();
        RingClosureParameters.printParameters();
    }

//------------------------------------------------------------------------------

}
