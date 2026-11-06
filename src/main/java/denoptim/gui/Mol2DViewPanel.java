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
	
    /**
     * Installed once so that loading another structure does not replace a
     * child of this split pane. Replacing the child resets the divider.
     */
    private TwoDimStructurePanel structurePanel;

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
    protected void initializeViewer()
    {
        structurePanel = new TwoDimStructurePanel();
        this.setTopComponent(structurePanel);
    }

//-----------------------------------------------------------------------------

    @Override
    public void clearAll(boolean dataIsComing)
    {
        clearDataTable();
        // Blanking the drawing is cheap. When new data is about to be loaded,
        // leave the current depiction until it is overwritten.
        if (!dataIsComing)
            structurePanel.clearStructure();
    }
	
//-----------------------------------------------------------------------------

    @Override
    protected void displayLoadedStructure(IAtomContainer mol)
	{
        structurePanel.loadStructure(mol, GUIPreferences.show2DHydrogens);
	}

//-----------------------------------------------------------------------------

	@Override
	public void dispose()
	{
        //Nothing to do
	}
  	
//-----------------------------------------------------------------------------

}
