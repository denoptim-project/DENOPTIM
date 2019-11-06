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
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Form collecting input parameters for a defining the fragment space.
 * Includes settings to handle ring-closures.
 * 
 * @author Marco Foscato
 */

public class FSParametersForm extends ParametersForm
{

    /**
	 * Version
	 */
	private static final long serialVersionUID = -821783042250597956L;
	
	JPanel lineSrcOrNew;
    JRadioButton rdbSrcOrNew;

    JPanel lineFSSource;
    JLabel lblFSSource;
    JTextField txtFSSource;
    JButton btnFSSource;

    String keyPar1 = "FS-ScaffoldLibFile";
    JPanel linePar1;
    JLabel lblPar1;
    JTextField txtPar1;
    JButton btnPar1;

    String keyPar2 = "FS-FragmentLibFile";
    JPanel linePar2;
    JLabel lblPar2;
    JTextField txtPar2;
    JButton btnPar2;

    String keyPar3 = "FS-CappingFragmentLibFile";
    JPanel linePar3;
    JLabel lblPar3;
    JTextField txtPar3;
    JButton btnPar3;

    String keyPar4 = "FS-CompMatrixFile";
    JPanel linePar4;
    JLabel lblPar4;
    JTextField txtPar4;
    JButton btnPar4;

    String keyPar6 = "FS-RotBondsDefFile";
    JPanel linePar6;
    JLabel lblPar6;
    JTextField txtPar6;
    JButton btnPar6;

    String keyPar7 = "FS-MaxHeavyAtom";
    JPanel linePar7;
    JLabel lblPar7;
    JTextField txtPar7;

    String keyPar8 = "FS-MaxMW";
    JPanel linePar8;
    JLabel lblPar8;
    JTextField txtPar8;

    String keyPar9 = "FS-MaxRotatableBond";
    JPanel linePar9;
    JLabel lblPar9;
    JTextField txtPar9;

    String keyPar10 = "FS-EnforceSymmetry";
    JPanel linePar10;
    JRadioButton rdbPar10;

    String keyPar11 = "FS-ConstrainSymmetry";
    JPanel linePar11;
    JLabel lblPar11;
    JTextField txtPar11;
    
    String keyPar12 = "RC-CloseRings";
    JPanel linePar12;
    JRadioButton rdbPar12;

    String keyPar5 = "FS-RCCompMatrixFile";
    JPanel linePar5;
    JLabel lblPar5;
    JTextField txtPar5;
    JButton btnPar5;

    String keyPar15 = "RC-MinNumberOfRingClosures";
    JPanel linePar15;
    JLabel lblPar15;
    JTextField txtPar15;

    String keyPar16 = "RC-MaxNumberRingClosures";
    JPanel linePar16;
    JLabel lblPar16;
    JTextField txtPar16;

    String keyPar17 = "RC-MinRCAPerTypePerGraph";
    JPanel linePar17;
    JLabel lblPar17;
    JTextField txtPar17;

    String keyPar18 = "RC-MaxRCAPerTypePerGraph";
    JPanel linePar18;
    JLabel lblPar18;
    JTextField txtPar18;

    String keyPar22 = "RC-RingSizeBias";
    JPanel linePar22;
    JLabel lblPar22;
    JTextField txtPar22;

    String keyPar23 = "RC-MaxSizeNewRings";
    JPanel linePar23;
    JLabel lblPar23;
    JTextField txtPar23;

    String keyPar19 = "RC-EvaluationClosabilityMode";
    JPanel linePar19;
    JLabel lblPar19;
    JComboBox<String> cmbPar19;

    String keyPar21 = "RC-ClosableRingSMARTS";
    JPanel linePar21;
    JLabel lblPar21;
    JTextField txtPar21;

    String keyPar24 = "RC-CheckInterdependentChains";
    JPanel linePar24;
    JRadioButton rdbPar24;

    String keyPar25 = "RC-MaxRotBonds";
    JPanel linePar25;
    JLabel lblPar25;
    JTextField txtPar25;

    String keyPar26 = "RC-ConfSearchStep";
    JPanel linePar26;
    JLabel lblPar26;
    JTextField txtPar26;

    String keyPar27 = "RC-LinearityLimit";
    JPanel linePar27;
    JLabel lblPar27;
    JTextField txtPar27;

    String keyPar28 = "RC-ExhaustiveConfSearch";
    JPanel linePar28;
    JRadioButton rdbPar28;

    String keyPar30 = "RC-RCCIndex";
    JPanel linePar30;
    JLabel lblPar30;
    JTextField txtPar30;
    JButton btnPar30;

    String keyPar31 = "RC-RCCFolder";
    JPanel linePar31;
    JLabel lblPar31;
    JTextField txtPar31;
    JButton btnPar31;

    String keyPar32 = "RC-MaxDotProd";
    JPanel linePar32;
    JLabel lblPar32;
    JTextField txtPar32;

    String keyPar33 = "RC-DistanceToleranceFactor";
    JPanel linePar33;
    JLabel lblPar33;
    JTextField txtPar33;

    String keyPar34 = "RC-ExtraDistanceToleranceFactor";
    JPanel linePar34;
    JLabel lblPar34;
    JTextField txtPar34;

    //HEREGOFIELDS  this is only to facilitate automated insertion of code    
        
    String NL = System.getProperty("line.separator");
    
    public FSParametersForm(Dimension d)
    {
        this.setLayout(new BorderLayout()); //Needed to allow dynamic resizing!

        JPanel block = new JPanel();
        JScrollPane scrollablePane = new JScrollPane(block);
        block.setLayout(new BoxLayout(block, SwingConstants.VERTICAL));    
        
        JPanel localBlock1 = new JPanel();
        localBlock1.setVisible(false);
        localBlock1.setLayout(new BoxLayout(localBlock1, SwingConstants.VERTICAL));
        
        JPanel localBlock2 = new JPanel();
        localBlock2.setVisible(true);
        localBlock2.setLayout(new BoxLayout(localBlock2, SwingConstants.VERTICAL));

        String toolTipSrcOrNew = "Tick here to use a previously defined fragment space.";
        lineSrcOrNew = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rdbSrcOrNew = new JRadioButton("Use existing fragment space:");
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

        //HEREGOESIMPLEMENTATION this is only to facilitate automated insertion of code

        // From here is all stuff that will become visible once some previously defined options have been selected

        String toolTipFSSource = "<html>Pathname of the file defining the fragment space.<br>This file is generated by the DENOPTIM's GUI upon construction of a new fragment space.</html>";
        lineFSSource = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblFSSource = new JLabel("Fragment space file:", SwingConstants.LEFT);
        lblFSSource.setPreferredSize(fileLabelSize);
        lblFSSource.setToolTipText(toolTipFSSource);
        txtFSSource = new JTextField();
        txtFSSource.setToolTipText(toolTipFSSource);
        txtFSSource.setPreferredSize(fileFieldSize);
        btnFSSource = new JButton("Browse");
        btnFSSource.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
                DenoptimGUIFileOpener.pickFile(txtFSSource);
           }
        });
        lineFSSource.add(lblFSSource);
        lineFSSource.add(txtFSSource);
        lineFSSource.add(btnFSSource);
        localBlock1.add(lineFSSource);

        String toolTipPar1 = "Pathname of the file containing the list of scaffolds.";
        linePar1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar1 = new JLabel("Scaffold fragments library:", SwingConstants.LEFT);
        lblPar1.setPreferredSize(fileLabelSize);
        lblPar1.setToolTipText(toolTipPar1);
        txtPar1 = new JTextField();
        txtPar1.setToolTipText(toolTipPar1);
        txtPar1.setPreferredSize(fileFieldSize);
        btnPar1 = new JButton("Browse");
        btnPar1.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
                DenoptimGUIFileOpener.pickFile(txtPar1);
           }
        });
        linePar1.add(lblPar1);
        linePar1.add(txtPar1);
        linePar1.add(btnPar1);
        localBlock2.add(linePar1);

        String toolTipPar2 = "Pathname of the file containing the list of fragments.";
        linePar2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar2 = new JLabel("Fragments library:", SwingConstants.LEFT);
        lblPar2.setPreferredSize(fileLabelSize);
        lblPar2.setToolTipText(toolTipPar2);
        txtPar2 = new JTextField();
        txtPar2.setToolTipText(toolTipPar2);
        txtPar2.setPreferredSize(fileFieldSize);
        btnPar2 = new JButton("Browse");
        btnPar2.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
                DenoptimGUIFileOpener.pickFile(txtPar2);
           }
        });
        linePar2.add(lblPar2);
        linePar2.add(txtPar2);
        linePar2.add(btnPar2);
        localBlock2.add(linePar2);

        String toolTipPar3 = "Pathname of the file containing the list of capping groups.";
        linePar3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar3 = new JLabel("Capping groups library:", SwingConstants.LEFT);
        lblPar3.setPreferredSize(fileLabelSize);
        lblPar3.setToolTipText(toolTipPar3);
        txtPar3 = new JTextField();
        txtPar3.setToolTipText(toolTipPar3);
        txtPar3.setPreferredSize(fileFieldSize);
        btnPar3 = new JButton("Browse");
        btnPar3.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
                DenoptimGUIFileOpener.pickFile(txtPar3);
           }
        });
        linePar3.add(lblPar3);
        linePar3.add(txtPar3);
        linePar3.add(btnPar3);
        localBlock2.add(linePar3);

        String toolTipPar4 = "<html>Pathname of the compatibility matrix file.<br>Note that this file contains the compatibility matrix, map of AP-Class to bond order, the capping rules, and the list of forbidden ends.</html>";
        linePar4 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar4 = new JLabel("Compatibility matrix file:", SwingConstants.LEFT);
        lblPar4.setPreferredSize(fileLabelSize);
        lblPar4.setToolTipText(toolTipPar4);
        txtPar4 = new JTextField();
        txtPar4.setToolTipText(toolTipPar4);
        txtPar4.setPreferredSize(fileFieldSize);
        btnPar4 = new JButton("Browse");
        btnPar4.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
                DenoptimGUIFileOpener.pickFile(txtPar4);
           }
        });
        linePar4.add(lblPar4);
        linePar4.add(txtPar4);
        linePar4.add(btnPar4);
        localBlock2.add(linePar4);

        String toolTipPar6 = "<html>Pathname of the file containing the definition of the rotatable bonds.<br>Must be a list of SMARTS.</html>";
        linePar6 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar6 = new JLabel("Rotatable bonds list:", SwingConstants.LEFT);
        lblPar6.setPreferredSize(fileLabelSize);
        lblPar6.setToolTipText(toolTipPar6);
        txtPar6 = new JTextField();
        txtPar6.setToolTipText(toolTipPar6);
        txtPar6.setPreferredSize(fileFieldSize);
        btnPar6 = new JButton("Browse");
        btnPar6.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
                DenoptimGUIFileOpener.pickFile(txtPar6);
           }
        });
        linePar6.add(lblPar6);
        linePar6.add(txtPar6);
        linePar6.add(btnPar6);
        localBlock2.add(linePar6);

        String toolTipPar7 = "Maximum number of heavy (non-hydrogen) atoms for a candidate.";
        linePar7 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar7 = new JLabel("Max non-H atoms:", SwingConstants.LEFT);
        lblPar7.setPreferredSize(fileLabelSize);
        lblPar7.setToolTipText(toolTipPar7);
        txtPar7 = new JTextField();
        txtPar7.setToolTipText(toolTipPar7);
        txtPar7.setPreferredSize(strFieldSize);
        linePar7.add(lblPar7);
        linePar7.add(txtPar7);
        localBlock2.add(linePar7);

        String toolTipPar8 = "Maximum molecular weight accepted for a candidate";
        linePar8 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar8 = new JLabel("Max molecular weight: ", SwingConstants.LEFT);
        lblPar8.setPreferredSize(fileLabelSize);
        lblPar8.setToolTipText(toolTipPar8);
        txtPar8 = new JTextField();
        txtPar8.setToolTipText(toolTipPar8);
        txtPar8.setPreferredSize(strFieldSize);
        linePar8.add(lblPar8);
        linePar8.add(txtPar8);
        localBlock2.add(linePar8);

        String toolTipPar9 = "Maximum number of rotatable bonds accepted for a candidate";
        linePar9 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar9 = new JLabel("Max rotatable bonds:", SwingConstants.LEFT);
        lblPar9.setPreferredSize(fileLabelSize);
        lblPar9.setToolTipText(toolTipPar9);
        txtPar9 = new JTextField();
        txtPar9.setToolTipText(toolTipPar9);
        txtPar9.setPreferredSize(strFieldSize);
        linePar9.add(lblPar9);
        linePar9.add(txtPar9);
        localBlock2.add(linePar9);

        String toolTipPar10 = "<html>Forces constitutional symmetry whenever possible.<br>Corresponds to setting the symmetric substitution probability to 100%.</html>";      
        linePar10 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rdbPar10 = new JRadioButton("Enforce symmetry:");
        rdbPar10.setToolTipText(toolTipPar10);
        linePar10.add(rdbPar10);
        localBlock2.add(linePar10);

        String toolTipPar11 = "<html>TODO(toTable): List of constraints in the symmetric substitution probability.<br>These constraints overwrite the any other settings only for the specific AP-classes.</html>";
        linePar11 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar11 = new JLabel("Symmetry constraints:", SwingConstants.LEFT);
        lblPar11.setPreferredSize(fileLabelSize);
        lblPar11.setToolTipText(toolTipPar11);
        txtPar11 = new JTextField();
        txtPar11.setToolTipText(toolTipPar11);
        txtPar11.setPreferredSize(strFieldSize);
        linePar11.add(lblPar11);
        linePar11.add(txtPar11);
        localBlock2.add(linePar11);   
        
        JPanel localBlock3 = new JPanel();
        localBlock3.setVisible(false);
        localBlock3.setLayout(new BoxLayout(localBlock3, SwingConstants.VERTICAL));
        
        String toolTipPar12 = "<html>Activates search and handling of rings of fragments.</html>";
        linePar12 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rdbPar12 = new JRadioButton("Enable cyclic graphs (create rings of fragments)");
        rdbPar12.setToolTipText(toolTipPar12);
        rdbPar12.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		if (rdbPar12.isSelected())
        		{
    				localBlock3.setVisible(true);
    				scrollablePane.validate();
    				scrollablePane.repaint();
    				scrollablePane.getVerticalScrollBar().setValue(
    						scrollablePane.getVerticalScrollBar().getValue() + (int) preferredHeight*2/3);
        		}
        		else
        		{
        			localBlock3.setVisible(false);
        		}
        	}
        });
        linePar12.add(rdbPar12);
        localBlock2.add(linePar12);
        localBlock2.add(localBlock3);
        
        String toolTipPar5 = "Pathname of the file containing the compatibility matrix for ring closures";
        linePar5 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar5 = new JLabel("Compatibility matrix for ring closures:", SwingConstants.LEFT);
        lblPar5.setPreferredSize(fileLabelSize);
        lblPar5.setToolTipText(toolTipPar5);
        txtPar5 = new JTextField();
        txtPar5.setToolTipText(toolTipPar5);
        txtPar5.setPreferredSize(fileFieldSize);
        btnPar5 = new JButton("Browse");
        btnPar5.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
                DenoptimGUIFileOpener.pickFile(txtPar5);
           }
        });
        linePar5.add(lblPar5);
        linePar5.add(txtPar5);
        linePar5.add(btnPar5);
        localBlock3.add(linePar5);

        String toolTipPar15 = "Specifies the minimum number of rings to be closed in order to accept a candidate graph.";
        linePar15 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar15 = new JLabel("Min. number of ring closures:", SwingConstants.LEFT);
        lblPar15.setPreferredSize(fileLabelSize);
        lblPar15.setToolTipText(toolTipPar15);
        txtPar15 = new JTextField();
        txtPar15.setToolTipText(toolTipPar15);
        txtPar15.setPreferredSize(strFieldSize);
        linePar15.add(lblPar15);
        linePar15.add(txtPar15);
        localBlock3.add(linePar15);

        String toolTipPar16 = "Maximum number of ring closures per graph.";
        linePar16 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar16 = new JLabel("Max. number of ring closures:", SwingConstants.LEFT);
        lblPar16.setPreferredSize(fileLabelSize);
        lblPar16.setToolTipText(toolTipPar16);
        txtPar16 = new JTextField();
        txtPar16.setToolTipText(toolTipPar16);
        txtPar16.setPreferredSize(strFieldSize);
        linePar16.add(lblPar16);
        linePar16.add(txtPar16);
        localBlock3.add(linePar16);

        String toolTipPar17 = "Minimum number of ring closing attractors (RCA) of the same type per graph.";
        linePar17 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar17 = new JLabel("<html>Min. num. RCA<sub>type</sub> per graph:</html>", SwingConstants.LEFT);
        lblPar17.setPreferredSize(fileLabelSize);
        lblPar17.setToolTipText(toolTipPar17);
        txtPar17 = new JTextField();
        txtPar17.setToolTipText(toolTipPar17);
        txtPar17.setPreferredSize(strFieldSize);
        linePar17.add(lblPar17);
        linePar17.add(txtPar17);
        localBlock3.add(linePar17);

        String toolTipPar18 = "Maximum number of ring closing attractors (RCA) of the same type that are accepted for a single graph.";
        linePar18 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar18 = new JLabel("<html>Max. num. RCA<sub>type</sub> per graph:</html>", SwingConstants.LEFT);
        lblPar18.setPreferredSize(fileLabelSize);
        lblPar18.setToolTipText(toolTipPar18);
        txtPar18 = new JTextField();
        txtPar18.setToolTipText(toolTipPar18);
        txtPar18.setPreferredSize(strFieldSize);
        linePar18.add(lblPar18);
        linePar18.add(txtPar18);
        localBlock3.add(linePar18);

        String toolTipPar22 = "TODO(toTable): Specifies the bias associated to a given ring size when selecting the combination of rings (i.e., RCAs) for a given graph.";
        linePar22 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar22 = new JLabel("Ring size preference biases:", SwingConstants.LEFT);
        lblPar22.setPreferredSize(fileLabelSize);
        lblPar22.setToolTipText(toolTipPar22);
        txtPar22 = new JTextField();
        txtPar22.setToolTipText(toolTipPar22);
        txtPar22.setPreferredSize(strFieldSize);
        linePar22.add(lblPar22);
        linePar22.add(txtPar22);
        localBlock3.add(linePar22);

        String toolTipPar23 = "Specifies the maximum number of ring members for rings created from scratch";
        linePar23 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar23 = new JLabel("Max. size of new rings:", SwingConstants.LEFT);
        lblPar23.setPreferredSize(fileLabelSize);
        lblPar23.setToolTipText(toolTipPar23);
        txtPar23 = new JTextField();
        txtPar23.setToolTipText(toolTipPar23);
        txtPar23.setPreferredSize(strFieldSize);
        linePar23.add(lblPar23);
        linePar23.add(txtPar23);
        localBlock3.add(linePar23);
        
        JPanel localBlock4 = new JPanel();
        localBlock4.setVisible(true);
        localBlock4.setLayout(new BoxLayout(localBlock4, SwingConstants.VERTICAL));
        
        JPanel localBlock5 = new JPanel();
        localBlock5.setVisible(false);
        localBlock5.setLayout(new BoxLayout(localBlock5, SwingConstants.VERTICAL));

        String toolTipPar19 = "Defined the closability condition's evaluation mode.";
        linePar19 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar19 = new JLabel("Type of ring-closability conditions", SwingConstants.LEFT);
        lblPar19.setPreferredSize(fileLabelSize);
        lblPar19.setToolTipText(toolTipPar19);
        cmbPar19 = new JComboBox<String>(new String[] {"Constitution", "Conformation(3D)", "Constitution and Conformation(3D)"});
        cmbPar19.setToolTipText(toolTipPar19);
        cmbPar19.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		switch (cmbPar19.getSelectedItem().toString())
        		{
        			case "Constitution":
        				localBlock4.setVisible(true);
            			localBlock5.setVisible(false);   
            			break;
            			
        			case "Conformation(3D)":
        				localBlock4.setVisible(false);
            			localBlock5.setVisible(true); 
            			break;
            			
        			case "Constitution and Conformation(3D)":
        				localBlock4.setVisible(true);
            			localBlock5.setVisible(true);   
            			break;
        		}
	        }
	    });
        linePar19.add(lblPar19);
        linePar19.add(cmbPar19);
        localBlock3.add(linePar19);
        localBlock3.add(localBlock4);
        localBlock3.add(localBlock5);

        String toolTipPar21 = "TODO(toTable): Specifies the constitutional ring closability conditions as SMARTS string.";
        linePar21 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar21 = new JLabel("Constitutional ring-closability rules:", SwingConstants.LEFT);
        lblPar21.setPreferredSize(fileLabelSize);
        lblPar21.setToolTipText(toolTipPar21);
        txtPar21 = new JTextField();
        txtPar21.setToolTipText(toolTipPar21);
        txtPar21.setPreferredSize(strFieldSize);
        linePar21.add(lblPar21);
        linePar21.add(txtPar21);
        localBlock4.add(linePar21);

        String toolTipPar25 = "Specifies the maximum number of rotatable bonds for which 3D chain closability is evaluated. Chains with a number of rotatable bonds higher than this value are assumed closable.";
        linePar25 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar25 = new JLabel("Max. rotatable bonds:", SwingConstants.LEFT);
        lblPar25.setPreferredSize(fileLabelSize);
        lblPar25.setToolTipText(toolTipPar25);
        txtPar25 = new JTextField();
        txtPar25.setToolTipText(toolTipPar25);
        txtPar25.setPreferredSize(strFieldSize);
        linePar25.add(lblPar25);
        linePar25.add(txtPar25);
        localBlock5.add(linePar25);

        String toolTipPar26 = "Specifies the torsion angle step (degrees) to be used for the evaluation of 3D chain closability by scanning the torsional space.";
        linePar26 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar26 = new JLabel("Dihedral angle step:", SwingConstants.LEFT);
        lblPar26.setPreferredSize(fileLabelSize);
        lblPar26.setToolTipText(toolTipPar26);
        txtPar26 = new JTextField();
        txtPar26.setToolTipText(toolTipPar26);
        txtPar26.setPreferredSize(strFieldSize);
        linePar26.add(lblPar26);
        linePar26.add(txtPar26);
        localBlock5.add(linePar26);

        String toolTipPar27 = "Specifies the bond angle above which three atoms are considered to be in a linear arragement.";
        linePar27 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar27 = new JLabel("Bond angle - linearity threshold:", SwingConstants.LEFT);
        lblPar27.setPreferredSize(fileLabelSize);
        lblPar27.setToolTipText(toolTipPar27);
        txtPar27 = new JTextField();
        txtPar27.setToolTipText(toolTipPar27);
        txtPar27.setPreferredSize(strFieldSize);
        linePar27.add(lblPar27);
        linePar27.add(txtPar27);
        localBlock5.add(linePar27);
        
        String toolTipPar24 = "<html>Requires evaluation of interdependent closability condition.<br><b>WARNING:</b> this function require exhaustive conformational search, which is very time consuming.</html>";
        linePar24 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rdbPar24 = new JRadioButton("Check interdependent chain closability.");
        rdbPar24.setToolTipText(toolTipPar24);
        linePar24.add(rdbPar24);
        localBlock5.add(linePar24);

        String toolTipPar28 = "<html>Requires the search for closable conformations to explore the complete rotational space.<br><b>WARNING:</b> this is very time consuming, but is currently needed to evaluate closability of interdependent chains.</html>";
        linePar28 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rdbPar28 = new JRadioButton("Extensive chain conformational search:");
        rdbPar28.setToolTipText(toolTipPar28);
        linePar28.add(rdbPar28);
        localBlock5.add(linePar28);

        String toolTipPar30 = "Pathname of the text file containing the previously encountered candidate closable chains. This file constitutes the index of the archive of ring closing conformations.";
        linePar30 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar30 = new JLabel("Index of closable chain archive:", SwingConstants.LEFT);
        lblPar30.setPreferredSize(fileLabelSize);
        lblPar30.setToolTipText(toolTipPar30);
        txtPar30 = new JTextField();
        txtPar30.setToolTipText(toolTipPar30);
        txtPar30.setPreferredSize(fileFieldSize);
        btnPar30 = new JButton("Browse");
        btnPar30.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
                DenoptimGUIFileOpener.pickFile(txtPar30);
           }
        });
        linePar30.add(lblPar30);
        linePar30.add(txtPar30);
        linePar30.add(btnPar30);
        localBlock5.add(linePar30);

        String toolTipPar31 = "Pathname of the folder containing the archive of ring closing conformations.";
        linePar31 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar31 = new JLabel("Root folder of closable chain archive:", SwingConstants.LEFT);
        lblPar31.setPreferredSize(fileLabelSize);
        lblPar31.setToolTipText(toolTipPar31);
        txtPar31 = new JTextField();
        txtPar31.setToolTipText(toolTipPar31);
        txtPar31.setPreferredSize(fileFieldSize);
        btnPar31 = new JButton("Browse");
        btnPar31.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
                DenoptimGUIFileOpener.pickFile(txtPar31);
           }
        });
        linePar31.add(lblPar31);
        linePar31.add(txtPar31);
        linePar31.add(btnPar31);
        localBlock5.add(linePar31);

        String toolTipPar32 = "Specifies the maximum value of the dot product of the two AP-vectors.";
        linePar32 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar32 = new JLabel("Closability – Dot product threshold:", SwingConstants.LEFT);
        lblPar32.setPreferredSize(fileLabelSize);
        lblPar32.setToolTipText(toolTipPar32);
        txtPar32 = new JTextField();
        txtPar32.setToolTipText(toolTipPar32);
        txtPar32.setPreferredSize(strFieldSize);
        linePar32.add(lblPar32);
        linePar32.add(txtPar32);
        localBlock5.add(linePar32);

        String toolTipPar33 = "Specifies the absolute normal deviation of the ideal value (a value between 0.0 and 1.0) that is considered acceptable for distances when evaluating the 3D ring closability of a conformation";
        linePar33 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar33 = new JLabel("Closability – Distance deviation:", SwingConstants.LEFT);
        lblPar33.setPreferredSize(fileLabelSize);
        lblPar33.setToolTipText(toolTipPar33);
        txtPar33 = new JTextField();
        txtPar33.setToolTipText(toolTipPar33);
        txtPar33.setPreferredSize(strFieldSize);
        linePar33.add(lblPar33);
        linePar33.add(txtPar33);
        localBlock5.add(linePar33);

        String toolTipPar34 = "Specifies the factor multiplying the tolerance for interatomic distances when evaluating the closability of a chain by a discrete (vs. continuous) exploration of torsional space.";
        linePar34 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar34 = new JLabel("Closability – Additional tolerance:", SwingConstants.LEFT);
        lblPar34.setPreferredSize(fileLabelSize);
        lblPar34.setToolTipText(toolTipPar34);
        txtPar34 = new JTextField();
        txtPar34.setToolTipText(toolTipPar34);
        txtPar34.setPreferredSize(strFieldSize);
        linePar34.add(lblPar34);
        linePar34.add(txtPar34);
        localBlock5.add(linePar34);

        //HEREGOESADVIMPLEMENTATION this is only to facilitate automated insertion of code    
        
        this.add(scrollablePane);
    }

    @Override
    public void putParametersToString(StringBuilder sb) 
    {
        sb.append("# FragmentSpace-paramerers").append(NL);
        if (rdbSrcOrNew.isSelected())
        {
        	FSFile fsFile = new FSFile(txtFSSource.getText());
        	sb.append(fsFile.getKVParamsText());
        }
        else
        {
	        sb.append(getStringIfNotEmpty(keyPar1,txtPar1));
	        sb.append(getStringIfNotEmpty(keyPar2,txtPar2));
	        sb.append(getStringIfNotEmpty(keyPar3,txtPar3));
	        sb.append(getStringIfNotEmpty(keyPar4,txtPar4));
	        sb.append(getStringIfNotEmpty(keyPar6,txtPar6));
	        sb.append(getStringIfNotEmpty(keyPar7,txtPar7));
	        sb.append(getStringIfNotEmpty(keyPar8,txtPar8));
	        sb.append(getStringIfNotEmpty(keyPar9,txtPar9));
	        sb.append(getStringIfSelected(keyPar10,rdbPar10));
	        sb.append(getStringIfNotEmpty(keyPar11,txtPar11));
	        sb.append(getStringIfSelected(keyPar12,rdbPar12));
	        sb.append(getStringIfNotEmpty(keyPar5,txtPar5));
	        sb.append(getStringIfNotEmpty(keyPar15,txtPar15));
	        sb.append(getStringIfNotEmpty(keyPar16,txtPar16));
	        sb.append(getStringIfNotEmpty(keyPar17,txtPar17));
	        sb.append(getStringIfNotEmpty(keyPar18,txtPar18));
	        sb.append(getStringIfNotEmpty(keyPar22,txtPar22));
	        sb.append(getStringIfNotEmpty(keyPar23,txtPar23));
	        sb.append(keyPar19).append("=").append(cmbPar19.getSelectedItem()).append(NL);
	        sb.append(getStringIfNotEmpty(keyPar21,txtPar21));
	        sb.append(getStringIfSelected(keyPar24,rdbPar24));
	        sb.append(getStringIfNotEmpty(keyPar25,txtPar25));
	        sb.append(getStringIfNotEmpty(keyPar26,txtPar26));
	        sb.append(getStringIfNotEmpty(keyPar27,txtPar27));
	        sb.append(getStringIfSelected(keyPar28,rdbPar28));
	        sb.append(getStringIfNotEmpty(keyPar30,txtPar30));
	        sb.append(getStringIfNotEmpty(keyPar31,txtPar31));
	        sb.append(getStringIfNotEmpty(keyPar32,txtPar32));
	        sb.append(getStringIfNotEmpty(keyPar33,txtPar33));
	        sb.append(getStringIfNotEmpty(keyPar34,txtPar34));
        //HEREGOESPRINT this is only to facilitate automated insertion of code
        }       
    }
}
