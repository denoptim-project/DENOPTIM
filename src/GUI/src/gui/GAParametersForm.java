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

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;

import denoptimga.EAUtils;
import denoptimga.GAParameters;


/**
 * Form collecting input parameters for a genetic algorithm experiment
 */

public class GAParametersForm extends ParametersForm
{

    /**
	 * Version
	 */
	private static final long serialVersionUID = 5067352357196631445L;
	
	/**
	 * Unique identified for instances of this form
	 */
	public static AtomicInteger gaFormUID = new AtomicInteger(1);
	
    /**
     * Map connecting the parameter keyword and the field
     * containing the parameter value. 
     */
	private Map<String,Object> mapKeyFieldToValueField;
	
    JPanel block;    
    JPanel localBlock1;
    JPanel localBlock2;
    JPanel localBlock3;
    JPanel localBlock4;
    JPanel localBlockGraph;
    JPanel advOptsBlock;
    
    DefaultXYDataset subsProbData;
    JFreeChart graphSubProbJFChart;
	JPanel graphSubProbJFChartChartPanel;
    JPanel graphSubProbJFChartCtrlPanel;
    JPanel graphSpinnerPane;
    Double minLevelProbPlot = 0.0;
    Double maxLevelProbPlot = 4.0;
	
	JPanel lineSrcOrNew;
    JRadioButton rdbSrcOrNew;
    
    JPanel lineGASource;
    JLabel lblGASource;
    JTextField txtGASource;
    JButton btnGASource;
    JButton btnLoadGASource;
	
	String keyPar3 = "GA-RandomSeed";
    JPanel linePar3;
    JLabel lblPar3;
    JTextField txtPar3;

    String keyPar1 = "GA-PrecisionLevel";
    JPanel linePar1;
    JLabel lblPar1;
    JTextField txtPar1;

    String keyPar2 = "GA-PrintLevel";
    JPanel linePar2;
    JLabel lblPar2;
    JComboBox<String> cmbPar2;

    String keyPar5 = "GA-SortByIncreasingFitness";
    JPanel linePar5;
    JRadioButton rdbPar5;

    String keyPar6 = "GA-PopulationSize";
    JPanel linePar6;
    JLabel lblPar6;
    JTextField txtPar6;

    String keyPar7 = "GA-NumChildren";
    JPanel linePar7;
    JLabel lblPar7;
    JTextField txtPar7;

    String keyPar8 = "GA-NumGenerations";
    JPanel linePar8;
    JLabel lblPar8;
    JTextField txtPar8;

    String keyPar9 = "GA-NumConvGen";
    JPanel linePar9;
    JLabel lblPar9;
    JTextField txtPar9;

    String keyPar10 = "GA-MaxTriesPerPopulation";
    JPanel linePar10;
    JLabel lblPar10;
    JTextField txtPar10;

    String keyPar11 = "GA-GrowthProbScheme";
    JPanel linePar11;
    JLabel lblPar11;
    JComboBox<String> cmbPar11;

    String keyPar12 = "GA-GrowthMultiplier";
    JPanel linePar12;
    JLabel lblPar12;
    JSpinner spnPar12;

    String keyPar13 = "GA-GrowthSigmaSteepness";
    JPanel linePar13;
    JLabel lblPar13;
    JSpinner spnPar13;

    String keyPar14 = "GA-GrowthSigmaMiddle";
    JPanel linePar14;
    JLabel lblPar14;
    JSpinner spnPar14;
    
    DefaultXYDataset crowdProbData;
    JFreeChart graphCrowdProbJFChart;
    JPanel graphCrowdProbJFChartChartPanel;
    JPanel graphCrowdProbJFChartCtrlPanel;
    JPanel graphCrowdProbSpinnerPane;
    Double minLevelCrowdProbPlot = 0.0;
    Double maxLevelCrowdProbPlot = 6.0;
    
    JPanel localBlockCrowd3;
    JPanel localBlockCrowd4;
    JPanel localBlockCrowdGraph;

    String keyParCrowd1 = "GA-CrowdProbScheme";
    JPanel lineParCrowd1;
    JLabel lblParCrowd1;
    JComboBox<String> cmbParCrowd1;

    String keyParCrowd2 = "GA-CrowdMultiplier";
    JPanel lineParCrowd2;
    JLabel lblParCrowd2;
    JSpinner spnParCrowd2;

    String keyParCrowd3 = "GA-CrowdSigmaSteepness";
    JPanel lineParCrowd3;
    JLabel lblParCrowd3;
    JSpinner spnParCrowd3;

    String keyParCrowd4 = "GA-CrowdSigmaMiddle";
    JPanel lineParCrowd4;
    JLabel lblParCrowd4;
    JSpinner spnParCrowd4;

    String keyPar15 = "GA-XOverSelectionMode";
    JPanel linePar15;
    JLabel lblPar15;
    JComboBox<String> cmbPar15;

    String keyPar16 = "GA-CrossoverProbability";
    JPanel linePar16;
    JLabel lblPar16;
    JTextField txtPar16;

    String keyPar17 = "GA-MutationProbability";
    JPanel linePar17;
    JLabel lblPar17;
    JTextField txtPar17;

    String keyPar18 = "GA-SymmetryProbability";
    JPanel linePar18;
    JLabel lblPar18;
    JTextField txtPar18;

    String keyPar19 = "GA-ReplacementStrategy";
    JPanel linePar19;
    JLabel lblPar19;
    JComboBox<String> cmbPar19;

    String keyPar20 = "GA-InitPoplnFile";
    JPanel linePar20;
    JLabel lblPar20;
    JTextField txtPar20;
    JButton btnPar20;

    String keyPar21 = "GA-UIDFileIn";
    JPanel linePar21;
    JLabel lblPar21;
    JTextField txtPar21;
    JButton btnPar21;

    String keyPar22 = "GA-UIDFileOut";
    JPanel linePar22;
    JLabel lblPar22;
    JTextField txtPar22;
    JButton btnPar22;

    String keyPar24 = "GA-NumParallelTasks";
    JPanel linePar24;
    JLabel lblPar24;
    JTextField txtPar24;

    String keyPar25 = "GA-Parallelization";
    JPanel linePar25;
    JLabel lblPar25;
    JComboBox<String> cmbPar25;

    //HEREGOFIELDS  this is only to facilitate automated insertion of code

    
    String NL = System.getProperty("line.separator");
    
    public GAParametersForm(Dimension d)
    {
    	mapKeyFieldToValueField = new HashMap<String,Object>();
    	
        this.setLayout(new BorderLayout()); //Needed to allow dynamic resizing!

        block = new JPanel();
        JScrollPane scrollablePane = new JScrollPane(block);
        block.setLayout(new BoxLayout(block, SwingConstants.VERTICAL));
        
        localBlock1 = new JPanel();
        localBlock1.setVisible(false);
        localBlock1.setLayout(new BoxLayout(localBlock1, SwingConstants.VERTICAL));
        
        localBlock2 = new JPanel();
        localBlock2.setVisible(true);
        localBlock2.setLayout(new BoxLayout(localBlock2, SwingConstants.VERTICAL));
        
        localBlock3 = new JPanel();
        localBlock3.setVisible(true);
        localBlock3.setLayout(new BoxLayout(localBlock3, SwingConstants.VERTICAL));
        
        localBlock4 = new JPanel();
        localBlock4.setVisible(false);
        localBlock4.setLayout(new BoxLayout(localBlock4, SwingConstants.VERTICAL));
        
        localBlockGraph = new JPanel();
        localBlockGraph.setVisible(true);
        
        localBlockCrowd3 = new JPanel();
        localBlockCrowd3.setVisible(false);
        localBlockCrowd3.setLayout(new BoxLayout(localBlockCrowd3, SwingConstants.VERTICAL));
        
        localBlockCrowd4 = new JPanel();
        localBlockCrowd4.setVisible(false);
        localBlockCrowd4.setLayout(new BoxLayout(localBlockCrowd4, SwingConstants.VERTICAL));
        
        localBlockCrowdGraph = new JPanel();
        localBlockCrowdGraph.setVisible(false);
        
        advOptsBlock = new JPanel();
        advOptsBlock.setVisible(false);
        advOptsBlock.setLayout(new BoxLayout(advOptsBlock, SwingConstants.VERTICAL));
        
        String toolTipSrcOrNew = "Tick here to use settings from file.";
        lineSrcOrNew = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rdbSrcOrNew = new JRadioButton("Use parameters from existing file");
        rdbSrcOrNew.setToolTipText(toolTipSrcOrNew);
        rdbSrcOrNew.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		if (rdbSrcOrNew.isSelected())
        		{
    				localBlock1.setVisible(true);
        			localBlock2.setVisible(false);
        		}
        		else
        		{
        			localBlock1.setVisible(false);
        			localBlock2.setVisible(true);
        		}
        	}
        });
        lineSrcOrNew.add(rdbSrcOrNew);
        block.add(lineSrcOrNew);
        block.add(localBlock1);
        block.add(localBlock2);
        
        String toolTipGASource = "<html>Pathname of a DENOPTIM's parameter file with GA settings.</html>";
        lineGASource = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblGASource = new JLabel("Use parameters from file:", SwingConstants.LEFT);
        lblGASource.setToolTipText(toolTipGASource);
        txtGASource = new JTextField();
        txtGASource.setToolTipText(toolTipGASource);
        txtGASource.setPreferredSize(fileFieldSize);
        btnGASource = new JButton("Browse");
        btnGASource.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
                GUIFileOpener.pickFileForTxtField(txtGASource,
                		btnGASource);
           }
        });
        btnLoadGASource = new JButton("Load...");
        txtGASource.setToolTipText("<html>Specify the file containing the "
        		+ "parameters to be loaded in this form.</html>");
        btnLoadGASource.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
	        	try 
	        	{
					importParametersFromDenoptimParamsFile(txtGASource.getText());
				} 
	        	catch (Exception e1) 
	        	{
	        		if (e1.getMessage().equals("") || e1.getMessage() == null)
	        		{
	        			e1.printStackTrace();
						JOptionPane.showMessageDialog(null,
								"<html>Exception occurred while importing parameters.<br>Please, report this to the DENOPTIM team.</html>",
				                "Error",
				                JOptionPane.ERROR_MESSAGE,
				                UIManager.getIcon("OptionPane.errorIcon"));
	        		}
	        		else
	        		{
						JOptionPane.showMessageDialog(null,
								e1.getMessage(),
				                "Error",
				                JOptionPane.ERROR_MESSAGE,
				                UIManager.getIcon("OptionPane.errorIcon"));
	        		}
					return;
				}
            }
        });
        lineGASource.add(lblGASource);
        lineGASource.add(txtGASource);
        lineGASource.add(btnGASource);
        lineGASource.add(btnLoadGASource);
        localBlock1.add(lineGASource);
        
        //HEREGOESIMPLEMENTATION this is only to facilitate automated insertion of code

        String toolTipPar6 = "Specifies the number of individuals in the initial population.";
        linePar6 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar6 = new JLabel("Size of population:", SwingConstants.LEFT);
        lblPar6.setPreferredSize(fileLabelSize);
        lblPar6.setToolTipText(toolTipPar6);
        txtPar6 = new JTextField();
        txtPar6.setToolTipText(toolTipPar6);
        txtPar6.setPreferredSize(strFieldSize);
        txtPar6.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar6.toUpperCase(),txtPar6);
        linePar6.add(lblPar6);
        linePar6.add(txtPar6);
        localBlock2.add(linePar6);
        
        String toolTipPar7 = "Specifies the number of children to be generated for each generation.";
        linePar7 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar7 = new JLabel("No. offsprings per generation:", SwingConstants.LEFT);
        lblPar7.setPreferredSize(fileLabelSize);
        lblPar7.setToolTipText(toolTipPar7);
        txtPar7 = new JTextField();
        txtPar7.setToolTipText(toolTipPar7);
        txtPar7.setPreferredSize(strFieldSize);
        txtPar7.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar7.toUpperCase(),txtPar7);
        linePar7.add(lblPar7);
        linePar7.add(txtPar7);
        localBlock2.add(linePar7);

        String toolTipPar8 = "Specifies the maximum number of generation.";
        linePar8 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar8 = new JLabel("Max. number of generations:", SwingConstants.LEFT);
        lblPar8.setPreferredSize(fileLabelSize);
        lblPar8.setToolTipText(toolTipPar8);
        txtPar8 = new JTextField();
        txtPar8.setToolTipText(toolTipPar8);
        txtPar8.setPreferredSize(strFieldSize);
        txtPar8.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar8.toUpperCase(),txtPar8);
        linePar8.add(lblPar8);
        linePar8.add(txtPar8);
        localBlock2.add(linePar8);

        String toolTipPar9 = "Specifies the convergence criterion as number of subsequent identical generations.";
        linePar9 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar9 = new JLabel("Max. stagnating generations:", SwingConstants.LEFT);
        lblPar9.setPreferredSize(fileLabelSize);
        lblPar9.setToolTipText(toolTipPar9);
        txtPar9 = new JTextField();
        txtPar9.setToolTipText(toolTipPar9);
        txtPar9.setPreferredSize(strFieldSize);
        txtPar9.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar9.toUpperCase(),txtPar9);
        linePar9.add(lblPar9);
        linePar9.add(txtPar9);
        localBlock2.add(linePar9);
        
        localBlock2.add(new JSeparator());
        
        String toolTipPar12 = "<html>Specifies the value of the factor used in"
        		+ " growth probability schemes <code>EXP_DIFF</code> and "
        		+ "<code>TANH</code></html>";
        linePar12 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar12 = new JLabel("<html>Graph extension - parameter "
        		+ "<code>&lambda;</code><html>", SwingConstants.LEFT);
        lblPar12.setPreferredSize(fileLabelSize);
        lblPar12.setToolTipText(toolTipPar12);
        spnPar12 = new JSpinner(new SpinnerNumberModel(1.0, 0.0, null, 0.1));
        spnPar12.setToolTipText(toolTipPar12);
        spnPar12.setPreferredSize(strFieldSize);
        mapKeyFieldToValueField.put(keyPar12.toUpperCase(),spnPar12);
        linePar12.add(lblPar12);
        linePar12.add(spnPar12);
        localBlock3.add(linePar12);
        
        String toolTipPar13 = "<html>Specifies the value of parameter "
        		+ "&sigma;<sub>1</sub> used for growth probability scheme"
        		+ " <code>SIGMA</code>.<br>It corresponds to the steepness of"
        		+ " the function where <i>P(level) = 50%</i></html>";
        linePar13 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar13 = new JLabel("<html>Graph extension - parameter "
        		+ "<code>&sigma;</code><sub>1</sub>:</html>", SwingConstants.LEFT);
        lblPar13.setPreferredSize(fileLabelSize);
        lblPar13.setToolTipText(toolTipPar13);
        spnPar13 = new JSpinner(new SpinnerNumberModel(1.0, null, null, 0.1));
        spnPar13.setToolTipText(toolTipPar13);
        spnPar13.setPreferredSize(strFieldSize);
        mapKeyFieldToValueField.put(keyPar13.toUpperCase(),spnPar13);
        linePar13.add(lblPar13);
        linePar13.add(spnPar13);
        localBlock4.add(linePar13);
        
        String toolTipPar14 = "<html>Specifies the value of parameter "
        		+ "&sigma;<sub>2</sub> used in growth probability scheme "
        		+ "<code>SIGMA</code>.<br>It corresponds to the level "
        		+ "where <i>P(level) = 50%</i></html>";
        linePar14 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar14 = new JLabel("<html>Graph extension - parameter <code>&sigma;</code><sub>2</sub>:<html>", SwingConstants.LEFT);
        lblPar14.setPreferredSize(fileLabelSize);
        lblPar14.setToolTipText(toolTipPar14);
        spnPar14 = new JSpinner(new SpinnerNumberModel(3.5, null, null, 0.1));
        spnPar14.setToolTipText(toolTipPar14);
        spnPar14.setPreferredSize(strFieldSize);
        mapKeyFieldToValueField.put(keyPar14.toUpperCase(),spnPar14);
        linePar14.add(lblPar14);
        linePar14.add(spnPar14);
        localBlock4.add(linePar14);
        
        String toolTipPar11 = "Specifies the growth probability scheme";
        linePar11 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar11 = new JLabel("Graph extension probability function:", SwingConstants.LEFT);
        lblPar11.setPreferredSize(fileLabelSize);
        lblPar11.setToolTipText(toolTipPar11);
        cmbPar11 = new JComboBox<String>(new String[] {"EXP_DIFF", "TANH", "SIGMA", "UNRESTRICTED"});
        cmbPar11.setSelectedIndex(0);
        cmbPar11.setToolTipText(toolTipPar11);
        cmbPar11.addActionListener(cmbFieldChange);
        mapKeyFieldToValueField.put(keyPar11.toUpperCase(),cmbPar11);
        
        // NB: we need to create the graph before setting the action listeners
        //     that will eventually edit the data plotted.
        createSubsProbGraph();
        
        graphSubProbJFChartCtrlPanel = new JPanel(new BorderLayout());
        graphSubProbJFChartCtrlPanel.setMaximumSize(new Dimension(100,100));
        graphSpinnerPane = new JPanel(new GridLayout(0,2));
        final JSpinner spnMaxLev = new JSpinner(new SpinnerNumberModel(
        		maxLevelProbPlot.intValue(), 1, null, 1));
        spnMaxLev.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent event)
            {
                int maxLev = ((Integer) spnMaxLev.getValue()).intValue();                
                ((XYPlot) graphSubProbJFChart.getPlot())
                	.getDomainAxis().setRange(minLevelProbPlot, maxLev);
            }
        }); 
        graphSpinnerPane.add(new JLabel("X-axis max: "));
        graphSpinnerPane.add(spnMaxLev);
        graphSubProbJFChartCtrlPanel.add(graphSpinnerPane, BorderLayout.NORTH);
        
        GroupLayout grpLyoSubPrb = new GroupLayout(localBlockGraph);
        localBlockGraph.setLayout(grpLyoSubPrb);
        grpLyoSubPrb.setAutoCreateGaps(true);
        grpLyoSubPrb.setAutoCreateContainerGaps(true);
		grpLyoSubPrb.setHorizontalGroup(grpLyoSubPrb.createSequentialGroup()
			.addComponent(graphSubProbJFChartChartPanel)
			.addComponent(graphSubProbJFChartCtrlPanel));
		grpLyoSubPrb.setVerticalGroup(grpLyoSubPrb.createParallelGroup(
				GroupLayout.Alignment.CENTER)
			.addComponent(graphSubProbJFChartChartPanel)
			.addComponent(graphSubProbJFChartCtrlPanel));
		
		// NB: The listeners must be defined here because we first have to 
		//     built all the pieces (plot + parameters) and then define how
		//     things change upon change of the parameters/controllers.

		spnPar12.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent event)
            {
            	updateSubsProbDataset();
            }
        });
		spnPar13.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent event)
            {
            	updateSubsProbDataset();
            }
        });
		spnPar14.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent event)
            {
            	updateSubsProbDataset();
            }
        });
		
        cmbPar11.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		switch (cmbPar11.getSelectedItem().toString())
        		{
        			case "EXP_DIFF":
        				updateSubsProbDataset();
        				localBlock3.setVisible(true);
        				localBlock4.setVisible(false);
        				localBlockGraph.setVisible(true);
            			break;
            			
        			case "TANH":
        				updateSubsProbDataset();
        				localBlock3.setVisible(true);
            			localBlock4.setVisible(false);  
            			localBlockGraph.setVisible(true);
            			break;
            			
        			case "SIGMA":
        				updateSubsProbDataset();
        				localBlock3.setVisible(false);
            			localBlock4.setVisible(true);
            			localBlockGraph.setVisible(true);
            			break;
            			
        			default:
        				localBlock3.setVisible(false);
            			localBlock4.setVisible(false); 
            			localBlockGraph.setVisible(false);
            			break;
        		}
	        }
	    });
        linePar11.add(lblPar11);
        linePar11.add(cmbPar11);
        localBlock2.add(linePar11);
        localBlock2.add(localBlock3);
        localBlock2.add(localBlock4);
        localBlock2.add(localBlockGraph);
        
        localBlock2.add(new JSeparator());
        
        String toolTipParCrowd2 = "<html>Specifies the value of the factor used in"
                + " crowding probability schemes <code>EXP_DIFF</code> and "
                + "<code>TANH</code></html>";
        lineParCrowd2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblParCrowd2 = new JLabel("<html>Crowding probability - parameter "
                + "<code>&lambda;</code><html>", SwingConstants.LEFT);
        lblParCrowd2.setPreferredSize(fileLabelSize);
        lblParCrowd2.setToolTipText(toolTipParCrowd2);
        spnParCrowd2 = new JSpinner(new SpinnerNumberModel(1.0, 0.0, null, 0.1));
        spnParCrowd2.setToolTipText(toolTipParCrowd2);
        spnParCrowd2.setPreferredSize(strFieldSize);
        mapKeyFieldToValueField.put(keyParCrowd2.toUpperCase(),spnParCrowd2);
        lineParCrowd2.add(lblParCrowd2);
        lineParCrowd2.add(spnParCrowd2);
        localBlockCrowd3.add(lineParCrowd2);
        
        String toolTipParCrowd3 = "<html>Specifies the value of parameter "
                + "&sigma;<sub>1</sub> used for crowding probability scheme"
                + " <code>SIGMA</code>.<br>It corresponds to the steepness of"
                + " the function where <i>P(level) = 50%</i></html>";
        lineParCrowd3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblParCrowd3 = new JLabel("<html>Crowding probability - parameter "
                + "<code>&sigma;</code><sub>1</sub>:</html>", SwingConstants.LEFT);
        lblParCrowd3.setPreferredSize(fileLabelSize);
        lblParCrowd3.setToolTipText(toolTipParCrowd3);
        spnParCrowd3 = new JSpinner(new SpinnerNumberModel(1.0, null, null, 0.1));
        spnParCrowd3.setToolTipText(toolTipParCrowd3);
        spnParCrowd3.setPreferredSize(strFieldSize);
        mapKeyFieldToValueField.put(keyParCrowd3.toUpperCase(),spnParCrowd3);
        lineParCrowd3.add(lblParCrowd3);
        lineParCrowd3.add(spnParCrowd3);
        localBlockCrowd4.add(lineParCrowd3);
        
        String toolTipParCrowd4 = "<html>Specifies the value of parameter "
                + "&sigma;<sub>2</sub> used in crowding probability scheme "
                + "<code>SIGMA</code>.<br>It corresponds to the level "
                + "where <i>P(level) = 50%</i></html>";
        lineParCrowd4 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblParCrowd4 = new JLabel("<html>Crowding probability - parameter "
                + "<code>&sigma;</code><sub>2</sub>:<html>", SwingConstants.LEFT);
        lblParCrowd4.setPreferredSize(fileLabelSize);
        lblParCrowd4.setToolTipText(toolTipParCrowd4);
        spnParCrowd4 = new JSpinner(new SpinnerNumberModel(3.5, null, null, 0.1));
        spnParCrowd4.setToolTipText(toolTipParCrowd4);
        spnParCrowd4.setPreferredSize(strFieldSize);
        mapKeyFieldToValueField.put(keyParCrowd4.toUpperCase(),spnParCrowd4);
        lineParCrowd4.add(lblParCrowd4);
        lineParCrowd4.add(spnParCrowd4);
        localBlockCrowd4.add(lineParCrowd4);
        
        String toolTipParCrowd1 = "Specifies the crowding probability scheme";
        lineParCrowd1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblParCrowd1 = new JLabel("Crowding probability function:", 
                SwingConstants.LEFT);
        lblParCrowd1.setPreferredSize(fileLabelSize);
        lblParCrowd1.setToolTipText(toolTipParCrowd1);
        cmbParCrowd1 = new JComboBox<String>(new String[] {
                "EXP_DIFF", "TANH", "SIGMA", "UNRESTRICTED"});
        cmbParCrowd1.setSelectedIndex(3);
        cmbParCrowd1.setToolTipText(toolTipParCrowd1);
        cmbParCrowd1.addActionListener(cmbFieldChange);
        mapKeyFieldToValueField.put(keyParCrowd1.toUpperCase(),cmbParCrowd1);
        
        // NB: we need to create the graph before setting the action listeners
        //     that will eventually edit the data plotted.
        createCrowdProbGraph();
        
        graphCrowdProbJFChartCtrlPanel = new JPanel(new BorderLayout());
        graphCrowdProbJFChartCtrlPanel.setMaximumSize(new Dimension(100,100));
        graphCrowdProbSpinnerPane = new JPanel(new GridLayout(0,2));
        final JSpinner spnMaxLevCrowd = new JSpinner(new SpinnerNumberModel(
                maxLevelCrowdProbPlot.intValue(), 1, null, 1));
        spnMaxLevCrowd.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent event)
            {
                int maxLev = ((Integer) spnMaxLevCrowd.getValue()).intValue();                
                ((XYPlot) graphCrowdProbJFChart.getPlot())
                    .getDomainAxis().setRange(minLevelCrowdProbPlot, maxLev);
            }
        }); 
        graphCrowdProbSpinnerPane.add(new JLabel("X-axis max: "));
        graphCrowdProbSpinnerPane.add(spnMaxLevCrowd);
        graphCrowdProbJFChartCtrlPanel.add(graphCrowdProbSpinnerPane, BorderLayout.NORTH);
        
        GroupLayout grpLyoCrowdProb = new GroupLayout(localBlockCrowdGraph);
        localBlockCrowdGraph.setLayout(grpLyoCrowdProb);
        grpLyoCrowdProb.setAutoCreateGaps(true);
        grpLyoCrowdProb.setAutoCreateContainerGaps(true);
        grpLyoCrowdProb.setHorizontalGroup(grpLyoCrowdProb.createSequentialGroup()
            .addComponent(graphCrowdProbJFChartChartPanel)
            .addComponent(graphCrowdProbJFChartCtrlPanel));
        grpLyoCrowdProb.setVerticalGroup(grpLyoCrowdProb.createParallelGroup(
                GroupLayout.Alignment.CENTER)
            .addComponent(graphCrowdProbJFChartChartPanel)
            .addComponent(graphCrowdProbJFChartCtrlPanel));
        
        // NB: The listeners must be defined here because we first have to 
        //     built all the pieces (plot + parameters) and then define how
        //     things change upon change of the parameters/controllers.

        spnParCrowd2.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent event)
            {
                updateCrowdProbDataset();
            }
        });
        spnParCrowd3.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent event)
            {
                updateCrowdProbDataset();
            }
        });
        spnParCrowd4.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent event)
            {
                updateCrowdProbDataset();
            }
        });
        
        cmbParCrowd1.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                String scheme = cmbParCrowd1.getSelectedItem().toString();
                switch (scheme)
                {
                    case "EXP_DIFF":
                        updateCrowdProbDataset();
                        localBlockCrowd3.setVisible(true);
                        localBlockCrowd4.setVisible(false);
                        localBlockCrowdGraph.setVisible(true);
                        break;
                        
                    case "TANH":
                        updateCrowdProbDataset();
                        localBlockCrowd3.setVisible(true);
                        localBlockCrowd4.setVisible(false);  
                        localBlockCrowdGraph.setVisible(true);
                        break;
                        
                    case "SIGMA":
                        updateCrowdProbDataset();
                        localBlockCrowd3.setVisible(false);
                        localBlockCrowd4.setVisible(true);
                        localBlockCrowdGraph.setVisible(true);
                        break;
                        
                    case "UNRESTRICTED":
                        updateCrowdProbDataset();
                        localBlockCrowd3.setVisible(false);
                        localBlockCrowd4.setVisible(false);
                        localBlockCrowdGraph.setVisible(true);
                        break;
                        
                    default:
                        localBlockCrowd3.setVisible(false);
                        localBlockCrowd4.setVisible(false); 
                        localBlockCrowdGraph.setVisible(false);
                        break;
                }
            }
        });
        lineParCrowd1.add(lblParCrowd1);
        lineParCrowd1.add(cmbParCrowd1);
        localBlock2.add(lineParCrowd1);
        localBlock2.add(localBlockCrowd3);
        localBlock2.add(localBlockCrowd4);
        localBlock2.add(localBlockCrowdGraph);
        
        localBlock2.add(new JSeparator());

        String toolTipPar15 = "<html>Specifies the strategy for selecting crossover partners.<ul>" 
        		+ "<li><code>RANDOM</code>: unbiased selection.</li>" 
        		+ "<li><code>TS</code>: Tournament.</li>" 
        		+ "<li><code>RW</code>: Roulette Wheel.</li>" 
        		+ "<li><code>SUS</code>: Stochastic Universal Sampling.</li>"
        		+ "</ul></html>";
        linePar15 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar15 = new JLabel("Parent selection algorithm:", SwingConstants.LEFT);
        lblPar15.setPreferredSize(fileLabelSize);
        lblPar15.setToolTipText(toolTipPar15);
        cmbPar15 = new JComboBox<String>(new String[] {"RANDOM", "TS", "RW", "SUS"});
        cmbPar15.setToolTipText(toolTipPar15);
        cmbPar15.addActionListener(cmbFieldChange);
        mapKeyFieldToValueField.put(keyPar15.toUpperCase(),cmbPar15);
        linePar15.add(lblPar15);
        linePar15.add(cmbPar15);
        localBlock2.add(linePar15);

        String toolTipPar16 = "Specifies the probability (0.0-1.0) at which crossover is performed.";
        linePar16 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar16 = new JLabel("Crossover probability:", SwingConstants.LEFT);
        lblPar16.setPreferredSize(fileLabelSize);
        lblPar16.setToolTipText(toolTipPar16);
        txtPar16 = new JTextField();
        txtPar16.setToolTipText(toolTipPar16);
        txtPar16.setPreferredSize(strFieldSize);
        txtPar16.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar16.toUpperCase(),txtPar16);
        linePar16.add(lblPar16);
        linePar16.add(txtPar16);
        localBlock2.add(linePar16);

        String toolTipPar17 = "Specifies the probability (0.0-1.0) at which mutation is performed.";
        linePar17 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar17 = new JLabel("Mutation probability:", SwingConstants.LEFT);
        lblPar17.setPreferredSize(fileLabelSize);
        lblPar17.setToolTipText(toolTipPar17);
        txtPar17 = new JTextField();
        txtPar17.setToolTipText(toolTipPar17);
        txtPar17.setPreferredSize(strFieldSize);
        txtPar17.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar17.toUpperCase(),txtPar17);
        linePar17.add(lblPar17);
        linePar17.add(txtPar17);
        localBlock2.add(linePar17);

        String toolTipPar18 = "Specifies the probability (0.0-1.0) of symmetric operations.";
        linePar18 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar18 = new JLabel("Symmetric operation probability:", SwingConstants.LEFT);
        lblPar18.setPreferredSize(fileLabelSize);
        lblPar18.setToolTipText(toolTipPar18);
        txtPar18 = new JTextField();
        txtPar18.setToolTipText(toolTipPar18);
        txtPar18.setPreferredSize(strFieldSize);
        txtPar18.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar18.toUpperCase(),txtPar18);
        linePar18.add(lblPar18);
        linePar18.add(txtPar18);
        localBlock2.add(linePar18);

        String toolTipPar19 = "Specifies the population members replacement strategy.";
        linePar19 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar19 = new JLabel("Population update mode:", SwingConstants.LEFT);
        lblPar19.setPreferredSize(fileLabelSize);
        lblPar19.setToolTipText(toolTipPar19);
        cmbPar19 = new JComboBox<String>(new String[] {"ELITIST", "NONE"});
        cmbPar19.setToolTipText(toolTipPar19);
        cmbPar19.addActionListener(cmbFieldChange);
        mapKeyFieldToValueField.put(keyPar19.toUpperCase(),cmbPar19);
        linePar19.add(lblPar19);
        linePar19.add(cmbPar19);
        localBlock2.add(linePar19);

        String toolTipPar24 = "Specifies the maximum number of parallel candidate evaluation tasks.";
        linePar24 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar24 = new JLabel("Number of parallel evaluation tasks:", SwingConstants.LEFT);
        lblPar24.setPreferredSize(fileLabelSize);
        lblPar24.setToolTipText(toolTipPar24);
        txtPar24 = new JTextField();
        txtPar24.setToolTipText(toolTipPar24);
        txtPar24.setPreferredSize(strFieldSize);
        txtPar24.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar24.toUpperCase(),txtPar24);
        linePar24.add(lblPar24);
        linePar24.add(txtPar24);
        localBlock2.add(linePar24);

        String toolTipPar25 = "<html>Specifies the parallelization scheme:<br><ul><li><code>synchronous</code>, i.e., parallel tasks are submitted in batches, thus no new task is submitted until the last of the previous tasks is completed.</li><li><code>asynchronous</code>, i.e., a new parallel tasks is submitted as soon as any of the previous task is completed.</li></ul></html>";
        linePar25 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar25 = new JLabel("Task parallelization scheme:", SwingConstants.LEFT);
        lblPar25.setPreferredSize(fileLabelSize);
        lblPar25.setToolTipText(toolTipPar25);
        cmbPar25 = new JComboBox<String>(new String[] {"Synchronous", "Asynchronous"});
        cmbPar25.setToolTipText(toolTipPar25);
        cmbPar25.addActionListener(cmbFieldChange);
        mapKeyFieldToValueField.put(keyPar25.toUpperCase(),cmbPar25);
        linePar25.add(lblPar25);
        linePar25.add(cmbPar25);
        localBlock2.add(linePar25);

        // From here it's all about advanced options

        String toolTipPar3 = "Specifies the seed number used by the random number generator";
        linePar3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar3 = new JLabel("Random Seed:", SwingConstants.LEFT);
        lblPar3.setPreferredSize(fileLabelSize);
        lblPar3.setToolTipText(toolTipPar3);
        txtPar3 = new JTextField();
        txtPar3.setToolTipText(toolTipPar3);
        txtPar3.setPreferredSize(new Dimension(150,preferredHeight));
        txtPar3.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar3.toUpperCase(),txtPar3);
        linePar3.add(lblPar3);
        linePar3.add(txtPar3);
        advOptsBlock.add(linePar3);

        String toolTipPar1 = "Specifies the number of figures used to report the fitness";
        linePar1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar1 = new JLabel("Precision of fitness value:", SwingConstants.LEFT);
        lblPar1.setPreferredSize(fileLabelSize);
        lblPar1.setToolTipText(toolTipPar1);
        txtPar1 = new JTextField();
        txtPar1.setToolTipText(toolTipPar1);
        txtPar1.setPreferredSize(strFieldSize);
        txtPar1.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar1.toUpperCase(),txtPar1);
        linePar1.add(lblPar1);
        linePar1.add(txtPar1);
        advOptsBlock.add(linePar1);

        String toolTipPar2 = "Specifies the amount of log produced.";
        linePar2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar2 = new JLabel("Verbosity of GA modules:", SwingConstants.LEFT);
        lblPar2.setPreferredSize(fileLabelSize);
        lblPar2.setToolTipText(toolTipPar2);
        cmbPar2 = new JComboBox<String>(new String[] {"0", "1", "2", "3"});
        cmbPar2.setToolTipText(toolTipPar2);
        cmbPar2.addActionListener(cmbFieldChange);
        mapKeyFieldToValueField.put(keyPar2.toUpperCase(),cmbPar2);
        linePar2.add(lblPar2);
        linePar2.add(cmbPar2);
        advOptsBlock.add(linePar2);

        String toolTipPar5 = "Specifies whether the candidates should be reported in descending or ascending order of fitness.";
        linePar5 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rdbPar5 = new JRadioButton("Sort by descending fitness");
        rdbPar5.setToolTipText(toolTipPar5);
        rdbPar5.addChangeListener(rdbFieldChange);
        mapKeyFieldToValueField.put(keyPar5.toUpperCase(),rdbPar5);
        linePar5.add(rdbPar5);
        advOptsBlock.add(linePar5);

        String toolTipPar10 = "<html>Controls the maximum number of attempts to build a new graph.<br> The maximum number of attempts to build a new graph is given by the size of the population, times this factor.</html>";
        linePar10 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar10 = new JLabel("Max tries to get new graph:", SwingConstants.LEFT);
        lblPar10.setPreferredSize(fileLabelSize);
        lblPar10.setToolTipText(toolTipPar10);
        txtPar10 = new JTextField();
        txtPar10.setToolTipText(toolTipPar10);
        txtPar10.setPreferredSize(strFieldSize);
        txtPar10.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar10.toUpperCase(),txtPar10);
        linePar10.add(lblPar10);
        linePar10.add(txtPar10);
        advOptsBlock.add(linePar10);

        String toolTipPar20 = "<html>Specifies the pathname of a file containing previously evaluated individuals to be added to the initial population.<br> The file can be an SDF file or a text file where each line containing the pathname to a single-molecule SDF file.</html>";
        linePar20 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar20 = new JLabel("Initial population file (SDF):", SwingConstants.LEFT);
        lblPar20.setPreferredSize(fileLabelSize);
        lblPar20.setToolTipText(toolTipPar20);
        txtPar20 = new JTextField();
        txtPar20.setToolTipText(toolTipPar20);
        txtPar20.setPreferredSize(fileFieldSize);
        txtPar20.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar20.toUpperCase(),txtPar20);
        btnPar20 = new JButton("Browse");
        btnPar20.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
                GUIFileOpener.pickFileForTxtField(txtPar20,btnPar20);
           }
        });
        linePar20.add(lblPar20);
        linePar20.add(txtPar20);
        linePar20.add(btnPar20);
        advOptsBlock.add(linePar20);

        String toolTipPar21 = "<html>Specifies the pathname of a text file collecting unique individual identification (UID) of previously known individuals.<br>The file must have one UID per line.</html>";
        linePar21 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar21 = new JLabel("File with known UIDs:", SwingConstants.LEFT);
        lblPar21.setPreferredSize(fileLabelSize);
        lblPar21.setToolTipText(toolTipPar21);
        txtPar21 = new JTextField();
        txtPar21.setToolTipText(toolTipPar21);
        txtPar21.setPreferredSize(fileFieldSize);
        txtPar21.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar21.toUpperCase(),txtPar21);
        btnPar21 = new JButton("Browse");
        btnPar21.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
                GUIFileOpener.pickFileForTxtField(txtPar21,btnPar21);
           }
        });
        linePar21.add(lblPar21);
        linePar21.add(txtPar21);
        linePar21.add(btnPar21);
        advOptsBlock.add(linePar21);

        String toolTipPar22 = "Specifies the pathname of the file that will collect unique individual identification (UID) strings encountered during an evolutionary experiment.";
        linePar22 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar22 = new JLabel("File for new UIDs:", SwingConstants.LEFT);
        lblPar22.setPreferredSize(fileLabelSize);
        lblPar22.setToolTipText(toolTipPar22);
        txtPar22 = new JTextField();
        txtPar22.setToolTipText(toolTipPar22);
        txtPar22.setPreferredSize(fileFieldSize);
        txtPar22.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar22.toUpperCase(),txtPar22);
        btnPar22 = new JButton("Browse");
        btnPar22.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
                GUIFileOpener.pickFileForTxtField(txtPar22,btnPar22);
           }
        });
        linePar22.add(lblPar22);
        linePar22.add(txtPar22);
        linePar22.add(btnPar22);
        advOptsBlock.add(linePar22);

        //HEREGOESADVIMPLEMENTATION this is only to facilitate automated insertion of code       
        
        JButton advOptShow = new JButton("Advanced Settings");
        advOptShow.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		if (advOptsBlock.isVisible())
        		{
        			advOptsBlock.setVisible(false);
        			advOptShow.setText("Show Advanced Settings");        			
        		}
        		else
        		{
        			advOptsBlock.setVisible(true);
        			advOptShow.setText("Hide Advanced Settings");
    				scrollablePane.validate();
    				scrollablePane.repaint();
    				scrollablePane.getVerticalScrollBar().setValue(
    						scrollablePane.getVerticalScrollBar().getValue() + (int) preferredHeight*2/3);
        		}
	        }
	    });
        
        JPanel advOptsController = new JPanel();
        advOptsController.add(advOptShow);
        localBlock2.add(new JSeparator());
        localBlock2.add(advOptsController);
        localBlock2.add(advOptsBlock);      
        
        this.add(scrollablePane);
    }
    
//-----------------------------------------------------------------------------

	private void updateSubsProbDataset() 
	{
		createSubsProbDataset();
		((XYPlot) graphSubProbJFChart.getPlot()).setDataset(subsProbData);	
	}
		
//-----------------------------------------------------------------------------

	private void createSubsProbDataset() 
	{	
        subsProbData = new DefaultXYDataset();
        
        ArrayList<Double> y = new ArrayList<Double>();
		try
		{
			int scheme = GAParameters.convertGrowthProbabilityScheme(
					cmbPar11.getSelectedItem().toString());
			double l = (Double) spnPar12.getValue();
			double s1 = (Double) spnPar13.getValue();
			double s2 = (Double) spnPar14.getValue();
			
			for (int level=0; level<100; level++)
			{
				double prob = EAUtils.getGrowthProbabilityAtLevel(level, scheme, l, s1, s2);
				y.add(prob);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			JOptionPane.showMessageDialog(null,
					"<html>Exception occurred while reading growth proability "
					+ "scheme.<br>"
					+ "Please, report this to the DENOPTIM team.</html>",
	                "Error",
	                JOptionPane.ERROR_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
		}

        double[][] data = new double[2][y.size()];
        for (int level=0; level<100; level++)
        {
        	data[0][level] = new Double(level);
        	data[1][level] = y.get(level);
        }
        
        subsProbData.addSeries("Growth Probability", data);
	}
	
//-----------------------------------------------------------------------------

	private void createSubsProbGraph() 
	{
		createSubsProbDataset();
    	createSubsProbChart();
    	graphSubProbJFChartChartPanel = new ChartPanel(graphSubProbJFChart);
    	graphSubProbJFChartChartPanel.setMaximumSize(new Dimension(400,200));
	}
	
//-----------------------------------------------------------------------------
	
    private void createSubsProbChart()
    {
        graphSubProbJFChart = ChartFactory.createXYLineChart(
            null,                         // plot title
            "Level",                      // x axis label
            "Probability",                // y axis label
            subsProbData,                 // data
            PlotOrientation.VERTICAL,  
            false,                        // include legend
            false,                        // tooltips
            false                         // urls
        );

        XYPlot plot = (XYPlot) graphSubProbJFChart.getPlot();
        
        // axis ranges
        plot.getDomainAxis().setRange(minLevelProbPlot, maxLevelProbPlot);
        plot.getRangeAxis().setRange(0.0, 1.0);
        
        // axis ticks interval 
        //NB: this if commented out because it blocks automated selection of tick units
        // and therefore it allows ticks overlap when range is large
        //((NumberAxis) plot.getDomainAxis()).setTickUnit(new NumberTickUnit(1.0));

        // series line thickness
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, Color.RED);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        plot.setRenderer(renderer);
        
        // font of axis label
        Font font3 = new Font("Dialog", Font.PLAIN, 12); 
        plot.getDomainAxis().setLabelFont(font3);
        plot.getRangeAxis().setLabelFont(font3);
    }
	
    
//-----------------------------------------------------------------------------

    private void updateCrowdProbDataset() 
    {
        createCrowdProbDataset();
        ((XYPlot) graphCrowdProbJFChart.getPlot()).setDataset(crowdProbData);   
    }
        
//-----------------------------------------------------------------------------

    private void createCrowdProbDataset() 
    {   
        crowdProbData = new DefaultXYDataset();
        
        ArrayList<Double> y = new ArrayList<Double>();
        try
        {
            int scheme = GAParameters.convertGrowthProbabilityScheme(
                    cmbParCrowd1.getSelectedItem().toString());
            double l = (Double) spnParCrowd2.getValue();
            double s1 = (Double) spnParCrowd3.getValue();
            double s2 = (Double) spnParCrowd4.getValue();
            
            for (int crowdedness=0; crowdedness<100; crowdedness++)
            {
                double prob = EAUtils.getCrowdingProbabilityForCrowdedness(
                        crowdedness, scheme, l, s1, s2);
                y.add(prob);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "<html>Exception occurred while reading croding proability "
                    + "scheme.<br>"
                    + "Please, report this to the DENOPTIM team.</html>",
                    "Error",
                    JOptionPane.ERROR_MESSAGE,
                    UIManager.getIcon("OptionPane.errorIcon"));
        }

        double[][] data = new double[2][y.size()];
        for (int crowdedness=0; crowdedness<100; crowdedness++)
        {
            data[0][crowdedness] = new Double(crowdedness);
            data[1][crowdedness] = y.get(crowdedness);
        }
        
        crowdProbData.addSeries("Crowding Probability", data);
    }
    
//-----------------------------------------------------------------------------

    private void createCrowdProbGraph() 
    {
        createCrowdProbDataset();
        createCrowdProbChart();
        graphCrowdProbJFChartChartPanel = new ChartPanel(graphCrowdProbJFChart);
        graphCrowdProbJFChartChartPanel.setMaximumSize(new Dimension(400,200));
    }
    
//-----------------------------------------------------------------------------
    
    private void createCrowdProbChart()
    {
        graphCrowdProbJFChart = ChartFactory.createXYLineChart(
            null,                         // plot title
            "Crowdedness",                // x axis label
            "Probability",                // y axis label
            crowdProbData,                 // data
            PlotOrientation.VERTICAL,  
            false,                        // include legend
            false,                        // tooltips
            false                         // urls
        );

        XYPlot plot = (XYPlot) graphCrowdProbJFChart.getPlot();
        
        // axis ranges
        plot.getDomainAxis().setRange(minLevelCrowdProbPlot, maxLevelCrowdProbPlot);
        plot.getRangeAxis().setRange(0.0, 1.0);
        
        // axis ticks interval 
        //NB: this if commented out because it blocks automated selection of tick units
        // and therefore it allows ticks overlap when range is large
        //((NumberAxis) plot.getDomainAxis()).setTickUnit(new NumberTickUnit(1.0));

        // series line thickness
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, Color.BLUE);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        plot.setRenderer(renderer);
        
        // font of axis label
        Font font3 = new Font("Dialog", Font.PLAIN, 12); 
        plot.getDomainAxis().setLabelFont(font3);
        plot.getRangeAxis().setLabelFont(font3);
    }
    
//-----------------------------------------------------------------------------

	/**
     * Imports parameters from a properly formatted parameters file.
     * The file is a text file with lines containing KEY=VALUE pairs.
     * The visibility of the blocks of content is set accordingly to the 
     * parameters.
     * @param fileName the pathname of the file to read
     * @throws Exception
     */
    
    @Override
    public void importParametersFromDenoptimParamsFile(String fileName) throws Exception
    {
    	importParametersFromDenoptimParamsFile(fileName,"GA-");
    	
    	rdbSrcOrNew.setSelected(false);
    	localBlock1.setVisible(false);
		localBlock2.setVisible(true);		
		switch (cmbPar11.getSelectedItem().toString())
		{
			case "EXP_DIFF":
				localBlock3.setVisible(true);
    			localBlock4.setVisible(false);   
    			break;
    			
			case "TANH":
				localBlock3.setVisible(true);
    			localBlock4.setVisible(false);   
    			break;
    			
			case "SIGMA":
				localBlock3.setVisible(false);
    			localBlock4.setVisible(true);   
    			break;
    			
			default:
				localBlock3.setVisible(false);
    			localBlock4.setVisible(false);   
    			break;
		}
		advOptsBlock.setVisible(true);
    }

//-----------------------------------------------------------------------------
    
  	@SuppressWarnings("unchecked")
	@Override
	public void importSingleParameter(String key, String value) throws Exception 
  	{
  		Object valueField;
  		String valueFieldClass;
  		if (mapKeyFieldToValueField.containsKey(key.toUpperCase()))
  		{
  		    valueField = mapKeyFieldToValueField.get(key.toUpperCase());
  		    valueFieldClass = valueField.getClass().toString();
  		}
  		else
  		{
			JOptionPane.showMessageDialog(null,
					"<html>Parameter '" + key + "' is not recognized<br> and will be ignored.</html>",
	                "WARNING",
	                JOptionPane.WARNING_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;
  		}
    
 		switch (valueFieldClass)
 		{				
 			case "class javax.swing.JTextField":
 				((JTextField) valueField).setText(value);
 				break;
 				
 			case "class javax.swing.JRadioButton":
 				((JRadioButton) valueField).setSelected(true);
 				break;
 				
 			case "class javax.swing.JComboBox":
 				((JComboBox<String>) valueField).setSelectedItem(value);
 				break;
 				
 			case "class javax.swing.table.DefaultTableModel":

 				//WARNING: there might be cases where we do not take all the records

 				((DefaultTableModel) valueField).addRow(value.split(" "));
 				break;
 				
 			case "class javax.swing.JSpinner":

 				//WARNING: assuming all JSpinners work on doubles
 				
 				((JSpinner) valueField).setValue(Double.parseDouble(value));
 				break;
 				
 			default:
 				throw new Exception("<html>Unexpected type for parameter: "  
 						+ key + " (" + valueFieldClass 
 						+ ").<br>Please report this to"
 						+ "the DEMOPTIM team.</html>");
 		}
 		
	}
  	
//-----------------------------------------------------------------------------
    
    @Override
    public void putParametersToString(StringBuilder sb) throws Exception
    {
    	sb.append("# Genetic Algorithm - parameters").append(NL);
    	
        if (rdbSrcOrNew.isSelected())
        {
        	if (txtGASource.getText().equals("") || txtGASource.getText() == null)
        	{
        		throw new Exception("<html>No source specified for GA parameters.<br>Please, specify the file name.</html>");
        	}
        	importParametersFromDenoptimParamsFile(txtGASource.getText());
        }
        
        sb.append(getStringIfNotEmpty(keyPar3,txtPar3));
        sb.append(getStringIfNotEmpty(keyPar1,txtPar1));
        sb.append(keyPar2).append("=").append(cmbPar2.getSelectedItem()).append(NL);
        sb.append(getStringIfSelected(keyPar5,rdbPar5));
        sb.append(getStringIfNotEmpty(keyPar6,txtPar6));
        sb.append(getStringIfNotEmpty(keyPar7,txtPar7));
        sb.append(getStringIfNotEmpty(keyPar8,txtPar8));
        sb.append(getStringIfNotEmpty(keyPar9,txtPar9));
        sb.append(getStringIfNotEmpty(keyPar10,txtPar10));
        sb.append(keyPar11).append("=").append(cmbPar11.getSelectedItem()).append(NL);
        sb.append(getStringForKVLine(keyPar12,spnPar12));
        sb.append(getStringForKVLine(keyPar13,spnPar13));
        sb.append(getStringForKVLine(keyPar14,spnPar14));
        sb.append(keyParCrowd1).append("=").append(cmbParCrowd1.getSelectedItem()).append(NL);
        sb.append(getStringForKVLine(keyParCrowd2,spnParCrowd2));
        sb.append(getStringForKVLine(keyParCrowd3,spnParCrowd3));
        sb.append(getStringForKVLine(keyParCrowd4,spnParCrowd4));
        sb.append(keyPar15).append("=").append(cmbPar15.getSelectedItem()).append(NL);
        sb.append(getStringIfNotEmpty(keyPar16,txtPar16));
        sb.append(getStringIfNotEmpty(keyPar17,txtPar17));
        sb.append(getStringIfNotEmpty(keyPar18,txtPar18));
        sb.append(keyPar19).append("=").append(cmbPar19.getSelectedItem()).append(NL);
        sb.append(getStringIfNotEmpty(keyPar20,txtPar20));
        sb.append(getStringIfNotEmpty(keyPar21,txtPar21));
        sb.append(getStringIfNotEmpty(keyPar22,txtPar22));
        sb.append(getStringIfNotEmpty(keyPar24,txtPar24));
        sb.append(keyPar25).append("=").append(cmbPar25.getSelectedItem()).append(NL);
        //HEREGOESPRINT this is only to facilitate automated insertion of code        
    }
}
