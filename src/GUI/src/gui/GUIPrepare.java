package gui;

import java.awt.BorderLayout;
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
						return;
					}
				}
				File outFile = DenoptimGUIFileOpener.saveFile();
				if (outFile == null)
				{
					return;
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
				}
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

		JButton btnCanc = new JButton("Close Tab");
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
