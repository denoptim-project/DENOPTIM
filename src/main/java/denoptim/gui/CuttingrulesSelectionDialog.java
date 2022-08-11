/*
 *   DENOPTIM
 *   Copyright (C) 2022 Marco Foscato <marco.foscato@uib.no>
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.graph.rings.RingClosureParameters;
import denoptim.programs.RunTimeParameters.ParametersType;
import denoptim.programs.fragmenter.CuttingRule;
import denoptim.programs.fragmenter.FragmenterParameters;



/**
 * A modal dialog to define parameters for fragmentation and fragment filtering.
 * This dialog allows to control only a limited set of parameters compared to
 * the full capacity of {@link FragmenterParameters}. This because many of the
 * parameters in the latter have no sense to exist in an interactive, 
 * GUI-controlled fragmentation.
 */

class CuttingrulesSelectionDialog extends GUIModalDialog
{
    /**
     * Version ID
     */
    private static final long serialVersionUID = 1L;
    
    private JRadioButton rdbUseDefault;
    private JRadioButton rdbUseCustom;
    
    private DefaultTableModel defaultRulesTabModel;
    private DefaultTableModel customRulesTabModel;
    
//-----------------------------------------------------------------------------
    
    /**
     * Constructor
     * @param defaultCuttingRules 
     * @param paramsHolder 
     */
    public CuttingrulesSelectionDialog(List<CuttingRule> defaultCuttingRules)
    {
        super();
        setTitle("Choose Cutting Rules");
        
        rdbUseDefault = new JRadioButton("Use default cutting rules.");
        rdbUseDefault.setSelected(true);
        rdbUseCustom = new JRadioButton(
                "Use the following curtomized cutting rules.");
        rdbUseCustom.setSelected(false);
        ButtonGroup rdbGroup = new ButtonGroup();
        rdbGroup.add(rdbUseDefault);
        rdbGroup.add(rdbUseCustom);
        
        defaultRulesTabModel = new DefaultTableModel();
        defaultRulesTabModel.setColumnCount(6);
        String column_names[]= {"<html><b>Rule Name</b></html>", 
                "<html><b>Order of Use</b></html>",
                "<html><b>SMARTS Atom 1</b></html>",
                "<html><b>SMARTS Bond 1-2</b></html>",
                "<html><b>SMARTS Atom 2</b></html>",
                "<html><b>Option</b></html>"};
        defaultRulesTabModel.setColumnIdentifiers(column_names);
        JTable defaultRulesTable = new JTable(defaultRulesTabModel);
        defaultRulesTable.setToolTipText("Double click to edit any entry.");
        defaultRulesTable.putClientProperty("terminateEditOnFocusLost", 
                Boolean.TRUE);
        for (CuttingRule cr : defaultCuttingRules)
        {
            String[] values = new String[6];
            values[0] = cr.getName();
            values[1] = String.valueOf(cr.getPriority());
            values[2] = cr.getSMARTSAtom0();
            values[3] = cr.getSMARTSBnd();
            values[4] = cr.getSMARTSAtom1();
            StringBuilder sb = new StringBuilder();
            cr.getOptions().stream().forEach(s -> sb.append(" " + s));
            values[5] = sb.toString();
            defaultRulesTabModel.addRow(values);
        }
        JScrollPane defaultRulesScrollPane = new JScrollPane(defaultRulesTable);
        defaultRulesTable.getTableHeader().setPreferredSize(
                new Dimension(defaultRulesScrollPane.getWidth(), 40));
        
        customRulesTabModel = new DefaultTableModel();
        customRulesTabModel.setColumnCount(6);
        customRulesTabModel.setColumnIdentifiers(column_names);
        JTable customRulesTable = new JTable(customRulesTabModel) {
            @Override
            public Dimension getPreferredScrollableViewportSize() {
                return new Dimension(super.getPreferredSize().width,
                    getRowHeight() * getRowCount());
            }
        };;
        customRulesTable.setToolTipText("Double click to edit any entry.");
        customRulesTable.putClientProperty("terminateEditOnFocusLost", 
                Boolean.TRUE);
        String[] emptyRow = new String[] {
                "double click to edit...","","","","",""};
        customRulesTabModel.addRow(emptyRow);
        
        JButton btnAddCustomRule = new JButton("Add Rule");
        btnAddCustomRule.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                customRulesTabModel.addRow(emptyRow);
            }
        });
        JButton btnRemoveCustomRule = new JButton("Remove Selected");
        btnRemoveCustomRule.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (customRulesTable.getRowCount() > 0) 
                {
                    if (customRulesTable.getSelectedRowCount() > 0) 
                    {
                        int selectedRowIds[] = 
                                customRulesTable.getSelectedRows();
                        Arrays.sort(selectedRowIds);
                        for (int i=(selectedRowIds.length-1); i>-1; i--) 
                        {
                            customRulesTabModel.removeRow(
                                    selectedRowIds[i]);
                        }
                    }
                }
            }
        });
        
        JScrollPane customRulesScrollPane = new JScrollPane(customRulesTable);
        customRulesTable.getTableHeader().setPreferredSize(
                new Dimension(customRulesScrollPane.getWidth(), 40));
        
        JPanel headerDefaultBlock = new JPanel(new BorderLayout());
        headerDefaultBlock.add(rdbUseDefault, BorderLayout.WEST);
        
        JPanel defaultRulesPanel = new JPanel(new BorderLayout());
        defaultRulesPanel.add(headerDefaultBlock, BorderLayout.NORTH);
        defaultRulesPanel.add(defaultRulesScrollPane, BorderLayout.CENTER);

        JPanel headerCustomBlock = new JPanel(new BorderLayout());
        headerCustomBlock.add(rdbUseCustom, BorderLayout.WEST);
        headerCustomBlock.add(btnAddCustomRule, BorderLayout.CENTER);
        headerCustomBlock.add(btnRemoveCustomRule, BorderLayout.EAST);
        
        JPanel customRulesPanel = new JPanel(new BorderLayout());
        customRulesPanel.add(headerCustomBlock, BorderLayout.NORTH);
        customRulesPanel.add(customRulesScrollPane, BorderLayout.CENTER);
        JSplitPane masterPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        masterPane.setTopComponent(defaultRulesPanel);
        masterPane.setBottomComponent(customRulesPanel);
        
        addToCentralPane(masterPane);
        
        btnDone.setText("Start Fragmentation");
        btnDone.setToolTipText(String.format("<html><body width='%1s'>"
                + "Uses the selected rules to produce fragments.</html>",400));
        
        // NB: Assumption: 1 action listener inherited from superclass.
        // We want to remove it because we need to acquire full control over
        // when the modal panel has to be closed.
        btnDone.removeActionListener(btnDone.getActionListeners()[0]);
        btnDone.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) 
            {   
                DefaultTableModel chosenTab = defaultRulesTabModel;
                if (rdbUseCustom.isSelected())
                    chosenTab = customRulesTabModel;
                    
                List<CuttingRule> chosenOnes = new ArrayList<CuttingRule>();
                for (int iRow=0; iRow<chosenTab.getRowCount(); iRow++)
                {
                    String[] values = new String[5];
                    for (int iCol=0; iCol<5; iCol++)
                    {
                        values[iCol] = chosenTab.getValueAt(iRow,iCol)
                                .toString().trim();
                    }
                    ArrayList<String> opts = new ArrayList<String>(
                            Arrays.asList(chosenTab.getValueAt(iRow,5)
                                    .toString().trim().split("\\s+")));
                    
                    int prioIdx = 0;
                    try {
                        prioIdx = Integer.parseInt(values[1]);
                    } catch (Throwable t)
                    {
                        JOptionPane.showMessageDialog(btnDone,String.format(
                                "<html><body width='%1s'"
                                + "Could not convert string '" + values[1]
                                + "' into an integer. Ignoring row " + iRow 
                                + " in table of custom cutting rules.</html>",
                                    400),
                                "Error",
                                JOptionPane.ERROR_MESSAGE,
                                UIManager.getIcon("OptionPane.errorIcon"));
                        continue;
                    }
                    chosenOnes.add(new CuttingRule(values[0], values[2], 
                            values[4], values[3], prioIdx, 
                            opts));
                }
                result = chosenOnes;
                close();
            }
        });
        
        this.btnCanc.setToolTipText("Exit without running fragmentation.");
    }
	
//-----------------------------------------------------------------------------

}