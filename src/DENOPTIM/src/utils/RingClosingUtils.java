package utils;


import java.util.Set;
import java.util.ArrayList;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import constants.DENOPTIMConstants;
import rings.RingClosingAttractor;
import molecule.DENOPTIMGraph;
import molecule.DENOPTIMVertex;
import molecule.DENOPTIMAttachmentPoint;
import molecule.DENOPTIMRing;


/**
 * Toolbox useful when dealing with Ring Closing Attractors and ring closures.
 *
 * @author Marco Foscato (University of Bergen)
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
