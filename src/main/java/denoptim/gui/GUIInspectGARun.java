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
import java.awt.Insets;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import javax.swing.AbstractButton;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.editor.ChartEditor;
import org.jfree.chart.editor.ChartEditorManager;
import org.jfree.chart.entity.PlotEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.files.FileUtils;
import denoptim.graph.CandidateLW;
import denoptim.graph.Provenance;
import denoptim.io.DenoptimIO;
import denoptim.logging.CounterID;
import denoptim.utils.GeneralUtils;


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
	private MoleculeViewerPanel molViewer;
	
	private File srcFolder;
	
	private List<CandidateLW> allIndividuals;
	
	/**
	 * Candidates that can be named by a {@link Provenance.ParentLink}:
	 * generation, then candidate name.
	 */
	private Map<Integer,Map<String,CandidateLW>> candidatesByGenerationAndName;
	
	private int molsWithFitness = 0;
	private boolean warnedAboutMissingCandFiles = false;
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
	
	/**
	 * One series per parent-child segment of the selected candidate's lineage.
	 */
	private DefaultXYDataset datasetLineage = new DefaultXYDataset();
	private final XYLineAndShapeRenderer lineageRenderer =
	        new XYLineAndShapeRenderer(true, false);
	
	/**
	 * Direct offspring of the selected candidate. Mutations and crossovers are
	 * separate datasets so each can keep its own color.
	 */
	private DefaultXYDataset datasetPosterityMutation = new DefaultXYDataset();
	private final XYLineAndShapeRenderer posterityMutationRenderer =
	        new XYLineAndShapeRenderer(true, false);
	private DefaultXYDataset datasetPosterityCrossover = new DefaultXYDataset();
	private final XYLineAndShapeRenderer posterityCrossoverRenderer =
	        new XYLineAndShapeRenderer(true, false);
	
	/**
	 * Dataset and renderer slot of each evolution-plot series, in draw order.
	 * With {@link DatasetRenderingOrder#REVERSE}, smaller numbers are painted
	 * in front. A series that occupies a dataset by itself is series
	 * {@link #SOLE_SERIES} inside that slot.
	 */
	private final Map<String,Integer> evoSeriesNumber =
	        new LinkedHashMap<String,Integer>();
	
	private static final String SERIES_SELECTED = "Selected_candidates";
	private static final String SERIES_ALL_FIT = "Candidates_with_fitness";
	private static final String SERIES_LINEAGE = "Lineage";
	private static final String SERIES_POSTERITY_MUTATION = "Posterity by Mutation";
	private static final String SERIES_POSTERITY_CROSSOVER =
	        "Posterity by Crossover";
	private static final String SERIES_POP_MIN = "Minimum Fitness in Population";
	private static final String SERIES_POP_MAX = "Maximum Fitness in Population";
	private static final String SERIES_POP_MEAN = "Mean of Fitness in Population";
	private static final String SERIES_POP_MEDIAN =
	        "Median of Fitness in Population";
	
	private static final Color LINEAGE_COLOR = Color.decode("#F07518");
	private static final Color POSTERITY_MUTATION_COLOR = Color.decode("#9D52E3");
	private static final Color POSTERITY_CROSSOVER_COLOR = Color.decode("#399936");
	private static final Color POP_MIN_COLOR = Color.blue;
	private static final Color POP_MAX_COLOR = Color.blue;
	private static final Color POP_MEAN_COLOR = Color.red;
	private static final Color POP_MEDIAN_COLOR = Color.decode("#22BB22");
	
	/**
	 * Series index inside a dataset that holds only one series.
	 */
	private static final int SOLE_SERIES = 0;
	
	private XYPlot evoPlot;
	private JFreeChart evoChart;
	private ChartPanel evoChartPanel;
	
    private JPopupMenu evoSeriesCheckList;
    private JButton evoSeriesBtn;
    private JPopupMenu familyTreeCheckList;
    private JButton familyTreeBtn;
	
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
     * Button offering the possibility to load the graphs of the final 
     * population.
     */
    private JButton openFinalPopGraphs;
    
    /**
     * Exports the candidates joined by the lineage lines of the selection.
     * Enabled only while those lines are displayed.
     */
    private JButton exportLineage;
    
    /**
     * Exports the selected candidate and the offspring joined by the posterity
     * lines that are currently visible.
     */
    private JButton exportPosterity;
    
    /**
     * Candidate currently drawn in the evolution plot, or {@code null}.
     */
    private CandidateLW selectedCandidate;
    
    /**
     * Candidates that are endpoints of the displayed lineage lines.
     */
    private final List<CandidateLW> displayedLineage =
            new ArrayList<CandidateLW>();
    
    /**
     * Direct offspring drawn by the mutation and crossover posterity lines.
     */
    private final List<CandidateLW> displayedMutationPosterity =
            new ArrayList<CandidateLW>();
    private final List<CandidateLW> displayedCrossoverPosterity =
            new ArrayList<CandidateLW>();
    
    /**
     * Pathways of population members collected by generation id. This info
     * is taken from the generation summary files, so in absence of such file 
     * for a generation, that generation will not be present here.
     */
    private Map<Integer,List<String>> candsPerGeneration;
    
    /**
     * Predefined list of data series colors.
     */
    private final Color[] colors = new Color[] {Color.black, 
            Color.decode("#354ccd"), //blue-ish
            Color.decode("#1e9222"), //dark green
            Color.decode("#9eca2f"), //light green
            Color.decode("#28cbad"), //cyan-ish
            Color.decode("#dea0c8"), //pink-ish
            Color.decode("#dd6835"), //oragne
            Color.decode("#b70505"), //red-ish
            Color.decode("#a42ac4")}; //violet
    
    /**
     * Predefined line strokes 
     */
    private final BasicStroke[] strokes = {
            new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                  0.0f),
            new BasicStroke(
                    1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                    1.0f, new float[] {2f, 2f}, 0.0f),
            new BasicStroke(
                    1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                    1.0f, new float[] {5f, 2f}, 0.0f),
            new BasicStroke(
                    1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                    1.0f, new float[] {2f, 2f, 5f, 2f}, 0.0f),
            new BasicStroke(
                    1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                    1.0f, new float[] {2f, 2f, 2f, 2f, 5f, 2f}, 0.0f),
            new BasicStroke(
                    1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                    1.0f, new float[] {2f, 2f, 2f, 2f, 2f, 2f, 5f, 2f}, 0.0f),
            new BasicStroke(
                    1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                    1.0f, new float[] {2f, 2f, 5f, 2f, 5f, 2f}, 0.0f),
            new BasicStroke(
                    1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                    1.0f, new float[] {2f, 2f, 2f, 2f, 5f, 2f, 5f, 2f}, 0.0f),
            new BasicStroke(
                    1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                    1.0f, new float[] {2f, 2f, 2f, 2f, 2f, 2f, 
                            5f, 2f, 5f, 2f, 5f, 2f}, 0.0f),
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
	 * Text baseline of a toolbar row. Buttons define the line when the row
	 * mixes buttons and labels, so a label in the neighbouring panel can
	 * share it.
	 */
	private static int toolbarBaseline(JPanel panel)
	{
	    Component anchor = null;
	    int rowHeight = 0;
	    for (Component c : panel.getComponents())
	    {
	        if (!c.isVisible())
	            continue;
	        rowHeight = Math.max(rowHeight, c.getPreferredSize().height);
	        if (anchor == null || (c instanceof AbstractButton
	                && !(anchor instanceof AbstractButton)))
	            anchor = c;
	    }
	    if (anchor == null || rowHeight <= 0)
	        return -1;
	    Dimension pref = anchor.getPreferredSize();
	    int base = anchor.getBaseline(Math.max(pref.width, 1),
	            Math.max(pref.height, 1));
	    if (base < 0)
	        return -1;
	    Insets insets = panel.getInsets();
	    int vgap = 0;
	    if (panel.getLayout() instanceof FlowLayout)
	        vgap = ((FlowLayout) panel.getLayout()).getVgap();
	    int yInRow = Math.max(0, (rowHeight - pref.height) / 2);
	    return insets.top + vgap + yInRow + base;
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
		ctrlPanelRow1Left = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0)) {
		    @Override
		    public int getBaseline(int width, int height)
		    {
		        return toolbarBaseline(this);
		    }
		};

        ctrlPanelRow1Left.add(new JLabel("Show/Hide Series:"));

	    evoSeriesCheckList = new JPopupMenu();
        evoSeriesBtn = new JButton("Population Stats");
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
		
	    familyTreeCheckList = new JPopupMenu();
        familyTreeBtn = new JButton("Family Tree");
        familyTreeBtn.setComponentPopupMenu(familyTreeCheckList);
        familyTreeBtn.setToolTipText(String.format(
                "<html><body width='%1s'>Click to select which lineage and "
                + "posterity lines to plot in the top plot.",300));
        familyTreeBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                familyTreeCheckList.show(familyTreeBtn, 0, 0);
            }
        });
        ctrlPanelRow1Left.add(familyTreeBtn);
		
		monitorSeriesCheckList = new ScrollableJPupupMenu();
        monitorSeriesBtn = new JButton("Monitor");
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
		
        ctrlPanelRow1Left.add(new JLabel("Charts:"));

	    JButton rstView = new JButton("Reset Axis");
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
                        String text = ((JCheckBoxMenuItem) c).getText();
                        ((JCheckBoxMenuItem) c).setSelected(
                                SERIES_POP_MIN.equals(text)
                                || SERIES_POP_MAX.equals(text));
                    }
                }
                for (int iItem=0; iItem<familyTreeCheckList.getComponentCount();
                        iItem++)
                {
                    Component c = familyTreeCheckList.getComponent(iItem);
                    if (c instanceof JCheckBoxMenuItem)
                        ((JCheckBoxMenuItem) c).setSelected(true);
                }
                
                monitorPlot.getDomainAxis().setAutoRange(true);
                monitorPlot.getRangeAxis().setAutoRange(true);
                monitorPlot.getDomainAxis().setLowerBound(-0.5);
                for (JCheckBoxMenuItem cb : monitorSeriesCheckList.getAllBChekBoxes())
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
	
		ctrlPanelRow1Right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0)) {
		    @Override
		    public int getBaseline(int width, int height)
		    {
		        return toolbarBaseline(this);
		    }

		    @Override
		    public Dimension getMaximumSize()
		    {
		        Dimension max = getPreferredSize();
		        max.width = Integer.MAX_VALUE;
		        return max;
		    }
		};
		lblTotItems = new JLabel("No item loaded");
		lblTotItems.setHorizontalAlignment(SwingConstants.RIGHT);
		lblTotItems.setPreferredSize(new Dimension(300,
		        lblTotItems.getPreferredSize().height));
		ctrlPanelRow1Right.add(lblTotItems);
		
        ctrlPanelRow2Left = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        
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
                GenerationChoiceDialog dialog = new GenerationChoiceDialog(
                        openGeneratinGraphs);
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
                String pathtoTmpFile = Utils.getTempFile("population_at_gen" 
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
        
        
        openFinalPopGraphs = new JButton("Open Final Pop Graphs");
        openFinalPopGraphs.setToolTipText(String.format(
                "<html><body width='%1s'>Open a "
                + "new tab for inspecting the graph representation of all "
                + "members of the final population. ",300));
        openFinalPopGraphs.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Collect population
                String pathtoTmpFile = Utils.getTempFile("final_population.sdf");
                File finalPopFolder = new File(srcFolder,"Final");
                if (!finalPopFolder.exists())
                {
                    JOptionPane.showMessageDialog(openGeneratinGraphs,
                            String.format("<html><body width='%1s'>" 
                                    + "Cannot find folder '" + finalPopFolder 
                                    + "'. Final population not found.", 300),
                                    "Error",
                            JOptionPane.ERROR_MESSAGE,
                            UIManager.getIcon("OptionPane.errorIcon"));
                    mainPanel.setCursor(Cursor.getPredefinedCursor(
                            Cursor.DEFAULT_CURSOR));
                    return;
                }
                mainPanel.setCursor(Cursor.getPredefinedCursor(
                        Cursor.WAIT_CURSOR));
                List<String> filesToMerge = new ArrayList<String>();
                for (File fitFile : finalPopFolder.listFiles(new FileFilter() {
                    
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
                    filesToMerge.add(fitFile.getAbsolutePath());
                }
       
                try
                {
                    FileUtils.mergeIntoOneFile(pathtoTmpFile, filesToMerge);
                    mainPanel.setCursor(Cursor.getPredefinedCursor(
                            Cursor.DEFAULT_CURSOR));
                } catch (IOException e1)
                {
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(openGeneratinGraphs,
                            String.format("<html><body width='%1s'>" 
                                    + "List of members of final population "
                                    + " could not be collected. Hint: "
                                    + e1.getMessage(), 400),
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
                System.out.println("File collecting members of the final "
                        + "population: "+ pathtoTmpFile);
            }
        });
        ctrlPanelRow2Left.add(openFinalPopGraphs);
        
        exportLineage = new JButton("Export Lineage");
        exportLineage.setEnabled(false);
        exportLineage.setToolTipText(String.format(
                "<html><body width='%1s'>Write the selected candidate and its "
                + "displayed ancestors into one SDF file.", 300));
        exportLineage.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                exportCandidates(new ArrayList<CandidateLW>(displayedLineage));
            }
        });
        ctrlPanelRow2Left.add(exportLineage);
        
        exportPosterity = new JButton("Export Posterity");
        exportPosterity.setEnabled(false);
        exportPosterity.setToolTipText(String.format(
                "<html><body width='%1s'>Write the selected candidate and the "
                + "offspring of the visible posterity lines into one SDF "
                + "file.", 300));
        exportPosterity.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                List<CandidateLW> toExport = new ArrayList<CandidateLW>();
                if (selectedCandidate != null)
                    toExport.add(selectedCandidate);
                if (isFamilySeriesShown(SERIES_POSTERITY_MUTATION))
                    toExport.addAll(displayedMutationPosterity);
                if (isFamilySeriesShown(SERIES_POSTERITY_CROSSOVER))
                    toExport.addAll(displayedCrossoverPosterity);
                exportCandidates(toExport);
            }
        });
        ctrlPanelRow2Left.add(exportPosterity);
        

        ctrlPanelRow1 = new JPanel();
        GroupLayout lyoCtrlPanelRow1 = new GroupLayout(ctrlPanelRow1);
        ctrlPanelRow1.setLayout(lyoCtrlPanelRow1);
        lyoCtrlPanelRow1.setAutoCreateGaps(false);
        lyoCtrlPanelRow1.setAutoCreateContainerGaps(false);
        lyoCtrlPanelRow1.setHorizontalGroup(
                lyoCtrlPanelRow1.createSequentialGroup()
                    .addComponent(ctrlPanelRow1Left)
                    .addComponent(ctrlPanelRow1Right));
        lyoCtrlPanelRow1.setVerticalGroup(lyoCtrlPanelRow1.createParallelGroup(
                GroupLayout.Alignment.BASELINE, false)
                    .addComponent(ctrlPanelRow1Left)
                    .addComponent(ctrlPanelRow1Right));
        
        ctrlPanel =  new JPanel();
        GroupLayout lyoCtrlPanel = new GroupLayout(ctrlPanel);
        ctrlPanel.setLayout(lyoCtrlPanel);
        lyoCtrlPanel.setAutoCreateGaps(false);
        lyoCtrlPanel.setAutoCreateContainerGaps(false);
        lyoCtrlPanel.setHorizontalGroup(lyoCtrlPanel.createParallelGroup()
                    .addComponent(ctrlPanelRow1)
                    .addComponent(ctrlPanelRow2Left));
        lyoCtrlPanel.setVerticalGroup(lyoCtrlPanel.createSequentialGroup()
                    .addComponent(ctrlPanelRow1, GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addGap(2)
                    .addComponent(ctrlPanelRow2Left, GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE));
        
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
		molViewer = new MoleculeViewerPanel();
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
	                    + "options, inclusing exporting the charts as PNG or "
	                    + "SVG.</li>"
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
    @SuppressWarnings("serial")
    private class GenerationChoiceDialog extends GUIModalDialog
    {
        public GenerationChoiceDialog(Component refForPlacement)
        {
            super(refForPlacement,false);
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
		int largestGenId = -1;
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
	        String zeroedGenId = GeneralUtils.getPaddedString(padSize, genId);
		    readOneGeneration(genFolder, "Gen" + zeroedGenId + ".txt", 
		            genId, parent, popProperties);
		    if (genId>largestGenId)
		        largestGenId = genId;
		}
		
		indexCandidatesByGenerationAndName();
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
		datasetAllFit.addSeries(SERIES_ALL_FIT, candsWithFitnessData);
		
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
        xAxis.setRange(-0.5, Math.max(1, numGen)-0.5); //"-" because numGen includes 0
        xAxis.setAutoRangeIncludesZero(false);
        NumberAxis yAxis = new NumberAxis("Fitness");
        yAxis.setAutoRangeIncludesZero(false);
		evoPlot = new XYPlot(null,xAxis,yAxis,null);
		
		evoChart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT,evoPlot,
		        GUIPreferences.showLegenInEvolutionPlot);
		if (evoChart.getLegend()!=null)
		    evoChart.getLegend().setPosition(RectangleEdge.BOTTOM);
		
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
		evoSeriesNumber.clear();
		
		// dataset of selected items
        Shape shape0 = new Ellipse2D.Double(
                 -GUIPreferences.chartPointSize*1.1/2.0,
                 -GUIPreferences.chartPointSize*1.1/2.0,
                 GUIPreferences.chartPointSize*1.1, 
                 GUIPreferences.chartPointSize*1.1);
        XYLineAndShapeRenderer renderer0 = 
                new XYLineAndShapeRenderer(false, true);
        // The selected series keeps the front-most slot. Search for
        // 'HERE@HERE' where that slot is reused.
        bindEvoSeries(SERIES_SELECTED, datasetSelected, renderer0);
        renderer0.setSeriesShape(SOLE_SERIES, shape0);
        renderer0.setSeriesPaint(SOLE_SERIES, Color.red);
        renderer0.setSeriesFillPaint(SOLE_SERIES, Color.red);
        renderer0.setSeriesOutlinePaint(SOLE_SERIES, Color.BLACK);
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
        bindEvoSeries(SERIES_ALL_FIT, datasetAllFit, renderer1);
        renderer1.setSeriesShape(SOLE_SERIES, shape1);
        renderer1.setSeriesPaint(SOLE_SERIES, new Color(192, 192, 192, 60));
        renderer1.setSeriesFillPaint(SOLE_SERIES, new Color(192, 192, 192, 60));
        renderer1.setSeriesOutlinePaint(SOLE_SERIES, Color.GRAY);
        renderer1.setUseOutlinePaint(true);
        renderer1.setUseFillPaint(true);
        
        // Parent-child lines for the selected candidate. Bound after the
        // candidate points, so those points stay in front and a click still
        // hits a candidate, and before the population statistics, so the
        // lines paint above those curves.
        lineageRenderer.setAutoPopulateSeriesPaint(false);
        lineageRenderer.setDefaultPaint(LINEAGE_COLOR);
        lineageRenderer.setAutoPopulateSeriesStroke(false);
        lineageRenderer.setDefaultStroke(new BasicStroke(2.0f,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        lineageRenderer.setDefaultSeriesVisibleInLegend(false);
        bindEvoSeries(SERIES_LINEAGE, datasetLineage, lineageRenderer);
        
        // Direct offspring of the selected candidate, in later generations.
        // Same layering as the lineage: under the points, above the statistics.
        posterityMutationRenderer.setAutoPopulateSeriesPaint(false);
        posterityMutationRenderer.setDefaultPaint(POSTERITY_MUTATION_COLOR);
        posterityMutationRenderer.setAutoPopulateSeriesStroke(false);
        posterityMutationRenderer.setDefaultStroke(new BasicStroke(2.0f,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        posterityMutationRenderer.setDefaultSeriesVisibleInLegend(false);
        bindEvoSeries(SERIES_POSTERITY_MUTATION, datasetPosterityMutation,
                posterityMutationRenderer);
        posterityCrossoverRenderer.setAutoPopulateSeriesPaint(false);
        posterityCrossoverRenderer.setDefaultPaint(POSTERITY_CROSSOVER_COLOR);
        posterityCrossoverRenderer.setAutoPopulateSeriesStroke(false);
        posterityCrossoverRenderer.setDefaultStroke(new BasicStroke(2.0f,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        posterityCrossoverRenderer.setDefaultSeriesVisibleInLegend(false);
        bindEvoSeries(SERIES_POSTERITY_CROSSOVER, datasetPosterityCrossover,
                posterityCrossoverRenderer);

        familyTreeCheckList = new JPopupMenu();
        addEvoSeriesCheckBox(familyTreeCheckList, SERIES_LINEAGE,
                LINEAGE_COLOR, true);
        addEvoSeriesCheckBox(familyTreeCheckList, SERIES_POSTERITY_MUTATION,
                POSTERITY_MUTATION_COLOR, true);
        addEvoSeriesCheckBox(familyTreeCheckList, SERIES_POSTERITY_CROSSOVER,
                POSTERITY_CROSSOVER_COLOR, true);
        
        // Collect the optional series for an easy selection in the menu list
        evoSeriesCheckList = new JPopupMenu();
        
        // min fitness in the population
        XYLineAndShapeRenderer renderer2 = 
        		new XYLineAndShapeRenderer(true, false);
        bindEvoSeries(SERIES_POP_MIN, datasetPopMin, renderer2);
        renderer2.setSeriesPaint(SOLE_SERIES, POP_MIN_COLOR);
        addEvoSeriesCheckBox(evoSeriesCheckList, SERIES_POP_MIN,
                POP_MIN_COLOR, true);
        
        // max fitness in the population
        XYLineAndShapeRenderer renderer3 = 
        		new XYLineAndShapeRenderer(true, false);
        bindEvoSeries(SERIES_POP_MAX, datasetPopMax, renderer3);
        renderer3.setSeriesPaint(SOLE_SERIES, POP_MAX_COLOR);
        addEvoSeriesCheckBox(evoSeriesCheckList, SERIES_POP_MAX,
                POP_MAX_COLOR, true);
        
        // mean fitness in the population
        XYLineAndShapeRenderer renderer4 = 
        		new XYLineAndShapeRenderer(true, false);
        bindEvoSeries(SERIES_POP_MEAN, datasetPopMean, renderer4);
        renderer4.setSeriesPaint(SOLE_SERIES, POP_MEAN_COLOR);
        renderer4.setSeriesStroke(SOLE_SERIES, new BasicStroke(
                2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                1.0f, new float[] {10.0f, 6.0f}, 0.0f));
        setEvoSeriesVisible(SERIES_POP_MEAN, false);
        addEvoSeriesCheckBox(evoSeriesCheckList, SERIES_POP_MEAN,
                POP_MEAN_COLOR, false);
        
        // median fitness in the population
        XYLineAndShapeRenderer renderer5 = 
        		new XYLineAndShapeRenderer(true, false);
        bindEvoSeries(SERIES_POP_MEDIAN, datasetPopMedian, renderer5);
        renderer5.setSeriesPaint(SOLE_SERIES, POP_MEDIAN_COLOR);
        renderer5.setSeriesStroke(SOLE_SERIES, new BasicStroke(
                    2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                    1.0f, new float[] {3.0f, 6.0f}, 0.0f));
        setEvoSeriesVisible(SERIES_POP_MEDIAN, false);
        addEvoSeriesCheckBox(evoSeriesCheckList, SERIES_POP_MEDIAN,
                POP_MEDIAN_COLOR, false);
        
        
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
					XYDataset ds = ((XYItemEntity)e.getEntity()).getDataset();
					if (!ds.equals(datasetAllFit))
					{
						return;
					}
					
					int serId = ((XYItemEntity)e.getEntity()).getSeriesIndex();
					if (serId == SOLE_SERIES)
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

	private void readOneGeneration(File genFolder, String summaryName,
	        int genId,
	        Component parent, 
	        Map<Integer,double[]> popProperties)
    {
        // Read Generation summary file
        File genSummary = new File(genFolder, summaryName);
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
        
        // Read candidates from file (if present)
        boolean foundCandidateFilesInThisGen = false;
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
            foundCandidateFilesInThisGen = true;
        }
        
        // Read traces of candidates if candidate's files are not present
        if (!foundCandidateFilesInThisGen)
        {
            if (!warnedAboutMissingCandFiles)
            {
                JOptionPane.showMessageDialog(parent,
                        String.format("<html><body width='%1s'>"
                                + "No Candidate files found in " + genFolder 
                                + ". This may be because the experiment "
                                + "has still not finished running, or"
                                + "it has been run with the "
                                + "'FP-DontWriteCandidatesOnDisk' option. "
                                + "We can only show the candidates' traces "
                                + "in the evolution. You may try "
                                + "to inspect the final generation, "
                                + "if present, by clicking on '" 
                                + openFinalPopGraphs.getText() + "'"
                                + "</html>", 400),
                        "Warning",
                        JOptionPane.WARNING_MESSAGE,
                        UIManager.getIcon("OptionPane.errorIcon"));
                warnedAboutMissingCandFiles = true;
            }
            readPopMembers = false;
            try
            {
                for (CandidateLW cand : 
                    DenoptimIO.readPopulationMembersTraces(genSummary))
                {
                    // We keep only the oldest version of a candidate.
                    // To this end, note that the equality of CandidateLW
                    // considers only name and UID.
                    if (allIndividuals.contains(cand))
                    {
                        CandidateLW otherTracePfCand = allIndividuals.get(
                                allIndividuals.indexOf(cand));
                        if (otherTracePfCand.getGeneration() > genId)
                        {
                            allIndividuals.remove(otherTracePfCand);
                            molsWithFitness--;
                        } else {
                            continue;
                        }
                    }
                    cand.setGeneration(genId);
                    molsWithFitness++;
                    allIndividuals.add(cand);
                }
            } catch (DENOPTIMException e1)
            {
                // Should never happen unless the file becomes unreadable
                // between the previous read and this one.
                e1.printStackTrace();
            }
        }
        
        //Read population members from summary
        if (readPopMembers)
        {
            try
            {
                List<String> membersPathnames = new ArrayList<String>(
                        DenoptimIO.readPopulationMemberPathnames(genSummary));
                // Adjust pathnames to the current folder: the summary stores
                // absolute paths from the original run, so replace the prefix
                // before the Gen* folder. The Gen* name stays as stored.
                for (int i=0; i<membersPathnames.size(); i++)
                {
                    membersPathnames.set(i, relocateToGenFolder(
                            membersPathnames.get(i), genFolder));
                }
                    
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

//------------------------------------------------------------------------------

    /**
     * Replaces the part of an absolute pathname before the generation folder
     * ({@code Gen*}) with the parent of {@code genFolder}. The original
     * {@code Gen*} folder name and the remainder of the pathname are kept.
     * The original pathname is returned when it contains no generation folder.
     */
    private static String relocateToGenFolder(String pathname, File genFolder)
    {
        File current = new File(pathname);
        List<String> belowGenFolder = new ArrayList<String>();
        while (current != null && !current.getName().startsWith(
                DENOPTIMConstants.GAGENDIRNAMEROOT))
        {
            belowGenFolder.add(0, current.getName());
            current = current.getParentFile();
        }
        if (current == null)
        {
            return pathname;
        }
        File relocated = genFolder.getParentFile() == null
                ? genFolder
                : new File(genFolder.getParentFile(), current.getName());
        for (String part : belowGenFolder)
        {
            relocated = new File(relocated, part);
        }
        return relocated.getAbsolutePath();
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
	    List<String> lines = null;
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
	    xAxis.setRange(-0.5, Math.max(Double.valueOf(iRecordsKept),2.0)-0.5);
	    xAxis.setAutoRangeIncludesZero(false);
	    // By default we show the first series (should be the number of attempts
	    // to create candidates)
	    NumberAxis yAxis = new NumberAxis("#");
	    yAxis.setAutoRangeIncludesZero(false);
	    
        monitorPlot = new XYPlot(null,xAxis,yAxis,null);

        monitorChart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, 
                monitorPlot, GUIPreferences.showLegenInMonitorPlot);
        if (monitorChart.getLegend()!=null)
            monitorChart.getLegend().setPosition(RectangleEdge.BOTTOM);

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
            CounterID counter = CounterID.valueOf(headers[iSeries]);
            DefaultXYDataset dataset = new DefaultXYDataset();
            dataset.addSeries(counter.getPrettyName(), xyData.get(iSeries));
            monitorDatasets.put(counter, dataset);
            
            boolean renderPoints = false;
            if (iRecordsKept<2)
                renderPoints = true;
            XYLineAndShapeRenderer serierRenderer =
                new XYLineAndShapeRenderer(true, renderPoints);
            monitorPlot.setDataset(iSeries, dataset);
            monitorPlot.setRenderer(iSeries, serierRenderer);
            
            serierRenderer.setSeriesPaint(0, colors[iColor]);
            serierRenderer.setSeriesStroke(0, strokes[iStroke]);
            
            JCheckBoxMenuItem cbi = new ScrollableJPupupMenu.StayOpenCheckBoxMenuItem(
                    counter.getPrettyName());
            cbi.setForeground(colors[iColor]);
            cbi.setToolTipText(String.format("<html><body width='%1s'>"
                    + counter.getDescription()
                    + ".</html>", 300));

            // We do this before setting the listener to avoid having to bypass
            boolean showSeries = iSeries==0;
            serierRenderer.setSeriesVisible(0, showSeries);
            serierRenderer.setSeriesVisibleInLegend(0, showSeries);
            if (showSeries)
            {
                nameFirstMonitorSeries = cbi.getText();
                cbi.setSelected(true);
            }
            
            cbi.addItemListener(new ItemListener(){
                @Override
                public void itemStateChanged(ItemEvent e)
                {
                    boolean selected = cbi.isSelected();
                    serierRenderer.setSeriesVisible(0, selected);
                    serierRenderer.setSeriesVisibleInLegend(0, selected);
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
        datasetSelected.removeSeries(SERIES_SELECTED);
        datasetSelected.addSeries(SERIES_SELECTED, selectedCandsData);
        // NB the slot is the one recorded for SERIES_SELECTED (HERE@HERE)
		evoPlot.setDataset(evoSeriesNumber.get(SERIES_SELECTED),
		        datasetSelected);
		selectedCandidate = item;
		showLineage(item);
		showPosterity(item);
        
		// Update the molecular viewer
        IAtomContainer mol;
        try {
            mol = DenoptimIO.readAllAtomContainers(new File(
                item.getPathToFile())).get(0);
        } catch (Exception e) {
            System.out.println("Could not read molecular structure from " 
                + item.getPathToFile() + ": " + e.getLocalizedMessage());
            e.printStackTrace();
            mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            return;
        }

		molViewer.loadContent(mol);
		pathToSelectedItem = item.getPathToFile();
        openSingleGraph.setEnabled(true);

        mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
	
//-----------------------------------------------------------------------------
	
	private void renderViewWithoutSelectedItems()
	{
		datasetSelected.removeSeries(SERIES_SELECTED);
		selectedCandidate = null;
		clearLineage();
		clearPosterity();
        molViewer.clear();
		openSingleGraph.setEnabled(false);
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Indexes loaded candidates by the generation and name stored in a
	 * {@link Provenance.ParentLink}.
	 */
	private void indexCandidatesByGenerationAndName()
	{
	    candidatesByGenerationAndName =
	            new HashMap<Integer,Map<String,CandidateLW>>();
	    for (CandidateLW candidate : allIndividuals)
	    {
	        if (candidate.getName() == null)
	            continue;
	        Map<String,CandidateLW> inGeneration =
	                candidatesByGenerationAndName.computeIfAbsent(
	                        candidate.getGeneration(),
	                        key -> new HashMap<String,CandidateLW>());
	        CandidateLW existing = inGeneration.get(candidate.getName());
	        if (existing == null
	                || (!existing.hasFitness() && candidate.hasFitness()))
	        {
	            inGeneration.put(candidate.getName(), candidate);
	        }
	    }
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Draws orange segments from the selected candidate back through every
	 * parent named by {@link CandidateLW#getGeneratingSource()}, and through
	 * the parents of those parents, until a candidate has no parent links.
	 */
	private void showLineage(CandidateLW selected)
	{
	    clearLineage();
	    if (selected == null)
	        return;
	    displayedLineage.addAll(lineageMembers(selected,
	            candidatesByGenerationAndName));
	    int seriesIndex = 0;
	    for (double[][] segment : lineageSegments(selected,
	            candidatesByGenerationAndName))
	    {
	        datasetLineage.addSeries("Lineage_" + seriesIndex, segment);
	        seriesIndex++;
	    }
	    updateFamilyExportButtons();
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Draws segments from the selected candidate to each later candidate whose
	 * generating source names it as a direct parent. Mutations use one color
	 * and crossovers another. Offspring of those offspring are not included.
	 */
	private void showPosterity(CandidateLW selected)
	{
	    clearPosterity();
	    if (selected == null)
	        return;
	    PosterityLines lines = posteritySegments(selected,
	            candidatesByGenerationAndName);
	    displayedMutationPosterity.addAll(lines.mutationCandidates);
	    displayedCrossoverPosterity.addAll(lines.crossoverCandidates);
	    int seriesIndex = 0;
	    for (double[][] segment : lines.mutations)
	    {
	        datasetPosterityMutation.addSeries(
	                "PosterityMutation_" + seriesIndex, segment);
	        seriesIndex++;
	    }
	    seriesIndex = 0;
	    for (double[][] segment : lines.crossovers)
	    {
	        datasetPosterityCrossover.addSeries(
	                "PosterityCrossover_" + seriesIndex, segment);
	        seriesIndex++;
	    }
	    updateFamilyExportButtons();
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Assigns the next evolution-plot slot to {@code series} and attaches
	 * both the dataset and its renderer at that slot.
	 */
	private void bindEvoSeries(String series, XYDataset dataset,
	        XYItemRenderer renderer)
	{
	    int number = evoSeriesNumber.size();
	    evoSeriesNumber.put(series, number);
	    evoPlot.setDataset(number, dataset);
	    evoPlot.setRenderer(number, renderer);
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Shows or hides the plot slot registered under {@code series}. A slot
	 * that holds one series toggles that series. Lineage and posterity hold
	 * one series per segment, so the whole slot is toggled.
	 */
	private void setEvoSeriesVisible(String series, boolean visible)
	{
	    int number = evoSeriesNumber.get(series);
	    XYItemRenderer renderer = evoPlot.getRenderer(number);
	    if (SERIES_LINEAGE.equals(series)
	            || SERIES_POSTERITY_MUTATION.equals(series)
	            || SERIES_POSTERITY_CROSSOVER.equals(series))
	        renderer.setDefaultSeriesVisible(visible);
	    else
	        renderer.setSeriesVisible(SOLE_SERIES, visible);
	    updateFamilyExportButtons();
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Adds a Show/Hide entry whose label color is the color of {@code series}.
	 */
	private void addEvoSeriesCheckBox(JPopupMenu menu, String series,
	        Color color, boolean selected)
	{
	    JCheckBoxMenuItem item = new JCheckBoxMenuItem(series);
	    item.setForeground(color);
	    item.setSelected(selected);
	    item.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e)
            {
                setEvoSeriesVisible(series, item.isSelected());
            }
        });
	    menu.add(item);
	}
	
//-----------------------------------------------------------------------------
	
	private void clearLineage()
	{
	    clearDataset(datasetLineage);
	    displayedLineage.clear();
	    updateFamilyExportButtons();
	}
	
//-----------------------------------------------------------------------------
	
	private void clearPosterity()
	{
	    clearDataset(datasetPosterityMutation);
	    clearDataset(datasetPosterityCrossover);
	    displayedMutationPosterity.clear();
	    displayedCrossoverPosterity.clear();
	    updateFamilyExportButtons();
	}
	
//-----------------------------------------------------------------------------
	
	private static void clearDataset(DefaultXYDataset dataset)
	{
	    List<Comparable<?>> keys = new ArrayList<Comparable<?>>();
	    for (int i=0; i<dataset.getSeriesCount(); i++)
	        keys.add(dataset.getSeriesKey(i));
	    for (Comparable<?> key : keys)
	        dataset.removeSeries(key);
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Parent-child segments of the phylogenetic tree rooted at
	 * {@code selected}. Each segment is {@code double[2][2]}: row 0 is
	 * generation, row 1 is fitness, and the two columns are the child and
	 * one parent. A member without a fitness is kept in the walk but makes
	 * no segment.
	 */
	static List<double[][]> lineageSegments(CandidateLW selected,
	        Map<Integer,Map<String,CandidateLW>> index)
	{
	    List<double[][]> segments = new ArrayList<double[][]>();
	    if (selected == null || index == null)
	        return segments;
	    collectLineage(selected, index, new HashSet<String>(), segments, null);
	    return segments;
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Candidates joined by the lineage lines of {@code selected}, from the
	 * earliest generation to {@code selected} itself.
	 */
	static List<CandidateLW> lineageMembers(CandidateLW selected,
	        Map<Integer,Map<String,CandidateLW>> index)
	{
	    List<CandidateLW> members = new ArrayList<CandidateLW>();
	    if (selected == null || index == null)
	        return members;
	    collectLineage(selected, index, new HashSet<String>(), null, members);
	    members.sort(new Comparator<CandidateLW>() {
            @Override
            public int compare(CandidateLW a, CandidateLW b)
            {
                int byGeneration = Integer.compare(a.getGeneration(),
                        b.getGeneration());
                if (byGeneration != 0)
                    return byGeneration;
                return a.getName().compareTo(b.getName());
            }
        });
	    return members;
	}
	
//-----------------------------------------------------------------------------
	
	private static void collectLineage(CandidateLW candidate,
	        Map<Integer,Map<String,CandidateLW>> index, Set<String> visited,
	        List<double[][]> segments, List<CandidateLW> members)
	{
	    if (candidate == null || candidate.getName() == null)
	        return;
	    if (!visited.add(candidate.getGeneration() + "\0" + candidate.getName()))
	        return;
	    Provenance.Summary summary = Provenance.parseSummary(
	            candidate.getGeneratingSource());
	    if (summary == null)
	        return;
	    for (Provenance.ParentLink link : summary.getParents())
	    {
	        CandidateLW parent = findCandidate(index, link.getGenerationId(),
	                link.getCandidateId());
	        if (parent == null)
	            continue;
	        if (candidate.hasFitness() && parent.hasFitness())
	        {
	            if (segments != null)
	            {
	                double[][] segment = new double[2][2];
	                segment[0][0] = candidate.getGeneration();
	                segment[1][0] = candidate.getFitness();
	                segment[0][1] = parent.getGeneration();
	                segment[1][1] = parent.getFitness();
	                segments.add(segment);
	            }
	            addOnce(members, candidate);
	            addOnce(members, parent);
	        }
	        collectLineage(parent, index, visited, segments, members);
	    }
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Direct mutation and crossover offspring of {@code selected}. Each
	 * segment is {@code double[2][2]}: row 0 is generation, row 1 is fitness,
	 * column 0 is the offspring, and column 1 is {@code selected}. Only
	 * candidates in a later generation are considered, and only when their
	 * summary names {@code selected} as a parent.
	 */
	static PosterityLines posteritySegments(CandidateLW selected,
	        Map<Integer,Map<String,CandidateLW>> index)
	{
	    PosterityLines lines = new PosterityLines();
	    if (selected == null || selected.getName() == null || index == null
	            || !selected.hasFitness())
	        return lines;
	    List<Integer> generations = new ArrayList<Integer>(index.keySet());
	    Collections.sort(generations);
	    for (int generation : generations)
	    {
	        if (generation <= selected.getGeneration())
	            continue;
	        Map<String,CandidateLW> inGeneration = index.get(generation);
	        if (inGeneration == null)
	            continue;
	        List<String> names = new ArrayList<String>(inGeneration.keySet());
	        Collections.sort(names);
	        for (String name : names)
	        {
	            CandidateLW child = inGeneration.get(name);
	            if (child == null || !child.hasFitness())
	                continue;
	            Provenance.Summary summary = Provenance.parseSummary(
	                    child.getGeneratingSource());
	            if (summary == null)
	                continue;
	            if (summary.getSource() != Provenance.Source.MUTATION
	                    && summary.getSource() != Provenance.Source.CROSSOVER)
	                continue;
	            if (!namesParent(summary, selected))
	                continue;
	            double[][] segment = new double[2][2];
	            segment[0][0] = child.getGeneration();
	            segment[1][0] = child.getFitness();
	            segment[0][1] = selected.getGeneration();
	            segment[1][1] = selected.getFitness();
	            if (summary.getSource() == Provenance.Source.MUTATION)
	            {
	                lines.mutations.add(segment);
	                lines.mutationCandidates.add(child);
	            }
	            else
	            {
	                lines.crossovers.add(segment);
	                lines.crossoverCandidates.add(child);
	            }
	        }
	    }
	    return lines;
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Mutation and crossover segments returned by
	 * {@link #posteritySegments(CandidateLW, Map)}.
	 */
	static final class PosterityLines
	{
	    final List<double[][]> mutations = new ArrayList<double[][]>();
	    final List<double[][]> crossovers = new ArrayList<double[][]>();
	    final List<CandidateLW> mutationCandidates =
	            new ArrayList<CandidateLW>();
	    final List<CandidateLW> crossoverCandidates =
	            new ArrayList<CandidateLW>();
	}
	
//-----------------------------------------------------------------------------
	
	private static boolean namesParent(Provenance.Summary summary,
	        CandidateLW parent)
	{
	    for (Provenance.ParentLink link : summary.getParents())
	    {
	        if (link.getGenerationId() == parent.getGeneration()
	                && parent.getName().equals(link.getCandidateId()))
	            return true;
	    }
	    return false;
	}
	
//-----------------------------------------------------------------------------
	
	private static void addOnce(List<CandidateLW> members, CandidateLW candidate)
	{
	    if (members == null || candidate == null || candidate.getName() == null)
	        return;
	    for (CandidateLW existing : members)
	    {
	        if (existing.getGeneration() == candidate.getGeneration()
	                && candidate.getName().equals(existing.getName()))
	            return;
	    }
	    members.add(candidate);
	}
	
//-----------------------------------------------------------------------------
	
	private void updateFamilyExportButtons()
	{
	    if (exportLineage == null || exportPosterity == null)
	        return;
	    exportLineage.setEnabled(isFamilySeriesShown(SERIES_LINEAGE)
	            && !displayedLineage.isEmpty());
	    boolean mutationShown = isFamilySeriesShown(SERIES_POSTERITY_MUTATION)
	            && !displayedMutationPosterity.isEmpty();
	    boolean crossoverShown = isFamilySeriesShown(SERIES_POSTERITY_CROSSOVER)
	            && !displayedCrossoverPosterity.isEmpty();
	    exportPosterity.setEnabled(mutationShown || crossoverShown);
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * @return whether the family-tree slot is currently drawn. A slot with no
	 * explicit per-series flag follows its default visibility.
	 */
	private boolean isFamilySeriesShown(String series)
	{
	    if (evoPlot == null || !evoSeriesNumber.containsKey(series))
	        return false;
	    XYItemRenderer renderer = evoPlot.getRenderer(
	            evoSeriesNumber.get(series));
	    if (renderer == null)
	        return false;
	    Boolean visible = renderer.getSeriesVisible(SOLE_SERIES);
	    if (visible == null)
	        visible = renderer.getDefaultSeriesVisible();
	    return visible == null || visible.booleanValue();
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Asks for an SDF file and writes {@code candidates} into it, in
	 * generation order, by concatenating their candidate files.
	 */
	private void exportCandidates(List<CandidateLW> candidates)
	{
	    if (candidates == null || candidates.isEmpty())
	        return;
	    denoptim.files.FileAndFormat destination =
	            GUIFileSaver.pickFileForSavingCandidates(exportLineage);
	    if (destination == null)
	        return;
	    List<CandidateLW> ordered = new ArrayList<CandidateLW>();
	    for (CandidateLW candidate : candidates)
	        addOnce(ordered, candidate);
	    ordered.sort(new Comparator<CandidateLW>() {
            @Override
            public int compare(CandidateLW a, CandidateLW b)
            {
                int byGeneration = Integer.compare(a.getGeneration(),
                        b.getGeneration());
                if (byGeneration != 0)
                    return byGeneration;
                return a.getName().compareTo(b.getName());
            }
        });
	    List<String> paths = new ArrayList<String>();
	    for (CandidateLW candidate : ordered)
	    {
	        String path = candidate.getPathToFile();
	        if (path == null || !new File(path).isFile())
	        {
	            JOptionPane.showMessageDialog(exportLineage,
	                    "Cannot export. Candidate file not found for "
	                    + candidate.getName() + " in generation "
	                    + candidate.getGeneration() + ".",
	                    "Error", JOptionPane.ERROR_MESSAGE);
	            return;
	        }
	        paths.add(path);
	    }
	    File output = destination.file;
	    if (output.exists() && !output.delete())
	    {
	        JOptionPane.showMessageDialog(exportLineage,
	                "Cannot overwrite " + output.getAbsolutePath() + ".",
	                "Error", JOptionPane.ERROR_MESSAGE);
	        return;
	    }
	    try
	    {
	        FileUtils.mergeIntoOneFile(output.getAbsolutePath(), paths);
	    } catch (IOException e)
	    {
	        JOptionPane.showMessageDialog(exportLineage,
	                "Could not write " + output.getAbsolutePath() + ". "
	                + e.getMessage(),
	                "Error", JOptionPane.ERROR_MESSAGE);
	    }
	}
	
//-----------------------------------------------------------------------------
	
	private static CandidateLW findCandidate(
	        Map<Integer,Map<String,CandidateLW>> index, int generation,
	        String name)
	{
	    if (index == null || name == null)
	        return null;
	    Map<String,CandidateLW> inGeneration = index.get(generation);
	    if (inGeneration == null)
	        return null;
	    return inGeneration.get(name);
	}
	
//-----------------------------------------------------------------------------
  	
}
