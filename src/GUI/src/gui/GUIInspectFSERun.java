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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.editor.ChartEditor;
import org.jfree.chart.editor.ChartEditorManager;
import org.jfree.chart.entity.PlotEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.molecule.DENOPTIMMolecule;


/**
 * A panel that allows to inspect the output of an combinatorial 
 * experiment exploring a fragment space.
 * 
 * @author Marco Foscato
 */

public class GUIInspectFSERun extends GUICardPanel
{
	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 2245706077350445364L;

	/**
	 * Unique identified for instances of this handler
	 */
	public static AtomicInteger fseInspectorTabUID = new AtomicInteger(1);
	
	private JPanel ctrlPanel;
	private JPanel ctrlPanelLeft;
	private JPanel ctrlPanelRight;
	private JSplitPane centralPanel;
	private JPanel rightPanel;
	private MoleculeViewPanel molViewer;
	private JPanel chartHolderPanel;
	
	private JComboBox<String> cmbPlotType;
	
	private ArrayList<DENOPTIMMolecule> allItems;
	private int molsWithFitness = 0;
	private int minLevel = 1;
	private int maxLevel = -1;
	private JLabel lblTotItems;
	
	private Map<Integer,DENOPTIMMolecule> mapCandsInByLevel;
	private ArrayList<DENOPTIMMolecule> sorted;
	
	// WARNING: integer key in the map is is just a 
	// locally generated unique 
	// identifier that has NO RELATION to level/molId/fitness
	// The Map 'mapCandsInByLevel' serve specifically to convert
	// the unique key into a DENOPTIMMolecule
	
	private DefaultXYDataset datasetAllFit;
	private DefaultXYDataset datasetSorted;
	private DefaultXYDataset datasetSelectedLev = new DefaultXYDataset();
	private DefaultXYDataset datasetSelectedOrd = new DefaultXYDataset();
	
	private JFreeChart chartByLevel;
	private JFreeChart chartBySorted;
	private ChartPanel chartPanelByLevel;
	private ChartPanel chartPanelSorted;
	
	
//-----------------------------------------------------------------------------
	
	/**
	 * Constructor
	 */
	public GUIInspectFSERun(GUIMainPanel mainPanel)
	{
		super(mainPanel, "FSERun Inspector #" 
					+ fseInspectorTabUID.getAndIncrement());
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
		JButton rstView = new JButton("Reset Chart View");
		rstView.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				resetView();
			}
		});
		ctrlPanelLeft.add(rstView);
		
		cmbPlotType = new JComboBox<String>(new String[] {
				"Plot Sorted List of Candidates",
				"Plot Candidates by Level"});
		cmbPlotType.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (cmbPlotType.getSelectedIndex() == 0)
				{
					((CardLayout) chartHolderPanel.getLayout()).show(chartHolderPanel,
							"sorted");
				}
				else
				{
					((CardLayout) chartHolderPanel.getLayout()).show(chartHolderPanel,
							"byLevel");
				}
			}
		});
		ctrlPanelLeft.add(cmbPlotType);
		
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
		btnCanc.setToolTipText("Closes this FSERun Inspector.");
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
	                    + "<li>Click on a dot in the chart to disply the "
	                    + "corresponding molecular "
	                    + "representation.</li>"
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
				JOptionPane.showMessageDialog(null, String.format(txt, 300),
                    "Tips",
                    JOptionPane.PLAIN_MESSAGE);
			}
		});
		commandsPane.add(btnHelp);
		
	}
	
//-----------------------------------------------------------------------------

	private void resetView() 
	{
		if (cmbPlotType.getSelectedIndex() == 1)
		{
			resetViewInPlotByLevel();
		}
		else
		{
			resetViewInPlotOfSortedList();		
		}
	}
	
//-----------------------------------------------------------------------------
	
	private void resetViewInPlotByLevel()
	{
		chartByLevel.getXYPlot().getRangeAxis().setAutoRange(true);
		chartByLevel.getXYPlot().getDomainAxis().setLowerBound(
				minLevel-0.5);
		chartByLevel.getXYPlot().getDomainAxis().setUpperBound(
				maxLevel+0.5);
	}
	
//-----------------------------------------------------------------------------
	
	private void resetViewInPlotOfSortedList()
	{
		chartBySorted.getXYPlot().getDomainAxis().setLowerBound(
				molsWithFitness * -0.05);
		chartBySorted.getXYPlot().getDomainAxis().setUpperBound(
				molsWithFitness * 1.05);
		chartBySorted.getXYPlot().getRangeAxis().setAutoRange(true);
	}
	
//-----------------------------------------------------------------------------

	public void importFSERunData(File folder) {

		if (!folder.isDirectory() || !folder.exists())
		{
			JOptionPane.showMessageDialog(null,
	                "Could not read data from folder '" + folder+ "'!",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;
		}
		mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		System.out.println("Importing data from '" + folder + "'... ");
		
		allItems = new ArrayList<DENOPTIMMolecule>();
		boolean skippFurtherErrors = false;
		for (File molFile : folder.listFiles(new FileFilter() {
			
			@Override
			public boolean accept(File pathname) {
				if (pathname.getName().startsWith(
						DENOPTIMConstants.FITFILENAMEPREFIX)
					&& pathname.getName().endsWith(
						DENOPTIMConstants.FITFILENAMEEXTOUT)
					&& !pathname.isDirectory())
				{
					return true;
				}
				return false;
			}
		}))
		{			
			DENOPTIMMolecule mol = new DENOPTIMMolecule();
			try {
				mol = DenoptimIO.readDENOPTIMMolecules(
						molFile,false).get(0);
			} catch (DENOPTIMException e1) {
				if (!skippFurtherErrors)
				{
					e1.printStackTrace();
					mainPanel.setCursor(Cursor.getPredefinedCursor(
							Cursor.DEFAULT_CURSOR));
	
					JPanel msgPanel = new JPanel(new GridLayout(2, 1));
					String msg = "<html><body width='%1s'>Could not read data "
							+ "from '" + molFile + "'. Hint on cause: "
							+ e1.getMessage() + " Should we try to "
							+ "visualize the results anyway?</html>";
					JLabel text = new JLabel(String.format(msg, 450));
					JCheckBox cb = new JCheckBox("Remember decision");
					cb.setSelected(false);
					msgPanel.add(text);
					msgPanel.add(cb);
					String[] options = new String[]{"Yes", "Abandon"};
					int res = JOptionPane.showOptionDialog(null,
							msgPanel,
							"ERROR",
							JOptionPane.DEFAULT_OPTION,
			                JOptionPane.QUESTION_MESSAGE,
			                UIManager.getIcon("OptionPane.errorIcon"),
			                options,
			                options[1]);
					if (cb.isSelected())
					{
						skippFurtherErrors = true;
					}
					switch (res)
					{
						case 0:
							break;
							
						case 1:
							return;
					}
				}
			}
			
			if (mol.hasFitness())
			{
				molsWithFitness++;
			}
			
			int lev = mol.getLevel();
			if (lev > maxLevel)
			{
				maxLevel = lev;
			} else if (lev < minLevel)
			{
				minLevel = lev;
			}
			
			allItems.add(mol);
		}
		
		System.out.println("Imported "+allItems.size()+" individuals.");
		
		lblTotItems.setText("Found "+allItems.size()+" candidates ("
				+molsWithFitness+" with fitness)");
		
		// Process data and organize them into series for the plot
        double[][] candsWithFitnessDataPerLevel = new double[2][molsWithFitness];
        mapCandsInByLevel = new HashMap<Integer,DENOPTIMMolecule>();
		int j= -1;
        for (int i=0; i<allItems.size(); i++)
        {
        	DENOPTIMMolecule mol = allItems.get(i);
        	if (!mol.hasFitness())
        	{
        		continue;
        	}
        	
        	// WARNING: itemId in the data is "j" and is just a unique 
        	// identifier that has NO RELATION to generation/molId/fitness
        	// The Map 'mapCandsInByLevel' serve specifically to convert
        	// the itemId 'j' into a DENOPTIMMolecule
        	
        	j++;
        	mapCandsInByLevel.put(j, mol);
        	candsWithFitnessDataPerLevel[0][j] = mol.getLevel();
        	candsWithFitnessDataPerLevel[1][j] = mol.getFitness();
        }
        
        sorted = new ArrayList<DENOPTIMMolecule>();
        sorted.addAll(mapCandsInByLevel.values());
        sorted.sort(new Comparator<DENOPTIMMolecule>() {
			public int compare(DENOPTIMMolecule a, DENOPTIMMolecule b) {
				return Double.compare(a.getFitness(),
						b.getFitness());
			}
		});
        
        double[][] candsWithFitnessDataSorted = new double[2][molsWithFitness];
        for (int i=0; i<molsWithFitness; i++)
        {
        	DENOPTIMMolecule mol = sorted.get(i);
        	candsWithFitnessDataSorted[0][i] = i;
        	candsWithFitnessDataSorted[1][i] = mol.getFitness();
        }
        
        datasetAllFit = new DefaultXYDataset(); 
		datasetAllFit.addSeries("Candidates_with_fitness", 
				candsWithFitnessDataPerLevel);
		
		datasetSorted = new DefaultXYDataset();
		datasetSorted.addSeries("Sorted_candidates", 
				candsWithFitnessDataSorted);
		
		//TODO: somehow collect and display the candidates that hit a mol error
		//      Could it be a histogram (#failed x level) below the levels plot
		//      Failed items should be selectable by click, to allow 
		//      visualization.
		
		chartByLevel = ChartFactory.createScatterPlot(
	            null,                         // plot title
	            "Level",                           // x axis label
	            "Fitness",                    // y axis label
	            datasetAllFit,                // all items with fitness
	            PlotOrientation.VERTICAL,  
	            false,                        // include legend
	            false,                        // tool tips
	            false                         // urls
	        );
		
		chartBySorted = ChartFactory.createScatterPlot(
	            null,                         // plot title
	            "",                           // x axis label
	            "Fitness",                    // y axis label
	            datasetSorted,                // all items with fitness
	            PlotOrientation.VERTICAL,  
	            false,                        // include legend
	            false,                        // tool tips
	            false                         // urls
	        );
		
		// Chart appearance
		XYPlot plotBL = (XYPlot) chartByLevel.getPlot();
		plotBL.getDomainAxis().setLowerBound(-0.5); //min X-axis
		plotBL.setBackgroundPaint(Color.WHITE);
		plotBL.setDomainGridlinePaint(Color.LIGHT_GRAY);
		plotBL.setRangeGridlinePaint(Color.LIGHT_GRAY);
		plotBL.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);
		plotBL.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
		// main dataset (all items with fitness)
		Shape shape0 = new Ellipse2D.Double(
				 -GUIPreferences.chartPointSize/2.0,
	             -GUIPreferences.chartPointSize/2.0,
	             GUIPreferences.chartPointSize, 
	             GUIPreferences.chartPointSize);
		XYLineAndShapeRenderer renderer0 = 
				(XYLineAndShapeRenderer) plotBL.getRenderer();
        renderer0.setSeriesShape(0, shape0);
        renderer0.setSeriesPaint(0, Color.decode("#848482"));
        renderer0.setSeriesOutlinePaint(0, Color.gray);
        
        // dataset of selected items
		Shape shape1 = new Ellipse2D.Double(
				 -GUIPreferences.chartPointSize*1.1/2.0,
	             -GUIPreferences.chartPointSize*1.1/2.0,
	             GUIPreferences.chartPointSize*1.1, 
	             GUIPreferences.chartPointSize*1.1);
        XYLineAndShapeRenderer renderer1 = new XYLineAndShapeRenderer();
        //now the dataset of selected is null. Created upon selection
        plotBL.setRenderer(1, renderer1);
        renderer1.setSeriesShape(0, shape1);
        renderer1.setSeriesPaint(0, Color.red);
        renderer1.setSeriesFillPaint(0, Color.red);
        renderer1.setSeriesOutlinePaint(0, Color.BLACK);
        renderer1.setUseOutlinePaint(true);
        renderer1.setUseFillPaint(true);
        
        // Chart appearance for chart of sorted
     	XYPlot  plotS = (XYPlot) chartBySorted.getPlot();
		plotS.getDomainAxis().setLowerBound(-0.5); //min X-axis
		plotS.setBackgroundPaint(Color.WHITE);
		plotS.setDomainGridlinePaint(Color.LIGHT_GRAY);
		plotS.setRangeGridlinePaint(Color.LIGHT_GRAY);
		plotS.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);
		plotS.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
		// main dataset (all items with fitness)
		XYLineAndShapeRenderer rendererS0 = 
				(XYLineAndShapeRenderer) plotS.getRenderer();
        rendererS0.setSeriesShape(0, shape0);
        rendererS0.setSeriesPaint(0, Color.decode("#848482"));
        rendererS0.setSeriesOutlinePaint(0, Color.gray);
        
        // dataset of selected items
        XYLineAndShapeRenderer rendererS1 = new XYLineAndShapeRenderer();
        plotS.setRenderer(1, rendererS1);
        rendererS1.setSeriesShape(0, shape1);
        rendererS1.setSeriesPaint(0, Color.red);
        rendererS1.setSeriesFillPaint(0, Color.red);
        rendererS1.setSeriesOutlinePaint(0, Color.BLACK);
        rendererS1.setUseOutlinePaint(true);
        rendererS1.setUseFillPaint(true);
     	
		// Create the actual panels that contains the charts
        chartPanelSorted = new ChartPanel(chartBySorted);
        resetViewInPlotOfSortedList();
		chartPanelByLevel = new ChartPanel(chartByLevel);
		resetViewInPlotByLevel();
		
		// Adapt chart size to the size of the panel
		rightPanel.addComponentListener(new ComponentAdapter() {
	        @Override
	        public void componentResized(ComponentEvent e) {
	        	chartPanelByLevel.setMaximumDrawHeight(
	        			e.getComponent().getHeight());
	        	chartPanelByLevel.setMaximumDrawWidth(
	        			e.getComponent().getWidth());
	        	chartPanelByLevel.setMinimumDrawWidth(
	        			e.getComponent().getWidth());
	        	chartPanelByLevel.setMinimumDrawHeight(
	        			e.getComponent().getHeight());
	        	chartPanelSorted.setMaximumDrawHeight(
	        			e.getComponent().getHeight());
	        	chartPanelSorted.setMaximumDrawWidth(
	        			e.getComponent().getWidth());
	        	chartPanelSorted.setMinimumDrawWidth(
	        			e.getComponent().getWidth());
	        	chartPanelSorted.setMinimumDrawHeight(
	        			e.getComponent().getHeight());
	        	// WARNING: this update is needed to make the new size affective
	        	// also after movement of the JSplitPane divider, which is
	        	// otherwise felt by this listener but the new sizes do not
	        	// take effect. 
	        	ChartEditor ceL = ChartEditorManager.getChartEditor(
	        			chartByLevel);
	        	ceL.updateChart(chartByLevel);
	        	ChartEditor ceS = ChartEditorManager.getChartEditor(
	        			chartBySorted);
	        	ceS.updateChart(chartBySorted);
	        }
	    });
		
		// Setting toolTip when on top of an series item in the chart
		XYToolTipGenerator ttg = new XYToolTipGenerator() {
			public String generateToolTip(XYDataset data, int sId, int itemId)
			{
				return mapCandsInByLevel.get(itemId).getName();
			}
		};
		chartByLevel.getXYPlot().getRenderer().setSeriesToolTipGenerator(0, 
				ttg);
		chartBySorted.getXYPlot().getRenderer().setSeriesToolTipGenerator(0, 
				ttg);
		
		// Click-based selection of item, possibly displaying mol structure
		chartPanelByLevel.addChartMouseListener(new ChartMouseListener() {
			
			public void chartMouseMoved(ChartMouseEvent e) {
				//nothing to do
			}
			
			public void chartMouseClicked(ChartMouseEvent e) 
			{
				if (e.getEntity() instanceof XYItemEntity)
				{
					int serId = ((XYItemEntity)e.getEntity()).getSeriesIndex();
					if (serId == 0)
					{
						int itemId = ((XYItemEntity) e.getEntity()).getItem();
						DENOPTIMMolecule mol = mapCandsInByLevel.get(itemId);
						renderViewWithSelectedItem(mol);
					}
					//do we do anything if we select other series? not now...
				}
				else if (e.getEntity() instanceof PlotEntity)
				{
					renderViewWithoutSelectedItems();
				}
			}
		});
		
		chartPanelSorted.addChartMouseListener(new ChartMouseListener() {
			
			public void chartMouseMoved(ChartMouseEvent e) {
				//nothing to do
			}
			
			public void chartMouseClicked(ChartMouseEvent e) 
			{
				if (e.getEntity() instanceof XYItemEntity)
				{
					int serId = ((XYItemEntity)e.getEntity()).getSeriesIndex();
					if (serId == 0)
					{
						int itemId = ((XYItemEntity) e.getEntity()).getItem();
						DENOPTIMMolecule mol = sorted.get(itemId);
						renderViewWithSelectedItem(mol);
					}
				}
				else if (e.getEntity() instanceof PlotEntity)
				{
					renderViewWithoutSelectedItems();
				}
			}
		});
		
		chartHolderPanel = new JPanel(new CardLayout());
		chartPanelByLevel.setName("byLevel");
		chartPanelSorted.setName("sorted");
		chartHolderPanel.add(chartPanelByLevel,"byLevel");
		chartHolderPanel.add(chartPanelSorted,"sorted");
		((CardLayout) chartHolderPanel.getLayout()).show(chartHolderPanel,
				"sorted");
		rightPanel.add(chartHolderPanel,BorderLayout.CENTER);
		
		mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
	
//-----------------------------------------------------------------------------
	
	private void renderViewWithSelectedItem(DENOPTIMMolecule mol)
	{	
		// NB: we just change the series of selected items
		// The charts are updated automatically
		
        double[][] selectedCandsDataLev = new double[2][1]; 
    	selectedCandsDataLev[0][0] = mol.getLevel();
    	selectedCandsDataLev[1][0] = mol.getFitness();
        datasetSelectedLev.removeSeries("Selected_candidates");
        datasetSelectedLev.addSeries("Selected_candidates", selectedCandsDataLev);
		chartByLevel.getXYPlot().setDataset(1, datasetSelectedLev);

		double[][] selectedCandsDataOrd = new double[2][1]; 
    	selectedCandsDataOrd[0][0] = sorted.indexOf(mol);
    	selectedCandsDataOrd[1][0] = mol.getFitness();        	
        datasetSelectedOrd.removeSeries("Selected_candidates");
        datasetSelectedOrd.addSeries("Selected_candidates", selectedCandsDataOrd);
		chartBySorted.getXYPlot().setDataset(1, datasetSelectedOrd);
		
		// Update the molecular viewer
		molViewer.loadChemicalStructureFromFile(mol.getSDFFile());
	}
	
//-----------------------------------------------------------------------------
	
	private void renderViewWithoutSelectedItems()
	{
		datasetSelectedLev.removeSeries("Selected_candidates");
		datasetSelectedOrd.removeSeries("Selected_candidates");
		molViewer.clearAll();
	}

//-----------------------------------------------------------------------------
  	
}
