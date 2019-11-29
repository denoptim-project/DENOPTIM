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
	 * Flag notifying that some data was not saved
	 */
	protected boolean unsavedChanges = false;
	
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
	
	/**
	 * Shows a dialog saying that the functionality is not ready yet. Use only during devel phase.
	 */
	protected void getNonImplementedError()
	{
		JOptionPane.showMessageDialog(null,
                "Sorry! Function not implemented yet.",
                "Error",
                JOptionPane.PLAIN_MESSAGE,
                UIManager.getIcon("OptionPane.errorIcon"));
	}
	
	/**
	 * Remove the card from the deck of cards and takes care of removing
	 * also the entry in the list of active tabs.
	 * 
	 * @author Marco Foscato
	 *
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
			if (unsavedChanges)
			{
				Object[] options = {"Yes","No"};
				int res = JOptionPane.showOptionDialog(null,
	                "<html>Abandon without saving?"
	                + "<br>Press NO to go back.</html>",
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
}
