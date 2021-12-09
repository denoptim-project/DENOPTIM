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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.openscience.cdk.fingerprint.BitSetFingerprint;
import org.openscience.cdk.fingerprint.IBitFingerprint;
import org.openscience.cdk.fingerprint.IFingerprinter;
import org.openscience.cdk.qsar.IDescriptor;

import denoptim.exception.DENOPTIMException;
import denoptim.fitness.DescriptorForFitness;
import denoptim.fitness.DescriptorUtils;

/**
 * Form collecting input parameters for a setting-up the fitness provider.
 */

public class FitnessParametersForm extends ParametersForm
{

    /**
	 * Version
	 */
	private static final long serialVersionUID = -282726238111247056L;
	
	/**
	 * Unique identified for instances of this form
	 */
	public static AtomicInteger fitFormUID = new AtomicInteger(1);
	
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
	
	JPanel lineSrcOrNew;
    JRadioButton rdbSrcOrNew;
    
    String key3dTrees = "FP-No3dTreeModel";
    JPanel line3dTrees;
    JRadioButton rdb3dTrees;
    
    String keyPreFitnessUIDCheck = "FP-CheckUidBeforeFitness";
    JPanel linePreFitnessUIDCheck;
    JRadioButton rdbPreFitnessUIDCheck;
    
    JPanel lineFPSource;
    JLabel lblFPSource;
    JTextField txtFPSource;
    JButton btnFPSource;
    JButton btnLoadFPSource;
	
	JPanel lineIntOrExt;
    JRadioButton rdbIntOrExt;

    String keyFitProviderSource = "FP-Source";
    JPanel lineFitProviderSource;
    JLabel lblFitProviderSource;
    JTextField txtFitProviderSource;
    JButton btnFitProviderSource;

    String keyFitProviderInterpreter = "FP-Interpreter";
    JPanel lineFitProviderInterpreter;
    JLabel lblFitProviderInterpreter;
    JComboBox<String> cmbFitProviderInterpreter;

    String keyEq = "FP-Equation";
    JPanel lineEq;
    JLabel lblEq;
    JTextField txtEq;
    
    String keyMoreEq = "FP-DescriptorSpecs";
    JPanel lineMoreEq;
    JLabel lblMoreEq;
    JTable tabMoreEq;
    DefaultTableModel tabMoreEqMod;
    
    JPanel descDefinitionPane;
    JScrollPane descDefScrollPane;
    JLabel lblDDValueTitle;
    JLabel lblDDValueName;
    JLabel lblDDValueDefinition;
    JLabel lblDDValueDescripton;
    JLabel lblDDValueClasses;
    JPanel pnlDDValueParams;
    JLabel lblDDValueParams;
    JButton btnDDValueParams;
    private String descNameToTune = "";
    private String[] paramsToTune;
    JLabel lblDDValueSource;

    //HEREGOFIELDS  this is only to facilitate automated insertion of code
        
    private static  Map<Class<?>,String> additionalDocForParameters;
    static {
        additionalDocForParameters = new HashMap<Class<?>,String>();
        Class<?> key = IBitFingerprint.class;
        additionalDocForParameters.put(key,
                "<p>The <code>" + key.getSimpleName() + "</code> parameter can "
                + "be generated "
                + "when importing the descriptor into the fitness provider. "
                + "To this end, use the prefix '<code>FILE:</code>' to "
                + "provide a pathname to an SDF file from which the "
                + "reference molecule can be fetched, and its fingerprint "
                + "calculated and finally used as parameter to configure "
                + "this descriptor.</p>");
        key = IFingerprinter.class;
        additionalDocForParameters.put(key,
                "<p>The <code>" + key.getSimpleName() 
                + "</code> parameter should be specified "
                + "as the simple name of the desired implementation. For "
                + "example <code>PubchemFingerprinter</code>. See "
                + "CDK documentation on IFingerprinter for available "
                + "implementations.</p>");
    }       
        
    String NL = System.getProperty("line.separator");
    
    public FitnessParametersForm(Dimension d)
    {
    	mapKeyFieldToValueField = new HashMap<String,Object>();
    	
        this.setLayout(new BorderLayout()); //Needed to allow dynamic resizing!

        block = new JPanel();
        JScrollPane scrollablePane = new JScrollPane(block);
        block.setLayout(new BoxLayout(block, SwingConstants.VERTICAL));    

        localBlock1 = new JPanel();
        localBlock1.setVisible(false);
        localBlock1.setLayout(new BoxLayout(localBlock1, 
        		SwingConstants.VERTICAL));
        
        localBlock2 = new JPanel();
        localBlock2.setVisible(true);
        localBlock2.setLayout(new BoxLayout(localBlock2, 
        		SwingConstants.VERTICAL));
        
        localBlock3 = new JPanel();
        localBlock3.setVisible(false);
        localBlock3.setLayout(new BoxLayout(localBlock3, 
        		SwingConstants.VERTICAL));
        
        localBlock4 = new JPanel();
        localBlock4.setVisible(true);
        localBlock4.setLayout(new BoxLayout(localBlock4, 
        		SwingConstants.VERTICAL));
        
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
        
        String toolTipFPSource = "<html>Pathname of a DENOPTIM's parameter "
        		+ "file with fitness-provider settings.</html>";
        lineFPSource = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblFPSource = new JLabel("Use parameters from file:", 
        		SwingConstants.LEFT);
        lblFPSource.setToolTipText(toolTipFPSource);
        txtFPSource = new JTextField();
        txtFPSource.setToolTipText(toolTipFPSource);
        txtFPSource.setPreferredSize(fileFieldSize);
        btnFPSource = new JButton("Browse");
        btnFPSource.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
                GUIFileOpener.pickFileForTxtField(txtFPSource,
                		btnFPSource);
           }
        });
        btnLoadFPSource = new JButton("Load...");
        txtFPSource.setToolTipText("<html>Specify the file containing the "
        		+ "parameters to be loaded in this form.</html>");
        btnLoadFPSource.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
	        	try 
	        	{
					importParametersFromDenoptimParamsFile(
							txtFPSource.getText());
				} 
	        	catch (Exception e1) 
	        	{
	        		if (e1.getMessage().equals("") || e1.getMessage() == null)
	        		{
	        			e1.printStackTrace();
						JOptionPane.showMessageDialog(btnLoadFPSource,
								"<html>Exception occurred while importing "
								+ "parameters.<br>Please, report this to the "
								+ "DENOPTIM team.</html>",
				                "Error",
				                JOptionPane.ERROR_MESSAGE,
				                UIManager.getIcon("OptionPane.errorIcon"));
	        		}
	        		else
	        		{
						JOptionPane.showMessageDialog(btnLoadFPSource,
								e1.getMessage(),
				                "Error",
				                JOptionPane.ERROR_MESSAGE,
				                UIManager.getIcon("OptionPane.errorIcon"));
	        		}
					return;
				}
            }
        });
        lineFPSource.add(lblFPSource);
        lineFPSource.add(txtFPSource);
        lineFPSource.add(btnFPSource);
        lineFPSource.add(btnLoadFPSource);
        localBlock1.add(lineFPSource);
        
        
        String toolTip3dTrees = "<html>Tick here to enable/disable preliminary "
        		+ "generation of a unrefined<br>3D geometry prior to fitness "
        		+ "evaluation.<br>These models are build by aligning the given "
        		+ "3D fragments<br> to the attachment point vectors. "
        		+ "These models are <br>not energy-refined.</html>";
        line3dTrees = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rdb3dTrees = new JRadioButton("Make unrefined 3d model.");
        rdb3dTrees.setToolTipText(toolTip3dTrees);
        rdb3dTrees.setSelected(true);
        mapKeyFieldToValueField.put(key3dTrees.toUpperCase(), rdb3dTrees);
        line3dTrees.add(rdb3dTrees);
        localBlock2.add(line3dTrees);
        
        String toolTipPreFitnessUIDCheck = "<html>Tick here to enable/disable evaluation "
                + "of candidate uniqueness prior to submission of the fitness "
                + "evaluation.</html>";
        linePreFitnessUIDCheck = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rdbPreFitnessUIDCheck = new JRadioButton("Check candidate uniqueness "
                + "before sending to fitness evaluation.");
        rdbPreFitnessUIDCheck.setToolTipText(toolTipPreFitnessUIDCheck);
        rdbPreFitnessUIDCheck.setSelected(true);
        mapKeyFieldToValueField.put(keyPreFitnessUIDCheck.toUpperCase(), rdbPreFitnessUIDCheck);
        linePreFitnessUIDCheck.add(rdbPreFitnessUIDCheck);
        localBlock2.add(linePreFitnessUIDCheck);
        

        String toolTipIntOrExt = "<html>A fitness provider is an existing "
        		+ "tool or script.<br> The fitness provider must produce an "
        		+ "output SDF file with the <code>&lt;FITNESS&gt;</code> or "
        		+ "<code>&lt;MOL_ERROR&gt;</code> tags.</html>";
        lineIntOrExt = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rdbIntOrExt = new JRadioButton("Use external fitnes provider");
        rdbIntOrExt.setToolTipText(toolTipIntOrExt);
        
        rdbIntOrExt.setSelected(false);
        rdbIntOrExt.setEnabled(true);
        
        rdbIntOrExt.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		setVisibilityAccordingToFitnessProviderSelection();
        	}
        });
        lineIntOrExt.add(rdbIntOrExt);
        localBlock2.add(lineIntOrExt);
        localBlock2.add(localBlock3);
        localBlock2.add(localBlock4);

        //HEREGOESIMPLEMENTATION this is only to facilitate automated insertion of code

        String toolTipFitProviderSource = "Pathname of the executable file.";
        lineFitProviderSource = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblFitProviderSource = new JLabel("Fitness provider executable:", 
        		SwingConstants.LEFT);
        lblFitProviderSource.setPreferredSize(fileLabelSize);
        lblFitProviderSource.setToolTipText(toolTipFitProviderSource);
        txtFitProviderSource = new JTextField();
        txtFitProviderSource.setToolTipText(toolTipFitProviderSource);
        txtFitProviderSource.setPreferredSize(fileFieldSize);
        txtFitProviderSource.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyFitProviderSource.toUpperCase(), 
        		txtFitProviderSource);
        btnFitProviderSource = new JButton("Browse");
        btnFitProviderSource.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
                GUIFileOpener.pickFileForTxtField(
                		txtFitProviderSource, btnFitProviderSource);
           }
        });
        lineFitProviderSource.add(lblFitProviderSource);
        lineFitProviderSource.add(txtFitProviderSource);
        lineFitProviderSource.add(btnFitProviderSource);
        localBlock3.add(lineFitProviderSource);
        
        String toolTipFitProviderInterpreter = "Interpreter to be used for the "
        		+ "fitness provider executable";
        lineFitProviderInterpreter = new JPanel(
        		new FlowLayout(FlowLayout.LEFT));
        lblFitProviderInterpreter = new JLabel("Interpreter for fitnes "
        		+ "provider", SwingConstants.LEFT);
        lblFitProviderInterpreter.setPreferredSize(fileLabelSize);
        lblFitProviderInterpreter.setToolTipText(toolTipFitProviderInterpreter);
        cmbFitProviderInterpreter = new JComboBox<String>(new String[] {"bash",
        		"python"});
        cmbFitProviderInterpreter.setToolTipText(toolTipFitProviderInterpreter);
        cmbFitProviderInterpreter.setEnabled(true);
        
        mapKeyFieldToValueField.put(keyFitProviderInterpreter.toUpperCase(), 
        		cmbFitProviderInterpreter);
        lineFitProviderInterpreter.add(lblFitProviderInterpreter);
        lineFitProviderInterpreter.add(cmbFitProviderInterpreter);
        localBlock3.add(lineFitProviderInterpreter);

        String toolTipEq = "Define integrated fitness provider expression.";
        lineEq = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblEq = new JLabel("<html><i>Fitness = </i>${</html>", 
        		SwingConstants.LEFT);
        JLabel lblEqEnd = new JLabel("<html>}</html>", SwingConstants.LEFT);
        //lblEq.setPreferredSize(fileLabelSize);
        lblEq.setToolTipText(toolTipEq);
        txtEq = new JTextField();
        txtEq.setToolTipText("<html>Type here the expression for computing the fitness value out of predefined<br>"
        		+ "descriptors and custom variables.<ul>"
        		+ "<li>Descriptors can be selected from the 'Available descriptors' section (below).</li>"
        		+ "<li>Custom variables, including atom-specific descriptors, can be defined in <br>"
        		+ "the 'Custom variables' section.</li></ul></html>");
        Dimension fitEqSize = new Dimension(700, 2*preferredHeight);
        txtEq.setPreferredSize(fitEqSize);
        txtEq.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyEq.toUpperCase(),txtEq);
        lineEq.add(lblEq);
        lineEq.add(txtEq);
        lineEq.add(lblEqEnd);
        localBlock4.add(lineEq);
        
        JLabel lblDescTreeTitle = new JLabel("Available descriptors:");
        JPanel lineDescTreeTitle = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lineDescTreeTitle.add(lblDescTreeTitle);
        localBlock4.add(lineDescTreeTitle);
        
        String toolTipDescs = "<html>To select descriptor names:"
        		+ "<ol><li>Browse the list of descriptors (double click to expand/reduce a node),"
        		+ "</li><li>Click on the name of the descriptor you want to select,"
        		+ "</li><li>Copy the selected name (<code>ctrl+C</code>/<code>command+C</code>), click in the fitness<br>"
        		+ "expression field, and paste (<code>ctrl+V</code><code>command+V</code>).</li>"
        		+ "</ol></html>";
        
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(
        		"Descriptors");
        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
        JTree descTree = new JTree(treeModel);
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setLeafIcon(null);
        descTree.setCellRenderer(renderer);
        descTree.setRootVisible(true);
        descTree.setShowsRootHandles(true);
        //NB: appearance depends on lookAndfeel. On Mac no lines, even if asked
        descTree.putClientProperty("JTree.lineStyle", "Angled");
        descTree.setToolTipText(toolTipDescs);
        int dValLength = 350;
		descTree.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				TreePath selPath = e.getPath();
				DefaultMutableTreeNode selTreeNode = (DefaultMutableTreeNode) 
						selPath.getLastPathComponent();
				if (!(selTreeNode instanceof DescriptorTreeNode)
						|| !selTreeNode.isLeaf())
				{
					return;
				}
				DescriptorTreeNode selNode = (DescriptorTreeNode) selTreeNode;
				DescriptorForFitness dff = selNode.dff;

				String titStr = dff.getDictTitle();
				titStr = "<html><body width='%1s'>" + titStr + "</body></html>";
				lblDDValueTitle.setText(String.format(titStr, dValLength));
				lblDDValueName.setText(dff.getShortName());
				
				String defStr = dff.getDictDefinition();
				defStr = "<html><body width='%1s'>" + defStr + "</body></html>";
				lblDDValueDefinition.setText(String.format(defStr, dValLength));
				
				lblDDValueDescripton.setText("No additional descripton available");

				String clsStr = Arrays.toString(dff.getDictClasses());
				clsStr = "<html><body width='%1s'>" + clsStr + "</body></html>";
				lblDDValueClasses.setText(String.format(clsStr, dValLength));
				
				String parStr ="";
				IDescriptor iDesc = dff.getImplementation();
				String[] parNames = iDesc.getParameterNames();
				Object[] params = iDesc.getParameters();
				//NB: some classes return null to avoid adding a way to alter
				// the default parameters.
				ArrayList<Object> paramsTypes = new ArrayList<Object>();
				if (params == null)
				{
					parStr = "Undeclared parameters. See source code.";
				} else {
					for (int ip=0; ip<parNames.length; ip++)
					{
						if (ip>0)
						{
							parStr = parStr + "<br>";
						}
						Object parTyp = iDesc.getParameterType(parNames[ip]);
						
						String parTypStr = parTyp.getClass().getSimpleName();
						if (parTyp instanceof Class<?>)
						{
						    parTypStr = ((Class<?>) iDesc.getParameterType(
                                    parNames[ip])).getSimpleName();
						}
						parStr = parStr + parNames[ip] + " = (" 
						        + parTypStr + ") " + params[ip];
						paramsTypes.add(parTyp);
					}
				}
                descNameToTune = dff.getShortName();
				if (paramsTypes.size() == 0)
				{
                    btnDDValueParams.setEnabled(false);
                    btnDDValueParams.setVisible(false);
                    paramsToTune = new String[0];
				} else {
				    btnDDValueParams.setEnabled(true);
				    btnDDValueParams.setVisible(true);
				    paramsToTune = parNames;
				}
				for (Object parTypeExample : paramsTypes)
				{
				    String s = additionalDocForParameters.get(parTypeExample);
                    if (s != null)
                    {
                        parStr = parStr + "<p> </p>" + s;
                    }
				}
				parStr = "<html><body width='%1s'>" + parStr + "</body></html>";
				lblDDValueParams.setText(String.format(parStr, dValLength));
				
				String srcStr = dff.getClassName();
				srcStr = "<html><body width='%1s'>" + srcStr + "</body></html>";
				lblDDValueSource.setText(String.format(srcStr, dValLength));
				
			}
		});
        JScrollPane descTreeScrollPane = new JScrollPane(descTree);
        
        String[] sources = new String[] {"CDK", "DENOPTIM"};
        for (String source : sources)
        {
            DescriptorTreeNode sourceNode = new DescriptorTreeNode(source);
            rootNode.add(sourceNode);
            
            List<DescriptorForFitness> allDescs = null;
            try {
                switch (source)
                {
                    case "CDK":
                        allDescs = DescriptorUtils.findAllCDKDescriptors(
                                null);
                        break;
                    case "DENOPTIM":
                        allDescs = DescriptorUtils.findAllDENOPTIMDescriptors(
                                null);
                        break;
                }
            } catch (DENOPTIMException e1) {
                System.out.println("No descriptor implementation found in "
                        + "source '" + source + "'!");
                e1.printStackTrace();
                continue;
            }
        
            // First identify the main klasses (first layer)
            Map<String,DescriptorTreeNode> mainClassificationNodes = 
            		new HashMap<String,DescriptorTreeNode>();
            for (DescriptorForFitness dff : allDescs)
            {	
            	String[] klasses = new String[]{"Unclassified"};
            	if (dff.getDictClasses()!=null)
            	{
            		klasses = dff.getDictClasses();
            	}
            	for (String kls : klasses)
            	{
            		if (!mainClassificationNodes.containsKey(kls))
            		{
            			DescriptorTreeNode klassNode = new DescriptorTreeNode(
            			        kls);
            			sourceNode.add(klassNode);
            			mainClassificationNodes.put(kls, klassNode);
            		}
            	}
            }
        
            // Then populate each class
            for (String klass : mainClassificationNodes.keySet())
            {
            	DescriptorTreeNode klassNode = mainClassificationNodes.get(
            	        klass);
            	Map<String,DescriptorTreeNode> descriptorNodes = 
            			new HashMap<String,DescriptorTreeNode>();
    	        for (DescriptorForFitness dff : allDescs)
    	        {
    	        	//TODO: check if getting the dictionary from 
    	            // DictionaryDatabase allows to get also the description in 
    	            // addition to definition
    	        	
    	        	// Decide if this descriptor goes under the present klass
    	        	List<String> klasses = new ArrayList<String>();
    	        	if (dff.getDictClasses() == null)
    	        	{
    	        		if (!klass.equals("Unclassified"))
    	        		{
    	        			continue;
    	        		} else {
    	        			klasses.add(klass);
    	        		}
    	        	} else {
    	        		klasses = new ArrayList<String>(
    		        			Arrays.asList(dff.getDictClasses()));
    	        	}
    	        	if (!klasses.contains(klass))
    	        	{
    	        		continue;
    	        	}
    	        	
    	        	// Identify parent node: either a klassNode or a descriptorNode
            		DescriptorTreeNode parentNode = klassNode;
            		if (dff.getImplementation().getDescriptorNames().length > 1)
            		{
    	        		String descriptorName = dff.getImplementation().getClass()
    	        				.getSimpleName();
    	        		if (!descriptorNodes.containsKey(descriptorName))
    	        		{
    	        			DescriptorTreeNode descNode = 
    	        			        new DescriptorTreeNode(descriptorName,dff);
    	        			parentNode.add(descNode);
    	        			descriptorNodes.put(descriptorName, descNode);
    	        		}
    	        		parentNode = descriptorNodes.get(descriptorName);
            		}
            		
            		// Finally make the node for the present desc-to-fitness
            		DescriptorTreeNode dtn = new DescriptorTreeNode(dff);
            		parentNode.add(dtn);
    	        }
            }
        }
        
        Dimension ddLabelsSize = new Dimension(100,30);
        JLabel lblDDTitle = new JLabel("<html><b>Title:</b></html>");
        lblDDTitle.setPreferredSize(ddLabelsSize);
        JLabel lblDDName = new JLabel("<html><b>Name:</b></html>");
        lblDDName.setPreferredSize(ddLabelsSize);
        JLabel lblDDDefinition = new JLabel("<html><b>Definition:</b></html>");
        lblDDDefinition.setPreferredSize(ddLabelsSize);
        JLabel lblDDDescription = new JLabel("<html><b>Description:</b></html>");
        lblDDDescription.setPreferredSize(ddLabelsSize);
        JLabel lblDDClasses = new JLabel("<html><b>Classification:</b></html>");
        lblDDClasses.setPreferredSize(ddLabelsSize);
        JLabel lblDDParams = new JLabel("<html><b>Parameters:</b></html>");
        lblDDParams.setPreferredSize(ddLabelsSize);
        JLabel lblDDSource = new JLabel("<html><b>Implementation:</b></html>");
        lblDDSource.setPreferredSize(ddLabelsSize);
        
        lblDDValueTitle = new JLabel();
        lblDDValueName = new JLabel();
        lblDDValueDefinition = new JLabel();
        lblDDValueDescripton = new JLabel();
        lblDDValueClasses = new JLabel();
        pnlDDValueParams = new JPanel();
        lblDDValueParams = new JLabel();
        btnDDValueParams = new JButton("Set parameters");
        btnDDValueParams.setEnabled(false);
        btnDDValueParams.setVisible(false);
        lblDDValueSource = new JLabel();
        
        btnDDValueParams.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                ParametrizedDescriptorDefinition dialog = 
                        new ParametrizedDescriptorDefinition(descNameToTune,
                                paramsToTune);
                Object[] res = (Object[]) dialog.showDialog();
                if (res!=null)
                {
                    tabMoreEqMod.addRow(res);
                }
            }
        });
        
        GroupLayout lyoDescParamsPanel = new GroupLayout(pnlDDValueParams);
        pnlDDValueParams.setLayout(lyoDescParamsPanel);
        lyoDescParamsPanel.setAutoCreateGaps(true);
        lyoDescParamsPanel.setAutoCreateContainerGaps(true);
        lyoDescParamsPanel.setHorizontalGroup(
                lyoDescParamsPanel.createParallelGroup()
                    .addComponent(lblDDValueParams)
                    .addComponent(btnDDValueParams));
        lyoDescParamsPanel.setVerticalGroup(
                lyoDescParamsPanel.createSequentialGroup()
                    .addComponent(lblDDValueParams)
                    .addComponent(btnDDValueParams));
        
        descDefinitionPane = new JPanel();
        GroupLayout lyoDescDefPanel = new GroupLayout(descDefinitionPane);
        descDefinitionPane.setLayout(lyoDescDefPanel);
        lyoDescDefPanel.setAutoCreateGaps(true);
        lyoDescDefPanel.setAutoCreateContainerGaps(true);
        lyoDescDefPanel.setHorizontalGroup(lyoDescDefPanel.createSequentialGroup()
                    .addGroup(lyoDescDefPanel.createParallelGroup()
                    		.addComponent(lblDDName)
                    		.addComponent(lblDDTitle)
                    		.addComponent(lblDDClasses)
                    		.addComponent(lblDDDefinition)
                    		//.addComponent(lblDDDescription)
                    		.addComponent(lblDDParams)
                    		.addComponent(lblDDSource)
                    		)
                    .addGroup(lyoDescDefPanel.createParallelGroup()
                    		.addComponent(lblDDValueName)
                    		.addComponent(lblDDValueTitle)
                    		.addComponent(lblDDValueClasses)
                    		.addComponent(lblDDValueDefinition)
                    		//.addComponent(lblDDValueDescripton)
                    		.addComponent(pnlDDValueParams)
                    		.addComponent(lblDDValueSource)
                    		));
        lyoDescDefPanel.setVerticalGroup(lyoDescDefPanel.createSequentialGroup()
                .addGroup(lyoDescDefPanel.createParallelGroup()
                		.addComponent(lblDDName)
                		.addComponent(lblDDValueName))
                .addGroup(lyoDescDefPanel.createParallelGroup()
                		.addComponent(lblDDTitle)
                		.addComponent(lblDDValueTitle))
                .addGroup(lyoDescDefPanel.createParallelGroup()
                		.addComponent(lblDDClasses)
                		.addComponent(lblDDValueClasses))
                .addGroup(lyoDescDefPanel.createParallelGroup()
                		.addComponent(lblDDDefinition)
                		.addComponent(lblDDValueDefinition))
        //        .addGroup(lyoDescDefPanel.createParallelGroup()
        //        		.addComponent(lblDDDescription)
        //        		.addComponent(lblDDValueDescripton))
                .addGroup(lyoDescDefPanel.createParallelGroup()
                		.addComponent(lblDDParams)
                		.addComponent(pnlDDValueParams))
                .addGroup(lyoDescDefPanel.createParallelGroup()
                		.addComponent(lblDDSource)
                		.addComponent(lblDDValueSource))
                );

        descDefScrollPane = new JScrollPane(descDefinitionPane);
        
        JSplitPane splitPaneDescs = new JSplitPane();
        splitPaneDescs.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        splitPaneDescs.setOneTouchExpandable(true);
        splitPaneDescs.setResizeWeight(0.5);
        splitPaneDescs.setLeftComponent(descTreeScrollPane);
        splitPaneDescs.setRightComponent(descDefScrollPane);
        JPanel lineDescsTree = new JPanel();
        lineDescsTree.setLayout(new BorderLayout(2,2));
        lineDescsTree.add(splitPaneDescs);
        localBlock4.add(lineDescsTree);
        
        lblMoreEq = new JLabel("Custom variables definition:");
        JPanel lineMoreEqTitle = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lineMoreEqTitle.add(lblMoreEq);
        localBlock4.add(lineMoreEqTitle);
        
        String toolTipMoreEq = "<html>Define atom/bond specific descriptors.</html>";
        lineMoreEq = new JPanel();
        lineMoreEq.setLayout(new BoxLayout(lineMoreEq, BoxLayout.LINE_AXIS));
        
        tabMoreEqMod = new DefaultTableModel();
        tabMoreEqMod.setColumnCount(2);
        String column_names[]= {"<html><b>Variable</b></html>", 
        		"<html><b>Definition</b></html>"};
        tabMoreEqMod.setColumnIdentifiers(column_names);
        tabMoreEq = new JTable(tabMoreEqMod);
        tabMoreEq.setToolTipText(toolTipMoreEq);
        tabMoreEq.putClientProperty("terminateEditOnFocusLost", true);
        tabMoreEq.getColumnModel().getColumn(0).setMinWidth(75);
        tabMoreEq.getColumnModel().getColumn(1).setMinWidth(100);
        tabMoreEq.setGridColor(Color.LIGHT_GRAY);
		JTableHeader tabMoreEqHeader = tabMoreEq.getTableHeader();
		tabMoreEqHeader.setPreferredSize(new Dimension(120, 20));
		JScrollPane tabMoreEqScrollPane = new JScrollPane(tabMoreEq);
		//tabMoreEqScrollPane.setMinimumSize(new Dimension(240,30));

        mapKeyFieldToValueField.put(keyMoreEq.toUpperCase(), 
        		tabMoreEqMod);
        

        tabMoreEqScrollPane.setAlignmentY(Component.TOP_ALIGNMENT);
        lineMoreEq.add(tabMoreEqScrollPane);

        JButton btnAtomSpec = new JButton("Add atom-specific variable");
        btnAtomSpec.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				AtomSpecExpressionDefinition dialog = 
				        new AtomSpecExpressionDefinition();
				Object[] res = (Object[]) dialog.showDialog();
				if (res!=null)
				{
					tabMoreEqMod.addRow(res);
				}
			}
		});
        btnAtomSpec.setAlignmentY(Component.TOP_ALIGNMENT);
        
        lineMoreEq.add(btnAtomSpec);
        localBlock4.add(lineMoreEq);
        

        //HEREGOESADVIMPLEMENTATION this is only to facilitate automated insertion of code       
        
        // From here it's all about advanced options
        /*
        JPanel advOptsBlock = new JPanel();
        advOptsBlock.setVisible(false);
        advOptsBlock.setLayout(new BoxLayout(advOptsBlock, SwingConstants.VERTICAL));

        */
        
        /*
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
        advOptsController.setPreferredSize(fileLabelSize); 
        advOptsController.add(advOptShow);
        block.add(new JSeparator());
        block.add(advOptsController);
        block.add(advOptsBlock);  
        */
        
        this.add(scrollablePane);
    }
    
//------------------------------------------------------------------------------
    
    protected void setVisibilityAccordingToFitnessProviderSelection() 
	{
		if (rdbIntOrExt.isSelected())
		{
			localBlock3.setVisible(true);
			localBlock4.setVisible(false);
		}
		else
		{
			localBlock3.setVisible(false);
			localBlock4.setVisible(true);
		}
	}
    
//------------------------------------------------------------------------------
    
    @Override
    protected void preliminatyTasksUponImportingParams()
    {
    	int initialRowCount = tabMoreEqMod.getRowCount();
    	for (int i=0; i<initialRowCount; i++)
    	{
    		tabMoreEqMod.removeRow(0);
    	}
    }
    
//------------------------------------------------------------------------------
    
    @Override
    protected void adaptVisibility() 
    {
    	setVisibilityAccordingToFitnessProviderSelection();
    }

//------------------------------------------------------------------------------
    
    @SuppressWarnings("serial")
	private class DescriptorTreeNode extends DefaultMutableTreeNode
    {
		protected DescriptorForFitness dff;
		protected boolean isLeaf = false;

		/**
		 * Constructor meant for nodes representing an actual descriptor value.
		 * For example, one of the values computed by a descriptor that returns
		 * multiple values.
		 */
		public DescriptorTreeNode(DescriptorForFitness dff) {
			super(dff.getShortName());
			this.dff = dff;
			this.isLeaf = true;
		}

		/**
		 * Constructor meant for nodes representing descriptors that return
		 * multiple values.
		 */
		public DescriptorTreeNode(String descriptorName, DescriptorForFitness dff) {
			super(descriptorName);
			this.dff = dff;
		}

		/**
		 * Constructor meant for nodes that represent only the main descriptor 
		 * classes (e.g., molecular, protein,electronic, etc.).
		 */
		public DescriptorTreeNode(String kls) {
			super(kls);
		}
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
    	importParametersFromDenoptimParamsFile(fileName,"FP-");
    	
    	rdbSrcOrNew.setSelected(false);
    	localBlock1.setVisible(false);
		localBlock2.setVisible(true);		
		if (rdbIntOrExt.isSelected())
		{
			localBlock3.setVisible(true);
			localBlock4.setVisible(false);
		}
		else
		{
			localBlock3.setVisible(false);
			localBlock4.setVisible(true);
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
			JOptionPane.showMessageDialog(this,
					"<html>Parameter '" + key + "' is not recognized<br> and "
							+ "will be ignored.</html>",
	                "WARNING",
	                JOptionPane.WARNING_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;
  		}
  		
 		switch (valueFieldClass)
 		{
 			case "class javax.swing.JTextField":
 				if (key.toUpperCase().equals(keyFitProviderSource.toUpperCase())) 
    			{
 				    rdbIntOrExt.setSelected(true);
    			}
 				if (key.toUpperCase().equals(keyEq.toUpperCase()))
 				{
 					value = value.trim();
 					if (value.startsWith("${") && value.endsWith("}"));
 					{
 						value = value.substring(2, value.lastIndexOf("}"));
 					}
 				}
 				((JTextField) valueField).setText(value);
 				break;
 				
 			case "class javax.swing.JRadioButton":
 				if (key.toUpperCase().equals(key3dTrees.toUpperCase()))
 				{
 					((JRadioButton) valueField).setSelected(false);
 				} else {
 					((JRadioButton) valueField).setSelected(true);
 				}
 				
 				if (key.toUpperCase().equals(keyPreFitnessUIDCheck.toUpperCase()))
 				{
                    ((JRadioButton) valueField).setSelected(false);
                } else {
                    ((JRadioButton) valueField).setSelected(true);
                }
 				break;
 				
 			case "class javax.swing.JComboBox":
 				((JComboBox<String>) valueField).setSelectedItem(value);
 				break;
 				
 			case "class javax.swing.table.DefaultTableModel":
 				if (key.toUpperCase().equals(keyMoreEq.toUpperCase())) 
    			{
 					String noHead = value.replace("${atomSpecific('", "");
 					Object[] rowContent = new Object[2];
 					rowContent[0] = noHead.split("'")[0];
 					rowContent[1] = value;
 					((DefaultTableModel) valueField).addRow(rowContent);
    			} else {
    				//WARNING: there might be other cases where we do not take 
    				// all the row/columns
    				((DefaultTableModel) valueField).addRow(value.split(" "));
    			}
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
    	sb.append(NL);
        sb.append("# Fitness Provider - parameters").append(NL);
        if (rdbIntOrExt.isSelected())
        {
	        sb.append(getStringIfNotEmpty(keyFitProviderSource,
	        		txtFitProviderSource));
	        sb.append(keyFitProviderInterpreter).append("=").append(
	        		cmbFitProviderInterpreter.getSelectedItem()).append(NL);
        }
        else
        {
        	sb.append(getStringIfNotEmpty(keyEq,txtEq,"${","}"));
        	for (int i=0; i<tabMoreEqMod.getRowCount(); i++) 
            {
        		sb.append(keyMoreEq).append("=").append(
        				tabMoreEqMod.getValueAt(i, 1)).append(NL);
            }
        }
        if (!rdb3dTrees.isSelected())
        {
            sb.append(key3dTrees).append(NL);
        }
        if (rdbPreFitnessUIDCheck.isSelected())
        {
            sb.append(keyPreFitnessUIDCheck).append(NL);
        }
        //HEREGOESPRINT this is only to facilitate automated insertion of code       
    }
    
//------------------------------------------------------------------------------
    
    private class ParametrizedDescriptorDefinition extends GUIModalDialog
    {   
        public ParametrizedDescriptorDefinition(String descName, String[] paramNames)
        {
            super();
            this.setBounds(150, 150, 500, 200);
            this.setTitle("Define parametrized descriptor variable");
            
            Dimension sizeNameFields = new Dimension(200,preferredHeight);
            Dimension sizeNameLbls = new Dimension(120,preferredHeight);
            
            JPanel rowOne = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel lblVarName = new JLabel("Variable name: ");
            lblVarName.setPreferredSize(sizeNameLbls);
            lblVarName.setToolTipText("<html>This is the string representing "
                    + "a user-defined variable <br> in the expression of the "
                    + "fitness.</html>");
            JTextField txtVarName = new JTextField();
            txtVarName.setPreferredSize(sizeNameFields);
            rowOne.add(lblVarName);
            rowOne.add(txtVarName);
            
            JPanel rowTwo = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel lblDescName = new JLabel("Descriptor name: ");
            lblDescName.setPreferredSize(sizeNameLbls);
            lblDescName.setToolTipText("<html>This is the pre-defined short "
                    + "name "
                    + "reported <br>in "
                    + "the collection of descriptors.</html>");
            JTextField txtDescName = new JTextField(descName);
            txtDescName.setPreferredSize(sizeNameFields);
            rowTwo.add(lblDescName);
            rowTwo.add(txtDescName);
            
            JPanel rowThree = new JPanel();
            rowThree.setLayout(new BorderLayout());
            JLabel lblParams = new JLabel("Parameters:");
            String paramToolTip = "<html>The parameters provided in the "
                    + "order defined in the description <br> "
                    + "of the descriptor. Only the parameter value is needed, "
                    + "not its name.</html>";
            lblParams.setToolTipText(paramToolTip);
            
            JTable tabParams;
            DefaultTableModel tabParamsMod = new DefaultTableModel();
            tabParamsMod.setColumnCount(1);
            int tabSize = paramNames.length;
            tabParamsMod.setRowCount(tabSize);
            tabParams = new JTable(tabParamsMod);
            tabParams.setToolTipText(paramToolTip);
            tabParams.putClientProperty("terminateEditOnFocusLost", true);
            tabParams.getColumnModel().getColumn(0).setMinWidth(150);
            tabParams.setGridColor(Color.LIGHT_GRAY);
            JScrollPane txtParamScrollPane = new JScrollPane(tabParams);
            tabParams.setTableHeader(null);
            
            rowThree.add(lblParams,BorderLayout.WEST);
            rowThree.add(txtParamScrollPane,BorderLayout.CENTER);
            JPanel firstTwo = new JPanel();
            firstTwo.setLayout(new BoxLayout(firstTwo, 
                    SwingConstants.VERTICAL));
            firstTwo.add(rowOne);
            firstTwo.add(rowTwo);
            addToNorthPane(firstTwo);
            addToCentralPane(rowThree);
            this.btnDone.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (txtVarName.getText().equals("")
                            && txtDescName.getText().equals(""))
                    {
                        result = null;
                    } else {
                        String line = "${parametrized('" + txtVarName.getText() 
                                + "','" + txtDescName.getText() + "','";
                        //NB: all params are collected into a single string!
                        for (int i=0; i<tabSize; i++)
                        {
                            String s = tabParamsMod.getValueAt(i,0).toString()
                                    .trim();
                            if (s.contains(paramNames[i]))
                            {
                                s = s.replaceFirst(paramNames[i],"");
                                s = s.trim();
                                if (s.startsWith("="))
                                {
                                    s = s.replaceFirst("=","");
                                    s = s.trim();
                                }
                            }
                            line = line + s;
                            if (i<(tabSize-1))
                                line = line  + ", ";
                        }
                        line = line + "')}";
                        result = new Object[] {txtVarName.getText(),line};
                    }
                    close();
                }
            });
            this.btnCanc.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    result = null;
                    close();
                }
            });
        }
    }
    
//------------------------------------------------------------------------------
    
    private class AtomSpecExpressionDefinition extends GUIModalDialog
    {
    	public AtomSpecExpressionDefinition()
    	{
    		super();
    		this.setBounds(150, 150, 500, 200);
    		this.setTitle("Define atom-specific variable");
    		
    		Dimension sizeNameFields = new Dimension(200,preferredHeight);
    		Dimension sizeNameLbls = new Dimension(120,preferredHeight);
    		
			JPanel rowOne = new JPanel(new FlowLayout(FlowLayout.LEFT));
			JLabel lblVarName = new JLabel("Variable name: ");
			lblVarName.setPreferredSize(sizeNameLbls);
			lblVarName.setToolTipText("<html>This is the string representing "
					+ "a user-defined variable <br> in the expression of the "
					+ "fitness.</html>");
			JTextField txtVarName = new JTextField();
			txtVarName.setPreferredSize(sizeNameFields);
			rowOne.add(lblVarName);
			rowOne.add(txtVarName);
			
			JPanel rowTwo = new JPanel(new FlowLayout(FlowLayout.LEFT));
			JLabel lblDescName = new JLabel("Descriptor name: ");
			lblDescName.setPreferredSize(sizeNameLbls);
			lblDescName.setToolTipText("<html>This is the pre-defined short "
					+ "name "
					+ "reported <br>in "
					+ "the collection of descriptors.</html>");
			JTextField txtDescName = new JTextField();
			txtDescName.setPreferredSize(sizeNameFields);
			rowTwo.add(lblDescName);
			rowTwo.add(txtDescName);
			
			JPanel rowThree = new JPanel();
			rowThree.setLayout(new BorderLayout());
			JLabel lblSmarts = new JLabel("SMARTS:");
			lblSmarts.setToolTipText("<html>The SMARTS query used to identify "
					+ "specific atom/bonds to <br> "
					+ "be used for the calculation of "
					+ "the numerical value of this variable.</html>");
			JEditorPane txtSmarts = new JEditorPane();
			JScrollPane txtSmartsScrollPane = new JScrollPane(txtSmarts);
			rowThree.add(lblSmarts,BorderLayout.WEST);
			rowThree.add(txtSmartsScrollPane,BorderLayout.CENTER);
			JPanel firstTwo = new JPanel();
			firstTwo.setLayout(new BoxLayout(firstTwo, 
					SwingConstants.VERTICAL));
			firstTwo.add(rowOne);
			firstTwo.add(rowTwo);
			addToNorthPane(firstTwo);
			addToCentralPane(rowThree);
			this.btnDone.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					if (txtVarName.getText().equals("")
							&& txtDescName.getText().equals("")
							&& txtSmarts.getText().equals(""))
					{
						result = null;
					} else {
						result = new Object[] {txtVarName.getText(), 
								"${atomSpecific('" + txtVarName.getText() 
								+ "','" + txtDescName.getText() + "','"
								+ txtSmarts.getText() + "')}"};
					}
					close();
				}
			});
			this.btnCanc.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					result = null;
					close();
				}
			});
    	}
    }
}
