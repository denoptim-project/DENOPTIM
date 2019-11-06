package gui;


import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

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

public class ToolBar extends JMenuBar {
	
	/**
	 * Version
	 */
	private static final long serialVersionUID = -6297787221312734786L;
	
	/**
	 * Main DENOPTIM menu
	 */
	private JMenu menuDenoptim;

	/*
	 NOT WORKING 
	
	 * The menu listing the active panels in the deck
	 
	private JMenu panelsMenu;
*/
	
	/**
	 * The main panel (cards deck)
	 */
	protected JPanel mainPanel;

	/**
	 * Create the application.
	 */
	public ToolBar() 
	{
		this.mainPanel = mainPanel;
		initialize();
	}
	
	/**
	 * Sets the main panel for creating functionality of menu items depending on main panel identity
	 */
	public void setRefToMainPanel(JPanel mainPanel)
	{
		this.mainPanel = mainPanel;
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		menuDenoptim = new JMenu("DENOPTIM");
		menuDenoptim.setFont(new Font("Lucida Grande", Font.BOLD, 12));
		this.add(menuDenoptim);
		
		JMenuItem open = new JMenuItem("Open...");
		open.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DenoptimGUIFileOpener openFileGui = new DenoptimGUIFileOpener();
				openFileGui.pickFile();
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
		        JLabel t1 = new JLabel("DENPTIM is open source software (Affero GPL-3.0).");
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
		                    Desktop.getDesktop().browse(new URI("http://github.com/denoptim-project"));
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
				JLabel h3 = new JLabel("http://doi.org/10.1021/acs.jcim.9b00516");
				h3.setForeground(Color.BLUE.darker());
				h3.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				h3.addMouseListener(new MouseAdapter() {
				    @Override
				    public void mouseClicked(MouseEvent e) {
				    	try {
		                    Desktop.getDesktop().browse(new URI("https://doi.org/10.1021/acs.jcim.9b00516"));
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
		
		/*
		 NOT WORKING: consider removing of fixing it
		panelsMenu = new JMenu("Active Panels");
		panelsMenu.setFont(new Font("Lucida Grande", Font.PLAIN, 12));
		super.add(panelsMenu);
		*/
		
		JMenu menuHelp = new JMenu("Help");
		menuHelp.setFont(new Font("Lucida Grande", Font.PLAIN, 12));
		super.add(menuHelp);
		
		JMenuItem usrManual = new JMenuItem("DENOPTIM user manual");
		usrManual.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
		    	try {
                    Desktop.getDesktop().browse(new URI("http://htmlpreview.github.com/?https://github.com/denoptim-project/DENOPTIM/blob/master/doc/user_manual.html"));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                    //TODO: use local version of user manual
                }
		    }
		});
		menuHelp.add(usrManual);
	}
	
	/**
	 * Returns the main menu of the toolbar
	 * @return the <b>DENOPTIM</b> menu of the toolbar
	 */
	public JMenu getMainMenu()
	{
		return menuDenoptim;
	}

	/*
	 * Add an entry to the list of active panels
	 
	 NOT WORKING!
	 
	public void addRefTo(JPanel panel)
	{
		JMenuItem ref = new JMenuItem(panel.getName());
		ref.addActionListener((java.awt.event.ActionEvent evt) -> {
            ((CardLayout) mainPanel.getLayout()).show(mainPanel,panel.getName());
        });
		panelsMenu.add(ref);
	}
	*/
}
