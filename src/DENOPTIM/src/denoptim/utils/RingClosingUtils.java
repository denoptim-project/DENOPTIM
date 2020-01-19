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


import java.util.Set;
import java.util.ArrayList;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMRing;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.rings.RingClosingAttractor;


/**
 * Toolbox useful when dealing with Ring Closing Attractors and ring closures.
 *
 * @author Marco Foscato 
 */

public class RingClosingUtils
{

    /**
     * Verbosity level
     */

    private static int verbosity = 0;

//------------------------------------------------------------------------------    

    /** 
     * Looks for pseudoatoms representing the possibility of closing a ring:
     * the so-called <code>RingClosingAttractor</code>s.
     *
     * @param mol the molecular system to analyze
     * @return the list of pseudoatoms as <code>RingClosingAttractor</code>s
     */

    public static ArrayList<RingClosingAttractor> findAttractors(IAtomContainer mol)
    {
        ArrayList<RingClosingAttractor> attractors =
                                         new ArrayList<RingClosingAttractor>();

        for (IAtom atm : mol.atoms())
        {
            RingClosingAttractor rca = new RingClosingAttractor(atm,mol);
            if (rca.isAttractor())
                attractors.add(rca);
        }

        return attractors;
    }

//------------------------------------------------------------------------------

    /**
     * Compares two combinations of <code>DENOPTIMRings</code>s and evaluates
     * whether these correspond to the same combination
     */
    public static boolean areSameRingsSet(Set<DENOPTIMRing> sA,
							   Set<DENOPTIMRing> sB)
    {
	// Compare size
	if (sA.size() != sB.size())
	{
	    return false;
	}

	// Compare individual rings
	int checkedRings = 0;
	for (DENOPTIMRing rA : sA)
	{
	    DENOPTIMVertex headA = rA.getHeadVertex();
	    DENOPTIMVertex tailA = rA.getTailVertex();
	    for (DENOPTIMRing rB : sB)
	    {
		if (rA.getSize() != rB.getSize())
		{
		    continue;
		}
		if (rB.contains(headA))
		{
                    DENOPTIMVertex headB = rB.getHeadVertex();
                    DENOPTIMVertex tailB = rB.getTailVertex();
		    if ((headA == headB && tailA != tailB) ||
			(headA == tailB && tailA != headB))
		    {
			return false;
		    }
		    else if ((headA == headB && tailA == tailB) ||
                             (headA == tailB && tailA == headB)) 
		    {
			checkedRings++;
		    }
		}
	    }
	}
	if (sA.size() != checkedRings)
	{
	    return false;
	}
	return true;
    }

//------------------------------------------------------------------------------    
}
