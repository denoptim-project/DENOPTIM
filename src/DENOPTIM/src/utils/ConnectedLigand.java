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

package utils;

import org.openscience.cdk.interfaces.IAtom;

import constants.DENOPTIMConstants;

/**
 * A ConnectedLigand is just an atom with an explicit field reporting the number
 * of connected atoms.
 *
 * @author Marco Foscato 
 */
public class ConnectedLigand
{
    //Root of the Ligand
    private IAtom seed;
    //Connections
    private int connections;
    //is dummy atom
    private boolean isDu;

//------------------------------------------------------------------------------
    
    public ConnectedLigand(IAtom seed, int connections)
    {
        this.seed = seed;
        this.connections = connections;
        String symbol = seed.getSymbol();
        boolean isRCA = DENOPTIMConstants.RCATYPEMAP.keySet().contains(symbol);
        this.isDu = false;
        if (symbol.equals(DENOPTIMConstants.DUMMYATMSYMBOL) || isRCA ||
                               !DENOPTIMConstants.ALL_ELEMENTS.contains(symbol))
        {
            this.isDu = true;
        }
    }

//------------------------------------------------------------------------------
    
    public int getConnections()
    {
        return this.connections;
    }

//------------------------------------------------------------------------------
    
    public boolean isDummy()
    {
        return this.isDu;
    }

//------------------------------------------------------------------------------
    
    public IAtom getAtom()
    {
        return this.seed;
    }

//------------------------------------------------------------------------------

    public String toString()
    {
	StringBuilder sb = new StringBuilder();
	sb.append(seed.getSymbol());
	sb.append("(").append(connections).append("-conn.)");
	return sb.toString();
    }
//------------------------------------------------------------------------------    
}
