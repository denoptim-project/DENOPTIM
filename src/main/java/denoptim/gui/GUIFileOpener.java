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

import java.awt.Component;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

/**
 * File opener for DENOPTIM GUI
 */

public class GUIFileOpener 
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

	public static File pickFileForTxtField(JTextField txtField, Component parent) 
	{
		File file = pickFile(parent);
		if (file != null)
		{
		    txtField.setText(file.getAbsolutePath());
		}
		return file;
	}
	
//-----------------------------------------------------------------------------

	public static Set<File> pickManyFiles(Component parent) 
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
	    fileChooser.resetChoosableFileFilters();
        fileChooser.setAcceptAllFileFilterUsed(true);
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		File file = null;
		if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION)
		{
			file = fileChooser.getSelectedFile();
		}
		return file;
	}
	
//-----------------------------------------------------------------------------
    
    public static File pickFileWithGraph(Component parent) 
    {
        fileChooser.resetChoosableFileFilters();
        fileChooser.setAcceptAllFileFilterUsed(true);
        FileNameExtensionFilter jsonFilter = new FileNameExtensionFilter(
                "JSON", "json");
        fileChooser.addChoosableFileFilter(jsonFilter);
        FileNameExtensionFilter sdfFilter = new FileNameExtensionFilter(
                "SDF", "sdf");
        fileChooser.addChoosableFileFilter(sdfFilter);
        fileChooser.setAcceptAllFileFilterUsed(true);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        File file = null;
        if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION)
        {
            file = fileChooser.getSelectedFile();
        }
        return file;
    }
		
//-----------------------------------------------------------------------------
		
	public static File pickFolderForTxtField(JTextField txtField, 
			Component parent) 
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
	
	public static File pickFileForSaving(Component parent) 
	{
		File file;
		if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION)
		{
			file = fileChooser.getSelectedFile();
		}
		else
		{
			/*
			JOptionPane.showMessageDialog(parent,
					"Could not save. Try again.",
	                "Error",
	                JOptionPane.ERROR_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
	                */
			return null;
		}
		return file;
	}
	
//-----------------------------------------------------------------------------

}
