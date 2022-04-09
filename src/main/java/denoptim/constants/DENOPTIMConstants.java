/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no> and
 *   Marco Foscato <marco.foscato@uib.no>
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

package denoptim.constants;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openscience.cdk.interfaces.IAtomContainer;

import denoptim.exception.DENOPTIMException;
import denoptim.files.FileUtils;
import denoptim.graph.AttachmentPoint;
import denoptim.graph.Vertex;


/**
 * General set of constants used in DENOPTIM
 */

public final class DENOPTIMConstants 
{
    /**
     * new line character
     */
    public static final String EOL = System.getProperty("line.separator");

    /**
     * file separator  
     */
    public static final String FSEP = System.getProperty("file.separator");

    /**
     * Global configuration folder
     */
    public static final File GLOBALCONFIG = ensureConfigFolderExists();
    private static File ensureConfigFolderExists()
    {
        File configFolder = new File(
                System.getProperty("user.home") + FSEP + ".denoptim");
        try
        {
            if (!configFolder.exists())
            {
                boolean wasMade = FileUtils.createDirectory(
                        configFolder.getAbsolutePath());
                if (!wasMade)
                {
                    throw new DENOPTIMException("ERROR: could not make "
                            + "configuration folder '" 
                            + configFolder.getAbsolutePath() + "'");
                }
            }
        } catch (Throwable t)
        {
            t.printStackTrace();
            String msg = "ERROR: unable to make configuration folder '"  
                    + configFolder.getAbsolutePath() + "'";
            System.err.println(msg);
            throw new Error(msg);
        }
        return configFolder;
    }
    
    /**
     * List of recent files
     */
    public static final File RECENTFILESLIST = new File(
            GLOBALCONFIG.getAbsoluteFile() + FSEP + "recent_files_list");
 
    public static final Map<String, Integer> VALENCE_MAP = createMap();
    private static Map<String, Integer> createMap()
    {
        Map<String, Integer> result = new HashMap<>();
        result.put("C", 4);
        result.put("N", 3);
        result.put("S", 6);
        result.put("As", 5);
        result.put("Cd", 2);
        result.put("Hg", 2);
        result.put("Zn", 2);
        result.put("B", 3);
        result.put("Si", 4);
        result.put("Ge", 4);
        result.put("Se", 6);
        result.put("Te", 6);
        result.put("Sb", 5);
        result.put("Sn", 4);
        result.put("Cl", 1);
        result.put("Br", 1);
        result.put("F", 1);
        result.put("I", 1);
        result.put("P", 5);
        result.put("O", 2);
        return Collections.unmodifiableMap(result);
    };
    
    public static final Set<String> ORGANIC_SUBSET_ELEMENTS = 
                    new HashSet<>(Arrays.asList(new String[] {"C",  
                        "N", "S", "O", "P", "F", "Br", "Cl", "I", "B"}
                    ));

    public static final Set<String> ALL_ELEMENTS = 
                    new HashSet<>(Arrays.asList(new String[] {"H", "He",
			        "Li", "Be", "B", "C", "N", "O", "F", "Ne",
			        "Na", "Mg", "Al", "SiP", "S", "Cl", "Ar",
			        "K", "Ca", "Ga", "Ge", "As", "Se", "Br", "Kr",
			        "Rb", "Sr", "In", "Sn", "Sb", "Te", "I", "Xe",
			        "Cs", "Ba", "Tl", "Pb", "Bi", "Po", "At", "Rn",
			        "Fr", "Ra",
			        "Sc", "Ti", "V", "Cr", "Mn", "Fe", "Co", "Ni",
				"Cu", "Zn",
			        "Y", "Zr", "Nb", "Mo", "Tc", "Ru", "Rh", "Pd", 
				"Ag", "Cd",
			        "La", "Hf", "Ta", "W", "Re", "Os", "Ir", "Pt", 
				"Au", "Hg",
			        "Ac", "Ce", "Pr", "Nd", "Pm", "Sm", "Eu", "Gd",
				"Tb", "Dy",
			        "Ho", "Er", "Tm", "Yb", "Lu", "Th", "Pa", "U", 
				"Np", "Pu",
			        "Am", "Cm", "Bk", "Cf", "Es", "Fm", "Md", "No",
				"Lr"}));
    
    public static final Set<String> ALLOWED_ELEMENTS = 
                    new HashSet<>(Arrays.asList(new String[] {"C", "H", 
                        "N", "S", "O", "P", "F", "Br", "Cl", "I", "B", "As", 
                        "Si", "Sn", "Cd", "Hg", "Zn", "Te", "Ge", "Sb", "Se"}
                    ));

    public static final Set<String> ALLOWED_EXTENSIONS = 
                    new HashSet<>(Arrays.asList(new String[] {".txt", 
			".sdf", ".mol"}
                    ));
    
    /**
     * Keyword identifying compatibility matrix file lines with 
     * APClass compatibility rules
     */
    public static final String APCMAPCOMPRULE = "RCN";
    
    /**
     * Keyword identifying compatibility matrix file lines with 
     * APClass-to-Bond order map
     */
    public static final String APCMAPAP2BO = "RBO";
    
    /**
     * Keyword identifying compatibility matrix file lines with
     * forbidden ends
     */
    public static final String APCMAPFORBIDDENEND = "DEL";
    
    /**
     * Keyword identifying compatibility matrix file lines with
     * capping rules
     */
    public static final String APCMAPCAPPING = "CAP";
    
    /**
     * Keyword identifying compatibility matrix file lines with
     * comments
     */
    public static final String APCMAPIGNORE = "#";
    

    /**
     * Prefix for generation folders
     */
    public static final String GAGENDIRNAMEROOT = "Gen";
    
    /**
     * Prefix for graph indexing files
     */
    public static final String FSEIDXNAMEROOT = "FSE-Level_";
    
    /**
     * Prefix filenames  of serialized graphs
     */
    public static final String SERGFILENAMEROOT = "dg_";
    
    /**
     * Extension filenames of serialized graphs
     */
    public static final String SERGFILENAMEEXT = "json";
    
    /**
     * Prefix of filenames for input/output files related to fitness
     */
    public static final String FITFILENAMEPREFIX = "M";
    
    /**
     * Ending and extension of input file of external fitness provider
     */
    public static final String FITFILENAMEEXTIN = "_inp.sdf";
    
    /**
     * Ending and extension of output file of external fitness provider
     */
    public static final String FITFILENAMEEXTOUT = "_out.sdf";
    
    /**
     * Extension of output file with 2D picture of candidate
     */
    public static final String CANDIDATE2DEXTENSION = ".png";    
    
    /**
     * Postfix used to mark a file that cannot be read.
     */
    public static final String UNREADABLEFILEPOSTFIX = "_UNREADABLE";
    
    /**
     * Label used to point at text based graph format
     */
    public static final String GRAPHFORMATSTRING = "STRING";
    
    /**
     * Label used to point at byte based graph format
     */
    public static final String GRAPHFORMATBYTE = "BYTE";
    
    
    public static final double INVPI = 1.0/Math.sqrt(Math.PI * 2);
    
    
    public static final int MOLDIGITS = 8;

    /**
     * SDF tag containing graph ID
     */
    public static final String GCODETAG = "GCODE";
    
    /**
     * SDF tag containing graph encoding
     */
    public static final String GRAPHTAG = "GraphENC";
    
    /**
     * SDF tag containing graph encoding in JSON format
     */
    public static final String GRAPHJSONTAG = "GraphJson";
    
    /**
     * SDF tag containing vertex encoding in JSON format
     */
    public static final String VERTEXJSONTAG = "VertexJson";
    
    /**
     * SDF tag containing metadata on graph
     */
    public static final String GMSGTAG = "GraphMsg";

    /**
     * SDF tag defining attachment points
     */
    public static final String APSTAG = "ATTACHMENT_POINTS";
    
    /**
     * SDF tag containing errors during execution of molecule specific tasks
     */
    public static final String MOLERRORTAG = "MOL_ERROR";
    
    /**
     * SDF tag containing the fitness of a candidate
     */
    public static final String FITNESSTAG = "FITNESS";
    
    /**
     * SDF tag containing the SMILES of a candidate
     */
    public static final String SMILESTAG = "SMILES";
    
    /**
     * SDF tag containing the unique identifier of a candidate
     */
    public static final String UNIQUEIDTAG = "UID";
    
    /**
     * SDF tag containing the unique identifier of a candidate
     */
    public static final String INCHIKEYTAG = "InChi";
    
    /**
     * SDF tag defining the ID of a parent graph (used in FSE)
     */
    public static final String PARENTGRAPHTAG = "ParentGraph";  
    
    /**
     * SDF tag defining the graph generating level in an FSE run 
     */
    public static final String GRAPHLEVELTAG = "GraphLevel";

    /**
     * Symbol of dummy atom
     */
    public static final String DUMMYATMSYMBOL = "Du";
    
    /**
     * String tag of <code>Atom</code> property used to store attachment points
     */
    public static final String ATMPROPAPS = "ATTACHMENT_POINTS";

    /**
     * String tag of <code>Atom</code> property used to store the unique ID of
     * the <code>DENOPTIMVertex</code> corresponding to the molecular fragment
     * to which the atom belongs.
     */
    public static final String ATMPROPVERTEXID = "DENOPTIMVertexID";
    
    /**
     * String tag of <code>Atom</code> property used to store the original
     * position of an atom in the atom list of the fragment.
     */
    public static final String ATMPROPORIGINALATMID = "DENOPTIMAtomPosition";

    /**
     * String tag of <code>Bond</code>'s property used to store the 
     * property of being rotatable.
     */
    public static final String BONDPROPROTATABLE = "DENOPTIMRotable";

    /**
     * Recognised types of RingClosingAttractor and compatible types
     */
    public static final Map<String,String> RCATYPEMAP =
		    new HashMap<String,String>() 
    {
        {
            put("ATP", "ATM");
            put("ATM", "ATP");
            put("ATN", "ATN");
        };
    };

    /**
     * Smallest difference for comparison of double and float numbers.
     */
    public static final double FLOATCOMPARISONTOLERANCE = 0.000000001;
    
    /**
     * Separator between APs on different atoms in molecular property.
     */
    public static final String SEPARATORAPPROPATMS = " ";
    
    /**
     * Separator between APs on same atom in molecular property.
     */
    public static final String SEPARATORAPPROPAPS = ",";
    
    /**
     * Separator between atom index and APClass in molecular property.
     */
    public static final String SEPARATORAPPROPAAP = "#";
    
    /**
     * Separator between APClass and APSubClass and coordinates.
     */
    public static final String SEPARATORAPPROPSCL = ":";
    
    /**
     * Separator between coordinates
     */
    public static final String SEPARATORAPPROPXYZ = "%";

    /**
     * Key for {@link IAtomContainer} property containing the map of 
     * {@link AttachmentPoint}s per vertex ID. 
     */
    public static final Object MOLPROPAPxVID = "APsPerVertexID";

    /**
     * Key for {@link IAtomContainer} property containing the map of 
     * {@link AttachmentPoint}s per edge. 
     */
    public static final Object MOLPROPAPxEDGE = "APsPerEdge";
    
    /**
     * Key for {@link IAtomContainer} property containing the map of 
     * {@link AttachmentPoint}s per vertex ID. 
     */
    public static final Object MOLPROPAPxATOM = "APsPerAtom";
    
    /**
     * Key for {@link IAtomContainer} property containing the map of 
     * {@link AttachmentPoint}s per atom. 
     */
    public static final Object MOLPROPAPxBOND = "APsPerBond";

    /**
     * Key of the property remembering vertex IDs. Note this property 
     * is rarely in sync with the actual vertex ID of that
     * vertex.
     */
    public static final Object STOREDVID = "OLDVERTEXID";

    /**
     * Key of property used to records references of APs. This is used, for
     * instance, when dealing with templates
     * that inherit a molecular representation.
     */
    public static final Object LINKAPS = "LINKTOGRAPHSAP";

    /**
     * Property of AP where we save the original length of the AP vector.
     */
    public static final Object APORIGINALLENGTH = "ORIGINALLENGTH";
    

    /**
     * Property of {@link Vertex} used to keep mark symmetric vertexes
     * during graph operations and before defining the actual set of symmetric 
     * vertexes.
     */
    public static final Object VRTSYMMSETID = "VRTSYMMSETID";
    
}
