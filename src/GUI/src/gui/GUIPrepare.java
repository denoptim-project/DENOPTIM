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
 * Form containing all the input parameters for DenoptimGA.
 */

public class GUIPrepare extends GUIWorkPanel
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
	public GUIPrepare(JPanel mainPanel, String newPanelName)
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
		
		JButton btnSaveParams = new JButton("Save Parameters",UIManager.getIcon("FileView.hardDriveIcon"));
		btnSaveParams.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				StringBuilder sb = new StringBuilder();
				for (IParametersForm p : allParams)
				{
				    p.putParametersToString(sb);
				}
				File outFile = DenoptimGUIFileOpener.saveFile();
				try
				{
				    FileWriter fw = new FileWriter(outFile);
				    fw.write(sb.toString());
				    fw.close();
				}
				catch (IOException io)
				{
					JOptionPane.showMessageDialog(null,
			                "Could not same to '"+outFile+"'!.",
			                "Error",
			                JOptionPane.PLAIN_MESSAGE,
			                UIManager.getIcon("OptionPane.errorIcon"));
				}
			}
		});
		commandsPane.add(btnSaveParams);
		
		/*
		//TODO
	    JButton btnCreateInp = new JButton("Validate Parameters",UIManager.getIcon("OptionPane.warningIcon"));
		btnCreateInp.setToolTipText("Check the correctness of the parameters");
		//TODO: add action
		commandsPane.add(btnCreateInp);
		*/
		
		JButton btnClose = new JButton("?");
		btnClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//TODO
			}
		});
		commandsPane.add(btnClose);
		
		JButton btnHelp = new JButton("?");
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(null,
                        new JLabel("Hover over the fields to get a tip."),
                        "About DENOPTIM",
                        JOptionPane.PLAIN_MESSAGE);
			}
		});
		commandsPane.add(btnHelp);

	}
}
