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

package denoptim.task;

import denoptim.exception.DENOPTIMException;
import fragspaceexplorer.FragSpaceExplorer;

/**
 * Task that runs a DENOPTIM experiment with the fragment space explorer. 
 * This class is meant to call the FragSPaceExplorer.main method from within
 * the GUI.
 */

public class FragSpaceExplorerTask extends GUIInvokedMainTask
{

//------------------------------------------------------------------------------ 

	@Override
	protected void mainCaller(String[] args) throws DENOPTIMException {
		FragSpaceExplorer.main(args);
	}
    
//------------------------------------------------------------------------------

}

