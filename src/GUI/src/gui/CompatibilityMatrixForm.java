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
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;

public class CompatibilityMatrixForm extends JPanel {

	/**
	 * Version UID
	 */
	private static final long serialVersionUID = -8042143358823563589L;
	
	/**
	 * List of all APClasses.
	 * These can be found in a fragment space or collected from
	 * fragment libraries
	 */
	private SortedSet<String> allAPClasses = new TreeSet<String>();
	
	/**
	 * List of APClasses of capping groups.
	 * These can be found in a fragment space or collected from
	 * the capping group library
	 */
	private SortedSet<String> allCapAPClasses = new TreeSet<String>();
	
	/**
	 * Sorted list of APClasses in the map of compatibility rules
	 */
	private SortedSet<String> allAPClsInCPMap = new TreeSet<String>();
	
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
    private Set<String> forbiddenEndList = new HashSet<String>();

    
    /**
     * Maximum bond order accepted in APClass-to-BO map
     */
    private final int MAXBO = 4;
	
	private JTabbedPane tabbedPane;
	private JPanel panelCPMap;
	private JButton btnAddCompRul;
	private JButton btnDelCompRul;
	private JButton btnCopyCompRul;
	private JButton btnHelpCPMap;
	private JPanel panelCPRules;
	private JScrollPane scrollPanelCPMap;
	
	private JPanel panelAPClsBO;
	private DefaultTableModel tabModAPClsBO;
	private JTable tableAPClsBO;
	private JButton btnUpdateAPClsBO;
	private JButton btnHelpAPClsBO;
	
	private JPanel panelCapping;
	private DefaultTableModel tabModCapping;
	private JTable tableCapping;
	private JButton btnAddCapping;
	private JButton btnDelCapping;
	private JButton btnSortCapping;
	private JButton btnHelpCapping;
	
	private JPanel panelFrbEnd;
	private DefaultTableModel tabModFrbEnd;
	private JTable tableFrbEnd;
	private JButton btnAddFrbEnd;
	private JButton btnDelFrbEnd;
	private JButton btnSortFrbEnd;
	private JButton btnHelpFrbEnd;
	
	
//-----------------------------------------------------------------------------
	
	public CompatibilityMatrixForm()
	{
		this.setLayout(new BorderLayout());
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		super.add(tabbedPane, BorderLayout.CENTER);
		
		//
		// APClass Compatibility rules (i.e., the actual compatibility matrix)
		//
		
		panelCPMap = new JPanel(new BorderLayout());
		tabbedPane.addTab("APClass compatibility",null,panelCPMap,null);

        btnAddCompRul = new JButton("Add Rule");
        btnAddCompRul.setToolTipText("Add compatibility rules for a new "
        		+ "source APClass.");
        btnAddCompRul.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DefaultListModel<String> srcAPCs =
                        new DefaultListModel<String>();
                JList<String> srcClsList = new JList<String>(srcAPCs);
                for (String apc : allAPClasses)
                {
                    if (!compatMap.keySet().contains(apc))
                    {
                    	srcAPCs.addElement(apc);
                    }
                }
                srcClsList.setSelectionMode(
                		ListSelectionModel.SINGLE_SELECTION);

                DefaultListModel<String> trgAPCs =
                        new DefaultListModel<String>();
                JList<String> trgClsList = new JList<String>(trgAPCs);
                for (String apc : allAPClasses)
                {
                	trgAPCs.addElement(apc);
                }
                trgClsList.setSelectionMode(
                		ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

                JPanel twoListsPanel = new JPanel();
                JLabel headSrc = new JLabel("APClass on the growing graph:");
                JLabel headTrg = new JLabel("APClass on incoming fragment:");
                JScrollPane scrollSrc = new JScrollPane(srcClsList);
                JScrollPane scrollTrg = new JScrollPane(trgClsList);
                GroupLayout lyoAddCapRule = new GroupLayout(twoListsPanel);
                twoListsPanel.setLayout(lyoAddCapRule);
                lyoAddCapRule.setAutoCreateGaps(true);
                lyoAddCapRule.setAutoCreateContainerGaps(true);
                lyoAddCapRule.setHorizontalGroup(lyoAddCapRule.createSequentialGroup()
                	.addGroup(lyoAddCapRule.createParallelGroup()
                			.addComponent(headSrc)
                        	.addComponent(scrollSrc))
                    .addGroup(lyoAddCapRule.createParallelGroup()
                    	.addComponent(headTrg)
                    	.addComponent(scrollTrg)));
                lyoAddCapRule.setVerticalGroup(lyoAddCapRule.createSequentialGroup()
                    .addGroup(lyoAddCapRule.createParallelGroup()
                    	.addComponent(headSrc)
                    	.addComponent(headTrg))
                    .addGroup(lyoAddCapRule.createParallelGroup()
                        .addComponent(scrollSrc)
                    	.addComponent(scrollTrg)));

                JOptionPane.showMessageDialog(
                        null,
                        twoListsPanel,
                        "New APClass compatibility rule",
                        JOptionPane.PLAIN_MESSAGE);

                if (trgClsList.getSelectedIndices().length > 0
                		&& srcClsList.getSelectedIndices().length > 0)
                {
	                //NB: we allow a single selection in the src APClass list
	                String srcAPClass = (String) srcAPCs.getElementAt(
	                		srcClsList.getSelectedIndices()[0]);

	                ArrayList<String> trgCPClasses = new ArrayList<String>();
	                for (Integer id : trgClsList.getSelectedIndices())
	                {
	                    trgCPClasses.add((String) trgAPCs.getElementAt(id));
	                }
	                
	                if (compatMap.keySet().contains(srcAPClass))
	                {
	                	compatMap.get(srcAPClass).addAll(trgCPClasses);
	                }
	                else
	                {
	                	compatMap.put(srcAPClass,trgCPClasses);
	                }
	                allAPClsInCPMap.add(srcAPClass);
	                
	                updateAPClassCompatibilitiesList();
                }
            }
        });
        
        btnCopyCompRul = new JButton("Copy Rule");
        btnCopyCompRul.setToolTipText(String.format("<html><body width='%1s'>"
        		+ "<p>Copy all the compatibility rules of a selected source "
        		+ "APClass to a new, user-selected source APClass.</p></html>",
        		300));
        btnCopyCompRul.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	ArrayList<String> selected = new ArrayList<String>();
            	for (Component lineComponent : panelCPRules.getComponents())
            	{
            		if (lineComponent instanceof CompatibilityRuleLine)
            		{
	            		if (((CompatibilityRuleLine) lineComponent).isSelected)
	            		{
	        				selected.add(lineComponent.getName());
	            		}
            		}
            	}
            	
                if (selected.size() == 1)
                {
                	String srcOrig = selected.get(0);

                    DefaultListModel<String> srcAPCs =
                            new DefaultListModel<String>();
                    JList<String> srcClsList = new JList<String>(srcAPCs);
                    for (String apc : allAPClasses)
                    {
                        if (!compatMap.keySet().contains(apc))
                        {
                        	srcAPCs.addElement(apc);
                        }
                    }
                    srcClsList.setSelectionMode(
                    		ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                    
                    JOptionPane.showMessageDialog(
                            null,
                            new JScrollPane(srcClsList),
                            "Select source APClasses of new rules",
                            JOptionPane.PLAIN_MESSAGE);

                    List<String> selSrc = srcClsList.getSelectedValuesList();
                    if (selSrc.size() > 0)
                    {
                    	for (String srcAPClass : selSrc)
                    	{
                    		ArrayList<String> newTrg = new ArrayList<String>();
                    		newTrg.addAll(compatMap.get(srcOrig));
	    	                compatMap.put(srcAPClass,newTrg);
	    	                allAPClsInCPMap.add(srcAPClass);
                    	}
    	                updateAPClassCompatibilitiesList();
                    }
                }
                else
                {
        			JOptionPane.showMessageDialog(null,
        					"<html>Please, select one and only one source "
        					+ "APCLasss.</html>",
        	                "Error",
        	                JOptionPane.WARNING_MESSAGE,
        	                UIManager.getIcon("OptionPane.errorIcon"));
        			return;
                }
            }
        });

        btnDelCompRul = new JButton("Remove Selected");
        btnDelCompRul.setToolTipText(String.format("<html><body width='%1s'>"
        		+ "<p>Remove all the compatibility rules of selected "
        		+ "source APClasses. Click on the "
        		+ "name of a source APClass to select all its compatibility"
        		+ "rules. You can select multiple source APClasses.</p></html>",
        		300));
        btnDelCompRul.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	int i = 0;
            	for (Component lineComponent : panelCPRules.getComponents())
            	{
            		if (lineComponent instanceof CompatibilityRuleLine)
            		{
	            		if (((CompatibilityRuleLine) lineComponent).isSelected)
	            		{
	            			compatMap.remove(lineComponent.getName());
	            			allAPClsInCPMap.remove(lineComponent.getName());
	            			panelCPRules.remove(lineComponent);
	            			i++;
	            		}
            		}
            	}
            	
                if (i > 0)
                {
                    updateAPClassCompatibilitiesList();
                }
                else
                {
                	JOptionPane.showMessageDialog(null,
        					"<html>Please, click to select at least one source "
        					+ "AP CLasss.</html>",
        	                "Error",
        	                JOptionPane.WARNING_MESSAGE,
        	                UIManager.getIcon("OptionPane.errorIcon"));
        			return;
                }
            }
        });

        btnHelpCPMap = new JButton("?");
        btnHelpCPMap.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	//TODO
                String txt = "<html><body width='%1s'><p>TODO</p></html>";
                JOptionPane.showMessageDialog(null,
                        String.format(txt, 400),
                        "Tips",
                        JOptionPane.PLAIN_MESSAGE);
            }
        });

        JPanel panelBtnCPMap = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelBtnCPMap.add(btnAddCompRul);
        panelBtnCPMap.add(btnCopyCompRul);
        panelBtnCPMap.add(btnDelCompRul);
        panelBtnCPMap.add(btnHelpCPMap);
        panelCPMap.add(panelBtnCPMap, BorderLayout.NORTH);
        
        panelCPRules = new JPanel();
        scrollPanelCPMap = new JScrollPane(panelCPRules,
        		JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        		JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        panelCPRules.setLayout(new BoxLayout(panelCPRules, 
        		SwingConstants.VERTICAL));
        panelCPMap.add(scrollPanelCPMap, BorderLayout.CENTER);
		
		//
		// APClass to Bond Order
		//
        panelAPClsBO = new JPanel(new BorderLayout());
		tabbedPane.addTab("APClass-to-Bond",null,panelAPClsBO,null);

        String toolTipAPClsBO = String.format("<html><body width='%1s'>This "
        		+ "table contains the APClass-to-Bond Order map that defines "
        		+ "the bond order of bonds that are generated as a result of "
        		+ "a fragment-fragment connection, i.e., an edge in the "
        		+ "DENOPTIMGraph.</html>",300);

        tabModAPClsBO = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
            	if (column == 0)
            		return false;
            	else
            		return true;
            }
        };
        tabModAPClsBO.setColumnCount(2);
        String column_name_bo[]= {"<html><b>APClass</b></html>",
                "<html><b>Bond Order</b></html>"};
        tabModAPClsBO.setColumnIdentifiers(column_name_bo);
        tabModAPClsBO.addTableModelListener(new PausableAPC2BOTabModListener());
        tableAPClsBO = new JTable(tabModAPClsBO);
        tableAPClsBO.getColumnModel().getColumn(1).setCellRenderer(
        		new AP2BOCellRenderer());
        tableAPClsBO.setToolTipText(toolTipAPClsBO);
        tableAPClsBO.putClientProperty("terminateEditOnFocusLost", true);

        btnUpdateAPClsBO = new JButton("Refresh");
        btnUpdateAPClsBO.setToolTipText("Updates the table with the most recent "
        		+ "list of APClasses");
        btnUpdateAPClsBO.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	//TODO: make sure every method adding APClasses puts an empty 
            	// entry also in the bo map
            	updateAPClassToBondOrderTable();
            }
        });

        btnHelpAPClsBO = new JButton("?");
        btnHelpAPClsBO.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String txt = "<html><body width='%1s'><p>This table contains "
                		+ "the APClass-to-Bond Order mapping. Use the "
                		+ "<code>Refresh</code> button to update a the table "
                		+ "when APClasses have been added in other tabs. The "
                		+ "<code>Refresh<code> allows also to recover the "
                		+ "last available and valid value provided for any "
                		+ "bond order.</p>"
                		+ "<br>"
                		+ "<p>Missing of invalid values (non-integer, "
                		+ "value&lt;1 or value&gt;" + MAXBO 
                		+ ") are highlighted in red, "
                		+ "and must be changed to acceptable values in order "
                		+ "to define a fragment space.</p></html>";
                JOptionPane.showMessageDialog(null,
                        String.format(txt, 400),
                        "Tips",
                        JOptionPane.PLAIN_MESSAGE);
            }
        });

        JPanel panelBtnAPClsBO = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelBtnAPClsBO.add(btnUpdateAPClsBO);
        panelBtnAPClsBO.add(btnHelpAPClsBO);
        panelAPClsBO.add(panelBtnAPClsBO, BorderLayout.NORTH);

        JScrollPane panelAPClsBOTable = new JScrollPane(tableAPClsBO);
        panelAPClsBO.add(panelAPClsBOTable, BorderLayout.CENTER);
        
		
		//
		// Capping rules
		//
		panelCapping = new JPanel(new BorderLayout());
		tabbedPane.addTab("Capping",null,panelCapping,null);

        String toolTipCapping = String.format("<html><body width='%1s'>"
        		+ "Capping rules define the attachment point class (APClass) "
        		+ "of the capping group used to saturate unused attachment "
        		+ "points of a given APClass that need to be saturated when "
        		+ "finalizing the construction of a graph.</html>",300);

        tabModCapping = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tabModCapping.setColumnCount(2);
        String column_name_fe[]= {"<html><b>APClasses on graph</b></html>", 
        		"<html><b>APClasses of Capping Group</b></html>"};
        tabModCapping.setColumnIdentifiers(column_name_fe);
        tableCapping = new JTable(tabModCapping);
        tableCapping.setToolTipText(toolTipCapping);

        btnAddCapping = new JButton("Append Capping Rules");
        btnAddCapping.setToolTipText("Add new lines to the table");
        btnAddCapping.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DefaultListModel<String> srcAPCs =
                        new DefaultListModel<String>();
                JList<String> srcClsList = new JList<String>(srcAPCs);
                for (String apc : allAPClasses)
                {
                    if (!cappingMap.keySet().contains(apc))
                    {
                    	srcAPCs.addElement(apc);
                    }
                }
                
                DefaultListModel<String> capAPCs =
                        new DefaultListModel<String>();
                JList<String> capClsList = new JList<String>(capAPCs);
                for (String apc : allCapAPClasses)
                {
                	capAPCs.addElement(apc);
                }
                capClsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            
                JPanel twoListsPanel = new JPanel();
                JLabel headSrc = new JLabel("APClass of APs to be capped:");
                JLabel headCap = new JLabel("APClass of capping group:");
                JScrollPane scrollSrc = new JScrollPane(srcClsList);
                JScrollPane scrollCap = new JScrollPane(capClsList);
                GroupLayout lyoAddCapRule = new GroupLayout(twoListsPanel);
                twoListsPanel.setLayout(lyoAddCapRule);
                lyoAddCapRule.setAutoCreateGaps(true);
                lyoAddCapRule.setAutoCreateContainerGaps(true);
                lyoAddCapRule.setHorizontalGroup(lyoAddCapRule.createSequentialGroup()
                	.addGroup(lyoAddCapRule.createParallelGroup()
                			.addComponent(headSrc)
                        	.addComponent(scrollSrc))
                    .addGroup(lyoAddCapRule.createParallelGroup()
                    	.addComponent(headCap)
                    	.addComponent(scrollCap)));
                lyoAddCapRule.setVerticalGroup(lyoAddCapRule.createSequentialGroup()
                    .addGroup(lyoAddCapRule.createParallelGroup()
                    	.addComponent(headSrc)
                    	.addComponent(headCap))
                    .addGroup(lyoAddCapRule.createParallelGroup()
                        .addComponent(scrollSrc)
                    	.addComponent(scrollCap)));

                JOptionPane.showMessageDialog(
                        null,
                        twoListsPanel,
                        "Choose combination of source and capping APClasses",
                        JOptionPane.PLAIN_MESSAGE);
                
                if (capClsList.getSelectedIndices().length > 0
                		&& srcClsList.getSelectedIndices().length > 0)
                {
	                //NB: we allow a single selection in the cap APClass list
	                String cappingAPClass = (String) capAPCs.getElementAt(
	                		capClsList.getSelectedIndices()[0]);
	                
	                for (Integer id : srcClsList.getSelectedIndices())
	                {
	                    String srcAPClass = (String) srcAPCs.getElementAt(id);
	                    tabModCapping.addRow(new Object[]{srcAPClass,cappingAPClass});
	                    cappingMap.put(srcAPClass,cappingAPClass);
	                }  
                }
            }
        });

        btnDelCapping = new JButton("Remove Selected");
        btnDelCapping.setToolTipText("Remove all the selected lines from the "
        		+ "list");
        btnDelCapping.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (tableCapping.getRowCount() > 0)
                {
                    if (tableCapping.getSelectedRowCount() > 0)
                    {
                        int selectedRowIds[] = tableCapping.getSelectedRows();
                        Arrays.sort(selectedRowIds);
                        for (int i=(selectedRowIds.length-1); i>-1; i--)
                        {
                            cappingMap.remove(tableCapping.getValueAt(
                                    selectedRowIds[i], 0));
                            tabModCapping.removeRow(selectedRowIds[i]);
                        }
                    }
                }
            }
        });
        
        btnSortCapping = new JButton("Sort List");
        btnSortCapping.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateCappingRulesTable();
			}
		});

        btnHelpCapping = new JButton("?");
        btnHelpCapping.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String txt = "<html><body width='%1s'><p>Capping rules are "
                		+ "used to saturate free attachment points (APs) when "
                		+ "finalizing the construction of a graph. Since APs "
                		+ "are often (but not always) the representation of "
                		+ "open valences, the capping procedure serves to "
                		+ "saturate all open valences according to APs "
                		+ "compatibility. This procedure follows the capping "
                		+ "rules defined in this table.</p>"
                		+ "<br>"
                		+ "<p>Each capping "
                		+ "rule (i.e., each line in this table) identifies "
                		+ "the combination of two attachment point classes "
                		+ "(APClasses): "
                		+ "<ul>"
                		+ "<li>the APCLass of the attachment points to be "
                		+ "capped (first column)</li>"
                		+ "<li>the APClass of the capping group used to "
                		+ "saturate APs above attachment points.</li>"
                		+ "</ul></p><br>"
                        + "<p>You can select multiple entries as intervals or "
                        + "by multiple clicks while holding the appropriate "
                        + "keys (e.g., <code>alt</code>, <code>ctrl</code>, "
                        + "<code>cmd</code> depending on your keyboard "
                        + "settings).</p></html>";
                JOptionPane.showMessageDialog(null,
                        String.format(txt, 400),
                        "Tips",
                        JOptionPane.PLAIN_MESSAGE);
            }
        });

        JPanel panelBtnCapping = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelBtnCapping.add(btnAddCapping);
        panelBtnCapping.add(btnDelCapping);
        panelBtnCapping.add(btnSortCapping);
        panelBtnCapping.add(btnHelpCapping);
        panelCapping.add(panelBtnCapping, BorderLayout.NORTH);

        JScrollPane panelCappingTable = new JScrollPane(tableCapping);
        panelCapping.add(panelCappingTable, BorderLayout.CENTER);
		
		//
		// Forbidden ends panel
		//
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
        String column_name_cap[]= {"<html><b>APClasses defining the forbidden "
        		+ "ends:</b></html>"};
        tabModFrbEnd.setColumnIdentifiers(column_name_cap);
        tableFrbEnd = new JTable(tabModFrbEnd);
        tableFrbEnd.setToolTipText(toolTipFrbEnd);
        
        btnAddFrbEnd = new JButton("Append Forbidden End");
        btnAddFrbEnd.setToolTipText("Add a new line in the list");
        btnAddFrbEnd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
    			DefaultListModel<String> claLstModel = 
    					new DefaultListModel<String>();
    			JList<String> clsList = new JList<String>(claLstModel);
    			for (String apc : allAPClasses)
    			{
    				if (!forbiddenEndList.contains(apc))
    				{
    					claLstModel.addElement(apc);
    				}
    			}
    			
    			JOptionPane.showMessageDialog(
    					null, 
    					new JScrollPane(clsList), 
    					"Choose attachment point classes:",
    					JOptionPane.PLAIN_MESSAGE);

    			for (Integer id : clsList.getSelectedIndices())
    			{
        			String apClass = (String) claLstModel.getElementAt(id);
    				tabModFrbEnd.addRow(new Object[]{apClass});
    				forbiddenEndList.add(apClass);
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
        	            	forbiddenEndList.remove(tableFrbEnd.getValueAt(
        	            			selectedRowIds[i], 0));
        	            	tabModFrbEnd.removeRow(selectedRowIds[i]);
        	            }
        	        }
        	    }
			}
		});
        
        btnSortFrbEnd = new JButton("Sort List");
        btnSortFrbEnd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateForbiddenEndsTable();
			}
		});
        
        btnHelpFrbEnd = new JButton("?");
        btnHelpFrbEnd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String txt = "<html><body width='%1s'><p>Use these buttons to "
						+ "add/remove attachment point classes (APClasses) "
						+ "that define <i>forbidden ends</i>, i.e., "
						+ "attachment point that cannot be left free in a "
						+ "finished graph. Graphs holding free (i.e., "
						+ "unsaturated) attachment point with any of these "
						+ "APClasses are considered incomplete and are not "
						+ "submitted to fitness evaluation.</p><br>"
						+ "<p>You can select multiple entries as intervals or "
						+ "by multiple clicks while holding the appropriate "
						+ "keys (e.g., <code>alt</code>, <code>ctrl</code>, "
						+ "<code>cmd</code> depending on your keyboard "
						+ "settings).</p></html>";
				JOptionPane.showMessageDialog(null, 
						String.format(txt, 400),
	                    "Tips",
	                    JOptionPane.PLAIN_MESSAGE);
			}
		});
        
        JPanel panelBtnFrbEnd = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelBtnFrbEnd.add(btnAddFrbEnd);
        panelBtnFrbEnd.add(btnDelFrbEnd);
        panelBtnFrbEnd.add(btnSortFrbEnd);
        panelBtnFrbEnd.add(btnHelpFrbEnd);
        panelFrbEnd.add(panelBtnFrbEnd, BorderLayout.NORTH);
        
        JScrollPane panelFrbEndTable = new JScrollPane(tableFrbEnd);
        panelFrbEnd.add(panelFrbEndTable, BorderLayout.CENTER);
		
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Listener that is pausable and that, when active, projects edits in the 
	 * table of APClass-to-BO rules into the corresponding map.
	 */
	private class PausableAPC2BOTabModListener implements TableModelListener
	{	
		private boolean isActive = false;
		
		public PausableAPC2BOTabModListener() 
		{};

		@Override
		public void tableChanged(TableModelEvent e) 
		{
            if (isActive && e.getType() == TableModelEvent.UPDATE)
            {
            	int row = e.getFirstRow();
            	int column = e.getColumn();
            	if (column != 1)
            	{
            		return;
            	}
            	String apc = tableAPClsBO.getValueAt(row, 0).toString();
            	try {
					int value = Integer.parseInt(
							tableAPClsBO.getValueAt(row, 1).toString());
					if (value<1 || value>MAXBO)
					{
						return;
					}
					bondOrderMap.put(apc, value);
				} catch (NumberFormatException e1) {
					// value is so invalid we ignore it
				}
            }
		}
        
		public void setActive(boolean var)
		{
			isActive = var;
		}
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Allows to activate and deactivate the listener.
	 * @param var use <code>true</code> to activate the listener
	 */
    public void activateTabEditsListener(boolean var)
    {
		int s = tabModAPClsBO.getTableModelListeners().length;
		for (int i=0; i<s; i++)
		{
			TableModelListener l = tabModAPClsBO.getTableModelListeners()[i];
			if (l instanceof PausableAPC2BOTabModListener)
			{
				((PausableAPC2BOTabModListener) l).setActive(var);
			}
		}
    }
	
//-----------------------------------------------------------------------------
	
	/**
	 * Rendered that displaying a red background for cells with nonsense bond 
	 * order.
	 */
    private class AP2BOCellRenderer extends DefaultTableCellRenderer 
    {
        public Component getTableCellRendererComponent(JTable table, 
        		Object value, boolean isSelected, boolean hasFocus, 
        		int row, int column)
        {
            Component cellComponent = super.getTableCellRendererComponent(
            		table, value, isSelected, hasFocus, row, column);

            if(column == 1)
            {
            	if (value instanceof Integer)
            	{
            		if (((Integer) value) < 1 || ((Integer) value) > MAXBO)
					{
						cellComponent.setBackground(Color.RED);
					}
            		else
            		{
            			if (isSelected)
                    		cellComponent.setBackground(Color.BLUE);
                    	else
                    		cellComponent.setBackground(Color.WHITE);
            		}
            	}
            	else if (value instanceof String)
            	{
            		
            		try {
						int val = Integer.parseInt((String) value);
						if (val < 1)
						{
							cellComponent.setBackground(Color.RED);
						}
						else
						{
							if (isSelected)
			            		cellComponent.setBackground(Color.BLUE);
			            	else
			            		cellComponent.setBackground(Color.WHITE);
						}
					} catch (NumberFormatException e) {
						cellComponent.setBackground(Color.RED);
					}
            	}
            }
            else
            {
            	if (isSelected)
            		cellComponent.setBackground(Color.BLUE);
            	else
            		cellComponent.setBackground(Color.WHITE);
            }
            return cellComponent;
        }
    }
    
//-----------------------------------------------------------------------------

	protected void importCPMapFromFile(File inFile)
	{	
		//Read data from file
        compatMap = new HashMap<String,ArrayList<String>>();
        bondOrderMap = new HashMap<String,Integer>();
        cappingMap = new HashMap<String,String>();
        forbiddenEndList = new HashSet<String>();
        try {
			DenoptimIO.readCompatibilityMatrix(inFile.getAbsolutePath(),
						compatMap,
						bondOrderMap,
						cappingMap,
						forbiddenEndList);
			allAPClsInCPMap.addAll(compatMap.keySet());
		} catch (DENOPTIMException e) {
			JOptionPane.showMessageDialog(null,
					"<html>Could not read compatibility matrix data from "
					+ "file<br>'" + inFile + "'</html>",
	                "Error",
	                JOptionPane.WARNING_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;
		}
        
        // Update list of all APClasses 
        // WARNING: the compatibility matrix files may not contain all the 
        // APClasses of a fragment space! But without reading the actual list
        // of fragments there is nothing else we can do.
        importAllAPClassesFromCPMatrix(true);
        importAllCappingGroupsAPClassesFromCPMatrix(true);
        
        //Place data into GUI
        updateAPClassCompatibilitiesList();
        updateAPClassToBondOrderTable();
        updateCappingRulesTable();
        updateForbiddenEndsTable();

	}
	
//-----------------------------------------------------------------------------
	
	private void updateAPClassCompatibilitiesList()
	{
		// Cleanup previous content
		panelCPRules.removeAll();
		
		// Fill with new content
		CompatRulesHeader h = new CompatRulesHeader();
		h.setAlignmentX(LEFT_ALIGNMENT);
		panelCPRules.add(h);
		for (String srcAPClass : allAPClsInCPMap)
		{
			CompatibilityRuleLine r = new CompatibilityRuleLine(srcAPClass);
			r.setAlignmentX(LEFT_ALIGNMENT);
			panelCPRules.add(r);
		}
		panelCPRules.repaint();
		panelCPRules.revalidate();	
	}
	
//-----------------------------------------------------------------------------
	
	private void updateAPClassToBondOrderTable()
	{
		activateTabEditsListener(false); 
		
		// Remove all lines
		int szTab = tabModAPClsBO.getRowCount();
        for (int i=0; i<szTab; i++) 
        {
        	//Always remove the first to avoid dealing with changing row ids
        	tabModAPClsBO.removeRow(0);
        }
        
        // Get sorted list of table rows
		ArrayList<String> sortedAPClsToBO = new ArrayList<String>();
		sortedAPClsToBO.addAll(bondOrderMap.keySet());
	    Collections.sort(sortedAPClsToBO);
	    
	    // Re-build table
	    for (String apc : sortedAPClsToBO)
	    {
	    	tabModAPClsBO.addRow(new Object[]{apc, bondOrderMap.get(apc)});
	    }
	    
	    activateTabEditsListener(true);
	}
	
//-----------------------------------------------------------------------------
	
	private void updateCappingRulesTable()
	{
		// Remove all lines
		int szTab = tabModCapping.getRowCount();
        for (int i=0; i<szTab; i++) 
        {
        	//Always remove the first to avoid dealing with changing row ids
        	tabModCapping.removeRow(0);
        }
        
        // Get sorted list of table rows
		ArrayList<String> sortedCappings = new ArrayList<String>();
	    sortedCappings.addAll(cappingMap.keySet());
	    Collections.sort(sortedCappings);
	    
	    // Re-build table
	    for (String apc : sortedCappings)
	    {
	        tabModCapping.addRow(new Object[]{apc, cappingMap.get(apc)});
	    }
	}
	
//-----------------------------------------------------------------------------
	
	private void updateForbiddenEndsTable()
	{
		// Remove all lines
		int szTab = tabModFrbEnd.getRowCount();
        for (int i=0; i<szTab; i++) 
        {
        	//Always remove the first to avoid dealing with changing row ids
        	tabModFrbEnd.removeRow(0);
        }
		
        // Get sorted list of table rows
        ArrayList<String> sortedFrbEnds = new ArrayList<String>();
        sortedFrbEnds.addAll(forbiddenEndList);
        Collections.sort(sortedFrbEnds);
        
        // Re-build table
        for (String apc : sortedFrbEnds)
        {
        	tabModFrbEnd.addRow(new Object[]{apc});
        }
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Reads all the APClasses found in the currently loaded compatibility 
	 * matrix.
	 * @param cleanup set <code>true</code> to cleanup previous listing of 
	 * APclasses.
	 */
	
	private void importAllAPClassesFromCPMatrix(boolean cleanup)
	{
		if (cleanup)
		{
			allAPClasses = new TreeSet<String>();
		}
		
		allAPClasses.addAll(compatMap.keySet());
		
		for (ArrayList<String> apcs : compatMap.values())
		{
			allAPClasses.addAll(apcs);
		}
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Reads all the APClasses of capping groups as found in the currently 
	 * loaded capping rules.
	 * @param cleanup set <code>true</code> to cleanup previous listing of 
	 * APclasses.
	 */
	
	private void importAllCappingGroupsAPClassesFromCPMatrix(boolean cleanup)
	{
		if (cleanup)
		{
			allCapAPClasses = new TreeSet<String>();
		}
		allCapAPClasses.addAll(cappingMap.values());
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Reads all the APClasses found in a list of files
	 * @param fragLibs the list of file to inspect.
	 * @param cleanup set <code>true</code> to cleanup previous listing of 
	 * APclasses.
	 */
	
	private void importAllAPClassesFromFragmentLibs(Set<File> fragLibs,
			boolean cleanup)
	{
		this.setCursor(Cursor.getPredefinedCursor(
				Cursor.WAIT_CURSOR));
		if (cleanup)
		{
			allAPClasses = new TreeSet<String>();
		}
		
		for (File fragLib : fragLibs)
		{
			allAPClasses.addAll(DenoptimIO.readAllAPClasses(fragLib));
		}
		
		this.setCursor(Cursor.getPredefinedCursor(
				Cursor.DEFAULT_CURSOR));
	}
	
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
	
	/**
	 * Single line in the list of APClass compatibility rules
	 */
	
	private class CompatibilityRuleLine extends JPanel implements MouseListener
	{
		/**
		 * Version UID
		 */
		private static final long serialVersionUID = -5007017502551954331L;

		private JLabel srcClassName;
		private JPanel trgClassesPanel;
		private JScrollPane trgClassesScroller;
		private JButton btnAdd;
		
		private boolean isSelected = false;
		
		private final Dimension minSrcAPClassName = new Dimension(200,26);
		private final Dimension scrollerSize = new Dimension(300,50);
		private final Color SELECTEDBACKGROUND = Color.BLUE;
		private final Color DEFAULTBACKGROUND = Color.WHITE;
		//UIManager.getLookAndFeelDefaults().getColor("Panel.background")
		
	//------------------------------------------------------------------------

		public CompatibilityRuleLine(String srcAPClass)
		{
			this.setName(srcAPClass);
			this.setBackground(DEFAULTBACKGROUND);
			this.setLayout(new BorderLayout());
			
			srcClassName = new JLabel(srcAPClass);
			srcClassName.setBackground(DEFAULTBACKGROUND);
			srcClassName.setPreferredSize(minSrcAPClassName);
			srcClassName.setToolTipText(srcAPClass);
			srcClassName.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
			srcClassName.addMouseListener(this);
			this.add(srcClassName, BorderLayout.WEST);
			
			trgClassesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			for (String trgAPClass : compatMap.get(srcAPClass))
			{
				trgClassesPanel.add(new JButton(trgAPClass));
			}
			trgClassesScroller = new JScrollPane(trgClassesPanel,
	        		JScrollPane.VERTICAL_SCROLLBAR_NEVER,
	        		JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			trgClassesScroller.setPreferredSize(scrollerSize);
	        this.add(trgClassesScroller, BorderLayout.CENTER);
	        
			btnAdd = new JButton("Add");
			btnAdd.setToolTipText(String.format("<html><body width='%1s'>"
					+ "Add more compatible APClasses to source class <i>"
					+ srcAPClass + "</i></html>",250));
			this.add(btnAdd, BorderLayout.EAST);
			
			this.setBorder(BorderFactory.createEmptyBorder(5,0,5,5));
			
			addMouseListener(this);
			setFocusable(true);
		}
		
	//-------------------------------------------------------------------------
		
		public boolean isSelected()
		{
			return isSelected;
		}
		
	//-------------------------------------------------------------------------
		
		public void renderdAsSelected(boolean selected)
		{
			if (selected)
			{
				super.setBackground(SELECTEDBACKGROUND);
				srcClassName.setForeground(Color.WHITE);
			}
			else
			{
				super.setBackground(DEFAULTBACKGROUND);
				srcClassName.setForeground(Color.BLACK);
			}
		}
		
	//-------------------------------------------------------------------------

		@Override
		public void mouseClicked(MouseEvent e) {
			if (isSelected)
				isSelected = false;
			else
				isSelected = true;
			renderdAsSelected(isSelected);
		}
	
	//-------------------------------------------------------------------------
	
		@Override
		public void mousePressed(MouseEvent e) {
			//Nothing
		}
		
	//-------------------------------------------------------------------------
	
		@Override
		public void mouseReleased(MouseEvent e) {
			//Nothing
		}
		
	//-------------------------------------------------------------------------
	
		@Override
		public void mouseEntered(MouseEvent e) {
			//Nothing
		}
		
	//-------------------------------------------------------------------------
	
		@Override
		public void mouseExited(MouseEvent e) {
			//Nothing
		}
	}
	
//-----------------------------------------------------------------------------
	
	private class CompatRulesHeader extends JPanel
	{
		private Dimension minSrcAPClassName = new Dimension(200,26);
		public final Color DEFAULTBACKGROUND = Color.WHITE;
		
		public CompatRulesHeader()
		{
			this.setName("Header");
			this.setBackground(DEFAULTBACKGROUND);
			this.setLayout(new BorderLayout());
			
			JLabel srcClassTitle = new JLabel("<html>"
					+ "<div style='text-align: center;'>"
					+ "<b>Source APClass:</b></div></html>");
			srcClassTitle.setBackground(DEFAULTBACKGROUND);
			srcClassTitle.setPreferredSize(minSrcAPClassName);
			srcClassTitle.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
			this.add(srcClassTitle, BorderLayout.WEST);
			
			JLabel trgClassTitle = new JLabel("<html>"
					+ "<div style='text-align: center;'>"
					+ "<b>Compatible target APClasses on incoming fragments:</b></div></html>");
			trgClassTitle.setBackground(DEFAULTBACKGROUND);
			trgClassTitle.setPreferredSize(minSrcAPClassName);
			trgClassTitle.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));

	        this.add(trgClassTitle, BorderLayout.CENTER);

			this.setBorder(BorderFactory.createEmptyBorder(5,0,5,5));
		}
	}
    
//-----------------------------------------------------------------------------
    
    
    
//-----------------------------------------------------------------------------

	
//-----------------------------------------------------------------------------
	
}
