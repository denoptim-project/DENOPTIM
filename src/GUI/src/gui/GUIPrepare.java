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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.task.DenoptimGATask;
import denoptim.task.DummyTask;
import denoptim.task.FragSpaceExplorerTask;
import denoptim.task.StaticTaskManager;
import denoptim.task.Task;

/**
 * Class representing the general structure of a form including a specific
 * set of parameter collections. Each parameter collection is a tab in a
 * set of tabs (i.e., a tabbed pane).
 * 
 * @author Marco Foscato
 */

public class GUIPrepare extends GUICardPanel
{
	
	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 6481647840284906676L;

	/**
	 * Parameters for the various components are divided in TABs
	 */
	protected JTabbedPane tabbedPane;

	/**
	 * Storage of parameters
	 */
	public ArrayList<IParametersForm> allParams;

	/**
	 * Constructor
	 */
	public GUIPrepare(GUIMainPanel mainPanel, String newPanelName)
	{
		super(mainPanel, newPanelName);
		super.setLayout(new BorderLayout());
		this.allParams = new ArrayList<IParametersForm>();
		initialize();
	}

	/**
	 * Initialize the panel with tabbedPane and buttons.
	 */
	private void initialize() 
	{
		
		// Parameters for the various components are divided in TABs
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		super.add(tabbedPane, BorderLayout.CENTER);
		
		// Buttons go below the tabs
		JPanel commandsPane = new JPanel();
		super.add(commandsPane, BorderLayout.SOUTH);
		
		JButton btnLoadParams = new JButton("Load Parameters",
				UIManager.getIcon("FileView.directoryIcon"));
		btnLoadParams.setToolTipText("<html>Reads a DENOPTIM parameter file,"
				+ "<br>and imports parameters into the form.</html>");
		btnLoadParams.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File inFile = DenoptimGUIFileOpener.pickFile();
				if (inFile == null || inFile.getAbsolutePath().equals(""))
				{
					return;
				}
				
				importParametersFromDenoptimParamsFile(inFile);
				
				for (IParametersForm p : allParams)
			    {
			    	p.setUnsavedChanges(false);
			    }
			}
		});
		commandsPane.add(btnLoadParams);
		
		JButton btnSaveParams = new JButton("Save Parameters",
				UIManager.getIcon("FileView.hardDriveIcon"));
		btnSaveParams.setToolTipText("<html>Write all parameters to file."
				+ "<br>This will produce a DENOPTIM parameter file.</html>");
		btnSaveParams.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File outFile = DenoptimGUIFileOpener.saveFile();
				printAllParamsToFile(outFile);
			}
		});
		commandsPane.add(btnSaveParams);
		
		/*
		//TODO		
	    JButton btnValidate = new JButton("Validate Parameters",
	    UIManager.getIcon("CheckBoxMenuItem.checkIcon"));
	    btnValidate.setToolTipText("Check the correctness of the parameters");
		btnValidate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//TODO
				getNonImplementedError();
			}
		});
		commandsPane.add(btnValidate);
		*/

		
		JButton btnRun = new JButton("Run...",
				UIManager.getIcon("Menu.arrowIcon"));
		btnRun.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String msg = "<html><body width='%1s'><p>Running a DENOPTIM "
						+ "experiment from the graphical user interface "
						+ "(GUI) makes it dependent on "
						+ "the GUI itself. Therefore, if the GUI is closed "
						+ "or shut down, "
						+ "the experiment will be terminated as well.</p><br>";
				msg = msg + StaticTaskManager.getQueueSnapshot();
				msg = msg + "<p>Continue?</p></body></html>";
				String[] options = new String[]{"Yes", "Cancel"};
				int res = JOptionPane.showOptionDialog(null,
						String.format(msg, 450),
						"WARNING",
						JOptionPane.DEFAULT_OPTION,
		                JOptionPane.QUESTION_MESSAGE,
		                UIManager.getIcon("OptionPane.warningIcon"),
		                options,
		                options[1]);
				switch (res)
				{
					case 0:
						String location = "unknownLocation";
						try {
							Task task = buildTaskRunningDenoptimMainClass();
							File wrkSpace = prepareWorkSpace();
							File paramFile = instatiateParametersFile(wrkSpace);
							if (printAllParamsToFile(paramFile))
							{
								StaticTaskManager.submit(task);
							} else {
								throw new DENOPTIMException("Failed to make "
										+ "parameter file '" + paramFile + "'");
							}
							location = wrkSpace.getAbsolutePath();
						} catch (DENOPTIMException e1) {
							JOptionPane.showMessageDialog(null,
									"Could not start task. " + e1.getMessage()
									+ ". " + e1.getCause().getMessage(),
				                    "ERROR",
				                    JOptionPane.ERROR_MESSAGE);
							return;
						}
						JOptionPane.showMessageDialog(null,
								"<html>Experiment submitted!<br>"
								+ "See under " + location+"</html>",
			                    "Submitted",
			                    JOptionPane.INFORMATION_MESSAGE);
						break;
						
					case 1:
						break;
				}
			}
		});
		commandsPane.add(btnRun);
		
		
		/*
		JButton btnSubmit = new JButton("Submit...",
		UIManager.getIcon("Menu.arrowIcon"));
		btnSubmit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//TODO
				getNonImplementedError();
			}
		});
		commandsPane.add(btnSubmit);
		*/

		JButton btnCanc = new JButton("Close Tab", 
				UIManager.getIcon("FileView.fileIcon"));
		btnCanc.setToolTipText("Closes this tab.");
		btnCanc.addActionListener(new removeCardActionListener(this));
		commandsPane.add(btnCanc);
		
		JButton btnHelp = new JButton("?");
		btnHelp.setToolTipText("Help");
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String txt = "<html><body width='%1s'>"
	                    + "<p>This tab allows to create, inspect, and edit "
	                    + "parameter used as input for DENOPTIM experiments. "
	                    + "These parameters are then collected into an input "
	                    + "file for DENOPTIM.</p>"
	                    + "<br>"
	                    + "<p>Hover over buttons and parameter fields to get "
	                    + "informations on a specific parameter.</p></html>";
				JOptionPane.showMessageDialog(null, 
						String.format(txt, 350),
	                    "Tips",
	                    JOptionPane.PLAIN_MESSAGE);
			}
		});
		commandsPane.add(btnHelp);
	}
	
//------------------------------------------------------------------------------
	
	private String getAchronimFromClass()
	{
		String baseName = "none";
		if (this instanceof GUIPrepareGARun)
		{
			baseName = "GA";
		} else if (this instanceof GUIPrepareFSERun)
		{
			baseName = "FSE";
		}
		return baseName;
	}
	
//------------------------------------------------------------------------------
	
	private File instatiateParametersFile(File wrkSpace)
	{
		String baseName = getAchronimFromClass() + ".params";
		File paramFile = new File (wrkSpace.getAbsolutePath() 
				+ System.getProperty("file.separator") + baseName);
		return paramFile;
	}

//------------------------------------------------------------------------------
	
	/**
	 * @param task that will make use of the parameters printed by this method.
	 * @param outFile where we'll try to print the parameters.
	 * @return <code>false</code> if we could not produce the file
	 */
	private boolean printAllParamsToFile(File outFile)
	{	
		StringBuilder sb = new StringBuilder();
		for (IParametersForm p : allParams)
		{
		    try 
		    {
				p.putParametersToString(sb);
			} 
		    catch (Exception e1) 
		    {
				JOptionPane.showMessageDialog(null,
		                e1.getMessage(),
		                "Error",
		                JOptionPane.ERROR_MESSAGE,
		                UIManager.getIcon("OptionPane.errorIcon"));
				return false;
			}
		}
		
		// It might be coming from a JOptionPane, which might return null
		// upon user's attempt to cancel the printing task.
		if (outFile == null)
		{
			return false;
		}
		
		try
		{
		    FileWriter fw = new FileWriter(outFile);
		    fw.write(sb.toString());
		    fw.close();
		    
		    for (IParametersForm p : allParams)
		    {
		    	p.setUnsavedChanges(false);
		    }
		}
		catch (IOException io)
		{
			JOptionPane.showMessageDialog(null,
	                "Could not write to '" + outFile + "'!.",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return false;
		}
		return true;
	}
	
//------------------------------------------------------------------------------
	
	/**
	 * The type of main to run is determined by which subclass calls this method
	 * @throws DENOPTIMException 
	 */
	private Task buildTaskRunningDenoptimMainClass() throws DENOPTIMException
	{
		Task task = null;
		if (this instanceof GUIPrepareGARun)
		{
			task = new DenoptimGATask();
		} else if (this instanceof GUIPrepareFSERun)
		{
			task = new FragSpaceExplorerTask();
		}
		return task;
	}
	
//------------------------------------------------------------------------------
	
	public File prepareWorkSpace() throws DENOPTIMException
	{
		String baseName = getAchronimFromClass() + "_run";
		File parent = new File(GUIPreferences.tmpSpace);
		File wrkSpace = DenoptimIO.getAvailableFileName(parent, baseName);
		DenoptimIO.createDirectory(wrkSpace.getAbsolutePath());
		return wrkSpace;
	}
//------------------------------------------------------------------------------
	
	public void importParametersFromDenoptimParamsFile(File file)
	{
		for (IParametersForm p : allParams)
		{
		    try
		    {
				p.importParametersFromDenoptimParamsFile(
						file.getAbsolutePath());
			}
		    catch (Exception e1)
		    {
        		if (e1.getMessage().equals("") 
        				|| e1.getMessage() == null)
        		{
        			e1.printStackTrace();
					JOptionPane.showMessageDialog(null,
							"<html>Exception occurred while importing"
							+ "parameters.<br>Please, report this to "
							+ "the DENOPTIM team.</html>",
			                "Error",
			                JOptionPane.ERROR_MESSAGE,
			                UIManager.getIcon("OptionPane.errorIcon"));
        		}
        		else
        		{
					JOptionPane.showMessageDialog(null,
							e1.getMessage(),
			                "Error",
			                JOptionPane.ERROR_MESSAGE,
			                UIManager.getIcon("OptionPane.errorIcon"));
        		}
			}
		}
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Check whether any of the parameter forms (i.e., a tab) in this list of
	 * tabs has unsaved changes.
	 * @return <code>true</code> if there are unsaved changes to the forms.
	 */
	
	public boolean hasUnsavedChanges()
	{
		boolean res = false;
		for (IParametersForm p : allParams)
		{
			if (p.hasUnsavedChanges())
			{
				res = true;
				break;
			}
		}
		return res;
	}
	
//-----------------------------------------------------------------------------
	
}
