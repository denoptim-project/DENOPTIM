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

import java.io.Serializable;

import denoptim.molecule.DENOPTIMFragment.BBType;


/**
 * Data structure containing information that identifies a single AP of 
 * a vertex/fragment.
 *
 * @author Marco Foscato
 */

public class IdFragmentAndAP implements Serializable
{
    /**
     * Vedrsion UID
     */
    private static final long serialVersionUID = -863201756763649026L;

    /**
     * the ID of the vertex containing the fragment.
     */
    private int vId = -1;

    /**
     * the index of the fragment in that library. 
     */
    private int molId = -1;

    /**
     * the type of library containing the fragment. 
     */
    private BBType molTyp = BBType.UNDEFINED;

    /**
     * the index of a specific attachment point.
     */
    private int apId = -1;

    /**
     * the index of the symmetric set the vertex belongs to.
     */
    private int vSymSetId = -1;

    /**
     * the index of the symmetric set the AP belongs to.
     */
    private int aSymSetId = -1;

//------------------------------------------------------------------------------

    public IdFragmentAndAP()
    {
    }

//------------------------------------------------------------------------------

    public IdFragmentAndAP(int m_vId, int m_molId, BBType vMolTyp, int m_apId, 
            int m_vSymSetId, int m_aSymSetId)
    {
        vId = m_vId;
        molId = m_molId;
        molTyp = vMolTyp;
        apId = m_apId;
    	vSymSetId = m_vSymSetId;
    	aSymSetId = m_aSymSetId;
    }

//------------------------------------------------------------------------------

    public int getVertexId()
    {
        return vId;
    }

//------------------------------------------------------------------------------

    public int getVertexMolId()
    {
        return molId;
    }

//------------------------------------------------------------------------------

    public BBType getVertexMolType()
    {
        return molTyp;
    }

//------------------------------------------------------------------------------

    public int getApId()
    {
        return apId;
    }

//------------------------------------------------------------------------------

    public int getVrtSymSetId()
    {
        return vSymSetId;
    }

//------------------------------------------------------------------------------

    public int getApSymSetId()
    {
        return aSymSetId;
    }

//------------------------------------------------------------------------------

    public void setVrtSymSetId(int m_vSymSetId)
    {
        vSymSetId = m_vSymSetId;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Compares this and another identifier considering solely the fragment ID
     * in the type-specific fragment library, and the attachment point ID in that
     * fragment.
     * @param other the other identified to be compared with this.
     * @return <code>true</code> if this and the other identify the same fragment 
     * and AP ID.
     */
    public boolean sameFragAndAp(IdFragmentAndAP other)
    {
    	if (this.getApId()==other.getApId() 
    			&& this.getVertexMolId()==other.getVertexMolId()
    			&& this.getVertexMolType()==other.getVertexMolType())
    	{
    		return true;
    	}
    	return false;
    }

//------------------------------------------------------------------------------

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("IdFragmentAndAP [vId=").append(vId);
    	sb.append(", molId=").append(molId);
    	sb.append(", molTyp=").append(molTyp);
    	sb.append(", apId=").append(apId);
    	sb.append(", vSymSetId=").append(vSymSetId);
    	sb.append(", aSymSetId=").append(aSymSetId).append("]");
    	return sb.toString();
    }

//------------------------------------------------------------------------------

}
