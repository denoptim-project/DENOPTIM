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
 */
public class APAtomHolder
{
    private int atm_num;
    private int ap_idx;
    private double[] dirVec;

//------------------------------------------------------------------------------

    public APAtomHolder()
    {

    }

//------------------------------------------------------------------------------

    public APAtomHolder(int m_atm_num, int m_ap_idx, double[] m_vec)
    {
        atm_num = m_atm_num;
        ap_idx = m_ap_idx;
        dirVec = m_vec;
    }

//------------------------------------------------------------------------------

    protected double[] getDirVec()
    {
        return dirVec;
    }

//------------------------------------------------------------------------------

    protected int getAtomNumber()
    {
        return atm_num;
    }

//------------------------------------------------------------------------------

    protected int getAPIndex()
    {
        return ap_idx;
    }

//------------------------------------------------------------------------------

}
