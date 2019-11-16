package gui;

import javax.swing.JFrame;
import javax.swing.JPanel;

import java.awt.EventQueue;

/**
 * Graphical User Interface of the DENOPTIM package.
 * This GUI aims to facilitate:
 * <ul>
 * <li>creation of the various types of DENOPTIM input files</li>
 * <li>reuse and modification of DENOPTIM input files</li>
 * </ul>
 * 
 * @author Marco Foscato
 */

public class GUI {

	private JFrame frame;     //GUI window frame
	private int mainFrameInitX = 100;
	private int mainFrameInitY = 100;
	private JPanel framePane; //Panel including tool bar
	private GUIMainPanel mainPanel; //Panel including all but tool bar

	/**
	 * Default width
	 */
	private int width = 800;
	
	/**
	 * Default height
	 */
	private int height = 500;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GUI window = new GUI();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public GUI() {
		initialize();
	}

	/**
	 * Create and fill frame.
	 */
	private void initialize() {
		frame = new JFrame("DENOPTIM - GUI");
		frame.setBounds(mainFrameInitX, mainFrameInitY, width, height);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		//Frame pane contains all (tools bar and the rest)
		framePane = (JPanel) frame.getContentPane();

		//Menu bar
		MainToolBar menuBar = new MainToolBar();
		frame.setJMenuBar(menuBar);
		
		//Main panel is a deck of cards that contains all but the tool bar
		mainPanel = new GUIMainPanel(menuBar);
		framePane.add(mainPanel);
		menuBar.setRefToMainPanel(mainPanel);
		mainPanel.add(new HomePanel(mainPanel));		
	}
}
