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
 * Form collecting input parameters for a setting-up the fitness provider.
 */

public class FitnessParametersForm extends ParametersForm
{

    /**
	 * Version
	 */
	private static final long serialVersionUID = -282726238111247056L;
	
	JPanel lineSrcOrNew;
    JRadioButton rdbSrcOrNew;

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

    //HEREGOFIELDS  this is only to facilitate automated insertion of code
        
        
    String NL = System.getProperty("line.separator");
    
    public FitnessParametersForm(Dimension d)
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

        String toolTipSrcOrNew = "<html>A fitness provider is an existing tool or script.<br> The fitness provider must produce an output SDF file with the <code>        //HEREGOESIMPLEMENTATIONlt;FITNESS        //HEREGOESIMPLEMENTATIONgt;</code> or <code>        //HEREGOESIMPLEMENTATIONlt;MOL_ERROR        //HEREGOESIMPLEMENTATIONgt;</code> tags.</html>";
        lineSrcOrNew = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rdbSrcOrNew = new JRadioButton("Use external fitnes provider:");
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

        String toolTipFitProviderSource = "Pathname of the executable file.";
        lineFitProviderSource = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblFitProviderSource = new JLabel("Fitness provider executable:", SwingConstants.LEFT);
        lblFitProviderSource.setPreferredSize(fileLabelSize);
        lblFitProviderSource.setToolTipText(toolTipFitProviderSource);
        txtFitProviderSource = new JTextField();
        txtFitProviderSource.setToolTipText(toolTipFitProviderSource);
        txtFitProviderSource.setPreferredSize(fileFieldSize);
        btnFitProviderSource = new JButton("Browse");
        btnFitProviderSource.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
                DenoptimGUIFileOpener.pickFile(txtFitProviderSource);
           }
        });
        lineFitProviderSource.add(lblFitProviderSource);
        lineFitProviderSource.add(txtFitProviderSource);
        lineFitProviderSource.add(btnFitProviderSource);
        localBlock1.add(lineFitProviderSource);
        
        String toolTipFitProviderInterpreter = "Interpreter to be used for the fitness provider executable";
        lineFitProviderInterpreter = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblFitProviderInterpreter = new JLabel("Interpreter for fitnes provider", SwingConstants.LEFT);
        lblFitProviderInterpreter.setPreferredSize(fileLabelSize);
        lblFitProviderInterpreter.setToolTipText(toolTipFitProviderInterpreter);
        cmbFitProviderInterpreter = new JComboBox<String>(new String[] {"BASH", "Python", "JAVA"});
        cmbFitProviderInterpreter.setToolTipText(toolTipFitProviderInterpreter);
        lineFitProviderInterpreter.add(lblFitProviderInterpreter);
        lineFitProviderInterpreter.add(cmbFitProviderInterpreter);
        localBlock1.add(lineFitProviderInterpreter);

        String toolTipEq = "Define equation of integrated fitness provider.";
        lineEq = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblEq = new JLabel("Fitness = ", SwingConstants.LEFT);
        lblEq.setPreferredSize(fileLabelSize);
        lblEq.setToolTipText(toolTipEq);
        txtEq = new JTextField();
        txtEq.setToolTipText(toolTipEq);
        txtEq.setPreferredSize(strFieldSize);
        lineEq.add(lblEq);
        lineEq.add(txtEq);
        localBlock2.add(lineEq);

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

    @Override
    public void putParametersToString(StringBuilder sb) 
    {
        sb.append("# Fitness Provider - paramerers").append(NL);
        sb.append(getStringIfNotEmpty(keyFitProviderSource,txtFitProviderSource));
        sb.append(keyFitProviderInterpreter).append("=").append(cmbFitProviderInterpreter.getSelectedItem()).append(NL);
        sb.append(getStringIfNotEmpty(keyEq,txtEq));;
        //HEREGOESPRINT this is only to facilitate automated insertion of code       
    }
}
