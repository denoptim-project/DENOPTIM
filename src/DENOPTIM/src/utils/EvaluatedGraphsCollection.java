package utils;

import java.util.ArrayList;
import java.util.logging.Level;

import logging.DENOPTIMLogger;
import exception.DENOPTIMException;
import molecule.DENOPTIMMolecule;
import utils.RandomUtils;
import org.apache.commons.math3.random.MersenneTwister;
import logging.DENOPTIMLogger;


/**
 * A collection of graphs with fitness
 * 
 * @author Marco Foscato
 */

public class EvaluatedGraphsCollection
{
    /**
     * Colection of DENOPTIM representations of
     * graphs with fitness.
     */
    private static ArrayList<DENOPTIMMolecule> allSpace = 
					      new ArrayList<DENOPTIMMolecule>();

    /**
     * List of previously visited flags
     */
    private static ArrayList<Boolean> alreadyUsed = null;

    /**
     * Random number generator
     */
    private static MersenneTwister rng = RandomUtils.getRNG();

    /**
     * Number of used graphs
     */
    private static int numUsed = 0;

    /**
     * Comment used to flag previously used graphs
     */
    private static String usedFlag = "ALREADY_USED";

//------------------------------------------------------------------------------

    /**
     * Append all entries to the collection
     * @param dmols the entities to append
     */

    public static void addAll(ArrayList<DENOPTIMMolecule> dmols)
    {
	allSpace.addAll(dmols);
    }

//------------------------------------------------------------------------------

    /**
     * Return a new graph with fitness. 
     * If no new graph is available returns null
     * @param verbosity sets the amoujnt of log
     * @return the new entity or null
     */

    public static DENOPTIMMolecule getRandomEntry(int verbosity) 
							throws DENOPTIMException
    {
	DENOPTIMMolecule newOne = null;

	int numAvail = allSpace.size();

        if (numUsed == numAvail)
        {
            throw new DENOPTIMException("ERROR! Not enough graphs in the "
                                           + "collection of evaluated graphs.");
        }
	while (numUsed < numAvail)
        {
	    int rndNum = rng.nextInt(numAvail);
	    String str = allSpace.get(rndNum).getComments();
	    if (str==null || (str!=null && !str.equals(usedFlag)))
	    {
		newOne = allSpace.get(rndNum);
		allSpace.get(rndNum).setComments(usedFlag);
		numUsed+=1;
	        if (verbosity > 0)
		{
		    StringBuilder sb = new StringBuilder();
		    sb.append(String.format("Selecting graph %d ",rndNum));
		    sb.append(newOne.getMoleculeUID());
		    sb.append(String.format(" (Usage %d/%d)",numUsed,numAvail));
		    DENOPTIMLogger.appLogger.log(Level.INFO,sb.toString());
		}
		break;
	    }
	}
	return newOne;
    }

//------------------------------------------------------------------------------

}
