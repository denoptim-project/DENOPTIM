package gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;

public class CompatibilityMatrixForm extends JPanel {

	/**
	 * Version UID
	 */
	private static final long serialVersionUID = -8042143358823563589L;
	
    /**
     * Data structure that stored the true entries of the 
     * attachment point classes compatibility matrix
     */
    private HashMap<String, ArrayList<String>> compatMap = 
    			new HashMap<String, ArrayList<String>>(); 

    /**
     * Data structure that stores compatible APclasses for joining APs 
     * in ring-closing bonds. Symmetric, purpose specific
     * compatibility matrix.
     */
    private HashMap<String, ArrayList<String>> rcCompatMap = 
    			new HashMap<String, ArrayList<String>>();

    /**
     * Data structure that stores the correspondence between bond order
     * and attachment point class.
     */
    private HashMap<String, Integer> bondOrderMap =
    			new HashMap<String, Integer>();

    /**
     * Data structure that stores the AP-classes to be used to cap unused
     * APS on the growing molecule.
     */
    private HashMap<String, String> cappingMap = new HashMap<String, String>();

    /**
     * Data structure that stores AP classes that cannot be held unused
     */
    private ArrayList<String> forbiddenEndList = new ArrayList<String>();

	
	private JTabbedPane tabbedPane;
	private JPanel cpMapPanel;
	private JPanel boMapPanel;
	private JPanel capPanel;
	private JPanel panelFrbEnd;

	private DefaultTableModel tabModFrbEnd;
	private JTable tableFrbEnd;
	private Set<String> apsInFrbEndTable = new HashSet<String>();
	private JButton btnAddFrbEnd;
	private JButton btnDelFrbEnd;

	
//-----------------------------------------------------------------------------
	
	public CompatibilityMatrixForm()
	{
		this.setLayout(new BorderLayout());
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		super.add(tabbedPane, BorderLayout.CENTER);
		
		cpMapPanel = new JPanel();
		tabbedPane.addTab("APClass compatibility",null,cpMapPanel,null);
		
		boMapPanel = new JPanel();
		tabbedPane.addTab("APClass-to-Bond",null,boMapPanel,null);
		
		capPanel = new JPanel();
		tabbedPane.addTab("Capping",null,capPanel,null);
		
		// Forbidden ends panel
		panelFrbEnd = new JPanel(new BorderLayout());
		tabbedPane.addTab("Forbidden ends",null,panelFrbEnd,null);
		
		String toolTipFrbEnd = String.format("<html><body width='%1s'>Graphs "
				+ "holding free "
				+ "(i.e., unsaturated) attachment point with these "
				+ "APClasses are considered incomplete and are not "
				+ "submitted to fitness evaluation.</html>",300);
		
		tabModFrbEnd = new DefaultTableModel() {
			@Override
		    public boolean isCellEditable(int row, int column) {
				return false;
		    }
		};
        tabModFrbEnd.setColumnCount(1);
        String column_name[]= {"<html><b>APClasses defining the forbidden "
        		+ "ends:</b></html>"};
        tabModFrbEnd.setColumnIdentifiers(column_name);
        tableFrbEnd = new JTable(tabModFrbEnd);
        tableFrbEnd.setToolTipText(toolTipFrbEnd);
        
        btnAddFrbEnd = new JButton("Add Forbidden End");
        btnAddFrbEnd.setToolTipText("Add a new line in the list");
        btnAddFrbEnd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
        		boolean done = false;
        		while (!done)
        		{
        			// Get the APClasses we can choose to add
        			ArrayList<String> availableAPCls = new ArrayList<String>();
        			for (String apc : bondOrderMap.keySet())
        			{
        				if (!apsInFrbEndTable.contains(apc))
        					availableAPCls.add(apc);
        			}
        			String[] apClasses = new String[availableAPCls.size()];
        			for(int i=0; i<availableAPCls.size(); i++)
        				apClasses[i] = availableAPCls.get(i);
        			
        			// Ask user to choose
        			String apClass = (String)JOptionPane.showInputDialog(
	        		                    null,
	        		                    "Choose attachment point class:",
	        		                    "APClass",
	        		                    JOptionPane.PLAIN_MESSAGE,
	        		                    null,
	        		                    apClasses,
	        		                    null);
	
	        		if ((apClass != null) && (apClass.length() > 0)) 
	        		{  
	        			//TODO 
	        			System.out.println("Adding "+apClass);
        				tabModFrbEnd.addRow(new Object[]{apClass});
        				apsInFrbEndTable.add(apClass);
	        		}
	        		if (apClass == null)
	        		{
	        			done = true;
	        		}
        		}
			}
		});
        
        btnDelFrbEnd = new JButton("Remove Selected");
        btnDelFrbEnd.setToolTipText("Remove all the selected lines in the list");
        btnDelFrbEnd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
        		if (tableFrbEnd.getRowCount() > 0) 
        		{
        	        if (tableFrbEnd.getSelectedRowCount() > 0) 
        	        {
        	            int selectedRowIds[] = tableFrbEnd.getSelectedRows();
        	            Arrays.sort(selectedRowIds);
        	            for (int i=(selectedRowIds.length-1); i>-1; i--) 
        	            {
        	            	apsInFrbEndTable.remove(tableFrbEnd.getValueAt(
        	            			selectedRowIds[i], 0));
        	            	tabModFrbEnd.removeRow(selectedRowIds[i]);
        	            }
        	        }
        	    }
			}
		});
        
        JPanel panelBtnFrbEnd = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelBtnFrbEnd.add(btnAddFrbEnd);
        panelBtnFrbEnd.add(btnDelFrbEnd);
        panelFrbEnd.add(panelBtnFrbEnd, BorderLayout.NORTH);
        
        JScrollPane panelFrbEndTable = new JScrollPane(tableFrbEnd);
        panelFrbEnd.add(panelFrbEndTable, BorderLayout.CENTER);
		
	}
	
//-----------------------------------------------------------------------------

	protected void importCPMapFromFile(File inFile)
	{
		//TODO del
		System.out.println("Importing file: "+inFile);
		
		//Read data from file
        compatMap = new HashMap<String,ArrayList<String>>();
        bondOrderMap = new HashMap<String,Integer>();
        cappingMap = new HashMap<String,String>();
        forbiddenEndList = new ArrayList<String>();
        try {
			DenoptimIO.readCompatibilityMatrix(inFile.getAbsolutePath(),
						compatMap,
						bondOrderMap,
						cappingMap,
						forbiddenEndList);
		} catch (DENOPTIMException e) {
			JOptionPane.showMessageDialog(null,
					"<html>Could not read compatibility matrix data from "
					+ "file<br>'" + inFile + "'</html>",
	                "Error",
	                JOptionPane.WARNING_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;
		}
        
        //Place data into GUI
        //updateForbiddenEndsPanel();
	}
	
//-----------------------------------------------------------------------------
	
	
	
//-----------------------------------------------------------------------------

	private void deprotectEditedSystem()
	{
		//TODO
	}
	
//-----------------------------------------------------------------------------
	
	private void protectEditedSystem()
	{
		//TODO
	}

//-----------------------------------------------------------------------------
	
}
