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
            massA = a.getAtom().getMassNumber();
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
            massB = b.getAtom().getMassNumber();
        }

        //Decide on priority
        if (cnnA == cnnB)
        {
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
