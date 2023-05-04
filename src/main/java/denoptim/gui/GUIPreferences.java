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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.openscience.cdk.CDKConstants;

import denoptim.constants.DENOPTIMConstants;

/**
 * The collection of tunable preferences.
 * 
 * @author Marco Foscato
 */

public class GUIPreferences {

	/**
	 * Graph visualization: font size of labels
	 */
	protected static int graphLabelFontSize = 10;
	
	/**
	 * Graph visualization: size of nodes
	 */
	protected static int graphNodeSize = 30;
	
	/**
	 * Evolutionary Inspector: size of points
	 */
	protected static int chartPointSize = 8;
	
	/**
	 * MolecularViewer: list of SDF tags specifying which properties to display.
	 */
	public static TreeSet<String> chosenSDFTags = new TreeSet<String>();
	
	/**
	 * MolecularViewer: default list of SDF tags with corresponding string
	 * to display instead of tag.
	 */
	public static Map<String,String> defualtSDFTags;
	static {
		defualtSDFTags = new HashMap<String,String>();
		defualtSDFTags.put(CDKConstants.TITLE,"Name");
		defualtSDFTags.put(DENOPTIMConstants.UNIQUEIDTAG,"UID");
		defualtSDFTags.put(DENOPTIMConstants.FITNESSTAG,"Fitness");
		defualtSDFTags.put(DENOPTIMConstants.MOLERRORTAG,"Error");
		defualtSDFTags.put("Generation","Generation");
		defualtSDFTags.put(DENOPTIMConstants.PROVENANCE,"Origin");
		for (String s : defualtSDFTags.keySet())
		{
			chosenSDFTags.add(s);
		}
	}
	
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
	
	/**
	 * Choice of displaying legend in evolution plot
	 */
	protected static boolean showLegenInEvolutionPlot = false;
	
    /**
     * Choice of displaying legend in monitor plot
     */
    protected static boolean showLegenInMonitorPlot = false;
    
    /**
     * File with last used cutting rules
     */
    protected static File lastCutRulesFile;
    
}
