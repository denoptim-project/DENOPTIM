package gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileSystemView;
import javax.swing.JTextField;

import org.apache.commons.io.FilenameUtils;

/**
 * File opener for DENOPTIM GUI
 */

public class DenoptimGUIFileOpener 
{

//-----------------------------------------------------------------------------

	public static File pickFile(JTextField txtField) 
	{
		File file = pickFile();
		if (file != null)
		{
		    txtField.setText(file.getAbsolutePath());
		}
		return file;
	}
	
//-----------------------------------------------------------------------------
	
	public static File pickFile() 
	{
		JFileChooser fileChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory()); 
		File file;
		if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
		{
			file = fileChooser.getSelectedFile();
		}
		else
		{
			return null;
		}
		return file;
	}
	
//-----------------------------------------------------------------------------
	
	public static File pickFolder(JTextField txtField) 
	{
		File file = pickFolder();
		if (file != null)
		{
		    txtField.setText(file.getAbsolutePath());
		}
		return file;
	}
	
//-----------------------------------------------------------------------------
	
	public static File pickFolder() 
	{
		JFileChooser fileChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		File file;
		if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
		{
			file = fileChooser.getSelectedFile();
		}
		else
		{
			return null;
		}
		return file;
	}
	
//-----------------------------------------------------------------------------
	
	public static File saveFile() 
	{
		JFileChooser fileChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
		File file;
		if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
		{
			file = fileChooser.getSelectedFile();
		}
		else
		{
			JOptionPane.showMessageDialog(null,
					"Could not save. Try again.",
	                "Error",
	                JOptionPane.ERROR_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return null;
		}
		return file;
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Inspects a text-based file and tries to detect if the file is one of
	 * the type recognized by DENOPTIM GUI.
	 * @param inFile the file to inspect
	 * @return a string informing on the detected file format, or null.
	 */
	
	public static String detectFileFormat(File inFile) throws Exception
	{
		String fType = null;
		
		String ext = FilenameUtils.getExtension(inFile.getAbsolutePath());
		
		//TODO: define class DenoptinFileType and subclasses for .par .sdf .ser etc...
		//TODO: All this should probably be in a the DENOPTIM package under io (including this method)
		
		switch (ext)
		{
		case "sdf":
			//TODO: FRAGMENT or GRAPH
			fType = "FRAGMET";
			fType = "DGRAPH";
			break;
		
		case "ser":
			//TODO serialized graph
			fType = "SERDGRAPH";
			break;
		
		case "par":
			//Parameters for any DENOPTIM module
			fType = detectKindOfParameterFile(inFile.getAbsolutePath());
		    break;
		    
		default:
			throw new Exception("Unrecognized file extension '" + ext + "'.");
		}
		
		return fType;
	}

//-----------------------------------------------------------------------------
	
	public static String detectKindOfParameterFile(String fileName) 
			throws Exception
	{
		//TODO: move this method to denoptim.io
	
		String fType = null;
		
		Map<String,String> determiningKeysMap = new HashMap<String,String>();
		determiningKeysMap.put("GA-","GA-PARAMS");
		determiningKeysMap.put("FSE-","FSE-PARAMS");
		
        String line;
        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(fileName));
            lineReadingLoop:
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
	                
	                for (String keyRoot : determiningKeysMap.keySet())
	                {
	                    if (line.toUpperCase().startsWith(keyRoot.toUpperCase()))
	                    {
	                    	fType = determiningKeysMap.get(keyRoot);
	                    	break lineReadingLoop;
	                    }
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
		
		return fType;
	}

//-----------------------------------------------------------------------------

}
