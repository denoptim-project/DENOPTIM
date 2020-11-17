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
import javax.vecmath.Point3d;

import denoptim.utils.DENOPTIMMoleculeUtils;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.molecule.APClass;
import denoptim.molecule.DENOPTIMFragment;
import denoptim.molecule.DENOPTIMFragment.BBType;


/**
 * A panel with a molecular viewer that understands DENOPTIM fragments
 * and allows to create and edit fragments.
 * The molecular viewer is provided by Jmol.
 * 
 * @author Marco Foscato
 */

public class GUIFragmentInspector extends GUICardPanel
{
	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 912850110991449553L;
	
	/**
	 * Unique identified for instances of this inspector
	 */
	public static AtomicInteger prepFragTabUID = new AtomicInteger(1);
	
	/**
	 * The currently loaded list of fragments
	 */
	private ArrayList<DENOPTIMFragment> fragmentLibrary =
			new ArrayList<DENOPTIMFragment>();
	
	/**
	 * The currently loaded fragment
	 */
	private DENOPTIMFragment fragment;
	
	/**
	 * The index of the currently loaded fragment [0–(n-1)}
	 */
	private int currFrgIdx = 0;
	
	/**
	 * Flag signaling that loaded data has changes since last save
	 */
	private boolean unsavedChanges = false;
	
	private FragmentViewPanel fragmentViewer;
	private JPanel fragCtrlPane;
	private JPanel fragNavigPanel;
	private JPanel fragNavigPanel2;
	private JPanel fragNavigPanel3;
	
	private JButton btnAddFrag;
	private JButton btnDelFrag;
	
	private JButton btnOpenFrags;
	
	private JSpinner fragNavigSpinner;
	private JLabel totalFragsLabel;
	private final FragSpinnerChangeEvent fragSpinnerListener = 
			new FragSpinnerChangeEvent();
	
	private JPanel pnlImportStruct;
	private JButton btnOpenMol;
	private JButton btnOpenSMILES;
	
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
	public GUIFragmentInspector(GUIMainPanel mainPanel)
	{
		super(mainPanel, "Fragment Inspector #" + prepFragTabUID.getAndIncrement());
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
		// - (Center) molecular viewer and APs
		// - (East) Fragment controls
		// - (South) general controls (load, save, close)
		
		// The viewer with Jmol and APtable
		fragmentViewer = new FragmentViewPanel(true);
		fragmentViewer.addPropertyChangeListener("APDATA", 
				new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				protectEditedSystem();				
			}
		});
        this.add(fragmentViewer,BorderLayout.CENTER);
		
		// General panel on the right: it containing all controls
        fragCtrlPane = new JPanel();
        fragCtrlPane.setVisible(true);
        fragCtrlPane.setLayout(new BoxLayout(fragCtrlPane, SwingConstants.VERTICAL));
        fragCtrlPane.add(new JSeparator());
		
        // NB: avoid GroupLayout because it interferes with Jmol viewer and causes exception
        
        // Controls to navigate the list of fragments
        fragNavigPanel = new JPanel();
        fragNavigPanel2 = new JPanel();
        fragNavigPanel3 = new JPanel();
        JLabel navigationLabel1 = new JLabel("Fragment # ");
        JLabel navigationLabel2 = new JLabel("Current library size: ");
        totalFragsLabel = new JLabel("0");
        
		fragNavigSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 0, 1));
		fragNavigSpinner.setToolTipText("Move to fragment number # in the currently loaded library.");
		fragNavigSpinner.setPreferredSize(new Dimension(75,20));
		fragNavigSpinner.addChangeListener(fragSpinnerListener);
        fragNavigPanel.add(navigationLabel1);
		fragNavigPanel.add(fragNavigSpinner);
		fragCtrlPane.add(fragNavigPanel);
		
        fragNavigPanel2.add(navigationLabel2);
        fragNavigPanel2.add(totalFragsLabel);
		fragCtrlPane.add(fragNavigPanel2);
		
		btnAddFrag = new JButton("Add");
		btnAddFrag.setToolTipText("Append fragment taken from file.");
		btnAddFrag.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File inFile = DenoptimGUIFileOpener.pickFile();
				if (inFile == null || inFile.getAbsolutePath().equals(""))
				{
					return;
				}
				
				ArrayList<IAtomContainer> fragLib;
				try {
					fragLib = DenoptimIO.readMoleculeData(
							inFile.getAbsolutePath());
				} catch (Exception e1) {
					JOptionPane.showMessageDialog(null,
			                "<html>Could not read fragments from file"
			                + "<br>'" + inFile + "'"
			                + "<br>Hint on cause: " + e1.getMessage() 
			                +"</html>",
			                "Error",
			                JOptionPane.ERROR_MESSAGE,
			                UIManager.getIcon("OptionPane.errorIcon"));
					return;
				}

				if (fragLib.size() == 0)
				{
					JOptionPane.showMessageDialog(null,
			                "<html>No fragments in file"
			                + "<br>'" + inFile + "'</html>",
			                "Error",
			                JOptionPane.ERROR_MESSAGE,
			                UIManager.getIcon("OptionPane.errorIcon"));
					return;
				}
				
				if (fragLib.size() == 1)
				{
					importFragmentsFromFile(inFile);
					return;
				}
				
				String[] options = new String[]{"All", 
						"Selected",
						"Cancel"};
				String txt = "<html><body width='%1s'>Do you want to "
						+ "append all fragments of only selected ones?"
						+ "</html>";
				int res = JOptionPane.showOptionDialog(null,
		                String.format(txt,200),
		                "Append Fragments",
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
						importFragmentsFromFile(inFile);
						break;
						
					case 1:						
						ArrayList<IAtomContainer> selectedFrags = 
								new ArrayList<IAtomContainer>();
						int iFrg = -1;
						while (true)
						{
							if (iFrg+1>=fragLib.size())
							{
								break;
							}
							GUIFragmentSelector fragSelector = 
									new GUIFragmentSelector(fragLib,iFrg+1);
							fragSelector.setRequireApSelection(false);
							Object selected = fragSelector.showDialog();

							if (selected != null)
							{
								iFrg = ((Integer[]) selected)[0];
								selectedFrags.add(fragLib.get(iFrg));
							}
							else
							{
								break;
							}
						}
						String tmpSDFFile = Utils.getTempFile(
								"Denoptim_FragViewer_loadedMol.sdf");
						try {
							DenoptimIO.writeMoleculeSet(tmpSDFFile, selectedFrags);
							importFragmentsFromFile(new File(tmpSDFFile));
						} catch (DENOPTIMException e1) {
							JOptionPane.showMessageDialog(null,
					                "<html>Could not read import fragments.<br>"
					                + "Error reading tmp file"
					                + "<br>'" + inFile + "'"
					                + "<br>Hint on cause: " + e1.getMessage() 
					                +"</html>",
					                "Error",
					                JOptionPane.ERROR_MESSAGE,
					                UIManager.getIcon("OptionPane.errorIcon"));
							return;
						}
						break;
					
					default:
						return;
				}
			}
		});
		btnDelFrag = new JButton("Remove");
		btnDelFrag.setToolTipText("Remove the present fragment from the library.");
		btnDelFrag.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					removeCurrentFragment();
				} catch (DENOPTIMException e1) {
					System.out.println("Esception while removing the current fragment:");
					e1.printStackTrace();
				}
			}
		});
		fragNavigPanel3.add(btnAddFrag);
		fragNavigPanel3.add(btnDelFrag);
		fragCtrlPane.add(fragNavigPanel3);
		
		fragCtrlPane.add(new JSeparator());
		
		pnlImportStruct = new JPanel();
		GroupLayout lyoImportStructure = new GroupLayout(pnlImportStruct);
		JLabel lblImportStruct = new JLabel("Import a structure from");
		btnOpenMol = new JButton("File");
		btnOpenMol.setToolTipText("Imports a chemical system"
				+ " from file.");
		btnOpenMol.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File inFile = DenoptimGUIFileOpener.pickFile();
				if (inFile == null || inFile.getAbsolutePath().equals(""))
				{
					return;
				}
				importStructureFromFile(inFile);
			}
		});
		
        btnOpenSMILES = new JButton("SMILES");
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
        fragCtrlPane.add(pnlImportStruct);
        
		fragCtrlPane.add(new JSeparator());
		
		pnlAtmToAP = new JPanel();
		btnAtmToAP = new JButton("Atom to AP");
		btnAtmToAP.setToolTipText("<html>Replaces the selected atoms with "
				+ "attachment points.<br>Click on atoms to select"
			    + " them. Click again to unselect.<br>"
			    + "<br><b>WARNING:</b> this action cannot be undone!<html>");
		btnAtmToAP.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ArrayList<IAtom> selectedAtms = 
						fragmentViewer.getAtomsSelectedFromJMol();
				
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
					
					// Use the APs stored in the atoms
					fragment.updateAPs();
					
					fragmentViewer.loadFragImentToViewer(fragment);
					
			        // Protect the temporary "fragment" obj
			        unsavedChanges = true;
			        protectEditedSystem();
				}
			}
		});
		pnlAtmToAP.add(btnAtmToAP);
		fragCtrlPane.add(pnlAtmToAP);
		
		pnlDelSel = new JPanel();
		btnDelSel = new JButton("Remove Atoms");
		btnDelSel.setToolTipText("<html>Removes all selected atoms from the "
				+ "system.<br><br><b>WARNING:</b> this action cannot be "
				+ "undone!");
		btnDelSel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ArrayList<IAtom> selectedAtms = 
						fragmentViewer.getAtomsSelectedFromJMol();
				
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
					
					fragment.updateAPs();
					
					fragmentViewer.loadFragImentToViewer(fragment);
					
			        // Protect the temporary "fragment" obj
			        unsavedChanges = true;
			        protectEditedSystem();
				}
			}
		});
		pnlDelSel.add(btnDelSel);
		fragCtrlPane.add(pnlDelSel);
		
		fragCtrlPane.add(new JSeparator());
		
        pnlSaveEdits = new JPanel();
        btnSaveEdits = new JButton("Save Changes");
        btnSaveEdits.setForeground(Color.RED);
        btnSaveEdits.setEnabled(false);
        btnSaveEdits.setToolTipText("<html>Save the current fragment replacing"
        		+ " <br>the original fragment in the loaded library.</html>");
        btnSaveEdits.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                	saveUnsavedChanges();
                }
        });
        pnlSaveEdits.add(btnSaveEdits);
        fragCtrlPane.add(pnlSaveEdits);
		
		this.add(fragCtrlPane,BorderLayout.EAST);
		
		
		// Panel with buttons to the bottom of the frame
		
		JPanel commandsPane = new JPanel();
		super.add(commandsPane, BorderLayout.SOUTH);
		
		btnOpenFrags = new JButton("Load Library of Fragments",
					UIManager.getIcon("FileView.directoryIcon"));
		btnOpenFrags.setToolTipText("Reads fragments or structures from "
				+ "file.");
		btnOpenFrags.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File inFile = DenoptimGUIFileOpener.pickFile();
				if (inFile == null || inFile.getAbsolutePath().equals(""))
				{
					return;
				}
				importFragmentsFromFile(inFile);
			}
		});
		commandsPane.add(btnOpenFrags);
		
		JButton btnSaveFrags = new JButton("Save Library of Fragments",
				UIManager.getIcon("FileView.hardDriveIcon"));
		btnSaveFrags.setToolTipText("Write all fragments to a file.");
		btnSaveFrags.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File outFile = DenoptimGUIFileOpener.saveFile();
				if (outFile == null)
				{
					return;
				}
				try
				{
					for (DENOPTIMFragment f : fragmentLibrary)
					{
						f.projectAPsToProperties();
					}
				    DenoptimIO.writeFragmentSet(outFile.getAbsolutePath(),
				    		fragmentLibrary);
				}
				catch (Exception ex)
				{
					JOptionPane.showMessageDialog(null,
			                "Could not write to '" + outFile + "'!.",
			                "Error",
			                JOptionPane.PLAIN_MESSAGE,
			                UIManager.getIcon("OptionPane.errorIcon"));
					return;
				}
				fragNavigSpinner.setModel(new SpinnerNumberModel(currFrgIdx+1, 1, 
						fragmentLibrary.size(), 1));
				deprotectEditedSystem();
				unsavedChanges = false;
			}
		});
		commandsPane.add(btnSaveFrags);

		JButton btnCanc = new JButton("Close Tab");
		btnCanc.setToolTipText("Closes this fragment inspector tab.");
		btnCanc.addActionListener(new removeCardActionListener(this));
		commandsPane.add(btnCanc);
		
		JButton btnHelp = new JButton("?");
		btnHelp.setToolTipText("<html>Hover over the buttons and fields "
                    + "to get a tip.</html>");
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String txt = "<html><body width='%1s'>"
						+ "<p>This tab allows to create, inspect, and edit"
						+ " three-dimensional molecular fragments.</p>"
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
						+ "Jmol menu. However, Jmol cannot handle the "
						+ "attachment points data. Therefore, Jmol "
						+ "functionality should only be used on systems "
						+ "that have no attachment points.</p></html>";
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
			
			fragment = new DENOPTIMFragment(mol,BBType.UNDEFINED);

			// the system is not a fragment but, this is done for consistency:
			// when we have a molecule loaded the list is not empty
			// The currently viewed fragment (if any) is always part of the lib
			fragmentLibrary.add(fragment); 
			currFrgIdx = fragmentLibrary.size()-1;
			
			loadCurrentAsPlainStructure();
			updateFragListSpinner();
			unsavedChanges = true;
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
		
		// Load the structure using CACTUS service via Jmol
		try {
			fragmentViewer.loadSMILESFromRemote(smiles);
		} catch (Exception e) {
			this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			return;
		}
		
		fragment = fragmentViewer.getLoadedStructure();
		
		// The system is not a fragment but, this is done for consistency:
		// when we have a molecule loaded the list is not empty:
		// The currently viewed fragment (if any) is always part of the library
	    fragmentLibrary.add(fragment);
		currFrgIdx = fragmentLibrary.size()-1;
		
		// finalize GUI status
		updateFragListSpinner();
		unsavedChanges = true;
		
		this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Imports fragments from a SDF file. This method expects to find fragments
	 * with DENOPTIM's format, i.e., with the 
	 * <code>ATTACHMENT_POINT</code> and possibly the
	 * <code>CLASS</code> tags.
	 * @param file the file to open
	 */
	public void importFragmentsFromFile(File file)
	{	
		importFragmentsFromFile(file,"SDF");
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Imports fragments from a SDF file. This method expects to find fragments
	 * with DENOPTIM's format, i.e., with the 
	 * <code>ATTACHMENT_POINT</code> and possibly the
	 * <code>CLASS</code> tags.
	 * @param file the file to open
	 * @param format the format
	 */
	public void importFragmentsFromFile(File file, String format)
	{	
		this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		int firstOfNew = 0;
		boolean libFromScrtch = false;
		if (fragmentLibrary == null)
		{
			libFromScrtch = true;
			fragmentLibrary = new ArrayList<DENOPTIMFragment>();
		}
		else
		{
			firstOfNew = fragmentLibrary.size();
		}
		
		try 
		{
			// Import library of fragments
			boolean addedOne = false;
			for (IAtomContainer iac : DenoptimIO.readMoleculeData(
												file.getAbsolutePath(),format))
			{
			    fragmentLibrary.add(new DENOPTIMFragment(iac,BBType.UNDEFINED));
			    addedOne = true;
			}
			
			// Display the first
			if (libFromScrtch)
			{
				currFrgIdx = 0;
			}
			else if (addedOne)
			{
				currFrgIdx = firstOfNew;
			}
			loadCurrentFragIdxToViewer();
			
	        // Update the fragment spinner
			updateFragListSpinner();
			
		} catch (Exception e) {
			e.printStackTrace();
			this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			JOptionPane.showMessageDialog(null,
	                "<html>Could not read file '" + file.getAbsolutePath() 
	                + "'!<br>Hint of cause: " + e.getCause() + "</html>",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
		}
		this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Puts currently loaded structure in the Jmol viewer.
	 * This assumes that the structure is already added to the list of 
	 * IAtomContainers and that the currFrgIdx filed is properly set.
	 */
	private void loadCurrentAsPlainStructure()
	{
		fragmentViewer.loadPlainStructure(fragment.getIAtomContainer());
	}

//-----------------------------------------------------------------------------
	
	/**
	 * Loads the fragments corresponding to the field index.
	 * The molecular data is loaded in the Jmol viewer,
	 * and the attachment point (AP) information in the the list of APs.
	 * Jmol is not aware of AP-related information, so this also launches
	 * the generation of the graphical objects representing the APs.
	 */
	private void loadCurrentFragIdxToViewer()
	{
		if (fragmentLibrary == null)
		{
			JOptionPane.showMessageDialog(null,
	                "No list of fragments loaded.",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;
		}
		
		clearCurrentSystem();

		fragment = fragmentLibrary.get(currFrgIdx);
		fragmentViewer.loadFragImentToViewer(fragment);
	}
	
//-----------------------------------------------------------------------------
	
	private void clearCurrentSystem()
	{
		// Get rid of currently loaded mol
		fragment = null;
		
		// Clear viewer?
		// No, its clears upon loading of a new system.
		// The exception (i.e., removal of the last fragment) is dealt with by
		// submitting "zap" only in that occasion.
		
		// Remove tmp storage of APs
		fragmentViewer.mapAPs = null;
		
		// Remove table of APs
		fragmentViewer.clearAPTable();
	}

//-----------------------------------------------------------------------------

	private void updateFragListSpinner()
	{		
		fragNavigSpinner.setModel(new SpinnerNumberModel(currFrgIdx+1, 1, 
				fragmentLibrary.size(), 1));
		totalFragsLabel.setText(Integer.toString(fragmentLibrary.size()));
	}
    
//-----------------------------------------------------------------------------
    
    /**
     * Removes an atom and replaces it with an attachment point.
     * @param apClass the attachment point class of the new fragment
     * @param trgAtm
     * @return <code>true</code> if the conversion was successful
     */
    private boolean convertAtomToAP(IAtom trgAtm, String apClass)
    {
    	// Accept ONLY if the atom has one and only one connected neighbour
    	if (fragment.getConnectedAtomsCount(trgAtm) != 1)
    	{
    		String str = "";
    		for (IAtom atm : fragment.getConnectedAtomsList(trgAtm))
    		{
    			str = str + " " + atm.getSymbol() 
    	                + (fragment.getAtomNumber(atm));
    		}
    		System.out.println("Connected atoms: "+str);
    		
			JOptionPane.showMessageDialog(null,
	                "<html>Atom "+ trgAtm.getSymbol() 
	                + (fragment.getAtomNumber(trgAtm)) 
	                + " has zero or more than one neighbour.<br>I can only "
	                + "transform atoms"
	                + " that have one and only one neighbour.</html>",
	                "Error",
	                JOptionPane.ERROR_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
    		return false;
    	}
    	
    	IAtom srcAtm = fragment.getConnectedAtomsList(trgAtm).get(0);
    	
    	Point3d srcP3d = DENOPTIMMoleculeUtils.getPoint3d(srcAtm);
    	Point3d trgP3d = DENOPTIMMoleculeUtils.getPoint3d(trgAtm);
    	Point3d vector = new Point3d();
    	vector.x = srcP3d.x + (trgP3d.x - srcP3d.x);
    	vector.y = srcP3d.y + (trgP3d.y - srcP3d.y);
    	vector.z = srcP3d.z + (trgP3d.z - srcP3d.z);
    	try {
			fragment.addAP(srcAtm, APClass.make(apClass), vector);
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
    	ArrayList<IBond> bnsToDel = new ArrayList<IBond>();
    	for (IAtom atm : atmsToDels)
    	{
	    	for (IBond bnd : fragment.bonds())
	    	{
	    		if (bnd.contains(atm))
	    		{
	    			bnsToDel.add(bnd);
	    		}
	    	}
    	}
    	for (IBond bnd : bnsToDel)
    	{
    		fragment.removeBond(bnd);
    	}
    	for (IAtom atm : atmsToDels)
    	{
    		fragment.removeAtomAndConnectedElectronContainers(atm);
    	}
    	
    	
    }
	
//-----------------------------------------------------------------------------
	
	private class FragSpinnerChangeEvent implements ChangeListener
	{
		private boolean inEnabled = true;
		
		public FragSpinnerChangeEvent()
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
        	currFrgIdx = ((Integer) fragNavigSpinner.getValue()).intValue() - 1;
        	loadCurrentFragIdxToViewer();
        	
        	activateTabEditsListener(true);
        }
	}
	
//-----------------------------------------------------------------------------

	private void deprotectEditedSystem()
	{
		btnSaveEdits.setEnabled(false);
		btnAddFrag.setEnabled(true);
		btnOpenFrags.setEnabled(true);
		btnOpenSMILES.setEnabled(true); 
		btnOpenMol.setEnabled(true);
		btnDelSel.setEnabled(true);
		btnAtmToAP.setEnabled(true);
		
		((DefaultEditor) fragNavigSpinner.getEditor())
			.getTextField().setEditable(true); 
		((DefaultEditor) fragNavigSpinner.getEditor())
			.getTextField().setForeground(Color.BLACK);
		fragmentViewer.deprotectEdits();
		
		fragSpinnerListener.setEnabled(true);
	}
	
//-----------------------------------------------------------------------------
	
	private void protectEditedSystem()
	{
		btnSaveEdits.setEnabled(true);
		btnAddFrag.setEnabled(false);
		btnOpenFrags.setEnabled(false);
		btnOpenSMILES.setEnabled(false); 
		btnOpenMol.setEnabled(false);
		btnDelSel.setEnabled(false);
		btnAtmToAP.setEnabled(false);
		
		fragNavigSpinner.setModel(new SpinnerNumberModel(currFrgIdx+1, 
				currFrgIdx+1, currFrgIdx+1, 1));
		((DefaultEditor) fragNavigSpinner.getEditor())
			.getTextField().setEditable(false); 
		((DefaultEditor) fragNavigSpinner.getEditor())
			.getTextField().setForeground(Color.GRAY);
		
		fragSpinnerListener.setEnabled(false);
	}
	
//-----------------------------------------------------------------------------

    private void activateTabEditsListener(boolean var)
    {
		fragmentViewer.activateTabEditsListener(var);
    }
    
//-----------------------------------------------------------------------------
    
    private void removeCurrentFragment() throws DENOPTIMException
    {
    	if (fragmentViewer.hasUnsavedAPEdits())
    	{
			String[] options = new String[]{"Yes","No"};
			int res = JOptionPane.showOptionDialog(null,
	                "<html>Removing unsaved fragment?",
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

    	// Takes care of "fragment" and AP info in GUI components
    	clearCurrentSystem();
    	
    	// Actual removal from the library
    	if (fragmentLibrary.size()>0)
    	{
    		fragmentLibrary.remove(currFrgIdx);
    		int libSize = fragmentLibrary.size();
    		
    		if (currFrgIdx>=0 && currFrgIdx<libSize)
    		{
    			//we keep currFrgIdx as it will correspond to the next item
    		}
    		else
    		{
    			currFrgIdx = currFrgIdx-1;
    		}
    		
    		if (currFrgIdx==-1 || fragmentLibrary.size()==0)
			{
				fragmentViewer.clearMolecularViewer();
				currFrgIdx = 0;
				fragNavigSpinner.setModel(new SpinnerNumberModel(0,0,0,1));
				totalFragsLabel.setText(Integer.toString(0));
				deprotectEditedSystem();
    		}
    		else
    		{
	    		// We use the currFrgIdx to load another fragment
		    	loadCurrentFragIdxToViewer();
		    	updateFragListSpinner();
		    	fragNavigSpinner.setModel(new SpinnerNumberModel(currFrgIdx+1, 1, 
						fragmentLibrary.size(), 1));
		        deprotectEditedSystem();
    		}
    	}
    }

//-----------------------------------------------------------------------------

  	private void saveUnsavedChanges() 
  	{
  		if (fragmentViewer.hasUnsavedAPEdits())
  		{
	  		// Retrieve chemical object from the viewer
	  		fragment = fragmentViewer.getLoadedStructure();
	  		
	  		// Import changes from AP table into molecular representation
	        for (int i=0; i<fragmentViewer.apTabModel.getRowCount(); i++) 
	        {	        	
	        	int apId = ((Integer) fragmentViewer.apTabModel.getValueAt(i, 0))
	        			.intValue();
	        	String currApClass = fragmentViewer.apTabModel.getValueAt(i, 1)
	        			.toString();
	        	
	        	// Make sure the new class has a proper syntax
	        	try {
					currApClass = ensureGoodAPClassString(currApClass,true);
				} catch (DENOPTIMException e1) {
					currApClass = "dafaultAPClass:0";
				}
	        	
	        	if (fragmentViewer.mapAPs.containsKey(apId))
	        	{
	        		String origApClass = 
	        				fragmentViewer.mapAPs.get(apId).getAPClass().toString();
	        		if (!origApClass.equals(currApClass))
	        		{
	        			try {
	        				fragmentViewer.mapAPs.get(apId).setAPClass(currApClass);
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
	  		
	  		// Overwrite fragment in library
	  		fragmentLibrary.set(currFrgIdx, fragment);
	  	}
        
        // Reload fragment from library to refresh table and viewer
    	activateTabEditsListener(false);
    	loadCurrentFragIdxToViewer();
  		
  		// Release constraints
    	activateTabEditsListener(true);
    	fragNavigSpinner.setModel(new SpinnerNumberModel(currFrgIdx+1, 1, 
				fragmentLibrary.size(), 1));
        deprotectEditedSystem();
  	}

//----------------------------------------------------------------------------
  	
  	/**
  	 * Forces the user to specify a properly formatted APClass.
  	 * @param currApClass the current value of the APClass, or empty string
  	 * @param mustReply set to <code>true</code> to prevent escaping the question
  	 * @return 
  	 * @throws DENOPTIMException 
  	 */
	private String ensureGoodAPClassString(String currApClass, 
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
	 * Check whether there are unsaved changes.
	 * @return <code>true</code> if there are unsaved changes.
	 */
	
	public boolean hasUnsavedChanges()
	{
		return unsavedChanges;
	}
		
//-----------------------------------------------------------------------------
  	
}
