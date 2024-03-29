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

package denoptim.gui;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.UIManager;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import denoptim.files.FileFormat;
import denoptim.files.FileUtils;
import denoptim.io.DenoptimIO;
import denoptim.logging.Version;
import denoptim.task.StaticTaskManager;


/**
 * Main tool bar of the DENOPTIM graphical user interface.
 * 
 * @author Marco Foscato
 */

public class MainToolBar extends JMenuBar
{
	
	/**
	 * Version
	 */
	private static final long serialVersionUID = 3L;
	
	/**
	 * Main DENOPTIM menu
	 */
	private JMenu menuDenoptim;
	
	/**
	 * Main File menu
	 */
	private JMenu menuFile;
	
	/**
	 * Menu with list of recent files
	 */
	private JMenu openRecent;

	/**
	 * The menu listing the active panels in the deck of cards
	 */	 
	private JMenu activeTabsMenu;
    
    /**
     * Progress bar indicating the queue of task to complete.
     */
    private JProgressBar queueStatusBar;
	
	/**
	 * List of the active panels in the deck of cards
	 */	 
	private Map<GUICardPanel,JMenuItem> activeTabsAndRefs;
	
	/**
	 * Reference to the GUI window
	 */
	protected GUI gui;
	
	/**
	 * Reference to the main panel (cards deck)
	 */
	protected GUIMainPanel mainPanel;
	
//-----------------------------------------------------------------------------

	/**
	 * Constructor that build the tool bar.
	 */
	public MainToolBar() 
	{
		activeTabsAndRefs = new HashMap<GUICardPanel,JMenuItem>();
		initialize();
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Sets the reference to master GUI
	 */
	public void setRefToMasterGUI(GUI gui)
	{
		this.gui = gui;
	}

//-----------------------------------------------------------------------------
	
	/**
	 * Sets the reference to main panel for creating functionality of menu 
	 * items depending on main panel identity
	 */
	public void setRefToMainPanel(GUIMainPanel mainPanel)
	{
		this.mainPanel = mainPanel;
	}

//-----------------------------------------------------------------------------
	
	/**
	 * Initialize the contents of the tool bar
	 */
	private void initialize() 
	{		
		menuDenoptim = new JMenu("DENOPTIM");
		this.add(menuDenoptim);
		
		JMenuItem prefs = new JMenuItem("Preferences");
		prefs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    // NB: if the component used to place the dialog JMenuItem, the
			    // dialog is NOT placed close to the menu (e.g., in other screen).
				GUIPreferencesDialog prefDialog = new GUIPreferencesDialog(
				        menuDenoptim);
				prefDialog.pack();
				prefDialog.setVisible(true);
			}
		});
		menuDenoptim.add(prefs);
		
		JMenuItem about = new JMenuItem("About");
		about.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(about,
                        getAboutPanel(),
                        "About DENOPTIM",
                        JOptionPane.PLAIN_MESSAGE);
			}
			
			private JPanel getAboutPanel() {
		        JPanel abtPanel = new JPanel();
		        abtPanel.setLayout(new GridLayout(4, 1, 3, 0));
		        
                JPanel row0 = new JPanel();
                JLabel t0 = new JLabel("Version "+Version.VERSION);
                row0.add(t0);
                abtPanel.add(row0);
		        
		        JPanel row1 = new JPanel();
		        JLabel t1 = new JLabel("DENOPTIM is open source software "
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
				gui.closeIfAllSaved();
			}
		});
		menuDenoptim.add(exit);
		
		menuFile = new JMenu("File");
		this.add(menuFile);
		
		JMenu newMenu = new JMenu("New");
		
		menuFile.add(newMenu);
		JMenuItem newGA = new JMenuItem("New Evolutionary De Novo Design");
		newGA.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mainPanel.add(new GUIPrepareGARun(mainPanel));
			}
		});
		newMenu.add(newGA);
		JMenuItem newVS = new JMenuItem("New Virtual Screening");
		newVS.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mainPanel.add(new GUIPrepareFSERun(mainPanel));
			}
		});
		newMenu.add(newVS);
		JMenuItem newFr = new JMenuItem("New Molecular Fragments");
		newFr.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mainPanel.add(new GUIVertexInspector(mainPanel));
			}
		});
		newMenu.add(newFr);		
		JMenuItem newCPM = new JMenuItem("New Compatibility Matrix");
		newCPM.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mainPanel.add(new GUICompatibilityMatrixTab(mainPanel));
			}
		});
		newMenu.add(newCPM);
		JMenuItem newGr = new JMenuItem("New Graphs");
		newGr.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mainPanel.add(new GUIGraphHandler(mainPanel));
			}
		});
		newMenu.add(newGr);
		
		JMenuItem open = new JMenuItem("Open...");
		open.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File file = GUIFileOpener.pickFileOrFolder(open);
				try {
					openFile(file, FileUtils.detectFileFormat(
							file));
				} catch (Exception e1) {
				    e1.printStackTrace();
					if (file == null)
					{
						return;
					}
					String[] options = {"Abandon", 
					        "GA Parameters", 
					        "FSE Parameters",
                            "FR Parameters",
					        "Compatibility Matrix",  
							"GA Output", 
							"FSE Output"};
					FileFormat[] ffOpts = {null, 
					        FileFormat.GA_PARAM, 
					        FileFormat.FSE_PARAM,
                            FileFormat.FR_PARAM,
					        FileFormat.COMP_MAP,
					        FileFormat.GA_RUN,
					        FileFormat.FSE_RUN};
					JComboBox<String> cmbFiletype = 
							new JComboBox<String>(options);
					cmbFiletype.setSelectedIndex(0);
					JLabel msgText = new JLabel(String.format(
							"<html><body width='%1s'>"
							+ "Failed to detect file type automatically."
							+ "<br>Please, specify how to interpret file "
							+ "'" + file.getAbsolutePath() + "'.",270));
					int res = JOptionPane.showConfirmDialog(open, 
							new Object[] {msgText,cmbFiletype}, 
							"Select file type",
							JOptionPane.OK_CANCEL_OPTION,
		                     JOptionPane.ERROR_MESSAGE);
					if (res != JOptionPane.OK_OPTION)
					{
						return;
					}
					else
					{
						if (cmbFiletype.getSelectedIndex() == 0)
						{
							return;
						}
						openFile(file,ffOpts[cmbFiletype.getSelectedIndex()]); 
					}
				}
			}
		});
		menuFile.add(open);
		
	    openRecent = new JMenu("Open Recent");
	    menuFile.add(openRecent);
	    menuFile.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) 
            {
                updateOpenRecentMenu();
            }
            @Override
            public void menuDeselected(MenuEvent e) {}
            @Override
            public void menuCanceled(MenuEvent e) {}
        });
	    
		
		activeTabsMenu = new JMenu("Active Tabs");
		this.add(activeTabsMenu);
		
		JMenu menuHelp = new JMenu("Help");
		this.add(menuHelp);
		
		JMenuItem usrManual = new JMenuItem("DENOPTIM user manual");
		usrManual.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
		    	try {
		    		String url = "http://htmlpreview.github.io/?https://"
		    				+ "github.com/denoptim-project/DENOPTIM/blob/"
		    				+ "master/doc/user_manual.html";
                    Desktop.getDesktop().browse(new URI(url));
                } catch (IOException | URISyntaxException e1) {
					JOptionPane.showMessageDialog(usrManual,
							"Could not launch the browser to open online "
							+ "version of the manual.",
			                "Error",
			                JOptionPane.ERROR_MESSAGE,
			                UIManager.getIcon("OptionPane.errorIcon"));
                }
		    }
		});
		menuHelp.add(usrManual);
		
		// From here the items will be added to the RIGHT of the menu bar
		this.add(Box.createGlue());

		queueStatusBar = new JProgressBar(0,1);
		StaticTaskManager.setLinkToProgressBar(queueStatusBar);
        queueStatusBar.setMaximum(1);
        queueStatusBar.setValue(1);
        queueStatusBar.setToolTipText("Status of tasks");
        this.add(queueStatusBar);
	}

//-----------------------------------------------------------------------------
	
	/**
	 * Updated the the list of recent files in the "open Recent" menu
	 */
    private void updateOpenRecentMenu()
    {
        openRecent.removeAll();
        Map<File,FileFormat> recentFiles = DenoptimIO.readRecentFilesMap();
        if (recentFiles.size() == 0)
        {
            openRecent.setEnabled(false);
        } else {
            openRecent.setEnabled(true);
        }
        for (Entry<File, FileFormat> e : recentFiles.entrySet())
        {
            RecentFileItem item = new RecentFileItem(e.getKey(), e.getValue());
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    openFile(item.file, item.ff); 
                }
            });
            openRecent.add(item);
        }
    }
    
//-----------------------------------------------------------------------------
    
    @SuppressWarnings("serial")
    private class RecentFileItem extends JMenuItem {
        protected FileFormat ff;
        protected File file;
        public RecentFileItem(File file, FileFormat ff)
        {
            super(file.getAbsolutePath());
            this.file = file;
            this.ff = ff;
        }
    }
	
//-----------------------------------------------------------------------------

	/**
	 * Process a file that has a recognized file format and loads a suitable 
	 * GUI card to visualize the file content.
	 * @param file the file to open
	 * @param fileFormat the DENOPTIM format of the file
	 */
	
	void openFile(File file, FileFormat fileFormat) 
	{
		switch (fileFormat)
		{
			case GA_PARAM:
			    GUIPrepareGARun gaParamsPanel = new GUIPrepareGARun(mainPanel);
				mainPanel.add(gaParamsPanel);
				gaParamsPanel.importParametersFromDenoptimParamsFile(file);
				break;	
				
			case FSE_PARAM:
				GUIPrepareFSERun fseParamsPanel = 
					new GUIPrepareFSERun(mainPanel);
				mainPanel.add(fseParamsPanel);
				fseParamsPanel.importParametersFromDenoptimParamsFile(file);
				break;
				
           case FR_PARAM:
                GUIPrepareFitnessRunner frParamsPanel = 
                    new GUIPrepareFitnessRunner(mainPanel);
                mainPanel.add(frParamsPanel);
                frParamsPanel.importParametersFromDenoptimParamsFile(file);
                break;
		
			case VRTXSDF:
				GUIVertexInspector fragPanel = 
					new GUIVertexInspector(mainPanel);
				mainPanel.add(fragPanel);
				fragPanel.importVerticesFromFile(file);
				break;	
			
	        case VRTXJSON:
                GUIVertexInspector fragPanel2 = 
                    new GUIVertexInspector(mainPanel);
                mainPanel.add(fragPanel2);
                fragPanel2.importVerticesFromFile(file);
                break;  
                
	        case CANDIDATESDF:
                GUIGraphHandler graphPanel3 = new GUIGraphHandler(mainPanel);
                mainPanel.add(graphPanel3);
                graphPanel3.importGraphsFromFile(file);
                break;
				
			case GRAPHSDF:
				GUIGraphHandler graphPanel = new GUIGraphHandler(mainPanel);
				mainPanel.add(graphPanel);
				graphPanel.importGraphsFromFile(file);
				break;
				
			case GRAPHJSON:
                GUIGraphHandler graphPanel2 = new GUIGraphHandler(mainPanel);
                mainPanel.add(graphPanel2);
                graphPanel2.importGraphsFromFile(file);
                break;
                
			case COMP_MAP:
				GUICompatibilityMatrixTab cpmap = new GUICompatibilityMatrixTab(mainPanel);
				mainPanel.add(cpmap);
				cpmap.importCPMapFromFile(mainPanel,file);
				break;
				
			case GA_RUN:
				GUIInspectGARun eiPanel = 
					new GUIInspectGARun(mainPanel);
				mainPanel.add(eiPanel);
				eiPanel.importGARunData(file,mainPanel);
				break;
				
			case FSE_RUN:
				GUIInspectFSERun fsei = new GUIInspectFSERun(mainPanel);
				mainPanel.add(fsei);
				fsei.importFSERunData(file);
				break;
				
            case TXT:
                GUITextReader txt = new GUITextReader(mainPanel);
                mainPanel.add(txt);
                txt.displayContent(file);
                break;
                
            case UNRECOGNIZED:
                JOptionPane.showMessageDialog(this,
                        "File format '" + fileFormat + "' not recognized.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE,
                        UIManager.getIcon("OptionPane.errorIcon"));
                break;
			
			default:
				JOptionPane.showMessageDialog(this,String.format(
                        "<html><body width='%1s'>"
						+ "There is no specific visual representation of file"
						+ " format '" + fileFormat + "'. If you think there "
						+ "should be a way to visually inspect this file, "
						+ "please, write to the development team.",300),
		                "Error",
		                JOptionPane.ERROR_MESSAGE,
		                UIManager.getIcon("OptionPane.errorIcon"));
		}
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
		refToPanel.addActionListener((ActionEvent evt) -> {
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
