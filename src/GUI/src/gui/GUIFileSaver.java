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
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

import denoptim.io.FileAndFormat;
import denoptim.io.FileFormat;
import denoptim.io.FileFormat.DataKind;

/**
 * GUI component to provide pathname where to save stuff.
 */

public class GUIFileSaver 
{
	private static JFileChooser fileChooser = new JFileChooser(
			FileSystemView.getFileSystemView().getHomeDirectory()); 
	
//-----------------------------------------------------------------------------

	public static FileAndFormat pickFileForSavingGraphs(Component parent) 
	{
		fileChooser.resetChoosableFileFilters();
		FileNameExtensionFilter jsonFilter = new FileNameExtensionFilter(
		        "JSON", "json");
		fileChooser.addChoosableFileFilter(jsonFilter);
		FileNameExtensionFilter sdfFilter = new FileNameExtensionFilter(
                "SDF", "sdf");
        fileChooser.addChoosableFileFilter(sdfFilter);
        fileChooser.setAcceptAllFileFilterUsed(false);
        File file;
        if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION)
		{
			file = fileChooser.getSelectedFile();
		}
		else
		{
			return null;
		}
        FileAndFormat ff = new FileAndFormat(file,FileFormat.fromString(
                fileChooser.getFileFilter().getDescription(), DataKind.GRAPH));
		return ff;
	}
	
//-----------------------------------------------------------------------------

}
