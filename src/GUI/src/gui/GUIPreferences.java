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

/**
 * The collection of tunable preferences.
 * 
 * @author Marco Foscato
 */

public class GUIPreferences {

	/**
	 * DENOPTIMGraph visualization: font size of labels
	 */
	protected static int graphLabelFontSize = 10;
	
	/**
	 * DENOPTIMGraph visualization: size of nodes
	 */
	protected static int graphNodeSize = 30;
	
	/**
	 * Evolutionary Inspector: size of points
	 */
	protected static int chartPointSize = 8;
	
	/**
	 * Readable/writable space for tmp files
	 */
	protected static String tmpSpace = "/tmp";

	/**
	 * Available engines used to do SMILES-to-3D conversion
	 */
	protected enum SMITo3DEngine {CACTVS, CDK};
	
	/**
	 * Selects the engine used to do SMILES-to-3D conversion
	 */
	protected static SMITo3DEngine smiTo3dResolver = 
			SMITo3DEngine.CACTVS;
}
