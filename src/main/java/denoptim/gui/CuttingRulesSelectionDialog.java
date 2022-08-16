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

import java.awt.Graphics;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

import org.apache.batik.bridge.UpdateManagerEvent;
import org.apache.batik.bridge.UpdateManagerListener;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.JSVGScrollPane;
import org.apache.batik.swing.gvt.AbstractImageZoomInteractor;
import org.apache.batik.swing.gvt.AbstractPanInteractor;
import org.apache.batik.swing.gvt.AbstractResetTransformInteractor;
import org.apache.batik.swing.gvt.AbstractRotateInteractor;
import org.apache.batik.swing.gvt.AbstractZoomInteractor;
import org.apache.batik.swing.gvt.GVTTreeRendererEvent;
import org.apache.batik.swing.gvt.GVTTreeRendererListener;
import org.apache.batik.swing.gvt.Interactor;
import org.apache.batik.swing.gvt.JGVTComponent;
import org.apache.batik.swing.gvt.JGVTComponentListener;
import org.apache.batik.swing.svg.GVTTreeBuilderEvent;
import org.apache.batik.swing.svg.GVTTreeBuilderListener;

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

class CuttingRulesSelectionDialog extends GUIModalDialog
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
    public CuttingRulesSelectionDialog(List<CuttingRule> defaultCuttingRules,
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
        defaultRulesTabModel.setColumnCount(7);
        String column_names[]= {"<html><b>Rule Name</b></html>", 
                "<html><b>Order of Use</b></html>",
                "<html><b>SMARTS Atom 1</b></html>",
                "<html><b>SMARTS Bond 1-2</b></html>",
                "<html><b>SMARTS Atom 2</b></html>",
                "<html><b>Options</b></html>"};
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
        defaultRulesTable.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(FocusEvent e)
            {
                rdbUseDefault.setSelected(true);
            }

            @Override
            public void focusLost(FocusEvent e) {}
        });
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
        JButton btnViewSelectedDefaultRule = new SMARTSVisualizationButton(
                defaultRulesTable, defaultRulesTabModel);
        
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
        customRulesTable.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(FocusEvent e)
            {
                rdbUseCustom.setSelected(true);
            }

            @Override
            public void focusLost(FocusEvent e) {}
        });
        buildCustomRulesTable(customCuttingRules, emptyRow);
        
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
        JButton btnViewSelectedCustomRule = new SMARTSVisualizationButton(
                customRulesTable, customRulesTabModel);
        JButton btnImportRules = new JButton("Import Rules...");
        btnImportRules.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                customCuttingRules.clear();
                File cutRulFile = GUIFileOpener.pickFile(btnImportRules);
                try
                {
                    DenoptimIO.readCuttingRules(cutRulFile, customCuttingRules);
                } catch (DENOPTIMException e1)
                {
                    JOptionPane.showMessageDialog(btnImportRules,String.format(
                            "<html><body width='%1s'"
                            + "Could not read cutting rules from '" 
                            + cutRulFile.getAbsolutePath() + "'. Hint: "
                            + e1.getMessage() + "</html>", 400),
                            "Error",
                            JOptionPane.ERROR_MESSAGE,
                            UIManager.getIcon("OptionPane.errorIcon"));
                    return;
                }
                buildCustomRulesTable(customCuttingRules, emptyRow);
                rdbUseCustom.setSelected(true);
                pack();
            }
        });
        
        JScrollPane customRulesScrollPane = new JScrollPane(customRulesTable);
        customRulesTable.getTableHeader().setPreferredSize(
                new Dimension(customRulesScrollPane.getWidth(), 40));
        

        JPanel pnlButtonsDefaultRules = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlButtonsDefaultRules.add(btnAddDefaultRule);
        pnlButtonsDefaultRules.add(btnRemoveDefaultRule);
        pnlButtonsDefaultRules.add(btnViewSelectedDefaultRule);
        
        JPanel headerDefaultBlock = new JPanel(new BorderLayout());
        headerDefaultBlock.add(rdbUseDefault, BorderLayout.WEST);
        headerDefaultBlock.add(pnlButtonsDefaultRules, BorderLayout.CENTER);
        
        JPanel defaultRulesPanel = new JPanel(new BorderLayout());
        defaultRulesPanel.add(headerDefaultBlock, BorderLayout.NORTH);
        defaultRulesPanel.add(defaultRulesScrollPane, BorderLayout.CENTER);
        
        
        JPanel pnlButtonsCustomRules = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlButtonsCustomRules.add(btnAddCustomRule);
        pnlButtonsCustomRules.add(btnRemoveCustomRule);
        pnlButtonsCustomRules.add(btnViewSelectedCustomRule);
        pnlButtonsCustomRules.add(btnImportRules);

        JPanel headerCustomBlock = new JPanel(new BorderLayout());
        headerCustomBlock.add(rdbUseCustom, BorderLayout.WEST);
        headerCustomBlock.add(pnlButtonsCustomRules, BorderLayout.CENTER);
        
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
                                "<html><body width='%1s'>"
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
                
                // Verify shortening of the list of rules (case not covered above)
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
        getRootPane().setDefaultButton(btnDone);
        
        this.btnCanc.setToolTipText("Exit without running fragmentation.");
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * A button that opens a modal dialog displaying the PNGs with a visual
     * representation of SMARTS queries that are selected in the table given
     * upon construction.
     */
    private class SMARTSVisualizationButton extends JButton
    {
        /**
         * A reference to this very instance. Only needed to place child dialogs.
         */
        private Component refToThis;
        
        private String templateURL = "https://smarts.plus/smartsview/"
                + "download_rest?smarts="
                + "SMARTS;"
                + "filetype=svg;"
                + "vmode=0;"
                + "textdesc=1;"
                + "depsymbols=1;"
                + "smartsheading=0"; //makes text size too small for long SMARTS
        
        public SMARTSVisualizationButton(JTable table, 
                DefaultTableModel tabModel)
        {
            super("Visualize Selected...");
            refToThis = this;
            addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    if (table.getRowCount()==0 
                            || table.getSelectedRowCount()==0) 
                    {
                        JOptionPane.showMessageDialog(refToThis, 
                                "No SMARTS selected.", 
                                "Visualization of SMARTS", 
                                JOptionPane.PLAIN_MESSAGE);
                        return;
                    }
                    
                    JPanel masterPanel = new JPanel();
                    masterPanel.setLayout(new BoxLayout(masterPanel, 
                            BoxLayout.PAGE_AXIS));

                    int selectedRowIds[] = table.getSelectedRows();
                    Arrays.sort(selectedRowIds);
                    for (int i=0; i<selectedRowIds.length; i++) 
                    {
                        String smarts = tabModel.getValueAt(
                                        selectedRowIds[i],2).toString()
                                + tabModel.getValueAt(
                                        selectedRowIds[i],3).toString()
                                + tabModel.getValueAt(
                                        selectedRowIds[i],4).toString();

                        smarts = escapeCharactersForSMARTSViewer(smarts);
                        String url = templateURL.replace("SMARTS", smarts);

                        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
                        header.add(new JLabel(tabModel.getValueAt(
                                selectedRowIds[i],0).toString() + " - "));
                        
                        JLabel link = new JLabel("Download SVG");
                        link.setForeground(Color.BLUE.darker());
                        link.setCursor(Cursor.getPredefinedCursor(
                                Cursor.HAND_CURSOR));
                        link.addMouseListener(new MouseAdapter() {
                            
                            @Override
                            public void mouseClicked(MouseEvent e) {
                                try {
                                    Desktop.getDesktop().browse(new URI(url));
                                } catch (IOException | URISyntaxException e1) {
                                    e1.printStackTrace();
                                }
                            }
                        });
                        header.add(link);
                        
                        // This is a trick to set the minimal size of the frame
                        header.add(new JLabel("                             "
                                + "                                         "));
                        
                        masterPanel.add(header);
                        
                        MyJSVGCanvas svgCanvas = new MyJSVGCanvas();
                        svgCanvas.setURI(url);
                        masterPanel.add(svgCanvas);
                    }
                    GUIModalDialog dialog = new GUIModalDialog(true);
                    dialog.addToCentralPane(masterPanel);
                    dialog.btnCanc.setVisible(false);
                    dialog.btnExtra.setText("?");
                    dialog.btnExtra.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e)
                        {
                            JOptionPane.showMessageDialog(dialog.btnExtra, 
                                    String.format("<html><body width='%1s'>"
                    + "<p>Visual representations of SMARTS are depicted by the "
                    + "SMARTSview service offered "
                    + "by the ZBH - Center for Bioinformatics of the "
                    + "University of Hamburg (visit https://smarts.plus/).</p>"
                    + "<br><ul>"
                    + "<li>Use the 'Download SVG' button to download the "
                    + "figure.</li>"
                    + "<li>Pan the image in any direction by dragging the mouse "
                    + "(left button).</li>"
                    + "<li>Zoom in/out by holding the SHIFT key while dragging "
                    + "down-/up-wards.</li></ul></html>", 400), 
                                    "Visualization of SMARTS - Instructions", 
                                    JOptionPane.PLAIN_MESSAGE);
                            return;
                        }
                    });
                    dialog.showDialog();
                }
            });
        }
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Special canvas that overrides the set interpretation of mouse+key input
     * to achieve a simplified zoom/pan capability using only BUTTON1 and
     * SHIFT or CTRL keys.
     */
    private class MyJSVGCanvas extends JSVGCanvas
    {
        /**
         * Version ID
         */
        private static final long serialVersionUID = 1L;

        public MyJSVGCanvas()
        {
            super();
            
            // To change the interaction modes. First, disable the defaults
            setEnableZoomInteractor(false);
            setEnableImageZoomInteractor(false);
            setEnablePanInteractor(false);
            setEnableRotateInteractor(false);
            setEnableResetTransformInteractor(false);
            
            // Override the  disabled default interactors that are still saved 
            // as fields.
            
            // Then, we add our own interpreters of the mouse+key input
            
            /**
             * Excluded because it does only zoom-in, no zoom-out.
             */
            zoomInteractor = null;
            
            /**
             * Zoom view in/out as a response to SHIFT+BUTTON1 drag down/up
             */
            imageZoomInteractor = new AbstractImageZoomInteractor() {
                public boolean startInteraction(InputEvent ie) {
                    int mods = ie.getModifiersEx();
                    return
                            ie.getID() == MouseEvent.MOUSE_PRESSED &&
                            (mods & InputEvent.BUTTON1_DOWN_MASK) != 0 &&
                            (mods & InputEvent.SHIFT_DOWN_MASK) != 0;
                }
            };
            
            /**
             * Pan view with BUTTON1 drag
             */
            panInteractor = new AbstractPanInteractor() {
                public boolean startInteraction(InputEvent ie) {
                    int mods = ie.getModifiersEx();
                    return
                        ie.getID() == MouseEvent.MOUSE_PRESSED &&
                        (mods & InputEvent.BUTTON1_DOWN_MASK) != 0;
                }
                
                /**
                 * Prevents the action from being interrupted when mouse exits 
                 * the component.
                 */
                @Override
                public void mouseExited(MouseEvent e) {}
            };

            /**
             * No reason to allow rotation of imae containing text.
             */
            rotateInteractor = null;
            
            /**
             * Reset view with CTRL+BUTTON1
             */
            resetTransformInteractor = new AbstractResetTransformInteractor() {
                public boolean startInteraction(InputEvent ie) {
                    int mods = ie.getModifiersEx();
                    return
                        ie.getID() == MouseEvent.MOUSE_PRESSED &&
                        (mods & InputEvent.CTRL_DOWN_MASK) != 0;
                }
            };
            // Finally, enable the desired interactors. It is crucial to do this
            // in the right order because the interactors are added to a List
            // that retains the order and the events are interpreted so that
            // the first interactor that returns true from startInteraction
            // does the action, the others are not tested. Therefore, pan
            // interactor, which has the less restrictive mask, must go last.
            setEnableImageZoomInteractor(true);
            setEnableResetTransformInteractor(true);
            setEnablePanInteractor(true);
        }
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Escaping some characters as explained in 
     * <a href="https://smarts.plus/rest">https://smarts.plus/rest</a>.
     */
    private String escapeCharactersForSMARTSViewer(String smarts)
    {
        String escapedSmarts = smarts;
        escapedSmarts = escapedSmarts.replaceAll("%", "%25");
        escapedSmarts = escapedSmarts.replaceAll("&", "%26");
        escapedSmarts = escapedSmarts.replaceAll("\\+", "%2B");
        escapedSmarts = escapedSmarts.replaceAll("#", "%23");
        escapedSmarts = escapedSmarts.replaceAll(";", "%3B");
        
        return escapedSmarts;
    }

//-----------------------------------------------------------------------------
    
    private void buildCustomRulesTable(List<CuttingRule> customCuttingRules,
            String[] emptyRow)
    {
        for (int iRow=(customRulesTabModel.getRowCount()-1); iRow>-1; iRow--)
        {
            customRulesTabModel.removeRow(iRow);
        }
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
    }
    
//-----------------------------------------------------------------------------
    
    private File getTmpFileForCuttingRules() throws DENOPTIMException
    {
        File parent = new File(GUIPreferences.tmpSpace);
        return FileUtils.getAvailableFileName(parent, "cuttingRules");
    }
	
//-----------------------------------------------------------------------------

}