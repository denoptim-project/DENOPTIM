package gui;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;

import denoptim.io.DenoptimIO;


/**
 * General structure of a form for collecting input parameters of various 
 * nature.
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
     * Default sizes for file pathname labels
     */
    final Dimension fileLabelSize = new Dimension(250,28);
        
    /**
     * Default text field height
     */
    final int preferredHeight = 
    		(int) (new JTextField()).getPreferredSize().getHeight();
        
    /**
     * Default sizes for file pathname fields
     */
    final Dimension fileFieldSize = new Dimension(350,preferredHeight);

    /**
     * Default sizes for short pathname fields (i.e., string or number)
     */
    final Dimension strFieldSize = new Dimension(75,preferredHeight);
        
    /**
     * The newline character 
     */
    private final String NL = System.getProperty("line.separator");
    
//-----------------------------------------------------------------------------
    
    /**
     * Empty constructor
     */
    
    public ParametersForm()
    {
    }
    
//-----------------------------------------------------------------------------
    
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
    
//-----------------------------------------------------------------------------
    
    /**
     * Produced the KEY:VALUE string for a general spinner. 
     * The text can include numbers, characters, or both.
     * @param key the keyword
     * @param field the txt field
     * @return the KEY:VALUE string including newline character at the end.
     */
    
    protected String getStringForKVLine(String key, JSpinner spinner)
    {
    	StringBuilder sb = new StringBuilder();
    	String value = spinner.getValue().toString();
    	if (!value.equals(""))
    	{
    		sb.append(key).append("=").append(value).append(NL);
    	}
    	return sb.toString();
    }
    
//-----------------------------------------------------------------------------
    
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
    
//-----------------------------------------------------------------------------
    
    /**
     * Produced an overall string including one or more KEY:VALUE strings taken
     * from a table.
     * By default this method takes as VALUE only the first field of the table.
     * @param key the keyword
     * @param tab the table
     * @return the overall string including one or more KEY:VALUE string, and 
     * newline character at the end.
     */
    
    protected String getStringFromTable(String key, JTable tab)
    {
    	return getStringFromTable(key, tab, new int[]{0}, false);
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Produced an overall string including one or more KEY:VALUE strings taken 
     * from a table.
     * By default this method takes as VALUE only the first field of the table.
     * @param key the keyword
     * @param tab the table
     * @param skipHeader use <code>true</code> to ignore first row
     * @return the overall string including one or more KEY:VALUE string, and 
     * newline character at the end.
     */
    
    protected String getStringFromTable(String key, JTable tab, 
    		boolean skipHeader)
    {
    	return getStringFromTable(key, tab, new int[]{0}, skipHeader);
    }
  
//-----------------------------------------------------------------------------
    
    /**
     * Produced an overall string including one or more KEY:VALUE strings taken
     *  from a table.
     * @param key the keyword
     * @param tab the table
     * @param fields the id (0, n-1) of the fields to use as space-separated 
     * values
     * @param skipHeader use <code>true</code> to ignore first row
     * @return the overall string including one or more KEY:VALUE string, and 
     * newline character at the end.
     */
    
    protected String getStringFromTable(String key, JTable tab, int[] fields, 
    		boolean skipHeader)
    {
    	return getStringFromTable(key, tab, fields, " ", skipHeader);
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Produced an overall string including one or more KEY:VALUE strings taken
     *  from a table.
     * @param key the keyword
     * @param tab the table
     * @param fields the indexes (0, n-1) of the fields to use as separated 
     * VALUEs
     * @param sep string separator for fields of a single VALUE
     * @param skipHeader use <code>true</code> to ignore first row
     * @return the overall string including one or more KEY:VALUE string, and 
     * newline character at the end.
     */
    
    protected String getStringFromTable(String key, JTable tab, int[] fields,
    		String sep, boolean skipHeader)
    {
    	StringBuilder sb = new StringBuilder();
    	int firstRow = 0;
    	if (skipHeader)
    	{
    		firstRow = 1;
    	}
    	for (int i=firstRow; i<tab.getRowCount(); i++)
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
    
//-----------------------------------------------------------------------------
    
    public void importParametersFromDenoptimParamsFile(String fileName) 
    		throws Exception
    {
    }
    
//-----------------------------------------------------------------------------
    
    /**
     * Reads in the parameters from a text file collecting  
     * DENOPTIM parameters in the form of KEY=VALUE lines.
     * @param fileName pathname to the parameters file
     * @param keyRoot initial part of keyword to select lines pertaining the 
     * present parameters set. For instance "GA-", "FSE-", etc.
     */
    
    protected void importParametersFromDenoptimParamsFile(String fileName, 
    		String keyRoot) throws Exception
    {  	    	
    	if (keyRoot.equals("") || keyRoot == null)
    	{
    		throw new Exception("<html>Root of DENOPTIM keywords not defined."
    				+ "<br>Bugus use of method "
    				+ "'importParametersFromDenoptimParamsFile'!</html>");
    	}
    	String parType = keyRoot.replaceAll("-", "");
    	if (fileName.equals("") || !DenoptimIO.checkExists(fileName))
    	{
    		throw new Exception("<html>Source file for the " + parType 
    				+ " parameters is not found!<br>Please, specify another"
    				+ " file name.");
    	}
    	
        String line;
        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(fileName));
            while ((line = br.readLine()) != null)
            {	
                if ((line.trim()).length() == 0)
                {
                    continue;
                }

                if (line.startsWith("#"))
                {
                    continue;
                }
                
                if (line.toUpperCase().startsWith(keyRoot.toUpperCase()))
                {              	
                    importParameterFromLine(line);
                }
            }
        }
        catch (IOException ioe)
        {
        	throw new Exception("Unable to read file '" + fileName + "'", ioe);
        }
        finally
        {
            try
            {
                if (br != null)
                {
                    br.close();
                    br = null;
                }
            }
            catch (IOException ioe)
            {
            	throw new Exception("Unable to close file '" + fileName + "'",
            			ioe);
            }
        }
    }

//-----------------------------------------------------------------------------
    
  	protected void importParameterFromLine(String line) throws Exception
  	{
        String key = line.trim();
        String value = "";
        if (line.contains("="))
        {
            key = line.substring(0,line.indexOf("=")).trim();
            value = line.substring(line.indexOf("=") + 1).trim();
        }
        importSingleParameter(key,value);
  	}
  
//-----------------------------------------------------------------------------
  	
  	@Override
	public void importSingleParameter(String key, String value) 
			throws Exception 
  	{	
	}

//-----------------------------------------------------------------------------

	@Override
	public void putParametersToString(StringBuilder sb) throws Exception
	{
	}
	
//-----------------------------------------------------------------------------
}
