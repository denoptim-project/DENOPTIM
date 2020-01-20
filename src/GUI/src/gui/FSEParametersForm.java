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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

/**
 * Form collecting input parameters for a combinatorial/virtual screening 
 * experiment performed by FragSpaceExplorer.
 */

public class FSEParametersForm extends ParametersForm
{

    /**
	 * Version
	 */
	private static final long serialVersionUID = 5067352357196631445L;
	
    /**
     * Map connecting the parameter keyword and the field
     * containing the parameter value. 
     */
	private Map<String,Object> mapKeyFieldToValueField;
	
    JPanel block;    
    JPanel localBlock1;
    JPanel localBlock2;
    JPanel advOptsBlock;
	
	JPanel lineSrcOrNew;
    JRadioButton rdbSrcOrNew;
    
    JPanel lineFSESource;
    JLabel lblFSESource;
    JTextField txtFSESource;
    JButton btnFSESource;
    JButton btnLoadFSESource;

    String keyPar2 = "FSE-MaxLevel";
    JPanel linePar2;
    JLabel lblPar2;
    JTextField txtPar2;

    String keyPar7 = "FSE-NumOfProcessors";
    JPanel linePar7;
    JLabel lblPar7;
    JTextField txtPar7;

    String keyPar3 = "FSE-UIDFile";
    JPanel linePar3;
    JLabel lblPar3;
    JTextField txtPar3;
    JButton btnPar3;

    String keyPar9 = "FSE-RootGraphs";
    JPanel linePar9;
    JLabel lblPar9;
    JTextField txtPar9;
    JButton btnPar9;

    String keyPar10 = "FSE-RootGraphsFormat";
    JPanel linePar10;
    JLabel lblPar10;
    JComboBox<String> cmbPar10;

    String keyPar8 = "FSE-Verbosity";
    JPanel linePar8;
    JLabel lblPar8;
    JTextField txtPar8;

    String keyPar1 = "FSE-WorkDir";
    JPanel linePar1;
    JLabel lblPar1;
    JTextField txtPar1;
    JButton btnPar1;

    String keyPar4 = "FSE-DBRootFolder";
    JPanel linePar4;
    JLabel lblPar4;
    JTextField txtPar4;
    JButton btnPar4;

    String keyPar5 = "FSE-MaxWait";
    JPanel linePar5;
    JLabel lblPar5;
    JTextField txtPar5;

    String keyPar6 = "FSE-WaitStep";
    JPanel linePar6;
    JLabel lblPar6;
    JTextField txtPar6;

    String keyPar11 = "FSE-CheckPointStepLength";
    JPanel linePar11;
    JLabel lblPar11;
    JTextField txtPar11;

    String keyPar12 = "FSE-RestartFromCheckpoint";
    JPanel linePar12;
    JLabel lblPar12;
    JTextField txtPar12;
    JButton btnPar12;

    //HEREGOFIELDS  this is only to facilitate automated insertion of code

    
    String NL = System.getProperty("line.separator");
    
    public FSEParametersForm(Dimension d)
    {
    	//Initialize this instance
    	mapKeyFieldToValueField = new HashMap<String,Object>();
    	
    	//Build form
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
        
        String toolTipFSESource = "<html>Pathname of a DENOPTIM's parameter file with FSE settings.</html>";
        lineFSESource = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblFSESource = new JLabel("Use parameters from file:", SwingConstants.LEFT);
        lblFSESource.setToolTipText(toolTipFSESource);
        txtFSESource = new JTextField();
        txtFSESource.setToolTipText(toolTipFSESource);
        txtFSESource.setPreferredSize(fileFieldSize);
        btnFSESource = new JButton("Browse");
        btnFSESource.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
                DenoptimGUIFileOpener.pickFile(txtFSESource);
           }
        });
        btnLoadFSESource = new JButton("Load...");
        txtFSESource.setToolTipText("<html>Load the parameters in this form.<br>Allows to inspect and edit the parameters.</html>");
        btnLoadFSESource.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
	        	try 
	        	{
					importParametersFromDenoptimParamsFile(txtFSESource.getText());
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
        lineFSESource.add(lblFSESource);
        lineFSESource.add(txtFSESource);
        lineFSESource.add(btnFSESource);
        lineFSESource.add(btnLoadFSESource);
        localBlock1.add(lineFSESource);
        
        String toolTipPar2 = "<html>Specifies up to which level we'll add layers of fragments.<br>Note that the root (i.e., scaffold or root graph) is assigned <code>level = -1</code>.<br>Therefore, if the maximum level permitted is 3, then we will try to append <br>up to 4 layers of fragments (levels = 0, 1, 2, and 3).</html>";
        linePar2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar2 = new JLabel("Maximum level numer to consider:", SwingConstants.LEFT);
        lblPar2.setPreferredSize(fileLabelSize);
        lblPar2.setToolTipText(toolTipPar2);
        txtPar2 = new JTextField();
        txtPar2.setToolTipText(toolTipPar2);
        txtPar2.setPreferredSize(strFieldSize);
        txtPar2.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar2.toUpperCase(),txtPar2);
        linePar2.add(lblPar2);
        linePar2.add(txtPar2);
        localBlock2.add(linePar2);

        String toolTipPar7 = "Specifies the number of asynchronous processes that can be run in parallel. Usually this corresponds to the number of slave cores, if 1 such core corresponds to 1 external tas";
        linePar7 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar7 = new JLabel("Mas. number parallel tasks:", SwingConstants.LEFT);
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

        String toolTipPar3 = "Specifies the pathname of the file with unique chemical entity ID.";
        linePar3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar3 = new JLabel("File with known UIDs:", SwingConstants.LEFT);
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

        String toolTipPar9 = "Specifies the pathname of a file containing the list of graphs that will be expanded.";
        linePar9 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar9 = new JLabel("Library of root graphs:", SwingConstants.LEFT);
        lblPar9.setPreferredSize(fileLabelSize);
        lblPar9.setToolTipText(toolTipPar9);
        txtPar9 = new JTextField();
        txtPar9.setToolTipText(toolTipPar9);
        txtPar9.setPreferredSize(fileFieldSize);
        txtPar9.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar9.toUpperCase(),txtPar9);
        btnPar9 = new JButton("Browse");
        btnPar9.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
                DenoptimGUIFileOpener.pickFile(txtPar9);
           }
        });
        linePar9.add(lblPar9);
        linePar9.add(txtPar9);
        linePar9.add(btnPar9);
        localBlock2.add(linePar9);

        String toolTipPar10 = "<html>Specifies the format of the root graphs.<br><ul><li><code><b>STRING</b></code> for human readable graphs</li><li><code><b>'BYTE'</b></code> for serialized graphs (binary)</li></ul></html>";
        linePar10 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar10 = new JLabel("Format of root graphs: ", SwingConstants.LEFT);
        lblPar10.setPreferredSize(fileLabelSize);
        lblPar10.setToolTipText(toolTipPar10);
        cmbPar10 = new JComboBox<String>(new String[] {"STRING", "BYTE"});
        cmbPar10.setToolTipText(toolTipPar10);
        cmbPar10.addActionListener(cmbFieldChange);
        mapKeyFieldToValueField.put(keyPar10.toUpperCase(),cmbPar10);
        linePar10.add(lblPar10);
        linePar10.add(cmbPar10);
        localBlock2.add(linePar10);

        //HEREGOESIMPLEMENTATION this is only to facilitate automated insertion of code


        // From here it's all about advanced options

        String toolTipPar8 = "Specifies the verbosity level.";
        linePar8 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar8 = new JLabel("Logging level:", SwingConstants.LEFT);
        lblPar8.setPreferredSize(fileLabelSize);
        lblPar8.setToolTipText(toolTipPar8);
        txtPar8 = new JTextField();
        txtPar8.setToolTipText(toolTipPar8);
        txtPar8.setPreferredSize(strFieldSize);
        txtPar8.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar8.toUpperCase(),txtPar8);
        linePar8.add(lblPar8);
        linePar8.add(txtPar8);
        advOptsBlock.add(linePar8);

        String toolTipPar1 = "Specifies the pathname of the directory where files will be created";
        linePar1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar1 = new JLabel("Work space:", SwingConstants.LEFT);
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
        advOptsBlock.add(linePar1);

        String toolTipPar4 = "Specifies the pathname of the root of the folder tree of generate graphs.";
        linePar4 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar4 = new JLabel("Location of DENOPTIMGRaph database:", SwingConstants.LEFT);
        lblPar4.setPreferredSize(fileLabelSize);
        lblPar4.setToolTipText(toolTipPar4);
        txtPar4 = new JTextField();
        txtPar4.setToolTipText(toolTipPar4);
        txtPar4.setPreferredSize(fileFieldSize);
        txtPar4.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar4.toUpperCase(),txtPar4);
        btnPar4 = new JButton("Browse");
        btnPar4.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
                DenoptimGUIFileOpener.pickFile(txtPar4);
           }
        });
        linePar4.add(lblPar4);
        linePar4.add(txtPar4);
        linePar4.add(btnPar4);
        advOptsBlock.add(linePar4);

        String toolTipPar5 = "Specifies the wall time limit (in seconds) for completion of one or more tasks. Accepts only integer number";
        linePar5 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar5 = new JLabel("Max. time for fitness evaluation:", SwingConstants.LEFT);
        lblPar5.setPreferredSize(fileLabelSize);
        lblPar5.setToolTipText(toolTipPar5);
        txtPar5 = new JTextField();
        txtPar5.setToolTipText(toolTipPar5);
        txtPar5.setPreferredSize(strFieldSize);
        txtPar5.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar5.toUpperCase(),txtPar5);
        linePar5.add(lblPar5);
        linePar5.add(txtPar5);
        advOptsBlock.add(linePar5);

        String toolTipPar6 = "Specifies the sleeping time (or time step, in seconds) between checks for completion of one or more tasks. Accepts only integer number";
        linePar6 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar6 = new JLabel("Time between checks for completion:", SwingConstants.LEFT);
        lblPar6.setPreferredSize(fileLabelSize);
        lblPar6.setToolTipText(toolTipPar6);
        txtPar6 = new JTextField();
        txtPar6.setToolTipText(toolTipPar6);
        txtPar6.setPreferredSize(strFieldSize);
        txtPar6.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar6.toUpperCase(),txtPar6);
        linePar6.add(lblPar6);
        linePar6.add(txtPar6);
        advOptsBlock.add(linePar6);

        String toolTipPar11 = "<html>Specifies the distance between two subsequent updates of the<br> checkpoint information as a number of generated graphs.</html>";
        linePar11 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar11 = new JLabel("Make checkpoint every # graphs:", SwingConstants.LEFT);
        lblPar11.setPreferredSize(fileLabelSize);
        lblPar11.setToolTipText(toolTipPar11);
        txtPar11 = new JTextField();
        txtPar11.setToolTipText(toolTipPar11);
        txtPar11.setPreferredSize(strFieldSize);
        txtPar11.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar11.toUpperCase(),txtPar11);
        linePar11.add(lblPar11);
        linePar11.add(txtPar11);
        advOptsBlock.add(linePar11);

        String toolTipPar12 = "<html>Specifies the pathname of the checkpoint file, <br> and makes FragSpaceExplorer restart from such file.</html>";
        linePar12 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblPar12 = new JLabel("Restart from checkpoint file: ", SwingConstants.LEFT);
        lblPar12.setPreferredSize(fileLabelSize);
        lblPar12.setToolTipText(toolTipPar12);
        txtPar12 = new JTextField();
        txtPar12.setToolTipText(toolTipPar12);
        txtPar12.setPreferredSize(fileFieldSize);
        txtPar12.getDocument().addDocumentListener(fieldListener);
        mapKeyFieldToValueField.put(keyPar12.toUpperCase(),txtPar12);
        btnPar12 = new JButton("Browse");
        btnPar12.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
                DenoptimGUIFileOpener.pickFile(txtPar12);
           }
        });
        linePar12.add(lblPar12);
        linePar12.add(txtPar12);
        linePar12.add(btnPar12);
        advOptsBlock.add(linePar12);

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
    	importParametersFromDenoptimParamsFile(fileName,"FSE-");
    	
    	rdbSrcOrNew.setSelected(false);
    	localBlock1.setVisible(false);
		localBlock2.setVisible(true);		
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
    	sb.append("# FragSpaceExplorer - paramerers").append(NL);
    	
        if (rdbSrcOrNew.isSelected())
        {
        	if (txtFSESource.getText().equals("") || txtFSESource.getText() == null)
        	{
        		throw new Exception("<html>No source specified for FSE parameters.<br>Please, specify the file name.</html>");
        	}
        	importParametersFromDenoptimParamsFile(txtFSESource.getText());
        }
        
        sb.append(getStringIfNotEmpty(keyPar2,txtPar2));;
        sb.append(getStringIfNotEmpty(keyPar7,txtPar7));;
        sb.append(getStringIfNotEmpty(keyPar3,txtPar3));
        sb.append(getStringIfNotEmpty(keyPar9,txtPar9));
        sb.append(keyPar10).append("=").append(cmbPar10.getSelectedItem()).append(NL);
        sb.append(getStringIfNotEmpty(keyPar8,txtPar8));;
        sb.append(getStringIfNotEmpty(keyPar1,txtPar1));
        sb.append(getStringIfNotEmpty(keyPar4,txtPar4));
        sb.append(getStringIfNotEmpty(keyPar5,txtPar5));;
        sb.append(getStringIfNotEmpty(keyPar6,txtPar6));;
        sb.append(getStringIfNotEmpty(keyPar11,txtPar11));;
        sb.append(getStringIfNotEmpty(keyPar12,txtPar12));
        //HEREGOESPRINT this is only to facilitate automated insertion of code        
    }
}
