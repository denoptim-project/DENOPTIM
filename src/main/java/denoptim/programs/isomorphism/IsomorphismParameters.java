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

package denoptim.programs.isomorphism;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import denoptim.exception.DENOPTIMException;
import denoptim.files.FileUtils;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.graph.rings.RingClosureParameters;
import denoptim.programs.RunTimeParameters;
import denoptim.programs.RunTimeParameters.ParametersType;


/**
 * Parameters controlling execution of Isomorphism main class.
 * 
 * @author Marco Foscato
 */

public class IsomorphismParameters extends RunTimeParameters
{   
    /**
     * Input file containing graph A
     */
    protected String inpFileGraphA;

    /**
     * Input file containing graph B
     */
    protected String inpFileGraphB;

//-----------------------------------------------------------------------------
    
    /**
     * Constructor
     */
    public IsomorphismParameters()
    {
        this(ParametersType.ISO_PARAMS);
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Constructor
     */
    private IsomorphismParameters(ParametersType paramType)
    {
        super(paramType);
    }

//-----------------------------------------------------------------------------

    /**
     * Processes a keyword/value pair and assign the related parameters.
     * @param key the keyword as string
     * @param value the value as a string
     * @throws DENOPTIMException
     */

    public void interpretKeyword(String key, String value)
            throws DENOPTIMException
    {

        String msg = "";
        switch (key.toUpperCase())
        {
            case "INPGRAPHA=":
                inpFileGraphA = value;
                break;
            case "INPGRAPHB=":
    	        inpFileGraphB = value;
                break;
            default:
                 msg = "Keyword " + key + " is not a known Isomorphism-"
                         + "related keyword. Check input files.";
                 throw new DENOPTIMException(msg);
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Evaluate consistency of input parameters.
     * @throws DENOPTIMException
     */

    public void checkParameters() throws DENOPTIMException
    {
        String msg = "";

        if (!FileUtils.checkExists(inpFileGraphA))
        {
            msg = "Input file '" + inpFileGraphA + "' not found.";
            throw new DENOPTIMException(msg);
        }
        
        if (!FileUtils.checkExists(inpFileGraphB))
        {
            msg = "Input file '" + inpFileGraphB + "' not found.";
            throw new DENOPTIMException(msg);
        }

        checkOtherParameters();
    }

//----------------------------------------------------------------------------

    /**
     * Processes all parameters and initialize related objects.
     * @throws DENOPTIMException
     */

    public void processParameters() throws DENOPTIMException
    {   
        processOtherParameters();
    }

//----------------------------------------------------------------------------

}
