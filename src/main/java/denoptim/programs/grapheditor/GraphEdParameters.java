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

package denoptim.programs.grapheditor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.logging.Level;

import org.apache.commons.io.FilenameUtils;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.exception.DENOPTIMException;
import denoptim.files.FileUtils;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.graph.DENOPTIMGraph;
import denoptim.graph.rings.RingClosureParameters;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.programs.RunTimeParameters;
import denoptim.programs.RunTimeParameters.ParametersType;
import denoptim.utils.DENOPTIMGraphEdit;


/**
 * Parameters controlling execution of GraphEditor.
 * 
 * @author Marco Foscato
 */

public class GraphEdParameters extends RunTimeParameters
{   
    /**
     * File with input graphs
     */
    private String inGraphsFile = null;
    

    //TODO: get rid of these and detect file format
    protected final String STRINGFORMATLABEL = "STRING";
    protected final String SERFORMATLABEL = "SER";
    //TODO: use FileFormat
    private String inGraphsFormat = STRINGFORMATLABEL; //Default

    /**
     * Input graphs
     */
    private ArrayList<DENOPTIMGraph> inGraphs = 
					         new ArrayList<DENOPTIMGraph>();

    /**
     * Input molecular objects
     */
    private ArrayList<IAtomContainer> inMols;

    /**
     * File with list of edit tasks
     */
    private String graphEditsFile = null;

    /**
     * Graph's editing tasks
     */
    private ArrayList<DENOPTIMGraphEdit> graphEdits;

    /**
     * File with output graphs
     */
    private String outGraphsFile = null;
    private String outGraphsFormat = STRINGFORMATLABEL; //Default

    /**
     * Flag controlling strategy with respect to symmetry
     */
    private boolean symmetry = false;

//-----------------------------------------------------------------------------
    
    /**
     * Constructor
     */
    public GraphEdParameters()
    {
        super(ParametersType.GE_PARAMS);
    }

//-----------------------------------------------------------------------------

    public ArrayList<DENOPTIMGraphEdit> getGraphEditTasks()
    {
        return graphEdits;
    }
    
//-----------------------------------------------------------------------------

    public ArrayList<DENOPTIMGraph> getInputGraphs()
    {
        return inGraphs;
    }

//-----------------------------------------------------------------------------

    public IAtomContainer getInpMol(int i)
    {
        return inMols.get(i);
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

    public boolean symmetryFlag()
    {
        return symmetry;
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
        case "INPUTGRAPHS=":
            inGraphsFile = value;
            break;
        case "ENFORCESYMMETRY=":
            if (value.toUpperCase().equals("YES") 
                || value.toUpperCase().equals("Y"))
            {
                symmetry = true;
            }
            break;
        case "GRAPHSEDITSFILE=":
            graphEditsFile = value;
            break;
        case "OUTPUTGRAPHS=":
            outGraphsFile = value;
            break;
        case "INPUTGRAPHSFORMAT=":
            inGraphsFormat = value.toUpperCase();
            break;
        case "OUTPUTGRAPHSFORMAT=":
            outGraphsFormat = value.toUpperCase();
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

        if (inGraphsFile == null)
        {
            msg = "Input file with graphs to edit not define. Check you input.";
            throw new DENOPTIMException(msg);
	    }
        else if (inGraphsFile != null && !FileUtils.checkExists(inGraphsFile))
        {
            msg = "File with input graphs not found. Check " + inGraphsFile;
            throw new DENOPTIMException(msg);
        }
	    else
	    {
	        inGraphsFormat = FilenameUtils.getExtension(
			    			    inGraphsFile).toUpperCase();
	    }

        if (graphEditsFile != null && !FileUtils.checkExists(graphEditsFile))
        {
            msg = "File with graph editing tasks not found. Check " + 
                                                                 graphEditsFile;
            throw new DENOPTIMException(msg);
        }

        if (outGraphsFile != null && FileUtils.checkExists(outGraphsFile))
        {
            msg = "Ouput file '" + outGraphsFile + "' exists already!";
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
        checkOtherParameters();
    }

//----------------------------------------------------------------------------

    /**
     * Processes all parameters and initialize related objects.
     * @throws DENOPTIMException
     */

    public void processParameters() throws DENOPTIMException 
    {
        processOtherParameters();
        
        if (outGraphsFile == null)
        {
            outGraphsFile = inGraphsFile + ".mod" ;
            if (FileUtils.checkExists(outGraphsFile))
            {
                String msg = "Ouput file '" + outGraphsFile + "' exists already!";
                throw new DENOPTIMException(msg);
            }
        }

        try
        {
    	    switch (inGraphsFormat)
    	    {
        		case (STRINGFORMATLABEL):
        		{
                    inGraphs = DenoptimIO.readDENOPTIMGraphsFromTxtFile(
                            inGraphsFile);
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
            			DENOPTIMGraph g = DenoptimIO.readGraphFromSDFileIAC(m, 
            			        i, inGraphsFile);
            			inGraphs.add(g);
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

}
