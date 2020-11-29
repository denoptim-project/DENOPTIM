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

package denoptim.rings;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.io.File;
import java.util.logging.Level;

import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;


/**
 * Parameters and setting related to handling ring closures.
 *
 * @author Marco Foscato
 */

public class RingClosureParameters
{
    /**
     * Verbosity level for ring-closure related tasks
     */
    protected static int verbosity = 0;

    /**
     * Flag indicating that at least one RC-parameter has been defined
     */
    protected static boolean rcParamsInUse = false;

    /**
     * Flag activating the ring closing machinery
     */
    protected static boolean closeRings = false;

    /**
     * Flag activating procedures favouring formation of chelates
     */
//TODO can we avoid this flag?
    protected static boolean buildChelatesMode = false;

    /**
     * Flag activating the biased selection of closable fragment chains
     */
    protected static boolean selectFragsFromCC = false;

    /**
     * Controller for type of ring closing potential to be used in Tinker
     */
    protected static String rcStrategy = "BONDOVERLAP"; 

    /**
     * The ring closability evaluation mode:
     * -1= only ring rize bias
     * 0 = only constitution of candidate ring,
     * 1 = only closability of 3D chain,
     * 2 = both 0 and 1.
     */
    protected static int rceMode = -1;

    /**
     * Maximum number of rotatable bonds for which conformational space
     * is explored.
     */
    protected static int maxRotBonds = 9; //TODO [n+2] => benzene has n=6 so: 8

    /**
     * Maximum size (number of atoms) in ring closing chain. 
     * The shortest path is considered
     */
    protected static int maxRingSize = 9;

    /**
     * Minimum number of <code>RingClosingAttractor</code>s in a valid graph
     */
    protected static int minRcaPerType = 0;

    /**
     * Maximum number of <code>RingClosingAttractor</code>s in a valid graph 
     */
    protected static int maxRcaPerType = 50;

    /**
     * Minimum number of <code>RingClosure</code>s in a valid graph
     */
    protected static int minRingClosures = 0;

    /**
     * Maximum number of <code>RingClosure</code>s in a valid graph
     */
    protected static int maxRingClosures = 50;

    /**
     * Maximum value for non-flat bond angle (in degree)
     */
    protected static double linearityLimit = 178.5;

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
    protected static double rcTolDist = 0.33;

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
    protected static double pathConfSearchExtraTol = 1.1;

    /**
     * Maximum value of dot product between the normalized 
     * attachment point vectors at the head and tail of a candidate chain.
     * Note that perfect alignement implies a dot product of -1.0.
     */
    protected static double rcMaxDot = -0.75;

    /**
     * Torsion angle step for conformational scan of candidate closable chain
     */
    protected static double pathConfSearchStep = 12.0;

    /**
     * Relative weight of ring sizes to bias the selection of a ring 
     * combination among the various alternatives.
     */
    protected static ArrayList<Integer> ringSizeBias = new ArrayList<Integer>()
        {{
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
     * SMARTS defining the constitution-based ring closability condition
     */
    protected static Map<String,String> ringClosabCondAsSMARTS =
                                                 new HashMap<String,String>();

    /**
     * Required elements in closable chains
     */
    protected static Set<String> reqElInRings = new HashSet<String>();

    /**
     * Pathname of the text file containing the list of visited
     * <code>RingClosingConformation</code>s (i.e., index file).
     * The index file is machine-written and is bound to the 
     * corresponding libraries of fragments.
     */
    protected static String rccIndex = "";

    /**
     * Pathname of the root folder containing the archive of serialized
     * <code>RingClosingConformation</code>s
     */
    protected static String rccFolder = "";

    /**
     * Flag controlling conformational search. If <code>true</code> the
     * whole torsional space is scanned looking for ring closing
     * conformations. WARNING! This is very time consuming, but is needed 
     * for the current implementation of the iterdependent ring criterion.
     */
    protected static boolean exhaustiveConfSrch = false; 

    /**
     * Flag controlling the ring-closing criterion evaluating the 
     * simultaneous closability of interdependent chains.
     * If <code>true</code> requires the evaluation of interdependent chains.
     */
    protected static boolean checkInterdepPaths = false;

    /**
     * FLag controlling the serialization of the 
     * <code>RingClosingConformations</code>. This flag is activated by the
     * keyword providing the pathname of the root folder of the RCCs archive
     */
    protected static boolean serializeRCCs = false;

//------------------------------------------------------------------------------
    
    public static void resetParameters()
    {
    	verbosity = 0;
    	rcParamsInUse = false;
    	closeRings = false;
    	buildChelatesMode = false;
    	selectFragsFromCC = false;
    	rcStrategy = "BONDOVERLAP";
    	rceMode = -1;
    	maxRotBonds = 9; //TODO [n+2] => benzene has n=6 so: 8
    	maxRingSize = 9;
    	minRcaPerType = 0;
    	maxRcaPerType = 50;
    	minRingClosures = 0;
    	maxRingClosures = 50;
    	linearityLimit = 178.5;
    	rcTolDist = 0.33;
    	pathConfSearchExtraTol = 1.1;
    	rcMaxDot = -0.75;
    	pathConfSearchStep = 12.0;
    	ringSizeBias = new ArrayList<Integer>()
            {{
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
        ringClosabCondAsSMARTS = new HashMap<String,String>();
        reqElInRings = new HashSet<String>();
        rccIndex = "";
        rccFolder = "";
        exhaustiveConfSrch = false; 
        checkInterdepPaths = false;
        serializeRCCs = false;
    }

//----------------------------------------------------------------------------

    public static boolean rcParamsInUse()
    {
	return rcParamsInUse;
    }

//----------------------------------------------------------------------------

    public static String getRCStrategy()
    {
        return rcStrategy;
    }

//----------------------------------------------------------------------------

    public static int getClosabilityEvalMode()
    {
        return rceMode;
    }

//----------------------------------------------------------------------------

    public static int getMaxNumberRotatableBonds()
    {
        return maxRotBonds;
    }

//----------------------------------------------------------------------------

    public static int getMaxRingSize()
    {
        return maxRingSize;
    }

//----------------------------------------------------------------------------

    public static ArrayList<Integer> getRingSizeBias()
    {
        return ringSizeBias;
    }

//------------------------------------------------------------------------------

    public static int getMinRcaPerType()
    {
        return minRcaPerType;
    }

//------------------------------------------------------------------------------

    public static int getMaxRcaPerType()
    {
        return maxRcaPerType;
    }

//------------------------------------------------------------------------------

    public static int getMinRingClosures()
    {
        return minRingClosures;
    }

//------------------------------------------------------------------------------

    public static int getMaxRingClosures()
    {
        return maxRingClosures;
    }

//----------------------------------------------------------------------------

    public static Map<String,String> getConstitutionalClosabilityConds()
    {
        return ringClosabCondAsSMARTS;
    }

//----------------------------------------------------------------------------

    public static int getVerbosity()
    {
        return verbosity;
    }

//-----------------------------------------------------------------------------

    public static boolean allowRingClosures()
    {
        return closeRings;
    }

//-----------------------------------------------------------------------------

    public static boolean buildChelatesMode()
    {
        return buildChelatesMode;
    }

//----------------------------------------------------------------------------

    public static boolean selectFragmentsFromClosableChains()
    {
	return selectFragsFromCC;
    }

//----------------------------------------------------------------------------

    public static double getRCDistTolerance()
    {
        return rcTolDist;
    }

//----------------------------------------------------------------------------

    public static double getConfPathExtraTolerance()
    {
        return pathConfSearchExtraTol;
    }

//----------------------------------------------------------------------------

    public static double getRCDotPrTolerance()
    {
        return rcMaxDot;
    }

//----------------------------------------------------------------------------

    public static double getLinearityLimit()
    {
        return linearityLimit;
    }

//----------------------------------------------------------------------------

    public static double getPathConfSearchStep()
    {
        return pathConfSearchStep;
    }

//----------------------------------------------------------------------------

    public static Set<String> getRequiredRingElements()
    {
        return reqElInRings;
    }

//----------------------------------------------------------------------------

    public static String getRCCLibraryIndexFile()
    {
        return rccIndex;
    }

//----------------------------------------------------------------------------

    public static String getRCCLibraryFolder()
    {
        return rccFolder;
    }

//----------------------------------------------------------------------------

    public static boolean doExhaustiveConfSrch()
    {
	return exhaustiveConfSrch;
    }

//----------------------------------------------------------------------------

    public static boolean serializeRCCs()
    {
	return serializeRCCs;
    }

//----------------------------------------------------------------------------

    public static boolean checkInterdependentChains()
    {
	return checkInterdepPaths;
    }

//----------------------------------------------------------------------------

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

//----------------------------------------------------------------------------

    public static void interpretKeyword(String key, String value)
                                                      throws DENOPTIMException
    {
	rcParamsInUse = true;
	String msg = "";
        switch (key.toUpperCase())
        {
        case "RC-VERBOSITY=":
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
        case "RC-CLOSERINGS":
            closeRings = true;
            break;
        case "RC-CLOSERINGS=":
            closeRings = true;
            break;
        case "RC-BUILDCHELATESMODE":
            buildChelatesMode = true;
            break;
        case "RC-SELECTFRAGMENTSFROMCLOSABLECHAINS":
	    selectFragsFromCC = true;
	    break;
        case "RC-RCPOTENTIALTYPE=":
            try
            {
                rcStrategy = value.toUpperCase();
            }
            catch (Throwable t)
            {
                msg = "Unable to understand value '" + value + "'";
                throw new DENOPTIMException(msg);
            }
            break;
        case "RC-EVALUATIONCLOSABILITYMODE=":
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
        case "RC-MAXROTBONDS=":
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
        case "RC-MAXSIZENEWRINGS=":
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
        case "RC-MINRCAPERTYPEPERGRAPH=":
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
        case "RC-MAXRCAPERTYPEPERGRAPH=":
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
        case "RC-MINNUMBEROFRINGCLOSURES=":
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
        case "RC-MAXNUMBERRINGCLOSURES=":
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
        case "RC-LINEARITYLIMIT=":
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
        case "RC-DISTANCETOLERANCEFACTOR=":
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
        case "RC-EXTRADISTANCETOLERANCEFACTOR=":
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
        case "RC-MAXDOTPROD=":
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
        case "RC-CONFSEARCHSTEP=":
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
        case "RC-RINGSIZEBIAS=":
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
        case "RC-CLOSABLERINGSMARTS=":
            int id = ringClosabCondAsSMARTS.size();
            ringClosabCondAsSMARTS.put("q"+id,value);
            break;
        case "RC-REQUIREDELEMENTINRINGS=":
            reqElInRings.add(value);
            break;
        case "RC-RCCINDEX=":
            rccIndex = value;
            break;
        case "RC-RCCFOLDER=":
	    serializeRCCs = true;
            rccFolder = value;
            break;
        case "RC-EXHAUSTIVECONFSEARCH":
	    exhaustiveConfSrch = true;
	    break;
        case "RC-CHECKINTERDEPENDENTCHAINS":
	    checkInterdepPaths = true;
	    break;
        default:
            msg = "Keyword '" + key + "' is not a known ring closure-"
                                      + "related keyword. Check input files.";
            throw new DENOPTIMException(msg);
        }
    }

//----------------------------------------------------------------------------

    public static void checkParameters() throws DENOPTIMException
    {
        String msg = "";
        if (!rcParamsInUse)
        {
            return;
        }

	if (!closeRings && rcParamsInUse)
	{
            msg = "The use of ring-closure related keywords "
		  + "is dependent on the activation of the ring-closing "
		  + "machinery (use RC-CLOSERINGS keyword).";
            throw new DENOPTIMException(msg);
	}

        if (!DenoptimIO.checkExists(rccIndex))
        {
            msg = "Index of the RCC archive: " + rccIndex
                  + " not found: making a new index file.";
            DENOPTIMLogger.appLogger.info(msg);
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

        if (rccFolder!="" && !DenoptimIO.checkExists(rccFolder))
        {
            msg = "Root folder for serialized RingClosingConformation"
		  + " not found. Creating new archive at " + rccFolder;
            DENOPTIMLogger.appLogger.info(msg);

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
                  + "rings. See the RC-CLOSERINGS keyword "
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
            msg = "Evaluation of the simultaneus ring closability "
		  + "condition requires exhaustive conformational search. "
		  + "Setting exhaustiveConfSrch=true.";
            DENOPTIMLogger.appLogger.info(msg);
	    exhaustiveConfSrch = true;
	}

	if (exhaustiveConfSrch)
	{
	    msg = "Exhaustive conformational search has been turned ON. "
		  + "This is a time consuming task! Make sure it's what "
		  + "you want to do.";
            DENOPTIMLogger.appLogger.log(Level.WARNING,msg);
	}

//TODO: update when development is over
	if (selectFragsFromCC)
	{
            msg = "DENOPTIM can guide the selection of fragments to " 
		  + "those leading to known closable chains. This "
		  + "functionality is currently under development "
		  + "and is fully operative only for rings "
		  + "involving the scaffolds. ";
            DENOPTIMLogger.appLogger.log(Level.WARNING,msg);
	}
    }

//----------------------------------------------------------------------------

    public static void processParameters() throws DENOPTIMException
    {
	RingClosuresArchive rcl = new RingClosuresArchive(rccIndex);
    }

//----------------------------------------------------------------------------

    public static void printParameters()
    {
	if (!rcParamsInUse)
	{
	    return;
	}
        String eol = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder(1024);
        sb.append(" RingClosureParameters ").append(eol);
        for (Field f : RingClosureParameters.class.getDeclaredFields())
        {
            try
            {
                sb.append(f.getName()).append(" = ").append(
                              f.get(RingClosureParameters.class)).append(eol);
            }
            catch (Throwable t)
            {
                sb.append("ERROR! Unable to print RingClosureParameters.");
                break;
            }
        }
        DENOPTIMLogger.appLogger.info(sb.toString());
        sb.setLength(0);
    }

//------------------------------------------------------------------------------

}
