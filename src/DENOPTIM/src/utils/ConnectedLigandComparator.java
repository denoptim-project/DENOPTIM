package utils;

import java.util.Comparator;

/**
 * Compare two ConnectedLigand according to the number of connected atoms and
 * the mass number. Dummy atoms are prioritized if they have only one connected
 * neighbour
 *
 * @author Marco Foscato (University of Bergen)
 */
public class ConnectedLigandComparator implements Comparator<ConnectedLigand>
{

    @Override
    public int compare(ConnectedLigand a, ConnectedLigand b)
    {
        final int FIRST = 1;
        final int EQUAL = 0;
        final int LAST = -1;

	// Get the connection number
        int cnnA = a.getConnections();
        int cnnB = b.getConnections();

        //Get the masses
        int massA;
        int massB;
        if (!a.isDummy() && !b.isDummy())
        {
            massA = a.getAtom().getMassNumber();
            massB = b.getAtom().getMassNumber();
        }
        else
        {
	    // For dummy atoms set fictitious mass and connectivity
            if (a.isDummy())
            {
                massA = 1;
                if (cnnA == 1)
                {
                    cnnA = 100;
                }
            }
            if (b.isDummy())
            {
                massB = 1;
                if (cnnB == 1)
                {
                    cnnB = 100;
                }
            }
        }

        //Decide on priority
        if (cnnA == cnnB)
        {
            if (a.isDummy())
            {
                massA = 1;
            }
            else
            {
                massA = a.getAtom().getMassNumber();
            }
            if (b.isDummy())
            {
                massB = 1;
            }
            else
            {
                massB = b.getAtom().getMassNumber();
            }

            if (massA == massB)
            {
                return EQUAL;
            }
            else
            {
                if (massA < massB)
                {
                    return FIRST;
                }
                else
                {
                    return LAST;
                }
            }
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
