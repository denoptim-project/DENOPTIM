package gui;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;

public class DenoptimGUIFileOpener {
	JFileChooser fileChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory()); 
	 
	public void pickFile() 
	{
		if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
		{
			File file = fileChooser.getSelectedFile();
			System.out.println("TODO: read file "+file);
		}
		else
		{
			//Nothing
			//System.out.println("No file selected");
		}
	}
}
