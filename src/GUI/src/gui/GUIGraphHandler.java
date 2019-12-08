package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BoxLayout;
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

import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMVertex;


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
	private static final long serialVersionUID = -8303012362366503382L;
	
	/**
	 * Unique identified for instances of this handler
	 */
	public static AtomicInteger graphHandlerTabUID = new AtomicInteger(1);
	
	/**
	 * The currently loaded list of graphs
	 */
	private ArrayList<DENOPTIMGraph> dnGraphLibrary =
			new ArrayList<DENOPTIMGraph>();
	
	/**
	 * The currently loaded graph as DENOPTIM object
	 */
	private DENOPTIMGraph dnGraph;
	
	/**
	 * The currently loaded graph as GRaphStream object
	 */
	private Graph graph;
	
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
	
	private JPanel centralPanel;
	private GraphViewerPanel graphPanel;
	//private GsTest graphPanel;
	private JPanel graphCtrlPane;
	private JPanel graphNavigPane;
	private JPanel graphNavigPane2;
	private JPanel graphNavigPane3;
	
	private JButton btnAddGraph;
	private JButton btnGraphDel;
	
	private JButton btnOpenGraphs;
	
	private JSpinner graphNavigSpinner;
	private JLabel totalGraphsLabel;
	private final GraphSpinnerChangeEvent graphSpinnerListener = 
												new GraphSpinnerChangeEvent();
	
	private JPanel pnlAddVrtx;
	private JButton btnAddVrtx;
	
	private JPanel pnlDelSel;
	private JButton btnDelSel;
	
	private JPanel pnlSaveEdits;
	private JButton btnSaveEdits;
	
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
		// - (Center) graph viewer
		// - (East) graph controls
		// - (South) general controls (load, save, close)
		
		// The graph viewer goes all in here	
		graphPanel = new GraphViewerPanel();
		//graphPanel = new GsTest();
		this.add(graphPanel,BorderLayout.CENTER);
		
        // Jmol viewer panel
        /*
        jmolPanel = new JmolPanel();
        jmolPanel.setPreferredSize(new Dimension(400, 400));
        centralPanel.add(jmolPanel,BorderLayout.CENTER);
        */
       
		// General panel on the right: it containing all controls
        graphCtrlPane = new JPanel();
        graphCtrlPane.setVisible(true);
        graphCtrlPane.setLayout(new BoxLayout(graphCtrlPane, SwingConstants.VERTICAL));
        graphCtrlPane.add(new JSeparator());

        // Controls to navigate the list of dnGraphs
        graphNavigPane = new JPanel();
        graphNavigPane2 = new JPanel();
        graphNavigPane3 = new JPanel();
        JLabel navigationLabel1 = new JLabel("Graph # ");
        JLabel navigationLabel2 = new JLabel("Current library size: ");
        totalGraphsLabel = new JLabel("0");
        
		graphNavigSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 0, 1));
		graphNavigSpinner.setToolTipText("Move to graph number # in the currently loaded library.");
		graphNavigSpinner.setPreferredSize(new Dimension(75,20));
		graphNavigSpinner.addChangeListener(graphSpinnerListener);
        graphNavigPane.add(navigationLabel1);
		graphNavigPane.add(graphNavigSpinner);
		graphCtrlPane.add(graphNavigPane);
		
        graphNavigPane2.add(navigationLabel2);
        graphNavigPane2.add(totalGraphsLabel);
		graphCtrlPane.add(graphNavigPane2);
		
		btnAddGraph = new JButton("Add");
		btnAddGraph.setToolTipText("Starts creation of a new graph.");
		btnAddGraph.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//TODO
			}
		});
		btnGraphDel = new JButton("Remove");
		btnGraphDel.setToolTipText("Remove the present graph from the library.");
		btnGraphDel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					removeCurrentdnGraph();
					if (dnGraphLibrary.size()==0)
					{
						//TODO jmolPanel.viewer.evalString("zap");
					}
				} catch (DENOPTIMException e1) {
					System.out.println("Esception while removing the current graph:");
					e1.printStackTrace();
				}
			}
		});
		graphNavigPane3.add(btnAddGraph);
		graphNavigPane3.add(btnGraphDel);
		graphCtrlPane.add(graphNavigPane3);
		
		graphCtrlPane.add(new JSeparator());
		
		pnlAddVrtx = new JPanel();
		btnAddVrtx = new JButton("Add vertex");
		btnAddVrtx.setToolTipText("<html>Append a vertex to the delected "
				+ "attachment point<html>");
		btnAddVrtx.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//TODO: get selected
				ArrayList<DENOPTIMAttachmentPoint> selectedAps = 
						new ArrayList<DENOPTIMAttachmentPoint>();
				if (selectedAps.size() == 0)
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
					//TODO 
					
			        // Protect the temporary "dnGraph" obj
			        unsavedChanges = true;
			        protectEditedSystem();
				}
			}
		});
		pnlAddVrtx.add(btnAddVrtx);
		graphCtrlPane.add(pnlAddVrtx);
		
		pnlDelSel = new JPanel();
		btnDelSel = new JButton("Remove vertex");
		btnDelSel.setToolTipText("<html>Removes all selected vertexes from the "
				+ "system.<br><br><b>WARNING:</b> this action cannot be "
				+ "undone!");
		btnDelSel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				//TODO: get selected
				ArrayList<DENOPTIMVertex> selectedVrtx = 
						new ArrayList<DENOPTIMVertex>();
				
				if (selectedVrtx.size() == 0)
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
					//TODO
					
			        // Protect the temporary "dnGraph" obj
			        unsavedChanges = true;
			        protectEditedSystem();
				}
			}
		});
		pnlDelSel.add(btnDelSel);
		graphCtrlPane.add(pnlDelSel);
		
		graphCtrlPane.add(new JSeparator());
		
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
		
		btnOpenGraphs = new JButton("Load Library of graphs",
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
		
		JButton btnSaveFrags = new JButton("Save Library of graphs",
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
					///TODO
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

	/**
	 * Imports graphs from file. 
	 * @param file the file to open
	 */
	public void importGraphsFromFile(File file)
	{	
		importGraphsFromFile(file, "SDF");
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Imports graphs from file. 
	 * @param file the file to open
	 * @param format the format (acceptable values are TXT or SDF)
	 */
	public void importGraphsFromFile(File file, String format)
	{	
		dnGraphLibrary = new ArrayList<DENOPTIMGraph>();
		try 
		{
			dnGraphLibrary = DenoptimIO.readDENOPTIMGraphsFromFile(
					file.getAbsolutePath(), format, hasFragSpace);	
			
			// Display the first
			currGrphIdx = 0;
			
			loadCurrentGraphIdxToViewer();
			updateGraphListSpinner();
			
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
		
		clearCurrentSystem();
		graph = convertDnGraphToGSGraph(dnGraphLibrary.get(currGrphIdx));
		//graphPanel.loadGraphToViewer(graph);
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
		Graph g = new SingleGraph("DENOPTIMGraph#"+dnG.getGraphId());
		
		//TODO: all 
		g.addNode("A");
		g.addNode("B");
		g.addNode("C");
		g.addNode("D");
		g.addNode("E");
		g.addEdge("AB","A","B");
		g.addEdge("AC","A","C");
		g.addEdge("AD","A","D");
		g.addEdge("AE","A","E");
		
		
		return g;
	}
	
//-----------------------------------------------------------------------------
	
	private void clearCurrentSystem()
	{
		// Get rid of currently loaded graph
		dnGraph = null;
		graph = null;
		
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
        	currGrphIdx = ((Integer) graphNavigSpinner.getValue()).intValue() - 1;
        	loadCurrentGraphIdxToViewer();
        }
	}
	
//-----------------------------------------------------------------------------

	private void deprotectEditedSystem()
	{
		btnSaveEdits.setEnabled(false);
		btnAddGraph.setEnabled(true);
		btnOpenGraphs.setEnabled(true);
		
		graphNavigSpinner.setModel(new SpinnerNumberModel(currGrphIdx+1, 1, 
				dnGraphLibrary.size(), 1));
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
    	// Takes care of "dnGraph" in GUI components
    	clearCurrentSystem();
    	
    	// Actual removal from the library
    	if (dnGraphLibrary.size()>0)
    	{
    		dnGraphLibrary.remove(currGrphIdx);
    		int libSize = dnGraphLibrary.size();
    		
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
	    	
	  		// Release constraints
	        deprotectEditedSystem();
    	}
    }

//-----------------------------------------------------------------------------

  	private void saveUnsavedChanges() 
  	{	      		
  		// Overwrite dnGraph in library
  		dnGraphLibrary.set(currGrphIdx, dnGraph);
        
        // Reload dnGraph from library to refresh viewer
    	loadCurrentGraphIdxToViewer();
  		
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
