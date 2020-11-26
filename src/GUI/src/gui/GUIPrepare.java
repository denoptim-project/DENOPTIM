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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import denoptim.task.GUIInvokedMainTask;
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
		commandsPane.setLayout(new GridBagLayout());
		GridBagConstraints cmdGridConstraints = new GridBagConstraints();
		super.add(commandsPane, BorderLayout.SOUTH);
		
		JButton btnLoadParams = new JButton("Load Parameters");
		// Adding an Icon overwrites the fontsize, no matter the setFont
		/*
		JButton btnLoadParams = new JButton("Load Parameters",
				UIManager.getIcon("FileView.directoryIcon"));
				*/
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
		cmdGridConstraints.gridx = 0;
		cmdGridConstraints.gridy = 0;
		cmdGridConstraints.ipadx = 5;
		cmdGridConstraints.ipady = 5;
		commandsPane.add(btnLoadParams,cmdGridConstraints);
		
		JButton btnSaveParams = new JButton("Save Parameters");
		// Adding an Icon overwrites the fontsize, no matter the setFont
		/*
		JButton btnSaveParams = new JButton("Save Parameters",
				UIManager.getIcon("FileView.hardDriveIcon"));
				*/
		btnSaveParams.setToolTipText("<html>Write all parameters to file."
				+ "<br>This will produce a DENOPTIM parameter file.</html>");
		btnSaveParams.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File outFile = DenoptimGUIFileOpener.saveFile();
				printAllParamsToFile(outFile);
			}
		});
		cmdGridConstraints.gridx = 1;
		commandsPane.add(btnSaveParams,cmdGridConstraints);
		
		/*
		//TODO		
	    JButton btnValidate = new JButton("Validate Parameters",
	    btnValidate.setToolTipText("Check the correctness of the parameters");
		btnValidate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//TODO
				getNonImplementedError();
			}
		});
		commandsPane.add(btnValidate);
		*/

		
		JButton btnRun = new JButton("Run now...");//,
		//		UIManager.getIcon("Menu.arrowIcon"));
		// Using the arrowIcon causes problems with adoptopenjdk-1.8
		// due to casting of the JButton into a JMenuItem. This could be
		// due to the fact thet the arrow icon is meant for a menu. 
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
							GUIInvokedMainTask task = buildMainCall();
							File wrkSpace = prepareWorkSpace();
							File paramFile = instatiateParametersFile(wrkSpace);
							if (printAllParamsToFile(paramFile))
							{
								task.setConfigFile(paramFile);
								task.setWorkSpace(wrkSpace);
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
		cmdGridConstraints.gridx = 2;
		commandsPane.add(btnRun,cmdGridConstraints);
		
		
		/*
		//
		//TODO? Right now it only prints the configuration file in a new folder
		//But, running the experiments requires a command from the user and 
		//from outside this JVM. Moreover, that command is platform specific.
		//
		JButton btnSubmit = new JButton("Submit...",
		btnSubmit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
					String location = "unknownLocation";
					try {
						File wrkSpace = prepareWorkSpace();
						File paramFile = instatiateParametersFile(wrkSpace);
						if (!printAllParamsToFile(paramFile))
						{
							throw new DENOPTIMException("Failed to make "
									+ "parameter file '" + paramFile + "'");
						}
						location = wrkSpace.getAbsolutePath();
					} catch (DENOPTIMException e1) {
						JOptionPane.showMessageDialog(null,
								"Could not prepare work space. " 
								+ e1.getMessage()
								+ ". " + e1.getCause().getMessage(),
			                    "ERROR",
			                    JOptionPane.ERROR_MESSAGE);
						return;
					}
					
					// We have only the .params file under the tmp space
					// Should get copies of all input files and place them under 
					// the same location.

					JOptionPane.showMessageDialog(null,
							"<html>The experiment is all set up under<br>"
							+ location+"</html>",
		                    "Prepared. Now, submit!",
		                    JOptionPane.INFORMATION_MESSAGE);
			}
		});
		commandsPane.add(btnSubmit);
		*/
		
		JButton btnCanc = new JButton("Close Tab");
		// Adding the icon overrites font size no matter setFont
		/*
		JButton btnCanc = new JButton("Close Tab", 
				UIManager.getIcon("FileView.fileIcon"));
		*/
		btnCanc.setToolTipText("Closes this tab.");
		btnCanc.addActionListener(new removeCardActionListener(this));
		cmdGridConstraints.gridx = 3;
		commandsPane.add(btnCanc,cmdGridConstraints);
		
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
		cmdGridConstraints.gridx = 4;
		commandsPane.add(btnHelp,cmdGridConstraints);
	}

//------------------------------------------------------------------------------

	/**
	 * This will get the dafault name of the JAR file with "Main" class defined
	 * for the kind of task we are submitting: DenoptimGA or FragSpaceEsplorer.
	 */
	private String getNameOfCorrespondingJAR()
	{
		String baseName = "some_name.jar";
		if (this instanceof GUIPrepareGARun)
		{
			baseName = "DenoptimGA";
		} else if (this instanceof GUIPrepareFSERun)
		{
			baseName = "FragSpaceExplorer";
		}
		return baseName;
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
	private GUIInvokedMainTask buildMainCall() throws DENOPTIMException
	{
		GUIInvokedMainTask task = null;
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
