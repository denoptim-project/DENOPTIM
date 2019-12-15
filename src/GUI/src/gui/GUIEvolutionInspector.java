package gui;

import gui.GUICardPanel.removeCardActionListener;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.UIManager;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.editor.ChartEditor;
import org.jfree.chart.editor.ChartEditorManager;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.DefaultXYDataset;

import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMMolecule;


/**
 * A panel that allows to inspect the output of an artificial evolution 
 * experiment. 
 * 
 * @author Marco Foscato
 */

public class GUIEvolutionInspector extends GUICardPanel
{
	/**
	 * Version UID
	 */
	private static final long serialVersionUID = -8303012362366503382L;
	
	/**
	 * Unique identified for instances of this handler
	 */
	public static AtomicInteger evolInspectorTabUID = new AtomicInteger(1);
	
	private JToolBar toolBar;
	private JMenu expMenu;
	private JMenu plotMenu;
	private JSplitPane centralPanel;
	private JPanel rightPanel;
	private JPanel leftPanel;
	
	
//-----------------------------------------------------------------------------
	
	/**
	 * Constructor
	 */
	public GUIEvolutionInspector(GUIMainPanel mainPanel)
	{
		super(mainPanel, "Evolution Inspector #" 
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
		toolBar = new JToolBar();
		toolBar.setFloatable(false);
		expMenu = new JMenu("Experiment");
		toolBar.add(expMenu);
		plotMenu = new JMenu("Plot");
		toolBar.add(plotMenu);
		this.add(toolBar,BorderLayout.NORTH);
		
		// Setting structure of central panel	
		centralPanel = new JSplitPane();
		centralPanel.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		centralPanel.setOneTouchExpandable(true);
		rightPanel = new JPanel(); 
		rightPanel.setLayout(new BorderLayout());
		centralPanel.setRightComponent(rightPanel);
		leftPanel = new JPanel();
		centralPanel.setLeftComponent(leftPanel);
		centralPanel.setDividerLocation(0.5);
		this.add(centralPanel,BorderLayout.CENTER);

		// Button to the bottom of the card
		JPanel commandsPane = new JPanel();
		this.add(commandsPane, BorderLayout.SOUTH);
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

	public void importEvolutionRunData(File file) {

		mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		//TODO del
		System.out.println("Importing data...");
		
		ArrayList<DENOPTIMMolecule> allIndividuals = 
											new ArrayList<DENOPTIMMolecule>();
		int molsWithFitness = 0;
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
			
			//TODO del
			System.out.println("Reading Generation "+genId);
			
			for (File fitFile : genFolder.listFiles(new FileFilter() {
				
				@Override
				public boolean accept(File pathname) {
					if (pathname.getName().endsWith("FIT.sdf"))
					{
						return true;
					}
					return false;
				}
			}))
			{
				DENOPTIMMolecule one;
				try {
					one = DenoptimIO.readDENOPTIMMolecules(fitFile,false).get(0);
				} catch (DENOPTIMException e1) {
					e1.printStackTrace();
					JOptionPane.showMessageDialog(null,
			                "Could not read data from to '" + fitFile + "'!.",
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
		
		//TODO del
		System.out.println("Imported "+allIndividuals.size()+" individuals.");
		
		DefaultXYDataset dataXY = new DefaultXYDataset(); 
        double[][] candidatesData = new double[2][molsWithFitness];
        int j = -1;
        for (int i=0; i<allIndividuals.size(); i++)
        {
        	DENOPTIMMolecule mol = allIndividuals.get(i);
        	if (!mol.hasFitness())
        	{
        		continue;
        	}
        	j++;
        	candidatesData[0][j] = mol.getGeneration();
        	candidatesData[1][j] = mol.getMoleculeFitness();
        }
		
		//TODO del
		System.out.println("Data prepared for chart");
        
		dataXY.addSeries("Candidates", candidatesData);
		
		//TODO: add max, min, average per generation
		
		//TODO: display the candidates that hit a mol error
		
		JFreeChart chart = ChartFactory.createScatterPlot(
	            null,                         // plot title
	            "Generation",                 // x axis label
	            "Fitness",                    // y axis label
	            dataXY,                       // all plottable data
	            PlotOrientation.VERTICAL,  
	            false,                        // include legend
	            false,                        // tool tips
	            false                         // urls
	        );
		
		ChartPanel chartPanel = new ChartPanel(chart);
		
		// Adapt chart at the size of the panel
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
		
		
		chartPanel.addChartMouseListener(new ChartMouseListener() {
			
			@Override
			public void chartMouseMoved(ChartMouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void chartMouseClicked(ChartMouseEvent e) {
				System.out.println("Clicked on chart "+e);
				
			}
		});
		
		rightPanel.add(chartPanel,BorderLayout.CENTER);
		
		mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
		
//-----------------------------------------------------------------------------
  	
}
