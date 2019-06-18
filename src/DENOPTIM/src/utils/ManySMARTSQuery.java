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

package utils;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.smarts.SMARTSQueryTool;
import org.openscience.cdk.exception.CDKException;


/**
 * Container of lists of atoms matching a list of SMARTS.
 *
 * @author Marco Foscato 
 */

public class ManySMARTSQuery
{
    //Container for all matches
    private Map<String,List<List<Integer>>> allMatches = new HashMap<>();

    //Counts for all matches
    private Map<String,Integer> numMatches = new HashMap<>();

    //Utils for detecting problems
    private boolean problems = false;
    private String message = "";


//------------------------------------------------------------------------------

    public ManySMARTSQuery()
    {
	super();
    }

//------------------------------------------------------------------------------

    public ManySMARTSQuery(IAtomContainer mol, Map<String, String> smarts)
    {
        super();
	String blankSmarts = "[*]";
	String err="";
	try {
                SMARTSQueryTool query = new SMARTSQueryTool(blankSmarts);
		for (String smartsRef : smarts.keySet())
		{
		    //get the new query
                    String oneSmarts = smarts.get(smartsRef);
		    err = smartsRef;

                    //Update the query tool
		    query.setSmarts(oneSmarts);
		    
		    if (query.matches(mol))
		    {
			//Store matches
			List<List<Integer>> listOfIds = new ArrayList<List<Integer>>();
			listOfIds = query.getUniqueMatchingAtoms();
			allMatches.put(smartsRef,listOfIds);
			//Store number
			int num = listOfIds.size();
			numMatches.put(smartsRef,num);
 		    }
		}
        } catch (CDKException cdkEx) 
        {
            String cause = cdkEx.getCause().getMessage();
            err = "\nWARNING! For query " + err + " => " + cause;
            problems = true;
            message = err;
	} 
        catch (Throwable t) 
        {
		java.lang.StackTraceElement[] stes = t.getStackTrace();
		String cause = "";
		int s = stes.length;
		if (s >= 1)
		{
		    java.lang.StackTraceElement ste = stes[0];
		    cause = ste.getClassName();
		} 
                else 
                {
		    cause = "'unknown' (try to process this molecule alone to "
							    + "get more infos)";
		}
                err = "\nWARNING! For query " + err + " => Exception returned "
								  + "by "+cause;
                problems = true;
                message = err;
	}
    }

//------------------------------------------------------------------------------

    public boolean hasProblems()
    {
        return problems;
    }

//------------------------------------------------------------------------------

    public String getMessage()
    {
	return message;
    }

//------------------------------------------------------------------------------

    public int getNumMatchesOfQuery(String query)
    {
	if (numMatches.keySet().contains(query))
	    return numMatches.get(query);
	else
	    return 0;
    }

//------------------------------------------------------------------------------

    public List<List<Integer>> getMatchesOfSMARTS(String ref)
    {
        return allMatches.get(ref);
    }

//------------------------------------------------------------------------------

}
