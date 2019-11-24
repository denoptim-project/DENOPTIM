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
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.vecmath.Point3d;

import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolViewer;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.molecule.DENOPTIMAttachmentPoint;
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
	
	private JmolPanel jmolPanel;
	
	private JPanel fragCtrlPane;
	
	private JPanel fragNavigPanel;
	private JSpinner fragNavigSpinner;
	private JLabel totalFragsLabel;
	
	private DefaultTableModel apTabModel;
	private JTable apTable;
	
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
        JPanel centralPanel = new JPanel(new BorderLayout());
        this.add(centralPanel,BorderLayout.CENTER);
        
        // Jmol viewer panel
        jmolPanel = new JmolPanel();
        jmolPanel.setPreferredSize(new Dimension(400, 400));
        centralPanel.add(jmolPanel,BorderLayout.CENTER);
        
		// List of attachment points
		apTabModel = new DefaultTableModel(){
			@Override
		    public boolean isCellEditable(int row, int column) {
		       return false;
		    }
		};
		apTabModel.setColumnCount(2);
		apTable = new JTable(apTabModel);
		apTable.getColumnModel().getColumn(0).setMaxWidth(75);
		TabRendenrer tabRenderer = new TabRendenrer();
		apTable.setDefaultRenderer(Object.class, tabRenderer);
		centralPanel.add(apTable,BorderLayout.SOUTH);

        /*
        // Jmol command line panel
        JPanel jmolCmdPanel = new JPanel();
        jmolCmdPanel.setLayout(new BorderLayout());
        jmolCmdPanel.setPreferredSize(new Dimension(400, 100));
        AppConsole jmolCmdLine = new AppConsole(jmolPanel.viewer, jmolCmdPanel,
        		"Clear Undo Redo");
        jmolPanel.viewer.setJmolCallbackListener(jmolCmdLine);
        jmolCmdPanel.setToolTipText("<html>Jmol command line and shortcut "
        		+ "buttons.<br>Use these to control the molecular viewer."
        		+ "</html>");
        centralPanel.add(jmolCmdPanel, BorderLayout.SOUTH);
        */
		
		// General panel on the right: it containing all controls
        fragCtrlPane = new JPanel();
        fragCtrlPane.setVisible(true);
        fragCtrlPane.setLayout(new BoxLayout(fragCtrlPane, SwingConstants.VERTICAL));
		
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
        apTabModel.addRow(new Object[]{"<html><b>AtomID<b></html>",
		"<html><b>APClass<b></html>"});
        
        // Re-populate list of APs
	    for (DENOPTIMAttachmentPoint ap : lstAPs)
	    {
	    	//NB: display 1-based indexed according to Jmol practices
	    	apTabModel.addRow(new Object[]{ap.getAtomPositionNumber()+1, 
	    			ap.getAPClass()});
	    }
        
        // Display the APs as arrows
        updateAPsInJmolViewer();
	}

//-----------------------------------------------------------------------------
	
	private void updateAPsInJmolViewer()
	{   
		StringBuilder sb = new StringBuilder();
		int arrId = -1;
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
        	sb.append("draw arrow").append(arrId).append(" arrow {");
        	sb.append(startArrow[0]).append(" ");
        	sb.append(startArrow[1]).append(" ");
        	sb.append(startArrow[2]).append("} {");
        	sb.append(endArrow[0]).append(" ");
        	sb.append(endArrow[1]).append(" ");
        	sb.append(endArrow[2]).append("} width 0.1");
        	sb.append(System.getProperty("line.separator"));
        }
        jmolPanel.viewer.evalString(sb.toString());
	}
	
//-----------------------------------------------------------------------------
	
	private class TabRendenrer extends DefaultTableCellRenderer {
	    @Override
	    public Component getTableCellRendererComponent(JTable table, 
	    		Object value, boolean isSelected, boolean hasFocus, int row, 
	    		int column) 
	    {

	        JComponent c = (JComponent) super.getTableCellRendererComponent(table,
	                value, isSelected, hasFocus, row, column);
	        
            Border borderWithMargin = BorderFactory.createLineBorder(Color.BLACK);
	        
	        c.setBorder(borderWithMargin);
	        
			return c;
	    }
	}
	
//-----------------------------------------------------------------------------
}
