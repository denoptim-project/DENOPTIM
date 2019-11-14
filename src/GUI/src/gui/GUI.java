package gui;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import java.awt.CardLayout;
import java.awt.EventQueue;

/**
 * Graphical User Interface for DENOPTIM package. 
 * 
 * @author Marco Foscato
 */

public class GUI {

	private JFrame frame;     //GUI window frame
	private int mainFrameInitX = 100;
	private int mainFrameInitY = 100;
	private JPanel framePane; //Panel including toolbar
	private JPanel mainPanel; //Panel including all but toolbar

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
		mainPanel = new GUIMainPanel(new CardLayout(), menuBar);
		framePane.add(mainPanel);
		menuBar.setRefToMainPanel(mainPanel);
		mainPanel.add(new HomePanel(mainPanel));
		
		//Menu item that takes the home panel on the top of the deck
	    JMenuItem homeItem = new JMenuItem("Home view");
	    homeItem.addActionListener((java.awt.event.ActionEvent evt) -> {
            ((CardLayout) mainPanel.getLayout()).first(mainPanel);
        });
		menuBar.getMainMenu().add(homeItem,0);
		
	}
}
