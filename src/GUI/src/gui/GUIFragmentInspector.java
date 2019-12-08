package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.vecmath.Point3d;

import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolViewer;
import org.jmol.viewer.Viewer;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMFragment;
import denoptim.utils.DENOPTIMMathUtils;
import denoptim.utils.FragmentUtils;


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
	 * Temporary list of attachment points of the current fragment
	 */
	private Map<Integer,DENOPTIMAttachmentPoint> mapAPs = null;
	
	/**
	 * The index of the currently loaded fragment [0–(n-1)}
	 */
	private int currFrgIdx = 0;
	
	/**
	 * Flag signaling that data about APs has been changed in the GUI
	 */
	private boolean alteredAPData = false;
	
	/**
	 * Flag signaling that loaded data has changes since last save
	 */
	private boolean unsavedChanges = false;
	
	private JPanel centralPanel;
	private JmolPanel jmolPanel;
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
	
	private DefaultTableModel apTabModel;
	private JTable apTable;
	
	private JPanel pnlOpenMol;
	private JButton btnOpenMol;
	
	private JPanel pnlOpenSMILES;
	private JButton btnOpenSMILES;
	
	private JPanel pnlAtmToAP;
	private JButton btnAtmToAP;
	
	private JPanel pnlDelSel;
	private JButton btnDelSel;
	
	private JPanel pnlPrintMol; //for testing
	private JButton btnPrintMol;//for testing
	
	private JPanel pnlSaveEdits;
	private JButton btnSaveEdits;
	
	private final String NL = System.getProperty("line.separator");
	
	String tmpSDFFile = "/tmp/Denoptim_GUIFragInspector_loadedMol.sdf";
	
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
	@SuppressWarnings("serial")
	private void initialize() {
		
		// BorderLayout is needed to allow dynamic resizing!
		this.setLayout(new BorderLayout()); 
		
		// This card structure includes center, east and south panels:
		// - (Center) Jmol stuff. Includes
		//            - molecular viewer
		//            - list (table) of attachment points
		// - (East) Fragment controls
		// - (South) general controls (load, save, close)
		
		// The Jmol stuff goes all in here
        centralPanel = new JPanel(new BorderLayout());
        this.add(centralPanel,BorderLayout.CENTER);
        
        // Jmol viewer panel
        jmolPanel = new JmolPanel();
        jmolPanel.setPreferredSize(new Dimension(400, 400));
        centralPanel.add(jmolPanel,BorderLayout.CENTER);
        
		// List of attachment points
		apTabModel = new DefaultTableModel(){
			@Override
		    public boolean isCellEditable(int row, int column) {
				if (column == 0 || row == 0)
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
		apTable = new JTable(apTabModel);
		apTable.putClientProperty("terminateEditOnFocusLost", true);
		apTable.getColumnModel().getColumn(0).setMaxWidth(75);
		apTable.setGridColor(Color.BLACK);
		apTabModel.addTableModelListener(new PausableTableModelListener());
		centralPanel.add(apTable,BorderLayout.SOUTH);
		
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
		btnAddFrag.setToolTipText("Starts creation of a new fragment.");
		btnAddFrag.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String[] options = new String[]{"SMILES","File","Cancel"};
				int res = JOptionPane.showOptionDialog(null,
		                "<html>Importing structure for creation of a new "
		                + "fragment<br>"
		                + "Please choose a source: <ul>"
		                + "<li>SMILES (requires internet connection)</li>"
		                + "<li>SD/SDF files (only the first "
		                + "structure is imported).</li>"
		                + "</ul></html>",
		                "Specify source of moelcular structure",
		                JOptionPane.DEFAULT_OPTION,
		                JOptionPane.QUESTION_MESSAGE,
		                UIManager.getIcon("OptionPane.warningIcon"),
		                options,
		                options[1]);
				switch (res)
				{
					case 0:
						String smiles = JOptionPane.showInputDialog(
	                			"Please input SMILES: ");
	                	if (smiles != null && !smiles.trim().equals(""))
	                	{
	                	    importStructureFromSMILES(smiles);
	                	}
	                	break;
	                	
	                case 1:
						File inFile = DenoptimGUIFileOpener.pickFile();
						if (inFile == null || inFile.getAbsolutePath().equals(""))
						{
							return;
						}
						importStructureFromFile(inFile);
						
	                case 3:
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
					if (fragmentLibrary.size()==0)
					{
						jmolPanel.viewer.evalString("zap");
					}
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
		
		pnlOpenMol = new JPanel();
		btnOpenMol = new JButton("Structure from File");
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
		pnlOpenMol.add(btnOpenMol);
		fragCtrlPane.add(pnlOpenMol);
		
        pnlOpenSMILES = new JPanel();
        btnOpenSMILES = new JButton("Structure from SMILES");
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
        pnlOpenSMILES.add(btnOpenSMILES);
        fragCtrlPane.add(pnlOpenSMILES);
		
		fragCtrlPane.add(new JSeparator());
		
		pnlAtmToAP = new JPanel();
		btnAtmToAP = new JButton("Atom to AP");
		btnAtmToAP.setToolTipText("<html>Replaces the selected atoms with "
				+ "attachment points.<br>Click on atoms to select"
			    + " them. Click again to unselect.<br>"
			    + "<br><b>WARNING:</b> this action cannot be undone!<html>");
		btnAtmToAP.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ArrayList<IAtom> selectedAtms = getAtomsSelectedFromJMol();
				
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
					//TODO: deal with changes to the AP table first
					
					
					//TODO: ask about hapticity:
					// if multihapto, then use all the selected atoms for 1 AP
					// and ask to select another set for other end of bond to 
					// break.
					
					String apClass = ensureGoodAPClassString("");
					
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
					
					// Load the "fragment" obj to the viewer
					loadCurrentAsPlainStructure();
					
					// Update APs using info in atoms (overwrites map and table)
					updateAPsMapAndTable();
			        
			        // Display the APs as arrows
			        updateAPsInJmolViewer();
					
			        // Protect the temporary "fragment" obj
			        unsavedChanges = true;
			        alteredAPData = true;
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
				ArrayList<IAtom> selectedAtms = getAtomsSelectedFromJMol();
				
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
					//TODO: deal with changes to the AP table first
					
					removeAtoms(selectedAtms);
					
					// Use the APs stored in the atoms
					fragment.updateAPs();
					
					// Load the "fragment" obj to the viewer
					loadCurrentAsPlainStructure();
					
					// Update APs using info in atoms (overwrites map and table)
					updateAPsMapAndTable();
			        
			        // Display the APs as arrows
			        updateAPsInJmolViewer();
					
			        // Protect the temporary "fragment" obj
			        unsavedChanges = true;
			        alteredAPData = true;
			        protectEditedSystem();
				}
			}
		});
		pnlDelSel.add(btnDelSel);
		fragCtrlPane.add(pnlDelSel);
		
		//TODO comment out button (only for testing)
		/*
        pnlPrintMol = new JPanel();
        btnPrintMol = new JButton("TEST STUFF");
        btnPrintMol.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {         	
            	System.out.println("#Bonds: "+fragment.getBondCount());
            	System.out.println("#Atoms: "+fragment.getAtomCount());
            	for (IAtom atm : fragment.atoms())
            	{
                	System.out.println("    "+atm.getSymbol()+fragment.getAtomNumber(atm));
            	}
            	System.out.println("#APs:   "+fragment.getAPCount());
            	for (DENOPTIMAttachmentPoint ap : fragment.getCurrentAPs())
            	{
            		System.out.println("    "+ap);
            	}
            	System.out.println("Library");
            	for (IAtomContainer mol : fragmentLibrary)
            	{
            		System.out.println("Frag #atoms: "+mol.getAtomCount());
            	}
            	
            }
        });
        pnlPrintMol.add(btnPrintMol);
        fragCtrlPane.add(pnlPrintMol);
		*/
		
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
				JOptionPane.showMessageDialog(null,
                    "<html>Hover over the buttons and fields "
                    + "to get a tip.</html>",
                    "Tips",
                    JOptionPane.PLAIN_MESSAGE);
			}
		});
		commandsPane.add(btnHelp);
	}

//-----------------------------------------------------------------------------
	
	public void importStructureFromFile(File file)
	{
		// Cleanup
		clearCurrentSystem();
		
		try {			
			IAtomContainer mol = DenoptimIO.readSingleSDFFile(
					file.getAbsolutePath());
			
			// We mean to import only the structure: get rid of AP
			mol.setProperty(DENOPTIMConstants.APTAG,null);
			mol.setProperty(DENOPTIMConstants.APCVTAG,null);
			
			fragment = new DENOPTIMFragment(mol);

			// the system is not a fragment but, this is done for consistency:
			// when we have a molecule loaded the list is not empty
			// The currently viewed fragment (if any) is always part of the lib
			fragmentLibrary.add(fragment); 
			currFrgIdx = fragmentLibrary.size()-1;
			
			loadCurrentAsPlainStructure();
			updateFragListSpinner();
			unsavedChanges = true;
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null,
	                "<html>Could not read file '" + file.getAbsolutePath() 
	                + "'!<br>Hint about reason: " + e.getCause() + "</html>",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
		}
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
		// Cleanup
		clearCurrentSystem();
		
		// Load the structure using CACTUS service via Jmol
		jmolPanel.viewer.evalString("load $"+smiles);
		waitForJmolViewer(1500);

		// NB: this is a workaround to the lack fo try/catch mechanism when
		// executing Jmol commands.
		// We want to catch errors that prevent loading the structure
		// For example, offline mode and invalid SMILES
		String data = jmolPanel.viewer.getData("*", "txt");				
		if (data == null || data.equals(""))
		{
			System.out.println("DATA: "+data);
			JOptionPane.showMessageDialog(null,
	                "<html>Could not find a valid structure.<br>"
	                + "Possible reasons are:"
	                + "<ul>"
	                + "<li>unreachable remote service (we are offline)</li>"
	                + "<li>no available structure for SMILES string<br>'" 
	                + smiles 
	                + "'</li></ul><br>"
	                + "Please create such structure and import it as SDF file."
	                + "</html>",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;
		}
		
		// Now we should have a structure loaded in the viewer, 
		// so we take that one and put it in the IAtomContainer representation
		try	{
		    fragment = new DENOPTIMFragment(getStructureFromJmolViewer());
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null,
	                "<html>Could not understand Jmol system.</html>",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;
		}
		
		// the system is not a fragment but, this is done for consistency:
		// when we have a molecule loaded the list is not empty:
		// The currently viewed fragment (if any) is always part of the library
	    fragmentLibrary.add(fragment);
		currFrgIdx = fragmentLibrary.size()-1;
		
		// finalize GUI status
		updateFragListSpinner();
		setJmolViewer();
		unsavedChanges = true;
	}
	
//-----------------------------------------------------------------------------
	
	private IAtomContainer getStructureFromJmolViewer() throws DENOPTIMException
	{
		IAtomContainer mol = new AtomContainer();
		
		String strData = jmolPanel.viewer.getData("*", "txt");
		strData = strData.replaceAll("[0-9] \\s+ 999 V2000", 
				"0  0  0  0  0999 V2000");
		String[] lines = strData.split("\\n");
		if (lines.length < 5)
		{
			clearCurrentSystem();
			throw new DENOPTIMException("Unexpected format in Jmol molecular "
					+ "data: " + strData);
		}
		if (!lines[3].matches(".*999 V2000.*"))
		{
			clearCurrentSystem();
			throw new DENOPTIMException("Unexpected format in Jmol molecular "
					+ "data: " + strData);
		}
		String[] counters = lines[3].trim().split("\\s+");
		int nAtms = Integer.parseInt(counters[0]);
		int nBonds = Integer.parseInt(counters[1]);
		
		
		StringBuilder sb = new StringBuilder();
		sb.append("Structure in JmolViewer").append(NL);
		sb.append("Jmol").append(NL).append(NL);
		sb.append(lines[3]).append(NL);
		for (int i=0; i<nAtms; i++)
		{
			sb.append(lines[3+i+1]).append("  0  0  0  0  0  0").append(NL);
		}
		for (int i=0; i<nBonds; i++)
		{
			sb.append(lines[3+nAtms+i+1]).append("  0").append(NL);
		}
		sb.append("M  END").append(NL).append("$$$$");
		
		DenoptimIO.writeData(tmpSDFFile, sb.toString(), false);
		mol = DenoptimIO.readSingleSDFFile(tmpSDFFile);
		return mol;
	}
	
//-----------------------------------------------------------------------------
	
	private void waitForJmolViewer(int milliSecFirst)
	{
		// We wait
		try {
			Thread.sleep(milliSecFirst);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// Waiting cycles
		Date date = new Date();
		long startTime = date.getTime();
		long wallTime = startTime + 5000; // 5 seconds
		while (jmolPanel.viewer.isScriptExecuting())
		{
				
			Date newDate = new Date();
			long now = newDate.getTime();
			System.out.println("Waiting "+now);
			if (now > wallTime)
			{
				String[] options = new String[]{"Yes","No"};
				int res = JOptionPane.showOptionDialog(null,
		                "<html>Slow response from Jmol.<br>Keep waiting?</html>",
		                "Should we wait for another 5 seconds?",
		                JOptionPane.DEFAULT_OPTION,
		                JOptionPane.QUESTION_MESSAGE,
		                UIManager.getIcon("OptionPane.warningIcon"),
		                options,
		                options[1]);
				if (res == 1)
				{
					break;
				}
				else
				{
					newDate = new Date();
					wallTime = newDate.getTime() + 5000; // 5 seconds
				}
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
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
		fragmentLibrary = new ArrayList<DENOPTIMFragment>();
		try 
		{
			// Import library of fragments
			for (IAtomContainer iac : DenoptimIO.readMoleculeData(
												file.getAbsolutePath(),format))
			{
			    fragmentLibrary.add(new DENOPTIMFragment(iac));
			}
			
			// Display the first
			currFrgIdx = 0;
			loadCurrentFragIdxToViewer();
			
	        // Update the fragment spinner
			updateFragListSpinner();
			
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null,
	                "<html>Could not read file '" + file.getAbsolutePath() 
	                + "'!<br>Hint of cause: " + e.getCause() + "</html>",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
		}
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Puts currently loaded structure in the Jmol viewer.
	 * This assumes that the structure is already added to the list of 
	 * IAtomContainers and that the currFrgIdx filed is properly set.
	 */
	private void loadCurrentAsPlainStructure()
	{
		if (fragment == null)
		{
			JOptionPane.showMessageDialog(null,
	                "<html>No structure loaded.<br>This is most likely a bug!"
	                + "Please report it to the development team.</html>",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;			
		}
		
		// NB: we use the nasty trick of a tmp file to by-pass the 
		// fragile/discontinued CDK-to-Jmol support.
		
		try {
			DenoptimIO.writeMolecule(tmpSDFFile, fragment, false);
		} catch (DENOPTIMException e) {
			// TODO change and look for other tmp file locations
			e.printStackTrace();
			System.out.println("Error writing TMP file");
		}
		jmolPanel.viewer.openFile(tmpSDFFile);
		setJmolViewer();
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
			
		// Load the molecular data into Jmol
		// NB: we use the nasty trick of a tmp file to by-pass the 
		// fragile/discontinued CDK-to-Jmol support.
		fragment = fragmentLibrary.get(currFrgIdx);
		loadCurrentAsPlainStructure();
		
		// Update APs using info in atoms (overwrites map and table)
		updateAPsMapAndTable();
        
        // Display the APs as arrows
        updateAPsInJmolViewer();
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Uses the AP of the {@link DENOPTIMFragment} to create a new map and 
	 * table of APs.
	 */
	private void updateAPsMapAndTable()
	{
		clearAPTable();
		mapAPs = new HashMap<Integer,DENOPTIMAttachmentPoint>();
		
		ArrayList<DENOPTIMAttachmentPoint> lstAPs = fragment.getCurrentAPs();		
        if (lstAPs.size() == 0)
        {
        	// WARNING: no dialog here because it fires event changes in JSpinner
			return;
        }
        
        activateTabEditsListener(false);
        apTabModel.addRow(new Object[]{"<html><b>AP#<b></html>",
		"<html><b>APClass<b></html>"});
        
        int arrId = 0;
	    for (DENOPTIMAttachmentPoint ap : lstAPs)
	    {
	    	arrId++;
	    	apTabModel.addRow(new Object[]{arrId, ap.getAPClass()});
	    	mapAPs.put(arrId,ap);
	    }
	    activateTabEditsListener(true);
	}
	
//-----------------------------------------------------------------------------
	
	private void clearAPTable()
	{
		activateTabEditsListener(false);
        for (int i=(apTabModel.getRowCount()-1); i>-1; i--) 
        {
        	apTabModel.removeRow(i);
        }
        activateTabEditsListener(true);
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
		mapAPs = null;
		
		// Remove table of APs
		clearAPTable();
	}

//-----------------------------------------------------------------------------
	
	private void updateAPsInJmolViewer()
	{   
		if (mapAPs == null || mapAPs.isEmpty())
		{
			return;
		}
		
		StringBuilder sb = new StringBuilder();
        for (int arrId : mapAPs.keySet())
        {
        	DENOPTIMAttachmentPoint ap = mapAPs.get(arrId);
        	int srcAtmId = ap.getAtomPositionNumber();
        	Point3d srcAtmPlace = FragmentUtils.getPoint3d(
        			fragment.getAtom(srcAtmId));
        	double[] startArrow = new double[]{
        			srcAtmPlace.x,
        			srcAtmPlace.y,
        			srcAtmPlace.z};
        	double[] endArrow = ap.getDirectionVector();
        	
        	double[] offSet = DENOPTIMMathUtils.scale(DENOPTIMMathUtils.subtract(endArrow,startArrow), 0.2);
        	double[] positionLabel = DENOPTIMMathUtils.add(endArrow,offSet); 
        	sb.append("draw arrow").append(arrId).append(" arrow ");
        	sb.append(getJmolPositionStr(startArrow));
        	sb.append(getJmolPositionStr(endArrow));
        	sb.append(" width 0.1");
        	sb.append(NL);
        	sb.append("set echo apLab").append(arrId);
        	sb.append(getJmolPositionStr(positionLabel));
        	sb.append("; echo ").append(arrId);
        	sb.append("; color echo yellow");
        	sb.append(NL);
        }
        jmolPanel.viewer.evalString(sb.toString());
	}

//-----------------------------------------------------------------------------

	private void updateFragListSpinner()
	{		
		fragNavigSpinner.setModel(new SpinnerNumberModel(currFrgIdx+1, 1, 
				fragmentLibrary.size(), 1));
		totalFragsLabel.setText(Integer.toString(fragmentLibrary.size()));
	}
	
//-----------------------------------------------------------------------------
	
	private String getJmolPositionStr(double[] position)
	{
		StringBuilder sb = new StringBuilder();
    	sb.append(" {");
    	sb.append(position[0]).append(" ");
    	sb.append(position[1]).append(" ");
    	sb.append(position[2]).append("} ");
		return sb.toString();
	}
	
//-----------------------------------------------------------------------------
	
	private void setJmolViewer()
	{
		//TODO del
		System.out.println("In setJmolViewer");
		StringBuilder sb = new StringBuilder();		
		sb.append("select none").append(NL); 
		sb.append("SelectionHalos ON").append(NL);
		sb.append("set picking ATOMS").append(NL);
		//sb.append("zoom 70").append(NL);
		
		jmolPanel.viewer.evalString(sb.toString());

	}
	
//-----------------------------------------------------------------------------
	
	private class JmolPanel extends JPanel 
	{

        /**
		 * Version UID
		 */
		private static final long serialVersionUID = 1699908697703788097L;

		JmolViewer viewer;

        private final Dimension hostPanelSize = new Dimension();

        public JmolPanel() {
            viewer = JmolViewer.allocateViewer(this, new SmarterJmolAdapter(), 
            null, null, null, null, null); //NB: can add listener here
        }
        
        //---------------------------------------------------------------------

		@Override
        public void paint(Graphics g) {
            getSize(hostPanelSize);
            viewer.renderScreenImage(g, hostPanelSize.width, hostPanelSize.height);
        }
    }

//-----------------------------------------------------------------------------
    
    private ArrayList<IAtom> getAtomsSelectedFromJMol()
    {
    	ArrayList<IAtom> selectedAtms = new ArrayList<IAtom>();
    	
    	if (fragment == null || fragment.getAtomCount()==0)
    	{
    		return selectedAtms;
    	}
    	
		for (int i =0; i< fragment.getAtomCount(); i++)
		{
			if (((Viewer) jmolPanel.viewer).slm.isSelected(i))
			{
				selectedAtms.add(fragment.getAtom(i));
			}
		}
    	
		return selectedAtms;
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
    	// Accept ONLY if the atom has one and only one connected neighbor
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
    	
    	Point3d srcP3d = FragmentUtils.getPoint3d(srcAtm);
    	Point3d trgP3d = FragmentUtils.getPoint3d(trgAtm);
    	Point3d vector = new Point3d();
    	vector.x = srcP3d.x + (trgP3d.x - srcP3d.x);
    	vector.y = srcP3d.y + (trgP3d.y - srcP3d.y);
    	vector.z = srcP3d.z + (trgP3d.z - srcP3d.z);
    	try {
			fragment.addAP(srcAtm, apClass, vector);
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
    
//-----------------------------------------------------------------------------
    
    /**
     * Create a multihapto AP from centroids of set of atoms.
     * @param srcAtms the set to be used to calculate centroid of the start of
     * the formal bond to break, or the end atom if only one is present
     * @param endAtm the set to be used to calculate centroid of the end of the
     * formal bond to break, or the end atom if only one is present
     * 
     */
    private void createAP(IAtom[] srcAtms, IAtom[] endAtms)
    {
    	//TODO
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Create a simple AP considering the bond from one atom to another.
     * @param srcAtm the atom on the fragment, and that will hold the AP
     * @param trgAtm the atom representing the next fragment, which does not 
     * exist. This atom will be removed from the system.
     */
    private void createAP(IAtom srcAtm, IAtom trgAtm)
    {  		
    	//Ask about class
    	//TODO
    	
    	//Make AP (use half-bond position for AP end)
    	//TODO
    	
    	// Remove atom
    	//TODO
    	
    	// Update APs
    	//TODO
    	
    	//Update viewer
    	//TODO
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
	
	private class PausableTableModelListener implements TableModelListener
	{	
		private boolean isActive = false;
		
		public PausableTableModelListener() {};

		@Override
		public void tableChanged(TableModelEvent e) 
		{
            if (isActive && !alteredAPData 
            		&& e.getType() == TableModelEvent.UPDATE)
            {
                alteredAPData = true;
                protectEditedSystem();
            }
		}
        
		public void setActive(boolean var)
		{
			isActive = var;
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
		
		fragNavigSpinner.setModel(new SpinnerNumberModel(currFrgIdx+1, 1, 
				fragmentLibrary.size(), 1));
		((DefaultEditor) fragNavigSpinner.getEditor())
			.getTextField().setEditable(true); 
		((DefaultEditor) fragNavigSpinner.getEditor())
			.getTextField().setForeground(Color.BLACK);
		
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
		try
		{
			PausableTableModelListener l = (PausableTableModelListener) 
					apTabModel.getTableModelListeners()[0];
    	    l.setActive(var);
		} catch (Throwable t) {
			//t.printStackTrace();
			System.out.println("Bad attempt to contro llistener: " 
					+ t.getMessage());
			System.out.println(t.getCause());
		}
    }
    
//-----------------------------------------------------------------------------
    
    private void removeCurrentFragment() throws DENOPTIMException
    {
    	if (alteredAPData)
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

    		// We use the currFrgIdx to load another fragment
	    	loadCurrentFragIdxToViewer();
	    	updateFragListSpinner();
	    	
	  		// Release constraints
	        alteredAPData = false;
	        deprotectEditedSystem();
	        
    	}
    }

//-----------------------------------------------------------------------------

  	private void saveUnsavedChanges() 
  	{
  		if (!alteredAPData)
  		{
  			return;
  		}
	    
  		// Retrieve chemical object from Jmol
		try	{
		    fragment = new DENOPTIMFragment(getStructureFromJmolViewer());
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null,
	                "<html>Could not understand Jmol system.</html>",
	                "Error",
	                JOptionPane.ERROR_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;
		}
  		
  		// Import changes from AP table into molecular representation
        for (int i=1; i<apTabModel.getRowCount(); i++) 
        {
        	int apId = ((Integer) apTabModel.getValueAt(i, 0)).intValue();
        	String currApClass = apTabModel.getValueAt(i, 1).toString();
        	
        	// Make sure the new class has a proper syntax
        	currApClass = ensureGoodAPClassString(currApClass);
        	
        	if (mapAPs.containsKey(apId))
        	{
        		String origApClass = mapAPs.get(apId).getAPClass();
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
        	
        	// Add AP to molecular representation
        	DENOPTIMAttachmentPoint ap = mapAPs.get(apId);
        	try {
				fragment.addAP(ap.getAtomPositionNumber(), currApClass,
						ap.getDirectionVector());
			} catch (DENOPTIMException e) {
				e.printStackTrace();
    			JOptionPane.showMessageDialog(null,
    	                "<html>Could not add attachment points.<br>"
    	                + "Please report this to the "
    	                + "DENOPTIM team.</html>",
    	                "Error",
    	                JOptionPane.PLAIN_MESSAGE,
    	                UIManager.getIcon("OptionPane.errorIcon"));
    			return;	
			}
        }
  		
  		// Overwrite fragment in library
  		fragmentLibrary.set(currFrgIdx, fragment);
        
        // Reload fragment from library to refresh table and viewer
    	activateTabEditsListener(false);
    	loadCurrentFragIdxToViewer();
  		
  		// Release constraints
    	activateTabEditsListener(true);
        alteredAPData = false;
        deprotectEditedSystem();
  	}
  	
//----------------------------------------------------------------------------
  	/**
  	 * Forces the user to specify a properly formatted APClass.
  	 * @param currApClass the current value of the APClass, or empty string
  	 * @return 
  	 */
	private String ensureGoodAPClassString(String currApClass) 
	{		
		String preStr = "";
		while (!DENOPTIMAttachmentPoint.isValidAPClassString(currApClass))
    	{
    		currApClass = JOptionPane.showInputDialog("<html>" + preStr 
    				+ "</ul>Please, provide a valid "
    				+ "APClass string: ");
        	if (currApClass == null)
        	{
        		currApClass = "";
        	}
        	
    		String syntax = "APClass '" + currApClass + "' is not valid!<br>"
    				+ "The valid syntax for APClass is:<br><br><code>rule" 
        			+ DENOPTIMConstants.SEPARATORAPPROPSCL 
    				+ "subClass</code><br><br> where "
    				+ "<ul><li><code>rule</code>"
    				+ " is a string (no spaces)</li>"
    				+ "<li><code>subClass</code> is an integer</li>";
        	
        	preStr = syntax;
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
