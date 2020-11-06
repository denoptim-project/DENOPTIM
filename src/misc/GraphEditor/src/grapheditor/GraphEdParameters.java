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

package grapheditor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.molecule.DENOPTIMGraph;
import denoptim.utils.DENOPTIMGraphEdit;
import denoptim.utils.GraphConversionTool;
import denoptim.exception.DENOPTIMException;
import denoptim.logging.DENOPTIMLogger;
import denoptim.io.DenoptimIO;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.rings.RingClosureParameters;


/**
 * Parameters controlling execution of GraphEditor.
 * 
 * @author Marco Foscato
 */

public class GraphEdParameters
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
    private static String inGraphsFile = null;
    protected static final String STRINGFORMATLABEL = "STRING";
    protected static final String SERFORMATLABEL = "SER";
    private static String inGraphsFormat = STRINGFORMATLABEL; //Default

    /**
     * Input graphs
     */
    private static ArrayList<DENOPTIMGraph> inGraphs = 
					         new ArrayList<DENOPTIMGraph>();

    /**
     * Input molecular objects
     */
    private static ArrayList<IAtomContainer> inMols;

    /**
     * File with list of edit tasks
     */
    private static String graphEditsFile = null;

    /**
     * Graph's editing tasks
     */
    private static ArrayList<DENOPTIMGraphEdit> graphEdits;

    /**
     * File with output graphs
     */
    private static String outGraphsFile = null;
    private static String outGraphsFormat = STRINGFORMATLABEL; //Default

    /**
     * Flag controlling strategy with respect to symmetry
     */
    private static boolean symmetry = false;

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

    public static ArrayList<DENOPTIMGraphEdit> getGraphEditTasks()
    {
        return graphEdits;
    }

//-----------------------------------------------------------------------------

    public static int getVerbosity()
    {
        return verbosity;
    }

//-----------------------------------------------------------------------------

    public static ArrayList<DENOPTIMGraph> getInputGraphs()
    {
        return inGraphs;
    }

//-----------------------------------------------------------------------------

    public static IAtomContainer getInpMol(int i)
    {
        return inMols.get(i);
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

    public static boolean symmetryFlag()
    {
        return symmetry;
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

                if (line.toUpperCase().startsWith("GRAPHEDIT-"))
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
        case "GRAPHEDIT-WORKDIR=":
            workDir = value;
            break;
        case "GRAPHEDIT-INPUTGRAPHS=":
            inGraphsFile = value;
            break;
        case "GRAPHEDIT-ENFORCESYMMETRY=":
            if (value.toUpperCase().equals("YES") 
                || value.toUpperCase().equals("Y"))
            {
                symmetry = true;
            }
            break;
        case "GRAPHEDIT-GRAPHSEDITSFILE=":
            graphEditsFile = value;
            break;
        case "GRAPHEDIT-OUTPUTGRAPHS=":
            outGraphsFile = value;
            break;
        case "GRAPHEDIT-INPUTGRAPHSFORMAT=":
            inGraphsFormat = value.toUpperCase();
            break;
        case "GRAPHEDIT-OUTPUTGRAPHSFORMAT=":
            outGraphsFormat = value.toUpperCase();
            break;
	case "GRAPHEDIT-LOGFILE=":
	    logFile = value;
            break;
        case "GRAPHEDIT-VERBOSITY=":
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

//-----------------------------------------------------------------------------

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

        if (inGraphsFile == null)
        {
            msg = "Input file with graphs to edit not define. Check you input.";
            throw new DENOPTIMException(msg);
	}
        else if (inGraphsFile != null && !DenoptimIO.checkExists(inGraphsFile))
        {
            msg = "File with input graphs not found. Check " + inGraphsFile;
            throw new DENOPTIMException(msg);
        }
	else
	{
	    inGraphsFormat = FilenameUtils.getExtension(
						    inGraphsFile).toUpperCase();
	}

        if (graphEditsFile != null && !DenoptimIO.checkExists(graphEditsFile))
        {
            msg = "File with graph editing tasks not found. Check " + 
                                                                 graphEditsFile;
            throw new DENOPTIMException(msg);
        }

        if (outGraphsFile != null && DenoptimIO.checkExists(outGraphsFile))
        {
            msg = "Ouput file '" + outGraphsFile + "' exists aleary!";
            throw new DENOPTIMException(msg);
        }

        if (inGraphsFormat != null 
            && !inGraphsFormat.equals("SDF")
            && !inGraphsFormat.equals(STRINGFORMATLABEL)
            && !inGraphsFormat.equals(SERFORMATLABEL))
        {
            msg = " The format for providing input graph must be either '" 
                  + STRINGFORMATLABEL + "' (default) for human readable "
                  + "strings, or '" + SERFORMATLABEL 
                  + "' for serialized objects. "
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

//----------------------------------------------------------------------------

    /**
     * Processes all parameters and initialize related objects.
     * @throws DENOPTIMException
     */

    public static void processParameters() throws DENOPTIMException {
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
            outGraphsFile = inGraphsFile + ".mod" ;
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
                    inGraphs = DenoptimIO.readDENOPTIMGraphsFromFile(
                                                             inGraphsFile,true);
		    break;
                }
		case (SERFORMATLABEL):
                {
                    //TODO get arraylist of graphs or accept multiple files
                    DENOPTIMGraph g = DenoptimIO.deserializeDENOPTIMGraph(
                                                        new File(inGraphsFile));
                    inGraphs.add(g);
		    break;
                }
		case ("SDF"):
		{
		    inMols = DenoptimIO.readSDFFile(inGraphsFile);
		    int i = 0;
		    for (IAtomContainer m : inMols)
		    {
			i++;
			if (m.getProperty("GraphENC") != null)
            		{
			    String sGrp = m.getProperty("GraphENC").toString();
			    GraphConversionTool gct = new GraphConversionTool();
			    DENOPTIMGraph g = gct.getGraphFromString(sGrp);
			    inGraphs.add(g);
			}
			else
			{
			    String msg = "Molecule " + i + " in file '" 
					 + inGraphsFile + "' has no GraphENC. "
                                         + " Unable to read DENOPTIMGraph!";
                            throw new DENOPTIMException(msg);
			}
		    }
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
            String msg = "Cannot read in graphs from " + inGraphsFile;
            DENOPTIMLogger.appLogger.log(Level.INFO,msg);
            throw new DENOPTIMException(msg,t);
        }

        if (graphEditsFile != null)
        {
            try
            {
                graphEdits = DenoptimIO.readDENOPTIMGraphEditFromFile( 
                                                               graphEditsFile);
            }
            catch (Throwable t)
            {
                String msg = "Cannot read in graph editing tasks from " 
                                                              + graphEditsFile;
                DENOPTIMLogger.appLogger.log(Level.INFO,msg);
                throw new DENOPTIMException(msg,t);
            }
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

//----------------------------------------------------------------------------

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
        sb.append(" GraphEdParameters ").append(eol);
        for (Field f : GraphEdParameters.class.getDeclaredFields()) 
        {
            try
            {
                sb.append(f.getName()).append(" = ").append(
                            f.get(GraphEdParameters.class)).append(eol);
            }
            catch (Throwable t)
            {
                sb.append("ERROR! Unable to print GraphEdParameters. "+
                                                                "Cause: " + t);
                break;
            }
        }
        DENOPTIMLogger.appLogger.info(sb.toString());
        sb.setLength(0);

        FragmentSpaceParameters.printParameters();
        RingClosureParameters.printParameters();
    }

//----------------------------------------------------------------------------

}
