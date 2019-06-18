/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no>
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

/**
 * For affinity fingerprint clustering
 * @author Vishwesh Venkatraman
 */
public class DENOPTIMAFPair implements Comparable<DENOPTIMAFPair>
{
    private int index;
    private double affinity;
    
//------------------------------------------------------------------------------    
    
    public DENOPTIMAFPair(int m_idx, double m_affinity)
    {
        index = m_idx;
        affinity = m_affinity;
    }
    
//------------------------------------------------------------------------------    
    
    public int getIndex()
    {
        return index;        
    }
    
//------------------------------------------------------------------------------    
    
    public double getAffinity()
    {
        return affinity;
    }
    
//------------------------------------------------------------------------------    
    
    @Override
    public String toString() 
    {
        return index + " " + affinity;
    }
    
//------------------------------------------------------------------------------    
    
    @Override
    public int compareTo(DENOPTIMAFPair B)
    {
        if (this.affinity > B.affinity)
            return 1;
        else if (this.affinity < B.affinity)
            return -1;
        return 0;
    }
    
//------------------------------------------------------------------------------    
    
}
