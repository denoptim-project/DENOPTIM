package gui;

import javax.swing.JPanel;


/**
 * Form containing all the input parameters for DenoptimGA.
 */

public class GUIPrepareGARun extends GUIPrepare
{

	/**
	 * Version UID
	 */
	private static final long serialVersionUID = -2208699600683389219L;

	/**
	 * Constructor
	 */
	public GUIPrepareGARun(JPanel mainPanel, String newPanelName) {
		super(mainPanel, newPanelName);
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		
		GAParametersForm gaParsPane = new GAParametersForm(mainPanel.getSize());
		super.allParams.add(gaParsPane);
		super.tabbedPane.addTab("Genetic Algorithm", null, gaParsPane, null);
		
		FSParametersForm fseParsPane = new FSParametersForm(mainPanel.getSize());
		super.allParams.add(fseParsPane);
		super.tabbedPane.addTab("Fragment Space", null, fseParsPane, null);
		
		FitnessParametersForm fitParsPane = new FitnessParametersForm(mainPanel.getSize());
		super.allParams.add(fitParsPane);
		super.tabbedPane.addTab("Fitness Provider", null, fitParsPane, null);
		
	}
}
