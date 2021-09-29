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

package gui;

import java.awt.CardLayout;
import java.awt.Component;

import javax.swing.JPanel;

/**
 * The main panel is a deck of cards that occupies all the GUI frame.
 * Only the main menu bar of the GUI resides outside of this main panel,
 * and this class keeps a reference to that tool bar.
 */

public class GUIMainPanel extends JPanel 
{
	/**
	 * Version
	 */
	private static final long serialVersionUID = 9090055883771428756L;
	
	/**
	 * Reference to the main tool bar of the GUI
	 */
    MainToolBar toolBar;
	
//-----------------------------------------------------------------------------
	
	public GUIMainPanel(MainToolBar toolBar) 
	{
		super(new CardLayout());
		this.toolBar = toolBar;
	}
	
//-----------------------------------------------------------------------------	

	/**
	 * Add a component and a reference to such component in the main tool bar.
	 * The reference is added only if the component if an instance of 
	 * GUICardPanel. Other components are added according to superclass method.
	 */
	
	@Override 
	public Component add(Component comp)
	{
		if (comp instanceof GUICardPanel)
		{
			String refName = comp.getName();	
			super.add(comp,refName);
			((CardLayout) this.getLayout()).last(this);
			toolBar.addActiveTab((GUICardPanel) comp);
		}
		else
		{
			super.add(comp);
		}
		return comp;
	}

//-----------------------------------------------------------------------------	
	
	/**
	 * Remove a panel (i.e., a card) from the pile of cards (i.e., card deck)
	 * @param comp the component to remove
	 */

	public void removeCard(Component comp)
	{
		if (comp instanceof GUICardPanel)
		{
			if (comp instanceof GUIVertexInspector)
			{
				((GUIVertexInspector) comp).dispose();
			}
			if (comp instanceof GUIGraphHandler)
			{
				((GUIGraphHandler) comp).dispose();
			}
			toolBar.removeActiveTab((GUICardPanel) comp);
			super.remove(comp);
		}
		else
		{
			super.remove(comp);
		}
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Checks is any card has unsaved changes
	 * @return <code>true</code> if there is at least one card with unsaved 
	 * changes
	 */
	public boolean hasUnsavedChanges()
	{
		boolean res = false;
		for (Component c : this.getComponents())
		{		
			if (c instanceof GUICardPanel)
			{
				if (((GUICardPanel) c).hasUnsavedChanges())
				{
					res = true;
					break;
				}
			}
		}
		return res;
	}
	
//-----------------------------------------------------------------------------
}
