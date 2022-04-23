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

package denoptim.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.files.FileAndFormat;
import denoptim.files.FileFormat;
import denoptim.files.FileUtils;
import denoptim.files.UndetectedFileFormatException;
import denoptim.fragspace.FragmentSpace;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.DGraph;
import denoptim.graph.Edge.BondType;
import denoptim.graph.EmptyVertex;
import denoptim.graph.Template;
import denoptim.graph.Template.ContractLevel;
import denoptim.graph.Vertex;
import denoptim.graph.Vertex.BBType;
import denoptim.gui.GraphViewerPanel.LabelType;
import denoptim.io.DenoptimIO;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;


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
	private static final long serialVersionUID = 1L;
	
	/**
	 * Unique identified for instances of this handler
	 */
	public static AtomicInteger graphHandlerTabUID = 
			new AtomicInteger(1);
	
	/**
	 * The currently loaded list of graphs
	 */
	protected ArrayList<DGraph> dnGraphLibrary =
			new ArrayList<DGraph>();
	
	/**
	 * The currently loaded list of molecular representations 
	 * of the graphs
	 */
	private ArrayList<IAtomContainer> molLibrary =
			new ArrayList<IAtomContainer>();
	
	/**
	 * The unsaved version of the currently loaded graph
	 */
	private DGraph dnGraph;
	
	/**
	 * Unique identified for graphs built here
	 */
	public static AtomicInteger graphUID = new AtomicInteger(1);
	
	/**
	 * The index of the currently loaded dnGraph [0–(n-1)}
	 */
	private int currGrphIdx = 0;
	
	/**
	 * Flag signaling that loaded data has changes since last save
	 */
	private boolean unsavedChanges = false;
	
	// The panel that hosts graph, vertex, and molecular viewers
	private GraphVertexMolViewerPanel visualPanel;
	
	// The panel hosting buttons for manipulation of graphs
	private JPanel graphCtrlPane;
	
	// The panel hosting buttons for navigation in the list of graphs
	private JPanel graphNavigPane;
	
    // Components managing loading of the fragment space
    private JButton btnFragSpace;
    private String loadFSToolTip = "<html>No space of building blocks loaded.<br>"
                   + "Graphs can be inspected without loading any space.<br>"
                   + "However, loading a space allows to edit and build"
                   + "graphs manually.</html>";
	
	private JPanel pnlMouseMode;
	private JButton btnPickMode;
	private JButton btnMoveMode;
    
	private JButton btnAddGraph;
	private JButton btnGraphDel;
	
	private JButton btnOpenGraphs;
	
	private JSpinner graphNavigSpinner;
	private JLabel totalGraphsLabel;
	private final GraphSpinnerChangeEvent graphSpinnerListener = 
												new GraphSpinnerChangeEvent();
	
	private JPanel pnlEditVrtxBtns;
	private JButton btnAddLibVrtx;
    private JButton btnAddEmptyVrtx;
	private JButton btnDelSel;
	private JButton btnAddChord; 
	
	private JPanel pnlShowLabels;
	private JButton btnLabAPC;
    private JButton btnLabBT;
    private JButton btnLabBB;
	
	private JPanel pnlSaveEdits;
	private JButton btnSaveEdits;
	
	/**
	 * Subset of vertices for compatible building block selecting GUI.
	 * These vertices are clones of those in the loaded library,
	 * and are annotate with fragmentID and AP pointers meant to 
	 * facilitate a quick selection of compatible connections.
	 */
	private ArrayList<Vertex> compatVrtxs;
	
	/**
	 * Map converting fragIDs in fragment library to fragIDs in subset
	 * of compatible fragments
	 */
	private Map<Integer,Integer> genToLocIDMap;
	
	private boolean updateMolViewer = false;
	
	private static final  IChemObjectBuilder builder = 
            SilentChemObjectBuilder.getInstance();
	
    private final String CONTRACTKEY = "CONTRACT";
    private final String BBTYPEKEY = "BBTYPE";
	
	private boolean painted;
	
	/**
	 * The fragment space this handler works with
	 */
	private FragmentSpace fragSpace = null;

//-----------------------------------------------------------------------------
	
	@Override
	public void paint(Graphics g) {
	    super.paint(g);

	    if (!painted) {
	        painted = true;
	        visualPanel.setDefaultDividerLocation();
	    }
	}
	
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
		// - (Center) where graphs/vertices/molecules are visualised
		// - (East) graph controls
		// - (South) general controls (load, save, close)
		
		visualPanel = new GraphVertexMolViewerPanel();
		this.add(visualPanel,BorderLayout.CENTER);
       
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
				int res = JOptionPane.showOptionDialog(btnAddGraph,
		                "<html>Please choose whether to start creations "
		                + "of a new graph (Build), "
		                + "or import graph from file.</html>",
		                "Specify source of new graph",
		                JOptionPane.DEFAULT_OPTION,
		                JOptionPane.QUESTION_MESSAGE,
		                UIManager.getIcon("OptionPane.warningIcon"),
		                options,
		                options[2]);
				switch (res)
				{
					case 0:
						if (fragSpace==null)
						{
							JOptionPane.showMessageDialog(btnAddGraph,
					                "<html>No fragment space is currently loaded!<br>"
					                + "You must load a fragment space to build graphs that<br>"
					                + "contain molecular frgments. <br>"
					                + "However, without a fragment space, you can still build<br>"
					                + "graphs made of empty vertexes (i.e., vertexes contain<br>"
					                + "no atoms, but only attachment points).</html>",
					                "WARNING",
					                JOptionPane.WARNING_MESSAGE,
					                UIManager.getIcon("OptionPane.warningIcon"));
						}
						try
                        {
                            startGraphFromFragSpaceOrCreationOfEmptyVertex();
                        } catch (DENOPTIMException e1)
                        {
                            e1.printStackTrace();
                            JOptionPane.showMessageDialog(btnAddGraph,
                                    "<html>Could not create graph!<br>"
                                    + "Exception thrown when starting the "
                                    + "construction<br>"
                                    + "of a new graph. Please, report this to "
                                    + "the authors.</html>",
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE,
                                    UIManager.getIcon("OptionPane.errorIcon"));
                            return;
                        }
						break;
					
					case 1:
						File inFile = GUIFileOpener.pickFileWithGraph(
						        btnAddGraph);
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
					removeCurrentDnGraph();
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
		
		
		JLabel mouseModeLab = new JLabel("Mouse mode:");
		btnPickMode = new JButton("Pick");
		btnPickMode.setToolTipText("Makes the mouse select vertex on click.");
		btnPickMode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                visualPanel.setMouseMode(ModalGraphMouse.Mode.PICKING);
            }
        });
        btnMoveMode = new JButton("Move");
        btnMoveMode.setToolTipText("Makes mouse move the graph view.");
        btnMoveMode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                visualPanel.setMouseMode(ModalGraphMouse.Mode.TRANSFORMING);
            }
        });
        pnlMouseMode = new JPanel();
        GroupLayout lyoMouseModeLayout = new GroupLayout(pnlMouseMode);
        pnlMouseMode.setLayout(lyoMouseModeLayout);
        lyoMouseModeLayout.setAutoCreateGaps(true);
        lyoMouseModeLayout.setAutoCreateContainerGaps(true);
        lyoMouseModeLayout.setHorizontalGroup(lyoMouseModeLayout.createParallelGroup(
                GroupLayout.Alignment.CENTER)
                .addComponent(mouseModeLab)
                .addGroup(lyoMouseModeLayout.createSequentialGroup()
                        .addComponent(btnMoveMode)
                        .addComponent(btnPickMode)));
        lyoMouseModeLayout.setVerticalGroup(lyoMouseModeLayout.createSequentialGroup()
                .addComponent(mouseModeLab)
                .addGroup(lyoMouseModeLayout.createParallelGroup()
                        .addComponent(btnMoveMode)
                        .addComponent(btnPickMode)));
        graphCtrlPane.add(pnlMouseMode);
        
		
		graphCtrlPane.add(new JSeparator());
		
		
		// Controls to alter the presently loaded graph (if any)
		pnlEditVrtxBtns = new JPanel();
		JLabel edtVertxsLab = new JLabel("Edit Graph:");
		
        btnFragSpace = new JButton("Load Library of Vertexes");
        btnFragSpace.setToolTipText(loadFSToolTip);
        btnFragSpace.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try
                {
                    loadFragmentSpace();
                } catch (Exception e1)
                {
                    JOptionPane.showMessageDialog(btnAddLibVrtx,
                            "<html>No fragment spaceFaild to define a space "
                            + "of building blocks from the given input.</html>",
                            "Error",
                            JOptionPane.ERROR_MESSAGE,
                            UIManager.getIcon("OptionPane.errorIcon"));
                    return;
                }
                btnFragSpace.setText("Change Library of Vertexes");
            }
        });
        
		btnAddLibVrtx = new JButton("Add Vertex from Library");
		btnAddLibVrtx.setToolTipText("<html>Choose a new vertex from the "
		        + "loaded space of building blocks and<br>"
		        + "append it to the "
				+ "attachment point/s selected in the current graph.<html>");
		btnAddLibVrtx.setEnabled(false);
		btnAddLibVrtx.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {	
				if (fragSpace==null)
				{
					JOptionPane.showMessageDialog(btnAddLibVrtx,
			                "<html>No space of building blocks is currently "
			                + "loaded!<br>"
			                + "You must first load a space in order to add"
			                + "vertexes from such space.</html>",
			                "Error",
			                JOptionPane.ERROR_MESSAGE,
			                UIManager.getIcon("OptionPane.errorIcon"));
					return;
				}
				ArrayList<AttachmentPoint> selAps = 
				        visualPanel.getAPsSelectedInViewer();				
				if (selAps.size() == 0)
				{
					JOptionPane.showMessageDialog(btnAddLibVrtx,
			                "<html>No attachment point selected!<br>"
			                + "Drag the mouse to select APs.<br> "
			                + "Click again to unselect.</html>",
			                "Error",
			                JOptionPane.ERROR_MESSAGE,
			                UIManager.getIcon("OptionPane.errorIcon"));
					return;
				}
				extendGraphFromFragSpace(selAps);
				
				// Update viewer
				visualPanel.loadDnGraphToViewer(dnGraph,true);
				
				// Protect edited system
		        unsavedChanges = true;
		        protectEditedSystem();

				// The molecular representation is updated when we save changes
		        visualPanel.renderMolVieverToNeedUpdate();
		        updateMolViewer = true;
			}
		});
		
		
	    btnAddEmptyVrtx = new JButton("Add Empty Vertex");
        btnAddEmptyVrtx.setToolTipText("<html>Creates an empty vertex "
                + "(i.e., a vertex with attachment points<br>"
                + "and properties, but that contains no atoms) and appends it "
                + "to<br>"
                + "the attachment points selected in the current graph."
                + "<html>");
        btnAddEmptyVrtx.setEnabled(true);
        btnAddEmptyVrtx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ArrayList<AttachmentPoint> selAps = 
                        new ArrayList<AttachmentPoint> ();
                if (dnGraph != null)
                {
                    selAps = visualPanel.getAPsSelectedInViewer(); 
                    if (selAps.size() == 0)
                    {
                        //This would overwrite the current graph, so no-go!
                        JOptionPane.showMessageDialog(btnAddEmptyVrtx,
                                "<html>No attachment point selected!<br>"
                                + "Drag the mouse to select APs.<br> "
                                + "Click again to unselect.</html>",
                                "Error",
                                JOptionPane.ERROR_MESSAGE,
                                UIManager.getIcon("OptionPane.errorIcon"));
                        return;
                    }
                }
                startGraphFromCreationOfEmptyVertex(selAps);
            }
        });
        
		
		btnDelSel = new JButton("Remove Vertex");
		btnDelSel.setToolTipText("<html>Removes the selected vertices from "
				+ "the system.<br><br><b>WARNING:</b> this action cannot be "
				+ "undone!</html>");
		btnDelSel.setEnabled(false);
		btnDelSel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ArrayList<Vertex> selVrtx = 
				        visualPanel.getSelectedNodesInViewer();
				if (selVrtx.size() == 0)
				{
					JOptionPane.showMessageDialog(btnDelSel,
							"<html>No vertex selected! Drag the "
			                + "mouse to select vertices."
					        + "<br>Click on background to unselect.</html>",
			                "Error",
			                JOptionPane.ERROR_MESSAGE,
			                UIManager.getIcon("OptionPane.errorIcon"));
					return;
				}
				
				// Prevent removal of the scaffold
				/*
				DENOPTIMVertex src = dnGraph.getSourceVertex();
				for (DENOPTIMVertex v : selVrtx)
				{
					if (v == src)
					{
						JOptionPane.showMessageDialog(btnDelSel,
								"<html>The scaffold cannot be removed."
						        + "</html>",
				                "Error",
				                JOptionPane.ERROR_MESSAGE,
				                UIManager.getIcon("OptionPane.errorIcon"));
						return;
					}
				}
				*/
				
				for (Vertex v : selVrtx)
				{
					dnGraph.removeVertex(v);
				}
				
				// Update viewer
                visualPanel.loadDnGraphToViewer(dnGraph,true);
				
		        // Protect the temporary "dnGraph" obj
		        unsavedChanges = true;
		        protectEditedSystem();
			
		        // The molecular representation is updated when we save changes
                visualPanel.renderMolVieverToNeedUpdate();
                updateMolViewer = true;
			}
		});
		
	    // Controls to add chord (ring closing edge)
        btnAddChord = new JButton("Add Chord");
        btnAddChord.setToolTipText("<html>Add a ring-closing edge between two "
                + "selected vertices.<html>");
        btnAddChord.setEnabled(false);
        btnAddChord.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {    
                ArrayList<Vertex> selVrtxs = 
                        visualPanel.getSelectedNodesInViewer();               
                if (selVrtxs.size() != 2)
                {
                    JOptionPane.showMessageDialog(btnAddChord,
                            "<html>Number of selected vertices: "
                            + selVrtxs.size() + " <br>"
                            + "Please, drag the mouse and "
                            + "select only two vertices!<br> "
                            + "Click again to unselect.</html>",
                            "Error",
                            JOptionPane.ERROR_MESSAGE,
                            UIManager.getIcon("OptionPane.errorIcon"));
                    return;
                }
                addChordOnGraph(selVrtxs);
                
                // Update viewer
                visualPanel.loadDnGraphToViewer(dnGraph,true);
                
                // Protect edited system
                unsavedChanges = true;
                protectEditedSystem();

                // The molecular representation is updated when we save changes
                visualPanel.renderMolVieverToNeedUpdate();
                updateMolViewer = true;
            }
        });
        
        
		GroupLayout lyoEditVertxs = new GroupLayout(pnlEditVrtxBtns);
		pnlEditVrtxBtns.setLayout(lyoEditVertxs);
		lyoEditVertxs.setAutoCreateGaps(true);
		lyoEditVertxs.setAutoCreateContainerGaps(true);
		lyoEditVertxs.setHorizontalGroup(lyoEditVertxs.createParallelGroup(
				GroupLayout.Alignment.CENTER)
				.addComponent(edtVertxsLab)
				.addComponent(btnFragSpace)
				.addComponent(btnAddLibVrtx)
                .addComponent(btnAddEmptyVrtx)
				.addComponent(btnDelSel)
				.addComponent(btnAddChord));
		lyoEditVertxs.setVerticalGroup(lyoEditVertxs.createSequentialGroup()
				.addComponent(edtVertxsLab)
                .addComponent(btnFragSpace)
				.addComponent(btnAddLibVrtx)
                .addComponent(btnAddEmptyVrtx)
				.addComponent(btnDelSel)
				.addComponent(btnAddChord));
		graphCtrlPane.add(pnlEditVrtxBtns);
		
		graphCtrlPane.add(new JSeparator());
		
		// Controls of displayed attributes
		pnlShowLabels = new JPanel();
		JLabel lblShowHideLabels = new JLabel("Show/Hide labels:");
		
		btnLabAPC = new JButton("APClass");
		btnLabAPC.addActionListener(new showHideLabelsListener(btnLabAPC,
		        LabelType.APC));
		btnLabAPC.setEnabled(false);
        btnLabAPC.setToolTipText("Show/Hide attachment point class labels.");
        
        btnLabBT = new JButton("Bnd Typ");
        btnLabBT.addActionListener(new showHideLabelsListener(btnLabBT,
                LabelType.BT));
        btnLabBT.setEnabled(false);
        btnLabBT.setToolTipText("Show/Hide bond type ID labels.");
        
        btnLabBB = new JButton("BB ID");
        btnLabBB.addActionListener(new showHideLabelsListener(btnLabBB,
                LabelType.BBID));
        btnLabBB.setEnabled(false);
        btnLabBB.setToolTipText("Show/Hide building block ID labels.");
		
        GroupLayout lyoShowAttr = new GroupLayout(pnlShowLabels);
        pnlShowLabels.setLayout(lyoShowAttr);
        lyoShowAttr.setAutoCreateGaps(true);
        lyoShowAttr.setAutoCreateContainerGaps(true);
        lyoShowAttr.setHorizontalGroup(lyoShowAttr.createParallelGroup(
                        GroupLayout.Alignment.CENTER)
                        .addComponent(lblShowHideLabels)
                        .addComponent(btnLabAPC)
                        .addComponent(btnLabBT)
                        .addComponent(btnLabBB));
        lyoShowAttr.setVerticalGroup(lyoShowAttr.createSequentialGroup()
		                .addComponent(lblShowHideLabels)
                        .addComponent(btnLabAPC)
                        .addComponent(btnLabBT)
                        .addComponent(btnLabBB));
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
		ButtonsBar commandsPane = new ButtonsBar();
		super.add(commandsPane, BorderLayout.SOUTH);
		
		btnOpenGraphs = new JButton("Load Library of Graphs");
		btnOpenGraphs.setToolTipText("Reads graphs or structures from file.");
		btnOpenGraphs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File inFile = GUIFileOpener.pickFile(btnOpenGraphs);
				if (inFile == null || inFile.getAbsolutePath().equals(""))
				{
					return;
				}
				importGraphsFromFile(inFile);
			}
		});
		commandsPane.add(btnOpenGraphs);
		  
        JButton btnSaveGraphs = new JButton("Save Library of Graphs");
        btnSaveGraphs.setToolTipText("Write all graphs to a file.");
        btnSaveGraphs.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                FileAndFormat fileAndFormat = 
                        GUIFileSaver.pickFileForSavingGraphs(btnSaveGraphs);
                if (fileAndFormat == null)
                {
                    return;
                }
                File outFile = fileAndFormat.file;
                try
                {
                    outFile = DenoptimIO.writeGraphsToFile(outFile,
                            fileAndFormat.format, dnGraphLibrary,
                            Logger.getLogger("GUILogger"),
                            GUI.PRNG);
                }
                catch (Exception ex)
                {
                    JOptionPane.showMessageDialog(btnSaveGraphs,
                            "Could not write to '" + outFile + "'!.",
                            "Error",
                            JOptionPane.PLAIN_MESSAGE,
                            UIManager.getIcon("OptionPane.errorIcon"));
                    return;
                }
                deprotectEditedSystem();
                unsavedChanges = false;
                FileUtils.addToRecentFiles(outFile, fileAndFormat.format);
            }
        });
        commandsPane.add(btnSaveGraphs);
		
		JButton btnSaveTmpl = new JButton("Save Library of Templates");
		btnSaveTmpl.setToolTipText("Make templates from the graphs and same "
		        + "them to file");
		btnSaveTmpl.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    
			    ConfigTemplateDialog tmplPropDialog = new ConfigTemplateDialog(
			            btnSaveTmpl, dnGraphLibrary.size());
		        tmplPropDialog.pack();
		        Object o = tmplPropDialog.showDialog();
		        if (o == null)
		        {
		            return;
		        }
		        
		        @SuppressWarnings("unchecked")
                Map<String,List<Object>> configs = (Map<String,List<Object>>) o;
		        
		        ArrayList<Vertex> templates = 
		                new ArrayList<Vertex>();
		        for (int i=0; i<dnGraphLibrary.size(); i++)
		        {
		            Template t = new Template(
		                    (BBType) configs.get(BBTYPEKEY).get(i));
		            t.setInnerGraph(dnGraphLibrary.get(i));
		            t.setContractLevel((ContractLevel) 
		                    configs.get(CONTRACTKEY).get(i));
		            templates.add(t);
		        }
				
		        FileAndFormat fileAndFormat = 
				        GUIFileSaver.pickFileForSavingVertexes(btnSaveTmpl);
		        if (fileAndFormat == null)
                {
                    return;
                }
                File outFile = fileAndFormat.file;
                try
                {
                    DenoptimIO.writeVertexesToFile(outFile,fileAndFormat.format,
                            templates);
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(btnSaveTmpl,
                            "Could not write to '" + outFile + "'! "
                            + "Hint: "+ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE,
                            UIManager.getIcon("OptionPane.errorIcon"));
                    return;
                }
                deprotectEditedSystem();
                unsavedChanges = false;
                FileUtils.addToRecentFiles(outFile, fileAndFormat.format);
			}
		});
		commandsPane.add(btnSaveTmpl);		

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
						+ "the graphs that DENOPTIM used to define candidate "
						+ "items, such as molecules.</p>"
						+ "<br>"
						+ "<p>In general, you can hover over any button or"
						+ "viewer to get a tip on its usage.</p>"
						+ "<br>"
						+ "<p>DENOPTIMGraphs is drawn in the "
						+ "central panel (i.e., the graph viewer). "
						+ "Each vertex is shown as a rounded square, and each "
						+ "edge as an arrow (or a line, for undirected edges)."
						+ " The color of a node represent the type of building"
						+ "block:<ul>"
						+ "<li>red for the scaffold,</li>"
						+ "<li>orange for ring-closing vertices,</li>"
						+ "<li>green for capping groups,</li>"
						+ "<li>blue for standard fragments,</li>"
                        + "<li>yellow for attachment points.</li>"
						+ "</ul></p>"
						+ "<p>If the loaded DENOPTIMGraph is associated with "
						+ "a chemical structure, the latter is shown in the "
						+ "molecular viewer (bottom-left panel).</p>"
						+ "<p>The content of a vertex in the graph viewer, "
						+ "i.e., a molecular fragment or an embedded graph, "
						+ "can be shown in the vertex viewer (top-left panel) "
						+ "upon selecting (see below) that vertex in the graph "
						+ "viewer. </p>"
						+ "<p>A building block space must be loaded to "
						+ "enable inspection of graph vertexes that depend on"
						+ "a building block space.</p>"
						+ "<br>"
						+ "<p><b>Control the graph view</b></p>"
						+ "<ul>"
						+ "<li>Use mouse mode <i>Pick</i> to enable selection "
						+ "of vertexes by single left click.</li>"
						+ "<li>Use mouse mode <i>Move</i> to drag the graph in "
						+ "any direction, <code>ALT</code>+drag to rotate, and "
						+ "<code>CTRL</code>+drag to skew the graph. Wheel, or "
						+ "analogous, to zomm in/out.</li>"
						+ "</ul>Mouse mode can be changed also by double click "
						+ "in the graph area, away from any vertex/edge, and "
						+ "hitting <code>p</code> for <i>Pick</i> or "
						+ "<code>t</code> for <i>Move</i> "
						+ "(or <i>Transform</i>).</p>"
						+ "<p>Right-click in the graph viewer shows a menu "
						+ "with shortcuts to refine node location, or "
						+ "show/hide graph labels.</p>"
						+ "<br>"
						+ "<p><b>Control the fragment and molecular views</b>"
						+ "</p>"
						+ "<p>Right-click on the Jmol viewer will open the "
						+ "Jmol menu. However, any change on the molecular "
						+ "object will not be saved.</p></html>";
				JOptionPane.showMessageDialog(btnHelp, 
						String.format(txt, 500),
	                    "Tips",
	                    JOptionPane.PLAIN_MESSAGE);
			}
		});
		commandsPane.add(btnHelp);
	}
	
//-----------------------------------------------------------------------------
	
	private class showHideLabelsListener implements ActionListener
	{
	    private JComponent parent;
	    private LabelType labTyp;
	    private Map<LabelType,Boolean> lastIteration = new HashMap<>();
	    
	    public showHideLabelsListener(JComponent parent, LabelType labTyp) 
	    {   
	        this.labTyp = labTyp;
	        this.parent = parent;
	        for (LabelType lt : LabelType.values())
	            lastIteration.put(lt, false);
	    }
	    
        @Override
        public void actionPerformed(ActionEvent e) {
            if (visualPanel.hasSelectedNodes())
            {
                boolean show = !lastIteration.get(labTyp);
                visualPanel.alterLabels(labTyp, show);
                lastIteration.put(labTyp,show);
            }
            else
            {
                JOptionPane.showMessageDialog(parent,
                        "<html>No elements selected! Drag the "
                        + "mouse to select elements."
                        + "<br>Click on background to unselect.</html>",
                        "Error",
                        JOptionPane.ERROR_MESSAGE,
                        UIManager.getIcon("OptionPane.errorIcon"));
            }
        }
	}
	
//-----------------------------------------------------------------------------
	
	private void enableGraphDependentButtons(boolean enable)
	{
		btnAddLibVrtx.setEnabled(enable);
        //btnAddEmptyVrtx.setEnabled(enable); //Always enabled
		btnDelSel.setEnabled(enable);
		btnAddChord.setEnabled(enable);
		btnLabAPC.setEnabled(enable);
        btnLabBT.setEnabled(enable);
        btnLabBB.setEnabled(enable);
	}
	
//-----------------------------------------------------------------------------
	
	private void startGraphFromCreationOfEmptyVertex(
	        ArrayList<AttachmentPoint> selAps)
	{   
        GUIEmptyVertexMaker makeEmptyVertexDialog = 
                new GUIEmptyVertexMaker(this);
        makeEmptyVertexDialog.pack();
        Object evObj = makeEmptyVertexDialog.showDialog();
        if (evObj == null)
        {
            return;
        }
        Vertex ev = (EmptyVertex) evObj;
        ArrayList<Vertex> lst = new ArrayList<Vertex>(1);
        lst.add(ev);
        GUIVertexSelector fragSelector = new GUIVertexSelector(this, false);
        fragSelector.ctrlPane.setVisible(false);
        if (selAps.size() == 0)
        {
            fragSelector.btnDone.setText("Confirm");
            fragSelector.setRequireApSelection(false); 
        } else {
            fragSelector.btnDone.setText("Confirm Selected AP");
            fragSelector.setRequireApSelection(true); 
        }
        fragSelector.load(lst, 0);
        Object selected = fragSelector.showDialog();
        if (selected == null)
        {
            return;
        }
        
        @SuppressWarnings("unchecked")
        ArrayList<Integer> trgFragApId = 
            ((ArrayList<ArrayList<Integer>>)selected).get(0);
        int incomingAPId = trgFragApId.get(1);
                      
        if (selAps.size() == 0)
        {
            currGrphIdx = dnGraphLibrary.size();
            initializeCurrentGraph();
            try
            {
                dnGraph.addVertex(ev);
            } catch (DENOPTIMException e1)
            {
                e1.printStackTrace();
                JOptionPane.showMessageDialog(this,"Could not make the "
                        + "new graph. " + e1.getMessage(),
                        "Error",
                        JOptionPane.PLAIN_MESSAGE,
                        UIManager.getIcon("OptionPane.errorIcon"));
                return;
            }
        } else {
            extendCurrentGraph(ev.getAP(incomingAPId), selAps);
        }
        
        // Update viewer
        visualPanel.loadDnGraphToViewer(dnGraph,true);
        enableGraphDependentButtons(true);
        
        // Protect edited system
        unsavedChanges = true;
        protectEditedSystem();

        // The molecular representation is updated when we save changes
        visualPanel.renderMolVieverToNeedUpdate();
        updateMolViewer = true;
	}

//-----------------------------------------------------------------------------

	/**
	 * Start the construction of a new graph from scratch
	 * @throws DENOPTIMException 
	 */
	private void startGraphFromFragSpaceOrCreationOfEmptyVertex() 
	        throws DENOPTIMException
	{
	    BBType rootType = BBType.SCAFFOLD;
	    String msg = "<html><body width='%1s'>"
                + "Please choose the type of building block to use as first "
                + "vertex of the graph.";
	    String[] options = null;
	    String defaultOpt = null;
	    if (fragSpace!=null)
	    {
	        options = new String[]{"Scaffold", "Fragment", "EmptyVertex", 
	            "Cancel"};
	        defaultOpt = options[3];
	        msg = msg + "Use a scaffold if the graph is meant to "
	                + "represent a necessary portion of a candidate entity.</html>";
	    } else {
	        options = new String[]{"EmptyVertex", "Cancel"};
	        defaultOpt = options[1];
	    }
        int res = JOptionPane.showOptionDialog(this,String.format(msg,350),
                "Specify type of initial building block",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                UIManager.getIcon("OptionPane.warningIcon"),
                options,
                defaultOpt);
        if (fragSpace==null)
        {
            res = res + 10;
        }
        
        ArrayList<Vertex> vrtxLib = new  ArrayList<Vertex>();
        switch (res)
        {
            case 0:
                rootType = BBType.SCAFFOLD;
                for (Vertex bb : fragSpace.getScaffoldLibrary())
                {
                    vrtxLib.add(bb.clone());
                }
                break;
                
            case 1:
                rootType = BBType.FRAGMENT;
                for (Vertex bb : fragSpace.getFragmentLibrary())
                {
                    vrtxLib.add(bb.clone());
                }
                break;
                
            case 10:
            case 2:
                // In this case we do not use the fragment space. So, all the 
                // index-based operations on the fragment space that are done 
                // after this 'switch' block make no sense. Instead, we use the 
                // same method called by the "Add Empty Vertex" button.
                ArrayList<AttachmentPoint> selectedAPs = new ArrayList<>();
                startGraphFromCreationOfEmptyVertex(selectedAPs);
                return;
            
            case 11: 
            case 3:
                return;
        }
        if (vrtxLib.size() == 0)
		{
			JOptionPane.showMessageDialog(this,"No building blocks of the "
			        + "choosen type.",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;
		}
		
		// Select the scaffold
		GUIVertexSelector fragSelector = new GUIVertexSelector(this, false);
		fragSelector.setRequireApSelection(false);
        fragSelector.load(vrtxLib, 0);
		Object selected = fragSelector.showDialog();
		if (selected == null)
		{
			return;
		}
		
		@SuppressWarnings("unchecked")
        ArrayList<Integer> trgFragApId = ((ArrayList<ArrayList<Integer>>)selected)
                .get(0);
        int scaffFragId = trgFragApId.get(0);
		
		// Create the new graph
		currGrphIdx = dnGraphLibrary.size();
		initializeCurrentGraph();
		
		// Create the node
		int firstBBId = 1;
		Vertex firstVertex = null;
        try
        {
            firstVertex = Vertex.newVertexFromLibrary(
                    firstBBId, scaffFragId, rootType, fragSpace);
        } catch (DENOPTIMException e)
        {
            JOptionPane.showMessageDialog(this,"Could not retrieve the "
                    + "requested building blocks. " + e.getMessage(),
                    "Error",
                    JOptionPane.PLAIN_MESSAGE,
                    UIManager.getIcon("OptionPane.errorIcon"));
            return;
        }

		dnGraph.addVertex(firstVertex);
		
		// Put the graph to the viewer
        visualPanel.loadDnGraphToViewer(dnGraph,false);
		enableGraphDependentButtons(true);
		unsavedChanges = true;
		updateMolViewer = true;
        protectEditedSystem();
	}
	
//-----------------------------------------------------------------------------
	
	private void initializeCurrentGraph()
	{
        dnGraph = new DGraph();
        dnGraph.setGraphId(graphUID.getAndIncrement());
        // Add new graph and corresponding mol representation (must exist)
        dnGraphLibrary.add(dnGraph);
        //NB: we add an empty molecular representation to keep the list
        // of graphs and that of mol.rep. in sync
        molLibrary.add(builder.newAtomContainer());
        visualPanel.clearMolecularViewer();
        updateGraphListSpinner();
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Edits the currently loaded graph by adding a chord involving the two
	 * selected vertices.
	 * @param rcvs the selected vertices. Must be two vertices.
	 */
	private void addChordOnGraph(ArrayList<Vertex> rcvs)
	{
        if (rcvs.size() != 2)
        {
            JOptionPane.showMessageDialog(this,
                    "<html>Number of selected vertices: "
                    + rcvs.size() + " <br>"
                    + "Please, drag the mouse and "
                    + "select only two vertices!<br> "
                    + "Click again to unselect.</html>",
                    "Error",
                    JOptionPane.ERROR_MESSAGE,
                    UIManager.getIcon("OptionPane.errorIcon"));
            return;
        }
        
        try
        {
            dnGraph.addRing(rcvs.get(0), rcvs.get(1));
        } catch (DENOPTIMException e)
        {
            BondType bt = BondType.UNDEFINED;
            dnGraph.addRing(rcvs.get(0), rcvs.get(1), bt);
        }
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Extends the current graph by appending a node to a specific free AP on 
	 * the growing graph. 
	 * This method will prompt a question on which incoming fragment to append 
	 * @param selAps attachment points on the growing graph.
	 */
	private void extendGraphFromFragSpace(
	        ArrayList<AttachmentPoint> selAps)
	{
		// For extensions of existing graphs we need to know where to extend
		if (selAps.size() == 0)
		{
			JOptionPane.showMessageDialog(this,"No AP selected in the "
					+ "graph.",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;
		}
		
		// Create clones of fragments and put the into 'compatFrags'
		collectFragAndAPsCompatibleWithSelectedAPs(selAps);
		
		Vertex.BBType trgFrgType = Vertex.BBType.UNDEFINED;
		ArrayList<Vertex> vertxLib = new ArrayList<Vertex>();		
		String[] options = new String[]{"Any Vertex",
				"Compatible Vertices ("+compatVrtxs.size()+")",
				"Capping group"};
		int res = JOptionPane.showOptionDialog(this,
                "<html>Choose a subset of possible vertices:</html>",
                "Choose Vertex Subset",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                UIManager.getIcon("OptionPane.warningIcon"),
                options,
                options[0]);
		
		switch (res)
		{
			case 0:
			    ArrayList<Vertex> tmp = fragSpace.getFragmentLibrary();
				vertxLib = new  ArrayList<Vertex>();
		        for (Vertex bb : fragSpace.getFragmentLibrary())
		        {
		        	vertxLib.add(bb.clone());
		        }
				trgFrgType = Vertex.BBType.FRAGMENT;
				break;
				
			case 1:
				vertxLib = compatVrtxs;
				trgFrgType = Vertex.BBType.FRAGMENT;
				break;
				
			case 2:
				vertxLib = new ArrayList<Vertex>();
		        for (Vertex bb : fragSpace.getCappingLibrary())
		        {
		            vertxLib.add(bb.clone());
		        }
				trgFrgType = Vertex.BBType.CAP;
				break;
			default:
				return;
		}

		if (vertxLib.size() == 0)
		{
			JOptionPane.showMessageDialog(this,"No fragments in the library",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;
		}
		
		// Select the incoming fragment and its AP to use

	    GUIVertexSelector fragSelector = new GUIVertexSelector(this,false);
        fragSelector.setRequireApSelection(true);
        fragSelector.load(vertxLib, 0);
        Object selected = fragSelector.showDialog();
		if (selected == null)
		{
			return;
		}
		ArrayList<Integer> trgFragApId = 
		        ((ArrayList<ArrayList<Integer>>)selected).get(0);
		Vertex chosenVrtx = vertxLib.get(trgFragApId.get(0));
		
		extendCurrentGraph(chosenVrtx.getAP(trgFragApId.get(1)),selAps);
	}
	
//-----------------------------------------------------------------------------
	
	private void extendCurrentGraph(AttachmentPoint apOnIncomingVrtx,
            ArrayList<AttachmentPoint> selAps)
	{   
        Vertex chosenVrtx = apOnIncomingVrtx.getOwner();
        if (chosenVrtx == null)
            return;
        
        int apIdOnIncVrtx = apOnIncomingVrtx.getIndexInOwner();
       
        for (int i=0; i<selAps.size(); i++)
        {
            AttachmentPoint srcAp = selAps.get(i);
            Vertex trgVertex = chosenVrtx.clone();
            trgVertex.setVertexId(dnGraph.getMaxVertexId()+1);
            AttachmentPoint trgAp = trgVertex.getAP(apIdOnIncVrtx);
            try
            {
                dnGraph.appendVertexOnAP(srcAp, trgAp);
            } catch (DENOPTIMException e) {
                JOptionPane.showMessageDialog(this,"Unable to make new edge. "
                        + e.getMessage(),
                        "Error",
                        JOptionPane.PLAIN_MESSAGE,
                        UIManager.getIcon("OptionPane.errorIcon"));
                return;
            }
	    }
	}

//-----------------------------------------------------------------------------
	
	private void collectFragAndAPsCompatibleWithSelectedAPs(
			ArrayList<AttachmentPoint> srcAPs) 
	{
		compatVrtxs = new ArrayList<Vertex>();
		
		// WARNING: here I re-do most of what is already done in
		// FragmentSpace.getFragmentsCompatibleWithTheseAPs.
		// However, here we add additional data to (clones) of the 
		// fragments, so that I can easily highlight the compatible APs in 
		// the selection GUI.
		
    	// First we get all possible APs on any fragment
    	ArrayList<AttachmentPoint> compatAps = 
    	        fragSpace.getAPsCompatibleWithThese(srcAPs);
    	
    	// then keep unique fragment identifiers, and store unique
		genToLocIDMap = new HashMap<Integer,Integer>();
		
		String PRESELPROP = GUIVertexSelector.PRESELECTEDAPSFIELD;
		String PRESELPROPSEP = GUIVertexSelector.PRESELECTEDAPSFIELDSEP;
		
		for (AttachmentPoint ap : compatAps)
		{
		    int vId = ap.getOwner().hashCode();
			int apId = ap.getOwner().getIndexOfAP(ap);
			if (genToLocIDMap.keySet().contains(vId))
			{
				Vertex vrtx = compatVrtxs.get(genToLocIDMap.get(vId));
				String prop = vrtx.getProperty(PRESELPROP).toString();
				vrtx.setProperty(PRESELPROP,prop+PRESELPROPSEP+apId);
			}
			else
			{
			    Vertex bb = ap.getOwner().clone();
				bb.setProperty(PRESELPROP,apId);
				genToLocIDMap.put(vId,compatVrtxs.size());
				compatVrtxs.add(bb);
			}
		}
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
			molLibrary = DenoptimIO.readSDFFile(file.getAbsolutePath());
		} catch (DENOPTIMException e) {
			System.out.println("Could not read molecules from " + file);
			for (int i=0; i<dnGraphLibrary.size(); i++)
			{
				molLibrary.add(builder.newAtomContainer());
			}
		}
			
		// Display the first
		currGrphIdx = 0;
		
		loadCurrentGraphIdxToViewer(false);
		updateGraphListSpinner();
	}

//-----------------------------------------------------------------------------

	private void appendGraphsFromFile(File file)
	{
	    // Reading graphs is format-agnostic
		ArrayList<DGraph> graphs = readGraphsFromFile(file);
		
		// Try to read or make molecular representations
		ArrayList<IAtomContainer> mols = new ArrayList<IAtomContainer>();
        FileFormat ff = null;
        try
        {
            ff = FileUtils.detectFileFormat(file);
        } catch (Exception e1)
        {
            // we'll ignore the format specific tasks
        }
        switch (ff)
        {
            case GRAPHSDF:
                try {
                    molLibrary.addAll(DenoptimIO.readSDFFile(
                            file.getAbsolutePath()));
                } catch (DENOPTIMException e) {
                    System.err.println("WARNING: Could not read molecular "
                            + "representation from " + file);
                    for (int i=0; i<graphs.size(); i++)
                    {
                        molLibrary.add(builder.newAtomContainer());
                    }
                }
                break;
                
            default:
                // Add empty place holders
                for (int i=0; i<graphs.size(); i++)
                {
                    molLibrary.add(builder.newAtomContainer());
                }   
                break;    
        }
		
		int oldSize = dnGraphLibrary.size();
		if (graphs.size() > 0)
		{
			dnGraphLibrary.addAll(graphs);
			molLibrary.addAll(mols);
			
			// WE choose to display the first of the imported ones
			currGrphIdx = oldSize;
			
			loadCurrentGraphIdxToViewer(false);
			updateGraphListSpinner();
		}
	}
	
//-----------------------------------------------------------------------------

	private ArrayList<DGraph> readGraphsFromFile(File file)
	{
		ArrayList<DGraph> graphs = new ArrayList<DGraph>();
		try
		{
    		try 
    		{
    			graphs = DenoptimIO.readDENOPTIMGraphsFromFile(file);	
    		} 
    		catch (UndetectedFileFormatException uff) 
    		{
                String[] options = {"Abandon", "SDF", "JSON"};
                FileFormat[] ffs = {null,
                        FileFormat.GRAPHSDF,
                        FileFormat.GRAPHJSON};
                int res = JOptionPane.showOptionDialog(this,
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
                FileFormat ff = null;
                switch (res)
                {
                    case 0:
                        graphs = new ArrayList<DGraph>();
                        break;
                        
                    case 1:
                        graphs = DenoptimIO.readDENOPTIMGraphsFromSDFile(
                                file.getAbsolutePath());
                        break;
                        
                    case 2:
                        graphs = DenoptimIO.readDENOPTIMGraphsFromJSONFile(
                                file.getAbsolutePath());
                        break;
                }
    		} 
    	}
		catch (Exception e) 
        {
			e.printStackTrace();
			String msg = "<html><body width='%1s'>Could not read graph from "
			        + "file <br> "
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
			msg = msg + "</html>";
			JOptionPane.showMessageDialog(this,String.format(msg,400),
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
	private void loadCurrentGraphIdxToViewer(boolean keepSprites)
	{
		if (dnGraphLibrary == null)
		{
			JOptionPane.showMessageDialog(this,
	                "No list of graphs loaded.",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;
		}
		
		// Clears the "dnGraph" and GUI components, but keep memory of the 
    	// status of the graph of an easy recovery
    	clearCurrentSystem();
    	
		dnGraph = dnGraphLibrary.get(currGrphIdx);
		
		if (molLibrary.get(currGrphIdx).getAtomCount() > 0)
		{
		    visualPanel.loadDnGraphToViewer(dnGraphLibrary.get(currGrphIdx), 
		            molLibrary.get(currGrphIdx), keepSprites);
		}
		else
		{
		    visualPanel.loadDnGraphToViewer(dnGraphLibrary.get(currGrphIdx),
		            keepSprites);
		}
		
		enableGraphDependentButtons(true);
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
        visualPanel.clearCurrentSystem();
	}

//-----------------------------------------------------------------------------

	private void updateGraphListSpinner()
	{		
		graphNavigSpinner.setModel(new SpinnerNumberModel(currGrphIdx+1, 1, 
				dnGraphLibrary.size(), 1));
		totalGraphsLabel.setText(Integer.toString(dnGraphLibrary.size()));
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
        	loadCurrentGraphIdxToViewer(false);
        }
	}
	
//-----------------------------------------------------------------------------
	
	private void loadFragmentSpace() throws Exception
	{
		// Define the fragment space via a new dialog
		FSParamsDialog fsDefinitionDialog = new FSParamsDialog(this);
        fsDefinitionDialog.pack();
        fsDefinitionDialog.setVisible(true);
        visualPanel.resetFragViewerCardDeck();
        fragSpace = fsDefinitionDialog.makeFragSpace();
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
    
    private void removeCurrentDnGraph() throws DENOPTIMException
    {
    	// Clears the "dnGraph" and GUI components, but keep memory of the 
    	// status of the graph of an easy recovery, though since the old graph
    	// is being removed, the recovered data is not needed anymore.
        clearCurrentSystem();
    	
    	// Actual removal from the library
    	if (dnGraphLibrary.size()>0)
    	{
    		dnGraphLibrary.remove(currGrphIdx);
    		molLibrary.remove(currGrphIdx);
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
		    	loadCurrentGraphIdxToViewer(false); 
		    	updateGraphListSpinner();
    		}
    		else
    		{
    			currGrphIdx = -1;
    			//Spinner will be fixed by the deprotection routine
    			totalGraphsLabel.setText(Integer.toString(
    					dnGraphLibrary.size()));
				visualPanel.bringCardToTopOfMolViewer(
				        visualPanel.EMPTYCARDNAME);
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
  		
  		// WARNING: the dnGraph in the visualPanel should be in sync because any
  		// changes to it has resulted in an update of the graphViewer.
  		// Still, it is possible to introduce code modifications that make it
  		// go out of sync.
  		// Here, we ASSUME the graph displayed in the graphViewer component
  		// of the visualPanel is in sync with dnGraph. Therefore, we just
  		// rebuild the molecular viewer.
  		
  		if (updateMolViewer)
  		{
		    IAtomContainer mol = visualPanel.updateMolevularViewer();
		    if (mol != null)
		    {
                molLibrary.set(currGrphIdx, mol);
                mol.setProperty(DENOPTIMConstants.GMSGTAG,
                        "ManuallyBuilt");
        	} else {
        	    // Logging done within visualPanel
        		molLibrary.set(currGrphIdx, builder.newAtomContainer());
        	}
  			updateMolViewer = false;
  		}
  		
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

	/*
	 * This is needed to stop JUNG and Jmol threads upon closure of this
	 * gui card.
	 */
	public void dispose() 
	{
		visualPanel.dispose();
	}

//-----------------------------------------------------------------------------
	
	/**
	 * Dialog to configure one or more templates.
	 */
	private class ConfigTemplateDialog extends GUIModalDialog
	{   
	    private JPanel centralPanel;
	    private JScrollPane scrollPanel;
	    private JPanel listPanel;
	    
	//------------------------------------------------------------------------------

	    /**
	     * Creates a modal dialog with a specified number of limes allowing to 
	     * configure the properties of the templates to be created from graphs.
	     * @param parent the parent component calling this modal dialog.
	     * @param num the number of templates to configure.
	     */
	    public ConfigTemplateDialog(Component parent, int num)
	    {
	        setLocationRelativeTo(parent);
	        setTitle("Define Properties of Templates");
	        centralPanel = new JPanel();
	        centralPanel.setLayout(new BoxLayout(
	                centralPanel, SwingConstants.VERTICAL)); 

	        listPanel = new JPanel();
	        listPanel.setLayout(new BoxLayout(listPanel, SwingConstants.VERTICAL));
	        scrollPanel = new JScrollPane(listPanel);
	        for (int i=0; i<num; i++)
	            listPanel.add(new TemplateConfiguration(i));
	        centralPanel.add(scrollPanel);
	        
	        this.btnDone.setText("OK");
	        this.btnDone.setToolTipText("Confirm properties.");
	        this.btnDone.addActionListener(new ActionListener() {

	            @Override
	            public void actionPerformed(ActionEvent e) {
	                // Collect data from form
	                Map<String,List<Object>> selection = 
	                        new HashMap<String,List<Object>>();
	                selection.put(CONTRACTKEY, new ArrayList<>());
	                selection.put(BBTYPEKEY, new ArrayList<>());
	                for (Component c : listPanel.getComponents())
	                {
	                    if (c instanceof TemplateConfiguration)
	                    {
	                        TemplateConfiguration tc = 
	                                ((TemplateConfiguration) c);
	                        selection.get(CONTRACTKEY).add(
	                                tc.contractCmb.getSelectedItem());
	                        selection.get(BBTYPEKEY).add(
	                                tc.bbTypeCmb.getSelectedItem());
	                    }    
	                }
	                result = selection;
	                close();
	            }
	        });
	        
	        this.btnCanc.setEnabled(true);
	        this.btnCanc.setVisible(true);
	        this.btnCanc.setToolTipText("Abandon");
	        
	        super.addToCentralPane(centralPanel);
	    }
	    
//------------------------------------------------------------------------------
	    
	    /**
	     * Utility class for a form to configure a template via the GUI.
	     *
	     */
	    private class TemplateConfiguration extends JPanel
	    {
	        JComboBox<ContractLevel> contractCmb = new JComboBox<ContractLevel>(
	                ContractLevel.values());

	        private String contractTolTip = "<html><body width='%1s'>"
	                + "Speicfy the type of contract of the template, i.e., "
	                + "to what "
	                + "extent the graph embedded in the template can change in "
	                + "structure and/or identity of the vertexes.</html>";
	        
	        JComboBox<BBType> bbTypeCmb = new JComboBox<BBType>(BBType.values());

	        private String bbtTolTip = "<html><body width='%1s'>"
	                + "Speicfy the type of contract of the template, i.e., "
	                + "to what "
	                + "extent the graph embedded in the template can change in "
	                + "structure and/or identity of the vertexes.</html>";
	        
	        private int szTolTip = 250;
	        
	        /**
	         * Constructor
	         * @param id 0-based ID to be converted to 1-based
	         * @return
	         */
	        public TemplateConfiguration(int id)
	        {
	            super();
	            this.add(new JLabel("#" + (id+1)));
	            
	            JLabel contractLbl = new JLabel("  Contract:");
	            contractLbl.setToolTipText(String.format(contractTolTip,
	                    szTolTip));
	            this.add(contractLbl);
	            contractCmb.setToolTipText(String.format(contractTolTip,
	                    szTolTip));
	            this.add(contractCmb);
	            
	            JLabel bbtLbl = new JLabel("  Building Block Type:");
	            bbtLbl.setToolTipText(String.format(bbtTolTip,szTolTip));
	            this.add(bbtLbl);
	            bbTypeCmb.setToolTipText(String.format(bbtTolTip,szTolTip));
	            bbTypeCmb.setSelectedItem(BBType.FRAGMENT);
	            this.add(bbTypeCmb);
	            
	            //Other properties should be added here
	            
	        }
	    }
	}
		
//-----------------------------------------------------------------------------
  	
}
