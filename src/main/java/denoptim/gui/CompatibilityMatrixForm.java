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
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListenerProxy;
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
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import denoptim.exception.DENOPTIMException;
import denoptim.graph.APClass;
import denoptim.graph.DENOPTIMEdge.BondType;
import denoptim.io.DenoptimIO;

public class CompatibilityMatrixForm extends JPanel {

	/**
	 * Version UID
	 */
	private static final long serialVersionUID = -8042143358823563589L;

	/**
	 * Property used to trigger removal of a target APClass.
	 */
	protected static final String REMOVETRGAPC = "REMOVETRGAPC";
	
	/**
	 * List of all APClasses.
	 * These can be found in a fragment space or collected from
	 * fragment libraries
	 */
	private SortedSet<APClass> allAPClasses = new TreeSet<APClass>();
	
	/**
     * List of all APRules. This list can be edited by creating/removing an 
     * APRule or by creating/removing an APClass.
     */
    private SortedSet<String> allAPRules = new TreeSet<String>();
	
	/**
	 * List of APClasses of capping groups.
	 * These can be found in a fragment space or collected from
	 * the capping group library
	 */
	private SortedSet<APClass> allCapAPClasses = new TreeSet<APClass>();
	
	/**
	 * Sorted list of APClasses in the map of compatibility rules
	 */
	private SortedSet<APClass> allAPClsInCPMap = new TreeSet<APClass>();
	
    /**
     * Data structure that stored the true entries of the 
     * attachment point classes compatibility matrix
     */
    private HashMap<APClass, ArrayList<APClass>> compatMap = 
    			new HashMap<APClass, ArrayList<APClass>>(); 

    /**
     * Data structure that stores compatible APclasses for joining APs 
     * in ring-closing bonds. Symmetric, purpose specific
     * compatibility matrix.
     */
    private HashMap<APClass, ArrayList<APClass>> rcCompatMap = 
    			new HashMap<APClass, ArrayList<APClass>>();

    /**
     * Data structure that stores the AP-classes to be used to cap unused
     * APS on the growing molecule.
     */
    private HashMap<APClass, APClass> cappingMap = 
            new HashMap<APClass, APClass>();

    /**
     * Data structure that stores AP classes that cannot be held unused
     */
    private HashSet<APClass> forbiddenEndList = new HashSet<APClass>();
	
	private JTabbedPane tabbedPane;
	
	private JPanel panelCPMap;
	private JButton btnAddCompRul;
	private JButton btnDelCompRul;
	private JButton btnCopyCompRul;
	private JButton btnClearMatch;
	private JButton btnHelpCPMap;
	private JPanel panelCPRules;
	private JScrollPane scrollPanelCPMap;
	private JTextField txtSearch;
	private JLabel matchCounter;
	
	private JPanel panelAPClsBO;
	private DefaultTableModel tabModAPClsBO;
	private JTable tableAPClsBO;
	private JButton btnAddAPClsBO;
	private JButton btnAddAllNewAPClsBO;
	private JButton btnDelAPClsBO;
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

        btnAddCompRul = new JButton("Add Compatibility Rule");
        btnAddCompRul.setToolTipText("Add compatibility rules for a new "
        		+ "source APClass.");
        btnAddCompRul.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DefaultListModel<String> sAPCsStr =
                        new DefaultListModel<String>();
                JList<String> srcClsList = new JList<String>(sAPCsStr);
                for (APClass apc : allAPClasses)
                {
                    if (!compatMap.keySet().contains(apc))
                    {
                    	sAPCsStr.addElement(apc.toString());
                    }
                }
                sAPCsStr.addElement("<html><b><i>Define a new APClass...<i>"
                		+ "</b></html>");
                srcClsList.setSelectionMode(
                		ListSelectionModel.SINGLE_SELECTION);
                if (srcClsList.getModel().getSize() == 1)
                {
                	srcClsList.setSelectedIndex(0);
                }

                DefaultListModel<String> trgAPCs =
                        new DefaultListModel<String>();
                JList<String> trgClsList = new JList<String>(trgAPCs);
                for (APClass apc : allAPClasses)
                {
                	trgAPCs.addElement(apc.toString());
                }
                trgAPCs.addElement("<html><b><i>Define a new APClass...<i>"
                		+ "</b></html>");
                trgClsList.setSelectionMode(
                		ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                if (trgClsList.getModel().getSize() == 1)
                {
                	trgClsList.setSelectedIndex(0);
                }

                JPanel twoListsPanel = new JPanel();
                JLabel headSrc = new JLabel("APClass on the growing graph");
                JLabel headTrg = new JLabel("APClass on incoming fragment");
                JScrollPane scrollSrc = new JScrollPane(srcClsList);
                JScrollPane scrollTrg = new JScrollPane(trgClsList);
                GroupLayout lyoAddCapRule = new GroupLayout(twoListsPanel);
                twoListsPanel.setLayout(lyoAddCapRule);
                lyoAddCapRule.setAutoCreateGaps(true);
                lyoAddCapRule.setAutoCreateContainerGaps(true);
                lyoAddCapRule.setHorizontalGroup(
                        lyoAddCapRule.createSequentialGroup()
                	.addGroup(lyoAddCapRule.createParallelGroup()
                			.addComponent(headSrc)
                        	.addComponent(scrollSrc))
                	.addGap(30)
                    .addGroup(lyoAddCapRule.createParallelGroup()
                    	.addComponent(headTrg)
                    	.addComponent(scrollTrg)));
                lyoAddCapRule.setVerticalGroup(
                        lyoAddCapRule.createSequentialGroup()
                    .addGroup(lyoAddCapRule.createParallelGroup()
                    	.addComponent(headSrc)
                    	.addGap(10)
                    	.addComponent(headTrg))
                    .addGroup(lyoAddCapRule.createParallelGroup()
                        .addComponent(scrollSrc)
                    	.addComponent(scrollTrg)));

                int res = JOptionPane.showConfirmDialog(btnAddCompRul,
                		twoListsPanel, 
    					"New APClass compatibility rule", 
    					JOptionPane.OK_CANCEL_OPTION,
    					JOptionPane.PLAIN_MESSAGE, 
    					null);
    			
    			if (res != JOptionPane.OK_OPTION)
    			{
    				return;
    			}

                if (trgClsList.getSelectedIndices().length > 0
                		&& srcClsList.getSelectedIndices().length > 0)
                {
	                //NB: we allow a single selection in the src APClass list
                	Integer idSrc = srcClsList.getSelectedIndices()[0];
                	
                	APClass srcAPClass = null;
                	if (idSrc.intValue() == (sAPCsStr.size()-1))
                	{
                		try {
                		    GUIAPClassDefinitionDialog apcDefiner = 
                		            new GUIAPClassDefinitionDialog(
                		                    btnAddCompRul, true);
                	        Object chosen = apcDefiner.showDialog();
                	        if (chosen == null)
                	        {
                	            return;
                	        }
                	        
                	        Object[] pair = (Object[]) chosen;
                	        srcAPClass = APClass.make(pair[0].toString(),
                	                (BondType) pair[1]);
                	        
                			if (allAPClasses.contains(srcAPClass))
                			{
                				JOptionPane.showMessageDialog(btnAddCompRul,
    		        					"<html>Class '<code>" + srcAPClass
    		        					+"</code>' is not new!</html>",
    		        	                "Error",
    		        	                JOptionPane.WARNING_MESSAGE,
    		        	                UIManager.getIcon(
    		        	                        "OptionPane.errorIcon"));
    		        			return;
                			}
							allAPClasses.add(srcAPClass);
							allAPRules.add(srcAPClass.getRule());
						} catch (DENOPTIMException e1) {
		        			JOptionPane.showMessageDialog(btnAddCompRul,
		        					"<html>Error definging anew APClass.<br>"
		        					+ "Please, report this to the DENOPTIM "
		        					+ "team.</html>",
		        	                "Error",
		        	                JOptionPane.WARNING_MESSAGE,
		        	                UIManager.getIcon("OptionPane.errorIcon"));
		        			return;
						} 
                	}
                	else
                	{
                		try
                        {
                            srcAPClass = APClass.make(
                                    sAPCsStr.getElementAt(idSrc));
                        } catch (DENOPTIMException e1)
                        {
                            // Unreachable thanks to ensureGoodAPClassString()
                            e1.printStackTrace();
                        }
                	}

	                ArrayList<APClass> trgCPClasses = new ArrayList<APClass>();
	                for (Integer id : trgClsList.getSelectedIndices())
	                {
	                	if (id.intValue() == (trgAPCs.size()-1))
	                	{
	                		try {
	                		    GUIAPClassDefinitionDialog apcDefiner = 
	                                    new GUIAPClassDefinitionDialog(
	                                            btnAddCompRul, true);
	                            Object chosen = apcDefiner.showDialog();
	                            if (chosen == null)
	                            {
	                                return;
	                            }
	                            
	                            Object[] pair = (Object[]) chosen;
	                            APClass cls = APClass.make(pair[0].toString(),
	                                    (BondType) pair[1]);
	                            
								trgCPClasses.add(cls);
								allAPClasses.add(cls);
								allAPRules.add(cls.getRule());
							} catch (DENOPTIMException e1) {
								continue;
							} 
	                	}
	                	else
	                	{
	                		try
                            {
                                trgCPClasses.add(APClass.make(
                                		(String) trgAPCs.getElementAt(id)));
                            } catch (DENOPTIMException e1)
                            {
                                // Unreachable thanks to ensureGoodAPClassString
                                e1.printStackTrace();
                            }
	                	}
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
                    for (APClass apc : allAPClasses)
                    {
                        if (!compatMap.keySet().contains(apc))
                        {
                        	srcAPCs.addElement(apc.toString());
                        }
                    }
                    srcClsList.setSelectionMode(
                    		ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                    
                    int res = JOptionPane.showConfirmDialog(btnCopyCompRul,
                    		new JScrollPane(srcClsList), 
        					"New APClass sources", 
        					JOptionPane.OK_CANCEL_OPTION,
        					JOptionPane.PLAIN_MESSAGE, 
        					null);
        			
        			if (res != JOptionPane.OK_OPTION)
        			{
        				return;
        			}

                    List<String> selSrc = srcClsList.getSelectedValuesList();
                    if (selSrc.size() > 0)
                    {
                    	for (String srcAPClassStr : selSrc)
                    	{
                    	    APClass srcAPClass = null;
                            try
                            {
                                srcAPClass = APClass.make(srcAPClassStr);
                            } catch (DENOPTIMException e1)
                            {
                                //should never happen
                                e1.printStackTrace();
                            }
                    		ArrayList<APClass> newTrg = 
                    		        new ArrayList<APClass>();
                    		newTrg.addAll(compatMap.get(srcOrig));
	    	                compatMap.put(srcAPClass,newTrg);
	    	                allAPClsInCPMap.add(srcAPClass);
                    	}
    	                updateAPClassCompatibilitiesList();
                    }
                }
                else
                {
        			JOptionPane.showMessageDialog(btnCopyCompRul,
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
        		+ "rules. You can select multiple source APClasses."
        		+ "</p></html>",
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
	            		    try
	            		    {
    	            			compatMap.remove(APClass.make(
    	            			        lineComponent.getName()));
    	            			allAPClsInCPMap.remove(APClass.make(
    	            			        lineComponent.getName()));
    	            			panelCPRules.remove(lineComponent);
	            			i++;
	            		    } catch (DENOPTIMException e1)
	            		    {
	            		        //This should never happen
	            		        e1.printStackTrace();
	            		    }
	            		}
            		}
            	}
            	
                if (i > 0)
                {
                    updateAPClassCompatibilitiesList();
                }
                else
                {
                	JOptionPane.showMessageDialog(btnDelCompRul,
        					"<html>Please, click to select at least one source "
        					+ "AP CLasss.</html>",
        	                "Error",
        	                JOptionPane.WARNING_MESSAGE,
        	                UIManager.getIcon("OptionPane.errorIcon"));
        			return;
                }
            }
        });
        
        JButton btnSearch = new JButton("Search");
        btnSearch.setToolTipText(String.format("<html><body width='%1s'>Search "
        		+ "for the given APClass. Matching compatibility rules are "
        		+ "selected and highlighted accordingly.</html>",150));
        btnSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				searchAPClass(txtSearch.getText());
			}
		});

        txtSearch = new JTextField();
        txtSearch.setToolTipText(String.format("<html><body width='%1s'>Type "
        		+ "here the APClass name or part of it. The search supports "
        		+ "regual expressions and initial and final '(.*)' are added "
        		+ "to all queries.",250));
        txtSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				searchAPClass(txtSearch.getText());
			}
		});
        JScrollPane txtSearchPanel = new JScrollPane(txtSearch,
        		JScrollPane.VERTICAL_SCROLLBAR_NEVER,
        		JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        txtSearchPanel.getHorizontalScrollBar().setPreferredSize(new Dimension(0,2));
        txtSearchPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        
        matchCounter = new JLabel(" ");

        btnClearMatch = new JButton("Clear");
        btnClearMatch.setEnabled(false);
        btnClearMatch.setToolTipText("Clear selection of search hits");
        btnClearMatch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearSearchMatches();
				txtSearch.setText("");
				matchCounter.setText(" ");
				btnClearMatch.setEnabled(false);
			}
		});
        

        btnHelpCPMap = new JButton("?");
        btnHelpCPMap.setToolTipText("Displays the help message.");
        btnHelpCPMap.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String txt = "<html><body width='%1s'><p>Attachment points "
                		+ "(APs) can be annotated with information encoded in "
                		+ "a string of text, i.e., the attachment point class "
                		+ "(APClass). The APClass can be used to define "
                		+ "APClass compatibility rules. Namely, whether two "
                		+ "attachment point can be used to form a connection "
                		+ "between fragment or not. Each rule includes:"
                		+ "<ul> "
                		+ "<li>the <i>Source APClass</i>, which is the class "
                		+ "of the AP on the growing molecule,</li>"
                		+ "<li>a list of compatible APClasses, i.e., an AP "
                		+ "belonging to any incoming fragment and annotated "
                		+ "with any of the compatible APClasses can be chosen "
                		+ "to form a bond with any AP annotated with the "
                		+ "<i>Source APClass</i>.</li></ul></p></html>";
                JOptionPane.showMessageDialog(btnHelpCPMap,
                        String.format(txt, 400),
                        "Tips",
                        JOptionPane.PLAIN_MESSAGE);
            }
        });

        JPanel panelBtnCPMap = new JPanel();
        panelBtnCPMap.setLayout(new BoxLayout(panelBtnCPMap, BoxLayout.X_AXIS));
        panelBtnCPMap.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        panelBtnCPMap.add(btnAddCompRul);
        panelBtnCPMap.add(btnCopyCompRul);
        panelBtnCPMap.add(btnDelCompRul);
        panelBtnCPMap.add(btnSearch);
        panelBtnCPMap.add(txtSearchPanel);
        panelBtnCPMap.add(matchCounter);
        panelBtnCPMap.add(btnClearMatch);
        panelBtnCPMap.add(btnHelpCPMap);
        panelCPMap.add(panelBtnCPMap, BorderLayout.NORTH);
        
        panelCPRules = new JPanel();
        scrollPanelCPMap = new JScrollPane(panelCPRules,
        		JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        		JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPanelCPMap.getVerticalScrollBar().setPreferredSize(
				new Dimension(15,0));
        panelCPRules.setLayout(new BoxLayout(panelCPRules, 
        		SwingConstants.VERTICAL));
        panelCPMap.add(scrollPanelCPMap, BorderLayout.CENTER);
		
		
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

        btnAddCapping = new JButton("Add Capping Rules");
        btnAddCapping.setToolTipText("Add new lines to the table");
        btnAddCapping.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DefaultListModel<String> srcAPCs =
                        new DefaultListModel<String>();
                JList<String> srcClsList = new JList<String>(srcAPCs);
                for (APClass apc : allAPClasses)
                {
                    if (!cappingMap.keySet().contains(apc))
                    {
                    	srcAPCs.addElement(apc.toString());
                    }
                }
                srcAPCs.addElement("<html><b><i>Define a new APClass...<i>"
                		+ "</b></html>");
                if (srcClsList.getModel().getSize() == 1)
                {
                	srcClsList.setSelectedIndex(0);
                }
                
                DefaultListModel<String> capAPCs =
                        new DefaultListModel<String>();
                JList<String> capClsList = new JList<String>(capAPCs);
                for (APClass apc : allCapAPClasses)
                {
                	capAPCs.addElement(apc.toString());
                }
                capAPCs.addElement("<html><b><i>Define a new APClass...<i>"
                		+ "</b></html>");
                capClsList.setSelectionMode(
                		ListSelectionModel.SINGLE_SELECTION);
                if (capClsList.getModel().getSize() == 1)
                {
                	capClsList.setSelectedIndex(0);
                }
            
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
                
                int res = JOptionPane.showConfirmDialog(btnAddCapping,
                		twoListsPanel, 
    					"Choose APClasses", 
    					JOptionPane.OK_CANCEL_OPTION,
    					JOptionPane.PLAIN_MESSAGE, 
    					null);
    			
    			if (res != JOptionPane.OK_OPTION)
    			{
    				return;
    			}
                
                if (capClsList.getSelectedIndices().length > 0
                		&& srcClsList.getSelectedIndices().length > 0)
                {
	                //NB: we allow a single selection in the cap APClass list
                	Integer idc = capClsList.getSelectedIndices()[0];
                	APClass cappingAPClass = null;
                	if (idc.intValue() == (capAPCs.size()-1))
                	{
                		try {
                            GUIAPClassDefinitionDialog apcDefiner = 
                                    new GUIAPClassDefinitionDialog(
                                            btnAddCapping, true);
                            Object chosen = apcDefiner.showDialog();
                            if (chosen == null)
                            {
                                return;
                            }
                            
                            Object[] pair = (Object[]) chosen;
                            cappingAPClass = APClass.make(pair[0].toString(),
                                    (BondType) pair[1]);
                            
                			if (allAPClasses.contains(cappingAPClass))
                			{
                				JOptionPane.showMessageDialog(btnAddCapping,
    		        					"<html>Class '<code>" + cappingAPClass
    		        					+"</code>' is not new!</html>",
    		        	                "Error",
    		        	                JOptionPane.WARNING_MESSAGE,
    		        	                UIManager.getIcon("OptionPane.errorIcon"));
    		        			return;
                			}
							allAPClasses.add(cappingAPClass);
							allAPRules.add(cappingAPClass.getRule());
						} catch (DENOPTIMException e1) {
		        			JOptionPane.showMessageDialog(btnAddCapping,
		        					"<html>Error definging a new APClass.<br>"
		        					+ "Please, report this to the DENOPTIM "
		        					+ "team.</html>",
		        	                "Error",
		        	                JOptionPane.WARNING_MESSAGE,
		        	                UIManager.getIcon("OptionPane.errorIcon"));
		        			return;
						} 
                	}
                	else
                	{
                		try
                        {
                            cappingAPClass = APClass.make(
                                    (String) capAPCs.getElementAt(idc));
                        } catch (DENOPTIMException e1)
                        {
                            //this should never happen
                            e1.printStackTrace();
                        }
                	}
	                
	                for (Integer id : srcClsList.getSelectedIndices())
	                {
	                	if (id.intValue() == (srcAPCs.size()-1))
	                	{
	                		try {
	                		    GUIAPClassDefinitionDialog apcDefiner = 
	                                    new GUIAPClassDefinitionDialog(
	                                            btnAddCapping, true);
	                            Object chosen = apcDefiner.showDialog();
	                            if (chosen == null)
	                            {
	                                return;
	                            }
	                            
	                            Object[] pair = (Object[]) chosen;
	                            APClass newAPC = APClass.make(pair[0].toString(),
	                                    (BondType) pair[1]);
	                		    
	                			if (allAPClasses.contains(newAPC))
	                			{
	                				JOptionPane.showMessageDialog(btnAddCapping,
	    		        					"<html>Class '<code>" + newAPC
	    		        					+"</code>' is not new!</html>",
	    		        	                "Error",
	    		        	                JOptionPane.WARNING_MESSAGE,
	    		        	                UIManager.getIcon("OptionPane.errorIcon"));
	    		        			return;
	                			}
								tabModCapping.addRow(new Object[]{newAPC,
			                    		cappingAPClass});
			                    cappingMap.put(newAPC,cappingAPClass);
								allAPClasses.add(newAPC);
								allAPRules.add(newAPC.getRule());
							} catch (DENOPTIMException e1) {
								continue;
							} 
	                	}
	                	else
	                	{
	                	    APClass srcAPClass = null;
                            try
                            {
                                srcAPClass = APClass.make(
                                        (String) srcAPCs.getElementAt(id));
                            } catch (DENOPTIMException e1)
                            {
                                //this should never happen
                                e1.printStackTrace();
                            }
		                    tabModCapping.addRow(new Object[]{srcAPClass,
		                    		cappingAPClass});
		                    cappingMap.put(srcAPClass,cappingAPClass);
	                	}
	                }  
                }
            }
        });

        btnDelCapping = new JButton("Remove Selected");
        btnDelCapping.setToolTipText(String.format("<html><body width='%1s'>"
        		+ "Remove all the selected "
        		+ "lines in the list. Click on one or more lines to select "
        		+ "them. Multiple lines can be selected by holding the "
        		+ "appropriate key (e.g., <code>shift</code>, "
        		+ "<code>alt</code>, <code>ctrl</code>, <code>cmd</code> "
        		+ "depending on your keyboard settings).</html>",250));
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
                            Object o = tableCapping.getValueAt(
                                    selectedRowIds[i], 0);
                            APClass apc = null;
                            try
                            {
                                apc = APClass.make(tableCapping.getValueAt(
                                		selectedRowIds[i], 0).toString());
                            } catch (DENOPTIMException e1)
                            {
                                //Nothing to do: this should never happen here
                                e1.printStackTrace();
                            }
                            cappingMap.remove(apc);
                            tabModCapping.removeRow(selectedRowIds[i]);
                        }
                    }
                }
            }
        });
        
        btnSortCapping = new JButton("Sort List");
        btnSortCapping.setToolTipText("Sorts according to alphabetic order.");
        btnSortCapping.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateCappingRulesTable();
			}
		});

        btnHelpCapping = new JButton("?");
        btnHelpCapping.setToolTipText("Displays the help message.");
        btnHelpCapping.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String txt = "<html><body width='%1s'><p>Capping rules are "
                		+ "used to saturate free attachment points (APs) when "
                		+ "finalizing the construction of a graph. Since APs "
                		+ "are often (but not always) the representation of "
                		+ "open valences, the capping procedure serves to "
                		+ "saturate all open valences according to AP's "
                		+ "compatibility. This procedure follows the capping "
                		+ "rules defined in this table.</p>"
                		+ "<br>"
                		+ "<p>Each capping "
                		+ "rule (i.e., each line in this table) identifies "
                		+ "the combination of two attachment point classes "
                		+ "(APClasses): "
                		+ "<ul>"
                		+ "<li>APCLass of the attachment points to be "
                		+ "capped (first column).</li>"
                		+ "<li>APClass of the capping group used to "
                		+ "saturate APs above attachment points.</li>"
                		+ "</ul></p><br>"
                        + "<p>You can select multiple entries as intervals or "
                        + "by multiple clicks while holding the appropriate "
                        + "key (e.g., <code>shift</code>, <code>alt</code>, "
                        + "<code>ctrl</code>, "
                        + "<code>cmd</code> depending on your keyboard "
                        + "settings).</p></html>";
                JOptionPane.showMessageDialog(btnHelpCapping,
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
        
        btnAddFrbEnd = new JButton("Add Forbidden End Rules");
        btnAddFrbEnd.setToolTipText("Define a new forbidden end and add it to "
        		+ "the list.");
        btnAddFrbEnd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
    			DefaultListModel<String> claLstModel = 
    					new DefaultListModel<String>();
    			JList<String> clsList = new JList<String>(claLstModel);
    			for (APClass apc : allAPClasses)
    			{
    				if (!forbiddenEndList.contains(apc))
    				{
    					claLstModel.addElement(apc.toString());
    				}
    			}
    			claLstModel.addElement("<html><b><i>Define a new APClass..."
    					+ "          <i>"
                		+ "</b></html>");
    			if (clsList.getModel().getSize() == 1)
                {
    				clsList.setSelectedIndex(0);
                }
    			
    			int res = JOptionPane.showConfirmDialog(btnAddFrbEnd,
    					new JScrollPane(clsList), 
    					"Choose APClasses", 
    					JOptionPane.OK_CANCEL_OPTION,
    					JOptionPane.PLAIN_MESSAGE, 
    					null);
    			
    			if (res != JOptionPane.OK_OPTION)
    			{
    				return;
    			}

    			for (Integer id : clsList.getSelectedIndices())
    			{
                	if (id.intValue() == (claLstModel.size()-1))
                	{
                		try {
                		    GUIAPClassDefinitionDialog apcDefiner = 
                                    new GUIAPClassDefinitionDialog(
                                            btnAddFrbEnd, true);
                            Object chosen = apcDefiner.showDialog();
                            if (chosen == null)
                            {
                                return;
                            }
                            
                            Object[] pair = (Object[]) chosen;
                            APClass newAPC = APClass.make(pair[0].toString(),
                                    (BondType) pair[1]);
                            
                			if (allAPClasses.contains(newAPC))
                			{
                				JOptionPane.showMessageDialog(btnAddFrbEnd,
    		        					"<html>Class '<code>" + newAPC
    		        					+"</code>' is not new!</html>",
    		        	                "Error",
    		        	                JOptionPane.WARNING_MESSAGE,
    		        	                UIManager.getIcon("OptionPane.errorIcon"));
    		        			return;
                			}
                			tabModFrbEnd.addRow(new Object[]{newAPC});
    	    				forbiddenEndList.add(newAPC);
							allAPClasses.add(newAPC);
							allAPRules.add(newAPC.getRule());
						} catch (DENOPTIMException e1) {
							continue;
						} 
                	}
                	else
                	{
                	    APClass apClass = null;
                        try
                        {
                            apClass = APClass.make(
                                    (String) claLstModel.getElementAt(id));
                        } catch (DENOPTIMException e1)
                        {
                            //this should never happen
                            e1.printStackTrace();
                        }
	    				tabModFrbEnd.addRow(new Object[]{apClass});
	    				forbiddenEndList.add(apClass);
                	}
    			}
			}
		});
        
        btnDelFrbEnd = new JButton("Remove Selected");
        btnDelFrbEnd.setToolTipText(String.format("<html><body width='%1s'>"
        		+ "Remove all the selected "
        		+ "lines in the list. Click on one or more lines to select "
        		+ "them. Multiple lines can be selected by holding the "
        		+ "appropriate key (e.g., <code>shift</code>, "
        		+ "<code>alt</code>, <code>ctrl</code>, <code>cmd</code> "
        		+ "depending on your keyboard settings).</html>",250));
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
        btnSortFrbEnd.setToolTipText("Sorts according to alphabetic order.");
        btnSortFrbEnd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateForbiddenEndsTable();
			}
		});
        
        btnHelpFrbEnd = new JButton("?");
        btnHelpFrbEnd.setToolTipText("Displays the help message.");
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
						+ "key (e.g., <code>shift</code>, <code>alt</code>, "
						+ "<code>ctrl</code>, "
						+ "<code>cmd</code> depending on your keyboard "
						+ "settings).</p></html>";
				JOptionPane.showMessageDialog(btnHelpFrbEnd, 
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

	protected void clearSearchMatches() 
	{
		for (Component lineComponent : panelCPRules.getComponents())
    	{
    		if (lineComponent instanceof CompatibilityRuleLine)
    		{    			
    			((CompatibilityRuleLine) lineComponent).clearMatches();
			}
		}
	}
	
//-----------------------------------------------------------------------------
	
	protected void searchAPClass(String query) 
	{
		clearSearchMatches();
		
		int n = 0;
		if (query.equals(""))
		{
			matchCounter.setText(" 0 matches");
			return;
		}
		
		for (Component lineComponent : panelCPRules.getComponents())
    	{
    		if (lineComponent instanceof CompatibilityRuleLine)
    		{    			
    			CompatibilityRuleLine line = 
    					(CompatibilityRuleLine) lineComponent;
        		int m = line.renderIfMatches("(.*)" + query + "(.*)");
        		n = n+m;
			}
		}
		
		if (n>0)
		{
			btnClearMatch.setEnabled(true);
			if (n == 1)
				matchCounter.setText(" "+n+" match");
			else
				matchCounter.setText(" "+n+" matches");
		}
	}

//-----------------------------------------------------------------------------

	protected void importCPMapFromFile(JComponent parent, File inFile)
	{	
		//Read data from file
        compatMap = new HashMap<APClass,ArrayList<APClass>>();
        cappingMap = new HashMap<APClass,APClass>();
        forbiddenEndList = new HashSet<APClass>();
        try {
			DenoptimIO.readCompatibilityMatrix(inFile.getAbsolutePath(),
						compatMap,
						cappingMap,
						forbiddenEndList);
			allAPClsInCPMap.addAll(compatMap.keySet());
		} catch (DENOPTIMException e) {
			JOptionPane.showMessageDialog(parent,
					"<html>Could not read compatibility matrix data from "
					+ "file<br>'" + inFile + "': " + e.getMessage() + "</html>",
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
        updateCappingRulesTable();
        updateForbiddenEndsTable();
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Writes all the compatibility matrix data to the given file. 
	 * this methods writes all data, that is, compatibility rules, APClass-to-
	 * bond type, capping rules, and definition of forbidden ends.
	 * @param parent the component to which the dialog should be bound.
	 * @param outFile where to write
	 */
	public void writeCopatibilityMatrixFile(JComponent parent, File outFile)
	{
		try {
			DenoptimIO.writeCompatibilityMatrix(outFile.getAbsolutePath(), 
					compatMap, cappingMap, forbiddenEndList);
		} catch (DENOPTIMException e) {
			JOptionPane.showMessageDialog(parent,
					"<html>Could not write compatibility matrix data to "
					+ "file<br>'" + outFile + "'</html>",
	                "Error",
	                JOptionPane.WARNING_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;
		}
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
		for (APClass srcAPClass : allAPClsInCPMap)
		{
			CompatibilityRuleLine r = new CompatibilityRuleLine(srcAPClass);
			r.setAlignmentX(LEFT_ALIGNMENT);
			panelCPRules.add(r);
		}
		panelCPRules.repaint();
		panelCPRules.revalidate();	
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
		ArrayList<APClass> sortedCappings = new ArrayList<APClass>();
	    sortedCappings.addAll(cappingMap.keySet());
	    Collections.sort(sortedCappings);
	    
	    // Re-build table
	    for (APClass apc : sortedCappings)
	    {
	        tabModCapping.addRow(
	                new Object[]{apc.toString(), cappingMap.get(apc)});
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
        ArrayList<APClass> sortedFrbEnds = new ArrayList<APClass>();
        sortedFrbEnds.addAll(forbiddenEndList);
        Collections.sort(sortedFrbEnds);
        
        // Re-build table
        for (APClass apc : sortedFrbEnds)
        {
        	tabModFrbEnd.addRow(new Object[]{apc.toString()});
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
			allAPClasses = new TreeSet<APClass>();
		}
		
		allAPClasses.addAll(compatMap.keySet());
		
		for (ArrayList<APClass> apcs : compatMap.values())
		{
			allAPClasses.addAll(apcs);
		}
		
		for (APClass apc : allAPClasses)
		{
		    allAPRules.add(apc.getRule());
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
			allCapAPClasses = new TreeSet<APClass>();
		}
		allCapAPClasses.addAll(cappingMap.values());
		allAPClasses.addAll(allCapAPClasses);
		for (APClass apc : allAPClasses)
        {
            allAPRules.add(apc.getRule());
        }
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Reads all the APClasses found in a list of files
	 * @param fragLibs the list of file to inspect.
	 * @param cleanup set <code>true</code> to cleanup previous listing of 
	 * APclasses.
	 */
	
	public void importAllAPClassesFromCappingGroupLibs(Set<File> fragLibs,
			boolean cleanup)
	{
		this.setCursor(Cursor.getPredefinedCursor(
				Cursor.WAIT_CURSOR));
		if (cleanup)
		{
			allCapAPClasses = new TreeSet<APClass>();
		}
		
		for (File fragLib : fragLibs)
		{
			allCapAPClasses.addAll(DenoptimIO.readAllAPClasses(fragLib));
		}
		
		for (APClass apc : allCapAPClasses)
		{
		    allAPRules.add(apc.getRule());
		}
		
		this.setCursor(Cursor.getPredefinedCursor(
				Cursor.DEFAULT_CURSOR));
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Reads all the APClasses found in a list of files
	 * @param fragLibs the list of file to inspect.
	 * @param cleanup set <code>true</code> to cleanup previous listing of 
	 * APclasses.
	 */
	
	public void importAllAPClassesFromFragmentLibs(Set<File> fragLibs,
			boolean cleanup)
	{
		this.setCursor(Cursor.getPredefinedCursor(
				Cursor.WAIT_CURSOR));
		if (cleanup)
		{
			allAPClasses = new TreeSet<APClass>();
			allAPRules = new TreeSet<String>();
		}
		
		for (File fragLib : fragLibs)
		{
			allAPClasses.addAll(DenoptimIO.readAllAPClasses(fragLib));
		}

		for (APClass apc : allAPClasses)
        {
            allAPRules.add(apc.getRule());
        }
		
		this.setCursor(Cursor.getPredefinedCursor(
				Cursor.DEFAULT_CURSOR));
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

		private JTextField srcClassName;
		private JScrollPane srcClassNameScroller;
		private JPanel trgClassesPanel;
		private JScrollPane trgClassesScroller;
		private JButton btnAdd;
		private TrgRemovalListener trgDelListener;
		
		private APClass srcAPClass;
		
		private boolean isSelected = false;
		
		private final Dimension minSrcAPClassName = new Dimension(200,26);
		private final Dimension scrollerSize = new Dimension(300,41);
		private final Color SELECTEDBACKGROUND = Color.BLUE;
		private final Color DEFAULTBACKGROUND = 
			UIManager.getLookAndFeelDefaults().getColor("Panel.background");
				
		
	//-------------------------------------------------------------------------

		public CompatibilityRuleLine(APClass srcAPClass)
		{
			this.srcAPClass = srcAPClass;
			this.setName(srcAPClass.toString());
			this.setBackground(DEFAULTBACKGROUND);
			this.setLayout(new BorderLayout());
			
			this.trgDelListener = new TrgRemovalListener(srcAPClass,this);
			
			srcClassName = new JTextField(srcAPClass.toString());
			srcClassName.setBorder(null);
			srcClassName.setOpaque(false);
			srcClassName.setEditable(false);
			srcClassName.setForeground(Color.BLACK);
			srcClassName.setFont(UIManager.getLookAndFeelDefaults()
					.getFont("Label.font"));
			srcClassName.setToolTipText(srcAPClass.toString());
			srcClassName.addMouseListener(this);
			srcClassNameScroller = new JScrollPane(srcClassName,
	        		JScrollPane.VERTICAL_SCROLLBAR_NEVER,
	        		JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			srcClassNameScroller.getHorizontalScrollBar().setPreferredSize(
					new Dimension(0,5));
			srcClassNameScroller.setPreferredSize(minSrcAPClassName);
			srcClassNameScroller.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
			srcClassNameScroller.setOpaque(false);
			srcClassNameScroller.getViewport().setOpaque(false);
			this.add(srcClassNameScroller, BorderLayout.WEST);
			
			trgClassesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			for (APClass trgAPClass : compatMap.get(srcAPClass))
			{
				TargetAPClassToken trg = new TargetAPClassToken(trgAPClass);
				trg.addPropertyChangeListener(
						new PropertyChangeListenerProxy(
						CompatibilityMatrixForm.REMOVETRGAPC,trgDelListener));
				trgClassesPanel.add(trg);
			}
			trgClassesScroller = new JScrollPane(trgClassesPanel,
	        		JScrollPane.VERTICAL_SCROLLBAR_NEVER,
	        		JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			trgClassesScroller.getHorizontalScrollBar().setPreferredSize(
					new Dimension(0,5));
			trgClassesScroller.setPreferredSize(scrollerSize);
	        this.add(trgClassesScroller, BorderLayout.CENTER);
	        
			btnAdd = new JButton("Add");
			btnAdd.setToolTipText(String.format("<html><body width='%1s'>"
					+ "Add more compatible APClasses to source class <i>"
					+ srcAPClass + "</i></html>",250));
			btnAdd.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
	                DefaultListModel<String> trgAPCs =
	                        new DefaultListModel<String>();
	                JList<String> trgClsList = new JList<String>(trgAPCs);
	                for (APClass apc : allAPClasses)
	                {
	                	trgAPCs.addElement(apc.toString());
	                }
	                trgAPCs.addElement("<html><b><i>Define a new APClass...<i>"
	                		+ "</b></html>");
	                trgClsList.setSelectionMode(
	                		ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

	                JScrollPane scrollTrg = new JScrollPane(trgClsList);
	                
	                JOptionPane.showMessageDialog(
	                        null,
	                        scrollTrg,
	                        "Choose Compatible APClasses",
	                        JOptionPane.PLAIN_MESSAGE);

	                if (trgClsList.getSelectedIndices().length > 0)
	                {
		                ArrayList<APClass> trgCPClasses = 
		                        new ArrayList<APClass>();
		                for (Integer id : trgClsList.getSelectedIndices())
		                {
		                	if (id.intValue() == (trgAPCs.size()-1))
		                	{
		                		try {
		                		    GUIAPClassDefinitionDialog apcDefiner = 
	                                        new GUIAPClassDefinitionDialog(
	                                                btnAdd, true);
	                                Object chosen = apcDefiner.showDialog();
	                                if (chosen == null)
	                                {
	                                    return;
	                                }
	                                
	                                Object[] pair = (Object[]) chosen;
	                                APClass newAPC = APClass.make(pair[0].toString(),
	                                        (BondType) pair[1]);
	                                
		                			if (allAPClasses.contains(newAPC))
		                			{
		                				JOptionPane.showMessageDialog(btnAdd,
		    		        					"<html>Class '<code>" + newAPC
		    		        					+"</code>' is not new!</html>",
		    		        	                "Error",
		    		        	                JOptionPane.WARNING_MESSAGE,
		    		        	                UIManager.getIcon(
		    		        	                       "OptionPane.errorIcon"));
		    		        			return;
		                			}
									trgCPClasses.add(newAPC);
									allAPClasses.add(newAPC);
									allAPRules.add(newAPC.getRule());
								} catch (DENOPTIMException e1) {
									continue;
								} 
		                	}
		                	else
		                	{
		                		try
                                {
                                    trgCPClasses.add(APClass.make(
                                    		(String) trgAPCs.getElementAt(id)));
                                } catch (DENOPTIMException e1)
                                {
                                    //this will never happen
                                    e1.printStackTrace();
                                }
		                	}
		                }
		                compatMap.get(srcAPClass).addAll(trgCPClasses);
		                updateAPClassCompatibilitiesList();
	                }
				}
			});
			this.add(btnAdd, BorderLayout.EAST);
			
			this.setBorder(BorderFactory.createRaisedSoftBevelBorder());
			
			addMouseListener(this);
			setFocusable(true);
		}
		
	//-------------------------------------------------------------------------
		
		public void clearMatches()
		{
			for (Component c : trgClassesPanel.getComponents())
			{
				if (c instanceof TargetAPClassToken)
				{
					((TargetAPClassToken) c).renderAsSelected(false);
				}
			}
			isSelected = false;
			renderdAsSelected(false);
		}
		
	//-------------------------------------------------------------------------
		
		/**
		 * Checks if there is any component matching the regex query and 
		 * returns the number of matches
		 * @param regex
		 * @return the number of matches
		 */
		public int renderIfMatches(String regex)
		{
			boolean found = false;
			int n = 0;
			
			if (srcAPClass.toString().matches(regex))
			{
				found = true;
				n = 1;
			}

			for (Component c : trgClassesPanel.getComponents())
			{
				if (c instanceof TargetAPClassToken)
				{
					TargetAPClassToken tac = (TargetAPClassToken) c;
					if (tac.matchesAPClass(regex))
					{
						found = true;
						tac.renderAsSelected(true);
						n++;
					}
				}
			}
			
			if (found)
			{
				isSelected = true;
				renderdAsSelected(true);
			}
			
			return n;
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
		public void mouseClicked(MouseEvent e) 
		{
			if (isSelected)
				isSelected = false;
			else
				isSelected = true;
			renderdAsSelected(isSelected);
		}
	
	//-------------------------------------------------------------------------
	
		@Override
		public void mousePressed(MouseEvent e) {}
	
		@Override
		public void mouseReleased(MouseEvent e) {}
	
		@Override
		public void mouseEntered(MouseEvent e) {}
	
		@Override
		public void mouseExited(MouseEvent e) {}
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Listens for clicks that require removal of a compatible APClass 
	 * (i.e., the target APClass)
	 * from the existing rule of a source APClass.
	 */
	private class TrgRemovalListener implements PropertyChangeListener
	{
		private APClass srcAPClass;
		private Component srcAPClassRulesPanel;
		
		public TrgRemovalListener(APClass srcAPClass, Component panel)
		{
			this.srcAPClass = srcAPClass;
			this.srcAPClassRulesPanel = panel;
		}

		public void propertyChange(PropertyChangeEvent evt) 
		{
			APClass trgAPClass = (APClass) evt.getNewValue();
			compatMap.get(srcAPClass).remove(trgAPClass);
			updateAPClassCompatibilitiesList();
		}
	}
	
//-----------------------------------------------------------------------------
	
	private class CompatRulesHeader extends JPanel
	{
		private Dimension minSrcAPClassName = new Dimension(200,26);
		public final Color DEFAULTBACKGROUND = 
				UIManager.getLookAndFeelDefaults().getColor("Panel.background");
		
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
					+ "<b>Compatible APClasses of incoming fragments:</b></div></html>");
			trgClassTitle.setBackground(DEFAULTBACKGROUND);
			trgClassTitle.setPreferredSize(minSrcAPClassName);
			trgClassTitle.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));

	        this.add(trgClassTitle, BorderLayout.CENTER);

			this.setBorder(BorderFactory.createRaisedSoftBevelBorder());
		}
	}
    
//-----------------------------------------------------------------------------
    
    private class TargetAPClassToken extends JPanel
    {
    	private APClass trgAPClass;
    	private JTextField trgAPClLabel;
    	private JButton btnDel;
    	
    	private final Color BTNPRESS = Color.decode("#fbae9d");
    	private final Color BTNDEF = Color.decode("#f74922");
    	
		private final Color SELECTEDBACKGROUND = Color.BLUE;
		private final Color DEFAULTBACKGROUND = 
				UIManager.getLookAndFeelDefaults().getColor("Panel.background");
    	
    	public TargetAPClassToken(APClass trgAPClass)
    	{
    		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    		this.trgAPClass = trgAPClass;
    		trgAPClLabel = new JTextField(trgAPClass.toString());
    		trgAPClLabel.setBorder(null);
    		trgAPClLabel.setOpaque(false);
    		trgAPClLabel.setEditable(false);
    		trgAPClLabel.setForeground(Color.BLACK);
    		trgAPClLabel.setFont(UIManager.getLookAndFeelDefaults()
					.getFont("Label.font"));
    		this.add(trgAPClLabel);
    		btnDel = new JButton("X");
    		btnDel.setMaximumSize(new Dimension(15,15));
    		btnDel.setBackground(BTNDEF);
    		btnDel.setOpaque(true);
    		btnDel.setBorderPainted(true);
    		btnDel.setBorder(BorderFactory.createRaisedSoftBevelBorder());
    		btnDel.setForeground(Color.BLACK);
    		btnDel.setToolTipText("<html>Remove <code>" + trgAPClass 
    				+ "</code></html>");
    		btnDel.addMouseListener(new MouseListener() {
				
				@Override
				public void mouseReleased(MouseEvent e) 
				{
					firePropertyChange(CompatibilityMatrixForm.REMOVETRGAPC,
							"#", trgAPClass);
				}
				
				@Override
				public void mousePressed(MouseEvent e) 
				{
					btnDel.setBackground(BTNPRESS);	
				}
				
				@Override
				public void mouseExited(MouseEvent e) 
				{
					btnDel.setBackground(BTNDEF);
				}
				
				@Override
				public void mouseEntered(MouseEvent e) {}
				
				@Override
				public void mouseClicked(MouseEvent e) {}
			});
    		this.add(btnDel);
    		this.add(Box.createRigidArea(new Dimension(15,15)));
    	}
    	
    //-------------------------------------------------------------------------
    	
    	public boolean matchesAPClass(String regex)
    	{
    		return trgAPClass.toString().matches(regex);
    	}
    	
    //-------------------------------------------------------------------------
    	
    	public void renderAsSelected(boolean selected)
    	{
			if (selected)
			{
				super.setBackground(SELECTEDBACKGROUND);
				trgAPClLabel.setForeground(Color.WHITE);
			}
			else
			{
				super.setBackground(DEFAULTBACKGROUND);
				trgAPClLabel.setForeground(Color.BLACK);
			}
    	}
    	
    //-------------------------------------------------------------------------
    	
    }
    
//-----------------------------------------------------------------------------
	
}
