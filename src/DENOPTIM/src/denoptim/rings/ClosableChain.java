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

import java.util.ArrayList;
import java.io.Serializable;

import denoptim.molecule.DENOPTIMVertex;

/**
 * ClosableChain represents a chain of fragments (chain links) that
 * is closable (or candidate closable).
 *
 * @author Marco Foscato 
 */

public class ClosableChain implements Cloneable, Serializable
{
    /**
     * List of <code>ChainLink</code>s in this chain
     */
    private ArrayList<ChainLink> links;

    /**
     * The position of the scaffold vertex: 
     * turning point for the direction of the chain
     */
    private int tuningPoint;

//-----------------------------------------------------------------------------

    /**
     *  Constructs an empty ClosableChain
     */

    public ClosableChain()
    {
	this.links = new ArrayList<ChainLink>();
    }

//-----------------------------------------------------------------------------

    /**
     * Constructs a ClosableChain from the string representation
     */

    public ClosableChain(String str)
    {
	links = new ArrayList<ChainLink>();

	String[] parts = str.trim().split("_");
	int lastFType = Integer.MAX_VALUE;
	for (int i=0; i<parts.length; i++)
	{
	    String clStr = parts[i];
            String[] clParts = clStr.trim().split("/");
            int molID = Integer.parseInt(clParts[0]);
            int ftype = Integer.parseInt(clParts[1]);
            String[] partsAps = clParts[2].split("ap");
            int apLeft = Integer.parseInt(partsAps[1]);
	    int apRight = Integer.parseInt(partsAps[2]);
	    if (ftype < lastFType)
	    {
		tuningPoint = i;
		lastFType = ftype;
	    }
	    ChainLink cl = new ChainLink(molID, ftype, apLeft, apRight);
	    links.add(cl);
	}
    }

//----------------------------------------------------------------------------

    /**
     * Get the list of <code>ChainLink</code>s
     */

    public ArrayList<ChainLink> getLinks()
    {
	return links;
    }

//-----------------------------------------------------------------------------

    /**
     * Get a specific <code>ChainLink</code>
     * @param i the index of the <code>ChainLink</code> in the list
     * @return the <code>ChainLink</code> in the given position of the chain
     */

    public ChainLink getLink(int i)
    {
	return links.get(i);
    }

//-----------------------------------------------------------------------------

    /**
     * Get length of chain
     * @return the size of the list if <code>ChainLink</code>s
     */

    public int getSize()
    {
	return links.size();
    }

//-----------------------------------------------------------------------------

    /**
     * Get index of the turning point. Note that since the chain is a 
     * path in a graph the relative direction of the
     * edges spanned can display an inversion point (i.e., the turning point).
     * @return the index of the <code>ChainLink</code> at which the direction
     * of the edges gets inverted.
     */

    public int getTurningPoint()
    {
	return tuningPoint;
    }

//-----------------------------------------------------------------------------

    /**
     * Get the vertex ID of the turning point.Note that since the chain is a 
     * path in a graph the relative direction of the
     * edges spanned can display an inversion point (i.e., the turning point).
     * @return the ID of the <code>DENOPTIMVertex</code> corresponding to the
     * turning point.
     */

    public int getTurningPointMolID()
    {
	return getLink(tuningPoint).getMolID();
    }

//-----------------------------------------------------------------------------

    /**
     * Check whether a given vertex is involved in this chain.
     * @param vert the candidate vertex
     * @return the poisiton of the related chain link or -1 if the vertex is
     * not part of this chain.
     */

    public int involvesVertex(DENOPTIMVertex vert)
    {
        int result = -1;
        int vertMolID = vert.getMolId();
        int vertFrgTyp = vert.getFragmentType();
        for (int i=0; i<links.size(); i++)
        {
            ChainLink cl = links.get(i);
            if (cl.getMolID() == vertMolID &&
                cl.getFragType() == vertFrgTyp)
            {
                result = i;
                break;
            }
        }
        return result;
    }

//-----------------------------------------------------------------------------

    /**
     * Check whether a combination of vertex and attachment points ID
     * is involved in this chain.
     * @param vert the candidate vertex
     * @param apIDA the index of the <code>DENOPTIMAttachmentPoint</code> on
     * the left/rigth hand side of the vertex
     * @param apIDB the index of the <code>DENOPTIMAttachmentPoint</code> on
     * the right/left hand side of the vertex
     * @return the poisiton of the related chain link or -1 if the vertex is
     * not part of this chain.
     */

    public int involvesVertexAndAP(DENOPTIMVertex vert, int apIDA, int apIDB)
    {
	int result = -1;
	int vertMolID = vert.getMolId();
	int vertFrgTyp = vert.getFragmentType();
	for (int i=0; i<links.size(); i++)
	{
	    ChainLink cl = links.get(i);
	    if (cl.getMolID() == vertMolID && 
		cl.getFragType() == vertFrgTyp &&
		((cl.getApIdToLeft()==apIDA && cl.getApIdToRight()==apIDB) || 
		 (cl.getApIdToLeft()==apIDB && cl.getApIdToRight()==apIDA)))
	    {
		result = i;
		break;
	    }
	}
        return result;
    }

//-----------------------------------------------------------------------------

    /**
     * @return the string representation of this ClosableChain
     */

    public String toString()
    {
	String str = " ClosableChain[";
	for (ChainLink cc : links)
	{
	    str = str + cc;
	}
	str = str + "]";
	return str;
    }

//-----------------------------------------------------------------------------
}

