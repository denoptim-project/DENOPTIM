package gui;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * Master form containing all sub-forms that need to be filled to define the input parameters for DenoptimGA.
 * 
 * @author Marco Foscato
 *
 */

public class GUIPrepareGARun extends GUIPrepare
{

	/**
	 * Version UID
	 */
	private static final long serialVersionUID = -2208699600683389219L;
	
	/**
	 * Unique identified for instances of this form
	 */
	public static AtomicInteger prepGATabUID = new AtomicInteger(1);

	/**
	 * Constructor
	 */
	public GUIPrepareGARun(GUIMainPanel mainPanel) {
		super(mainPanel, "Prepare GA experiment #" + prepGATabUID.getAndIncrement());
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
