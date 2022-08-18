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
import java.awt.Frame;
import java.awt.Dialog;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import denoptim.files.FileUtils;

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
		
		if (!FileUtils.canWriteAndReadTo(tmpSDFFile))
		{
			String tmpFolder = FileUtils.getTempFolder();
			tmpSDFFile = tmpFolder + System.getProperty("file.separator")
					+ tmpFileName;
			if (!FileUtils.canWriteAndReadTo(tmpSDFFile))
			{		
				String preStr = "Could not find a location for temprorary"
						+ " files automatically ";
				while (!FileUtils.canWriteAndReadTo(tmpSDFFile))
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

//------------------------------------------------------------------------------
	
	

}
