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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

import denoptim.exception.DENOPTIMException;
import denoptim.files.FileUtils;
import denoptim.gui.GUIPreferences.MolDimDisplay;
import denoptim.gui.GUIPreferences.SMITo3DEngine;

public class GUIPreferencesDialog extends GUIModalDialog
{

	/**
	 * Version UID
	 */
	private static final long serialVersionUID = -1416475901274128714L;
	
    /**
     * Default sizes for file pathname labels
     */
    final Dimension fileLabelSize = new Dimension(250,28);
    
    /**
     * Default text field height
     */
    final int preferredHeight = 
    		(int) (new JTextField()).getPreferredSize().getHeight();
    
    /**
     * Default sizes for short pathname fields (i.e., string or number)
     */
    final Dimension strFieldSize = new Dimension(75,preferredHeight);
    
	private JPanel centralPanel;
	
	private String namMolDimDisplay = "Display mode for chemical structures";
	private JPanel pnlMolDimDisplay;
	private JLabel lblMolDimDisplay;
	private JComboBox<MolDimDisplay> cmbMolDimDisplay;
	
	private String namGraphTxtSize = "Font size for graph labels";
	private JPanel pnlGraphTxtSize;
	private JLabel lblGraphTxtSize;
	private JTextField txtGraphTxtSize;
	
    private String namGraphNodeSize = "Size of graph nodes";
    private JPanel pnlGraphNodeSize;
    private JLabel lblGraphNodeSize;
    private JTextField txtGraphNodeSize;
    
    private String namChartPointSize = "Size of points in evolution chart";
    private JPanel pnlChartPointSize;
    private JLabel lblChartPointSize;
    private JTextField txtChartPointSize;
    
    private String namEvoChartLegend = "Display legend in evolution chart";
    private JPanel pnlEvoChartLegend;
    private JRadioButton rcbEvoChartLegend;

    private String namMntChartLegend = "Display legend in monitor chart";
    private JPanel pnlMntChartLegend;
    private JRadioButton rcbMntChartLegend;

    private String namShow2DHydrogens = "Show H atoms in 2D structures";
    private JPanel pnlShow2DHydrogens;
    private JRadioButton rcbShow2DHydrogens;
    
    private JPanel linePropTags;
    private JLabel lblPropTags;
    private DefaultTableModel tabModPropTags;
    private JTable tabPropTags;
    private JButton btnPropTagsInsert;
    private JButton btnPropTagsCleanup;
    
    private String namTmpSpace = "Folder for tmp files";
    private JPanel pnlTmpSpace;
    private JLabel lblTmpSpace;
    private JTextField txtTmpSpace;
    
    private String namSMILESTo3D = "SMILES-to-3D converer";
    private JPanel pnlSMILESTo3D;
    private JComboBox<SMITo3DEngine> cmbSMILESTo3D;
    private JLabel lblSMILESTo3D;
    
	
	private boolean inputIsOK = true;
	
//------------------------------------------------------------------------------

	/**
	 * Constructs a dialog for setting the preferences of the GUI.
	 * @param refForPlacement the component used to place this dialog.
	 */
	@SuppressWarnings("serial")
    public GUIPreferencesDialog(Component refForPlacement)
	{
	    super(refForPlacement, true);
	    setTitle("Preferences");
		centralPanel = new JPanel();
        JScrollPane scrollablePane = new JScrollPane(centralPanel);
        centralPanel.setLayout(new BoxLayout(
        		centralPanel, SwingConstants.VERTICAL)); 

        JPanel titleGeneral = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titleGeneral.add(new JLabel("<html><b>General</b></html>"));
        centralPanel.add(titleGeneral);

        pnlTmpSpace = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblTmpSpace = new JLabel(namTmpSpace + ":");
        txtTmpSpace = new JTextField();
        txtTmpSpace.setPreferredSize(fileLabelSize);
        pnlTmpSpace.add(lblTmpSpace);
        pnlTmpSpace.add(txtTmpSpace);
        centralPanel.add(pnlTmpSpace);
        
        centralPanel.add(new JSeparator());
        
        JPanel titleFragments = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titleFragments.add(new JLabel("<html><b>Handling of fragments</b></html>"));
        centralPanel.add(titleFragments);
        
        pnlSMILESTo3D = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblSMILESTo3D = new JLabel(namSMILESTo3D + ":");
        cmbSMILESTo3D = new JComboBox<SMITo3DEngine>(SMITo3DEngine.values());
        cmbSMILESTo3D.setSelectedItem(GUIPreferences.smiTo3dResolver);
        cmbSMILESTo3D.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				GUIPreferences.smiTo3dResolver = (SMITo3DEngine) 
						cmbSMILESTo3D.getSelectedItem();
			}
		});
        pnlSMILESTo3D.add(lblSMILESTo3D);
        pnlSMILESTo3D.add(cmbSMILESTo3D);
        centralPanel.add(pnlSMILESTo3D);
        
        centralPanel.add(new JSeparator());
        
        JPanel titleGraphViewer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titleGraphViewer.add(new JLabel("<html><b>Graph visualization</b></html>"));
        centralPanel.add(titleGraphViewer);
        
        pnlGraphTxtSize = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblGraphTxtSize = new JLabel(namGraphTxtSize + ":");
        txtGraphTxtSize = new JTextField();
        txtGraphTxtSize.setPreferredSize(strFieldSize);
        pnlGraphTxtSize.add(lblGraphTxtSize);
        pnlGraphTxtSize.add(txtGraphTxtSize);
        centralPanel.add(pnlGraphTxtSize);
        
        pnlGraphNodeSize = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblGraphNodeSize = new JLabel(namGraphNodeSize + ":");
        txtGraphNodeSize = new JTextField();
        txtGraphNodeSize.setPreferredSize(strFieldSize);
        pnlGraphNodeSize.add(lblGraphNodeSize);
        pnlGraphNodeSize.add(txtGraphNodeSize);
        centralPanel.add(pnlGraphNodeSize);
        
        centralPanel.add(new JSeparator());
        
        JPanel titleMolViewer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titleMolViewer.add(new JLabel("<html><b>Chemical structure visualization</b></html>"));
        centralPanel.add(titleMolViewer);

        pnlMolDimDisplay = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblMolDimDisplay = new JLabel(namMolDimDisplay + ":");
        cmbMolDimDisplay = new JComboBox<MolDimDisplay>(MolDimDisplay.values());
        cmbMolDimDisplay.setSelectedItem(GUIPreferences.molDimDisplay);
        cmbMolDimDisplay.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GUIPreferences.molDimDisplay = (MolDimDisplay) cmbMolDimDisplay.getSelectedItem();
            }
        });
        pnlMolDimDisplay.add(lblMolDimDisplay);
        pnlMolDimDisplay.add(cmbMolDimDisplay);
        centralPanel.add(pnlMolDimDisplay);

        pnlShow2DHydrogens = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rcbShow2DHydrogens = new JRadioButton(namShow2DHydrogens);
        pnlShow2DHydrogens.add(rcbShow2DHydrogens);
        centralPanel.add(pnlShow2DHydrogens);
        
        String toolTipPropTags = "</html>Customizes the list of properties displayed together with the chemical representation of an item.</html>";
        linePropTags = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPropTags = new JLabel("Additional properties to display:", SwingConstants.LEFT);
        lblPropTags.setPreferredSize(fileLabelSize);
        lblPropTags.setToolTipText(toolTipPropTags);
        tabModPropTags = new DefaultTableModel() {
        	@Override
            public boolean isCellEditable(int row, int column) {
               return false;
            }
        };
        tabModPropTags.setColumnCount(1);
        tabPropTags = new JTable(tabModPropTags);
        tabPropTags.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        btnPropTagsInsert = new JButton("Add Property Name");
        btnPropTagsInsert.setToolTipText("Click to add the reference name or SDF tag of the desired property.");
        btnPropTagsInsert.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		String propName = (String)JOptionPane.showInputDialog(
	        				btnPropTagsInsert,
		                    "Specify the reference name or SDF tag of the "
		                    + "desired property",
		                    "Specify Property Name",
		                    JOptionPane.PLAIN_MESSAGE);
	
				if ((propName != null) && (propName.length() > 0) 
						&& !GUIPreferences.chosenSDFTags.contains(propName)) 
				{  
					tabModPropTags.addRow(new Object[]{propName});
					GUIPreferences.chosenSDFTags.add(propName);
				}    		
        	}
        });
        btnPropTagsCleanup = new JButton("Remove Selected");
        btnPropTagsCleanup.setToolTipText("Remove all selected entries from the list.");
        btnPropTagsCleanup.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		if (tabPropTags.getRowCount() > 0) 
        		{
        	        if (tabPropTags.getSelectedRowCount() > 0) 
        	        {
        	            int selectedRowIds[] = tabPropTags.getSelectedRows();
        	            Arrays.sort(selectedRowIds);
        	            for (int i=(selectedRowIds.length-1); i>-1; i--) 
        	            {
        	            	String val = tabModPropTags.getValueAt(
        	            			selectedRowIds[i], 0).toString();
        	            	GUIPreferences.chosenSDFTags.remove(val);
        	            	tabModPropTags.removeRow(selectedRowIds[i]);
        	            }
        	        }
        	    }
        	}
        });
        GroupLayout grpLyoPropTags = new GroupLayout(linePropTags);
        linePropTags.setLayout(grpLyoPropTags);
        grpLyoPropTags.setAutoCreateGaps(true);
        grpLyoPropTags.setAutoCreateContainerGaps(true);
        grpLyoPropTags.setHorizontalGroup(grpLyoPropTags.createSequentialGroup()
                .addComponent(lblPropTags)
                .addGroup(grpLyoPropTags.createParallelGroup()
                        .addGroup(grpLyoPropTags.createSequentialGroup()
                                        .addComponent(btnPropTagsInsert)
                                        .addComponent(btnPropTagsCleanup))
                        .addComponent(tabPropTags))
        );
        grpLyoPropTags.setVerticalGroup(grpLyoPropTags.createParallelGroup(
        		GroupLayout.Alignment.LEADING)
                .addComponent(lblPropTags)
                .addGroup(grpLyoPropTags.createSequentialGroup()
                        .addGroup(grpLyoPropTags.createParallelGroup()
                                .addComponent(btnPropTagsInsert)
                                .addComponent(btnPropTagsCleanup))
                        .addComponent(tabPropTags))
        );
        centralPanel.add(linePropTags);
        
        centralPanel.add(new JSeparator());
        
        JPanel titleEvolutionPlots = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titleEvolutionPlots.add(new JLabel("<html><b>Evolution run plots</b></html>"));
        centralPanel.add(titleEvolutionPlots);
        
        pnlChartPointSize = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblChartPointSize = new JLabel(namChartPointSize + ":");
        txtChartPointSize = new JTextField();
        txtChartPointSize.setPreferredSize(strFieldSize);
        pnlChartPointSize.add(lblChartPointSize);
        pnlChartPointSize.add(txtChartPointSize);
        centralPanel.add(pnlChartPointSize);
        
        pnlEvoChartLegend = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rcbEvoChartLegend = new JRadioButton(namEvoChartLegend);
        pnlEvoChartLegend.add(rcbEvoChartLegend);
        centralPanel.add(pnlEvoChartLegend);
        
        pnlMntChartLegend = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rcbMntChartLegend = new JRadioButton(namMntChartLegend);
        pnlMntChartLegend.add(rcbMntChartLegend);
        centralPanel.add(pnlMntChartLegend);
        
        refreshUIFromPreferences();
        
		// Customize the buttons of the modal dialog
		this.btnDone.setText("Save");
		this.btnDone.setToolTipText("Save the values to the preferences file "
		        + "and close dialog");
		this.btnDone.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				checkInput();
				if (inputIsOK)
				{
					storeValues();
					GUIPreferences.save();
					close();
				}
			}
		});
		
		this.btnExtra.setText("Restore defaults");
		this.btnExtra.setToolTipText("Restore factory-default preferences "
		        + "from the defaults file");
		this.btnExtra.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GUIPreferences.restoreDefaults();
                refreshUIFromPreferences();
            }
        });
		
		this.btnCanc.setEnabled(false);
		this.btnCanc.setVisible(false);
		
		super.addToCentralPane(scrollablePane);
	}

//-----------------------------------------------------------------------------

	/**
	 * Updates all dialog widgets from the current in-memory preferences.
	 */
	private void refreshUIFromPreferences()
	{
	    txtTmpSpace.setText(GUIPreferences.tmpSpace);
	    cmbSMILESTo3D.setSelectedItem(GUIPreferences.smiTo3dResolver);
	    txtGraphTxtSize.setText(GUIPreferences.graphLabelFontSize + "");
	    txtGraphNodeSize.setText(GUIPreferences.graphNodeSize + "");
	    cmbMolDimDisplay.setSelectedItem(GUIPreferences.molDimDisplay);
        rcbShow2DHydrogens.setSelected(GUIPreferences.show2DHydrogens);
	    while (tabModPropTags.getRowCount() > 0)
	    {
	        tabModPropTags.removeRow(0);
	    }
	    for (String propName : GUIPreferences.chosenSDFTags)
	    {
	        tabModPropTags.addRow(new Object[]{propName});
	    }
	    txtChartPointSize.setText(GUIPreferences.chartPointSize + "");
	    rcbEvoChartLegend.setSelected(GUIPreferences.showLegenInEvolutionPlot);
	    rcbMntChartLegend.setSelected(GUIPreferences.showLegenInMonitorPlot);
	}
	
//-----------------------------------------------------------------------------
	
	private void checkInput()
	{
		inputIsOK = true; //resetting results from previous attempts
		mustParseToInt(txtGraphTxtSize, namGraphTxtSize);
		mustParseToInt(txtGraphNodeSize, namGraphNodeSize);
		mustParseToInt(txtChartPointSize, namChartPointSize);
		pathMustBeReadableWritable(txtTmpSpace, namTmpSpace);
	}

//-----------------------------------------------------------------------------
	
	private void mustParseToInt(JTextField field, String name)
	{
		try {
			Integer.parseInt(field.getText());
		} catch (Exception e) {
			inputIsOK = false;
			JOptionPane.showMessageDialog(this,
					"<html>Unacceptable value for '" + name + "'<br>"
					+ "<br>The value should be an integer.</html>",
	                "Error",
	                JOptionPane.ERROR_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
		}
	}
	
//-----------------------------------------------------------------------------
	
	private void pathMustBeReadableWritable(JTextField field, String name)
	{
		String testFileName = field.getText()
				+ System.getProperty("file.separator") 
				+ "test";
		if (!FileUtils.canWriteAndReadTo(testFileName))
		{
			inputIsOK = false;
			JOptionPane.showMessageDialog(this,
					"<html>Unacceptable value for '" + name + "'<br>"
					+ "<br>The pathname should be readable and writable.</html>",
	                "Error",
	                JOptionPane.ERROR_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
		}
		try {
			FileUtils.deleteFile(testFileName);
		} catch (DENOPTIMException e) {
			e.printStackTrace();
		}
	}
	
//-----------------------------------------------------------------------------
	
	private void storeValues()
	{
		GUIPreferences.graphLabelFontSize = 
				Integer.parseInt(txtGraphTxtSize.getText());
		GUIPreferences.graphNodeSize =
                Integer.parseInt(txtGraphNodeSize.getText());
        GUIPreferences.chartPointSize =
                Integer.parseInt(txtChartPointSize.getText());
        GUIPreferences.tmpSpace = txtTmpSpace.getText();
        GUIPreferences.showLegenInEvolutionPlot = rcbEvoChartLegend.isSelected();
        GUIPreferences.showLegenInMonitorPlot = rcbMntChartLegend.isSelected();
        GUIPreferences.show2DHydrogens = rcbShow2DHydrogens.isSelected();
        // NB: other values are stored by the action listeners of the combo boxes

	}
}
