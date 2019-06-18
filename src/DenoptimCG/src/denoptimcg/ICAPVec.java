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

package denoptimcg;

/**
 *
 * @author Vishwesh Venkatraman
 * This data structure stores the connected atoms and the corresponding attachment 
 * point indices
 */
public class ICAPVec 
{
    private int icIdA; // the position of atomA in the IC list
    private int icIdB; // the position of atomB in the IC list
    private int vecIdxA; // the index of the AP vector associated with A
    private int vecIdxB; // the index of the AP vector associated with B
    
//------------------------------------------------------------------------------    
    
    public ICAPVec()
    {
        
    }
    
//------------------------------------------------------------------------------
    
    public ICAPVec(int m_icIdA, int m_icIdB, int m_vecIdxA, int m_vecIdxB)
    {
        icIdA = m_icIdA;
        icIdB = m_icIdB;
        vecIdxA = m_vecIdxA;
        vecIdxB = m_vecIdxB;
    }
    
//------------------------------------------------------------------------------    
    
    protected int getFirstAP()
    {
        return vecIdxA;
    }
    
//------------------------------------------------------------------------------        
    
    protected int getSecondAP()
    {
        return vecIdxB;
    }
    
//------------------------------------------------------------------------------            
    
    protected int getFirstIC()
    {
        return icIdA;
    }
    
//------------------------------------------------------------------------------        
    
    protected int getSecondIC()
    {
        return icIdB;
    }
    
//------------------------------------------------------------------------------            
    
    @Override
    public String toString()
    {
        String str = icIdA + ":" + vecIdxA + " | " + icIdB + ":" + vecIdxB + "\n";
        return str;
    }
    
//------------------------------------------------------------------------------                
    
}
