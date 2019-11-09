package gui;

import java.awt.Dimension;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;


/**
 * General structure of a form for collecting input parameters of various nature.
 * 
 * @author Marco Foscato
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
    
    /**
     * Empty constructor
     */
    public ParametersForm()
    {
    }
    
    /**
     * Produced the KEY:VALUE string for a general text field. 
     * The text can include numbers, characters, or both.
     * @param key the keyword
     * @param field the txt field
     * @return the KEY:VALUE string including newline character at the end.
     */
    
    protected String getStringIfNotEmpty(String key, JTextField field)
    {
    	StringBuilder sb = new StringBuilder();
    	if (!field.getText().equals(""))
    	{
    		sb.append(key).append("=").append(field.getText()).append(NL);
    	}
    	return sb.toString();
    }
    
    /**
     * Produced the KEY:VALUE string for a on/off button
     * @param key the keyword
     * @param btn the on/off button
     * @return the KEY:VALUE string including newline character at the end.
     */
    protected String getStringIfSelected(String key, JRadioButton btn)
    {
    	StringBuilder sb = new StringBuilder();
    	if (btn.isSelected())
    	{
    		sb.append(key).append("=").append("SELECTED_by_GUI").append(NL);
    	}
    	return sb.toString();
    }
    
    /**
     * Produced an overall string including one or more KEY:VALUE strings taken from a table.
     * By default this method takes as VALUE only the first field of the table.
     * @param key the keyword
     * @param tab the table
     * @return the overall string including one or more KEY:VALUE string, and newline character at the end.
     */
    protected String getStringFromTable(String key, JTable tab)
    {
    	return getStringFromTable(key, tab, new int[]{0});
    }
    
    /**
     * Produced an overall string including one or more KEY:VALUE strings taken from a table.
     * @param key the keyword
     * @param tab the table
     * @param fields the id (0, n-1) of the fields to use as space-separated values
     * @return the overall string including one or more KEY:VALUE string, and newline character at the end.
     */
    protected String getStringFromTable(String key, JTable tab, int[] fields)
    {
    	return getStringFromTable(key, tab, fields, " ");
    }
    
    /**
     * Produced an overall string including one or more KEY:VALUE strings taken from a table.
     * @param key the keyword
     * @param tab the table
     * @param fields the indexes (0, n-1) of the fields to use as separated VALUEs
     * @param sep string separator for fields of a single VALUE
     * @return the overall string including one or more KEY:VALUE string, and newline character at the end.
     */
    protected String getStringFromTable(String key, JTable tab, int[] fields, String sep)
    {
    	StringBuilder sb = new StringBuilder();
    	for (int i=0; i<tab.getRowCount(); i++)
    	{
    		String val = "";
    		for (int j=0; j<fields.length; j++)
    		{
    			if (j==0)
    			{
    				val = tab.getValueAt(i, fields[j]).toString();
    			}    
    			else
    			{
    				val = val + sep + tab.getValueAt(i, fields[j]).toString();
    			}
    		}
    		sb.append(key).append("=").append(val).append(NL);
    	}
    	return sb.toString();
    }  

	@Override
	public void putParametersToString(StringBuilder sb) 
	{
	}
}
