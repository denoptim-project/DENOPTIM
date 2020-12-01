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

package gui;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import denoptim.task.StaticTaskManager;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.concurrent.TimeUnit;

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

public class GUI 
{

	private JFrame frame;     //GUI window frame
	private JPanel framePane; //Panel including tool bar
	private GUIMainPanel mainPanel; //Panel including all but tool bar

	/**
	 * Default width
	 */
	private int width = 900;
	
	/**
	 * Default height
	 */
	private int height = 600;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		
		// Hack to debug com.apple.laf.AquaLookAndFeel. Such LAF when combined 
		// with "dark mode" (i.e., the system appearance where all windows have 
		// dark colors) tries to change the appearance, but in not fully 
		// self-consistent. For example, it sets the background 
		// of the MenuBar components to a dark color, but does not adapt the
		// foreground color accordingly. 
		// Thus, the menu is unreadable because painter using
		// dark grey font on black-ish background. To overcome this problem we
		// set the foreground to a lighter shade of grey.
		if (UIManager.getLookAndFeel().getClass().getName().equals(
				"com.apple.laf.AquaLookAndFeel") && weRunOnMacDarkMode())
		{
			UIManager.getLookAndFeelDefaults().put("MenuBar.foreground", 
					new Color(150,150,150));
			UIManager.getLookAndFeelDefaults().put("TableHeader.background", 
					new Color(200,200,200));
		}
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GUI window = new GUI();
					window.frame.pack();
					window.frame.setLocationRelativeTo(null);
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Checks if we are in dark mode.
	 */
	private static boolean weRunOnMacDarkMode()
    {
        try
        {
            final Process p = Runtime.getRuntime().exec(new String[] 
            		{"defaults", "read", "-g", "AppleInterfaceStyle"});
            p.waitFor(100, TimeUnit.MILLISECONDS);
            return p.exitValue() == 0;
        }
        catch (Throwable ex)
        {
            return false;
        }
    }

//-----------------------------------------------------------------------------
	
	/**
	 * Create the application.
	 */
	public GUI() {
		initialize();
	}

//-----------------------------------------------------------------------------
	
	/**
	 * Create and fill frame.
	 */
	private void initialize() {
		frame = new JFrame("DENOPTIM - GUI");
		frame.setPreferredSize(new Dimension(width, height));
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
		    @Override
		    public void windowClosing(WindowEvent windowEvent) {
		    	closeIfAllSaved();
		    }
		});
		
		//Frame pane contains all (tools bar and the rest)
		framePane = (JPanel) frame.getContentPane();

		//Menu bar
		MainToolBar menuBar = new MainToolBar();
		frame.setJMenuBar(menuBar);
		
		//Main panel is a deck of cards that contains all but the tool bar
		mainPanel = new GUIMainPanel(menuBar);
		framePane.add(mainPanel);
		menuBar.setRefToMainPanel(mainPanel);
		menuBar.setRefToMasterGUI(this);
		
		mainPanel.add(new HomePanel(mainPanel));
		
		// We instantiate also the task manager, even it it might not be used
		// This is to pre-start the tasks and get a more reliable queue status
		// at any given time after this point.
		StaticTaskManager.getInstance();
		
		//Set the timing of tooltips
		ToolTipManager.sharedInstance().setDismissDelay(6000);
		ToolTipManager.sharedInstance().setInitialDelay(1000);
		ToolTipManager.sharedInstance().setReshowDelay(100);
		
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Close GUI is there are no unsaved changes to the open components
	 */
	protected void closeIfAllSaved()
	{
		if (StaticTaskManager.hasActive())
		{
			String[] options = new String[]{"Yes, stop running tasks","No"};
			int res = JOptionPane.showOptionDialog(null,
					"<html>Found running tasks.<br>Do you want to "
				            + "stop all running tasks?</html>",
				    "Stop all?", 
	                JOptionPane.YES_NO_OPTION,
	                JOptionPane.QUESTION_MESSAGE,
	                UIManager.getIcon("OptionPane.warningIcon"),
	                options,
	                options[1]);
		    if (res == 1 || res == -1)
		    {
		    	return;
		    }
		}
		
    	try {
			StaticTaskManager.stopAll();
		} catch (Exception e) {
			e.printStackTrace();
			String[] options = new String[]{"Yes, stop and close"};
			int res = JOptionPane.showOptionDialog(null,
					"<html>Problems killing the running tasks.<br>Will force "
					+ "quit all tasks and shutdown.</html>",
				    "Force quit", 
	                JOptionPane.DEFAULT_OPTION,
	                JOptionPane.QUESTION_MESSAGE,
	                UIManager.getIcon("OptionPane.warningIcon"),
	                options,
	                options[0]);
			Runtime.getRuntime().halt(0);
		}
    	
		if (mainPanel.hasUnsavedChanges())
		{
			/*
			//FOR SOME REASON USING HTML HERE PREVENTS DISPLAYING THE TEXT
			"<html>Found unsaved changes.<br>Are you sure you want to "
            + "close this window?</html>",
            */
			String[] options = new String[]{"Yes","No"};
			int res = JOptionPane.showOptionDialog(null,"Found unsaved changes. Are you sure you want to close this window?",
				    "Close Window?", 
	                JOptionPane.DEFAULT_OPTION,
	                JOptionPane.QUESTION_MESSAGE,
	                UIManager.getIcon("OptionPane.warningIcon"),
	                options,
	                options[1]);
		    if (res == 0)
		    {
				Runtime.getRuntime().halt(0);
		    }
		}
		else
		{
			//System.exit(0); //this will wait for synch locks to the released
			// but here we want to really stop the JVM and kill all threads.
			Runtime.getRuntime().halt(0);
		}
	}
	
//-----------------------------------------------------------------------------
	
}
