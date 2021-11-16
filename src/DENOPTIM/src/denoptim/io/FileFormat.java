package denoptim.io;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import denoptim.constants.DENOPTIMConstants;

public enum FileFormat {
    
    GRAPHJSON, GRAPHSDF, VRTXJSON, VRTXSDF,
    FSE_RUN, GA_RUN,
    GA_PARAM, FSE_PARAM, FR_PARAM, COMP_MAP,
    TXT;
        
    private String extension = "";
    
    /**
     * Collection of regex that, if matched, suggests assigning the format to a
     * specific FileFormat.
     */
    private Set<String> definingRegex = new HashSet<String>();
    
    /**
     * Collection of regex that, if matched, prevents assigning the format to a
     * specific FileFormat.
     */
    private Set<String> negatingRegex = new HashSet<String>();
    
    /**
     * Regex used to identify the end of the sampled text used to determine the
     * format of a file.
     */
    private String endOfSampleRegex = null;
    
    private Boolean isFolder = false;
    
    static {
        GRAPHSDF.extension = "sdf";
        GRAPHSDF.definingRegex = new HashSet<String>(Arrays.asList(
                "^> *<" + DENOPTIMConstants.GRAPHTAG + ">.*",
                "^> *<" + DENOPTIMConstants.GRAPHJSONTAG + ">.*"
                ));
        GRAPHSDF.endOfSampleRegex = "\\$\\$\\$\\$";
        
        //------------------------------------
        
        GRAPHJSON.extension = "json";
        
        //------------------------------------
        
        VRTXJSON.extension = "json";

        //------------------------------------
        
        VRTXSDF.extension = "sdf";
        VRTXSDF.definingRegex = new HashSet<String>(Arrays.asList(
                "^> *<" + DENOPTIMConstants.APSTAG+">.*"));
        VRTXSDF.endOfSampleRegex = "\\$\\$\\$\\$";

        //------------------------------------
        
        FSE_RUN.extension = "";
        FSE_RUN.isFolder = true;

        //------------------------------------
        
        GA_RUN.extension = "";
        GA_RUN.isFolder = true;

        //------------------------------------
        
        GA_PARAM.extension = "";
        GA_PARAM.definingRegex = new HashSet<String>(Arrays.asList(
                "^GA-.*"));

        //------------------------------------
        
        FSE_PARAM.extension = "";
        FSE_PARAM.definingRegex = new HashSet<String>(Arrays.asList(
                "^FSE-.*"));
        
        //------------------------------------
        
        FR_PARAM.extension = "";
        FR_PARAM.definingRegex = new HashSet<String>(Arrays.asList(
                "^FR-.*"));

        //------------------------------------
        
        COMP_MAP.extension = "";
        COMP_MAP.definingRegex = new HashSet<String>(Arrays.asList(
                "^RCN .*", "^RBO .*", "^CAP .*"));
        
        //------------------------------------
        
        TXT.extension = "";
    }
    
    public enum DataKind {GRAPH, VERTEX, GA_RUN, FSE_RUN, GA_PARAM, FSE_PARAM,
        FR_PARAM, COMP_MAP}
    
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
                        ff = GRAPHSDF;
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
                        ff = GRAPHJSON;
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
                    case FSE_PARAM:
                        ff = FSE_PARAM;
                        break;
                    case FR_PARAM:
                        ff = FR_PARAM;
                        break;
                    case COMP_MAP:
                        ff = COMP_MAP;
                        break;
                }
                break;
        }
        return ff;
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
