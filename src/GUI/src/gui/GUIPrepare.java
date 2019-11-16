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
 * Class representing the general structure of a form specifying a specific
 * set of parameters.
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
	private void initialize() {
		
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
				for (IParametersForm p : allParams)
				{
				    try
				    {
						p.importParametersFromDenoptimParamsFile(
								inFile.getAbsolutePath());
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

		JButton btnCanc = new JButton("Cancel");
		btnCanc.setToolTipText("Abandon form without saving.");
		btnCanc.addActionListener(new removeCardActionListener(this));
		commandsPane.add(btnCanc);
		
		JButton btnHelp = new JButton("?");
		btnHelp.setToolTipText("Help");
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(null,
                    new JLabel("<html>Hover over the buttons and parameter"
                    		+ "fields to get a tip.</html>"),
                    "Tips",
                    JOptionPane.PLAIN_MESSAGE);
			}
		});
		commandsPane.add(btnHelp);

	}
	
	private class removeCardActionListener implements ActionListener
	{
		private GUICardPanel parentPanel;
		
		public removeCardActionListener(GUICardPanel panel)
		{
			this.parentPanel = panel;
		}
		
		public void actionPerformed(ActionEvent e) 
		{
			Object[] options = {"Yes","No"};
			int res = JOptionPane.showOptionDialog(null,
                "<html>Abandon without saving?"
                + "<br>Press NO to go back.</html>",
                "Abandon?",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                UIManager.getIcon("OptionPane.warningIcon"),
                options,
                options[1]);
			if (res == 0)
			{
				mainPanel.removeCard(parentPanel);
			}
		}
	}
}
