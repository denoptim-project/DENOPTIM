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
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListenerProxy;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.io.FilenameUtils;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.fragspace.IdFragmentAndAP;
import denoptim.io.DenoptimIO;
import denoptim.molecule.APClass;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMEdge;
import denoptim.molecule.DENOPTIMFragment;
import denoptim.molecule.DENOPTIMFragment.BBType;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMRing;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.rings.RingClosureParameters;


/**
 * A panel that understands DENOPTIM graphs and allows to create and edit
 * them.
 * 
 * @author Marco Foscato
 */

public class GUIGraphHandler extends GUICardPanel
{
	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 
			-8303012362366503382L;
	
	/**
	 * Unique identified for instances of this handler
	 */
	public static AtomicInteger graphHandlerTabUID = 
			new AtomicInteger(1);
	
	/**
	 * The currently loaded list of graphs
	 */
	private ArrayList<DENOPTIMGraph> dnGraphLibrary =
			new ArrayList<DENOPTIMGraph>();
	
	/**
	 * The currently loaded list of molecular representations 
	 * of the graphs
	 */
	private ArrayList<IAtomContainer> molLibrary =
			new ArrayList<IAtomContainer>();
	
	/**
	 * The unsaved version of the currently loaded graph
	 */
	private DENOPTIMGraph dnGraph;
	
	/**
	 * Unique identified for graphs built here
	 */
	public static AtomicInteger graphUID = new AtomicInteger(1);
	
	/**
	 * The currently loaded graph as GraphStream object
	 */
	private Graph graph;
	
	/**
	 * The snapshot of the old (removed) visualized GraphStrem system. 
	 * Used only to remember stuff like sprites and node positions.
	 */
	private GSGraphSnapshot oldGSStatus;
	
	/**
	 * The index of the currently loaded dnGraph [0–(n-1)}
	 */
	private int currGrphIdx = 0;
	
	/**
	 * Flag signaling that loaded data has changes since last save
	 */
	private boolean unsavedChanges = false;
	
	/**
	 * Flag signaling that there is a fully defined fragment space
	 */
	private boolean hasFragSpace = false;
	
	private JSplitPane centralPane;
	private JSplitPane leftPane;
	private FragmentViewPanel fragViewer;
	private JPanel fragViewerPanel;
	private JPanel fragViewerHeader;
	private JPanel fragViewerCardHolder;
	private JPanel fragViewerEmptyCard;
	private JPanel fragViewerNoFSCard;
	private MoleculeViewPanel molViewer;
	private JPanel molViewerPanel;
	private JPanel molViewerHeader;
	private JPanel molViewerCardHolder;
	private JPanel molViewerEmptyCard;
	private final String NOFSCARDNAME = "noFSCard";
	private final String EMPTYCARDNAME = "emptyCard";
	private final String MOLVIEWERCARDNAME = "molViewerCard";
	private final String FRAGVIEWERCARDNAME = "fragViewerCard";
	private GraphViewerPanel graphViewer;
	private JPanel graphCtrlPane;
	private JPanel graphNavigPane;
	
	private JPanel pnlFragSpace;
	private JTextField txtFragSpace;
	private JButton btnFragSpace; 
	private String loadFSToolTip = "<html>No fragment space loaded.<br>"
			+ "Graphs can be inspected without loading a fragment space.<br>"
			+ "However, loading a fragment space allows: <ul>"
			+ "<li>visualize the molecular fragments linked to each node,</li>"
			+ "<li>edit existing graphs,</li>"
			+ "<li>build graphs manually.</li>"
			+ "</ul></html>";
	
	private JButton btnAddGraph;
	private JButton btnGraphDel;
	
	private JButton btnOpenGraphs;
	
	private JSpinner graphNavigSpinner;
	private JLabel totalGraphsLabel;
	private final GraphSpinnerChangeEvent graphSpinnerListener = 
												new GraphSpinnerChangeEvent();
	
	private JPanel pnlEditVrtxBtns;
	private JButton btnAddVrtx;
	private JButton btnDelSel;
	
	private JPanel pnlShowLabels;
	private JButton btnAddLabel;
	private JButton btnDelLabel;
	private JComboBox<String> cmbLabel;
	
	private JPanel pnlSaveEdits;
	private JButton btnSaveEdits;
	
	/**
	 * Subset of fragments for compatible fragment selecting GUI.
	 * These fragments are clone of those in the fragment library,
	 * and are annotate with fragmentID and AP pointers meant to 
	 * facilitate a quick selection of compatible frag-frag connections.
	 */
	private ArrayList<IAtomContainer> compatFrags;
	
	/**
	 * Map converting fragIDs in fragment library to fragIDs in subset
	 * of compatible fragments
	 */
	private Map<Integer,Integer> genToLocIDMap;
	
//-----------------------------------------------------------------------------
	
	/**
	 * Constructor
	 */
	public GUIGraphHandler(GUIMainPanel mainPanel)
	{
		super(mainPanel, "Graph Handler #" 
					+ graphHandlerTabUID.getAndIncrement());
		super.setLayout(new BorderLayout());
		initialize();
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Initialize the panel and add buttons.
	 */
	private void initialize() 
	{	
		// BorderLayout is needed to allow dynamic resizing!
		this.setLayout(new BorderLayout()); 
		
		// This card structure includes center, east and south panels:
		// - (Center) splitPane with graph and fragment viewers
		// - (East) graph controls
		// - (South) general controls (load, save, close)
		
		// The central pane includes two parts: graph viewer, and mol+frag viewers
		centralPane = new JSplitPane();
		centralPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		centralPane.setOneTouchExpandable(true);
		centralPane.setDividerLocation(200);
		centralPane.setResizeWeight(0.5);
		
		// In the left part of the central pane we have the mol and frag viewers
		leftPane = new JSplitPane();
		leftPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		leftPane.setOneTouchExpandable(true);
		leftPane.setResizeWeight(0.5);
		centralPane.setLeftComponent(leftPane);
		
		graphViewer = new GraphViewerPanel();
		centralPane.setRightComponent(graphViewer);
		graphViewer.addPropertyChangeListener(
				new PropertyChangeListenerProxy(
						"NODECLICKED", new NodeClickedListener()));
		
		fragViewerPanel = new JPanel(new BorderLayout());
		fragViewerHeader = new JPanel();
		fragViewerHeader.add(new JLabel("Node content:"));
		String fragViewerToolTip = "<html>This viewer shows the "
				+ "chemical structure of the fragment contained in a "
				+ "specific node.<br>Click on a node to display its "
				+ "content.</html>";
		fragViewerHeader.setToolTipText(fragViewerToolTip);
		fragViewerPanel.add(fragViewerHeader, BorderLayout.NORTH);
		fragViewerCardHolder = new JPanel(new CardLayout());
		
		fragViewerPanel.add(fragViewerCardHolder, BorderLayout.CENTER);
		
		fragViewerEmptyCard = new JPanel();
		String txt = "<html><body width='%1s'><center>No chosen node.</center></html>";
		fragViewerEmptyCard.add(new JLabel(String.format(txt, 120)));
		fragViewerEmptyCard.setToolTipText(fragViewerToolTip);
		fragViewerCardHolder.add(fragViewerEmptyCard, EMPTYCARDNAME);
		
		fragViewerNoFSCard = new JPanel();
		String txtb = "<html><body width='%1s'><center>To inspect the content "
				+ "of nodes, please load a fragment space.</center></html>";
		fragViewerNoFSCard.add(new JLabel(String.format(txtb, 120)));
		fragViewerNoFSCard.setToolTipText(fragViewerToolTip);
		fragViewerCardHolder.add(fragViewerNoFSCard, NOFSCARDNAME);
		
		fragViewer = new FragmentViewPanel(false);
		fragViewerCardHolder.add(fragViewer, FRAGVIEWERCARDNAME);
		
		((CardLayout) fragViewerCardHolder.getLayout()).show(
				fragViewerCardHolder, NOFSCARDNAME);
		
		leftPane.setTopComponent(fragViewerPanel);
		
		
		// The molecular viewer is embedded in a container structure that 
		// is meant to show/hide the molViewer according to specific needs.
		molViewerPanel = new JPanel(new BorderLayout());
		molViewerHeader = new JPanel();
		molViewerHeader.add(new JLabel("Associated Structure:"));
		String molViewerToolTip = "<html>This viewer shows the chemical "
				+ "structure associated with the current graph.</html>";
		molViewerHeader.setToolTipText(molViewerToolTip);
		molViewerPanel.add(molViewerHeader, BorderLayout.NORTH);
		molViewerCardHolder = new JPanel(new CardLayout());
		
		molViewerPanel.add(molViewerCardHolder, BorderLayout.CENTER);
		
		molViewerEmptyCard = new JPanel();
		String txt2 = "<html><body width='%1s'><center>No chemical "
				+ "structure.</center>"
				+ "</html>";
		molViewerEmptyCard.add(new JLabel(String.format(txt2, 120)));
		molViewerEmptyCard.setToolTipText(molViewerToolTip);
		molViewerCardHolder.add(molViewerEmptyCard, EMPTYCARDNAME);
		
		molViewer = new MoleculeViewPanel();
		molViewer.enablePartialData(true);
		molViewerCardHolder.add(molViewer, MOLVIEWERCARDNAME);
		((CardLayout) molViewerCardHolder.getLayout()).show(
				molViewerCardHolder, EMPTYCARDNAME);
		
		leftPane.setBottomComponent(molViewerPanel);
		
		this.add(centralPane,BorderLayout.CENTER);
       
		// General panel on the right: it containing all controls
        graphCtrlPane = new JPanel();
        graphCtrlPane.setVisible(true);
        graphCtrlPane.setLayout(new BoxLayout(graphCtrlPane, 
        		SwingConstants.VERTICAL));

        // Controls to navigate the list of dnGraphs
        graphNavigPane = new JPanel();
        JLabel navigationLabel1 = new JLabel("Graph # ");
        JLabel navigationLabel2 = new JLabel("Current library size: ");
        totalGraphsLabel = new JLabel("0");
        
		graphNavigSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 0, 1));
		graphNavigSpinner.setToolTipText("Move to graph number # in the "
				+ "currently loaded library.");
		graphNavigSpinner.setMaximumSize(new Dimension(75,20));
		graphNavigSpinner.addChangeListener(graphSpinnerListener);
        
		btnAddGraph = new JButton("Add");
		btnAddGraph.setToolTipText("Append a graph to the currently loaded "
				+ "list of graphs.");
		btnAddGraph.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String[] options = new String[]{"Build", "File", "Cancel"};
				int res = JOptionPane.showOptionDialog(null,
		                "<html>Please choose wherther to start creations "
		                + "of a new graph, or import graph from file.</html>",
		                "Specify source of new graph",
		                JOptionPane.DEFAULT_OPTION,
		                JOptionPane.QUESTION_MESSAGE,
		                UIManager.getIcon("OptionPane.warningIcon"),
		                options,
		                options[2]);
				switch (res)
				{
					case 0:
						if (!hasFragSpace)
						{
							JOptionPane.showMessageDialog(null,
					                "<html>No fragment space is currently "
					                + "loaded!<br>"
					                + "You must load a fragment space to build"
					                + " graphs.</html>",
					                "Error",
					                JOptionPane.ERROR_MESSAGE,
					                UIManager.getIcon("OptionPane.errorIcon"));
							return;
						}
						startGraphFromFragSpace();
						break;
					
					case 1:
						File inFile = DenoptimGUIFileOpener.pickFile();
						if (inFile == null 
								|| inFile.getAbsolutePath().equals(""))
						{
							return;
						}
						appendGraphsFromFile(inFile);
						break;
				}
			}
		});
		btnGraphDel = new JButton("Remove");
		btnGraphDel.setToolTipText("<html>Remove the present graph from the "
				+ "library.<br><br><b>WARNING:</b> this action cannot be "
				+ "undone!</html>");
		btnGraphDel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					removeCurrentdnGraph();
				} catch (DENOPTIMException e1) {
					System.out.println("Exception while removing the current "
							+ "graph:");
					e1.printStackTrace();
				}
			}
		});
		
        GroupLayout lyoAddDetGraphs = new GroupLayout(graphNavigPane);
        graphNavigPane.setLayout(lyoAddDetGraphs);
        lyoAddDetGraphs.setAutoCreateGaps(true);
        lyoAddDetGraphs.setAutoCreateContainerGaps(true);
        lyoAddDetGraphs.setHorizontalGroup(lyoAddDetGraphs.createParallelGroup(
                        		GroupLayout.Alignment.CENTER)
                        .addGroup(lyoAddDetGraphs.createSequentialGroup()
                                        .addComponent(navigationLabel1)
                                        .addComponent(graphNavigSpinner))
                        .addGroup(lyoAddDetGraphs.createSequentialGroup()
                                        .addComponent(navigationLabel2)
                                        .addComponent(totalGraphsLabel))
                        .addGroup(lyoAddDetGraphs.createSequentialGroup()
                                        .addComponent(btnAddGraph)
                                        .addComponent(btnGraphDel)));
        lyoAddDetGraphs.setVerticalGroup(lyoAddDetGraphs.createSequentialGroup()
                        .addGroup(lyoAddDetGraphs.createParallelGroup(
                        		GroupLayout.Alignment.CENTER)
                                        .addComponent(navigationLabel1)
                                        .addComponent(graphNavigSpinner))
                        .addGroup(lyoAddDetGraphs.createParallelGroup()
                                        .addComponent(navigationLabel2)
                                        .addComponent(totalGraphsLabel))
                        .addGroup(lyoAddDetGraphs.createParallelGroup()
                                        .addComponent(btnAddGraph)
                                        .addComponent(btnGraphDel)));
		graphCtrlPane.add(graphNavigPane);
		
		graphCtrlPane.add(new JSeparator());
		
		// Fragment Space
		if (FragmentSpace.isDefined())
		{
			hasFragSpace = true;
		}
		
		txtFragSpace = new JTextField();
		txtFragSpace.setHorizontalAlignment(JTextField.CENTER);
		if (!hasFragSpace)
		{
			renderForLackOfFragSpace();
		}
		else
		{
			renderForPresenceOfFragSpace();
		}
		txtFragSpace.setEditable(false);
		
		btnFragSpace = new JButton("Load Fragment Space");
		btnFragSpace.setToolTipText(loadFSToolTip);
		btnFragSpace.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean showWarning = false;
				String msg = "<html><body width='%1s'>"
						+ "<b>WARNING</b>: you are introducing a "
						+ "potential source of mistmatch between "
						+ "the fragments IDs used in graphs and the "
						+ "fragment space.<br>In particular:<br>"
						+ "<ul>";
				
				if (dnGraphLibrary.size() != 0)
				{
					msg = msg 
							+ "<li>One or more graphs are already loaded.</li>";
					showWarning = true;
				}
				if (hasFragSpace)
				{
					msg = msg + "<li>A fragment space is alredy loaded.</li>";
					showWarning = true;
				}
				if (showWarning)
				{
					msg = msg + "</ul>"
							+ ""
			                + "Are you sure you want to load a fragment "
			                + "space? </html>";
					String[] options = new String[]{"Yes", "No"};
					int res = JOptionPane.showOptionDialog(null,
							String.format(msg,350),			            
			                "Change frgment space?",
			                JOptionPane.DEFAULT_OPTION,
			                JOptionPane.WARNING_MESSAGE,
			                UIManager.getIcon("OptionPane.warningIcon"),
			                options,
			                options[1]);
					if (res == 1)
					{
						return;
					}
				}
				loadFragmentSpace();
			}
		});
		pnlFragSpace = new JPanel();
        GroupLayout lyoFragSpace = new GroupLayout(pnlFragSpace);
        pnlFragSpace.setLayout(lyoFragSpace);
        lyoFragSpace.setAutoCreateGaps(true);
        lyoFragSpace.setAutoCreateContainerGaps(true);
        lyoFragSpace.setHorizontalGroup(lyoFragSpace.createParallelGroup(
                                        GroupLayout.Alignment.CENTER)
                        .addComponent(btnFragSpace)
                        .addComponent(txtFragSpace));
        lyoFragSpace.setVerticalGroup(lyoFragSpace.createSequentialGroup()
		                .addComponent(btnFragSpace)
		                .addComponent(txtFragSpace));
        graphCtrlPane.add(pnlFragSpace);
		
		graphCtrlPane.add(new JSeparator());
		
		// Controls to alter the presently loaded graph (if any)
		pnlEditVrtxBtns = new JPanel();
		JLabel edtVertxsLab = new JLabel("Edit verteces:");
		btnAddVrtx = new JButton("Add");
		btnAddVrtx.setToolTipText("<html>Append a vertex to the selected "
				+ "attachment point<html>");
		btnAddVrtx.setEnabled(false);
		btnAddVrtx.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {								
				ArrayList<IdFragmentAndAP> selAps = getAPsSelectedInViewer();				
				if (selAps.size() == 0)
				{
					JOptionPane.showMessageDialog(null,
			                "<html>No attachment point selected!<br> Drag the "
			                + "mouse to select APs.<br> "
			                + "Click again to unselect.</html>",
			                "Error",
			                JOptionPane.ERROR_MESSAGE,
			                UIManager.getIcon("OptionPane.errorIcon"));
					return;
				}
				else
				{
					extendGraphFromFragSpace(selAps);
					
					// Update viewer
					loadDnGraphToViewer();
					
					// Protect edited system
			        unsavedChanges = true;
			        protectEditedSystem();
				}
			}
		});
		
		btnDelSel = new JButton("Remove");
		btnDelSel.setToolTipText("<html>Removes all selected vertexes from "
				+ "the system.<br><br><b>WARNING:</b> this action cannot be "
				+ "undone!</html>");
		btnDelSel.setEnabled(false);
		btnDelSel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ArrayList<DENOPTIMVertex> selVrtx = getSelectedNodesInViewer();
				if (selVrtx.size() == 0)
				{
					JOptionPane.showMessageDialog(null,
							"<html>No vertex selected! Drag the "
			                + "mouse to select vertexes."
					        + "<br>Click on background to unselect.</html>",
			                "Error",
			                JOptionPane.ERROR_MESSAGE,
			                UIManager.getIcon("OptionPane.errorIcon"));
					return;
				}
				else
				{				
					for (DENOPTIMVertex v : selVrtx)
					{
						dnGraph.removeVertex(v);
					}
					
					// Update viewer
					loadDnGraphToViewer();
					
			        // Protect the temporary "dnGraph" obj
			        unsavedChanges = true;
			        protectEditedSystem();
				}
			}
		});
		
		GroupLayout lyoEditVertxs = new GroupLayout(pnlEditVrtxBtns);
		pnlEditVrtxBtns.setLayout(lyoEditVertxs);
		lyoEditVertxs.setAutoCreateGaps(true);
		lyoEditVertxs.setAutoCreateContainerGaps(true);
		lyoEditVertxs.setHorizontalGroup(lyoEditVertxs.createParallelGroup(
				GroupLayout.Alignment.CENTER)
				.addComponent(edtVertxsLab)
				.addGroup(lyoEditVertxs.createSequentialGroup()
						.addComponent(btnAddVrtx)
						.addComponent(btnDelSel)));
		lyoEditVertxs.setVerticalGroup(lyoEditVertxs.createSequentialGroup()
				.addComponent(edtVertxsLab)
				.addGroup(lyoEditVertxs.createParallelGroup()
						.addComponent(btnAddVrtx)
						.addComponent(btnDelSel)));
		graphCtrlPane.add(pnlEditVrtxBtns);
		
		graphCtrlPane.add(new JSeparator());
		
		// Controls of displayed attributes
		pnlShowLabels = new JPanel();
		JLabel lblShowHideLabels = new JLabel("Manage graph labels:");
		cmbLabel = new JComboBox<String>(
				new String[] {graphViewer.SPRITE_APCLASS, 
				graphViewer.SPRITE_BNDORD, graphViewer.SPRITE_FRGID});
		cmbLabel.setToolTipText("<html>Select the kind of type of information"
				+ "<br>to add or remove from the graph view.</html>");
		cmbLabel.setEnabled(false);
		btnAddLabel = new JButton("Show");
		btnAddLabel.setToolTipText("<html>Shows the chosen label for the "
				+ "<br>selected elements.</html>");
		btnAddLabel.setEnabled(false);
		btnAddLabel.addActionListener(new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				if (graphViewer.hasSelected())
				{
					graphViewer.appendSprites(
							cmbLabel.getSelectedItem().toString());
				}
				else
				{
					JOptionPane.showMessageDialog(null,
							"<html>No elements selected! Drag the "
			                + "mouse to select elements."
					        + "<br>Click on background to unselect.</html>",
			                "Error",
			                JOptionPane.ERROR_MESSAGE,
			                UIManager.getIcon("OptionPane.errorIcon"));
				}
			}
		});
		btnDelLabel = new JButton("Hide");
		btnDelLabel.setToolTipText("<html>Hides the chosen label for the "
				+ "<br>selected elements.</html>");
		btnDelLabel.setEnabled(false);
		btnDelLabel.addActionListener(new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				if (graphViewer.hasSelected())
				{
					graphViewer.removeSprites(
							cmbLabel.getSelectedItem().toString());
				}
				else
				{
					JOptionPane.showMessageDialog(null,
							"<html>No elements selected! Drag the "
			                + "mouse to select elements."
					        + "<br>Click on background to unselect.</html>",
			                "Error",
			                JOptionPane.ERROR_MESSAGE,
			                UIManager.getIcon("OptionPane.errorIcon"));
				}
			}
		});
		
        GroupLayout lyoShowAttr = new GroupLayout(pnlShowLabels);
        pnlShowLabels.setLayout(lyoShowAttr);
        lyoShowAttr.setAutoCreateGaps(true);
        lyoShowAttr.setAutoCreateContainerGaps(true);
        lyoShowAttr.setHorizontalGroup(lyoShowAttr.createParallelGroup(
                        GroupLayout.Alignment.CENTER)
                        .addComponent(lblShowHideLabels)
                        .addComponent(cmbLabel)
                        .addGroup(lyoShowAttr.createSequentialGroup()
	                        .addComponent(btnAddLabel)
	                        .addComponent(btnDelLabel)));
        lyoShowAttr.setVerticalGroup(lyoShowAttr.createSequentialGroup()
		                .addComponent(lblShowHideLabels)
		                .addComponent(cmbLabel)
		                .addGroup(lyoShowAttr.createParallelGroup()
		                        .addComponent(btnAddLabel)
		                        .addComponent(btnDelLabel)));
        graphCtrlPane.add(pnlShowLabels);
        
        graphCtrlPane.add(new JSeparator());
		
		// Control for unsaved changes
        pnlSaveEdits = new JPanel();
        btnSaveEdits = new JButton("Save Changes");
        btnSaveEdits.setForeground(Color.RED);
        btnSaveEdits.setEnabled(false);
        btnSaveEdits.setToolTipText("<html>Save the current graph replacing"
        	+ " <br>the original one in the currently loaded library.</html>");
        btnSaveEdits.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                	saveUnsavedChanges();
                }
        });
        pnlSaveEdits.add(btnSaveEdits);
        graphCtrlPane.add(pnlSaveEdits);
		this.add(graphCtrlPane,BorderLayout.EAST);
		
		// Panel with buttons to the bottom of the frame
		JPanel commandsPane = new JPanel();
		super.add(commandsPane, BorderLayout.SOUTH);
		
		btnOpenGraphs = new JButton("Load Library of Graphs",
					UIManager.getIcon("FileView.directoryIcon"));
		btnOpenGraphs.setToolTipText("Reads graphs or structures from file.");
		btnOpenGraphs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File inFile = DenoptimGUIFileOpener.pickFile();
				if (inFile == null || inFile.getAbsolutePath().equals(""))
				{
					return;
				}
				importGraphsFromFile(inFile);
			}
		});
		commandsPane.add(btnOpenGraphs);
		
		JButton btnSaveFrags = new JButton("Save Library of Graphs",
				UIManager.getIcon("FileView.hardDriveIcon"));
		btnSaveFrags.setToolTipText("Write all graphs to a file.");
		btnSaveFrags.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File outFile = DenoptimGUIFileOpener.saveFile();
				if (outFile == null)
				{
					return;
				}
				try
				{
					DenoptimIO.writeGraphsToFile(outFile.getAbsolutePath(),
							dnGraphLibrary, false);
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
		btnCanc.setToolTipText("Closes this graph handler.");
		btnCanc.addActionListener(new removeCardActionListener(this));
		commandsPane.add(btnCanc);
		
		JButton btnHelp = new JButton("?");
		btnHelp.setToolTipText("<html>Hover over the buttons and fields "
                    + "to get a tip.</html>");
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String txt = "<html><body width='%1s'>"
						+ "<p>This tab allows to create, inspect, and edit "
						+ "DENOPTIM's graph representation of chemical "
						+ "objects (i.e., DENOPTIMGraph).</p>"
						+ "<br>"
						+ "<p>In general, you can hover over any button or"
						+ "viewer to get a tip on its usage.</p>"
						+ "<br>"
						+ "<p>DENOPTIMGraphs is drawn in the "
						+ "central panel (i.e., the graph viewer). "
						+ "Each vertex is shown as a rounded square, and each "
						+ "edge as an arrow (or a line, for undirected edges)."
						+ " The color code identified the type of fragment "
						+ "contained in a node:<ul>"
						+ "<li>red for the scaffold,</li>"
						+ "<li>orange for ring-closing vertexes,</li>"
						+ "<li>green for capping groups,</li>"
						+ "<li>blue for standard fragments.</li>"
						+ "</ul></p>"
						+ "<p>If the loaded DENOPTIMGraph is associated with "
						+ "a chemical structure, the latter is shown in the "
						+ "molecular viewer (bottom-left panel).</p>"
						+ "<p>The molecular fragment contained in a node is "
						+ "shown in the fragment viewer (top-left panel) upon "
						+ "clicking on that node in the graph viewer. "
						+ "A fragment space must be loaded in order to "
						+ "inspect the chemical structure of "
						+ "fragments, and also to build new DENOPTIMGraphs"
						+ " from manually.</p>"
						+ "<br>"
						+ "<p><b>Control the graph view</b></p>"
						+ "<p>Move the mouse up/down while holding the "
						+ "<code>ctrl</code> key to zoom in/out.</p>"
						+ "<p>Move the mouse left/right while holding the "
						+ "<code>Alt</code> key to pan the view.</p>"
						+ "<br>"
						+ "<p><b>Control the fragment and molecular views</b>"
						+ "</p>"
						+ "<p>Right-click on the Jmol viewer will open the "
						+ "Jmol menu. However, any change on the molecular "
						+ "object will not be saved in the "
						+ "fragment or in the chemical structure associated"
						+ " with the graph.</p></html>";
				JOptionPane.showMessageDialog(null, 
						String.format(txt, 450),
	                    "Tips",
	                    JOptionPane.PLAIN_MESSAGE);
			}
		});
		commandsPane.add(btnHelp);
		
		//TODO del (This is used only for devel phase of debug)
		/*
		try {
			ArrayList<String> lines = DenoptimIO.readList("/Users/mfo051/___/_fs.params");
			for (String l : lines)
			{
			    FragmentSpaceParameters.interpretKeyword(l);
			}
			FragmentSpaceParameters.processParameters();
			renderForPresenceOfFragSpace();
		} catch (DENOPTIMException e1) {
			e1.printStackTrace();
		}
		*/
		
	}
	
//-----------------------------------------------------------------------------
	
	private void enableGraphDependentButtons(boolean enable)
	{
		btnAddVrtx.setEnabled(enable);
		btnDelSel.setEnabled(enable);
		cmbLabel.setEnabled(enable);
		btnAddLabel.setEnabled(enable);
		btnDelLabel.setEnabled(enable);
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Identifies which APs are selected in the graph viewer.
	 * @return the list of identifiers
	 */
	private ArrayList<IdFragmentAndAP> getAPsSelectedInViewer()
	{
		ArrayList<IdFragmentAndAP> allIDs = new ArrayList<IdFragmentAndAP>();
		for (Node n : graphViewer.getSelectedNodes())
		{
			if (!n.getAttribute("ui.class").equals("ap"))
			{
				continue;
			}
			int vId = n.getAttribute("dnp.srcVrtId");
			int apId = n.getAttribute("dnp.srcVrtApId");
			
			IdFragmentAndAP id = null;
			if (hasFragSpace)
			{
				DENOPTIMVertex v = dnGraph.getVertexWithId(vId);
				id = new IdFragmentAndAP(vId,
										 v.getMolId(),
										 v.getFragmentType(),
										 apId,-99,-99);
			}
			else
			{
				id = new IdFragmentAndAP(vId,-99,BBType.UNDEFINED,apId,-99,-99);
			}
			
			allIDs.add(id);
		}
		return allIDs;
	}
	
//-----------------------------------------------------------------------------
	
	private ArrayList<DENOPTIMVertex> getSelectedNodesInViewer()
	{
		ArrayList<DENOPTIMVertex> selected = new ArrayList<DENOPTIMVertex>();
		for (Node n : graphViewer.getSelectedNodes())
		{
			if (n.getAttribute("ui.class").equals("ap"))
			{
				continue;
			}
			int vId = Integer.parseInt(n.getAttribute("dnp.VrtId"));
			selected.add(dnGraph.getVertexWithId(vId));
		}	
		return selected; 
	}

//-----------------------------------------------------------------------------

	/**
	 * Start the construction of a new graph from scratch
	 */
	private void startGraphFromFragSpace()
	{
		ArrayList<IAtomContainer> fragLib = new  ArrayList<IAtomContainer>();
        for (DENOPTIMVertex bb : FragmentSpace.getScaffoldLibrary())
        {
        	if (bb instanceof DENOPTIMFragment)
        	{
        		fragLib.add(((DENOPTIMFragment) bb).getIAtomContainer());
        	} else
        	{
        		//TODO deal with templates and other stuff
        		fragLib.add(new AtomContainer());
        	}
        }
		if (fragLib.size() == 0)
		{
			JOptionPane.showMessageDialog(null,"No fragments in the library",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;
		}
		
		// Select the scaffold
		GUIFragmentSelector fragSelector = new GUIFragmentSelector(fragLib);
		fragSelector.setRequireApSelection(false);
		Object selected = fragSelector.showDialog();
		if (selected == null)
		{
			return;
		}
		int scaffFragId = ((Integer[]) selected)[0];
		
		// Create the new graph
		currGrphIdx = dnGraphLibrary.size();
		dnGraph = new DENOPTIMGraph();
		dnGraph.setGraphId(graphUID.getAndIncrement());
		graph = null;
		
		// Add new graph and corresponding mol representation
		dnGraphLibrary.add(dnGraph);
		//NB: we add an empty molecular representation to keep the list
		// of graphs and that of mol.rep. in sync
		molLibrary.add(new AtomContainer());
		
		// Since there is no molecular representation in it, we cleanup
		// the mol viewer and replace it with the placeholder
		molViewer.clearAll();
		((CardLayout) molViewerCardHolder.getLayout()).show(molViewerCardHolder, EMPTYCARDNAME);
		
		
		updateGraphListSpinner();

		
		// Create the node
		int scaffVrtId = 1;
		DENOPTIMVertex scaffVertex = DENOPTIMVertex.newVertexFromLibrary(
		        scaffVrtId, scaffFragId, BBType.SCAFFOLD);

		scaffVertex.setLevel(-1); //NB: scaffold gets level -1
		dnGraph.addVertex(scaffVertex);
		
		// Put the graph to the viewer
		loadDnGraphToViewer();
		enableGraphDependentButtons(true);
		unsavedChanges = true;
        protectEditedSystem();
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Extends the current graph by appending a node to a specific free AP on 
	 * the growing graph. 
	 * This method will prompt a question on which incoming fragment to append 
	 * @param srcAPs list of identifiers for APs on the growing graph.
	 */
	private void extendGraphFromFragSpace(ArrayList<IdFragmentAndAP> srcAPs)
	{
		// For extensions of existing graphs we need to know where to extend
		if (srcAPs.size() == 0)
		{
			JOptionPane.showMessageDialog(null,"No AP selected in the "
					+ "graph.",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;
		}
		
		// Create clones of fragments and put the into 'compatFrags'
		collectFragAndAPsCompatibleWithSelectedAPs(srcAPs);
		
		BBType trgFrgType = BBType.UNDEFINED;
		ArrayList<IAtomContainer> fragLib = new ArrayList<IAtomContainer>();		
		String[] options = new String[]{"Any Fragment",
				"Compatible Fragments ("+compatFrags.size()+")",
				"Capping group"};
		int res = JOptionPane.showOptionDialog(null,
                "<html>Choose a subset of possible fragments:</html>",
                "Choose fragment subset",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                UIManager.getIcon("OptionPane.warningIcon"),
                options,
                options[0]);

        //TODO-V3 deal with non-fragment vertexes
        // Templates do not fit 
        // within the concept of capping group, so this should 
        // never happen for templates. Yet, other things than 
        // templates might need code here
		
		switch (res)
		{
			case 0:
				fragLib = new  ArrayList<IAtomContainer>();
		        for (DENOPTIMVertex bb : FragmentSpace.getFragmentLibrary())
		        {
		        	if (bb instanceof DENOPTIMFragment)
		        	{
		        		fragLib.add(((DENOPTIMFragment) bb).getIAtomContainer());
		        	} else
		        	{
		        		//TODO deal with templates and other stuff
		        		fragLib.add(new AtomContainer());
		        	}
		        }
				trgFrgType = BBType.FRAGMENT;
				break;
			case 1:
				fragLib = compatFrags;
				trgFrgType = BBType.FRAGMENT;
				break;
			case 2:
				fragLib = new ArrayList<IAtomContainer>();
		        for (DENOPTIMVertex bb : FragmentSpace.getCappingLibrary())
		        {
		        	if (bb instanceof DENOPTIMFragment)
		        	{
		        		fragLib.add(((DENOPTIMFragment)bb).getIAtomContainer());
		        	} else
		        	{
		        		fragLib.add(new AtomContainer());
		        	}
		        }
				trgFrgType = BBType.CAP;
				break;
			default:
				return;
		}

		if (fragLib.size() == 0)
		{
			JOptionPane.showMessageDialog(null,"No fragments in the library",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;
		}
		
		// Select the incoming fragment and its AP to use
		GUIFragmentSelector fragSelector = new GUIFragmentSelector(fragLib);
		fragSelector.setRequireApSelection(true);
		Object selected = fragSelector.showDialog();
		if (selected == null)
		{
			return;
		}
		int trgFragId = ((Integer[]) selected)[0];
		if (res==1)
		{
			// When we are selecting among a subset we need to convert the
			// trgFragId into a value that corresponds to the fragId in
			// the global fragment library
			
			for (int candFragId : genToLocIDMap.keySet())
			{
				if (genToLocIDMap.get(candFragId)==trgFragId)
				{
					trgFragId = candFragId;
					break;
				}
			}
		}
		int trgApId = ((Integer[]) selected)[1];
		
		// Take the graph that will be extended
		dnGraph = dnGraphLibrary.get(currGrphIdx);
		graph = null;
		
		// Append the new nodes
		for (int i=0; i<srcAPs.size(); i++)
		{
			IdFragmentAndAP ids = srcAPs.get(i);
			int srcVertexId = ids.getVertexId();
			int srcApId = ids.getApId();
			
			int trgVrtId = dnGraph.getMaxVertexId()+1;
			
			DENOPTIMVertex trgVertex = DENOPTIMVertex.newVertexFromLibrary(
			       trgVrtId, trgFragId, trgFrgType);
	
			// Identify the source vertex/node and its AP
			DENOPTIMVertex srcVertex = dnGraph.getVertexWithId(srcVertexId);
				
			APClass sCls = srcVertex.getAttachmentPoints().get(srcApId).getAPClass();
			APClass tCls = trgVertex.getAttachmentPoints().get(trgApId).getAPClass();
				
			trgVertex.setLevel(srcVertex.getLevel() + 1);
				
			//NB: we ignore symmetry here
	                
	        DENOPTIMEdge edge = srcVertex.connectVertices(
	        		trgVertex, srcApId, trgApId, sCls, tCls
			);
	
	        if (edge == null)
	        {
	        	JOptionPane.showMessageDialog(null,"Unable to make new edge.",
	    	                "Error",
	    	                JOptionPane.PLAIN_MESSAGE,
	    	                UIManager.getIcon("OptionPane.errorIcon"));
	    		return;
	        }
	
	        dnGraph.addVertex(trgVertex);
	        dnGraph.addEdge(edge);
		}
	}

//-----------------------------------------------------------------------------
	
	private void collectFragAndAPsCompatibleWithSelectedAPs(
			ArrayList<IdFragmentAndAP> srcAPs) 
	{
		compatFrags = new ArrayList<IAtomContainer>();
		
		// WARNING: here I re-do most of what is already done in
		// FragmentSpace.getFragmentsCompatibleWithTheseAPs.
		// However, here we add additional data to (clones) of the 
		// fragments, so that I can easily highlight the compatible APs in 
		// the selection GUI.
		
    	// First we get all possible APs on any fragment
    	ArrayList<IdFragmentAndAP> compatFragAps = 
				FragmentSpace.getFragAPsCompatibleWithTheseAPs(srcAPs);
    	
    	// then keep unique fragment identifiers, and store unique
		genToLocIDMap = new HashMap<Integer,Integer>();
		
		String PRESELPROP = GUIFragmentSelector.PRESELECTEDAPSFIELD;
		String PRESELPROPSEP = GUIFragmentSelector.PRESELECTEDAPSFIELDSEP;
		
		for (IdFragmentAndAP frgApId : compatFragAps)
		{
			int fragId = frgApId.getVertexMolId();
			int apId = frgApId.getApId();
			if (genToLocIDMap.keySet().contains(fragId))
			{
				IAtomContainer frg = compatFrags.get(
						genToLocIDMap.get(fragId));
				String prop = frg.getProperty(PRESELPROP).toString();
				frg.setProperty(PRESELPROP,prop+PRESELPROPSEP+apId);
			}
			else
			{
				IAtomContainer frg = null;
				try
				{
					DENOPTIMVertex bb = FragmentSpace.getVertexFromLibrary(
					        BBType.FRAGMENT,fragId);
					if (bb instanceof DENOPTIMFragment)
					{
						frg = ((DENOPTIMFragment) bb).getIAtomContainer();
					} else {
						//TODO deal with templates
						frg = new AtomContainer();
					}
					frg.setProperty(PRESELPROP,apId);
				}
				catch (Throwable t)
				{
					continue;
				}
				genToLocIDMap.put(fragId,compatFrags.size());
				compatFrags.add(frg);
			}
		}
	}
	
//-----------------------------------------------------------------------------

	protected void renderForLackOfFragSpace() 
	{
		hasFragSpace = false;
		txtFragSpace.setText("No fragment space");
		txtFragSpace.setToolTipText(loadFSToolTip);
		txtFragSpace.setBackground(Color.ORANGE);
	}
	
//-----------------------------------------------------------------------------

	protected void renderForPresenceOfFragSpace() 
	{
		hasFragSpace = true;
		txtFragSpace.setText("Fragment space loaded");
		txtFragSpace.setToolTipText("<html>A fragment space has been loaded "
				+ "previously<br>and is ready to use. You can change the "
				+ "fragment space<br> by loading another one, but be aware "
				+ "of any dependency from<br>currently loaded graphs.</html>");
		txtFragSpace.setBackground(Color.decode("#4cc253"));
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Imports graphs from file. 
	 * @param file the file to open
	 */

	public void importGraphsFromFile(File file)
	{	
		dnGraphLibrary = readGraphsFromFile(file);
		
		try {
			molLibrary = DenoptimIO.readMoleculeData(
					file.getAbsolutePath());
		} catch (DENOPTIMException e) {
			System.out.println("Could not read molecules from " + file);
			for (int i=0; i<dnGraphLibrary.size(); i++)
			{
				molLibrary.add(new AtomContainer());
			}
		}
			
		// Display the first
		currGrphIdx = 0;
		
		loadCurrentGraphIdxToViewer();
		updateGraphListSpinner();
	}

//-----------------------------------------------------------------------------

	private void appendGraphsFromFile(File file)
	{
		ArrayList<DENOPTIMGraph> graphs = readGraphsFromFile(file);
		int oldSize = dnGraphLibrary.size();
		if (graphs.size() > 0)
		{
			dnGraphLibrary.addAll(graphs);
			
			try {
				molLibrary.addAll(DenoptimIO.readMoleculeData(
						file.getAbsolutePath()));
			} catch (DENOPTIMException e) {
				System.out.println("Could not read molecules from " + file);
				for (int i=0; i<graphs.size(); i++)
				{
					molLibrary.add(new AtomContainer());
				}
			}
			
			// Display the first of the imported ones
			currGrphIdx = oldSize;
			
			loadCurrentGraphIdxToViewer();
			updateGraphListSpinner();
		}
	}
	
//-----------------------------------------------------------------------------

	private ArrayList<DENOPTIMGraph> readGraphsFromFile(File file)
	{
		//TODO change: this should be done elsewhere, maybe in DenoptimIO
		
		String format="";
		String ext = FilenameUtils.getExtension(file.getAbsolutePath());
		switch (ext.toUpperCase())
		{
			case ("SDF"):
				format="SDF";
				break;
				
			case ("TXT"):
				format="TXT";
				break;
			
			case ("SER"):
				format="SER";
				break;
				
			default:
				String[] options = {"Abandon", "TXT", "SDF", "SERIALIZED"};
				int res = JOptionPane.showOptionDialog(null,
					"<html>Failed to detect file type from file's "
					+ "extension.<br>"
					+ "Please, tell me how to interpret file <br>"
					+ "'" + file.getAbsolutePath() + "'<br>"
					+ "or 'Abandon' to give up.</html>",
					"Specify File Type",
	                JOptionPane.DEFAULT_OPTION,
	                JOptionPane.QUESTION_MESSAGE,
	                UIManager.getIcon("OptionPane.warningIcon"),
	                options,
	                options[0]);
				switch (res)
				{
					case 0:
						return new ArrayList<DENOPTIMGraph>();
						
					case 1:
						format = "TXT";
						break;
						
					case 2:
						format="SDF";
						break;
						
					case 3:
						format="SER";
						break;
				}
				break;
		}
		
		ArrayList<DENOPTIMGraph> graphs = new ArrayList<DENOPTIMGraph>();
		try 
		{
			graphs = DenoptimIO.readDENOPTIMGraphsFromFile(
					file.getAbsolutePath(), format, hasFragSpace);	
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
			String msg = "<html>Could not read graph from file <br> "
					+ "'" + file.getAbsolutePath() 
	                + "'<br>Hint on cause: ";
			msg = msg + e.getClass().getName()+ " (";
			if (e.getCause() != null)
			{
				msg = msg + e.getCause();
			}
			if (e.getMessage() != null)
			{
				msg = msg + " " + e.getMessage();
			}
			msg = msg + ")";
			if (hasFragSpace)
			{
				msg = msg + "<br>This could be due to a mistmatch between "
						+ "the fragment IDs in the<br>"
						+ "graph you are trying to load, "
						+ "and the currently loaded fragment space.<br>"
						+ "Aborting import of graphs.";
			}
			msg = msg + "</html>";
			JOptionPane.showMessageDialog(null,msg,
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
		}
		return graphs;
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Loads the graph corresponding to the field {@link #currGrphIdx}
	 */
	private void loadCurrentGraphIdxToViewer()
	{
		if (dnGraphLibrary == null)
		{
			JOptionPane.showMessageDialog(null,
	                "No list of graphs loaded.",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;
		}
		
		// Clears the "dnGraph" and GUI components, but keep memory of the 
    	// status of the graph of an easy recovery, (see GSGraphSnapshot)
    	clearCurrentSystem();
    	
		dnGraph = dnGraphLibrary.get(currGrphIdx);
		loadDnGraphToViewer();
		
		if (molLibrary.get(currGrphIdx).getAtomCount() > 0)
		{
		    try {
				molViewer.loadChemicalStructure(molLibrary.get(currGrphIdx));
				((CardLayout) molViewerCardHolder.getLayout()).show(
						molViewerCardHolder, MOLVIEWERCARDNAME);
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Could not read molecular data: "+
						e.getCause() + " " + e.getMessage());
				((CardLayout) molViewerCardHolder.getLayout()).show(
						molViewerCardHolder, EMPTYCARDNAME);
			}
		}
		else
		{
			((CardLayout) molViewerCardHolder.getLayout()).show(
					molViewerCardHolder, EMPTYCARDNAME);
		}
		
		enableGraphDependentButtons(true);
	}
	
//-----------------------------------------------------------------------------
	
	private void loadDnGraphToViewer()
	{
		if (fragViewer != null)
		{
			fragViewer.clearAll();
			((CardLayout) fragViewerCardHolder.getLayout()).show(
					fragViewerCardHolder, EMPTYCARDNAME);
		}
		graph = convertDnGraphToGSGraph(dnGraph);
		
		// Keep a snapshot of the old data visualized
		oldGSStatus = graphViewer.getStatusSnapshot();
		
		graphViewer.cleanup();
		graphViewer.loadGraphToViewer(graph,oldGSStatus);
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Created a graph object suitable for GraphStrem viewer from a 
	 * DENOPTIMGraph.
	 * @param dnG the graph to be converted
	 * @return the GraphStream object
	 */
	private Graph convertDnGraphToGSGraph(DENOPTIMGraph dnG) 
	{
		graph = new SingleGraph("DENOPTIMGraph#"+dnG.getGraphId());
		
		for (DENOPTIMVertex v : dnG.getVertexList())
		{
			// Create representation of this vertex
			String vID = Integer.toString(v.getVertexId());
			
			Node n = graph.addNode(vID);
			n.addAttribute("ui.label", vID);
			n.addAttribute("dnp.VrtId", vID);
			n.setAttribute("dnp.molID", v.getMolId());
			n.setAttribute("dnp.frgType", v.getFragmentType());
			switch (v.getFragmentType())
			{
				case SCAFFOLD:
					n.setAttribute("ui.class", "scaffold");
					break;
				case FRAGMENT:
					n.setAttribute("ui.class", "fragment");
					break;
				case CAP:
					n.setAttribute("ui.class", "cap");
					break;
			}
			if (v.isRCV())
			{
				n.setAttribute("ui.class", "rcv");
			}
			n.setAttribute("dnp.level", v.getLevel());
			n.setAttribute("dnp.", "");
			
			// Create representation of free APs
			
			for (int i=0; i<v.getNumberOfAP(); i++)
			{
				DENOPTIMAttachmentPoint ap = v.getAttachmentPoints().get(i);
				if (ap.isAvailable())
				{
					String nApId = "v"+vID+"ap"+Integer.toString(i);
					Node nAP = graph.addNode(nApId);
					nAP.addAttribute("ui.label", nApId);
					nAP.setAttribute("ui.class", "ap");
					nAP.addAttribute("dnp.srcVrtApId", i);
					nAP.addAttribute("dnp.srcVrtId", v.getVertexId());
					Edge eAP = graph.addEdge(vID+"-"+nApId,vID,nApId);
					eAP.setAttribute("ui.class", "ap");
					eAP.setAttribute("dnp.srcAPClass", ap.getAPClass());
				}
			}
		} 
		
		for (DENOPTIMEdge dnE : dnG.getEdgeList())
		{
			String srcIdx = Integer.toString(dnE.getSrcVertex());
			String trgIdx = Integer.toString(dnE.getTrgVertex());
			Edge e = graph.addEdge(srcIdx+"-"+trgIdx, srcIdx, trgIdx,true);
			e.setAttribute("dnp.srcAPId", dnE.getSrcAPID());
			e.setAttribute("dnp.trgAPId", dnE.getTrgAPID());
			e.setAttribute("dnp.srcAPClass", dnE.getSrcAPClass());
			e.setAttribute("dnp.trgAPClass", dnE.getTrgAPClass());
			e.setAttribute("dnp.bondType", dnE.getBondType());
		}
		 
		for (DENOPTIMRing r : dnG.getRings())
		{
			String srcIdx = Integer.toString(r.getHeadVertex().getVertexId());
			String trgIdx = Integer.toString(r.getTailVertex().getVertexId());
			Edge e = graph.addEdge(srcIdx+"-"+trgIdx, srcIdx, trgIdx,false);
			e.setAttribute("ui.class", "rc");
			
			//WARNING: graphs loaded without having a consistent definition of 
			// the fragment space will not have all the AP data (which should be 
			// taken from the fragment space). Therefore, they cannot be 
			// recognized as RCV, but here we can fix at least part of the issue
			// by using the DENOPTIMRing to identify the RCVs
			
			graph.getNode(srcIdx).setAttribute("ui.class", "rcv");
			graph.getNode(trgIdx).setAttribute("ui.class", "rcv");
		}
		
		return graph;
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Clears the current graph viewer but keeps track of the latest graph 
	 * loaded. 
	 */
	private void clearCurrentSystem()
	{	
		// Get rid of currently loaded graph
		dnGraph = null;
        graphViewer.cleanup();
	}

//-----------------------------------------------------------------------------

	private void updateGraphListSpinner()
	{		
		graphNavigSpinner.setModel(new SpinnerNumberModel(currGrphIdx+1, 1, 
				dnGraphLibrary.size(), 1));
		totalGraphsLabel.setText(Integer.toString(dnGraphLibrary.size()));
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Listener for identifying the node on which the user has clicked and 
	 * load the corresponding fragment into the fragment viewer pane.
	 */
	private class NodeClickedListener implements PropertyChangeListener
	{
		
		@Override
		public void propertyChange(PropertyChangeEvent evt) 
		{
			// null is used to trigger cleanup
			if (evt.getNewValue() == null)
			{
				if (fragViewer != null)
				{
					fragViewer.clearAll();
					((CardLayout) fragViewerCardHolder.getLayout()).show(
							fragViewerCardHolder, EMPTYCARDNAME);
				}
			}
			
			// Otherwise try to load the fragment into the viewer
			String nodeId = (String) evt.getNewValue();
			Node n = graph.getNode(nodeId);
			if (n == null || !hasFragSpace)
			{
				return;
			}
			
			DENOPTIMVertex v;
			try {
				v = dnGraph.getVertexWithId(
						Integer.parseInt(nodeId));
			} catch (NumberFormatException e1) {
				//e1.printStackTrace();
				return;
			}
			
			DENOPTIMFragment frag = null;
			try {
				DENOPTIMVertex bb = FragmentSpace.getVertexFromLibrary(
						v.getFragmentType(), v.getMolId());
				if (bb instanceof DENOPTIMFragment)
					frag = (DENOPTIMFragment) bb;
			} catch (DENOPTIMException e) {
				//e.printStackTrace();
				return;
			}
			
			fragViewer.loadFragImentToViewer(frag);
			((CardLayout) fragViewerCardHolder.getLayout()).show(
					fragViewerCardHolder, FRAGVIEWERCARDNAME);
		}
	}

//-----------------------------------------------------------------------------

	private class GraphSpinnerChangeEvent implements ChangeListener
	{
		private boolean inEnabled = true;
		
		public GraphSpinnerChangeEvent()
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
        	
        	//NB here we convert from 1-based index in GUI to 0-based index
        	currGrphIdx = ((Integer) graphNavigSpinner.getValue())
        			.intValue() - 1;
        	loadCurrentGraphIdxToViewer();
        }
	}
	
//-----------------------------------------------------------------------------
	
	private void loadFragmentSpace()
	{
		// Define the fragment space via a new dialog
		FSParams fsParams = new FSParams(this);
        fsParams.pack();
        fsParams.setVisible(true);
        
        ((CardLayout) fragViewerCardHolder.getLayout()).show(
				fragViewerCardHolder, EMPTYCARDNAME);
	}
	
//-----------------------------------------------------------------------------
	
	private class FSParams extends GUIModalDialog
    {
		private FSParametersForm fsParsForm;
		
		private GUIGraphHandler parent;
		
	//-------------------------------------------------------------------------
		
		/**
		 * Constructor
		 */
		public FSParams(GUIGraphHandler parentPanel)
		{
			super();
			this.parent = parentPanel;
			
			fsParsForm = new FSParametersForm(this.getSize());
			addToCentralPane(fsParsForm);
			
			this.btnDone.setText("Create Fragment Space");
			this.btnDone.setToolTipText("<html>Uses the parameters defined "
					+ "above to"
					+ "<br> build a fragment space and make it available to"
					+ "<br>the graph handler.</html>");
			
			this.btnDone.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {					
					try {
						fsParsForm.possiblyReadParamsFromFSParFile();
						makeFragSpace();
					} catch (Exception e1) {
						String msg = "<html>The given parameters did not "
								+ "allow to "
								+ "build a fragment space.<br>"
								+ "Possible cause of this problem: " 
								+ "<br>";
								
						if (e1.getCause() != null)
						{
							msg = msg + e1.getCause();
						}
						if (e1.getMessage() != null)
						{
							msg = msg + " " + e1.getMessage();
						}
						msg = msg + "<br>Please alter the "
								+ "settings and try again.</html>";
								
						JOptionPane.showMessageDialog(null, msg,
				                "Error",
				                JOptionPane.ERROR_MESSAGE,
				                UIManager.getIcon("OptionPane.errorIcon"));
						
						parent.renderForLackOfFragSpace();
						return;
					}
					parent.renderForPresenceOfFragSpace();
					close();
				}
			});
			
			this.btnCanc.setToolTipText("Exit without creating a fragment "
					+ "space.");
		}
		
	//-------------------------------------------------------------------------
		
		/**
		 * Reads all the parameters, calls the interpreters, and eventually
		 * creates the static FragmentSpace object.
		 * @throws Exception
		 */
		private void makeFragSpace() throws Exception
		{
			if (fsParsForm.txtPar1.getText().trim().equals(""))
			{
				throw new DENOPTIMException("No library of fragments");
			}
			
			StringBuilder sbPars = new StringBuilder();
			fsParsForm.putParametersToString(sbPars);
			
			String[] lines = sbPars.toString().split(
					System.getProperty("line.separator"));
			for (String line : lines)
			{
				if ((line.trim()).length() == 0)
                {
                    continue;
                }
				if (line.startsWith("#"))
                {
                    continue;
                }
				if (line.toUpperCase().startsWith("FS-"))
                {
                    FragmentSpaceParameters.interpretKeyword(line);
                    continue;
                }
                if (line.toUpperCase().startsWith("RC-"))
                {
                    RingClosureParameters.interpretKeyword(line);
                    continue;
                }
			}
			
			// This creates the static FragmentSpace object
			if (FragmentSpaceParameters.fsParamsInUse())
	        {
	            FragmentSpaceParameters.checkParameters();
	            FragmentSpaceParameters.processParameters();
	        }
	        if (RingClosureParameters.rcParamsInUse())
	        {
	            RingClosureParameters.checkParameters();
	            RingClosureParameters.processParameters();
	        }
		}
		
	//-------------------------------------------------------------------------

    }
	
//-----------------------------------------------------------------------------

	private void deprotectEditedSystem()
	{
		btnSaveEdits.setEnabled(false);
		btnAddGraph.setEnabled(true);
		btnOpenGraphs.setEnabled(true);
		
		if (dnGraphLibrary.size()==0)
		{
			graphNavigSpinner.setModel(new SpinnerNumberModel(0,0,0,0));
		}
		else
		{
			graphNavigSpinner.setModel(new SpinnerNumberModel(currGrphIdx+1, 1, 
					dnGraphLibrary.size(), 1));
		}
		((DefaultEditor) graphNavigSpinner.getEditor())
			.getTextField().setEditable(true); 
		((DefaultEditor) graphNavigSpinner.getEditor())
			.getTextField().setForeground(Color.BLACK);
		
		graphSpinnerListener.setEnabled(true);
	}
	
//-----------------------------------------------------------------------------
	
	private void protectEditedSystem()
	{
		btnSaveEdits.setEnabled(true);
		btnAddGraph.setEnabled(false);
		btnOpenGraphs.setEnabled(false);
		
		graphNavigSpinner.setModel(new SpinnerNumberModel(currGrphIdx+1, 
				currGrphIdx+1, currGrphIdx+1, 1));
		((DefaultEditor) graphNavigSpinner.getEditor())
			.getTextField().setEditable(false); 
		((DefaultEditor) graphNavigSpinner.getEditor())
			.getTextField().setForeground(Color.GRAY);
		
		graphSpinnerListener.setEnabled(false);
	}
	
//-----------------------------------------------------------------------------
    
    private void removeCurrentdnGraph() throws DENOPTIMException
    {
    	// Clears the "dnGraph" and GUI components, but keep memory of the 
    	// status of the graph of an easy recovery, though since the old graph
    	// is being removed, the recovered data is not needed anymore.
        clearCurrentSystem();
    	
    	// Actual removal from the library
    	if (dnGraphLibrary.size()>0)
    	{
    		dnGraphLibrary.remove(currGrphIdx);
    		int libSize = dnGraphLibrary.size();
    		
    		if (libSize > 0)
    		{
	    		if (currGrphIdx>=0 && currGrphIdx<libSize)
	    		{
	    			//we keep currGrphIdx as it will correspond to the next item
	    		}
	    		else
	    		{
	    			currGrphIdx = currGrphIdx-1;
	    		}
	
	    		// We use the currGrphIdx to load another dnGraph
		    	loadCurrentGraphIdxToViewer();
		    	updateGraphListSpinner();
    		}
    		else
    		{
    			currGrphIdx = -1;
    			//Spinner will be fixed by the deprotection routine
    			totalGraphsLabel.setText(Integer.toString(
    					dnGraphLibrary.size()));
    			enableGraphDependentButtons(false);
    		}
    		deprotectEditedSystem();
    	}
    }

//-----------------------------------------------------------------------------

  	private void saveUnsavedChanges() 
  	{	      		
  		// Overwrite dnGraph in library
  		dnGraphLibrary.set(currGrphIdx, dnGraph);
  		
  		// Release constraints
        deprotectEditedSystem();
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
