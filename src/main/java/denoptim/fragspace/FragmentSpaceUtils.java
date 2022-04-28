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

import java.util.List;
import java.util.Map;

import denoptim.graph.APMapping;
import denoptim.graph.AttachmentPoint;


/**
 * Utility class for the fragment space
 * 
 * @author Marco Foscato
 */
public class FragmentSpaceUtils
{

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
    public static boolean recursiveCombiner(List<AttachmentPoint> keys,
            int currentKey, Map<AttachmentPoint,
                List<AttachmentPoint>> possibilities,
            APMapping combination, List<APMapping> completeCombinations, 
            boolean screenAll, int maxCombs)
    {
        boolean stopped = false;
        AttachmentPoint apA = keys.get(currentKey);
        for (int i=0; i<possibilities.get(apA).size(); i++)
        {
            // Prevent combinatorial explosion.
            if (stopped)
                break;
            
            AttachmentPoint apB = possibilities.get(apA).get(i);
            
            // Move on if apB is already used by another pairing
            if (combination.containsValue(apB))
                continue;
    
            // add this pairing to the growing combinations
            APMapping priorCombination = combination.clone();
            if (apA != null && apB != null)
            {
                combination.put(apA,apB);
            }
            // NB: when one is null it means that the other will not be listed
            // in the mapping. Therefore, it will be left out of any operation
            // that uses the mapping. For example, the AP for which the is a 
            // 'null' possibility will be left free.
            
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
