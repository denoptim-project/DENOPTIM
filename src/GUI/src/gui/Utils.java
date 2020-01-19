package gui;

import javax.swing.JOptionPane;

import denoptim.io.DenoptimIO;

public class Utils 
{
	
	/**
	 * Returns the pathname to a tmp file. This method
	 * verifies the currently defined location for tmp files, and, 
	 * if that is not suitable, it tries to find a suitable alternative.
	 * @param tmpFileName a string without spaces that is used to name the 
	 * tmp file
	 * @return
	 */
	public static String getTempFile(String tmpFileName)
	{
		String tmpSDFFile = GUIPreferences.tmpSpace
				+ System.getProperty("file.separator")
				+ tmpFileName;
		
		if (!DenoptimIO.canWriteAndReadTo(tmpSDFFile))
		{
			String tmpFolder = DenoptimIO.getTempFolder();
			tmpSDFFile = tmpFolder + System.getProperty("file.separator")
					+ tmpFileName;
			if (!DenoptimIO.canWriteAndReadTo(tmpSDFFile))
			{		
				String preStr = "Could not find a location for temprorary"
						+ " files automatically ";
				while (!DenoptimIO.canWriteAndReadTo(tmpSDFFile))
				{
					tmpFolder = JOptionPane.showInputDialog("<html>" + preStr
						+ "<br>Please, "
						+ "specify the absolute path of a folder I can use:");
					
					if (tmpFolder == null)
					{
						tmpFolder = "";
					}
					
					tmpFolder = tmpFolder.replaceAll("\\\\","/"); 
					//NB: '/' is properly interpreted by Jmol even in Windows.
					
					preStr = "I tried, but I cannot use '" + tmpSDFFile + "'.";
					
					tmpSDFFile = tmpFolder 
							+ System.getProperty("file.separator")
							+ tmpFileName;				
				}
			}
			GUIPreferences.tmpSpace = tmpFolder;
		}
		return tmpSDFFile;
	}

}
