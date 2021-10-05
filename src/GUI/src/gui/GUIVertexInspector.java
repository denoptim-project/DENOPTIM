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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.vecmath.Point3d;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.io.FileFormat;
import denoptim.molecule.APClass;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMFragment;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.molecule.EmptyVertex;
import denoptim.utils.DENOPTIMMoleculeUtils;

import static denoptim.molecule.DENOPTIMVertex.*;


/**
 * A panel with a viewer capable of visualising DENOPTIM fragments
 * and allows to create and edit fragments.
 * The molecular viewer is provided by Jmol.
 * 
 * @author Marco Foscato
 */

public class GUIVertexInspector extends GUICardPanel
{
	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 912850110991449553L;
	
	/**
	 * Unique identified for instances of this inspector
	 */
	public static AtomicInteger prepVrtxTabUID = new AtomicInteger(1);
	
	/**
	 * The currently loaded list of fragments
	 */
	private ArrayList<DENOPTIMVertex> verticesLibrary =
			new ArrayList<DENOPTIMVertex>();
	
	/**
	 * The currently loaded vertex
	 */
	private DENOPTIMVertex vertex;
	
	/**
	 * The index of the currently loaded fragment [0–(n-1)}
	 */
	private int currVrtxIdx = 0;
	
	/**
	 * Flag signaling that loaded data has changes since last save
	 */
	private boolean unsavedChanges = false;
	
	private VertexViewPanel vertexViewer;
	private JPanel ctrlPane;
	private JPanel navigPanel;
	private JPanel navigPanel2;
	private JPanel navigPanel3;
	
	private JButton btnAddVrtx;
	private JButton btnDelVrtx;
	
	private JButton btnOpenVrtxs;
	
	private JSpinner navigSpinner;
	private JLabel totalVrtxsLabel;
	private final VrtxSpinnerChangeEvent vrtxSpinnerListener = 
			new VrtxSpinnerChangeEvent();
	
	private JPanel pnlImportStruct;
	private JButton btnOpenMol;
	private JButton btnOpenSMILES;
	
    private JPanel pnlEmptFrag;
    private JButton btnEmptFrag;
    
	private JPanel pnlAtmToAP;
	private JButton btnAtmToAP;
	
	private JPanel pnlDelSel;
	private JButton btnDelSel;
	
	private JPanel pnlSaveEdits;
	private JButton btnSaveEdits;

	
//-----------------------------------------------------------------------------
	
	/**
	 * Constructor
	 */
	public GUIVertexInspector(GUIMainPanel mainPanel)
	{
		super(mainPanel, "Vertex Inspector #" + prepVrtxTabUID.getAndIncrement());
		super.setLayout(new BorderLayout());
		initialize();
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Initialize the panel and add buttons.
	 */
	private void initialize() {
		
		// BorderLayout is needed to allow dynamic resizing!
		this.setLayout(new BorderLayout()); 
		
		// This card structure includes center, east and south panels:
		// - (Center) molecular/graph viewer and APs
		// - (East) vertex controls
		// - (South) general controls (load, save, close)
		
		// The viewer with Jmol and APtable
		vertexViewer = new VertexViewPanel(this,true);
		vertexViewer.addPropertyChangeListener(
		        IVertexAPSelection.APDATACHANGEEVENT, 
		        new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				protectEditedSystem();				
			}
		});
        this.add(vertexViewer,BorderLayout.CENTER);
		
		// General panel on the right: it containing all controls
        ctrlPane = new JPanel();
        ctrlPane.setVisible(true);
        ctrlPane.setLayout(new BoxLayout(ctrlPane, SwingConstants.VERTICAL));
        ctrlPane.add(new JSeparator());
		
        // NB: avoid GroupLayout because it interferes with Jmol viewer and causes exception
        
        // Controls to navigate the list of vertices
        navigPanel = new JPanel();
        navigPanel2 = new JPanel();
        navigPanel3 = new JPanel();
        JLabel navigationLabel1 = new JLabel("Vertex # ");
        JLabel navigationLabel2 = new JLabel("Current library size: ");
        totalVrtxsLabel = new JLabel("0");
        
		navigSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 0, 1));
		navigSpinner.setToolTipText("Move to vertex number # in the currently loaded library.");
		navigSpinner.setPreferredSize(new Dimension(75,20));
		navigSpinner.addChangeListener(vrtxSpinnerListener);
        navigPanel.add(navigationLabel1);
		navigPanel.add(navigSpinner);
		ctrlPane.add(navigPanel);
		
        navigPanel2.add(navigationLabel2);
        navigPanel2.add(totalVrtxsLabel);
		ctrlPane.add(navigPanel2);
		
		btnAddVrtx = new JButton("Add");
		btnAddVrtx.setToolTipText("Append vertices taken from a file.");
		btnAddVrtx.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File inFile = GUIFileOpener.pickFile(btnAddVrtx);
				if (inFile == null || inFile.getAbsolutePath().equals(""))
				{
					return;
				}
				
				ArrayList<DENOPTIMVertex> vrtxLib = new ArrayList<>();
				try {
				    DenoptimIO.appendVerticesFromFileToLibrary(inFile,
				            BBType.FRAGMENT, vrtxLib, true);
				} catch (Exception e1) {
					e1.printStackTrace();
					JOptionPane.showMessageDialog(btnAddVrtx,
			                "<html>Could not read building blocks from file"
			                + "<br>'" + inFile + "'"
			                + "<br>Hint on cause: " + e1.getMessage() 
			                +"</html>",
			                "Error",
			                JOptionPane.ERROR_MESSAGE,
			                UIManager.getIcon("OptionPane.errorIcon"));
					return;
				}

				if (vrtxLib.size() == 0)
				{
					JOptionPane.showMessageDialog(btnAddVrtx,
			                "<html>No building blocks in file"
			                + "<br>'" + inFile + "'</html>",
			                "Error",
			                JOptionPane.ERROR_MESSAGE,
			                UIManager.getIcon("OptionPane.errorIcon"));
					return;
				}
				
				if (vrtxLib.size() == 1)
				{
				    importVertices(vrtxLib);
					return;
				}
				
				String[] options = new String[]{"All", 
						"Selected",
						"Cancel"};
				String txt = "<html><body width='%1s'>Do you want to "
						+ "append all building blocks or only selected ones?"
						+ "</html>";
				int res = JOptionPane.showOptionDialog(btnAddVrtx,
		                String.format(txt,200),
		                "Append Building Blocks",
		                JOptionPane.DEFAULT_OPTION,
		                JOptionPane.QUESTION_MESSAGE,
		                UIManager.getIcon("OptionPane.warningIcon"),
		                options,
		                options[0]);
				
				if (res == 2)
				{
					return;
				}
				
				switch (res)
				{
					case 0:
					    importVertices(vrtxLib);
						break;
						
					case 1:
						ArrayList<DENOPTIMVertex> selectedVrtxs = 
								new ArrayList<DENOPTIMVertex>();
						GUIVertexSelector vrtxSelector = 
						        new GUIVertexSelector(vrtxLib,true);
						vrtxSelector.setRequireApSelection(false);
						Object selected = vrtxSelector.showDialog();

						if (selected != null)
						{
						    ArrayList<ArrayList<Integer>> selList = 
						            (ArrayList<ArrayList<Integer>>) selected;
						    for (ArrayList<Integer> pair : selList)
						    {
						        selectedVrtxs.add(vrtxLib.get(pair.get(0)));
						    }
						}
						importVertices(selectedVrtxs);
						break;
					
					default:
						return;
				}
			}
		});
		btnDelVrtx = new JButton("Remove");
		btnDelVrtx.setToolTipText("Remove the present building block from the "
		        + "library.");
		btnDelVrtx.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					removeCurrentVrtx();
				} catch (DENOPTIMException e1) {
					System.out.println("Esception while removing the current "
					        + "building block:");
					e1.printStackTrace();
				}
			}
		});
		navigPanel3.add(btnAddVrtx);
		navigPanel3.add(btnDelVrtx);
		ctrlPane.add(navigPanel3);
		
		ctrlPane.add(new JSeparator());
		
		pnlImportStruct = new JPanel();
		GroupLayout lyoImportStructure = new GroupLayout(pnlImportStruct);
		JLabel lblImportStruct = new JLabel("Import a structure from");
		btnOpenMol = new JButton("File");
		btnOpenMol.setToolTipText("Imports a chemical system"
				+ " from file.");
		btnOpenMol.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File inFile = GUIFileOpener.pickFile(btnOpenMol);
				if (inFile == null || inFile.getAbsolutePath().equals(""))
				{
					return;
				}
				if (!inFile.getName().endsWith(".sdf"))
				{
					JOptionPane.showMessageDialog(null,
			                "<html>Expecting and MDL SDF file, but file<br>'"
							+ inFile.getAbsolutePath() + "' does not have .sdf"
							+ " extension.</html>",
			                "Error",
			                JOptionPane.ERROR_MESSAGE,
			                UIManager.getIcon("OptionPane.errorIcon"));
					return;
				}
				importStructureFromFile(inFile);
			}
		});
		
		// the '+' is to prevent search/replace of the string
        btnOpenSMILES = new JButton("SMI"+"LES"); 
        btnOpenSMILES.setToolTipText("<html>Imports chemical system"
                        + " from SMILES string.<br>The conversion of SMILES "
                        + "to 3D structure requires"
                        + "<br> an internet connection.</html>");
        btnOpenSMILES.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                	String smiles = JOptionPane.showInputDialog(
                			"Please input SMILES: ");
                	if (smiles != null && !smiles.trim().equals(""))
                	{
                	    importStructureFromSMILES(smiles);
                	}
                }
        });

        pnlImportStruct.setLayout(lyoImportStructure);
        lyoImportStructure.setAutoCreateGaps(true);
        lyoImportStructure.setAutoCreateContainerGaps(true);
        lyoImportStructure.setHorizontalGroup(lyoImportStructure.createParallelGroup(
                                        GroupLayout.Alignment.CENTER)
                        .addComponent(lblImportStruct)
                        .addGroup(lyoImportStructure.createSequentialGroup()
                                        .addComponent(btnOpenMol)
                                        .addComponent(btnOpenSMILES)));
        lyoImportStructure.setVerticalGroup(lyoImportStructure.createSequentialGroup()
        				.addComponent(lblImportStruct)
                        .addGroup(lyoImportStructure.createParallelGroup()
	                                .addComponent(btnOpenMol)
	                                .addComponent(btnOpenSMILES)));       
        ctrlPane.add(pnlImportStruct);

        ctrlPane.add(new JSeparator());
        
        pnlEmptFrag = new JPanel();
        btnEmptFrag = new JButton("Create Empty Vertex");
        btnEmptFrag.setToolTipText("<html>Creates an empty vertex:<br>a vertex "
                + "that contains no molecular structure.<html>");
        btnEmptFrag.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                GUIEmptyVertexMaker makeEmptyVertexDialog = 
                        new GUIEmptyVertexMaker();
                makeEmptyVertexDialog.pack();
                Object ev = makeEmptyVertexDialog.showDialog();
                if (ev == null)
                {
                    return;
                }
                ArrayList<DENOPTIMVertex> lst = new ArrayList<DENOPTIMVertex>(1);
                lst.add((EmptyVertex) ev);
                GUIVertexSelector fragSelector = new GUIVertexSelector(lst);
                fragSelector.btnDone.setText("Confirm");
                fragSelector.ctrlPane.setVisible(false);
                fragSelector.setRequireApSelection(false);
                Object selected = fragSelector.showDialog();
                if (selected == null)
                {
                    return;
                }
                importVertices(lst);
            }
        });
        pnlEmptFrag.add(btnEmptFrag);
        ctrlPane.add(pnlEmptFrag);
        
        ctrlPane.add(new JSeparator());
		
		pnlAtmToAP = new JPanel();
		btnAtmToAP = new JButton("Atom to AP");
		btnAtmToAP.setToolTipText("<html>Replaces the selected atoms with "
				+ "attachment points.<br>Click on atoms to select"
			    + " them. Click again to unselect.<br>"
			    + "<br><b>WARNING:</b> this action cannot be undone!<html>");
		btnAtmToAP.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				vertex = vertexViewer.getLoadedStructure();
				
				ArrayList<IAtom> selectedAtms = 
				        vertexViewer.getAtomsSelectedFromJMol();
				
				if (selectedAtms.size() == 0)
				{
					JOptionPane.showMessageDialog(null,
			                "<html>No atom selected! Click on atoms to select"
			                + " them.<br>Click again to unselect.</html>",
			                "Error",
			                JOptionPane.ERROR_MESSAGE,
			                UIManager.getIcon("OptionPane.errorIcon"));
					return;
				}
				else
				{
					//TODO: ask about hapticity:
					// if multihapto, then use all the selected atoms for 1 AP
					// and ask to select another set for other end of bond to 
					// break.
					
					String apClass;
					try {
						apClass = ensureGoodAPClassString("",false);
					} catch (Exception e1) {
						// We have pressed cancel or closed the dialog, so abandon
						return;
					}
					
					ArrayList<IAtom> failed = new ArrayList<IAtom>();
					for (IAtom atm : selectedAtms)
					{
						if (!convertAtomToAP(atm, apClass))
						{
							failed.add(atm);
						}
					}
					for (IAtom atm : failed)
					{
						selectedAtms.remove(atm);
					}
					if (selectedAtms.size() == 0)
					{
						return;
					}
					
					removeAtoms(selectedAtms);

					vertexViewer.loadVertexToViewer(vertex);
					
			        unsavedChanges = true;
			        protectEditedSystem();
				}
			}
		});
		pnlAtmToAP.add(btnAtmToAP);
		ctrlPane.add(pnlAtmToAP);
		
		pnlDelSel = new JPanel();
		btnDelSel = new JButton("Remove Atoms");
		btnDelSel.setToolTipText("<html>Removes all selected atoms from the "
				+ "system.<br><br><b>WARNING:</b> this action cannot be "
				+ "undone!");
		btnDelSel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ArrayList<IAtom> selectedAtms = 
				        vertexViewer.getAtomsSelectedFromJMol();
				
				if (selectedAtms.size() == 0)
				{
					JOptionPane.showMessageDialog(null,
							"<html>No atom selected! Click on atoms to select"
					        + " them.<br>Click again to unselect.</html>",
			                "Error",
			                JOptionPane.ERROR_MESSAGE,
			                UIManager.getIcon("OptionPane.errorIcon"));
					return;
				}
				else
				{					
					removeAtoms(selectedAtms);
					
					vertexViewer.loadVertexToViewer(vertex);
					
			        unsavedChanges = true;
			        protectEditedSystem();
				}
			}
		});
		pnlDelSel.add(btnDelSel);
		ctrlPane.add(pnlDelSel);
		
		ctrlPane.add(new JSeparator());
		
        pnlSaveEdits = new JPanel();
        btnSaveEdits = new JButton("Save Changes");
        //btnSaveEdits.setForeground(Color.RED);
        btnSaveEdits.setEnabled(true);
        btnSaveEdits.setToolTipText("<html>Save the current system replacing"
        		+ " <br>the original one in the loaded library.</html>");
        btnSaveEdits.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                	saveUnsavedChanges();
                }
        });
        pnlSaveEdits.add(btnSaveEdits);
        ctrlPane.add(pnlSaveEdits);
		this.add(ctrlPane,BorderLayout.EAST);
		
		
		// Panel with buttons to the bottom of the frame
		ButtonsBar commandsPane = new ButtonsBar();
		super.add(commandsPane, BorderLayout.SOUTH);
		
		btnOpenVrtxs = new JButton("Load Library of Building Blocks");
		btnOpenVrtxs.setToolTipText("Reads building blocks or structures from "
				+ "file.");
		btnOpenVrtxs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File inFile = GUIFileOpener.pickFile(btnOpenVrtxs);
				if (inFile == null || inFile.getAbsolutePath().equals(""))
				{
					return;
				}
				ArrayList<DENOPTIMVertex> vrtxLib = new ArrayList<>();
                try {
                    DenoptimIO.appendVerticesFromFileToLibrary(inFile,
                            BBType.FRAGMENT, vrtxLib, true);
                } catch (Exception e1) {
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(btnAddVrtx,
                            "<html>Could not read building blocks from file"
                            + "<br>'" + inFile + "'"
                            + "<br>Hint on cause: " + e1.getMessage() 
                            +"</html>",
                            "Error",
                            JOptionPane.ERROR_MESSAGE,
                            UIManager.getIcon("OptionPane.errorIcon"));
                    return;
                }

                if (vrtxLib.size() == 0)
                {
                    JOptionPane.showMessageDialog(btnAddVrtx,
                            "<html>No building blocks in file"
                            + "<br>'" + inFile + "'</html>",
                            "Error",
                            JOptionPane.ERROR_MESSAGE,
                            UIManager.getIcon("OptionPane.errorIcon"));
                    return;
                }
				importVertices(vrtxLib);
			}
		});
		commandsPane.add(btnOpenVrtxs);
		
		JButton btnSaveVrtxs = new JButton("Save Library of Building Blocks");
		btnSaveVrtxs.setToolTipText("Write all building blocks to a file.");
		btnSaveVrtxs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File outFile = GUIFileOpener.pickFileForSaving(
				        btnSaveVrtxs);
				if (outFile == null)
				{
					return;
				}
				//The format is given by the DenoptimIO.writeVertices method
				//TODO: consider using other formats, e.g., VRTXJSON.
				FileFormat ff = FileFormat.VRTXSDF;
				try
				{
				    DenoptimIO.writeVertices(outFile.getAbsolutePath(),
							verticesLibrary);
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
					JOptionPane.showMessageDialog(null,
			                "Could not write to '" + outFile + "'! "
			                + "Hint: "+ex.getMessage(),
			                "Error",
			                JOptionPane.PLAIN_MESSAGE,
			                UIManager.getIcon("OptionPane.errorIcon"));
					return;
				}
				navigSpinner.setModel(new SpinnerNumberModel(currVrtxIdx+1, 1, 
						verticesLibrary.size(), 1));
				deprotectEditedSystem();
				unsavedChanges = false;
				DenoptimIO.addToRecentFiles(outFile, ff);
			}
		});
		commandsPane.add(btnSaveVrtxs);

		JButton btnCanc = new JButton("Close Tab");
		btnCanc.setToolTipText("Closes this tab.");
		btnCanc.addActionListener(new removeCardActionListener(this));
		commandsPane.add(btnCanc);
		
		JButton btnHelp = new JButton("?");
		btnHelp.setToolTipText("<html>Hover over the buttons and fields "
                    + "to get a tip.</html>");
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String txt = "<html><body width='%1s'>"
						+ "<p>This tab allows to create, inspect, and edit "
						+ "building blocks and "
						+ "three-dimensional molecular fragments.</p>"
						+ "<p>New fragments can be created starting from any "
						+ "chemical structure that can be loaded from file or "
						+ "generated from SMILES (SMILES-to-3D conversion "
						+ "requires an Internet connection).</p>"
						+ "<p>Any terminal atom (i.e., atoms that have only "
						+ "one connected neighbor) can be transformed into "
						+ "on attachment point (AP). Click on the atom to "
						+ "select it, and press <code><b>Atom to AP</b></code>."
						+ "</p>"
						+ "<p>Attachment points are depicted in the molecular "
						+ "viewer as yellow arrows in the 3D space, and their "
						+ "attachment point class (APClass) is specified in "
						+ "the table below the viewer. Double-click on a "
						+ "specific APClass field to change its value.</p>"
						+ "<br>"
						+ "<p>Hover over buttons get a tip.</p>"
						+ "<br>"
						+ "<p>Right-click on the Jmol viewer will open the "
						+ "Jmol menu. However, since Jmol cannot handle the "
						+ "attachment points data. Therefore, Jmol "
						+ "functionality should only be used on systems "
						+ "that have no attachment points, or for alterations "
						+ "of the molecular structure that do not change the "
						+ "list of atoms au to the last atom decorated with an "
						+ "attachment point.</p></html>";
				JOptionPane.showMessageDialog(null, 
						String.format(txt, 400),
	                    "Tips",
	                    JOptionPane.PLAIN_MESSAGE);
			}
		});
		commandsPane.add(btnHelp);
	}

//-----------------------------------------------------------------------------
	
	public void importStructureFromFile(File file)
	{
		this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		// Cleanup
		clearCurrentSystem();
		
		try {			
			IAtomContainer mol = DenoptimIO.readSingleSDFFile(
					file.getAbsolutePath());
			
			// We mean to import only the structure: get rid of AP
			mol.setProperty(DENOPTIMConstants.APTAG,null);
			mol.setProperty(DENOPTIMConstants.APCVTAG,null);
			
			// NB: here we let the vertexViewer create a fragment object that we
			// then put into the local library. This to make sure that the 
			// references to atoms selected in the viewer are referring to
			// members of the "vertex" object
			vertexViewer.loadPlainStructure(mol);
			vertex = vertexViewer.getLoadedStructure();

			// the system is not a fragment but, this is done for consistency:
			// when we have a molecule loaded the list is not empty
			// The currently viewed fragment (if any) is always part of the lib
			verticesLibrary.add(vertex);
			currVrtxIdx = verticesLibrary.size()-1;

			updateVrtxListSpinner();
			unsavedChanges = true;
	        btnDelSel.setEnabled(true);
	        btnAtmToAP.setEnabled(true);
		} catch (Exception e) {
			this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			e.printStackTrace();
			JOptionPane.showMessageDialog(null,
	                "<html>Could not read file '" + file.getAbsolutePath() 
	                + "'!<br>Hint about reason: " + e.getCause() + "</html>",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
		}
		
		this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

//-----------------------------------------------------------------------------

	/**
	 * Imports the given SMILES into the viewer. As SMILES cannot hold 
	 * attachment points (APs), no AP will be created and the resulting system 
	 * is a plain molecule instead of a fragment.
	 * @param smiles the SMILES string
	 */
	public void importStructureFromSMILES(String smiles)
	{	
		this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		// Cleanup
		clearCurrentSystem();
		
		// Load the structure using CACTUS service or CDK builder
		try {
		    vertexViewer.loadSMILES(smiles);
		} catch (Exception e) {
			this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			return;
		}
		
		vertex = vertexViewer.getLoadedStructure();
		
		// The system is not a fragment but, this is done for consistency:
		// when we have a molecule loaded the list is not empty:
		// The currently viewed fragment (if any) is always part of the library
	    verticesLibrary.add(vertex);
		currVrtxIdx = verticesLibrary.size()-1;
		
		// finalize GUI status
		updateVrtxListSpinner();
		unsavedChanges = true;
		btnDelSel.setEnabled(true);
        btnAtmToAP.setEnabled(true);
        btnSaveEdits.setEnabled(true);
		
		this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Imports fragments from a file.
	 * @param file the file to open
	 * @param format the format
	 */
	public void importVerticesFromFile(File file, String format)
	{	
		this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		ArrayList<DENOPTIMVertex> vrtxLib = new ArrayList<>();
        try {
            DenoptimIO.appendVerticesFromFileToLibrary(file, BBType.FRAGMENT,
                    vrtxLib, true);
        } catch (Exception e1) {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(btnAddVrtx,
                    "<html>Could not read building blocks from file"
                    + "<br>'" + file + "'"
                    + "<br>Hint on cause: " + e1.getMessage() 
                    +"</html>",
                    "Error",
                    JOptionPane.ERROR_MESSAGE,
                    UIManager.getIcon("OptionPane.errorIcon"));
            return;
        }

        if (vrtxLib.size() == 0)
        {
            JOptionPane.showMessageDialog(btnAddVrtx,
                    "<html>No building blocks in file"
                    + "<br>'" + file + "'</html>",
                    "Error",
                    JOptionPane.ERROR_MESSAGE,
                    UIManager.getIcon("OptionPane.errorIcon"));
            return;
        }
        importVertices(vrtxLib);
		this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
	
//-----------------------------------------------------------------------------
    
    /**
     * Imports vertices.
     * @param list the list of vertices to import
     */
    public void importVertices(ArrayList<DENOPTIMVertex> list)
    {   
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        
        int firstOfNew = 0;
        boolean libFromScrtch = false;
        if (verticesLibrary == null)
        {
            libFromScrtch = true;
            verticesLibrary = new ArrayList<DENOPTIMVertex>();
        }
        else
        {
            firstOfNew = verticesLibrary.size();
        }
        
        boolean addedOne = false;
        if (list.size() > 0)
        {
            verticesLibrary.addAll(list);
            addedOne = true;
            
            // Display the first
            if (libFromScrtch)
            {
                currVrtxIdx = 0;
            }
            else if (addedOne)
            {
                currVrtxIdx = firstOfNew;
            }
            loadCurrentVrtxIdxToViewer();
            
            // Update the fragment spinner
            updateVrtxListSpinner();
        } else {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            JOptionPane.showMessageDialog(null,
                    "<html>No vertices to import from the given list.</html>",
                    "Error",
                    JOptionPane.PLAIN_MESSAGE,
                    UIManager.getIcon("OptionPane.errorIcon"));
        }
        this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }
    
	
//-----------------------------------------------------------------------------
	
	/**
	 * Loads the fragments corresponding to the field index.
	 * The molecular data is loaded in the Jmol viewer,
	 * and the attachment point (AP) information in the the list of APs.
	 * Jmol is not aware of AP-related information, so this also launches
	 * the generation of the graphical objects representing the APs.
	 */
	private void loadCurrentVrtxIdxToViewer()
	{
		if (verticesLibrary == null)
		{
			JOptionPane.showMessageDialog(null,
	                "No list of building blocks loaded.",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;
		}
		
		clearCurrentSystem();

		vertex = verticesLibrary.get(currVrtxIdx);
		vertexViewer.loadVertexToViewer(vertex);
		if (vertex == null || vertex instanceof DENOPTIMFragment == false)
		{
	        btnDelSel.setEnabled(false);
	        btnAtmToAP.setEnabled(false);
		} else {
	        btnDelSel.setEnabled(true);
	        btnAtmToAP.setEnabled(true);
		}
	}
	
//-----------------------------------------------------------------------------
	
	private void clearCurrentSystem()
	{
		// Get rid of currently loaded mol
		vertex = null;
		vertexViewer.clearCurrentSystem();
	}

//-----------------------------------------------------------------------------

	private void updateVrtxListSpinner()
	{		
		navigSpinner.setModel(new SpinnerNumberModel(currVrtxIdx+1, 1, 
				verticesLibrary.size(), 1));
		totalVrtxsLabel.setText(Integer.toString(verticesLibrary.size()));
	}
    
//-----------------------------------------------------------------------------
    
    /**
     * Removes an atom and replaces it with an attachment point.
     * @param apClass the attachment point class of the new AP
     * @param trgAtm
     * @return <code>true</code> if the conversion was successful
     */
    private boolean convertAtomToAP(IAtom trgAtm, String apClass)
    {
        if (!(vertex instanceof DENOPTIMFragment))
        {
            return false;
        }
        DENOPTIMFragment frag = (DENOPTIMFragment) vertex;
    	// Accept ONLY if the atom has one and only one connected neighbour
    	if (frag.getConnectedAtomsCount(trgAtm) != 1)
    	{
    		String str = "";
    		for (IAtom atm : frag.getConnectedAtomsList(trgAtm))
    		{
    			str = str + " " + atm.getSymbol() 
    	                + (frag.getAtomNumber(atm));
    		}
    		System.out.println("Connected atoms: "+str);
    		
			JOptionPane.showMessageDialog(null,
	                "<html>Atom "+ trgAtm.getSymbol() 
	                + (frag.getAtomNumber(trgAtm)) 
	                + " has zero or more than one neighbour.<br>I can only "
	                + "transform atoms"
	                + " that have one and only one neighbour.</html>",
	                "Error",
	                JOptionPane.ERROR_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
    		return false;
    	}
    	
    	IAtom srcAtm = frag.getConnectedAtomsList(trgAtm).get(0);
    	
    	Point3d srcP3d = DENOPTIMMoleculeUtils.getPoint3d(srcAtm);
    	Point3d trgP3d = DENOPTIMMoleculeUtils.getPoint3d(trgAtm);
    	Point3d vector = new Point3d();
    	vector.x = srcP3d.x + (trgP3d.x - srcP3d.x);
    	vector.y = srcP3d.y + (trgP3d.y - srcP3d.y);
    	vector.z = srcP3d.z + (trgP3d.z - srcP3d.z);
    	try {
    	    frag.addAPOnAtom(srcAtm, APClass.make(apClass), vector);
		} catch (DENOPTIMException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null,
	                "<html>Could not make AP.<br>Possible cause: " 
	                + e.getMessage() +"</html>",
	                "Error",
	                JOptionPane.ERROR_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
    		return false;
		}   	
    	return true;
    }
    
//----------------------------------------------------------------------------

    private void removeAtoms(ArrayList<IAtom> atmsToDels)
    {
        if (!(vertex instanceof DENOPTIMFragment))
        {
            return;
        }
        DENOPTIMFragment frag = (DENOPTIMFragment) vertex;
    	ArrayList<IBond> bnsToDel = new ArrayList<IBond>();
    	for (IAtom atm : atmsToDels)
    	{
	    	for (IBond bnd : frag.bonds())
	    	{
	    		if (bnd.contains(atm))
	    		{
	    			bnsToDel.add(bnd);
	    		}
	    	}
    	}
    	for (IBond bnd : bnsToDel)
    	{
    	    frag.removeBond(bnd);
    	}
    	for (IAtom atm : atmsToDels)
    	{
    	    frag.removeAtomAndConnectedElectronContainers(atm);
    	}
    	frag.updateAPs();
    }
	
//-----------------------------------------------------------------------------
	
	private class VrtxSpinnerChangeEvent implements ChangeListener
	{
		private boolean inEnabled = true;
		
		public VrtxSpinnerChangeEvent()
		{}
		
		/**
		 * Enables/disable the listener
		 * @param var <code>true</code> to activate listener, 
		 * <code>false</code> to disable.
		 */
		public void setEnabled(boolean var)
		{
			this.inEnabled = var;
		}
		
        @Override
        public void stateChanged(ChangeEvent event)
        {
        	if (!inEnabled)
        	{
        		return;
        	}
      
        	activateTabEditsListener(false);
        	
        	//NB here we convert from 1-based index in GUI to 0-based index
        	currVrtxIdx = ((Integer) navigSpinner.getValue()).intValue() - 1;
        	loadCurrentVrtxIdxToViewer();
        	
        	activateTabEditsListener(true);
        }
	}
	
//-----------------------------------------------------------------------------

	private void deprotectEditedSystem()
	{
		//btnSaveEdits.setEnabled(false);
		btnAddVrtx.setEnabled(true);
		btnOpenVrtxs.setEnabled(true);
		btnOpenSMILES.setEnabled(true); 
		btnOpenMol.setEnabled(true);
        if (vertex == null || vertex instanceof DENOPTIMFragment == false)
        {
            btnDelSel.setEnabled(false);
            btnAtmToAP.setEnabled(false);
        } else {
            btnDelSel.setEnabled(true);
            btnAtmToAP.setEnabled(true);
        }
		
		((DefaultEditor) navigSpinner.getEditor())
			.getTextField().setEditable(true); 
		((DefaultEditor) navigSpinner.getEditor())
			.getTextField().setForeground(Color.BLACK);
		vertexViewer.deprotectEdits();
		vertexViewer.setSwitchable(true);
		
		vrtxSpinnerListener.setEnabled(true);
	}
	
//-----------------------------------------------------------------------------
	
	private void protectEditedSystem()
	{
		btnSaveEdits.setEnabled(true);
		btnAddVrtx.setEnabled(false);
		btnOpenVrtxs.setEnabled(false);
		btnOpenSMILES.setEnabled(false); 
		btnOpenMol.setEnabled(false);
		btnDelSel.setEnabled(false);
		btnAtmToAP.setEnabled(false);
		
		navigSpinner.setModel(new SpinnerNumberModel(currVrtxIdx+1, 
				currVrtxIdx+1, currVrtxIdx+1, 1));
		((DefaultEditor) navigSpinner.getEditor())
			.getTextField().setEditable(false); 
		((DefaultEditor) navigSpinner.getEditor())
			.getTextField().setForeground(Color.GRAY);
		
		vertexViewer.setSwitchable(false);
		
		vrtxSpinnerListener.setEnabled(false);
	}
	
//-----------------------------------------------------------------------------

    private void activateTabEditsListener(boolean var)
    {
        vertexViewer.activateTabEditsListener(var);
    }
    
//-----------------------------------------------------------------------------
    
    private void removeCurrentVrtx() throws DENOPTIMException
    {
    	if (vertexViewer.hasUnsavedAPEdits())
    	{
			String[] options = new String[]{"Yes","No"};
			int res = JOptionPane.showOptionDialog(null,
	                "<html>Removing unsaved vertex?",
	                "Warning",
	                JOptionPane.DEFAULT_OPTION,
	                JOptionPane.QUESTION_MESSAGE,
	                UIManager.getIcon("OptionPane.warningIcon"),
	                options,
	                options[1]);
			if (res == 1)
			{
				return;
			}
    	}

    	clearCurrentSystem();
    	
    	// Actual removal from the library
    	if (verticesLibrary.size()>0)
    	{
    		verticesLibrary.remove(currVrtxIdx);
    		int libSize = verticesLibrary.size();
    		
    		if (currVrtxIdx>=0 && currVrtxIdx<libSize)
    		{
    			//we keep currFrgIdx as it will correspond to the next item
    		}
    		else
    		{
    			currVrtxIdx = currVrtxIdx-1;
    		}
    		
    		if (currVrtxIdx==-1 || verticesLibrary.size()==0)
			{
    		    vertexViewer.clearMolecularViewer();
				currVrtxIdx = 0;
				navigSpinner.setModel(new SpinnerNumberModel(0,0,0,1));
				totalVrtxsLabel.setText(Integer.toString(0));
				deprotectEditedSystem();
    		}
    		else
    		{
		    	loadCurrentVrtxIdxToViewer();
		    	updateVrtxListSpinner();
		    	navigSpinner.setModel(new SpinnerNumberModel(currVrtxIdx+1, 1, 
						verticesLibrary.size(), 1));
		        deprotectEditedSystem();
    		}
    	}
    }

//-----------------------------------------------------------------------------

  	private void saveUnsavedChanges() 
  	{
  		this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
  		
  		if (vertexViewer.hasUnsavedAPEdits())
  		{
  		    DefaultTableModel tabModel = vertexViewer.getAPTableModel();
	  		// Import changes from AP table into molecular representation
	        for (int i=0; i<tabModel.getRowCount(); i++) 
	        {	        	
	        	int apId = ((Integer) tabModel.getValueAt(i, 0)).intValue();
	        	String currApClass = tabModel.getValueAt(i, 1).toString();
	        	
	        	// Make sure the new class has a proper syntax
	        	try {
					currApClass = ensureGoodAPClassString(currApClass,true);
				} catch (DENOPTIMException e1) {
					currApClass = "dafaultAPClass:0";
				}
	        	
	        	Map<Integer, DENOPTIMAttachmentPoint> mapAPs = 
	        	        vertexViewer.getActiveMapAPs();
	        	
	        	if (mapAPs.containsKey(apId))
	        	{
	        		String origApClass = mapAPs.get(apId).getAPClass().toString();
	        		if (!origApClass.equals(currApClass))
	        		{
	        			try {
	        			    mapAPs.get(apId).setAPClass(currApClass);
						} catch (DENOPTIMException e) {
							// We made sure the class is valid, so this
							// should never happen, though one never knows
							e.printStackTrace();
							JOptionPane.showMessageDialog(null,
			    	                "<html>Could not save due to errors setting a "
			    	                + "new APClass.<br>Please report this to the "
			    	                + "DENOPTIM team.</html>",
			    	                "Error",
			    	                JOptionPane.PLAIN_MESSAGE,
			    	                UIManager.getIcon("OptionPane.errorIcon"));
			    			return;	
						}
	        		}
	        	}
	        	else
	        	{
	    			JOptionPane.showMessageDialog(null,
	    	                "<html>Could not save due to mistmatch between AP "
	    	                + "table and map.<br>Please report this to the "
	    	                + "DENOPTIM team.</html>",
	    	                "Error",
	    	                JOptionPane.PLAIN_MESSAGE,
	    	                UIManager.getIcon("OptionPane.errorIcon"));
	    			return;	
	        	}
	        }
	  	}
  		
  		// Retrieve chemical object from the viewer, if edited, otherwise
  		// we get what is already in 'vertex'
  		vertex = vertexViewer.getLoadedStructure();
  		if (verticesLibrary.size()==0
  				&& (vertex==null || vertex.getNumberOfAPs()==0))
  		{
  			//Nothing to same
  	        this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  			return;
  		}
  		verticesLibrary.set(currVrtxIdx,vertex);
        
        // Reload fragment from library to refresh table and viewer
    	activateTabEditsListener(false);
    	try {
    	    loadCurrentVrtxIdxToViewer();
    	} catch (Throwable t) {
			//This can happen if the viewer has been started but is empty
    		// E.G:, if the cactvs server is down).
    		// We just keep going, and make sure we get the default cursor back.
		}
  		// Release constraints
    	activateTabEditsListener(true);
    	navigSpinner.setModel(new SpinnerNumberModel(currVrtxIdx+1, 1, 
				verticesLibrary.size(), 1));
        deprotectEditedSystem();
  		
        this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  	}

//----------------------------------------------------------------------------
  	
  	/**
  	 * Forces the user to specify a properly formatted APClass.
  	 * @param currApClass the current value of the APClass, or empty string
  	 * @param mustReply set to <code>true</code> to prevent escaping the question
  	 * @return 
  	 * @throws DENOPTIMException 
  	 */
	public static String ensureGoodAPClassString(String currApClass, 
			boolean mustReply) 
			throws DENOPTIMException 
	{		
		return ensureGoodAPClassString(currApClass,"Define APClass",mustReply);
	}
  	
//-----------------------------------------------------------------------------
  	
  	/**
  	 * Forces the user to specify a properly formatted APClass.
  	 * @param currApClass the current value of the APClass, or empty string
  	 * @param mustReply set to <code>true</code> to prevent escaping the 
  	 * question
  	 * @return the APClass
  	 * @throws DENOPTIMException 
  	 */
	public static String ensureGoodAPClassString(String currApClass, 
			String title, boolean mustReply) throws DENOPTIMException 
	{		
		String preStr = "";
		while (!APClass.isValidAPClassString(currApClass))
    	{
			if (currApClass != "")
			{
	    		preStr = "APClass '" + currApClass + "' is not valid!<br>"
	    				+ "The valid syntax for APClass is:<br><br><code>rule" 
	        			+ DENOPTIMConstants.SEPARATORAPPROPSCL 
	    				+ "subClass</code><br><br> where "
	    				+ "<ul><li><code>rule</code>"
	    				+ " is a string (no spaces)</li>"
	    				+ "<li><code>subClass</code> is an integer</li>";
			}
			
    		currApClass = JOptionPane.showInputDialog(null, 
    				"<html>" + preStr + "</ul>Please, provide a valid "
    				+ "APClass string: ", title, JOptionPane.PLAIN_MESSAGE);
        	
    		if (currApClass == null)
        	{
        		currApClass = "";
        		if (!mustReply)
        		{
        			throw new DENOPTIMException();
        		}
        	}
        	
    		preStr = "APClass '" + currApClass + "' is not valid!<br>"
    				+ "The valid syntax for APClass is:<br><br><code>rule" 
        			+ DENOPTIMConstants.SEPARATORAPPROPSCL 
    				+ "subClass</code><br><br> where "
    				+ "<ul><li><code>rule</code>"
    				+ " is a string (no spaces)</li>"
    				+ "<li><code>subClass</code> is an integer</li>";
    	}
    	
    	return currApClass;
	}
	
//-----------------------------------------------------------------------------
  	
  	/**
  	 * Forces the user to specify a properly formatted APRule, i.e., the 
  	 * first component of an APClass.
  	 * @param currApRule the current value of the APRule, or empty string
  	 * @param mustReply set to <code>true</code> to prevent escaping the question
  	 * @return 
  	 * @throws DENOPTIMException is the used did not choose a valid value.
  	 */
	public static String ensureGoodAPRuleString(String currApRule, 
			String title, boolean mustReply) throws DENOPTIMException 
	{		
		String preStr = "";
		while (!APClass.isValidAPRuleString(currApRule))
    	{
			if (currApRule != "")
			{
	    		preStr = "APClass rule '" + currApRule + "' is not valid!<br>"
	    				+ "The valid APClass rile is a string with no spaces "
	    				+ "and no '"
	        			+ DENOPTIMConstants.SEPARATORAPPROPSCL 
	    				+ "' character.<br>";
			}
			
    		currApRule = JOptionPane.showInputDialog(null, 
    				"<html>" + preStr + " Please, provide a valid "
    				+ "APClass rule string: ",
    				title,
    				JOptionPane.PLAIN_MESSAGE);
        	
    		if (currApRule == null)
        	{
        		currApRule = "";
        		if (!mustReply)
        		{
        			throw new DENOPTIMException();
        		}
        	}
    	}
    	
    	return currApRule;
	}
  	
//-----------------------------------------------------------------------------

	/**
	 * Check whether there are unsaved changes.
	 * @return <code>true</code> if there are unsaved changes.
	 */
	
	public boolean hasUnsavedChanges()
	{
		return unsavedChanges;
	}
	
//-----------------------------------------------------------------------------

	/*
	 * This is needed to stop Jmol threads upon closure of this gui card.
	 */
	public void dispose() 
	{
	    vertexViewer.dispose();
	}
		
//-----------------------------------------------------------------------------
  	
}
