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

package denoptim.fragmenter;

import java.util.ArrayList;

import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.graph.Fragment;
import denoptim.programs.fragmenter.FragmenterParameters;
import denoptim.task.Task;
import denoptim.utils.TaskUtils;

/**
 * Task that chops one chemical system into fragments
 */

public abstract class FragmentationTask extends Task
{
    
	/**
	 * The chemical system to chop
	 */
    protected IAtomContainer mol = null;
    
    /**
     * The data structure holding the results of this task
     */
    protected ArrayList<Fragment> results = null;
   
    /**
     * Settings for the calculation of the fitness
     */
    protected FragmenterParameters settings;

//------------------------------------------------------------------------------
    
    public FragmentationTask(FragmenterParameters settings, IAtomContainer mol)
    {
    	super(TaskUtils.getUniqueTaskIndex());
    	this.settings = settings;
    	this.mol = mol;
    }

//------------------------------------------------------------------------------
    
    /**
     * Calls the task
     */
 
    @Override
    public Object call() throws Exception
    {
        results = new ArrayList<Fragment>();
        
        //TODO
        Fragment frag = new Fragment();
        results.add(frag);
        
        
        return results;
    }

//------------------------------------------------------------------------------

}
