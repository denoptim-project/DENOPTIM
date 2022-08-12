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

import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
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
import denoptim.files.FileFormat;
import denoptim.files.FileUtils;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.graph.rings.RingClosureParameters;
import denoptim.io.DenoptimIO;
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
    
    /**
     * The file where we last saved cutting rules different from the default.
     */
    protected File lastUsedCutRulFile = null;
    
    /**
     * The file where we will save next edited list of cutting rules
     */
    protected File nextWrittenCutRulFile = null;
    
    /**
     * Flag indicating whether to preselect the default or the custom list of 
     * cutting rules next time we are asked to display the dialog.
     */
    protected boolean useDefaultNextTime = true;
    
//-----------------------------------------------------------------------------
    
    /**
     * Constructor
     * @param defaultCuttingRules 
     * @param lastUsedCutRulFile 
     * @throws DENOPTIMException 
     */
    public CuttingrulesSelectionDialog(List<CuttingRule> defaultCuttingRules,
            List<CuttingRule> customCuttingRules, boolean preselectDefault) 
    {
        super();
        setTitle("Choose Cutting Rules");
        
        rdbUseDefault = new JRadioButton("Use default cutting rules.");
        rdbUseCustom = new JRadioButton(
                "Use the following curtomized cutting rules.");

        if (preselectDefault)
        {
            rdbUseDefault.setSelected(true);
            rdbUseCustom.setSelected(false);
        } else {
            rdbUseDefault.setSelected(false);
            rdbUseCustom.setSelected(true);
        }
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
        try
        {
            nextWrittenCutRulFile = getTmpFileForCuttingRules();
        } catch (DENOPTIMException e2)
        {
            e2.printStackTrace();
            nextWrittenCutRulFile = new File("edited_cuttingrules");
        }
        defaultRulesTable.setToolTipText("<html>"
                + "Double click to edit any entry. <br>"
                + "Edited lists are saved in '" 
                + nextWrittenCutRulFile.getAbsolutePath() + "'</html>");
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
        
        String[] emptyRow = new String[] {
                "double click to edit...","","","","",""};
        
        JButton btnAddDefaultRule = new JButton("Add Rule");
        btnAddDefaultRule.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                defaultRulesTabModel.addRow(emptyRow);
            }
        });
        JButton btnRemoveDefaultRule = new JButton("Remove Selected");
        btnRemoveDefaultRule.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (defaultRulesTable.getRowCount() > 0) 
                {
                    if (defaultRulesTable.getSelectedRowCount() > 0) 
                    {
                        int selectedRowIds[] = 
                                defaultRulesTable.getSelectedRows();
                        Arrays.sort(selectedRowIds);
                        for (int i=(selectedRowIds.length-1); i>-1; i--) 
                        {
                            defaultRulesTabModel.removeRow(
                                    selectedRowIds[i]);
                        }
                    }
                }
            }
        });
        
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
        customRulesTable.setToolTipText("<html>"
                + "Double click to edit any entry. <br>"
                + "Edited lists are saved in '" 
                + nextWrittenCutRulFile.getAbsolutePath() + "'</html>");
        customRulesTable.putClientProperty("terminateEditOnFocusLost", 
                Boolean.TRUE);
        if (customCuttingRules.size()==0)
        {
            customRulesTabModel.addRow(emptyRow);
        } else {
            for (CuttingRule cr : customCuttingRules)
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
                customRulesTabModel.addRow(values);
            }
        }
        
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
        

        JPanel pnlBottonsDefaultRules = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlBottonsDefaultRules.add(btnAddDefaultRule);
        pnlBottonsDefaultRules.add(btnRemoveDefaultRule);
        
        JPanel headerDefaultBlock = new JPanel(new BorderLayout());
        headerDefaultBlock.add(rdbUseDefault, BorderLayout.WEST);
        headerDefaultBlock.add(pnlBottonsDefaultRules, BorderLayout.CENTER);
        
        JPanel defaultRulesPanel = new JPanel(new BorderLayout());
        defaultRulesPanel.add(headerDefaultBlock, BorderLayout.NORTH);
        defaultRulesPanel.add(defaultRulesScrollPane, BorderLayout.CENTER);
        
        
        JPanel pnlBottonsCustomRules = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlBottonsCustomRules.add(btnAddCustomRule);
        pnlBottonsCustomRules.add(btnRemoveCustomRule);

        JPanel headerCustomBlock = new JPanel(new BorderLayout());
        headerCustomBlock.add(rdbUseCustom, BorderLayout.WEST);
        headerCustomBlock.add(pnlBottonsCustomRules, BorderLayout.CENTER);
        
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
                
                boolean setOfCuttingRulesHasBeenModified = false;
                    
                List<CuttingRule> chosenOnes = new ArrayList<CuttingRule>();
                for (int iRow=0; iRow<chosenTab.getRowCount(); iRow++)
                {
                    String[] values = new String[5];
                    for (int iCol=0; iCol<5; iCol++)
                    {
                        values[iCol] = chosenTab.getValueAt(iRow,iCol)
                                .toString().trim();
                    }
                    ArrayList<String> opts = new ArrayList<String>();
                    if (!chosenTab.getValueAt(iRow,5).toString().trim().isBlank())
                        opts.addAll(Arrays.asList(chosenTab.getValueAt(iRow,5)
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
                    
                    CuttingRule newCuttingRule = new CuttingRule(values[0], 
                            values[2], values[4], values[3], prioIdx, opts);
                    chosenOnes.add(newCuttingRule);
                    
                    // Verify introduction of differences
                    if (!setOfCuttingRulesHasBeenModified)
                    {
                        boolean fromDefault = false;
                        if (defaultCuttingRules.size() > iRow)
                        {
                            fromDefault = defaultCuttingRules.get(iRow).equals(
                                    newCuttingRule);
                        }
                        boolean fromCustom = false;
                        if (customCuttingRules.size() > iRow)
                        {
                            fromCustom = customCuttingRules.get(iRow).equals(
                                    newCuttingRule);
                        }
                        if (!fromDefault && !fromCustom)
                        {
                            setOfCuttingRulesHasBeenModified = true;
                        }
                    }
                }
                
                // Verify shortening of the list of rules (case jnot covered above)
                if (chosenOnes.size() != defaultCuttingRules.size()
                        && chosenOnes.size() != customCuttingRules.size())
                {
                    setOfCuttingRulesHasBeenModified = true;
                }
                
                //Store a copy of the modified/customized rules
                useDefaultNextTime = false;
                if (setOfCuttingRulesHasBeenModified)
                {
                    try
                    {
                        lastUsedCutRulFile = nextWrittenCutRulFile;
                        DenoptimIO.writeCuttingRules(lastUsedCutRulFile, 
                                chosenOnes);
                        FileUtils.addToRecentFiles(lastUsedCutRulFile, 
                                FileFormat.CUTRULE);
                    } catch (DENOPTIMException e1)
                    {
                        e1.printStackTrace();
                    }
                } else {
                    if (rdbUseDefault.isSelected())
                        useDefaultNextTime = true;
                }
                
                result = chosenOnes;
                close();
            }
        });
        
        this.btnCanc.setToolTipText("Exit without running fragmentation.");
    }
    
//-----------------------------------------------------------------------------
    
    private File getTmpFileForCuttingRules() throws DENOPTIMException
    {
        File parent = new File(GUIPreferences.tmpSpace);
        return FileUtils.getAvailableFileName(parent, "cuttingRules");
    }
	
//-----------------------------------------------------------------------------

}