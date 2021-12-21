/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no> and
 *   Marco Foscato <marco.foscato@uib.no>
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

import java.util.HashMap;
import java.util.Map;



/**
 * General set of constants used to deal with Tinker
 */

public final class TinkerConstants 
{
    /**
     * Recognised error messages and proposed solutions.
     */
    @SuppressWarnings("serial")
    public static final Map<String,String> KNOWNERRORS =
		    new HashMap<String,String>() 
    {
        {
            put("READXYZ  --  Check Connection of Atoms", 
                    "Tinker reports inconsistent connectivity. Recompiling "
                    + "Tinker with a larger value for MAXVAL might solve the "
                    + "problem.");
            put("TORSIONS  --  Too many Torsional Angles; Increase MAXTORS",
                    "Recompiling Tinker with a larger value for MAXTORS might "
                    + "solve the problem.");
            put("BITORS  --  Too many Adjacent Torsions; Increase MAXBITOR",
                    "Recompiling Tinker with a larger value for MAXBITOR might "
                    + "solve the problem.");
        }
    };
    
}
