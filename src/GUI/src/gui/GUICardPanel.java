package gui;

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
}
