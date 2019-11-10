package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Form collecting input parameters for a genetic algorithm experiment
 */

public class GAParametersForm extends ParametersForm
{

    /**
	 * Version
	 */
	private static final long serialVersionUID = 5067352357196631445L;
	
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

    String keyPar4 = "GA-ShowGraphics";
    JPanel linePar4;
    JRadioButton rdbPar4;

    String keyPar5 = "GA-SortOrder";
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
    JTextField txtPar12;

    String keyPar13 = "GA-GrowthSigmaSteepness";
    JPanel linePar13;
    JLabel lblPar13;
    JTextField txtPar13;

    String keyPar14 = "GA-GrowthSigmaMiddle";
    JPanel linePar14;
    JLabel lblPar14;
    JTextField txtPar14;

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
        this.setLayout(new BorderLayout()); //Needed to allow dynamic resizing!

        JPanel block = new JPanel();
        JScrollPane scrollablePane = new JScrollPane(block);
        block.setLayout(new BoxLayout(block, SwingConstants.VERTICAL));        

        String toolTipPar6 = "Specifies the number of individuals in the initial population.";
        linePar6 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar6 = new JLabel("Size of population:", SwingConstants.LEFT);
        lblPar6.setPreferredSize(fileLabelSize);
        lblPar6.setToolTipText(toolTipPar6);
        txtPar6 = new JTextField();
        txtPar6.setToolTipText(toolTipPar6);
        txtPar6.setPreferredSize(strFieldSize);
        linePar6.add(lblPar6);
        linePar6.add(txtPar6);
        block.add(linePar6);
        
        String toolTipPar7 = "Specifies the number of children to be generated for each generation.";
        linePar7 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar7 = new JLabel("No. offsprings per generation:", SwingConstants.LEFT);
        lblPar7.setPreferredSize(fileLabelSize);
        lblPar7.setToolTipText(toolTipPar7);
        txtPar7 = new JTextField();
        txtPar7.setToolTipText(toolTipPar7);
        txtPar7.setPreferredSize(strFieldSize);
        linePar7.add(lblPar7);
        linePar7.add(txtPar7);
        block.add(linePar7);

        String toolTipPar8 = "Specifies the maximum number of generation.";
        linePar8 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar8 = new JLabel("Max. number of generations:", SwingConstants.LEFT);
        lblPar8.setPreferredSize(fileLabelSize);
        lblPar8.setToolTipText(toolTipPar8);
        txtPar8 = new JTextField();
        txtPar8.setToolTipText(toolTipPar8);
        txtPar8.setPreferredSize(strFieldSize);
        linePar8.add(lblPar8);
        linePar8.add(txtPar8);
        block.add(linePar8);

        String toolTipPar9 = "Specifies the convergence criterion as number of subsequent identical generations.";
        linePar9 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar9 = new JLabel("Max. stagnating generations:", SwingConstants.LEFT);
        lblPar9.setPreferredSize(fileLabelSize);
        lblPar9.setToolTipText(toolTipPar9);
        txtPar9 = new JTextField();
        txtPar9.setToolTipText(toolTipPar9);
        txtPar9.setPreferredSize(strFieldSize);
        linePar9.add(lblPar9);
        linePar9.add(txtPar9);
        block.add(linePar9);
        
        JPanel localBlock2 = new JPanel();
        localBlock2.setVisible(true);
        localBlock2.setLayout(new BoxLayout(localBlock2, SwingConstants.VERTICAL));
        
        String toolTipPar12 = "<html>Specifies the value of the factor used in growth probability schemes <code>EXP_DIFF TANH</code></html>";
        linePar12 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar12 = new JLabel("<html>Graph extension - parameter <code>&lambda;</code><html>", SwingConstants.LEFT);
        lblPar12.setPreferredSize(fileLabelSize);
        lblPar12.setToolTipText(toolTipPar12);
        txtPar12 = new JTextField("1.0");
        txtPar12.setToolTipText(toolTipPar12);
        txtPar12.setPreferredSize(strFieldSize);
        linePar12.add(lblPar12);
        linePar12.add(txtPar12);
        localBlock2.add(linePar12);
        
        JPanel localBlock3 = new JPanel();
        localBlock3.setVisible(false);
        localBlock3.setLayout(new BoxLayout(localBlock3, SwingConstants.VERTICAL));
        
        String toolTipPar13 = "<html>Specifies the value of parameter &sigma;<sub>1</sub> used in growth probability scheme <code>SIGMA</code></html>";
        linePar13 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar13 = new JLabel("<html>Graph extension - parameter <code>&sigma;</code><sub>1</sub>:</html>", SwingConstants.LEFT);
        lblPar13.setPreferredSize(fileLabelSize);
        lblPar13.setToolTipText(toolTipPar13);
        txtPar13 = new JTextField();
        txtPar13.setToolTipText(toolTipPar13);
        txtPar13.setPreferredSize(strFieldSize);
        linePar13.add(lblPar13);
        linePar13.add(txtPar13);
        localBlock3.add(linePar13);
        
        String toolTipPar14 = "<html>Specifies the value of parameter &sigma;<sub>2</sub> used in growth probability scheme <code>SIGMA</code></html>";
        linePar14 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar14 = new JLabel("<html>Graph extension - parameter <code>&sigma;</code><sub>2</sub>:<html>", SwingConstants.LEFT);
        lblPar14.setPreferredSize(fileLabelSize);
        lblPar14.setToolTipText(toolTipPar14);
        txtPar14 = new JTextField();
        txtPar14.setToolTipText(toolTipPar14);
        txtPar14.setPreferredSize(strFieldSize);
        linePar14.add(lblPar14);
        linePar14.add(txtPar14);
        localBlock3.add(linePar14);

        //TODO: add graph for visualizing the function
        
        String toolTipPar11 = "Specifies the growth probability scheme";
        linePar11 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar11 = new JLabel("Graph extension probability function:", SwingConstants.LEFT);
        lblPar11.setPreferredSize(fileLabelSize);
        lblPar11.setToolTipText(toolTipPar11);
        cmbPar11 = new JComboBox<String>(new String[] {"EXP_DIFF", "TANH", "SIGMA", "UNRESTRICTED"});
        cmbPar11.setToolTipText(toolTipPar11);
        cmbPar11.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		switch (cmbPar11.getSelectedItem().toString())
        		{
        			case "EXP_DIFF":
        				localBlock2.setVisible(true);
            			localBlock3.setVisible(false);   
            			break;
            			
        			case "TANH":
        				localBlock2.setVisible(true);
            			localBlock3.setVisible(false);   
            			break;
            			
        			case "SIGMA":
        				localBlock2.setVisible(false);
            			localBlock3.setVisible(true);   
            			break;
            			
        			default:
        				localBlock2.setVisible(false);
            			localBlock3.setVisible(false);   
            			break;
        		}
	        }
	    });
        linePar11.add(lblPar11);
        linePar11.add(cmbPar11);
        block.add(linePar11);
        block.add(localBlock2);
        block.add(localBlock3);

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
        linePar15.add(lblPar15);
        linePar15.add(cmbPar15);
        block.add(linePar15);

        String toolTipPar16 = "Specifies the probability (0.0-1.0) at which crossover is performed.";
        linePar16 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar16 = new JLabel("Crossover probability:", SwingConstants.LEFT);
        lblPar16.setPreferredSize(fileLabelSize);
        lblPar16.setToolTipText(toolTipPar16);
        txtPar16 = new JTextField();
        txtPar16.setToolTipText(toolTipPar16);
        txtPar16.setPreferredSize(strFieldSize);
        linePar16.add(lblPar16);
        linePar16.add(txtPar16);
        block.add(linePar16);

        String toolTipPar17 = "Specifies the probability (0.0-1.0) at which mutation is performed.";
        linePar17 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar17 = new JLabel("Mutation probability:", SwingConstants.LEFT);
        lblPar17.setPreferredSize(fileLabelSize);
        lblPar17.setToolTipText(toolTipPar17);
        txtPar17 = new JTextField();
        txtPar17.setToolTipText(toolTipPar17);
        txtPar17.setPreferredSize(strFieldSize);
        linePar17.add(lblPar17);
        linePar17.add(txtPar17);
        block.add(linePar17);

        String toolTipPar18 = "Specifies the probability (0.0-1.0) of symmetric operations.";
        linePar18 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar18 = new JLabel("Symmetric operation probability:", SwingConstants.LEFT);
        lblPar18.setPreferredSize(fileLabelSize);
        lblPar18.setToolTipText(toolTipPar18);
        txtPar18 = new JTextField();
        txtPar18.setToolTipText(toolTipPar18);
        txtPar18.setPreferredSize(strFieldSize);
        linePar18.add(lblPar18);
        linePar18.add(txtPar18);
        block.add(linePar18);

        String toolTipPar19 = "Specifies the population members replacement strategy.";
        linePar19 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar19 = new JLabel("Population update mode:", SwingConstants.LEFT);
        lblPar19.setPreferredSize(fileLabelSize);
        lblPar19.setToolTipText(toolTipPar19);
        cmbPar19 = new JComboBox<String>(new String[] {"ELITIST", "NONE"});
        cmbPar19.setToolTipText(toolTipPar19);
        linePar19.add(lblPar19);
        linePar19.add(cmbPar19);
        block.add(linePar19);

        String toolTipPar24 = "Specifies the maximum number of parallel candidate evaluation tasks.";
        linePar24 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar24 = new JLabel("Number of parallel evaluation tasks:", SwingConstants.LEFT);
        lblPar24.setPreferredSize(fileLabelSize);
        lblPar24.setToolTipText(toolTipPar24);
        txtPar24 = new JTextField();
        txtPar24.setToolTipText(toolTipPar24);
        txtPar24.setPreferredSize(strFieldSize);
        linePar24.add(lblPar24);
        linePar24.add(txtPar24);
        block.add(linePar24);

        String toolTipPar25 = "<html>Specifies the parallelization scheme:<br><ul><li><code>synchronous</code>, i.e., parallel tasks are submitted in batches, thus no new task is submitted until the last of the previous tasks is completed.</li><li><code>asynchronous</code>, i.e., a new parallel tasks is submitted as soon as any of the previous task is completed.</li></ul></html>";
        linePar25 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar25 = new JLabel("Task parallelization scheme:", SwingConstants.LEFT);
        lblPar25.setPreferredSize(fileLabelSize);
        lblPar25.setToolTipText(toolTipPar25);
        cmbPar25 = new JComboBox<String>(new String[] {"Synchronous", "Asynchronous"});
        cmbPar25.setToolTipText(toolTipPar25);
        linePar25.add(lblPar25);
        linePar25.add(cmbPar25);
        block.add(linePar25);

        //HEREGOESIMPLEMENTATION this is only to facilitate automated insertion of code

        // From here it's all about advanced options
        JPanel advOptsBlock = new JPanel();
        advOptsBlock.setVisible(false);
        advOptsBlock.setLayout(new BoxLayout(advOptsBlock, SwingConstants.VERTICAL));

        String toolTipPar3 = "Specifies the seed number used by the random number generator";
        linePar3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar3 = new JLabel("Random Seed:", SwingConstants.LEFT);
        lblPar3.setPreferredSize(fileLabelSize);
        lblPar3.setToolTipText(toolTipPar3);
        txtPar3 = new JTextField();
        txtPar3.setToolTipText(toolTipPar3);
        txtPar3.setPreferredSize(strFieldSize);
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
        linePar2.add(lblPar2);
        linePar2.add(cmbPar2);
        advOptsBlock.add(linePar2);

        String toolTipPar4 = "<html>Specifies whether to produce the 2D molecular representation for each candidate.</html>";
        linePar4 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rdbPar4 = new JRadioButton("Prepare 2D graphs for candidates");
        rdbPar4.setToolTipText(toolTipPar4);
        linePar4.add(rdbPar4);
        advOptsBlock.add(linePar4);

        String toolTipPar5 = "Specifies whether the candidates should be reported in descending or ascending order of fitness.";
        linePar5 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rdbPar5 = new JRadioButton("Sort by descending fitness");
        rdbPar5.setToolTipText(toolTipPar5);
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
        btnPar20 = new JButton("Browse");
        btnPar20.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
                DenoptimGUIFileOpener.pickFile(txtPar20);
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
        btnPar21 = new JButton("Browse");
        btnPar21.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
                DenoptimGUIFileOpener.pickFile(txtPar21);
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
        btnPar22 = new JButton("Browse");
        btnPar22.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
                DenoptimGUIFileOpener.pickFile(txtPar22);
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
        block.add(new JSeparator());
        block.add(advOptsController);
        block.add(advOptsBlock);      
        
        this.add(scrollablePane);
    }

    @Override
    public void putParametersToString(StringBuilder sb) 
    {
        sb.append("# Genetic Algorithm - paramerers").append(NL);
        sb.append(getStringIfNotEmpty(keyPar3,txtPar3));;
        sb.append(getStringIfNotEmpty(keyPar1,txtPar1));;
        sb.append(keyPar2).append("=").append(cmbPar2.getSelectedItem()).append(NL);
        sb.append(getStringIfSelected(keyPar4,rdbPar4));
        sb.append(getStringIfSelected(keyPar5,rdbPar5));
        sb.append(getStringIfNotEmpty(keyPar6,txtPar6));;
        sb.append(getStringIfNotEmpty(keyPar7,txtPar7));;
        sb.append(getStringIfNotEmpty(keyPar8,txtPar8));;
        sb.append(getStringIfNotEmpty(keyPar9,txtPar9));;
        sb.append(getStringIfNotEmpty(keyPar10,txtPar10));;
        sb.append(keyPar11).append("=").append(cmbPar11.getSelectedItem()).append(NL);
        sb.append(getStringIfNotEmpty(keyPar12,txtPar12));;
        sb.append(getStringIfNotEmpty(keyPar13,txtPar13));;
        sb.append(getStringIfNotEmpty(keyPar14,txtPar14));;
        sb.append(keyPar15).append("=").append(cmbPar15.getSelectedItem()).append(NL);
        sb.append(getStringIfNotEmpty(keyPar16,txtPar16));;
        sb.append(getStringIfNotEmpty(keyPar17,txtPar17));;
        sb.append(getStringIfNotEmpty(keyPar18,txtPar18));;
        sb.append(keyPar19).append("=").append(cmbPar19.getSelectedItem()).append(NL);
        sb.append(getStringIfNotEmpty(keyPar20,txtPar20));
        sb.append(getStringIfNotEmpty(keyPar21,txtPar21));
        sb.append(getStringIfNotEmpty(keyPar22,txtPar22));
        sb.append(getStringIfNotEmpty(keyPar24,txtPar24));;
        sb.append(keyPar25).append("=").append(cmbPar25.getSelectedItem()).append(NL);
        //HEREGOESPRINT this is only to facilitate automated insertion of code        
    }
}
