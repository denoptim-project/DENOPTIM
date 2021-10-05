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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
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
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.io.DenoptimIO;
import denoptim.molecule.APClass;
import denoptim.molecule.EmptyVertex;
import gui.GUIPreferences.SMITo3DEngine;

public class GUIEmptyVertexMaker extends GUIModalDialog
{

	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 1L;
    
	private JPanel centralPanel;
	
    private JPanel lineAPsBtns;
    private JButton btnAPInsert;
    private JButton btnAPDelete;

    private JScrollPane apTabPanel;
    private JTable apTable;
    private DefaultTableModel apTabModel;
	

    //TODO: uncomment when properties will be enables
    /*
    private JPanel linePropBtns;
    private JButton btnPropInsert;
    private JButton btnPropDelete;
    
    private JScrollPane propTabPanel;
    private JTable propTable;
    private DefaultTableModel propTabModel;
	*/
    
//------------------------------------------------------------------------------

	public GUIEmptyVertexMaker()
	{
	    setTitle("Create Empty Vertex");
		centralPanel = new JPanel();
        centralPanel.setLayout(new BoxLayout(
        		centralPanel, SwingConstants.VERTICAL)); 
        
        JPanel titleAPs = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titleAPs.add(new JLabel("Attachment Points:"));
        centralPanel.add(titleAPs);

        apTabModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                if (column == 0)
                {
                    return false;
                }
                else
                {
                    return true;
                }
            }
        };
        apTabModel.setColumnCount(2);
        String column_names[]= {"<html><b>AP#</b></html>", "<html><b>APClass</b></html>"};
        apTabModel.setColumnIdentifiers(column_names);
        apTable = new JTable(apTabModel);
        apTable.putClientProperty("terminateEditOnFocusLost", true);
        apTable.getColumnModel().getColumn(0).setMaxWidth(75);
        apTable.setGridColor(Color.LIGHT_GRAY);
        JTableHeader apTabHeader = apTable.getTableHeader();
        apTabHeader.setPreferredSize(new Dimension(100, 20));
        apTabPanel = new JScrollPane(apTable);
        apTabPanel.setMaximumSize(new Dimension(400, 150));

        lineAPsBtns = new JPanel(new GridLayout(2, 2));
        btnAPInsert = new JButton("Add AP");
        btnAPInsert.setToolTipText("Click to add an attachment point.");
        btnAPInsert.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                String apClass;
                try {
                    apClass = GUIVertexInspector.ensureGoodAPClassString("",false);
                } catch (Exception e1) {
                    // We have pressed cancel or closed the dialog, so abandon
                    return;
                }
                apTabModel.addRow(new Object[]{apTabModel.getRowCount()+1,apClass});
            }
        });
        btnAPDelete = new JButton("Remove Selected");
        btnAPDelete.setToolTipText("Remove all selected APs from list.");
        btnAPDelete.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if (apTable.getRowCount() > 0)
                {
                    if (apTable.getSelectedRowCount() > 0)
                    {
                        int selectedRowIds[] = apTable.getSelectedRows();
                        Arrays.sort(selectedRowIds);
                        for (int i=(selectedRowIds.length-1); i>-1; i--)
                        {
                            apTabModel.removeRow(selectedRowIds[i]);
                        }
                    }
                }
            }
        });
        GroupLayout grpLyoAPs = new GroupLayout(lineAPsBtns);
        lineAPsBtns.setLayout(grpLyoAPs);
        grpLyoAPs.setAutoCreateGaps(true);
        grpLyoAPs.setAutoCreateContainerGaps(true);
        grpLyoAPs.setHorizontalGroup(grpLyoAPs.createSequentialGroup()
            .addGroup(grpLyoAPs.createParallelGroup()
                .addGroup(grpLyoAPs.createSequentialGroup()
                        .addComponent(btnAPInsert)
                        .addComponent(btnAPDelete))
                .addComponent(apTabPanel))
        );
        grpLyoAPs.setVerticalGroup(grpLyoAPs.createParallelGroup(
                GroupLayout.Alignment.CENTER)
            .addGroup(grpLyoAPs.createSequentialGroup()
                .addGroup(grpLyoAPs.createParallelGroup()
                    .addComponent(btnAPInsert)
                    .addComponent(btnAPDelete))
                .addComponent(apTabPanel))
        );
        centralPanel.add(lineAPsBtns);
        
        //TODO: uncomment when properties will be enables
        /*
        JPanel titleProps = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titleProps.add(new JLabel("Properties:"));
        titleProps.setToolTipText("Properties represent any general form data "
                + "contained in the vertex");
        centralPanel.add(titleProps);
        
        propTabModel = new DefaultTableModel();
        propTabModel.setColumnCount(2);
        String column_names_prop[]= {"<html><b>Name</b></html>", 
                "<html><b>Value</b></html>"};
        propTabModel.setColumnIdentifiers(column_names_prop);
        propTable = new JTable(propTabModel);
        propTable.putClientProperty("terminateEditOnFocusLost", true);
        propTable.getColumnModel().getColumn(0).setMaxWidth(75);
        propTable.setGridColor(Color.LIGHT_GRAY);
        propTable.getTableHeader().setPreferredSize(new Dimension(100, 20));
        propTabPanel = new JScrollPane(propTable);
        propTabPanel.setMaximumSize(new Dimension(400, 150));
        
        linePropBtns = new JPanel(new GridLayout(2, 2));
        btnPropInsert = new JButton("Add Property");
        btnPropInsert.setToolTipText("Click to add an property.");
        btnPropInsert.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                String propName = "noName";
                String propValue = "1.23456";
                //TODO: launch dialog to provide name and value
                propTabModel.addRow(new Object[]{propName,propValue});
            }
        });
        btnPropDelete = new JButton("Remove Selected");
        btnPropDelete.setToolTipText("Remove all selected properties from list.");
        btnPropDelete.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if (propTable.getRowCount() > 0) 
                {
                    if (propTable.getSelectedRowCount() > 0) 
                    {
                        int selectedRowIds[] = propTable.getSelectedRows();
                        Arrays.sort(selectedRowIds);
                        for (int i=(selectedRowIds.length-1); i>-1; i--) 
                        {
                            propTabModel.removeRow(selectedRowIds[i]);
                        }
                    }
                }
            }
        }); 
        GroupLayout grpLyoProps = new GroupLayout(linePropBtns);
        linePropBtns.setLayout(grpLyoProps);
        grpLyoProps.setAutoCreateGaps(true);
        grpLyoProps.setAutoCreateContainerGaps(true);
        grpLyoProps.setHorizontalGroup(grpLyoProps.createSequentialGroup()
            .addGroup(grpLyoProps.createParallelGroup()
                .addGroup(grpLyoProps.createSequentialGroup()
                        .addComponent(btnPropInsert)
                        .addComponent(btnPropDelete))
                .addComponent(propTabPanel))
        );
        grpLyoProps.setVerticalGroup(grpLyoProps.createParallelGroup(
                GroupLayout.Alignment.CENTER)
            .addGroup(grpLyoProps.createSequentialGroup()
                .addGroup(grpLyoProps.createParallelGroup()
                    .addComponent(btnPropInsert)
                    .addComponent(btnPropDelete))
                .addComponent(propTabPanel))
        );
        centralPanel.add(linePropBtns); 
        */       
        
		this.btnDone.setText("Create");
		this.btnDone.setToolTipText("<html>Create an empty vertex with the "
		        + "given attachment points<br>and properties</html>");
		this.btnDone.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
			    EmptyVertex ev = new EmptyVertex();
			    for (int i=0; i<apTabModel.getRowCount(); i++)
			    {
			        String apClass = apTabModel.getValueAt(i, 1).toString();
			        try
                    {
                        ev.addAP(-1,1,1, APClass.make(apClass));
                    } catch (DENOPTIMException e1)
                    {
                        e1.printStackTrace();
                        JOptionPane.showMessageDialog(btnDone,
                                "<html>Could not add attachment point with "
                                + "<br>APClass '" + apClass + "'. "
                                + "<br>Hint on cause: " + e1.getMessage() 
                                +"</html>",
                                "Error",
                                JOptionPane.ERROR_MESSAGE,
                                UIManager.getIcon("OptionPane.errorIcon"));
                    }
			    }
			    
			    //TODO : add props
			    
			    //TODO-GG isRCV
			    
			    result = ev;
			    close();
			}
		});
		
		this.btnCanc.setEnabled(true);
		this.btnCanc.setVisible(true);
		
		super.addToCentralPane(centralPanel);
	}
	
//------------------------------------------------------------------------------

}
