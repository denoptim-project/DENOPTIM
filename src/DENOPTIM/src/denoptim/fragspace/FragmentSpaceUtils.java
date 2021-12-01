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

package denoptim.fragspace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import denoptim.exception.DENOPTIMException;
import denoptim.molecule.APClass;
import denoptim.molecule.APMapping;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.utils.RandomUtils;


/**
 * Utility class for the fragment space
 * 
 * @author Marco Foscato
 */
public class FragmentSpaceUtils
{
	
//------------------------------------------------------------------------------
	
	/**
	 * Performs grouping and classification operations on the fragment library
	 * @param apClassBasedApproch <code>true</code> if you are using class based
	 * approach
	 */
	public static void groupAndClassifyFragments(boolean apClassBasedApproch)
	 throws DENOPTIMException
	{	
		FragmentSpace.setFragPoolPerNumAP(
					     new HashMap<Integer,ArrayList<Integer>>());
		if (apClassBasedApproch)
		{
		    FragmentSpace.setFragsApsPerApClass(
				   new HashMap<APClass,ArrayList<ArrayList<Integer>>>());
		    FragmentSpace.setAPClassesPerFrag(
					      new HashMap<Integer,ArrayList<APClass>>());
		}
		for (int j=0; j<FragmentSpace.getFragmentLibrary().size(); j++)
		{
			DENOPTIMVertex frag = FragmentSpace.getFragmentLibrary().get(j);
		    classifyFragment(frag,j);
		}
	}

//------------------------------------------------------------------------------

    /**
     * Classify a fragment in terms of the number of APs and possibly their 
     * type (AP-Class).
     * @param frg the building block to classify
     * @param id the index of the fragment in the library
     * @throws DENOPTIMException
     */

    static void classifyFragment(DENOPTIMVertex frg,int fragId)
    {   
		// Classify according to number of APs
        int nAps = frg.getFreeAPCount();
		if (nAps != 0)
		{
            if (FragmentSpace.getMapOfFragsPerNumAps().containsKey(nAps))
            {
                FragmentSpace.getFragsWithNumAps(nAps).add(fragId);
            }
            else
            {
                ArrayList<Integer> lst = new ArrayList<>();
                lst.add(fragId);
                FragmentSpace.getMapOfFragsPerNumAps().put(nAps,lst);
            }
		}
	
		if (FragmentSpace.useAPclassBasedApproach())
		{
		    // Collect classes per fragment
		    ArrayList<APClass> lstAPC = frg.getAllAPClasses();
	        FragmentSpace.getMapAPClassesPerFragment().put(fragId,lstAPC);
	        
		    // Classify according to AP-Classes
	        ArrayList<DENOPTIMAttachmentPoint> lstAPs = 
	                frg.getAttachmentPoints();
	        
		    for (int j=0; j<lstAPs.size(); j++)
		    {
		        DENOPTIMAttachmentPoint ap = lstAPs.get(j);
				ArrayList<Integer> apId = new ArrayList<Integer>();
				apId.add(fragId);
				apId.add(j);
				APClass cls = ap.getAPClass();
				
				if (!ap.isAvailable())
				{
				    continue;
				}
				
			    if (FragmentSpace.getMapFragsAPsPerAPClass().containsKey(cls))
				{
				    FragmentSpace.getMapFragsAPsPerAPClass().get(cls)
				    .add(apId);
				}
				else
				{
				    ArrayList<ArrayList<Integer>> outLst = 
							    new ArrayList<ArrayList<Integer>>();
				    outLst.add(apId);
				    FragmentSpace.getMapFragsAPsPerAPClass().put(cls,outLst);
				}
		    }
		    
		    if (frg.isRCV())
		        FragmentSpace.registerRCV(frg);
		}
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Given two lists of APs this method maps the APClass-compatibilities 
     * from between the two lists considering the APs in the first list the 
     * for the role of source AP in the hypothetical edge.
     * @param listA list of candidate source APs
     * @param listB list of candidate target APs
     * @param maxCombinations a maximum limit; if reached we are happy we give
     * up finding more combination.
     * @return 
     */
    public static List<APMapping> mapAPClassCompatibilities(
            List<DENOPTIMAttachmentPoint> listA, 
            List<DENOPTIMAttachmentPoint> listB, int maxCombinations)
    {
        Map<DENOPTIMAttachmentPoint,List<DENOPTIMAttachmentPoint>> apCompatilities =
                new HashMap<DENOPTIMAttachmentPoint,List<DENOPTIMAttachmentPoint>>();
        
        for (DENOPTIMAttachmentPoint apA : listA)
        {
            for (DENOPTIMAttachmentPoint apB : listB)
            {  
                boolean compatible = false;
                if (FragmentSpace.useAPclassBasedApproach())
                {   
                    if (apA.getAPClass().isCPMapCompatibleWith(apB.getAPClass()))
                    {
                        compatible = true;
                    }
                } else {
                    compatible = true;
                }
                if (compatible)
                {
                    if (apCompatilities.containsKey(apA))
                    {
                        apCompatilities.get(apA).add(apB);
                    } else {
                        List<DENOPTIMAttachmentPoint> lst = 
                                new ArrayList<DENOPTIMAttachmentPoint>();
                        lst.add(apB);
                        apCompatilities.put(apA,lst);
                    }
                }
            }
        }
        
        // This is used only to keep a sorted list of the map keys
        List<DENOPTIMAttachmentPoint> keys = 
                new ArrayList<DENOPTIMAttachmentPoint>(
                        apCompatilities.keySet());
        
        // Get all possible combinations of compatible AP pairs
        List<APMapping> apMappings = new ArrayList<APMapping>();
        if (keys.size() > 0)
        {
            int currentKey = 0;
            APMapping currentMapping = new APMapping();
            Boolean stopped = recursiveCombiner(keys, currentKey, 
                    apCompatilities, currentMapping, apMappings, true, 
                    maxCombinations);
        }
        
        return apMappings;
    }
    

//------------------------------------------------------------------------------
      
    /**
     * Search for all possible combinations of compatible APs. 
     * @param keys sorted list of keys in the 'possibilities' argument.
     * @param currentKey index of the currently active key in 'keys'
     * @param possibilities the mapping of all APs from listB that are
     * compatible with a specific AP from list A.
     * @param combination the combination currently under construction.
     * @param completeCombinations the storage of all valid combination; this
     * is effectively the output of this method.
     * @param screenAll flag requiring comprehensive exploration of all 
     * combinations
     * @param maxCombs maximum number of combinations to consider. If this 
     * number is reached then we stop searching for more.
     * @return a flag indicating whether execution was stopper or not.
     */
    public static boolean recursiveCombiner(List<DENOPTIMAttachmentPoint> keys,
            int currentKey, Map<DENOPTIMAttachmentPoint,
                List<DENOPTIMAttachmentPoint>> possibilities,
            APMapping combination, List<APMapping> completeCombinations, 
            boolean screenAll, int maxCombs)
    {
        boolean stopped = false;
        DENOPTIMAttachmentPoint apA = keys.get(currentKey);
        for (int i=0; i<possibilities.get(apA).size(); i++)
        {
            // Prevent combinatorial explosion.
            if (stopped)
                break;
            
            DENOPTIMAttachmentPoint apB = possibilities.get(apA).get(i);
            
            // Move on if apB is already used by another pairing
            if (combination.containsValue(apB))
                continue;
    
            // add this pairing to the growing combinations
            APMapping priorCombination = combination.clone();
            if (apA != null && apB != null)
            {
                combination.put(apA,apB);
            }
            
            // go deeper, to the next key
            if (currentKey+1 < keys.size())
            {
                stopped = recursiveCombiner(keys, currentKey+1, possibilities, 
                        combination, completeCombinations, screenAll, maxCombs);
            }
            
            // we reached the deepest level: save combination
            if (currentKey+1 == keys.size() && !combination.isEmpty())
            {   
                APMapping storable = combination.clone(); //Shallow clone
                completeCombinations.add(storable);
                if (!screenAll && completeCombinations.size() >= maxCombs)
                {
                    stopped = true;
                }
            }
            
            // Restart building a new combination from the previous combination
            combination = priorCombination;
        }
        return stopped;
    }

//------------------------------------------------------------------------------

}
