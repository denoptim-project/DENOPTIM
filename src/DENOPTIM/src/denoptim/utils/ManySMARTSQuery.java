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

package denoptim.utils;

import java.util.Map;
import java.util.HashMap;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.isomorphism.Mappings;
import org.openscience.cdk.isomorphism.Pattern;
import org.openscience.cdk.smarts.SmartsPattern;


/**
 * Container of lists of atoms matching a list of SMARTS.
 *
 * @author Marco Foscato 
 */

public class ManySMARTSQuery
{
    //Container for all matches
    private Map<String,Mappings> allMatches = new HashMap<>();

    //Counts for all matches
    private Map<String,Integer> numMatches = new HashMap<>();

    //Utils for detecting problems
    private Throwable problem;
    private boolean problems = false;
    private String message = "";

//------------------------------------------------------------------------------

    public ManySMARTSQuery(IAtomContainer mol, Map<String, String> smarts) {
        String err="";
        try {
            for (String smartsRef : smarts.keySet())
            {
                //get the new query
                String oneSmarts = smarts.get(smartsRef);
                err = smartsRef;
                
                Pattern sp = SmartsPattern.create(oneSmarts);

                // WARNING: assumptions on implicit H count and bond orders!
                DENOPTIMMoleculeUtils.setZeroImplicitHydrogensToAllAtoms(mol);
                DENOPTIMMoleculeUtils.ensureNoUnsetBondOrders(mol);
                
                if (sp.matches(mol))
                {
                    Mappings listOfIds = sp.matchAll(mol);
                    allMatches.put(smartsRef,listOfIds);
                    numMatches.put(smartsRef,listOfIds.count());
                }
            }
        } catch (Throwable t) {
            java.lang.StackTraceElement[] stes = t.getStackTrace();
            String cause = "";
            int s = stes.length;
            if (s >= 1) {
                java.lang.StackTraceElement ste = stes[0];
                cause = ste.getClassName();
            } else {
                cause = "'unknown' (try to process this molecule alone to "
                        + "get more info)";
            }
            err = "WARNING! For query " + err + " => Exception returned "
                    + "by " + cause;
            problems = true;
            problem = t;
            message = err;
        }
    }

//------------------------------------------------------------------------------

    public boolean hasProblems()
    {
        return problems;
    }

//------------------------------------------------------------------------------

    public Throwable getProblem()
    {
        return problem;
    }

//------------------------------------------------------------------------------

    public String getMessage()
    {
        return message;
    }
    
//------------------------------------------------------------------------------
    
    public Map<String, Mappings> getAllMatches()
    {
    	return allMatches;
    }

//------------------------------------------------------------------------------

    public int getNumMatchesOfQuery(String query)
    {
        return numMatches.getOrDefault(query, 0);
    }

//------------------------------------------------------------------------------

    public Mappings getMatchesOfSMARTS(String ref)
    {
        return allMatches.get(ref);
    }

//------------------------------------------------------------------------------

}
