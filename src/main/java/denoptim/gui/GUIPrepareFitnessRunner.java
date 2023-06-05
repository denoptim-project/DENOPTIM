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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;


/**
 * Form that allows to test the configuration of a fitness provider.
 * 
 * @author Marco Foscato
 *
 */

public class GUIPrepareFitnessRunner extends GUIPrepare
{

	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 2579606720045728971L;
	
	/**
	 * Unique identified for instances of this form
	 */
	public static AtomicInteger testFitnessTabUID = new AtomicInteger(1);

//------------------------------------------------------------------------------
	    
	/**
	 * Constructor
	 */
	public GUIPrepareFitnessRunner(GUIMainPanel mainPanel) {
		super(mainPanel, "Prepare Fitness Runner #" + testFitnessTabUID.getAndIncrement());
		initialize();
	}

//------------------------------------------------------------------------------
	    
	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		
	    InputForm inputParsPane = new InputForm(mainPanel.getSize());
		super.allParams.add(inputParsPane);
		super.tabbedPane.addTab("Input/Output Files", null, inputParsPane, null);
		
		FitnessParametersForm fitParsPane = new FitnessParametersForm(
		        mainPanel.getSize());
		super.allParams.add(fitParsPane);
		super.tabbedPane.addTab("Fitness Provider", null, fitParsPane, null);
		
	}
	
//------------------------------------------------------------------------------
	
	private class InputForm extends ParametersForm 
	{
        /**
         * Version UID
         */
        private static final long serialVersionUID = 1L;
	    
        JPanel block;
        
        /**
         * Map connecting the parameter keyword and the field
         * containing the parameter value. 
         */
        private Map<String,Object> mapKeyFieldToValueField;
        
        String keyInFile = "FR-Input";
        JTextField txtInFile;
        
        String keyOutFile = "FR-Output";
        JTextField txtOutFile;
        
        String NL = System.getProperty("line.separator");
        
        public InputForm(Dimension d)
        {
            mapKeyFieldToValueField = new HashMap<String,Object>();
            this.setLayout(new BorderLayout()); //Needed to allow dynamic resizing!

            block = new JPanel();
            JScrollPane scrollablePane = new JScrollPane(block);
            block.setLayout(new BoxLayout(block, SwingConstants.VERTICAL));
            
            String toolTipInFile = "Pathname of the SDF file with the candidate to evaluate.";
            JPanel LineInFile = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel lblInFile = new JLabel("Input SDF file:", SwingConstants.LEFT);
            lblInFile.setPreferredSize(fileLabelSize);
            lblInFile.setToolTipText(toolTipInFile);
            txtInFile = new JTextField();
            txtInFile.setToolTipText(toolTipInFile);
            txtInFile.setPreferredSize(fileFieldSize);
            txtInFile.getDocument().addDocumentListener(fieldListener);
            JButton btnInFile = new JButton("Browse");
            btnInFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                    GUIFileOpener.pickFileForTxtField(txtInFile,btnInFile);
               }
            });
            LineInFile.add(lblInFile);
            LineInFile.add(txtInFile);
            LineInFile.add(btnInFile);

            mapKeyFieldToValueField.put(keyInFile.toUpperCase(), txtInFile);
            
            block.add(LineInFile);
            
            String toolTipOutFile = "Pathname of the SDF file where to write the results of the evaluation.";
            JPanel LineOutFile = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel lblOutFile = new JLabel("Output SDF file:", SwingConstants.LEFT);
            lblOutFile.setPreferredSize(fileLabelSize);
            lblOutFile.setToolTipText(toolTipOutFile);
            txtOutFile = new JTextField();
            txtOutFile.setToolTipText(toolTipOutFile);
            txtOutFile.setPreferredSize(fileFieldSize);
            txtOutFile.getDocument().addDocumentListener(fieldListener);
            JButton btnOutFile = new JButton("Browse");
            btnOutFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                    GUIFileOpener.pickFileForTxtField(txtOutFile,btnOutFile);
               }
            });
            LineOutFile.add(lblOutFile);
            LineOutFile.add(txtOutFile);
            LineOutFile.add(btnOutFile);

            mapKeyFieldToValueField.put(keyOutFile.toUpperCase(), txtOutFile);

            block.add(LineOutFile);
            
            block.add(super.getPanelForUnformattedInput());
            
            this.add(scrollablePane);
        }
        
    //--------------------------------------------------------------------------
        
        /**
         * Imports parameters from a properly formatted parameters file.
         * The file is a text file with lines containing KEY=VALUE pairs.
         * @param fileName the pathname of the file to read
         * @throws Exception
         */
        
        @Override
        public void importParametersFromDenoptimParamsFile(String fileName) 
                throws Exception
        {
            clearUnformattedTxtArea();
            importParametersFromDenoptimParamsFile(fileName,"FR-");
            showUnknownKeyWarning(this, "Fitness Runner");
        }
        
    //--------------------------------------------------------------------------
        

        @Override
        public void importSingleParameter(String key, String value) 
                throws Exception 
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
                addToUnformattedTxt(key, value);
                return;
            }
            
            switch (valueFieldClass)
            {
                case "class javax.swing.JTextField":
                    if (key.toUpperCase().equals(keyInFile.toUpperCase())) 
                    {
                        value = value.trim();
                    }
                    ((JTextField) valueField).setText(value);
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
            sb.append("# Fitness Runner - parameters").append(NL);
            sb.append(getStringIfNotEmpty(keyInFile,txtInFile));
            sb.append(getStringIfNotEmpty(keyOutFile,txtOutFile));
            sb.append(NL);

            sb.append(getTextForUnformattedSettings()).append(NL);
        }
	}
	
//------------------------------------------------------------------------------
    
}
