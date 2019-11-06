package gui;

import java.awt.Dimension;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;


/*
 * Form collecting input parameters for a genetic algorithm experiment
 */

public class ParametersForm extends JPanel implements IParametersForm
{
        
    /**
     * Version
     */
     private static final long serialVersionUID = 3787855399355076733L;
        
    /**
     * Storage of parameters defined in this form
     */
    Map<String,Object> params = new TreeMap<String,Object>();
        
    /**
     * Default sizes for file pathname labels
     */
    final Dimension fileLabelSize = new Dimension(250,28);
        
    /**
     * Default text field height
     */
    final int preferredHeight = (int) (new JTextField()).getPreferredSize().getHeight();
        
    /**
     * Default sizes for file pathname fields
     */
    final Dimension fileFieldSize = new Dimension(350,preferredHeight);

    /**
     * Default sizes for short pathname fields (i.e., string or number)
     */
    final Dimension strFieldSize = new Dimension(75,preferredHeight);
        
        
    private final String NL = System.getProperty("line.separator");
    
    public ParametersForm()
    {
    }
    
    protected String getStringIfNotEmpty(String key, JTextField field)
    {
    	StringBuilder sb = new StringBuilder();
    	if (!field.getText().equals(""))
    	{
    		sb.append(key).append("=").append(field.getText()).append(NL);
    	}
    	return sb.toString();
    }
    
    protected String getStringIfSelected(String key, JRadioButton btn)
    {
    	StringBuilder sb = new StringBuilder();
    	if (btn.isSelected())
    	{
    		sb.append(key).append("=").append("SELECTED_by_GUI").append(NL);
    	}
    	return sb.toString();
    }

	@Override
	public void putParametersToString(StringBuilder sb) {
	}
}
