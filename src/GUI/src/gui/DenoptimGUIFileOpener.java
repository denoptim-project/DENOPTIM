package gui;

import java.io.File;

/**
 * File opener for DENOPTIM GUI
 */

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;
import javax.swing.JTextField;

public class DenoptimGUIFileOpener {
	 
	public static File pickFile(JTextField txtField) 
	{
		File file = pickFile();
		if (file != null)
		{
		    txtField.setText(file.getAbsolutePath());
		}
		return file;
	}
	
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
			//TODO change to something sensible
			return null;
		}
		return file;
	}
	
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
			//TODO: insist
			return null;
		}
		return file;
	}
}
