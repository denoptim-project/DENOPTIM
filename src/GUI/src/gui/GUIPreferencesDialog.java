package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

public class GUIPreferencesDialog extends GUIModalDialog
{

	/**
	 * Version UID
	 */
	private static final long serialVersionUID = -1416475901274128714L;
	
    /**
     * Default sizes for file pathname labels
     */
    final Dimension fileLabelSize = new Dimension(250,28);
    
    /**
     * Default text field height
     */
    final int preferredHeight = 
    		(int) (new JTextField()).getPreferredSize().getHeight();
    
    /**
     * Default sizes for short pathname fields (i.e., string or number)
     */
    final Dimension strFieldSize = new Dimension(75,preferredHeight);
    
	private JPanel centralPanel;
	
	private String namGraphTxtSize = "Font size for graph labels";
	private JPanel pnlGraphTxtSize;
	private JLabel lblGraphTxtSize;
	private JTextField txtGraphTxtSize;
	
    private String namGraphNodeSize = "Size of graph nodes";
    private JPanel pnlGraphNodeSize;
    private JLabel lblGraphNodeSize;
    private JTextField txtGraphNodeSize;
    
    private String namChartPointSize = "Size of points in evolution chart";
    private JPanel pnlChartPointSize;
    private JLabel lblChartPointSize;
    private JTextField txtChartPointSize;
	
	
	private boolean inputIsOK = true;

	public GUIPreferencesDialog()
	{
		centralPanel = new JPanel();
        JScrollPane scrollablePane = new JScrollPane(centralPanel);
        centralPanel.setLayout(new BoxLayout(
        		centralPanel, SwingConstants.VERTICAL)); 
        
        JPanel titleGraphViewer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titleGraphViewer.add(new JLabel("<html><b>Graph viewer</b></html>"));
        centralPanel.add(titleGraphViewer);
        
        pnlGraphTxtSize = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblGraphTxtSize = new JLabel(namGraphTxtSize + ":");
        txtGraphTxtSize = new JTextField();
        txtGraphTxtSize.setPreferredSize(strFieldSize);
        txtGraphTxtSize.setText(GUIPreferences.graphLabelFontSize+"");
        pnlGraphTxtSize.add(lblGraphTxtSize);
        pnlGraphTxtSize.add(txtGraphTxtSize);
        centralPanel.add(pnlGraphTxtSize);
        
        pnlGraphNodeSize = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblGraphNodeSize = new JLabel(namGraphNodeSize + ":");
        txtGraphNodeSize = new JTextField();
        txtGraphNodeSize.setPreferredSize(strFieldSize);
        txtGraphNodeSize.setText(GUIPreferences.graphNodeSize+"");
        pnlGraphNodeSize.add(lblGraphNodeSize);
        pnlGraphNodeSize.add(txtGraphNodeSize);
        centralPanel.add(pnlGraphNodeSize);
        
        centralPanel.add(new JSeparator());
        
        pnlChartPointSize = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblChartPointSize = new JLabel(namChartPointSize + ":");
        txtChartPointSize = new JTextField();
        txtChartPointSize.setPreferredSize(strFieldSize);
        txtChartPointSize.setText(GUIPreferences.chartPointSize+"");
        pnlChartPointSize.add(lblChartPointSize);
        pnlChartPointSize.add(txtChartPointSize);
        centralPanel.add(pnlChartPointSize);
        
		// Customize the buttons of the modal dialog
		this.btnDone.setText("Save");
		this.btnDone.setToolTipText("Save the values and close dialog");
		this.btnDone.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				checkInput();
				if (inputIsOK)
				{
					storeValues();
					close();
				}
			}
			
		});
		
		super.addToCentralPane(scrollablePane);
	}
	
//-----------------------------------------------------------------------------
	
	private void checkInput()
	{
		inputIsOK = true; //resetting results from previous attempts
		mustParseToInt(txtGraphTxtSize, namGraphTxtSize);
		mustParseToInt(txtGraphNodeSize, namGraphNodeSize);
		mustParseToInt(txtChartPointSize, namChartPointSize);
	}

//-----------------------------------------------------------------------------
	
	private void mustParseToInt(JTextField field, String name)
	{
		try {
			Integer.parseInt(field.getText());
		} catch (Exception e) {
			inputIsOK = false;
			JOptionPane.showMessageDialog(null,
					"<html>Unacceptable value for '" + name + "'<br>"
					+ "<br>The value should be an integer.</html>",
	                "Error",
	                JOptionPane.ERROR_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
		}
	}
	
//-----------------------------------------------------------------------------
	
	private void storeValues()
	{
		GUIPreferences.graphLabelFontSize = 
				Integer.parseInt(txtGraphTxtSize.getText());
		GUIPreferences.graphNodeSize =
                Integer.parseInt(txtGraphNodeSize.getText());
        GUIPreferences.chartPointSize =
                Integer.parseInt(txtChartPointSize.getText());
	}
}
