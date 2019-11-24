package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.vecmath.Point3d;

import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolViewer;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.utils.DENOPTIMMathUtils;
import denoptim.utils.FragmentUtils;


/**
 * A panel with a molecular viewer that understands DENOPTIM fragments.
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
	private ArrayList<IAtomContainer> fragmentLibrary;
	
	/**
	 * The currently loaded fragment
	 */
	private IAtomContainer fragIAC;
	
	/**
	 * The list of attachment points of the current fragment
	 */
	private ArrayList<DENOPTIMAttachmentPoint> lstAPs =
            new ArrayList<DENOPTIMAttachmentPoint>();
	
	/**
	 * The index of the currently loaded fragment [0–(n-1)}
	 */
	private int currFrgIdx = 0;
	
	/**
	 * Flag signaling that data about APs has been changed in the GUI
	 */
	private boolean alteredAPData = false;
	
	private JPanel centralPanel;
	private JmolPanel jmolPanel;
	private JPanel fragCtrlPane;
	private JPanel fragNavigPanel;
	
	private JSpinner fragNavigSpinner;
	private JLabel totalFragsLabel;
	
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
	
	private final String NL = System.getProperty("line.separator");
	
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
		apTable = new JTable(apTabModel);
		apTable.putClientProperty("terminateEditOnFocusLost", true);
		apTable.getColumnModel().getColumn(0).setMaxWidth(75);
		apTable.setGridColor(Color.BLACK);
		apTabModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent tme) {
                if (tme.getType() == TableModelEvent.UPDATE) {
                    alteredAPData = true;
                }
            }
        });
		centralPanel.add(apTable,BorderLayout.SOUTH);
		
		// General panel on the right: it containing all controls
        fragCtrlPane = new JPanel();
        fragCtrlPane.setVisible(true);
        fragCtrlPane.setLayout(new BoxLayout(fragCtrlPane, SwingConstants.VERTICAL));
        fragCtrlPane.add(new JSeparator());
		
        // NB: avoid GroupLayout because it interferes with Jmol viewer and causes exception
        
        // Controls to navigate the list of fragments
        fragNavigPanel = new JPanel();
        JLabel navigationLabel1 = new JLabel("Fragment ");
        JLabel navigationLabel2 = new JLabel(" of ");
        totalFragsLabel = new JLabel("none");
        
		fragNavigSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 0, 1));
		fragNavigSpinner.setToolTipText("Move to fragment number...");
		fragNavigSpinner.setPreferredSize(new Dimension(75,20));
		fragNavigSpinner.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent event)
            {
            	//NB here we convert from 1-based index in GUI to o-based index
            	currFrgIdx = ((Integer) fragNavigSpinner.getValue()).intValue() - 1;
            	loadCurrentFragIdxToViewer();
            }
        });
        fragNavigPanel.add(navigationLabel1);
		fragNavigPanel.add(fragNavigSpinner);
        fragNavigPanel.add(navigationLabel2);
        fragNavigPanel.add(totalFragsLabel);
		fragCtrlPane.add(fragNavigPanel);
		
		fragCtrlPane.add(new JSeparator());
		
		/*
		JPanel pnlTitleImport = new JPanel();
		JLabel lblImport = new JLabel("Import a molecular model");
		pnlTitleImport.add(lblImport);
		fragCtrlPane.add(pnlTitleImport);
		*/
		
		pnlOpenMol = new JPanel();
		btnOpenMol = new JButton("Structure from File");
		btnOpenMol.setToolTipText("Imports a chemical system"
				+ "from file.");
		btnOpenMol.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//TODO
			}
		});
		pnlOpenMol.add(btnOpenMol);
		fragCtrlPane.add(pnlOpenMol);
		
        pnlOpenSMILES = new JPanel();
        btnOpenSMILES = new JButton("Structure from SMILES");
        btnOpenSMILES.setToolTipText("Imports chemical system"
                        + "from file.");
        btnOpenSMILES.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                        //TODO
                }
        });
        pnlOpenSMILES.add(btnOpenSMILES);
        fragCtrlPane.add(pnlOpenSMILES);
		
		fragCtrlPane.add(new JSeparator());
		
		/*
		JPanel pnlTitleEdit = new JPanel();
		JLabel lblEdit = new JLabel("Edit fragment");
		pnlTitleEdit.add(lblEdit);
		fragCtrlPane.add(pnlTitleEdit);
		*/
		
		pnlAtmToAP = new JPanel();
		btnAtmToAP = new JButton("Atom to AP");
		btnAtmToAP.setToolTipText("<html>Replaces the selected atom with an "
				+ "attachment point.<br>APClass can be specified after clcking"
				+ " here.<html>");
		btnAtmToAP.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//TODO 
			}
		});
		pnlAtmToAP.add(btnAtmToAP);
		fragCtrlPane.add(pnlAtmToAP);
		
		pnlDelSel = new JPanel();
		btnDelSel = new JButton("Remove Atoms");
		btnDelSel.setToolTipText("<html>Removes all selected atoms from the "
				+ "systhem.<br>This is not reversible, but takes care of "
				+ "updating the attachment"
				+ " points.<br>Use this instead of Jmol commands.</html>");
		btnDelSel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//TODO 
			}
		});
		pnlDelSel.add(btnDelSel);
		fragCtrlPane.add(pnlDelSel);
		
		this.add(fragCtrlPane,BorderLayout.EAST);
		
		// Panel with buttons to the bottom of the frame
		JPanel commandsPane = new JPanel();
		super.add(commandsPane, BorderLayout.SOUTH);
		
		JButton btnOpenFrags = new JButton("Load",
					UIManager.getIcon("FileView.directoryIcon"));
		btnOpenFrags.setToolTipText("Reads fragments from file.");
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
		
		JButton btnSaveFrags = new JButton("Save",
				UIManager.getIcon("FileView.hardDriveIcon"));
		btnSaveFrags.setToolTipText("Write all fragments to file.");
		btnSaveFrags.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File outFile = DenoptimGUIFileOpener.saveFile();
				if (outFile == null)
				{
					return;
				}
				try
				{
				    //TODO write SDF
				}
				catch (Exception ex)
				{
					JOptionPane.showMessageDialog(null,
			                "Could not write to '" + outFile + "'!.",
			                "Error",
			                JOptionPane.PLAIN_MESSAGE,
			                UIManager.getIcon("OptionPane.errorIcon"));
				}
			}
		});
		commandsPane.add(btnSaveFrags);

		JButton btnCanc = new JButton("Cancel");
		btnCanc.setToolTipText("Leave without saving.");
		btnCanc.addActionListener(new removeCardActionListener(this));
		commandsPane.add(btnCanc);
		
		JButton btnHelp = new JButton("?");
		btnHelp.setToolTipText("Help");
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(null,
                    new JLabel("<html>Hover over the buttons and fields "
                    		+ "to get a tip.</html>"),
                    "Tips",
                    JOptionPane.PLAIN_MESSAGE);
			}
		});
		commandsPane.add(btnHelp);
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
		try {
			// Import library of fragments
			fragmentLibrary = DenoptimIO.readMoleculeData(
												file.getAbsolutePath());
			
			// Display the first
			currFrgIdx = 0;
			loadCurrentFragIdxToViewer();
			
	        // Update the fragment spinner
			fragNavigSpinner.setModel(new SpinnerNumberModel(currFrgIdx+1, 1, 
					fragmentLibrary.size(), 1));
			totalFragsLabel.setText(Integer.toString(fragmentLibrary.size()));
			
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
			
		// Load the molecular data into Jmol
		// NB: we use the nasty trick of a tmp file to by-pass the 
		// fragile/discontinued CDK-to-Jmol support.
		fragIAC = fragmentLibrary.get(currFrgIdx);
		String tmpSDFFile = "/tmp/Denoptim_GUIFragInspector_loadedMol.sdf";
		try {
			DenoptimIO.writeMolecule(tmpSDFFile, fragIAC, false);
		} catch (DENOPTIMException e) {
			// TODO change and look for other tmp file locations
			e.printStackTrace();
			System.out.println("Error writing TMP file");
		}
		jmolPanel.viewer.openFile(tmpSDFFile);
		setJmolViewer();
		
		// Get attachment point data
        try
        {
            lstAPs = FragmentUtils.getAPForFragment(fragIAC);
        }
        catch (DENOPTIMException de)
        {
			JOptionPane.showMessageDialog(null,
	                "<html>Fragment #" + (currFrgIdx + 1) 
	                + " does not seem to be"
	                + "properly formatted.<br> Hint: " 
	                + de.getMessage() + "</html>",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;
        }
        
        // Clear showed list of APs
        for (int i=(apTabModel.getRowCount()-1); i>-1; i--) 
        {
        	apTabModel.removeRow(i);
        }
        apTabModel.addRow(new Object[]{"<html><b>AP<b></html>",
		"<html><b>APClass<b></html>"});
        
        // Re-populate list of APs
        int arrId = 0;  //NB: consistent with updateAPsInJmolViewer()
	    for (DENOPTIMAttachmentPoint ap : lstAPs)
	    {
	    	arrId++;
	    	apTabModel.addRow(new Object[]{arrId, ap.getAPClass()});
	    }
        
        // Display the APs as arrows
        updateAPsInJmolViewer();
	}

//-----------------------------------------------------------------------------
	
	private void updateAPsInJmolViewer()
	{   
		StringBuilder sb = new StringBuilder();
		int arrId = 0;
        for (DENOPTIMAttachmentPoint ap : lstAPs)
        {
        	arrId++;
        	int srcAtmId = ap.getAtomPositionNumber();
        	Point3d srcAtmPlace = fragIAC.getAtom(srcAtmId).getPoint3d();
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
		StringBuilder sb = new StringBuilder();		
		sb.append("select none").append(NL); 
		sb.append("SelectionHalos ON").append(NL);
		sb.append("set picking ATOMS").append(NL);
		
		// sb.append("").append(NL);
		
		jmolPanel.viewer.evalString(sb.toString());
	}
	
//-----------------------------------------------------------------------------
	
	private class JmolPanel extends JPanel {

        /**
		 * Version UID
		 */
		private static final long serialVersionUID = 1699908697703788097L;

		JmolViewer viewer;

        private final Dimension hostPanelSize = new Dimension();

        public JmolPanel() {
            viewer = JmolViewer.allocateViewer(this, new SmarterJmolAdapter(), 
            null, null, null, null, null);
        }

        @Override
        public void paint(Graphics g) {
            getSize(hostPanelSize);
            viewer.renderScreenImage(g, hostPanelSize.width, hostPanelSize.height);
        }
    }
	
//-----------------------------------------------------------------------------
}
