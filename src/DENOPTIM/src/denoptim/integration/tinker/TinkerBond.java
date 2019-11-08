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

package denoptim.integration.tinker;

import java.io.Serializable;

/**
 *
 * @author Vishwesh Venkatraman
 */
public class TinkerBond implements Serializable,Cloneable
{
    private TinkerAtom atoms[];

//------------------------------------------------------------------------------

    /**
     * Bond constructor.
     *
     * @param a1 Atom number 1.
     * @param a2 Atom number 2.
     */
    public TinkerBond(TinkerAtom a1, TinkerAtom a2)
    {
        atoms = new TinkerAtom[2];
        int i1 = a1.getXYZIndex();
        int i2 = a2.getXYZIndex();
        if (i1 < i2)
        {
            atoms[0] = a1;
            atoms[1] = a2;
        }
        else
        {
            atoms[0] = a2;
            atoms[1] = a1;
        }
    }

//------------------------------------------------------------------------------

    /**
     * Get the constituent TinkerAtom specified by index.
     *
     * @param index a int.
     * @return a TinkerAtom object.
     */
    public TinkerAtom getAtom(int index)
    {
        if (index >= 0 && index < atoms.length)
        {
            return atoms[index];
        }
        return null;
    }

//------------------------------------------------------------------------------

}
