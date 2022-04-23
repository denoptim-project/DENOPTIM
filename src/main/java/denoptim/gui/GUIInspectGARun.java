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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
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
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.files.FileUtils;
import denoptim.graph.APClass;
import denoptim.graph.CandidateLW;
import denoptim.io.DenoptimIO;
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
	private JPanel ctrlPanelLeft;
	private JPanel ctrlPanelRight;
	private JSplitPane centralPanel;
	private JPanel rightPanel;
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
	private XYPlot plot;
	private JFreeChart chart;
	private ChartPanel chartPanel;
	
	
	private final String NL = System.getProperty("line.separator");

	/**
	 * Button offering the possibility to load the graph inspector for a 
	 * selected item.
	 */
	private JButton openGraph;
	
	/**
	 * Storage of pathname to the item selected in the chart. This is used to
	 * enable loading the graph inspector at any time after selection of the
	 * item in the chart.
	 */
    private String pathToSelectedItem;
	
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
		//    |-> plot (RIGHT)
		
		// Creating local tool bar
		ctrlPanelLeft = new JPanel(new FlowLayout(FlowLayout.LEFT));
		ctrlPanelLeft.add(new JLabel("Plot Features:"));
		JCheckBox ctrlMin = new JCheckBox("Minimum");
		ctrlMin.setToolTipText("The min fitness value in the population.");
		ctrlMin.setSelected(true);
		JCheckBox ctrlMax = new JCheckBox("Maximum");
		ctrlMax.setToolTipText("The max fitness value in the population.");
		ctrlMax.setSelected(true);
		JCheckBox ctrlMean = new JCheckBox("Mean");
		ctrlMean.setToolTipText("The mean fitness value in the population.");
		JCheckBox ctrlMedian = new JCheckBox("Median");
		ctrlMedian.setToolTipText("The median of the fitness in the "
				+ "population.");
		ctrlMin.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (ctrlMin.isSelected())
				{
					plot.getRenderer(2).setSeriesVisible(0,true);
				}
				else
				{
					plot.getRenderer(2).setSeriesVisible(0,false);
				}
			}
		});
		ctrlMax.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (ctrlMax.isSelected())
				{
					plot.getRenderer(3).setSeriesVisible(0,true);
				}
				else
				{
					plot.getRenderer(3).setSeriesVisible(0,false);
				}
			}
		});
		ctrlMean.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (ctrlMean.isSelected())
				{
					plot.getRenderer(4).setSeriesVisible(0,true);
				}
				else
				{
					plot.getRenderer(4).setSeriesVisible(0,false);
				}
			}
		});
		ctrlMedian.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (ctrlMedian.isSelected())
				{
					plot.getRenderer(5).setSeriesVisible(0,true);
				}
				else
				{
					plot.getRenderer(5).setSeriesVisible(0,false);
				}
			}
		});
		ctrlPanelLeft.add(ctrlMin);
		ctrlPanelLeft.add(ctrlMax);
		ctrlPanelLeft.add(ctrlMean);
		ctrlPanelLeft.add(ctrlMedian);
		JButton rstView = new JButton("Reset Chart View");
		rstView.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				plot.getDomainAxis().setAutoRange(true);
				plot.getRangeAxis().setAutoRange(true);
				plot.getDomainAxis().setLowerBound(-0.5);			
			}
		});
		ctrlPanelLeft.add(rstView);
		
	    openGraph = new JButton("Open Candidate's Graph");
        openGraph.setEnabled(false); //Enables only upon selection of an item
	    openGraph.setToolTipText("Open a new tab for inspecting the "
	            + "DENOPTIMGraph of the selected candidate.");
	    openGraph.addActionListener(new ActionListener() {
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
	    ctrlPanelLeft.add(openGraph);

		ctrlPanelRight = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		lblTotItems = new JLabel("No item loaded");
		lblTotItems.setHorizontalAlignment(SwingConstants.RIGHT);
		lblTotItems.setPreferredSize(new Dimension(300,28));
		ctrlPanelRight.add(lblTotItems);
		ctrlPanel = new JPanel();
        GroupLayout lyoCtrlPanel = new GroupLayout(ctrlPanel);
        ctrlPanel.setLayout(lyoCtrlPanel);
        lyoCtrlPanel.setAutoCreateGaps(true);
        lyoCtrlPanel.setAutoCreateContainerGaps(true);
        lyoCtrlPanel.setHorizontalGroup(lyoCtrlPanel.createSequentialGroup()
                    .addComponent(ctrlPanelLeft)
                    .addComponent(ctrlPanelRight));
        lyoCtrlPanel.setVerticalGroup(lyoCtrlPanel.createParallelGroup()
			        .addComponent(ctrlPanelLeft)
			        .addComponent(ctrlPanelRight));
		this.add(ctrlPanel,BorderLayout.NORTH);
		
		
		// Setting structure of central panel	
		centralPanel = new JSplitPane();
		centralPanel.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		centralPanel.setOneTouchExpandable(true);
		rightPanel = new JPanel(); 
		rightPanel.setLayout(new BorderLayout());
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
			
			if (!FileUtils.checkExists(genSummary.getAbsolutePath()))
			{
				JOptionPane.showMessageDialog(parent,
		                "<html>File '" + genSummary + "' not found!<br>"
		                + "There will be holes in the min/max/mean profile."
		                + "</html>",
		                "Error",
		                JOptionPane.PLAIN_MESSAGE,
		                UIManager.getIcon("OptionPane.errorIcon"));
			}
			try {
				popProperties.put(genId, DenoptimIO.readPopulationProps(
						genSummary));
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
			
			// Read DENOPTIMMolecules
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
		plot = new XYPlot(null,xAxis,yAxis,null);
		
		chart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT,plot,false);
		
		plot.getDomainAxis().setLowerBound(-0.5); //min X-axis
		plot.setBackgroundPaint(Color.WHITE);
		plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
		plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
		plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);
		// The following brings the points for datasets of selected
		// items and the main dataset in front of the
		// mean/min/max/median lines, thus allowing selection of points
		// even in presence of these line series, which would otherwise (if in
		// front-most layer) prevent the mouse clicked event to identify the 
		// item from datasetAllFit, thus preventing to display the molecular
		// representation of that item.
		plot.setDatasetRenderingOrder(DatasetRenderingOrder.REVERSE);
		
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
        plot.setDataset(0, datasetSelected); 
        plot.setRenderer(0, renderer0);
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
        plot.setDataset(1, datasetAllFit);
        plot.setRenderer(1, renderer1);
        renderer1.setSeriesShape(0, shape1);
        renderer1.setSeriesPaint(0, new Color(192, 192, 192, 60));
        renderer1.setSeriesFillPaint(0, new Color(192, 192, 192, 60));
        renderer1.setSeriesOutlinePaint(0, Color.GRAY);
        renderer1.setUseOutlinePaint(true);
        renderer1.setUseFillPaint(true);
        
        // min fitness in the population
        XYLineAndShapeRenderer renderer2 = 
        		new XYLineAndShapeRenderer(true, false);
        plot.setDataset(2, datasetPopMin);
        plot.setRenderer(2, renderer2);
        renderer2.setSeriesPaint(0, Color.blue);
        
        // max fitness in the population
        XYLineAndShapeRenderer renderer3 = 
        		new XYLineAndShapeRenderer(true, false);
        plot.setDataset(3, datasetPopMax);
        plot.setRenderer(3, renderer3);
        renderer3.setSeriesPaint(0, Color.blue);
        
        // mean fitness in the population
        XYLineAndShapeRenderer renderer4 = 
        		new XYLineAndShapeRenderer(true, false);
        plot.setDataset(4, datasetPopMean);
        plot.setRenderer(4, renderer4);
        renderer4.setSeriesPaint(0, Color.red);
        renderer4.setSeriesStroke(0, new BasicStroke(
                2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                1.0f, new float[] {10.0f, 6.0f}, 0.0f));
        renderer4.setSeriesVisible(0, false);
        
        // median fitness in the population
        XYLineAndShapeRenderer renderer5 = 
        		new XYLineAndShapeRenderer(true, false);
        plot.setDataset(5, datasetPopMedian);
        plot.setRenderer(5, renderer5);
        renderer5.setSeriesPaint(0, Color.decode("#22BB22"));   
        renderer5.setSeriesStroke(0, new BasicStroke(
                    2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                    1.0f, new float[] {3.0f, 6.0f}, 0.0f));
        renderer5.setSeriesVisible(0, false);
        
		// Create the actual panel that contains the chart
		chartPanel = new ChartPanel(chart);
		
		// Adapt chart size to the size of the panel
		rightPanel.addComponentListener(new ComponentAdapter() {
	        @Override
	        public void componentResized(ComponentEvent e) {
	        	chartPanel.setMaximumDrawHeight(e.getComponent().getHeight());
	        	chartPanel.setMaximumDrawWidth(e.getComponent().getWidth());
	        	chartPanel.setMinimumDrawWidth(e.getComponent().getWidth());
	        	chartPanel.setMinimumDrawHeight(e.getComponent().getHeight());
	        	// WARNING: this update is needed to make the new size affective
	        	// also after movement of the JSplitPane divider, which is
	        	// otherwise felt by this listener but the new sizes do not
	        	// take effect. 
	        	ChartEditor ce = ChartEditorManager.getChartEditor(chart);
	        	ce.updateChart(chart);
	        }
	    });
		
		// Setting toolTip when on top of an series item in the chart
        //TODO deal with superposed points by adding all their names to tip text
		XYToolTipGenerator ttg = new XYToolTipGenerator() {
			@Override
			public String generateToolTip(XYDataset data, int sId, int itemId)
			{
				return candsWithFitnessMap.get(itemId).getName();
			}
		};
		plot.getRenderer().setSeriesToolTipGenerator(0, ttg);
		
		// Clock-based selection of item, possibly displaying mol structure
		chartPanel.addChartMouseListener(new ChartMouseListener() {
			
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
						int initPos = allIndividuals.indexOf(item);
		                double tolerance = Math.abs(plot.getRangeAxis()
		                        .getRange().getLength() * 0.02);
		                int maxItems = 25;
		                int nItems = 0;
		                List<CandidateLW> overlappingItems = 
		                        new ArrayList<CandidateLW>();
		                while (nItems<maxItems && 
		                        (initPos+nItems)<allIndividuals.size())
		                {
		                    CandidateLW c = allIndividuals.get(initPos + nItems);
		                    if (!c.hasFitness())
		                    {
	                            nItems++;
		                        continue;
		                    }
		                    double delta = Math.abs(item.getFitness() 
		                            - c.getFitness());
		                    if (delta > tolerance)
		                        break;
		                    overlappingItems.add(c);
		                    nItems++;
		                }
		                nItems = 1;
		                while (nItems<maxItems && (initPos-nItems)>-1)
                        {
                            CandidateLW c = allIndividuals.get(initPos - nItems);
                            if (!c.hasFitness())
                            {
                                nItems++;
                                continue;
                            }
                            double delta = Math.abs(item.getFitness() 
                                    - c.getFitness());
                            if (delta > tolerance)
                                break;
                            overlappingItems.add(0,c);
                            nItems++;
                        }
		                CandidateLW choosenItem = choseAmongPossiblyOverlapping(
		                        chartPanel, overlappingItems);
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
		
		rightPanel.add(chartPanel,BorderLayout.CENTER);
		
		mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
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
		plot.setDataset(0, datasetSelected);
		
		// Update the molecular viewer
		molViewer.loadChemicalStructureFromFile(item.getPathToFile());
		pathToSelectedItem = item.getPathToFile();
        openGraph.setEnabled(true);

        mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
	
//-----------------------------------------------------------------------------
	
	private void renderViewWithoutSelectedItems()
	{
		datasetSelected.removeSeries("Selected_candidates");
		molViewer.clearAll();
		openGraph.setEnabled(false);
	}
	
//-----------------------------------------------------------------------------
  	
}
