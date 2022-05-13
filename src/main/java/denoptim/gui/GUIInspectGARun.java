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


import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Ellipse2D;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.editor.ChartEditor;
import org.jfree.chart.editor.ChartEditorManager;
import org.jfree.chart.entity.PlotEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import com.google.common.io.Files;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.files.FileUtils;
import denoptim.graph.APClass;
import denoptim.graph.CandidateLW;
import denoptim.io.DenoptimIO;
import denoptim.logging.CounterID;
import denoptim.logging.Monitor;
import denoptim.utils.GenUtils;


/**
 * A panel that allows to inspect the output of an artificial evolution 
 * experiment. 
 * 
 * @author Marco Foscato
 */

public class GUIInspectGARun extends GUICardPanel
{
	/**
	 * Version UID
	 */
	private static final long serialVersionUID = -8303012362366503382L;
	
	/**
	 * Unique identified for instances of this handler
	 */
	public static AtomicInteger evolInspectorTabUID = new AtomicInteger(1);
	
	private JPanel ctrlPanel;
	private JPanel ctrlPanelRow1;
	private JPanel ctrlPanelRow1Left;
	private JPanel ctrlPanelRow1Right;
    private JPanel ctrlPanelRow2Left;
	private JSplitPane centralPanel;
	private JSplitPane rightPanel;
    private JPanel rightUpPanel;
    private JPanel rightDownPanel;
	private MoleculeViewPanel molViewer;
	
	private File srcFolder;
	
	private ArrayList<CandidateLW> allIndividuals;
	private int molsWithFitness = 0;
	private JLabel lblTotItems;
	private Map<Integer,CandidateLW> candsWithFitnessMap;
	
	// WARNING: itemId in the map is "j" and is just a 
	// locally generated unique 
	// identifier that has NO RELATION to generation/molId/fitness
	// The Map 'candsWithFitnessMap' serve specifically to convert
	// the itemId 'j' into a reference to the appropriate object.
	
	private DefaultXYDataset datasetAllFit = new DefaultXYDataset();;
	private DefaultXYDataset datasetSelected = new DefaultXYDataset();
	private DefaultXYDataset datasetPopMin = new DefaultXYDataset();	
	private DefaultXYDataset datasetPopMax = new DefaultXYDataset();
	private DefaultXYDataset datasetPopMean = new DefaultXYDataset();	
	private DefaultXYDataset datasetPopMedian = new DefaultXYDataset();
	private XYPlot evoPlot;
	private JFreeChart evoChart;
	private ChartPanel evoChartPanel;
	
    private JPopupMenu evoSeriesCheckList;
    private JButton evoSeriesBtn;
	
	private Map<CounterID,DefaultXYDataset> monitorDatasets = 
	        new HashMap<CounterID,DefaultXYDataset>();
	
    private XYPlot monitorPlot;
	private JFreeChart monitorChart;
	private ChartPanel monitorChartPanel;
	
	private ScrollableJPupupMenu monitorSeriesCheckList;
	private JButton monitorSeriesBtn;
	
	
	private final String NL = System.getProperty("line.separator");

	/**
	 * Button offering the possibility to load the graph inspector for a 
	 * selected item.
	 */
	private JButton openSingleGraph;
	
	/**
	 * Storage of pathname to the item selected in the chart. This is used to
	 * enable loading the graph inspector at any time after selection of the
	 * item in the chart.
	 */
    private String pathToSelectedItem;
    
    /**
     * Button offering the possibility to load the graphs of the population
     * at a given time (i.e., generation).
     */
    private JButton openGeneratinGraphs;
    
    /**
     * Pathways of population members collected by generation id. This info
     * is taken from the generation summary files, so in absence of such file 
     * for a generation, that generation will not be present here.
     */
    private Map<Integer,List<String>> candsPerGeneration;
    
    /**
     * Predefined list of data series colors.
     */
    private final Color[] colors = new Color[] {Color.black, Color.blue, 
            Color.cyan, Color.darkGray, Color.green, Color.magenta, 
            Color.orange, Color.pink, Color.red};
    
    /**
     * Predefined line strokes 
     */
    private final BasicStroke[] strokes = {
            new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                  0.0f),
            new BasicStroke(
                    1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                    1.0f, new float[] {4f, 2f}, 0.0f),
            new BasicStroke(
                    1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                    1.0f, new float[] {6f, 2f}, 0.0f),
            new BasicStroke(
                    1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                    1.0f, new float[] {6f, 2f, 4f, 2f}, 0.0f),
            new BasicStroke(
                    1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                    1.0f, new float[] {6f, 2f, 4f, 2f, 4f, 2f}, 0.0f),
            new BasicStroke(
                    1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                    1.0f, new float[] {6f, 2f, 4f, 2f, 4f, 2f, 4f, 2f}, 0.0f),
            new BasicStroke(
                    1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                    1.0f, new float[] {6f, 2f, 6f, 2f, 4f, 2f}, 0.0f),
            new BasicStroke(
                    1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                    1.0f, new float[] {6f, 2f, 6f, 2f, 4f, 2f, 4f, 2f}, 0.0f),
            new BasicStroke(
                    1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                    1.0f, new float[] {6f, 2f, 6f, 2f, 6f, 2f,
                            4f, 2f, 4f, 2f, 4f, 2f}, 0.0f),
    };
    
    /**
     * Records the name of the first series in the monitor plot to facilitate
     * its recovery upon chart reset.
     */
    private String nameFirstMonitorSeries = "unset";
	
//-----------------------------------------------------------------------------
	
	/**
	 * Constructor
	 */
	public GUIInspectGARun(GUIMainPanel mainPanel)
	{
		super(mainPanel, "GARun Inspector #" 
					+ evolInspectorTabUID.getAndIncrement());
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
		
		// The Card has
		// -> its own toolbar
		// -> a central panel vertically divided in two
		//    |-> mol/graph viewers? (LEFT)
		//    |-> plots (RIGHT)
		
		// Creating local tool bar
		ctrlPanelRow1Left = new JPanel(new FlowLayout(FlowLayout.LEFT));
		
	    evoSeriesCheckList = new JPopupMenu();
        evoSeriesBtn = new JButton("Show/Hide Population Stats");
        evoSeriesBtn.setComponentPopupMenu(evoSeriesCheckList);
        evoSeriesBtn.setToolTipText(String.format(
                "<html><body width='%1s'>Click to select which population "
                + "statistics to plot in the top plot.",300));
        evoSeriesBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                evoSeriesCheckList.show(evoSeriesBtn, 0, 0);
            }
        });
        ctrlPanelRow1Left.add(evoSeriesBtn);
		
		monitorSeriesCheckList = new ScrollableJPupupMenu();
        monitorSeriesBtn = new JButton("Show/Hide Monitor Series");
        monitorSeriesBtn.setToolTipText(String.format(
                "<html><body width='%1s'>Click to select which monitored event "
                + "counts to plot in the bottom plot.",300));
        monitorSeriesBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                monitorSeriesCheckList.showMenu(monitorSeriesBtn, 0, 0);
                /*
                LineExamplePanel p = new LineExamplePanel();
                p.showDialog();
                */
            }
        });
		ctrlPanelRow1Left.add(monitorSeriesBtn);
		
	    JButton rstView = new JButton("Reset Chart Views");
        rstView.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                evoPlot.getDomainAxis().setAutoRange(true);
                evoPlot.getRangeAxis().setAutoRange(true);
                evoPlot.getDomainAxis().setLowerBound(-0.5);
                for (int iItem=0; iItem<evoSeriesCheckList.getComponentCount(); 
                        iItem++)
                {
                    Component c = evoSeriesCheckList.getComponent(iItem);
                    if (c instanceof JCheckBoxMenuItem)
                    {
                        // NB: this sets the default series displayed upon reset
                        if (((JCheckBoxMenuItem) c).getText().startsWith("Max")
                            || ((JCheckBoxMenuItem) c).getText().startsWith("Min"))
                            ((JCheckBoxMenuItem) c).setSelected(true);
                        else
                            ((JCheckBoxMenuItem) c).setSelected(false);
                    }
                }
                
                monitorPlot.getDomainAxis().setAutoRange(true);
                monitorPlot.getRangeAxis().setAutoRange(true);
                monitorPlot.getDomainAxis().setLowerBound(-0.5);
                for (JCheckBox cb : monitorSeriesCheckList.getAllBChekBoxes())
                {
                    // NB: this sets the default series displayed upon reset
                    if (cb.getText().startsWith(nameFirstMonitorSeries))
                        cb.setSelected(true);
                    else
                        cb.setSelected(false);
                }
            }
        });
        ctrlPanelRow1Left.add(rstView);
		
		// Done with left part of bar, not the right-hand part...
	
		ctrlPanelRow1Right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		lblTotItems = new JLabel("No item loaded");
		lblTotItems.setHorizontalAlignment(SwingConstants.RIGHT);
		lblTotItems.setPreferredSize(new Dimension(300,28));
		ctrlPanelRow1Right.add(lblTotItems);
		
        ctrlPanelRow2Left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        openSingleGraph = new JButton("Open Candidate's Graph");
        openSingleGraph.setEnabled(false); //Enables only upon selection of an item
        openSingleGraph.setToolTipText("Open a new tab for inspecting the "
                + "graph representation of the selected candidate.");
        openSingleGraph.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mainPanel.setCursor(Cursor.getPredefinedCursor(
                        Cursor.WAIT_CURSOR));
                GUIGraphHandler graphPanel = new GUIGraphHandler(mainPanel);
                mainPanel.add(graphPanel);
                graphPanel.importGraphsFromFile(new File(pathToSelectedItem));
                mainPanel.setCursor(Cursor.getPredefinedCursor(
                        Cursor.DEFAULT_CURSOR));
            }
        });
        ctrlPanelRow2Left.add(openSingleGraph);
        
        openGeneratinGraphs = new JButton("Open Population Graphs");
        openGeneratinGraphs.setToolTipText(String.format(
                "<html><body width='%1s'>Open a "
                + "new tab for inspecting the graph representation of all "
                + "members of the population. "
                + "Opens a dialog to specify which generation to consider of "
                + "the selected candidate.",300));
        openGeneratinGraphs.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Ask which generation
                GenerationChoiceDialog dialog = new GenerationChoiceDialog();
                Object res = dialog.showDialog();
                if (res==null)
                {
                    return;
                }
                int genId = -1;
                try {
                    genId = Integer.parseInt((String) res);
                } catch (Throwable t) {
                    JOptionPane.showMessageDialog(openGeneratinGraphs,
                            String.format("<html><body width='%1s'>String '" 
                                    + res+ "' could not be used to identify "
                                    + "a generation. "
                                    + "Please, type an integer 0, 1, 2,...", 300),
                            "Error",
                            JOptionPane.PLAIN_MESSAGE,
                            UIManager.getIcon("OptionPane.errorIcon"));
                    return;
                }
                if (!candsPerGeneration.containsKey(genId))
                {
                    JOptionPane.showMessageDialog(openGeneratinGraphs,
                            String.format("<html><body width='%1s'>" 
                                    + "List of members of generation " + genId 
                                    + " could not be found. Generation summary "
                                    + "missing or not properly formatted.",300),
                            "Error",
                            JOptionPane.PLAIN_MESSAGE,
                            UIManager.getIcon("OptionPane.errorIcon"));
                    return;
                }
                
                // Collect population
                String pathtoTmpFile = Utils.getTempFile("population_al_gen" 
                        + genId + ".sdf");
                mainPanel.setCursor(Cursor.getPredefinedCursor(
                        Cursor.WAIT_CURSOR));
                try
                {
                    FileUtils.mergeIntoOneFile(pathtoTmpFile,
                            candsPerGeneration.get(genId));
                    mainPanel.setCursor(Cursor.getPredefinedCursor(
                            Cursor.DEFAULT_CURSOR));
                } catch (IOException e1)
                {
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(openGeneratinGraphs,
                            String.format("<html><body width='%1s'>" 
                                    + "List of members of generation " + genId 
                                    + " could not be collected. Hint: "
                                    + e1.getMessage(), 300),
                            "Error",
                            JOptionPane.PLAIN_MESSAGE,
                            UIManager.getIcon("OptionPane.errorIcon"));
                    mainPanel.setCursor(Cursor.getPredefinedCursor(
                            Cursor.DEFAULT_CURSOR));
                    return;
                }
                
                // Launch the GUI component to visualize population
                GUIGraphHandler graphPanel = new GUIGraphHandler(mainPanel);
                mainPanel.add(graphPanel);
                graphPanel.importGraphsFromFile(new File(pathtoTmpFile));
                
                //Log
                System.out.println("File collecting members of the population "
                        + "at generation " + genId + ": "+ pathtoTmpFile);
            }
        });
        ctrlPanelRow2Left.add(openGeneratinGraphs);
        

        ctrlPanelRow1 = new JPanel();
        GroupLayout lyoCtrlPanelRow1 = new GroupLayout(ctrlPanelRow1);
        ctrlPanelRow1.setLayout(lyoCtrlPanelRow1);
        lyoCtrlPanelRow1.setAutoCreateGaps(true);
        lyoCtrlPanelRow1.setAutoCreateContainerGaps(true);
        lyoCtrlPanelRow1.setHorizontalGroup(
                lyoCtrlPanelRow1.createSequentialGroup()
                    .addComponent(ctrlPanelRow1Left)
                    .addComponent(ctrlPanelRow1Right));
        lyoCtrlPanelRow1.setVerticalGroup(lyoCtrlPanelRow1.createParallelGroup()
                    .addComponent(ctrlPanelRow1Left)
                    .addComponent(ctrlPanelRow1Right));
        
        ctrlPanel =  new JPanel();
        GroupLayout lyoCtrlPanel = new GroupLayout(ctrlPanel);
        ctrlPanel.setLayout(lyoCtrlPanel);
        lyoCtrlPanel.setAutoCreateGaps(true);
        lyoCtrlPanel.setAutoCreateContainerGaps(true);
        lyoCtrlPanel.setHorizontalGroup(lyoCtrlPanel.createParallelGroup()
                    .addComponent(ctrlPanelRow1)
                    .addComponent(ctrlPanelRow2Left));
        lyoCtrlPanel.setVerticalGroup(lyoCtrlPanel.createSequentialGroup()
                    .addComponent(ctrlPanelRow1)
                    .addComponent(ctrlPanelRow2Left));
        
        this.add(ctrlPanel,BorderLayout.NORTH);
		
		// Setting structure of panels structure that goes in the center	
		centralPanel = new JSplitPane();
		centralPanel.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		centralPanel.setOneTouchExpandable(true);
		rightPanel = new JSplitPane();
		rightPanel.setOrientation(JSplitPane.VERTICAL_SPLIT);
		rightPanel.setDividerLocation(300);
		rightUpPanel = new JPanel(); 
		rightUpPanel.setLayout(new BorderLayout());
        rightDownPanel = new JPanel(); 
        rightDownPanel.setLayout(new BorderLayout());
        rightPanel.setTopComponent(rightUpPanel);
        rightPanel.setBottomComponent(rightDownPanel);
        
		centralPanel.setRightComponent(rightPanel);
		molViewer = new MoleculeViewPanel();
		centralPanel.setLeftComponent(molViewer);
		centralPanel.setDividerLocation(300);
		this.add(centralPanel,BorderLayout.CENTER);

		// Button to the bottom of the card
		ButtonsBar commandsPane = new ButtonsBar();
		this.add(commandsPane, BorderLayout.SOUTH);
		JButton btnCanc = new JButton("Close Tab");
		btnCanc.setToolTipText("Closes this GARun Inspector.");
		btnCanc.addActionListener(new removeCardActionListener(this));
		commandsPane.add(btnCanc);
		
		JButton btnHelp = new JButton("?");
		btnHelp.setToolTipText("<html>Hover over buttons and fields "
                    + "to get a tip.<br>"
                    + "Click the '?' button for further instructions.</html>");
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String txt = "<html><body width='%1s'>"
	                    + "<p>Selection of candidates:"
	                    + "<ul>"
	                    + "<li>Click on a dot in the chart to load the "
	                    + "molecular "
	                    + "representation of that candidates.</li>"
	                    + "<li>Click away from any dot to reset the molecular "
	                    + "viewer."
	                    + "</li></ul></p>"
	                    + "<p>Chart view:"
	                    + "<ul>"
	                    + "<li>zoom in: click-and-drag "
	                    + "from the top-left to the bottom-right "
	                    + "corners of the new region of the plot to focus on."
	                    + "</li>"
	                    + "<li>Use the <code>Reset Chart View</code> to reset "
	                    + "the view.</li>"
	                    + "<li>Right-click to get advanced controls and "
	                    + "options</li>"
	                    + "</ul></p>"
	                    + "</body></html>";
				JOptionPane.showMessageDialog(btnHelp, String.format(txt, 300),
                    "Tips",
                    JOptionPane.PLAIN_MESSAGE);
			}
		});
		commandsPane.add(btnHelp);
		
	}
	
//------------------------------------------------------------------------------
    
	/**
	 * Modal dialog that asks the user for a generation number.
	 */
    private class GenerationChoiceDialog extends GUIModalDialog
    {
        public GenerationChoiceDialog()
        {
            super(false);
            this.setBounds(150, 150, 500, 200);
            this.setTitle("Choose Generation");
            
            Dimension sizeNameFields = new Dimension(200,
                    (int) (new JTextField()).getPreferredSize().getHeight());
            
            JPanel rowOne = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel lblVarName = new JLabel("Generation Number:");
            JTextField txtVarName = new JTextField();
            txtVarName.setPreferredSize(sizeNameFields);
            rowOne.add(lblVarName);
            rowOne.add(txtVarName);
            
            addToCentralPane(rowOne);
            
            this.btnDone.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (txtVarName.getText().equals(""))
                    {
                        result = null;
                    } else {
                        result = txtVarName.getText();
                    }
                    close();
                }
            });
            this.getRootPane().setDefaultButton(this.btnDone);
            this.btnCanc.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    result = null;
                    close();
                }
            });
        }
    }
	
//-----------------------------------------------------------------------------

	public void importGARunData(File file, JComponent parent) 
	{
		if (!file.isDirectory() || !file.exists())
		{
			JOptionPane.showMessageDialog(parent,
	                "Could not read data from folder '" + file + "'!",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;
		}

		mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		srcFolder = file;
		
		System.out.println("Importing data from '" + srcFolder + "'...");
		
		candsPerGeneration = new HashMap<Integer,List<String>>();
		Map<Integer,double[]> popProperties = new HashMap<Integer,double[]>();
		allIndividuals = new ArrayList<CandidateLW>();
		for (File genFolder : file.listFiles(new FileFilter() {
			
			@Override
			public boolean accept(File pathname) {
				if (pathname.getName().startsWith("Gen")
						&& pathname.isDirectory())
				{
					return true;
				}
				return false;
			}
		}))
		{			
			//WARNING: assuming folders are named "Gen.*"
			int genId = Integer.parseInt(genFolder.getName().substring(3));
			int padSize = genFolder.getName().substring(3).length();
			
			String zeroedGenId = GenUtils.getPaddedString(padSize,genId);
			
			// Read Generation summaries
			File genSummary = new File(genFolder 
					+ System.getProperty("file.separator") 
					+ "Gen" + zeroedGenId + ".txt");
			
			System.out.println("Reading "+genSummary);
			boolean readPopMembers = false;
			try {
				popProperties.put(genId, DenoptimIO.readPopulationProps(
						genSummary));
				readPopMembers = true;
			} catch (DENOPTIMException e2) {
				JOptionPane.showMessageDialog(parent,
		                "<html>File '" + genSummary + "' not found!<br>"
		                + "There will be holes in the min/max/mean profile."
		                + "</html>",
		                "Error",
		                JOptionPane.PLAIN_MESSAGE,
		                UIManager.getIcon("OptionPane.errorIcon"));
				popProperties.put(genId, new double[] {
						Double.NaN, Double.NaN, Double.NaN, Double.NaN});
			}
			
			// Read candidates
			for (File fitFile : genFolder.listFiles(new FileFilter() {
				
				@Override
				public boolean accept(File pathname) {
					if (pathname.getName().endsWith(
					       DENOPTIMConstants.FITFILENAMEEXTOUT))
					{
						return true;
					}
					return false;
				}
			}))
			{
				CandidateLW one;
				try {
				    //WARNING: here we assume one candidate per file
					one = DenoptimIO.readLightWeightCandidate(fitFile).get(0);
				} catch (DENOPTIMException e1) {
					e1.printStackTrace();
					JOptionPane.showMessageDialog(parent,
			                "Could not read data from to '" + fitFile + "'! "
			                + NL + e1.getMessage(),
			                "Error",
			                JOptionPane.PLAIN_MESSAGE,
			                UIManager.getIcon("OptionPane.errorIcon"));
					return;
				}
				if (one.hasFitness())
				{
					molsWithFitness++;
				}
				one.setGeneration(genId);
				allIndividuals.add(one);
			}
			
			//Read population members from summary
			if (readPopMembers)
			{
                try
                {
                    List<String> membersPathnames = new ArrayList<String>(
                            DenoptimIO.readPopulationMemberPathnames(genSummary));
                    candsPerGeneration.put(genId, membersPathnames);
                } catch (DENOPTIMException e1)
                {
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(parent,
                            String.format("<html><body width='%1s'>"
                                + "File '" + genSummary + "' has been found, "
                                + "but pathnames to population members could "
                                + "not be read.</html>", 400),
                            "Error",
                            JOptionPane.PLAIN_MESSAGE,
                            UIManager.getIcon("OptionPane.errorIcon"));
                }
			}
		}
		
		System.out.println("Imported "+allIndividuals.size()+" individuals.");
		
		lblTotItems.setText("Found "+allIndividuals.size()+" candidates ("
				+molsWithFitness+" with fitness)");
		
		// Process data and organize then into series for the plot 
        double[][] candsWithFitnessData = new double[2][molsWithFitness];
        candsWithFitnessMap = new HashMap<Integer,CandidateLW>();
        int j = -1;
        for (int i=0; i<allIndividuals.size(); i++)
        {
        	CandidateLW item = allIndividuals.get(i);
        	if (!item.hasFitness())
        	{
        		continue;
        	}
        	
        	// WARNING: itemId in the data is "j" and is just a unique 
        	// identifier that has NO RELATION to generation/molId/fitness
        	// The Map 'candsWithFitnessMap' serve specifically to convert
        	// the itemId 'j' into a DENOPTIMMolecule
        	
        	j++;
        	candsWithFitnessMap.put(j, item);
        	candsWithFitnessData[0][j] = item.getGeneration();
        	candsWithFitnessData[1][j] = item.getFitness();
        }
		datasetAllFit.addSeries("Candidates_with_fitness", candsWithFitnessData);
		
		// We sort the list of individuals by fitness and generation so that we 
		// can quickly identify overlapping items
		Collections.sort(allIndividuals, new PlottedCandidatesComparator());
		
		int numGen = popProperties.keySet().size();
		double[][] popMin = new double[2][numGen];
		double[][] popMax = new double[2][numGen];
		double[][] popMean = new double[2][numGen];
		double[][] popMedian = new double[2][numGen];
		for (int i=0; i<numGen; i++)
		{
			double[] values = popProperties.get(i);
			popMin[0][i] = i;
			popMin[1][i] = values[0];
			popMax[0][i] = i;
			popMax[1][i] = values[1];
			popMean[0][i] = i;
			popMean[1][i] = values[2];
			popMedian[0][i] = i;
			popMedian[1][i] = values[3];
			
		}
		datasetPopMin.addSeries("Population_min", popMin);
		datasetPopMax.addSeries("Population_max", popMax);
		datasetPopMean.addSeries("Population_mean", popMean);
		datasetPopMedian.addSeries("Population_median", popMedian);
	
		
		//TODO: somehow collect and display the candidates that hit a mol error
		//      Could it be a histogram (#failed x gen) below the evolution plot
		
        NumberAxis xAxis = new NumberAxis("Generation");
        xAxis.setRange(-0.5, numGen-0.5); //"-" because numGen includes 0
        xAxis.setAutoRangeIncludesZero(false);
        NumberAxis yAxis = new NumberAxis("Fitness");
        yAxis.setAutoRangeIncludesZero(false);
		evoPlot = new XYPlot(null,xAxis,yAxis,null);
		
		evoChart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT,evoPlot,false);
		
		evoPlot.getDomainAxis().setLowerBound(-0.5); //min X-axis
		evoPlot.setBackgroundPaint(Color.WHITE);
		evoPlot.setDomainGridlinePaint(Color.LIGHT_GRAY);
		evoPlot.setRangeGridlinePaint(Color.LIGHT_GRAY);
		evoPlot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);
		// The following brings the points for datasets of selected
		// items and the main dataset in front of the
		// mean/min/max/median lines, thus allowing selection of points
		// even in presence of these line series, which would otherwise (if in
		// front-most layer) prevent the mouse clicked event to identify the 
		// item from datasetAllFit, thus preventing to display the molecular
		// representation of that item.
		evoPlot.setDatasetRenderingOrder(DatasetRenderingOrder.REVERSE);
		
		// dataset of selected items
        Shape shape0 = new Ellipse2D.Double(
                 -GUIPreferences.chartPointSize*1.1/2.0,
                 -GUIPreferences.chartPointSize*1.1/2.0,
                 GUIPreferences.chartPointSize*1.1, 
                 GUIPreferences.chartPointSize*1.1);
        XYLineAndShapeRenderer renderer0 = 
                new XYLineAndShapeRenderer(false, true);
        //NB: here is where we define that the selected ones are at '0'
        // Search for the 'HERE@HERE' string to find where I use this assumption
        evoPlot.setDataset(0, datasetSelected); 
        evoPlot.setRenderer(0, renderer0);
        renderer0.setSeriesShape(0, shape0);
        renderer0.setSeriesPaint(0, Color.red);
        renderer0.setSeriesFillPaint(0, Color.red);
        renderer0.setSeriesOutlinePaint(0, Color.BLACK);
        renderer0.setUseOutlinePaint(true);
        renderer0.setUseFillPaint(true);
        
		// main dataset (all items with fitness)
		Shape shape1 = new Ellipse2D.Double(
				 -GUIPreferences.chartPointSize/2.0,
	             -GUIPreferences.chartPointSize/2.0,
	             GUIPreferences.chartPointSize, 
	             GUIPreferences.chartPointSize);
		XYLineAndShapeRenderer renderer1 = 
		        new XYLineAndShapeRenderer(false, true);
        evoPlot.setDataset(1, datasetAllFit);
        evoPlot.setRenderer(1, renderer1);
        renderer1.setSeriesShape(0, shape1);
        renderer1.setSeriesPaint(0, new Color(192, 192, 192, 60));
        renderer1.setSeriesFillPaint(0, new Color(192, 192, 192, 60));
        renderer1.setSeriesOutlinePaint(0, Color.GRAY);
        renderer1.setUseOutlinePaint(true);
        renderer1.setUseFillPaint(true);
        

        // Collect the ooptional series for an easy selection in the menu list
        evoSeriesCheckList = new JPopupMenu();
        
        // min fitness in the population
        XYLineAndShapeRenderer renderer2 = 
        		new XYLineAndShapeRenderer(true, false);
        evoPlot.setDataset(2, datasetPopMin);
        evoPlot.setRenderer(2, renderer2);
        renderer2.setSeriesPaint(0, Color.blue);
        //NB: 'rstView' button assumes this item string begins with "Min"
        JCheckBoxMenuItem cbmiMin = new JCheckBoxMenuItem(
                "Minimum Fitness in Population");
        cbmiMin.setForeground(Color.blue);
        cbmiMin.setSelected(true);
        cbmiMin.addItemListener(new ItemListener(){
            @Override
            public void itemStateChanged(ItemEvent e)
            {
                if (cbmiMin.isSelected())
                {
                    renderer2.setSeriesVisible(0, true);
                } else {
                    renderer2.setSeriesVisible(0, false);
                }                    
            }
        });
        evoSeriesCheckList.add(cbmiMin);
        
        // max fitness in the population
        XYLineAndShapeRenderer renderer3 = 
        		new XYLineAndShapeRenderer(true, false);
        evoPlot.setDataset(3, datasetPopMax);
        evoPlot.setRenderer(3, renderer3);
        renderer3.setSeriesPaint(0, Color.blue);
        //NB: 'rstView' button assumes this item string begins with "Max"
        JCheckBoxMenuItem cbmiMax = new JCheckBoxMenuItem(
                "Maximum Fitness in Population");
        cbmiMax.setForeground(Color.blue);
        cbmiMax.setSelected(true);
        cbmiMax.addItemListener(new ItemListener(){
            @Override
            public void itemStateChanged(ItemEvent e)
            {
                if (cbmiMax.isSelected())
                {
                    renderer3.setSeriesVisible(0, true);
                } else {
                    renderer3.setSeriesVisible(0, false);
                }                    
            }
        });
        evoSeriesCheckList.add(cbmiMax);
        
        // mean fitness in the population
        XYLineAndShapeRenderer renderer4 = 
        		new XYLineAndShapeRenderer(true, false);
        evoPlot.setDataset(4, datasetPopMean);
        evoPlot.setRenderer(4, renderer4);
        renderer4.setSeriesPaint(0, Color.red);
        renderer4.setSeriesStroke(0, new BasicStroke(
                2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                1.0f, new float[] {10.0f, 6.0f}, 0.0f));
        renderer4.setSeriesVisible(0, false);
        JCheckBoxMenuItem cbmiMean = new JCheckBoxMenuItem(
                "Mean of Fitness in Population");
        cbmiMean.setForeground(Color.red);
        cbmiMean.setSelected(false);
        cbmiMean.addItemListener(new ItemListener(){
            @Override
            public void itemStateChanged(ItemEvent e)
            {
                if (cbmiMean.isSelected())
                {
                    renderer4.setSeriesVisible(0, true);
                } else {
                    renderer4.setSeriesVisible(0, false);
                }                    
            }
        });
        evoSeriesCheckList.add(cbmiMean);
        
        // median fitness in the population
        XYLineAndShapeRenderer renderer5 = 
        		new XYLineAndShapeRenderer(true, false);
        evoPlot.setDataset(5, datasetPopMedian);
        evoPlot.setRenderer(5, renderer5);
        renderer5.setSeriesPaint(0, Color.decode("#22BB22"));   
        renderer5.setSeriesStroke(0, new BasicStroke(
                    2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                    1.0f, new float[] {3.0f, 6.0f}, 0.0f));
        renderer5.setSeriesVisible(0, false);
        JCheckBoxMenuItem cbmiMedian = new JCheckBoxMenuItem(
                "Median of Fitness in Population");
        cbmiMedian.setForeground(Color.decode("#22BB22"));
        cbmiMedian.setSelected(false);
        cbmiMedian.addItemListener(new ItemListener(){
            @Override
            public void itemStateChanged(ItemEvent e)
            {
                if (cbmiMedian.isSelected())
                {
                    renderer5.setSeriesVisible(0, true);
                } else {
                    renderer5.setSeriesVisible(0, false);
                }                    
            }
        });
        evoSeriesCheckList.add(cbmiMedian);
        
        
		// Create the actual panel that contains the chart
		evoChartPanel = new ChartPanel(evoChart);
		
		// Adapt chart size to the size of the panel
		rightUpPanel.addComponentListener(new ComponentAdapter() {
	        @Override
	        public void componentResized(ComponentEvent e) {
	        	evoChartPanel.setMaximumDrawHeight(e.getComponent().getHeight());
	        	evoChartPanel.setMaximumDrawWidth(e.getComponent().getWidth());
	        	evoChartPanel.setMinimumDrawWidth(e.getComponent().getWidth());
	        	evoChartPanel.setMinimumDrawHeight(e.getComponent().getHeight());
	        	// WARNING: this update is needed to make the new size affective
	        	// also after movement of the JSplitPane divider, which is
	        	// otherwise felt by this listener but the new sizes do not
	        	// take effect. 
	        	ChartEditor ce = ChartEditorManager.getChartEditor(evoChart);
	        	ce.updateChart(evoChart);
	        }
	    });
		
		// Setting toolTip when on top of an series item in the chart
		XYToolTipGenerator ttg = new XYToolTipGenerator() {
			@Override
			public String generateToolTip(XYDataset data, int sId, int itemId)
			{
			    CandidateLW itemOnTop = candsWithFitnessMap.get(itemId);
			    // Is there more than one item in the stack?
			    // One overlapping neighbor is enough to say there
			    // is more than one item in the tack.
			    List<CandidateLW> overlappingItems = getOverlappingItems(
			            itemOnTop, 2);
			    if (overlappingItems.size()>1)
			        return "Overlapping Items";
			    else
			        return candsWithFitnessMap.get(itemId).getName();
			}
		};
		renderer1.setDefaultToolTipGenerator(ttg);
		
		// Click-based selection of item, possibly displaying mol structure
		evoChartPanel.addChartMouseListener(new ChartMouseListener() {
			
			@Override
			public void chartMouseMoved(ChartMouseEvent e) {
				//nothing to do
			}
			
			@Override
			public void chartMouseClicked(ChartMouseEvent e) 
			{   
				if (e.getEntity() instanceof XYItemEntity)
				{
				    XYItemEntity i = (XYItemEntity) e.getEntity();
					XYDataset ds = ((XYItemEntity)e.getEntity()).getDataset();
					if (!ds.equals(datasetAllFit))
					{
						return;
					}
					
					int serId = ((XYItemEntity)e.getEntity()).getSeriesIndex();
					if (serId == 0)
					{
						int itemId = ((XYItemEntity) e.getEntity()).getItem();
						CandidateLW item = candsWithFitnessMap.get(itemId);
						
						// The even can carry only one item, but there could be 
						// many items overlapping each other.
						// Search for overlapping items and ask which one to the
						// user wants to see.
		                List<CandidateLW> overlappingItems = getOverlappingItems(
		                        item,25);
		                CandidateLW choosenItem = choseAmongPossiblyOverlapping(
		                        evoChartPanel, overlappingItems);
		                if (choosenItem!=null)
		                    renderViewWithSelectedItem(choosenItem);
					}
					//do we do anything if we select other series? not now...
				}
				else if (e.getEntity() instanceof PlotEntity)
				{
					renderViewWithoutSelectedItems();
				}
			}
		});
		
		rightUpPanel.add(evoChartPanel,BorderLayout.CENTER);
		
		buildAndFillMonitorPlot(file, parent);
		
		mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
	
//------------------------------------------------------------------------------

	/**
     * Modal dialog to display visual examples of line colors and strokes
     */
	/*
    private class LineExamplePanel extends GUIModalDialog
    {
        public LineExamplePanel()
        {
            super(false);
            this.setBounds(150, 150, 500, 200);
            this.setTitle("LineExamples");
            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, SwingConstants.VERTICAL));
            JScrollPane scrolPane = new JScrollPane(mainPanel);
            addToCentralPane(scrolPane);
            
            int iColor = 0;
            int iStroke = 0;
            for (int iSeries=0; iSeries<100; iSeries++)
            {
                Color color = colors[iColor];
                BasicStroke stroke = strokes[iStroke];
                JPanel linePanel = new JPanel() {
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        g.setColor(color);
                        ((Graphics2D) g).setStroke(stroke);
                        g.drawLine(10,10, 5000, 15);
                    };
                };
                JPanel locPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                mainPanel.add(linePanel);
                mainPanel.add(new JLabel("Color: "+ iColor+" Stroke: "+iStroke));
                
                iColor++;
                if (iColor >= colors.length)
                {
                    iColor = 0;
                    if (iStroke < strokes.length-1)
                    {
                        iStroke++;
                    } else {
                        iStroke = 0;
                    }
                }
            }
        }
	}
	*/

//------------------------------------------------------------------------------
	
	private void buildAndFillMonitorPlot(File runFolder, JComponent parent)
	{
	    File eaMonitorDumps = new File(runFolder + ".eaMonitor");
	    if (!eaMonitorDumps.exists())
	    {
	        rightDownPanel.add(new JLabel("Monitor file " + eaMonitorDumps
	                + "not found."));
	        return;
	    }
	    ArrayList<String> lines = null;
	    try
        {
            lines = DenoptimIO.readList(eaMonitorDumps.getAbsolutePath());
        } catch (DENOPTIMException e1)
        {
            e1.printStackTrace();
            rightDownPanel.add(new JLabel("Cannot read " + eaMonitorDumps));
            return;
        }
	    if (lines.size()<2)
	    {
	        rightDownPanel.add(new JLabel("Not enough data to plot"));
            return;
        }
	    
	    String[] words = lines.get(0).trim().split("\\s+");
	    String[] headers = IntStream.range(3, words.length)
                .mapToObj(i -> words[i])
                .toArray(String[]::new);
	    
	    // Prepare data storage structure...
	    List<double[][]> xyData = new ArrayList<double[][]>();
	    for (int iSeries=0; iSeries<headers.length; iSeries++)
	        xyData.add(new double[2][lines.size()-1]);
	    // ...and fill it with actual data
	    int iRecordsKept = -1;
	    for (int iRecordr=1; iRecordr<lines.size(); iRecordr++)
	    {
	        String line = lines.get(iRecordr).trim();
	        
	        // TODO: change. Now, we ignore dumps more frequent than generations
	        if (!line.startsWith("SUMMARY"))
	            continue;
	        
	        iRecordsKept++;
	        String[] values = line.split("\\s+");
	        for (int iSeries=3; iSeries<values.length; iSeries++)
	        {
	            xyData.get(iSeries-3)[0][iRecordsKept] = 
	                    Double.valueOf(values[2]); // generation ID
                xyData.get(iSeries-3)[1][iRecordsKept] = 
                        Double.valueOf(values[iSeries]); // counter value
	        }
	    }
	   
	    // Done with collecting data, not build the plot
	    
	    NumberAxis xAxis = new NumberAxis("Generation");
	    //"-0.5" because includes 0
	    xAxis.setRange(-0.5, Double.valueOf(iRecordsKept)-0.5);
	    xAxis.setAutoRangeIncludesZero(false);
	    // By default we show the first series (should be the number of attempts
	    // to create candidates)
	    NumberAxis yAxis = new NumberAxis("#");
	    yAxis.setAutoRangeIncludesZero(false);
	    
        monitorPlot = new XYPlot(null,xAxis,yAxis,null);

        monitorChart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, 
                monitorPlot, false);

        monitorPlot.getDomainAxis().setLowerBound(-0.5); //min X-axis
        monitorPlot.setBackgroundPaint(Color.WHITE);
        monitorPlot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        monitorPlot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        // NB: ineffective
        //monitorPlot.setDomainGridlinesVisible(true);
        //monitorPlot.setRangeGridlinesVisible(true);
        // Instead, this is needed to show the grid.
        XYLineAndShapeRenderer renderer0 =
            new XYLineAndShapeRenderer(false, true);
        monitorPlot.setDataset(0, new DefaultXYDataset());
        monitorPlot.setRenderer(0, renderer0);
        
        int iColor = 0;
        int iStroke = 0;
        for (int iSeries=0; iSeries<headers.length; iSeries++)
        {
            DefaultXYDataset dataset = new DefaultXYDataset();
            dataset.addSeries(headers[iSeries], xyData.get(iSeries));
            monitorDatasets.put(CounterID.valueOf(headers[iSeries]), dataset);
            
            XYLineAndShapeRenderer serierRenderer =
                new XYLineAndShapeRenderer(true, false);
            monitorPlot.setDataset(iSeries, dataset);
            monitorPlot.setRenderer(iSeries, serierRenderer);
            
            serierRenderer.setSeriesPaint(0, colors[iColor]);
            serierRenderer.setSeriesStroke(0, strokes[iStroke]);
            
            //Form here New with JCheckBox
            JCheckBox cbi = new JCheckBox(
                    CounterID.valueOf(headers[iSeries]).getPrettyName());
            cbi.setForeground(colors[iColor]);
            cbi.setToolTipText(String.format("<html><body width='%1s'>"
                    + CounterID.valueOf(headers[iSeries]).getDescription()
                    + ".</html>", 300));

            // We do this before setting the listener to avoid having to bypass
            if (iSeries!=0)
            {
                serierRenderer.setSeriesVisible(0, false);
            } else {
                nameFirstMonitorSeries = cbi.getText();
                cbi.setSelected(true);
            }
            
            cbi.addItemListener(new ItemListener(){
                @Override
                public void itemStateChanged(ItemEvent e)
                {
                    if (cbi.isSelected())
                    {
                        serierRenderer.setSeriesVisible(0, true);
                    } else {
                        serierRenderer.setSeriesVisible(0, false);
                    }                    
                }
            });
            monitorSeriesCheckList.addCheckBox(cbi);
            
            // Restart color sequence from beginning
            iColor++;
            if (iColor >= colors.length)
            {
                iColor = 0;
                if (iStroke < strokes.length-1)
                {
                    iStroke++;
                } else {
                    iStroke = 0;
                }
            }
        }

        // Create the actual panel that contains the chart
        monitorChartPanel = new ChartPanel(monitorChart);

        // Adapt chart size to the size of the panel
        rightDownPanel.addComponentListener(new ComponentAdapter() {
	        @Override
	        public void componentResized(ComponentEvent e) {
	            monitorChartPanel.setMaximumDrawHeight(e.getComponent().getHeight());
	            monitorChartPanel.setMaximumDrawWidth(e.getComponent().getWidth());
	            monitorChartPanel.setMinimumDrawWidth(e.getComponent().getWidth());
	            monitorChartPanel.setMinimumDrawHeight(e.getComponent().getHeight());
	            // WARNING: this update is needed to make the new size affective
	            // also after movement of the JSplitPane divider, which is
	            // otherwise felt by this listener but the new sizes do not
	            // take effect.
	            ChartEditor ce = ChartEditorManager.getChartEditor(monitorChart);
	            ce.updateChart(monitorChart);
	        }
        });

        rightDownPanel.add(monitorChartPanel,BorderLayout.CENTER);
	}

//------------------------------------------------------------------------------
	
	private List<CandidateLW> getOverlappingItems(CandidateLW item, 
	        int maxNeighbours)
	{
        int initPos = allIndividuals.indexOf(item);
        double toleranceY = Math.abs(evoPlot.getRangeAxis()
                .getRange().getLength() * 0.02);

        double toleranceX = Math.abs(evoPlot.getDomainAxis()
                .getRange().getLength() * 0.01);
        int nItems = 0;
        List<CandidateLW> overlappingItems = 
                new ArrayList<CandidateLW>();
        while (nItems<maxNeighbours && 
                (initPos+nItems)<allIndividuals.size())
        {
            CandidateLW c = allIndividuals.get(initPos + nItems);
            if (!c.hasFitness())
            {
                nItems++;
                continue;
            }
            double deltaY = Math.abs(item.getFitness() 
                    - c.getFitness());
            if (deltaY > toleranceY)
                break;
            double deltaX = Math.abs(item.getGeneration()
                    - c.getGeneration());
            if (deltaX > toleranceX)
                break;
            overlappingItems.add(c);
            nItems++;
        }
        nItems = 1;
        while (nItems<maxNeighbours && (initPos-nItems)>-1)
        {
            CandidateLW c = allIndividuals.get(initPos - nItems);
            if (!c.hasFitness())
            {
                nItems++;
                continue;
            }
            double deltaY = Math.abs(item.getFitness() 
                    - c.getFitness());
            if (deltaY > toleranceY)
                break;
            double deltaX = Math.abs(item.getGeneration()
                    - c.getGeneration());
            if (deltaX > toleranceX)
                break;
            overlappingItems.add(0,c);
            nItems++;
        }
        return overlappingItems;
	}

//------------------------------------------------------------------------------
	
	private CandidateLW choseAmongPossiblyOverlapping(JComponent parent,
	        List<CandidateLW> overlappingItems)
    {
	    switch (overlappingItems.size())
	    {
	        case 0:
	            return null;
	        case 1:
	            return overlappingItems.get(0);
	    }
	    
        DefaultListModel<String> listModel = new DefaultListModel<String>();
        JList<String> optionsList = new JList<String>(listModel);
        overlappingItems.stream().forEach(c -> listModel.addElement(c.getName()));
        optionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JPanel chooseItem = new JPanel();
        JLabel header = new JLabel("Select item to visualize:");
        JScrollPane scrollPane = new JScrollPane(optionsList);
        chooseItem.add(header);
        chooseItem.add(scrollPane);
      
        int res = JOptionPane.showConfirmDialog(parent,
              chooseItem, 
              "Choose Among Overlapping Items", 
              JOptionPane.OK_CANCEL_OPTION,
              JOptionPane.PLAIN_MESSAGE, 
              null);
        if (res != JOptionPane.OK_OPTION)
        {
          return  null;
        }
        return overlappingItems.get(optionsList.getSelectedIndex());
    }
	
//------------------------------------------------------------------------------
	
	private class PlottedCandidatesComparator implements Comparator<CandidateLW>
	{
        @Override
        public int compare(CandidateLW c1, CandidateLW c2)
        {
            int byGen = Integer.compare(c1.getGeneration(), c2.getGeneration());
            if (byGen!=0)
                return byGen;
            if (c1.hasFitness() && c2.hasFitness())
                return Double.compare(c1.getFitness(), c2.getFitness());
            else if (c1.hasFitness())
                return 1;
            else if (c2.hasFitness())
                return -1;
            return 0;
        }
	}
	
//-----------------------------------------------------------------------------
	
	private void renderViewWithSelectedItem(CandidateLW item)
	{
        mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		// Update series of selected (chart is updated automatically)
        double[][] selectedCandsData = new double[2][1]; //NB: for now allow only one
        int j = -1;
        for (int i=0; i<1; i++) //NB: for now allow only one selected item
        {	
        	j++;
        	selectedCandsData[0][j] = item.getGeneration();
        	selectedCandsData[1][j] = item.getFitness();
        }
        datasetSelected.removeSeries("Selected_candidates");
        datasetSelected.addSeries("Selected_candidates", selectedCandsData);
        // NB the '0' is determined by initialization at HERE@HERE
		evoPlot.setDataset(0, datasetSelected);
		
		// Update the molecular viewer
		molViewer.loadChemicalStructureFromFile(item.getPathToFile());
		pathToSelectedItem = item.getPathToFile();
        openSingleGraph.setEnabled(true);

        mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
	
//-----------------------------------------------------------------------------
	
	private void renderViewWithoutSelectedItems()
	{
		datasetSelected.removeSeries("Selected_candidates");
        //TODO: we could avoid 'zap-ping' jmol by covering it with an opaque card
        molViewer.clearAll(false);
		openSingleGraph.setEnabled(false);
	}
	
//-----------------------------------------------------------------------------
  	
}
