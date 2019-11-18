package gui;

import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import java.awt.CardLayout;
import java.awt.Font;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Main tool bar of the DENOPTIM graphical user interface.
 * 
 * @author Marco Foscato
 */

public class MainToolBar extends JMenuBar {
	
	/**
	 * Version
	 */
	private static final long serialVersionUID = -6297787221312734786L;
	
	/**
	 * Main DENOPTIM menu
	 */
	private JMenu menuDenoptim;

	/**
	 * The menu listing the active panels in the deck of cards
	 */	 
	private JMenu activeTabsMenu;
	
	/**
	 * List of the active panels in the deck of cards
	 */	 
	private Map<GUICardPanel,JMenuItem> activeTabsAndRefs;
	
	/**
	 * Reference to the main panel (cards deck)
	 */
	protected JPanel mainPanel;

	/**
	 * Constructor that build the tool bar.
	 */
	public MainToolBar() 
	{
		activeTabsAndRefs = new HashMap<GUICardPanel,JMenuItem>();
		initialize();
	}
	
	/**
	 * Sets the main panel for creating functionality of menu items depending
	 * on main panel identity
	 */
	public void setRefToMainPanel(JPanel mainPanel)
	{
		this.mainPanel = mainPanel;
	}

	/**
	 * Initialize the contents of the tool bar
	 */
	private void initialize() {
		menuDenoptim = new JMenu("DENOPTIM");
		menuDenoptim.setFont(new Font("Lucida Grande", Font.BOLD, 12));
		this.add(menuDenoptim);
		
		JMenuItem open = new JMenuItem("Open...");
		open.addActionListener(new ActionListener() {
			@SuppressWarnings("static-access")
			public void actionPerformed(ActionEvent e) {
				DenoptimGUIFileOpener opener = new DenoptimGUIFileOpener();
				opener.pickFile();
			}
		});
		menuDenoptim.add(open);
	
		menuDenoptim.addSeparator();
		
		JMenuItem about = new JMenuItem("About");
		about.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(null,
                        getAboutPanel(),
                        "About DENOPTIM",
                        JOptionPane.PLAIN_MESSAGE);
			}
			
			private JPanel getAboutPanel() {
		        JPanel abtPanel = new JPanel();
		        abtPanel.setLayout(new GridLayout(3, 1, 3, 0));
		        JPanel row1 = new JPanel();
		        JLabel t1 = new JLabel("DENPTIM is open source software "
		        		+" (Affero GPL-3.0).");
		        row1.add(t1);
		        abtPanel.add(row1);
		        
		        JPanel row2 = new JPanel();
		        JLabel t2 = new JLabel("Get involved at	");
				JLabel h2 = new JLabel("http://github.com/denoptim-project");
				h2.setForeground(Color.BLUE.darker());
				h2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				h2.addMouseListener(new MouseAdapter() {
				    @Override
				    public void mouseClicked(MouseEvent e) {
				    	try {
		                    Desktop.getDesktop().browse(new URI(
		                    		"http://github.com/denoptim-project"));
		                } catch (IOException | URISyntaxException e1) {
		                    e1.printStackTrace();
		                }
				    }
				});
				row2.add(t2);
				row2.add(h2);
				abtPanel.add(row2);
				
				JPanel row3 = new JPanel();
		        JLabel t3 = new JLabel("Cite ");
				JLabel h3 = new JLabel("http://doi.org/10.1021/"
						+ "acs.jcim.9b00516");
				h3.setForeground(Color.BLUE.darker());
				h3.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				h3.addMouseListener(new MouseAdapter() {
				    @Override
				    public void mouseClicked(MouseEvent e) {
				    	try {
				    		String url = "https://doi.org/10.1021/"
				    				+" acs.jcim.9b00516";
		                    Desktop.getDesktop().browse(new URI(url));
		                } catch (IOException | URISyntaxException e1) {
		                    e1.printStackTrace();
		                }
				    }
				});
				row3.add(t3);
				row3.add(h3);
		        abtPanel.add(row3);

		        return abtPanel;
		    }
		});
		menuDenoptim.add(about);
		
		menuDenoptim.addSeparator();
		
		JMenuItem exit = new JMenuItem("Exit");
		exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		menuDenoptim.add(exit);
		
		activeTabsMenu = new JMenu("Active Tabs");
		activeTabsMenu.setFont(new Font("Lucida Grande", Font.PLAIN, 12));
		this.add(activeTabsMenu);
		
		JMenu menuHelp = new JMenu("Help");
		menuHelp.setFont(new Font("Lucida Grande", Font.PLAIN, 12));
		this.add(menuHelp);
		
		JMenuItem usrManual = new JMenuItem("DENOPTIM user manual");
		usrManual.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
		    	try {
		    		String url = "http://htmlpreview.github.com/?https://"
		    				+ "github.com/denoptim-project/DENOPTIM/blob/"
		    				+ "master/doc/user_manual.html";
                    Desktop.getDesktop().browse(new URI(url));
                } catch (IOException | URISyntaxException e1) {
					JOptionPane.showMessageDialog(null,
							"Could not launch the browser to open online "
							+ "version of the manual.",
			                "Error",
			                JOptionPane.ERROR_MESSAGE,
			                UIManager.getIcon("OptionPane.errorIcon"));
                }
		    }
		});
		menuHelp.add(usrManual);
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Returns the main menu of the tool bar
	 * @return the <b>DENOPTIM</b> menu of the tool bar
	 */
	public JMenu getMainMenu()
	{
		return menuDenoptim;
	}

//-----------------------------------------------------------------------------
	
	/**
	 * Add the reference to an active panel/tab in the menu listing active 
	 * panel/tabs.
	 * The panel is assumed to be included in the deck of cards (i.e., this 
	 * method does not check if this assumption holds!).
	 * @param panel the panel to reference
	 */
	public void addActiveTab(GUICardPanel panel)
	{
		JMenuItem refToPanel = new JMenuItem(panel.getName());
		refToPanel.addActionListener((java.awt.event.ActionEvent evt) -> {
            ((CardLayout) mainPanel.getLayout()).show(
            		mainPanel,panel.getName());
        });
		activeTabsAndRefs.put(panel, refToPanel);
		activeTabsMenu.add(refToPanel);
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Remove the reference to an panel/tab in the menu listing active tabs.
	 * If the panel is not listed among the active ones, this does nothing.
	 * @param panel the panel referenced by the reference to remove from the 
	 * list
	 */
	public void removeActiveTab(GUICardPanel panel)
	{
		if (activeTabsAndRefs.containsKey(panel))
		{
			JMenuItem refToDel = activeTabsAndRefs.get(panel);
			activeTabsMenu.remove(refToDel);
			activeTabsAndRefs.remove(panel);
		}
	}

//-----------------------------------------------------------------------------
}
