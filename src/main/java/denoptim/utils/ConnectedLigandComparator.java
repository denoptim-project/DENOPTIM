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

import java.util.Comparator;

/**
 * Compare two ConnectedLigand according to the number of connected atoms and
 * the mass number. Dummy atoms are prioritized if they have only one connected
 * neighbour
 *
 * @author Marco Foscato 
 */
public class ConnectedLigandComparator implements Comparator<ConnectedLigand>
{

    @Override
    public int compare(ConnectedLigand a, ConnectedLigand b)
    {
        final int FIRST = 1;
        final int LAST = -1;

	// Get the connection number
        int cnnA = a.getConnections();
        int cnnB = b.getConnections();

        //Get the masses and #connection (fictitious for dummy)
        int massA = 1; 
        int massB = 1; 
        if (a.isDummy())
        {
            if (cnnA == 1)
            {
                cnnA = 100;
            }
    	}
    	else
    	{
            massA = a.getAtom().getAtomicNumber();
        }

        if (b.isDummy())
        {
            if (cnnB == 1)
            {
                cnnB = 100;
            }
        }
        else
        {
            massB = b.getAtom().getAtomicNumber();
        }

        //Decide on priority
        if (cnnA == cnnB)
        {
            return Integer.compare(massB, massA);
        }
        else
        {
            if (cnnA < cnnB)
            {
                return FIRST;
            }
            else
            {
                return LAST;
            }
        }
    }
}
