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

package denoptim.graph.rings;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import denoptim.exception.DENOPTIMException;
import denoptim.files.FileUtils;
import denoptim.graph.APClass;
import denoptim.logging.StaticLogger;
import denoptim.programs.RunTimeParameters;


/**
 * Parameters and setting related to handling ring closures.
 *
 * @author Marco Foscato
 */

public class RingClosureParameters extends RunTimeParameters
{
    /**
     * Flag activating the ring closing machinery.
     */
    protected boolean closeRings = false;

    /**
     * Flag requesting complete ring closure of all pairs of RCAs in at least
     * one combination of RCAs to consider the calculation successful.
     */
    //TODO-V3 make controllable from parameters side.
    public boolean requireCompleteRingclosure = true;
    
    /**
     * Flag activating procedures favoring formation of chelates.
     */
    protected boolean buildChelatesMode = false;
    
    /**
     * List of metal-coordinating APClasses. Used to identify 'orphan'
     * metal-coordinating sites that give rise to coordination isomerism (i.e.,
     * we have build a specific isomer, but the existence of unused metal-
     * coordinating groups opens for the possibility of binding the metal/s with
     * a different combination of groups).
     */
    protected Set<APClass> metalCoordinatingAPClasses = 
            new HashSet<APClass>();

    /**
     * Flag activating the biased selection of closable fragment chains.
     */
    protected boolean selectFragsFromCC = false;

    /**
     * The ring closability evaluation mode:
     * -1= only ring rize bias
     * 0 = only constitution of candidate ring,
     * 1 = only closability of 3D chain,
     * 2 = both 0 and 1.
     */
    protected int rceMode = -1;

    /**
     * Maximum number of rotatable bonds for which conformational space
     * is explored.
     */
    protected int maxRotBonds = 7;

    /**
     * Maximum size (number of atoms) in ring closing chain. 
     * The shortest path is considered.
     */
    protected int maxRingSize = 9;

    /**
     * Minimum number of <code>RingClosingAttractor</code>s in a valid graph.
     */
    protected int minRcaPerType = 0;

    /**
     * Maximum number of <code>RingClosingAttractor</code>s in a valid graph.
     */
    protected int maxRcaPerType = 50;

    /**
     * Minimum number of <code>RingClosure</code>s in a valid graph.
     */
    protected int minRingClosures = 0;

    /**
     * Maximum number of <code>RingClosure</code>s in a valid graph.
     */
    protected int maxRingClosures = 50;

    /**
     * Maximum value for non-flat bond angle (in degree).
     */
    protected double linearityLimit = 178.5;

    /**
     * Tolerance factor for interatomic distances. This value
     * is the percentace (p) of the average between the attachment point 
     * vector lengths at the head (l_H) and tail (l_T) of a candidate 
     * chain.
     * </br>
     * dt = p * (l_H + l_T) / 2 
     * </br>
     * Interatomic distance criteria are satisfied with a deviation of +/- dt
     */
    protected double rcTolDist = 0.33;

    /**
     * Extra tolerance factor for interatomic distances. This value (ep) is 
     * multiplied to the tolerance factor (p) when discrete as opposite to 
     * continue variation of torsion angles is performed.
     * With the normal tolerance factor (p) and the lengths of the
     * AP vectors l_H and l_T, the accepted deviation to an interatomic
     * distance (dt) is calculated as
     * </br>
     * dt = ep * p * (l_H + l_T) / 2
     * </br>
     */
    protected double pathConfSearchExtraTol = 1.1;

    /**
     * Maximum value of dot product between the normalized 
     * attachment point vectors at the head and tail of a candidate chain.
     * Note that perfect alignement implies a dot product of -1.0.
     */
    protected double rcMaxDot = -0.75;

    /**
     * Torsion angle step for conformational scan of candidate closable chain
     */
    protected double pathConfSearchStep = 12.0;

    /**
     * Relative weight of ring sizes to bias the selection of a ring 
     * combination among the various alternatives.
     */
    protected ArrayList<Integer> ringSizeBias = new ArrayList<Integer>()
        {
            private final long serialVersionUID = 1L;
            {
                for (int i=0; i<maxRingSize+1; i++)
                {
                    add(0);
                }
    	    if (maxRingSize>=7)
    	    {
                    // WARNING: if the default value of maxRingSize is changed
                    // also the default content of this array has to change
                    set(5,2);
                    set(6,4);
                    set(7,1);
    	    }
        }};

    /**
     * SMARTS defining the constitution-based ring closability condition.
     */
    protected Map<String,String> ringClosabCondAsSMARTS =
                                                 new HashMap<String,String>();

    /**
     * Required elements in closable chains.
     */
    protected Set<String> reqElInRings = new HashSet<String>();

    /**
     * Pathname of the text file containing the list of visited
     * <code>RingClosingConformation</code>s (i.e., index file).
     * The index file is machine-written and is bound to the 
     * corresponding libraries of fragments.
     */
    protected String rccIndex = "";

    /**
     * Pathname of the root folder containing the archive of serialized
     * <code>RingClosingConformation</code>s.
     */
    protected String rccFolder = "";
    
    /**
     * Collection of information about ring-closability of graph substructures.
     */
    private RingClosuresArchive rcArchive;

    /**
     * Flag controlling conformational search. If <code>true</code> the
     * whole torsional space is scanned looking for ring closing
     * conformations. WARNING! This is very time consuming, but is needed 
     * for the current implementation of the iterdependent ring criterion.
     */
    protected boolean exhaustiveConfSrch = false; 

    /**
     * Flag controlling the ring-closing criterion evaluating the 
     * simultaneous closability of interdependent chains.
     * If <code>true</code> requires the evaluation of interdependent chains.
     */
    protected boolean checkInterdepPaths = false;

    /**
     * FLag controlling the serialization of the 
     * <code>RingClosingConformations</code>. This flag is activated by the
     * keyword providing the pathname of the root folder of the RCCs archive
     */
    protected boolean serializeRCCs = false;

//-----------------------------------------------------------------------------
    
    /**
     * Constructor
     * @param paramType the type of parameters this instance is meant to collect.
     */
    public RingClosureParameters()
    {
        super(ParametersType.RC_PARAMS);
        rcArchive = new RingClosuresArchive();
    }

//----------------------------------------------------------------------------

    public int getClosabilityEvalMode()
    {
        return rceMode;
    }

//----------------------------------------------------------------------------

    public int getMaxNumberRotatableBonds()
    {
        return maxRotBonds;
    }

//----------------------------------------------------------------------------

    public int getMaxRingSize()
    {
        return maxRingSize;
    }

//----------------------------------------------------------------------------

    public ArrayList<Integer> getRingSizeBias()
    {
        return ringSizeBias;
    }

//------------------------------------------------------------------------------

    public int getMinRcaPerType()
    {
        return minRcaPerType;
    }

//------------------------------------------------------------------------------

    public int getMaxRcaPerType()
    {
        return maxRcaPerType;
    }

//------------------------------------------------------------------------------

    public int getMinRingClosures()
    {
        return minRingClosures;
    }

//------------------------------------------------------------------------------

    public int getMaxRingClosures()
    {
        return maxRingClosures;
    }

//----------------------------------------------------------------------------

    public Map<String,String> getConstitutionalClosabilityConds()
    {
        return ringClosabCondAsSMARTS;
    }

//----------------------------------------------------------------------------

    public int getVerbosity()
    {
        return verbosity;
    }

//-----------------------------------------------------------------------------

    public void allowRingClosures(boolean value)
    {
        closeRings = value;
    }
    
//-----------------------------------------------------------------------------

    public boolean allowRingClosures()
    {
        return closeRings;
    }

//-----------------------------------------------------------------------------

    public boolean buildChelatesMode()
    {
        return buildChelatesMode;
    }

//----------------------------------------------------------------------------

    public boolean selectFragmentsFromClosableChains()
    {
        return selectFragsFromCC;
    }

//----------------------------------------------------------------------------

    public double getRCDistTolerance()
    {
        return rcTolDist;
    }

//----------------------------------------------------------------------------

    public double getConfPathExtraTolerance()
    {
        return pathConfSearchExtraTol;
    }

//----------------------------------------------------------------------------

    public double getRCDotPrTolerance()
    {
        return rcMaxDot;
    }

//----------------------------------------------------------------------------

    public double getLinearityLimit()
    {
        return linearityLimit;
    }

//----------------------------------------------------------------------------

    public double getPathConfSearchStep()
    {
        return pathConfSearchStep;
    }

//----------------------------------------------------------------------------

    public Set<String> getRequiredRingElements()
    {
        return reqElInRings;
    }

//----------------------------------------------------------------------------

    public String getRCCLibraryIndexFile()
    {
        return rccIndex;
    }

//----------------------------------------------------------------------------

    public String getRCCLibraryFolder()
    {
        return rccFolder;
    }
    
//----------------------------------------------------------------------------
    
    public RingClosuresArchive getRingClosuresArchive()
    {
        return rcArchive;
    }

//----------------------------------------------------------------------------

    public boolean doExhaustiveConfSrch()
    {
	return exhaustiveConfSrch;
    }

//----------------------------------------------------------------------------

    public boolean serializeRCCs()
    {
	return serializeRCCs;
    }

//----------------------------------------------------------------------------

    public boolean checkInterdependentChains()
    {
	return checkInterdepPaths;
    }

//----------------------------------------------------------------------------

    public void interpretKeyword(String key, String value)
            throws DENOPTIMException
    {
        String msg = "";
        switch (key.toUpperCase())
        {
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
            case "CLOSERINGS":
                closeRings = true;
                break;
            case "CLOSERINGS=":
                closeRings = true;
                break;
            case "BUILDCHELATESMODE":
                buildChelatesMode = true;
                break;
            case "ORPHANAPCLASS=":
                buildChelatesMode = true;
                metalCoordinatingAPClasses.add(APClass.make(value));
                break;
            case "SELECTFRAGMENTSFROMCLOSABLECHAINS":
        	    selectFragsFromCC = true;
        	    break;
            case "EVALUATIONCLOSABILITYMODE=":
            	switch (value.toUpperCase())
            	{
    	        	case "RING_SIZE":
    	    			rceMode = -1;
    	    			break;
    	    			
            		case "CONSTITUTION":
            			rceMode = 0;
            			break;
            			
            		case "3D-CONFORMATION":
            			rceMode = 1;
            			break;
            			
            		case "CONSTITUTION_AND_3D-CONFORMATION":
            			rceMode = 2;
            			break;
            			
            		default:
            			msg = "Unable to understand value '" + value + "'";
            	        throw new DENOPTIMException(msg);
            	}
                break;
            case "MAXROTBONDS=":
                try
                {
                    maxRotBonds = Integer.parseInt(value);
                }
                catch (Throwable t)
                {
                    msg = "Unable to understand value '" + value + "'";
                    throw new DENOPTIMException(msg);
                }
                break;
            case "MAXSIZENEWRINGS=":
                try
                {
                    maxRingSize = Integer.parseInt(value);
                }
                catch (Throwable t)
                {
                    msg = "Unable to understand value '" + value + "'";
                    throw new DENOPTIMException(msg);
                }
                break;
            case "MINRCAPERTYPEPERGRAPH=":
                try
                {
                    minRcaPerType = Integer.parseInt(value);
                }
                catch (Throwable t)
                {
                    msg = "Unable to understand value '" + value + "'";
                    throw new DENOPTIMException(msg);
                }
                break;
            case "MAXRCAPERTYPEPERGRAPH=":
                try
                {
                    maxRcaPerType = Integer.parseInt(value);
                }
                catch (Throwable t)
                {
                    msg = "Unable to understand value '" + value + "'";
                    throw new DENOPTIMException(msg);
                }
                break;
            case "MINNUMBEROFRINGCLOSURES=":
                try
                {
                    minRingClosures = Integer.parseInt(value);
                }
                catch (Throwable t)
                {
                    msg = "Unable to understand value '" + value + "'";
                    throw new DENOPTIMException(msg);
                }
                break;
            case "MAXNUMBERRINGCLOSURES=":
                try
                {
                    maxRingClosures = Integer.parseInt(value);
                }
                catch (Throwable t)
                {
                    msg = "Unable to understand value '" + value + "'";
                    throw new DENOPTIMException(msg);
                }
                break;
            case "LINEARITYLIMIT=":
                try
                {
                    linearityLimit = Double.parseDouble(value);
                }
                catch (Throwable t)
                {
                    msg = "Unable to understand value '" + value + "'";
                    throw new DENOPTIMException(msg);
                }
                break;
            case "DISTANCETOLERANCEFACTOR=":
                try
                {
                    rcTolDist = Double.parseDouble(value);
                }
                catch (Throwable t)
                {
                    msg = "Unable to understand value '" + value + "'";
                    throw new DENOPTIMException(msg);
                }
                break;
            case "EXTRADISTANCETOLERANCEFACTOR=":
                try
                {
                    pathConfSearchExtraTol = Double.parseDouble(value);
                }
                catch (Throwable t)
                {
                    msg = "Unable to understand value '" + value + "'";
                    throw new DENOPTIMException(msg);
                }
                break;
            case "MAXDOTPROD=":
                try
                {
                    rcMaxDot = Double.parseDouble(value);
                }
                catch (Throwable t)
                {
                    msg = "Unable to understand value '" + value + "'";
                    throw new DENOPTIMException(msg);
                }
                break;
            case "CONFSEARCHSTEP=":
                try
                {
                    pathConfSearchStep = Double.parseDouble(value);
                }
                catch (Throwable t)
                {
                    msg = "Unable to understand value '" + value + "'";
                    throw new DENOPTIMException(msg);
                }
                break;
            case "RINGSIZEBIAS=":
                String[] words = value.split("\\s+");
                if (words.length != 2)
                {
                     msg = "Unable to read ring size and bias wevight from "
                                 + "'" + value + "'. Expected syntax is: "
                                 + "'5 2' as to specify that 5-member rings "
                                 + "are given a weight of 2.";
                    throw new DENOPTIMException(msg);
                }
                int size = -1;
                int weight = 0;
                try
                {
                    size = Integer.parseInt(words[0]);
                } catch (Throwable t)
                {
                	throw new DENOPTIMException("Ring size must be an integer");
                }
                try
                {
                    weight = Integer.parseInt(words[1]);
                } catch (Throwable t)
                {
                	throw new DENOPTIMException("Bias weight must be an integer");
                }
                if (size < 0)
                {
                    msg = "Ring size must be >= 0";
                    throw new DENOPTIMException(msg);
                }
                if (weight < 0)
                {
                    msg = "Bias for ring size must be >= 0";
                    throw new DENOPTIMException(msg);
                }
                if (size > maxRingSize)
                {
                    for (int i=maxRingSize+1; i<size; i++)
                    {
                        ringSizeBias.add(0);
                    }
                    ringSizeBias.add(weight);
                    maxRingSize = size;
                }
                else
                {
                    ringSizeBias.set(size,weight);
                }
                break;
            case "CLOSABLERINGSMARTS=":
                int id = ringClosabCondAsSMARTS.size();
                ringClosabCondAsSMARTS.put("q"+id,value);
                break;
            case "REQUIREDELEMENTINRINGS=":
                reqElInRings.add(value);
                break;
            case "RCCINDEX=":
                rccIndex = value;
                break;
            case "RCCFOLDER=":
            	serializeRCCs = true;
                rccFolder = value;
                break;
            case "EXHAUSTIVECONFSEARCH":
            	exhaustiveConfSrch = true;
            	break;
            case "CHECKINTERDEPENDENTCHAINS":
            	checkInterdepPaths = true;
            	break;
            default:
                msg = "Keyword '" + key + "' is not a known ring closure-"
                        + "related keyword. Check input files.";
                throw new DENOPTIMException(msg);
        }
    }

//----------------------------------------------------------------------------

    public void checkParameters() throws DENOPTIMException
    {
        String msg = "";
    	if (!closeRings)
    	{
            msg = "The use of ring-closure related keywords "
        		  + "is dependent on the activation of the ring-closing "
        		  + "machinery (use " 
        		  + ParametersType.RC_PARAMS.getKeywordRoot() 
        		  + "CLOSERINGS keyword).";
            throw new DENOPTIMException(msg);
	    }

        if (rceMode > 2)
        {
            msg = "Unknown ring-closure evaluation mode '" + rceMode + "' "
		  + "Acceptable values are: "
          + "-1 (only ring size)"
		  + "0 (only contitutional), "
		  + "1 (only 3D chain), "
		  + "2 (both constitutional and 3D chain). ";
            throw new DENOPTIMException(msg);
        }

        if (rccFolder!="" && !FileUtils.checkExists(rccFolder))
        {
    	    File folder = new File(rccFolder);
    	    try
            {
                folder.mkdir();
            }
            catch (Throwable t2)
            {
                msg = "CyclicGraphHandler can't make folder " + rccFolder
                        + ". " + t2;
                throw new DENOPTIMException(msg);
            }
        }

        if (buildChelatesMode && !closeRings)
        {
            msg = "To use the BuildChelates mode you need to perform "
                  + "experiments capable of closing/opening and modifying "
                  + "rings. See the "
                  + ParametersType.RC_PARAMS.getKeywordRoot() 
                  + "CLOSERINGS keyword "
                  + "and provide the related input (i.e., ring "
                  + "closing attractor fragments).";
            throw new DENOPTIMException(msg);
        }

        if (minRcaPerType > maxRcaPerType)
        {
            msg = "Check values of maxRcaPerType and maxRcaPerType";
            throw new DENOPTIMException(msg);
        }

        if (minRingClosures > maxRingClosures)
        {
            msg = "Check values of minRingClosures and maxRingClosures";
            throw new DENOPTIMException(msg);
        }

    	if (checkInterdepPaths && !exhaustiveConfSrch)
    	{
    	    exhaustiveConfSrch = true;
    	}
    
    	if (exhaustiveConfSrch)
    	{
    	    msg = "Exhaustive conformational search has been turned ON. "
    		  + "This is a very time consuming task!";
            StaticLogger.appLogger.log(Level.WARNING,msg);
    	}
    
    	if (selectFragsFromCC)
    	{
            msg = "DENOPTIM can guide the selection of fragments to " 
    		  + "those leading to known closable chains. This "
    		  + "functionality is currently under development "
    		  + "and is fully operative only for rings "
    		  + "involving the scaffolds. ";
            StaticLogger.appLogger.log(Level.WARNING,msg);
    	}
    	checkOtherParameters();
    }

//----------------------------------------------------------------------------

    public void processParameters() throws DENOPTIMException
    {
    	rcArchive = new RingClosuresArchive(this);
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

}
