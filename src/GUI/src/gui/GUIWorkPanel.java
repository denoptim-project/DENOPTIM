package gui;

import java.awt.CardLayout;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

/**
 * The general class of GUI panels meant to occupy one card in the card layout of the main panel.
 */

public class GUIWorkPanel extends JPanel
{
	
	/**
	 * Version
	 */
	private static final long serialVersionUID = -1640517890155875184L;
	
	/**
	 * The main panel (cards deck)
	 */
	protected JPanel mainPanel;

	/**
	 * Constructor
	 */
	public GUIWorkPanel(JPanel mainPanel, String newPanelName) {
		this.mainPanel = mainPanel;
		this.setName(newPanelName);
	}
	
	/**
	 * Adds a panel to the cards deck in mainPanel and sets this latter panel as the top of the deck
	 * @param newPanel the new panel to be added
	 * @param newPanelName the name of the new panel
	 */
	protected void addPanelToDeck(JPanel newPanel)
	{
		mainPanel.add(newPanel);
		((CardLayout) mainPanel.getLayout()).last(mainPanel);
	}
	
	/**
	 * Shows a dialog saying that the functionality is not ready yet. Use only during devel phase.
	 */
	protected static void getNonImplementedError()
	{
		JOptionPane.showMessageDialog(null,
                "Sorry! Function not implemented yet.",
                "Error",
                JOptionPane.PLAIN_MESSAGE,
                UIManager.getIcon("OptionPane.errorIcon"));
	}
}
