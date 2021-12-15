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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

/**
 * Class of GUI panels meant to occupy one card in the deck-of-cards layout
 * of the main panel.
 */

public class GUICardPanel extends JPanel
{
	/**
	 * Version
	 */
	private static final long serialVersionUID = -1640517890155875184L;
	
	/**
	 * The main panel (cards deck)
	 */
	protected GUIMainPanel mainPanel;

	/**
	 * Constructor for JPanel meant to be a  single "card" in the deck of cards. 
	 * @param mainPanel container playing the role of the deck of cards
	 * @param newPanelName the reference name of this card in the deck.
	 */
	public GUICardPanel(GUIMainPanel mainPanel, String newPanelName) {
		this.mainPanel = mainPanel;
		this.setName(newPanelName);
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Check for unsaved changes in the components included in this card.
	 * @return <code>true</code> is there are unsaved changes.
	 */
	public boolean hasUnsavedChanges()
	{		
		boolean res = false;
		if (this instanceof GUIPrepare)
		{
			res = ((GUIPrepare) this).hasUnsavedChanges();
		}
		else if (this instanceof GUIVertexInspector)
		{
			res = ((GUIVertexInspector) this).hasUnsavedChanges();
		}
		
		return res;
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Remove the card from the deck of cards and takes care of removing
	 * also the entry in the list of active tabs.
	 */
	protected class removeCardActionListener implements ActionListener
	{
		private GUICardPanel parentPanel;
		
		public removeCardActionListener(GUICardPanel panel)
		{
			this.parentPanel = panel;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) 
		{		
			if (hasUnsavedChanges())
			{
				Object[] options = {"Yes","No"};
				int res = JOptionPane.showOptionDialog(parentPanel,
	                "<html>Found unsaved edits.<br>"
	                + "Abandon without saving?</html>",
	                "Abandon?",
	                JOptionPane.DEFAULT_OPTION,
	                JOptionPane.QUESTION_MESSAGE,
	                UIManager.getIcon("OptionPane.warningIcon"),
	                options,
	                options[1]);
				if (res == 0)
				{
					mainPanel.removeCard(parentPanel);
				}
			}
			else
			{
				mainPanel.removeCard(parentPanel);
			}
		}
	}

//-----------------------------------------------------------------------------

}
