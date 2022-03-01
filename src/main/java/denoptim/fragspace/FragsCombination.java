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

import java.util.HashMap;

/**
 * Data structure identifying a combination of one or more pairs of 
 * attachment points located on specific fragments/vertices.
 *
 * @author Marco Foscato
 */

public class FragsCombination extends HashMap<IdFragmentAndAP, IdFragmentAndAP>
{

//------------------------------------------------------------------------------

    /**
     * Constructor
     */

    public FragsCombination()
    {
	super();
    }

//------------------------------------------------------------------------------

}
