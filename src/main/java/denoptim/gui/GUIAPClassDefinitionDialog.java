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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

import org.apache.commons.lang3.Validate;
import org.xmlcml.cml.element.AbstractActionList;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.files.FileUtils;
import denoptim.graph.APClass;
import denoptim.graph.DENOPTIMEdge.BondType;
import gui.GUIPreferences.SMITo3DEngine;

public class GUIAPClassDefinitionDialog extends GUIModalDialog
{

	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 3L;
	
    /**
     * Default sizes for file pathname labels
     */
    final Dimension fileLabelSize = new Dimension(250,28);
    
    /**
     * Default text field height
     */
    final int preferredHeight = 
    		(int) (new JTextField()).getPreferredSize().getHeight();
    
    final Dimension strFieldSize = new Dimension(500,preferredHeight);
    
	private JPanel centralPanel;
	
	private String apcStringSyntax = "The valid syntax for APClass "
            + "strings is:<br><br><code>rule" 
            + DENOPTIMConstants.SEPARATORAPPROPSCL 
            + "subClass</code><br><br> where "
            + "<ul><li><code>rule</code>"
            + " is a string with no spaces</li>"
            + "<li><code>subClass</code> is "
            + "an integer</li></ul><br>";
	
	private JPanel pnlAPCName;
	private JLabel lblAPCName;
	private JTextField txtAPCName;
    
    private JPanel pnlAPC2BO;
    private JComboBox<BondType> cmbAPC2BO;
    private JLabel lblAPC2BO;
    
	
//------------------------------------------------------------------------------

	public GUIAPClassDefinitionDialog(JComponent parent, boolean askForBO)
	{
        setLocationRelativeTo(parent);
	    setTitle("APClass Definition");
		centralPanel = new JPanel();
        centralPanel.setLayout(new BoxLayout(
        		centralPanel, SwingConstants.VERTICAL));
        
        pnlAPCName = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblAPCName = new JLabel("APClass: ");
        lblAPCName.setToolTipText("<html>"+apcStringSyntax+"</html>");
        txtAPCName = new JTextField();
        txtAPCName.setPreferredSize(strFieldSize);
        txtAPCName.setToolTipText("<html>"+apcStringSyntax+"</html>");
        pnlAPCName.add(lblAPCName);
        pnlAPCName.add(txtAPCName);
        centralPanel.add(pnlAPCName);
        
        pnlAPC2BO = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblAPC2BO = new JLabel("Bond type: ");
        String tt = "<html>Chose a type of bond to generate from "
                + "<br>attachment points belonging to this APClass.</html>";
        lblAPC2BO.setToolTipText(tt);
        cmbAPC2BO = new JComboBox<BondType>(BondType.values());
        cmbAPC2BO.setToolTipText(tt);
        cmbAPC2BO.setSelectedItem(APClass.DEFAULTBT);
        if (askForBO)
        {
            pnlAPC2BO.add(lblAPC2BO);
            pnlAPC2BO.add(cmbAPC2BO);
            centralPanel.add(pnlAPC2BO);
        }

        addToCentralPane(centralPanel);
        
		// Customise the buttons of the modal dialog
		this.btnDone.setText("OK");
		this.btnDone.setToolTipText("Confirm the definition of the APClass");
		for (ActionListener al : this.btnDone.getActionListeners())
		    this.btnDone.removeActionListener(al);
		this.btnDone.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
			    String currApClass = txtAPCName.getText();
			    if (!APClass.isValidAPClassString(currApClass))
			    {
			        String msg = "<html>'" + currApClass + "' is not a valid "
			                + "string for making an APClass.<br>"
			                + apcStringSyntax
			                + "Please, provide a valid string.</html>";

                    JOptionPane.showConfirmDialog(btnDone, msg, "Invalid Input",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
			    } else {
			        Object[] choice = new Object[2];
			        choice[0] = currApClass;
			        choice[1] = cmbAPC2BO.getSelectedItem();
			        result = choice;
			        close();
			    }
			}
		});
		
		this.btnCanc.setEnabled(false);
		this.btnCanc.setVisible(false);
		
		pack();
	}

//------------------------------------------------------------------------------

}
