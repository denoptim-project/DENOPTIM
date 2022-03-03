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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import org.apache.commons.cli.CommandLine;

import denoptim.files.FileFormat;
import denoptim.files.FileUtils;
import denoptim.main.CLIOptions;
import denoptim.task.StaticTaskManager;

/**
 * Graphical User Interface of the DENOPTIM package.
 * This GUI aims to facilitate:
 * <ul>
 * <li>creation of the various types of DENOPTIM input files,</li>
 * <li>execution of GUI-controlled runs,</li>
 * <li>reuse and modification of DENOPTIM input files,</li>
 * <li>inspection of results produced by DENOPTIM runs.</li>
 * </ul>
 * 
 * @author Marco Foscato
 */

public class GUI implements Runnable
{
    /**
     * GUI window frame
     */
	private JFrame frame;
	
	/**
	 * Panel including tool bar
	 */
	private JPanel framePane;
	
	/**
	 * Panel including all but tool bar
	 */
	private GUIMainPanel mainPanel;

	/**
	 * Default width
	 */
	private int width = 900;
	
	/**
	 * Default height
	 */
	private int height = 650;
	
	/**
	 * Command line options
	 */
	private CommandLine cmd;
	
//------------------------------------------------------------------------------
	
	/**
	 * Constructor that specifies parameters
	 */
	public GUI(CommandLine cmd) 
	{
	    this.cmd = cmd;
		ToolTipManager.sharedInstance().setDismissDelay(6000);
		ToolTipManager.sharedInstance().setInitialDelay(1000);
		ToolTipManager.sharedInstance().setReshowDelay(100);
	}
	
//------------------------------------------------------------------------------
	   
    /**
     * Launch the application.
     */
    public void run()
    {
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
        
        try {
            this.frame.pack();
            this.frame.setLocationRelativeTo(null);
            this.frame.setVisible(true);
            this.frame.addWindowFocusListener(new WindowFocusListener() {
                
                @Override
                public void windowLostFocus(WindowEvent e) {
                    StaticTaskManager.queueStatusBar.repaint();
                }
                
                @Override
                public void windowGainedFocus(WindowEvent e) {
                    StaticTaskManager.queueStatusBar.repaint();
                }
            });
            
            if (cmd!=null && cmd.getArgList().size()>0)
            {
                for (String arg : cmd.getArgList())
                {   
                    File file = new File(arg);
                    FileFormat format = FileFormat.UNRECOGNIZED;
                    try
                    {
                        format = FileUtils.detectFileFormat(file);
                    } catch (Throwable t)
                    {
                        // We have ensured the format is recognized in the Main
                    }
                    menuBar.openFile(file, format);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
//------------------------------------------------------------------------------
    
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
	
//------------------------------------------------------------------------------
	
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
			int res = JOptionPane.showOptionDialog(null,
			        "Found unsaved changes. "
			        + "Are you sure you want to close this window?",
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
			//System.exit(0); //this will wait for synch locks to be released,
			// but here we want to really stop the JVM and kill all threads.
			Runtime.getRuntime().halt(0);
		}
	}
	
//------------------------------------------------------------------------------
	
}
