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
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;


/**
 * Form that allows to test the configuration of a fitness provider.
 * 
 * @author Marco Foscato
 *
 */

public class GUITestFitnessProvider extends GUIPrepare
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
	public GUITestFitnessProvider(GUIMainPanel mainPanel) {
		super(mainPanel, "Fitness Tester #" + testFitnessTabUID.getAndIncrement());
		initialize();
	}

//------------------------------------------------------------------------------
	    
	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		
	    InputForm inputParsPane = new InputForm(mainPanel.getSize());
		super.allParams.add(inputParsPane);
		super.tabbedPane.addTab("Test Data", null, inputParsPane, null);
		
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
        
        public InputForm(Dimension d)
        {
            this.setLayout(new BorderLayout()); //Needed to allow dynamic resizing!

            block = new JPanel();
            JScrollPane scrollablePane = new JScrollPane(block);
            block.setLayout(new BoxLayout(block, SwingConstants.VERTICAL));
            
            String toolTipPar1 = "Pathname of the SDF file with the candidate to evaluate.";
            JPanel LinePar1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel lblPar1 = new JLabel("Input SDF file:", SwingConstants.LEFT);
            lblPar1.setPreferredSize(fileLabelSize);
            lblPar1.setToolTipText(toolTipPar1);
            JTextField txtPar1 = new JTextField();
            txtPar1.setToolTipText(toolTipPar1);
            txtPar1.setPreferredSize(fileFieldSize);
            txtPar1.getDocument().addDocumentListener(fieldListener);
            JButton btnPar1 = new JButton("Browse");
            btnPar1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                    GUIFileOpener.pickFileForTxtField(txtPar1,btnPar1);
               }
            });
            LinePar1.add(lblPar1);
            LinePar1.add(txtPar1);
            LinePar1.add(btnPar1);
            
            block.add(LinePar1);
            
            String toolTipPar2 = "Pathname of the SDF file where to write the results of the evaluation.";
            JPanel LinePar2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel lblPar2 = new JLabel("Output SDF file:", SwingConstants.LEFT);
            lblPar2.setPreferredSize(fileLabelSize);
            lblPar2.setToolTipText(toolTipPar2);
            JTextField txtPar2 = new JTextField();
            txtPar2.setToolTipText(toolTipPar2);
            txtPar2.setPreferredSize(fileFieldSize);
            txtPar2.getDocument().addDocumentListener(fieldListener);
            JButton btnPar2 = new JButton("Browse");
            btnPar2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                    GUIFileOpener.pickFileForTxtField(txtPar2,btnPar2);
               }
            });
            LinePar2.add(lblPar2);
            LinePar2.add(txtPar2);
            LinePar2.add(btnPar2);

            block.add(LinePar2);
            
            this.add(scrollablePane);
        }
	}
	
//------------------------------------------------------------------------------
    
}
