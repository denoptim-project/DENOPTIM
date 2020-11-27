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

import java.awt.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileSystemView;
import javax.swing.JTextField;

import org.apache.commons.io.FilenameUtils;

import denoptim.constants.DENOPTIMConstants;

/**
 * File opener for DENOPTIM GUI
 */

public class DenoptimGUIFileOpener 
{
	protected static JFileChooser fileChooser = new JFileChooser(
			FileSystemView.getFileSystemView().getHomeDirectory()); 
	
//-----------------------------------------------------------------------------

	public static File pickFileOrFolder(Component parent)
	{
		fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		File file = null;
		if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION)
		{
			file = fileChooser.getSelectedFile();
		}
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		return file;
	}

//-----------------------------------------------------------------------------

	public static File pickFile(JTextField txtField, Component parent) 
	{
		File file = pickFile(parent);
		if (file != null)
		{
		    txtField.setText(file.getAbsolutePath());
		}
		return file;
	}
	
//-----------------------------------------------------------------------------

	public static Set<File> pickManyFile(Component parent) 
	{
		fileChooser.setMultiSelectionEnabled(true);
		fileChooser.setDialogTitle("Select one or more files");
		Set<File> files = null;
		if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION)
		{
			files = new HashSet<File>();
			File[] arr = fileChooser.getSelectedFiles();
			for (int i=0; i<arr.length; i++)
			{
				files.add(arr[i]);
			}
		}
		fileChooser.setDialogTitle("Open");
		fileChooser.setMultiSelectionEnabled(false);
		return files;
	}
	
//-----------------------------------------------------------------------------
	
	public static File pickFile(Component parent) 
	{
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		File file = null;
		if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION)
		{
			file = fileChooser.getSelectedFile();
		}
		return file;
	}
		
//-----------------------------------------------------------------------------
		
	public static File pickFolder(JTextField txtField, Component parent) 
	{
		File file = pickFolder(parent);
		if (file != null)
		{
		    txtField.setText(file.getAbsolutePath());
		}
		return file;
	}
	
//-----------------------------------------------------------------------------
	
	public static File pickFolder(Component parent) 
	{
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileChooser.setDialogTitle("Choose Folder to Load");
		File file = null;
		if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION)
		{
			file = fileChooser.getSelectedFile();
		}
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setDialogTitle("Open");
		return file;
	}
	
//-----------------------------------------------------------------------------
	
	public static File saveFile(Component parent) 
	{
		File file;
		if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION)
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
		//TODO Junit test
		
		// Folders are presumed to contain output kind of data
		if (inFile.isDirectory())
		{
			
			// This is to distinguish GS from FSE runs
			for(File folder : inFile.listFiles(new FileFilter() {
				
				@Override
				public boolean accept(File pathname) {
					if (pathname.isDirectory())
					{
						return true;
					}
					return false;
				}
			}))
			{
				if (folder.getName().startsWith(
						DENOPTIMConstants.FSEIDXNAMEROOT))
				{
					fType = "FSE-RUN";
					return fType;
				}
				else if (folder.getName().startsWith(
						DENOPTIMConstants.GAGENDIRNAMEROOT))
				{
					fType = "GA-RUN";
					return fType;
				}
			}
	
			throw new Exception("Could not recognized data in the folder '" 
					+ ext + "'.");
		}
		
		// Files are distinguished first by extension
		switch (ext)
		{
			/*
			case "txt":
				//Human readable graph as text files are too a-specific
				//TODO add something specific, like "GraphENC" at beginning 
				//of line.
				fType = "GRAPHS";
				break;
			*/		
				
			case "sdf":
				//Either graphs or fragment
				fType = detectKindOfSDFFile(inFile.getAbsolutePath());
				break;
			
			case "ser":
				//Serialized graph
				fType = "SERGRAPH";
				break;
			
			case "par":
				//Parameters for any DENOPTIM module
				fType = detectKindOfParameterFile(inFile.getAbsolutePath());
			    break;
			    
			default:
				throw new Exception("Unrecognized file extension '" + ext 
						+ "'.");
		}
		
		return fType;
	}

//-----------------------------------------------------------------------------

	/**
	 * Looks into a text file and tries to understand is the file is a 
	 * collection of parameters for any specific DENOPTIM module.
	 * 
	 * @param fileName The pathname of the file to analyze
	 * @return a string that defined the kind of parameters
	 * @throws Exception
	 */
	public static String detectKindOfSDFFile(String fileName) 
			throws Exception
	{
		//TODO: move this method to denoptim.io
		
		Map<String,String> determiningKeysMap = new HashMap<String,String>();
		determiningKeysMap.put("^> *<ATTACHMENT_POINT>.*","FRAGMENTS");
		determiningKeysMap.put("^> *<GraphENC>.*","GRAPHS");

		return detectKindFile(fileName, determiningKeysMap, "\\$\\$\\$\\$");
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Looks into a text file and tries to understand is the file is a 
	 * collection of parameters for any specific DENOPTIM module.
	 * 
	 * @param fileName The pathname of the file to analyze
	 * @return a string that defined the kind of parameters
	 * @throws Exception
	 */
	public static String detectKindOfParameterFile(String fileName) 
			throws Exception
	{
		//TODO: move this method to denoptim.io
		
		Map<String,String> determiningKeysMap = new HashMap<String,String>();
		determiningKeysMap.put("^GA-.*","GA-PARAMS");
		determiningKeysMap.put("^FSE-.*","FSE-PARAMS");
		determiningKeysMap.put("^RCN .*","CompatibilityMatrix");
		determiningKeysMap.put("^RBO .*","CompatibilityMatrix");
		determiningKeysMap.put("^CAP .*","CompatibilityMatrix");
		
		return detectKindFile(fileName, determiningKeysMap, null);
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Looks into a text file and tries to understand what it is given a
	 * a patterns-to-file kind map.
	 * 
	 * @param fileName The pathname of the file to analyze
	 * @param definingMap the map of regex (key) to file kind (values)
	 * @param endOfSample a pattern identifying the end of the sampled text.
	 * Use this to avoid reading long files that have no hope of matching any 
	 * known kind-defining criteria.
	 * @return a string that defined the kind of parameters, or null
	 * @throws Exception when something goes wrong handling the file
	 */
	public static String detectKindFile(String fileName, 
			Map<String,String> definingMap, String endOfSample) 
			throws Exception
	{
		//TODO: move this method to denoptim.io
	
		String fType = null;
        String line;
        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(fileName));
            lineReadingLoop:
	            while ((line = br.readLine()) != null)
	            {	            	
	            	if (endOfSample != null && line.matches(endOfSample))
	            	{
	            		break lineReadingLoop;
	            	}
	            	
	                if ((line.trim()).length() == 0)
	                {
	                    continue;
	                }
	                
	                for (String keyRoot : definingMap.keySet())
	                {
	                    if (line.matches(keyRoot))
	                    {
	                    	fType = definingMap.get(keyRoot);
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
