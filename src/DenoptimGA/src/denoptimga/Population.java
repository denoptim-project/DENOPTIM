/*
 *   DENOPTIM
 *   Copyright (C) 2021  Marco Foscato <marco.foscato@uib.no>
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

package denoptimga;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import denoptim.molecule.Candidate;

/**
 * A collection of candidates. To speed-up operations such as the selection of
 * parents for crossover, this class holds also compatibility relations between 
 * candidates.
 * 
 * @author Marco Foscato
 */

public class Population extends ArrayList<Candidate> implements Cloneable
{

    /**
     * Version UID
     */
    private static final long serialVersionUID = 1L;

//------------------------------------------------------------------------------

    public Population()
    {
        super();
    }
    
//------------------------------------------------------------------------------

    /**
     * Creates a population from a collection of candidates. All candidates are 
     * processed to determine the APClass-based compatibility relations between 
     * the candidates.
     * @param collection
     */
    public Population(Collection<Candidate> collection)
    {
        super();
        for (Candidate c : collection)
        {
            this.add(c);
        }
    }
    
//------------------------------------------------------------------------------

    public Population clone()
    {
        Population clone = new Population();

        for (Candidate m : this)
        {
            clone.addNoProcessing(m.clone());
        }
        
        //TODO: clone relations
        
        return clone;
    }
    
//------------------------------------------------------------------------------

    private void addNoProcessing(Candidate c)
    {
        super.add(c);
    }
    
//------------------------------------------------------------------------------

    @Override
    public boolean add(Candidate c)
    {
        
        //TODO: create relations
        return super.add(c);
    }
    
//------------------------------------------------------------------------------
    
}
