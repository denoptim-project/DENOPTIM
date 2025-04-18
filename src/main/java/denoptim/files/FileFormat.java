/*
 *   DENOPTIM
 *   Copyright (C) 2022 Marco Foscato <marco.foscato@uib.no>
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

package denoptim.files;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import denoptim.constants.DENOPTIMConstants;
import denoptim.programs.RunTimeParameters.ParametersType;

/**
 * File formats identified by DENOPTIM.
 *
 */
public enum FileFormat {
    
    CANDIDATESDF,
    GRAPHJSON, GRAPHSDF, VRTXJSON, VRTXSDF, MOLSDF,
    FSE_RUN, GA_RUN,
    
    GA_PARAM, FSE_PARAM, FR_PARAM, COMP_MAP, GO_PARAM, CLG_PARAM, GE_PARAM, 
    GI_PARAM, B3D_PARAM, FRG_RUN, FRG_PARAM, CUTRULE, M2G_PARAM,
    
    TXT, GRAPHTXT, GENSUMMARY,
    UNRECOGNIZED;
        
    private String extension = "";
    
    /**
     * Collection of regex that, if matched, suggests assigning the format to a
     * specific FileFormat. Consider a logical <b>OR</b> operator, i.e., any 
     * match will suggest among the set of regex will lead to format 
     * interpretation. Regex are matches against toUpperCase strings, so they
     * behave as case-insensitive.
     */
    private Set<String> definingRegex = new HashSet<String>();
    
    /**
     * Collection of regex that, if matched, prevents assigning the format to a
     * specific FileFormat. Regex are matches against toUpperCase strings, so they
     * behave as case-insensitive.
     */
    private Set<String> negatingRegex = new HashSet<String>();
    
    /**
     * Regex used to identify the end of the sampled text used to determine the
     * format of a file. Regex are matches against toUpperCase strings, so they
     * behave as case-insensitive.
     */
    private String endOfSampleRegex = null;
    
    private Boolean isFolder = false;
    
    static {
        GRAPHSDF.extension = "sdf";
        GRAPHSDF.definingRegex = new HashSet<String>(Arrays.asList(
                "^> *<" + DENOPTIMConstants.GRAPHTAG + ">.*",
                "^> *<" + DENOPTIMConstants.GRAPHJSONTAG + ">.*"
                ));
        GRAPHSDF.negatingRegex = new HashSet<String>(Arrays.asList(
                "^> *<" + DENOPTIMConstants.FITNESSTAG + ">.*",
                "^> *<" + DENOPTIMConstants.MOLERRORTAG + ">.*",
                "^> *<" + DENOPTIMConstants.UNIQUEIDTAG + ">.*",
                "^> *<" + DENOPTIMConstants.VERTEXJSONTAG + ">.*"
                ));
        GRAPHSDF.endOfSampleRegex = "\\$\\$\\$\\$";
        
        //------------------------------------
        
        CANDIDATESDF.extension = "sdf";
        CANDIDATESDF.definingRegex = new HashSet<String>(Arrays.asList(
                "^> *<" + DENOPTIMConstants.FITNESSTAG + ">.*",
                "^> *<" + DENOPTIMConstants.MOLERRORTAG + ">.*",
                "^> *<" + DENOPTIMConstants.UNIQUEIDTAG + ">.*",
                "^> *<" + DENOPTIMConstants.GRAPHJSONTAG + ">.*"
                ));
        CANDIDATESDF.endOfSampleRegex = "\\$\\$\\$\\$";
        
        //------------------------------------
        
        //TODO
        GRAPHJSON.extension = "json";
        
        //------------------------------------
        
        //TODO
        VRTXJSON.extension = "json";

        //------------------------------------
        
        VRTXSDF.extension = "sdf";
        VRTXSDF.definingRegex = new HashSet<String>(Arrays.asList(
                "^> *<" + DENOPTIMConstants.APSTAG+">.*"));
        VRTXSDF.negatingRegex = new HashSet<String>(Arrays.asList(
                "^> *<" + DENOPTIMConstants.FITNESSTAG + ">.*",
                "^> *<" + DENOPTIMConstants.MOLERRORTAG + ">.*",
                "^> *<" + DENOPTIMConstants.UNIQUEIDTAG + ">.*",
                "^> *<" + DENOPTIMConstants.GRAPHJSONTAG + ">.*"
                ));
        VRTXSDF.endOfSampleRegex = "\\$\\$\\$\\$";
        
        //------------------------------------
        
        MOLSDF.extension = "sdf";
        MOLSDF.negatingRegex = new HashSet<String>(Arrays.asList(
                "^> *<" + DENOPTIMConstants.FITNESSTAG + ">.*",
                "^> *<" + DENOPTIMConstants.APSTAG+">.*",
                "^> *<" + DENOPTIMConstants.MOLERRORTAG + ">.*",
                "^> *<" + DENOPTIMConstants.UNIQUEIDTAG + ">.*",
                "^> *<" + DENOPTIMConstants.GRAPHJSONTAG + ">.*"
                ));
        MOLSDF.endOfSampleRegex = "\\$\\$\\$\\$";

        //------------------------------------
        
        FSE_RUN.extension = "";
        FSE_RUN.isFolder = true;

        //------------------------------------
        
        FRG_RUN.extension = "";
        FRG_RUN.isFolder = true;

        //------------------------------------
        
        GA_RUN.extension = "";
        GA_RUN.isFolder = true;

        //------------------------------------
        
        GA_PARAM.extension = "";
        GA_PARAM.definingRegex = new HashSet<String>(Arrays.asList(
                "^" + ParametersType.GA_PARAMS.getKeywordRoot() + ".*"));

        //------------------------------------
        
        FSE_PARAM.extension = "";
        FSE_PARAM.definingRegex = new HashSet<String>(Arrays.asList(
                "^" + ParametersType.CEBL_PARAMS.getKeywordRoot() + ".*"));
        
        //------------------------------------
        
        FRG_PARAM.extension = "";
        FRG_PARAM.definingRegex = new HashSet<String>(Arrays.asList(
                "^" + ParametersType.FRG_PARAMS.getKeywordRoot() + ".*"));
        
        //------------------------------------
        
        M2G_PARAM.extension = "";
        M2G_PARAM.definingRegex = new HashSet<String>(Arrays.asList(
                "^" + ParametersType.M2G_PARAMS.getKeywordRoot() + ".*"));
        
        //------------------------------------
        
        CUTRULE.extension = "";
        CUTRULE.definingRegex = new HashSet<String>(Arrays.asList(
                "^CTR.*"));
        
        //------------------------------------
        
        FR_PARAM.extension = "";
        FR_PARAM.definingRegex = new HashSet<String>(Arrays.asList(
                "^" + ParametersType.FR_PARAMS.getKeywordRoot() + ".*"));

        //------------------------------------
        
        COMP_MAP.extension = "";
        COMP_MAP.definingRegex = new HashSet<String>(Arrays.asList(
                "^RCN .*", "^RBO .*", "^CAP .*"));
        
        //------------------------------------
        
        GO_PARAM.extension = "";
        GO_PARAM.definingRegex = new HashSet<String>(Arrays.asList(
                "^" + ParametersType.GO_PARAMS.getKeywordRoot() + ".*"));
        
        //------------------------------------
        
        GE_PARAM.extension = "";
        GE_PARAM.definingRegex = new HashSet<String>(Arrays.asList(
                "^" + ParametersType.GE_PARAMS.getKeywordRoot() + ".*"));
        
        //------------------------------------
        
        CLG_PARAM.extension = "";
        CLG_PARAM.definingRegex = new HashSet<String>(Arrays.asList(
                "^" + ParametersType.GLH_PARAMS.getKeywordRoot() + ".*"));
        
        //------------------------------------
        
        GI_PARAM.extension = "";
        GI_PARAM.definingRegex = new HashSet<String>(Arrays.asList(
                "^" + ParametersType.ISO_PARAMS.getKeywordRoot() + ".*"));
        
        B3D_PARAM.extension = "";
        B3D_PARAM.definingRegex = new HashSet<String>(Arrays.asList(
                "^" + ParametersType.MMB_PARAM.getKeywordRoot() + ".*"));
        
        //------------------------------------
        
        TXT.extension = "";
        
        //------------------------------------
        
        GRAPHTXT.extension = "txt";
        
        //------------------------------------
        
        GENSUMMARY.extension = "txt";
        GENSUMMARY.definingRegex = new HashSet<String>(Arrays.asList(
                "^" + DENOPTIMConstants.GAGENSUMMARYHEADER + ".*"));
        // WARNING: format of the GENSUMMARY is expected by methods such as 
        // DenoptimIO.readPopulationMembersTraces
        // Any change of format should could cause the need to change those 
        // methods.
    }
    
    /**
     * The kind of data found in a file.
     */
    public enum DataKind {GRAPH, VERTEX, GA_RUN, FSE_RUN, GA_PARAM, FSE_PARAM,
        FR_PARAM, GO_PARAM, CLG_PARAM, GE_PARAM, GI_PARAM, COMP_MAP, B3D_PARAM,
        M2G_PARAM,
        FRG_RUN, FRG_PARAM}
    
//------------------------------------------------------------------------------

    /**
     * Gets the FileFormat from file extension and kind of data.
     * @param extension the extension of the file (i.e., in 
     * <code>blabla.ext</code> the extension is <code>ext</code>).
     * @param kind the kind of data contained in the file.
     * @return the file format corresponding to the given extension and kind 
     * of data.
     */
    public static FileFormat fromString(String extension, DataKind kind) {
        FileFormat ff = null;
        switch (extension.toUpperCase())
        {
            case "SDF":
                switch (kind)
                {
                    case GRAPH:
                        ff = GRAPHSDF;
                        break;
                    case VERTEX:
                        ff = VRTXSDF;
                        break;
                    default:
                        ff = MOLSDF;
                        break;
                }
                break;
            case "JSON":
                switch (kind)
                {
                    case GRAPH:
                        ff = GRAPHJSON;
                        break;
                    case VERTEX:
                        ff = VRTXJSON;
                        break;
                    default:
                        break;
                }
                break;
                
            case "TXT":
                switch (kind)
                {
                    case GRAPH:
                        ff = GRAPHTXT;
                        break;
                    default:
                        break;
                }
                break;
                
            case "":
                switch (kind)
                {
                    case GA_RUN:
                        ff = GA_RUN;
                        break;
                    case FSE_RUN:
                        ff = FSE_RUN;
                        break;
                    case GA_PARAM:
                        ff = GA_PARAM;
                        break;
                    case M2G_PARAM:
                        ff = M2G_PARAM;
                    case FRG_RUN:
                        ff = FRG_RUN;
                        break;
                    case FRG_PARAM:
                        ff = FRG_PARAM;
                        break;
                    case FSE_PARAM:
                        ff = FSE_PARAM;
                        break;
                    case FR_PARAM:
                        ff = FR_PARAM;
                        break;
                    case GO_PARAM:
                        ff = GO_PARAM;
                        break;
                    case GE_PARAM:
                        ff = GE_PARAM;
                        break;
                    case CLG_PARAM:
                        ff = CLG_PARAM;
                        break;
                    case GI_PARAM:
                        ff = GI_PARAM;
                        break;
                    case COMP_MAP:
                        ff = COMP_MAP;
                        break;
                    case B3D_PARAM:
                        ff = B3D_PARAM;
                        break;
                    default:
                        break;
                }
                break;
        }
        return ff;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the collection of file formats using SDF syntax.
     * @return the collection of file formats using SDF syntax.
     */
    public static FileFormat[] getSDFFormats()
    {
        FileFormat[] a = {
                // GraphSDF must come before Vertex SDF
                FileFormat.CANDIDATESDF,
                FileFormat.GRAPHSDF, 
                FileFormat.VRTXSDF,
                FileFormat.MOLSDF};
        return a;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the collection of file formats with input parameters.
     * @return the collection of file formats with input parameters.
     */
    public static FileFormat[] getParameterFormats()
    {
        FileFormat[] a = {
            FileFormat.GO_PARAM,  
            FileFormat.GE_PARAM,
            FileFormat.CLG_PARAM,
            FileFormat.GI_PARAM, 
            FileFormat.B3D_PARAM,
         // GA must come after others that might use GA parameters, for example 
         // the setting of the random seed)
            FileFormat.GA_PARAM,  
            FileFormat.FSE_PARAM,
            FileFormat.M2G_PARAM,
            FileFormat.FR_PARAM,
            FileFormat.FRG_PARAM,
            FileFormat.COMP_MAP};
        return a;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the collection of file formats restricted to TXT files (excluded
     * parameter files)
     * @return the collection of file formats restricted to TXT files.
     */
    public static FileFormat[] getTXTFormats()
    {
        FileFormat[] a = {
                FileFormat.GENSUMMARY,
                FileFormat.CUTRULE,
                FileFormat.GRAPHTXT,
                FileFormat.TXT
        };
        return a;
    }
    
//------------------------------------------------------------------------------

    /**
     * @return <code>true</code> if the file is supposed to be a folder
     */
    public boolean isFolder()
    {
        return isFolder;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * In identifying a text file we read as many lines as possible until we 
     * match the "end-of-sample" string. This method returns the regex that 
     * defines the "end-of-sample" string.
     * @return the regex identifying the end of the sample text.
     */
    public String getSampleEndRegex()
    {
        return endOfSampleRegex;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the list of regex strings that allow identification of a file.
     * @return
     */
    public Set<String> getDefiningRegex()
    {
        return definingRegex;
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the list of regex strings that exclude a file format.
     * @return
     */
    public Set<String> getNegatingRegex()
    {
        return negatingRegex;
    }
    
//------------------------------------------------------------------------------
    
    public String getExtension()
    {
        return extension;
    }
    
//------------------------------------------------------------------------------

}
