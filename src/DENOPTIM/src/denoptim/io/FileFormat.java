package denoptim.io;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import denoptim.constants.DENOPTIMConstants;

public enum FileFormat {
    
    GRAPHJSON, GRAPHSDF, VRTXJSON, VRTXSDF,
    FSE_RUN, GA_RUN,
    GA_PARAM, FSE_PARAM, COMP_MAP;
        
    private String extension = "";
    
    private Set<String> definingRegex = new HashSet<String>();
    
    private String endOfSampleRegex = null;
    
    private Boolean isFolder = false;
    
    static {
        GRAPHSDF.extension = "sdf";
        GRAPHSDF.definingRegex = new HashSet<String>(Arrays.asList(
                "^> *<" + DENOPTIMConstants.GRAPHTAG + ">.*"));
        GRAPHSDF.endOfSampleRegex = "\\$\\$\\$\\$";
        
        GRAPHJSON.extension = "json";
        
        VRTXJSON.extension = "json";
        
        VRTXSDF.extension = "sdf";
        VRTXSDF.definingRegex = new HashSet<String>(Arrays.asList(
                "^> *<" + DENOPTIMConstants.APTAG+">.*"));
        VRTXSDF.endOfSampleRegex = "\\$\\$\\$\\$";
        
        FSE_RUN.extension = "";
        FSE_RUN.isFolder = true;
        
        GA_RUN.extension = "";
        GA_RUN.isFolder = true;
        
        GA_PARAM.extension = "";
        GA_PARAM.definingRegex = new HashSet<String>(Arrays.asList(
                "^GA-.*"));
        
        FSE_PARAM.extension = "";
        FSE_PARAM.definingRegex = new HashSet<String>(Arrays.asList(
                "^FSE-.*"));
        
        COMP_MAP.extension = "";
        COMP_MAP.definingRegex = new HashSet<String>(Arrays.asList(
                "^RCN .*", "^RBO .*", "^CAP .*"));
    }
    
    public enum DataKind {GRAPH, VERTEX, GA_RUN, FSE_RUN, GA_PARAM, FSE_PARAM,
        COMP_MAP}
    
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
                    case COMP_MAP:
                        ff = COMP_MAP;
                        break;
                }
                break;
        }
        return ff;
    }

    /**
     * @return <code>true</code> if the file is supposed to be a folder
     */
    public boolean isFolder()
    {
        return isFolder;
    }
    
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
    
    /**
     * Returns the list of regex strings that allow identification of a file.
     * @return
     */
    public Set<String> getDefiningRegex()
    {
        return definingRegex;
    }
    
    public String getExtension()
    {
        return extension;
    }
}
