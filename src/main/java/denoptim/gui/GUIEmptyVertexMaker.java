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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import denoptim.exception.DENOPTIMException;
import denoptim.graph.APClass;
import denoptim.graph.EmptyVertex;
import denoptim.graph.Vertex.BBType;

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
	
    private JPanel lineBBType;
    private JComboBox<BBType> cmbBBType;
    
    private JPanel lineRCV;
    private JRadioButton rcbIsRCV;

    //TODO: uncomment when properties will be enables
    /*
    private JPanel linePropBtns;
    private JButton btnPropInsert;
    private JButton btnPropDelete;
    
    private JScrollPane propTabPanel;
    private JTable propTable;
    private DefaultTableModel propTabModel;
	*/
    
    private boolean canBeScaffold = true;
    

//------------------------------------------------------------------------------

    /**
     * Constructs a maker that can make empty vertexes on a specific 
     * {@link BBType} and does not allow to change such type.
     * @param parent the parent component used to place the dialog in 
     * proximity of the parent.
     * @param givenType the given type of the generated vertexes.
     */
    public GUIEmptyVertexMaker(Component parent, BBType givenType)
    {
        this(parent, false);
        BBType[] bt = new BBType[1];
        bt[0] = givenType;
        cmbBBType.setModel(new DefaultComboBoxModel<BBType>(bt));
        cmbBBType.setEnabled(false);
    }

//------------------------------------------------------------------------------

    /**
     * Constructs a maker that can make empty vertexes freely.
     * @param parent the parent component used to place the dialog in 
     * proximity of the parent.
     */
    public GUIEmptyVertexMaker(Component parent)
    {
        this(parent, true);
    }
    
//------------------------------------------------------------------------------

    /**
     * Constructs a maker that can make empty vertexes, but controls whether
     * the generated vertexes are allowed to be {@link BBType#SCAFFOLD}.
     * @param parent the parent component used to place the dialog in 
     * proximity of the parent.
     */
    public GUIEmptyVertexMaker(Component parent, boolean canBeScaffold)
    {
        this.canBeScaffold = canBeScaffold;
	    setLocationRelativeTo(parent);
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
                List<APClass> selectedAPCs = 
                        GUIVertexInspector.choseOrCreateNewAPClass(btnAPInsert,
                                false);
                for (APClass apc : selectedAPCs)
                {
                    apTabModel.addRow(new Object[]{apTabModel.getRowCount()+1,
                            apc.toString()});
                }
                updateBBTypeBasedOnAPCount();
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
                        updateBBTypeBasedOnAPCount();
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
        

        cmbBBType = new JComboBox<BBType>();
        updateBBTypeBasedOnAPCount();
        cmbBBType.setToolTipText(String.format("<html><body width='%1s'>"
                + "Speicfy the type of building block of the vertex. This"
                + "determines, for instance, the type of other vertexes "
                + "that can be used to replace thi one (if such mutation "
                + "is permitted).</html>",300));
        lineBBType = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lineBBType.add(cmbBBType);
        centralPanel.add(lineBBType);
        
        
        rcbIsRCV = new JRadioButton("ring-closing vertex");
        rcbIsRCV.setToolTipText("Select to mark this vertex as a ring-closing "
                + "vertex");
        lineRCV = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lineRCV.add(rcbIsRCV);
        centralPanel.add(lineRCV);
        
        
        
        
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
			    EmptyVertex ev = new EmptyVertex(BBType.valueOf(
			            cmbBBType.getSelectedItem().toString()));
			    for (int i=0; i<apTabModel.getRowCount(); i++)
			    {
			        String apClass = apTabModel.getValueAt(i, 1).toString();
			        try
                    {
                        ev.addAP(APClass.make(apClass));
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
			    
			    ev.setAsRCV(rcbIsRCV.isSelected());
			    
			    result = ev;
			    close();
			}
		});
		
		this.btnCanc.setEnabled(true);
		this.btnCanc.setVisible(true);
		
		super.addToCentralPane(centralPanel);
	}
	
//------------------------------------------------------------------------------
	
	private void updateBBTypeBasedOnAPCount()
	{
	    List<BBType> collection = new ArrayList<BBType>();
	    
	    if (canBeScaffold)
	    {
	        collection.add(BBType.SCAFFOLD);
	        for (int i=0; i<BBType.values().length; i++)
	            if (!collection.contains(BBType.values()[i]))
	                collection.add(BBType.values()[i]);
	    } else {
    	    switch (apTabModel.getRowCount())
    	    {
    	        case 0:
    	            collection.add(BBType.UNDEFINED);
    	            break;
    	            
    	        case 1:
                    collection.add(BBType.CAP);
                    for (int i=0; i<BBType.values().length; i++)
                    {
                        BBType cand = BBType.values()[i];
                        if (!collection.contains(cand)
                                && !cand.equals(BBType.SCAFFOLD))
                            collection.add(cand);
                    }
                    break;
                    
    	        default:
    	            collection.add(BBType.FRAGMENT);
                    for (int i=0; i<BBType.values().length; i++)
                    {
                        BBType cand = BBType.values()[i];
                        if (!collection.contains(cand)
                                && !cand.equals(BBType.SCAFFOLD)
                                && !cand.equals(BBType.CAP))
                            collection.add(cand);
                    }
                    break;
    
    	    }
	    }
	    BBType[] types = new BBType[collection.size()];
	    for (int i=0; i<collection.size(); i++)
        {
	        types[i] = collection.get(i);
        }new DefaultComboBoxModel<BBType>(types);
        cmbBBType.setModel(new DefaultComboBoxModel<BBType>(types));
	}
	
//------------------------------------------------------------------------------

}
