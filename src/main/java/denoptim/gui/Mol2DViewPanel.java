/*
 *   DENOPTIM
 *   Copyright (C) 2020 Marco Foscato <marco.foscato@uib.no>
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

package denoptim.gui;

import javax.swing.JPanel;

import org.openscience.cdk.interfaces.IAtomContainer;


/**
 * A panel with a 2D molecular viewer and data table.
 * 
 * @author Marco Foscato
 */

public class Mol2DViewPanel extends MolAndPropTableViewPanel
{
	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 912850110991449553L;
	
    private JPanel emptyTopPanel = new JPanel();

//-----------------------------------------------------------------------------
	
	/**
	 * Constructor
	 */
	public Mol2DViewPanel()
	{
		initialize();
	}

//-----------------------------------------------------------------------------

    @Override
    public void clearAll(boolean dataIsComing)
    {
        clearDataTable();
        this.setTopComponent(emptyTopPanel);
    }
	
//-----------------------------------------------------------------------------

    @Override
    protected void displayLoadedStructure(IAtomContainer mol)
	{
        this.setTopComponent(new TwoDimStructurePanel(mol, 
            GUIPreferences.show2DHydrogens));
        fillDataTable(mol);
	}

//-----------------------------------------------------------------------------

	@Override
	public void dispose()
	{
        //Nothing to do
	}
  	
//-----------------------------------------------------------------------------

}
