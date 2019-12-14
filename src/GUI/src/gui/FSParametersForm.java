package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

import denoptim.fragspace.FragmentSpace;

/**
 * Form collecting input parameters for defining the fragment space.
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
    JPanel localBlock5;
	
	JPanel lineSrcOrNew;
    JRadioButton rdbSrcOrNew;

    JPanel lineFSSource;
    JLabel lblFSSource;
    JTextField txtFSSource;
    JButton btnFSSource;
    JButton btnLoadFSSource;

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

    String keyCPMat = "FS-CompMatrixFile";
    JPanel lineCPMat;
    JLabel lblCPMat;
    JTextField txtCPMat;
    JButton btnCPMat;

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
    JButton btnPar11Insert;
    JButton btnPar11Cleanup;
    JLabel lblPar11;
    JTable tabPar11;
    DefaultTableModel tabModPar11;
    
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
    JButton btnPar22Insert;
    JButton btnPar22Cleanup;
    JTable tabPar22;
    DefaultTableModel tabModPar22;

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
    JButton btnPar21Insert;
    JButton btnPar21Cleanup;
    JTable tabPar21;
    DefaultTableModel tabModPar21;

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
    	mapKeyFieldToValueField = new HashMap<String,Object>();
    	
        this.setLayout(new BorderLayout()); //Needed to allow dynamic resizing!

        JPanel block = new JPanel();
        JScrollPane scrollablePane = new JScrollPane(block);
        block.setLayout(new BoxLayout(block, SwingConstants.VERTICAL));    
        
        localBlock1 = new JPanel();
        localBlock1.setVisible(false);
        localBlock1.setLayout(new BoxLayout(localBlock1, SwingConstants.VERTICAL));
        
        localBlock2 = new JPanel();
        localBlock2.setVisible(true);
        localBlock2.setLayout(new BoxLayout(localBlock2, SwingConstants.VERTICAL));
        
        localBlock3 = new JPanel();
        localBlock3.setVisible(false);
        localBlock3.setLayout(new BoxLayout(localBlock3, SwingConstants.VERTICAL));
        
        localBlock4 = new JPanel();
        localBlock4.setVisible(true);
        localBlock4.setLayout(new BoxLayout(localBlock4, SwingConstants.VERTICAL));
        
        localBlock5 = new JPanel();
        localBlock5.setVisible(false);
        localBlock5.setLayout(new BoxLayout(localBlock5, SwingConstants.VERTICAL));

        String toolTipSrcOrNew = "Tick here to use a previously defined fragment space.";
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

        //HEREGOESIMPLEMENTATION this is only to facilitate automated insertion of code

        // From here is all stuff that will become visible once some previously defined options have been selected

        String toolTipFSSource = "<html>Pathname of a DENOPTIM's parameter file with GA settings.</html>";
        lineFSSource = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblFSSource = new JLabel("Fragment space file:", SwingConstants.LEFT);
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
        btnLoadFSSource = new JButton("Load...");
        txtFSSource.setToolTipText("<html>Load the parameters in this form.<br>Allows to inspect and edit the parameters.</html>");
        btnLoadFSSource.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
	        	try 
	        	{
					importParametersFromDenoptimParamsFile(txtFSSource.getText());
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
        lineFSSource.add(lblFSSource);
        lineFSSource.add(txtFSSource);
        lineFSSource.add(btnFSSource);
        lineFSSource.add(btnLoadFSSource);
        localBlock1.add(lineFSSource);

        String toolTipPar1 = "Pathname of the file containing the list of scaffolds.";
        linePar1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar1 = new JLabel("Scaffold fragments library:", SwingConstants.LEFT);
        lblPar1.setPreferredSize(fileLabelSize);
        lblPar1.setToolTipText(toolTipPar1);
        txtPar1 = new JTextField();
        txtPar1.setToolTipText(toolTipPar1);
        txtPar1.setPreferredSize(fileFieldSize);
        txtPar1.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar1.toUpperCase(),txtPar1);
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
        txtPar2.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar2.toUpperCase(),txtPar2);
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
        txtPar3.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar3.toUpperCase(),txtPar3);
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

        String toolTipCPMat = "<html>Pathname of the compatibility matrix file.<br>Note that this file contains the compatibility matrix, map of AP-Class to bond order, the capping rules, and the list of forbidden ends.</html>";
        lineCPMat = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblCPMat = new JLabel("Compatibility matrix file:", SwingConstants.LEFT);
        lblCPMat.setPreferredSize(fileLabelSize);
        lblCPMat.setToolTipText(toolTipCPMat);
        txtCPMat = new JTextField();
        txtCPMat.setToolTipText(toolTipCPMat);
        txtCPMat.setPreferredSize(fileFieldSize);
        txtCPMat.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyCPMat.toUpperCase(),txtCPMat);
        btnCPMat = new JButton("Browse");
        btnCPMat.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
                DenoptimGUIFileOpener.pickFile(txtCPMat);
           }
        });
        lineCPMat.add(lblCPMat);
        lineCPMat.add(txtCPMat);
        lineCPMat.add(btnCPMat);
        localBlock2.add(lineCPMat);

        String toolTipPar6 = "<html>Pathname of the file containing the definition of the rotatable bonds.<br>Must be a list of SMARTS.</html>";
        linePar6 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar6 = new JLabel("Rotatable bonds list:", SwingConstants.LEFT);
        lblPar6.setPreferredSize(fileLabelSize);
        lblPar6.setToolTipText(toolTipPar6);
        txtPar6 = new JTextField();
        txtPar6.setToolTipText(toolTipPar6);
        txtPar6.setPreferredSize(fileFieldSize);
        txtPar6.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar6.toUpperCase(),txtPar6);
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
        txtPar7.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar7.toUpperCase(),txtPar7);
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
        txtPar8.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar8.toUpperCase(),txtPar8);
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
        txtPar9.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar9.toUpperCase(),txtPar9);
        linePar9.add(lblPar9);
        linePar9.add(txtPar9);
        localBlock2.add(linePar9);

        String toolTipPar10 = "<html>Forces constitutional symmetry whenever possible.<br>Corresponds to setting the symmetric substitution probability to 100%.</html>";      
        linePar10 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rdbPar10 = new JRadioButton("Enforce symmetry:");
        rdbPar10.setToolTipText(toolTipPar10);
        rdbPar10.addChangeListener(rdbFieldChange);
        mapKeyFieldToValueField.put(keyPar10.toUpperCase(),rdbPar10);
        linePar10.add(rdbPar10);
        localBlock2.add(linePar10);

        String toolTipPar11 = "<html>List of constraints in the symmetric substitution probability.<br>These constraints overwrite the any other settings only for the specific AP-classes.</html>";
        linePar11 = new JPanel(new GridLayout(2, 2));
        lblPar11 = new JLabel("Symmetry constraints:", SwingConstants.LEFT);
        lblPar11.setPreferredSize(fileLabelSize);
        lblPar11.setToolTipText(toolTipPar11); 
        tabModPar11 = new DefaultTableModel();
        tabModPar11.setColumnCount(2);
        tabModPar11.addTableModelListener(tabFieldChange);
        mapKeyFieldToValueField.put(keyPar11.toUpperCase(),tabModPar11);
        tabPar11 = new JTable(tabModPar11);
        tabPar11.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        btnPar11Insert = new JButton("Add Constraint");
        btnPar11Insert.setToolTipText("Click to choose an AP-Class from the current lists of scaffolds and fragments.");
        btnPar11Insert.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		
        		// WARNING: here we use the compatibility matrix file to get all the APClasses
        		// This assumes that the libraries of fragments (scaffolds, frags, and capping groups) 
        		// are consistent with the compatibility matrix file.
        		
        		boolean apClassesFromCPMap = false;
        		Object[] allAPClasses = new Object[]{};
        		try
        		{
        			FragmentSpace.importCompatibilityMatrixFromFile(txtCPMat.getText());
        			allAPClasses = new Object[FragmentSpace.getAllAPClassesFromCPMap().size()];
        			FragmentSpace.getAllAPClassesFromCPMap().toArray(allAPClasses);
        			apClassesFromCPMap=true;
        		}
        		catch (Throwable t)
        		{
        			JOptionPane.showMessageDialog(null,
        					"<html>The current parameters do not create a valid fragment space.<br>"
        					+ "It looks like you want to manually insert APClasses.<br>"
        					+ "If this is not true, you must adjust the parameters defining the fragment space.</html>",
        					"No valid FragmentSpace",
        					JOptionPane.WARNING_MESSAGE);
        		}
        		
        		if (apClassesFromCPMap)
        		{        		
	        		boolean done = false;
	        		while (!done)
	        		{
		        		String apClass = (String)JOptionPane.showInputDialog(
		        		                    null,
		        		                    "Choose attachment point class:",
		        		                    "apClass",
		        		                    JOptionPane.PLAIN_MESSAGE,
		        		                    null,
		        		                    allAPClasses,
		        		                    null);
		
		        		if ((apClass != null) && (apClass.length() > 0)) 
		        		{  
		        			boolean goodChoice = true;
		        			if (tabPar11.getRowCount() > 0) 
		        			{
		        				for (int i=0; i<tabPar11.getRowCount(); i++)
		        				{
		        					if (apClass.equals(tabPar11.getValueAt(i, 0)))
		        					{
		        						JOptionPane.showMessageDialog(null, "<html>apClass already in the table.<br>Choose another class.", "Duplicate apClass", JOptionPane.ERROR_MESSAGE);
		        						goodChoice = false;
		        						break;
		        					}
		        				}
		        			}
		        			if (goodChoice)
		        			{
		        				tabModPar11.addRow(new Object[]{apClass, 1.00});
		        				done = true;
		        			}
		        		}
		        		if (apClass == null)
		        		{
		        			done =true;
		        		}
	        		}
        		}
        		else
        		{
        			boolean done = false;
	        		while (!done)
	        		{
		        		String apClass = (String)JOptionPane.showInputDialog(
			                    null,
			                    "<html>No fragment library found.<br>Type an attachment point class:</html>",
			                    "apClass",
			                    JOptionPane.PLAIN_MESSAGE);
	
						if ((apClass != null) && (apClass.length() > 0)) 
						{  
							boolean goodChoice = true;
							if (tabPar11.getRowCount() > 0) 
							{
								for (int i=0; i<tabPar11.getRowCount(); i++)
								{
									if (apClass.equals(tabPar11.getValueAt(i, 0)))
									{
										JOptionPane.showMessageDialog(null, "<html>apClass already in the table.<br>Choose another class.", "Duplicate apClass", JOptionPane.ERROR_MESSAGE);
										goodChoice = false;
										break;
									}
								}
							}
							if (goodChoice)
							{
								tabModPar11.addRow(new Object[]{apClass, 1.00});
							}
						}
						if (apClass == null)
		        		{
		        			done =true;
		        		}
	        		}
        		}
        	}
        });
        btnPar11Cleanup = new JButton("Remove Selected");
        btnPar11Cleanup.setToolTipText("Remove all selected entries from list.");
        btnPar11Cleanup.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		if (tabPar11.getRowCount() > 0) 
        		{
        	        if (tabPar11.getSelectedRowCount() > 0) 
        	        {
        	            int selectedRowIds[] = tabPar11.getSelectedRows();
        	            Arrays.sort(selectedRowIds);
        	            for (int i=(selectedRowIds.length-1); i>-1; i--) 
        	            {
        	            	tabModPar11.removeRow(selectedRowIds[i]);
        	            }
        	        }
        	    }
        	}
        }); 
        GroupLayout grpLyoPar11 = new GroupLayout(linePar11);
        linePar11.setLayout(grpLyoPar11);
        grpLyoPar11.setAutoCreateGaps(true);
        grpLyoPar11.setAutoCreateContainerGaps(true);
		grpLyoPar11.setHorizontalGroup(grpLyoPar11.createSequentialGroup()
			.addComponent(lblPar11)
			.addGroup(grpLyoPar11.createParallelGroup()
				.addGroup(grpLyoPar11.createSequentialGroup()
						.addComponent(btnPar11Insert)
						.addComponent(btnPar11Cleanup))
				.addComponent(tabPar11))
		);
		grpLyoPar11.setVerticalGroup(grpLyoPar11.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addComponent(lblPar11)
			.addGroup(grpLyoPar11.createSequentialGroup()
				.addGroup(grpLyoPar11.createParallelGroup()
					.addComponent(btnPar11Insert)
					.addComponent(btnPar11Cleanup))
				.addComponent(tabPar11))
		);
        localBlock2.add(linePar11);
        
        String toolTipPar12 = "<html>Activates search and handling of rings of fragments.</html>";
        linePar12 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rdbPar12 = new JRadioButton("Enable cyclic graphs (create rings of fragments)");
        rdbPar12.setToolTipText(toolTipPar12);
        rdbPar12.addChangeListener(rdbFieldChange);
        mapKeyFieldToValueField.put(keyPar12.toUpperCase(),rdbPar12);
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
        txtPar5.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar5.toUpperCase(),txtPar5);
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
        txtPar15.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar15.toUpperCase(),txtPar15);
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
        txtPar16.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar16.toUpperCase(),txtPar16);
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
        txtPar17.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar17.toUpperCase(),txtPar17);
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
        txtPar18.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar18.toUpperCase(),txtPar18);
        linePar18.add(lblPar18);
        linePar18.add(txtPar18);
        localBlock3.add(linePar18);

        String toolTipPar22 = "<html>Specifies the bias associated to a given ring size.<br> The bias is used when chosing among combination of rings (i.e., RCAs) for a given graph.</html>";
        linePar22 = new JPanel(new FlowLayout(FlowLayout.LEFT));      
        lblPar22 = new JLabel("Ring size preference biases:", SwingConstants.LEFT);
        lblPar22.setPreferredSize(fileLabelSize);
        lblPar22.setToolTipText(toolTipPar22);
        tabModPar22 = new DefaultTableModel();
        tabModPar22.setColumnCount(2);
        tabModPar22.addTableModelListener(tabFieldChange);
        mapKeyFieldToValueField.put(keyPar22.toUpperCase(),tabModPar22);
        tabPar22 = new JTable(tabModPar22);
        tabPar22.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        btnPar22Insert = new JButton("Add Bias");
        btnPar22Insert.setToolTipText("Click to set a new ring size bias.");
        btnPar22Insert.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		boolean done = false;
        		while (!done)
        		{
	        		String rsBias = (String)JOptionPane.showInputDialog(
		                    null,
		                    "Specify ring size:",
		                    "Ring Size",
		                    JOptionPane.PLAIN_MESSAGE);
	
					if ((rsBias != null) && (rsBias.length() > 0)) 
					{  
						boolean goodChoice = true;
						if (tabPar22.getRowCount() > 0) 
						{
							for (int i=0; i<tabPar22.getRowCount(); i++)
							{
								if (rsBias.equals(tabPar22.getValueAt(i, 0)))
								{
									JOptionPane.showMessageDialog(null, "<html>Rins size already in the table.<br>Choose another size or modify the value of the bias in the table.</html>", "Duplicate Entry", JOptionPane.ERROR_MESSAGE);
									goodChoice = false;
									break;
								}
							}
						}
						if (goodChoice)
						{
		        			if (tabPar22.getRowCount() == 0)
		        			{
		        				tabModPar22.addRow(new Object[]{"<html><b>Ring Size</b></html>", "<html><b>Bias</b></html>"});
		        			}
							tabModPar22.addRow(new Object[]{rsBias, 1.00});
							done = true;
						}
					}
					if (rsBias == null)
	        		{
	        			done =true;
	        		}
				}       		
        	}
        });
        btnPar22Cleanup = new JButton("Remove Selected");
        btnPar22Cleanup.setToolTipText("Remove all selected entries from list.");
        btnPar22Cleanup.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		if (tabPar22.getRowCount() > 1) 
        		{
        	        if (tabPar22.getSelectedRowCount() > 0) 
        	        {
        	            int selectedRowIds[] = tabPar22.getSelectedRows();
        	            Arrays.sort(selectedRowIds);
        	            for (int i=(selectedRowIds.length-1); i>-1; i--) 
        	            {
        	            	if (selectedRowIds[i] != 0)
        	            	{
        	            		tabModPar22.removeRow(selectedRowIds[i]);
        	            	}
        	            }
        	            if (tabPar22.getRowCount() == 1)
        	            {
        	            	tabModPar22.removeRow(0);
        	            }
        	        }
        	    }
        	}
        });
        
        GroupLayout grpLyoPar22 = new GroupLayout(linePar22);
        linePar22.setLayout(grpLyoPar22);
        grpLyoPar22.setAutoCreateGaps(true);
        grpLyoPar22.setAutoCreateContainerGaps(true);
		grpLyoPar22.setHorizontalGroup(grpLyoPar22.createSequentialGroup()
			.addComponent(lblPar22)
			.addGroup(grpLyoPar22.createParallelGroup()
				.addGroup(grpLyoPar22.createSequentialGroup()
						.addComponent(btnPar22Insert)
						.addComponent(btnPar22Cleanup))
				.addComponent(tabPar22))
		);
		grpLyoPar22.setVerticalGroup(grpLyoPar22.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addComponent(lblPar22)
			.addGroup(grpLyoPar22.createSequentialGroup()
				.addGroup(grpLyoPar22.createParallelGroup()
					.addComponent(btnPar22Insert)
					.addComponent(btnPar22Cleanup))
				.addComponent(tabPar22))
		);
		
        localBlock3.add(linePar22);

        String toolTipPar23 = "Specifies the maximum number of ring members for rings created from scratch";
        linePar23 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar23 = new JLabel("Max. size of new rings:", SwingConstants.LEFT);
        lblPar23.setPreferredSize(fileLabelSize);
        lblPar23.setToolTipText(toolTipPar23);
        txtPar23 = new JTextField();
        txtPar23.setToolTipText(toolTipPar23);
        txtPar23.setPreferredSize(strFieldSize);
        txtPar23.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar23.toUpperCase(),txtPar23);
        linePar23.add(lblPar23);
        linePar23.add(txtPar23);
        localBlock3.add(linePar23);

        String toolTipPar19 = "Defined the closability condition's evaluation mode.";
        linePar19 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar19 = new JLabel("Type of ring-closability conditions", SwingConstants.LEFT);
        lblPar19.setPreferredSize(fileLabelSize);
        lblPar19.setToolTipText(toolTipPar19);
        cmbPar19 = new JComboBox<String>(new String[] {"Constitution", "3D-Conformation", "Constitution_and_3D-Conformation"});
        cmbPar19.setToolTipText(toolTipPar19);
        cmbPar19.addActionListener(cmbFieldChange);
        mapKeyFieldToValueField.put(keyPar19.toUpperCase(),cmbPar19);
        cmbPar19.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		switch (cmbPar19.getSelectedItem().toString())
        		{
        			case "Constitution":
        				localBlock4.setVisible(true);
            			localBlock5.setVisible(false);   
            			break;
            			
        			case "3D-Conformation":
        				localBlock4.setVisible(false);
            			localBlock5.setVisible(true); 
            			break;
            			
        			case "Constitution_and_3D-Conformation":
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

        String toolTipPar21 = "Specifies the constitutional ring closability conditions by SMARTS string.";
        linePar21 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar21 = new JLabel("Constitutional ring-closability rules:", SwingConstants.LEFT);
        lblPar21.setPreferredSize(fileLabelSize);
        lblPar21.setToolTipText(toolTipPar21);
        tabModPar21 = new DefaultTableModel();
        tabModPar21.setColumnCount(1);
        tabModPar21.addTableModelListener(tabFieldChange);
        mapKeyFieldToValueField.put(keyPar21.toUpperCase(),tabModPar21);
        tabPar21 = new JTable(tabModPar21);
        tabPar21.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        btnPar21Insert = new JButton("Add SMARTS");
        btnPar21Insert.setToolTipText("Click to set a new ring size bias.");
        btnPar21Insert.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		boolean done = false;
        		while (!done)
        		{
	        		String rcRule = (String)JOptionPane.showInputDialog(
		                    null,
		                    "Specify ring-closability rules:",
		                    "Add Constitutional Closability Rule",
		                    JOptionPane.PLAIN_MESSAGE);
	
					if ((rcRule != null) && (rcRule.length() > 0)) 
					{  
						boolean goodChoice = true;
						if (tabPar21.getRowCount() > 0) 
						{
							for (int i=0; i<tabPar21.getRowCount(); i++)
							{
								if (rcRule.equals(tabPar21.getValueAt(i, 0)))
								{
									JOptionPane.showMessageDialog(null, "<html>Rule already in the table.</html>", "Duplicate Entry", JOptionPane.ERROR_MESSAGE);
									goodChoice = false;
									break;
								}
							}
						}
						if (goodChoice)
						{
							tabModPar21.addRow(new Object[]{rcRule});
							done = true;
						}
					}
					if (rcRule == null)
	        		{
	        			done =true;
	        		}
				}       		
        	}
        });
        btnPar21Cleanup = new JButton("Remove Selected");
        btnPar21Cleanup.setToolTipText("Remove all selected entries from list.");
        btnPar21Cleanup.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		if (tabPar21.getRowCount() > 0) 
        		{
        	        if (tabPar21.getSelectedRowCount() > 0) 
        	        {
        	            int selectedRowIds[] = tabPar21.getSelectedRows();
        	            Arrays.sort(selectedRowIds);
        	            for (int i=(selectedRowIds.length-1); i>-1; i--) 
        	            {
        	            	tabModPar21.removeRow(selectedRowIds[i]);
        	            }
        	        }
        	    }
        	}
        });
        GroupLayout grpLyoPar21 = new GroupLayout(linePar21);
        linePar21.setLayout(grpLyoPar21);
        grpLyoPar21.setAutoCreateGaps(true);
        grpLyoPar21.setAutoCreateContainerGaps(true);
        grpLyoPar21.setHorizontalGroup(grpLyoPar21.createSequentialGroup()
                .addComponent(lblPar21)
                .addGroup(grpLyoPar21.createParallelGroup()
                        .addGroup(grpLyoPar21.createSequentialGroup()
                                        .addComponent(btnPar21Insert)
                                        .addComponent(btnPar21Cleanup))
                        .addComponent(tabPar21))
        );
        grpLyoPar21.setVerticalGroup(grpLyoPar21.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(lblPar21)
                .addGroup(grpLyoPar21.createSequentialGroup()
                        .addGroup(grpLyoPar21.createParallelGroup()
                                .addComponent(btnPar21Insert)
                                .addComponent(btnPar21Cleanup))
                        .addComponent(tabPar21))
        );
        localBlock4.add(linePar21);

        String toolTipPar25 = "Specifies the maximum number of rotatable bonds for which 3D chain closability is evaluated. Chains with a number of rotatable bonds higher than this value are assumed closable.";
        linePar25 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar25 = new JLabel("Max. rotatable bonds:", SwingConstants.LEFT);
        lblPar25.setPreferredSize(fileLabelSize);
        lblPar25.setToolTipText(toolTipPar25);
        txtPar25 = new JTextField();
        txtPar25.setToolTipText(toolTipPar25);
        txtPar25.setPreferredSize(strFieldSize);
        txtPar25.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar25.toUpperCase(),txtPar25);
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
        txtPar26.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar26.toUpperCase(),txtPar26);
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
        txtPar27.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar27.toUpperCase(),txtPar27);
        linePar27.add(lblPar27);
        linePar27.add(txtPar27);
        localBlock5.add(linePar27);
        
        String toolTipPar24 = "<html>Requires evaluation of interdependent closability condition.<br><b>WARNING:</b> this function require exhaustive conformational search, which is very time consuming.</html>";
        linePar24 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rdbPar24 = new JRadioButton("Check interdependent chain closability.");
        rdbPar24.setToolTipText(toolTipPar24);
        rdbPar24.addChangeListener(rdbFieldChange);
        mapKeyFieldToValueField.put(keyPar24.toUpperCase(),rdbPar24);
        linePar24.add(rdbPar24);
        localBlock5.add(linePar24);

        String toolTipPar28 = "<html>Requires the search for closable conformations to explore the complete rotational space.<br><b>WARNING:</b> this is very time consuming, but is currently needed to evaluate closability of interdependent chains.</html>";
        linePar28 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rdbPar28 = new JRadioButton("Extensive chain conformational search:");
        rdbPar28.setToolTipText(toolTipPar28);
        rdbPar28.addChangeListener(rdbFieldChange);
        mapKeyFieldToValueField.put(keyPar28.toUpperCase(),rdbPar28);
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
        txtPar30.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar30.toUpperCase(),txtPar30);
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
        txtPar31.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar31.toUpperCase(),txtPar31);
        btnPar31 = new JButton("Browse");
        btnPar31.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
                DenoptimGUIFileOpener.pickFolder(txtPar31);
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
        txtPar32.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar32.toUpperCase(),txtPar32);
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
        txtPar33.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar33.toUpperCase(),txtPar33);
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
        txtPar34.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar34.toUpperCase(),txtPar34);
        linePar34.add(lblPar34);
        linePar34.add(txtPar34);
        localBlock5.add(linePar34);

        //HEREGOESADVIMPLEMENTATION this is only to facilitate automated insertion of code    
        
        this.add(scrollablePane);
    }
   
//-----------------------------------------------------------------------------
    
    /**
     * Imports parameters from a properly formatted parameters file.
     * The file is a text file with lines containing KEY=VALUE pairs.
     * @param fileName the pathname of the file to read
     * @throws Exception
     */
    
    @Override
    public void importParametersFromDenoptimParamsFile(String fileName) throws Exception
    {
    	importParametersFromDenoptimParamsFile(fileName,"FS-");
		importParametersFromDenoptimParamsFile(fileName,"RC-");
		
    	rdbSrcOrNew.setSelected(false);
    	localBlock1.setVisible(false);
		localBlock2.setVisible(true);
		if (rdbPar12.isSelected())
		{
			localBlock3.setVisible(true);
		}
		switch (cmbPar19.getSelectedItem().toString())
		{
			case "Constitution":
				localBlock4.setVisible(true);
    			localBlock5.setVisible(false);   
    			break;
    			
			case "3D-Conformation":
				localBlock4.setVisible(false);
    			localBlock5.setVisible(true); 
    			break;
    			
			case "Constitution_and_3D-Conformation":
				localBlock4.setVisible(true);
    			localBlock5.setVisible(true);   
    			break;
		}
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
					"<html>Parameter '" + key + "' is not recognized.<br>Ignoring line.</html>",
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

    			if (key.equals(keyPar22) && (((DefaultTableModel) valueField).getRowCount() == 0)) 
    			{
    				((DefaultTableModel) valueField).addRow(new Object[]{"<html><b>Ring Size</b></html>", "<html><b>Bias</b></html>"});
    			}

 				((DefaultTableModel) valueField).addRow(value.split(" "));
 				break;
 				
 			default:
 				throw new Exception("<html>Unexpected type for parameter: "  
 						+ key + " (" + valueFieldClass 
 						+ ").<br>Please report this to"
 						+ "the DEMOPTIM team.</html>");
 		}
 		
	}
  	
//-----------------------------------------------------------------------------
  	
  	public void possiblyReadParamsFromFSParFile() throws Exception
  	{
  		if (rdbSrcOrNew.isSelected())
        {
	    	if (txtFSSource.getText().equals("") 
	    			|| txtFSSource.getText() == null)
	    	{
	    		throw new Exception("<html>No source specified for fragment "
	    				+ "space.<br>Please, specify the file name.</html>");
	    	}
	    	importParametersFromDenoptimParamsFile(txtFSSource.getText());
        }
  	}
  	
//-----------------------------------------------------------------------------

    @Override
    public void putParametersToString(StringBuilder sb) throws Exception
    {
    	possiblyReadParamsFromFSParFile();
    	
        sb.append("# Fragment Space - paramerers").append(NL);
        sb.append(getStringIfNotEmpty(keyPar1,txtPar1));
        sb.append(getStringIfNotEmpty(keyPar2,txtPar2));
        sb.append(getStringIfNotEmpty(keyPar3,txtPar3));
        sb.append(getStringIfNotEmpty(keyCPMat,txtCPMat));
        sb.append(getStringIfNotEmpty(keyPar6,txtPar6));
        sb.append(getStringIfNotEmpty(keyPar7,txtPar7));
        sb.append(getStringIfNotEmpty(keyPar8,txtPar8));
        sb.append(getStringIfNotEmpty(keyPar9,txtPar9));
        sb.append(getStringIfSelected(keyPar10,rdbPar10));
        sb.append(getStringFromTable(keyPar11,tabPar11, new int[]{0,1},false));
        if (rdbPar12.isSelected())
        {
            sb.append(getStringIfSelected(keyPar12,rdbPar12));
            sb.append(getStringIfNotEmpty(keyPar5,txtPar5));
	        sb.append(getStringIfNotEmpty(keyPar15,txtPar15));
	        sb.append(getStringIfNotEmpty(keyPar16,txtPar16));
	        sb.append(getStringIfNotEmpty(keyPar17,txtPar17));
	        sb.append(getStringIfNotEmpty(keyPar18,txtPar18));
	        sb.append(getStringFromTable(keyPar22,tabPar22, new int[]{0,1},true));
	        sb.append(getStringIfNotEmpty(keyPar23,txtPar23));
	        sb.append(keyPar19).append("=").append(cmbPar19.getSelectedItem()).append(NL);
	        switch (cmbPar19.getSelectedItem().toString())
    		{
    			case "Constitution":
    				sb.append(getStringFromTable(keyPar21,tabPar21, new int[]{0},false));
        			break;
        			
    			case "3D-Conformation":
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
        			break;
        			
    			case "Constitution_and_3D-Conformation":
    				sb.append(getStringFromTable(keyPar21,tabPar21, new int[]{0},false));
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
        			break;
    		}
        }
        //HEREGOESPRINT this is only to facilitate automated insertion of code  
    }
}
