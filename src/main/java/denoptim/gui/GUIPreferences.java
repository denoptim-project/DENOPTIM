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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;

/**
 * The collection of tunable preferences.
 * 
 * @author Marco Foscato
 */

public class GUIPreferences
{
	/**
	 * Available engines used to do SMILES-to-3D conversion
	 */
	protected enum SMITo3DEngine {CACTVS, CDK};

    /**
	 * Available representations for molecular structures
	 */
	protected enum MolDimDisplay {THREE_DIMENSIONAL, TWO_DIMENSIONAL};

    /**
     * Factory-default values for all tunable preferences.
     */
    public static final class Defaults
    {
        public static final int GRAPH_LABEL_FONT_SIZE = 10;
        public static final int GRAPH_NODE_SIZE = 30;
        public static final int CHART_POINT_SIZE = 8;
        public static final String TMP_SPACE = "/tmp";
        public static final SMITo3DEngine SMI_TO_3D_RESOLVER =
                SMITo3DEngine.CACTVS;
        public static final MolDimDisplay MOL_DIM_DISPLAY =
                MolDimDisplay.THREE_DIMENSIONAL;
        public static final boolean SHOW_LEGEND_IN_EVOLUTION_PLOT = false;
        public static final boolean SHOW_LEGEND_IN_MONITOR_PLOT = false;
        public static final TreeSet<String> CHOSEN_SDF_TAGS;
        static {
            CHOSEN_SDF_TAGS = new TreeSet<String>();
            CHOSEN_SDF_TAGS.add(CDKConstants.TITLE);
            CHOSEN_SDF_TAGS.add(DENOPTIMConstants.UNIQUEIDTAG);
            CHOSEN_SDF_TAGS.add(DENOPTIMConstants.FITNESSTAG);
            CHOSEN_SDF_TAGS.add(DENOPTIMConstants.MOLERRORTAG);
            CHOSEN_SDF_TAGS.add("Generation");
            CHOSEN_SDF_TAGS.add(DENOPTIMConstants.PROVENANCE);
        }
    }

	/**
	 * Graph visualization: font size of labels
	 */
	protected static int graphLabelFontSize = Defaults.GRAPH_LABEL_FONT_SIZE;
	
	/**
	 * Graph visualization: size of nodes
	 */
	protected static int graphNodeSize = Defaults.GRAPH_NODE_SIZE;
	
	/**
	 * Evolutionary Inspector: size of points
	 */
	protected static int chartPointSize = Defaults.CHART_POINT_SIZE;
	
	/**
	 * MolecularViewer: list of SDF tags specifying which properties to display.
	 */
	public static TreeSet<String> chosenSDFTags = new TreeSet<String>(
	        Defaults.CHOSEN_SDF_TAGS);
	
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
	}
	
	/**
	 * Readable/writable space for tmp files
	 */
	protected static String tmpSpace = Defaults.TMP_SPACE;
	
	/**
	 * Selects the engine used to do SMILES-to-3D conversion
	 */
	protected static SMITo3DEngine smiTo3dResolver = Defaults.SMI_TO_3D_RESOLVER;
	
	/**
	 * Selects the representation for molecular structures
	 */
	protected static MolDimDisplay molDimDisplay = Defaults.MOL_DIM_DISPLAY;
	
	/**
	 * Choice of displaying legend in evolution plot
	 */
	protected static boolean showLegenInEvolutionPlot =
	        Defaults.SHOW_LEGEND_IN_EVOLUTION_PLOT;
	
    /**
     * Choice of displaying legend in monitor plot
     */
    protected static boolean showLegenInMonitorPlot =
            Defaults.SHOW_LEGEND_IN_MONITOR_PLOT;
    
    /**
     * File with last used cutting rules
     */
    protected static File lastCutRulesFile = null;
    
//-----------------------------------------------------------------------------

    /**
     * Serializable snapshot of the tunable GUI preferences.
     */
    public static class PreferencesData
    {
        int graphLabelFontSize = Defaults.GRAPH_LABEL_FONT_SIZE;
        int graphNodeSize = Defaults.GRAPH_NODE_SIZE;
        int chartPointSize = Defaults.CHART_POINT_SIZE;
        TreeSet<String> chosenSDFTags = new TreeSet<String>(
                Defaults.CHOSEN_SDF_TAGS);
        String tmpSpace = Defaults.TMP_SPACE;
        SMITo3DEngine smiTo3dResolver = Defaults.SMI_TO_3D_RESOLVER;
        MolDimDisplay molDimDisplay = Defaults.MOL_DIM_DISPLAY;
        boolean showLegenInEvolutionPlot =
                Defaults.SHOW_LEGEND_IN_EVOLUTION_PLOT;
        boolean showLegenInMonitorPlot = Defaults.SHOW_LEGEND_IN_MONITOR_PLOT;
        String lastCutRulesFile = null;
    }
    
//-----------------------------------------------------------------------------

    private static Gson gson()
    {
        return new GsonBuilder().setPrettyPrinting().create();
    }
    
//-----------------------------------------------------------------------------

    /**
     * Builds a snapshot of the current in-memory preferences.
     */
    public static PreferencesData toData()
    {
        PreferencesData data = new PreferencesData();
        data.graphLabelFontSize = graphLabelFontSize;
        data.graphNodeSize = graphNodeSize;
        data.chartPointSize = chartPointSize;
        data.chosenSDFTags = new TreeSet<String>(chosenSDFTags);
        data.tmpSpace = tmpSpace;
        data.smiTo3dResolver = smiTo3dResolver;
        data.molDimDisplay = molDimDisplay;
        data.showLegenInEvolutionPlot = showLegenInEvolutionPlot;
        data.showLegenInMonitorPlot = showLegenInMonitorPlot;
        data.lastCutRulesFile = lastCutRulesFile == null ? null
                : lastCutRulesFile.getAbsolutePath();
        return data;
    }
    
//-----------------------------------------------------------------------------

    /**
     * Applies a preferences snapshot to the in-memory static fields.
     */
    public static void applyData(PreferencesData data)
    {
        if (data == null)
        {
            return;
        }
        graphLabelFontSize = data.graphLabelFontSize;
        graphNodeSize = data.graphNodeSize;
        chartPointSize = data.chartPointSize;
        chosenSDFTags.clear();
        if (data.chosenSDFTags != null)
        {
            chosenSDFTags.addAll(data.chosenSDFTags);
        }
        if (data.tmpSpace != null && !data.tmpSpace.isEmpty())
        {
            tmpSpace = data.tmpSpace;
        }
        if (data.smiTo3dResolver != null)
        {
            smiTo3dResolver = data.smiTo3dResolver;
        }
        if (data.molDimDisplay != null)
        {
            molDimDisplay = data.molDimDisplay;
        }
        showLegenInEvolutionPlot = data.showLegenInEvolutionPlot;
        showLegenInMonitorPlot = data.showLegenInMonitorPlot;
        if (data.lastCutRulesFile != null && !data.lastCutRulesFile.isEmpty())
        {
            lastCutRulesFile = new File(data.lastCutRulesFile);
        } else {
            lastCutRulesFile = null;
        }
    }
    
//-----------------------------------------------------------------------------

    /**
     * Resets all tunable preferences to the factory defaults (in memory only).
     */
    public static void applyFactoryDefaults()
    {
        applyData(new PreferencesData());
    }
    
//-----------------------------------------------------------------------------

    /**
     * Writes the given preferences snapshot to a JSON file.
     */
    public static void writeToFile(File file, PreferencesData data)
            throws DENOPTIMException
    {
        DenoptimIO.writeData(file.getAbsolutePath(), gson().toJson(data),
                false);
    }
    
//-----------------------------------------------------------------------------

    /**
     * Reads a preferences snapshot from a JSON file.
     */
    public static PreferencesData readFromFile(File file)
            throws DENOPTIMException
    {
        String json = DenoptimIO.readText(file.getAbsolutePath());
        return gson().fromJson(json, PreferencesData.class);
    }
    
//-----------------------------------------------------------------------------

    /**
     * Saves the current in-memory preferences to
     * {@link DENOPTIMConstants#GUIPREFERENCESFILE}.
     */
    public static void save()
    {
        try {
            writeToFile(DENOPTIMConstants.GUIPREFERENCESFILE, toData());
        } catch (Exception e) {
            System.err.println("WARNING: could not save GUI preferences to '"
                    + DENOPTIMConstants.GUIPREFERENCESFILE.getAbsolutePath()
                    + "': " + e.getMessage());
        }
    }
    
//-----------------------------------------------------------------------------

    /**
     * Ensures the defaults JSON file exists (writes factory defaults if
     * missing), then loads {@link DENOPTIMConstants#GUIPREFERENCESFILE} into
     * memory. If the preferences file is missing, it is created from the
     * defaults file.
     */
    public static void loadAtStartup()
    {
        ensureDefaultsFileExists();
        File prefsFile = DENOPTIMConstants.GUIPREFERENCESFILE;
        if (!prefsFile.exists())
        {
            try {
                PreferencesData defaults = readFromFile(
                        DENOPTIMConstants.GUIPREFERENCESDEFAULTSFILE);
                writeToFile(prefsFile, defaults);
            } catch (Exception e) {
                try {
                    writeToFile(prefsFile, new PreferencesData());
                } catch (Exception e2) {
                    System.err.println("WARNING: could not create GUI "
                            + "preferences file '"
                            + prefsFile.getAbsolutePath() + "': "
                            + e2.getMessage());
                    applyFactoryDefaults();
                    return;
                }
            }
        }
        try {
            applyData(readFromFile(prefsFile));
        } catch (Exception e) {
            System.err.println("WARNING: could not read GUI preferences from '"
                    + prefsFile.getAbsolutePath()
                    + "'. Using factory defaults. Cause: " + e.getMessage());
            applyFactoryDefaults();
        }
    }
    
//-----------------------------------------------------------------------------

    /**
     * Writes factory defaults to
     * {@link DENOPTIMConstants#GUIPREFERENCESDEFAULTSFILE} if that file does
     * not exist yet.
     */
    public static void ensureDefaultsFileExists()
    {
        File defaultsFile = DENOPTIMConstants.GUIPREFERENCESDEFAULTSFILE;
        if (defaultsFile.exists())
        {
            return;
        }
        try {
            writeToFile(defaultsFile, new PreferencesData());
        } catch (Exception e) {
            System.err.println("WARNING: could not create GUI preferences "
                    + "defaults file '" + defaultsFile.getAbsolutePath()
                    + "': " + e.getMessage());
        }
    }
    
//-----------------------------------------------------------------------------

    /**
     * Restores preferences from
     * {@link DENOPTIMConstants#GUIPREFERENCESDEFAULTSFILE} (or factory
     * defaults if that file cannot be read), applies them in memory, and
     * overwrites the current preferences file.
     */
    public static void restoreDefaults()
    {
        PreferencesData defaults = null;
        File defaultsFile = DENOPTIMConstants.GUIPREFERENCESDEFAULTSFILE;
        if (defaultsFile.exists())
        {
            try {
                defaults = readFromFile(defaultsFile);
            } catch (Exception e) {
                System.err.println("WARNING: could not read GUI preferences "
                        + "defaults from '" + defaultsFile.getAbsolutePath()
                        + "': " + e.getMessage());
            }
        }
        if (defaults == null)
        {
            defaults = new PreferencesData();
            ensureDefaultsFileExists();
        }
        applyData(defaults);
        save();
    }
    
//-----------------------------------------------------------------------------

}
